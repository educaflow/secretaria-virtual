---
type: design
---

# Diseño as-built: Vistas del subsistema importacion

**Objetivo:** Crear el subsistema `importacion` con la entidad `TareaImportacion`, las vistas de listado y detalle/creación (un único formulario con dos fases inline) y la persistencia del resultado de importación, de forma que los administradores puedan registrar y consultar importaciones de ficheros, dejando cada importación persistida aunque el proceso falle.

**Capa:** subsystem/importacion

**Análisis de origen:** `.sdd/drafts/2026-05-13_10-16_importacion-vistas/analysis_01/analysis.md`

**Skills necesarios para la implementación:** k-sistemas, k-vistas

---

## Ficheros creados o modificados

| Fichero | Acción | Descripción |
|---------|--------|-------------|
| `src/main/java/com/educaflow/subsystem/importacion/domains/TareaImportacion.xml` | Creado | Dominio XML con la entidad `TareaImportacion` y el enum `TipoFicheroImportacion`. |
| `src/main/java/com/educaflow/subsystem/importacion/exception/ImportadorException.java` | Creado | Excepción checked del proceso de importación. |
| `src/main/java/com/educaflow/subsystem/importacion/importador/ResultadoImportacion.java` | Creado | Record Java 21 inmutable con el resultado de un importador. |
| `src/main/java/com/educaflow/subsystem/importacion/importador/ImportadorFichero.java` | Creado | Interfaz con el método `importar()`. |
| `src/main/java/com/educaflow/subsystem/importacion/importador/ImportadorFicheroFactory.java` | Creado | Factoría que resuelve la implementación según `TipoFicheroImportacion`. |
| `src/main/java/com/educaflow/subsystem/importacion/importador/impl/ImportadorUsuarioXML.java` | Creado | Implementación para `PROFESOR`, `ALUMNO`, `FAMILIAR`. Siempre lanza `ImportadorException`. |
| `src/main/java/com/educaflow/subsystem/importacion/importador/impl/ImportadorUsuarioCSV.java` | Creado | Implementación para `PROFESOR_EXTERNO`. Siempre lanza `ImportadorException`. |
| `src/main/java/com/educaflow/subsystem/importacion/service/TareaImportacionService.java` | Creado | Interfaz `ModelService<TareaImportacion>`. |
| `src/main/java/com/educaflow/subsystem/importacion/service/impl/TareaImportacionServiceImpl.java` | Creado | Implementación `DefaultModelService<TareaImportacion>` con validaciones de inmutabilidad y orquestación de la importación. |
| `src/main/java/com/educaflow/subsystem/importacion/controller/TareaImportacionController.java` | Creado | Controlador con el `@CallMethod` `validateSave`. |
| `src/main/java/com/educaflow/subsystem/importacion/views/TareaImportacion.xml` | Creado | `action-view` principal `@Main-action`, grid `@Main-grid`, form `@Main-form` (dual: creación + detalle solo lectura) y todas las acciones asociadas. |

---

## Pasos

### Paso 1 — Crear el dominio `TareaImportacion` y el enum `TipoFicheroImportacion`

Crear `src/main/java/com/educaflow/subsystem/importacion/domains/TareaImportacion.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<domain-models xmlns="http://axelor.com/xml/ns/domain-models"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://axelor.com/xml/ns/domain-models https://axelor.com/xml/ns/domain-models/domain-models_8.1.xsd">

    <module name="importacion" package="com.educaflow.subsystem.importacion.db"/>

    <entity name="TareaImportacion">

        <many-to-one name="usuario"
                     ref="com.axelor.auth.db.User"
                     required="true"
                     title="Usuario"/>

        <many-to-one name="centro"
                     ref="com.educaflow.subsystem.common.db.Centro"
                     title="Centro"/>

        <integer name="curso" title="Curso"/>

        <datetime name="fechaImportacion"
                  required="true"
                  title="Fecha de importación"/>

        <datetime name="fechaExportacion"
                  title="Fecha de exportación"/>

        <enum name="tipoFichero"
              ref="com.educaflow.subsystem.importacion.db.TipoFicheroImportacion"
              required="true"
              title="Tipo de fichero"/>

        <many-to-one name="fichero"
                     ref="com.axelor.meta.db.MetaFile"
                     required="true"
                     title="Fichero"/>

        <boolean name="estado"
                 required="true"
                 title="Estado"
                 help="true = correcta, false = fallida"/>

        <string name="log"
                large="true"
                multiline="true"
                title="Log"/>

    </entity>

    <enum name="TipoFicheroImportacion">
        <item name="PROFESOR"         title="Profesor"/>
        <item name="ALUMNO"           title="Alumno"/>
        <item name="FAMILIAR"         title="Familiar"/>
        <item name="PROFESOR_EXTERNO" title="Profesor externo"/>
    </enum>

</domain-models>
```

