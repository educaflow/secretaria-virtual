---
name: k-secure-coding
description: Reglas de codificación segura para la secretaría virtual sobre Axelor. Cubre el modelo de confianza cliente↔servidor (endpoint REST automático `/ws/rest/<FQN>` y por qué `readonly`/`showIf`/`hidden` en XML **NO son defensa**), el control de qué campos puede dictar el cliente (la defensa central que unifica `AllowProperties`, sobrescritura incondicional en el servicio y restauración de campos inmutables), autorización multi-centro / IDOR, inyección JPQL/SQL, log injection, validación de adjuntos y manejo de secretos. **CRITICAL**: este skill define defensas que protegen al resto del sistema; saltarse cualquiera de sus reglas introduce fallos de seguridad explotables incluso si el resto del análisis/diseño/código es correcto. Distinto de `k-seguridad`, que describe el modelo de roles/ACL del negocio; este skill describe cómo se escribe el código para que ese modelo no se pueda saltar.
---

# k-secure-coding

Eres autor o revisor de código de la secretaría virtual. Tu trabajo es aplicar las reglas de este skill al escribir o revisar Java, XML, JPQL o configuración del proyecto. **CRITICAL**: aplicación obligatoria en todo el pipeline SDD y en cualquier modificación de código que toque entidades, servicios o controladores. Saltarse una regla no es un fallo de estilo: es un fallo de seguridad explotable.

> **No confundir con `k-seguridad`**. Ese skill describe **qué** puede hacer cada rol (modelo de permisos del negocio). Este skill describe **cómo** se escribe el código para que esos permisos no se puedan saltar. Sin `k-secure-coding`, el modelo de `k-seguridad` se puede eludir desde Postman aunque esté bien diseñado.

---

## 1. Modelo mental: por dónde entra el ataque

Antes de cualquier regla, hay que tener clarísimo el flujo real de una petición de guardado en Axelor. Casi todas las reglas del skill se entienden mirando este diagrama:

```
┌─ Cliente (browser, Postman, curl, otro servicio) ──────────────────┐
│                                                                    │
│  Vía A: el botón de la UI                                          │
│    POST /ws/action/<accion> → controller @CallMethod               │
│      ↳ filtra campos con AllowProperties                           │
│      ↳ delega en *ServiceImpl                                      │
│                                                                    │
│  Vía B: endpoint REST genérico (Postman, curl)                     │
│    POST /ws/rest/<FQN>     ← JSON ENTERO del bean, SIN pasar       │
│                              por el controller ni AllowProperties  │
│      ↳ Axelor deserializa JSON → bean                              │
│      ↳ llama directamente a Repository.save() → *ServiceImpl       │
│                                                                    │
└────────────────────────────┬───────────────────────────────────────┘
                             │
                             ▼
            ┌────────────────────────────────────┐
            │  *ServiceImpl.insert/update/remove │  ← ÚNICA capa que
            │    + validateInsert/Update/Remove  │    siempre se ejecuta
            │      (defensa en profundidad)      │
            └────────────────────────────────────┘
                             │
                             ▼
                            DB
```

Hechos clave que se derivan del diagrama y que **MUST** asumir como verdad operativa:

1. **Axelor expone automáticamente un endpoint REST por cada entidad JPA** (`POST /ws/rest/<FQN>`, `GET /ws/rest/<FQN>/<id>`, etc.). No hay que configurarlo: existe por el simple hecho de tener una entidad mapeada. Un atacante con `curl` puede mandar el JSON entero del bean.
2. **Ese endpoint NO pasa por el controller**, así que **NO pasa por `AllowProperties`**. El `@CallMethod` con `AllowProperties` solo defiende cuando la petición llega por la Vía A (botón de la UI con `<action-method>`).
3. **La capa `JpaSecurity`/`MetaPermissions` de Axelor** sí se ejecuta en ambas vías y controla **quién** puede invocar el save (`READ`/`WRITE`/`CREATE`/`REMOVE` por modelo y opcionalmente por campo), pero **no valida los VALORES del JSON**. Un usuario autorizado con permiso `WRITE` puede mass-assignar cualquier campo del bean si nadie más le pone freno.
4. **La única capa universal donde puedes sobrescribir y validar campo a campo es `*ServiceImpl`** (con sus `insert/update/remove` y sus `validateInsert/Update/Remove`). Aquí pasan **todas** las peticiones, vengan del botón o de Postman.

