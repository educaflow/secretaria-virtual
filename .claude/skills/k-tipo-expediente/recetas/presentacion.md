# Receta: el usuario presenta un documento (acaba en el registro de entrada)

El camino completo por el que unos datos que el usuario teclea en el expediente se convierten en un PDF, lo firma, lo presenta, queda **asentado en el registro de entrada** con su resguardo y se verifica. Es el camino de las dos **fases comunes** `ENTRADA` y `VERIFICACION`, las mismas en todos los tipos (`SKILL.md` §1.2). Los ejemplos usan el trámite inventado `MiTramite` (`SKILL.md`); para ver uno de verdad, abre las carpetas `entrada/` y `verificacion/` de cualquier tipo bajo `src/main/java/com/educaflow/tramites/`.

El registro de entrada asienta el documento **firmado por el usuario** (`RegistroEntrada.documentoOriginalFirmado`): por eso la presentación telemática pasa **siempre** por la firma de `recetas/firma.md` §1, que esta receta no repite sino que referencia en el paso que le toca. En papel se asienta la solicitud firmada a mano y escaneada (§5.5).

| Paso | Qué pasa | Dónde | Sección |
|---|---|---|---|
| 0 | Las dos fases comunes: tres estados de `ENTRADA` y uno de `VERIFICACION` | `TipoExpedienteInstance.xml` | §0 |
| 1 | Los campos: los datos, los anexos y los cinco campos comunes; el descriptor `CAMPOS_ENTRADA` | `domains.xml`, `<Code>Util` | §1 |
| 2 | El expediente nace con solicitante e interesado, que son los que van al registro, y en el estado que le toca | `InitialEventManagerImpl` | §2 |
| 3 | El documento que se va a presentar, con hueco para la firma | `documentospdf/` | §3 |
| 4 | El usuario teclea los datos; el trigger genera el PDF | estado `ENTRADA_DATOS` | §4 |
| 5 | El usuario revisa el PDF, lo firma y presenta; el trigger asienta el registro y guarda el resguardo | estado `PENDIENTE_PRESENTACION` | §5 |
| 5.5 | En papel: el `TRAMITADOR` adjunta el escaneado, copia los datos y presenta | estados `PENDIENTE_DOCUMENTO_ESCANEADO` y `ENTRADA_DATOS` | §5.5 |
| 6 | El `TRAMITADOR` verifica: correcta, o a subsanar | estado `PENDIENTE_VERIFICACION` | §6 |

Dependencias:

- El registro lo crea `EventContext` contra `subsystem/registroentradasalida` (`phaseeventmanager.md` §3); no hay que inyectar ni resolver nada del registro en el trámite.
- Lo que hacen igual todos los tipos está en `tramites/util/entrada/` (`EntradaHelper`, `CamposEntrada`) y `tramites/util/verificacion/` (`VerificacionHelper`); los paneles, en `tramites/shared/template-views.xml` (`vistas.md` §3.2).
- **MUST NOT** copiar esa lógica a la carpeta del tipo: CPD rompe el build si dos trámites repiten código. El tipo solo escribe lo que se ve en esta receta, incluidas las reglas de validación, que van en su validador (`validator.md` §3.2).

## 0. La máquina de estados

```xml
<fase name="ENTRADA" title="Entrada">
    <state name="PENDIENTE_DOCUMENTO_ESCANEADO" events="DELETE,CONTINUAR"          profile="TRAMITADOR" title="Pendiente de adjuntar la solicitud en papel escaneada"/>
    <state name="ENTRADA_DATOS"                 events="DELETE,BACK,GUARDAR_DATOS" profile="CREADOR"    title="Entrada de datos"/>
    <state name="PENDIENTE_PRESENTACION"        events="BACK,PRESENTAR"            profile="CREADOR"    title="Pendiente de presentación"/>
</fase>
<fase name="VERIFICACION" title="Verificación">
    <state name="PENDIENTE_VERIFICACION"        events="VERIFICAR"                 profile="TRAMITADOR" title="Pendiente de verificación"/>
</fase>
<fase name="RESOLUCION" title="Resolución">
    <state name="PENDIENTE_RESOLUCION"          events="RESOLVER"                  profile="TRAMITADOR" title="Pendiente de resolución"/>
    ...
</fase>
```

- Las dos primeras `<fase>` **MUST** ser estas, tal cual, en todos los tipos (`SKILL.md` §1.2). El tipo solo decide lo que viene después.
- `ENTRADA_DATOS` y `PENDIENTE_PRESENTACION` son **dos** estados y no uno porque el usuario **MUST** ver el PDF que va a firmar antes de firmarlo, y ese PDF lo genera el servidor a partir de los datos: primero se guardan los datos y se genera (`GUARDAR_DATOS`), después se firma y presenta (`PRESENTAR`).
  Además `FirmaPdf` compara el PDF firmado con el original persistido (`recetas/firma.md` §1.4), así que el original **MUST** existir en el expediente antes de firmar.
