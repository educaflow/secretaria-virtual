---
type: test-e2e
id: T-029
---

<!-- Escrito a mano (no generado por /sdd-create-tests-e2e) al fijar la forma de presentar por la entrada de menú
     del asistente «Nuevo expediente». No tiene fuente en .sdd/. -->

# T-029 — Usuario que no puede registrar en papel en ningún centro entra por «Tramitación»

**Origen ESC:** —
**Verifica:** los centros y trámites que ofrece el asistente dependen del perfil de la entrada; sin ninguno por «Tramitación» se avisa y no queda pantalla abierta
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

**Entradas del asistente** (la forma de presentar la fija la entrada de menú; el asistente nunca pregunta «¿Cómo se presenta?»):
- «Mis trámites» → «Nuevo trámite»: lo presenta el propio usuario (perfil CREADOR).
- «Tramitación» → «Nuevo trámite»: registra un trámite recibido en papel (perfil TRAMITADOR). El grupo «Tramitación» solo lo ven los usuarios con algún perfil de tramitación y el administrador.

## Precondiciones
- Estado inicial de la base de datos.

## Pasos
1. **Dado** que el director `director@mislata.es` (Profesor con cargo de Director: ve el grupo "Tramitación", pero no puede registrar en papel ningún trámite en ninguno de sus centros) ha iniciado sesión con la contraseña `demo1234`.
2. **Cuando** abre el menú "Mis trámites" y pulsa "Nuevo trámite".
3. **Entonces** se abre "Nuevo expediente: elija el trámite" con el centro "CIPFP Mislata" y únicamente "Trámites para el profesor" con "Justificación de falta del profesorado" y "Trámite de prueba".
4. **Cuando** pulsa "Cancelar".
5. **Y**, sin recargar la aplicación, abre el menú "Tramitación" y pulsa "Nuevo trámite".

**Nota.**
Un alumno (p. ej. `alumno1@mislata.es`) no sirve para este caso: no ve el grupo "Tramitación".

## Resultado esperado
- El sistema muestra el aviso "No puede crear expedientes en ninguno de sus centros".
- Tras aceptarlo no queda abierta ninguna pantalla del asistente: ni "Nuevo expediente: elija el centro", ni "Nuevo expediente: elija el trámite", ni "Nuevo expediente".
