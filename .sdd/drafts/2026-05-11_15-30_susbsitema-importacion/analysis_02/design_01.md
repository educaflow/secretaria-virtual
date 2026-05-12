---
type: design
---

# Diseño: Subsistema de Importación de Usuarios

**Objetivo:** Crear el subsistema `importacion` que permite a los administradores importar ficheros XML/CSV para registrar usuarios autorizados y actualizar automáticamente los tipos de usuario de los ya registrados.  
**Capa:** subsystem/importacion  
**Análisis de origen:** `.sdd/drafts/2026-05-11_15-30_susbsitema-importacion/analysis_02/analysis.md`  
**Skills necesarios para la implementación:** k-sistemas, k-vistas

---

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/importacion/domains/TareaImportacion.xml` | Crear | k-sistemas (modelos.md) | Entidad TareaImportacion con todos sus campos y FKs |
| `subsystem/registrousuario/domains/UsuarioAutorizado.xml` | Modificar | k-sistemas (modelos.md) | `curso` → required; `fechaExportacion` → datetime + required; ampliar unique-constraint a 5 campos |
| `subsystem/importacion/service/TareaImportacionService.java` | Crear | k-sistemas (servicios.md) | Interfaz del servicio de importación |
| `subsystem/importacion/service/impl/TareaImportacionServiceImpl.java` | Crear | k-sistemas (servicios.md) | Implementación del servicio de importación |
| `subsystem/importacion/db/repo/TareaImportacionRepository.java` | Crear | k-sistemas (modelos.md) | Repositorio con validateInsert (V-003) y finder para V-005 |
| `subsystem/registrousuario/db/repo/UsuarioAutorizadoRepository.java` | Recrear | k-sistemas (modelos.md) | Restaurar métodos originales del flujo de registro + nuevos finders para OP-04 |
| `subsystem/importacion/controller/TareaImportacionController.java` | Crear | k-sistemas (controladores.md) | Controlador que orquesta el flujo OP-03 con separación de transacciones |
| `subsystem/importacion/views/TareaImportacion.xml` | Crear | k-vistas | @Main-action global (sin filtro) + @Main-grid + @Main-form de detalle |
| `subsystem/importacion/views/TareaImportacion-New.xml` | Crear | k-vistas | @New-action + @New-form para crear nueva importación |
| `system/gestioncentro/views/gestion-centro-main.xml` | Modificar | k-vistas | Eliminar vistas TareaImportacion obsoletas; añadir @SecretariaVirtualModule-action con filtro de centro |

---

## Paso 1 — Dominio: crear `TareaImportacion.xml`

**Fichero:** `subsystem/importacion/domains/TareaImportacion.xml`

XML completo de la entidad:

```xml
<domain-models xmlns="http://axelor.com/xml/ns/domain-models"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://axelor.com/xml/ns/domain-models https://axelor.com/xml/ns/domain-models/domain-models_8.1.xsd">

    <module name="importacion" package="com.educaflow.subsystem.importacion.db"/>
    <entity name="TareaImportacion" repository="abstract">
        <datetime name="fechaImportacion" title="Fecha de importación" required="true"/>
        <many-to-one name="tipoUsuario"  ref="com.educaflow.subsystem.common.db.TipoUsuario"
                     title="Tipo de usuario" required="true"/>
        <many-to-one name="fichero"      ref="com.axelor.meta.db.MetaFile"
                     title="Fichero"     required="true"/>
        <string      name="nombreFichero" title="Nombre de fichero"   required="true"/>
        <many-to-one name="usuario"      ref="com.axelor.auth.db.User"
                     title="Importador"  required="true"/>
        <many-to-one name="centro"       ref="com.educaflow.subsystem.common.db.Centro"
                     title="Centro"      required="true"/>
        <integer     name="curso"        title="Curso académico"      required="true"/>
        <datetime    name="fechaExportacion" title="Fecha de exportación" required="true"/>
        <boolean     name="correcta"     title="Correcta"             required="true" default="false"/>
        <string      name="log"          title="Log"                  large="true"/>
    </entity>

