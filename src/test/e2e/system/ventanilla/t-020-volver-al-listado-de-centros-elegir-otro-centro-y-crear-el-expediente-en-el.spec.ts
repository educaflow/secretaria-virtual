import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-020 — Volver al listado de centros, elegir otro centro y crear el expediente en él
// origen: ESC-019  |  verifica: U-nuevo-expediente-001, U-nuevo-expediente-002, U-nuevo-expediente-003, U-nuevo-expediente-005, R-AsistenteNuevoExpediente-001, R-AsistenteNuevoExpediente-004
// fuente: .sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/test-e2e-desc/t-020-volver-al-listado-de-centros-elegir-otro-centro-y-crear-el-expediente-en-el.desc.md

/**
 * IDEMPOTENCIA (§4 del contrato de generación) — este test CREA un expediente, y su
 * identificador (`NNNNN/2026-<código del centro>`) lo asigna el servidor con un contador, así que NO se
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
 * listados que sí comprueba —centros y trámites, en los dos centros— dependen solo
 * de los datos maestros del estado inicial, que ningún test modifica. El ida y
 * vuelta con "Atrás" tampoco deja nada: es navegación pura, sin escritura.
 */

// Credenciales del usuario de la precondición (tabla «Usuarios de acceso» del .desc.md):
// alumno en DOS centros, CIPFP Mislata y CIPFP Batoi. Es el caso clave del escenario:
// al tener más de un centro, el asistente le pregunta en cuál crea el expediente, y por
// tanto tiene una pantalla de elección de centro a la que volver con "Atrás".
const USUARIO = 'alumnodoscentros@mislata.es';
const CONTRASENA = 'demo1234';

// Los DOS centros del alumno, EN ORDEN ALFABÉTICO: el orden del array es parte de la
// aserción (`toHaveText` compara posición a posición).
const CENTROS_DEL_ALUMNO = ['CIPFP Batoi', 'CIPFP Mislata'];
// El escenario pasa primero por Mislata (para volver atrás) y termina eligiendo Batoi
// (donde de verdad crea el expediente): el que NO debe acabar en el expediente es
// Mislata, aunque haya sido la primera elección.
const CENTRO_DESCARTADO = 'CIPFP Mislata';
const CENTRO_ELEGIDO = 'CIPFP Batoi';
const TRAMITE = 'Anulación de matrícula en ciclo formativo';
const TIPO_TRAMITE_ALUMNO = 'Trámites para el alumno';
const TIPO_TRAMITE_PROFESOR = 'Trámites para el profesor';
// Identidad del alumno tal y como la carga `data-demo/input/usuarios-demo.xml`.
const ALUMNO_NOMBRE = 'AlumnoDosCentros';
const ALUMNO_APELLIDOS = 'CIPFP Mislata y Batoi';
const ALUMNO_DNI = '71359246K';
const ALUMNO_NOMBRE_COMPLETO = 'AlumnoDosCentros CIPFP Mislata y Batoi';

// Títulos de las TRES pantallas del asistente (los fijan los `action-view` de
// `system/ventanilla/views/`). Se comparan con regex anclada porque Axelor añade un `*`
// al título de la pestaña cuando el formulario está sucio, y porque "Nuevo expediente" a
// secas es prefijo de las otras dos (sin anclar, cazaría pestañas que no son la suya).
const PANTALLA_CENTRO = /^Nuevo expediente: elija el centro\*?$/;
const PANTALLA_TRAMITE = /^Nuevo expediente: elija el trámite\*?$/;
const PANTALLA_CONTEXTO = /^Nuevo expediente\*?$/;

// Primer estado del tipo de expediente para quien lo presenta él mismo (perfil
// CREADOR), según `TipoExpedienteInstance.xml`: fase ENTRADA, estado ENTRADA_DATOS.
const FASE_INICIAL = 'Entrada';
const ESTADO_INICIAL = 'Entrada de datos';

// Panel que la plantilla común pinta con `showIf="presentadoEnRepresentacion"`: su
// ausencia prueba que el expediente es para el propio alumno, no en representación.
const PANEL_PERSONA_SOLICITANTE = 'panel:persona-solicitante-editable';

// Título de la pestaña del expediente creado: <nº>/<año>-<código del centro>-<trámite> V1.
const TITULO_EXPEDIENTE = new RegExp(`^\\d+/\\d{4}-\\d+-${TRAMITE} V1$`);

/**
 * Filas de datos del grid de centros (excluye la cabecera). Es un grid independiente
 * embebido en un `panel-dashlet` (testid `panel:centrosPanel`), con los testids
 * estándar de un grid embebido: sus filas llevan `row:`.
 */
function filasDeCentros(page: Page) {
  return page.getByTestId('panel:centrosPanel').locator('[data-testid^="row:"]');
}

/**
 * Filas de datos del árbol de trámites (excluye cabecera y filas de agrupación). Es un
 * `<tree>` embebido en un `panel-dashlet` (testid `panel:tramitesPanel`): sus nodos no
 * llevan `data-testid` propio, así que se localizan por rol ARIA de un `treegrid` —
 * `aria-level="2"` son las filas hoja (los trámites), `aria-level="1"` las de
 * agrupación (el tipo de trámite).
 */
