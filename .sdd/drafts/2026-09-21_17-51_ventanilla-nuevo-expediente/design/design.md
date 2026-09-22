---
type: design
template: system
---

# Diseño: Ventanilla — nuevo expediente

**Objetivo:** que cualquier usuario cree un expediente con un asistente de tres pasos (centro → trámite → contexto del trámite) que solo le ofrece los centros y los trámites en los que realmente puede iniciar, y que vuelve a comprobarlo todo en el servidor.
**Capa:** system/ventanilla
**Especificación de origen:** .sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/specification.md
**Skills necesarios para la implementación:** k-sistemas, k-code-quality, k-secure-coding, k-vistas, k-validaciones, k-datainit, k-i18n

El sistema es **nuevo** y **autónomo**: no toca `subsystem/tramitador` ni `subsystem/expedientes`, y reutiliza tal cual `PerfilesUsuarioService.getPerfilesDeInicioSobreTramite` (no `getPerfil`, ver «Notas y supuestos»), `TramitadorService.validateTriggerInitialEvent` y el controlador `TramitadorController.triggerInitialEvent` (al que apunta la acción propia `sysVentanilla.Main@AsistenteNuevoExpediente-Remote-triggerInitialEvent-action`, declarada en las vistas del sistema). Convive con la entrada antigua «Expedientes → Trámites» de `system/expedientes`, que no se modifica; el código equivalente de allí se ha tomado como punto de partida y se ha **copiado, no referenciado** (un sistema no puede depender de otro).

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/system/ventanilla/domains/AsistenteNuevoExpediente.xml` | Crear | k-sistemas (modelos.md) | Modelo de pantalla del asistente, `persistable="false"` |
| `src/main/java/com/educaflow/system/ventanilla/service/AsistenteNuevoExpedienteService.java` | Crear | k-sistemas (servicios.md) | Interfaz del servicio del asistente |
| `src/main/java/com/educaflow/system/ventanilla/service/impl/AsistenteNuevoExpedienteServiceImpl.java` | Crear | k-sistemas (servicios.md), k-validaciones, k-secure-coding | Implementación: candidatos, opciones y validación de la creación |
| `src/main/java/com/educaflow/system/ventanilla/db/repo/VentanillaRepository.java` | Crear | k-sistemas (db) | Repositorio del sistema con la única consulta: los trámites del catálogo que se pueden clasificar |
| `src/main/java/com/educaflow/system/ventanilla/controller/AsistenteNuevoExpedienteController.java` | Crear | k-sistemas (controladores.md) | Los cuatro `@CallMethod` que usan las vistas |
| `src/main/java/com/educaflow/system/ventanilla/views/ElegirCentro-AsistenteNuevoExpediente.xml` | Crear | k-vistas (forms.md, grids.md, actions.md) | Paso 1: listado de centros candidatos |
| `src/main/java/com/educaflow/system/ventanilla/views/ElegirTramite-AsistenteNuevoExpediente.xml` | Crear | k-vistas (forms.md, grids.md, actions.md) | Paso 2: listado de trámites agrupado por tipo de trámite |
| `src/main/java/com/educaflow/system/ventanilla/views/Main-AsistenteNuevoExpediente.xml` | Crear | k-vistas (forms.md, actions.md) | Paso 3: contexto del trámite y creación |
| `src/main/java/com/educaflow/system/ventanilla/data-init/input-config.xml` | Crear | k-datainit | Manifiesto del data-init del sistema |
| `src/main/java/com/educaflow/system/ventanilla/data-init/input/auth-ventanilla.xml` | Crear | k-datainit | Permiso del modelo de pantalla |
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Añadir el menú «Ventanilla» y su hijo «Nuevo expediente» |
| `src/main/resources/data-init/input/auth.xml` | Modificar | k-datainit | Referenciar `AsistenteNuevoExpediente.all` en los grupos `admins` y `users` |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.
>
> **Los comentarios de este `design.md` NO se transcriben al código.** Son material de `.sdd/`: explican el diseño, no acompañan al código. **MUST NOT** aparecer en los `.java` los comentarios `//` de estos pasos, ni ningún identificador de la spec o del diseño (`V-`, `R-`, `U-`, `CC-`, `ESC-`, `HU-`, `VAL-`, `RN-`, `RUI-`), ni la justificación de una decisión «para que no se pierda» (`k-code-quality/comentarios.md`): lo que el código no revela por sí solo se dice con el nombre del método o de la variable, y lo demás vive aquí y en `decisiones.md`.

---

## Cómo se encadenan los tres pasos (leer antes de implementar)

El asistente son **tres vistas** sobre el **mismo** modelo de pantalla, cada una con su propio `onNew` que la rellena desde el contexto que recibe:

```
menú «Ventanilla → Nuevo expediente»
  └─ sysVentanilla.ElegirCentro@AsistenteNuevoExpediente-action        (pestaña)
        ├─ 0 centros candidatos  → aviso y se cierra
        ├─ 1 centro candidato    → abre el paso 2 con ese centro y se cierra
        └─ 2 o más               → se queda y muestra el listado de centros
              └─ clic en una fila → sysVentanilla.ElegirTramite@…-action  (pestaña) y se cierra
                    └─ clic en un trámite → sysVentanilla.Main@…-action   (pestaña) y se cierra
                          ├─ «Atrás»            → reabre el paso 2 con el mismo centro y se cierra
                          └─ «Crear expediente» → sysVentanilla.Main@AsistenteNuevoExpediente-Remote-triggerInitialEvent-action
                                                   (→ TramitadorController.triggerInitialEvent)
```

Tres reglas, y no hay más que recordar:

1. **Cada paso abre el siguiente y se cierra a sí mismo**: un `<action-group>` con el `<action-view>` del siguiente y `close` al final (el framework exige que `close` sea la última acción del grupo). Así nunca hay dos pasos abiertos a la vez y «Crear expediente» deja la pantalla limpia.
2. **Los TRES pasos son pestañas. Ninguno es una ventana emergente, y eso MUST NOT cambiarse:** **MUST NOT** añadir `<view-param name="popup">` ni `popup-save` a ninguno de los tres `<action-view>`. Por qué la topología es esa —el mecanismo de `axelor-front`, el código citado y las alternativas evaluadas— está en **D1** de `decisiones.md`, su **único dueño**.
3. **Cada valor arrastrado se lee de un sitio distinto: NO hay una forma única, y las expresiones no son intercambiables.** Una acción disparada desde una **fila** de un listado embebido recibe el contexto de la fila (`handleRowDoubleClickActionId`, `axelor-front/src/views/grid/builder/grid.tsx`: `{...processContextValues(record), selected, _signal, id}`) fundido sobre `getActionContext()` del `panel-related` (`one-to-many.tsx`), que mete el contexto del formulario padre **bajo `_parent`**, no en el primer nivel; `__parent__` resuelve a `Context.getParent()` (`axelor-core/.../ScriptBindings.java`) desde `_parent._model` y es la forma que el proyecto ya usa (`system/gestioncentro/views/gestion-centro-usuarios-autorizados.xml`). Por eso **qué lleva la fila** decide cada expresión, una por valor arrastrado:

   - **`_centroId` del paso 2 — `eval: centro?.id ?: id`: la FILA, no `__parent__`.** Desde el formulario del paso 3 («Atrás») el registro actual ya lleva el centro (`centro?.id`); desde una fila del listado del paso 1 la fila **es** el `Centro`, así que su `id` **es** el centro. **MUST NOT** cambiarse por `__parent__?.centro?.id`: el formulario padre es el del paso 1 y ahí `centro` está a **null** justo cuando hay varios centros —`prepararCentros` solo lo fija si hay exactamente uno—, que es el único caso en que se llega a pulsar una fila; el salto llegaría sin centro y sin ningún error (ESC-005, ESC-019).
   - **`_hayQueElegirCentro` del paso 2 — `eval: hayQueElegirCentro == null ? __parent__?.hayQueElegirCentro : hayQueElegirCentro`: test de nulidad contra `__parent__`.** No puede resolverse como el centro porque la fila pulsada es un `Centro` y **no lleva ese campo**: cuando el salto sale de una fila el valor solo está en el formulario padre, y cuando sale del formulario del paso 3 lo lleva el registro actual.
   - **`_centroId` y `_hayQueElegirCentro` del paso 3 — `eval: __parent__?.centro?.id` y `eval: __parent__?.hayQueElegirCentro`: los DOS con `__parent__`.** Aquí la fila pulsada es un `Tramite`, que no tiene ninguno de los dos, y el formulario padre es el del paso 2, que sí los tiene fijados por su `onNew`.
   - **`_tramiteId` del paso 3 — `eval: id`: la FILA.** El único salto al paso 3 sale de una fila que **es** el `Tramite`.

   `hayQueElegirCentro` lo calcula el paso 1 y lo transporta la vista (ver Paso 1).

---

## Pasos

### Paso 1 — Modelo de dominio `AsistenteNuevoExpediente`

Fichero: `design/domains/AsistenteNuevoExpediente.xml` → `src/main/java/com/educaflow/system/ventanilla/domains/AsistenteNuevoExpediente.xml`.

**Resumen estructural:** una sola entidad, `AsistenteNuevoExpediente`, con `persistable="false"` y `<module name="ventanilla" package="com.educaflow.system.ventanilla.db"/>`. Once campos en cuatro grupos:

| Grupo | Campos | Origen |
|---|---|---|
| Elección del usuario | `centro` (m2o `Centro`), `tramite` (m2o `Tramite`), `presentadoEnPapel` (boolean **nullable**), `presentadoEnRepresentacion` (boolean **nullable**) | `cliente` |
| Instantánea del trámite | `nombreTramite` (string `transient`), `ayudaTramite` (string `large` `transient`) | `servidor` (derivados de solo lectura) |
| Candidatos de cada paso | `centrosDisponibles` (m2m `Centro`), `tramitesDisponibles` (m2m `Tramite`), `hayQueElegirCentro` (boolean) | `servidor` |
| Qué preguntas se hacen | `hayQuePreguntarPresentacion`, `hayQuePreguntarParaQuien` (boolean) | `servidor` |

