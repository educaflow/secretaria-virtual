import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-054 — El supervisor solo ve las notificaciones de su centro
// origen: ESC-041  |  verifica: —
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-054-el-supervisor-solo-ve-las-notificaciones-de-su-centro.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (lo
// verifican T-046 y T-049), así que este test NO tiene teardown — excepción
// documentada del §4.5 del contrato. Para no colisionar con las notificaciones
// que dejan los runs anteriores (la BD compartida no se resetea), los motivos
// «Aviso Mislata» y «Aviso Batoi» llevan un sufijo único por ejecución y las
// aserciones localizan cada fila por su motivo.
const RUN = Date.now();
const MOTIVO_MISLATA = `Aviso Mislata t054-${RUN}`;
const MOTIVO_BATOI = `Aviso Batoi t054-${RUN}`;

// Correo de referencia, con el motivo «Aviso Mislata».
const CORREO_MISLATA = {
  centro: 'CIPFP Mislata',
  motivo: MOTIVO_MISLATA,
  dni: '86862719E',
  nombre: 'Alumno1',
  apellidos: 'CIPFP Mislata',
  para: 'alumno1@mislata.es',
  asunto: 'Reunión de inicio de curso',
  cuerpo: 'La reunión será el lunes a las 10:00.',
};

// Correo de referencia cambiado al centro «CIPFP Batoi».
const CORREO_BATOI = {
  ...CORREO_MISLATA,
  centro: 'CIPFP Batoi',
  motivo: MOTIVO_BATOI,
  dni: '65399546N',
  apellidos: 'CIPFP Batoi',
  para: 'alumno1@batoi.es',
};

// Columnas del grid «Notificaciones del centro», en orden: Centro, Tipo, Estado,
// DNI del destinatario, Nombre, Apellidos, Motivo, Destino, Expediente,
// Fecha de creación, Fecha de envío.
const COL = { centro: 0, tipo: 1, motivo: 6, destino: 7 };
const celda = (fila: Locator, col: number): Locator => fila.getByRole('gridcell').nth(col);

async function abrirMenu(page: Page, submenu: string): Promise<void> {
  // Los ítems del menú lateral de Axelor no exponen rol ARIA; se acota por el
  // data-testid estable que Axelor deriva del nombre del menuitem.
  const menu = page.getByTestId('item:notificaciones-menuitem');
  await menu.getByText('Notificaciones', { exact: true }).click();
  await menu.getByText(submenu, { exact: true }).click();
}

async function altaCorreo(page: Page, correo: typeof CORREO_MISLATA): Promise<void> {
  await page.getByRole('button', { name: 'Nueva notificación' }).click();
  const eleccion = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Elija el canal de la notificación' }) });
  await eleccion.getByRole('radio', { name: 'Correo' }).click();
  await eleccion.getByRole('button', { name: 'Continuar' }).click();
  const form = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Correo', exact: true }) });
  await expect(form.getByRole('region', { name: 'Datos del correo' })).toBeVisible();
  // El centro primero: es un many-to-one con autocompletado y su selección
  // re-renderiza el formulario.
  await form.getByRole('combobox', { name: 'Centro' }).fill(correo.centro);
  await page.getByRole('option', { name: correo.centro, exact: true }).click();
  await expect(form.getByRole('combobox', { name: 'Centro' })).toHaveValue(correo.centro);
  await form.getByRole('textbox', { name: 'Motivo' }).fill(correo.motivo);
  await form.getByRole('textbox', { name: 'DNI del destinatario' }).fill(correo.dni);
  await form.getByRole('textbox', { name: 'Nombre', exact: true }).fill(correo.nombre);
  await form.getByRole('textbox', { name: 'Apellidos' }).fill(correo.apellidos);
  await form.getByRole('textbox', { name: /^Para/ }).fill(correo.para);
  await form.getByRole('textbox', { name: 'Asunto' }).fill(correo.asunto);
  await form.getByRole('textbox', { name: 'Cuerpo' }).fill(correo.cuerpo);
  await form.getByRole('button', { name: 'Guardar' }).click();
  await expect(form).toBeHidden();
  await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
}

test.describe('Notificaciones — Del centro', () => {
  test('El supervisor solo ve las notificaciones de su centro', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el administrador da de alta el correo de referencia con
    //         el motivo «Aviso Mislata» y otro del centro «CIPFP Batoi» con el
    //         motivo «Aviso Batoi», DNI «65399546N», nombre «Alumno1», apellidos
    //         «CIPFP Batoi» y «para» «alumno1@batoi.es», y cierra sesión.
    await login(page, 'admin', 'admin');
    await abrirMenu(page, 'Todas');
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
    await altaCorreo(page, CORREO_MISLATA);
    await altaCorreo(page, CORREO_BATOI);
    await logout(page);

    // Paso 2: Cuando «supervisor1@mislata.es» inicia sesión y abre
    //         «Notificaciones» → «Del centro».
    await login(page, 'supervisor1@mislata.es', 'demo1234');
    await abrirMenu(page, 'Del centro');

    // Resultado esperado: se ve la fila con motivo «Aviso Mislata», tipo
    // «Correo», destino «alumno1@mislata.es» y centro «CIPFP Mislata».
    const filaMislata = page.getByRole('row', { name: MOTIVO_MISLATA });
    await expect(filaMislata).toBeVisible();
    await expect(celda(filaMislata, COL.motivo)).toHaveText(MOTIVO_MISLATA);
    await expect(celda(filaMislata, COL.tipo)).toHaveText('Correo');
    await expect(celda(filaMislata, COL.destino)).toHaveText(CORREO_MISLATA.para);
    await expect(celda(filaMislata, COL.centro)).toHaveText(CORREO_MISLATA.centro);

    // Resultado esperado: no se ve ninguna fila con el motivo «Aviso Batoi»
    // (ni la de este run ni las de runs anteriores). La aserción va después de
    // ver la fila de Mislata, así el grid ya está cargado y no pasa en vacío.
    await expect(page.getByRole('row', { name: MOTIVO_BATOI })).toHaveCount(0);
    await expect(page.getByRole('row', { name: /Aviso Batoi/ })).toHaveCount(0);

    await logout(page);
  });
});
