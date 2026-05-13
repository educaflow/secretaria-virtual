---
type: design
---

# Diseño: Subsistema de importación de usuarios autorizados

**Objetivo:** Crear el subsistema `importacion` que permite a los administradores importar masivamente DNIs de usuarios autorizados (XML para PROFESOR/ALUMNO/FAMILIAR; CSV para PROFESOR_EXTERNO), registrar cada operación como `TareaImportacion` inmutable con su log y actualizar los tipos de usuario sobre los usuarios ya registrados del centro.
**Capa:** subsystem/importacion
**Análisis de origen:** `.sdd/drafts/2026-05-11_15-30_susbsitema-importacion/analysis_03/analysis.md`
**Skills necesarios para la implementación:** k-sistemas, k-vistas, k-seguridad

---

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---|---|---|---|
| `src/main/resources/data-import/schemas/profesores.xsd` | Reutilizar | k-sistemas | XSD existente, sin cambios. |
| `src/main/resources/data-import/schemas/alumnos.xsd` | Reutilizar | k-sistemas | XSD existente, sin cambios. |
| `src/main/resources/data-import/schemas/familiares.xsd` | Reutilizar | k-sistemas | XSD existente, sin cambios. |
| `src/main/java/com/educaflow/subsystem/importacion/domains/TareaImportacion.xml` | Crear | k-sistemas | Dominio `TareaImportacion` + enum `TipoFicheroImportacion` + finder de unicidad. |
| `src/main/java/com/educaflow/subsystem/registrousuario/domains/UsuarioAutorizado.xml` | Modificar | k-sistemas | `curso` pasa a required; `fechaExportacion` de date a datetime required; reemplazar unique-constraint; añadir finders. |
| `src/main/java/com/educaflow/subsystem/importacion/service/TareaImportacionService.java` | Crear | k-sistemas | Interfaz `ModelService<TareaImportacion>` + orquestación de Op-4. |
| `src/main/java/com/educaflow/subsystem/importacion/service/impl/TareaImportacionServiceImpl.java` | Crear | k-sistemas | Implementación de la orquestación y validaciones de inmutabilidad. |
| `src/main/java/com/educaflow/subsystem/importacion/service/dto/TareaImportacionInsertDTO.java` | Crear | k-sistemas | Record DTO con `tipoFichero` y `fichero` que recibe `procesarImportacion`. |
| `src/main/java/com/educaflow/subsystem/importacion/service/dto/ResultadoImportacion.java` | Crear | k-sistemas | Record DTO devuelto por `ImportadorFichero` (centroCodigo, curso, fechaExportacion, dnis válidos/inválidos/duplicados, logParcial). |
| `src/main/java/com/educaflow/subsystem/importacion/service/ImportadorFichero.java` | Crear | k-sistemas | Interfaz colaboradora con `importar()`. |
| `src/main/java/com/educaflow/subsystem/importacion/service/ImportadorFicheroFactory.java` | Crear | k-sistemas | Factoría que resuelve la implementación adecuada según `TipoFicheroImportacion`. |
| `src/main/java/com/educaflow/subsystem/importacion/service/ImportadorException.java` | Crear | k-sistemas | Excepción funcional de fallo de formato/coherencia/centro. |
| `src/main/java/com/educaflow/subsystem/importacion/service/impl/ImportadorUsuarioXML.java` | Crear | k-sistemas | Implementación XML (PROFESOR_XML / ALUMNO_XML / FAMILIAR_XML). |
| `src/main/java/com/educaflow/subsystem/importacion/service/impl/ImportadorUsuarioCSV.java` | Crear | k-sistemas | Implementación CSV (PROFESOR_EXTERNO_CSV). |
| `src/main/java/com/educaflow/subsystem/importacion/service/ActualizadorTiposUsuarioRegistrados.java` | Crear | k-sistemas | Clase colaboradora con la lógica de actualización XML (tabla 2x2) y CSV (PROFESOR_EXTERNO). |
| `src/main/java/com/educaflow/subsystem/registrousuario/service/UsuarioAutorizadoService.java` | Crear | k-sistemas | Interfaz `ModelService<UsuarioAutorizado>` con inserción para importación, finders y validateXxx para inmutabilidad. |
| `src/main/java/com/educaflow/subsystem/registrousuario/service/impl/UsuarioAutorizadoServiceImpl.java` | Crear | k-sistemas | Implementación de los métodos anteriores. |
| `src/main/java/com/educaflow/subsystem/common/service/CentroService.java` | Crear | k-sistemas | Servicio `ModelService<Centro>` con `findByCodigo(String)`. |
| `src/main/java/com/educaflow/subsystem/common/service/impl/CentroServiceImpl.java` | Crear | k-sistemas | Implementación que delega en `CentroRepository.findByCodigo(...)` (ya existe en `db/repo`). |
| `src/main/java/com/educaflow/subsystem/common/service/CentroUsuarioService.java` | Crear | k-sistemas | Servicio `ModelService<CentroUsuario>` con operaciones de alta de CentroUsuario y gestión de tipos (añadir/quitar tipo, idempotente). |
| `src/main/java/com/educaflow/subsystem/common/service/impl/CentroUsuarioServiceImpl.java` | Crear | k-sistemas | Implementación de los métodos anteriores. |
| `src/main/java/com/educaflow/subsystem/importacion/controller/TareaImportacionController.java` | Crear | k-sistemas | Controller con `importar(actionRequest, actionResponse)` (botón Importar del wizard). |
| `src/main/java/com/educaflow/subsystem/importacion/views/TareaImportacion.xml` | Crear | k-vistas | `<action-view>` `@Main-action` + grid + form único wizard/detalle + acciones. |
| `src/main/java/com/educaflow/subsystem/importacion/views/UsuarioAutorizado.xml` | Crear | k-vistas | `<action-view>` `@Main-action` + grid solo lectura del listado de usuarios autorizados. |
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas | Añadir `administracionSv-usuariosAutorizados-menuitem`. |

> Nota: el menuitem `administracionSv-importacion-menuitem` (action `subsysImportacion.TareaImportacion@Main-action`, `groups="admins"`) ya existe en `menus.xml` y no se modifica.

> Nota: los ficheros `i18n_es.csv` / `i18n_ca.csv` de las nuevas carpetas se generan automáticamente por el build — **no se crean a mano**.

---

## Pasos

### Paso 1 — Recursos / ficheros estáticos

Los XSDs ya existen en `src/main/resources/data-import/schemas/`:

- `profesores.xsd` — raíz `<centro codigo curso fechaExportacion>` con sección `<docentes>` y al menos un `<docente documento="...">`.
- `alumnos.xsd` — raíz `<centro codigo curso fechaExportacion>` con sección `<alumnos>` y al menos un `<alumno documento="...">`.
- `familiares.xsd` — raíz `<centro codigo curso fechaExportacion>` con sección `<familiares>` y al menos un `<familiar documento="...">`.

Se cargan desde el classpath con `getClass().getResourceAsStream("/data-import/schemas/<nombre>.xsd")` desde `ImportadorUsuarioXML`. No se modifican.

---

### Paso 2 — Dominios

#### 2.1 Crear `subsystem/importacion/domains/TareaImportacion.xml`

Contiene el enum `TipoFicheroImportacion` y la entidad `TareaImportacion`.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<domain-models xmlns="http://axelor.com/xml/ns/domain-models"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://axelor.com/xml/ns/domain-models https://axelor.com/xml/ns/domain-models/domain-models_8.1.xsd">

    <module name="importacion" package="com.educaflow.subsystem.importacion.db"/>

    <enum name="TipoFicheroImportacion">
        <item name="PROFESOR_XML"         title="Profesores (XML)"/>
        <item name="ALUMNO_XML"           title="Alumnos (XML)"/>
        <item name="FAMILIAR_XML"         title="Familiares (XML)"/>
        <item name="PROFESOR_EXTERNO_CSV" title="Profesor externo (CSV)"/>
    </enum>

    <entity name="TareaImportacion" repository="abstract" cacheable="false">
        <datetime    name="fechaImportacion"  required="true" title="Fecha de importación"/>
        <many-to-one name="centro"            ref="com.educaflow.subsystem.common.db.Centro" required="true" title="Centro"/>
        <enum        name="tipoFichero"       ref="TipoFicheroImportacion" required="true" title="Tipo de fichero"/>
        <string      name="nombreFichero"     required="true" title="Nombre del fichero"/>
        <many-to-one name="fichero"           ref="com.axelor.meta.db.MetaFile" required="true" title="Fichero"/>
        <many-to-one name="usuarioImportador" ref="com.axelor.auth.db.User" required="true" title="Usuario importador"/>
        <integer     name="curso"             required="true" title="Curso académico"/>
        <datetime    name="fechaExportacion"  required="true" title="Fecha de exportación"/>
        <boolean     name="estado"            required="true" title="Estado" help="true = correcta; false = fallida"/>
        <string      name="log"               required="true" large="true" title="Log de importación"/>

        <finder-method name="findCorrectaByClave"
                       using="com.educaflow.subsystem.common.db.Centro:centro,TipoFicheroImportacion:tipoFichero,java.time.LocalDateTime:fechaExportacion,Integer:curso"
                       filter="self.centro = :centro AND self.tipoFichero = :tipoFichero AND self.fechaExportacion = :fechaExportacion AND self.curso = :curso AND self.estado = true"
                       all="true"/>
    </entity>

