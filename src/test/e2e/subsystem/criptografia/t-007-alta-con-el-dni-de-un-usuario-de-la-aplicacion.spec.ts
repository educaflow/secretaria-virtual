import { test, expect, Page } from '@playwright/test';
import path from 'path';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-007 — Alta con el DNI de un usuario de la aplicación
// origen: ESC-001  |  verifica: R-CertificadoDigital-001, U-certificados-digitales-001, U-certificados-digitales-007
// fuente: .sdd/drafts/2026-09-08_02-53_nombre-apellidos-certificado-digital/test-e2e-desc/t-007-alta-con-el-dni-de-un-usuario-de-la-aplicacion.desc.md

// El DNI del escenario: el del usuario `sincertificado2@mislata.es` de los datos de
// demostración. NO lleva sufijo `Date.now()` a propósito (excepción documentada
// de la regla de nombres únicos): el escenario entero se apoya en que ESE DNI
// corresponde a un usuario de la aplicación para que el alta copie su nombre y
// sus apellidos, así que cambiarlo destruiría lo que el test verifica (y además
// es un DNI con letra de control, que un sufijo invalidaría). La idempotencia
// frente a la BD compartida (que NO se resetea) se consigue con la pre-limpieza
// defensiva del arranque + el teardown del `finally`, tal y como describe el
// «Estado inicial de la base de datos» de la descripción.
const DNI = '96931712Y';

// Nombre y apellidos que el alta MUST copiar de la ficha de ese usuario.
const NOMBRE = 'SinCertificado2';
const APELLIDOS = 'CIPFP Mislata';

// El título de la opción FICHERO_BD del enum «Tipo de certificado». Los
// certificados de test NO están dentro del WAR, así que no pueden usar el tipo
// que lee el fichero del classpath: se suben y se guardan en la base de datos.
const OPCION_FICHERO_BD = 'Subir un fichero con el certificado para guardarlo en la base de datos';

// Ruta absoluta de un certificado de test (`src/test/resources/firma/test/`).
// Son certificados de la CA de demo que no son de ningún usuario; su contraseña
// es `demo1234`, pero el alta no la exige y el escenario no la escribe.
const rutaCertificadoTest = (nombre: string) => path.resolve(__dirname, '../../../resources/firma/test', nombre);

const FICHERO_CERTIFICADO = 'test1.p12';

const botonAnhadir = (page: Page) => page.getByRole('button', { name: 'Añadir certificado digital' });

// La fila del listado cuyo DNI es el del escenario (el nombre accesible de la
// fila es «<dni> <nombre> <apellidos> <tipo de certificado>», y el match por
// nombre es por subcadena).
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

// Barrera de carga del listado.
// CRITICAL: la rejilla pide sus filas en una petición aparte de la que pinta la
// vista, así que mirar las filas nada más aparecer el botón de alta es una
// condición de carrera — y esa carrera haría que la pre-limpieza no viese la fila
// residual de un run anterior y el alta muriera con un error de DNI duplicado.
// Esperar a que la rejilla se resuelva en uno de sus dos estados posibles la
// elimina: o está la fila del DNI, o está la fila «No records found.» (la rejilla está filtrada por el DNI del escenario, ver `filtrarPorDni`;
// si apareciese otro estado, el test falla de forma ruidosa en vez de
// saltarse la limpieza en silencio).
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

