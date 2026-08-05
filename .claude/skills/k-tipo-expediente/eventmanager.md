# El `EventManager` — la máquina de estados en Java

Clase Java que decide qué pasa en cada evento y a qué estado se transita. El esqueleto lo genera `./gradlew CreateFilesTask` (`SKILL.md` §3.1) con todos los métodos requeridos vacíos y los tres enums ya codificados; compilar **no** lo genera. Ejemplo real completo: `tramites/justificacion_falta_profesorado/v1/EventManagerImpl.java`.

## 1. Anatomía

```java
public class EventManagerImpl
        extends EventManager<JustificacionFaltaProfesoradoV1, EventManagerImpl.State, EventManagerImpl.Event, EventManagerImpl.Profile> {

    private final JustificacionFaltaProfesoradoV1Repository repository;

    @Inject
    AlmacenClaveResolver almacenClaveResolver;      // firma en servidor (§6.4)
    @Inject
    private ModelServiceFactory modelServiceFactory; // servicios de otros subsistemas (§6.8)

    @Inject
    public EventManagerImpl(JustificacionFaltaProfesoradoV1Repository repository) {
        super(JustificacionFaltaProfesoradoV1.class, State.class, Event.class, Profile.class);
        this.repository = repository;
    }
    // ... triggers, onEnters y los enums State/Event/Profile generados
}
```

- Se instancia por **reflexión + Guice** (el FQCN viene de la tabla `TipoExpediente`), así que admite inyección normal: repositorio por constructor y `@Inject` de campo para lo demás.
- Los enums `State`/`Event`/`Profile` salen del `TipoExpedienteInstance.xml`, pero **solo la primera vez**, al generar el esqueleto. El `State` es "rico" (perfil dueño, `initial`, `closed`, eventos permitidos) y es **la única fuente de la máquina de estados en runtime**. Si cambias estados/eventos en el XML **NO se regeneran**: los enums están en TU fichero y el generador nunca lo pisa, así que hay que actualizarlos a mano — los errores de los tests (§7) te dan el código a copiar.

## 2. Los tres tipos de método (convención de nombres)

| Método | Cuándo se invoca | Firma exacta |
|---|---|---|
| `triggerInitialEvent` | Al **crear** el expediente | `void triggerInitialEvent(<Entidad>, EventContext<Profile,State>) throws BusinessException` (abstract en la base, sin anotación) |
| `trigger<EventoEnUpperCamel>` | Al disparar el evento | `@WhenEvent void trigger<Evento>(<Entidad> actual, <Entidad> original, EventContext<Profile,State>) throws BusinessException` |
| `onEnter<EstadoEnUpperCamel>` | Al **entrar** en el estado (tras el trigger) | `@OnEnterState void onEnter<Estado>(<Entidad>, EventContext<Profile,State>)` |

Obligaciones de `triggerInitialEvent` (antes de llamarlo, `Tramitador` ya rellenó tipo, centro, `usuarioRegistrador`, `name` y `numeroExpediente`):

1. **MUST** dejar relleno `dniFirmaDocumentoEntrada` con un DNI válido (lo exige `Tramitador` con `Preconditions`).
2. **MUST** dejar rellenos `personaSolicitante` y `personaInteresada` (si `personaSolicitante` es null, `createRegistroEntrada` revienta con NPE). Patrón habitual: construir la `Persona` desde `getUsuarioRegistrador()`.
3. Inicializar el resto de campos con valor por defecto (año, etc.).

En cada `trigger<Evento>`: la lógica de negocio y **la decisión del estado destino** con `eventContext.updateState(State.XXX)` (puede depender de los datos, §5). Los `onEnter<Estado>` pueden quedarse vacíos pero **MUST** existir todos.

## 3. API de `EventContext`

| Método | Qué hace |
|---|---|
| `updateState(State)` | Fija el estado destino de la transición |
| `getProfile()` | Perfil con el que actúa el usuario |
| `getCentro()` | Centro activo |
| `createRegistroEntrada(MetaFile documentoPdf, List<MetaFile> anexos)` | Crea el registro de entrada (§6.2) y lo devuelve. **LIMIT**: uno por evento |
| `createRegistroSalida(MetaFile documentoPdf, List<MetaFile> anexos)` | Crea el registro de salida (§6.3) y lo devuelve. **LIMIT**: uno por evento |
| `getRegistroEntrada()` / `getRegistroSalida()` | Recuperan el registro creado en este evento |

Los registros toman solicitante/interesado, número y asunto del propio expediente; los anexos se clonan y **MUST** tener `fileName` no nulo (se admite `null` como lista).

## 4. Eventos comunes

