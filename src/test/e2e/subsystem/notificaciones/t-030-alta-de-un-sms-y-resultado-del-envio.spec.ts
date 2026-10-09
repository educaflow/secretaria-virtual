import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-030 — Alta de un SMS y resultado del envío
// origen: ESC-023  |  verifica: R-Sms-001, R-Notificacion-001, R-Notificacion-003, U-notificaciones-todas-025, U-notificaciones-todas-026
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-030-alta-de-un-sms-y-resultado-del-envio.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (lo
// verifica T-049), así que este test NO tiene teardown — excepción documentada
// del §4.5 del contrato. Para no colisionar con las notificaciones que dejan los
// runs anteriores (la BD compartida no se resetea), el motivo del «SMS de
// referencia» lleva un sufijo único por ejecución y todas las aserciones localizan
// la fila por ese motivo. El resto de datos son los del SMS de referencia tal cual.
const MOTIVO = `Aviso de prueba de SMS t030-${Date.now()}`;
const SMS = {
  centro: 'CIPFP Mislata',
  dni: '86862719E',
  nombre: 'Alumno1',
  apellidos: 'CIPFP Mislata',
  telefono: '600111222',
  mensaje: 'Mañana no hay clase',
};
// El destino del SMS es el teléfono normalizado a formato internacional.
const DESTINO = '+34600111222';

// El listado se ordena por «Fecha de creación» descendente, así que la
// notificación recién creada está en la primera página.
const filaDelSms = (page: Page): Locator => page.getByRole('row', { name: MOTIVO });

// Columnas del grid «Todas las notificaciones», en orden: Estado, Tipo, Motivo,
// DNI, Nombre, Apellidos, Destino, Centro, Expediente, Fecha de creación,
// Fecha de envío.
const COL = { estado: 0, tipo: 1, motivo: 2, destino: 6, centro: 7, fechaEnvio: 10 };
const celda = (fila: Locator, col: number): Locator => fila.getByRole('gridcell').nth(col);

async function abrirTodas(page: Page): Promise<void> {
  await page.getByText('Notificaciones', { exact: true }).click();
  await page.getByText('Todas', { exact: true }).click();
  await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
}

test.describe('Notificaciones — Todas', () => {
  test('Alta de un SMS y resultado del envío', async ({ page }) => {
    await ensureLoggedOut(page);
    // Paso 1: Dado que el administrador ha iniciado sesión.
    await login(page, 'admin', 'admin');

    // Paso 2: Cuando abre «Todas», pulsa «Nueva notificación», elige «SMS»,
    //         pulsa «Continuar», rellena el SMS de referencia y pulsa «Guardar».
    await abrirTodas(page);
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
    // El widget «phone» de Axelor no asocia la etiqueta «Teléfono» a su input
    // (su nombre accesible es el del país detectado, p.ej. «Desconocido»), así
    // que se localiza por su placeholder. Se teclea carácter a carácter porque
    // el widget formatea el número al vuelo.
    const telefono = form.getByPlaceholder('+34 123 456 789');
    await telefono.click();
    await telefono.pressSequentially(SMS.telefono);
    await expect(telefono).toHaveValue(/600\s*111\s*222/);
    await form.getByRole('textbox', { name: 'Mensaje' }).fill(SMS.mensaje);
    await form.getByRole('button', { name: 'Guardar' }).click();

    // Paso 3: Entonces el listado muestra la notificación con tipo «SMS», motivo
    //         «Aviso de prueba de SMS», destino «+34600111222» y centro
    //         «CIPFP Mislata».
    await expect(form).toBeHidden();
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
    const fila = filaDelSms(page);
    await expect(fila).toBeVisible();
    await expect(celda(fila, COL.tipo)).toHaveText('SMS');
    await expect(celda(fila, COL.motivo)).toHaveText(MOTIVO);
    await expect(celda(fila, COL.motivo)).toContainText('Aviso de prueba de SMS');
    await expect(celda(fila, COL.destino)).toHaveText(DESTINO);
    await expect(celda(fila, COL.centro)).toHaveText(SMS.centro);

    // Paso 4: Cuando pasados unos segundos recarga el listado.
    // El envío es asíncrono: se recarga hasta que la notificación deja «Pendiente».
    await expect(async () => {
      await page.reload();
      await expect(filaDelSms(page)).toBeVisible({ timeout: 10_000 });
      await expect(celda(filaDelSms(page), COL.estado)).toHaveText(/^(Enviado|Fallido)$/, { timeout: 1_000 });
    }).toPass({ timeout: 60_000 });
    const estado = (await celda(filaDelSms(page), COL.estado).innerText()).trim();

    if (estado === 'Enviado') {
      // Resultado esperado: si está «Enviado», tiene fecha de envío.
      await expect(celda(filaDelSms(page), COL.fechaEnvio)).toHaveText(/\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}/);
    } else {
      // Resultado esperado: si está «Fallido», no tiene fecha de envío...
      await expect(celda(filaDelSms(page), COL.fechaEnvio)).toHaveText('');

      // ...y, al abrirla, muestra la descripción del fallo.
      await filaDelSms(page).click();
      const envio = page.getByRole('region', { name: 'Datos del envío' });
      await expect(envio).toBeVisible();
      // El estado es de solo lectura: se pinta como combobox, no como radios.
      const estadoEnvio = envio.getByRole('combobox', { name: 'Estado' });
      await expect(estadoEnvio).toHaveValue('Fallido');
      await expect(estadoEnvio).not.toBeEditable();
      await expect(envio.getByRole('radio')).toHaveCount(0);
      await expect(envio.getByRole('textbox', { name: 'Fecha de envío' })).toHaveCount(0);
      // El campo es de solo lectura (widget Text): se pinta como texto a
      // continuación de su etiqueta «Descripción del último fallo», no como
      // textbox. Se exige que tras la etiqueta haya contenido no vacío.
      await expect(envio).toContainText(/Descripción del último fallo\s*\S/);
      await page.getByRole('button', { name: 'Salir' }).click();
    }

    await logout(page);
  });
});
