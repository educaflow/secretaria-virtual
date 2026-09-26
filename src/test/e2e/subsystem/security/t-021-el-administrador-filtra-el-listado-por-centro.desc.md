---
type: test-e2e
id: T-021
---
<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-09-25_20-58_mantenimiento-perfiles-centro/test-e2e-desc/t-021-el-administrador-filtra-el-listado-por-centro.desc.md
     Iniciativa: 2026-09-25_20-58_mantenimiento-perfiles-centro
     Test: T-021  |  Origen ESC: ESC-014
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-021 — El administrador filtra el listado por centro

**Origen ESC:** ESC-014
**Verifica:** —
**Pantalla principal:** screen-perfiles-tramites-todos-centros.md
**Tipo:** UI

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
3. **Y** elige el centro «CIPFP Batoi», el trámite «Trámite de prueba», el perfil «Tramitador» y el cargo «Jefe de estudios», y pulsa «Guardar».
4. **Y** pulsa «Nuevo», elige el centro «CIPFP Mislata», el trámite «Trámite de prueba», el perfil «Tramitador» y el cargo «Jefe de estudios», y pulsa «Guardar».
5. **Y** vuelve al listado.
6. **Y** en la búsqueda del listado filtra por centro escribiendo «CIPFP Batoi» y aplica el filtro.

## Resultado esperado

- El listado muestra la fila del centro «CIPFP Batoi» con trámite «Trámite de prueba», perfil «Tramitador» y cargo «Jefe de estudios».
- El listado no muestra ninguna fila del centro «CIPFP Mislata».
