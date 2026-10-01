import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-006 — Alta sin teléfono
// origen: ESC-006  |  verifica: V-Sms-005
// fuente: .sdd/drafts/2026-09-30_19-35_subsistema-sms/test-e2e-desc/t-006-alta-sin-telefono.desc.md
test.describe('SMS — Todos', () => {
  test('Alta sin teléfono', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    // Idempotencia (BD compartida, sin reset): el mensaje lleva un sufijo único por
    // ejecución, así la comprobación "no se guarda" mira SOLO lo que intenta crear este
    // run y no se confunde con SMS de otros tests o ejecuciones con el mismo texto.
    // No hay teardown: el test no debe crear nada (si lo crease, la aserción falla).
    const mensaje = `Mañana no hay clase t006-${Date.now()}`;

    // Paso 1: Dado que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
    // (hecho arriba con login)

    // Paso 2: Cuando abre el menú "SMS" → "Todos" y pulsa "Nuevo SMS".
    await page.getByText('SMS', { exact: true }).click();
    await page.getByTestId('item:sms-todos-menuitem').click();
    await page.getByRole('button', { name: 'Nuevo SMS' }).click();

    // Paso 3: Y elige el centro "CIPFP Mislata", escribe el DNI del destinatario «86862719E»,
    // el nombre «Alumno1», los apellidos «CIPFP Mislata», deja vacío el teléfono y escribe el
    // mensaje «Mañana no hay clase».
    // El label real incluye un icono de ayuda ("Centro ?"), de ahí el regex.
    await page.getByRole('combobox', { name: /^Centro\b/ }).click();
    await page.getByRole('option', { name: 'CIPFP Mislata', exact: true }).click();
    await page.getByLabel('DNI del destinatario').fill('86862719E');
    await page.getByLabel('Nombre', { exact: true }).fill('Alumno1');
    await page.getByLabel('Apellidos').fill('CIPFP Mislata');
    // El teléfono se deja vacío: no se toca. El widget de teléfono internacional muestra por sí
    // solo el prefijo de país "+34 " sin ningún número, que es su estado vacío.
    await expect(page.getByPlaceholder('+34 123 456 789')).toHaveValue(/^(\+34\s*)?$/);
    await page.getByLabel('Mensaje').fill(mensaje);

    // Paso 4: Y pulsa "Guardar".
    await page.getByRole('button', { name: 'Guardar' }).click();

    // Resultado esperado: el sistema muestra «El teléfono es obligatorio».
    await expect(page.getByText('El teléfono es obligatorio')).toBeVisible();
    // Sigue en el formulario de alta: no ha vuelto al listado.
    await expect(page).toHaveURL(/\/edit/);

    // Resultado esperado: no se guarda el SMS.
    // Se vuelve al listado con el botón "Cancelar" del propio formulario de alta.
    await page.getByRole('button', { name: 'Cancelar' }).click();
    // Axelor pregunta si se descartan los cambios sin guardar: se acepta.
    const dialogo = page.getByRole('dialog').filter({ hasText: 'Los cambios actuales se perderán' });
    await dialogo.getByRole('button', { name: 'Aceptar' }).click();
    await expect(page).toHaveURL(/\/list\//);
    await expect(page.getByRole('button', { name: 'Nuevo SMS' })).toBeVisible();
    // Esperar a que el grid esté pintado antes de comprobar la ausencia (evita un falso verde).
    await expect(page.getByRole('columnheader').first()).toBeVisible();
    await expect(page.getByRole('row', { name: mensaje })).toHaveCount(0);
    await expect(page.getByText(mensaje)).toHaveCount(0);

    await logout(page);
  });
});
