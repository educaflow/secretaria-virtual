import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-052 — Reenvío de un correo desde «Todas»
// origen: ESC-039  |  verifica: V-Notificacion-014, V-Notificacion-015, R-Notificacion-004, U-notificaciones-todas-009, U-notificaciones-todas-011
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-052-reenvio-de-un-correo-desde-todas.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (lo
// verifica T-046), así que este test NO tiene teardown — excepción documentada
// del §4.5 del contrato. Para no colisionar con las notificaciones de runs
// anteriores (la BD compartida no se resetea), el motivo «Correo a reenviar»
// lleva un sufijo único por ejecución y la fila se localiza por ese motivo. El
// resto de datos son los del correo de referencia tal cual.
const MOTIVO = `Correo a reenviar t052-${Date.now()}`;
const CORREO = {
  centro: 'CIPFP Mislata',
  dni: '86862719E',
  nombre: 'Alumno1',
  apellidos: 'CIPFP Mislata',
  para: 'alumno1@mislata.es',
  asunto: 'Reunión de inicio de curso',
  cuerpo: 'La reunión será el lunes a las 10:00.',
};

const FECHA_HORA = /^\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}$/;

// El listado se ordena por «Fecha de creación» descendente, así que la
// notificación recién creada está en la primera página.
const filaDelCorreo = (page: Page): Locator => page.getByRole('row', { name: MOTIVO });
const ventanaCorreo = (page: Page): Locator =>
  page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Correo', exact: true }) });

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
    const fila = filaDelCorreo(page);
    await expect(fila).toBeVisible({ timeout: 10_000 });
    await expect(fila.getByRole('gridcell').first()).toHaveText(/^(Enviado|Fallido)$/, { timeout: 1_000 });
  }).toPass({ timeout: 60_000 });
  return (await filaDelCorreo(page).getByRole('gridcell').first().innerText()).trim();
}

test.describe('Notificaciones — Todas', () => {
  test('Reenvío de un correo desde «Todas»', async ({ page }) => {
    await ensureLoggedOut(page);
    // Paso 1: Dado que el administrador ha iniciado sesión y da de alta el correo
    //         de referencia con el motivo «Correo a reenviar».
    await login(page, 'admin', 'admin');
    await abrirTodas(page);
    await page.getByRole('button', { name: 'Nueva notificación' }).click();
    const eleccion = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Elija el canal de la notificación' }) });
    await eleccion.getByRole('radio', { name: 'Correo' }).click();
    await eleccion.getByRole('button', { name: 'Continuar' }).click();

    const form = ventanaCorreo(page);
    await expect(form.getByRole('region', { name: 'Datos del correo' })).toBeVisible();
    // El centro primero: es un many-to-one con autocompletado y su selección
    // re-renderiza el formulario.
    await form.getByRole('combobox', { name: 'Centro' }).fill(CORREO.centro);
    await page.getByRole('option', { name: CORREO.centro, exact: true }).click();
    await expect(form.getByRole('combobox', { name: 'Centro' })).toHaveValue(CORREO.centro);
    await form.getByRole('textbox', { name: 'Motivo' }).fill(MOTIVO);
    await form.getByRole('textbox', { name: 'DNI del destinatario' }).fill(CORREO.dni);
    await form.getByRole('textbox', { name: 'Nombre', exact: true }).fill(CORREO.nombre);
    await form.getByRole('textbox', { name: 'Apellidos' }).fill(CORREO.apellidos);
    await form.getByRole('textbox', { name: /^Para/ }).fill(CORREO.para);
    await form.getByRole('textbox', { name: 'Asunto' }).fill(CORREO.asunto);
    await form.getByRole('textbox', { name: 'Cuerpo' }).fill(CORREO.cuerpo);
    await form.getByRole('button', { name: 'Guardar' }).click();
    await expect(form).toBeHidden();
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();

    // Paso 2: Cuando pasados unos segundos pulsa en «Todas» la fila «Correo a reenviar».
    const estado = await esperarEstadoFinal(page);
    await filaDelCorreo(page).click();
    const consulta = ventanaCorreo(page);
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
      // Resultado esperado: si estaba «Fallido», se ve el botón «Reenviar».
      const reenviar = consulta.getByRole('button', { name: 'Reenviar' });
      await expect(reenviar).toBeVisible();

      // Paso 3: Y si está «Fallido», pulsa «Reenviar».
      await reenviar.click();
      // Resultado esperado: al pulsarlo aparece el aviso de que el reenvío se
      // ha puesto en marcha (texto genérico, sin nombrar el canal).
      await expect(page.getByText(/El reenvío .+ se ha puesto en marcha\./)).toBeVisible();

      // Cierra la ventana para volver a abrir el correo desde el listado.
      if (await consulta.isVisible()) {
        await consulta.getByRole('button', { name: 'Salir' }).click();
        await expect(consulta).toBeHidden();
      }

      // Paso 4: Y pasados unos segundos vuelve a abrir el correo «Correo a reenviar».
      // El reenvío también es asíncrono: se reabre hasta que el segundo intento
      // ha terminado (nº de reintentos «2» y estado distinto de «Pendiente»).
      await expect(async () => {
        await page.reload();
        await expect(filaDelCorreo(page)).toBeVisible({ timeout: 10_000 });
        await filaDelCorreo(page).click();
        const reabierto = ventanaCorreo(page);
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
      // está «Enviado» con fecha de envío, o de nuevo «Fallido» con la
      // descripción del nuevo fallo.
      const reabierto = ventanaCorreo(page);
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
