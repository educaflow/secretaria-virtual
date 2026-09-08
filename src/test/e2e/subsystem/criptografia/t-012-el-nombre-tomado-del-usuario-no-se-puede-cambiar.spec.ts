import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-012 — El nombre tomado del usuario no se puede cambiar
// origen: ESC-005  |  verifica: U-certificados-digitales-003, U-certificados-digitales-004
// fuente: .sdd/drafts/2026-09-08_02-53_nombre-apellidos-certificado-digital/test-e2e-desc/t-012-el-nombre-tomado-del-usuario-no-se-puede-cambiar.desc.md

// El DNI del escenario: el del usuario `secretario@mislata.es` de los datos de
// demostración. NO lleva sufijo `Date.now()` a propósito (excepción documentada
// de la regla de nombres únicos): el escenario entero se apoya en que ESE DNI
// corresponde a un usuario de la aplicación, para que el alta copie su nombre y
// sus apellidos de la ficha y por eso queden bloqueados al reabrir el
// certificado — que es justo lo que este test verifica; cambiarlo lo destruiría
// (y además es un DNI con letra de control, que un sufijo invalidaría). La
// idempotencia frente a la BD compartida (que NO se resetea) se consigue con la
// pre-limpieza defensiva del arranque + el teardown del `finally`, tal y como
// describe el «Estado inicial de la base de datos» de la descripción.
const DNI = '29050788V';

// Nombre y apellidos que el alta MUST copiar de la ficha de ese usuario.
const NOMBRE = 'Secretario';
const APELLIDOS = 'CIPFP Mislata';

// El título del enum llega a la UI con el sufijo `__!!` (marca de "no traducir"
// que el script de i18n elimina al generar los CSV, pero que el título crudo del
// dominio conserva). Los locators por nombre accesible hacen match por SUBCADENA,
// así que basta con el texto sin el sufijo.
const OPCION_CLASSPATH = 'Usar un fichero con el certificado que ya está dentro del del WAR';

const RUTA_CLASSPATH = 'firma/mi_certificado.p12';

const botonAnhadir = (page: Page) => page.getByRole('button', { name: 'Añadir certificado digital' });

// La fila del listado cuyo DNI es el del escenario (el nombre accesible de la
// fila es «<dni> <nombre> <apellidos> <tipo de certificado>», y el match por
// nombre es por subcadena).
const filaDelDni = (page: Page) => page.getByRole('row', { name: DNI });

// La fila que la rejilla pinta cuando no hay ningún registro.
const filaSinRegistros = (page: Page) => page.getByRole('row', { name: 'No records found.' });

// --- Campos del formulario -------------------------------------------------
// Los tres campos de la persona titular: son los que el escenario exige ver de
// SOLO LECTURA (el «solo lectura» de Axelor se renderiza como input deshabilitado).
// El nombre accesible real de dos de ellos es «Nombre ?» / «Apellidos ?» por el
// icono de ayuda; el match por subcadena lo cubre. El `exact: true` del DNI evita
// que ese locator casase con otro campo que lo contuviera.
const campoDni = (page: Page) => page.getByRole('textbox', { name: 'DNI', exact: true });
const campoNombre = (page: Page) => page.getByRole('textbox', { name: 'Nombre' });
const campoApellidos = (page: Page) => page.getByRole('textbox', { name: 'Apellidos' });

// Los demás campos del formulario, que el escenario exige ver EDITABLES.
// «Ruta classpath» y «Contraseña» solo se muestran con el tipo CLASSPATH elegido;
// la etiqueta real de la ruta es «Ruta classpath__!!» (match por subcadena) y la
// del check es «Habilitado ?».
const campoTipoCertificado = (page: Page) => page.getByRole('combobox', { name: 'Tipo de certificado' });
const campoRutaClasspath = (page: Page) => page.getByRole('textbox', { name: 'Ruta classpath' });
const campoContrasena = (page: Page) => page.getByRole('textbox', { name: 'Contraseña' });
const campoHabilitado = (page: Page) => page.getByRole('checkbox', { name: 'Habilitado' });

// Diálogo de aviso de Axelor con un único botón de aceptar (el «Current changes
// will be lost» que salta al abandonar un formulario que la SPA aún considera
// sucio, o un «Validation Error»). Mientras está abierto tapa la pantalla e
// intercepta cualquier clic, así que MUST cerrarse antes de tocar nada.
async function cerrarDialogoDeAviso(page: Page): Promise<void> {
  const ok = page.getByRole('dialog').getByRole('button', { name: /^(OK|Aceptar)$/ });
  if (await ok.isVisible().catch(() => false)) {
    await ok.click();
  }
}

