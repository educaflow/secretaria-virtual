---
type: test-e2e
id: T-026
---

<!-- Escrito a mano (no procede de /sdd-create-tests-e2e) al retirar `User.centroActivo`:
     «Correos de mis centros» ya no filtra por un centro activo sino por todos los centros del usuario. -->

# T-026 — El supervisor de dos centros ve los correos de ambos

**Verifica:** — (dominio de `subsysCorreos.Centro@Correo-action` y permiso `Correo.propio-centro-supervisor`)
**Pantalla principal:** Correos de mis centros
**Tipo:** happy

## Estado inicial de la base de datos

Datos de demo ya precargados por otros subsistemas (gestión de centro), de los que parten **todos** los tests:

- Centros: «CIPFP Mislata» (código 46019660) y «CIPFP Batoi» (código 03012165).
- Usuario administrador global, con acceso a cualquier centro.
- Cuenta `supervisordoscentros@mislata.es`, con el tipo de usuario SUPERVISOR en «CIPFP Mislata» y en «CIPFP Batoi».
- Cuenta de usuario con DNI: `alumno1@mislata.es` (DNI «95591733F», del centro «CIPFP Mislata»).

**Usuarios de acceso**:

| Login | Contraseña | Rol / Tipo | Centro |
|---|---|---|---|
| admin | admin | Administrador | — (cualquier centro) |
| supervisordoscentros@mislata.es | demo1234 | Supervisor | CIPFP Mislata y CIPFP Batoi |

## Precondiciones
- Ninguna más allá del "Estado inicial de la base de datos".

## Pasos
1. **Dado** que el administrador ha iniciado sesión, pulsa "Nuevo correo", rellena el DNI «95591733F», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «alumno1@mislata.es», el asunto «Aviso Mislata», el cuerpo «texto», elige el centro «CIPFP Mislata» y pulsa "Guardar".
2. **Y** pulsa "Nuevo correo" de nuevo, rellena los mismos datos pero con el asunto «Aviso Batoi», elige el centro «CIPFP Batoi» y pulsa "Guardar".
3. **Y** cierra sesión.
4. **Cuando** el supervisor «supervisordoscentros@mislata.es» inicia sesión con contraseña «demo1234».
5. **Y** abre la pantalla "Correos de mis centros".

## Resultado esperado
- El sistema muestra el correo «Aviso Mislata» con el centro «CIPFP Mislata» y el correo «Aviso Batoi» con el centro «CIPFP Batoi».