Consecuencia: la UI (`readonly`, `showIf`, `hidden`, `required`, validaciones JS) es **UX**, no defensa. Un atacante con `curl` la ignora completamente. **MUST NOT** tratar como defensa de seguridad ninguno de estos mecanismos:

- `readonly="true"` / `readonlyIf="..."` en XML de vistas.
- `hidden="true"` / `showIf="..."` / `hideIf="..."`.
- `required="true"` (sirve para UX, no para integridad).
- Validaciones JavaScript en el navegador.
- Que un campo no aparezca en la pantalla actual o el botón esté oculto.

**Regla absoluta**: el servidor es la única fuente de verdad. Asume que cualquier valor que llega del cliente es **hostil** hasta demostrar lo contrario. Esto incluye el JSON del body, los query params, las cookies y headers (salvo los de autenticación), los nombres de fichero y `Content-Type` de uploads, y `actionRequest.getData()` en los `@CallMethod`.

---

## 2. Vocabulario

El skill asume estos términos. Si alguno no te resulta familiar, refresca el skill correspondiente antes de seguir.

| Término                                  | Qué es / dónde vive                                                                                                                                                                      | Cuándo se ejecuta                                                            |
|------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------|
| `*ServiceImpl.insert/update/remove`      | Implementación del servicio de la entidad. Único punto donde **todo** save acaba pasando. Definido en `k-sistemas`.                                                                      | En toda petición que persiste (botón de UI o REST genérico).                 |
| `validateInsert/Update/Remove`           | Métodos hermanos del servicio que devuelven `Optional<BusinessMessages>`. Aquí van las reglas `V-…`. Ver `k-validaciones`.                                                               | Justo antes de `insert/update/remove`. Si devuelven mensaje, el save aborta. |
| `@CallMethod`                            | Anotación de Axelor en métodos de un controller que se invocan desde una `<action-method>` de la UI.                                                                                     | Solo cuando la petición llega por la Vía A (botón de UI).                    |
| `AllowProperties`                        | Clase de proyecto (`base/util/AllowProperties.java`) que filtra qué campos del JSON entrante llegan al bean. Se usa **dentro** de los `@CallMethod`.                                     | Solo dentro del controller, nunca en el endpoint REST genérico.              |
| `ActionRequest` / `ActionResponse`       | Objetos que Axelor pasa al `@CallMethod` con los datos del cliente. `actionRequest.getContext()` y los DTOs de `AllowProperties` se extraen de aquí.                                     | Solo en la Vía A.                                                            |
| `AuthUtils.getUser()`                    | API de Axelor para obtener el usuario autenticado del servidor. **Nunca** del JSON.                                                                                                      | Siempre disponible en código del servidor.                                   |
| `centroActivo`                           | Campo del `User` extendido (`subsystem/common/domains/User.xml`) con el centro actualmente seleccionado por el usuario. Helpers: `getCentroUsuarioActivo()`, `getTiposUsuarioActivos()`. | Es el dato que se usa para filtrar multi-centro.                             |
| `JPA.all(X.class).filter(...).bind(...)` | API de consulta JPQL de Axelor. **MUST** usar `:param` con `bind`, nunca concatenar.                                                                                                     | En repositorios y servicios.                                                 |
| `BusinessMessages`                       | Estructura de mensajes de error de negocio devueltos por `validate*`. Definido en `base/infrastructure/validation`.                                                                      | Carga útil de cualquier validación que rechaza.                              |
| `V-<E>-NNN` / `R-<E>-NNN` / `U-<E>-NNN`  | Identificadores de validaciones, reglas de negocio y reglas de UI sobre una entidad. Definidos en `entity-*.md` del análisis SDD. Detalle en `k-validaciones`.                           | Trazabilidad entre análisis, diseño y código.                                |
| Columna "Origen del valor"               | Columna obligatoria del `entity-*.md` que clasifica cada campo en `cliente` o `servidor`. Es el input del que parte §3 de este skill.                                                    | En el análisis SDD; el diseñador y el implementador la consultan.            |

---

## 3. Cómo implementar `allowPropertiesXxx` seguros

> **CRITICAL**: defensa central contra mass-assignment. Aplicación obligatoria.

### 3.1 Origen de cada campo

Cada campo de la entidad está clasificado en `entity-*.md` (columna "Origen del valor"):

