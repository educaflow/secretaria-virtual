import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../_support/auth';

// T-002 — Consulta del registro en el listado
// origen: ESC-002  |  verifica: —
// fuente: .sdd/drafts/2026-06-30_11-18_smoke-test-desarrollador/test-e2e-desc/t-002-consulta-del-registro-en-el-listado.desc.md

// Formato de las fechas que muestra Axelor en la columna del listado: "DD/MM/YYYY HH:mm".
// Que la celda case con este patrón prueba que su valor NO está vacío (es una fecha real).
const FECHA_RE = /^\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}$/;

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
  // Diálogo de confirmación ("Do you really want to delete...?"). El botón de
  // confirmar aparece en inglés ("Delete") con el idioma del admin.
  await page.getByRole('button', { name: /Delete|Eliminar/ }).click();
  await expect(page.getByRole('button', { name: 'Añadir un nuevo smoke test' })).toBeVisible();
}

test.describe('Smoke test (Desarrollador)', () => {
  test('Consulta del registro en el listado', async ({ page }) => {
    await ensureLoggedOut(page);
    // Paso 1: Dado que el usuario `admin` inicia sesión con contraseña `admin`.
    await login(page, 'admin', 'admin');

    // Idempotencia (BD compartida, NO se resetea entre ejecuciones): el texto lleva un
    // sufijo único por ejecución para no colisionar con registros de runs previos, y el
    // registro creado se BORRA en el `finally` aunque falle una aserción → test repetible.
    // El escenario usa "Prueba de humo 2"; el sufijo no afecta a lo que verifica el test.
    const texto = `Prueba de humo 2 t002-${Date.now()}`;

    try {
      // Paso 2: Cuando abre el menú «Desarrollador» → «Smoke test».
      await abrirSmokeTest(page);

      // Paso 3: Y pulsa «Añadir un nuevo smoke test», escribe «Prueba de humo 2» en
      //         «Texto» y pulsa «Guardar».
      await page.getByRole('button', { name: 'Añadir un nuevo smoke test' }).click();
      await expect(page.getByRole('textbox', { name: 'Texto' })).toBeVisible();
      await page.getByRole('textbox', { name: 'Texto' }).fill(texto);
      await page.getByRole('button', { name: 'Guardar' }).click();

      // Paso 4: Y pulsa «Cancelar» para volver al listado.
      // En esta versión de la app, «Guardar» ya devuelve por sí solo al listado (la vista
      // pasa de /edit a /list), que es exactamente el efecto que persigue el paso 4: volver
      // al listado para consultarlo. No hay, por tanto, ningún formulario abierto sobre el
      // que pulsar «Cancelar». Esperar de forma determinista a que aparezca el botón del
      // listado evita la condición de carrera de intentar pulsar un «Cancelar» que
      // desaparece durante la navegación (dejaría un formulario abierto y rompería la
      // limpieza y el logout posteriores).
      await expect(page.getByRole('button', { name: 'Añadir un nuevo smoke test' })).toBeVisible();

      // Resultado esperado 1: el listado muestra una fila con el texto «Prueba de humo 2».
      // La fila del grid concatena Texto + Fecha de creación + Fecha de última modificación;
      // la filtramos por su texto único.
      const fila = page.getByRole('row').filter({ hasText: texto });
      await expect(fila).toBeVisible();

      // Resultado esperado 2: la columna «Fecha de creación» de esa fila tiene un valor no
      // vacío. Las celdas del grid son, por orden: [0] Texto, [1] Fecha de creación,
      // [2] Fecha de última modificación. Que la celda case con el formato de fecha prueba
      // que no está vacía.
      const celdaFechaCreacion = fila.getByRole('gridcell').nth(1);
      await expect(celdaFechaCreacion).toHaveText(FECHA_RE);
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
