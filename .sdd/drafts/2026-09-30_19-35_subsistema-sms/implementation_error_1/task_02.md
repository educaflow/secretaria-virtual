---
type: implementation-task
template: system
---

# Tarea 02 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-secure-coding

### Fichero(s) de esta tarea (de «Ficheros a crear o modificar» de `design/design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/resources/axelor-config.properties` | Modificar | — | `sms.credentials.twilio.accountSid`, `sms.credentials.twilio.authToken` y `sms.twilio.from` (las tres vacías) |

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

### Notas y supuestos 7 (de `design/design.md`)

7. **La clave del tamaño del pool NO se versiona, y ahora es UNA sola.** `axelor-config.properties` lleva solo las **tres** propiedades que enumeran las guías (las que llevan secreto y van vacías); las guías fijan exactamente esas tres para este trabajo. El tamaño del único pool de la aplicación lo lee `EjecutorAsincronoProvider` con `AppSettings.get().getInt("async.pool-size", 2)`, igual que hoy hace `CorreoAsyncExecutorProvider` con `mail.send.pool-size`, que **tampoco** está en el fichero versionado (`grep -n "mail" src/main/resources/axelor-config.properties` no devuelve nada). Quien quiera otro tamaño pone la clave en su configuración privada; así hay un único sitio donde está escrito el valor por defecto y no dos que puedan divergir. Con el ejecutor único, además, la aplicación pasa de **dos** claves de pool (`mail.send.pool-size` y la que habría traído SMS) a **una**, que es exactamente lo contrario de esconder configuración. **Nota para `/sdd-implementer`:** si en el futuro se decide versionar esta clave, va en `src/main/resources/axelor-config.properties` (`CLAUDE.md` § Configuración) y como `async.pool-size=2`, **nunca** como una clave por subsistema.

### Decisión del descomponedor (no es texto del diseño)

Esta tarea cubre solo `axelor-config.properties` (la parte de propiedades del Paso 1). La tabla no asigna skill (`—`); se lista `k-secure-coding` porque el paso cita su §8 (ninguna credencial real en el fichero versionado). Es una modificación puntual: **MUST** conservarse todo el contenido existente.
