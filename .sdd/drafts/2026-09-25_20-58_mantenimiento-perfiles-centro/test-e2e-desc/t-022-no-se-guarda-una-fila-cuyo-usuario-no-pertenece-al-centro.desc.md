---
type: test-e2e
id: T-022
---

# T-022 — No se guarda una fila cuyo usuario no pertenece al centro

**Origen ESC:** ESC-015
**Verifica:** V-AceProfileCentro-006
**Pantalla principal:** screen-perfiles-tramites-todos-centros.md
**Tipo:** error

## Estado inicial de la base de datos

Estado previo (datos maestros gestionados por otros subsistemas y datos de demo) del que parten **todos** los tests. Ningún test puede presuponer más estado que este; cada test lo referencia en sus `Precondiciones`.

- Centros «CIPFP Mislata» y «CIPFP Batoi».
- Tipos de usuario del catálogo, entre ellos «Profesor», «Alumno», «Administrativo» y «Supervisor del centro».
- Cargos del catálogo, entre ellos «Director», «Secretario» y «Jefe de estudios».
- Trámites «Trámite de prueba», «Justificación de falta del profesorado» y «Anulación de matrícula en ciclo formativo».
- Usuarios de demo: «Supervisor1 CIPFP Mislata» es supervisor **solo** de «CIPFP Mislata»; «Director CIPFP Mislata» tiene el cargo «Director» en «CIPFP Mislata» y no es supervisor; «Profesor1 CIPFP Mislata» es profesor de «CIPFP Mislata»; «Profesor1 CIPFP Batoi» es profesor de «CIPFP Batoi».
- «Profesor1 CIPFP Mislata» no tiene ningún perfil que le permita crear el trámite «Anulación de matrícula en ciclo formativo» en «CIPFP Mislata».
- La tabla de perfiles de trámites por centro está **vacía**.

**Usuarios de acceso** (login y contraseña que `/sdd-debug-with-test-e2e-desc` usará para iniciar sesión):

| Login | Contraseña | Rol / Tipo | Centro |
|---|---|---|---|
| admin | admin | Administrador | — (todos) |
| supervisor1@mislata.es | demo1234 | Supervisor del centro | CIPFP Mislata |
| director@mislata.es | demo1234 | Profesor con cargo Director | CIPFP Mislata |
| profesor1@mislata.es | demo1234 | Profesor | CIPFP Mislata |

## Precondiciones

- El estado descrito en «Estado inicial de la base de datos».

## Pasos

1. **Dado** que el administrador inicia sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre «Administración → Perfiles de trámites por centro» y pulsa «Nuevo».
3. **Y** elige el centro «CIPFP Mislata», el trámite «Trámite de prueba», el perfil «Colaborador» y el usuario «Profesor1 CIPFP Mislata».
4. **Y** cambia el centro a «CIPFP Batoi» sin cambiar el usuario.
5. **Y** pulsa «Guardar».

## Resultado esperado

- El sistema muestra el mensaje «El usuario no pertenece al centro» y no guarda la fila.
