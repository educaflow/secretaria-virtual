import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-064 — El director reenvía un SMS fallido
// origen: ESC-051  |  verifica: V-Notificacion-015, U-notificaciones-centro-010, U-notificaciones-centro-011, U-notificaciones-centro-012
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-064-el-director-reenvia-un-sms-fallido.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (lo
// verifica T-049), así que este test NO tiene teardown — excepción documentada
// del §4.5 del contrato. Para no colisionar con las notificaciones de runs
// anteriores (la BD compartida no se resetea), el motivo «SMS a reenviar» lleva
// un sufijo único por ejecución y la fila se localiza por ese motivo. El resto
// de datos son los del SMS de referencia tal cual.
const MOTIVO = `SMS a reenviar t064-${Date.now()}`;
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

  const form = ventanaSms(page);
  await expect(form.getByRole('region', { name: 'Datos del SMS' })).toBeVisible();
  // El centro primero: many-to-one con autocompletado que re-renderiza el formulario.
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

// El envío es asíncrono: se recarga el listado hasta que el SMS deja
// «Pendiente» y se devuelve su estado final. En «Del centro» la columna
// «Estado» no es la primera (va tras «Centro» y «Tipo de notificación»), así
// que la celda se localiza por su valor y no por su posición.
const celdaEstadoFinal = (page: Page): Locator =>
  filaDelSms(page).getByRole('gridcell', { name: /^(Enviado|Fallido)$/ });
async function esperarEstadoFinal(page: Page): Promise<string> {
  await expect(async () => {
    await page.reload();
    await expect(filaDelSms(page)).toBeVisible({ timeout: 10_000 });
    await expect(celdaEstadoFinal(page)).toBeVisible({ timeout: 1_000 });
  }).toPass({ timeout: 60_000 });
  return (await celdaEstadoFinal(page).innerText()).trim();
}

test.describe('Notificaciones — Del centro', () => {
  test('El director reenvía un SMS fallido', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el administrador da de alta el SMS de referencia con el
    //         motivo «SMS a reenviar» y cierra sesión.
    await login(page, 'admin', 'admin');
    await abrirMenu(page, 'Todas');
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
    await altaSms(page);
    await logout(page);

    // Paso 2: Cuando «director@mislata.es» inicia sesión, espera unos segundos,
    //         abre «Del centro» y pulsa la fila «SMS a reenviar».
    await login(page, 'director@mislata.es', 'demo1234');
    await abrirMenu(page, 'Del centro');
    // Se espera a que la pestaña esté abierta (y la URL apunte a su acción)
    // antes de recargar; si no, la recarga vuelve a la portada sin pestañas.
    await expect(page.getByRole('tab', { name: 'Notificaciones del centro' })).toBeVisible();
    await expect(page).toHaveURL(/Centro%40Notificacion-action/);
    // «Espera unos segundos»: se recarga «Del centro» hasta que el primer envío termina.
    const estado = await esperarEstadoFinal(page);
    await filaDelSms(page).click();
    const consulta = ventanaSms(page);
    const envio = consulta.getByRole('region', { name: 'Datos del envío' });
    await expect(envio).toBeVisible();
    await expect(consulta.getByRole('textbox', { name: /^Motivo/ })).toHaveValue(MOTIVO);
    await expect(envio.getByRole('radio', { name: estado })).toBeChecked();

    if (estado === 'Enviado') {
      // Resultado esperado: si estaba «Enviado», no se ve «Reenviar».
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
      // Resultado esperado: aparece «El reenvío del SMS se ha puesto en marcha.».
      await expect(page.getByText('El reenvío del SMS se ha puesto en marcha.')).toBeVisible();

      // Cierra la ventana para volver a abrir el SMS desde el listado.
      if (await consulta.isVisible()) {
        await consulta.getByRole('button', { name: 'Salir' }).click();
        await expect(consulta).toBeHidden();
      }

      // El reenvío también es asíncrono: se reabre hasta que el segundo intento
      // ha terminado (reintentos «2» y estado distinto de «Pendiente»).
      await expect(async () => {
        await page.reload();
        await expect(filaDelSms(page)).toBeVisible({ timeout: 10_000 });
        await filaDelSms(page).click();
        const reabierto = ventanaSms(page);
        const datosEnvio = reabierto.getByRole('region', { name: 'Datos del envío' });
        try {
          await expect(datosEnvio).toBeVisible({ timeout: 10_000 });
          await expect(datosEnvio.getByRole('textbox', { name: 'Nº reintentos' })).toHaveValue('2', { timeout: 2_000 });
          await expect(datosEnvio.getByRole('radio', { name: 'Pendiente' })).not.toBeChecked({ timeout: 2_000 });
        } catch (e) {
          await reabierto.getByRole('button', { name: 'Salir' }).click().catch(() => {});
          throw e;
        }
      }).toPass({ timeout: 60_000 });

      // Resultado esperado: al volver a abrirlo, reintentos «2» y «Enviado»
      // (con fecha de envío) o «Fallido» con la descripción del nuevo fallo.
      const reabierto = ventanaSms(page);
      const datosEnvio = reabierto.getByRole('region', { name: 'Datos del envío' });
      await expect(reabierto.getByRole('textbox', { name: /^Motivo/ })).toHaveValue(MOTIVO);
      await expect(datosEnvio.getByRole('textbox', { name: 'Nº reintentos' })).toHaveValue('2');
      const enviado = datosEnvio.getByRole('radio', { name: 'Enviado' });
      if (await enviado.isChecked()) {
        await expect(datosEnvio.getByRole('textbox', { name: 'Fecha de envío' })).toHaveValue(FECHA_HORA);
      } else {
        await expect(datosEnvio.getByRole('radio', { name: 'Fallido' })).toBeChecked();
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
