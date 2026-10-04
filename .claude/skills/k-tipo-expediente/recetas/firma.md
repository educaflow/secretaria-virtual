# Receta: firmar documentos en un tipo de expediente

Las tres formas de que un documento de un expediente acabe firmado, cada una con todas sus piezas en orden (modelo, vista, validator, trigger). Los ejemplos usan el trámite inventado `MiTramite` (`SKILL.md`); para ver uno de verdad, abre cualquier tipo bajo `src/main/java/com/educaflow/tramites/` que tenga `pdfSolicitudFirmada`.

| Forma | Quién firma | Dónde | Sección |
|---|---|---|---|
| El usuario firma al presentar | El usuario autenticado, con su certificado custodiado (en servidor) o con AutoFirma | Modelo + vista + validator + trigger | §1 |
| El centro firma un documento que emite | El certificado del centro (director, secretario…) | Solo el trigger | §2 |
| Poner un documento a firmar a otro usuario | Otro usuario, desde su bandeja de firmas, cuando quiera | Máquina de estados + modelo + trigger + callback + eventos de sistema | §3 |

Dónde cae la firma dentro del PDF es lo mismo en las tres: §4.

Dependencias: §1 y §2 firman con `subsystem/criptografia` (a través de `tramites/util/firma` en §1). **MUST NOT** depender de `subsystem/firmas` para firmar en §1 ni en §2: ese subsistema es la bandeja tipo portafirmas de §3 y podría desaparecer.

## 1. El usuario firma al presentar (en servidor o con AutoFirma)

### 1.1 Cómo funciona

- Quien firma es siempre el **usuario autenticado**. El DNI se lee de él con `SecurityUtil.getUser()`; nunca del bean ni del formulario.
- Si firma **en el servidor** (tiene un certificado custodiado) o **con AutoFirma** (lo firma en su equipo y sube el PDF) lo decide su `SituacionFirma` (`subsystem/criptografia`), **no el trámite**. El servidor la recalcula del DNI cada vez que la necesita con `CertificadoDigitalService.getSituacionFirmaByDni(dni)`.
- `SituacionFirma.isFirmaEnServidor()` es **la** definición de «corresponde firmar en servidor». **MUST NOT** enumerar valores del enum en ningún sitio (validator, vista, trigger): una situación nueva se decide en el enum y todo lo demás la sigue.
- Con firma en servidor, el usuario teclea la clave de su certificado (PIN del dispositivo o contraseña del fichero) en `claveCertificado`, y el trigger firma con ella. Con AutoFirma, el usuario sube el PDF firmado y el validator comprueba la firma.
- El código común vive en `tramites/util/firma/`: `FirmaServidorRules.kt` (reglas del validator) y `FirmaServidorHelper` (firmar en el trigger).
  Lo que la vista pregunta en el `onLoad` no es de los trámites: lo da `CertificadoDigitalController` del subsistema `criptografia`.
- La firma de **la solicitud** en la fase común `ENTRADA` (`recetas/presentacion.md` §5) ya la tiene montada lo común: el panel `firma-solicitud` de `tramites/shared/template-views.xml` (§1.3) y `EntradaHelper.firmarSolicitudSiEsEnServidor` (§1.5). Esta sección explica cómo funciona por dentro y qué escribe cada tipo.

### 1.2 Documento y modelo (`documentospdf/` y `domains.xml`)

- El documento que se firma marca el hueco de la firma con `campoFirma="firmaSolicitante"` (`documentos.md` §2.10). Ese nombre es el que usan después la vista (§1.3) y el trigger (§1.5).

- Par de campos `MetaFile` original/firmado. El original lo genera el trigger anterior (`phaseeventmanager.md` §6.1); el firmado lo sube el usuario (AutoFirma) o lo genera el trigger que presenta (servidor).
  ```xml
  <many-to-one name="pdfSolicitud" title="PDF de la solicitud" ref="com.axelor.meta.db.MetaFile" />
  <many-to-one name="pdfSolicitudFirmada" title="PDF de la solicitud firmada" ref="com.axelor.meta.db.MetaFile" />
  ```
  Para la solicitud **MUST** llamarse exactamente así: son campos comunes (`SKILL.md` §1.2).
- `claveCertificado` **ya está heredado** de `Expediente` (`modelo.md` §2): transient, `password="true"`, nunca se persiste ni se devuelve al cliente. **MUST NOT** redeclararlo.
- La situación de firma **MUST NOT** ser un campo de la entidad: es un campo de vista (§1.3). El servidor no la lee nunca del formulario.

### 1.3 Vista (`views.xml` de la fase)

