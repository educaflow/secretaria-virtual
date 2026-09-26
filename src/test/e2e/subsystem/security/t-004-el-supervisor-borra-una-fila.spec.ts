import { test, expect, Locator, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-004 — El supervisor borra una fila
// origen: ESC-004  |  verifica: V-AceProfileCentro-008
// fuente: .sdd/drafts/2026-09-25_20-58_mantenimiento-perfiles-centro/test-e2e-desc/t-004-el-supervisor-borra-una-fila.desc.md

// Valores de la fila del escenario. NO llevan sufijo `Date.now()` (excepción
// documentada de la regla de nombres únicos): los cuatro son valores de catálogo
// (centro, trámite, perfil del enum y tipo de usuario), no texto libre.
// La idempotencia se consigue porque el propio escenario BORRA la fila que crea,
// y el `finally` la borra si una aserción falló antes del borrado.
//
// CRITICAL — NO hay pre-limpieza defensiva a propósito: el usuario NO autoriza
// borrar datos preexistentes de la BD. Si al arrancar ya hubiera una fila con
// estos valores (residual de otro run o dato ajeno), el test FALLA en la guarda
// de precondición en vez de borrarla; y el teardown solo borra la fila que haya
// creado ESTA ejecución (`creada`).
const CENTRO = 'CIPFP Mislata';
const TRAMITE = 'Anulación de matrícula en ciclo formativo';
const PERFIL = 'Auditor';
const TIPO_USUARIO = 'Administrativo';

const URL_LISTADO = '/#/ds/subsysSecurity.Centro%40AceProfileCentro-action/list/1';

// El nombre accesible de una fila del listado es «<centro> <trámite> <perfil>
// <tipo usuario> <cargo> <usuario>» (las celdas vacías no aportan texto). El
// match de Playwright es por subcadena, así que este locator casa con CUALQUIER
// fila con ese trámite, perfil y tipo de usuario, tenga o no cargo/usuario: es
// justo lo que pide el «Resultado esperado».
const filasDelEscenario = (page: Page) =>
  page.getByRole('row', { name: `${TRAMITE} ${PERFIL} ${TIPO_USUARIO}` });
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
// guardar y que intercepta o descarta la navegación (patrón de los specs de
// `subsystem/criptografia`).
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

// Recarga en duro el listado para leer el estado REAL de la BD, no la rejilla en
// memoria de la SPA.
async function recargarListado(page: Page): Promise<void> {
  await page.goto(URL_LISTADO);
  await page.reload();
  await esperarListadoCargado(page);
}

// Abre la (única) fila del escenario y la borra confirmando el diálogo.
async function abrirYBorrarFila(page: Page): Promise<void> {
  await pulsarHasta(page, filasDelEscenario(page).first(), botonBorrar(page), 'la fila del escenario');
  // Paso 6: Y pulsa «Borrar».
  await botonBorrar(page).click();
  // Paso 7: Y confirma el borrado (diálogo «Pregunta» → «Eliminar»).
  const dialogo = page.getByRole('dialog');
  await expect(dialogo.getByText('¿Realmente quieres eliminar el registro seleccionado?')).toBeVisible();
  await dialogo.getByRole('button', { name: /^(Eliminar|Delete)$/ }).click();
  await expect(page).toHaveURL(/AceProfileCentro-action\/list/);
  await esperarListadoCargado(page);
}

test.describe('Perfiles de trámites (Mi centro)', () => {
  test('El supervisor borra una fila', async ({ page }) => {
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
      // regla de asignación repetida y el test no probaría lo que dice probar.
      await expect(
        filasDelEscenario(page),
        `Ya existe una fila «${TRAMITE} / ${PERFIL} / ${TIPO_USUARIO}» antes de empezar; no se borra (dato preexistente)`,
      ).toHaveCount(0);

      // … y pulsa «Nuevo».
      await pulsarHasta(page, botonNuevo(page), campo(page, 'Trámite'), 'el botón «Nuevo»');

      // Paso 3: Y elige el centro «CIPFP Mislata», el trámite «Anulación de
      //         matrícula en ciclo formativo», el perfil «Auditor» y el tipo de
      //         usuario «Administrativo», sin rellenar cargo ni usuario.
      // El centro sale prellenado con el del supervisor; solo se elige si no lo está.
      const centro = campo(page, 'Centro');
      if ((await centro.inputValue()) !== CENTRO) {
        await elegir(page, 'Centro', CENTRO, 'Mislata');
      }
      await expect(centro).toHaveValue(CENTRO);
      await elegir(page, 'Trámite', TRAMITE, 'Anulación');
      await elegir(page, 'Perfil', PERFIL);
      await elegir(page, 'Tipo usuario', TIPO_USUARIO);
      await expect(campo(page, 'Cargo')).toHaveValue('');
      await expect(campo(page, 'Usuario')).toHaveValue('');

      // Paso 4: Y pulsa «Guardar» y vuelve al listado.
      await page.getByRole('button', { name: 'Guardar' }).click();
      const error = dialogoValidationError(page);
      await error.or(botonNuevo(page)).first().waitFor();
      await expect(error).toHaveCount(0);
      await expect(page).toHaveURL(/AceProfileCentro-action\/list/);
      creada = true;
      // Drena el aviso con retardo que Axelor puede lanzar tras guardar.
      await botonAceptarAviso(page).waitFor({ timeout: 5000 }).catch(() => {});
      await cerrarAviso(page);
      await esperarListadoCargado(page);

      // La fila recién creada está en el listado (y es exactamente una).
      await expect(filasDelEscenario(page)).toHaveCount(1);
      await expect(filasDelEscenario(page).first()).toContainText(CENTRO);

      // Paso 5: Y abre la fila con centro «CIPFP Mislata», trámite «Anulación de
      //         matrícula en ciclo formativo», perfil «Auditor» y tipo de usuario
      //         «Administrativo».
      // Pasos 6 y 7: Y pulsa «Borrar» y confirma el borrado.
      await pulsarHasta(page, filasDelEscenario(page).first(), botonBorrar(page), 'la fila del escenario');
      await expect(campo(page, 'Trámite')).toHaveValue(TRAMITE);
      await expect(campo(page, 'Perfil')).toHaveValue(PERFIL);
      await expect(campo(page, 'Tipo usuario')).toHaveValue(TIPO_USUARIO);
      await botonBorrar(page).click();
      const dialogo = page.getByRole('dialog');
      await expect(dialogo.getByText('¿Realmente quieres eliminar el registro seleccionado?')).toBeVisible();
      await dialogo.getByRole('button', { name: /^(Eliminar|Delete)$/ }).click();
      await expect(page).toHaveURL(/AceProfileCentro-action\/list/);
      await esperarListadoCargado(page);

      // Resultado esperado: el listado ya no muestra ninguna fila con trámite
      // «Anulación de matrícula en ciclo formativo», perfil «Auditor» y tipo de
      // usuario «Administrativo».
      await expect(filasDelEscenario(page)).toHaveCount(0);
      // Y lo mismo tras una recarga en duro: el borrado llegó a la BD, no solo a
      // la rejilla en memoria de la SPA.
      await recargarListado(page);
      await expect(filasDelEscenario(page)).toHaveCount(0);
      creada = false;
    } finally {
      // Teardown: solo si ESTA ejecución creó la fila y una aserción falló antes
      // de borrarla. Best-effort (el `.catch`/`try` es intencional) para no
      // enmascarar el fallo real.
      if (creada) {
        try {
          await recargarListado(page);
          if ((await filasDelEscenario(page).count()) === 1) {
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
