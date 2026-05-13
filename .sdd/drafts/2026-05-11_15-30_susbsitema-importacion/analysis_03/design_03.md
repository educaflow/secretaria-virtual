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
| `src/main/java/com/educaflow/subsystem/importacion/service/ImportadorFicheroFactory.java` | Crear | k-sistemas | Clase utilidad con método `static crear(...)` que resuelve la implementación adecuada según `TipoFicheroImportacion`. |
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
| `src/main/java/com/educaflow/subsystem/common/service/TipoUsuarioService.java` | Crear | k-sistemas | Servicio `ModelService<TipoUsuario>` con `findByCodigo(String)`. Encapsula el acceso a TipoUsuario para los subsistemas consumidores (guía 9). |
| `src/main/java/com/educaflow/subsystem/common/service/impl/TipoUsuarioServiceImpl.java` | Crear | k-sistemas | Implementación que delega en `TipoUsuarioRepository.findByCodigo(...)`. |
| `src/main/java/com/educaflow/subsystem/common/service/UserService.java` | Crear | k-sistemas | Servicio `ModelService<User>` con `findByDni(String)`. Encapsula el acceso a `User` desde subsistemas externos (guía 9). |
| `src/main/java/com/educaflow/subsystem/common/service/impl/UserServiceImpl.java` | Crear | k-sistemas | Implementación que delega en `UserRepository.findByDni(...)`. |
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

Clase utilidad sin estado: no tiene campos de instancia, no accede a servicios ni repositorios, y solo opera sobre sus parámetros. Se modela con constructor privado y un único método `static crear(...)`. Los llamantes la invocan como `ImportadorFicheroFactory.crear(...)` sin instanciación ni `@Inject`.

```java
public final class ImportadorFicheroFactory {

    private ImportadorFicheroFactory();

    /**
     * Resuelve la implementación adecuada al tipoFichero:
     *   PROFESOR_XML / ALUMNO_XML / FAMILIAR_XML  -> ImportadorUsuarioXML(fichero, tipoFichero)
     *   PROFESOR_EXTERNO_CSV                      -> ImportadorUsuarioCSV(fichero, tipoFichero)
     *
     * Conforme a la guía 5, el constructor de cada implementación recibe únicamente `fichero` y `tipoFichero`.
     * El centro y el curso se obtienen del fichero (XML) o del contexto (`AuthUtils.getUser().getCentroActivo()`
     * en CSV) dentro de la propia implementación.
     *
     * Si tipoFichero es null o no está en los 4 valores -> IllegalArgumentException; el llamante
     * (TareaImportacionServiceImpl) lo traduce a BusinessMessages V-019 antes de invocar al importador.
     */
    public static ImportadorFichero crear(MetaFile fichero, TipoFicheroImportacion tipoFichero);
}
```

#### 3.6 `com.educaflow.subsystem.importacion.service.impl.ImportadorUsuarioXML`

