import { test, expect, Locator, Page, Request } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-032 — Cancelar por «Mis trámites» y entrar por «Tramitación» sin recargar registra en papel
// origen: —  |  escrito a mano como regresión del cliente (sin fuente en .sdd)

/**
 * IDEMPOTENCIA — este test CREA un expediente cuyo número asigna el servidor:
 *   - se identifica por SU número, capturado del título de su pestaña, y
 *   - se BORRA en el `finally` reabriéndolo desde «Tramitación» → «Pendientes de mí», donde
 *     el servidor lo abre con el perfil de su estado (TRAMITADOR), que ofrece «Borrar el
 *     expediente»; arranca con un `goto` para que el teardown sea robusto aunque el test
 *     falle a medio navegar.
 * No hace falta pre-limpieza defensiva: ningún límite de negocio impide crear otro
 * expediente de este trámite y el test solo mira el suyo, por su número.
 *
 * Qué prueba (regresión del cliente Axelor): al cerrar el asistente desde una acción
 * («Cancelar») la URL seguía apuntando a la entrada de menú por la que se abrió, y el
 * siguiente clic en un menú reabría ESA entrada a la vez que la nueva; los dos `onNew` de
 * «elija el centro» competían con `_presentadoEnPapel` distintos y ganaba la última
 * respuesta, así que el asistente abierto por «Tramitación» podía quedarse con la forma de
 * «Mis trámites» (no preguntaba «¿Para quién es el expediente?» y creaba el expediente como
 * presentado por el propio usuario). Por eso el test NO recarga entre las dos entradas, y
 * además de lo visible comprueba en la red que tras pulsar «Tramitación» solo hubo UN
 * `onNew` de «elija el centro», y con `_presentadoEnPapel: true`.
 *
 * Por qué `administrativo2@mislata.es`: es Administrativo (registra en papel los trámites
 * del alumno) y Alumno (los presenta él mismo) en un solo centro, así que la pregunta del
 * último paso delata la forma de presentar: por «Mis trámites» no se hace y por
 * «Tramitación» sí.
 */

const USUARIO = 'administrativo2@mislata.es';
const CONTRASENA = 'demo1234';
const USUARIO_NOMBRE_COMPLETO = 'Administrativo2 CIPFP Mislata';

const CENTRO = 'CIPFP Mislata';
const TRAMITE = 'Anulación de matrícula en ciclo formativo';
const TIPO_TRAMITE_ALUMNO = 'Trámites para el alumno';

const PANTALLA_CENTRO = 'Nuevo expediente: elija el centro';
const PANTALLA_TRAMITE = 'Nuevo expediente: elija el trámite';
const PANTALLA_CONTEXTO = 'Nuevo expediente';

const PREGUNTA_PARA_QUIEN = '¿Para quién es el expediente?';
const OPCION_PARA_MI = 'Para mí';
const OPCION_EN_REPRESENTACION =
  'Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)';

// Primer estado cuando se registra EN PAPEL; presentándolo él mismo arrancaría en
// «Entrada de datos», así que el estado ya delata la forma de presentar.
const FASE_INICIAL = 'Entrada';
const ESTADO_INICIAL_EN_PAPEL = 'Pendiente de adjuntar la solicitud en papel escaneada';

// Acción `onNew` de «elija el centro»: la comparten las dos entradas, que solo se
// diferencian por el `_presentadoEnPapel` de su contexto.
const ACCION_ON_NEW_ELEGIR_CENTRO = 'sysVentanilla.ElegirCentro@AsistenteNuevoExpediente-onNew-action';

// Acción de la entrada «Mis trámites» → «Nuevo trámite» (la que el fallo dejaba en la URL).
const ACCION_ENTRADA_MIS_TRAMITES = 'AsistenteNuevoExpedientePresentadoElectronicamente-action';

const MODELO_EXPEDIENTE = 'com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1';
const SUFIJO_TITULO_EXPEDIENTE = `-${TRAMITE} V1`;
const TITULO_EXPEDIENTE = new RegExp(`^\\d+/\\d{4}-\\d+${SUFIJO_TITULO_EXPEDIENTE}$`);

interface ExpedientePersistido {
  numeroExpediente: string;
  presentadoEnPapel: boolean;
  presentadoEnRepresentacion: boolean;
  codePhase: string;
  codeState: string;
  'centro.name': string;
  'usuarioRegistrador.name': string;
}

/** Lee del servidor el expediente `numero` tal y como quedó persistido (con la sesión del navegador). */
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
 * Si `request` es el `onNew` de «elija el centro», devuelve el `_presentadoEnPapel` de su
 * contexto (puede ser `undefined` si no viaja); si es otra petición, `null`.
 */
