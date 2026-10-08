import { test, expect, Page, Locator } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-044 — Al abrir una fila se abre el formulario de su canal
// origen: ESC-035  |  verifica: U-notificaciones-todas-004, U-notificaciones-todas-023
// fuente: .sdd/drafts/2026-10-06_15-00_subsistema-notificaciones/test-e2e-desc/t-044-al-abrir-una-fila-se-abre-el-formulario-de-su-canal.desc.md

// Idempotencia: una notificación ya creada NO se puede modificar ni borrar (lo
// verifican T-046 y T-049), así que este test NO tiene teardown — excepción
// documentada del §4.5 del contrato. Para no colisionar con las notificaciones
// de runs anteriores (la BD compartida no se resetea), los motivos «Correo de
// prueba» y «SMS de prueba» llevan un sufijo único por ejecución y cada fila se
// localiza por su motivo.
const RUN = Date.now();
const MOTIVO_CORREO = `Correo de prueba t044-${RUN}`;
const MOTIVO_SMS = `SMS de prueba t044-${RUN}`;

// Correo de referencia.
const CORREO = {
  centro: 'CIPFP Mislata',
  dni: '86862719E',
  nombre: 'Alumno1',
  apellidos: 'CIPFP Mislata',
  para: 'alumno1@mislata.es',
  asunto: 'Reunión de inicio de curso',
  cuerpo: 'La reunión será el lunes a las 10:00.',
};

// SMS de referencia.
const SMS = {
  centro: 'CIPFP Mislata',
  dni: '86862719E',
  nombre: 'Alumno1',
  apellidos: 'CIPFP Mislata',
  telefono: '600111222',
  mensaje: 'Mañana no hay clase',
};
// El teléfono se guarda normalizado a formato internacional.
const TELEFONO_GUARDADO = '+34600111222';

// Escapa un literal para usarlo dentro de una RegExp.
const literal = (texto: string): string => texto.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

const EN_COPIA = /^En copia(?! oculta)/;
const EN_COPIA_OCULTA = /^En copia oculta/;

const fila = (page: Page, motivo: string): Locator => page.getByRole('row', { name: motivo });
const ventana = (page: Page, canal: 'Correo' | 'SMS'): Locator =>
  page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: canal, exact: true }) });

async function abrirTodas(page: Page): Promise<void> {
  await page.getByText('Notificaciones', { exact: true }).click();
  await page.getByText('Todas', { exact: true }).click();
  await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
}

async function elegirCanal(page: Page, canal: 'Correo' | 'SMS'): Promise<Locator> {
  await page.getByRole('button', { name: 'Nueva notificación' }).click();
  const eleccion = page.getByRole('dialog').filter({ has: page.getByRole('heading', { name: 'Elija el canal de la notificación' }) });
  await eleccion.getByRole('radio', { name: canal }).click();
  await eleccion.getByRole('button', { name: 'Continuar' }).click();
  return ventana(page, canal);
}

async function elegirCentro(page: Page, form: Locator, centro: string): Promise<void> {
  // El centro primero: es un many-to-one con autocompletado y su selección
  // re-renderiza el formulario.
  await form.getByRole('combobox', { name: 'Centro' }).fill(centro);
  await page.getByRole('option', { name: centro, exact: true }).click();
  await expect(form.getByRole('combobox', { name: 'Centro' })).toHaveValue(centro);
}

async function guardar(page: Page, form: Locator): Promise<void> {
  await form.getByRole('button', { name: 'Guardar' }).click();
  await expect(form).toBeHidden();
  await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
}