---

### Paso 2 — Crear la excepción `ImportadorException`

`com.educaflow.subsystem.importacion.exception.ImportadorException` extiende `java.lang.Exception` (checked).

```
public ImportadorException(String message)
public ImportadorException(String message, Throwable cause)
```

---

### Paso 3 — Crear el DTO `ResultadoImportacion`

Record Java 21 inmutable `com.educaflow.subsystem.importacion.importador.ResultadoImportacion`:

```
public record ResultadoImportacion(
    int usuariosImportados,
    int numeroErrores,
    String log,
    com.educaflow.subsystem.common.db.Centro centro,
    Integer curso
)
```

---

### Paso 4 — Crear la interfaz `ImportadorFichero`

```
public interface ImportadorFichero
    ResultadoImportacion importar() throws ImportadorException
```

---

### Paso 5 — Crear las implementaciones `ImportadorUsuarioXML` e `ImportadorUsuarioCSV`

Ambas en `importador/impl/`. Constructores reciben `MetaFile fichero` y `TipoFicheroImportacion tipoFichero`. Método `importar()` lanza siempre `new ImportadorException("@TODO: Importación no implementada todavía")`.

---

### Paso 6 — Crear la factoría `ImportadorFicheroFactory`

Clase `final` con constructor privado:

```
public static ImportadorFichero create(TipoFicheroImportacion tipoFichero, MetaFile fichero)
// PROFESOR, ALUMNO, FAMILIAR → ImportadorUsuarioXML
// PROFESOR_EXTERNO            → ImportadorUsuarioCSV
// null                        → IllegalArgumentException
```

---

### Paso 7 — Crear la interfaz `TareaImportacionService`

```
public interface TareaImportacionService
    extends ModelService<TareaImportacion>
// Sin métodos adicionales
```

---

### Paso 8 — Crear la implementación `TareaImportacionServiceImpl`

`com.educaflow.subsystem.importacion.service.impl.TareaImportacionServiceImpl` extiende `DefaultModelService<TareaImportacion>` e implementa `TareaImportacionService`.

```
public TareaImportacionServiceImpl(Class<TareaImportacion> model, Repository<TareaImportacion> repository)
// Constructor invocado por reflexión desde ModelServiceFactory.

public Optional<BusinessMessages> validateInsert(TareaImportacion tareaImportacion)
// V-001: tipoFichero != null
// V-003: fichero != null

public Optional<BusinessMessages> validateUpdate(TareaImportacion entidad, TareaImportacion entidadOriginal)
// V-005: devuelve siempre BusinessMessages con "Las importaciones ya registradas no se pueden modificar."

public Optional<BusinessMessages> validateRemove(TareaImportacion entidad)
// V-006: devuelve siempre BusinessMessages con "Las importaciones no se pueden eliminar."

public TareaImportacion insert(TareaImportacion tareaImportacion)
// 1. fireActionRule_asignarCamposSistema(tareaImportacion)
// 2. fireActionRule_ejecutarImportacion(tareaImportacion)
// 3. super.insert(tareaImportacion)

private void fireActionRule_asignarCamposSistema(TareaImportacion tareaImportacion)
// Asigna: usuario=AuthUtils.getUser(), fechaImportacion=LocalDateTime.now(),
// fechaExportacion=null, estado=false, log=null

private void fireActionRule_ejecutarImportacion(TareaImportacion tareaImportacion)
// ImportadorFicheroFactory.create(...).importar()
// Éxito: estado=true, log="Importación finalizada. " + resultado.log(),
//        centro=resultado.centro(), curso=resultado.curso(), fechaExportacion=LocalDateTime.now()
// ImportadorException: estado=false, log=ex.getMessage()
// NUNCA repropaga la excepción
```

---

### Paso 9 — Crear el controlador `TareaImportacionController`

`com.educaflow.subsystem.importacion.controller.TareaImportacionController` con `@Inject ModelServiceFactory`.

```
@CallMethod
public void validateSave(ActionRequest actionRequest, ActionResponse actionResponse)
// Extrae tipoFichero y fichero del request via AllowProperties.
// Si actionRequestHelper.getId() == null → llama a validateInsert.
// Si actionRequestHelper.getId() != null → llama a validateUpdate(entidad, null).
// Si hay BusinessMessages → doResponseBusinessMessagesAsError y retorna.
// No llama a insert(): la persistencia la gestiona el save nativo de Axelor.
```

**Nota:** no hay métodos `subir()` ni `aceptar()`. La persistencia la delega el botón "Importar" al mecanismo nativo `save` de Axelor (declarado en el action-group de la vista). El `validateSave` solo valida y bloquea si hay errores.

