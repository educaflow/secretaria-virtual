# El `PhaseEventManager` — la máquina de estados en Java

Clase Java que decide qué pasa en cada evento y a qué estado se transita. El esqueleto lo genera `./gradlew CreateFilesTask` (`SKILL.md` §3.1) con todos los métodos requeridos vacíos; compilar **no** lo genera.

Los ejemplos usan el trámite inventado `MiTramite` (`SKILL.md`); para ver uno de verdad, abre el `PhaseEventManagerImpl.java` de cualquier fase bajo `src/main/java/com/educaflow/tramites/`.

**Hay uno por fase**, en `<vN>/<fase en minúsculas>/PhaseEventManagerImpl.java`, y cada uno atiende **solo los estados de su propia fase** y los eventos que salen de ellos (`SKILL.md` §1.4). En runtime lo resuelve `ExpedienteLocator` a partir de la fase (`SKILL.md` §1.6).

El **evento inicial** no está aquí: es del tipo de expediente entero y lo atiende el `InitialEventManagerImpl` de la raíz de la versión (§2.1).

## 1. Anatomía

```java
package com.educaflow.tramites.mi_tramite.v1.recepcion;

import com.educaflow.tramites.mi_tramite.v1.States;

public class PhaseEventManagerImpl extends PhaseEventManager<MiTramiteV1> {

    private final MiTramiteV1Repository repository;

    @Inject
    AlmacenClaveResolver almacenClaveResolver;      // firma en servidor (§6.4)
    @Inject
    private ModelServiceFactory modelServiceFactory; // servicios de otros subsistemas (§6.8)

    @Inject
    public PhaseEventManagerImpl(MiTramiteV1Repository repository) {
        super(MiTramiteV1.class);
        this.repository = repository;
    }
    // ... triggers y onEnters
}
```

- Se instancia por **reflexión + Guice** (el FQCN lo compone `ExpedienteLocator` con el `basePackageName` de la tabla `TipoExpediente` y el `codePhase` del expediente), así que admite inyección normal: repositorio por constructor y `@Inject` de campo para lo demás.
- **La máquina de estados NO está aquí**: vive en la clase `States` que el build genera del `TipoExpedienteInstance.xml` (`SKILL.md` §2.3) y que este fichero solo importa. Cambiar estados o eventos en el XML se propaga solo; lo único que hay que actualizar a mano son los métodos, y los errores de los tests (§7) te dan el código a copiar.
- Un estado se nombra `States.<Fase>.<ESTADO>`, con la fase en **UpperCamelCase** (`States.Recepcion.ENTRADA_DATOS`); `States.RECEPCION` es otra cosa: el alias de la fase, tipado `Phase`, y no lleva estados. La clase lleva **todas** las fases del tipo, así que un `updateState` que cruza de fase se escribe igual que uno que no.

## 2. Los dos tipos de método (convención de nombres)

| Método | Cuándo se invoca | Firma exacta |
|---|---|---|
| `trigger<EventoEnUpperCamel>` | Al disparar el evento | `@WhenEvent void trigger<Evento>(<Entidad> actual, <Entidad> original, EventContext) throws BusinessException` |
| `onEnter<EstadoEnUpperCamel>` | Al **entrar** en el estado (tras el trigger) | `@OnEnterState void onEnter<Estado>(<Entidad>, EventContext)` |

**El `<Estado>` del nombre del método es el nombre del estado dentro de su fase** (`ENTRADA_DATOS` → `onEnterEntradaDatos`): la clase ya está en el paquete de su fase y no hay ambigüedad. `PhaseEventManager.onEnterState` compone el nombre del método con el `codeState`, que ya es ese nombre.

**MUST NOT** declarar aquí un `triggerInitialEvent`: el evento inicial no es de ninguna fase (§2.1) y un método así **no lo llamaría nadie**. Como la clase base ya no declara el método, tampoco habría un `@Override` que fallase al compilar; lo detecta el test E5 (§7).

### 2.1 El evento inicial: `InitialEventManagerImpl`, uno por tipo de expediente

