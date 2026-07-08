---
type: test-e2e
id: T-002
---

# T-002 — Alta de un correo con adjunto

**Origen ESC:** ESC-002
**Verifica:** V-Adjunto-004, V-Adjunto-005, U-correos-administracion-listado-adjuntos-001, U-correos-administracion-formulario-adjunto-001, R-Correo-001
**Pantalla principal:** screen-correos-administracion.md
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
- El administrador ha iniciado sesión.

## Pasos
1. **Dado** que el administrador está en la pantalla "Administración de correos" y pulsa "Nuevo correo".
2. **Cuando** rellena el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Documento adjunto», el cuerpo «Adjunto el documento solicitado» y elige el centro «CIPFP Mislata».
3. **Y**, en el panel de adjuntos, pulsa "Añadir adjunto".
4. **Y** rellena el nombre del fichero con «documento.pdf» y sube un fichero como contenido.
5. **Y** pulsa "Guardar" del adjunto.
6. **Y** pulsa "Guardar" en el correo.
7. **Entonces** el sistema crea el correo con un adjunto llamado «documento.pdf» asociado.

## Resultado esperado
- El correo «Documento adjunto» existe con un adjunto «documento.pdf» y se intenta su envío asíncrono.