---

### Paso 10 — Crear la vista `TareaImportacion.xml` (listado + formulario dual)

Un único fichero `src/main/java/com/educaflow/subsystem/importacion/views/TareaImportacion.xml` que contiene todo.

**Grid `subsysImportacion.TareaImportacion@Main-grid`:**
- `canAdvanceSearch="false" canRefresh="false" allowSearchFields="false"` (guía 7.3)
- `canNew="true" newButtonTitle="Importar nueva"` — el botón nativo de Axelor con texto personalizado activa la creación inline
- `canEdit="false" canDelete="false" canSave="false"`
- `editable="false" edit-icon="false" x-selector="none" canEditOnClick="true"`
- `orderBy="-fechaImportacion"`
- Columnas: `fechaImportacion`, `tipoFichero`, `centro`, `curso`, `fechaExportacion`, `estado` (con `x-true-text="Correcta" x-false-text="Fallida"`), `usuario`

**Form `subsysImportacion.TareaImportacion@Main-form`** — formulario dual (creación + detalle solo lectura):
- `width="large" canAttach="false" canBack="false" canDelete="false" canNew="false" canSave="false" canMore="false" canBackOnSave="false"`
- `onNew="subsysImportacion.TareaImportacion@Main-onNew-action"` — pre-rellena usuario y fechaImportacion en el cliente al crear
- **Panel `panelEntrada`** (`showIf="id == null"`): campo `tipoFichero` (widget SwitchSelect, required, colSpan 12) y campo `fichero` (widget binary-link, required, colSpan 12)
- **Panel `panelResultado`** (`showIf="id != null"`, `readonlyIf="true"`): sub-panel con `usuario`, `fechaImportacion`, `tipoFichero`, `centro`, `curso`, `fechaExportacion`, `fichero`; sub-panel con `estado` (x-true-text/x-false-text) y `log` (widget text, colSpan 12)
- **Panel `panelBotones`** (showFrame false, colSpan 12): `btnCancelar` (back, showIf id==null), `btnImportar` (showIf id==null), `btnAceptar` (back, showIf id!=null)

**Action-view `subsysImportacion.TareaImportacion@Main-action`:**
- Vistas: grid + form
- `show-toolbar-form=false`, `forceEdit=true`, `reload-grid=true`
- Sin `<domain>`, sin `groups` (restricción viene del menuitem)

**Acciones en el fichero:**

| Nombre | Tipo | Propósito |
|--------|------|-----------|
| `@Main-btnCancelar-action` | action-group | `back` |
| `@Main-btnImportar-action` | action-group | validateLocal → validateRemote → `save` (persistencia nativa) |
| `@Main-btnAceptar-action` | action-group | `back` |
| `@Main-onNew-action` | action-group | llama a `@Main-set-campos-sistema-action` |
| `@Main-Local-validateImportar-action` | action-condition | V-001 y V-003 en cliente |
| `@Main-set-campos-sistema-action` | action-record | pre-rellena `usuario=__user__` y `fechaImportacion=__datetime__` en el cliente (el servidor los sobreescribe igualmente en `fireActionRule_asignarCamposSistema`) |
| `@Main-Remote-validateSave-action` | action-method | llama a `TareaImportacionController.validateSave` |

---

### Paso 11 — Seguridad

Sin ficheros nuevos en `subsystem/security/`. El menuitem `administracionSv-importacion-menuitem` ya lleva `groups="admins"` y es el único punto de entrada.

---

### Paso 12 — Datos iniciales

No se cargan registros iniciales. La tabla arranca vacía.

---

## Matriz de trazabilidad

### Validaciones V-XXX

| Regla | Capa | Ubicación |
|-------|------|-----------|
| V-001 (`tipoFichero` obligatorio) | modelo | `domains/TareaImportacion.xml`: `<enum required="true">` |
| V-001 | cliente | `views/TareaImportacion.xml`: `@Main-Local-validateImportar-action` (check sobre `tipoFichero`) |
| V-001 | servidor | `TareaImportacionServiceImpl.validateInsert` |
| V-002 (`tipoFichero` dominio cerrado) | modelo | `domains/TareaImportacion.xml`: tipo enum Java (imposible asignar valor fuera del conjunto) |
| V-002 | cliente | campo `tipoFichero` con `widget="SwitchSelect"` en `@Main-form` |
| V-003 (`fichero` obligatorio) | modelo | `domains/TareaImportacion.xml`: `<many-to-one name="fichero" required="true">` |
| V-003 | cliente | `views/TareaImportacion.xml`: `@Main-Local-validateImportar-action` (check sobre `fichero`) |
| V-003 | servidor | `TareaImportacionServiceImpl.validateInsert` |
| V-004 (campos no asignables) | transporte | `TareaImportacionController.validateSave`: `AllowProperties` solo permite `tipoFichero` y `fichero` |
| V-004 | servidor | `fireActionRule_asignarCamposSistema`: sobreescribe usuario, fechaImportacion, fechaExportacion, estado, log |
| V-004 | cliente | `@Main-set-campos-sistema-action` (action-record en onNew): pre-rellena usuario y fechaImportacion (el servidor los sobreescribe) |
| V-005 (edición prohibida) | cliente | `@Main-form`: `canSave="false"`, panel resultado con `readonlyIf="true"`; `@Main-grid`: `canEdit="false"` |
| V-005 | servidor | `TareaImportacionServiceImpl.validateUpdate` devuelve siempre error |
| V-006 (borrado prohibido) | cliente | `@Main-grid` y `@Main-form`: `canDelete="false"` |
| V-006 | servidor | `TareaImportacionServiceImpl.validateRemove` devuelve siempre error |
| V-007 (acceso restringido a admins) | menú | `secretariavirtual/menus/menus.xml`: `administracionSv-importacion-menuitem groups="admins"` |

