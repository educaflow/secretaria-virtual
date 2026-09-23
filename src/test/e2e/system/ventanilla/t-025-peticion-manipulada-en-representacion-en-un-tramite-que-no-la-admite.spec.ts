import { test, expect, Page, Route, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-025 — Petición manipulada en representación en un trámite que no la admite
// origen: ESC-024  |  verifica: V-AsistenteNuevoExpediente-006, U-nuevo-expediente-015
// fuente: .sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/test-e2e-desc/t-025-peticion-manipulada-en-representacion-en-un-tramite-que-no-la-admite.desc.md

/**
 * IDEMPOTENCIA (§4 del contrato de generación) — este test es de SOLO LECTURA: manipula
 * la petición de red para que "Crear expediente" pida el expediente en representación de
 * otra persona en un trámite que no lo admite, y comprueba que el servidor la rechaza SIN
 * crear ningún expediente. No hay ningún registro que crear (esa es justo la aserción:
 * que no se cree ninguno), así que no hace falta ni sufijo `Date.now()` ni borrado en el
 * `finally`. Tampoco hace falta pre-limpieza defensiva: un run anterior que abortara no
 * puede haber dejado un expediente a medio crear, porque el servidor rechaza la petición
 * manipulada antes de persistir nada.
 */

// Credenciales del usuario de la precondición (tabla «Usuarios de acceso» del .desc.md):
// el jefe de estudios, que sobre los trámites del profesor SÍ tiene permiso de
// registrarlos en papel (a diferencia del alumno de T-017/T-015), así que la pantalla le
// pregunta "¿Cómo se presenta?" — es la vía por la que este test llega a marcar "Estoy
// registrando un trámite recibido en papel" antes de manipular la petición.
const USUARIO = 'jefeestudios1@mislata.es';
const CONTRASENA = 'demo1234';

// Datos del estado inicial de la BD que el test da por sentados. "Justificación de falta
// del profesorado" NO admite presentarse en representación de otra persona.
const CENTRO = 'CIPFP Mislata';
const TRAMITE = 'Justificación de falta del profesorado';

// Rótulos de las dos preguntas del último paso del asistente (los `title` de
// `presentadoEnPapel` y `presentadoEnRepresentacion` en `AsistenteNuevoExpediente.xml`).
const PREGUNTA_COMO_SE_PRESENTA = '¿Cómo se presenta?';
const PREGUNTA_PARA_QUIEN = '¿Para quién es el expediente?';

// Las dos opciones de «¿Cómo se presenta?» (los `x-false-text`/`x-true-text` del widget
// `boolean-radio`): «Estoy registrando…» es `presentadoEnPapel = true`.
const OPCION_LO_PRESENTO_YO = 'Lo presento yo mismo';
const OPCION_EN_PAPEL = 'Estoy registrando un trámite recibido en papel';

// Fragmento de la acción que dispara "Crear expediente"
// (`sysVentanilla.ElegirFormaPresentacion@AsistenteNuevoExpediente-btnCrear-action`, a
// veces con un sufijo `[2]` cuando Axelor encadena una segunda invocación del mismo
// botón): basta con esta subcadena para identificar la petición a manipular sin depender
// de si lleva sufijo.
const ACCION_CREAR = 'AsistenteNuevoExpediente-btnCrear-action';

// Título y mensaje del diálogo de error que debe mostrar el servidor al rechazar la
// petición manipulada (`TramitadorService.validateRepresentacion`).
const TITULO_ERROR = 'No es posible crear el expediente';
const MENSAJE_REPRESENTACION_NO_ADMITIDA =
  'Este trámite no permite presentar la solicitud en representación de otra persona';

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

/** Filas de datos del árbol de trámites (excluye cabecera y filas de agrupación). */
function filasDeTramites(page: Page) {
  return page.getByTestId('panel:tramitesPanel').locator('[role="row"][aria-level="2"]');
}

/** Filas de agrupación por tipo de trámite del árbol de trámites. */
function gruposDeTipoTramite(page: Page) {
  return page.getByTestId('panel:tramitesPanel').locator('[role="row"][aria-level="1"]');
}

/** Despliega todos los grupos de tipo de trámite del árbol (nace con los grupos plegados). */
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
 * Intercepta la petición que dispara "Crear expediente" (`POST /ws/action`) y sustituye,
 * en el `context` que viaja en el cuerpo, `presentadoEnRepresentacion` (que la pantalla no
 * llegó a preguntar, porque este trámite no admite representación, y que por tanto viaja
 * con su valor por defecto `false`) por `true` — es decir, "en representación de otra
 * persona" — dejando el resto de la petición tal cual la fijó la pantalla (centro,
 * trámite y presentado en papel). Es la manipulación que describe el paso 5 del
 * `.desc.md`. Las demás peticiones a `/ws/action` (login, menús, otras acciones del
 * asistente) siguen intactas.
 */
