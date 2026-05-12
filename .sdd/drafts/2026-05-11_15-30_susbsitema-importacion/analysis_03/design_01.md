---
type: design
---

# Diseño: Subsistema de importación de usuarios

**Objetivo:** Crear el subsistema `importacion` con la entidad `TareaImportacion`, modificar el esquema de `UsuarioAutorizado` (en `subsystem/registrousuario`) y construir los servicios, controladores, vistas, menús y permisos que permiten a los administradores cargar masivamente DNIs desde ficheros XML (PROFESOR/ALUMNO/FAMILIAR) o CSV (PROFESOR_EXTERNO), registrando cada intento como una `TareaImportacion` inmutable y sincronizando automáticamente los tipos de usuario de los usuarios registrados del centro.
**Capa:** subsystem/importacion (y modificaciones a subsystem/registrousuario).
**Análisis de origen:** .sdd/drafts/2026-05-11_15-30_susbsitema-importacion/analysis_03/analysis.md
**Skills necesarios para la implementación:** k-sistemas, k-vistas, k-seguridad

---

## Ficheros a crear o modificar

| # | Acción | Ruta | Skill | Descripción |
|---|--------|------|-------|-------------|
| 1 | Crear | `src/main/java/com/educaflow/subsystem/importacion/domains/TareaImportacion.xml` | k-sistemas | Dominio nuevo con los 10 campos del análisis y `<finder-method>` para V-025. |
| 2 | Modificar | `src/main/java/com/educaflow/subsystem/registrousuario/domains/UsuarioAutorizado.xml` | k-sistemas | `curso` y `fechaExportacion` pasan a requeridos; `fechaExportacion` pasa de `<date>` a `<datetime>`; se elimina la unique-constraint `(centro,dni,tipoUsuario)` y se añade `(centro,dni,tipoUsuario,curso,fechaExportacion)`; se añaden finders para la resolución Actual/Anterior. |
| 3 | Crear | `src/main/java/com/educaflow/subsystem/importacion/service/TareaImportacionService.java` | k-sistemas | Interfaz que extiende `ModelService<TareaImportacion>` con `ejecutarImportacion(...)`. |
| 4 | Crear | `src/main/java/com/educaflow/subsystem/importacion/service/impl/TareaImportacionServiceImpl.java` | k-sistemas | Implementación que orquesta el flujo síncrono de Op-4. |
| 5 | Crear | `src/main/java/com/educaflow/subsystem/importacion/service/EjecutarImportacionDTO.java` | k-sistemas | Record con la entrada del flujo de importación desde el controlador. |
| 6 | Crear | `src/main/java/com/educaflow/subsystem/importacion/service/ResultadoImportacion.java` | k-sistemas | Record con el resultado del parseo devuelto por `ImportadorFichero`. |
| 7 | Crear | `src/main/java/com/educaflow/subsystem/importacion/service/ImportadorFichero.java` | k-sistemas | Interfaz con `ResultadoImportacion importar() throws ImportadorException`. |
| 8 | Crear | `src/main/java/com/educaflow/subsystem/importacion/service/ImportadorFicheroFactory.java` | k-sistemas | Factory estática que selecciona XML o CSV según el `TipoUsuario`. |
| 9 | Crear | `src/main/java/com/educaflow/subsystem/importacion/service/ImportadorException.java` | k-sistemas | `RuntimeException` con `detalleLog` para fallos de parseo y coherencia. |
| 10 | Crear | `src/main/java/com/educaflow/subsystem/importacion/service/impl/ImportadorUsuarioXML.java` | k-sistemas | Implementación XML (PROFESOR/ALUMNO/FAMILIAR). |
| 11 | Crear | `src/main/java/com/educaflow/subsystem/importacion/service/impl/ImportadorUsuarioCSV.java` | k-sistemas | Implementación CSV (PROFESOR_EXTERNO). |
| 12 | Crear | `src/main/java/com/educaflow/subsystem/registrousuario/service/UsuarioAutorizadoService.java` | k-sistemas | Interfaz que extiende `ModelService<UsuarioAutorizado>` con `insertarDesdeImportacion(...)`. |
| 13 | Crear | `src/main/java/com/educaflow/subsystem/registrousuario/service/impl/UsuarioAutorizadoServiceImpl.java` | k-sistemas | Implementación con inmutabilidad pública (V-028) y método interno consumido por la importación. |
| 14 | Modificar | `src/main/java/com/educaflow/subsystem/registrousuario/db/repo/UsuarioAutorizadoRepository.java` | k-sistemas | Eliminar todos los métodos legacy que referencian el campo `activo` (campo inexistente tras esta iniciativa). |
| 15 | Crear | `src/main/java/com/educaflow/subsystem/importacion/controller/TareaImportacionController.java` | k-sistemas | Controlador con `ejecutarImportacion`, `validateSave`, `validateDelete`. |
| 16 | Crear | `src/main/java/com/educaflow/subsystem/registrousuario/controllers/UsuarioAutorizadoController.java` | k-sistemas | Controlador con `validateSave`, `validateDelete` (blindaje API V-028). |
| 17 | Crear | `src/main/java/com/educaflow/subsystem/importacion/views/TareaImportacion.xml` | k-vistas | Vista `@Main` del listado y detalle de tareas (Op-1, Op-2, Op-3). |
| 18 | Crear | `src/main/java/com/educaflow/subsystem/importacion/views/TareaImportacion-wizard.xml` | k-vistas | Vista `@Wizard` del asistente de importación (Op-4). |
| 19 | Crear | `src/main/java/com/educaflow/subsystem/importacion/views/UsuarioAutorizado.xml` | k-vistas | Vista `@Main` del listado de usuarios autorizados (Op-5). |
| 20 | Modificar | `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | k-vistas | Añadir `administracionSv-usuariosAutorizados-menuitem`. El `administracionSv-importacion-menuitem` ya existe. |
| 21 | Crear | `src/main/resources/data-init/input/auth-importacion.xml` | k-seguridad | Permisos `TareaImportacion.admins` y `UsuarioAutorizado.admins` (read-only). |
| 22 | Modificar | `src/main/resources/data-init/input-config.xml` | k-seguridad | Registrar `auth-importacion.xml` antes del bloque que carga `auth.xml`. |
| 23 | Modificar | `src/main/resources/data-init/input/auth.xml` | k-seguridad | Asignar los nuevos permisos al grupo `admins` y eliminar `UsuarioAutorizado.all` de los grupos `admins` y `users`. |

Notas estructurales:
- No se crea módulo Guice para `TareaImportacionService` ni para `UsuarioAutorizadoService`: los descubre `ModelServiceFactory` por convención (paquete `service.impl`, sufijo `ServiceImpl`).
- No se crean listeners JPA: la lógica de negocio vive en el servicio como `fireActionRule_*`.
- No se crean ficheros `i18n_es.csv` ni `i18n_ca.csv`: los genera el script de build.
- No se crea `TareaImportacionRepository.java`: el único query relevante (`findCorrectaByCentroTipoCursoFecha` para V-025) se declara como `<finder-method>` en el dominio.
- `Centro.xml` ya declara la OTM `tareasImportacion mappedBy="centro"` apuntando a `com.educaflow.subsystem.importacion.db.TareaImportacion`; no se toca.

---

## Pasos

### Paso 1 — Recursos estáticos

No aplica. Los esquemas XSD `profesores.xsd`, `alumnos.xsd` y `familiares.xsd` ya existen en `src/main/resources/data-import/schemas/`. Se reutilizan directamente. `ImportadorUsuarioXML` los carga del classpath con `getClass().getResourceAsStream("/data-import/schemas/<nombre>.xsd")`.

**Verificación:** comprobar que los tres ficheros existen.

---

### Paso 2 — Dominios

#### 2.1 Crear `subsystem/importacion/domains/TareaImportacion.xml` (NUEVO)

XML COMPLETO:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<domain-models xmlns="http://axelor.com/xml/ns/domain-models"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://axelor.com/xml/ns/domain-models https://axelor.com/xml/ns/domain-models/domain-models_8.1.xsd">

    <module name="importacion" package="com.educaflow.subsystem.importacion.db"/>

    <entity name="TareaImportacion">
        <datetime    name="fechaImportacion"   title="Fecha de importación"   required="true"/>
        <many-to-one name="centro"             ref="com.educaflow.subsystem.common.db.Centro"      title="Centro"             required="true"/>
        <many-to-one name="tipoUsuario"        ref="com.educaflow.subsystem.common.db.TipoUsuario" title="Tipo de usuario"    required="true"/>
        <string      name="nombreFichero"      title="Nombre del fichero"     required="true"/>
        <many-to-one name="fichero"            ref="com.axelor.meta.db.MetaFile"                   title="Fichero original"   required="true"/>
        <many-to-one name="usuarioImportador"  ref="com.axelor.auth.db.User"                       title="Usuario importador" required="true"/>
        <integer     name="curso"              title="Curso académico"        required="true"/>
        <datetime    name="fechaExportacion"   title="Fecha de exportación"   required="true"/>
        <boolean     name="estado"             title="Correcta"               required="true" help="true = importación correcta, false = importación fallida"/>
        <string      name="log"                title="Log"                    required="true" large="true" multiline="true"/>

        <!-- V-025: existe ya una TareaImportacion CORRECTA con la misma combinación (centro, tipoUsuario, curso, fechaExportacion). -->
        <finder-method name="findCorrectaByCentroTipoCursoFecha"
                       using="com.educaflow.subsystem.common.db.Centro:centro,com.educaflow.subsystem.common.db.TipoUsuario:tipoUsuario,Integer:curso,java.time.LocalDateTime:fechaExportacion"
                       filter="self.centro = :centro AND self.tipoUsuario = :tipoUsuario AND self.curso = :curso AND self.fechaExportacion = :fechaExportacion AND self.estado = true"
                       all="false"/>
    </entity>

</domain-models>
```