Los paneles son comunes: el visor `pdfSolicitud` y el panel `firma-solicitud` de `tramites/shared/template-views.xml` (`vistas.md` §3.2). Lo que escribe cada tipo, en el `views.xml` de la fase, son el form con sus botones y las acciones con prefijo `exp-<Code>-`: sus nombres son globales, y ponerlas junto a su botón hace que copiar la fase se lo lleve todo.

```xml
<form state="PENDIENTE_PRESENTACION" profile="CREADOR" onLoad="exp-MiTramiteV1-PENDIENTE_PRESENTACION-onLoad-action">
    <include-panels>
        -pdfSolicitud
        firma-solicitud
    </include-panels>

    <!-- Dos botones del mismo evento; se muestra uno u otro -->
    <footer>
        <buttons-left>
            <button name="BACK" colSpan="2" outline="true" title="Atrás" onClick="subsysTramitador-event-action"/>
        </buttons-left>
        <buttons-right>
            <button name="PRESENTAR" colSpan="4" title="Firmar con AutoFirma__!! y Presentar la solicitud"
                    showIf="situacionFirma=='SIN_CERTIFICADO'"
                    onClick="serial:exp-MiTramiteV1-firmarDocumentacionParaPresentar-action,subsysTramitador-event-action"
                    prompt="¿Esta seguro que desea presentar la documentación?&lt;br&gt;No podrá deshacer esta acción"/>
            <button name="PRESENTAR" colSpan="4" title="Firmar y Presentar la solicitud"
                    showIf="firmaEnServidor"
                    onClick="serial:subsysTramitador-event-action,exp-MiTramiteV1-set-claveCertificado-null-action"
                    prompt="¿Esta seguro que desea presentar la documentación?&lt;br&gt;No podrá deshacer esta acción"/>
        </buttons-right>
    </footer>
</form>

<action-group name="exp-MiTramiteV1-PENDIENTE_PRESENTACION-onLoad-action">
    <action name="exp-MiTramiteV1-set-situacionFirma-action"/>
</action-group>

<action-record name="exp-MiTramiteV1-set-situacionFirma-action" model="com.educaflow.subsystem.expedientes.db.MiTramiteV1">
    <field name="situacionFirma" expr="call:com.educaflow.subsystem.criptografia.controller.CertificadoDigitalController:getSituacionFirma()"/>
    <field name="firmaEnServidor" expr="call:com.educaflow.subsystem.criptografia.controller.CertificadoDigitalController:isFirmaEnServidor()"/>
</action-record>

<action-method name="exp-MiTramiteV1-firmarDocumentacionParaPresentar-action">
    <call class="com.educaflow.tramites.util.firma.FirmaClienteController"
          method='firmarDocumentoEnCampo(id,"pdfSolicitud","pdfSolicitudFirmada","firmaSolicitante")'/>
</action-method>

<action-record name="exp-MiTramiteV1-set-claveCertificado-null-action" model="com.educaflow.subsystem.expedientes.db.MiTramiteV1">
    <field name="claveCertificado" expr="eval: null"/>
</action-record>
```

1. **El panel común `firma-solicitud`** trae lo que antes declaraba cada tipo inline:
   - Los campos de vista `situacionFirma` (string) y `firmaEnServidor` (boolean): llevan `type=` porque no existen en la entidad. Solo viajan del servidor al cliente para decidir qué se pinta.
   - Un panel por situación con `showIf="situacionFirma=='X'"`: ahí sí se compara con el valor concreto, porque el texto de ayuda y si se pide PIN o contraseña dependen de cada uno. El `<field name="claveCertificado" widget="password">` solo aparece en `DISPOSITIVO_SIN_PIN` (título "PIN") y `FICHERO_SIN_CLAVE` (título "Contraseña"). `SIN_DNI` lleva un `<help variant="warning">`.
   - **MUST** incluirse **sin guion**: con `-firma-solicitud`, `claveCertificado` quedaría de solo lectura y no se podría firmar en servidor.
2. **`onLoad` del form** → `action-group` → `action-record` que rellena esos dos campos con `call:` a `CertificadoDigitalController`: `getSituacionFirma()` da el nombre del enum e `isFirmaEnServidor()` el boolean. Es del tipo porque una `action-record` lleva `model`.
3. **Dos botones `PRESENTAR`**, mismo `name` (mismo evento) y `showIf` excluyentes; con `SIN_DNI` no se muestra ninguno:
   - AutoFirma: `showIf="situacionFirma=='SIN_CERTIFICADO'"` y `serial:` con la `action-method` que llama a `FirmaClienteController.firmarDocumentoEnCampo(...)` **antes** del evento.
   - Servidor: `showIf="firmaEnServidor"` y `serial:` con el evento **y después** la `action-record` que pone `claveCertificado` a `null`, para que la clave no se quede en el formulario.