</domain-models>
```

Notas:
- El enum se declara en el mismo fichero de dominio. Axelor genera la clase enum en el paquete del módulo.
- `Centro.xml` (en `subsystem/common`) ya declara `<one-to-many name="tareasImportacion" ref="com.educaflow.subsystem.importacion.db.TareaImportacion" mappedBy="centro"/>`; no se modifica.

#### 2.2 Modificar `subsystem/registrousuario/domains/UsuarioAutorizado.xml`

Cambios sobre el fichero actual:

- `curso` pasa a `required="true"`.
- `fechaExportacion` cambia de `<date>` a `<datetime>` y pasa a `required="true"`.
- Eliminar la antigua `<unique-constraint columns="centro,dni,tipoUsuario"/>`.
- Añadir `<unique-constraint columns="centro,dni,tipoUsuario,curso,fechaExportacion"/>`.
- Añadir finders para soportar la actualización XML y la verificación de unicidad de inserción.

Fichero resultante:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<domain-models xmlns="http://axelor.com/xml/ns/domain-models"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://axelor.com/xml/ns/domain-models https://axelor.com/xml/ns/domain-models/domain-models_8.1.xsd">

    <module name="registro" package="com.educaflow.subsystem.registrousuario.db"/>

    <entity name="UsuarioAutorizado" repository="abstract" cacheable="false">
        <many-to-one name="centro"           ref="com.educaflow.subsystem.common.db.Centro" required="true" title="Centro"/>
        <string      name="dni"              required="true" title="dni__!!"/>
        <many-to-one name="tipoUsuario"      ref="com.educaflow.subsystem.common.db.TipoUsuario" required="true" title="Tipo de usuario"/>
        <integer     name="curso"            required="true" title="Curso académico"/>
        <datetime    name="fechaExportacion" required="true" title="Fecha de exportación"/>

        <unique-constraint columns="centro,dni,tipoUsuario,curso,fechaExportacion"/>

        <finder-method name="findByClave"
                       using="com.educaflow.subsystem.common.db.Centro:centro,String:dni,com.educaflow.subsystem.common.db.TipoUsuario:tipoUsuario,Integer:curso,java.time.LocalDateTime:fechaExportacion"
                       filter="self.centro = :centro AND self.dni = :dni AND self.tipoUsuario = :tipoUsuario AND self.curso = :curso AND self.fechaExportacion = :fechaExportacion"/>

        <finder-method name="findUltimaFechaExportacion"
                       using="com.educaflow.subsystem.common.db.Centro:centro,String:dni,com.educaflow.subsystem.common.db.TipoUsuario:tipoUsuario,Integer:curso"
                       filter="self.centro = :centro AND self.dni = :dni AND self.tipoUsuario = :tipoUsuario AND self.curso = :curso ORDER BY self.fechaExportacion DESC"
                       all="true"/>

        <finder-method name="findDnisByCentroTipoCurso"
                       using="com.educaflow.subsystem.common.db.Centro:centro,com.educaflow.subsystem.common.db.TipoUsuario:tipoUsuario,Integer:curso"
                       filter="self.centro = :centro AND self.tipoUsuario = :tipoUsuario AND self.curso = :curso"
                       all="true"/>
    </entity>

</domain-models>
```

Notas operativas:
- Asunción A10* del análisis: los registros preexistentes deben quedar compatibles con el nuevo esquema. La migración SQL/data del cambio `date → datetime` y el relleno de `curso`/`fechaExportacion` queda fuera del alcance del diseño y es responsabilidad operativa.

---

### Paso 3 — Servicios

#### 3.1 `com.educaflow.subsystem.importacion.service.dto.TareaImportacionInsertDTO`

Record con los dos campos del wizard que llegan en la petición:

```java
public record TareaImportacionInsertDTO(TipoFicheroImportacion tipoFichero, MetaFile fichero) { }
```

#### 3.2 `com.educaflow.subsystem.importacion.service.dto.ResultadoImportacion`

Record inmutable devuelto por `ImportadorFichero`:

```java
public record ResultadoImportacion(
        String centroCodigo,
        Integer curso,
        LocalDateTime fechaExportacion,
        List<String> dnisValidos,
        List<String> dnisInvalidos,
        List<String> dnisDuplicadosIntraFichero,
        String logParcial
) { }
```

Reglas internas: `dnisValidos` ya es la lista deduplicada (primera aparición); `dnisDuplicadosIntraFichero` contiene cada DNI que apareció más de una vez (V-029); `dnisInvalidos` contiene los DNIs descartados por `DniUtil.clean+isValid` (V-017).

#### 3.3 `com.educaflow.subsystem.importacion.service.ImportadorException`

```java
public class ImportadorException extends Exception {
    public ImportadorException(String motivo);
    public ImportadorException(String motivo, Throwable causa);
}
```

Excepción usada por las implementaciones de `ImportadorFichero` cuando falla V-021, V-022, V-023 o V-024. La captura `TareaImportacionServiceImpl.procesarImportacion` y la traduce a `TareaImportacion` con `estado=false`.

#### 3.4 `com.educaflow.subsystem.importacion.service.ImportadorFichero`

```java
public interface ImportadorFichero {

    /**
     * Lee, valida y parsea el fichero según el tipo. NO inserta UsuarioAutorizado ni toca usuarios registrados.
     *
     * Reglas aplicadas:
     *  - V-021 (XML cumple XSD) o V-022 (CSV legible).
     *  - V-023 (tipoFichero coincide con el contenido XML).
     *  - V-024 (solo XML: <centro codigo> coincide con el centro activo del importador).
     *  - V-017 (cada DNI se procesa con DniUtil.clean + isValid; los inválidos se acumulan en dnisInvalidos).
     *  - V-029 (DNIs duplicados intra-fichero se acumulan en dnisDuplicadosIntraFichero; la primera aparición queda en dnisValidos).
     *  - CSV: si la primera línea no es DNI válido tras clean, se descarta silenciosamente (A13*).
     *  - V-032: 0 DNIs válidos NO es error — devuelve ResultadoImportacion con dnisValidos vacío.
     *
     * Lanza ImportadorException si V-021/V-022/V-023/V-024 fallan; el mensaje se incluirá tal cual en el log de la TareaImportacion fallida (V-030).
     */
    ResultadoImportacion importar() throws ImportadorException;
}
```

#### 3.5 `com.educaflow.subsystem.importacion.service.ImportadorFicheroFactory`

```java
public class ImportadorFicheroFactory {

    /**
     * Resuelve la implementación adecuada al tipoFichero:
     *   PROFESOR_XML / ALUMNO_XML / FAMILIAR_XML  → new ImportadorUsuarioXML(fichero, tipoFichero)
     *   PROFESOR_EXTERNO_CSV                      → new ImportadorUsuarioCSV(fichero, tipoFichero)
     *
     * Conforme a la guía 5, el constructor recibe únicamente `fichero` y `tipoFichero`. El centro y el curso
     * se obtienen del fichero (XML) o del contexto (`AuthUtils.getUser().getCentroActivo()` en CSV) dentro
     * de la propia implementación.
     *
     * Si tipoFichero es null o no está en los 4 valores -> IllegalArgumentException; el llamador
     * (TareaImportacionServiceImpl) lo traduce a BusinessMessages V-019 antes de invocar al importador.
     */
    public ImportadorFichero crear(MetaFile fichero, TipoFicheroImportacion tipoFichero);
}
```

#### 3.6 `com.educaflow.subsystem.importacion.service.impl.ImportadorUsuarioXML`

