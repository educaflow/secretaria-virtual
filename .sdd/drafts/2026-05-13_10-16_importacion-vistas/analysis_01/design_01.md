---
type: design
---

# Diseño: Vistas del subsistema importacion

**Objetivo:** Crear el subsistema `importacion` con la entidad `TareaImportacion`, las vistas de listado y detalle (solo lectura) y el modal de subida en dos fases (entrada → resultado), de forma que los administradores puedan registrar y consultar importaciones de ficheros, dejando cada importación persistida aunque el proceso falle.

**Capa:** subsystem/importacion

**Análisis de origen:** `.sdd/drafts/2026-05-13_10-16_importacion-vistas/analysis_01/analysis.md`

**Skills necesarios para la implementación:** k-sistemas, k-vistas

---

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/importacion/domains/TareaImportacion.xml` | Crear | k-sistemas | Dominio XML con la entidad `TareaImportacion` y el enum `TipoFicheroImportacion`. |
| `src/main/java/com/educaflow/subsystem/importacion/exception/ImportadorException.java` | Crear | k-sistemas | Excepción de dominio del proceso de importación. |
| `src/main/java/com/educaflow/subsystem/importacion/importador/ResultadoImportacion.java` | Crear | k-sistemas | DTO inmutable con el resultado de un importador. |
| `src/main/java/com/educaflow/subsystem/importacion/importador/ImportadorFichero.java` | Crear | k-sistemas | Interfaz con el método `importar()`. |
| `src/main/java/com/educaflow/subsystem/importacion/importador/ImportadorFicheroFactory.java` | Crear | k-sistemas | Factoría que resuelve la implementación según `TipoFicheroImportacion`. |
| `src/main/java/com/educaflow/subsystem/importacion/importador/impl/ImportadorUsuarioXML.java` | Crear | k-sistemas | Implementación para `PROFESOR`, `ALUMNO`, `FAMILIAR`. Siempre lanza `ImportadorException`. |
| `src/main/java/com/educaflow/subsystem/importacion/importador/impl/ImportadorUsuarioCSV.java` | Crear | k-sistemas | Implementación para `PROFESOR_EXTERNO`. Siempre lanza `ImportadorException`. |
| `src/main/java/com/educaflow/subsystem/importacion/service/TareaImportacionService.java` | Crear | k-sistemas | Interfaz `ModelService<TareaImportacion>`. |
| `src/main/java/com/educaflow/subsystem/importacion/service/impl/TareaImportacionServiceImpl.java` | Crear | k-sistemas | Implementación `DefaultModelService<TareaImportacion>` con validaciones de inmutabilidad y orquestación de la importación. |
| `src/main/java/com/educaflow/subsystem/importacion/controller/TareaImportacionController.java` | Crear | k-sistemas | Controlador con los `@CallMethod` `subir` y `aceptar`. |
| `src/main/java/com/educaflow/subsystem/importacion/views/TareaImportacion.xml` | Crear | k-vistas | `action-view` principal `@Main-action`, grid `@Main-grid` y form `@Main-form` (detalle solo lectura). |
| `src/main/java/com/educaflow/subsystem/importacion/views/TareaImportacion-Subir.xml` | Crear | k-vistas | `action-view` modal `@Subir-action` y form `@Subir-form` con paneles de fase entrada / fase resultado. |
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Verificar (no modificar) | — | El menuitem `administracionSv-importacion-menuitem` ya existe y apunta a `subsysImportacion.TareaImportacion@Main-action` con `groups="admins"`. |
| `src/main/java/com/educaflow/subsystem/common/domains/Centro.xml` | Verificar (no modificar) | — | La relación inversa `tareasImportacion` ya existe en el dominio de `Centro`. |

---

## Pasos

### Paso 1 — Crear el dominio `TareaImportacion` y el enum `TipoFicheroImportacion`

Crear el fichero `src/main/java/com/educaflow/subsystem/importacion/domains/TareaImportacion.xml` con el siguiente contenido completo (única parte 100 % detallada del diseño):

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

Notas:
- `estado` es `boolean` y obligatorio (guía 1: "Boolean, no enum").
- `fichero` es `many-to-one` obligatorio a `MetaFile` (asunción A1* del análisis: el fichero original se conserva para auditoría).
- `tipoFichero` es un `enum` Axelor: el tipo Java generado garantiza V-002 a nivel de modelo.

**Verificación:** ejecutar `./gradlew clean build --info`; las clases generadas `TareaImportacion.java` y `TipoFicheroImportacion.java` deben aparecer en `build/src-gen/com/educaflow/subsystem/importacion/db/`. La relación inversa `Centro.tareasImportacion` (ya declarada en `subsystem/common/domains/Centro.xml`) debe resolver sin errores.

---

### Paso 2 — Crear la excepción `ImportadorException`

Crear `src/main/java/com/educaflow/subsystem/importacion/exception/ImportadorException.java`.

- FQCN: `com.educaflow.subsystem.importacion.exception.ImportadorException`.
- Extiende `java.lang.Exception` (checked, fuerza al servicio a tratarla explícitamente).

Firmas:

```
public class ImportadorException extends Exception

    public ImportadorException(String message)
    // Construye la excepción con un mensaje legible. El mensaje se vuelca después
    // al campo `log` de la TareaImportacion.

    public ImportadorException(String message, Throwable cause)
    // Variante con causa encadenada.
