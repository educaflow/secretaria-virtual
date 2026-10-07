import { test, expect, Page } from '@playwright/test';
import * as path from 'path';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-002 — Deshabilitar una entrada
// origen: ESC-002  |  verifica: —
// fuente: .sdd/drafts/2026-08-10_23-21_deshabilitar-certificado-digital/test-e2e-desc/t-002-deshabilitar-una-entrada.desc.md

// El DNI del escenario. NO lleva sufijo `Date.now()` a propósito (excepción
// documentada de la regla de nombres únicos): el «Resultado esperado» exige
// literalmente la fila del DNI «94307933K», y el campo es un DNI con letra de
// control, así que un sufijo lo invalidaría. La idempotencia frente a la BD
// compartida (que NO se resetea) se consigue con la pre-limpieza defensiva del
// arranque + el teardown del `finally`, tal y como describe el «Estado inicial
// de la base de datos» de la descripción.
const DNI = '94307933K';

// La opción de «Tipo de certificado» que sube el fichero del certificado y lo
// guarda en la base de datos (tipo FICHERO_BD).
const OPCION_FICHERO_BD = 'Subir un fichero con el certificado para guardarlo en la base de datos';

// El certificado de test que se sube en el alta: lo emite la CA de demo y no es
// de ningún usuario. Vive en src/test/resources (no va dentro del WAR), por eso
// no puede darse de alta como tipo CLASSPATH y se sube como FICHERO_BD.
const NOMBRE_FICHERO_CERTIFICADO = 'test1.p12';
const FICHERO_CERTIFICADO = path.resolve(__dirname, '../../../resources/firma/test', NOMBRE_FICHERO_CERTIFICADO);
const CONTRASENA_CERTIFICADO = 'demo1234';

const botonAnhadir = (page: Page) => page.getByRole('button', { name: 'Añadir certificado digital' });

// La fila del listado cuyo DNI es el del escenario (el nombre accesible de la
// fila es «<dni> <tipo de certificado>», y el match por nombre es por subcadena).
// Se busca en el ÚLTIMO rowgroup (el de los datos): el primero lleva la cabecera
// y la fila de filtros, cuyo «Buscar...» de la columna DNI contiene el DNI
// tecleado (ver `filtrarPorDni`) y también casaría por nombre.
const filaDelDni = (page: Page) => page.getByRole('rowgroup').last().getByRole('row', { name: DNI });

// La fila que la rejilla pinta cuando no hay ningún registro.
const filaSinRegistros = (page: Page) => page.getByRole('row', { name: 'No se encontraron registros.' });

// Filtro de la columna «DNI» del listado (la fila de «Buscar...» de la cabecera).
// CRITICAL: el listado NO está vacío: los datos de demostración traen un
// certificado por cada usuario de demo y la rejilla pagina (40 filas por página),
// así que sin filtrar las filas del DNI del escenario pueden caer en otra página
// y la pre-limpieza, los recuentos y las posiciones no las verían. Filtrando por
// el DNI la rejilla solo muestra las suyas (o «No se encontraron registros.»).
// Axelor conserva el filtro al volver al listado tras guardar o borrar, pero no
// tras recargar la página: por eso se reaplica siempre que no esté puesto.
const filtroDni = (page: Page) => page.getByTestId('column:dni').getByPlaceholder('Buscar...');

async function filtrarPorDni(page: Page): Promise<void> {
  if ((await filtroDni(page).inputValue()) === DNI) {
    return;
  }
  await filtroDni(page).fill(DNI);
  // Se espera a la respuesta de la búsqueda: hasta entonces la rejilla sigue
  // mostrando la primera página sin filtrar.
  await Promise.all([
    page.waitForResponse(
      (respuesta) => respuesta.url().includes('CertificadoDigital/search') && respuesta.request().method() === 'POST',
    ),
    filtroDni(page).press('Enter'),
  ]);
}

// La casilla «Habilitado» del formulario. (El nombre accesible real es
// «Habilitado ?» por el icono de ayuda; el match por subcadena lo cubre.)
const casillaHabilitado = (page: Page) => page.getByRole('checkbox', { name: 'Habilitado' });

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
// El guardado navega directamente al listado. Históricamente Axelor superponía
// además el diálogo modal «Current changes will be lost» (el formulario que se
// abandonaba quedaba marcado como «sucio» al normalizar el servidor el registro),
// cuya capa modal interceptaba los clics posteriores sobre la rejilla. Ese diálogo
// ya no aparece en el flujo normal —los tests t-007 en adelante guardan sin él—,
// así que se confirma solo si surge y el fin del guardado se asierta contra la
// vuelta al listado, que es el efecto observable de verdad.
async function guardarYVolverAlListado(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Guardar' }).click();
  // El diálogo ya no aparece en el flujo normal de guardado (ver comentario arriba),
  // pero se confirma si apareciera: su capa modal interceptaría los clics siguientes.
  if (await okCambiosPerdidos(page).isVisible().catch(() => false)) {
    await okCambiosPerdidos(page).click();
    await expect(okCambiosPerdidos(page)).toBeHidden();
  }
  // Aserción real del fin del guardado: la vista pasa de /edit a /list y reaparece
  // el botón de alta, que solo existe en la lista.
  await expect(page).toHaveURL(/CertificadoDigital-action\/list/);
  await expect(botonAnhadir(page)).toBeVisible();
}

