import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../../../_support/auth';

// T-001 — El alumno presenta su propia solicitud telemáticamente
// origen: ESC —  |  CREADOR | [*] --(alta: botón «Crear expediente»)--> SOLICITUD/DATOS_SOLICITUD  |  tipo: happy
// fuente: .sdd/drafts/2026-09-19_23-14_anulacion-matricula-arranque/test-e2e-desc/t-001-el-alumno-presenta-su-propia-solicitud-telematicamente.desc.md
test.describe('Anulación de matrícula en ciclo formativo — SOLICITUD', () => {
  test('El alumno presenta su propia solicitud telemáticamente', async ({ page }) => {
    let numero = '';
    try {
      // --- Tramo único: CREADOR (alumno1@mislata.es) ---
      // Given: que `alumno1@mislata.es` (contraseña `demo1234`) es alumno de CIPFP Mislata y solo
      // tiene ese centro, y que por ser alumno tiene el perfil CREADOR sobre los trámites de alumno.
      await ensureLoggedOut(page);
      await login(page, 'alumno1@mislata.es', 'demo1234');

      // When: inicia sesión, abre «Expedientes» → «Trámites», despliega «Trámites si eres alumno»
      // y pulsa sobre «Anulación de matrícula en ciclo formativo».
      await page.getByText('Expedientes').click();
      await page.getByText('Trámites', { exact: true }).click();
      await page.getByRole('row', { name: 'Trámites si eres alumno' }).getByText('arrow_right').click();
      await page
        .getByRole('row', { name: 'Anulación de matrícula en ciclo formativo' })
        .getByText('Anulación de matrícula en ciclo formativo')
        .click();

      // Then: se abre la ventana «Nuevo expediente» con la tarjeta de ayuda del trámite,
      await expect(page.getByRole('heading', { name: 'Nuevo expediente' })).toBeVisible();
      await expect(
        page.getByText(/Con este trámite puedes solicitar la anulación de tu matrícula en un ciclo formativo/),
      ).toBeVisible();
      // … el campo «Centro» ya relleno con «CIPFP Mislata» y de solo lectura,
      await expect(page.getByLabel('Centro')).toHaveValue('CIPFP Mislata');
      await expect(page.getByLabel('Centro')).toBeDisabled();
      // … y SIN el interruptor de la forma de presentación (solo tiene el perfil CREADOR,
      // así que no hay nada que elegir).
      await expect(
        page.getByText('Presentado el expediente a partir de un documento en papel'),
      ).toHaveCount(0);

      // And: se muestra la pregunta «¿Para quién es el expediente?» con las opciones «Para mí» y
      // «Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)», ninguna marcada.
      await expect(page.getByText('¿Para quién es el expediente?').first()).toBeVisible();
      const opcionParaMi = page.getByText('Para mí', { exact: true }).locator('..').getByRole('radio');
      const opcionRepresentacion = page
        .getByText('Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)')
        .locator('..')
        .getByRole('radio');
      await expect(opcionParaMi).toBeVisible();
      await expect(opcionRepresentacion).toBeVisible();
      await expect(opcionParaMi).not.toBeChecked();
      await expect(opcionRepresentacion).not.toBeChecked();

      // When: marca «Para mí» y pulsa «Crear expediente».
      await opcionParaMi.click();
      await expect(opcionParaMi).toBeChecked();
      await page.getByRole('button', { name: 'Crear expediente' }).click();

      // Then: se cierra la ventana y se abre el expediente en la fase SOLICITUD, estado DATOS_SOLICITUD,
      // cuya cabecera muestra «Solicitud de anulación» y «Datos de la solicitud».
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
