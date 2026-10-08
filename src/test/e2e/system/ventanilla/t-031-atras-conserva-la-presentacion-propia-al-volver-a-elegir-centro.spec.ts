import { test, expect, Locator, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-031 — «Atrás» conserva la presentación propia al volver a elegir centro
// origen: —  |  escrito a mano al fijar la forma de presentar por la entrada de menú (sin fuente en .sdd)

/**
 * IDEMPOTENCIA — este test CREA un expediente cuyo número asigna el servidor:
 *   - se identifica por SU número, capturado del título de su pestaña, y
 *   - se BORRA en el `finally` reabriéndolo desde la bandeja de su estado (ver
 *     `borrarExpediente`), arrancando con un `goto` para que el teardown sea robusto aunque
 *     el test falle a medio navegar.
 * No hace falta pre-limpieza defensiva: ningún límite de negocio impide crear otro
 * expediente de este trámite y el test solo mira el suyo, por su número.
 *
 * Por qué `administrativo3@mislata.es`: es Administrativo, Alumno y Familiar en CIPFP Mislata
 * y en CIPFP Batoi, así que puede iniciar «Anulación de matrícula en ciclo formativo» en los
 * DOS centros por las DOS entradas, y el asistente le hace elegir centro. Es el mismo usuario
 * que T-030 (la variante en papel, por «Tramitación»): aquí entra por «Mis trámites».
 */

const USUARIO = 'administrativo3@mislata.es';
const CONTRASENA = 'demo1234';
const USUARIO_NOMBRE_COMPLETO = 'Administrativo3 CIPFP Mislata';

// Entrada de menú por la que se entra (la que fija la forma de presentar) y lo que esa
// forma implica en el expediente creado.
const GRUPO = 'misTramites';
const PRESENTADO_EN_PAPEL = false;
const ESTADO_INICIAL = 'Entrada de datos';
const CODE_STATE_INICIAL = 'ENTRADA_DATOS';
// Bandeja donde queda el expediente recién creado para quien lo creó, y desde la que el
// servidor lo abre con el perfil de su estado, que ofrece «Borrar el expediente».
const BANDEJA_BORRADO = { grupo: 'misTramites-menuitem', entrada: 'misTramites-pendientesDeMi-menuitem' };

const CENTROS = ['CIPFP Batoi', 'CIPFP Mislata'];
const CENTRO_PRIMERO = 'CIPFP Mislata';
const CENTRO_FINAL = 'CIPFP Batoi';
const TRAMITE = 'Anulación de matrícula en ciclo formativo';
const TIPO_TRAMITE_ALUMNO = 'Trámites para el alumno';

const PANTALLA_CENTRO = 'Nuevo expediente: elija el centro';
const PANTALLA_TRAMITE = 'Nuevo expediente: elija el trámite';
const PANTALLA_CONTEXTO = 'Nuevo expediente';

const PREGUNTA_COMO_SE_PRESENTA = '¿Cómo se presenta?';
const PREGUNTA_PARA_QUIEN = '¿Para quién es el expediente?';
const OPCION_PARA_MI = 'Para la persona que lo presenta';
const OPCION_EN_REPRESENTACION =
  'Para otra persona a la que representa quien lo presenta (hijo/a menor de edad o persona tutelada)';

const MODELO_EXPEDIENTE = 'com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1';
const SUFIJO_TITULO_EXPEDIENTE = `-${TRAMITE} V1`;
const TITULO_EXPEDIENTE = new RegExp(`^\\d+/\\d{4}-\\d+${SUFIJO_TITULO_EXPEDIENTE}$`);

interface ExpedientePersistido {
  numeroExpediente: string;
  presentadoEnPapel: boolean;
  presentadoEnRepresentacion: boolean;
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

function filasDeCentros(page: Page): Locator {
  return page.getByTestId('panel:centrosPanel').locator('[data-testid^="row:"]');
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

function opcionParaQuien(page: Page, texto: string): Locator {
  return page
    .getByTestId('field:presentadoEnRepresentacion')
    .locator('div:has(> [data-testid="radio"])')
    .filter({ hasText: texto })
    .getByRole('radio');
}

function botonDelAsistente(page: Page, nombre: string): Locator {
  return page.getByTestId('panel:buttons-panel').getByRole('button', { name: nombre });
}

/** Paso 1 del asistente: los dos centros, por orden de nombre, y solo «Cancelar». */
async function comprobarEleccionDeCentro(page: Page): Promise<void> {
  await expect(page.getByRole('tab', { name: PANTALLA_CENTRO, exact: true })).toBeVisible();
  await expect(filasDeCentros(page)).toHaveText(CENTROS);
  await expect(page.getByTestId('panel:buttons-panel').getByRole('button')).toHaveCount(1);
  await expect(botonDelAsistente(page, 'Cancelar')).toBeVisible();
}

/** Paso 2 del asistente: el centro elegido y únicamente la anulación de matrícula. */
async function comprobarEleccionDeTramite(page: Page, centro: string): Promise<void> {
  await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE, exact: true })).toBeVisible();
  await expect(page.getByTestId('field:centro').getByRole('textbox')).toHaveValue(centro);
  await desplegarGruposDeTramites(page);
  await expect(gruposDeTipoTramite(page)).toHaveCount(1);
  await expect(gruposDeTipoTramite(page).first()).toContainText(TIPO_TRAMITE_ALUMNO);
  await expect(filasDeTramites(page)).toHaveText([TRAMITE]);
  await expect(botonDelAsistente(page, 'Atrás')).toBeVisible();
}

