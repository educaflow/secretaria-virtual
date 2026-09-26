import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-020 — El selector de usuario del administrador solo ofrece usuarios del centro de la fila
// origen: ESC-013  |  verifica: U-perfiles-tramites-todos-centros-002
// fuente: .sdd/drafts/2026-09-25_20-58_mantenimiento-perfiles-centro/test-e2e-desc/t-020-el-selector-de-usuario-del-administrador-solo-ofrece-usuarios-del-centro-de-la-fila.desc.md

// Idempotencia: el escenario NO guarda nada (solo abre un alta y despliega el
// selector), así que no crea datos con nombre y no necesita sufijo `Date.now()`
// ni teardown de registros. El `finally` solo descarta el formulario sin guardar
// recargando el listado.
//
// CRITICAL — NO hay pre-limpieza defensiva a propósito: el usuario NO autoriza
// borrar datos preexistentes de la BD, y este test no depende de ellos.
const CENTRO = 'CIPFP Batoi';
const USUARIO_DEL_CENTRO = 'Profesor1 CIPFP Batoi';
const USUARIO_DE_OTRO_CENTRO = 'Profesor1 CIPFP Mislata';
const BUSQUEDA = 'Profesor1';

const URL_LISTADO_ADMIN = '/#/ds/subsysSecurity.Main%40AceProfileCentro-action/list/1';

const filaSinRegistros = (page: Page) => page.getByRole('row', { name: 'No se encontraron registros.' });
const cabeceraListado = (page: Page) => page.getByRole('columnheader', { name: 'Trámite' });
const botonNuevo = (page: Page) => page.getByRole('button', { name: 'Nuevo' });
const botonAceptarAviso = (page: Page) => page.getByRole('dialog').getByRole('button', { name: /^(OK|Aceptar)$/ });

const campo = (page: Page, nombre: string) => page.getByRole('combobox', { name: nombre, exact: true });
const opcion = (page: Page, nombre: string) =>
  page.getByRole('listbox').getByRole('option', { name: nombre, exact: true });

// Barrera de carga del listado: la rejilla pide sus filas en una petición aparte,
// así que se espera a una fila de datos (de cualquier centro «CIPFP …») o a la de
// «sin registros».
async function esperarListadoCargado(page: Page): Promise<void> {
  await expect(botonNuevo(page)).toBeVisible();
  await expect(cabeceraListado(page)).toBeVisible();
  const filaDeDatos = page.getByRole('row', { name: /CIPFP/ });
  await expect(filaDeDatos.or(filaSinRegistros(page)).first()).toBeVisible();
}

// Abre «Administración → Perfiles de trámites por centro» desde el menú.
async function abrirPerfilesPorCentro(page: Page): Promise<void> {
  await page.getByText('Administración', { exact: true }).click();
  await page.getByText('Perfiles de trámites por centro', { exact: true }).click();
  await esperarListadoCargado(page);
}

test.describe('Perfiles de trámites por centro (Administración)', () => {
  test('El selector de usuario del administrador solo ofrece usuarios del centro de la fila', async ({ page }) => {
    await ensureLoggedOut(page);
    // Paso 1: Dado que el administrador inicia sesión con usuario «admin» y
    //         contraseña «admin».
    await login(page, 'admin', 'admin');

    try {
      // Paso 2: Cuando abre «Administración → Perfiles de trámites por centro» y pulsa «Nuevo».
      await abrirPerfilesPorCentro(page);
      await botonNuevo(page).click();
      await expect(campo(page, 'Trámite')).toBeVisible();

      // Paso 3: Y elige el centro «CIPFP Batoi».
      // Se teclea carácter a carácter: con `fill` el many-to-one abre la lista sin filtrar.
      const centro = campo(page, 'Centro');
      await centro.fill('');
      await centro.pressSequentially('Batoi', { delay: 50 });
      await opcion(page, CENTRO).click();
      await expect(centro).toHaveValue(CENTRO);

      // Paso 4: Y despliega el selector de usuario buscando «Profesor1».
      const usuario = campo(page, 'Usuario');
      await usuario.fill('');
      await usuario.pressSequentially(BUSQUEDA, { delay: 50 });

      // Resultado esperado: el selector ofrece «Profesor1 CIPFP Batoi» …
      await expect(opcion(page, USUARIO_DEL_CENTRO)).toBeVisible();
      // … y no ofrece «Profesor1 CIPFP Mislata» (la lista ya está cargada: la
      // opción anterior es visible, así que la ausencia no es por carga pendiente).
      await expect(opcion(page, USUARIO_DE_OTRO_CENTRO)).toHaveCount(0);
      const opciones = await page.getByRole('listbox').getByRole('option').allInnerTexts();
      expect(opciones.map((o) => o.trim())).not.toContain(USUARIO_DE_OTRO_CENTRO);
    } finally {
      // Descarta el alta sin guardar (no hay nada que borrar). Best-effort: el
      // `try/catch` es intencional para no enmascarar un fallo real del test.
      try {
        await page.keyboard.press('Escape');
        await page.goto(URL_LISTADO_ADMIN);
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