- `BACK` desde `PENDIENTE_PRESENTACION` devuelve a `ENTRADA_DATOS` para corregir los datos: al volver a `GUARDAR_DATOS` el PDF se regenera y el ciclo empieza otra vez.
- El camino telemático y el camino en papel **comparten estados pero no eventos**: `PENDIENTE_DOCUMENTO_ESCANEADO`, su `CONTINUAR` y el `BACK` de `ENTRADA_DATOS` son solo del papel; `PENDIENTE_PRESENTACION` y su `PRESENTAR`, solo del telemático; `GUARDAR_DATOS` hace una cosa distinta en cada uno (§4.3).
- `DELETE` solo antes de presentar y nunca en `PENDIENTE_PRESENTACION`: desde ahí se vuelve con `BACK` para borrar, y una vez presentado el expediente ya no se borra (§5.4).
- El destino de presentar es siempre `VERIFICACION` / `PENDIENTE_VERIFICACION`; cruzar de fase no necesita nada especial (`SKILL.md` §1.2).

## 1. Modelo (`domains.xml`)

```xml
<entity name="MiTramiteV1" extends="Expediente">
    <!-- 1. Los datos que el usuario teclea y que van al documento -->
    <string name="dias"/>
    <enum name="motivo" ref="MotivoMiTramiteV1"/>
    <string name="otroMotivo"/>

    <!-- 2. Lo que el usuario adjunta: va como anexo del registro -->
    <many-to-one name="justificante" title="Foto o PDF del justificante" ref="com.axelor.meta.db.MetaFile"/>

    <!-- 3. El documento que presenta: original (lo genera GUARDAR_DATOS) y firmado (lo firma PRESENTAR; en papel, el escaneado) -->
    <many-to-one name="pdfSolicitud"        title="PDF de la solicitud"         ref="com.axelor.meta.db.MetaFile"/>
    <many-to-one name="pdfSolicitudFirmada" title="PDF de la solicitud firmada" ref="com.axelor.meta.db.MetaFile"/>

    <!-- 4. El resguardo sellado que devuelve el registro de entrada -->
    <many-to-one name="pdfJustificanteRegistroEntrada" title="Justificante del registro de entrada" ref="com.axelor.meta.db.MetaFile"/>

    <!-- 5. La verificación -->
    <enum name="resultadoVerificacion" title="Resultado de la verificación" ref="ResultadoVerificacionMiTramiteV1"/>
    <string name="textoSubsanacion" title="Qué hay que subsanar" large="true"/>
    ...
</entity>

<enum name="ResultadoVerificacionMiTramiteV1">
    <item name="CORRECTO" title="La solicitud es correcta"/>
    <item name="SUBSANAR" title="Pedir subsanación"/>
</enum>
```

1. **Datos**: los campos que el usuario rellena y que el documento estampa con `self.<campo>` (§3).
2. **Anexos**: un `MetaFile` por cada fichero que el usuario aporta. Pueden ser imágenes o PDF; el registro los admite todos como anexos.
3. **Par original/firmado**: es el mismo par de `recetas/firma.md` §1.2. El original lo genera el trigger de `GUARDAR_DATOS`; el firmado lo produce `PRESENTAR` (en servidor), lo sube el usuario (AutoFirma) o, en papel, es el escaneado que adjunta el `TRAMITADOR`.
4. **Resguardo**: el `MetaFile` que devuelve el registro de entrada; se guarda para enseñárselo al usuario (§6.1).
5. **Verificación**: el resultado (enum propio del tipo, con los ítems `CORRECTO` y `SUBSANAR`) y el texto de lo que hay que subsanar.
- Los campos de 3, 4 y 5 son los **campos comunes**: **MUST** llamarse exactamente así, porque los paneles comunes los nombran (`vistas.md` §3.2). No están en `Expediente`: los declara cada tipo.
- `personaSolicitante`, `personaInteresada`, `numeroExpediente`, `centro` y `claveCertificado` ya están heredados de `Expediente` (`modelo.md` §2). **MUST NOT** redeclararlos: son justo los que `createRegistroEntrada` lee del expediente (§5.3).

- ✅ CORRECTO: `<many-to-one name="pdfSolicitudFirmada" .../>`
- ❌ INCORRECTO: `<many-to-one name="pdfSolicitudFirmado" .../>` o `solicitudFirmada` (los paneles comunes pintarían un campo vacío)
- ❌ INCORRECTO: `<enum name="ResultadoVerificacion">` compartido entre tipos (los enums son del tipo y llevan su sufijo, `modelo.md` §3)

### 1.1 `CAMPOS_ENTRADA` en `<Code>Util`

El código común no puede nombrar los campos del tipo (son de **su** entidad, no de `Expediente`): los recibe en un descriptor `CamposEntrada<T>` con referencias a sus getters y setters. Cada tipo construye el suyo **una vez**, como constante de su `<Code>Util`, junto a la mutación que vacía la subsanación:

```java
/** Los campos de este tipo con los que trabaja el código común de la fase ENTRADA (tramites/util/entrada). */
public static final CamposEntrada<MiTramiteV1> CAMPOS_ENTRADA = new CamposEntrada<>(
        MiTramiteV1::getPdfSolicitud,
        MiTramiteV1::getPdfSolicitudFirmada,
        MiTramiteV1::setPdfSolicitudFirmada,
        MiTramiteV1::setPdfJustificanteRegistroEntrada,
        MiTramiteV1Util::borrarSubsanacion);

public static void borrarSubsanacion(MiTramiteV1 expediente) {
    expediente.setResultadoVerificacion(null);
    expediente.setTextoSubsanacion(null);
}
```

