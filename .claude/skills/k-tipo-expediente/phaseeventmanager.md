# El `PhaseEventManager` y el `InitialEventManager` — la lógica del tipo en Java

Clases Java que deciden qué pasa en cada evento y a qué estado se transita. Los esqueletos los genera `CreateFilesTask` (`SKILL.md` §3.1) con todos los métodos requeridos vacíos; compilar **no** los genera.

Los ejemplos usan el trámite inventado `MiTramite` (`SKILL.md`). Para ver uno de verdad, abre el `PhaseEventManagerImpl.java` de cualquier fase bajo `src/main/java/com/educaflow/tramites/`.

- **Hay un `PhaseEventManagerImpl` por fase**, en `<vN>/<fase en minúsculas>/`, y atiende **solo** los estados de su fase y los eventos que salen de ellos (`SKILL.md` §1.4).
- **Hay un único `InitialEventManagerImpl` por tipo**, en la raíz de la versión: atiende el alta (§2.1).
- Qué hace el motor antes y después de llamar a estos métodos: `SKILL.md` §1.6.

## 1. Anatomía

```java
package com.educaflow.tramites.mi_tramite.v1.recepcion;

import com.educaflow.tramites.mi_tramite.v1.States;

public class PhaseEventManagerImpl extends PhaseEventManager<MiTramiteV1> {

    private final MiTramiteV1Repository repository;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    AlmacenClaveResolver almacenClaveResolver;      // firma con el certificado del centro (§6.4)
    @Inject
    private ModelServiceFactory modelServiceFactory; // servicios de otros subsistemas (§6.6)

    @Inject
    public PhaseEventManagerImpl(MiTramiteV1Repository repository) {
        super(MiTramiteV1.class);
        this.repository = repository;
    }
    // ... triggers y onEnters
}
```

- La instancia Guice: admite inyección normal (repositorio por constructor, `@Inject` de campo para lo demás; `k-guice` si la construcción no es trivial).
- El repositorio de su entidad y el `log` van **siempre**, aunque no se usen: son parte de la plantilla. **MUST NOT** quitarlos al limpiar.
- El trámite es dueño de las entidades de su `domains.xml`: puede usar su repositorio y `JpaRepository.of(...)` sobre ellas. **MUST NOT** usar el repositorio ni `JpaRepository.of(...)` de una entidad de otro trámite o de un sistema/subsistema: esos datos se piden al servicio del dueño (§6.6). Lo verifican las reglas C26 y C27 de `agent_docs/architecture-rules.md`.
  - ✅ CORRECTO: `JpaRepository.of(MiTramiteV1.class)` (entidad del propio trámite)
  - ✅ CORRECTO: `(RegistroEntradaService) modelServiceFactory.resolve(RegistroEntrada.class)`
  - ❌ INCORRECTO: `@Inject RegistroEntradaRepository registroEntradaRepository;` (se salta los servicios del subsistema dueño)
- Un estado se nombra `States.<Fase>.<ESTADO>` (`SKILL.md` §2.2). `States` lleva **todas** las fases del tipo, así que un `updateState` que cruza de fase se escribe igual que uno que no.

## 2. Los métodos (convención de nombres)

| Método | Cuándo se invoca | Firma exacta |
|---|---|---|
| `trigger<EventoEnUpperCamel>` | Al disparar el evento | `@WhenEvent public void trigger<Evento>(<Entidad> expediente, <Entidad> original, EventContext eventContext) throws BusinessException` |
| `onEnter<EstadoEnUpperCamel>` | Al **entrar** en el estado (tras el `trigger*`) | `@OnEnterState public void onEnter<Estado>(<Entidad> expediente, EventContext eventContext)` |

- `<Estado>` es el nombre del estado dentro de su fase (`ENTRADA_DATOS` → `onEnterEntradaDatos`).
- `expediente` ya trae los datos del formulario copiados y validados. `original` es el expediente **antes** de copiarlos: úsalo para saber cómo estaba (p. ej. desde qué estado se dispara un evento multi-origen, §5) o para comparar.
- Los `onEnter<Estado>` pueden quedarse vacíos, pero **MUST** existir todos los de la fase.
- **MUST NOT** declarar un `triggerInitialEvent` en un `PhaseEventManagerImpl`: no lo llama nadie (test E5).

### 2.1 El alta: `InitialEventManagerImpl`, uno por tipo

```java
package com.educaflow.tramites.mi_tramite.v1;

public class InitialEventManagerImpl implements InitialEventManager<MiTramiteV1> {

    @Override
    public void triggerInitialEvent(InitialEventContext<MiTramiteV1> initialEventContext) throws BusinessException {
        MiTramiteV1 expediente = initialEventContext.getExpediente();
        // ... valores iniciales de los campos propios del tipo
        initialEventContext.updateState(States.Recepcion.ENTRADA_DATOS);
    }
}
```

