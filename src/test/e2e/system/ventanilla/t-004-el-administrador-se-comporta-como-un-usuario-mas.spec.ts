import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-004 — El Administrador se comporta como un usuario más
// origen: ESC-004  |  verifica: U-nuevo-expediente-001, R-AsistenteNuevoExpediente-003
// fuente: .sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/test-e2e-desc/t-004-el-administrador-se-comporta-como-un-usuario-mas.desc.md

/**
 * IDEMPOTENCIA (§4 del contrato de generación) — este test es de SOLO LECTURA: abre el
 * asistente de nuevo expediente con el administrador de la aplicación (que no tiene
 * ningún perfil de inicio) y comprueba que el sistema lo avisa, que no le deja ninguna
 * pantalla abierta y que no se le ofrece un centro al que no pertenece. No crea, modifica
 * ni borra ninguna fila, así que no hay nombre que aislar con un sufijo `Date.now()` ni
 * nada que borrar en el teardown. Lo único que el test puede dejar en pantalla es el
 * propio aviso (y, detrás, la pestaña del asistente que se cierra al aceptarlo): el
 * `finally` lo acepta para que un run no herede el diálogo del anterior. Tampoco hace
 * falta pre-limpieza defensiva: el aviso depende solo de los datos maestros (centros,
 * trámites y permisos del estado inicial), que ningún test modifica; un run que abortara
 * no puede dejar nada pegado que cambie estas aserciones.
 */

// Credenciales del usuario de la precondición (tabla «Usuarios de acceso» del .desc.md):
// administrador de la aplicación, adscrito a CIPFP Mislata y sin ningún perfil de inicio.
// Es el caso clave del escenario: ser administrador no le da trato especial en el
// asistente, se comporta como un usuario más.
const USUARIO = 'admin';
const CONTRASENA = 'admin';

// Texto literal del aviso (`<info message="…">` de
// `system/ventanilla/views/ElegirCentro-AsistenteNuevoExpediente.xml`).
const AVISO = 'No puede crear expedientes en ninguno de sus centros';

// Centro al que el administrador NO pertenece: no debe ofrecérsele en ningún momento,
// pese a que como administrador de la aplicación sí puede ver datos de cualquier centro
// en otras pantallas.
const CENTRO_AJENO = 'CIPFP Batoi';

// Títulos de las TRES pantallas del asistente (los fijan los `action-view` de
// `system/ventanilla/views/`): ninguna debe quedar abierta. Se comparan con regex
// anclada porque Axelor añade un `*` al título de la pestaña cuando el formulario está
// sucio ("Nuevo expediente: elija el centro*"), y porque "Nuevo expediente" a secas es
// prefijo de las otras dos (sin anclar, cazaría pestañas que no son la suya).
const PANTALLA_CENTRO = /^Nuevo expediente: elija el centro\*?$/;
const PANTALLA_TRAMITE = /^Nuevo expediente: elija el trámite\*?$/;
const PANTALLA_MAIN = /^Nuevo expediente\*?$/;

/**
 * Abre el asistente desde el menú «Mis trámites» → «Nuevo trámite». El grupo se pliega
 * y despliega al pulsarlo, así que solo se despliega si la entrada no se ve: pulsarlo a
 * ciegas lo cerraría cuando ya venía abierto.
 */
async function abrirNuevoExpediente(page: Page): Promise<void> {
  const entrada = page.getByTestId('item:misTramites-nuevoTramite-menuitem');
  if (!(await entrada.isVisible())) {
    await page.getByTestId('item:misTramites-menuitem').getByTestId('title').first().click();
  }
  await entrada.click();
}

/**
 * Comprueba que el centro ajeno no está ofrecido en ninguna parte de lo que el usuario
 * tiene delante: ni en el diálogo, ni en el listado de centros del asistente, ni en
 * cualquier otro punto de la página.
 */
async function noSeOfreceElCentroAjeno(page: Page): Promise<void> {
  await expect(page.getByText(CENTRO_AJENO)).toHaveCount(0);
}

test.describe('Ventanilla — Nuevo expediente', () => {
  test('El Administrador se comporta como un usuario más', async ({ page }) => {
    await ensureLoggedOut(page);

    // Paso 1: Dado que el administrador ha iniciado sesión con el usuario `admin` y la
    // contraseña `admin`.
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

      // Resultado esperado (1ª mitad de "en ningún momento se le ofrece el centro
      // CIPFP Batoi"): con el aviso ya en pantalla, el centro ajeno no aparece ni en el
      // diálogo ni detrás de él. Este es el único instante en el que el asistente llega
      // a montarse, así que es donde un listado de centros indebido sería visible.
      await noSeOfreceElCentroAjeno(page);

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

      // Resultado esperado (2ª mitad de "en ningún momento"): tras aceptar el aviso
      // tampoco se le ofrece el centro ajeno.
      await noSeOfreceElCentroAjeno(page);
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