```java
public class ImportadorUsuarioXML implements ImportadorFichero {

    private final MetaFile fichero;
    private final TipoFicheroImportacion tipoFichero;

    public ImportadorUsuarioXML(MetaFile fichero, TipoFicheroImportacion tipoFichero);

    /**
     * Pasos:
     *  1) Resuelve XSD según tipoFichero:
     *       PROFESOR_XML  -> /data-import/schemas/profesores.xsd  (elemento sección esperado: <docentes>, hijos <docente>)
     *       ALUMNO_XML    -> /data-import/schemas/alumnos.xsd     (sección <alumnos>, hijos <alumno>)
     *       FAMILIAR_XML  -> /data-import/schemas/familiares.xsd  (sección <familiares>, hijos <familiar>)
     *  2) Valida el XML contra el XSD con `XMLUtil.validarConSchema(xmlStream, xsdStream)`.
     *     Si Optional<String> está presente -> ImportadorException("Formato XML inválido: " + detalle)
     *     transmitiendo el motivo concreto del validador (V-021).
     *  3) Parsea con DOM (DocumentBuilder). Comprueba que el elemento raíz contiene la sección esperada
     *     para tipoFichero; si no, ImportadorException con el motivo de coherencia (V-023). Esta
     *     comprobación se hace incluso si el XSD pasa (los XSD aceptan `<xs:any>` adicionales, así que la coherencia
     *     se valida explícitamente).
     *  4) Extrae los atributos de <centro>: codigo (String), curso (Integer.parseInt), fechaExportacion
     *     (parse con DateTimeFormatter "dd/MM/yyyy HH:mm:ss"). Si parseo falla -> ImportadorException
     *     describiendo el motivo (cae en V-021).
     *  5) Resuelve el centro activo del contexto (`AuthUtils.getUser().getCentroActivo()`) y compara su
     *     `code` con el atributo `codigo` del fichero (V-024). Si distinto -> ImportadorException transmitiendo
     *     el código del fichero y el código del centro activo.
     *  6) Recorre los hijos de la sección (docente/alumno/familiar). Por cada hijo extrae el atributo
     *     "documento", aplica DniUtil.clean. Si DniUtil.isValid es false -> dnisInvalidos (V-017).
     *     Si ya estaba en dnisValidos -> dnisDuplicadosIntraFichero (V-029). En otro caso -> dnisValidos.
     *  7) Construye y devuelve ResultadoImportacion(centroCodigo=codigo del fichero, curso, fechaExportacion,
     *     dnisValidos, dnisInvalidos, dnisDuplicadosIntraFichero, logParcial). logParcial contiene un resumen
     *     numérico de la lectura.
     *
     * Mensajes transmitidos (no literales):
     *  - V-021: motivo concreto devuelto por el validador XSD.
     *  - V-023: tipoFichero esperado vs sección encontrada.
     *  - V-024: codigoFichero vs codigoCentroActivo.
     *  - V-017: cada DNI inválido con su valor.
     *  - V-029: cada DNI duplicado intra-fichero con su valor.
     */
    @Override
    public ResultadoImportacion importar() throws ImportadorException;

    /** Mapeo tipoFichero -> ruta XSD. */
    private String resolverRutaXSD();

    /** Mapeo tipoFichero -> nombre del elemento sección esperado (docentes/alumnos/familiares). */
    private String resolverSeccionEsperada();

    /** Mapeo tipoFichero -> nombre del elemento hijo esperado (docente/alumno/familiar). */
    private String resolverElementoHijoEsperado();
}
```

#### 3.7 `com.educaflow.subsystem.importacion.service.impl.ImportadorUsuarioCSV`

```java
public class ImportadorUsuarioCSV implements ImportadorFichero {

    private final MetaFile fichero;
    private final TipoFicheroImportacion tipoFichero;

    public ImportadorUsuarioCSV(MetaFile fichero, TipoFicheroImportacion tipoFichero);

    /**
     * Pasos:
     *  1) Lee el MetaFile como texto UTF-8 línea a línea. Si la lectura técnica falla
     *     (IOException, codificación no válida) -> ImportadorException con el motivo (V-022).
     *  2) Si la primera línea NO supera DniUtil.clean + DniUtil.isValid, se descarta silenciosamente
     *     como cabecera (A13*) y NO se anota en log.
     *  3) Procesa el resto de líneas no vacías: por cada línea aplica clean + isValid; los inválidos
     *     se acumulan en dnisInvalidos (V-017); los duplicados intra-fichero en dnisDuplicadosIntraFichero (V-029);
     *     los válidos no duplicados en dnisValidos.
     *  4) Resuelve el centro activo del contexto (`AuthUtils.getUser().getCentroActivo()`).
     *     centroCodigo = centroActivo.getCode(); curso = centroActivo.getCurso();
     *     fechaExportacion = LocalDateTime.now() (instante del procesado).
     *  5) NO valida centro contra fichero (no aplica V-024 en CSV).
     *  6) V-032: si todas las líneas son inválidas o el fichero solo tenía cabecera, devuelve ResultadoImportacion
     *     con dnisValidos vacío y logParcial indicando "0 DNIs válidos".
     *  7) Devuelve el ResultadoImportacion.
     */
    @Override
    public ResultadoImportacion importar() throws ImportadorException;
}
```

#### 3.8 `com.educaflow.subsystem.importacion.service.ActualizadorTiposUsuarioRegistrados`

Clase colaboradora (Guice-managed via `@Inject`) que aplica las reglas posteriores a la inserción de `UsuarioAutorizado`. Se justifica como clase aparte (guía 10) por la complejidad cohesiva de la matriz XML 2x2.

```java
public class ActualizadorTiposUsuarioRegistrados {

    @Inject private ModelServiceFactory modelServiceFactory;
    // Resuelve CentroUsuarioService y UsuarioAutorizadoService desde modelServiceFactory.

    /**
     * Aplica las reglas XML (V-033..V-037) sobre los usuarios registrados del centro.
     *
     * Parámetros:
     *   centro: centro de la importación.
     *   tipoBase: TipoUsuario T resuelto desde tipoFichero (PROFESOR/ALUMNO/FAMILIAR).
     *   tipoEx:   TipoUsuario EX_T contrapartida (EXPROFESOR/EXALUMNO/EXFAMILIAR).
     *   curso:    curso del fichero (A14*).
     *   fechaExportacionFichero: fechaExportacion del fichero (sirve para distinguir Actual/Anterior).
     *   dnisFicheroValidos: lista deduplicada de DNIs válidos insertados en UsuarioAutorizado en esta importación.
     *   logBuilder: StringBuilder donde se acumulan las trazas de cada acción.
     *
     * Algoritmo:
     *  1) Construir el universo:
     *      - dnisRegistradosBase = UsuarioAutorizadoService.findDnisByCentroTipoCurso(centro, tipoBase, curso)
     *      - dnisRegistradosEx   = UsuarioAutorizadoService.findDnisByCentroTipoCurso(centro, tipoEx, curso)
     *      - universoDnis = union(dnisFicheroValidos, dnisRegistradosBase, dnisRegistradosEx)
     *      (Se opera sobre DNIs porque User puede no existir y debe respetarse V-040.)
     *  2) Para cada dni del universo:
     *      a) Resolver User por DNI vía UserRepository (finder findByDni; si no existe, añadirlo).
     *         Si user == null -> registrar V-040 en logBuilder (línea informativa) y continuar.
     *      b) Resolver CentroUsuario(centro, user) vía CentroUsuarioService.findByCentroAndUser.
     *         Si no existe -> registrar nota informativa y continuar (no se crea CentroUsuario en XML).
     *      c) Calcular "Actual": el dni está en dnisFicheroValidos.
     *      d) Calcular "Anterior":
     *         - ultima = UsuarioAutorizadoService.findUltimaFechaExportacion(centro, dni, tipoBase, curso)
     *         - Anterior = ultima.isPresent() && ultima.get().isBefore(fechaExportacionFichero)
     *      e) Aplicar la tabla:
     *           (Actual=No, Anterior=No)  -> CentroUsuarioService.quitarTipoUsuario(cu, tipoBase) y quitarTipoUsuario(cu, tipoEx)  (V-033, defensivo; log)
     *           (Actual=No, Anterior=Sí)  -> CentroUsuarioService.añadirTipoUsuario(cu, tipoEx); quitarTipoUsuario(cu, tipoBase)  (V-034)
     *           (Actual=Sí, Anterior=No)  -> CentroUsuarioService.añadirTipoUsuario(cu, tipoBase); quitarTipoUsuario(cu, tipoEx)  (V-035)
     *           (Actual=Sí, Anterior=Sí)  -> CentroUsuarioService.añadirTipoUsuario(cu, tipoBase); quitarTipoUsuario(cu, tipoEx)  (V-036)
     *         Las operaciones añadir/quitar son idempotentes; tras cada par se garantiza la mutua exclusión
     *         base ↔ EX (V-037).
     *      f) Anotar la acción en logBuilder transmitiendo dni, centro, tipo añadido y tipo retirado.
     */
    public void aplicarParaXML(Centro centro,
                               TipoUsuario tipoBase,
                               TipoUsuario tipoEx,
                               Integer curso,
                               LocalDateTime fechaExportacionFichero,
                               List<String> dnisFicheroValidos,
                               StringBuilder logBuilder);

    /**
     * Aplica las reglas CSV (V-038, V-039, V-040) para PROFESOR_EXTERNO.
     *
     * Parámetros:
     *   centro: centro activo del importador.
     *   tipoProfesorExterno: TipoUsuario con codigo "PROFESOR_EXTERNO".
     *   dnisValidos: lista deduplicada de DNIs válidos del CSV.
     *   logBuilder.
     *
     * Algoritmo: para cada dni:
     *   - Resolver User por DNI.
     *   - Si user == null -> V-040 (no actúa sobre registrados; sin nota en log según A8*).
     *   - Si user != null y existe CentroUsuario(centro,user) -> CentroUsuarioService.añadirTipoUsuario(cu, tipoProfesorExterno) (idempotente, V-038).
     *   - Si user != null y NO existe CentroUsuario(centro,user) -> CentroUsuarioService.crearCentroUsuarioConTipo(centro, user, tipoProfesorExterno) (V-039).
     * Cada acción de V-038/V-039 anota línea en logBuilder transmitiendo el dni y la operación.
     */
    public void aplicarParaCSV(Centro centro,
                               TipoUsuario tipoProfesorExterno,
                               List<String> dnisValidos,
                               StringBuilder logBuilder);
}
```

#### 3.9 `com.educaflow.subsystem.importacion.service.TareaImportacionService` (interfaz)

