---
type: test-e2e
id: T-005
---
<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-09-25_20-58_mantenimiento-perfiles-centro/test-e2e-desc/t-005-el-supervisor-solo-ve-y-usa-sus-centros.desc.md
     Iniciativa: 2026-09-25_20-58_mantenimiento-perfiles-centro
     Test: T-005  |  Origen ESC: ESC-005
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-005 — El supervisor solo ve y usa sus centros

**Origen ESC:** ESC-005
**Verifica:** U-perfiles-tramites-mi-centro-001
**Pantalla principal:** screen-perfiles-tramites-mi-centro.md
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
3. **Y** elige el centro «CIPFP Batoi», el trámite «Trámite de prueba», el perfil «Director» y el cargo «Director», sin rellenar tipo de usuario ni usuario.
4. **Y** pulsa «Guardar».
5. **Y** el administrador cierra sesión.
6. **Y** el supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
7. **Y** abre «Mi centro → Perfiles de trámites».
8. **Entonces** el listado no muestra ninguna fila del centro «CIPFP Batoi».
9. **Cuando** pulsa «Nuevo» y despliega el selector de centro.

## Resultado esperado

- El selector de centro ofrece «CIPFP Mislata» y no ofrece «CIPFP Batoi».
