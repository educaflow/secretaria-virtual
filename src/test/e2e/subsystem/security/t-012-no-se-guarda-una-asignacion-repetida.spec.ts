import { test, expect, Locator, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-012 — No se guarda una asignación repetida
// origen: ESC-009  |  verifica: V-AceProfileCentro-007
// fuente: .sdd/drafts/2026-09-25_20-58_mantenimiento-perfiles-centro/test-e2e-desc/t-012-no-se-guarda-una-asignacion-repetida.desc.md

// Valores de la fila del escenario. NO llevan sufijo `Date.now()` (excepción
// documentada de la regla de nombres únicos): son valores de catálogo (centro,
// trámite, perfil del enum y tipo de usuario), no texto libre, y el escenario
// necesita precisamente repetirlos.
// La idempotencia se consigue con el teardown del `finally`, que borra la fila
// que ESTA ejecución ha creado (haya fallado o no una aserción) y, si por un
// fallo de la app la segunda alta llegara a guardarse, también esa.
//
// CRITICAL — NO hay pre-limpieza defensiva a propósito: el usuario NO autoriza
// borrar datos preexistentes de la BD. Si al arrancar ya hubiera una fila con
// estos valores (residual de otro run o dato ajeno), el test FALLA en la guarda
// de precondición en vez de borrarla.
const CENTRO = 'CIPFP Mislata';
const TRAMITE = 'Trámite de prueba';
const PERFIL = 'Creador';
const TIPO_USUARIO = 'Profesor';
const MENSAJE = 'Ya existe esa asignación de perfil';

const URL_LISTADO = '/#/ds/subsysSecurity.Centro%40AceProfileCentro-action/list/1';
// Formulario de un registro YA guardado: `/edit/<id>`. Un alta sin guardar queda en `/edit`.
const URL_REGISTRO_GUARDADO = /AceProfileCentro-action\/edit\/\d+/;

// El nombre accesible de una fila del listado es «<centro> <trámite> <perfil>
// <tipo usuario> <cargo> <usuario>» (las celdas vacías no aportan texto).
const filasDelEscenario = (page: Page) =>
  page.getByRole('row', { name: `${TRAMITE} ${PERFIL} ${TIPO_USUARIO}` });
const filaSinRegistros = (page: Page) => page.getByRole('row', { name: 'No se encontraron registros.' });
const cabeceraListado = (page: Page) => page.getByRole('columnheader', { name: 'Trámite' });

const botonNuevo = (page: Page) => page.getByRole('button', { name: 'Nuevo' });
const botonGuardar = (page: Page) => page.getByRole('button', { name: 'Guardar' });
// «Cancelar» del formulario (no el del diálogo de pregunta, que se llama igual).
const botonCancelar = (page: Page) =>
  page.getByRole('tabpanel').getByRole('button', { name: 'Cancelar', exact: true });
// «Borrar» solo existe en el formulario de un registro ya guardado.
const botonBorrar = (page: Page) => page.getByRole('button', { name: 'Borrar' });
const botonAceptarAviso = (page: Page) => page.getByRole('dialog').getByRole('button', { name: /^(OK|Aceptar)$/ });
const dialogoValidationError = (page: Page) =>
  page.getByRole('dialog').getByRole('heading', { name: 'La acción no se completó por los siguientes motivos' });

const campo = (page: Page, nombre: string) => page.getByRole('combobox', { name: nombre, exact: true });

// Barrera de carga del listado: la rejilla pide sus filas en una petición aparte,
// así que se espera a que haya al menos una fila de datos o la de «sin registros».
async function esperarListadoCargado(page: Page): Promise<void> {
  await expect(botonNuevo(page)).toBeVisible();
  await expect(cabeceraListado(page)).toBeVisible();
  const filaDeDatos = page.getByRole('row', { name: CENTRO });
  await expect(filaDeDatos.or(filaSinRegistros(page)).first()).toBeVisible();
}

// Cierra el aviso «Current changes will be lost» si está abierto ahora mismo.
async function cerrarAviso(page: Page): Promise<void> {
  const ok = botonAceptarAviso(page);
  if (await ok.isVisible().catch(() => false)) {
    await ok.click();
  }
}

// Pulsa `destino` hasta que el clic surte efecto (`resultado` visible), tolerando
// el aviso asíncrono «Current changes will be lost» que Axelor puede lanzar tras
// guardar o al abandonar un formulario, y que intercepta la navegación.
async function pulsarHasta(page: Page, destino: Locator, resultado: Locator, descripcion: string): Promise<void> {
  for (let intento = 0; intento < 5; intento++) {
    await cerrarAviso(page);
    try {
      await destino.click({ timeout: 5000 });
    } catch {
      continue;
    }
    const aceptar = botonAceptarAviso(page);
    try {
      await aceptar.or(resultado).first().waitFor({ timeout: 10000 });
    } catch {
      continue;
    }
    if (await aceptar.isVisible().catch(() => false)) {
      await aceptar.click();
      continue;
    }
    return;
  }
  throw new Error(`No se pudo pulsar ${descripcion}`);
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

// Pasos 3 y 6: rellena el alta con los valores del escenario, sin cargo ni usuario.
// El centro sale prellenado con el del supervisor; solo se elige si no lo está.
async function rellenarAlta(page: Page): Promise<void> {
  const centro = campo(page, 'Centro');
  if ((await centro.inputValue()) !== CENTRO) {
    await elegir(page, 'Centro', CENTRO, 'Mislata');
  }
  await expect(centro).toHaveValue(CENTRO);
  await elegir(page, 'Trámite', TRAMITE, 'prueba');
  // «Perfil» es un grupo de radios (`widget="RadioSelect"`), no un combobox.
  await page.getByRole('radio', { name: PERFIL, exact: true }).check();
  await expect(page.getByRole('radio', { name: PERFIL, exact: true })).toBeChecked();
  // «Tipo usuario» es una selección (lista completa, sin filtrar).
  await elegir(page, 'Tipo usuario', TIPO_USUARIO);
  await expect(campo(page, 'Cargo')).toHaveValue('');
  await expect(campo(page, 'Usuario')).toHaveValue('');
}

// Recarga en duro el listado para leer el estado REAL de la BD, no la rejilla en
// memoria de la SPA.
async function recargarListado(page: Page): Promise<void> {
  await page.goto(URL_LISTADO);
  await page.reload();
  await esperarListadoCargado(page);
}

// Celda de `fila` bajo la columna titulada `columna`: se resuelve por la posición
// de la cabecera, así no depende del orden de columnas de la vista.
async function celda(page: Page, fila: Locator, columna: string): Promise<Locator> {
  const cabeceras = await page.getByRole('columnheader').allInnerTexts();
  // El texto de una cabecera ordenable incluye el icono («Perfil\nsort»): se
  // compara solo su primera línea, que es el título.
  const indice = cabeceras.findIndex((t) => t.split('\n')[0].trim() === columna);
  expect(indice, `No hay columna «${columna}» en el listado (cabeceras: ${cabeceras.join(' | ')})`).toBeGreaterThanOrEqual(0);
  return fila.getByRole('gridcell').nth(indice);
}

// Abre la primera fila del escenario y la borra confirmando el diálogo.
async function abrirYBorrarFila(page: Page): Promise<void> {
  await pulsarHasta(page, filasDelEscenario(page).first(), botonBorrar(page), 'la fila del escenario');
  await botonBorrar(page).click();
  const dialogo = page.getByRole('dialog');
  await expect(dialogo.getByText('¿Realmente quieres eliminar el registro seleccionado?')).toBeVisible();
  await dialogo.getByRole('button', { name: /^(Eliminar|Delete)$/ }).click();
  await expect(page).toHaveURL(/AceProfileCentro-action\/list/);
  await esperarListadoCargado(page);
}

test.describe('Perfiles de trámites (Mi centro)', () => {
  test('No se guarda una asignación repetida', async ({ page }) => {
    // Número de filas del escenario que ESTA ejecución ha guardado: es lo único
    // que el teardown puede borrar (nunca datos preexistentes).
    let creadas = 0;

    await ensureLoggedOut(page);
    // Paso 1: Dado que el supervisor inicia sesión con usuario
    //         «supervisor1@mislata.es» y contraseña «demo1234».
    await login(page, 'supervisor1@mislata.es', 'demo1234');

    try {
      // Paso 2: Cuando abre «Mi centro → Perfiles de trámites» …
      await abrirPerfilesDeTramites(page);

      // Guarda de precondición (sin borrar nada): el estado inicial no tiene
      // ninguna fila con estos valores. Si la hubiera, la primera alta ya
      // chocaría con la regla y el test no probaría lo que dice probar.
      await expect(
        filasDelEscenario(page),
        `Ya existe una fila «${TRAMITE} / ${PERFIL} / ${TIPO_USUARIO}» antes de empezar; no se borra (dato preexistente)`,
      ).toHaveCount(0);

      // … y pulsa «Nuevo».
      await pulsarHasta(page, botonNuevo(page), campo(page, 'Trámite'), 'el botón «Nuevo»');

      // Paso 3: Y elige el centro «CIPFP Mislata», el trámite «Trámite de prueba»,
      //         el perfil «Creador» y el tipo de usuario «Profesor», sin rellenar
      //         cargo ni usuario.
      await rellenarAlta(page);

      // Paso 4: Y pulsa «Guardar».
      await botonGuardar(page).click();
      const error = dialogoValidationError(page);
      await error.or(botonNuevo(page)).first().waitFor();
      await expect(error).toHaveCount(0);
      await expect(page).toHaveURL(/AceProfileCentro-action\/list/);
      creadas = 1;

      // Paso 5: Y vuelve al listado y pulsa «Nuevo».
      // Drena el aviso con retardo que Axelor puede lanzar tras guardar.
      await botonAceptarAviso(page).waitFor({ timeout: 5000 }).catch(() => {});
      await cerrarAviso(page);
      await esperarListadoCargado(page);
      await pulsarHasta(page, botonNuevo(page), campo(page, 'Trámite'), 'el botón «Nuevo»');

      // Paso 6: Y elige exactamente los mismos valores, sin cargo ni usuario.
      await rellenarAlta(page);

      // Paso 7: Y pulsa «Guardar».
      await botonGuardar(page).click();

      // Paso 8: Entonces el sistema muestra el mensaje «Ya existe esa asignación
      //         de perfil» …
      await expect(dialogoValidationError(page)).toBeVisible();
      await expect(page.getByRole('dialog').getByText(MENSAJE, { exact: true })).toBeVisible();
      // … y no guarda la segunda fila: el formulario sigue siendo un alta sin id.
      await expect(page).not.toHaveURL(URL_REGISTRO_GUARDADO);
      await botonAceptarAviso(page).click();
      await expect(page).not.toHaveURL(URL_REGISTRO_GUARDADO);

      // Paso 9: Cuando pulsa «Cancelar» y vuelve al listado.
      // Con cambios sin guardar, Axelor pregunta «Los cambios actuales se perderán.
      // ¿Realmente quieres continuar?»: se acepta para volver al listado.
      await botonCancelar(page).click();
      const pregunta = page.getByRole('dialog').filter({ hasText: 'Los cambios actuales se perderán' });
      await pregunta.or(botonNuevo(page)).first().waitFor();
      if (await pregunta.isVisible().catch(() => false)) {
        await pregunta.getByRole('button', { name: 'Aceptar' }).click();
      }
      await expect(page).toHaveURL(/AceProfileCentro-action\/list/);
      await esperarListadoCargado(page);

      // Resultado esperado: el listado muestra una sola fila con centro «CIPFP
      // Mislata», trámite «Trámite de prueba», perfil «Creador» y tipo de usuario
      // «Profesor». Se comprueba tras una recarga en duro: lo que hay en la BD,
      // no solo la rejilla en memoria de la SPA.
      await recargarListado(page);
      const filas = filasDelEscenario(page);
      const total = await filas.count();
      if (total > 1) creadas = total; // la app guardó el duplicado: el teardown también lo borra.
      await expect(filas).toHaveCount(1);
      const fila = filas.first();
      await expect(await celda(page, fila, 'Centro')).toHaveText(CENTRO);
      await expect(await celda(page, fila, 'Trámite')).toHaveText(TRAMITE);
      await expect(await celda(page, fila, 'Perfil')).toHaveText(PERFIL);
      await expect(await celda(page, fila, 'Tipo usuario')).toHaveText(TIPO_USUARIO);
      await expect(await celda(page, fila, 'Cargo')).toHaveText('');
      await expect(await celda(page, fila, 'Usuario')).toHaveText('');
    } finally {
      // Teardown: borra las filas del escenario que ESTA ejecución creó (y solo
      // esas; la guarda de precondición asegura que no había ninguna antes).
      // Best-effort (el `try/catch` es intencional) para no enmascarar un fallo real.
      if (creadas > 0) {
        try {
          await recargarListado(page);
          for (let i = 0; i < creadas && (await filasDelEscenario(page).count()) > 0; i++) {
            await abrirYBorrarFila(page);
          }
        } catch {
          // limpieza best-effort.
        }
      }
    }

    await logout(page);
  });
});
