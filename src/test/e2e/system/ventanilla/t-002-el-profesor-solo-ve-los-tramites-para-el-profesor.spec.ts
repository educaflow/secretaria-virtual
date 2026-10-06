import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-002 — El profesor solo ve los trámites para el profesor
// origen: ESC-002  |  verifica: U-nuevo-expediente-002, U-nuevo-expediente-003, U-nuevo-expediente-004, R-AsistenteNuevoExpediente-004
// fuente: .sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/test-e2e-desc/t-002-el-profesor-solo-ve-los-tramites-para-el-profesor.desc.md

/**
 * IDEMPOTENCIA (§4 del contrato de generación) — este test es de SOLO LECTURA: abre
 * el asistente de nuevo expediente y comprueba qué trámites le ofrece al director.
 * No crea, modifica ni borra ninguna fila, así que no hay nombre que aislar con un
 * sufijo `Date.now()` ni nada que borrar en el teardown. Lo único que deja abierto es
 * la pantalla del asistente, y el `finally` la cierra con su botón "Cancelar" (el
 * mismo que el test acaba de comprobar) para que un run no herede la pestaña del
 * anterior. Tampoco hace falta pre-limpieza defensiva: el listado depende solo de los
 * datos maestros (trámites, tipos de usuario y permisos del estado inicial), que
 * ningún test modifica; un run que abortara no puede dejar nada pegado que cambie
 * estas aserciones.
 */

// Credenciales del usuario de la precondición (tabla «Usuarios de acceso» del .desc.md):
// profesor con cargo de Director en CIPFP Mislata, un solo centro.
const USUARIO = 'director@mislata.es';
const CONTRASENA = 'demo1234';

// Datos del estado inicial de la BD que el test da por sentados.
const CENTRO = 'CIPFP Mislata';
const TIPO_TRAMITE_PROFESOR = 'Trámites para el profesor';
const TIPO_TRAMITE_ALUMNO = 'Trámites para el alumno';
// Los dos trámites del tipo «Trámites para el profesor», EN ORDEN ALFABÉTICO: el
// orden del array es parte de la aserción (`toHaveText` compara posición a posición).
const TRAMITES_DEL_PROFESOR = ['Justificación de falta del profesorado', 'Trámite de prueba'];

// Títulos de las pantallas del asistente (los fijan los `action-view` de
// `system/ventanilla/views/`): la de elegir centro NO debe llegar a abrirse.
const PANTALLA_CENTRO = 'Nuevo expediente: elija el centro';
const PANTALLA_TRAMITE = 'Nuevo expediente: elija el trámite';

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

test.describe('Ventanilla — Nuevo expediente', () => {
  test('El profesor solo ve los trámites para el profesor', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el director `director@mislata.es` ha iniciado sesión con la
    // contraseña `demo1234`.
    await login(page, USUARIO, CONTRASENA);

    try {
      // Paso 2: Cuando abre el menú "Mis trámites" y pulsa "Nuevo trámite".
      await abrirNuevoExpediente(page);

      // Resultado esperado: el sistema no muestra el listado de centros y abre "Nuevo
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

      // Resultado esperado: el listado muestra ÚNICAMENTE el tipo de trámite
      // "Trámites para el profesor"…
      await desplegarGruposDeTramites(page);
      await expect(gruposDeTipoTramite(page)).toHaveCount(1);
      await expect(gruposDeTipoTramite(page).first()).toContainText(TIPO_TRAMITE_PROFESOR);
      // …y, dentro y POR ORDEN ALFABÉTICO, "Justificación de falta del profesorado" y
      // "Trámite de prueba". `toHaveText` con un array compara posición a posición, así
      // que verifica a la vez el contenido, el número de filas y el orden.
      await expect(filasDeTramites(page)).toHaveText(TRAMITES_DEL_PROFESOR);

      // Resultado esperado: no aparece "Trámites para el alumno" (ni como grupo del
      // listado ni en ningún otro sitio de la pantalla).
      await expect(page.getByText(TIPO_TRAMITE_ALUMNO)).toHaveCount(0);

      // Resultado esperado: debajo del listado hay un único botón, "Cancelar"; no hay
      // botón "Atrás".
      const botonera = page.getByTestId('panel:buttons-panel');
      await expect(botonera.getByRole('button')).toHaveCount(1);
      await expect(botonera.getByRole('button', { name: 'Cancelar' })).toBeVisible();
      await expect(botonera.getByRole('button', { name: 'Atrás' })).toHaveCount(0);
      // La botonera está debajo del listado, no encima.
      const cajaBotonera = await botonera.boundingBox();
      expect(cajaBotonera).not.toBeNull();
      expect(cajaBotonera!.y).toBeGreaterThan(cajaTramites!.y);
    } finally {
      // Teardown: cerrar el asistente aunque una aserción haya fallado, para no dejar
      // su pestaña abierta al siguiente run. Si el fallo fue antes de abrirlo, el
      // botón no existe: el `.catch` es intencional, no un error tapado.
      await page
        .getByTestId('panel:buttons-panel')
        .getByRole('button', { name: 'Cancelar' })
        .click({ timeout: 5000 })
        .catch(() => {});

      await logout(page);
    }
  });
});
