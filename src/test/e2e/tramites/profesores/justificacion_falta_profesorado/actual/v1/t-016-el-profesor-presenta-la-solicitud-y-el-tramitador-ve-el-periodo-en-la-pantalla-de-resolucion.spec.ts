import { test, expect, Page } from '@playwright/test';
import { inflateSync } from 'zlib';
import { ensureLoggedOut, login, logout } from '../../../../../_support/auth';

// T-016 — El profesor presenta la solicitud y el tramitador ve el periodo en la pantalla de resolución
// origen: ESC —  |  CREADOR | RECEPCION/PENDIENTE_PRESENTACION --PRESENTAR--> TRAMITACION/PENDIENTE_RESOLUCION  |  tipo: happy
// MANUAL: el botón «Firmar con AutoFirma y Presentar la solicitud» abre la aplicación de escritorio
//         AutoFirma y exige el certificado digital del profesor instalado en su máquina; la carga de
//         demo no trae ningún certificado.
// Ejecutar con:  E2E_MANUAL=1 npx playwright test --grep @manual --headed
// fuente: .sdd/drafts/2026-09-22_16-01_justificacion-falta-profesorado-fechas/test-e2e-desc/t-016-el-profesor-presenta-la-solicitud-y-el-tramitador-ve-el-periodo-en-la-pantalla-de-resolucion.desc.md

const TRAMITE = 'Justificación de falta del profesorado';
const TIPO_TRAMITE = 'Trámites para el profesor';

const PROFESOR = { login: 'director@mislata.es', password: 'demo1234' };
const TRAMITADOR = { login: 'jefeestudios1@mislata.es', password: 'demo1234' };

// El footer del expediente (un botón por evento disponible) lo pinta la plantilla común
// del tramitador; es el sitio donde se dispara la transición y donde se comprueba qué
// eventos ofrece el estado para el perfil con el que se ha abierto el expediente.
const FOOTER = 'panel:subsysExpedientes-template-footer-panel';

// Panel que enseña el PDF de la solicitud (un `iframe` contra el MetaFile generado al
// guardar los datos). Es el documento que AutoFirma firma y que se presenta por registro
// de entrada; el evento PRESENTAR no lo modifica (la firma se guarda como un fichero nuevo).
const PANEL_PDF_SOLICITUD = 'panel:pdfSolicitud';

const BANDEJA_PENDIENTES = 'expedientes-expedientesPendientes-menuitem';
const BANDEJA_ESPERANDO = 'expedientes-expedientesEsperando-menuitem';

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

