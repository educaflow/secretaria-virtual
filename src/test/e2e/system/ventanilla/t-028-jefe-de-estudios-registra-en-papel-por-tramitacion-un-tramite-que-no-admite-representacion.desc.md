---
type: test-e2e
id: T-028
---

<!-- Escrito a mano (no generado por /sdd-create-tests-e2e) al fijar la forma de presentar por la entrada de menú
     del asistente «Nuevo expediente». No tiene fuente en .sdd/. -->

# T-028 — Jefe de estudios registra en papel por «Tramitación» un trámite que no admite representación

**Origen ESC:** —
**Verifica:** la entrada «Tramitación» → «Trámite en papel» registra en papel sin hacer ninguna pregunta cuando el trámite no admite representación
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
- «Tramitación» → «Trámite en papel»: registra un trámite recibido en papel (perfil TRAMITADOR). El grupo «Tramitación» solo lo ven los usuarios con algún perfil de tramitación y el administrador.

## Precondiciones
- Estado inicial de la base de datos.

## Pasos
1. **Dado** que el jefe de estudios `jefeestudios1@mislata.es` ha iniciado sesión con la contraseña `demo1234`.
2. **Cuando** abre el menú "Tramitación" y pulsa "Trámite en papel".
3. **Entonces** se abre directamente "Nuevo expediente: elija el trámite" con el centro "CIPFP Mislata", únicamente "Trámites para el profesor" con "Justificación de falta del profesorado" y "Trámite de prueba" por orden alfabético, y un único botón debajo, "Cancelar".
4. **Cuando** pulsa la fila "Justificación de falta del profesorado".
5. **Entonces** se abre "Nuevo expediente" con ese trámite, su ayuda y el centro en solo lectura; no se ve "¿Cómo se presenta?" (la forma la fija la entrada de menú) ni "¿Para quién es el expediente?" (el trámite no admite representación).
6. **Cuando** elige el idioma "Castellano" y pulsa "Crear expediente".

## Resultado esperado
- El asistente se cierra y se abre el expediente recién creado de "Justificación de falta del profesorado" en la fase "Entrada", estado "Pendiente de adjuntar la solicitud en papel escaneada", con el panel para adjuntar la solicitud escaneada y su aviso, y sin el panel "Datos del profesor interesado".
- El pie ofrece "Borrar el expediente" y "Siguiente", sin "Salir" ni "Presentar la solicitud"; "Creado por" es el jefe de estudios.
- El expediente persistido está en el centro "CIPFP Mislata", registrado en papel (`presentadoEnPapel = true`) y para él mismo (`presentadoEnRepresentacion = false`), con `usuarioRegistrador` el jefe de estudios y las personas interesada y solicitante vacías.
