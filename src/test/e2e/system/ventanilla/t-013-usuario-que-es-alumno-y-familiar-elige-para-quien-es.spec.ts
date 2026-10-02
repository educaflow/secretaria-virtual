import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-013 — Usuario que es alumno y familiar elige para quién es
// origen: ESC-013  |  verifica: V-AsistenteNuevoExpediente-003, U-nuevo-expediente-009, U-nuevo-expediente-010, U-nuevo-expediente-013, R-AsistenteNuevoExpediente-001, R-AsistenteNuevoExpediente-005
// fuente: .sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/test-e2e-desc/t-013-usuario-que-es-alumno-y-familiar-elige-para-quien-es.desc.md

/**
 * IDEMPOTENCIA (§4 del contrato de generación) — este test CREA un expediente, y su
 * identificador (`0000N/2026`) lo asigna el servidor con un contador, así que NO se
 * puede aislar con un sufijo `Date.now()` en un nombre: no hay ningún nombre que el
 * test elija. La idempotencia se consigue de las otras dos formas:
 *   - el expediente creado se identifica por el título de SU pestaña, capturado en
 *     tiempo de ejecución justo después de crearlo (nunca por un número fijo), y
 *   - se BORRA en el `finally` con el botón "Borrar el expediente" que el propio
 *     estado inicial ofrece, de modo que la BD compartida queda como estaba.
 * El intento fallido del paso 6 (pulsar "Crear expediente" sin marcar "¿Para quién es
 * el expediente?") no crea NINGÚN expediente —es justo lo que el paso 7 comprueba—,
 * así que no deja basura propia que limpiar.
 * No hace falta pre-limpieza defensiva: ninguna regla de negocio limita cuántos
 * expedientes de este trámite puede tener el usuario, así que un expediente residual
 * de un run que abortara no impide crear otro ni cambia ninguna aserción (el test no
 * cuenta expedientes, solo mira el que acaba de abrir).
 */

// Credenciales del usuario de la precondición (tabla «Usuarios de acceso» del .desc.md):
// alumnofamiliar@mislata.es es a la vez Alumno y Familiar en el MISMO centro, el caso
// clave de este escenario: al ser alumno, el trámite podría ser para él mismo; al ser
// también familiar (y el trámite admitir representación), podría ser en representación
// de otra persona. Por eso, a diferencia de T-001/T-012, aquí SÍ hay que preguntar
// "¿Para quién es el expediente?" — pero NO "¿Cómo se presenta?": solo tiene una forma
// de iniciar este trámite, presentándolo él mismo (no tiene permiso para registrarlo en
// papel).
const USUARIO = 'alumnofamiliar@mislata.es';
const CONTRASENA = 'demo1234';

// Datos del estado inicial de la BD que el test da por sentados.
const CENTRO = 'CIPFP Mislata';
const TRAMITE = 'Anulación de matrícula en ciclo formativo';
const TIPO_TRAMITE_ALUMNO = 'Trámites para el alumno';
const TIPO_TRAMITE_PROFESOR = 'Trámites para el profesor';

// Identidad del usuario tal y como la carga `data-demo/input/usuarios-demo.xml`: al
// marcar "Para mí" es quien queda como interesado (alumno) del expediente, así que sus
// datos deben aparecer prerrellenados y en solo lectura en el panel "Alumno/a al que se
// refiere la solicitud".
const NOMBRE = 'AlumnoFamiliar';
const APELLIDOS = 'CIPFP Mislata';
const DNI = '48213579J';
const NOMBRE_COMPLETO = 'AlumnoFamiliar CIPFP Mislata';

// Títulos de las TRES pantallas del asistente (los fijan los `action-view` de
// `system/ventanilla/views/`). Se usan para comprobar tanto que se abre la que toca
// como que al final NO queda ninguna abierta. Se expresan como expresiones regulares
// con un `*` final opcional porque, en cuanto el usuario toca un radio, Axelor marca
// con un asterisco la pestaña cuyo formulario tiene cambios sin guardar (ver T-011).
const PANTALLA_CENTRO = /^Nuevo expediente: elija el centro\*?$/;
const PANTALLA_TRAMITE = /^Nuevo expediente: elija el trámite\*?$/;
const PANTALLA_CONTEXTO = /^Nuevo expediente\*?$/;