- **`cliente`** — lo aporta el usuario en el JSON (`asunto`, `cuerpo`).
- **`servidor`** — lo asigna el servidor: reloj, usuario logueado, cálculo (`fechaCreacion`, `estado`, `centro`).

**Regla absoluta**: el cliente NUNCA dicta un campo `servidor`. El servicio tiene que honrarlo.

### 3.2 Elegir la forma del `allowPropertiesXxx`

| Forma | Cuándo |
|---|---|
| `createAllowProperties(Map.of(...))` (**whitelist**) | Enumera solo los `cliente` que esta acción acepta. **Obligatorio** si hay algún `servidor` que la acción no asigna. |
| `createAllowAllProperties()` (**abierto**) | Admisible **solo** si la acción asigna incondicionalmente **todos** los `servidor` de la entidad (o si no hay ninguno). |

Un campo `servidor` que la acción no asigna se queda **fuera** de la whitelist: el cliente no puede enviarlo.

✅ `insert` que asigna todos los `servidor`:

```java
@Override
public AllowProperties allowPropertiesInsert() {
    return AllowProperties.createAllowAllProperties();
}
```

✅ `update` que no recalcula `fechaCreacion`:

```java
@Override
public AllowProperties allowPropertiesUpdate() {
    return AllowProperties.createAllowProperties(Map.of(
        "asunto", Map.of(),
        "cuerpo", Map.of()
        // fechaCreacion fuera: campo servidor que update no toca
    ));
}
```

### 3.3 Asignar campos `servidor` en la acción

Asignación **incondicional**, sin guardas, dentro del cuerpo de la acción:

```java
correo.setFechaCreacion(LocalDateTime.now());
correo.setEstado(EstadoCorreo.PENDIENTE);
correo.setCentro(AuthUtils.getUser().getCentroActivo());
```

**MUST NOT** envolver en `if (campo == null)`: respeta el valor del cliente y rompe la defensa incluso si la whitelist está bien.

### 3.4 Detectores mecánicos (code-review)

- `if (entidad.getCampo() == null) entidad.setCampo(...)` con campo `servidor` → **bloquear**.
- `createAllowAllProperties()` en acción que no asigna todos los `servidor` → **vulnerabilidad**.
- `createAllowProperties(Map.of(...))` que enumera un campo `servidor` → **bloquear**.

### 3.5 Excepción: alta programática vía DTO

Si la entrada viene de otro subsistema con un `record`/DTO propio (no del REST), el DTO ya **es** la whitelist; no hace falta `AllowProperties`. Cualquier campo `servidor` en el DTO sin justificación es un error de diseño.

---

## 4. Multi-centro / IDOR

La aplicación es multicentro. Un usuario pertenece a uno o varios centros y en cada momento tiene **un centro activo** (`User.centroActivo`). Toda consulta que devuelve entidades de un centro **MUST** filtrar por el centro activo del usuario autenticado, obtenido del servidor.

**MUST** obtener el centro del servidor:

```java
AuthUtils.getUser().getCentroActivo()    // Java (servicio/repositorio)
```

En un `<domain>` de XML **MUST NOT** usar `:__user__.centroActivo` (parámetro **con punto**): Hibernate **no admite puntos en nombres de parámetro**, así que el filtro no se resuelve y el listado sale **vacío** (o lanza `no viable alternative at input '.'`). `:__user__` solo se admite **sin punto** (objeto `User` completo); para acceder a un campo del usuario define un `<context>` y referéncialo como `:nombrePlano` (ver ejemplo ✅ abajo).

**MUST NOT** confiar en el `centro` que viene del cliente. Postman puede mandar el centro de otro tenant.

✅ CORRECTO (Java, repositorio):

```java
JPA.all(Correo.class)
   .filter("self.centro = :centro AND self.id = :id")
   .bind("centro", AuthUtils.getUser().getCentroActivo())
   .bind("id", id)
   .fetchOne();
```

✅ CORRECTO (XML, `<action-view>`): el campo del usuario se pasa por un `<context>`, y el `<domain>` va **antes** que el `<context>` (lo exige el XSD del `<action-view>`):

```xml
<domain>self.centro = :centroActivoUsuario</domain>
<context name="centroActivoUsuario" expr="eval: __user__?.centroActivo"/>
```

❌ INCORRECTO (IDOR — el usuario del centro A lee del centro B con un id válido):

```java
JPA.all(Correo.class).filter("self.id = :id").bind("id", id).fetchOne();
```

