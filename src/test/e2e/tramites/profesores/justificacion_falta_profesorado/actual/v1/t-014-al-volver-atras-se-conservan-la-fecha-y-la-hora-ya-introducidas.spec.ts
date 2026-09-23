import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../../../../_support/auth';

// T-014 — Al volver atrás se conservan la fecha y la hora ya introducidas
// origen: ESC-014  |  CREADOR | RECEPCION/PENDIENTE_PRESENTACION --BACK--> RECEPCION/ENTRADA_DATOS  |  tipo: happy
// fuente: .sdd/drafts/2026-09-22_16-01_justificacion-falta-profesorado-fechas/test-e2e-desc/t-014-al-volver-atras-se-conservan-la-fecha-y-la-hora-ya-introducidas.desc.md

const TRAMITE = 'Justificación de falta del profesorado';
const TIPO_TRAMITE = 'Trámites para el profesor';

const PROFESOR = { login: 'director@mislata.es', password: 'demo1234' };

// El footer del expediente (un botón por evento disponible) lo pinta la plantilla común
// del tramitador; es el sitio donde se dispara la transición y donde se comprueba qué
// eventos ofrece el estado.
const FOOTER = 'panel:subsysExpedientes-template-footer-panel';

/**
 * Justificante mínimo válido: un PDF de ~390 bytes generado en memoria, para que el test
 * sea autocontenido y no dependa de ningún fichero del árbol (la descripción solo exige
 * «un PDF pequeño, de menos de 1 MB, llamado justificante.pdf»).
 */
const JUSTIFICANTE_PDF = Buffer.from(
  '%PDF-1.4\n' +
    '1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n' +
    '2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n' +
    '3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 200 200]/Contents 4 0 R/Resources<</Font<</F1 5 0 R>>>>>>endobj\n' +
    '4 0 obj<</Length 44>>stream\nBT /F1 12 Tf 20 100 Td (Justificante) Tj ET\nendstream endobj\n' +
    '5 0 obj<</Type/Font/Subtype/Type1/BaseFont/Helvetica>>endobj\n' +
    'trailer<</Root 1 0 R>>\n%%EOF\n',
  'latin1',
);

/** Despliega un grupo del menú principal y abre una de sus entradas. */
async function abrirMenu(page: Page, grupo: string, entrada: string): Promise<void> {
  // El grupo se pliega al pulsarlo: solo se despliega si la entrada no se ve ya.
  const item = page.getByTestId(`item:${entrada}`);
  if (!(await item.isVisible())) {
    await page.getByTestId(`item:${grupo}`).getByTestId('title').first().click();
  }
  await item.click();
}

/**
 * Crea un expediente del trámite desde «Ventanilla» → «Nuevo expediente» (no hay botón
 * «Nuevo» de un grid: el alta la dispara el evento inicial del tipo de expediente) y
 * devuelve el NÚMERO que le ha asignado el servidor, que es lo único que identifica a lo
 * creado en una BD compartida que no se resetea.
 */
async function crearExpediente(page: Page): Promise<string> {
  await abrirMenu(page, 'ventanilla-menuitem', 'ventanilla-nuevoExpediente-menuitem');

  // Árbol de trámites: los tipos de trámite (nivel 1) nacen plegados y sus trámites
  // (nivel 2) no existen en el DOM hasta desplegarlos.
  const arbol = page.getByTestId('panel:tramitesPanel');
  await arbol.locator('[role="row"][aria-level="1"]').filter({ hasText: TIPO_TRAMITE }).click();
  await arbol.locator('[role="row"][aria-level="2"]').filter({ hasText: TRAMITE }).click();

  await page.getByRole('button', { name: 'Crear expediente' }).click();

  // La aplicación abre el expediente en una pestaña titulada «<número>-<tipo de expediente>».
  const pestana = page.getByRole('tab').last();
  await expect(pestana).toContainText(new RegExp(`\\d{4,}/\\d{4}-${TRAMITE}`));
  const numero = (await pestana.textContent())!.split('-')[0].trim();
  expect(numero).toMatch(/^\d{4,}\/\d{4}$/);
  return numero;
}