</domain-models>
```

**Notas de implementación:**
- No existe `unique-constraint` en esta entidad (múltiples intentos fallidos sobre la misma combinación son válidos; la unicidad de importaciones correctas se controla por V-005 en el servicio).
- La integridad referencial RESTRICT de V-016, V-017 y V-018 la aplica la propia BD por defecto al crear la FK (Axelor/JPA no configura `ON DELETE CASCADE`, por lo que PostgreSQL lanza violación de FK si se intenta borrar un Centro, TipoUsuario o User referenciado). No se requiere atributo adicional en el XML.
- `fechaExportacion` se inicializa a `fechaImportacion` al crear la tarea; para ficheros XML se actualiza al valor del XML dentro de la misma transacción de procesamiento.

**Verificar:** el build genera `AbstractTareaImportacionRepository.java` sin errores de compilación.

---

## Paso 2 — Dominio: modificar `UsuarioAutorizado.xml`

**Fichero:** `subsystem/registrousuario/domains/UsuarioAutorizado.xml`

XML completo de la entidad (estado final):

```xml
<domain-models xmlns="http://axelor.com/xml/ns/domain-models"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://axelor.com/xml/ns/domain-models https://axelor.com/xml/ns/domain-models/domain-models_8.1.xsd">

    <module name="registro" package="com.educaflow.subsystem.registrousuario.db"/>
    <entity name="UsuarioAutorizado" repository="abstract">
        <many-to-one name="centro"       ref="com.educaflow.subsystem.common.db.Centro"
                     required="true"/>
        <string      name="dni"          title="dni__!!"              required="true"/>
        <many-to-one name="tipoUsuario"  ref="com.educaflow.subsystem.common.db.TipoUsuario"
                     required="true"     title="Tipo de usuario"/>
        <integer     name="curso"        required="true"/>
        <datetime    name="fechaExportacion" title="Fecha de exportación" required="true"/>
        <unique-constraint columns="centro,dni,tipoUsuario,curso,fechaExportacion"/>
    </entity>

</domain-models>
```

**Cambios respecto al estado anterior:**
- `curso`: añadido `required="true"`.
- `fechaExportacion`: tipo cambiado de `date` a `datetime`; añadido `required="true"`.
- `unique-constraint`: ampliado de `(centro,dni,tipoUsuario)` a `(centro,dni,tipoUsuario,curso,fechaExportacion)`.

**Impacto en el flujo de registro existente:** Los métodos `isAuthorized(String)` y `findAllByDni(String)` de `UsuarioAutorizadoRepository` no leen `curso` ni `fechaExportacion`, por lo que no se ven afectados. El servicio de registro (`RegistroServiceImpl`) tampoco los usa. No hay migración de datos pendiente salvo actualizar los registros existentes para rellenar los nuevos campos NOT NULL (tarea de migración fuera del scope de este diseño).

**Verificar:** el build no produce errores por la restricción `NOT NULL` sobre `curso` y `fechaExportacion`.

---

## Paso 3 — Servicio: `TareaImportacionService` e implementación

### 3a. Interfaz: `TareaImportacionService.java`

**Fichero:** `subsystem/importacion/service/TareaImportacionService.java`  
**Paquete:** `com.educaflow.subsystem.importacion.service`

```java
public interface TareaImportacionService extends ModelService<TareaImportacion> {

    /**
     * Crea y persiste (tx independiente) el registro inicial de la tarea con correcta=false.
     * fechaExportacion se inicializa a fechaImportacion; será corregida en procesarImportacion.
     * El centro y curso se toman de importador.getCentroActivo() (ya validados por el controlador).
     */
    TareaImportacion crearTareaInicial(User importador, TipoUsuario tipoUsuario,
                                       MetaFile fichero, String nombreFichero);

    /**
     * Parsea el fichero, valida el contenido, inserta UsuarioAutorizado y ejecuta OP-04/OP-05.
     * Ejecuta en su propia transacción; si falla hace rollback de UsuarioAutorizado y
     * CentroUsuarioTipoUsuario pero NO de TareaImportacion (ya comprometida en tx anterior).
     * Retorna el mensaje de log de resumen (n.º insertados, n.º omitidos por DNI inválido).
     * Lanza BusinessException con el log de error si cualquier validación o procesamiento falla.
     */
    String procesarImportacion(Long tareaId) throws BusinessException;

