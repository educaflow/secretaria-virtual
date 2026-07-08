---
type: test-e2e
id: T-023
---

<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-06-30_13-56_subsistema-correos/test-e2e-desc/t-023-el-destinatario-consulta-un-correo-enviado-con-exito-y-descarga-su-adjunto.desc.md
     Test: T-023  |  Origen ESC: ESC-008
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-023 — El destinatario consulta un correo enviado con éxito y descarga su adjunto

**Origen ESC:** ESC-008
**Verifica:** — (permiso `Correo.propio-destinatario`/`Adjunto.propio-destinatario`, ver design.md paso 10)
**Pantalla principal:** screen-mis-correos.md
**Tipo:** happy

## Estado inicial de la base de datos

Datos de demo ya precargados por otros subsistemas (gestión de centro), de los que parten **todos** los tests:

- Centros: «CIPFP Mislata» (código 46019660) y «CIPFP Batoi» (código 03012165).
- Usuario administrador global, con acceso a cualquier centro.
- Cuenta de supervisor de «CIPFP Mislata»: `supervisor1@mislata.es`.
- Cuentas de usuario con DNI: `alumno1@mislata.es` (DNI «86862719E», del centro «CIPFP Mislata») y `alumno2@mislata.es` (DNI «03532821K», del centro «CIPFP Mislata»).

**Usuarios de acceso**:

| Login | Contraseña | Rol / Tipo | Centro |
|---|---|---|---|
| admin | admin | Administrador | — (cualquier centro) |
| supervisor1@mislata.es | demo1234 | Supervisor | CIPFP Mislata |
| alumno1@mislata.es | demo1234 | Destinatario (DNI 86862719E) | CIPFP Mislata |
| alumno2@mislata.es | demo1234 | Destinatario (DNI 03532821K) | CIPFP Mislata |

## Precondiciones
- Ninguna más allá del "Estado inicial de la base de datos".

## Pasos
1. **Dado** que el administrador ha iniciado sesión y crea un correo dirigido al DNI «86862719E», nombre «Alumno1», apellidos «CIPFP Mislata», «para» «alumno1@mislata.es», asunto «Tu certificado», cuerpo «Adjunto tu certificado», un adjunto llamado «certificado.pdf» y centro «CIPFP Mislata», y cierra sesión.
2. **Cuando** el usuario «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
3. **Y** abre la pantalla "Mis correos".
4. **Y** pulsa la fila del correo «Tu certificado» para abrir su detalle.
5. **Y**, en el panel de adjuntos, descarga el adjunto «certificado.pdf».

## Resultado esperado
- El sistema muestra el correo «Tu certificado» en solo lectura, con su asunto, cuerpo y fecha de envío, y descarga el fichero «certificado.pdf».