El evento inicial se dispara al **crear** el expediente, cuando todavía no hay estado del que partir, así que **es del tipo de expediente, no de una fase**. Lo atiende una clase aparte, **exactamente una por tipo**, en la raíz de la carpeta de versión junto al `TipoExpedienteInstance.xml`:

```java
package com.educaflow.tramites.mi_tramite.v1;

public class InitialEventManagerImpl implements InitialEventManager<MiTramiteV1> {

    @Override
    public void triggerInitialEvent(MiTramiteV1 miTramite, EventContext eventContext) throws BusinessException {
        ...
    }
}
```

- La interfaz `InitialEventManager<T extends Expediente>` está en `subsystem/expedientes/services/eventmanager/` y tiene ese único método.
- El nombre de la clase es fijo (`InitialEventManagerImpl`): lo resuelve `ExpedienteLocator.getInitialEventManager(tipoExpediente)` por reflexión sobre el `basePackageName` (`SKILL.md` §1.6), así que se instancia con Guice y admite inyección normal.
- El esqueleto lo genera `CreateFilesTask` entre los ficheros de la raíz de la versión (`SKILL.md` §3.1).

Sus obligaciones (antes de llamarlo, `Tramitador` ya rellenó tipo, centro, `usuarioRegistrador`, `name` y `numeroExpediente`):

**`Tramitador` no impone ninguna**: un `triggerInitialEvent` con el cuerpo vacío es un tipo de expediente perfectamente válido y pasa todos los tests.
Qué hay que rellenar lo decide **lo que el tipo use después**, y el fallo aparece más tarde, en el sitio que lo usa:

1. **Si los documentos de entrada del tipo se firman**: **MUST** dejar relleno `dniFirmaDocumentoEntrada` con un DNI válido.
   No lo exige `Tramitador` al crear el expediente, sino `FirmaController.firmarDocumentoEntrada` **en el momento de firmar**, con tres `RuntimeException` planas (null / en blanco / `DniUtil.isValid` falso).
   Es decir: el expediente se crea sin problema y revienta después, al pulsar el botón de firmar.
2. **Si el tipo crea registro de entrada** (`eventContext.createRegistroEntrada`, §3): **MUST** dejar rellenos `personaSolicitante` y `personaInteresada` (si `personaSolicitante` es null, `createRegistroEntrada` revienta con NPE). Patrón habitual: construir la `Persona` desde `getUsuarioRegistrador()`.
3. Inicializar el resto de campos con valor por defecto (año, etc.).

**MUST NOT** dar por hecho que estos campos son obligatorios siempre: un tipo que ni firma ni registra no necesita ninguno.

El `onEnter<Estado>` del **primer estado** sí es de una fase, y se ejecuta justo después: vive en el `PhaseEventManagerImpl` de la fase del estado inicial, como cualquier otro.

### 2.2 En cada método de la fase

En cada `trigger<Evento>`: la lógica de negocio y **la decisión del estado destino** con `eventContext.updateState(States.<Fase>.<ESTADO>)` (puede depender de los datos, §5); el destino puede estar en **otra fase** con toda normalidad. Los `onEnter<Estado>` pueden quedarse vacíos pero **MUST** existir todos los de la fase.

## 3. API de `EventContext`

| Método | Qué hace |
|---|---|
| `updateState(State)` | Fija el estado destino de la transición |
| `getProfile()` | Perfil con el que actúa el usuario |
| `getCentro()` | Centro activo |
| `createRegistroEntrada(MetaFile documentoPdf, List<MetaFile> anexos)` | Crea el registro de entrada (§6.2) y lo devuelve. **LIMIT**: uno por evento |
| `createRegistroSalida(MetaFile documentoPdf, List<MetaFile> anexos)` | Crea el registro de salida (§6.3) y lo devuelve. **LIMIT**: uno por evento |
| `getRegistroEntrada()` / `getRegistroSalida()` | Recuperan el registro creado en este evento |

El registro de **entrada** toma del expediente el solicitante, el interesado, el número de expediente y el asunto (más el centro); el de **salida** toma **solo el centro y el asunto**. El asunto lo compone `EventContext` como `"Expediente: <numeroExpediente> - <name>"`, así que el número llega también al de salida dentro de él. En ambos, los anexos se clonan y **MUST** tener `fileName` no nulo (se admite `null` como lista).

