---
type: test-e2e
id: T-022
---

<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-06-30_13-56_subsistema-correos/test-e2e-desc/t-022-el-supervisor-descarga-el-adjunto-de-un-correo-de-su-centro.desc.md
     Test: T-022  |  Origen ESC: ESC-018
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-022 — El supervisor descarga el adjunto de un correo de su centro

**Origen ESC:** ESC-018
**Verifica:** U-correos-administracion-formulario-adjunto-007
**Pantalla principal:** screen-correos-centro.md
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
1. **Dado** que el administrador ha iniciado sesión, pulsa "Nuevo correo", rellena el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Circular con adjunto», el cuerpo «texto» y elige el centro «CIPFP Mislata».
2. **Y**, en el panel de adjuntos, pulsa "Añadir adjunto", rellena el nombre del fichero «circular.pdf», sube un fichero como contenido y pulsa "Guardar" del adjunto.
3. **Y** pulsa "Guardar" en el correo y cierra sesión.
4. **Cuando** el supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234» y abre la pantalla "Correos de mi centro".
5. **Y** recarga el listado hasta ver el correo «Circular con adjunto» en estado "Enviado" y abre su detalle; en el panel de adjuntos aparece «circular.pdf».
6. **Y** pulsa la fila del adjunto «circular.pdf» para abrir su formulario de detalle.
7. **Y** descarga el fichero del campo contenido.

## Resultado esperado
- El sistema descarga el fichero «circular.pdf».
