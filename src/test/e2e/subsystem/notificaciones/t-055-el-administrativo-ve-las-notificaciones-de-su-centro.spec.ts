import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-055 — El administrativo ve las notificaciones de su centro
// origen: ESC-042  |  verifica: —
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-055-el-administrativo-ve-las-notificaciones-de-su-centro.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (lo
// verifican T-046 y T-049), así que este test NO tiene teardown — excepción
// documentada del §4.5 del contrato. Para no colisionar con las notificaciones
// que dejan los runs anteriores (la BD compartida no se resetea), el motivo
// «SMS Mislata» lleva un sufijo único por ejecución y la aserción localiza la
// fila por ese motivo.
const MOTIVO = `SMS Mislata t055-${Date.now()}`;

// SMS de referencia, con el motivo «SMS Mislata».
const SMS = {
  centro: 'CIPFP Mislata',
  dni: '86862719E',
  nombre: 'Alumno1',
  apellidos: 'CIPFP Mislata',
  telefono: '600111222',
  mensaje: 'Mañana no hay clase',
};

// Columnas del grid «Notificaciones del centro», en orden: Centro, Tipo, Estado,
// DNI del destinatario, Nombre, Apellidos, Motivo, Destino, Expediente,
// Fecha de creación, Fecha de envío.
const COL_MOTIVO = 6;
const celda = (fila: Locator, col: number): Locator => fila.getByRole('gridcell').nth(col);

async function abrirMenu(page: Page, submenu: string): Promise<void> {
  // Los ítems del menú lateral de Axelor no exponen rol ARIA; se acota por el
  // data-testid estable que Axelor deriva del nombre del menuitem.
  const menu = page.getByTestId('item:notificaciones-menuitem');
  await menu.getByText('Notificaciones', { exact: true }).click();
  await menu.getByText(submenu, { exact: true }).click();
}

async function altaSms(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Nueva notificación' }).click();
  const eleccion = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Elija el canal de la notificación' }) });
  await eleccion.getByRole('radio', { name: 'SMS' }).click();
  await eleccion.getByRole('button', { name: 'Continuar' }).click();
  const form = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'SMS', exact: true }) });
  await expect(form.getByRole('region', { name: 'Datos del SMS' })).toBeVisible();
  // El centro primero: es un many-to-one con autocompletado y su selección
  // re-renderiza el formulario.
  await form.getByRole('combobox', { name: 'Centro' }).fill(SMS.centro);
  await page.getByRole('option', { name: SMS.centro, exact: true }).click();
  await expect(form.getByRole('combobox', { name: 'Centro' })).toHaveValue(SMS.centro);
  await form.getByRole('textbox', { name: 'Motivo' }).fill(MOTIVO);
  await form.getByRole('textbox', { name: 'DNI del destinatario' }).fill(SMS.dni);
  await form.getByRole('textbox', { name: 'Nombre', exact: true }).fill(SMS.nombre);
  await form.getByRole('textbox', { name: 'Apellidos' }).fill(SMS.apellidos);
  // El widget «phone» de Axelor no asocia la etiqueta «Teléfono» a su input,
  // así que se localiza por su placeholder y se teclea carácter a carácter
  // porque el widget formatea el número al vuelo.
  const telefono = form.getByPlaceholder('+34 123 456 789');
  await telefono.click();
  await telefono.pressSequentially(SMS.telefono);
  await expect(telefono).toHaveValue(/600\s*111\s*222/);
  await form.getByRole('textbox', { name: 'Mensaje' }).fill(SMS.mensaje);
  await form.getByRole('button', { name: 'Guardar' }).click();
  await expect(form).toBeHidden();
  await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
}

test.describe('Notificaciones — Del centro', () => {
  test('El administrativo ve las notificaciones de su centro', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el administrador da de alta el SMS de referencia con el
    //         motivo «SMS Mislata» y cierra sesión.
    await login(page, 'admin', 'admin');
    await abrirMenu(page, 'Todas');
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
    await altaSms(page);
    await logout(page);

    // Paso 2: Cuando «administrativo1@mislata.es» inicia sesión y abre
    //         «Notificaciones» → «Del centro».
    await login(page, 'administrativo1@mislata.es', 'demo1234');
    await abrirMenu(page, 'Del centro');

    // Resultado esperado: se ve «SMS Mislata».
    const fila = page.getByRole('row', { name: MOTIVO });
    await expect(fila).toBeVisible();
    await expect(celda(fila, COL_MOTIVO)).toHaveText(MOTIVO);

    await logout(page);
  });
});
