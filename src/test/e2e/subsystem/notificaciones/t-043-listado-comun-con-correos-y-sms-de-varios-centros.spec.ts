import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-043 — Listado común con correos y SMS de varios centros
// origen: ESC-034  |  verifica: R-Notificacion-001
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-043-listado-comun-con-correos-y-sms-de-varios-centros.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (lo
// verifican T-046 y T-049), así que este test NO tiene teardown — excepción
// documentada del §4.5 del contrato. Para no colisionar con las notificaciones
// que dejan los runs anteriores (la BD compartida no se resetea), los motivos
// «Correo Mislata» y «SMS Batoi» llevan un sufijo único por ejecución y todas
// las aserciones localizan cada fila por su motivo.
const RUN = Date.now();
const MOTIVO_CORREO = `Correo Mislata t043-${RUN}`;
const MOTIVO_SMS = `SMS Batoi t043-${RUN}`;

// Correo de referencia, con el motivo «Correo Mislata».
const CORREO = {
  centro: 'CIPFP Mislata',
  dni: '86862719E',
  nombre: 'Alumno1',
  apellidos: 'CIPFP Mislata',
  para: 'alumno1@mislata.es',
  asunto: 'Reunión de inicio de curso',
  cuerpo: 'La reunión será el lunes a las 10:00.',
};

// SMS del centro «CIPFP Batoi».
const SMS = {
  centro: 'CIPFP Batoi',
  dni: '65399546N',
  nombre: 'Alumno1',
  apellidos: 'CIPFP Batoi',
  telefono: '600222333',
  mensaje: 'Mañana no hay clase',
};
// El destino del SMS es el teléfono normalizado a formato internacional.
const DESTINO_SMS = '+34600222333';

// Columnas del grid «Todas las notificaciones», en orden: Estado, Tipo, Motivo,
// DNI, Nombre, Apellidos, Destino, Centro, Expediente, Fecha de creación,
// Fecha de envío.
const COL = { tipo: 1, motivo: 2, destino: 6, centro: 7 };
const celda = (fila: Locator, col: number): Locator => fila.getByRole('gridcell').nth(col);
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
  test('Listado común con correos y SMS de varios centros', async ({ page }) => {
    await ensureLoggedOut(page);
    // Paso 1: Dado que el administrador ha iniciado sesión.
    await login(page, 'admin', 'admin');

    // Paso 2: Cuando da de alta un correo (correo de referencia con el motivo
    //         «Correo Mislata»).
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

    // Paso 3: Y da de alta un SMS del centro «CIPFP Batoi» con el motivo
    //         «SMS Batoi», DNI «65399546N», nombre «Alumno1», apellidos
    //         «CIPFP Batoi», teléfono «600222333» y mensaje «Mañana no hay clase».
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
    await expect(telefono).toHaveValue(/600\s*222\s*333/);
    await formSms.getByRole('textbox', { name: 'Mensaje' }).fill(SMS.mensaje);
    await guardar(page, formSms);

    // Paso 4: Y abre «Notificaciones» → «Todas».
    // Se recarga para partir del listado tal cual lo sirve el servidor (orden
    // por «Fecha de creación» descendente: las dos recién creadas están en la
    // primera página).
    await page.reload();
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();

    // Resultado esperado: el listado muestra «Correo Mislata» con tipo «Correo»,
    // destino «alumno1@mislata.es» y centro «CIPFP Mislata»...
    const filaCorreo = fila(page, MOTIVO_CORREO);
    await expect(filaCorreo).toBeVisible();
    await expect(celda(filaCorreo, COL.motivo)).toHaveText(MOTIVO_CORREO);
    await expect(celda(filaCorreo, COL.tipo)).toHaveText('Correo');
    await expect(celda(filaCorreo, COL.destino)).toHaveText(CORREO.para);
    await expect(celda(filaCorreo, COL.centro)).toHaveText(CORREO.centro);

    // ...y «SMS Batoi» con tipo «SMS», destino «+34600222333» y centro
    // «CIPFP Batoi».
    const filaSms = fila(page, MOTIVO_SMS);
    await expect(filaSms).toBeVisible();
    await expect(celda(filaSms, COL.motivo)).toHaveText(MOTIVO_SMS);
    await expect(celda(filaSms, COL.tipo)).toHaveText('SMS');
    await expect(celda(filaSms, COL.destino)).toHaveText(DESTINO_SMS);
    await expect(celda(filaSms, COL.centro)).toHaveText(SMS.centro);

    await logout(page);
  });
});
