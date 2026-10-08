import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-032 — Alta de un SMS sin el DNI del destinatario
// origen: ESC-025  |  verifica: V-Notificacion-003, U-notificaciones-todas-029
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-032-alta-de-un-sms-sin-el-dni-del-destinatario.desc.md

// Idempotencia: el test no crea nada (precisamente comprueba que sin DNI no se
// crea la notificación), así que no hay nada que borrar en un teardown.
// Aun así, el motivo del «SMS de referencia» lleva un sufijo único por ejecución:
// otros tests (p.ej. T-030) sí guardan SMS con ese motivo, las notificaciones no
// se pueden borrar (ver T-049), y un motivo fijo haría que «no muestra ninguna
// notificación con el motivo» dependiera de lo que dejaron otros runs. Con el
// sufijo se comprueba sobre el motivo de este run; además se compara el total del
// listado con el de antes del alta.
const MOTIVO = `Aviso de prueba de SMS t032-${Date.now()}`;
// «SMS de referencia» del .desc.md; el DNI se deja vacío a propósito.
const SMS = {
  centro: 'CIPFP Mislata',
  nombre: 'Alumno1',
  apellidos: 'CIPFP Mislata',
  telefono: '600111222',
  mensaje: 'Mañana no hay clase',
};

const panelTodas = (page: Page): Locator => page.getByRole('tabpanel', { name: 'Todas las notificaciones' });
// Texto del paginador de Axelor: «<desde> a <hasta> de <total>».
const paginador = (page: Page): Locator => panelTodas(page).getByRole('toolbar', { name: 'View toolbar' }).getByText(/^\d+ a \d+ de \d+$/);
const grid = (page: Page): Locator => panelTodas(page).getByRole('grid');
// Filas de datos: el segundo rowgroup del grid (el primero son la cabecera y la fila de búsqueda).
const filasDeDatos = (page: Page): Locator => grid(page).getByRole('rowgroup').nth(1).getByRole('row');
// Caja de búsqueda de la columna «Motivo» (tercera columna: Estado, Tipo de notificación, Motivo).
const buscarMotivo = (page: Page): Locator => grid(page).getByRole('rowgroup').first().getByRole('row').nth(1).getByRole('gridcell').nth(2).getByRole('textbox');

// El paginador pinta «0 a 0 de 0» hasta que llega la búsqueda del grid, así que
// las acciones que (re)cargan el listado esperan a la respuesta de su `/search`.
// Devuelve el total que informa el servidor y espera a que el paginador lo pinte.
async function esperandoBusqueda(page: Page, accion: () => Promise<unknown>): Promise<number> {
  const busqueda = page.waitForResponse((r) => /\/ws\/rest\/[^/]+\/search$/.test(new URL(r.url()).pathname) && r.ok());
  await accion();
  const total = Number((await (await busqueda).json()).total ?? 0);
  await expect(paginador(page)).toHaveText(new RegExp(` de ${total}$`));
  return total;
}

test.describe('Notificaciones — Todas', () => {
  test('Alta de un SMS sin el DNI del destinatario', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    // Paso 1: Dado que el administrador ha iniciado sesión y abre el alta de un SMS
    //         («Notificaciones» → «Todas» → «Nueva notificación» → «SMS» → «Continuar»).
    await page.getByText('Notificaciones', { exact: true }).click();
    const totalAntes = await esperandoBusqueda(page, () => page.getByText('Todas', { exact: true }).click());
    await page.getByRole('button', { name: 'Nueva notificación' }).click();
    const eleccion = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Elija el canal de la notificación' }) });
    await eleccion.getByRole('radio', { name: 'SMS' }).click();
    await eleccion.getByRole('button', { name: 'Continuar' }).click();

    // Paso 2: Cuando rellena el SMS de referencia, deja el DNI vacío y pulsa «Guardar».
    const form = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'SMS', exact: true }) });
    await expect(form.getByRole('region', { name: 'Datos del SMS' })).toBeVisible();
    // El centro primero: es un many-to-one con autocompletado y su selección
    // re-renderiza el formulario.
    await form.getByRole('combobox', { name: 'Centro' }).fill(SMS.centro);
    await page.getByRole('option', { name: SMS.centro, exact: true }).click();
    await expect(form.getByRole('combobox', { name: 'Centro' })).toHaveValue(SMS.centro);
    await form.getByRole('textbox', { name: /^Motivo/ }).fill(MOTIVO);
    await form.getByRole('textbox', { name: 'Nombre', exact: true }).fill(SMS.nombre);
    await form.getByRole('textbox', { name: 'Apellidos' }).fill(SMS.apellidos);
    // El widget «phone» de Axelor no asocia la etiqueta «Teléfono» a su input, así
    // que se localiza por su placeholder; se teclea carácter a carácter porque el
    // widget formatea el número al vuelo.
    const telefono = form.getByPlaceholder('+34 123 456 789');
    await telefono.click();
    await telefono.pressSequentially(SMS.telefono);
    await expect(telefono).toHaveValue(/600\s*111\s*222/);
    await form.getByRole('textbox', { name: 'Mensaje' }).fill(SMS.mensaje);
    await expect(form.getByRole('textbox', { name: 'DNI del destinatario' })).toHaveValue('');
    await form.getByRole('button', { name: 'Guardar' }).click();

    // Paso 3: Entonces el sistema muestra «El DNI del destinatario es obligatorio»...
    await expect(form.getByText('El DNI del destinatario es obligatorio')).toBeVisible();
    // ...y no crea la notificación: el formulario sigue abierto sin guardar.
    await expect(form).toBeVisible();
    await expect(form.getByRole('textbox', { name: 'DNI del destinatario' })).toHaveValue('');

    // Paso 4: Cuando pulsa «Cancelar».
    await form.getByRole('button', { name: 'Cancelar' }).click();

    // Resultado esperado: el sistema vuelve al listado...
    await expect(form).toBeHidden();
    await expect(page.getByRole('dialog')).toHaveCount(0);
    await expect(panelTodas(page)).toBeVisible();
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
    await expect(grid(page).getByRole('columnheader', { name: 'Motivo' })).toBeVisible();

    // ...y no muestra ninguna notificación con el motivo «Aviso de prueba de SMS»
    // (el de este run). Se recarga para leer el estado real del servidor: el total
    // no ha cambiado...
    const totalDespues = await esperandoBusqueda(page, () => page.reload());
    expect(totalDespues).toBe(totalAntes);
    await expect(filasDeDatos(page).filter({ hasText: MOTIVO })).toHaveCount(0);
    // ...y buscando por ese motivo en la columna «Motivo» el listado sale vacío.
    await buscarMotivo(page).fill(MOTIVO);
    const totalFiltrado = await esperandoBusqueda(page, () => buscarMotivo(page).press('Enter'));
    expect(totalFiltrado).toBe(0);
    await expect(paginador(page)).toHaveText('0 a 0 de 0');
    // Con el grid vacío Axelor pinta una única fila marcadora «No se encontraron registros.».
    await expect(filasDeDatos(page)).toHaveText(['No se encontraron registros.']);

    await logout(page);
  });
});
