import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-001 — Alumno de un solo centro crea un expediente sin que se le pregunte nada
// origen: ESC-001  |  verifica: U-nuevo-expediente-001, U-nuevo-expediente-002, U-nuevo-expediente-003, U-nuevo-expediente-004, U-nuevo-expediente-005, U-nuevo-expediente-006, U-nuevo-expediente-008, U-nuevo-expediente-009, U-nuevo-expediente-011, U-nuevo-expediente-013, R-AsistenteNuevoExpediente-001, R-AsistenteNuevoExpediente-003, R-AsistenteNuevoExpediente-004
// fuente: .sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/test-e2e-desc/t-001-alumno-de-un-solo-centro-crea-un-expediente-sin-que-se-le-pregunte-nada.desc.md

/**
 * IDEMPOTENCIA (§4 del contrato de generación) — este test CREA un expediente, y su
 * identificador (`00016/2026`) lo asigna el servidor con un contador, así que NO se
 * puede aislar con un sufijo `Date.now()` en un nombre: no hay ningún nombre que el
 * test elija. La idempotencia se consigue de las otras dos formas:
 *   - el expediente creado se identifica por el título de SU pestaña, capturado en
 *     tiempo de ejecución justo después de crearlo (nunca por un número fijo), y
 *   - se BORRA en el `finally` con el botón "Borrar el expediente" que el propio
 *     estado inicial ofrece, de modo que la BD compartida queda como estaba.
 * No hace falta pre-limpieza defensiva: ninguna regla de negocio limita cuántos
 * expedientes de este trámite puede tener el alumno, así que un expediente residual
 * de un run que abortara no impide crear otro ni cambia ninguna aserción (el test no
 * cuenta expedientes, solo mira el que acaba de abrir).
 */

// Credenciales del usuario de la precondición (tabla «Usuarios de acceso» del .desc.md).
const USUARIO = 'alumno1@mislata.es';
const CONTRASENA = 'demo1234';

// Datos del estado inicial de la BD que el test da por sentados.
const CENTRO = 'CIPFP Mislata';
const TRAMITE = 'Anulación de matrícula en ciclo formativo';
const TIPO_TRAMITE_ALUMNO = 'Trámites para el alumno';
const TIPO_TRAMITE_PROFESOR = 'Trámites para el profesor';
// Identidad del alumno tal y como la carga `data-demo/input/usuarios-demo.xml`.
const ALUMNO_NOMBRE = 'Alumno1';
const ALUMNO_APELLIDOS = 'CIPFP Mislata';
const ALUMNO_DNI = '86862719E';
const ALUMNO_NOMBRE_COMPLETO = 'Alumno1 CIPFP Mislata';

// Títulos de las TRES pantallas del asistente (los fijan los `action-view` de
// `system/ventanilla/views/`). Se usan para comprobar tanto que se abre la que toca
// como que al final NO queda ninguna abierta.
const PANTALLA_CENTRO = 'Nuevo expediente: elija el centro';
const PANTALLA_TRAMITE = 'Nuevo expediente: elija el trámite';
const PANTALLA_CONTEXTO = 'Nuevo expediente';

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
 * que hay que aceptar. Al borrarlo el asistente vuelve a la elección de trámite.
 */
async function borrarExpediente(page: Page, titulo: string): Promise<void> {
  await page.getByTestId('widget:DELETE').getByRole('button').click();
  await page.getByRole('button', { name: 'Aceptar' }).click();
  await expect(page.getByRole('tab', { name: titulo, exact: true })).toHaveCount(0);
}

test.describe('Ventanilla — Nuevo expediente', () => {
  test('Alumno de un solo centro crea un expediente sin que se le pregunte nada', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el alumno `alumno1@mislata.es` ha iniciado sesión con la
    // contraseña `demo1234`.
    await login(page, USUARIO, CONTRASENA);

    // Título de la pestaña del expediente creado; se captura tras crearlo y lo usa el
    // teardown para borrarlo. Vacío mientras no exista el expediente.
    let tituloExpediente = '';

    try {
      // Paso 2: Cuando abre el menú "Ventanilla" y pulsa "Nuevo expediente".
      await abrirNuevoExpediente(page);

      // Paso 3: Entonces el sistema no muestra el listado de centros y abre "Nuevo
      // expediente: elija el trámite"…
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE, exact: true })).toBeVisible();
      // …no muestra el listado de centros: ni se abre esa pantalla del asistente ni
      // aparece su panel con el listado.
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO, exact: true })).toHaveCount(0);
      await expect(page.getByTestId('panel:centrosPanel')).toHaveCount(0);

      // …con el centro "CIPFP Mislata" encima del listado.
      const campoCentro = page.getByTestId('field:centro').getByRole('textbox');
      await expect(campoCentro).toHaveValue(CENTRO);
      await expect(campoCentro).toBeDisabled();
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
      await expect(filasDeTramites(page)).toHaveCount(1);
      await expect(filasDeTramites(page).first()).toHaveText(TRAMITE);
      await expect(gruposDeTipoTramite(page)).toHaveCount(1);
      await expect(gruposDeTipoTramite(page).first()).toContainText(TIPO_TRAMITE_ALUMNO);
      // …no aparece "Trámites para el profesor".
      await expect(page.getByText(TIPO_TRAMITE_PROFESOR)).toHaveCount(0);

      // …debajo hay un único botón, "Cancelar", y no hay botón "Atrás".
      const botonera = page.getByTestId('panel:buttons-panel');
      await expect(botonera.getByRole('button')).toHaveCount(1);
      await expect(botonera.getByRole('button', { name: 'Cancelar' })).toBeVisible();
      await expect(botonera.getByRole('button', { name: 'Atrás' })).toHaveCount(0);

      // Paso 4: Cuando pulsa la fila "Anulación de matrícula en ciclo formativo".
      await filasDeTramites(page).first().click();

      // Paso 5: Entonces se abre "Nuevo expediente" con el nombre del trámite, su
      // texto de ayuda y el centro "CIPFP Mislata" en solo lectura.
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

      // …no se ve "¿Cómo se presenta?" ni "¿Para quién es el expediente?". Se
      // comprueban por su etiqueta y también por el campo del modelo que las pinta
      // (`presentadoEnPapel` / `presentadoEnRepresentacion`), para que el test siga
      // cazando el fallo aunque cambie el rótulo.
      await expect(page.getByText('¿Cómo se presenta?')).toHaveCount(0);
      await expect(page.getByText('¿Para quién es el expediente?')).toHaveCount(0);
      await expect(page.getByTestId('field:presentadoEnPapel')).toHaveCount(0);
      await expect(page.getByTestId('field:presentadoEnRepresentacion')).toHaveCount(0);

      // …se ven los botones "Atrás" y "Crear expediente".
      await expect(page.getByRole('button', { name: 'Atrás' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Crear expediente' })).toBeVisible();

      // Paso 6: Cuando pulsa "Crear expediente".
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
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO, exact: true })).toHaveCount(0);
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE, exact: true })).toHaveCount(0);
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO, exact: true })).toHaveCount(0);
      await expect(page.getByTestId('panel:centrosPanel')).toHaveCount(0);
      await expect(page.getByTestId('panel:tramitesPanel')).toHaveCount(0);
      await expect(page.getByTestId('panel:tramitePanel')).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Crear expediente' })).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Cancelar' })).toHaveCount(0);

      // Resultado esperado: el expediente está en el centro "CIPFP Mislata".
      await expect(page.getByTestId('field:nombreCentro').getByRole('textbox')).toHaveValue(
        CENTRO,
      );

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

      await logout(page);
    }
  });
});