// Borra TODAS las entradas del DNI si siguen existiendo, partiendo del listado.
// Se usa DOS veces: como pre-limpieza defensiva al arrancar (un run anterior que
// abortó deja la entrada creada y el alta con ese DNI volvería a fallar) y como
// teardown en el `finally`.
async function borrarEntradasDelDniSiExisten(page: Page): Promise<void> {
  // Un diálogo abierto (p.ej. el «Validation Error» de un alta que falló) tapa la
  // pantalla y bloquea cualquier clic: ciérralo antes de nada.
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

  // Bucle: el escenario deja una sola entrada, pero un run anterior abortado pudo
  // dejar más de una con el mismo DNI. LIMIT defensivo para no colgar el test si
  // el borrado dejara de funcionar.
  for (let i = 0; i < 5 && (await filaDelDni(page).count()) > 0; i++) {
    await filaDelDni(page).first().click();
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
  test('Alta con el DNI de un usuario de la aplicación', async ({ page }) => {
    await ensureLoggedOut(page);
    // Precondición: el usuario `admin` ha iniciado sesión con «admin»/«admin».
    await login(page, 'admin', 'admin');

    try {
      // Paso 1: Dado que el administrador está en la pantalla «Certificados
      //         digitales» (menú «Criptografía» → «Certificados digitales»).
      await abrirCertificadosDigitales(page);

      // Precondición: no existe ningún certificado digital con DNI «96931712Y»
      // (si quedaran de una ejecución anterior, se borran desde el listado).
      await borrarEntradasDelDniSiExisten(page);

      // Paso 2: Cuando pulsa «Añadir certificado digital».
      await botonAnhadir(page).click();
      const dni = page.getByRole('textbox', { name: 'DNI', exact: true });
      await expect(dni).toBeVisible();

      // Paso 3: Y escribe en el campo «DNI» el valor «96931712Y».
      // El `Tab` saca el foco del campo: el autorrelleno de nombre y apellidos lo
      // dispara el evento de cambio, que Axelor emite al perder el foco, no al
      // teclear.
      await dni.fill(DNI);
      await dni.press('Tab');

      // Paso 4: Entonces el campo «Nombre» muestra «SinCertificado2» y el campo
      //         «Apellidos» muestra «CIPFP Mislata», y los dos están de solo
      //         lectura. (El nombre accesible real es «Nombre ?» / «Apellidos ?»
      //         por el icono de ayuda; el match por subcadena lo cubre. El «solo
      //         lectura» de Axelor se renderiza como input deshabilitado.)
      const nombre = page.getByRole('textbox', { name: 'Nombre' });
      const apellidos = page.getByRole('textbox', { name: 'Apellidos' });
      await expect(nombre).toHaveValue(NOMBRE);
      await expect(apellidos).toHaveValue(APELLIDOS);
      await expect(nombre).toBeDisabled();
      await expect(apellidos).toBeDisabled();

      // Paso 5: Cuando elige en «Tipo de certificado» la opción «Subir un fichero
      //         con el certificado para guardarlo en la base de datos».
      // («Tipo de certificado» es un grupo de radios, `widget="RadioSelect"`.)
      await page.getByRole('radio', { name: OPCION_FICHERO_BD }).check();

      // Paso 6: Y sube en el campo «Fichero» el fichero «test1.p12». (El campo
      //         solo se muestra al elegir el tipo FICHERO_BD; es un widget
      //         `binary-link` que esconde su `input[type=file]` y, al terminar la
      //         subida, pinta el nombre del fichero en un botón.)
      const campoFichero = page.getByTestId('field:fichero');
      await campoFichero.locator('input[type="file"]').setInputFiles(rutaCertificadoTest(FICHERO_CERTIFICADO));
      await expect(campoFichero.getByRole('button', { name: FICHERO_CERTIFICADO })).toBeVisible();

      // Paso 7: Y pulsa «Guardar».
      await page.getByRole('button', { name: 'Guardar' }).click();

      // Resultado esperado 1: el sistema guarda el certificado y vuelve al listado
      // «Certificados digitales» (la vista pasa de /edit a /list y reaparece el
      // botón de alta, que solo existe en la vista de lista).
      await expect(page).toHaveURL(/CertificadoDigital-action\/list/);
      await expect(botonAnhadir(page)).toBeVisible();

      // Resultado esperado 2: el listado muestra una fila con DNI «96931712Y»,
      // «Nombre» «SinCertificado2», «Apellidos» «CIPFP Mislata», «Tipo de certificado»
      // «Subir un fichero con el certificado para guardarlo en la base de datos» y
      // «Habilitado» marcado (la celda de esa columna es la única casilla de la
      // fila: la rejilla no tiene columna de selección).
      const fila = filaDelDni(page);
      await expect(fila).toBeVisible();
      await expect(fila.getByRole('gridcell', { name: DNI, exact: true })).toBeVisible();
      await expect(fila.getByRole('gridcell', { name: NOMBRE, exact: true })).toBeVisible();
      await expect(fila.getByRole('gridcell', { name: APELLIDOS, exact: true })).toBeVisible();
      await expect(fila.getByRole('gridcell', { name: OPCION_FICHERO_BD })).toBeVisible();
      await expect(fila.getByRole('checkbox')).toBeChecked();
    } finally {
      try {
        // Teardown: el alta deja el certificado creado, y su DNI no puede
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
