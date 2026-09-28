import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-005 — Alumno de dos centros elige el centro
// origen: ESC-005  |  verifica: U-nuevo-expediente-001, U-nuevo-expediente-002, U-nuevo-expediente-003, U-nuevo-expediente-004, U-nuevo-expediente-005, R-AsistenteNuevoExpediente-001, R-AsistenteNuevoExpediente-003, R-AsistenteNuevoExpediente-004
// fuente: .sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/test-e2e-desc/t-005-alumno-de-dos-centros-elige-el-centro.desc.md

/**
 * IDEMPOTENCIA (§4 del contrato de generación) — este test CREA un expediente, y su
 * identificador (`00004/2026`) lo asigna el servidor con un contador, así que NO se
 * puede aislar con un sufijo `Date.now()` en un nombre: no hay ningún nombre que el
 * test elija. La idempotencia se consigue de las otras dos formas:
 *   - el expediente creado se identifica por el título de SU pestaña, capturado en
 *     tiempo de ejecución justo después de crearlo (nunca por un número fijo), y
 *   - se BORRA en el `finally` con el botón "Borrar el expediente" que el propio
 *     estado inicial ofrece, de modo que la BD compartida queda como estaba.
 * No hace falta pre-limpieza defensiva: ninguna regla de negocio limita cuántos
 * expedientes de este trámite puede tener el alumno en un centro, así que un
 * expediente residual de un run que abortara no impide crear otro ni cambia ninguna
 * aserción (el test no cuenta expedientes, solo mira el que acaba de abrir). Los
 * listados que sí comprueba —centros y trámites— dependen solo de los datos maestros
 * del estado inicial, que ningún test modifica.
 */

// Credenciales del usuario de la precondición (tabla «Usuarios de acceso» del .desc.md):
// alumno en DOS centros, CIPFP Mislata y CIPFP Batoi. Es el caso clave del escenario:
// al tener más de un centro, el asistente le pregunta en cuál crea el expediente.
const USUARIO = 'alumnodoscentros@mislata.es';
const CONTRASENA = 'demo1234';

// Datos del estado inicial de la BD que el test da por sentados.
// Los DOS centros del alumno, EN ORDEN ALFABÉTICO: el orden del array es parte de la
// aserción (`toHaveText` compara posición a posición).
const CENTROS_DEL_ALUMNO = ['CIPFP Batoi', 'CIPFP Mislata'];
// El centro que el alumno ELIGE, y el otro (el que NO debe acabar en el expediente).
const CENTRO_ELEGIDO = 'CIPFP Batoi';
const CENTRO_NO_ELEGIDO = 'CIPFP Mislata';
const TRAMITE = 'Anulación de matrícula en ciclo formativo';
const TIPO_TRAMITE_ALUMNO = 'Trámites para el alumno';
const TIPO_TRAMITE_PROFESOR = 'Trámites para el profesor';
// Identidad del alumno tal y como la carga `data-demo/input/usuarios-demo.xml`.
const ALUMNO_NOMBRE = 'AlumnoDosCentros';
const ALUMNO_APELLIDOS = 'CIPFP Mislata y Batoi';
const ALUMNO_DNI = '71359246K';
const ALUMNO_NOMBRE_COMPLETO = 'AlumnoDosCentros CIPFP Mislata y Batoi';

// Títulos de las TRES pantallas del asistente (los fijan los `action-view` de
// `system/ventanilla/views/`). Se usan para comprobar tanto que se abre la que toca
// como que al final NO queda ninguna abierta. Se comparan con regex anclada porque
// Axelor añade un `*` al título de la pestaña cuando el formulario está sucio, y
// porque "Nuevo expediente" a secas es prefijo de las otras dos (sin anclar, cazaría
// pestañas que no son la suya).
const PANTALLA_CENTRO = /^Nuevo expediente: elija el centro\*?$/;
const PANTALLA_TRAMITE = /^Nuevo expediente: elija el trámite\*?$/;
const PANTALLA_CONTEXTO = /^Nuevo expediente\*?$/;

// Primer estado del tipo de expediente para quien lo presenta él mismo (perfil
// CREADOR), según `TipoExpedienteInstance.xml`: fase SOLICITUD, estado DATOS_SOLICITUD.
const FASE_INICIAL = 'Solicitud de anulación';
const ESTADO_INICIAL = 'Datos de la solicitud';

// Aviso que la vista del estado pinta con `showIf="!presentadoEnPapel"`: verlo es la
// prueba en la UI de que el expediente NO quedó marcado como presentado en papel.
const AVISO_PRESENTA_EL_MISMO =
  'Para presentar la solicitud necesitará firmarla con su certificado digital desde este mismo ordenador';