- Los nombres `tramite`, `centro`, `presentadoEnPapel` y `presentadoEnRepresentacion` son **literales obligatorios**: `TramitadorController.triggerInitialEvent` —al que llama la acción `sysVentanilla.Main@AsistenteNuevoExpediente-Remote-triggerInitialEvent-action`— los lee del formulario por esos nombres.
- `presentadoEnPapel` es **nullable**: el interruptor booleano del sistema antiguo siempre vale `false` y no distingue «no contestado» de «lo presento yo mismo», que es justo lo que el asistente necesita (`VAL-AsistenteNuevoExpediente-003`).
- **`nombreTramite` y `ayudaTramite` son campos DERIVADOS, no campos que una regla asigne.** `CC-AsistenteNuevoExpediente-001` y `-002` son de `momento: lectura` y `sobreescribible: nunca`, y ese es exactamente el mapeo «campo derivado de solo lectura» de la conversión spec → V/R/U: se declaran `transient="true"` con cuerpo (el generador emite `computeX()` y el getter hace `x = computeX(); return x;`), así que el getter los recalcula siempre desde `tramite` y ninguna acción del servicio los asigna ni los acepta del cliente. No necesitan ninguna `R-` que los escriba. La garantía **no** es «no tienen setter» (`Property.createSetterMethod()` lo genera igual), sino que el getter recalcula y que los dos campos están fuera de **todas** las `AllowProperties`. El cuerpo lee el **campo** `tramite`, nunca `getTramite()` (`Mapper.findComputeDependencies` solo registra las dependencias que ve como `GETFIELD`); mismo motivo documentado en `subsystem/sistemaeducativo/domains/Grado.xml`. El cuerpo de `nombreTramite` devuelve `I18n.get("value:" + tramite.getName())`: `value:` es el prefijo con el que el build registra el `<name>` de cada `TramiteInstance.xml` (los `i18n_*.csv` de `tramites/**` contienen literalmente `"value:Anulación de matrícula en ciclo formativo"`) y `I18nBundle.handleGetObject` devuelve la clave cuando no hay entrada, así que sin el prefijo un usuario en catalán vería siempre el castellano y sin ningún error. Este cuerpo es el **único** sitio del lado Java que compone esa clave.
- Ningún campo lleva `required="true"`: los que rellena el servidor no pueden llevarlo (`k-sistemas/modelos.md`) y los dos que contesta el usuario se exigen en el servidor, no con Bean Validation.
- **`hayQuePreguntarPresentacion` y `hayQuePreguntarParaQuien` son el oráculo que la vista consulta** (D2 de `decisiones.md`): el servidor publica la decisión «¿hay que preguntar esto?» ya tomada, no las mitades con las que se toma, así que la vista nunca compone una condición y la clasificación tiene un único dueño. Son los mismos dos nombres que ya usa `system/expedientes/domains/NuevoExpediente.xml`. Semántica: `true` → se pregunta y la respuesta llega sin marcar; `false` → no se pregunta, el servidor fija el único valor posible (o ninguno si no hay ninguno) y el campo no se muestra.
- **`hayQueElegirCentro` lo calcula el servidor en el paso 1 y de ahí en adelante lo transporta la vista. Este bullet es su ÚNICO dueño: el resto del diseño solo remite aquí.** Los pasos 2 y 3 lo reciben en el `<context>` de su `<action-view>` y lo fijan en su registro con el `<action-record>` del `onNew`; ninguna acción del servidor vuelve a calcularlo ni lo lee (está fuera de **todas** las `AllowProperties`), así que manipularlo solo cambia el rótulo del botón de salida del paso 2, nunca lo que el servidor ofrece o acepta. Recalcularlo en el paso 2 costaría además recorrer TODOS los centros del usuario por TODO el catálogo —(N_centros+1) x N_tramites clasificaciones de perfiles, cinco repositorios `AceProfile*` cada una— en cada apertura del paso 2 y en cada «Atrás» desde el paso 3, para un valor que solo gobierna el rótulo de un botón.
- **Dos campos tienen doble clasificación, y es deliberado.** No hay ambigüedad porque las whitelists son **por acción**: en la acción (o en la rama) en la que el servidor dicta el valor, el cliente no puede tocarlo.
  - **`centro`** es `cliente` en `prepararTramites`, `recalcular` y `validateTriggerInitialEvent` (es lo que el usuario eligió en el paso 1, y está en la línea `Input AllowProperties` de «Crear expediente»), y `servidor` en `prepararCentros`, que es la **única** acción que lo decide (lo fija su regla `fireActionRule_AsignarArranqueDelAsistente` cuando el usuario tiene un único centro candidato). `prepararCentros` es deny-all, así que ahí el cliente no lo puede dictar.
  - **`presentadoEnPapel`** es `cliente` en `recalcular` y en `validateTriggerInitialEvent` —es la respuesta del usuario a «¿Cómo se presenta?» y está en las dos whitelists, como dicen las tablas de «Frontera de confianza»— y `servidor` **solo en la rama en la que la pregunta no se hace**: ahí `fireActionRule_AsignarFormaDePresentar` lo sobrescribe **incondicionalmente** con la única forma posible, así que lo que mandara el cliente se descarta. La condición que separa las dos ramas mira si la pregunta se hace, **nunca** si el campo llegó a null: no es la guarda de nulidad que `k-secure-coding` §3.3 prohíbe.

**Verificar al final:** `./gradlew compileJava` genera `com.educaflow.system.ventanilla.db.AsistenteNuevoExpediente` con los once getters; `grep -rn '"value:"' src/main/java/com/educaflow/system/ventanilla/` devuelve **una sola** línea, la del cuerpo de `nombreTramite` en `domains/AsistenteNuevoExpediente.xml`.

---

### Paso 2 — Interfaz `AsistenteNuevoExpedienteService`

```java
// Clase: com.educaflow.system.ventanilla.service.AsistenteNuevoExpedienteService
//   extends ModelService<AsistenteNuevoExpediente>

// Paso 1: rellena los centros candidatos y decide el arranque del asistente.
AsistenteNuevoExpediente prepararCentros(AsistenteNuevoExpediente asistente);
Optional<BusinessMessages> validatePrepararCentros(AsistenteNuevoExpediente asistente);
AllowProperties allowPropertiesPrepararCentros();

// Paso 2: rellena los trámites que el usuario puede iniciar en el centro ya elegido.
AsistenteNuevoExpediente prepararTramites(AsistenteNuevoExpediente asistente);
Optional<BusinessMessages> validatePrepararTramites(AsistenteNuevoExpediente asistente);
AllowProperties allowPropertiesPrepararTramites();

// Paso 3: instantánea del trámite y qué puede elegir el usuario con la forma de presentar vigente.
AsistenteNuevoExpediente recalcular(AsistenteNuevoExpediente asistente);
Optional<BusinessMessages> validateRecalcular(AsistenteNuevoExpediente asistente);
AllowProperties allowPropertiesRecalcular();

// Paso 3: la comprobación completa antes de crear el expediente.
Optional<BusinessMessages> validateTriggerInitialEvent(AsistenteNuevoExpediente asistente);
AllowProperties allowPropertiesTriggerInitialEvent();
```

- **MUST NOT** re-declarar `insert`/`update`/`remove` ni sus `validate*`/`allowProperties*`: el modelo no se persiste nunca y el `DefaultModelService` ya los trae.
- **Cada acción propia va con su `validate<Accion>`, sin excepciones** (`k-sistemas/servicios.md`, «Regla obligatoria»), también las de solo lectura, y **cada acción empieza** por `validate<Accion>(asistente).ifPresent(BusinessMessages::throwIfInvalid)`. Las precondiciones de `prepararTramites` y `recalcular` (centro ausente, trámite ausente o no evaluable) se rechazan así, con el **mismo literal de negocio** que usa `validateTriggerInitialEvent`, y **nunca** con `IllegalStateException` ni `IllegalArgumentException`: una petición manipulada al `@CallMethod` debe contestar un mensaje, no un 500.

**Verificar al final:** `grep -nE "validate(PrepararCentros|PrepararTramites|Recalcular|TriggerInitialEvent)\(" src/main/java/com/educaflow/system/ventanilla/service/AsistenteNuevoExpedienteService.java` devuelve cuatro líneas, una por acción propia.

---

### Paso 3 — Implementación `AsistenteNuevoExpedienteServiceImpl`

```java
// Clase: com.educaflow.system.ventanilla.service.impl.AsistenteNuevoExpedienteServiceImpl
//   extends DefaultModelService<AsistenteNuevoExpediente>
//   implements AsistenteNuevoExpedienteService
//
// Colaboradores (campos @Inject):
//   TramitadorService        tramitadorService;        // oráculo del motor de tramitación
//   PerfilesUsuarioService   perfilesUsuarioService;   // qué perfiles tiene el usuario sobre un trámite en un centro
//   VentanillaRepository     ventanillaRepository;     // com.educaflow.system.ventanilla.db.repo.VentanillaRepository
//
// Constructor obligatorio (lo invoca ModelServiceFactory por reflexión):
public AsistenteNuevoExpedienteServiceImpl(Class<AsistenteNuevoExpediente> model,
                                           Repository<AsistenteNuevoExpediente> repository);
//   Cuerpo: super(model, repository);
```

#### Acciones

```java
public AsistenteNuevoExpediente prepararCentros(AsistenteNuevoExpediente asistente);
//   Primera línea: validatePrepararCentros(asistente).ifPresent(BusinessMessages::throwIfInvalid).
//   Aplica UNA sola regla:
//     - R-AsistenteNuevoExpediente-003 (Origen spec: —) llamando a fireActionRule_AsignarArranqueDelAsistente.
//   No persiste nada (el modelo no es persistible) y devuelve el mismo bean.

public AsistenteNuevoExpediente prepararTramites(AsistenteNuevoExpediente asistente);
//   Primera línea: validatePrepararTramites(asistente).ifPresent(BusinessMessages::throwIfInvalid).
//   Aplica UNA sola regla:
//     - R-AsistenteNuevoExpediente-004 llamando a fireActionRule_AsignarTramitesDisponibles.
//   NO recalcula `hayQueElegirCentro` ni toca el `centro`: los dos los decidió el paso 1 (ver
//   Paso 1). Tampoco rellena `centrosDisponibles`, que este paso no pinta.

public AsistenteNuevoExpediente recalcular(AsistenteNuevoExpediente asistente);
//   Primera línea: validateRecalcular(asistente).ifPresent(BusinessMessages::throwIfInvalid).
//   Aplica UNA sola regla:
//     - R-AsistenteNuevoExpediente-005 (fireActionRule_AsignarPresentacion)
//   Las dos preguntas del formulario se resuelven juntas porque no son separables: la de «para quién
//   es» solo puede contestarse sobre el `presentadoEnPapel` que la otra acaba de fijar, y las dos
//   salen de la MISMA matriz forma × destinatario, que así se calcula una vez por `recalcular` en vez
//   de interrogar seis veces al oráculo.
//   `nombreTramite` y `ayudaTramite` NO se asignan aquí: son campos derivados de solo lectura y su
//   getter los recalcula desde `tramite` (paso 1); el controlador los lee del getter al responder.
//   Esta acción NUNCA puede lanzar por una forma de presentar que el usuario no tenga: no consulta
//   PerfilesUsuarioService.getPerfil (ver fireActionRule_AsignarPresentacion).
```

