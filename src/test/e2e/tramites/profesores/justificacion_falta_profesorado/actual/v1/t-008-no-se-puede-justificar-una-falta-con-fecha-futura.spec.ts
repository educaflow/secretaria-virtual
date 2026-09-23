import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../../../../_support/auth';

// T-008 — No se puede justificar una falta con fecha futura
// origen: ESC-009  |  CREADOR | RECEPCION/ENTRADA_DATOS --GUARDAR_DATOS--> RECEPCION/ENTRADA_DATOS  |  tipo: error
// fuente: .sdd/drafts/2026-09-22_16-01_justificacion-falta-profesorado-fechas/test-e2e-desc/t-008-no-se-puede-justificar-una-falta-con-fecha-futura.desc.md

const TRAMITE = 'Justificación de falta del profesorado';
const TIPO_TRAMITE = 'Trámites para el profesor';

const PROFESOR = { login: 'director@mislata.es', password: 'demo1234' };

/**
 * Fecha futura: fija y lo bastante lejana como para seguir siendo futuro se ejecute el año
 * que se ejecute (una fecha relativa a `Date.now()` podría caer justo en el límite y volver
 * el test dependiente del reloj). Está dentro de los últimos 12 meses «por arriba», así que
 * la única regla que incumple es la de no ser posterior a hoy: el recuadro rojo trae un
 * único mensaje y el test puede afirmar cuál es.
 */
const FECHA_FUTURA = '01/01/2030';

// El footer del expediente (un botón por evento disponible) lo pinta la plantilla común
// del tramitador; es el sitio donde se dispara la transición y donde salen, en un recuadro
// rojo (`role="alert"`), los mensajes de validación del servidor.
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
  test('No se puede justificar una falta con fecha futura', async ({ page }) => {
    let numero = '';
    try {
      // --- Tramo único: CREADOR (director@mislata.es) ---
      // Given: el profesor `director@mislata.es` (contraseña `demo1234`) ha iniciado sesión,
      // ha creado un expediente nuevo de «Justificación de falta del profesorado» y lo tiene
      // abierto en RECEPCION / ENTRADA_DATOS.
      await ensureLoggedOut(page);
      await login(page, PROFESOR.login, PROFESOR.password);
      numero = await crearExpediente(page);
      await expect(page.getByLabel('Fase')).toHaveValue('Recepción');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Entrada de datos');

      const panelFalta = page.getByRole('region', { name: 'Datos de la falta' });

      // When: elige el tipo de jornada faltada «Un día completo», …
      await panelFalta.getByRole('radio', { name: 'Un día completo', exact: true }).click();

      // When (cont.): … rellena «Fecha» con 01/01/2030 — al elegir «Un día completo» el panel
      // pasa a mostrar un único campo de fecha titulado «Fecha» (el dominio lo llama
      // «Fecha de Inicio»), y se le da una fecha posterior a hoy.
      const fecha = panelFalta.getByRole('textbox', { name: 'Fecha', exact: true });
      await expect(fecha).toBeVisible();
      await fecha.fill(FECHA_FUTURA);
      await expect(fecha).toHaveValue(FECHA_FUTURA);

      // When (cont.): … rellena «Motivo falta» con «Enfermedad común», …
      await panelFalta.getByRole('radio', { name: 'Enfermedad común' }).click();

      // When (cont.): … adjunta `justificante.pdf` …
      // El widget `binary-link` esconde su `input[type=file]`; `setInputFiles` escribe en él
      // igualmente, y así el fichero va en memoria sin depender de ninguna ruta del disco.
      await page
        .getByTestId('field:justificante')
        .locator('input[type="file"]')
        .setInputFiles({ name: 'justificante.pdf', mimeType: 'application/pdf', buffer: JUSTIFICANTE_PDF });
      await expect(page.getByTestId('field:justificante').getByRole('button', { name: 'justificante.pdf' })).toBeVisible();

      // When (cont.): … y pulsa «Siguiente» (evento GUARDAR_DATOS, botón del footer).
      await page.getByTestId(FOOTER).getByRole('button', { name: 'Siguiente' }).click();

      // Then: el sistema muestra el error «La fecha no puede ser posterior a hoy» …
      // Los mensajes de validación salen en el recuadro rojo del footer (`role="alert"`),
      // como una lista con el título del campo en negrita delante del mensaje. El título que
      // pinta en negrita es el del campo del dominio («Fecha de Inicio»), no el que la vista
      // le da en pantalla para este tipo de jornada («Fecha»).
      const recuadroErrores = page.getByTestId(FOOTER).getByRole('alert');
      await expect(recuadroErrores).toBeVisible();
      await expect(
        recuadroErrores.getByRole('listitem').filter({ hasText: /La fecha no puede ser posterior a hoy/ }),
      ).toBeVisible();
      await expect(recuadroErrores).toContainText(/Fecha de Inicio/);

      // Then (cont.): … y el expediente **sigue** en RECEPCION / ENTRADA_DATOS.
      // CRITICAL: sin esta comprobación el test pasaría aunque la máquina de estados avanzase.
      await expect(page.getByLabel('Fase')).toHaveValue('Recepción');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Entrada de datos');
    } finally {
      // Teardown (§5.2): el expediente queda en ENTRADA_DATOS, que sí ofrece el evento DELETE
      // («Borrar el expediente»). El borrado exige SESIÓN ABIERTA y el perfil del estado final
      // (CREADOR, el del último tramo), así que va ANTES del `logout`. DELETE recarga la
      // aplicación entera (refresh-app), por eso se vuelve a entrar por la bandeja del CREADOR.
      // El `.catch(() => {})` es intencional: el teardown no debe enmascarar el fallo de una aserción.
      if (numero) {
        await (async () => {
          await abrirDesdeBandeja(page, 'expedientes-expedientesPendientes-menuitem', numero);
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
