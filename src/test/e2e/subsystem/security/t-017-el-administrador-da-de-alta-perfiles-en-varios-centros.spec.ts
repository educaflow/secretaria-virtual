import { test, expect, Locator, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-017 — El administrador da de alta perfiles en varios centros
// origen: ESC-010  |  verifica: —
// fuente: .sdd/drafts/2026-09-25_20-58_mantenimiento-perfiles-centro/test-e2e-desc/t-017-el-administrador-da-de-alta-perfiles-en-varios-centros.desc.md

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
const PERFIL = 'Secretario';
const CARGO = 'Secretario';

const URL_LISTADO_ADMIN = '/#/ds/subsysSecurity.Main%40AceProfileCentro-action/list/1';

// El nombre accesible de una fila del listado es «<centro> <trámite> <perfil>
// <tipo usuario> <cargo> <usuario>» (las celdas vacías no aportan texto). Con el
// tipo de usuario vacío, trámite, perfil y cargo quedan contiguos.
const filaDelCentro = (page: Page, centro: string) =>
  page.getByRole('row', { name: `${centro} ${TRAMITE} ${PERFIL} ${CARGO}` });
const filasDelEscenario = (page: Page) =>
  page.getByRole('row', { name: `${TRAMITE} ${PERFIL} ${CARGO}` });
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

// Pasos 3 y 6: rellena el alta con el centro indicado y el resto de valores del
// escenario, sin tipo de usuario ni usuario.
async function rellenarAlta(page: Page, centro: string, textoCentro: string): Promise<void> {
  await elegir(page, 'Centro', centro, textoCentro);
  await elegir(page, 'Trámite', TRAMITE, 'prueba');
  // «Perfil» es un grupo de radios (widget RadioSelect); «Cargo» sigue siendo combobox.
  const radioPerfil = page.getByRole('radio', { name: PERFIL, exact: true });
  await radioPerfil.check();
  await expect(radioPerfil).toBeChecked();
  await elegir(page, 'Cargo', CARGO, 'Secretario');
  await expect(campo(page, 'Tipo usuario')).toHaveValue('');
  await expect(campo(page, 'Usuario')).toHaveValue('');
}

// Pasos 4 y 7: pulsa «Guardar» y comprueba que no hay error de validación y que
// la app vuelve al listado (la fila ya está guardada).
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
  test('El administrador da de alta perfiles en varios centros', async ({ page }) => {
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
      //         el perfil «Secretario» y el cargo «Secretario», sin rellenar tipo
      //         de usuario ni usuario.
      await rellenarAlta(page, CENTRO_BATOI, 'Batoi');

      // Paso 4: Y pulsa «Guardar».
      await guardar(page);
      creadas.push(CENTRO_BATOI);
      await drenarAviso(page);

      // Paso 5: Y pulsa «Nuevo».
      await pulsarHasta(page, botonNuevo(page), campo(page, 'Trámite'), 'el botón «Nuevo»');

      // Paso 6: Y elige el centro «CIPFP Mislata», el trámite «Trámite de prueba»,
      //         el perfil «Secretario» y el cargo «Secretario», sin rellenar tipo
      //         de usuario ni usuario.
      await rellenarAlta(page, CENTRO_MISLATA, 'Mislata');

      // Paso 7: Y pulsa «Guardar».
      await guardar(page);
      creadas.push(CENTRO_MISLATA);

      // Paso 8: Y vuelve al listado.
      await drenarAviso(page);

      // Resultado esperado: el listado muestra dos filas con trámite «Trámite de
      // prueba», perfil «Secretario» y cargo «Secretario»: una del centro «CIPFP
      // Batoi» y otra del centro «CIPFP Mislata».
      // Se comprueba tras una recarga en duro: las filas llegaron a la BD, no solo
      // a la rejilla en memoria de la SPA.
      await recargarListado(page);
      await expect(filasDelEscenario(page)).toHaveCount(2);
      for (const centro of [CENTRO_BATOI, CENTRO_MISLATA]) {
        await expect(filaDelCentro(page, centro)).toHaveCount(1);
        const fila = filaDelCentro(page, centro).first();
        await expect(await celda(page, fila, 'Centro')).toHaveText(centro);
        await expect(await celda(page, fila, 'Trámite')).toHaveText(TRAMITE);
        await expect(await celda(page, fila, 'Perfil')).toHaveText(PERFIL);
        await expect(await celda(page, fila, 'Cargo')).toHaveText(CARGO);
      }
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