```

**Verificación:** compila con `./gradlew clean build --info`.

---

### Paso 3 — Crear el DTO `ResultadoImportacion`

Crear `src/main/java/com/educaflow/subsystem/importacion/importador/ResultadoImportacion.java` como `record` Java 21 inmutable.

- FQCN: `com.educaflow.subsystem.importacion.importador.ResultadoImportacion`.

Firma:

```
public record ResultadoImportacion(
    int usuariosImportados,
    int numeroErrores,
    String log,
    com.educaflow.subsystem.common.db.Centro centro,
    Integer curso
)
// DTO devuelto por ImportadorFichero.importar() en caso de éxito. El servicio
// lo usa para volcar `centro`, `curso` y `log` en la TareaImportacion.
// En esta iniciativa la rama de éxito NUNCA se ejecuta (guía 8): se mantiene el
// contrato para iniciativas futuras.
```

**Verificación:** compila con `./gradlew clean build --info`.

---

### Paso 4 — Crear la interfaz `ImportadorFichero`

Crear `src/main/java/com/educaflow/subsystem/importacion/importador/ImportadorFichero.java`.

- FQCN: `com.educaflow.subsystem.importacion.importador.ImportadorFichero`.

Firmas:

```
public interface ImportadorFichero

    ResultadoImportacion importar() throws ImportadorException
    // Ejecuta el proceso de importación a partir del fichero y el tipo recibidos
    // en el constructor de la implementación concreta. Devuelve un
    // ResultadoImportacion poblado con los datos extraídos del fichero, o lanza
    // ImportadorException si no puede completarse.
    // En esta iniciativa todas las implementaciones lanzan ImportadorException
    // con el mensaje exacto "@TODO: Importación no implementada todavía" (guía 8).
```

**Verificación:** compila con `./gradlew clean build --info`.

---

### Paso 5 — Crear las implementaciones `ImportadorUsuarioXML` e `ImportadorUsuarioCSV`

Crear dos clases en `src/main/java/com/educaflow/subsystem/importacion/importador/impl/`.

**`com.educaflow.subsystem.importacion.importador.impl.ImportadorUsuarioXML`** — implementa `ImportadorFichero`.

```
public class ImportadorUsuarioXML implements ImportadorFichero

    public ImportadorUsuarioXML(
        com.axelor.meta.db.MetaFile fichero,
        com.educaflow.subsystem.importacion.db.TipoFicheroImportacion tipoFichero)
    // Guarda en campos `private final` el fichero y el tipo (PROFESOR/ALUMNO/FAMILIAR).
    // No recibe centro ni curso: se obtendrían del XML al implementarse la lógica real.

    @Override
    public ResultadoImportacion importar() throws ImportadorException
    // Guía 8: lanza siempre new ImportadorException("@TODO: Importación no implementada todavía").
```

**`com.educaflow.subsystem.importacion.importador.impl.ImportadorUsuarioCSV`** — implementa `ImportadorFichero`.

```
public class ImportadorUsuarioCSV implements ImportadorFichero

    public ImportadorUsuarioCSV(
        com.axelor.meta.db.MetaFile fichero,
        com.educaflow.subsystem.importacion.db.TipoFicheroImportacion tipoFichero)
    // Guarda en campos `private final` el fichero y el tipo (siempre PROFESOR_EXTERNO,
    // según la factoría).

    @Override
    public ResultadoImportacion importar() throws ImportadorException
    // Guía 8: lanza siempre new ImportadorException("@TODO: Importación no implementada todavía").
```

**Verificación:** ambas clases compilan con `./gradlew clean build --info`.

---

### Paso 6 — Crear la factoría `ImportadorFicheroFactory`

Crear `src/main/java/com/educaflow/subsystem/importacion/importador/ImportadorFicheroFactory.java`.

- FQCN: `com.educaflow.subsystem.importacion.importador.ImportadorFicheroFactory`.

Firmas:

```
public final class ImportadorFicheroFactory

    private ImportadorFicheroFactory()
    // Constructor privado: clase de utilidad estática.

    public static ImportadorFichero create(
        com.educaflow.subsystem.importacion.db.TipoFicheroImportacion tipoFichero,
        com.axelor.meta.db.MetaFile fichero)
    // Implementación de la guía 5:
    //   - PROFESOR, ALUMNO, FAMILIAR  -> new ImportadorUsuarioXML(fichero, tipoFichero)
    //   - PROFESOR_EXTERNO            -> new ImportadorUsuarioCSV(fichero, tipoFichero)
    //   - null o desconocido          -> lanza IllegalArgumentException (caso defensivo;
    //                                    V-001/V-002 ya impiden llegar aquí con un valor inválido).
    // No persiste nada en TareaImportacion: esa responsabilidad es del servicio.
```

**Verificación:** compila con `./gradlew clean build --info`.

---

### Paso 7 — Crear la interfaz del servicio `TareaImportacionService`

Crear `src/main/java/com/educaflow/subsystem/importacion/service/TareaImportacionService.java`.

- FQCN: `com.educaflow.subsystem.importacion.service.TareaImportacionService`.

```
public interface TareaImportacionService
    extends com.axelor.db.modelservice.ModelService<
        com.educaflow.subsystem.importacion.db.TareaImportacion>

    // No declara métodos adicionales: las operaciones del subsistema se cubren
    // sobrescribiendo `validateInsert`, `validateUpdate`, `validateRemove` y `insert`
    // heredados de ModelService. El controlador llama directamente a `service.insert(...)`.