## 4. Eventos comunes

- `EXIT` (cerrar pestaña): lo intercepta `ExpedienteController`, **no llega al PhaseEventManager**.
- `DELETE`: no valida ni copia campos, hace `repository.remove` directamente — pero **MUST** existir su `@WhenEvent triggerDelete` (vacío) si el XML lo declara, porque el test E1 lo exige. Su método en el validator, en cambio, **no** hace falta (`validator.md` §5).
- `BACK` **no es común**: se declara en `events="..."`, se implementa (§5) y se le da método en el validator.

## 5. Patrones de transición

**Condicional por datos** (un evento, varios destinos):

```java
switch (expediente.getTipoResolucion()) {
    case ACEPTAR ->        eventContext.updateState(States.Tramitacion.ACEPTADO);
    case RECHAZAR ->       eventContext.updateState(States.Tramitacion.RECHAZADO);
    // el destino está en OTRA fase: no hay nada especial que hacer
    case SUBSANAR_DATOS -> eventContext.updateState(States.Recepcion.ENTRADA_DATOS);
    default -> throw new IllegalArgumentException("Tipo de resolución no reconocido");
}
```

**Evento multi-origen** (el mismo evento declarado en varios estados decide según el estado actual). El estado actual se resuelve con la **pareja** de columnas contra el `States.INSTANCE` del propio tipo, que aquí se conoce en compilación:

```java
State actual = States.INSTANCE
        .getState(expediente.getCodePhase(), expediente.getCodeState())
        .orElseThrow(() -> new IllegalArgumentException("State no reconocido: "
                + expediente.getCodePhase() + "/" + expediente.getCodeState()));

switch (actual) {
    case States.Recepcion.PENDIENTE_PRESENTACION -> eventContext.updateState(States.Recepcion.ENTRADA_DATOS);
    ...
    default -> throw new IllegalArgumentException("State no reconocido: " + actual);
}
```

El `switch` sobre una variable tipada con la **interfaz** `State` es legal desde Java 21 (JEP 441), pero `State` **no** es `sealed`: el `default` es obligatorio.

Si el mismo evento sale de estados de **fases distintas**, cada fase lleva su propio `trigger<Evento>` y cada uno solo necesita cubrir los estados de la suya.

## 6. Catálogo de acciones de un evento

Catálogo **abierto**: aún no están definidas todas las posibilidades — cuando aparezca una capacidad nueva (notificaciones push, integración con otro subsistema…), añádela aquí como un apartado más con su patrón.

### 6.1 Generar un documento PDF y guardarlo en la entidad

```java
DocumentoPdf solicitudPdf = expediente.getDocumentoPdf(MiTramiteV1.TipoDocumentoPdf.SOLICITUD);
expediente.setPdfSolicitud(MetaFileHelper.createMetaFile(solicitudPdf));
```

`getDocumentoPdf` rellena el formulario del PDF evaluando las expresiones Groovy de sus campos con `self` = el expediente (ver `documentos.md`). `MetaFileHelper.createMetaFile(documentoPdf)` convierte cualquier `DocumentoPdf` en `MetaFile` asignable a un campo.

Operaciones útiles de `DocumentoPdf`: `firmar(...)` (§6.4), `anyadirDocumentoPdf` (concatenar), `estamparTextoConAppend`, `addNewPage`, `getPlainText`, `removePdfAConformance`.

### 6.2 Registro de entrada (el usuario presenta documentación)

```java
RegistroEntrada registroEntrada = eventContext.createRegistroEntrada(exp.getPdfSolicitudFirmado(), List.of(exp.getJustificante()));
exp.setPdfJustificanteRegistroEntrada(registroEntrada.getDocumentoResguardoPresentacion());
```

El registro devuelve el **resguardo de presentación** sellado (`getDocumentoResguardoPresentacion()`, ya `MetaFile`), que se guarda en un campo de la entidad para mostrarlo (visor de PDF, `vistas.md`).

