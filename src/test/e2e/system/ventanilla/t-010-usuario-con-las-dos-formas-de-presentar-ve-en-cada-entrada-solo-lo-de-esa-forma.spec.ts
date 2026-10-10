import { test, expect, Locator, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-010 — Usuario con las dos formas de presentar ve en cada entrada solo lo de esa forma
// origen: ESC-010 (reescrito a mano al fijar la forma de presentar por la entrada de menú)

/**
 * IDEMPOTENCIA — test de SOLO LECTURA: recorre el asistente por las dos entradas hasta su
 * último paso y lo cierra con «Atrás» + «Cancelar», sin crear nada. No hay nada que aislar
 * ni que borrar.
 *
 * Qué prueba: la forma de presentar ya no se pregunta, la fija la entrada de menú, y el
 * asistente calcula lo que ofrece con el perfil de ESA forma. `administrativo2@mislata.es`
 * es Administrativo (perfil TRAMITADOR: registra en papel los trámites del alumno) y Alumno
 * (perfil CREADOR: los presenta él mismo).
 * Con los datos de demo las dos listas de trámites coinciden (sus dos perfiles alcanzan el
 * mismo trámite), así que la diferencia por entrada se observa en la pregunta del último
 * paso, que sale de evaluar los permisos SOLO con el perfil de la entrada:
 *   - por «Mis trámites» (CREADOR), como alumno que no es familiar, solo puede ser «Para la persona que lo presenta»:
 *     no se pregunta «¿Para quién es el expediente?»;
 *   - por «Tramitación» (TRAMITADOR, en papel), en un trámite que admite representación,
 *     siempre se pregunta.
 * Las listas distintas por entrada las prueban T-009 (administrativo1: solo «Tramitación»)
 * y T-029 (director: solo «Mis trámites»).
 */

const USUARIO = 'administrativo2@mislata.es';
const CONTRASENA = 'demo1234';

const CENTRO = 'CIPFP Mislata';
const TRAMITE = 'Anulación de matrícula en ciclo formativo';
const TIPO_TRAMITE_ALUMNO = 'Trámites para el alumno';

const PANTALLA_TRAMITE = 'Nuevo expediente: elija el trámite';
const PANTALLA_CONTEXTO = 'Nuevo expediente';

const PREGUNTA_COMO_SE_PRESENTA = '¿Cómo se presenta?';
const PREGUNTA_PARA_QUIEN = '¿Para quién es el expediente?';
const OPCION_PARA_MI = 'Para la persona que lo presenta';
const OPCION_EN_REPRESENTACION =
  'Para otra persona a la que representa quien lo presenta (hijo/a menor de edad o persona tutelada)';

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

// Entrada de cada grupo con la que se crea un expediente.
const ENTRADA_NUEVO_TRAMITE = {
  misTramites: 'misTramites-nuevoTramite-menuitem',
  tramitacion: 'tramitacion-tramiteEnPapel-menuitem',
} as const;

/** Abre «Mis trámites» → «Nuevo trámite» o «Tramitación» → «Trámite en papel»; el grupo solo se despliega si la entrada no se ve. */
async function abrirNuevoTramite(page: Page, grupo: keyof typeof ENTRADA_NUEVO_TRAMITE): Promise<void> {
  const entrada = page.getByTestId(`item:${ENTRADA_NUEVO_TRAMITE[grupo]}`);
  if (!(await entrada.isVisible())) {
    await page.getByTestId(`item:${grupo}-menuitem`).getByTestId('title').first().click();
  }
  await entrada.click();
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
 * Comprueba el paso 2 (un solo centro, así que se abre directamente) y que solo se ofrece
 * «Anulación de matrícula en ciclo formativo», y pulsa su fila para llegar al paso 3.
 */
async function elegirElTramite(page: Page): Promise<void> {
  await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE, exact: true })).toBeVisible();
  await expect(page.getByTestId('field:centro').getByRole('textbox')).toHaveValue(CENTRO);
  await desplegarGruposDeTramites(page);
  await expect(gruposDeTipoTramite(page)).toHaveCount(1);
  await expect(gruposDeTipoTramite(page).first()).toContainText(TIPO_TRAMITE_ALUMNO);
  await expect(filasDeTramites(page)).toHaveText([TRAMITE]);
  await filasDeTramites(page).first().click();

  await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO, exact: true })).toBeVisible();
  await expect(page.getByTestId('field:nombreTramite').getByRole('textbox')).toHaveValue(TRAMITE);
  // En ninguna de las dos entradas se pregunta cómo se presenta.
  await expect(page.getByText(PREGUNTA_COMO_SE_PRESENTA)).toHaveCount(0);
  await expect(page.getByTestId('field:presentadoEnPapel')).toHaveCount(0);
}

