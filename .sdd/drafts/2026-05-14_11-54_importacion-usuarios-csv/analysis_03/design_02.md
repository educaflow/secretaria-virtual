---
type: design
---

# Diseño: Importación de usuarios autorizados desde CSV

**Objetivo:** Implementar la lógica CSV real en `ImportadorUsuarioCSV`, modificar `UsuarioAutorizado` para ampliar el unique-constraint con `curso` y cambiar `fechaExportacion` a `datetime`, crear `UsuarioAutorizadoService`/`UsuarioAutorizadoServiceImpl` para respetar la frontera entre subsistemas al crear entidades, y propagar centro/curso en los abortos globales (R-015).

**Capa:** `subsystem/importacion` (ampliación) · `subsystem/registrousuario` (nuevo servicio y repositorio) · `subsystem/common` (nuevo repositorio)

**Análisis de origen:** `.sdd/drafts/2026-05-14_11-54_importacion-usuarios-csv/analysis_03/analysis.md`

**Skills necesarios para la implementación:** `k-sistemas`, `k-validaciones`

---

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/registrousuario/domains/UsuarioAutorizado.xml` | Modificar | k-sistemas | `date`→`datetime` en `fechaExportacion`; unique-constraint ampliado a 4 campos con `curso` |
| `src/main/java/com/educaflow/subsystem/registrousuario/service/UsuarioAutorizadoService.java` | Crear | k-sistemas | Interfaz `ModelService<UsuarioAutorizado>` sin métodos adicionales |
| `src/main/java/com/educaflow/subsystem/registrousuario/service/impl/UsuarioAutorizadoServiceImpl.java` | Crear | k-sistemas + k-validaciones | `DefaultModelService<UsuarioAutorizado>` con `validateInsert` (V-001..V-004) |
| `src/main/java/com/educaflow/subsystem/common/db/repo/TipoUsuarioRepository.java` | Crear | k-sistemas | Extiende `AbstractTipoUsuarioRepository`; añade `findByCodigo(String)` → `Optional<TipoUsuario>` |
| `src/main/java/com/educaflow/subsystem/registrousuario/db/repo/UsuarioAutorizadoRepository.java` | Crear | k-sistemas | Extiende `AbstractUsuarioAutorizadoRepository`; añade `findByCentroDniTipoUsuarioCurso(...)` → `Optional<UsuarioAutorizado>` |
| `src/main/java/com/educaflow/subsystem/importacion/exception/ImportadorException.java` | Modificar | k-sistemas | Añade campos `Centro`/`Integer curso` opcionales, nuevos constructores y getters para R-015 |
| `src/main/java/com/educaflow/subsystem/importacion/importador/impl/ImportadorUsuarioCSV.java` | Modificar | k-sistemas | Implementa `importar()` con 6 métodos privados, `ContextoImportacion`, `EstadoProcesamiento` y `LineaResultado` |
| `src/main/java/com/educaflow/subsystem/importacion/service/impl/TareaImportacionServiceImpl.java` | Modificar | k-sistemas | Actualiza `fireActionRule_ejecutarImportacion`: propaga centro/curso desde `ImportadorException` en el catch (R-015) |

---

## Pasos

### Paso 1 — Modificar `UsuarioAutorizado.xml` (V-001, cambio de tipo)

**Fichero:** `src/main/java/com/educaflow/subsystem/registrousuario/domains/UsuarioAutorizado.xml`

Dos cambios sobre el estado actual:
1. `<date name="fechaExportacion" ...>` → `<datetime name="fechaExportacion" ...>`  
2. `<unique-constraint columns="centro,dni,tipoUsuario"/>` → `<unique-constraint columns="centro,dni,tipoUsuario,curso"/>`

**XML completo resultante:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<domain-models xmlns="http://axelor.com/xml/ns/domain-models"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://axelor.com/xml/ns/domain-models https://axelor.com/xml/ns/domain-models/domain-models_8.1.xsd">

    <module name="registro" package="com.educaflow.subsystem.registrousuario.db"/>

    <entity name="UsuarioAutorizado" repository="abstract">
        <many-to-one name="centro"      ref="com.educaflow.subsystem.common.db.Centro"      required="true"/>
        <string      name="dni"         title="dni__!!"                                     required="true"/>
        <many-to-one name="tipoUsuario" ref="com.educaflow.subsystem.common.db.TipoUsuario" required="true" title="Tipo de usuario"/>
        <integer     name="curso"/>
        <datetime    name="fechaExportacion" title="Fecha de exportación"/>
        <unique-constraint columns="centro,dni,tipoUsuario,curso"/>
    </entity>

</domain-models>
```