4. `firmarDocumentoEnCampo(id, campoOrigen, campoDestino, nombreCampoFirma)` lanza AutoFirma sobre el `MetaFile` del campo origen, deja el firmado en el destino y exige firmar con el DNI del usuario autenticado (revienta con `RuntimeException` si no tiene DNI válido). El nombre del campo de firma **MUST** ser el mismo que use el trigger (§1.5), para que la firma caiga en el mismo sitio se firme donde se firme.
5. Para firmar así **otro** documento que no sea la solicitud, el tipo declara en su form plantilla un panel propio con el mismo patrón que `firma-solicitud` (míralo en `tramites/shared/template-views.xml`) y su propio par de campos.

- ✅ CORRECTO: `firma-solicitud` en el `<include-panels>` (sin guion)
- ❌ INCORRECTO: `-firma-solicitud` (la clave del certificado queda de solo lectura)
- ❌ INCORRECTO: declarar inline en el form de cada tipo los paneles `firmaSolicitudSinCertificado`, `firmaSolicitudFicheroSinClave`… (ya están en el panel común)
- ✅ CORRECTO: `showIf="firmaEnServidor"` en el botón de firma en servidor
- ❌ INCORRECTO: `showIf="situacionFirma=='DISPOSITIVO_CON_PIN' || situacionFirma=='DISPOSITIVO_SIN_PIN' || ..."` (enumera la clasificación del enum; una situación nueva se quedaría sin botón)
- ❌ INCORRECTO: `showIf="!firmaEnServidor"` en el botón de AutoFirma (se lo mostraría también a quien no tiene DNI)
- ❌ INCORRECTO: `<string name="situacionFirma" .../>` en el `domains.xml` (es un campo de vista; el servidor lo recalcula del DNI)
- ❌ INCORRECTO: botón de servidor con `onClick="subsysTramitador-event-action"` a secas (la clave se queda en el formulario tras presentar)

### 1.4 Validator (`StateEventValidatorImpl.kt` de la fase)

El evento que presenta declara **siempre las dos ramas**, una por campo:

```kotlin
import com.educaflow.tramites.util.firma.ClaveCertificadoValida
import com.educaflow.tramites.util.firma.ifSituacionFirma
...
@BeanValidationRulesForStateAndEvent
fun getForStatePendientePresentacionInEventPresentar(): BeanValidationRules = rules {
    field(model::getClaveCertificado) {
        +ifSituacionFirma({ it.isFirmaEnServidor() }) {
            +ClaveCertificadoValida()
        }
    }
    field(model::getPdfSolicitudFirmada) {
        +ifSituacionFirma({ !it.isFirmaEnServidor() }) {
            +Required()
            +FirmaPdf(model::getPdfSolicitud)
        }
    }
}
```

- Los dos `field(...)` **MUST** estar siempre, aunque el trámite «solo vaya a usar» un camino: son la whitelist del evento (`validator.md` §1) y la rama que aplica depende del usuario que presente, no del trámite.
- La condición de `ifSituacionFirma` se escribe con los métodos de `SituacionFirma`. Las dos ramas **MUST** ser complementarias (`it.isFirmaEnServidor()` / `!it.isFirmaEnServidor()`) para que toda situación caiga en una; un usuario sin DNI cae en la de AutoFirma y `FirmaPdf` la rechaza.
- `ClaveCertificadoValida` va **solo** dentro de la rama de servidor: si la situación pide PIN o contraseña exige que venga, y en todo caso comprueba que la clave abre el almacén del certificado. Fuera de esa rama revienta con `IllegalStateException` (error de programación, no del usuario).
- `FirmaPdf(original)` va **solo** dentro de la rama de AutoFirma, junto con `Required()`. Comprueba: exactamente una firma nueva, certificado en la lista de confiables, que no es sello de tiempo, texto plano del PDF idéntico al original y DNI del certificado igual al del usuario autenticado. Si el usuario no tiene DNI válido **falla** aunque el campo venga vacío.
- Las reglas **no** vuelven a preguntar la situación por dentro para decidir si actúan: eso lo hace la rama. **MUST NOT** escribir una regla de firma que «se calle» según la situación confiando en que otra tape el caso.
- Fuente de verdad: `tramites/util/firma/FirmaServidorRules.kt` (su cabecera trae este mismo ejemplo) y `FirmaPdf` en `base/.../validation/rules/PdfRules.kt`.

- ✅ CORRECTO: `+ifSituacionFirma({ it.isFirmaEnServidor() }) { +ClaveCertificadoValida() }`
- ❌ INCORRECTO: `+ifSituacionFirma({ it == SituacionFirma.SIN_CERTIFICADO }) { +Required(); +FirmaPdf(...) }` (enumera un valor del enum y deja sin rama a `SIN_DNI`)
- ❌ INCORRECTO: `field(model::getPdfSolicitudFirmada) { +Required(); +FirmaPdf(model::getPdfSolicitud) }` sin rama (obliga a subir el PDF también a quien firma en servidor, donde lo genera el trigger)
- ❌ INCORRECTO: `field(model::getClaveCertificado) { +ClaveCertificadoValida() }` sin rama (revienta con `IllegalStateException` para quien firma con AutoFirma)
- ❌ INCORRECTO: declarar solo el `field` del PDF «porque este trámite es de AutoFirma» (el camino lo decide el certificado del usuario; sin el otro `field`, la clave no entra en la whitelist y la firma en servidor falla)

