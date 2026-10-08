---
type: test-e2e
id: T-030
---

<!-- Escrito a mano (no generado por /sdd-create-tests-e2e) al fijar la forma de presentar por la entrada de menú
     del asistente «Nuevo expediente». No tiene fuente en .sdd/. -->

# T-030 — «Atrás» conserva el registro en papel al volver a elegir centro

**Origen ESC:** —
**Verifica:** «Atrás» (paso 3 → paso 2 → paso 1) conserva la forma de presentar que fijó la entrada «Tramitación»
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
| administrativo3@mislata.es | demo1234 | Administrativo, Alumno y Familiar | CIPFP Mislata y CIPFP Batoi |

**Entradas del asistente** (la forma de presentar la fija la entrada de menú; el asistente nunca pregunta «¿Cómo se presenta?»):
- «Mis trámites» → «Nuevo trámite»: lo presenta el propio usuario (perfil CREADOR).
- «Tramitación» → «Nuevo trámite»: registra un trámite recibido en papel (perfil TRAMITADOR). El grupo «Tramitación» solo lo ven los usuarios con algún perfil de tramitación y el administrador.

## Precondiciones
- Estado inicial de la base de datos.

## Pasos
1. **Dado** que `administrativo3@mislata.es` (Administrativo, Alumno y Familiar en CIPFP Mislata y en CIPFP Batoi) ha iniciado sesión con la contraseña `demo1234`.
2. **Cuando** abre el menú "Tramitación" y pulsa "Nuevo trámite" (registra un trámite recibido en papel).
3. **Entonces** se abre "Nuevo expediente: elija el centro" con "CIPFP Batoi" y "CIPFP Mislata", y un único botón, "Cancelar".
4. **Cuando** pulsa "CIPFP Mislata" y, en "Nuevo expediente: elija el trámite" (con "CIPFP Mislata" y únicamente "Trámites para el alumno" → "Anulación de matrícula en ciclo formativo"), pulsa la fila del trámite.
5. **Entonces** "Nuevo expediente" no pregunta "¿Cómo se presenta?" y sí "¿Para quién es el expediente?", sin marcar.
6. **Cuando** pulsa "Atrás", vuelve a "elija el trámite" con "CIPFP Mislata" y la misma lista; y cuando vuelve a pulsar "Atrás", vuelve a "elija el centro" con los mismos dos centros.
7. **Cuando** elige "CIPFP Batoi", pulsa la fila del trámite (la misma lista, con "CIPFP Batoi"), comprueba la misma pantalla del paso 5, marca "Para mí", elige el idioma "Castellano" y pulsa "Crear expediente".

## Resultado esperado
- El asistente se cierra y se abre el expediente recién creado de "Anulación de matrícula en ciclo formativo" en el estado "Pendiente de adjuntar la solicitud en papel escaneada", el primero de la forma de presentar de la entrada "Tramitación".
- El expediente persistido está en "CIPFP Batoi", con `presentadoEnPapel = true`, para él mismo (`presentadoEnRepresentacion = false`) y con `usuarioRegistrador` el propio usuario.
- Al final se borra desde "Tramitación" → "Pendientes de mí".