Notas:
- Sin atributo `repository`: Axelor genera un `AbstractTareaImportacionRepository`; no se crea `TareaImportacionRepository.java` porque el único query lo provee el `<finder-method>`.
- Sin `namecolumn`: los listados se ordenan por `fechaImportacion`.

#### 2.2 Modificar `subsystem/registrousuario/domains/UsuarioAutorizado.xml`

XML COMPLETO de la versión nueva (sustituye al actual):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<domain-models xmlns="http://axelor.com/xml/ns/domain-models"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://axelor.com/xml/ns/domain-models https://axelor.com/xml/ns/domain-models/domain-models_8.1.xsd">

    <module name="registro" package="com.educaflow.subsystem.registrousuario.db"/>

    <entity name="UsuarioAutorizado" repository="abstract">
        <many-to-one name="centro"       ref="com.educaflow.subsystem.common.db.Centro"      title="Centro"           required="true"/>
        <string      name="dni"          title="dni__!!"                                                              required="true"/>
        <many-to-one name="tipoUsuario"  ref="com.educaflow.subsystem.common.db.TipoUsuario" title="Tipo de usuario"  required="true"/>
        <integer     name="curso"        title="Curso académico"                                                      required="true"/>
        <datetime    name="fechaExportacion" title="Fecha de exportación"                                              required="true"/>

        <unique-constraint columns="centro,dni,tipoUsuario,curso,fechaExportacion"/>

        <!-- Lista de DNIs distintos del centro con un tipoUsuario y curso dados (universo de las reglas XML). -->
        <finder-method name="findByCentroAndTipoUsuarioAndCurso"
                       using="com.educaflow.subsystem.common.db.Centro:centro,com.educaflow.subsystem.common.db.TipoUsuario:tipoUsuario,Integer:curso"
                       filter="self.centro = :centro AND self.tipoUsuario = :tipoUsuario AND self.curso = :curso"
                       all="true"/>

        <!-- Historial de un DNI para (centro, tipoUsuario, curso), ordenado por fechaExportacion descendente.
             El servicio toma la primera para conocer la fecha máxima (A12) y el resto para evaluar "Anterior". -->
        <finder-method name="findByCentroDniTipoCursoOrderByFechaDesc"
                       using="com.educaflow.subsystem.common.db.Centro:centro,String:dni,com.educaflow.subsystem.common.db.TipoUsuario:tipoUsuario,Integer:curso"
                       filter="self.centro = :centro AND self.dni = :dni AND self.tipoUsuario = :tipoUsuario AND self.curso = :curso"
                       orderBy="-fechaExportacion"
                       all="true"/>

        <!-- Comprueba si existe al menos una fila con fechaExportacion estrictamente anterior a la pasada
             para la terna (centro, dni, tipoUsuario, curso). Cubre la condición "Anterior" sin traerse el historial completo. -->
        <finder-method name="existsAnteriorByCentroDniTipoCurso"
                       using="com.educaflow.subsystem.common.db.Centro:centro,String:dni,com.educaflow.subsystem.common.db.TipoUsuario:tipoUsuario,Integer:curso,java.time.LocalDateTime:fechaExportacion"
                       filter="self.centro = :centro AND self.dni = :dni AND self.tipoUsuario = :tipoUsuario AND self.curso = :curso AND self.fechaExportacion &lt; :fechaExportacion"
                       all="false"/>
    </entity>

</domain-models>
```

Cambios respecto al esquema actual:
- `curso` pasa de opcional a `required="true"`.
- `fechaExportacion` pasa de `<date>` a `<datetime>` y `required="true"`.
- Eliminada `<unique-constraint columns="centro,dni,tipoUsuario"/>`.
- Añadida `<unique-constraint columns="centro,dni,tipoUsuario,curso,fechaExportacion"/>` (V-016).
- Eliminados todos los finders del XML antiguo (no había ninguno declarado; lo que hay es código Java legacy en el repositorio que se limpia en el paso 4).
- Tres `<finder-method>` nuevos para las reglas XML (Actual/Anterior) y el universo de evaluación.
- Se mantiene `repository="abstract"` porque ya existe `UsuarioAutorizadoRepository.java` que extiende el abstract.

**Migración (A10):** Axelor genera el DDL al arrancar; los registros preexistentes deben quedar compatibles con la nueva unicidad (curso y fechaExportacion obligatorios; fechaExportacion migrada de `date` a `datetime`). El script de generación es responsabilidad del framework.

**Verificación:** `./gradlew clean build --info`; comprobar que se regeneran `build/src-gen/.../db/TareaImportacion.java` y `db/UsuarioAutorizado.java` con los campos correctos.

---

### Paso 3 — Servicios

> Todas las consultas JPA se canalizan por los `<finder-method>` del dominio. No hay `.filter().bind().fetch()` inline en los servicios.

#### 3.1 `com.educaflow.subsystem.importacion.service.ResultadoImportacion` (record)

```java
public record ResultadoImportacion(
    Centro centro,
    Integer curso,
    LocalDateTime fechaExportacion,
    List<String> dnisValidos,
    int totalLeidos,
    int totalDuplicadosIntraFichero,
    int totalDniInvalidos,
    String logParcial
) {}
```

Comentario:
- Contenedor inmutable que devuelve `ImportadorFichero.importar()` tras parsear y filtrar el fichero.
- `dnisValidos`: lista deduplicada intra-fichero, con DNIs válidos según `DniUtil.isValid`.
- `logParcial`: texto humano que acumula líneas por cada DNI inválido (V-017) y duplicado intra-fichero (V-029); se concatena más tarde al log final de la `TareaImportacion`.

#### 3.2 `com.educaflow.subsystem.importacion.service.EjecutarImportacionDTO` (record)

```java
public record EjecutarImportacionDTO(
    TipoUsuario tipoUsuario,
    MetaFile fichero,
    String nombreFichero
) {}
```

Comentario:
- DTO de entrada del flujo Op-4. El controlador lo construye con los dos campos del asistente más el `nombreFichero` (obtenido de `MetaFile.getFileName()` o del propio asistente). El centro activo, el usuario importador y el instante de la importación los resuelve el servicio.

#### 3.3 `com.educaflow.subsystem.importacion.service.ImportadorException` (clase)

```java
public class ImportadorException extends RuntimeException {
    public ImportadorException(String detalleLog);
    public String getDetalleLog();
}
```

Comentario:
- Portadora del motivo a registrar en el log de la `TareaImportacion` fallida cuando una validación previa de formato/coherencia falla en el importador (V-021/V-022/V-023/V-024).

#### 3.4 `com.educaflow.subsystem.importacion.service.ImportadorFichero` (interfaz)

```java
public interface ImportadorFichero {
    /**
     * Valida formato (V-021/V-022) y coherencia (V-023/V-024), parsea el fichero,
     * extrae centro/curso/fechaExportacion y construye la lista de DNIs aplicando
     * V-017 (formato) y V-029 (deduplicación intra-fichero). NO inserta UsuarioAutorizado,
     * NO toca usuarios registrados y NO registra TareaImportacion.
     * Lanza ImportadorException con detalleLog para fallos V-021..V-024.
     */
    ResultadoImportacion importar() throws ImportadorException;
}
```

#### 3.5 `com.educaflow.subsystem.importacion.service.ImportadorFicheroFactory` (clase)

```java
public final class ImportadorFicheroFactory {
    private ImportadorFicheroFactory() {}
    public static ImportadorFichero create(TipoUsuario tipoUsuario,
                                           MetaFile fichero,
                                           Centro centroActivo,
                                           LocalDateTime instanteImportacion);
}
```

Comentario:
- Si `tipoUsuario.getCodigo()` ∈ {`PROFESOR`, `ALUMNO`, `FAMILIAR`} → devuelve `new ImportadorUsuarioXML(fichero, tipoUsuario, centroActivo)`.
- Si `tipoUsuario.getCodigo()` = `PROFESOR_EXTERNO` → devuelve `new ImportadorUsuarioCSV(fichero, tipoUsuario, centroActivo, instanteImportacion)`.
- Cualquier otro código → `IllegalArgumentException`. Es una rama defensiva: V-019 (SwitchSelect del wizard) ya filtra antes en el cliente.

#### 3.6 `com.educaflow.subsystem.importacion.service.impl.ImportadorUsuarioXML` (clase)

```java
public class ImportadorUsuarioXML implements ImportadorFichero {

    public ImportadorUsuarioXML(MetaFile fichero, TipoUsuario tipoUsuario, Centro centroActivo);

    @Override
    public ResultadoImportacion importar() throws ImportadorException;
}
```

Comentario del cuerpo de `importar()`:
- Carga los bytes del MetaFile con `MetaFileUtil.downloadContent(fichero)`.
- Selecciona el XSD según `tipoUsuario.getCodigo()`:
  - `PROFESOR` → `/data-import/schemas/profesores.xsd`.
  - `ALUMNO` → `/data-import/schemas/alumnos.xsd`.
  - `FAMILIAR` → `/data-import/schemas/familiares.xsd`.
- V-021/V-023: invoca `XMLUtil.validarConSchema(xmlStream, xsdStream)`. Si devuelve `Optional` presente, lanza `ImportadorException` con un detalle que transmita el `tipoUsuario` seleccionado y el mensaje del validador (A15). La elección del XSD según el `tipoUsuario` cubre conjuntamente V-021 (formato) y V-023 (coherencia tipo↔estructura): si el fichero no tiene la sección esperada por el tipo, el XSD lo rechaza con un mensaje específico.
- Parsea el DOM con `XMLUtil.getDocument(bytes)`.
- Lee los atributos del elemento raíz `<centro>` con `XMLUtil.getStringAttribute`: `codigo`, `curso`, `fechaExportacion`.
- V-024: compara `codigo` con `centroActivo.getCode()`. Si difieren, lanza `ImportadorException` con un detalle que transmita ambos códigos.
- Convierte `curso` a `Integer` y `fechaExportacion` a `LocalDateTime` con el patrón `dd/MM/yyyy HH:mm:ss`. Si el patrón falla, lanza `ImportadorException` con detalle.
- Itera los elementos hijos `<docente>`, `<alumno>` o `<familiar>` (según `tipoUsuario`) extrayendo el atributo `documento`.
- V-029: dedup con un `LinkedHashSet`; los duplicados se descartan del resultado y se anotan en `logParcial` con un texto que transmita el DNI.
- V-017: `DniUtil.isValid(documento)`. Los inválidos se descartan y se anotan en `logParcial` con un texto que transmita el DNI.
- Devuelve `ResultadoImportacion(centroActivo, curso, fechaExportacion, dnisValidos, totalLeidos, totalDuplicadosIntraFichero, totalDniInvalidos, logParcial)`.

#### 3.7 `com.educaflow.subsystem.importacion.service.impl.ImportadorUsuarioCSV` (clase)

```java
public class ImportadorUsuarioCSV implements ImportadorFichero {

