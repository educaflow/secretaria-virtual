import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../../../../_support/auth';

// T-006 — No se puede continuar sin indicar la fecha
// origen: ESC-005  |  CREADOR | RECEPCION/ENTRADA_DATOS --GUARDAR_DATOS--> RECEPCION/ENTRADA_DATOS  |  tipo: error
// fuente: .sdd/drafts/2026-09-22_16-01_justificacion-falta-profesorado-fechas/test-e2e-desc/t-006-no-se-puede-continuar-sin-indicar-la-fecha.desc.md

const TRAMITE = 'Justificación de falta del profesorado';
const TIPO_TRAMITE = 'Trámites para el profesor';

const PROFESOR = { login: 'director@mislata.es', password: 'demo1234' };

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

/** Despliega los grupos del menú principal (de raíz a hoja) que haga falta y abre la entrada. */
async function abrirMenu(page: Page, entrada: string, ...grupos: string[]): Promise<void> {
  const item = page.getByTestId(`item:${entrada}`);
  // Cada grupo se pliega al pulsarlo: solo se despliega si la entrada no se ve ya.
  for (const grupo of grupos) {
    if (await item.isVisible()) {
      break;
    }
    await page.getByTestId(`item:${grupo}`).getByTestId('title').first().click();
  }
  await item.click();
}

/**
 * Abre la lista `entrada` del menú (desplegando sus `grupos`), la filtra por la columna
 * «Num. Exped.» y devuelve la fila del expediente cuyo número se pasa. Nunca «la primera
 * fila»: en una BD compartida que no se resetea, al segundo run hay varios expedientes del
 * mismo trámite y en el mismo estado.
 */
async function filtrarEnBandeja(page: Page, numero: string, entrada: string, ...grupos: string[]) {
  await abrirMenu(page, entrada, ...grupos);
  const filtro = page.getByTestId('column:numeroExpediente').getByPlaceholder('Buscar...');
  await filtro.fill(numero);
  await filtro.press('Enter');
  // El último rowgroup es el de los datos; el primero lleva la cabecera y la fila de filtros
  // (que también contiene el número tecleado y haría ambigua la búsqueda por rol).
  return page.getByRole('rowgroup').last().getByRole('row', { name: new RegExp(numero) });
}

/**
 * Abre desde una lista del menú el expediente cuyo número se pasa. Ninguna lista fija el
 * perfil: el servidor abre el expediente con el perfil real del usuario sobre él (el del
 * estado actual si lo ostenta; si no, el primero que tenga, con la vista genérica de solo
 * lectura). Lo que sí decide la lista es dónde está el expediente: «Mis trámites» para quien
 * lo registró (pendientes de mí / en tramitación / finalizados) y «Tramitación» para quien
 * lo tramita (pendientes de mí, o su unidad: abiertos / cerrados).
 */
async function abrirDesdeBandeja(page: Page, numero: string, entrada: string, ...grupos: string[]): Promise<void> {
  await (await filtrarEnBandeja(page, numero, entrada, ...grupos)).click();
  await expect(page.getByRole('tab', { name: new RegExp(numero) })).toBeVisible();
}

/**
 * Abre desde «Mis trámites» el expediente cuyo número se pasa, esté en la lista que esté:
 * lo usa el teardown, que no sabe en qué estado dejó el expediente el fallo de una aserción.
 */
async function abrirDesdeMisTramites(page: Page, numero: string): Promise<void> {
  const listas = ['misTramites-pendientesDeMi-menuitem', 'misTramites-enTramitacion-menuitem', 'misTramites-finalizados-menuitem'];
  for (const lista of listas) {
    const fila = await filtrarEnBandeja(page, numero, lista, 'misTramites-menuitem');
    const encontrado = await expect(fila).toHaveCount(1, { timeout: 5000 }).then(() => true, () => false);
    if (encontrado) {
      await fila.click();
      await expect(page.getByRole('tab', { name: new RegExp(numero) })).toBeVisible();
      return;
    }
  }
  throw new Error(`El expediente ${numero} no está en ninguna lista de «Mis trámites»`);
}

/**
 * Crea un expediente del trámite desde «Mis trámites» → «Nuevo trámite» (no hay botón
 * «Nuevo» de un grid: el alta la dispara el evento inicial del tipo de expediente) y
 * devuelve el NÚMERO que le ha asignado el servidor, que es lo único que identifica a lo
 * creado en una BD compartida que no se resetea.
 */
async function crearExpediente(page: Page): Promise<string> {
  await abrirMenu(page, 'misTramites-nuevoTramite-menuitem', 'misTramites-menuitem');

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


test.describe('Justificación de falta del profesorado — RECEPCION', () => {
  test('No se puede continuar sin indicar la fecha', async ({ page }) => {
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

      // When (cont.): … deja «Fecha» vacía — al elegir «Un día completo» el panel pasa a
      // mostrar un único campo de fecha titulado «Fecha», que nace vacío y así se deja.
      const fecha = panelFalta.getByRole('textbox', { name: 'Fecha', exact: true });
      await expect(fecha).toBeVisible();
      await expect(fecha).toHaveValue('');

      // When (cont.): … rellena «Motivo falta» con «Traslado de domicilio», …
      await panelFalta.getByRole('radio', { name: 'Traslado de domicilio' }).click();

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

      // Then: el sistema muestra el error «Es requerido» …
      // Los mensajes de validación salen en el recuadro rojo del footer (`role="alert"`),
      // como una lista con el título del campo en negrita delante del mensaje. El título que
      // pinta en negrita es el del campo del dominio («Fecha de Inicio»), no el que la vista
      // le da en pantalla para este tipo de jornada («Fecha»).
      const recuadroErrores = page.getByTestId(FOOTER).getByRole('alert');
      await expect(recuadroErrores).toBeVisible();
      await expect(
        recuadroErrores.getByRole('listitem').filter({ hasText: /Fecha de Inicio.*Es requerido/ }),
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
          await abrirDesdeBandeja(page, numero, 'misTramites-pendientesDeMi-menuitem', 'misTramites-menuitem');
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
