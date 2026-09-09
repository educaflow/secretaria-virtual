# Receta: el usuario presenta un documento (acaba en el registro de entrada)

El camino completo por el que unos datos que el usuario teclea en el expediente se convierten en un PDF, lo firma, lo presenta y queda **asentado en el registro de entrada** con su resguardo. Los ejemplos usan el trámite inventado `MiTramite` (`SKILL.md`); para ver uno de verdad, abre cualquier tipo bajo `src/main/java/com/educaflow/tramites/` que tenga `pdfJustificanteRegistroEntrada`.

El registro de entrada asienta el documento **firmado por el usuario** (`RegistroEntrada.documentoOriginalFirmado`): por eso la presentación pasa **siempre** por la firma de `recetas/firma.md` §1, que esta receta no repite sino que referencia en el paso que le toca.

| Paso | Qué pasa | Dónde | Sección |
|---|---|---|---|
| 0 | Dos estados de perfil `CREADOR`: uno para teclear los datos y otro para revisar el PDF, firmarlo y presentarlo | `TipoExpedienteInstance.xml` | §0 |
| 1 | Los campos: los datos, los anexos, el par PDF original/firmado y el resguardo | `domains.xml` | §1 |
| 2 | El expediente nace con solicitante e interesado, que son los que van al registro | `InitialEventManagerImpl` | §2 |
| 3 | El documento que se va a presentar, con hueco para la firma | `documentospdf/` | §3 |
| 4 | El usuario teclea los datos; el trigger genera el PDF | estado `ENTRADA_DATOS` | §4 |
| 5 | El usuario revisa el PDF, lo firma y presenta; el trigger asienta el registro y guarda el resguardo | estado `PENDIENTE_PRESENTACION` | §5 |
| 6 | Qué ve cada perfil después | estado siguiente | §6 |

Dependencias: el registro lo crea `EventContext` contra `subsystem/registroentradasalida` (`phaseeventmanager.md` §3); no hay que inyectar ni resolver nada del registro en el trámite.

## 0. La máquina de estados mínima

```xml
<fase name="RECEPCION" title="Recepción">
    <state name="ENTRADA_DATOS"          events="DELETE,GUARDAR_DATOS" profile="CREADOR" title="Entrada de datos"           initial="true"/>
    <state name="PENDIENTE_PRESENTACION" events="BACK,PRESENTAR"       profile="CREADOR" title="Pendiente de presentación"/>
</fase>
<fase name="TRAMITACION" title="Tramitación">
    <state name="PENDIENTE_RESOLUCION"   events="RESOLVER"             profile="RESPONSABLE" title="Pendiente de resolución"/>
    ...
</fase>
```

- Son **dos** estados y no uno porque el usuario **MUST** ver el PDF que va a firmar antes de firmarlo, y ese PDF lo genera el servidor a partir de los datos: primero se guardan los datos y se genera (`GUARDAR_DATOS`), después se firma y presenta (`PRESENTAR`).
  Además `FirmaPdf` compara el PDF firmado con el original persistido (`recetas/firma.md` §1.4), así que el original **MUST** existir en el expediente antes de firmar.
- `BACK` devuelve a `ENTRADA_DATOS` para corregir los datos: al volver a `GUARDAR_DATOS` el PDF se regenera y el ciclo empieza otra vez.
- `DELETE` solo en el primer estado: desde `PENDIENTE_PRESENTACION` se vuelve con `BACK` para borrar, y una vez presentado el expediente ya no se borra (§5.4).
- El estado destino de `PRESENTAR` está normalmente en **otra fase** (la de quien tramita); no hay nada especial que hacer para cruzarla (`SKILL.md` §1.2).

## 1. Modelo (`domains.xml`)