#### Métodos de validación

```java
// Forma canónica de CADA mensaje que estos métodos añaden a su BusinessMessages (k-i18n, reglas 4
// y 5): el texto pasa siempre por I18n.get y el label sale del `title` del dominio, nunca de un
// literal en castellano —los `title` ya están declarados en domains/AsistenteNuevoExpediente.xml—:
//   new BusinessMessage("<campo>",
//                       I18n.get("<mensaje>"),
//                       I18n.get(Mapper.of(AsistenteNuevoExpediente.class)
//                                      .getProperty("<campo>").getTitle()))

public Optional<BusinessMessages> validatePrepararCentros(AsistenteNuevoExpediente asistente);
//   Sin precondiciones: la acción no lee nada del cliente (whitelist deny-all) y todo sale del
//   usuario autenticado, así que devuelve Optional.empty(). Se declara igualmente porque el par
//   acción + validador es el contrato público mínimo de toda acción propia, también las de solo
//   lectura (k-sistemas/servicios.md), y es el sitio al que añadir una precondición futura.

public Optional<BusinessMessages> validatePrepararTramites(AsistenteNuevoExpediente asistente);
//   Una sola puerta: validateCentroIndicado(asistente), el dueño privado de
//   V-AsistenteNuevoExpediente-001 (el centro tiene que venir indicado; es lo que el usuario eligió
//   en el paso 1). Esta acción no tiene ninguna precondición más.

public Optional<BusinessMessages> validateRecalcular(AsistenteNuevoExpediente asistente);
//   Dos puertas, en este orden, y se para en la primera que falla: los mensajes se devuelven TAL
//   CUAL y no se evalúa la siguiente.
//     1. validateCentroIndicado — V-AsistenteNuevoExpediente-001.
//     2. validateTramiteEvaluable — V-AsistenteNuevoExpediente-009: el otro dato que los pasos
//        anteriores ya fijaron y sin el cual no hay nada que recalcular.

public Optional<BusinessMessages> validateTriggerInitialEvent(AsistenteNuevoExpediente asistente);
//   Comprobación completa y ORDENADA en servidor de la creación, sin fiarse de nada de lo que la
//   pantalla ocultó o calculó. Se para en la primera puerta que falla y devuelve UN solo mensaje,
//   porque el spec exige que un rechazo no arrastre los mensajes de las puertas siguientes
//   (ESC-014, ESC-023, ESC-025). Las puertas, en orden:
//
//     1. validateCentroIndicado(asistente) — V-AsistenteNuevoExpediente-001. Es la ÚNICA puerta de
//        centro/perfil que la ventanilla escribe, porque el motor no puede darla:
//        ContextoTramitacion prohíbe un centro nulo en su constructor compacto, así que su rama
//        equivalente es inalcanzable desde fuera (ver D5 de decisiones.md).
//
//     2. validateTramiteEvaluable(asistente) — V-AsistenteNuevoExpediente-009. Cubre de una vez los
//        dos casos en los que la puerta 5 contestaría un 500 en vez del mensaje que exige HU-006: el
//        trámite ausente —que allowPropertiesTriggerInitialEvent deja pasar, porque una whitelist no exige campos,
//        y que revienta en el Objects.requireNonNull(tramite) del constructor compacto de
//        ContextoTramitacion— y el trámite que el sistema no sabe juzgar —el id de cualquier fila
//        del catálogo sin tipo de trámite o sin tipo de expediente por defecto, que revienta dentro
//        de PerfilesUsuarioServiceImpl—.
//
//   Invocar a los dueños privados de las puertas 1 y 2 es lo que hace que lo que el paso 3 acepta no
//   pueda separarse de lo que «Crear expediente» rechaza. El `return` temprano de cada puerta
//   conserva el «un solo mensaje» de ESC-025.
//
//     3. V-AsistenteNuevoExpediente-002 (Origen spec: VAL-AsistenteNuevoExpediente-003)
//        `presentadoEnPapel` viene informado.
//        Mensaje: I18n.get("Debe indicar cómo se presenta el expediente").
//        Se exige SIEMPRE, se pregunte o no: cuando no se pregunta, `recalcular` ya dejó el campo
//        con la única forma posible, y el controlador del tramitador lee ese mismo campo del
//        formulario. Si el servidor dedujera aquí un valor distinto del que va a enviarse, la
//        validación y el alta dirían cosas distintas. Además el valor de este campo es el que elige
//        el perfil con el que se construye el contexto, así que hay que tenerlo antes de la puerta 5.
//
//     4. V-AsistenteNuevoExpediente-003 (Origen spec: VAL-AsistenteNuevoExpediente-004)
//        `presentadoEnRepresentacion` viene informado.
//        Mensaje: I18n.get("Debe indicar para quién es el expediente"). Mismo razonamiento que la
//        puerta 3.
//
//     5. delega en validateAlta(tramite, centro, presentadoEnPapel, presentadoEnRepresentacion),
//        que construye el ContextoTramitacion con el perfil que EXIGE la forma elegida y aporta
//        V-AsistenteNuevoExpediente-004, -005 y -006 (los tres del motor, con SUS literales) más
//        V-007 y V-008 (el afinado propio de la ventanilla).
//
//   La ventanilla NO reescribe ningún literal del motor: "No puede crear expedientes de este
//   trámite en el centro indicado" y "No puede presentar el expediente de esa forma en el centro
//   indicado" los emite TramitadorService.validateCentroYPerfil, que además tiene un `return`
//   temprano entre los dos, que es el "únicamente ese mensaje" de ESC-014/ESC-023 (ver D5).
//
//   MUST NOT reordenar estas puertas: el orden ES la regla (ver los ESC-014/023/025).

private Optional<BusinessMessages> validateCentroIndicado(AsistenteNuevoExpediente asistente);
//   ÚNICO dueño de V-AsistenteNuevoExpediente-001: el centro tiene que venir indicado (es lo que el
//   usuario eligió en el paso 1). Mensaje: I18n.get("Debe indicar el centro").
//   Lo invocan las tres acciones que necesitan el centro ya elegido: validatePrepararTramites,
//   validateRecalcular (puerta 1) y validateTriggerInitialEvent (puerta 1).

private Optional<BusinessMessages> validateTramiteEvaluable(AsistenteNuevoExpediente asistente);
//   ÚNICO dueño de V-AsistenteNuevoExpediente-009: I18n.get("Debe indicar el trámite"). Lo invocan
//   validateRecalcular (puerta 2) y validateTriggerInitialEvent (puerta 2).
//   NO comprueba «el trámite no es nulo», sino que el trámite está entre los que el sistema SABE
//   juzgar: ventanillaRepository.findTramitesEvaluables() lo contiene (el caso nulo queda cubierto
//   por la misma condición, porque una lista no contiene null). `tramite` está en la whitelist de las
//   dos acciones, así que una petición manipulada puede traer el id de cualquier fila del catálogo, y
//   una fila sin tipo de trámite o sin tipo de expediente por defecto reventaría con NPE dentro de
//   PerfilesUsuarioServiceImpl (Objects.requireNonNull sobre los dos m2o, que son nullable en
//   Tramite.xml).
//   MUST NOT comprobarse a mano getDefaultTipoExpediente() != null && getTipoTramite() != null: ese
//   predicado tiene UN solo dueño, la consulta del repositorio (Paso 4), y escribirlo aquí le daría
//   un segundo.

private Optional<BusinessMessages> validateAlta(Tramite tramite, Centro centro,
                                                boolean presentadoEnPapel,
                                                boolean presentadoEnRepresentacion);
//   EL ORÁCULO del sistema: la única respuesta a «¿se puede crear este expediente así?», y el ÚNICO
//   sitio donde se construye un ContextoTramitacion:
//     new ContextoTramitacion(tramite, centro, perfilDeInicioPara(presentadoEnPapel),
//                             presentadoEnPapel, presentadoEnRepresentacion)
//   El perfil se DEDUCE de la forma de presentar elegida, sin consultar los del usuario y sin
//   nombrar ninguna constante del enum: lo resuelve perfilDeInicioPara (ver «Otras funciones»),
//   ÚNICO dueño de la correspondencia «forma de presentar → perfil de inicio que la exige». Así
//   nunca se llama a getPerfil (que lanza) y es el motor quien redacta los dos mensajes de
//   centro/perfil.
//   Dos puertas EN CADENA, no una suma:
//     1. tramitadorService.validateTriggerInitialEvent(contexto): si devuelve mensajes, se
//        devuelven TAL CUAL y el afinado NO se evalúa. Aporta
//        V-AsistenteNuevoExpediente-004 (Origen spec: VAL-AsistenteNuevoExpediente-005),
//        V-AsistenteNuevoExpediente-005 (Origen spec: VAL-AsistenteNuevoExpediente-006) y
//        V-AsistenteNuevoExpediente-006 (Origen spec: VAL-AsistenteNuevoExpediente-007).
//     2. SOLO si el motor acepta, validateAfinadoDestinatario(contexto) — aporta V-007 y V-008.
//   El corte es lo que hace TOTAL al afinado: solo recibe entradas que el motor ya dio por buenas
//   (el usuario pertenece al centro y tiene ahí perfil de inicio para esa forma), que son las
//   únicas que sabe juzgar; no acepta un caso que no sabe tratar confiando en lo que hizo la otra
//   pieza. Y deja a validateTriggerInitialEvent con UNA sola semántica: todas sus puertas cortan en el primer
//   fallo y devuelven un solo mensaje (ESC-014/015/023/024/025).
//   Lo usan tanto validateTriggerInitialEvent (para rechazar) como admiteAlta (para saber qué ofrecer), así que
//   lo que la pantalla enseña y lo que el servidor acepta no pueden separarse nunca.
//   Puede dar por hecho que el trámite es EVALUABLE: los dos caminos que llegan hasta aquí pasan
//   antes por V-009, es decir por validateTramiteEvaluable —que validateRecalcular ejecuta antes de
//   la regla que llama a admiteAlta, y validateTriggerInitialEvent en su puerta 2—, así que nunca recibe una fila
//   que PerfilesUsuarioServiceImpl no sepa tratar.

private Optional<BusinessMessages> validateAfinadoDestinatario(ContextoTramitacion contexto);
//   PRIMERA rama, antes de nada: si el tipo de trámite NO declara tipo de usuario (el m2o
//   TipoTramite.tipoUsuario no es required), devuelve Optional.empty() — el afinado entero habla de
//   «el tipo de usuario al que va dirigido el trámite», así que sin él no hay nada que afinar y el
//   veredicto del motor se respeta. La condición vive AQUÍ y no dentro de esDestinatarioDelTramite:
//   escondida en el predicado, el mismo `false` RECHAZARÍA en V-007 y ACEPTARÍA en V-008, es decir
//   una decisión de negocio tomada dos veces y en sentidos opuestos sobre un caso que el predicado
//   no sabe juzgar.
//   Solo entonces aplica:
//     - V-AsistenteNuevoExpediente-007 (Origen spec: VAL-AsistenteNuevoExpediente-008): si el
//       expediente es para el propio usuario, lo presenta él mismo y el trámite admite
//       representación, el usuario tiene que ser en ese centro del tipo de usuario al que va
//       dirigido el trámite, o no ser Familiar.
//       Mensaje: I18n.get("No puede crear este expediente para usted mismo en el centro indicado").
//     - V-AsistenteNuevoExpediente-008 (Origen spec: VAL-AsistenteNuevoExpediente-009): si el
//       expediente es en representación de otra persona y lo presenta el propio usuario, tiene que
//       ser Familiar en ese centro, o no ser del tipo de usuario al que va dirigido el trámite.
//       Mensaje: I18n.get("No puede crear este expediente en representación de otra persona en el
//       centro indicado").
//   Las dos condiciones son excluyentes (cada una mira un valor distinto de
//   presentadoEnRepresentacion), así que nunca salen los dos mensajes a la vez.
//   Este afinado es propio de la ventanilla: el motor da por bueno «para mí» siempre y «en
//   representación» siempre que el trámite lo admita.

// MUST NOT sobrescribirse `validateInsert`/`validateUpdate`/`validateRemove`: serían código
// inalcanzable (motivo en «Frontera de confianza → `insert` / `update`»).
```

