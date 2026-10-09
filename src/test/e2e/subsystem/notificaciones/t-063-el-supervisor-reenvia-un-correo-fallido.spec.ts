import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-063 — El supervisor reenvía un correo fallido
// origen: ESC-050  |  verifica: V-Notificacion-015, U-notificaciones-centro-004, U-notificaciones-centro-005
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-063-el-supervisor-reenvia-un-correo-fallido.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (lo
// verifica T-046), así que este test NO tiene teardown — excepción documentada
// del §4.5 del contrato. Para no colisionar con las notificaciones de runs
// anteriores (la BD compartida no se resetea), el motivo «Correo a reenviar»
// lleva un sufijo único por ejecución y la fila se localiza por ese motivo. El
// resto de datos son los del correo de referencia tal cual.
const MOTIVO = `Correo a reenviar t063-${Date.now()}`;
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

async function abrirMenu(page: Page, submenu: string): Promise<void> {
  // Los ítems del menú lateral de Axelor no exponen rol ARIA; se acota por el
  // data-testid estable que Axelor deriva del nombre del menuitem.
  const menu = page.getByTestId('item:notificaciones-menuitem');
  await menu.getByText('Notificaciones', { exact: true }).click();
  await menu.getByText(submenu, { exact: true }).click();
}

async function altaCorreo(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Nueva notificación' }).click();
  const eleccion = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Elija el canal de la notificación' }) });
  await eleccion.getByRole('radio', { name: 'Correo' }).click();
  await eleccion.getByRole('button', { name: 'Continuar' }).click();

  const form = ventanaCorreo(page);
  await expect(form.getByRole('region', { name: 'Datos del correo' })).toBeVisible();
  // El centro primero: many-to-one con autocompletado que re-renderiza el formulario.
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
}

// El envío es asíncrono: se recarga el listado hasta que el correo deja
// «Pendiente» y se devuelve su estado final. En «Del centro» la columna
// «Estado» no es la primera (va tras «Centro» y «Tipo de notificación»), así
// que la celda se localiza por su valor y no por su posición.
const celdaEstadoFinal = (page: Page): Locator =>
  filaDelCorreo(page).getByRole('gridcell', { name: /^(Enviado|Fallido)$/ });
async function esperarEstadoFinal(page: Page): Promise<string> {
  await expect(async () => {
    await page.reload();
    const fila = filaDelCorreo(page);
    await expect(fila).toBeVisible({ timeout: 10_000 });
    await expect(celdaEstadoFinal(page)).toBeVisible({ timeout: 1_000 });
  }).toPass({ timeout: 60_000 });
  return (await celdaEstadoFinal(page).innerText()).trim();
}

test.describe('Notificaciones — Del centro', () => {
  test('El supervisor reenvía un correo fallido', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el administrador da de alta el correo de referencia con el
    //         motivo «Correo a reenviar» y cierra sesión.
    await login(page, 'admin', 'admin');
    await abrirMenu(page, 'Todas');
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
    await altaCorreo(page);
    await logout(page);

    // Paso 2: Cuando «supervisor1@mislata.es» inicia sesión, espera unos segundos,
    //         abre «Del centro» y pulsa la fila «Correo a reenviar».
    await login(page, 'supervisor1@mislata.es', 'demo1234');
    await abrirMenu(page, 'Del centro');
    // Se espera a que la pestaña esté abierta (y la URL apunte a su acción)
    // antes de recargar; si no, la recarga vuelve a la portada sin pestañas.
    await expect(page.getByRole('tab', { name: 'Notificaciones del centro' })).toBeVisible();
    await expect(page).toHaveURL(/Centro%40Notificacion-action/);
    // «Espera unos segundos»: se recarga «Del centro» hasta que el primer envío termina.
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
      // Resultado esperado: si estaba «Enviado», no se ve «Reenviar».
      await expect(consulta.getByRole('button', { name: 'Reenviar' })).toHaveCount(0);
      await consulta.getByRole('button', { name: 'Salir' }).click();
      await expect(consulta).toBeHidden();
    } else {
      // Resultado esperado: si estaba «Fallido», se ve «Reenviar».
      const reenviar = consulta.getByRole('button', { name: 'Reenviar' });
      await expect(reenviar).toBeVisible();

      // Paso 3: Y si está «Fallido», pulsa «Reenviar» ...
      await reenviar.click();
      // Resultado esperado: aparece el aviso de que el reenvío se ha puesto en
      // marcha (el texto es genérico, no nombra el canal).
      await expect(page.getByText(/El reenvío .+ se ha puesto en marcha\./)).toBeVisible();

      // Cierra la ventana para volver a abrir el correo desde el listado.
      if (await consulta.isVisible()) {
        await consulta.getByRole('button', { name: 'Salir' }).click();
        await expect(consulta).toBeHidden();
      }

      // Paso 3 (cont.): ... y, pasados unos segundos, vuelve a abrir el correo.
      // El reenvío también es asíncrono: se reabre hasta que el segundo intento
      // ha terminado (reintentos «2» y estado distinto de «Pendiente»).
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

      // Resultado esperado: al volver a abrirlo, «Datos del envío» muestra
      // reintentos «2» y «Enviado» (con fecha de envío) o «Fallido» (con la
      // descripción del nuevo fallo).
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
