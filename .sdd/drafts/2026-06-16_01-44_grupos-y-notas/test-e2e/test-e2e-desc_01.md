---
type: test-e2e
id: T-001
---

# T-001 — Crear un grupo con sus alumnos

**Origen ESC:** ESC-001
**Verifica:** R-Grupo-001, R-Grupo-002, R-AlumnoGrupo-001, U-grupos-supervisor-001, U-grupos-supervisor-002
**Pantalla principal:** screen-grupos-supervisor.md
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

**Usuarios de acceso** (login y contraseña que `/sdd-test-e2e` usará para iniciar sesión):

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

> Convención adoptada por el diseño (el spec no fija credenciales): los logins son el `code`/email de cada usuario en `usuarios-demo.xml` y la contraseña común es `demo1234` (el administrador es `admin`/`admin`). Documentado aquí para que `/sdd-test-e2e` pueda iniciar sesión.

## Precondiciones
- El usuario `supervisor1@mislata.es` ha iniciado sesión.

## Pasos
1. **Dado** que el supervisor está en la pantalla "Grupos".
2. **Cuando** pulsa "Nuevo grupo".
3. **Y** escribe el nombre "1º DAM A".
4. **Y** elige el curso "1º DAM".
5. **Y** observa que el campo "Centro" muestra "CIPFP Mislata" en solo lectura y el "Curso académico" muestra "2024" en solo lectura.
6. **Y** pulsa "Guardar".
7. **Y** en el panel "Alumnos" pulsa "Añadir alumno", elige "Alumno1 CIPFP Mislata" y guarda; repite con "Alumno2 CIPFP Mislata".

## Resultado esperado
- El grupo "1º DAM A" se crea en estado "Abierto", con centro "CIPFP Mislata" y curso académico "2024".
- El panel "Módulos" muestra "Programación" y "Bases de datos".
- El panel "Alumnos" muestra "Alumno1 CIPFP Mislata" y "Alumno2 CIPFP Mislata", cada uno con nota media "Sin nota".
- Al entrar en el módulo "Programación", cada alumno tiene una nota "No evaluado".
