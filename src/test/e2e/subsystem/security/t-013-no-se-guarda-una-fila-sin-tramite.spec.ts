import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-013 — No se guarda una fila sin trámite
// origen: ESC-016  |  verifica: V-AceProfileCentro-002, U-perfiles-tramites-mi-centro-004
// fuente: .sdd/drafts/2026-09-25_20-58_mantenimiento-perfiles-centro/test-e2e-desc/t-013-no-se-guarda-una-fila-sin-tramite.desc.md

// Valores de la fila del escenario. NO llevan sufijo `Date.now()` (excepción
// documentada de la regla de nombres únicos): son valores de catálogo (centro,
// perfil del enum y cargo), no texto libre.
// Idempotencia: el escenario NO debe crear nada. Si, por un fallo de la app, la
// fila llegara a guardarse, el teardown del `finally` borra SOLO el registro que
// ESTA ejecución ha guardado (el formulario abierto pasa a tener id en la URL).
//
// CRITICAL — NO hay pre-limpieza defensiva a propósito: el usuario NO autoriza
// borrar datos preexistentes de la BD.
const CENTRO = 'CIPFP Mislata';
const PERFIL = 'Tramitador';
const CARGO = 'Jefe de estudios';
const MENSAJE = 'El trámite es obligatorio';

const URL_LISTADO = '/#/ds/subsysSecurity.Centro%40AceProfileCentro-action/list/1';
// Formulario de un registro YA guardado: `/edit/<id>`. Un alta sin guardar queda en `/edit`.
const URL_REGISTRO_GUARDADO = /AceProfileCentro-action\/edit\/\d+/;

// Filas del listado de este centro SIN trámite, con este perfil y cargo: el nombre
// accesible de la fila es «<centro> <trámite> <perfil> <tipo usuario> <cargo> <usuario>»,
// así que sin trámite el perfil va justo detrás del centro.
const filasSinTramite = (page: Page) =>
  page.getByRole('row', { name: new RegExp(`^${CENTRO}\\s+${PERFIL}\\s+${CARGO}`) });
const filaSinRegistros = (page: Page) => page.getByRole('row', { name: 'No se encontraron registros.' });
const cabeceraListado = (page: Page) => page.getByRole('columnheader', { name: 'Trámite' });

const botonNuevo = (page: Page) => page.getByRole('button', { name: 'Nuevo' });
const botonBorrar = (page: Page) => page.getByRole('button', { name: 'Borrar' });
const botonAceptarAviso = (page: Page) => page.getByRole('dialog').getByRole('button', { name: /^(OK|Aceptar)$/ });
const dialogoValidationError = (page: Page) =>
  page.getByRole('dialog').getByRole('heading', { name: 'La acción no se completó por los siguientes motivos' });

const campo = (page: Page, nombre: string) => page.getByRole('combobox', { name: nombre, exact: true });

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

// Elige `opcion` en un combobox de Axelor: lo abre (o escribe `texto` para
// filtrar, en los many-to-one) y pulsa la opción de la lista desplegada.
async function elegir(page: Page, nombreCampo: string, opcion: string, texto?: string): Promise<void> {
  const combo = campo(page, nombreCampo);
  if (texto !== undefined) {
    await combo.fill(texto);
  } else {
    await combo.click();
  }
  await page.getByRole('listbox').getByRole('option', { name: opcion, exact: true }).click();
  await expect(combo).toHaveValue(opcion);
}

// Recarga en duro el listado para leer el estado REAL de la BD, no la rejilla en
// memoria de la SPA (descarta además el formulario sin guardar).
async function recargarListado(page: Page): Promise<void> {
  await page.goto(URL_LISTADO);
  await page.reload();
  await esperarListadoCargado(page);
}

test.describe('Perfiles de trámites (Mi centro)', () => {
  test('No se guarda una fila sin trámite', async ({ page }) => {
    await ensureLoggedOut(page);
    // Paso 1: Dado que el supervisor inicia sesión con usuario
    //         «supervisor1@mislata.es» y contraseña «demo1234».
    await login(page, 'supervisor1@mislata.es', 'demo1234');

    try {
      // Paso 2: Cuando abre «Mi centro → Perfiles de trámites» …
      await abrirPerfilesDeTramites(page);
      // Recuento previo (sin borrar nada) para comprobar después que no se guarda ninguna fila.
      const filasAntes = await filasSinTramite(page).count();

      // … y pulsa «Nuevo».
      await botonNuevo(page).click();
      await expect(campo(page, 'Trámite')).toBeVisible();

      // Paso 3: Y elige el centro «CIPFP Mislata», el perfil «Tramitador» y el
      //         cargo «Jefe de estudios», sin elegir trámite.
      // El centro sale prellenado (de forma asíncrona) con el del supervisor;
      // solo se elige a mano si no llega a prellenarse.
      const centro = campo(page, 'Centro');
      const prellenado = await expect(centro)
        .toHaveValue(CENTRO, { timeout: 3000 })
        .then(() => true)
        .catch(() => false);
      if (!prellenado) {
        await elegir(page, 'Centro', CENTRO, 'Mislata');
      }
      await expect(centro).toHaveValue(CENTRO);
      // El perfil es un grupo de radios (widget RadioSelect), no un combobox.
      await page.getByRole('radio', { name: PERFIL, exact: true }).check();
      await elegir(page, 'Cargo', CARGO, 'Jefe');
      await expect(campo(page, 'Trámite')).toHaveValue('');

      // Paso 4: Y pulsa «Guardar».
      await page.getByRole('button', { name: 'Guardar' }).click();

      // Resultado esperado (1/2): el sistema muestra el mensaje «El trámite es obligatorio» …
      await expect(dialogoValidationError(page)).toBeVisible();
      await expect(page.getByRole('dialog').getByRole('listitem').filter({ hasText: MENSAJE })).toBeVisible();

      // Resultado esperado (2/2): … y no guarda la fila. El formulario sigue siendo
      // un alta sin id y, tras recargar el listado desde la BD, no hay filas nuevas.
      await expect(page).not.toHaveURL(URL_REGISTRO_GUARDADO);
      await botonAceptarAviso(page).click();
      await expect(page).not.toHaveURL(URL_REGISTRO_GUARDADO);
      await recargarListado(page);
      await expect(filasSinTramite(page)).toHaveCount(filasAntes);
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
