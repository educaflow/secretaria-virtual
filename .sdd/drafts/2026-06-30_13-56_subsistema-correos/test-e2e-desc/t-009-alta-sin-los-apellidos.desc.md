---
type: test-e2e
id: T-009
---

# T-009 — Alta sin los apellidos

**Origen ESC:** ESC-015
**Verifica:** V-Correo-004
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
1. **Dado** que el administrador pulsa "Nuevo correo".
2. **Cuando** rellena el DNI «86862719E», el nombre «Alumno1», el «para» «alumno1@mislata.es», el asunto «Convocatoria de reunión», el cuerpo «texto», elige el centro «CIPFP Mislata» y deja vacíos los apellidos.
3. **Y** pulsa "Guardar".

## Resultado esperado
- El sistema muestra el mensaje "Los apellidos son obligatorios" y no crea el correo.
