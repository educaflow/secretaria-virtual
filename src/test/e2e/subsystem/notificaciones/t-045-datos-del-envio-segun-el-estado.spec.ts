import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-045 — Datos del envío según el estado
// origen: ESC-036  |  verifica: U-notificaciones-todas-024, U-notificaciones-todas-025, U-notificaciones-todas-026, R-Notificacion-003
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-045-datos-del-envio-segun-el-estado.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (lo
// verifica T-049), así que este test NO tiene teardown — excepción documentada
// del §4.5 del contrato. Para no colisionar con las notificaciones de runs
// anteriores (la BD compartida no se resetea), el motivo «SMS estado» lleva un
// sufijo único por ejecución y la fila se localiza por ese motivo. El resto de
// datos son los del SMS de referencia tal cual.
const MOTIVO = `SMS estado t045-${Date.now()}`;
const SMS = {
  centro: 'CIPFP Mislata',
  dni: '86862719E',
  nombre: 'Alumno1',
  apellidos: 'CIPFP Mislata',
  telefono: '600111222',
  mensaje: 'Mañana no hay clase',
};

const FECHA_HORA = /^\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}$/;

// El listado se ordena por «Fecha de creación» descendente, así que la
// notificación recién creada está en la primera página.
const filaDelSms = (page: Page): Locator => page.getByRole('row', { name: MOTIVO });
const ventanaSms = (page: Page): Locator =>
  page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'SMS', exact: true }) });

async function abrirTodas(page: Page): Promise<void> {
  await page.getByText('Notificaciones', { exact: true }).click();
  await page.getByText('Todas', { exact: true }).click();
  await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
}

test.describe('Notificaciones — Todas', () => {
  test('Datos del envío según el estado', async ({ page }) => {
    await ensureLoggedOut(page);
    // Paso 1: Dado que el administrador ha iniciado sesión y da de alta el SMS
    //         de referencia con el motivo «SMS estado».
    await login(page, 'admin', 'admin');
    await abrirTodas(page);
    await page.getByRole('button', { name: 'Nueva notificación' }).click();
    const eleccion = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Elija el canal de la notificación' }) });
    await eleccion.getByRole('radio', { name: 'SMS' }).click();
    await eleccion.getByRole('button', { name: 'Continuar' }).click();

    const form = ventanaSms(page);
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
    // así que se localiza por su placeholder. Se teclea carácter a carácter
    // porque el widget formatea el número al vuelo.
    const telefono = form.getByPlaceholder('+34 123 456 789');
    await telefono.click();
    await telefono.pressSequentially(SMS.telefono);
    await expect(telefono).toHaveValue(/600\s*111\s*222/);
    await form.getByRole('textbox', { name: 'Mensaje' }).fill(SMS.mensaje);
    await form.getByRole('button', { name: 'Guardar' }).click();
    await expect(form).toBeHidden();
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();

    // Paso 2: Cuando pasados unos segundos pulsa en «Todas» la fila «SMS estado».
    // El envío es asíncrono: se recarga hasta que la notificación deja «Pendiente».
    await expect(async () => {
      await page.reload();
      const fila = filaDelSms(page);
      await expect(fila).toBeVisible({ timeout: 10_000 });
      await expect(fila.getByRole('gridcell').first()).toHaveText(/^(Enviado|Fallido)$/, { timeout: 1_000 });
    }).toPass({ timeout: 60_000 });
    const estado = (await filaDelSms(page).getByRole('gridcell').first().innerText()).trim();
    await filaDelSms(page).click();

    // Resultado esperado: «Datos del envío» muestra el estado, el número de
    // reintentos «1», la fecha de creación y las fechas del primer y del último
    // intento.
    const consulta = ventanaSms(page);
    const envio = consulta.getByRole('region', { name: 'Datos del envío' });
    await expect(envio).toBeVisible();
    await expect(consulta.getByRole('textbox', { name: /^Motivo/ })).toHaveValue(MOTIVO);
    const enviado = envio.getByRole('radio', { name: 'Enviado' });
    const fallido = envio.getByRole('radio', { name: 'Fallido' });
    await expect(envio.getByRole('radio', { name: 'Pendiente' })).not.toBeChecked();
    await expect(envio.getByRole('radio', { name: estado })).toBeChecked();
    await expect(envio.getByRole('textbox', { name: 'Nº reintentos' })).toHaveValue('1');
    await expect(envio.getByRole('textbox', { name: 'Fecha de creación' })).toHaveValue(FECHA_HORA);
    await expect(envio.getByRole('textbox', { name: 'Fecha del primer intento de envío' })).toHaveValue(FECHA_HORA);
    await expect(envio.getByRole('textbox', { name: 'Fecha del último intento de envío' })).toHaveValue(FECHA_HORA);

    // La «Descripción del último fallo» es de solo lectura (widget Text): se
    // pinta como texto tras su etiqueta, no como textbox.
    if (estado === 'Enviado') {
      await expect(fallido).not.toBeChecked();
      // Resultado esperado: si el estado es «Enviado», muestra la fecha de envío
      // y no la descripción del último fallo.
      await expect(envio.getByRole('textbox', { name: 'Fecha de envío' })).toHaveValue(FECHA_HORA);
      await expect(envio).not.toContainText('Descripción del último fallo');
    } else {
      // Resultado esperado: si el estado es «Fallido», muestra la descripción
      // del último fallo y no la fecha de envío.
      await expect(enviado).not.toBeChecked();
      await expect(envio).toContainText(/Descripción del último fallo\s*\S/);
      await expect(envio.getByRole('textbox', { name: 'Fecha de envío' })).toHaveCount(0);
      await expect(envio).not.toContainText('Fecha de envío');
    }

    // El formulario se abre como ventana modal sobre el listado: se cierra antes
    // del logout para que el menú de usuario quede accesible.
    await consulta.getByRole('button', { name: 'Salir' }).click();
    await expect(consulta).toBeHidden();

    await logout(page);
  });
});