    /**
     * Actualiza TareaImportacion con el resultado final (correcta y log) en una tx independiente.
     * Se llama tanto en caso de éxito como en caso de fallo de procesarImportacion.
     */
    void actualizarResultado(Long tareaId, boolean correcta, String log);
}
```

**Imports necesarios:** `com.axelor.auth.db.User`, `com.axelor.meta.db.MetaFile`, `com.axelor.db.modelservice.ModelService`, `com.educaflow.subsystem.common.db.TipoUsuario`, `com.educaflow.subsystem.importacion.db.TareaImportacion`, `com.educaflow.base.infrastructure.validation.messages.BusinessException`.

---

### 3b. Implementación: `TareaImportacionServiceImpl.java`

**Fichero:** `subsystem/importacion/service/impl/TareaImportacionServiceImpl.java`  
**Paquete:** `com.educaflow.subsystem.importacion.service.impl`

**Clase:** `public class TareaImportacionServiceImpl extends DefaultModelService<TareaImportacion> implements TareaImportacionService`

**Constructor obligatorio:**
```java
public TareaImportacionServiceImpl(Class<TareaImportacion> model, Repository repository) {
    super(model, repository);
}
```

**Métodos (firmas + descripción del cuerpo):**

```java
@Override
@Transactional
public TareaImportacion crearTareaInicial(User importador, TipoUsuario tipoUsuario,
                                           MetaFile fichero, String nombreFichero) {
    // Crea TareaImportacion con todos los campos:
    //   fechaImportacion = LocalDateTime.now()
    //   tipoUsuario, fichero, nombreFichero = parámetros
    //   usuario = importador
    //   centro = importador.getCentroActivo()
    //   curso = importador.getCentroActivo().getCurso()
    //   fechaExportacion = fechaImportacion (valor provisional; se corrige en procesarImportacion si es XML)
    //   correcta = false
    //   log = null
    // Llama a tareaRepo.save(tarea) y retorna el registro persistido.
    // validateInsert del repositorio comprueba V-003 (tipoUsuario válido).
}

@Override
@Transactional
public String procesarImportacion(Long tareaId) throws BusinessException {
    // Lee TareaImportacion por tareaId (con centroActivo, tipoUsuario, fichero cargados).
    // Descarga contenido del fichero con MetaFileUtil.readBytes(tarea.getFichero()).
    // Rama XML (tipoUsuario.getCodigo() != "PROFESOR_EXTERNO"):
    //   - parsearXml(content): extrae codigoCentro, cursoXml, fechaExportacionXml, List<String> dnis
    //     → si falla parsing: lanza BusinessException con log V-006
    //   - V-004: si codigoCentro != tarea.getCentro().getCode() → lanza BusinessException con log V-004
    //   - V-005: llama tareaRepo.existsImportacionCorrecta(centro, tipoUsuario, cursoXml, fechaExportacionXml)
    //     → si true: lanza BusinessException con log V-005
    //   - Actualiza tarea.fechaExportacion = fechaExportacionXml, tarea.curso = cursoXml
    //     (dentro de la misma tx2; si tx2 hace rollback, estos cambios también se revierten)
    // Rama CSV (tipoUsuario.getCodigo() == "PROFESOR_EXTERNO"):
    //   - parsearCsv(content): extrae List<String> dnis (una por línea; si primera línea no supera DniUtil.isValid() se omite como cabecera)
    //     → si falla parsing: lanza BusinessException con log V-007
    //   - V-005: llama tareaRepo.existsImportacionCorrecta(centro, tipoUsuario, tarea.getCurso(), tarea.getFechaExportacion())
    //     → si true: lanza BusinessException con log V-005
    // Procesar DNIs uno a uno:
    //   - V-008: DniUtil.isValid(dni); si inválido, añade línea al log de errores y omite
    //   - Para DNIs válidos: insertar UsuarioAutorizado si no existe ya (existsByUniqueKey check)
    //     → si existe (V-014): silencio (idempotente)
    //     → si no existe: crear y guardar UsuarioAutorizado
    // Ejecutar OP-04 o OP-05:
    //   - XML: ejecutarOP04(tarea, dnis válidos)
    //   - CSV: ejecutarOP05(tarea, dnis válidos)
    //   → si cualquier excepción no controlada: se propaga y causa rollback de tx2 (V-009)
    // Construir y retornar String log con resumen: n.º insertados, n.º ya existentes, n.º DNIs inválidos
}

@Override
@Transactional
public void actualizarResultado(Long tareaId, boolean correcta, String log) {
    // Busca TareaImportacion por tareaId (con JpaRepository.of(TareaImportacion.class).find(tareaId))
    // Establece tarea.setCorrecta(correcta) y tarea.setLog(log)
    // Llama save(); el repositorio no tiene validateUpdate porque TareaImportacion es inmutable
    // desde la perspectiva de la UI, pero el servicio necesita esta actualización interna.
}
```

**Métodos privados auxiliares:**

```java
private record ParsedXml(String codigoCentro, Integer curso,
                          LocalDateTime fechaExportacion, List<String> dnis) {}

private ParsedXml parsearXml(byte[] content) throws BusinessException {
    // Usa XmlUtil para parsear; busca nodo <centro> con atributos codigo, curso, fechaExportacion
    // y el nodo colección de DNIs (ej. <profesores><profesor dni="..."/> ...)
    // Si falta el nodo <centro> o algún atributo obligatorio → lanza BusinessException (log V-006)
}

private List<String> parsearCsv(byte[] content) throws BusinessException {
    // Lee bytes como texto (UTF-8), split por líneas; omite vacías
    // Si no puede leerse como texto → lanza BusinessException (log V-007)
    // Si la primera línea no supera DniUtil.isValid(), la omite (cabecera)
    // Retorna todas las líneas no vacías como List<String> (valores crudos; validación DNI en el bucle principal)
}

