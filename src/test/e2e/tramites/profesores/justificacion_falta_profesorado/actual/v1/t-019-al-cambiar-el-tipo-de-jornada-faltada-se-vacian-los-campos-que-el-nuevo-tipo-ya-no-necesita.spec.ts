import { test, expect, Page } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../../../../_support/auth';

// T-019 — Al cambiar el tipo de jornada faltada se vacían los campos que el nuevo tipo ya no necesita
// origen: — (sin ESC)  |  CREADOR | ENTRADA/ENTRADA_DATOS --(sin evento)--> ENTRADA/ENTRADA_DATOS  |  tipo: happy
// fuente: .sdd/drafts/2026-09-22_16-01_justificacion-falta-profesorado-fechas/test-e2e-desc/t-019-al-cambiar-el-tipo-de-jornada-faltada-se-vacian-los-campos-que-el-nuevo-tipo-ya-no-necesita.desc.md

const TRAMITE = 'Justificación de falta del profesorado';
const TIPO_TRAMITE = 'Trámites para el profesor';

const PROFESOR = { login: 'director@mislata.es', password: 'demo1234' };

// El footer del expediente (un botón por evento disponible) lo pinta la plantilla común
// del tramitador; es el sitio donde se dispara la transición y donde se comprueba qué
// eventos ofrece el estado.
const FOOTER = 'panel:subsysExpedientes-template-footer-panel';

const TIPO_VARIOS_DIAS_PRIMERO_PARCIAL = 'Varios días pero del primer día solo faltó unas horas';
const TIPO_UN_DIA_COMPLETO = 'Un día completo';

/** Despliega los grupos del menú principal (de raíz a hoja) que haga falta y abre la entrada. */
async function abrirMenu(page: Page, entrada: string, ...grupos: string[]): Promise<void> {
  const item = page.getByTestId(`item:${entrada}`);
  // Cada grupo se pliega al pulsarlo: solo se despliega si la entrada no se ve ya.
  for (const grupo of grupos) {
    if (await item.isVisible()) {
      break;
    }
    await page.getByTestId(`item:${grupo}`).getByTestId('title').first().click();
  }
  await item.click();
}

/**
 * Abre la lista `entrada` del menú (desplegando sus `grupos`), la filtra por la columna
 * «Num. Exped.» y devuelve la fila del expediente cuyo número se pasa. Nunca «la primera
 * fila»: en una BD compartida que no se resetea, al segundo run hay varios expedientes del
 * mismo trámite y en el mismo estado.
 */
async function filtrarEnBandeja(page: Page, numero: string, entrada: string, ...grupos: string[]) {
  await abrirMenu(page, entrada, ...grupos);
  const filtro = page.getByTestId('column:numeroExpediente').getByPlaceholder('Buscar...');
  await filtro.fill(numero);
  await filtro.press('Enter');
  // El último rowgroup es el de los datos; el primero lleva la cabecera y la fila de filtros
  // (que también contiene el número tecleado y haría ambigua la búsqueda por rol).
  return page.getByRole('rowgroup').last().getByRole('row', { name: new RegExp(numero) });
}

/**
 * Abre desde una lista del menú el expediente cuyo número se pasa. Ninguna lista fija el
 * perfil: el servidor abre el expediente con el perfil real del usuario sobre él (el del
 * estado actual si lo ostenta; si no, el primero que tenga, con la vista genérica de solo
 * lectura). Lo que sí decide la lista es dónde está el expediente: «Mis trámites» para quien
 * lo registró (pendientes de mí / en tramitación / finalizados) y «Tramitación» para quien
 * lo tramita (pendientes de mí, o su unidad: abiertos / cerrados).
 */
async function abrirDesdeBandeja(page: Page, numero: string, entrada: string, ...grupos: string[]): Promise<void> {
  await (await filtrarEnBandeja(page, numero, entrada, ...grupos)).click();
  await expect(page.getByRole('tab', { name: new RegExp(numero) })).toBeVisible();
}

/**
 * Abre desde «Mis trámites» el expediente cuyo número se pasa, esté en la lista que esté:
 * lo usa el teardown, que no sabe en qué estado dejó el expediente el fallo de una aserción.
 */
