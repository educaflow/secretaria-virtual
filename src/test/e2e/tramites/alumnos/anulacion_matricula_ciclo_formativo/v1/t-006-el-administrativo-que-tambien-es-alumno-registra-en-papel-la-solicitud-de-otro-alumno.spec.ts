import { test, expect, Locator, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../../../_support/auth';

// T-006 — El administrativo que también es alumno registra en papel la solicitud de otro alumno
// origen: ESC —  |  TRAMITADOR | [*] --CONTINUAR--> SOLICITUD/DATOS_SOLICITUD  |  tipo: happy
// fuente: .sdd/drafts/2026-09-19_23-14_anulacion-matricula-arranque/test-e2e-desc/t-006-el-administrativo-que-tambien-es-alumno-registra-en-papel-la-solicitud-de-otro-alumno.desc.md

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
function opcion(page: Page, campo: 'presentadoEnPapel' | 'presentadoEnRepresentacion', texto: string): Locator {
  return page
    .getByTestId(`field:${campo}`)
    .locator('div:has(> [data-testid="radio"])')
    .filter({ hasText: texto })
    .getByRole('radio');
}

test.describe('Anulación de matrícula en ciclo formativo — SOLICITUD', () => {
  test('El administrativo que también es alumno registra en papel la solicitud de otro alumno', async ({ page }) => {
    let numero = '';
    try {
      // --- Tramo único: TRAMITADOR (administrativo2@mislata.es) ---
      // Given: que `administrativo2@mislata.es` (contraseña `demo1234`) tiene los dos perfiles de
      // inicio en CIPFP Mislata, y que otro alumno le ha entregado en ventanilla su solicitud de
      // anulación firmada en papel.
      await ensureLoggedOut(page);
      await login(page, 'administrativo2@mislata.es', 'demo1234');

      // When: inicia sesión, abre «Mis trámites» → «Nuevo trámite», despliega «Trámites para el alumno»
      // y pulsa sobre «Anulación de matrícula en ciclo formativo».
      await abrirAltaDelTramite(page);

      // Then: se abre la pantalla «Nuevo expediente» con la pregunta «¿Cómo se presenta?» SIN
      // MARCAR —la ve porque tiene los dos perfiles de inicio—…
      await expect(page.getByText('¿Cómo se presenta?')).toBeVisible();
      const opcionLoPresentoYo = opcion(page, 'presentadoEnPapel', 'Lo presento yo mismo');
      const opcionEnPapel = opcion(page, 'presentadoEnPapel', 'Estoy registrando un trámite recibido en papel');
      await expect(opcionLoPresentoYo).not.toBeChecked();
      await expect(opcionEnPapel).not.toBeChecked();
      // … y todavía sin «¿Para quién es el expediente?»: depende de cómo se presente.
      await expect(page.getByTestId('field:presentadoEnRepresentacion')).toHaveCount(0);

      // When: marca «Estoy registrando un trámite recibido en papel». El cambio va al servidor a
      // recalcular qué más hay que preguntar; se espera esa respuesta antes de comprobar nada.
      const recalculo = page.waitForResponse(
        (respuesta) =>
          respuesta.url().endsWith('/ws/action') &&
          (respuesta.request().postData() ?? '').includes(
            'AsistenteNuevoExpediente-onChange-presentadoEnPapel-action',
          ),
      );
      await opcionEnPapel.click();
      await recalculo;
      await expect(opcionEnPapel).toBeChecked();

      // Then: aparece la pregunta «¿Para quién es el expediente?» («Para mí» es para la persona que
      // ha entregado el papel)…
      await expect(page.getByText('¿Para quién es el expediente?')).toBeVisible();
      // And: SIN MARCAR — ninguna de las dos opciones está seleccionada.
      const opcionParaMi = opcion(page, 'presentadoEnRepresentacion', 'Para mí');
      const opcionRepresentacion = opcion(
        page,
        'presentadoEnRepresentacion',
        'Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)',
      );
      await expect(opcionParaMi).toBeVisible();
      await expect(opcionRepresentacion).toBeVisible();
      await expect(opcionParaMi).not.toBeChecked();
      await expect(opcionRepresentacion).not.toBeChecked();

      // When: marca «Para mí» —la solicitud es del alumno que la ha entregado, no suya— y pulsa
      // «Crear expediente».
      await opcionParaMi.click();
      await expect(opcionParaMi).toBeChecked();
      // La forma de presentar sigue siendo en papel.
      await expect(opcionEnPapel).toBeChecked();
      await page.getByRole('button', { name: 'Crear expediente' }).click();

      // Then: se abre el expediente en la fase SOLICITUD, estado PENDIENTE_DOCUMENTO_ESCANEADO…
      await expect(page.getByRole('tab', { name: PANTALLA_ALTA, exact: true })).toHaveCount(0);
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
      // / «Datos de la solicitud»…
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Datos de la solicitud');
      await expect(page.getByLabel('Fase')).toHaveValue('Solicitud de anulación');
      // El expediente sigue siendo el que creó este test.
      await expect(page.getByRole('tab', { name: new RegExp(numero) })).toBeVisible();
      // … y el aviso «Copie los datos de la solicitud entregada en papel que ha adjuntado escaneada.
      // Al presentarla se registrará su entrada».
      await expect(page.getByRole('status')).toContainText(
        /Copie los datos de la solicitud entregada en papel que ha adjuntado escaneada\. Al presentarla se registrará su entrada/,
      );

      // And: el panel «Persona que presenta la solicitud» NO aparece…
      await expect(page.getByText('Persona que presenta la solicitud')).toHaveCount(0);
      // … y en «Alumno/a al que se refiere la solicitud» los campos «Apellidos», «Nombre» y «DNI/NIE»
      // están VACÍOS y EDITABLES: no se han precargado los suyos, porque el interesado es el otro
      // alumno (a diferencia de T-005, donde presentaba telemáticamente para sí mismo).
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
