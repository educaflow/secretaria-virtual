---
type: design
---

# Diseño: Importación de usuarios autorizados desde CSV

**Objetivo:** Implementar el cuerpo real de `ImportadorUsuarioCSV.importar()` para que el flujo `PROFESOR_EXTERNO` de `TareaImportacion` lea un CSV con un DNI por línea, valide cada DNI con dígito de control, cree `UsuarioAutorizado` para el centro y curso activos del importador, ignore duplicados y devuelva un `ResultadoImportacion` con contadores y log. Adicionalmente, ajustar el dominio `UsuarioAutorizado` para que la unicidad pase a `(centro, dni, tipoUsuario, curso)` y `fechaExportacion` sea de tipo `<datetime>`.

**Capa:** subsystem/importacion (ampliación) + subsystem/registrousuario (ajuste de dominio)

**Análisis de origen:** `.sdd/drafts/2026-05-14_11-54_importacion-usuarios-csv/analysis_03/analysis.md`

**Skills necesarios para la implementación:** `k-sistemas`, `k-validaciones`

---

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---|---|---|---|
| `src/main/java/com/educaflow/subsystem/registrousuario/domains/UsuarioAutorizado.xml` | Modificar | k-sistemas | Cambiar `<unique-constraint>` a `centro,dni,tipoUsuario,curso` (V-001), convertir `fechaExportacion` de `<date>` a `<datetime>` y añadir `<finder-method>` `findByCentroAndDniAndTipoUsuarioAndCurso` para localizar por la combinación exacta (R-010, R-011). |
| `src/main/java/com/educaflow/subsystem/importacion/importador/impl/ImportadorUsuarioCSV.java` | Modificar | k-sistemas, k-validaciones | Sustituir el `importar()` actual (que lanza `ImportadorException("@TODO: ...")`) por la lógica real: resolución de centro/curso/TipoUsuario, lectura del CSV en UTF-8, normalización y validación de DNI por línea, creación o ignorar según unicidad, composición del log con contadores y devolución del `ResultadoImportacion`. Mantiene constructor y campos existentes. |
| `src/main/java/com/educaflow/subsystem/importacion/db/TareaImportacion.xml` | Sin cambios | — | Entidad estable. |
| `src/main/java/com/educaflow/subsystem/importacion/service/impl/TareaImportacionServiceImpl.java` | Sin cambios | — | El `insert()` ya invoca `fireActionRule_asignarCamposSistema(t)` (R-016) y `fireActionRule_ejecutarImportacion(t)` (R-014 / R-015). |
| `src/main/java/com/educaflow/subsystem/importacion/controller/TareaImportacionController.java` | Sin cambios | — | Las validaciones de save existentes cubren la operación. |
| `src/main/java/com/educaflow/subsystem/importacion/views/TareaImportacion.xml` | Sin cambios | — | Vistas heredadas de la spec `0003_importacion-vistas`. |
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Sin cambios | — | El menuitem `administracionSv-importacion-menuitem` (groups="admins") ya da acceso. |
| Seguridad / Roles / MetaPermission | Sin cambios | — | El grupo `admins` ya tiene acceso a `TareaImportacion`. La creación de `UsuarioAutorizado` se hace vía `JpaRepository.save(...)` desde código de servidor. |
| Datos iniciales (`data-init`) | Sin cambios | — | `TipoUsuario(codigo='PROFESOR_EXTERNO')` se asume preexistente en el catálogo del subsistema `common`; si faltase, R-004 aborta con un mensaje claro. |
| `i18n_es.csv` / `i18n_ca.csv` | NO TOCAR | — | Generados automáticamente por el build. |

Decisiones arquitectónicas explícitas:

- **NO crear `UsuarioAutorizadoService` / `UsuarioAutorizadoServiceImpl`.** El uso de la entidad se limita a (a) localizar por combinación y (b) persistir nuevos registros. Las V-XXX las cubren los atributos declarativos del modelo (required + unique-constraint). No hay reglas de negocio R-XXX propias de `UsuarioAutorizado`: todas las R-XXX viven en el proceso de importación. Introducir un service sólo para envolver `save(...)` es ruido contra la guía de "alcance estricto" heredada de `0003_importacion-vistas`.
- **NO crear repositorio personalizado en `db/repo/UsuarioAutorizadoRepository.java`.** La única consulta necesaria se cubre con el `<finder-method>` declarado en el dominio (Axelor lo genera sobre el repositorio abstracto). Esto respeta la regla del proyecto "queries JPA en el repositorio, nunca inline en el servicio" sin añadir una clase vacía.
- **NO extender el record `ResultadoImportacion`.** La guía pide respetar la forma del resultado fijada por `0003_importacion-vistas`. El mapeo es: `usuariosImportados` ← creados; `numeroErrores` ← errores. Los `ignorados` no tienen columna propia: viven exclusivamente en el `log` textual (R-013, A7).
- **NO modificar el subsistema `importacion` (vistas, controlador, servicio).** Toda la lógica se concentra en `ImportadorUsuarioCSV`. El servicio existente ya orquesta `fireActionRule_asignarCamposSistema` (R-016) y `fireActionRule_ejecutarImportacion` (R-014 / R-015).