### 1.5 Trigger (`PhaseEventManagerImpl.java` de la fase)

```java
/** El campoFirma del hueco de la firma en documentospdf/solicitud.xml: MUST ser el mismo que la <action-method>
 *  de AutoFirma pasa a firmarDocumentoEnCampo (§1.3), para que la firma caiga en el mismo sitio se firme donde se firme. */
private static final String CAMPO_FIRMA_SOLICITUD = "firmaSolicitante";

@Inject
EntradaHelper entradaHelper;

@WhenEvent
public void triggerPresentar(MiTramiteV1 exp, MiTramiteV1 original, EventContext eventContext) throws BusinessException {
    try {
        EntradaHelper.exigePresentadoEnPapel(original, false);
        entradaHelper.firmarSolicitudSiEsEnServidor(exp, MiTramiteV1Util.CAMPOS_ENTRADA, CAMPO_FIRMA_SOLICITUD);

        entradaHelper.presentar(exp, MiTramiteV1Util.CAMPOS_ENTRADA, List.of(exp.getJustificante()), eventContext);
        eventContext.updateState(States.Verificacion.PENDIENTE_VERIFICACION);
    } finally {
        exp.setClaveCertificado(null);
    }
}

@WhenEvent
public void triggerBack(MiTramiteV1 exp, MiTramiteV1 original, EventContext eventContext) throws BusinessException {
    exp.setClaveCertificado(null);
    // ... volver al estado anterior (recetas/presentacion.md §5.3)
}
```

1. `EntradaHelper.firmarSolicitudSiEsEnServidor(expediente, CAMPOS_ENTRADA, nombreCampoFirma)` (`tramites/util/entrada`) hace por dentro lo que haría el trigger a mano:
   - Recalcula la situación del DNI del usuario autenticado (`SecurityUtil.getUser().getDni()` + `CertificadoDigitalService.getSituacionFirmaByDni`), igual que el validator. **MUST NOT** intentar leerla del formulario: no existe como campo.
   - Si `isFirmaEnServidor()`, firma con `FirmaServidorHelper.firmarEnServidor(dni, situacion, clave, original, nombreCampoFirma)` y deja el `MetaFile` firmado en `pdfSolicitudFirmada`. Si la clave no abre el almacén lanza `BusinessException` con el motivo; el validator ya lo comprobó, así que aquí es red de seguridad.
   - Con AutoFirma **no hace nada**: el campo firmado ya llegó validado.
2. El resto del trigger (registro de entrada, transición) es igual en los dos casos: `recetas/presentacion.md` §5.3.
3. `claveCertificado` **MUST** ponerse a `null` en un `finally`, para que no sobreviva en el objeto si algo falla después. **MUST** hacerlo también el `triggerBack` que deshace la presentación, porque el cliente puede haberla enviado.
4. `EntradaHelper` se inyecta con `@Inject` de campo (`phaseeventmanager.md` §1). Qué es `CAMPOS_ENTRADA`: `recetas/presentacion.md` §1.1.
5. Para firmar así **otro** documento que no sea la solicitud, el trigger usa directamente `FirmaServidorHelper` (inyectado) dentro de un `if (situacionFirma.isFirmaEnServidor())`, con esos mismos pasos.

- ✅ CORRECTO: `entradaHelper.firmarSolicitudSiEsEnServidor(exp, MiTramiteV1Util.CAMPOS_ENTRADA, CAMPO_FIRMA_SOLICITUD)`
- ❌ INCORRECTO: `if (situacionFirma.isFirmaEnServidor()) { exp.setPdfSolicitudFirmada(firmaServidorHelper.firmarEnServidor(...)); }` escrito a mano en el `triggerPresentar` de la fase `ENTRADA` (es lo que ya hace el helper; repetido en dos tipos, CPD rompe el build)

### 1.6 Checklist

- [ ] Documento: el hueco de la firma lleva `campoFirma`.
- [ ] Modelo: par `MetaFile` original/firmado (`pdfSolicitud`/`pdfSolicitudFirmada`); sin redeclarar `claveCertificado`; sin `situacionFirma` en el `domains.xml`.
- [ ] Vista: `-pdfSolicitud` y `firma-solicitud` (sin guion); `onLoad` con la `action-record` de `situacionFirma`/`firmaEnServidor`; dos botones `PRESENTAR` con `showIf` excluyentes; `action-record` que vacía la clave tras el evento de servidor.
- [ ] Validator: dos `field(...)`, dos ramas complementarias sobre `isFirmaEnServidor()`.
- [ ] Trigger: `entradaHelper.firmarSolicitudSiEsEnServidor(...)`, mismo nombre de campo de firma que la `action-method`, `finally` que vacía la clave, y `triggerBack` que también la vacía.