```java
public class ImportadorUsuarioXML implements ImportadorFichero {

    private final MetaFile fichero;
    private final TipoFicheroImportacion tipoFichero;

    public ImportadorUsuarioXML(MetaFile fichero, TipoFicheroImportacion tipoFichero);

    /**
     * Orquesta la importación XML delegando cada paso a un método privado:
     *   1. validarContraXSD(...)           — V-021 (estructura XML conforme al XSD asociado).
     *   2. parsearDocumento(...)           — convierte el InputStream en Document DOM.
     *   3. verificarCoherenciaSeccion(...) — V-023 (la sección del fichero corresponde al tipoFichero).
     *   4. extraerAtributosCentro(...)     — codigo/curso/fechaExportacion del elemento <centro>.
     *   5. verificarCoincidenciaCentroActivo(...) — V-024.
     *   6. clasificarDnis(...)             — V-017 + V-029, particiona en válidos/inválidos/duplicados con streams.
     *   7. construye ResultadoImportacion con los datos extraídos y la clasificación de DNIs;
     *      el logParcial transmite un resumen numérico (cantidad de cada partición).
     *
     * Cualquier excepción de los pasos 1-5 es ImportadorException con motivo descriptivo (V-021..V-024).
     * V-032 (0 DNIs válidos) no es error — el resultado se devuelve igual con dnisValidos vacío.
     */
    @Override
    public ResultadoImportacion importar() throws ImportadorException;

    /**
     * V-021. Resuelve la ruta del XSD vía resolverRutaXSD(), abre el InputStream y delega en
     * XMLUtil.validarConSchema. Si el validador devuelve Optional<String> presente, lanza
     * ImportadorException("Formato XML inválido: " + detalle) transmitiendo el motivo concreto del validador.
     */
    private void validarContraXSD(InputStream xmlStream) throws ImportadorException;

    /**
     * Parsea el XML con DocumentBuilder y devuelve el Document. Si el parseo falla por motivo técnico
     * posterior al XSD (situación poco probable), lanza ImportadorException con la causa (cae bajo V-021).
     */
    private Document parsearDocumento(InputStream xmlStream) throws ImportadorException;

    /**
     * V-023. Comprueba que el elemento raíz contiene la sección esperada (resolverSeccionEsperada()) para
     * tipoFichero. Aunque el XSD pase, los XSD aceptan `<xs:any>` adicionales, así que la coherencia se
     * valida explícitamente. Si no coincide, lanza ImportadorException transmitiendo tipo esperado vs
     * sección encontrada.
     */
    private void verificarCoherenciaSeccion(Document documento) throws ImportadorException;

    /**
     * Extrae los atributos del elemento <centro>: codigo (String), curso (Integer.parseInt) y
     * fechaExportacion (parse con DateTimeFormatter "dd/MM/yyyy HH:mm:ss"). Si algún parseo falla,
     * lanza ImportadorException describiendo el motivo (cae bajo V-021).
     */
    private AtributosCentro extraerAtributosCentro(Document documento) throws ImportadorException;

    /**
     * V-024. Resuelve el centro activo del contexto (`AuthUtils.getUser().getCentroActivo()`) y compara
     * su `code` con el codigoFichero. Si difiere, lanza ImportadorException transmitiendo el código del
     * fichero y el código del centro activo.
     */
    private void verificarCoincidenciaCentroActivo(String codigoFichero) throws ImportadorException;

    /**
     * V-017 + V-029. Obtiene la NodeList de hijos de la sección esperada (resolverElementoHijoEsperado()),
     * la convierte en Stream<Element> (vía IntStream.range + NodeList.item) y la mapea con DniUtil.clean
     * sobre el atributo "documento". Particiona el stream en tres listas mediante operaciones funcionales:
     *  - filter(dni -> !DniUtil.isValid(dni)).collect(toList()) -> dnisInvalidos.
     *  - filter(DniUtil::isValid).collect(toList()) -> lista de candidatos válidos; sobre ella aplica
     *    distinct() y collect(toList()) para obtener dnisValidos (primera aparición preservada).
     *  - dnisDuplicadosIntraFichero se obtiene mediante un groupingBy(identity(), counting()) sobre los
     *    válidos y un filter(count > 1) para quedarse con los DNIs repetidos.
     * Devuelve un DnisClasificados inmutable con las tres listas resultantes.
     */
    private DnisClasificados clasificarDnis(Document documento);

    /** Mapeo tipoFichero -> ruta XSD ("/data-import/schemas/{profesores|alumnos|familiares}.xsd"). */
    private String resolverRutaXSD();

    /** Mapeo tipoFichero -> nombre del elemento sección esperado (docentes/alumnos/familiares). */
    private String resolverSeccionEsperada();

    /** Mapeo tipoFichero -> nombre del elemento hijo esperado (docente/alumno/familiar). */
    private String resolverElementoHijoEsperado();

    /** Estructura intermedia inmutable con los atributos del elemento raíz <centro>. */
    private record AtributosCentro(String codigo, Integer curso, LocalDateTime fechaExportacion) { }

    /** Estructura intermedia inmutable con la clasificación de DNIs tras leer el fichero. */
    private record DnisClasificados(List<String> dnisValidos,
                                    List<String> dnisInvalidos,
                                    List<String> dnisDuplicadosIntraFichero) { }
}
```

#### 3.7 `com.educaflow.subsystem.importacion.service.impl.ImportadorUsuarioCSV`

```java
public class ImportadorUsuarioCSV implements ImportadorFichero {

    private final MetaFile fichero;
    private final TipoFicheroImportacion tipoFichero;

    public ImportadorUsuarioCSV(MetaFile fichero, TipoFicheroImportacion tipoFichero);

    /**
     * Orquesta la importación CSV delegando cada paso a un método privado:
     *   1. leerLineas(...)             — lee el MetaFile como UTF-8 línea a línea (V-022 si falla la lectura).
     *   2. descartarCabeceraSiAplica() — A13*: la primera línea se descarta si no es DNI válido tras clean.
     *   3. clasificarDnis(...)         — V-017 + V-029, particiona con streams en válidos/inválidos/duplicados.
     *   4. resolverContextoCentroActivo() — obtiene Centro activo, curso y fechaExportacion = now() (no aplica V-024).
     *   5. construye ResultadoImportacion con los datos del contexto y la clasificación. logParcial transmite
     *      un resumen numérico y, en su caso, V-032 ("0 DNIs válidos").
     */
    @Override
    public ResultadoImportacion importar() throws ImportadorException;

    /**
     * V-022. Abre el MetaFile y devuelve la lista de líneas leídas como UTF-8 (no vacías tras trim).
     * Cualquier IOException o codificación inválida se traduce a ImportadorException con el motivo
     * técnico transmitido en el mensaje.
     */
    private List<String> leerLineas(MetaFile fichero) throws ImportadorException;

    /**
     * A13*. Si la primera línea no supera DniUtil.clean + DniUtil.isValid, se considera cabecera y se
     * elimina silenciosamente (sin anotar en log). Devuelve la lista restante.
     * La operación se expresa como filter(skip(1)) + ramo condicional según el resultado de isValid sobre
     * la primera línea limpia.
     */
    private List<String> descartarCabeceraSiAplica(List<String> lineas);

    /**
     * V-017 + V-029. Transforma la lista de líneas en un stream, aplica DniUtil.clean a cada elemento
     * y particiona el resultado:
     *  - filter(dni -> !DniUtil.isValid(dni)).collect(toList()) -> dnisInvalidos.
     *  - filter(DniUtil::isValid).distinct().collect(toList()) -> dnisValidos (primera aparición preservada).
     *  - groupingBy(identity(), counting()) sobre los válidos + filter(count > 1) -> dnisDuplicadosIntraFichero.
     * Devuelve un DnisClasificados inmutable.
     */
    private DnisClasificados clasificarDnis(List<String> lineas);

    /**
     * Resuelve el contexto del importador: centro activo (`AuthUtils.getUser().getCentroActivo()`),
     * curso del centro activo y fechaExportacion = LocalDateTime.now() (instante del procesado).
     * No valida coincidencia centro fichero (no aplica V-024 en CSV).
     */
    private ContextoCSV resolverContextoCentroActivo();

    /** Estructura intermedia inmutable con la clasificación de DNIs tras leer el fichero CSV. */
    private record DnisClasificados(List<String> dnisValidos,
                                    List<String> dnisInvalidos,
                                    List<String> dnisDuplicadosIntraFichero) { }

    /** Estructura intermedia inmutable con el contexto resuelto del importador CSV. */
    private record ContextoCSV(String centroCodigo, Integer curso, LocalDateTime fechaExportacion) { }
}
```

