# `ActionRequest`: métodos públicos

La clase `com.axelor.rpc.ActionRequest` se usa en los controladores Java de Axelor para enviar instrucciones desde una acción del cliente (navegador) a un controlador Java. Es el objeto que reciben los métodos de controlador y contiene toda la información que el cliente ha enviado: el contexto del formulario, los filtros, la paginación, los campos solicitados, etc.

## Resumen de los métodos

| Uso | Métodos |
|---|---|
| **Acceso al formulario** (lo más habitual) | `getContext()`, `getRawContext()` |
| **Acciones sobre selección en grid** | `getRecords()` |
| **Búsquedas y listados** | `getCriteria()`, `getLimit()`, `getOffset()`, `getSortBy()`, `getFields()` |
| **Metadatos y tipo de entidad** | `getModel()`, `getBeanClass()` |
| **Evaluar expresiones dinámicas** | `getScriptHelper()` |
| **Infraestructura / serialización** | `Request.current()`, `getData()`, `getSelect()`, `getRelated()` |
| **Raramente necesario a mano** | `getAction()`, `isTranslate()`, `getUser()` |


---

```java
public void miAccion(ActionRequest request, ActionResponse response) { ... }
```

---

## Métodos propios de `ActionRequest`

### `getAction()`

Nombre de la acción en formato `NombreClase:nombreMetodo` o nombre de acción de metadatos.

```java
// Resource.java — enrutar llamada al método del controlador
String[] parts = request.getAction().split("\\:");

// ActionHandler.java — resolver acción de metadatos
String name = request.getAction();
```

---

## Acceso al contexto del formulario

### `getContext()`

Devuelve los datos del registro activo del formulario como un `Context`. Es **el método más importante** en controladores: permite trabajar con los datos del formulario sin ir a base de datos.

```java
// Patrón estándar: convertir el contexto al tipo del modelo
UserToken userToken = request.getContext().asType(UserToken.class);

// Acceder al registro padre (formulario contenedor en un o2m/m2m)
User owner = request.getContext().getParent().asType(User.class);

// Múltiples campos con asType
MetaFilter filter = request.getContext().asType(MetaFilter.class);
```

El objeto devuelto por `asType()` es un proxy: tiene los valores enviados desde el cliente pero **no es una entidad gestionada por JPA**. Si se necesita persistir, hay que buscar la entidad real con su `id`.

---

### `getRawContext()`

Devuelve el contexto como `Map<String, Object>` combinando `context` y `_domainContext`. Útil para leer campos auxiliares (`_campo`) que no existen en la clase del modelo.

```java
// MailController.java
String type = (String) request.getRawContext().get("type");
```

**Cuándo preferirlo sobre `getContext()`:** cuando se necesitan claves técnicas del contexto (`_signal`, `_domainAction`, variables auxiliares con prefijo `_`) que no están mapeadas en la entidad Java.

---

### `getScriptHelper()`

Devuelve un `ScriptHelper` ligado al contexto actual, para evaluar expresiones dinámicas (Groovy/JS) con los valores del formulario.

```java
// MetaController.java
ScriptHelper sh = request.getScriptHelper();
Object result = sh.eval("record.amount * 2");

// GridView.java — evaluar condición de visibilidad
final ScriptHelper helper = request.getScriptHelper();
boolean visible = (Boolean) helper.eval(condition);
```

En la práctica se usa más en código de infraestructura (serialización, vistas) que en controladores de negocio.

---

## Modelo y metadatos

### `getModel()` / `getBeanClass()`

`getModel()` devuelve el nombre completo de la clase (p.ej. `"com.axelor.auth.db.User"`). `getBeanClass()` resuelve ese nombre a `Class<?>` mediante reflexión (devuelve `null` si el modelo no está definido).

```java
// Verificar tipo antes de operar
if (request.getBeanClass() != null
    && MetaView.class.isAssignableFrom(request.getBeanClass())) { ... }

// Metadatos de campos
response.setData(MetaStore.findFields(request.getBeanClass(), request.getFields()));
```

---

### `getCriteria()`

Parsea los filtros de búsqueda enviados por el cliente (desde `getData()`) y los devuelve como un objeto `Criteria`. Es lazy: solo se construye la primera vez que se llama.

```java
// Resource.java
if (request.getCriteria() != null) {
    query = request.getCriteria().createQuery(model, ...);
}
```

