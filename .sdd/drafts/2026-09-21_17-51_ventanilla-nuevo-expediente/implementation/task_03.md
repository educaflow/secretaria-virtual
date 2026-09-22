---
type: implementation-task
template: system
---

# Tarea 03 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-validaciones
- k-secure-coding
- k-i18n
- k-code-quality

Implementa el servicio del asistente: la **interfaz** y su **implementación**. Van en la misma tarea porque son
un único componente lógico (la interfaz declara exactamente las acciones, los `validate*` y las `allowProperties*`
que la implementación materializa).

El dominio `AsistenteNuevoExpediente` y el repositorio `VentanillaRepository` ya están en el árbol (tareas 01 y 02):
son **contrato fijo** y **MUST NOT** editarse ni regenerarse.

El detalle de `R-AsistenteNuevoExpediente-005` (la regla de `recalcular`) vive en
`.sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/design/rules/R-AsistenteNuevoExpediente-005.md`,
su **único dueño**: **MUST** leerse antes de implementar `fireActionRule_AsignarPresentacion` y sus dos mitades.

## Filas de la tabla «Ficheros a crear o modificar» del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/system/ventanilla/service/AsistenteNuevoExpedienteService.java` | Crear | k-sistemas (servicios.md) | Interfaz del servicio del asistente |
| `src/main/java/com/educaflow/system/ventanilla/service/impl/AsistenteNuevoExpedienteServiceImpl.java` | Crear | k-sistemas (servicios.md), k-validaciones, k-secure-coding | Implementación: candidatos, opciones y validación de la creación |

> **Nota para `/sdd-implementer`:** los XML de `domains/`, `views/` y `menus.xml` ya están materializados en la carpeta `design/`. **MUST NOT** modificarlos, reescribirlos ni regenerarlos: se **copian verbatim** a su ubicación final (`menus.xml` se fusiona en el `menus.xml` único del proyecto). El código Java es lo único que se implementa a partir de las firmas y comentarios del diseño.
>
> **Los comentarios de este `design.md` NO se transcriben al código.** Son material de `.sdd/`: explican el diseño, no acompañan al código. **MUST NOT** aparecer en los `.java` los comentarios `//` de estos pasos, ni ningún identificador de la spec o del diseño (`V-`, `R-`, `U-`, `CC-`, `ESC-`, `HU-`, `VAL-`, `RN-`, `RUI-`), ni la justificación de una decisión «para que no se pierda» (`k-code-quality/comentarios.md`): lo que el código no revela por sí solo se dice con el nombre del método o de la variable, y lo demás vive aquí y en `decisiones.md`.

## Diseño (verbatim)

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

### Paso 6 — Módulos Guice

No se crea ninguno. Los tres colaboradores inyectados ya tienen binding: `TramitadorService` y `VentanillaRepository` son beans normales y `PerfilesUsuarioService` lo enlaza `SecurityModule`. `AsistenteNuevoExpedienteService` es un `ModelService`, y **MUST NOT** registrarse en Guice: lo descubre `ModelServiceFactory` por su ubicación (`service/impl/<Entidad>ServiceImpl`).

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