- El orden de los cinco argumentos es el del record: la solicitud generada, la que se registra, dónde se deja la firmada en servidor, dónde se deja el resguardo y cómo se vacía la subsanación.
- `borrarSubsanacion` la llama `EntradaHelper.presentar`: al volver a presentar, lo que el verificador pidió subsanar queda atendido.

## 2. Evento inicial (`InitialEventManagerImpl`)

- `createRegistroEntrada` compone el solicitante y el interesado del asiento (`nombre + " " + apellidos` y `dni`) con `personaSolicitante` y `personaInteresada`, que **ya** rellena el motor al crear el expediente y completa la entrada de datos (`modelo.md` §2.1). El `InitialEventManagerImpl` **MUST NOT** tocarlas.
- En el modo representación `personaInteresada` nace vacía: el estado de entrada de datos **MUST** pedir y validar su nombre, apellidos y DNI (panel común `persona-interesada` y su acción de `onLoad`, `vistas.md` §3.1), o el asiento saldrá sin interesado.
- **MUST** fijar el estado inicial según `presentadoEnPapel`: `States.Entrada.PENDIENTE_DOCUMENTO_ESCANEADO` en papel, `States.Entrada.ENTRADA_DATOS` si no (`phaseeventmanager.md` §2.1).

## 3. El documento (`documentospdf/solicitud.xml`)

- Un XML de definición con los datos del expediente como `nombreCampo="self.<campo>"` (`documentos.md`); el build genera la constante `TipoDocumentoPdf.SOLICITUD` con la que el trigger lo pide (§4.3), y el PDF se dibuja en runtime al pedirlo. Qué hacer cuando hay impreso oficial: `documentos.md` §1.
- **MUST** dejar en el documento un hueco para la firma del usuario marcado con `campoFirma` (un `<texto rowSpan="4" campoFirma="firmaSolicitante">` al final, `documentos.md` §2.10). Ese nombre es el que usas en §5, y **MUST** ser el mismo en la `<action-method>` de AutoFirma y en el trigger (`recetas/firma.md` §1.3 y §1.5).
- El registro de entrada **no** necesita nada del documento: la portada con el número de registro, la fecha y el sello la genera él y la antepone al PDF firmado (§5.3).

## 4. Estado `ENTRADA_DATOS`: teclear los datos y generar el PDF

### 4.1 Vista (`entrada/views.xml`)

```xml
<form state="ENTRADA_DATOS" profile="CREADOR">
    <include-panels>
        -subsanacion
        -datos-interesado
        datos-solicitud
        justificante-upload
    </include-panels>
    <footer>
        <buttons-left>
            <button name="DELETE" colSpan="2" css="btn-danger" outline="true" icon="trash" title="Borrar el expediente"
                    onClick="subsysTramitador-event-action" prompt="¿Está seguro que desea borrar el expediente?"/>
        </buttons-left>
        <buttons-right>
            <button name="GUARDAR_DATOS" colSpan="2" title="Siguiente" onClick="subsysTramitador-event-action"/>
        </buttons-right>
    </footer>
</form>
<form state="ENTRADA_DATOS">
    <include-panels>
        -subsanacion
        -datos-interesado
        -datos-solicitud
        -justificante-view
    </include-panels>
    <panel name="avisoEstadoExpediente" colSpan="12" showFrame="false">
        <help variant="info" colSpan="12">La solicitud está pendiente de que el interesado complete y guarde sus datos</help>
    </panel>
    <footer>
        <buttons-left/>
        <buttons-right>
            <button name="EXIT" colSpan="2" title="Salir" onClick="subsysTramitador-event-action"/>
        </buttons-right>
    </footer>
</form>
```

- El form del `CREADOR` incluye editables los paneles de datos y el de subida del anexo (`widget="binary-link"`, `vistas.md` §6); el genérico los incluye todos con `-`, solo `EXIT` y el panel `avisoEstadoExpediente` (`vistas.md` §6.1).
- `-subsanacion` (panel común) va el primero en los forms de la fase `ENTRADA` en los que se corrige la solicitud: solo se ve cuando el verificador ha pedido subsanar, y le dice al usuario qué tiene que corregir.
- El form del `CREADOR` **MUST NOT** llevar el botón `BACK`: en este estado es solo del papel, y va en el form `profile="TRAMITADOR"` (§5.5).
- Los paneles de datos viven en el form plantilla de la raíz (`vistas.md` §1); los comunes, en `tramites/shared/template-views.xml` (`vistas.md` §3.2).

### 4.2 Validator (`entrada/StateEventValidatorImpl.kt`)

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

@BeanValidationRulesForStateAndEvent
fun getForStateEntradaDatosInEventBack(): BeanValidationRules = rules { }
```

- Aquí van **todos** los campos que el usuario teclea o sube en este estado, porque el `rules { }` es la whitelist del evento (`validator.md` §1): un campo sin `field(...)` no se copia del request.
- Las personas que nacen vacías en papel o en representación se validan aquí, anidadas (`modelo.md` §2.1).
- **MUST NOT** dar `field(...)` a `pdfSolicitud`, `pdfSolicitudFirmada` ni `pdfJustificanteRegistroEntrada`: en este evento los rellena el servidor o ya vienen de un evento anterior (§4.3, §5.3 y §5.5). Un `field` sobre ellos deja que el cliente dicte qué documento se registra.
- `DELETE` no necesita método (`validator.md` §5); el `BACK` de este estado sí, vacío.

### 4.3 Trigger (`entrada/PhaseEventManagerImpl.java`)

```java
@Inject
EntradaHelper entradaHelper;

