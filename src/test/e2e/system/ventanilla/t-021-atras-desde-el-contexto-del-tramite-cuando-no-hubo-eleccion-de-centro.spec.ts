import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-021 — Atrás desde el contexto del trámite cuando no hubo elección de centro
// origen: ESC-020  |  verifica: U-nuevo-expediente-002, U-nuevo-expediente-005
// fuente: .sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/test-e2e-desc/t-021-atras-desde-el-contexto-del-tramite-cuando-no-hubo-eleccion-de-centro.desc.md

/**
 * IDEMPOTENCIA (§4 del contrato de generación) — este test es de SOLO LECTURA, y lo es
 * por construcción: recorre el asistente hacia delante hasta el contexto del trámite y
 * lo deshace con "Atrás" y luego "Cancelar", sin llegar nunca a "Crear expediente". No
 * inserta, modifica ni borra ninguna fila, así que no hay ningún nombre que aislar con
 * un sufijo `Date.now()` ni nada que borrar en el teardown; de hecho, que NO se haya
 * creado ningún expediente es parte de lo que el test afirma.
 * Tampoco hace falta pre-limpieza defensiva: el único listado que comprueba —el de
 * trámites— depende solo de los datos maestros del estado inicial (los permisos del
 * alumno para iniciar trámites en su único centro), que ningún test modifica, así que
 * un run que abortara no puede dejar nada pegado que cambie estas aserciones. Lo único
 * que un fallo puede dejar en pantalla es la propia pestaña del asistente, y el
 * `finally` la cierra para que un run no la herede del anterior.
 * Nota de diseño del propio escenario (a diferencia de T-007, que usa un alumno de DOS
 * centros): este test usa `alumno1@mislata.es`, que solo tiene UN centro, así que el
 * asistente nunca muestra la pantalla de elección de centro. El punto del test es
 * precisamente ese: pulsar "Atrás" desde el contexto del trámite vuelve al listado de
 * trámites (con su único botón "Cancelar", sin "Atrás"), no a una pantalla de elección
 * de centro que para este alumno no existe.
 */

// Credenciales del usuario de la precondición (tabla «Usuarios de acceso» del .desc.md).
const USUARIO = 'alumno1@mislata.es';
const CONTRASENA = 'demo1234';

// Datos del estado inicial de la BD que el test da por sentados.
const CENTRO = 'CIPFP Mislata';
const TRAMITE = 'Anulación de matrícula en ciclo formativo';
const TIPO_TRAMITE_ALUMNO = 'Trámites para el alumno';
const TIPO_TRAMITE_PROFESOR = 'Trámites para el profesor';

// Primera frase del texto de ayuda del trámite (`TramiteInstance.xml`): verla es la
// prueba de que el contexto del trámite pinta su ayuda.
const AYUDA_TRAMITE =
  'Con este trámite puedes solicitar la anulación de tu matrícula en un ciclo formativo de este centro';

// Títulos de las TRES pantallas del asistente (los fijan los `action-view` de
// `system/ventanilla/views/`). Se comparan con regex anclada porque Axelor añade un
// `*` al título de la pestaña cuando el formulario está sucio, y porque "Nuevo
// expediente" a secas es prefijo de las otras dos (sin anclar, cazaría pestañas que no
// son la suya).
const PANTALLA_CENTRO = /^Nuevo expediente: elija el centro\*?$/;
const PANTALLA_TRAMITE = /^Nuevo expediente: elija el trámite\*?$/;
const PANTALLA_CONTEXTO = /^Nuevo expediente\*?$/;

// Título que tendría la pestaña de un expediente ya creado: <nº>/<año>-<trámite> V1.
// Retroceder con "Atrás" y luego "Cancelar" no debe abrir ninguna, sea del trámite que
// sea.
const TITULO_EXPEDIENTE = /^\d+\/\d{4}-.+ V1\*?$/;

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
 * Despliega todos los grupos de tipo de trámite del árbol. El `<tree>` nace con los
 * grupos plegados (icono `arrow_right`, sin `aria-expanded`) y hay que pulsarlos para
 * que sus filas de datos existan en el DOM. Cada vez que la pantalla del listado de
 * trámites se vuelve a montar (p. ej. al volver con "Atrás") el árbol nace plegado otra
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
 * Comprueba que NO hay ningún expediente abierto: ni una pestaña con el título de uno,
 * ni el formulario de uno pintado (fase y estado son los campos que todo expediente
 * muestra).
 */