### 6.3 Registro de salida (la administración emite un documento)

```java
RegistroSalida registroSalida = eventContext.createRegistroSalida(pdfResolucion, List.of(exp.getJustificante()));
exp.setPdfResolucion(registroSalida.getDocumento());
```

El registro devuelve el **documento registrado** (`getDocumento()`), que es el que se guarda y se muestra al usuario.

### 6.4 Firma en servidor (sello del centro: director, secretario…)

```java
private static final Rectangulo posicionFirma = new Rectangulo(75, 280, 400, 20);
...
DocumentoPdf resolucionFirmada = resolucion.firmar(almacenClaveResolver.getDirector(expediente.getCentro()), new CampoFirma(posicionFirma));
```

- `AlmacenClaveResolver` (inyectado): `getDirector(centro)`, `getSecretario(centro)`, `getByDNI(dni)`, `getDummy()` (pruebas).
- `CampoFirma` es un builder: `setMensaje/setMotivo/setFontSize/setNumeroPagina/setImage/setFechaFirma`.

### 6.5 Firma del usuario con AutoFirma

Tres piezas, una por fichero:

1. **Modelo**: par de campos `MetaFile` original/firmado (`modelo.md` §4).
2. **Vista**: `<action-method>` que llama a `FirmaController.firmarDocumentoEntrada(...)`, encadenada con `serial:` antes del evento en el botón (`vistas.md` §10) — exige firmar con el `dniFirmaDocumentoEntrada` del expediente.
3. **Validator**: regla `FirmaPdf(original, dniGetter)` en el evento que presenta (`validator.md` §4) — verifica en servidor que lo subido es el original firmado por ese DNI.

En el PhaseEventManager no hay código de AutoFirma: el trigger del evento ya recibe el campo firmado validado.

### 6.6 Poner documentos a firmar a otros usuarios (subsistema Firmas)

Estilo "portafirmas": crear una `TareaFirma` para que otro usuario firme, con callback cuando lo haga.

```java
public class PhaseEventManagerImpl extends PhaseEventManager<...> implements TareaFirmaNotifier {
    ...
    TareaFirmaService tareaFirmaService = (TareaFirmaService) modelServiceFactory.resolve(TareaFirma.class);
    tareaFirmaService.insert(new TareaFirmaInsertDTO(
            firmante,                 // User que debe firmar
            List.of(pdf1, pdf2),      // PDFs a firmar (MUST ser PDFs, lista no vacía)
            "Firma Expediente:" + expediente.getNumeroExpediente(),  // motivo
            new Rectangulo(100, 100, 400, 50), 1,                    // área y página de la firma visible
            this.getClass(),          // clase TareaFirmaNotifier del callback
            "datos de callback"));    // callBackData que se te devuelve

    @Override
    public void notify(TareaFirma tareaFirma, Object callBackData) { /* qué hacer al completarse la firma */ }
}
```

Nota: es un patrón **sin llamantes vivos** — el único que hubo era un `insert` de prueba, ya borrado. El patrón es este, pero confirma el caso de uso antes de copiarlo.

> **CRITICAL — la `TareaFirma` congela el FQCN del notifier.** El `this.getClass()` que se pasa se guarda tal cual en la columna `fqcnFirmaNotifier` de la fila (y el tipo del callback en `fqcnCallBackData`), así que la fila apunta a una clase que vive **bajo `tramites/**`**, justo el árbol que mueven las recetas de fase y de versionado.
>
> A diferencia del `PhaseEventManager` y del `StateEventValidator` —que se resuelven por `basePackageName` + `codePhase` y por eso mover la carpeta de un tipo se autocorrige (`SKILL.md` §1.6)—, aquí **no hay autocuración**: mover o renombrar la carpeta de la versión, o mover el `PhaseEventManagerImpl` de una fase a otra, deja las `TareaFirma` **pendientes** apuntando a un FQCN que ya no existe, y su callback revienta al completarse la firma.
>
> Mientras el notifier siga resolviéndose por FQCN: **MUST** comprobar, antes de mover una carpeta de versión o de fase que tenga firmas en marcha, si hay filas de `TareaFirma` pendientes con ese `fqcnFirmaNotifier`, y actualizarlas a mano.

