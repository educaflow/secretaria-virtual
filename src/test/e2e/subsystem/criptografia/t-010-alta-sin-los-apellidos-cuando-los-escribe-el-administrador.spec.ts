import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-010 — Alta sin los apellidos cuando los escribe el administrador
// origen: ESC-003  |  verifica: V-CertificadoDigital-002
// fuente: .sdd/drafts/2026-09-08_02-53_nombre-apellidos-certificado-digital/test-e2e-desc/t-010-alta-sin-los-apellidos-cuando-los-escribe-el-administrador.desc.md

// El DNI del escenario. NO lleva sufijo `Date.now()` a propósito (excepción
// documentada de la regla de nombres únicos): el escenario se apoya en que ESE
// DNI **no** corresponde a ningún usuario de la aplicación, para que el alta deje
// «Apellidos» vacío y editable y el rechazo venga de la validación de los
// apellidos obligatorios, no del autorrelleno; cambiarlo destruiría lo que el
// test verifica (y además es un DNI con letra de control, que un sufijo
// invalidaría). La idempotencia frente a la BD compartida (que NO se resetea) se
// consigue con la pre-limpieza defensiva del arranque + el teardown del
// `finally`, tal y como describe el «Estado inicial de la base de datos» de la
// descripción.
const DNI = '12345678Z';

// El nombre que teclea el administrador. Los «Apellidos» se dejan VACÍOS: es
// justo lo que el test verifica que la aplicación rechaza.
const NOMBRE = 'Ana';

// El título del enum llega a la UI con el sufijo `__!!` (marca de "no traducir"
// que el script de i18n elimina al generar los CSV, pero que el título crudo del
// dominio conserva). Los locators por nombre accesible hacen match por SUBCADENA,
// así que basta con el texto sin el sufijo.
const OPCION_CLASSPATH = 'Usar un fichero con el certificado que ya está dentro del del WAR';

const RUTA_CLASSPATH = 'firma/mi_certificado.p12';

// Texto del error que el escenario espera. En la UI el diálogo lo pinta como un
// ítem de lista «<campo>: <mensaje>», con el nombre del campo en negrita.
const CAMPO_EN_ERROR = 'apellidos:';
const MENSAJE_ERROR = 'Los apellidos son obligatorios';

const botonAnhadir = (page: Page) => page.getByRole('button', { name: 'Añadir certificado digital' });

// La fila del listado cuyo DNI es el del escenario (el nombre accesible de la
// fila es «<dni> <nombre> <apellidos> <tipo de certificado>», y el match por
// nombre es por subcadena).
const filaDelDni = (page: Page) => page.getByRole('row', { name: DNI });

// La fila que la rejilla pinta cuando no hay ningún registro.
const filaSinRegistros = (page: Page) => page.getByRole('row', { name: 'No records found.' });

// Diálogo de aviso de Axelor con un único botón de aceptar («Validation Error»
// de un alta que falló, o el «Current changes will be lost» que salta al
// abandonar un formulario que la SPA aún considera sucio). Mientras está abierto
// tapa la pantalla e intercepta cualquier clic, así que MUST cerrarse antes de
// tocar nada.
async function cerrarDialogoDeAviso(page: Page): Promise<void> {
  const ok = page.getByRole('dialog').getByRole('button', { name: /^(OK|Aceptar)$/ });
  if (await ok.isVisible().catch(() => false)) {
    await ok.click();
  }
}

// Barrera de carga del listado.
// CRITICAL: la rejilla pide sus filas en una petición aparte de la que pinta la
// vista, así que mirar las filas nada más aparecer el botón de alta es una
// condición de carrera — y esa carrera haría que la comprobación final («no se
// guardó ninguna fila») pasara en falso simplemente porque la rejilla aún no
// había pintado nada. Esperar a que la rejilla se resuelva en uno de sus dos
// estados posibles la elimina: o está la fila del DNI, o está la fila «No records
// found.» (esta tabla solo la escriben los tests de esta iniciativa y la
// anterior, y ambas limpian lo que crean; si apareciese otro estado, el test
// falla de forma ruidosa en vez de dar por buena una rejilla a medio cargar).
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