async function noHayNingunExpedienteAbierto(page: Page): Promise<void> {
  await expect(page.getByRole('tab', { name: TITULO_EXPEDIENTE })).toHaveCount(0);
  await expect(page.getByTestId('field:namePhase')).toHaveCount(0);
  await expect(page.getByTestId('field:nameState')).toHaveCount(0);
}

/**
 * Abre el asistente desde el menú «Ventanilla» → «Nuevo expediente». El grupo se pliega
 * y despliega al pulsarlo, así que solo se despliega si la entrada no se ve: pulsarlo a
 * ciegas lo cerraría cuando ya venía abierto.
 */
async function abrirNuevoExpediente(page: Page): Promise<void> {
  const entrada = page.getByTestId('item:ventanilla-nuevoExpediente-menuitem');
  if (!(await entrada.isVisible())) {
    await page.getByTestId('item:ventanilla-menuitem').getByTestId('title').first().click();
  }
  await entrada.click();
}

test.describe('Ventanilla — Nuevo expediente', () => {
  test('Atrás desde el contexto del trámite cuando no hubo elección de centro', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el alumno `alumno1@mislata.es` ha iniciado sesión con la
    // contraseña `demo1234`.
    await login(page, USUARIO, CONTRASENA);

    try {
      // Paso 2: Cuando abre el menú "Ventanilla" y pulsa "Nuevo expediente".
      await abrirNuevoExpediente(page);

      // Paso 3: Entonces se abre directamente "Nuevo expediente: elija el trámite"
      // (NO se abre la pantalla de elección de centro: este alumno solo tiene un
      // centro) con el centro "CIPFP Mislata" encima del listado…
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE })).toBeVisible();
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO })).toHaveCount(0);
      await expect(page.getByTestId('panel:centrosPanel')).toHaveCount(0);

      const campoCentroTramites = page.getByTestId('field:centro').getByRole('textbox');
      await expect(campoCentroTramites).toHaveValue(CENTRO);
      await expect(campoCentroTramites).toBeDisabled();
      // "Encima" es posición real en pantalla, no solo presencia: el panel del centro
      // tiene que quedar por encima del grid/árbol de trámites.
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

      // …y un único botón debajo, "Cancelar".
      await expect(botonera(page).getByRole('button')).toHaveCount(1);
      await expect(botonera(page).getByRole('button', { name: 'Cancelar' })).toBeVisible();

      // Paso 4: Cuando pulsa la fila "Anulación de matrícula en ciclo formativo".
      await filasDeTramites(page).first().click();

      // Paso 5: Entonces se abre "Nuevo expediente" con ese trámite, su ayuda y el
      // centro en solo lectura, sin las dos preguntas, y con los botones "Atrás" y
      // "Crear expediente".
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO })).toBeVisible();
      const campoTramite = page.getByTestId('field:nombreTramite').getByRole('textbox');
      await expect(campoTramite).toHaveValue(TRAMITE);
      await expect(campoTramite).toBeDisabled();
      const campoCentroContexto = page.getByTestId('field:centro').getByRole('textbox');
      await expect(campoCentroContexto).toHaveValue(CENTRO);
      await expect(campoCentroContexto).toBeDisabled();
      await expect(page.getByTestId('field:ayudaTramite')).toContainText(AYUDA_TRAMITE);

      // …sin las dos preguntas ("¿Cómo se presenta?" y "¿Para quién es el
      // expediente?"). Se comprueban por su etiqueta y también por el campo del
      // modelo que las pinta (`presentadoEnPapel` / `presentadoEnRepresentacion`),
      // para que el test siga cazando el fallo aunque cambie el rótulo.
      await expect(page.getByText('¿Cómo se presenta?')).toHaveCount(0);
      await expect(page.getByText('¿Para quién es el expediente?')).toHaveCount(0);
      await expect(page.getByTestId('field:presentadoEnPapel')).toHaveCount(0);
      await expect(page.getByTestId('field:presentadoEnRepresentacion')).toHaveCount(0);

      await expect(botonera(page).getByRole('button')).toHaveCount(2);
      await expect(botonera(page).getByRole('button', { name: 'Atrás' })).toBeVisible();
      await expect(botonera(page).getByRole('button', { name: 'Crear expediente' })).toBeVisible();

      // Paso 6: Cuando pulsa "Atrás".
      await botonera(page).getByRole('button', { name: 'Atrás' }).click();

      // Paso 7: Entonces vuelve a "Nuevo expediente: elija el trámite" con el centro
      // "CIPFP Mislata" encima del listado, sin abrir ningún expediente…
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE })).toBeVisible();
      await expect(page.getByTestId('panel:tramitesPanel')).toBeVisible();
      // …y el contexto del trámite queda cerrado (ni su pestaña ni su contenido
      // siguen ahí): se ha retrocedido de verdad, no se ha abierto una pantalla
      // encima.
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO })).toHaveCount(0);
      await expect(page.getByTestId('panel:tramitePanel')).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Crear expediente' })).toHaveCount(0);

      const campoCentroAlVolver = page.getByTestId('field:centro').getByRole('textbox');
      await expect(campoCentroAlVolver).toHaveValue(CENTRO);
      const cajaCentroAlVolver = await page.getByTestId('panel:centroPanel').boundingBox();
      const cajaTramitesAlVolver = await page.getByTestId('panel:tramitesPanel').boundingBox();
      expect(cajaCentroAlVolver).not.toBeNull();
      expect(cajaTramitesAlVolver).not.toBeNull();
      expect(cajaCentroAlVolver!.y).toBeLessThan(cajaTramitesAlVolver!.y);

      await noHayNingunExpedienteAbierto(page);
      await expect(page.getByRole('tab')).toHaveCount(1);

      // …y debajo del listado sigue habiendo un único botón, "Cancelar" (no hay
      // "Atrás"): al no haber habido elección de centro, no hay pantalla anterior a
      // la que ofrecer volver.
      await desplegarGruposDeTramites(page);
      await expect(filasDeTramites(page)).toHaveText([TRAMITE]);
      await expect(botonera(page).getByRole('button')).toHaveCount(1);
      await expect(botonera(page).getByRole('button', { name: 'Cancelar' })).toBeVisible();
      await expect(botonera(page).getByRole('button', { name: 'Atrás' })).toHaveCount(0);
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO })).toHaveCount(0);

      // Paso 8: Cuando pulsa "Cancelar".
      await botonera(page).getByRole('button', { name: 'Cancelar' }).click();

      // Resultado esperado: el asistente se cierra sin abrir ningún expediente — no
      // queda visible ninguna de sus pantallas y no hay ninguna pestaña abierta.
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE })).toHaveCount(0);
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO })).toHaveCount(0);
      await expect(page.getByTestId('panel:tramitesPanel')).toHaveCount(0);
      await noHayNingunExpedienteAbierto(page);
      await expect(page.getByRole('tab')).toHaveCount(0);
    } finally {
      // Teardown: el camino verde ya cierra el asistente con "Cancelar" en el propio
      // paso 8, así que normalmente no queda nada que cerrar. Si una aserción falló
      // antes, puede quedar abierta una pantalla del asistente con su propio botón
      // "Cancelar"; se intenta cerrar de forma defensiva para no heredar la pestaña
      // al siguiente run. El test no crea nada más que borrar.
      await botonera(page)
        .getByRole('button', { name: 'Cancelar' })
        .click({ timeout: 5000 })
        .catch(() => {});

      await logout(page);
    }
  });
});
