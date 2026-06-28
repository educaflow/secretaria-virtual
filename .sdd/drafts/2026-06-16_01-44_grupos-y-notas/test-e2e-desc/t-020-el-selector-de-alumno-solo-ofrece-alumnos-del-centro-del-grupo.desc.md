---
type: test-e2e
id: T-020
---

# T-020 — El selector de alumno solo ofrece alumnos del centro del grupo

**Origen ESC:** ESC-020
**Verifica:** V-AlumnoGrupo-004, U-grupos-supervisor-006
**Pantalla principal:** screen-grupos-supervisor.md
**Tipo:** UI

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
- El usuario `supervisor1@mislata.es` ha iniciado sesión.

## Pasos
1. **Dado** que el supervisor crea el grupo "1º DAM A" (curso "1º DAM").
2. **Cuando** en el panel "Alumnos" pulsa "Añadir alumno" y abre el selector de alumno.

## Resultado esperado
- El selector ofrece a "Alumno1 CIPFP Mislata", "Alumno2 CIPFP Mislata", "Alumno3 CIPFP Mislata" y "Alumno4 CIPFP Mislata" (alumnos de "CIPFP Mislata").
- El selector NO ofrece a "Director CIPFP Mislata" (profesor de "CIPFP Mislata") ni a "Alumno1 CIPFP Batoi" (alumna de "CIPFP Batoi").
