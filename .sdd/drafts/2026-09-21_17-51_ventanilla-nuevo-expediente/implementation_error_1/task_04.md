---
type: implementation-task
template: system
---

# Tarea 04 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality
- k-i18n

Implementa el controlador del asistente con sus cuatro `@CallMethod` y los dos conversores privados a
referencia. Depende del servicio (tarea 03), ya en el árbol.

## Fila de la tabla «Ficheros a crear o modificar»


| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/system/ventanilla/controller/AsistenteNuevoExpedienteController.java` | Crear | k-sistemas (controladores.md) | Los cuatro `@CallMethod` que usan las vistas |

## Nota del diseño sobre los comentarios

> **Los comentarios de este `design.md` NO se transcriben al código.** Son material de `.sdd/`: explican el diseño, no acompañan al código. **MUST NOT** aparecer en los `.java` los comentarios `//` de estos pasos, ni ningún identificador de la spec o del diseño (`V-`, `R-`, `U-`, `CC-`, `ESC-`, `HU-`, `VAL-`, `RN-`, `RUI-`), ni la justificación de una decisión «para que no se pierda» (`k-code-quality/comentarios.md`): lo que el código no revela por sí solo se dice con el nombre del método o de la variable, y lo demás vive aquí y en `decisiones.md`.

---

## Texto del diseño — Paso 5

### Paso 5 — Controlador `AsistenteNuevoExpedienteController`

