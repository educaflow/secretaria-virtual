import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../../../_support/auth';
import * as fs from 'fs';

// T-008 — Camino feliz: de la solicitud del alumno a la anulación aceptada
// origen: —  |  CREADOR → TRAMITADOR → firmantes | ENTRADA/ENTRADA_DATOS --GUARDAR_DATOS, PRESENTAR, VERIFICAR, RESOLVER, FIRMAR, FIRMAR--> CIERRE/ACEPTADA  |  tipo: happy
// fuente: escrito a mano (no viene del pipeline SDD)

const TRAMITE = 'Anulación de matrícula en ciclo formativo';
const TIPO_TRAMITE = 'Trámites para el alumno';

const ALUMNO = { login: 'alumno1@mislata.es', password: 'demo1234' };
const ADMINISTRATIVO = { login: 'administrativo1@mislata.es', password: 'demo1234' };
const SECRETARIO = { login: 'secretario@mislata.es', password: 'demo1234' };
const DIRECTOR = { login: 'director@mislata.es', password: 'demo1234' };

const FOOTER = 'panel:subsysExpedientes-template-footer-panel';

// La descarga pública de un registro de salida por su CSV: la URL que lleva el código QR que se
// estampa en el documento. Es un contrato con los documentos ya emitidos (su QR no se puede
// cambiar), así que la ruta va aquí literal a propósito: si alguien la cambia, este test debe fallar.
const RUTA_DESCARGA = '/ws/public/registro-salida/download';

/** Despliega los grupos del menú principal (de raíz a hoja) que haga falta y abre la entrada. */
async function abrirMenu(page: Page, entrada: string, ...grupos: string[]): Promise<void> {
  const item = page.getByTestId(`item:${entrada}`);
  // Cada grupo se pliega al pulsarlo: solo se despliega si la entrada no se ve ya.
  for (const grupo of grupos) {
    if (await item.isVisible()) {
      break;
    }
    await page.getByTestId(`item:${grupo}`).getByTestId('title').first().click();
  }
  await item.click();
}

/**
 * Abre la lista `entrada` del menú, la filtra por «Num. Exped.» y devuelve la fila del expediente
 * cuyo número se pasa. Nunca «la primera fila»: la BD no se resetea entre ejecuciones.
 */
async function filtrarEnBandeja(page: Page, numero: string, entrada: string, ...grupos: string[]) {
  await abrirMenu(page, entrada, ...grupos);
  const filtro = page.getByTestId('column:numeroExpediente').getByPlaceholder('Buscar...');
  await filtro.fill(numero);
  await filtro.press('Enter');
  // El último rowgroup es el de los datos; el primero lleva la cabecera y la fila de filtros.
  return page.getByRole('rowgroup').last().getByRole('row', { name: new RegExp(numero) });
}

async function abrirDesdeBandeja(page: Page, numero: string, entrada: string, ...grupos: string[]): Promise<void> {
  await (await filtrarEnBandeja(page, numero, entrada, ...grupos)).click();
  await expect(page.getByRole('tab', { name: new RegExp(numero) })).toBeVisible();
}

/** Crea un expediente del trámite desde «Mis trámites» → «Nuevo trámite» y devuelve su número. */
async function crearExpediente(page: Page): Promise<string> {
  await abrirMenu(page, 'misTramites-nuevoTramite-menuitem', 'misTramites-menuitem');

  // Árbol de trámites: los tipos de trámite (nivel 1) nacen plegados y sus trámites
  // (nivel 2) no existen en el DOM hasta desplegarlos.
  const arbol = page.getByTestId('panel:tramitesPanel');
  await arbol.locator('[role="row"][aria-level="1"]').filter({ hasText: TIPO_TRAMITE }).click();
  await arbol.locator('[role="row"][aria-level="2"]').filter({ hasText: TRAMITE }).click();

  await page.getByRole('button', { name: 'Crear expediente' }).click();

  // La pestaña se titula «<número>/<año>-<códigoCentro>-<tipo de expediente>».
  const pestana = page.getByRole('tab').last();
  await expect(pestana).toContainText(new RegExp(`\\d{4,}/\\d{4}-\\d+-${TRAMITE}`));
  const numero = (await pestana.textContent())!.split('-').slice(0, 2).join('-').trim();
  expect(numero).toMatch(/^\d{4,}\/\d{4}-\d+$/);
  return numero;
}

