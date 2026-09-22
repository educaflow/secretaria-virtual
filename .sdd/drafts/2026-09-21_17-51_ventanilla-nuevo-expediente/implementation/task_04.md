---
type: implementation-task
template: system
---

# Tarea 04 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-i18n
- k-code-quality

Implementa el controlador del asistente, con los cuatro `@CallMethod` que invocan las vistas.

El dominio y el servicio ya están en el árbol (tareas 01 y 03): son **contrato fijo**. Las vistas que llaman a estos
métodos se materializan en las tareas 05, 06 y 07; los nombres de los métodos **MUST** ser exactamente los que el
diseño declara aquí.

## Fila de la tabla «Ficheros a crear o modificar» del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/system/ventanilla/controller/AsistenteNuevoExpedienteController.java` | Crear | k-sistemas (controladores.md) | Los cuatro `@CallMethod` que usan las vistas |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.
>
> **Los comentarios de este `design.md` NO se transcriben al código.** Son material de `.sdd/`: explican el diseño, no acompañan al código. **MUST NOT** aparecer en los `.java` los comentarios `//` de estos pasos, ni ningún identificador de la spec o del diseño (`V-`, `R-`, `U-`, `CC-`, `ESC-`, `HU-`, `VAL-`, `RN-`, `RUI-`), ni la justificación de una decisión «para que no se pierda» (`k-code-quality/comentarios.md`): lo que el código no revela por sí solo se dice con el nombre del método o de la variable, y lo demás vive aquí y en `decisiones.md`.

## Diseño (verbatim)

### Paso 5 — Controlador `AsistenteNuevoExpedienteController`

```java
// Clase: com.educaflow.system.ventanilla.controller.AsistenteNuevoExpedienteController
//   @Inject private ModelServiceFactory modelServiceFactory;
//   En cada método: (AsistenteNuevoExpedienteService) modelServiceFactory.resolve(AsistenteNuevoExpediente.class)
//   y ActionRequestHelper<AsistenteNuevoExpediente> / ActionResponseHelper.
//   Ninguno lleva @Transactional: ninguno escribe en base de datos.

// Los CUATRO métodos siguen el MISMO tramo, el que ya escribía validateTriggerInitialEvent: construir el bean con
// la AllowProperties de la acción, consultar su validador y, si devuelve mensajes, contestarlos con
// actionResponseHelper.doResponseBusinessMessagesAsError(...) y `return` sin llamar a la acción.
// Sin ese tramo el único camino de un mensaje sería el throwIfInvalid del servicio, que llega al
// usuario como ValidationException cruda y no como el diálogo de negocio que exige HU-006; el
// `validate + throw` del servicio se queda como red de seguridad, no como vía de presentación.

@CallMethod
public void prepararCentros(ActionRequest actionRequest, ActionResponse actionResponse);
//   getModel(allowPropertiesPrepararCentros()) → validatePrepararCentros → (mensajes: error y return)
//   → prepararCentros, y devuelve a la vista: `centrosDisponibles` (lista de referencias de centro),
//   `hayQueElegirCentro` y `centro` (referencia o null). Con esos tres valores el action-group del
//   onNew decide el arranque.

@CallMethod
public void prepararTramites(ActionRequest actionRequest, ActionResponse actionResponse);
//   getModel(allowPropertiesPrepararTramites()) → validatePrepararTramites → (mensajes: error y
//   return) → prepararTramites, y devuelve `tramitesDisponibles` (lista de referencias de trámite).
//   NO devuelve `hayQueElegirCentro`: lo calcula el paso 1 y lo transporta la vista (ver Paso 1).

@CallMethod
public void recalcular(ActionRequest actionRequest, ActionResponse actionResponse);
//   getModel(allowPropertiesRecalcular()) → validateRecalcular → (mensajes: error y return) →
//   recalcular, y devuelve los seis valores que la pantalla del paso 3 necesita: `nombreTramite`,
//   `ayudaTramite`, `presentadoEnPapel`, `presentadoEnRepresentacion`,
//   `hayQuePreguntarPresentacion` y `hayQuePreguntarParaQuien`.
//   Los dos primeros son campos derivados: se leen del getter del bean (que los recalcula desde
//   `tramite`), no de ninguna asignación previa del servicio.

@CallMethod
public void validateTriggerInitialEvent(ActionRequest actionRequest, ActionResponse actionResponse);
//   getModel(allowPropertiesTriggerInitialEvent()) → validateTriggerInitialEvent y, si hay mensajes, los devuelve con
//   doResponseBusinessMessagesAsError(I18n.get("No es posible crear el expediente"), …) — el mismo
//   título que usa TramitadorController, para que el usuario vea siempre el mismo encabezado rechace
//   quien rechace. Los tres anteriores usan la sobrecarga SIN título, que es la forma normal del
//   proyecto para el rechazo de una precondición. El error corta el action-group y el alta no llega
//   a dispararse.

private Map<String, Object> toReferenciaCentro(Centro centro);
//   {id, name}: lo que el listado de centros necesita para pintar la fila y para que el clic
//   devuelva el id.

private Map<String, Object> toReferenciaTramite(Tramite tramite);
//   {id, name}: la referencia mínima, idéntica a la del hermano
//   `system/expedientes/controller/NuevoExpedienteController.toReference`.
//   MUST NOT componerse aquí ni el nombre traducido ni la referencia anidada del `tipoTramite`: el
//   `panel-related` vuelve a pedir estas filas al servidor (`one-to-many.tsx`, `onSearch` selecciona
//   los items sin `version` ni `_fetched` y funde con `{...item, ...record}`, donde gana el
//   registro traído), así que todo lo que se componga aquí se descarta. El listado del paso 2 sale
//   traducido sin hacer nada: `Tramite.name` es `translatable="true"`, y por eso `Resource.search`
//   adjunta `$t:name` (`Translator.applyTranslatables`, clave `value:` + el valor) y el grid pinta
//   esa traducción (`formatString`, `axelor-front/src/utils/format.ts`).
```