### Reglas de negocio

| Regla | Capa | Ubicación |
|-------|------|-----------|
| Listar ordenado por fechaImportacion DESC | cliente | `@Main-grid`: `orderBy="-fechaImportacion"` |
| Detalle solo lectura | cliente | `@Main-form`: panel resultado con `readonlyIf="true"`, visible con `showIf="id != null"` |
| Registrar nueva importación (flujo completo) | servidor | `TareaImportacionController.validateSave` → acción `save` → `TareaImportacionServiceImpl.insert` → `fireActionRule_asignarCamposSistema` + `fireActionRule_ejecutarImportacion` |
| Formulario en dos fases inline (no modal) | cliente | `@Main-form`: `panelEntrada` con `showIf="id==null"`, `panelResultado` con `showIf="id!=null"` |
| Cierre de la vista pulsando Aceptar | cliente | `@Main-btnAceptar-action`: `back` |
| Estado mostrado legible | cliente | campo `estado` con `x-true-text="Correcta" x-false-text="Fallida"` en grid y form |
| Persistencia incluso cuando falla | servidor | `fireActionRule_ejecutarImportacion` captura `ImportadorException` sin repropagar |
| Routing de implementaciones por tipo | servidor | `ImportadorFicheroFactory.create` |
| Listado multicentro sin filtrado | cliente | `@Main-action` sin `<domain>` |

---

## Notas de cierre (as-built)

Cambios aplicados respecto al draft original:

- **Arquitectura de vistas (cambio mayor)**: el diseño planificaba dos ficheros XML (`TareaImportacion.xml` para listado+detalle y `TareaImportacion-Subir.xml` para un modal popup). La implementación usa un único fichero `TareaImportacion.xml` donde el `@Main-form` sirve tanto para la creación (panelEntrada, `showIf="id==null"`) como para el detalle (panelResultado, `showIf="id!=null"`). No existe ventana modal popup.

- **Grid: `canNew="true"` en lugar de toolbar button (cambio mayor)**: el diseño usaba `canNew="false"` + botón explícito en el toolbar del action-view. La implementación usa `canNew="true" newButtonTitle="Importar nueva"` (botón nativo de Axelor con texto personalizado).

- **Controlador: `validateSave()` en lugar de `subir()` + `aceptar()` (cambio mayor)**: el diseño planificaba que `subir()` llamara a `service.insert()` y volcara los resultados con `setValues()`. La implementación solo tiene `validateSave()` que valida y devuelve errores; la persistencia la gestiona el `save` nativo de Axelor declarado en el action-group del botón "Importar". No hay método `aceptar()`.

- **Botones Cancelar y Aceptar: `back` en lugar de `close`**: consecuencia del punto anterior; sin modal, los botones vuelven al listado en lugar de cerrar un popup.

- **`action-record` para `onNew` (nuevo, no en diseño)**: se añadió un `action-record` que pre-rellena `usuario` y `fechaImportacion` en el cliente al crear. No afecta a la seguridad porque el servidor los sobreescribe en `fireActionRule_asignarCamposSistema`.

- **`canEditOnClick="true"` en lugar de `false`**: el grid abre el form en modo edición visual, aunque `canSave="false"` impide guardar cambios.

- **`validateUpdate` con dos parámetros**: la firma real del framework es `validateUpdate(TareaImportacion entidad, TareaImportacion entidadOriginal)`; el diseño simplificó a un solo parámetro.

- **Paso 11 eliminado**: el diseño dedicaba el paso 11 a la vista modal `TareaImportacion-Subir.xml`. Al fusionarse en `TareaImportacion.xml`, ese paso desaparece y su contenido queda integrado en el Paso 10.