import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-018 — Profesor crea un expediente de un trámite que no admite representación sin
// que se le pregunte nada
// origen: ESC-017  |  verifica: U-nuevo-expediente-006, U-nuevo-expediente-008,
// U-nuevo-expediente-009, U-nuevo-expediente-011, U-nuevo-expediente-013,
// R-AsistenteNuevoExpediente-001
// fuente: .sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/test-e2e-desc/t-018-profesor-crea-un-expediente-de-un-tramite-que-no-admite-representacion-sin-que-se-le-pregunte-nada.desc.md

/**
 * IDEMPOTENCIA (§4 del contrato de generación) — este test CREA un expediente, y su
 * identificador (`0000N/2026`) lo asigna el servidor con un contador, así que NO se
 * puede aislar con un sufijo `Date.now()` en un nombre: no hay ningún nombre que el
 * test elija. La idempotencia se consigue de las otras dos formas:
 *   - el expediente creado se identifica por el título de SU pestaña, capturado en
 *     tiempo de ejecución justo después de crearlo (nunca por un número fijo), y
 *   - se BORRA en el `finally` con el botón "Borrar el expediente" que el propio
 *     estado inicial ofrece, de modo que la BD compartida queda como estaba.
 * No hace falta pre-limpieza defensiva: ninguna regla de negocio limita cuántos
 * expedientes de "Justificación de falta del profesorado" puede tener el director, así
 * que un expediente residual de un run que abortara no impide crear otro ni cambia
 * ninguna aserción (el test no cuenta expedientes, solo mira el que acaba de abrir).
 */

// Credenciales del usuario de la precondición (tabla «Usuarios de acceso» del .desc.md):
// un Profesor con cargo de Director en CIPFP Mislata, un solo centro.
const USUARIO = 'director@mislata.es';
const CONTRASENA = 'demo1234';

// Datos del estado inicial de la BD que el test da por sentados.
const CENTRO = 'CIPFP Mislata';
const TRAMITE = 'Justificación de falta del profesorado';
const TIPO_TRAMITE_PROFESOR = 'Trámites para el profesor';
const TIPO_TRAMITE_ALUMNO = 'Trámites para el alumno';
// Los dos trámites del tipo «Trámites para el profesor», EN ORDEN ALFABÉTICO: el orden
// del array es parte de la aserción (`toHaveText` compara posición a posición).
const TRAMITES_DEL_PROFESOR = ['Justificación de falta del profesorado', 'Trámite de prueba'];

// Identidad del director tal y como la carga `data-demo/input/usuarios-demo.xml`: es él
// mismo quien presenta la solicitud (personaInteresada), así que sus propios datos
// deben aparecer prerrellenados y en solo lectura en el panel "Datos del profesor
// interesado" (comprobado pilotando la app real: `panel:datos-profesor`).
const DIRECTOR_NOMBRE = 'Director';
const DIRECTOR_APELLIDOS = 'CIPFP Mislata';
const DIRECTOR_DNI = '85432016B';
const DIRECTOR_NOMBRE_COMPLETO = 'Director CIPFP Mislata';

// Títulos de las TRES pantallas del asistente (los fijan los `action-view` de
// `system/ventanilla/views/`). Se usan para comprobar tanto que se abre la que toca
// como que al final NO queda ninguna abierta.
const PANTALLA_CENTRO = 'Nuevo expediente: elija el centro';
const PANTALLA_TRAMITE = 'Nuevo expediente: elija el trámite';
const PANTALLA_CONTEXTO = 'Nuevo expediente';

// Primer estado del tipo de expediente para quien lo presenta él mismo (perfil
// CREADOR), según `TipoExpedienteInstance.xml`: fase RECEPCION, estado ENTRADA_DATOS.
// Comprobado pilotando la app real (los rótulos son los de la vista, no las constantes
// del enum): Fase "Recepción", Estado "Entrada de datos".
const FASE_INICIAL = 'Recepción';
const ESTADO_INICIAL = 'Entrada de datos';

// Panel que la plantilla común pinta con `showIf="presentadoEnRepresentacion"`
// (`persona-solicitante-editable` de `tramites/shared/template-views.xml`): su
// ausencia es la prueba en la UI de que el expediente NO es en representación de otra
// persona (este trámite, de hecho, ni siquiera lo permite: `permitidoPresentarEnRepresentacion=false`
// en su `TramiteInstance.xml`).
const PANEL_PERSONA_SOLICITANTE = 'panel:persona-solicitante-editable';

