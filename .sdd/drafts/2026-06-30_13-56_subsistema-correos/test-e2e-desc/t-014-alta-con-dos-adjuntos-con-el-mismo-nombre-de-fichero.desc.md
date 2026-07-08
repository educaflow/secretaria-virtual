---
type: test-e2e
id: T-014
---

# T-014 — Alta con dos adjuntos con el mismo nombre de fichero

**Origen ESC:** ESC-022
**Verifica:** V-Adjunto-006
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
1. **Dado** que el administrador pulsa "Nuevo correo" y rellena el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Documento adjunto», el cuerpo «texto» y elige el centro «CIPFP Mislata».
2. **Cuando**, en el panel de adjuntos, pulsa "Añadir adjunto", rellena el nombre del fichero «documento.pdf», sube un fichero como contenido y pulsa "Guardar" del adjunto.
3. **Y** pulsa de nuevo "Añadir adjunto", rellena otra vez el nombre del fichero «documento.pdf», sube un fichero como contenido y pulsa "Guardar" del adjunto.
4. **Y** pulsa "Guardar" en el correo.

## Resultado esperado
- El sistema muestra el mensaje "Ya existe un adjunto con ese nombre en el correo" y no crea el correo.