```

**Verificación:** compila con `./gradlew clean build --info`.

---

### Paso 8 — Crear la implementación `TareaImportacionServiceImpl`

Crear `src/main/java/com/educaflow/subsystem/importacion/service/impl/TareaImportacionServiceImpl.java`.

- FQCN: `com.educaflow.subsystem.importacion.service.impl.TareaImportacionServiceImpl`.
- La convención de paquete (`service.impl.<Entidad>ServiceImpl`) la descubre `ModelServiceFactory` automáticamente: **NO se crea módulo Guice**.

```
public class TareaImportacionServiceImpl
    extends com.axelor.db.modelservice.DefaultModelService<
        com.educaflow.subsystem.importacion.db.TareaImportacion>
    implements TareaImportacionService

    public TareaImportacionServiceImpl(
        Class<com.educaflow.subsystem.importacion.db.TareaImportacion> model,
        com.axelor.db.Repository<com.educaflow.subsystem.importacion.db.TareaImportacion> repository)
    // Constructor obligatorio que invoca super(model, repository). Lo localiza
    // ModelServiceFactory por reflexión.

    @Override
    public java.util.Optional<
        com.educaflow.base.infrastructure.validation.messages.BusinessMessages>
    validateInsert(com.educaflow.subsystem.importacion.db.TareaImportacion entidad)
    // Validaciones servidor previas al insert:
    //   - V-001: tipoFichero != null. Mensaje al BusinessMessages: debe transmitir
    //     que el tipo de fichero es obligatorio e incluir los cuatro valores válidos.
    //   - V-002: tipoFichero pertenece al enum. El tipo Java enum ya lo garantiza;
    //     se reverifica defensivamente. Mensaje: debe transmitir el valor recibido
    //     y los cuatro valores válidos.
    //   - V-003: fichero != null. Mensaje: debe transmitir que el fichero es obligatorio.
    // Devuelve Optional.empty() si todo es correcto. Nunca lanza BusinessException.

    @Override
    public java.util.Optional<
        com.educaflow.base.infrastructure.validation.messages.BusinessMessages>
    validateUpdate(com.educaflow.subsystem.importacion.db.TareaImportacion entidad)
    // V-005 (edición prohibida). Devuelve siempre Optional.of(BusinessMessages) con
    // un BusinessMessage cuyo mensaje transmita que las importaciones ya registradas
    // no se pueden modificar. No mira los cambios concretos: la inmutabilidad es total.

    @Override
    public java.util.Optional<
        com.educaflow.base.infrastructure.validation.messages.BusinessMessages>
    validateRemove(com.educaflow.subsystem.importacion.db.TareaImportacion entidad)
    // V-006 (borrado prohibido). Devuelve siempre Optional.of(BusinessMessages) con
    // un BusinessMessage cuyo mensaje transmita que las importaciones no se pueden borrar.

    @Override
    public com.educaflow.subsystem.importacion.db.TareaImportacion
    insert(com.educaflow.subsystem.importacion.db.TareaImportacion entidad)
    // Orquesta la creación. Secuencia interna (en este orden):
    //   1. fireActionRule_asignarCamposSistema(entidad)
    //   2. fireActionRule_ejecutarImportacion(entidad)
    //   3. super.insert(entidad)  -> persiste con repository.save().
    // El método NO captura ImportadorException directamente: lo hace
    // fireActionRule_ejecutarImportacion. Si validateInsert acumula mensajes, el flujo
    // del framework Axelor lanza BusinessException ANTES de llegar aquí (vía el
    // controlador que comprueba los mensajes y los muestra al usuario).

    private void fireActionRule_asignarCamposSistema(
        com.educaflow.subsystem.importacion.db.TareaImportacion entidad)
    // V-004 (campos no asignables por el usuario) — parte servidor:
    //   - entidad.setUsuario(com.axelor.auth.AuthUtils.getUser())
    //   - entidad.setFechaImportacion(java.time.LocalDateTime.now())
    //   - entidad.setFechaExportacion(null)  -> siempre null en esta iniciativa.
    //   - entidad.setEstado(false)           -> valor por defecto; lo recalculará
    //                                            fireActionRule_ejecutarImportacion.
    //   - entidad.setLog(null)               -> lo escribirá fireActionRule_ejecutarImportacion.
    // Sobrescribe SIEMPRE estos campos ignorando cualquier valor que hubiese venido del cliente.

    private void fireActionRule_ejecutarImportacion(
        com.educaflow.subsystem.importacion.db.TareaImportacion entidad)
    // Regla de negocio "ejecutar el proceso de importación y registrar resultado":
    //   1. importador = ImportadorFicheroFactory.create(entidad.getTipoFichero(), entidad.getFichero())
    //   2. try { resultado = importador.importar() } catch (ImportadorException ex) { ... }
    //   3. En el camino feliz (no alcanzable en esta iniciativa por guía 8):
    //        entidad.setEstado(true)
    //        entidad.setLog(resultado.log())
    //        entidad.setCentro(resultado.centro())
    //        entidad.setCurso(resultado.curso())
    //        entidad.setFechaExportacion(LocalDateTime.now())
    //   4. En el camino con excepción (siempre en esta iniciativa):
    //        entidad.setEstado(false)
    //        entidad.setLog(ex.getMessage())   // será "@TODO: Importación no implementada todavía"
    // NUNCA repropaga la excepción: la tarea queda persistida con estado=false y log
    // explicativo (regla del análisis: "todas las importaciones se deben guardar,
    // aunque fallen").
