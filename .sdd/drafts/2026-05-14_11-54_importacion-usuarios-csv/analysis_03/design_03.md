---
type: design
---

# Diseño: Importación de usuarios autorizados desde CSV

**Objetivo:** Implementar la importación masiva de `UsuarioAutorizado` de tipo `PROFESOR_EXTERNO` desde un CSV de DNIs, ampliando el subsistema `importacion` existente y creando el servicio de dominio de `UsuarioAutorizado` en `registrousuario`.
**Capa:** `subsystem/importacion` (ampliación) + `subsystem/registrousuario` (nuevo servicio) + `subsystem/common` (finder en TipoUsuario)
**Análisis de origen:** `.sdd/drafts/2026-05-14_11-54_importacion-usuarios-csv/analysis_03/analysis.md`
**Skills necesarios para la implementación:** `k-sistemas`, `k-validaciones`, `k-code-quality`

---

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/registrousuario/domains/UsuarioAutorizado.xml` | Modificar | k-sistemas (modelos.md) | Cambiar `<date>` → `<datetime>` en `fechaExportacion`; cambiar `<unique-constraint>` a 4 columnas; añadir `<finder-method>` |
| `subsystem/common/domains/TipoUsuario.xml` | Modificar | k-sistemas (modelos.md) | Añadir `<finder-method name="findByCodigo">` |
| `subsystem/registrousuario/service/UsuarioAutorizadoService.java` | Crear | k-sistemas (servicios.md) | Interfaz del servicio — extiende `ModelService<UsuarioAutorizado>` |
| `subsystem/registrousuario/service/impl/UsuarioAutorizadoServiceImpl.java` | Crear | k-sistemas (servicios.md), k-validaciones | Implementación: `validateInsert` (V-001), `insert` con `fireActionRule_asignarFechaExportacion` (R-010), `findByCentroDniTipoUsuarioCurso` |
| `subsystem/registrousuario/db/repo/UsuarioAutorizadoRepository.java` | Crear | k-sistemas (modelos.md) | Repositorio concreto que extiende `AbstractUsuarioAutorizadoRepository` (necesario por `repository="abstract"` en el XML) |
| `subsystem/importacion/importador/ResultadoImportacion.java` | Modificar | k-sistemas | Renombrar `usuariosImportados`→`creados`, `numeroErrores`→`errores`; añadir `ignorados` |
| `subsystem/importacion/importador/ImportadorFicheroFactory.java` | Modificar | k-sistemas | Añadir parámetro `ModelServiceFactory` a `create()` y pasarlo al constructor de `ImportadorUsuarioCSV` |
| `subsystem/importacion/importador/impl/ImportadorUsuarioCSV.java` | Modificar | k-sistemas, k-validaciones, k-code-quality | Implementar `importar()` y métodos privados de descomposición (R-001..R-016) |
| `subsystem/importacion/service/impl/TareaImportacionServiceImpl.java` | Modificar | k-sistemas | Añadir `@Inject ModelServiceFactory`; pasar a la factoría; eliminar prefijo del log |

---

## Pasos

### Paso 1 — Modificar `UsuarioAutorizado.xml`

Fichero: `src/main/java/com/educaflow/subsystem/registrousuario/domains/UsuarioAutorizado.xml`

**XML completo resultante:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<domain-models xmlns="http://axelor.com/xml/ns/domain-models"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://axelor.com/xml/ns/domain-models
                   https://axelor.com/xml/ns/domain-models/domain-models_8.1.xsd">

    <module name="registro" package="com.educaflow.subsystem.registrousuario.db"/>

    <entity name="UsuarioAutorizado" repository="abstract">

        <!-- V-002: obligatorio en modelo -->
        <many-to-one name="centro"
                     ref="com.educaflow.subsystem.common.db.Centro"
                     required="true"/>

        <!-- V-003: obligatorio en modelo -->
        <string name="dni" title="dni__!!" required="true"/>

        <!-- V-004: obligatorio en modelo -->
        <many-to-one name="tipoUsuario"
                     ref="com.educaflow.subsystem.common.db.TipoUsuario"
                     required="true"
                     title="Tipo de usuario"/>

        <integer name="curso"/>

        <!-- Cambio 2 (A9): de <date> a <datetime> para registrar el instante exacto de importación (R-010) -->
        <datetime name="fechaExportacion" title="Fecha de exportación"/>

        <!-- Cambio 1 (V-001): unique-constraint ampliado de 3 a 4 columnas -->
        <unique-constraint columns="centro,dni,tipoUsuario,curso"/>

        <!-- Finder usado por UsuarioAutorizadoServiceImpl para V-001, R-010 y R-011 -->
        <finder-method name="findByCentroAndDniAndTipoUsuarioAndCurso"
                       using="com.educaflow.subsystem.common.db.Centro:centro,
                              String:dni,
                              com.educaflow.subsystem.common.db.TipoUsuario:tipoUsuario,
                              Integer:curso"
                       filter="self.centro = :centro
                           AND self.dni = :dni
                           AND self.tipoUsuario = :tipoUsuario
                           AND self.curso = :curso"/>

    </entity>

</domain-models>
```