async function abrirDesdeMisTramites(page: Page, numero: string): Promise<void> {
  const listas = ['misTramites-pendientesDeMi-menuitem', 'misTramites-enTramitacion-menuitem', 'misTramites-finalizados-menuitem'];
  for (const lista of listas) {
    const fila = await filtrarEnBandeja(page, numero, lista, 'misTramites-menuitem');
    const encontrado = await expect(fila).toHaveCount(1, { timeout: 5000 }).then(() => true, () => false);
    if (encontrado) {
      await fila.click();
      await expect(page.getByRole('tab', { name: new RegExp(numero) })).toBeVisible();
      return;
    }
  }
  throw new Error(`El expediente ${numero} no está en ninguna lista de «Mis trámites»`);
}

/**
 * Crea un expediente del trámite desde «Mis trámites» → «Nuevo trámite» (no hay botón
 * «Nuevo» de un grid: el alta la dispara el evento inicial del tipo de expediente) y
 * devuelve el NÚMERO que le ha asignado el servidor, que es lo único que identifica a lo
 * creado en una BD compartida que no se resetea.
 */
async function crearExpediente(page: Page): Promise<string> {
  await abrirMenu(page, 'misTramites-nuevoTramite-menuitem', 'misTramites-menuitem');

  // Árbol de trámites: los tipos de trámite (nivel 1) nacen plegados y sus trámites
  // (nivel 2) no existen en el DOM hasta desplegarlos.
  const arbol = page.getByTestId('panel:tramitesPanel');
  await arbol.locator('[role="row"][aria-level="1"]').filter({ hasText: TIPO_TRAMITE }).click();
  await arbol.locator('[role="row"][aria-level="2"]').filter({ hasText: TRAMITE }).click();

  await page.getByRole('button', { name: 'Crear expediente' }).click();

  // La aplicación abre el expediente en una pestaña titulada «<número>-<tipo de expediente>».
  const pestana = page.getByRole('tab').last();
  await expect(pestana).toContainText(new RegExp(`\\d{4,}/\\d{4}-${TRAMITE}`));
  const numero = (await pestana.textContent())!.split('-')[0].trim();
  expect(numero).toMatch(/^\d{4,}\/\d{4}$/);
  return numero;
}


/**
 * Fechas de la falta, relativas a hoy. El trámite solo admite faltas de los últimos 7 días, así
 * que una fecha fija caduca y el test empieza a fallar solo por el calendario. La de inicio es
 * hace 3 días y las de fin hace 2 y hace 1: el mismo tramo de días (10, 11 y 12/09/2026) que
 * describe el `.desc.md`, pero desplazado a la fecha en que corre el test.
 */
function diasAntes(dias: number): string {
  const fecha = new Date();
  fecha.setDate(fecha.getDate() - dias);
  const dd = String(fecha.getDate()).padStart(2, '0');
  const mm = String(fecha.getMonth() + 1).padStart(2, '0');
  return `${dd}/${mm}/${fecha.getFullYear()}`;
}
const FECHA_INICIO = diasAntes(3); // «10/09/2026» en la descripción
const FECHA_FIN_1 = diasAntes(2); // «11/09/2026» en la descripción
const FECHA_FIN_2 = diasAntes(1); // «12/09/2026» en la descripción

