import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../../../../_support/auth';

// T-001 — Justifica un día completo
// origen: ESC-001  |  CREADOR | [*] --GUARDAR_DATOS--> ENTRADA/PENDIENTE_PRESENTACION  |  tipo: happy
// fuente: .sdd/drafts/2026-09-22_16-01_justificacion-falta-profesorado-fechas/test-e2e-desc/t-001-justifica-un-dia-completo.desc.md

const TRAMITE = 'Justificación de falta del profesorado';
const TIPO_TRAMITE = 'Trámites para el profesor';

const PROFESOR = { login: 'director@mislata.es', password: 'demo1234' };
const TRAMITADOR = { login: 'jefeestudios1@mislata.es', password: 'demo1234' };

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

  // La aplicación abre el expediente en una pestaña titulada
  // «<número>/<año>-<códigoCentro>-<tipo de expediente>» (p. ej. «00136/2026-46019660-…»);
  // el número del expediente son los dos primeros trozos separados por «-».
  const pestana = page.getByRole('tab').last();
  await expect(pestana).toContainText(new RegExp(`\\d{4,}/\\d{4}-\\d+-${TRAMITE}`));
  const numero = (await pestana.textContent())!.split('-').slice(0, 2).join('-').trim();
  expect(numero).toMatch(/^\d{4,}\/\d{4}-\d+$/);
  return numero;
}


/**
 * Fechas de la falta, relativas a hoy. El trámite solo admite faltas de los últimos 7 días, así
 * que una fecha fija caduca y el test empieza a fallar solo por el calendario. La de inicio es
 * hace 3 días y las de fin hace 2 y hace 1: el mismo tramo de días (10, 11 y 12/09/2026) que
 * describe el `.desc.md`, pero desplazado a la fecha en que corre el test.
 */
function diasAntes(dias: number): string {
  const fecha = new Date();
  fecha.setDate(fecha.getDate() - dias);
  const dd = String(fecha.getDate()).padStart(2, '0');
  const mm = String(fecha.getMonth() + 1).padStart(2, '0');
  return `${dd}/${mm}/${fecha.getFullYear()}`;
}
const FECHA_INICIO = diasAntes(3); // «10/09/2026» en la descripción
const FECHA_FIN_1 = diasAntes(2); // «11/09/2026» en la descripción
const FECHA_FIN_2 = diasAntes(1); // «12/09/2026» en la descripción