❌ INCORRECTO (API inventada / parámetro con punto que no resuelve):

```java
AuthUtils.getUser().getCentro()                       // no existe, es getCentroActivo()
<domain>self.centro = :__user__.centro</domain>       // el campo del User es centroActivo
<domain>self.centro = :__user__.centroActivo</domain> // punto en el parámetro: Hibernate no lo resuelve → listado vacío. Usar <context>
```

> Nota: `self.centro` se refiere al campo `centro` **de la entidad consultada** (p.ej. `Correo.centro`), que sí se llama así. El que se llama `centroActivo` es el del `User`.

**Asignación del centro al crear**: si el centro lo dicta el servidor (caso normal de no-administradores), tratar como campo `servidor` y sobrescribir incondicionalmente en `insert` desde `AuthUtils.getUser().getCentroActivo()` (§3.3). Si lo dicta un Administrador desde un dropdown, tratar como `cliente` con la validación correspondiente.

**Administradores**: el rol Administrador puede ver/operar sobre varios centros. Se modela con una rama explícita (`if (esAdministrador(user)) { ... } else { filtrar por centroActivo }`), nunca eliminando el filtro indiscriminadamente.

**Multi-pertenencia**: `User.centroUsuarios` puede tener varias entradas. Validaciones basadas en tipo de usuario **MUST** consultar `getCentroUsuarioActivo()` / `getTiposUsuarioActivos()` (helpers del dominio extendido), no asumir un único `CentroUsuario` por usuario.

---

## 5. Inyección JPQL / SQL

**MUST** usar siempre parámetros nombrados (`:param`) en JPQL y bindings (`?`) en SQL. **MUST NOT** concatenar input del usuario en una query, ni siquiera para valores que parezcan controlados (un enum llegado del cliente puede ser un string arbitrario antes de que el setter lo valide).

✅ CORRECTO:

```java
JPA.all(Correo.class)
   .filter("self.asunto LIKE :patron AND self.estado = :estado")
   .bind("patron", "%" + filtro + "%")
   .bind("estado", EstadoCorreo.ENVIADO)
   .fetch();
```

❌ INCORRECTO (inyección JPQL clásica):

```java
String jpql = "self.asunto LIKE '%" + filtro + "%' AND self.estado = '" + estado + "'";
JPA.all(Correo.class).filter(jpql).fetch();
```

**MUST NOT** usar `String.format`, concatenación ni `MessageFormat` para construir JPQL/SQL con valores que provengan (aunque sea indirectamente) del cliente.

Para `<domain>` en XML, mismo principio: usar `:__user__` (sin punto), `:__date__` y `:foo` con `<context name="foo" expr="..."/>`. Nunca interpolar literales y nunca usar `:__user__.campo` con punto (no resuelve; ver §4).

---

## 6. Logs

**MUST**:

- Sanitizar saltos de línea (`\n`, `\r`) en cualquier valor del cliente antes de loguearlo. Sin esto, un atacante con control de un campo libre puede inyectar líneas falsas de log y confundir a quien investiga un incidente (log injection / log forging).
- Usar parametrización del logger (`log.info("Procesando correo id={}", id)`), no concatenación.

**MUST NOT** loguear:

- Contraseñas, tokens, claves privadas, certificados completos.
- Números de DNI completos, IBAN completos.
- Contenido (bytes) de adjuntos.
- JSON completo del request (puede contener cualquiera de los anteriores).
- Stack traces con valores sensibles incrustados en mensajes.

✅ CORRECTO: `log.info("Login intentado para usuario id={}", userId);`

❌ INCORRECTO: `log.info("Login intentado: usuario=" + username + " password=" + password);` (loguea password en claro y por concatenación).

Para trazabilidad de un valor sensible (p.ej. un token), loguear solo un fragmento o un hash truncado (SHA-256), nunca el valor entero.

---

## 7. Adjuntos / MetaFile

Adjuntos subidos por el usuario son superficie de ataque clásica. Tres defensas obligatorias.

### 7.1 Validación de tipo (por contenido, no por extensión)

**MUST NOT** confiar en:

- La extensión del nombre (`.pdf` puede ser un EXE renombrado).
- El `Content-Type` declarado en la subida (lo dicta el cliente).
- `URLConnection.guessContentTypeFromName(nombre)` (heurística por extensión, no inspecciona contenido).

