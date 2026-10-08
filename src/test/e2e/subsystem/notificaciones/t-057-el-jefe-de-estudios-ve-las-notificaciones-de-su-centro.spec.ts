import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-057 — El jefe de estudios ve las notificaciones de su centro
// origen: ESC-044  |  verifica: —
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-057-el-jefe-de-estudios-ve-las-notificaciones-de-su-centro.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (lo
// verifican T-046 y T-049), así que este test NO tiene teardown — excepción
// documentada del §4.5 del contrato. Para no colisionar con las notificaciones
// que dejan los runs anteriores (la BD compartida no se resetea), el motivo
// «Aviso para jefatura» lleva un sufijo único por ejecución y la aserción
// localiza la fila por ese motivo.
const MOTIVO = `Aviso para jefatura t057-${Date.now()}`;

// SMS de referencia, con el motivo «Aviso para jefatura».
const SMS = {
  centro: 'CIPFP Mislata',
  motivo: MOTIVO,
  dni: '86862719E',
  nombre: 'Alumno1',
  apellidos: 'CIPFP Mislata',
  telefono: '600111222',
  mensaje: 'Mañana no hay clase',
};

// Columnas del grid «Notificaciones del centro», en orden: Centro, Tipo, Estado,
// DNI del destinatario, Nombre, Apellidos, Motivo, Destino, Expediente,
// Fecha de creación, Fecha de envío.
const COL = { motivo: 6 };
const celda = (fila: Locator, col: number): Locator => fila.getByRole('gridcell').nth(col);

async function abrirMenu(page: Page, submenu: string): Promise<void> {
  // Los ítems del menú lateral de Axelor no exponen rol ARIA; se acota por el
  // data-testid estable que Axelor deriva del nombre del menuitem.
  const menu = page.getByTestId('item:notificaciones-menuitem');
  await menu.getByText('Notificaciones', { exact: true }).click();
  await menu.getByText(submenu, { exact: true }).click();
}

async function altaSms(page: Page, sms: typeof SMS): Promise<void> {
  await page.getByRole('button', { name: 'Nueva notificación' }).click();
  const eleccion = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Elija el canal de la notificación' }) });
  await eleccion.getByRole('radio', { name: 'SMS' }).click();
  await eleccion.getByRole('button', { name: 'Continuar' }).click();
  const form = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'SMS', exact: true }) });
  await expect(form.getByRole('region', { name: 'Datos del SMS' })).toBeVisible();
  // El centro primero: es un many-to-one con autocompletado y su selección
  // re-renderiza el formulario.
  await form.getByRole('combobox', { name: 'Centro' }).fill(sms.centro);
  await page.getByRole('option', { name: sms.centro, exact: true }).click();
  await expect(form.getByRole('combobox', { name: 'Centro' })).toHaveValue(sms.centro);
  await form.getByRole('textbox', { name: 'Motivo' }).fill(sms.motivo);
  await form.getByRole('textbox', { name: 'DNI del destinatario' }).fill(sms.dni);
  await form.getByRole('textbox', { name: 'Nombre', exact: true }).fill(sms.nombre);
  await form.getByRole('textbox', { name: 'Apellidos' }).fill(sms.apellidos);
  // El widget «phone» de Axelor no asocia la etiqueta «Teléfono» a su input,
  // así que se localiza por su placeholder. Se teclea carácter a carácter
  // porque el widget formatea el número al vuelo.
  const telefono = form.getByPlaceholder('+34 123 456 789');
  await telefono.click();
  await telefono.pressSequentially(sms.telefono);
  await expect(telefono).toHaveValue(/600\s*111\s*222/);
  await form.getByRole('textbox', { name: 'Mensaje' }).fill(sms.mensaje);
  await form.getByRole('button', { name: 'Guardar' }).click();
  await expect(form).toBeHidden();
  await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
}

test.describe('Notificaciones — Del centro', () => {
  test('El jefe de estudios ve las notificaciones de su centro', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el administrador da de alta el SMS de referencia con el
    //         motivo «Aviso para jefatura» y cierra sesión.
    await login(page, 'admin', 'admin');
    await abrirMenu(page, 'Todas');
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
    await altaSms(page, SMS);
    await logout(page);

    // Paso 2: Cuando «jefeestudios1@mislata.es» inicia sesión y abre
    //         «Notificaciones» → «Del centro».
    await login(page, 'jefeestudios1@mislata.es', 'demo1234');
    await abrirMenu(page, 'Del centro');

    // Resultado esperado: se ve «Aviso para jefatura».
    const fila = page.getByRole('row', { name: MOTIVO });
    await expect(fila).toBeVisible();
    await expect(celda(fila, COL.motivo)).toHaveText(MOTIVO);

    await logout(page);
  });
});
