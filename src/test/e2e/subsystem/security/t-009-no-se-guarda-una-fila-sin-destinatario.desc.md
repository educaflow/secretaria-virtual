---
type: test-e2e
id: T-009
---
<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-09-25_20-58_mantenimiento-perfiles-centro/test-e2e-desc/t-009-no-se-guarda-una-fila-sin-destinatario.desc.md
     Iniciativa: 2026-09-25_20-58_mantenimiento-perfiles-centro
     Test: T-009  |  Origen ESC: ESC-006
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-009 — No se guarda una fila sin destinatario

**Origen ESC:** ESC-006
**Verifica:** V-AceProfileCentro-004
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
3. **Y** elige el centro «CIPFP Mislata», el trámite «Trámite de prueba» y el perfil «Tramitador», sin rellenar tipo de usuario, cargo ni usuario.
4. **Y** pulsa «Guardar».

## Resultado esperado

- El sistema muestra el mensaje «Indica a quién se da el perfil: un tipo de usuario, un cargo o un usuario» y no guarda la fila.