private void ejecutarOP04(TareaImportacion tarea, List<String> dnisValidos) {
    // Determina tipoBase (ej. PROFESOR) y tipoEx (EXPROFESOR) del EX_MAPPING
    // Carga desde UsuarioAutorizadoRepository:
    //   ultimaFechaExportacion = findUltimaFechaExportacion(centro, tipoBase, tarea.getCurso())
    //   dnisCurrent = findDnisByFechaExportacion(centro, tipoBase, tarea.getCurso(), ultimaFechaExportacion)
    //   dnisAnterior = findDnisByFechaExportacion(centro, tipoBase, tarea.getCurso(), <todas las fechaExportacion < ultimaFechaExportacion>)
    //     → esto implica un finder con fechaExportacion < ultimaFechaExportacion
    // Para cada CentroUsuario del centro que tenga tipoBase o tipoEx en CentroUsuarioTipoUsuario:
    //   dni = centroUsuario.getUsuario().getDni()
    //   uia  = dnisCurrent.contains(dni)
    //   uiant = dnisAnterior.contains(dni)
    //   Aplica tabla OP-04:
    //     (No,No) → eliminar tipoBase (caso defensivo)
    //     (No,Sí) → añadir tipoEx; eliminar tipoBase si lo tenía
    //     (Sí,No) o (Sí,Sí) → añadir tipoBase; eliminar tipoEx si lo tenía
    //   Usa CentroUsuarioTipoUsuarioRepository (JpaRepository.of) para add/remove
}

private void ejecutarOP05(TareaImportacion tarea, List<String> dnis) {
    // Para cada DNI válido:
    //   Busca CentroUsuario cuyo User.dni == dni y centroUsuario.centro == tarea.getCentro()
    //   Si existe y no tiene ya PROFESOR_EXTERNO en CentroUsuarioTipoUsuario → añade el tipo
    //   Si no existe CentroUsuario con ese DNI → omite silenciosamente (usuario no registrado)
    // Usa CentroUsuarioRepository y CentroUsuarioTipoUsuarioRepository (JpaRepository.of)
}
```

**Notas sobre transacciones:**
- `crearTareaInicial`, `procesarImportacion` y `actualizarResultado` son métodos `@Transactional` independientes.
- Son llamados desde el controlador (fuera del bean), por lo que Guice AOP intercepta cada llamada y abre/cierra su propia transacción. Si `procesarImportacion` lanza excepción, su tx hace rollback sin afectar al TareaImportacion ya comprometido en `crearTareaInicial`.
- **NO crear módulo Guice** para este servicio: `ModelServiceFactory` descubre automáticamente `TareaImportacionServiceImpl` por convención de paquete `service.impl.*ServiceImpl`.

**Verificar:** el servicio compila sin importaciones cíclicas; `procesarImportacion` lanza `BusinessException` en todos los casos de error documentados.

---

## Paso 4 — Repositorios

### 4a. `TareaImportacionRepository.java`

**Fichero:** `subsystem/importacion/db/repo/TareaImportacionRepository.java`  
**Paquete:** `com.educaflow.subsystem.importacion.db.repo`

**Clase:** `public class TareaImportacionRepository extends AbstractTareaImportacionRepository`

**Métodos:**

```java
/**
 * V-005: Comprueba si ya existe un TareaImportacion con correcta=true
 * para la combinación (centro, tipoUsuario, curso, fechaExportacion).
 * Query JPQL: SELECT COUNT(t) > 0 FROM TareaImportacion t
 *   WHERE t.correcta = true
 *   AND t.centro = :centro AND t.tipoUsuario = :tipoUsuario
 *   AND t.curso = :curso AND t.fechaExportacion = :fechaExportacion
 */
public boolean existsImportacionCorrecta(Centro centro, TipoUsuario tipoUsuario,
                                          Integer curso, LocalDateTime fechaExportacion) { ... }

/**
 * V-003: tipoUsuario debe pertenecer a {PROFESOR, ALUMNO, FAMILIAR, PROFESOR_EXTERNO}.
 * Si el código de tipoUsuario no está en ese conjunto lanza ValidationException
 * con mensaje descriptivo que incluye el valor recibido y los valores válidos.
 */