**Verificación:** `./gradlew clean build --info` compila sin errores. La clase generada `UsuarioAutorizado.java` tiene `LocalDateTime getFechaExportacion()` y la anotación `@UniqueConstraint` incluye los 4 campos.

---

### Paso 2 — Crear `UsuarioAutorizadoService` y `UsuarioAutorizadoServiceImpl` (V-001..V-004)

**Ficheros:**
- `src/main/java/com/educaflow/subsystem/registrousuario/service/UsuarioAutorizadoService.java`
- `src/main/java/com/educaflow/subsystem/registrousuario/service/impl/UsuarioAutorizadoServiceImpl.java`

**IMPORTANTE:** `ModelServiceFactory` descubre `UsuarioAutorizadoServiceImpl` automáticamente por estar en el paquete `service.impl.*ServiceImpl`. **No añadir ningún binding a `RegistroModule.java`** — hacerlo causaría un doble registro y rompería la factoría.

```java
// Interfaz — com.educaflow.subsystem.registrousuario.service.UsuarioAutorizadoService
public interface UsuarioAutorizadoService extends ModelService<UsuarioAutorizado> {
    // Sin métodos adicionales: insert/update/remove/validate* se heredan de ModelService
}
```

```java
// Implementación — com.educaflow.subsystem.registrousuario.service.impl.UsuarioAutorizadoServiceImpl
public class UsuarioAutorizadoServiceImpl
        extends DefaultModelService<UsuarioAutorizado>
        implements UsuarioAutorizadoService {

    // Constructor obligatorio — invocado por reflexión desde ModelServiceFactory
    public UsuarioAutorizadoServiceImpl(
            Class<UsuarioAutorizado> model,
            Repository<UsuarioAutorizado> repository)
    // → llama a super(model, repository)

    /**
     * Valida los campos obligatorios y la unicidad antes de insertar un UsuarioAutorizado.
     *
     * V-002: centro != null — mensaje: "El centro del usuario autorizado es obligatorio."
     * V-003: dni != null y no en blanco — mensaje: "El DNI del usuario autorizado es obligatorio."
     * V-004: tipoUsuario != null — mensaje: "El tipo de usuario del usuario autorizado es obligatorio."
     * V-001: comprueba que no exista otra fila con la misma combinación (centro, dni, tipoUsuario, curso)
     *        usando UsuarioAutorizadoRepository.findByCentroDniTipoUsuarioCurso.
     *        Solo se comprueba cuando los tres campos de V-002/V-003/V-004 están presentes (evita NPE).
     *        Mensaje: incluye dni, nombre del centro, nombre del tipoUsuario y curso.
     *
     * Acumula todos los errores antes de devolver (no falla en el primero).
     * Devuelve Optional.empty() si no hay errores; Optional.of(messages) si los hay.
     */
    @Override
    public Optional<BusinessMessages> validateInsert(UsuarioAutorizado entidad)
}
```

**Verificación:** compila sin errores. `Beans.get(ModelServiceFactory.class).resolve(UsuarioAutorizado.class)` devuelve una instancia sin lanzar excepción.

---

### Paso 3 — Crear `TipoUsuarioRepository` (R-004)

**Fichero:** `src/main/java/com/educaflow/subsystem/common/db/repo/TipoUsuarioRepository.java`

```java
// com.educaflow.subsystem.common.db.repo.TipoUsuarioRepository
public class TipoUsuarioRepository extends AbstractTipoUsuarioRepository {

    /**
     * Busca un TipoUsuario por su código de negocio (p.ej. "PROFESOR_EXTERNO").
     * Usado por ImportadorUsuarioCSV para resolver el mapeo enum→código (R-004, A1).
     *
     * JPQL: all().filter("self.codigo = :codigo").bind("codigo", codigo).fetchOne()
     * Devuelve Optional.empty() si no existe ningún TipoUsuario con ese código.
     */
    public Optional<TipoUsuario> findByCodigo(String codigo)
}
```

