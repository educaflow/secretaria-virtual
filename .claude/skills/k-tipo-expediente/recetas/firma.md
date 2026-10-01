# Receta: firmar documentos en un tipo de expediente

Las tres formas de que un documento de un expediente acabe firmado, cada una con todas sus piezas en orden (modelo, vista, validator, trigger). Los ejemplos usan el trámite inventado `MiTramite` (`SKILL.md`); para ver uno de verdad, abre cualquier tipo bajo `src/main/java/com/educaflow/tramites/` que tenga `pdfSolicitudFirmado`.

| Forma | Quién firma | Dónde | Sección |
|---|---|---|---|
| El usuario firma al presentar | El usuario autenticado, con su certificado custodiado (en servidor) o con AutoFirma | Modelo + vista + validator + trigger | §1 |
| El centro firma un documento que emite | El certificado del centro (director, secretario…) | Solo el trigger | §2 |
| Poner un documento a firmar a otro usuario | Otro usuario, desde su portafirmas, cuando quiera | Trigger + callback | §3 |

Dónde cae la firma dentro del PDF es lo mismo en las tres: §4.

Dependencias: §1 y §2 firman con `subsystem/criptografia` (a través de `tramites/util/firma` en §1). **MUST NOT** depender de `subsystem/firmas` para firmar: ese subsistema es la bandeja tipo portafirmas de §3 y podría desaparecer.

## 1. El usuario firma al presentar (en servidor o con AutoFirma)

### 1.1 Cómo funciona

- Quien firma es siempre el **usuario autenticado**. El DNI se lee de él con `SecurityUtil.getUser()`; nunca del bean ni del formulario.
- Si firma **en el servidor** (tiene un certificado custodiado) o **con AutoFirma** (lo firma en su equipo y sube el PDF) lo decide su `SituacionFirma` (`subsystem/criptografia`), **no el trámite**. El servidor la recalcula del DNI cada vez que la necesita con `CertificadoDigitalHelper.getSituacionFirmaByDni(dni)`.
- `SituacionFirma.isFirmaEnServidor()` es **la** definición de «corresponde firmar en servidor». **MUST NOT** enumerar valores del enum en ningún sitio (validator, vista, trigger): una situación nueva se decide en el enum y todo lo demás la sigue.
- Con firma en servidor, el usuario teclea la clave de su certificado (PIN del dispositivo o contraseña del fichero) en `claveCertificado`, y el trigger firma con ella. Con AutoFirma, el usuario sube el PDF firmado y el validator comprueba la firma.
- El código común vive en `tramites/util/firma/`: `FirmaServidorRules.kt` (reglas del validator), `FirmaServidorHelper` (firmar en el trigger) y `FirmaServidorController` (lo que la vista pregunta en el `onLoad`).

### 1.2 Documento y modelo (`documentospdf/` y `domains.xml`)

- El documento que se firma marca el hueco de la firma con `campoFirma="firmaSolicitante"` (`documentos.md` §2.10). Ese nombre es el que usan después la vista (§1.3) y el trigger (§1.5).

- Par de campos `MetaFile` original/firmado. El original lo genera el trigger anterior (`phaseeventmanager.md` §6.1); el firmado lo sube el usuario (AutoFirma) o lo genera el trigger que presenta (servidor).
  ```xml
  <many-to-one name="pdfSolicitud" title="PDF de la solicitud" ref="com.axelor.meta.db.MetaFile" />
  <many-to-one name="pdfSolicitudFirmado" title="PDF de la solicitud" ref="com.axelor.meta.db.MetaFile" />
  ```
- `claveCertificado` **ya está heredado** de `Expediente` (`modelo.md` §2): transient, `password="true"`, nunca se persiste ni se devuelve al cliente. **MUST NOT** redeclararlo.
- La situación de firma **MUST NOT** ser un campo de la entidad: es un campo de vista (§1.3). El servidor no la lee nunca del formulario.

### 1.3 Vista (`views.xml` de la fase)