- El parámetro de tipo de `implements InitialEventManager<…>` **MUST** ser la entidad del tipo: es de donde el motor saca qué entidad instanciar (test M1). **MUST NOT** implementarla en crudo.
- Cuando se llama, el motor ya ha rellenado los campos del alta (`modelo.md` §2). `initialEventContext.getContextoTramitacion()` da además el trámite, el centro y el perfil con que se crea.
- **CRITICAL**: **MUST** fijar el estado inicial con `initialEventContext.updateState(...)`. El XML no declara estado inicial; si no se fija, el alta revienta y ningún test lo detecta antes.
- El estado inicial puede depender de cómo se crea el expediente (`perfiles.md` §3).
- Inicializa aquí los campos propios del tipo que no dependen de lo que teclee el usuario (curso, datos del centro…).
- **MUST NOT** crear ni reasignar `personaSolicitante` ni `personaInteresada`: las decide el alta (`modelo.md` §2.1).
- El `onEnter<Estado>` del estado inicial se ejecuta justo después, en el `PhaseEventManagerImpl` de su fase.

- ✅ CORRECTO: `initialEventContext.updateState(Boolean.TRUE.equals(expediente.getPresentadoEnPapel()) ? States.Recepcion.PENDIENTE_ESCANEADO : States.Recepcion.ENTRADA_DATOS)`
- ❌ INCORRECTO: un `triggerInitialEvent` con el cuerpo vacío (el alta falla en runtime: no hay estado inicial)

### 2.2 En cada `trigger<Evento>`

1. Primero las **guardas** que lanzan `BusinessException`, antes de crear nada: lo que ya se haya creado fuera del expediente no se deshace si después se lanza (`SKILL.md` §1.6).
2. Después la lógica de negocio.
3. Por último, **la decisión del estado destino** con `eventContext.updateState(States.<Fase>.<ESTADO>)`, que puede depender de los datos (§5) y puede estar en **otra fase**.

Lo que **se repite en varios `trigger*`** (del mismo o de distinto `PhaseEventManagerImpl`) sale a una función estática de `<Code>Util` (`SKILL.md` §1.7). Lo que se hace en un solo evento se queda inline.

- **Quién puede disparar el evento**, más allá del perfil del estado que ya exige el motor (que es el creador, que pertenece al centro del expediente…): una función `exige<Condicion>(expediente, mensaje)` que lanza `BusinessException(I18n.get(mensaje))`. Se llama en la primera línea del `trigger*`, con un mensaje propio de cada evento.
- **Limpiar campos que dejan de tener sentido** al cambiar de rama (el motivo de rechazo al aceptar, el texto de subsanación al presentar de nuevo): en el `trigger*` del evento, no en la vista. Si varios eventos limpian lo mismo, una mutación de `<Code>Util`.

```java
@WhenEvent
public void triggerPresentar(MiTramiteV1 expediente, MiTramiteV1 original, EventContext eventContext) throws BusinessException {
    MiTramiteV1Util.exigeSerElCreador(expediente, "Solo puede presentar sus propias solicitudes");

    MiTramiteV1Util.borrarDevolucion(expediente);   // también lo hacen otros dos eventos
    ... // lo propio de este evento: generar el PDF, crear el registro de entrada
    eventContext.updateState(States.Tramitacion.PENDIENTE_REVISION);
}
```

## 3. API de `EventContext`

| Método | Qué hace |
|---|---|
| `updateState(State)` | Fija el estado destino. Solo admite estados del `States` del propio tipo |
| `getProfile()` | Perfil con el que actúa el usuario |
| `createRegistroEntrada(MetaFile documentoPdf, List<MetaFile> anexos)` | Crea el registro de entrada (§6.2) y lo devuelve. **LIMIT**: uno por evento |
| `createRegistroSalida(MetaFile documentoPdf, List<MetaFile> anexos)` | Crea el registro de salida (§6.3) y lo devuelve. **LIMIT**: uno por evento |
| `getRegistroEntrada()` / `getRegistroSalida()` | El registro creado en este evento |

- El registro de **entrada** toma del expediente el centro, el solicitante, el interesado y el asunto (`"Expediente: <numeroExpediente> - <name>"`); el de **salida**, solo el centro y el asunto.
- En los dos, el documento **MUST** ser un PDF no nulo y los anexos **MUST** tener `fileName`; la lista de anexos puede ser `null`. Los anexos se clonan.
- El registro creado queda enlazado en el historial del expediente sin hacer nada más.
- El centro del expediente se lee con `expediente.getCentro()`.

## 4. Eventos comunes