#### 3.8 `com.educaflow.subsystem.importacion.service.ActualizadorTiposUsuarioRegistrados`

Clase colaboradora (Guice-managed via `@Inject`) que aplica las reglas posteriores a la inserción de `UsuarioAutorizado`. Se justifica como clase aparte (guía 10) por la complejidad cohesiva de la matriz XML 2x2.

Conforme a la guía 9, esta clase NO accede a `UserRepository` ni a `TipoUsuarioRepository` directamente: delega en `UserService` (subsistema `common`) para resolver el `User` por DNI y en `CentroUsuarioService` para todas las operaciones de pertenencia y tipos. La resolución de `TipoUsuario` ya viene parametrizada (el servicio orquestador la realiza vía `TipoUsuarioService`).

```java
public class ActualizadorTiposUsuarioRegistrados {

    @Inject private ModelServiceFactory modelServiceFactory;
    // Resuelve CentroUsuarioService, UsuarioAutorizadoService y UserService desde modelServiceFactory.

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
     * Orquestación:
     *  1. construirUniversoDnis(...) -> Set<String> con la unión de DNIs del fichero y de los registrados base/EX.
     *  2. Para cada dni del universo, delega en actualizarTiposParaDni(...).
     */
    public void aplicarParaXML(Centro centro,
                               TipoUsuario tipoBase,
                               TipoUsuario tipoEx,
                               Integer curso,
                               LocalDateTime fechaExportacionFichero,
                               List<String> dnisFicheroValidos,
                               StringBuilder logBuilder);

    /**
     * Construye el universo de DNIs a recorrer aplicando una unión funcional:
     *  - dnisRegistradosBase = UsuarioAutorizadoService.findDnisByCentroTipoCurso(centro, tipoBase, curso)
     *  - dnisRegistradosEx   = UsuarioAutorizadoService.findDnisByCentroTipoCurso(centro, tipoEx,  curso)
     *  - Devuelve Stream.concat(Stream.concat(dnisFichero, dnisBase), dnisEx).collect(toSet()).
     * Se opera sobre DNIs porque User puede no existir y debe respetarse V-040.
     */
    private Set<String> construirUniversoDnis(Centro centro,
                                              TipoUsuario tipoBase,
                                              TipoUsuario tipoEx,
                                              Integer curso,
                                              List<String> dnisFicheroValidos);

    /**
     * Procesa un único DNI del universo XML:
     *  - Resuelve User vía UserService.findByDni(dni). Si Optional.empty -> V-040: anota la línea informativa
     *    en logBuilder y termina (no actúa sobre registrados).
     *  - Resuelve CentroUsuario vía CentroUsuarioService.findByCentroAndUser(centro, user). Si Optional.empty
     *    -> nota informativa y termina (no se crea CentroUsuario en XML).
     *  - Calcula actual = dnisFicheroValidos.contains(dni) (anyMatch sobre el stream del fichero).
     *  - Calcula anterior = calcularAnterior(centro, dni, tipoBase, curso, fechaExportacionFichero).
     *  - Delega en aplicarTabla2x2(cu, tipoBase, tipoEx, actual, anterior, logBuilder).
     */
    private void actualizarTiposParaDni(String dni,
                                        Centro centro,
                                        TipoUsuario tipoBase,
                                        TipoUsuario tipoEx,
                                        Integer curso,
                                        LocalDateTime fechaExportacionFichero,
                                        List<String> dnisFicheroValidos,
                                        StringBuilder logBuilder);

    /**
     * Determina si existe una exportación anterior para (centro, dni, tipoBase, curso) con fechaExportacion
     * estrictamente anterior a fechaExportacionFichero. Devuelve true si
     * UsuarioAutorizadoService.findUltimaFechaExportacion(...).isPresent() y la fecha obtenida es anterior.
     */
    private boolean calcularAnterior(Centro centro,
                                     String dni,
                                     TipoUsuario tipoBase,
                                     Integer curso,
                                     LocalDateTime fechaExportacionFichero);

    /**
     * Aplica las cuatro ramas (V-033..V-036) sobre el CentroUsuario y registra cada acción en logBuilder:
     *   (Actual=No, Anterior=No)  -> quitarTipoUsuario(cu, tipoBase) + quitarTipoUsuario(cu, tipoEx)  (V-033, defensivo)
     *   (Actual=No, Anterior=Sí)  -> añadirTipoUsuario(cu, tipoEx)   + quitarTipoUsuario(cu, tipoBase) (V-034)
     *   (Actual=Sí, Anterior=No)  -> añadirTipoUsuario(cu, tipoBase) + quitarTipoUsuario(cu, tipoEx)   (V-035)
     *   (Actual=Sí, Anterior=Sí)  -> añadirTipoUsuario(cu, tipoBase) + quitarTipoUsuario(cu, tipoEx)   (V-036)
     * Las operaciones añadir/quitar son idempotentes (CentroUsuarioService); tras cada par se garantiza la
     * mutua exclusión base ↔ EX (V-037).
     */
    private void aplicarTabla2x2(CentroUsuario centroUsuario,
                                 TipoUsuario tipoBase,
                                 TipoUsuario tipoEx,
                                 boolean actual,
                                 boolean anterior,
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
     * Orquestación: itera dnisValidos.stream().forEach(dni -> procesarDniCSV(...)).
     */
    public void aplicarParaCSV(Centro centro,
                               TipoUsuario tipoProfesorExterno,
                               List<String> dnisValidos,
                               StringBuilder logBuilder);

    /**
     * Procesa un único DNI del CSV:
     *  - Resuelve User vía UserService.findByDni(dni). Si Optional.empty -> V-040: no actúa, sin nota en log (A8*).
     *  - Si CentroUsuarioService.findByCentroAndUser(centro, user).isPresent() -> V-038:
     *      CentroUsuarioService.añadirTipoUsuario(cu, tipoProfesorExterno) (idempotente);
     *      anota la operación en logBuilder transmitiendo el dni.
     *  - Si findByCentroAndUser.isEmpty() -> V-039:
     *      CentroUsuarioService.crearCentroUsuarioConTipo(centro, user, tipoProfesorExterno);
     *      anota la operación en logBuilder transmitiendo el dni.
     */
    private void procesarDniCSV(String dni,
                                Centro centro,
                                TipoUsuario tipoProfesorExterno,
                                StringBuilder logBuilder);
}
```

