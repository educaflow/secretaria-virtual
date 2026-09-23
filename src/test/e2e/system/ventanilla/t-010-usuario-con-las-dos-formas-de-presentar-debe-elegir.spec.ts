import { test, expect, Locator, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-010 — Usuario con las dos formas de presentar debe elegir
// origen: ESC-010  |  verifica: V-AsistenteNuevoExpediente-002, U-nuevo-expediente-006, U-nuevo-expediente-007, U-nuevo-expediente-009, U-nuevo-expediente-012, U-nuevo-expediente-013, R-AsistenteNuevoExpediente-001, R-AsistenteNuevoExpediente-005
// fuente: .sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/test-e2e-desc/t-010-usuario-con-las-dos-formas-de-presentar-debe-elegir.desc.md

/**
 * IDEMPOTENCIA (§4 del contrato de generación) — este test CREA un expediente, y su
 * identificador (`00041/2026`) lo asigna el servidor con un contador, así que NO se
 * puede aislar con un sufijo `Date.now()` en un nombre: no hay ningún nombre que el
 * test elija. La idempotencia se consigue de las otras dos formas:
 *   - el expediente creado se identifica por SU número, capturado en tiempo de
 *     ejecución del título de su pestaña (nunca por un número fijo), y
 *   - se BORRA en el `finally`, de modo que la BD compartida queda como estaba.
 * El borrado no se puede hacer sobre la pestaña que abre el alta: registrar en papel
 * abre el expediente con perfil TRAMITADOR, y para ENTRADA_DATOS ese perfil cae en el
 * form genérico de solo lectura, cuyo único botón es «Salir». El botón «Borrar el
 * expediente» (evento DELETE) lo ofrece el form de perfil CREADOR, así que el teardown
 * reabre el expediente desde «Expedientes Pendientes», el listado que lo abre con ese
 * perfil (`<context name="_profile" expr="CREADOR"/>` de su `action-view`). Arrancar el
 * teardown con un `goto` lo hace robusto aunque el test falle con un diálogo abierto o
 * a medio navegar.
 * No hace falta pre-limpieza defensiva: ninguna regla de negocio limita cuántas
 * justificaciones de falta puede registrar el jefe de estudios, así que un expediente
 * residual de un run que abortara no impide crear otro ni cambia ninguna aserción (el
 * test no cuenta expedientes en absoluto: mira el suyo, filtrado por su número).
 */

// Credenciales del usuario de la precondición (tabla «Usuarios de acceso» del .desc.md).
// jefeestudios1@mislata.es es el caso clave del escenario: sobre los trámites del
// profesor tiene AMBAS formas de iniciarlos —como profesor puede presentarlos él mismo
// y como jefe de estudios puede registrarlos en papel—, así que el asistente MUST
// preguntarle cómo se presenta y no puede deducirlo.
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
// apellidos y DNI son los que `Tramitador.updatePersonas` copiaría en la persona
// interesada SI el expediente se presentara por vía telemática (ver la aserción de
// «presentado en papel» al final del test).
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
// `boolean-radio`): «Estoy registrando…» es `presentadoEnPapel = true`.
const OPCION_LO_PRESENTO_YO = 'Lo presento yo mismo';
const OPCION_EN_PAPEL = 'Estoy registrando un trámite recibido en papel';

// Mensaje del `<action-validate>` del form cuando se intenta crear sin contestar a
// «¿Cómo se presenta?».
const ERROR_FALTA_FORMA_DE_PRESENTAR = 'Debe indicar cómo se presenta el expediente';

// Estado en el que nace el expediente según `InitialEventManagerImpl`: fase RECEPCION,
// estado ENTRADA_DATOS. Son los `title` del `TipoExpedienteInstance.xml`.
const FASE_INICIAL = 'Recepción';
const ESTADO_INICIAL = 'Entrada de datos';

// Panel del form plantilla del tipo (`views.xml` de la raíz de la versión) con los datos
// de la persona interesada. Está en el `<include-panels>` de los DOS forms de
// ENTRADA_DATOS, así que se pinta se mire con el perfil que se mire.
const PANEL_DATOS_PROFESOR = 'panel:datos-profesor';

// Modelo JPA del tipo de expediente: lo necesita la lectura por REST del expediente ya
// persistido (ver `leerExpedientePersistido`).
const MODELO_EXPEDIENTE = 'com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1';

// Título de la pestaña del expediente creado: <nº>/<año>-<trámite> V1.
const TITULO_EXPEDIENTE = new RegExp(`^\\d+/\\d{4}-${TRAMITE} V1$`);

/** Lo que la lectura por REST devuelve del expediente ya persistido. */
interface ExpedientePersistido {
  numeroExpediente: string;
  presentadoEnPapel: boolean;
  presentadoEnRepresentacion: boolean;
  codePhase: string;
  codeState: string;
  'centro.name': string;
  'usuarioRegistrador.name': string;
  'personaInteresada.nombre': string | null;
  'personaSolicitante.nombre': string | null;
}

/**
 * Lee del servidor el expediente `numero` TAL Y COMO QUEDÓ PERSISTIDO.
 *
 * <p>Hace falta porque este tipo de expediente no enseña en NINGUNA de sus vistas si el
 * expediente es en representación o para uno mismo: el único panel de la UI que lo
 * delata es `persona-solicitante` (`showIf="presentadoEnRepresentacion"`) y no está en
 * el `<include-panels>` de ningún form de «Justificación de falta del profesorado» —que
 * es una ALLOWLIST—, así que mirarlo aquí sería una comprobación VACUA: faltaría igual
 * con `presentadoEnRepresentacion = true`. Lo mismo pasa con el centro, que tampoco
 * pinta ningún panel de este tipo. Por eso esos dos puntos del «Resultado esperado» se
 * comprueban contra el dato guardado, no contra la pantalla.
 *
 * <p>Se lee con `fetch` DESDE LA PÁGINA (y no con `page.request`) para reutilizar la
 * sesión y el `CSRF-TOKEN` del navegador: son los mismos que usa la propia aplicación.
 */
async function leerExpedientePersistido(page: Page, numero: string): Promise<ExpedientePersistido> {
  const registro = await page.evaluate(
    async ({ modelo, numero }) => {
      const csrf = decodeURIComponent((document.cookie.match(/CSRF-TOKEN=([^;]+)/) ?? [])[1] ?? '');
      const respuesta = await fetch(`/ws/rest/${modelo}/search`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': csrf },
        body: JSON.stringify({
          offset: 0,
          limit: 2,
          fields: [
            'numeroExpediente',
            'presentadoEnPapel',
            'presentadoEnRepresentacion',
            'codePhase',
            'codeState',
            'centro.name',
            'usuarioRegistrador.name',
            'personaInteresada.nombre',
            'personaSolicitante.nombre',
          ],
          data: {
            operator: 'and',
            criteria: [{ fieldName: 'numeroExpediente', operator: '=', value: numero }],
          },
        }),
      });
      const json = await respuesta.json();
      return (json.data ?? [])[0] ?? null;
    },
    { modelo: MODELO_EXPEDIENTE, numero },
  );

  expect(registro, `el expediente ${numero} tiene que existir en la BD`).not.toBeNull();
  return registro as ExpedientePersistido;
}

