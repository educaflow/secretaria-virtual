import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../_support/auth';

// T-004 — Borrado de un registro
// origen: ESC-004  |  verifica: —
// fuente: .sdd/drafts/2026-06-30_11-18_smoke-test-desarrollador/test-e2e-desc/t-004-borrado-de-un-registro.desc.md

// Abre el menú «Desarrollador» → «Smoke test» y espera la lista.
async function abrirSmokeTest(page: Page): Promise<void> {
  await page.getByText('Desarrollador', { exact: true }).click();
  await page.getByText('Smoke test', { exact: true }).click();
  await expect(page.getByRole('button', { name: 'Añadir un nuevo smoke test' })).toBeVisible();
}

// Teardown idempotente: borra el registro con ese `texto` si sigue en la lista.
// En el camino feliz de este test el registro ya está borrado (el borrado ES la
// acción del test), así que esto solo actúa si el test abortó antes de borrarlo.
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
  test('Borrado de un registro', async ({ page }) => {
    await ensureLoggedOut(page);
    // Paso 1: Dado que el usuario `admin` inicia sesión con contraseña `admin`.
    await login(page, 'admin', 'admin');

    // Idempotencia (BD compartida, NO se resetea entre ejecuciones): el texto lleva un
    // sufijo único por ejecución para no colisionar con registros de runs previos, y el
    // registro se BORRA en el `finally` si el test abortó antes del borrado → test repetible.
    // El escenario usa "Prueba de humo 4"; el sufijo no afecta a lo que verifica el test.
    const texto = `Prueba de humo 4 t004-${Date.now()}`;

    try {
      // Paso 2: Cuando abre el menú «Desarrollador» → «Smoke test».
      await abrirSmokeTest(page);

      // Paso 3: Y pulsa «Añadir un nuevo smoke test», escribe «Prueba de humo 4» en
      //         «Texto» y pulsa «Guardar».
      await page.getByRole('button', { name: 'Añadir un nuevo smoke test' }).click();
      await expect(page.getByRole('textbox', { name: 'Texto' })).toBeVisible();
      await page.getByRole('textbox', { name: 'Texto' }).fill(texto);
      await page.getByRole('button', { name: 'Guardar' }).click();

      // Paso 4: Y pulsa «Cancelar» para volver al listado.
      // En esta versión de la app, «Guardar» ya devuelve por sí solo al listado (la vista
      // pasa de /edit a /list), que es exactamente el efecto que persigue el paso 4: volver
      // al listado. No queda, por tanto, ningún formulario abierto sobre el que pulsar
      // «Cancelar». Esperar de forma determinista a que aparezca el botón del listado evita
      // la condición de carrera de intentar pulsar un «Cancelar» que desaparece durante la
      // navegación.
      await expect(page.getByRole('button', { name: 'Añadir un nuevo smoke test' })).toBeVisible();
      await expect(page.getByText(texto, { exact: true })).toBeVisible();

      // Paso 5: Y pulsa sobre la fila del registro «Prueba de humo 4» para abrir su formulario.
      await page.getByText(texto, { exact: true }).click();
      await expect(page.getByRole('textbox', { name: 'Texto' })).toHaveValue(texto);

      // Paso 6: Y pulsa el botón «Borrar» y confirma el borrado.
      await expect(page.getByRole('button', { name: 'Borrar' })).toBeVisible();
      await page.getByRole('button', { name: 'Borrar' }).click();
      // Diálogo de confirmación; el botón de confirmar aparece en inglés ("Delete")
      // con el idioma del admin.
      await page.getByRole('button', { name: /Delete|Eliminar/ }).click();

      // Resultado esperado: el sistema elimina el registro y muestra el listado sin la
      // fila «Prueba de humo 4».
      // 1) Vuelve al listado (aparece el botón de alta de la vista de lista).
      await expect(page.getByRole('button', { name: 'Añadir un nuevo smoke test' })).toBeVisible();
      // 2) La fila con el texto del registro borrado ya NO está en el listado.
      await expect(page.getByText(texto, { exact: true })).toHaveCount(0);
    } finally {
      try {
        // El camino feliz ya borró el registro; esto solo limpia si el test abortó antes.
        await borrarSmokeTestSiExiste(page, texto);
      } catch {
        // limpieza best-effort: no debe enmascarar el fallo real de una aserción.
      }
    }

    await logout(page);
  });
});