function filasDeTramites(page: Page) {
  return page.getByTestId('panel:tramitesPanel').locator('[role="row"][aria-level="2"]');
}

/** Filas de agrupación por tipo de trámite del árbol de trámites. */
function gruposDeTipoTramite(page: Page) {
  return page.getByTestId('panel:tramitesPanel').locator('[role="row"][aria-level="1"]');
}

/**
 * Despliega todos los grupos de tipo de trámite del árbol. El `<tree>` nace con los
 * grupos plegados (icono `arrow_right`, sin `aria-expanded`) y hay que pulsarlos para
 * que sus filas de datos existan en el DOM. Cada vez que la pantalla del listado de
 * trámites se vuelve a montar (p. ej. al elegir otro centro) el árbol nace plegado otra
 * vez, así que hay que volver a llamar a esta función.
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

/** Botonera del asistente: la comparten sus tres pantallas, cada una con sus botones. */
function botonera(page: Page) {
  return page.getByTestId('panel:buttons-panel');
}

/**
 * Abre el asistente desde el menú «Mis trámites» → «Nuevo trámite». El grupo se pliega
 * y despliega al pulsarlo, así que solo se despliega si la entrada no se ve: pulsarlo a
 * ciegas lo cerraría cuando ya venía abierto.
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
 * confirmación de Axelor ("Se va a eliminar el expediente y no podrá recuperarlo") que
 * hay que aceptar. Al borrarlo el asistente vuelve a la elección de centro.
 */
async function borrarExpediente(page: Page, titulo: string): Promise<void> {
  await page.getByTestId('widget:DELETE').getByRole('button').click();
  await page.getByRole('button', { name: 'Aceptar' }).click();
  await expect(page.getByRole('tab', { name: titulo, exact: true })).toHaveCount(0);
}

