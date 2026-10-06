import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../../../../_support/auth';

// T-015 — La vista de solo consulta de ENTRADA_DATOS muestra el periodo sin poder tocarlo
// origen: ESC —  |  TRAMITADOR y SECRETARIO | ENTRADA/ENTRADA_DATOS --(ningún evento)--> ENTRADA/ENTRADA_DATOS  |  tipo: solo-lectura
// fuente: .sdd/drafts/2026-09-22_16-01_justificacion-falta-profesorado-fechas/test-e2e-desc/t-015-la-vista-de-solo-consulta-de-entrada-datos-muestra-el-periodo-sin-poder-tocarlo.desc.md

const TRAMITE = 'Justificación de falta del profesorado';
const TIPO_TRAMITE = 'Trámites para el profesor';

const PROFESOR = { login: 'director@mislata.es', password: 'demo1234' };
const TRAMITADOR = { login: 'jefeestudios1@mislata.es', password: 'demo1234' };
// No es el CREADOR ni ostenta el perfil TRAMITADOR: su perfil sobre el expediente es SECRETARIO,
// para el que ENTRADA_DATOS no tiene pantalla, así que es quien ve la vista genérica.
const SECRETARIO = { login: 'secretario@mislata.es', password: 'demo1234' };

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

  // La aplicación abre el expediente en una pestaña titulada «<número>-<tipo de expediente>»,
  // con el número con el formato «00001/2026-46019660» (secuencial/año-código del centro).
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
  test('La vista de solo consulta de ENTRADA_DATOS muestra el periodo sin poder tocarlo', async ({ page }) => {
    let numero = '';
    try {
      // --- Tramo 1: CREADOR (director@mislata.es) — deja el expediente en ENTRADA_DATOS con datos guardados ---
      // Given: el profesor `director@mislata.es` (contraseña `demo1234`) ha iniciado sesión,
      // ha creado un expediente nuevo de «Justificación de falta del profesorado» …
      await ensureLoggedOut(page);
      await login(page, PROFESOR.login, PROFESOR.password);
      numero = await crearExpediente(page);
      await expect(page.getByLabel('Fase')).toHaveValue('Entrada');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Entrada de datos');

      // Given (cont.): … y, en ENTRADA / ENTRADA_DATOS, ha elegido el tipo de jornada faltada
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

      await panelFalta.getByRole('textbox', { name: 'Fecha', exact: true }).fill(FECHA_INICIO);
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

      // Given (cont.): … ha pulsado «Siguiente» (el expediente pasa a ENTRADA / PENDIENTE_PRESENTACION) …
      await page.getByTestId(FOOTER).getByRole('button', { name: 'Siguiente' }).click();
      await expect(page.getByLabel('Fase')).toHaveValue('Entrada');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Pendiente de presentación');

      // Given (cont.): … y después «Atrás», con lo que el expediente vuelve a ENTRADA /
      // ENTRADA_DATOS con esos datos ya guardados …
      await page.getByTestId(FOOTER).getByRole('button', { name: 'Atrás' }).click();
      await expect(page.getByLabel('Fase')).toHaveValue('Entrada');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Entrada de datos');

      // Given (cont.): … y cierra sesión.
      await logout(page);

      // --- Tramo 2: TRAMITADOR (jefeestudios1@mislata.es) ---
      // Given (cont.): después `jefeestudios1@mislata.es` (contraseña `demo1234`) inicia sesión
      // y abre ese expediente entrando por «Tramitación» → «Jefatura de estudios» → «Abiertos»
      // (en ENTRADA_DATOS el expediente espera al profesor, así que no está en «Pendientes de
      // mí»); su perfil sobre el expediente es TRAMITADOR y ENTRADA_DATOS tiene una pantalla
      // para ese perfil —la de registrar una solicitud entregada en papel—, así que el sistema
      // abre esa pantalla y no la vista genérica.
      await login(page, TRAMITADOR.login, TRAMITADOR.password);
      await abrirDesdeBandeja(page, numero, 'tramitacion-jefaturaDeEstudios-abiertos-menuitem', 'tramitacion-menuitem', 'tramitacion-jefaturaDeEstudios-menuitem');

      // When: mira la pantalla.
      const pantallaTramitador = page.getByRole('tabpanel', { name: new RegExp(numero) });

      // Then: el expediente **sigue** en ENTRADA / ENTRADA_DATOS y la cabecera muestra
      // «Entrada» y «Entrada de datos».
      await expect(page.getByLabel('Fase')).toHaveValue('Entrada');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Entrada de datos');

      // And: el panel «Datos de la falta» muestra el tipo de jornada faltada «Unas horas de un
      // único día», «Fecha» a 10/09/2026, «Hora de inicio» a 09:00 y «Hora de fin» a 11:00, y
      // **no** muestra «Fecha de fin» …
      // La pantalla del perfil TRAMITADOR es la del papel, que pinta el tipo de jornada como grupo
      // de radios (igual que la del CREADOR), no como el `input` de solo lectura de la genérica.
      const panelTramitador = pantallaTramitador.getByRole('region', { name: 'Datos de la falta' });
      await expect(
        panelTramitador.getByRole('radio', { name: 'Unas horas de un único día', exact: true }),
      ).toHaveAttribute('aria-checked', 'true');
      await expect(panelTramitador.getByRole('textbox', { name: 'Fecha', exact: true })).toHaveValue(FECHA_INICIO);
      await expect(panelTramitador.getByRole('textbox', { name: 'Hora de inicio', exact: true })).toHaveValue('09:00');
      await expect(panelTramitador.getByRole('textbox', { name: 'Hora de fin', exact: true })).toHaveValue('11:00');
      await expect(panelTramitador.getByRole('textbox', { name: 'Fecha de fin', exact: true })).toHaveCount(0);

      // And (cont.): … como la solicitud es telemática, la pantalla muestra el aviso «La solicitud
      // está pendiente de que el profesor complete o corrija los datos y la presente» …
      await expect(pantallaTramitador.getByTestId('panel:avisoEntradaDatosTramitador').getByRole('status')).toContainText(
        'La solicitud está pendiente de que el profesor complete o corrija los datos y la presente',
      );

      // And (cont.): … y **no ofrece ningún botón** (ni «Siguiente», ni «Presentar la solicitud»,
      // ni «Atrás», ni «Borrar el expediente»), así que no puede guardar ningún cambio.
      // Los botones de esta pantalla son solo para el papel (`showIf="presentadoEnPapel"`): en un
      // expediente telemático el footer se queda vacío, sin siquiera «Salir».
      await expect(pantallaTramitador.getByTestId(FOOTER).getByRole('button')).toHaveCount(0);

      await logout(page);

      // --- Tramo 3 (final): SECRETARIO (secretario@mislata.es), vista genérica de solo consulta ---
      // And: tras cerrar sesión la jefatura, `secretario@mislata.es` (contraseña `demo1234`) inicia
      // sesión y abre ese expediente entrando por la misma lista «Tramitación» → «Jefatura de
      // estudios» → «Abiertos»; como su perfil sobre el expediente es SECRETARIO y ENTRADA_DATOS
      // no tiene pantalla para ese perfil, el sistema abre la vista genérica de solo consulta …
      await login(page, SECRETARIO.login, SECRETARIO.password);
      await abrirDesdeBandeja(page, numero, 'tramitacion-jefaturaDeEstudios-abiertos-menuitem', 'tramitacion-menuitem', 'tramitacion-jefaturaDeEstudios-menuitem');

      const formulario = page.getByRole('tabpanel', { name: new RegExp(numero) });

      // And (cont.): … el panel «Datos de la falta» muestra, en solo lectura, el tipo de jornada
      // faltada «Unas horas de un único día» …
      // El tipo de jornada se pinta como grupo de radios (`RadioSelect`), como en la vista del
      // CREADOR, pero en modo solo lectura: la opción guardada marcada y ninguna se puede tocar.
      const panelConsulta = formulario.getByRole('region', { name: 'Datos de la falta' });
      const tipoJornada = panelConsulta.getByRole('radio', { name: 'Unas horas de un único día', exact: true });
      await expect(tipoJornada).toBeChecked();
      await expect(panelConsulta.getByRole('radio', { name: 'Un día completo', exact: true })).not.toBeChecked();
      // Un radio de solo lectura no lleva `disabled` (son `div role="radio"`): simplemente ignora
      // el clic. Se prueba así: pulsar otra opción no mueve la selección.
      await panelConsulta.getByRole('radio', { name: 'Un día completo', exact: true }).click();
      await expect(tipoJornada).toBeChecked();
      await expect(panelConsulta.getByRole('radio', { name: 'Un día completo', exact: true })).not.toBeChecked();

      // And (cont.): … «Fecha» a 10/09/2026, «Hora de inicio» a 09:00 y «Hora de fin» a 11:00 …
      // Los tres son campos que en la vista del perfil CREADOR SÍ son editables (el tramo 1 los
      // tecleó): que aquí estén deshabilitados es la mitad que da valor a la red de seguridad.
      const fecha = panelConsulta.getByRole('textbox', { name: 'Fecha', exact: true });
      const horaInicio = panelConsulta.getByRole('textbox', { name: 'Hora de inicio', exact: true });
      const horaFin = panelConsulta.getByRole('textbox', { name: 'Hora de fin', exact: true });
      await expect(fecha).toHaveValue(FECHA_INICIO);
      await expect(fecha).toBeDisabled();
      await expect(horaInicio).toHaveValue('09:00');
      await expect(horaInicio).toBeDisabled();
      await expect(horaFin).toHaveValue('11:00');
      await expect(horaFin).toBeDisabled();

      // And (cont.): … y **no** muestra «Fecha de fin» …
      await expect(panelConsulta.getByRole('textbox', { name: 'Fecha de fin', exact: true })).toHaveCount(0);
      await expect(panelConsulta.getByText('Fecha de fin', { exact: true })).toHaveCount(0);

      // And (cont.): … se ve el aviso «La solicitud está pendiente de que el profesor complete y
      // guarde los datos de la falta» …
      await expect(formulario.getByRole('status')).toContainText(
        'La solicitud está pendiente de que el profesor complete y guarde los datos de la falta',
      );

      // And (cont.): … no hay ningún campo editable …
      // No basta con los tres campos de arriba: se comprueba que en TODO el formulario del
      // expediente no queda ni un control de entrada que se pueda teclear o desplegar.
      // Salvo «Nueva nota»: el panel de notas internas va debajo de todas las pantallas de
      // estado y el personal del centro puede escribir en él; no es un dato del expediente.
      await expect(
        formulario.locator(
          'input:not([disabled]):not([readonly]), textarea:not([disabled]):not([readonly]), select:not([disabled])',
        ).and(formulario.locator(':not([data-testid="field:nuevaNota"] *)')),
      ).toHaveCount(0);

      // And (cont.): … el único botón es «Salir» …
      // El footer no ofrece ninguno de los eventos que ENTRADA_DATOS sí da al perfil CREADOR.
      const footer = formulario.getByTestId(FOOTER);
      await expect(footer.getByRole('button')).toHaveText(['Salir']);
      await expect(footer.getByRole('button', { name: 'Siguiente' })).toHaveCount(0);
      await expect(footer.getByRole('button', { name: 'Borrar el expediente' })).toHaveCount(0);

      // And (cont.): … y el expediente **sigue** en ENTRADA / ENTRADA_DATOS.
      await expect(page.getByLabel('Fase')).toHaveValue('Entrada');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Entrada de datos');
    } finally {
      // Teardown (§5.2): el expediente queda en ENTRADA_DATOS, que sí ofrece «Borrar el
      // expediente» (DELETE). Ese evento es del perfil CREADOR, no del SECRETARIO con el que
      // termina el test, así que el borrado se hace reautenticándose como el profesor y
      // entrando por «Mis trámites» (las listas del CREADOR). Va ANTES del
      // `logout` porque necesita SESIÓN ABIERTA. Si una aserción falló antes del «Atrás», el
      // expediente puede haber quedado en PENDIENTE_PRESENTACION, que no ofrece DELETE pero sí
      // «Atrás»: por eso se pulsa primero cuando está. DELETE recarga la aplicación entera
      // (refresh-app). El `.catch(() => {})` es intencional: el teardown no debe enmascarar el
      // fallo de una aserción.
      if (numero) {
        await (async () => {
          await ensureLoggedOut(page);
          await login(page, PROFESOR.login, PROFESOR.password);
          await abrirDesdeMisTramites(page, numero);
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