```xml
<entity name="MiTramiteV1" extends="Expediente">
    <!-- 1. Los datos que el usuario teclea y que van al documento -->
    <string name="dias"/>
    <enum name="motivo" ref="MotivoMiTramiteV1"/>
    <string name="otroMotivo"/>

    <!-- 2. Lo que el usuario adjunta: va como anexo del registro -->
    <many-to-one name="justificante" title="Foto o PDF del justificante" ref="com.axelor.meta.db.MetaFile"/>

    <!-- 3. El documento que presenta: original (lo genera GUARDAR_DATOS) y firmado (lo firma PRESENTAR) -->
    <many-to-one name="pdfSolicitud"        title="PDF de la solicitud" ref="com.axelor.meta.db.MetaFile"/>
    <many-to-one name="pdfSolicitudFirmado" title="PDF de la solicitud" ref="com.axelor.meta.db.MetaFile"/>

    <!-- 4. El resguardo sellado que devuelve el registro de entrada -->
    <many-to-one name="pdfJustificanteRegistroEntrada" title="Justificante del registro de entrada" ref="com.axelor.meta.db.MetaFile"/>
    ...
</entity>
```

1. **Datos**: los campos que el usuario rellena y que el documento estampa con `self.<campo>` (§3).
2. **Anexos**: un `MetaFile` por cada fichero que el usuario aporta. Pueden ser imágenes o PDF; el registro los admite todos como anexos.
3. **Par original/firmado**: es el mismo par de `recetas/firma.md` §1.2. El original lo genera el trigger de `GUARDAR_DATOS`; el firmado lo produce `PRESENTAR` (en servidor) o lo sube el usuario (AutoFirma).
4. **Resguardo**: el `MetaFile` que devuelve el registro de entrada; se guarda para enseñárselo al usuario (§6).
- `personaSolicitante`, `personaInteresada`, `numeroExpediente`, `centro` y `claveCertificado` ya están heredados de `Expediente` (`modelo.md` §2). **MUST NOT** redeclararlos: son justo los que `createRegistroEntrada` lee del expediente (§5.3).

## 2. Evento inicial (`InitialEventManagerImpl`)

```java
@Override
public void triggerInitialEvent(MiTramiteV1 exp, EventContext eventContext) throws BusinessException {
    Persona persona = new Persona();
    persona.setNombre(exp.getUsuarioRegistrador().getNombre());
    persona.setApellidos(exp.getUsuarioRegistrador().getApellidos());
    persona.setDni(exp.getUsuarioRegistrador().getDni());
    exp.setPersonaSolicitante(persona);
    exp.setPersonaInteresada(persona);
}
```

- **MUST** dejar rellenos `personaSolicitante` y `personaInteresada`: `createRegistroEntrada` compone con ellos el solicitante y el interesado del asiento (`nombre + " " + apellidos` y `dni`) y con `personaSolicitante` a `null` revienta con NPE (`phaseeventmanager.md` §2.1). `Tramitador` no lo comprueba: el fallo aparece al presentar, no al crear.
- Patrón habitual: los dos son el propio `usuarioRegistrador`. Si el interesado es otra persona (un familiar presenta por un alumno), rellena `personaInteresada` con sus datos en el estado que los pida y deja aquí solo el solicitante.

## 3. El documento (`documentospdf/solicitud.xml`)

- Un XML de definición con los datos del expediente como `nombreCampo="self.<campo>"` (`documentos.md`); el build genera `solicitud.pdf` y la constante `TipoDocumentoPdf.SOLICITUD` con la que el trigger lo pide (§4.3). Si hay impreso oficial, se versiona ese PDF tal cual en vez del XML.
- **MUST** dejar en el documento un hueco para la firma del usuario (una `<fila>` con `rowSpan` o un `<texto>` vacío al final). El recuadro `Rectangulo` y la página que uses en §5 son los de ese hueco, y **MUST** ser los mismos en la `<action-method>` de AutoFirma y en el trigger (`recetas/firma.md` §1.3 y §1.5).
- El registro de entrada **no** necesita nada del documento: la portada con el número de registro, la fecha y el sello la genera él y la antepone al PDF firmado (§5.3).

## 4. Estado `ENTRADA_DATOS`: teclear los datos y generar el PDF

