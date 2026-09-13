import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../_support/auth';

// T-005 — Alta sin texto rechazada con mensaje de error
// origen: ESC-005  |  verifica: V-SmokeTest-001
// fuente: .sdd/drafts/2026-06-30_11-18_smoke-test-desarrollador/test-e2e-desc/t-005-alta-sin-texto-rechazada-con-mensaje-de-error.desc.md

// Abre el menú «Desarrollador» → «Smoke test» y espera la lista.
async function abrirSmokeTest(page: Page): Promise<void> {
  // Por data-testid y no por texto: el ítem se rotula literalmente
  // «Smoke__!! test__!!» porque la traducción del CSV de menús no está aplicada en
  // runtime, así que un locator por texto es frágil ante ese detalle de i18n.
  await page.getByTestId('item:desarrollador-menuitem').click();
  await page.getByTestId('item:desarrollador-smoketest-menuitem').click();
  await expect(page.getByRole('button', { name: 'Añadir un nuevo smoke test' })).toBeVisible();
}

// Espera a que la grid haya TERMINADO de cargar antes de leer el paginador.
//
// CRITICAL — evitar la carrera de carga: al entrar en la lista, Axelor pinta un
// paginador transitorio "0 a 0 de 0" ANTES de que llegue la query de datos, así que
// leer de inmediato daría un total falso. La grid ha terminado de cargar cuando aparece
// una fila con una fecha DD/MM/YYYY (hay datos) O el mensaje "No se encontraron
// registros." (lista vacía).
// MUST esperar a cualquiera de los dos: la tabla `smoke_test` PUEDE estar vacía (BD
// compartida que se resetea, o cuando el resto de tests han limpiado sus registros), y
// esperar solo la fila-con-fecha colgaría el test 20s en ese caso.
async function esperarGridCargada(page: Page): Promise<void> {
  const filaConFecha = page.getByRole('row').filter({ hasText: /\d{2}\/\d{2}\/\d{4}/ }).first();
  const sinRegistros = page.getByText(/No se encontraron registros/i);
  await expect(filaConFecha.or(sinRegistros).first()).toBeVisible();
}

// Nº total de registros según el paginador de la lista ("1 a N de N", o "0 a 0 de 0"
// si está vacía; sale traducido al español, que es el locale de la aplicación).
// Sirve para comprobar que un alta rechazada NO incrementa el total.
async function contarRegistros(page: Page): Promise<number> {
  await esperarGridCargada(page);
  const paginador = page.getByText(/\d+\s+a\s+\d+\s+de\s+\d+/);
  const txt = await paginador.first().innerText();
  const m = txt.match(/de\s+(\d+)/);
  if (!m) throw new Error(`Paginador no parseable: "${txt}"`);
  return Number(m[1]);
}

test.describe('Smoke test (Desarrollador)', () => {
  test('Alta sin texto rechazada con mensaje de error', async ({ page }) => {
    // Idempotencia (BD compartida, NO se resetea entre ejecuciones): este test NO crea
    // ningún registro (el alta se rechaza por validación), así que no necesita nombres
    // únicos ni teardown. Es repetible por construcción: verifica que el total de la
    // lista NO cambia tras intentar el alta vacía.

    await ensureLoggedOut(page);
    // Paso 1: Dado que el usuario `admin` inicia sesión con contraseña `admin`.
    await login(page, 'admin', 'admin');

    // Paso 2: Cuando abre el menú «Desarrollador» → «Smoke test».
    await abrirSmokeTest(page);

    // Total de registros ANTES del intento de alta (para comprobar después que no crece).
    const totalAntes = await contarRegistros(page);

    // Paso 3: Y pulsa «Añadir un nuevo smoke test».
    await page.getByRole('button', { name: 'Añadir un nuevo smoke test' }).click();
    await expect(page.getByRole('textbox', { name: 'Texto' })).toBeVisible();

    // Paso 4: Y deja el campo «Texto» vacío.
    await expect(page.getByRole('textbox', { name: 'Texto' })).toHaveValue('');

    // Paso 5: Y pulsa el botón «Guardar».
    await page.getByRole('button', { name: 'Guardar' }).click();

    // Resultado esperado 1: el sistema muestra el mensaje «El texto es obligatorio».
    // Axelor lo presenta en un diálogo de error de validación; el guardado queda bloqueado
    // (la vista permanece en el formulario de edición, no vuelve a la lista).
    await expect(page.getByText('El texto es obligatorio')).toBeVisible();
    await expect(page).toHaveURL(/\/edit$/);

    // Cerrar el diálogo (Aceptar) y salir del formulario (Cancelar) para volver al listado.
    await page.getByRole('button', { name: 'Aceptar' }).click();
    await page.getByRole('button', { name: 'Cancelar' }).click();
    await expect(page.getByRole('button', { name: 'Añadir un nuevo smoke test' })).toBeVisible();

    // Resultado esperado 2: no se crea ningún registro nuevo en la base de datos.
    // El total de la lista es el mismo que antes del intento de alta.
    const totalDespues = await contarRegistros(page);
    expect(totalDespues).toBe(totalAntes);

    await logout(page);
  });
});
