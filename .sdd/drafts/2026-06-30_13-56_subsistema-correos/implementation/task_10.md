---
type: implementation-task
---

# Tarea 10 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-guice

## Ficheros que cubre esta tarea (filas de la tabla "Ficheros a crear o modificar" de `design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/correos/module/CorreosModule.java` | Crear | k-guice | Módulo Guice: bindings de `MailSender`, `CorreoAsyncExecutor`, `CorreoEventObserver` |
| `src/main/java/com/educaflow/subsystem/correos/module/MailSenderProvider.java` | Crear | k-guice | `Provider<MailSender>` que lee la configuración SMTP |
| `src/main/java/com/educaflow/subsystem/correos/module/CorreoAsyncExecutorProvider.java` | Crear | k-guice | `Provider<CorreoAsyncExecutor>` que lee `mail.send.pool-size` |

## Texto del diseño (verbatim, `design.md`, Paso 6 — Módulo Guice)

El envío asíncrono necesita tres piezas que **no** son `ModelService` y cuya construcción no es trivial (dependen de configuración): `MailSender` (credenciales SMTP), `CorreoAsyncExecutor` (tamaño de pool) y el observador de ciclo de vida. Ver `[[k-guice]]`.

#### `com.educaflow.subsystem.correos.infrastructure.CorreoEventObserver`

(clase ya materializada en la Tarea 09 — **no** se toca en esta tarea, se referencia solo por el `bind(CorreoEventObserver.class)` de `CorreosModule`)

#### `com.educaflow.subsystem.correos.module.MailSenderProvider`

```java
package com.educaflow.subsystem.correos.module;

public class MailSenderProvider implements jakarta.inject.Provider<com.educaflow.base.infrastructure.mail.MailSender> {
    @Override
    public com.educaflow.base.infrastructure.mail.MailSender get();
    //   com.axelor.app.AppSettings settings = com.axelor.app.AppSettings.get();
    //   var credencial = new SmtpCredentialSimplePassword(settings.get("mail.smtp.host"),
    //                                                      settings.get("mail.smtp.user"),
    //                                                      settings.get("mail.smtp.password"));
    //   return new MailSenderImpl(credencial);
    //   (mismo patrón que el ejemplo canónico de [[k-guice]] §3.3; centraliza la lectura de
    //   AppSettings/credenciales en el Provider, no dispersa por el servicio — k-secure-coding §8)
}
```

#### `com.educaflow.subsystem.correos.module.CorreoAsyncExecutorProvider`

```java
package com.educaflow.subsystem.correos.module;

public class CorreoAsyncExecutorProvider implements jakarta.inject.Provider<CorreoAsyncExecutor> {
    @Override
    public CorreoAsyncExecutor get();
    //   int tamanoPool = com.axelor.app.AppSettings.get().getInt("mail.send.pool-size", 2);
    //   return new CorreoAsyncExecutor(tamanoPool);
}
```

#### `com.educaflow.subsystem.correos.module.CorreosModule`

```java
package com.educaflow.subsystem.correos.module;

public class CorreosModule extends com.axelor.app.AxelorModule {
    @Override
    protected void configure();
    //   bind(com.educaflow.base.infrastructure.mail.MailSender.class).toProvider(MailSenderProvider.class);
    //   bind(CorreoAsyncExecutor.class).toProvider(CorreoAsyncExecutorProvider.class).in(com.google.inject.Singleton.class);
    //   bind(CorreoEventObserver.class);
    //   MUST NOT bindear CorreoService/AdjuntoService (son ModelService — los resuelve
    //   ModelServiceFactory, ver k-guice §2).
}
```

**Verificar:** `./run.sh` arranca sin `Guice/MissingConstructor` ni errores de bindeo; el log de arranque no muestra excepciones de `CorreosModule`.

### Nota y supuesto aplicable (verbatim, `design.md`)

3. **Se añade el primer binding real de `MailSender` del proyecto.** La búsqueda en el código existente confirma que `MailSender` no tiene hoy ningún binding de Guice (aunque `RegistroSalidaServiceImpl` ya lo inyecta con `@Inject`, lo que sugiere una carencia previa no relacionada con este subsistema). El `Provider` de `CorreosModule` es, de hecho, el primer binding real de `MailSender` en toda la aplicación — como efecto colateral positivo, también arregla ese hueco para `registroentradasalida`. Si en el futuro se decide que ese binding debe vivir en un módulo más genérico (p.ej. `base/infrastructure`), basta moverlo sin tocar `correos`.

## Superficie cerrada

**MUST** crear únicamente estas tres clases (`CorreosModule`, `MailSenderProvider`, `CorreoAsyncExecutorProvider`) con exactamente los métodos listados. `MailSender`/`MailSenderImpl` (Tarea 01), `CorreoAsyncExecutor` (Tarea 07) y `CorreoEventObserver` (Tarea 09) ya están en el árbol y son contrato fijo — **MUST NOT** modificarlos aquí. **MUST NOT** bindear `CorreoService`/`AdjuntoService` en este módulo (los resuelve `ModelServiceFactory`). Si detectas que hace falta algo no listado, **detente y reporta** `BLOCKED`.