// Panel que la vista pinta con `showIf="presentadoEnPapel"`: su ausencia es la otra
// cara de la misma comprobación.
const PANEL_SOLICITUD_EN_PAPEL = 'panel:solicitudEscaneadaDatosSolicitud';

// Panel que la plantilla común pinta con `showIf="presentadoEnRepresentacion"`: su
// ausencia prueba que el expediente es para el propio alumno, no en representación.
const PANEL_PERSONA_SOLICITANTE = 'panel:persona-solicitante-editable';

// Título de la pestaña del expediente creado: <nº>/<año>-<trámite> V1.
const TITULO_EXPEDIENTE = new RegExp(`^\\d+/\\d{4}-${TRAMITE} V1$`);

/**
 * Filas de datos del grid de centros (excluye la cabecera). Ya no es un
 * `panel-related field="centrosDisponibles"` (testid `field:centrosDisponibles`), sino
 * un grid independiente embebido en un `panel-dashlet` (testid `panel:centrosPanel`),
 * con los testids estándar de un grid embebido: sus filas siguen llevando `row:`.
 */
function filasDeCentros(page: Page) {
  return page.getByTestId('panel:centrosPanel').locator('[data-testid^="row:"]');
}

/**
 * Filas de datos del árbol de trámites (excluye cabecera y filas de agrupación). El
 * `panel-related` agrupado con testids `field:tramitesDisponibles`/`row:`/`group-row:`
 * fue sustituido por un `<tree>` embebido en un `panel-dashlet` (commit `98755ea`): sus
 * nodos no llevan `data-testid` propio, así que se localizan por rol ARIA de un
 * `treegrid` — `aria-level="2"` son las filas hoja (los trámites), `aria-level="1"` las
 * de agrupación (el tipo de trámite).
 */
function filasDeTramites(page: Page) {
  return page.getByTestId('panel:tramitesPanel').locator('[role="row"][aria-level="2"]');
}

