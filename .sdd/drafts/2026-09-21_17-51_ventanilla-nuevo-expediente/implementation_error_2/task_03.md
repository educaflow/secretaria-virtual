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
- k-code-quality

Implementa el **servicio del asistente**: la **interfaz** y su **implementación**. Van en una sola tarea
porque son un único componente lógico (el contrato público y su realización). Depende del modelo de dominio
(tarea 01) y del repositorio (tarea 02), que ya están en el árbol.

El detalle completo de la regla `R-AsistenteNuevoExpediente-005` (la matriz forma × destinatario de
`fireActionRule_AsignarPresentacion`, con su secuencia, la semántica de cada rama, sus errores y sus
`MUST NOT`) está en
`.sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/design/rules/R-AsistenteNuevoExpediente-005.md`,
**único dueño** de ese detalle: **MUST** leerse antes de escribir ese método (no se copia aquí por extensión).


## Estado del árbol al empezar (decisión del descomponedor, no del diseño)

Una ejecución anterior de `/sdd-implementer` se detuvo por un error del diseño (ver
`.sdd/drafts/2026-09-21_17-51_ventanilla-nuevo-expediente/implementation_error_1/error_design.log`) y dejó
**ya materializados** en el árbol los ficheros de esta tarea, escritos contra una versión anterior del
diseño. La **fuente de verdad es el diseño de hoy**: si el fichero ya existe, **MUST** comprobarse que
coincide exactamente con lo que este texto prescribe y, si no coincide, dejarlo como el diseño manda
(para los XML materializados, copia literal desde `design/`). Que el destino ya exista **NO** es un
`CONFLICT`: la acción `Crear` de la tabla se refiere al estado previo a la iniciativa.

## Fila de la tabla «Ficheros a crear o modificar»

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/system/ventanilla/service/AsistenteNuevoExpedienteService.java` | Crear | k-sistemas (servicios.md) | Interfaz del servicio del asistente |
| `src/main/java/com/educaflow/system/ventanilla/service/impl/AsistenteNuevoExpedienteServiceImpl.java` | Crear | k-sistemas (servicios.md), k-validaciones, k-secure-coding | Implementación: candidatos, opciones y validación de la creación |

## Nota del diseño sobre los comentarios

> **Los comentarios de este `design.md` NO se transcriben al código.** Son material de `.sdd/`: explican el diseño, no acompañan al código. **MUST NOT** aparecer en los `.java` los comentarios `//` de estos pasos, ni ningún identificador de la spec o del diseño (`V-`, `R-`, `U-`, `CC-`, `ESC-`, `HU-`, `VAL-`, `RN-`, `RUI-`), ni la justificación de una decisión «para que no se pierda» (`k-code-quality/comentarios.md`): lo que el código no revela por sí solo se dice con el nombre del método o de la variable, y lo demás vive aquí y en `decisiones.md`.