Cambios respecto al estado anterior:
- `<date name="fechaExportacion">` → `<datetime name="fechaExportacion">`
- `<unique-constraint columns="centro,dni,tipoUsuario"/>` → añade `,curso`
- Añadido `<finder-method name="findByCentroAndDniAndTipoUsuarioAndCurso">` con JPQL de búsqueda por los cuatro campos

**Verificación:** `./gradlew clean build --info` regenera `AbstractUsuarioAutorizadoRepository` con el método `findByCentroAndDniAndTipoUsuarioAndCurso` y el nuevo unique-constraint.

---

### Paso 2 — Modificar `TipoUsuario.xml`

Fichero: `src/main/java/com/educaflow/subsystem/common/domains/TipoUsuario.xml`

**XML completo resultante:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<domain-models xmlns="http://axelor.com/xml/ns/domain-models"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://axelor.com/xml/ns/domain-models
                   https://axelor.com/xml/ns/domain-models/domain-models_8.1.xsd">

    <module name="common" package="com.educaflow.subsystem.common.db"/>

    <entity name="TipoUsuario" repository="abstract">
        <string name="codigo" title="Código"/>
        <string name="nombre" namecolumn="true" title="Nombre"/>
        <one-to-many name="centroUsuarioTipoUsuario"
                     ref="com.educaflow.subsystem.common.db.CentroUsuarioTipoUsuario"
                     mappedBy="tipoUsuario" title=""/>
        <one-to-many name="cargos"
                     ref="com.educaflow.subsystem.common.db.Cargo"
                     mappedBy="tipoUsuario" title="Cargos"/>

        <!-- Finder para resolver TipoUsuario por código. Usado en R-004 desde ImportadorUsuarioCSV -->
        <finder-method name="findByCodigo"
                       using="String:codigo"
                       filter="self.codigo = :codigo"/>
    </entity>

</domain-models>
```

Cambio: añadido `<finder-method name="findByCodigo">`.

**Verificación:** `./gradlew clean build --info` regenera `AbstractTipoUsuarioRepository` con el método `findByCodigo(String codigo)`.

---

### Paso 3 — Crear `UsuarioAutorizadoService` e `UsuarioAutorizadoServiceImpl`

#### 3.1 `UsuarioAutorizadoService.java`

FQN: `com.educaflow.subsystem.registrousuario.service.UsuarioAutorizadoService`

```java
package com.educaflow.subsystem.registrousuario.service;

import com.axelor.db.modelservice.ModelService;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado;

import java.util.Optional;

public interface UsuarioAutorizadoService extends ModelService<UsuarioAutorizado> {

