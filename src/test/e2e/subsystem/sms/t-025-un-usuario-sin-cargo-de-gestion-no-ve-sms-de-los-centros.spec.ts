import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-025 — Un usuario sin cargo de gestión no ve SMS de los centros
// origen: ESC-025  |  verifica: —
// fuente: .sdd/drafts/2026-09-30_19-35_subsistema-sms/test-e2e-desc/t-025-un-usuario-sin-cargo-de-gestion-no-ve-sms-de-los-centros.desc.md

// Da de alta un SMS desde el formulario "Nuevo SMS" del listado "Todos" y vuelve al listado.
async function altaSms(
  page: Page,
  datos: { centro: string; dni: string; nombre: string; apellidos: string; telefono: string; mensaje: string },
): Promise<void> {
  await page.getByRole('button', { name: 'Nuevo SMS' }).click();
  // El label real incluye un icono de ayuda ("Centro ?"), de ahí el regex.
  await page.getByRole('combobox', { name: /^Centro\b/ }).click();
  await page.getByRole('option', { name: datos.centro }).click();
  await page.getByLabel('DNI del destinatario').fill(datos.dni);
  await page.getByLabel('Nombre', { exact: true }).fill(datos.nombre);
  await page.getByLabel('Apellidos').fill(datos.apellidos);
  // El widget de teléfono internacional arranca con el prefijo "+34": se deja solo el
  // prefijo y se teclea el número detrás (un fill() lo reinterpretaría como otro país).
  const telefono = page.getByPlaceholder('+34 123 456 789');
  await telefono.click();
  await telefono.press('ControlOrMeta+a');
  await telefono.press('Backspace');
  await telefono.pressSequentially('+34');
  await telefono.pressSequentially(datos.telefono);
  await page.getByLabel('Mensaje').fill(datos.mensaje);
  await page.getByRole('button', { name: 'Guardar' }).click();
  await expect(page).toHaveURL(/\/list\//);
}

test.describe('SMS — Del centro', () => {
  test('Un usuario sin cargo de gestión no ve SMS de los centros', async ({ page }) => {
    test.setTimeout(120_000);
    await ensureLoggedOut(page);

    // Idempotencia (BD compartida, sin reset): el mensaje lleva un sufijo único por ejecución
    // para identificar SIEMPRE la fila que crea este run.
    // Teardown: un SMS ya creado no se puede modificar ni borrar desde la UI (dato inmutable
    // de auditoría, ver T-013); se confía en el sufijo único (excepción documentada §4.5).
    const aviso = `Aviso para Alumno2 t025-${Date.now()}`;

    // Paso 1: Dado que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
    await login(page, 'admin', 'admin');

    // Paso 2: Cuando abre el menú "SMS" → "Todos", pulsa "Nuevo SMS", elige el centro "CIPFP Mislata",
    // escribe el DNI «03532821K», el nombre «Alumno2», los apellidos «CIPFP Mislata», el teléfono
    // «600555666» y el mensaje «Aviso para Alumno2», y pulsa "Guardar".
    await page.getByText('SMS', { exact: true }).click();
    await page.getByTestId('item:sms-todos-menuitem').click();
    await altaSms(page, {
      centro: 'CIPFP Mislata', dni: '03532821K', nombre: 'Alumno2', apellidos: 'CIPFP Mislata',
      telefono: '600555666', mensaje: aviso,
    });

    // Paso 3: Y comprueba que el SMS «Aviso para Alumno2» aparece en el listado, y cierra sesión.
    await expect(page.getByRole('row', { name: aviso })).toBeVisible();
    await logout(page);

    // Paso 4: Y el alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234» y despliega el menú "SMS".
    await login(page, 'alumno1@mislata.es', 'demo1234');
    await page.getByText('SMS', { exact: true }).click();
    const delCentro = page.getByTestId('item:sms-delCentro-menuitem');
    await expect(delCentro).toBeVisible();

    // Resultado esperado: el menú "SMS" no muestra la entrada "Todos".
    await expect(page.getByTestId('item:sms-todos-menuitem')).toHaveCount(0);
    await expect(page.getByTestId('item:sms-menuitem').getByText('Todos', { exact: true })).toHaveCount(0);

    // Paso 5: Y abre el menú "SMS" → "Del centro".
    await delCentro.click();
    await expect(page).toHaveURL(/\/list\//);
    const grid = page.getByRole('tabpanel', { name: 'SMS de mis centros' }).getByRole('grid');
    await expect(grid.getByRole('columnheader', { name: 'Mensaje' })).toBeVisible();

    // Resultado esperado: el listado de "Del centro" no muestra ningún SMS; en particular,
    // no muestra «Aviso para Alumno2» (ni el de este run ni los de runs anteriores).
    await expect(grid.getByRole('row', { name: 'No se encontraron registros.' })).toBeVisible();
    await expect(grid.getByRole('row', { name: /Aviso para Alumno2/ })).toHaveCount(0);
    await expect(grid.getByRole('row').filter({ hasText: /Aviso/ })).toHaveCount(0);

    await logout(page);
  });
});
