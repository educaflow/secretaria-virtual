import { test, expect } from '@playwright/test';
import { ensureLoggedOut, login, logout } from '../../_support/auth';

// T-025 — El destinatario no ve un correo enviado con éxito a otra persona
// origen: ESC-019  |  verifica: — (permiso `Correo.propio-destinatario`, condición `dniDestinatario = __user__.dni`)
// fuente: .sdd/drafts/2026-06-30_13-56_subsistema-correos/test-e2e-desc/t-025-el-destinatario-no-ve-un-correo-enviado-con-exito-a-otra-persona.desc.md
test.describe('Mis correos', () => {
  test('El destinatario no ve un correo enviado con éxito a otra persona', async ({ page }) => {
    await ensureLoggedOut(page);
    await login(page, 'admin', 'admin');

    // Idempotencia (BD compartida, sin reset): el asunto lleva un sufijo único por
    // ejecución para poder identificar SIEMPRE la fila que crea este run, incluso si
    // hay correos de ejecuciones anteriores en la BD.
    const asunto = `Aviso para Alumno2 t025-${Date.now()}`;

    // Paso 1: Dado que el administrador ha iniciado sesión y crea un correo dirigido
    // al DNI «03532821K», nombre «Alumno2», apellidos «CIPFP Mislata», «para»
    // «alumno2@mislata.es», asunto «Aviso para Alumno2» y cuerpo «texto», en el
    // centro «CIPFP Mislata», y cierra sesión.
    await page.getByText('Correos', { exact: true }).click();
    await page.getByText('Administración de correos', { exact: true }).click();
    await page.getByRole('button', { name: 'Nuevo correo' }).click();

    await page.getByLabel('DNI del destinatario').fill('03532821K');
    await page.getByLabel('Nombre', { exact: true }).fill('Alumno2');
    await page.getByLabel('Apellidos').fill('CIPFP Mislata');
    // El label real incluye un icono de ayuda ("Para ?"), de ahí el prefijo con regex.
    await page.getByLabel(/^Para\b/).fill('alumno2@mislata.es');
    await page.getByLabel('Asunto').fill(asunto);
    await page.getByLabel('Cuerpo').fill('texto');
    await page.getByLabel('Centro', { exact: true }).click();
    await page.getByRole('option', { name: 'CIPFP Mislata' }).click();

    const row = page.getByRole('row', { name: asunto });
    await expect(page.getByRole('button', { name: 'Guardar' })).toBeVisible();
    await page.getByRole('button', { name: 'Guardar' }).click();
    await expect(row).toBeVisible();

    // Antes de cambiar de usuario, confirma que el envío asíncrono ha terminado en
    // SUCCESS ("Enviado"): si se comprobara la ausencia mientras el correo aún
    // estuviera "Pendiente", un "no aparece" no demostraría el filtro de permisos —
    // podría deberse solo a que el envío aún no ha terminado. Igual que en T-001.
    await row.click();
    await page.waitForURL(/\/edit\/\d+/);
    await expect(async () => {
      await page.reload();
      // El campo "Estado" es un <input role="combobox"> de solo lectura: su valor
      // vive en el atributo value, no como texto — de ahí toHaveValue.
      await expect(page.getByRole('combobox', { name: 'Estado' })).toHaveValue('Enviado');
    }).toPass({ timeout: 60_000, intervals: [2_000] });
    await page.getByRole('button', { name: 'Salir' }).click();

    await logout(page);

    // Paso 2: Cuando el usuario «alumno1@mislata.es» inicia sesión con contraseña
    // «demo1234».
    await login(page, 'alumno1@mislata.es', 'demo1234');

    // Paso 3: Y abre la pantalla "Mis correos".
    // El menú "Mis correos" tiene un único submenú con el mismo texto (no es un rol
    // "menuitem", es un elemento genérico, igual que el patrón de "Correos" →
    // "Administración de correos"), de ahí el índice para distinguir el elemento de
    // menú superior del submenú.
    await page.getByText('Mis correos', { exact: true }).nth(0).click();
    await page.getByText('Mis correos', { exact: true }).nth(1).click();
    await page.waitForURL(/Correo%40Mis/);

    // Resultado esperado: el sistema no muestra el correo «Aviso para Alumno2»,
    // porque su DNI de destinatario no coincide con el del usuario. El envío ya se
    // ha confirmado en SUCCESS arriba, así que esta ausencia prueba el filtro de
    // permisos y no una simple demora de entrega.
    await expect(page.getByRole('row', { name: asunto })).toHaveCount(0);

    await logout(page);

    // Teardown: el correo creado por el administrador queda enviado (Enviado) y no
    // se puede borrar desde la UI una vez que el envío asíncrono se ha intentado (el
    // formulario de consulta de "Administración de correos" solo ofrece
    // "Reenviar"/"Salir" — es un dato inmutable de auditoría, igual que en
    // T-001/T-019/T-020/T-022/T-023). Se confía en el asunto único para no colisionar
    // entre ejecuciones; no es un error tapado, es la excepción documentada por el
    // contrato (§4.5).
  });
});
