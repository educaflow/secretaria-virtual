import { test, expect, Locator, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-010 — Usuario con las dos formas de presentar debe elegir
// origen: ESC-010  |  verifica: V-AsistenteNuevoExpediente-002, U-nuevo-expediente-006, U-nuevo-expediente-007, U-nuevo-expediente-009, U-nuevo-expediente-012, U-nuevo-expediente-013, R-AsistenteNuevoExpediente-001, R-AsistenteNuevoExpediente-005
// fuente: .sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/test-e2e-desc/t-010-usuario-con-las-dos-formas-de-presentar-debe-elegir.desc.md

/**
 * IDEMPOTENCIA (§4 del contrato de generación) — este test CREA un expediente, y su
 * identificador (`00041/2026-46019660`) lo asigna el servidor con un contador, así que NO se
 * puede aislar con un sufijo `Date.now()` en un nombre: no hay ningún nombre que el
 * test elija. La idempotencia se consigue de las otras dos formas:
 *   - el expediente creado se identifica por SU número, capturado en tiempo de
 *     ejecución del título de su pestaña (nunca por un número fijo), y
 *   - se BORRA en el `finally`, de modo que la BD compartida queda como estaba.
 * El teardown reabre el expediente desde «Tramitación» → «Pendientes de mí» —donde el
 * servidor lo abre con el perfil de su estado, TRAMITADOR, que es el único que ofrece
 * «Borrar el expediente» en PENDIENTE_DOCUMENTO_ESCANEADO— en vez de borrarlo sobre la
 * pestaña que dejó el alta: arrancar con un `goto` lo hace robusto aunque el test falle
 * con un diálogo abierto o a medio navegar.
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

// Identidad del jefe de estudios tal y como la carga `data-demo/input/usuarios-demo.xml`:
// es el nombre con el que la aplicación lo pinta en «Creado por» y el que queda en
// `usuarioRegistrador`.
const JEFE_NOMBRE_COMPLETO = 'JefeEstudios1 CIPFP Mislata';

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

// Primer estado del tipo de expediente cuando se registra EN PAPEL, según
// `InitialEventManagerImpl`: fase ENTRADA, estado PENDIENTE_DOCUMENTO_ESCANEADO. Son los
// `title` del `TipoExpedienteInstance.xml`. Quien lo presenta él mismo por vía telemática
// arranca en ENTRADA_DATOS, así que este estado es, por sí solo, la prueba de que el
// expediente quedó en papel.
const FASE_INICIAL = 'Entrada';
const ESTADO_INICIAL_EN_PAPEL = 'Pendiente de adjuntar la solicitud en papel escaneada';

// Panel común (`solicitud-escaneada-upload` de `tramites/shared/template-views.xml`) y
// aviso del form de PENDIENTE_DOCUMENTO_ESCANEADO con perfil TRAMITADOR: verlos prueba
// que la aplicación trata el expediente como registrado en papel.
const PANEL_SOLICITUD_ESCANEADA = 'panel:solicitud-escaneada-upload';
const AVISO_EN_PAPEL =
  'Adjunte escaneada en PDF la solicitud que ha entregado firmada la persona que la presenta';

// Panel del form plantilla del tipo (`views.xml` de la raíz de la versión) con los datos
// de la persona interesada. Lo incluyen los forms de ENTRADA_DATOS —donde nacería el
// expediente por la vía telemática—, pero NO el de PENDIENTE_DOCUMENTO_ESCANEADO con
// perfil TRAMITADOR: su ausencia es la otra cara de la misma comprobación.
const PANEL_DATOS_PROFESOR = 'panel:datos-profesor';

// Modelo JPA del tipo de expediente: lo necesita la lectura por REST del expediente ya
// persistido (ver `leerExpedientePersistido`).
const MODELO_EXPEDIENTE = 'com.educaflow.subsystem.expedientes.db.JustificacionFaltaProfesoradoV1';

// Sufijo del título de la pestaña del expediente: -<trámite> V1.
const SUFIJO_TITULO_EXPEDIENTE = `-${TRAMITE} V1`;

// Título de la pestaña del expediente creado: <nº>/<año>-<código de centro>-<trámite> V1
// (p. ej. `00076/2026-46019660-Justificación de falta del profesorado V1`).
const TITULO_EXPEDIENTE = new RegExp(`^\\d+/\\d{4}-\\d+${SUFIJO_TITULO_EXPEDIENTE}$`);

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
 * Borra el expediente `numero` reabriéndolo desde «Tramitación» → «Pendientes de mí», la
 * lista donde está un expediente en PENDIENTE_DOCUMENTO_ESCANEADO de quien lo tramita;
 * el servidor lo abre con el perfil de su estado, TRAMITADOR, el único que ofrece «Borrar
 * el expediente» en ese estado. Se filtra el listado por el número (en vez de recorrer sus filas) para
 * no depender de la paginación: la BD es compartida y el listado crece con los
 * expedientes de otros runs. El botón abre un diálogo de confirmación de Axelor que hay
 * que aceptar; el evento DELETE responde con `refresh-app`, así que la aplicación se
 * recarga entera y la pestaña del expediente desaparece.
 */
async function borrarExpediente(page: Page, numero: string): Promise<void> {
  await page.goto('/#/');
  await abrirEntradaDeMenu(page, 'tramitacion-menuitem', 'tramitacion-pendientesDeMi-menuitem');

  const filtro = page
    .getByTestId('search-row')
    .getByTestId('column:numeroExpediente')
    .locator('input');
  await filtro.fill(numero);
  await filtro.press('Enter');
  await expect(filasDeExpedientes(page)).toHaveCount(1);
  await filasDeExpedientes(page).first().click();
  await expect(page.getByRole('tab', { name: TITULO_EXPEDIENTE })).toBeVisible();

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
      // falta del profesorado" en su primer estado. Al haberse registrado en papel ese
      // primer estado es PENDIENTE_DOCUMENTO_ESCANEADO de la fase ENTRADA, y no
      // ENTRADA_DATOS, que es donde nacería por la vía telemática (lo fija
      // `InitialEventManagerImpl`).
      const pestanaExpediente = page.getByRole('tab', { name: TITULO_EXPEDIENTE });
      await expect(pestanaExpediente).toBeVisible();
      const tituloExpediente = (await pestanaExpediente.getByTestId('title').innerText()).trim();
      // El número completo es `<nº>/<año>-<código de centro>`: el título sin el sufijo.
      numeroExpediente = tituloExpediente.slice(0, -SUFIJO_TITULO_EXPEDIENTE.length);
      await expect(page.getByTestId('field:namePhase').getByRole('textbox')).toHaveValue(
        FASE_INICIAL,
      );
      // Ojo: el rótulo "Estado" es subcadena de "Fecha último estado", así que
      // localizarlo por nombre accesible resolvería a dos inputs. Se acota al campo.
      await expect(page.getByTestId('field:nameState').getByRole('textbox')).toHaveValue(
        ESTADO_INICIAL_EN_PAPEL,
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
      // 1) Está pintado el formulario de la vía en papel (control positivo): el panel
      //    para adjuntar la solicitud escaneada, con su aviso. Y NO el panel "Datos del
      //    profesor interesado", que es lo primero que vería si el expediente hubiera
      //    nacido por la vía telemática —en ENTRADA_DATOS, con sus datos ya copiados—:
      //    lo comprueba T-019 sobre este mismo trámite y usuario.
      await expect(page.getByTestId(PANEL_SOLICITUD_ESCANEADA)).toBeVisible();
      await expect(page.getByText(AVISO_EN_PAPEL)).toBeVisible();
      await expect(page.getByTestId(PANEL_DATOS_PROFESOR)).toHaveCount(0);
      //
      // 2) El alta abrió el expediente con perfil TRAMITADOR —el único que
      //    `Profile.permitePresentacionEnPapel()` admite—, y para
      //    PENDIENTE_DOCUMENTO_ESCANEADO ese perfil trae la botonera de quien registra
      //    el papel: "Borrar el expediente" y "Siguiente", SIN "Salir" (el del form
      //    genérico de solo lectura) ni "Presentar la solicitud" (el del estado
      //    siguiente, ENTRADA_DATOS).
      await expect(page.getByRole('button', { name: 'Borrar el expediente' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Siguiente' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Salir' })).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Presentar la solicitud' })).toHaveCount(0);

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
      expect(persistido.codePhase).toBe('ENTRADA');
      expect(persistido.codeState).toBe('PENDIENTE_DOCUMENTO_ESCANEADO');
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
