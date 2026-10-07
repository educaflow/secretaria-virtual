import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-002 — Alta con el teléfono escrito ya en formato internacional
// origen: ESC-002  |  verifica: V-Sms-006, R-Sms-001
// fuente: .sdd/drafts/2026-09-30_19-35_subsistema-sms/test-e2e-desc/t-002-alta-con-el-telefono-escrito-ya-en-formato-internacional.desc.md
test.describe('SMS — Todos', () => {
  test('Alta con el teléfono escrito ya en formato internacional', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    // Idempotencia (BD compartida, sin reset): el mensaje lleva un sufijo único por
    // ejecución para identificar SIEMPRE la fila que crea este run, aunque haya SMS
    // con el mismo texto de ejecuciones anteriores.
    const mensaje = `Recogida de notas el viernes t002-${Date.now()}`;

    // Paso 1: Dado que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
    // (hecho arriba con login)

    // Paso 2: Cuando abre el menú "SMS" → "Todos" y pulsa "Nuevo SMS".
    await page.getByText('SMS', { exact: true }).click();
    await page.getByTestId('item:sms-todos-menuitem').click();
    await page.getByRole('button', { name: 'Nuevo SMS' }).click();

    // Paso 3: Y elige el centro "CIPFP Mislata", escribe el DNI del destinatario «95591733F», el nombre
    // «Alumno1», los apellidos «CIPFP Mislata», el teléfono «+34600111222» y el mensaje
    // «Recogida de notas el viernes».
    // El label real incluye un icono de ayuda ("Centro ?"), de ahí el regex.
    await page.getByRole('combobox', { name: /^Centro\b/ }).click();
    await page.getByRole('option', { name: 'CIPFP Mislata' }).click();
    await page.getByLabel('DNI del destinatario').fill('95591733F');
    await page.getByLabel('Nombre', { exact: true }).fill('Alumno1');
    await page.getByLabel('Apellidos').fill('CIPFP Mislata');
    // El widget de teléfono internacional arranca con "+34" ya escrito: se vacía y se teclea
    // el número completo con su prefijo internacional, tal cual lo escribe el usuario.
    const telefono = page.getByPlaceholder('+34 123 456 789');
    await telefono.click();
    await telefono.press('ControlOrMeta+a');
    await telefono.press('Backspace');
    await telefono.pressSequentially('+34600111222');
    await expect(telefono).toHaveValue('+34 600 111 222');
    await page.getByLabel('Mensaje').fill(mensaje);

    // Paso 4: Y pulsa "Guardar".
    await page.getByRole('button', { name: 'Guardar' }).click();

    // Resultado esperado: el sistema guarda el SMS y vuelve al listado.
    await expect(page).toHaveURL(/\/list\//);
    const row = page.getByRole('row', { name: mensaje });
    await expect(row).toBeVisible();

    // Resultado esperado: el SMS «Recogida de notas el viernes» aparece con el teléfono
    // «+34600111222» (sin duplicar ni alterar el prefijo).
    await expect(row).toContainText('Recogida de notas el viernes');
    await expect(row.getByRole('gridcell', { name: '+34600111222', exact: true })).toBeVisible();
    await expect(row).not.toContainText('+34+34');
    await expect(row).not.toContainText('+3434');

    // Teardown: un SMS ya creado no se puede modificar ni borrar desde la UI (es un dato
    // inmutable de auditoría, ver T-013). Se confía en el mensaje único para no colisionar
    // entre ejecuciones (excepción documentada §4.5 de generation.md).

    await logout(page);
  });
});
