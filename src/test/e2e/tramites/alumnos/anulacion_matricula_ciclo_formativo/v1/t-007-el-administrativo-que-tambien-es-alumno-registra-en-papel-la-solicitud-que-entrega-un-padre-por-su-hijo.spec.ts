import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../../../_support/auth';

// T-007 — El administrativo que también es alumno registra en papel la solicitud que entrega un padre por su hijo
// origen: ESC —  |  TRAMITADOR | [*] --CONTINUAR--> SOLICITUD/DATOS_SOLICITUD  |  tipo: happy
// fuente: .sdd/drafts/2026-09-19_23-14_anulacion-matricula-arranque/test-e2e-desc/t-007-el-administrativo-que-tambien-es-alumno-registra-en-papel-la-solicitud-que-entrega-un-padre-por-su-hijo.desc.md

const NOMBRE_PDF = 'solicitud-escaneada.pdf';

/**
 * PDF mínimo válido (1 página en blanco, ~350 bytes) construido EN MEMORIA.
 * El test es de regresión versionado: MUST NOT depender de ningún fichero fuera del
 * repositorio (rutas de /tmp o scratchpads desaparecen y dejan el test rojo en otra
 * máquina). Su contenido es indiferente: en este test nadie lo lee, solo se adjunta.
 */
function pdfMinimo(): Buffer {
  const objetos = [
    '1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n',
    '2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n',
    '3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << >> >>\nendobj\n',
  ];
  let pdf = '%PDF-1.4\n';
  const offsets: number[] = [];
  for (const objeto of objetos) {
    offsets.push(pdf.length);
    pdf += objeto;
  }
  const inicioXref = pdf.length;
  pdf += 'xref\n0 4\n0000000000 65535 f \n';
  pdf += offsets.map((o) => String(o).padStart(10, '0') + ' 00000 n \n').join('');
  pdf += `trailer\n<< /Size 4 /Root 1 0 R >>\nstartxref\n${inicioXref}\n%%EOF\n`;
  return Buffer.from(pdf, 'latin1');
}