/**
 * Abre desde una bandeja el expediente cuyo número se pasa. **Cada bandeja fija el perfil**
 * con el que se pinta la vista, así que el `menuitem` no es intercambiable.
 * El expediente se localiza filtrando la columna «Num. Exped.» por su número: nunca «el
 * primero de la bandeja», que al segundo run sería otro (y que además podría caer fuera de
 * la primera página según crece la BD).
 */
async function abrirDesdeBandeja(page: Page, menuitem: string, numero: string): Promise<void> {
  await abrirMenu(page, 'expedientes-menuitem', menuitem);
  const filtro = page.getByTestId('column:numeroExpediente').getByPlaceholder('Buscar...');
  await filtro.fill(numero);
  await filtro.press('Enter');
  // El último rowgroup es el de los datos; el primero lleva la cabecera y la fila de filtros
  // (que también contiene el número tecleado y haría ambigua la búsqueda por rol).
  await page.getByRole('rowgroup').last().getByRole('row', { name: new RegExp(numero) }).click();
  await expect(page.getByRole('tab', { name: new RegExp(numero) })).toBeVisible();
}

test.describe('Justificación de falta del profesorado — RECEPCION', () => {
  test('Al volver atrás se conservan la fecha y la hora ya introducidas', async ({ page }) => {
    let numero = '';
    try {
      // --- Tramo único: CREADOR (director@mislata.es) ---
      // Given: el profesor `director@mislata.es` (contraseña `demo1234`) ha iniciado sesión,
      // ha creado un expediente nuevo de «Justificación de falta del profesorado», …
      await ensureLoggedOut(page);
      await login(page, PROFESOR.login, PROFESOR.password);
      numero = await crearExpediente(page);
      await expect(page.getByLabel('Fase')).toHaveValue('Recepción');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Entrada de datos');

      // Given (cont.): … en RECEPCION / ENTRADA_DATOS ha elegido el tipo de jornada faltada
      // «Varios días pero del primer día solo faltó unas horas» con «Fecha de Inicio»
      // 10/09/2026, «Hora de inicio» 12:00 y «Fecha de fin» 11/09/2026, «Motivo falta»
      // «Deber inexcusable» y `justificante.pdf` adjunto, …
      const panelFalta = page.getByRole('region', { name: 'Datos de la falta' });
      await panelFalta
        .getByRole('radio', { name: 'Varios días pero del primer día solo faltó unas horas', exact: true })
        .click();

      // Elegir el tipo repinta el panel desde el servidor: «Hora de inicio» y «Fecha de fin»
      // no existen hasta entonces. Hay que esperar a que estén, o el re-render llega después
      // del `fill` y se lleva por delante lo ya tecleado.
      await expect(panelFalta.getByRole('textbox', { name: 'Fecha de Inicio', exact: true })).toBeVisible();
      await expect(panelFalta.getByRole('textbox', { name: 'Hora de inicio', exact: true })).toBeVisible();
      await expect(panelFalta.getByRole('textbox', { name: 'Fecha de fin', exact: true })).toBeVisible();

      await panelFalta.getByRole('textbox', { name: 'Fecha de Inicio', exact: true }).fill('10/09/2026');
      await panelFalta.getByRole('textbox', { name: 'Hora de inicio', exact: true }).fill('12:00');
      await panelFalta.getByRole('textbox', { name: 'Fecha de fin', exact: true }).fill('11/09/2026');
      await panelFalta.getByRole('radio', { name: 'Deber inexcusable' }).click();

      // El widget `binary-link` esconde su `input[type=file]`; `setInputFiles` escribe en él
      // igualmente, y así el fichero va en memoria sin depender de ninguna ruta del disco.
      await page
        .getByTestId('field:justificante')
        .locator('input[type="file"]')
        .setInputFiles({ name: 'justificante.pdf', mimeType: 'application/pdf', buffer: JUSTIFICANTE_PDF });
      await expect(
        page.getByTestId('field:justificante').getByRole('button', { name: 'justificante.pdf' }),
      ).toBeVisible();

      // Given (cont.): … y ha pulsado «Siguiente», de modo que el expediente está en
      // RECEPCION / PENDIENTE_PRESENTACION.
      await page.getByTestId(FOOTER).getByRole('button', { name: 'Siguiente' }).click();
      await expect(page.getByLabel('Fase')).toHaveValue('Recepción');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Pendiente de presentación');

      // When: pulsa «Atrás» (evento BACK, botón del footer).
      await page.getByTestId(FOOTER).getByRole('button', { name: 'Atrás' }).click();

      // Then: el expediente vuelve a RECEPCION / ENTRADA_DATOS (la cabecera muestra
      // «Recepción» y «Entrada de datos»).
      await expect(page.getByLabel('Fase')).toHaveValue('Recepción');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Entrada de datos');

      // And: el panel «Datos de la falta» muestra ya elegido el tipo de jornada faltada
      // «Varios días pero del primer día solo faltó unas horas», con «Fecha de Inicio» a
      // 10/09/2026, «Hora de inicio» a 12:00 y «Fecha de fin» a 11/09/2026.
      // Tras la transición la vista se repinta entera, así que el panel se vuelve a localizar.
      const panelFaltaVuelta = page.getByRole('region', { name: 'Datos de la falta' });
      await expect(
        panelFaltaVuelta.getByRole('radio', {
          name: 'Varios días pero del primer día solo faltó unas horas',
          exact: true,
        }),
      ).toBeChecked();
      await expect(panelFaltaVuelta.getByRole('textbox', { name: 'Fecha de Inicio', exact: true })).toHaveValue(
        '10/09/2026',
      );
      await expect(panelFaltaVuelta.getByRole('textbox', { name: 'Hora de inicio', exact: true })).toHaveValue('12:00');
      await expect(panelFaltaVuelta.getByRole('textbox', { name: 'Fecha de fin', exact: true })).toHaveValue(
        '11/09/2026',
      );
    } finally {
      // Teardown (§5.2): el camino feliz deja el expediente en ENTRADA_DATOS, que sí ofrece
      // «Borrar el expediente» (DELETE). Si una aserción falló antes del «Atrás», el
      // expediente puede haber quedado en PENDIENTE_PRESENTACION, que no ofrece DELETE pero
      // sí «Atrás»: por eso se pulsa primero cuando está. El borrado exige SESIÓN ABIERTA y
      // el perfil del estado en el que queda el expediente (CREADOR), así que va ANTES del
      // `logout`, reautenticándose para que el teardown funcione sea cual sea el punto en el
      // que quedó la sesión. DELETE recarga la aplicación entera (refresh-app).
      // El `.catch(() => {})` es intencional: el teardown no debe enmascarar el fallo de una aserción.
      if (numero) {
        await (async () => {
          await ensureLoggedOut(page);
          await login(page, PROFESOR.login, PROFESOR.password);
          await abrirDesdeBandeja(page, 'expedientes-expedientesPendientes-menuitem', numero);
          const botonAtras = page.getByTestId(FOOTER).getByRole('button', { name: 'Atrás' });
          if (await botonAtras.isVisible()) {
            await botonAtras.click();
          }
          await page.getByTestId(FOOTER).getByRole('button', { name: 'Borrar el expediente' }).click();
          await page.getByRole('dialog').getByRole('button', { name: 'Aceptar' }).click();
          // Se comprueba por el NÚMERO del expediente que este test creó, nunca por «el primero».
          await expect(page.getByRole('tab', { name: new RegExp(numero) })).toHaveCount(0);
        })().catch(() => {});
      }
      await logout(page).catch(() => {});
    }
  });
});