```

Notas:
- La interfaz hereda `update(...)` y `remove(...)` de `ModelService`/`DefaultModelService`. No es necesario sobrescribirlas: `validateUpdate`/`validateRemove` se invocan por el controlador antes de cualquier intento de actualización/borrado, y devuelven siempre errores que abortan la operación.
- No se inyecta `ModelServiceFactory` aquí porque el servicio no necesita resolver otros servicios.

**Verificación:** compila con `./gradlew clean build --info`. `ModelServiceFactory.resolve(TareaImportacion.class)` devuelve una instancia de esta clase.

---

### Paso 9 — Crear el controlador `TareaImportacionController`

Crear `src/main/java/com/educaflow/subsystem/importacion/controller/TareaImportacionController.java`.

- FQCN: `com.educaflow.subsystem.importacion.controller.TareaImportacionController`.

```
public class TareaImportacionController

    @com.google.inject.Inject
    private com.axelor.db.modelservice.ModelServiceFactory modelServiceFactory;

    @com.axelor.meta.CallMethod
    @com.google.inject.persist.Transactional
    public void subir(
        com.axelor.rpc.ActionRequest actionRequest,
        com.axelor.rpc.ActionResponse actionResponse)
    // Acción del botón "Importar" del modal `TareaImportacion-Subir@Subir-form`
    // en fase de entrada. Flujo:
    //   1. final TareaImportacionService tareaImportacionService =
    //          (TareaImportacionService) modelServiceFactory.resolve(TareaImportacion.class);
    //   2. AllowProperties allowProperties =
    //          AllowProperties.createAllowProperties(Map.of(
    //              "tipoFichero", Map.of(),
    //              "fichero",     Map.of()));
    //      Esta es la barrera de transporte de V-004: ningún otro campo recibido
    //      del cliente se acepta (usuario/fechaImportacion/estado/log/fechaExportacion/
    //      centro/curso).
    //   3. ActionRequestHelper<TareaImportacion> actionRequestHelper =
    //          new ActionRequestHelper<>(actionRequest, TareaImportacion.class);
    //      TareaImportacion tareaImportacion = actionRequestHelper.getModel(allowProperties);
    //   4. Validar antes de insertar:
    //        Optional<BusinessMessages> validation = tareaImportacionService.validateInsert(tareaImportacion);
    //        if (validation.isPresent()) {
    //            new ActionResponseHelper(actionResponse)
    //                .doResponseBusinessMessagesAsError(validation.get());
    //            return;   // no se llama a insert; el modal sigue en fase de entrada.
    //        }
    //      Cubre V-001/V-002/V-003 en servidor.
    //   5. TareaImportacion tareaImportacionGuardada = tareaImportacionService.insert(tareaImportacion);
    //      El servicio asigna usuario/fechaImportacion, ejecuta el importador
    //      (que en esta iniciativa siempre falla -> estado=false, log="@TODO:...")
    //      y persiste.
    //   6. actionResponse.setValues(tareaImportacionGuardada);
    //      Vuelca al form todos los campos calculados (id, usuario, fechaImportacion,
    //      estado, log...). Como `id != null` tras esto, el panel "Resultado" del
    //      form se muestra automáticamente y el "Entrada" se oculta (los showIf del
    //      form están atados a `id == null` / `id != null`).
    //      No se llama a setReload ni a setSignal: el modal queda abierto en fase
    //      de resultado hasta que el usuario pulse "Aceptar" (operación 4 del análisis,
    //      guía 7.1).

    @com.axelor.meta.CallMethod
    public void aceptar(
        com.axelor.rpc.ActionRequest actionRequest,
        com.axelor.rpc.ActionResponse actionResponse)
    // Acción del botón "Aceptar" del modal en fase de resultado.
    // No persiste nada: la tarea ya está guardada.
    //   actionResponse.setSignal("close", null);
    // Cierra el popup; el listado padre permanece abierto y refrescable.