```java
// Clase: com.educaflow.system.ventanilla.controller.AsistenteNuevoExpedienteController
//   @Inject private ModelServiceFactory modelServiceFactory;
//   En cada método: (AsistenteNuevoExpedienteService) modelServiceFactory.resolve(AsistenteNuevoExpediente.class)
//   y ActionRequestHelper<AsistenteNuevoExpediente> / ActionResponseHelper.
//   Ninguno lleva @Transactional: ninguno escribe en base de datos.

// Los CUATRO métodos siguen el MISMO tramo, el que ya escribía validateCrear: construir el bean con
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
//   NO devuelve `hayQueElegirCentro`: en este paso lo trae el `<context>` del paso 1 y el servidor
//   ni lo calcula ni lo lee.

@CallMethod
public void recalcular(ActionRequest actionRequest, ActionResponse actionResponse);
//   getModel(allowPropertiesRecalcular()) → validateRecalcular → (mensajes: error y return) →
//   recalcular, y devuelve los seis valores que la pantalla del paso 3 necesita: `nombreTramite`,
//   `ayudaTramite`, `presentadoEnPapel`, `presentadoEnRepresentacion`,
//   `hayQuePreguntarPresentacion` y `hayQuePreguntarParaQuien`.
//   Los dos primeros son campos derivados: se leen del getter del bean (que los recalcula desde
//   `tramite`), no de ninguna asignación previa del servicio.

@CallMethod
public void validateCrear(ActionRequest actionRequest, ActionResponse actionResponse);
//   getModel(allowPropertiesCrear()) → validateCrear y, si hay mensajes, los devuelve con
//   doResponseBusinessMessagesAsError(I18n.get("No es posible crear el expediente"), …) — el mismo
//   título que usa TramitadorController, para que el usuario vea siempre el mismo encabezado rechace
//   quien rechace. Los tres anteriores usan la sobrecarga SIN título, que es la forma normal del
//   proyecto para el rechazo de una precondición. El error corta el action-group y el alta no llega
//   a dispararse.

private Map<String, Object> toReferenciaCentro(Centro centro);
//   {id, name}: lo que el listado de centros necesita para pintar la fila y para que el clic
//   devuelva el id.

private Map<String, Object> toReferenciaTramite(Tramite tramite);
//   {id, name, tipoTramite: {id, name}}: el listado de trámites agrupa por `tipoTramite`, así que la
//   referencia del tipo tiene que viajar en cada fila.
//   El nombre del trámite se traduce con la clave REAL del catálogo, I18n.get("value:" + getName()),
//   nunca con I18n.get(getName()): el build registra el `<name>` de cada `TramiteInstance.xml` con el
//   prefijo `value:` (los i18n_*.csv de `tramites/**` contienen literalmente
//   "value:Anulación de matrícula en ciclo formativo"), y `I18nBundle.handleGetObject` devuelve la
//   clave cuando no hay entrada, así que sin el prefijo un usuario en catalán vería siempre el
//   castellano sin ningún error. Es la forma que ya usa `system/expedientes/views/Tramites.xml`
//   (`_t("value:"+name)`).
//   El nombre del `tipoTramite` viaja SIN traducir: viene del data-init `TipoTramites.xml`, que el
//   build no extrae a CSV, así que no hay ninguna clave que buscar y el I18n.get no haría nada.
```

**Verificar al final:** `grep -n "actionRequest, ActionResponse actionResponse" …/AsistenteNuevoExpedienteController.java` devuelve cuatro líneas (los parámetros se llaman siempre así).

---

## Frontera de confianza — AllowProperties por acción (cada `@CallMethod` usa la de su acción)

## Frontera de confianza — AllowProperties por acción

### `AsistenteNuevoExpedienteServiceImpl.prepararCentros` (invocado desde `AsistenteNuevoExpedienteController.prepararCentros`)

Entidad: `AsistenteNuevoExpediente`. **Forma elegida**: `createDenyAllProperties`.
**Origen spec:** la acción no aparece en `entity-AsistenteNuevoExpediente.md`; es el arranque del asistente y no recibe ningún dato del usuario.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| (todos) | servidor | **NO** | La acción solo lee el usuario autenticado. `centrosDisponibles`, `hayQueElegirCentro` y `centro` los asigna `fireActionRule_AsignarCentrosDisponibles`, igual que en las otras dos acciones: todo campo `servidor` se escribe dentro de un `fireActionRule_*`. |

### `AsistenteNuevoExpedienteServiceImpl.prepararTramites` (invocado desde `AsistenteNuevoExpedienteController.prepararTramites`)

Entidad: `AsistenteNuevoExpediente`. **Forma elegida**: `createAllowProperties`.
**Origen spec:** el centro es el campo `centro` de `entity-AsistenteNuevoExpediente.md`, elegido en el paso anterior.

| Campo | Origen | En whitelist | Justificación / Ubicación de la asignación |
|---|---|---|---|
| `centro` | cliente | sí | El centro elegido en el paso 1. Un centro ajeno no filtra nada: `getPerfilesDeInicioSobreTramite` devuelve vacío para un centro al que el usuario no pertenece. |
| `tramitesDisponibles` | servidor | **NO** | Asignado en `fireActionRule_AsignarTramitesDisponibles`. |
| `hayQueElegirCentro` | servidor | **NO** | Esta acción **no lo toca**: lo calculó `prepararCentros` en el paso 1 y la vista lo transporta en su `<context>`. Fuera de la whitelist, así que el cliente tampoco puede dictarlo; y el servidor no lo lee en ninguna acción, solo gobierna el rótulo del botón de salida. |
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
| `centrosDisponibles`, `tramitesDisponibles`, `hayQueElegirCentro` | servidor | **NO** | No intervienen en este paso. `hayQueElegirCentro` solo viaja en el registro de la vista, para el rótulo del botón del paso 2; ninguna acción del servidor lo lee. |

### `AsistenteNuevoExpedienteServiceImpl.validateCrear` (invocado desde `AsistenteNuevoExpedienteController.validateCrear`)

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

El modelo es `persistable="false"`, es decir `@MappedSuperclass`: **no tiene tabla ni entidad JPA**, así que no hay ningún endpoint de guardado utilizable contra él (por `POST /ws/rest/<FQN>` la petición muere en `JPA.edit`/`JPA.manage`, y `Resource.save` solo llega a `modelService.insert/update` después de las dos) y ninguna de las tres vistas declara `save` ni `delete`. El deny-all es lo que **sí** se ejecuta —`modelService.validate` corre antes que todo eso— e impide además que el cliente dicte un solo campo. `remove` no necesita nada: el permiso de `auth-ventanilla.xml` lo niega y no hay campos que filtrar.

---

## Trazabilidad — la regla de UI que aterriza en el controlador

| U | Origen spec | Ubicación |
|---|---|---|
| U-nuevo-expediente-015 | RUI-nuevo-expediente-formulario-011 | `AsistenteNuevoExpedienteController.validateCrear` → `doResponseBusinessMessagesAsError("No es posible crear el expediente", …)`; los avisos de pregunta sin contestar salen del `<action-validate>` local, sin título. En los dos casos el error corta el `action-group` y el formulario sigue abierto |

## Notas y supuestos del diseño

## Notas y supuestos

- **El paso 1 se abre como pestaña, no como ventana emergente.** El spec describe «Listado de centros» como ventana emergente. Con una emergente, el clic en una fila del listado no puede cerrarla (`DefaultActionExecutor.#closeView()` se rinde si hay emergentes y el `FormActionHandler` del listado embebido no tiene `closeHandler`), así que el asistente se quedaría a medio cerrar al crear el expediente. El paso 3, que es donde el spec vuelve a pedir ventana emergente, sí lo es. Detalle y alternativas en **D1** de `decisiones.md`.
- **Mientras el paso 1 decide, no muestra nada.** El listado de centros y el botón «Cancelar» solo se ven si `hayQueElegirCentro`, así que en los casos «ningún centro» y «un solo centro» el usuario **nunca** ve el listado de centros, como exigen ESC-001 y ESC-003: la ventana existe un instante, pero vacía, y se cierra sola.
- **El modelo vive en `domains/` y no en `views-models/`.** Motivo y alternativa en **D6** de `decisiones.md`.
- **`hayQuePreguntarPresentacion`, `hayQuePreguntarParaQuien`, `centrosDisponibles`, `tramitesDisponibles` y `hayQueElegirCentro` no están en `entity-AsistenteNuevoExpediente.md`.** No son campos de negocio: son la forma de que el servidor —único que conoce los perfiles del usuario— conteste a las reglas de UI que el propio spec exige (`RUI-…-formulario-002` a `-009` y `RUI-…-arbol-tramites-001`). Sin ellos esas reglas no se podrían evaluar en la vista, o habría que replicar la clasificación en JavaScript (**D2** y **D3** de `decisiones.md`).
- **Las dos preguntas no llevan `requiredIf`.** Con `requiredIf`, la acción `validate` del framework cortaría antes con su mensaje genérico y el usuario nunca vería «Debe indicar cómo se presenta el expediente» / «Debe indicar para quién es el expediente», que el spec fija literalmente. La obligatoriedad se expresa con el `<action-validate>` local y, sobre todo, con las puertas 3 y 4 de `validateCrear`.
- **El alta la ejecuta el controlador del tramitador, y V-007/V-008 no la defienden — riesgo aceptado.** El afinado propio de la ventanilla (V-007 y V-008) solo se evalúa en `…-Remote-validateCrear-action`, que es una llamada previa e independiente dentro del `action-group` del botón; quien invoque directamente `TramitadorController.triggerInitialEvent` (por la acción de esta vista, por la del tramitador o por su `@CallMethod`) crea el expediente sin pasar por él. Lo que la ventanilla declara es solo el **nombre local** de la acción (`sysVentanilla.Main@AsistenteNuevoExpediente-Remote-triggerInitialEvent-action`); el **controlador** se reutiliza tal cual, como exige `design-guidelines.md`, y el afinado no se traslada al motor. Motivo, alternativa descartada y qué **MUST NOT** hacerse como parche puntual: **D7** de `decisiones.md`.
- **Las cuatro acciones propias declaran su `validate<Accion>`, y las consultan los dos lados.** También `prepararCentros`, cuyo validador no tiene hoy ninguna precondición: el par acción + validador es el contrato público mínimo de toda acción (`k-sistemas/servicios.md`). El **controlador** consulta el validador y contesta sus mensajes con `doResponseBusinessMessagesAsError` antes de invocar la acción, y la **acción** empieza igualmente por `validate<Accion>(...).ifPresent(BusinessMessages::throwIfInvalid)` como red de seguridad de cualquier otro llamante. Sin el tramo del controlador el mensaje llegaría al usuario como `ValidationException` cruda, que es el mismo 500 disfrazado que se quería evitar al sustituir `IllegalStateException`.
- **La ficha no se guarda porque no hay dónde guardarla.** `persistable="false"` genera el modelo como `@MappedSuperclass`: sin `@Entity` ni `@Table`, no tiene tabla ni endpoint de guardado utilizable, y ninguna de las tres vistas declara `save` ni `delete`. La única defensa que hace falta —y la única que se ejecuta— es el deny-all de `allowPropertiesInsert/Update`, que corre en `modelService.validate`.
- **El centro del salto 2→3 llega bajo `_parent`, y por eso el paso 3 lo lee con `__parent__`.** No es un riesgo pendiente de confirmar: el código del fork lo determina. `handleRowDoubleClickActionId` (`axelor-front/src/views/grid/builder/grid.tsx`) ejecuta la acción de la fila con `context: {...processContextValues(record), selected, _signal, id}` —solo la fila—, y `DefaultActionExecutor.#execute` (`view-containers/action/executor.ts`) lo funde sobre `this.#handler.getContext()`, que para un `panel-related` es el `getActionContext()` de `one-to-many.tsx`: `{_viewType, _views, _ids?, _parent: <contexto del formulario padre>}`. Los campos del formulario del paso 2 viajan por tanto **dentro de `_parent`**, y la fila pulsada es un `Tramite`, que no tiene `centro`: con `eval: centro?.id` el `_centroId` sería null, el paso 3 se abriría sin centro y `validateCrear` respondería «Debe indicar el centro», rompiendo ESC-005 a ESC-013 y ESC-017 a ESC-022. El `<context>` del paso 3 usa `eval: __parent__?.centro?.id` (`__parent__` → `Context.getParent()` de `axelor-core/.../ScriptBindings.java`, construido desde `_parent._model`; forma ya usada en `system/gestioncentro/views/gestion-centro-usuarios-autorizados.xml`). El `<context>` del paso 2 se queda en `eval: centro?.id ?: id` porque ahí la fila pulsada **es** el propio `Centro`.
- **El perfil de inicio NO se obtiene llamando a `PerfilesUsuarioService.getPerfil(tramite, user, centro, presentadoEnPapel)`, pese a que `design-guidelines.md` pide reutilizarla tal cual.** `getPerfil` lanza `IllegalStateException` precisamente en los dos casos que HU-006 exige rechazar con un mensaje de negocio, y `recalcular` —que es quien necesita el perfil— corre en el `onNew`/`onChange`, antes de cualquier validación: llamarla ahí convertiría una petición manipulada en un 500 en vez del mensaje que pide HU-006. En su lugar, `perfilDeInicioPara(boolean)` deriva el perfil de inicio preguntando a los predicados del propio enum `Profile` (`puedeCrearExpediente()` y `permitePresentacionEnPapel()`), sin consultar los perfiles del usuario. Detalle y justificación completa en **D5** de `decisiones.md`.
