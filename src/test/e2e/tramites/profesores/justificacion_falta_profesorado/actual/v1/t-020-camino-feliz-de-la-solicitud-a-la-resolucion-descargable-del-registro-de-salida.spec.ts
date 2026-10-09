import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../../../../_support/auth';
import * as fs from 'fs';
import * as zlib from 'zlib';

// T-020 — Camino feliz: de la solicitud del profesor a la resolución descargable del registro de salida
// origen: —  |  CREADOR → TRAMITADOR → DIRECTOR | ENTRADA/ENTRADA_DATOS --GUARDAR_DATOS, PRESENTAR, VERIFICAR, RESOLVER--> RESOLUCION/ACEPTADO  |  tipo: happy
// fuente: escrito a mano (no viene del pipeline SDD)

const TRAMITE = 'Justificación de falta del profesorado';
const TIPO_TRAMITE = 'Trámites para el profesor';

const PROFESOR = { login: 'profesor1@mislata.es', password: 'demo1234' };
const JEFE_ESTUDIOS = { login: 'jefeestudios1@mislata.es', password: 'demo1234' };
const DIRECTOR = { login: 'director@mislata.es', password: 'demo1234' };

const FOOTER = 'panel:subsysExpedientes-template-footer-panel';
const BOTON_PRESENTAR = 'Firmar y Presentar la solicitud';
const BOTON_RESOLVER = 'Resolver el expediente';

// La descarga pública de un registro de salida por su CSV: la URL que lleva el código QR que se
// estampa en el documento. Es un contrato con los documentos ya emitidos (su QR no se puede
// cambiar), así que la ruta va aquí literal a propósito: si alguien la cambia, este test debe fallar.
const RUTA_DESCARGA = '/ws/public/registro-salida/download';

const FECHA_FALTA = diasAntes(2);

/** Justificante mínimo válido: un PDF generado en memoria, para no depender de ningún fichero del árbol. */
const JUSTIFICANTE_PDF = Buffer.from(
  '%PDF-1.4\n' +
    '1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n' +
    '2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n' +
    '3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 200 200]/Contents 4 0 R/Resources<</Font<</F1 5 0 R>>>>>>endobj\n' +
    '4 0 obj<</Length 44>>stream\nBT /F1 12 Tf 20 100 Td (Justificante) Tj ET\nendstream endobj\n' +
    '5 0 obj<</Type/Font/Subtype/Type1/BaseFont/Helvetica>>endobj\n' +
    'trailer<</Root 1 0 R>>\n%%EOF\n',
  'latin1',
);

/** Fecha relativa a hoy: el trámite solo admite faltas de los últimos 7 días. */
function diasAntes(dias: number): string {
  const fecha = new Date();
  fecha.setDate(fecha.getDate() - dias);
  const dd = String(fecha.getDate()).padStart(2, '0');
  const mm = String(fecha.getMonth() + 1).padStart(2, '0');
  return `${dd}/${mm}/${fecha.getFullYear()}`;
}

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
 * Filtra el grid ya abierto por `columna` y devuelve sus filas de datos que contienen `texto`.
 * El último rowgroup es el de los datos; el primero lleva la cabecera y la fila de filtros
 * (que también contiene el texto tecleado y haría ambigua la búsqueda por rol).
 */
async function filtrarGrid(page: Page, columna: string, texto: string) {
  const filtro = page.getByTestId(`column:${columna}`).getByPlaceholder('Buscar...');
  await filtro.fill(texto);
  await filtro.press('Enter');
  return page.getByRole('rowgroup').last().getByRole('row', { name: new RegExp(texto) });
}

/** Abre desde una lista del menú el expediente cuyo número se pasa (nunca «la primera fila»). */
async function abrirDesdeBandeja(page: Page, numero: string, entrada: string, ...grupos: string[]): Promise<void> {
  await abrirMenu(page, entrada, ...grupos);
  await (await filtrarGrid(page, 'numeroExpediente', numero)).click();
  await expect(page.getByRole('tab', { name: new RegExp(numero) })).toBeVisible();
}

/**
 * Crea un expediente del trámite desde «Mis trámites» → «Nuevo trámite» y devuelve el NÚMERO que
 * le ha asignado el servidor, que es lo único que identifica a lo creado en una BD que no se resetea.
 */