/**
 * Cuántos expedientes de este tipo hay ahora mismo. Sirve para comprobar que un intento
 * de alta fallido NO creó ninguno: el `playwright.config.ts` fija `workers: 1`, así que
 * durante el test nadie más escribe en la BD y el total solo puede moverlo este test.
 */
async function contarExpedientesDelTramite(page: Page): Promise<number> {
  return await page.evaluate(async (modelo) => {
    const csrf = decodeURIComponent((document.cookie.match(/CSRF-TOKEN=([^;]+)/) ?? [])[1] ?? '');
    const respuesta = await fetch(`/ws/rest/${modelo}/search`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': csrf },
      body: JSON.stringify({ offset: 0, limit: 1, fields: ['numeroExpediente'] }),
    });
    const json = await respuesta.json();
    return json.total as number;
  }, MODELO_EXPEDIENTE);
}

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

/** Filas de datos de un listado de expedientes (la fila de búsqueda no lleva `row:`). */
function filasDeExpedientes(page: Page): Locator {
  return page.getByRole('grid').locator('[data-testid^="row:"]');
}

/**
 * Borra el expediente `numero` reabriéndolo desde «Expedientes Pendientes», el listado
 * que lo abre con perfil CREADOR —el único form de ENTRADA_DATOS que ofrece «Borrar el
 * expediente»—. Se filtra el listado por el número (en vez de recorrer sus filas) para
 * no depender de la paginación: la BD es compartida y el listado crece con los
 * expedientes de otros runs. El botón abre un diálogo de confirmación de Axelor que hay
 * que aceptar; el evento DELETE responde con `refresh-app`, así que la aplicación se
 * recarga entera y la pestaña del expediente desaparece.
 */
