---
type: design
---

# Diseño: Importación de usuarios autorizados desde CSV

**Objetivo:** Implementar la lógica de importación masiva de usuarios autorizados a partir de un fichero CSV (un DNI por línea), creando registros de `UsuarioAutorizado` para el centro y curso activos del importador, y registrando un log con el resumen y los errores del proceso.
**Capa:** `subsystem/importacion` + `subsystem/registrousuario` + `subsystem/common`
**Análisis de origen:** `.sdd/drafts/2026-05-14_11-54_importacion-usuarios-csv/analysis_01/analysis.md`
**Skills necesarios para la implementación:** k-sistemas, k-validaciones

---

## Ficheros a crear o modificar

| Fichero | Acción | Descripción |
|---------|--------|-------------|
| `subsystem/registrousuario/domains/UsuarioAutorizado.xml` | MODIFICAR | Cambiar `unique-constraint` de `(centro,dni,tipoUsuario)` a `(centro,dni,tipoUsuario,curso)` |
| `subsystem/registrousuario/db/repo/UsuarioAutorizadoRepository.java` | CREAR | Repositorio concreto con finder para detectar duplicados; maneja `curso=null` |
| `subsystem/common/db/repo/TipoUsuarioRepository.java` | CREAR | Repositorio concreto con finder por código |
| `subsystem/importacion/importador/ResultadoImportacion.java` | MODIFICAR | Añadir campo `int usuariosIgnorados` al record |
| `subsystem/importacion/importador/impl/ImportadorUsuarioCSV.java` | MODIFICAR | Implementar el método `importar()` |

Los ficheros siguientes **no se modifican**:
- `TareaImportacion.xml`, `TareaImportacionService.java`, `TareaImportacionServiceImpl.java`
- `TareaImportacionController.java`, vistas `TareaImportacion.xml`
- `ImportadorFichero.java`, `ImportadorFicheroFactory.java`, `ImportadorException.java`
- `TipoUsuario.xml` — sin cambios de dominio; la búsqueda por código va al repositorio

---

## Pasos

### Paso 1 — Dominio: modificar `UsuarioAutorizado.xml`

**Fichero:** `src/main/java/com/educaflow/subsystem/registrousuario/domains/UsuarioAutorizado.xml`

Cambiar la cláusula `<unique-constraint>` para incluir el campo `curso`, de forma que la combinación `(centro, dni, tipoUsuario, curso)` sea única en base de datos.

XML completo resultante:

```xml
<domain-models xmlns="http://axelor.com/xml/ns/domain-models"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://axelor.com/xml/ns/domain-models https://axelor.com/xml/ns/domain-models/domain-models_8.1.xsd">

    <module name="registro" package="com.educaflow.subsystem.registrousuario.db"/>
    <entity name="UsuarioAutorizado" repository="abstract">
        <many-to-one name="centro" ref="com.educaflow.subsystem.common.db.Centro" required="true"/>
        <string name="dni" title="dni__!!" required="true"/>
        <many-to-one name="tipoUsuario" ref="com.educaflow.subsystem.common.db.TipoUsuario" required="true" title="Tipo de usuario"/>
        <integer name="curso" />
        <date name="fechaExportacion" title="Fecha de exportación"/>
        <unique-constraint columns="centro,dni,tipoUsuario,curso"/>
    </entity>

</domain-models>
```

---

### Paso 2 — Repositorio: crear `UsuarioAutorizadoRepository`

**Fichero:** `src/main/java/com/educaflow/subsystem/registrousuario/db/repo/UsuarioAutorizadoRepository.java`
**Paquete:** `com.educaflow.subsystem.registrousuario.db.repo`
**Extiende:** `AbstractUsuarioAutorizadoRepository`

Métodos:

```
Optional<UsuarioAutorizado> findByCentroDniTipoUsuarioCurso(Centro centro, String dni, TipoUsuario tipoUsuario, Integer curso)
```
Ejecuta una query JPQL sobre `UsuarioAutorizado` filtrando por `self.centro = :centro AND self.dni = :dni AND self.tipoUsuario = :tipoUsuario` más la condición de curso:
- Si `curso` es **no nulo**: añade `AND self.curso = :curso` y hace `.bind("curso", curso)`.
- Si `curso` es **nulo**: añade `AND self.curso IS NULL` (sin parámetro `:curso`).

Devuelve `Optional.ofNullable(fetchOne())`.