## 2. El centro firma un documento que emite (sello: director, secretario…)

Solo hay trigger: el documento lo genera y lo firma el servidor con el certificado del centro, sin intervención del usuario.

```java
/** El campoFirma del hueco de la firma del director en documentospdf/resolucion.xml. */
private static final String CAMPO_FIRMA_RESOLUCION = "firmaDirector";

@Inject
AlmacenClaveResolver almacenClaveResolver;
...
DocumentoPdf resolucionFirmada = resolucion.firmar(almacenClaveResolver.getDirector(expediente.getCentro()), new CampoFirma(CAMPO_FIRMA_RESOLUCION));
```

- `AlmacenClaveResolver` (inyectado): `getDirector(centro)`, `getSecretario(centro)`, `getByDNI(dni)`, `getDummy()` (pruebas).
- `CampoFirma` dice dónde se firma (§4) y es un builder: `setMensaje/setMotivo/setFontSize/setNumeroPagina/setImage/setFechaFirma`.
- El resultado es un `DocumentoPdf`; para guardarlo en la entidad o registrarlo de salida, `phaseeventmanager.md` §6.1 y §6.3.

## 3. Poner un documento a firmar a otro usuario (`TareaFirma`, subsistema Firmas)

Estilo portafirmas: un evento crea una `TareaFirma` para que otro usuario firme en su bandeja de firmas cuando quiera, y el expediente espera en un estado **sin botones** hasta que el subsistema de firmas avisa de que ha firmado o ha rechazado firmar. Es el único caso legítimo en que un trámite depende de `subsystem/firmas`. Para ver uno de verdad, busca `implements TareaFirmaNotifier` bajo `src/main/java/com/educaflow/tramites/`.

| Paso | Qué pasa | Dónde |
|---|---|---|
| 1 | Un evento de usuario (`RESOLVER`) genera el PDF, busca al firmante, crea la `TareaFirma` y pasa al estado de espera | `triggerResolver` (§3.3) |
| 2 | El firmante firma o rechaza en su bandeja; el subsistema de firmas llama al `notify` del notifier | `notify` (§3.4) |
| 3 | `notify` dispara el evento de sistema que toca, `FIRMAR` o `RECHAZAR_FIRMA`, con los datos de la tarea en el `requestData` | `notify` (§3.4) |
| 4 | `triggerFirmar` registra de salida el documento firmado y cierra; `triggerRechazarFirma` vuelve al estado de decisión | §3.4 |

### 3.1 Máquina de estados

```xml
<fase name="RESOLUCION" title="Resolución">
    <state name="PENDIENTE_RESOLUCION"     events="RESOLVER" profile="TRAMITADOR" title="Pendiente de resolución"/>
    <!-- Sin botones: los dos eventos los dispara el servidor cuando el director firma o rechaza en su bandeja de firmas -->
    <state name="PENDIENTE_FIRMA_DIRECTOR" events="" systemEvents="FIRMAR,RECHAZAR_FIRMA" profile="DIRECTOR" title="Pendiente de la firma del director"/>
    <state name="ACEPTADO"                 events="" profile="TRAMITADOR" title="Aceptado"  closed="true"/>
    <state name="RECHAZADO"                events="" profile="TRAMITADOR" title="Rechazado" closed="true"/>
</fase>
```

- `FIRMAR` y `RECHAZAR_FIRMA` son **eventos de sistema** (`SKILL.md` §2.1): tienen su `trigger*` y su método del validador, pero ningún botón.
- El estado de espera es del perfil de quien firma (`DIRECTOR`). Sus forms, el del perfil y el genérico, son de solo `EXIT` con su `avisoEstadoExpediente` (`vistas.md` §6.1): al firmante le dice dónde se firma (menú «Firmas», opción «Pendientes») y a los demás que está pendiente de esa firma.

### 3.2 Documento y modelo

- El documento marca el hueco de la firma con `campoFirma="firmaDirector"` (`documentos.md` §2.10).
- El modelo lleva el documento sin firmar y el firmado. **MUST NOT** añadir un campo para quién ha firmado: se lee de la firma del PDF.
  ```xml
  <many-to-one name="pdfResolucion"        title="Resolución sin firmar" ref="com.axelor.meta.db.MetaFile"/>
  <many-to-one name="pdfResolucionFirmada" title="Resolución"            ref="com.axelor.meta.db.MetaFile"/>
  ```
