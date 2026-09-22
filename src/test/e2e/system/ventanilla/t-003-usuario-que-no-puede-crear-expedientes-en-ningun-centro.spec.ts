import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-003 — Usuario que no puede crear expedientes en ningún centro
// origen: ESC-003  |  verifica: U-nuevo-expediente-001, R-AsistenteNuevoExpediente-003
// fuente: .sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/test-e2e-desc/t-003-usuario-que-no-puede-crear-expedientes-en-ningun-centro.desc.md

/**
 * IDEMPOTENCIA (§4 del contrato de generación) — este test es de SOLO LECTURA: abre el
 * asistente de nuevo expediente con un exalumno que no puede iniciar ningún trámite en
 * ninguno de sus centros y comprueba que el sistema lo avisa y no le deja ninguna
 * pantalla abierta. No crea, modifica ni borra ninguna fila, así que no hay nombre que
 * aislar con un sufijo `Date.now()` ni nada que borrar en el teardown. Lo único que el
 * test puede dejar en pantalla es el propio aviso (y, detrás, la pestaña del asistente
 * que se cierra al aceptarlo): el `finally` lo acepta para que un run no herede el
 * diálogo del anterior. Tampoco hace falta pre-limpieza defensiva: el aviso depende solo
 * de los datos maestros (centros, trámites y permisos del estado inicial), que ningún
 * test modifica; un run que abortara no puede dejar nada pegado que cambie estas
 * aserciones.
 */

// Credenciales del usuario de la precondición (tabla «Usuarios de acceso» del .desc.md):
// exalumno de CIPFP Mislata, sin permiso para iniciar ningún trámite en ningún centro.
const USUARIO = 'exalumno1@mislata.es';
const CONTRASENA = 'demo1234';

// Texto literal del aviso (`<info message="…">` de
// `system/ventanilla/views/ElegirCentro-AsistenteNuevoExpediente.xml`).
const AVISO = 'No puede crear expedientes en ninguno de sus centros';

// Títulos de las TRES pantallas del asistente (los fijan los `action-view` de
// `system/ventanilla/views/`): ninguna debe quedar abierta. Se comparan con regex
// anclada porque Axelor añade un `*` al título de la pestaña cuando el formulario está
// sucio ("Nuevo expediente: elija el centro*"), y porque "Nuevo expediente" a secas es
// prefijo de las otras dos (sin anclar, cazaría pestañas que no son la suya).
const PANTALLA_CENTRO = /^Nuevo expediente: elija el centro\*?$/;
const PANTALLA_TRAMITE = /^Nuevo expediente: elija el trámite\*?$/;
const PANTALLA_MAIN = /^Nuevo expediente\*?$/;

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

test.describe('Ventanilla — Nuevo expediente', () => {
  test('Usuario que no puede crear expedientes en ningún centro', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el exalumno `exalumno1@mislata.es` ha iniciado sesión con la
    // contraseña `demo1234`.
    await login(page, USUARIO, CONTRASENA);

    try {
      // Paso 2: Cuando abre el menú "Ventanilla" y pulsa "Nuevo expediente".
      await abrirNuevoExpediente(page);

      // Resultado esperado: el sistema muestra el aviso "No puede crear expedientes en
      // ninguno de sus centros". Es un diálogo modal de información de Axelor, así que
      // se comprueba dentro del propio diálogo (no en cualquier parte de la página).
      const aviso = page.getByRole('dialog');
      await expect(aviso).toBeVisible();
      await expect(aviso).toContainText(AVISO);

      // El aviso es modal y tapa la aplicación: el estado final en el que "no queda
      // abierta ninguna pantalla del asistente" solo es observable tras aceptarlo, que
      // es además la única salida que el diálogo ofrece al usuario.
      await aviso.getByRole('button', { name: 'Aceptar' }).click();
      await expect(aviso).toHaveCount(0);

      // Resultado esperado: no queda abierta ninguna pantalla del asistente: ni "Nuevo
      // expediente: elija el centro", ni "Nuevo expediente: elija el trámite", ni
      // "Nuevo expediente".
      await expect(page.getByRole('tab', { name: PANTALLA_CENTRO })).toHaveCount(0);
      await expect(page.getByRole('tab', { name: PANTALLA_TRAMITE })).toHaveCount(0);
      await expect(page.getByRole('tab', { name: PANTALLA_MAIN })).toHaveCount(0);
      // Y no solo la pestaña: tampoco queda renderizado el contenido de ninguna de las
      // tres pantallas (el listado de centros, el de trámites ni el formulario final).
      await expect(page.getByTestId('panel:centrosPanel')).toHaveCount(0);
      await expect(page.getByTestId('panel:tramitesPanel')).toHaveCount(0);
      await expect(page.getByTestId('panel:tramitePanel')).toHaveCount(0);
    } finally {
      // Teardown: si una aserción falló con el aviso todavía en pantalla, aceptarlo para
      // no dejar el modal abierto al siguiente run. Si ya se aceptó (camino verde) el
      // botón no existe: el `.catch` es intencional, no un error tapado.
      await page
        .getByRole('dialog')
        .getByRole('button', { name: 'Aceptar' })
        .click({ timeout: 5000 })
        .catch(() => {});

      await logout(page);
    }
  });
});