### 4.1 Vista (`views.xml` de la fase)

```xml
<form state="ENTRADA_DATOS" profile="CREADOR">
    <include-panels>
        -datos-interesado
        datos-solicitud
        justificante-upload
    </include-panels>
    <footer>
        <buttons-left>
            <button name="DELETE" colSpan="2" css="btn-danger" outline="true" icon="trash" title="Borrar el expediente"
                    onClick="subsysExpedientes-event-action" prompt="¿Está seguro que desea borrar el expediente?"/>
        </buttons-left>
        <buttons-right>
            <button name="GUARDAR_DATOS" colSpan="2" title="Siguiente" onClick="subsysExpedientes-event-action"/>
        </buttons-right>
    </footer>
</form>
<form state="ENTRADA_DATOS">
    <include-panels>
        -datos-interesado
        -datos-solicitud
        -justificante-view
    </include-panels>
    <footer>
        <buttons-left/>
        <buttons-right>
            <button name="EXIT" colSpan="2" title="Salir" onClick="subsysExpedientes-event-action"/>
        </buttons-right>
    </footer>
</form>
```

- El form del `CREADOR` incluye editables los paneles de datos y el de subida del anexo (`widget="binary-link"`, `vistas.md` §6); el genérico los incluye todos con `-` y solo `EXIT`.
- Los paneles viven en el form plantilla de la raíz (`vistas.md` §1).

### 4.2 Validator (`StateEventValidatorImpl.kt` de la fase)

```kotlin
@BeanValidationRulesForStateAndEvent
fun getForStateEntradaDatosInEventGuardarDatos(): BeanValidationRules = rules {
    field(model::getDias) {
        +Required()
        +Pattern("^...$")
    }
    field(model::getMotivo) {
        +Required()
    }
    field(model::getOtroMotivo) {
        +ifValueIn(model::getMotivo, listOf(MotivoMiTramiteV1.OTROS)) {
            +Required()
        }
    }
    field(model::getJustificante) {
        +Required()
        +FileType(listOf("image/png", "image/jpeg", "application/pdf"))
        +FileMaxSize(5, SizeUnit.MB)
    }
}
```

- Aquí van **todos** los campos que el usuario teclea o sube en este estado, porque el `rules { }` es la whitelist del evento (`validator.md` §1): un campo sin `field(...)` no se copia del request.
- **MUST NOT** dar `field(...)` a `pdfSolicitud`, `pdfSolicitudFirmado` ni `pdfJustificanteRegistroEntrada`: los rellena el servidor (§4.3 y §5.3). Un `field` sobre ellos deja que el cliente dicte qué documento se registra.
- `DELETE` no necesita método (`validator.md` §5).

### 4.3 Trigger (`PhaseEventManagerImpl.java` de la fase)

```java
@WhenEvent
public void triggerGuardarDatos(MiTramiteV1 exp, MiTramiteV1 original, EventContext eventContext) throws BusinessException {
    DocumentoPdf solicitudPdf = exp.getDocumentoPdf(MiTramiteV1.TipoDocumentoPdf.SOLICITUD);
    exp.setPdfSolicitud(MetaFileHelper.createMetaFile(solicitudPdf));
    eventContext.updateState(States.Recepcion.PENDIENTE_PRESENTACION);
}

@WhenEvent
public void triggerDelete(MiTramiteV1 exp, MiTramiteV1 original, EventContext eventContext) throws BusinessException {
    // vacío: DELETE borra sin pasar por aquí, pero el test E1 exige el método
}
```

- Genera el PDF con los datos recién validados (`phaseeventmanager.md` §6.1) y transita. Nada más.
- **MUST NOT** crear aquí el registro de entrada: el documento aún no está firmado y el usuario aún no lo ha visto. Un registro asienta lo que el usuario presentó, no un borrador.
- Si el estado se alcanza también por `BACK`, se sobrescribe `pdfSolicitud` con el PDF nuevo; el firmado anterior, si lo hubiera, ya no vale y `FirmaPdf` lo rechazaría contra el nuevo original.

