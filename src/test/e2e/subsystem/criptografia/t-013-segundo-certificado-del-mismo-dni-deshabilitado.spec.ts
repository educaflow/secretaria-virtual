import { test, expect, Locator, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-013 — Segundo certificado del mismo DNI, deshabilitado
// origen: ESC-006  |  verifica: V-CertificadoDigital-005, R-CertificadoDigital-001, U-certificados-digitales-007
// fuente: .sdd/drafts/2026-09-08_02-53_nombre-apellidos-certificado-digital/test-e2e-desc/t-013-segundo-certificado-del-mismo-dni-deshabilitado.desc.md

// El DNI del escenario: el del usuario `secretario@mislata.es` de los datos de
// demostración. NO lleva sufijo `Date.now()` a propósito (excepción documentada
// de la regla de nombres únicos): el escenario entero se apoya en que ESE DNI
// corresponde a un usuario de la aplicación, para que las DOS altas copien el
// mismo nombre y los mismos apellidos de su ficha — que es justo lo que el
// «Resultado esperado» exige ver repetido en las dos filas; cambiarlo lo
// destruiría (y además es un DNI con letra de control, que un sufijo
// invalidaría). La idempotencia frente a la BD compartida (que NO se resetea) se
// consigue con la pre-limpieza defensiva del arranque + el teardown del
// `finally`, tal y como describe el «Estado inicial de la base de datos» de la
// descripción.
const DNI = '29050788V';

// Nombre y apellidos que las dos altas MUST copiar de la ficha de ese usuario.
const NOMBRE = 'Secretario';
const APELLIDOS = 'CIPFP Mislata';

// El título del enum llega a la UI con el sufijo `__!!` (marca de "no traducir"
// que el script de i18n elimina al generar los CSV, pero que el título crudo del
// dominio conserva). Los locators por nombre accesible hacen match por SUBCADENA,
// así que basta con el texto sin el sufijo.
const OPCION_CLASSPATH = 'Usar un fichero con el certificado que ya está dentro del del WAR';

// Las dos rutas del escenario. Son DISTINTAS a propósito: como la «Ruta
// classpath» NO es columna del listado, es lo único que permite demostrar CUÁL
// de las dos filas es la del paso 2 y cuál la del paso 4 (ver `abrirFilaDelDni`).
const RUTA_PRIMERO = 'firma/mi_certificado.p12';
const RUTA_SEGUNDO = 'firma/instalar_certificado_criptografico/secretario.p12';

const botonAnhadir = (page: Page) => page.getByRole('button', { name: 'Añadir certificado digital' });

// Las filas del listado cuyo DNI es el del escenario.
// CRITICAL: este test deja DOS filas con ese DNI y, como el nombre accesible de
// una fila es «<dni> <nombre> <apellidos> <tipo de certificado>» y los cuatro
// valores coinciden, las dos filas tienen un nombre accesible IDÉNTICO. Por eso
// el locator es intrínsecamente múltiple: se resuelve siempre con `.nth(...)`
// (el orden del listado es significativo, ver el «Resultado esperado») y NUNCA
// se usa sin acotar, que dispararía el modo estricto de Playwright.
const filasDelDni = (page: Page) => page.getByRole('row', { name: DNI });

// La fila que la rejilla pinta cuando no hay ningún registro.
const filaSinRegistros = (page: Page) => page.getByRole('row', { name: 'No records found.' });

// --- Campos del formulario -------------------------------------------------
// El nombre accesible real de varios de ellos lleva un « ?» por el icono de
// ayuda; el match por subcadena lo cubre. El `exact: true` del DNI evita que ese
// locator casase con otro campo que lo contuviera. «Ruta classpath» solo se
// muestra con el tipo CLASSPATH elegido y su etiqueta real es «Ruta classpath__!!».
const campoDni = (page: Page) => page.getByRole('textbox', { name: 'DNI', exact: true });
const campoNombre = (page: Page) => page.getByRole('textbox', { name: 'Nombre' });
const campoApellidos = (page: Page) => page.getByRole('textbox', { name: 'Apellidos' });
const campoTipoCertificado = (page: Page) => page.getByRole('combobox', { name: 'Tipo de certificado' });
const campoRutaClasspath = (page: Page) => page.getByRole('textbox', { name: 'Ruta classpath' });
const campoHabilitado = (page: Page) => page.getByRole('checkbox', { name: 'Habilitado' });

// Botón de aceptar del diálogo de aviso de Axelor (el «Current changes will be
// lost» que salta cuando la SPA aún considera sucio un formulario, o un
// «Validation Error»). Mientras el diálogo está abierto tapa la pantalla e
// intercepta cualquier clic.
const botonAceptarAviso = (page: Page) => page.getByRole('dialog').getByRole('button', { name: /^(OK|Aceptar)$/ });

// Cierra el aviso si está abierto AHORA MISMO. `isVisible()` responde al
// instante, así que esto solo sirve para un aviso ya presente; para el que llega
// con retardo, ver `asentarTrasGuardar`.
async function cerrarDialogoDeAviso(page: Page): Promise<void> {
  const ok = botonAceptarAviso(page);
  if (await ok.isVisible().catch(() => false)) {
    await ok.click();
  }
}

// Deja la SPA asentada después de un «Guardar».
//
// CRITICAL — comprobado contra la app real: al guardar, Axelor vuelve al listado
// y AL RATO lanza sobre él el aviso «Current changes will be lost». Ese aviso
// pendiente es venenoso para el paso siguiente: si mientras tanto se abre otra
// vista (el alta del segundo certificado, o una fila), al resolverse el aviso la
// SPA DESCARTA esa vista y vuelve al listado — con el resultado desconcertante
// de que el campo «DNI» aparece un instante y se esfuma. Por eso, en vez de
// pelearse con la carrera en cada clic posterior, se drena aquí: se espera a que
// el aviso salga y se cierra.
//
// La espera está acotada y se ignora si no llega: el aviso es un efecto
// secundario de la SPA, no parte del escenario, así que su ausencia no debe
// hacer fallar el test (y cuando aparece —que es lo normal— la espera termina en
// cuanto se pinta, sin coste).
async function asentarTrasGuardar(page: Page): Promise<void> {
  await botonAceptarAviso(page).waitFor({ timeout: 10000 }).catch(() => {});
  await cerrarDialogoDeAviso(page);
}

// Pulsa un elemento del listado tolerando el aviso ASÍNCRONO «Current changes
// will be lost» que Axelor lanza tras guardar, y no se da por satisfecho hasta
// que el clic ha SURTIDO EFECTO (`resultado` visible).
//
// CRITICAL: comprobado contra la app real — tras el «Guardar» de un certificado,
// el aviso se pinta ENCIMA del listado con un retardo, y tanto el clic en
// «Añadir certificado digital» (para dar de alta el siguiente) como el clic en
// una fila mueren interceptados por el botón «Cancel» del modal. `isVisible()`
// responde al instante y daría un falso negativo, así que no basta con
// preguntar una vez y pulsar.
//
// CRITICAL — y el clic tampoco basta como señal de éxito: hay una carrera en la
// que el clic NO se intercepta (no lanza excepción) pero el diálogo aparece
// justo después y, al aceptarlo, la SPA se queda en el listado en vez de abrir
// lo que se había pulsado. Un reintento que solo mirase si el clic lanzó
// excepción daría por bueno ese caso y dejaría el test fallando más adelante con
// un error desconcertante («no encuentro el campo DNI»). Por eso se espera al
// EFECTO del clic y, si no llega, se vuelve a pulsar.
async function pulsarHasta(page: Page, destino: Locator, resultado: Locator, descripcion: string): Promise<void> {
  for (let intento = 0; intento < 5; intento++) {
    await cerrarDialogoDeAviso(page);
    try {
      await destino.click({ timeout: 5000 });
    } catch {
      // El aviso ya estaba abierto y interceptó el clic: se cierra en la vuelta
      // siguiente y se reintenta.
      continue;
    }

    // Tras el clic, espera a que la SPA se resuelva en uno de sus DOS estados
    // posibles, sin sondear ni dormir: o sale el aviso (que llegó tarde y se ha
    // comido la navegación), o aparece el efecto buscado.
    const aceptar = page.getByRole('dialog').getByRole('button', { name: /^(OK|Aceptar)$/ });
    try {
      await aceptar.or(resultado).first().waitFor({ timeout: 10000 });
    } catch {
      continue;
    }

    if (await aceptar.isVisible().catch(() => false)) {
      // El aviso ganó la carrera: aceptarlo DESCARTA la vista que el clic estaba
      // abriendo y devuelve al listado, así que hay que volver a pulsar — ya sin
      // ningún aviso pendiente, porque este solo se lanza una vez tras guardar.
      await aceptar.click();
      continue;
    }
    return;
  }
  throw new Error(`No se pudo pulsar ${descripcion}`);
}

// Barrera de carga del listado.
// CRITICAL: la rejilla pide sus filas en una petición aparte de la que pinta la
// vista, así que mirar las filas nada más aparecer el botón de alta es una
// condición de carrera — y esa carrera haría que la pre-limpieza no viese las
// filas residuales de un run anterior y las aserciones contasen una rejilla a
// medio cargar. Esperar a que la rejilla se resuelva en uno de sus dos estados
// posibles la elimina: o hay filas del DNI, o está la fila «No records found.»
// (esta tabla solo la escriben los tests de esta iniciativa y la anterior, y
// ambas limpian lo que crean; si apareciese otro estado, el test falla de forma
// ruidosa en vez de dar por buena una rejilla incompleta).
async function esperarListadoCargado(page: Page): Promise<void> {
  await expect(botonAnhadir(page)).toBeVisible();
  await expect(filasDelDni(page).or(filaSinRegistros(page)).first()).toBeVisible();
}

// Abre el listado «Certificados digitales» desde el menú «Administración SV».
// MUST llamarse solo con la pestaña aún cerrada: una vez abierta, el título de la
// pestaña repite el texto del ítem de menú y el locator por texto sería ambiguo.
async function abrirCertificadosDigitales(page: Page): Promise<void> {
  await page.getByText('Administración SV', { exact: true }).click();
  await page.getByText('Certificados digitales', { exact: true }).click();
  await esperarListadoCargado(page);
}

// Abre el formulario de la fila `indice`-ésima del DNI (0 = la primera del
// listado). El índice importa: el «Resultado esperado» identifica las dos filas
// por su POSICIÓN, y abrirlas es la única forma de leer su «Ruta classpath»,
// que no es columna del listado.
async function abrirFilaDelDni(page: Page, indice: number): Promise<void> {
  // El efecto que confirma que la fila se abrió: el botón «Borrar», que solo
  // existe en el formulario de un registro ya guardado.
  await pulsarHasta(
    page,
    filasDelDni(page).nth(indice),
    page.getByRole('button', { name: 'Borrar' }),
    `la fila ${indice} del DNI ${DNI}`,
  );
}

// Vuelve al listado desde donde esté: cierra cualquier diálogo abierto y, si hay
// un formulario delante, lo abandona confirmando el descarte de cambios.
async function volverAlListado(page: Page): Promise<void> {
  await cerrarDialogoDeAviso(page);

  const cancelar = page.getByRole('button', { name: 'Cancelar' });
  if (await cancelar.isVisible().catch(() => false)) {
    await cancelar.click();
    // Axelor pregunta antes de descartar un formulario con cambios sin guardar
    // («Current changes will be lost. Do you really want to proceed?»).
    // CRITICAL: ese diálogo se pinta de forma ASÍNCRONA, así que preguntarle a
    // `isVisible()` (que responde al instante) justo tras el clic da un falso
    // negativo, el diálogo se queda abierto tapando la pantalla y la espera del
    // listado muere por timeout. Se espera a que la SPA se resuelva en uno de sus
    // dos estados posibles: o aparece el diálogo (había cambios sin guardar), o
    // ya estamos en el listado (no los había y salió directo).
    const confirmar = page.getByRole('dialog').getByRole('button', { name: /^(OK|Aceptar|Yes|Sí)$/ });
    await confirmar.or(botonAnhadir(page)).first().waitFor();
    if (await confirmar.isVisible().catch(() => false)) {
      await confirmar.click();
    }
  }
  await esperarListadoCargado(page);
}

// Borra TODAS las filas del DNI si siguen existiendo, partiendo de donde esté.
// Se usa DOS veces: como pre-limpieza defensiva al arrancar (este test CREA
// certificados con ese DNI, y una fila HABILITADA residual de un run anterior
// abortado haría fallar el alta del primero por DNI ya habilitado) y como
// teardown en el `finally`.
// CRITICAL — a diferencia de los tests hermanos, este escenario deja DOS filas
// del mismo DNI: la limpieza MUST borrarlas TODAS, no solo una. De ahí el bucle
// sobre el número de filas restantes en vez de un único borrado.
async function borrarEntradasDelDniSiExisten(page: Page): Promise<void> {
  await volverAlListado(page);

  // El escenario deja dos filas, pero un run anterior abortado pudo dejar más.
  // LIMIT defensivo para no colgar el test si el borrado dejara de funcionar.
  for (let i = 0; i < 6 && (await filasDelDni(page).count()) > 0; i++) {
    await abrirFilaDelDni(page, 0);
    await page.getByRole('button', { name: 'Borrar' }).click();
    // Diálogo de confirmación de Axelor; con el idioma del admin el botón de
    // confirmar aparece en inglés («Delete»). Se acota al diálogo para no chocar
    // con el botón «Borrar» del formulario que queda detrás.
    await page.getByRole('dialog').getByRole('button', { name: /Delete|Eliminar/ }).click();
    await esperarListadoCargado(page);
  }
  await expect(filasDelDni(page)).toHaveCount(0);
}

// Da de alta un certificado del DNI del escenario con la ruta indicada,
// dejándolo habilitado o no, y vuelve al listado.
//
// `habilitado` es el estado en que MUST quedar la casilla: el alta la trae
// marcada por defecto, así que para el segundo certificado hay que desmarcarla.
// Se comprueba que venía marcada ANTES de tocarla: `uncheck()` sobre una casilla
// ya desmarcada es un no-op silencioso, así que sin esa aserción el test pasaría
// igual aunque el «desmarcar» del escenario no estuviera cambiando nada.
async function altaCertificado(page: Page, ruta: string, habilitado: boolean): Promise<void> {
  // El efecto que confirma que el alta se abrió: el campo «DNI» editable del
  // formulario nuevo.
  const dni = campoDni(page);
  await pulsarHasta(page, botonAnhadir(page), dni, 'el botón «Añadir certificado digital»');
  await expect(dni).toBeVisible();

  // El `Tab` saca el foco del campo: el autorrelleno de nombre y apellidos lo
  // dispara el evento de cambio, que Axelor emite al perder el foco, no al
  // teclear.
  //
  // CRITICAL: ese autorrelleno es una llamada AL SERVIDOR (`POST /ws/action` con
  // la acción `...CertificadoDigital-onChange-dni-action`), y su respuesta ESCRIBE
  // en «Nombre» y «Apellidos» — con este DNI, copiando los de la ficha del usuario
  // `secretario@mislata.es`. Sin esperar a que termine, seguir adelante sería una
  // condición de carrera: el alta podría llegar al servidor con el nombre aún
  // vacío y morir con «El nombre es obligatorio».
  await dni.fill(DNI);
  await Promise.all([
    page.waitForResponse(
      (respuesta) =>
        respuesta.url().includes('/ws/action') &&
        respuesta.request().method() === 'POST' &&
        (respuesta.request().postData() ?? '').includes('onChange-dni-action'),
    ),
    dni.press('Tab'),
  ]);

  // Guarda de precondición: el nombre y los apellidos los pone el AUTORRELLENO
  // desde la ficha del usuario, no el administrador. Es lo que hace que el
  // «Resultado esperado» pueda exigir los mismos valores en las DOS filas.
  await expect(campoNombre(page)).toHaveValue(NOMBRE);
  await expect(campoApellidos(page)).toHaveValue(APELLIDOS);

  await campoTipoCertificado(page).click();
  await page.getByRole('option', { name: OPCION_CLASSPATH }).click();

  // El campo solo se muestra al elegir el tipo CLASSPATH.
  await campoRutaClasspath(page).fill(ruta);

  const casilla = campoHabilitado(page);
  await expect(casilla).toBeVisible();
  await expect(casilla).toBeChecked();
  if (!habilitado) {
    await casilla.uncheck();
    await expect(casilla).not.toBeChecked();
  }

  await page.getByRole('button', { name: 'Guardar' }).click();

  // El sistema guarda y vuelve al listado (la vista pasa de /edit a /list y
  // reaparece el botón de alta, que solo existe en la vista de lista).
  await expect(page).toHaveURL(/CertificadoDigital-action\/list/);
  await asentarTrasGuardar(page);
  await esperarListadoCargado(page);
}

test.describe('Certificados digitales', () => {
  test('Segundo certificado del mismo DNI, deshabilitado', async ({ page }) => {
    await ensureLoggedOut(page);
    // Precondición: el usuario `admin` ha iniciado sesión con «admin»/«admin».
    await login(page, 'admin', 'admin');

    try {
      // Paso 1: Dado que el administrador está en la pantalla «Certificados
      //         digitales» (menú «Administración SV» → «Certificados digitales»).
      await abrirCertificadosDigitales(page);

      // Precondición: no existe ningún certificado digital con DNI «29050788V»
      // (si quedaran de una ejecución anterior, se borran TODOS desde el listado).
      await borrarEntradasDelDniSiExisten(page);

      // Paso 2: Cuando pulsa «Añadir certificado digital», escribe en «DNI»
      //         «29050788V», elige en «Tipo de certificado» la opción «Usar un
      //         fichero con el certificado que ya está dentro del del WAR»,
      //         escribe en «Ruta classpath» «firma/mi_certificado.p12» y pulsa
      //         «Guardar».
      await altaCertificado(page, RUTA_PRIMERO, true);

      // Paso 3: Entonces el sistema guarda el certificado y vuelve al listado.
      await expect(filasDelDni(page)).toHaveCount(1);

      // Paso 4: Cuando pulsa «Añadir certificado digital», escribe en «DNI»
      //         «29050788V», elige el mismo tipo de certificado, escribe en «Ruta
      //         classpath» «firma/instalar_certificado_criptografico/secretario.p12»
      //         y desmarca la casilla «Habilitado».
      // Paso 5: Y pulsa «Guardar».
      await altaCertificado(page, RUTA_SEGUNDO, false);

      // Resultado esperado 1: el sistema guarda el certificado y vuelve al
      // listado «Certificados digitales» (lo comprueba `altaCertificado`: la URL
      // pasa a /list y reaparece el botón de alta). Que el SEGUNDO certificado se
      // haya guardado —y no lo rechace la regla del DNI ya habilitado— es el
      // corazón del escenario: se admite porque va deshabilitado.
      await expect(page).toHaveURL(/CertificadoDigital-action\/list/);
      await expect(botonAnhadir(page)).toBeVisible();

      // Resultado esperado 2: el listado muestra DOS filas con DNI «29050788V»,
      // las dos con «Nombre» «Secretario» y «Apellidos» «CIPFP Mislata».
      await expect(filasDelDni(page)).toHaveCount(2);
      for (const indice of [0, 1]) {
        const fila = filasDelDni(page).nth(indice);
        await expect(fila.getByRole('gridcell', { name: DNI, exact: true })).toBeVisible();
        await expect(fila.getByRole('gridcell', { name: NOMBRE, exact: true })).toBeVisible();
        await expect(fila.getByRole('gridcell', { name: APELLIDOS, exact: true })).toBeVisible();
      }

      // Resultado esperado 3: de esas dos filas, la PRIMERA tiene «Habilitado»
      // marcado y la SEGUNDA lo tiene sin marcar — la ordenación por defecto
      // (`dni,-enabled`) pone los habilitados antes que los deshabilitados dentro
      // del mismo DNI. (La celda de esa columna es la única casilla de la fila:
      // la rejilla no tiene columna de selección.)
      await expect(filasDelDni(page).nth(0).getByRole('checkbox')).toBeChecked();
      await expect(filasDelDni(page).nth(1).getByRole('checkbox')).not.toBeChecked();

      // Y que esa primera fila es exactamente «la creada en el paso 2» (y la
      // segunda «la creada en el paso 4»), como dice el «Resultado esperado»:
      // las dos filas son indistinguibles en el listado (mismo DNI, nombre,
      // apellidos y tipo), así que lo único que las identifica es su «Ruta
      // classpath», que NO es columna y solo se ve abriendo el formulario. Sin
      // esta comprobación, la aserción de orden de arriba pasaría igual aunque el
      // criterio de ordenación fuera otro que casualmente dejara una habilitada
      // arriba.
      await abrirFilaDelDni(page, 0);
      await expect(campoRutaClasspath(page)).toHaveValue(RUTA_PRIMERO);
      await expect(campoHabilitado(page)).toBeChecked();
      await volverAlListado(page);

      await abrirFilaDelDni(page, 1);
      await expect(campoRutaClasspath(page)).toHaveValue(RUTA_SEGUNDO);
      await expect(campoHabilitado(page)).not.toBeChecked();
      await volverAlListado(page);
    } finally {
      try {
        // Teardown: el escenario deja DOS certificados creados con ese DNI, y uno
        // de ellos habilitado impediría el alta del siguiente run, así que MUST
        // borrarse TODOS para que el test se pueda reejecutar sin limpiar la BD a
        // mano. Best-effort para no enmascarar el fallo real de una aserción.
        await borrarEntradasDelDniSiExisten(page);
      } catch {
        // limpieza best-effort.
      }
    }

    await logout(page);
  });
});