/**
 * Paso 3 del asistente: el trámite y el centro, sin «¿Cómo se presenta?» y con «¿Para quién
 * es el expediente?» sin marcar (el usuario es alumno y familiar, así que presentándolo él
 * mismo valen las dos respuestas; en papel también: por las dos entradas se pregunta).
 */
async function comprobarContexto(page: Page, centro: string): Promise<void> {
  await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO, exact: true })).toBeVisible();
  await expect(page.getByTestId('field:nombreTramite').getByRole('textbox')).toHaveValue(TRAMITE);
  await expect(page.getByTestId('field:centro').getByRole('textbox')).toHaveValue(centro);
  await expect(page.getByText(PREGUNTA_COMO_SE_PRESENTA)).toHaveCount(0);
  await expect(page.getByTestId('field:presentadoEnPapel')).toHaveCount(0);
  await expect(page.getByText(PREGUNTA_PARA_QUIEN)).toBeVisible();
  await expect(opcionParaQuien(page, OPCION_PARA_MI)).not.toBeChecked();
  await expect(opcionParaQuien(page, OPCION_EN_REPRESENTACION)).not.toBeChecked();
}

function filasDeExpedientes(page: Page): Locator {
  return page.getByRole('grid').locator('[data-testid^="row:"]');
}

/** Borra el expediente `numero` reabriéndolo desde la bandeja de su estado inicial. */
async function borrarExpediente(page: Page, numero: string): Promise<void> {
  await page.goto('/#/');
  await abrirEntradaDeMenu(page, BANDEJA_BORRADO.grupo, BANDEJA_BORRADO.entrada);
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
  test('«Atrás» conserva la presentación propia al volver a elegir centro', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que `administrativo3@mislata.es` ha iniciado sesión.
    await login(page, USUARIO, CONTRASENA);

    let numeroExpediente = '';

    try {
      // Paso 2: Cuando abre "Mis trámites" → "Nuevo trámite".
      await abrirEntradaDeMenu(page, `${GRUPO}-menuitem`, `${GRUPO}-nuevoTramite-menuitem`);

      // Paso 3: Entonces se abre "Nuevo expediente: elija el centro" con "CIPFP Batoi" y
      // "CIPFP Mislata".
      await comprobarEleccionDeCentro(page);

      // Paso 4: Cuando pulsa "CIPFP Mislata" y, en "elija el trámite", la fila de "Anulación
      // de matrícula en ciclo formativo".
      await filasDeCentros(page).filter({ hasText: CENTRO_PRIMERO }).click();
      await comprobarEleccionDeTramite(page, CENTRO_PRIMERO);
      await filasDeTramites(page).first().click();

      // Paso 5: Entonces "Nuevo expediente" no pregunta cómo se presenta y sí para quién es.
      await comprobarContexto(page, CENTRO_PRIMERO);

      // Paso 6: Cuando pulsa "Atrás", vuelve a "elija el trámite" con "CIPFP Mislata" y la
      // misma lista…
      await botonDelAsistente(page, 'Atrás').click();
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO, exact: true })).toHaveCount(0);
      await comprobarEleccionDeTramite(page, CENTRO_PRIMERO);

      // …y cuando vuelve a pulsar "Atrás", vuelve a "elija el centro" con los mismos dos
      // centros.
      await botonDelAsistente(page, 'Atrás').click();
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE, exact: true })).toHaveCount(0);
      await comprobarEleccionDeCentro(page);

      // Paso 7: Cuando elige ahora "CIPFP Batoi", pulsa la fila del trámite, marca "Para la persona que lo presenta"
      // y pulsa "Crear expediente".
      await filasDeCentros(page).filter({ hasText: CENTRO_FINAL }).click();
      await comprobarEleccionDeTramite(page, CENTRO_FINAL);
      await filasDeTramites(page).first().click();
      await comprobarContexto(page, CENTRO_FINAL);
      const radioParaMi = opcionParaQuien(page, OPCION_PARA_MI);
      await radioParaMi.click();
      await expect(radioParaMi).toBeChecked();
      await page.getByRole('button', { name: 'Crear expediente' }).click();

      // Resultado esperado: se abre el expediente en el primer estado que corresponde a la
      // forma de la entrada por la que se entró, pese a las dos idas y vueltas con "Atrás".
      const pestanaExpediente = page.getByRole('tab', { name: TITULO_EXPEDIENTE });
      await expect(pestanaExpediente).toBeVisible();
      const tituloExpediente = (await pestanaExpediente.getByTestId('title').innerText()).trim();
      numeroExpediente = tituloExpediente.slice(0, -SUFIJO_TITULO_EXPEDIENTE.length);
      await expect(page.getByTestId('field:nameState').getByRole('textbox')).toHaveValue(ESTADO_INICIAL);
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO, exact: true })).toHaveCount(0);

      // Resultado esperado (lo PERSISTIDO): en "CIPFP Batoi", con la forma de presentar de la
      // entrada, para él mismo y registrado por él.
      const persistido = await leerExpedientePersistido(page, numeroExpediente);
      expect(persistido.numeroExpediente).toBe(numeroExpediente);
      expect(persistido['centro.name']).toBe(CENTRO_FINAL);
      expect(persistido.presentadoEnPapel).toBe(PRESENTADO_EN_PAPEL);
      expect(persistido.presentadoEnRepresentacion).toBe(false);
      expect(persistido.codeState).toBe(CODE_STATE_INICIAL);
      expect(persistido['usuarioRegistrador.name']).toBe(USUARIO_NOMBRE_COMPLETO);
    } finally {
      // Teardown: borrar el expediente creado aunque una aserción haya fallado.
      if (numeroExpediente !== '') {
        await borrarExpediente(page, numeroExpediente);
      }

      await logout(page);
    }
  });
});