test.describe('Justificación de falta del profesorado — ENTRADA', () => {
  test('Al cambiar el tipo de jornada faltada se vacían los campos que el nuevo tipo ya no necesita', async ({
    page,
  }) => {
    let numero = '';
    try {
      // --- Tramo único: CREADOR (director@mislata.es) ---
      // Given: el profesor `director@mislata.es` (contraseña `demo1234`) ha iniciado sesión,
      // ha creado un expediente nuevo de «Justificación de falta del profesorado» y lo tiene
      // abierto en ENTRADA / ENTRADA_DATOS; …
      await ensureLoggedOut(page);
      await login(page, PROFESOR.login, PROFESOR.password);
      numero = await crearExpediente(page);
      await expect(page.getByLabel('Fase')).toHaveValue('Entrada');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Entrada de datos');

      // Given (cont.): … en el panel «Datos de la falta» ha elegido el tipo de jornada faltada
      // «Varios días pero del primer día solo faltó unas horas» …
      const panelFalta = page.getByRole('region', { name: 'Datos de la falta' });
      await panelFalta.getByRole('radio', { name: TIPO_VARIOS_DIAS_PRIMERO_PARCIAL, exact: true }).click();

      // Elegir el tipo repinta el panel desde el servidor: «Hora de inicio» y «Fecha de fin»
      // no existen hasta entonces. Hay que esperar a que estén, o el re-render llega después
      // del `fill` y se lleva por delante lo ya tecleado.
      await expect(panelFalta.getByRole('textbox', { name: 'Fecha de Inicio', exact: true })).toBeVisible();
      await expect(panelFalta.getByRole('textbox', { name: 'Hora de inicio', exact: true })).toBeVisible();
      await expect(panelFalta.getByRole('textbox', { name: 'Fecha de fin', exact: true })).toBeVisible();

      // Given (cont.): … y ha rellenado «Fecha de Inicio» con 10/09/2026, «Hora de inicio» con
      // 12:00 y «Fecha de fin» con 11/09/2026, sin pulsar «Siguiente».
      await panelFalta.getByRole('textbox', { name: 'Fecha de Inicio', exact: true }).fill(FECHA_INICIO);
      await panelFalta.getByRole('textbox', { name: 'Hora de inicio', exact: true }).fill('12:00');
      await panelFalta.getByRole('textbox', { name: 'Fecha de fin', exact: true }).fill(FECHA_FIN_1);

      // La precondición se comprueba ANTES del cambio de tipo: sin ella, las aserciones de que
      // los campos acaban VACÍOS pasarían igual aunque el `fill` no hubiera escrito nada, y el
      // test no probaría nada en absoluto.
      await expect(panelFalta.getByRole('textbox', { name: 'Fecha de Inicio', exact: true })).toHaveValue(
        FECHA_INICIO,
      );
      await expect(panelFalta.getByRole('textbox', { name: 'Hora de inicio', exact: true })).toHaveValue('12:00');
      await expect(panelFalta.getByRole('textbox', { name: 'Fecha de fin', exact: true })).toHaveValue(FECHA_FIN_1);

      // When: cambia el tipo de jornada faltada a «Un día completo».
      await panelFalta.getByRole('radio', { name: TIPO_UN_DIA_COMPLETO, exact: true }).click();

      // Then: el panel «Datos de la falta» … solo muestra «Fecha» (con el título «Fecha», no
      // «Fecha de Inicio»), que sigue mostrando 10/09/2026.
      // Se asierta PRIMERO la presencia del campo nuevo: así se espera al repintado real del
      // panel y las ausencias de más abajo no pueden pasar por un DOM aún a medio renderizar.
      await expect(panelFalta.getByRole('textbox', { name: 'Fecha', exact: true })).toBeVisible();
      await expect(panelFalta.getByRole('textbox', { name: 'Fecha', exact: true })).toHaveValue(FECHA_INICIO);

      // Then (cont.): el panel deja de mostrar «Fecha de fin» y «Hora de inicio» …
      await expect(panelFalta.getByRole('textbox', { name: 'Fecha de fin', exact: true })).toHaveCount(0);
      await expect(panelFalta.getByRole('textbox', { name: 'Hora de inicio', exact: true })).toHaveCount(0);
      // … y el campo de fecha ya no se titula «Fecha de Inicio», sino «Fecha».
      await expect(panelFalta.getByRole('textbox', { name: 'Fecha de Inicio', exact: true })).toHaveCount(0);

      // Then (cont.): el expediente **sigue** en ENTRADA / ENTRADA_DATOS …
      await expect(page.getByLabel('Fase')).toHaveValue('Entrada');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Entrada de datos');
      // (la parte «sin haberse guardado nada» se comprueba al final, releyendo el expediente
      // del servidor: es el único observable fiable — ver el bloque de cierre del test)

      // And: vuelve a elegir el tipo de jornada faltada «Varios días pero del primer día solo
      // faltó unas horas»: el panel vuelve a mostrar «Fecha de Inicio», «Hora de inicio» y
      // «Fecha de fin».
      await panelFalta.getByRole('radio', { name: TIPO_VARIOS_DIAS_PRIMERO_PARCIAL, exact: true }).click();
      await expect(panelFalta.getByRole('textbox', { name: 'Fecha de Inicio', exact: true })).toBeVisible();
      await expect(panelFalta.getByRole('textbox', { name: 'Hora de inicio', exact: true })).toBeVisible();
      await expect(panelFalta.getByRole('textbox', { name: 'Fecha de fin', exact: true })).toBeVisible();

      // And (cont.): «Fecha de Inicio» conserva 10/09/2026, pero «Hora de inicio» y «Fecha de
      // fin» aparecen **vacías** — no conservan los valores 12:00 y 11/09/2026 que tenían antes
      // del primer cambio.
      await expect(panelFalta.getByRole('textbox', { name: 'Fecha de Inicio', exact: true })).toHaveValue(
        FECHA_INICIO,
      );
      await expect(panelFalta.getByRole('textbox', { name: 'Hora de inicio', exact: true })).toHaveValue('');
      await expect(panelFalta.getByRole('textbox', { name: 'Fecha de fin', exact: true })).toHaveValue('');

      // Then (cont.): «… sin haberse guardado nada». Se comprueba releyendo el expediente del
      // SERVIDOR: se reabre desde la bandeja (lo que descarta el formulario en curso) y no debe
      // haber ni tipo de jornada elegido ni fecha alguna. El marcador «*» de cambios sin
      // guardar de la pestaña NO sirve como prueba: el `onChange` del tipo hace un viaje al
      // servidor que lo limpia, así que su ausencia no significa que se haya guardado.
      // Va al final para no alterar la secuencia de pasos de la descripción.
      await abrirDesdeBandeja(page, numero, 'misTramites-pendientesDeMi-menuitem', 'misTramites-menuitem');
      const panelReleido = page.getByRole('region', { name: 'Datos de la falta' });
      await expect(panelReleido.getByRole('radio', { name: TIPO_UN_DIA_COMPLETO, exact: true })).not.toBeChecked();
      await expect(
        panelReleido.getByRole('radio', { name: TIPO_VARIOS_DIAS_PRIMERO_PARCIAL, exact: true }),
      ).not.toBeChecked();
      await expect(panelReleido.getByRole('textbox', { name: 'Fecha de Inicio', exact: true })).toHaveValue('');
      // Y el expediente sigue donde estaba, sin que ningún evento lo haya movido.
      await expect(page.getByLabel('Fase')).toHaveValue('Entrada');
      await expect(page.getByLabel('Estado', { exact: true })).toHaveValue('Entrada de datos');
    } finally {
      // Teardown (§5.2): el test no dispara ningún evento, así que el expediente queda en
      // ENTRADA_DATOS, que sí ofrece «Borrar el expediente» (DELETE). El borrado exige SESIÓN
      // ABIERTA y el perfil del estado en el que queda el expediente (CREADOR), así que va
      // ANTES del `logout`, reautenticándose para que el teardown funcione sea cual sea el
      // punto en el que quedó la sesión. Los cambios del formulario nunca se guardaron: al
      // recargar la app se descartan sin pedir confirmación. DELETE recarga la aplicación
      // entera (refresh-app).
      // El `.catch(() => {})` es intencional: el teardown no debe enmascarar el fallo de una aserción.
      if (numero) {
        await (async () => {
          await ensureLoggedOut(page);
          await login(page, PROFESOR.login, PROFESOR.password);
          await abrirDesdeMisTramites(page, numero);
          await page.getByTestId(FOOTER).getByRole('button', { name: 'Borrar el expediente' }).click();
          await page.getByRole('dialog').getByRole('button', { name: 'Aceptar' }).click();
          // Se comprueba por el NÚMERO del expediente que este test creó, nunca por «el primero».
          await expect(page.getByRole('tab', { name: new RegExp(numero) })).toHaveCount(0);
        })().catch(() => {});
      }
      await logout(page).catch(() => {});
    }
  });
});