    /**
     * Busca un UsuarioAutorizado por la clave natural (centro, dni normalizado,
     * tipoUsuario, curso). Devuelve Optional vacío si no existe ningún registro
     * con esa combinación exacta.
     * Usado en R-010 y R-011 para verificar existencia antes de insertar.
     */
    Optional<UsuarioAutorizado> findByCentroDniTipoUsuarioCurso(
            Centro centro, String dni, TipoUsuario tipoUsuario, Integer curso);
}
```

Regla: NO se re-declaran `validateInsert`, `validateUpdate` ni `validateRemove` (se heredan de `ModelService<T>`).

#### 3.2 `UsuarioAutorizadoServiceImpl.java`

FQN: `com.educaflow.subsystem.registrousuario.service.impl.UsuarioAutorizadoServiceImpl`

```java
package com.educaflow.subsystem.registrousuario.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.axelor.db.modelservice.DefaultModelService;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado;
import com.educaflow.subsystem.registrousuario.db.repo.AbstractUsuarioAutorizadoRepository;
import com.educaflow.subsystem.registrousuario.service.UsuarioAutorizadoService;

import java.time.LocalDateTime;
import java.util.Optional;

public class UsuarioAutorizadoServiceImpl
        extends DefaultModelService<UsuarioAutorizado>
        implements UsuarioAutorizadoService {

    /**
     * Constructor obligatorio — ModelServiceFactory lo invoca por reflexión.
     * No crear módulo Guice para este servicio: la factoría lo descubre por
     * convención de paquetes (service.impl.*ServiceImpl).
     */
    public UsuarioAutorizadoServiceImpl(Class<UsuarioAutorizado> model,
                                        Repository<UsuarioAutorizado> repository) {
        super(model, repository);
    }

    // -------------------------------------------------------------------------
    // CRUD — insert con regla de negocio
    // -------------------------------------------------------------------------

    /**
     * R-010 (parte de persistencia): antes de persistir asigna fechaExportacion
     * al instante actual (lógica de sistema). Los demás campos (centro, dni, tipoUsuario,
     * curso) ya vienen rellenos por el importador. Llama a super.insert(entidad), que
     * a su vez invoca validateInsert (V-001) antes de persistir.
     */
    @Override
    public UsuarioAutorizado insert(UsuarioAutorizado entidad);

    // -------------------------------------------------------------------------
    // Validaciones (V-XXX)
    // -------------------------------------------------------------------------

    /**
     * V-001 — Unicidad (centro, dni, tipoUsuario, curso).
     * Comprueba si ya existe un UsuarioAutorizado con la misma combinación de los
     * cuatro campos antes de persistir. Si ya existe, devuelve BusinessMessages con
     * un mensaje que transmite: el DNI recibido, el nombre del centro, el nombre del
     * tipo de usuario y el valor del curso.
     * V-002..V-004 (nulidad de centro, dni y tipoUsuario) están cubiertas por
     * required=true en el XML de dominio; no se duplican aquí.
     */
    @Override
    public Optional<BusinessMessages> validateInsert(UsuarioAutorizado entidad);

    // -------------------------------------------------------------------------
    // Consultas (delegadas al repositorio)
    // -------------------------------------------------------------------------

    /**
     * Delega en el finder generado en AbstractUsuarioAutorizadoRepository.
     * El cast del campo `repository` heredado de DefaultModelService permite
     * acceder al método generado findByCentroAndDniAndTipoUsuarioAndCurso(...).
     * Envuelve el resultado en Optional.ofNullable.
     */
    @Override
    public Optional<UsuarioAutorizado> findByCentroDniTipoUsuarioCurso(
            Centro centro, String dni, TipoUsuario tipoUsuario, Integer curso);

    // -------------------------------------------------------------------------
    // Action Rules privadas
    // -------------------------------------------------------------------------

    /**
     * R-010 (asignación): establece fechaExportacion=LocalDateTime.now() en el
     * UsuarioAutorizado antes de que super.insert() lo persista. Invocado desde
     * insert() antes de super.insert().
     */
    private void fireActionRule_asignarFechaExportacion(UsuarioAutorizado entidad);
}
```

**Verificación:** `./gradlew clean build --info` compila sin errores; `ModelServiceFactory` descubre la implementación por convención.

---

### Paso 4 — Crear `UsuarioAutorizadoRepository`

Fichero: `src/main/java/com/educaflow/subsystem/registrousuario/db/repo/UsuarioAutorizadoRepository.java`

FQN: `com.educaflow.subsystem.registrousuario.db.repo.UsuarioAutorizadoRepository`

```java
package com.educaflow.subsystem.registrousuario.db.repo;

