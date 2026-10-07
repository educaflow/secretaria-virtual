import { test, expect, Locator, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../../../_support/auth';

// T-005 — El administrativo que también es alumno presenta su propia solicitud telemáticamente
// origen: ESC —  |  CREADOR | [*] --(alta: botón «Crear expediente»)--> ENTRADA/ENTRADA_DATOS  |  tipo: happy
// fuente: .sdd/drafts/2026-09-19_23-14_anulacion-matricula-arranque/test-e2e-desc/t-005-el-administrativo-que-tambien-es-alumno-presenta-su-propia-solicitud-telematicamente.desc.md

const TRAMITE = 'Anulación de matrícula en ciclo formativo';
const TIPO_TRAMITE = 'Trámites para el alumno';
// Título de la pestaña del último paso del asistente de ventanilla.
const PANTALLA_ALTA = 'Nuevo expediente';

/**
 * Abre el asistente «Mis trámites» → «Nuevo trámite» y elige el trámite hasta llegar a su
 * último paso, «Nuevo expediente». Cómo funciona el asistente (pantallas, testids y cuándo
 * pregunta cada cosa) está en `system/ventanilla/views/nuevoexpediente/CLAUDE.md`.
 * El usuario de estos tests es de un solo centro, así que el asistente se salta la elección
 * de centro y abre directamente la de trámite.
 */
async function abrirAltaDelTramite(page: Page): Promise<void> {
  // El grupo del menú se pliega al pulsarlo: solo se despliega si la entrada no se ve.
  const entrada = page.getByTestId('item:misTramites-nuevoTramite-menuitem');
  if (!(await entrada.isVisible())) {
    await page.getByTestId('item:misTramites-menuitem').getByTestId('title').first().click();
  }
  await entrada.click();

  // Árbol de trámites: los tipos de trámite (nivel 1) nacen plegados y sus trámites (nivel 2)
  // no existen en el DOM hasta desplegarlos.
  const arbol = page.getByTestId('panel:tramitesPanel');
  await arbol.locator('[role="row"][aria-level="1"]').filter({ hasText: TIPO_TRAMITE }).click();
  await arbol.locator('[role="row"][aria-level="2"]').filter({ hasText: TRAMITE }).click();

  await expect(page.getByRole('tab', { name: PANTALLA_ALTA, exact: true })).toBeVisible();
}

/**
 * El radio de una opción de una pregunta del asistente. Axelor pinta cada opción como
 * `<div><input type="radio"><span>texto</span></div>` sin `<label>`, así que se localiza el
 * `div` que contiene el texto y, dentro, su radio.
 */
function opcion(page: Page, campo: 'presentadoEnRepresentacion', texto: string): Locator {
  return page
    .getByTestId(`field:${campo}`)
    .locator('div:has(> [data-testid="radio"])')
    .filter({ hasText: texto })
    .getByRole('radio');
}

test.describe('Anulación de matrícula en ciclo formativo — ENTRADA', () => {
  test('El administrativo que también es alumno presenta su propia solicitud telemáticamente', async ({ page }) => {
    let numero = '';
    try {
      // --- Tramo único: CREADOR (administrativo2@mislata.es) ---
      // Given: que `administrativo2@mislata.es` (contraseña `demo1234`) es a la vez administrativo y
      // alumno de CIPFP Mislata, de modo que tiene los dos perfiles de inicio (TRAMITADOR y CREADOR)
      // sobre los trámites de alumno, y que quiere anular su propia matrícula.
      await ensureLoggedOut(page);
      await login(page, 'administrativo2@mislata.es', 'demo1234');

      // When: inicia sesión, abre «Mis trámites» → «Nuevo trámite», despliega «Trámites para el alumno»
      // y pulsa sobre «Anulación de matrícula en ciclo formativo».
      await abrirAltaDelTramite(page);

      // Then: se abre la pantalla «Nuevo expediente» SIN la pregunta «¿Cómo se presenta?»: aunque
      // tiene los dos perfiles de inicio, la forma de presentar la fija la entrada de menú, y
      // «Mis trámites» → «Nuevo trámite» es la de quien lo presenta él mismo (perfil CREADOR).
      await expect(page.getByText('¿Cómo se presenta?')).toHaveCount(0);

      // And: NO se pregunta «¿Para quién es el expediente?»: presentándolo él mismo, como es
      // alumno y no familiar, el expediente solo puede ser para él.
      await expect(page.getByTestId('field:presentadoEnRepresentacion')).toHaveCount(0);

      // When: pulsa «Crear expediente».
      await page.getByRole('button', { name: 'Crear expediente' }).click();

      // Then: se abre el expediente en la fase ENTRADA, estado ENTRADA_DATOS, con la cabecera
      // «Entrada» / «Entrada de datos», exactamente igual que en T-001: el
      // expediente nace como presentación telemática aunque quien entra sea administrativo.
      await expect(page.getByRole('tab', { name: PANTALLA_ALTA, exact: true })).toHaveCount(0);
      // Idempotencia (§5.1): el número de expediente lo asigna el servidor y es lo ÚNICO que
      // identifica a lo creado; la pestaña se titula «<número>-<nombre del tipo de expediente>»
      // y el número tiene el formato «NNNNN/AAAA-<código del centro>».
      const pestana = page.getByRole('tab').last();
      await expect(pestana).toContainText(/\d{4,}\/\d{4}-\d+-Anulación de matrícula en ciclo formativo/);
      numero = (await pestana.textContent())!.split('-').slice(0, 2).join('-').trim();
      expect(numero).toMatch(/^\d{4,}\/\d{4}-\d+$/);
      await expect(page.getByRole('tab', { name: new RegExp(numero) })).toBeVisible();
      await expect(page.getByLabel('Fase')).toHaveValue('Entrada');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Entrada de datos');

      // And: el aviso es «Para presentar la solicitud necesitará firmarla con su certificado digital
      // desde este mismo ordenador».
      await expect(page.getByRole('status')).toContainText(
        /Para presentar la solicitud necesitará firmarla con su certificado digital desde este mismo ordenador/,
      );

      // And: el panel «Persona que presenta la solicitud» NO aparece…
      await expect(page.getByText('Persona que presenta la solicitud')).toHaveCount(0);
      // … y en «Alumno/a al que se refiere la solicitud» los campos vienen rellenos con sus propios
      // datos —«Apellidos» = «CIPFP Mislata», «Nombre» = «Administrativo2», «DNI/NIE» = «97345780M»—
      // y bloqueados.
      const panelInteresado = page.getByRole('region', { name: 'Alumno/a al que se refiere la solicitud' });
      await expect(panelInteresado.getByLabel('Apellidos')).toHaveValue('CIPFP Mislata');
      await expect(panelInteresado.getByLabel('Apellidos')).toBeDisabled();
      await expect(panelInteresado.getByLabel('Nombre')).toHaveValue('Administrativo2');
      await expect(panelInteresado.getByLabel('Nombre')).toBeDisabled();
      await expect(panelInteresado.getByLabel('DNI/NIE')).toHaveValue('97345780M');
      await expect(panelInteresado.getByLabel('DNI/NIE')).toBeDisabled();

      // And: el pie ofrece «Borrar el expediente» y «Siguiente».
      await expect(page.getByRole('button', { name: 'Borrar el expediente' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Siguiente' })).toBeVisible();
    } finally {
      // Teardown (§5.2): el estado de llegada ENTRADA_DATOS ofrece el evento DELETE
      // («Borrar el expediente»), así que el expediente se borra aquí. El borrado exige SESIÓN
      // ABIERTA y el perfil del estado final (CREADOR, el del único tramo), por eso va ANTES del
      // logout. DELETE recarga la aplicación entera (refresh-app).
      // El `.catch(() => {})` es intencional: el teardown no debe enmascarar el fallo de una aserción.
      await page.getByRole('button', { name: 'Borrar el expediente' }).click().catch(() => {});
      await page.getByRole('button', { name: 'Aceptar' }).click().catch(() => {});
      // Se comprueba por el NÚMERO del expediente que este test creó, nunca por «el primero».
      if (numero) {
        await expect(page.getByRole('tab', { name: new RegExp(numero) })).toHaveCount(0).catch(() => {});
      }
      await logout(page).catch(() => {});
    }
  });
});
