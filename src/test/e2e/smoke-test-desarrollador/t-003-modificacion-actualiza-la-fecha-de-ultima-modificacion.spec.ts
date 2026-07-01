import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../_support/auth';

// T-003 — Modificación actualiza la fecha de última modificación
// origen: ESC-003  |  verifica: R-SmokeTest-002
// fuente: .sdd/drafts/2026-06-30_11-18_smoke-test-desarrollador/test-e2e-desc/t-003-modificacion-actualiza-la-fecha-de-ultima-modificacion.desc.md

// Formato de las fechas que muestra Axelor: "DD/MM/YYYY HH:mm".
const FECHA_RE = /^\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}$/;

// Convierte "DD/MM/YYYY HH:mm" a epoch ms (hora local del host, la misma del
// servidor). El valor se trunca al minuto; comparaciones ">=" toleran ese redondeo.
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
  // Diálogo de confirmación; el botón de confirmar aparece en inglés ("Delete")
  // con el idioma del admin.
  await page.getByRole('button', { name: /Delete|Eliminar/ }).click();
  await expect(page.getByRole('button', { name: 'Añadir un nuevo smoke test' })).toBeVisible();
}

test.describe('Smoke test (Desarrollador)', () => {
  test('Modificación actualiza la fecha de última modificación', async ({ page }) => {
    await ensureLoggedOut(page);
    // Paso 1: Dado que el usuario `admin` inicia sesión con contraseña `admin`.
    await login(page, 'admin', 'admin');

    // Idempotencia (BD compartida, NO se resetea entre ejecuciones): los textos llevan un
    // sufijo único por ejecución para no colisionar con registros de runs previos, y el
    // registro creado se BORRA en el `finally` aunque falle una aserción → test repetible.
    // El escenario usa "Prueba de humo 3" / "Prueba de humo 3 editada"; el sufijo no afecta
    // a lo que verifica el test.
    const stamp = Date.now();
    const textoOriginal = `Prueba de humo 3 t003-${stamp}`;
    const textoEditado = `Prueba de humo 3 editada t003-${stamp}`;

    try {
      // Paso 2: Cuando abre el menú «Desarrollador» → «Smoke test».
      await abrirSmokeTest(page);

      // Paso 3: Y pulsa «Añadir un nuevo smoke test», escribe «Prueba de humo 3» en
      //         «Texto» y pulsa «Guardar».
      await page.getByRole('button', { name: 'Añadir un nuevo smoke test' }).click();
      await expect(page.getByRole('textbox', { name: 'Texto' })).toBeVisible();
      await page.getByRole('textbox', { name: 'Texto' }).fill(textoOriginal);
      await page.getByRole('button', { name: 'Guardar' }).click();
      // Tras guardar, Axelor vuelve a la lista; el registro guardado aparece en ella.
      await expect(page.getByRole('button', { name: 'Añadir un nuevo smoke test' })).toBeVisible();
      await expect(page.getByText(textoOriginal, { exact: true })).toBeVisible();

      // Paso 4: Y anota el valor del campo «Fecha de última modificación» mostrado (FechaMod1).
      // Se reabre el registro para leer los campos de fecha del formulario.
      await page.getByText(textoOriginal, { exact: true }).click();
      await expect(page.getByRole('textbox', { name: 'Texto' })).toHaveValue(textoOriginal);

      const fCreacion = page.getByRole('textbox', { name: 'Fecha de creación' });
      const fModificacion = page.getByRole('textbox', { name: 'Fecha de última modificación' });

      const fechaCreacion1 = await fCreacion.inputValue();
      const fechaMod1 = await fModificacion.inputValue();
      expect(fechaCreacion1).toMatch(FECHA_RE);
      expect(fechaMod1).toMatch(FECHA_RE);

      // Paso 5: Y cambia el campo «Texto» a «Prueba de humo 3 editada».
      await page.getByRole('textbox', { name: 'Texto' }).fill(textoEditado);

      // Paso 6: Y pulsa «Guardar».
      await page.getByRole('button', { name: 'Guardar' }).click();
      // Tras guardar, Axelor vuelve a la lista; el registro editado aparece en ella.
      await expect(page.getByRole('button', { name: 'Añadir un nuevo smoke test' })).toBeVisible();
      await expect(page.getByText(textoEditado, { exact: true })).toBeVisible();

      // Reabrir el registro editado para comprobar el formulario y los campos de fecha.
      await page.getByText(textoEditado, { exact: true }).click();

      // Resultado esperado 1: el campo «Texto» muestra «Prueba de humo 3 editada».
      await expect(page.getByRole('textbox', { name: 'Texto' })).toHaveValue(textoEditado);

      const fechaCreacion2 = await fCreacion.inputValue();
      const fechaMod2 = await fModificacion.inputValue();
      expect(fechaCreacion2).toMatch(FECHA_RE);
      expect(fechaMod2).toMatch(FECHA_RE);

      // Resultado esperado 2: «Fecha de creación» conserva el mismo valor que en el alta.
      expect(fechaCreacion2).toBe(fechaCreacion1);

      // Resultado esperado 3: «Fecha de última modificación» tiene un valor igual o
      // posterior a FechaMod1 (las fechas van truncadas al minuto, por eso ">=").
      expect(parseFecha(fechaMod2)).toBeGreaterThanOrEqual(parseFecha(fechaMod1));
    } finally {
      try {
        // El estado final del registro es el texto editado; si el test abortó antes de
        // editar, aún puede tener el texto original. Se intentan ambos.
        await borrarSmokeTestSiExiste(page, textoEditado);
        await borrarSmokeTestSiExiste(page, textoOriginal);
      } catch {
        // limpieza best-effort: no debe enmascarar el fallo real de una aserción.
      }
    }

    await logout(page);
  });
});
