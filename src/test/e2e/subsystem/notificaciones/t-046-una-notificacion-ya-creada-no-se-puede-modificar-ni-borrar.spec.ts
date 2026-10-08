import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-046 — Una notificación ya creada no se puede modificar ni borrar
// origen: ESC-037  |  verifica: V-Notificacion-012, V-Notificacion-013, U-notificaciones-todas-004, U-notificaciones-todas-008
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-046-una-notificacion-ya-creada-no-se-puede-modificar-ni-borrar.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (es
// justo lo que verifica este test), así que NO tiene teardown — excepción
// documentada del §4.5 del contrato. Para no colisionar con las notificaciones
// de runs anteriores (la BD compartida no se resetea), el motivo «Correo
// inmutable» lleva un sufijo único por ejecución y la fila se localiza por él.
const MOTIVO = `Correo inmutable t046-${Date.now()}`;

// Correo de referencia.
const CORREO = {
  centro: 'CIPFP Mislata',
  dni: '86862719E',
  nombre: 'Alumno1',
  apellidos: 'CIPFP Mislata',
  para: 'alumno1@mislata.es',
  asunto: 'Reunión de inicio de curso',
  cuerpo: 'La reunión será el lunes a las 10:00.',
};

// Escapa un literal para usarlo dentro de una RegExp.
const literal = (texto: string): string => texto.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

const ventanaCorreo = (page: Page): Locator =>
  page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Correo', exact: true }) });

async function abrirTodas(page: Page): Promise<void> {
  await page.getByText('Notificaciones', { exact: true }).click();
  await page.getByText('Todas', { exact: true }).click();
  await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
}

test.describe('Notificaciones — Todas', () => {
  test('Una notificación ya creada no se puede modificar ni borrar', async ({ page }) => {
    await ensureLoggedOut(page);
    // Paso 1: Dado que el administrador ha iniciado sesión y da de alta el correo
    //         de referencia con el motivo «Correo inmutable».
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

    // Paso 2: Cuando en «Todas» pulsa la fila «Correo inmutable».
    const fila = page.getByRole('row', { name: MOTIVO });
    await expect(fila).toBeVisible();
    await fila.click();

    // Resultado esperado: el formulario muestra motivo, centro, DNI, nombre,
    // apellidos, «para», asunto y cuerpo, todos en solo lectura.
    const consulta = ventanaCorreo(page);
    const datos = consulta.getByRole('region', { name: 'Datos del correo' });
    await expect(datos).toBeVisible();
    const motivo = datos.getByRole('textbox', { name: /^Motivo/ });
    await expect(motivo).toHaveValue(MOTIVO);
    await expect(motivo).toBeDisabled();
    // El centro, en solo lectura, se pinta como enlace-botón a su registro y no
    // como combobox editable.
    await expect(datos.getByRole('button', { name: CORREO.centro, exact: true })).toBeVisible();
    await expect(datos.getByRole('combobox', { name: 'Centro' })).toHaveCount(0);
    const dni = datos.getByRole('textbox', { name: 'DNI del destinatario' });
    await expect(dni).toHaveValue(CORREO.dni);
    await expect(dni).toBeDisabled();
    const nombre = datos.getByRole('textbox', { name: 'Nombre', exact: true });
    await expect(nombre).toHaveValue(CORREO.nombre);
    await expect(nombre).toBeDisabled();
    const apellidos = datos.getByRole('textbox', { name: 'Apellidos' });
    await expect(apellidos).toHaveValue(CORREO.apellidos);
    await expect(apellidos).toBeDisabled();
    const para = datos.getByRole('textbox', { name: /^Para/ });
    await expect(para).toHaveValue(CORREO.para);
    await expect(para).toBeDisabled();
    const asunto = datos.getByRole('textbox', { name: 'Asunto' });
    await expect(asunto).toHaveValue(CORREO.asunto);
    await expect(asunto).toBeDisabled();
    // El cuerpo, en solo lectura, se pinta como texto tras su etiqueta, no como textbox.
    await expect(datos.getByRole('textbox', { name: 'Cuerpo' })).toHaveCount(0);
    await expect(datos).toContainText(new RegExp(`Cuerpo\\s*${literal(CORREO.cuerpo)}`));

    // Resultado esperado: muestra el botón «Salir» y no los botones «Guardar»,
    // «Cancelar» ni «Borrar».
    await expect(consulta.getByRole('button', { name: 'Salir' })).toBeVisible();
    await expect(consulta.getByRole('button', { name: 'Guardar' })).toHaveCount(0);
    await expect(consulta.getByRole('button', { name: 'Cancelar' })).toHaveCount(0);
    await expect(consulta.getByRole('button', { name: 'Borrar' })).toHaveCount(0);

    // El formulario se abre como ventana modal sobre el listado: se cierra antes
    // del logout para que el menú de usuario quede accesible.
    await consulta.getByRole('button', { name: 'Salir' }).click();
    await expect(consulta).toBeHidden();

    await logout(page);
  });
});