@Override
public TareaImportacion validateInsert(TareaImportacion entity) { ... }
```

**Nota:** `validateInsert` es llamado por el repositorio base en el `prePersist` JPA. Como TareaImportacion solo se crea (nunca se actualiza por el usuario), no es necesario `validateUpdate`.

---

### 4b. `UsuarioAutorizadoRepository.java`

**Fichero:** `subsystem/registrousuario/db/repo/UsuarioAutorizadoRepository.java`  
**Paquete:** `com.educaflow.subsystem.registrousuario.db.repo`

**Clase:** `public class UsuarioAutorizadoRepository extends AbstractUsuarioAutorizadoRepository`

**Métodos existentes a restaurar (usados por el flujo de registro):**

```java
/**
 * Comprueba si existe al menos un UsuarioAutorizado con el DNI dado (cualquier centro/tipo).
 * Usado por UsuarioAutorizadoService.isAuthorized(String) en el flujo de registro.
 */
public boolean isAuthorized(String dni) { ... }

/**
 * Retorna todos los UsuarioAutorizado con el DNI dado.
 * Usado por RegistroPendienteRepository.findTiposUsuarioByDni para calcular perfiles.
 */
public List<UsuarioAutorizado> findAllByDni(String dni) { ... }
```

**Métodos nuevos para OP-04:**

```java
/**
 * Retorna la fecha de exportación más reciente de todos los UsuarioAutorizado
 * para la combinación (centro, tipoUsuario, curso).
 * Query: SELECT MAX(ua.fechaExportacion) FROM UsuarioAutorizado ua
 *   WHERE ua.centro = :centro AND ua.tipoUsuario = :tipoUsuario AND ua.curso = :curso
 * Retorna Optional.empty() si no hay ningún registro para esa combinación.
 */
public Optional<LocalDateTime> findUltimaFechaExportacion(Centro centro,
                                                            TipoUsuario tipoUsuario,
                                                            Integer curso) { ... }

/**
 * Retorna la lista de DNIs de todos los UsuarioAutorizado con la fechaExportacion dada
 * (para la combinación centro/tipoUsuario/curso).
 * Usado en OP-04 para obtener tanto el conjunto "actual" (= última fechaExportacion)
 * como el conjunto "anterior" (= fechaExportacion < última) llamando dos veces con
 * dos fechas distintas, o bien con un predicado < para anteriores.
 * Query: SELECT ua.dni FROM UsuarioAutorizado ua
 *   WHERE ua.centro = :centro AND ua.tipoUsuario = :tipoUsuario
 *   AND ua.curso = :curso AND ua.fechaExportacion = :fechaExportacion
 */
public List<String> findDnisByFechaExportacion(Centro centro, TipoUsuario tipoUsuario,
                                                Integer curso,
                                                LocalDateTime fechaExportacion) { ... }

/**
 * Retorna los DNIs de todos los UsuarioAutorizado cuya fechaExportacion
 * sea estrictamente anterior al valor dado (misma combinación centro/tipoUsuario/curso).
 * Usado en OP-04 para determinar el conjunto "anterior" (UsuarioImportadoAnterior).
 * Query: SELECT ua.dni FROM UsuarioAutorizado ua
 *   WHERE ua.centro = :centro AND ua.tipoUsuario = :tipoUsuario
 *   AND ua.curso = :curso AND ua.fechaExportacion < :fechaExportacion
 */
public List<String> findDnisConFechaExportacionAnterior(Centro centro,
                                                          TipoUsuario tipoUsuario,
                                                          Integer curso,
                                                          LocalDateTime fechaExportacion) { ... }

/**
 * V-014: Comprueba si ya existe exactamente un UsuarioAutorizado con la clave de 5 campos.
 * Usado antes de cada inserción para ignorar duplicados silenciosamente.
 * Query: SELECT COUNT(ua) > 0 FROM UsuarioAutorizado ua
 *   WHERE ua.centro = :centro AND ua.dni = :dni AND ua.tipoUsuario = :tipoUsuario
 *   AND ua.curso = :curso AND ua.fechaExportacion = :fechaExportacion
 */
public boolean existsByUniqueKey(Centro centro, String dni, TipoUsuario tipoUsuario,
                                  Integer curso, LocalDateTime fechaExportacion) { ... }
```

**Verificar:** los métodos originales `isAuthorized` y `findAllByDni` son alcanzables desde `RegistroServiceImpl` sin cambios en ese servicio.

---

## Paso 5 — Controlador: `TareaImportacionController.java`

**Fichero:** `subsystem/importacion/controller/TareaImportacionController.java`  
**Paquete:** `com.educaflow.subsystem.importacion.controller`

**Clase:** `public class TareaImportacionController`

```java
/**
 * Orquesta el flujo completo OP-03 con tres transacciones independientes:
 *   tx1: crearTareaInicial (siempre se compromete)
 *   tx2: procesarImportacion (puede hacer rollback sin afectar tx1)
 *   tx3: actualizarResultado (siempre se compromete con el resultado final)
 *
 * La separación de transacciones funciona porque el controlador no es @Transactional:
 * cada llamada al servicio (proxy Guice) abre y cierra su propia transacción.
 */
