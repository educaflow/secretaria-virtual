import { test, expect, Locator, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-017 — Borrar uno de los certificados de una persona
// origen: ESC-010  |  verifica: —
// fuente: .sdd/drafts/2026-09-08_02-53_nombre-apellidos-certificado-digital/test-e2e-desc/t-017-borrar-uno-de-los-certificados-de-una-persona.desc.md

// El DNI del escenario: el del usuario `secretario@mislata.es` de los datos de
// demostración. NO lleva sufijo `Date.now()` a propósito (excepción documentada
// de la regla de nombres únicos): el escenario se apoya en que ESE DNI
// corresponde a un usuario de la aplicación (las DOS altas copian nombre y
// apellidos de su ficha, sin los cuales el guardado moriría por «El nombre es
// obligatorio» antes de llegar a lo que este test verifica), y además es un DNI
// con letra de control que un sufijo invalidaría. La idempotencia frente a la BD
// compartida (que NO se resetea) se consigue con la pre-limpieza defensiva del
// arranque + el teardown del `finally`, tal y como describe el «Estado inicial de
// la base de datos» de la descripción.
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
// classpath» NO es columna del listado, es lo único que permite demostrar CUÁL de
// las dos filas es cuál — y, en este escenario, que la fila BORRADA es la que
// tocaba y que la SUPERVIVIENTE es la otra (que es justo lo que se prueba).
const RUTA_SUPERVIVIENTE = 'firma/mi_certificado.p12';
const RUTA_A_BORRAR = 'firma/instalar_certificado_criptografico/secretario.p12';

// La URL del listado, para la RECARGA EN DURO del final (ver el «Resultado
// esperado»): es la ruta a la que Axelor vuelve tras guardar y la que se pide de
// nuevo al servidor para leer el estado REAL de la base de datos.
const URL_LISTADO = '/#/ds/subsysCriptografia.Main%40CertificadoDigital-action/list/1';

const botonAnhadir = (page: Page) => page.getByRole('button', { name: 'Añadir certificado digital' });

// Las filas del listado cuyo DNI es el del escenario.
// CRITICAL: este test deja DOS filas con ese DNI (hasta que borra una) y, como el
// nombre accesible de una fila es «<dni> <nombre> <apellidos> <tipo de
// certificado>» y los cuatro valores coinciden, las dos filas tienen un nombre
// accesible IDÉNTICO. Por eso el locator es intrínsecamente múltiple: se resuelve
// siempre con `.nth(...)` o con `toHaveCount(...)` y NUNCA se usa sin acotar, que
// dispararía el modo estricto de Playwright.
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

// El botón «Borrar» solo existe en el formulario de un registro YA GUARDADO: es
// la señal de que hay una fila abierta (y no el listado ni un alta nueva). En
// ESTE escenario es además el botón que ejecuta el paso central del test.
const botonBorrar = (page: Page) => page.getByRole('button', { name: 'Borrar' });

// El diálogo con el que Axelor rechazaría un guardado por una regla de negocio.
// En ESTE escenario no debe salir nunca: los dos guardados tienen éxito (el
// primero porque no hay ningún otro certificado del DNI, el segundo porque va
// deshabilitado), así que se aserta su AUSENCIA (ver `guardarConExito`).
const dialogoValidationError = (page: Page) =>
  page.getByRole('dialog').getByRole('heading', { name: 'Validation Error' });

// Botón de aceptar del diálogo de aviso de Axelor (el «Current changes will be
// lost» que salta cuando la SPA aún considera sucio un formulario). Mientras el
// diálogo está abierto tapa la pantalla e intercepta cualquier clic.
const botonAceptarAviso = (page: Page) => page.getByRole('dialog').getByRole('button', { name: /^(OK|Aceptar)$/ });

// ¿Queda un aviso «Current changes will be lost» PENDIENTE de llegar?
//
// CRITICAL — es la pieza que hace este test determinista en vez de dependiente
// del reloj. Axelor lanza ese aviso con RETARDO tras guardar o borrar, cuando la
// vista ya ha vuelto al listado, y el retardo no está acotado: en una máquina
// cargada llega segundos después. Mientras siga pendiente, CUALQUIER vista que se
// abra puede ser descartada al resolverse (el síntoma observado: el campo «DNI»
// aparece un instante y se esfuma, y la SPA vuelve al listado). Preguntar «¿hay
// un diálogo ahora mismo?» con `isVisible()` no basta, porque el aviso todavía no
// se ha pintado; por eso el test RECUERDA que se lo deben y `pulsarHasta` no da
// por asentado ningún clic hasta haberlo drenado.
//
// Se pone a `true` tras cada operación que lo provoca (guardar, borrar) y vuelve
// a `false` en cuanto se drena —o cuando se comprueba que ya no va a llegar—.
let avisoPendiente = false;

// Cierra el aviso si está abierto AHORA MISMO y dice si lo ha hecho.
// `isVisible()` responde al instante, así que esto solo sirve para un aviso ya
// presente; para el que llega con retardo, ver `asentarTrasGuardar` y la ventana
// de asentamiento de `pulsarHasta`.
async function cerrarDialogoDeAviso(page: Page): Promise<boolean> {
  const ok = botonAceptarAviso(page);
  if (await ok.isVisible().catch(() => false)) {
    await ok.click();
    avisoPendiente = false;
    return true;
  }
  return false;
}

// Deja la SPA asentada después de un «Guardar» que SÍ ha vuelto al listado.
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
      avisoPendiente = false;
      continue;
    }

    // El resultado ya está en pantalla y no hay ningún diálogo AHORA MISMO. Si no
    // se debe ningún aviso, el clic está asentado y se puede seguir.
    if (!avisoPendiente) {
      return;
    }

    // Pero si el aviso sigue pendiente, puede llegar AÚN DESPUÉS de que el
    // resultado se haya pintado y descartar la vista recién abierta. Esta es la
    // carrera que hacía el test intermitente: `pulsarHasta` devolvía el control
    // con el formulario ya en pantalla y, milisegundos más tarde, la SPA lo
    // tiraba y volvía al listado, con lo que el paso siguiente moría con un
    // «no encuentro el campo DNI» desconcertante. Por eso aquí se ESPERA al
    // aviso durante una ventana corta en vez de preguntar una sola vez.
    try {
      await aceptar.waitFor({ timeout: 6000 });
    } catch {
      // No ha llegado: ya no va a llegar, y la vista está asentada.
      avisoPendiente = false;
      return;
    }

    await aceptar.click();
    avisoPendiente = false;

    // Lo normal es que el aviso se haya llevado por delante la vista que el clic
    // abrió, así que hay que volver a pulsar. Pero si sobrevivió, volver a
    // pulsar sería un error (el destino ya no está en pantalla), así que se
    // comprueba antes de reintentar.
    if (await resultado.waitFor({ timeout: 2000 }).then(() => true).catch(() => false)) {
      return;
    }
  }
  throw new Error(`No se pudo pulsar ${descripcion}`);
}

