import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../../../../_support/auth';

// T-017 — La dirección acepta la justificación y el estado cerrado sigue mostrando el periodo
// origen: ESC —  |  DIRECTOR | RESOLUCION/PENDIENTE_RESOLUCION --RESOLVER--> RESOLUCION/ACEPTADO  |  tipo: happy
// MANUAL: llegar a PENDIENTE_RESOLUCION exige presentar con AutoFirma (T-016) —y que la jefatura de
//         estudios verifique la solicitud— y resolver exige el certificado digital del director del
//         centro instalado en el servidor; la carga de demo no trae ninguno de los dos.
// Ejecutar con:  E2E_MANUAL=1 npx playwright test --grep @manual --headed
// fuente: .sdd/drafts/2026-09-22_16-01_justificacion-falta-profesorado-fechas/test-e2e-desc/t-017-el-tramitador-acepta-la-justificacion-y-el-estado-cerrado-sigue-mostrando-el-periodo.desc.md

const TRAMITE = 'Justificación de falta del profesorado';
const TIPO_TRAMITE = 'Trámites para el profesor';

const PROFESOR = { login: 'director@mislata.es', password: 'demo1234' };
const TRAMITADOR = { login: 'jefeestudios1@mislata.es', password: 'demo1234' };
// Quien resuelve es el director del centro (perfil DIRECTOR, que le da su cargo). En la carga de
// demo es la misma persona que presenta la solicitud: sobre SU expediente es además el CREADOR.
const DIRECTOR = PROFESOR;

// El footer del expediente (un botón por evento disponible) lo pinta la plantilla común
// del tramitador; es el sitio donde se dispara la transición y donde se comprueba qué
// eventos ofrece el estado para el perfil con el que se ha abierto el expediente.
const FOOTER = 'panel:subsysExpedientes-template-footer-panel';

// Panel que enseña el PDF de la resolución (un `iframe` contra el MetaFile que el evento
// RESOLVER genera, firma con el certificado del director y registra de salida).
const PANEL_PDF_RESOLUCION = 'panel:pdfResolucion';
// Panel de solo lectura con el sentido de la resolución (y el motivo, si es un rechazo).
const PANEL_RESOLUCION_VIEW = 'panel:resolucion-view';


const BOTON_PRESENTAR = 'Firmar con AutoFirma y Presentar la solicitud';
const BOTON_RESOLVER = 'Resolver el expediente';

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
 * Recarga la aplicación (la sesión se conserva) y espera a que el menú principal esté pintado:
 * deja el menú plegado y sin pestañas abiertas, que es el punto de partida que `abrirMenu` espera.
 */