@WhenEvent
public void triggerGuardarDatos(MiTramiteV1 exp, MiTramiteV1 original, EventContext eventContext) throws BusinessException {
    DocumentoPdf solicitudPdf = exp.getDocumentoPdf(MiTramiteV1.TipoDocumentoPdf.SOLICITUD);
    exp.setPdfSolicitud(MetaFileHelper.createMetaFile(solicitudPdf));

    if (Boolean.TRUE.equals(original.getPresentadoEnPapel())) {
        // Papel (§5.5): ya la firmó a mano quien la entregó, así que se presenta con el escaneado
        entradaHelper.presentar(exp, MiTramiteV1Util.CAMPOS_ENTRADA, List.of(exp.getJustificante()), eventContext);
        eventContext.updateState(States.Verificacion.PENDIENTE_VERIFICACION);
    } else {
        exp.setPdfSolicitudFirmada(null);
        eventContext.updateState(States.Entrada.PENDIENTE_PRESENTACION);
    }
}

@WhenEvent
public void triggerDelete(MiTramiteV1 exp, MiTramiteV1 original, EventContext eventContext) throws BusinessException {
    // El motor lo llama justo antes de borrar: aquí solo van las guardas de quién puede borrar
    MiTramiteV1Util.exigeSerElCreador(exp, "Solo puede borrar sus propias solicitudes");
}
```

- Genera el PDF con los datos recién validados (`phaseeventmanager.md` §6.1) y decide por `original.getPresentadoEnPapel()`.
- Telemáticamente **MUST NOT** crear aquí el registro de entrada: el documento aún no está firmado y el usuario aún no lo ha visto. Un registro asienta lo que el usuario presentó, no un borrador.
- Telemáticamente **MUST** vaciar `pdfSolicitudFirmada`: si el estado se alcanzó por `BACK` o por una subsanación, el firmado anterior ya no vale (`FirmaPdf` lo rechazaría contra el nuevo original).
- En papel **MUST NOT** vaciarlo: `pdfSolicitudFirmada` es el escaneado que se va a registrar.
- Lo que el tipo haga además (fechar la solicitud, limpiar campos que el modo elegido no pide, anexar el justificante al PDF) va antes de generar el PDF (`phaseeventmanager.md` §6.1).

## 5. Estado `PENDIENTE_PRESENTACION`: revisar, firmar y presentar

### 5.1 Vista

Es, tal cual, la de `recetas/firma.md` §1.3: `-pdfSolicitud` (el visor común del PDF, `vistas.md` §3.2), `firma-solicitud` (el panel común con un panel por situación de firma, **sin guion**), el `onLoad` que rellena `situacionFirma`/`firmaEnServidor`, `BACK` a la izquierda y los dos botones `PRESENTAR` a la derecha. El genérico incluye `-pdfSolicitud`, el panel `avisoEstadoExpediente` y `EXIT`.

- **MUST** enseñar el PDF que se va a firmar (`-pdfSolicitud`), no los campos de datos: el usuario firma un documento y tiene que ver ese documento.
- Los dos `PRESENTAR` llevan `prompt` de confirmación: la presentación no se deshace (§5.4).
- Las acciones `exp-<Code>-…` del `onLoad` y de los botones **MUST** declararse en el `entrada/views.xml` de cada tipo: sus nombres son globales y llevan el `<Code>`.

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
    field(model::getPdfSolicitudFirmada) {
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
private static final String CAMPO_FIRMA_SOLICITUD = "firmaSolicitante";

@WhenEvent
public void triggerPresentar(MiTramiteV1 exp, MiTramiteV1 original, EventContext eventContext) throws BusinessException {
    try {
        EntradaHelper.exigePresentadoEnPapel(original, false);

        // 1. Firma (recetas/firma.md §1.5): solo firma si a quien presenta le corresponde firmar en servidor
        entradaHelper.firmarSolicitudSiEsEnServidor(exp, MiTramiteV1Util.CAMPOS_ENTRADA, CAMPO_FIRMA_SOLICITUD);

        // 2. Registro de entrada del documento firmado + los anexos, resguardo y subsanación atendida
        entradaHelper.presentar(exp, MiTramiteV1Util.CAMPOS_ENTRADA, List.of(exp.getJustificante()), eventContext);

        // 3. Transición: siempre a la verificación
        eventContext.updateState(States.Verificacion.PENDIENTE_VERIFICACION);
    } finally {
        exp.setClaveCertificado(null);
    }
}

/** Lo disparan dos estados: PENDIENTE_PRESENTACION vuelve a los datos, y ENTRADA_DATOS (solo en papel) vuelve al escaneado. */
@WhenEvent
public void triggerBack(MiTramiteV1 exp, MiTramiteV1 original, EventContext eventContext) throws BusinessException {
    exp.setClaveCertificado(null);

    if (EntradaHelper.estaEn(original, States.Entrada.ENTRADA_DATOS)) {
        EntradaHelper.exigePresentadoEnPapel(original, true);
        eventContext.updateState(States.Entrada.PENDIENTE_DOCUMENTO_ESCANEADO);
    } else {
        eventContext.updateState(States.Entrada.ENTRADA_DATOS);
    }
}
```

