import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-023 — La elección de canal no deja continuar hasta elegir uno
// origen: ESC-058  |  verifica: U-notificaciones-todas-001, U-notificaciones-todas-002
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-023-la-eleccion-de-canal-no-deja-continuar-hasta-elegir-uno.desc.md

// Idempotencia: el test no guarda nada (abre el formulario de alta del correo y lo
// cierra sin «Guardar»), así que no crea datos y no necesita nombres únicos ni teardown.

async function abrirTodas(page: Page): Promise<void> {
  await page.getByText('Notificaciones', { exact: true }).click();
  await page.getByText('Todas', { exact: true }).click();
  await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
}

test.describe('Notificaciones — Todas', () => {
  test('La elección de canal no deja continuar hasta elegir uno', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    // Paso 1: Dado que el administrador ha iniciado sesión y abre «Notificaciones» → «Todas».
    await abrirTodas(page);

    // Paso 2: Cuando pulsa «Nueva notificación».
    await page.getByRole('button', { name: 'Nueva notificación' }).click();
    const eleccion = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Elija el canal de la notificación' }) });
    await expect(eleccion).toBeVisible();

    // Paso 3: Entonces la ventana de elección de canal aparece sin ningún canal elegido
    //         y con «Continuar» no disponible.
    const canales = eleccion.getByRole('radio');
    await expect(canales.first()).toBeVisible();
    await expect(eleccion.getByRole('radio', { name: 'Correo' })).toBeVisible();
    await expect(eleccion.getByRole('radio', { name: 'SMS' })).toBeVisible();
    const numCanales = await canales.count();
    for (let i = 0; i < numCanales; i++) {
      await expect(canales.nth(i)).not.toBeChecked();
    }
    const continuar = eleccion.getByRole('button', { name: 'Continuar' });
    await expect(continuar).toBeVisible();
    await expect(continuar).toBeDisabled();

    // Paso 4: Cuando elige «Correo».
    await eleccion.getByRole('radio', { name: 'Correo' }).click();
    await expect(eleccion.getByRole('radio', { name: 'Correo' })).toBeChecked();

    // Paso 5: Entonces «Continuar» queda disponible.
    await expect(continuar).toBeEnabled();

    // Paso 6: Cuando pulsa «Continuar».
    await continuar.click();

    // Resultado esperado: el sistema abre el formulario de alta del correo.
    const form = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Correo', exact: true }) });
    await expect(form).toBeVisible();
    await expect(form.getByRole('region', { name: 'Datos del correo' })).toBeVisible();
    await expect(form.getByRole('textbox', { name: 'Asunto' })).toBeVisible();
    await expect(form.getByRole('textbox', { name: 'Asunto' })).toBeEditable();
    await expect(form.getByRole('button', { name: 'Guardar' })).toBeVisible();
    await expect(eleccion).toBeHidden();

    // Cierre sin guardar: no deja ninguna notificación creada.
    await form.getByRole('button', { name: 'Cancelar' }).click();
    await expect(form).toBeHidden();

    await logout(page);
  });
});
