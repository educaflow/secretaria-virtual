import { test, expect, Locator, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-003 — El supervisor cambia el perfil de una fila y el centro no se puede cambiar
// origen: ESC-003  |  verifica: V-AceProfileCentro-008, U-perfiles-tramites-mi-centro-002
// fuente: .sdd/drafts/2026-09-25_20-58_mantenimiento-perfiles-centro/test-e2e-desc/t-003-el-supervisor-cambia-el-perfil-de-una-fila-y-el-centro-no-se-puede-cambiar.desc.md

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
const CENTRO = 'CIPFP Mislata';
const TRAMITE = 'Trámite de prueba';
const PERFIL_INICIAL = 'Creador';
const PERFIL_NUEVO = 'Afectado';
const TIPO_USUARIO = 'Alumno';

const URL_LISTADO = '/#/ds/subsysSecurity.Centro%40AceProfileCentro-action/list/1';

// El nombre accesible de una fila del listado es «<centro> <trámite> <perfil>
// <tipo usuario> <cargo> <usuario>» (las celdas vacías no aportan texto) y el
// match de Playwright es por subcadena.
const filasConPerfil = (page: Page, perfil: string) =>
  page.getByRole('row', { name: `${TRAMITE} ${perfil} ${TIPO_USUARIO}` });
// Cualquier fila (de cualquier trámite) con perfil «Creador» y tipo «Alumno».
const filasCreadorAlumno = (page: Page) =>
  page.getByRole('row', { name: `${PERFIL_INICIAL} ${TIPO_USUARIO}` });
const filaSinRegistros = (page: Page) => page.getByRole('row', { name: 'No se encontraron registros.' });
const cabeceraListado = (page: Page) => page.getByRole('columnheader', { name: 'Trámite' });

const botonNuevo = (page: Page) => page.getByRole('button', { name: 'Nuevo' });
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

test.describe('Perfiles de trámites (Mi centro)', () => {
  test('El supervisor cambia el perfil de una fila y el centro no se puede cambiar', async ({ page }) => {
    // true en cuanto ESTA ejecución ha guardado la fila del escenario: es lo único
    // que el teardown puede borrar (nunca datos preexistentes).
    let creada = false;

    await ensureLoggedOut(page);
    // Paso 1: Dado que el supervisor inicia sesión con usuario
    //         «supervisor1@mislata.es» y contraseña «demo1234».
    await login(page, 'supervisor1@mislata.es', 'demo1234');

    try {
      // Paso 2: Cuando abre «Mi centro → Perfiles de trámites» …
      await abrirPerfilesDeTramites(page);

      // Guarda de precondición (sin borrar nada): el estado inicial no tiene
      // ninguna fila con estos valores. Si la hubiera, el alta chocaría con la
      // regla de asignación repetida o el resultado esperado sería ambiguo.
      await expect(
        filasCreadorAlumno(page),
        `Ya existe una fila «${PERFIL_INICIAL} / ${TIPO_USUARIO}» antes de empezar; no se borra (dato preexistente)`,
      ).toHaveCount(0);
      await expect(
        filasConPerfil(page, PERFIL_NUEVO),
        `Ya existe una fila «${TRAMITE} / ${PERFIL_NUEVO} / ${TIPO_USUARIO}» antes de empezar; no se borra (dato preexistente)`,
      ).toHaveCount(0);

      // … y pulsa «Nuevo».
      await pulsarHasta(page, botonNuevo(page), campo(page, 'Trámite'), 'el botón «Nuevo»');

      // Paso 3: Y elige el centro «CIPFP Mislata», el trámite «Trámite de prueba»,
      //         el perfil «Creador» y el tipo de usuario «Alumno», sin rellenar
      //         cargo ni usuario.
      // El centro sale prellenado con el del supervisor; solo se elige si no lo está.
      const centro = campo(page, 'Centro');
      if ((await centro.inputValue()) !== CENTRO) {
        await elegir(page, 'Centro', CENTRO, 'Mislata');
      }
      await expect(centro).toHaveValue(CENTRO);
      await elegir(page, 'Trámite', TRAMITE, 'prueba');
      await elegir(page, 'Perfil', PERFIL_INICIAL);
      await elegir(page, 'Tipo usuario', TIPO_USUARIO);
      await expect(campo(page, 'Cargo')).toHaveValue('');
      await expect(campo(page, 'Usuario')).toHaveValue('');

      // Paso 4: Y pulsa «Guardar» y vuelve al listado.
      await guardarYVolverAlListado(page);
      creada = true;
      await expect(filasConPerfil(page, PERFIL_INICIAL)).toHaveCount(1);

      // Paso 5: Y abre la fila con centro «CIPFP Mislata», trámite «Trámite de
      //         prueba», perfil «Creador» y tipo de usuario «Alumno».
      await pulsarHasta(page, filasConPerfil(page, PERFIL_INICIAL).first(), botonBorrar(page), 'la fila del escenario');
      await expect(campo(page, 'Trámite')).toHaveValue(TRAMITE);
      await expect(campo(page, 'Perfil')).toHaveValue(PERFIL_INICIAL);
      await expect(campo(page, 'Tipo usuario')).toHaveValue(TIPO_USUARIO);

      // Paso 6: Entonces el formulario muestra el centro «CIPFP Mislata» sin
      //         posibilidad de cambiarlo.
      // En un registro ya guardado Axelor pinta el centro en solo lectura: el valor
      // como enlace (botón) dentro de la sección «Perfil» y SIN el combobox
      // editable que tiene en el alta.
      const seccionPerfil = page.getByRole('region', { name: 'Perfil', exact: true });
      await expect(seccionPerfil.getByText('Centro', { exact: true })).toBeVisible();
      await expect(seccionPerfil.getByRole('button', { name: CENTRO, exact: true })).toBeVisible();
      await expect(campo(page, 'Centro')).toHaveCount(0);
      await expect(seccionPerfil.getByRole('textbox', { name: 'Centro', exact: true })).toHaveCount(0);

      // Paso 7: Cuando cambia el perfil a «Afectado».
      await elegir(page, 'Perfil', PERFIL_NUEVO);

      // Paso 8: Y pulsa «Guardar» y vuelve al listado.
      await guardarYVolverAlListado(page);

      // Resultado esperado (tras recarga en duro: el cambio llegó a la BD, no solo
      // a la rejilla en memoria de la SPA):
      await recargarListado(page);

      // - El listado muestra la fila del centro «CIPFP Mislata» con trámite
      //   «Trámite de prueba», perfil «Afectado» y tipo de usuario «Alumno».
      await expect(filasConPerfil(page, PERFIL_NUEVO)).toHaveCount(1);
      const fila = filasConPerfil(page, PERFIL_NUEVO).first();
      await expect(await celda(page, fila, 'Centro')).toHaveText(CENTRO);
      await expect(await celda(page, fila, 'Trámite')).toHaveText(TRAMITE);
      await expect(await celda(page, fila, 'Perfil')).toHaveText(PERFIL_NUEVO);
      await expect(await celda(page, fila, 'Tipo usuario')).toHaveText(TIPO_USUARIO);

      // - El listado ya no muestra ninguna fila con perfil «Creador» y tipo de
      //   usuario «Alumno».
      await expect(filasCreadorAlumno(page)).toHaveCount(0);
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