- `EXIT` (cerrar la pestaña): no llega al `PhaseEventManagerImpl`.
- `DELETE`: no valida ni copia campos y borra el expediente. **MUST** existir su `@WhenEvent triggerDelete` (test E1); lo normal es que contenga solo las guardas de quién puede borrar. Su método en el validador **no** hace falta (`validator.md` §5).
- `BACK` **no es común**: se declara en `events="..."`, se implementa (§5) y se le da método en el validador.

## 5. Patrones de transición

**Condicional por datos** (un evento, varios destinos):

```java
switch (expediente.getTipoResolucion()) {
    case ACEPTAR ->        eventContext.updateState(States.Tramitacion.ACEPTADO);
    case RECHAZAR ->       eventContext.updateState(States.Tramitacion.RECHAZADO);
    // el destino está en OTRA fase: no hay nada especial que hacer
    case SUBSANAR_DATOS -> eventContext.updateState(States.Recepcion.ENTRADA_DATOS);
    case null, default -> throw new IllegalArgumentException("Tipo de resolución no reconocido: " + expediente.getTipoResolucion());
}
```

**Evento multi-origen** (el mismo evento declarado en varios estados de la fase decide según el estado desde el que se dispara):

```java
State origen = States.INSTANCE
        .getState(original.getCodePhase(), original.getCodeState())
        .orElseThrow(() -> new IllegalStateException("Estado no reconocido: "
                + original.getCodePhase() + "/" + original.getCodeState()));

switch (origen) {
    case States.Recepcion.PENDIENTE_PRESENTACION -> eventContext.updateState(States.Recepcion.ENTRADA_DATOS);
    ...
    default -> throw new IllegalStateException("Estado no reconocido: " + origen);
}
```

- El `default` es obligatorio: `State` no es `sealed`.
- Si el evento solo sale de **un** estado, no hace falta: llama directamente a `updateState`.
- Si el mismo evento sale de estados de **fases distintas**, cada fase lleva su propio `trigger<Evento>` y cubre solo los estados de la suya.

## 6. Catálogo de acciones de un evento

Catálogo **abierto**: cuando aparezca una capacidad nueva, añádela aquí como un apartado más con su patrón.

### 6.1 Generar un documento PDF y guardarlo en la entidad

```java
DocumentoPdf solicitudPdf = expediente.getDocumentoPdf(MiTramiteV1.TipoDocumentoPdf.SOLICITUD);
expediente.setPdfSolicitud(MetaFileHelper.createMetaFile(solicitudPdf));
```

`getDocumentoPdf` genera el documento de `documentospdf/` con los datos del expediente (`documentos.md`). `MetaFileHelper.createMetaFile(documentoPdf)` lo convierte en un `MetaFile` asignable a un campo.

**MUST** asignar las fechas que estampa el documento (y el resto de datos de ese momento, como el firmante) **antes** de llamar a `getDocumentoPdf`, en el mismo evento: el PDF es una foto de la entidad en ese instante.

- ✅ CORRECTO:
  ```java
  expediente.setFechaResolucion(LocalDate.now(Convert.defaultZoneId));
  expediente.setFirmadoPor(SecurityUtil.getUser());
  DocumentoPdf resolucionPdf = expediente.getDocumentoPdf(MiTramiteV1.TipoDocumentoPdf.RESOLUCION);
  ```
- ❌ INCORRECTO: generar `resolucionPdf` y asignar `fechaResolucion` en la línea siguiente (el documento sale sin la fecha que dice llevar).
- ❌ INCORRECTO: asignar `fechaResolucion` en `onEnter<Estado>` del estado destino (se ejecuta después del evento que ya generó el documento).

Operaciones útiles de `DocumentoPdf`: `firmar(...)` (§6.4), `anyadirDocumentoPdf` (concatenar; p. ej. anexar un justificante con `MetaFileHelper.getDocumentoPdfFromImagenOrPdf(metaFile)`), `estamparTextoConAppend`, `addNewPage`, `getPlainText`.

**Generar en un evento y firmar en otro posterior.** El PDF que un evento dejó en un campo se recupera con `MetaFileHelper.getDocumentoPdf(metaFile)`:

```java
// evento 1 (p.ej. ENVIAR_A_FIRMA): se genera sin firmar para que el firmante lo revise
expediente.setPdfResolucion(MetaFileHelper.createMetaFile(expediente.getDocumentoPdf(MiTramiteV1.TipoDocumentoPdf.RESOLUCION)));

// evento 2 (p.ej. FIRMAR): se recupera, se firma (recetas/firma.md §2) y se registra de salida (§6.3)
DocumentoPdf resolucion = MetaFileHelper.getDocumentoPdf(expediente.getPdfResolucion());
DocumentoPdf resolucionFirmada = resolucion.firmar(almacenDirector, new CampoFirma(CAMPO_FIRMA_RESOLUCION));
MetaFile pdfResolucionFirmada = MetaFileHelper.createMetaFile(resolucionFirmada);
```

