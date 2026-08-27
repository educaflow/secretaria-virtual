---
type: test-e2e
id: T-025
---

<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-06-30_13-56_subsistema-correos/test-e2e-desc/t-025-el-destinatario-no-ve-un-correo-enviado-con-exito-a-otra-persona.desc.md
     Test: T-025  |  Origen ESC: ESC-019
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-025 — El destinatario no ve un correo enviado con éxito a otra persona

**Origen ESC:** ESC-019
**Verifica:** — (permiso `Correo.propio-destinatario`, condición `dniDestinatario = __user__.dni`)
**Pantalla principal:** screen-mis-correos.md
**Tipo:** error

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
1. **Dado** que el administrador ha iniciado sesión y crea un correo dirigido al DNI «03532821K», nombre «Alumno2», apellidos «CIPFP Mislata», «para» «alumno2@mislata.es», asunto «Aviso para Alumno2» y cuerpo «texto», en el centro «CIPFP Mislata», y cierra sesión.
2. **Cuando** el usuario «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
3. **Y** abre la pantalla "Mis correos".

## Resultado esperado
- El sistema no muestra el correo «Aviso para Alumno2», porque su DNI de destinatario no coincide con el del usuario.