---

## Pasos

> Orden obligatorio aplicado (recursos → dominios → servicios → repositorios → controladores → vistas → seguridad → datos → verificación). En esta iniciativa sólo dominios y la clase `ImportadorUsuarioCSV` aportan cambios reales; el resto se marca explícitamente como "sin cambios".

### Paso 1 — Modificar el dominio `UsuarioAutorizado`

**Fichero:** `src/main/java/com/educaflow/subsystem/registrousuario/domains/UsuarioAutorizado.xml`
**Acción:** Modificar.

Tres cambios sobre el XML actual:

1. La `unique-constraint` pasa de `columns="centro,dni,tipoUsuario"` a `columns="centro,dni,tipoUsuario,curso"` (V-001 a nivel servidor declarativo).
2. El campo `fechaExportacion` pasa de `<date>` a `<datetime>` (coherente con `LocalDateTime.now()` usado en R-010 y R-014 y con el tipo de `TareaImportacion.fechaExportacion`).
3. Se añade un `<finder-method>` `findByCentroAndDniAndTipoUsuarioAndCurso` para que Axelor genere el método de búsqueda exacta por la cuádrupla en el repositorio abstracto, evitando filtros JPQL inline en `ImportadorUsuarioCSV`.

XML completo resultante:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<domain-models xmlns="http://axelor.com/xml/ns/domain-models"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://axelor.com/xml/ns/domain-models https://axelor.com/xml/ns/domain-models/domain-models_8.1.xsd">

    <module name="registro" package="com.educaflow.subsystem.registrousuario.db"/>

    <entity name="UsuarioAutorizado" repository="abstract">
        <many-to-one name="centro" ref="com.educaflow.subsystem.common.db.Centro" required="true"/>
        <string name="dni" title="dni__!!" required="true"/>
        <many-to-one name="tipoUsuario" ref="com.educaflow.subsystem.common.db.TipoUsuario" required="true" title="Tipo de usuario"/>
        <integer name="curso"/>
        <datetime name="fechaExportacion" title="Fecha de exportación"/>

        <unique-constraint columns="centro,dni,tipoUsuario,curso"/>

        <finder-method name="findByCentroAndDniAndTipoUsuarioAndCurso"
                       using="centro,dni,tipoUsuario,curso"/>
    </entity>

</domain-models>
```

Firma generada por Axelor en `AbstractUsuarioAutorizadoRepository` (esperada):

```java
public UsuarioAutorizado findByCentroAndDniAndTipoUsuarioAndCurso(
        com.educaflow.subsystem.common.db.Centro centro,
        String dni,
        com.educaflow.subsystem.common.db.TipoUsuario tipoUsuario,
        Integer curso);
```

Devuelve la instancia que cumple la combinación exacta o `null` si no existe. Es el método que invoca `ImportadorUsuarioCSV` para R-010 y R-011.

Notas:

- `repository="abstract"` se mantiene. `JpaRepository.of(UsuarioAutorizado.class)` resuelve la clase concreta generada por Axelor.
- El cambio de `unique-constraint` es estrictamente menos restrictivo (A5 del análisis): los datos existentes son compatibles. No requiere migración de datos.
- El cambio `<date>` → `<datetime>` modifica el tipo Java generado de `LocalDate` a `LocalDateTime` para `fechaExportacion`. En el flujo de importación siempre se rellena con `LocalDateTime.now()` (R-010).
- No se crea `i18n_es.csv` ni `i18n_ca.csv` (los regenera el build).

Verificación específica del paso:

- `./gradlew clean build --info` regenera `AbstractUsuarioAutorizadoRepository` con el nuevo finder.
- El log de arranque de la aplicación muestra la actualización del índice único `(centro, dni, tipoUsuario, curso)`.

### Paso 2 — Modificar `ImportadorUsuarioCSV`

**Fichero:** `src/main/java/com/educaflow/subsystem/importacion/importador/impl/ImportadorUsuarioCSV.java`
**Acción:** Modificar (reemplazar el cuerpo placeholder por la implementación real).

**FQN de la clase:** `com.educaflow.subsystem.importacion.importador.impl.ImportadorUsuarioCSV`
**Implementa:** `com.educaflow.subsystem.importacion.importador.ImportadorFichero`

**Imports relevantes (orientativos):**

- `com.axelor.auth.AuthUtils`
- `com.axelor.auth.db.User`
- `com.axelor.db.JpaRepository`
- `com.axelor.meta.MetaFiles` (o `com.educaflow.base.util.MetaFileUtil`, según convención del proyecto)
- `com.axelor.meta.db.MetaFile`
- `com.educaflow.base.util.DniUtil`
- `com.educaflow.subsystem.common.db.Centro`
- `com.educaflow.subsystem.common.db.TipoUsuario`
- `com.educaflow.subsystem.importacion.db.TipoFicheroImportacion`
- `com.educaflow.subsystem.importacion.exception.ImportadorException`
- `com.educaflow.subsystem.importacion.importador.ImportadorFichero`
- `com.educaflow.subsystem.importacion.importador.ResultadoImportacion`
- `com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado`
- `java.nio.charset.StandardCharsets`
- `java.nio.file.Files`, `java.nio.file.Path`
- `java.time.LocalDateTime`
- `java.util.ArrayList`, `java.util.EnumMap`, `java.util.List`, `java.util.Map`

#### 2.1 Cabecera, campos y constantes

```java
public class ImportadorUsuarioCSV implements ImportadorFichero {

