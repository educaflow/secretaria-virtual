---
type: implementation-task
template: system
---

# Tarea 02 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-secure-coding

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/resources/axelor-config.properties` | Modificar | — | `sms.credentials.twilio.accountSid`, `sms.credentials.twilio.authToken` y `sms.twilio.from` (las tres vacías); `mail.send.pool-size` se sustituye por `async.pool-size` |

### Paso 1 — Dependencia y propiedades de configuración

**Fichero:** `build.gradle` (Modificar) y `src/main/resources/axelor-config.properties` (Modificar).

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
- En `axelor-config.properties`, **sustituir** la línea `mail.send.pool-size = 2` y su comentario
  `#Envio de correos desde la secretaría virtual` (bloque de Quartz) por:

  ```properties
  #Pool compartido de tareas en segundo plano (EjecutorAsincrono: envío de correos y de SMS)
  async.pool-size = 2
  ```

  Es la clave que lee `EjecutorAsincronoProvider` (paso 3); `mail.send.pool-size` deja de leerse y **MUST NOT**
  quedarse en el fichero.

`grep -n "sms\." src/main/resources/axelor-config.properties` muestra las tres propiedades y ninguna con valor;
`grep -n "pool-size" src/main/resources/axelor-config.properties` muestra solo `async.pool-size = 2`.



## Eliminaciones declaradas

| Elemento eliminado | Fichero | Justificación |
|---|---|---|
| Clase `CorreoAsyncExecutorProvider` | `src/main/java/com/educaflow/subsystem/correos/module/CorreoAsyncExecutorProvider.java` | — (mismo motivo). Lo sustituye `EjecutorAsincronoProvider`, el único sitio del proyecto que lee un tamaño de pool. **Efecto de configuración:** la clave `mail.send.pool-size` deja de leerse y la sustituye `async.pool-size` (mismo default, 2); la clave se sustituye en `axelor-config.properties` (paso 1) |

> **Nota del descomponedor:** acción `Modificar`: se edita en sitio con **solo** los cambios descritos. Se añade `k-secure-coding` porque el diseño exige aplicar su §8 (ninguna credencial real en el fichero versionado).
