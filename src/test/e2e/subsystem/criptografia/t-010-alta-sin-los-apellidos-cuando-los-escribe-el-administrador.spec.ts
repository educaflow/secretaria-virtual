import { test, expect, Page } from '@playwright/test';
import * as path from 'path';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-010 — Alta sin los apellidos cuando los escribe el administrador
// origen: ESC-003  |  verifica: V-CertificadoDigital-002
// fuente: .sdd/drafts/2026-09-08_02-53_nombre-apellidos-certificado-digital/test-e2e-desc/t-010-alta-sin-los-apellidos-cuando-los-escribe-el-administrador.desc.md

// El DNI del escenario. NO lleva sufijo `Date.now()` a propósito (excepción
// documentada de la regla de nombres únicos): el escenario se apoya en que ESE
// DNI **no** corresponde a ningún usuario de la aplicación, para que el alta deje
// «Apellidos» vacío y editable y el rechazo venga de la validación de los
// apellidos obligatorios, no del autorrelleno; cambiarlo destruiría lo que el
// test verifica (y además es un DNI con letra de control, que un sufijo
// invalidaría). La idempotencia frente a la BD compartida (que NO se resetea) se
// consigue con la pre-limpieza defensiva del arranque + el teardown del
// `finally`, tal y como describe el «Estado inicial de la base de datos» de la
// descripción.
const DNI = '91851115V';

// El nombre que teclea el administrador. Los «Apellidos» se dejan VACÍOS: es
// justo lo que el test verifica que la aplicación rechaza.
const NOMBRE = 'Ana';

// La opción de «Tipo de certificado» que sube el fichero del certificado y lo
// guarda en la base de datos (tipo FICHERO_BD).
const OPCION_FICHERO_BD = 'Subir un fichero con el certificado para guardarlo en la base de datos';

// El certificado de test que se sube en el alta: lo emite la CA de demo y no es
// de ningún usuario. Vive en src/test/resources (no va dentro del WAR), por eso
// no puede darse de alta como tipo CLASSPATH y se sube como FICHERO_BD.
const NOMBRE_FICHERO_CERTIFICADO = 'test1.p12';
const FICHERO_CERTIFICADO = path.resolve(__dirname, '../../../resources/firma/test', NOMBRE_FICHERO_CERTIFICADO);
const CONTRASENA_CERTIFICADO = 'demo1234';

// Texto del error que el escenario espera. En la UI el diálogo lo pinta como un
// ítem de lista «<campo>: <mensaje>», con el nombre del campo en negrita.
const CAMPO_EN_ERROR = 'apellidos:';
const MENSAJE_ERROR = 'Los apellidos son obligatorios';

const botonAnhadir = (page: Page) => page.getByRole('button', { name: 'Añadir certificado digital' });

// La fila del listado cuyo DNI es el del escenario (el nombre accesible de la
// fila es «<dni> <nombre> <apellidos> <tipo de certificado>», y el match por
// nombre es por subcadena).
// Se busca en el ÚLTIMO rowgroup (el de los datos): el primero lleva la cabecera
// y la fila de filtros, cuyo «Buscar...» de la columna DNI contiene el DNI
// tecleado (ver `filtrarPorDni`) y también casaría por nombre.
const filaDelDni = (page: Page) => page.getByRole('rowgroup').last().getByRole('row', { name: DNI });

// La fila que la rejilla pinta cuando no hay ningún registro.
const filaSinRegistros = (page: Page) => page.getByRole('row', { name: 'No se encontraron registros.' });

// Filtro de la columna «DNI» del listado (la fila de «Buscar...» de la cabecera).
// CRITICAL: el listado NO está vacío: los datos de demostración traen un
// certificado por cada usuario de demo y la rejilla pagina (40 filas por página),
// así que sin filtrar las filas del DNI del escenario pueden caer en otra página
// y la pre-limpieza, los recuentos y las posiciones no las verían. Filtrando por
// el DNI la rejilla solo muestra las suyas (o «No se encontraron registros.»).
// Axelor conserva el filtro al volver al listado tras guardar o borrar, pero no
// tras recargar la página: por eso se reaplica siempre que no esté puesto.
const filtroDni = (page: Page) => page.getByTestId('column:dni').getByPlaceholder('Buscar...');