test.describe('Justificación de falta del profesorado — ENTRADA', () => {
  test('Justifica un día completo', async ({ page }) => {
    let numero = '';
    try {
      // --- Tramo 1: CREADOR (director@mislata.es) ---
      // Given: el profesor `director@mislata.es` (contraseña `demo1234`) ha iniciado sesión,
      // abre la lista de trámites disponibles, elige «Justificación de falta del profesorado»
      // y crea un expediente nuevo;
      await ensureLoggedOut(page);
      await login(page, PROFESOR.login, PROFESOR.password);
      numero = await crearExpediente(page);

      // … el sistema lo abre en la fase ENTRADA, estado ENTRADA_DATOS,
      await expect(page.getByLabel('Fase')).toHaveValue('Entrada');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Entrada de datos');

      // … con el panel «Datos del profesor interesado» ya relleno con sus apellidos, su
      // nombre y su DNI,
      const panelProfesor = page.getByRole('region', { name: 'Datos del profesor interesado' });
      await expect(panelProfesor.getByLabel('Apellidos')).toHaveValue('CIPFP Mislata');
      await expect(panelProfesor.getByLabel('Nombre')).toHaveValue('Director');
      await expect(panelProfesor.getByLabel('DNI')).toHaveValue('98803877V');

      // … y con el panel «Datos de la falta» sin ningún dato del periodo precargado.
      const panelFalta = page.getByRole('region', { name: 'Datos de la falta' });
      await expect(panelFalta.getByRole('radio', { checked: true })).toHaveCount(0);
      await expect(panelFalta.getByRole('textbox', { name: 'Fecha de Inicio' })).toHaveValue('');

      // When: elige el tipo de jornada faltada «Un día completo», …
      await panelFalta.getByRole('radio', { name: 'Un día completo', exact: true }).click();

      // And: antes de pulsar «Siguiente», el panel «Datos de la falta» muestra el campo
      // titulado «Fecha» y **no** muestra «Fecha de fin», ni «Hora de inicio», ni «Hora de fin».
      await expect(panelFalta.getByRole('textbox', { name: 'Fecha', exact: true })).toBeVisible();
      await expect(panelFalta.getByRole('textbox', { name: 'Fecha de fin' })).toHaveCount(0);
      await expect(panelFalta.getByRole('textbox', { name: 'Hora de inicio' })).toHaveCount(0);
      await expect(panelFalta.getByRole('textbox', { name: 'Hora de fin' })).toHaveCount(0);

      // When (cont.): … rellena «Fecha» con 10/09/2026 y «Motivo falta» con «Traslado de
      // domicilio», adjunta `justificante.pdf` en «Foto o PDF del justificante» …
      await panelFalta.getByRole('textbox', { name: 'Fecha', exact: true }).fill(FECHA_INICIO);
      await panelFalta.getByRole('radio', { name: 'Traslado de domicilio' }).click();
      // El widget `binary-link` esconde su `input[type=file]`; `setInputFiles` escribe en él
      // igualmente, y así el fichero va en memoria sin depender de ninguna ruta del disco.
      await page
        .getByTestId('field:justificante')
        .locator('input[type="file"]')
        .setInputFiles({ name: 'justificante.pdf', mimeType: 'application/pdf', buffer: JUSTIFICANTE_PDF });
      await expect(page.getByTestId('field:justificante').getByRole('button', { name: 'justificante.pdf' })).toBeVisible();

      // When (cont.): … y pulsa «Siguiente» (evento GUARDAR_DATOS, botón del footer).
      await page.getByTestId(FOOTER).getByRole('button', { name: 'Siguiente' }).click();

      // Then: el expediente queda en la fase ENTRADA, estado PENDIENTE_PRESENTACION, y la
      // cabecera muestra «Entrada» y «Pendiente de presentación».
      await expect(page.getByLabel('Fase')).toHaveValue('Entrada');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Pendiente de presentación');

      // And: la nueva pantalla muestra la solicitud generada en PDF …
      const pdfSolicitud = page.getByRole('region', { name: 'Solicitud a firmar' }).locator('iframe');
      await expect(pdfSolicitud).toBeVisible();
      await expect(pdfSolicitud).toHaveAttribute('src', /MetaFile\/\d+\/content\/download/);
      // … y ofrece los botones «Atrás» y el de firmar y presentar.
      const footerCreador = page.getByTestId(FOOTER);
      await expect(footerCreador.getByRole('button', { name: 'Atrás' })).toBeVisible();
      await expect(footerCreador.getByRole('button', { name: /Firmar .*y Presentar la solicitud/ })).toBeVisible();

      // --- Tramo 2: TRAMITADOR (jefeestudios1@mislata.es) ---
      // And: tras cerrar sesión el profesor, `jefeestudios1@mislata.es` (contraseña
      // `demo1234`) inicia sesión y abre ese expediente desde «Tramitación» → «Jefatura de
      // estudios» → «Abiertos» (en PENDIENTE_PRESENTACION el expediente espera al profesor, así
      // que no está en «Pendientes de mí»); su perfil sobre él es TRAMITADOR;
      await logout(page);
      await login(page, TRAMITADOR.login, TRAMITADOR.password);
      await abrirDesdeBandeja(page, numero, 'tramitacion-jefaturaDeEstudios-abiertos-menuitem', 'tramitacion-menuitem', 'tramitacion-jefaturaDeEstudios-menuitem');

      // … como PENDIENTE_PRESENTACION no tiene pantalla para ese perfil, el sistema abre la
      // vista genérica de solo consulta, que muestra la solicitud en PDF …
      const pdfConsulta = page.getByRole('region', { name: 'Solicitud a firmar' }).locator('iframe');
      await expect(pdfConsulta).toBeVisible();
      await expect(pdfConsulta).toHaveAttribute('src', /MetaFile\/\d+\/content\/download/);
      // … y el aviso «La solicitud está pendiente de que el profesor la firme y la presente»;
      await expect(page.getByRole('status')).toContainText(
        /La solicitud está pendiente de que el profesor la firme y la presente/,
      );
      // … no hay ningún campo editable, …
      await expect(page.getByTestId('view:form').locator('input:not([disabled]):not([readonly])')).toHaveCount(0);
      // … el único botón es «Salir» …
      const footerConsulta = page.getByTestId(FOOTER);
      await expect(footerConsulta.getByRole('button')).toHaveCount(1);
      await expect(footerConsulta.getByRole('button', { name: 'Salir' })).toBeVisible();
      // … y el expediente **sigue** en ENTRADA / PENDIENTE_PRESENTACION.
      await expect(page.getByLabel('Fase')).toHaveValue('Entrada');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Pendiente de presentación');
    } finally {
      // Teardown (§5.2): PENDIENTE_PRESENTACION no ofrece DELETE, pero sí BACK, que devuelve
      // el expediente a ENTRADA_DATOS, donde «Borrar el expediente» sí está. El borrado exige
      // SESIÓN ABIERTA y el perfil del estado en el que queda el expediente (CREADOR), y el
      // último tramo lo hizo el TRAMITADOR: por eso aquí se reautentica como el profesor y el
      // `logout` va DESPUÉS. DELETE recarga la aplicación entera (refresh-app).
      // El `.catch(() => {})` es intencional: el teardown no debe enmascarar el fallo de una aserción.
      if (numero) {
        await (async () => {
          await ensureLoggedOut(page);
          await login(page, PROFESOR.login, PROFESOR.password);
          await abrirDesdeMisTramites(page, numero);
          await page.getByTestId(FOOTER).getByRole('button', { name: 'Atrás' }).click();
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