async function crearExpediente(page: Page): Promise<string> {
  await abrirMenu(page, 'misTramites-nuevoTramite-menuitem', 'misTramites-menuitem');

  // Árbol de trámites: los tipos de trámite (nivel 1) nacen plegados y sus trámites
  // (nivel 2) no existen en el DOM hasta desplegarlos.
  const arbol = page.getByTestId('panel:tramitesPanel');
  await arbol.locator('[role="row"][aria-level="1"]').filter({ hasText: TIPO_TRAMITE }).click();
  await arbol.locator('[role="row"][aria-level="2"]').filter({ hasText: TRAMITE }).click();

  await page.getByRole('button', { name: 'Crear expediente' }).click();

  // La pestaña se titula «<número>/<año>-<códigoCentro>-<tipo de expediente>»: el número del
  // expediente son los dos primeros trozos separados por «-».
  const pestana = page.getByRole('tab').last();
  await expect(pestana).toContainText(new RegExp(`\\d{4,}/\\d{4}-\\d+-${TRAMITE}`));
  const numero = (await pestana.textContent())!.split('-').slice(0, 2).join('-').trim();
  expect(numero).toMatch(/^\d{4,}\/\d{4}-\d+$/);
  return numero;
}

/** Los ficheros de un ZIP (nombre → contenido), leídos de su directorio central. */
function descomprimir(zip: Buffer): Map<string, Buffer> {
  const ficheros = new Map<string, Buffer>();
  const finDirectorio = zip.lastIndexOf(Buffer.from('PK\x05\x06', 'latin1'));
  let entrada = zip.readUInt32LE(finDirectorio + 16);
  for (let i = 0; i < zip.readUInt16LE(finDirectorio + 10); i++) {
    const metodo = zip.readUInt16LE(entrada + 10);
    const longitudComprimido = zip.readUInt32LE(entrada + 20);
    const longitudNombre = zip.readUInt16LE(entrada + 28);
    const cabecera = zip.readUInt32LE(entrada + 42);
    const nombre = zip.toString('utf8', entrada + 46, entrada + 46 + longitudNombre);
    const datos = cabecera + 30 + zip.readUInt16LE(cabecera + 26) + zip.readUInt16LE(cabecera + 28);
    const comprimido = zip.subarray(datos, datos + longitudComprimido);
    ficheros.set(nombre, metodo === 0 ? comprimido : zlib.inflateRawSync(comprimido));
    entrada += 46 + longitudNombre + zip.readUInt16LE(entrada + 30) + zip.readUInt16LE(entrada + 32);
  }
  return ficheros;
}