async function recargarAplicacion(page: Page): Promise<void> {
  await page.goto('/');
  await expect(page.getByTestId('item:tramitacion-menuitem')).toBeVisible();
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

test.describe('Justificación de falta del profesorado — RESOLUCION', () => {
  test(
    'La dirección acepta la justificación y el estado cerrado sigue mostrando el periodo',
    { tag: '@manual' },
    async ({ page }) => {
      // El paso manual (abrir AutoFirma, elegir el certificado y firmar) lo hace una persona:
      // no cabe en el `timeout` global de `playwright.config.ts` (90 s). La espera de ese paso
      // tiene su propio timeout de 600 s, y el del test es mayor para que, si la persona no
      // llega a tiempo, falle la aserción de la puerta manual (con su mensaje) y no el test entero.
      test.setTimeout(900_000);

      let numero = '';
      try {
        // --- Tramo 1: CREADOR (director@mislata.es) — junto con el tramo 2, solo para DEJAR el
        //     expediente en el estado de partida `RESOLUCION` / `PENDIENTE_RESOLUCION` que exige el
        //     `Given`. El tramo del evento RESOLVER (el del campo `Perfil`) es el 3, el del DIRECTOR ---
        // Given: existe un expediente de «Justificación de falta del profesorado» del centro CIPFP
        // Mislata en RESOLUCION / PENDIENTE_RESOLUCION, presentado con el tipo de jornada faltada
        // «Unas horas de un único día», «Fecha» 10/09/2026, «Hora de inicio» 09:00 y «Hora de fin»
        // 11:00 …
        // El estado de partida se alcanza recorriendo la máquina de estados POR LA UI (nunca por
        // REST ni por SQL): el CREADOR teclea los datos, pulsa «Siguiente» (GUARDAR_DATOS) y
        // presenta la solicitud (PRESENTAR), que es el paso que exige AutoFirma; después la jefatura
        // de estudios la da por correcta (VERIFICAR), que es lo que la pasa a la dirección.
        await ensureLoggedOut(page);
        await login(page, PROFESOR.login, PROFESOR.password);
        numero = await crearExpediente(page);
        await expect(page.getByLabel('Fase')).toHaveValue('Entrada');
        await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Entrada de datos');

        const panelFalta = page.getByRole('region', { name: 'Datos de la falta' });
        await panelFalta.getByRole('radio', { name: 'Unas horas de un único día', exact: true }).click();

        // Elegir el tipo repinta el panel desde el servidor: hay que esperar a que los campos del
        // nuevo tipo estén, o el re-render llega después del `fill` y se lleva lo ya tecleado.
        // Con «Unas horas de un único día» la fecha se titula «Fecha» (no «Fecha de Inicio»).
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

        await page.getByTestId(FOOTER).getByRole('button', { name: 'Siguiente' }).click();
        await expect(page.getByLabel('Fase')).toHaveValue('Entrada');
        await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Pendiente de presentación');

        // El servidor no tiene certificado de este profesor (`situacionFirma == 'SIN_CERTIFICADO'`),
        // que es justo el caso en el que la firma la hace AutoFirma en el equipo del profesor: es
        // lo que enseña el panel «Firma de la solicitud» y lo que hace que salga ESTE botón.
        await expect(page.getByRole('region', { name: 'Firma de la solicitud' })).toBeVisible();
        await expect(page.getByTestId(FOOTER).getByRole('button', { name: BOTON_PRESENTAR })).toBeVisible();

        await page.getByTestId(FOOTER).getByRole('button', { name: BOTON_PRESENTAR }).click();
        const avisoPresentar = page.getByRole('dialog');
        await expect(avisoPresentar).toContainText('¿Esta seguro que desea presentar la documentación?');
        await expect(avisoPresentar).toContainText('No podrá deshacer esta acción');
        await avisoPresentar.getByRole('button', { name: 'Aceptar' }).click();

        // === PASO MANUAL 1 de 2: firmar la solicitud con AutoFirma ===
        // Al aceptar, la aplicación lanza AutoFirma en el equipo de quien ejecuta el test.
        // LA PERSONA DEBE: dejar que se abra AutoFirma, elegir en el diálogo el certificado digital
        // del profesor `director@mislata.es` (su DNI debe coincidir con el del usuario), introducir
        // el PIN o la contraseña del certificado si se la pide y confirmar la firma.
        // Ninguna automatización puede hacerlo: AutoFirma es una aplicación de escritorio y el
        // certificado vive en la máquina del firmante.
        // El test continúa solo cuando el efecto de la firma es visible en la UI: el expediente ha
        // transicionado. No se espera un tiempo fijo ni se hace `page.pause()`, para que el test
        // siga fallando si la persona cancela AutoFirma o firma con el certificado equivocado.
        // Presentar ya no lleva a la resolución: la solicitud pasa antes por la verificación de la
        // jefatura de estudios.
        await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Pendiente de verificación', {
          timeout: 600_000,
        });
        await expect(page.getByLabel('Fase')).toHaveValue('Verificación');

        // --- Tramo 2: TRAMITADOR (jefeestudios1@mislata.es) — sigue siendo preparación del
        //     `Given`: la jefatura de estudios verifica la solicitud y la da por correcta ---
        // Given (cont.): … y verificado como correcto por la jefatura de estudios …
        // En PENDIENTE_VERIFICACION el expediente espera al perfil TRAMITADOR, así que está en su
        // lista «Tramitación» → «Pendientes de mí».
        await logout(page);
        await login(page, TRAMITADOR.login, TRAMITADOR.password);
        await abrirDesdeBandeja(page, numero, 'tramitacion-pendientesDeMi-menuitem', 'tramitacion-menuitem');
        await expect(page.getByLabel('Fase')).toHaveValue('Verificación');
        await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Pendiente de verificación');

        const solicitudCorrecta = page
          .getByRole('region', { name: 'Verificación de la solicitud' })
          .getByRole('radio', { name: 'La solicitud es correcta', exact: true });
        await solicitudCorrecta.click();
        // El botón solo se pulsa cuando la elección ha cuajado en el formulario: si se pulsara
        // antes, el evento viajaría sin `resultadoVerificacion` y el expediente no avanzaría.
        await expect(solicitudCorrecta).toHaveAttribute('aria-checked', 'true');
        // VERIFICAR es el botón «Siguiente» del footer, y no pide confirmación.
        await page.getByTestId(FOOTER).getByRole('button', { name: 'Siguiente' }).click();
        await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Pendiente de resolución');
        await expect(page.getByLabel('Fase')).toHaveValue('Resolución de la dirección');

        // --- Tramo 3: DIRECTOR (director@mislata.es) — el del campo `Perfil`, el que dispara el
        //     evento RESOLVER ---
        // Given (cont.): … `director@mislata.es` (contraseña `demo1234`) ha iniciado sesión, lo abre
        // por la lista «Tramitación» → «Pendientes de mí» …
        // En PENDIENTE_RESOLUCION el expediente espera al perfil DIRECTOR. El servidor abre el
        // expediente con el perfil del estado si el usuario lo ostenta, así que al director le pinta
        // la pantalla de resolver aunque sobre este expediente sea además el CREADOR.
        await logout(page);
        await login(page, DIRECTOR.login, DIRECTOR.password);
        await abrirDesdeBandeja(page, numero, 'tramitacion-pendientesDeMi-menuitem', 'tramitacion-menuitem');
        await expect(page.getByLabel('Fase')).toHaveValue('Resolución de la dirección');
        await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Pendiente de resolución');

        // When: elige «Tipo resolución» «Resolver positivamente», …
        const panelResolver = page.getByRole('region', { name: 'Resolver expediente' });
        const resolverPositivamente = panelResolver.getByRole('radio', {
          name: 'Resolver positivamente',
          exact: true,
        });
        await resolverPositivamente.click();
        // El botón solo se pulsa cuando la elección ha cuajado en el formulario: si se pulsara
        // antes, el evento viajaría sin `tipoResolucion` y el expediente no iría a ACEPTADO.
        await expect(resolverPositivamente).toHaveAttribute('aria-checked', 'true');

        // When (cont.): … pulsa «Resolver el expediente» y confirma el aviso de que no podrá
        // deshacer la acción.
        await page.getByTestId(FOOTER).getByRole('button', { name: BOTON_RESOLVER }).click();
        const avisoResolver = page.getByRole('dialog');
        await expect(avisoResolver).toContainText('¿Esta seguro que desea resolver el expediente?');
        await expect(avisoResolver).toContainText('No podrá deshacer esta acción');
        await avisoResolver.getByRole('button', { name: 'Aceptar' }).click();

        // === PASO MANUAL 2 de 2: el certificado del director, en el SERVIDOR ===
        // Este paso no lo ejecuta la persona en el navegador: la firma de la resolución la hace el
        // servidor (`resolucion/PhaseEventManagerImpl.triggerResolver` → `almacenClaveResolver.getDirector(centro)`).
        // Es, por tanto, una PRECONDICIÓN DE ENTORNO que la persona debe haber dejado lista ANTES de
        // lanzar el test: el certificado digital del director del centro CIPFP Mislata instalado en
        // el servidor. La carga de demo no lo trae, así que sin él el evento falla y el expediente
        // se queda en PENDIENTE_RESOLUCION.

        // Then: el expediente pasa a la fase RESOLUCION, estado ACEPTADO, que es un estado cerrado.
        // El título visible del estado es «Aceptado»: `ACEPTADO` no declara `title` en el
        // `TipoExpedienteInstance.xml`, así que el build humaniza su `name`.
        // El timeout es amplio porque generar, firmar y registrar de salida la resolución es un
        // trabajo de servidor bastante más lento que una transición normal.
        await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Aceptado', { timeout: 120_000 });
        await expect(page.getByLabel('Fase')).toHaveValue('Resolución de la dirección');

        const pantallaAceptado = page.getByRole('tabpanel', { name: new RegExp(numero) });

        // And: la pantalla de ACEPTADO es la genérica de solo consulta: muestra el panel «Datos de
        // la falta» en solo lectura con «Fecha» a 10/09/2026, «Hora de inicio» a 09:00 y «Hora de
        // fin» a 11:00, sin «Fecha de fin» …
        const panelFaltaAceptado = pantallaAceptado.getByRole('region', { name: 'Datos de la falta' });

        // El tipo de jornada ya no se pinta como grupo de radios (como en la pantalla del CREADOR)
        // sino como un `input` de selección en modo solo lectura.
        const tipoJornada = panelFaltaAceptado.getByRole('combobox', { name: 'Tipo de jornada faltada' });
        await expect(tipoJornada).toHaveValue('Unas horas de un único día');
        await expect(tipoJornada).toHaveAttribute('readonly', '');

        // Los tres campos del periodo son campos que en la pantalla del perfil CREADOR SÍ son
        // editables (el tramo 1 los tecleó): que aquí estén deshabilitados es lo que hace que la
        // vista sea de verdad de solo lectura, y no solo una pantalla sin botones.
        const fecha = panelFaltaAceptado.getByRole('textbox', { name: 'Fecha', exact: true });
        const horaInicio = panelFaltaAceptado.getByRole('textbox', { name: 'Hora de inicio', exact: true });
        const horaFin = panelFaltaAceptado.getByRole('textbox', { name: 'Hora de fin', exact: true });
        await expect(fecha).toHaveValue(FECHA_INICIO);
        await expect(fecha).toBeDisabled();
        await expect(horaInicio).toHaveValue('09:00');
        await expect(horaInicio).toBeDisabled();
        await expect(horaFin).toHaveValue('11:00');
        await expect(horaFin).toBeDisabled();

        // And (cont.): … sin «Fecha de fin» …
        await expect(panelFaltaAceptado.getByRole('textbox', { name: 'Fecha de fin' })).toHaveCount(0);
        // El título es «Fecha», no «Fecha de Inicio»: el periodo de un solo día no se pinta con la
        // etiqueta de los tipos de jornada que abarcan varios días.
        await expect(
          panelFaltaAceptado.getByRole('textbox', { name: 'Fecha de Inicio', exact: true }),
        ).toHaveCount(0);

        // And (cont.): … muestra la resolución en PDF …
        // El documento lo pinta un `iframe` contra el MetaFile que RESOLVER acaba de firmar y
        // registrar de salida; que el `src` apunte a un MetaFile es lo que prueba que hay documento.
        const iframeResolucion = pantallaAceptado.getByTestId(PANEL_PDF_RESOLUCION).locator('iframe');
        await expect(iframeResolucion).toBeVisible();
        await expect(iframeResolucion).toHaveAttribute('src', /com\.axelor\.meta\.db\.MetaFile\/\d+/);

        // And (cont.): … y el bloque «Resolución» …
        // Se localiza por su `name` y no por su título: en esta pantalla hay DOS paneles titulados
        // «Resolución», el del PDF (`pdfResolucion`) y este, el del sentido de la resolución.
        const panelResolucion = pantallaAceptado.getByTestId(PANEL_RESOLUCION_VIEW);
        await expect(panelResolucion).toBeVisible();
        // Dentro del bloque, el sentido de la resolución, también en solo lectura: el campo se
        // titula «Resolución» y lleva lo que el director eligió.
        const resolucion = panelResolucion.getByRole('combobox', { name: 'Resolución' });
        await expect(resolucion).toHaveValue('Resolver positivamente');
        await expect(resolucion).toHaveAttribute('readonly', '');

        // And (cont.): … no hay ningún campo editable en toda la pantalla …
        // No basta con los campos de arriba: se comprueba que en TODO el formulario del expediente
        // no queda ni un control de entrada que se pueda teclear o desplegar.
        await expect(
          pantallaAceptado.locator(
            'input:not([disabled]):not([readonly]), textarea:not([disabled]):not([readonly]), select:not([disabled])',
          ),
        ).toHaveCount(0);

        // And (cont.): … no ofrece ningún evento y su único botón es «Salir».
        const footerAceptado = pantallaAceptado.getByTestId(FOOTER);
        await expect(footerAceptado.getByRole('button')).toHaveText(['Salir']);
        await expect(footerAceptado.getByRole('button', { name: BOTON_RESOLVER })).toHaveCount(0);

        // Then (cont.): ACEPTADO es un estado CERRADO. Lo observable de que lo sea es en qué lista
        // cae el expediente: sale de «Tramitación» → «Pendientes de mí» (donde lo tenía el director
        // en PENDIENTE_RESOLUCION) y aparece en la de cerrados de la unidad que lo tramita
        // («Jefatura de estudios» → «Cerrados»). Se busca siempre por SU número.
        // Antes de cada lista se recarga la aplicación: `abrirMenu` da por hecho que el menú está
        // plegado (como tras un login) y, con el grupo «Tramitación» ya desplegado, lo plegaría.
        await recargarAplicacion(page);
        await expect(await filtrarEnBandeja(page, numero, 'tramitacion-jefaturaDeEstudios-cerrados-menuitem', 'tramitacion-menuitem', 'tramitacion-jefaturaDeEstudios-menuitem')).toHaveCount(1);
        await recargarAplicacion(page);
        await expect(await filtrarEnBandeja(page, numero, 'tramitacion-pendientesDeMi-menuitem', 'tramitacion-menuitem')).toHaveCount(0);
      } finally {
        // Teardown (§5.2 y §5.3): en el camino nominal el expediente acaba en RESOLUCION /
        // ACEPTADO, que es un estado CERRADO y cuyo `events` está vacío. Ahí NO hay DELETE, así que
        // el expediente QUEDA VIVO a propósito: es correcto y no rompe la idempotencia porque el
        // test siempre trabaja con SU número, nunca con «el primero de la bandeja».
        // Pero si el test se corta antes de presentar (o la persona no llega a firmar con
        // AutoFirma), el expediente se queda en ENTRADA, donde sí se puede borrar: se intenta y,
        // si no procede (ya está en VERIFICACION o en RESOLUCION, que tampoco ofrecen DELETE), se
        // deja como está. El borrado exige SESIÓN ABIERTA y el perfil del estado en el que ha
        // quedado el expediente (CREADOR), así que va ANTES del `logout` y reautenticándose como el
        // profesor, porque el tramo 2 lo condujo el TRAMITADOR y porque el fallo de una aserción
        // puede haber dejado la sesión en cualquier punto.
        // DELETE recarga la aplicación entera (refresh-app).
        // El `.catch(() => {})` es intencional: el teardown no debe enmascarar el fallo de una aserción.
        if (numero) {
          await (async () => {
            // Si el test se corta en la puerta manual (nadie firma), el diálogo modal «Cargando
            // AutoFirma» se queda abierto y tapa toda la aplicación. `ensureLoggedOut` navega solo
            // cambiando el hash, sin recargar, así que el diálogo seguiría ahí: se recarga antes.
            await page.reload();
            await ensureLoggedOut(page);
            await login(page, PROFESOR.login, PROFESOR.password);
            await abrirDesdeMisTramites(page, numero);
            const footer = page.getByTestId(FOOTER);
            // El formulario tarda en pintarse tras abrirse la pestaña: se espera a que el footer
            // tenga algún botón (todos los estados ofrecen al menos uno) antes de mirar cuáles hay.
            await expect(footer.getByRole('button').first()).toBeVisible();
            // PENDIENTE_PRESENTACION no ofrece DELETE, pero sí «Atrás», que devuelve el expediente
            // a ENTRADA_DATOS, donde «Borrar el expediente» sí está.
            const atras = footer.getByRole('button', { name: 'Atrás' });
            if (await atras.isVisible()) {
              await atras.click();
              await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Entrada de datos');
              await expect(footer.getByRole('button').first()).toBeVisible();
            }
            const borrar = footer.getByRole('button', { name: 'Borrar el expediente' });
            if (await borrar.isVisible()) {
              await borrar.click();
              await page.getByRole('dialog').getByRole('button', { name: 'Aceptar' }).click();
              // Se comprueba por el NÚMERO del expediente que este test creó, nunca por «el primero».
              await expect(page.getByRole('tab', { name: new RegExp(numero) })).toHaveCount(0);
            }
          })().catch(() => {});
        }
        await logout(page).catch(() => {});
      }
    },
  );
});
