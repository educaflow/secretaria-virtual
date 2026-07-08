---
type: test-e2e
id: T-021
---

# T-021 — El supervisor reintenta un correo fallido de su centro

**Origen ESC:** ESC-007
**Verifica:** V-Correo-018, R-Correo-002, U-correos-centro-formulario-001, U-correos-centro-formulario-002, U-correos-centro-formulario-004
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
1. **Dado** que el administrador ha iniciado sesión, pulsa "Nuevo correo", rellena el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «destino-invalido@example.com», el asunto «Aviso», el cuerpo «texto», elige el centro «CIPFP Mislata» y pulsa "Guardar".
2. **Y** cierra sesión.
3. **Cuando** el supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234» y abre la pantalla "Correos de mi centro".
4. **Y** recarga el listado hasta ver el correo «Aviso» en estado "Fallido" y abre su detalle, donde ve la descripción del fallo, las fechas de intento y el botón "Reenviar".
5. **Y** pulsa "Reenviar".

## Resultado esperado
- El sistema muestra un aviso breve de que el reenvío está en curso; el correo permanece en estado FAIL con el número de reintentos incrementado de 1 a 2.
