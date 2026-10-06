---
type: test-e2e
id: T-009
---

<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/test-e2e-desc/t-009-administrativo-que-solo-puede-registrar-en-papel-no-se-le-pregunta-como-se-presenta.desc.md
     Iniciativa: 2026-09-21_17-51_ventanilla-nuevo-expediente
     Test: T-009  |  Origen ESC: ESC-009
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-009 — Administrativo que solo puede registrar en papel crea el expediente por «Tramitación»

**Origen ESC:** ESC-009
**Verifica:** U-nuevo-expediente-006, U-nuevo-expediente-008, U-nuevo-expediente-009, U-nuevo-expediente-010, U-nuevo-expediente-013, R-AsistenteNuevoExpediente-001, R-AsistenteNuevoExpediente-005
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** happy

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
1. **Dado** que el administrativo `administrativo1@mislata.es` ha iniciado sesión con la contraseña `demo1234`.
2. **Cuando** abre el menú "Tramitación" y pulsa "Nuevo trámite" (la entrada con la que se registra en papel un trámite recibido en ventanilla).
3. **Entonces** se abre directamente "Nuevo expediente: elija el trámite" con el centro "CIPFP Mislata", únicamente "Trámites para el alumno" con "Anulación de matrícula en ciclo formativo" dentro, y un único botón debajo, "Cancelar".
4. **Cuando** pulsa la fila "Anulación de matrícula en ciclo formativo".
5. **Entonces** se abre "Nuevo expediente" con ese trámite, su ayuda y el centro en solo lectura; no se ve "¿Cómo se presenta?" (la forma la fija la entrada de menú); sí se ve "¿Para quién es el expediente?" con las opciones "Para mí" y "Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)", sin ninguna marcada; se ven los botones "Atrás" y "Crear expediente".
6. **Cuando** marca "Para mí" y pulsa "Crear expediente".

## Resultado esperado
- El asistente se cierra y se abre el expediente recién creado de "Anulación de matrícula en ciclo formativo" en su primer estado, en el centro "CIPFP Mislata".
- El expediente queda registrado como presentado en papel por el administrativo y para él mismo (no en representación de otra persona).