function presentadoEnPapelDelOnNew(request: Request): boolean | undefined | null {
  if (request.method() !== 'POST' || !request.url().endsWith('/ws/action')) {
    return null;
  }
  let cuerpo: { action?: string; data?: { context?: Record<string, unknown> } } | null;
  try {
    cuerpo = request.postDataJSON();
  } catch {
    return null;
  }
  // Solo el ARRANQUE del `onNew`: Axelor manda su acción como primer elemento de una lista
  // separada por comas. Cuando el grupo se interrumpe para mostrar un aviso, el cliente
  // reanuda el resto con otra petición `<acción>[<índice>]`, que es la misma ejecución y no
  // otro `onNew`.
  if (cuerpo?.action?.split(',')[0] !== ACCION_ON_NEW_ELEGIR_CENTRO) {
    return null;
  }
  return cuerpo.data?.context?._presentadoEnPapel as boolean | undefined;
}

function filasDeTramites(page: Page): Locator {
  return page.getByTestId('panel:tramitesPanel').locator('[role="row"][aria-level="2"]');
}

function gruposDeTipoTramite(page: Page): Locator {
  return page.getByTestId('panel:tramitesPanel').locator('[role="row"][aria-level="1"]');
}

/** Despliega todos los grupos del árbol de trámites, que nacen plegados. */
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

/** Abre una entrada del menú lateral; el grupo solo se despliega si la entrada no se ve. */
async function abrirEntradaDeMenu(page: Page, grupo: string, entrada: string): Promise<void> {
  const item = page.getByTestId(`item:${entrada}`);
  if (!(await item.isVisible())) {
    await page.getByTestId(`item:${grupo}`).getByTestId('title').first().click();
  }
  await item.click();
}

/** El radio de una opción de «¿Para quién es el expediente?» (localizado por su texto). */
function opcionParaQuien(page: Page, texto: string): Locator {
  return page
    .getByTestId('field:presentadoEnRepresentacion')
    .locator('div:has(> [data-testid="radio"])')
    .filter({ hasText: texto })
    .getByRole('radio');
}

/**
 * Comprueba el paso 2 (un solo centro, así que se abre directamente) con solo «Anulación
 * de matrícula en ciclo formativo» y pulsa su fila para llegar al paso 3.
 */
async function elegirElTramite(page: Page): Promise<void> {
  await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE, exact: true })).toHaveCount(1);
  await expect(page.getByRole('tab', { name: PANTALLA_CENTRO, exact: true })).toHaveCount(0);
  await expect(page.getByTestId('field:centro').getByRole('textbox')).toHaveValue(CENTRO);
  await desplegarGruposDeTramites(page);
  await expect(gruposDeTipoTramite(page)).toHaveCount(1);
  await expect(gruposDeTipoTramite(page).first()).toContainText(TIPO_TRAMITE_ALUMNO);
  await expect(filasDeTramites(page)).toHaveText([TRAMITE]);
  await filasDeTramites(page).first().click();

  await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO, exact: true })).toHaveCount(1);
  await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE, exact: true })).toHaveCount(0);
  await expect(page.getByTestId('field:nombreTramite').getByRole('textbox')).toHaveValue(TRAMITE);
  await expect(page.getByTestId('field:centro').getByRole('textbox')).toHaveValue(CENTRO);
}

function filasDeExpedientes(page: Page): Locator {
  return page.getByRole('grid').locator('[data-testid^="row:"]');
}