### 6.7 Enviar correos (subsistema Correos)

Patrón previsto (aún sin uso real en ningún trámite): insertar un `Correo` vía su servicio — el alta ya programa el envío asíncrono, no hay que llamar a nada más.

```java
CorreoService correoService = (CorreoService) modelServiceFactory.resolve(Correo.class);
Correo correo = new Correo();
correo.setPara("destinatario@example.com");        // to; admite varios separados por comas
correo.setAsunto("...");
correo.setCuerpo("...");
correo.setCentro(eventContext.getCentro());
// opcionales: dniDestinatario/nombre/apellidos, enCopia, enCopiaOculta, adjuntos, historialEstado
correoService.insert(correo);
```

El correo es inmutable tras crearse y no se puede borrar; los reintentos de envío los gestiona el propio subsistema (cron `correos.envio.cron`).

### 6.8 Acceder a servicios de otros subsistemas

`modelServiceFactory.resolve(<Entidad>.class)` devuelve el `ModelService` del subsistema dueño (es el mecanismo usado en §6.6 y §6.7). Para dependencias que no son ModelService, inyección Guice normal (`@Inject` de campo o constructor; ver `k-guice` si la construcción no es trivial).

## 7. Los tests que comprueban el PhaseEventManager

Lo comprueban los tests de `src/test/java/com/educaflow/tiposexpedientes/` — `phaseeventmanager/PhaseEventManagerTest.java`, `phaseeventmanager/ApiBaseReservadaTest.java`, `initialeventmanager/InitialEventManagerTest.java` y `modelo/ModeloDelTipoTest.java` (`./gradlew test`), **no** el build de `generateCode`: hasta hace poco era un check con Spoon dentro de `createfiles`, y se movió a tests para separar la generación de esqueletos de su validación. Leen **bytecode**, así que el fallo aparece al ejecutar los tests, no al compilar.

Las reglas del `PhaseEventManager` se comprueban **fase a fase**: la unidad no es el tipo de expediente, sino cada una de sus fases, y el mensaje de error identifica la fase como `MiTramiteV1/RECEPCION`. Las del `InitialEventManager` (I1/I2), en cambio, van **por tipo de expediente**, que es su unidad natural.