**Verificación:** compila sin errores. El cast `(TipoUsuarioRepository) JpaRepository.of(TipoUsuario.class)` o `Beans.get(TipoUsuarioRepository.class)` es resolvible.

---

### Paso 4 — Crear `UsuarioAutorizadoRepository` (R-010, R-011)

**Fichero:** `src/main/java/com/educaflow/subsystem/registrousuario/db/repo/UsuarioAutorizadoRepository.java`

```java
// com.educaflow.subsystem.registrousuario.db.repo.UsuarioAutorizadoRepository
public class UsuarioAutorizadoRepository extends AbstractUsuarioAutorizadoRepository {

    /**
     * Busca un UsuarioAutorizado por la combinación exacta que forma el unique-constraint.
     * Solo lectura — usar directamente desde importadores de otros subsistemas está permitido
     * (la restricción de "fronteras de subsistema" aplica solo a escrituras, no a lecturas).
     *
     * Usado por:
     *   - ImportadorUsuarioCSV.procesarLinea para R-010 (crear si no existe) / R-011 (ignorar si existe).
     *   - UsuarioAutorizadoServiceImpl.validateInsert para V-001 (unicidad).
     *
     * JPQL: all()
     *         .filter("self.centro = :centro AND self.dni = :dni
     *                  AND self.tipoUsuario = :tipoUsuario AND self.curso = :curso")
     *         .bind("centro", centro).bind("dni", dniNormalizado)
     *         .bind("tipoUsuario", tipoUsuario).bind("curso", curso)
     *         .fetchOne()
     * Devuelve Optional.empty() si no existe ninguna fila con esa combinación.
     */
    public Optional<UsuarioAutorizado> findByCentroDniTipoUsuarioCurso(
            Centro centro,
            String dniNormalizado,
            TipoUsuario tipoUsuario,
            Integer curso)
}
```

**Verificación:** compila sin errores. `Beans.get(UsuarioAutorizadoRepository.class)` es resolvible.

---

### Paso 5 — Modificar `ImportadorException`: añadir `Centro` y `Integer curso` para R-015

**Fichero:** `src/main/java/com/educaflow/subsystem/importacion/exception/ImportadorException.java`

Los dos constructores existentes se mantienen sin cambios. Se añaden dos campos opcionales y dos constructores nuevos:

```java
// com.educaflow.subsystem.importacion.exception.ImportadorException
public class ImportadorException extends Exception {

    /** null si el aborto ocurrió antes de determinar el centro (R-002). */
    private final Centro centro;

    /** null si el aborto ocurrió antes de determinar el curso (R-003). */
    private final Integer curso;

    // --- Constructores existentes (sin cambios, centro/curso quedan null) ---

    /** R-002: aborto sin contexto. */
    public ImportadorException(String message)

    /** Aborto con causa encadenada, sin centro/curso. */
    public ImportadorException(String message, Throwable cause)

    // --- Constructores nuevos ---

    /**
     * R-003: aborto con centro determinado pero sin curso.
     * R-004: aborto con centro y curso determinados.
     */
    public ImportadorException(String message, Centro centro, Integer curso)

    /**
     * R-005: aborto con centro y curso determinados + causa técnica encadenada.
     * Permite preservar la causa original (IOException, RuntimeException) para el log técnico.
     */
    public ImportadorException(String message, Centro centro, Integer curso, Throwable cause)

    // --- Getters ---

    /** @return el centro determinado antes del aborto, o null si no se llegó a determinar. */
    public Centro getCentro()

    /** @return el curso determinado antes del aborto, o null si no se llegó a determinar. */
    public Integer getCurso()
}
```

**Verificación:** compila sin errores. Los usos existentes de `new ImportadorException(msg)` y `new ImportadorException(msg, cause)` siguen compilando sin cambios.

---

### Paso 6 — Implementar `ImportadorUsuarioCSV` (R-001..R-013)

**Fichero:** `src/main/java/com/educaflow/subsystem/importacion/importador/impl/ImportadorUsuarioCSV.java`

