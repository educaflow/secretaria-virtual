import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-027 — Alta de un correo con varias direcciones en copia y en copia oculta
// origen: ESC-063  |  verifica: V-Correo-004, V-Correo-005, V-Correo-010
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-027-alta-de-un-correo-con-varias-direcciones-en-copia-y-en-copia-oculta.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (lo
// verifica T-046), así que este test NO tiene teardown — excepción documentada
// del §4.5 del contrato. Para no colisionar con las notificaciones que dejan los
// runs anteriores (la BD compartida no se resetea), el motivo «Correo con copias»
// lleva un sufijo único por ejecución y todas las aserciones localizan la fila por él.
const MOTIVO = `Correo con copias t027-${Date.now()}`;
const CORREO = {
  centro: 'CIPFP Mislata',
  dni: '86862719E',
  nombre: 'Alumno1',
  apellidos: 'CIPFP Mislata',
  para: 'alumno1@mislata.es',
  enCopia: 'familiar1@mislata.es, familiar2@mislata.es',
  enCopiaOculta: 'supervisor1@mislata.es',
  asunto: 'Aviso con copias',
  cuerpo: 'Texto',
};

// El listado se ordena por «Fecha de creación» descendente, así que la
// notificación recién creada está en la primera página.
const filaDelCorreo = (page: Page): Locator => page.getByRole('row', { name: MOTIVO });

// Columnas del grid «Todas las notificaciones», en orden: Estado, Tipo, Motivo,
// DNI, Nombre, Apellidos, Destino, Centro, Expediente, Fecha de creación,
// Fecha de envío.
const COL = { motivo: 2, destino: 6 };
const celda = (fila: Locator, col: number): Locator => fila.getByRole('gridcell').nth(col);

const EN_COPIA = /^En copia(?! oculta)/;
const EN_COPIA_OCULTA = /^En copia oculta/;

test.describe('Notificaciones — Todas', () => {
  test('Alta de un correo con varias direcciones en copia y en copia oculta', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    // Paso 1: Dado que el administrador ha iniciado sesión y abre el alta de un correo
    //         («Notificaciones» → «Todas» → «Nueva notificación» → «Correo» → «Continuar»).
    await page.getByText('Notificaciones', { exact: true }).click();
    await page.getByText('Todas', { exact: true }).click();
    await page.getByRole('button', { name: 'Nueva notificación' }).click();
    const eleccion = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Elija el canal de la notificación' }) });
    await eleccion.getByRole('radio', { name: 'Correo' }).click();
    await eleccion.getByRole('button', { name: 'Continuar' }).click();

    // Paso 2: Cuando rellena el correo de referencia con el motivo «Correo con copias»,
    //         el «en copia» «familiar1@mislata.es, familiar2@mislata.es», el «en copia
    //         oculta» «supervisor1@mislata.es», el asunto «Aviso con copias» y el cuerpo
    //         «Texto», y pulsa «Guardar».
    const form = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Correo', exact: true }) });
    await expect(form.getByRole('region', { name: 'Datos del correo' })).toBeVisible();
    // El centro primero: es un many-to-one con autocompletado y su selección
    // re-renderiza el formulario.
    await form.getByRole('combobox', { name: 'Centro' }).fill(CORREO.centro);
    await page.getByRole('option', { name: CORREO.centro, exact: true }).click();
    await expect(form.getByRole('combobox', { name: 'Centro' })).toHaveValue(CORREO.centro);
    await form.getByRole('textbox', { name: /^Motivo/ }).fill(MOTIVO);
    await form.getByRole('textbox', { name: 'DNI del destinatario' }).fill(CORREO.dni);
    await form.getByRole('textbox', { name: 'Nombre', exact: true }).fill(CORREO.nombre);
    await form.getByRole('textbox', { name: 'Apellidos' }).fill(CORREO.apellidos);
    await form.getByRole('textbox', { name: /^Para/ }).fill(CORREO.para);
    await form.getByRole('textbox', { name: EN_COPIA }).fill(CORREO.enCopia);
    await form.getByRole('textbox', { name: EN_COPIA_OCULTA }).fill(CORREO.enCopiaOculta);
    await form.getByRole('textbox', { name: 'Asunto' }).fill(CORREO.asunto);
    await form.getByRole('textbox', { name: 'Cuerpo' }).fill(CORREO.cuerpo);
    await form.getByRole('button', { name: 'Guardar' }).click();

    // Resultado esperado: el listado muestra «Correo con copias» con el destino
    // «alumno1@mislata.es».
    await expect(form).toBeHidden();
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
    const fila = filaDelCorreo(page);
    await expect(fila).toBeVisible();
    await expect(celda(fila, COL.motivo)).toHaveText(MOTIVO);
    await expect(celda(fila, COL.destino)).toHaveText(CORREO.para);

    // Paso 3: Y pulsa la fila «Correo con copias».
    await fila.click();

    // Resultado esperado: el formulario muestra el «en copia»
    // «familiar1@mislata.es, familiar2@mislata.es» y el «en copia oculta»
    // «supervisor1@mislata.es».
    // La notificación guardada se abre en un diálogo con su formulario de canal.
    const datos = page.getByRole('dialog').getByRole('region', { name: 'Datos del correo' });
    await expect(datos).toBeVisible();
    await expect(datos.getByRole('textbox', { name: /^Motivo/ })).toHaveValue(MOTIVO);
    await expect(datos.getByRole('textbox', { name: EN_COPIA })).toHaveValue(CORREO.enCopia);
    await expect(datos.getByRole('textbox', { name: EN_COPIA_OCULTA })).toHaveValue(CORREO.enCopiaOculta);

    await page.getByRole('button', { name: 'Salir' }).click();
    await logout(page);
  });
});
