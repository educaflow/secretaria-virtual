import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-007 — Atrás desde el contexto del trámite y desde el listado de trámites
// origen: ESC-007  |  verifica: U-nuevo-expediente-002, U-nuevo-expediente-003, U-nuevo-expediente-005
// fuente: .sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/test-e2e-desc/t-007-atras-desde-el-contexto-del-tramite-y-desde-el-listado-de-tramites.desc.md

/**
 * IDEMPOTENCIA (§4 del contrato de generación) — este test es de SOLO LECTURA, y lo es
 * por construcción: recorre el asistente hacia delante hasta el contexto del trámite y
 * deshace ese camino con los dos "Atrás", sin llegar nunca a "Crear expediente". No
 * inserta, modifica ni borra ninguna fila, así que no hay ningún nombre que aislar con
 * un sufijo `Date.now()` ni nada que borrar en el teardown; de hecho, que NO se haya
 * creado ningún expediente es parte de lo que el test afirma.
 * Tampoco hace falta pre-limpieza defensiva: los dos listados que comprueba —centros y
 * trámites— dependen solo de los datos maestros del estado inicial (los centros del
 * alumno y sus permisos para iniciar trámites), que ningún test modifica, así que un run
 * que abortara no puede dejar nada pegado que cambie estas aserciones. Lo único que un
 * fallo puede dejar en pantalla es la propia pestaña del asistente, y el `finally` la
 * cierra para que un run no la herede del anterior.
 */

// Credenciales del usuario de la precondición (tabla «Usuarios de acceso» del .desc.md):
// alumno en DOS centros, CIPFP Mislata y CIPFP Batoi. Es el usuario que hace falta para
// este escenario: solo a quien tiene más de un centro le pregunta el asistente en cuál
// crea el expediente, y por tanto solo él tiene una pantalla de elección de centro a la
// que volver con el segundo "Atrás".
const USUARIO = 'alumnodoscentros@mislata.es';
const CONTRASENA = 'demo1234';

// Los DOS centros del alumno, EN EL ORDEN EN QUE LOS PINTA EL GRID (alfabético): el
// orden del array es parte de la aserción, porque `toHaveText` compara posición a
// posición.
const CENTROS_DEL_ALUMNO = ['CIPFP Batoi', 'CIPFP Mislata'];
// El centro que el alumno ELIGE, y el otro (el que NO debe aparecer como centro elegido
// ni al entrar ni al volver atrás al listado de trámites).
const CENTRO_ELEGIDO = 'CIPFP Mislata';
const CENTRO_NO_ELEGIDO = 'CIPFP Batoi';

// Único trámite que el alumno puede iniciar, y su tipo. El otro tipo de trámite
// («Trámites para el profesor») no debe aparecerle.
const TRAMITE = 'Anulación de matrícula en ciclo formativo';
const TIPO_TRAMITE_ALUMNO = 'Trámites para el alumno';
const TIPO_TRAMITE_PROFESOR = 'Trámites para el profesor';

// Primera frase del texto de ayuda del trámite (`TramiteInstance.xml`): verla es la
// prueba de que el contexto del trámite pinta su ayuda.
const AYUDA_TRAMITE =
  'Con este trámite puedes solicitar la anulación de tu matrícula en un ciclo formativo de este centro';

// Títulos de las TRES pantallas del asistente (los fijan los `action-view` de
// `system/ventanilla/views/`). Se usan para seguir en qué paso del asistente estamos y
// para comprobar que la pantalla que se deja atrás se cierra. Se comparan con regex
// anclada porque Axelor añade un `*` al título de la pestaña cuando el formulario está
// sucio, y porque "Nuevo expediente" a secas es prefijo de las otras dos (sin anclar,
// cazaría pestañas que no son la suya).
const PANTALLA_CENTRO = /^Nuevo expediente: elija el centro\*?$/;
const PANTALLA_TRAMITE = /^Nuevo expediente: elija el trámite\*?$/;
const PANTALLA_CONTEXTO = /^Nuevo expediente\*?$/;

