import { test, expect, Locator, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-021 — El administrador filtra el listado por centro
// origen: ESC-014  |  verifica: —
// fuente: .sdd/drafts/2026-09-25_20-58_mantenimiento-perfiles-centro/test-e2e-desc/t-021-el-administrador-filtra-el-listado-por-centro.desc.md

// Valores de las filas del escenario. NO llevan sufijo `Date.now()` (excepción
// documentada de la regla de nombres únicos): son valores de catálogo (centros,
// trámite, perfil del enum y cargo), no texto libre.
// La idempotencia se consigue con el teardown del `finally`, que borra las filas
// que ESTA ejecución ha creado (haya fallado o no una aserción).
//
// CRITICAL — NO hay pre-limpieza defensiva a propósito: el usuario NO autoriza
// borrar datos preexistentes de la BD. Si al arrancar ya hubiera una fila con
// estos valores (residual de otro run o dato ajeno), el test FALLA en la guarda
// de precondición en vez de borrarla; y el teardown solo borra las filas que haya
// creado ESTA ejecución.
const CENTRO_BATOI = 'CIPFP Batoi';
const CENTRO_MISLATA = 'CIPFP Mislata';
const TRAMITE = 'Trámite de prueba';
const PERFIL = 'Tramitador';
const CARGO = 'Jefe de estudios';

const URL_LISTADO_ADMIN = '/#/ds/subsysSecurity.Main%40AceProfileCentro-action/list/1';

// El nombre accesible de una fila del listado es «<centro> <trámite> <perfil>
// <tipo usuario> <cargo> <usuario>» (las celdas vacías no aportan texto). Con el
// tipo de usuario vacío, trámite, perfil y cargo quedan contiguos.
const filaDelCentro = (page: Page, centro: string) =>
  page.getByRole('row', { name: `${centro} ${TRAMITE} ${PERFIL} ${CARGO}` });
const filasDelEscenario = (page: Page) =>
  page.getByRole('row', { name: `${TRAMITE} ${PERFIL} ${CARGO}` });
// Cualquier fila de datos de un centro (el nombre accesible empieza por el centro).
const filasDeCentro = (page: Page, centro: string) =>
  page.getByRole('row', { name: new RegExp(`^${centro}\\b`) });
const filaSinRegistros = (page: Page) => page.getByRole('row', { name: 'No se encontraron registros.' });
const cabeceraListado = (page: Page) => page.getByRole('columnheader', { name: 'Trámite' });

const botonNuevo = (page: Page) => page.getByRole('button', { name: 'Nuevo' });
const botonGuardar = (page: Page) => page.getByRole('button', { name: 'Guardar' });
// «Borrar» solo existe en el formulario de un registro ya guardado.
const botonBorrar = (page: Page) => page.getByRole('button', { name: 'Borrar' });
const botonAceptarAviso = (page: Page) => page.getByRole('dialog').getByRole('button', { name: /^(OK|Aceptar)$/ });
const dialogoValidationError = (page: Page) =>
  page.getByRole('dialog').getByRole('heading', { name: 'La acción no se completó por los siguientes motivos' });

const campo = (page: Page, nombre: string) => page.getByRole('combobox', { name: nombre, exact: true });

// Barrera de carga del listado: la rejilla pide sus filas en una petición aparte,
// así que se espera a que haya al menos una fila de datos (de cualquier centro
// «CIPFP …») o la de «sin registros».
async function esperarListadoCargado(page: Page): Promise<void> {
  await expect(botonNuevo(page)).toBeVisible();
  await expect(cabeceraListado(page)).toBeVisible();
  const filaDeDatos = page.getByRole('row', { name: /CIPFP/ });
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
// guardar y que intercepta o descarta la navegación.
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

// Abre «Administración → Perfiles de trámites por centro» desde el menú.
async function abrirPerfilesPorCentro(page: Page): Promise<void> {
  await page.getByText('Administración', { exact: true }).click();
  await page.getByText('Perfiles de trámites por centro', { exact: true }).click();
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

// Pasos 3 y 4: rellena el alta con el centro indicado y el resto de valores del
// escenario.
async function rellenarAlta(page: Page, centro: string, textoCentro: string): Promise<void> {
  await elegir(page, 'Centro', centro, textoCentro);
  await elegir(page, 'Trámite', TRAMITE, 'prueba');
  // «Perfil» es un enum con `widget="RadioSelect"`: un grupo de radios, no un combobox.
  await page.getByRole('radio', { name: PERFIL, exact: true }).check();
  await elegir(page, 'Cargo', CARGO, 'Jefe');
}

// Pulsa «Guardar» y comprueba que no hay error de validación y que la app vuelve
// al listado (la fila ya está guardada).
async function guardar(page: Page): Promise<void> {
  await botonGuardar(page).click();
  const error = dialogoValidationError(page);
  await error.or(botonNuevo(page)).first().waitFor();
  await expect(error).toHaveCount(0);
  await expect(page).toHaveURL(/AceProfileCentro-action\/list/);
}

// Drena el aviso con retardo que Axelor puede lanzar tras guardar.
async function drenarAviso(page: Page): Promise<void> {
  await botonAceptarAviso(page).waitFor({ timeout: 5000 }).catch(() => {});
  await cerrarAviso(page);
  await esperarListadoCargado(page);
}

// Recarga en duro el listado para leer el estado REAL de la BD, no la rejilla en
// memoria de la SPA.
async function recargarListado(page: Page): Promise<void> {
  await page.goto(URL_LISTADO_ADMIN);
  await page.reload();
  await esperarListadoCargado(page);
}

// Índice de la columna titulada `columna`: se resuelve por la posición de la
// cabecera, así no depende del orden de columnas de la vista.
async function indiceColumna(page: Page, columna: string): Promise<number> {
  const cabeceras = await page.getByRole('columnheader').allInnerTexts();
  // El texto de una cabecera ordenable incluye el icono («Perfil\nsort»): se
  // compara solo su primera línea, que es el título.
  const indice = cabeceras.findIndex((t) => t.split('\n')[0].trim() === columna);
  expect(indice, `No hay columna «${columna}» en el listado (cabeceras: ${cabeceras.join(' | ')})`).toBeGreaterThanOrEqual(0);
  return indice;
}

// Celda de `fila` bajo la columna titulada `columna`.
async function celda(page: Page, fila: Locator, columna: string): Promise<Locator> {
  return fila.getByRole('gridcell').nth(await indiceColumna(page, columna));
}

// Caja «Buscar...» de la fila de búsqueda del listado bajo la columna `columna`.
async function cajaBusqueda(page: Page, columna: string): Promise<Locator> {
  const filaBusqueda = page.getByRole('row').filter({ has: page.getByRole('textbox', { name: 'Buscar...' }) });
  return filaBusqueda.getByRole('gridcell').nth(await indiceColumna(page, columna)).getByRole('textbox', { name: 'Buscar...' });
}

// Abre la (única) fila del escenario de `centro` y la borra confirmando el diálogo.
async function abrirYBorrarFila(page: Page, centro: string): Promise<void> {
  await pulsarHasta(page, filaDelCentro(page, centro).first(), botonBorrar(page), `la fila de «${centro}»`);
  await botonBorrar(page).click();
  const dialogo = page.getByRole('dialog');
  await expect(dialogo.getByText('¿Realmente quieres eliminar el registro seleccionado?')).toBeVisible();
  await dialogo.getByRole('button', { name: /^(Eliminar|Delete)$/ }).click();
  await expect(page).toHaveURL(/AceProfileCentro-action\/list/);
  await esperarListadoCargado(page);
}

test.describe('Perfiles de trámites por centro (Administración)', () => {
  test('El administrador filtra el listado por centro', async ({ page }) => {
    // Centros cuya fila del escenario ha guardado ESTA ejecución: es lo único
    // que el teardown puede borrar (nunca datos preexistentes).
    const creadas: string[] = [];

    await ensureLoggedOut(page);
    // Paso 1: Dado que el administrador inicia sesión con usuario «admin» y
    //         contraseña «admin».
    await login(page, 'admin', 'admin');

    try {
      // Paso 2: Cuando abre «Administración → Perfiles de trámites por centro» …
      await abrirPerfilesPorCentro(page);

      // Guarda de precondición (sin borrar nada): el estado inicial no tiene
      // ninguna fila con estos valores. Si la hubiera, el alta chocaría con la
      // regla de asignación repetida y el test no probaría lo que dice probar.
      await expect(
        filasDelEscenario(page),
        `Ya existe una fila «${TRAMITE} / ${PERFIL} / ${CARGO}» antes de empezar; no se borra (dato preexistente)`,
      ).toHaveCount(0);

      // … y pulsa «Nuevo».
      await pulsarHasta(page, botonNuevo(page), campo(page, 'Trámite'), 'el botón «Nuevo»');

      // Paso 3: Y elige el centro «CIPFP Batoi», el trámite «Trámite de prueba»,
      //         el perfil «Tramitador» y el cargo «Jefe de estudios», y pulsa
      //         «Guardar».
      await rellenarAlta(page, CENTRO_BATOI, 'Batoi');
      await guardar(page);
      creadas.push(CENTRO_BATOI);
      await drenarAviso(page);

      // Paso 4: Y pulsa «Nuevo», elige el centro «CIPFP Mislata», el trámite
      //         «Trámite de prueba», el perfil «Tramitador» y el cargo «Jefe de
      //         estudios», y pulsa «Guardar».
      await pulsarHasta(page, botonNuevo(page), campo(page, 'Trámite'), 'el botón «Nuevo»');
      await rellenarAlta(page, CENTRO_MISLATA, 'Mislata');
      await guardar(page);
      creadas.push(CENTRO_MISLATA);

      // Paso 5: Y vuelve al listado.
      await drenarAviso(page);
      await recargarListado(page);
      // Sanidad del escenario: sin filtro, ambas filas están en el listado (así
      // la ausencia de Mislata tras filtrar es obra del filtro).
      await expect(filaDelCentro(page, CENTRO_BATOI)).toHaveCount(1);
      await expect(filaDelCentro(page, CENTRO_MISLATA)).toHaveCount(1);

      // Paso 6: Y en la búsqueda del listado filtra por centro escribiendo
      //         «CIPFP Batoi» y aplica el filtro.
      const buscarCentro = await cajaBusqueda(page, 'Centro');
      await buscarCentro.fill(CENTRO_BATOI);
      await buscarCentro.press('Enter');

      // Resultado esperado: el listado no muestra ninguna fila del centro «CIPFP
      // Mislata».
      await expect(filasDeCentro(page, CENTRO_MISLATA)).toHaveCount(0);

      // Resultado esperado: el listado muestra la fila del centro «CIPFP Batoi»
      // con trámite «Trámite de prueba», perfil «Tramitador» y cargo «Jefe de
      // estudios».
      await expect(filaDelCentro(page, CENTRO_BATOI)).toHaveCount(1);
      const fila = filaDelCentro(page, CENTRO_BATOI).first();
      await expect(await celda(page, fila, 'Centro')).toHaveText(CENTRO_BATOI);
      await expect(await celda(page, fila, 'Trámite')).toHaveText(TRAMITE);
      await expect(await celda(page, fila, 'Perfil')).toHaveText(PERFIL);
      await expect(await celda(page, fila, 'Cargo')).toHaveText(CARGO);
    } finally {
      // Teardown: borra las filas que ESTA ejecución creó (y solo esas; la guarda
      // de precondición asegura que no había ninguna antes). Best-effort (el
      // `try/catch` es intencional) para no enmascarar un fallo real.
      for (const centro of creadas) {
        try {
          await recargarListado(page);
          if ((await filaDelCentro(page, centro).count()) === 1) {
            await abrirYBorrarFila(page, centro);
          }
        } catch {
          // limpieza best-effort.
        }
      }
    }

    await logout(page);
  });
});