Todo va dentro del `<form state=...>` del perfil que presenta, tras su `<include-panels>` (`vistas.md` §6). Las acciones se declaran en ese mismo fichero con prefijo `exp-<Code>-`: sus nombres son globales, y ponerlas junto a su botón hace que copiar la fase se lo lleve todo.

```xml
<form state="PENDIENTE_PRESENTACION" profile="CREADOR" onLoad="exp-MiTramiteV1-PENDIENTE_PRESENTACION-onLoad-action">
    <include-panels>
        -pdfSolicitud
    </include-panels>

    <!-- 1. Campos de vista (no del modelo) que rellena el onLoad -->
    <panel name="firmaSolicitudSituacion" title="" colSpan="12" showFrame="false" readonly="true">
        <field name="situacionFirma" type="string" showIf="false" readonly="true"/>
        <field name="firmaEnServidor" type="boolean" showIf="false" readonly="true"/>
    </panel>

    <!-- 2. Un panel por situación: texto de ayuda y, si hace falta, la clave -->
    <panel name="firmaSolicitudSinCertificado" title="Firma de la solicitud" colSpan="12" showIf="situacionFirma=='SIN_CERTIFICADO'">
        <help variant="info" colSpan="12"><![CDATA[Para presentar la solicitud debe tener <a href="...">AutoFirma</a> instalada y un certificado digital válido]]></help>
    </panel>
    <panel name="firmaSolicitudFicheroSinClave" title="Firma de la solicitud" colSpan="12" showIf="situacionFirma=='FICHERO_SIN_CLAVE'">
        <help variant="info" colSpan="12"><![CDATA[La solicitud se firmará en el servidor con su certificado digital. Introduzca la contraseña de su certificado.]]></help>
        <field name="claveCertificado" title="Contraseña" widget="password" required="true" colSpan="4"/>
    </panel>
    <!-- ídem DISPOSITIVO_SIN_PIN (title="PIN"), DISPOSITIVO_CON_PIN y FICHERO_CON_CLAVE (solo ayuda) y SIN_DNI (variant="warning", sin botón) -->

    <!-- 3. Dos botones del mismo evento; se muestra uno u otro -->
    <footer>
        <buttons-right>
            <button name="PRESENTAR" colSpan="4" title="Firmar con AutoFirma__!! y Presentar la solicitud"
                    showIf="situacionFirma=='SIN_CERTIFICADO'"
                    onClick="serial:exp-MiTramiteV1-firmarDocumentacionParaPresentar-action,subsysTramitador-event-action"/>
            <button name="PRESENTAR" colSpan="4" title="Firmar y Presentar la solicitud"
                    showIf="firmaEnServidor"
                    onClick="serial:subsysTramitador-event-action,exp-MiTramiteV1-set-claveCertificado-null-action"/>
        </buttons-right>
    </footer>
</form>

<action-group name="exp-MiTramiteV1-PENDIENTE_PRESENTACION-onLoad-action">
    <action name="exp-MiTramiteV1-set-situacionFirma-action"/>
</action-group>

<action-record name="exp-MiTramiteV1-set-situacionFirma-action" model="com.educaflow.subsystem.expedientes.db.MiTramiteV1">
    <field name="situacionFirma" expr="call:com.educaflow.tramites.util.firma.FirmaServidorController:getSituacionFirma()"/>
    <field name="firmaEnServidor" expr="call:com.educaflow.tramites.util.firma.FirmaServidorController:isFirmaEnServidor()"/>
</action-record>

<action-method name="exp-MiTramiteV1-firmarDocumentacionParaPresentar-action">
    <call class="com.educaflow.tramites.util.firma.FirmaClienteController"
          method='firmarDocumentoEnCampo(id,"pdfSolicitud","pdfSolicitudFirmado","firmaSolicitante")'/>
</action-method>

<action-record name="exp-MiTramiteV1-set-claveCertificado-null-action" model="com.educaflow.subsystem.expedientes.db.MiTramiteV1">
    <field name="claveCertificado" expr="eval: null"/>
</action-record>
```

