---
type: design
---

# Diseño: Subsistema Firmas

**Objetivo:** Permitir a sistemas externos solicitar la firma digital de uno o varios documentos PDF a un usuario, gestionar el ciclo de vida de la solicitud (pendiente → firmada/rechazada), validar la firma del lado servidor y notificar al sistema solicitante el resultado.
**Capa:** subsystem/firmas
**Análisis de origen:** .sdd/drafts/2026-05-10_17-00_firmas-documentos/analysis_01/analysis.md
**Skills necesarios para la implementación:** k-sistemas, k-vistas, k-seguridad

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/firmas/domains/TareaFirma.xml` | Crear | k-sistemas (modelos.md) | Entidad TareaFirma + enum EstadoTareaFirma |
| `subsystem/firmas/domains/DocumentoFirma.xml` | Crear | k-sistemas (modelos.md) | Entidad DocumentoFirma |
| `subsystem/firmas/service/TareaFirmaService.java` | Crear | k-sistemas (servicios.md) | Interfaz del servicio de tareas de firma |
| `subsystem/firmas/service/TareaFirmaInsertDTO.java` | Crear | k-sistemas (servicios.md) | DTO de inserción con invariantes de constructor |
| `subsystem/firmas/service/TareaFirmaNotifier.java` | Crear | k-sistemas (servicios.md) | Interfaz de callback que el sistema solicitante implementa |
| `subsystem/firmas/service/impl/TareaFirmaServiceImpl.java` | Crear | k-sistemas (servicios.md) | Implementación del servicio |
| `subsystem/firmas/controller/TareaFirmaController.java` | Crear | k-sistemas (controladores.md) | Controlador con métodos `@CallMethod` invocados desde las vistas |
| `subsystem/firmas/views/firma-pendiente.xml` | Crear | k-vistas (forms.md, grids.md, actions.md) | Vistas y acciones del estado PENDIENTE (flujo de resolución) |
| `subsystem/firmas/views/firma-firmado.xml` | Crear | k-vistas (forms.md, grids.md) | Vistas read-only del estado FIRMADO |
| `subsystem/firmas/views/firma-rechazado.xml` | Crear | k-vistas (forms.md, grids.md) | Vistas read-only del estado RECHAZADO |
| `subsystem/firmas/views/firma-todos.xml` | Crear | k-vistas (forms.md, grids.md) | Vista global de todas las tareas (sin filtrar por estado/usuario) |
| `secretariavirtual/menus/NNN_menuitem_firmas.xml` | Modificar | k-vistas (menus.md) | Entrada de menú "Firmar documentos" con sus 4 hijos (Todos, Pendientes, Firmados, Rechazados) |
| `data-init/input/auth-firmas.xml` | Crear | k-seguridad (auth-task.md) | Permisos de filas: `TareaFirma.firmante` y `DocumentoFirma.propio` |
| `data-init/input-config.xml` | Modificar | k-seguridad (auth-task.md) | Registrar `auth-firmas.xml` en el binding de carga inicial |

## Pasos

### Paso 1 — Dominios

Crear los dos ficheros XML de dominio del subsistema. Cubren el modelo completo (entidades, enumerados, relaciones, finders).

**Fichero `subsystem/firmas/domains/TareaFirma.xml`** (XML completo):

```xml
<domain-models xmlns="http://axelor.com/xml/ns/domain-models"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://axelor.com/xml/ns/domain-models https://axelor.com/xml/ns/domain-models/domain-models_8.1.xsd">

    <module name="firmas" package="com.educaflow.subsystem.firmas.db"/>
    <entity name="TareaFirma" >
        <many-to-one name="firmante" ref="com.axelor.auth.db.User" required="true" />
        <one-to-many name="documentosFirma" ref="DocumentoFirma" mappedBy="tareaFirma"  />
        <datetime name="fechaSolicitud" title="Fecha de la solicitud" required="true" />
        <datetime name="fechaResolucion" title="Fecha de la resolución" />
        <boolean name="firmaRapida" title="Firma rápida" help="La firma se permite que se haga inmediatamente sin intervención del usuario si existe su certificado" />
        <enum name="estadoTareaFirma" ref="EstadoTareaFirma" title="Estado" required="true" />
        <string name="motivoFirma" required="true" />
        <string name="motivoRechazo" large="true" multiline="true" title="Motivo del rechazo de la firma de los documentos" />
        <string name="fqcnFirmaNotifier" title="FQCN donde notificar" />
        <string name="fqcnCallBackData" title="FQCN del datos de vuelta"  />
        <string name="callBackData" large="true" multiline="true" title="Datos de vuelta" />
        <decimal name="x" precision="8" scale="2" required="true"/>
        <decimal name="y" precision="8" scale="2" required="true"/>
        <decimal name="width" precision="8" scale="2" title="Ancho" required="true"/>
        <decimal name="height" precision="8" scale="2" title="Alto" required="true"/>
        <integer name="page" title="Página" required="true"/>
    </entity>

    <enum name="EstadoTareaFirma">
        <item name="PENDIENTE" title="Pendiente de firmar" description="Está pendiente la firma de los documentos"/>
        <item name="FIRMADO" title="Firmado" description="Están firmados los documentos"/>
        <item name="RECHAZADO" title="Rechazada la firma" description="Se ha rechazado la firma de los documentos"/>
    </enum>