**El constructor NO cambia:** `ImportadorUsuarioCSV(MetaFile fichero, TipoFicheroImportacion tipoFichero)`

#### Constante de mapeo explícito (A1)

```java
// Mapeo explícito enum PROFESOR_EXTERNO → código "PROFESOR_EXTERNO" (A1 del análisis)
private static final String CODIGO_TIPO_PROFESOR_EXTERNO = "PROFESOR_EXTERNO";
```

#### Método público — orquestador

```java
/**
 * Orquesta el proceso completo de importación CSV (R-001..R-014).
 *
 * Secuencia:
 *   1. resolverCentroActivo()                          → R-001 / R-002
 *   2. resolverCursoActivo(centro)                     → R-003
 *   3. resolverTipoUsuarioProfesorExterno(centro, curso) → R-004
 *   4. leerLineas(centro, curso)                       → R-005
 *   5. procesarLineas(lineas, ContextoImportacion)      → R-006..R-014
 *
 * Los pasos 1-4 lanzan ImportadorException en caso de aborto global.
 * El paso 5 procesa línea a línea sin abortar; los fallos individuales se acumulan.
 */
@Override
public ResultadoImportacion importar() throws ImportadorException
```

#### Métodos privados — resolución del contexto

```java
/**
 * Obtiene el centro activo del usuario autenticado (R-001).
 * Llama a AuthUtils.getUser().getCentroActivo().
 * R-002: si es null → lanza ImportadorException(mensaje) sin centro ni curso.
 */
private Centro resolverCentroActivo() throws ImportadorException

/**
 * Obtiene el curso activo del centro (R-003).
 * Llama a centro.getCurso().
 * R-003: si es null o 0 → lanza ImportadorException(mensaje, centro, null).
 */
private Integer resolverCursoActivo(Centro centro) throws ImportadorException

/**
 * Resuelve el TipoUsuario con código PROFESOR_EXTERNO desde BD (R-004).
 * Usa Beans.get(TipoUsuarioRepository.class).findByCodigo(CODIGO_TIPO_PROFESOR_EXTERNO).
 * R-004: si está vacío → lanza ImportadorException(mensaje, centro, curso).
 */
private TipoUsuario resolverTipoUsuarioProfesorExterno(Centro centro, Integer curso)
        throws ImportadorException

/**
 * Lee el contenido del CSV como lista de líneas en UTF-8 (R-005, R-006).
 * Usa MetaFileUtil.downloadContent(fichero) para obtener los bytes;
 * convierte a String con StandardCharsets.UTF_8 y divide por saltos de línea.
 * Las líneas vacías se incluyen en la lista (R-006: cuentan para la numeración física).
 * R-005: si descarga o conversión falla → lanza ImportadorException(mensaje, centro, curso, causa).
 */
private List<String> leerLineas(Centro centro, Integer curso) throws ImportadorException
```

#### Métodos privados — procesamiento de líneas

```java
/**
 * Itera sobre todas las líneas y acumula el estado de procesamiento (R-006..R-013).
 * Crea un EstadoProcesamiento, un ContextoImportacion y llama a procesarLinea por cada línea
 * con índice 1-based (R-006). Al terminar llama a construirResultado (R-013, R-014).
 */
private ResultadoImportacion procesarLineas(List<String> lineas, ContextoImportacion contexto)

/**
 * Procesa una única línea del CSV (R-007..R-012).
 * Devuelve un LineaResultado que procesarLineas aplica al EstadoProcesamiento.
 *
 * R-007: si lineaRaw.isBlank() → devuelve LineaResultado.silenciosa() (no cuenta en ningún contador).
 * R-008: dniLeido = lineaRaw.trim(); dniNorm = DniUtil.clean(dniLeido).
 *         dniLeido se usa en el log (A4: DNI leído del CSV, no el normalizado).
 * R-009: DniUtil.isValid(dniNorm) == false → devuelve LineaResultado.error(numLinea, dniLeido, "DNI no válido").
 * R-011: Beans.get(UsuarioAutorizadoRepository.class)
 *              .findByCentroDniTipoUsuarioCurso(ctx.centro(), dniNorm, ctx.tipoUsuario(), ctx.curso())
 *              .isPresent() → devuelve LineaResultado.ignorado(numLinea, dniLeido).
 * R-010: si no existe → construye UsuarioAutorizado (centro, dniNorm, tipoUsuario, curso,
 *         fechaExportacion=LocalDateTime.now()) y llama a
 *         Beans.get(ModelServiceFactory.class).resolve(UsuarioAutorizado.class).insert(ua).
 *         → devuelve LineaResultado.creado().
 * R-012: Exception inesperada → devuelve LineaResultado.error(numLinea, dniLeido,
 *         "Error inesperado: " + ex.getMessage()).
 */
private LineaResultado procesarLinea(String lineaRaw, int numeroLinea, ContextoImportacion contexto)

/**
 * Compone el ResultadoImportacion final (R-013, R-014).
 * Log: "Creados: N\nIgnorados: M\nErrores: P\n" + líneas de detalle de errores e ignorados
 * en orden de aparición. Las creaciones exitosas NO se listan.
 * Devuelve new ResultadoImportacion(estado.creados(), estado.errores(), log, ctx.centro(), ctx.curso()).
 */
private ResultadoImportacion construirResultado(EstadoProcesamiento estado, ContextoImportacion contexto)
```

