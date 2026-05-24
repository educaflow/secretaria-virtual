---
name: k-guice
description: Inyección de dependencias con Guice en la secretaría virtual sobre Axelor — cómo se declaran los módulos (`module/<Subsistema>Module.java`), las formas de binding (`bind`, `.to`, `.toProvider`, `@Provides`), cuándo una clase necesita un `Provider` porque sus dependencias vienen de configuración o de valores de runtime y no de otros beans inyectables, y cómo diagnosticar y resolver el error `Guice/MissingConstructor`. Distinto de `k-sistemas`, que describe la estructura del servicio; este skill describe cómo se cablea con Guice. Cárgalo siempre que diseñes o implementes el cableado DI de un sistema, sobre todo cuando la construcción de un objeto no es trivial.
---

# k-guice

Cubre el cableado de inyección de dependencias con **Guice** en la secretaría virtual sobre Axelor. Va dirigido al modelo cuando crea o revisa el `module/` de un sistema/subsistema, o cuando una clase no se puede instanciar por DI. Lo cargan `k-sistemas` y los `sdd-*` cuando la lógica de construcción de un objeto deja de ser trivial.

**CRITICAL**: el error más frecuente (`Guice/MissingConstructor`) aparece justo cuando la lógica es "más compleja" — un objeto cuyas dependencias **no** son otros beans, sino valores leídos de configuración o calculados en runtime. Ese caso **MUST** resolverse con un `Provider`, nunca con un `bind(...).to(...)` directo. Ver §4.

---

## 1. Conceptos clave

- **Guice instancia una clase solo si** tiene (a) un constructor sin argumentos, o (b) un constructor anotado con `@Inject`. Si el constructor pide parámetros y no lleva `@Inject`, Guice no sabe construirla → `MissingConstructor`.
- **Inyectable ≠ construible.** Aunque el constructor lleve `@Inject`, cada parámetro **MUST** ser a su vez un tipo que Guice sepa resolver (otro bean con binding). Un `String`, un `record` de configuración o un valor de runtime **no** son resolubles por defecto.
- **Módulo (`AxelorModule`)**: clase que extiende `com.axelor.app.AxelorModule` y declara bindings en `configure()`. Axelor **descubre automáticamente** todos los `AxelorModule` por `ServiceLoader`; **MUST NOT** registrarlos manualmente en `SecretariaVirtualModule`.
- **Provider**: objeto cuyo único método `get()` fabrica la instancia "a mano". Es la vía para encapsular lógica de construcción (leer config, elegir implementación, validar).

---

## 2. Convenciones del proyecto

- Cada sistema/subsistema con cableado propio tiene **un** módulo en `…/<subsistema>/module/<Subsistema>Module.java` (p. ej. `CorreosModule`, `CriptografiaModule`, `SecurityModule`).
- **`ModelService` NO se cablea con Guice.** Los servicios que extienden `ModelService<T>`/`DefaultModelService<T>` los instancia `ModelServiceFactory` por reflexión a partir de su ubicación en `service.impl`. **MUST NOT** hacer `bind(...)` de un `ModelService` ni inyectarlo con `@Inject`. Esto pertenece a `[[k-sistemas]]` (sección "Obtener otro servicio desde un servicio").
- **Todo lo que NO es `ModelService`** (infraestructura, clientes de terceros, implementaciones de interfaces como `MailSender`, loaders, observers) **sí** usa Guice con `@Inject` + binding en el `module/` del subsistema.
- **Secretos y configuración**: el binding que lee `AppSettings` (p. ej. `mail.smtp.*`) **MUST** estar encapsulado en un único `Provider`, no esparcido por servicios. Es el punto natural donde se respeta `[[k-secure-coding]]` (manejo de secretos).

---

## 3. Patrones recomendados

Elige la forma de binding según **cómo** se construye el objeto:

### 3.1 Clase concreta autoconstruible → `bind(Clase.class)`