/** Cierra el asistente desde el paso 3: «Atrás» al paso 2 y, como no hubo centro, «Cancelar». */
async function cerrarAsistente(page: Page): Promise<void> {
  await page.getByTestId('panel:buttons-panel').getByRole('button', { name: 'Atrás' }).click();
  await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE, exact: true })).toBeVisible();
  await page.getByTestId('panel:buttons-panel').getByRole('button', { name: 'Cancelar' }).click();
  await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE, exact: true })).toHaveCount(0);
  await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO, exact: true })).toHaveCount(0);
}

test.describe('Ventanilla — Nuevo expediente', () => {
  test('Usuario con las dos formas de presentar ve en cada entrada solo lo de esa forma', async ({
    page,
  }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que `administrativo2@mislata.es` (Administrativo y Alumno) ha iniciado sesión.
    await login(page, USUARIO, CONTRASENA);

    try {
      // Paso 2: Cuando abre "Mis trámites" → "Nuevo trámite" y pulsa la fila de "Anulación
      // de matrícula en ciclo formativo".
      await abrirNuevoTramite(page, 'misTramites');
      await elegirElTramite(page);

      // Paso 3: Entonces "Nuevo expediente" NO pregunta "¿Cómo se presenta?" ni "¿Para
      // quién es el expediente?": presentándolo él mismo, como alumno que no es familiar,
      // solo puede ser para él. Control positivo de que la pantalla está viva: "Crear
      // expediente" sí está.
      await expect(page.getByText(PREGUNTA_PARA_QUIEN)).toHaveCount(0);
      await expect(page.getByTestId('field:presentadoEnRepresentacion')).toHaveCount(0);
      await expect(page.getByRole('button', { name: 'Crear expediente' })).toBeVisible();

      // Paso 4: Cuando pulsa "Atrás" y "Cancelar".
      await cerrarAsistente(page);

      // Paso 5: Y abre "Tramitación" → "Trámite en papel" y pulsa la misma fila.
      await abrirNuevoTramite(page, 'tramitacion');
      await elegirElTramite(page);

      // Paso 6: Entonces "Nuevo expediente" sigue sin preguntar "¿Cómo se presenta?" (lo
      // comprueba `elegirElTramite`) pero SÍ pregunta "¿Para quién es el expediente?", con
      // sus dos opciones sin marcar: registrando en papel, el trámite admite las dos.
      await expect(page.getByText(PREGUNTA_PARA_QUIEN)).toBeVisible();
      const radioParaMi = opcionParaQuien(page, OPCION_PARA_MI);
      const radioEnRepresentacion = opcionParaQuien(page, OPCION_EN_REPRESENTACION);
      await expect(radioParaMi).toBeVisible();
      await expect(radioEnRepresentacion).toBeVisible();
      await expect(radioParaMi).not.toBeChecked();
      await expect(radioEnRepresentacion).not.toBeChecked();

      // Paso 7: Cuando pulsa "Atrás" y "Cancelar", el asistente se cierra.
      await cerrarAsistente(page);
    } finally {
      await logout(page);
    }
  });
});