- Si entre los dos eventos cambian datos que el documento estampa, en el evento 2 se **regenera** con `getDocumentoPdf` en vez de recuperarlo.
- **MUST NOT** generar ni firmar un documento que el destino no necesita (p. ej. la resolución cuando se pide subsanar).

### 6.2 Registro de entrada (el usuario presenta documentación)

```java
RegistroEntrada registroEntrada = eventContext.createRegistroEntrada(expediente.getPdfSolicitudFirmado(), List.of(expediente.getJustificante()));
expediente.setPdfJustificanteRegistroEntrada(registroEntrada.getDocumentoResguardoPresentacion());
```

El registro devuelve el **resguardo de presentación** sellado, que se guarda en la entidad para mostrarlo. El camino entero está en la receta `recetas/presentacion.md`.

### 6.3 Registro de salida (la administración emite un documento)

```java
RegistroSalida registroSalida = eventContext.createRegistroSalida(pdfResolucionFirmada, List.of());
expediente.setPdfResolucion(registroSalida.getDocumento());
```

El registro devuelve el **documento registrado** (`getDocumento()`), que es el que se guarda y se muestra al usuario.

### 6.4 Firmar documentos

Todo está en la receta `recetas/firma.md`: el usuario firma al presentar (§1), el centro firma un documento que emite con su certificado (§2) y poner un documento a firmar a otro usuario (§3).

### 6.5 Enviar correos (subsistema Correos)

Patrón previsto (aún sin uso real en ningún trámite): insertar un `Correo` con su servicio; el alta ya programa el envío.

```java
CorreoService correoService = (CorreoService) modelServiceFactory.resolve(Correo.class);
Correo correo = new Correo();
correo.setPara("destinatario@example.com");        // admite varios separados por comas
correo.setAsunto("...");
correo.setCuerpo("...");
correo.setCentro(expediente.getCentro());
// opcionales: dniDestinatario/nombre/apellidos, enCopia, enCopiaOculta, adjuntos, historialEstado
correoService.insert(correo);
```

El correo no se puede modificar ni borrar después de crearlo.

### 6.6 Acceder a servicios de otros subsistemas

`modelServiceFactory.resolve(<Entidad>.class)` devuelve el `ModelService` del subsistema dueño. Para dependencias que no son `ModelService`, inyección Guice normal.

## 7. Los tests que comprueban estas clases

Al ejecutar los tests (`SKILL.md` §3.3) el mensaje de fallo trae el código del método que falta, listo para pegar.

- **E0**: existe `<paquete de la fase>.PhaseEventManagerImpl` y extiende `PhaseEventManager`.
- **E1 / E3**: **exactamente un** `trigger<Evento>` por evento de la fase y **un** `onEnter<Estado>` por estado de la fase, con la firma de §2.
- **E2 / E4**: ningún `@WhenEvent`/`@OnEnterState` de más. Si quitas un evento del XML, quita su método (y el del validador); si mueves un estado de fase, mueve su `onEnter`.
- **E5**: ningún `PhaseEventManagerImpl` declara `triggerInitialEvent`.
- **I1 / I2**: existe `InitialEventManagerImpl` en la raíz y declara su `triggerInitialEvent`.
- **M1**: todas estas clases usan como entidad la primera `<entity>` del `domains.xml`.
- **A1**: ningún nombre de estado o evento produce un método que pise uno de la clase base.
- Solo cuentan los métodos **declarados en la propia clase**: uno heredado de una superclase no lo ve ni el test ni el motor.

## 8. Anti-patrones

- **MUST NOT** olvidar `eventContext.updateState(...)` en un `trigger*` que deba transitar.
- **MUST NOT** validar aquí datos que teclea el usuario: eso es del validador (que además es la whitelist). Aquí solo lógica de negocio y guardas.
- **MUST NOT** lanzar una `BusinessException` después de crear un registro, un `MetaFile` o un correo (§2.2).
- **MUST NOT** usar `System.out`: logger slf4j.
- **MUST NOT** llamar dos veces a `createRegistroEntrada`/`createRegistroSalida` en el mismo evento.
- **MUST NOT** nombrar un estado por sus strings (`"ENTRADA_DATOS"`, `codeState.equals(...)`): usa `States.<Fase>.<ESTADO>`, que se compara con `==`.
- **MUST NOT** editar ni versionar la clase `States`.
- **MUST NOT** poner el `trigger`/`onEnter` de un estado en la clase de otra fase.
- **MUST NOT** inicializar el expediente en un `PhaseEventManagerImpl`: va en el `InitialEventManagerImpl` (§2.1).
- **MUST NOT** factorizar los `trigger`/`onEnter` comunes a una superclase compartida: deja el método declarado en cada fase y que delegue en `<Code>Util`.