/**
 * Elige un valor en un campo many-to-one vacío. El desplegable solo filtra con pulsaciones reales
 * (un `fill` cambia el valor sin disparar la búsqueda), de ahí el `pressSequentially`. Se usa el
 * `input` y no el rol `combobox` porque al desplegarse Axelor saca el control del árbol de accesibilidad.
 */
async function elegirEnDesplegable(page: Page, campo: string, valor: string): Promise<void> {
  const control = page.getByTestId(`field:${campo}`).getByTestId('input');
  await control.click();
  await control.pressSequentially(valor);
  await page.getByRole('option', { name: valor, exact: true }).click();
  await expect(control).toHaveValue(valor);
}

/**
 * Firma, desde «Firmas» → «Pendientes», la resolución del expediente. El servidor tiene el
 * certificado del firmante, así que firma él, sin AutoFirma.
 */
async function firmarResolucion(page: Page, numero: string): Promise<void> {
  await abrirMenu(page, 'firmas-pendientes-menuitem', 'firmas-menuitem');
  // El motivo de la tarea de firma es «Resolución de la solicitud de anulación de matrícula del expediente <número>».
  const tarea = page.getByRole('rowgroup').last().getByRole('row', { name: new RegExp(numero) });
  await expect(tarea).toHaveCount(1);
  await tarea.click();

  await page.getByRole('button', { name: 'Firmar todos los documentos', exact: true }).click();
  // Los certificados de demo se guardan con su contraseña (FICHERO_CON_CLAVE): no se pide al firmar.
  const firmarYFinalizar = page.getByRole('button', { name: 'Firmar todos los documentos y finalizar' });
  await firmarYFinalizar.click();
  // Firmada en el servidor, se vuelve a la bandeja (`force-back`): hasta que el formulario no se
  // cierra la firma no ha terminado, y mirar antes la bandeja daría un «0 filas» falso (no hay grid).
  await expect(firmarYFinalizar).toHaveCount(0);
  await expect(page.getByRole('columnheader', { name: 'Motivo' })).toBeVisible();
  // Firmada, la tarea sale de la bandeja de pendientes.
  await expect(page.getByRole('rowgroup').last().getByRole('row', { name: new RegExp(numero) })).toHaveCount(0);
}

