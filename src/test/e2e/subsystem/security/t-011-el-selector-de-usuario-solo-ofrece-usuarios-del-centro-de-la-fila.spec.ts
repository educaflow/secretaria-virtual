import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-011 — El selector de usuario solo ofrece usuarios del centro de la fila
// origen: ESC-008  |  verifica: U-perfiles-tramites-mi-centro-003
// fuente: .sdd/drafts/2026-09-25_20-58_mantenimiento-perfiles-centro/test-e2e-desc/t-011-el-selector-de-usuario-solo-ofrece-usuarios-del-centro-de-la-fila.desc.md

// Idempotencia: el escenario NO guarda nada (solo abre un alta y despliega el
// selector), así que no crea datos con nombre y no necesita sufijo `Date.now()`
// ni teardown de registros. El `finally` solo descarta el formulario sin guardar
// recargando el listado.
//
// CRITICAL — NO hay pre-limpieza defensiva a propósito: el usuario NO autoriza
// borrar datos preexistentes de la BD, y este test no depende de ellos.
const CENTRO = 'CIPFP Mislata';
const USUARIO_DEL_CENTRO = 'Profesor1 CIPFP Mislata';
const USUARIO_DE_OTRO_CENTRO = 'Profesor1 CIPFP Batoi';
const BUSQUEDA = 'Profesor1';

const URL_LISTADO = '/#/ds/subsysSecurity.Centro%40AceProfileCentro-action/list/1';

const filaSinRegistros = (page: Page) => page.getByRole('row', { name: 'No se encontraron registros.' });
const cabeceraListado = (page: Page) => page.getByRole('columnheader', { name: 'Trámite' });
const botonNuevo = (page: Page) => page.getByRole('button', { name: 'Nuevo' });
const botonAceptarAviso = (page: Page) => page.getByRole('dialog').getByRole('button', { name: /^(OK|Aceptar)$/ });

const campo = (page: Page, nombre: string) => page.getByRole('combobox', { name: nombre, exact: true });
const opcion = (page: Page, nombre: string) =>
  page.getByRole('listbox').getByRole('option', { name: nombre, exact: true });

// Barrera de carga del listado: la rejilla pide sus filas en una petición aparte.
async function esperarListadoCargado(page: Page): Promise<void> {
  await expect(botonNuevo(page)).toBeVisible();
  await expect(cabeceraListado(page)).toBeVisible();
  const filaDeDatos = page.getByRole('row', { name: CENTRO });
  await expect(filaDeDatos.or(filaSinRegistros(page)).first()).toBeVisible();
}

// Abre «Mi centro → Perfiles de trámites» desde el menú.
async function abrirPerfilesDeTramites(page: Page): Promise<void> {
  await page.getByText('Mi centro', { exact: true }).click();
  await page.getByText('Perfiles de trámites', { exact: true }).click();
  await esperarListadoCargado(page);
}

test.describe('Perfiles de trámites (Mi centro)', () => {
  test('El selector de usuario solo ofrece usuarios del centro de la fila', async ({ page }) => {
    await ensureLoggedOut(page);
    // Paso 1: Dado que el supervisor inicia sesión con usuario
    //         «supervisor1@mislata.es» y contraseña «demo1234».
    await login(page, 'supervisor1@mislata.es', 'demo1234');

    try {
      // Paso 2: Cuando abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
      await abrirPerfilesDeTramites(page);
      await botonNuevo(page).click();
      await expect(campo(page, 'Trámite')).toBeVisible();

      // Paso 3: Y elige el centro «CIPFP Mislata».
      // El centro se prellena de forma asíncrona con el del supervisor; se le da
      // margen y solo se elige a mano si no llega a prellenarse.
      const centro = campo(page, 'Centro');
      const prellenado = await expect(centro)
        .toHaveValue(CENTRO, { timeout: 5000 })
        .then(() => true)
        .catch(() => false);
      if (!prellenado) {
        await centro.fill('');
        await centro.pressSequentially('Mislata', { delay: 50 });
        await opcion(page, CENTRO).click();
      }
      await expect(centro).toHaveValue(CENTRO);

      // Paso 4: Y despliega el selector de usuario y escribe «Profesor1» en la búsqueda.
      // Se teclea carácter a carácter: con `fill` el many-to-one abre la lista sin filtrar.
      const usuario = campo(page, 'Usuario');
      await usuario.fill('');
      await usuario.pressSequentially(BUSQUEDA, { delay: 50 });

      // Resultado esperado: el selector ofrece «Profesor1 CIPFP Mislata» …
      await expect(opcion(page, USUARIO_DEL_CENTRO)).toBeVisible();
      // … y no ofrece «Profesor1 CIPFP Batoi» (la lista ya está cargada: la
      // opción anterior es visible, así que la ausencia no es por carga pendiente).
      await expect(opcion(page, USUARIO_DE_OTRO_CENTRO)).toHaveCount(0);
      const opciones = await page.getByRole('listbox').getByRole('option').allInnerTexts();
      expect(opciones.map((o) => o.trim())).not.toContain(USUARIO_DE_OTRO_CENTRO);
    } finally {
      // Descarta el alta sin guardar (no hay nada que borrar). Best-effort: el
      // `try/catch` es intencional para no enmascarar un fallo real del test.
      try {
        await page.keyboard.press('Escape');
        await page.goto(URL_LISTADO);
        await page.reload();
        const aviso = botonAceptarAviso(page);
        if (await aviso.isVisible().catch(() => false)) {
          await aviso.click();
        }
        await esperarListadoCargado(page);
      } catch {
        // limpieza best-effort.
      }
    }

    await logout(page);
  });
});