/**
 * Repositorio concreto de UsuarioAutorizado. Necesario porque la entidad declara
 * repository="abstract" en el XML de dominio, lo que genera solo AbstractUsuarioAutorizadoRepository.
 * Esta clase concreta es el punto que Axelor inyecta como Repository<UsuarioAutorizado>
 * en UsuarioAutorizadoServiceImpl. No añade métodos adicionales: el finder
 * findByCentroAndDniAndTipoUsuarioAndCurso está generado en la clase abstracta.
 */
public class UsuarioAutorizadoRepository extends AbstractUsuarioAutorizadoRepository {
}
```

**Verificación:** `./gradlew clean build --info` compila; la inyección en `UsuarioAutorizadoServiceImpl` resuelve correctamente.

---

### Paso 5 — Modificar `ResultadoImportacion`

Fichero: `src/main/java/com/educaflow/subsystem/importacion/importador/ResultadoImportacion.java`

FQN: `com.educaflow.subsystem.importacion.importador.ResultadoImportacion`

```java
package com.educaflow.subsystem.importacion.importador;

import com.educaflow.subsystem.common.db.Centro;

/**
 * DTO inmutable con el resultado de una importación completada (R-014).
 *
 * Cambios respecto al estado anterior:
 *   - usuariosImportados → creados
 *   - numeroErrores      → errores
 *   - Añadido: ignorados (DNIs válidos ya existentes en la combinación clave)
 */
public record ResultadoImportacion(
        int creados,
        int ignorados,
        int errores,
        String log,
        Centro centro,
        Integer curso
) {}
```

**Verificación:** `./gradlew clean build --info` falla si hay usos de `resultado.usuariosImportados()` o `resultado.numeroErrores()` en `TareaImportacionServiceImpl` (los detecta y fuerza a actualizar).

---

### Paso 6 — Modificar `ImportadorFicheroFactory`

Fichero: `src/main/java/com/educaflow/subsystem/importacion/importador/ImportadorFicheroFactory.java`

FQN: `com.educaflow.subsystem.importacion.importador.ImportadorFicheroFactory`

Firma del método actualizada:

```java
/**
 * Crea el importador correspondiente al tipo de fichero.
 * Cambio: tercer parámetro ModelServiceFactory, necesario para que
 * ImportadorUsuarioCSV pueda resolver UsuarioAutorizadoService
 * sin @Inject (la clase no es un bean Guice).
 * ImportadorUsuarioXML recibe el parámetro igualmente pero puede ignorarlo
 * (la firma es uniforme). El switch no cambia su estructura.
 */
public static ImportadorFichero create(TipoFicheroImportacion tipoFichero,
                                       MetaFile fichero,
                                       ModelServiceFactory modelServiceFactory);
```

**Verificación:** `./gradlew clean build --info` detecta la llamada en `TareaImportacionServiceImpl` que debe actualizarse para pasar tres argumentos.

---

### Paso 7 — Implementar `ImportadorUsuarioCSV`

Fichero: `src/main/java/com/educaflow/subsystem/importacion/importador/impl/ImportadorUsuarioCSV.java`

FQN: `com.educaflow.subsystem.importacion.importador.impl.ImportadorUsuarioCSV`

#### Constructor actualizado

```java
/**
 * Recibe ModelServiceFactory para resolver UsuarioAutorizadoService dentro de
 * importar() sin @Inject (la clase no es un bean gestionado por Guice).
 */
public ImportadorUsuarioCSV(MetaFile fichero,
                             TipoFicheroImportacion tipoFichero,
                             ModelServiceFactory modelServiceFactory);
// Almacena los tres parámetros como campos finales privados.
```

#### Método público — contrato sin cambios

```java
/**
 * Orquesta el pipeline completo (R-001..R-016) delegando en métodos privados:
 *   1. resolverContexto()      → R-001..R-004 — aborta si falla
 *   2. leerLineas()            → R-005        — aborta si falla
 *   3. procesarLineas(...)     → R-006..R-012 — acumula resultado
 *   4. componerLog(...)        → R-013        — compone el texto del log
 * Devuelve ResultadoImportacion(creados, ignorados, errores, log, centro, curso) (R-014).
 * Propaga ImportadorException sin capturar cuando resolverContexto() o leerLineas() fallan
 * (TareaImportacionServiceImpl la captura → estado=false, R-015).
 */
