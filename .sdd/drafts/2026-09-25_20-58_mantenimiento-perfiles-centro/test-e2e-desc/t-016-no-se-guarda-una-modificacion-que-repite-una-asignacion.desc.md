---
type: test-e2e
id: T-016
---

# T-016 — No se guarda una modificación que repite una asignación

**Origen ESC:** ESC-019
**Verifica:** V-AceProfileCentro-007
**Pantalla principal:** screen-perfiles-tramites-mi-centro.md
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

1. **Dado** que el supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
2. **Cuando** abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
3. **Y** elige el centro «CIPFP Mislata», el trámite «Trámite de prueba», el perfil «Creador» y el tipo de usuario «Alumno», y pulsa «Guardar».
4. **Y** vuelve al listado y pulsa «Nuevo».
5. **Y** elige el centro «CIPFP Mislata», el trámite «Trámite de prueba», el perfil «Afectado» y el tipo de usuario «Alumno», sin rellenar cargo ni usuario.
6. **Y** pulsa «Guardar».
7. **Y** vuelve al listado y abre la fila con perfil «Afectado» y tipo de usuario «Alumno».
8. **Y** cambia el perfil a «Creador» y pulsa «Guardar».
9. **Entonces** el sistema muestra el mensaje «Ya existe esa asignación de perfil» y no guarda el cambio.
10. **Cuando** pulsa «Cancelar» y vuelve al listado.

## Resultado esperado

- El listado muestra la fila con perfil «Afectado» y tipo de usuario «Alumno».
- El listado muestra una sola fila con perfil «Creador» y tipo de usuario «Alumno».