test.describe('Ventanilla — Nuevo expediente', () => {
  test('Volver al listado de centros, elegir otro centro y crear el expediente en él', async ({
    page,
  }) => {
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

      // Paso 3: Entonces se abre "Nuevo expediente: elija el centro" con las filas
      // "CIPFP Batoi" y "CIPFP Mislata" (en ese orden) y un botón "Cancelar" — uno
      // solo, porque es la primera pantalla del asistente.
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO })).toBeVisible();
      await expect(page.getByTestId('panel:centrosPanel')).toBeVisible();
      await expect(filasDeCentros(page)).toHaveText(CENTROS_DEL_ALUMNO);
      await expect(botonera(page).getByRole('button')).toHaveCount(1);
      await expect(botonera(page).getByRole('button', { name: 'Cancelar' })).toBeVisible();

      // Paso 4: Cuando pulsa la fila "CIPFP Mislata".
      await filasDeCentros(page).filter({ hasText: CENTRO_DESCARTADO }).click();

      // Paso 5: Entonces se abre "Nuevo expediente: elija el trámite" con el centro
      // "CIPFP Mislata" encima del listado…
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE })).toBeVisible();
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO })).toHaveCount(0);
      const campoCentroMislata = page.getByTestId('field:centro').getByRole('textbox');
      await expect(campoCentroMislata).toHaveValue(CENTRO_DESCARTADO);
      await expect(campoCentroMislata).not.toHaveValue(CENTRO_ELEGIDO);
      // "Encima" es posición real en pantalla, no solo presencia.
      const cajaCentroMislata = await page.getByTestId('panel:centroPanel').boundingBox();
      const cajaTramitesMislata = await page.getByTestId('panel:tramitesPanel').boundingBox();
      expect(cajaCentroMislata).not.toBeNull();
      expect(cajaTramitesMislata).not.toBeNull();
      expect(cajaCentroMislata!.y).toBeLessThan(cajaTramitesMislata!.y);
      // …y un único botón debajo, "Atrás" (no hay "Cancelar": al haber habido elección
      // de centro, el asistente ofrece volver a ella).
      await expect(botonera(page).getByRole('button')).toHaveCount(1);
      await expect(botonera(page).getByRole('button', { name: 'Atrás' })).toBeVisible();

      // Paso 6: Cuando pulsa "Atrás" debajo del listado.
      await botonera(page).getByRole('button', { name: 'Atrás' }).click();

      // Paso 7: Entonces vuelve a "Nuevo expediente: elija el centro" con las filas
      // "CIPFP Batoi" y "CIPFP Mislata" — se ha retrocedido, no apilado: la pantalla de
      // trámites queda cerrada.
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO })).toBeVisible();
      await expect(page.getByTestId('panel:centrosPanel')).toBeVisible();
      await expect(filasDeCentros(page)).toHaveText(CENTROS_DEL_ALUMNO);
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE })).toHaveCount(0);
      await expect(page.getByTestId('panel:tramitesPanel')).toHaveCount(0);
      await expect(botonera(page).getByRole('button')).toHaveCount(1);
      await expect(botonera(page).getByRole('button', { name: 'Cancelar' })).toBeVisible();

      // Paso 8: Cuando pulsa la fila "CIPFP Batoi".
      await filasDeCentros(page).filter({ hasText: CENTRO_ELEGIDO }).click();

      // Paso 9: Entonces se abre "Nuevo expediente: elija el trámite" con el centro
      // "CIPFP Batoi" encima del listado…
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE })).toBeVisible();
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO })).toHaveCount(0);
      const campoCentroBatoi = page.getByTestId('field:centro').getByRole('textbox');
      await expect(campoCentroBatoi).toHaveValue(CENTRO_ELEGIDO);
      await expect(campoCentroBatoi).not.toHaveValue(CENTRO_DESCARTADO);
      const cajaCentroBatoi = await page.getByTestId('panel:centroPanel').boundingBox();
      const cajaTramitesBatoi = await page.getByTestId('panel:tramitesPanel').boundingBox();
      expect(cajaCentroBatoi).not.toBeNull();
      expect(cajaTramitesBatoi).not.toBeNull();
      expect(cajaCentroBatoi!.y).toBeLessThan(cajaTramitesBatoi!.y);
      // …únicamente "Trámites para el alumno" con "Anulación de matrícula en ciclo
      // formativo" dentro (no aparece "Trámites para el profesor")…
      await desplegarGruposDeTramites(page);
      await expect(gruposDeTipoTramite(page)).toHaveCount(1);
      await expect(gruposDeTipoTramite(page).first()).toContainText(TIPO_TRAMITE_ALUMNO);
      await expect(filasDeTramites(page)).toHaveText([TRAMITE]);
      await expect(page.getByText(TIPO_TRAMITE_PROFESOR)).toHaveCount(0);
      // …y un único botón debajo, "Atrás".
      await expect(botonera(page).getByRole('button')).toHaveCount(1);
      await expect(botonera(page).getByRole('button', { name: 'Atrás' })).toBeVisible();
      const cajaBotoneraBatoi = await botonera(page).boundingBox();
      expect(cajaBotoneraBatoi).not.toBeNull();
      expect(cajaBotoneraBatoi!.y).toBeGreaterThan(cajaTramitesBatoi!.y);

      // Paso 10: Cuando pulsa la fila "Anulación de matrícula en ciclo formativo".
      await filasDeTramites(page).first().click();

      // Paso 11: Entonces se abre "Nuevo expediente" con ese trámite, su ayuda y el
      // centro "CIPFP Batoi" en solo lectura, sin las dos preguntas.
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO })).toBeVisible();
      const campoTramite = page.getByTestId('field:nombreTramite').getByRole('textbox');
      await expect(campoTramite).toHaveValue(TRAMITE);
      await expect(campoTramite).toBeDisabled();
      const centroContexto = page.getByTestId('field:centro').getByRole('textbox');
      await expect(centroContexto).toHaveValue(CENTRO_ELEGIDO);
      await expect(centroContexto).not.toHaveValue(CENTRO_DESCARTADO);
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
      await expect(page.getByRole('button', { name: 'Atrás' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Crear expediente' })).toBeVisible();

      // Paso 12: Cuando pulsa "Crear expediente".
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
      // "CIPFP Mislata" — el que el alumno eligió al final del ida y vuelta, no el
      // primero que descartó con "Atrás".
      const centroExpediente = page.getByTestId('field:centro.name').getByRole('textbox');
      await expect(centroExpediente).toHaveValue(CENTRO_ELEGIDO);
      await expect(centroExpediente).not.toHaveValue(CENTRO_DESCARTADO);

      // Resultado esperado: presentado por el propio alumno (no por personal del
      // centro).
      await expect(page.getByTestId('field:createdBy').getByRole('textbox')).toHaveValue(
        ALUMNO_NOMBRE_COMPLETO,
      );

      // Resultado esperado: y para él mismo, NO en representación de otra persona. El
      // panel "Persona que presenta la solicitud" solo se pinta cuando hay
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
      // Teardown: borrar el expediente creado aunque una aserción haya fallado — la BD
      // es compartida y no se resetea, así que dejarlo lo acumularía run tras run. Si
      // el test falló ANTES de crearlo no hay nada que borrar.
      if (tituloExpediente !== '') {
        await borrarExpediente(page, tituloExpediente);
      }

      // Al borrar el expediente el asistente vuelve a la elección de centro (y si el
      // fallo fue antes de crearlo, la pantalla del asistente sigue abierta, en el paso
      // que fuera): se cierra con su propio botón "Cancelar" para no heredar la pestaña
      // al siguiente run. Si no hay ninguna pantalla abierta el botón no existe: el
      // `.catch` es intencional, no un error tapado.
      await page
        .getByTestId('panel:buttons-panel')
        .getByRole('button', { name: 'Cancelar' })
        .click({ timeout: 5000 })
        .catch(() => {});

      await logout(page);
    }
  });
});