test.describe('Justificación de falta del profesorado — RECEPCION → TRAMITACION', () => {
  test(
    'El profesor presenta la solicitud y el tramitador ve el periodo en la pantalla de resolución',
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
        //     el del campo `Perfil` de la descripción; los tramos 2 y 3 solo consultan ---
        // Given: el profesor `director@mislata.es` (contraseña `demo1234`) tiene un expediente de
        // «Justificación de falta del profesorado» en RECEPCION / PENDIENTE_PRESENTACION, creado
        // con el tipo de jornada faltada «Varios días (todos ellos completos)», «Fecha de Inicio»
        // 10/09/2026, «Fecha de fin» 12/09/2026, «Motivo falta» «Enfermedad común» y
        // `justificante.pdf` adjunto …
        await ensureLoggedOut(page);
        await login(page, PROFESOR.login, PROFESOR.password);
        numero = await crearExpediente(page);
        await expect(page.getByLabel('Fase')).toHaveValue('Recepción');
        await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Entrada de datos');

        const panelFalta = page.getByRole('region', { name: 'Datos de la falta' });
        await panelFalta
          .getByRole('radio', { name: 'Varios días (todos ellos completos)', exact: true })
          .click();

        // Elegir el tipo repinta el panel desde el servidor: hay que esperar a que «Fecha de fin»
        // esté, o el re-render llega después del `fill` y se lleva por delante lo ya tecleado.
        await expect(panelFalta.getByRole('textbox', { name: 'Fecha de Inicio', exact: true })).toBeVisible();
        await expect(panelFalta.getByRole('textbox', { name: 'Fecha de fin', exact: true })).toBeVisible();

        await panelFalta.getByRole('textbox', { name: 'Fecha de Inicio', exact: true }).fill('10/09/2026');
        await panelFalta.getByRole('textbox', { name: 'Fecha de fin', exact: true }).fill('12/09/2026');
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
        await expect(page.getByLabel('Fase')).toHaveValue('Recepción');
        await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Pendiente de presentación');

        // Given (cont.): … y tiene AutoFirma instalado con un certificado válido cuyo DNI es el suyo.
        // El servidor no tiene certificado de este profesor (`situacionFirma == 'SIN_CERTIFICADO'`),
        // que es justo el caso en el que la firma la hace AutoFirma en el equipo del profesor: es
        // lo que enseña el panel «Firma de la solicitud» y lo que hace que salga ESTE botón.
        await expect(page.getByRole('region', { name: 'Firma de la solicitud' })).toBeVisible();
        await expect(page.getByTestId(FOOTER).getByRole('button', { name: BOTON_PRESENTAR })).toBeVisible();

        // La URL del PDF de la solicitud se captura ANTES de presentar, porque después esta
        // pantalla desaparece. El fichero no cambia: PRESENTAR guarda la firma en otro MetaFile
        // (`pdfSolicitudFirmado`) y deja intacto el de la solicitud.
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

        // Then: el expediente pasa a la fase TRAMITACION, estado PENDIENTE_RESOLUCION.
        await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Pendiente de resolución', {
          timeout: 600_000,
        });
        await expect(page.getByLabel('Fase')).toHaveValue('Tramitación');

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
        expect(solicitud.campos.filter((campo) => campo === '10/09/2026')).toHaveLength(2);
        expect(solicitud.campos.filter((campo) => campo === '12/09/2026')).toHaveLength(2);

        // --- Tramo 2: TRAMITADOR (jefeestudios1@mislata.es) ---
        // And: al iniciar sesión `jefeestudios1@mislata.es` (contraseña `demo1234`) y abrir el
        // expediente por la bandeja «Expedientes esperando a que otra persona realice una tarea»,
        // la pantalla del perfil TRAMITADOR muestra en solo lectura el panel «Datos de la falta» …
        await logout(page);
        await login(page, TRAMITADOR.login, TRAMITADOR.password);
        await abrirDesdeBandeja(page, BANDEJA_ESPERANDO, numero);

        const pantallaTramitador = page.getByRole('tabpanel', { name: new RegExp(numero) });
        await expect(page.getByLabel('Fase')).toHaveValue('Tramitación');
        await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Pendiente de resolución');

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
        await expect(fechaInicioTramitador).toHaveValue('10/09/2026');
        await expect(fechaInicioTramitador).toBeDisabled();
        await expect(fechaFinTramitador).toHaveValue('12/09/2026');
        await expect(fechaFinTramitador).toBeDisabled();

        // And (cont.): … sin «Hora de inicio» ni «Hora de fin» …
        await expect(panelTramitador.getByRole('textbox', { name: 'Hora de inicio', exact: true })).toHaveCount(0);
        await expect(panelTramitador.getByRole('textbox', { name: 'Hora de fin', exact: true })).toHaveCount(0);

        // And (cont.): … y ofrece el botón «Resolver el expediente».
        await expect(
          pantallaTramitador.getByTestId(FOOTER).getByRole('button', { name: 'Resolver el expediente' }),
        ).toBeVisible();

        // --- Tramo 3: CREADOR (director@mislata.es), vista genérica de solo consulta ---
        // And: al iniciar sesión de nuevo `director@mislata.es` (contraseña `demo1234`) y abrir el
        // expediente entrando por la bandeja «Expedientes Pendientes» («Listado de expedientes
        // pendientes de que realices la tarea»), que es la del perfil CREADOR, como
        // PENDIENTE_RESOLUCION no tiene pantalla para ese perfil el sistema abre la vista genérica
        // de solo consulta …
        await logout(page);
        await login(page, PROFESOR.login, PROFESOR.password);
        await abrirDesdeBandeja(page, BANDEJA_PENDIENTES, numero);

        const pantallaCreador = page.getByRole('tabpanel', { name: new RegExp(numero) });

        // And (cont.): … el panel «Datos de la falta» sale en solo lectura con el tipo de jornada
        // faltada «Varios días (todos ellos completos)» …
        const panelCreador = pantallaCreador.getByRole('region', { name: 'Datos de la falta' });
        const tipoJornadaCreador = panelCreador.getByRole('combobox', { name: 'Tipo de jornada faltada' });
        await expect(tipoJornadaCreador).toHaveValue('Varios días (todos ellos completos)');
        await expect(tipoJornadaCreador).toHaveAttribute('readonly', '');

        // And (cont.): … el campo de la fecha titulado «Fecha de Inicio» (no «Fecha») a 10/09/2026 y
        // «Fecha de fin» a 12/09/2026 …
        const fechaInicioCreador = panelCreador.getByRole('textbox', { name: 'Fecha de Inicio', exact: true });
        const fechaFinCreador = panelCreador.getByRole('textbox', { name: 'Fecha de fin', exact: true });
        await expect(fechaInicioCreador).toHaveValue('10/09/2026');
        await expect(fechaInicioCreador).toBeDisabled();
        await expect(fechaFinCreador).toHaveValue('12/09/2026');
        await expect(fechaFinCreador).toBeDisabled();
        // El título es «Fecha de Inicio», no «Fecha»: el periodo no se pinta con la etiqueta del
        // tipo de jornada de un solo día.
        await expect(panelCreador.getByRole('textbox', { name: 'Fecha', exact: true })).toHaveCount(0);

        // And (cont.): … sin «Hora de inicio» ni «Hora de fin» …
        await expect(panelCreador.getByRole('textbox', { name: 'Hora de inicio', exact: true })).toHaveCount(0);
        await expect(panelCreador.getByRole('textbox', { name: 'Hora de fin', exact: true })).toHaveCount(0);

        // And (cont.): … no hay ningún campo editable …
        // No basta con los campos de arriba: se comprueba que en TODO el formulario del expediente
        // no queda ni un control de entrada que se pueda teclear o desplegar.
        await expect(
          pantallaCreador.locator(
            'input:not([disabled]):not([readonly]), textarea:not([disabled]):not([readonly]), select:not([disabled])',
          ),
        ).toHaveCount(0);

        // And (cont.): … el único botón es «Salir» …
        const footerCreador = pantallaCreador.getByTestId(FOOTER);
        await expect(footerCreador.getByRole('button')).toHaveText(['Salir']);
        await expect(footerCreador.getByRole('button', { name: 'Resolver el expediente' })).toHaveCount(0);

        // And (cont.): … y el expediente **sigue** en TRAMITACION / PENDIENTE_RESOLUCION.
        await expect(page.getByLabel('Fase')).toHaveValue('Tramitación');
        await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Pendiente de resolución');
      } finally {
        // Teardown (§5.2 y §5.3): en el camino nominal el expediente acaba en TRAMITACION /
        // PENDIENTE_RESOLUCION, cuyo único evento es RESOLVER. Ahí NO hay DELETE, así que el
        // expediente QUEDA VIVO a propósito: es correcto y no rompe la idempotencia porque el test
        // siempre trabaja con SU número, nunca con «el primero de la bandeja».
        // Pero si el test se corta antes del paso manual (o la persona no llega a firmar), el
        // expediente se queda en RECEPCION, donde sí se puede borrar: se intenta y, si no procede,
        // se deja como está. El borrado exige SESIÓN ABIERTA y el perfil del estado en el que ha
        // quedado el expediente (CREADOR), así que va ANTES del `logout` y reautenticándose como el
        // profesor, porque el fallo de una aserción puede haber dejado la sesión en cualquier punto.
        // DELETE recarga la aplicación entera (refresh-app).
        // El `.catch(() => {})` es intencional: el teardown no debe enmascarar el fallo de una aserción.
        if (numero) {
          await (async () => {
            await ensureLoggedOut(page);
            await login(page, PROFESOR.login, PROFESOR.password);
            await abrirDesdeBandeja(page, BANDEJA_PENDIENTES, numero);
            const footer = page.getByTestId(FOOTER);
            // PENDIENTE_PRESENTACION no ofrece DELETE, pero sí «Atrás», que devuelve el expediente
            // a ENTRADA_DATOS, donde «Borrar el expediente» sí está.
            const atras = footer.getByRole('button', { name: 'Atrás' });
            if (await atras.isVisible()) {
              await atras.click();
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
