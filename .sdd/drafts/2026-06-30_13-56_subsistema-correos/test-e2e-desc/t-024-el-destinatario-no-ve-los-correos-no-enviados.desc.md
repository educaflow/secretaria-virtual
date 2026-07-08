---
type: test-e2e
id: T-024
---

# T-024 — El destinatario no ve los correos no enviados

**Origen ESC:** ESC-009
**Verifica:** — (permiso `Correo.propio-destinatario`, condición `estado = 'SUCCESS'`)
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
1. **Dado** que el administrador ha iniciado sesión y crea un correo dirigido al DNI «86862719E», nombre «Alumno1», apellidos «CIPFP Mislata», «para» «destino-invalido@example.com», asunto «No entregado» y cuerpo «texto», y cierra sesión.
2. **Cuando** el usuario «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
3. **Y** abre la pantalla "Mis correos".

## Resultado esperado
- El sistema no muestra el correo «No entregado».
