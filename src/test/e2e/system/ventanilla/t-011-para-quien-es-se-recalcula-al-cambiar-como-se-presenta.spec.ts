import { test, expect, Locator, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-011 — «¿Para quién es?» se recalcula al cambiar cómo se presenta
// origen: ESC-011  |  verifica: V-AsistenteNuevoExpediente-003, U-nuevo-expediente-006, U-nuevo-expediente-009, U-nuevo-expediente-010, U-nuevo-expediente-011, U-nuevo-expediente-012, U-nuevo-expediente-013, R-AsistenteNuevoExpediente-001, R-AsistenteNuevoExpediente-005
// fuente: .sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/test-e2e-desc/t-011-para-quien-es-se-recalcula-al-cambiar-como-se-presenta.desc.md

/**
 * IDEMPOTENCIA (§4 del contrato de generación) — este test CREA un expediente, y su
 * identificador (`00049/2026`) lo asigna el servidor con un contador, así que NO se
 * puede aislar con un sufijo `Date.now()` en un nombre: no hay ningún nombre que el
 * test elija. La idempotencia se consigue de las otras dos formas:
 *   - el expediente creado se identifica por SU número, capturado en tiempo de
 *     ejecución del título de su pestaña (nunca por un número fijo), y
 *   - se BORRA en el `finally`, de modo que la BD compartida queda como estaba.
 * El teardown reabre el expediente desde «Expedientes Esperando» —el listado que lo
 * abre con perfil TRAMITADOR, que es el único que ofrece «Borrar el expediente» en
 * PENDIENTE_DOCUMENTO_ESCANEADO— en vez de borrarlo sobre la pestaña que dejó el alta:
 * arrancar con un `goto` lo hace robusto aunque el test falle con un diálogo abierto o
 * a medio navegar.
 * No hace falta pre-limpieza defensiva: ninguna regla de negocio limita cuántas
 * anulaciones de matrícula puede registrar el administrativo, así que un expediente
 * residual de un run que abortara no impide crear otro ni cambia ninguna aserción (el
 * test no cuenta expedientes en absoluto salvo para comprobar que el intento fallido no
 * creó ninguno, y eso lo hace con un delta dentro del propio run).
 */

// Credenciales del usuario de la precondición (tabla «Usuarios de acceso» del .desc.md).
// administrativo2@mislata.es es el caso clave del escenario: en CIPFP Mislata es a la
// vez ADMINISTRATIVO y ALUMNO, así que sobre los trámites del alumno tiene las DOS
// formas de iniciarlos (presentarlos él mismo como alumno, registrarlos en papel como
// administrativo) y el asistente MUST preguntarle cómo se presenta. Y como es alumno y
// NO es familiar, mientras conteste «Lo presento yo mismo» el expediente solo puede ser
// para él mismo: ahí no hay nada que preguntar. Es justo ese contraste —la misma
// pantalla, dos respuestas distintas a «¿Cómo se presenta?»— lo que prueba el recálculo.
const USUARIO = 'administrativo2@mislata.es';
const CONTRASENA = 'demo1234';

// Datos del estado inicial de la BD que el test da por sentados.
const CENTRO = 'CIPFP Mislata';
const TRAMITE = 'Anulación de matrícula en ciclo formativo';
const TIPO_TRAMITE_ALUMNO = 'Trámites para el alumno';
const TIPO_TRAMITE_PROFESOR = 'Trámites para el profesor';

// Identidad del administrativo tal y como la carga `data-demo/input/usuarios-demo.xml`;
// es el nombre con el que la aplicación lo pinta en «Creado por» y el que queda en
// `usuarioRegistrador`.
const ADMINISTRATIVO_NOMBRE_COMPLETO = 'Administrativo2 CIPFP Mislata';

// Títulos de las TRES pantallas del asistente (los fijan los `action-view` de
// `system/ventanilla/views/`). Se usan para comprobar tanto que se abre la que toca
// como que al final NO queda ninguna abierta.
// Se expresan como expresiones regulares ANCLADAS con un `*` final opcional porque
// Axelor marca con un asterisco la pestaña cuyo formulario tiene cambios sin guardar:
// en cuanto el usuario toca un radio, «Nuevo expediente» pasa a llamarse «Nuevo
// expediente*». El ancla `$` es lo que mantiene la precisión de un `exact: true` —
// `/^Nuevo expediente\*?$/` NO casa con «Nuevo expediente: elija el trámite»—, y usarlas
// también en las aserciones de ausencia las hace MÁS estrictas, no menos: una pestaña
// del asistente que siguiera abierta y sucia tampoco se colaría.
const PANTALLA_CENTRO = /^Nuevo expediente: elija el centro\*?$/;
const PANTALLA_TRAMITE = /^Nuevo expediente: elija el trámite\*?$/;
const PANTALLA_CONTEXTO = /^Nuevo expediente\*?$/;

// Rótulos de las dos preguntas del último paso del asistente (los `title` de
// `presentadoEnPapel` y `presentadoEnRepresentacion` en `Main-AsistenteNuevoExpediente.xml`).
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

// Los DOS mensajes del `<action-validate>` local del form. El escenario espera el
// segundo; el primero es su hermano en la MISMA acción y sirve de control de la
// aserción negativa (ver el paso 11).
const ERROR_FALTA_PARA_QUIEN = 'Debe indicar para quién es el expediente';
const ERROR_FALTA_FORMA_DE_PRESENTAR = 'Debe indicar cómo se presenta el expediente';

// Primer estado del tipo de expediente cuando se registra EN PAPEL, según
// `InitialEventManagerImpl`: fase SOLICITUD, estado PENDIENTE_DOCUMENTO_ESCANEADO.
// Quien lo presenta él mismo por vía telemática arranca en DATOS_SOLICITUD, así que
// este estado es, por sí solo, la prueba de que el expediente quedó en papel.
const FASE_INICIAL = 'Solicitud de anulación';
const ESTADO_INICIAL_EN_PAPEL = 'Pendiente de adjuntar la solicitud en papel escaneada';

// Aviso del form de PENDIENTE_DOCUMENTO_ESCANEADO con perfil TRAMITADOR: verlo prueba
// que la aplicación trata el expediente como registrado en papel.
const AVISO_EN_PAPEL =
  'Adjunte escaneada en PDF la solicitud que ha entregado firmada la persona que la presenta';

// Aviso que la vista pinta a quien presenta por vía TELEMÁTICA (estado DATOS_SOLICITUD,
// `showIf="!presentadoEnPapel"`): su ausencia es la otra cara de la misma comprobación.
const AVISO_PRESENTA_EL_MISMO =
  'Para presentar la solicitud necesitará firmarla con su certificado digital desde este mismo ordenador';

// Modelo JPA del tipo de expediente: lo necesita la lectura por REST del expediente ya
// persistido (ver `leerExpedientePersistido`).
const MODELO_EXPEDIENTE =
  'com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1';

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
}