/** Borra el expediente `numero` reabriéndolo desde «Tramitación» → «Pendientes de mí». */
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
  test('Cancelar por «Mis trámites» y entrar por «Tramitación» sin recargar registra en papel', async ({
    page,
  }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que `administrativo2@mislata.es` (Administrativo y Alumno) ha iniciado sesión.
    await login(page, USUARIO, CONTRASENA);

    let numeroExpediente = '';
    // `_presentadoEnPapel` de cada `onNew` de «elija el centro» enviado a partir del paso 5.
    let capturando = false;
    const onNewsTrasTramitacion: (boolean | undefined)[] = [];
    page.on('request', (request) => {
      const presentadoEnPapel = presentadoEnPapelDelOnNew(request);
      if (capturando && presentadoEnPapel !== null) {
        onNewsTrasTramitacion.push(presentadoEnPapel);
      }
    });

    try {
      // Paso 2: Cuando abre "Mis trámites" → "Nuevo trámite" y pulsa la fila del trámite.
      await abrirEntradaDeMenu(page, 'misTramites-menuitem', 'misTramites-nuevoTramite-menuitem');
      await elegirElTramite(page);

      // Paso 3: Entonces "Nuevo expediente" no pregunta "¿Para quién es el expediente?"
      // (presentándolo él mismo, como alumno que no es familiar, solo puede ser para él).
      await expect(page.getByRole('button', { name: 'Crear expediente' })).toBeVisible();
      await expect(page.getByTestId('field:presentadoEnRepresentacion')).toHaveCount(0);

      // Paso 4: Cuando pulsa "Atrás" y, en "elija el trámite", "Cancelar", el asistente se
      // cierra.
      const botonera = page.getByTestId('panel:buttons-panel');
      await botonera.getByRole('button', { name: 'Atrás' }).click();
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE, exact: true })).toBeVisible();
      await botonera.getByRole('button', { name: 'Cancelar' }).click();
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE, exact: true })).toHaveCount(0);
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO, exact: true })).toHaveCount(0);
      // La causa del fallo: la URL se quedaba en la acción de la entrada "Mis trámites".
      await expect(page).not.toHaveURL(new RegExp(ACCION_ENTRADA_MIS_TRAMITES));

      // Paso 5: Y, SIN recargar la aplicación, abre "Tramitación" → "Nuevo trámite" y pulsa
      // la fila del trámite (con una sola pestaña del asistente cada vez: no se ha reabierto
      // la entrada anterior).
      capturando = true;
      await abrirEntradaDeMenu(page, 'tramitacion-menuitem', 'tramitacion-nuevoTramite-menuitem');
      await elegirElTramite(page);

      // Paso 6: Entonces "Nuevo expediente" SÍ pregunta "¿Para quién es el expediente?", con
      // sus dos opciones sin marcar: registrando en papel, el trámite admite las dos.
      await expect(page.getByText(PREGUNTA_PARA_QUIEN)).toBeVisible();
      const radioParaMi = opcionParaQuien(page, OPCION_PARA_MI);
      const radioEnRepresentacion = opcionParaQuien(page, OPCION_EN_REPRESENTACION);
      await expect(radioParaMi).not.toBeChecked();
      await expect(radioEnRepresentacion).not.toBeChecked();

      // Paso 7: Cuando marca "Para mí" y pulsa "Crear expediente".
      await radioParaMi.click();
      await expect(radioParaMi).toBeChecked();
      await page.getByRole('button', { name: 'Crear expediente' }).click();

      // Resultado esperado: se abre el expediente en su primer estado PARA EL PAPEL y el
      // asistente se ha cerrado.
      const pestanaExpediente = page.getByRole('tab', { name: TITULO_EXPEDIENTE });
      await expect(pestanaExpediente).toBeVisible();
      const tituloExpediente = (await pestanaExpediente.getByTestId('title').innerText()).trim();
      numeroExpediente = tituloExpediente.slice(0, -SUFIJO_TITULO_EXPEDIENTE.length);
      await expect(page.getByTestId('field:namePhase').getByRole('textbox')).toHaveValue(FASE_INICIAL);
      await expect(page.getByTestId('field:nameState').getByRole('textbox')).toHaveValue(
        ESTADO_INICIAL_EN_PAPEL,
      );
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO, exact: true })).toHaveCount(0);
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE, exact: true })).toHaveCount(0);
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO, exact: true })).toHaveCount(0);

      // Resultado esperado (lo PERSISTIDO): registrado en papel, para él mismo, en
      // "CIPFP Mislata" y con él como registrador.
      const persistido = await leerExpedientePersistido(page, numeroExpediente);
      expect(persistido.numeroExpediente).toBe(numeroExpediente);
      expect(persistido['centro.name']).toBe(CENTRO);
      expect(persistido.presentadoEnPapel).toBe(true);
      expect(persistido.presentadoEnRepresentacion).toBe(false);
      expect(persistido['usuarioRegistrador.name']).toBe(USUARIO_NOMBRE_COMPLETO);
      expect(persistido.codePhase).toBe('ENTRADA');
      expect(persistido.codeState).toBe('PENDIENTE_DOCUMENTO_ESCANEADO');

      // Resultado esperado (la RED): tras pulsar "Tramitación" solo hubo un `onNew` de
      // "elija el centro", el de esa entrada; ninguno de "Mis trámites" compitió con él.
      expect(onNewsTrasTramitacion).toEqual([true]);
    } finally {
      capturando = false;
      // Teardown: borrar el expediente creado aunque una aserción haya fallado.
      if (numeroExpediente !== '') {
        await borrarExpediente(page, numeroExpediente);
      }

      await logout(page);
    }
  });
});