**MUST** validar el tipo por **magic bytes** del contenido cuando el tipo importa para el flujo. Si existe un helper del proyecto en `base/util/` o `base/infrastructure/`, usarlo; si no, añadirlo antes de implementar el flujo sensible.

### 7.2 Tamaño y memoria

**MUST**:

- Imponer un límite de tamaño por fichero (configurable, documentado en `design.md`).
- Imponer un límite agregado por request si se permiten varios adjuntos.
- Comprobar el tamaño **antes** de cargar el fichero entero en memoria como `byte[]`. Para ficheros grandes, usar `InputStream`/streaming.

### 7.3 Nombre de fichero (path traversal)

**MUST NOT** usar el `filename` del cliente como ruta en disco. Nombres como `../../../etc/passwd` o `C:\Windows\System32\drivers\etc\hosts` se cuelan si se concatena directamente.

**MUST**:

- Sanitizar el `filename` antes de mostrarlo o guardarlo: extraer solo el nombre base, eliminar separadores de directorio y caracteres de control, normalizar Unicode.
- Si se almacena en disco, generar el nombre real en el servidor (UUID o id). Mantener el `filename` original solo como metadato visible.

---

## 8. Secretos

**MUST**:

- Mantener contraseñas, tokens de API, claves privadas y secretos fuera del código fuente. Usar `application.properties` (no commiteado con valores reales) o variables de entorno.
- Almacenar passwords de usuario con el hashing del framework (Axelor/Shiro). **MUST NOT** implementar tu propio hashing.
- Cifrar en reposo los secretos que la app gestiona en nombre del usuario (claves privadas de firma, tokens externos). Usar `base/infrastructure/criptografia/`.

**MUST NOT**:

- Hardcodear secretos en `.java`, `.xml`, `.properties` commiteados, ni en tests.
- Devolver secretos en responses JSON. Si un endpoint devuelve un `Usuario`, **MUST** verificar que el password hasheado no se serializa.
- Loguear secretos a ningún nivel (ver §6).

---

## 9. Defensa en profundidad: `validate*` en el servicio

`validateInsert/Update/Remove` del `*ServiceImpl` son la red de seguridad final del **flujo de guardado genérico** (`insert`/`update`/`remove`): tanto la Vía A (un botón que invoca la acción genérica) como la Vía B acaban pasando por aquí.

> **Matiz — acciones propias**: una acción propia (`marcarComoFirmada`, `cambiarEstado`…) solo se alcanza por Vía A (`@CallMethod`), persiste con `repository.save/remove` y **NO** pasa por `validateUpdate`. Su red de seguridad final es **su propio** `validateMiAccion(...)`, ejecutado como primera línea de la acción (patrón validate + throw). **MUST** implementar ahí las V-rules que apliquen a esa acción. Recuerda: en la `*Impl` **MUST NOT** llamar **nunca** a `super.insert/update/remove` (ni siquiera al sobrescribir `insert/update/remove`); se persiste con `repository.save/remove` y el `validateXxx(...).ifPresent(throwIfInvalid)` lo pones tú como primera línea (ver `[[k-sistemas]]` §"Persistir: siempre `repository`, nunca `super.*`").

**MUST** implementar las validaciones `V-<Entidad>-NNN` declarativas de la entidad **aquí**. **MUST NOT** dejar la validación solo en `<action-validate>`/`<action-condition>` del XML ni en chequeos JS del cliente: la Vía B se salta la vista entera y solo pasa por el servicio. Los chequeos en XML/JS sirven para UX, no para integridad.

El controller no es el sitio para las V-rules de la entidad: su papel es extraer el bean con `AllowProperties`, hacer chequeos de autorización contextuales (p.ej. "solo el rol X puede invocar esta acción") y delegar en el servicio.

El caso particular en que la entidad bloquea por completo `update`/`remove` se trata en §9.2.

### 9.1 Forma general de un `validate*`

Un `validate*` comprueba una condición y **devuelve el mensaje solo cuando la condición falla**. La forma general es:

```java
@Override
public Optional<BusinessMessages> validateUpdate(Correo nuevo, Correo original) {
    if (seCumpleLaValidacionA(nuevo, original)==false) {
        return Optional.of(BusinessMessages.single(
                I18n.get("Mensaje de negocio explicando que no se cumple la validación A.")));
    }
    return Optional.empty();    // todo bien, dejar pasar
}
```