## Paso 2 del diseño (verbatim)

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
Optional<BusinessMessages> validateCrear(AsistenteNuevoExpediente asistente);
AllowProperties allowPropertiesCrear();
```

- **MUST NOT** re-declarar `insert`/`update`/`remove` ni sus `validate*`/`allowProperties*`: el modelo no se persiste nunca y el `DefaultModelService` ya los trae.
- **Cada acción propia va con su `validate<Accion>`, sin excepciones** (`k-sistemas/servicios.md`, «Regla obligatoria»), también las de solo lectura, y **cada acción empieza** por `validate<Accion>(asistente).ifPresent(BusinessMessages::throwIfInvalid)`. Las precondiciones de `prepararTramites` y `recalcular` (centro ausente, trámite ausente o no evaluable) se rechazan así, con el **mismo literal de negocio** que usa `validateCrear`, y **nunca** con `IllegalStateException` ni `IllegalArgumentException`: una petición manipulada al `@CallMethod` debe contestar un mensaje, no un 500.

**Verificar al final:** `grep -nE "validate(PrepararCentros|PrepararTramites|Recalcular|Crear)\(" src/main/java/com/educaflow/system/ventanilla/service/AsistenteNuevoExpedienteService.java` devuelve cuatro líneas, una por acción propia.


## Paso 3 del diseño (verbatim)

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
//   NO recalcula `hayQueElegirCentro` ni toca el `centro`: los dos los decidió el paso 1.
//   `hayQueElegirCentro` viaja desde allí en el `<context>` de la vista del paso 2 y solo gobierna
//   el rótulo del botón («Atrás» o «Cancelar»), así que recalcularlo costaba recorrer TODOS los
//   centros del usuario por TODO el catálogo —(N_centros+1) x N_tramites clasificaciones de
//   perfiles, cinco repositorios AceProfile* cada una— en cada apertura del paso 2 y en cada
//   «Atrás» desde el paso 3, y de paso rellenaba un `centrosDisponibles` que este paso no pinta.

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
public Optional<BusinessMessages> validatePrepararCentros(AsistenteNuevoExpediente asistente);
//   Sin precondiciones: la acción no lee nada del cliente (whitelist deny-all) y todo sale del
//   usuario autenticado, así que devuelve Optional.empty(). Se declara igualmente porque el par
//   acción + validador es el contrato público mínimo de toda acción propia, también las de solo
//   lectura (k-sistemas/servicios.md), y es el sitio al que añadir una precondición futura.

public Optional<BusinessMessages> validatePrepararTramites(AsistenteNuevoExpediente asistente);
//   Una sola puerta: validateCentroIndicado(asistente), el dueño privado de
//   V-AsistenteNuevoExpediente-001 (el centro tiene que venir indicado; es lo que el usuario eligió
//   en el paso 1). Esta acción no tiene ninguna precondición más, y MUST NOT ejecutar el validador
//   de ninguna otra acción.

public Optional<BusinessMessages> validateRecalcular(AsistenteNuevoExpediente asistente);
//   Dos puertas, en este orden, y se para en la primera que falla: los mensajes se devuelven TAL
//   CUAL y no se evalúa la siguiente.
//     1. validateCentroIndicado(asistente) — V-AsistenteNuevoExpediente-001 ("Debe indicar el
//        centro").
//     2. validateTramiteEvaluable(asistente) — V-AsistenteNuevoExpediente-009 ("Debe indicar el
//        trámite"): el otro dato que los pasos anteriores ya fijaron y sin el cual no hay nada que
//        recalcular.
//   MUST NOT reescribirse aquí ninguna de las dos condiciones ni sus literales —cada una tiene un
//   único dueño, el validador privado que la implementa—, ni ejecutar el validador de otra acción.

public Optional<BusinessMessages> validateCrear(AsistenteNuevoExpediente asistente);
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
//        trámite ausente —que allowPropertiesCrear deja pasar, porque una whitelist no exige campos,
//        y que revienta en el Objects.requireNonNull(tramite) del constructor compacto de
//        ContextoTramitacion— y el trámite que el sistema no sabe juzgar —el id de cualquier fila
//        del catálogo sin tipo de trámite o sin tipo de expediente por defecto, que revienta dentro
//        de PerfilesUsuarioServiceImpl—.
//
//   Las puertas 1 y 2 MUST NOT reescribirse aquí: cada regla tiene UN solo dueño, el validador
//   privado que la implementa, y así lo que el paso 3 acepta no puede separarse de lo que «Crear
//   expediente» rechaza. Lo que validateCrear NO hace es ejecutar el validador de otra acción:
//   invoca a los dueños, no a validateRecalcular. El `return` temprano de cada puerta conserva el
//   «un solo mensaje» de ESC-025.
//
//     3. V-AsistenteNuevoExpediente-002 (Origen spec: VAL-AsistenteNuevoExpediente-003)
//        `presentadoEnPapel` viene informado. Mensaje: "Debe indicar cómo se presenta el expediente".
//        Se exige SIEMPRE, se pregunte o no: cuando no se pregunta, `recalcular` ya dejó el campo
//        con la única forma posible, y el controlador del tramitador lee ese mismo campo del
//        formulario. Si el servidor dedujera aquí un valor distinto del que va a enviarse, la
//        validación y el alta dirían cosas distintas. Además el valor de este campo es el que elige
//        el perfil con el que se construye el contexto, así que hay que tenerlo antes de la puerta 5.
//
//     4. V-AsistenteNuevoExpediente-003 (Origen spec: VAL-AsistenteNuevoExpediente-004)
//        `presentadoEnRepresentacion` viene informado. Mensaje: "Debe indicar para quién es el
//        expediente". Mismo razonamiento que la puerta 3.
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
//   usuario eligió en el paso 1). Mensaje: "Debe indicar el centro".
//   Lo invocan las tres acciones que necesitan el centro ya elegido —validatePrepararTramites,
//   validateRecalcular (puerta 1) y validateCrear (puerta 1)—, así que afinar la condición o el
//   literal se hace aquí y en ningún sitio más, y ninguna de las tres tiene que ejecutar el
//   validador de las otras para heredarla.

private Optional<BusinessMessages> validateTramiteEvaluable(AsistenteNuevoExpediente asistente);
//   ÚNICO dueño de V-AsistenteNuevoExpediente-009: "Debe indicar el trámite". Lo invocan
//   validateRecalcular (puerta 2) y validateCrear (puerta 2).
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
//   pieza. Y deja a validateCrear con UNA sola semántica: todas sus puertas cortan en el primer
//   fallo y devuelven un solo mensaje (ESC-014/015/023/024/025).
//   Lo usan tanto validateCrear (para rechazar) como admiteAlta (para saber qué ofrecer), así que
//   lo que la pantalla enseña y lo que el servidor acepta no pueden separarse nunca.
//   Puede dar por hecho que el trámite es EVALUABLE: los dos caminos que llegan hasta aquí pasan
//   antes por V-009, es decir por validateTramiteEvaluable —que validateRecalcular ejecuta antes de
//   la regla que llama a admiteAlta, y validateCrear en su puerta 2—, así que nunca recibe una fila
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
//       dirigido el trámite, o no ser Familiar. Mensaje: "No puede crear este expediente para usted
//       mismo en el centro indicado".
//     - V-AsistenteNuevoExpediente-008 (Origen spec: VAL-AsistenteNuevoExpediente-009): si el
//       expediente es en representación de otra persona y lo presenta el propio usuario, tiene que
//       ser Familiar en ese centro, o no ser del tipo de usuario al que va dirigido el trámite.
//       Mensaje: "No puede crear este expediente en representación de otra persona en el centro
//       indicado".
//   Las dos condiciones son excluyentes (cada una mira un valor distinto de
//   presentadoEnRepresentacion), así que nunca salen los dos mensajes a la vez.
//   Este afinado es propio de la ventanilla: el motor da por bueno «para mí» siempre y «en
//   representación» siempre que el trámite lo admita.

// MUST NOT sobrescribirse `validateInsert`/`validateUpdate`/`validateRemove`: serían código
// inalcanzable. `persistable="false"` genera la clase como `@MappedSuperclass` (Entity.java del
// generador: `setPersistable(false)` → `mappedSuperClass`, y entonces ni `@Entity` ni `@Table`), así
// que no hay tabla ni entidad JPA; por el endpoint REST genérico la petición muere antes en
// `JPA.edit`/`JPA.manage`, y `Resource.save` solo llama a `modelService.insert/update` DESPUÉS de
// esas dos. Ninguna de las tres vistas declara `save` ni `delete`, así que tampoco hay camino desde
// el cliente. El único hook que sí se ejecuta es `modelService.validate`, donde actúa el deny-all de
// `allowPropertiesInsert/Update`. Si algún día hiciera falta rechazar además la operación, el sitio
// es el `auth-ventanilla.xml` del Paso 10, que es lo que mira `security.check` antes de `JPA.edit`,
// no un `validate*` que no se ejecuta. Misma clasificación que el hermano
// `NuevoExpedienteServiceImpl`, también `persistable="false"`.
```

