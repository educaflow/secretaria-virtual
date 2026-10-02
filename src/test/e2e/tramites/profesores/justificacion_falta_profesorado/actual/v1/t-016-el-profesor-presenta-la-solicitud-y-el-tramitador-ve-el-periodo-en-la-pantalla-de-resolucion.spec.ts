import { test, expect, Page } from '@playwright/test';
import { inflateSync } from 'zlib';
import { ensureLoggedOut, login, logout } from '../../../../../_support/auth';

// T-016 — El profesor presenta la solicitud, la jefatura la verifica y la dirección ve el periodo en la pantalla de resolución
// origen: ESC —  |  CREADOR | ENTRADA/PENDIENTE_PRESENTACION --PRESENTAR--> VERIFICACION/PENDIENTE_VERIFICACION (--VERIFICAR--> RESOLUCION/PENDIENTE_RESOLUCION)  |  tipo: happy
// MANUAL: el botón «Firmar con AutoFirma y Presentar la solicitud» abre la aplicación de escritorio
//         AutoFirma y exige el certificado digital del profesor instalado en su máquina; la carga de
//         demo no trae ningún certificado.
// Ejecutar con:  E2E_MANUAL=1 npx playwright test --grep @manual --headed
// fuente: .sdd/drafts/2026-09-22_16-01_justificacion-falta-profesorado-fechas/test-e2e-desc/t-016-el-profesor-presenta-la-solicitud-y-el-tramitador-ve-el-periodo-en-la-pantalla-de-resolucion.desc.md

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

// Panel que enseña el PDF de la solicitud (un `iframe` contra el MetaFile generado al
// guardar los datos). Es el documento que AutoFirma firma y que se presenta por registro
// de entrada; el evento PRESENTAR no lo modifica (la firma se guarda como un fichero nuevo).
const PANEL_PDF_SOLICITUD = 'panel:pdfSolicitud';


const BOTON_PRESENTAR = 'Firmar con AutoFirma y Presentar la solicitud';

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


/**
 * Descarga con la sesión del navegador el PDF que hay en `url` y devuelve lo que el documento
 * ha rellenado: el estado de sus **casillas** y el valor de sus **campos**, ambos en el orden
 * en el que el PDF los define.
 *
 * El documento lo genera el servidor a partir de `documentospdf/solicitud.xml`, así que no hay
 * ninguna pantalla donde comprobar su contenido: el `iframe` del formulario lo pinta el visor
 * de PDF del navegador, fuera del DOM. Se lee, por tanto, del propio fichero:
 *   - cada casilla es una apariencia de 9,92 x 9,92 puntos **sin texto**; cuando está marcada,
 *     dibuja además las dos diagonales del aspa (el trazo `1.2 8.72 m … S`);
 *   - cada campo es una apariencia con texto (`/Tx BMC`), cuyo valor es su único `Tj`; los
 *     campos que el documento deja en blanco llevan un espacio duro (` `).
 */
