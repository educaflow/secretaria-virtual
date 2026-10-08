import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-039 — Mensaje con acentos en el límite de un SMS
// origen: ESC-032  |  verifica: V-Sms-004
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-039-mensaje-con-acentos-en-el-limite-de-un-sms.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (lo
// verifica T-049), así que este test NO tiene teardown — excepción documentada
// del §4.5 del contrato. Para no colisionar con las notificaciones que dejan los
// runs anteriores (la BD compartida no se resetea), el motivo del «SMS de
// referencia» lleva un sufijo único por ejecución y las aserciones localizan la
// fila por ese motivo. Que al final haya EXACTAMENTE una fila con ese motivo
// prueba además que el primer «Guardar» (71 letras «á») no creó nada.
const MOTIVO = `Aviso de prueba de SMS t039-${Date.now()}`;
// «SMS de referencia» del .desc.md; el mensaje se sustituye por 71/70 letras «á».
const SMS = {
  centro: 'CIPFP Mislata',
  dni: '86862719E',
  nombre: 'Alumno1',
  apellidos: 'CIPFP Mislata',
  telefono: '600111222',
};
const MENSAJE_71 = 'á'.repeat(71);
const MENSAJE_70 = 'á'.repeat(70);
const ERROR_LONGITUD =
  'El mensaje no cabe en un solo SMS: como máximo 160 caracteres, o 70 si contiene acentos u otros caracteres especiales';
// El destino del SMS es el teléfono normalizado: sin espacios y con el prefijo.
const DESTINO = '+34600111222';

// El listado se ordena por «Fecha de creación» descendente, así que la
// notificación recién creada está en la primera página.
const filasDelSms = (page: Page): Locator => page.getByRole('row', { name: MOTIVO });

// Columnas del grid «Todas las notificaciones», en orden: Estado, Tipo, Motivo,
// DNI, Nombre, Apellidos, Destino, Centro, Expediente, Fecha de creación,
// Fecha de envío.
const COL = { tipo: 1, motivo: 2, destino: 6, centro: 7 };
const celda = (fila: Locator, col: number): Locator => fila.getByRole('gridcell').nth(col);

async function abrirTodas(page: Page): Promise<void> {
  await page.getByText('Notificaciones', { exact: true }).click();
  await page.getByText('Todas', { exact: true }).click();
  await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
}

test.describe('Notificaciones — Todas', () => {
  test('Mensaje con acentos en el límite de un SMS', async ({ page }) => {
    await ensureLoggedOut(page);
    // Paso 1: Dado que el administrador ha iniciado sesión y abre el alta de un SMS.
    await login(page, 'admin', 'admin');
    await abrirTodas(page);
    await page.getByRole('button', { name: 'Nueva notificación' }).click();
    const eleccion = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Elija el canal de la notificación' }) });
    await eleccion.getByRole('radio', { name: 'SMS' }).click();
    await eleccion.getByRole('button', { name: 'Continuar' }).click();

    const form = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'SMS', exact: true }) });
    await expect(form.getByRole('region', { name: 'Datos del SMS' })).toBeVisible();

    // Paso 2: Cuando rellena el SMS de referencia con un mensaje de 71 letras «á»
    //         y pulsa «Guardar».
    // El centro primero: es un many-to-one con autocompletado y su selección
    // re-renderiza el formulario.
    await form.getByRole('combobox', { name: 'Centro' }).fill(SMS.centro);
    await page.getByRole('option', { name: SMS.centro, exact: true }).click();
    await expect(form.getByRole('combobox', { name: 'Centro' })).toHaveValue(SMS.centro);
    await form.getByRole('textbox', { name: /^Motivo/ }).fill(MOTIVO);
    await form.getByRole('textbox', { name: 'DNI del destinatario' }).fill(SMS.dni);
    await form.getByRole('textbox', { name: 'Nombre', exact: true }).fill(SMS.nombre);
    await form.getByRole('textbox', { name: 'Apellidos' }).fill(SMS.apellidos);
    // El widget «phone» de Axelor no asocia la etiqueta «Teléfono» a su input, así
    // que se localiza por su placeholder. Se teclea carácter a carácter porque
    // el widget formatea el número al vuelo.
    const telefono = form.getByPlaceholder('+34 123 456 789');
    await telefono.click();
    await telefono.pressSequentially(SMS.telefono);
    await expect(telefono).toHaveValue(/600\s*111\s*222/);
    const mensaje = form.getByRole('textbox', { name: 'Mensaje' });
    await mensaje.fill(MENSAJE_71);
    await expect(mensaje).toHaveValue(MENSAJE_71);
    await form.getByRole('button', { name: 'Guardar' }).click();

    // Paso 3: Entonces el sistema muestra el error de longitud...
    // Axelor lo pinta en un diálogo de error modal encima del formulario.
    const errorDialog = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'La acción no se completó por los siguientes motivos' }) });
    await expect(errorDialog.getByRole('listitem')).toHaveText(ERROR_LONGITUD);
    await errorDialog.getByRole('button', { name: 'Aceptar' }).click();
    await expect(errorDialog).toBeHidden();
    // ...y no crea la notificación: el formulario sigue abierto sin guardar
    // (al final se comprueba además que solo existe una fila con el motivo).
    await expect(form).toBeVisible();
    await expect(mensaje).toHaveValue(MENSAJE_71);

    // Paso 4: Cuando borra un carácter (quedan 70 letras «á») y pulsa «Guardar».
    await mensaje.click();
    await mensaje.press('End');
    await mensaje.press('Backspace');
    await expect(mensaje).toHaveValue(MENSAJE_70);
    await form.getByRole('button', { name: 'Guardar' }).click();

    // Resultado esperado: el sistema guarda el SMS y el listado muestra tipo «SMS»,
    // motivo «Aviso de prueba de SMS» (el de este run), destino «+34600111222» y
    // centro «CIPFP Mislata».
    await expect(form).toBeHidden();
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
    const filas = filasDelSms(page);
    await expect(filas).toHaveCount(1);
    const fila = filas.first();
    await expect(celda(fila, COL.tipo)).toHaveText('SMS');
    await expect(celda(fila, COL.motivo)).toHaveText(MOTIVO);
    await expect(celda(fila, COL.destino)).toHaveText(DESTINO);
    await expect(celda(fila, COL.centro)).toHaveText(SMS.centro);

    await logout(page);
  });
});