```java
public interface TareaImportacionService extends ModelService<TareaImportacion> {

    /**
     * Op-4 completo. Punto de entrada del wizard.
     *
     * Pasos internos (referencias V-XXX entre paréntesis):
     *  1) Validar V-019 sobre el DTO (tipoFichero ∈ enum); si falla, devolver mensaje sin persistir nada.
     *  2) Resolver el ImportadorFichero vía ImportadorFicheroFactory.crear(dto.fichero, dto.tipoFichero).
     *  3) Invocar importador.importar():
     *       try -> ResultadoImportacion (contiene centroCodigo, curso, fechaExportacion, dnisValidos, etc.);
     *       catch ImportadorException -> persistirFallida con motivo del importador y return id (V-021..V-024, V-030).
     *       En este punto, si todavía no se conoce el Centro (ImportadorException antes de obtener centroCodigo),
     *       se persiste la tarea fallida con `centro = AuthUtils.getUser().getCentroActivo()` como mejor esfuerzo.
     *  4) Resolver el Centro vía CentroService.findByCodigo(resultado.centroCodigo()). Si no existe,
     *     persistirFallida con motivo "centro inexistente" y return id (V-024 defensivo).
     *  5) Verificar V-025 vía TareaImportacionRepository.findCorrectaByClave(centro, tipoFichero, fechaExportacion, curso);
     *     si la lista NO está vacía -> persistirFallida con motivo "ya existe una importación correcta previa"
     *     transmitiendo (centro, tipoFichero, curso, fechaExportacion) y return id (V-025, V-030).
     *  6) Resolver TipoUsuario base T por código (PROFESOR_XML→PROFESOR, ALUMNO_XML→ALUMNO, FAMILIAR_XML→FAMILIAR,
     *     PROFESOR_EXTERNO_CSV→PROFESOR_EXTERNO) usando `JpaRepository.of(TipoUsuario.class)` con filtro por code (V-013).
     *  7) Iniciar la fase de modificaciones BD (toda dentro de la transacción del controller):
     *      a) Por cada dni de resultadoImportacion.dnisValidos():
     *           usuarioAutorizadoService.insertarPorImportacion(centro, dni, tipoBase, curso, fechaExportacion)
     *         Cualquier violación de unicidad (V-016) capturada se anota en logBuilder (no aborta).
     *      b) Si tipoFichero es XML -> resolver tipoEx (T_EX) y llamar
     *         actualizador.aplicarParaXML(centro, tipoBase, tipoEx, curso, fechaExportacion, dnisValidos, logBuilder).
     *      c) Si tipoFichero es PROFESOR_EXTERNO_CSV -> llamar
     *         actualizador.aplicarParaCSV(centro, tipoBase, dnisValidos, logBuilder).
     *  8) Si todo el paso 7 termina sin excepción técnica:
     *       persistirCorrecta(centro, tipoFichero, fichero, curso, fechaExportacion, log final).
     *       (V-032: si dnisValidos estaba vacío, persistirCorrecta igualmente con log "0 importados".)
     *       return id.
     *  9) Si en el paso 7 salta una RuntimeException técnica:
     *       - Marcar rollback de la transacción del controller (revierte UsuarioAutorizado insertados y cambios
     *         en CentroUsuario/CentroUsuarioTipoUsuario aplicados en esta llamada — V-031).
     *       - Persistir TareaImportacion fallida en una transacción nueva (REQUIRES_NEW) con estado=false y
     *         motivo "{causa.getMessage()}; cambios revertidos".
     *       - return id.
     *
     * Devuelve el id de la TareaImportacion persistida (correcta o fallida) para que el controller
     * abra el detalle en el mismo @Main-form.
     *
     * Mensajes transmitidos al log (descritos, no literales):
     *  - V-018/V-019/V-020: campo o valor que falta o no es válido (devueltos al controller como BusinessMessages
     *    si la validación cliente fuese eludida; en flujo normal estos ya quedan filtrados por action-validate).
     *  - V-021/V-022/V-023/V-024: motivo del importador.
     *  - V-025: clave (centro, tipoFichero, curso, fechaExportacion) que ya existía.
     *  - V-016: dni que violó la unicidad.
     *  - V-017: cada dni inválido omitido.
     *  - V-029: cada dni duplicado intra-fichero.
     *  - V-030: motivo de la persistencia con estado=false.
     *  - V-031: causa técnica + nota de reversión.
     *  - V-032: "Importación correcta con 0 usuarios importados."
     *  - V-033..V-040: cada acción aplicada sobre los registrados.
     */
    Long procesarImportacion(TareaImportacionInsertDTO dto);

    /**
     * V-001..V-010. Garantiza que todos los campos requeridos están informados al persistir
     * una TareaImportacion desde el flujo interno. Transmite por cada campo faltante un mensaje
     * que identifica el nombre lógico del campo.
     * V-027: si el insert llega desde fuera del flujo interno (UI / API), rechaza con mensaje
     * que transmite "operación no permitida — TareaImportacion solo se crea mediante el flujo
     * de importación". El servicio distingue ambos flujos usando un flag interno (parámetro
     * de un método privado o ThreadLocal del subsistema).
     */
    @Override
    Optional<BusinessMessages> validateInsert(TareaImportacion entidad);

    /** V-027: rechaza siempre, transmitiendo que la tarea es inmutable. */
    @Override
    Optional<BusinessMessages> validateUpdate(TareaImportacion entidad);

    /** V-027: rechaza siempre, transmitiendo que la tarea no puede eliminarse. */
    @Override
    Optional<BusinessMessages> validateRemove(TareaImportacion entidad);
}
```

#### 3.10 `com.educaflow.subsystem.importacion.service.impl.TareaImportacionServiceImpl`

```java
public class TareaImportacionServiceImpl
        extends DefaultModelService<TareaImportacion>
        implements TareaImportacionService {

    @Inject private ImportadorFicheroFactory importadorFicheroFactory;
    @Inject private ActualizadorTiposUsuarioRegistrados actualizador;
    @Inject private ModelServiceFactory modelServiceFactory;
    // Repositorios resueltos vía JpaRepository.of(...) donde haga falta para entidades distintas
    // a TareaImportacion. Para TareaImportacion se usa `repository` heredado.

    public TareaImportacionServiceImpl(Class<TareaImportacion> model,
                                       Repository<TareaImportacion> repository) {
        super(model, repository);
    }

    /**
     * Implementación de procesarImportacion según el algoritmo descrito en la interfaz.
     * Compone el logBuilder con secciones: resumen de lectura del importador (resultadoImportacion.logParcial()),
     * resultado de inserción de UsuarioAutorizado, resultado de la actualización de registrados, y
     * conclusión final (correcta / fallida con motivo). Internamente:
     *  - Llama a `persistirCorrecta(...)` o `persistirFallida(...)` (métodos privados de esta clase) según el caso.
     *  - Usa un flag interno (parámetro `boolean trusted=true` en `crearYPersistir(...)`) para
     *    pasar por encima de validateInsert respecto a la regla V-027 cuando el flujo es legítimo.
     */
    @Override
    @Transactional
    public Long procesarImportacion(TareaImportacionInsertDTO dto);

    /** V-001..V-010 + V-027 (sin flag interno -> rechaza). Mensaje describe el campo o la condición violada. */
    @Override
    public Optional<BusinessMessages> validateInsert(TareaImportacion entidad);

    /** V-027: siempre rechaza. */
    @Override
    public Optional<BusinessMessages> validateUpdate(TareaImportacion entidad);

    /** V-027: siempre rechaza. */
    @Override
    public Optional<BusinessMessages> validateRemove(TareaImportacion entidad);

    // --- Métodos privados auxiliares ---

    /**
     * V-019: comprueba que tipoFichero no es null y está entre los 4 valores; si no, devuelve
     * BusinessMessages con el mensaje que transmite el valor recibido y los 4 valores válidos.
     * (En flujo normal nunca se alcanza porque la action-validate del cliente ya lo bloquea.)
     */
    private Optional<BusinessMessages> validarDTO(TareaImportacionInsertDTO dto);

    /** Crea, valida y persiste TareaImportacion con estado=true. Devuelve id. */
    private Long persistirCorrecta(Centro centro,
                                   TipoFicheroImportacion tipoFichero,
                                   MetaFile fichero,
                                   Integer curso,
                                   LocalDateTime fechaExportacion,
                                   User usuarioImportador,
                                   String log);

    /**
     * V-030. Crea y persiste TareaImportacion con estado=false en una transacción independiente
     * (REQUIRES_NEW) para garantizar que el rastro queda aunque la transacción principal se reverta. Devuelve id.
     */
    private Long persistirFallida(Centro centro,
                                  TipoFicheroImportacion tipoFichero,
                                  MetaFile fichero,
                                  Integer curso,
                                  LocalDateTime fechaExportacion,
                                  User usuarioImportador,
                                  String motivo);

    /** Mapeo TipoFicheroImportacion -> codigo TipoUsuario (PROFESOR_XML→PROFESOR, etc.). */
    private String codigoTipoUsuarioBasePara(TipoFicheroImportacion tipoFichero);

    /** Mapeo codigo TipoUsuario base -> codigo TipoUsuario EX (PROFESOR→EXPROFESOR, …). */
    private String codigoTipoUsuarioExPara(String codigoBase);

    /** Resuelve un TipoUsuario por código consultando JpaRepository.of(TipoUsuario.class) por el finder estándar. */
    private TipoUsuario resolverTipoUsuarioPorCodigo(String codigo);
}
```