> **Nota sobre `curso=null`:** La query `self.curso = :curso` con `:curso = null` devuelve cero filas en SQL (NULL ≠ NULL). Por eso es obligatorio bifurcar el filtro según si `curso` es null o no.

---

### Paso 3 — Repositorio: crear `TipoUsuarioRepository`

**Fichero:** `src/main/java/com/educaflow/subsystem/common/db/repo/TipoUsuarioRepository.java`
**Paquete:** `com.educaflow.subsystem.common.db.repo`
**Extiende:** `AbstractTipoUsuarioRepository`

Métodos:

```
Optional<TipoUsuario> findByCodigo(String codigo)
```
Ejecuta `.all().filter("self.codigo = :codigo").bind("codigo", codigo).fetchOne()` y devuelve `Optional.ofNullable(resultado)`.

---

### Paso 4 — Record: modificar `ResultadoImportacion`

**Fichero:** `src/main/java/com/educaflow/subsystem/importacion/importador/ResultadoImportacion.java`
**Paquete:** `com.educaflow.subsystem.importacion.importador`

Record completo resultante (añadir campo `usuariosIgnorados`):

```java
package com.educaflow.subsystem.importacion.importador;

import com.educaflow.subsystem.common.db.Centro;

public record ResultadoImportacion(
        int usuariosImportados,
        int usuariosIgnorados,
        int numeroErrores,
        String log,
        Centro centro,
        Integer curso
) {}
```

---

### Paso 5 — Importador: implementar `ImportadorUsuarioCSV.importar()`

**Fichero:** `src/main/java/com/educaflow/subsystem/importacion/importador/impl/ImportadorUsuarioCSV.java`
**Paquete:** `com.educaflow.subsystem.importacion.importador.impl`
**Implementa:** `ImportadorFichero`

La clase mantiene los dos campos finales del constructor (`fichero`, `tipoFichero`) y añade la implementación del método `importar()`.

Firma del método público:
```
ResultadoImportacion importar() throws ImportadorException
```

**Descripción del cuerpo** (sin código implementado):

El método delega toda la lógica en métodos privados de propósito único, siguiendo el principio de métodos pequeños:

1. **Obtener centro activo** (R-002): Llama a un método privado `obtenerCentroActivo()` que llama a `AuthUtils.getUser().getCentroActivo()`. Si el resultado es `null`, lanza `ImportadorException` con el mensaje `"El importador no tiene centro activo asignado."` (esto aborta el proceso; `TareaImportacionServiceImpl` captura la excepción y guarda el log con `estado=false`).

2. **Obtener curso activo**: Llama a un método privado `obtenerCurso(Centro centro)` que devuelve `centro.getCurso()` (puede ser null; se acepta y se propaga al `UsuarioAutorizado`).

3. **Obtener TipoUsuario** para PROFESOR_EXTERNO: Llama a un método privado `obtenerTipoUsuario()` que hace `Beans.get(TipoUsuarioRepository.class).findByCodigo("PROFESOR_EXTERNO")` y lanza `ImportadorException` con `"No se encontró el tipo de usuario PROFESOR_EXTERNO."` si no existe.

4. **Leer CSV** (R-003): Llama a un método privado `leerLineasCsv()` que obtiene los bytes con `MetaFileUtil.downloadContent(fichero)`, los convierte a `String` con `StandardCharsets.UTF_8`, divide por `\n` y devuelve la lista de líneas como `List<String>`.

5. **Procesar líneas** (R-003, R-004, R-005, R-006): Llama a un método privado `procesarLineas(List<String> lineas, Centro centro, Integer curso, TipoUsuario tipoUsuario)` que itera cada línea:
   - Normaliza: `.trim()` seguido de `.toUpperCase()` (equivalente a `DniUtil.clean(linea)` — ver siguiente punto).
   - Descarta silenciosamente las líneas vacías tras normalizar.
   - Llama a `DniUtil.clean(linea)` para normalizar el DNI (elimina prefijos especiales).
   - Valida con `DniUtil.isValid(dni)`. Si inválido: anota el error como `dni + ": DNI no válido"`, incrementa `errores`, continúa.
   - Busca duplicado: `Beans.get(UsuarioAutorizadoRepository.class).findByCentroDniTipoUsuarioCurso(centro, dni, tipoUsuario, curso)`. Si existe: incrementa `ignorados`, continúa.
   - Si no existe: crea un nuevo `UsuarioAutorizado` con `centro`, `dni`, `tipoUsuario` y `curso`; lo persiste con `Beans.get(UsuarioAutorizadoRepository.class).save(nuevoRegistro)`; incrementa `importados`.
   - Devuelve un objeto interno `ContadoresImportacion(importados, ignorados, errores, listaErrores)` (record o clase privada de apoyo dentro del importador).