test.describe('Notificaciones — Todas', () => {
  test('Al abrir una fila se abre el formulario de su canal', async ({ page }) => {
    await ensureLoggedOut(page);
    // Paso 1: Dado que el administrador ha iniciado sesión y da de alta el correo
    //         de referencia con el motivo «Correo de prueba» y el SMS de
    //         referencia con el motivo «SMS de prueba».
    await login(page, 'admin', 'admin');
    await abrirTodas(page);

    const formCorreo = await elegirCanal(page, 'Correo');
    await expect(formCorreo.getByRole('region', { name: 'Datos del correo' })).toBeVisible();
    await elegirCentro(page, formCorreo, CORREO.centro);
    await formCorreo.getByRole('textbox', { name: 'Motivo' }).fill(MOTIVO_CORREO);
    await formCorreo.getByRole('textbox', { name: 'DNI del destinatario' }).fill(CORREO.dni);
    await formCorreo.getByRole('textbox', { name: 'Nombre', exact: true }).fill(CORREO.nombre);
    await formCorreo.getByRole('textbox', { name: 'Apellidos' }).fill(CORREO.apellidos);
    await formCorreo.getByRole('textbox', { name: /^Para/ }).fill(CORREO.para);
    await formCorreo.getByRole('textbox', { name: 'Asunto' }).fill(CORREO.asunto);
    await formCorreo.getByRole('textbox', { name: 'Cuerpo' }).fill(CORREO.cuerpo);
    await guardar(page, formCorreo);

    const formSms = await elegirCanal(page, 'SMS');
    await expect(formSms.getByRole('region', { name: 'Datos del SMS' })).toBeVisible();
    await elegirCentro(page, formSms, SMS.centro);
    await formSms.getByRole('textbox', { name: 'Motivo' }).fill(MOTIVO_SMS);
    await formSms.getByRole('textbox', { name: 'DNI del destinatario' }).fill(SMS.dni);
    await formSms.getByRole('textbox', { name: 'Nombre', exact: true }).fill(SMS.nombre);
    await formSms.getByRole('textbox', { name: 'Apellidos' }).fill(SMS.apellidos);
    // El widget «phone» de Axelor no asocia la etiqueta «Teléfono» a su input,
    // así que se localiza por su placeholder. Se teclea carácter a carácter
    // porque el widget formatea el número al vuelo.
    const telefono = formSms.getByPlaceholder('+34 123 456 789');
    await telefono.click();
    await telefono.pressSequentially(SMS.telefono);
    await expect(telefono).toHaveValue(/600\s*111\s*222/);
    await formSms.getByRole('textbox', { name: 'Mensaje' }).fill(SMS.mensaje);
    await guardar(page, formSms);

    // Paso 2: Cuando en «Todas» pulsa la fila «Correo de prueba».
    await expect(fila(page, MOTIVO_CORREO)).toBeVisible();
    await fila(page, MOTIVO_CORREO).click();

    // Paso 3: Entonces se abre el formulario del correo en solo lectura con tipo
    //         «Correo», motivo, centro, DNI, nombre, apellidos, «para», «en copia»
    //         y «en copia oculta» vacíos, asunto, cuerpo y el panel «Adjuntos»
    //         sin adjuntos.
    const consultaCorreo = ventana(page, 'Correo');
    const datosCorreo = consultaCorreo.getByRole('region', { name: 'Datos del correo' });
    await expect(datosCorreo).toBeVisible();
    await expect(datosCorreo.getByRole('radio', { name: 'Correo' })).toBeChecked();
    // Solo lectura: no hay botón «Guardar» y los campos están deshabilitados.
    await expect(consultaCorreo.getByRole('button', { name: 'Guardar' })).toHaveCount(0);
    const motivoCorreo = datosCorreo.getByRole('textbox', { name: /^Motivo/ });
    await expect(motivoCorreo).toHaveValue(MOTIVO_CORREO);
    await expect(motivoCorreo).toBeDisabled();
    // El centro, en solo lectura, se pinta como enlace-botón a su registro.
    await expect(datosCorreo.getByRole('button', { name: CORREO.centro, exact: true })).toBeVisible();
    await expect(datosCorreo.getByRole('combobox', { name: 'Centro' })).toHaveCount(0);
    const dniCorreo = datosCorreo.getByRole('textbox', { name: 'DNI del destinatario' });
    await expect(dniCorreo).toHaveValue(CORREO.dni);
    await expect(dniCorreo).toBeDisabled();
    const nombreCorreo = datosCorreo.getByRole('textbox', { name: 'Nombre', exact: true });
    await expect(nombreCorreo).toHaveValue(CORREO.nombre);
    await expect(nombreCorreo).toBeDisabled();
    const apellidosCorreo = datosCorreo.getByRole('textbox', { name: 'Apellidos' });
    await expect(apellidosCorreo).toHaveValue(CORREO.apellidos);
    await expect(apellidosCorreo).toBeDisabled();
    const para = datosCorreo.getByRole('textbox', { name: /^Para/ });
    await expect(para).toHaveValue(CORREO.para);
    await expect(para).toBeDisabled();
    const enCopia = datosCorreo.getByRole('textbox', { name: EN_COPIA });
    await expect(enCopia).toHaveValue('');
    await expect(enCopia).toBeDisabled();
    const enCopiaOculta = datosCorreo.getByRole('textbox', { name: EN_COPIA_OCULTA });
    await expect(enCopiaOculta).toHaveValue('');
    await expect(enCopiaOculta).toBeDisabled();
    const asunto = datosCorreo.getByRole('textbox', { name: 'Asunto' });
    await expect(asunto).toHaveValue(CORREO.asunto);
    await expect(asunto).toBeDisabled();
    // El cuerpo, en solo lectura, se pinta como texto tras su etiqueta, no como textbox.
    await expect(datosCorreo.getByRole('textbox', { name: 'Cuerpo' })).toHaveCount(0);
    await expect(datosCorreo).toContainText(new RegExp(`Cuerpo\\s*${literal(CORREO.cuerpo)}`));
    // Panel «Adjuntos» sin adjuntos: el grid solo tiene la fila de cabecera.
    const adjuntos = consultaCorreo.getByRole('region', { name: 'Adjuntos' });
    await expect(adjuntos).toBeVisible();
    await expect(adjuntos.getByRole('columnheader', { name: 'Nombre del fichero' })).toBeVisible();
    await expect(adjuntos.getByRole('row')).toHaveCount(1);
    await expect(adjuntos.getByRole('gridcell')).toHaveCount(0);

    // Paso 4: Cuando pulsa «Salir».
    await consultaCorreo.getByRole('button', { name: 'Salir' }).click();

    // Paso 5: Entonces vuelve al listado de «Todas».
    await expect(consultaCorreo).toBeHidden();
    await expect(page.getByRole('button', { name: 'Nueva notificación' })).toBeVisible();
    await expect(fila(page, MOTIVO_SMS)).toBeVisible();

    // Paso 6: Cuando pulsa la fila «SMS de prueba».
    await fila(page, MOTIVO_SMS).click();

    // Resultado esperado: se abre el formulario del SMS con teléfono
    // «+34600111222», mensaje «Mañana no hay clase» y motivo «SMS de prueba».
    const consultaSms = ventana(page, 'SMS');
    const datosSms = consultaSms.getByRole('region', { name: 'Datos del SMS' });
    await expect(datosSms).toBeVisible();
    await expect(datosSms.getByRole('radio', { name: 'SMS' })).toBeChecked();
    await expect(datosSms.getByRole('textbox', { name: /^Motivo/ })).toHaveValue(MOTIVO_SMS);
    // El teléfono, en solo lectura, se pinta como enlace «tel:» con el número
    // agrupado para leerlo; su destino es el valor guardado.
    const enlaceTelefono = datosSms.getByRole('link', { name: '+34 600 111 222' });
    await expect(enlaceTelefono).toBeVisible();
    await expect(enlaceTelefono).toHaveAttribute('href', `tel:${TELEFONO_GUARDADO}`);
    // El mensaje, en solo lectura, se pinta como texto tras su etiqueta.
    await expect(datosSms).toContainText(new RegExp(`Mensaje\\s*${literal(SMS.mensaje)}`));

    // El formulario se abre como ventana modal sobre el listado: se cierra antes
    // del logout para que el menú de usuario quede accesible.
    await consultaSms.getByRole('button', { name: 'Salir' }).click();
    await expect(consultaSms).toBeHidden();

    await logout(page);
  });
});
