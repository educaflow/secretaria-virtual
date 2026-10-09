import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-053 — Reenvío de un SMS desde «Todas»
// origen: ESC-040  |  verifica: V-Notificacion-014, R-Notificacion-004, U-notificaciones-todas-028, U-notificaciones-todas-031, U-notificaciones-todas-035
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-053-reenvio-de-un-sms-desde-todas.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (lo
// verifica T-049), así que este test NO tiene teardown — excepción documentada
// del §4.5 del contrato. Para no colisionar con las notificaciones de runs
// anteriores (la BD compartida no se resetea), el motivo «SMS a reenviar» lleva
// un sufijo único por ejecución y la fila se localiza por ese motivo. El resto
// de datos son los del SMS de referencia tal cual.
const MOTIVO = `SMS a reenviar t053-${Date.now()}`;
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

// El envío es asíncrono: se recarga el listado hasta que la notificación deja
// «Pendiente» y se devuelve su estado final («Enviado» o «Fallido»).
async function esperarEstadoFinal(page: Page): Promise<string> {
  await expect(async () => {
    await page.reload();
    const fila = filaDelSms(page);
    await expect(fila).toBeVisible({ timeout: 10_000 });
    await expect(fila.getByRole('gridcell').first()).toHaveText(/^(Enviado|Fallido)$/, { timeout: 1_000 });
  }).toPass({ timeout: 60_000 });
  return (await filaDelSms(page).getByRole('gridcell').first().innerText()).trim();
}

test.describe('Notificaciones — Todas', () => {
  test('Reenvío de un SMS desde «Todas»', async ({ page }) => {
    await ensureLoggedOut(page);
    // Paso 1: Dado que el administrador ha iniciado sesión y da de alta el SMS de
    //         referencia con el motivo «SMS a reenviar».
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

    // Paso 2: Cuando pasados unos segundos pulsa en «Todas» la fila «SMS a reenviar».
    const estado = await esperarEstadoFinal(page);
    await filaDelSms(page).click();
    const consulta = ventanaSms(page);
    const envio = consulta.getByRole('region', { name: 'Datos del envío' });
    await expect(envio).toBeVisible();
    await expect(consulta.getByRole('textbox', { name: /^Motivo/ })).toHaveValue(MOTIVO);
    // El estado es un enumerado de solo lectura: se pinta como combobox no
    // editable, sin radios.
    const comboEstado = envio.getByRole('combobox', { name: 'Estado' });
    await expect(comboEstado).toHaveValue(estado);
    await expect(comboEstado).not.toBeEditable();
    await expect(envio.getByRole('radio')).toHaveCount(0);

    if (estado === 'Enviado') {
      // Resultado esperado: si estaba «Enviado», no se ve el botón «Reenviar».
      await expect(consulta.getByRole('button', { name: 'Reenviar' })).toHaveCount(0);
      await consulta.getByRole('button', { name: 'Salir' }).click();
      await expect(consulta).toBeHidden();
    } else {
      // Paso 3: Y si está «Fallido», pulsa «Reenviar».
      const reenviar = consulta.getByRole('button', { name: 'Reenviar' });
      await expect(reenviar).toBeVisible();
      await reenviar.click();

      // Paso 4: Entonces el sistema pide confirmación con «Reenviar un SMS tiene
      //         coste. ¿Desea reenviarlo?».
      const confirmacion = page.getByRole('dialog').filter({ hasText: 'Reenviar un SMS tiene coste. ¿Desea reenviarlo?' });
      await expect(confirmacion).toBeVisible();

      // Paso 5: Cuando pulsa «Aceptar» y, pasados unos segundos, vuelve a abrir el SMS.
      await confirmacion.getByRole('button', { name: 'Aceptar' }).click();
      // Resultado esperado: aparece el aviso de que el reenvío se ha puesto en
      // marcha (el texto exacto es genérico, común a correo y SMS).
      await expect(page.getByText(/El reenvío .+ se ha puesto en marcha\./)).toBeVisible();

      // Cierra la ventana para volver a abrir el SMS desde el listado.
      if (await consulta.isVisible()) {
        await consulta.getByRole('button', { name: 'Salir' }).click();
        await expect(consulta).toBeHidden();
      }

      // El reenvío también es asíncrono: se reabre hasta que el segundo intento
      // ha terminado (nº de reintentos «2» y estado distinto de «Pendiente»).
      await expect(async () => {
        await page.reload();
        await expect(filaDelSms(page)).toBeVisible({ timeout: 10_000 });
        await filaDelSms(page).click();
        const reabierto = ventanaSms(page);
        const datosEnvio = reabierto.getByRole('region', { name: 'Datos del envío' });
        try {
          await expect(datosEnvio).toBeVisible({ timeout: 10_000 });
          await expect(datosEnvio.getByRole('textbox', { name: 'Nº reintentos' })).toHaveValue('2', { timeout: 2_000 });
          await expect(datosEnvio.getByRole('combobox', { name: 'Estado' })).not.toHaveValue('Pendiente', { timeout: 2_000 });
        } catch (e) {
          await reabierto.getByRole('button', { name: 'Salir' }).click().catch(() => {});
          throw e;
        }
      }).toPass({ timeout: 60_000 });

      // Resultado esperado: al volver a abrirlo, el número de reintentos es «2» y
      // está «Enviado» con fecha de envío, o «Fallido» con la descripción del
      // nuevo fallo.
      const reabierto = ventanaSms(page);
      const datosEnvio = reabierto.getByRole('region', { name: 'Datos del envío' });
      await expect(reabierto.getByRole('textbox', { name: /^Motivo/ })).toHaveValue(MOTIVO);
      await expect(datosEnvio.getByRole('textbox', { name: 'Nº reintentos' })).toHaveValue('2');
      const comboEstadoFinal = datosEnvio.getByRole('combobox', { name: 'Estado' });
      if ((await comboEstadoFinal.inputValue()) === 'Enviado') {
        await expect(datosEnvio.getByRole('textbox', { name: 'Fecha de envío' })).toHaveValue(FECHA_HORA);
      } else {
        await expect(comboEstadoFinal).toHaveValue('Fallido');
        // La «Descripción del último fallo» es de solo lectura (widget Text): se
        // pinta como texto tras su etiqueta, no como textbox.
        await expect(datosEnvio).toContainText(/Descripción del último fallo\s*\S/);
      }
      await reabierto.getByRole('button', { name: 'Salir' }).click();
      await expect(reabierto).toBeHidden();
    }

    await logout(page);
  });
});
