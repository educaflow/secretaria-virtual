---
type: test-e2e
id: T-017
---

<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/test-e2e-desc/t-017-peticion-manipulada-para-crear-el-expediente-en-representacion-sin-poder.desc.md
     Iniciativa: 2026-09-21_17-51_ventanilla-nuevo-expediente
     Test: T-017  |  Origen ESC: ESC-016
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-017 — Petición manipulada para crear el expediente en representación sin poder

**Origen ESC:** ESC-016
**Verifica:** V-AsistenteNuevoExpediente-008, U-nuevo-expediente-015
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** error

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
1. **Dado** que el alumno `alumno1@mislata.es` ha iniciado sesión con la contraseña `demo1234`.
2. **Y** ha llegado a "Nuevo expediente" para el trámite "Anulación de matrícula en ciclo formativo" en el centro "CIPFP Mislata", sin que se le pregunte nada.
3. **Cuando** pulsa "Crear expediente" con la petición manipulada para indicar que el expediente es en representación de otra persona, dejando el resto como lo fijó la pantalla: el centro "CIPFP Mislata", el trámite "Anulación de matrícula en ciclo formativo" y presentado por el propio alumno.

## Resultado esperado
- El sistema no crea ningún expediente.
- Bajo el título "No es posible crear el expediente" muestra el mensaje "No puede crear este expediente en representación de otra persona en el centro indicado".
- La pantalla "Nuevo expediente" sigue abierta con el trámite "Anulación de matrícula en ciclo formativo" y el centro "CIPFP Mislata", y no se abre ningún expediente.