1. **Firma**: `firmarSolicitudSiEsEnServidor(expediente, campos, nombreCampoFirma)` recalcula la situación de firma del usuario autenticado y, si le corresponde firmar en servidor, firma `pdfSolicitud` y deja el resultado en `pdfSolicitudFirmada`. Con AutoFirma no hace nada: el `pdfSolicitudFirmada` ya llegó validado.
2. **`presentar(expediente, campos, anexos, eventContext)`** hace tres cosas: `eventContext.createRegistroEntrada(pdfSolicitudFirmada, anexos)` (`phaseeventmanager.md` §3), guarda en `pdfJustificanteRegistroEntrada` el resguardo (`getDocumentoResguardoPresentacion()`) y llama a `borrarSubsanacion`. El asiento toma del expediente el solicitante, el interesado y el número de expediente, y devuelve un resguardo con número y fecha de registro seguido del documento firmado.
3. **Transición** con el `States` del tipo: es lo único que el código común no puede hacer, porque cada tipo solo puede nombrar sus propios estados.
4. `claveCertificado` a `null` en un `finally`, y también en `triggerBack` (`recetas/firma.md` §1.5).
5. `EntradaHelper.exigePresentadoEnPapel(original, false)`: `PRESENTAR` es solo del camino telemático. Lanza `IllegalStateException` si llega en un expediente en papel (la vista no se lo ofrece, §5.5).
6. `triggerBack` usa `EntradaHelper.estaEn(original, estado)` y **no** el `switch` sobre `States.INSTANCE.getState(...)`: ese bloque sería idéntico en todos los tipos y CPD rompería el build (`phaseeventmanager.md` §5).
7. `EntradaHelper` se inyecta con `@Inject` de campo; `estaEn` y `exigePresentadoEnPapel` son estáticos.

Restricciones de `createRegistroEntrada`, que es lo que `presentar` llama por dentro:

- **LIMIT**: **uno** por evento; la segunda llamada revienta con "Ya existe un registro de entrada definido". Si un evento registra dos documentos, se concatenan antes en un solo PDF (`DocumentoPdf.anyadirDocumentoPdf`, `phaseeventmanager.md` §6.1) o van como anexos.
- El documento **MUST** ser un PDF no nulo (`IllegalArgumentException` si no lo es). Los anexos admiten cualquier tipo (una foto del justificante), pero cada uno **MUST** tener `fileName`; la lista puede ser `null` o vacía.
- No hay `BusinessException` que capturar: sus fallos son de programación (solicitante sin rellenar, documento que no es PDF, doble llamada), no del usuario.

- ✅ CORRECTO: `entradaHelper.presentar(exp, MiTramiteV1Util.CAMPOS_ENTRADA, List.of(exp.getJustificante()), eventContext)`
- ✅ CORRECTO: `entradaHelper.presentar(exp, MiTramiteV1Util.CAMPOS_ENTRADA, List.of(), eventContext)` (sin anexos)
- ❌ INCORRECTO: `eventContext.createRegistroEntrada(exp.getPdfSolicitudFirmada(), ...)` + `exp.setPdfJustificanteRegistroEntrada(...)` escritos a mano en el trigger (es lo que ya hace `presentar`; repetido en dos tipos, CPD rompe el build, y además se olvida `borrarSubsanacion`)
- ❌ INCORRECTO: un `CamposEntrada` cuyo `getPdfSolicitudFirmada` devuelve `pdfSolicitud` (registra el original sin firmar; el asiento debe llevar lo que el usuario firmó)
- ❌ INCORRECTO: `entradaHelper.presentar(...)` en la rama telemática de `triggerGuardarDatos` (aún no hay firma ni el usuario ha visto el documento)
- ❌ INCORRECTO: `eventContext.updateState(States.Resolucion.PENDIENTE_RESOLUCION)` tras presentar (se salta la fase común `VERIFICACION`)
- ❌ INCORRECTO: resolver `RegistroEntradaService` con `modelServiceFactory` desde el trigger (ya lo hace `EventContext`, que además es quien rellena solicitante, interesado y asunto)

### 5.4 Por qué la presentación no tiene vuelta atrás

Una vez asentado, el registro de entrada tiene número y fecha oficiales y queda en el historial del expediente y en el registro del centro; el expediente no puede "despresentarse".
Si el verificador necesita que el usuario corrija algo, lo devuelve a `ENTRADA` con `VERIFICAR` y el resultado `SUBSANAR` (§6) y el usuario vuelve a recorrer §4 y §5: la nueva presentación crea **otro** asiento, y los dos quedan en el historial.

### 5.5 El camino en papel

El `TRAMITADOR` registra una solicitud entregada en papel (`perfiles.md`): el documento a registrar no es un PDF generado y firmado electrónicamente, sino la solicitud **firmada a mano y escaneada**, que va en el mismo campo `pdfSolicitudFirmada`. Es parte de la fase común, no una variante opcional.