El `update`/`remove` correspondiente, **si lo sobrescribes**, pone `validateUpdate(...).ifPresent(throwIfInvalid)` como primera línea y luego el flujo normal de la operación (sobrescrituras de campos `servidor`, restauraciones de inmutables, `repository.save/remove` — **nunca** `super.update/remove`). Las comprobaciones de la V-… viven en `validateUpdate`, no se repiten inline en el cuerpo.

### 9.2 Caso particular: entidad que NUNCA admite la operación

Cuando el análisis dice que la entidad **no admite** una operación en absoluto (p.ej. `Correo` es inmutable tras crearse → ningún update es válido), el `if` del `validate*` se vuelve trivialmente cierto y desaparece. Y, además, el `update`/`remove` deja de tener "flujo normal" — cualquier llamada que llegue ahí es un caller programático que se saltó `validateUpdate`. Solo **en este caso** `update`/`remove` lanza `UnsupportedOperationException` incondicional, en paralelo al `validate*` que siempre rechaza:

```java
@Override
public Optional<BusinessMessages> validateUpdate(Correo nuevo, Correo original) {
    // No hay if: la entidad nunca admite update → siempre se rechaza.
    return Optional.of(BusinessMessages.single(
            I18n.get("El correo es inmutable tras su creación.")));
}

@Override
public Correo update(Correo nuevo, Correo original) {
    // Mismo motivo: no hay flujo normal, solo el rechazo.
    throw new UnsupportedOperationException(
            I18n.get("El Correo es inmutable tras su creación."));
}
```

¿Por qué las dos respuestas a la vez? `validateUpdate` produce el mensaje de negocio que el cliente verá si alguna capa intenta el update por error; `update` con `UnsupportedOperationException` es el cinturón final si alguien se salta la validación (un caller programático que ignore el `Optional`, por ejemplo). Una sola de las dos no basta: la primera sin la segunda permite que un caller despistado siga adelante; la segunda sin la primera devuelve un 500 sin mensaje de negocio en lugar de un error controlado.

---

## 10. Checklist de revisión

Aplicar a cada PR o cambio que toque `*ServiceImpl`, `*Controller`, vistas XML con `<form>` que escriben en BD, o que añada un nuevo endpoint.

- [ ] **`allowPropertiesXxx`** (§3.2): ¿cada acción del servicio invocada desde `@CallMethod` tiene su `allowPropertiesXxx` declarado? Si usa `createAllowProperties` (whitelist): ¿enumera solo campos `cliente`? Si usa `createAllowAllProperties` (abierto): ¿**todos** los campos `servidor` se asignan incondicionalmente en esa acción?
- [ ] **Asignación incondicional de campos `servidor`** (§3.3): ¿hay algún `if (campo == null) setCampo(...)` en una acción del `*ServiceImpl` para un campo clasificado `servidor` en `entity-*.md`? → quitar el `if`, asignación incondicional.
- [ ] **Campos `servidor` que la acción NO toca** (§3.2 regla 1): ¿están **excluidos** de la whitelist? Si la acción no los asigna, el cliente no puede enviarlos.
- [ ] **Multi-centro**: ¿toda consulta de detalle/listado filtra por el centro del usuario — en Java `AuthUtils.getUser().getCentroActivo()`, en XML un `<context>` referenciado como `:nombrePlano` en el `<domain>` (NUNCA `:__user__.centroActivo` con punto)? ¿`<action-view>` de centro lleva `<domain>`?
- [ ] **Asignación de centro al crear**: si el centro lo dicta el servidor, ¿se asigna incondicionalmente en `*ServiceImpl.insert` desde `AuthUtils.getUser().getCentroActivo()`?
- [ ] **JPQL/SQL**: ¿todos los filtros usan `:param` con `bind(...)`? ¿Ninguna query concatena strings con input del usuario?
- [ ] **Logs**: ¿se loguea algún password/token/clave/DNI completo/bytes de adjunto? ¿Se sanitizan CRLF en valores libres del cliente?
- [ ] **Adjuntos**: ¿se valida tipo por contenido (no solo por extensión)? ¿Hay límite de tamaño? ¿Se sanitiza el `filename`?
- [ ] **Secretos**: ¿algún secreto hardcodeado en el diff? ¿Algún response devuelve campos sensibles (password hash, clave privada)?
- [ ] **`validate*`**: ¿las V-… están implementadas en el servicio (no solo en el controller ni solo en el XML)?
- [ ] **UI ≠ seguridad**: ¿hay algún comentario o suposición del tipo "este campo es readonly en la UI, así que no hace falta validarlo en el servidor"? → eliminar la suposición.