    public ImportadorUsuarioCSV(MetaFile fichero, TipoUsuario tipoUsuario, Centro centroActivo, LocalDateTime instanteImportacion);

    @Override
    public ResultadoImportacion importar() throws ImportadorException;
}
```

Comentario del cuerpo de `importar()`:
- Carga los bytes del MetaFile con `MetaFileUtil.downloadContent`.
- V-022: intenta decodificar UTF-8 a texto plano. Si el contenido no es legible como texto, lanza `ImportadorException` con detalle que transmita el formato esperado (líneas de DNIs, cabecera opcional).
- Trocea por líneas y elimina líneas en blanco con `trim()`.
- A13 (cabecera opcional): si la primera línea NO es DNI válido según `DniUtil.isValid`, se descarta silenciosamente; si SÍ lo es, se procesa como cualquier otra línea. No se anota en el log.
- Por cada línea restante:
  - V-029: dedup con `LinkedHashSet`; duplicado → anotar en `logParcial`.
  - V-017: si `DniUtil.isValid(linea)` = false → anotar en `logParcial`.
  - Válido y nuevo → añadir a `dnisValidos`.
- Devuelve `ResultadoImportacion(centroActivo, centroActivo.getCurso(), instanteImportacion, dnisValidos, totalLeidos, totalDuplicadosIntraFichero, totalDniInvalidos, logParcial)`.
- V-032 (tolerancia 0): no se lanza excepción si `dnisValidos` queda vacío; la tarea seguirá adelante en el servicio.

#### 3.8 `com.educaflow.subsystem.registrousuario.service.UsuarioAutorizadoService` (interfaz)

```java
public interface UsuarioAutorizadoService extends ModelService<UsuarioAutorizado> {

    /** V-028: SIEMPRE rechaza con mensaje de inmutabilidad. */
    @Override Optional<BusinessMessages> validateInsert(UsuarioAutorizado entity);

    /** V-028: SIEMPRE rechaza con mensaje de inmutabilidad. */
    @Override Optional<BusinessMessages> validateUpdate(UsuarioAutorizado entity, UsuarioAutorizado original);

    /** V-028: SIEMPRE rechaza con mensaje de inmutabilidad. */
    @Override Optional<BusinessMessages> validateRemove(UsuarioAutorizado entity);

    /**
     * Único punto autorizado para crear UsuarioAutorizado.
     * Lo invoca exclusivamente TareaImportacionServiceImpl durante el flujo Op-4.
     * Aplica V-011..V-015 implícitamente (las marcas required del dominio rechazan nulos).
     * Si choca con V-016 (unique-constraint), captura la PersistenceException y devuelve Optional.empty()
     * (sólo puede ocurrir como red de seguridad: el importador ya filtra duplicados intra-fichero con V-029).
     */
    Optional<UsuarioAutorizado> insertarDesdeImportacion(
        Centro centro, String dni, TipoUsuario tipoUsuario, Integer curso, LocalDateTime fechaExportacion);
}
```

#### 3.9 `com.educaflow.subsystem.registrousuario.service.impl.UsuarioAutorizadoServiceImpl`

```java
public class UsuarioAutorizadoServiceImpl extends DefaultModelService<UsuarioAutorizado>
        implements UsuarioAutorizadoService {

    public UsuarioAutorizadoServiceImpl(Class<UsuarioAutorizado> model, Repository<UsuarioAutorizado> repository) {
        super(model, repository);
    }

    @Override public Optional<BusinessMessages> validateInsert(UsuarioAutorizado entity);
    @Override public Optional<BusinessMessages> validateUpdate(UsuarioAutorizado entity, UsuarioAutorizado original);
    @Override public Optional<BusinessMessages> validateRemove(UsuarioAutorizado entity);

    @Override public Optional<UsuarioAutorizado> insertarDesdeImportacion(
        Centro centro, String dni, TipoUsuario tipoUsuario, Integer curso, LocalDateTime fechaExportacion);
}
```

Comentario de los métodos:
- `validateInsert` / `validateUpdate` / `validateRemove`: V-028. SIEMPRE devuelven un `BusinessMessages` con un único mensaje que transmite que los usuarios autorizados no pueden crearse, modificarse ni eliminarse manualmente. La intención es bloquear UI y API REST.
- `insertarDesdeImportacion`: construye una `UsuarioAutorizado` con los 5 campos y la guarda con `repository.save(entidad)` directamente (sin pasar por `super.insert`, que invocaría `validateInsert` y rechazaría). Si la unique-constraint V-016 saltara (red de seguridad — no debería al estar deduplicado intra-fichero por V-029), captura la excepción de persistencia y devuelve `Optional.empty()` sin propagarla.

#### 3.10 `com.educaflow.subsystem.importacion.service.TareaImportacionService` (interfaz)

```java
public interface TareaImportacionService extends ModelService<TareaImportacion> {

    /**
     * Op-4. Orquesta el flujo síncrono completo.
     * Cubre V-021..V-025 (validaciones previas), V-029/V-017 (procesado, ya delegado al importador),
     *       V-030 (persistencia fallida), V-031 (reversión por excepción), V-032 (tolerancia 0),
     *       V-033..V-037 (actualización XML), V-038/V-039/V-040 (actualización CSV).
     * Devuelve la TareaImportacion persistida (estado true o false) para que el controlador abra su detalle (A9).
     */
    TareaImportacion ejecutarImportacion(EjecutarImportacionDTO datos);

    /** V-027: SIEMPRE rechaza con mensaje de inmutabilidad. */
    @Override Optional<BusinessMessages> validateInsert(TareaImportacion entity);

    /** V-027: SIEMPRE rechaza con mensaje de inmutabilidad. */
    @Override Optional<BusinessMessages> validateUpdate(TareaImportacion entity, TareaImportacion original);