async function borrarExpediente(page: Page, numero: string): Promise<void> {
  await page.goto('/#/');
  await abrirEntradaDeMenu(page, 'expedientes-menuitem', 'expedientes-expedientesPendientes-menuitem');

  const filtro = page
    .getByTestId('search-row')
    .getByTestId('column:numeroExpediente')
    .locator('input');
  await filtro.fill(numero);
  await filtro.press('Enter');
  await expect(filasDeExpedientes(page)).toHaveCount(1);
  await filasDeExpedientes(page).first().click();
  await expect(page.getByRole('tab', { name: TITULO_EXPEDIENTE })).toBeVisible();

  // CONTROL POSITIVO de la aserción de más arriba («el alta abrió el form de solo
  // lectura del TRAMITADOR, sin "Siguiente" ni "Borrar el expediente"»): el MISMO
  // expediente, en el MISMO estado, abierto con perfil CREADOR sí trae esos dos
  // botones. Sin esto, aquella ausencia podría deberse a que el estado no los tiene.
  await expect(page.getByRole('button', { name: 'Siguiente' })).toBeVisible();

  await page.getByTestId('widget:DELETE').getByRole('button').click();
  await page.getByRole('dialog').getByRole('button', { name: 'Aceptar' }).click();
  await expect(page.getByRole('tab', { name: TITULO_EXPEDIENTE })).toHaveCount(0);
}