async function filtrarPorDni(page: Page): Promise<void> {
  if ((await filtroDni(page).inputValue()) === DNI) {
    return;
  }
  await filtroDni(page).fill(DNI);
  // Se espera a la respuesta de la búsqueda: hasta entonces la rejilla sigue
  // mostrando la primera página sin filtrar.
  await Promise.all([
    page.waitForResponse(
      (respuesta) => respuesta.url().includes('CertificadoDigital/search') && respuesta.request().method() === 'POST',
    ),
    filtroDni(page).press('Enter'),
  ]);
}

// Diálogo de aviso de Axelor con un único botón de aceptar («Validation Error»
// de un alta que falló, o el «Current changes will be lost» que salta al
// abandonar un formulario que la SPA aún considera sucio). Mientras está abierto
// tapa la pantalla e intercepta cualquier clic, así que MUST cerrarse antes de
// tocar nada.
async function cerrarDialogoDeAviso(page: Page): Promise<void> {
  const ok = page.getByRole('dialog').getByRole('button', { name: /^(OK|Aceptar)$/ });
  if (await ok.isVisible().catch(() => false)) {
    await ok.click();
  }
}

// Barrera de carga del listado.
// CRITICAL: la rejilla pide sus filas en una petición aparte de la que pinta la
// vista, así que mirar las filas nada más aparecer el botón de alta es una
// condición de carrera — y esa carrera haría que la pre-limpieza no viese la fila
// residual de un run anterior y el alta muriera con un error de DNI duplicado.
// Esperar a que la rejilla se resuelva en uno de sus dos estados posibles la
// elimina: o está la fila del DNI, o está la fila «No se encontraron registros.»
// (la rejilla está filtrada por el DNI del escenario, ver `filtrarPorDni`; si
// apareciese otro estado, el test falla de forma ruidosa en vez de saltarse la
// limpieza en silencio).
async function esperarListadoCargado(page: Page): Promise<void> {
  await expect(botonAnhadir(page)).toBeVisible();
  await filtrarPorDni(page);
  await expect(filaDelDni(page).or(filaSinRegistros(page)).first()).toBeVisible();
}

// Abre el listado «Certificados digitales» desde el menú «Criptografía».
// MUST llamarse solo con la pestaña aún cerrada: una vez abierta, el título de la
// pestaña repite el texto del ítem de menú y el locator por texto sería ambiguo.
async function abrirCertificadosDigitales(page: Page): Promise<void> {
  await page.getByText('Criptografía', { exact: true }).click();
  await page.getByText('Certificados digitales', { exact: true }).click();
  await esperarListadoCargado(page);
}

// Abre el formulario de la primera fila del DNI. El aviso «Current changes will
// be lost» puede aparecer de forma asíncrona justo al salir de un formulario e
// interceptar el clic, así que se cierra antes y se reintenta.
async function abrirFilaDelDni(page: Page): Promise<void> {
  for (let intento = 0; intento < 3; intento++) {
    await cerrarDialogoDeAviso(page);
    try {
      await filaDelDni(page).first().click({ timeout: 5000 });
      await cerrarDialogoDeAviso(page);
      await expect(page.getByRole('button', { name: 'Borrar' })).toBeVisible({ timeout: 5000 });
      return;
    } catch {
      // El diálogo interceptó el clic: se cierra en la vuelta siguiente.
    }
  }
  throw new Error(`No se pudo abrir la fila del DNI ${DNI}`);
}

