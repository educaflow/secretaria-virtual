import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-007 — Alta con un teléfono que no es un móvil de España
// origen: ESC-007  |  verifica: V-Sms-006
// fuente: .sdd/drafts/2026-09-30_19-35_subsistema-sms/test-e2e-desc/t-007-alta-con-un-telefono-que-no-es-un-movil-de-espana.desc.md
test.describe('SMS — Todos', () => {
  test('Alta con un teléfono que no es un móvil de España', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    // Idempotencia (BD compartida, sin reset): el mensaje lleva un sufijo único por
    // ejecución, así la comprobación "no se guarda" mira SOLO lo que intenta crear este
    // run y no se confunde con SMS de otros tests o ejecuciones con el mismo texto.
    // No hay teardown: el test no debe crear nada (si lo crease, la aserción falla).
    const mensaje = `Mañana no hay clase t007-${Date.now()}`;
    const ERROR_TELEFONO = 'El teléfono debe ser un número de móvil de España válido (por ejemplo, 600111222)';

    // La validación de negocio del servicio sale en el diálogo de error de Axelor
    // («La acción no se completó por los siguientes motivos»), que hay que cerrar con "Aceptar".
    const errores = page.getByRole('dialog').filter({ hasText: 'La acción no se completó por los siguientes motivos' });
    const esperarErrorTelefonoYCerrar = async (p: Page) => {
      await expect(errores.getByRole('listitem').filter({ hasText: ERROR_TELEFONO })).toBeVisible();
      await errores.getByRole('button', { name: 'Aceptar' }).click();
      await expect(errores).toHaveCount(0);
      // Sigue en el formulario de alta: no ha vuelto al listado (no se ha guardado).
      await expect(p).toHaveURL(/\/edit/);
    };

    // Paso 1: Dado que el administrador ha iniciado sesión con usuario «admin» y contraseña «admin».
    // (hecho arriba con login)

    // Paso 2: Cuando abre el menú "SMS" → "Todos" y pulsa "Nuevo SMS".
    await page.getByText('SMS', { exact: true }).click();
    await page.getByTestId('item:sms-todos-menuitem').click();
    await page.getByRole('button', { name: 'Nuevo SMS' }).click();

    // Paso 3: Y elige el centro "CIPFP Mislata", escribe el DNI del destinatario «86862719E»,
    // el nombre «Alumno1», los apellidos «CIPFP Mislata», el teléfono fijo «963000000» y el
    // mensaje «Mañana no hay clase».
    // El label real incluye un icono de ayuda ("Centro ?"), de ahí el regex.
    await page.getByRole('combobox', { name: /^Centro\b/ }).click();
    await page.getByRole('option', { name: 'CIPFP Mislata', exact: true }).click();
    await page.getByLabel('DNI del destinatario').fill('86862719E');
    await page.getByLabel('Nombre', { exact: true }).fill('Alumno1');
    await page.getByLabel('Apellidos').fill('CIPFP Mislata');
    // El widget de teléfono internacional pone el prefijo "+34" al enfocarlo: se enfoca y se
    // teclea el número local tal cual lo escribe el usuario. (Su nombre accesible cambia según
    // el tipo de número detectado —"Línea fija"—, por eso se localiza por placeholder.)
    const telefono = page.getByPlaceholder('+34 123 456 789');
    await telefono.click();
    await telefono.pressSequentially('963000000');
    await expect(telefono).toHaveValue('+34 963 000 000');
    await page.getByLabel('Mensaje').fill(mensaje);

    // Paso 4: Y pulsa "Guardar".
    await page.getByRole('button', { name: 'Guardar' }).click();

    // Paso 5: Entonces el sistema muestra «El teléfono debe ser un número de móvil de España
    // válido (por ejemplo, 600111222)» y no guarda el SMS.
    await esperarErrorTelefonoYCerrar(page);

    // Paso 6: Cuando cambia el teléfono por el número incompleto «60011» y pulsa "Guardar".
    // Al vaciar el campo el widget pierde también el prefijo "+34" (sin él interpretaría
    // «60011» como prefijo +60), así que se vuelve a teclear con el prefijo de España.
    await telefono.click();
    await telefono.press('ControlOrMeta+a');
    await telefono.press('Backspace');
    await telefono.pressSequentially('+3460011');
    await expect(telefono).toHaveValue('+34 600 11');
    await page.getByRole('button', { name: 'Guardar' }).click();

    // Resultado esperado: el sistema vuelve a mostrar «El teléfono debe ser un número de móvil
    // de España válido (por ejemplo, 600111222)».
    await esperarErrorTelefonoYCerrar(page);

    // Resultado esperado: no se guarda el SMS en ninguno de los dos intentos.
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