test.describe('Justificación de falta del profesorado — camino feliz', () => {
  test(
    'Camino feliz: de la solicitud del profesor a la resolución descargable del registro de salida',
    async ({ page, request }) => {
      // Tres sesiones y tres documentos firmados por el servidor (solicitud, resolución y registro
      // de salida): no cabe en el `timeout` global de `playwright.config.ts` (90 s).
      test.setTimeout(300_000);

      // --- Tramo 1: el PROFESOR rellena y presenta la solicitud ---
      await ensureLoggedOut(page);
      await login(page, PROFESOR.login, PROFESOR.password);
      const numero = await crearExpediente(page);
      await expect(page.getByLabel('Fase')).toHaveValue('Entrada');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Entrada de datos');

      const panelFalta = page.getByRole('region', { name: 'Datos de la falta' });
      await panelFalta.getByRole('radio', { name: 'Un día completo', exact: true }).click();
      // Elegir el tipo repinta el panel desde el servidor: hay que esperar a que esté el campo del
      // nuevo tipo, o el re-render llega después del `fill` y se lleva lo ya tecleado.
      const fecha = panelFalta.getByRole('textbox', { name: 'Fecha', exact: true });
      await expect(fecha).toBeVisible();
      await fecha.fill(FECHA_FALTA);
      await panelFalta.getByRole('radio', { name: 'Traslado de domicilio' }).click();

      // El widget `binary-link` esconde su `input[type=file]`; `setInputFiles` escribe en él igualmente.
      await page
        .getByTestId('field:justificante')
        .locator('input[type="file"]')
        .setInputFiles({ name: 'justificante.pdf', mimeType: 'application/pdf', buffer: JUSTIFICANTE_PDF });
      await expect(page.getByTestId('field:justificante').getByRole('button', { name: 'justificante.pdf' })).toBeVisible();

      await page.getByTestId(FOOTER).getByRole('button', { name: 'Siguiente' }).click();
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Pendiente de presentación');

      // El servidor tiene el certificado del profesor: la firma la hace él, sin AutoFirma.
      await page.getByTestId(FOOTER).getByRole('button', { name: BOTON_PRESENTAR }).click();
      await page.getByRole('dialog').getByRole('button', { name: 'Aceptar' }).click();
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Pendiente de verificación');
      await expect(page.getByLabel('Fase')).toHaveValue('Verificación');
      await logout(page);

      // --- Tramo 2: la JEFATURA DE ESTUDIOS verifica la solicitud ---
      await login(page, JEFE_ESTUDIOS.login, JEFE_ESTUDIOS.password);
      await abrirDesdeBandeja(page, numero, 'tramitacion-pendientesDeMi-menuitem', 'tramitacion-menuitem');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Pendiente de verificación');

      const solicitudCorrecta = page
        .getByRole('region', { name: 'Verificación de la solicitud' })
        .getByRole('radio', { name: 'La solicitud es correcta', exact: true });
      await solicitudCorrecta.click();
      // Si se pulsara antes de que la elección cuaje, el evento viajaría sin `resultadoVerificacion`.
      await expect(solicitudCorrecta).toHaveAttribute('aria-checked', 'true');
      await page.getByTestId(FOOTER).getByRole('button', { name: 'Siguiente' }).click();
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Pendiente de resolución');
      await expect(page.getByLabel('Fase')).toHaveValue('Resolución de la dirección');
      await logout(page);

      // --- Tramo 3: la DIRECCIÓN resuelve positivamente ---
      await login(page, DIRECTOR.login, DIRECTOR.password);
      await abrirDesdeBandeja(page, numero, 'tramitacion-pendientesDeMi-menuitem', 'tramitacion-menuitem');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Pendiente de resolución');

      const resolverPositivamente = page
        .getByRole('region', { name: 'Resolver expediente' })
        .getByRole('radio', { name: 'Resolver positivamente', exact: true });
      await resolverPositivamente.click();
      await expect(resolverPositivamente).toHaveAttribute('aria-checked', 'true');
      await page.getByTestId(FOOTER).getByRole('button', { name: BOTON_RESOLVER }).click();
      await page.getByRole('dialog').getByRole('button', { name: 'Aceptar' }).click();

      // Generar, firmar y registrar de salida la resolución es lento.
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Aceptado', { timeout: 120_000 });

      // --- Tramo 4: la resolución está en el registro de salida ---
      // Se recarga para dejar el menú plegado, que es lo que `abrirMenu` espera.
      await page.goto('/');
      await abrirMenu(page, 'registro-salida-menuitem', 'registro-menuitem');
      await expect(page.getByRole('tab', { name: 'Registro de salida' })).toBeVisible();
      // El asunto del registro de salida de un expediente es «Expediente: <número> - <nombre>».
      const filas = await filtrarGrid(page, 'asunto', numero);
      await expect(filas).toHaveCount(1);
      await filas.click();

      // El campo «URL de descarga» es un enlace a la descarga pública por el CSV del registro.
      const panelRegistro = page.getByRole('region', { name: 'Registro de Salida' });
      const csv = panelRegistro.getByRole('textbox', { name: 'Código seguro de verificación', exact: true });
      await expect(csv).toHaveValue(/^[A-Z0-9]+$/);
      const urlDescarga = panelRegistro.getByRole('link', { name: new RegExp(`${RUTA_DESCARGA}\\?CSV=`) });
      await expect(urlDescarga).toHaveAttribute('href', new RegExp(`${RUTA_DESCARGA}\\?CSV=${await csv.inputValue()}$`));
      const url = (await urlDescarga.getAttribute('href'))!;

      // El documento de salida tal cual lo descarga un usuario con sesión, para compararlo luego.
      const botonDocumento = page.getByTestId('field:documento').getByTestId('btn-download');
      const nombreDocumento = (await botonDocumento.innerText()).trim();
      const nombresAnexos = await page.getByRole('region', { name: 'Anexos' }).getByRole('gridcell').allInnerTexts();
      // La resolución lleva como anexo el justificante que presentó el profesor.
      expect(nombresAnexos).toHaveLength(1);
      const descargaPromise = page.waitForEvent('download');
      await botonDocumento.click();
      const documento = fs.readFileSync(await (await descargaPromise).path());

      await page.getByRole('button', { name: 'Salir' }).click();
      await logout(page);

      // --- Tramo 5: sin sesión, la URL de descarga devuelve el documento de salida y sus anexos ---
      // `request` no comparte las cookies de ninguna página: la petición va sin sesión.
      const respuesta = await request.get(url, { maxRedirects: 0 });
      expect(respuesta.status()).toBe(200);
      expect(respuesta.headers()['cache-control']).toBe('no-store');
      // Con anexos se descarga un ZIP con el documento y los anexos.
      expect(respuesta.headers()['content-type']).toContain('application/zip');
      const ficheros = descomprimir(await respuesta.body());
      expect([...ficheros.keys()].sort()).toEqual([nombreDocumento, ...nombresAnexos].sort());
      expect(ficheros.get(nombreDocumento)!.equals(documento)).toBe(true);
    },
  );
});
