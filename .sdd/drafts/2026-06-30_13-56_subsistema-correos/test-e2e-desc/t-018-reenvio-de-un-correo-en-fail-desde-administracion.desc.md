---
type: test-e2e
id: T-018
---

# T-018 — Reenvío de un correo en FAIL desde Administración

**Origen ESC:** ESC-004
**Verifica:** V-Correo-017, R-Correo-002, U-correos-administracion-formulario-001
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
1. **Dado** que el administrador pulsa "Nuevo correo" y rellena el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «destino-invalido@example.com», el asunto «Aviso», el cuerpo «texto» y elige el centro «CIPFP Mislata».
2. **Cuando** pulsa "Guardar".
3. **Y** recarga el listado de correos hasta que el correo «Aviso» aparezca en estado "Fallido".
4. **Y** abre el detalle del correo «Aviso», donde aparece el botón "Reenviar".
5. **Y** pulsa "Reenviar".

## Resultado esperado
- El correo permanece en estado FAIL, con el número de reintentos incrementado de 1 a 2 y la fecha del último intento actualizada.