// Barrera de carga del listado.
// CRITICAL: la rejilla pide sus filas en una petición aparte de la que pinta la
// vista, así que mirar las filas nada más aparecer el botón de alta es una
// condición de carrera — y esa carrera haría que la pre-limpieza no viese la fila
// residual de un run anterior y el alta muriera con un error de DNI duplicado.
// Esperar a que la rejilla se resuelva en uno de sus dos estados posibles la
// elimina: o está la fila del DNI, o está la fila «No records found.» (esta tabla
// solo la escriben los tests de esta iniciativa y la anterior, y ambas limpian lo
// que crean; si apareciese otro estado, el test falla de forma ruidosa en vez de
// dar por buena una rejilla a medio cargar).
async function esperarListadoCargado(page: Page): Promise<void> {
  await expect(botonAnhadir(page)).toBeVisible();
  await expect(filaDelDni(page).or(filaSinRegistros(page)).first()).toBeVisible();
}

// Abre el listado «Certificados digitales» desde el menú «Administración SV».
// MUST llamarse solo con la pestaña aún cerrada: una vez abierta, el título de la
// pestaña repite el texto del ítem de menú y el locator por texto sería ambiguo.
async function abrirCertificadosDigitales(page: Page): Promise<void> {
  await page.getByText('Administración SV', { exact: true }).click();
  await page.getByText('Certificados digitales', { exact: true }).click();
  await esperarListadoCargado(page);
}

