import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-049 — Un SMS ya creado no se puede modificar ni borrar
// origen: ESC-068  |  verifica: V-Notificacion-012, V-Notificacion-013, U-notificaciones-todas-023, U-notificaciones-todas-027
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-049-un-sms-ya-creado-no-se-puede-modificar-ni-borrar.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (es
// justo lo que verifica este test), así que NO tiene teardown — excepción
// documentada del §4.5 del contrato. Para no colisionar con las notificaciones
// de runs anteriores (la BD compartida no se resetea), el motivo «SMS inmutable»
// lleva un sufijo único por ejecución y la fila se localiza por él.
const MOTIVO = `SMS inmutable t049-${Date.now()}`;

// SMS de referencia (con el motivo sustituido por MOTIVO).
const SMS = {
  centro: 'CIPFP Mislata',
  dni: '86862719E',
  nombre: 'Alumno1',
  apellidos: 'CIPFP Mislata',
  telefono: '600111222',
  mensaje: 'Mañana no hay clase',
};

// Escapa un literal para usarlo dentro de una RegExp.
const literal = (texto: string): string => texto.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

const ventanaSms = (page: Page): Locator =>
  page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'SMS', exact: true }) });

async function abrirTodas(page: Page): Promise<void> {
  await page.getByText('Notificaciones', { exact: true }).click();
  await page.getByText('Todas', { exact: true }).click();
  await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
}

test.describe('Notificaciones — Todas', () => {
  test('Un SMS ya creado no se puede modificar ni borrar', async ({ page }) => {
    await ensureLoggedOut(page);
    // Paso 1: Dado que el administrador ha iniciado sesión y da de alta el SMS
    //         de referencia con el motivo «SMS inmutable».
    await login(page, 'admin', 'admin');
    await abrirTodas(page);
    await page.getByRole('button', { name: 'Nueva notificación' }).click();
    const eleccion = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Elija el canal de la notificación' }) });
    await eleccion.getByRole('radio', { name: 'SMS' }).click();
    await eleccion.getByRole('button', { name: 'Continuar' }).click();

    const form = ventanaSms(page);
    await expect(form.getByRole('region', { name: 'Datos del SMS' })).toBeVisible();
    // El centro primero: es un many-to-one con autocompletado y su selección
    // re-renderiza el formulario.
    await form.getByRole('combobox', { name: 'Centro' }).fill(SMS.centro);
    await page.getByRole('option', { name: SMS.centro, exact: true }).click();
    await expect(form.getByRole('combobox', { name: 'Centro' })).toHaveValue(SMS.centro);
    await form.getByRole('textbox', { name: 'Motivo' }).fill(MOTIVO);
    await form.getByRole('textbox', { name: 'DNI del destinatario' }).fill(SMS.dni);
    await form.getByRole('textbox', { name: 'Nombre', exact: true }).fill(SMS.nombre);
    await form.getByRole('textbox', { name: 'Apellidos' }).fill(SMS.apellidos);
    // El widget «phone» de Axelor no asocia la etiqueta «Teléfono» a su input,
    // así que se localiza por su placeholder; se teclea carácter a carácter
    // porque el widget formatea el número al vuelo.
    const telefono = form.getByPlaceholder('+34 123 456 789');
    await telefono.click();
    await telefono.pressSequentially(SMS.telefono);
    await expect(telefono).toHaveValue(/600\s*111\s*222/);
    await form.getByRole('textbox', { name: 'Mensaje' }).fill(SMS.mensaje);
    await form.getByRole('button', { name: 'Guardar' }).click();
    await expect(form).toBeHidden();
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();

    // Paso 2: Cuando en el listado pulsa la fila «SMS inmutable».
    const fila = page.getByRole('row', { name: MOTIVO });
    await expect(fila).toBeVisible();
    await fila.click();

    const consulta = ventanaSms(page);
    const datos = consulta.getByRole('region', { name: 'Datos del SMS' });
    await expect(datos).toBeVisible();
    // Resultado esperado: el botón «Salir» está visible (se espera primero: el
    // formulario termina de pintarse en modo solo lectura cuando aparece).
    const salir = consulta.getByRole('button', { name: 'Salir' });
    await expect(salir).toBeVisible();

    // Resultado esperado: todos los datos del SMS aparecen en solo lectura.
    const motivo = datos.getByRole('textbox', { name: /^Motivo/ });
    await expect(motivo).toHaveValue(MOTIVO);
    await expect(motivo).toBeDisabled();
    // El centro, en solo lectura, se pinta como enlace-botón a su registro y no
    // como combobox editable.
    await expect(datos.getByRole('button', { name: SMS.centro, exact: true })).toBeVisible();
    await expect(datos.getByRole('combobox', { name: /^Centro/ })).toHaveCount(0);
    const dni = datos.getByRole('textbox', { name: 'DNI del destinatario' });
    await expect(dni).toHaveValue(SMS.dni);
    await expect(dni).toBeDisabled();
    const nombre = datos.getByRole('textbox', { name: 'Nombre', exact: true });
    await expect(nombre).toHaveValue(SMS.nombre);
    await expect(nombre).toBeDisabled();
    const apellidos = datos.getByRole('textbox', { name: 'Apellidos' });
    await expect(apellidos).toHaveValue(SMS.apellidos);
    await expect(apellidos).toBeDisabled();
    // El teléfono, en solo lectura, se pinta como enlace «tel:» con el número
    // normalizado, sin el input editable del widget.
    await expect(datos.getByRole('link', { name: '+34 600 111 222' })).toHaveAttribute('href', 'tel:+34600111222');
    await expect(datos.getByPlaceholder('+34 123 456 789')).toHaveCount(0);
    // El mensaje, en solo lectura, se pinta como texto tras su etiqueta, no como textbox.
    await expect(datos.getByRole('textbox', { name: 'Mensaje' })).toHaveCount(0);
    await expect(datos).toContainText(new RegExp(`Mensaje\\s*${literal(SMS.mensaje)}`));

    // Resultado esperado: sin los botones «Guardar», «Cancelar» ni «Borrar».
    await expect(consulta.getByRole('button', { name: 'Guardar' })).toHaveCount(0);
    await expect(consulta.getByRole('button', { name: 'Cancelar' })).toHaveCount(0);
    await expect(consulta.getByRole('button', { name: 'Borrar' })).toHaveCount(0);

    // El formulario se abre como ventana modal sobre el listado: se cierra antes
    // del logout para que el menú de usuario quede accesible.
    await salir.click();
    await expect(consulta).toBeHidden();

    await logout(page);
  });
});