## 5. Estado `PENDIENTE_PRESENTACION`: revisar, firmar y presentar

### 5.1 Vista

Es, tal cual, la de `recetas/firma.md` §1.3: `-pdfSolicitud` (visor del PDF, `vistas.md` §9), el `onLoad` que rellena `situacionFirma`/`firmaEnServidor`, un panel por situación, `BACK` a la izquierda y los dos botones `PRESENTAR` a la derecha. El genérico incluye `-pdfSolicitud` y `EXIT`.

- **MUST** enseñar el PDF que se va a firmar (`-pdfSolicitud`), no los campos de datos: el usuario firma un documento y tiene que ver ese documento.
- Los dos `PRESENTAR` llevan `prompt` de confirmación: la presentación no se deshace (§5.4).

### 5.2 Validator

```kotlin
@BeanValidationRulesForStateAndEvent
fun getForStatePendientePresentacionInEventBack(): BeanValidationRules = rules { }

@BeanValidationRulesForStateAndEvent
fun getForStatePendientePresentacionInEventPresentar(): BeanValidationRules = rules {
    field(model::getClaveCertificado) {
        +ifSituacionFirma({ it.isFirmaEnServidor() }) {
            +ClaveCertificadoValida()
        }
    }
    field(model::getPdfSolicitudFirmado) {
        +ifSituacionFirma({ !it.isFirmaEnServidor() }) {
            +Required()
            +FirmaPdf(model::getPdfSolicitud)
        }
    }
}
```

- `PRESENTAR` es exactamente `recetas/firma.md` §1.4: dos `field(...)`, dos ramas complementarias. Nada más entra en la whitelist: los datos ya se validaron en `GUARDAR_DATOS` y en este estado no se editan.
- `BACK` con `rules { }` vacío: no lleva datos, pero **MUST** existir (`validator.md` §1).

### 5.3 Trigger `PRESENTAR`

```java
private static final Rectangulo POSICION_FIRMA_SOLICITUD = new Rectangulo(100, 20, 600, 100);
private static final int PAGINA_FIRMA_SOLICITUD = 1;

@Inject
FirmaServidorHelper firmaServidorHelper;

@WhenEvent
public void triggerPresentar(MiTramiteV1 exp, MiTramiteV1 original, EventContext eventContext) throws BusinessException {
    String dniFirmante = SecurityUtil.getUser().getDni();
    SituacionFirma situacionFirma = CertificadoDigitalHelper.getSituacionFirmaByDni(dniFirmante);

    try {
        // 1. Firma (recetas/firma.md §1.5): solo hay código para la rama de servidor
        if (situacionFirma.isFirmaEnServidor()) {
            exp.setPdfSolicitudFirmado(firmaServidorHelper.firmarEnServidor(
                    dniFirmante, situacionFirma, exp.getClaveCertificado(),
                    exp.getPdfSolicitud(), POSICION_FIRMA_SOLICITUD, PAGINA_FIRMA_SOLICITUD));
        }

        // 2. Registro de entrada: el documento firmado + los anexos que aportó el usuario
        RegistroEntrada registroEntrada = eventContext.createRegistroEntrada(exp.getPdfSolicitudFirmado(), List.of(exp.getJustificante()));

        // 3. El resguardo sellado, para enseñárselo al usuario
        exp.setPdfJustificanteRegistroEntrada(registroEntrada.getDocumentoResguardoPresentacion());

        // 4. Transición, normalmente a la fase de quien tramita
        eventContext.updateState(States.Tramitacion.PENDIENTE_RESOLUCION);
    } finally {
        exp.setClaveCertificado(null);
    }
}

@WhenEvent
public void triggerBack(MiTramiteV1 exp, MiTramiteV1 original, EventContext eventContext) throws BusinessException {
    exp.setClaveCertificado(null);
    eventContext.updateState(States.Recepcion.ENTRADA_DATOS);
}
```