    /** V-027: SIEMPRE rechaza con mensaje de inmutabilidad. */
    @Override Optional<BusinessMessages> validateRemove(TareaImportacion entity);
}
```

#### 3.11 `com.educaflow.subsystem.importacion.service.impl.TareaImportacionServiceImpl`

```java
public class TareaImportacionServiceImpl extends DefaultModelService<TareaImportacion>
        implements TareaImportacionService {

    public TareaImportacionServiceImpl(Class<TareaImportacion> model, Repository<TareaImportacion> repository) {
        super(model, repository);
    }

    @Inject private ModelServiceFactory modelServiceFactory;
    @Inject private UsuarioAutorizadoRepository usuarioAutorizadoRepository;
    @Inject private CentroUsuarioRepository centroUsuarioRepository;
    @Inject private CentroUsuarioTipoUsuarioRepository centroUsuarioTipoUsuarioRepository;
    @Inject private TipoUsuarioRepository tipoUsuarioRepository;
    @Inject private UserRepository userRepository;

    @Override public TareaImportacion ejecutarImportacion(EjecutarImportacionDTO datos);

    @Override public Optional<BusinessMessages> validateInsert(TareaImportacion entity);
    @Override public Optional<BusinessMessages> validateUpdate(TareaImportacion entity, TareaImportacion original);
    @Override public Optional<BusinessMessages> validateRemove(TareaImportacion entity);

    private TareaImportacion fireActionRule_persistirCorrecta(
        EjecutarImportacionDTO datos, LocalDateTime fechaImportacion, User usuarioImportador,
        ResultadoImportacion resultado, String logFinal);

    private TareaImportacion fireActionRule_persistirFallida(
        EjecutarImportacionDTO datos, LocalDateTime fechaImportacion, User usuarioImportador,
        Centro centroFallback, Integer cursoFallback, LocalDateTime fechaExportacionFallback,
        String motivoLog);

    private void fireActionRule_insertarUsuariosAutorizados(
        ResultadoImportacion resultado, TipoUsuario tipoUsuario);

    private String fireActionRule_actualizarUsuariosRegistradosXML(
        Centro centro, TipoUsuario tipoBase, Integer curso, LocalDateTime fechaExportacionFichero,
        List<String> dnisFichero);

    private String fireActionRule_actualizarUsuariosRegistradosCSV(
        Centro centro, List<String> dnisValidos);

    private void fireActionRule_aplicarMutuaExclusion(
        CentroUsuario centroUsuario, TipoUsuario tipoAAniadir, TipoUsuario tipoAQuitar);

    private TipoUsuario tipoExContraparte(TipoUsuario tipoBase);

    private String composicionLogFinal(
        ResultadoImportacion resultado, int totalInsertados, String logActualizacion);
}
```

Comentarios:

- `ejecutarImportacion(datos)` — Op-4. Orquestación:
  1. `fechaImportacion = LocalDateTime.now()`.
  2. Resuelve `usuarioImportador` con `AuthUtils.getUser()` y `centroActivo = usuarioImportador.getCentroActivo()`. Si `centroActivo == null`, lanza `RuntimeException` con mensaje que transmita que el usuario no tiene centro activo (no contemplado en el análisis, pero es defensivo).
  3. Construye `ImportadorFichero importador = ImportadorFicheroFactory.create(datos.tipoUsuario(), datos.fichero(), centroActivo, fechaImportacion)`.
  4. `try { ResultadoImportacion r = importador.importar(); } catch (ImportadorException e) { ... }`:
     - En `catch`: V-021/V-022/V-023/V-024 + V-030 — invoca `fireActionRule_persistirFallida(...)` con motivo = `e.getDetalleLog()`. Devuelve la `TareaImportacion` resultante (estado=false).
  5. V-025: consulta `tareaImportacionRepository.findCorrectaByCentroTipoCursoFecha(r.centro(), datos.tipoUsuario(), r.curso(), r.fechaExportacion())`. Si encuentra coincidencia → `fireActionRule_persistirFallida(...)` con motivo que transmita los 4 valores en conflicto + V-030.
  6. Bloque crítico envuelto en try/catch:
     - `fireActionRule_insertarUsuariosAutorizados(r, datos.tipoUsuario())`.
     - Si `datos.tipoUsuario().getCodigo()` ∈ {PROFESOR, ALUMNO, FAMILIAR} → `logActualizacion = fireActionRule_actualizarUsuariosRegistradosXML(r.centro(), datos.tipoUsuario(), r.curso(), r.fechaExportacion(), r.dnisValidos())`.
     - Si `PROFESOR_EXTERNO` → `logActualizacion = fireActionRule_actualizarUsuariosRegistradosCSV(r.centro(), r.dnisValidos())`.
     - `catch (RuntimeException ex)` — V-031: la transacción principal hará rollback de los inserts y cambios; se invoca `fireActionRule_persistirFallida(...)` en transacción independiente con motivo que transmita el mensaje técnico de `ex` y la advertencia de que los cambios se han revertido. Devuelve la `TareaImportacion` fallida.
  7. V-032: si `r.dnisValidos().isEmpty()`, el bloque del paso 6 ejecuta de todas formas (insertar 0 filas + actualizar normalmente con universo posiblemente vacío); el log final incluye una línea que transmita "0 usuarios importados".
  8. `return fireActionRule_persistirCorrecta(...)` con log final compuesto.
- `validateInsert` / `validateUpdate` / `validateRemove`: V-027. SIEMPRE devuelven `BusinessMessages` con un único mensaje que transmite que las tareas de importación no pueden crearse, modificarse ni eliminarse manualmente. La intención es bloquear UI y API REST. El flujo legítimo (`fireActionRule_persistirCorrecta/Fallida`) hace bypass llamando a `repository.save` directamente.
- `fireActionRule_persistirCorrecta`: construye una `TareaImportacion` con los 10 campos y `estado=true`, y la guarda con `repository.save(entidad)` (bypass de las validaciones públicas). Cubre V-001..V-010 implícitamente (todos los campos requeridos quedan informados por construcción).
- `fireActionRule_persistirFallida`: análogo, pero con `estado=false` y `log=motivoLog`. Se ejecuta en **transacción nueva e independiente** (REQUIRES_NEW o `JPA.runInTransaction`) para garantizar que la traza sobrevive incluso si la transacción principal hace rollback en V-031. Para los campos que no se han podido extraer del fichero (cuando V-021 falla muy temprano), usa `centroFallback = centroActivo`, `cursoFallback = centroActivo.getCurso()` y `fechaExportacionFallback = fechaImportacion`.
- `fireActionRule_insertarUsuariosAutorizados`: por cada DNI en `r.dnisValidos()`, resuelve `UsuarioAutorizadoService` con `modelServiceFactory.resolve(UsuarioAutorizado.class)` y llama a `usuarioAutorizadoService.insertarDesdeImportacion(r.centro(), dni, tipoUsuario, r.curso(), r.fechaExportacion())`. Acumula el contador de insertados.
- `fireActionRule_actualizarUsuariosRegistradosXML`: aplica V-033..V-037 y V-040.
  - Resuelve `tipoEx = tipoExContraparte(tipoBase)`: PROFESOR↔EXPROFESOR, ALUMNO↔EXALUMNO, FAMILIAR↔EXFAMILIAR, vía `tipoUsuarioRepository.findByCodigo(...)`. (Asume que el repositorio de TipoUsuario tiene un finder `findByCodigo(String)`; si no existe, declarar uno como `<finder-method>` en `TipoUsuario.xml` no entra en este alcance — alternativa: usar `tipoUsuarioRepository.all().filter("self.codigo = :c")...`. Para mantener la regla "no JPQL inline en servicios", se asume que el finder existe o se añade en el dominio común; ver Notas de unificación.)
  - Construye el universo de evaluación: unión de (a) `User`s del centro cuyo DNI aparece en `dnisFichero` —localizados con un finder de `User` o vía `CentroUsuario.usuario.dni`— y (b) `User`s del centro que actualmente tienen el tipo `tipoBase` o `tipoEx` —localizados con queries via `CentroUsuarioTipoUsuario`.
  - Para cada `User u` del universo:
    - Calcula `Actual`: ¿existe una fila `UsuarioAutorizado` con `(centro, dni=u.dni, tipo=tipoBase, curso)` cuya `fechaExportacion` sea igual a la **mayor** registrada para esa terna y el curso del fichero? Tras los inserts de paso 6, la mayor incluye `fechaExportacionFichero`; por tanto `Actual = (fechaMaxima == fechaExportacionFichero)`. Usa `usuarioAutorizadoRepository.findByCentroDniTipoCursoOrderByFechaDesc(...)` y toma la primera para `fechaMaxima`.
    - Calcula `Anterior` con `usuarioAutorizadoRepository.existsAnteriorByCentroDniTipoCurso(centro, u.dni, tipoBase, curso, fechaMaxima)`.
    - Localiza el `CentroUsuario` correspondiente con `centroUsuarioRepository.findByCentroAndUsuario(centro, u)` (asume que existe el finder o se declara en el dominio `CentroUsuario`; ver Notas de unificación). V-040: si no existe `User` por ese DNI (es decir, el universo aporta DNIs que sí existen como User), no entra al bucle; los DNIs del fichero sin User registrado no producen acción.
    - Aplica la matriz:
      - **V-033 — (Actual=No, Anterior=No):** elimina `CentroUsuarioTipoUsuario` con `tipoBase` y con `tipoEx` si existen. Anota en log un texto que transmita la rama defensiva.
      - **V-034 — (Actual=No, Anterior=Sí):** `fireActionRule_aplicarMutuaExclusion(centroUsuario, tipoEx, tipoBase)` (añade EX_T, quita T).
      - **V-035 — (Actual=Sí, Anterior=No):** `fireActionRule_aplicarMutuaExclusion(centroUsuario, tipoBase, tipoEx)` (añade T, quita EX_T).
      - **V-036 — (Actual=Sí, Anterior=Sí):** igual que V-035.
  - V-037 queda garantizada por construcción: cada rama elimina la contrapartida.
  - Devuelve un texto resumen para concatenar al log final: nº de DNIs procesados, nº pasados a EX, nº reincorporados, casos defensivos.
- `fireActionRule_actualizarUsuariosRegistradosCSV`: aplica V-038, V-039 y V-040.
  - Localiza `tipoProfesorExterno = tipoUsuarioRepository.findByCodigo("PROFESOR_EXTERNO")`.
  - Para cada DNI en `dnisValidos`:
    - Busca `User` por DNI con un finder de `User` (`userRepository.findByDni(...)` o equivalente declarado en el dominio `User`; ver Notas de unificación). V-040: si no existe, continúa sin anotación.
    - Si existe `User u`: busca `CentroUsuario cu = centroUsuarioRepository.findByCentroAndUsuario(centro, u)`.
      - V-038: si `cu != null`: si no tiene `CentroUsuarioTipoUsuario(cu, tipoProfesorExterno)`, créalo. Anota en log un texto que transmita la acción.
      - V-039: si `cu == null`: crea `CentroUsuario` con `centroUsuarioRepository.save(new CentroUsuario(centro, u))` y a continuación crea `CentroUsuarioTipoUsuario` con `tipoProfesorExterno`. Anota en log.
  - Devuelve resumen.
- `fireActionRule_aplicarMutuaExclusion(centroUsuario, tipoAAniadir, tipoAQuitar)`: V-037. Añade `CentroUsuarioTipoUsuario` con `tipoAAniadir` si no existe; elimina el de `tipoAQuitar` si existe. Si por corrupción detecta ambos presentes, deja solo `tipoAAniadir`.
- `tipoExContraparte(tipoBase)`: mapeo por `codigo` de TipoUsuario (`PROFESOR`→`EXPROFESOR`, `ALUMNO`→`EXALUMNO`, `FAMILIAR`→`EXFAMILIAR`). Consulta `tipoUsuarioRepository.findByCodigo(...)`. Origen del valor: catálogo `TipoUsuario` (filas pre-cargadas en BD).
- `composicionLogFinal`: compone el texto humano del campo `log` con: resumen numérico (leídos, válidos, omitidos por formato, omitidos por duplicado intra-fichero), líneas detalladas de cada DNI omitido o duplicado (heredadas de `resultado.logParcial()`), y el log de actualización de usuarios registrados.

---

### Paso 4 — Repositorios

#### 4.1 No crear `TareaImportacionRepository.java`

El único query funcional necesario para V-025 (`findCorrectaByCentroTipoCursoFecha`) se declara como `<finder-method>` en el dominio `TareaImportacion.xml`. Axelor genera la clase abstracta y la interfaz `Repository<TareaImportacion>` recibida por el constructor del servicio expone el finder a través del repositorio concreto generado automáticamente; el cast en el servicio (`((TareaImportacionRepository) repository).findCorrectaByCentroTipoCursoFecha(...)`) usa la clase generada. No hace falta crear repositorio personalizado.

#### 4.2 Modificar `subsystem/registrousuario/db/repo/UsuarioAutorizadoRepository.java`

Eliminar todos los métodos que referencien el campo `activo`, que **no existe** en el dominio nuevo (no estaba en `UsuarioAutorizado.xml` actual, pero el repositorio Java tiene métodos legacy que sí lo usan en JPQL):

- Eliminar `findActivosByCentroAndCodigo(Long, String)`.
- Eliminar `marcarTodosInactivos(Long, Long)`.

Conservar los métodos que no dependen de `activo` y siguen siendo válidos con el esquema nuevo, en caso de que tengan consumidores externos (lo que se determina en la implementación con `Find Usages`):
- `findByCentro(Long)`, `findByCentroAndDocumento(Centro, String)`, `findByCentroAndCodigoTipoUsuario(Long, String)`, `findByCentroAndDocumentoAndTipoUsuario(Centro, String, TipoUsuario)`, `findAllByDni(String)`, `isAuthorized(String)`, `deleteByCentroAndTipoUsuario(Long, Long)`.

Si tras la limpieza alguno de los métodos conservados no tiene consumidores externos, eliminarlo también. Documentar en el código comentario que los queries propios del flujo de importación viven en `<finder-method>` del dominio (no hay JPQL inline para las reglas XML).

---

### Paso 5 — Controladores

#### 5.1 `com.educaflow.subsystem.importacion.controller.TareaImportacionController`

```java
public class TareaImportacionController {