> Los cuatro helpers privados que devuelven `Optional<BusinessMessages>` van en este bloque y **no** en «Otras funciones» (`k-sistemas/servicios.md`, «Orden de los métodos en la `*Impl`»): quien abra la clase buscando dónde se valida algo tiene aquí **todas** las reglas V del sistema.
>
> **Dos instrucciones valen para TODO el bloque y por eso no se repiten en cada método** (este blockquote es su único dueño): cada regla V tiene **UN solo dueño**, el validador privado que la implementa, así que su condición y su literal **MUST NOT** reescribirse en el validador público que la invoca —el mensaje se lee en el comentario de su dueño y en la matriz de trazabilidad—; y **ningún validador público ejecuta el validador de otra acción**: invoca directamente a los dueños privados que necesita.

#### AllowProperties

```java
public AllowProperties allowPropertiesPrepararCentros();
//   createDenyAllProperties(): esta acción no acepta ningún dato del cliente; todo sale del usuario
//   autenticado (SecurityUtil.getUser()).

public AllowProperties allowPropertiesPrepararTramites();
//   createAllowProperties(Map.of("centro", Map.of())): solo el centro elegido en el paso anterior.
//   Que el centro sea ajeno no hace falta comprobarlo aparte: getPerfilesDeInicioSobreTramite
//   devuelve vacío para un centro al que el usuario no pertenece, así que la lista sale vacía.

public AllowProperties allowPropertiesRecalcular();
//   createAllowProperties(Map.of("tramite", Map.of(), "centro", Map.of(), "presentadoEnPapel", Map.of())):
//   los tres datos de los que depende el cálculo. `presentadoEnRepresentacion` NO entra: esta acción
//   lo recalcula siempre, nunca lo lee.

public AllowProperties allowPropertiesTriggerInitialEvent();
//   createAllowProperties(Map.of("tramite", Map.of(), "centro", Map.of(),
//                                "presentadoEnPapel", Map.of(), "presentadoEnRepresentacion", Map.of())):
//   exactamente los cuatro campos de la línea `Input AllowProperties` de la acción «Crear expediente».

public AllowProperties allowPropertiesInsert();
public AllowProperties allowPropertiesUpdate();
//   createDenyAllProperties() en las dos, por el motivo de «Frontera de confianza → `insert` /
//   `update`». Son las dos ÚNICAS sobrescrituras del ciclo de vida que hacen falta, las mismas que
//   el hermano `NuevoExpedienteServiceImpl`.
//   MUST NOT sobrescribirse `allowPropertiesRemove`: el permiso de `auth-ventanilla.xml` ya niega
//   `remove`, y el borrado no tiene ningún campo que filtrar.
```

#### Action Rules

```java
private void fireActionRule_AsignarArranqueDelAsistente(AsistenteNuevoExpediente asistente);
//   Implementa R-AsistenteNuevoExpediente-003 (Origen spec: —; campos `centrosDisponibles`,
//   `hayQueElegirCentro` y `centro` clasificados `servidor` en esta acción).
//   Asignación INCONDICIONAL de los tres (sin guarda `if (campo == null)`: permitiría que un
//   atacante por el endpoint REST genérico dictara qué centros se le ofrecen — k-secure-coding §3.3):
//     - centrosDisponibles = getCentrosCandidatos()
//     - hayQueElegirCentro = centrosDisponibles.size() > 1
//     - centro = el único de `centrosDisponibles` si hay exactamente uno, y null en cualquier otro
//       caso. La whitelist de su único llamante (prepararCentros) es deny-all, así que el cliente no
//       puede dictarlo.
//   `hayQueElegirCentro` se calcula aquí y de ahí en adelante lo transporta la vista (ver Paso 1).

private void fireActionRule_AsignarTramitesDisponibles(AsistenteNuevoExpediente asistente);
//   Implementa R-AsistenteNuevoExpediente-004 (Origen spec: —; campo `tramitesDisponibles`
//   clasificado `servidor`). Hace la LECTURA del catálogo (la única de esta acción) y se la pasa al
//   cálculo. Asignación INCONDICIONAL:
//     - tramitesDisponibles = getTramitesCandidatos(centro, ventanillaRepository.findTramitesEvaluables())

private void fireActionRule_AsignarPresentacion(AsistenteNuevoExpediente asistente);
//   Implementa R-AsistenteNuevoExpediente-005 (Origen spec: —; campos `hayQuePreguntarPresentacion`,
//   `presentadoEnPapel`, `hayQuePreguntarParaQuien` y `presentadoEnRepresentacion`).
//   Calcula UNA sola vez la matriz forma x destinatario (cuatro llamadas a admiteAlta) y delega las
//   dos asignaciones en fireActionRule_AsignarFormaDePresentar y fireActionRule_AsignarDestinatario,
//   que son sus dos mitades y NO dos reglas: `recalcular` sigue invocando UNA sola, esta.
//   ÚNICO dueño del detalle —secuencia, semántica de cada rama, errores y los MUST NOT de la
//   regla—: design/rules/R-AsistenteNuevoExpediente-005.md. MUST leerse antes de implementarla.

private Boolean fireActionRule_AsignarFormaDePresentar(AsistenteNuevoExpediente asistente,
                                                       MatrizPresentacion matriz);
//   La mitad de R-AsistenteNuevoExpediente-005 que asigna los dos campos de la primera pregunta
//   (`hayQuePreguntarPresentacion` y `presentadoEnPapel`) y DEVUELVE el valor con el que deja
//   `presentadoEnPapel` (null incluido), que es lo que necesita la segunda. Lo invoca solo
//   fireActionRule_AsignarPresentacion.

private void fireActionRule_AsignarDestinatario(AsistenteNuevoExpediente asistente,
                                                MatrizPresentacion matriz,
                                                Boolean presentadoEnPapel);
//   La mitad de R-AsistenteNuevoExpediente-005 que asigna los dos campos de la segunda pregunta
//   (`hayQuePreguntarParaQuien` y `presentadoEnRepresentacion`) sobre la FILA de la matriz que
//   corresponde a la forma ya fijada, que recibe como parámetro. Lo invoca solo
//   fireActionRule_AsignarPresentacion.
```

#### Otras funciones

```java
// El tipo propio de R-AsistenteNuevoExpediente-005 (las dos mitades que lo consumen son
// `fireActionRule_*` y están en «Action Rules»). Su semántica, sus ramas y sus MUST NOT viven
// en design/rules/R-AsistenteNuevoExpediente-005.md, ÚNICO dueño del detalle de esa regla.

private record MatrizPresentacion(boolean yoMismoParaMi, boolean yoMismoEnRepresentacion,
                                  boolean enPapelParaMi, boolean enPapelEnRepresentacion);
//   Las cuatro celdas ya calculadas, con dos métodos de lectura sin más lógica:
//   admite(forma, destinatario) y admiteAlgunDestinatario(forma).

private List<Tramite> getTramitesCandidatos(Centro centro, List<Tramite> tramitesEvaluables);
//   EL predicado «el usuario puede iniciar este trámite en ese centro», escrito UNA sola vez: de
//   los `tramitesEvaluables` que recibe se queda con aquellos para los que
//   getPerfilesDeInicioSobreTramite(tramite, SecurityUtil.getUser(), centro) no está vacío.
//   No lee el catálogo: lo recibe ya leído para que la lectura ocurra una sola vez y no una por
//   centro (ver getCentrosCandidatos). Sí consulta PerfilesUsuarioService y SecurityUtil.getUser(),
//   así que en los tests hay que mockear los dos.
//   La lectura (ventanillaRepository.findTramitesEvaluables()) la hacen sus dos
//   llamantes ANTES de llamar: fireActionRule_AsignarTramitesDisponibles (una lectura) y
//   getCentrosCandidatos (una lectura antes del bucle, no una por centro). El repositorio les
//   entrega solo filas que ese servicio sabe juzgar, así que este método no tiene que defenderse de
//   ninguna (ver Paso 4).
//   NO se filtra por el tipo de usuario del TipoTramite: dejaría fuera al Administrativo y al
//   Familiar, que inician trámites dirigidos a otro tipo de usuario.
//   No ordena: el orden de la lista lo fija el `orderBy` del grid que la pinta.
//   Lo usan la regla de los trámites del paso 2 y getCentrosCandidatos, así que el paso 1 y el
//   paso 2 nunca pueden discrepar sobre qué se puede iniciar.

private List<Centro> getCentrosCandidatos();
//   Los centros del usuario autenticado (SecurityUtil.getUser().getCentroUsuarios(), nunca
//   User.centroActivo) en los que puede iniciar al menos un trámite. Lee el catálogo UNA sola vez
//   (List<Tramite> evaluables = ventanillaRepository.findTramitesEvaluables()) ANTES del bucle y se
//   lo pasa a cada llamada: !getTramitesCandidatos(centro, evaluables).isEmpty(). MUST NOT leerlo
//   dentro del bucle (sería una consulta por centro para obtener siempre la misma lista).
//   Lista vacía si el usuario no tiene centros.
//   No ordena: el orden lo fija el `orderBy` del grid que la pinta, igual que en
//   getTramitesCandidatos. El otro uso de la lista (la propia regla, que coge el único candidato
//   cuando size() == 1) no depende del orden.

private Profile perfilDeInicioPara(boolean presentadoEnPapel);
//   El perfil de inicio que EXIGE esa forma de presentar, preguntando a los predicados del propio
//   enum y sin nombrar ninguna de sus constantes: de Profile.values(), los que cumplen
//   puedeCrearExpediente() && permitePresentacionEnPapel() == presentadoEnPapel.
//   Reducción a EXACTAMENTE uno: si hay cero o más de uno, IllegalStateException (el enum ya no
//   modela una correspondencia 1:1 con la forma de presentar; es un error de programación, no una
//   entrada del usuario). MUST NOT devolver el primero ni null: null reventaría después en el
//   Objects.requireNonNull(profile) del constructor compacto de ContextoTramitacion, justo en la
//   puerta que tiene que contestar un mensaje.
//   Es el ÚNICO sitio donde vive esa correspondencia, y su único llamante es validateAlta (para
//   construir el ContextoTramitacion).

private boolean admiteAlta(Tramite tramite, Centro centro,
                           boolean presentadoEnPapel, boolean presentadoEnRepresentacion);
//   validateAlta(tramite, centro, presentadoEnPapel, presentadoEnRepresentacion).isEmpty(). Es el
//   único sitio donde se pregunta «¿podría crearse así?»: lo usa la regla del paso 3, que con sus
//   cuatro celdas decide a la vez qué formas de presentar se ofrecen y si hay que preguntar para
//   quién es.
//   No recibe Profile: lo deduce validateAlta, así que preguntar por una forma que el usuario no
//   tiene devuelve false, nunca una excepción.

private boolean esDestinatarioDelTramite(Tramite tramite, Centro centro);
//   Si el usuario tiene, en ese centro, el tipo de usuario al que va dirigido el tipo de trámite
//   del trámite: tieneTipoUsuario(centro, tramite.getTipoTramite().getTipoUsuario().getCodigo())
//   (el m2o ya viene cargado desde el Tramite). Da por hecho que el tipo de trámite declara tipo de
//   usuario: esa rama la resuelve su único llamante antes de preguntar (validateAfinadoDestinatario).

private boolean esFamiliar(Centro centro);
//   tieneTipoUsuario(centro, "FAMILIAR").

private boolean tieneTipoUsuario(Centro centro, String codigoTipoUsuario);
//   Base de los dos anteriores: recorre los CentroUsuarioTipoUsuario del CentroUsuario del usuario
//   en ese centro (User.getCentroUsuario(centro)) y compara por CÓDIGO, que es lo que los dos
//   llamantes tienen a mano: así ninguno necesita resolver la entidad TipoUsuario y el servicio no
//   gana un cuarto colaborador.
//   Si el usuario NO pertenece al centro (getCentroUsuario devuelve null): IllegalStateException.
//   En el único punto desde el que se llega aquí el motor ya ha aceptado el contexto, y sin
//   pertenecer al centro no habría perfiles de inicio; devolver false en vez de fallar convertiría
//   un error de programación en un veredicto de negocio.
```