> Los cuatro helpers privados que devuelven `Optional<BusinessMessages>` van en este bloque y **no** en «Otras funciones» (`k-sistemas/servicios.md`, «Orden de los métodos en la `*Impl`»): quien abra la clase buscando dónde se valida algo tiene aquí **todas** las reglas V del sistema.

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

public AllowProperties allowPropertiesCrear();
//   createAllowProperties(Map.of("tramite", Map.of(), "centro", Map.of(),
//                                "presentadoEnPapel", Map.of(), "presentadoEnRepresentacion", Map.of())):
//   exactamente los cuatro campos de la línea `Input AllowProperties` de la acción «Crear expediente».

public AllowProperties allowPropertiesInsert();
public AllowProperties allowPropertiesUpdate();
//   createDenyAllProperties() en las dos: la ficha no se guarda nunca, así que ni un alta ni una
//   modificación pueden aceptar un solo campo del cliente. Son las dos ÚNICAS sobrescrituras del
//   ciclo de vida que hacen falta —las mismas que el hermano `NuevoExpedienteServiceImpl`—, porque
//   `modelService.validate` es el único hook del guardado que llega a ejecutarse sobre un modelo
//   `@MappedSuperclass` (ver «Métodos de validación»).
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
//   `hayQueElegirCentro` se calcula aquí una sola vez, en el paso 1, y de ahí en adelante viaja en
//   el `<context>` de los pasos 2 y 3.