**Cuándo usarlo:** en acciones de búsqueda o listado para aplicar los filtros que el usuario ha definido en el cliente.

---

### `isTranslate()`

Indica si los valores deben traducirse al idioma del usuario al construir queries y criterios.

```java
// Resource.java
query.translate(request.isTranslate());

// Criteria.java
return parse(raw, request.getBeanClass(), request.isTranslate());
```

El framework lo gestiona automáticamente. El único caso manual relevante encontrado en el repo es `Resource.export(...)`, que fuerza `setTranslate(false)` para exportar valores en bruto.

---

## Paginación y orden

### `getLimit()` / `getOffset()`

Parámetros de paginación: número máximo de registros y posición de inicio.

```java
// Resource.java
int offset = request.getOffset();
int limit  = request.getLimit();

// MailController.java
final List<Object> all = find(SQL_INBOX, request.getOffset(), request.getLimit());
```

---

### `getSortBy()`

Lista de campos de ordenación. Un campo con prefijo `-` indica orden descendente (p.ej. `"-date"`).

```java
// Resource.java
if (request.getSortBy() != null) {
    sortOn.addAll(request.getSortBy());
}
```

---

## Datos y proyección

### `getData()`

Mapa bruto con el payload completo del request. Contiene `context`, `_domainContext` y cualquier otro parámetro enviado por el cliente.

```java
// SearchService.java
final String matching = (String) request.getData().get("search");
final List<?> selected = (List<?>) request.getData().get("selected");
```

En controladores de negocio es preferible `getContext()` o `getRawContext()`. `getData()` es más útil en servicios de infraestructura que necesitan el payload completo.

---

### `getRecords()`

Lista de registros seleccionados por el usuario. Cada elemento es un `Map<String, Object>` con al menos `id` y `version`.

```java
// MailController.java — obtener el registro asociado
final Model related = (Model) request.getRecords().getFirst();

// RestService.java — extraer los IDs de una selección múltiple
Long[] ids = request.getRecords().stream()
    .map(rec -> Long.valueOf(((Map<?, ?>) rec).get("id").toString()))
    .toArray(Long[]::new);
```

**Cuándo usarlo:** en acciones de botón sobre una selección de registros en un grid (acciones masivas).

---

### `getFields()`

Lista de nombres de campos que el cliente quiere recibir en la respuesta.

```java
// Resource.java — proyección en la query
if (request.getFields() != null) {
    query.select(request.getFields().toArray(new String[0]));
}
```

Permite optimizar consultas devolviendo solo los campos necesarios.

---

### `getRelated()`

Mapa de relaciones a expandir con sus subcampos: `{ "campoRelacional": ["subcampo1", "subcampo2"] }`.

```java
// Resource.java
final Map<String, List<String>> related = request.getRelated();
// ... inyecta los datos relacionados en la respuesta
```

Típicamente lo rellena Jackson desde el JSON del cliente; no se observan llamadas Java a `setRelated(...)`.

---

### `getSelect()`

Grafo de selección de campos para transformaciones de datos (exportaciones, respuestas enriquecidas).

```java
// Resource.java
final Map<String, Object> select = request.getSelect();
if (select != null) {
    // mezcla resultado de toGraph(...)
}
```

Como `getRelated()`, suele llegar serializado desde el cliente.

---

## Usuario y acceso estático

### `getUser()`

Devuelve el usuario de la sesión actual. Es equivalente a `AuthUtils.getUser()`.

```java
User user = request.getUser();
```

No se encontraron llamadas directas a `request.getUser()` en el repo: el patrón habitual es inyectar `AuthUtils` directamente. Se incluye por completitud de la API.

---

### `Request.current()` _(estático)_

Devuelve la instancia de `Request` del hilo actual, almacenada en un `ThreadLocal`. Puede ser `null` si se llama fuera del ciclo de vida de una petición HTTP (`RequestFilter` es quien la registra y limpia).

```java
// ObjectMapperProvider.java — desde código de serialización sin inyección
final Request request = Request.current();
if (request == null) {
    return true; // fuera de contexto de petición, valor por defecto
}
final ScriptHelper helper = request.getScriptHelper();
```

**Cuándo usarlo:** en código de infraestructura (serialización Jackson, listeners, helpers estáticos) que no recibe el `Request` como parámetro pero necesita el contexto de la petición en curso. En controladores normales no hace falta porque el `request` ya llega como parámetro.

---