#### 3.11 `com.educaflow.subsystem.registrousuario.service.UsuarioAutorizadoService` (interfaz)

```java
public interface UsuarioAutorizadoService extends ModelService<UsuarioAutorizado> {

    /**
     * Inserta un UsuarioAutorizado durante el flujo de importación.
     *  - Si findByClave(centro, dni, tipoUsuario, curso, fechaExportacion) ya existe -> no inserta y devuelve
     *    BusinessMessages con un mensaje V-016 que transmite la clave.
     *  - Si V-017 (DniUtil.clean+isValid) falla -> devuelve BusinessMessages con mensaje V-017 que transmite el dni.
     *    (En flujo normal este chequeo ya lo hace el importador, pero se repite como defensa.)
     *  - Caso normal: persiste con repository.save() (NO pasa por validateInsert, que rechazaría por V-028).
     * Devuelve Optional<BusinessMessages> con mensajes informativos o Optional.empty() si la inserción fue limpia.
     */
    Optional<BusinessMessages> insertarPorImportacion(Centro centro,
                                                     String dni,
                                                     TipoUsuario tipoUsuario,
                                                     Integer curso,
                                                     LocalDateTime fechaExportacion);

    /**
     * V-040 helper. Devuelve la fechaExportacion más reciente de UsuarioAutorizado(centro, dni, tipoUsuario, curso)
     * o Optional.empty si no hay ninguno. Usado por ActualizadorTiposUsuarioRegistrados para determinar "Anterior".
     */
    Optional<LocalDateTime> findUltimaFechaExportacion(Centro centro, String dni, TipoUsuario tipoUsuario, Integer curso);

    /**
     * Devuelve el conjunto de DNIs registrados en UsuarioAutorizado para (centro, tipoUsuario, curso).
     * Usado por ActualizadorTiposUsuarioRegistrados para construir el universo XML.
     */
    Set<String> findDnisByCentroTipoCurso(Centro centro, TipoUsuario tipoUsuario, Integer curso);

    /**
     * V-011..V-015 (required) + V-028: la inserción externa SIEMPRE se rechaza con un mensaje que
     * transmite "UsuarioAutorizado solo se crea mediante el proceso de importación".
     */
    @Override
    Optional<BusinessMessages> validateInsert(UsuarioAutorizado entidad);

    /** V-028: siempre rechaza. */
    @Override
    Optional<BusinessMessages> validateUpdate(UsuarioAutorizado entidad);

    /** V-028: siempre rechaza. */
    @Override
    Optional<BusinessMessages> validateRemove(UsuarioAutorizado entidad);
}
```

#### 3.12 `com.educaflow.subsystem.registrousuario.service.impl.UsuarioAutorizadoServiceImpl`

```java
public class UsuarioAutorizadoServiceImpl
        extends DefaultModelService<UsuarioAutorizado>
        implements UsuarioAutorizadoService {

    public UsuarioAutorizadoServiceImpl(Class<UsuarioAutorizado> model,
                                        Repository<UsuarioAutorizado> repository) {
        super(model, repository);
    }

    /**
     * Inserta con (UsuarioAutorizadoRepository) repository directamente (bypaseando validateInsert).
     * Antes consulta findByClave para detectar duplicados existentes (V-016).
     */
    @Override
    public Optional<BusinessMessages> insertarPorImportacion(Centro centro, String dni, TipoUsuario tipoUsuario,
                                                            Integer curso, LocalDateTime fechaExportacion);

    /** Cast a UsuarioAutorizadoRepository y llamada a findUltimaFechaExportacion. */
    @Override
    public Optional<LocalDateTime> findUltimaFechaExportacion(Centro centro, String dni, TipoUsuario tipoUsuario, Integer curso);

    /** Cast a UsuarioAutorizadoRepository y llamada a findDnisByCentroTipoCurso, devolviendo set deduplicado. */
    @Override
    public Set<String> findDnisByCentroTipoCurso(Centro centro, TipoUsuario tipoUsuario, Integer curso);

    /** V-028: siempre devuelve mensaje de inmutabilidad. */
    @Override
    public Optional<BusinessMessages> validateInsert(UsuarioAutorizado entidad);

    /** V-028: siempre rechaza. */
    @Override
    public Optional<BusinessMessages> validateUpdate(UsuarioAutorizado entidad);

    /** V-028: siempre rechaza. */
    @Override
    public Optional<BusinessMessages> validateRemove(UsuarioAutorizado entidad);
}
```

#### 3.13 `com.educaflow.subsystem.common.service.CentroService` (interfaz + impl)

```java
public interface CentroService extends ModelService<Centro> {

    /**
     * Devuelve el Centro cuyo `code` coincide con `codigo`, o Optional.empty si no existe.
     * Usado por `TareaImportacionServiceImpl.procesarImportacion` para resolver el Centro a partir
     * del centroCodigo devuelto por el ImportadorFichero.
     */
    Optional<Centro> findByCodigo(String codigo);
}
```

```java
public class CentroServiceImpl extends DefaultModelService<Centro> implements CentroService {

    public CentroServiceImpl(Class<Centro> model, Repository<Centro> repository) {
        super(model, repository);
    }

    /** Cast a `CentroRepository` (ya existe en `db/repo/CentroRepository.java` con `findByCodigo`) y delega. */
    @Override
    public Optional<Centro> findByCodigo(String codigo);
}
```

#### 3.14 `com.educaflow.subsystem.common.service.CentroUsuarioService` (interfaz)

```java
public interface CentroUsuarioService extends ModelService<CentroUsuario> {

    /**
     * Devuelve el CentroUsuario(centro, user) o Optional.empty. Consulta vía JpaRepository.of(CentroUsuario.class)
     * con filtro by (centro, usuario).
     */
    Optional<CentroUsuario> findByCentroAndUser(Centro centro, User user);

    /**
     * Añade tipoUsuario al CentroUsuario si no lo tenía ya (idempotente). Crea un CentroUsuarioTipoUsuario.
     * Devuelve true si lo añadió, false si ya estaba.
     */
    boolean añadirTipoUsuario(CentroUsuario centroUsuario, TipoUsuario tipoUsuario);

    /**
     * Elimina el CentroUsuarioTipoUsuario(centroUsuario, tipoUsuario) si existe (idempotente).
     * Devuelve true si lo eliminó, false si no estaba.
     */
    boolean quitarTipoUsuario(CentroUsuario centroUsuario, TipoUsuario tipoUsuario);

    /**
     * Crea un nuevo CentroUsuario(centro, user) si no existe y le añade el tipoUsuario indicado (V-039).
     * Devuelve el CentroUsuario resultante.
     */
    CentroUsuario crearCentroUsuarioConTipo(Centro centro, User user, TipoUsuario tipoUsuario);
}
```

No se crea servicio para `CentroUsuarioTipoUsuario` (guía 8 — es tabla de enlace puro).

#### 3.15 `com.educaflow.subsystem.common.service.impl.CentroUsuarioServiceImpl`

```java
public class CentroUsuarioServiceImpl
        extends DefaultModelService<CentroUsuario>
        implements CentroUsuarioService {

    public CentroUsuarioServiceImpl(Class<CentroUsuario> model,
                                    Repository<CentroUsuario> repository) {
        super(model, repository);
    }

    /** Cast a CentroUsuarioRepository (autogenerado) y consulta por (centro, usuario). */
    @Override
    public Optional<CentroUsuario> findByCentroAndUser(Centro centro, User user);

    /**
     * Comprueba si centroUsuario.getCentroUsuarioTipoUsuario() ya contiene un elemento con ese tipoUsuario.
     * Si no, crea un nuevo CentroUsuarioTipoUsuario, lo persiste con JpaRepository.of(CentroUsuarioTipoUsuario.class)
     * y lo añade a la colección del CentroUsuario. Devuelve true.
     */
    @Override
    public boolean añadirTipoUsuario(CentroUsuario centroUsuario, TipoUsuario tipoUsuario);

    /** Localiza y elimina el CentroUsuarioTipoUsuario correspondiente vía JpaRepository.of(CentroUsuarioTipoUsuario.class). */
    @Override
    public boolean quitarTipoUsuario(CentroUsuario centroUsuario, TipoUsuario tipoUsuario);

    /** Crea CentroUsuario, lo persiste y delega en añadirTipoUsuario para el tipo inicial. */
    @Override
    public CentroUsuario crearCentroUsuarioConTipo(Centro centro, User user, TipoUsuario tipoUsuario);
}
```

---

### Paso 4 — Repositorios

No se crean repositorios Java personalizados (todas las consultas necesarias caben en `<finder-method>` declarados en los dominios — paso 2):

- `TareaImportacionRepository` (generado por Axelor) expone `findCorrectaByClave(centro, tipoFichero, fechaExportacion, curso)`.
- `UsuarioAutorizadoRepository` (generado) expone `findByClave(...)`, `findUltimaFechaExportacion(...)`, `findDnisByCentroTipoCurso(...)`.
- `CentroUsuarioRepository`, `TipoUsuarioRepository`, `UserRepository`, `CentroRepository` son los generados/existentes. Se accede vía `JpaRepository.of(...)` o consultas inline dentro de los servicios (NO en TareaImportacionServiceImpl según la guía 9 — siempre vía servicios).