// Rótulo de la única pregunta que este escenario muestra (el `title` de
// `presentadoEnRepresentacion` en `Main-AsistenteNuevoExpediente.xml`).
const PREGUNTA_PARA_QUIEN = '¿Para quién es el expediente?';

// Las dos opciones de «¿Para quién es el expediente?» (los `x-false-text`/`x-true-text`
// del widget `boolean-radio`): «Para otra persona…» es `presentadoEnRepresentacion = true`.
const OPCION_PARA_MI = 'Para mí';
const OPCION_EN_REPRESENTACION =
  'Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)';

// Mensaje del `<action-validate>` local del form cuando no se contesta "¿Para quién es
// el expediente?" (el mismo que T-011 ve al no contestar ninguna de las dos preguntas).
const ERROR_FALTA_PARA_QUIEN = 'Debe indicar para quién es el expediente';

// Primer estado del tipo de expediente para quien lo presenta él mismo (perfil
// CREADOR), según `TipoExpedienteInstance.xml`: fase ENTRADA, estado ENTRADA_DATOS.
const FASE_INICIAL = 'Entrada';
const ESTADO_INICIAL = 'Entrada de datos';

// Aviso que la vista del estado pinta con `showIf="!presentadoEnPapel"`: verlo es la
// prueba en la UI de que el expediente NO quedó marcado como presentado en papel.
const AVISO_PRESENTA_EL_MISMO =
  'Para presentar la solicitud necesitará firmarla con su certificado digital desde este mismo ordenador';

// Panel que la vista pinta con `showIf="presentadoEnPapel"`: su ausencia es la otra
// cara de la misma comprobación.
const PANEL_SOLICITUD_EN_PAPEL = 'panel:solicitudEscaneadaDatosSolicitud';

// Panel que la plantilla común pinta con `showIf="presentadoEnRepresentacion"`
// (`persona-solicitante-editable` de `tramites/shared/template-views.xml`): su ausencia
// prueba que el expediente es para el propio usuario, no en representación de otro.
const PANEL_PERSONA_SOLICITANTE = 'panel:persona-solicitante-editable';

// Panel propio del trámite ("Alumno/a al que se refiere la solicitud", `datos-alumno`
// de `tramites/alumnos/.../v1/views.xml`) que identifica al interesado (el alumno).
const PANEL_ALUMNO = 'panel:alumno';

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
 * El radio de una de las opciones de «¿Para quién es el expediente?». Axelor pinta cada
 * opción como `<div><input type="radio"><span>texto</span></div>`, SIN `<label>` (y con
 * el mismo `id` en los dos inputs), así que la opción no se puede localizar por su
 * nombre accesible: se localiza el `div` que contiene su texto y, dentro, su radio. Así
 * la aserción sigue atada al texto que lee el usuario, no a la posición ni al valor
 * interno (ojo: en pantalla la opción `true` va ANTES que la `false`).
 */
function opcionParaQuien(page: Page, texto: string) {
  return page
    .getByTestId('field:presentadoEnRepresentacion')
    .locator('div:has(> [data-testid="radio"])')
    .filter({ hasText: texto })
    .getByRole('radio');
}

/**
 * Borra el expediente cuya pestaña se titula `titulo`. El botón abre un diálogo de
 * confirmación de Axelor ("Se va a eliminar el expediente y no podrá recuperarlo") que
 * hay que aceptar. Al borrarlo la aplicación se recarga entera.
 */
async function borrarExpediente(page: Page, titulo: string): Promise<void> {
  await page.getByTestId('widget:DELETE').getByRole('button').click();
  await page.getByRole('button', { name: 'Aceptar' }).click();
  await expect(page.getByRole('tab', { name: titulo, exact: true })).toHaveCount(0);
}

