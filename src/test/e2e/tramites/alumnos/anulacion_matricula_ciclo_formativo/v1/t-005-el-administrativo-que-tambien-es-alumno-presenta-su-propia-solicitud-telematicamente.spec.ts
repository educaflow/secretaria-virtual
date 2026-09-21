import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../../../_support/auth';

// T-005 — El administrativo que también es alumno presenta su propia solicitud telemáticamente
// origen: ESC —  |  CREADOR | [*] --(alta: botón «Crear expediente»)--> SOLICITUD/DATOS_SOLICITUD  |  tipo: happy
// fuente: .sdd/drafts/2026-09-19_23-14_anulacion-matricula-arranque/test-e2e-desc/t-005-el-administrativo-que-tambien-es-alumno-presenta-su-propia-solicitud-telematicamente.desc.md
test.describe('Anulación de matrícula en ciclo formativo — SOLICITUD', () => {
  test('El administrativo que también es alumno presenta su propia solicitud telemáticamente', async ({ page }) => {
    let numero = '';
    try {
      // --- Tramo único: CREADOR (administrativo2@mislata.es) ---
      // Given: que `administrativo2@mislata.es` (contraseña `demo1234`) es a la vez administrativo y
      // alumno de CIPFP Mislata, de modo que tiene los dos perfiles de inicio (TRAMITADOR y CREADOR)
      // sobre los trámites de alumno, y que quiere anular su propia matrícula.
      await ensureLoggedOut(page);
      await login(page, 'administrativo2@mislata.es', 'demo1234');

      // When: inicia sesión, abre «Expedientes» → «Trámites», despliega «Trámites si eres alumno»
      // y pulsa sobre «Anulación de matrícula en ciclo formativo».
      await page.getByText('Expedientes').click();
      await page.getByText('Trámites', { exact: true }).click();
      await page.getByRole('row', { name: 'Trámites si eres alumno' }).getByText('arrow_right').click();
      await page
        .getByRole('row', { name: 'Anulación de matrícula en ciclo formativo' })
        .getByText('Anulación de matrícula en ciclo formativo')
        .click();

      // Then: se abre la ventana «Nuevo expediente» y, a diferencia de los tests anteriores, SÍ se
      // muestra el interruptor «Presentado el expediente a partir de un documento en papel», apagado:
      // como tiene los dos perfiles de inicio, se le pregunta cómo presenta.
      await expect(page.getByRole('heading', { name: 'Nuevo expediente' })).toBeVisible();
      const interruptorPapel = page.getByRole('switch', {
        name: 'Presentado el expediente a partir de un documento en papel',
      });
      await expect(interruptorPapel).toBeVisible();
      await expect(interruptorPapel).not.toBeChecked();

      // And: la pregunta de destinatario es «¿Para quién es el expediente?» (la forma telemática).
      await expect(page.getByText('¿Para quién es el expediente?').first()).toBeVisible();

      // When: deja el interruptor apagado, marca «Para mí» y pulsa «Crear expediente».
      const opcionParaMi = page.getByText('Para mí', { exact: true }).locator('..').getByRole('radio');
      await opcionParaMi.click();
      await expect(opcionParaMi).toBeChecked();
      // El interruptor sigue apagado: la presentación es telemática.
      await expect(interruptorPapel).not.toBeChecked();
      await page.getByRole('button', { name: 'Crear expediente' }).click();

      // Then: se abre el expediente en la fase SOLICITUD, estado DATOS_SOLICITUD, con la cabecera
      // «Solicitud de anulación» / «Datos de la solicitud», exactamente igual que en T-001: el
      // expediente nace como presentación telemática aunque quien entra sea administrativo.
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

      // And: el aviso es «Para presentar la solicitud necesitará firmarla con su certificado digital
      // desde este mismo ordenador».
      await expect(page.getByRole('status')).toContainText(
        /Para presentar la solicitud necesitará firmarla con su certificado digital desde este mismo ordenador/,
      );

      // And: el panel «Persona que presenta la solicitud» NO aparece…
      await expect(page.getByText('Persona que presenta la solicitud')).toHaveCount(0);
      // … y en «Alumno/a al que se refiere la solicitud» los campos vienen rellenos con sus propios
      // datos —«Apellidos» = «CIPFP Mislata», «Nombre» = «Administrativo2», «DNI/NIE» = «16493254T»—
      // y bloqueados.
      const panelInteresado = page.getByRole('region', { name: 'Alumno/a al que se refiere la solicitud' });
      await expect(panelInteresado.getByLabel('Apellidos')).toHaveValue('CIPFP Mislata');
      await expect(panelInteresado.getByLabel('Apellidos')).toBeDisabled();
      await expect(panelInteresado.getByLabel('Nombre')).toHaveValue('Administrativo2');
      await expect(panelInteresado.getByLabel('Nombre')).toBeDisabled();
      await expect(panelInteresado.getByLabel('DNI/NIE')).toHaveValue('16493254T');
      await expect(panelInteresado.getByLabel('DNI/NIE')).toBeDisabled();

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
