---
type: implementation-task
template: system
---

# Tarea 10 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-guice

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/secretariavirtual/module/SecretariaVirtualModule.java` | Modificar | k-guice | Una línea: bindea `EjecutorAsincrono` como singleton (es donde el proyecto cablea lo transversal) |

#### Cableado del ejecutor único

```java

// Clase: com.educaflow.secretariavirtual.module.SecretariaVirtualModule  (Modificar — delta)
protected void configure();

```

- **`SecretariaVirtualModule.configure()`** — **delta:** un binding más, que enlaza `EjecutorAsincrono` a
  `EjecutorAsincronoProvider` con ámbito singleton. No cambia
  ninguna otra línea de la clase.

`grep -rn "new EjecutorAsincrono\|bind(EjecutorAsincrono" src/main/java` devuelve **una** línea de cada;