/**
 * Lee del servidor el expediente `numero` TAL Y COMO QUEDÓ PERSISTIDO.
 *
 * <p>Hace falta porque el formulario en el que el alta deja al usuario (el de
 * PENDIENTE_DOCUMENTO_ESCANEADO con perfil TRAMITADOR) no enseña si el expediente es en
 * representación: el único panel de la UI que lo delata es `persona-solicitante`
 * (`showIf="presentadoEnRepresentacion"`) y no está en el `<include-panels>` de ese
 * form —que es una ALLOWLIST—, así que mirarlo ahí no probaría nada en ningún sentido.
 * Lo mismo pasa con el centro. Por eso esos dos puntos del «Resultado esperado» se
 * comprueban contra el dato guardado, y además en POSITIVO
 * (`presentadoEnRepresentacion === true`), que es lo que este escenario espera.
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
 * Cuántos expedientes de este tipo hay ahora mismo. Sirve para comprobar que el intento
 * de alta fallido del paso 10 NO creó ninguno: el `playwright.config.ts` fija
 * `workers: 1`, así que durante el test nadie más escribe en la BD y el total solo puede
 * moverlo este test. Se compara como DELTA dentro del run, nunca contra un valor fijo,
 * para que la BD compartida (que crece con otros runs) no lo rompa.
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
 * quien decide si hay que preguntar «¿Para quién es el expediente?».
 *
 * <p>**CRITICAL**: sin esta espera, las comprobaciones de los pasos 7 y 9 se harían
 * sobre la pantalla ANTERIOR a la respuesta. La del paso 7 («sigue sin verse») pasaría
 * aunque el servidor mandara pintar la pregunta, que es justo el fallo que este test
 * existe para cazar. Se registra el `waitForResponse` ANTES de pulsar el radio.
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

/** Filas de datos de un listado de expedientes (la fila de búsqueda no lleva `row:`). */
function filasDeExpedientes(page: Page): Locator {
  return page.getByRole('grid').locator('[data-testid^="row:"]');
}

