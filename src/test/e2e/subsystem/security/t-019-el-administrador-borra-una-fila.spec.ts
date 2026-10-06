import { test, expect, Locator, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-019 — El administrador borra una fila
// origen: ESC-012  |  verifica: —
// fuente: .sdd/drafts/2026-09-25_20-58_mantenimiento-perfiles-centro/test-e2e-desc/t-019-el-administrador-borra-una-fila.desc.md

// Valores de la fila del escenario. NO llevan sufijo `Date.now()` (excepción
// documentada de la regla de nombres únicos): los cuatro son valores de catálogo
// (centro, trámite, perfil del enum y cargo), no texto libre.
// La idempotencia se consigue porque el propio escenario BORRA la fila que crea,
// y el `finally` la borra si una aserción falló antes del borrado.
//
// CRITICAL — NO hay pre-limpieza defensiva a propósito: el usuario NO autoriza
// borrar datos preexistentes de la BD. Si al arrancar ya hubiera una fila con
// estos valores (residual de otro run o dato ajeno), el test FALLA en la guarda
// de precondición en vez de borrarla; y el teardown solo borra la fila que haya
// creado ESTA ejecución (`creada`).
const CENTRO = 'CIPFP Batoi';
const TRAMITE = 'Anulación de matrícula en ciclo formativo';
const PERFIL = 'Auditor';
const CARGO = 'Secretario';

const URL_LISTADO_ADMIN = '/#/ds/subsysSecurity.Main%40AceProfileCentro-action/list/1';

// El nombre accesible de una fila del listado es «<centro> <trámite> <perfil>
// <tipo usuario> <cargo> <usuario>» (las celdas vacías no aportan texto). Con el
// tipo de usuario vacío, perfil y cargo quedan contiguos. El listado del
// administrador mezcla centros, así que la fila se identifica también por el
// centro. El match es por subcadena: casa con CUALQUIER fila de ese centro,
// trámite, perfil y cargo, que es justo lo que pide el «Resultado esperado».
const filasDelEscenario = (page: Page) =>
  page.getByRole('row', { name: `${CENTRO} ${TRAMITE} ${PERFIL} ${CARGO}` });
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

// Recarga en duro el listado para leer el estado REAL de la BD, no la rejilla en
// memoria de la SPA.
async function recargarListado(page: Page): Promise<void> {
  await page.goto(URL_LISTADO_ADMIN);
  await page.reload();
  await esperarListadoCargado(page);
}

// Pulsa «Borrar» en el formulario abierto y confirma el diálogo de Axelor.
async function borrarYConfirmar(page: Page): Promise<void> {
  await botonBorrar(page).click();
  const dialogo = page.getByRole('dialog');
  await expect(dialogo.getByText('¿Realmente quieres eliminar el registro seleccionado?')).toBeVisible();
  await dialogo.getByRole('button', { name: /^(Eliminar|Delete)$/ }).click();
  await expect(page).toHaveURL(/AceProfileCentro-action\/list/);
  await esperarListadoCargado(page);
}

test.describe('Perfiles de trámites por centro (Administración)', () => {
  test('El administrador borra una fila', async ({ page }) => {
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
      // ninguna fila con estos valores. Si la hubiera, el alta chocaría con la
      // regla de asignación repetida y el test no probaría lo que dice probar.
      await expect(
        filasDelEscenario(page),
        `Ya existe una fila «${CENTRO} / ${TRAMITE} / ${PERFIL} / ${CARGO}» antes de empezar; no se borra (dato preexistente)`,
      ).toHaveCount(0);

      // … y pulsa «Nuevo».
      await pulsarHasta(page, botonNuevo(page), campo(page, 'Trámite'), 'el botón «Nuevo»');

      // Paso 3: Y elige el centro «CIPFP Batoi», el trámite «Anulación de
      //         matrícula en ciclo formativo», el perfil «Auditor» y el cargo
      //         «Secretario», y pulsa «Guardar».
      await elegir(page, 'Centro', CENTRO, 'Batoi');
      await elegir(page, 'Trámite', TRAMITE, 'Anulación');
      await page.getByRole('radio', { name: PERFIL, exact: true }).check();
      await elegir(page, 'Cargo', CARGO, 'Secret');
      await expect(campo(page, 'Tipo usuario')).toHaveValue('');
      await expect(campo(page, 'Usuario')).toHaveValue('');

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

      // Paso 4: Y vuelve al listado y abre la fila del centro «CIPFP Batoi» con
      //         trámite «Anulación de matrícula en ciclo formativo», perfil
      //         «Auditor» y cargo «Secretario».
      await expect(filasDelEscenario(page)).toHaveCount(1);
      await pulsarHasta(page, filasDelEscenario(page).first(), botonBorrar(page), 'la fila del escenario');
      await expect(campo(page, 'Trámite')).toHaveValue(TRAMITE);
      await expect(page.getByRole('radio', { name: PERFIL, exact: true })).toBeChecked();
      await expect(campo(page, 'Cargo')).toHaveValue(CARGO);

      // Paso 5: Y pulsa «Borrar».
      // Paso 6: Y confirma el borrado cuando el sistema pide confirmación.
      await borrarYConfirmar(page);

      // Resultado esperado: el listado ya no muestra ninguna fila del centro
      // «CIPFP Batoi» con trámite «Anulación de matrícula en ciclo formativo»,
      // perfil «Auditor» y cargo «Secretario».
      await expect(filasDelEscenario(page)).toHaveCount(0);
      // Y lo mismo tras una recarga en duro: el borrado llegó a la BD, no solo a
      // la rejilla en memoria de la SPA.
      await recargarListado(page);
      await expect(filasDelEscenario(page)).toHaveCount(0);
      creada = false;
    } finally {
      // Teardown: solo si ESTA ejecución creó la fila y una aserción falló antes
      // de borrarla. Best-effort (el `try/catch` es intencional) para no
      // enmascarar el fallo real.
      if (creada) {
        try {
          await recargarListado(page);
          if ((await filasDelEscenario(page).count()) === 1) {
            await pulsarHasta(page, filasDelEscenario(page).first(), botonBorrar(page), 'la fila del escenario');
            await borrarYConfirmar(page);
          }
        } catch {
          // limpieza best-effort.
        }
      }
    }

    await logout(page);
  });
});
