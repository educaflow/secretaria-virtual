---
type: design
---

# Diseño: Importación de usuarios autorizados desde CSV

**Objetivo:** Implementar `ImportadorUsuarioCSV.importar()` para la importación masiva de profesores externos desde un fichero CSV (un DNI por línea), ampliar la constraint de unicidad de `UsuarioAutorizado` para incluir el campo `curso`, y crear los repositorios concretos necesarios para las queries de deduplicación y resolución de tipo de usuario.
**Capa:** `subsystem/importacion` + `subsystem/registrousuario` + `subsystem/common`
**Análisis de origen:** `.sdd/drafts/2026-05-14_11-54_importacion-usuarios-csv/analysis_01/analysis.md`
**Skills necesarios para la implementación:** k-sistemas, k-validaciones

---

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `subsystem/registrousuario/domains/UsuarioAutorizado.xml` | MODIFICAR | k-sistemas (modelos.md) | Cambiar `unique-constraint` de `(centro,dni,tipoUsuario)` a `(centro,dni,tipoUsuario,curso)` |
| `subsystem/registrousuario/db/repo/UsuarioAutorizadoRepository.java` | CREAR | k-sistemas (referencias/repositories.md) | Repositorio concreto con finder para detectar duplicados; bifurcación explícita para `curso=null` |
| `subsystem/common/db/repo/TipoUsuarioRepository.java` | CREAR | k-sistemas (referencias/repositories.md) | Repositorio concreto con `findByCodigo` para resolver el TipoUsuario por código |
| `subsystem/importacion/importador/ResultadoImportacion.java` | MODIFICAR | k-sistemas (servicios.md) | Añadir campo `int usuariosIgnorados` al record (de 5 a 6 componentes) |
| `subsystem/importacion/importador/impl/ImportadorUsuarioCSV.java` | MODIFICAR | k-sistemas (servicios.md) | Implementar `importar()` descompuesto en métodos privados de propósito único |

Los ficheros siguientes **no se modifican**:
- `TareaImportacion.xml`, `TareaImportacionService.java`, `TareaImportacionServiceImpl.java`
- `TareaImportacionController.java`, `views/TareaImportacion.xml`
- `ImportadorFichero.java`, `ImportadorFicheroFactory.java`, `ImportadorException.java`
- `TipoUsuario.xml` — sin cambios de dominio; la búsqueda por código va al repositorio concreto

---

## Pasos

### Paso 1 — Modificar dominio `UsuarioAutorizado.xml`

**Fichero:** `src/main/java/com/educaflow/subsystem/registrousuario/domains/UsuarioAutorizado.xml`

Único cambio respecto al fichero original: ampliar el atributo `columns` de `<unique-constraint>` añadiendo `curso` al final, de forma que la combinación `(centro, dni, tipoUsuario, curso)` sea única en base de datos. El build de Axelor regenerará `AbstractUsuarioAutorizadoRepository` con la nueva constraint.

**XML completo resultante:**

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
        <integer name="curso" />
        <date name="fechaExportacion" title="Fecha de exportación"/>
        <unique-constraint columns="centro,dni,tipoUsuario,curso"/>
    </entity>

</domain-models>
```

**Verificación:**
```bash
grep "unique-constraint" src/main/java/com/educaflow/subsystem/registrousuario/domains/UsuarioAutorizado.xml
# Debe mostrar: columns="centro,dni,tipoUsuario,curso"
```

---

### Paso 2 — Crear `UsuarioAutorizadoRepository`

**Fichero:** `src/main/java/com/educaflow/subsystem/registrousuario/db/repo/UsuarioAutorizadoRepository.java`
**FQN:** `com.educaflow.subsystem.registrousuario.db.repo.UsuarioAutorizadoRepository`
**Extiende:** `AbstractUsuarioAutorizadoRepository` (generado por Axelor a partir del dominio modificado en el Paso 1)

**Firma y descripción del método:**

```
Optional<UsuarioAutorizado> findByCentroDniTipoUsuarioCurso(
        Centro centro, String dni, TipoUsuario tipoUsuario, Integer curso)
