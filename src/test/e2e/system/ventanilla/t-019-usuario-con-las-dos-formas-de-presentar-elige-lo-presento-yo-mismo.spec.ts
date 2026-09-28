import { test, expect, Locator, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-019 — Usuario con las dos formas de presentar elige «Lo presento yo mismo»
// origen: ESC-018  |  verifica: U-nuevo-expediente-006, U-nuevo-expediente-009, U-nuevo-expediente-011, U-nuevo-expediente-013, R-AsistenteNuevoExpediente-001, R-AsistenteNuevoExpediente-005
// fuente: .sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/test-e2e-desc/t-019-usuario-con-las-dos-formas-de-presentar-elige-lo-presento-yo-mismo.desc.md

/**
 * IDEMPOTENCIA (§4 del contrato de generación) — este test CREA un expediente, y su
 * identificador (`0000N/2026`) lo asigna el servidor con un contador, así que NO se
 * puede aislar con un sufijo `Date.now()` en un nombre: no hay ningún nombre que el
 * test elija. La idempotencia se consigue de las otras dos formas:
 *   - el expediente creado se identifica por el título de SU pestaña, capturado en
 *     tiempo de ejecución justo después de crearlo (nunca por un número fijo), y
 *   - se BORRA en el `finally` con el botón "Borrar el expediente" que el propio
 *     estado inicial ofrece (perfil CREADOR — quien lo presenta él mismo, a
 *     diferencia de T-010 que lo registra en papel y abre perfil TRAMITADOR sin ese
 *     botón), de modo que la BD compartida queda como estaba.
 * No hace falta pre-limpieza defensiva: ninguna regla de negocio limita cuántas
 * justificaciones de falta puede tener el jefe de estudios, así que un expediente
 * residual de un run que abortara no impide crear otro ni cambia ninguna aserción (el
 * test no cuenta expedientes, solo mira el que acaba de abrir).
 */

// Credenciales del usuario de la precondición (tabla «Usuarios de acceso» del .desc.md).
// jefeestudios1@mislata.es es el caso clave del escenario: sobre los trámites del
// profesor tiene AMBAS formas de iniciarlos —como profesor puede presentarlos él mismo
// y como jefe de estudios puede registrarlos en papel—, así que el asistente le
// pregunta cómo se presenta. Este test comprueba la rama en la que elige presentarlo
// él mismo (la rama complementaria, registrarlo en papel, la cubre T-010).
const USUARIO = 'jefeestudios1@mislata.es';
const CONTRASENA = 'demo1234';

// Datos del estado inicial de la BD que el test da por sentados.
const CENTRO = 'CIPFP Mislata';
const TRAMITE = 'Justificación de falta del profesorado';
const TIPO_TRAMITE_PROFESOR = 'Trámites para el profesor';
const TIPO_TRAMITE_ALUMNO = 'Trámites para el alumno';
// Los dos trámites del tipo «Trámites para el profesor», EN ORDEN ALFABÉTICO: el orden
// del array es parte de la aserción (`toHaveText` compara posición a posición).
const TRAMITES_DEL_PROFESOR = [TRAMITE, 'Trámite de prueba'];

// Identidad del jefe de estudios tal y como la carga `data-demo/input/usuarios-demo.xml`.
// `NOMBRE_COMPLETO` es con lo que la aplicación lo pinta en «Creado por»; nombre,
// apellidos y DNI son los que `Tramitador.updatePersonas` copia en la persona
// interesada cuando el expediente se presenta por vía telemática (este test), a
// diferencia de T-010 (en papel), donde esos tres campos nacen vacíos.
const JEFE_NOMBRE_COMPLETO = 'JefeEstudios1 CIPFP Mislata';
const JEFE_NOMBRE = 'JefeEstudios1';
const JEFE_APELLIDOS = 'CIPFP Mislata';
const JEFE_DNI = '15519084H';

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

// Las dos opciones de «¿Cómo se presenta?» (los `x-false-text`/`x-true-text` del widget
// `boolean-radio`): «Lo presento yo mismo» es `presentadoEnPapel = false`.
const OPCION_LO_PRESENTO_YO = 'Lo presento yo mismo';
const OPCION_EN_PAPEL = 'Estoy registrando un trámite recibido en papel';

// Estado en el que nace el expediente según `InitialEventManagerImpl`: fase RECEPCION,
// estado ENTRADA_DATOS. Son los `title` del `TipoExpedienteInstance.xml`.
const FASE_INICIAL = 'Recepción';
const ESTADO_INICIAL = 'Entrada de datos';

// Panel del form plantilla del tipo (`views.xml` de la raíz de la versión) con los datos
// de la persona interesada. Está en el `<include-panels>` de los DOS forms de
// ENTRADA_DATOS, así que se pinta se mire con el perfil que se mire.
const PANEL_DATOS_PROFESOR = 'panel:datos-profesor';

// Panel que la plantilla común pinta con `showIf="presentadoEnRepresentacion"`
// (`persona-solicitante-editable` de `tramites/shared/template-views.xml`): su
// ausencia es la prueba en la UI de que el expediente NO es en representación de otra
// persona (este trámite, de hecho, ni siquiera lo permite:
// `permitidoPresentarEnRepresentacion=false` en su `TramiteInstance.xml`).
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
 * El radio de una de las opciones de «¿Cómo se presenta?». El widget `boolean-radio` de
 * Axelor pinta cada opción como `<div><input type="radio"><span>texto</span></div>`, SIN
 * `<label>` (y con el mismo `id` en los dos inputs), así que la opción no se puede
 * localizar por su nombre accesible: se localiza el `div` que contiene su texto y,
 * dentro, su radio. Así la aserción sigue atada al texto que lee el usuario, no a la
 * posición ni al valor interno.
 */
function opcionComoSePresenta(page: Page, texto: string): Locator {
  return page
    .getByTestId('field:presentadoEnPapel')
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

/**
 * Borra el expediente cuya pestaña se titula `titulo`, pulsando "Borrar el expediente"
 * directamente sobre la pestaña recién abierta: al presentarlo él mismo el alta abre el
 * form con perfil CREADOR, que es el único que ofrece ese botón (evento DELETE), así
 * que —a diferencia de T-010 (registrado en papel)— no hace falta reabrir el
 * expediente desde ningún listado. El botón abre un diálogo de confirmación de Axelor
 * que hay que aceptar; el evento DELETE responde con `refresh-app`, así que la
 * aplicación se recarga entera y la pestaña del expediente desaparece.
 */
async function borrarExpediente(page: Page, titulo: string): Promise<void> {
  await page.getByTestId('widget:DELETE').getByRole('button').click();
  await page.getByRole('button', { name: 'Aceptar' }).click();
  await expect(page.getByRole('tab', { name: titulo, exact: true })).toHaveCount(0);
}

test.describe('Ventanilla — Nuevo expediente', () => {
  test('Usuario con las dos formas de presentar elige «Lo presento yo mismo»', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el jefe de estudios `jefeestudios1@mislata.es` ha iniciado
    // sesión con la contraseña `demo1234`.
    await login(page, USUARIO, CONTRASENA);

    // Título de la pestaña del expediente creado; se captura tras crearlo y lo usa el
    // teardown para borrarlo. Vacío mientras no exista el expediente.
    let tituloExpediente = '';

    try {
      // Paso 2: Cuando abre el menú "Mis trámites" y pulsa "Nuevo trámite".
      await abrirEntradaDeMenu(page, 'misTramites-menuitem', 'misTramites-nuevoTramite-menuitem');

      // Paso 3: Entonces se abre DIRECTAMENTE "Nuevo expediente: elija el trámite"…
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE, exact: true })).toBeVisible();
      // …"directamente" = sin pasar por la elección de centro: el jefe de estudios solo
      // tiene un centro, así que ni se abre esa pantalla ni aparece su listado.
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO, exact: true })).toHaveCount(0);
      await expect(page.getByTestId('panel:centrosPanel')).toHaveCount(0);

      // …con el centro "CIPFP Mislata".
      const campoCentro = page.getByTestId('field:centro').getByRole('textbox');
      await expect(campoCentro).toHaveValue(CENTRO);
      await expect(campoCentro).toBeDisabled();

      // …y únicamente "Trámites para el profesor" con "Justificación de falta del
      // profesorado" y "Trámite de prueba" POR ORDEN ALFABÉTICO. `toHaveText` con un
      // array compara posición a posición: verifica contenido, número de filas y orden.
      await desplegarGruposDeTramites(page);
      await expect(gruposDeTipoTramite(page)).toHaveCount(1);
      await expect(gruposDeTipoTramite(page).first()).toContainText(TIPO_TRAMITE_PROFESOR);
      await expect(filasDeTramites(page)).toHaveText(TRAMITES_DEL_PROFESOR);
      await expect(page.getByText(TIPO_TRAMITE_ALUMNO)).toHaveCount(0);

      // …y un único botón debajo, "Cancelar".
      const botonera = page.getByTestId('panel:buttons-panel');
      await expect(botonera.getByRole('button')).toHaveCount(1);
      await expect(botonera.getByRole('button', { name: 'Cancelar' })).toBeVisible();

      // Paso 4: Cuando pulsa la fila "Justificación de falta del profesorado".
      await filasDeTramites(page).filter({ hasText: TRAMITE }).click();

      // Paso 5: Entonces se abre "Nuevo expediente" con ese trámite, su ayuda y el
      // centro en solo lectura; se ve "¿Cómo se presenta?" con sus dos opciones, SIN
      // ninguna marcada.
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO, exact: true })).toBeVisible();
      const campoTramite = page.getByTestId('field:nombreTramite').getByRole('textbox');
      await expect(campoTramite).toHaveValue(TRAMITE);
      await expect(campoTramite).toBeDisabled();
      const centroContexto = page.getByTestId('field:centro').getByRole('textbox');
      await expect(centroContexto).toHaveValue(CENTRO);
      await expect(centroContexto).toBeDisabled();
      await expect(page.getByTestId('field:ayudaTramite')).toContainText(
        'Este trámite permite a justificar la falta del profesorado',
      );

      await expect(page.getByText(PREGUNTA_COMO_SE_PRESENTA)).toBeVisible();
      await expect(page.getByTestId('field:presentadoEnPapel')).toBeVisible();
      const radioLoPresentoYo = opcionComoSePresenta(page, OPCION_LO_PRESENTO_YO);
      const radioEnPapel = opcionComoSePresenta(page, OPCION_EN_PAPEL);
      await expect(radioLoPresentoYo).toBeVisible();
      await expect(radioEnPapel).toBeVisible();
      await expect(radioLoPresentoYo).not.toBeChecked();
      await expect(radioEnPapel).not.toBeChecked();

      // …y NO se ve "¿Para quién es el expediente?": el trámite no admite
      // representación. Se comprueba por su rótulo y por el campo del modelo que la
      // pinta (`presentadoEnRepresentacion`), para que el test siga cazando el fallo
      // aunque cambie el texto.
      await expect(page.getByText(PREGUNTA_PARA_QUIEN)).toHaveCount(0);
      await expect(page.getByTestId('field:presentadoEnRepresentacion')).toHaveCount(0);

      // Paso 6: Cuando marca "Lo presento yo mismo".
      // El `onChange` del campo va al servidor (`…-onChange-presentadoEnPapel-action`)
      // a recalcular qué hay que preguntar. Se espera esa respuesta ANTES de comprobar
      // el paso 7, para no comprobar sobre la pantalla previa al recálculo.
      const recalculo = page.waitForResponse(
        (respuesta) =>
          respuesta.url().endsWith('/ws/action') &&
          (respuesta.request().postData() ?? '').includes(
            'AsistenteNuevoExpediente-onChange-presentadoEnPapel-action',
          ),
      );
      await radioLoPresentoYo.click();
      await recalculo;

      // Paso 7: Entonces sigue sin verse "¿Para quién es el expediente?", porque el
      // trámite no admite representación. Control positivo de esa ausencia: la opción
      // que el usuario acaba de marcar quedó marcada y la otra no, o sea que el panel
      // de la presentación sigue vivo y recalculado.
      await expect(radioLoPresentoYo).toBeChecked();
      await expect(radioEnPapel).not.toBeChecked();
      await expect(page.getByText(PREGUNTA_PARA_QUIEN)).toHaveCount(0);
      await expect(page.getByTestId('field:presentadoEnRepresentacion')).toHaveCount(0);

      // Paso 8: Cuando pulsa "Crear expediente".
      await page.getByRole('button', { name: 'Crear expediente' }).click();

      // Resultado esperado: el asistente se cierra y se abre el expediente recién
      // creado de "Justificación de falta del profesorado" en su primer estado (fase
      // RECEPCION, estado ENTRADA_DATOS, los que fija `InitialEventManagerImpl`).
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

      // …en el centro "CIPFP Mislata". Este tipo de expediente no repite un campo de
      // centro en la pantalla del expediente creado, así que el centro queda probado
      // donde SÍ es observable: en las dos pantallas del asistente (Pasos 3 y 5,
      // arriba) con el campo "Centro" en solo lectura fijado a "CIPFP Mislata" — valor
      // que no puede cambiar entre elegir el trámite y crear el expediente porque el
      // propio campo está deshabilitado.

      // Resultado esperado: el expediente queda PRESENTADO POR EL PROPIO JEFE DE
      // ESTUDIOS (NO registrado como presentado en papel). Se ve en la UI en dos
      // sitios distintos:
      //
      // 1) Lo crea el propio jefe de estudios.
      await expect(page.getByTestId('field:createdBy').getByRole('textbox')).toHaveValue(
        JEFE_NOMBRE_COMPLETO,
      );
      //
      // 2) El alta abrió el expediente con perfil CREADOR —el único que ofrece
      //    "Borrar el expediente" (evento DELETE)—, y para ENTRADA_DATOS ese perfil
      //    trae la botonera de quien aún tiene que rellenar y firmar: "Siguiente" y
      //    "Borrar el expediente", SIN "Salir" ni "Presentar la solicitud" (el "Salir"
      //    es del form genérico de solo lectura que vería el perfil TRAMITADOR si lo
      //    hubiera registrado en papel el propio jefe de estudios — lo comprueba
      //    T-010 sobre este mismo trámite y usuario).
      await expect(page.getByRole('button', { name: 'Siguiente' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Borrar el expediente' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Salir' })).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Presentar la solicitud' })).toHaveCount(0);

      // Resultado esperado: y PARA ÉL MISMO (no en representación de otra persona). El
      // panel "Persona que presenta la solicitud" solo se pinta cuando hay
      // representación, así que no debe existir; y el panel propio del trámite "Datos
      // del profesor interesado" nace PRERRELLENO y EN SOLO LECTURA con la identidad
      // del propio jefe de estudios —el sistema ya sabe quién es, no hay que
      // preguntárselo—, a diferencia de T-010 (en papel), donde esos tres campos nacen
      // vacíos porque quien presenta lo entregó en ventanilla y el sistema no sabe
      // nada de él.
      await expect(page.getByTestId(PANEL_PERSONA_SOLICITANTE)).toHaveCount(0);
      await expect(page.getByText('Persona que presenta la solicitud')).toHaveCount(0);
      const panelDatosProfesor = page.getByTestId(PANEL_DATOS_PROFESOR);
      await expect(panelDatosProfesor).toBeVisible();
      const apellidosInteresado = panelDatosProfesor.getByRole('textbox', { name: 'Apellidos' });
      const nombreInteresado = panelDatosProfesor.getByRole('textbox', { name: 'Nombre' });
      const dniInteresado = panelDatosProfesor.getByRole('textbox', { name: 'DNI' });
      await expect(apellidosInteresado).toHaveValue(JEFE_APELLIDOS);
      await expect(nombreInteresado).toHaveValue(JEFE_NOMBRE);
      await expect(dniInteresado).toHaveValue(JEFE_DNI);
      await expect(apellidosInteresado).toBeDisabled();
      await expect(nombreInteresado).toBeDisabled();
      await expect(dniInteresado).toBeDisabled();
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
