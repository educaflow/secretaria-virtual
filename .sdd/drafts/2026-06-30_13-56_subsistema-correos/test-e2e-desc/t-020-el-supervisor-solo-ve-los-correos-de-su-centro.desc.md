---
type: test-e2e
id: T-020
---

# T-020 — El supervisor solo ve los correos de su centro

**Origen ESC:** ESC-006
**Verifica:** — (permiso `Correo.propio-centro-supervisor`, ver design.md paso 10)
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
1. **Dado** que el administrador ha iniciado sesión, pulsa "Nuevo correo", rellena el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Aviso Mislata», el cuerpo «texto», elige el centro «CIPFP Mislata» y pulsa "Guardar".
2. **Y** pulsa "Nuevo correo" de nuevo, rellena los mismos datos pero con el asunto «Aviso Batoi», elige el centro «CIPFP Batoi» y pulsa "Guardar".
3. **Y** cierra sesión.
4. **Cuando** el supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234».
5. **Y** abre la pantalla "Correos de mi centro".

## Resultado esperado
- El sistema muestra el correo «Aviso Mislata» y no muestra el correo «Aviso Batoi».
