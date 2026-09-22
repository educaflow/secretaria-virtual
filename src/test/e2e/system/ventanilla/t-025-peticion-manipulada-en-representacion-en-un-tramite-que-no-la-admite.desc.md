---
type: test-e2e
id: T-025
---

<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/test-e2e-desc/t-025-peticion-manipulada-en-representacion-en-un-tramite-que-no-la-admite.desc.md
     Iniciativa: 2026-09-21_17-51_ventanilla-nuevo-expediente
     Test: T-025  |  Origen ESC: ESC-024
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-025 — Petición manipulada en representación en un trámite que no la admite

**Origen ESC:** ESC-024
**Verifica:** V-AsistenteNuevoExpediente-006, U-nuevo-expediente-015
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
1. **Dado** que el jefe de estudios `jefeestudios1@mislata.es` ha iniciado sesión con la contraseña `demo1234`.
2. **Y** ha llegado a "Nuevo expediente" para el trámite "Justificación de falta del profesorado" en el centro "CIPFP Mislata", con "¿Cómo se presenta?" visible.
3. **Cuando** marca "Estoy registrando un trámite recibido en papel".
4. **Entonces** sigue sin verse "¿Para quién es el expediente?", porque el trámite no admite representación.
5. **Cuando** pulsa "Crear expediente" con la petición manipulada para indicar que el expediente es en representación de otra persona, dejando el resto como está en la pantalla: el centro "CIPFP Mislata", el trámite "Justificación de falta del profesorado" y registrado como recibido en papel.

## Resultado esperado
- El sistema no crea ningún expediente.
- Bajo el título "No es posible crear el expediente" muestra el mensaje "Este trámite no permite presentar la solicitud en representación de otra persona".
- La pantalla "Nuevo expediente" sigue abierta con el trámite "Justificación de falta del profesorado" y el centro "CIPFP Mislata", con "Estoy registrando un trámite recibido en papel" todavía marcado; no se abre ningún expediente.