```

Cuerpo — ejecuta una query JPQL bifurcada obligatoriamente según si `curso` es null o no:
- **Si `curso == null`:** el filtro añade `AND self.curso IS NULL` sin ningún bind de parámetro para `curso`. Razón: en JPQL, `self.curso = :curso` con parámetro `null` no hace match con filas que tienen `curso = NULL` (el operador `=` con NULL es siempre NULL/false en SQL, no true). Si se usara la expresión de igualdad con null, el finder nunca devolvería los registros sin curso y la deduplicación fallaría.
- **Si `curso != null`:** el filtro añade `AND self.curso = :curso` con bind del valor numérico.

En ambas ramas, filtra también por `self.centro = :centro`, `self.dni = :dni` y `self.tipoUsuario = :tipoUsuario` con sus respectivos binds. Llama a `fetchOne()` y devuelve `Optional.ofNullable(resultado)`.

El método `save(entity)` se hereda de `AbstractUsuarioAutorizadoRepository` y no se sobreescribe.

**Verificación:**
```bash
grep -rn "class UsuarioAutorizadoRepository" src/main/java/com/educaflow/subsystem/registrousuario/db/repo/
```

---

### Paso 3 — Crear `TipoUsuarioRepository`

**Fichero:** `src/main/java/com/educaflow/subsystem/common/db/repo/TipoUsuarioRepository.java`
**FQN:** `com.educaflow.subsystem.common.db.repo.TipoUsuarioRepository`
**Extiende:** `AbstractTipoUsuarioRepository` (generado por Axelor)

**Firma y descripción del método:**

```
Optional<TipoUsuario> findByCodigo(String codigo)
```

Cuerpo — ejecuta `.all().filter("self.codigo = :codigo").bind("codigo", codigo).fetchOne()` y devuelve `Optional.ofNullable(resultado)`. Sigue exactamente el mismo patrón que `CentroRepository.findByCodigo` del subsistema `common`.

**Verificación:**
```bash
grep -rn "class TipoUsuarioRepository" src/main/java/com/educaflow/subsystem/common/db/repo/
```

---

### Paso 4 — Modificar `ResultadoImportacion`

**Fichero:** `src/main/java/com/educaflow/subsystem/importacion/importador/ResultadoImportacion.java`

Se añade `usuariosIgnorados` como segundo componente del record, entre `usuariosImportados` y `numeroErrores`, agrupando los tres contadores juntos. `TareaImportacionServiceImpl.fireActionRule_ejecutarImportacion` accede solo a `.log()`, `.centro()` y `.curso()` — estos siguen existiendo y la semántica no cambia, por lo que el servicio existente no requiere modificaciones.

**Record completo resultante:**

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

**Verificación:**
```bash
grep "usuariosIgnorados" src/main/java/com/educaflow/subsystem/importacion/importador/ResultadoImportacion.java
```

---

### Paso 5 — Implementar `ImportadorUsuarioCSV`

**Fichero:** `src/main/java/com/educaflow/subsystem/importacion/importador/impl/ImportadorUsuarioCSV.java`
**FQN:** `com.educaflow.subsystem.importacion.importador.impl.ImportadorUsuarioCSV`
**Implementa:** `ImportadorFichero`

La clase mantiene los dos campos finales del constructor (`fichero`, `tipoFichero`) e implementa `importar()` delegando en métodos privados de propósito único (principio de responsabilidad única, alineado con las guías de calidad). No se añade lógica compleja directamente en `importar()`.

**Firma del método público:**

```
@Override
public ResultadoImportacion importar() throws ImportadorException
```

Cuerpo del método público — orquesta los métodos privados en el siguiente orden:

1. Llama a `obtenerCentroActivo()` para obtener el centro activo del usuario en sesión (R-002). Si el método lanza `ImportadorException`, se propaga sin capturar — `TareaImportacionServiceImpl.fireActionRule_ejecutarImportacion` la captura y guarda la tarea con `estado=false`.
2. Llama a `centro.getCurso()` para obtener el curso (puede ser null; se acepta y se propaga al `UsuarioAutorizado`).
3. Llama a `obtenerTipoUsuario()` para resolver el `TipoUsuario` correspondiente a `PROFESOR_EXTERNO`.
4. Llama a `leerLineasCsv()` para obtener la lista de líneas del fichero.
5. Llama a `procesarLineas(lineas, centro, curso, tipoUsuario)` para ejecutar el bucle de importación y obtener los contadores.
6. Llama a `construirLog(contadores)` para generar el texto del log.
7. Devuelve `new ResultadoImportacion(contadores.importados(), contadores.ignorados(), contadores.errores(), log, centro, curso)`.

---

**Métodos privados — firmas y descripciones:**

```
private Centro obtenerCentroActivo() throws ImportadorException
```
Cuerpo — llama a `AuthUtils.getUser().getCentroActivo()`. Si el resultado es `null`, lanza `new ImportadorException("El importador no tiene centro activo asignado. La importación no puede realizarse.")`. Si no es null, lo devuelve (R-002).

---

```
private TipoUsuario obtenerTipoUsuario() throws ImportadorException
```
Cuerpo — llama a `Beans.get(TipoUsuarioRepository.class).findByCodigo("PROFESOR_EXTERNO")`. Si devuelve `Optional.empty()`, lanza `new ImportadorException("No se encontró el tipo de usuario PROFESOR_EXTERNO en el sistema. Verifique la configuración de datos iniciales.")`. Si está presente, devuelve el valor.

---

```
private List<String> leerLineasCsv() throws ImportadorException
```
Cuerpo — obtiene los bytes del fichero con `MetaFileUtil.downloadContent(fichero)`. Los convierte a `String` con `StandardCharsets.UTF_8`. Divide el texto por el patrón `"\\R"` (que cubre tanto `\n` como `\r\n`) usando `split`. Devuelve la lista como `List<String>`. Si el proceso de descarga lanza una excepción inesperada, la envuelve en `ImportadorException` con el mensaje sobre el error de lectura del fichero (R-003).

---

```
private ContadoresImportacion procesarLineas(
        List<String> lineas, Centro centro, Integer curso, TipoUsuario tipoUsuario)