    // Mapeo explícito enum TipoFicheroImportacion → código de TipoUsuario en BD (A1, R-004).
    // Se documenta como tabla; sólo contiene PROFESOR_EXTERNO en esta iniciativa, se ampliará
    // explícitamente cuando se admitan más tipos importables por CSV.
    private static final Map<TipoFicheroImportacion, String> MAPEO_CODIGO_TIPO_USUARIO = ... ;

    // Codificación esperada del CSV (A2).
    private static final java.nio.charset.Charset CHARSET = StandardCharsets.UTF_8;

    // Campos existentes (no cambian).
    private final MetaFile fichero;
    private final TipoFicheroImportacion tipoFichero;

    // Constructor existente (firma exacta, no cambia).
    public ImportadorUsuarioCSV(MetaFile fichero, TipoFicheroImportacion tipoFichero);
    // Comentario: asigna los dos campos finales. No realiza I/O ni validación.

    // ...
}
```

#### 2.2 Tipos auxiliares privados

```java
// Acumulador mutable de contadores durante el procesamiento del CSV.
private static final class Contadores {
    int creados;
    int ignorados;
    int errores;
}

// Entrada de detalle del log para errores e ignorados; mantiene orden de aparición.
private record EntradaLog(int numeroLinea, String dniLeido, String motivo) {}
```

#### 2.3 Método público `importar()`

```java
@Override
public ResultadoImportacion importar() throws ImportadorException;
```

Comentario descriptivo del cuerpo (qué hace, qué reglas aplica, qué llamadas hace):

- Orquesta el flujo completo:
  1. Resuelve el `Centro` a usar llamando a `obtenerCentroACUsarOLanzar()` (R-001). Si no hay centro activo, propaga `ImportadorException` (R-002).
  2. Resuelve el curso a usar llamando a `obtenerCursoACUsarOLanzar(centro)` (R-003). Si el centro no tiene curso, propaga `ImportadorException`.
  3. Resuelve el `TipoUsuario` destino llamando a `resolverTipoUsuarioOLanzar()` (R-004). Si no existe en BD el registro con código `"PROFESOR_EXTERNO"`, propaga `ImportadorException`.
  4. Lee las líneas del CSV llamando a `leerLineasCsv()` (R-005). Cualquier fallo de I/O o decodificación se envuelve en `ImportadorException` con causa.
  5. Inicializa una instancia de `Contadores`, una `List<EntradaLog>` vacía `incidencias` (mantiene orden de aparición; mezcla errores e ignorados) y recorre las líneas con índice 1-based (R-006). Para cada `String linea`:
     - Si `linea == null` o `linea.trim()` está vacía, la salta sin tocar contadores ni log (R-007); el contador de línea avanza.
     - En otro caso invoca `procesarLinea(numeroLinea, linea, centro, curso, tipoUsuario, contadores, incidencias)`.
  6. Compone el log final llamando a `componerLog(contadores, incidencias)` (R-013).
  7. Devuelve `new ResultadoImportacion(contadores.creados, contadores.errores, log, centro, curso)`. Mapeo justificado: `usuariosImportados=creados`, `numeroErrores=errores`. El conteo de `ignorados` queda solo en el log textual (no se extiende el record).
- Para R-014: el servicio existente `TareaImportacionServiceImpl.fireActionRule_ejecutarImportacion` recibe este `ResultadoImportacion`, asigna `estado=true`, copia `centro`, `curso`, compone el log final con prefijo "Importación finalizada. " y asigna `fechaExportacion = LocalDateTime.now()`.
- Para R-015: cuando `importar()` lanza `ImportadorException`, el mismo método del servicio existente captura la excepción y persiste la `TareaImportacion` con `estado=false`, sin `fechaExportacion`, y `log = ex.getMessage()`. `centro` y `curso` quedan según lo que se haya rellenado antes del fallo (en los abortos globales actuales, ninguno se asigna a la `TareaImportacion` porque `importar()` no devuelve resultado).

#### 2.4 Método privado `obtenerCentroACUsarOLanzar()`

```java
private Centro obtenerCentroACUsarOLanzar() throws ImportadorException;
```

Comentario:

- Obtiene el `User` actual con `AuthUtils.getUser()` y devuelve `user.getCentroActivo()` si no es `null`.
- Si es `null`, lanza `ImportadorException` cuyo mensaje transmite: "la importación se aborta porque el importador no tiene centro activo asignado" (texto exacto a juicio del implementador siempre que respete el contenido descrito en el análisis para R-002).
- Cubre R-001 (resolución del centro a usar) y R-002 (aborto global si falta centro activo).

#### 2.5 Método privado `obtenerCursoACUsarOLanzar(Centro)`

```java
private Integer obtenerCursoACUsarOLanzar(Centro centro) throws ImportadorException;
```

Comentario:

- Devuelve `centro.getCurso()` si no es `null`.
- Si es `null`, lanza `ImportadorException` cuyo mensaje transmite: que la importación se aborta porque el centro recibido (`centro.getName()`) no tiene curso activo asignado. El mensaje incluye el nombre del centro, conforme a R-003.
- Cubre R-003.

#### 2.6 Método privado `resolverTipoUsuarioOLanzar()`

```java
private TipoUsuario resolverTipoUsuarioOLanzar() throws ImportadorException;
```

Comentario:

- Consulta el mapa `MAPEO_CODIGO_TIPO_USUARIO` por `this.tipoFichero` para obtener el código (constante `"PROFESOR_EXTERNO"` en esta iniciativa, A1). Si la entrada falta (caso futuro de tipos no mapeados), lanza `ImportadorException` describiendo error de configuración del importador para ese tipo de fichero.
- Con el código obtenido, busca el `TipoUsuario` correspondiente a través del repositorio (`JpaRepository.of(TipoUsuario.class).all().filter("self.codigo = :codigo").bind("codigo", codigo).fetchOne()` o el finder generado si lo hubiera). Esta query, aunque inline, opera sobre el repositorio (no sobre el servicio del subsistema actual) y es la única salida razonable dado que `TipoUsuario` se busca por código fuera del ámbito del propio repositorio personalizado de `UsuarioAutorizado`.
- Si el resultado es `null`, lanza `ImportadorException` cuyo mensaje transmite: que la importación se aborta como error técnico de configuración porque no existe en BD el `TipoUsuario` con código `"PROFESOR_EXTERNO"`.
- Cubre R-004 y A1.

#### 2.7 Método privado `leerLineasCsv()`

```java
private List<String> leerLineasCsv() throws ImportadorException;
```

Comentario:

- Resuelve la ruta física del `MetaFile` mediante la utilidad estándar del proyecto (`MetaFiles.getPath(this.fichero)` o `MetaFileUtil` equivalente).
- Lee todas las líneas con `Files.readAllLines(path, CHARSET)`. La elección de `StandardCharsets.UTF_8` es estricta (A2) y conserva el orden físico, incluidas las líneas en blanco (necesario para R-006).
- Captura las excepciones de E/S y de decodificación (`MalformedInputException`, `UnmappableCharacterException`, `IOException`, etc.) y las convierte en `ImportadorException(message, cause)` cuyo mensaje transmite: que no se ha podido leer el fichero CSV, incluyendo el motivo recuperado de la causa. Conforme a R-005.
- Cubre R-005 y A2.

#### 2.8 Método privado `procesarLinea(...)`

```java
private void procesarLinea(
        int numeroLinea,
        String lineaCruda,
        Centro centro,
        Integer curso,
        TipoUsuario tipoUsuario,
        Contadores contadores,
        List<EntradaLog> incidencias);