#### Tipos internos privados

```java
/**
 * Contexto inmutable del proceso de importación.
 * Agrupa los 3 parámetros que viajan juntos a través de los métodos de procesamiento,
 * evitando listas de parámetros de longitud >3 (k-code-quality).
 */
private record ContextoImportacion(Centro centro, Integer curso, TipoUsuario tipoUsuario) {}

/**
 * Resultado de procesar una sola línea. Permite que procesarLinea sea una función pura
 * (sin efectos sobre estado mutable externo) con ≤3 parámetros (k-code-quality).
 *
 * Tipos:
 *   SILENCIOSA — línea en blanco (R-007): no cuenta en ningún contador.
 *   CREADO     — R-010: UsuarioAutorizado creado correctamente.
 *   IGNORADO   — R-011: combinación ya existente.
 *   ERROR      — R-009/R-012: DNI inválido o excepción inesperada.
 *
 * mensajeLog: presente (con número de línea, DNI leído y motivo) para IGNORADO y ERROR;
 *             absent para SILENCIOSA y CREADO (las creaciones no se listan en el log).
 */
private record LineaResultado(TipoLinea tipo, String mensajeLog) {

    enum TipoLinea { SILENCIOSA, CREADO, IGNORADO, ERROR }

    static LineaResultado silenciosa()                                      { ... }
    static LineaResultado creado()                                          { ... }
    static LineaResultado ignorado(int numLinea, String dniLeido)           { ... }
    static LineaResultado error(int numLinea, String dniLeido, String motivo) { ... }
}

/**
 * Acumulador mutable de contadores y log detallado durante el procesamiento.
 * Es una clase (no record) porque sus campos se modifican iterativamente.
 *
 * Campos: int creados, int ignorados, int errores, StringBuilder logDetalle.
 * Métodos:
 *   aplicar(LineaResultado) — incrementa el contador correcto y añade la línea al log si procede.
 *   componerLog()           — devuelve "Creados: N\nIgnorados: M\nErrores: P\n" + logDetalle.
 *   Getters: creados(), ignorados(), errores().
 */
private static final class EstadoProcesamiento { ... }
```

**Verificación:** compila sin errores. Tests manuales:
- CSV de 3 DNIs válidos nuevos: `resultado.usuariosImportados()==3`, log empieza por "Creados: 3".
- CSV con DNI inválido en línea 2: log contiene "Línea 2" y "DNI no válido".
- CSV con DNI duplicado: log contiene "Ya existe".
- Importador sin centro activo: `ImportadorException.getCentro()==null`.
- Centro sin curso activo: `ImportadorException.getCentro()!=null`, `getCurso()==null`.

---

### Paso 7 — Modificar `TareaImportacionServiceImpl`: R-015

**Fichero:** `src/main/java/com/educaflow/subsystem/importacion/service/impl/TareaImportacionServiceImpl.java`

Solo se modifica el bloque `catch` dentro de `fireActionRule_ejecutarImportacion`. El resto del método (bloque `try`, llamada a `fireActionRule_asignarCamposSistema`, `super.insert`) permanece sin cambios.