Si en el momento de la implementación se detecta que `User` o `CentroUsuarioTipoUsuario` no exponen el finder necesario, se añadirá un `<finder-method>` en el dominio correspondiente.

---

### Paso 5 — Controlador `TareaImportacionController`

Fichero: `src/main/java/com/educaflow/subsystem/importacion/controller/TareaImportacionController.java`

```java
public class TareaImportacionController {

    @Inject private ModelServiceFactory modelServiceFactory;

    /**
     * Acción del botón "Importar" del wizard (`subsysImportacion.TareaImportacion@Main-Remote-importar-action`).
     * Se ejecuta tras la validación cliente (V-018, V-020) del action-validate del action-group.
     *
     * Pasos:
     *  1) Resolver TareaImportacionService vía modelServiceFactory.
     *  2) Crear ActionRequestHelper<TareaImportacion>(actionRequest, TareaImportacion.class) y
     *     ActionResponseHelper(actionResponse).
     *  3) Extraer la TareaImportacion del request con AllowProperties LIMITADO a "tipoFichero" y "fichero":
     *       AllowProperties allowProperties = AllowProperties.createAllowProperties(Map.of(
     *           "tipoFichero", Map.of(),
     *           "fichero", Map.of()));
     *       TareaImportacion borrador = actionRequestHelper.getModel(allowProperties);
     *     Esto bloquea V-027 si la UI intenta enviar otros campos.
     *  4) Construir TareaImportacionInsertDTO(borrador.getTipoFichero(), borrador.getFichero()).
     *  5) Llamar a tareaImportacionService.procesarImportacion(dto). Devuelve el id de la tarea creada
     *     (correcta o fallida).
     *  6) Reabrir el mismo @Main-form en modo detalle mediante actionResponse.setView(
     *       ActionView.define("Detalle de importación")
     *                 .model("com.educaflow.subsystem.importacion.db.TareaImportacion")
     *                 .add("form", "subsysImportacion.TareaImportacion@Main-form")
     *                 .param("show-toolbar-form", "false")
     *                 .param("forceEdit", "true")
     *                 .context("_showRecord", idDeTareaCreada)
     *                 .map());
     *     (V-Op-2, A9*) Esto muestra el detalle del registro recién creado en el mismo @Main-form;
     *     al tener id != null, el form renderiza el panel de detalle.
     *  7) Si tareaImportacionService.procesarImportacion lanzó RuntimeException no controlada,
     *     actionResponseHelper.doResponseBusinessMessagesAsError con un BusinessMessages que transmite
     *     el motivo técnico (en flujo normal nunca debería ocurrir — el servicio captura todo).
     *
     * Cobertura: V-018/V-020 (vía action-validate), V-019 (vía servicio), V-021..V-040 (vía servicio).
     */
    @CallMethod
    @Transactional
    public void importar(ActionRequest actionRequest, ActionResponse actionResponse);
}
```

Reglas observadas:
- Parámetros `actionRequest` / `actionResponse` (k-sistemas/controladores.md).
- `@Transactional` de `com.google.inject.persist.Transactional` (NO `jakarta.transaction`).
- Una única entidad por controller; no se crea otro controller para `UsuarioAutorizado` (su vista es solo grid de listado).

Los botones "Cancelar" del wizard y "Aceptar" del detalle no requieren método controller — se resuelven con `<action-group>` que invoca `back`.

---

### Paso 6 — Vistas

#### 6.1 `src/main/java/com/educaflow/subsystem/importacion/views/TareaImportacion.xml`

Un único `<action-view>` en este fichero (regla arquitectónica de k-sistemas).

**Vistas declaradas:**

| Vista | Nombre | Modelo | Descripción estructural |
|---|---|---|---|
| `<action-view>` | `subsysImportacion.TareaImportacion@Main-action` | `com.educaflow.subsystem.importacion.db.TareaImportacion` | Title "Importaciones". `<view type="grid" name="subsysImportacion.TareaImportacion@Main-grid"/>` y `<view type="form" name="subsysImportacion.TareaImportacion@Main-form"/>`. `<view-param name="show-toolbar-form" value="false"/>` y `<view-param name="forceEdit" value="true"/>` (guía 7.2). |
| `<grid>` | `subsysImportacion.TareaImportacion@Main-grid` | TareaImportacion | `canNew="true"`, `canEdit="false"`, `canDelete="false"`, `canEditOnClick="true"`. Atributos obligatorios de la guía 7.3: `canAdvanceSearch="false"`, `canRefresh="false"`, `allowSearchFields="false"`. `orderBy="-fechaImportacion"`. Columnas: `fechaImportacion`, `centro`, `tipoFichero`, `nombreFichero`, `usuarioImportador`, `estado`. |
| `<form>` | `subsysImportacion.TareaImportacion@Main-form` | TareaImportacion | `canBack="false"` (guía 7.2), `canDelete="false"`, `canSave="false"`, `canCopy="false"`, `canAttach="false"`, `canMore="false"`. Estructura con dos paneles excluyentes (panel principal `showIf="id == null"` para wizard; panel principal `showIf="id != null"` para detalle) + panel de botones. |

**Estructura del form:**

- Panel wizard `showIf="id == null"`:
  - `tipoFichero` (`widget="SwitchSelect"`, `required="true"`).
  - `fichero` (`widget="binary-link"`, `required="true"`).
- Panel detalle `showIf="id != null"` con todos los campos `readonly="true"`:
  - Fila 1: `fechaImportacion`, `estado`.
  - Fila 2: `centro`, `usuarioImportador`.
  - Fila 3: `tipoFichero`, `curso`, `fechaExportacion`.
  - Fila 4: `nombreFichero`, `fichero` (`widget="binary-link"` para descarga — cubre Op-3).
  - Fila 5: `log` ocupando `colSpan="12"`, multilínea/large, readonly.
- Panel de botones `name="buttons-panel"` `showFrame="false"`:
  - Wizard (`showIf="id == null"`): "Cancelar" (`onClick="subsysImportacion.TareaImportacion@Main-btnCancelar-action"`), "Importar" (`onClick="subsysImportacion.TareaImportacion@Main-btnImportar-action"`).
  - Detalle (`showIf="id != null"`): "Aceptar" (`onClick="subsysImportacion.TareaImportacion@Main-btnAceptar-action"`).

**Acciones declaradas en el fichero:**

| Acción | Tipo | Propósito | Campos / condiciones |
|---|---|---|---|
| `subsysImportacion.TareaImportacion@Main-btnImportar-action` | `<action-group>` | Encadena la validación cliente y la llamada al controlador para procesar la importación. | Ejecuta primero `Main-Local-validateImportar-action`, después `Main-Remote-importar-action`. |
| `subsysImportacion.TareaImportacion@Main-btnCancelar-action` | `<action-group>` | Cierra el wizard sin procesar. | Ejecuta `back` directo. |
| `subsysImportacion.TareaImportacion@Main-btnAceptar-action` | `<action-group>` | Cierra el detalle volviendo al listado. | Ejecuta `back` directo. |
| `subsysImportacion.TareaImportacion@Main-Local-validateImportar-action` | `<action-validate>` | V-018 + V-020 en cliente. | Dos `<error>`: uno con condición `tipoFichero == null` cuyo mensaje transmite "selecciona el tipo de fichero"; otro con condición `fichero == null` cuyo mensaje transmite "selecciona el fichero a importar". V-019 queda cubierta intrínsecamente por el enum + SwitchSelect (dominio finito). |
| `subsysImportacion.TareaImportacion@Main-Remote-importar-action` | `<action-method>` | Llama al controlador para ejecutar la importación. | `<call class="com.educaflow.subsystem.importacion.controller.TareaImportacionController" method="importar"/>`. |

Orden interno del fichero (k-vistas): action-view → grid → form → action-group → action-validate → action-method (no hay action-record ni action-attrs en esta vista).

#### 6.2 `src/main/java/com/educaflow/subsystem/importacion/views/UsuarioAutorizado.xml`

Un único `<action-view>` (regla arquitectónica). El modelo está en otro subsistema (`registrousuario`) pero la UI vive en `importacion` porque pertenece al ámbito funcional del importador.

**Vistas declaradas:**

| Vista | Nombre | Modelo | Descripción estructural |
|---|---|---|---|
| `<action-view>` | `subsysImportacion.UsuarioAutorizado@Main-action` | `com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado` | Title "Usuarios autorizados". `<view type="grid" name="subsysImportacion.UsuarioAutorizado@Main-grid"/>`. (Sin `<form>` propio — el grid es de solo lectura y no abre detalle, ver más abajo.) |
| `<grid>` | `subsysImportacion.UsuarioAutorizado@Main-grid` | UsuarioAutorizado | `canNew="false"`, `canEdit="false"`, `canDelete="false"` (V-028). Atributos obligatorios (guía 7.3): `canAdvanceSearch="false"`, `canRefresh="false"`, `allowSearchFields="false"`. Columnas: `centro`, `dni`, `tipoUsuario`, `curso`, `fechaExportacion`. Sin `canEditOnClick` ni `canViewOnClick` (solo listado). |

