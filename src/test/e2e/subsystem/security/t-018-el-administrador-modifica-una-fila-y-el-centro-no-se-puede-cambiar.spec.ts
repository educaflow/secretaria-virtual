import { test, expect, Locator, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-018 — El administrador modifica una fila y el centro no se puede cambiar
// origen: ESC-011  |  verifica: U-perfiles-tramites-todos-centros-001
// fuente: .sdd/drafts/2026-09-25_20-58_mantenimiento-perfiles-centro/test-e2e-desc/t-018-el-administrador-modifica-una-fila-y-el-centro-no-se-puede-cambiar.desc.md

// Valores de la fila del escenario. NO llevan sufijo `Date.now()` (excepción
// documentada de la regla de nombres únicos): son valores de catálogo (centro,
// trámite, perfiles del enum y tipo de usuario), no texto libre.
// La idempotencia se consigue con el teardown del `finally`, que borra la fila
// que ESTA ejecución ha creado (tenga el perfil inicial o el cambiado).
//
// CRITICAL — NO hay pre-limpieza defensiva a propósito: el usuario NO autoriza
// borrar datos preexistentes de la BD. Si al arrancar ya hubiera una fila con
// estos valores (residual de otro run o dato ajeno), el test FALLA en la guarda
// de precondición en vez de borrarla; y el teardown solo borra la fila que haya
// creado ESTA ejecución (`creada`).
const CENTRO = 'CIPFP Batoi';
const TRAMITE = 'Trámite de prueba';
const PERFIL_INICIAL = 'Creador';
const PERFIL_NUEVO = 'Colaborador';
const TIPO_USUARIO = 'Profesor';

const URL_LISTADO_ADMIN = '/#/ds/subsysSecurity.Main%40AceProfileCentro-action/list/1';

// El nombre accesible de una fila del listado es «<centro> <trámite> <perfil>
// <tipo usuario> <cargo> <usuario>» (las celdas vacías no aportan texto) y el
// match de Playwright es por subcadena. El listado del administrador mezcla
// centros, así que la fila se identifica también por el centro.
const filasConPerfil = (page: Page, perfil: string) =>
  page.getByRole('row', { name: `${CENTRO} ${TRAMITE} ${perfil} ${TIPO_USUARIO}` });
const filaSinRegistros = (page: Page) => page.getByRole('row', { name: 'No se encontraron registros.' });
const cabeceraListado = (page: Page) => page.getByRole('columnheader', { name: 'Trámite' });

const botonNuevo = (page: Page) => page.getByRole('button', { name: 'Nuevo' });
// «Borrar» solo existe en el formulario de un registro ya guardado.
const botonBorrar = (page: Page) => page.getByRole('button', { name: 'Borrar' });
const botonAceptarAviso = (page: Page) => page.getByRole('dialog').getByRole('button', { name: /^(OK|Aceptar)$/ });
const dialogoValidationError = (page: Page) =>
  page.getByRole('dialog').getByRole('heading', { name: 'La acción no se completó por los siguientes motivos' });

const campo = (page: Page, nombre: string) => page.getByRole('combobox', { name: nombre, exact: true });
// «Perfil» es un grupo de radios (widget RadioSelect), no un combobox.
const radioPerfil = (page: Page, opcion: string) => page.getByRole('radio', { name: opcion, exact: true });

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

// Pulsa «Guardar» en el formulario y espera a volver al listado sin error.
async function guardarYVolverAlListado(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Guardar' }).click();
  const error = dialogoValidationError(page);
  await error.or(botonNuevo(page)).first().waitFor();
  await expect(error).toHaveCount(0);
  await expect(page).toHaveURL(/AceProfileCentro-action\/list/);
  // Drena el aviso con retardo que Axelor puede lanzar tras guardar.
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

// Abre la (única) fila `filas` y la borra confirmando el diálogo.
async function abrirYBorrarFila(page: Page, filas: Locator): Promise<void> {
  await pulsarHasta(page, filas.first(), botonBorrar(page), 'la fila del escenario');
  await botonBorrar(page).click();
  const dialogo = page.getByRole('dialog');
  await expect(dialogo.getByText('¿Realmente quieres eliminar el registro seleccionado?')).toBeVisible();
  await dialogo.getByRole('button', { name: /^(Eliminar|Delete)$/ }).click();
  await expect(page).toHaveURL(/AceProfileCentro-action\/list/);
  await esperarListadoCargado(page);
}

test.describe('Perfiles de trámites por centro (Administración)', () => {
  test('El administrador modifica una fila y el centro no se puede cambiar', async ({ page }) => {
    // true en cuanto ESTA ejecución ha guardado la fila del escenario: es lo único
    // que el teardown puede borrar (nunca datos preexistentes).
    let creada = false;

    await ensureLoggedOut(page);
    // Paso 1: Dado que el administrador inicia sesión con usuario «admin» y
    //         contraseña «admin».
    await login(page, 'admin', 'admin');

    try {
      // Paso 2: Cuando abre «Administración → Perfiles de trámites por centro» …
      await abrirPerfilesPorCentro(page);

      // Guarda de precondición (sin borrar nada): el estado inicial no tiene
      // ninguna fila de «CIPFP Batoi» con estos valores. Si la hubiera, el alta
      // chocaría con la regla de asignación repetida o el resultado esperado
      // sería ambiguo.
      for (const perfil of [PERFIL_INICIAL, PERFIL_NUEVO]) {
        await expect(
          filasConPerfil(page, perfil),
          `Ya existe una fila «${CENTRO} / ${TRAMITE} / ${perfil} / ${TIPO_USUARIO}» antes de empezar; no se borra (dato preexistente)`,
        ).toHaveCount(0);
      }

      // … y pulsa «Nuevo».
      await pulsarHasta(page, botonNuevo(page), campo(page, 'Trámite'), 'el botón «Nuevo»');

      // Paso 3: Y elige el centro «CIPFP Batoi», el trámite «Trámite de prueba»,
      //         el perfil «Creador» y el tipo de usuario «Profesor», sin rellenar
      //         cargo ni usuario.
      await elegir(page, 'Centro', CENTRO, 'Batoi');
      await elegir(page, 'Trámite', TRAMITE, 'prueba');
      await radioPerfil(page, PERFIL_INICIAL).check();
      await elegir(page, 'Tipo usuario', TIPO_USUARIO);
      await expect(campo(page, 'Cargo')).toHaveValue('');
      await expect(campo(page, 'Usuario')).toHaveValue('');

      // Paso 4: Y pulsa «Guardar».
      await guardarYVolverAlListado(page);
      creada = true;

      // Paso 5: Y vuelve al listado y abre la fila recién creada.
      await expect(filasConPerfil(page, PERFIL_INICIAL)).toHaveCount(1);
      await pulsarHasta(page, filasConPerfil(page, PERFIL_INICIAL).first(), botonBorrar(page), 'la fila del escenario');
      await expect(campo(page, 'Trámite')).toHaveValue(TRAMITE);
      await expect(radioPerfil(page, PERFIL_INICIAL)).toBeChecked();
      await expect(campo(page, 'Tipo usuario')).toHaveValue(TIPO_USUARIO);

      // Paso 6: Entonces el formulario muestra el centro «CIPFP Batoi» sin
      //         posibilidad de cambiarlo.
      // En un registro ya guardado Axelor pinta el centro en solo lectura: el valor
      // como enlace (botón) dentro de la sección «Perfil» y SIN el combobox
      // editable que tiene en el alta.
      const seccionPerfil = page.getByRole('region', { name: 'Perfil', exact: true });
      await expect(seccionPerfil.getByText('Centro', { exact: true })).toBeVisible();
      await expect(seccionPerfil.getByRole('button', { name: CENTRO, exact: true })).toBeVisible();
      await expect(campo(page, 'Centro')).toHaveCount(0);
      await expect(seccionPerfil.getByRole('textbox', { name: 'Centro', exact: true })).toHaveCount(0);

      // Paso 7: Cuando cambia el perfil a «Colaborador» y pulsa «Guardar».
      await radioPerfil(page, PERFIL_NUEVO).check();
      // Paso 8: Y vuelve al listado.
      await guardarYVolverAlListado(page);

      // Resultado esperado (tras recarga en duro: el cambio llegó a la BD, no solo
      // a la rejilla en memoria de la SPA):
      await recargarListado(page);

      // - La fila del centro «CIPFP Batoi» muestra trámite «Trámite de prueba»,
      //   perfil «Colaborador» y tipo de usuario «Profesor».
      await expect(filasConPerfil(page, PERFIL_NUEVO)).toHaveCount(1);
      const fila = filasConPerfil(page, PERFIL_NUEVO).first();
      await expect(await celda(page, fila, 'Centro')).toHaveText(CENTRO);
      await expect(await celda(page, fila, 'Trámite')).toHaveText(TRAMITE);
      await expect(await celda(page, fila, 'Perfil')).toHaveText(PERFIL_NUEVO);
      await expect(await celda(page, fila, 'Tipo usuario')).toHaveText(TIPO_USUARIO);
      // La modificación no dejó la fila con el perfil anterior.
      await expect(filasConPerfil(page, PERFIL_INICIAL)).toHaveCount(0);
    } finally {
      // Teardown: borra la fila que ESTA ejecución creó (y solo esa), tenga ya el
      // perfil cambiado o no. Best-effort (el `try/catch` es intencional) para no
      // enmascarar un fallo real.
      if (creada) {
        try {
          await recargarListado(page);
          for (const perfil of [PERFIL_NUEVO, PERFIL_INICIAL]) {
            if ((await filasConPerfil(page, perfil).count()) === 1) {
              await abrirYBorrarFila(page, filasConPerfil(page, perfil));
            }
          }
        } catch {
          // limpieza best-effort.
        }
      }
    }

    await logout(page);
  });
});
