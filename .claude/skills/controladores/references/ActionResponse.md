# `ActionResponse`: métodos públicos

La clase `com.axelor.rpc.ActionResponse` se usa en los controladores Java de Axelor para enviar instrucciones al cliente (navegador) tras ejecutar una acción.

## Resumen de los métodos

| Método | Efecto en la vista |
|---|---|
| `setReload` | Recarga la vista actual. |
| `setCanClose` | Permite o bloquea el cierre de la vista. |
| `setInfo` | Muestra un diálogo informativo modal. |
| `setNotify` | Muestra una notificación no modal. |
| `setAlert` | Muestra una alerta con opción de confirmación/cancelación. |
| `setError` | Muestra un diálogo de error modal. |
| `setPending` | Deja acciones pendientes para ejecutar después del diálogo. |
| `setExportFile` | Dispara la descarga de un archivo en el cliente. |
| `setValues` | Actualiza varios campos de la vista a la vez. |
| `setValue` | Actualiza un campo específico. |
| `setView` | Abre una vista (formulario, grid, popup…). |
| `setSignal` | Envía una señal de UI con datos opcionales. |
| `setAttrs` | Reemplaza atributos visuales de múltiples campos. |
| `setAttr` | Cambia un atributo visual de un campo. |
| `setRequired` | Marca o desmarca un campo como obligatorio. |
| `setReadonly` | Marca o desmarca un campo como solo lectura. |
| `setHidden` | Oculta o muestra un campo. |
| `setColor` | Cambia el color visual de un campo. |


---

```java
public void miAccion(ActionRequest request, ActionResponse response) { ... }
```

---

## Control general de la vista

### `setReload(boolean reload)`

Indica al cliente que recargue la vista actual.

```java
response.setReload(true);

// Combinado con notify tras una operación exitosa:
response.setReload(true);
response.setNotify(I18n.get("Multi-factor authentication has been enabled."));
```

**Cuándo usarlo:** cuando un cambio en base de datos requiere refrescar completamente la pantalla sin que el usuario lo haga manualmente.

---

### `setCanClose(boolean canClose)`

Indica si la vista actual (normalmente un popup/diálogo) puede cerrarse.

```java
// Cerrar popup y abrir otra vista
response.setCanClose(true);
response.setView(
    ActionView.define("API key")
        .model(UserToken.class.getName())
        .add("form", "user-token-api-key-form")
        .param("popup", "true")
        .map());
```

**Cuándo usarlo:** en acciones ejecutadas dentro de un popup para indicar que la operación ha terminado y el popup debe cerrarse. Se combina frecuentemente con `setView` para navegar a otra pantalla.

---

## Mensajes al usuario

### `setInfo(String message)`
### `setInfo(String message, String title)`
### `setInfo(String message, String title, String confirmBtnTitle)`

Muestra un diálogo informativo modal.

- `message`: texto principal.
- `title`: título del modal (opcional).
- `confirmBtnTitle`: texto del botón de confirmación (opcional).

```java
response.setInfo("A new verification code has been sent to your email address.");
response.setInfo("Hello World!!!", "My title");
```

**Cuándo usarlo:** cuando se quiere que el usuario lea una confirmación importante. Más intrusivo que `setNotify` ya que bloquea la interacción hasta cerrarlo.

---

### `setNotify(String message)`
### `setNotify(String message, String title)`

Muestra una notificación no modal (tipo toast/snackbar).

- `message`: texto de la notificación.
- `title`: título de la caja de notificación (opcional).

```java
response.setNotify(I18n.get("Multi-factor authentication has been enabled."));
response.setNotify(I18n.get("Job has been updated."));
```

**Cuándo usarlo:** para confirmar operaciones exitosas sin interrumpir el flujo del usuario.

---

### `setAlert(String message)`
### `setAlert(String message, String title)`
### `setAlert(String message, String title, String confirmBtnTitle, String cancelBtnTitle, String action)`

Muestra un diálogo de alerta con botones de confirmar/cancelar.

- `message`: texto de alerta.
- `title`: título del modal (opcional).
- `confirmBtnTitle`: texto del botón confirmar (opcional).
- `cancelBtnTitle`: texto del botón cancelar (opcional).
- `action`: acción a ejecutar al cancelar o cerrar el diálogo (opcional).

```java
response.setAlert(
    I18n.get(
        "You have changed your email address. Please reconfigure it for"
            + " multi-factor authentication."));
```

**Cuándo usarlo:** para advertir al usuario de algo importante antes de continuar, o cuando se necesita confirmación explícita. La variante completa permite ejecutar acciones correctivas si el usuario cancela.

---

### `setError(String message)`
### `setError(String message, String title)`
### `setError(String message, String title, String confirmBtnTitle, String action)`

Muestra un diálogo de error modal.

- `message`: texto de error.
- `title`: título del modal (opcional).
- `confirmBtnTitle`: texto del botón confirmar (opcional).
- `action`: acción a ejecutar al cerrar el diálogo para medidas correctivas (opcional).

```java
response.setError(I18n.get("API key should be attached to a valid user"));
response.setError(I18n.get("Invalid cron :") + " " + cronExpression);
response.setError(e.getMessage());
```

**Cuándo usarlo:** cuando la operación ha fallado y se debe informar al usuario del motivo. Bloquea la interacción hasta que el usuario cierre el diálogo.

---

### `setPending(String actions)`

Registra una lista de acciones pendientes separadas por coma, que se ejecutarán tras la interacción del usuario con el diálogo.

