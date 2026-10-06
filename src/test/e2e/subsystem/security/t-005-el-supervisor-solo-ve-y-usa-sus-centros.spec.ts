import { test, expect, Locator, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-005 — El supervisor solo ve y usa sus centros
// origen: ESC-005  |  verifica: U-perfiles-tramites-mi-centro-001
// fuente: .sdd/drafts/2026-09-25_20-58_mantenimiento-perfiles-centro/test-e2e-desc/t-005-el-supervisor-solo-ve-y-usa-sus-centros.desc.md

// Valores de la fila que da de alta el administrador en «CIPFP Batoi». NO llevan
// sufijo `Date.now()` (excepción documentada de la regla de nombres únicos): los
// cuatro son valores de catálogo (centro, trámite, perfil del enum y cargo), no
// texto libre. La idempotencia se consigue con el teardown del `finally`, que
// (como administrador) borra la fila que ESTA ejecución ha creado.
//
// CRITICAL — NO hay pre-limpieza defensiva a propósito: el usuario NO autoriza
// borrar datos preexistentes de la BD. Si al arrancar ya hubiera una fila con
// estos valores, el test FALLA en la guarda de precondición en vez de borrarla;
// y el teardown solo borra la fila que haya creado ESTA ejecución (`creada`).
const CENTRO_AJENO = 'CIPFP Batoi';
const CENTRO_PROPIO = 'CIPFP Mislata';
const TRAMITE = 'Trámite de prueba';
const PERFIL = 'Director';
const CARGO = 'Director';

const URL_LISTADO_ADMIN = '/#/ds/subsysSecurity.Main%40AceProfileCentro-action/list/1';

// El nombre accesible de una fila del listado es «<centro> <trámite> <perfil>
// <tipo usuario> <cargo> <usuario>» (las celdas vacías no aportan texto).
const filasDelEscenario = (page: Page) =>
  page.getByRole('row', { name: `${CENTRO_AJENO} ${TRAMITE} ${PERFIL} ${CARGO}` });
const filasDeCentro = (page: Page, centro: string) => page.getByRole('row', { name: centro });
const filaSinRegistros = (page: Page) => page.getByRole('row', { name: 'No se encontraron registros.' });
const cabeceraListado = (page: Page) => page.getByRole('columnheader', { name: 'Trámite' });

const botonNuevo = (page: Page) => page.getByRole('button', { name: 'Nuevo' });
// «Borrar» solo existe en el formulario de un registro ya guardado.
const botonBorrar = (page: Page) => page.getByRole('button', { name: 'Borrar' });
const botonAceptarAviso = (page: Page) => page.getByRole('dialog').getByRole('button', { name: /^(OK|Aceptar)$/ });
const dialogoValidationError = (page: Page) =>
  page.getByRole('dialog').getByRole('heading', { name: 'La acción no se completó por los siguientes motivos' });

const campo = (page: Page, nombre: string) => page.getByRole('combobox', { name: nombre, exact: true });
const opciones = (page: Page) => page.getByRole('listbox');

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

// Abre `<menú> → <opción>` desde el menú lateral.
async function abrirMenu(page: Page, menu: string, opcion: string): Promise<void> {
  await page.getByText(menu, { exact: true }).click();
  await page.getByText(opcion, { exact: true }).click();
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
  await opciones(page).getByRole('option', { name: opcion, exact: true }).click();
  await expect(combo).toHaveValue(opcion);
}

// Recarga en duro el listado del administrador para leer el estado REAL de la BD.
async function recargarListadoAdmin(page: Page): Promise<void> {
  await page.goto(URL_LISTADO_ADMIN);
  await page.reload();
  await esperarListadoCargado(page);
}

// Abre la (única) fila del escenario y la borra confirmando el diálogo.
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
  test('El supervisor solo ve y usa sus centros', async ({ page }) => {
    // true en cuanto ESTA ejecución ha guardado la fila de «CIPFP Batoi»: es lo
    // único que el teardown puede borrar (nunca datos preexistentes).
    let creada = false;

    await ensureLoggedOut(page);
    // Paso 1: Dado que el administrador inicia sesión con usuario «admin» y
    //         contraseña «admin».
    await login(page, 'admin', 'admin');

    try {
      // Paso 2: Cuando abre «Administración → Perfiles de trámites por centro» …
      await abrirMenu(page, 'Administración', 'Perfiles de trámites por centro');

      // Guarda de precondición (sin borrar nada): no existe ya la fila del
      // escenario. Si la hubiera, el alta chocaría con la regla de asignación
      // repetida y no se sabría quién la creó.
      await expect(
        filasDelEscenario(page),
        `Ya existe una fila «${CENTRO_AJENO} / ${TRAMITE} / ${PERFIL} / ${CARGO}» antes de empezar; no se borra (dato preexistente)`,
      ).toHaveCount(0);

      // … y pulsa «Nuevo».
      await pulsarHasta(page, botonNuevo(page), campo(page, 'Trámite'), 'el botón «Nuevo»');

      // Paso 3: Y elige el centro «CIPFP Batoi», el trámite «Trámite de prueba»,
      //         el perfil «Director» y el cargo «Director», sin rellenar tipo de
      //         usuario ni usuario.
      await elegir(page, 'Centro', CENTRO_AJENO, 'Batoi');
      await elegir(page, 'Trámite', TRAMITE, 'prueba');
      // «Perfil» es un grupo de radios (widget RadioSelect), no un combobox; «Cargo»
      // sigue siendo un combobox, así que el radio «Director» es inequívoco.
      const radioPerfil = page.getByRole('radio', { name: PERFIL, exact: true });
      await radioPerfil.check();
      await expect(radioPerfil).toBeChecked();
      await elegir(page, 'Cargo', CARGO, 'Director');
      await expect(campo(page, 'Tipo usuario')).toHaveValue('');
      await expect(campo(page, 'Usuario')).toHaveValue('');

      // Paso 4: Y pulsa «Guardar».
      await page.getByRole('button', { name: 'Guardar' }).click();
      const error = dialogoValidationError(page);
      await error.or(botonNuevo(page)).first().waitFor();
      await expect(error).toHaveCount(0);
      await expect(page).toHaveURL(/AceProfileCentro-action\/list/);
      creada = true;
      // Drena el aviso con retardo que Axelor puede lanzar tras guardar.
      await botonAceptarAviso(page).waitFor({ timeout: 5000 }).catch(() => {});
      await cerrarAviso(page);
      // La fila de «CIPFP Batoi» llegó a la BD (recarga en duro): así el paso 8
      // prueba de verdad que el supervisor no la ve, no que no exista.
      await recargarListadoAdmin(page);
      await expect(filasDelEscenario(page)).toHaveCount(1);

      // Paso 5: Y el administrador cierra sesión.
      await logout(page);

      // Paso 6: Y el supervisor inicia sesión con usuario «supervisor1@mislata.es»
      //         y contraseña «demo1234».
      await login(page, 'supervisor1@mislata.es', 'demo1234');

      // Paso 7: Y abre «Mi centro → Perfiles de trámites».
      await abrirMenu(page, 'Mi centro', 'Perfiles de trámites');

      // Paso 8: Entonces el listado no muestra ninguna fila del centro «CIPFP Batoi».
      await expect(filasDeCentro(page, CENTRO_AJENO)).toHaveCount(0);

      // Paso 9: Cuando pulsa «Nuevo» y despliega el selector de centro.
      await pulsarHasta(page, botonNuevo(page), campo(page, 'Trámite'), 'el botón «Nuevo»');
      const centro = campo(page, 'Centro');
      await centro.click();
      await expect(opciones(page)).toBeVisible();

      // Resultado esperado: el selector de centro ofrece «CIPFP Mislata» y no
      // ofrece «CIPFP Batoi».
      await expect(opciones(page).getByRole('option', { name: CENTRO_PROPIO, exact: true })).toBeVisible();
      await expect(opciones(page).getByRole('option', { name: CENTRO_AJENO, exact: true })).toHaveCount(0);
      // Y lo mismo buscando «CIPFP», texto común a ambos centros: descarta que
      // «CIPFP Batoi» falte solo porque el buscador filtra por el valor prellenado.
      // Con la lista abierta el combobox pierde su nombre accesible: se cierra
      // antes de escribir en él.
      await page.keyboard.press('Escape');
      await expect(opciones(page)).toHaveCount(0);
      await centro.fill('CIPFP');
      await expect(opciones(page).getByRole('option', { name: CENTRO_PROPIO, exact: true })).toBeVisible();
      await expect(opciones(page).getByRole('option', { name: CENTRO_AJENO, exact: true })).toHaveCount(0);

      // El alta del supervisor no se guarda: se abandona con «Cancelar».
      await page.keyboard.press('Escape');
      await page.getByRole('button', { name: 'Cancelar' }).click();
      await cerrarAviso(page);
    } finally {
      // Teardown: como administrador, borra la fila que ESTA ejecución creó (y
      // solo esa). Best-effort (el `try/catch` es intencional) para no
      // enmascarar un fallo real.
      if (creada) {
        try {
          await ensureLoggedOut(page);
          await login(page, 'admin', 'admin');
          await recargarListadoAdmin(page);
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
