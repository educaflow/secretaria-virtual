---
type: test-e2e
id: T-008
---

# T-008 — El perfil concedido por el supervisor surte efecto

**Origen ESC:** ESC-022
**Verifica:** —
**Pantalla principal:** screen-perfiles-tramites-mi-centro.md
**Tipo:** happy

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

1. **Dado** que el profesor inicia sesión con usuario «profesor1@mislata.es» y contraseña «demo1234».
2. **Cuando** abre «Mis trámites → Nuevo trámite» y elige el centro «CIPFP Mislata».
3. **Entonces** el sistema no ofrece el trámite «Anulación de matrícula en ciclo formativo».
4. **Cuando** el profesor cierra sesión.
5. **Y** el supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
6. **Y** abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
7. **Y** elige el centro «CIPFP Mislata», el trámite «Anulación de matrícula en ciclo formativo», el perfil «Creador» y el usuario «Profesor1 CIPFP Mislata», sin rellenar tipo de usuario ni cargo.
8. **Y** pulsa «Guardar» y vuelve al listado.
9. **Entonces** el listado muestra una fila con centro «CIPFP Mislata», trámite «Anulación de matrícula en ciclo formativo», perfil «Creador» y usuario «Profesor1 CIPFP Mislata».
10. **Cuando** el supervisor cierra sesión.
11. **Y** el profesor inicia sesión con usuario «profesor1@mislata.es» y contraseña «demo1234».
12. **Y** abre «Mis trámites → Nuevo trámite» y elige el centro «CIPFP Mislata».

## Resultado esperado

- El sistema ofrece el trámite «Anulación de matrícula en ciclo formativo».