@CallMethod
public void importar(ActionRequest request, ActionResponse response) {
    // 1. Extraer tipoUsuario y fichero del modelo de la request.
    //    Usar ActionRequestHelper<TareaImportacion>.getModel() con AllowProperties("tipoUsuario","fichero")
    // 2. User importador = AuthUtils.getUser()
    // 3. V-010: si importador.getCentroActivo() == null
    //    → response.setError("El importador '{importador.name}' no tiene un centro activo asignado."); return
    // 4. V-011: si importador.getCentroActivo().getCurso() == null
    //    → response.setError("El centro activo '{centro.name}' no tiene curso académico activo configurado."); return
    // 5. String nombreFichero = tarea.getFichero().getFileName()  (o nombre del MetaFile)
    // 6. TareaImportacion tareaGuardada = service.crearTareaInicial(importador, tipoUsuario, fichero, nombreFichero)  [tx1]
    // 7. try {
    //       String log = service.procesarImportacion(tareaGuardada.getId())  [tx2]
    //       service.actualizarResultado(tareaGuardada.getId(), true, log)    [tx3-éxito]
    //       response.setFlash("Importación completada. " + log)
    //       response.setSignal("back", true)
    //    } catch (BusinessException e) {
    //       service.actualizarResultado(tareaGuardada.getId(), false, e.getMessage())  [tx3-fallo]
    //       response.setError("La importación ha fallado. Consulte el log para más detalles.")
    //    }
}
```

**Notas:**
- El controlador no lleva `@Transactional`.
- `AuthUtils` está en el paquete `com.axelor.auth`.
- Si `procesarImportacion` lanza cualquier excepción que NO sea `BusinessException` (error técnico inesperado), deberá propagarse o ser capturada como `Exception` con el mismo patrón de `actualizarResultado(id, false, ex.getMessage())`.

**Verificar:** compila con los imports correctos; `@CallMethod` está en el paquete `com.axelor.rpc`.

---

## Paso 6 — Vistas: `TareaImportacion.xml`

**Fichero:** `subsystem/importacion/views/TareaImportacion.xml`

**Contenido del fichero:** `@Main-action` (global, sin filtro de centro) + `@Main-grid` + `@Main-form` de detalle.

### `subsysImportacion.TareaImportacion@Main-action`

- `action-view` con título "Ficheros de importación"
- Modelo: `com.educaflow.subsystem.importacion.db.TareaImportacion`
- Views: `@Main-grid` (grid) + `@Main-form` (form)
- Sin `<domain>` ni `<context>` — muestra todas las importaciones
- `<view-param name="show-toolbar-form" value="false"/>`
- `<view-param name="forceEdit" value="false"/>` (solo lectura para registros existentes)

### `subsysImportacion.TareaImportacion@Main-grid`

- `canNew="false"`, `canEdit="false"`, `canDelete="false"`, `canSave="false"`
- `canEditOnClick="true"` (abre `@Main-form` al hacer clic)
- `orderBy="-fechaImportacion"`, `x-selector="none"`, `edit-icon="false"`
- **Toolbar con botón "Nueva importación"**: botón que llama `subsysImportacion.TareaImportacion@abrir-nueva-action`, una `action-view` que abre `@New-form` en una nueva pestaña/página. El título del botón es "Nueva importación".
- Columnas visibles:
  - `fechaImportacion` (width="160px", título "Fecha de importación")
  - `tipoUsuario` (width="160px")
  - `nombreFichero` (título "Fichero")
  - `usuario` (width="180px", título "Importador")
  - `centro` (width="180px")
  - `correcta` (width="100px")

### `subsysImportacion.TareaImportacion@Main-form`

- `width="large"`, `canAttach="false"`, `canBack="false"`, `canDelete="false"`, `canNew="false"`, `canSave="false"`, `canMore="false"`
- Todos los campos con `readonly="true"`
- **Panel principal** (`importacionDetalleDatosPanel`): columnas (4 col):
  - `fechaImportacion` colSpan=3, `correcta` colSpan=1
  - `tipoUsuario` colSpan=2, `curso` colSpan=2
  - `centro` colSpan=3, `usuario` colSpan=3 
  - `nombreFichero` colSpan=4
  - `fichero` widget="binary-link" colSpan=4 (descarga del fichero original)
- **Panel log** (`importacionDetalleLogPanel`, título "Log"):
  - `log` widget="text" colSpan=12
- **Panel botones** (`importacionDetalleBotonesPanel`, showFrame=false):
  - Botón "Volver" onClick=`subsysImportacion.TareaImportacion@Main-btnVolver-action` colSpan=2 colOffset=10

### Acciones del fichero `TareaImportacion.xml`

`subsysImportacion.TareaImportacion@abrir-nueva-action`:
- Acción `action-view` (o `action-method` que hace `response.setView(...)`) para abrir `@New-form`.
- Si se usa `action-view` inline: referencia a `subsysImportacion.TareaImportacion@New-action`.

`subsysImportacion.TareaImportacion@Main-btnVolver-action` (action-group):
- `<action name="back"/>`

---

## Paso 7 — Vistas: `TareaImportacion-New.xml`

**Fichero:** `subsystem/importacion/views/TareaImportacion-New.xml`

**Contenido del fichero:** `@New-action` + `@New-form`.

### `subsysImportacion.TareaImportacion@New-action`

- `action-view` con título "Nueva importación"
- Modelo: `com.educaflow.subsystem.importacion.db.TareaImportacion`
- Views: solo `@New-form` (form)
- `<view-param name="show-toolbar-form" value="false"/>`
- Sin `canBack`, sin `forceEdit`; el formulario se abre vacío (para creación).

### `subsysImportacion.TareaImportacion@New-form`

- `width="large"`, `canAttach="false"`, `canBack="false"`, `canDelete="false"`, `canNew="false"`, `canSave="false"`, `canMore="false"`
- **Panel datos** (`importacionNuevaDatosPanel`, sin título):
  - `tipoUsuario` colSpan=6, widget="SwitchSelect", `selection-in="['PROFESOR','ALUMNO','FAMILIAR','PROFESOR_EXTERNO']"`, required=true
  - `fichero` colSpan=6, widget="binary-link", required=true
- **Panel botones** (`importacionNuevaBotonesPanel`, showFrame=false):
  - Botón "Cancelar" onClick=`subsysImportacion.TareaImportacion@New-btnCancelar-action` colSpan=2 colOffset=8 outline=true
  - Botón "Importar"  onClick=`subsysImportacion.TareaImportacion@New-btnImportar-action` colSpan=2

### Acciones del fichero `TareaImportacion-New.xml`

**`subsysImportacion.TareaImportacion@New-btnImportar-action`** (action-group):
1. `subsysImportacion.TareaImportacion@New-validate-action` (action-validate: V-001 y V-002)
2. `subsysImportacion.TareaImportacion@New-importar-method-action` (action-method: llama al controlador)

**`subsysImportacion.TareaImportacion@New-btnCancelar-action`** (action-group):
1. `<action name="back"/>`

**`subsysImportacion.TareaImportacion@New-validate-action`** (action-validate):
- V-001: condición `!tipoUsuario`, mensaje "El tipo de usuario es obligatorio para iniciar la importación."
- V-002: condición `!fichero`, mensaje "El fichero es obligatorio para iniciar la importación."

**`subsysImportacion.TareaImportacion@New-importar-method-action`** (action-method):
- `call="com.educaflow.subsystem.importacion.controller.TareaImportacionController:importar"`

---

## Paso 8 — Modificar `gestion-centro-main.xml`

**Fichero:** `system/gestioncentro/views/gestion-centro-main.xml`

**Cambios:**

1. **Eliminar** los siguientes elementos XML (obsoletos, con nombre de campo incorrecto `tipoFichero`):
   - `<grid name="sysGestionCentro.TareaImportacion@Main-grid" ...>` (campos `tipoFichero`, etc.)
   - `<form name="sysGestionCentro.TareaImportacion@Main-form" ...>`
   - `<action-view name="sysGestion.Importacion@Main-action" ...>`
   - `<form name="sysGestion.TareaImportacion@Menu-form" ...>`
   - `<action-record name="sysGestion.TareaImportacion@OnNew-action" ...>`

2. **Añadir** al final del fichero (antes del cierre `</object-views>`):

`sysGestion.Importacion@SecretariaVirtualModule-action` (action-view):
- Título: "Ficheros de importación"
- Modelo: `com.educaflow.subsystem.importacion.db.TareaImportacion`
- Views: `subsysImportacion.TareaImportacion@Main-grid` (grid) + `subsysImportacion.TareaImportacion@New-form` (form)
  - **Nota:** esta acción reutiliza las vistas del subsistema; NO duplica grid ni form.
- `<domain>self.centro = :centroActivo</domain>`
- `<context name="centroActivo" expr="eval: __user__?.centroActivo"/>`
- `<view-param name="show-toolbar-form" value="false"/>`

**Resultado:** el menú `sysGestion.Importacion@SecretariaVirtualModule-menuitem` ya apunta a esta action-view (ya definido en `menus.xml`). El grid reutilizado muestra solo las importaciones del `centroActivo` del usuario.

**Verificar:** al abrir la sección "Ficheros de importación" desde el menú de gestión de centro, el grid solo muestra registros del centro activo del usuario.

---

## Paso 9 — Verificación final

1. `./gradlew clean build --info` compila sin errores.
2. El build genera `AbstractTareaImportacionRepository` en `subsystem/importacion/db/`.
3. El build regenera `AbstractUsuarioAutorizadoRepository` con la nueva firma de unique-constraint.
4. La tabla `tarea_importacion` se crea en BD con todas las columnas y FKs.
5. La constraint `UNIQUE(centro, dni, tipo_usuario, curso, fecha_exportacion)` existe en la tabla `registro_usuario_autorizado`.
6. Las vistas `subsysImportacion.*` son accesibles desde el menú "Ficheros importación" (grupo `admins`).
7. La vista filtrada `sysGestion.Importacion@SecretariaVirtualModule-action` solo muestra registros del centro activo.
8. El botón "Importar" en `@New-form` llama al controlador y se comporta según OP-03.
9. `RegistroServiceImpl` sigue compilando (no cambios de API en su subsistema).

---

## Matriz de trazabilidad V-XXX → ubicación

| ID | Regla | Capa | Ubicación |
|----|-------|------|-----------|
| V-001 | `tipoUsuario` required | Cliente | `subsysImportacion.TareaImportacion@New-validate-action` (action-validate) |
| V-002 | `fichero` required | Cliente | `subsysImportacion.TareaImportacion@New-validate-action` (action-validate) |
| V-003 | `tipoUsuario` en {PROFESOR, ALUMNO, FAMILIAR, PROFESOR_EXTERNO} | Servidor | `TareaImportacionRepository.validateInsert()` (repositorio, prePersist) |
| V-004 | `codigoCentro` XML coincide con `centroActivo.code` | Servidor | `TareaImportacionServiceImpl.procesarImportacion()` — rama XML, después de parsear cabecera |
| V-005 | No existe importación correcta con misma (centro, tipoUsuario, curso, fechaExportacion) | Servidor | `TareaImportacionServiceImpl.procesarImportacion()` — llama `tareaRepo.existsImportacionCorrecta(...)` |
| V-006 | Formato XML válido | Servidor | `TareaImportacionServiceImpl.procesarImportacion()` — método privado `parsearXml()` |
| V-007 | Formato CSV válido | Servidor | `TareaImportacionServiceImpl.procesarImportacion()` — método privado `parsearCsv()` |
| V-008 | Formato DNI/NIE válido por registro | Servidor | `TareaImportacionServiceImpl.procesarImportacion()` — bucle de DNIs, `DniUtil.isValid(dni)` |
| V-009 | Error no controlado en OP-04/OP-05 | Servidor | `TareaImportacionServiceImpl.procesarImportacion()` — excepción propagada causa rollback tx2; `actualizarResultado(false, ...)` en tx3 |
| V-010 | `centroActivo` del importador no es null | Servidor | `TareaImportacionController.importar()` — antes de llamar `crearTareaInicial` |
| V-011 | `centroActivo.curso` no es null | Servidor | `TareaImportacionController.importar()` — antes de llamar `crearTareaInicial` |
| V-012 | `TareaImportacion` inmutable | Aplicación | Sin método `update()` en `TareaImportacionService`; no hay controles de edición en vistas; sin permiso `update` para el grupo `admins` |
| V-013 | `UsuarioAutorizado` inmutable | Aplicación | Sin método `update()` en ningún servicio del subsistema importacion; sin permiso `update` para el grupo `admins` |
| V-014 | Unique `(centro,dni,tipoUsuario,curso,fechaExportacion)` en UsuarioAutorizado — duplicado silencioso | Dominio + Servidor | Constraint `<unique-constraint>` en `UsuarioAutorizado.xml` (nivel BD); `UsuarioAutorizadoRepository.existsByUniqueKey(...)` en `procesarImportacion()` antes de cada insert |
| V-015 | Exclusividad tipo base / EX en CentroUsuarioTipoUsuario | Servidor | `TareaImportacionServiceImpl.ejecutarOP04()` — elimina el tipo contrario antes de añadir el nuevo |
| V-016 | RESTRICT al borrar `Centro` | BD | FK sin CASCADE generada por JPA; PostgreSQL lanza error de FK (no requiere código adicional) |
| V-017 | RESTRICT al borrar `TipoUsuario` | BD | FK sin CASCADE generada por JPA; PostgreSQL lanza error de FK (no requiere código adicional) |
| V-018 | RESTRICT al borrar `User` (importador) | BD | FK sin CASCADE generada por JPA; PostgreSQL lanza error de FK (no requiere código adicional) |
