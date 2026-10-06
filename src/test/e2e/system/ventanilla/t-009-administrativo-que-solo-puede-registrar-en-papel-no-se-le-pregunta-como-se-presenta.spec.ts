import { test, expect, Locator, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-009 — Administrativo que solo puede registrar en papel: no se le pregunta cómo se presenta
// origen: ESC-009  |  verifica: U-nuevo-expediente-006, U-nuevo-expediente-008, U-nuevo-expediente-009, U-nuevo-expediente-010, U-nuevo-expediente-013, R-AsistenteNuevoExpediente-001, R-AsistenteNuevoExpediente-005
// fuente: .sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/test-e2e-desc/t-009-administrativo-que-solo-puede-registrar-en-papel-no-se-le-pregunta-como-se-presenta.desc.md

/**
 * IDEMPOTENCIA (§4 del contrato de generación) — este test CREA un expediente, y su
 * identificador (`00024/2026-46019660`) lo asigna el servidor con un contador, así que NO se
 * puede aislar con un sufijo `Date.now()` en un nombre: no hay ningún nombre que el
 * test elija. La idempotencia se consigue de las otras dos formas:
 *   - el expediente creado se identifica por SU número, capturado en tiempo de
 *     ejecución del título de su pestaña (nunca por un número fijo), y
 *   - se BORRA en el `finally`, de modo que la BD compartida queda como estaba.
 * El borrado no puede hacerse sobre la pestaña recién creada porque el test navega
 * antes a los listados para comprobar el centro (Axelor sustituye la pestaña del
 * expediente al abrir otra vista): por eso el teardown lo reabre desde la bandeja del
 * estado en que quedó, donde el servidor lo abre con el perfil que declara ese estado y,
 * por tanto, con el botón «Borrar el expediente» (lo ofrecen los dos estados por los que
 * pasa el test, PENDIENTE_DOCUMENTO_ESCANEADO y ENTRADA_DATOS). Arrancar el teardown con un `goto` lo
 * hace robusto aunque el test falle con un diálogo abierto o a medio navegar.
 * No hace falta pre-limpieza defensiva: ninguna regla de negocio limita cuántos
 * expedientes de este trámite puede registrar el administrativo, así que un expediente
 * residual de un run que abortara no impide crear otro ni cambia ninguna aserción (el
 * test no cuenta expedientes: filtra los listados por su propio número).
 */

// Credenciales del usuario de la precondición (tabla «Usuarios de acceso» del .desc.md).
// administrativo1@mislata.es es el caso clave del escenario: sobre los trámites del
// alumno solo tiene permiso para REGISTRARLOS EN PAPEL, así que el asistente no tiene
// nada que preguntarle sobre cómo se presenta.
const USUARIO = 'administrativo1@mislata.es';
const CONTRASENA = 'demo1234';

// Datos del estado inicial de la BD que el test da por sentados.
const CENTRO = 'CIPFP Mislata';
const TRAMITE = 'Anulación de matrícula en ciclo formativo';
const TIPO_TRAMITE_ALUMNO = 'Trámites para el alumno';
const TIPO_TRAMITE_PROFESOR = 'Trámites para el profesor';
// Identidad del administrativo tal y como la carga `data-demo/input/usuarios-demo.xml`;
// es el nombre con el que la aplicación lo pinta en «Creado por».
const ADMINISTRATIVO_NOMBRE_COMPLETO = 'Administrativo1 CIPFP Mislata';

// Títulos de las TRES pantallas del asistente (los fijan los `action-view` de
// `system/ventanilla/views/`). Se usan para comprobar tanto que se abre la que toca
// como que al final NO queda ninguna abierta.
const PANTALLA_CENTRO = 'Nuevo expediente: elija el centro';
const PANTALLA_TRAMITE = 'Nuevo expediente: elija el trámite';
const PANTALLA_CONTEXTO = 'Nuevo expediente';

// Rótulos de las dos preguntas del último paso del asistente (los `title` de
// `presentadoEnPapel` y `presentadoEnRepresentacion` en `AsistenteNuevoExpediente.xml`).
const PREGUNTA_COMO_SE_PRESENTA = '¿Cómo se presenta?';
const PREGUNTA_PARA_QUIEN = '¿Para quién es el expediente?';

// Las dos opciones de «¿Para quién es el expediente?» (los `x-false-text`/`x-true-text`
// del widget `boolean-radio`): «Para mí» es `presentadoEnRepresentacion = false`.
const OPCION_PARA_MI = 'Para mí';
const OPCION_EN_REPRESENTACION =
  'Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)';

// Primer estado del tipo de expediente cuando se registra EN PAPEL, según
// `InitialEventManagerImpl`: fase ENTRADA, estado PENDIENTE_DOCUMENTO_ESCANEADO.
// Quien lo presenta él mismo por vía telemática arranca en ENTRADA_DATOS, así que
// este estado es, por sí solo, la prueba de que el expediente quedó en papel.
const FASE_INICIAL = 'Entrada';
const ESTADO_INICIAL_EN_PAPEL = 'Pendiente de adjuntar la solicitud en papel escaneada';

// Estado siguiente de la misma fase. Es el primero en el que el expediente YA CREADO delata
// si está o no en representación (ver la última sección del test).
const ESTADO_ENTRADA_DATOS = 'Entrada de datos';
const NOMBRE_PDF = 'solicitud-escaneada.pdf';

// Aviso del form de PENDIENTE_DOCUMENTO_ESCANEADO con perfil TRAMITADOR: verlo prueba
// que la aplicación trata el expediente como registrado en papel.
const AVISO_EN_PAPEL =
  'Adjunte escaneada en PDF la solicitud que ha entregado firmada la persona que la presenta';

// Aviso que la vista pinta a quien presenta por vía TELEMÁTICA (estado ENTRADA_DATOS,
// `showIf="!presentadoEnPapel"`): su ausencia es la otra cara de la misma comprobación.
const AVISO_PRESENTA_EL_MISMO =
  'Para presentar la solicitud necesitará firmarla con su certificado digital desde este mismo ordenador';

// Panel que la plantilla común (`tramites/shared/template-views.xml`) pinta con
// `showIf="presentadoEnRepresentacion"`, en sus dos variantes: `persona-solicitante`
// (solo lectura) y `persona-solicitante-editable`. Verlo o no verlo solo significa algo
// en un formulario que lo incluya en su `<include-panels>`, que es una ALLOWLIST.
const PANEL_PERSONA_SOLICITANTE = 'Persona que presenta la solicitud';

// Título de la pestaña del expediente creado: <nº>/<año>-<código del centro>-<trámite> V1
// (p. ej. `00072/2026-46019660-Anulación de matrícula en ciclo formativo V1`).
const SUFIJO_TITULO_EXPEDIENTE = `-${TRAMITE} V1`;
const TITULO_EXPEDIENTE = new RegExp(`^\\d+/\\d{4}-\\d+${SUFIJO_TITULO_EXPEDIENTE}$`);

/**
 * PDF mínimo válido (1 página en blanco) construido EN MEMORIA. El test es de regresión
 * versionado: MUST NOT depender de ningún fichero fuera del repositorio. Su contenido es
 * indiferente —aquí nadie lo lee—, solo tiene que pasar el `FileType(application/pdf)` que
 * `StateEventValidatorImpl` exige para salir de PENDIENTE_DOCUMENTO_ESCANEADO.
 */
function pdfMinimo(): Buffer {
  const objetos = [
    '1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n',
    '2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n',
    '3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << >> >>\nendobj\n',
  ];
  let pdf = '%PDF-1.4\n';
  const offsets: number[] = [];
  for (const objeto of objetos) {
    offsets.push(pdf.length);
    pdf += objeto;
  }
  const inicioXref = pdf.length;
  pdf += 'xref\n0 4\n0000000000 65535 f \n';
  pdf += offsets.map((o) => String(o).padStart(10, '0') + ' 00000 n \n').join('');
  pdf += `trailer\n<< /Size 4 /Root 1 0 R >>\nstartxref\n${inicioXref}\n%%EOF\n`;
  return Buffer.from(pdf, 'latin1');
}

/**
 * Filas de datos del árbol de trámites (excluye cabecera y filas de agrupación). El
 * `panel-related` agrupado con testids `field:tramitesDisponibles`/`row:`/`group-row:`
 * fue sustituido por un `<tree>` embebido en un `panel-dashlet` (commit `98755ea`): sus
 * nodos no llevan `data-testid` propio, así que se localizan por rol ARIA de un
 * `treegrid` — `aria-level="2"` son las filas hoja (los trámites), `aria-level="1"` las
 * de agrupación (el tipo de trámite).
 */
function filasDeTramites(page: Page): Locator {
  return page.getByTestId('panel:tramitesPanel').locator('[role="row"][aria-level="2"]');
}

/** Filas de agrupación por tipo de trámite del árbol de trámites. */
function gruposDeTipoTramite(page: Page): Locator {
  return page.getByTestId('panel:tramitesPanel').locator('[role="row"][aria-level="1"]');
}

/**
 * Despliega todos los grupos de tipo de trámite del árbol. A diferencia del grid
 * agrupado que sustituyó (que mostraba sus filas ya desplegadas), el `<tree>` nace con
 * los grupos plegados (icono `arrow_right`, sin `aria-expanded`) y hay que pulsarlos
 * para que sus filas de datos existan en el DOM.
 */
async function desplegarGruposDeTramites(page: Page): Promise<void> {
  const grupos = gruposDeTipoTramite(page);
  await grupos.first().waitFor();
  const total = await grupos.count();
  for (let i = 0; i < total; i++) {
    const grupo = grupos.nth(i);
    if ((await grupo.getAttribute('aria-expanded')) !== 'true') {
      await grupo.click();
    }
  }
}

/**
 * El radio de una de las opciones de «¿Para quién es el expediente?». El widget
 * `boolean-radio` de Axelor pinta cada opción como `<div><input type="radio"><span>texto
 * </span></div>`, SIN `<label>` (y con el mismo `id` en los dos inputs), así que la
 * opción no se puede localizar por su nombre accesible: se localiza el `div` que
 * contiene su texto y, dentro, su radio. Así la aserción sigue atada al texto que lee
 * el usuario, no a la posición ni al valor interno.
 */
function opcionParaQuien(page: Page, texto: string): Locator {
  return page
    .getByTestId('field:presentadoEnRepresentacion')
    .locator('div:has(> [data-testid="radio"])')
    .filter({ hasText: texto })
    .getByRole('radio');
}

/**
 * Abre una entrada del menú lateral. El grupo se pliega y despliega al pulsarlo, así
 * que solo se despliega si la entrada no se ve: pulsarlo a ciegas lo cerraría cuando ya
 * venía abierto.
 */
async function abrirEntradaDeMenu(page: Page, grupo: string, entrada: string): Promise<void> {
  const item = page.getByTestId(`item:${entrada}`);
  if (!(await item.isVisible())) {
    await page.getByTestId(`item:${grupo}`).getByTestId('title').first().click();
  }
  await item.click();
}

/** Filas de datos de un listado de expedientes (la fila de búsqueda no lleva `row:`). */
function filasDeExpedientes(page: Page): Locator {
  return page.getByRole('grid').locator('[data-testid^="row:"]');
}

/**
 * Acota un listado de expedientes al número indicado con el buscador por columna del
 * grid. Se filtra —en vez de recorrer las filas— para que el test no dependa de la
 * paginación: la BD es compartida y el listado crece con los expedientes de otros runs.
 */
async function filtrarPorNumeroExpediente(page: Page, numero: string): Promise<void> {
  const filtro = page
    .getByTestId('search-row')
    .getByTestId('column:numeroExpediente')
    .locator('input');
  await filtro.fill(numero);
  await filtro.press('Enter');
  await expect(filasDeExpedientes(page)).toHaveCount(1);
}

/**
 * Abre el expediente `numero` desde «Tramitación» → «Pendientes de mí»: al pulsar la fila,
 * `BandejaController.abrirExpediente` lo abre con el perfil que declara su estado
 * (TRAMITADOR), que es con el que el administrativo lo tramita: el que pinta el
 * formulario editable del estado y sus botones. Arrancar con un `goto` lo hace robusto
 * aunque se venga de un diálogo abierto o de media navegación.
 */
async function abrirExpedienteComoTramitador(page: Page, numero: string): Promise<void> {
  await abrirExpedienteDesdeBandeja(page, numero, 'tramitacion-menuitem', 'tramitacion-pendientesDeMi-menuitem');
}

/** Abre el expediente `numero` desde la bandeja `entrada` del menú `grupo`. */
async function abrirExpedienteDesdeBandeja(
  page: Page,
  numero: string,
  grupo: string,
  entrada: string,
): Promise<void> {
  await page.goto('/#/');
  await abrirEntradaDeMenu(page, grupo, entrada);
  await filtrarPorNumeroExpediente(page, numero);
  await filasDeExpedientes(page).first().click();
  await expect(page.getByRole('tab', { name: TITULO_EXPEDIENTE })).toBeVisible();
}

/**
 * Borra el expediente `numero`. Los dos estados por los que pasa el test ofrecen «Borrar el
 * expediente» al perfil que declaran, pero cada uno está en una bandeja distinta:
 * PENDIENTE_DOCUMENTO_ESCANEADO (TRAMITADOR) en «Tramitación» → «Pendientes de mí» y
 * ENTRADA_DATOS (CREADOR, que es el administrativo que lo registró) en «Mis trámites» →
 * «Pendientes de mí». `enEntradaDatos` dice en cuál de los dos quedó. El botón abre un
 * diálogo de confirmación de Axelor que hay que aceptar.
 */
async function borrarExpediente(page: Page, numero: string, enEntradaDatos: boolean): Promise<void> {
  if (enEntradaDatos) {
    await abrirExpedienteDesdeBandeja(page, numero, 'misTramites-menuitem', 'misTramites-pendientesDeMi-menuitem');
  } else {
    await abrirExpedienteComoTramitador(page, numero);
  }

  await page.getByTestId('widget:DELETE').getByRole('button').click();
  await page.getByRole('button', { name: 'Aceptar' }).click();
  await expect(page.getByRole('tab', { name: TITULO_EXPEDIENTE })).toHaveCount(0);
}

test.describe('Ventanilla — Nuevo expediente', () => {
  test('Administrativo que solo puede registrar en papel: no se le pregunta cómo se presenta', async ({
    page,
  }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el administrativo `administrativo1@mislata.es` ha iniciado
    // sesión con la contraseña `demo1234`.
    await login(page, USUARIO, CONTRASENA);

    // Número del expediente creado; se captura tras crearlo y lo usa el teardown para
    // borrarlo. Vacío mientras no exista el expediente.
    let numeroExpediente = '';
    // Si el expediente ya avanzó a ENTRADA_DATOS, que cambia la bandeja desde la que el
    // teardown lo reabre para borrarlo.
    let enEntradaDatos = false;

    try {
      // Paso 2: Cuando abre el menú "Mis trámites" y pulsa "Nuevo trámite".
      await abrirEntradaDeMenu(page, 'misTramites-menuitem', 'misTramites-nuevoTramite-menuitem');

      // Paso 3: Entonces se abre DIRECTAMENTE "Nuevo expediente: elija el trámite"…
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE, exact: true })).toBeVisible();
      // …"directamente" = sin pasar por la elección de centro: el administrativo solo
      // tiene un centro, así que ni se abre esa pantalla ni aparece su listado.
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO, exact: true })).toHaveCount(0);
      await expect(page.getByTestId('panel:centrosPanel')).toHaveCount(0);

      // …con el centro "CIPFP Mislata".
      const campoCentro = page.getByTestId('field:centro').getByRole('textbox');
      await expect(campoCentro).toHaveValue(CENTRO);
      await expect(campoCentro).toBeDisabled();

      // …y únicamente "Trámites para el alumno" con "Anulación de matrícula en ciclo
      // formativo" dentro: son los únicos que este administrativo puede iniciar.
      await desplegarGruposDeTramites(page);
      await expect(filasDeTramites(page)).toHaveCount(1);
      await expect(filasDeTramites(page).first()).toHaveText(TRAMITE);
      await expect(gruposDeTipoTramite(page)).toHaveCount(1);
      await expect(gruposDeTipoTramite(page).first()).toContainText(TIPO_TRAMITE_ALUMNO);
      await expect(page.getByText(TIPO_TRAMITE_PROFESOR)).toHaveCount(0);

      // …y un único botón debajo, "Cancelar".
      const botonera = page.getByTestId('panel:buttons-panel');
      await expect(botonera.getByRole('button')).toHaveCount(1);
      await expect(botonera.getByRole('button', { name: 'Cancelar' })).toBeVisible();

      // Paso 4: Cuando pulsa la fila "Anulación de matrícula en ciclo formativo".
      await filasDeTramites(page).first().click();

      // Paso 5: Entonces se abre "Nuevo expediente" con ese trámite, su ayuda y el
      // centro en solo lectura.
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO, exact: true })).toBeVisible();
      const campoTramite = page.getByTestId('field:nombreTramite').getByRole('textbox');
      await expect(campoTramite).toHaveValue(TRAMITE);
      await expect(campoTramite).toBeDisabled();
      const centroContexto = page.getByTestId('field:centro').getByRole('textbox');
      await expect(centroContexto).toHaveValue(CENTRO);
      await expect(centroContexto).toBeDisabled();
      await expect(page.getByTestId('field:ayudaTramite')).toContainText(
        'Con este trámite puedes solicitar la anulación de tu matrícula en un ciclo formativo de este centro',
      );

      // …no se ve "¿Cómo se presenta?": el administrativo solo puede registrar en
      // papel, así que no hay nada que elegir. Se comprueba por su rótulo y también
      // por el campo del modelo que la pinta (`presentadoEnPapel`), para que el test
      // siga cazando el fallo aunque cambie el texto.
      await expect(page.getByText(PREGUNTA_COMO_SE_PRESENTA)).toHaveCount(0);
      await expect(page.getByTestId('field:presentadoEnPapel')).toHaveCount(0);

      // …sí se ve "¿Para quién es el expediente?" (el trámite admite representación)…
      await expect(page.getByText(PREGUNTA_PARA_QUIEN)).toBeVisible();
      await expect(page.getByTestId('field:presentadoEnRepresentacion')).toBeVisible();

      // …con las opciones "Para mí" y "Para otra persona a la que represento (hijo/a
      // menor de edad o persona tutelada)", sin ninguna marcada.
      const radioParaMi = opcionParaQuien(page, OPCION_PARA_MI);
      const radioEnRepresentacion = opcionParaQuien(page, OPCION_EN_REPRESENTACION);
      await expect(radioParaMi).toBeVisible();
      await expect(radioEnRepresentacion).toBeVisible();
      await expect(radioParaMi).not.toBeChecked();
      await expect(radioEnRepresentacion).not.toBeChecked();

      // …y se ven los botones "Atrás" y "Crear expediente".
      await expect(page.getByRole('button', { name: 'Atrás' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Crear expediente' })).toBeVisible();

      // Paso 6: Cuando marca "Para mí" y pulsa "Crear expediente".
      await radioParaMi.click();
      // El expediente se crea con lo que estaba marcado en ese instante: sin esta
      // comprobación, un radio que no llegara a marcarse dejaría pasar el test.
      await expect(radioParaMi).toBeChecked();
      await expect(radioEnRepresentacion).not.toBeChecked();
      await page.getByRole('button', { name: 'Crear expediente' }).click();

      // Resultado esperado: se abre el expediente recién creado de "Anulación de
      // matrícula en ciclo formativo" en su primer estado.
      const pestanaExpediente = page.getByRole('tab', { name: TITULO_EXPEDIENTE });
      await expect(pestanaExpediente).toBeVisible();
      const tituloExpediente = (await pestanaExpediente.getByTestId('title').innerText()).trim();
      // Número completo del expediente (`<nº>/<año>-<código del centro>`): el título sin
      // el sufijo `-<trámite> V1`.
      numeroExpediente = tituloExpediente.slice(0, -SUFIJO_TITULO_EXPEDIENTE.length);
      await expect(page.getByTestId('field:namePhase').getByRole('textbox')).toHaveValue(
        FASE_INICIAL,
      );
      // Ojo: el rótulo "Estado" es subcadena de "Fecha último estado", así que
      // localizarlo por nombre accesible resolvería a dos inputs. Se acota al campo.
      await expect(page.getByTestId('field:nameState').getByRole('textbox')).toHaveValue(
        ESTADO_INICIAL_EN_PAPEL,
      );

      // Resultado esperado: el asistente se cierra — no queda visible ninguna de sus
      // TRES pantallas, ni por su pestaña ni por sus paneles y botones.
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO, exact: true })).toHaveCount(0);
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE, exact: true })).toHaveCount(0);
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO, exact: true })).toHaveCount(0);
      await expect(page.getByTestId('panel:centrosPanel')).toHaveCount(0);
      await expect(page.getByTestId('panel:tramitesPanel')).toHaveCount(0);
      await expect(page.getByTestId('panel:tramitePanel')).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Crear expediente' })).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Cancelar' })).toHaveCount(0);

      // Resultado esperado: el expediente queda registrado como PRESENTADO EN PAPEL.
      // `InitialEventManagerImpl` solo lleva a PENDIENTE_DOCUMENTO_ESCANEADO cuando
      // `presentadoEnPapel` es true (si no, arranca en ENTRADA_DATOS), así que el
      // estado de arriba ya lo prueba; aquí se confirma con lo que ve el usuario: el
      // panel para adjuntar la solicitud en papel y su aviso, y la AUSENCIA del aviso
      // de la vía telemática (firmar con certificado desde este ordenador).
      await expect(page.getByTestId('panel:solicitud-escaneada-upload')).toBeVisible();
      await expect(page.getByText(AVISO_EN_PAPEL)).toBeVisible();
      await expect(page.getByText(AVISO_PRESENTA_EL_MISMO)).toHaveCount(0);

      // Resultado esperado: …POR EL ADMINISTRATIVO.
      await expect(page.getByTestId('field:createdBy').getByRole('textbox')).toHaveValue(
        ADMINISTRATIVO_NOMBRE_COMPLETO,
      );

      // Resultado esperado: …y PARA ÉL MISMO, no en representación de otra persona.
      // Lo que el usuario eligió ya está comprobado arriba (marcó "Para mí" y era la
      // única opción marcada al pulsar "Crear expediente"), pero eso es la ENTRADA de la
      // acción, no lo persistido. Aquí solo se deja constancia de que en esta pantalla
      // tampoco asoma el panel del solicitante; ojo: por sí sola esta línea NO prueba
      // nada, porque ninguno de los dos forms de PENDIENTE_DOCUMENTO_ESCANEADO incluye
      // `persona-solicitante` en su `<include-panels>` (que es una ALLOWLIST), así que
      // faltaría igual con `presentadoEnRepresentacion = true`. La prueba de verdad se
      // hace abajo, sobre el expediente ya creado.
      await expect(page.getByText(PANEL_PERSONA_SOLICITANTE)).toHaveCount(0);

      // Resultado esperado: …en el centro "CIPFP Mislata".
      // El formulario de este estado (perfil TRAMITADOR) no incluye el panel de la
      // matrícula, así que no pinta el centro. Se comprueba en la columna "Centro" de
      // «Tramitación» → «Pendientes de mí», que lista el expediente con su centro.
      await abrirEntradaDeMenu(page, 'tramitacion-menuitem', 'tramitacion-pendientesDeMi-menuitem');
      await filtrarPorNumeroExpediente(page, numeroExpediente);
      await expect(filasDeExpedientes(page).first()).toContainText(CENTRO);

      // Resultado esperado: …PARA ÉL MISMO (no en representación), comprobado sobre el
      // expediente YA CREADO y no sobre lo que se tecleó en el asistente.
      //
      // El único sitio de la UI donde el expediente lo delata es el panel "Persona que
      // presenta la solicitud" (`showIf="presentadoEnRepresentacion"`), y ese panel no
      // está en la allowlist de NINGÚN form de PENDIENTE_DOCUMENTO_ESCANEADO: el del
      // TRAMITADOR incluye `subsanacion` + `solicitud-escaneada-upload` y el genérico
      // `datos-alumno` + `matricula`. El primer estado que sí lo incluye es el siguiente
      // de la misma fase, ENTRADA_DATOS, cuyo form de TRAMITADOR lista
      // `persona-solicitante-editable`. Por eso el test avanza un estado: adjunta la
      // solicitud escaneada —lo único que `StateEventValidatorImpl` exige para CONTINUAR
      // desde PENDIENTE_DOCUMENTO_ESCANEADO— y mira allí.
      await abrirExpedienteComoTramitador(page, numeroExpediente);
      await expect(page.getByTestId('field:nameState').getByRole('textbox')).toHaveValue(
        ESTADO_INICIAL_EN_PAPEL,
      );
      // El widget de Axelor esconde su `input[type=file]` detrás del botón de subir; se le
      // pasan los bytes directamente para no depender de un diálogo nativo de ficheros.
      const panelSolicitudEscaneada = page.getByTestId('panel:solicitud-escaneada-upload');
      await panelSolicitudEscaneada
        .locator('input[type="file"]')
        .setInputFiles({ name: NOMBRE_PDF, mimeType: 'application/pdf', buffer: pdfMinimo() });
      await expect(panelSolicitudEscaneada.getByRole('button', { name: NOMBRE_PDF })).toBeVisible();
      await page.getByRole('button', { name: 'Siguiente' }).click();
      await expect(page.getByTestId('field:nameState').getByRole('textbox')).toHaveValue(
        ESTADO_ENTRADA_DATOS,
      );
      enEntradaDatos = true;

      // Control positivo, para que la ausencia de abajo NO pueda volver a ser vacua: los
      // otros paneles de la MISMA lista `<include-panels>` que `persona-solicitante-editable`
      // (`datos-alumno`, con su panel interno `alumno`, y `matricula`) sí están pintados.
      // Es decir, se está mirando el formulario que SÍ pintaría al solicitante.
      await expect(page.getByTestId('panel:alumno')).toBeVisible();
      await expect(page.getByTestId('panel:matricula')).toBeVisible();

      // La aserción de verdad: en ese formulario no hay ni rastro del panel del
      // solicitante, ni por su `name` ni por su rótulo. Solo puede faltar porque su
      // `showIf="presentadoEnRepresentacion"` es falso, o sea porque el expediente quedó
      // PERSISTIDO con `presentadoEnRepresentacion = false`: el interesado es el propio
      // administrativo que lo registró y no hay nadie a quien represente.
      await expect(page.getByTestId('panel:persona-solicitante-editable')).toHaveCount(0);
      await expect(page.getByText(PANEL_PERSONA_SOLICITANTE)).toHaveCount(0);
    } finally {
      // Teardown: borrar el expediente creado aunque una aserción haya fallado — la
      // BD es compartida y no se resetea, así que dejarlo lo acumularía run tras run.
      // Si el test falló ANTES de crearlo no hay nada que borrar.
      if (numeroExpediente !== '') {
        await borrarExpediente(page, numeroExpediente, enEntradaDatos);
      }

      await logout(page);
    }
  });
});