private void fireActionRule_AsignarTramitesDisponibles(AsistenteNuevoExpediente asistente);
//   Implementa R-AsistenteNuevoExpediente-004 (Origen spec: —; campo `tramitesDisponibles`
//   clasificado `servidor`). Hace la LECTURA del catálogo (la única de esta acción) y se la pasa al
//   cálculo. Asignación INCONDICIONAL:
//     - tramitesDisponibles = getTramitesCandidatos(centro, ventanillaRepository.findTramitesEvaluables())

private void fireActionRule_AsignarPresentacion(AsistenteNuevoExpediente asistente);
//   Implementa R-AsistenteNuevoExpediente-005 (Origen spec: —; campos `hayQuePreguntarPresentacion`,
//   `presentadoEnPapel`, `hayQuePreguntarParaQuien` y `presentadoEnRepresentacion`).
//   Calcula UNA sola vez la matriz forma x destinatario (cuatro llamadas a admiteAlta) y delega las
//   dos asignaciones en asignarFormaDePresentar y asignarDestinatario.
//   ÚNICO dueño del detalle —secuencia, semántica de cada rama, errores y los MUST NOT de la
//   regla—: design/rules/R-AsistenteNuevoExpediente-005.md. MUST leerse antes de implementarla.
```

#### Otras funciones

```java
// Las tres piezas de R-AsistenteNuevoExpediente-005. Su semántica, sus ramas y sus MUST NOT viven
// en design/rules/R-AsistenteNuevoExpediente-005.md, ÚNICO dueño del detalle de esa regla.

private record MatrizPresentacion(boolean yoMismoParaMi, boolean yoMismoEnRepresentacion,
                                  boolean enPapelParaMi, boolean enPapelEnRepresentacion);
//   Las cuatro celdas ya calculadas, con dos métodos de lectura sin más lógica:
//   admite(forma, destinatario) y admiteAlgunDestinatario(forma).

private Boolean asignarFormaDePresentar(AsistenteNuevoExpediente asistente, MatrizPresentacion matriz);
//   Asigna los dos campos de la primera pregunta y DEVUELVE el valor con el que deja
//   `presentadoEnPapel` (null incluido), que es lo que necesita la segunda.

private void asignarDestinatario(AsistenteNuevoExpediente asistente, MatrizPresentacion matriz,
                                 Boolean presentadoEnPapel);
//   Asigna los dos campos de la segunda pregunta sobre la FILA de la matriz que corresponde a la
//   forma ya fijada, que recibe como parámetro.

private List<Tramite> getTramitesCandidatos(Centro centro, List<Tramite> tramitesEvaluables);
//   EL predicado «el usuario puede iniciar este trámite en ese centro», escrito UNA sola vez: de
//   los `tramitesEvaluables` que recibe se queda con aquellos para los que
//   getPerfilesDeInicioSobreTramite(tramite, SecurityUtil.getUser(), centro) no está vacío.
//   Cálculo PURO: NO lee el catálogo (k-code-quality/metodos.md §«Cálculo puro y efectos
//   secundarios»). La lectura (ventanillaRepository.findTramitesEvaluables()) la hacen sus dos
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


## Frontera de confianza — AllowProperties por acción (verbatim)

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


## Trazabilidad Origen spec → V/R/U → ubicación (verbatim, validaciones, reglas de negocio y campos calculados)