test.describe('Anulación de matrícula en ciclo formativo — camino feliz', () => {
  test('Camino feliz: de la solicitud del alumno a la anulación aceptada', async ({ page, request }) => {
    // Cuatro sesiones y tres documentos firmados por el servidor: no cabe en el `timeout` global (90 s).
    test.setTimeout(300_000);

    let numero = '';
    try {
      // --- Tramo 1: el ALUMNO rellena y presenta la solicitud ---
      await ensureLoggedOut(page);
      await login(page, ALUMNO.login, ALUMNO.password);
      numero = await crearExpediente(page);
      await expect(page.getByLabel('Fase')).toHaveValue('Entrada');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Entrada de datos');

      const panelDatosAlumno = page.getByRole('region', { name: 'Datos del alumno/a' });
      await panelDatosAlumno.getByLabel('NIA').fill('12345678');
      await panelDatosAlumno.getByLabel('Teléfono').fill('612345678');
      await panelDatosAlumno.getByLabel('Dirección').fill('Calle Mayor, 1');
      await elegirEnDesplegable(page, 'municipio', 'Mislata');
      await panelDatosAlumno.getByLabel('Código Postal').fill('46920');
      await elegirEnDesplegable(page, 'ciclo', 'Desarrollo de Aplicaciones Web');

      await page.getByTestId(FOOTER).getByRole('button', { name: 'Siguiente' }).click();
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Pendiente de presentación');

      // El servidor tiene el certificado del alumno: la firma la hace él, sin AutoFirma.
      await page.getByTestId(FOOTER).getByRole('button', { name: 'Firmar y presentar la solicitud' }).click();
      await page.getByRole('dialog').getByRole('button', { name: 'Aceptar' }).click();
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Pendiente de verificación');
      await expect(page.getByLabel('Fase')).toHaveValue('Verificación');
      await logout(page);

      // --- Tramo 2: la SECRETARÍA verifica y resuelve la solicitud ---
      await login(page, ADMINISTRATIVO.login, ADMINISTRATIVO.password);
      await abrirDesdeBandeja(page, numero, 'tramitacion-pendientesDeMi-menuitem', 'tramitacion-menuitem');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Pendiente de verificación');

      const solicitudCorrecta = page.getByRole('radio', { name: 'La solicitud es correcta', exact: true });
      await solicitudCorrecta.click();
      // Si se pulsara antes de que la elección cuaje, el evento viajaría sin `resultadoVerificacion`.
      await expect(solicitudCorrecta).toHaveAttribute('aria-checked', 'true');
      await page.getByTestId(FOOTER).getByRole('button', { name: 'Siguiente' }).click();
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Pendiente de resolución');
      await expect(page.getByLabel('Fase')).toHaveValue('Resolución del centro');

      const aceptarAnulacion = page
        .getByRole('region', { name: 'Resolución de la solicitud' })
        .getByRole('radio', { name: 'Aceptar la anulación', exact: true });
      await aceptarAnulacion.click();
      await expect(aceptarAnulacion).toHaveAttribute('aria-checked', 'true');
      await page.getByTestId(FOOTER).getByRole('button', { name: 'Enviar a la firma' }).click();
      const aviso = page.getByRole('dialog');
      await expect(aviso).toContainText('Va a generar la resolución y a enviarla a la firma del secretario y del director del centro');
      await aviso.getByRole('button', { name: 'Aceptar' }).click();
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Pendiente de la firma del secretario');
      await expect(page.getByLabel('Fase')).toHaveValue('Firma de la resolución');
      await logout(page);

      // --- Tramo 3: el SECRETARIO firma la resolución en su bandeja de firmas ---
      await login(page, SECRETARIO.login, SECRETARIO.password);
      await firmarResolucion(page, numero);
      await logout(page);

      // --- Tramo 4: el DIRECTOR firma la resolución en su bandeja de firmas ---
      await login(page, DIRECTOR.login, DIRECTOR.password);
      await firmarResolucion(page, numero);

      // --- Tramo 5: la resolución firmada está en el registro de salida ---
      // Se recarga para dejar el menú plegado, que es lo que `abrirMenu` espera.
      await page.goto('/');
      await abrirMenu(page, 'registro-salida-menuitem', 'registro-menuitem');
      await expect(page.getByRole('tab', { name: 'Registro de salida' })).toBeVisible();
      // El asunto del registro de salida de un expediente es «Expediente: <número> - <nombre>».
      const filtroAsunto = page.getByTestId('column:asunto').getByPlaceholder('Buscar...');
      await filtroAsunto.fill(numero);
      await filtroAsunto.press('Enter');
      const registros = page.getByRole('rowgroup').last().getByRole('row', { name: new RegExp(numero) });
      await expect(registros).toHaveCount(1);
      await registros.click();

      // El campo «URL de descarga» es un enlace a la descarga pública por el CSV del registro.
      const panelRegistro = page.getByRole('region', { name: 'Registro de Salida' });
      const csv = panelRegistro.getByRole('textbox', { name: 'Código seguro de verificación', exact: true });
      await expect(csv).toHaveValue(/^[A-Z0-9]+$/);
      const urlDescarga = panelRegistro.getByRole('link', { name: new RegExp(`${RUTA_DESCARGA}\\?CSV=`) });
      await expect(urlDescarga).toHaveAttribute('href', new RegExp(`${RUTA_DESCARGA}\\?CSV=${await csv.inputValue()}$`));
      const url = (await urlDescarga.getAttribute('href'))!;

      // La resolución de la anulación se registra de salida sin anexos.
      await expect(page.getByRole('region', { name: 'Anexos' }).getByRole('gridcell')).toHaveCount(0);

      // El documento de salida tal cual lo descarga un usuario con sesión, para compararlo luego.
      const descargaPromise = page.waitForEvent('download');
      await page.getByTestId('field:documento').getByTestId('btn-download').click();
      const documento = fs.readFileSync(await (await descargaPromise).path());

      await page.getByRole('button', { name: 'Salir' }).click();
      await logout(page);

      // --- Tramo 6: sin sesión, la URL de descarga devuelve el PDF de la resolución ---
      // `request` no comparte las cookies de ninguna página: la petición va sin sesión.
      const respuesta = await request.get(url, { maxRedirects: 0 });
      expect(respuesta.status()).toBe(200);
      expect(respuesta.headers()['cache-control']).toBe('no-store');
      // Sin anexos se descarga el documento tal cual, no un ZIP.
      expect(respuesta.headers()['content-type']).toContain('application/pdf');
      expect((await respuesta.body()).equals(documento)).toBe(true);

      // --- Tramo 7: el ALUMNO ve su expediente cerrado con la anulación aceptada ---
      await login(page, ALUMNO.login, ALUMNO.password);
      await abrirDesdeBandeja(page, numero, 'misTramites-finalizados-menuitem', 'misTramites-menuitem');
      await expect(page.getByLabel('Fase')).toHaveValue('Cierre');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Anulación aceptada');
      // La resolución firmada por el secretario y el director, ya registrada de salida, se ve en un
      // `iframe` contra su MetaFile: que el `src` apunte a un MetaFile es lo que prueba que hay documento.
      const iframeResolucion = page.getByTestId('panel:resolucion-firmada').locator('iframe');
      await expect(iframeResolucion).toBeVisible();
      await expect(iframeResolucion).toHaveAttribute('src', /com\.axelor\.meta\.db\.MetaFile\/\d+/);
    } finally {
      // Teardown: en el camino nominal el expediente acaba en CIERRE/ACEPTADA, que es un estado cerrado
      // sin DELETE, así que QUEDA VIVO a propósito (el test siempre trabaja con SU número).
      // Si el test se corta en ENTRADA_DATOS, el alumno lo borra.
      // El `.catch(() => {})` es intencional: el teardown no debe enmascarar el fallo de una aserción.
      if (numero) {
        await (async () => {
          await page.reload();
          await ensureLoggedOut(page);
          await login(page, ALUMNO.login, ALUMNO.password);
          await abrirDesdeBandeja(page, numero, 'misTramites-pendientesDeMi-menuitem', 'misTramites-menuitem');
          const footer = page.getByTestId(FOOTER);
          await expect(footer.getByRole('button').first()).toBeVisible();
          // PENDIENTE_PRESENTACION no ofrece DELETE, pero «Atrás» lo devuelve a ENTRADA_DATOS, que sí.
          const atras = footer.getByRole('button', { name: 'Atrás' });
          if (await atras.isVisible()) {
            await atras.click();
            await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Entrada de datos');
          }
          await footer.getByRole('button', { name: 'Borrar el expediente' }).click({ timeout: 5000 });
          await page.getByRole('dialog').getByRole('button', { name: 'Aceptar' }).click();
          // Hay que esperar a que el borrado termine (se cierra su pestaña): un logout inmediato lo corta
          // y el expediente se queda vivo, bloqueando la siguiente ejecución («Ya tiene una solicitud
          // de anulación en curso para este ciclo»).
          await expect(page.getByRole('tab', { name: new RegExp(numero) })).toHaveCount(0);
        })().catch(() => {});
      }
      await logout(page).catch(() => {});
    }
  });
});
