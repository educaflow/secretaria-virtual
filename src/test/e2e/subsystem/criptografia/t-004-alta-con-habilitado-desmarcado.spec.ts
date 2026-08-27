import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-004 — Alta con «Habilitado» desmarcado
// origen: ESC-004  |  verifica: —
// fuente: .sdd/drafts/2026-08-10_23-21_deshabilitar-certificado-digital/test-e2e-desc/t-004-alta-con-habilitado-desmarcado.desc.md

// El DNI del escenario. NO lleva sufijo `Date.now()` a propósito (excepción
// documentada de la regla de nombres únicos): el «Resultado esperado» exige
// literalmente la fila del DNI «85432016B», y el campo es un DNI con letra de
// control, así que un sufijo lo invalidaría. La idempotencia frente a la BD
// compartida (que NO se resetea) se consigue con la pre-limpieza defensiva del
// arranque + el teardown del `finally`, tal y como describe el «Estado inicial
// de la base de datos» de la descripción.
const DNI = '85432016B';

// El título del enum llega a la UI con el sufijo `__!!` (marca de "no traducir"
// que el script de i18n elimina al generar los CSV, pero que el título crudo del
// dominio conserva). Los locators por nombre accesible hacen match por SUBCADENA,
// así que basta con el texto sin el sufijo.
const OPCION_CLASSPATH = 'Usar un fichero con el certificado que ya está dentro del del WAR';

const botonAnhadir = (page: Page) => page.getByRole('button', { name: 'Añadir certificado digital' });

// La fila del listado cuyo DNI es el del escenario (el nombre accesible de la
// fila es «<dni> <tipo de certificado>», y el match por nombre es por subcadena).
const filaDelDni = (page: Page) => page.getByRole('row', { name: DNI });

// La fila que la rejilla pinta cuando no hay ningún registro.
const filaSinRegistros = (page: Page) => page.getByRole('row', { name: 'No records found.' });

// La casilla «Habilitado» del formulario. (El nombre accesible real es
// «Habilitado ?» por el icono de ayuda; el match por subcadena lo cubre.)
const casillaHabilitado = (page: Page) => page.getByRole('checkbox', { name: 'Habilitado' });

// El campo «Contraseña» del formulario. `exact` es obligatorio: sin él chocaría
// con «Repetir contraseña» y con el campo de contraseña del login.
const campoContrasena = (page: Page) => page.getByRole('textbox', { name: 'Contraseña', exact: true });

// El botón «OK» del diálogo que Axelor levanta al abandonar el formulario tras
// guardar («Question — Current changes will be lost. Do you really want to
// proceed?», en inglés con el idioma del admin). Se acota por el texto del
// diálogo para no confundirlo con el de confirmación del borrado.
const okCambiosPerdidos = (page: Page) =>
  page
    .getByRole('dialog')
    .filter({ hasText: /Current changes will be lost|Los cambios actuales se perderán/ })
    .getByRole('button', { name: /^(OK|Aceptar)$/ });

// Pulsa «Guardar» y deja la aplicación de vuelta en el listado, utilizable.
// CRITICAL: al guardar, la aplicación graba el registro y navega al listado, pero
// el formulario que abandona sigue marcado como «sucio» (el servidor normaliza el
// registro al grabarlo), así que Axelor superpone el diálogo modal «Current changes
// will be lost». El registro YA está guardado — el diálogo solo pregunta por el
// formulario que se abandona—, pero su capa modal intercepta cualquier clic sobre
// la rejilla: sin confirmarlo, el clic sobre la fila del DNI se queda esperando
// hasta el timeout. Aparece tras CADA guardado (alta y edición), así que el
// `click()` sobre el «OK» —que auto-espera— también sirve de aserción de que el
// guardado ha llegado a su fin.
async function guardarYVolverAlListado(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Guardar' }).click();
  await okCambiosPerdidos(page).click();
  await expect(okCambiosPerdidos(page)).toBeHidden();
}

// Barrera de carga del listado.
// CRITICAL: la rejilla pide sus filas en una petición aparte de la que pinta la
// vista, así que contar filas nada más aparecer el botón de alta es una condición
// de carrera — y esa carrera hacía que la pre-limpieza no viese la fila residual de
// un run anterior y el alta muriera con «Ya existe un certificado digital con el
// DNI '85432016B'». Esperar a que la rejilla se resuelva en uno de sus dos estados
// posibles la elimina: o está la fila del DNI, o está la fila «No records found.»
// (esta tabla solo la escriben los tests de esta iniciativa y el seed la deja
// vacía, así que no hay más estados posibles; si apareciese otro, el test falla de
// forma ruidosa en vez de saltarse la limpieza en silencio).
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

