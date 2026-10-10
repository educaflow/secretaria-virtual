import { test, expect, Locator, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-029 — Usuario que no puede registrar en papel en ningún centro entra por «Tramitación»
// origen: —  |  escrito a mano al fijar la forma de presentar por la entrada de menú (sin fuente en .sdd)

/**
 * IDEMPOTENCIA — test de SOLO LECTURA: abre el asistente por las dos entradas y lo cierra
 * (con «Cancelar» o aceptando el aviso) sin crear nada, así que no hay nada que aislar ni
 * que borrar. El `finally` acepta el aviso si una aserción falló con él en pantalla.
 *
 * Por qué el director: ve el grupo «Tramitación» (lo ven los usuarios con algún perfil de
 * tramitación, y él lo tiene sobre expedientes ya iniciados) pero NO tiene permiso para
 * iniciar ningún trámite registrándolo en papel en ninguno de sus centros. Un alumno
 * (p. ej. `alumno1@mislata.es`) no sirve para este caso: ni siquiera ve el grupo
 * «Tramitación», así que no puede llegar a la entrada.
 */

const USUARIO = 'director@mislata.es';
const CONTRASENA = 'demo1234';

const CENTRO = 'CIPFP Mislata';
const TIPO_TRAMITE_PROFESOR = 'Trámites para el profesor';
// Lo que el director SÍ puede iniciar presentándolo él mismo (como profesor), por orden
// alfabético: es el contraste que prueba que la lista depende de la entrada.
const TRAMITES_DEL_PROFESOR = ['Justificación de falta del profesorado', 'Trámite de prueba'];

const PANTALLA_CENTRO = 'Nuevo expediente: elija el centro';
const PANTALLA_TRAMITE = 'Nuevo expediente: elija el trámite';
const PANTALLA_CONTEXTO = 'Nuevo expediente';

const AVISO = 'No puede crear expedientes en ninguno de sus centros';

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

test.describe('Ventanilla — Nuevo expediente', () => {
  test('Usuario que no puede registrar en papel en ningún centro entra por «Tramitación»', async ({
    page,
  }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el director `director@mislata.es` ha iniciado sesión.
    await login(page, USUARIO, CONTRASENA);

    try {
      // Paso 2: Cuando abre "Mis trámites" → "Nuevo trámite".
      await abrirNuevoTramite(page, 'misTramites');

      // Paso 3: Entonces se abre "Nuevo expediente: elija el trámite" con "CIPFP Mislata" y
      // únicamente "Trámites para el profesor" con sus dos trámites (los que puede presentar
      // él mismo como profesor).
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE, exact: true })).toBeVisible();
      await expect(page.getByTestId('field:centro').getByRole('textbox')).toHaveValue(CENTRO);
      await desplegarGruposDeTramites(page);
      await expect(gruposDeTipoTramite(page)).toHaveCount(1);
      await expect(gruposDeTipoTramite(page).first()).toContainText(TIPO_TRAMITE_PROFESOR);
      await expect(filasDeTramites(page)).toHaveText(TRAMITES_DEL_PROFESOR);

      // Paso 4: Cuando pulsa "Cancelar".
      await page.getByTestId('panel:buttons-panel').getByRole('button', { name: 'Cancelar' }).click();
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE, exact: true })).toHaveCount(0);

      // Paso 5: Y abre "Tramitación" → "Trámite en papel".
      await abrirNuevoTramite(page, 'tramitacion');

      // Resultado esperado: el sistema muestra el aviso "No puede crear expedientes en
      // ninguno de sus centros" (diálogo modal de información de Axelor).
      const aviso = page.getByRole('dialog');
      await expect(aviso).toBeVisible();
      await expect(aviso).toContainText(AVISO);

      // El estado final solo es observable tras aceptar el aviso, que es modal.
      await aviso.getByRole('button', { name: 'Aceptar' }).click();
      await expect(aviso).toHaveCount(0);

      // Resultado esperado: no queda abierta ninguna pantalla del asistente, ni por su
      // pestaña ni por su contenido.
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO })).toHaveCount(0);
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE })).toHaveCount(0);
      await expect(page.getByRole('tab', { name: PANTALLA_CONTEXTO, exact: true })).toHaveCount(0);
      await expect(page.getByTestId('panel:centrosPanel')).toHaveCount(0);
      await expect(page.getByTestId('panel:tramitesPanel')).toHaveCount(0);
      await expect(page.getByTestId('panel:tramitePanel')).toHaveCount(0);
    } finally {
      // Si una aserción falló con el aviso en pantalla, aceptarlo; en el camino verde ya no
      // existe y el `.catch` es intencional.
      await page
        .getByRole('dialog')
        .getByRole('button', { name: 'Aceptar' })
        .click({ timeout: 5000 })
        .catch(() => {});

      await logout(page);
    }
  });
});
