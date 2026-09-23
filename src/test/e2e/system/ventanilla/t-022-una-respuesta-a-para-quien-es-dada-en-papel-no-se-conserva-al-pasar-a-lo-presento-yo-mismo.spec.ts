import { test, expect, Locator, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-022 — Una respuesta a «¿Para quién es?» dada en papel no se conserva al pasar a
// «Lo presento yo mismo»
// origen: ESC-021  |  verifica: U-nuevo-expediente-009, U-nuevo-expediente-010, U-nuevo-expediente-011, U-nuevo-expediente-013, R-AsistenteNuevoExpediente-001, R-AsistenteNuevoExpediente-005
// fuente: .sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/test-e2e-desc/t-022-una-respuesta-a-para-quien-es-dada-en-papel-no-se-conserva-al-pasar-a-lo-presento-yo-mismo.desc.md

/**
 * IDEMPOTENCIA (§4 del contrato de generación) — este test CREA un expediente, y su
 * identificador lo asigna el servidor con un contador, así que NO se puede aislar con
 * un sufijo `Date.now()` en un nombre: no hay ningún nombre que el test elija. La
 * idempotencia se consigue de las otras dos formas:
 *   - el expediente creado se identifica por el título de SU pestaña, capturado en
 *     tiempo de ejecución justo después de crearlo (nunca por un número fijo), y
 *   - se BORRA en el `finally` con el botón "Borrar el expediente" que el propio
 *     estado inicial ofrece (perfil CREADOR — el expediente termina presentado por el
 *     propio usuario, no en papel, así que abre con ese perfil, igual que T-001/T-019 y
 *     a diferencia de T-011, que sí lo registra en papel y necesita reabrirlo desde
 *     "Expedientes Esperando").
 * No hace falta pre-limpieza defensiva: ninguna regla de negocio limita cuántas
 * anulaciones de matrícula puede tener el mismo alumno, así que un expediente residual
 * de un run que abortara no impide crear otro ni cambia ninguna aserción (el test no
 * cuenta expedientes, solo mira el que acaba de abrir).
 */

// Credenciales del usuario de la precondición (tabla «Usuarios de acceso» del .desc.md).
// administrativo2@mislata.es es el caso clave del escenario: en CIPFP Mislata es a la
// vez ADMINISTRATIVO y ALUMNO, así que sobre "Anulación de matrícula en ciclo
// formativo" (que SÍ admite representación) tiene las DOS formas de presentarlo
// (presentarlo él mismo como alumno, registrarlo en papel como administrativo) y el
// asistente le pregunta cómo se presenta. Es justo ese usuario el que permite marcar
// "en representación" mientras está en modo papel (paso 8) y comprobar que esa
// respuesta NO sobrevive al volver a "Lo presento yo mismo" (paso 9): como alumno y no
// familiar, presentándolo él mismo solo puede ser para sí mismo.
const USUARIO = 'administrativo2@mislata.es';
const CONTRASENA = 'demo1234';

// Datos del estado inicial de la BD que el test da por sentados.
const CENTRO = 'CIPFP Mislata';
const TRAMITE = 'Anulación de matrícula en ciclo formativo';
const TIPO_TRAMITE_ALUMNO = 'Trámites para el alumno';
const TIPO_TRAMITE_PROFESOR = 'Trámites para el profesor';

// Identidad de administrativo2 tal y como la carga `data-demo/input/usuarios-demo.xml`:
// es a la vez ADMINISTRATIVO y ALUMNO en CIPFP Mislata. `NOMBRE_COMPLETO` es con lo que
// la aplicación lo pinta en «Creado por»; nombre, apellidos y DNI son los que rellenan
// el panel "Datos del alumno" cuando el expediente se presenta por vía TELEMÁTICA (el
// caso de este test), igual que hace T-001 con alumno1.
const ADMINISTRATIVO_NOMBRE_COMPLETO = 'Administrativo2 CIPFP Mislata';
const ADMINISTRATIVO_NOMBRE = 'Administrativo2';
const ADMINISTRATIVO_APELLIDOS = 'CIPFP Mislata';
const ADMINISTRATIVO_DNI = '16493254T';

// Títulos de las TRES pantallas del asistente (los fijan los `action-view` de
// `system/ventanilla/views/`). Se usan para comprobar tanto que se abre la que toca
// como que al final NO queda ninguna abierta. Se expresan como expresiones regulares
// ANCLADAS con un `*` final opcional porque Axelor marca con un asterisco la pestaña
// cuyo formulario tiene cambios sin guardar: en cuanto el usuario toca un radio, "Nuevo
// expediente" pasa a llamarse "Nuevo expediente*".
const PANTALLA_CENTRO = /^Nuevo expediente: elija el centro\*?$/;
const PANTALLA_TRAMITE = /^Nuevo expediente: elija el trámite\*?$/;
const PANTALLA_CONTEXTO = /^Nuevo expediente\*?$/;

// Rótulos de las dos preguntas del último paso del asistente (los `title` de
// `presentadoEnPapel` y `presentadoEnRepresentacion` en
// `ElegirFormaPresentacion-AsistenteNuevoExpediente.xml`).
const PREGUNTA_COMO_SE_PRESENTA = '¿Cómo se presenta?';
const PREGUNTA_PARA_QUIEN = '¿Para quién es el expediente?';

// Las dos opciones de «¿Cómo se presenta?» (los `x-false-text`/`x-true-text` del widget
// `boolean-radio`): «Estoy registrando…» es `presentadoEnPapel = true`.
const OPCION_LO_PRESENTO_YO = 'Lo presento yo mismo';
const OPCION_EN_PAPEL = 'Estoy registrando un trámite recibido en papel';

// Las dos opciones de «¿Para quién es el expediente?»: «Para otra persona…» es
// `presentadoEnRepresentacion = true`.
const OPCION_PARA_MI = 'Para mí';
const OPCION_EN_REPRESENTACION =
  'Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)';

// Primer estado del tipo de expediente para quien lo presenta él mismo (perfil
// CREADOR), según `TipoExpedienteInstance.xml`: fase SOLICITUD, estado DATOS_SOLICITUD.
// Es justo el estado que este test tiene que alcanzar para probar que, pese a haber
// marcado antes "en papel" + "en representación", el expediente nace por la vía
// TELEMÁTICA (T-011 prueba la rama contraria, que sí queda en PENDIENTE_DOCUMENTO_ESCANEADO).
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
// ausencia es la prueba, en la UI, de que la respuesta "en representación" dada en el
// paso 8 (mientras estaba en modo papel) NO sobrevivió al paso 9 (cambiar a "Lo
// presento yo mismo"): si hubiera sobrevivido, este panel estaría visible.
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
 * El radio de una de las opciones de un `boolean-radio` del asistente. Axelor pinta cada
 * opción como `<div><input type="radio"><span>texto</span></div>`, SIN `<label>` (y con
 * el mismo `id` en los dos inputs), así que la opción no se puede localizar por su
 * nombre accesible: se localiza el `div` que contiene su texto y, dentro, su radio. Así
 * la aserción sigue atada al texto que lee el usuario, no a la posición ni al valor
 * interno (ojo: en pantalla la opción `true` va ANTES que la `false`).
 */
function opcionDeRadio(page: Page, campo: string, texto: string): Locator {
  return page
    .getByTestId(`field:${campo}`)
    .locator('div:has(> [data-testid="radio"])')
    .filter({ hasText: texto })
    .getByRole('radio');
}

/** El radio de una de las opciones de «¿Cómo se presenta?». */
function opcionComoSePresenta(page: Page, texto: string): Locator {
  return opcionDeRadio(page, 'presentadoEnPapel', texto);
}

/** El radio de una de las opciones de «¿Para quién es el expediente?». */
function opcionParaQuien(page: Page, texto: string): Locator {
  return opcionDeRadio(page, 'presentadoEnRepresentacion', texto);
}

/**
 * Espera a que termine el recálculo que el `onChange` de «¿Cómo se presenta?» dispara
 * contra el servidor (`…-onChange-presentadoEnPapel-action` → `recalcular`), que es
 * quien decide si hay que preguntar «¿Para quién es el expediente?» y quien, además,
 * fija en el mismo viaje el valor de `presentadoEnRepresentacion`
 * (`AsistenteNuevoExpedienteController.recalcular`, `value:set`).
 *
 * <p>**CRITICAL**: sin esta espera, las comprobaciones de los pasos 7 y 10 se harían
 * sobre la pantalla ANTERIOR a la respuesta del servidor. Se registra el
 * `waitForResponse` ANTES de pulsar el radio.
 */
function esperarRecalculo(page: Page): Promise<unknown> {
  return page.waitForResponse(
    (respuesta) =>
      respuesta.url().endsWith('/ws/action') &&
      (respuesta.request().postData() ?? '').includes(
        'AsistenteNuevoExpediente-onChange-presentadoEnPapel-action',
      ),
  );
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

/**
 * Borra el expediente cuya pestaña se titula `titulo`, pulsando "Borrar el expediente"
 * directamente sobre la pestaña recién abierta: al presentarlo él mismo el alta abre el
 * form con perfil CREADOR, que es el único que ofrece ese botón (evento DELETE), así
 * que no hace falta reabrir el expediente desde ningún listado (a diferencia de T-011,
 * que sí lo registra en papel). El botón abre un diálogo de confirmación de Axelor que
 * hay que aceptar; el evento DELETE responde con `refresh-app`, así que la aplicación
 * se recarga entera y la pestaña del expediente desaparece.
 */
async function borrarExpediente(page: Page, titulo: string): Promise<void> {
  await page.getByTestId('widget:DELETE').getByRole('button').click();
  await page.getByRole('button', { name: 'Aceptar' }).click();
  await expect(page.getByRole('tab', { name: titulo, exact: true })).toHaveCount(0);
}

test.describe('Ventanilla — Nuevo expediente', () => {
  test('Una respuesta a «¿Para quién es?» dada en papel no se conserva al pasar a «Lo presento yo mismo»', async ({
    page,
  }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el administrativo `administrativo2@mislata.es` (que en "CIPFP
    // Mislata" es además alumno) ha iniciado sesión con la contraseña `demo1234`.
    await login(page, USUARIO, CONTRASENA);

    // Título de la pestaña del expediente creado; se captura tras crearlo y lo usa el
    // teardown para borrarlo. Vacío mientras no exista el expediente.
    let tituloExpediente = '';

    try {
      // Paso 2: Cuando abre el menú "Ventanilla" y pulsa "Nuevo expediente".
      await abrirEntradaDeMenu(page, 'ventanilla-menuitem', 'ventanilla-nuevoExpediente-menuitem');

      // Paso 3: Entonces se abre DIRECTAMENTE "Nuevo expediente: elija el trámite" con
      // el centro "CIPFP Mislata"…
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE })).toBeVisible();
      // …"directamente" = sin pasar por la elección de centro: el administrativo solo
      // tiene un centro, así que ni se abre esa pantalla ni aparece su listado.
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO })).toHaveCount(0);
      await expect(page.getByTestId('panel:centrosPanel')).toHaveCount(0);

      const campoCentro = page.getByTestId('field:centro').getByRole('textbox');
      await expect(campoCentro).toHaveValue(CENTRO);
      await expect(campoCentro).toBeDisabled();

      // …únicamente "Trámites para el alumno" con "Anulación de matrícula en ciclo
      // formativo" dentro: aunque el usuario es administrativo Y alumno, los dos
      // papeles le dan permiso sobre el MISMO tipo de trámite, así que solo ve ese
      // grupo.
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

      // Paso 5: Entonces se abre "Nuevo expediente" con "¿Cómo se presenta?" visible y
      // sin marcar…
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO })).toBeVisible();
      await expect(page.getByText(PREGUNTA_COMO_SE_PRESENTA)).toBeVisible();
      await expect(page.getByTestId('field:presentadoEnPapel')).toBeVisible();
      const radioLoPresentoYo = opcionComoSePresenta(page, OPCION_LO_PRESENTO_YO);
      const radioEnPapel = opcionComoSePresenta(page, OPCION_EN_PAPEL);
      await expect(radioLoPresentoYo).toBeVisible();
      await expect(radioEnPapel).toBeVisible();
      await expect(radioLoPresentoYo).not.toBeChecked();
      await expect(radioEnPapel).not.toBeChecked();

      // …y SIN "¿Para quién es el expediente?": mientras no se conteste cómo se
      // presenta, no hay destinatario que preguntar. Se comprueba por su rótulo y por
      // el campo del modelo que la pinta (`presentadoEnRepresentacion`), para que el
      // test siga cazando el fallo aunque cambie el texto.
      await expect(page.getByText(PREGUNTA_PARA_QUIEN)).toHaveCount(0);
      await expect(page.getByTestId('field:presentadoEnRepresentacion')).toHaveCount(0);

      // Paso 6: Cuando marca "Estoy registrando un trámite recibido en papel".
      // El `onChange` va al servidor a recalcular qué hay que preguntar; se espera su
      // respuesta ANTES de comprobar nada (ver `esperarRecalculo`).
      const recalculoEnPapel = esperarRecalculo(page);
      await radioEnPapel.click();
      await recalculoEnPapel;

      // Paso 7: Entonces se muestra "¿Para quién es el expediente?" sin ninguna opción
      // marcada. Control positivo: la opción que acaba de marcar quedó marcada y la
      // otra no, o sea que el recálculo del servidor llegó y se aplicó.
      await expect(radioEnPapel).toBeChecked();
      await expect(radioLoPresentoYo).not.toBeChecked();
      await expect(page.getByText(PREGUNTA_PARA_QUIEN)).toBeVisible();
      await expect(page.getByTestId('field:presentadoEnRepresentacion')).toBeVisible();
      const radioParaMi = opcionParaQuien(page, OPCION_PARA_MI);
      const radioEnRepresentacion = opcionParaQuien(page, OPCION_EN_REPRESENTACION);
      await expect(radioParaMi).toBeVisible();
      await expect(radioEnRepresentacion).toBeVisible();
      await expect(radioParaMi).not.toBeChecked();
      await expect(radioEnRepresentacion).not.toBeChecked();

      // Paso 8: Cuando marca "Para otra persona a la que represento (hijo/a menor de
      // edad o persona tutelada)". Este campo no lleva `onChange` propio (no hay
      // recálculo del servidor que esperar): es una simple marca local.
      await radioEnRepresentacion.click();
      await expect(radioEnRepresentacion).toBeChecked();
      await expect(radioParaMi).not.toBeChecked();

      // Paso 9: Y cambia "¿Cómo se presenta?" a "Lo presento yo mismo".
      const recalculoTelematico = esperarRecalculo(page);
      await radioLoPresentoYo.click();
      await recalculoTelematico;

      // Paso 10: Entonces deja de verse "¿Para quién es el expediente?" (es alumno y
      // no es familiar: el expediente es para él mismo). Control positivo de esta
      // ausencia: la opción que acaba de marcar quedó marcada y la otra no, o sea que
      // el panel de la presentación sigue vivo y el recálculo del servidor se aplicó.
      await expect(radioLoPresentoYo).toBeChecked();
      await expect(radioEnPapel).not.toBeChecked();
      await expect(page.getByText(PREGUNTA_COMO_SE_PRESENTA)).toBeVisible();
      await expect(page.getByText(PREGUNTA_PARA_QUIEN)).toHaveCount(0);
      await expect(page.getByTestId('field:presentadoEnRepresentacion')).toHaveCount(0);

      // Paso 11: Cuando pulsa "Crear expediente".
      await page.getByRole('button', { name: 'Crear expediente' }).click();

      // Resultado esperado: el asistente se cierra y se abre el expediente recién
      // creado de "Anulación de matrícula en ciclo formativo" en su primer estado, en
      // el centro "CIPFP Mislata".
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
      await expect(page.getByTestId('field:nombreCentro').getByRole('textbox')).toHaveValue(
        CENTRO,
      );

      // El asistente se cierra: no queda visible ninguna de sus TRES pantallas, ni por
      // su pestaña ni por sus paneles y botones. Ninguna de estas ausencias es vacua:
      // son las mismas pestañas, paneles y botones que este test ha ido viendo en los
      // pasos 3 a 11.
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO })).toHaveCount(0);
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE })).toHaveCount(0);
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO })).toHaveCount(0);
      await expect(page.getByTestId('panel:centrosPanel')).toHaveCount(0);
      await expect(page.getByTestId('panel:tramitesPanel')).toHaveCount(0);
      await expect(page.getByTestId('panel:tramitePanel')).toHaveCount(0);
      await expect(page.getByTestId('panel:presentacionPanel')).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Crear expediente' })).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Cancelar' })).toHaveCount(0);

      // Resultado esperado: el expediente queda presentado por el propio usuario…
      // (NO registrado como presentado en papel). En la UI eso se ve en las tres
      // bifurcaciones que la vista del estado hace sobre `presentadoEnPapel`: el aviso
      // de firma con certificado, la ausencia del panel de la solicitud escaneada y la
      // botonera del que aún tiene que rellenar y firmar ("Siguiente" + "Borrar el
      // expediente" con perfil CREADOR, sin "Presentar la solicitud" ni "Salir").
      await expect(page.getByText(AVISO_PRESENTA_EL_MISMO)).toBeVisible();
      await expect(page.getByTestId(PANEL_SOLICITUD_EN_PAPEL)).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Siguiente' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Borrar el expediente' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Presentar la solicitud' })).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Salir' })).toHaveCount(0);
      await expect(page.getByTestId('field:createdBy').getByRole('textbox')).toHaveValue(
        ADMINISTRATIVO_NOMBRE_COMPLETO,
      );

      // Resultado esperado: …y para él mismo: no queda en representación de otra
      // persona. Esta es la comprobación CLAVE del escenario — la respuesta "en
      // representación" que marcó en el paso 8, mientras estaba en modo papel, NO
      // sobrevivió al cambiar a "Lo presento yo mismo" en el paso 9. Se ve en la UI
      // por partida doble:
      //
      // 1) el panel "Persona que presenta la solicitud" solo se pinta cuando hay
      //    representación (`showIf="presentadoEnRepresentacion"`), así que su AUSENCIA
      //    es la prueba directa de que el campo quedó en `false`, no en el `true` que
      //    el usuario había marcado. La ausencia no es vacua: ese mismo panel es el
      //    que se pintaría si la respuesta del paso 8 se hubiera conservado — es
      //    justo el fallo que este test existe para cazar.
      await expect(page.getByTestId(PANEL_PERSONA_SOLICITANTE)).toHaveCount(0);
      await expect(page.getByText('Persona que presenta la solicitud')).toHaveCount(0);
      //
      // 2) el panel propio del trámite "Datos del alumno" nace PRERRELLENO y EN SOLO
      //    LECTURA con la identidad del propio administrativo2 (el sistema ya sabe
      //    quién es, no hay que preguntárselo) — si el expediente hubiera quedado en
      //    representación de otra persona, estos datos no coincidirían con los suyos
      //    propios.
      const panelAlumno = page.getByTestId('panel:alumno');
      await expect(panelAlumno).toBeVisible();
      const apellidosAlumno = panelAlumno.getByRole('textbox', { name: 'Apellidos' });
      const nombreAlumno = panelAlumno.getByRole('textbox', { name: 'Nombre' });
      const dniAlumno = panelAlumno.getByRole('textbox', { name: 'DNI/NIE' });
      await expect(apellidosAlumno).toHaveValue(ADMINISTRATIVO_APELLIDOS);
      await expect(nombreAlumno).toHaveValue(ADMINISTRATIVO_NOMBRE);
      await expect(dniAlumno).toHaveValue(ADMINISTRATIVO_DNI);
      await expect(apellidosAlumno).toBeDisabled();
      await expect(nombreAlumno).toBeDisabled();
      await expect(dniAlumno).toBeDisabled();
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