1. **Firma**: la rama de servidor de `recetas/firma.md` §1.5. Con AutoFirma el `pdfSolicitudFirmado` ya llegó validado y no hay nada que hacer; el resto del trigger es igual en los dos casos.
2. **`createRegistroEntrada(documento, anexos)`** (`phaseeventmanager.md` §3): el primer argumento es el PDF **firmado**; el segundo, los ficheros que el usuario aportó. Lo que hace el subsistema de registro, sin que el trámite tenga que saberlo:
   - Numera el asiento por centro y año (`nnnnn/aaaa`) y le pone fecha.
   - Toma del expediente el solicitante, el interesado (§2), el `numeroExpediente` y el asunto `"Expediente: <numeroExpediente> - <name>"`.
   - Compone el **resguardo de presentación**: una portada con los datos del asiento seguida del documento firmado, y lo firma con el certificado del **secretario** del centro.
   - Guarda en el asiento el documento firmado (renombrado a `solicitud_expediente_<numero>.pdf`), el resguardo (`resguardo_solicitud_expediente_<numero>.pdf`) y los anexos, que **clona**.
3. **Resguardo**: `getDocumentoResguardoPresentacion()` ya es un `MetaFile`; se guarda en el campo de la entidad para el visor de §6.
4. **Transición** al estado de quien tramita.
5. `claveCertificado` a `null` en un `finally`, y también en `triggerBack` (`recetas/firma.md` §1.5).

Restricciones de `createRegistroEntrada`:

- **LIMIT**: **uno** por evento; la segunda llamada revienta con "Ya existe un registro de entrada definido". Si un evento registra dos documentos, se concatenan antes en un solo PDF (`DocumentoPdf.anyadirDocumentoPdf`, `phaseeventmanager.md` §6.1) o van como anexos.
- El documento **MUST** ser un PDF no nulo (`IllegalArgumentException` si no lo es). Los anexos admiten cualquier tipo (una foto del justificante), pero cada uno **MUST** tener `fileName`; la lista puede ser `null` o vacía.
- No hay `BusinessException` que capturar: sus fallos son de programación (solicitante sin rellenar, documento que no es PDF, doble llamada), no del usuario.

- ✅ CORRECTO: `eventContext.createRegistroEntrada(exp.getPdfSolicitudFirmado(), List.of(exp.getJustificante()))`
- ✅ CORRECTO: `eventContext.createRegistroEntrada(exp.getPdfSolicitudFirmado(), null)` (sin anexos)
- ❌ INCORRECTO: `eventContext.createRegistroEntrada(exp.getPdfSolicitud(), ...)` (registra el original sin firmar; el asiento debe llevar lo que el usuario firmó)
- ❌ INCORRECTO: `createRegistroEntrada(...)` en `triggerGuardarDatos` (aún no hay firma ni el usuario ha visto el documento)
- ❌ INCORRECTO: `exp.setPdfJustificanteRegistroEntrada(registroEntrada.getDocumentoOriginalFirmado())` (eso es el documento del usuario, no el resguardo sellado)
- ❌ INCORRECTO: resolver `RegistroEntradaService` con `modelServiceFactory` desde el trigger (ya lo hace `EventContext`, que además es quien rellena solicitante, interesado y asunto)

### 5.4 Por qué la presentación no tiene vuelta atrás

Una vez asentado, el registro de entrada tiene número y fecha oficiales y queda en el historial del expediente y en el registro del centro; el expediente no puede "despresentarse".
Si el tramitador necesita que el usuario corrija algo, lo devuelve a `ENTRADA_DATOS` con un evento suyo (una resolución de subsanación) y el usuario vuelve a recorrer §4 y §5: el siguiente `PRESENTAR` crea **otro** asiento, y los dos quedan en el historial.

## 6. Después de presentar: qué ve cada perfil

En el estado destino, cada form incluye el PDF que le corresponde (los paneles-visor están en el form plantilla de la raíz, uno por campo, `vistas.md` §9):