#### 3.9 `com.educaflow.subsystem.importacion.service.TareaImportacionService` (interfaz)

```java
public interface TareaImportacionService extends ModelService<TareaImportacion> {

    /**
     * Op-4 completo. Punto de entrada del wizard.
     *
     * Orquestación de pasos (cada paso es un método privado de la implementación):
     *  1. validarDTO(dto) -> V-019 (defensivo; en flujo normal la action-validate del cliente filtra).
     *  2. ejecutarImportador(dto) -> ResultadoImportacion o persiste fallida y retorna id (V-021..V-024, V-030).
     *  3. resolverCentro(resultadoImportacion) -> Centro o persiste fallida y retorna id (V-024 defensivo).
     *  4. verificarImportacionPreviaCorrecta(centro, tipoFichero, curso, fechaExp)
     *     -> si existe, persiste fallida con motivo "ya existe una importación correcta previa" (V-025, V-030).
     *  5. resolverTipoUsuarioBase(tipoFichero) -> TipoUsuario (vía TipoUsuarioService, no JpaRepository) (V-013).
     *  6. insertarUsuariosAutorizados(...) -> aplica V-016 con anotación en log sin abortar.
     *  7. actualizarUsuariosRegistrados(...) -> dispatch XML/CSV al ActualizadorTiposUsuarioRegistrados.
     *  8. Si todos los pasos previos terminan sin excepción técnica: persistirCorrecta(...) y retorna id.
     *     V-032 cae aquí (dnisValidos vacío -> correcta con log "0 importados").
     *  9. Si en el paso 6 o 7 salta una RuntimeException técnica: marca rollback de la transacción del
     *     controller (revierte UsuarioAutorizado y cambios sobre CentroUsuario/CentroUsuarioTipoUsuario,
     *     V-031), persiste TareaImportacion fallida en REQUIRES_NEW y retorna id.
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

    @Inject private ActualizadorTiposUsuarioRegistrados actualizador;
    @Inject private ModelServiceFactory modelServiceFactory;
    // Resuelve CentroService, TipoUsuarioService y UsuarioAutorizadoService vía modelServiceFactory.
    // NO inyecta ImportadorFicheroFactory: se invoca como clase utilidad estática.
    // NO accede a JpaRepository.of(TipoUsuario.class) ni JpaRepository.of(User.class) — guía 9.

    public TareaImportacionServiceImpl(Class<TareaImportacion> model,
                                       Repository<TareaImportacion> repository) {
        super(model, repository);
    }

    /**
     * Implementación de procesarImportacion según el algoritmo descrito en la interfaz. La orquestación
     * encadena los métodos privados (validarDTO, ejecutarImportador, resolverCentro,
     * verificarImportacionPreviaCorrecta, resolverTipoUsuarioBase, insertarUsuariosAutorizados,
     * actualizarUsuariosRegistrados, persistirCorrecta, persistirFallida) componiendo el logBuilder
     * con secciones: resumen de lectura, resultado de inserción de UsuarioAutorizado, resultado de la
     * actualización de registrados, y conclusión final.
     *
     * Usa un flag interno (parámetro `boolean trusted=true` en crearYPersistir(...)) para pasar por
     * encima de validateInsert respecto a la regla V-027 cuando el flujo es legítimo.
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

    /**
     * Resuelve el ImportadorFichero vía `ImportadorFicheroFactory.crear(dto.fichero(), dto.tipoFichero())`
     * (invocación estática, sin @Inject). Invoca importador.importar() y devuelve el ResultadoImportacion.
     * Si lanza ImportadorException, persiste la tarea fallida con el motivo transmitido por el importador
     * (V-021..V-024 + V-030) y propaga un Optional.empty al llamante para que aborte. En este punto, si
     * todavía no se conoce el Centro real (excepción antes de obtener centroCodigo), se persiste la tarea
     * fallida con `centro = AuthUtils.getUser().getCentroActivo()` como mejor esfuerzo.
     */
    private Optional<ResultadoImportacion> ejecutarImportador(TareaImportacionInsertDTO dto);

    /**
     * Resuelve el Centro vía `CentroService.findByCodigo(resultado.centroCodigo())`. Si Optional.empty,
     * persiste la tarea fallida con motivo "centro inexistente" (V-024 defensivo) y devuelve Optional.empty
     * al llamante para que aborte.
     */
    private Optional<Centro> resolverCentro(ResultadoImportacion resultado, TareaImportacionInsertDTO dto);

    /**
     * V-025. Consulta `TareaImportacionRepository.findCorrectaByClave(centro, tipoFichero, fechaExp, curso)`.
     * Si la lista NO está vacía, persiste la tarea fallida con motivo que transmite la clave duplicada y
     * devuelve true para que el llamante aborte. Si la lista está vacía, devuelve false (continuar).
     */
    private boolean existeImportacionPreviaCorrecta(Centro centro,
                                                    TipoFicheroImportacion tipoFichero,
                                                    Integer curso,
                                                    LocalDateTime fechaExportacion);

    /**
     * Resuelve el TipoUsuario base T por código a partir de tipoFichero (PROFESOR_XML->PROFESOR,
     * ALUMNO_XML->ALUMNO, FAMILIAR_XML->FAMILIAR, PROFESOR_EXTERNO_CSV->PROFESOR_EXTERNO) delegando en
     * TipoUsuarioService.findByCodigo(codigo) (NO JpaRepository.of — guía 9). Si Optional.empty, lanza
     * RuntimeException (configuración inconsistente, no debería ocurrir por A12*). Cumple V-013.
     */
    private TipoUsuario resolverTipoUsuarioBase(TipoFicheroImportacion tipoFichero);

    /**
     * Resuelve el TipoUsuario contrapartida EX_T a partir del código base (PROFESOR->EXPROFESOR,
     * ALUMNO->EXALUMNO, FAMILIAR->EXFAMILIAR) vía TipoUsuarioService.findByCodigo(codigoEx). Solo se llama
     * para los tipos XML (no aplica a PROFESOR_EXTERNO_CSV).
     */
    private TipoUsuario resolverTipoUsuarioEx(TipoFicheroImportacion tipoFichero);

    /**
     * V-016. Recorre dnisValidos como stream y para cada dni delega en
     * UsuarioAutorizadoService.insertarPorImportacion(centro, dni, tipoBase, curso, fechaExp); si la
     * inserción devuelve BusinessMessages (duplicado preexistente), anota su mensaje en logBuilder sin
     * abortar. No usa `for` clásico: se expresa como dnisValidos.stream().forEach(...).
     */
    private void insertarUsuariosAutorizados(Centro centro,
                                             TipoUsuario tipoBase,
                                             Integer curso,
                                             LocalDateTime fechaExportacion,
                                             List<String> dnisValidos,
                                             StringBuilder logBuilder);

    /**
     * Dispatcha la actualización de registrados según tipoFichero:
     *  - XML: resuelve tipoEx y llama actualizador.aplicarParaXML(...).
     *  - CSV: llama actualizador.aplicarParaCSV(...) con tipoBase (PROFESOR_EXTERNO).
     */
    private void actualizarUsuariosRegistrados(TipoFicheroImportacion tipoFichero,
                                               Centro centro,
                                               TipoUsuario tipoBase,
                                               Integer curso,
                                               LocalDateTime fechaExportacion,
                                               List<String> dnisValidos,
                                               StringBuilder logBuilder);

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

#### 3.16 `com.educaflow.subsystem.common.service.TipoUsuarioService` (interfaz + impl)

Servicio del subsistema `common` que encapsula el acceso a `TipoUsuario` para que los subsistemas consumidores (`importacion`, `registrousuario`, etc.) no consulten `TipoUsuarioRepository` directamente (guía 9).

```java
public interface TipoUsuarioService extends ModelService<TipoUsuario> {

