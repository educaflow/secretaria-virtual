import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-009 — Mensaje sin acentos en el límite de un SMS
// origen: ESC-009  |  verifica: V-Sms-008
// fuente: .sdd/drafts/2026-09-30_19-35_subsistema-sms/test-e2e-desc/t-009-mensaje-sin-acentos-en-el-limite-de-un-sms.desc.md

const MENSAJE_161 = 'a'.repeat(161);
const MENSAJE_160 = 'a'.repeat(160);

// Busca por la API REST de Axelor (con la sesión del navegador) los SMS cuyo mensaje es
// exactamente `mensaje`, del más nuevo al más antiguo. El mensaje del test es fijo (la letra
// «a» repetida) y no admite un sufijo único por ejecución sin cambiar su longitud, así que
// "se guarda" / "no se guarda" se comprueba comparando el recuento antes y después, y el SMS
// creado por este run se identifica como uno cuyo id no existía antes.
async function buscarSms(page: Page, mensaje: string): Promise<{ total: number; ids: number[]; telefonos: string[] }> {
  const csrf = (await page.context().cookies()).find((c) => c.name === 'CSRF-TOKEN')?.value ?? '';
  const respuesta = await page.request.post('/ws/rest/com.educaflow.subsystem.sms.db.Sms/search', {
    headers: { 'X-CSRF-Token': csrf },
    data: {
      fields: ['id', 'telefono'],
      sortBy: ['-id'],
      limit: 1000,
      data: { criteria: [{ fieldName: 'mensaje', operator: '=', value: mensaje }] },
    },
  });
  expect(respuesta.ok()).toBeTruthy();
  const cuerpo = await respuesta.json();
  expect(cuerpo.status).toBe(0);
  const filas: Array<{ id: number; telefono: string }> = cuerpo.data ?? [];
  return { total: cuerpo.total ?? 0, ids: filas.map((f) => f.id), telefonos: filas.map((f) => f.telefono) };
}

test.describe('SMS — Todos', () => {
  test('Mensaje sin acentos en el límite de un SMS', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
    await login(page, 'admin', 'admin');

    // Idempotencia (BD compartida, sin reset): línea base de SMS con cada uno de los dos
    // mensajes antes del alta. Teardown: un SMS ya creado no se puede borrar desde la UI
    // (dato inmutable de auditoría, ver T-013); se confía en el recuento antes/después y en
    // el id nuevo para no depender de SMS idénticos de ejecuciones anteriores (§4.5).
    const antes161 = await buscarSms(page, MENSAJE_161);
    const antes160 = await buscarSms(page, MENSAJE_160);

    // Paso 2: Cuando abre el menú "SMS" → "Todos" y pulsa "Nuevo SMS".
    await page.getByText('SMS', { exact: true }).click();
    await page.getByTestId('item:sms-todos-menuitem').click();
    await page.getByRole('button', { name: 'Nuevo SMS' }).click();

    // Paso 3: Y elige el centro "CIPFP Mislata", escribe el DNI del destinatario «95591733F», el
    // nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono «600111222» y como mensaje la
    // letra «a» repetida 161 veces.
    // El label real incluye un icono de ayuda ("Centro ?"), de ahí el regex.
    await page.getByRole('combobox', { name: /^Centro\b/ }).click();
    await page.getByRole('option', { name: 'CIPFP Mislata', exact: true }).click();
    await page.getByLabel('DNI del destinatario').fill('95591733F');
    await page.getByLabel('Nombre', { exact: true }).fill('Alumno1');
    await page.getByLabel('Apellidos').fill('CIPFP Mislata');
    // El widget de teléfono internacional arranca con "+34" ya escrito: se vacía, se teclea el
    // prefijo y detrás el número (un fill() lo interpretaría como otro país).
    const telefono = page.getByPlaceholder('+34 123 456 789');
    await telefono.click();
    await telefono.press('ControlOrMeta+a');
    await telefono.press('Backspace');
    await telefono.pressSequentially('+34');
    await telefono.pressSequentially('600111222');
    await expect(telefono).toHaveValue('+34 600 111 222');
    const mensaje = page.getByLabel('Mensaje');
    await mensaje.fill(MENSAJE_161);
    await expect(mensaje).toHaveValue(MENSAJE_161);

    // Paso 4: Y pulsa "Guardar".
    await page.getByRole('button', { name: 'Guardar' }).click();

    // Paso 5: Entonces el sistema muestra «El mensaje no cabe en un solo SMS: como máximo 160
    // caracteres, o 70 si contiene acentos u otros caracteres especiales» y no guarda el SMS.
    await expect(
      page.getByText(
        'El mensaje no cabe en un solo SMS: como máximo 160 caracteres, o 70 si contiene acentos u otros caracteres especiales',
      ),
    ).toBeVisible();
    await expect(page).toHaveURL(/\/edit/);
    expect((await buscarSms(page, MENSAJE_161)).total).toBe(antes161.total);
    expect((await buscarSms(page, MENSAJE_160)).total).toBe(antes160.total);
    // Cierra el aviso de error (si es un diálogo modal) para poder seguir editando.
    const aviso = page.getByRole('dialog').filter({ hasText: 'El mensaje no cabe en un solo SMS' });
    if (await aviso.isVisible()) {
      await aviso.getByRole('button', { name: /Aceptar|Cerrar|OK/i }).first().click();
      await expect(aviso).toBeHidden();
    }

    // Paso 6: Cuando borra una letra para dejar el mensaje con la letra «a» repetida 160 veces y
    // pulsa "Guardar".
    await mensaje.click();
    await mensaje.press('ControlOrMeta+End');
    await mensaje.press('Backspace');
    await expect(mensaje).toHaveValue(MENSAJE_160);
    await page.getByRole('button', { name: 'Guardar' }).click();

    // Resultado esperado: el sistema guarda el SMS y vuelve al listado.
    await expect(page).toHaveURL(/\/list\//);
    await expect(page.getByRole('button', { name: 'Nuevo SMS' })).toBeVisible();
    const despues160 = await buscarSms(page, MENSAJE_160);
    expect(despues160.total).toBe(antes160.total + 1);
    expect((await buscarSms(page, MENSAJE_161)).total).toBe(antes161.total);
    const idNuevo = despues160.ids.find((id) => !antes160.ids.includes(id));
    expect(idNuevo).toBeDefined();
    expect(despues160.telefonos[despues160.ids.indexOf(idNuevo!)]).toBe('+34600111222');

    // Resultado esperado: el SMS aparece con el teléfono «+34600111222» y como mensaje la letra
    // «a» repetida 160 veces.
    const fila = page
      .getByRole('row')
      .filter({ has: page.getByRole('gridcell', { name: MENSAJE_160, exact: true }) })
      .filter({ has: page.getByRole('gridcell', { name: '+34600111222', exact: true }) })
      .first();
    await expect(fila).toBeVisible();

    await logout(page);
  });
});
