---
type: design
---

# Diseño: Subsistema Firmas

**Objetivo:** Implementar el subsistema reutilizable `firmas` que permite a otros sistemas de la aplicación solicitar a un usuario la firma digital de uno o varios documentos PDF, gestionar el ciclo de vida `PENDIENTE → FIRMADO/RECHAZADO` desde una UI guiada, validar criptográficamente la firma producida con AutoFirma y notificar al sistema solicitante el resultado mediante un callback resuelto dinámicamente por FQCN+JSON.
**Capa:** subsystem/firmas
**Análisis de origen:** .sdd/drafts/2026-05-10_17-00_firmas-documentos/analysis_01/analysis.md
**Skills necesarios para la implementación:** k-sistemas, k-vistas, k-seguridad

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/firmas/db/.gitkeep` | Crear | k-sistemas | Marcador de paquete vacío (las clases JPA las genera Axelor desde el dominio). |
| `subsystem/firmas/domains/TareaFirma.xml` | Crear | k-sistemas | Entidad `TareaFirma` + enum `EstadoTareaFirma`. |
| `subsystem/firmas/domains/DocumentoFirma.xml` | Crear | k-sistemas | Entidad hija `DocumentoFirma`. |
| `subsystem/firmas/service/TareaFirmaInsertDTO.java` | Crear | k-sistemas | DTO de entrada del método `insert` del servicio. |
| `subsystem/firmas/service/TareaFirmaNotifier.java` | Crear | k-sistemas | Interfaz pública del callback que implementa cada sistema solicitante. |
| `subsystem/firmas/service/TareaFirmaService.java` | Crear | k-sistemas | Interfaz pública del servicio (extiende `ModelService<TareaFirma>`). |
| `subsystem/firmas/service/impl/TareaFirmaServiceImpl.java` | Crear | k-sistemas | Implementación que extiende `DefaultModelService<TareaFirma>`. |
| `subsystem/firmas/controller/TareaFirmaController.java` | Crear | k-sistemas | Endpoints `@CallMethod` invocados desde el formulario de pendientes. |
| `subsystem/firmas/views/TareaFirma.xml` | Crear | k-vistas | Grids, forms y acciones de las cuatro vistas (Pendientes/Firmados/Rechazados/Todos). |
| `subsystem/firmas/views/DocumentoFirma.xml` | Crear | k-vistas | Grid embebido y forms de visualización de PDF (originales y firmados). |
| `secretariavirtual/menus/NNN_menuitem_firmas.xml` | Crear | k-vistas | Menú raíz "Firmar documentos" con sus 4 hijos. |
| `data-init/input/auth-firmas.xml` | Crear | k-seguridad | Permisos por filas para `TareaFirma` y `DocumentoFirma`. |
| `data-init/input-config.xml` | Modificar | k-seguridad | Registrar `auth-firmas.xml` en el binding de carga inicial. |

---

## Pasos

### Paso 1 — Dominios

#### Fichero `subsystem/firmas/domains/TareaFirma.xml` (XML completo)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<domain-models xmlns="http://axelor.com/xml/ns/domain-models"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://axelor.com/xml/ns/domain-models
               https://axelor.com/xml/ns/domain-models/domain-models_8.1.xsd">

    <module name="firmas" package="com.educaflow.subsystem.firmas.db"/>

    <entity name="TareaFirma">
        <many-to-one name="firmante" ref="com.axelor.auth.db.User" required="true" title="Firmante"/>
        <one-to-many name="documentosFirma" ref="com.educaflow.subsystem.firmas.db.DocumentoFirma"
                     mappedBy="tareaFirma" title="Documentos a firmar"/>

        <datetime name="fechaSolicitud" required="true" title="Fecha de solicitud"/>
        <datetime name="fechaResolucion" title="Fecha de resolución"/>

        <boolean name="firmaRapida" title="Firma rápida"
                 help="Reservado para firma automática sin intervención del usuario; no usado en esta versión."/>

        <enum name="estadoTareaFirma" ref="EstadoTareaFirma" required="true" title="Estado"/>

        <string name="motivoFirma" required="true" title="Motivo de la firma"/>
        <string name="motivoRechazo" large="true" multiline="true" title="Motivo del rechazo"/>

        <string name="fqcnFirmaNotifier" title="FQCN del notifier"/>
        <string name="fqcnCallBackData" title="FQCN del objeto de callback"/>
        <string name="callBackData" large="true" multiline="true" title="Datos de callback (JSON)"/>

        <decimal name="x"      precision="8" scale="2" required="true" title="X"/>
        <decimal name="y"      precision="8" scale="2" required="true" title="Y"/>
        <decimal name="width"  precision="8" scale="2" required="true" title="Ancho"/>
        <decimal name="height" precision="8" scale="2" required="true" title="Alto"/>
        <integer name="page"   required="true" title="Página"/>
    </entity>

    <enum name="EstadoTareaFirma">
        <item name="PENDIENTE" title="Pendiente" description="Tarea pendiente de resolución por el firmante."/>
        <item name="FIRMADO"   title="Firmado"   description="Tarea firmada por el usuario."/>
        <item name="RECHAZADO" title="Rechazado" description="Tarea rechazada por el usuario."/>
    </enum>

</domain-models>
```