1. **E0**: la clase `<paquete de la fase>.PhaseEventManagerImpl` existe compilada y extiende `PhaseEventManager`.
2. **E1**: por cada evento **de la fase** (la unión de los eventos de sus estados), **exactamente un** `@WhenEvent trigger<Evento>` con la firma exacta de §2. **E3**: ídem por cada estado **de la fase** con `@OnEnterState onEnter<Estado>`. El mensaje de fallo trae el **código del método listo para pegar**, renderizado con la misma plantilla que usa el generador.
3. **E2 / E4**: **no puede sobrar** ningún `@WhenEvent`/`@OnEnterState` que no corresponda a un evento/estado de la propia fase: si quitas un evento del XML, quita su método (y el del validator); si mueves un estado a otra fase, mueve también su `onEnter`.
4. **A1** (`ApiBaseReservadaTest`): ningún nombre de método compuesto a partir de un estado o un evento puede coincidir con un método público de `PhaseEventManager`, de `StateEventValidator` o de `InitialEventManager`. Un estado llamado `STATE` produciría un `onEnterState(<Entidad>, EventContext)` con la firma exacta del dispatcher de la clase base y lo sobrescribiría en silencio; y un evento `INITIAL_EVENT` produciría un `triggerInitialEvent` en el `PhaseEventManagerImpl` de su fase, que es justo lo que E5 prohíbe — por eso `InitialEventManager` cuenta como clase base a estos efectos aunque el `PhaseEventManagerImpl` no la implemente.
5. **E5**: **ningún** `PhaseEventManagerImpl` puede declarar un `triggerInitialEvent`. El evento inicial es del tipo de expediente, así que un método así no lo llama nadie: se quedaría ahí sin ejecutarse, y como la clase base ya no declara el método tampoco hay un `@Override` que falle al compilar. Es el fallo típico de un tipo a medio migrar.
6. **I1 / I2** (`InitialEventManagerTest`): por cada **tipo de expediente**, la clase `<basePackageName>.InitialEventManagerImpl` existe compilada e implementa `InitialEventManager` (**I1**), y declara **exactamente un** `void triggerInitialEvent(<Entidad>, EventContext)` (**I2**). `Tramitador` la resuelve por reflexión, así que olvidarla no es un error de compilación sino una excepción al crear el primer expediente; el mensaje del test trae el comando de `CreateFilesTask` que la genera.
7. **M1** (`modelo/ModeloDelTipoTest`): el `InitialEventManagerImpl` y el `PhaseEventManagerImpl` de **cada fase** declaran la **misma** entidad en su parámetro de tipo, y es la primera `<entity>` del `domains.xml` (`modelo.md` §1). Es lo que `ExpedienteLocator.getModelClass` lee en runtime para instanciar el expediente, así que una divergencia no la caza el compilador: se nota al tramitar.
8. Detalles:
   - Un método con nombre correcto pero **firma equivocada** se reporta **una sola vez**, en E1/E3, mostrando la firma declarada frente a la esperada (el antiguo check lo contaba a la vez como que faltaba y como que sobraba).
   - Solo cuentan los métodos **declarados en la propia clase de la fase**: un `trigger`/`onEnter` heredado de una superclase **no** cuenta ni en los tests (leen los métodos declarados del bytecode) ni en runtime (el dispatcher los busca con `getDeclaredMethods()` sobre la clase concreta, así que lo daría por ausente y reventaría al disparar el evento). Lo que sí aporta leer bytecode en vez del fuente, frente al check antiguo con Spoon, es alcanzar al validator en Kotlin y descartar los métodos sintéticos y puente.
   - El `triggerInitialEvent` del `InitialEventManagerImpl` no lleva anotación ni está en ninguna fase, así que no entra en E1/E2/E3/E4; lo cubren I1/I2.
   - El FQCN de la entidad que esperan E1/E3/I2 está hardcodeado como `com.educaflow.subsystem.expedientes.db.<code>` — otra razón para no tocar el `<module>` del `domains.xml`. M1 llega a esa misma entidad por otro camino, el `domains.xml`, así que las dos lecturas tienen que coincidir.

## 8. Anti-patrones

- **MUST NOT** olvidar `eventContext.updateState(...)` en un trigger que deba transitar (el expediente se quedaría en el estado actual).
- **MUST NOT** validar datos del usuario aquí: eso es del validator (que además es la whitelist de campos). Aquí solo lógica de negocio.
- **MUST NOT** usar `System.out`: logger slf4j (la regla C22 está congelada en `archunit_store` con la deuda de `base/infrastructure`; los tipos de expediente están limpios y **MUST** seguir así — una violación nueva rompe el build).
- **MUST NOT** llamar dos veces a `createRegistroEntrada`/`createRegistroSalida` en el mismo evento ("Ya existe un registro de entrada definido").
- **MUST NOT** nombrar un estado por sus strings (`"ENTRADA_DATOS"`, `codeState.equals(...)`): usa la constante `States.<Fase>.<ESTADO>` (fase en UpperCamelCase: `States.Recepcion.ENTRADA_DATOS`), que además compara con `==`.
- **MUST NOT** editar ni versionar la clase `States`: la reemite el build en cada compilación (`SKILL.md` §2.3).
- **MUST NOT** poner el `trigger`/`onEnter` de un estado en la clase de otra fase: el test lo detecta, pero además en runtime nunca se le llamaría.
- **MUST NOT** escribir la inicialización del expediente en un `PhaseEventManagerImpl`: va en el `InitialEventManagerImpl` del tipo (§2.1). Un `triggerInitialEvent` en una fase no lo llama nadie.
- **MUST NOT** factorizar los `trigger`/`onEnter` comunes a una superclase compartida entre fases o versiones: solo se ven los declarados en la clase de la fase (§7). Si hay lógica común, deja el método declarado en cada fase y que delegue en un helper o servicio.
