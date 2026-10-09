import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-041 — Formulario de alta del SMS y cancelar el alta
// origen: ESC-060  |  verifica: U-notificaciones-todas-022, U-notificaciones-todas-024, U-notificaciones-todas-027
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-041-formulario-de-alta-del-sms-y-cancelar-el-alta.desc.md

// Idempotencia: el test no guarda nada (precisamente comprueba que «Cancelar» no crea
// la notificación), así que no hay nada que borrar en un teardown.
// Aun así, el motivo «SMS cancelado» lleva un sufijo único por ejecución: las
// notificaciones no se pueden borrar (ver T-049), y si un run defectuoso llegara a
// guardar una, un motivo fijo dejaría el test rojo para siempre. Con el sufijo,
// «no hay ninguna notificación con ese motivo» se comprueba sobre el motivo de este run.
// Además se compara el total del listado con el de antes del alta.

// «SMS de referencia» del .desc.md (el motivo lo sustituye el paso 3 por «SMS cancelado»).
const SMS = {
  centro: 'CIPFP Mislata',
  dni: '86862719E',
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
  test('Formulario de alta del SMS y cancelar el alta', async ({ page }) => {
    const motivo = `SMS cancelado t041-${Date.now()}`;

    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    // Paso 1: Dado que el administrador ha iniciado sesión, abre «Todas», pulsa
    //         «Nueva notificación», elige «SMS» y pulsa «Continuar».
    await page.getByText('Notificaciones', { exact: true }).click();
    const totalAntes = await esperandoBusqueda(page, () => page.getByText('Todas', { exact: true }).click());
    await page.getByRole('button', { name: 'Nueva notificación' }).click();
    const eleccion = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Elija el canal de la notificación' }) });
    await eleccion.getByRole('radio', { name: 'SMS' }).click();
    await eleccion.getByRole('button', { name: 'Continuar' }).click();

    // Paso 2: Entonces el formulario muestra el tipo de notificación «SMS» en solo
    //         lectura, sin el panel «Datos del envío», con «Guardar» y «Cancelar» y
    //         sin «Salir» ni «Reenviar».
    const form = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'SMS', exact: true }) });
    const datos = form.getByRole('region', { name: 'Datos del SMS' });
    await expect(datos).toBeVisible();
    await expect(datos.getByText('Tipo de notificación')).toBeVisible();
    // Solo lectura: el enumerado no editable se pinta como combobox (sin RadioSelect, VAR-6.7).
    const tipoNotificacion = datos.getByRole('combobox', { name: 'Tipo de notificación' });
    await expect(tipoNotificacion).toHaveValue('SMS');
    await expect(tipoNotificacion).not.toBeEditable();
    await expect(datos.getByRole('radio')).toHaveCount(0);
    await expect(form.getByRole('heading', { name: 'SMS', exact: true })).toBeVisible();
    await expect(page.getByRole('region', { name: 'Datos del envío' })).toHaveCount(0);
    await expect(page.getByText('Datos del envío')).toHaveCount(0);
    await expect(form.getByRole('button', { name: 'Guardar' })).toBeVisible();
    await expect(form.getByRole('button', { name: 'Cancelar' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Salir' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Reenviar' })).toHaveCount(0);

    // Paso 3: Cuando rellena el SMS de referencia con el motivo «SMS cancelado»...
    await form.getByRole('textbox', { name: /^Motivo/ }).fill(motivo);
    await form.getByRole('combobox', { name: /^Centro/ }).fill(SMS.centro);
    await page.getByRole('option', { name: SMS.centro, exact: true }).click();
    await expect(form.getByRole('combobox', { name: /^Centro/ })).toHaveValue(SMS.centro);
    await form.getByRole('textbox', { name: 'DNI del destinatario' }).fill(SMS.dni);
    await form.getByRole('textbox', { name: 'Nombre', exact: true }).fill(SMS.nombre);
    await form.getByRole('textbox', { name: 'Apellidos' }).fill(SMS.apellidos);
    // El widget «phone» de Axelor no asocia la etiqueta «Teléfono» a su input, así
    // que se localiza por su placeholder. Se teclea carácter a carácter porque
    // el widget formatea el número al vuelo.
    const telefono = form.getByPlaceholder('+34 123 456 789');
    await telefono.click();
    await telefono.pressSequentially(SMS.telefono);
    await expect(telefono).toHaveValue(/600\s*111\s*222/);
    await form.getByRole('textbox', { name: 'Mensaje' }).fill(SMS.mensaje);
    await expect(form.getByRole('textbox', { name: /^Motivo/ })).toHaveValue(motivo);

    // ...y pulsa «Cancelar».
    await form.getByRole('button', { name: 'Cancelar' }).click();

    // Resultado esperado: el sistema vuelve al listado...
    await expect(form).toBeHidden();
    await expect(page.getByRole('dialog')).toHaveCount(0);
    await expect(panelTodas(page)).toBeVisible();
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
    await expect(grid(page).getByRole('columnheader', { name: 'Motivo' })).toBeVisible();

    // ...y no muestra ninguna notificación con el motivo «SMS cancelado».
    // Se recarga para leer el estado real del servidor: el total no ha cambiado...
    const totalDespues = await esperandoBusqueda(page, () => page.reload());
    expect(totalDespues).toBe(totalAntes);
    await expect(filasDeDatos(page).filter({ hasText: motivo })).toHaveCount(0);
    // ...y buscando por ese motivo en la columna «Motivo» el listado sale vacío.
    await buscarMotivo(page).fill(motivo);
    const totalFiltrado = await esperandoBusqueda(page, () => buscarMotivo(page).press('Enter'));
    expect(totalFiltrado).toBe(0);
    await expect(paginador(page)).toHaveText('0 a 0 de 0');
    // Con el grid vacío Axelor pinta una única fila marcadora «No se encontraron registros.».
    await expect(filasDeDatos(page)).toHaveText(['No se encontraron registros.']);
    await expect(filasDeDatos(page).filter({ hasText: motivo })).toHaveCount(0);

    await logout(page);
  });
});
