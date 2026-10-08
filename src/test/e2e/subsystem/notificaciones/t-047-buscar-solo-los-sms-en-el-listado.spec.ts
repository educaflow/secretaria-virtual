import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-047 — Buscar solo los SMS en el listado
// origen: ESC-038  |  verifica: —
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-047-buscar-solo-los-sms-en-el-listado.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (lo
// verifican T-046 y T-049), así que este test NO tiene teardown — excepción
// documentada del §4.5 del contrato. Para no colisionar con las notificaciones
// que dejan los runs anteriores (la BD compartida no se resetea), los motivos
// «Correo búsqueda» y «SMS búsqueda» llevan un sufijo único por ejecución y las
// aserciones localizan cada fila por su motivo.
const RUN = Date.now();
const MOTIVO_CORREO = `Correo búsqueda t047-${RUN}`;
const MOTIVO_SMS = `SMS búsqueda t047-${RUN}`;

// Correo de referencia (con el motivo «Correo búsqueda»).
const CORREO = {
  centro: 'CIPFP Mislata',
  dni: '86862719E',
  nombre: 'Alumno1',
  apellidos: 'CIPFP Mislata',
  para: 'alumno1@mislata.es',
  asunto: 'Reunión de inicio de curso',
  cuerpo: 'La reunión será el lunes a las 10:00.',
};

// SMS de referencia (con el motivo «SMS búsqueda»).
const SMS = {
  centro: 'CIPFP Mislata',
  dni: '86862719E',
  nombre: 'Alumno1',
  apellidos: 'CIPFP Mislata',
  telefono: '600111222',
  mensaje: 'Mañana no hay clase',
};

const fila = (page: Page, motivo: string): Locator => page.getByRole('row', { name: motivo });

async function abrirTodas(page: Page): Promise<void> {
  await page.getByText('Notificaciones', { exact: true }).click();
  await page.getByText('Todas', { exact: true }).click();
  await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
}

async function elegirCanal(page: Page, canal: 'Correo' | 'SMS'): Promise<Locator> {
  await page.getByRole('button', { name: 'Nueva notificación' }).click();
  const eleccion = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Elija el canal de la notificación' }) });
  await eleccion.getByRole('radio', { name: canal }).click();
  await eleccion.getByRole('button', { name: 'Continuar' }).click();
  return page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: canal, exact: true }) });
}

async function elegirCentro(page: Page, form: Locator, centro: string): Promise<void> {
  // El centro primero: es un many-to-one con autocompletado y su selección
  // re-renderiza el formulario.
  await form.getByRole('combobox', { name: 'Centro' }).fill(centro);
  await page.getByRole('option', { name: centro, exact: true }).click();
  await expect(form.getByRole('combobox', { name: 'Centro' })).toHaveValue(centro);
}

async function guardar(page: Page, form: Locator): Promise<void> {
  await form.getByRole('button', { name: 'Guardar' }).click();
  await expect(form).toBeHidden();
  await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
}

test.describe('Notificaciones — Todas', () => {
  test('Buscar solo los SMS en el listado', async ({ page }) => {
    await ensureLoggedOut(page);
    // Paso 1: Dado que el administrador ha iniciado sesión...
    await login(page, 'admin', 'admin');

    // ...y da de alta el correo de referencia con el motivo «Correo búsqueda»...
    await abrirTodas(page);
    const formCorreo = await elegirCanal(page, 'Correo');
    await expect(formCorreo.getByRole('region', { name: 'Datos del correo' })).toBeVisible();
    await elegirCentro(page, formCorreo, CORREO.centro);
    await formCorreo.getByRole('textbox', { name: 'Motivo' }).fill(MOTIVO_CORREO);
    await formCorreo.getByRole('textbox', { name: 'DNI del destinatario' }).fill(CORREO.dni);
    await formCorreo.getByRole('textbox', { name: 'Nombre', exact: true }).fill(CORREO.nombre);
    await formCorreo.getByRole('textbox', { name: 'Apellidos' }).fill(CORREO.apellidos);
    await formCorreo.getByRole('textbox', { name: /^Para/ }).fill(CORREO.para);
    await formCorreo.getByRole('textbox', { name: 'Asunto' }).fill(CORREO.asunto);
    await formCorreo.getByRole('textbox', { name: 'Cuerpo' }).fill(CORREO.cuerpo);
    await guardar(page, formCorreo);

    // ...y el SMS de referencia con el motivo «SMS búsqueda».
    const formSms = await elegirCanal(page, 'SMS');
    await expect(formSms.getByRole('region', { name: 'Datos del SMS' })).toBeVisible();
    await elegirCentro(page, formSms, SMS.centro);
    await formSms.getByRole('textbox', { name: 'Motivo' }).fill(MOTIVO_SMS);
    await formSms.getByRole('textbox', { name: 'DNI del destinatario' }).fill(SMS.dni);
    await formSms.getByRole('textbox', { name: 'Nombre', exact: true }).fill(SMS.nombre);
    await formSms.getByRole('textbox', { name: 'Apellidos' }).fill(SMS.apellidos);
    // El widget «phone» de Axelor no asocia la etiqueta «Teléfono» a su input,
    // así que se localiza por su placeholder. Se teclea carácter a carácter
    // porque el widget formatea el número al vuelo.
    const telefono = formSms.getByPlaceholder('+34 123 456 789');
    await telefono.click();
    await telefono.pressSequentially(SMS.telefono);
    await expect(telefono).toHaveValue(/600\s*111\s*222/);
    await formSms.getByRole('textbox', { name: 'Mensaje' }).fill(SMS.mensaje);
    await guardar(page, formSms);

    // Se recarga para partir del listado tal cual lo sirve el servidor (orden
    // por «Fecha de creación» descendente: las dos recién creadas están en la
    // primera página). Sin filtro se ven las dos: así la ausencia del correo
    // tras filtrar se debe al filtro y no a que no se hubiera creado.
    await page.reload();
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
    await expect(fila(page, MOTIVO_CORREO)).toBeVisible();
    await expect(fila(page, MOTIVO_SMS)).toBeVisible();

    // Paso 2: Cuando en «Todas» filtra la columna tipo por «SMS».
    // El filtro de la columna «Tipo de notificación» es un selector (Correo /
    // SMS) que aplica la búsqueda al elegir la opción.
    // Al elegir la opción el input pierde su placeholder «Buscar...» (y con él
    // su nombre accesible), así que se localiza por la columna, no por nombre.
    const filtroTipo = page.getByTestId('column:tipoNotificacion').getByRole('combobox');
    await filtroTipo.click();
    await page.getByRole('option', { name: 'SMS', exact: true }).click();
    await expect(filtroTipo).toHaveValue('SMS');

    // Resultado esperado: el listado muestra «SMS búsqueda»...
    await expect(fila(page, MOTIVO_SMS)).toBeVisible();
    // ...y no «Correo búsqueda».
    await expect(fila(page, MOTIVO_CORREO)).toHaveCount(0);

    await logout(page);
  });
});