@Override
public ResultadoImportacion importar() throws ImportadorException;
```

#### Record privado de soporte

```java
/**
 * Datos inmutables resueltos antes del bucle principal:
 * centro activo, curso activo y TipoUsuario con código "PROFESOR_EXTERNO".
 */
private record ContextoImportacion(Centro centro, Integer curso, TipoUsuario tipoUsuario) {}
```

#### Clase privada mutable de acumulación

```java
/**
 * Acumulador de contadores y entradas de log durante el procesamiento.
 * Mutable por diseño — se actualiza línea a línea dentro del bucle.
 * Campos: int creados, int ignorados, int errores, List<String> entradasLog.
 * Métodos: incrementarCreados(), incrementarIgnorados(), añadirEntradaLog(String).
 */
private static final class ContadorImportacion { ... }
```

#### Métodos privados de descomposición

```java
/**
 * R-001 — Obtiene AuthUtils.getUser().getCentroActivo().
 * R-002 — Si centro es null → lanza ImportadorException con mensaje:
 *          "Importación abortada: el importador no tiene centro activo asignado."
 * R-003 — Si centro.getCurso() es null → lanza ImportadorException con mensaje:
 *          "Importación abortada: el centro '{centro.getName()}' no tiene curso activo asignado."
 * R-004 — Busca TipoUsuario con código "PROFESOR_EXTERNO" vía:
 *          ((AbstractTipoUsuarioRepository) JpaRepository.of(TipoUsuario.class)).findByCodigo("PROFESOR_EXTERNO")
 *          Si no existe → lanza ImportadorException con mensaje:
 *          "Importación abortada: error de configuración, no existe el tipo de usuario con código 'PROFESOR_EXTERNO'."
 *          El mapeo enum→código es explícito ("PROFESOR_EXTERNO"), no por convención (A1*).
 */
private ContextoImportacion resolverContexto() throws ImportadorException;

/**
 * R-005 — Obtiene el Path del MetaFile con MetaFiles y lee todas las líneas
 *          con Files.readAllLines(path, StandardCharsets.UTF_8) (A2: UTF-8).
 *          Captura IOException y lanza ImportadorException con mensaje:
 *          "No se ha podido leer el fichero CSV. Motivo: {ex.getMessage()}."
 *          Devuelve la lista completa incluyendo líneas vacías (necesarias para
 *          mantener la numeración 1-based, R-006, A3).
 */
private List<String> leerLineas() throws ImportadorException;

/**
 * R-006 — Recorre la lista con índice 0-based, número de línea = índice+1 (1-based).
 * R-007 — Si la línea es vacía o solo espacios, incrementa el número de línea pero
 *          no llama a procesarLinea (no cuenta en ningún contador, no aparece en el log).
 * Para cada línea no vacía, delega en procesarLinea(...) y actualiza el ContadorImportacion.
 * Al finalizar, llama a componerLog() y devuelve ResultadoImportacion.
 * Resuelve UsuarioAutorizadoService una sola vez antes del bucle:
 *   (UsuarioAutorizadoService) modelServiceFactory.resolve(UsuarioAutorizado.class)
 */
private ResultadoImportacion procesarLineas(List<String> lineas,
                                             ContextoImportacion ctx);

/**
 * R-007 — Llamado solo para líneas no vacías (el filtro está en procesarLineas).
 * R-008 — dniLeido = linea.trim() (para log, A4); dniNormalizado = DniUtil.clean(dniLeido).
 * R-009 — Si !DniUtil.isValid(dniNormalizado): incrementa errores, añade entrada al log
 *          con número de línea, dniLeido y motivo "DNI no válido". Retorna sin abortar.
 * Llama a procesarDni(...) para el caso válido.
 * R-012 — Captura cualquier excepción inesperada: incrementa errores, añade entrada al log
 *          con número de línea, dniLeido y motivo "Error inesperado: {ex.getMessage()}".
 */