```xml
<!-- <vN>/tramitacion/views.xml -->
<form state="PENDIENTE_RESOLUCION" profile="RESPONSABLE">
    <include-panels>
        -datos-interesado
        -datos-solicitud-view
        -pdfSolicitudFirmado        <!-- lo que el usuario firmó -->
        resolucion
    </include-panels>
    ...
</form>
<form state="PENDIENTE_RESOLUCION">
    <include-panels>
        -datos-interesado
        -datos-solicitud-view
        -pdfJustificanteRegistroEntrada   <!-- su resguardo sellado, con número de registro -->
    </include-panels>
    <footer>
        <buttons-left/>
        <buttons-right>
            <button name="EXIT" colSpan="2" title="Salir" onClick="subsysExpedientes-event-action"/>
        </buttons-right>
    </footer>
</form>
```

- El **`RESPONSABLE`** ve `pdfSolicitudFirmado`: el documento tal cual lo firmó el usuario, que es sobre lo que resuelve.
- El **`CREADOR`** cae en el form genérico (el estado ya no es suyo) y ve `pdfJustificanteRegistroEntrada`: el resguardo con el número de registro, que es su prueba de haber presentado.
- La cabecera global ya enseña gratis el historial de estados con los registros de entrada y salida de cada uno (`vistas.md` §3): no hace falta declarar nada más para que el asiento aparezca.

## 7. Checklist

- [ ] Máquina: dos estados `CREADOR` (`ENTRADA_DATOS` con `GUARDAR_DATOS`, `PENDIENTE_PRESENTACION` con `BACK` y `PRESENTAR`); `PRESENTAR` transita al estado de quien tramita.
- [ ] Modelo: los campos de datos, los anexos, el par `pdfSolicitud`/`pdfSolicitudFirmado` y `pdfJustificanteRegistroEntrada`; sin redeclarar `personaSolicitante`/`personaInteresada`/`claveCertificado`.
- [ ] Evento inicial: `personaSolicitante` y `personaInteresada` rellenos.
- [ ] Documento: XML (o impreso oficial) en `documentospdf/` con hueco para la firma; mismo `Rectangulo` y página en la `<action-method>` y en el trigger.
- [ ] `ENTRADA_DATOS`: validator con **todos** los campos tecleados y **ninguno** de los PDF; trigger que genera `pdfSolicitud` y transita, sin registrar.
- [ ] `PENDIENTE_PRESENTACION`: vista, validator y firma de `recetas/firma.md` §1; trigger que firma si toca, `createRegistroEntrada(firmado, anexos)`, guarda el resguardo, transita, y vacía la clave en `finally`; `triggerBack` vacía la clave.
- [ ] Estado destino: el `RESPONSABLE` ve `-pdfSolicitudFirmado`; el genérico, `-pdfJustificanteRegistroEntrada`.

## 8. Anti-patrones

- **MUST NOT** registrar en `GUARDAR_DATOS` ni registrar `pdfSolicitud`: el asiento lleva el documento **firmado**, y la firma ocurre en `PRESENTAR`.
- **MUST NOT** dar `field(...)` en el validator a los PDF que rellena el servidor (`pdfSolicitud`, `pdfJustificanteRegistroEntrada`): abriría la puerta a que el cliente dicte qué se registra (`k-secure-coding`).
- **MUST NOT** llamar dos veces a `createRegistroEntrada` en el mismo evento; concatena o anexa.
- **MUST NOT** dejar `personaSolicitante` sin rellenar en el evento inicial: el NPE sale al presentar, en otro sitio y otro momento.
- **MUST NOT** guardar `getDocumentoOriginalFirmado()` como resguardo: el resguardo es `getDocumentoResguardoPresentacion()`.
- **MUST NOT** hablar con `subsystem/registroentradasalida` desde el trámite: la única puerta es `eventContext.createRegistroEntrada`.
- **MUST NOT** saltarse la firma "porque este trámite no la necesita": el registro asienta un documento firmado, y firmar no cuesta nada al trámite (`recetas/firma.md` §1 decide en servidor o AutoFirma según el certificado del usuario).
