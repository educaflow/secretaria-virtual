import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../../../_support/auth';

// T-001 — El alumno presenta su propia solicitud telemáticamente
// origen: ESC —  |  CREADOR | [*] --(alta: botón «Crear expediente»)--> ENTRADA/ENTRADA_DATOS  |  tipo: happy
// fuente: .sdd/drafts/2026-09-19_23-14_anulacion-matricula-arranque/test-e2e-desc/t-001-el-alumno-presenta-su-propia-solicitud-telematicamente.desc.md

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

test.describe('Anulación de matrícula en ciclo formativo — ENTRADA', () => {
  test('El alumno presenta su propia solicitud telemáticamente', async ({ page }) => {
    let numero = '';
    try {
      // --- Tramo único: CREADOR (alumno1@mislata.es) ---
      // Given: que `alumno1@mislata.es` (contraseña `demo1234`) es alumno de CIPFP Mislata y solo
      // tiene ese centro, y que por ser alumno tiene el perfil CREADOR sobre los trámites de alumno.
      await ensureLoggedOut(page);
      await login(page, 'alumno1@mislata.es', 'demo1234');

      // When: inicia sesión, abre «Mis trámites» → «Nuevo trámite», despliega «Trámites para el alumno»
      // y pulsa sobre «Anulación de matrícula en ciclo formativo».
      await abrirAltaDelTramite(page);

      // Then: se abre la pantalla «Nuevo expediente» con la ayuda del trámite,
      await expect(page.getByTestId('field:ayudaTramite')).toContainText(
        /Con este trámite puedes solicitar la anulación de tu matrícula en un ciclo formativo/,
      );
      // … el campo «Centro» ya relleno con «CIPFP Mislata» y de solo lectura,
      const campoCentro = page.getByTestId('field:centro').getByRole('textbox');
      await expect(campoCentro).toHaveValue('CIPFP Mislata');
      await expect(campoCentro).toBeDisabled();
      // … SIN la pregunta «¿Cómo se presenta?» (la forma la fija la entrada «Mis trámites»: lo
      // presenta él mismo) y SIN «¿Para quién es el expediente?» (es alumno y no familiar, así que el
      // expediente solo puede ser para él mismo).
      await expect(page.getByText('¿Cómo se presenta?')).toHaveCount(0);
      await expect(page.getByTestId('field:presentadoEnRepresentacion')).toHaveCount(0);

      // When: pulsa «Crear expediente».
      await page.getByRole('button', { name: 'Crear expediente' }).click();

      // Then: se cierra la pantalla «Nuevo expediente» y se abre el expediente en la fase ENTRADA, estado ENTRADA_DATOS,
      // cuya cabecera muestra «Entrada» y «Entrada de datos».
      await expect(page.getByRole('tab', { name: PANTALLA_ALTA, exact: true })).toHaveCount(0);
      // Idempotencia (§5.1): el número de expediente lo asigna el servidor y es lo ÚNICO que
      // identifica a lo creado; la pestaña se titula «<número>-<nombre del tipo de expediente>»,
      // y el número tiene el formato «NNNNN/AAAA-<código del centro>».
      const pestana = page.getByRole('tab').last();
      await expect(pestana).toContainText(/\d{4,}\/\d{4}-\d+-Anulación de matrícula en ciclo formativo/);
      numero = (await pestana.textContent())!.split('-').slice(0, 2).join('-').trim();
      expect(numero).toMatch(/^\d{4,}\/\d{4}-\d+$/);
      await expect(page.getByRole('tab', { name: new RegExp(numero) })).toBeVisible();
      await expect(page.getByLabel('Fase')).toHaveValue('Entrada');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Entrada de datos');

      // And: el aviso de la pantalla es «Para presentar la solicitud necesitará firmarla con su
      // certificado digital desde este mismo ordenador».
      await expect(page.getByRole('status')).toContainText(
        /Para presentar la solicitud necesitará firmarla con su certificado digital desde este mismo ordenador/,
      );

      // And: el panel «Persona que presenta la solicitud» NO aparece: solicitante e interesado
      // son la misma persona.
      await expect(page.getByText('Persona que presenta la solicitud')).toHaveCount(0);

      // And: en «Alumno/a al que se refiere la solicitud» los campos vienen rellenos con los datos
      // de quien ha entrado —«Apellidos», «Nombre», «DNI/NIE»— y los tres están bloqueados.
      const panelInteresado = page.getByRole('region', { name: 'Alumno/a al que se refiere la solicitud' });
      await expect(panelInteresado.getByLabel('Apellidos')).toHaveValue('CIPFP Mislata');
      await expect(panelInteresado.getByLabel('Apellidos')).toBeDisabled();
      await expect(panelInteresado.getByLabel('Nombre')).toHaveValue('Alumno1');
      await expect(panelInteresado.getByLabel('Nombre')).toBeDisabled();
      await expect(panelInteresado.getByLabel('DNI/NIE')).toHaveValue('86862719E');
      await expect(panelInteresado.getByLabel('DNI/NIE')).toBeDisabled();

      // And: el panel «Datos del alumno/a» («NIA», «Teléfono», «Dirección», «Municipio», «CP»)
      // está vacío y editable…
      const panelDatosAlumno = page.getByRole('region', { name: 'Datos del alumno/a' });
      for (const campo of ['NIA', 'Teléfono', 'Dirección', 'Código Postal']) {
        await expect(panelDatosAlumno.getByLabel(campo)).toHaveValue('');
        await expect(panelDatosAlumno.getByLabel(campo)).toBeEnabled();
      }
      await expect(panelDatosAlumno.getByLabel('Municipio')).toHaveValue('');
      await expect(panelDatosAlumno.getByLabel('Municipio')).toBeEnabled();
      // … y el panel «Matrícula que se anula» muestra el curso académico y «CIPFP Mislata»
      // de solo lectura, con «Ciclo» vacío.
      const panelMatricula = page.getByRole('region', { name: 'Matrícula que se anula' });
      await expect(panelMatricula.getByLabel('Curso académico')).toHaveValue(/^\d{4}\/\d{4}$/);
      await expect(panelMatricula.getByLabel('Curso académico')).toBeDisabled();
      await expect(panelMatricula.getByLabel('Centro')).toHaveValue('CIPFP Mislata');
      await expect(panelMatricula.getByLabel('Centro')).toBeDisabled();
      await expect(panelMatricula.getByLabel('Ciclo formativo')).toHaveValue('');

      // And: el pie ofrece los botones «Borrar el expediente» y «Siguiente».
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