test.describe('Anulación de matrícula en ciclo formativo — SOLICITUD', () => {
  test('El administrativo que también es alumno registra en papel la solicitud que entrega un padre por su hijo', async ({
    page,
  }) => {
    let numero = '';
    try {
      // --- Tramo único: TRAMITADOR (administrativo2@mislata.es) ---
      // Given: que `administrativo2@mislata.es` (contraseña `demo1234`) tiene los dos perfiles de
      // inicio en CIPFP Mislata, y que quien ha entregado la solicitud en ventanilla es el padre de
      // un alumno, que la presenta en representación de su hijo.
      await ensureLoggedOut(page);
      await login(page, 'administrativo2@mislata.es', 'demo1234');

      // When: inicia sesión, abre «Expedientes» → «Trámites», despliega «Trámites si eres alumno»,
      // pulsa sobre «Anulación de matrícula en ciclo formativo»…
      await page.getByText('Expedientes').click();
      await page.getByText('Trámites', { exact: true }).click();
      await page.getByRole('row', { name: 'Trámites si eres alumno' }).getByText('arrow_right').click();
      await page
        .getByRole('row', { name: 'Anulación de matrícula en ciclo formativo' })
        .getByText('Anulación de matrícula en ciclo formativo')
        .click();

      // … y enciende el interruptor «Presentado el expediente a partir de un documento en papel»
      // (lo ve porque tiene los dos perfiles de inicio; nace apagado, en la forma telemática).
      await expect(page.getByRole('heading', { name: 'Nuevo expediente' })).toBeVisible();
      const interruptorPapel = page.getByRole('switch', {
        name: 'Presentado el expediente a partir de un documento en papel',
      });
      await expect(interruptorPapel).toBeVisible();
      await expect(interruptorPapel).not.toBeChecked();
      await interruptorPapel.click();
      await expect(interruptorPapel).toBeChecked();

      // Then: la pregunta de destinatario es «¿Para quién es la solicitud? («Para mí» es para la
      // persona que la ha entregado)»: la de papel. Se comprueba además que la pregunta telemática
      // («¿Para quién es el expediente?») ha desaparecido, que es la mutación del interruptor.
      await expect(page.getByText('¿Para quién es el expediente?')).toHaveCount(0);
      await expect(
        page
          .getByText('¿Para quién es la solicitud? («Para mí» es para la persona que la ha entregado)')
          .first(),
      ).toBeVisible();

      // When: marca «Para otra persona a la que represento (hijo/a menor de edad o persona
      // tutelada)»…
      const opcionRepresentacion = page
        .getByText('Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)')
        .locator('..')
        .getByRole('radio');
      await opcionRepresentacion.click();
      await expect(opcionRepresentacion).toBeChecked();
      // El interruptor sigue encendido: la presentación es en papel.
      await expect(interruptorPapel).toBeChecked();
      // … y pulsa «Crear expediente».
      await page.getByRole('button', { name: 'Crear expediente' }).click();

      // Then: se abre el expediente en la fase SOLICITUD, estado PENDIENTE_DOCUMENTO_ESCANEADO…
      await expect(page.getByRole('heading', { name: 'Nuevo expediente' })).toHaveCount(0);
      // Idempotencia (§5.1): el número de expediente lo asigna el servidor y es lo ÚNICO que
      // identifica a lo creado; la pestaña se titula «<número>-<nombre del tipo de expediente>».
      const pestana = page.getByRole('tab').last();
      await expect(pestana).toContainText(/\d{4,}\/\d{4}-Anulación de matrícula en ciclo formativo/);
      numero = (await pestana.textContent())!.split('-')[0].trim();
      expect(numero).toMatch(/^\d{4,}\/\d{4}$/);
      await expect(page.getByRole('tab', { name: new RegExp(numero) })).toBeVisible();
      await expect(page.getByLabel('Fase')).toHaveValue('Solicitud de anulación');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue(
        'Pendiente de adjuntar la solicitud en papel escaneada',
      );
      // … con el panel «Solicitud entregada en papel» y su campo «Solicitud escaneada (PDF)».
      const panelPapel = page.getByRole('region', { name: 'Solicitud entregada en papel' });
      await expect(panelPapel).toBeVisible();
      await expect(panelPapel.getByText('Solicitud escaneada (PDF)')).toBeVisible();
      await expect(panelPapel.getByRole('button', { name: 'Subir' })).toBeVisible();

      // When: adjunta un PDF de menos de 10 MB…
      // El widget de Axelor esconde su `input[type=file]` detrás del botón «Subir»; se le pasan los
      // bytes directamente para no depender de un diálogo nativo de selección de ficheros.
      await panelPapel
        .locator('input[type="file"]')
        .setInputFiles({ name: NOMBRE_PDF, mimeType: 'application/pdf', buffer: pdfMinimo() });
      await expect(panelPapel.getByRole('button', { name: NOMBRE_PDF })).toBeVisible();
      // … y pulsa «Siguiente».
      await page.getByRole('button', { name: 'Siguiente' }).click();

      // Then: el expediente pasa al estado DATOS_SOLICITUD, con la cabecera «Solicitud de anulación»
      // / «Datos de la solicitud».
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Datos de la solicitud');
      await expect(page.getByLabel('Fase')).toHaveValue('Solicitud de anulación');
      // El expediente sigue siendo el que creó este test.
      await expect(page.getByRole('tab', { name: new RegExp(numero) })).toBeVisible();

      // And: aparece el panel «Persona que presenta la solicitud» con los tres campos VACÍOS y
      // EDITABLES: es el padre que entregó el papel, y en papel la aplicación no sabe quién es
      // (a diferencia de la presentación telemática en representación, donde los precarga con los
      // datos del usuario que ha entrado). Que el panel EXISTA es lo que distingue este caso de
      // T-006, donde se presentaba en papel «Para mí» y el panel no aparece.
      const panelSolicitante = page.getByRole('region', { name: 'Persona que presenta la solicitud' });
      await expect(panelSolicitante).toBeVisible();
      for (const campo of ['Apellidos', 'Nombre', 'DNI/NIE']) {
        await expect(panelSolicitante.getByLabel(campo)).toHaveValue('');
        await expect(panelSolicitante.getByLabel(campo)).toBeEnabled();
      }

      // And: en «Alumno/a al que se refiere la solicitud» los tres campos están también VACÍOS y
      // EDITABLES: es donde se identificará al hijo.
      const panelInteresado = page.getByRole('region', { name: 'Alumno/a al que se refiere la solicitud' });
      for (const campo of ['Apellidos', 'Nombre', 'DNI/NIE']) {
        await expect(panelInteresado.getByLabel(campo)).toHaveValue('');
        await expect(panelInteresado.getByLabel(campo)).toBeEnabled();
      }

      // And: el pie ofrece «Borrar el expediente», «Atrás» y «Presentar la solicitud».
      await expect(page.getByRole('button', { name: 'Borrar el expediente' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Atrás' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Presentar la solicitud' })).toBeVisible();
    } finally {
      // Teardown (§5.2): el estado de llegada DATOS_SOLICITUD ofrece el evento DELETE
      // («Borrar el expediente»), así que el expediente se borra aquí. El borrado exige SESIÓN
      // ABIERTA y el perfil del estado final (TRAMITADOR, el del único tramo), por eso va ANTES del
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
