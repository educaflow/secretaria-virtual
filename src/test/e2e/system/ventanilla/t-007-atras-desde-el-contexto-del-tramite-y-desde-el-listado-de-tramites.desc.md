---
type: test-e2e
id: T-007
---

<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/test-e2e-desc/t-007-atras-desde-el-contexto-del-tramite-y-desde-el-listado-de-tramites.desc.md
     Iniciativa: 2026-09-21_17-51_ventanilla-nuevo-expediente
     Test: T-007  |  Origen ESC: ESC-007
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-007 — Atrás desde el contexto del trámite y desde el listado de trámites

**Origen ESC:** ESC-007
**Verifica:** U-nuevo-expediente-002, U-nuevo-expediente-003, U-nuevo-expediente-005
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** UI

## Estado inicial de la base de datos

Estado previo (datos maestros gestionados por otros subsistemas) del que parten **todos** los tests. Ningún test puede presuponer más estado que este; cada test lo referencia en sus `Precondiciones`.

- Dos centros: «CIPFP Mislata» y «CIPFP Batoi».
- Dos tipos de trámite: «Trámites para el alumno» (dirigido al tipo de usuario Alumno) y «Trámites para el profesor» (dirigido al tipo de usuario Profesor).
- Tres trámites, todos con su texto de ayuda y activos en los dos centros:
  - «Anulación de matrícula en ciclo formativo», del tipo «Trámites para el alumno», **admite** presentarse en representación.
  - «Justificación de falta del profesorado», del tipo «Trámites para el profesor», **no** admite representación.
  - «Trámite de prueba», del tipo «Trámites para el profesor», **no** admite representación.
- Permisos por defecto para iniciar trámites (los que ya carga el data-init de `subsystem/security`): Profesor, Alumno y Familiar pueden iniciarlos **presentándolos ellos mismos**; Administrativo (sobre los trámites del alumno) y Jefe de estudios (sobre los del profesor) pueden iniciarlos **registrándolos en papel**.
- Los usuarios de la tabla de abajo, cada uno con sus tipos de usuario y cargos en el centro indicado.

**Usuarios de acceso** (login y contraseña que `/sdd-debug-with-test-e2e-desc` usará para iniciar sesión):

| Login | Contraseña | Rol / Tipo | Centro |
|---|---|---|---|
| admin | admin | Administrador de la aplicación, sin ningún perfil de inicio | CIPFP Mislata |
| alumno1@mislata.es | demo1234 | Alumno | CIPFP Mislata |
| alumnodoscentros@mislata.es | demo1234 | Alumno | CIPFP Mislata y CIPFP Batoi |
| exalumno1@mislata.es | demo1234 | Exalumno | CIPFP Mislata |
| director@mislata.es | demo1234 | Profesor con cargo de Director | CIPFP Mislata |
| jefeestudios1@mislata.es | demo1234 | Profesor con cargo de Jefe de estudios | CIPFP Mislata |
| administrativo1@mislata.es | demo1234 | Administrativo | CIPFP Mislata |
| administrativo2@mislata.es | demo1234 | Administrativo y Alumno | CIPFP Mislata |
| familiar1@mislata.es | demo1234 | Familiar | CIPFP Mislata |
| alumnofamiliar@mislata.es | demo1234 | Alumno y Familiar | CIPFP Mislata |

## Precondiciones
- Estado inicial de la base de datos.

## Pasos
1. **Dado** que el alumno `alumnodoscentros@mislata.es` ha iniciado sesión con la contraseña `demo1234`.
2. **Cuando** abre el menú "Mis trámites" y pulsa "Nuevo trámite".
3. **Entonces** se abre "Nuevo expediente: elija el centro" con las filas "CIPFP Batoi" y "CIPFP Mislata" y un botón "Cancelar".
4. **Cuando** pulsa la fila "CIPFP Mislata".
5. **Entonces** se abre "Nuevo expediente: elija el trámite" con el centro "CIPFP Mislata" encima del listado, únicamente "Trámites para el alumno" con "Anulación de matrícula en ciclo formativo" dentro, y un único botón debajo, "Atrás".
6. **Cuando** pulsa la fila "Anulación de matrícula en ciclo formativo".
7. **Entonces** se abre "Nuevo expediente" con ese trámite, su ayuda y el centro "CIPFP Mislata" en solo lectura, sin las dos preguntas, y con los botones "Atrás" y "Crear expediente".
8. **Cuando** pulsa "Atrás".
9. **Entonces** vuelve a "Nuevo expediente: elija el trámite" con el mismo centro "CIPFP Mislata" encima del listado, sin abrir ningún expediente.
10. **Cuando** pulsa "Atrás" debajo del listado.

## Resultado esperado
- Vuelve a "Nuevo expediente: elija el centro" con las filas "CIPFP Batoi" y "CIPFP Mislata".
- No se ha abierto ningún expediente.