- `EXIT` (cerrar pestaña): lo intercepta `ExpedienteController`, **no llega al EventManager**.
- `DELETE`: no valida ni copia campos, hace `repository.remove` directamente — pero **MUST** existir su `@WhenEvent triggerDelete` (vacío) si el XML lo declara, porque el test E1 lo exige. Su método en el validator, en cambio, **no** hace falta (`validator.md` §5).
- `BACK` **no es común**: se declara en `events="..."`, se implementa (§5) y se le da método en el validator.

## 5. Patrones de transición

**Condicional por datos** (un evento, varios destinos):

```java
switch (expediente.getTipoResolucion()) {
    case ACEPTAR ->        eventContext.updateState(State.ACEPTADO);
    case RECHAZAR ->       eventContext.updateState(State.RECHAZADO);
    case SUBSANAR_DATOS -> eventContext.updateState(State.ENTRADA_DATOS);
    default -> throw new IllegalArgumentException("Tipo de resolución no reconocido");
}
```

**Evento multi-origen** (el mismo evento declarado en varios estados decide según el estado actual):

```java
State actual = State.valueOf(expediente.getCodeState());
switch (actual) {
    case PENDIENTE_PRESENTACION -> eventContext.updateState(State.ENTRADA_DATOS);
    ...
}
```

## 6. Catálogo de acciones de un evento

Catálogo **abierto**: aún no están definidas todas las posibilidades — cuando aparezca una capacidad nueva (notificaciones push, integración con otro subsistema…), añádela aquí como un apartado más con su patrón.

### 6.1 Generar un documento PDF y guardarlo en la entidad

```java
DocumentoPdf solicitudPdf = expediente.getDocumentoPdf(JustificacionFaltaProfesoradoV1.TipoDocumentoPdf.SOLICITUD);
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

En el EventManager no hay código de AutoFirma: el trigger del evento ya recibe el campo firmado validado.

### 6.6 Poner documentos a firmar a otros usuarios (subsistema Firmas)

Estilo "portafirmas": crear una `TareaFirma` para que otro usuario firme, con callback cuando lo haga.

```java
public class EventManagerImpl extends EventManager<...> implements TareaFirmaNotifier {
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

Nota: el uso en `EventManagerImpl` de v1 está marcado como "solo una prueba" — el patrón es este, pero confirma el caso de uso antes de copiarlo.

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

## 7. Los tests que comprueban el EventManager

Lo comprueban los tests `src/test/java/com/educaflow/tiposexpedientes/eventmanager/EventManagerTest.java` (`./gradlew test`), **no** el build de `generateCode`: hasta hace poco era un check con Spoon dentro de `createfiles`, y se movió a tests para separar la generación de esqueletos de su validación. Leen **bytecode**, así que el fallo aparece al ejecutar los tests, no al compilar.

1. **E0**: la clase `<fqcnEventManager>` existe compilada y extiende `EventManager`.
2. **E1**: por cada evento, **exactamente un** `@WhenEvent trigger<Evento>` con la firma exacta de §2. **E3**: ídem por cada estado con `@OnEnterState onEnter<Estado>`. El mensaje de fallo trae el **código del método listo para pegar**, renderizado con la misma plantilla que usa el generador.
3. **E2 / E4**: **no puede sobrar** ningún `@WhenEvent`/`@OnEnterState` que no corresponda a un evento/estado declarado: si quitas un evento del XML, quita su método (y el del validator).
4. Detalles:
   - Un método con nombre correcto pero **firma equivocada** se reporta **una sola vez**, en E1/E3, mostrando la firma declarada frente a la esperada (el antiguo check lo contaba a la vez como que faltaba y como que sobraba).
   - Se lee la clase compilada, así que los **métodos heredados** de una superclase también cuentan; el check antiguo, que parseaba solo tu fichero, los daba por ausentes.
   - `triggerInitialEvent` no entra en ninguna regla (no lleva anotación; lo fuerza el compilador por ser abstract).
   - El FQCN de la entidad está hardcodeado como `com.educaflow.subsystem.expedientes.db.<code>` — otra razón para no tocar el `<module>` del `domains.xml`.

## 8. Anti-patrones

- **MUST NOT** olvidar `eventContext.updateState(...)` en un trigger que deba transitar (el expediente se quedaría en el estado actual).
- **MUST NOT** validar datos del usuario aquí: eso es del validator (que además es la whitelist de campos). Aquí solo lógica de negocio.
- **MUST NOT** usar `System.out`: logger slf4j (hay una violación congelada en `archunit_store` por el código de prueba de v1; no la repliques).
- **MUST NOT** llamar dos veces a `createRegistroEntrada`/`createRegistroSalida` en el mismo evento ("Ya existe un registro de entrada definido").