// Vuelve al listado desde donde esté: cierra cualquier diálogo abierto y, si hay
// un formulario delante, lo abandona confirmando el descarte de cambios.
async function volverAlListado(page: Page): Promise<void> {
  await cerrarDialogoDeAviso(page);

  const cancelar = page.getByRole('button', { name: 'Cancelar' });
  if (await cancelar.isVisible().catch(() => false)) {
    await cancelar.click();
    // Axelor pregunta antes de descartar un formulario con cambios sin guardar
    // («Current changes will be lost. Do you really want to proceed?»).
    // CRITICAL: ese diálogo se pinta de forma ASÍNCRONA, así que preguntarle a
    // `isVisible()` (que responde al instante) justo tras el clic da un falso
    // negativo, el diálogo se queda abierto tapando la pantalla y la espera del
    // listado muere por timeout. Se espera a que la SPA se resuelva en uno de sus
    // dos estados posibles: o aparece el diálogo (había cambios sin guardar), o
    // ya estamos en el listado (no los había y salió directo).
    const confirmar = page.getByRole('dialog').getByRole('button', { name: /^(OK|Aceptar|Yes|Sí)$/ });
    await confirmar.or(botonAnhadir(page)).first().waitFor();
    if (await confirmar.isVisible().catch(() => false)) {
      await confirmar.click();
    }
  }
  await esperarListadoCargado(page);
}

// Borra TODAS las entradas del DNI si siguen existiendo, partiendo de donde esté.
// Este test NO debe crear nada (el alta se rechaza), así que en condiciones
// normales no borra nada; se usa DOS veces igualmente: como pre-limpieza
// defensiva al arrancar (un run anterior de OTRO test del mismo DNI que abortase
// dejaría la fila, y entonces este alta moriría por DNI duplicado en vez de por
// los apellidos obligatorios, verificando algo distinto de lo que dice el
// escenario) y como teardown en el `finally` (si una regresión llegara a guardar
// la fila, el siguiente run no se envenena).
async function borrarEntradasDelDniSiExisten(page: Page): Promise<void> {
  await volverAlListado(page);

  // Bucle: un run anterior abortado pudo dejar más de una entrada con el mismo
  // DNI. LIMIT defensivo para no colgar el test si el borrado dejara de funcionar.
  for (let i = 0; i < 5 && (await filaDelDni(page).count()) > 0; i++) {
    await abrirFilaDelDni(page);
    await page.getByRole('button', { name: 'Borrar' }).click();
    // Diálogo de confirmación de Axelor; con el idioma del admin el botón de
    // confirmar aparece en inglés («Delete»). Se acota al diálogo para no chocar
    // con el botón «Borrar» del formulario que queda detrás.
    await page.getByRole('dialog').getByRole('button', { name: /Delete|Eliminar/ }).click();
    await esperarListadoCargado(page);
  }
  await expect(filaDelDni(page)).toHaveCount(0);
}

// Sube el certificado de test al campo «Fichero» (widget `binary-link`), que solo
// se muestra con el tipo FICHERO_BD. El widget esconde su `input[type=file]`:
// `setInputFiles` escribe en él directamente, y el botón con el nombre del fichero
// confirma que la subida terminó antes de seguir.
async function subirCertificado(page: Page): Promise<void> {
  const campoFichero = page.getByTestId('field:fichero');
  await campoFichero.locator('input[type="file"]').setInputFiles(FICHERO_CERTIFICADO);
  await expect(campoFichero.getByRole('button', { name: NOMBRE_FICHERO_CERTIFICADO })).toBeVisible();
}

