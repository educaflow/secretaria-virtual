---
type: test-e2e
id: T-012
---

<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-06-30_13-56_subsistema-correos/test-e2e-desc/t-012-alta-con-para-de-formato-invalido.desc.md
     Test: T-012  |  Origen ESC: ESC-020
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-012 — Alta con «para» de formato inválido

**Origen ESC:** ESC-020
**Verifica:** V-Correo-006
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
2. **Cuando** rellena el DNI «86862719E», el nombre «Alumno1», los apellidos «CIPFP Mislata», el «para» «direccion-sin-arroba», el asunto «Convocatoria de reunión», el cuerpo «texto» y elige el centro «CIPFP Mislata».
3. **Y** pulsa "Guardar".

## Resultado esperado
- El sistema muestra el mensaje "El «para» debe contener direcciones de correo válidas (por ejemplo, usuario@dominio.com)" y no crea el correo.
