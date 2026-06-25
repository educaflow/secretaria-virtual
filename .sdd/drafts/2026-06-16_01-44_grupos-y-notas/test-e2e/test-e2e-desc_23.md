---
type: test-e2e
id: T-023
---

# T-023 — Poner un valor de nota inválido

**Origen ESC:** ESC-023
**Verifica:** V-Nota-003
**Pantalla principal:** screen-grupos-supervisor.md (parte UI) + endpoint REST genérico `/ws/rest/com.educaflow.system.gruposnotas.db.Nota` (parte servidor)
**Tipo:** error

> **Nota de ejecutabilidad (VAL-016 / V-Nota-003):** `valor` se modela como enum `ValorNota` (No evaluado / 1..10 / Matrícula de Honor), por lo que la UI lo presenta como un **selector que solo ofrece valores válidos**: desde la pantalla es **imposible** introducir "11". El selector impide por construcción el valor inválido (eso es lo que se comprueba en la parte UI). La defensa servidor V-Nota-003 (VAL-016), que protege ante un valor crudo fuera del enum, solo es alcanzable por la **Vía B** (endpoint REST genérico `/ws/rest`), no por la UI; por eso la comprobación de dominio inválido se ejerce vía petición REST directa.

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
1. **Dado** que el supervisor crea el grupo "1º DAM A" (curso "1º DAM") y le añade a "Alumno1 CIPFP Mislata".
2. **Cuando** abre la nota de "Alumno1 CIPFP Mislata" en "Programación" en la pantalla y despliega el selector de "Valor".
3. **Entonces** el selector solo ofrece "No evaluado", "1".."10" y "Matrícula de Honor" — no existe la opción "11" (la UI impide por construcción un valor fuera del dominio).
4. **Y cuando** se envía directamente al endpoint REST genérico (Vía B, `POST /ws/rest/com.educaflow.system.gruposnotas.db.Nota` con la acción de guardado) un `update` de esa misma nota con un `valor` fuera del enum `ValorNota`.

## Resultado esperado
- En la UI, el selector de "Valor" no ofrece "11" ni ningún valor fuera del dominio (No evaluado / 1..10 / Matrícula de Honor).
- En la Vía B (REST), el servidor rechaza el `update` con el mensaje "La nota debe ser No evaluado, un número entero del 1 al 10 o Matrícula de Honor" (V-Nota-003 / VAL-016) y la nota no cambia.
