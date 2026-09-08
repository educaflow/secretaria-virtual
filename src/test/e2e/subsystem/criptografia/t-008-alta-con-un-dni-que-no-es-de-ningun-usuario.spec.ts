import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-008 — Alta con un DNI que no es de ningún usuario
// origen: ESC-002  |  verifica: R-CertificadoDigital-001, U-certificados-digitales-002
// fuente: .sdd/drafts/2026-09-08_02-53_nombre-apellidos-certificado-digital/test-e2e-desc/t-008-alta-con-un-dni-que-no-es-de-ningun-usuario.desc.md

// El DNI del escenario. NO lleva sufijo `Date.now()` a propósito (excepción
// documentada de la regla de nombres únicos): el escenario entero se apoya en que
// ESE DNI **no** corresponde a ningún usuario de la aplicación, para que el alta
// deje «Nombre» y «Apellidos» vacíos y editables; cambiarlo destruiría lo que el
// test verifica (y además es un DNI con letra de control, que un sufijo
// invalidaría). La idempotencia frente a la BD compartida (que NO se resetea) se
// consigue con la pre-limpieza defensiva del arranque + el teardown del
// `finally`, tal y como describe el «Estado inicial de la base de datos» de la
// descripción.
const DNI = '12345678Z';

// Nombre y apellidos que teclea el administrador: al no haber usuario con ese
// DNI, el alta NO los rellena sola.
const NOMBRE = 'Ana';
const APELLIDOS = 'García López';

// El título del enum llega a la UI con el sufijo `__!!` (marca de "no traducir"
// que el script de i18n elimina al generar los CSV, pero que el título crudo del
// dominio conserva). Los locators por nombre accesible hacen match por SUBCADENA,
// así que basta con el texto sin el sufijo.
const OPCION_CLASSPATH = 'Usar un fichero con el certificado que ya está dentro del del WAR';

const RUTA_CLASSPATH = 'firma/mi_certificado.p12';

const botonAnhadir = (page: Page) => page.getByRole('button', { name: 'Añadir certificado digital' });

// La fila del listado cuyo DNI es el del escenario (el nombre accesible de la
// fila es «<dni> <nombre> <apellidos> <tipo de certificado>», y el match por
// nombre es por subcadena).
const filaDelDni = (page: Page) => page.getByRole('row', { name: DNI });

// La fila que la rejilla pinta cuando no hay ningún registro.
const filaSinRegistros = (page: Page) => page.getByRole('row', { name: 'No records found.' });

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
// elimina: o está la fila del DNI, o está la fila «No records found.» (esta tabla
// solo la escriben los tests de esta iniciativa y la anterior, y ambas limpian lo
// que crean; si apareciese otro estado, el test falla de forma ruidosa en vez de
// saltarse la limpieza en silencio).
async function esperarListadoCargado(page: Page): Promise<void> {
  await expect(botonAnhadir(page)).toBeVisible();
  await expect(filaDelDni(page).or(filaSinRegistros(page)).first()).toBeVisible();
}

// Abre el listado «Certificados digitales» desde el menú «Administración SV».
// MUST llamarse solo con la pestaña aún cerrada: una vez abierta, el título de la
// pestaña repite el texto del ítem de menú y el locator por texto sería ambiguo.
async function abrirCertificadosDigitales(page: Page): Promise<void> {
  await page.getByText('Administración SV', { exact: true }).click();
  await page.getByText('Certificados digitales', { exact: true }).click();
  await esperarListadoCargado(page);
}

// Abre el formulario de la primera fila del DNI. El aviso «Current changes will
// be lost» puede aparecer de forma asíncrona justo al salir del formulario recién
// guardado e interceptar el clic, así que se cierra antes y se reintenta.
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