- **MUST NOT** declarar un `many-to-one` a la `TareaFirma`: el expediente no la guarda (§3.4).

### 3.3 El evento que pone a firmar

```java
public class PhaseEventManagerImpl extends PhaseEventManager<MiTramiteV1> implements TareaFirmaNotifier {

    /** El campoFirma del hueco de la firma del director en documentospdf/resolucion.xml. */
    private static final String CAMPO_FIRMA_RESOLUCION = "firmaDirector";

    @Inject
    ModelServiceFactory modelServiceFactory;
    @Inject
    TramitadorService tramitadorService;
    ...

    @WhenEvent
    public void triggerResolver(MiTramiteV1 expediente, MiTramiteV1 original, EventContext eventContext) throws BusinessException {
        // 1. Lo que lanza BusinessException, lo primero: antes de crear nada
        UserService userService = (UserService) modelServiceFactory.resolve(User.class);
        User director = userService.getByCentroAndCargo(expediente.getCentro(), CargoCodigo.DIRECTOR);

        // 2. El PDF que se va a firmar, sin fecha al pie: la lleva la firma (documentos.md §2.10)
        DocumentoPdf resolucionPdf = expediente.getDocumentoPdf(MiTramiteV1.TipoDocumentoPdf.RESOLUCION);
        expediente.setPdfResolucion(MetaFileHelper.createMetaFile(resolucionPdf));

        // 3. La tarea de firma
        TareaFirmaService tareaFirmaService = (TareaFirmaService) modelServiceFactory.resolve(TareaFirma.class);
        tareaFirmaService.insert(new TareaFirmaInsertDTO(
                director,                              // User que debe firmar
                expediente.getCentro(),                // centro al que pertenece la tarea de firma
                List.of(expediente.getPdfResolucion()),// PDFs a firmar (MUST ser PDFs, lista no vacía)
                I18n.get("Resolución del expediente %s").formatted(expediente.getNumeroExpediente()),  // motivo
                CAMPO_FIRMA_RESOLUCION,                // campoFirma de los PDFs: el campo de firma vacío en el que se firma (§4)
                PhaseEventManagerImpl.class,           // clase TareaFirmaNotifier del callback
                expediente.getId()));                  // callBackData: vuelve tal cual en notify

        // 4. Se pasa al estado sin botones
        eventContext.updateState(States.Resolucion.PENDIENTE_FIRMA_DIRECTOR);
    }
}
```

1. **El firmante**: `UserService.getByCentroAndCargo(centro, cargo)` (el `ModelService` de `User`, en `com.axelor.auth.service`; se obtiene con `modelServiceFactory.resolve(User.class)`) devuelve el único usuario con ese cargo (`CargoCodigo.DIRECTOR`, `CargoCodigo.SECRETARIO`…) en el centro, y lanza `RuntimeException` si no hay ninguno o hay varios: es un centro mal configurado, que no debería darse nunca. Como toda comprobación, **MUST** ir al principio del trigger, antes de crear el PDF o la tarea (`SKILL.md` §1.6).
2. **El PDF** se genera sin firmar y se guarda en el expediente; es el que el firmante verá en su bandeja.
3. **`TareaFirmaInsertDTO(firmante, centro, pdfs, motivo, nombreCampoFirma, notifier, callBackData)`**: todos los PDFs **MUST** tener ese campo de firma vacío (§4).
4. El notifier se pasa como `PhaseEventManagerImpl.class`, **no** `this.getClass()`: lo que se guarda es su FQCN (ver el CRITICAL de abajo), y tiene que ser exactamente el de la clase.
5. `callBackData` es el **id del expediente**: es todo lo que `notify` necesita para recuperarlo.

- ✅ CORRECTO: `PhaseEventManagerImpl.class` como notifier
- ❌ INCORRECTO: `this.getClass()` (lo que se congela en la fila es un FQCN: se escribe el de la clase, no el que tenga la instancia en runtime)
- ❌ INCORRECTO: `userService.getByCentroAndCargo(...)` después de `tareaFirmaService.insert(...)` o de crear el `MetaFile` (la comprobación va antes de crear nada)
- ❌ INCORRECTO: `expediente.setTareaFirmaResolucion(tareaFirma)` (el expediente no guarda la tarea: `notify` lo encuentra por el `callBackData`)

### 3.4 El callback y los eventos de sistema

