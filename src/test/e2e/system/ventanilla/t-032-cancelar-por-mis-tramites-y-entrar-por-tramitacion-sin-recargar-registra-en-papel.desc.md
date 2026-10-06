---
type: test-e2e
id: T-032
---

<!-- Escrito a mano (no generado por /sdd-create-tests-e2e) como regresión del cliente Axelor: al cerrar el asistente
     «Nuevo expediente» desde una acción la URL seguía en la entrada de menú y el siguiente menú la reabría. No tiene fuente en .sdd/. -->

# T-032 — Cancelar por «Mis trámites» y entrar por «Tramitación» sin recargar registra en papel

**Origen ESC:** —
**Verifica:** tras cerrar el asistente abierto por «Mis trámites», entrar sin recargar por «Tramitación» usa solo la forma de presentar de «Tramitación»
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

**Entradas del asistente** (la forma de presentar la fija la entrada de menú; el asistente nunca pregunta «¿Cómo se presenta?»):
- «Mis trámites» → «Nuevo trámite»: lo presenta el propio usuario (perfil CREADOR).
- «Tramitación» → «Nuevo trámite»: registra un trámite recibido en papel (perfil TRAMITADOR). El grupo «Tramitación» solo lo ven los usuarios con algún perfil de tramitación y el administrador.

## Precondiciones
- Estado inicial de la base de datos.

## Pasos
1. **Dado** que `administrativo2@mislata.es` (Administrativo y Alumno, con los perfiles TRAMITADOR y CREADOR sobre los trámites del alumno) ha iniciado sesión con la contraseña `demo1234`.
2. **Cuando** abre el menú "Mis trámites", pulsa "Nuevo trámite" y, en "Nuevo expediente: elija el trámite" (con el centro "CIPFP Mislata" y únicamente "Trámites para el alumno" → "Anulación de matrícula en ciclo formativo"), pulsa la fila del trámite.
3. **Entonces** "Nuevo expediente" no pregunta "¿Para quién es el expediente?" (presentándolo él mismo, como alumno que no es familiar, solo puede ser para él).
4. **Cuando** pulsa "Atrás" y, en "elija el trámite", "Cancelar", el asistente se cierra.
5. **Y**, sin recargar la aplicación, abre el menú "Tramitación", pulsa "Nuevo trámite" y, en "elija el trámite" (con el mismo centro y el mismo trámite), pulsa la fila del trámite.
6. **Entonces** "Nuevo expediente" sí pregunta "¿Para quién es el expediente?", con "Para mí" y "Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)" sin marcar.
7. **Cuando** marca "Para mí" y pulsa "Crear expediente".

**Nota.**
Es la regresión de un fallo del cliente: al cerrar el asistente con "Cancelar" la URL seguía apuntando a la entrada "Mis trámites", y el siguiente clic en un menú la reabría a la vez que "Tramitación".
Los dos `onNew` de "elija el centro" competían con formas de presentar distintas y ganaba la última respuesta, así que la pantalla podía quedarse con la forma de "Mis trámites".
Por eso el test no recarga entre las dos entradas y, además de lo visible, comprueba la red.

## Resultado esperado
- Tras "Cancelar" la URL ya no apunta a la entrada "Mis trámites", y en cada momento hay como mucho una pestaña del asistente.
- Tras pulsar "Tramitación" solo se envía un `onNew` de "elija el centro", con `_presentadoEnPapel = true`.
- El asistente se cierra y se abre el expediente recién creado de "Anulación de matrícula en ciclo formativo" en el estado "Pendiente de adjuntar la solicitud en papel escaneada", el primero de la forma de presentar de la entrada "Tramitación".
- El expediente persistido está en "CIPFP Mislata", con `presentadoEnPapel = true`, para él mismo (`presentadoEnRepresentacion = false`) y con `usuarioRegistrador` el propio usuario.
- Al final se borra desde "Tramitación" → "Pendientes de mí".