La clase tiene constructor sin args o `@Inject` y todas sus dependencias son inyectables.

```java
@Override
protected void configure() {
    bind(AlmacenClaveResolver.class);   // CriptografiaModule
}
```

### 3.2 Interfaz cuya impl es autoconstruible → `bind(Iface.class).to(Impl.class)`

La `Impl` tiene constructor `@Inject` (o sin args) y **todas** sus dependencias son a su vez inyectables. Plantilla (sustituye por tus tipos reales):

```java
bind(MiServicio.class).to(MiServicioImpl.class);
```

### 3.3 Construcción con lógica → `Provider` (o `@Provides`)

**MUST** usar esta forma cuando la instancia se fabrica a partir de:
- valores de **configuración** (`AppSettings.get().get("…")`),
- valores de **runtime** (usuario actual, centro, fecha),
- una **elección** entre varias implementaciones,
- un `record`/objeto que **no** es un bean inyectable.

Provider en una clase propia del paquete `module/`:

```java
public class MailSenderProvider implements jakarta.inject.Provider<MailSender> {
    @Override
    public MailSender get() {
        AppSettings settings = AppSettings.get();
        SmtpCredentialSimplePassword credential = new SmtpCredentialSimplePassword(
                settings.get("mail.smtp.host"),
                settings.get("mail.smtp.user"),
                settings.get("mail.smtp.password"));
        return new MailSenderImpl(credential);
    }
}
```

```java
// En el módulo:
bind(MailSender.class).toProvider(MailSenderProvider.class);
```

Variante inline con `@Provides` (válida para lógica muy corta; el método vive en el propio módulo):

```java
@Provides
MailSender provideMailSender() {
    AppSettings s = AppSettings.get();
    return new MailSenderImpl(new SmtpCredentialSimplePassword(
            s.get("mail.smtp.host"), s.get("mail.smtp.user"), s.get("mail.smtp.password")));
}
```

### 3.4 Obtención mediante un parámetro de runtime → inyectar una factory y llamar a `resolve(...)`

Úsalo cuando el objeto que necesitas **no** se conoce al cablear, sino que se elige en tiempo de llamada según un **parámetro** (p. ej. la clase de entidad). No hay un binding por cada variante: se inyecta **una** factory y se le pide el objeto pasándole el parámetro.

El caso canónico del proyecto es `ModelServiceFactory`: inyectas **solo** la factory (es un bean normal) y obtienes el `ModelService` con `resolve(...)` dentro del método que lo usa.

```java
@Inject
private ModelServiceFactory modelServiceFactory;   // se inyecta la factory, no el servicio

// Dentro del método que lo necesite — el parámetro decide qué servicio devuelve:
final CorreoService correoService = (CorreoService) modelServiceFactory.resolve(Correo.class);
```

- **CRITICAL**: un `ModelService` **MUST** obtenerse **siempre** así, nunca con `@Inject` directo ni con `bind(...).to(...)` (§2). La resolución por reflexión a partir de la entidad pertenece a `[[k-sistemas]]`.
- La factory **sí** se inyecta con `@Inject` como cualquier otro bean (§3.1).
- Generaliza a cualquier objeto cuya construcción dependa de un valor de llamada: en vez de cablear N bindings, inyecta una factory con un método parametrizado.

---

## 4. El error `Guice/MissingConstructor` — diagnóstico y solución

### 4.1 Síntoma

```
[Guice/MissingConstructor]: No injectable constructor for type MailSenderImpl.
class MailSenderImpl does not have a @Inject annotated constructor or a no-arg constructor.
… at CorreosModule.configure(CorreosModule.java:11)
```

### 4.2 Causa

`bind(MailSender.class).to(MailSenderImpl.class)` ordena a Guice instanciar `MailSenderImpl`, pero su único constructor pide un `SmtpCredentialSimplePassword` y **no** lleva `@Inject`. Aunque se lo pusieras, Guice tampoco sabría fabricar el `record` de credenciales, porque sus valores vienen de configuración, **no** de otro bean.

