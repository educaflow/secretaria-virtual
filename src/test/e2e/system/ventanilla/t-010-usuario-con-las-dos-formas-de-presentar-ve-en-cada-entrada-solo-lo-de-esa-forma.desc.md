---
type: test-e2e
id: T-010
---

<!-- Escrito a mano (no generado por /sdd-create-tests-e2e) al fijar la forma de presentar por la entrada de menú
     del asistente «Nuevo expediente». No tiene fuente en .sdd/. -->

# T-010 — Usuario con las dos formas de presentar ve en cada entrada solo lo de esa forma

**Origen ESC:** —
**Verifica:** el asistente calcula lo que ofrece con el perfil de la entrada de menú y nunca pregunta «¿Cómo se presenta?»
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
3. **Entonces** "Nuevo expediente" no pregunta "¿Cómo se presenta?" ni "¿Para quién es el expediente?" (presentándolo él mismo, como alumno que no es familiar, solo puede ser para él).
4. **Cuando** pulsa "Atrás" y, en "elija el trámite", "Cancelar".
5. **Y**, sin recargar la aplicación, abre el menú "Tramitación", pulsa "Nuevo trámite" y, en "elija el trámite" (con el mismo centro y el mismo trámite), pulsa la fila del trámite.
6. **Entonces** "Nuevo expediente" sigue sin preguntar "¿Cómo se presenta?" pero sí pregunta "¿Para quién es el expediente?", con "Para mí" y "Para otra persona a la que represento (hijo/a menor de edad o persona tutelada)" sin marcar.
7. **Cuando** pulsa "Atrás" y "Cancelar".

**Nota.**
Con los datos de demo no hay ningún usuario con los dos perfiles cuyas listas de trámites difieran por entrada: los dos perfiles de `administrativo2@mislata.es` (y de `jefeestudios1@mislata.es`) alcanzan los mismos trámites.
Por eso la diferencia por entrada se observa en la pregunta del último paso; las listas distintas por entrada las prueban T-009 (`administrativo1@mislata.es`, solo «Tramitación») y T-029 (`director@mislata.es`, solo «Mis trámites»).

## Resultado esperado
- En ningún momento se pregunta "¿Cómo se presenta?".
- La pregunta "¿Para quién es el expediente?" depende de la entrada: no aparece por "Mis trámites" y sí por "Tramitación".
- No se crea ningún expediente y el asistente queda cerrado.