</domain-models>
```

**Fichero `subsystem/firmas/domains/DocumentoFirma.xml`** (XML completo):

```xml
<domain-models xmlns="http://axelor.com/xml/ns/domain-models"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://axelor.com/xml/ns/domain-models https://axelor.com/xml/ns/domain-models/domain-models_8.1.xsd">

    <module name="firmas" package="com.educaflow.subsystem.firmas.db"/>
    <entity name="DocumentoFirma" >
        <many-to-one name="tareaFirma" ref="com.educaflow.subsystem.firmas.db.TareaFirma" required="true" title="Tarea de firma"/>
        <many-to-one name="documentoOriginal" ref="com.axelor.meta.db.MetaFile" required="true" title="Documento original a firmar"/>
        <many-to-one name="documentoFirmado" ref="com.axelor.meta.db.MetaFile"  title="Documento firmado"/>
    </entity>

</domain-models>
```

**Verificación:** `./gradlew clean build --info` compila y genera las clases `com.educaflow.subsystem.firmas.db.TareaFirma`, `DocumentoFirma`, `EstadoTareaFirma`.

### Paso 2 — Servicios

#### Clase: `com.educaflow.subsystem.firmas.service.TareaFirmaInsertDTO`

`record` Java que transporta los datos para crear una nueva tarea de firma. Contiene invariantes de infraestructura en su constructor compacto (no son reglas de negocio: son contratos del DTO que un sistema cliente debe respetar antes de pasarle datos al servicio).

```java
public record TareaFirmaInsertDTO(
    User firmante,
    List<MetaFile> documentos,
    String motivoFirma,
    Rectangulo areaFirma,
    Class<? extends TareaFirmaNotifier> firmaNotifierClass,
    Object callBackData
);
```

Constructor compacto:
```java
public TareaFirmaInsertDTO { ... }
//   Aplica los siguientes invariantes (lanzan NullPointerException o IllegalArgumentException, NO BusinessException — son
//   contratos del DTO, no reglas de negocio que deban verse en UI):
//     - INV-DTO-1: firmante, documentos, motivoFirma, areaFirma, firmaNotifierClass no pueden ser null (Objects.requireNonNull).
//     - INV-DTO-2: documentos no puede estar vacío.
//     - INV-DTO-3: motivoFirma no puede estar en blanco (isBlank).
//     - INV-DTO-4: cada MetaFile de documentos no puede ser null y debe ser un PDF (MetaFileHelper.isPdf).
```

#### Interfaz: `com.educaflow.subsystem.firmas.service.TareaFirmaNotifier`

Callback que un sistema cliente implementa para recibir la notificación cuando una tarea cambia a FIRMADO o RECHAZADO. La implementación se resuelve por FQCN almacenado en la entidad y se obtiene vía `Beans.get(...)`.

```java
public interface TareaFirmaNotifier {
    void notify(TareaFirma tareaFirma, Object callBackData);
}
```

#### Interfaz: `com.educaflow.subsystem.firmas.service.TareaFirmaService`

```java
public interface TareaFirmaService extends ModelService<TareaFirma> {
    TareaFirma insert(TareaFirmaInsertDTO tareaFirmaInsertDTO) throws BusinessException;
    TareaFirma marcarComoFirmada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal);
    TareaFirma marcarComoRechazada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal);
    Optional<BusinessMessages> validarDocumentosFirmados(TareaFirma tareaFirma);
}
```

#### Implementación: `com.educaflow.subsystem.firmas.service.impl.TareaFirmaServiceImpl`

Extiende `DefaultModelService<TareaFirma>` e implementa `TareaFirmaService`. Es descubierta automáticamente por `ModelServiceFactory` (paquete `service.impl.*ServiceImpl`); **no se crea módulo Guice**.

```java
public TareaFirmaServiceImpl(Class<TareaFirma> model, Repository repository);
//   Constructor obligatorio del patrón ModelService. Pasa TareaFirma.class y el repository al super.
```

```java
@Override
public TareaFirma insert(TareaFirmaInsertDTO tareaFirmaInsertDTO);
//   Construye una nueva TareaFirma a partir del DTO y la persiste.
//   Aplica:
//     - RN-1 (Inicialización al insertar): asigna firmante, fechaSolicitud=now, estadoTareaFirma=PENDIENTE,
//       motivoFirma del DTO, motivoRechazo=null.
//     - RN-2 (Clonado de documentos originales): para cada MetaFile del DTO, crea un DocumentoFirma con
//       documentoOriginal = MetaFileUtil.cloneMetaFile(documento) (no se referencia el original — se clona
//       para que el sistema cliente no pueda alterarlo después). El campo documentoFirmado queda null.
//     - RN-3 (Persistencia del callback como FQCN+JSON): guarda fqcnFirmaNotifier = nombre de la clase del
//       notifier; si callBackData != null, guarda fqcnCallBackData = clase del objeto y callBackData =
//       JsonUtil.toJson(objeto); si es null, ambos campos se ponen a null.
//     - RN-4 (Persistencia del área de firma): copia x/y/width/height del Rectangulo del DTO al BigDecimal
//       de la entidad. La página NO se asigna desde el DTO en insert (el campo page del modelo se rellena
//       más tarde — ver nota de unificación al final).
//   Llama a super.insert(tareaFirma) para persistir mediante el ModelService.
```

```java
@Override
public TareaFirma marcarComoFirmada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal);
//   Aplica la transición PENDIENTE → FIRMADO sobre una tarea ya cargada con documentosFirma.documentoFirmado
//   poblados desde el flujo de UI.
//   Aplica:
//     - RN-5 (Transición a FIRMADO): asigna estadoTareaFirma=FIRMADO, motivoRechazo=null,
//       fechaResolucion=now.
//     - RN-6 (Persistencia con detección de cambios): llama a super.update(tareaFirma, tareaFirmaOriginal)
//       — la versión original se usa por el ModelService para auditoría/control de concurrencia.
//     - RN-7 (Notificación al sistema solicitante): invoca fireActionRule_NotificarFirmaResuelta(tareaFirma).
```

```java
@Override
public TareaFirma marcarComoRechazada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal);
//   Aplica la transición PENDIENTE → RECHAZADO. El motivoRechazo viene ya asignado en tareaFirma desde
//   el flujo de UI (el cliente lo escribe en el formulario y lo envía).
//   Aplica:
//     - RN-8 (Transición a RECHAZADO): asigna estadoTareaFirma=RECHAZADO, fechaResolucion=now.
//       NO se sobrescribe motivoRechazo (lo aporta la UI).
//     - RN-6 (Persistencia con detección de cambios): super.update(tareaFirma, tareaFirmaOriginal).
//     - RN-7 (Notificación al sistema solicitante): fireActionRule_NotificarFirmaResuelta(tareaFirma).
```

```java
@Override
public Optional<BusinessMessages> validarDocumentosFirmados(TareaFirma tareaFirma);
//   Validación de servidor que se ejecuta ANTES de marcarComoFirmada (la dispara el action-group del botón
//   "Firmar todos" tras la subida de los PDF firmados por AutoFirma). Comprueba que cada PDF firmado se
//   corresponde con su original y está firmado con el DNI del firmante.
//   Aplica:
//     - V-006 (Validez de la firma criptográfica): para cada DocumentoFirma de la tarea, obtiene el
//       DocumentoPdf de documentoOriginal y de documentoFirmado (MetaFileHelper.getDocumentoPdf), llama a
//       DocumentoPdfUtil.validateFirmaPdf(original, firmado, firmante.getDni()) y, si retorna error,
//       acumula un BusinessMessage cuyo target es el fileName del documentoFirmado y cuyo mensaje debe
//       transmitir: el motivo concreto devuelto por el validador (firma inválida, contenido alterado,
//       DNI no coincide, etc.).
//   Devuelve Optional.empty() si no hay errores; Optional.of(BusinessMessages) si los hay.
```

```java
@SuppressWarnings("unchecked")
private void fireActionRule_NotificarFirmaResuelta(TareaFirma tareaFirma);
//   Effecto secundario común a las transiciones a FIRMADO y a RECHAZADO. Resuelve dinámicamente el
//   notifier del sistema cliente y le notifica el resultado.
//   Aplica:
//     - RN-9 (Resolución dinámica del notifier): Class.forName(tareaFirma.getFqcnFirmaNotifier()) — si la
//       clase no existe, encapsula ClassNotFoundException en RuntimeException (es un error de configuración
//       del sistema cliente, no del usuario).
//     - RN-10 (Inyección del notifier vía Guice): obtiene la instancia con Beans.get(firmaNotifierClass)
//       — el notifier debe estar registrado en el contenedor de DI del sistema cliente.
//     - RN-11 (Deserialización del callBackData): si fqcnCallBackData != null, Class.forName del FQCN y
//       JsonUtil.fromJson(callBackData, clase) para reconstruir el objeto; si es null, callBackData=null.
//     - RN-12 (Invocación del callback): notifier.notify(tareaFirma, callBackData). Cualquier excepción
//       lanzada por el notifier se propaga al caller (no se captura).
```

**Verificación:** las clases compilan; `ModelServiceFactory.resolve(TareaFirma.class)` devuelve una instancia de `TareaFirmaServiceImpl` (no requiere `@Bind` ni módulo Guice).

### Paso 3 — Controladores

#### Clase: `com.educaflow.subsystem.firmas.controller.TareaFirmaController`

Inyecta `ModelServiceFactory` para resolver `TareaFirmaService` en cada método. Los métodos siguen el patrón `@CallMethod` de Axelor (`ActionRequest`/`ActionResponse`).

```java
@Inject private ModelServiceFactory modelServiceFactory;
```

```java
@CallMethod
public void firmarDocumentosConAutoFirma(ActionRequest actionRequest, ActionResponse actionResponse);
//   Punto de entrada que la vista invoca para iniciar el flujo de firma con AutoFirma desktop.
//   NO es transaccional (no escribe en BD): solo construye el payload para AutoFirma y se lo devuelve al
//   cliente vía actionResponse.
//   Aplica:
//     - RN-13 (Construcción del payload de AutoFirma): carga la TareaFirma por id (JpaRepository.find);
//       construye un AutoFirma con: rectángulo (x,y,width,height del modelo, convertidos a float),
//       pageNumber=tareaFirma.getPage(), dni=tareaFirma.getFirmante().getDni().
//     - RN-14 (Mapeo source→target por documento): para cada índice i de documentosFirma, registra
//       autofirma.addSourceTargetField("documentosFirma[i].documentoOriginal",
//       "documentosFirma[i].documentoFirmado") — AutoFirma firmará el documentoOriginal y devolverá el
//       resultado en documentoFirmado.
//     - RN-15 (Envío al cliente): AutoFirma.sendToActionResponse(autofirma, actionResponse) inyecta en
//       la respuesta los datos que el cliente JS usará para invocar la app desktop.
```

```java
@CallMethod
@Transactional
public void marcarComoFirmada(ActionRequest actionRequest, ActionResponse actionResponse);
//   Endpoint @Transactional. Resuelve TareaFirmaService; obtiene tareaFirmaOriginal vía
//   ActionRequestHelper.getOriginalModel(); construye AllowProperties que solo permite escritura en
//   documentosFirma[*].documentoFirmado (campos enviados por AutoFirma) — todos los demás campos son
//   ignorados aunque vengan en el request; obtiene tareaFirma con esa lista blanca; delega en
//   tareaFirmaService.marcarComoFirmada(tareaFirma, tareaFirmaOriginal).
//   Aplica:
//     - RN-16 (Lista blanca de propiedades modificables al firmar): solo
//       documentosFirma[*].documentoFirmado puede llegar del cliente; el resto se descarta para impedir
//       que un cliente malicioso altere otros campos en este endpoint.
```

```java
@CallMethod
@Transactional
public void marcarComoRechazada(ActionRequest actionRequest, ActionResponse actionResponse);
//   Igual al anterior pero la lista blanca permite escritura solo en motivoRechazo. Delega en
//   tareaFirmaService.marcarComoRechazada(tareaFirma, tareaFirmaOriginal).
//   Aplica:
//     - RN-17 (Lista blanca de propiedades modificables al rechazar): solo motivoRechazo puede llegar
//       del cliente.
```

```java
@CallMethod
public void validarDocumentosFirmados(ActionRequest actionRequest, ActionResponse actionResponse);
//   Endpoint NO transaccional (solo valida, no escribe). Lista blanca igual a marcarComoFirmada
//   (documentosFirma[*].documentoFirmado). Llama a tareaFirmaService.validarDocumentosFirmados; si
//   devuelve BusinessMessages, los envía al cliente como error vía
//   actionResponseHelper.doResponseBusinessMessagesAsError(...).
//   Aplica:
//     - V-006 — delega en el servicio (ver Paso 2).
```

**Verificación:** las cuatro acciones `action-method` declaradas en `firma-pendiente.xml` (Paso 4) resuelven contra los métodos de esta clase.

### Paso 4 — Vistas

#### Fichero `subsystem/firmas/views/firma-pendiente.xml`

Vista del estado PENDIENTE — la única editable. Aplica el **patrón de máquina de estados con `pasoActual`** (Patrón 4 de k-vistas) para guiar al usuario por el flujo de resolución (paso1Inicio → paso2Firmar / paso2Rechazado).

Vistas declaradas:

- `action-view name="subsysFirma.TareaFirma@Pendiente-action"`, model `com.educaflow.subsystem.firmas.db.TareaFirma`.
  - Abre el grid `@Pendiente-grid` y el form `@Pendiente-form`.
  - view-params: `show-toolbar-grid=false`, `show-toolbar-form=false`, `forceEdit=true`, `reload-grid=true`.
  - Domain: `self.estadoTareaFirma= :estadoTareaFirma and firmante.id= :firmanteId`.
  - Contexts: `estadoTareaFirma=PENDIENTE`, `firmanteId=eval:__user__.id`.

- `grid name="subsysFirma.TareaFirma@Pendiente-grid"`, model `TareaFirma`.
  - Atributos: `editable=false`, `edit-icon=false`, `x-selector=none`, `canNew=false`, `canEdit=true`,
    `canDelete=false`, `canSave=false`, `orderBy=fechaSolicitud`, `canEditOnClick=true`.
  - Columnas: `fechaSolicitud`, `firmante`, `motivoFirma`, `estadoTareaFirma`.

- `form name="subsysFirma.TareaFirma@Pendiente-form"`, model `TareaFirma`.
  - Atributos: `canEdit=true`, `canAttach=false`, `canBack=false`, `canBackOnSave=true`, `canDelete=false`,
    `canNew=false`, `canSave=false`, `canCancel=false`, `canMore=false`.
  - `onLoad`: `subsysFirma.TareaFirma@Pendiente-set-pasoActual-paso1Inicio-action` (inicializa la máquina
    de estados al primer paso al abrir el form).
  - `related`: `{"documentosFirma":["documentoOriginal","documentoFirmado"]}` (carga eager de los
    MetaFile para que los iframes los puedan visualizar).
  - Panels:
    - **`tareaFirmaInsertDTO`** (readonly): muestra `motivoFirma`, `fechaSolicitud`, `estadoTareaFirma`,
      `fechaResolucion` (showIf `fechaResolucion!=null`), `pasoActual` (campo virtual que dirige la
      máquina de estados — no existe en el dominio).
    - **`documentosFirma`** (`panel-related`, readonly) → grid `@DocumentoFirma@Pendiente-grid`,
      form `@DocumentoFirma@Pendiente-form`.
    - **`paso1Inicio`** (showIf `pasoActual=='paso1Inicio'`): dos botones — "Rechazar firmar"
      (action `@Pendiente-btnPaso1InicioRechazar-action`) y "Firmar todos los documentos"
      (action `@Pendiente-btnPaso1InicioFirmar-action`).
    - **`paso2Rechazado`** (showIf `pasoActual=='paso2Rechazado'`): campo `motivoRechazo` (widget Text),
      botones "Atrás" y "Finalizar".
    - **`paso2Firmar`** (showIf `pasoActual=='paso2Firmar'`): bloque de ayuda con enlace a AutoFirma__!!,
      botones "Atrás" y "Firmar todos los documentos con AutoFirma__!! y finalizar". Este último ejecuta
      en serie (`serial:`) la acción remota `firmarDocumentosConAutoFirma` y a continuación el grupo
      `@Pendiente-btnPaso2FirmarGuardar-action`.

- `grid name="subsysFirma.TareaFirma.DocumentoFirma@Pendiente-grid"`, model `DocumentoFirma`.
  - Atributos: `edit-icon=false`, `x-selector=none`, `canNew=false`, `canEdit=false`, `canDelete=false`,
    `canSave=false`, `canViewOnClick=true`.
  - Columna: `documentoOriginal.fileName`.

- `form name="subsysFirma.TareaFirma.DocumentoFirma@Pendiente-form"`, model `DocumentoFirma`.
  - Atributos: `canAttach=false`, `canEdit=false`, `canBack=true`, `canDelete=false`, `canNew=false`,
    `canSave=false`, `canMore=false`.
  - `panel-tabs` con dos pestañas:
    - **`tabDocumentoOriginal`**: `<viewer>` con iframe que descarga el MetaFile de `documentoOriginal` por
      `ws/rest/com.axelor.meta.db.MetaFile/${id}/content/download?inline=true`.
    - **`tabDocumentoFirmado`** (showIf `documentoFirmado!=null`): igual con `documentoFirmado`.

Acciones declaradas:

- `action-group "subsysFirma.TareaFirma@Pendiente-btnPaso1InicioRechazar-action"`
  - Propósito: avanzar la máquina de estados a `paso2Rechazado`.
  - Compone: `@Pendiente-set-pasoActual-paso2Rechazado-action`.

- `action-group "subsysFirma.TareaFirma@Pendiente-btnPaso1InicioFirmar-action"`
  - Propósito: avanzar la máquina de estados a `paso2Firmar`.
  - Compone: `@Pendiente-set-pasoActual-paso2Firmar-action`.

- `action-group "subsysFirma.TareaFirma@Pendiente-btnPaso2RechazadoAtras-action"`
  - Propósito: volver al `paso1Inicio` desde el panel de rechazo.
  - Compone: `@Pendiente-set-pasoActual-paso1Inicio-action`.

- `action-group "subsysFirma.TareaFirma@Pendiente-btnPaso2RechazadoGuardar-action"`
  - Propósito: validar en cliente que `motivoRechazo` está informado, llamar al servidor para marcar
    como rechazada, y volver atrás.
  - Compone (en orden): `@Pendiente-Local-validateMarcarComoRechazada-action`,
    `@Pendiente-Remote-marcarComoRechazada-action`, `force-back`.

- `action-group "subsysFirma.TareaFirma@Pendiente-btnPaso2FirmarAtras-action"`
  - Propósito: volver al `paso1Inicio` desde el panel de firma.
  - Compone: `@Pendiente-set-pasoActual-paso1Inicio-action`.

- `action-group "subsysFirma.TareaFirma@Pendiente-btnPaso2FirmarGuardar-action"`
  - Propósito: tras AutoFirma haber subido los PDF firmados, validar la firma en servidor, marcar la
    tarea como firmada, y volver atrás.
  - Compone (en orden): `@Pendiente-Remote-validarDocumentosFirmados-action`,
    `@Pendiente-Remote-marcarComoFirmada-action`, `force-back`.

- `action-condition "subsysFirma.TareaFirma@Pendiente-Local-validateMarcarComoRechazada-action"`
  - Propósito: V-005 en cliente — exige que `motivoRechazo` esté informado al rechazar.
  - Condición: `<check field="motivoRechazo"/>` (Axelor genera el mensaje "Campo obligatorio" del campo).

- `action-record "subsysFirma.TareaFirma@Pendiente-set-pasoActual-paso1Inicio-action"` (model `TareaFirma`)
  - Asigna `pasoActual=paso1Inicio`.

- `action-record "subsysFirma.TareaFirma@Pendiente-set-pasoActual-paso2Rechazado-action"` (model `TareaFirma`)
  - Asigna `pasoActual=paso2Rechazado`.

- `action-record "subsysFirma.TareaFirma@Pendiente-set-pasoActual-paso2Firmar-action"` (model `TareaFirma`)
  - Asigna `pasoActual=paso2Firmar`.

- `action-method "subsysFirma.TareaFirma@Pendiente-Remote-validarDocumentosFirmados-action"`
  - Propósito: invocar `TareaFirmaController.validarDocumentosFirmados`. Cubre V-006 (capa servidor).

- `action-method "subsysFirma.TareaFirma@Pendiente-Remote-marcarComoFirmada-action"`
  - Propósito: invocar `TareaFirmaController.marcarComoFirmada`. Aplica RN-5/6/7.

- `action-method "subsysFirma.TareaFirma@Pendiente-Remote-marcarComoRechazada-action"`
  - Propósito: invocar `TareaFirmaController.marcarComoRechazada`. Aplica RN-6/7/8.

- `action-method "subsysFirma.TareaFirma@Pendiente-Remote-firmarDocumentosConAutoFirma-action"`
  - Propósito: invocar `TareaFirmaController.firmarDocumentosConAutoFirma`. Aplica RN-13/14/15.

#### Fichero `subsystem/firmas/views/firma-firmado.xml`

Vista de solo lectura del estado FIRMADO.

- `action-view name="subsysFirma.TareaFirma@Firmado-action"`. Domain igual que pendiente pero
  `estadoTareaFirma=FIRMADO`.
- `grid name="subsysFirma.TareaFirma@Firmado-grid"` con columnas `fechaSolicitud`, `fechaResolucion`,
  `firmante`, `motivoFirma`, `estadoTareaFirma`. Atributo `groups="admins,users"`.
- `form name="subsysFirma.TareaFirma@Firmado-form"`: panel readonly con los mismos campos del grid +
  `motivoFirma`; `panel-related "documentosFirma"` apuntando a `@DocumentoFirma@Firmado-grid`/`-form`,
  readonly. Botón "Salir" que ejecuta `back`. `related='{"documentosFirma":["documentoFirmado"]}'`.
- `grid/form @DocumentoFirma@Firmado` muestran únicamente `documentoFirmado.fileName` y un viewer
  iframe del PDF firmado.

Sin acciones — toda la vista es read-only.

#### Fichero `subsystem/firmas/views/firma-rechazado.xml`

Igual estructura que firmado pero filtrando por `estadoTareaFirma=RECHAZADO`. El form muestra además
`motivoRechazo` (readonly) y los `panel-related` apuntan a `@DocumentoFirma@Rechazado-grid`/`-form` que
sólo muestran `documentoOriginal` (no hay firmado al estar rechazado).

#### Fichero `subsystem/firmas/views/firma-todos.xml`

Vista global: el `action-view` NO tiene `<domain>`, muestra todas las tareas independientemente del
estado o del firmante. Pensada para administradores. El grid tiene `allowSearchFields=true`. El form
muestra `motivoRechazo` con `showIf="motivoRechazo!=null"` y los documentos con ambas pestañas
(original y firmado, esta última con `showIf="documentoFirmado!=null"`).

Sin acciones — solo navegación.

### Paso 5 — Menús

Modificar `secretariavirtual/menus/NNN_menuitem_firmas.xml` (fichero de menús, ver Patrón 2 de k-vistas).
Añadir el menú raíz "Firmar documentos" con sus 4 hijos:

- `menuitem name="firmarDocumentos-menuitem"` (raíz, sin action), title="Firmar documentos", order=70.
- Hijos (parent=`firmarDocumentos-menuitem`), por orden:
  1. `firmarDocumentos-todos-menuitem` → `subsysFirma.TareaFirma@Todos-action`.
  2. `firmarDocumentos-pendientes-menuitem` → `subsysFirma.TareaFirma@Pendiente-action`.
  3. `firmarDocumentos-firmados-menuitem` → `subsysFirma.TareaFirma@Firmado-action`.
  4. `firmarDocumentos-rechazados-menuitem` → `subsysFirma.TareaFirma@Rechazado-action`.

### Paso 6 — Seguridad

Crear `data-init/input/auth-firmas.xml` con dos `permission` que delimitan el acceso por filas: cada
usuario sólo ve sus propias TareaFirma y sus DocumentoFirma asociadas (cubre V-007 y V-008).

- `permission name="TareaFirma.firmante"`, object `com.educaflow.subsystem.firmas.db.TareaFirma`.
  - Condition JPQL: `self.firmante = ?`, conditionParams `__user__`.
  - `<can create="false" read="true" write="true" remove="false" export="false"/>`.
  - Lectura y escritura permitidas sobre las propias; no se puede crear (la creación pasa por
    `TareaFirmaService.insert` desde sistemas cliente) ni borrar.

- `permission name="DocumentoFirma.propio"`, object `com.educaflow.subsystem.firmas.db.DocumentoFirma`.
  - Condition JPQL: `self.tareaFirma IN (SELECT tf FROM TareaFirma tf WHERE tf.firmante = ?)`,
    conditionParams `__user__`.
  - Mismos `<can>` que el anterior.

Modificar `data-init/input-config.xml` para registrar el nuevo fichero `auth-firmas.xml` en el
binding de carga inicial (junto a `auth.xml`, `auth-common.xml`, etc.) — sin esto los permisos no
se cargan al arrancar.

### Paso 7 — Verificación final

```bash
./gradlew clean build --info
```

Comprobar:
- El build compila sin errores.
- `ModelServiceFactory.resolve(TareaFirma.class)` devuelve `TareaFirmaServiceImpl`.
- Al arrancar la aplicación los permisos `TareaFirma.firmante` y `DocumentoFirma.propio` aparecen
  cargados en BD.
- Los 4 menuitems "Firmar documentos > {Todos, Pendientes, Firmados, Rechazados}" son visibles para
  un usuario autenticado.

---

## Matriz de trazabilidad V-XXX y reglas de negocio → ubicación

| ID    | Capa     | Ubicación                                                                                  | Comentario |
|-------|----------|--------------------------------------------------------------------------------------------|------------|
| V-001 | Modelo   | `TareaFirma.xml` — `<many-to-one name="firmante" required="true">`                          | Firmante obligatorio (NOT NULL en BD). Mensaje generado por Axelor — el alta vía DTO ya lo garantiza. |
| V-002 | Modelo   | `TareaFirma.xml` — `<datetime name="fechaSolicitud" required="true">`                       | Fecha de solicitud obligatoria. Asignada automáticamente por `TareaFirmaServiceImpl.insert` (RN-1). |
| V-003 | Modelo   | `TareaFirma.xml` — `<enum name="estadoTareaFirma" required="true">` + dominio finito        | Estado obligatorio y dentro de {PENDIENTE, FIRMADO, RECHAZADO}. Asignado en `insert` (PENDIENTE), `marcarComoFirmada` (FIRMADO) y `marcarComoRechazada` (RECHAZADO). |
| V-004 | Modelo   | `TareaFirma.xml` — `<string name="motivoFirma" required="true">` + DTO INV-DTO-3            | Motivo de firma obligatorio y no en blanco. Garantizado por el constructor del DTO (no llega vacío al servicio). |
| V-005 | Cliente  | `firma-pendiente.xml` — `action-condition @Pendiente-Local-validateMarcarComoRechazada-action` (`<check field="motivoRechazo"/>`) | `motivoRechazo` obligatorio al rechazar. Bloquea el botón "Finalizar" del paso2Rechazado. |
| V-006 | Servidor | `TareaFirmaServiceImpl.validarDocumentosFirmados` (invocado vía `TareaFirmaController.validarDocumentosFirmados` desde `action-method @Pendiente-Remote-validarDocumentosFirmados-action`) | Cada PDF firmado debe ser firma válida del PDF original con el DNI del firmante. Mensaje incluye el fileName del documento que falla y la causa devuelta por `DocumentoPdfUtil.validateFirmaPdf`. |
| V-007 | Servidor | `auth-firmas.xml` — `permission TareaFirma.firmante` (condition `self.firmante = __user__`) | Cada usuario sólo ve y edita sus propias TareaFirma. |
| V-008 | Servidor | `auth-firmas.xml` — `permission DocumentoFirma.propio` (condition JPQL sobre la tareaFirma) | Cada usuario sólo ve y edita los DocumentoFirma de sus propias tareas. |
| RN-1  | Servidor | `TareaFirmaServiceImpl.insert`                                                              | Inicialización de campos en la creación (firmante, fechaSolicitud=now, estado=PENDIENTE, motivos…). |
| RN-2  | Servidor | `TareaFirmaServiceImpl.insert`                                                              | Clonado de los MetaFile originales (`MetaFileUtil.cloneMetaFile`) — el sistema cliente no puede alterarlos. |
| RN-3  | Servidor | `TareaFirmaServiceImpl.insert`                                                              | Persistencia del callback como FQCN+JSON (`fqcnFirmaNotifier`, `fqcnCallBackData`, `callBackData`). |
| RN-4  | Servidor | `TareaFirmaServiceImpl.insert`                                                              | Persistencia del área de firma (x/y/width/height) desde `Rectangulo` del DTO. |
| RN-5  | Servidor | `TareaFirmaServiceImpl.marcarComoFirmada`                                                   | Transición a FIRMADO + limpieza de `motivoRechazo` + `fechaResolucion=now`. |
| RN-6  | Servidor | `TareaFirmaServiceImpl.marcarComoFirmada` y `marcarComoRechazada`                           | Persistencia con `super.update(tareaFirma, tareaFirmaOriginal)` (auditoría/concurrencia). |
| RN-7  | Servidor | `TareaFirmaServiceImpl.fireActionRule_NotificarFirmaResuelta` (invocado desde ambas transiciones) | Invoca el callback del notifier del sistema solicitante. |
| RN-8  | Servidor | `TareaFirmaServiceImpl.marcarComoRechazada`                                                 | Transición a RECHAZADO + `fechaResolucion=now` (preserva `motivoRechazo` enviado por la UI). |
| RN-9  | Servidor | `TareaFirmaServiceImpl.fireActionRule_NotificarFirmaResuelta`                               | Resolución dinámica del notifier por FQCN (`Class.forName`). |
| RN-10 | Servidor | `TareaFirmaServiceImpl.fireActionRule_NotificarFirmaResuelta`                               | Inyección del notifier por DI (`Beans.get`). |
| RN-11 | Servidor | `TareaFirmaServiceImpl.fireActionRule_NotificarFirmaResuelta`                               | Deserialización del callBackData con `JsonUtil.fromJson` y la clase guardada. |
| RN-12 | Servidor | `TareaFirmaServiceImpl.fireActionRule_NotificarFirmaResuelta`                               | Invocación del callback (`notifier.notify`). |
| RN-13 | Servidor | `TareaFirmaController.firmarDocumentosConAutoFirma`                                         | Construcción del payload de AutoFirma (rectángulo, página, dni). |
| RN-14 | Servidor | `TareaFirmaController.firmarDocumentosConAutoFirma`                                         | Mapeo source→target por documento para que AutoFirma sepa qué firmar y dónde dejar el resultado. |
| RN-15 | Servidor | `TareaFirmaController.firmarDocumentosConAutoFirma`                                         | Inyección del payload en `ActionResponse` (`AutoFirma.sendToActionResponse`). |
| RN-16 | Servidor | `TareaFirmaController.marcarComoFirmada` — `AllowProperties`                                | Lista blanca: solo `documentosFirma[*].documentoFirmado` puede llegar del cliente. |
| RN-17 | Servidor | `TareaFirmaController.marcarComoRechazada` — `AllowProperties`                              | Lista blanca: solo `motivoRechazo` puede llegar del cliente. |
| INV-DTO-1..4 | Infraestructura | `TareaFirmaInsertDTO` constructor compacto                                       | Invariantes del DTO (no son reglas de negocio — son contratos del API del subsistema con sus consumidores). |

**Máquina de estados de `TareaFirma`** (referenciada por V-003, RN-5, RN-8):

| Origen     | Destino    | Disparador                                              | Acción posterior |
|------------|------------|---------------------------------------------------------|--------------------|
| (inicial)  | PENDIENTE  | `TareaFirmaService.insert` desde sistema cliente        | RN-1..4 |
| PENDIENTE  | FIRMADO    | `TareaFirmaController.marcarComoFirmada` (validación V-006 obligatoria previa) | RN-5, RN-6, RN-7 |
| PENDIENTE  | RECHAZADO  | `TareaFirmaController.marcarComoRechazada` (V-005 en cliente) | RN-6, RN-7, RN-8 |

FIRMADO y RECHAZADO son finales — no hay transiciones desde ellos. Las vistas `firma-firmado.xml` y
`firma-rechazado.xml` son completamente read-only.

---

## Notas de unificación

- El campo `page` del modelo `TareaFirma` está marcado `required="true"` pero el método `insert` actual
  no lo asigna desde el DTO ni desde el `Rectangulo` (la página no es parte de `Rectangulo`). Se asume
  que el sistema cliente lo rellena por otra vía o que existe un default en BD; el diseño replica el
  comportamiento del código real sin añadir lógica adicional.
- El campo `firmaRapida` (boolean) está declarado en el modelo pero no se usa en ningún flujo del
  servicio o de las vistas en el código actual. Se mantiene en el dominio como infraestructura para
  un caso de uso futuro de firma automática sin intervención.
