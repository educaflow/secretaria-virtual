import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-006 — Borrar una entrada deshabilitada sigue funcionando
// origen: ESC-006  |  verifica: —
// fuente: .sdd/drafts/2026-08-10_23-21_deshabilitar-certificado-digital/test-e2e-desc/t-006-borrar-una-entrada-deshabilitada-sigue-funcionando.desc.md

// El DNI del escenario. NO lleva sufijo `Date.now()` a propósito (excepción
// documentada de la regla de nombres únicos): el «Resultado esperado» exige
// literalmente que el listado ya no muestre la fila del DNI «85432016B», y el
// campo es un DNI con letra de control, así que un sufijo lo invalidaría. La
// idempotencia frente a la BD compartida (que NO se resetea) se consigue con la
// pre-limpieza defensiva del arranque + el teardown del `finally`, tal y como
// describe el «Estado inicial de la base de datos» de la descripción.
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

// El botón de confirmar del diálogo de borrado de Axelor; con el idioma del admin
// aparece en inglés («Delete»). Se acota al diálogo para no chocar con el botón
// «Borrar» del formulario que queda detrás.
const confirmarBorrado = (page: Page) =>
  page.getByRole('dialog').getByRole('button', { name: /Delete|Eliminar/ });

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
// NOTA: en ESTE test el borrado es además el objeto del escenario, así que los
// pasos 6-9 se escriben INLINE en el cuerpo del test (con su aserción de que el
// sistema pide confirmación) en vez de delegar aquí: el teardown no puede hacer
// de aserción del propio comportamiento que el test verifica.
async function borrarEntradaSiExiste(page: Page): Promise<void> {
  // Un diálogo abierto (p.ej. el «Validation Error» de un alta que falló) tapa la
  // pantalla y bloquea cualquier clic: ciérralo antes de nada.
  const okDialogo = page.getByRole('dialog').getByRole('button', { name: /^(OK|Aceptar)$/ });
  if (await okDialogo.isVisible().catch(() => false)) {
    await okDialogo.click();
  }

  // Si quedó un formulario abierto (p.ej. el test falló a mitad del alta o de la
  // edición), vuelve al listado antes de buscar la fila.
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
  await confirmarBorrado(page).click();
  await expect(botonAnhadir(page)).toBeVisible();
  await expect(filaDelDni(page)).toHaveCount(0);
}

test.describe('Certificados digitales', () => {
  test('Borrar una entrada deshabilitada sigue funcionando', async ({ page }) => {
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

      // Paso 2: Cuando pulsa «Añadir certificado digital», rellena «DNI» con
      //         «85432016B», elige en «Tipo de certificado» la opción «Usar un
      //         fichero con el certificado que ya está dentro del del WAR»,
      //         rellena «Ruta classpath» con «firma/mi_certificado.p12» y
      //         «Contraseña» con «nadanada», y pulsa «Guardar».
      await botonAnhadir(page).click();
      await page.getByRole('textbox', { name: 'DNI', exact: true }).fill(DNI);
      await page.getByRole('combobox', { name: 'Tipo de certificado' }).click();
      await page.getByRole('option', { name: OPCION_CLASSPATH }).click();
      // «Ruta classpath» y «Contraseña» solo se muestran al elegir el tipo CLASSPATH.
      await page.getByRole('textbox', { name: 'Ruta classpath' }).fill('firma/mi_certificado.p12');
      // `exact` separa este campo del «Contraseña» de otros widgets y del login.
      await page.getByRole('textbox', { name: 'Contraseña', exact: true }).fill('nadanada');
      await guardarYVolverAlListado(page);

      // Paso 3: Entonces el sistema guarda la entrada y vuelve al listado (la vista
      //         pasa de /edit a /list y reaparece el botón de alta, que solo existe
      //         en la vista de lista).
      await expect(page).toHaveURL(/CertificadoDigital-action\/list/);
      await esperarListadoCargado(page);
      await expect(filaDelDni(page)).toBeVisible();

      // Paso 4: Cuando abre la fila del DNI «85432016B», desmarca la casilla
      //         «Habilitado» y pulsa «Guardar». La casilla viene marcada del alta, y
      //         se comprueba ANTES de tocarla: `uncheck()` sobre una casilla ya
      //         desmarcada es un no-op silencioso, así que sin esta aserción el
      //         «desmarcar» podría no estar cambiando nada y el test pasaría igual
      //         aunque la entrada llegase habilitada al borrado.
      await filaDelDni(page).first().click();
      const habilitado = casillaHabilitado(page);
      await expect(habilitado).toBeVisible();
      await expect(habilitado).toBeChecked();
      await habilitado.uncheck();
      await expect(habilitado).not.toBeChecked();
      await guardarYVolverAlListado(page);

      // Paso 5: Entonces el sistema guarda el cambio y vuelve al listado.
      await expect(page).toHaveURL(/CertificadoDigital-action\/list/);
      await esperarListadoCargado(page);
      await expect(filaDelDni(page)).toBeVisible();

      // Paso 6: Cuando abre la fila del DNI «85432016B».
      await filaDelDni(page).first().click();
      const habilitadoAntesDeBorrar = casillaHabilitado(page);
      await expect(habilitadoAntesDeBorrar).toBeVisible();
      // La entrada que se va a borrar MUST estar deshabilitada: es la premisa del
      // escenario (borrar una entrada DESHABILITADA). Releído del servidor, así que
      // confirma que el deshabilitado del paso 4 quedó persistido.
      await expect(habilitadoAntesDeBorrar).not.toBeChecked();

      // Paso 7: Y pulsa «Borrar».
      await page.getByRole('button', { name: 'Borrar' }).click();

      // Paso 8: Entonces el sistema pide confirmar el borrado.
      await expect(confirmarBorrado(page)).toBeVisible();

      // Paso 9: Cuando el administrador confirma.
      await confirmarBorrado(page).click();

      // Resultado esperado 1: el sistema borra la entrada y vuelve al listado
      // «Certificados digitales».
      await expect(page).toHaveURL(/CertificadoDigital-action\/list/);
      await expect(botonAnhadir(page)).toBeVisible();

      // Resultado esperado 2: el listado ya no muestra ninguna fila con el DNI
      // «85432016B». Se espera antes a que la rejilla haya resuelto su carga (si no,
      // contar cero filas sería trivialmente cierto sobre una rejilla aún vacía).
      await esperarListadoCargado(page);
      await expect(filaDelDni(page)).toHaveCount(0);
    } finally {
      try {
        // Teardown: si el test llegó al final, el borrado del escenario ya dejó la
        // BD limpia y esto es un no-op; si falló a mitad, borra la entrada que dejó
        // el alta para que el test se pueda reejecutar sin limpiar la BD a mano.
        // Best-effort para no enmascarar el fallo real de una aserción.
        await borrarEntradaSiExiste(page);
      } catch {
        // limpieza best-effort.
      }
    }

    await logout(page);
  });
});
