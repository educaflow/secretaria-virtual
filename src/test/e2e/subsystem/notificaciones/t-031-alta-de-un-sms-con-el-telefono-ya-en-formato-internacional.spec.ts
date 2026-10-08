import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-031 — Alta de un SMS con el teléfono ya en formato internacional
// origen: ESC-024  |  verifica: R-Sms-001, V-Sms-002
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-031-alta-de-un-sms-con-el-telefono-ya-en-formato-internacional.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (lo
// verifica T-049), así que este test NO tiene teardown — excepción documentada
// del §4.5 del contrato. Para no colisionar con las notificaciones que dejan los
// runs anteriores (la BD compartida no se resetea), el motivo del «SMS de
// referencia» lleva un sufijo único por ejecución y la aserción localiza la fila
// por ese motivo. El resto de datos son los del SMS de referencia, salvo el
// teléfono, que este test teclea ya en formato internacional.
const MOTIVO = `Aviso de prueba de SMS t031-${Date.now()}`;
const SMS = {
  centro: 'CIPFP Mislata',
  dni: '86862719E',
  nombre: 'Alumno1',
  apellidos: 'CIPFP Mislata',
  telefono: '+34 600 111 222',
  mensaje: 'Mañana no hay clase',
};
// El destino del SMS es el teléfono normalizado: sin espacios y con el prefijo.
const DESTINO = '+34600111222';

// El listado se ordena por «Fecha de creación» descendente, así que la
// notificación recién creada está en la primera página.
const filaDelSms = (page: Page): Locator => page.getByRole('row', { name: MOTIVO });

// Columnas del grid «Todas las notificaciones», en orden: Estado, Tipo, Motivo,
// DNI, Nombre, Apellidos, Destino, Centro, Expediente, Fecha de creación,
// Fecha de envío.
const COL = { tipo: 1, motivo: 2, destino: 6 };
const celda = (fila: Locator, col: number): Locator => fila.getByRole('gridcell').nth(col);

async function abrirTodas(page: Page): Promise<void> {
  await page.getByText('Notificaciones', { exact: true }).click();
  await page.getByText('Todas', { exact: true }).click();
  await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
}

test.describe('Notificaciones — Todas', () => {
  test('Alta de un SMS con el teléfono ya en formato internacional', async ({ page }) => {
    await ensureLoggedOut(page);
    // Paso 1: Dado que el administrador ha iniciado sesión y abre el alta de un SMS.
    await login(page, 'admin', 'admin');
    await abrirTodas(page);
    await page.getByRole('button', { name: 'Nueva notificación' }).click();
    const eleccion = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Elija el canal de la notificación' }) });
    await eleccion.getByRole('radio', { name: 'SMS' }).click();
    await eleccion.getByRole('button', { name: 'Continuar' }).click();

    const form = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'SMS', exact: true }) });
    await expect(form.getByRole('region', { name: 'Datos del SMS' })).toBeVisible();

    // Paso 2: Cuando rellena el SMS de referencia con el teléfono «+34 600 111 222»
    //         y pulsa «Guardar».
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
    // así que se localiza por su placeholder. Se teclea carácter a carácter
    // porque el widget formatea el número al vuelo. El widget arranca ya con el
    // prefijo «+34 » escrito, así que se vacía primero para teclear el número
    // completo en formato internacional (si no, el prefijo saldría duplicado).
    const telefono = form.getByPlaceholder('+34 123 456 789');
    await telefono.click();
    await telefono.press('Control+A');
    await telefono.press('Backspace');
    await expect(telefono).toHaveValue('');
    await telefono.pressSequentially(SMS.telefono);
    await expect(telefono).toHaveValue(/\+34\s*600\s*111\s*222/);
    await form.getByRole('textbox', { name: 'Mensaje' }).fill(SMS.mensaje);
    await form.getByRole('button', { name: 'Guardar' }).click();

    // Resultado esperado: el SMS se guarda y el listado muestra el destino
    //                     «+34600111222».
    await expect(form).toBeHidden();
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
    const fila = filaDelSms(page);
    await expect(fila).toBeVisible();
    await expect(celda(fila, COL.tipo)).toHaveText('SMS');
    await expect(celda(fila, COL.motivo)).toHaveText(MOTIVO);
    await expect(celda(fila, COL.destino)).toHaveText(DESTINO);

    await logout(page);
  });
});
