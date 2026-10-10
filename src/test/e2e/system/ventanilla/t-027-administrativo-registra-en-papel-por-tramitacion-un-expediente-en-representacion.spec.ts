import { test, expect, Locator, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-027 — Administrativo registra en papel por «Tramitación» un expediente en representación
// origen: —  |  escrito a mano al fijar la forma de presentar por la entrada de menú (sin fuente en .sdd)

/**
 * IDEMPOTENCIA — este test CREA un expediente, y su número lo asigna el servidor con un
 * contador, así que no hay ningún nombre que aislar con un sufijo `Date.now()`:
 *   - el expediente creado se identifica por SU número, capturado del título de su pestaña
 *     en tiempo de ejecución (nunca un número fijo), y
 *   - se BORRA en el `finally`, reabriéndolo desde «Tramitación» → «Pendientes de mí»,
 *     donde el servidor lo abre con el perfil de su estado (TRAMITADOR), que es el que
 *     ofrece «Borrar el expediente» en PENDIENTE_DOCUMENTO_ESCANEADO. Arrancar el teardown
 *     con un `goto` lo hace robusto aunque el test falle a medio navegar.
 * No hace falta pre-limpieza defensiva: ningún límite de negocio impide registrar otro
 * expediente de este trámite, y el test solo mira el suyo, filtrado por su número.
 */

// administrativo1@mislata.es solo tiene el perfil TRAMITADOR sobre los trámites del alumno:
// los registra en papel, entrando por «Tramitación» → «Trámite en papel».
const USUARIO = 'administrativo1@mislata.es';
const CONTRASENA = 'demo1234';
const ADMINISTRATIVO_NOMBRE_COMPLETO = 'Administrativo1 CIPFP Mislata';

const CENTRO = 'CIPFP Mislata';
const TRAMITE = 'Anulación de matrícula en ciclo formativo';
const TIPO_TRAMITE_ALUMNO = 'Trámites para el alumno';
const TIPO_TRAMITE_PROFESOR = 'Trámites para el profesor';

const PANTALLA_CENTRO = 'Nuevo expediente: elija el centro';
const PANTALLA_TRAMITE = 'Nuevo expediente: elija el trámite';
const PANTALLA_CONTEXTO = 'Nuevo expediente';

const PREGUNTA_COMO_SE_PRESENTA = '¿Cómo se presenta?';
const PREGUNTA_PARA_QUIEN = '¿Para quién es el expediente?';
const OPCION_PARA_MI = 'Para la persona que lo presenta';
const OPCION_EN_REPRESENTACION =
  'Para otra persona a la que representa quien lo presenta (hijo/a menor de edad o persona tutelada)';

// Primer estado cuando se registra EN PAPEL (`InitialEventManagerImpl`): quien lo presenta él
// mismo arranca en ENTRADA_DATOS, así que este estado ya delata la forma de presentar.
const FASE_INICIAL = 'Entrada';
const ESTADO_INICIAL_EN_PAPEL = 'Pendiente de adjuntar la solicitud en papel escaneada';
const AVISO_EN_PAPEL =
  'Adjunte escaneada en PDF la solicitud que ha entregado firmada la persona que la presenta';

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

/**
 * Lee del servidor el expediente `numero` TAL Y COMO QUEDÓ PERSISTIDO. El formulario de
 * PENDIENTE_DOCUMENTO_ESCANEADO no pinta el panel del solicitante (su `<include-panels>` es
 * una allowlist que no lo incluye), así que la representación no se puede comprobar en la
 * pantalla sin que la comprobación sea vacua. Se lee con `fetch` desde la página para
 * reutilizar la sesión y el `CSRF-TOKEN` del navegador.
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

/** Filas hoja (los trámites) del árbol de trámites. */
function filasDeTramites(page: Page): Locator {
  return page.getByTestId('panel:tramitesPanel').locator('[role="row"][aria-level="2"]');
}

/** Filas de agrupación (los tipos de trámite) del árbol de trámites. */
function gruposDeTipoTramite(page: Page): Locator {
  return page.getByTestId('panel:tramitesPanel').locator('[role="row"][aria-level="1"]');
}

/** Despliega todos los grupos del árbol, que nacen plegados. */
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
 * El radio de una opción de «¿Para quién es el expediente?». Axelor pinta cada opción como
 * `<div><input type="radio"><span>texto</span></div>` sin `<label>`, así que se localiza el
 * `div` que contiene el texto y, dentro, su radio.
 */
function opcionParaQuien(page: Page, texto: string): Locator {
  return page
    .getByTestId('field:presentadoEnRepresentacion')
    .locator('div:has(> [data-testid="radio"])')
    .filter({ hasText: texto })
    .getByRole('radio');
}

/** Abre una entrada del menú lateral; el grupo solo se despliega si la entrada no se ve. */
async function abrirEntradaDeMenu(page: Page, grupo: string, entrada: string): Promise<void> {
  const item = page.getByTestId(`item:${entrada}`);
  if (!(await item.isVisible())) {
    await page.getByTestId(`item:${grupo}`).getByTestId('title').first().click();
  }
  await item.click();
}

function filasDeExpedientes(page: Page): Locator {
  return page.getByRole('grid').locator('[data-testid^="row:"]');
}

/**
 * Borra el expediente `numero` reabriéndolo desde «Tramitación» → «Pendientes de mí»
 * (filtrando por su número para no depender de la paginación) y pulsando «Borrar el
 * expediente», que abre un diálogo de confirmación.
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
  test('Administrativo registra en papel por «Tramitación» un expediente en representación', async ({
    page,
  }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el administrativo `administrativo1@mislata.es` ha iniciado sesión.
    await login(page, USUARIO, CONTRASENA);

    let numeroExpediente = '';

    try {
      // Paso 2: Cuando abre el menú "Tramitación" y pulsa "Trámite en papel" (es la primera
      // entrada del grupo).
      await page.getByTestId('item:tramitacion-menuitem').getByTestId('title').first().click();
      const entradas = page
        .getByTestId('item:tramitacion-menuitem')
        .locator('[data-testid^="item:tramitacion-"][data-testid$="-menuitem"]');
      await expect(entradas.first()).toHaveAttribute('data-testid', 'item:tramitacion-tramiteEnPapel-menuitem');
      await abrirEntradaDeMenu(page, 'tramitacion-menuitem', 'tramitacion-tramiteEnPapel-menuitem');

      // Paso 3: Entonces se abre directamente "Nuevo expediente: elija el trámite" (un solo
      // centro) con "CIPFP Mislata" y únicamente "Trámites para el alumno" → "Anulación de
      // matrícula en ciclo formativo", y un único botón, "Cancelar".
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE, exact: true })).toBeVisible();
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO, exact: true })).toHaveCount(0);
      await expect(page.getByTestId('field:centro').getByRole('textbox')).toHaveValue(CENTRO);
      await desplegarGruposDeTramites(page);
      await expect(gruposDeTipoTramite(page)).toHaveCount(1);
      await expect(gruposDeTipoTramite(page).first()).toContainText(TIPO_TRAMITE_ALUMNO);
      await expect(filasDeTramites(page)).toHaveText([TRAMITE]);
      await expect(page.getByText(TIPO_TRAMITE_PROFESOR)).toHaveCount(0);
      const botonera = page.getByTestId('panel:buttons-panel');
      await expect(botonera.getByRole('button')).toHaveCount(1);
      await expect(botonera.getByRole('button', { name: 'Cancelar' })).toBeVisible();

      // Paso 4: Cuando pulsa la fila "Anulación de matrícula en ciclo formativo".
      await filasDeTramites(page).first().click();

      // Paso 5: Entonces se abre "Nuevo expediente" con el trámite y el centro en solo
      // lectura, SIN "¿Cómo se presenta?" (la forma la fija la entrada) y CON "¿Para quién
      // es el expediente?" (en papel, en un trámite que admite representación, siempre se
      // pregunta), con sus dos opciones sin marcar.
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO, exact: true })).toBeVisible();
      await expect(page.getByTestId('field:nombreTramite').getByRole('textbox')).toHaveValue(TRAMITE);
      await expect(page.getByTestId('field:centro').getByRole('textbox')).toHaveValue(CENTRO);
      await expect(page.getByText(PREGUNTA_COMO_SE_PRESENTA)).toHaveCount(0);
      await expect(page.getByTestId('field:presentadoEnPapel')).toHaveCount(0);
      await expect(page.getByText(PREGUNTA_PARA_QUIEN)).toBeVisible();
      const radioParaMi = opcionParaQuien(page, OPCION_PARA_MI);
      const radioEnRepresentacion = opcionParaQuien(page, OPCION_EN_REPRESENTACION);
      await expect(radioParaMi).toBeVisible();
      await expect(radioEnRepresentacion).toBeVisible();
      await expect(radioParaMi).not.toBeChecked();
      await expect(radioEnRepresentacion).not.toBeChecked();

      // Paso 6: Cuando marca "Para otra persona a la que representa quien lo presenta…" y pulsa "Crear
      // expediente".
      await radioEnRepresentacion.click();
      await expect(radioEnRepresentacion).toBeChecked();
      await expect(radioParaMi).not.toBeChecked();
      // En papel el idioma del expediente es obligatorio: se elige explícitamente para no
      // depender del que el asistente propone (el del usuario que registra).
      await page.getByTestId('field:idioma').getByRole('combobox').click();
      await page.getByRole('option', { name: 'Castellano', exact: true }).click();
      await page.getByRole('button', { name: 'Crear expediente' }).click();

      // Resultado esperado: se abre el expediente en su primer estado PARA EL PAPEL.
      const pestanaExpediente = page.getByRole('tab', { name: TITULO_EXPEDIENTE });
      await expect(pestanaExpediente).toBeVisible();
      const tituloExpediente = (await pestanaExpediente.getByTestId('title').innerText()).trim();
      numeroExpediente = tituloExpediente.slice(0, -SUFIJO_TITULO_EXPEDIENTE.length);
      await expect(page.getByTestId('field:namePhase').getByRole('textbox')).toHaveValue(FASE_INICIAL);
      await expect(page.getByTestId('field:nameState').getByRole('textbox')).toHaveValue(
        ESTADO_INICIAL_EN_PAPEL,
      );
      await expect(page.getByTestId('panel:solicitud-escaneada-upload')).toBeVisible();
      await expect(page.getByText(AVISO_EN_PAPEL)).toBeVisible();

      // …el asistente se ha cerrado.
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE, exact: true })).toHaveCount(0);
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO, exact: true })).toHaveCount(0);

      // …y lo ha registrado el administrativo.
      await expect(page.getByTestId('field:createdBy').getByRole('textbox')).toHaveValue(
        ADMINISTRATIVO_NOMBRE_COMPLETO,
      );

      // Resultado esperado (lo PERSISTIDO): en "CIPFP Mislata", registrado en papel por el
      // administrativo y EN REPRESENTACIÓN de otra persona.
      const persistido = await leerExpedientePersistido(page, numeroExpediente);
      expect(persistido.numeroExpediente).toBe(numeroExpediente);
      expect(persistido['centro.name']).toBe(CENTRO);
      expect(persistido.presentadoEnPapel).toBe(true);
      expect(persistido.presentadoEnRepresentacion).toBe(true);
      expect(persistido['usuarioRegistrador.name']).toBe(ADMINISTRATIVO_NOMBRE_COMPLETO);
      expect(persistido.codePhase).toBe('ENTRADA');
      expect(persistido.codeState).toBe('PENDIENTE_DOCUMENTO_ESCANEADO');
    } finally {
      // Teardown: borrar el expediente creado aunque una aserción haya fallado.
      if (numeroExpediente !== '') {
        await borrarExpediente(page, numeroExpediente);
      }

      await logout(page);
    }
  });
});
