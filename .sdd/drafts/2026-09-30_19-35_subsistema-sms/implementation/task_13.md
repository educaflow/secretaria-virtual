---
type: implementation-task
template: system
---

# Tarea 13 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-guice

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/correos/module/CorreosModule.java` | Modificar | k-guice | Quedan fuera los dos `bind` del pool y del observer; **se conserva** el de `MailSender` |

#### Cambios en correos (comportamiento idéntico; **el resto de cada clase se conserva**)

```java
// Clase: com.educaflow.subsystem.correos.module.CorreosModule  (Modificar — delta)
protected void configure();


```

- **`CorreosModule.configure()`** — **delta:** desaparecen `bind(CorreoAsyncExecutor…)` y
  `bind(CorreoEventObserver.class)`; se **conserva** `bind(MailSender.class).toProvider(MailSenderProvider.class)`.



## Eliminaciones declaradas

| Elemento eliminado | Fichero | Justificación |
|---|---|---|
| Los dos `bind` del pool y del observer en `CorreosModule` | `src/main/java/com/educaflow/subsystem/correos/module/CorreosModule.java` | — (consecuencia de las tres anteriores). Se **conserva** el `bind(MailSender.class)` |