1. El `InitialEventManagerImpl` hace nacer el expediente en `PENDIENTE_DOCUMENTO_ESCANEADO` cuando `presentadoEnPapel` es `true` (§2).
2. En ese estado el `TRAMITADOR` sube el escaneado con el panel común `solicitud-escaneada-upload`; `CONTINUAR` exige que sea un PDF y pasa a `ENTRADA_DATOS`.
3. En `ENTRADA_DATOS` copia los datos de la solicitud con el form `profile="TRAMITADOR"`, incluidas las personas, que en papel nacen vacías (`modelo.md` §2.1). Puede volver al escaneado con `BACK`.
4. `GUARDAR_DATOS` registra directamente con `entradaHelper.presentar(...)` (§4.3): no pasa por `PENDIENTE_PRESENTACION` ni por la firma.
5. Los `trigger*` que comparten los dos modos deciden con `original.getPresentadoEnPapel()`; los que son de un solo modo lo exigen con `EntradaHelper.exigePresentadoEnPapel(original, true|false)`.

```xml
<!-- entrada/views.xml -->
<form state="PENDIENTE_DOCUMENTO_ESCANEADO" profile="TRAMITADOR">
    <include-panels>
        -subsanacion
        solicitud-escaneada-upload
    </include-panels>
    <footer>
        <buttons-left>
            <button name="DELETE" colSpan="2" css="btn-danger" outline="true" icon="trash" title="Borrar el expediente"
                    onClick="subsysTramitador-event-action" prompt="¿Está seguro que desea borrar el expediente?"/>
        </buttons-left>
        <buttons-right>
            <button name="CONTINUAR" colSpan="2" title="Siguiente" onClick="subsysTramitador-event-action"/>
        </buttons-right>
    </footer>
</form>

<form state="ENTRADA_DATOS" profile="TRAMITADOR" onLoad="subsysExpedientes-persona-interesada-onLoad-action">
    <include-panels>
        -subsanacion
        datos-interesado-editable
        datos-solicitud
        justificante-upload
        -solicitud-escaneada-view
    </include-panels>

    <!-- Este form lo ve también el TRAMITADOR que abre un expediente telemático (perfiles.md §3) -->
    <panel name="avisoEntradaDatosTramitador" colSpan="12" showFrame="false">
        <help variant="info" colSpan="12" showIf="!presentadoEnPapel">La solicitud está pendiente de que el interesado complete o corrija los datos y la presente</help>
        <help variant="info" colSpan="12" showIf="presentadoEnPapel">Copie los datos de la solicitud entregada en papel que ha adjuntado escaneada. Al presentarla se registrará su entrada</help>
    </panel>

    <footer>
        <buttons-left>
            <button name="DELETE" showIf="presentadoEnPapel" .../>
            <button name="BACK" showIf="presentadoEnPapel" colSpan="2" outline="true" title="Atrás" onClick="subsysTramitador-event-action"/>
        </buttons-left>
        <buttons-right>
            <button name="GUARDAR_DATOS" showIf="presentadoEnPapel" colSpan="4" title="Presentar la solicitud" onClick="subsysTramitador-event-action"
                    prompt="Va a presentar la solicitud entregada en papel. Una vez presentada no podrá modificarla"/>
        </buttons-right>
    </footer>
</form>
```

- Los botones y el aviso de papel del form `profile="TRAMITADOR"` llevan `showIf="presentadoEnPapel"`: el `TRAMITADOR` también cae en este form cuando abre un expediente telemático en `ENTRADA_DATOS` (p. ej. el que acaba de devolver para subsanar), y ahí no le toca hacer nada (`perfiles.md` §3).
  El panel editable de la persona ya declara `presentadoEnPapel` como `<field hidden="true"/>`, que es lo que hace que la condición se evalúe (`vistas.md` §6.2).

```kotlin
@BeanValidationRulesForStateAndEvent
fun getForStatePendienteDocumentoEscaneadoInEventContinuar(): BeanValidationRules = rules {
    field(model::getPdfSolicitudFirmada) {
        +Required()
        +FileType(listOf("application/pdf"))
        +FileMaxSize(10, SizeUnit.MB)
    }
}
```

```java
/** Solo en papel: adjuntada la solicitud escaneada, se pasa a copiar sus datos. */
@WhenEvent
public void triggerContinuar(MiTramiteV1 exp, MiTramiteV1 original, EventContext eventContext) throws BusinessException {
    EntradaHelper.exigePresentadoEnPapel(original, true);

    eventContext.updateState(States.Entrada.ENTRADA_DATOS);
}
```

- `PENDIENTE_DOCUMENTO_ESCANEADO` lleva además su form genérico (solo `EXIT` y `avisoEstadoExpediente`), como todo estado.
- `CONTINUAR` es el **único** evento en el que `pdfSolicitudFirmada` entra en la whitelist sin pasar por `FirmaPdf`: lo sube el `TRAMITADOR`, y el validador exige que sea un PDF (obligatorio, `application/pdf`, 10 MB como máximo).
- El mismo `GUARDAR_DATOS` lleva título distinto en cada form: «Siguiente» para el `CREADOR`, «Presentar la solicitud» con `prompt` para el `TRAMITADOR`, que es quien presenta con él.
- El form `profile="CREADOR"` de `ENTRADA_DATOS` **MUST NOT** llevar botones ni avisos del papel (`perfiles.md` §3).