// Abre el formulario de la primera fila del DNI.
// CRITICAL: tras guardar, Axelor lanza de forma ASÍNCRONA el aviso «Current
// changes will be lost» sobre el listado; mientras está abierto intercepta el
// clic en la fila (comprobado contra la app real: el primer clic tras el alta
// muere interceptado por el botón «Cancel» del modal). `isVisible()` responde al
// instante y daría un falso negativo, así que en vez de preguntar una sola vez se
// cierra el diálogo, se intenta el clic con timeout corto y se reintenta.
async function abrirFilaDelDni(page: Page): Promise<void> {
  for (let intento = 0; intento < 3; intento++) {
    await cerrarDialogoDeAviso(page);
    try {
      await filaDelDni(page).first().click({ timeout: 5000 });
      await cerrarDialogoDeAviso(page);
      await expect(page.getByRole('button', { name: 'Borrar' })).toBeVisible({ timeout: 5000 });
      return;
    } catch {
      // El diálogo interceptó el clic: se cierra en la vuelta siguiente.
    }
  }
  throw new Error(`No se pudo abrir la fila del DNI ${DNI}`);
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

// Borra TODAS las entradas del DNI si siguen existiendo, partiendo de donde esté.
// Se usa DOS veces: como pre-limpieza defensiva al arrancar (este test CREA un
// certificado con ese DNI, así que una entrada residual de un run anterior
// abortado haría fallar el alta por DNI duplicado) y como teardown en el
// `finally` (el certificado que deja creado MUST borrarse para que el test se
// pueda reejecutar sin limpiar la BD a mano).
async function borrarEntradasDelDniSiExisten(page: Page): Promise<void> {
  await volverAlListado(page);

  // Bucle: el escenario deja una sola entrada, pero un run anterior abortado pudo
  // dejar más de una con el mismo DNI. LIMIT defensivo para no colgar el test si
  // el borrado dejara de funcionar.
  for (let i = 0; i < 5 && (await filaDelDni(page).count()) > 0; i++) {
    await abrirFilaDelDni(page);
    await page.getByRole('button', { name: 'Borrar' }).click();
    // Diálogo de confirmación de Axelor; con el idioma del admin el botón de
    // confirmar aparece en inglés («Delete»). Se acota al diálogo para no chocar
    // con el botón «Borrar» del formulario que queda detrás.
    await page.getByRole('dialog').getByRole('button', { name: /Delete|Eliminar/ }).click();
    await esperarListadoCargado(page);
  }
  await expect(filaDelDni(page)).toHaveCount(0);
}

test.describe('Certificados digitales', () => {
  test('El nombre tomado del usuario no se puede cambiar', async ({ page }) => {
    await ensureLoggedOut(page);
    // Precondición: el usuario `admin` ha iniciado sesión con «admin»/«admin».
    await login(page, 'admin', 'admin');

    try {
      // Paso 1: Dado que el administrador está en la pantalla «Certificados
      //         digitales» (menú «Administración SV» → «Certificados digitales»).
      await abrirCertificadosDigitales(page);

      // Precondición: no existe ningún certificado digital con DNI «29050788V»
      // (si quedaran de una ejecución anterior, se borran desde el listado).
      await borrarEntradasDelDniSiExisten(page);

      // Paso 2: Cuando pulsa «Añadir certificado digital», escribe en «DNI»
      //         «29050788V», elige en «Tipo de certificado» la opción «Usar un
      //         fichero con el certificado que ya está dentro del del WAR»,
      //         escribe en «Ruta classpath» «firma/mi_certificado.p12» y pulsa
      //         «Guardar».
      await botonAnhadir(page).click();
      const dniAlta = campoDni(page);
      await expect(dniAlta).toBeVisible();

      // El `Tab` saca el foco del campo: el autorrelleno de nombre y apellidos lo
      // dispara el evento de cambio, que Axelor emite al perder el foco, no al
      // teclear.
      //
      // CRITICAL: ese autorrelleno es una llamada AL SERVIDOR (`POST /ws/action`
      // con la acción `...CertificadoDigital-onChange-dni-action`), y su respuesta
      // ESCRIBE en «Nombre» y «Apellidos» — con este DNI, copiando los de la ficha
      // del usuario `secretario@mislata.es`. Sin esperar a que termine, seguir
      // adelante sería una condición de carrera: el alta podría llegar al servidor
      // con el nombre aún vacío y morir con «El nombre es obligatorio».
      await dniAlta.fill(DNI);
      await Promise.all([
        page.waitForResponse(
          (respuesta) =>
            respuesta.url().includes('/ws/action') &&
            respuesta.request().method() === 'POST' &&
            (respuesta.request().postData() ?? '').includes('onChange-dni-action'),
        ),
        dniAlta.press('Tab'),
      ]);

      // Guarda de precondición: el escenario exige que el nombre y los apellidos
      // los ponga el AUTORRELLENO desde la ficha del usuario (el administrador no
      // los teclea). Si no fuera así, lo que este test comprueba después —que no
      // se pueden cambiar— no probaría nada; se verifica aquí para fallar con un
      // mensaje claro.
      await expect(campoNombre(page)).toHaveValue(NOMBRE);
      await expect(campoApellidos(page)).toHaveValue(APELLIDOS);

      await campoTipoCertificado(page).click();
      await page.getByRole('option', { name: OPCION_CLASSPATH }).click();

      // El campo solo se muestra al elegir el tipo CLASSPATH; su etiqueta real es
      // «Ruta classpath__!!».
      await campoRutaClasspath(page).fill(RUTA_CLASSPATH);

      await page.getByRole('button', { name: 'Guardar' }).click();

      // Paso 3: Entonces el sistema guarda el certificado y vuelve al listado (la
      //         vista pasa de /edit a /list y reaparece el botón de alta, que solo
      //         existe en la vista de lista).
      await expect(page).toHaveURL(/CertificadoDigital-action\/list/);
      await esperarListadoCargado(page);
      await expect(filaDelDni(page)).toHaveCount(1);

      // Paso 4: Cuando pulsa la fila del DNI «29050788V».
      await abrirFilaDelDni(page);

      // Resultado esperado 1: el formulario muestra el campo «DNI» con
      // «29050788V», el campo «Nombre» con «Secretario» y el campo «Apellidos»
      // con «CIPFP Mislata», LOS TRES DE SOLO LECTURA. (El «solo lectura» de
      // Axelor se renderiza como input deshabilitado.) Se comprueban también los
      // valores: si el formulario abierto no fuera el del certificado recién
      // creado, las aserciones no probarían nada.
      await expect(campoDni(page)).toHaveValue(DNI);
      await expect(campoNombre(page)).toHaveValue(NOMBRE);
      await expect(campoApellidos(page)).toHaveValue(APELLIDOS);
      await expect(campoDni(page)).toBeDisabled();
      await expect(campoNombre(page)).toBeDisabled();
      await expect(campoApellidos(page)).toBeDisabled();

      // Resultado esperado 2: los demás campos del formulario («Tipo de
      // certificado», «Ruta classpath», «Contraseña», «Habilitado») siguen siendo
      // editables — es decir, el bloqueo alcanza SOLO a los tres campos de la
      // persona titular y no ha dejado el formulario entero de solo lectura.
      await expect(campoTipoCertificado(page)).toBeEnabled();
      await expect(campoRutaClasspath(page)).toBeEnabled();
      await expect(campoContrasena(page)).toBeEnabled();
      await expect(campoHabilitado(page)).toBeEnabled();
    } finally {
      try {
        // Teardown: el escenario deja el certificado creado, y su DNI no puede
        // repetirse habilitado, así que MUST borrarse para que el test se pueda
        // reejecutar sin limpiar la BD a mano. Best-effort para no enmascarar el
        // fallo real de una aserción.
        await borrarEntradasDelDniSiExisten(page);
      } catch {
        // limpieza best-effort.
      }
    }

    await logout(page);
  });
});