// Título que tendría la pestaña de un expediente ya creado: <nº>/<año>-<trámite> V1.
// Retroceder con "Atrás" no debe abrir ninguna, sea del trámite que sea.
const TITULO_EXPEDIENTE = /^\d+\/\d{4}-.+ V1\*?$/;

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
 * para que sus filas de datos existan en el DOM. Cada vez que la pantalla del listado
 * de trámites se vuelve a montar (p. ej. al volver con "Atrás") el árbol nace plegado
 * otra vez, así que hay que volver a llamar a esta función.
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
 * Comprueba que NO hay ningún expediente abierto: ni una pestaña con el título de uno,
 * ni el formulario de uno pintado (fase y estado son los campos que todo expediente
 * muestra). Se usa en los dos puntos en que el test lo afirma —tras el primer "Atrás" y
 * al final—, porque el sentido del escenario es que retroceder no crea nada.
 */
async function noHayNingunExpedienteAbierto(page: Page): Promise<void> {
  await expect(page.getByRole('tab', { name: TITULO_EXPEDIENTE })).toHaveCount(0);
  await expect(page.getByTestId('field:namePhase')).toHaveCount(0);
  await expect(page.getByTestId('field:nameState')).toHaveCount(0);
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

test.describe('Ventanilla — Nuevo expediente', () => {
  test('Atrás desde el contexto del trámite y desde el listado de trámites', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el alumno `alumnodoscentros@mislata.es` ha iniciado sesión con la
    // contraseña `demo1234`.
    await login(page, USUARIO, CONTRASENA);

    try {
      // Paso 2: Cuando abre el menú "Ventanilla" y pulsa "Nuevo expediente".
      await abrirNuevoExpediente(page);

      // Paso 3: Entonces se abre "Nuevo expediente: elija el centro"…
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO })).toBeVisible();
      await expect(page.getByTestId('panel:centrosPanel')).toBeVisible();

      // …con las filas "CIPFP Batoi" y "CIPFP Mislata". `toHaveText` con un array compara
      // posición a posición, así que verifica a la vez el contenido, el número de filas
      // (dos, ni una más) y el orden.
      await expect(filasDeCentros(page)).toHaveText(CENTROS_DEL_ALUMNO);

      // …y un botón "Cancelar" (uno solo: en esta pantalla no hay "Atrás", porque es la
      // primera del asistente y no hay adónde volver).
      await expect(botonera(page).getByRole('button')).toHaveCount(1);
      await expect(botonera(page).getByRole('button', { name: 'Cancelar' })).toBeVisible();

      // Paso 4: Cuando pulsa la fila "CIPFP Mislata".
      await filasDeCentros(page).filter({ hasText: CENTRO_ELEGIDO }).click();

      // Paso 5: Entonces se abre "Nuevo expediente: elija el trámite"…
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE })).toBeVisible();
      // …y la pantalla de elección de centro se cierra al pasar a esta.
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO })).toHaveCount(0);
      await expect(page.getByTestId('panel:centrosPanel')).toHaveCount(0);

      // …con el centro "CIPFP Mislata" encima del listado: el centro que el alumno acaba
      // de elegir, no el otro al que también pertenece.
      const campoCentroTramites = page.getByTestId('field:centro').getByRole('textbox');
      await expect(campoCentroTramites).toHaveValue(CENTRO_ELEGIDO);
      await expect(campoCentroTramites).not.toHaveValue(CENTRO_NO_ELEGIDO);
      // "Encima" es posición real en pantalla, no solo presencia: el panel del centro
      // tiene que quedar por encima del grid de trámites.
      const cajaCentro = await page.getByTestId('panel:centroPanel').boundingBox();
      const cajaTramites = await page.getByTestId('panel:tramitesPanel').boundingBox();
      expect(cajaCentro).not.toBeNull();
      expect(cajaTramites).not.toBeNull();
      expect(cajaCentro!.y).toBeLessThan(cajaTramites!.y);

      // …con únicamente "Trámites para el alumno" y, dentro, únicamente "Anulación de
      // matrícula en ciclo formativo"; no aparece "Trámites para el profesor".
      await desplegarGruposDeTramites(page);
      await expect(gruposDeTipoTramite(page)).toHaveCount(1);
      await expect(gruposDeTipoTramite(page).first()).toContainText(TIPO_TRAMITE_ALUMNO);
      await expect(filasDeTramites(page)).toHaveText([TRAMITE]);
      await expect(page.getByText(TIPO_TRAMITE_PROFESOR)).toHaveCount(0);

      // …y un único botón debajo, "Atrás" (no hay "Cancelar": al haber habido elección de
      // centro, el asistente ofrece volver a ella en vez de cancelar).
      await expect(botonera(page).getByRole('button')).toHaveCount(1);
      await expect(botonera(page).getByRole('button', { name: 'Atrás' })).toBeVisible();
      await expect(botonera(page).getByRole('button', { name: 'Cancelar' })).toHaveCount(0);
      // "Debajo" es posición real en pantalla: la botonera va bajo el listado, no encima.
      const cajaBotonera = await botonera(page).boundingBox();
      expect(cajaBotonera).not.toBeNull();
      expect(cajaBotonera!.y).toBeGreaterThan(cajaTramites!.y);

      // Paso 6: Cuando pulsa la fila "Anulación de matrícula en ciclo formativo".
      await filasDeTramites(page).first().click();

      // Paso 7: Entonces se abre "Nuevo expediente" con ese trámite, su ayuda y el centro
      // "CIPFP Mislata" en solo lectura…
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO })).toBeVisible();
      const campoTramite = page.getByTestId('field:nombreTramite').getByRole('textbox');
      await expect(campoTramite).toHaveValue(TRAMITE);
      await expect(campoTramite).toBeDisabled();
      const campoCentroContexto = page.getByTestId('field:centro').getByRole('textbox');
      await expect(campoCentroContexto).toHaveValue(CENTRO_ELEGIDO);
      await expect(campoCentroContexto).not.toHaveValue(CENTRO_NO_ELEGIDO);
      await expect(campoCentroContexto).toBeDisabled();
      await expect(page.getByTestId('field:ayudaTramite')).toContainText(AYUDA_TRAMITE);

      // …sin las dos preguntas ("¿Cómo se presenta?" y "¿Para quién es el expediente?").
      // Se comprueban por su etiqueta y también por el campo del modelo que las pinta
      // (`presentadoEnPapel` / `presentadoEnRepresentacion`), para que el test siga
      // cazando el fallo aunque cambie el rótulo.
      await expect(page.getByText('¿Cómo se presenta?')).toHaveCount(0);
      await expect(page.getByText('¿Para quién es el expediente?')).toHaveCount(0);
      await expect(page.getByTestId('field:presentadoEnPapel')).toHaveCount(0);
      await expect(page.getByTestId('field:presentadoEnRepresentacion')).toHaveCount(0);

      // …y con los botones "Atrás" y "Crear expediente" (esos dos, y ningún otro).
      await expect(botonera(page).getByRole('button')).toHaveCount(2);
      await expect(botonera(page).getByRole('button', { name: 'Atrás' })).toBeVisible();
      await expect(botonera(page).getByRole('button', { name: 'Crear expediente' })).toBeVisible();

      // Paso 8: Cuando pulsa "Atrás".
      await botonera(page).getByRole('button', { name: 'Atrás' }).click();

      // Paso 9: Entonces vuelve a "Nuevo expediente: elija el trámite"…
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE })).toBeVisible();
      await expect(page.getByTestId('panel:tramitesPanel')).toBeVisible();
      await desplegarGruposDeTramites(page);
      await expect(filasDeTramites(page)).toHaveText([TRAMITE]);
      // …y el contexto del trámite queda cerrado (ni su pestaña ni su contenido siguen
      // ahí): se ha retrocedido de verdad, no se ha abierto una pantalla encima.
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO })).toHaveCount(0);
      await expect(page.getByTestId('panel:tramitePanel')).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Crear expediente' })).toHaveCount(0);

      // …con el MISMO centro "CIPFP Mislata" encima del listado: volver atrás conserva la
      // elección de centro, no la pierde ni la cambia por el otro centro del alumno.
      const campoCentroAlVolver = page.getByTestId('field:centro').getByRole('textbox');
      await expect(campoCentroAlVolver).toHaveValue(CENTRO_ELEGIDO);
      await expect(campoCentroAlVolver).not.toHaveValue(CENTRO_NO_ELEGIDO);
      const cajaCentroAlVolver = await page.getByTestId('panel:centroPanel').boundingBox();
      const cajaTramitesAlVolver = await page.getByTestId('panel:tramitesPanel').boundingBox();
      expect(cajaCentroAlVolver).not.toBeNull();
      expect(cajaTramitesAlVolver).not.toBeNull();
      expect(cajaCentroAlVolver!.y).toBeLessThan(cajaTramitesAlVolver!.y);

      // …sin abrir ningún expediente: pulsar "Atrás" en el contexto del trámite retrocede,
      // no crea. Y el asistente sigue ocupando una sola pestaña, la suya.
      await noHayNingunExpedienteAbierto(page);
      await expect(page.getByRole('tab')).toHaveCount(1);

      // Paso 10: Cuando pulsa "Atrás" debajo del listado.
      await botonera(page).getByRole('button', { name: 'Atrás' }).click();

      // Resultado esperado: vuelve a "Nuevo expediente: elija el centro" con las filas
      // "CIPFP Batoi" y "CIPFP Mislata" (las dos, en su orden).
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO })).toBeVisible();
      await expect(page.getByTestId('panel:centrosPanel')).toBeVisible();
      await expect(filasDeCentros(page)).toHaveText(CENTROS_DEL_ALUMNO);
      // Y es de nuevo la PRIMERA pantalla del asistente: su única salida vuelve a ser
      // "Cancelar", porque ya no hay ningún paso anterior al que retroceder.
      await expect(botonera(page).getByRole('button')).toHaveCount(1);
      await expect(botonera(page).getByRole('button', { name: 'Cancelar' })).toBeVisible();
      await expect(botonera(page).getByRole('button', { name: 'Atrás' })).toHaveCount(0);

      // Resultado esperado: la pantalla de elección de trámite queda cerrada — se ha
      // retrocedido, no apilado.
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE })).toHaveCount(0);
      await expect(page.getByTestId('panel:tramitesPanel')).toHaveCount(0);
      await expect(page.getByTestId('panel:centroPanel')).toHaveCount(0);

      // Resultado esperado: no se ha abierto ningún expediente. Ni el contexto del
      // trámite sigue abierto, ni hay pestaña de un expediente creado, ni se pinta el
      // formulario de uno; y el asistente sigue ocupando una sola pestaña, la suya, que
      // es la forma más directa de comprobar que todo el ida y vuelta no dejó nada más
      // abierto.
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO })).toHaveCount(0);
      await expect(page.getByTestId('panel:tramitePanel')).toHaveCount(0);
      await noHayNingunExpedienteAbierto(page);
      await expect(page.getByRole('tab')).toHaveCount(1);
    } finally {
      // Teardown: el camino verde termina con el asistente abierto en la elección de
      // centro (y si una aserción falló antes, con la pantalla que fuera): se cierra con
      // su propio botón "Cancelar" para no heredar la pestaña al siguiente run. Si el
      // fallo dejó una pantalla sin "Cancelar" el botón no existe: el `.catch` es
      // intencional, no un error tapado. El test no crea nada más que borrar.
      await botonera(page)
        .getByRole('button', { name: 'Cancelar' })
        .click({ timeout: 5000 })
        .catch(() => {});

      await logout(page);
    }
  });
});
