import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../../../../_support/auth';

// T-015 — La vista de solo consulta de ENTRADA_DATOS muestra el periodo sin poder tocarlo
// origen: ESC —  |  TRAMITADOR | RECEPCION/ENTRADA_DATOS --(ningún evento)--> RECEPCION/ENTRADA_DATOS  |  tipo: solo-lectura
// fuente: .sdd/drafts/2026-09-22_16-01_justificacion-falta-profesorado-fechas/test-e2e-desc/t-015-la-vista-de-solo-consulta-de-entrada-datos-muestra-el-periodo-sin-poder-tocarlo.desc.md

const TRAMITE = 'Justificación de falta del profesorado';
const TIPO_TRAMITE = 'Trámites para el profesor';

const PROFESOR = { login: 'director@mislata.es', password: 'demo1234' };
const TRAMITADOR = { login: 'jefeestudios1@mislata.es', password: 'demo1234' };

// El footer del expediente (un botón por evento disponible) lo pinta la plantilla común
// del tramitador; es el sitio donde se dispara la transición y donde se comprueba qué
// eventos ofrece el estado para el perfil con el que se ha abierto el expediente.
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
 * con el que se pinta la vista, así que el `menuitem` no es intercambiable: «Expedientes
 * Esperando» pinta la vista del perfil TRAMITADOR y «Expedientes Pendientes» la del CREADOR.
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
  test('La vista de solo consulta de ENTRADA_DATOS muestra el periodo sin poder tocarlo', async ({ page }) => {
    let numero = '';
    try {
      // --- Tramo 1: CREADOR (director@mislata.es) — deja el expediente en ENTRADA_DATOS con datos guardados ---
      // Given: el profesor `director@mislata.es` (contraseña `demo1234`) ha iniciado sesión,
      // ha creado un expediente nuevo de «Justificación de falta del profesorado» …
      await ensureLoggedOut(page);
      await login(page, PROFESOR.login, PROFESOR.password);
      numero = await crearExpediente(page);
      await expect(page.getByLabel('Fase')).toHaveValue('Recepción');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Entrada de datos');

      // Given (cont.): … y, en RECEPCION / ENTRADA_DATOS, ha elegido el tipo de jornada faltada
      // «Unas horas de un único día» con «Fecha» 10/09/2026, «Hora de inicio» 09:00 y
      // «Hora de fin» 11:00, «Motivo falta» «Asistencia a pruebas selectivas y exámenes» y
      // `justificante.pdf` adjunto …
      const panelFalta = page.getByRole('region', { name: 'Datos de la falta' });
      await panelFalta.getByRole('radio', { name: 'Unas horas de un único día', exact: true }).click();

      // Elegir el tipo repinta el panel desde el servidor: «Fecha», «Hora de inicio» y
      // «Hora de fin» no existen hasta entonces. Hay que esperar a que estén, o el re-render
      // llega después del `fill` y se lleva por delante lo ya tecleado.
      await expect(panelFalta.getByRole('textbox', { name: 'Fecha', exact: true })).toBeVisible();
      await expect(panelFalta.getByRole('textbox', { name: 'Hora de inicio', exact: true })).toBeVisible();
      await expect(panelFalta.getByRole('textbox', { name: 'Hora de fin', exact: true })).toBeVisible();

      await panelFalta.getByRole('textbox', { name: 'Fecha', exact: true }).fill('10/09/2026');
      await panelFalta.getByRole('textbox', { name: 'Hora de inicio', exact: true }).fill('09:00');
      await panelFalta.getByRole('textbox', { name: 'Hora de fin', exact: true }).fill('11:00');
      await panelFalta.getByRole('radio', { name: 'Asistencia a pruebas selectivas y exámenes' }).click();

      // El widget `binary-link` esconde su `input[type=file]`; `setInputFiles` escribe en él
      // igualmente, y así el fichero va en memoria sin depender de ninguna ruta del disco.
      await page
        .getByTestId('field:justificante')
        .locator('input[type="file"]')
        .setInputFiles({ name: 'justificante.pdf', mimeType: 'application/pdf', buffer: JUSTIFICANTE_PDF });
      await expect(
        page.getByTestId('field:justificante').getByRole('button', { name: 'justificante.pdf' }),
      ).toBeVisible();

      // Given (cont.): … ha pulsado «Siguiente» (el expediente pasa a RECEPCION / PENDIENTE_PRESENTACION) …
      await page.getByTestId(FOOTER).getByRole('button', { name: 'Siguiente' }).click();
      await expect(page.getByLabel('Fase')).toHaveValue('Recepción');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Pendiente de presentación');

      // Given (cont.): … y después «Atrás», con lo que el expediente vuelve a RECEPCION /
      // ENTRADA_DATOS con esos datos ya guardados …
      await page.getByTestId(FOOTER).getByRole('button', { name: 'Atrás' }).click();
      await expect(page.getByLabel('Fase')).toHaveValue('Recepción');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Entrada de datos');

      // Given (cont.): … y cierra sesión.
      await logout(page);

      // --- Tramo 2 (final): TRAMITADOR (jefeestudios1@mislata.es) ---
      // Given (cont.): después `jefeestudios1@mislata.es` (contraseña `demo1234`) inicia sesión
      // y abre ese expediente entrando por la bandeja «Expedientes esperando a que otra persona
      // realice una tarea», que es la del perfil TRAMITADOR; como ENTRADA_DATOS no tiene
      // pantalla para ese perfil, el sistema abre la vista genérica de solo consulta.
      await login(page, TRAMITADOR.login, TRAMITADOR.password);
      await abrirDesdeBandeja(page, 'expedientes-expedientesEsperando-menuitem', numero);

      // When: mira la pantalla.
      const formulario = page.getByRole('tabpanel', { name: new RegExp(numero) });

      // Then: el expediente **sigue** en RECEPCION / ENTRADA_DATOS y la cabecera muestra
      // «Recepción» y «Entrada de datos».
      await expect(page.getByLabel('Fase')).toHaveValue('Recepción');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Entrada de datos');

      // And: el panel «Datos de la falta» muestra, en solo lectura, el tipo de jornada faltada
      // «Unas horas de un único día» …
      // En la vista de solo consulta el tipo de jornada ya no se pinta como grupo de radios
      // (como en la vista del CREADOR) sino como un `input` de selección en modo solo lectura.
      const panelConsulta = formulario.getByRole('region', { name: 'Datos de la falta' });
      const tipoJornada = panelConsulta.getByRole('combobox', { name: 'Tipo de jornada faltada' });
      await expect(tipoJornada).toHaveValue('Unas horas de un único día');
      await expect(tipoJornada).toHaveAttribute('readonly', '');

      // And (cont.): … «Fecha» a 10/09/2026, «Hora de inicio» a 09:00 y «Hora de fin» a 11:00 …
      // Los tres son campos que en la vista del perfil CREADOR SÍ son editables (el tramo 1 los
      // tecleó): que aquí estén deshabilitados es la mitad que da valor a la red de seguridad.
      const fecha = panelConsulta.getByRole('textbox', { name: 'Fecha', exact: true });
      const horaInicio = panelConsulta.getByRole('textbox', { name: 'Hora de inicio', exact: true });
      const horaFin = panelConsulta.getByRole('textbox', { name: 'Hora de fin', exact: true });
      await expect(fecha).toHaveValue('10/09/2026');
      await expect(fecha).toBeDisabled();
      await expect(horaInicio).toHaveValue('09:00');
      await expect(horaInicio).toBeDisabled();
      await expect(horaFin).toHaveValue('11:00');
      await expect(horaFin).toBeDisabled();

      // And (cont.): … y **no** muestra «Fecha de fin» …
      await expect(panelConsulta.getByRole('textbox', { name: 'Fecha de fin', exact: true })).toHaveCount(0);
      await expect(panelConsulta.getByText('Fecha de fin', { exact: true })).toHaveCount(0);

      // And (cont.): … no hay ningún campo editable …
      // No basta con los tres campos de arriba: se comprueba que en TODO el formulario del
      // expediente no queda ni un control de entrada que se pueda teclear o desplegar.
      await expect(
        formulario.locator(
          'input:not([disabled]):not([readonly]), textarea:not([disabled]):not([readonly]), select:not([disabled])',
        ),
      ).toHaveCount(0);

      // And (cont.): … y el único botón es «Salir».
      // El footer no ofrece ninguno de los eventos que ENTRADA_DATOS sí da al perfil CREADOR.
      const footer = formulario.getByTestId(FOOTER);
      await expect(footer.getByRole('button')).toHaveText(['Salir']);
      await expect(footer.getByRole('button', { name: 'Siguiente' })).toHaveCount(0);
      await expect(footer.getByRole('button', { name: 'Borrar el expediente' })).toHaveCount(0);
    } finally {
      // Teardown (§5.2): el expediente queda en ENTRADA_DATOS, que sí ofrece «Borrar el
      // expediente» (DELETE). Ese evento es del perfil CREADOR, no del TRAMITADOR con el que
      // termina el test, así que el borrado se hace reautenticándose como el profesor y
      // entrando por la bandeja «Expedientes Pendientes» (la del CREADOR). Va ANTES del
      // `logout` porque necesita SESIÓN ABIERTA. Si una aserción falló antes del «Atrás», el
      // expediente puede haber quedado en PENDIENTE_PRESENTACION, que no ofrece DELETE pero sí
      // «Atrás»: por eso se pulsa primero cuando está. DELETE recarga la aplicación entera
      // (refresh-app). El `.catch(() => {})` es intencional: el teardown no debe enmascarar el
      // fallo de una aserción.
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