    @Inject private ModelServiceFactory modelServiceFactory;

    @CallMethod
    @Transactional   // com.google.inject.persist.Transactional
    public void ejecutarImportacion(ActionRequest actionRequest, ActionResponse actionResponse);

    @CallMethod
    public void validateSave(ActionRequest actionRequest, ActionResponse actionResponse);

    @CallMethod
    public void validateDelete(ActionRequest actionRequest, ActionResponse actionResponse);
}
```

Comentarios:

- `ejecutarImportacion(actionRequest, actionResponse)` — Op-4 y A9:
  - Construye un `ActionRequestHelper<TareaImportacion>` con `MetaModel`.
  - Extrae del contexto el `tipoUsuario` (M2O serializado como Map con id) y el `fichero` (M2O a `MetaFile`) usando un `AllowProperties` restringido EXCLUSIVAMENTE a `tipoUsuario` y `fichero`. Resuelve las entidades reales mediante los repositorios correspondientes (no se confía en los Maps directamente para los datos persistidos).
  - Calcula `nombreFichero = fichero.getFileName()`.
  - Compone `EjecutarImportacionDTO(tipoUsuario, fichero, nombreFichero)`.
  - Resuelve `TareaImportacionService` con `modelServiceFactory.resolve(TareaImportacion.class)`.
  - Llama a `TareaImportacion tarea = service.ejecutarImportacion(dto)`.
  - Cualquier `BusinessException` previa (por ejemplo si el `centroActivo` es nulo) se traduce a `ActionResponseHelper.doResponseBusinessMessagesAsError(...)`.
  - Tras obtener la `tarea`, dispara la apertura del detalle con `actionResponse.setView(...)` apuntando a la action-view `subsysImportacion.TareaImportacion@Main-action` filtrada con `context = Map.of("_showRecord", tarea.getId())` y `viewType = "form"` (A9).
  - Adicionalmente, `actionResponse.setReload(true)` para refrescar el grid origen.
  - Errores internos no controlados (los del flujo de actualización) los traga el propio servicio dentro de V-031; el controlador no los ve.

- `validateSave(actionRequest, actionResponse)` — V-027:
  - Resuelve `TareaImportacionService`.
  - Construye `ActionRequestHelper` con `AllowProperties.createAllowAllProperties()` (solo validamos, no escribimos).
  - Si `requestHelper.getId() == null` → invoca `service.validateInsert(...)`; en caso contrario → `service.validateUpdate(entity, original)`.
  - El servicio siempre devuelve un `BusinessMessages` con el mensaje de inmutabilidad; el controlador lo traduce con `ActionResponseHelper.doResponseBusinessMessagesAsError(...)`.

- `validateDelete(actionRequest, actionResponse)` — V-027:
  - Análogo, invocando `service.validateRemove(...)`.

Parámetros: SIEMPRE `(ActionRequest actionRequest, ActionResponse actionResponse)`. Servicios SIEMPRE vía `modelServiceFactory.resolve(...)`.

#### 5.2 `com.educaflow.subsystem.registrousuario.controllers.UsuarioAutorizadoController`

```java
public class UsuarioAutorizadoController {

    @Inject private ModelServiceFactory modelServiceFactory;

    @CallMethod
    public void validateSave(ActionRequest actionRequest, ActionResponse actionResponse);

