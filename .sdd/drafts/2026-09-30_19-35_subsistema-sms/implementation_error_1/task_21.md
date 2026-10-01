---
type: implementation-task
template: system
---

# Tarea 21 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-guice
- k-code-quality

### Fichero(s) de esta tarea (de «Ficheros a crear o modificar» de `design/design.md`)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/sms/module/SmsModule.java` | Crear | k-guice | Un único binding: el `SmsSender` (ningún `ModelService`, ningún pool propio) |
| `src/main/java/com/educaflow/subsystem/sms/module/SmsSenderProvider.java` | Crear | k-guice | `SmsSender` de Twilio a partir de las propiedades de configuración |

### Paso 7 — Cableado Guice: módulo y provider del subsistema (de `design/design.md`)

**Ficheros:** `subsystem/sms/module/SmsSenderProvider.java` y `subsystem/sms/module/SmsModule.java` (los dos Crear).

El subsistema **no aporta ninguna clase de infraestructura asíncrona**: el pool es el `EjecutorAsincrono` único de la aplicación (paso 3), que `SmsServiceImpl` se limita a inyectar. `SmsSenderProvider` está descrito en `design/rules/R-Sms-003.md` §«Clases nuevas» (firma y comentario de cada método).

`SmsModule` tiene **un único binding, y ninguno más**, y va sin `Singleton` porque el emisor se fabrica en cada intento de envío. El `Provider` es obligatorio (no basta `bind(...).to(...)`): construye el emisor a partir de propiedades de configuración, no de otros beans (`k-guice` §3.3/§4.2, `decisiones.md` D4). Además, `SmsModule` **MUST NOT** bindear `SmsServiceImpl` ni ningún otro `ModelService` (los descubre `ModelServiceFactory`), **MUST NOT** bindear ni subclasear el `EjecutorAsincrono` (lo bindea `SecretariaVirtualModule` una sola vez para toda la aplicación, paso 3, y aquí solo se inyecta) y **MUST NOT** registrarse en `SecretariaVirtualModule`: Axelor descubre los `AxelorModule` solo.

```java
// Clase: com.educaflow.subsystem.sms.module.SmsModule extends AxelorModule
protected void configure();
//   bind(SmsSender.class).toProvider(SmsSenderProvider.class);
```

**Verificar:** la aplicación arranca (`./run.sh`) sin `Guice/MissingConstructor`; al parar, el pool se detiene sin dejar hilos `async-*`; `grep -rn "bind(" src/main/java/com/educaflow/subsystem/sms/module/SmsModule.java` devuelve **una** línea.

### Clases nuevas

El ejecutor asíncrono que usa esta regla es el `EjecutorAsincrono` **único** de la aplicación; su
contrato y su cableado (`EjecutorAsincronoProvider`, el `bind` de `SecretariaVirtualModule` y la parada
desde `AppEventObserver`) están en `design.md` paso 3 — aquí **no** se declara ninguna clase de
infraestructura asíncrona: `SmsModule` **MUST NOT** bindearla ni crear pool, `Provider` ni observer
propios; `SmsServiceImpl` la **inyecta** tal cual.

- `com.educaflow.subsystem.sms.module.SmsSenderProvider implements Provider<SmsSender>`
  - `public SmsSender get()` — construye `TwilioCredential` con `sms.credentials.twilio.accountSid` y
    `sms.credentials.twilio.authToken` y devuelve
    `SmsSenderFactory.getTwilioSmsSender(credencial, settings.get("sms.twilio.from"))`.
    **MUST NOT** ir a `Singleton`: se fabrica en cada intento de envío (ver «Errores»).

### Notas y supuestos 6 (de `design/design.md`)

6. **`SmsSender` se inyecta como `Provider<SmsSender>`**, no como `SmsSender` (correos hace lo segundo). `TwilioCredential` rechaza credenciales blancas y este diseño las deja vacías en `axelor-config.properties`, así que inyectar el emisor haría fallar la construcción del servicio y ni se podría dar de alta un SMS; con el `Provider` la falta de credenciales cae por el camino normal y el SMS queda FALLIDO con su descripción, que es lo que piden ESC-001 y ESC-020. Ver `decisiones.md` D4.

### Decisión del descomponedor (no es texto del diseño)

`SmsModule` y su `SmsSenderProvider` van juntos (el módulo solo bindea ese provider: un único componente lógico de cableado, descrito junto en el Paso 7). La fila de `SmsSenderProvider` remite a `design/rules/R-Sms-003.md` §«Clases nuevas», copiado arriba.