```java
/**
 * Actualización del bloque catch en fireActionRule_ejecutarImportacion.
 *
 * R-015: si ImportadorException lleva centro/curso determinados antes del aborto,
 *        se persisten en TareaImportacion (estado=false) para trazabilidad.
 *        fechaExportacion NO se establece (queda null) en abortos globales.
 *
 * El bloque try (éxito) permanece sin cambios.
 */
private void fireActionRule_ejecutarImportacion(TareaImportacion tareaImportacion)
// → en el catch(ImportadorException ex): añadir
//      if (ex.getCentro() != null) tareaImportacion.setCentro(ex.getCentro());
//      if (ex.getCurso() != null)  tareaImportacion.setCurso(ex.getCurso());
//   tras las líneas existentes de setEstado(false) y setLog(ex.getMessage()).
```

**Verificación:** importación abortada por R-003 (centro sin curso) persiste `TareaImportacion` con `estado=false`, `centro` relleno y `curso=null`. Importación abortada por R-002 (sin centro activo) persiste con `centro=null` y `curso=null`.

---

### Paso 8 — Verificación final

```bash
./gradlew clean build --info
```

Criterios:
1. BUILD SUCCESSFUL sin errores de compilación.
2. La clase generada `UsuarioAutorizado.java` tiene `LocalDateTime getFechaExportacion()` y `@UniqueConstraint` con 4 columnas.
3. `Beans.get(ModelServiceFactory.class).resolve(UsuarioAutorizado.class)` devuelve una instancia sin excepción.
4. `Beans.get(TipoUsuarioRepository.class)` es resolvible.
5. `Beans.get(UsuarioAutorizadoRepository.class)` es resolvible.

---

## Notas de implementación

- **Acceso a repositorios desde `ImportadorUsuarioCSV`**: usar `Beans.get(TipoUsuarioRepository.class)` y `Beans.get(UsuarioAutorizadoRepository.class)`. Son válidos desde clases no inyectadas por Guice porque Axelor registra automáticamente todos los repositorios concretos que extienden una clase abstracta generada.
- **`RegistroModule.java`**: no añadir ningún binding para `UsuarioAutorizadoService` ni `UsuarioAutorizadoServiceImpl`. El módulo contiene bindings para `RegistroService` (que no es ModelService); añadir `UsuarioAutorizadoServiceImpl` causaría un doble registro.
- **Curso como `Integer` vs `int`**: `Centro.getCurso()` devuelve `Integer` (puede ser null). `UsuarioAutorizado.getCurso()` devuelve `int` (primitivo, por lo que el valor 0 puede indicar "no asignado"). En `resolverCursoActivo`, tratar `null` o `0` como "sin curso activo" (R-003).
- **`MetaFileUtil.downloadContent`** lanza `RuntimeException` (no `IOException`) porque envuelve las excepciones internamente. El catch en `leerLineas` debe capturar al menos `RuntimeException`.
- **R-016** ya está implementado en `fireActionRule_asignarCamposSistema`; no tocar ese método.

---

## Matriz de trazabilidad

### Validaciones V-XXX

| Regla | Capa | Clase + Método / Fichero + Elemento |
|-------|------|--------------------------------------|
| V-001 — unicidad (centro, dni, tipoUsuario, curso) | Dominio XML | `UsuarioAutorizado.xml` — `<unique-constraint columns="centro,dni,tipoUsuario,curso"/>` |
| V-001 | Servidor | `UsuarioAutorizadoServiceImpl.validateInsert` — llama a `UsuarioAutorizadoRepository.findByCentroDniTipoUsuarioCurso`; mensajes incluyen dni, centro, tipoUsuario y curso |
| V-002 — centro obligatorio | Servidor | `UsuarioAutorizadoServiceImpl.validateInsert` — `entidad.getCentro() == null` |
| V-003 — dni obligatorio y no vacío | Servidor | `UsuarioAutorizadoServiceImpl.validateInsert` — `entidad.getDni() == null || entidad.getDni().isBlank()` |
| V-004 — tipoUsuario obligatorio | Servidor | `UsuarioAutorizadoServiceImpl.validateInsert` — `entidad.getTipoUsuario() == null` |

### Reglas de negocio R-XXX