#### Fichero `subsystem/firmas/domains/DocumentoFirma.xml` (XML completo)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<domain-models xmlns="http://axelor.com/xml/ns/domain-models"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://axelor.com/xml/ns/domain-models
               https://axelor.com/xml/ns/domain-models/domain-models_8.1.xsd">

    <module name="firmas" package="com.educaflow.subsystem.firmas.db"/>

    <entity name="DocumentoFirma">
        <many-to-one name="tareaFirma" ref="com.educaflow.subsystem.firmas.db.TareaFirma"
                     required="true" title="Tarea de firma"/>
        <many-to-one name="documentoOriginal" ref="com.axelor.meta.db.MetaFile"
                     required="true" title="Documento original"/>
        <many-to-one name="documentoFirmado" ref="com.axelor.meta.db.MetaFile"
                     title="Documento firmado"/>
    </entity>

</domain-models>
```

**Verificación:** `./gradlew clean build --info` genera `com.educaflow.subsystem.firmas.db.{TareaFirma, DocumentoFirma, EstadoTareaFirma}`.

### Paso 2 — Servicios

#### Clase `com.educaflow.subsystem.firmas.service.TareaFirmaNotifier`

```java
public interface TareaFirmaNotifier {
    void notify(TareaFirma tareaFirma, Object callBackData);
}
```

Comentario: invocado por el subsistema tras persistir una transición a estado final. El `callBackData` puede ser `null`. Cualquier excepción se propaga; la transición ya está persistida (guía 1, A7).

#### Clase `com.educaflow.subsystem.firmas.service.TareaFirmaInsertDTO`

```java
public record TareaFirmaInsertDTO(
    User firmante,
    List<MetaFile> documentos,
    String motivoFirma,
    Rectangulo areaFirma,
    Class<? extends TareaFirmaNotifier> firmaNotifierClass,
    Object callBackData
) { /* invariantes en constructor compacto: requireNonNull de firmante, documentos, motivoFirma,
       areaFirma, firmaNotifierClass; documentos no vacío; motivoFirma no en blanco; cada
       MetaFile no null y MetaFileHelper.isPdf(documento) == true. Lanza
       NullPointerException / IllegalArgumentException — son contratos del DTO, no reglas de negocio
       de UI. */ }