```
Cuerpo — itera cada línea y ejecuta en orden para cada una (R-003, R-004, R-005, R-006):

1. Normaliza: llama a `DniUtil.clean(linea)` — este método ya aplica trim y conversión a mayúsculas además de eliminar prefijos especiales de NIE. Si el resultado es blank tras `clean`, descarta la línea silenciosamente con `continue` (R-003: líneas vacías se ignoran sin contabilizar error ni ignorado).
2. Valida: llama a `DniUtil.isValid(dniNormalizado)`. Si devuelve `false`, añade la cadena `"[" + dniNormalizado + "] - DNI no válido"` a la lista de errores, incrementa `errores` y hace `continue` (R-004).
3. Deduplica: llama a `Beans.get(UsuarioAutorizadoRepository.class).findByCentroDniTipoUsuarioCurso(centro, dniNormalizado, tipoUsuario, curso)`.
   - Si devuelve `Optional.empty()` (no existe la combinación): crea un nuevo `UsuarioAutorizado` con los campos `centro`, `dni = dniNormalizado`, `tipoUsuario` y `curso`; lo persiste con `Beans.get(UsuarioAutorizadoRepository.class).save(nuevoRegistro)`; incrementa `importados` (R-005).
   - Si devuelve un valor (ya existe): incrementa `ignorados` sin crear ni modificar nada (R-006).

Devuelve un `ContadoresImportacion(importados, ignorados, errores, listaErrores)`.

---

```
private String construirLog(ContadoresImportacion contadores)
```
Cuerpo — construye un `String` con el formato (R-007):
```
Importados: {importados}. Ignorados: {ignorados}. Errores: {errores}.
{si hay errores → salto de línea + cada entrada de listaErrores en su propia línea}
```
Devuelve el texto resultante.

---

**Record de apoyo privado dentro del fichero:**

```
private record ContadoresImportacion(
        int importados,
        int ignorados,
        int errores,
        List<String> listaErrores)