// Panel propio del trámite ("Datos del profesor interesado", `datos-profesor` de
// `tramites/profesores/.../v1/views.xml`) que identifica al interesado (el propio
// profesor). Nace PRERRELLENO y EN SOLO LECTURA con la identidad de quien ha iniciado
// sesión: es la prueba de que el expediente es "para él mismo".
const PANEL_PROFESOR = 'panel:datos-profesor';

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
 * Abre el asistente desde el menú «Ventanilla» → «Nuevo expediente». El grupo se
 * pliega y despliega al pulsarlo, así que solo se despliega si la entrada no se ve:
 * pulsarlo a ciegas lo cerraría cuando ya venía abierto.
 */
async function abrirNuevoExpediente(page: Page): Promise<void> {
  const entrada = page.getByTestId('item:ventanilla-nuevoExpediente-menuitem');
  if (!(await entrada.isVisible())) {
    await page.getByTestId('item:ventanilla-menuitem').getByTestId('title').first().click();
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
  test('Profesor crea un expediente de un trámite que no admite representación sin que se le pregunte nada', async ({
    page,
  }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el director `director@mislata.es` ha iniciado sesión con la
    // contraseña `demo1234`.
    await login(page, USUARIO, CONTRASENA);

    // Título de la pestaña del expediente creado; se captura tras crearlo y lo usa el
    // teardown para borrarlo. Vacío mientras no exista el expediente.
    let tituloExpediente = '';

    try {
      // Paso 2: Cuando abre el menú "Ventanilla" y pulsa "Nuevo expediente".
      await abrirNuevoExpediente(page);

      // Paso 3: Entonces se abre directamente "Nuevo expediente: elija el trámite"…
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE, exact: true })).toBeVisible();
      // …no se muestra el listado de centros: ni se abre esa pantalla del asistente
      // ni aparece su panel con el listado.
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO, exact: true })).toHaveCount(0);
      await expect(page.getByTestId('panel:centrosPanel')).toHaveCount(0);

      // …con el centro "CIPFP Mislata"…
      const campoCentro = page.getByTestId('field:centro').getByRole('textbox');
      await expect(campoCentro).toHaveValue(CENTRO);
      await expect(campoCentro).toBeDisabled();
      const cajaCentro = await page.getByTestId('panel:centroPanel').boundingBox();
      const cajaTramites = await page.getByTestId('panel:tramitesPanel').boundingBox();
      expect(cajaCentro).not.toBeNull();
      expect(cajaTramites).not.toBeNull();
      expect(cajaCentro!.y).toBeLessThan(cajaTramites!.y);

      // …únicamente "Trámites para el profesor" con "Justificación de falta del
      // profesorado" y "Trámite de prueba" por orden alfabético.
      await desplegarGruposDeTramites(page);
      await expect(gruposDeTipoTramite(page)).toHaveCount(1);
      await expect(gruposDeTipoTramite(page).first()).toContainText(TIPO_TRAMITE_PROFESOR);
      await expect(filasDeTramites(page)).toHaveText(TRAMITES_DEL_PROFESOR);
      // …no aparece "Trámites para el alumno".
      await expect(page.getByText(TIPO_TRAMITE_ALUMNO)).toHaveCount(0);

      // …y un único botón debajo, "Cancelar" (sin "Atrás").
      const botonera = page.getByTestId('panel:buttons-panel');
      await expect(botonera.getByRole('button')).toHaveCount(1);
      await expect(botonera.getByRole('button', { name: 'Cancelar' })).toBeVisible();
      await expect(botonera.getByRole('button', { name: 'Atrás' })).toHaveCount(0);

      // Paso 4: Cuando pulsa la fila "Justificación de falta del profesorado".
      await filasDeTramites(page).filter({ hasText: TRAMITE }).click();

      // Paso 5: Entonces se abre "Nuevo expediente" con ese trámite, su ayuda y el
      // centro en solo lectura…
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO, exact: true })).toBeVisible();
      const campoTramite = page.getByTestId('field:nombreTramite').getByRole('textbox');
      await expect(campoTramite).toHaveValue(TRAMITE);
      await expect(campoTramite).toBeDisabled();
      const centroContexto = page.getByTestId('field:centro').getByRole('textbox');
      await expect(centroContexto).toHaveValue(CENTRO);
      await expect(centroContexto).toBeDisabled();
      await expect(page.getByTestId('field:ayudaTramite')).toContainText(
        'Este trámite permite a justificar la falta del profesorado.',
      );

      // …sin "¿Cómo se presenta?" ni "¿Para quién es el expediente?" (no se le
      // pregunta nada: el director solo puede presentarlo él mismo — no tiene permiso
      // de registrarlo en papel, solo lo tiene el Jefe de estudios — y el trámite no
      // admite representación). Se comprueban por su etiqueta y también por el campo
      // del modelo que las pinta (`presentadoEnPapel` / `presentadoEnRepresentacion`),
      // para que el test siga cazando el fallo aunque cambie el rótulo.
      await expect(page.getByText('¿Cómo se presenta?')).toHaveCount(0);
      await expect(page.getByText('¿Para quién es el expediente?')).toHaveCount(0);
      await expect(page.getByTestId('field:presentadoEnPapel')).toHaveCount(0);
      await expect(page.getByTestId('field:presentadoEnRepresentacion')).toHaveCount(0);

      // …y con los botones "Atrás" y "Crear expediente".
      await expect(page.getByRole('button', { name: 'Atrás' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Crear expediente' })).toBeVisible();

      // Paso 6: Cuando pulsa "Crear expediente".
      await page.getByRole('button', { name: 'Crear expediente' }).click();

      // Resultado esperado: el asistente se cierra y se abre el expediente recién
      // creado de "Justificación de falta del profesorado" en su primer estado.
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

      // El asistente se cierra: no queda visible ninguna de sus TRES pantallas, ni
      // por su pestaña ni por sus paneles y botones.
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO, exact: true })).toHaveCount(0);
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE, exact: true })).toHaveCount(0);
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO, exact: true })).toHaveCount(0);
      await expect(page.getByTestId('panel:centrosPanel')).toHaveCount(0);
      await expect(page.getByTestId('panel:tramitesPanel')).toHaveCount(0);
      await expect(page.getByTestId('panel:tramitePanel')).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Crear expediente' })).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Cancelar' })).toHaveCount(0);

      // …en el centro "CIPFP Mislata". La vista de este trámite no repite un campo de
      // centro en la pantalla del expediente creado (comprobado pilotando la app real:
      // no hay ningún `field:`/`panel:` con "centro" en el DOM de este estado), así
      // que el centro queda probado donde SÍ es observable: en las dos pantallas del
      // asistente (Pasos 3 y 5, arriba) con el campo "Centro" en solo lectura fijado a
      // "CIPFP Mislata" — valor que no puede cambiar entre elegir el trámite y crear
      // el expediente porque el propio campo está deshabilitado.

      // Resultado esperado: el expediente queda presentado por el propio director (NO
      // registrado como presentado en papel). La prueba en la UI es que lo crea el
      // propio director (si lo hubiera registrado en papel el Jefe de estudios, sería
      // él quien figurase aquí, no el director) y que el estado inicial es el de
      // autogestión (perfil CREADOR, "Entrada de datos") con la botonera de quien aún
      // tiene que rellenar y firmar ("Siguiente", sin "Presentar la solicitud" ni
      // "Atrás").
      await expect(page.getByTestId('field:createdBy').getByRole('textbox')).toHaveValue(
        DIRECTOR_NOMBRE_COMPLETO,
      );
      await expect(page.getByRole('button', { name: 'Siguiente' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Presentar la solicitud' })).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Atrás' })).toHaveCount(0);

      // Resultado esperado: y para él mismo (NO en representación de otra persona). El
      // panel "Persona que presenta la solicitud" solo se pinta cuando hay
      // representación, así que no debe existir; y el panel propio del trámite "Datos
      // del profesor interesado" nace prerrelleno y en solo lectura con la identidad
      // del propio director (el sistema ya sabe quién es: no hay que preguntárselo).
      await expect(page.getByTestId(PANEL_PERSONA_SOLICITANTE)).toHaveCount(0);
      await expect(page.getByText('Persona que presenta la solicitud')).toHaveCount(0);
      const panelProfesor = page.getByTestId(PANEL_PROFESOR);
      await expect(panelProfesor).toBeVisible();
      const campoApellidos = page.getByTestId('field:personaInteresada.apellidos').getByRole('textbox');
      const campoNombre = page.getByTestId('field:personaInteresada.nombre').getByRole('textbox');
      const campoDni = page.getByTestId('field:personaInteresada.dni').getByRole('textbox');
      await expect(campoApellidos).toHaveValue(DIRECTOR_APELLIDOS);
      await expect(campoNombre).toHaveValue(DIRECTOR_NOMBRE);
      await expect(campoDni).toHaveValue(DIRECTOR_DNI);
      await expect(campoApellidos).toBeDisabled();
      await expect(campoNombre).toBeDisabled();
      await expect(campoDni).toBeDisabled();
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