```

#### Interfaz `com.educaflow.subsystem.firmas.service.TareaFirmaService`

```java
public interface TareaFirmaService extends ModelService<TareaFirma> {
    TareaFirma insert(TareaFirmaInsertDTO dto) throws BusinessException;
    TareaFirma marcarComoFirmada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal);
    TareaFirma marcarComoRechazada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal);
    Optional<BusinessMessages> validarDocumentosFirmados(TareaFirma tareaFirma);
}
```

#### Implementación `com.educaflow.subsystem.firmas.service.impl.TareaFirmaServiceImpl`

Extiende `DefaultModelService<TareaFirma>`. Descubierta por `ModelServiceFactory` por convención (`service.impl.*ServiceImpl`). **No se crea módulo Guice**.

```java
public TareaFirmaServiceImpl(Class<TareaFirma> model, Repository repository);
//   Constructor obligatorio del patrón ModelService.
```

```java
@Override
public TareaFirma insert(TareaFirmaInsertDTO dto);
//   Construye una TareaFirma desde el DTO y la persiste en estado PENDIENTE.
//   Aplica:
//     - RN-1: asigna firmante, fechaSolicitud=now, estadoTareaFirma=PENDIENTE,
//       motivoFirma=dto.motivoFirma, motivoRechazo=null.
//     - RN-2 (guía 2 — clonado): para cada MetaFile m del DTO, crea un DocumentoFirma
//       con documentoOriginal = MetaFileUtil.cloneMetaFile(m). El campo documentoFirmado queda null.
//     - RN-3 (guía 1 — callback FQCN+JSON): guarda fqcnFirmaNotifier = dto.firmaNotifierClass.getName().
//       Si dto.callBackData != null: fqcnCallBackData = dto.callBackData.getClass().getName(),
//       callBackData = JsonUtil.toJson(dto.callBackData); en otro caso ambos a null.
//     - RN-4: copia x/y/width/height del Rectangulo del DTO al BigDecimal de la entidad.
//     - V-001..V-004 las cubre validateInsert (heredado del super si validateInsert añade reglas
//       específicas; ver Notas de unificación al final).
//   Llama a super.insert(tareaFirma).
```

```java
@Override
public TareaFirma marcarComoFirmada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal);
//   Aplica la transición PENDIENTE → FIRMADO sobre una tarea cuya colección documentosFirma ya tiene
//   poblado documentoFirmado en cada elemento (lo aporta el flujo de UI tras AutoFirma).
//   Aplica:
//     - RN-5: estadoTareaFirma=FIRMADO, motivoRechazo=null, fechaResolucion=now.
//     - RN-6: super.update(tareaFirma, tareaFirmaOriginal) (auditoría/concurrencia mediante el
//       original snapshot).
//     - RN-7: fireActionRule_NotificarFirmaResuelta(tareaFirma).
//   Precondición funcional: validarDocumentosFirmados ya pasó (ver acción del action-group del paso 4).
```

```java
@Override
public TareaFirma marcarComoRechazada(TareaFirma tareaFirma, TareaFirma tareaFirmaOriginal);
//   Transición PENDIENTE → RECHAZADO. motivoRechazo viene ya asignado en tareaFirma (lo escribe la UI).
//   Aplica:
//     - RN-8: estadoTareaFirma=RECHAZADO, fechaResolucion=now. NO sobrescribe motivoRechazo.
//     - RN-6: super.update(tareaFirma, tareaFirmaOriginal).
//     - RN-7: fireActionRule_NotificarFirmaResuelta(tareaFirma).
```

```java
@Override
public Optional<BusinessMessages> validarDocumentosFirmados(TareaFirma tareaFirma);
//   Validación de servidor disparada ANTES de marcarComoFirmada por el action-group del botón
//   "Firmar todos". No persiste.
//   Aplica:
//     - V-006: por cada DocumentoFirma df, obtiene DocumentoPdf de original y firmado con
//       MetaFileHelper.getDocumentoPdf, llama a DocumentoPdfUtil.validateFirmaPdf(original, firmado,
//       firmante.getDni()) y, si retorna error, acumula un BusinessMessage cuyo target es el
//       fileName del documentoFirmado y cuyo mensaje transmite la causa devuelta por el validador.
//   Devuelve Optional.empty() si no hay errores; Optional.of(messages) si los hay.
```

```java
@SuppressWarnings("unchecked")
private void fireActionRule_NotificarFirmaResuelta(TareaFirma tareaFirma);
//   Efecto secundario común a las dos transiciones a estado final. Implementa la guía 1.
//   Aplica:
//     - RN-9: Class.forName(tareaFirma.getFqcnFirmaNotifier()) — error de configuración del cliente
//       si lanza ClassNotFoundException; envolver en RuntimeException.
//     - RN-10: TareaFirmaNotifier notifier = Beans.get(notifierClass).
//     - RN-11: si fqcnCallBackData != null, Class.forName(...) + JsonUtil.fromJson(callBackData,
//       cbClass). Si null, callBackData=null.
//     - RN-12: notifier.notify(tareaFirma, callBackData). Cualquier excepción del notifier se propaga;
//       la transición ya está persistida (A7).
```

**Verificación:** las clases compilan; `ModelServiceFactory.resolve(TareaFirma.class)` devuelve la instancia.

### Paso 3 — Controladores

#### Clase `com.educaflow.subsystem.firmas.controller.TareaFirmaController`

```java
@Inject private ModelServiceFactory modelServiceFactory;
```

```java
@CallMethod
public void firmarDocumentosConAutoFirma(ActionRequest req, ActionResponse resp);
//   NO transaccional (no escribe en BD). Carga la TareaFirma por id (JpaRepository.find) y
//   construye el payload de AutoFirma: rectángulo (x,y,width,height a float), pageNumber=tarea.page,
//   dni=tarea.firmante.getDni(). Por cada índice i de documentosFirma registra
//   autofirma.addSourceTargetField("documentosFirma[i].documentoOriginal",
//   "documentosFirma[i].documentoFirmado"). Envía con AutoFirma.sendToActionResponse(autofirma, resp)
//   para que el cliente JS lance AutoFirma desktop.
//   Aplica: RN-13 (rectángulo+page+dni), RN-14 (mapeo source→target), RN-15 (envío al cliente).
```

```java
@CallMethod
@Transactional
public void marcarComoFirmada(ActionRequest req, ActionResponse resp);
//   Resuelve TareaFirmaService; obtiene tareaFirmaOriginal con ActionRequestHelper.getOriginalModel;
//   construye AllowProperties que SOLO permite escritura en documentosFirma[*].documentoFirmado;
//   obtiene tareaFirma con esa lista blanca; delega en
//   tareaFirmaService.marcarComoFirmada(tareaFirma, tareaFirmaOriginal).
//   Aplica: RN-16 (lista blanca al firmar).
```

```java
@CallMethod
@Transactional
public void marcarComoRechazada(ActionRequest req, ActionResponse resp);
//   Igual al anterior con lista blanca = solo motivoRechazo.
//   Delega en tareaFirmaService.marcarComoRechazada(tareaFirma, tareaFirmaOriginal).
//   Aplica: RN-17 (lista blanca al rechazar).
```

```java
@CallMethod
public void validarDocumentosFirmados(ActionRequest req, ActionResponse resp);
//   NO transaccional. Lista blanca = documentosFirma[*].documentoFirmado. Llama a
//   tareaFirmaService.validarDocumentosFirmados; si retorna BusinessMessages, los envía como error
//   al cliente vía actionResponseHelper.doResponseBusinessMessagesAsError(messages).
//   Aplica: V-006 (delega en el servicio).
```

**Verificación:** las cuatro acciones `<action-method>` declaradas en el paso 4 resuelven contra estos métodos.

### Paso 4 — Vistas

#### Fichero `subsystem/firmas/views/TareaFirma.xml`

Único fichero de vistas para `TareaFirma`. Contiene las cuatro variantes (Pendiente/Firmado/Rechazado/Todos) declaradas como acciones de menú independientes con `<domain>` filtrando por estado y firmante (excepto Todos).

Vistas declaradas:

- **`subsysFirma.TareaFirma@Pendiente-action`** (`<action-view>`):
  - Domain: `self.estadoTareaFirma= :estadoTareaFirma and firmante.id= :firmanteId`.
  - Contexts: `estadoTareaFirma=PENDIENTE`, `firmanteId=eval:__user__.id`.
  - view-params: `show-toolbar-grid=false`, `show-toolbar-form=false`, `forceEdit=true`, `reload-grid=true`.

- **`subsysFirma.TareaFirma@Pendiente-grid`** (`<grid>`): columnas `fechaSolicitud`, `firmante`, `motivoFirma`, `estadoTareaFirma`. Atributos: `editable=false`, `canNew=false`, `canEdit=true`, `canDelete=false`, `canSave=false`, `orderBy=fechaSolicitud`, `canEditOnClick=true`.

- **`subsysFirma.TareaFirma@Pendiente-form`** (`<form>`): aplica el **patrón "Máquina de estados con `pasoActual`"** (k-vistas Patrón 4). Atributos: `canEdit=true`, `canBackOnSave=true`, `canNew=false`, `canDelete=false`, `canSave=false`. `onLoad="@Pendiente-set-pasoActual-paso1Inicio-action"`. `related='{"documentosFirma":["documentoOriginal","documentoFirmado"]}'`.
  - Panel `tareaFirmaInsertDTO` (readonly): `motivoFirma`, `fechaSolicitud`, `estadoTareaFirma`, `fechaResolucion` (showIf `fechaResolucion!=null`), campo virtual `pasoActual`.
  - Panel `documentosFirma` (panel-related, readonly) → `@DocumentoFirma@Pendiente-grid`/`-form`.
  - Panel `paso1Inicio` (`showIf="pasoActual=='paso1Inicio'"`): botones "Rechazar firmar" → `@Pendiente-btnPaso1InicioRechazar-action`; "Firmar todos los documentos" → `@Pendiente-btnPaso1InicioFirmar-action`.
  - Panel `paso2Rechazado` (`showIf="pasoActual=='paso2Rechazado'"`): campo `motivoRechazo` (widget Text); botones "Atrás" → `@Pendiente-btnPaso2RechazadoAtras-action`; "Finalizar" → `@Pendiente-btnPaso2RechazadoGuardar-action`.
  - Panel `paso2Firmar` (`showIf="pasoActual=='paso2Firmar'"`): bloque de ayuda con enlace a AutoFirma__!!; botones "Atrás" → `@Pendiente-btnPaso2FirmarAtras-action`; "Firmar todos los documentos con AutoFirma__!! y finalizar" → `serial:@Pendiente-Remote-firmarDocumentosConAutoFirma-action,@Pendiente-btnPaso2FirmarGuardar-action`.

- **`subsysFirma.TareaFirma.DocumentoFirma@Pendiente-grid`** (`<grid>`): columna `documentoOriginal.fileName`. `canViewOnClick=true`.
- **`subsysFirma.TareaFirma.DocumentoFirma@Pendiente-form`** (`<form>`): `panel-tabs` con dos pestañas:
  - `tabDocumentoOriginal`: `<viewer depends="documentoOriginal">` con iframe a `ws/rest/com.axelor.meta.db.MetaFile/${documentoOriginal.id}/content/download?inline=true&name=${documentoOriginal.fileName}`.
  - `tabDocumentoFirmado` (showIf `documentoFirmado!=null`): igual con `documentoFirmado`.

- **`subsysFirma.TareaFirma@Firmado-action`** (`<action-view>`): domain igual que pendiente con `estadoTareaFirma=FIRMADO`. Sin `forceEdit`.
- **`subsysFirma.TareaFirma@Firmado-grid`** (`<grid>`): columnas `fechaSolicitud`, `fechaResolucion`, `firmante`, `motivoFirma`, `estadoTareaFirma`. `canEdit=false`, `canNew=false`.
- **`subsysFirma.TareaFirma@Firmado-form`** (`<form>`): readonly. Panel con los campos del grid + `panel-related` `documentosFirma` → `@DocumentoFirma@Firmado-grid`/`-form` (que muestra `documentoFirmado.fileName` y un viewer del PDF firmado). Botón "Salir" → `back`.

- **`subsysFirma.TareaFirma@Rechazado-action`** (`<action-view>`): domain con `estadoTareaFirma=RECHAZADO`.
- **`subsysFirma.TareaFirma@Rechazado-grid`/`-form`**: readonly. El form muestra además `motivoRechazo` (readonly) y el `panel-related` apunta a `@DocumentoFirma@Rechazado-grid`/`-form` (sólo `documentoOriginal`).

- **`subsysFirma.TareaFirma@Todos-action`** (`<action-view>`): SIN `<domain>`. Vista global para administradores. El grid tiene `allowSearchFields=true`.
- **`subsysFirma.TareaFirma@Todos-grid`/`-form`**: readonly. El form muestra `motivoRechazo` (showIf no-null) y los documentos con ambas pestañas (original y firmado, este último con showIf no-null).

Acciones (orden según convención de comentarios de k-vistas):

Acciones de las tareas principales:
- `action-group "@Pendiente-btnPaso1InicioRechazar-action"` → `@Pendiente-set-pasoActual-paso2Rechazado-action`.
- `action-group "@Pendiente-btnPaso1InicioFirmar-action"` → `@Pendiente-set-pasoActual-paso2Firmar-action`.
- `action-group "@Pendiente-btnPaso2RechazadoAtras-action"` → `@Pendiente-set-pasoActual-paso1Inicio-action`.
- `action-group "@Pendiente-btnPaso2RechazadoGuardar-action"` → en orden:
  `@Pendiente-Local-validateMarcarComoRechazada-action`, `@Pendiente-Remote-marcarComoRechazada-action`, `force-back`.
- `action-group "@Pendiente-btnPaso2FirmarAtras-action"` → `@Pendiente-set-pasoActual-paso1Inicio-action`.
- `action-group "@Pendiente-btnPaso2FirmarGuardar-action"` → en orden:
  `@Pendiente-Remote-validarDocumentosFirmados-action`, `@Pendiente-Remote-marcarComoFirmada-action`, `force-back`.

Acciones de Validaciones en local:
- `action-condition "@Pendiente-Local-validateMarcarComoRechazada-action"` con `<check field="motivoRechazo"/>`. **Cubre V-005 cliente**: bloquea el botón "Finalizar" del paso2Rechazado si `motivoRechazo` está vacío. Mensaje gestionado por Axelor.

Acciones básicas que cambian campos simples:
- `action-record "@Pendiente-set-pasoActual-paso1Inicio-action"` → `pasoActual=paso1Inicio`.
- `action-record "@Pendiente-set-pasoActual-paso2Rechazado-action"` → `pasoActual=paso2Rechazado`.
- `action-record "@Pendiente-set-pasoActual-paso2Firmar-action"` → `pasoActual=paso2Firmar`.

Acciones de llamadas Remotas al servidor:
- `action-method "@Pendiente-Remote-validarDocumentosFirmados-action"` → `TareaFirmaController.validarDocumentosFirmados`. Cubre V-006 (capa servidor).
- `action-method "@Pendiente-Remote-marcarComoFirmada-action"` → `TareaFirmaController.marcarComoFirmada`. Aplica RN-5/6/7.
- `action-method "@Pendiente-Remote-marcarComoRechazada-action"` → `TareaFirmaController.marcarComoRechazada`. Aplica RN-6/7/8.
- `action-method "@Pendiente-Remote-firmarDocumentosConAutoFirma-action"` → `TareaFirmaController.firmarDocumentosConAutoFirma`. Aplica RN-13/14/15.

#### Fichero `subsystem/firmas/views/DocumentoFirma.xml`

Vistas embebidas en los `panel-related` del form de `TareaFirma`. Por cada estado (Pendiente, Firmado, Rechazado, Todos) declara su propio par grid+form con `panel-tabs` y `<viewer>` iframe contra `ws/rest/com.axelor.meta.db.MetaFile/${id}/content/download?inline=true&name=${fileName}`.

### Paso 5 — Menús

Modificar/crear `secretariavirtual/menus/NNN_menuitem_firmas.xml`:

- `menuitem name="firmarDocumentos-menuitem"`, title="Firmar documentos", order=70 (sin action — agrupador).
- Hijos en orden:
  1. `firmarDocumentos-todos-menuitem` → `subsysFirma.TareaFirma@Todos-action`.
  2. `firmarDocumentos-pendientes-menuitem` → `subsysFirma.TareaFirma@Pendiente-action`.
  3. `firmarDocumentos-firmados-menuitem` → `subsysFirma.TareaFirma@Firmado-action`.
  4. `firmarDocumentos-rechazados-menuitem` → `subsysFirma.TareaFirma@Rechazado-action`.

### Paso 6 — Seguridad

Crear `data-init/input/auth-firmas.xml` con dos `permission` que limitan el acceso por filas:

- `permission name="TareaFirma.firmante"`, object `com.educaflow.subsystem.firmas.db.TareaFirma`, condition `self.firmante = ?`, conditionParams `__user__`. `<can create="false" read="true" write="true" remove="false" export="false"/>`. **Cubre V-007**.
- `permission name="DocumentoFirma.propio"`, object `com.educaflow.subsystem.firmas.db.DocumentoFirma`, condition `self.tareaFirma IN (SELECT tf FROM TareaFirma tf WHERE tf.firmante = ?)`, conditionParams `__user__`. Mismos `<can>`. **Cubre V-008**.

Modificar `data-init/input-config.xml` para registrar `auth-firmas.xml` en el binding (junto a `auth.xml`, `auth-common.xml`, etc.).

### Paso 7 — Verificación final

```bash
./gradlew clean build --info
```

Comprobar:
- El build compila sin errores.
- `ModelServiceFactory.resolve(TareaFirma.class)` devuelve `TareaFirmaServiceImpl`.
- Los permisos cargados en BD aparecen.
- El menú "Firmar documentos" muestra los 4 hijos en el orden declarado.

---

## Matriz de trazabilidad V-XXX y reglas de negocio → ubicación

| ID | Capa | Ubicación | Comentario |
|----|------|-----------|------------|
| V-001 | Modelo | `TareaFirma.xml` `<many-to-one name="firmante" required="true">` | Garantizado por NOT NULL en BD. La inserción vía DTO lo asegura. |
| V-002 | Modelo + Servidor | `TareaFirma.xml` `<datetime name="fechaSolicitud" required="true">` + `TareaFirmaServiceImpl.insert` | Asignada automáticamente en el insert. |
| V-003 | Modelo | `<enum name="estadoTareaFirma" required="true">` + dominio finito | Estado y dominio garantizados; asignado en insert (PENDIENTE), `marcarComoFirmada` (FIRMADO), `marcarComoRechazada` (RECHAZADO). |
| V-004 | Modelo + DTO | `<string name="motivoFirma" required="true">` + invariante DTO (no en blanco) | El DTO impide pasar blank al servicio. |
| V-005 | Cliente | `firma views` `<action-condition @Pendiente-Local-validateMarcarComoRechazada-action>` con `<check field="motivoRechazo"/>` | `motivoRechazo` obligatorio al rechazar — bloquea el botón "Finalizar" del paso2Rechazado. |
| V-006 | Servidor | `TareaFirmaServiceImpl.validarDocumentosFirmados` (invocado vía `TareaFirmaController.validarDocumentosFirmados` desde `action-method @Pendiente-Remote-validarDocumentosFirmados-action`) | Cada PDF firmado debe ser firma válida del original con el DNI del firmante. Mensaje incluye fileName + causa devuelta por `DocumentoPdfUtil.validateFirmaPdf`. |
| V-007 | Servidor | `auth-firmas.xml` `permission TareaFirma.firmante` (condition `self.firmante = __user__`) | Cada usuario sólo ve y edita sus propias TareaFirma. |
| V-008 | Servidor | `auth-firmas.xml` `permission DocumentoFirma.propio` (subquery sobre `tareaFirma`) | Cada usuario sólo ve y edita los DocumentoFirma de sus propias tareas. |
| RN-1 | Servidor | `TareaFirmaServiceImpl.insert` | Inicialización de campos en la creación. |
| RN-2 | Servidor | `TareaFirmaServiceImpl.insert` | Clonado de los MetaFile originales (guía 2). |
| RN-3 | Servidor | `TareaFirmaServiceImpl.insert` | Persistencia del callback como FQCN+JSON (guía 1). |
| RN-4 | Servidor | `TareaFirmaServiceImpl.insert` | Persistencia del área de firma (x/y/width/height) desde Rectangulo. |
| RN-5 | Servidor | `TareaFirmaServiceImpl.marcarComoFirmada` | Transición a FIRMADO + limpieza de motivoRechazo + fechaResolucion=now. |
| RN-6 | Servidor | `TareaFirmaServiceImpl.marcarComoFirmada` y `marcarComoRechazada` | Persistencia con `super.update(tareaFirma, tareaFirmaOriginal)`. |
| RN-7 | Servidor | `TareaFirmaServiceImpl.fireActionRule_NotificarFirmaResuelta` (invocado desde ambas transiciones) | Invoca el callback del notifier del sistema solicitante. |
| RN-8 | Servidor | `TareaFirmaServiceImpl.marcarComoRechazada` | Transición a RECHAZADO + fechaResolucion=now (preserva motivoRechazo enviado por la UI). |
| RN-9 | Servidor | `fireActionRule_NotificarFirmaResuelta` | Resolución dinámica del notifier por FQCN (`Class.forName`). |
| RN-10 | Servidor | `fireActionRule_NotificarFirmaResuelta` | Inyección del notifier por DI (`Beans.get`). |
| RN-11 | Servidor | `fireActionRule_NotificarFirmaResuelta` | Deserialización del callBackData con `JsonUtil.fromJson`. |
| RN-12 | Servidor | `fireActionRule_NotificarFirmaResuelta` | Invocación del callback (`notifier.notify`). |
| RN-13 | Servidor | `TareaFirmaController.firmarDocumentosConAutoFirma` | Construcción del payload de AutoFirma (rectángulo, página, dni). |
| RN-14 | Servidor | `TareaFirmaController.firmarDocumentosConAutoFirma` | Mapeo source→target por documento. |
| RN-15 | Servidor | `TareaFirmaController.firmarDocumentosConAutoFirma` | Inyección del payload en `ActionResponse`. |
| RN-16 | Servidor | `TareaFirmaController.marcarComoFirmada` (`AllowProperties`) | Lista blanca: solo `documentosFirma[*].documentoFirmado` puede llegar del cliente. |
| RN-17 | Servidor | `TareaFirmaController.marcarComoRechazada` (`AllowProperties`) | Lista blanca: solo `motivoRechazo` puede llegar del cliente. |

**Máquina de estados** (referenciada por V-003, RN-5, RN-8):

| Origen     | Destino    | Disparador                                                | Acción posterior     |
|------------|------------|-----------------------------------------------------------|----------------------|
| (inicial)  | PENDIENTE  | `TareaFirmaService.insert` desde sistema cliente          | RN-1..4              |
| PENDIENTE  | FIRMADO    | `TareaFirmaController.marcarComoFirmada` (V-006 previa)   | RN-5, RN-6, RN-7     |
| PENDIENTE  | RECHAZADO  | `TareaFirmaController.marcarComoRechazada` (V-005 cliente)| RN-6, RN-7, RN-8     |

FIRMADO y RECHAZADO son finales — no hay transiciones desde ellos. Las vistas Firmado/Rechazado/Todos son completamente read-only.

---

## Notas de unificación

- Los 5 subagentes propusieron una `validateInsert`/`validateUpdate` en el servicio que reaplica defensivamente V-001..V-004. El diseño unificado las omite porque (a) los campos están marcados `required="true"` en el dominio y por tanto cualquier insert violaría la constraint NOT NULL antes de llegar al callback, y (b) el `TareaFirmaInsertDTO` ya las garantiza con sus invariantes de constructor. Si en alguna iteración futura se decide añadirlas como defensa-en-profundidad, irían en el servicio con las firmas estándar `Optional<BusinessMessages> validateInsert(TareaFirma)` y `validateUpdate(TareaFirma, TareaFirma)`.
- El campo `firmaRapida` se mantiene en el modelo con `<help>` aclaratorio pero ningún flujo lo usa (asunción A8).
- El campo `page` es obligatorio y lo aporta el sistema cliente al crear la tarea (asunción A9). El método `insert` no lo asigna desde el `Rectangulo` del DTO porque la página no es parte de un rectángulo; se asume que el sistema cliente lo aporta por una vía adicional que el análisis no detalla.
- Cuatro subagentes propusieron envolver cada `MetaFile` en un `DocumentoFirmaInsertDTO`; uno propuso pasar la lista de `MetaFile` directamente. Se opta por la lista directa para evitar abstracciones innecesarias (un wrapper de un solo campo no aporta valor).
- Tres subagentes propusieron operaciones `firmar(id, ...)` y `rechazar(id, ...)` que cargan la entidad por id internamente; dos propusieron operaciones que reciben la entidad ya cargada. Se opta por recibir `(tareaFirma, tareaFirmaOriginal)` para alinearse con el patrón estándar de `ModelService.update` (que usa el original snapshot para detección de cambios y auditoría). El controller carga ambos via `ActionRequestHelper.getOriginalModel()` y `getModel(allowProperties)`.
- Cuatro subagentes consolidaron toda la UI en un único fichero `views/TareaFirma.xml`; uno propuso 4 ficheros (uno por estado). Se opta por **un único fichero** alineado con el patrón "una entidad → un fichero de vistas" de `k-sistemas` (sección "views/" del SKILL.md). Esto es una **divergencia con el código real existente del subsistema** (que usa 4 ficheros separados), pero esa decisión histórica no está justificada por ningún skill ni guía de diseño actual.