6. **Construir log** (R-007): Llama a un método privado `construirLog(ContadoresImportacion contadores)` que devuelve un `String` con el formato:
   ```
   Importados: {importados}. Ignorados: {ignorados}. Errores: {errores}.
   {si hay errores → salto de línea + listado de errores, uno por línea}
   ```

7. **Devolver resultado**: Construye y devuelve `new ResultadoImportacion(importados, ignorados, errores, log, centro, curso)`.

**Clases de apoyo internas** (privadas, dentro del mismo fichero o como record privado dentro de la clase):

```
private record ContadoresImportacion(
    int importados,
    int ignorados,
    int errores,
    List<String> listaErrores
)
```

**Imports necesarios:**
- `com.axelor.auth.AuthUtils`
- `com.axelor.inject.Beans`
- `com.educaflow.base.util.DniUtil`
- `com.educaflow.base.util.MetaFileUtil`
- `com.educaflow.subsystem.common.db.Centro`
- `com.educaflow.subsystem.common.db.TipoUsuario`
- `com.educaflow.subsystem.common.db.repo.TipoUsuarioRepository`
- `com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado`
- `com.educaflow.subsystem.registrousuario.db.repo.UsuarioAutorizadoRepository`
- `java.nio.charset.StandardCharsets`
- `java.util.ArrayList`
- `java.util.List`

---

### Paso 6 — Verificación final

Compilar el proyecto para verificar que no hay errores:

```bash
./gradlew clean build --info
```

Se espera BUILD SUCCESSFUL sin errores de compilación. Prestar especial atención a:
- La clase generada `AbstractUsuarioAutorizadoRepository` incluye el repositorio concreto en el mismo paquete.
- La clase generada `AbstractTipoUsuarioRepository` incluye el repositorio concreto en el mismo paquete.
- El record `ResultadoImportacion` compila con los 6 campos.
- `TareaImportacionServiceImpl` sigue compilando sin cambios (accede a `resultado.log()`, `resultado.centro()`, `resultado.curso()` — todos siguen existiendo en la misma posición).

---

## Matriz de trazabilidad V-XXX / R-XXX / U-XXX

| Regla | Capa | Ubicación | Descripción |
|---|---|---|---|
| V-001 | servidor + cliente | `TareaImportacionServiceImpl.validateInsert` + action-condition existente | Ya implementada: bloquea si `tipoFichero` es null |
| V-002 | servidor + cliente | `TareaImportacionServiceImpl.validateInsert` + action-condition existente | Ya implementada: bloquea si `fichero` es null |
| R-001 | servidor | `TareaImportacionServiceImpl.fireActionRule_asignarCamposSistema` | Ya implementada: asigna usuario, fechaImportacion, estado=false, log=null |
| R-002 | servidor | `ImportadorUsuarioCSV.importar()` → `obtenerCentroActivo()` | Nuevo: lanza `ImportadorException` si el usuario no tiene centro activo |
| R-003 | servidor | `ImportadorUsuarioCSV.importar()` → `leerLineasCsv()` + `procesarLineas()` | Nuevo: lee CSV línea a línea con UTF-8, normaliza con `DniUtil.clean`, descarta vacías |
| R-004 | servidor | `ImportadorUsuarioCSV.importar()` → `procesarLineas()` | Nuevo: valida DNI con `DniUtil.isValid`; anota error y continúa si inválido |
| R-005 | servidor | `ImportadorUsuarioCSV.importar()` → `procesarLineas()` + `UsuarioAutorizadoRepository.save` | Nuevo: crea `UsuarioAutorizado` si no existe el combo exacto |
| R-006 | servidor | `ImportadorUsuarioCSV.importar()` → `procesarLineas()` | Nuevo: incrementa ignorados si el combo ya existe; no modifica el registro existente |
| R-007 | servidor | `ImportadorUsuarioCSV.importar()` → `construirLog()` | Nuevo: construye log con contadores y errores; devuelve `ResultadoImportacion` |
| U-001 | vista | `panelEntrada showIf="id == null"` en `TareaImportacion.xml` | Ya implementada: panel de entrada editable solo para registros nuevos |
| U-002 | vista | `panelResultado showIf="id != null"` en `TareaImportacion.xml` | Ya implementada: panel de resultado readonly para registros guardados |