// Borra TODAS las entradas del DNI si siguen existiendo, partiendo del listado.
// Se usa DOS veces: como pre-limpieza defensiva al arrancar (un run anterior que
// abortó deja la entrada creada y el alta con ese DNI volvería a fallar) y como
// teardown en el `finally`.
async function borrarEntradasDelDniSiExisten(page: Page): Promise<void> {
  await cerrarDialogoDeAviso(page);

  // Si quedó un formulario abierto (p.ej. el test falló a mitad del alta), vuelve
  // al listado antes de buscar la fila.
  const cancelar = page.getByRole('button', { name: 'Cancelar' });
  if (await cancelar.isVisible().catch(() => false)) {
    await cancelar.click();
    // Axelor pregunta antes de descartar un formulario con cambios sin guardar.
    const confirmar = page.getByRole('dialog').getByRole('button', { name: /^(OK|Aceptar|Yes|Sí)$/ });
    if (await confirmar.isVisible().catch(() => false)) {
      await confirmar.click();
    }
  }
  await esperarListadoCargado(page);

  // Bucle: el escenario deja una sola entrada, pero un run anterior abortado pudo
  // dejar más de una con el mismo DNI. LIMIT defensivo para no colgar el test si
  // el borrado dejara de funcionar.
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

test.describe('Certificados digitales', () => {
  test('Alta con un DNI que no es de ningún usuario', async ({ page }) => {
    await ensureLoggedOut(page);
    // Precondición: el usuario `admin` ha iniciado sesión con «admin»/«admin».
    await login(page, 'admin', 'admin');

    try {
      // Paso 1: Dado que el administrador está en la pantalla «Certificados
      //         digitales» (menú «Administración SV» → «Certificados digitales»).
      await abrirCertificadosDigitales(page);

      // Precondición: no existe ningún certificado digital con DNI «12345678Z»
      // (si quedaran de una ejecución anterior, se borran desde el listado).
      await borrarEntradasDelDniSiExisten(page);

      // Paso 2: Cuando pulsa «Añadir certificado digital».
      await botonAnhadir(page).click();
      const dni = page.getByRole('textbox', { name: 'DNI', exact: true });
      await expect(dni).toBeVisible();

      // Paso 3: Y escribe en el campo «DNI» el valor «12345678Z».
      // El `Tab` saca el foco del campo: el autorrelleno de nombre y apellidos lo
      // dispara el evento de cambio, que Axelor emite al perder el foco, no al
      // teclear. Aquí MUST hacerse igualmente para comprobar que, con un DNI que
      // no es de ningún usuario, ese evento NO rellena nada.
      await dni.fill(DNI);
      await dni.press('Tab');

      // Paso 4: Entonces los campos «Nombre» y «Apellidos» están vacíos y son
      //         editables. (El nombre accesible real es «Nombre ?» / «Apellidos ?»
      //         por el icono de ayuda; el match por subcadena lo cubre. El «solo
      //         lectura» de Axelor se renderiza como input deshabilitado, así que
      //         «editable» se comprueba con `toBeEnabled()`.)
      const nombre = page.getByRole('textbox', { name: 'Nombre' });
      const apellidos = page.getByRole('textbox', { name: 'Apellidos' });
      await expect(nombre).toHaveValue('');
      await expect(apellidos).toHaveValue('');
      await expect(nombre).toBeEnabled();
      await expect(apellidos).toBeEnabled();

      // Paso 5: Cuando escribe en «Nombre» el valor «Ana» y en «Apellidos» el
      //         valor «García López».
      await nombre.fill(NOMBRE);
      await apellidos.fill(APELLIDOS);

      // Paso 6: Y elige en «Tipo de certificado» la opción «Usar un fichero con
      //         el certificado que ya está dentro del del WAR».
      await page.getByRole('combobox', { name: 'Tipo de certificado' }).click();
      await page.getByRole('option', { name: OPCION_CLASSPATH }).click();

      // Paso 7: Y escribe en el campo «Ruta classpath» el valor
      //         «firma/mi_certificado.p12». (El campo solo se muestra al elegir
      //         el tipo CLASSPATH; su etiqueta real es «Ruta classpath__!!».)
      await page.getByRole('textbox', { name: 'Ruta classpath' }).fill(RUTA_CLASSPATH);

      // Paso 8: Y pulsa «Guardar».
      await page.getByRole('button', { name: 'Guardar' }).click();

      // Resultado esperado 1: el sistema guarda el certificado y vuelve al listado
      // «Certificados digitales» (la vista pasa de /edit a /list y reaparece el
      // botón de alta, que solo existe en la vista de lista).
      await expect(page).toHaveURL(/CertificadoDigital-action\/list/);
      await expect(botonAnhadir(page)).toBeVisible();

      // Resultado esperado 2: el listado muestra una fila con DNI «12345678Z»,
      // «Nombre» «Ana», «Apellidos» «García López», «Tipo de certificado» «Usar un
      // fichero con el certificado que ya está dentro del del WAR» y «Habilitado»
      // marcado (la celda de esa columna es la única casilla de la fila: la
      // rejilla no tiene columna de selección).
      const fila = filaDelDni(page);
      await expect(fila).toBeVisible();
      await expect(fila.getByRole('gridcell', { name: DNI, exact: true })).toBeVisible();
      await expect(fila.getByRole('gridcell', { name: NOMBRE, exact: true })).toBeVisible();
      await expect(fila.getByRole('gridcell', { name: APELLIDOS, exact: true })).toBeVisible();
      await expect(fila.getByRole('gridcell', { name: OPCION_CLASSPATH })).toBeVisible();
      await expect(fila.getByRole('checkbox')).toBeChecked();
    } finally {
      try {
        // Teardown: el alta deja el certificado creado, y su DNI no puede
        // repetirse habilitado, así que MUST borrarse para que el test se pueda
        // reejecutar sin limpiar la BD a mano. Best-effort para no enmascarar el
        // fallo real de una aserción.
        await borrarEntradasDelDniSiExisten(page);
      } catch {
        // limpieza best-effort.
      }
    }

    await logout(page);
  });
});
