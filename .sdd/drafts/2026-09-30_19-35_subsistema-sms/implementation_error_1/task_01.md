---
type: implementation-task
template: system
---

# Tarea 01 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality

### Fichero(s) de esta tarea (de «Ficheros a crear o modificar» de `design/design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `build.gradle` | Modificar | — | Dependencia `com.googlecode.libphonenumber:libphonenumber` (solo la usa `NumeroTelefono`) |

### Paso 1 — Dependencia y propiedades de configuración

**Fichero:** `build.gradle` (Modificar) y `src/main/resources/axelor-config.properties` (Modificar).

- En `build.gradle`, junto al bloque `//Envio de SMS con Twilio`, añadir:
  `implementation 'com.googlecode.libphonenumber:libphonenumber:8.13.55'` con el comentario de que **solo** `com.educaflow.base.util.NumeroTelefono` puede importarla.
- En `axelor-config.properties`, un bloque nuevo con **exactamente** las tres propiedades que enumera
  `design-guidelines.md`:

  ```properties
  #Envio de SMS desde la secretaría virtual
  sms.credentials.twilio.accountSid=
  sms.credentials.twilio.authToken=
  sms.twilio.from=
  ```

  Las tres van **vacías**: los valores reales viven en la configuración privada
  (`../secretaria-virtual-private/axelor-config.dev.properties`), que sobrescribe este fichero.
  **MUST NOT** escribir aquí ninguna credencial real (`k-secure-coding` §8).
  **MUST NOT** añadir ninguna propiedad de pool: el subsistema **no tiene pool propio** (paso 3), y el
  tamaño del único pool de la aplicación lo lee `EjecutorAsincronoProvider` de `async.pool-size` con el
  default `2`, igual que hoy `CorreoAsyncExecutorProvider` lee `mail.send.pool-size` sin que esa clave
  esté versionada aquí (ver «Notas y supuestos» 7).

**Verificar:** `./gradlew -q compileJava` resuelve la dependencia nueva;
`grep -n "sms\." src/main/resources/axelor-config.properties` muestra las tres propiedades y ninguna con valor.

### Decisión del descomponedor (no es texto del diseño)

Esta tarea cubre solo `build.gradle` (la parte de la dependencia del Paso 1); las propiedades de `axelor-config.properties` del mismo paso son otra tarea. La tabla del diseño no asigna skill a esta fila (`—`); se lista `k-code-quality` solo como referencia general (no es código Java). Es una modificación puntual de un fichero existente: **MUST** conservarse todo lo demás de `build.gradle`.