1. **Campos de vista** `situacionFirma` (string) y `firmaEnServidor` (boolean): llevan `type=` porque no existen en la entidad. Solo viajan del servidor al cliente para decidir qué se pinta.
2. **`onLoad` del form** → `action-group` → `action-record` que los rellena con `call:` a `FirmaServidorController`: `getSituacionFirma()` da el nombre del enum e `isFirmaEnServidor()` el boolean.
3. **Paneles por situación** con `showIf="situacionFirma=='X'"`: aquí sí se compara con el valor concreto, porque el texto de ayuda y si se pide PIN o contraseña dependen de cada uno. El `<field name="claveCertificado" widget="password">` solo aparece en `DISPOSITIVO_SIN_PIN` (título "PIN") y `FICHERO_SIN_CLAVE` (título "Contraseña"). `SIN_DNI` lleva un `<help variant="warning">` y ningún botón.
4. **Dos botones `PRESENTAR`**, mismo `name` (mismo evento) y `showIf` excluyentes:
   - AutoFirma: `showIf="situacionFirma=='SIN_CERTIFICADO'"` y `serial:` con la `action-method` que llama a `FirmaClienteController.firmarDocumentoEnCampo(...)` **antes** del evento.
   - Servidor: `showIf="firmaEnServidor"` y `serial:` con el evento **y después** la `action-record` que pone `claveCertificado` a `null`, para que la clave no se quede en el formulario.