```

**Imports necesarios (sin código, solo declaración):**
- `com.axelor.auth.AuthUtils`
- `com.axelor.inject.Beans`
- `com.educaflow.base.util.DniUtil`
- `com.educaflow.base.util.MetaFileUtil`
- `com.educaflow.subsystem.common.db.Centro`, `TipoUsuario`
- `com.educaflow.subsystem.common.db.repo.TipoUsuarioRepository`
- `com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado`
- `com.educaflow.subsystem.registrousuario.db.repo.UsuarioAutorizadoRepository`
- `java.nio.charset.StandardCharsets`, `java.util.ArrayList`, `java.util.List`

**Verificación:**
```bash
grep -n "@TODO\|throw new ImportadorException" \
  src/main/java/com/educaflow/subsystem/importacion/importador/impl/ImportadorUsuarioCSV.java
# No debe aparecer ninguna línea con "@TODO"
# Sí deben aparecer las líneas de las excepciones de R-002 y obtenerTipoUsuario
```

---

### Paso 6 — Verificación final

```bash
./gradlew clean build --info
```

Se espera `BUILD SUCCESSFUL` sin errores de compilación. Verificaciones clave:
- El generador de código Axelor regenera `AbstractUsuarioAutorizadoRepository` incluyendo la nueva constraint.
- `UsuarioAutorizadoRepository` y `TipoUsuarioRepository` compilan extendiendo sus respectivas clases abstractas generadas.
- El record `ResultadoImportacion` compila con los 6 componentes.
- `TareaImportacionServiceImpl` compila sin cambios: accede a `resultado.log()`, `resultado.centro()` y `resultado.curso()`, que siguen existiendo con la misma semántica.
- `ImportadorUsuarioCSV` compila y no contiene `@TODO`.

---

## Matriz de trazabilidad V-XXX / R-XXX / U-XXX

| Regla | Capa | Ubicación | Descripción |
|---|---|---|---|
| V-001 | servidor + cliente | `TareaImportacionServiceImpl.validateInsert` + action-condition existente en vista | Ya implementada: bloquea si `tipoFichero` es null |
| V-002 | servidor + cliente | `TareaImportacionServiceImpl.validateInsert` + action-condition existente en vista | Ya implementada: bloquea si `fichero` es null |
| R-001 | servidor | `TareaImportacionServiceImpl.fireActionRule_asignarCamposSistema` | Ya implementada: asigna usuario, fechaImportacion, estado=false, log=null |
| R-002 | servidor | `ImportadorUsuarioCSV.obtenerCentroActivo()` | Nuevo (Paso 5): lanza `ImportadorException` si `getCentroActivo()` devuelve null |
| R-003 | servidor | `ImportadorUsuarioCSV.leerLineasCsv()` + `procesarLineas()` | Nuevo (Paso 5): lee CSV en UTF-8, normaliza con `DniUtil.clean`, descarta líneas vacías |
| R-004 | servidor | `ImportadorUsuarioCSV.procesarLineas()` | Nuevo (Paso 5): valida DNI con `DniUtil.isValid`; anota error y continúa si inválido |
| R-005 | servidor | `ImportadorUsuarioCSV.procesarLineas()` + `UsuarioAutorizadoRepository.save` | Nuevo (Pasos 2+5): crea `UsuarioAutorizado` si no existe el combo (centro, dni, tipoUsuario, curso) |
| R-006 | servidor | `ImportadorUsuarioCSV.procesarLineas()` | Nuevo (Paso 5): incrementa ignorados si el combo ya existe; no modifica el registro |
| R-007 | servidor | `ImportadorUsuarioCSV.construirLog()` + `importar()` — return | Nuevo (Pasos 4+5): construye log con contadores y errores; devuelve `ResultadoImportacion` |
| U-001 | vista | `panelEntrada showIf="id == null"` en `TareaImportacion.xml` | Ya implementada: panel de entrada editable solo en registros nuevos |
| U-002 | vista | `panelResultado showIf="id != null"` en `TareaImportacion.xml` | Ya implementada: panel de resultado en modo readonly para registros guardados |
