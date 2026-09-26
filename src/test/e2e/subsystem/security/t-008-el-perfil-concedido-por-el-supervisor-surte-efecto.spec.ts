import { test, expect, Locator, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-008 — El perfil concedido por el supervisor surte efecto
// origen: ESC-022  |  verifica: —
// fuente: .sdd/drafts/2026-09-25_20-58_mantenimiento-perfiles-centro/test-e2e-desc/t-008-el-perfil-concedido-por-el-supervisor-surte-efecto.desc.md

// Valores de la fila del escenario. NO llevan sufijo `Date.now()` (excepción
// documentada de la regla de nombres únicos): los cuatro son valores de catálogo
// (centro, trámite, perfil del enum y usuario de demo), no texto libre.
// La idempotencia se consigue con el teardown del `finally`, que borra la fila
// que ESTA ejecución ha creado (haya fallado o no una aserción), devolviendo al
// profesor a su estado inicial (sin perfil sobre el trámite).
//
// CRITICAL — NO hay pre-limpieza defensiva a propósito: el usuario NO autoriza
// borrar datos preexistentes de la BD. Si al arrancar ya hubiera una fila con
// estos valores (residual de otro run o dato ajeno), el test FALLA en la guarda
// de precondición (el paso 3 o la guarda del listado) en vez de borrarla.
const CENTRO = 'CIPFP Mislata';
const TRAMITE = 'Anulación de matrícula en ciclo formativo';
const PERFIL = 'Creador';
const USUARIO = 'Profesor1 CIPFP Mislata';

const PROFESOR = 'profesor1@mislata.es';
const SUPERVISOR = 'supervisor1@mislata.es';
const CONTRASENA = 'demo1234';

const URL_LISTADO = '/#/ds/subsysSecurity.Centro%40AceProfileCentro-action/list/1';

// ---------------------------------------------------------------------------
// Asistente «Mis trámites → Nuevo trámite» (ventanilla)
// ---------------------------------------------------------------------------

const PANTALLA_TRAMITE = 'Nuevo expediente: elija el trámite';

// El árbol de trámites es un `treegrid` sin testids propios en sus nodos:
// `aria-level="1"` son los grupos (tipo de trámite), `aria-level="2"` los trámites.
const gruposDeTipoTramite = (page: Page) =>
  page.getByTestId('panel:tramitesPanel').locator('[role="row"][aria-level="1"]');
const filasDeTramites = (page: Page) =>
  page.getByTestId('panel:tramitesPanel').locator('[role="row"][aria-level="2"]');

// Abre «Mis trámites → Nuevo trámite». El grupo del menú se pliega/despliega al
// pulsarlo, así que solo se despliega si la entrada no se ve.
async function abrirNuevoTramite(page: Page): Promise<void> {
  const entrada = page.getByTestId('item:misTramites-nuevoTramite-menuitem');
  if (!(await entrada.isVisible())) {
    await page.getByTestId('item:misTramites-menuitem').getByTestId('title').first().click();
  }
  await entrada.click();
}

// Elige el centro «CIPFP Mislata». Profesor1 CIPFP Mislata tiene un único centro,
// así que el asistente salta la pantalla de elegir centro y lo deja fijado; si
// algún día tuviera varios, se elige en esa pantalla. En ambos casos se comprueba
// que la pantalla de elegir trámite queda con ese centro.
async function elegirCentro(page: Page): Promise<void> {
  // Se decide por el LISTADO de centros y no por la pestaña: la pestaña «elija el
  // centro» aparece un instante y el asistente salta solo a «elija el trámite»
  // cuando el usuario tiene un único centro.
  const tabTramite = page.getByRole('tab', { name: PANTALLA_TRAMITE, exact: true });
  const opcionCentro = page.getByTestId('panel:centrosPanel').getByText(CENTRO, { exact: true });
  await tabTramite.or(opcionCentro).first().waitFor();
  if (await opcionCentro.isVisible().catch(() => false)) {
    await opcionCentro.click();
  }
  await expect(tabTramite).toBeVisible();
  await expect(page.getByTestId('field:centro').getByRole('textbox')).toHaveValue(CENTRO);
}

// Despliega todos los grupos de tipo de trámite: el `<tree>` nace plegado y sus
// trámites no existen en el DOM hasta desplegarlos, así que sin esto una aserción
// de ausencia pasaría en falso.
async function desplegarGruposDeTramites(page: Page): Promise<void> {
  const grupos = gruposDeTipoTramite(page);
  await grupos.first().waitFor();
  const total = await grupos.count();
  for (let i = 0; i < total; i++) {
    const grupo = grupos.nth(i);
    if ((await grupo.getAttribute('aria-expanded')) !== 'true') {
      await grupo.click();
    }
  }
  // Barrera: tras desplegar, al menos un trámite es visible (el profesor siempre
  // tiene los «Trámites para el profesor»).
  await expect(filasDeTramites(page).first()).toBeVisible();
}

// Cierra el asistente con su botón «Cancelar» (best-effort: si no está abierto,
// el `.catch` es intencional).
async function cancelarAsistente(page: Page): Promise<void> {
  await page
    .getByTestId('panel:buttons-panel')
    .getByRole('button', { name: 'Cancelar' })
    .click({ timeout: 5000 })
    .catch(() => {});
}

// ---------------------------------------------------------------------------
// «Mi centro → Perfiles de trámites» (mismos helpers que el hermano T-002)
// ---------------------------------------------------------------------------

// El nombre accesible de una fila del listado es «<centro> <trámite> <perfil>
// <tipo usuario> <cargo> <usuario>» (las celdas vacías no aportan texto).
const filasDelEscenario = (page: Page) =>
  page.getByRole('row', { name: `${TRAMITE} ${PERFIL} ${USUARIO}` });
const filaSinRegistros = (page: Page) => page.getByRole('row', { name: 'No se encontraron registros.' });
const cabeceraListado = (page: Page) => page.getByRole('columnheader', { name: 'Trámite' });

const botonNuevo = (page: Page) => page.getByRole('button', { name: 'Nuevo' });
const botonBorrar = (page: Page) => page.getByRole('button', { name: 'Borrar' });
const botonAceptarAviso = (page: Page) => page.getByRole('dialog').getByRole('button', { name: /^(OK|Aceptar)$/ });
const dialogoValidationError = (page: Page) =>
  page.getByRole('dialog').getByRole('heading', { name: 'La acción no se completó por los siguientes motivos' });

const campo = (page: Page, nombre: string) => page.getByRole('combobox', { name: nombre, exact: true });

async function esperarListadoCargado(page: Page): Promise<void> {
  await expect(botonNuevo(page)).toBeVisible();
  await expect(cabeceraListado(page)).toBeVisible();
  const filaDeDatos = page.getByRole('row', { name: CENTRO });
  await expect(filaDeDatos.or(filaSinRegistros(page)).first()).toBeVisible();
}

async function cerrarAviso(page: Page): Promise<void> {
  const ok = botonAceptarAviso(page);
  if (await ok.isVisible().catch(() => false)) {
    await ok.click();
  }
}

// Pulsa `destino` hasta que el clic surte efecto (`resultado` visible), tolerando
// el aviso asíncrono «Current changes will be lost» de Axelor.
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

async function abrirPerfilesDeTramites(page: Page): Promise<void> {
  await page.getByText('Mi centro', { exact: true }).click();
  await page.getByText('Perfiles de trámites', { exact: true }).click();
  await esperarListadoCargado(page);
}

// Elige `opcion` en un combobox de Axelor tecleando `texto` carácter a carácter
// (con `fill` los many-to-one no filtran) o abriéndolo si no hay texto.
async function elegir(page: Page, nombreCampo: string, opcion: string, texto?: string): Promise<void> {
  const combo = campo(page, nombreCampo);
  if (texto !== undefined) {
    await combo.fill('');
    await combo.pressSequentially(texto, { delay: 50 });
  } else {
    await combo.click();
  }
  await page.getByRole('listbox').getByRole('option', { name: opcion, exact: true }).click();
  await expect(combo).toHaveValue(opcion);
}

async function recargarListado(page: Page): Promise<void> {
  await page.goto(URL_LISTADO);
  await page.reload();
  await esperarListadoCargado(page);
}

async function celda(page: Page, fila: Locator, columna: string): Promise<Locator> {
  const cabeceras = await page.getByRole('columnheader').allInnerTexts();
  const indice = cabeceras.findIndex((t) => t.split('\n')[0].trim() === columna);
  expect(indice, `No hay columna «${columna}» en el listado (cabeceras: ${cabeceras.join(' | ')})`).toBeGreaterThanOrEqual(0);
  return fila.getByRole('gridcell').nth(indice);
}

async function abrirYBorrarFila(page: Page): Promise<void> {
  await pulsarHasta(page, filasDelEscenario(page).first(), botonBorrar(page), 'la fila del escenario');
  await botonBorrar(page).click();
  const dialogo = page.getByRole('dialog');
  await expect(dialogo.getByText('¿Realmente quieres eliminar el registro seleccionado?')).toBeVisible();
  await dialogo.getByRole('button', { name: /^(Eliminar|Delete)$/ }).click();
  await expect(page).toHaveURL(/AceProfileCentro-action\/list/);
  await esperarListadoCargado(page);
}

test.describe('Perfiles de trámites (Mi centro) — efecto sobre Nuevo trámite', () => {
  test('El perfil concedido por el supervisor surte efecto', async ({ page }) => {
    // true en cuanto ESTA ejecución ha guardado la fila del escenario: es lo único
    // que el teardown puede borrar (nunca datos preexistentes).
    let creada = false;

    await ensureLoggedOut(page);

    try {
      // Paso 1: Dado que el profesor inicia sesión con usuario «profesor1@mislata.es»
      //         y contraseña «demo1234».
      await login(page, PROFESOR, CONTRASENA);

      // Paso 2: Cuando abre «Mis trámites → Nuevo trámite» y elige el centro
      //         «CIPFP Mislata».
      await abrirNuevoTramite(page);
      await elegirCentro(page);

      // Paso 3: Entonces el sistema no ofrece el trámite «Anulación de matrícula en
      //         ciclo formativo» (con todos los grupos desplegados, para que la
      //         ausencia sea real y no por estar plegado).
      await desplegarGruposDeTramites(page);
      await expect(filasDeTramites(page).filter({ hasText: TRAMITE })).toHaveCount(0);
      await cancelarAsistente(page);

      // Paso 4: Cuando el profesor cierra sesión.
      await logout(page);

      // Paso 5: Y el supervisor inicia sesión con usuario «supervisor1@mislata.es» y
      //         contraseña «demo1234».
      await login(page, SUPERVISOR, CONTRASENA);

      // Paso 6: Y abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
      await abrirPerfilesDeTramites(page);
      // Guarda de precondición (sin borrar nada): no existe ya la fila del escenario.
      await expect(
        filasDelEscenario(page),
        `Ya existe una fila «${TRAMITE} / ${PERFIL} / ${USUARIO}» antes de empezar; no se borra (dato preexistente)`,
      ).toHaveCount(0);
      await pulsarHasta(page, botonNuevo(page), campo(page, 'Trámite'), 'el botón «Nuevo»');

      // Paso 7: Y elige el centro «CIPFP Mislata», el trámite «Anulación de matrícula
      //         en ciclo formativo», el perfil «Creador» y el usuario «Profesor1 CIPFP
      //         Mislata», sin rellenar tipo de usuario ni cargo.
      // El centro sale prellenado con el del supervisor; solo se elige si no lo está.
      const centro = campo(page, 'Centro');
      if ((await centro.inputValue()) !== CENTRO) {
        await elegir(page, 'Centro', CENTRO, 'Mislata');
      }
      await expect(centro).toHaveValue(CENTRO);
      await elegir(page, 'Trámite', TRAMITE, 'Anulación');
      await elegir(page, 'Perfil', PERFIL);
      await elegir(page, 'Usuario', USUARIO, 'Profesor1 C');
      await expect(campo(page, 'Tipo usuario')).toHaveValue('');
      await expect(campo(page, 'Cargo')).toHaveValue('');

      // Paso 8: Y pulsa «Guardar» y vuelve al listado.
      await page.getByRole('button', { name: 'Guardar' }).click();
      const error = dialogoValidationError(page);
      await error.or(botonNuevo(page)).first().waitFor();
      await expect(error).toHaveCount(0);
      await expect(page).toHaveURL(/AceProfileCentro-action\/list/);
      creada = true;
      await botonAceptarAviso(page).waitFor({ timeout: 5000 }).catch(() => {});
      await cerrarAviso(page);
      await esperarListadoCargado(page);

      // Paso 9: Entonces el listado muestra una fila con centro «CIPFP Mislata»,
      //         trámite «Anulación de matrícula en ciclo formativo», perfil «Creador»
      //         y usuario «Profesor1 CIPFP Mislata» (tras recarga en duro: está en BD).
      await recargarListado(page);
      await expect(filasDelEscenario(page)).toHaveCount(1);
      const fila = filasDelEscenario(page).first();
      await expect(await celda(page, fila, 'Centro')).toHaveText(CENTRO);
      await expect(await celda(page, fila, 'Trámite')).toHaveText(TRAMITE);
      await expect(await celda(page, fila, 'Perfil')).toHaveText(PERFIL);
      await expect(await celda(page, fila, 'Usuario')).toHaveText(USUARIO);

      // Paso 10: Cuando el supervisor cierra sesión.
      await logout(page);

      // Paso 11: Y el profesor inicia sesión con usuario «profesor1@mislata.es» y
      //          contraseña «demo1234».
      await login(page, PROFESOR, CONTRASENA);

      // Paso 12: Y abre «Mis trámites → Nuevo trámite» y elige el centro
      //          «CIPFP Mislata».
      await abrirNuevoTramite(page);
      await elegirCentro(page);

      // Resultado esperado: el sistema ofrece el trámite «Anulación de matrícula en
      // ciclo formativo».
      await desplegarGruposDeTramites(page);
      await expect(filasDeTramites(page).filter({ hasText: TRAMITE })).toHaveCount(1);
      await expect(filasDeTramites(page).filter({ hasText: TRAMITE })).toBeVisible();
      await cancelarAsistente(page);
    } finally {
      // Teardown: el supervisor borra la fila que ESTA ejecución creó (y solo esa),
      // aunque una aserción haya fallado. Best-effort (el `try/catch` es
      // intencional) para no enmascarar el fallo real.
      if (creada) {
        try {
          await cancelarAsistente(page);
          await ensureLoggedOut(page);
          await login(page, SUPERVISOR, CONTRASENA);
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
