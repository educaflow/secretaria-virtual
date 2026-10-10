---
type: test-e2e
id: T-033
---

<!-- Escrito a mano (no generado por /sdd-create-tests-e2e) como regresión del cliente Axelor: al cerrar el asistente
     «Nuevo expediente» desde una acción la URL seguía en la entrada de menú y el siguiente menú la reabría. No tiene fuente en .sdd/. -->

# T-033 — Cancelar por «Mis trámites» y entrar por «Tramitación» sin recargar avisa una sola vez

**Origen ESC:** —
**Verifica:** tras cerrar el asistente abierto por «Mis trámites», entrar sin recargar por «Tramitación» sin centros avisa una sola vez y no deja pantalla abierta
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
- «Tramitación» → «Trámite en papel»: registra un trámite recibido en papel (perfil TRAMITADOR). El grupo «Tramitación» solo lo ven los usuarios con algún perfil de tramitación y el administrador.

## Precondiciones
- Estado inicial de la base de datos.

## Pasos
1. **Dado** que el director `director@mislata.es` (Profesor con cargo de Director: ve el grupo "Tramitación", pero no puede registrar en papel ningún trámite en ninguno de sus centros) ha iniciado sesión con la contraseña `demo1234`.
2. **Cuando** abre el menú "Mis trámites" y pulsa "Nuevo trámite", se abre "Nuevo expediente: elija el trámite" con el centro "CIPFP Mislata".
3. **Cuando** pulsa "Cancelar", el asistente se cierra.
4. **Y**, sin recargar la aplicación, abre el menú "Tramitación" y pulsa "Trámite en papel".
5. **Entonces** el sistema muestra el aviso "No puede crear expedientes en ninguno de sus centros".
6. **Cuando** lo acepta.

**Nota.**
Es la regresión de un fallo del cliente: al cerrar el asistente con "Cancelar" la URL seguía apuntando a la entrada "Mis trámites", y el siguiente clic en un menú la reabría a la vez que "Tramitación"; el aviso reaparecía en bucle al aceptarlo y la pestaña "elija el centro" no se cerraba.
Por eso el test no recarga entre las dos entradas.
El aviso interrumpe el `onNew` de "elija el centro", y al aceptarlo el cliente reanuda el resto del mismo `onNew` con otra petición (`<acción>[<índice>]`): no cuenta como un segundo `onNew`.

## Resultado esperado
- Tras "Cancelar" la URL ya no apunta a la entrada "Mis trámites".
- El aviso aparece una sola vez: tras aceptarlo no vuelve a aparecer.
- Tras pulsar "Tramitación" solo se envía un `onNew` de "elija el centro", con `_presentadoEnPapel = true`.
- No queda abierta ninguna pantalla del asistente: ni "Nuevo expediente: elija el centro", ni "Nuevo expediente: elija el trámite", ni "Nuevo expediente".
