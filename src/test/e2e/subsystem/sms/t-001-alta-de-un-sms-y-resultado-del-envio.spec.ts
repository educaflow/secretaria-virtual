import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-001 — Alta de un SMS y resultado del envío
// origen: ESC-001  |  verifica: V-Sms-001, V-Sms-003, V-Sms-004, V-Sms-005, V-Sms-007, V-Sms-009,
//   R-Sms-001, R-Sms-002, R-Sms-003, R-Sms-004, R-Sms-005, R-Sms-006, U-sms-todos-002, U-sms-todos-008
// fuente: .sdd/drafts/2026-09-30_19-35_subsistema-sms/test-e2e-desc/t-001-alta-de-un-sms-y-resultado-del-envio.desc.md
test.describe('SMS — Todos', () => {
  test('Alta de un SMS y resultado del envío', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    // Idempotencia (BD compartida, sin reset): el mensaje lleva un sufijo único por
    // ejecución para identificar SIEMPRE la fila que crea este run, aunque haya SMS de
    // «Alumno1» de ejecuciones anteriores.
    const mensaje = `Mañana no hay clase t001-${Date.now()}`;

    // Paso 1: Dado que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
    // (hecho arriba con login)

    // Paso 2: Cuando abre el menú "SMS" → "Todos" y pulsa "Nuevo SMS".
    await page.getByText('SMS', { exact: true }).click();
    await page.getByTestId('item:sms-todos-menuitem').click();
    await page.getByRole('button', { name: 'Nuevo SMS' }).click();

    // Paso 3: Y elige el centro "CIPFP Mislata", escribe el DNI «86862719E», el nombre «Alumno1»,
    // los apellidos «CIPFP Mislata», el teléfono «600111222» y el mensaje «Mañana no hay clase».
    // El label real incluye un icono de ayuda ("Centro ?"), de ahí el regex.
    await page.getByRole('combobox', { name: /^Centro\b/ }).click();
    await page.getByRole('option', { name: 'CIPFP Mislata' }).click();
    await page.getByLabel('DNI del destinatario').fill('86862719E');
    await page.getByLabel('Nombre', { exact: true }).fill('Alumno1');
    await page.getByLabel('Apellidos').fill('CIPFP Mislata');
    // El widget de teléfono internacional (bandera "Spain") arranca con el prefijo "+34" ya
    // escrito; el usuario teclea el número detrás. Un fill() sustituiría el prefijo y el widget
    // interpretaría los dígitos como otro país, por eso se teclea tras dejar solo el "+34".
    const telefono = page.getByPlaceholder('+34 123 456 789');
    await telefono.click();
    await telefono.press('ControlOrMeta+a');
    await telefono.press('Backspace');
    await telefono.pressSequentially('+34');
    await telefono.pressSequentially('600111222');
    await expect(telefono).toHaveValue('+34 600 111 222');
    await page.getByLabel('Mensaje').fill(mensaje);

    // Paso 4: Y pulsa "Guardar".
    await page.getByRole('button', { name: 'Guardar' }).click();

    // Paso 5: Entonces el sistema guarda el SMS y vuelve al listado, donde aparece el SMS de
    // «Alumno1» con el teléfono «+34600111222».
    await expect(page).toHaveURL(/\/list\//);
    const row = page.getByRole('row', { name: mensaje });
    await expect(row).toBeVisible();
    await expect(row).toContainText('Alumno1');
    // Resultado esperado: teléfono normalizado a formato internacional aunque se escribiera sin prefijo.
    await expect(row.getByRole('gridcell', { name: '+34600111222', exact: true })).toBeVisible();

    // Paso 6: Y espera unos segundos y recarga el listado.
    // El envío es asíncrono: se recarga en bucle hasta que el SMS sale de "Pendiente".
    await expect(async () => {
      await page.reload();
      await expect(page.getByRole('row', { name: mensaje }).getByRole('gridcell', { name: /^(Enviado|Fallido)$/ }))
        .toBeVisible({ timeout: 3_000 });
    }).toPass({ timeout: 60_000, intervals: [2_000] });

    // Resultado esperado: ya no está "Pendiente"; está "Enviado" con fecha de envío, o "Fallido"
    // sin fecha de envío. En la fila, "Fecha de creación" siempre tiene fecha; "Fecha de envío"
    // solo si se envió → dos fechas si Enviado, una sola si Fallido.
    const filaFinal = page.getByRole('row', { name: mensaje });
    await expect(filaFinal.getByRole('gridcell', { name: 'Pendiente', exact: true })).toHaveCount(0);
    const enviado = (await filaFinal.getByRole('gridcell', { name: 'Enviado', exact: true }).count()) > 0;
    const dosFechas = /\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}[\s\S]*\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}/;
    if (enviado) {
      await expect(filaFinal).toContainText(dosFechas);
    } else {
      await expect(filaFinal.getByRole('gridcell', { name: 'Fallido', exact: true })).toBeVisible();
      await expect(filaFinal).not.toContainText(dosFechas);
    }

    // Paso 7: Y pulsa sobre el SMS de «Alumno1».
    await filaFinal.click();
    await page.waitForURL(/\/edit\/\d+/);

    // Resultado esperado: el detalle se muestra en solo lectura con el panel "Datos del envío"
    // y 1 reintento; si está "Fallido", con la descripción del último fallo y sin fecha de envío.
    await expect(page.getByLabel('DNI del destinatario')).toBeDisabled();
    await expect(page.getByLabel('Nombre', { exact: true })).toBeDisabled();
    await expect(page.getByLabel('Apellidos')).toBeDisabled();
    await expect(page.getByRole('button', { name: 'Guardar' })).toHaveCount(0);
    const datosEnvio = page.getByRole('region', { name: 'Datos del envío' });
    await expect(datosEnvio).toBeVisible();
    await expect(datosEnvio.getByLabel('Número de reintentos')).toHaveValue('1');
    // El campo "Estado" es un combobox de solo lectura: su valor vive en el atributo value.
    if (enviado) {
      await expect(datosEnvio.getByRole('combobox', { name: 'Estado' })).toHaveValue('Enviado');
      await expect(datosEnvio.getByLabel('Fecha de envío', { exact: true })).not.toHaveValue('');
    } else {
      await expect(datosEnvio.getByRole('combobox', { name: 'Estado' })).toHaveValue('Fallido');
      // La descripción del fallo es un texto (no un input): label seguido de contenido no vacío.
      await expect(datosEnvio).toContainText(/Descripción del último fallo\??\s*\S+/);
      await expect(datosEnvio.getByText('Fecha de envío', { exact: true })).toHaveCount(0);
    }

    // Teardown: un SMS ya creado no se puede modificar ni borrar desde la UI (solo ofrece
    // "Reenviar" y "Salir": es un dato inmutable de auditoría, ver T-013). Se confía en el
    // mensaje único para no colisionar entre ejecuciones (excepción documentada §4.5).
    await page.getByRole('button', { name: 'Salir' }).click();

    await logout(page);
  });
});