Acciones: ninguna (vista de solo lectura).

#### 6.3 Modificar `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`

Bajo `administracionSv-menuitem` (que ya existe y tiene `groups="admins"`) añadir el siguiente menuitem **debajo** del existente `administracionSv-importacion-menuitem` (cuya `order=2` ya está):

```xml
<menuitem name="administracionSv-usuariosAutorizados-menuitem" parent="administracionSv-menuitem" title="Usuarios autorizados" action="subsysImportacion.UsuarioAutorizado@Main-action" groups="admins" order="3"/>
```

(Se elige `order="3"` porque el menuitem existente `administracionSv-usuarios-menuitem` ocupa `order="3"` actualmente; debe reordenarse junto con los siguientes para evitar colisión, o asignar al nuevo menuitem un `order` libre dentro del rango. El implementador ajustará los `order` consecutivos.)

---

### Paso 7 — Seguridad

- **Control de acceso principal (V-026):** los dos menuitems (`administracionSv-importacion-menuitem` ya existente y `administracionSv-usuariosAutorizados-menuitem` nuevo) llevan `groups="admins"`. Esto cubre el acceso a los menús, los listados, el detalle de tarea, la descarga del fichero y la ejecución de nuevas importaciones.

- **Inmutabilidad por servidor (V-027, V-028):** se aplica en los servicios:
  - `TareaImportacionService.validateInsert/Update/Remove` rechaza siempre cualquier operación que no provenga del flujo `procesarImportacion`. La distinción se hace pasando un flag interno cuando el propio servicio inserta directamente con `repository.save(...)`.
  - `UsuarioAutorizadoService.validateInsert/Update/Remove` rechaza siempre. El servicio expone `insertarPorImportacion(...)` que persiste con `repository.save(...)` sin pasar por `validateInsert`.

- **Inmutabilidad por cliente:**
  - El grid de `TareaImportacion` lleva `canEdit="false"`, `canDelete="false"`; `canNew="true"` se permite porque el "+" abre el wizard, no un alta libre.
  - El grid de `UsuarioAutorizado` lleva `canNew="false"`, `canEdit="false"`, `canDelete="false"`.
  - El form de `TareaImportacion` lleva `canDelete="false"`, `canSave="false"`, `canCopy="false"`; las acciones de "guardado" se canalizan únicamente a través del botón Importar que llama al controlador.
  - El controlador construye la entidad con `AllowProperties` limitado a `tipoFichero` y `fichero`; el resto de campos son asignados en el servidor.

- **Permisos JPA opcionales (refuerzo):** si se desea blindar también la API REST de Axelor, se pueden añadir `Permission`/`Role` denegando `canWrite`/`canRemove` sobre `TareaImportacion` y `UsuarioAutorizado` para todos los roles. Es defensa en profundidad y no es estrictamente necesario porque los `validateInsert/Update/Remove` ya rechazan. **Si se opta por implementarlo**, se hace siguiendo el patrón de `k-seguridad/auth-task.md` en un fichero `data-init/input/auth-importacion.xml`. No se incluye en este diseño como obligatorio (alcance estricto, guía 11).

- **Filtrado:** los listados muestran todos los centros (visión global de admins, A11*). No se aplica filtro por `centro` ni por `centroActivo`.

---

### Paso 8 — Datos iniciales

No se requieren datos iniciales propios del subsistema `importacion`. Se asume que:

- Los `TipoUsuario` con códigos `PROFESOR`, `ALUMNO`, `FAMILIAR`, `PROFESOR_EXTERNO`, `EXPROFESOR`, `EXALUMNO`, `EXFAMILIAR` ya están precargados por el subsistema `common` (necesarios para que la actualización XML y CSV funcione). Verificar en `data-init` de `common` durante la implementación; si alguno falta, añadirlo allí (no en `importacion`).
- El grupo `admins` ya existe.

El enum `TipoFicheroImportacion` se materializa al generar el código del dominio; no requiere precarga.

---

### Paso 9 — Verificación final

Comando exacto desde la raíz del proyecto:

```bash
./gradlew clean build --info
```

Debe completar sin errores. Tras compilar correctamente, arrancar con:

```bash
./gradlew --no-daemon run --debug-jvm --port 8080 --context-path /
```

Comprobaciones manuales:

1. Loguearse como administrador. Verificar que en "Administración SV" aparecen los menús "Ficheros importación" y "Usuarios autorizados".
2. Loguearse con un usuario que no sea admin: ninguno de los dos menús aparece.
3. En "Ficheros importación", el grid muestra `canNew=true`. Pulsar "+" abre el `@Main-form` en modo wizard (id==null).
4. Pulsar "Importar" sin rellenar campos → action-validate del cliente muestra V-018 y V-020.
5. Subir un XML de profesores válido y coherente con el centro activo → grid muestra nueva fila con `estado=true`; al hacer clic en la fila se abre el detalle (id!=null) con todos los campos readonly y el log con el resumen y las trazas de actualización.
6. Subir el mismo XML otra vez → tarea con `estado=false` y log con motivo V-025.
7. Subir un XML con `<centro codigo>` distinto al centro activo → tarea con `estado=false` y motivo V-024.
8. Subir un XML malformado → tarea con `estado=false` y motivo V-021 (detalle del validador XSD).
9. Subir un CSV de PROFESOR_EXTERNO con DNIs válidos → tarea correcta; verificar en "Usuarios autorizados" que aparecen, y que los usuarios registrados afectados ahora tienen el tipo PROFESOR_EXTERNO (V-038 / V-039).
10. Intentar crear `TareaImportacion` o `UsuarioAutorizado` vía API REST (`POST /ws/rest/...`) → rechazado por `validateInsert` (V-027 / V-028).

---

## Matriz de trazabilidad V-XXX → ubicación