/**
 * Borra el expediente `numero` reabriéndolo desde «Expedientes Esperando», el listado que
 * lo abre con perfil TRAMITADOR (`<context name="_profile" expr="TRAMITADOR"/>` de su
 * `action-view`), el único que ofrece «Borrar el expediente» en este estado. Se filtra el
 * listado por el número (en vez de recorrer sus filas) para no depender de la paginación:
 * la BD es compartida y el listado crece con los expedientes de otros runs. El botón abre
 * un diálogo de confirmación de Axelor que hay que aceptar; el evento DELETE responde con
 * `refresh-app`, así que la aplicación se recarga entera y la pestaña desaparece.
 */
async function borrarExpediente(page: Page, numero: string): Promise<void> {
  await page.goto('/#/');
  await abrirEntradaDeMenu(page, 'expedientes-menuitem', 'expedientes-expedientesEsperando-menuitem');

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
  test('«¿Para quién es?» se recalcula al cambiar cómo se presenta', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el administrativo `administrativo2@mislata.es` (que en "CIPFP
    // Mislata" es además alumno) ha iniciado sesión con la contraseña `demo1234`.
    await login(page, USUARIO, CONTRASENA);

    // Número del expediente creado; se captura tras crearlo y lo usa el teardown para
    // borrarlo. Vacío mientras no exista el expediente.
    let numeroExpediente = '';

    try {
      // Paso 2: Cuando abre el menú "Ventanilla" y pulsa "Nuevo expediente".
      await abrirEntradaDeMenu(page, 'ventanilla-menuitem', 'ventanilla-nuevoExpediente-menuitem');

      // Paso 3: Entonces se abre DIRECTAMENTE "Nuevo expediente: elija el trámite"…
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE })).toBeVisible();
      // …"directamente" = sin pasar por la elección de centro: el administrativo solo
      // tiene un centro, así que ni se abre esa pantalla ni aparece su listado.
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO })).toHaveCount(0);
      await expect(page.getByTestId('panel:centrosPanel')).toHaveCount(0);

      // …con el centro "CIPFP Mislata".
      const campoCentro = page.getByTestId('field:centro').getByRole('textbox');
      await expect(campoCentro).toHaveValue(CENTRO);
      await expect(campoCentro).toBeDisabled();

      // …y únicamente "Trámites para el alumno" con "Anulación de matrícula en ciclo
      // formativo" dentro: aunque el usuario es administrativo Y alumno, los dos papeles
      // le dan permiso sobre el MISMO tipo de trámite, así que solo ve ese grupo.
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

      // Paso 5: Entonces se abre "Nuevo expediente" con ese trámite y su centro.
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO })).toBeVisible();
      const campoTramite = page.getByTestId('field:nombreTramite').getByRole('textbox');
      await expect(campoTramite).toHaveValue(TRAMITE);
      await expect(campoTramite).toBeDisabled();
      const centroContexto = page.getByTestId('field:centro').getByRole('textbox');
      await expect(centroContexto).toHaveValue(CENTRO);
      await expect(centroContexto).toBeDisabled();

      // …con "¿Cómo se presenta?" VISIBLE y SIN MARCAR: el usuario tiene las dos formas
      // de iniciar este trámite (alumno y administrativo), así que hay que preguntárselo.
      await expect(page.getByText(PREGUNTA_COMO_SE_PRESENTA)).toBeVisible();
      await expect(page.getByTestId('field:presentadoEnPapel')).toBeVisible();
      const radioLoPresentoYo = opcionComoSePresenta(page, OPCION_LO_PRESENTO_YO);
      const radioEnPapel = opcionComoSePresenta(page, OPCION_EN_PAPEL);
      await expect(radioLoPresentoYo).toBeVisible();
      await expect(radioEnPapel).toBeVisible();
      await expect(radioLoPresentoYo).not.toBeChecked();
      await expect(radioEnPapel).not.toBeChecked();

      // …y SIN "¿Para quién es el expediente?": mientras no se conteste cómo se
      // presenta, no hay destinatario que preguntar. Se comprueba por su rótulo y por el
      // campo del modelo que la pinta (`presentadoEnRepresentacion`), para que el test
      // siga cazando el fallo aunque cambie el texto.
      //
      // La ausencia NO es vacua, por partida doble: (1) el campo vive en el MISMO
      // `presentacionPanel` que `presentadoEnPapel`, que sí está pintado dos líneas más
      // arriba —o sea, se está mirando un panel vivo, no uno que no existe—, y (2) ese
      // mismo campo APARECE en esta misma pantalla en el paso 9, sin recargar nada: si la
      // pregunta se pintara siempre, esta aserción fallaría.
      await expect(page.getByText(PREGUNTA_PARA_QUIEN)).toHaveCount(0);
      await expect(page.getByTestId('field:presentadoEnRepresentacion')).toHaveCount(0);

      // Paso 6: Cuando marca "Lo presento yo mismo".
      // El `onChange` va al servidor a recalcular qué hay que preguntar; se espera su
      // respuesta ANTES de comprobar nada (ver `esperarRecalculo`).
      const recalculoTelematico = esperarRecalculo(page);
      await radioLoPresentoYo.click();
      await recalculoTelematico;

      // Paso 7: Entonces SIGUE sin verse "¿Para quién es el expediente?": es alumno y no
      // es familiar, así que el expediente solo puede ser para él mismo y no hay nada que
      // elegir.
      //
      // Control positivo de esta ausencia: la opción que acaba de marcar quedó marcada y
      // la otra no, o sea que el recálculo del servidor llegó, se aplicó y el panel de la
      // presentación sigue vivo. La ausencia es, por tanto, una decisión del servidor y
      // no una pantalla a medio pintar.
      await expect(radioLoPresentoYo).toBeChecked();
      await expect(radioEnPapel).not.toBeChecked();
      await expect(page.getByText(PREGUNTA_COMO_SE_PRESENTA)).toBeVisible();
      await expect(page.getByText(PREGUNTA_PARA_QUIEN)).toHaveCount(0);
      await expect(page.getByTestId('field:presentadoEnRepresentacion')).toHaveCount(0);

      // Paso 8: Cuando cambia a "Estoy registrando un trámite recibido en papel".
      const recalculoEnPapel = esperarRecalculo(page);
      await radioEnPapel.click();
      await recalculoEnPapel;

      // Paso 9: Entonces se MUESTRA "¿Para quién es el expediente?" — el recálculo del
      // paso 7 se deshace al cambiar la respuesta: registrando en papel, quien entrega la
      // solicitud puede estar haciéndolo por otra persona. Esta es la cara POSITIVA de
      // las dos ausencias de los pasos 5 y 7, sobre la misma pantalla y sin recargarla.
      await expect(page.getByText(PREGUNTA_PARA_QUIEN)).toBeVisible();
      await expect(page.getByTestId('field:presentadoEnRepresentacion')).toBeVisible();
      await expect(radioEnPapel).toBeChecked();
      await expect(radioLoPresentoYo).not.toBeChecked();

      // …con "Para mí" y "Para otra persona a la que represento (hijo/a menor de edad o
      // persona tutelada)", SIN NINGUNA MARCADA.
      const radioParaMi = opcionParaQuien(page, OPCION_PARA_MI);
      const radioEnRepresentacion = opcionParaQuien(page, OPCION_EN_REPRESENTACION);
      await expect(radioParaMi).toBeVisible();
      await expect(radioEnRepresentacion).toBeVisible();
      await expect(radioParaMi).not.toBeChecked();
      await expect(radioEnRepresentacion).not.toBeChecked();

      // Cuántos expedientes de este trámite hay ANTES del intento fallido, para poder
      // comprobar en el paso 11 que ese intento no creó ninguno.
      const expedientesAntes = await contarExpedientesDelTramite(page);

      // Paso 10: Cuando pulsa "Crear expediente" sin marcar ninguna opción.
      await page.getByRole('button', { name: 'Crear expediente' }).click();

      // Paso 11: Entonces el sistema muestra "Debe indicar para quién es el expediente"…
      const aviso = page.getByRole('dialog');
      await expect(aviso).toBeVisible();
      await expect(aviso).toContainText(ERROR_FALTA_PARA_QUIEN);
      // …y SOLO ese mensaje: el otro `<error>` de la misma `<action-validate>` no salta,
      // porque "¿Cómo se presenta?" sí está contestado. La ausencia no es vacua —es un
      // mensaje real, hermano del anterior en la misma acción, que este mismo diálogo
      // mostraría si su condición se cumpliera—, y es lo que separa el diálogo del
      // cliente de una copia que contestara por preguntas que no están en pantalla.
      await expect(aviso).not.toContainText(ERROR_FALTA_FORMA_DE_PRESENTAR);

      // …no crea ningún expediente…
      // Dos comprobaciones independientes: no se abrió la pestaña del expediente (la
      // misma que sí aparece al final de este test cuando el alta funciona, así que esta
      // ausencia no puede ser vacua) y el total de expedientes del trámite en la BD no se
      // movió.
      await expect(page.getByRole('tab', { name: TITULO_EXPEDIENTE })).toHaveCount(0);
      await aviso.getByRole('button', { name: 'Aceptar' }).click();
      await expect(page.getByRole('dialog')).toHaveCount(0);
      expect(await contarExpedientesDelTramite(page)).toBe(expedientesAntes);

      // …y "Nuevo expediente" sigue abierta, con todo lo que tenía: las dos preguntas en
      // pantalla, la respuesta ya dada conservada y la que falta aún sin marcar.
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO })).toBeVisible();
      await expect(page.getByText(PREGUNTA_COMO_SE_PRESENTA)).toBeVisible();
      await expect(page.getByText(PREGUNTA_PARA_QUIEN)).toBeVisible();
      await expect(radioEnPapel).toBeChecked();
      await expect(radioParaMi).not.toBeChecked();
      await expect(radioEnRepresentacion).not.toBeChecked();
      await expect(page.getByRole('button', { name: 'Crear expediente' })).toBeVisible();

      // Paso 12: Cuando marca "Para otra persona a la que represento (hijo/a menor de
      // edad o persona tutelada)" y pulsa "Crear expediente".
      await radioEnRepresentacion.click();
      // El expediente se crea con lo que estaba marcado en ese instante: sin esta
      // comprobación, un radio que no llegara a marcarse dejaría pasar el test.
      await expect(radioEnRepresentacion).toBeChecked();
      await expect(radioParaMi).not.toBeChecked();
      await page.getByRole('button', { name: 'Crear expediente' }).click();

      // Resultado esperado: se abre el expediente recién creado de "Anulación de
      // matrícula en ciclo formativo" en su primer estado. Al haberse registrado en papel
      // ese primer estado es PENDIENTE_DOCUMENTO_ESCANEADO de la fase SOLICITUD, y no
      // DATOS_SOLICITUD, que es donde nacería por la vía telemática.
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
        ESTADO_INICIAL_EN_PAPEL,
      );

      // Resultado esperado: el asistente SE CIERRA — no queda visible ninguna de sus TRES
      // pantallas, ni por su pestaña ni por sus paneles y botones. Ninguna de estas
      // ausencias es vacua: son las mismas pestañas, paneles y botones que este test ha
      // ido viendo en los pasos 3 a 12.
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO })).toHaveCount(0);
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE })).toHaveCount(0);
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO })).toHaveCount(0);
      await expect(page.getByTestId('panel:centrosPanel')).toHaveCount(0);
      await expect(page.getByTestId('panel:tramitesPanel')).toHaveCount(0);
      await expect(page.getByTestId('panel:tramitePanel')).toHaveCount(0);
      await expect(page.getByTestId('panel:presentacionPanel')).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Crear expediente' })).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Cancelar' })).toHaveCount(0);

      // Resultado esperado: queda registrado como PRESENTADO EN PAPEL, y así lo ve el
      // usuario: el panel para adjuntar la solicitud escaneada, con su aviso (control
      // POSITIVO: es el formulario de la vía en papel el que está pintado), y la AUSENCIA
      // del aviso de la vía telemática.
      //
      // Esa ausencia no es vacua pese a que los `<include-panels>` sean allowlists: si el
      // expediente hubiera nacido por la vía telemática NO estaría en este estado ni en
      // este formulario, sino en DATOS_SOLICITUD, cuyo form sí trae ese aviso
      // (`showIf="!presentadoEnPapel"`). O sea, con el comportamiento contrario el
      // elemento existiría de verdad — que es justo lo que se le pide a una negativa.
      // Las dos ramas son excluyentes, así que ver una y no ver la otra es la misma
      // comprobación por sus dos caras, y la refrenda el estado ya asertado arriba.
      await expect(page.getByTestId('panel:solicitud-escaneada-upload')).toBeVisible();
      await expect(page.getByText(AVISO_EN_PAPEL)).toBeVisible();
      await expect(page.getByText(AVISO_PRESENTA_EL_MISMO)).toHaveCount(0);

      // Resultado esperado: …POR EL ADMINISTRATIVO.
      await expect(page.getByTestId('field:createdBy').getByRole('textbox')).toHaveValue(
        ADMINISTRATIVO_NOMBRE_COMPLETO,
      );

      // Resultado esperado (lo PERSISTIDO): en el centro "CIPFP Mislata", presentado en
      // papel por el administrativo y EN REPRESENTACIÓN de otra persona (no para él
      // mismo). Se lee del servidor porque el formulario de este estado no pinta ni el
      // centro ni la representación (ver `leerExpedientePersistido`): mirarlos en la
      // pantalla sería una comprobación vacua. Y se comprueba en POSITIVO —el valor
      // guardado es exactamente `true`—, que es lo contrario de una aserción de ausencia.
      const persistido = await leerExpedientePersistido(page, numeroExpediente);
      expect(persistido.numeroExpediente).toBe(numeroExpediente);
      expect(persistido['centro.name']).toBe(CENTRO);
      expect(persistido.presentadoEnPapel).toBe(true);
      expect(persistido.presentadoEnRepresentacion).toBe(true);
      expect(persistido['usuarioRegistrador.name']).toBe(ADMINISTRATIVO_NOMBRE_COMPLETO);
      expect(persistido.codePhase).toBe('SOLICITUD');
      expect(persistido.codeState).toBe('PENDIENTE_DOCUMENTO_ESCANEADO');
    } finally {
      // Teardown: borrar el expediente creado aunque una aserción haya fallado — la BD es
      // compartida y no se resetea, así que dejarlo lo acumularía run tras run.
      // Si el test falló ANTES de crearlo no hay nada que borrar.
      if (numeroExpediente !== '') {
        await borrarExpediente(page, numeroExpediente);
      }

      await logout(page);
    }
  });
});
