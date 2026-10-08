import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-059 — El supervisor de dos centros ve las de ambos y filtra por centro
// origen: ESC-046  |  verifica: —
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-059-el-supervisor-de-dos-centros-ve-las-de-ambos-y-filtra-por-centro.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (lo
// verifican T-046 y T-049), así que este test NO tiene teardown — excepción
// documentada del §4.5 del contrato. Para no colisionar con las notificaciones
// que dejan los runs anteriores (la BD compartida no se resetea), los motivos
// «Aviso Mislata» y «Aviso Batoi» llevan un sufijo único por ejecución y las
// aserciones localizan cada fila por su motivo.
const RUN = Date.now();
const MOTIVO_MISLATA = `Aviso Mislata t059-${RUN}`;
const MOTIVO_BATOI = `Aviso Batoi t059-${RUN}`;

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
const COL = { centro: 0, motivo: 6 };
const celda = (fila: Locator, col: number): Locator => fila.getByRole('gridcell').nth(col);
const fila = (page: Page, motivo: string): Locator => page.getByRole('row', { name: motivo });

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
  test('El supervisor de dos centros ve las de ambos y filtra por centro', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el administrador da de alta el correo de referencia con
    //         el motivo «Aviso Mislata» y otro de «CIPFP Batoi» con el motivo
    //         «Aviso Batoi», DNI «65399546N», nombre «Alumno1», apellidos
    //         «CIPFP Batoi» y «para» «alumno1@batoi.es», y cierra sesión.
    await login(page, 'admin', 'admin');
    await abrirMenu(page, 'Todas');
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
    await altaCorreo(page, CORREO_MISLATA);
    await altaCorreo(page, CORREO_BATOI);
    await logout(page);

    // Paso 2: Cuando «supervisordoscentros@mislata.es» inicia sesión y abre
    //         «Notificaciones» → «Del centro».
    await login(page, 'supervisordoscentros@mislata.es', 'demo1234');
    await abrirMenu(page, 'Del centro');

    // Paso 3: Entonces se ven «Aviso Mislata» con centro «CIPFP Mislata» y
    //         «Aviso Batoi» con centro «CIPFP Batoi».
    const filaMislata = fila(page, MOTIVO_MISLATA);
    await expect(filaMislata).toBeVisible();
    await expect(celda(filaMislata, COL.motivo)).toHaveText(MOTIVO_MISLATA);
    await expect(celda(filaMislata, COL.centro)).toHaveText(CORREO_MISLATA.centro);
    const filaBatoi = fila(page, MOTIVO_BATOI);
    await expect(filaBatoi).toBeVisible();
    await expect(celda(filaBatoi, COL.motivo)).toHaveText(MOTIVO_BATOI);
    await expect(celda(filaBatoi, COL.centro)).toHaveText(CORREO_BATOI.centro);

    // Paso 4: Cuando filtra la columna centro por «CIPFP Batoi».
    // La fila de filtros del grid tiene, en la columna «Centro», un input de
    // texto «Buscar...» que aplica la búsqueda al pulsar Enter.
    const filtroCentro = page.getByTestId('column:centro').getByPlaceholder('Buscar...');
    await filtroCentro.fill(CORREO_BATOI.centro);
    await filtroCentro.press('Enter');

    // Resultado esperado: se ve «Aviso Batoi»...
    await expect(filaBatoi).toBeVisible();
    await expect(celda(filaBatoi, COL.centro)).toHaveText(CORREO_BATOI.centro);
    // ...y no «Aviso Mislata». Va después de ver la de Batoi, así el grid ya
    // se ha recargado con el filtro y la ausencia no pasa en vacío.
    await expect(filaMislata).toHaveCount(0);
    await expect(page.getByRole('row', { name: /Aviso Mislata/ })).toHaveCount(0);

    await logout(page);
  });
});