/** Filas de agrupación por tipo de trámite del árbol de trámites. */
function gruposDeTipoTramite(page: Page) {
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
 * Abre el asistente desde el menú «Mis trámites» → «Nuevo trámite». El grupo se
 * pliega y despliega al pulsarlo, así que solo se despliega si la entrada no se ve:
 * pulsarlo a ciegas lo cerraría cuando ya venía abierto.
 */
async function abrirNuevoExpediente(page: Page): Promise<void> {
  const entrada = page.getByTestId('item:misTramites-nuevoTramite-menuitem');
  if (!(await entrada.isVisible())) {
    await page.getByTestId('item:misTramites-menuitem').getByTestId('title').first().click();
  }
  await entrada.click();
}

/**
 * Borra el expediente cuya pestaña se titula `titulo`. El botón abre un diálogo de
 * confirmación de Axelor ("Se va a eliminar el expediente y no podrá recuperarlo")
 * que hay que aceptar. Al borrarlo el asistente vuelve a la elección de centro.
 */
async function borrarExpediente(page: Page, titulo: string): Promise<void> {
  await page.getByTestId('widget:DELETE').getByRole('button').click();
  await page.getByRole('button', { name: 'Aceptar' }).click();
  await expect(page.getByRole('tab', { name: titulo, exact: true })).toHaveCount(0);
}

test.describe('Ventanilla — Nuevo expediente', () => {
  test('Alumno de dos centros elige el centro', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el alumno `alumnodoscentros@mislata.es` ha iniciado sesión con
    // la contraseña `demo1234`.
    await login(page, USUARIO, CONTRASENA);

    // Título de la pestaña del expediente creado; se captura tras crearlo y lo usa el
    // teardown para borrarlo. Vacío mientras no exista el expediente.
    let tituloExpediente = '';

    try {
      // Paso 2: Cuando abre el menú "Ventanilla" y pulsa "Nuevo expediente".
      await abrirNuevoExpediente(page);

      // Paso 3: Entonces se abre "Nuevo expediente: elija el centro"…
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO })).toBeVisible();
      await expect(page.getByTestId('panel:centrosPanel')).toBeVisible();

      // …con dos filas POR ORDEN ALFABÉTICO, "CIPFP Batoi" y "CIPFP Mislata".
      // `toHaveText` con un array compara posición a posición, así que verifica a la
      // vez el contenido, el número de filas (dos, ni una más) y el orden.
      await expect(filasDeCentros(page)).toHaveText(CENTROS_DEL_ALUMNO);

      // …y un botón "Cancelar" (uno solo: en esta pantalla no hay "Atrás", porque es
      // la primera del asistente).
      const botoneraCentros = page.getByTestId('panel:buttons-panel');
      await expect(botoneraCentros.getByRole('button')).toHaveCount(1);
      await expect(botoneraCentros.getByRole('button', { name: 'Cancelar' })).toBeVisible();

      // Paso 4: Cuando pulsa la fila "CIPFP Batoi".
      await filasDeCentros(page).filter({ hasText: CENTRO_ELEGIDO }).click();

      // Paso 5: Entonces se abre "Nuevo expediente: elija el trámite"…
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE })).toBeVisible();
      // …y la pantalla de elección de centro se cierra al pasar a esta.
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO })).toHaveCount(0);

      // …con el centro "CIPFP Batoi" encima del listado: el centro que el alumno acaba
      // de elegir, no el otro.
      const campoCentro = page.getByTestId('field:centro').getByRole('textbox');
      await expect(campoCentro).toHaveValue(CENTRO_ELEGIDO);
      await expect(campoCentro).not.toHaveValue(CENTRO_NO_ELEGIDO);
      // "Encima" es posición real en pantalla, no solo presencia: el panel del centro
      // tiene que quedar por encima del grid de trámites.
      const cajaCentro = await page.getByTestId('panel:centroPanel').boundingBox();
      const cajaTramites = await page.getByTestId('panel:tramitesPanel').boundingBox();
      expect(cajaCentro).not.toBeNull();
      expect(cajaTramites).not.toBeNull();
      expect(cajaCentro!.y).toBeLessThan(cajaTramites!.y);

      // …el listado muestra ÚNICAMENTE el tipo de trámite "Trámites para el alumno"
      // y, dentro, únicamente "Anulación de matrícula en ciclo formativo".
      await desplegarGruposDeTramites(page);
      await expect(gruposDeTipoTramite(page)).toHaveCount(1);
      await expect(gruposDeTipoTramite(page).first()).toContainText(TIPO_TRAMITE_ALUMNO);
      await expect(filasDeTramites(page)).toHaveText([TRAMITE]);
      // …no aparece "Trámites para el profesor" (ni como grupo del listado ni en
      // ningún otro sitio de la pantalla).
      await expect(page.getByText(TIPO_TRAMITE_PROFESOR)).toHaveCount(0);

      // …debajo hay un único botón, "Atrás", y no hay botón "Cancelar": al haber
      // habido elección de centro, el asistente ofrece volver a ella en vez de
      // cancelar.
      const botoneraTramites = page.getByTestId('panel:buttons-panel');
      await expect(botoneraTramites.getByRole('button')).toHaveCount(1);
      await expect(botoneraTramites.getByRole('button', { name: 'Atrás' })).toBeVisible();
      await expect(botoneraTramites.getByRole('button', { name: 'Cancelar' })).toHaveCount(0);
      // La botonera está debajo del listado, no encima.
      const cajaBotonera = await botoneraTramites.boundingBox();
      expect(cajaBotonera).not.toBeNull();
      expect(cajaBotonera!.y).toBeGreaterThan(cajaTramites!.y);

      // Paso 6: Cuando pulsa la fila "Anulación de matrícula en ciclo formativo".
      await filasDeTramites(page).first().click();

      // Paso 7: Entonces se abre "Nuevo expediente" con el trámite "Anulación de
      // matrícula en ciclo formativo", su ayuda y el centro "CIPFP Batoi" en solo
      // lectura.
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO })).toBeVisible();
      const campoTramite = page.getByTestId('field:nombreTramite').getByRole('textbox');
      await expect(campoTramite).toHaveValue(TRAMITE);
      await expect(campoTramite).toBeDisabled();
      const centroContexto = page.getByTestId('field:centro').getByRole('textbox');
      await expect(centroContexto).toHaveValue(CENTRO_ELEGIDO);
      await expect(centroContexto).not.toHaveValue(CENTRO_NO_ELEGIDO);
      await expect(centroContexto).toBeDisabled();
      await expect(page.getByTestId('field:ayudaTramite')).toContainText(
        'Con este trámite puedes solicitar la anulación de tu matrícula en un ciclo formativo de este centro',
      );

      // …sin las preguntas "¿Cómo se presenta?" ni "¿Para quién es el expediente?". Se
      // comprueban por su etiqueta y también por el campo del modelo que las pinta
      // (`presentadoEnPapel` / `presentadoEnRepresentacion`), para que el test siga
      // cazando el fallo aunque cambie el rótulo.
      await expect(page.getByText('¿Cómo se presenta?')).toHaveCount(0);
      await expect(page.getByText('¿Para quién es el expediente?')).toHaveCount(0);
      await expect(page.getByTestId('field:presentadoEnPapel')).toHaveCount(0);
      await expect(page.getByTestId('field:presentadoEnRepresentacion')).toHaveCount(0);

      // …y con los botones "Atrás" y "Crear expediente".
      await expect(page.getByRole('button', { name: 'Atrás' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Crear expediente' })).toBeVisible();

      // Paso 8: Cuando pulsa "Crear expediente".
      await page.getByRole('button', { name: 'Crear expediente' }).click();

      // Resultado esperado: se abre el expediente recién creado de "Anulación de
      // matrícula en ciclo formativo" en su primer estado.
      const pestanaExpediente = page.getByRole('tab', { name: TITULO_EXPEDIENTE });
      await expect(pestanaExpediente).toBeVisible();
      tituloExpediente = (await pestanaExpediente.getByTestId('title').innerText()).trim();
      await expect(page.getByTestId('field:namePhase').getByRole('textbox')).toHaveValue(
        FASE_INICIAL,
      );
      // Ojo: el rótulo "Estado" es subcadena de "Fecha último estado", así que
      // localizarlo por nombre accesible resolvería a dos inputs. Se acota al campo.
      await expect(page.getByTestId('field:nameState').getByRole('textbox')).toHaveValue(
        ESTADO_INICIAL,
      );

      // Resultado esperado: el asistente se cierra — no queda visible ninguna de sus
      // TRES pantallas, ni por su pestaña ni por sus paneles y botones.
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO })).toHaveCount(0);
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE })).toHaveCount(0);
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO })).toHaveCount(0);
      await expect(page.getByTestId('panel:centrosPanel')).toHaveCount(0);
      await expect(page.getByTestId('panel:tramitesPanel')).toHaveCount(0);
      await expect(page.getByTestId('panel:tramitePanel')).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Crear expediente' })).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Cancelar' })).toHaveCount(0);

      // Resultado esperado: el expediente está en el centro "CIPFP Batoi" y NO en
      // "CIPFP Mislata" — el que el alumno eligió, no el otro al que también pertenece
      // (que es además el centro por el que se le conoce en la aplicación: sin esta
      // elección el expediente habría nacido en Mislata).
      const centroExpediente = page.getByTestId('field:nombreCentro').getByRole('textbox');
      await expect(centroExpediente).toHaveValue(CENTRO_ELEGIDO);
      await expect(centroExpediente).not.toHaveValue(CENTRO_NO_ELEGIDO);

      // Resultado esperado: presentado por el propio alumno, NO registrado como
      // presentado en papel. En la UI eso se ve en las tres bifurcaciones que la
      // vista del estado hace sobre `presentadoEnPapel`: el aviso de firma con
      // certificado, la ausencia del panel de la solicitud escaneada y la botonera
      // del que aún tiene que rellenar y firmar ("Siguiente", sin "Presentar la
      // solicitud" ni "Atrás").
      await expect(page.getByText(AVISO_PRESENTA_EL_MISMO)).toBeVisible();
      await expect(page.getByTestId(PANEL_SOLICITUD_EN_PAPEL)).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Siguiente' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Presentar la solicitud' })).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Atrás' })).toHaveCount(0);
      // Y lo crea el propio alumno, no el personal del centro.
      await expect(page.getByTestId('field:createdBy').getByRole('textbox')).toHaveValue(
        ALUMNO_NOMBRE_COMPLETO,
      );

      // Resultado esperado: y para él mismo, NO en representación de otra persona.
      // El panel "Persona que presenta la solicitud" solo se pinta cuando hay
      // representación, así que no debe existir; y la persona interesada es el propio
      // alumno que ha iniciado sesión.
      await expect(page.getByTestId(PANEL_PERSONA_SOLICITANTE)).toHaveCount(0);
      await expect(page.getByText('Persona que presenta la solicitud')).toHaveCount(0);
      const panelAlumno = page.getByTestId('panel:alumno');
      await expect(panelAlumno.getByRole('textbox', { name: 'Apellidos' })).toHaveValue(
        ALUMNO_APELLIDOS,
      );
      await expect(panelAlumno.getByRole('textbox', { name: 'Nombre' })).toHaveValue(ALUMNO_NOMBRE);
      await expect(panelAlumno.getByRole('textbox', { name: 'DNI/NIE' })).toHaveValue(ALUMNO_DNI);
    } finally {
      // Teardown: borrar el expediente creado aunque una aserción haya fallado — la
      // BD es compartida y no se resetea, así que dejarlo lo acumularía run tras run.
      // Si el test falló ANTES de crearlo no hay nada que borrar.
      if (tituloExpediente !== '') {
        await borrarExpediente(page, tituloExpediente);
      }

      // Al borrar el expediente el asistente vuelve a la elección de centro (y si el
      // fallo fue antes de crearlo, la pantalla del asistente sigue abierta): se
      // cierra con su propio botón "Cancelar" para no heredar la pestaña al siguiente
      // run. Si no hay ninguna pantalla abierta el botón no existe: el `.catch` es
      // intencional, no un error tapado.
      await page
        .getByTestId('panel:buttons-panel')
        .getByRole('button', { name: 'Cancelar' })
        .click({ timeout: 5000 })
        .catch(() => {});

      await logout(page);
    }
  });
});