```java
/** Los dos eventos de sistema (systemEvents) del estado PENDIENTE_FIRMA_DIRECTOR. */
private static final String EVENTO_FIRMAR = "FIRMAR";
private static final String EVENTO_RECHAZAR_FIRMA = "RECHAZAR_FIRMA";

/** El director ha firmado o rechazado en su bandeja de firmas. Hace de controlador del tramitador. callBackData es el id del expediente. */
@Override
public void notify(TareaFirma tareaFirma, Object callBackData) {
    // 1. El expediente
    MiTramiteV1 expediente = repository.find(((Number) callBackData).longValue());
    if (expediente == null) {
        throw new IllegalStateException("No existe el expediente id=" + callBackData + " de la tarea de firma id=" + tareaFirma.getId());
    }

    // 2. El evento
    String evento = switch (tareaFirma.getEstadoTareaFirma()) {
        case FIRMADO -> EVENTO_FIRMAR;
        case RECHAZADO -> EVENTO_RECHAZAR_FIRMA;
        case PENDIENTE -> throw new IllegalStateException("La tarea de firma id=" + tareaFirma.getId() + " sigue pendiente");
    };

    // 3. El requestData: lo que el evento necesita de la tarea
    Map<String, Object> requestData = Map.of();
    if (tareaFirma.getEstadoTareaFirma() == EstadoTareaFirma.FIRMADO) {
        requestData = Map.of("pdfResolucionFirmada", Map.of("id", tareaFirma.getDocumentosFirma().get(0).getDocumentoFirmado().getId()));
    }

    if (tareaFirma.getEstadoTareaFirma() == EstadoTareaFirma.RECHAZADO) {
        ExpedienteNotasUtil.addNote(expediente, I18n.get("Rechazo firmar la resolución: %s").formatted(tareaFirma.getMotivoRechazo()));
    }

    try {
        tramitadorService.triggerEvent(expediente, evento, requestData, new EventContext(expediente, Profile.DIRECTOR, modelServiceFactory));
    } catch (BusinessException ex) {
        throw new IllegalStateException("No se ha podido disparar el evento " + evento + " del expediente " + expediente.getNumeroExpediente() + ": " + ex.getBusinessMessages(), ex);
    }
}

/** Evento de sistema: el director ha firmado. pdfResolucionFirmada ya está en el expediente. Se registra de salida el documento firmado y el expediente se cierra. */
@WhenEvent
public void triggerFirmar(MiTramiteV1 expediente, MiTramiteV1 original, EventContext eventContext) throws BusinessException {
    MetaFile resolucionFirmada = MetaFileUtil.cloneMetaFile(expediente.getPdfResolucionFirmada());
    RegistroSalida registroSalida = eventContext.createRegistroSalida(resolucionFirmada, List.of());
    expediente.setPdfResolucionFirmada(registroSalida.getDocumento());
    expediente.setPdfResolucion(null);

    switch (expediente.getTipoResolucion()) {
        case ACEPTAR -> eventContext.updateState(States.Resolucion.ACEPTADO);
        case RECHAZAR -> eventContext.updateState(States.Resolucion.RECHAZADO);
        case null -> throw new IllegalStateException("El expediente " + expediente.getNumeroExpediente() + " no tiene tipo de resolución");
    }
}

/** Evento de sistema: el director ha rechazado firmar. El expediente vuelve a quien decide; el motivo ya está en las notas. */
@WhenEvent
public void triggerRechazarFirma(MiTramiteV1 expediente, MiTramiteV1 original, EventContext eventContext) throws BusinessException {
    expediente.setPdfResolucion(null);
    eventContext.updateState(States.Resolucion.PENDIENTE_RESOLUCION);
}
```

```kotlin
@BeanValidationRulesForStateAndEvent
fun getForStatePendienteFirmaDirectorInEventFirmar(): BeanValidationRules = rules {
    field(model::getPdfResolucionFirmada) { +Required() }
}

@BeanValidationRulesForStateAndEvent
fun getForStatePendienteFirmaDirectorInEventRechazarFirma(): BeanValidationRules = rules { }
```

1. **`notify(tareaFirma, callBackData)`** lo llama el subsistema de firmas tanto cuando la tarea se firma como cuando se rechaza. Hace de controlador del tramitador (`phaseeventmanager.md` §5.1): de la tarea saca el expediente (por el id del `callBackData`), el evento de sistema que corresponde al `EstadoTareaFirma` (`FIRMADO`, `RECHAZADO`) y el `requestData`.
2. **El `requestData`** de `FIRMAR` lleva el documento firmado, que está en `tareaFirma.getDocumentosFirma().get(0).getDocumentoFirmado()` (la tarea tiene un `DocumentoFirma` por cada PDF; aquí solo hay uno). El de `RECHAZAR_FIRMA` va vacío.
3. **El disparo**: `tramitadorService.triggerEvent(expediente, evento, requestData, new EventContext(expediente, Profile.DIRECTOR, modelServiceFactory))`. `notify` no declara `BusinessException`, así que la envuelve en una `IllegalStateException`.
4. **`triggerFirmar`** no conoce la `TareaFirma`: lee `pdfResolucionFirmada` del expediente, donde el motor ya lo ha copiado. El documento se **clona** con `MetaFileUtil.cloneMetaFile` antes de registrarlo de salida (`phaseeventmanager.md` §6.3): el que llega es de la tarea de firma, no del expediente.
5. **El rechazo**: el motivo (`tareaFirma.getMotivoRechazo()`, que puede venir vacío) lo deja `notify` como **nota** del expediente (`ExpedienteNotasUtil.addNote`, `vistas.md` §4), a nombre de quien rechaza, antes de disparar el evento. **MUST NOT** crear campos en el `domains.xml` para guardarlo: lo que se dicen entre sí quienes tramitan va a las notas. `triggerRechazarFirma` solo vuelve al estado de decisión, donde el siguiente `RESOLVER` creará **otra** tarea.
6. **El validador**: `getForStatePendienteFirmaDirectorInEventFirmar` declara `Required` el campo del `requestData` (sin esa entrada el motor no lo copia); `…InEventRechazarFirma` lleva `rules { }` vacío.

