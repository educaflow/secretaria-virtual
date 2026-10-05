import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-001 — Alta con «Habilitado» marcado por defecto
// origen: ESC-001  |  verifica: U-certificados-digitales-001
// fuente: .sdd/drafts/2026-08-10_23-21_deshabilitar-certificado-digital/test-e2e-desc/t-001-alta-con-habilitado-marcado-por-defecto.desc.md

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

// Cualquier fila del CUERPO de la rejilla, sea del DNI del escenario, de datos
// ajenos (residuos reales que puede haber en la BD compartida — la tabla NO es
// exclusiva de estos tests) o la fila «No se encontraron registros.». El grid
// de Axelor tiene exactamente dos <div role="rowgroup">: el primero es la
// cabecera (títulos + fila de filtros «Buscar...»), el segundo es el cuerpo con
// los datos. Da igual el contenido: basta con que exista alguna fila ahí.
const filaCuerpoRejilla = (page: Page) => page.getByRole('grid').getByRole('rowgroup').nth(1).getByRole('row');

// Barrera de carga del listado.
// CRITICAL: la rejilla pide sus filas en una petición aparte de la que pinta la
// vista, así que contar filas nada más aparecer el botón de alta es una condición
// de carrera — y esa carrera hacía que la pre-limpieza no viese la fila residual de
// un run anterior y el alta muriera con «Ya existe un certificado digital con el
// DNI '85432016B'». Esperar a que el CUERPO de la rejilla pinte al menos una fila
// (cualquiera: del DNI del escenario, ajena, o el mensaje de vacío) certifica que
// la petición de datos ya respondió, sin asumir qué filas va a haber — la tabla
// puede llevar filas de otros DNIs (residuos reales, no del seed) en cualquier
// momento y eso no debe bloquear ni fallar esta barrera.
async function esperarListadoCargado(page: Page): Promise<void> {
  await expect(botonAnhadir(page)).toBeVisible();
  await expect(filaCuerpoRejilla(page).first()).toBeVisible();
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
  test('Alta con «Habilitado» marcado por defecto', async ({ page }) => {
    await ensureLoggedOut(page);
    // Precondición: el usuario `admin` ha iniciado sesión.
    await login(page, 'admin', 'admin');

    try {
      // Paso 1: Dado que el administrador está en la pantalla «Certificados
      //         digitales» (menú «Criptografía» → «Certificados digitales»).
      await abrirCertificadosDigitales(page);

      // Precondición: no existe ninguna entrada con el DNI «85432016B» (si la dejó
      // una ejecución anterior, se borra desde el listado).
      await borrarEntradaSiExiste(page);

      // Paso 2: Cuando pulsa «Añadir certificado digital».
      await botonAnhadir(page).click();

      // Paso 3: Entonces el formulario de alta muestra la casilla «Habilitado»
      //         marcada. (El nombre accesible real es «Habilitado ?» por el icono
      //         de ayuda; el match por subcadena lo cubre.)
      const habilitado = page.getByRole('checkbox', { name: 'Habilitado' });
      await expect(habilitado).toBeVisible();
      await expect(habilitado).toBeChecked();

      // Paso 4: Cuando rellena el campo «DNI» con «85432016B».
      await page.getByRole('textbox', { name: 'DNI', exact: true }).fill(DNI);

      // Paso 5: Y elige en «Tipo de certificado» la opción «Usar un fichero con el
      //         certificado que ya está dentro del del WAR».
      await page.getByRole('combobox', { name: 'Tipo de certificado' }).click();
      await page.getByRole('option', { name: OPCION_CLASSPATH }).click();

      // Paso 6: Y rellena el campo «Ruta classpath» con «firma/mi_certificado.p12»
      //         y el campo «Nueva contraseña» con «nadanada». (Ambos campos solo se
      //         muestran al elegir el tipo CLASSPATH.)
      await page.getByRole('textbox', { name: 'Ruta classpath' }).fill('firma/mi_certificado.p12');
      // Es un <input type="password"> (sin rol `textbox`): se localiza por su etiqueta.
      await page.getByLabel('Nueva contraseña').fill('nadanada');

      // Paso 7: Y pulsa «Guardar».
      await page.getByRole('button', { name: 'Guardar' }).click();

      // Resultado esperado 1: el sistema guarda la entrada y vuelve al listado
      // «Certificados digitales» (la vista pasa de /edit a /list y reaparece el
      // botón de alta, que solo existe en la vista de lista).
      await expect(page).toHaveURL(/CertificadoDigital-action\/list/);
      await expect(botonAnhadir(page)).toBeVisible();

      // Resultado esperado 2: el listado muestra la fila del DNI «85432016B» con la
      // columna «Habilitado» marcada (la celda de esa columna es la única casilla
      // de la fila: la rejilla no tiene columna de selección).
      const fila = filaDelDni(page);
      await expect(fila).toBeVisible();
      await expect(fila.getByRole('checkbox')).toBeChecked();
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