| Regla | Capa | Clase + Método |
|-------|------|----------------|
| R-001 — determina centro activo del importador | Importador | `ImportadorUsuarioCSV.resolverCentroActivo()` — `AuthUtils.getUser().getCentroActivo()` |
| R-002 — sin centro activo → aborto | Importador | `ImportadorUsuarioCSV.resolverCentroActivo()` — lanza `ImportadorException(mensaje)` sin centro ni curso |
| R-003 — sin curso activo → aborto | Importador | `ImportadorUsuarioCSV.resolverCursoActivo(Centro)` — lanza `ImportadorException(mensaje, centro, null)` |
| R-004 — TipoUsuario no existe → aborto | Importador | `ImportadorUsuarioCSV.resolverTipoUsuarioProfesorExterno(Centro, Integer)` — `TipoUsuarioRepository.findByCodigo(CODIGO_TIPO_PROFESOR_EXTERNO).orElseThrow(...)` |
| R-005 — error de E/S → aborto | Importador | `ImportadorUsuarioCSV.leerLineas(Centro, Integer)` — catch de `RuntimeException` al llamar `MetaFileUtil.downloadContent`; lanza `ImportadorException(mensaje, centro, curso, causa)` |
| R-006 — numeración 1-based (líneas en blanco cuentan) | Importador | `ImportadorUsuarioCSV.procesarLineas` — bucle con índice `i+1` sobre la lista completa |
| R-007 — líneas vacías ignoradas silenciosamente | Importador | `ImportadorUsuarioCSV.procesarLinea` — `lineaRaw.isBlank()` → `LineaResultado.silenciosa()` |
| R-008 — normalización con DniUtil.clean | Importador | `ImportadorUsuarioCSV.procesarLinea` — `DniUtil.clean(lineaRaw.trim())` |
| R-009 — validación DNI; si inválido → error, continúa | Importador | `ImportadorUsuarioCSV.procesarLinea` — `DniUtil.isValid(dniNorm)` → `LineaResultado.error(numLinea, dniLeido, "DNI no válido")` |
| R-010 — si no existe → crear UsuarioAutorizado con fechaExportacion=now | Importador + Servicio | `ImportadorUsuarioCSV.procesarLinea` — `UsuarioAutorizadoRepository.findByCentroDniTipoUsuarioCurso(...).isEmpty()` → construye entidad y llama a `Beans.get(ModelServiceFactory.class).resolve(UsuarioAutorizado.class).insert(ua)` |
| R-011 — si existe → ignorado, log "Ya existe" | Importador | `ImportadorUsuarioCSV.procesarLinea` — `findByCentroDniTipoUsuarioCurso(...).isPresent()` → `LineaResultado.ignorado(numLinea, dniLeido)` |
| R-012 — excepción inesperada por línea → error, continúa | Importador | `ImportadorUsuarioCSV.procesarLinea` — `catch(Exception)` → `LineaResultado.error(numLinea, dniLeido, "Error inesperado: " + ex.getMessage())` |
| R-013 — log final con contadores + detalle | Importador | `ImportadorUsuarioCSV.construirResultado` — `EstadoProcesamiento.componerLog()` → "Creados: N\nIgnorados: M\nErrores: P\n" + detalles de errores e ignorados en orden |
| R-014 — CSV completado → estado=true, fechaExportacion=now | Servicio | `TareaImportacionServiceImpl.fireActionRule_ejecutarImportacion` — bloque try: `setEstado(true)`, `setFechaExportacion(now)`, `setLog("Importación finalizada. " + resultado.log())` (ya implementado) |
| R-015 — aborto global → estado=false; centro/curso si determinados | Servicio | `TareaImportacionServiceImpl.fireActionRule_ejecutarImportacion` — bloque catch: `setEstado(false)`, `setLog(ex.getMessage())`, `if (ex.getCentro() != null) setCentro(...)`, `if (ex.getCurso() != null) setCurso(...)` |
| R-016 — usuario=importador, fechaImportacion=now (ya implementado) | Servicio | `TareaImportacionServiceImpl.fireActionRule_asignarCamposSistema` — sin cambios |

### Reglas de UI U-XXX

*(ninguna en esta iniciativa — las vistas existentes no se modifican)*