## Trazabilidad Origen spec → V/R/U → ubicación

### Validaciones

| V | Origen spec | Ubicación | Mensaje |
|---|---|---|---|
| V-AsistenteNuevoExpediente-001 | VAL-AsistenteNuevoExpediente-001 | `AsistenteNuevoExpedienteServiceImpl.validateCentroIndicado` (privado) — **único dueño**; lo invocan `…validatePrepararTramites`, `…validateRecalcular` (puerta 1) y `…validateCrear` (puerta 1) | "Debe indicar el centro" |
| V-AsistenteNuevoExpediente-002 | VAL-AsistenteNuevoExpediente-003 | `…validateCrear` (puerta 3) + copia de cortesía **con la misma condición** (`presentadoEnPapel == null`, sin ningún término más) en `views/Main-AsistenteNuevoExpediente.xml`, `sysVentanilla.Main@AsistenteNuevoExpediente-Local-validateCrear-action` | "Debe indicar cómo se presenta el expediente" |
| V-AsistenteNuevoExpediente-003 | VAL-AsistenteNuevoExpediente-004 | `…validateCrear` (puerta 4) + la misma copia de cortesía, también con la condición íntegra (`presentadoEnRepresentacion == null`) | "Debe indicar para quién es el expediente" |
| V-AsistenteNuevoExpediente-004 | VAL-AsistenteNuevoExpediente-005 | `…validateAlta` (puerta 5) → `TramitadorService.validateTriggerInitialEvent` | "No puede crear expedientes de este trámite en el centro indicado" |
| V-AsistenteNuevoExpediente-005 | VAL-AsistenteNuevoExpediente-006 | `…validateAlta` (puerta 5) → `TramitadorService.validateTriggerInitialEvent` | "No puede presentar el expediente de esa forma en el centro indicado" |
| V-AsistenteNuevoExpediente-006 | VAL-AsistenteNuevoExpediente-007 | `…validateAlta` (puerta 5) → `TramitadorService.validateTriggerInitialEvent` | "Este trámite no permite presentar la solicitud en representación de otra persona" |
| V-AsistenteNuevoExpediente-007 | VAL-AsistenteNuevoExpediente-008 | `…validateAfinadoDestinatario` | "No puede crear este expediente para usted mismo en el centro indicado" |
| V-AsistenteNuevoExpediente-008 | VAL-AsistenteNuevoExpediente-009 | `…validateAfinadoDestinatario` | "No puede crear este expediente en representación de otra persona en el centro indicado" |
| V-AsistenteNuevoExpediente-009 | — | `AsistenteNuevoExpedienteServiceImpl.validateTramiteEvaluable` (privado) — **único dueño**: el trámite está entre `VentanillaRepository.findTramitesEvaluables()` (condición que cubre también el trámite ausente); lo invocan `…validateRecalcular` (puerta 2) y `…validateCrear` (puerta 2) | "Debe indicar el trámite" |

### Reglas de negocio

| R | Origen spec | Ubicación | Momento | Detalle |
|---|---|---|---|---|
| R-AsistenteNuevoExpediente-001 | RN-AsistenteNuevoExpediente-001 | `views/Main-AsistenteNuevoExpediente.xml`, `sysVentanilla.Main@AsistenteNuevoExpediente-btnCrear-action` → `sysVentanilla.Main@AsistenteNuevoExpediente-Remote-triggerInitialEvent-action` (`TramitadorController.triggerInitialEvent` → `TramitadorService.triggerInitialEvent`) | antes de commit; crea el expediente, cierra el asistente y abre el expediente | — |
| R-AsistenteNuevoExpediente-003 | — | `…fireActionRule_AsignarArranqueDelAsistente` (desde `prepararCentros`, su único llamante) | Antes — asignación incondicional | — |
| R-AsistenteNuevoExpediente-004 | — | `…fireActionRule_AsignarTramitesDisponibles` (desde `prepararTramites`) | Antes — asignación incondicional | — |
| R-AsistenteNuevoExpediente-005 | — | `…fireActionRule_AsignarPresentacion` (desde `recalcular`), que calcula la matriz y delega las dos asignaciones en `asignarFormaDePresentar` y `asignarDestinatario` | Antes — asignación incondicional | Detalle: design/rules/R-AsistenteNuevoExpediente-005.md |