// Barrera de carga del listado.
// CRITICAL: la rejilla pide sus filas en una petición aparte de la que pinta la
// vista, así que mirar las filas nada más aparecer el botón de alta es una
// condición de carrera — y esa carrera haría que la pre-limpieza no viese la fila
// residual de un run anterior y el alta muriera con un error de DNI duplicado.
// Esperar a que la rejilla se resuelva en uno de sus dos estados posibles la
// elimina: o está la fila del DNI, o está la fila «No se encontraron registros.»
// (la rejilla está filtrada por el DNI del escenario, ver `filtrarPorDni`; si
// apareciese otro estado, el test falla de forma ruidosa en vez de saltarse la
// limpieza en silencio).
async function esperarListadoCargado(page: Page): Promise<void> {
  await expect(botonAnhadir(page)).toBeVisible();
  await filtrarPorDni(page);
  await expect(filaDelDni(page).or(filaSinRegistros(page)).first()).toBeVisible();
}

// Abre el listado «Certificados digitales» desde el menú «Criptografía».
// MUST llamarse solo con la pestaña aún cerrada: una vez abierta, el título de la
// pestaña repite el texto del ítem de menú y el locator por texto sería ambiguo.
async function abrirCertificadosDigitales(page: Page): Promise<void> {
  await page.getByText('Criptografía', { exact: true }).click();
  await page.getByText('Certificados digitales', { exact: true }).click();
  await esperarListadoCargado(page);
}

// Borra la entrada del DNI si sigue existiendo, partiendo del listado.
// Se usa DOS veces: como pre-limpieza defensiva al arrancar (un run anterior que
// abortó deja la entrada creada y el DNI es único en BD, así que sin esto el alta
// fallaría) y como teardown en el `finally`.
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
  // Diálogo de confirmación de Axelor; con el idioma del admin el botón de
  // confirmar aparece en inglés («Delete»). Se acota al diálogo para no chocar
  // con el botón «Borrar» del formulario que queda detrás.
  await page.getByRole('dialog').getByRole('button', { name: /Delete|Eliminar/ }).click();
  await expect(botonAnhadir(page)).toBeVisible();
  await expect(filaDelDni(page)).toHaveCount(0);
}

// Sube el certificado de test al campo «Fichero» (widget `binary-link`), que solo
// se muestra con el tipo FICHERO_BD. El widget esconde su `input[type=file]`:
// `setInputFiles` escribe en él directamente, y el botón con el nombre del fichero
// confirma que la subida terminó antes de seguir.
async function subirCertificado(page: Page): Promise<void> {
  const campoFichero = page.getByTestId('field:fichero');
  await campoFichero.locator('input[type="file"]').setInputFiles(FICHERO_CERTIFICADO);
  await expect(campoFichero.getByRole('button', { name: NOMBRE_FICHERO_CERTIFICADO })).toBeVisible();
}

test.describe('Certificados digitales', () => {
  test('Deshabilitar una entrada', async ({ page }) => {
    await ensureLoggedOut(page);
    // Precondición: el usuario `admin` ha iniciado sesión.
    await login(page, 'admin', 'admin');

    try {
      // Paso 1: Dado que el administrador está en la pantalla «Certificados
      //         digitales» (menú «Criptografía» → «Certificados digitales»).
      await abrirCertificadosDigitales(page);

      // Precondición: no existe ninguna entrada con el DNI «94307933K» (si la dejó
      // una ejecución anterior, se borra desde el listado).
      await borrarEntradaSiExiste(page);

      // Paso 2: Cuando pulsa «Añadir certificado digital», rellena «DNI» con
      //         «94307933K», elige en «Tipo de certificado» la opción «Subir un
      //         fichero con el certificado para guardarlo en la base de datos»,
      //         sube en «Fichero» el fichero «test1.p12», rellena «Nueva
      //         contraseña» con «demo1234», y pulsa «Guardar».
      await botonAnhadir(page).click();
      await page.getByRole('textbox', { name: 'DNI', exact: true }).fill(DNI);
      await page.getByRole('radio', { name: OPCION_FICHERO_BD }).check();
      // «Fichero» y «Nueva contraseña» solo se muestran al elegir el tipo FICHERO_BD.
      await subirCertificado(page);
      // Es un <input type="password"> (sin rol `textbox`): se localiza por su etiqueta.
      await page.getByLabel('Nueva contraseña').fill(CONTRASENA_CERTIFICADO);
      await guardarYVolverAlListado(page);

      // Paso 3: Entonces el sistema guarda la entrada y vuelve al listado (la vista
      //         pasa de /edit a /list y reaparece el botón de alta, que solo existe
      //         en la vista de lista).
      await expect(page).toHaveURL(/CertificadoDigital-action\/list/);
      await esperarListadoCargado(page);
      await expect(filaDelDni(page)).toBeVisible();

      // Paso 4: Cuando abre la fila del DNI «94307933K».
      await filaDelDni(page).first().click();
      const habilitado = casillaHabilitado(page);
      await expect(habilitado).toBeVisible();

      // Paso 5: Y desmarca la casilla «Habilitado».
      await habilitado.uncheck();
      await expect(habilitado).not.toBeChecked();

      // Paso 6: Y pulsa «Guardar».
      await guardarYVolverAlListado(page);

      // Resultado esperado 1: el sistema guarda el cambio y vuelve al listado
      // «Certificados digitales».
      await expect(page).toHaveURL(/CertificadoDigital-action\/list/);
      await expect(botonAnhadir(page)).toBeVisible();

      // Resultado esperado 2: el listado muestra la fila del DNI «94307933K» con la
      // columna «Habilitado» sin marcar (la celda de esa columna es la única casilla
      // de la fila: la rejilla no tiene columna de selección).
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