**Regla de diagnóstico**: si la clase que falla necesita un valor que **no** es otro bean (un `String` de config, un `record`, algo de runtime), el `bind(...).to(...)` directo es la elección equivocada.

### 4.3 Solución

Sustituir el binding directo por un `Provider` que encapsule la construcción (§3.3). El `Provider` lee la configuración y llama al constructor a mano. La clase de implementación se mantiene **sin** `@Inject` y sin acoplarse a `AppSettings`.

### 4.4 Otras causas frecuentes del mismo error

- Constructor con parámetros pero **sin** `@Inject` y la intención era que Guice los inyectara → añade `@Inject` (solo si todos los parámetros son beans inyectables).
- Un parámetro del constructor `@Inject` que no tiene binding en ningún módulo → añade su `bind(...)`.
- Intentar bindear un `ModelService` → **MUST NOT**; se resuelve por `ModelServiceFactory`, no por Guice (§2, `[[k-sistemas]]`).

---

## 5. Ejemplos ✅/❌

- ✅ CORRECTO: `bind(MailSender.class).toProvider(MailSenderProvider.class);` (la impl necesita credenciales de config → Provider).
- ✅ CORRECTO: `bind(AlmacenClaveResolver.class);` (clase concreta autoconstruible, sus deps son beans).
- ✅ CORRECTO: `@Inject private MailSender mailSender;` en un servicio que **no** es `ModelService` (binding existe en el módulo).
- ❌ INCORRECTO: `bind(MailSender.class).to(MailSenderImpl.class);` cuando `MailSenderImpl` exige un `SmtpCredentialSimplePassword` (Guice no puede fabricar el record → `MissingConstructor`).
- ❌ INCORRECTO: `bind(CorreoService.class).to(CorreoServiceImpl.class);` (`CorreoServiceImpl` es un `ModelService`; lo instancia `ModelServiceFactory`, no Guice).
- ❌ INCORRECTO: leer `AppSettings.get().get("mail.smtp.user")` dentro del servicio de negocio (la lectura de config **MUST** encapsularse en el Provider, no dispersarse).

---

## 6. Anti-patrones

- **MUST NOT** poner `@Inject` en un constructor cuyos parámetros no son beans inyectables solo para "callar" el error — fallará igual en runtime al no haber binding para esos tipos. Usa un `Provider`.
- **MUST NOT** registrar los `AxelorModule` en otro módulo; Axelor los descubre por `ServiceLoader`.
- **MUST NOT** mover la lógica de lectura de configuración a los servicios para evitar crear un Provider. Centralízala en el `Provider` del `module/`.
- **MUST NOT** usar Guice para `ModelService`. Pertenece a `ModelServiceFactory` (`[[k-sistemas]]`).
- **MUST NOT** crear un binding `toInstance(...)` con un objeto que dependa del usuario/centro/petición actual: eso es un singleton de arranque y filtra estado entre peticiones. Usa un `Provider` que lo resuelva en cada `get()`.

---

## Quick Guidelines

- Guice solo instancia con constructor sin args o `@Inject`; cada parámetro `@Inject` **MUST** ser otro bean resoluble.
- Un módulo por subsistema en `module/<Subsistema>Module.java`; los `AxelorModule` se autodescubren — **MUST NOT** registrarlos a mano.
- `ModelService` **MUST NOT** cablearse con Guice: lo resuelve `ModelServiceFactory` (`[[k-sistemas]]`). El resto sí usa `@Inject` + binding.
- Si la construcción necesita config/runtime/elección/`record` → usa `Provider` o `@Provides`, nunca `bind(...).to(...)` directo.
- `MissingConstructor` ⇒ la clase pide un valor que no es un bean → muévelo a un `Provider` que lea `AppSettings` y llame al constructor a mano.
- La lectura de `AppSettings`/secretos **MUST** centralizarse en el `Provider`, no dispersarse por los servicios (`[[k-secure-coding]]`).