```java
response.setError("Debe completar los datos requeridos.");
response.setPending("save,reload");
```

**Cuándo usarlo:** combinado con `setAlert` o `setError` para encadenar acciones que deben ejecutarse después de que el usuario cierre el diálogo.

---

## Exportación de archivos

Todas las variantes copian el archivo a un área temporal, generan un token y el cliente inicia la descarga automáticamente.

### `setExportFile(String path)`

Exporta el archivo en la ruta indicada usando el nombre real del fichero.

### `setExportFile(String path, String fileName)`

Exporta el archivo en la ruta indicada forzando el nombre de descarga.

### `setExportFile(Path path)`

Igual que la variante `String`, pero usando `java.nio.file.Path`.

### `setExportFile(Path path, String fileName)`

Exporta desde un `Path` con nombre de descarga personalizado.

### `setExportFile(InputStream stream, String fileName)`

Exporta directamente desde un stream con el nombre indicado.

```java
try (InputStream stream = zipDirectory(outputDir)) {
    response.setExportFile(stream, fileName);
}
```

**Cuándo usarlo:** para exportaciones generadas en el servidor (CSV, ZIP, PDF, etc.). El cliente iniciará automáticamente la descarga del fichero.

---

## Valores de campos

### `setValues(Object context)`

Actualiza múltiples campos del formulario actual. El argumento puede ser un `Map`, un `Context` o un proxy de `Model` obtenido con `Context.asType()`.

```java
response.setValues(
    Map.of(
        "_qrCode", qrCodeData,
        "_secretKey", mfa.getTotpSecret(),
        "isTotpValidated", mfa.getIsTotpValidated()));
```

**Cuándo usarlo:** para actualizar varios campos a la vez sin recargar toda la vista. Ideal para campos calculados o campos auxiliares (prefijo `_`).

---

### `setValue(String fieldName, Object value)`

Actualiza el valor de un campo concreto. Las llamadas se acumulan en el mismo mapa interno, por lo que se pueden encadenar.

```java
response.setValue("id", user.getId());
response.setValue("name", name.get(user));
response.setValue("nameField", name.getName());
response.setValue("login", user.getCode());
response.setValue("lang", user.getLanguage());
```

**Cuándo usarlo:** cuando solo se necesita actualizar uno o pocos campos específicos.

---

## Apertura de vistas

### `setView(Map<String, Object> view)`

Indica al cliente que abra la vista descrita por el mapa. Normalmente se construye con el builder `ActionView`.

```java
response.setView(
    ActionView.define("API key")
        .model(UserToken.class.getName())
        .add("form", "user-token-api-key-form")
        .param("popup", "true")
        .param("show-toolbar", "false")
        .param("show-confirm", "false")
        .param("popup-save", "false")
        .context("_apiKey", userToken.getApiKey())
        .map());
```

### `setView(String title, String model, String mode, String domain)`

Atajo para abrir rápidamente una vista de un modelo con un título y un filtro.

```java
response.setView(
    ActionView.define(metaField.getTypeName())
        .model(MetaModel.class.getName())
        .domain(domain)
        .map());
```

**Cuándo usarlo:** para navegar a otra vista o abrir un popup desde una acción de botón.

---

## Señales al cliente

### `setSignal(String signal, Object data)`

Envía una señal arbitraria al cliente con datos asociados. La vista cliente puede escuchar señales concretas y reaccionar a ellas.

```java
response.setSignal("back", true);   // volver a la pantalla anterior
response.setSignal("refresh-tab", tabIndex);
```

**Cuándo usarlo:** para comunicación personalizada entre el servidor y la lógica de vista del cliente cuando los métodos estándar no cubren el caso de uso. Las señales disponibles dependen de la implementación del cliente.

---

## Atributos de campos

### `setAttrs(Map<String, Map<String, Object>> attrs)`

Reemplaza el conjunto completo de atributos de campos. Estructura esperada: `{ "campo": { "atributo": valor } }`.

**Cuándo usarlo:** cuando se necesita cambiar atributos de varios campos de una vez.

---

### `setAttr(String fieldName, String attr, Object value)`

Agrega o actualiza un atributo puntual de un campo (`domain`, `readonly`, `required`, `hidden`, etc.).

```java
// Filtrar el dominio de campos relacionales
response.setAttr("user", "domain", getDomain(request, DMSPermission::getUser));
response.setAttr("group", "domain", getDomain(request, DMSPermission::getGroup));
```

**Cuándo usarlo:** para modificar dinámicamente la visibilidad, editabilidad, obligatoriedad o dominio de un campo según la lógica de negocio.

---

### `setRequired(String fieldName, boolean required)`

Atajo para `setAttr(fieldName, "required", required)`. Marca un campo como obligatorio u opcional en tiempo de ejecución.

```java
response.setRequired("email", true);
```

---

### `setReadonly(String fieldName, boolean readonly)`

Atajo para `setAttr(fieldName, "readonly", readonly)`. Bloquea o habilita la edición de un campo.

```java
response.setReadonly("code", true);
```

---

### `setHidden(String fieldName, boolean hidden)`

Atajo para `setAttr(fieldName, "hidden", hidden)`. Oculta o muestra un campo dinámicamente.

```java
response.setHidden("internalNotes", true);
```

---

### `setColor(String fieldName, String color)`

Atajo para `setAttr(fieldName, "color", color)`. Aplica un color CSS a un campo para resaltarlo visualmente.

```java
response.setColor("status", "#FF0000");
```

---