private void procesarLinea(int numeroLinea,
                            String linea,
                            ContextoImportacion ctx,
                            UsuarioAutorizadoService svc,
                            ContadorImportacion contador);

/**
 * R-010 — Llama a svc.findByCentroDniTipoUsuarioCurso(ctx.centro(), dniNormalizado,
 *          ctx.tipoUsuario(), ctx.curso()). Si devuelve Optional vacío:
 *          crea new UsuarioAutorizado() con los cuatro campos rellenos
 *          (centro, dniNormalizado, tipoUsuario, curso; fechaExportacion la asigna
 *          UsuarioAutorizadoServiceImpl.insert vía fireActionRule_asignarFechaExportacion)
 *          y llama a svc.insert(ua). Incrementa contador.creados.
 *          Las creaciones exitosas no se añaden al log individualmente.
 * R-011 — Si findByCentroDniTipoUsuarioCurso devuelve Optional con valor:
 *          Incrementa contador.ignorados. Añade entrada al log con número de línea,
 *          dniLeido y motivo "Ya existe".
 */
private void procesarDni(int numeroLinea,
                          String dniLeido,
                          String dniNormalizado,
                          ContextoImportacion ctx,
                          UsuarioAutorizadoService svc,
                          ContadorImportacion contador);

/**
 * R-013 — Construye el texto del log en castellano:
 *   Cabecera: "Creados: {n}\nIgnorados: {n}\nErrores: {n}"
 *   Seguida de una línea por cada entrada de ContadorImportacion.entradasLog,
 *   en orden de aparición (errores e ignorados mezclados cronológicamente).
 *   Las creaciones exitosas no aparecen en el detalle.
 */
private String componerLog(ContadorImportacion contador);
```

**Verificación:** `./gradlew clean build --info`; también buscar con grep que `resolverContexto`, `leerLineas`, `procesarLineas`, `procesarLinea`, `procesarDni` y `componerLog` están referenciados desde `importar()`.

---

### Paso 8 — Modificar `TareaImportacionServiceImpl`

Fichero: `src/main/java/com/educaflow/subsystem/importacion/service/impl/TareaImportacionServiceImpl.java`

FQN: `com.educaflow.subsystem.importacion.service.impl.TareaImportacionServiceImpl`

**Campo añadido:**

```java
// Inyectado para pasarlo a ImportadorFicheroFactory.create(...).
// Permite que ImportadorUsuarioCSV resuelva UsuarioAutorizadoService
// sin @Inject en el importador.
@Inject
private ModelServiceFactory modelServiceFactory;
```

**Método modificado:**

```java
/**
 * R-014 / R-015: cambios respecto al estado anterior:
 *   1. Pasa modelServiceFactory como tercer argumento a ImportadorFicheroFactory.create().
 *   2. tarea.setLog(resultado.log()) — sin el prefijo "Importación finalizada. " que
 *      contradice R-013 (el log ya empieza con "Creados: {n}\nIgnorados: {n}\nErrores: {n}").
 * El resto no cambia: estado=true + centro + curso + fechaExportacion en éxito (R-014);
 * estado=false + log=ex.getMessage() en ImportadorException capturada (R-015).
 */