test.describe('Certificados digitales', () => {
  test('Alta sin los apellidos cuando los escribe el administrador', async ({ page }) => {
    await ensureLoggedOut(page);
    // Precondición: el usuario `admin` ha iniciado sesión con «admin»/«admin».
    await login(page, 'admin', 'admin');

    try {
      // Paso 1: Dado que el administrador está en la pantalla «Certificados
      //         digitales» (menú «Criptografía» → «Certificados digitales»).
      await abrirCertificadosDigitales(page);

      // Precondición: no existe ningún certificado digital con DNI «91851115V»
      // (si quedaran de una ejecución anterior, se borran desde el listado).
      await borrarEntradasDelDniSiExisten(page);

      // Paso 2: Cuando pulsa «Añadir certificado digital».
      await botonAnhadir(page).click();
      const dni = page.getByRole('textbox', { name: 'DNI', exact: true });
      await expect(dni).toBeVisible();

      // Paso 3: Y escribe en el campo «DNI» el valor «91851115V».
      // El `Tab` saca el foco del campo: el autorrelleno de nombre y apellidos lo
      // dispara el evento de cambio, que Axelor emite al perder el foco, no al
      // teclear. MUST hacerse para que el escenario sea el real: como ese DNI no
      // es de ningún usuario, el autorrelleno no escribe nada y «Apellidos» queda
      // vacío y editable, que es la situación que el test necesita.
      await dni.fill(DNI);
      await dni.press('Tab');

      // Paso 4: Y elige en «Tipo de certificado» la opción «Subir un fichero con
      //         el certificado para guardarlo en la base de datos».
      await page.getByRole('radio', { name: OPCION_FICHERO_BD }).check();

      // Paso 5: Y sube en el campo «Fichero» el fichero
      //         «test1.p12». (El campo solo se muestra al elegir el tipo
      //         FICHERO_BD.)
      await subirCertificado(page);

      // Paso 6: Y escribe en «Nombre» el valor «Ana» y deja el campo «Apellidos»
      //         vacío. (El nombre accesible real es «Nombre ?» / «Apellidos ?»
      //         por el icono de ayuda; el match por subcadena lo cubre. Se
      //         comprueba que «Apellidos» está de verdad vacío antes de guardar:
      //         si el autorrelleno lo hubiera escrito, el alta no probaría el
      //         caso de error que describe el escenario.)
      await page.getByRole('textbox', { name: 'Nombre' }).fill(NOMBRE);
      const apellidos = page.getByRole('textbox', { name: 'Apellidos' });
      await expect(apellidos).toHaveValue('');

      // Paso 7: Y pulsa «Guardar».
      await page.getByRole('button', { name: 'Guardar' }).click();

      // Resultado esperado 1: el sistema muestra el error «Los apellidos son
      // obligatorios». Axelor lo presenta en un diálogo «Validation Error» con un
      // ítem por campo rechazado, «<campo>: <mensaje>».
      const dialogoError = page.getByRole('dialog');
      await expect(dialogoError).toBeVisible();
      await expect(dialogoError.getByRole('heading', { name: 'La acción no se completó por los siguientes motivos' })).toBeVisible();
      await expect(dialogoError.getByRole('listitem').filter({ hasText: MENSAJE_ERROR })).toContainText(CAMPO_EN_ERROR);
      await expect(dialogoError).toContainText(MENSAJE_ERROR);
      // El «Nombre» sí se rellenó, así que el rechazo MUST venir solo de los
      // apellidos: si además saliera el error del nombre, el test no estaría
      // verificando el caso del escenario.
      await expect(dialogoError).not.toContainText('El nombre es obligatorio');

      // Resultado esperado 2 (parte 1): no se guarda nada — el alta ni siquiera
      // sale del formulario (la vista sigue en /edit, no vuelve a /list).
      await expect(page).toHaveURL(/CertificadoDigital-action\/edit/);

      // Cerrar el diálogo y abandonar el formulario (descartando los cambios)
      // para poder mirar el listado.
      await cerrarDialogoDeAviso(page);
      await volverAlListado(page);

      // Resultado esperado 2 (parte 2): el listado «Certificados digitales» no
      // muestra ninguna fila con DNI «91851115V».
      // CRITICAL: se recarga la página en duro antes de mirar. La SPA de Axelor
      // sirve la rejilla desde su propio caché de la vista, así que sin recargar
      // el listado podría estar mostrando el estado que tenía ANTES del intento
      // de alta y la aserción no probaría nada sobre lo que hay en la BD.
      await page.reload();
      await esperarListadoCargado(page);
      await expect(filaDelDni(page)).toHaveCount(0);
    } finally {
      try {
        // Teardown: el escenario NO debe dejar nada creado (el alta se rechaza),
        // pero se limpia igualmente para que una regresión que sí guardase la
        // fila no envenenase los runs siguientes. Best-effort para no enmascarar
        // el fallo real de una aserción.
        await borrarEntradasDelDniSiExisten(page);
      } catch {
        // limpieza best-effort.
      }
    }

    await logout(page);
  });
});
