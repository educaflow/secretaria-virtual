import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../../../_support/auth';

// T-002 — El familiar presenta la solicitud en representación de su hijo
// origen: ESC —  |  CREADOR | [*] --(alta: botón «Crear expediente»)--> SOLICITUD/DATOS_SOLICITUD  |  tipo: happy
// fuente: .sdd/drafts/2026-09-19_23-14_anulacion-matricula-arranque/test-e2e-desc/t-002-el-familiar-presenta-la-solicitud-en-representacion-de-su-hijo.desc.md
test.describe('Anulación de matrícula en ciclo formativo — SOLICITUD', () => {
  test('El familiar presenta la solicitud en representación de su hijo', async ({ page }) => {
    let numero = '';
    try {
      // --- Tramo único: CREADOR (familiar1@mislata.es) ---
      // Given: que `familiar1@mislata.es` (contraseña `demo1234`) es familiar en CIPFP Mislata, que el
      // data-init de security le da el perfil CREADOR sobre los trámites de alumno y que el trámite
      // admite presentar en representación.
      await ensureLoggedOut(page);
      await login(page, 'familiar1@mislata.es', 'demo1234');

      // When: inicia sesión, abre «Expedientes» → «Trámites», despliega «Trámites si eres alumno»
      // y pulsa sobre «Anulación de matrícula en ciclo formativo».
      await page.getByText('Expedientes').click();
      await page.getByText('Trámites', { exact: true }).click();
      await page.getByRole('row', { name: 'Trámites si eres alumno' }).getByText('arrow_right').click();
      await page
        .getByRole('row', { name: 'Anulación de matrícula en ciclo formativo' })
        .getByText('Anulación de matrícula en ciclo formativo')
        .click();

      // Then: se abre la ventana «Nuevo expediente»…
      await expect(page.getByRole('heading', { name: 'Nuevo expediente' })).toBeVisible();
      const dialogoAlta = page.getByRole('dialog');
      // … con «Centro» = «CIPFP Mislata» de solo lectura,
      await expect(page.getByLabel('Centro')).toHaveValue('CIPFP Mislata');
      await expect(page.getByLabel('Centro')).toBeDisabled();
      // … SIN interruptor de la forma de presentación (el familiar solo tiene el perfil CREADOR,
      // así que no hay nada que elegir),
      await expect(
        page.getByText('Presentado el expediente a partir de un documento en papel'),
      ).toHaveCount(0);
      // … y con la pregunta «¿Para quién es el expediente?».
      await expect(page.getByText('¿Para quién es el expediente?').first()).toBeVisible();

      // And: el hijo al que se refiere la solicitud NO se elige en ninguna pantalla — la aplicación no
      // guarda ningún vínculo entre el familiar y el alumno. En la ventana de alta eso se observa como
      // la ausencia de cualquier selector de persona: solo hay «Centro» y la pregunta de para quién es.
      await expect(dialogoAlta.getByRole('combobox')).toHaveCount(0);

      // When: marca «Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)»
      // y pulsa «Crear expediente».
      const opcionRepresentacion = page
        .getByText('Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)')
        .locator('..')
        .getByRole('radio');
      await opcionRepresentacion.click();
      await expect(opcionRepresentacion).toBeChecked();
      await page.getByRole('button', { name: 'Crear expediente' }).click();

      // Then: se abre el expediente en la fase SOLICITUD, estado DATOS_SOLICITUD, con la cabecera
      // «Solicitud de anulación» / «Datos de la solicitud».
      await expect(page.getByRole('heading', { name: 'Nuevo expediente' })).toHaveCount(0);
      // Idempotencia (§5.1): el número de expediente lo asigna el servidor y es lo ÚNICO que
      // identifica a lo creado; la pestaña se titula «<número>-<nombre del tipo de expediente>».
      const pestana = page.getByRole('tab').last();
      await expect(pestana).toContainText(/\d{4,}\/\d{4}-Anulación de matrícula en ciclo formativo/);
      numero = (await pestana.textContent())!.split('-')[0].trim();
      expect(numero).toMatch(/^\d{4,}\/\d{4}$/);
      await expect(page.getByRole('tab', { name: new RegExp(numero) })).toBeVisible();
      await expect(page.getByLabel('Fase')).toHaveValue('Solicitud de anulación');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Datos de la solicitud');

      // And: aparece el panel «Persona que presenta la solicitud» con los datos del familiar que ha
      // entrado —«Apellidos» = «de Alumno1 CIPFP Mislata», «Nombre» = «Familiar1»,
      // «DNI/NIE» = «43145636M»— y los tres campos bloqueados.
      const panelSolicitante = page.getByRole('region', { name: 'Persona que presenta la solicitud' });
      await expect(panelSolicitante).toBeVisible();
      await expect(panelSolicitante.getByLabel('Apellidos')).toHaveValue('de Alumno1 CIPFP Mislata');
      await expect(panelSolicitante.getByLabel('Apellidos')).toBeDisabled();
      await expect(panelSolicitante.getByLabel('Nombre')).toHaveValue('Familiar1');
      await expect(panelSolicitante.getByLabel('Nombre')).toBeDisabled();
      await expect(panelSolicitante.getByLabel('DNI/NIE')).toHaveValue('43145636M');
      await expect(panelSolicitante.getByLabel('DNI/NIE')).toBeDisabled();

      // And: en «Alumno/a al que se refiere la solicitud» los campos «Apellidos», «Nombre» y «DNI/NIE»
      // están vacíos y editables: es donde se identificará al hijo.
      const panelInteresado = page.getByRole('region', { name: 'Alumno/a al que se refiere la solicitud' });
      for (const campo of ['Apellidos', 'Nombre', 'DNI/NIE']) {
        await expect(panelInteresado.getByLabel(campo)).toHaveValue('');
        await expect(panelInteresado.getByLabel(campo)).toBeEnabled();
      }

      // And: el pie ofrece «Borrar el expediente» y «Siguiente».
      await expect(page.getByRole('button', { name: 'Borrar el expediente' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Siguiente' })).toBeVisible();
    } finally {
      // Teardown (§5.2): el estado de llegada DATOS_SOLICITUD ofrece el evento DELETE
      // («Borrar el expediente»), así que el expediente se borra aquí. El borrado exige SESIÓN
      // ABIERTA y el perfil del estado final (CREADOR, el del único tramo), por eso va ANTES del
      // logout. DELETE recarga la aplicación entera (refresh-app) y la deja en el árbol de trámites.
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