**Verificar al final:** `grep -n "actionRequest, ActionResponse actionResponse" …/AsistenteNuevoExpedienteController.java` devuelve cuatro líneas (los parámetros se llaman siempre así).

---

## Frontera de confianza — AllowProperties por acción

> Es la whitelist con la que cada `@CallMethod` construye el bean (`getModel(allowProperties<Accion>())`).

### `AsistenteNuevoExpedienteServiceImpl.prepararCentros` (invocado desde `AsistenteNuevoExpedienteController.prepararCentros`)

Entidad: `AsistenteNuevoExpediente`. **Forma elegida**: `createDenyAllProperties`.
**Origen spec:** la acción no aparece en `entity-AsistenteNuevoExpediente.md`; es el arranque del asistente y no recibe ningún dato del usuario.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| (todos) | servidor | **NO** | La acción solo lee el usuario autenticado. Todo lo que escribe lo asigna `fireActionRule_AsignarArranqueDelAsistente`, igual que en las otras dos acciones: todo campo `servidor` se escribe dentro de un `fireActionRule_*`. |

### `AsistenteNuevoExpedienteServiceImpl.prepararTramites` (invocado desde `AsistenteNuevoExpedienteController.prepararTramites`)

Entidad: `AsistenteNuevoExpediente`. **Forma elegida**: `createAllowProperties`.
**Origen spec:** el centro es el campo `centro` de `entity-AsistenteNuevoExpediente.md`, elegido en el paso anterior.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `centro` | cliente | sí | El centro elegido en el paso 1. Un centro ajeno no filtra nada: `getPerfilesDeInicioSobreTramite` devuelve vacío para un centro al que el usuario no pertenece. |
| `tramitesDisponibles` | servidor | **NO** | Asignado en `fireActionRule_AsignarTramitesDisponibles`. |
| `hayQueElegirCentro` | servidor | **NO** | Esta acción no lo toca ni lo lee (ver Paso 1). Fuera de la whitelist, así que el cliente tampoco puede dictarlo. |
| resto | servidor/cliente | **NO** | Esta acción no los usa ni los toca. |

### `AsistenteNuevoExpedienteServiceImpl.recalcular` (invocado desde `AsistenteNuevoExpedienteController.recalcular`)

Entidad: `AsistenteNuevoExpediente`. **Forma elegida**: `createAllowProperties`.
**Origen spec:** `centro`, `tramite` y `presentadoEnPapel` de `entity-AsistenteNuevoExpediente.md`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `tramite` | cliente | sí | El trámite elegido en el paso 2. `validateRecalcular` exige (V-009) que esté entre los evaluables de `findTramitesEvaluables()`, no solo que venga indicado: un id manipulado de una fila sin tipo de trámite o sin tipo de expediente por defecto reventaría dentro de `PerfilesUsuarioServiceImpl`. |
| `centro` | cliente | sí | El centro elegido en el paso 1. |
| `presentadoEnPapel` | cliente | sí | Es la respuesta del usuario a «¿Cómo se presenta?», y el cálculo del destinatario depende de ella. Si no hay que preguntarla, `fireActionRule_AsignarPresentacion` la sobrescribe con la única forma posible. |
| `presentadoEnRepresentacion` | cliente | **NO** | Esta acción la **recalcula siempre** (`fireActionRule_AsignarPresentacion`), nunca la lee: aceptarla dejaría que el cliente conservara una respuesta que ya no es posible (ESC-021). |
| `nombreTramite`, `ayudaTramite` | servidor | **NO** | **Campos derivados de solo lectura** (`transient` con cuerpo en `domains/AsistenteNuevoExpediente.xml`): nadie los asigna, su getter los recalcula desde `tramite`. Fuera de la whitelist, lo que llegue del cliente se descarta y además se sobrescribiría al leerlos. |
| `hayQuePreguntarPresentacion`, `hayQuePreguntarParaQuien` | servidor | **NO** | Asignados en `fireActionRule_AsignarPresentacion`. |
| `centrosDisponibles`, `tramitesDisponibles`, `hayQueElegirCentro` | servidor | **NO** | No intervienen en este paso. Esta acción no toca ni lee `hayQueElegirCentro` (ver Paso 1). |