**Verificar al final:** compila; `grep -nE "if\s*\(.*==\s*null\s*\).*set[A-Z]" src/main/java/com/educaflow/system/ventanilla/service/impl/AsistenteNuevoExpedienteServiceImpl.java` no devuelve nada.

---

### Paso 4 — Repositorios

```java
// Clase: com.educaflow.system.ventanilla.db.repo.VentanillaRepository extends JpaRepository<Tramite>
//   Constructor protegido sin argumentos: super(Tramite.class).

public List<Tramite> findTramitesEvaluables();
//   Los trámites del catálogo que sus consumidores SABEN tratar: los que tienen tipo de trámite y
//   tipo de expediente por defecto. Los dos m2o son nullable en `Tramite.xml`, y
//   PerfilesUsuarioServiceImpl hace Objects.requireNonNull sobre los dos, así que una sola fila mal
//   dada del catálogo convertiría el asistente entero en un 500 ya en el onNew del paso 1, para
//   TODOS los usuarios; el tipo de trámite lo necesita además el `groupBy="tipoTramite.name"` del
//   grid del paso 2. El filtro es la consulta que justifica el
//   método: descarta las filas con cualquiera de los dos a null.
//   Es el ÚNICO dueño del predicado «el sistema sabe juzgar este trámite», y lo consultan las DOS
//   puertas por las que entra un `Tramite`: la lista que la ventanilla construye ella misma (lo
//   llaman fireActionRule_AsignarTramitesDisponibles y getCentrosCandidatos, que pasan el resultado
//   al cálculo de candidatos getTramitesCandidatos) y el `tramite` que llega del cliente (V-009, en
//   validateTramiteEvaluable, al que invocan validateRecalcular y validateTriggerInitialEvent). MUST NOT
//   reescribirse ese predicado en el servicio.
//   NO filtra por perfiles de inicio (eso es regla de negocio: getTramitesCandidatos) ni ordena
//   (el orden lo fija el `orderBy` del grid).
```

El sistema no tiene entidades persistentes propias, pero sí una consulta: la lectura de los trámites evaluables del catálogo. Va aquí y no inline en el servicio por las dos reglas de `k-sistemas`/`k-code-quality`: **las consultas JPA viven en el repositorio, nunca en la capa de servicio**, y **un sistema no llama al repositorio de otro subsistema**. Es exactamente lo que ya hace el sistema hermano `system/gestioncentro/db/repo/GestionCentroRepository`, un repositorio del propio sistema sobre una entidad de otro subsistema con un método nombrado; el servicio inyecta `VentanillaRepository` y no `subsystem.expedientes.db.repo.TramiteRepository`.

**Verificar al final:** `./gradlew compileJava` compila; `grep -nE "^\s*(public|protected)" src/main/java/com/educaflow/system/ventanilla/db/repo/VentanillaRepository.java` devuelve exactamente dos líneas —el constructor protegido sin argumentos y `findTramitesEvaluables()`—, es decir `findTramitesEvaluables()` es el único método del repositorio.

---

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

### Paso 6 — Módulos Guice

No se crea ninguno. Los tres colaboradores inyectados ya tienen binding: `TramitadorService` y `VentanillaRepository` son beans normales y `PerfilesUsuarioService` lo enlaza `SecurityModule`. `AsistenteNuevoExpedienteService` es un `ModelService`, y **MUST NOT** registrarse en Guice: lo descubre `ModelServiceFactory` por su ubicación (`service/impl/<Entidad>ServiceImpl`).

---

### Paso 7 — Jobs programados

No aplica: el sistema no tiene ninguna tarea recurrente.

---

### Paso 8 — Vistas

#### `views/ElegirCentro-AsistenteNuevoExpediente.xml` — paso 1

Dos bloques.

**Bloque maestro** `sysVentanilla.ElegirCentro@AsistenteNuevoExpediente`: un `<action-view>` (pestaña, sin `popup`) que abre el `<form>`; el `<form>` tiene tres paneles (los campos técnicos ocultos, el listado y los botones) y un `onNew` que llama a `prepararCentros` y decide el arranque; un `<action-validate>` con el `<info>` del aviso cuando no hay ningún centro candidato; y el `<action-method>` de `prepararCentros`.

El `action-group` del `onNew` es la **única** pieza que decide cómo arranca el asistente, y cada una de sus condiciones pregunta por **lo que significa** a quien lo decidió, sin recomponerlo desde otros dos valores. Los tres estados alcanzables del arranque y las ramas que los cubren:

| Estado (lo que devolvió el servidor) | Condición | Qué hace |
|---|---|---|
| 0 centros candidatos (`centrosDisponibles` vacía, `hayQueElegirCentro` `false`, `centro` `null`) | `centrosDisponibles.isEmpty()` | muestra el aviso «No puede crear expedientes en ninguno de sus centros» |
| 1 centro candidato (`centro` fijado, `hayQueElegirCentro` `false`) | `centro != null` | abre el paso 2 con ese centro |
| 0 o 1 centro candidato (las dos ramas anteriores terminan aquí) | `!hayQueElegirCentro` | cierra este paso |
| 2 o más centros candidatos (`hayQueElegirCentro` `true`, `centro` `null`) | `hayQueElegirCentro` (ninguna de las anteriores) | no hace nada: el paso se queda y muestra el listado |

`centrosDisponibles` es el **único dueño** de «no hay ningún centro candidato»: `prepararCentros` la asigna incondicionalmente en esa misma respuesta y de ella deriva el servidor los otros dos valores, así que la vista la lee tal cual en vez de rehacer la conjunción `!hayQueElegirCentro && centro == null`, que quedaría desincronizada en silencio si `fireActionRule_AsignarArranqueDelAsistente` se afinara.

ASCII Layout de los paneles no triviales:

```
estadoPanel
············   ← centro(6, oculto) + hayQueElegirCentro(6, oculto)
                 Los dos únicos items del panel son ocultos, así que la fila queda vacía entera y
                 no desplaza nada de los paneles siguientes

centrosPanel (visible solo si hayQueElegirCentro)
pppppppppppp   ← panel-related centrosDisponibles(12)

buttons-panel (visible solo si hayQueElegirCentro)
..........cc   ← colOffset(10) + btnCancelar(2)     [único botón, principal, al borde derecho]
                 El `showIf` va en el propio `buttons-panel`: con un solo botón no hay estados
                 que particionar, así que no hace falta ningún panel de estado anidado y el
                 panel entero colapsa cuando no hay que elegir centro
```

**Bloque de detalle** `sysVentanilla.ElegirCentro@AsistenteNuevoExpediente.Centro`: el `<grid>` de `Centro` con una sola columna (el nombre), ordenado por nombre, y su `action-group` de clic de fila, que abre el paso 2 y cierra este. El `<grid>` no tiene `canNew`/`canEdit`/`canDelete`: del listado solo se elige.

#### `views/ElegirTramite-AsistenteNuevoExpediente.xml` — paso 2

Dos bloques.

**Bloque maestro** `sysVentanilla.ElegirTramite@AsistenteNuevoExpediente`: `<action-view>` (pestaña) con los dos valores que trae del paso 1, `_centroId` (`eval: centro?.id ?: id`) y `_hayQueElegirCentro` (`eval: hayQueElegirCentro == null ? __parent__?.hayQueElegirCentro : hayQueElegirCentro` — el registro actual cuando el salto sale de un formulario, y el del padre cuando sale de una fila, que es un `Centro` y no lo lleva); `<form>` con el centro en solo lectura encima del listado, el listado y el botón; `onNew` que fija los dos en el registro con `…-set-centroYHayQueElegirCentro-recibidos-action` y llama a `prepararTramites`; el `<action-method>` de `prepararTramites`.

El servidor **no** recalcula `hayQueElegirCentro` en este paso: lo calcula el paso 1 y lo transporta la vista (ver Paso 1).

