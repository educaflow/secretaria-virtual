import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-022 — No se guarda una fila cuyo usuario no pertenece al centro
// origen: ESC-015  |  verifica: V-AceProfileCentro-006
// fuente: .sdd/drafts/2026-09-25_20-58_mantenimiento-perfiles-centro/test-e2e-desc/t-022-no-se-guarda-una-fila-cuyo-usuario-no-pertenece-al-centro.desc.md

// Valores de la fila del escenario. NO llevan sufijo `Date.now()` (excepción
// documentada de la regla de nombres únicos): son valores de catálogo (centros,
// trámite, perfil del enum y usuario de demo), no texto libre.
// Idempotencia: el escenario NO debe crear nada. Si, por un fallo de la app, la
// fila llegara a guardarse, el teardown del `finally` borra SOLO el registro que
// ESTA ejecución ha guardado (el formulario abierto pasa a tener id en la URL).
//
// CRITICAL — NO hay pre-limpieza defensiva a propósito: el usuario NO autoriza
// borrar datos preexistentes de la BD.
const CENTRO_INICIAL = 'CIPFP Mislata';
const CENTRO_FINAL = 'CIPFP Batoi';
const TRAMITE = 'Trámite de prueba';
const PERFIL = 'Colaborador';
const USUARIO = 'Profesor1 CIPFP Mislata';
const MENSAJE = 'El usuario no pertenece al centro';

const URL_LISTADO_ADMIN = '/#/ds/subsysSecurity.Main%40AceProfileCentro-action/list/1';
// Formulario de un registro YA guardado: `/edit/<id>`. Un alta sin guardar queda en `/edit`.
const URL_REGISTRO_GUARDADO = /AceProfileCentro-action\/edit\/\d+/;

// Filas del listado con este trámite y perfil (cualquier centro y destinatario): el
// nombre accesible de la fila es «<centro> <trámite> <perfil> <tipo usuario> <cargo> <usuario>».
const filasTramitePerfil = (page: Page) => page.getByRole('row', { name: `${TRAMITE} ${PERFIL}` });
const filaSinRegistros = (page: Page) => page.getByRole('row', { name: 'No se encontraron registros.' });
const cabeceraListado = (page: Page) => page.getByRole('columnheader', { name: 'Trámite' });

const botonNuevo = (page: Page) => page.getByRole('button', { name: 'Nuevo' });
const botonBorrar = (page: Page) => page.getByRole('button', { name: 'Borrar' });
const botonAceptarAviso = (page: Page) => page.getByRole('dialog').getByRole('button', { name: /^(OK|Aceptar)$/ });
const dialogoValidationError = (page: Page) =>
  page.getByRole('dialog').getByRole('heading', { name: 'La acción no se completó por los siguientes motivos' });

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

// Elige `valor` en un many-to-one tecleando `texto` carácter a carácter (con
// `fill` el many-to-one abre la lista sin filtrar) y pulsando la opción.
async function elegirTecleando(page: Page, nombreCampo: string, valor: string, texto: string): Promise<void> {
  const combo = campo(page, nombreCampo);
  await combo.fill('');
  await combo.pressSequentially(texto, { delay: 50 });
  await opcion(page, valor).click();
  await expect(combo).toHaveValue(valor);
}

// Recarga en duro el listado para leer el estado REAL de la BD, no la rejilla en
// memoria de la SPA (descarta además el formulario sin guardar).
async function recargarListado(page: Page): Promise<void> {
  await page.goto(URL_LISTADO_ADMIN);
  await page.reload();
  const aviso = botonAceptarAviso(page);
  if (await aviso.isVisible().catch(() => false)) {
    await aviso.click();
  }
  await esperarListadoCargado(page);
}

test.describe('Perfiles de trámites por centro (Administración)', () => {
  test('No se guarda una fila cuyo usuario no pertenece al centro', async ({ page }) => {
    await ensureLoggedOut(page);
    // Paso 1: Dado que el administrador inicia sesión con usuario «admin» y
    //         contraseña «admin».
    await login(page, 'admin', 'admin');

    try {
      // Paso 2: Cuando abre «Administración → Perfiles de trámites por centro» …
      await abrirPerfilesPorCentro(page);
      // Recuento previo (sin borrar nada) para comprobar después que no se guarda ninguna fila.
      const filasAntes = await filasTramitePerfil(page).count();

      // … y pulsa «Nuevo».
      await botonNuevo(page).click();
      await expect(campo(page, 'Trámite')).toBeVisible();

      // Paso 3: Y elige el centro «CIPFP Mislata», el trámite «Trámite de prueba»,
      //         el perfil «Colaborador» y el usuario «Profesor1 CIPFP Mislata».
      await elegirTecleando(page, 'Centro', CENTRO_INICIAL, 'Mislata');
      await elegirTecleando(page, 'Trámite', TRAMITE, 'prueba');
      await campo(page, 'Perfil').click();
      await opcion(page, PERFIL).click();
      await expect(campo(page, 'Perfil')).toHaveValue(PERFIL);
      await elegirTecleando(page, 'Usuario', USUARIO, 'Profesor1');

      // Paso 4: Y cambia el centro a «CIPFP Batoi» sin cambiar el usuario.
      await elegirTecleando(page, 'Centro', CENTRO_FINAL, 'Batoi');
      await expect(campo(page, 'Usuario')).toHaveValue(USUARIO);

      // Paso 5: Y pulsa «Guardar».
      await page.getByRole('button', { name: 'Guardar' }).click();

      // Resultado esperado (1/2): el sistema muestra el mensaje «El usuario no
      // pertenece al centro» …
      await expect(dialogoValidationError(page)).toBeVisible();
      await expect(page.getByRole('dialog').getByRole('listitem').filter({ hasText: MENSAJE })).toBeVisible();

      // Resultado esperado (2/2): … y no guarda la fila. El formulario sigue siendo
      // un alta sin id y, tras recargar el listado desde la BD, no hay filas nuevas.
      await expect(page).not.toHaveURL(URL_REGISTRO_GUARDADO);
      await botonAceptarAviso(page).click();
      await expect(page).not.toHaveURL(URL_REGISTRO_GUARDADO);
      await recargarListado(page);
      await expect(filasTramitePerfil(page)).toHaveCount(filasAntes);
    } finally {
      // Teardown: solo si, por un fallo, ESTA ejecución guardó el registro (el
      // formulario abierto tiene id), se borra ese registro. Best-effort (el
      // `try/catch` es intencional) para no enmascarar el fallo real del test.
      try {
        if (URL_REGISTRO_GUARDADO.test(page.url())) {
          await botonBorrar(page).click();
          const dialogo = page.getByRole('dialog');
          await dialogo.getByRole('button', { name: /^(Eliminar|Delete)$/ }).click();
          await expect(page).toHaveURL(/AceProfileCentro-action\/list/);
        }
      } catch {
        // limpieza best-effort.
      }
    }

    await logout(page);
  });
});
