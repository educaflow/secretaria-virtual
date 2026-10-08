import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-009 — Alta de un correo sin el nombre
// origen: ESC-009  |  verifica: V-Notificacion-005
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-009-alta-de-un-correo-sin-el-nombre.desc.md

// Idempotencia: el test no crea nada (precisamente comprueba que sin nombre no se
// crea la notificación), así que no necesita teardown ni nombres únicos.
// La BD es compartida y no se resetea: otros tests dejan notificaciones (que no se
// pueden borrar, ver T-046), así que «no crea la notificación» se verifica como
// «el total del listado es el mismo que antes de pulsar "Nueva notificación"».

// «Correo de referencia» del .desc.md; el nombre se deja vacío a propósito.
const CORREO = {
  centro: 'CIPFP Mislata',
  motivo: 'Aviso de prueba de correo',
  dni: '86862719E',
  apellidos: 'CIPFP Mislata',
  para: 'alumno1@mislata.es',
  asunto: 'Reunión de inicio de curso',
  cuerpo: 'La reunión será el lunes a las 10:00.',
};

const panelTodas = (page: Page): Locator => page.getByRole('tabpanel', { name: 'Todas las notificaciones' });
// Texto del paginador de Axelor: «<desde> a <hasta> de <total>».
const paginador = (page: Page): Locator => panelTodas(page).getByRole('toolbar', { name: 'View toolbar' }).getByText(/^\d+ a \d+ de \d+$/);

// El paginador pinta «0 a 0 de 0» hasta que llega la búsqueda del grid, así que
// las acciones que (re)cargan el listado esperan a la respuesta de su `/search`.
// Devuelve el total que informa el servidor y espera a que el paginador lo pinte.
async function esperandoBusqueda(page: Page, accion: () => Promise<unknown>): Promise<number> {
  const busqueda = page.waitForResponse((r) => /\/ws\/rest\/[^/]+\/search$/.test(new URL(r.url()).pathname) && r.ok());
  await accion();
  const total = Number((await (await busqueda).json()).total ?? 0);
  await expect(paginador(page)).toHaveText(new RegExp(` de ${total}$`));
  return total;
}

test.describe('Notificaciones — Todas', () => {
  test('Alta de un correo sin el nombre', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    // Paso 1: Dado que el administrador ha iniciado sesión y abre el alta de un correo
    //         («Notificaciones» → «Todas» → «Nueva notificación» → «Correo» → «Continuar»).
    await page.getByText('Notificaciones', { exact: true }).click();
    const totalAntes = await esperandoBusqueda(page, () => page.getByText('Todas', { exact: true }).click());
    await page.getByRole('button', { name: 'Nueva notificación' }).click();
    const eleccion = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Elija el canal de la notificación' }) });
    await eleccion.getByRole('radio', { name: 'Correo' }).click();
    await eleccion.getByRole('button', { name: 'Continuar' }).click();

    // Paso 2: Cuando rellena el correo de referencia y deja el nombre vacío.
    const form = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Correo', exact: true }) });
    await expect(form.getByRole('region', { name: 'Datos del correo' })).toBeVisible();
    await form.getByRole('combobox', { name: 'Centro' }).fill(CORREO.centro);
    await page.getByRole('option', { name: CORREO.centro, exact: true }).click();
    await expect(form.getByRole('combobox', { name: 'Centro' })).toHaveValue(CORREO.centro);
    await form.getByRole('textbox', { name: /^Motivo/ }).fill(CORREO.motivo);
    await form.getByRole('textbox', { name: 'DNI del destinatario' }).fill(CORREO.dni);
    await form.getByRole('textbox', { name: 'Apellidos' }).fill(CORREO.apellidos);
    await form.getByRole('textbox', { name: /^Para/ }).fill(CORREO.para);
    await form.getByRole('textbox', { name: 'Asunto' }).fill(CORREO.asunto);
    await form.getByRole('textbox', { name: 'Cuerpo' }).fill(CORREO.cuerpo);
    await expect(form.getByRole('textbox', { name: 'Nombre', exact: true })).toHaveValue('');

    // Paso 3: Y pulsa «Guardar».
    await form.getByRole('button', { name: 'Guardar' }).click();

    // Resultado esperado: el sistema muestra «El nombre es obligatorio»...
    await expect(form.getByText('El nombre es obligatorio')).toBeVisible();
    // ...y no crea la notificación: el formulario sigue abierto sin guardar...
    await expect(form).toBeVisible();
    await expect(form.getByRole('textbox', { name: 'Nombre', exact: true })).toHaveValue('');

    // ...y el listado conserva el mismo total que antes del alta. Se recarga la
    // página (descarta el formulario) para leer el estado real del servidor.
    const totalDespues = await esperandoBusqueda(page, () => page.reload());
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
    expect(totalDespues).toBe(totalAntes);

    await logout(page);
  });
});