// Borra la entrada del DNI si sigue existiendo, partiendo del listado.
// Se usa DOS veces: como pre-limpieza defensiva al arrancar (un run anterior que
// abortó deja la entrada creada y el DNI es único en BD, así que sin esto el alta
// fallaría) y como teardown en el `finally`.
async function borrarEntradaSiExiste(page: Page): Promise<void> {
  // Un diálogo abierto (p.ej. el «Validation Error» de un alta que falló, o el
  // «Current changes will be lost» de un guardado) tapa la pantalla y bloquea
  // cualquier clic: ciérralo antes de nada.
  const okDialogo = page.getByRole('dialog').getByRole('button', { name: /^(OK|Aceptar)$/ });
  if (await okDialogo.isVisible().catch(() => false)) {
    await okDialogo.click();
  }

  // Si quedó un formulario abierto (p.ej. el test falló a mitad del alta), vuelve
  // al listado antes de buscar la fila.
  const cancelar = page.getByRole('button', { name: 'Cancelar' });
  if (await cancelar.isVisible().catch(() => false)) {
    await cancelar.click();
    // Axelor pregunta antes de descartar un formulario con cambios sin guardar.
    const confirmar = page.getByRole('dialog').getByRole('button', { name: /^(OK|Aceptar|Yes|Sí)$/ });
    if (await confirmar.isVisible().catch(() => false)) {
      await confirmar.click();
    }
  }
  await esperarListadoCargado(page);

  if ((await filaDelDni(page).count()) === 0) return;

  await filaDelDni(page).first().click();
  await page.getByRole('button', { name: 'Borrar' }).click();
  // Diálogo de confirmación de Axelor; con el idioma del admin el botón de
  // confirmar aparece en inglés («Delete»). Se acota al diálogo para no chocar
  // con el botón «Borrar» del formulario que queda detrás.
  await page.getByRole('dialog').getByRole('button', { name: /Delete|Eliminar/ }).click();
  await expect(botonAnhadir(page)).toBeVisible();
  await expect(filaDelDni(page)).toHaveCount(0);
}

test.describe('Certificados digitales', () => {
  test('Alta con «Habilitado» desmarcado', async ({ page }) => {
    await ensureLoggedOut(page);
    // Precondición: el usuario `admin` ha iniciado sesión.
    await login(page, 'admin', 'admin');

    try {
      // Paso 1: Dado que el administrador está en la pantalla «Certificados
      //         digitales» (menú «Administración SV» → «Certificados digitales»).
      await abrirCertificadosDigitales(page);

      // Precondición: no existe ninguna entrada con el DNI «85432016B» (si la dejó
      // una ejecución anterior, se borra desde el listado).
      await borrarEntradaSiExiste(page);

      // Paso 2: Cuando pulsa «Añadir certificado digital».
      await botonAnhadir(page).click();

      // Paso 3: Y rellena «DNI» con «85432016B», elige en «Tipo de certificado» la
      //         opción «Usar un fichero con el certificado que ya está dentro del
      //         del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12»
      //         y «Contraseña» con «nadanada».
      await page.getByRole('textbox', { name: 'DNI', exact: true }).fill(DNI);
      await page.getByRole('combobox', { name: 'Tipo de certificado' }).click();
      await page.getByRole('option', { name: OPCION_CLASSPATH }).click();
      // «Ruta classpath» y «Contraseña» solo se muestran al elegir el tipo CLASSPATH.
      await page.getByRole('textbox', { name: 'Ruta classpath' }).fill('firma/mi_certificado.p12');
      await campoContrasena(page).fill('nadanada');

      // Paso 4: Y desmarca la casilla «Habilitado». En el alta viene marcada por
      //         defecto, y se comprueba antes de tocarla: `uncheck()` sobre una
      //         casilla ya desmarcada es un no-op silencioso, así que sin esta
      //         aserción el test pasaría igual aunque el «desmarcar» del escenario
      //         no estuviera cambiando nada.
      const habilitado = casillaHabilitado(page);
      await expect(habilitado).toBeVisible();
      await expect(habilitado).toBeChecked();
      await habilitado.uncheck();
      await expect(habilitado).not.toBeChecked();

      // Paso 5: Y pulsa «Guardar».
      await guardarYVolverAlListado(page);

      // Resultado esperado 1: el sistema guarda la entrada y vuelve al listado
      // «Certificados digitales» (la vista pasa de /edit a /list y reaparece el
      // botón de alta, que solo existe en la vista de lista).
      await expect(page).toHaveURL(/CertificadoDigital-action\/list/);
      await expect(botonAnhadir(page)).toBeVisible();

      // Resultado esperado 2: el listado muestra la fila del DNI «85432016B» con la
      // columna «Habilitado» sin marcar (la celda de esa columna es la única casilla
      // de la fila: la rejilla no tiene columna de selección).
      await esperarListadoCargado(page);
      const fila = filaDelDni(page);
      await expect(fila).toBeVisible();
      await expect(fila.getByRole('checkbox')).not.toBeChecked();
    } finally {
      try {
        // Teardown: el alta deja la entrada creada, y su DNI es único en BD, así
        // que MUST borrarse para que el test se pueda reejecutar sin limpiar la
        // BD a mano. Best-effort para no enmascarar el fallo real de una aserción.
        await borrarEntradaSiExiste(page);
      } catch {
        // limpieza best-effort.
      }
    }

    await logout(page);
  });
});