test.describe('Ventanilla — Nuevo expediente', () => {
  test('Usuario con las dos formas de presentar debe elegir', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el jefe de estudios `jefeestudios1@mislata.es` ha iniciado
    // sesión con la contraseña `demo1234`.
    await login(page, USUARIO, CONTRASENA);

    // Número del expediente creado; se captura tras crearlo y lo usa el teardown para
    // borrarlo. Vacío mientras no exista el expediente.
    let numeroExpediente = '';

    try {
      // Paso 2: Cuando abre el menú "Ventanilla" y pulsa "Nuevo expediente".
      await abrirEntradaDeMenu(page, 'ventanilla-menuitem', 'ventanilla-nuevoExpediente-menuitem');

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
      await filasDeTramites(page).first().click();

      // Paso 5: Entonces se abre "Nuevo expediente" con ese trámite, su ayuda y el
      // centro en solo lectura.
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

      // …se ve "¿Cómo se presenta?" con sus dos opciones, SIN ninguna marcada.
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
      // aunque cambie el texto. La ausencia NO es vacua: el campo vive en el mismo
      // panel `presentacionPanel` que `presentadoEnPapel`, que sí está pintado (dos
      // líneas más arriba), y en un trámite que admite representación aparece —lo
      // comprueba T-009 sobre esta misma pantalla—.
      await expect(page.getByText(PREGUNTA_PARA_QUIEN)).toHaveCount(0);
      await expect(page.getByTestId('field:presentadoEnRepresentacion')).toHaveCount(0);

      // Cuántos expedientes de este trámite hay ANTES del intento fallido, para poder
      // comprobar en el paso 7 que ese intento no creó ninguno.
      const expedientesAntes = await contarExpedientesDelTramite(page);

      // Paso 6: Cuando pulsa "Crear expediente" sin marcar ninguna opción.
      await page.getByRole('button', { name: 'Crear expediente' }).click();

      // Paso 7: Entonces el sistema muestra "Debe indicar cómo se presenta el
      // expediente"…
      const aviso = page.getByRole('dialog');
      await expect(aviso).toBeVisible();
      await expect(aviso).toContainText(ERROR_FALTA_FORMA_DE_PRESENTAR);

      // …no crea ningún expediente…
      // Dos comprobaciones independientes: no se abrió la pestaña del expediente (la
      // misma que sí aparece al final de este test cuando el alta funciona, así que
      // esta ausencia no puede ser vacua) y el total de expedientes del trámite en la
      // BD no se movió.
      await expect(page.getByRole('tab', { name: TITULO_EXPEDIENTE })).toHaveCount(0);
      await aviso.getByRole('button', { name: 'Aceptar' }).click();
      await expect(page.getByRole('dialog')).toHaveCount(0);
      expect(await contarExpedientesDelTramite(page)).toBe(expedientesAntes);

      // …y "Nuevo expediente" sigue abierta, con todo lo que tenía.
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO, exact: true })).toBeVisible();
      await expect(page.getByTestId('field:presentadoEnPapel')).toBeVisible();
      await expect(radioLoPresentoYo).not.toBeChecked();
      await expect(radioEnPapel).not.toBeChecked();
      await expect(page.getByRole('button', { name: 'Crear expediente' })).toBeVisible();

      // Paso 8: Cuando marca "Estoy registrando un trámite recibido en papel".
      // El `onChange` del campo va al servidor (`…-onChange-presentadoEnPapel-action`)
      // a recalcular qué hay que preguntar. Se espera esa respuesta ANTES de comprobar
      // que "¿Para quién es el expediente?" sigue sin verse: sin la espera, la
      // comprobación se haría sobre la pantalla previa a la respuesta y pasaría aunque
      // el servidor mandara pintar la pregunta.
      const recalculo = page.waitForResponse(
        (respuesta) =>
          respuesta.url().endsWith('/ws/action') &&
          (respuesta.request().postData() ?? '').includes(
            'AsistenteNuevoExpediente-onChange-presentadoEnPapel-action',
          ),
      );
      await radioEnPapel.click();
      await recalculo;

      // Paso 9: Entonces sigue sin verse "¿Para quién es el expediente?", porque el
      // trámite no admite representación. Control positivo de esa ausencia: la opción
      // que el usuario acaba de marcar quedó marcada y la otra no, o sea que el panel
      // de la presentación sigue vivo y recalculado.
      await expect(radioEnPapel).toBeChecked();
      await expect(radioLoPresentoYo).not.toBeChecked();
      await expect(page.getByText(PREGUNTA_PARA_QUIEN)).toHaveCount(0);
      await expect(page.getByTestId('field:presentadoEnRepresentacion')).toHaveCount(0);

      // Paso 10: Cuando pulsa "Crear expediente".
      await page.getByRole('button', { name: 'Crear expediente' }).click();

      // Resultado esperado: se abre el expediente recién creado de "Justificación de
      // falta del profesorado" en su primer estado (fase RECEPCION, estado
      // ENTRADA_DATOS, los que fija `InitialEventManagerImpl`).
      const pestanaExpediente = page.getByRole('tab', { name: TITULO_EXPEDIENTE });
      await expect(pestanaExpediente).toBeVisible();
      const tituloExpediente = (await pestanaExpediente.getByTestId('title').innerText()).trim();
      numeroExpediente = tituloExpediente.split('-')[0];
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

      // Resultado esperado: el expediente lo registra EL JEFE DE ESTUDIOS.
      await expect(page.getByTestId('field:createdBy').getByRole('textbox')).toHaveValue(
        JEFE_NOMBRE_COMPLETO,
      );

      // Resultado esperado: queda registrado como PRESENTADO EN PAPEL. Se ve en la UI
      // en dos sitios distintos:
      //
      // 1) El panel "Datos del profesor interesado" está pintado (control positivo) y
      //    sus tres campos están VACÍOS. `Tramitador.updatePersonas` solo deja vacías
      //    las personas cuando se registra en papel —quien presenta lo entregó en
      //    ventanilla y el sistema no sabe nada de él—; por la vía telemática habrían
      //    nacido con el nombre, los apellidos y el DNI del jefe de estudios.
      const panelDatosProfesor = page.getByTestId(PANEL_DATOS_PROFESOR);
      await expect(panelDatosProfesor).toBeVisible();
      const apellidosInteresado = panelDatosProfesor.getByRole('textbox', { name: 'Apellidos' });
      const nombreInteresado = panelDatosProfesor.getByRole('textbox', { name: 'Nombre' });
      const dniInteresado = panelDatosProfesor.getByRole('textbox', { name: 'DNI' });
      await expect(apellidosInteresado).toBeVisible();
      await expect(apellidosInteresado).toHaveValue('');
      await expect(nombreInteresado).toHaveValue('');
      await expect(dniInteresado).toHaveValue('');
      await expect(apellidosInteresado).not.toHaveValue(JEFE_APELLIDOS);
      await expect(nombreInteresado).not.toHaveValue(JEFE_NOMBRE);
      await expect(dniInteresado).not.toHaveValue(JEFE_DNI);
      //
      // 2) El alta abrió el expediente con perfil TRAMITADOR —el único que
      //    `Profile.permitePresentacionEnPapel()` admite—, y para ENTRADA_DATOS ese
      //    perfil cae en el form genérico de solo lectura: único botón "Salir", sin
      //    "Siguiente" ni "Borrar el expediente" (los del form del CREADOR, que es con
      //    el que se abriría si lo hubiera presentado él mismo). El control positivo de
      //    estas dos ausencias lo hace el teardown, que reabre ESTE MISMO expediente en
      //    ESTE MISMO estado con perfil CREADOR y comprueba que ahí sí están.
      await expect(page.getByRole('button', { name: 'Salir' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Siguiente' })).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Borrar el expediente' })).toHaveCount(0);

      // Resultado esperado (lo PERSISTIDO): en el centro "CIPFP Mislata", presentado en
      // papel, por el jefe de estudios y PARA ÉL MISMO (no en representación).
      // Se lee del servidor porque este tipo de expediente no pinta en ninguna de sus
      // vistas ni el centro ni la representación (ver `leerExpedientePersistido`):
      // mirarlos en la pantalla sería una comprobación vacua.
      const persistido = await leerExpedientePersistido(page, numeroExpediente);
      expect(persistido.numeroExpediente).toBe(numeroExpediente);
      expect(persistido['centro.name']).toBe(CENTRO);
      expect(persistido.presentadoEnPapel).toBe(true);
      expect(persistido.presentadoEnRepresentacion).toBe(false);
      expect(persistido['usuarioRegistrador.name']).toBe(JEFE_NOMBRE_COMPLETO);
      expect(persistido.codePhase).toBe('RECEPCION');
      expect(persistido.codeState).toBe('ENTRADA_DATOS');
      // Y las dos personas nacieron vacías, que es lo que hace el alta en papel: no se
      // copió en ellas al jefe de estudios, como habría pasado por la vía telemática.
      expect(persistido['personaInteresada.nombre']).toBeNull();
      expect(persistido['personaSolicitante.nombre']).toBeNull();
    } finally {
      // Teardown: borrar el expediente creado aunque una aserción haya fallado — la
      // BD es compartida y no se resetea, así que dejarlo lo acumularía run tras run.
      // Si el test falló ANTES de crearlo no hay nada que borrar.
      if (numeroExpediente !== '') {
        await borrarExpediente(page, numeroExpediente);
      }

      await logout(page);
    }
  });
});
