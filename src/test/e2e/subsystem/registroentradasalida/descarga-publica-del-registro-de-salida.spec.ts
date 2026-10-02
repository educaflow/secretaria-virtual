import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';
import * as fs from 'fs';
import * as zlib from 'zlib';

// La descarga pública de un registro de salida por su CSV: la URL que lleva el código QR que se
// estampa en el documento. Es un contrato con los documentos ya emitidos (su QR no se puede
// cambiar), así que la ruta va aquí literal a propósito: si alguien la cambia, este test debe fallar.
// Escrito a mano (no viene del pipeline SDD): no tiene `.desc.md`.
const RUTA_DESCARGA = '/ws/public/registro-salida/download';
const RUTA_VALIDATE = '/ws/public/registro-salida/validate-download';
// El metadato del PDF de un registro de salida que lleva su CSV (RegistroSalidaService.METADATO_CSV).
const METADATO_CSV = 'SecretariaVirtualCSV';

const MENSAJE_NO_EXISTE = 'No existe ningún documento con ese código de verificación.';
// Con forma de CSV (26 caracteres de su alfabeto) pero que no es de ningún registro.
const CSV_INEXISTENTE = '00000000000000000000000000';

/**
 * El CSV que el documento de un registro de salida lleva en sus metadatos, o `null` si no lo lleva.
 * iText escribe el diccionario de información sin comprimir, así que el metadato se lee del propio
 * texto del PDF; si hay varios (una revisión por firma) vale el último.
 */
function leerCsvDelMetadato(pdf: Buffer): string | null {
  const metadatos = [...pdf.toString('latin1').matchAll(new RegExp(`/${METADATO_CSV}\\s*\\(([^)]*)\\)`, 'g'))];
  return metadatos.length === 0 ? null : metadatos[metadatos.length - 1][1];
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

async function abrirRegistroDeSalida(page: Page): Promise<void> {
  const entrada = page.getByTestId('item:registro-salida-menuitem');
  if (!(await entrada.isVisible())) {
    await page.getByTestId('item:registro-menuitem').getByTestId('title').first().click();
  }
  await entrada.click();
  await expect(page.getByRole('tab', { name: 'Registro de salida' })).toBeVisible();
  await expect(page.getByRole('columnheader', { name: 'Num. registro' })).toBeVisible();
}

test.describe('Descarga pública de un registro de salida por su CSV', () => {
  // `request` no comparte las cookies de ninguna página: todas sus peticiones van sin sesión.

  test('Sin iniciar sesión, un CSV que no es de ningún registro responde que el documento no existe', async ({ request }) => {
    const respuesta = await request.get(RUTA_DESCARGA, { params: { CSV: CSV_INEXISTENTE }, maxRedirects: 0 });

    // Un 404 propio de la descarga, no la redirección al login de una ruta protegida.
    expect(respuesta.status()).toBe(404);
    expect(await respuesta.text()).toBe(MENSAJE_NO_EXISTE);
  });

  test('Sin iniciar sesión, algo que no tiene forma de CSV responde que el documento no existe', async ({ request }) => {
    for (const csv of ["' or '1'='1", '../../etc/passwd', '']) {
      const respuesta = await request.get(RUTA_DESCARGA, { params: { CSV: csv }, maxRedirects: 0 });

      expect(respuesta.status(), `CSV «${csv}»`).toBe(404);
      expect(await respuesta.text()).toBe(MENSAJE_NO_EXISTE);
    }
  });

  test('Sin iniciar sesión, validar un CSV que no es de ningún registro devuelve el mensaje de la validación', async ({ request }) => {
    const respuesta = await request.get(RUTA_VALIDATE, { params: { CSV: CSV_INEXISTENTE }, maxRedirects: 0 });

    expect(respuesta.status()).toBe(400);
    expect((await respuesta.json()).errors[0].message).toBe(MENSAJE_NO_EXISTE);
  });

  test('Sin iniciar sesión, con el CSV que lleva el documento en sus metadatos se descarga ese registro de salida', async ({ page, request }) => {
    // Paso 1: el administrador abre el último registro de salida y descarga su documento.
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');
    await abrirRegistroDeSalida(page);

    // Un registro de salida solo lo crea la resolución de un expediente, que exige firmar (tests
    // @manual de los trámites): este test no puede crearlo, usa el último que haya.
    const filas = page.getByRole('rowgroup').last().getByRole('row');
    const hayRegistros = await expect(filas.first()).toBeVisible({ timeout: 5000 }).then(() => true, () => false);
    test.skip(!hayRegistros, 'No hay ningún registro de salida: resuelve antes un expediente (tests @manual de los trámites).');

    await filas.last().click();
    const botonDocumento = page.getByTestId('field:documento').getByTestId('btn-download');
    await expect(botonDocumento).toBeVisible();
    const nombreDocumento = (await botonDocumento.innerText()).trim();
    const nombresAnexos = await page.getByRole('region', { name: 'Anexos' }).getByRole('gridcell').allInnerTexts();

    const descargaPromise = page.waitForEvent('download');
    await botonDocumento.click();
    const documento = fs.readFileSync(await (await descargaPromise).path());

    await page.getByRole('button', { name: 'Salir' }).click();
    await logout(page);

    // Paso 2: el CSV sale del propio documento, como lo tendría quien solo tiene el PDF.
    const csv = leerCsvDelMetadato(documento);
    test.skip(csv === null, `El último registro de salida es anterior al metadato «${METADATO_CSV}»: resuelve un expediente con la versión actual.`);

    // Paso 3: sin sesión, la URL del código QR devuelve el registro de salida.
    const respuesta = await request.get(RUTA_DESCARGA, { params: { CSV: csv! }, maxRedirects: 0 });

    expect(respuesta.status()).toBe(200);
    expect(respuesta.headers()['cache-control']).toBe('no-store');
    const descarga = await respuesta.body();
    if (nombresAnexos.length === 0) {
      // Sin anexos se descarga el documento tal cual.
      expect(respuesta.headers()['content-type']).toContain('application/pdf');
      expect(descarga.equals(documento)).toBe(true);
    } else {
      // Con anexos, un ZIP con el documento y los anexos.
      expect(respuesta.headers()['content-type']).toContain('application/zip');
      const ficheros = descomprimir(descarga);
      expect([...ficheros.keys()].sort()).toEqual([nombreDocumento, ...nombresAnexos].sort());
      expect(ficheros.get(nombreDocumento)!.equals(documento)).toBe(true);
    }

    // Y validar ese mismo CSV dice que es válido.
    const validacion = await request.get(RUTA_VALIDATE, { params: { CSV: csv! }, maxRedirects: 0 });
    expect(validacion.status()).toBe(200);
  });
});