### `AsistenteNuevoExpedienteServiceImpl.validateTriggerInitialEvent` (invocado desde `AsistenteNuevoExpedienteController.validateTriggerInitialEvent`)

Entidad: `AsistenteNuevoExpediente`. **Forma elegida**: `createAllowProperties`.
**Origen spec:** `Input AllowProperties` de la acción «Crear expediente» de `entity-AsistenteNuevoExpediente.md`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `centro` | cliente | sí | En `Input AllowProperties`. Se valida en la puerta 1 (indicado) y, entero, en la puerta 5 (`validateAlta` → el motor comprueba que el usuario tiene perfiles de inicio ahí). |
| `tramite` | cliente | sí | En `Input AllowProperties`. Se valida en la puerta 2 (evaluable: entre los de `findTramitesEvaluables()`, condición que cubre también el trámite ausente) y en la puerta 5 (`validateAlta` → motor). |
| `presentadoEnPapel` | cliente | sí | En `Input AllowProperties`. Se valida en la puerta 3 (informado) y en la puerta 5 (el perfil que exige esa forma tiene que estar entre los de inicio del usuario). |
| `presentadoEnRepresentacion` | cliente | sí | En `Input AllowProperties`. Se valida en la puerta 4 (informado) y en la puerta 5 (motor + afinado). |
| `nombreTramite`, `ayudaTramite` | servidor | **NO** | Campos derivados de solo lectura: no se aceptan del cliente y su getter los recalcula desde `tramite`. |
| `centrosDisponibles`, `tramitesDisponibles`, `hayQueElegirCentro`, `hayQuePreguntarPresentacion`, `hayQuePreguntarParaQuien` | servidor | **NO** | Son cálculos del servidor; aceptarlos permitiría que el cliente dictara las condiciones con las que se le valida. |

### `insert` / `update`

Entidad: `AsistenteNuevoExpediente`. **Forma elegida**: `createDenyAllProperties` en las dos.
**Origen spec:** las acciones «Crear» y «Modificar» de `entity-AsistenteNuevoExpediente.md` declaran `Input AllowProperties: (ninguna — la ficha no se guarda nunca)`.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| (todos) | — | **NO** | La ficha no se persiste nunca, así que ni un alta ni una modificación pueden aceptar un solo campo del cliente, ni siquiera por el endpoint REST genérico. |

**Único dueño de por qué basta con el deny-all** (el resto del diseño remite aquí). El modelo es `persistable="false"`, y el generador emite entonces la clase como `@MappedSuperclass` (`Entity.java`: `setPersistable(false)` → `mappedSuperClass`, sin `@Entity` ni `@Table`): **no tiene tabla ni entidad JPA**, así que no hay ningún endpoint de guardado utilizable contra él —por `POST /ws/rest/<FQN>` la petición muere en `JPA.edit`/`JPA.manage`, y `Resource.save` solo llega a `modelService.insert/update` **después** de esas dos— y ninguna de las tres vistas declara `save` ni `delete`. El único hook del guardado que **sí** se ejecuta es `modelService.validate`, donde actúa el deny-all, que impide además que el cliente dicte un solo campo; por eso `validateInsert`/`validateUpdate`/`validateRemove` serían código inalcanzable y **MUST NOT** sobrescribirse, y si algún día hiciera falta rechazar además la operación el sitio es el `auth-ventanilla.xml` del Paso 10, que es lo que mira `security.check` antes de `JPA.edit`. `remove` no necesita nada: el permiso de `auth-ventanilla.xml` lo niega y no hay campos que filtrar. Misma clasificación que el hermano `NuevoExpedienteServiceImpl`, también `persistable="false"`.

---

### Trazabilidad — Reglas de UI (fila que le corresponde)

| U | Origen spec | Ubicación |
|---|---|---|
| U-nuevo-expediente-015 | RUI-nuevo-expediente-formulario-011 | `AsistenteNuevoExpedienteController.validateTriggerInitialEvent` → `doResponseBusinessMessagesAsError("No es posible crear el expediente", …)`; los avisos de pregunta sin contestar salen del `<action-validate>` local, sin título. En los dos casos el error corta el `action-group` y el formulario sigue abierto |