## 6. Fase `VERIFICACION`: el estado `PENDIENTE_VERIFICACION`

Toda presentación acaba aquí. El `TRAMITADOR` comprueba la solicitud y dice una de dos cosas: está **correcta** y pasa a la fase propia del tipo, o hay que **subsanarla** y vuelve a `ENTRADA`. **MUST NOT** aceptarse ni rechazarse aquí la solicitud: eso es de la fase propia.

### 6.1 Vista (`verificacion/views.xml`)

```xml
<form state="PENDIENTE_VERIFICACION" profile="TRAMITADOR">
    <include-panels>
        -datos-interesado
        -datos-solicitud-view
        -justificante-view
        -pdfSolicitudFirmada        <!-- lo que se presentó: la firmada o, en papel, la escaneada -->
        verificacion                <!-- sin guion: resultadoVerificacion y textoSubsanacion se editan -->
    </include-panels>
    <footer>
        <buttons-left/>
        <buttons-right>
            <button name="VERIFICAR" colSpan="2" title="Siguiente" onClick="subsysTramitador-event-action"/>
        </buttons-right>
    </footer>
</form>
<form state="PENDIENTE_VERIFICACION">
    <include-panels>
        -datos-interesado
        -datos-solicitud-view
        -pdfJustificanteRegistroEntrada   <!-- su resguardo sellado, con número de registro -->
    </include-panels>
    <panel name="avisoEstadoExpediente" colSpan="12" showFrame="false">
        <help variant="info" colSpan="12">La solicitud está pendiente de que la secretaría del centro la verifique</help>
    </panel>
    <footer>
        <buttons-left/>
        <buttons-right>
            <button name="EXIT" colSpan="2" title="Salir" onClick="subsysTramitador-event-action"/>
        </buttons-right>
    </footer>
</form>
```

- El **`TRAMITADOR`** ve `pdfSolicitudFirmada` (visor común): el documento tal cual se presentó, que es sobre lo que verifica.
- El **`CREADOR`** cae en el form genérico (el estado ya no es suyo) y ve `pdfJustificanteRegistroEntrada`: el resguardo con el número de registro, que es su prueba de haber presentado.
- La cabecera global ya enseña gratis el historial de estados con los registros de entrada y salida de cada uno (`vistas.md` §3): no hace falta declarar nada más para que el asiento aparezca.

### 6.2 Validator (`verificacion/StateEventValidatorImpl.kt`)

```kotlin
import com.educaflow.subsystem.expedientes.db.ResultadoVerificacionMiTramiteV1 as ResultadoVerificacion
...
@BeanValidationRulesForStateAndEvent
fun getForStatePendienteVerificacionInEventVerificar(): BeanValidationRules = rules {
    field(model::getResultadoVerificacion) {
        +Required()
    }
    field(model::getTextoSubsanacion) {
        +ifValueIn(model::getResultadoVerificacion, listOf(ResultadoVerificacion.SUBSANAR)) {
            +Required()
            +MinLength(10)
            +MaxLength(1000)
        }
    }
}
```

- `resultadoVerificacion` es obligatorio; `textoSubsanacion` solo es obligatorio (entre 10 y 1000 caracteres) cuando el resultado es `SUBSANAR` (`validator.md` §3.2).

### 6.3 Trigger (`verificacion/PhaseEventManagerImpl.java`)

```java
@Inject
VerificacionHelper verificacionHelper;

@WhenEvent
public void triggerVerificar(MiTramiteV1 exp, MiTramiteV1 original, EventContext eventContext) throws BusinessException {
    ResultadoVerificacionMiTramiteV1 resultadoVerificacion = exp.getResultadoVerificacion();
    switch (resultadoVerificacion) {
        case CORRECTO -> {
            exp.setTextoSubsanacion(null);
            eventContext.updateState(States.Resolucion.PENDIENTE_RESOLUCION);   // el primer estado de la fase propia
        }
        case SUBSANAR -> {
            verificacionHelper.avisarDeSubsanacion(exp, exp.getTextoSubsanacion());

            // En papel la persona entregará una solicitud nueva: se vuelve a empezar por el escaneado
            if (Boolean.TRUE.equals(original.getPresentadoEnPapel())) {
                eventContext.updateState(States.Entrada.PENDIENTE_DOCUMENTO_ESCANEADO);
            } else {
                eventContext.updateState(States.Entrada.ENTRADA_DATOS);
            }
        }
        case null -> throw new IllegalArgumentException("Resultado de la verificación no reconocido: " + resultadoVerificacion);
    }
}
```