async function manipularParaQuienEsEnPeticionDeCrear(page: Page): Promise<void> {
  await page.route('**/ws/action', async (route: Route) => {
    const request = route.request();
    const body = request.postDataJSON() as
      | { data?: { context?: Record<string, unknown> }; action?: string }
      | undefined;
    const context = body?.data?.context;
    if (
      context &&
      typeof context['presentadoEnRepresentacion'] === 'boolean' &&
      String(body?.['action'] ?? '').includes(ACCION_CREAR)
    ) {
      context['presentadoEnRepresentacion'] = true;
      await route.continue({ postData: JSON.stringify(body) });
    } else {
      await route.continue();
    }
  });
}

test.describe('Ventanilla — Nuevo expediente', () => {
  test('Petición manipulada en representación en un trámite que no la admite', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el jefe de estudios `jefeestudios1@mislata.es` ha iniciado sesión
    // con la contraseña `demo1234`.
    await login(page, USUARIO, CONTRASENA);

    try {
      // Paso 2: Y ha llegado a "Nuevo expediente" para el trámite "Justificación de falta
      // del profesorado" en el centro "CIPFP Mislata", con "¿Cómo se presenta?" visible
      // (menú "Ventanilla" → "Nuevo expediente" → directamente a "elija el trámite",
      // porque el jefe de estudios solo tiene un centro → fila del trámite).
      await abrirNuevoExpediente(page);
      await desplegarGruposDeTramites(page);
      await filasDeTramites(page).filter({ hasText: TRAMITE }).click();

      // Nombre por regex, no exacto: Axelor añade un `*` al título de la pestaña en
      // cuanto el form tiene cambios sin guardar (aquí, desde que se marca "¿Cómo se
      // presenta?"), y la pantalla sigue siendo la misma antes y después de ese marcador.
      const pantallaContexto = page.getByRole('tab', { name: /^Nuevo expediente\*?$/ });
      await expect(pantallaContexto).toBeVisible();
      const campoTramite = page.getByTestId('field:nombreTramite').getByRole('textbox');
      await expect(campoTramite).toHaveValue(TRAMITE);
      const campoCentro = page.getByTestId('field:centro').getByRole('textbox');
      await expect(campoCentro).toHaveValue(CENTRO);

      // "¿Cómo se presenta?" SÍ se ve (el jefe de estudios tiene las dos formas de
      // presentar este trámite: él mismo o registrándolo en papel), sin ninguna opción
      // marcada. "¿Para quién es el expediente?" no se ve todavía: el trámite no admite
      // representación.
      await expect(page.getByText(PREGUNTA_COMO_SE_PRESENTA)).toBeVisible();
      const radioLoPresentoYo = opcionComoSePresenta(page, OPCION_LO_PRESENTO_YO);
      const radioEnPapel = opcionComoSePresenta(page, OPCION_EN_PAPEL);
      await expect(radioLoPresentoYo).toBeVisible();
      await expect(radioEnPapel).toBeVisible();
      await expect(radioLoPresentoYo).not.toBeChecked();
      await expect(radioEnPapel).not.toBeChecked();
      await expect(page.getByText(PREGUNTA_PARA_QUIEN)).toHaveCount(0);
      await expect(page.getByTestId('field:presentadoEnRepresentacion')).toHaveCount(0);

      // Paso 3: Cuando marca "Estoy registrando un trámite recibido en papel". El
      // `onChange` del campo va al servidor a recalcular qué hay que preguntar; se espera
      // esa respuesta ANTES de comprobar la ausencia de "¿Para quién es el expediente?",
      // para no comprobarlo sobre la pantalla previa a la respuesta del servidor.
      const recalculo = page.waitForResponse(
        (respuesta) =>
          respuesta.url().endsWith('/ws/action') &&
          (respuesta.request().postData() ?? '').includes(
            'AsistenteNuevoExpediente-onChange-presentadoEnPapel-action',
          ),
      );
      await radioEnPapel.click();
      await recalculo;

      // Paso 4: Entonces sigue sin verse "¿Para quién es el expediente?", porque el
      // trámite no admite representación. Control positivo de esa ausencia: la opción que
      // el usuario acaba de marcar quedó marcada y la otra no, o sea que el panel de la
      // presentación sigue vivo y recalculado.
      await expect(radioEnPapel).toBeChecked();
      await expect(radioLoPresentoYo).not.toBeChecked();
      await expect(page.getByText(PREGUNTA_PARA_QUIEN)).toHaveCount(0);
      await expect(page.getByTestId('field:presentadoEnRepresentacion')).toHaveCount(0);

      // Paso 5: Cuando pulsa "Crear expediente" con la petición manipulada para indicar
      // que el expediente es en representación de otra persona, dejando el resto como
      // está en la pantalla: el centro "CIPFP Mislata", el trámite "Justificación de
      // falta del profesorado" y registrado como recibido en papel.
      await manipularParaQuienEsEnPeticionDeCrear(page);
      await page.getByRole('button', { name: 'Crear expediente' }).click();

      // Resultado esperado: bajo el título "No es posible crear el expediente" muestra el
      // mensaje "Este trámite no permite presentar la solicitud en representación de otra
      // persona".
      const aviso = page.getByRole('dialog');
      await expect(aviso).toBeVisible();
      await expect(aviso).toContainText(TITULO_ERROR);
      await expect(aviso).toContainText(MENSAJE_REPRESENTACION_NO_ADMITIDA);

      // El aviso es modal: la comprobación de que la pantalla "Nuevo expediente" sigue
      // abierta (y que no se ha creado ningún expediente) solo es observable tras
      // aceptarlo, que es además la única salida que el diálogo ofrece al usuario.
      await aviso.getByRole('button', { name: 'Aceptar' }).click();
      await expect(aviso).toHaveCount(0);

      // Resultado esperado: el sistema no crea ningún expediente. La pantalla "Nuevo
      // expediente" sigue abierta con el trámite "Justificación de falta del
      // profesorado" y el centro "CIPFP Mislata", con "Estoy registrando un trámite
      // recibido en papel" todavía marcado; no se abre ningún expediente (su pestaña
      // llevaría el patrón "<nº>/<año>-<trámite>", nunca el título fijo "Nuevo
      // expediente").
      await expect(pantallaContexto).toBeVisible();
      await expect(campoTramite).toHaveValue(TRAMITE);
      await expect(campoCentro).toHaveValue(CENTRO);
      await expect(radioEnPapel).toBeChecked();
      await expect(radioLoPresentoYo).not.toBeChecked();
      await expect(page.getByRole('tab', { name: new RegExp(`^\\d+/\\d{4}-${TRAMITE}`) })).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Crear expediente' })).toBeVisible();
    } finally {
      // Teardown: si una aserción falló con el aviso todavía en pantalla, aceptarlo para
      // no dejar el modal abierto al siguiente run. Si ya se aceptó (camino verde) el
      // botón no existe: el `.catch` es intencional, no un error tapado.
      await page
        .getByRole('dialog')
        .getByRole('button', { name: 'Aceptar' })
        .click({ timeout: 5000 })
        .catch(() => {});
      await page.unroute('**/ws/action');

      await logout(page);
    }
  });
});