// Abre el formulario de la primera fila del DNI. El aviso «Current changes will
// be lost» puede aparecer de forma asíncrona justo al salir de un formulario e
// interceptar el clic, así que se cierra antes y se reintenta.
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
// Este test NO debe crear nada (el alta se rechaza), así que en condiciones
// normales no borra nada; se usa DOS veces igualmente: como pre-limpieza
// defensiva al arrancar (un run anterior de OTRO test del mismo DNI que abortase
// dejaría la fila, y entonces este alta moriría por DNI duplicado en vez de por
// los apellidos obligatorios, verificando algo distinto de lo que dice el
// escenario) y como teardown en el `finally` (si una regresión llegara a guardar
// la fila, el siguiente run no se envenena).
async function borrarEntradasDelDniSiExisten(page: Page): Promise<void> {
  await volverAlListado(page);

  // Bucle: un run anterior abortado pudo dejar más de una entrada con el mismo
  // DNI. LIMIT defensivo para no colgar el test si el borrado dejara de funcionar.
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
  test('Alta sin los apellidos cuando los escribe el administrador', async ({ page }) => {
    await ensureLoggedOut(page);
    // Precondición: el usuario `admin` ha iniciado sesión con «admin»/«admin».
    await login(page, 'admin', 'admin');

    try {
      // Paso 1: Dado que el administrador está en la pantalla «Certificados
      //         digitales» (menú «Administración SV» → «Certificados digitales»).
      await abrirCertificadosDigitales(page);

      // Precondición: no existe ningún certificado digital con DNI «12345678Z»
      // (si quedaran de una ejecución anterior, se borran desde el listado).
      await borrarEntradasDelDniSiExisten(page);

      // Paso 2: Cuando pulsa «Añadir certificado digital».
      await botonAnhadir(page).click();
      const dni = page.getByRole('textbox', { name: 'DNI', exact: true });
      await expect(dni).toBeVisible();

      // Paso 3: Y escribe en el campo «DNI» el valor «12345678Z».
      // El `Tab` saca el foco del campo: el autorrelleno de nombre y apellidos lo
      // dispara el evento de cambio, que Axelor emite al perder el foco, no al
      // teclear. MUST hacerse para que el escenario sea el real: como ese DNI no
      // es de ningún usuario, el autorrelleno no escribe nada y «Apellidos» queda
      // vacío y editable, que es la situación que el test necesita.
      await dni.fill(DNI);
      await dni.press('Tab');

      // Paso 4: Y elige en «Tipo de certificado» la opción «Usar un fichero con
      //         el certificado que ya está dentro del del WAR».
      await page.getByRole('combobox', { name: 'Tipo de certificado' }).click();
      await page.getByRole('option', { name: OPCION_CLASSPATH }).click();

      // Paso 5: Y escribe en el campo «Ruta classpath» el valor
      //         «firma/mi_certificado.p12». (El campo solo se muestra al elegir
      //         el tipo CLASSPATH; su etiqueta real es «Ruta classpath__!!».)
      await page.getByRole('textbox', { name: 'Ruta classpath' }).fill(RUTA_CLASSPATH);

      // Paso 6: Y escribe en «Nombre» el valor «Ana» y deja el campo «Apellidos»
      //         vacío. (El nombre accesible real es «Nombre ?» / «Apellidos ?»
      //         por el icono de ayuda; el match por subcadena lo cubre. Se
      //         comprueba que «Apellidos» está de verdad vacío antes de guardar:
      //         si el autorrelleno lo hubiera escrito, el alta no probaría el
      //         caso de error que describe el escenario.)
      await page.getByRole('textbox', { name: 'Nombre' }).fill(NOMBRE);
      const apellidos = page.getByRole('textbox', { name: 'Apellidos' });
      await expect(apellidos).toHaveValue('');

      // Paso 7: Y pulsa «Guardar».
      await page.getByRole('button', { name: 'Guardar' }).click();

      // Resultado esperado 1: el sistema muestra el error «Los apellidos son
      // obligatorios». Axelor lo presenta en un diálogo «Validation Error» con un
      // ítem por campo rechazado, «<campo>: <mensaje>».
      const dialogoError = page.getByRole('dialog');
      await expect(dialogoError).toBeVisible();
      await expect(dialogoError.getByRole('heading', { name: 'Validation Error' })).toBeVisible();
      await expect(dialogoError.getByRole('listitem').filter({ hasText: MENSAJE_ERROR })).toContainText(CAMPO_EN_ERROR);
      await expect(dialogoError).toContainText(MENSAJE_ERROR);
      // El «Nombre» sí se rellenó, así que el rechazo MUST venir solo de los
      // apellidos: si además saliera el error del nombre, el test no estaría
      // verificando el caso del escenario.
      await expect(dialogoError).not.toContainText('El nombre es obligatorio');

      // Resultado esperado 2 (parte 1): no se guarda nada — el alta ni siquiera
      // sale del formulario (la vista sigue en /edit, no vuelve a /list).
      await expect(page).toHaveURL(/CertificadoDigital-action\/edit/);

      // Cerrar el diálogo y abandonar el formulario (descartando los cambios)
      // para poder mirar el listado.
      await cerrarDialogoDeAviso(page);
      await volverAlListado(page);

      // Resultado esperado 2 (parte 2): el listado «Certificados digitales» no
      // muestra ninguna fila con DNI «12345678Z».
      // CRITICAL: se recarga la página en duro antes de mirar. La SPA de Axelor
      // sirve la rejilla desde su propio caché de la vista, así que sin recargar
      // el listado podría estar mostrando el estado que tenía ANTES del intento
      // de alta y la aserción no probaría nada sobre lo que hay en la BD.
      await page.reload();
      await esperarListadoCargado(page);
      await expect(filaDelDni(page)).toHaveCount(0);
    } finally {
      try {
        // Teardown: el escenario NO debe dejar nada creado (el alta se rechaza),
        // pero se limpia igualmente para que una regresión que sí guardase la
        // fila no envenenase los runs siguientes. Best-effort para no enmascarar
        // el fallo real de una aserción.
        await borrarEntradasDelDniSiExisten(page);
      } catch {
        // limpieza best-effort.
      }
    }

    await logout(page);
  });
});