```

Notas:
- Imports: `com.educaflow.base.infrastructure.axelorhelper.ActionRequestHelper`, `com.educaflow.base.infrastructure.axelorhelper.ActionResponseHelper`, `com.educaflow.base.util.AllowProperties`.
- Los parámetros se llaman exactamente `actionRequest` y `actionResponse` (regla de `k-sistemas/controladores.md`).
- `@Transactional` viene de `com.google.inject.persist.Transactional`.
- No hay métodos para editar ni borrar: la inmutabilidad se sostiene en cliente (`canEdit="false"`, `canDelete="false"`, `canSave="false"`) y en servidor (`validateUpdate`/`validateRemove`).

**Verificación:** compila con `./gradlew clean build --info`.

---

### Paso 10 — Crear la vista principal `TareaImportacion.xml` (listado + detalle solo lectura)

Crear `src/main/java/com/educaflow/subsystem/importacion/views/TareaImportacion.xml`.

Un único `<action-view>` en este fichero (regla arquitectónica: "un action-view por fichero").

**Vistas declaradas:**

- **Grid `subsysImportacion.TareaImportacion@Main-grid`** (`model="com.educaflow.subsystem.importacion.db.TareaImportacion"`).
  - Atributos obligatorios por la guía 7.3: `canAdvanceSearch="false" canRefresh="false" allowSearchFields="false"`.
  - Atributos de inmutabilidad: `canNew="false"` (la creación va por el botón "Importar nueva" del toolbar, no por el `+` nativo), `canEdit="false"`, `canDelete="false"`, `canSave="false"`.
  - Atributos: `editable="false" edit-icon="false" x-selector="none" canEditOnClick="false" canViewOnClick="true"` (al pulsar una fila se abre el form de detalle en modo lectura).
  - `orderBy="-fechaImportacion"` (operación 1 del análisis: más recientes primero).
  - Columnas, en este orden: `fechaImportacion`, `tipoFichero`, `centro`, `curso`, `fechaExportacion`, `estado`, `usuario`.
  - Para que la columna `estado` muestre "correcta"/"fallida" en lugar de "true"/"false", el campo `<field name="estado">` lleva la etiqueta de selección Axelor `x-true-text="correcta"` y `x-false-text="fallida"` (atributos de campo boolean en Axelor 8.1).

- **Form `subsysImportacion.TareaImportacion@Main-form`** (`model="com.educaflow.subsystem.importacion.db.TareaImportacion"`) — detalle solo lectura (operación 2 del análisis).
  - Atributos: `width="large" canAttach="false" canBack="false" canDelete="false" canNew="false" canSave="false" canMore="false" canBackOnSave="true" readonlyIf="true"`.
  - El `readonlyIf="true"` a nivel form garantiza que ningún campo sea editable (refuerzo cliente de V-005).
  - **Panel `panelDatos`** (`title="Datos de la importación"`): campos `usuario`, `centro`, `curso`, `fechaImportacion`, `fechaExportacion`, `tipoFichero`, `fichero` (este último con `widget="binary-link"`).
  - **Panel `panelResultado`** (`title="Resultado"`): campo `estado` (con `x-true-text="correcta"` / `x-false-text="fallida"`) y campo `log` (`widget="text"`, `colSpan="12"`, multilínea preservando saltos de línea).
  - No hay panel de botones: el form es exclusivamente de consulta.

**Acción declarada:**

- **`<action-view name="subsysImportacion.TareaImportacion@Main-action" title="Ficheros importación" model="com.educaflow.subsystem.importacion.db.TareaImportacion">`**
  - `<view type="grid" name="subsysImportacion.TareaImportacion@Main-grid"/>`
  - `<view type="form" name="subsysImportacion.TareaImportacion@Main-form"/>`
  - `<view-param name="show-toolbar-form" value="false"/>` (guía 7.2).
  - `<view-param name="forceEdit" value="true"/>` (guía 7.2). Aunque `forceEdit=true`, el form lleva `readonlyIf="true"` y `canSave="false"`, por lo que el modo edición es visual pero ningún campo se puede modificar.
  - Toolbar superior: un único `<button name="btnImportarNueva" title="Importar nueva" onClick="subsysImportacion.TareaImportacion@Subir-action"/>`. Es el único punto de entrada al modal de subida.
  - **No lleva `<domain>`**: los administradores ven todas las importaciones de todos los centros (regla "multicentro sin filtrado" del análisis).
  - **No lleva `groups`**: la restricción a `admins` viene del menuitem `administracionSv-importacion-menuitem` (único punto de entrada).

**No se declaran** `action-record`, `action-attrs`, `action-method`, `action-validate` ni `action-condition` adicionales en este fichero — el detalle es solo lectura puro.

**Verificación:** `./gradlew clean build --info`; al abrir el menú "Administración SV > Ficheros importación" como admin, se muestra el listado vacío con las columnas correctas y sin botones de búsqueda avanzada/recarga.

---

### Paso 11 — Crear la vista modal `TareaImportacion-Subir.xml` (modal en dos fases)

Crear `src/main/java/com/educaflow/subsystem/importacion/views/TareaImportacion-Subir.xml`.

Un único `<action-view>` en este fichero (regla arquitectónica: "un action-view por fichero").

**Vistas declaradas:**

- **Form `subsysImportacion.TareaImportacion@Subir-form`** (`model="com.educaflow.subsystem.importacion.db.TareaImportacion"`) — modal en dos fases.
  - Atributos: `width="large" canAttach="false" canBack="false" canDelete="false" canNew="false" canSave="false" canMore="false"`. (Sin `canBackOnSave` porque es modal; guía 7.2 obliga a `canBack="false"`.)
  - La conmutación entre fases se basa en el `id` de la entidad:
    - **Fase entrada** (visible mientras la tarea no se ha guardado): paneles con `showIf="id == null && cid == null"` (ó `hideIf="id != null"` — la forma exacta queda a juicio del implementador, ambas son equivalentes en Axelor 8.1).
    - **Fase resultado** (visible una vez guardada): paneles con `showIf="id != null"`.
  - **Panel `panelEntrada`** (`title="Nueva importación"`, `showIf="id == null"`):
    - Campo `tipoFichero` con `widget="SwitchSelect"` (guía 6), `required="true"`, `colSpan="12"`.
    - Campo `fichero` (`widget="binary-link"`), `required="true"`, `colSpan="12"`.
  - **Panel `panelResultado`** (`title="Resultado"`, `showIf="id != null"`, `readonlyIf="true"`):
    - Sub-panel `panelDatosResultado`: campos `usuario`, `fechaImportacion`, `tipoFichero`, `centro`, `curso`, `fechaExportacion`, `fichero` (todos readonly).
    - Sub-panel `panelEstadoLog`: campo `estado` (con `x-true-text="correcta"` / `x-false-text="fallida"`) y campo `log` (`widget="text"`, `colSpan="12"`, multilínea).
  - **Panel `panelBotones`** (`name="buttons-panel"`, `showFrame="false"`, `colSpan="12"`):
    - `<button name="btnCancelar" title="Cancelar" onClick="subsysImportacion.TareaImportacion@Subir-btnCancelar-action" outline="true" colSpan="2" colOffset="6" showIf="id == null"/>` — visible sólo en fase entrada.
    - `<button name="btnImportar" title="Importar" onClick="subsysImportacion.TareaImportacion@Subir-btnImportar-action" colSpan="2" showIf="id == null"/>` — visible sólo en fase entrada.
    - `<button name="btnAceptar" title="Aceptar" onClick="subsysImportacion.TareaImportacion@Subir-btnAceptar-action" colSpan="2" colOffset="8" showIf="id != null"/>` — visible sólo en fase resultado.

**Acción de apertura:**

- **`<action-view name="subsysImportacion.TareaImportacion@Subir-action" title="Importar fichero" model="com.educaflow.subsystem.importacion.db.TareaImportacion">`**
  - `<view type="form" name="subsysImportacion.TareaImportacion@Subir-form"/>`
  - `<view-param name="popup" value="true"/>` (modal).
  - `<view-param name="popup-save" value="false"/>` (la persistencia la dispara el botón "Importar", no el save nativo).
  - `<view-param name="show-toolbar-form" value="false"/>` (guía 7.2).
  - `<view-param name="forceEdit" value="true"/>` (guía 7.2 — necesario para que la fase entrada sea editable).

**Acciones declaradas en este fichero** (todas por nombre + propósito; sin XML literal):

| Nombre | Tipo | Propósito | Campos/condiciones |
|--------|------|-----------|-----------------------|
| `subsysImportacion.TareaImportacion@Subir-btnCancelar-action` | `action-group` | Acción del botón "Cancelar" en fase entrada. Cierra el modal sin persistir nada. | Una sola acción interna: `<action name="close"/>`. |
| `subsysImportacion.TareaImportacion@Subir-Local-validateImportar-action` | `action-condition` | Validaciones cliente V-001 y V-003 antes de invocar el servidor. | Dos checks: (1) campo `tipoFichero`, sin `if` (verifica != null), mensaje correspondiente a V-001 que transmita la obligatoriedad e incluya los cuatro valores válidos. (2) campo `fichero`, sin `if` (verifica != null), mensaje correspondiente a V-003 que transmita la obligatoriedad. |
| `subsysImportacion.TareaImportacion@Subir-Remote-subir-action` | `action-method` | Invoca `TareaImportacionController.subir(actionRequest, actionResponse)`. Es donde el servidor persiste la tarea, ejecuta la importación y vuelca los campos calculados al response. | `<call class="com.educaflow.subsystem.importacion.controller.TareaImportacionController" method="subir"/>` con `model="com.educaflow.subsystem.importacion.db.TareaImportacion"`. |
| `subsysImportacion.TareaImportacion@Subir-btnImportar-action` | `action-group` | Acción del botón "Importar" en fase entrada. Orquesta validación local → llamada remota. | Secuencia: 1) `subsysImportacion.TareaImportacion@Subir-Local-validateImportar-action`, 2) `subsysImportacion.TareaImportacion@Subir-Remote-subir-action`. **Sin `save` final**: la persistencia la hace el controlador. Tras la llamada remota, `id != null` y los `showIf` del form conmutan automáticamente al panel "Resultado". |
| `subsysImportacion.TareaImportacion@Subir-Remote-aceptar-action` | `action-method` | Invoca `TareaImportacionController.aceptar(actionRequest, actionResponse)`. El controlador emite `setSignal("close")`. | `<call class="com.educaflow.subsystem.importacion.controller.TareaImportacionController" method="aceptar"/>` con `model="com.educaflow.subsystem.importacion.db.TareaImportacion"`. |
| `subsysImportacion.TareaImportacion@Subir-btnAceptar-action` | `action-group` | Acción del botón "Aceptar" en fase resultado. Cierra el modal. | Una sola acción interna: `subsysImportacion.TareaImportacion@Subir-Remote-aceptar-action`. |

Orden y comentarios obligatorios del fichero (k-vistas):
- Bloque de cabecera "TareaImportacion : Vistas".
- Orden: action-view → form → bloques de acciones (`Acciones de las tareas principales` con los `action-group`; `Acciones de Validaciones en local` con el `action-condition`; `Acciones de llamadas Remotas al servidor` con los dos `action-method`). No hay `action-record` ni `action-attrs` en este fichero.

**Verificación:** `./gradlew clean build --info`; desde el listado, "Importar nueva" abre el modal con `tipoFichero` (SwitchSelect) y `fichero`; pulsar "Importar" sin rellenar nada muestra los mensajes V-001 y V-003 (cliente); rellenando ambos y pulsando "Importar", el modal pasa a fase "Resultado" mostrando `estado="fallida"` y `log="@TODO: Importación no implementada todavía"`; pulsar "Aceptar" cierra el modal.

---

### Paso 12 — Seguridad

**No se crean ficheros nuevos** en `subsystem/security/data-init/input/` ni se modifica `input-config.xml`.

Justificación (de `k-seguridad/permisos.md`):
- Axelor concede acceso total al grupo `admins` sin necesidad de roles ni permisos explícitos.
- El menuitem `administracionSv-importacion-menuitem` ya lleva `groups="admins"` y es el único punto de entrada al `@Main-action`. El `@Subir-action` se invoca desde el toolbar del listado, también accesible sólo a `admins`.
- Ningún otro tipo de usuario (Supervisor/Profesor/etc.) ve el menú ni puede invocar los action-views.

Cobertura por validación:
- **V-007** (acceso restringido a `admins`): garantizado por `groups="admins"` del menuitem y por la política por defecto de Axelor sobre el grupo `admins`.

**Verificación:** un usuario sin grupo `admins` no ve el menú "Ficheros importación".

---

### Paso 13 — Datos iniciales

**No se cargan** registros iniciales. La tabla `TareaImportacion` arranca vacía. El enum `TipoFicheroImportacion` se materializa como tipo Java generado: no requiere `data-init`.

---

### Paso 14 — Verificación final

1. Compilar el proyecto: `./gradlew clean build --info`. Debe terminar sin errores.
2. Verificar que los ficheros generados existen bajo `build/src-gen/com/educaflow/subsystem/importacion/db/`:
   - `TareaImportacion.java`
   - `TipoFicheroImportacion.java`
   - `TareaImportacionRepository.java` (repositorio abstracto generado).
3. Verificar (con `grep`) que el menuitem `administracionSv-importacion-menuitem` en `secretariavirtual/menus/menus.xml` apunta a `subsysImportacion.TareaImportacion@Main-action` (debe existir; no modificar).
4. Arrancar la aplicación: `./gradlew --no-daemon run --port 8080 --context-path /`.
5. Como usuario del grupo `admins`:
   - Acceder a **Administración SV > Ficheros importación** → grid vacío con columnas `fechaImportacion`, `tipoFichero`, `centro`, `curso`, `fechaExportacion`, `estado`, `usuario`. Sin búsqueda avanzada, recarga ni campos de búsqueda.
   - Pulsar "Importar nueva" → modal en fase entrada con `tipoFichero` (SwitchSelect con los cuatro valores) y `fichero`.
   - Pulsar "Importar" sin rellenar nada → aparecen los mensajes V-001 (`tipoFichero`) y V-003 (`fichero`) bajo los campos.
   - Rellenar `tipoFichero` (cualquier valor) y subir cualquier fichero, pulsar "Importar" → el modal pasa a fase Resultado mostrando `estado = "fallida"` y `log = "@TODO: Importación no implementada todavía"`, y los demás campos calculados (`usuario`, `fechaImportacion`) rellenos.
   - Pulsar "Aceptar" → el modal se cierra y el listado, al recargarse, muestra la nueva fila.
   - Pulsar una fila del listado → se abre el detalle en modo solo lectura con todos los campos (incluida la columna `estado` mostrando "correcta"/"fallida" como texto legible).
   - Comprobar que NO hay botones de editar ni borrar ni en el grid ni en el detalle.
6. Como usuario NO `admins` (p.ej. usuario `users`): comprobar que el menú "Ficheros importación" NO aparece.
7. (Opcional, prueba defensiva del servidor) Llamar a la API REST de Axelor con un PUT/DELETE sobre un registro existente y comprobar que devuelve el error V-005/V-006.

---

## Matriz de trazabilidad

Cada regla `V-XXX` y cada regla de negocio del análisis tiene al menos una ubicación concreta en el diseño.

### Validaciones V-XXX

| Regla | Capa | Ubicación | Comentario |
|-------|------|-----------|------------|
| V-001 (`tipoFichero` obligatorio) | modelo | `domains/TareaImportacion.xml`: `<enum name="tipoFichero" required="true">` | NOT NULL a nivel de BD. |
| V-001 (`tipoFichero` obligatorio) | cliente | `views/TareaImportacion-Subir.xml`: `action-condition` `subsysImportacion.TareaImportacion@Subir-Local-validateImportar-action` (check sobre `tipoFichero`) | Mensaje que transmite obligatoriedad + lista de los cuatro valores. |
| V-001 (`tipoFichero` obligatorio) | servidor | `TareaImportacionServiceImpl.validateInsert(TareaImportacion)` | Defensa en profundidad; añade `BusinessMessage` si llegara `null`. |
| V-002 (`tipoFichero` dominio cerrado) | modelo | `domains/TareaImportacion.xml`: `<enum ref="TipoFicheroImportacion">` | El tipo Java es el enumerado: imposible asignar un valor fuera del conjunto. |
| V-002 (`tipoFichero` dominio cerrado) | cliente | `views/TareaImportacion-Subir.xml`: campo `tipoFichero` con `widget="SwitchSelect"` | El widget solo renderiza los cuatro valores del enum. |
| V-002 (`tipoFichero` dominio cerrado) | servidor | `TareaImportacionServiceImpl.validateInsert(TareaImportacion)` | Reverificación defensiva. |
| V-003 (`fichero` obligatorio) | modelo | `domains/TareaImportacion.xml`: `<many-to-one name="fichero" required="true">` | NOT NULL en BD. Confirma la asunción A1*. |
| V-003 (`fichero` obligatorio) | cliente | `views/TareaImportacion-Subir.xml`: `action-condition` `subsysImportacion.TareaImportacion@Subir-Local-validateImportar-action` (check sobre `fichero`) | Mensaje que transmite obligatoriedad. |
| V-003 (`fichero` obligatorio) | servidor | `TareaImportacionServiceImpl.validateInsert(TareaImportacion)` | Defensa en profundidad. |
| V-004 (`usuario`, `fechaImportacion`, `estado`, `log`, `fechaExportacion` no asignables) | transporte | `TareaImportacionController.subir(actionRequest, actionResponse)`: `AllowProperties.createAllowProperties(Map.of("tipoFichero", Map.of(), "fichero", Map.of()))` | Sólo `tipoFichero` y `fichero` se aceptan del cliente. |
| V-004 (`usuario`, `fechaImportacion`) | servidor | `TareaImportacionServiceImpl.fireActionRule_asignarCamposSistema(TareaImportacion)` | Sobrescribe con `AuthUtils.getUser()` y `LocalDateTime.now()`. |
| V-004 (`estado`, `log`, `fechaExportacion`) | servidor | `TareaImportacionServiceImpl.fireActionRule_ejecutarImportacion(TareaImportacion)` | El sistema asigna estos campos según el resultado del importador. |
| V-005 (edición prohibida) | cliente | `views/TareaImportacion.xml`: form `@Main-form` con `readonlyIf="true"`, `canSave="false"`, `canNew="false"`; grid `@Main-grid` con `canEdit="false"` | La UI no expone edición. |
| V-005 (edición prohibida) | servidor | `TareaImportacionServiceImpl.validateUpdate(TareaImportacion)` | Devuelve siempre `BusinessMessages` con mensaje que transmite que las importaciones registradas no se modifican. |
| V-006 (borrado prohibido) | cliente | `views/TareaImportacion.xml`: grid `@Main-grid` y form `@Main-form` con `canDelete="false"` | La UI no expone borrado. |
| V-006 (borrado prohibido) | servidor | `TareaImportacionServiceImpl.validateRemove(TareaImportacion)` | Devuelve siempre `BusinessMessages` con mensaje que transmite que las importaciones no se borran. |
| V-007 (acceso restringido a `admins`) | menú/seguridad | `secretariavirtual/menus/menus.xml`: `administracionSv-importacion-menuitem groups="admins"` (ya existente) | Axelor oculta el menú al resto de grupos; concede acceso total al grupo `admins` por política por defecto. |

### Reglas de negocio del análisis (mapeo complementario)

| Regla de negocio | Capa | Ubicación | Comentario |
|------------------|------|-----------|------------|
| Operación 1 — Listar importaciones ordenado por fechaImportacion DESC | cliente | `views/TareaImportacion.xml`: grid `@Main-grid` con `orderBy="-fechaImportacion"` | Listado completo sin filtros. |
| Operación 2 — Consultar detalle solo lectura | cliente | `views/TareaImportacion.xml`: form `@Main-form` con `readonlyIf="true"` y todos los `can*="false"` | Detalle inmutable. |
| Operación 3 — Registrar nueva importación (persistencia + ejecución) | servidor | `TareaImportacionController.subir(actionRequest, actionResponse)` → `TareaImportacionService.insert(...)` → `fireActionRule_asignarCamposSistema` + `fireActionRule_ejecutarImportacion` | Orquesta la creación completa. |
| Operación 4 — Modal de subida en dos fases sin cierre automático | cliente | `views/TareaImportacion-Subir.xml`: form `@Subir-form` con paneles condicionados por `id == null` / `id != null`; controlador hace `actionResponse.setValues(...)` para que el modal pase a fase resultado sin cerrarse | El cierre lo dispara el botón Aceptar. |
| Operación 4 — Cierre del modal con "Aceptar" | servidor | `TareaImportacionController.aceptar(actionRequest, actionResponse)` → `actionResponse.setSignal("close", null)` | n/a. |
| Campo calculado `usuario` (auto, usuario logado) | servidor | `TareaImportacionServiceImpl.fireActionRule_asignarCamposSistema(TareaImportacion)` | `AuthUtils.getUser()`. |
| Campo calculado `fechaImportacion` (auto, now) | servidor | `TareaImportacionServiceImpl.fireActionRule_asignarCamposSistema(TareaImportacion)` | `LocalDateTime.now()`. |
| Campo calculado `estado` (resultado del proceso) | servidor | `TareaImportacionServiceImpl.fireActionRule_ejecutarImportacion(TareaImportacion)` | `false` si `ImportadorException`; `true` si `ResultadoImportacion`. |
| Campo calculado `log` (resultado del proceso) | servidor | `TareaImportacionServiceImpl.fireActionRule_ejecutarImportacion(TareaImportacion)` | Mensaje de la excepción (en esta iniciativa, `"@TODO: Importación no implementada todavía"`) o `ResultadoImportacion.log()`. |
| Campo calculado `fechaExportacion` (siempre null en esta iniciativa) | servidor | `TareaImportacionServiceImpl.fireActionRule_ejecutarImportacion(TareaImportacion)` y `fireActionRule_asignarCamposSistema` | Sólo se asigna en la rama de éxito, no alcanzable por guía 8. |
| Selección de implementación de `ImportadorFichero` según `tipoFichero` | servidor | `ImportadorFicheroFactory.create(TipoFicheroImportacion, MetaFile)` | XML para PROFESOR/ALUMNO/FAMILIAR; CSV para PROFESOR_EXTERNO (guía 5). |
| Importación nunca implementada — siempre lanza ImportadorException | servidor | `importador/impl/ImportadorUsuarioXML.importar()` y `importador/impl/ImportadorUsuarioCSV.importar()` | Lanzan `ImportadorException("@TODO: Importación no implementada todavía")` (guía 8). |
| Persistencia incluso cuando la importación falla | servidor | `TareaImportacionServiceImpl.fireActionRule_ejecutarImportacion(TareaImportacion)` captura `ImportadorException` y NO la repropaga | La tarea queda guardada con `estado=false` y `log` poblado. |
| Estado mostrado legible ("correcta"/"fallida"), nunca true/false | cliente | `views/TareaImportacion.xml` y `views/TareaImportacion-Subir.xml`: campo `estado` con atributos `x-true-text="correcta"` / `x-false-text="fallida"` | Aplicado en grid `@Main-grid`, form `@Main-form` y form `@Subir-form` (panel resultado). |
| Listado multicentro sin filtrado por centro/usuario | cliente | `views/TareaImportacion.xml`: `@Main-action` sin `<domain>` | Los administradores ven todas las importaciones. |
| Reutiliza el menú existente | menú | `secretariavirtual/menus/menus.xml` (no se modifica) | El menuitem `administracionSv-importacion-menuitem` ya apunta al `@Main-action`. |
| Persistencia del fichero subido (asunción A1*) | modelo | `domains/TareaImportacion.xml`: `<many-to-one name="fichero" ref="MetaFile" required="true">` | Conserva el fichero para auditoría. |
| `tipoFichero` con `SwitchSelect` (guía 6) | cliente | `views/TareaImportacion-Subir.xml`: campo `tipoFichero` con `widget="SwitchSelect"` | Renderiza los cuatro valores del enum sin BD. |
| Ocultación de toolbar y forceEdit (guía 7.2) | cliente | Ambos action-views (`@Main-action`, `@Subir-action`) con `show-toolbar-form=false` + `forceEdit=true`; ambos forms con `canBack="false"` | Cumple guía 7.2. |
| Grid sin búsqueda avanzada/recarga/campos de búsqueda (guía 7.3) | cliente | `views/TareaImportacion.xml`: grid `@Main-grid` con `canAdvanceSearch="false" canRefresh="false" allowSearchFields="false"` | Cumple guía 7.3. |
| Un `<action-view>` por fichero | estructura | `views/TareaImportacion.xml` contiene sólo `@Main-action`; `views/TareaImportacion-Subir.xml` contiene sólo `@Subir-action` | Cumple la regla arquitectónica de k-sistemas. |

---

## Notas de unificación

- **Conmutación entre fases del modal con `id == null` / `id != null`**: en lugar de un campo virtual transitorio (`_faseResultado`, `$resultadoVisible`), se conmutan los paneles directamente con `showIf` sobre el `id`. Es más simple y no necesita acciones extra: tras `service.insert(...)` el controlador vuelca la entidad guardada con `actionResponse.setValues(...)` y `id` deja de ser nulo, lo que dispara automáticamente el cambio de panel.
- **Validación servidor vía controlador, no vía `insert`**: el controlador llama explícitamente a `tareaImportacionService.validateInsert(...)` antes de `insert(...)`. Si hay mensajes, los muestra como diálogo modal de error con `doResponseBusinessMessagesAsError(...)` y NO invoca `insert`. Esto evita lanzar/capturar `BusinessException` en flujo normal y permite que el modal permanezca en fase entrada con los errores visibles.
- **Persistencia incluso al fallar el importador**: `fireActionRule_ejecutarImportacion` captura `ImportadorException` y la traduce a `estado=false` + `log`. Nunca repropaga, garantizando la regla del análisis "todas las importaciones se deben guardar, aunque fallen".
- **Boolean con texto legible**: se usan los atributos `x-true-text` y `x-false-text` del campo boolean en Axelor 8.1, evitando lógica extra.

## Conflictos detectados con guías

Ninguno bloqueante.

Aclaraciones:
1. **Asunción A1\*** (campo `fichero` obligatorio): el análisis lo marca como asunción a confirmar. Las guías no lo listan explícitamente en la sección 1 pero (a) la guía 5 implica que el importador recibe el fichero como entrada (luego debe almacenarse) y (b) la restricción del análisis "todas las importaciones se deben guardar, aunque fallen" exige conservarlo para auditoría. Se aplica con `required="true"` y queda incluida la asunción para confirmación del usuario.
2. **Guía 7.2 `forceEdit="true"` en el `@Main-action` (detalle solo lectura)**: la guía obliga a `forceEdit=true`. El form de detalle se mantiene inmutable mediante `readonlyIf="true"` y `canSave="false"`, por lo que el `forceEdit` no permite cambios reales; sólo afecta a la presentación visual. No es un conflicto, es un refuerzo defensivo.