```

Comentario (este método NO lanza nunca al exterior; cualquier excepción inesperada cae en R-012):

- Envuelve todo el cuerpo en `try { ... } catch (Exception ex) { ... }` para soporte de R-012. En el `catch`, incrementa `contadores.errores`, calcula `dniLeido = (lineaCruda != null ? lineaCruda.trim() : "")` y añade a `incidencias` un `EntradaLog(numeroLinea, dniLeido, "Error inesperado: " + ex.getMessage())`. No relanza, no aborta el bucle del `importar()`.
- En el `try`:
  1. Calcula `dniLeido = lineaCruda.trim()` (forma "leída" tras trim, A4 — es lo que se mostrará al usuario en el log para errores e ignorados).
  2. Normaliza con `DniUtil.clean(dniLeido)` obteniendo `dniNormalizado` (R-008). El persistido en `UsuarioAutorizado.dni` es siempre el normalizado.
  3. Valida con `DniUtil.isValid(dniNormalizado)`. Si devuelve `false`:
     - Incrementa `contadores.errores`.
     - Añade a `incidencias` un `EntradaLog(numeroLinea, dniLeido, "DNI no válido")` (R-009).
     - Retorna sin tocar BD.
  4. Si es válido, consulta preexistencia con `JpaRepository.of(UsuarioAutorizado.class).findByCentroAndDniAndTipoUsuarioAndCurso(centro, dniNormalizado, tipoUsuario, curso)` (finder generado en Paso 1).
     - Si el resultado **no es null** (R-011): incrementa `contadores.ignorados`, añade a `incidencias` un `EntradaLog(numeroLinea, dniLeido, "Ya existe")`. No modifica el registro encontrado. Retorna.
     - Si el resultado **es null** (R-010): instancia un nuevo `UsuarioAutorizado`, asigna `centro`, `dni = dniNormalizado`, `tipoUsuario`, `curso` y `fechaExportacion = LocalDateTime.now()`, y lo persiste con `JpaRepository.of(UsuarioAutorizado.class).save(nuevo)`. Incrementa `contadores.creados`. NO añade entrada al log (las creaciones no se listan, R-013).
- Cubre R-006 (recibe `numeroLinea` 1-based desde `importar()`), R-008, R-009, R-010, R-011 y R-012.

> Nota: aunque el código JpaRepository expone también `.findBy...` directamente, la firma del `<finder-method>` declarado en el Paso 1 es la que asegura una query con nombre fuera del importador (cumple "queries en repositorio, no inline en el servicio"). El cast implícito al repositorio concreto generado por Axelor (`UsuarioAutorizadoRepository`) se obtiene a través de `JpaRepository.of(UsuarioAutorizado.class)`; si el contrato del repositorio devuelto requiere un cast explícito, el implementador puede usar `Beans.get(UsuarioAutorizadoRepository.class)` (clase generada por Axelor a partir del `repository="abstract"`). La firma del finder es invariante: `findByCentroAndDniAndTipoUsuarioAndCurso(Centro, String, TipoUsuario, Integer)`.

#### 2.9 Método privado `componerLog(...)`

```java
private String componerLog(Contadores contadores, List<EntradaLog> incidencias);
```

Comentario:

- Construye una cadena multilínea en este orden estricto (R-013):
  1. "Creados: {contadores.creados}".
  2. "Ignorados: {contadores.ignorados}".
  3. "Errores: {contadores.errores}".
  4. A continuación, una línea por cada `EntradaLog` de `incidencias` en orden de aparición original en el CSV (la lista las recibió secuencialmente desde `procesarLinea`). Cada entrada transmite: "nº línea {numeroLinea}: '{dniLeido}': {motivo}". El formato literal queda a juicio del implementador siempre que el contenido informativo (número de línea + DNI leído + motivo) se respete.
- Devuelve la cadena resultante. Separador: salto de línea (`\n`).
- Cubre R-013. A7 se respeta: el log es un único campo de texto multilínea.

#### 2.10 Notas técnicas adicionales

- `ImportadorUsuarioCSV` se instancia desde `ImportadorFicheroFactory.create(...)`, no es un bean Guice. No requiere `@Inject`.
- No se requiere `@Transactional` en esta clase: el método `insert()` del servicio se ejecuta dentro de la transacción Axelor abierta por el flujo de save; las llamadas a `.save(...)` se persisten en esa misma transacción.
- Para R-015 no se requiere código adicional: la propagación de `ImportadorException` desde los puntos R-002/R-003/R-004/R-005 ya es interceptada por `fireActionRule_ejecutarImportacion` del servicio existente, que deja `estado=false`, sin `fechaExportacion` y `log = ex.getMessage()`. `centro` y `curso` no llegan a la `TareaImportacion` en los abortos globales actuales porque `importar()` lanza antes de devolver `ResultadoImportacion`.
- Para R-016 no se requiere código adicional: `TareaImportacionServiceImpl.fireActionRule_asignarCamposSistema` ya asigna `usuario = AuthUtils.getUser()` y `fechaImportacion = LocalDateTime.now()` antes de invocar al importador.

### Paso 3 — Repositorios / Servicios

Sin cambios. No se crean `UsuarioAutorizadoService`/`UsuarioAutorizadoServiceImpl` ni un `UsuarioAutorizadoRepository` personalizado en `db/repo/`. El `<finder-method>` del Paso 1 cubre la única consulta requerida; la persistencia se hace con `JpaRepository.of(UsuarioAutorizado.class).save(...)`. Razón: alcance estricto heredado de `0003_importacion-vistas`.

### Paso 4 — Controladores

Sin cambios. `TareaImportacionController.validateSave(actionRequest, actionResponse)` ya está implementado y cubre la validación previa al save de `TareaImportacion`. No se introducen nuevos métodos. Los parámetros `actionRequest`/`actionResponse` siguen la convención del proyecto.

### Paso 5 — Vistas

Sin cambios. Las vistas existentes (`subsysImportacion.TareaImportacion@Main-grid`, `subsysImportacion.TareaImportacion@Main-form`, `subsysImportacion.TareaImportacion@Main-action` en `src/main/java/com/educaflow/subsystem/importacion/views/TareaImportacion.xml`) cubren la operación de crear una `TareaImportacion` con tipo `PROFESOR_EXTERNO` y de consultar su log. El campo `fechaExportacion` ya se muestra como datetime conforme al cambio del dominio.

### Paso 6 — Menús

Sin cambios. El único punto de entrada es `administracionSv-importacion-menuitem` con `groups="admins"`, ya declarado en el fichero único de menús `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml`. No se añade ni modifica ningún `<menuitem>`.

### Paso 7 — Seguridad

Sin cambios. El acceso a la operación está gobernado por el grupo `admins` del menuitem heredado. La creación de `UsuarioAutorizado` se hace desde código de servidor con `JpaRepository.save(...)` y no requiere nuevos permisos.

### Paso 8 — Datos iniciales (`data-init`)

Sin cambios. La iniciativa asume que `TipoUsuario(codigo='PROFESOR_EXTERNO')` ya existe en el catálogo del subsistema `common`. Si no estuviese, R-004 produce un aborto global con un mensaje explícito.

### Paso 9 — Verificación final

Comando de compilación:

```bash
./gradlew clean build --info
```

Comprobaciones esperadas:

1. Build sin errores. Axelor regenera `AbstractUsuarioAutorizadoRepository` con el método `findByCentroAndDniAndTipoUsuarioAndCurso(Centro, String, TipoUsuario, Integer)` y con `fechaExportacion` tipada como `LocalDateTime`.
2. Tras arrancar la aplicación, el log muestra la actualización del índice único `(centro, dni, tipoUsuario, curso)` (Axelor/Hibernate gestiona el DDL diferencial).
3. `ImportadorFicheroFactory.create(PROFESOR_EXTERNO, fichero)` sigue devolviendo `ImportadorUsuarioCSV` con su comportamiento ya real.

Smoke tests funcionales (manuales, no automatizados en esta iniciativa):

1. Camino feliz. Subir un CSV con: (a) un DNI válido nuevo, (b) un DNI válido ya existente para la misma combinación (centro/dni/tipoUsuario/curso), (c) un DNI inválido, (d) una línea en blanco, (e) una línea con sólo espacios. Resultado esperado:
   - `TareaImportacion.estado = true`, `fechaExportacion` rellena.
   - `centro` y `curso` = los del importador.
   - `log` empieza con tres líneas "Creados: 1", "Ignorados: 1", "Errores: 1" y luego dos entradas (una de error con "DNI no válido" y una de ignorado con "Ya existe") en orden de aparición. Las líneas en blanco y con sólo espacios no aparecen en el log.
   - Existe en BD un `UsuarioAutorizado` con el DNI normalizado y `fechaExportacion` reciente.
2. Repetir la misma importación. Resultado esperado: todos los DNIs nuevos del primer pase pasan a ignorados; los inválidos siguen como errores; las líneas vacías siguen sin contar.
3. Aborto: importador sin `centroActivo` → `estado=false`, `fechaExportacion` nula, log con el mensaje de R-002.
4. Aborto: centro con `curso` nulo → `estado=false`, log con el mensaje de R-003 incluyendo el nombre del centro.
5. Aborto: catálogo sin `TipoUsuario(codigo='PROFESOR_EXTERNO')` → `estado=false`, log con el mensaje de R-004.
6. Aborto: fichero CSV en otra codificación (p. ej. ISO-8859-1 con caracteres no decodificables como UTF-8) → `estado=false`, log con el mensaje de R-005 y el motivo de la decodificación.

---

## Matriz de trazabilidad

### Validaciones V-XXX

| Regla | Capa | Ubicación | Comentario |
|-------|------|-----------|------------|
| V-001 (unicidad ampliada por curso) | modelo (servidor declarativo) | `domains/UsuarioAutorizado.xml` → `<unique-constraint columns="centro,dni,tipoUsuario,curso"/>` | Garantiza la unicidad de la cuádrupla a nivel BBDD. El importador la respeta proactivamente consultando el `<finder-method>` antes de crear (R-010 / R-011), por lo que en el flujo CSV nunca se llega a violar el constraint. Mensaje informativo (cuando lo viola otra vía): debe transmitir DNI recibido, centro, tipoUsuario y curso. |
| V-002 (centro obligatorio) | modelo (servidor declarativo) | `domains/UsuarioAutorizado.xml` → `<many-to-one name="centro" ... required="true"/>` | Obligatoriedad ya presente en el modelo. En el flujo de importación, `centro` se asigna siempre desde `resolverContexto` (R-001), por lo que el constraint no se activa desde este flujo. Mensaje: debe transmitir que el centro del usuario autorizado es obligatorio. |
| V-003 (dni obligatorio) | modelo (servidor declarativo) | `domains/UsuarioAutorizado.xml` → `<string name="dni" ... required="true"/>` | Obligatoriedad ya presente en el modelo. En el flujo, las líneas vacías se descartan (R-007) y las inválidas no llegan a crearse (R-009), por lo que el `dni` persistido nunca es null ni vacío. Mensaje: debe transmitir que el DNI del usuario autorizado es obligatorio. |
| V-004 (tipoUsuario obligatorio) | modelo (servidor declarativo) | `domains/UsuarioAutorizado.xml` → `<many-to-one name="tipoUsuario" ... required="true"/>` | Obligatoriedad ya presente en el modelo. En el flujo, se asigna siempre el `TipoUsuario` resuelto por código `"PROFESOR_EXTERNO"` (R-004). Mensaje: debe transmitir que el tipo de usuario del usuario autorizado es obligatorio. |

### Reglas de negocio R-XXX

| Regla | Capa | Ubicación | Comentario |
|-------|------|-----------|------------|
| R-001 (centro a usar = centro activo del importador) | servidor | `ImportadorUsuarioCSV.obtenerCentroACUsarOLanzar()` | Lee `AuthUtils.getUser().getCentroActivo()` y devuelve el centro a usar. |
| R-002 (aborto global si no hay centro activo) | servidor | `ImportadorUsuarioCSV.obtenerCentroACUsarOLanzar()` | Si el centro activo es `null`, lanza `ImportadorException` cuyo mensaje transmite que la importación se aborta porque el importador no tiene centro activo asignado. `TareaImportacionServiceImpl.fireActionRule_ejecutarImportacion` (existente) persiste `estado=false` y `log = ex.getMessage()` (R-015). |
| R-003 (aborto global si el centro no tiene curso) | servidor | `ImportadorUsuarioCSV.obtenerCursoACUsarOLanzar(Centro)` | Si `centro.getCurso()` es `null`, lanza `ImportadorException` cuyo mensaje transmite que la importación se aborta porque el centro indicado (incluyendo su nombre) no tiene curso activo asignado. |
| R-004 (resolución de TipoUsuario por código; aborto si no existe) | servidor | `ImportadorUsuarioCSV.resolverTipoUsuarioOLanzar()` + constante `MAPEO_CODIGO_TIPO_USUARIO` | Mapea el enum `TipoFicheroImportacion` al `codigo` del `TipoUsuario` (PROFESOR_EXTERNO → "PROFESOR_EXTERNO", A1). Si no existe en BD, lanza `ImportadorException` cuyo mensaje transmite que la importación se aborta por error de configuración: no existe el `TipoUsuario` con código `"PROFESOR_EXTERNO"`. |
| R-005 (aborto global si CSV no se puede leer) | servidor | `ImportadorUsuarioCSV.leerLineasCsv()` | Lee con `Files.readAllLines(path, StandardCharsets.UTF_8)`. Cualquier `IOException` / `MalformedInputException` / `UnmappableCharacterException` se traduce en `ImportadorException(message, cause)` cuyo mensaje transmite que no se ha podido leer el fichero CSV con el motivo recuperado de la causa. |
| R-006 (numeración 1-based, líneas blancas cuentan para numeración) | servidor | `ImportadorUsuarioCSV.importar()` (bucle de procesamiento) | Recorre la lista devuelta por `leerLineasCsv()` con índice `i` desde 0; el número de línea pasado a `procesarLinea` es `i + 1`. La lista contiene también las líneas en blanco. |
| R-007 (líneas vacías o sólo espacios se ignoran silenciosamente) | servidor | `ImportadorUsuarioCSV.importar()` (filtro previo a `procesarLinea`) | Si `linea == null` o `linea.trim()` está vacío, no se invoca `procesarLinea`: ni cuenta ni se añade al log; el número de línea avanza por estar en el bucle. |
| R-008 (normalización de DNI con `DniUtil.clean` antes de validar/usar) | servidor | `ImportadorUsuarioCSV.procesarLinea(...)` (sub-fase de normalización) | Aplica `DniUtil.clean(dniLeido)`; el valor persistido en `UsuarioAutorizado.dni` es siempre `dniNormalizado`. |
| R-009 (validación con `DniUtil.isValid`; si inválido, error con DNI leído) | servidor | `ImportadorUsuarioCSV.procesarLinea(...)` (sub-fase de validación) | Si `DniUtil.isValid(dniNormalizado)` es `false`, incrementa `contadores.errores` y añade a `incidencias` un `EntradaLog(numeroLinea, dniLeido, "DNI no válido")`. `dniLeido` es la forma original tras trim (A4), no la normalizada. No corta el bucle. |
| R-010 (crear nuevo `UsuarioAutorizado` si no existe la combinación) | servidor | `ImportadorUsuarioCSV.procesarLinea(...)` (rama "finder devuelve null") + `<finder-method>` generado + `JpaRepository.of(UsuarioAutorizado.class).save(...)` | Si `findByCentroAndDniAndTipoUsuarioAndCurso(centro, dniNormalizado, tipoUsuario, curso)` devuelve `null`, crea instancia con esos cuatro campos + `fechaExportacion = LocalDateTime.now()` y la persiste. Incrementa `contadores.creados`. No añade entrada al log. |
| R-011 (ignorar si existe la combinación) | servidor | `ImportadorUsuarioCSV.procesarLinea(...)` (rama "finder devuelve no-null") | Si el finder devuelve un registro no nulo, incrementa `contadores.ignorados` y añade `EntradaLog(numeroLinea, dniLeido, "Ya existe")`. No modifica el registro. |
| R-012 (excepción inesperada por línea → error y continuar) | servidor | `ImportadorUsuarioCSV.procesarLinea(...)` (try/catch externo) | Bloque `try/catch (Exception ex)` envolviendo todo el cuerpo. En el `catch`, incrementa `contadores.errores`, calcula `dniLeido` y añade `EntradaLog(numeroLinea, dniLeido, "Error inesperado: " + ex.getMessage())`. No relanza. |
| R-013 (composición del log final con contadores y detalle) | servidor | `ImportadorUsuarioCSV.componerLog(Contadores, List<EntradaLog>)` | Construye la cadena con cabecera "Creados/Ignorados/Errores" y a continuación las incidencias (errores e ignorados mezclados) en orden de aparición. Las creaciones no se listan. |
| R-014 (procesamiento completado: estado=true + fechaExportacion + log) | servidor | `TareaImportacionServiceImpl.fireActionRule_ejecutarImportacion(...)` (existente, no se modifica) | Cuando `importar()` retorna `ResultadoImportacion` sin lanzar, el servicio existente asigna `estado=true`, `centro=resultado.centro()`, `curso=resultado.curso()`, `fechaExportacion=LocalDateTime.now()` y `log="Importación finalizada. " + resultado.log()`. Sólo se trazabiliza. |
| R-015 (aborto global: estado=false, sin fechaExportacion, log con causa) | servidor | `TareaImportacionServiceImpl.fireActionRule_ejecutarImportacion(...)` (catch `ImportadorException`, existente) | Cuando `importar()` lanza `ImportadorException`, el servicio existente asigna `estado=false` y `log = ex.getMessage()`. `fechaExportacion` queda nula (la rama del catch no la asigna). `centro` y `curso` quedan nulos porque los abortos globales ocurren antes de devolver `ResultadoImportacion`. Sólo se trazabiliza. |
| R-016 (asignación de usuario y fechaImportacion al crear `TareaImportacion`) | servidor | `TareaImportacionServiceImpl.fireActionRule_asignarCamposSistema(...)` (existente, no se modifica) | Asigna `usuario = AuthUtils.getUser()` y `fechaImportacion = LocalDateTime.now()` antes de ejecutar la importación. Sólo se trazabiliza. |

### Reglas de UI U-XXX

Sin entradas (la iniciativa no añade reglas de UI).

---

## Notas de unificación

- **Repositorio personalizado de `UsuarioAutorizado`: NO se crea.** Decisión consensuada por mayoría de propuestas y respaldada por las guías. El `<finder-method>` en el dominio cubre la única consulta requerida y respeta la regla "queries en repositorio, no inline". Si en el futuro aparecen otras consultas o filtros propios, se introducirá entonces un repositorio personalizado en `db/repo/UsuarioAutorizadoRepository.java` heredando del abstracto generado.
- **Servicio `UsuarioAutorizadoService`: NO se crea.** No hay R-XXX propias de la entidad `UsuarioAutorizado`. Toda la lógica del proceso vive en `ImportadorUsuarioCSV`. Promover la entidad a `ModelService` se hará cuando aparezcan operaciones explícitas sobre ella (alta manual, edición, baja con reglas).
- **Mapeo del record `ResultadoImportacion`: explícito y documentado.** `usuariosImportados = creados`, `numeroErrores = errores`. El conteo `ignorados` queda exclusivamente en el `log` textual (cabecera "Ignorados: N" + entradas individuales con motivo "Ya existe"). Se respeta así la "forma del resultado" fijada por `0003_importacion-vistas`.
- **Mapeo enum→código de TipoUsuario: como tabla estática.** Se introduce `MAPEO_CODIGO_TIPO_USUARIO` (`Map<TipoFicheroImportacion, String>`) como punto único de mapeo. Conforme a A1, se documenta como tabla; en esta iniciativa solo contiene la entrada `PROFESOR_EXTERNO → "PROFESOR_EXTERNO"`. Se ampliará explícitamente cuando se admitan más tipos importables por CSV.
- **Tipos auxiliares privados (`Contadores`, `EntradaLog`).** Acotan la firma de los métodos internos sin exponer API nueva fuera del importador. `Contadores` es una clase privada mutable; `EntradaLog` es un record privado.
- **Orden del log final.** Errores e ignorados se mezclan en orden de aparición en el CSV (no se separan en bloques). Las creaciones no se listan. El análisis dice "una línea por cada error e ignorado, en orden de aparición"; esa lectura es la unificada.
- **Lectura del CSV.** `Files.readAllLines(path, StandardCharsets.UTF_8)` para conservar el orden físico y respetar la codificación esperada (A2). `MalformedInputException` y `UnmappableCharacterException` se tratan como fallo de R-005 (no se decodifica con `REPLACE`).
- **Transaccionalidad.** No se añade `@Transactional` en el importador: el `insert` del servicio padre ya abre transacción Axelor y los `.save(...)` se persisten en esa misma transacción.

---

## Conflictos detectados con guías

Sin conflictos.

- Se respeta íntegramente la spec previa `0003_importacion-vistas`: no se altera `TareaImportacion`, el enum `TipoFicheroImportacion`, `ImportadorException`, la interfaz `ImportadorFichero`, la factoría `ImportadorFicheroFactory` ni el record `ResultadoImportacion` (no se extiende).
- No se modifican vistas, menús, controlador ni servicio del subsistema `importacion`.
- No se crean módulos Guice para `ModelService`.
- No se crean listeners JPA para lógica de negocio.
- No se crean ficheros `i18n_es.csv` / `i18n_ca.csv`.
- No se referencia código de `expedientes`/`tiposexpedientes`/`tramites`.
- Los parámetros del controlador existente siguen siendo `actionRequest`/`actionResponse` (no se modifica).
- Los `<menuitem>` siguen en el fichero único `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` (no se añade ni modifica ninguno).
- Cada `<action-view>` existente sigue en su propio fichero (no se introduce ningún `<action-view>` nuevo).

El cambio `<date>` → `<datetime>` en `UsuarioAutorizado.fechaExportacion` está solicitado explícitamente por el análisis (cambio 2 sobre la entidad) y es compatible con `LocalDateTime.now()` usado en R-010 y con el tipo de `TareaImportacion.fechaExportacion`. No supone discrepancia con `0003_importacion-vistas`, que no fijaba el tipo de `UsuarioAutorizado.fechaExportacion`.