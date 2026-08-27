---
type: test-e2e
id: T-010
---

<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-06-30_13-56_subsistema-correos/test-e2e-desc/t-010-alta-con-adjunto-sin-nombre-de-fichero.desc.md
     Test: T-010  |  Origen ESC: ESC-016
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-010 — Alta con adjunto sin nombre de fichero

**Origen ESC:** ESC-016
**Verifica:** V-Adjunto-004, U-correos-administracion-formulario-adjunto-005
**Pantalla principal:** screen-correos-administracion.md
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
- El administrador ha iniciado sesión.

## Pasos
1. **Dado** que el administrador pulsa "Nuevo correo" y rellena el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Documento adjunto», el cuerpo «Adjunto el documento solicitado» y elige el centro «CIPFP Mislata».
2. **Cuando**, en el panel de adjuntos, pulsa "Añadir adjunto".
3. **Y** sube un fichero como contenido y deja vacío el nombre del fichero.
4. **Y** pulsa "Guardar" del adjunto.
5. **Y** pulsa "Guardar" en el correo.

## Resultado esperado
- El sistema muestra el mensaje "El nombre del fichero es obligatorio" y no crea el correo.