ASCII Layout:

```
centroPanel
cccccc······   ← centro(6) + hayQueElegirCentro(6, oculto)
                 El oculto va DETRÁS del visible: un campo oculto consume igualmente sus columnas
                 (computeLayout asigna gridColumnStart/End a todos los items), así que delante
                 empujaría el centro a la mitad derecha de la fila. Detrás, el hueco queda al borde
                 derecho y el centro se queda en las columnas 1-6: un nombre de centro no necesita
                 las 12 (k-vistas/forms.md, «los campos no tienen que rellenar las 12 columnas»)

tramitesPanel
pppppppppppp   ← panel-related tramitesDisponibles(12)

buttons-panel → buttonsConEleccionDeCentro (hayQueElegirCentro)
..........aa   ← colOffset(10) + btnAtras(2)

buttons-panel → buttonsSinEleccionDeCentro (!hayQueElegirCentro)
..........cc   ← colOffset(10) + btnCancelar(2)
```

Los dos botones van en paneles anidados excluyentes porque llevan `colOffset`: dos gemelos con `colOffset` en un panel plano no renderizan bien (`k-vistas/forms.md`, «botones condicionales por estado»). Las dos condiciones son complementarias, así que siempre se ve **un** botón y solo uno.

**Bloque de detalle** `sysVentanilla.ElegirTramite@AsistenteNuevoExpediente.Tramite`: el `<grid>` de `Tramite` con una sola columna **visible** (el nombre del trámite; el tipo de trámite se pinta solo como cabecera de grupo, que es el nivel que pide el spec, como hacen los dos grids con `groupBy` del repo), `groupBy="tipoTramite.name"` y `orderBy="tipoTramite.name,name"` — el primer nivel es el grupo (el nombre del tipo de trámite) y el segundo las filas (los trámites), y Axelor pinta los grupos ya abiertos. La clave de agrupar es la **misma ruta punteada** con la que empieza la de ordenar, que es como agrupan los dos precedentes del repo (`system/expedientes/views/Abierto-Expediente.xml` y `Cerrado-Expediente.xml`): `useGridSortBy` (`axelor-front/src/views/grid/builder/utils.ts`) compone el `sortBy` con las columnas de `groupBy` más las de `orderBy` que no estén ya en `groupBy`, así que agrupar por el m2o `tipoTramite` dejaría tres claves y la primera ordenaría por un objeto. Su `action-group` de clic de fila abre el paso 3 y cierra este.

**La columna por la que se agrupa va declarada, oculta:** `<field name="tipoTramite.name" title="Tipo de trámite" hidden="true"/>`. No es decorativo ni redundante con el `groupBy`: la **proyección** que el listado pide al servidor la compone `useGridColumnNames` (`axelor-front/src/views/grid/builder/scope.tsx`) recorriendo `view.items` y las columnas de `orderBy`, y de estas últimas **solo conserva las que tienen metadatos de campo** (`const field = fields?.[item.name!]; … return names;`), que una ruta punteada no declarada como `<field>` no tiene; el `groupBy` no entra en esa cuenta en ningún caso. Sin esta columna el `POST …/Tramite/search` viaja con `"fields":["name"]`, `tipoTramite.name` no llega en los registros y **la cabecera de grupo se pinta vacía** (se ve `": (1 elementos)"` y el `aria-label` queda en `"Collapse group undefined: "`), aunque los datos en base de datos sean correctos. `hidden="true"` apaga solo el pintado —`columnProps.visible = !hidden` en `axelor-front/src/views/grid/builder/grid.tsx`—, **no** quita el nombre de la proyección, así que el tipo de trámite sigue sin ser una columna y solo se ve como cabecera de grupo, que es lo que pide `RUI-nuevo-expediente-arbol-tramites-003`. El `title` es obligatorio por la i18n del proyecto (`k-i18n`): el script de traducción lo recoge del XML aunque la columna no se pinte.

#### `views/Main-AsistenteNuevoExpediente.xml` — paso 3

Un solo bloque, `sysVentanilla.Main@AsistenteNuevoExpediente`: `<action-view>` **de pestaña** —igual que los otros dos: **sin** `popup` ni `popup-save`, por lo que explica la regla 2 de «Cómo se encadenan los tres pasos» y **D1** de `decisiones.md`— con `_centroId`, `_hayQueElegirCentro` y `_tramiteId`; `<form>` con el panel «Trámite» (nombre, centro y ayuda, todos en solo lectura, y **al final** los cuatro campos ocultos: `tramite`, los dos `hayQuePreguntar*` y `hayQueElegirCentro`), el panel «Presentación» (las dos preguntas) y la botonera; los `action-group` de los dos botones, del `onNew` y del `onChange` de «¿Cómo se presenta?»; el `<action-validate>` local de las dos preguntas obligatorias; el `<action-record>` que fija trámite, centro y `hayQueElegirCentro` desde el contexto; y los tres `<action-method>` (`recalcular`, `validateTriggerInitialEvent` y `triggerInitialEvent`).

El último, `sysVentanilla.Main@AsistenteNuevoExpediente-Remote-triggerInitialEvent-action`, es la acción de alta del `btnCrear`: la declara la sección `<?sv-remotes?>` de **este** fichero y llama, **sin `<context>` propio**, a `com.educaflow.subsystem.tramitador.controller.TramitadorController#triggerInitialEvent`. El `actions-tramitador.xml` del tramitador **MUST NOT** tocarse. Motivo de la desviación respecto a `design-guidelines.md` y su equivalencia funcional: **D7** de `decisiones.md`.

`hayQueElegirCentro` está aquí solo para devolverlo: el `btnAtras` reabre el paso 2 con el mismo `<context>`, así que el botón de salida de aquel recupera el rótulo que le corresponde (lo calcula el paso 1 y lo transporta la vista, ver Paso 1).

ASCII Layout:

```
tramitePanel — trámite CON ayuda
nnnnnncccccc   ← nombreTramite(6) + centro(6)          [los dos son nombres cortos y van juntos]
aaaaaaaaaaaa   ← ayudaTramite(12)                      [texto largo con HTML → fila propia]
············   ← tramite(6) + hayQuePreguntarPresentacion(6)   [ocultos]
············   ← hayQuePreguntarParaQuien(6) + hayQueElegirCentro(6)   [ocultos]

tramitePanel — trámite SIN ayuda
nnnnnncccccc   ← nombreTramite(6) + centro(6)
············   ← la fila de ayudaTramite, vacía: ocupa las 12 columnas él solo, así que su showIf
                 va en el propio campo y al ocultarse no desplaza nada
············   ← tramite(6) + hayQuePreguntarPresentacion(6)
············   ← hayQuePreguntarParaQuien(6) + hayQueElegirCentro(6)

presentacionPanel — se preguntan las dos
pppppppppppp   ← presentadoEnPapel(12)                 [boolean-radio con dos opciones largas]
rrrrrrrrrrrr   ← presentadoEnRepresentacion(12)        [boolean-radio con dos opciones largas]

presentacionPanel — solo «¿Cómo se presenta?»
pppppppppppp

presentacionPanel — solo «¿Para quién es el expediente?»
rrrrrrrrrrrr

presentacionPanel — ninguna de las dos: el panel entero no se ve

buttons-panel
.......aaccc   ← colOffset(7) + btnAtras(2) + btnCrear(3)
```

Cada pregunta ocupa su fila entera y es el único elemento de esa fila, así que su `showIf` va en el propio campo y no hace falta ningún panel anidado.

Los cuatro campos ocultos de `tramitePanel` van **al final**, detrás de `ayudaTramite`, por la misma razón que el oculto de `centroPanel` en el paso 2: un campo oculto **consume sus columnas igual** (`computeLayout` asigna `gridColumnStart/End` a todos los items y `GridItem` envuelve el widget en un div con ese `gridColumn` aunque no pinte nada), así que delante de los visibles partiría la fila «Trámite + Centro» en dos mitades desalineadas. Detrás, sus filas quedan vacías al final del panel y no desplazan nada.

**Ninguno de los tres formularios guarda nada:** el modelo no se persiste nunca (`persistable="false"`) y el spec lo declara explícitamente para el paso 3 («este formulario no lleva “Guardar”, “Cancelar” ni “Borrar” (la ficha no se guarda nunca)», `screen-nuevo-expediente.md`), así que en los tres no hay `btnSave`, ni `btnDelete`, ni `canBackOnSave`, ni `remote-validationSave-action`, ni `save` → `force-back`. Los cinco botones que sí existen (`btnCancelar` ×2, `btnAtras` ×2 y `btnCrear`) navegan o cierran con `close`, que es lo único que funciona en estas vistas: `back` solo conmuta entre el `grid` y el `form` de una misma pestaña, y el `<action-view>` de cada uno de los tres ficheros declara **solo** `<view type="form">`, sin ninguna `<view type="grid">`, así que no hay `grid` al que volver. Los `btnCancelar` son los de los pasos 1 y 2 —la salida del asistente, que el spec sí pide ahí— y no la «Cancelar» de la botonera de mantenimiento, que no existe en ninguno de los tres.

El botón de salida de los pasos 1 y 2 se llama **`btnCancelar`** —rótulo «Cancelar», el que comprueban los `ESC-`/`T-`— y su `action-group`, `…-btnCancelar-action` como exige `VAR-7.1`, contiene **`close`**, igual que los otros cuatro botones: ninguno de los tres forms persiste (ninguno declara `btnSave`) y ningún `<action-view>` de los tres declara una `<view type="grid">`, así que no hay ni cambios que perder ni `grid` al que volver. Es la **rama del asistente** de `VAR-7.2`, que ya es norma vigente en `agent_docs/view-rules.md`, en `k-vistas/forms.md` y en el test generado; el choque que la produjo y las alternativas, en **D8** de `decisiones.md`.

**Verificar al final:** la aplicación arranca sin errores de carga de vistas y `grep -n "canBackOnSave\|remote-validation" src/main/java/com/educaflow/system/ventanilla/views/*.xml` no devuelve nada.

---

### Paso 9 — Menús

Modificación del fichero único `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`: se añade la porción de `design/menus.xml`, un menú raíz «Ventanilla» (`order="5"`, delante de «Expedientes») con un único hijo, «Nuevo expediente», que apunta a `sysVentanilla.ElegirCentro@AsistenteNuevoExpediente-action`. Los dos con `groups="admins,users"`, porque la ventanilla la ve cualquier usuario con sesión iniciada.

**Verificar al final:** `grep -n "ventanilla-menuitem\|ventanilla-nuevoExpediente-menuitem" src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` devuelve las dos líneas fusionadas (el raíz «Ventanilla» y su hijo «Nuevo expediente» con `action="sysVentanilla.ElegirCentro@AsistenteNuevoExpediente-action"`), y ninguna otra entrada del fichero se ha perdido (`grep -c "<menuitem" …/menus.xml` crece exactamente en 2 respecto al valor previo).