5. `firmarDocumentoEnCampo(id, campoOrigen, campoDestino, nombreCampoFirma)` lanza AutoFirma sobre el `MetaFile` del campo origen, deja el firmado en el destino y exige firmar con el DNI del usuario autenticado (revienta con `RuntimeException` si no tiene DNI válido). El nombre del campo de firma **MUST** ser el mismo que use el trigger (§1.5), para que la firma caiga en el mismo sitio se firme donde se firme.

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
    field(model::getPdfSolicitudFirmado) {
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
- ❌ INCORRECTO: `field(model::getPdfSolicitudFirmado) { +Required(); +FirmaPdf(model::getPdfSolicitud) }` sin rama (obliga a subir el PDF también a quien firma en servidor, donde lo genera el trigger)
- ❌ INCORRECTO: `field(model::getClaveCertificado) { +ClaveCertificadoValida() }` sin rama (revienta con `IllegalStateException` para quien firma con AutoFirma)
- ❌ INCORRECTO: declarar solo el `field` del PDF «porque este trámite es de AutoFirma» (el camino lo decide el certificado del usuario; sin el otro `field`, la clave no entra en la whitelist y la firma en servidor falla)

### 1.5 Trigger (`PhaseEventManagerImpl.java` de la fase)

```java
/** El campoFirma del hueco de la firma en documentospdf/solicitud.xml: MUST ser el mismo que la <action-method>
 *  de AutoFirma pasa a firmarDocumentoEnCampo (§1.3), para que la firma caiga en el mismo sitio se firme donde se firme. */
private static final String CAMPO_FIRMA_SOLICITUD = "firmaSolicitante";

@Inject
FirmaServidorHelper firmaServidorHelper;

@WhenEvent
public void triggerPresentar(MiTramiteV1 exp, MiTramiteV1 original, EventContext eventContext) throws BusinessException {
    String dniFirmante = SecurityUtil.getUser().getDni();
    SituacionFirma situacionFirma = CertificadoDigitalHelper.getSituacionFirmaByDni(dniFirmante);

    try {
        if (situacionFirma.isFirmaEnServidor()) {
            exp.setPdfSolicitudFirmado(firmaServidorHelper.firmarEnServidor(
                    dniFirmante, situacionFirma, exp.getClaveCertificado(),
                    exp.getPdfSolicitud(), CAMPO_FIRMA_SOLICITUD));
        }

        RegistroEntrada registroEntrada = eventContext.createRegistroEntrada(exp.getPdfSolicitudFirmado(), List.of(exp.getJustificante()));
        exp.setPdfJustificanteRegistroEntrada(registroEntrada.getDocumentoResguardoPresentacion());
        eventContext.updateState(States.Tramitacion.PENDIENTE_RESOLUCION);
    } finally {
        exp.setClaveCertificado(null);
    }
}

@WhenEvent
public void triggerBack(MiTramiteV1 exp, MiTramiteV1 original, EventContext eventContext) throws BusinessException {
    exp.setClaveCertificado(null);
    // ... volver al estado anterior
}
```

1. Recalcula la situación del DNI del usuario autenticado, igual que el validator. **MUST NOT** intentar leerla del formulario: no existe como campo.
2. Si `isFirmaEnServidor()`, firma con `FirmaServidorHelper.firmarEnServidor(dni, situacion, clave, original, nombreCampoFirma)` y deja el `MetaFile` firmado en el campo firmado. Si la clave no abre el almacén lanza `BusinessException` con el motivo; el validator ya lo comprobó, así que aquí es red de seguridad.
3. Con AutoFirma **no hay nada que firmar**: el campo firmado ya llegó validado. El resto del trigger (registro de entrada, transición) es igual en los dos casos.
4. `claveCertificado` **MUST** ponerse a `null` en un `finally`, para que no sobreviva en el objeto si algo falla después. **MUST** hacerlo también el `triggerBack` que deshace la presentación, porque el cliente puede haberla enviado.
5. `FirmaServidorHelper` se inyecta con `@Inject` de campo (`phaseeventmanager.md` §1).

### 1.6 Checklist

- [ ] Documento: el hueco de la firma lleva `campoFirma`.
- [ ] Modelo: par `MetaFile` original/firmado; sin redeclarar `claveCertificado`; sin `situacionFirma` en el `domains.xml`.
- [ ] Vista: `onLoad` con la `action-record` de `situacionFirma`/`firmaEnServidor`; un panel por situación; dos botones `PRESENTAR` con `showIf` excluyentes; `action-record` que vacía la clave tras el evento de servidor.
- [ ] Validator: dos `field(...)`, dos ramas complementarias sobre `isFirmaEnServidor()`.
- [ ] Trigger: `if (situacionFirma.isFirmaEnServidor())` con `firmarEnServidor`, mismo nombre de campo de firma que la `action-method`, `finally` que vacía la clave, y `triggerBack` que también la vacía.

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

Estilo portafirmas: se crea una `TareaFirma` para que otro usuario firme cuando quiera, con callback cuando lo haga. Es el único caso legítimo en que un trámite depende de `subsystem/firmas`.

```java
public class PhaseEventManagerImpl extends PhaseEventManager<...> implements TareaFirmaNotifier {
    ...
    TareaFirmaService tareaFirmaService = (TareaFirmaService) modelServiceFactory.resolve(TareaFirma.class);
    tareaFirmaService.insert(new TareaFirmaInsertDTO(
            firmante,                 // User que debe firmar
            expediente.getCentro(),   // centro al que pertenece la tarea de firma
            List.of(pdf1, pdf2),      // PDFs a firmar (MUST ser PDFs, lista no vacía)
            "Firma Expediente:" + expediente.getNumeroExpediente(),  // motivo
            "firmaDirector",          // campoFirma de los PDFs: el campo de firma vacío en el que se firma (§4)
            this.getClass(),          // clase TareaFirmaNotifier del callback
            "datos de callback"));    // callBackData que se te devuelve

    @Override
    public void notify(TareaFirma tareaFirma, Object callBackData) { /* qué hacer al completarse la firma */ }
}
```

Nota: ningún trámite lo usa todavía en un flujo real. El patrón es este, pero confirma el caso de uso antes de copiarlo.

> **CRITICAL — la `TareaFirma` congela el FQCN del notifier.** El `this.getClass()` que se pasa se guarda tal cual en la columna `fqcnFirmaNotifier` de la fila (y el tipo del callback en `fqcnCallBackData`), así que la fila apunta a una clase que vive **bajo `tramites/**`**, justo el árbol que mueven las recetas de fase y de versionado.
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
