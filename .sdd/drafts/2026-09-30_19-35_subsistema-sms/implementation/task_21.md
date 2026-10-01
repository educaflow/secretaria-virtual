---
type: implementation-task
template: system
---

# Tarea 21 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-guice
- k-secure-coding
- k-code-quality

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/sms/module/SmsModule.java` | Crear | k-guice | Un único binding: el `SmsSender` (ningún `ModelService`, ningún pool propio) |
| `src/main/java/com/educaflow/subsystem/sms/module/SmsSenderProvider.java` | Crear | k-guice | `SmsSender` de Twilio a partir de las propiedades de configuración |

### Paso 7 — Cableado Guice: módulo y provider del subsistema

**Ficheros:** `subsystem/sms/module/SmsSenderProvider.java` y `subsystem/sms/module/SmsModule.java` (los dos Crear).

El subsistema **no aporta ninguna clase de infraestructura asíncrona**: el pool es el `EjecutorAsincrono` único de la aplicación (paso 3), que `SmsServiceImpl` se limita a inyectar. `SmsSenderProvider` está descrito en `design/rules/R-Sms-003.md` §«Clases nuevas» (firma y comentario de cada método).

`SmsModule` tiene **un único binding, y ninguno más**, y va sin `Singleton` porque el emisor se fabrica en cada intento de envío. El `Provider` es obligatorio (no basta `bind(...).to(...)`): construye el emisor a partir de propiedades de configuración, no de otros beans (`k-guice` §3.3/§4.2, `decisiones.md` D4). Además, `SmsModule` **MUST NOT** bindear `SmsServiceImpl` ni ningún otro `ModelService` (los descubre `ModelServiceFactory`), **MUST NOT** bindear ni subclasear el `EjecutorAsincrono` (lo bindea `SecretariaVirtualModule` una sola vez para toda la aplicación, paso 3, y aquí solo se inyecta) y **MUST NOT** registrarse en `SecretariaVirtualModule`: Axelor descubre los `AxelorModule` solo.

```java
// Clase: com.educaflow.subsystem.sms.module.SmsModule extends AxelorModule
protected void configure();
//   bind(SmsSender.class).toProvider(SmsSenderProvider.class);
```

**Verificar:** la aplicación arranca (`./run.sh`) sin `Guice/MissingConstructor`; al parar, el pool se detiene sin dejar hilos `async-*`; `grep -rn "bind(" src/main/java/com/educaflow/subsystem/sms/module/SmsModule.java` devuelve **una** línea.



De `design/rules/R-Sms-003.md`:

### Clases nuevas

El ejecutor asíncrono que usa esta regla es el `EjecutorAsincrono` **único** de la aplicación; su
contrato y su cableado están en `design.md` paso 3.

- `com.educaflow.subsystem.sms.module.SmsSenderProvider implements Provider<SmsSender>`
  - `public SmsSender get()` — construye `TwilioCredential` con `sms.credentials.twilio.accountSid` y
    `sms.credentials.twilio.authToken` y devuelve
    `SmsSenderFactory.getTwilioSmsSender(credencial, settings.get("sms.twilio.from"))`.



| Condición | Origen | Tratamiento |
|-----------|--------|-------------|
| Credenciales del proveedor ausentes o vacías (`TwilioCredential` las rechaza) | `SmsSenderProvider.get()`, **dentro** del `try` | R-Sms-006: FALLIDO con la traza. Por eso el servicio inyecta `Provider<SmsSender>` y no `SmsSender` (`decisiones.md` D4): si se inyectara el emisor, el fallo ocurriría al construir el servicio y el alta moriría con un error técnico en vez de dejar el SMS FALLIDO |

Decisión de diseño citada: `design/decisiones.md` D4.

5. **`SmsSender` se inyecta como `Provider<SmsSender>`**, no como `SmsSender` (correos hace lo segundo). Ver `decisiones.md` D4.

> **Nota del descomponedor:** se añade `k-secure-coding` porque `SmsSenderProvider` lee las credenciales del proveedor (manejo de secretos, §8).