test.describe('Ventanilla — Nuevo expediente', () => {
  test('Usuario que es alumno y familiar elige para quién es', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el usuario `alumnofamiliar@mislata.es` ha iniciado sesión con
    // la contraseña `demo1234`.
    await login(page, USUARIO, CONTRASENA);

    // Título de la pestaña del expediente creado; se captura tras crearlo y lo usa el
    // teardown para borrarlo. Vacío mientras no exista el expediente.
    let tituloExpediente = '';

    try {
      // Paso 2: Cuando abre el menú "Ventanilla" y pulsa "Nuevo expediente".
      await abrirNuevoExpediente(page);

      // Paso 3: Entonces se abre directamente "Nuevo expediente: elija el trámite"…
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE })).toBeVisible();
      // …directamente = sin pasar por la elección de centro: el usuario solo tiene un
      // centro, así que ni se abre esa pantalla ni aparece su listado.
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO })).toHaveCount(0);
      await expect(page.getByTestId('panel:centrosPanel')).toHaveCount(0);

      // …con el centro "CIPFP Mislata" encima del listado.
      const campoCentro = page.getByTestId('field:centro').getByRole('textbox');
      await expect(campoCentro).toHaveValue(CENTRO);
      await expect(campoCentro).toBeDisabled();
      const cajaCentro = await page.getByTestId('panel:centroPanel').boundingBox();
      const cajaTramites = await page.getByTestId('panel:tramitesPanel').boundingBox();
      expect(cajaCentro).not.toBeNull();
      expect(cajaTramites).not.toBeNull();
      expect(cajaCentro!.y).toBeLessThan(cajaTramites!.y);

      // …únicamente "Trámites para el alumno" con "Anulación de matrícula en ciclo
      // formativo" dentro.
      await desplegarGruposDeTramites(page);
      await expect(filasDeTramites(page)).toHaveCount(1);
      await expect(filasDeTramites(page).first()).toHaveText(TRAMITE);
      await expect(gruposDeTipoTramite(page)).toHaveCount(1);
      await expect(gruposDeTipoTramite(page).first()).toContainText(TIPO_TRAMITE_ALUMNO);
      // …no aparece "Trámites para el profesor".
      await expect(page.getByText(TIPO_TRAMITE_PROFESOR)).toHaveCount(0);

      // …y un único botón debajo, "Cancelar".
      const botonera = page.getByTestId('panel:buttons-panel');
      await expect(botonera.getByRole('button')).toHaveCount(1);
      await expect(botonera.getByRole('button', { name: 'Cancelar' })).toBeVisible();
      await expect(botonera.getByRole('button', { name: 'Atrás' })).toHaveCount(0);

      // Paso 4: Cuando pulsa la fila "Anulación de matrícula en ciclo formativo".
      await filasDeTramites(page).first().click();

      // Paso 5: Entonces se abre "Nuevo expediente" SIN "¿Cómo se presenta?"…
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO })).toBeVisible();
      await expect(page.getByText('¿Cómo se presenta?')).toHaveCount(0);
      await expect(page.getByTestId('field:presentadoEnPapel')).toHaveCount(0);

      // …y CON "¿Para quién es el expediente?" visible, con "Para mí" y "Para otra
      // persona a la que represento (hijo/a menor de edad o persona tutelada)", sin
      // ninguna marcada.
      await expect(page.getByText(PREGUNTA_PARA_QUIEN)).toBeVisible();
      await expect(page.getByTestId('field:presentadoEnRepresentacion')).toBeVisible();
      const radioParaMi = opcionParaQuien(page, OPCION_PARA_MI);
      const radioEnRepresentacion = opcionParaQuien(page, OPCION_EN_REPRESENTACION);
      await expect(radioParaMi).toBeVisible();
      await expect(radioEnRepresentacion).toBeVisible();
      await expect(radioParaMi).not.toBeChecked();
      await expect(radioEnRepresentacion).not.toBeChecked();

      // Paso 6: Cuando pulsa "Crear expediente" sin marcar ninguna opción.
      await page.getByRole('button', { name: 'Crear expediente' }).click();

      // Paso 7: Entonces el sistema muestra "Debe indicar para quién es el
      // expediente"…
      const aviso = page.getByRole('dialog');
      await expect(aviso).toBeVisible();
      await expect(aviso).toContainText(ERROR_FALTA_PARA_QUIEN);

      // …no crea ningún expediente: no se abre su pestaña (la misma que sí aparece al
      // final de este test cuando el alta funciona, así que esta ausencia no puede ser
      // vacua).
      await expect(page.getByRole('tab', { name: TITULO_EXPEDIENTE })).toHaveCount(0);
      await aviso.getByRole('button', { name: 'Aceptar' }).click();
      await expect(page.getByRole('dialog')).toHaveCount(0);

      // …y "Nuevo expediente" sigue abierta, con la pregunta aún visible y sin marcar.
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO })).toBeVisible();
      await expect(page.getByText(PREGUNTA_PARA_QUIEN)).toBeVisible();
      await expect(radioParaMi).not.toBeChecked();
      await expect(radioEnRepresentacion).not.toBeChecked();
      await expect(page.getByRole('button', { name: 'Crear expediente' })).toBeVisible();

      // Paso 8: Cuando marca "Para mí" y pulsa "Crear expediente".
      await radioParaMi.click();
      // El expediente se crea con lo que estaba marcado en ese instante: sin esta
      // comprobación, un radio que no llegara a marcarse dejaría pasar el test.
      await expect(radioParaMi).toBeChecked();
      await expect(radioEnRepresentacion).not.toBeChecked();
      await page.getByRole('button', { name: 'Crear expediente' }).click();

      // Resultado esperado: el asistente se cierra y se abre el expediente recién
      // creado de "Anulación de matrícula en ciclo formativo" en su primer estado.
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

      // El asistente se cierra: no queda visible ninguna de sus TRES pantallas, ni por
      // su pestaña ni por sus paneles y botones.
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO })).toHaveCount(0);
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE })).toHaveCount(0);
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO })).toHaveCount(0);
      await expect(page.getByTestId('panel:centrosPanel')).toHaveCount(0);
      await expect(page.getByTestId('panel:tramitesPanel')).toHaveCount(0);
      await expect(page.getByTestId('panel:tramitePanel')).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Crear expediente' })).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Cancelar' })).toHaveCount(0);

      // …en el centro "CIPFP Mislata".
      await expect(page.getByTestId('field:nombreCentro').getByRole('textbox')).toHaveValue(
        CENTRO,
      );

      // Resultado esperado: el expediente queda presentado por el propio usuario (NO
      // registrado como presentado en papel). En la UI eso se ve en las tres
      // bifurcaciones que la vista del estado hace sobre `presentadoEnPapel`: el aviso
      // de firma con certificado, la ausencia del panel de la solicitud escaneada y la
      // botonera del que aún tiene que rellenar y firmar ("Siguiente", sin "Presentar
      // la solicitud" ni "Atrás").
      await expect(page.getByText(AVISO_PRESENTA_EL_MISMO)).toBeVisible();
      await expect(page.getByTestId(PANEL_SOLICITUD_EN_PAPEL)).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Siguiente' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Presentar la solicitud' })).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Atrás' })).toHaveCount(0);
      // Y lo crea el propio usuario, identificado por su sesión.
      await expect(page.getByTestId('field:createdBy').getByRole('textbox')).toHaveValue(
        NOMBRE_COMPLETO,
      );

      // Resultado esperado: y para él mismo, NO en representación de otra persona. El
      // panel "Persona que presenta la solicitud" solo se pinta cuando hay
      // representación, así que su ausencia prueba que el expediente es para el propio
      // usuario; y la persona interesada (panel "Alumno/a al que se refiere la
      // solicitud") es él mismo, prerrellena y en solo lectura.
      await expect(page.getByTestId(PANEL_PERSONA_SOLICITANTE)).toHaveCount(0);
      await expect(page.getByText('Persona que presenta la solicitud')).toHaveCount(0);
      const panelAlumno = page.getByTestId(PANEL_ALUMNO);
      await expect(panelAlumno).toBeVisible();
      const campoApellidosAlumno = panelAlumno.getByRole('textbox', { name: 'Apellidos' });
      const campoNombreAlumno = panelAlumno.getByRole('textbox', { name: 'Nombre' });
      const campoDniAlumno = panelAlumno.getByRole('textbox', { name: 'DNI/NIE' });
      await expect(campoApellidosAlumno).toHaveValue(APELLIDOS);
      await expect(campoNombreAlumno).toHaveValue(NOMBRE);
      await expect(campoDniAlumno).toHaveValue(DNI);
      await expect(campoApellidosAlumno).toBeDisabled();
      await expect(campoNombreAlumno).toBeDisabled();
      await expect(campoDniAlumno).toBeDisabled();
    } finally {
      // Teardown: borrar el expediente creado aunque una aserción haya fallado — la BD
      // es compartida y no se resetea, así que dejarlo lo acumularía run tras run. Si
      // el test falló ANTES de crearlo no hay nada que borrar.
      if (tituloExpediente !== '') {
        await borrarExpediente(page, tituloExpediente);
      }

      await logout(page);
    }
  });
});