- `avisarDeSubsanacion(expediente, textoSubsanacion)` avisa por correo al solicitante (`personaSolicitante`) de lo que tiene que subsanar. Es una cortesía: si no hay a quién escribir (en papel no hay correo del solicitante) no envía nada, devuelve `false` y la verificación sigue. **MUST NOT** condicionar la transición a su resultado.
- Con `SUBSANAR`, `resultadoVerificacion` y `textoSubsanacion` **se conservan**: el panel `-subsanacion` de la fase `ENTRADA` enseña el texto, y los vacía `borrarSubsanacion` cuando se vuelve a presentar (§5.3).
- Con `CORRECTO` se vacía el texto que hubiera quedado tecleado.
- El switch **MUST** quedarse en cada tipo: el destino de `CORRECTO` es un estado de su fase propia, y solo él puede nombrar su `States`.
- Lo que el tipo haga además (guardas de quién puede verificar, fecha y autor de la verificación) va al principio del trigger.

## 7. Checklist

- [ ] Máquina: las dos fases comunes tal cual (`ENTRADA` con sus tres estados, `VERIFICACION` con `PENDIENTE_VERIFICACION`); presentar transita siempre a `VERIFICACION` / `PENDIENTE_VERIFICACION`.
- [ ] Modelo: los campos de datos, los anexos y los cinco comunes con su nombre exacto (`pdfSolicitud`, `pdfSolicitudFirmada`, `pdfJustificanteRegistroEntrada`, `resultadoVerificacion`, `textoSubsanacion`); enum `ResultadoVerificacion<Code>` con `CORRECTO`/`SUBSANAR`; sin redeclarar `personaSolicitante`/`personaInteresada`/`claveCertificado`.
- [ ] `<Code>Util`: `CAMPOS_ENTRADA` y `borrarSubsanacion`.
- [ ] Evento inicial: fija el estado según `presentadoEnPapel` y **no** toca las personas; la entrada de datos pide y valida las que nacen vacías en el modo del trámite (`modelo.md` §2.1).
- [ ] Documento: XML en `documentospdf/` con el hueco de la firma marcado con `campoFirma`; mismo nombre de campo de firma en la `<action-method>` y en el trigger.
- [ ] `ENTRADA_DATOS`: forms `CREADOR`, `TRAMITADOR` y genérico, con `-subsanacion`; validator con **todos** los campos tecleados y **ninguno** de los PDF; trigger que genera `pdfSolicitud` y, telemáticamente, vacía el firmado y transita sin registrar; en papel, `entradaHelper.presentar`.
- [ ] `PENDIENTE_PRESENTACION`: vista con `-pdfSolicitud` y `firma-solicitud`, validator de `recetas/firma.md` §1; trigger con `exigePresentadoEnPapel(original, false)`, `firmarSolicitudSiEsEnServidor`, `presentar`, transición y `finally` que vacía la clave; `triggerBack` vacía la clave y decide con `EntradaHelper.estaEn`.
- [ ] `PENDIENTE_DOCUMENTO_ESCANEADO`: form `TRAMITADOR` con `solicitud-escaneada-upload`, validator del PDF escaneado (`validator.md` §3.2), `triggerContinuar` con `exigePresentadoEnPapel(original, true)`.
- [ ] `PENDIENTE_VERIFICACION`: el `TRAMITADOR` ve `-pdfSolicitudFirmada` y edita `verificacion`; el genérico, `-pdfJustificanteRegistroEntrada`; validator de `resultadoVerificacion` y `textoSubsanacion` (`validator.md` §3.2); trigger con `avisarDeSubsanacion` y el destino según el resultado y el modo.

## 8. Anti-patrones

- **MUST NOT** registrar en la rama telemática de `GUARDAR_DATOS` ni registrar `pdfSolicitud`: el asiento lleva el documento **firmado**, y la firma ocurre en `PRESENTAR`.
- **MUST NOT** dar `field(...)` en el validator a los PDF que rellena el servidor (`pdfSolicitud`, `pdfJustificanteRegistroEntrada`): abriría la puerta a que el cliente dicte qué se registra (`k-secure-coding`).
- **MUST NOT** llamar dos veces a `createRegistroEntrada` (ni a `entradaHelper.presentar`) en el mismo evento; concatena o anexa.
- **MUST NOT** presentar sin haber pedido y validado las personas que nacen vacías: el asiento saldría sin solicitante o sin interesado.
- **MUST NOT** guardar `getDocumentoOriginalFirmado()` como resguardo: el resguardo es `getDocumentoResguardoPresentacion()`, y ya lo guarda `presentar`.
- **MUST NOT** hablar con `subsystem/registroentradasalida` desde el trámite: la única puerta es `eventContext.createRegistroEntrada`, a través de `entradaHelper.presentar`.
- **MUST NOT** saltarse la firma "porque este trámite no la necesita": el registro asienta un documento firmado, y firmar no cuesta nada al trámite (`recetas/firma.md` §1 decide en servidor o AutoFirma según el certificado del usuario). La única excepción es el papel (§5.5), donde la firma es la manuscrita del escaneado.
- **MUST NOT** cambiar los nombres de las fases, estados, eventos o campos comunes, ni saltarse `VERIFICACION`: los paneles y el código común cuentan con ellos, y un tercer trámite los copia tal cual.
- **MUST NOT** copiar a la carpeta del tipo lo que ya hacen `EntradaHelper`, `VerificacionHelper` o las reglas comunes: CPD rompe el build.
- **MUST NOT** aceptar ni rechazar la solicitud en `VERIFICAR`: solo dice si está correcta o hay que subsanarla.
