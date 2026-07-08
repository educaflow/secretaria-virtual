---
type: test-e2e
id: T-001
---

# T-001 — Alta de un correo que se envía con éxito

**Origen ESC:** ESC-001
**Verifica:** V-Correo-001, V-Correo-003, V-Correo-004, V-Correo-005, V-Correo-009, V-Correo-011, V-Correo-012, R-Correo-001, R-Correo-003, R-Correo-004, U-correos-administracion-formulario-002, U-correos-administracion-formulario-005, U-correos-administracion-formulario-008
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
- El administrador ha iniciado sesión (usuario «admin», contraseña «admin»).

## Pasos
1. **Dado** que el administrador está en la pantalla "Administración de correos".
2. **Cuando** pulsa "Nuevo correo".
3. **Y** rellena el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Convocatoria de reunión», el cuerpo «Le esperamos el lunes a las 9:00» y elige el centro «CIPFP Mislata».
4. **Y** pulsa "Guardar".
5. **Entonces** el sistema crea el correo y lo muestra en estado "Pendiente" (o ya "Enviado" si el envío asíncrono ha terminado), con la fecha de creación registrada.
6. **Y**, recargando el detalle del correo poco después, el correo aparece en estado "Enviado" ("SUCCESS"), con la fecha de envío rellena y el botón "Reenviar" ausente.

## Resultado esperado
- El correo «Convocatoria de reunión» queda en estado SUCCESS, con fecha de envío registrada y número de reintentos igual a 1.