> «Antes» aquí significa **antes de devolver el bean a la vista**, no antes de `repository.save`: el modelo no se persiste nunca, así que en este sistema no hay ningún `save` al que anteponerse. Las tres reglas cumplen lo que esa clasificación garantiza — que ningún campo `servidor` se queda con lo que mandó el cliente.

> **No existen `R-AsistenteNuevoExpediente-002` ni `-006`** (los huecos son intencionados y la numeración de las demás no se toca). `R-002` era la regla que asignaba `nombreTramite` y `ayudaTramite`, y desapareció al materializar `CC-AsistenteNuevoExpediente-001` y `-002` como campos derivados de solo lectura: un `CC-` con `momento: lectura` no necesita ninguna `R-` que lo asigne, porque no se persiste. `R-006` era la regla del destinatario, absorbida por `R-005`: las dos preguntas del paso 3 no son separables (la segunda solo puede contestarse sobre el `presentadoEnPapel` que fija la primera) y salen de la misma matriz, así que son una sola regla. Las referencias a `R-006` de `test-e2e-desc.md` se han reapuntado a `R-005`.

### Campos calculados

| CC del spec | Momento / sobreescribible | Ubicación |
|---|---|---|
| CC-AsistenteNuevoExpediente-001 (nombre del trámite) | lectura / nunca | campo derivado `nombreTramite` (`transient="true"` con cuerpo) de `domains/AsistenteNuevoExpediente.xml`: el getter devuelve `I18n.get("value:" + tramite.getName())` —la clave real con la que el build registra el nombre de un trámite, la misma que usa `Tramites.xml` con `_t("value:"+name)`—, o null sin trámite. Fuera de todas las `AllowProperties`. |
| CC-AsistenteNuevoExpediente-002 (ayuda del trámite) | lectura / nunca | campo derivado `ayudaTramite` (`transient="true"` con cuerpo) de `domains/AsistenteNuevoExpediente.xml`: el getter devuelve `tramite.getHelp()`, o null sin trámite. Fuera de todas las `AllowProperties`. |

## Notas y supuestos del diseño (verbatim)

## Notas y supuestos