---

### Paso 10 — Seguridad

`src/main/java/com/educaflow/system/ventanilla/data-init/input/auth-ventanilla.xml` con un único permiso:

```xml
<?xml version="1.0"?>
<auth>
  <!-- AsistenteNuevoExpediente es persistable="false": no tiene filas que filtrar, así que el
       permiso no lleva condition. Quién puede iniciar qué y en qué centro lo decide el servidor
       (AsistenteNuevoExpedienteServiceImpl.validateTriggerInitialEvent y TramitadorService). -->
  <permission name="AsistenteNuevoExpediente.all"
              object="com.educaflow.system.ventanilla.db.AsistenteNuevoExpediente">
    <can create="true" read="true" write="true" remove="false" export="false"/>
  </permission>
</auth>
```

Y su manifiesto `src/main/java/com/educaflow/system/ventanilla/data-init/input-config.xml`, con `priority="10"` y un `<input file="auth-ventanilla.xml" root="auth">` que enlaza `name`, `object`, `condition`, `conditionParams` y los cinco `can/@*` sobre `com.axelor.auth.db.Permission` (`search="self.name = :name"`, `create="true" update="true"`), igual que el de `system/expedientes`.

Regla de acceso en lenguaje natural: **cualquier usuario con sesión iniciada** ve el menú y puede usar el asistente; el permiso solo le deja operar sobre la ficha de pantalla, que no se guarda. El alcance real —qué centros se le ofrecen, qué trámites, cómo puede presentar y para quién— lo decide el servidor en cada paso, y de qué centro se fía para ello es una **desviación declarada** de `k-secure-coding` §4: su dueño es **D9** de `decisiones.md`, que fija el alcance y la ampliación normativa pendiente.

Modificación de `src/main/resources/data-init/input/auth.xml`: añadir `<permission name="AsistenteNuevoExpediente.all"/>` dentro de los grupos `admins` y `users`. **MUST NOT** redefinir allí su `<can>`: ese fichero tiene `priority="-1"` y se carga el último, así que sobrescribiría al del sistema.

**Verificar al final:** `grep -n "AsistenteNuevoExpediente.all" src/main/java/com/educaflow/system/ventanilla/data-init/input/auth-ventanilla.xml src/main/resources/data-init/input/auth.xml` devuelve tres líneas: la definición del permiso en `auth-ventanilla.xml` y sus dos referencias en `auth.xml` (grupos `admins` y `users`), ninguna de ellas con un `<can>` propio; además `grep -n "auth-ventanilla.xml" src/main/java/com/educaflow/system/ventanilla/data-init/input-config.xml` devuelve el `<input>` del manifiesto.

---

### Paso 11 — Datos iniciales

No hay catálogos propios. La ventanilla se apoya en datos que ya cargan otros subsistemas: los tipos de trámite y los trámites (`subsystem/expedientes` y el data-init que el build genera por trámite) y los perfiles por defecto para iniciarlos (`subsystem/security`: `AceProfileGlobal` y `AceProfileTipoTramite`).

---

### Paso 12 — Verificación final

```bash
./run.sh
```

Compila, pasa los tests y arranca en el 8080. Comprobaciones a ojo tras arrancar: el menú «Ventanilla → Nuevo expediente» aparece para un usuario normal, y con `alumno1@mislata.es` el asistente salta directamente al listado de trámites. El resto lo cubren los tests E2E de `test-e2e-desc.md`.

---

## Frontera de confianza — AllowProperties por acción

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

## Trazabilidad Origen spec → V/R/U → ubicación

### Validaciones

| V | Origen spec | Ubicación | Mensaje |
|---|---|---|---|
| V-AsistenteNuevoExpediente-001 | VAL-AsistenteNuevoExpediente-001 | `AsistenteNuevoExpedienteServiceImpl.validateCentroIndicado` (privado) — **único dueño**; lo invocan `…validatePrepararTramites`, `…validateRecalcular` (puerta 1) y `…validateTriggerInitialEvent` (puerta 1) | `I18n.get("Debe indicar el centro")` |
| V-AsistenteNuevoExpediente-002 | VAL-AsistenteNuevoExpediente-003 | `…validateTriggerInitialEvent` (puerta 3) + copia de cortesía **con la misma condición** (`presentadoEnPapel == null`, sin ningún término más) en `views/Main-AsistenteNuevoExpediente.xml`, `sysVentanilla.Main@AsistenteNuevoExpediente-Local-validateTriggerInitialEvent-action` | `I18n.get("Debe indicar cómo se presenta el expediente")` |
| V-AsistenteNuevoExpediente-003 | VAL-AsistenteNuevoExpediente-004 | `…validateTriggerInitialEvent` (puerta 4) + la misma copia de cortesía, que reproduce la condición de la puerta 4 **con su precondición** —la puerta 3 ya ha pasado—: `presentadoEnPapel != null && presentadoEnRepresentacion == null`, para que el diálogo del cliente conteste lo mismo que el servidor y no un segundo mensaje sobre una pregunta que no está en pantalla | `I18n.get("Debe indicar para quién es el expediente")` |
| V-AsistenteNuevoExpediente-004 | VAL-AsistenteNuevoExpediente-005 | `…validateAlta` (puerta 5) → `TramitadorService.validateTriggerInitialEvent` | "No puede crear expedientes de este trámite en el centro indicado" |
| V-AsistenteNuevoExpediente-005 | VAL-AsistenteNuevoExpediente-006 | `…validateAlta` (puerta 5) → `TramitadorService.validateTriggerInitialEvent` | "No puede presentar el expediente de esa forma en el centro indicado" |
| V-AsistenteNuevoExpediente-006 | VAL-AsistenteNuevoExpediente-007 | `…validateAlta` (puerta 5) → `TramitadorService.validateTriggerInitialEvent` | "Este trámite no permite presentar la solicitud en representación de otra persona" |
| V-AsistenteNuevoExpediente-007 | VAL-AsistenteNuevoExpediente-008 | `…validateAfinadoDestinatario` | `I18n.get("No puede crear este expediente para usted mismo en el centro indicado")` |
| V-AsistenteNuevoExpediente-008 | VAL-AsistenteNuevoExpediente-009 | `…validateAfinadoDestinatario` | `I18n.get("No puede crear este expediente en representación de otra persona en el centro indicado")` |
| V-AsistenteNuevoExpediente-009 | — | `AsistenteNuevoExpedienteServiceImpl.validateTramiteEvaluable` (privado) — **único dueño**: el trámite está entre `VentanillaRepository.findTramitesEvaluables()` (condición que cubre también el trámite ausente); lo invocan `…validateRecalcular` (puerta 2) y `…validateTriggerInitialEvent` (puerta 2) | `I18n.get("Debe indicar el trámite")` |

### Reglas de negocio

| R | Origen spec | Ubicación | Momento | Detalle |
|---|---|---|---|---|
| R-AsistenteNuevoExpediente-001 | RN-AsistenteNuevoExpediente-001 | `views/Main-AsistenteNuevoExpediente.xml`, `sysVentanilla.Main@AsistenteNuevoExpediente-btnCrear-action` → `sysVentanilla.Main@AsistenteNuevoExpediente-Remote-triggerInitialEvent-action` (`TramitadorController.triggerInitialEvent` → `TramitadorService.triggerInitialEvent`) | antes de commit; crea el expediente, cierra el asistente y abre el expediente | — |
| R-AsistenteNuevoExpediente-003 | — | `…fireActionRule_AsignarArranqueDelAsistente` (desde `prepararCentros`, su único llamante) | Antes — asignación incondicional | — |
| R-AsistenteNuevoExpediente-004 | — | `…fireActionRule_AsignarTramitesDisponibles` (desde `prepararTramites`) | Antes — asignación incondicional | — |
| R-AsistenteNuevoExpediente-005 | — | `…fireActionRule_AsignarPresentacion` (desde `recalcular`), que calcula la matriz y delega las dos asignaciones en `…fireActionRule_AsignarFormaDePresentar` y `…fireActionRule_AsignarDestinatario` | Antes — asignación incondicional | Detalle: design/rules/R-AsistenteNuevoExpediente-005.md |

> «Antes» aquí significa **antes de devolver el bean a la vista**, no antes de `repository.save`: el modelo no se persiste nunca, así que en este sistema no hay ningún `save` al que anteponerse. Las tres reglas cumplen lo que esa clasificación garantiza — que ningún campo `servidor` se queda con lo que mandó el cliente.

> **No existen `R-AsistenteNuevoExpediente-002` ni `-006`** (los huecos son intencionados y la numeración de las demás no se toca). `R-002` era la regla que asignaba `nombreTramite` y `ayudaTramite`, y desapareció al materializar `CC-AsistenteNuevoExpediente-001` y `-002` como campos derivados de solo lectura: un `CC-` con `momento: lectura` no necesita ninguna `R-` que lo asigne, porque no se persiste. `R-006` era la regla del destinatario, absorbida por `R-005`: las dos preguntas del paso 3 no son separables (la segunda solo puede contestarse sobre el `presentadoEnPapel` que fija la primera) y salen de la misma matriz, así que son una sola regla. Las referencias a `R-006` de `test-e2e-desc.md` se han reapuntado a `R-005`.

### Campos calculados

| CC del spec | Momento / sobreescribible | Ubicación |
|---|---|---|
| CC-AsistenteNuevoExpediente-001 (nombre del trámite) | lectura / nunca | campo derivado `nombreTramite` (`transient="true"` con cuerpo) de `domains/AsistenteNuevoExpediente.xml`: el getter devuelve `I18n.get("value:" + tramite.getName())` —`value:` es la clave real con la que el build registra el nombre de un trámite, y este cuerpo es su único dueño en Java—, o null sin trámite. Fuera de todas las `AllowProperties`. |
| CC-AsistenteNuevoExpediente-002 (ayuda del trámite) | lectura / nunca | campo derivado `ayudaTramite` (`transient="true"` con cuerpo) de `domains/AsistenteNuevoExpediente.xml`: el getter devuelve `tramite.getHelp()`, o null sin trámite. Fuera de todas las `AllowProperties`. |

### Reglas de UI