    @CallMethod
    public void validateDelete(ActionRequest actionRequest, ActionResponse actionResponse);
}
```

Comentarios:
- `validateSave`: V-028. Resuelve `UsuarioAutorizadoService` con `modelServiceFactory.resolve(UsuarioAutorizado.class)`; invoca `validateInsert` o `validateUpdate` según `getId()`; traslada el `BusinessMessages` resultante con `ActionResponseHelper.doResponseBusinessMessagesAsError(...)`.
- `validateDelete`: V-028. Análogo con `validateRemove`.

---

### Paso 6 — Vistas

> Cada `<action-view>` en su propio fichero (k-sistemas / k-vistas). Comentarios de sección con 3 líneas alineadas y comentarios de grupos de acciones (1 línea, 15 asteriscos a cada lado) en el orden obligatorio:
> 1. `Acciones de las tareas principales` (action-group de botones y eventos; primero botones, luego eventos).
> 2. `Acciones de Validaciones en local` (action-validate / action-condition).
> 3. `Acciones básicas que cambian campos simples` (action-record / action-attrs).
> 4. `Acciones de llamadas Remotas al servidor` (action-method / action-script).

#### 6.1 `subsystem/importacion/views/TareaImportacion.xml`

Cabecera de sección "TareaImportacion : Vistas" (3 líneas con asteriscos alineados al `-->`).

**Vistas declaradas:**

- **`<action-view>` `subsysImportacion.TareaImportacion@Main-action`** — abre el listado y detalle de tareas. Asocia `@Main-grid` y `@Main-form`. Sin filtro inicial (A11: admins ven todos los centros). `<view-param name="show-toolbar-form" value="false"/>` opcional.
- **`<grid>` `subsysImportacion.TareaImportacion@Main-grid`** — listado solo lectura.
  - `model="com.educaflow.subsystem.importacion.db.TareaImportacion"`, `orderBy="-fechaImportacion"`, `canNew="false"`, `canEdit="false"`, `canDelete="false"`, `canSave="false"`, `editable="false"`, `canEditOnClick="false"`, `canViewOnClick="true"`.
  - Columnas: `fechaImportacion`, `centro`, `tipoUsuario`, `nombreFichero`, `usuarioImportador`, `estado`.
  - Toolbar con un único `<button name="btnImportar" title="Importar" onClick="subsysImportacion.TareaImportacion@Main-btnImportar-action">` que dispara el action-group que abre el wizard.
- **`<form>` `subsysImportacion.TareaImportacion@Main-form`** — detalle solo lectura.
  - `model="com.educaflow.subsystem.importacion.db.TareaImportacion"`, `canNew="false"`, `canEdit="false"`, `canDelete="false"`, `canSave="false"`, `canAttach="false"`, `canMore="false"`, `readonly="true"`.
  - `onSave="subsysImportacion.TareaImportacion@Main-Remote-validateSave-action"` (blindaje API V-027).
  - `onDelete="subsysImportacion.TareaImportacion@Main-Remote-validateDelete-action"` (blindaje API V-027).
  - Panel "Datos de la importación": `fechaImportacion`, `estado`, `centro`, `tipoUsuario`, `curso`, `fechaExportacion`, `usuarioImportador` (todos `readonly="true"`).
  - Panel "Fichero original": `nombreFichero` (readonly) y `fichero` con `widget="binary-link"` y `readonly="true"` (Op-3 — el widget aporta el botón de descarga del MetaFile).
  - Panel "Log de importación" (Guía 4): un único campo `log` con `widget="Text"`, `readonly="true"`, `colSpan="12"`, altura amplia (≈500px / 20 filas). Sin popup.

**Acciones declaradas:**

Grupo 1 — *Acciones de las tareas principales*:
- `action-group` `subsysImportacion.TareaImportacion@Main-btnImportar-action`
  - Propósito: invocar la action-view del wizard.
  - Encadena un único elemento: la propia action-view `subsysImportacion.TareaImportacion@Wizard-action` (declarada en el fichero `TareaImportacion-wizard.xml`).
- `action-group` `subsysImportacion.TareaImportacion@Main-onSave-action`
  - Propósito: blindaje servidor de cualquier intento de guardado. Llama a `subsysImportacion.TareaImportacion@Main-Remote-validateSave-action`.
- `action-group` `subsysImportacion.TareaImportacion@Main-onDelete-action`
  - Propósito: blindaje servidor de cualquier intento de borrado. Llama a `subsysImportacion.TareaImportacion@Main-Remote-validateDelete-action`.

Grupo 2 — *Acciones de Validaciones en local*: (ninguna; la inmutabilidad se valida en servidor para evitar falsos positivos en cliente).

Grupo 3 — *Acciones básicas que cambian campos simples*: (ninguna).

Grupo 4 — *Acciones de llamadas Remotas al servidor*:
- `action-method` `subsysImportacion.TareaImportacion@Main-Remote-validateSave-action`
  - Propósito: invocar `TareaImportacionController.validateSave` (V-027 vía API).
  - `model="com.educaflow.subsystem.importacion.db.TareaImportacion"`. `class="com.educaflow.subsystem.importacion.controller.TareaImportacionController"`, `method="validateSave"`.
- `action-method` `subsysImportacion.TareaImportacion@Main-Remote-validateDelete-action`
  - Propósito: invocar `TareaImportacionController.validateDelete` (V-027 vía API).

#### 6.2 `subsystem/importacion/views/TareaImportacion-wizard.xml`

Cabecera de sección "TareaImportacion (Wizard) : Vistas".

**Vistas declaradas:**

- **`<action-view>` `subsysImportacion.TareaImportacion@Wizard-action`** — abre el asistente como popup modal.
  - `model="com.educaflow.subsystem.importacion.db.TareaImportacion"`, `title="Nueva importación"`.
  - `<view type="form" name="subsysImportacion.TareaImportacion@Wizard-form"/>`.
  - `<view-param name="popup" value="true"/>` y `<view-param name="popup-save" value="false"/>` para que el cierre lo gestione el propio action-group del botón Importar.
- **`<form>` `subsysImportacion.TareaImportacion@Wizard-form`** — formulario modal del asistente.
  - `model="com.educaflow.subsystem.importacion.db.TareaImportacion"`. `canBack="false"`, `canDelete="false"`, `canNew="false"`, `canAttach="false"`, `canMore="false"`.
  - Panel "Asistente":
    - `tipoUsuario` — many-to-one a `TipoUsuario`, `widget="SwitchSelect"`, `required="true"`, `domain="self.codigo IN ('PROFESOR','ALUMNO','FAMILIAR','PROFESOR_EXTERNO')"` (cubre V-019: dominio finito de los 4 valores; **Guía 6 anulada** por decisión del usuario — se permiten los 4 tipos).
    - `fichero` — many-to-one a `MetaFile`, `widget="binary-link"`, `required="true"`.
  - Panel de botones (sin frame):
    - Botón `btnCancelar` `onClick="subsysImportacion.TareaImportacion@Wizard-btnCancelar-action"`.
    - Botón `btnImportar` `onClick="subsysImportacion.TareaImportacion@Wizard-btnImportar-action"`.

**Acciones declaradas:**

Grupo 1 — *Acciones de las tareas principales*:
- `action-group` `subsysImportacion.TareaImportacion@Wizard-btnImportar-action`
  - Propósito: encadenar la validación local del wizard con la ejecución remota.
  - Secuencia:
    1. `subsysImportacion.TareaImportacion@Wizard-Local-validate-action` (V-018, V-019, V-020 en cliente).
    2. `subsysImportacion.TareaImportacion@Wizard-Remote-ejecutar-action` (delega en el controlador, que tras procesar abre el detalle con `setView`).
    3. `close` (cierra el popup; la apertura del detalle ya la ha hecho el controlador con `setView`).
- `action-group` `subsysImportacion.TareaImportacion@Wizard-btnCancelar-action`
  - Propósito: cerrar el popup sin acción.
  - Acción: `close`.

Grupo 2 — *Acciones de Validaciones en local*:
- `action-condition` `subsysImportacion.TareaImportacion@Wizard-Local-validate-action`
  - Propósito: V-018 y V-020 (campos obligatorios). V-019 ya está implícita en el SwitchSelect.
  - Checks: `<check field="tipoUsuario"/>` (mensaje que transmita que el tipo es obligatorio); `<check field="fichero"/>` (mensaje que transmita que el fichero es obligatorio).

Grupo 3 — *Acciones básicas que cambian campos simples*: (ninguna).

Grupo 4 — *Acciones de llamadas Remotas al servidor*:
- `action-method` `subsysImportacion.TareaImportacion@Wizard-Remote-ejecutar-action`
  - Propósito: invocar `TareaImportacionController.ejecutarImportacion`. El controlador procesa el flujo síncrono completo (Op-4) y devuelve un `setView` que abre el detalle de la `TareaImportacion` creada (A9).
  - `model="com.educaflow.subsystem.importacion.db.TareaImportacion"`. `class="com.educaflow.subsystem.importacion.controller.TareaImportacionController"`, `method="ejecutarImportacion"`. Campos del context implicados: `tipoUsuario`, `fichero`.

#### 6.3 `subsystem/importacion/views/UsuarioAutorizado.xml`

Cabecera de sección "UsuarioAutorizado : Vistas".

> **Decisión de ubicación:** las vistas viven en `subsystem/importacion/views/` (no en `subsystem/registrousuario/views/`) porque el subsistema `importacion` es quien las muestra y porque el namespace usa `subsysImportacion`. La entidad y su controlador siguen viviendo en `subsystem/registrousuario` (origen del modelo).

**Vistas declaradas:**

- **`<action-view>` `subsysImportacion.UsuarioAutorizado@Main-action`** — abre el listado de usuarios autorizados (Op-5).
  - `model="com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado"`.
  - Asocia `@Main-grid` y `@Main-form`.
  - Sin filtro inicial (A11).
- **`<grid>` `subsysImportacion.UsuarioAutorizado@Main-grid`** — listado solo lectura.
  - `canNew="false"`, `canEdit="false"`, `canDelete="false"`, `canSave="false"`, `editable="false"`, `canViewOnClick="true"`, `orderBy="centro,dni"`.
  - Columnas: `centro`, `dni`, `tipoUsuario`, `curso`, `fechaExportacion`.
- **`<form>` `subsysImportacion.UsuarioAutorizado@Main-form`** — detalle solo lectura.
  - `canNew="false"`, `canEdit="false"`, `canDelete="false"`, `canSave="false"`, `canAttach="false"`, `canMore="false"`, `readonly="true"`.
  - `onSave="subsysImportacion.UsuarioAutorizado@Main-Remote-validateSave-action"` (V-028).
  - `onDelete="subsysImportacion.UsuarioAutorizado@Main-Remote-validateDelete-action"` (V-028).
  - Panel "Datos del usuario autorizado": los 5 campos en `readonly="true"`.

**Acciones declaradas:**

Grupo 1 — *Acciones de las tareas principales*:
- `action-group` `subsysImportacion.UsuarioAutorizado@Main-onSave-action`
  - Propósito: blindaje servidor V-028. Llama a `subsysImportacion.UsuarioAutorizado@Main-Remote-validateSave-action`.
- `action-group` `subsysImportacion.UsuarioAutorizado@Main-onDelete-action`
  - Propósito: blindaje servidor V-028. Llama a `subsysImportacion.UsuarioAutorizado@Main-Remote-validateDelete-action`.

Grupo 2 — *Acciones de Validaciones en local*: (ninguna).

Grupo 3 — *Acciones básicas que cambian campos simples*: (ninguna).

Grupo 4 — *Acciones de llamadas Remotas al servidor*:
- `action-method` `subsysImportacion.UsuarioAutorizado@Main-Remote-validateSave-action`
  - Propósito: invocar `UsuarioAutorizadoController.validateSave` (V-028 vía API).
  - `model="com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado"`. `class="com.educaflow.subsystem.registrousuario.controllers.UsuarioAutorizadoController"`, `method="validateSave"`.
- `action-method` `subsysImportacion.UsuarioAutorizado@Main-Remote-validateDelete-action`
  - Propósito: invocar `UsuarioAutorizadoController.validateDelete` (V-028 vía API).

#### 6.4 Modificar `secretariavirtual/menus/menus.xml`

El menuitem `administracionSv-importacion-menuitem` **ya existe** (`parent="administracionSv-menuitem"`, `groups="admins"`, `action="subsysImportacion.TareaImportacion@Main-action"`, `order="2"`). No se toca.

**Añadir** un menuitem nuevo bajo `administracionSv-menuitem`:

- `name="administracionSv-usuariosAutorizados-menuitem"`.
- `parent="administracionSv-menuitem"`.
- `title="Usuarios autorizados"`.
- `action="subsysImportacion.UsuarioAutorizado@Main-action"`.
- `groups="admins"`.
- `order` siguiente al de `administracionSv-importacion-menuitem` (proponer `order="3"` y reordenar `administracionSv-usuarios-menuitem` a 4 si entra en conflicto).

---

### Paso 7 — Seguridad

#### 7.1 Crear `src/main/resources/data-init/input/auth-importacion.xml`

Permisos a declarar (sin condition — A11: admins ven todos los centros):

- `TareaImportacion.admins` con `object="com.educaflow.subsystem.importacion.db.TareaImportacion"` y `<can create="false" read="true" write="false" remove="false" export="false"/>`.
- `UsuarioAutorizado.admins` con `object="com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado"` y `<can create="false" read="true" write="false" remove="false" export="false"/>`.

Estructura del fichero:
- `<auth>` raíz con los dos `<permission>` anteriores.
- Comentarios que indiquen V-026 (autorización) y V-027/V-028 (refuerzo de inmutabilidad: `create=false`/`write=false`/`remove=false` para la API REST/UI; el servicio escribe con bypass al hacer `repository.save` directamente desde el flujo de importación).

#### 7.2 Modificar `src/main/resources/data-init/input-config.xml`

Añadir un nuevo `<input file="auth-importacion.xml" root="auth">` con el mismo binding que el resto de `auth-*.xml` (mapear `<permission>` a `com.axelor.auth.db.Permission`), **antes** del bloque que carga `auth.xml` (que contiene roles/grupos).

#### 7.3 Modificar `src/main/resources/data-init/input/auth.xml`

- En el grupo `admins`:
  - **Eliminar** la línea `<permission name="UsuarioAutorizado.all"/>` (queda sustituida por `UsuarioAutorizado.admins`).
  - **Añadir** `<permission name="TareaImportacion.admins"/>`.
  - **Añadir** `<permission name="UsuarioAutorizado.admins"/>`.
- En el grupo `users`:
  - **Eliminar** la línea `<permission name="UsuarioAutorizado.all"/>` (V-026: el grupo `users` no accede a este subsistema).
- No tocar `center-admins` (este subsistema no le concede acceso; A8 y guía 8: la gestión por centro queda fuera de alcance).
- Si la declaración global del permiso `UsuarioAutorizado.all` queda huérfana (sin consumidores tras eliminarla de los grupos), retirarla también de su `auth-*.xml` original.

---

### Paso 8 — Datos iniciales

No aplica. Los `TipoUsuario` con códigos `PROFESOR`, `ALUMNO`, `FAMILIAR`, `PROFESOR_EXTERNO`, `EXPROFESOR`, `EXALUMNO`, `EXFAMILIAR` ya están definidos como filas en BD por el seed del subsistema `common`. No se crean tampoco filas iniciales de `TareaImportacion` ni `UsuarioAutorizado`.

---

### Paso 9 — Verificación final

Comando de compilación:

```
./gradlew clean build --info
```

Comprobaciones de coherencia:

```
grep -n "self.activo" src/main/java/com/educaflow/subsystem/registrousuario/db/repo/UsuarioAutorizadoRepository.java       # debe devolver 0 resultados
grep -n "administracionSv-usuariosAutorizados-menuitem" src/main/java/com/educaflow/secretariavirtual/menus/menus.xml      # debe encontrar el nuevo menuitem
grep -rn "auth-importacion.xml" src/main/resources/data-init/                                                              # debe encontrarlo registrado en input-config.xml
grep -n "UsuarioAutorizado.all" src/main/resources/data-init/input/auth.xml                                                # debe devolver 0 resultados
grep -rn "subsysImportacion.TareaImportacion@Main-action" src/main/java/com/educaflow/                                     # debe aparecer en menus.xml y en TareaImportacion.xml
grep -rn "subsysImportacion.UsuarioAutorizado@Main-action" src/main/java/com/educaflow/                                    # debe aparecer en menus.xml y en UsuarioAutorizado.xml
```

Comprobaciones funcionales (manuales tras arrancar la app):
- El menú "Administración SV → Ficheros importación" abre el listado de tareas.
- El menú "Administración SV → Usuarios autorizados" abre el listado de usuarios autorizados.
- El botón "Importar" del listado abre el wizard.
- Con `tipoUsuario` o `fichero` vacíos, el botón "Importar" del wizard muestra el error de campo obligatorio y no llama al servidor.
- Subiendo un XML válido con el tipo correcto se crea una `TareaImportacion` correcta y se abre su detalle automáticamente.
- Subiendo un XML inválido (XSD), o con `<centro codigo>` distinto al centro activo, se crea una `TareaImportacion` fallida y se abre su detalle con el motivo en el log.
- Subiendo un CSV con cabecera no-DNI se procesa correctamente descartando la cabecera.
- Un usuario no admin no ve los dos menús ni puede llamar a las action-view por URL directa.

---

## Matriz de trazabilidad V-XXX → ubicación

| Regla | Capa | Ubicación (clase.método o fichero+acción) | Comentario |
|-------|------|-------------------------------------------|-----------|
| V-001 | Modelo | `TareaImportacion.xml` → `<datetime name="fechaImportacion" required="true"/>` | Required del dominio. |
| V-002 | Modelo | `TareaImportacion.xml` → `<many-to-one name="centro" required="true"/>` | Required del dominio. |
| V-003 | Modelo | `TareaImportacion.xml` → `<many-to-one name="tipoUsuario" required="true"/>` | Required del dominio. |
| V-004 | Modelo | `TareaImportacion.xml` → `<string name="nombreFichero" required="true"/>` | Required del dominio. |
| V-005 | Modelo | `TareaImportacion.xml` → `<many-to-one name="fichero" required="true"/>` | Required del dominio. |
| V-006 | Modelo | `TareaImportacion.xml` → `<many-to-one name="usuarioImportador" required="true"/>` | Required del dominio. |
| V-007 | Modelo | `TareaImportacion.xml` → `<integer name="curso" required="true"/>` | Required del dominio. |
| V-008 | Modelo | `TareaImportacion.xml` → `<datetime name="fechaExportacion" required="true"/>` | Required del dominio. |
| V-009 | Modelo | `TareaImportacion.xml` → `<boolean name="estado" required="true"/>` | Required del dominio. |
| V-010 | Modelo | `TareaImportacion.xml` → `<string name="log" required="true" large="true" multiline="true"/>` | Required del dominio. |
| V-011 | Modelo | `UsuarioAutorizado.xml` → `<many-to-one name="centro" required="true"/>` | Required del dominio. |
| V-012 | Modelo | `UsuarioAutorizado.xml` → `<string name="dni" required="true"/>` | Required del dominio. |
| V-013 | Modelo | `UsuarioAutorizado.xml` → `<many-to-one name="tipoUsuario" required="true"/>` | Required del dominio. |
| V-014 | Modelo | `UsuarioAutorizado.xml` → `<integer name="curso" required="true"/>` | Required del dominio. |
| V-015 | Modelo | `UsuarioAutorizado.xml` → `<datetime name="fechaExportacion" required="true"/>` | Required del dominio. |
| V-016 | Modelo + Servidor | `UsuarioAutorizado.xml` → `<unique-constraint columns="centro,dni,tipoUsuario,curso,fechaExportacion"/>`; `UsuarioAutorizadoServiceImpl.insertarDesdeImportacion` (captura constraint como red de seguridad y devuelve `Optional.empty()`). | El importador ya deduplica intra-fichero por V-029. |
| V-017 | Servidor | `ImportadorUsuarioXML.importar()` y `ImportadorUsuarioCSV.importar()` | Filtrado con `DniUtil.isValid`, anotación en `logParcial` del `ResultadoImportacion`. |
| V-018 | Cliente | `TareaImportacion-wizard.xml` → `subsysImportacion.TareaImportacion@Wizard-Local-validate-action` (`<check field="tipoUsuario"/>`) | También required en el form. |
| V-019 | Cliente | `TareaImportacion-wizard.xml` → form `subsysImportacion.TareaImportacion@Wizard-form` campo `tipoUsuario` con `widget="SwitchSelect"` y `domain="self.codigo IN ('PROFESOR','ALUMNO','FAMILIAR','PROFESOR_EXTERNO')"`. Refuerzo en `ImportadorFicheroFactory.create` (rama defensiva `IllegalArgumentException`). | Dominio finito visible al usuario. |
| V-020 | Cliente | `TareaImportacion-wizard.xml` → `subsysImportacion.TareaImportacion@Wizard-Local-validate-action` (`<check field="fichero"/>`) | También required en el form. |
| V-021 | Servidor | `ImportadorUsuarioXML.importar()` invoca `XMLUtil.validarConSchema` con el XSD correspondiente; lanza `ImportadorException`. → `TareaImportacionServiceImpl.ejecutarImportacion` (catch) → `fireActionRule_persistirFallida` (V-030). | Mensaje del validador propagado al log (A15). |
| V-022 | Servidor | `ImportadorUsuarioCSV.importar()` valida decodificación textual; lanza `ImportadorException` → `fireActionRule_persistirFallida` (V-030). | |
| V-023 | Servidor | `ImportadorUsuarioXML.importar()` — la elección del XSD según `tipoUsuario.codigo` hace que un XML del tipo incorrecto falle V-021 con un mensaje que transmita la incongruencia. | A15 cubre el caso. |
| V-024 | Servidor | `ImportadorUsuarioXML.importar()` — comparación de atributo `codigo` del `<centro>` con `centroActivo.code`; lanza `ImportadorException` con texto que transmita ambos códigos. → `fireActionRule_persistirFallida` (V-030). | |
| V-025 | Servidor | `TareaImportacionServiceImpl.ejecutarImportacion` invoca `tareaImportacionRepository.findCorrectaByCentroTipoCursoFecha(...)` (finder declarado en `TareaImportacion.xml`); si encuentra coincidencia → `fireActionRule_persistirFallida` (V-030) con mensaje que transmita los 4 valores en conflicto. | |
| V-026 | Seguridad | `auth-importacion.xml` (`TareaImportacion.admins`, `UsuarioAutorizado.admins`) + `auth.xml` (asignación al grupo `admins`) + `menus.xml` (`groups="admins"`). | |
| V-027 | Servidor + Seguridad + Cliente | `TareaImportacionServiceImpl.validateInsert/Update/Remove`; `TareaImportacionController.validateSave/validateDelete`; vista Main `canNew/canEdit/canDelete=false`; `auth-importacion.xml` con `create=false write=false remove=false`. Acciones `subsysImportacion.TareaImportacion@Main-Remote-validateSave-action` y `@Main-Remote-validateDelete-action` enganchadas en `onSave`/`onDelete` (blindaje API). | Triple barrera. |
| V-028 | Servidor + Seguridad + Cliente | `UsuarioAutorizadoServiceImpl.validateInsert/Update/Remove`; `UsuarioAutorizadoController.validateSave/validateDelete`; vista Main `canNew/canEdit/canDelete=false`; `auth-importacion.xml` con `create=false write=false remove=false`. Acciones `subsysImportacion.UsuarioAutorizado@Main-Remote-validateSave-action` y `@Main-Remote-validateDelete-action` enganchadas en `onSave`/`onDelete` (blindaje API). | Triple barrera. |
| V-029 | Servidor | `ImportadorUsuarioXML.importar()` e `ImportadorUsuarioCSV.importar()` — deduplicación intra-fichero con `LinkedHashSet`, anotación en `logParcial`. | |
| V-030 | Servidor | `TareaImportacionServiceImpl.fireActionRule_persistirFallida` (invocado tanto desde el catch de `ImportadorException` como tras V-025 ó V-031) y `fireActionRule_persistirCorrecta`. | Persiste la `TareaImportacion` con `estado` y log final. |
| V-031 | Servidor | `TareaImportacionServiceImpl.ejecutarImportacion` (try/catch que envuelve el bloque insert + actualización) + `fireActionRule_persistirFallida` en transacción **REQUIRES_NEW** (o `JPA.runInTransaction`) para que la traza sobreviva al rollback. | |
| V-032 | Servidor | `TareaImportacionServiceImpl.ejecutarImportacion` no bifurca por `dnisValidos.isEmpty()`; el bloque normal se ejecuta con universo vacío y `composicionLogFinal` indica "0 usuarios importados". | |
| V-033 | Servidor | `TareaImportacionServiceImpl.fireActionRule_actualizarUsuariosRegistradosXML` rama (No, No) — elimina T y EX_T del CentroUsuario (caso defensivo). | |
| V-034 | Servidor | `…fireActionRule_actualizarUsuariosRegistradosXML` rama (No, Sí) → `fireActionRule_aplicarMutuaExclusion(cu, EX_T, T)`. | |
| V-035 | Servidor | `…fireActionRule_actualizarUsuariosRegistradosXML` rama (Sí, No) → `fireActionRule_aplicarMutuaExclusion(cu, T, EX_T)`. | |
| V-036 | Servidor | `…fireActionRule_actualizarUsuariosRegistradosXML` rama (Sí, Sí) → `fireActionRule_aplicarMutuaExclusion(cu, T, EX_T)`. | |
| V-037 | Servidor | `TareaImportacionServiceImpl.fireActionRule_aplicarMutuaExclusion` — garantizado por construcción: toda rama añade un tipo y elimina la contrapartida. | |
| V-038 | Servidor | `TareaImportacionServiceImpl.fireActionRule_actualizarUsuariosRegistradosCSV` rama "tiene CentroUsuario" — añade `PROFESOR_EXTERNO` si no lo tenía. | |
| V-039 | Servidor | `TareaImportacionServiceImpl.fireActionRule_actualizarUsuariosRegistradosCSV` rama "no tiene CentroUsuario" — crea CentroUsuario + añade `PROFESOR_EXTERNO`. | |
| V-040 | Servidor | `TareaImportacionServiceImpl.fireActionRule_actualizarUsuariosRegistradosXML` y `…CSV` — DNI sin `User` registrado se omite (silencioso, A8). | |

### Reglas de negocio (operaciones, transiciones, campos calculados, efectos secundarios)

| Regla | Ubicación |
|-------|-----------|
| Op-1 (listar tareas, todos los centros, orden desc) | `TareaImportacion.xml` (`@Main-action`, `@Main-grid` con `orderBy="-fechaImportacion"`) + `menus.xml` `administracionSv-importacion-menuitem` |
| Op-2 (ver detalle solo lectura, log completo) | `TareaImportacion.xml` (`@Main-form` readonly, panel "Log" widget Text alto, Guía 4) |
| Op-3 (descargar fichero original) | `TareaImportacion.xml` (`@Main-form` panel "Fichero original" con `fichero` widget `binary-link`) |
| Op-4 (asistente + flujo síncrono completo + apertura del detalle) | `TareaImportacion-wizard.xml` (form + acciones del asistente) + `TareaImportacionController.ejecutarImportacion` + `TareaImportacionServiceImpl.ejecutarImportacion` |
| Op-5 (listar usuarios autorizados, todos los centros) | `UsuarioAutorizado.xml` (`@Main-action`, `@Main-grid`) + `menus.xml` `administracionSv-usuariosAutorizados-menuitem` |
| Campo calculado `fechaImportacion` | `TareaImportacionServiceImpl.ejecutarImportacion` (`LocalDateTime.now()` al inicio) |
| Campo calculado `usuarioImportador` | `TareaImportacionServiceImpl.ejecutarImportacion` (`AuthUtils.getUser()`) |
| Campo calculado `centro` (XML/CSV) | `ImportadorUsuarioXML.importar()` (`centroActivo` validado contra atributo `codigo`) / `ImportadorUsuarioCSV.importar()` (`centroActivo` directo) |
| Campo calculado `curso` (XML/CSV) | `ImportadorUsuarioXML.importar()` (atributo `curso` del XML) / `ImportadorUsuarioCSV.importar()` (`centroActivo.getCurso()`) |
| Campo calculado `fechaExportacion` (XML/CSV) | `ImportadorUsuarioXML.importar()` (atributo `fechaExportacion` del XML, patrón `dd/MM/yyyy HH:mm:ss`) / `ImportadorUsuarioCSV.importar()` (`instanteImportacion`) |
| Campo calculado `estado` | `TareaImportacionServiceImpl.fireActionRule_persistirCorrecta` / `…persistirFallida` |
| Campo calculado `log` (composición) | `TareaImportacionServiceImpl.composicionLogFinal` |
| Mapeo PROFESOR↔EXPROFESOR, ALUMNO↔EXALUMNO, FAMILIAR↔EXFAMILIAR | `TareaImportacionServiceImpl.tipoExContraparte` (consulta `TipoUsuario` por código) |
| A9 — apertura automática del detalle tras importar | `TareaImportacionController.ejecutarImportacion` (`actionResponse.setView(...)` apuntando a `@Main-action` con `_showRecord` = id de la tarea creada) |
| A11 — admins ven todos los centros | Permisos `TareaImportacion.admins` y `UsuarioAutorizado.admins` sin `condition`; vistas sin domain inicial |
| A12 — "última fechaExportacion" para Actual/Anterior | `UsuarioAutorizado.xml` finder `findByCentroDniTipoCursoOrderByFechaDesc` (usado tras los inserts del lote) |
| A13 — cabecera CSV silenciosa | `ImportadorUsuarioCSV.importar()` |
| A14 — curso usado en las reglas viene del fichero (XML) o de `Centro.curso` activo (CSV) | `ResultadoImportacion.curso` se propaga desde el importador a la actualización de registrados |
| A15 — mensaje del validador XSD íntegro en el log | `ImportadorUsuarioXML.importar()` propaga el texto retornado por `XMLUtil.validarConSchema` como `ImportadorException.detalleLog` |

---

## Notas de unificación

1. **Guía 6 anulada por decisión del usuario.** El `SwitchSelect` del asistente incluye los 4 tipos (PROFESOR, ALUMNO, FAMILIAR, PROFESOR_EXTERNO). La selección de PROFESOR_EXTERNO activa la rama CSV en `ImportadorFicheroFactory`.

2. **Bypass de inmutabilidad para el flujo legítimo.** `validateInsert/Update/Remove` de los dos servicios (`TareaImportacionService`, `UsuarioAutorizadoService`) SIEMPRE rechazan. El flujo legítimo (los métodos `fireActionRule_persistir*` y `insertarDesdeImportacion`) usa `repository.save(entidad)` directamente, sin pasar por `super.insert`/`super.update`, evitando así el rechazo. Esto preserva la regla "validateInsert/Update/Remove devuelven Optional<BusinessMessages>" sin sacrificar la operatividad interna.

3. **V-031 — transacción REQUIRES_NEW para la traza fallida.** La persistencia de la `TareaImportacion` con `estado=false` debe sobrevivir al rollback de los inserts en `UsuarioAutorizado` y de los cambios en `CentroUsuario`/`CentroUsuarioTipoUsuario`. La implementación lo logra con `@Transactional(REQUIRES_NEW)` (Google Guice Persist) o con `JPA.runInTransaction(...)` desde un bloque interno aislado. El implementador decidirá el mecanismo concreto siempre que cumpla el requisito.

4. **Finders adicionales que pueden requerir declaración en sus dominios.** El servicio `TareaImportacionServiceImpl` necesita acceder por código a `TipoUsuario` y por DNI a `User`, y por (centro, usuario) a `CentroUsuario`. El diseño da por hecho que existen los finders `tipoUsuarioRepository.findByCodigo(String)`, `userRepository.findByDni(String)` y `centroUsuarioRepository.findByCentroAndUsuario(Centro, User)`. Si alguno NO existe, declararlo como `<finder-method>` en el dominio correspondiente del subsistema `common` o `auth.User` durante la implementación (decisión del implementador, sin JPQL inline en el servicio).

5. **Limpieza del repositorio Java legacy.** `UsuarioAutorizadoRepository.java` tiene métodos que referencian un campo `activo` que ya no existe en el dominio (vestigio de una iteración anterior). El paso 4.2 los elimina (`findActivosByCentroAndCodigo`, `marcarTodosInactivos`). El resto de finders del repositorio se mantienen si tienen consumidores externos vigentes, o se eliminan si quedan huérfanos; el implementador hará `Find Usages` durante la implementación.

6. **Vistas de UsuarioAutorizado bajo el subsistema importacion.** Aunque la entidad `UsuarioAutorizado` reside en `subsystem/registrousuario`, su listado lo muestra el subsistema `importacion`. Por coherencia funcional con el menú y el namespace de vista (`subsysImportacion.UsuarioAutorizado@Main-*`), los XML de vista viven en `subsystem/importacion/views/UsuarioAutorizado.xml`. El controlador `UsuarioAutorizadoController` y el servicio sí viven en `subsystem/registrousuario` porque son intrínsecos al modelo.

7. **Botón "Importar" del grid.** Se modela como un `<button>` en el `toolbar` del grid `@Main-grid` cuyo `onClick` dispara el `action-group` `subsysImportacion.TareaImportacion@Main-btnImportar-action`, que a su vez invoca la `action-view` `subsysImportacion.TareaImportacion@Wizard-action` declarada en el fichero del wizard.

8. **Apertura automática del detalle tras importar (A9).** La hace el controlador después de recibir la `TareaImportacion` del servicio, mediante `actionResponse.setView(...)` apuntando a `subsysImportacion.TareaImportacion@Main-action` con `context = { "_showRecord": tarea.getId() }` y `viewType = "form"`. El `close` final del `action-group` del botón Importar del wizard cierra el popup tras la apertura del detalle.

9. **Sin módulos Guice para `ModelService`.** `TareaImportacionService` y `UsuarioAutorizadoService` los descubre `ModelServiceFactory` por convención (paquete `service.impl`, sufijo `ServiceImpl`).

10. **Sin listeners JPA.** Toda la lógica de sincronización con usuarios registrados vive en los `fireActionRule_*` del servicio de importación.