// Barrera de carga del listado.
// CRITICAL: la rejilla pide sus filas en una petición aparte de la que pinta la
// vista, así que mirar las filas nada más aparecer el botón de alta es una
// condición de carrera — y esa carrera haría que la pre-limpieza no viese las
// filas residuales de un run anterior y que el conteo del final contase una
// rejilla a medio cargar (dando por bueno «queda UNA sola fila» por el motivo
// equivocado: porque la segunda aún no se había pintado, no porque se borrara).
// Esperar a que la rejilla se resuelva en uno de sus dos estados posibles la
// elimina: o hay filas del DNI, o está la fila «No records found.».
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
// listado). El índice importa: el escenario identifica las dos filas por su
// POSICIÓN, y abrirlas es la única forma de leer su «Ruta classpath», que no es
// columna del listado.
async function abrirFilaDelDni(page: Page, indice: number): Promise<void> {
  // El efecto que confirma que la fila se abrió: el botón «Borrar», que solo
  // existe en el formulario de un registro ya guardado.
  await pulsarHasta(
    page,
    filasDelDni(page).nth(indice),
    botonBorrar(page),
    `la fila ${indice} del DNI ${DNI}`,
  );
  // El formulario se pinta antes de que sus campos tengan valor; sin esta espera,
  // leer «Ruta classpath» con `inputValue()` (que responde al instante) devolvería
  // la cadena vacía y la identificación de la fila sería aleatoria.
  await expect(campoRutaClasspath(page)).not.toHaveValue('');
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

// Borra el registro cuyo formulario está ABIERTO: pulsa «Borrar» y confirma.
//
// CRITICAL — es el mecanismo que ejecuta el paso central de ESTE escenario (y,
// de paso, el que usan la pre-limpieza y el teardown). El diálogo de confirmación
// de Axelor sale con el idioma del admin, así que su botón puede aparecer en
// inglés («Delete»); se acota al `dialog` para no chocar con el botón «Borrar»
// del formulario que queda detrás, que tiene un nombre accesible parecido.
async function borrarFilaAbierta(page: Page): Promise<void> {
  await botonBorrar(page).click();
  await page.getByRole('dialog').getByRole('button', { name: /Delete|Eliminar/ }).click();
  // El borrado también deja a deber el aviso con retardo (ver `avisoPendiente`).
  avisoPendiente = true;
  await esperarListadoCargado(page);
}

// Borra TODAS las filas del DNI si siguen existiendo, partiendo de donde esté.
// Se usa DOS veces: como pre-limpieza defensiva al arrancar (este test CREA
// certificados con ese DNI, y una fila HABILITADA residual de un run anterior
// abortado haría fallar ya el alta del PRIMERO —«Ya existe un certificado digital
// habilitado para el DNI…»— y el test dejaría de probar lo que dice probar) y
// como teardown en el `finally`.
// CRITICAL — este escenario deja hasta DOS filas del mismo DNI: la limpieza MUST
// borrarlas TODAS, no solo una. De ahí el bucle sobre el número de filas
// restantes en vez de un único borrado.
async function borrarEntradasDelDniSiExisten(page: Page): Promise<void> {
  await volverAlListado(page);

  // El escenario deja como mucho dos filas, pero un run anterior abortado pudo
  // dejar más. LIMIT defensivo para no colgar el test si el borrado dejara de
  // funcionar.
  for (let i = 0; i < 6 && (await filasDelDni(page).count()) > 0; i++) {
    await abrirFilaDelDni(page, 0);
    await borrarFilaAbierta(page);
  }
  await expect(filasDelDni(page)).toHaveCount(0);
}

// Pulsa «Guardar» y comprueba que el guardado tiene ÉXITO: la vista vuelve al
// listado (de /edit a /list) sin que el servidor haya rechazado nada.
//
// CRITICAL — en este escenario los DOS guardados salen bien, así que la ausencia
// de rechazo es parte de lo que hay que demostrar: se espera a que la SPA se
// resuelva en uno de sus dos estados posibles (diálogo «Validation Error», o
// listado) y se aserta que el que salió NO es el del error. Sin esta
// comprobación, un rechazo se manifestaría más adelante como un fallo
// desconcertante de conteo de filas — y, peor, el «queda UNA sola fila» del final
// podría darse por bueno porque la segunda alta nunca llegó a crearse.
async function guardarConExito(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Guardar' }).click();
  // A partir de aquí Axelor debe un aviso con retardo (ver `avisoPendiente`).
  avisoPendiente = true;

  const error = dialogoValidationError(page);
  await error.or(botonAnhadir(page)).first().waitFor();
  await expect(error).toHaveCount(0);

  await expect(page).toHaveURL(/CertificadoDigital-action\/list/);
  await asentarTrasGuardar(page);
  await esperarListadoCargado(page);
}

// Abre el alta y rellena el formulario de un certificado del DNI del escenario
// con la ruta indicada, dejándolo listo para pulsar «Guardar».
//
// `habilitado` es el estado en que MUST quedar la casilla: el alta la trae
// marcada por defecto, así que para el segundo certificado hay que desmarcarla.
// Se comprueba que venía marcada ANTES de tocarla: `uncheck()` sobre una casilla
// ya desmarcada es un no-op silencioso, así que sin esa aserción el test pasaría
// igual aunque el «desmarcar» del escenario no estuviera cambiando nada.
async function rellenarAlta(page: Page, ruta: string, habilitado: boolean): Promise<void> {
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
  // vacío y morir con «El nombre es obligatorio», un rechazo que este escenario
  // (en el que las dos altas tienen éxito) no contempla.
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
  // desde la ficha del usuario, no el administrador. Sin ellos el alta moriría
  // por campo obligatorio antes de llegar siquiera a guardarse.
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
}

// Da de alta un certificado y comprueba que el guardado SÍ tiene éxito. En ESTE
// escenario las DOS altas se guardan bien: la primera porque no hay ningún otro
// certificado del DNI, y la segunda porque va deshabilitada.
async function altaCertificado(page: Page, ruta: string, habilitado: boolean): Promise<void> {
  await rellenarAlta(page, ruta, habilitado);
  await guardarConExito(page);
}

// Abre las DOS filas del DNI, una a una y en orden de listado, y devuelve lo que
// hay DENTRO de cada formulario: su «Ruta classpath» y el estado de su casilla
// «Habilitado». Deja la vista de vuelta en el listado.
//
// CRITICAL: la «Ruta classpath» no es columna del listado y las dos filas son
// indistinguibles en él (mismo DNI, nombre, apellidos y tipo), así que abrirlas es
// la ÚNICA forma de saber cuál es cuál. En ESTE escenario eso no es un adorno: lo
// que hay que borrar es una fila CONCRETA (la de
// `firma/instalar_certificado_criptografico/secretario.p12`), y confiar solo en la
// posición dejaría el test dando por bueno el borrado de la fila equivocada.
async function leerLasDosFilas(page: Page): Promise<Array<{ ruta: string; habilitado: boolean }>> {
  await expect(filasDelDni(page)).toHaveCount(2);

  const filas: Array<{ ruta: string; habilitado: boolean }> = [];
  for (const indice of [0, 1]) {
    await abrirFilaDelDni(page, indice);
    filas.push({
      ruta: await campoRutaClasspath(page).inputValue(),
      habilitado: await campoHabilitado(page).isChecked(),
    });
    await volverAlListado(page);
  }
  return filas;
}

test.describe('Certificados digitales', () => {
  test('Borrar uno de los certificados de una persona', async ({ page }) => {
    // El estado del aviso pendiente es de ESTA ejecución: al reintentar un test,
    // Playwright puede reutilizar el módulo ya cargado, así que se parte de cero.
    avisoPendiente = false;

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
      await altaCertificado(page, RUTA_SUPERVIVIENTE, true);
      await expect(filasDelDni(page)).toHaveCount(1);

      // Paso 3: Y pulsa «Añadir certificado digital», escribe en «DNI»
      //         «29050788V», elige el mismo tipo de certificado, escribe en «Ruta
      //         classpath» «firma/instalar_certificado_criptografico/secretario.p12»,
      //         DESMARCA la casilla «Habilitado» y pulsa «Guardar».
      await altaCertificado(page, RUTA_A_BORRAR, false);

      // Estado de partida del borrado: dos filas del DNI, la primera habilitada
      // (ordenación `dni,-enabled`) y la segunda no.
      await expect(filasDelDni(page)).toHaveCount(2);
      await expect(filasDelDni(page).nth(0).getByRole('checkbox')).toBeChecked();
      await expect(filasDelDni(page).nth(1).getByRole('checkbox')).not.toBeChecked();

      // Paso 4: Y pulsa la SEGUNDA fila del DNI «29050788V», que es la que tiene
      //         «Habilitado» sin marcar.
      //
      // CRITICAL — lo que este test tiene que demostrar es que se borra la fila
      // CORRECTA, así que la fila a borrar NO se elige solo por su posición: se
      // abren las dos y se identifica por su «Ruta classpath» (el único dato que
      // las distingue, y que no es columna del listado). Si el borrado se llevara
      // por delante la otra, un test que confiara en el índice seguiría en verde
      // (quedaría igualmente UNA fila) y estaría mintiendo.
      const filasAntesDeBorrar = await leerLasDosFilas(page);
      expect(filasAntesDeBorrar).toEqual([
        { ruta: RUTA_SUPERVIVIENTE, habilitado: true },
        { ruta: RUTA_A_BORRAR, habilitado: false },
      ]);

      const indiceABorrar = filasAntesDeBorrar.findIndex(
        (fila) => fila.ruta === RUTA_A_BORRAR && !fila.habilitado,
      );
      expect(indiceABorrar).toBeGreaterThanOrEqual(0);

      await abrirFilaDelDni(page, indiceABorrar);
      // Última verificación DENTRO del formulario ya abierto, justo antes de
      // pulsar «Borrar»: la fila que se va a borrar es la deshabilitada y su ruta
      // es la del segundo certificado. Es la garantía de que el borrado no cae
      // sobre el certificado equivocado.
      await expect(campoRutaClasspath(page)).toHaveValue(RUTA_A_BORRAR);
      await expect(campoHabilitado(page)).not.toBeChecked();

      // Paso 5: Y pulsa «Borrar».
      // Paso 6: Y confirma el borrado si el sistema lo pide.
      await borrarFilaAbierta(page);

      // Resultado esperado 1: el sistema borra ese certificado y vuelve al listado
      // «Certificados digitales».
      await expect(page).toHaveURL(/CertificadoDigital-action\/list/);
      await expect(botonAnhadir(page)).toBeVisible();

      // Resultado esperado 2 y 3: queda UNA SOLA fila con DNI «29050788V», con
      // «Habilitado» marcado, y al abrirla su «Ruta classpath» es
      // «firma/mi_certificado.p12» (la del primer certificado, el que NO se borró).
      //
      // CRITICAL: se comprueba tras una RECARGA EN DURO del navegador, no sobre la
      // rejilla que la SPA tenga en memoria. El borrado ocurre en el SERVIDOR, así
      // que lo que hay que demostrar es que llegó a la BASE DE DATOS; la recarga
      // obliga a releer el listado del servidor y descarta la posibilidad de que
      // la fila solo haya desaparecido de la vista.
      await page.goto(URL_LISTADO);
      await page.reload();
      // La recarga en duro se lleva por delante cualquier estado de la SPA, avisos
      // pendientes incluidos: a partir de aquí no se debe ninguno.
      avisoPendiente = false;
      await esperarListadoCargado(page);

      await expect(filasDelDni(page)).toHaveCount(1);
      await expect(filasDelDni(page).nth(0).getByRole('checkbox')).toBeChecked();

      // La ruta no es columna del listado: se lee DENTRO del formulario de la única
      // fila que queda, que es donde se demuestra que la superviviente es la del
      // paso 2 y no la del paso 3.
      await abrirFilaDelDni(page, 0);
      await expect(campoRutaClasspath(page)).toHaveValue(RUTA_SUPERVIVIENTE);
      await expect(campoHabilitado(page)).toBeChecked();
      await volverAlListado(page);
    } finally {
      try {
        // Teardown: el escenario deja UN certificado creado con ese DNI (el
        // habilitado que sobrevive al borrado), y ese impediría el alta del
        // siguiente run, así que MUST borrarse para que el test se pueda reejecutar
        // sin limpiar la BD a mano. Se borran TODAS las filas del DNI porque, si
        // una aserción falló antes del borrado, pueden quedar dos.
        // Best-effort para no enmascarar el fallo real de una aserción.
        await borrarEntradasDelDniSiExisten(page);
      } catch {
        // limpieza best-effort.
      }
    }

    await logout(page);
  });
});