| U | Origen spec | Ubicación |
|---|---|---|
| U-nuevo-expediente-001 | RUI-nuevo-expediente-listado-centros-001 | `ElegirCentro-AsistenteNuevoExpediente.xml`: `sysVentanilla.ElegirCentro@AsistenteNuevoExpediente-onNew-action` (las tres ramas) + `…-Local-avisoSinCentros-action` + `showIf="hayQueElegirCentro"` del panel `centrosPanel` |
| U-nuevo-expediente-002 | RUI-nuevo-expediente-arbol-tramites-001 | `ElegirTramite-…xml`: paneles `buttonsConEleccionDeCentro` / `buttonsSinEleccionDeCentro` con `showIf` excluyentes sobre `hayQueElegirCentro`, que llega del paso 1 en el `<context>` `_hayQueElegirCentro` y lo fija `…-set-centroYHayQueElegirCentro-recibidos-action` |
| U-nuevo-expediente-003 | RUI-nuevo-expediente-arbol-tramites-002 | `ElegirTramite-…xml`: `<field name="centro" readonly="true">` del panel `centroPanel`, encima del listado |
| U-nuevo-expediente-004 | RUI-nuevo-expediente-arbol-tramites-003 | `ElegirTramite-…xml`: `groupBy="tipoTramite.name"` + `orderBy="tipoTramite.name,name"` del grid `…@AsistenteNuevoExpediente.Tramite-grid`, más la columna `<field name="tipoTramite.name" hidden="true"/>` que mete esa ruta en la proyección para que la cabecera de grupo tenga texto (misma clave para agrupar y ordenar; Axelor pinta los grupos abiertos) |
| U-nuevo-expediente-005 | RUI-nuevo-expediente-formulario-001 | `Main-…xml`: `…-set-tramiteCentroYHayQueElegirCentro-recibidos-action` desde `onNew` (fija trámite, centro y `hayQueElegirCentro` desde el contexto), más `readonly="true"` en `nombreTramite`, `centro` y `ayudaTramite` |
| U-nuevo-expediente-006 | RUI-nuevo-expediente-formulario-002 | `Main-…xml`: `showIf="hayQuePreguntarPresentacion"` del campo `presentadoEnPapel`, con el booleano calculado por `…fireActionRule_AsignarPresentacion` → `fireActionRule_AsignarFormaDePresentar` |
| U-nuevo-expediente-007 | RUI-nuevo-expediente-formulario-003 | `…fireActionRule_AsignarPresentacion` → `fireActionRule_AsignarFormaDePresentar` deja `presentadoEnPapel` sin valor cuando hay que preguntar; el `<error>` de `…-Local-validateTriggerInitialEvent-action` y la puerta 3 de `validateTriggerInitialEvent` la hacen obligatoria |
| U-nuevo-expediente-008 | RUI-nuevo-expediente-formulario-004 | `…fireActionRule_AsignarPresentacion` → `fireActionRule_AsignarFormaDePresentar` sobrescribe `presentadoEnPapel` con la única forma posible cuando no se pregunta |
| U-nuevo-expediente-009 | RUI-nuevo-expediente-formulario-005 | `Main-…xml`: `showIf="hayQuePreguntarParaQuien"` del campo `presentadoEnRepresentacion`, con el booleano calculado por `…fireActionRule_AsignarPresentacion` → `fireActionRule_AsignarDestinatario` y recalculado en el `onChange` de `presentadoEnPapel` |
| U-nuevo-expediente-010 | RUI-nuevo-expediente-formulario-006 | `…fireActionRule_AsignarPresentacion` → `fireActionRule_AsignarDestinatario` deja `presentadoEnRepresentacion` en null cuando hay que preguntar; el `<error>` de `…-Local-validateTriggerInitialEvent-action` y la puerta 4 de `validateTriggerInitialEvent` la hacen obligatoria |
| U-nuevo-expediente-011 | RUI-nuevo-expediente-formulario-007 | `…fireActionRule_AsignarPresentacion` → `fireActionRule_AsignarDestinatario` fija `presentadoEnRepresentacion` con el único valor posible cuando no se pregunta (y con `false` si ninguno lo es, para que rechace la puerta 5 de `validateTriggerInitialEvent` con el literal correcto y no la puerta 4) |
| U-nuevo-expediente-012 | RUI-nuevo-expediente-formulario-008 | `…fireActionRule_AsignarPresentacion` → `fireActionRule_AsignarDestinatario`: con `presentadoEnPapel` sin contestar deja `hayQuePreguntarParaQuien` a false y el campo a null, así que la pregunta no se ve y no tiene valor |
| U-nuevo-expediente-013 | RUI-nuevo-expediente-formulario-009 | `Main-…xml`: `showIf="hayQuePreguntarPresentacion || hayQuePreguntarParaQuien"` del panel `presentacionPanel` |
| U-nuevo-expediente-014 | RUI-nuevo-expediente-formulario-010 | `Main-…xml`: `showIf="ayudaTramite"` del campo `ayudaTramite` |
| U-nuevo-expediente-015 | RUI-nuevo-expediente-formulario-011 | `AsistenteNuevoExpedienteController.validateTriggerInitialEvent` → `doResponseBusinessMessagesAsError("No es posible crear el expediente", …)`; los avisos de pregunta sin contestar salen del `<action-validate>` local, sin título. En los dos casos el error corta el `action-group` y el formulario sigue abierto |

---

## Cambios necesarios fuera del sistema

Los dos primeros son ficheros de datos del proyecto; el tercero es la única ampliación normativa pendiente. De las reglas de vistas no queda ninguna: tanto el `<grid action="…">` de los pasos 1 y 2 (D4) como la rama del asistente de `VAR-7.2` (D8) ya están recogidos en `agent_docs/view-rules.md` y en los skills `k-vistas`. **MUST NOT** aplicar ninguno de los tres el diseñador: los aplica `/sdd-implementer`.

1. **`src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`** — fusionar la porción de `design/menus.xml`.
2. **`src/main/resources/data-init/input/auth.xml`** — añadir `<permission name="AsistenteNuevoExpediente.all"/>` dentro de los grupos `admins` y `users` (solo la referencia, sin `<can>`).
3. **Ampliar a `system/ventanilla` la «Excepción — expedientes» de `.claude/skills/k-secure-coding/SKILL.md` §4 y el párrafo equivalente de `CLAUDE.md` § «La aplicación»** — los dos enumeran hoy solo `subsystem/expedientes`, `subsystem/tramitador` y `tramites` como los paquetes que **MUST NOT** usar `User.centroActivo`. Texto exacto de lo que hay que añadir y por qué: **D9** de `decisiones.md`. **MUST NOT** tocarse ninguna otra regla de esos dos ficheros.

---

## Tests

- **Tests unitarios** (JUnit + Mockito): descritos en `test-unit-desc.md` (lo materializa una fase posterior del pipeline).
- **Tests E2E**: descritos en `test-e2e-desc.md`, 26 tests (`T-001`…`T-026`) que cubren los 25 escenarios del spec. La carpeta destino es `src/test/e2e/system/ventanilla/`, que aún no existe, por eso la numeración arranca en `T-001`.

---

## Reglas del spec descartadas

Ninguna. Las ocho `VAL-`, la `RN-`, las dos `CC-` y las quince `RUI-` del spec están ubicadas en la matriz de trazabilidad.

---

## Notas y supuestos

- **Mientras el paso 1 decide, no muestra nada.** El listado de centros y el botón «Cancelar» solo se ven si `hayQueElegirCentro`, así que en los casos «ningún centro» y «un solo centro» el usuario **nunca** ve el listado de centros, como exigen ESC-001 y ESC-003: la ventana existe un instante, pero vacía, y se cierra sola.
- **Desviación declarada respecto al spec: los pasos 1 y 3 son pestañas, no ventanas emergentes.** `screen-nuevo-expediente.md` describe el listado de centros (paso 1) «en una ventana emergente titulada «Nuevo expediente: elija el centro»» y el formulario de contexto (paso 3) «en una ventana emergente titulada «Nuevo expediente»». El diseño los materializa como **pestañas**, igual que el paso 2, y **ningún** `<action-view>` de los tres lleva `popup` ni `popup-save`: con una emergente en la cadena el alta no funciona (la lista de pestañas llega a cero y el router reabre el asistente encima del expediente). El mecanismo, el código del framework que lo demuestra y las alternativas evaluadas están en **D1** de `decisiones.md`, su único dueño; la regla operativa, en la regla 2 de «Cómo se encadenan los tres pasos». Lo único que cambia es el contenedor: los títulos, los contenidos y el comportamiento que el spec pide para las dos pantallas se respetan tal cual.
- **Desviación declarada respecto a las guías: no se invoca `PerfilesUsuarioService.getPerfil(tramite, user, centro, presentadoEnPapel)`**, pese a que `design-guidelines.md` lo lista entre las piezas a reutilizar tal cual. El `Profile` del `ContextoTramitacion` lo deduce `perfilDeInicioPara(presentadoEnPapel)` de los predicados del enum `Profile`; `getPerfilesDeInicioSobreTramite` sí se usa tal cual. El motivo y la evidencia están en **D5** de `decisiones.md`, su único dueño.
- **Desviación declarada respecto a las guías: el `btnCrear` no invoca la acción global `subsysTramitador-trigger-initial-event-action`**, pese a que `design-guidelines.md` la lista entre las piezas a reutilizar tal cual. En su lugar declara la acción propia `sysVentanilla.Main@AsistenteNuevoExpediente-Remote-triggerInitialEvent-action`, que llama al mismo `TramitadorController.triggerInitialEvent` leyendo del formulario los mismos cuatro campos (`tramite`, `centro`, `presentadoEnPapel` y `presentadoEnRepresentacion`), así que la cadena es funcionalmente equivalente. El motivo —de ámbito (`VAR-4.1`), con la global en `subsystem/tramitador/controller/actions-tramitador.xml`, fichero que **MUST NOT** tocarse— está en **D7** de `decisiones.md`, su único dueño.
- **El modelo vive en `domains/`, como cualquier otro modelo de dominio del proyecto.** Ver **D6** de `decisiones.md`.
- **`hayQuePreguntarPresentacion`, `hayQuePreguntarParaQuien`, `centrosDisponibles`, `tramitesDisponibles` y `hayQueElegirCentro` no están en `entity-AsistenteNuevoExpediente.md`.** No son campos de negocio: son la forma de que el servidor —único que conoce los perfiles del usuario— conteste a las reglas de UI que el propio spec exige (`RUI-…-formulario-002` a `-009` y `RUI-…-arbol-tramites-001`). Sin ellos esas reglas no se podrían evaluar en la vista, o habría que replicar la clasificación en JavaScript (**D2** y **D3** de `decisiones.md`).
- **Las dos preguntas no llevan `requiredIf`.** Con `requiredIf`, la acción `validate` del framework cortaría antes con su mensaje genérico y el usuario nunca vería «Debe indicar cómo se presenta el expediente» / «Debe indicar para quién es el expediente», que el spec fija literalmente. La obligatoriedad se expresa con el `<action-validate>` local y, sobre todo, con las puertas 3 y 4 de `validateTriggerInitialEvent`.
