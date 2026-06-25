---
type: test-e2e
id: T-007
---

# T-007 — La media excluye no evaluados y cuenta la matrícula de honor como 10

**Origen ESC:** ESC-007
**Verifica:** R-Nota-001
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
1. **Dado** que el supervisor crea el grupo "1º DAM A" (curso "1º DAM", módulos "Programación" y "Bases de datos") y le añade a "Alumno1 CIPFP Mislata".
2. **Cuando** pone a "Alumno1 CIPFP Mislata" la nota "Matrícula de Honor" en "Programación" y deja "Bases de datos" en "No evaluado".
3. **Entonces** la nota media de "Alumno1 CIPFP Mislata" es "10".
4. **Y cuando** pone "Bases de datos" en "7".
5. **Entonces** la nota media de "Alumno1 CIPFP Mislata" pasa a "9".

## Resultado esperado
- Con MH en "Programación" y "Bases de datos" no evaluado, la nota media es "10".
- Tras poner "7" en "Bases de datos", la nota media es "9" (media de 10 y 7 redondeada).