    /**
     * Devuelve el TipoUsuario cuyo `code` coincide con `codigo`, o Optional.empty si no existe.
     * Usado por TareaImportacionServiceImpl para resolver tipos base y EX a partir del enum
     * TipoFicheroImportacion (V-013).
     */
    Optional<TipoUsuario> findByCodigo(String codigo);
}
```

```java
public class TipoUsuarioServiceImpl
        extends DefaultModelService<TipoUsuario>
        implements TipoUsuarioService {

    public TipoUsuarioServiceImpl(Class<TipoUsuario> model, Repository<TipoUsuario> repository) {
        super(model, repository);
    }

    /** Cast a TipoUsuarioRepository (autogenerado/existente) y delega en su finder por código. */
    @Override
    public Optional<TipoUsuario> findByCodigo(String codigo);
}
```

#### 3.17 `com.educaflow.subsystem.common.service.UserService` (interfaz + impl)

Servicio del subsistema `common` que encapsula el acceso a `User` (la entidad de Axelor extendida en `common`) para que los subsistemas consumidores no consulten `UserRepository` directamente (guía 9).

```java
public interface UserService extends ModelService<User> {

    /**
     * Devuelve el User cuyo `dni` coincide con el parámetro (tras DniUtil.clean si procede),
     * o Optional.empty si no existe. Usado por ActualizadorTiposUsuarioRegistrados para resolver
     * el User asociado a cada DNI del universo. Cubre la rama V-040 (no encontrado).
     */
    Optional<User> findByDni(String dni);
}
```

```java
public class UserServiceImpl
        extends DefaultModelService<User>
        implements UserService {

    public UserServiceImpl(Class<User> model, Repository<User> repository) {
        super(model, repository);
    }

    /**
     * Cast a UserRepository (en `subsystem/common/db/repo`, ya creado en esta iniciativa) y delega
     * en su finder por DNI. Si la consulta devuelve null, retorna Optional.empty.
     */
    @Override
    public Optional<User> findByDni(String dni);
}
```

---

### Paso 4 — Repositorios

No se crean repositorios Java personalizados (todas las consultas necesarias caben en `<finder-method>` declarados en los dominios — paso 2):

- `TareaImportacionRepository` (generado por Axelor) expone `findCorrectaByClave(centro, tipoFichero, fechaExportacion, curso)`.
- `UsuarioAutorizadoRepository` (generado) expone `findByClave(...)`, `findUltimaFechaExportacion(...)`, `findDnisByCentroTipoCurso(...)`.
- `CentroRepository` (existente en `subsystem/common/db/repo`) expone `findByCodigo(...)`.
- `TipoUsuarioRepository` (existente en `subsystem/common/db/repo`) expone `findByCodigo(...)`.
- `UserRepository` (existente en `subsystem/common/db/repo`) expone `findByDni(...)`.
- `CentroUsuarioRepository` (generado) y `CentroUsuarioTipoUsuario` se acceden con `JpaRepository.of(...)` SOLO desde `CentroUsuarioServiceImpl` (mismo subsistema).

Acceso desde subsistemas externos (guía 9): todo acceso a entidades de `common` (`Centro`, `TipoUsuario`, `User`, `CentroUsuario`) desde `subsystem/importacion` o `subsystem/registrousuario` pasa obligatoriamente por su servicio correspondiente (`CentroService`, `TipoUsuarioService`, `UserService`, `CentroUsuarioService`). Ningún `JpaRepository.of(...)` para esas clases vive fuera de los `*ServiceImpl` de `common`.

Si en el momento de la implementación se detecta que falta algún finder, se añadirá un `<finder-method>` en el dominio correspondiente.

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
     * Orquesta tres pasos delegados a métodos privados:
     *  1. extraerDTO(actionRequest) -> TareaImportacionInsertDTO con tipoFichero y fichero del wizard, usando
     *     ActionRequestHelper y AllowProperties limitado a esos dos campos (bloquea V-027 si la UI manipula
     *     otros campos).
     *  2. tareaImportacionService.procesarImportacion(dto) -> id de la TareaImportacion (correcta o fallida).
     *  3. abrirDetalleTareaImportacion(actionResponse, id) -> reabre el mismo @Main-form en modo detalle
     *     (V-Op-2, A9*) para mostrar el detalle del registro recién creado.
     *
     * Si tareaImportacionService.procesarImportacion lanza RuntimeException no controlada (en flujo normal
     * nunca debería ocurrir — el servicio captura todo), responde con BusinessMessages como error transmitiendo
     * el motivo técnico.
     *
     * Cobertura: V-018/V-020 (vía action-validate), V-019 (vía servicio), V-021..V-040 (vía servicio).
     */
    @CallMethod
    @Transactional
    public void importar(ActionRequest actionRequest, ActionResponse actionResponse);

    /**
     * Construye el TareaImportacionInsertDTO a partir del ActionRequest. Crea un ActionRequestHelper
     * tipado a TareaImportacion, configura un AllowProperties restrictivo que solo permite los campos
     * `tipoFichero` y `fichero`, extrae el borrador de TareaImportacion con esa restricción y empaqueta
     * el DTO con los dos campos. Si la UI intenta enviar otros campos, AllowProperties los descarta antes
     * de llegar al servicio (V-027 defensivo).
     */
    private TareaImportacionInsertDTO extraerDTO(ActionRequest actionRequest);

    /**
     * Configura el ActionResponse para reabrir el form `subsysImportacion.TareaImportacion@Main-form`
     * en modo detalle con el id de la tarea creada. Incluye los view-params `show-toolbar-form=false` y
     * `forceEdit=true` y el contexto `_showRecord` con el id, para que el form renderice el panel
     * `showIf="id != null"`. (V-Op-2, A9*: mismo form para wizard y detalle.)
     */
    private void abrirDetalleTareaImportacion(ActionResponse actionResponse, Long idTareaImportacion);
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
| V-013 | `UsuarioAutorizado.xml` campo `tipoUsuario required="true"` + `TareaImportacionServiceImpl.resolverTipoUsuarioBase` (vía `TipoUsuarioService.findByCodigo`) + `UsuarioAutorizadoServiceImpl.insertarPorImportacion` | Dominio + Servidor | TipoUsuario base resuelto por código a través de servicio (guía 9). |
| V-014 | `UsuarioAutorizado.xml` campo `curso required="true"` | Dominio | Required dominio (modificado por esta iniciativa). |
| V-015 | `UsuarioAutorizado.xml` campo `fechaExportacion required="true"` | Dominio | Required dominio (cambio date→datetime). |
| V-016 | `UsuarioAutorizado.xml` `unique-constraint(centro,dni,tipoUsuario,curso,fechaExportacion)` + `UsuarioAutorizadoServiceImpl.insertarPorImportacion` (chequeo previo con `findByClave`) + `TareaImportacionServiceImpl.insertarUsuariosAutorizados` (anotación de duplicado en log) | Dominio + Servidor | Constraint BD + comprobación en servicio para anotar duplicado en log sin abortar. |
| V-017 | `ImportadorUsuarioXML.clasificarDnis`, `ImportadorUsuarioCSV.clasificarDnis` (`DniUtil.clean` + `DniUtil.isValid` con filter/collect); secundariamente `UsuarioAutorizadoServiceImpl.insertarPorImportacion` | Servidor | DNIs inválidos a `dnisInvalidos`; mensaje transmite el valor recibido. |
| V-018 | `views/TareaImportacion.xml` `Main-Local-validateImportar-action` (action-validate) | Cliente | `<error if="tipoFichero == null">`; el mensaje transmite que es obligatorio. |
| V-019 | `domains/TareaImportacion.xml` enum `TipoFicheroImportacion` + widget `SwitchSelect` en form; defensivo en `TareaImportacionServiceImpl.validarDTO` | Cliente (dominio finito) + Servidor | El enum garantiza el dominio finito. |
| V-020 | `views/TareaImportacion.xml` `Main-Local-validateImportar-action` (action-validate) | Cliente | `<error if="fichero == null">`. |
| V-021 | `ImportadorUsuarioXML.validarContraXSD` (`XMLUtil.validarConSchema` → `ImportadorException`) | Servidor | Capturado por `TareaImportacionServiceImpl.ejecutarImportador` → `persistirFallida` (V-030). |
| V-022 | `ImportadorUsuarioCSV.leerLineas` (excepción de lectura UTF-8 / formato → `ImportadorException`) | Servidor | Capturado por `TareaImportacionServiceImpl.ejecutarImportador` → `persistirFallida`. |
| V-023 | `ImportadorUsuarioXML.verificarCoherenciaSeccion` (comprobación sección esperada vs `tipoFichero` → `ImportadorException`) | Servidor | Capturado → `persistirFallida`. Mensaje transmite tipo esperado vs sección encontrada. |
| V-024 | `ImportadorUsuarioXML.verificarCoincidenciaCentroActivo` (comparación `<centro codigo>` vs `centroActivo.code` → `ImportadorException`) + `TareaImportacionServiceImpl.resolverCentro` (defensivo si codigo no existe) | Servidor | Mensaje transmite código del fichero y código del centro activo. |
| V-025 | `TareaImportacionServiceImpl.existeImportacionPreviaCorrecta` (consulta `TareaImportacionRepository.findCorrectaByClave`) | Servidor | Mensaje transmite (centro, tipoFichero, curso, fechaExportacion). |
| V-026 | `menus.xml` (`groups="admins"` en ambos menuitems) | Seguridad | Acceso al menú. |
| V-027 | `TareaImportacionServiceImpl.validateInsert/validateUpdate/validateRemove` + `TareaImportacionController.extraerDTO` (`AllowProperties` restrictivo) + grid sin canEdit/canDelete + form `canDelete="false" canSave="false"` | Servidor + Cliente | Inmutabilidad de TareaImportacion. |
| V-028 | `UsuarioAutorizadoServiceImpl.validateInsert/validateUpdate/validateRemove` + grid `UsuarioAutorizado@Main-grid` con `canNew="false" canEdit="false" canDelete="false"` | Servidor + Cliente | Inmutabilidad de UsuarioAutorizado. |
| V-029 | `ImportadorUsuarioXML.clasificarDnis` y `ImportadorUsuarioCSV.clasificarDnis` (deduplicación con distinct + groupingBy/filter para los repetidos) | Servidor | Cada duplicado se anota en el log. |
| V-030 | `TareaImportacionServiceImpl.persistirFallida` (invocado tras V-018..V-025) | Servidor | Persiste tarea con estado=false y motivo en log; en transacción independiente. |
| V-031 | `TareaImportacionServiceImpl.procesarImportacion` (try/catch del bloque de modificaciones + rollback + `persistirFallida` en REQUIRES_NEW) | Servidor | Reversión por excepción técnica con persistencia del rastro. |
| V-032 | `ImportadorUsuarioXML.importar` y `ImportadorUsuarioCSV.importar` (tolerancia a 0 DNIs válidos) + `TareaImportacionServiceImpl.procesarImportacion` (rama "correcta con 0 importados") | Servidor | log transmite "0 usuarios importados". |
| V-033 | `ActualizadorTiposUsuarioRegistrados.aplicarTabla2x2` (rama Actual=No, Anterior=No) | Servidor | Defensivo: elimina T y EX_T. |
| V-034 | `ActualizadorTiposUsuarioRegistrados.aplicarTabla2x2` (rama Actual=No, Anterior=Sí) | Servidor | Añade EX_T y elimina T. |
| V-035 | `ActualizadorTiposUsuarioRegistrados.aplicarTabla2x2` (rama Actual=Sí, Anterior=No) | Servidor | Añade T y elimina EX_T. |
| V-036 | `ActualizadorTiposUsuarioRegistrados.aplicarTabla2x2` (rama Actual=Sí, Anterior=Sí) | Servidor | Añade T y elimina EX_T (idempotente). |
| V-037 | `ActualizadorTiposUsuarioRegistrados.aplicarTabla2x2` (invariante garantizado por V-033..V-036 + idempotencia de `CentroUsuarioService.añadirTipoUsuario`/`quitarTipoUsuario`) | Servidor | Mutua exclusión base↔EX. |
| V-038 | `ActualizadorTiposUsuarioRegistrados.procesarDniCSV` (rama "User con CentroUsuario en centro activo" → `CentroUsuarioService.añadirTipoUsuario`) | Servidor | Añade PROFESOR_EXTERNO si no lo tenía. |
| V-039 | `ActualizadorTiposUsuarioRegistrados.procesarDniCSV` (rama "User sin CentroUsuario en centro activo" → `CentroUsuarioService.crearCentroUsuarioConTipo`) | Servidor | Crea CentroUsuario y añade PROFESOR_EXTERNO. |
| V-040 | `ActualizadorTiposUsuarioRegistrados.actualizarTiposParaDni` y `procesarDniCSV` (rama "UserService.findByDni == Optional.empty" → no actúa sobre registrados; A8* indica que tampoco se anota en log en CSV) | Servidor | Solo deja rastro en UsuarioAutorizado. |

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
- **`ImportadorFicheroFactory` como utilidad estática (no servicio).** Al no tener estado, dependencias inyectadas ni acceso a repositorios, se modela como `final class` con constructor privado y método `static crear(...)`. Los llamantes invocan `ImportadorFicheroFactory.crear(fichero, tipoFichero)` directamente, sin `@Inject` ni intermediario Guice.
- **Una sola clase colaboradora `ActualizadorTiposUsuarioRegistrados` con dos métodos públicos** (`aplicarParaXML`, `aplicarParaCSV`) descompuestos a su vez en métodos privados (`construirUniversoDnis`, `actualizarTiposParaDni`, `calcularAnterior`, `aplicarTabla2x2`, `procesarDniCSV`). La lógica común (resolución de User vía `UserService`, manejo del log) cabe naturalmente en la misma clase; la guía 10 pide responsabilidades cohesivas, no la fragmentación máxima.
- **Acceso a entidades de `common` solo a través de servicios (guía 9).** Se crean `TipoUsuarioService` y `UserService` además de `CentroService` y `CentroUsuarioService` para que ningún subsistema externo a `common` consulte `TipoUsuarioRepository` ni `UserRepository` directamente. `TareaImportacionServiceImpl.resolverTipoUsuarioBase/Ex` delega en `TipoUsuarioService`, y `ActualizadorTiposUsuarioRegistrados` resuelve el `User` por DNI vía `UserService`.
- **Inmutabilidad sin permisos JPA explícitos.** El bloqueo se hace a nivel de servicio (`validateInsert/Update/Remove` rechazan siempre) + UI (form/grid sin acciones de mutación) + controlador (`AllowProperties` restrictivo). Los permisos JPA adicionales son opcionales y se documentan como refuerzo.
- **Apertura del detalle tras importar:** el controller delega en el método privado `abrirDetalleTareaImportacion(actionResponse, id)` que configura el `ActionResponse` para reabrir el mismo `@Main-form` con el id, y el form en modo detalle (`showIf="id != null"`) muestra los datos. No se usan popups separados (cumple guía 7.1).
- **Vistas en `subsystem/importacion/views/`** aunque `UsuarioAutorizado` esté en `registrousuario`. La UI pertenece al ámbito funcional del importador, y los nombres con prefijo `subsysImportacion` lo hacen explícito.
- **Comunicación entre subsistemas via servicios** (guía 9): `TareaImportacionServiceImpl` y `ActualizadorTiposUsuarioRegistrados` invocan `UsuarioAutorizadoService`, `CentroService`, `CentroUsuarioService`, `TipoUsuarioService` y `UserService`. Los `JpaRepository.of(...)` directos quedan reservados a `CentroUsuarioServiceImpl` para gestionar `CentroUsuarioTipoUsuario` (tabla de enlace puro dentro del mismo subsistema, guía 8).
- **Sin servicio para `CentroUsuarioTipoUsuario`** (guía 8): es tabla de enlace puro; se gestiona desde `CentroUsuarioService`.
- **Sin módulos Guice para ModelService** (regla k-sistemas): `TareaImportacionServiceImpl`, `UsuarioAutorizadoServiceImpl`, `CentroServiceImpl`, `CentroUsuarioServiceImpl`, `TipoUsuarioServiceImpl` y `UserServiceImpl` viven en `service.impl.*ServiceImpl` y los descubre `ModelServiceFactory`.
- **Sin listeners JPA** para lógica de negocio (regla k-sistemas).
- **Uso de streams para operaciones sobre colecciones** (R-11). Los importadores XML y CSV particionan los DNIs leídos del fichero usando `filter`/`distinct`/`collect`/`groupingBy` en lugar de bucles imperativos. La unión del universo XML se obtiene con `Stream.concat(...).collect(toSet())`. Los recorridos con efectos secundarios sobre BD se expresan como `stream().forEach(...)` delegando cada elemento a un método privado descriptivo.

No se ha detectado ningún conflicto entre las guías de diseño y el análisis funcional.