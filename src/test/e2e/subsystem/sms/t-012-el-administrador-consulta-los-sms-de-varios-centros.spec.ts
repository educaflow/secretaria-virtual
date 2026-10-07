import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-012 — El administrador consulta los SMS de varios centros
// origen: ESC-012  |  verifica: V-Sms-010, R-Sms-002
// fuente: .sdd/drafts/2026-09-30_19-35_subsistema-sms/test-e2e-desc/t-012-el-administrador-consulta-los-sms-de-varios-centros.desc.md

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
  await expect(page.getByRole('row', { name: datos.mensaje })).toBeVisible();
}

test.describe('SMS — Todos', () => {
  test('El administrador consulta los SMS de varios centros', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    // Idempotencia (BD compartida, sin reset): los mensajes llevan un sufijo único por
    // ejecución para identificar SIEMPRE las filas que crea este run.
    // Teardown: un SMS ya creado no se puede modificar ni borrar desde la UI (dato inmutable
    // de auditoría, ver T-013); se confía en el sufijo único (excepción documentada §4.5).
    const run = Date.now();
    const avisoMislata = `Aviso Mislata t012-${run}`;
    const avisoBatoi = `Aviso Batoi t012-${run}`;

    // Paso 1: Dado que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
    // (hecho arriba con login)

    // Paso 2: Cuando abre el menú "SMS" → "Todos", pulsa "Nuevo SMS", elige el centro "CIPFP Mislata",
    // escribe el DNI «95591733F», el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono
    // «600111222» y el mensaje «Aviso Mislata», y pulsa "Guardar".
    await page.getByText('SMS', { exact: true }).click();
    await page.getByTestId('item:sms-todos-menuitem').click();
    await altaSms(page, {
      centro: 'CIPFP Mislata', dni: '95591733F', nombre: 'Alumno1', apellidos: 'CIPFP Mislata',
      telefono: '600111222', mensaje: avisoMislata,
    });

    // Paso 3: Y pulsa "Nuevo SMS", elige el centro "CIPFP Batoi", escribe el DNI «92898219T», el nombre
    // «Alumno1», los apellidos «CIPFP Batoi», el teléfono «600333444» y el mensaje «Aviso Batoi»,
    // y pulsa "Guardar".
    await altaSms(page, {
      centro: 'CIPFP Batoi', dni: '92898219T', nombre: 'Alumno1', apellidos: 'CIPFP Batoi',
      telefono: '600333444', mensaje: avisoBatoi,
    });

    // Resultado esperado: el sistema muestra los dos SMS en el listado, aunque sean de centros distintos.
    const filaBatoi = page.getByRole('row', { name: avisoBatoi });
    const filaMislata = page.getByRole('row', { name: avisoMislata });
    await expect(filaBatoi).toBeVisible();
    await expect(filaMislata).toBeVisible();
    // Los apellidos del destinatario también son «CIPFP …»: el centro se comprueba en su columna.
    await expect(filaBatoi.getByTestId('column:centro')).toHaveText('CIPFP Batoi');
    await expect(filaMislata.getByTestId('column:centro')).toHaveText('CIPFP Mislata');

    // Resultado esperado: «Aviso Batoi», con el centro "CIPFP Batoi", aparece en la primera fila, y
    // «Aviso Mislata», con el centro "CIPFP Mislata", en la siguiente: del más reciente al más antiguo.
    // Filas de datos = las de testid "row:<id>" (se excluyen la cabecera y la fila de búsqueda
    // "search-row" del grid de Axelor, que también tiene celdas).
    const filasDatos = page.locator('[role="row"][data-testid^="row:"]');
    await expect(filasDatos.nth(0)).toContainText(avisoBatoi);
    await expect(filasDatos.nth(0).getByTestId('column:centro')).toHaveText('CIPFP Batoi');
    await expect(filasDatos.nth(1)).toContainText(avisoMislata);
    await expect(filasDatos.nth(1).getByTestId('column:centro')).toHaveText('CIPFP Mislata');

    await logout(page);
  });
});
