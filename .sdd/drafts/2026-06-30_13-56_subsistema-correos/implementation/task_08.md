---
type: implementation-task
---

# Tarea 08 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
(Ninguno especial de la lista de skills del proyecto: la columna "Skill" de esta fila del diseño es "—". Sigue las convenciones estándar de Java del proyecto.)

## Ficheros que cubre esta tarea (fila de la tabla "Ficheros a crear o modificar" de `design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/correos/infrastructure/PostCommitRunner.java` | Crear | — | Utilidad estática: ejecutar una tarea tras el commit de la transacción actual |

## Texto del diseño (verbatim, `design.md`, Paso 6 — Módulo Guice)

#### `com.educaflow.subsystem.correos.infrastructure.PostCommitRunner`

```java
package com.educaflow.subsystem.correos.infrastructure;

public final class PostCommitRunner {
    private PostCommitRunner() {}

    public static void runAfterCommit(Runnable tarea);
    //   org.hibernate.Session session = com.axelor.db.JPA.em().unwrap(org.hibernate.Session.class);
    //   session.getTransaction().registerSynchronization(new jakarta.transaction.Synchronization() {
    //     public void beforeCompletion() {}
    //     public void afterCompletion(int status) {
    //       if (status == jakarta.transaction.Status.STATUS_COMMITTED) { tarea.run(); }
    //     }
    //   });
    //   Diseño completo (por qué hace falta) en design/rules/R-Correo-001.md.
}
```

## Diseño detallado de referencia (verbatim, `design/rules/R-Correo-001.md` — sección "Diseño detallado", clase `PostCommitRunner`)

- `com.educaflow.subsystem.correos.infrastructure.PostCommitRunner` — utilidad **estática**, sin estado, sin binding de Guice (no lo necesita: opera sobre el `EntityManager` de la transacción actual del hilo que la invoca).
  - `public static void runAfterCommit(Runnable tarea)` — obtiene la sesión de Hibernate subyacente (`com.axelor.db.JPA.em().unwrap(org.hibernate.Session.class)`) y registra en su transacción actual una `jakarta.transaction.Synchronization` cuyo `afterCompletion(int status)` ejecuta `tarea.run()` **solo si** `status == jakarta.transaction.Status.STATUS_COMMITTED`. Si la transacción hace rollback, la tarea no se ejecuta (no se envía un correo que en realidad no llegó a crearse).

### Garantía de transaccionalidad (verbatim, `design/rules/R-Correo-001.md` — "Análisis de la regla")

- El hilo en segundo plano que hace el envío **no puede** ver el `Correo` recién creado hasta que la transacción HTTP que lo creó **haya hecho commit** (si lo intentara antes, la fila podría no ser visible todavía para una conexión JDBC distinta — condición de carrera). Por eso el envío no se somete al ejecutor en el propio `fireActionRule`, sino que se **programa para ejecutarse tras el commit** de la transacción actual.

## Superficie cerrada

**MUST** crear únicamente la clase final `PostCommitRunner` con constructor privado y el único método estático público `runAfterCommit(Runnable)`. **MUST NOT** añadir binding de Guice para esta clase (no lo necesita, es una utilidad estática). Si detectas que hace falta algo no listado, **detente y reporta** `BLOCKED`.