| V-XXX | Ubicación (clase.método o fichero+acción) | Capa | Comentario breve |
|---|---|---|---|
| V-001 | `TareaImportacion.xml` campo `fechaImportacion required="true"` + `TareaImportacionServiceImpl.validateInsert` | Dominio + Servidor | Required dominio + comprobación en validateInsert. |
| V-002 | `TareaImportacion.xml` campo `centro required="true"` + `TareaImportacionServiceImpl.validateInsert` | Dominio + Servidor | Required dominio + comprobación. |
| V-003 | `TareaImportacion.xml` campo `tipoFichero required="true"` + `TareaImportacionServiceImpl.validateInsert` | Dominio + Servidor | Required dominio + comprobación. |
| V-004 | `TareaImportacion.xml` campo `nombreFichero required="true"` + `TareaImportacionServiceImpl.validateInsert` | Dominio + Servidor | Servicio asigna desde `MetaFile.fileName`. |
| V-005 | `TareaImportacion.xml` campo `fichero required="true"` + `TareaImportacionServiceImpl.validateInsert` | Dominio + Servidor | Required dominio + comprobación. |
| V-006 | `TareaImportacion.xml` campo `usuarioImportador required="true"` + `TareaImportacionServiceImpl.procesarImportacion` (asignación desde `AuthUtils.getUser()`) | Dominio + Servidor | Servicio asigna automáticamente. |
| V-007 | `TareaImportacion.xml` campo `curso required="true"` + `TareaImportacionServiceImpl.procesarImportacion` (del ResultadoImportacion) | Dominio + Servidor | Servicio asigna desde resultado. |
| V-008 | `TareaImportacion.xml` campo `fechaExportacion required="true"` + `TareaImportacionServiceImpl.procesarImportacion` | Dominio + Servidor | Servicio asigna desde resultado. |
| V-009 | `TareaImportacion.xml` campo `estado required="true"` + `TareaImportacionServiceImpl.procesarImportacion` | Dominio + Servidor | Servicio asigna true/false. |
| V-010 | `TareaImportacion.xml` campo `log required="true"` + `TareaImportacionServiceImpl.procesarImportacion` | Dominio + Servidor | Servicio compone el log. |
| V-011 | `UsuarioAutorizado.xml` campo `centro required="true"` + `UsuarioAutorizadoServiceImpl.insertarPorImportacion` | Dominio + Servidor | Centro tomado del ResultadoImportacion. |
| V-012 | `UsuarioAutorizado.xml` campo `dni required="true"` + `UsuarioAutorizadoServiceImpl.insertarPorImportacion` | Dominio + Servidor | DNIs nunca null en el flujo. |
| V-013 | `UsuarioAutorizado.xml` campo `tipoUsuario required="true"` + `UsuarioAutorizadoServiceImpl.insertarPorImportacion` | Dominio + Servidor | TipoUsuario base resuelto por código. |
| V-014 | `UsuarioAutorizado.xml` campo `curso required="true"` | Dominio | Required dominio (modificado por esta iniciativa). |
| V-015 | `UsuarioAutorizado.xml` campo `fechaExportacion required="true"` | Dominio | Required dominio (cambio date→datetime). |
| V-016 | `UsuarioAutorizado.xml` `unique-constraint(centro,dni,tipoUsuario,curso,fechaExportacion)` + `UsuarioAutorizadoServiceImpl.insertarPorImportacion` (chequeo previo con `findByClave`) | Dominio + Servidor | Constraint BD + comprobación en servicio para anotar duplicado en log sin abortar. |
| V-017 | `ImportadorUsuarioXML.importar`, `ImportadorUsuarioCSV.importar` (`DniUtil.clean` + `DniUtil.isValid`); secundariamente `UsuarioAutorizadoServiceImpl.insertarPorImportacion` | Servidor | DNIs inválidos a `dnisInvalidos`; mensaje transmite el valor recibido. |
| V-018 | `views/TareaImportacion.xml` `Main-Local-validateImportar-action` (action-validate) | Cliente | `<error if="tipoFichero == null">`; el mensaje transmite que es obligatorio. |
| V-019 | `domains/TareaImportacion.xml` enum `TipoFicheroImportacion` + widget `SwitchSelect` en form; defensivo en `TareaImportacionServiceImpl.validarDTO` | Cliente (dominio finito) + Servidor | El enum garantiza el dominio finito. |
| V-020 | `views/TareaImportacion.xml` `Main-Local-validateImportar-action` (action-validate) | Cliente | `<error if="fichero == null">`. |
| V-021 | `ImportadorUsuarioXML.importar` (`XMLUtil.validarConSchema` → `ImportadorException`) | Servidor | Capturado por `TareaImportacionServiceImpl.procesarImportacion` → `persistirFallida` (V-030). |
| V-022 | `ImportadorUsuarioCSV.importar` (excepción de lectura UTF-8 / formato → `ImportadorException`) | Servidor | Capturado → `persistirFallida`. |
| V-023 | `ImportadorUsuarioXML.importar` (comprobación sección esperada vs `tipoFichero` → `ImportadorException`) | Servidor | Capturado → `persistirFallida`. Mensaje transmite tipo esperado vs sección encontrada. |
| V-024 | `ImportadorUsuarioXML.importar` (comparación `<centro codigo>` vs `centroActivo.code` → `ImportadorException`) | Servidor | Mensaje transmite código del fichero y código del centro activo. |
| V-025 | `TareaImportacionServiceImpl.procesarImportacion` (consulta `TareaImportacionRepository.findCorrectaByClave`) | Servidor | Mensaje transmite (centro, tipoFichero, curso, fechaExportacion). |
| V-026 | `menus.xml` (`groups="admins"` en ambos menuitems) | Seguridad | Acceso al menú. |
| V-027 | `TareaImportacionServiceImpl.validateInsert/validateUpdate/validateRemove` + `TareaImportacionController.importar` (`AllowProperties` restrictivo) + grid sin canEdit/canDelete + form `canDelete="false" canSave="false"` | Servidor + Cliente | Inmutabilidad de TareaImportacion. |
| V-028 | `UsuarioAutorizadoServiceImpl.validateInsert/validateUpdate/validateRemove` + grid `UsuarioAutorizado@Main-grid` con `canNew="false" canEdit="false" canDelete="false"` | Servidor + Cliente | Inmutabilidad de UsuarioAutorizado. |
| V-029 | `ImportadorUsuarioXML.importar` y `ImportadorUsuarioCSV.importar` (deduplicación + acumulación en `dnisDuplicadosIntraFichero`) | Servidor | Cada duplicado se anota en el log. |
| V-030 | `TareaImportacionServiceImpl.persistirFallida` (invocado tras V-018..V-025) | Servidor | Persiste tarea con estado=false y motivo en log; en transacción independiente. |
| V-031 | `TareaImportacionServiceImpl.procesarImportacion` (try/catch del bloque de modificaciones + rollback + `persistirFallida` en REQUIRES_NEW) | Servidor | Reversión por excepción técnica con persistencia del rastro. |
| V-032 | `ImportadorUsuarioXML.importar` y `ImportadorUsuarioCSV.importar` (tolerancia a 0 DNIs válidos) + `TareaImportacionServiceImpl.procesarImportacion` (rama "correcta con 0 importados") | Servidor | log transmite "0 usuarios importados". |
| V-033 | `ActualizadorTiposUsuarioRegistrados.aplicarParaXML` (rama Actual=No, Anterior=No) | Servidor | Defensivo: elimina T y EX_T. |
| V-034 | `ActualizadorTiposUsuarioRegistrados.aplicarParaXML` (rama Actual=No, Anterior=Sí) | Servidor | Añade EX_T y elimina T. |
| V-035 | `ActualizadorTiposUsuarioRegistrados.aplicarParaXML` (rama Actual=Sí, Anterior=No) | Servidor | Añade T y elimina EX_T. |
| V-036 | `ActualizadorTiposUsuarioRegistrados.aplicarParaXML` (rama Actual=Sí, Anterior=Sí) | Servidor | Añade T y elimina EX_T (idempotente). |
| V-037 | `ActualizadorTiposUsuarioRegistrados.aplicarParaXML` (invariante garantizado por V-033..V-036 + idempotencia de `CentroUsuarioService.añadirTipoUsuario`/`quitarTipoUsuario`) | Servidor | Mutua exclusión base↔EX. |
| V-038 | `ActualizadorTiposUsuarioRegistrados.aplicarParaCSV` (rama "User con CentroUsuario en centro activo" → `CentroUsuarioService.añadirTipoUsuario`) | Servidor | Añade PROFESOR_EXTERNO si no lo tenía. |
| V-039 | `ActualizadorTiposUsuarioRegistrados.aplicarParaCSV` (rama "User sin CentroUsuario en centro activo" → `CentroUsuarioService.crearCentroUsuarioConTipo`) | Servidor | Crea CentroUsuario y añade PROFESOR_EXTERNO. |
| V-040 | `ActualizadorTiposUsuarioRegistrados.aplicarParaXML` y `aplicarParaCSV` (rama "User no encontrado por DNI" → no actúa sobre registrados; A8* indica que tampoco se anota en log) | Servidor | Solo deja rastro en UsuarioAutorizado. |

### Trazabilidad de operaciones del análisis

| Op | Ubicación | Comentario |
|---|---|---|
| Op-1 Listar tareas | `subsysImportacion.TareaImportacion@Main-action` + `@Main-grid` | Grid solo lectura. |
| Op-2 Ver detalle | `subsysImportacion.TareaImportacion@Main-form` panel `showIf="id != null"` | Mismo form que el wizard. |
| Op-3 Descargar fichero | `subsysImportacion.TareaImportacion@Main-form` campo `fichero` `widget="binary-link"` (readonly) | Widget Axelor estándar. |
| Op-4 Iniciar y procesar importación | `TareaImportacionController.importar` → `TareaImportacionServiceImpl.procesarImportacion` | Flujo síncrono completo. |
| Op-5 Listar usuarios autorizados | `subsysImportacion.UsuarioAutorizado@Main-action` + `@Main-grid` | Grid solo lectura. |

---

## Notas de unificación

- **Enum en el mismo fichero de dominio.** El enum `TipoFicheroImportacion` se declara dentro del propio `TareaImportacion.xml`, no en un fichero aparte. Axelor lo soporta y el resultado es más cohesivo.
- **`tipoFichero` como `<enum>`, no como `<string selection=...>`.** Aprovecha el soporte nativo del framework para enums tipados y evita el doble nombrado.
- **Una sola clase colaboradora `ActualizadorTiposUsuarioRegistrados` con dos métodos** (`aplicarParaXML`, `aplicarParaCSV`) en lugar de dos clases separadas: la lógica común (resolución de User, manejo de log) cabe naturalmente; la guía 10 pide responsabilidades cohesivas, no la fragmentación máxima.
- **Inmutabilidad sin permisos JPA explícitos.** El bloqueo se hace a nivel de servicio (`validateInsert/Update/Remove` rechazan siempre) + UI (form/grid sin acciones de mutación) + controlador (`AllowProperties` restrictivo). Los permisos JPA adicionales son opcionales y se documentan como refuerzo.
- **Apertura del detalle tras importar:** `actionResponse.setView(ActionView.define(...).context("_showRecord", id))` reabre el mismo `@Main-form` con el id, y el form en modo detalle (`showIf="id != null"`) muestra los datos. No se usan popups separados (cumple guía 7.1).
- **Vistas en `subsystem/importacion/views/`** aunque `UsuarioAutorizado` esté en `registrousuario`. La UI pertenece al ámbito funcional del importador, y los nombres con prefijo `subsysImportacion` lo hacen explícito.
- **Comunicación entre subsistemas via servicios** (guía 9): `TareaImportacionServiceImpl` y `ActualizadorTiposUsuarioRegistrados` invocan `UsuarioAutorizadoService` y `CentroUsuarioService`, no sus repositorios. Los `JpaRepository.of(...)` directos quedan reservados para entidades de catálogo (`TipoUsuario`, `User`) sin servicio dedicado o para finders genéricos.
- **Sin servicio para `CentroUsuarioTipoUsuario`** (guía 8): es tabla de enlace puro; se gestiona desde `CentroUsuarioService`.
- **Sin módulos Guice para ModelService** (regla k-sistemas): `TareaImportacionServiceImpl`, `UsuarioAutorizadoServiceImpl` y `CentroUsuarioServiceImpl` viven en `service.impl.*ServiceImpl` y los descubre `ModelServiceFactory`.
- **Sin listeners JPA** para lógica de negocio (regla k-sistemas).

No se ha detectado ningún conflicto entre las guías de diseño y el análisis funcional.