- **El paso 1 se abre como pestaña, no como ventana emergente.** El spec describe «Listado de centros» como ventana emergente. Con una emergente, el clic en una fila del listado no puede cerrarla (`DefaultActionExecutor.#closeView()` se rinde si hay emergentes y el `FormActionHandler` del listado embebido no tiene `closeHandler`), así que el asistente se quedaría a medio cerrar al crear el expediente. El paso 3, que es donde el spec vuelve a pedir ventana emergente, sí lo es. Detalle y alternativas en **D1** de `decisiones.md`.
- **Mientras el paso 1 decide, no muestra nada.** El listado de centros y el botón «Cancelar» solo se ven si `hayQueElegirCentro`, así que en los casos «ningún centro» y «un solo centro» el usuario **nunca** ve el listado de centros, como exigen ESC-001 y ESC-003: la ventana existe un instante, pero vacía, y se cierra sola.
- **El modelo vive en `domains/` y no en `views-models/`.** Motivo y alternativa en **D6** de `decisiones.md`.
- **`hayQuePreguntarPresentacion`, `hayQuePreguntarParaQuien`, `centrosDisponibles`, `tramitesDisponibles` y `hayQueElegirCentro` no están en `entity-AsistenteNuevoExpediente.md`.** No son campos de negocio: son la forma de que el servidor —único que conoce los perfiles del usuario— conteste a las reglas de UI que el propio spec exige (`RUI-…-formulario-002` a `-009` y `RUI-…-arbol-tramites-001`). Sin ellos esas reglas no se podrían evaluar en la vista, o habría que replicar la clasificación en JavaScript (**D2** y **D3** de `decisiones.md`).
- **Las dos preguntas no llevan `requiredIf`.** Con `requiredIf`, la acción `validate` del framework cortaría antes con su mensaje genérico y el usuario nunca vería «Debe indicar cómo se presenta el expediente» / «Debe indicar para quién es el expediente», que el spec fija literalmente. La obligatoriedad se expresa con el `<action-validate>` local y, sobre todo, con las puertas 3 y 4 de `validateCrear`.
- **El `btnCrear` NO invoca la acción global `subsysTramitador-trigger-initial-event-action`, pese a que `design-guidelines.md` pide reutilizarla tal cual.** En su lugar declara en la sección `<?sv-remotes?>` de `views/Main-AsistenteNuevoExpediente.xml` una acción propia, `sysVentanilla.Main@AsistenteNuevoExpediente-Remote-triggerInitialEvent-action`, que llama al **mismo** `TramitadorController.triggerInitialEvent` sin `<context>` propio: la cadena es funcionalmente equivalente —el controlador lee del formulario los mismos cuatro campos `tramite`, `centro`, `presentadoEnPapel` y `presentadoEnRepresentacion`, que por eso se llaman exactamente así—, y lo único que la ventanilla aporta es el **nombre local**. El motivo es de ámbito, no de comportamiento: `VAR-4.1` exige que el `<action name>` de un `action-group` resuelva contra una acción declarada en el ámbito de las vistas del sistema, y la global vive en `subsystem/tramitador/controller/actions-tramitador.xml`. Ese fichero **MUST NOT** tocarse: sigue siendo el que usa `system/expedientes/views/Main-NuevoExpediente.xml`, pantalla que esta iniciativa no retira. La desviación afecta al nombre de la acción, **no** al controlador ni al servicio del motor, que se reutilizan tal cual. Detalle del choque con `view-rules.md` en **D8** de `decisiones.md`.
- **El alta la ejecuta el controlador del tramitador, y V-007/V-008 no la defienden — riesgo aceptado.** El afinado propio de la ventanilla (V-007 y V-008) solo se evalúa en `…-Remote-validateCrear-action`, que es una llamada previa e independiente dentro del `action-group` del botón; quien invoque directamente `TramitadorController.triggerInitialEvent` (por la acción de esta vista, por la del tramitador o por su `@CallMethod`) crea el expediente sin pasar por él. El afinado no se traslada al motor. Motivo, alternativa descartada y qué **MUST NOT** hacerse como parche puntual: **D7** de `decisiones.md`.
- **Las cuatro acciones propias declaran su `validate<Accion>`, y las consultan los dos lados.** También `prepararCentros`, cuyo validador no tiene hoy ninguna precondición: el par acción + validador es el contrato público mínimo de toda acción (`k-sistemas/servicios.md`). El **controlador** consulta el validador y contesta sus mensajes con `doResponseBusinessMessagesAsError` antes de invocar la acción, y la **acción** empieza igualmente por `validate<Accion>(...).ifPresent(BusinessMessages::throwIfInvalid)` como red de seguridad de cualquier otro llamante. Sin el tramo del controlador el mensaje llegaría al usuario como `ValidationException` cruda, que es el mismo 500 disfrazado que se quería evitar al sustituir `IllegalStateException`.
- **La ficha no se guarda porque no hay dónde guardarla.** `persistable="false"` genera el modelo como `@MappedSuperclass`: sin `@Entity` ni `@Table`, no tiene tabla ni endpoint de guardado utilizable, y ninguna de las tres vistas declara `save` ni `delete`. La única defensa que hace falta —y la única que se ejecuta— es el deny-all de `allowPropertiesInsert/Update`, que corre en `modelService.validate`.
- **El perfil de inicio NO se obtiene llamando a `PerfilesUsuarioService.getPerfil(tramite, user, centro, presentadoEnPapel)`, pese a que `design-guidelines.md` pide reutilizarla tal cual.** `getPerfil` lanza `IllegalStateException` precisamente en los dos casos que HU-006 exige rechazar con un mensaje de negocio, y `recalcular` —que es quien necesita el perfil— corre en el `onNew`/`onChange`, antes de cualquier validación: llamarla ahí convertiría una petición manipulada en un 500 en vez del mensaje que pide HU-006. En su lugar, `perfilDeInicioPara(boolean)` deriva el perfil de inicio preguntando a los predicados del propio enum `Profile` (`puedeCrearExpediente()` y `permitePresentacionEnPapel()`), sin consultar los perfiles del usuario. Detalle y justificación completa en **D5** de `decisiones.md`.
