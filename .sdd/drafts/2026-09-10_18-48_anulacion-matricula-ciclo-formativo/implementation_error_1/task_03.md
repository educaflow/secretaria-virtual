---
type: implementation-task
template: expediente
---

# Tarea 03 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-tipo-expediente

## Qué hay que hacer

Esta tarea **no escribe ningún fichero a mano**: ejecuta la tarea Gradle que genera los esqueletos del tipo de expediente. Es el punto exacto en el que el árbol de ficheros del tipo pasa a existir, y su ejecución tiene que ser observable como un paso propio.

Comando exacto, con la ruta resuelta:

```
./gradlew -q CreateFilesTask -Ptipo=src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1
```

- **MUST** ejecutarse **después** de que `…/v1/TipoExpedienteInstance.xml` exista y esté **completo** (las tres fases, los seis estados y todos los `events`, incluido `events=""` en los dos cerrados), porque la tarea **lee** ese XML para saber qué subcarpetas de fase crear.
- **MUST** ejecutarse **antes** de rellenar nada: las tareas siguientes escriben **sobre** los esqueletos que esta deja.
- Qué crea exactamente: en la raíz de la versión, `domains.xml`, `views.xml` e `InitialEventManagerImpl.java`; y en **cada** subcarpeta de fase (`solicitud/`, `revision/`, `resolucion/`), su `PhaseEventManagerImpl.java`, su `StateEventValidatorImpl.kt` y su `views.xml`.
- Es **idempotente** y **nunca pisa lo ya escrito**: imprime una línea `CREADO <ruta>` por fichero **creado** y **nada** por los que ya existían.
- **Verificación:** aparece una línea `CREADO` por cada uno de esos doce ficheros (o el fichero ya existía). La tarea falla con un mensaje explícito si la ruta no corresponde a ningún tipo de expediente o si la fase no existe.
- **MUST NOT** usarse `-Pfase`: acota a una sola fase y entonces **no** genera los ficheros de la raíz de la versión; si alguna vez se usara, **MUST** ir siempre junto con `-Ptipo`.
- **MUST NOT** engancharse al build: compilar no debe escribir en `src/main/java`.

## `### Paso 3 — Ejecutar CreateFilesTask` del diseño (verbatim)

### Paso 3 — Ejecutar `CreateFilesTask`

```
./gradlew -q CreateFilesTask -Ptipo=src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1
```

- La tarea **lee** el `TipoExpedienteInstance.xml`, así que el paso 2 tiene que estar **completo** antes: cada `<fase>` declarada produce su subcarpeta.
- Crea, en la raíz de la versión, `domains.xml`, `views.xml` e `InitialEventManagerImpl.java`; y en **cada** una de las carpetas `solicitud/`, `revision/` y `resolucion/`, su `PhaseEventManagerImpl.java`, su `StateEventValidatorImpl.kt` y su `views.xml`.
- Imprime una línea `CREADO <ruta>` por fichero creado: la verificación es que aparezca **una línea `CREADO` por cada uno de esos doce ficheros**.
- Es **idempotente** y nunca pisa lo ya escrito.
- **MUST NOT** usarse `-Pfase` (acota a una fase y entonces no genera los ficheros de la raíz de la versión); si alguna vez se usara, **MUST** ir siempre junto con `-Ptipo`.
- **MUST NOT** engancharse al build: compilar no debe escribir en `src/main/java`.

