import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../_support/auth';

// T-001 — Alta correcta: el servidor sella las fechas de creación y última modificación
// origen: ESC-001  |  verifica: R-SmokeTest-001, R-SmokeTest-002
// fuente: .sdd/drafts/2026-06-30_11-18_smoke-test-desarrollador/test-e2e-desc/t-001-alta-correcta-el-servidor-sella-las-fechas-de-creacion-y-ultima-modificacion.desc.md

// Formato de las fechas que muestra Axelor: "DD/MM/YYYY HH:mm".
const FECHA_RE = /^\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}$/;

// Convierte "DD/MM/YYYY HH:mm" a epoch ms (hora local del host, que es la misma
// del servidor de la app). El valor se trunca al minuto, así que nunca queda por
// delante del sello real del servidor.
function parseFecha(s: string): number {
  const m = s.match(/^(\d{2})\/(\d{2})\/(\d{4}) (\d{2}):(\d{2})$/);
  if (!m) return NaN;
  const [, dd, MM, yyyy, hh, mm] = m;
  return new Date(+yyyy, +MM - 1, +dd, +hh, +mm).getTime();
}

// Abre el menú «Desarrollador» → «Smoke test» y espera la lista.
async function abrirSmokeTest(page: Page): Promise<void> {
  await page.getByText('Desarrollador', { exact: true }).click();
  await page.getByText('Smoke test', { exact: true }).click();
  await expect(page.getByRole('button', { name: 'Añadir un nuevo smoke test' })).toBeVisible();
}

// Teardown idempotente: borra el registro con ese `texto` si sigue en la lista.
// Best-effort para no enmascarar el fallo real de una aserción.
async function borrarSmokeTestSiExiste(page: Page, texto: string): Promise<void> {
  await abrirSmokeTest(page);
  const fila = page.getByText(texto, { exact: true });
  if ((await fila.count()) === 0) return;
  await fila.first().click();
  await expect(page.getByRole('button', { name: 'Borrar' })).toBeVisible();
  await page.getByRole('button', { name: 'Borrar' }).click();
  // Diálogo de confirmación ("¿Realmente quieres eliminar...?"). El botón de
  // confirmar aparece en inglés ("Delete") con el idioma del admin.
  await page.getByRole('button', { name: /Delete|Eliminar/ }).click();
  await expect(page.getByRole('button', { name: 'Añadir un nuevo smoke test' })).toBeVisible();
}

test.describe('Smoke test (Desarrollador)', () => {
  test('Alta correcta: el servidor sella las fechas de creación y última modificación', async ({ page }) => {
    await ensureLoggedOut(page);
    // Paso 1: Dado que el usuario `admin` inicia sesión con contraseña `admin`.
    await login(page, 'admin', 'admin');

    // Idempotencia (BD compartida, NO se resetea entre ejecuciones): el texto lleva un
    // sufijo único por ejecución para no colisionar con registros de runs previos, y el
    // registro creado se BORRA en el `finally` aunque falle una aserción → test repetible.
    // El escenario usa "Prueba de humo 1"; el sufijo no afecta a lo que verifica el test.
    const texto = `Prueba de humo 1 t001-${Date.now()}`;

    try {
      // Paso 2: Cuando abre el menú «Desarrollador» → «Smoke test».
      await abrirSmokeTest(page);

      // Paso 3: Y pulsa el botón «Añadir un nuevo smoke test».
      await page.getByRole('button', { name: 'Añadir un nuevo smoke test' }).click();
      await expect(page.getByRole('textbox', { name: 'Texto' })).toBeVisible();

      // Paso 4: Y escribe «Prueba de humo 1» en el campo «Texto».
      await page.getByRole('textbox', { name: 'Texto' }).fill(texto);

      // Paso 5: Y pulsa el botón «Guardar».
      await page.getByRole('button', { name: 'Guardar' }).click();
      // Tras guardar, Axelor vuelve a la lista; el registro guardado aparece en ella.
      await expect(page.getByRole('button', { name: 'Añadir un nuevo smoke test' })).toBeVisible();
      await expect(page.getByText(texto, { exact: true })).toBeVisible();

      // Reabrir el registro para comprobar el formulario y los campos de fecha.
      await page.getByText(texto, { exact: true }).click();

      // Resultado esperado 1: el sistema guarda el registro y el formulario muestra el texto.
      await expect(page.getByRole('textbox', { name: 'Texto' })).toHaveValue(texto);

      const fCreacion = page.getByRole('textbox', { name: 'Fecha de creación' });
      const fModificacion = page.getByRole('textbox', { name: 'Fecha de última modificación' });

      // «El momento actual» de la comprobación. parseFecha va truncada al minuto, así que
      // un sello legítimo del servidor nunca supera este instante; una tolerancia pequeña
      // absorbe el redondeo al minuto y cualquier desfase de segundos entre relojes.
      const ahora = Date.now();

      // Resultado esperado 2: «Fecha de creación» tiene un valor de fecha/hora no vacío
      //                       igual o anterior al momento actual.
      const valCreacion = await fCreacion.inputValue();
      expect(valCreacion).toMatch(FECHA_RE);
      expect(parseFecha(valCreacion)).toBeLessThanOrEqual(ahora + 60_000);

      // Resultado esperado 3: «Fecha de última modificación» tiene un valor de fecha/hora
      //                       no vacío igual o anterior al momento actual.
      const valModificacion = await fModificacion.inputValue();
      expect(valModificacion).toMatch(FECHA_RE);
      expect(parseFecha(valModificacion)).toBeLessThanOrEqual(ahora + 60_000);

      // Resultado esperado 4: ninguno de los dos campos de fecha fue introducido por el
      //                       administrador → ambos son de solo lectura (disabled) en el
      //                       formulario, así que el admin no pudo escribirlos.
      await expect(fCreacion).toBeDisabled();
      await expect(fModificacion).toBeDisabled();
    } finally {
      try {
        await borrarSmokeTestSiExiste(page, texto);
      } catch {
        // limpieza best-effort: no debe enmascarar el fallo real de una aserción.
      }
    }

    await logout(page);
  });
});