private void fireActionRule_ejecutarImportacion(TareaImportacion tareaImportacion);
```

**Verificación:** `./gradlew clean build --info`; comprobar que no queda ninguna referencia a `resultado.usuariosImportados()` ni a `resultado.numeroErrores()`.

---

### Paso 9 — Verificación final

```bash
./gradlew clean build --info
```

La compilación verifica que:
- El XML de `UsuarioAutorizado.xml` regenera `AbstractUsuarioAutorizadoRepository` con `findByCentroAndDniAndTipoUsuarioAndCurso` y el nuevo unique-constraint de 4 columnas.
- El XML de `TipoUsuario.xml` regenera `AbstractTipoUsuarioRepository` con `findByCodigo(String)`.
- `UsuarioAutorizadoRepository` extiende el abstracto correctamente.
- `UsuarioAutorizadoServiceImpl` compila usando el cast al repositorio abstracto.
- `ImportadorUsuarioCSV` compila con el constructor de tres parámetros.
- `ResultadoImportacion` con `creados`/`ignorados`/`errores` hace fallar cualquier uso de los campos renombrados, forzando actualización.
- `ImportadorFicheroFactory.create()` compila con los tres argumentos; `TareaImportacionServiceImpl` los pasa correctamente.

---

## Vistas

No aplica. Esta iniciativa no crea ni modifica vistas. Reutiliza las vistas existentes de `subsystem/importacion` del iniciativa anterior `0003_importacion-vistas`.

---

## Seguridad

Sin cambios. La seguridad (grupo `admins`, acceso a TareaImportacion) está gestionada por la iniciativa anterior y no se toca.

---

## Datos iniciales

Sin cambios. El catálogo de `TipoUsuario` con código `PROFESOR_EXTERNO` debe existir en BD (gestionado por datos iniciales existentes de `common`).

---

## Matriz de trazabilidad

### Validaciones V-XXX

| ID | Regla | Capa | Clase.Método o Fichero.Elemento |
|----|-------|------|--------------------------------|
| V-001 | Unicidad (centro, dni, tipoUsuario, curso) | Dominio XML + servidor | `UsuarioAutorizado.xml` → `<unique-constraint columns="centro,dni,tipoUsuario,curso"/>` (barrera BD) + `UsuarioAutorizadoServiceImpl.validateInsert()` (barrera Java, mensaje al usuario) |
| V-002 | `UsuarioAutorizado.centro` obligatorio | Dominio XML | `UsuarioAutorizado.xml` → `<many-to-one name="centro" required="true"/>` — existente, sin código nuevo |
| V-003 | `UsuarioAutorizado.dni` obligatorio | Dominio XML | `UsuarioAutorizado.xml` → `<string name="dni" required="true"/>` — existente, sin código nuevo |
| V-004 | `UsuarioAutorizado.tipoUsuario` obligatorio | Dominio XML | `UsuarioAutorizado.xml` → `<many-to-one name="tipoUsuario" required="true"/>` — existente, sin código nuevo |

### Reglas de negocio R-XXX

| ID | Descripción resumida | Capa | Clase.Método |
|----|----------------------|------|-------------|
| R-001 | Determinar centro activo del importador | Importador | `ImportadorUsuarioCSV.resolverContexto()` — `AuthUtils.getUser().getCentroActivo()` |
| R-002 | Sin centro activo → abortar | Importador | `ImportadorUsuarioCSV.resolverContexto()` → lanza `ImportadorException` con mensaje fijo; capturado en `TareaImportacionServiceImpl.fireActionRule_ejecutarImportacion()` → `estado=false` |
| R-003 | Centro sin curso activo → abortar | Importador | `ImportadorUsuarioCSV.resolverContexto()` → lanza `ImportadorException` con nombre del centro; capturado en `fireActionRule_ejecutarImportacion()` |
| R-004 | TipoUsuario PROFESOR_EXTERNO no existe → abortar | Importador | `ImportadorUsuarioCSV.resolverContexto()` → finder `AbstractTipoUsuarioRepository.findByCodigo("PROFESOR_EXTERNO")`; si null, lanza `ImportadorException` |
| R-005 | CSV ilegible → abortar | Importador | `ImportadorUsuarioCSV.leerLineas()` → captura `IOException`, lanza `ImportadorException` |
| R-006 | Numeración 1-based incluyendo líneas en blanco | Importador | `ImportadorUsuarioCSV.procesarLineas()` → bucle con índice explícito 0-based, `numeroLinea = i+1` |
| R-007 | Líneas vacías → ignorar silenciosamente | Importador | `ImportadorUsuarioCSV.procesarLineas()` → `if (linea.isBlank()) continue` (no llama a procesarLinea; el número de línea sí avanza) |
| R-008 | Normalizar DNI con DniUtil.clean() | Importador | `ImportadorUsuarioCSV.procesarLinea()` → `DniUtil.clean(linea.trim())` |
| R-009 | DNI inválido → error individual, continuar | Importador | `ImportadorUsuarioCSV.procesarLinea()` → `!DniUtil.isValid(dniNorm)` → `contador.incrementarErrores()` + entrada de log con `dniLeido` |
| R-010 | DNI válido + no existe → crear UsuarioAutorizado con fechaExportacion=now() | Importador + Servicio | `ImportadorUsuarioCSV.procesarDni()` → `svc.insert(ua)` + `UsuarioAutorizadoServiceImpl.insert()` → `fireActionRule_asignarFechaExportacion()` establece `fechaExportacion=now()` antes de `super.insert()` |
| R-011 | DNI válido + ya existe → ignorar, loguear | Importador | `ImportadorUsuarioCSV.procesarDni()` → `svc.findByCentroDniTipoUsuarioCurso()` devuelve presente → `contador.incrementarIgnorados()` + entrada de log con `dniLeido` y motivo "Ya existe" |
| R-012 | Excepción inesperada por línea → capturar, continuar | Importador | `ImportadorUsuarioCSV.procesarLinea()` → bloque `catch(Exception e)` sobre la llamada a `procesarDni()` → `contador.incrementarErrores()` + entrada de log con motivo "Error inesperado: {detalle}" |
| R-013 | Componer log: cabecera de contadores + detalle en orden | Importador | `ImportadorUsuarioCSV.componerLog(ContadorImportacion)` → "Creados: {n}\nIgnorados: {n}\nErrores: {n}" + entradas de `ContadorImportacion.entradasLog` |
| R-014 | Procesamiento completado → estado=true, log, centro, curso, fechaExportacion=now() | Servicio | `TareaImportacionServiceImpl.fireActionRule_ejecutarImportacion()` → rama `try`: `setEstado(true)`, `setCentro(resultado.centro())`, `setCurso(resultado.curso())`, `setLog(resultado.log())`, `setFechaExportacion(now())` |
| R-015 | Aborto global → estado=false, sin fechaExportacion, log=mensaje de causa | Servicio | `TareaImportacionServiceImpl.fireActionRule_ejecutarImportacion()` → rama `catch(ImportadorException ex)`: `setEstado(false)`, `setLog(ex.getMessage())` |
| R-016 | (Heredada) usuario=importador autenticado, fechaImportacion=now() | Servicio | `TareaImportacionServiceImpl.fireActionRule_asignarCamposSistema()` — ya implementada, sin cambios |

### Reglas de UI U-XXX

Sin entradas. Esta iniciativa no añade reglas de UI.

---

## Notas de unificación

1. **fechaExportacion de UsuarioAutorizado**: se asigna en `UsuarioAutorizadoServiceImpl.fireActionRule_asignarFechaExportacion()` (dentro del `insert()` del servicio), no en el importador. Esto respeta el patrón del proyecto donde los campos de sistema los asigna el servicio. La alternativa (asignarla en el importador) funcionaría pero viola el principio de que la lógica de dominio va en el servicio.

2. **Repositorio concreto `UsuarioAutorizadoRepository`**: necesario porque el XML declara `repository="abstract"`, lo que genera solo el abstracto. El concreto es el punto de inyección de Axelor para `Repository<UsuarioAutorizado>` en el constructor del servicio. Solo 1 de los 5 subagentes lo detectó; el resto asumía que el abstracto era suficiente (incorrecto en Axelor).

3. **Nombre del finder XML vs. nombre del método de servicio**: el XML usa `findByCentroAndDniAndTipoUsuarioAndCurso` (convención Axelor con `And`); el método público de la interfaz usa `findByCentroDniTipoUsuarioCurso` (más idiomático en Java). El `impl` hace el cast al repositorio abstracto y llama al nombre generado.

4. **validateInsert solo cubre V-001**: V-002..V-004 están garantizadas por `required=true` en el XML y se listan en la trazabilidad solo por completitud (el análisis los relista "por trazabilidad"). No se añade código de validación Java para ellas.

5. **Prefijo del log eliminado**: `"Importación finalizada. "` se eliminó de `fireActionRule_ejecutarImportacion` porque contradice R-013 (el log debe empezar con "Creados: {n}"). Todos los subagentes coincidieron en este cambio.