> **CRITICAL — la `TareaFirma` congela el FQCN del notifier.** El FQCN de la clase que se pasa (`PhaseEventManagerImpl.class`) se guarda tal cual en la columna `fqcnFirmaNotifier` de la fila (y el tipo del callback en `fqcnCallBackData`), así que la fila apunta a una clase que vive **bajo `tramites/**`**, justo el árbol que mueven las recetas de fase y de versionado.
>
> A diferencia del `PhaseEventManager` y del `StateEventValidator` —que se encuentran por convención y por eso mover la carpeta de un tipo se corrige solo (`SKILL.md` §1.1)—, aquí **no hay autocuración**: mover o renombrar la carpeta de la versión, o mover el `PhaseEventManagerImpl` de una fase a otra, deja las `TareaFirma` **pendientes** apuntando a un FQCN que ya no existe, y su callback revienta al completarse la firma.
>
> Mientras el notifier siga resolviéndose por FQCN: **MUST** comprobar, antes de mover una carpeta de versión o de fase que tenga firmas en marcha (`recetas/versionado.md`), si hay filas de `TareaFirma` pendientes con ese `fqcnFirmaNotifier`, y actualizarlas a mano.

## 4. Dónde cae la firma dentro del PDF

Hay dos formas de decirlo. **MUST** usarse la primera siempre que el PDF tenga un campo de firma vacío para esa firma.

| Forma | Cuándo | En el servidor | Con AutoFirma (`<action-method>`) |
|---|---|---|---|
| Por el **nombre de un campo de firma vacío** del PDF | Documento generado de un XML con `campoFirma` (`documentos.md` §2.10), o PDF versionado que ya trae el campo | `new CampoFirma("firmaSolicitante")` · `firmarEnServidor(dni, situacion, clave, original, "firmaSolicitante")` | `firmarDocumentoEnCampo(id, origen, destino, "firmaSolicitante")` |
| En un **rectángulo** de una página | El PDF no tiene campo de firma para esa firma (un PDF versionado sin campos, un documento que sube el usuario) | `new CampoFirma(new Rectangulo(x, y, ancho, alto)).setNumeroPagina(n)` · `firmarEnServidor(dni, situacion, clave, original, rectangulo, pagina)` | `firmarDocumento(id, origen, destino, x, y, ancho, alto, pagina)` |

- Con el nombre del campo, la página y el recuadro son los del campo: la firma sigue a su hueco aunque el contenido del documento lo mueva.
- Firmar en un campo que no existe, o que ya está firmado, lanza `RuntimeException` y aborta el evento.
- **MUST NOT** firmar en un rectángulo medido a mano un documento generado de un XML: su contenido se desplaza con los datos (`visible`, valores largos, saltos de página) y la firma se queda donde estaba.
- El rectángulo va en puntos PDF desde la esquina inferior izquierda de la página; sin `setNumeroPagina`, la página es la última.
- La `TareaFirma` de §3 admite las dos: `new TareaFirmaInsertDTO(firmante, centro, pdfs, motivo, "firmaDirector", notifier, datos)` o `new TareaFirmaInsertDTO(firmante, centro, pdfs, motivo, rectangulo, pagina, notifier, datos)`. Con el nombre, **todos** los PDFs de la tarea **MUST** tener ese campo de firma vacío: si a alguno le falta, crear la tarea lanza `IllegalArgumentException`.

- ✅ CORRECTO: `<espacio alto="88.56" campoFirma="firmaDirector"/>` en el documento + `resolucion.firmar(almacen, new CampoFirma("firmaDirector"))`.
- ❌ INCORRECTO: `new CampoFirma(new Rectangulo(150, 317, 300, 53))` sobre la resolución generada de `resolucion.xml` (coordenadas medidas con unos datos concretos: con otros, la firma cae fuera de su hueco).