---

## 11. Anti-patrones detectados en revisiones reales

**MUST NOT** repetir ninguno.

| Anti-patrón                                                                                         | Por qué falla                                                          | Patrón correcto                                                                                           |
|-----------------------------------------------------------------------------------------------------|------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| `if (bean.getCampo() == null) bean.setCampo(valorInicial)` para campo `servidor`                    | Postman envía el campo relleno y la guarda lo respeta                  | Asignación incondicional `bean.setCampo(valorInicial)`                                                    |
| `createAllowAllProperties()` en una acción que **no** sobrescribe en una R-… todos los no-`cliente` | El cliente puede mandar cualquier campo y el servicio no lo neutraliza | Pasar a whitelist (`createAllowProperties(...)`) o añadir/extender la R-…                                 |
| `update` que no restaura campos inmutables ni lanza `UnsupportedOperationException`                 | El cliente pisa `fechaCreacion`/`numeroSecuencial`/etc.                | Restaurar desde `original` o lanzar `UnsupportedOperationException`                                       |
| `filter("self.id = :id")` sin centro                                                                | IDOR cross-tenant                                                      | `filter("self.centro = :centro AND self.id = :id").bind("centro", AuthUtils.getUser().getCentroActivo())` |
| `AuthUtils.getUser().getCentro()`                                                                   | API inexistente — no compila                                           | `AuthUtils.getUser().getCentroActivo()`                                                                   |
| `<domain>self.centro = :__user__.centro</domain>`                                                   | Campo incorrecto en el `User` (es `centroActivo`)                      | `<context name="c" expr="eval: __user__?.centroActivo"/>` + `<domain>self.centro = :c</domain>`           |
| `<domain>self.centro = :__user__.centroActivo</domain>` (parámetro con punto)                       | Hibernate no admite puntos en el nombre del parámetro → listado vacío  | `<context name="c" expr="eval: __user__?.centroActivo"/>` + `<domain>self.centro = :c</domain>` (domain antes que context, lo exige el XSD) |
| `"WHERE x = '" + valor + "'"` en JPQL                                                               | Inyección                                                              | `filter("self.x = :v").bind("v", valor)`                                                                  |
| `URLConnection.guessContentTypeFromName(filename)` como única defensa de tipo                       | Heurística por extensión, no por contenido                             | Validar magic bytes                                                                                       |
| `log.info("payload=" + json)` en flujos de alta                                                     | Logs llenos de secretos                                                | Loguear solo IDs y campos no sensibles                                                                    |

---

## Quick Guidelines

- **El servidor es la única fuente de verdad**. UI (`readonly`, `showIf`, `hidden`, `required`) es UX, **NO** defensa.
- **Dos vías de entrada**: el botón de la UI (pasa por controller → `AllowProperties`) y el REST genérico `/ws/rest/<FQN>` (NO pasa por el controller). La única capa universal es `*ServiceImpl`.
- **Control de campos = una pregunta**: ¿quién dicta este campo, cliente o servidor? Dos defensas combinables: A) `allowPropertiesXxx` filtra en entrada (whitelist explícita, o abierto solo si todos los `servidor` se asignan en la acción); B) el `*ServiceImpl` asigna **sin `if`** todo campo `servidor` que la acción tenga que tocar; los campos `servidor` que la acción no toque quedan **fuera de la whitelist**.
- **`AllowProperties` por acción**: whitelist por defecto; allow-all solo si una R-… sobrescribe todos los no-`cliente`. Declarar modo en `design.md`.
- **Multi-centro**: filtrar siempre por `AuthUtils.getUser().getCentroActivo()` (Java) o, en XML, un `<context>` referenciado como `:nombrePlano` en el `<domain>` (NUNCA `:__user__.campo` con punto: no resuelve); asignar el centro al crear desde el servidor, no desde el bean.
- **JPQL/SQL**: solo `:param` con `bind`; cero concatenación.
- **Adjuntos**: validar tipo por contenido, limitar tamaño, sanear `filename`.
- **Logs**: nada de secretos; sanear CRLF.
- **Defensa en profundidad**: `validate*` en el servicio implementa las V-…, además del controller y la UI.
- **CRITICAL**: una violación de este skill no es estilo, es **fallo de seguridad**. El reviewer **MUST** bloquear el cambio hasta corregirlo.
