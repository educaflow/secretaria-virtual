import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-001 — Alta de un correo y resultado del envío
// origen: ESC-001  |  verifica: R-Notificacion-001, R-Notificacion-002, R-Notificacion-003, U-notificaciones-todas-005, U-notificaciones-todas-006, U-notificaciones-todas-007
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-001-alta-de-un-correo-y-resultado-del-envio.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (lo
// verifica T-046), así que este test NO tiene teardown — excepción documentada
// del §4.5 del contrato. Para no colisionar con las notificaciones que dejan los
// runs anteriores (la BD compartida no se resetea), el motivo del «correo de
// referencia» lleva un sufijo único por ejecución y todas las aserciones localizan
// la fila por ese motivo. El resto de datos son los del correo de referencia tal
// cual (el DNI tiene letra de control y el «para» es una dirección real del seed).
const MOTIVO = `Aviso de prueba de correo t001-${Date.now()}`;
const CORREO = {
  centro: 'CIPFP Mislata',
  dni: '86862719E',
  nombre: 'Alumno1',
  apellidos: 'CIPFP Mislata',
  para: 'alumno1@mislata.es',
  asunto: 'Reunión de inicio de curso',
  cuerpo: 'La reunión será el lunes a las 10:00.',
};

// El listado se ordena por «Fecha de creación» descendente, así que la
// notificación recién creada está en la primera página.
const filaDelCorreo = (page: Page): Locator => page.getByRole('row', { name: MOTIVO });

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
  test('Alta de un correo y resultado del envío', async ({ page }) => {
    await ensureLoggedOut(page);
    // Paso 1: Dado que el administrador ha iniciado sesión con «admin»/«admin».
    await login(page, 'admin', 'admin');

    // Paso 2: Cuando abre «Notificaciones» → «Todas», pulsa «Nueva notificación»,
    //         elige «Correo» y pulsa «Continuar».
    await abrirTodas(page);
    await page.getByRole('button', { name: 'Nueva notificación' }).click();
    const eleccion = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Elija el canal de la notificación' }) });
    await eleccion.getByRole('radio', { name: 'Correo' }).click();
    await eleccion.getByRole('button', { name: 'Continuar' }).click();

    // Paso 3: Y rellena el correo de referencia y pulsa «Guardar».
    const form = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Correo', exact: true }) });
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

    // Paso 4: Entonces el sistema vuelve al listado y muestra la notificación con
    //         tipo «Correo», motivo «Aviso de prueba de correo», destino
    //         «alumno1@mislata.es» y centro «CIPFP Mislata».
    await expect(form).toBeHidden();
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
    const fila = filaDelCorreo(page);
    await expect(fila).toBeVisible();
    await expect(celda(fila, COL.tipo)).toHaveText('Correo');
    await expect(celda(fila, COL.motivo)).toHaveText(MOTIVO);
    await expect(celda(fila, COL.motivo)).toContainText('Aviso de prueba de correo');
    await expect(celda(fila, COL.destino)).toHaveText(CORREO.para);
    await expect(celda(fila, COL.centro)).toHaveText(CORREO.centro);

    // Paso 5: Y pasados unos segundos recarga el listado.
    // El envío es asíncrono: se recarga hasta que la notificación deja «Pendiente».
    await expect(async () => {
      await page.reload();
      await expect(filaDelCorreo(page)).toBeVisible({ timeout: 10_000 });
      await expect(celda(filaDelCorreo(page), COL.estado)).toHaveText(/^(Enviado|Fallido)$/, { timeout: 1_000 });
    }).toPass({ timeout: 60_000 });
    const estado = (await celda(filaDelCorreo(page), COL.estado).innerText()).trim();

    if (estado === 'Enviado') {
      // Resultado esperado: si está «Enviado», tiene fecha de envío.
      await expect(celda(filaDelCorreo(page), COL.fechaEnvio)).toHaveText(/\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}/);
    } else {
      // Resultado esperado: si está «Fallido», no tiene fecha de envío...
      await expect(celda(filaDelCorreo(page), COL.fechaEnvio)).toHaveText('');

      // Paso 6: Y si la notificación está «Fallido», pulsa su fila.
      await filaDelCorreo(page).click();

      // ...y, al abrirla, el panel «Datos del envío» muestra la descripción del
      // último fallo rellena.
      const envio = page.getByRole('region', { name: 'Datos del envío' });
      await expect(envio).toBeVisible();
      await expect(envio.getByRole('radio', { name: 'Fallido' })).toBeChecked();
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
