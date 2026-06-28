---
type: test-e2e
id: T-013
---

<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-06-16_01-44_grupos-y-notas/test-e2e-desc/t-013-el-administrador-reabre-un-grupo-cerrado.desc.md
     Test: T-013  |  Origen ESC: ESC-013
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-013 — El administrador reabre un grupo cerrado

**Origen ESC:** ESC-013
**Verifica:** V-Grupo-008, R-Grupo-004, U-grupos-administrador-003
**Pantalla principal:** screen-grupos-administrador.md
**Tipo:** happy

## Estado inicial de la base de datos

Estado previo (datos maestros gestionados por otros subsistemas) del que parten **todos** los tests. Ningún test puede presuponer más estado que este; cada test lo referencia en sus `Precondiciones`.

- **Centros**: «CIPFP Mislata» (curso académico 2024) e «CIPFP Batoi» (curso académico 2024). El campo `curso` (curso académico, entero) de cada centro = `2024`.
- **Catálogo educativo** (subsistema `sistemaeducativo`):
  - Curso «1º DAM» con módulos «Programación» y «Bases de datos».
  - Curso «1º SMR» (con al menos un módulo).
- **Usuarios** (subsistema `common`, con su `CentroUsuario` + `CentroUsuarioTipoUsuario`):
  - Un **Administrador** (superusuario de Axelor, login `admin`/`admin`; se autoriza vía `isAdmin()`, no por un tipo de usuario).
  - Un **Supervisor** del centro «CIPFP Mislata» (tipo SUPERVISOR).
  - Alumnos del centro «CIPFP Mislata» (tipo ALUMNO): «Alumno1 CIPFP Mislata», «Alumno2 CIPFP Mislata», «Alumno3 CIPFP Mislata», «Alumno4 CIPFP Mislata».
  - Un Profesor del centro «CIPFP Mislata» (tipo PROFESOR): «Director CIPFP Mislata».
  - Una alumna del centro «CIPFP Batoi» (tipo ALUMNO): «Alumno1 CIPFP Batoi».

**Usuarios de acceso** (login y contraseña que `/sdd-debug-with-test-e2e-desc` usará para iniciar sesión):

| Login | Contraseña | Rol / Tipo | Centro |
|---|---|---|---|
| admin | admin | Administrador | (todos) |
| supervisor1@mislata.es | demo1234 | Supervisor | CIPFP Mislata |
| alumno1@mislata.es | demo1234 | Alumno | CIPFP Mislata |
| alumno2@mislata.es | demo1234 | Alumno | CIPFP Mislata |
| alumno3@mislata.es | demo1234 | Alumno | CIPFP Mislata |
| alumno4@mislata.es | demo1234 | Alumno | CIPFP Mislata |
| director@mislata.es | demo1234 | Profesor | CIPFP Mislata |
| alumno1@batoi.es | demo1234 | Alumno | CIPFP Batoi |

> Convención adoptada por el diseño (el spec no fija credenciales): los logins son el `code`/email de cada usuario en `usuarios-demo.xml` y la contraseña común es `demo1234` (el administrador es `admin`/`admin`). Documentado aquí para que `/sdd-debug-with-test-e2e-desc` pueda iniciar sesión.

## Precondiciones
- El usuario `supervisor1@mislata.es` y el usuario `admin` figuran en la tabla de acceso.

## Pasos
1. **Dado** que el supervisor crea el grupo "1º DAM A" (curso "1º DAM"), le añade a "Alumno1 CIPFP Mislata" y lo cierra; después cierra sesión.
2. **Cuando** el administrador inicia sesión, abre "Grupos (administración)" y entra en "1º DAM A" (está "Cerrado").
3. **Y** pulsa "Reabrir grupo".

## Resultado esperado
- El grupo pasa a "Abierto" y la "Fecha de cierre" queda vacía.
- Vuelve a permitir modificar las notas.