async function contenidoDelPdf(
  page: Page,
  url: string,
): Promise<{ casillas: boolean[]; campos: string[] }> {
  const respuesta = await page.request.get(url);
  expect(respuesta.status()).toBe(200);
  const pdf = (await respuesta.body()).toString('latin1');

  const casillas: boolean[] = [];
  const campos: string[] = [];
  const objetos = /\d+ 0 obj([\s\S]*?)endobj/g;
  let objeto: RegExpExecArray | null;
  while ((objeto = objetos.exec(pdf)) !== null) {
    const flujo = /stream\r?\n([\s\S]*?)\r?\nendstream/.exec(objeto[1]);
    if (!flujo) continue;
    let contenido: string;
    try {
      contenido = inflateSync(Buffer.from(flujo[1], 'latin1')).toString('latin1');
    } catch {
      continue; // no es un flujo comprimido: no es ni una casilla ni un campo
    }
    if (contenido.includes('9.92 9.92 re') && !contenido.includes('BT')) {
      casillas.push(contenido.includes('1.2 8.72 m'));
    } else if (contenido.includes('/Tx BMC')) {
      const texto = /\((.*?)\)Tj/.exec(contenido);
      if (texto) campos.push(texto[1]);
    }
  }
  return { casillas, campos };
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

test.describe('Justificación de falta del profesorado — ENTRADA → VERIFICACION → RESOLUCION', () => {
  test(
    'El profesor presenta la solicitud, la jefatura la verifica y la dirección ve el periodo en la pantalla de resolución',
    { tag: '@manual' },
    async ({ page }) => {
      // El paso manual (abrir AutoFirma, elegir el certificado y firmar) lo hace una persona:
      // no cabe en el `timeout` global de `playwright.config.ts` (90 s). La espera de ese paso
      // tiene su propio timeout de 600 s, y el del test es mayor para que, si la persona no
      // llega a tiempo, falle la aserción de la puerta manual (con su mensaje) y no el test entero.
      test.setTimeout(900_000);

      let numero = '';
      let urlPdfSolicitud = '';
      try {
        // --- Tramo 1: CREADOR (director@mislata.es) — es el tramo del evento PRESENTAR,
        //     el del campo `Perfil` de la descripción; el tramo 2 verifica y el 3 solo consulta ---
        // Given: el profesor `director@mislata.es` (contraseña `demo1234`) tiene un expediente de
        // «Justificación de falta del profesorado» en ENTRADA / PENDIENTE_PRESENTACION, creado
        // con el tipo de jornada faltada «Varios días (todos ellos completos)», «Fecha de Inicio»
        // 10/09/2026, «Fecha de fin» 12/09/2026, «Motivo falta» «Enfermedad común» y
        // `justificante.pdf` adjunto …
        await ensureLoggedOut(page);
        await login(page, PROFESOR.login, PROFESOR.password);
        numero = await crearExpediente(page);
        await expect(page.getByLabel('Fase')).toHaveValue('Entrada');
        await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Entrada de datos');

        const panelFalta = page.getByRole('region', { name: 'Datos de la falta' });
        await panelFalta
          .getByRole('radio', { name: 'Varios días (todos ellos completos)', exact: true })
          .click();

        // Elegir el tipo repinta el panel desde el servidor: hay que esperar a que «Fecha de fin»
        // esté, o el re-render llega después del `fill` y se lleva por delante lo ya tecleado.
        await expect(panelFalta.getByRole('textbox', { name: 'Fecha de Inicio', exact: true })).toBeVisible();
        await expect(panelFalta.getByRole('textbox', { name: 'Fecha de fin', exact: true })).toBeVisible();

        await panelFalta.getByRole('textbox', { name: 'Fecha de Inicio', exact: true }).fill(FECHA_INICIO);
        await panelFalta.getByRole('textbox', { name: 'Fecha de fin', exact: true }).fill(FECHA_FIN_2);
        await panelFalta.getByRole('radio', { name: 'Enfermedad común' }).click();

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

        // Given (cont.): … y tiene AutoFirma instalado con un certificado válido cuyo DNI es el suyo.
        // El servidor no tiene certificado de este profesor (`situacionFirma == 'SIN_CERTIFICADO'`),
        // que es justo el caso en el que la firma la hace AutoFirma en el equipo del profesor: es
        // lo que enseña el panel «Firma de la solicitud» y lo que hace que salga ESTE botón.
        await expect(page.getByRole('region', { name: 'Firma de la solicitud' })).toBeVisible();
        await expect(page.getByTestId(FOOTER).getByRole('button', { name: BOTON_PRESENTAR })).toBeVisible();

        // La URL del PDF de la solicitud se captura ANTES de presentar, porque después esta
        // pantalla desaparece. El fichero no cambia: PRESENTAR guarda la firma en otro MetaFile
        // (`pdfSolicitudFirmada`) y deja intacto el de la solicitud.
        const iframeSolicitud = page.getByTestId(PANEL_PDF_SOLICITUD).locator('iframe');
        await expect(iframeSolicitud).toBeVisible();
        urlPdfSolicitud = (await iframeSolicitud.getAttribute('src'))!;
        expect(urlPdfSolicitud).toContain('com.axelor.meta.db.MetaFile');

        // When: pulsa «Firmar con AutoFirma y Presentar la solicitud», confirma el aviso de que no
        // podrá deshacer la acción y firma en AutoFirma.
        await page.getByTestId(FOOTER).getByRole('button', { name: BOTON_PRESENTAR }).click();
        const aviso = page.getByRole('dialog');
        await expect(aviso).toContainText('¿Esta seguro que desea presentar la documentación?');
        await expect(aviso).toContainText('No podrá deshacer esta acción');
        await aviso.getByRole('button', { name: 'Aceptar' }).click();

        // === PASO MANUAL ===
        // Al aceptar, la aplicación lanza AutoFirma en el equipo de quien ejecuta el test.
        // LA PERSONA DEBE: dejar que se abra AutoFirma, elegir en el diálogo el certificado digital
        // del profesor `director@mislata.es` (su DNI debe coincidir con el del usuario), introducir
        // el PIN o la contraseña del certificado si se la pide y confirmar la firma.
        // Ninguna automatización puede hacerlo: AutoFirma es una aplicación de escritorio y el
        // certificado vive en la máquina del firmante.
        // El test continúa solo cuando el efecto de la firma es visible en la UI: el expediente ha
        // transicionado. No se espera un tiempo fijo ni se hace `page.pause()`, para que el test
        // siga fallando si la persona cancela AutoFirma o firma con el certificado equivocado.

        // Then: el expediente pasa a la fase VERIFICACION, estado PENDIENTE_VERIFICACION.
        await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Pendiente de verificación', {
          timeout: 600_000,
        });
        await expect(page.getByLabel('Fase')).toHaveValue('Verificación');

        // And: en el PDF de la solicitud, el bloque «Declara que» tiene marcada la casilla
        // «Desde el 10/09/2026 hasta el 12/09/2026» y las otras tres casillas de tipo de jornada
        // están sin marcar.
        // Las cuatro primeras casillas del documento son, en orden, las cuatro del bloque
        // «Declara que» («El día …», «Desde el … hasta el …», «El día …, de … a … horas» y
        // «El día … desde las … horas, hasta el …»); la quinta y última es la de la declaración
        // responsable, que el documento marca siempre.
        const solicitud = await contenidoDelPdf(page, urlPdfSolicitud);
        expect(solicitud.casillas).toHaveLength(5);
        expect(solicitud.casillas.slice(0, 4)).toEqual([false, true, false, false]);

        // And (cont.): la casilla marcada es la del periodo, y no otra: las fechas solo aparecen en
        // los campos de ESA fila. El documento es bilingüe y pinta cada valor dos veces (columna en
        // valenciano y columna en castellano), así que cada fecha sale exactamente dos veces; si el
        // periodo se hubiera escrito además en cualquier otra fila, saldrían más.
        expect(solicitud.campos.filter((campo) => campo === FECHA_INICIO)).toHaveLength(2);
        expect(solicitud.campos.filter((campo) => campo === FECHA_FIN_2)).toHaveLength(2);

        // --- Tramo 2: TRAMITADOR (jefeestudios1@mislata.es) — la jefatura de estudios verifica ---
        // And: al iniciar sesión `jefeestudios1@mislata.es` (contraseña `demo1234`) y abrir el
        // expediente por la lista «Tramitación» → «Pendientes de mí», la pantalla del perfil
        // TRAMITADOR —la de verificar la solicitud— muestra en solo lectura el panel «Datos de la
        // falta» …
        await logout(page);
        await login(page, TRAMITADOR.login, TRAMITADOR.password);
        await abrirDesdeBandeja(page, numero, 'tramitacion-pendientesDeMi-menuitem', 'tramitacion-menuitem');

        const pantallaTramitador = page.getByRole('tabpanel', { name: new RegExp(numero) });
        await expect(page.getByLabel('Fase')).toHaveValue('Verificación');
        await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Pendiente de verificación');

        // And (cont.): … con el tipo de jornada faltada «Varios días (todos ellos completos)» …
        // En la pantalla del TRAMITADOR el tipo de jornada no se pinta como grupo de radios (como
        // en la del CREADOR) sino como un `input` de selección en modo solo lectura.
        const panelTramitador = pantallaTramitador.getByRole('region', { name: 'Datos de la falta' });
        const tipoJornadaTramitador = panelTramitador.getByRole('combobox', { name: 'Tipo de jornada faltada' });
        await expect(tipoJornadaTramitador).toHaveValue('Varios días (todos ellos completos)');
        await expect(tipoJornadaTramitador).toHaveAttribute('readonly', '');

        // And (cont.): … «Fecha de Inicio» a 10/09/2026 y «Fecha de fin» a 12/09/2026 …
        // Los dos son campos que en la pantalla del perfil CREADOR SÍ son editables (el tramo 1 los
        // tecleó): que aquí estén deshabilitados es lo que hace que la vista sea de solo lectura.
        const fechaInicioTramitador = panelTramitador.getByRole('textbox', { name: 'Fecha de Inicio', exact: true });
        const fechaFinTramitador = panelTramitador.getByRole('textbox', { name: 'Fecha de fin', exact: true });
        await expect(fechaInicioTramitador).toHaveValue(FECHA_INICIO);
        await expect(fechaInicioTramitador).toBeDisabled();
        await expect(fechaFinTramitador).toHaveValue(FECHA_FIN_2);
        await expect(fechaFinTramitador).toBeDisabled();

        // And (cont.): … sin «Hora de inicio» ni «Hora de fin» …
        await expect(panelTramitador.getByRole('textbox', { name: 'Hora de inicio', exact: true })).toHaveCount(0);
        await expect(panelTramitador.getByRole('textbox', { name: 'Hora de fin', exact: true })).toHaveCount(0);

        // And (cont.): … y ofrece el panel «Verificación de la solicitud» y el botón «Siguiente».
        const panelVerificacion = pantallaTramitador.getByRole('region', { name: 'Verificación de la solicitud' });
        await expect(panelVerificacion).toBeVisible();
        const footerTramitador = pantallaTramitador.getByTestId(FOOTER);
        await expect(footerTramitador.getByRole('button')).toHaveText(['Siguiente']);

        // And: la jefatura elige «La solicitud es correcta» y pulsa «Siguiente» (evento VERIFICAR) …
        const solicitudCorrecta = panelVerificacion.getByRole('radio', { name: 'La solicitud es correcta', exact: true });
        await solicitudCorrecta.click();
        // El botón solo se pulsa cuando la elección ha cuajado en el formulario: si se pulsara
        // antes, el evento viajaría sin `resultadoVerificacion` y el expediente no avanzaría.
        await expect(solicitudCorrecta).toHaveAttribute('aria-checked', 'true');
        // VERIFICAR no pide confirmación.
        await footerTramitador.getByRole('button', { name: 'Siguiente' }).click();

        // And (cont.): … el expediente pasa a la fase RESOLUCION, estado PENDIENTE_RESOLUCION, y la
        // cabecera muestra «Resolución de la dirección» y «Pendiente de resolución».
        await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Pendiente de resolución');
        await expect(page.getByLabel('Fase')).toHaveValue('Resolución de la dirección');

        // And (cont.): … como PENDIENTE_RESOLUCION es del perfil DIRECTOR y no tiene pantalla para
        // el TRAMITADOR, lo que le queda a la jefatura es la vista genérica de solo consulta …
        const pantallaConsulta = page.getByRole('tabpanel', { name: new RegExp(numero) });

        // And (cont.): … el panel «Datos de la falta» sale en solo lectura con el tipo de jornada
        // faltada «Varios días (todos ellos completos)» …
        const panelConsulta = pantallaConsulta.getByRole('region', { name: 'Datos de la falta' });
        const tipoJornadaConsulta = panelConsulta.getByRole('combobox', { name: 'Tipo de jornada faltada' });
        await expect(tipoJornadaConsulta).toHaveValue('Varios días (todos ellos completos)');
        await expect(tipoJornadaConsulta).toHaveAttribute('readonly', '');

        // And (cont.): … el campo de la fecha titulado «Fecha de Inicio» (no «Fecha») a 10/09/2026 y
        // «Fecha de fin» a 12/09/2026 …
        const fechaInicioConsulta = panelConsulta.getByRole('textbox', { name: 'Fecha de Inicio', exact: true });
        const fechaFinConsulta = panelConsulta.getByRole('textbox', { name: 'Fecha de fin', exact: true });
        await expect(fechaInicioConsulta).toHaveValue(FECHA_INICIO);
        await expect(fechaInicioConsulta).toBeDisabled();
        await expect(fechaFinConsulta).toHaveValue(FECHA_FIN_2);
        await expect(fechaFinConsulta).toBeDisabled();
        // El título es «Fecha de Inicio», no «Fecha»: el periodo no se pinta con la etiqueta del
        // tipo de jornada de un solo día.
        await expect(panelConsulta.getByRole('textbox', { name: 'Fecha', exact: true })).toHaveCount(0);

        // And (cont.): … sin «Hora de inicio» ni «Hora de fin» …
        await expect(panelConsulta.getByRole('textbox', { name: 'Hora de inicio', exact: true })).toHaveCount(0);
        await expect(panelConsulta.getByRole('textbox', { name: 'Hora de fin', exact: true })).toHaveCount(0);

        // And (cont.): … no hay ningún campo editable …
        // No basta con los campos de arriba: se comprueba que en TODO el formulario del expediente
        // no queda ni un control de entrada que se pueda teclear o desplegar.
        await expect(
          pantallaConsulta.locator(
            'input:not([disabled]):not([readonly]), textarea:not([disabled]):not([readonly]), select:not([disabled])',
          ),
        ).toHaveCount(0);

        // And (cont.): … y el único botón es «Salir».
        const footerConsulta = pantallaConsulta.getByTestId(FOOTER);
        await expect(footerConsulta.getByRole('button')).toHaveText(['Salir']);
        await expect(footerConsulta.getByRole('button', { name: 'Resolver el expediente' })).toHaveCount(0);

        // --- Tramo 3: DIRECTOR (director@mislata.es), la pantalla de resolución ---
        // And: al iniciar sesión de nuevo `director@mislata.es` (contraseña `demo1234`) y abrir el
        // expediente por la lista «Tramitación» → «Pendientes de mí», la pantalla del perfil
        // DIRECTOR —la de resolver— muestra en solo lectura el panel «Datos de la falta» …
        // En PENDIENTE_RESOLUCION el expediente espera al perfil DIRECTOR. El servidor abre el
        // expediente con el perfil del estado si el usuario lo ostenta, así que al director le pinta
        // la pantalla de resolver aunque sobre este expediente sea además el CREADOR.
        await logout(page);
        await login(page, DIRECTOR.login, DIRECTOR.password);
        await abrirDesdeBandeja(page, numero, 'tramitacion-pendientesDeMi-menuitem', 'tramitacion-menuitem');

        const pantallaDirector = page.getByRole('tabpanel', { name: new RegExp(numero) });
        await expect(page.getByLabel('Fase')).toHaveValue('Resolución de la dirección');
        await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Pendiente de resolución');

        // And (cont.): … con el tipo de jornada faltada «Varios días (todos ellos completos)» …
        const panelDirector = pantallaDirector.getByRole('region', { name: 'Datos de la falta' });
        const tipoJornadaDirector = panelDirector.getByRole('combobox', { name: 'Tipo de jornada faltada' });
        await expect(tipoJornadaDirector).toHaveValue('Varios días (todos ellos completos)');
        await expect(tipoJornadaDirector).toHaveAttribute('readonly', '');

        // And (cont.): … «Fecha de Inicio» a 10/09/2026 y «Fecha de fin» a 12/09/2026 …
        const fechaInicioDirector = panelDirector.getByRole('textbox', { name: 'Fecha de Inicio', exact: true });
        const fechaFinDirector = panelDirector.getByRole('textbox', { name: 'Fecha de fin', exact: true });
        await expect(fechaInicioDirector).toHaveValue(FECHA_INICIO);
        await expect(fechaInicioDirector).toBeDisabled();
        await expect(fechaFinDirector).toHaveValue(FECHA_FIN_2);
        await expect(fechaFinDirector).toBeDisabled();

        // And (cont.): … sin «Hora de inicio» ni «Hora de fin» …
        await expect(panelDirector.getByRole('textbox', { name: 'Hora de inicio', exact: true })).toHaveCount(0);
        await expect(panelDirector.getByRole('textbox', { name: 'Hora de fin', exact: true })).toHaveCount(0);

        // And (cont.): … y ofrece el panel «Resolver expediente» y el botón «Resolver el expediente» …
        await expect(pantallaDirector.getByRole('region', { name: 'Resolver expediente' })).toBeVisible();
        await expect(
          pantallaDirector.getByTestId(FOOTER).getByRole('button', { name: 'Resolver el expediente' }),
        ).toBeVisible();

        // And (cont.): … y el expediente **sigue** en RESOLUCION / PENDIENTE_RESOLUCION.
        await expect(page.getByLabel('Fase')).toHaveValue('Resolución de la dirección');
        await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Pendiente de resolución');
      } finally {
        // Teardown (§5.2 y §5.3): en el camino nominal el expediente acaba en RESOLUCION /
        // PENDIENTE_RESOLUCION, cuyo único evento es RESOLVER. Ahí NO hay DELETE (tampoco en
        // VERIFICACION / PENDIENTE_VERIFICACION, si el test se corta tras presentar), así que el
        // expediente QUEDA VIVO a propósito: es correcto y no rompe la idempotencia porque el test
        // siempre trabaja con SU número, nunca con «el primero de la bandeja».
        // Pero si el test se corta antes del paso manual (o la persona no llega a firmar), el
        // expediente se queda en ENTRADA, donde sí se puede borrar: se intenta y, si no procede,
        // se deja como está. El borrado exige SESIÓN ABIERTA y el perfil del estado en el que ha
        // quedado el expediente (CREADOR), así que va ANTES del `logout` y reautenticándose como el
        // profesor, porque el fallo de una aserción puede haber dejado la sesión en cualquier punto.
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
