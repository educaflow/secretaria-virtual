import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-050 — El listado muestra primero las notificaciones más recientes
// origen: ESC-069  |  verifica: —
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-050-el-listado-muestra-primero-las-notificaciones-mas-recientes.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (lo
// verifican T-046 y T-049), así que este test NO tiene teardown — excepción
// documentada del §4.5 del contrato. Para no colisionar con las notificaciones
// que dejan los runs anteriores (la BD compartida no se resetea), los motivos
// «Primer aviso» y «Segundo aviso» llevan un sufijo único por ejecución y las
// aserciones localizan cada fila por su motivo.
const RUN = Date.now();
const MOTIVO_PRIMERO = `Primer aviso t050-${RUN}`;
const MOTIVO_SEGUNDO = `Segundo aviso t050-${RUN}`;

// Correo de referencia (centro, DNI, nombre, apellidos y «para»); el motivo, el
// asunto y el cuerpo los fija cada paso.
const CORREO = {
  centro: 'CIPFP Mislata',
  dni: '86862719E',
  nombre: 'Alumno1',
  apellidos: 'CIPFP Mislata',
  para: 'alumno1@mislata.es',
};

async function abrirTodas(page: Page): Promise<void> {
  await page.getByText('Notificaciones', { exact: true }).click();
  await page.getByText('Todas', { exact: true }).click();
  await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
}

async function elegirCanalCorreo(page: Page): Promise<Locator> {
  await page.getByRole('button', { name: 'Nueva notificación' }).click();
  const eleccion = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Elija el canal de la notificación' }) });
  await eleccion.getByRole('radio', { name: 'Correo' }).click();
  await eleccion.getByRole('button', { name: 'Continuar' }).click();
  const form = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Correo', exact: true }) });
  await expect(form.getByRole('region', { name: 'Datos del correo' })).toBeVisible();
  return form;
}

async function altaCorreo(page: Page, form: Locator, motivo: string, asunto: string, cuerpo: string): Promise<void> {
  // El centro primero: es un many-to-one con autocompletado y su selección
  // re-renderiza el formulario.
  await form.getByRole('combobox', { name: 'Centro' }).fill(CORREO.centro);
  await page.getByRole('option', { name: CORREO.centro, exact: true }).click();
  await expect(form.getByRole('combobox', { name: 'Centro' })).toHaveValue(CORREO.centro);
  await form.getByRole('textbox', { name: 'Motivo' }).fill(motivo);
  await form.getByRole('textbox', { name: 'DNI del destinatario' }).fill(CORREO.dni);
  await form.getByRole('textbox', { name: 'Nombre', exact: true }).fill(CORREO.nombre);
  await form.getByRole('textbox', { name: 'Apellidos' }).fill(CORREO.apellidos);
  await form.getByRole('textbox', { name: /^Para/ }).fill(CORREO.para);
  await form.getByRole('textbox', { name: 'Asunto' }).fill(asunto);
  await form.getByRole('textbox', { name: 'Cuerpo' }).fill(cuerpo);
  await form.getByRole('button', { name: 'Guardar' }).click();
  await expect(form).toBeHidden();
  await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
}

// Posición (índice) de la fila del grid cuyo texto contiene el motivo; -1 si no está.
async function posicionFila(page: Page, motivo: string): Promise<number> {
  const textos = await page.getByRole('row').allInnerTexts();
  return textos.findIndex((t) => t.includes(motivo));
}

test.describe('Notificaciones — Todas', () => {
  test('El listado muestra primero las notificaciones más recientes', async ({ page }) => {
    await ensureLoggedOut(page);
    // Paso 1: Dado que el administrador ha iniciado sesión.
    await login(page, 'admin', 'admin');

    // Paso 2: Cuando da de alta el correo de referencia con el motivo «Primer
    //         aviso», el asunto «Primero» y el cuerpo «Texto».
    await abrirTodas(page);
    const form1 = await elegirCanalCorreo(page);
    await altaCorreo(page, form1, MOTIVO_PRIMERO, 'Primero', 'Texto');

    // Paso 3: Y desde el mismo listado pulsa «Nueva notificación», elige
    //         «Correo», pulsa «Continuar» y da de alta el correo de referencia
    //         con el motivo «Segundo aviso», el asunto «Segundo» y el cuerpo «Texto».
    const form2 = await elegirCanalCorreo(page);
    await altaCorreo(page, form2, MOTIVO_SEGUNDO, 'Segundo', 'Texto');

    // Resultado esperado: el listado muestra la fila «Segundo aviso» por encima
    // de «Primer aviso».
    const filaPrimero = page.getByRole('row', { name: MOTIVO_PRIMERO });
    const filaSegundo = page.getByRole('row', { name: MOTIVO_SEGUNDO });
    await expect(filaPrimero).toBeVisible();
    await expect(filaSegundo).toBeVisible();
    await expect.poll(async () => {
      const segundo = await posicionFila(page, MOTIVO_SEGUNDO);
      const primero = await posicionFila(page, MOTIVO_PRIMERO);
      return segundo >= 0 && primero >= 0 && segundo < primero;
    }).toBe(true);

    await logout(page);
  });
});
