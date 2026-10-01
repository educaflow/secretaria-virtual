---
type: implementation-task
template: system
---

# Tarea 01 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- (ninguno: la tabla del diseño no asigna skill a este fichero)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `build.gradle` | Modificar | — | Dependencia `com.googlecode.libphonenumber:libphonenumber` (solo la usa `NumeroTelefono`) |

### Paso 1 — Dependencia y propiedades de configuración

**Fichero:** `build.gradle` (Modificar) y `src/main/resources/axelor-config.properties` (Modificar).

- En `build.gradle`, junto al bloque `//Envio de SMS con Twilio`, añadir:
  `implementation 'com.googlecode.libphonenumber:libphonenumber:8.13.55'` con el comentario de que **solo** `com.educaflow.base.util.NumeroTelefono` puede importarla.

**Verificar:** `./gradlew -q compileJava` resuelve la dependencia nueva;

> **Nota del descomponedor:** acción `Modificar` de un fichero no-Java y no-XML del diseño: se edita en sitio añadiendo **solo** la línea descrita y conservando todo lo demás.
