---
type: design
---

# Diseño: Importación de usuarios autorizados por CSV

**Objetivo:** Hacer funcional la importación masiva de `UsuarioAutorizado` por CSV implementando `ImportadorUsuarioCSV` y completando la cadena de reglas (V-XXX, R-XXX) en el subsistema `importacion`, ampliando la unicidad de `UsuarioAutorizado` a `(centro, dni, tipoUsuario, curso)`.

**Capa:** `subsystem/importacion` (modificar subsistema existente) + cambios puntuales en `subsystem/registrousuario` (unicidad + repositorio concreto) y `subsystem/common` (finder por código en `TipoUsuario`).

**Análisis de origen:** `.sdd/drafts/2026-05-14_11-54_importacion-usuarios-csv/analysis_02/analysis.md`

**Skills necesarios para la implementación:** `k-sistemas`, `k-validaciones`, `k-vistas`

---

## Decisiones de diseño previas

Antes de listar ficheros y pasos se fijan tres decisiones que el análisis dejaba abiertas:

1. **Firma del importador.** El constructor recibe la `TareaImportacion` y `importar()` queda sin parámetros. Razones: la tarea es inmutable durante el proceso (V-003), permite pre-resolver el `TipoUsuario` y los repositorios una sola vez en el constructor, y deja explícito que el importador es un objeto de un solo uso.
   - `ImportadorFichero.importar() throws ImportadorException` — **no cambia**.
   - `ImportadorFicheroFactory.create(TareaImportacion)` — cambia de `(TipoFicheroImportacion, MetaFile)` a `(TareaImportacion)`.
   - `ImportadorUsuarioCSV(TareaImportacion)` como constructor único.

2. **`ResultadoImportacion` ajustado y `centro`/`curso` los aporta el importador.** El record `ResultadoImportacion` lo comparten todas las implementaciones de `ImportadorFichero`, no solo el CSV. La implementación hermana `ImportadorUsuarioXML` (fuera del alcance funcional de esta iniciativa, pero que comparte interfaz) obtiene `centro` y `curso` del propio fichero XML; el CSV los obtiene del contexto del usuario importador. En ambos casos **es el importador quien los calcula**, no el servicio. Por tanto el record los mantiene como campos de salida y solo se ajustan los contadores para reflejar la semántica del análisis (creados / ignorados / fallidos en lugar de `usuariosImportados` / `numeroErrores`):

   ```java
   public record ResultadoImportacion(
           int creados,
           int ignorados,
           int fallidos,
           String log,
           Centro centro,
           Integer curso) { }
   ```

   Convenio entre importador y servicio para esos dos campos:
   - **`ImportadorUsuarioCSV`** (esta iniciativa): al inicio de `importar()` calcula `centro = AuthUtils.getUser().getCentroActivo()` y `curso = centro != null ? centro.getCurso() : null`. Si `centro` es `null` lanza `ImportadorException` con el mensaje de R-006; si `curso` es `null` lanza `ImportadorException` con el mensaje de R-007. Si todo va bien, devuelve esos mismos valores dentro del `ResultadoImportacion` final.
   - **`ImportadorUsuarioXML`** (otra iniciativa): obtiene `centro` y `curso` del fichero XML y los devuelve en el resultado. Si el fichero no los contiene o son inválidos, su responsabilidad es lanzar también `ImportadorException` con el motivo apropiado.
   - El **servicio NO asigna `centro` ni `curso`** ni *Antes* ni en ningún otro punto: simplemente los **vuelca a la tarea** desde `ResultadoImportacion` tras el retorno normal del importador. Esto mantiene el código del servicio agnóstico a la implementación concreta del importador y respeta el flujo asimétrico CSV/XML.

   **Reubicación de R-002, R-003, R-006 y R-007:**
   - R-002 (asignar centro) y R-003 (asignar curso) se ubican dentro de `ImportadorUsuarioCSV.importar()` — son donde se calcula el valor — y se aplican efectivamente sobre la tarea desde `fireActionRule_ejecutarImportacion` *Después* (no *Antes*) al volcar `resultado.centro()` / `resultado.curso()`.
   - R-006 y R-007 se ubican también en `ImportadorUsuarioCSV.importar()`: el importador detecta `centroActivo == null` o `Centro.curso == null` y lanza `ImportadorException` con el mensaje específico que el análisis define para cada caso. El servicio captura la excepción de forma uniforme (estado=false + log con el mensaje) en el mismo *catch* que cubre R-018. Lo que distingue R-006 / R-007 / R-018 es exclusivamente el mensaje que el importador (o el servicio para imprevistos) graba en el log.
   - El análisis declara R-002/R-003 como "Antes" pensando en el caso CSV. Esa etiqueta sigue siendo coherente entendida como "antes del procesamiento de las líneas del fichero", pero desde el punto de vista del flujo `insert` del servicio son acciones que ocurren *Después* de `super.insert` (porque el importador se invoca *Después*). El diseño explicita este punto para evitar interpretaciones erróneas durante la implementación.

3. **Acceso a `TipoUsuario` por código.** Se añade un `<finder-method name="findByCodigo" using="codigo"/>` en `TipoUsuario.xml`. Axelor lo genera en `AbstractTipoUsuarioRepository` (no hace falta crear repo concreto). El importador lo consume vía `JpaRepository.of(TipoUsuario.class).findByCodigo(...)`.

---

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---|---|---|---|
| `subsystem/registrousuario/domains/UsuarioAutorizado.xml` | Modificar | k-sistemas, k-validaciones | Cambiar `<unique-constraint>` a 4 columnas (V-005) y añadir `<finder-method>` `findByCentroDniTipoUsuarioCurso`. Se mantiene `repository="abstract"`. |
| `subsystem/registrousuario/db/repo/UsuarioAutorizadoRepository.java` | Crear | k-sistemas | Repositorio concreto que extiende `AbstractUsuarioAutorizadoRepository` y expone `existsByCentroDniTipoUsuarioCurso(...)` y `findByCentroDniTipoUsuarioCurso(...)`. |
| `subsystem/common/domains/TipoUsuario.xml` | Modificar | k-sistemas | Añadir `<finder-method name="findByCodigo" using="codigo"/>` (R-013). |
| `subsystem/importacion/importador/ResultadoImportacion.java` | Modificar | k-sistemas | Renombrar y reformular los contadores a `(int creados, int ignorados, int fallidos, String log, Centro centro, Integer curso)`. Se conservan `centro` y `curso` porque `ImportadorUsuarioXML` los extrae del propio fichero y la interfaz es compartida. |
| `subsystem/importacion/importador/ImportadorFichero.java` | Sin cambios | k-sistemas | La interfaz mantiene `ResultadoImportacion importar() throws ImportadorException`. |
| `subsystem/importacion/importador/ImportadorFicheroFactory.java` | Modificar | k-sistemas | Cambiar firma a `create(TareaImportacion)`. Switch sobre `tareaImportacion.getTipoFichero()`. |
| `subsystem/importacion/importador/impl/ImportadorUsuarioCSV.java` | Modificar | k-sistemas, k-validaciones | Núcleo del trabajo: implementar R-008..R-017. Constructor `(TareaImportacion)`. Métodos privados auxiliares. |
| `subsystem/importacion/importador/impl/ImportadorUsuarioXML.java` | Modificar (mínimo) | k-sistemas | Adaptar el constructor a la nueva firma de la factoría (`(TareaImportacion)`). Cuerpo de `importar()` queda fuera de alcance (sigue lanzando el `@TODO`). |
| `subsystem/importacion/service/impl/TareaImportacionServiceImpl.java` | Modificar | k-sistemas, k-validaciones | Ampliar `fireActionRule_asignarCamposSistema` con centro y curso (R-002, R-003). Reorganizar `fireActionRule_ejecutarImportacion` para R-005..R-007, R-016, R-017, R-018. Confirmar validaciones V-001..V-004. |
| `subsystem/importacion/service/TareaImportacionService.java` | Sin cambios | k-sistemas | Interfaz mantiene la re-declaración legacy de `validateInsert/Update/Remove`. |
| `subsystem/importacion/controller/TareaImportacionController.java` | Sin cambios | k-sistemas | `validateSave(actionRequest, actionResponse)` ya existe con `AllowProperties(tipoFichero, fichero)`. |
| `subsystem/importacion/views/TareaImportacion.xml` | Sin cambios | k-vistas | U-001..U-005 y V-001/V-002 en cliente ya implementadas. |
| `secretariavirtual/menus/menus.xml` | Sin cambios | k-vistas | Menú con grupo `admins` ya existente. |

---

## Pasos

### Paso 1 — Recursos previos

No hay recursos estáticos nuevos. La i18n (`i18n_es.csv`, `i18n_ca.csv`) la genera el script del proyecto y no se crea manualmente. Los textos del log y los mensajes server se redactan en español tal cual, sin sufijos `__!!` salvo para etiquetas técnicas existentes.

### Paso 2 — Dominios

#### 2.1 `subsystem/registrousuario/domains/UsuarioAutorizado.xml` (modificado)

XML completo del fichero tras los cambios (V-005 + finder para R-014/R-015):

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
        <date name="fechaExportacion" title="Fecha de exportación"/>

        <unique-constraint columns="centro,dni,tipoUsuario,curso"/>

        <finder-method name="findByCentroDniTipoUsuarioCurso"
                       using="centro,dni,tipoUsuario,curso"
                       all="false"/>
    </entity>

</domain-models>
```

Notas:
- `repository="abstract"` se mantiene; el repositorio concreto se crea en el paso 4.
- El finder generado tendrá la firma `UsuarioAutorizado findByCentroDniTipoUsuarioCurso(Centro centro, String dni, TipoUsuario tipoUsuario, Integer curso)` en `AbstractUsuarioAutorizadoRepository`.

#### 2.2 `subsystem/common/domains/TipoUsuario.xml` (modificado — añadir finder)

Se conserva la definición existente; añadir dentro de `<entity name="TipoUsuario" repository="abstract">`:

```xml
<finder-method name="findByCodigo" using="codigo" all="false"/>
```

Axelor lo genera en `AbstractTipoUsuarioRepository.findByCodigo(String codigo)`. El importador lo consume vía `JpaRepository.of(TipoUsuario.class).findByCodigo(...)` (no se crea repo concreto).

#### 2.3 `subsystem/importacion/domains/TareaImportacion.xml`

Sin cambios. La entidad existente cubre los campos y los `required="true"` aplican V-001 y V-002 a nivel de modelo. La inmutabilidad (V-003) y la no-borrabilidad (V-004) se aplican en servidor, no en el dominio.

### Paso 3 — Servicio: validaciones e interfaz

#### 3.1 `subsystem/importacion/service/TareaImportacionService.java` (sin cambios funcionales)

Se mantiene la interfaz existente; **no se modifica**. Mantiene la re-declaración legacy de `validateInsert/Update/Remove` por consistencia con el código actual del subsistema.

```java
// public interface TareaImportacionService extends ModelService<TareaImportacion> {
//     Optional<BusinessMessages> validateInsert(TareaImportacion entidad);
//     Optional<BusinessMessages> validateUpdate(TareaImportacion entidad, TareaImportacion entidadOriginal);
//     Optional<BusinessMessages> validateRemove(TareaImportacion entidad);
// }
//
// No se añaden métodos públicos: el procesamiento se lanza internamente desde insert()
// vía fireActionRule_ejecutarImportacion.
```

### Paso 4 — Repositorios

#### 4.1 `subsystem/registrousuario/db/repo/UsuarioAutorizadoRepository.java` (crear)

FQN: `com.educaflow.subsystem.registrousuario.db.repo.UsuarioAutorizadoRepository`

```java
// public class UsuarioAutorizadoRepository extends AbstractUsuarioAutorizadoRepository {
//
//     public boolean existsByCentroDniTipoUsuarioCurso(
//             Centro centro,
//             String dni,
//             TipoUsuario tipoUsuario,
//             Integer curso);
//       Devuelve true si existe ya un UsuarioAutorizado con la 4-tupla recibida.
//       Implementación: delega en findByCentroDniTipoUsuarioCurso (finder generado por
//       Axelor desde el XML) y comprueba != null. Soporta R-015 (ya existente → ignorar).
//
//     public UsuarioAutorizado findByCentroDniTipoUsuarioCurso(
//             Centro centro,
//             String dni,
//             TipoUsuario tipoUsuario,
//             Integer curso);
//       Atajo tipado al finder generado en AbstractUsuarioAutorizadoRepository.
//       Útil para llamadas que quieren la entidad en lugar de un booleano.
// }
```

No se crea repositorio concreto para `TipoUsuario`: el finder se declara en el XML del dominio (paso 2.2) y se accede a él con `JpaRepository.of(TipoUsuario.class).findByCodigo(...)`.

### Paso 5 — Importador: contratos y DTO

#### 5.1 `subsystem/importacion/importador/ImportadorFichero.java`

```java
// public interface ImportadorFichero {
//
//     ResultadoImportacion importar() throws ImportadorException;
//       Contrato sin cambios: la implementación recibe el contexto vía constructor.
//       Devuelve estadísticas + log textual. Errores recuperables por línea NO lanzan;
//       se acumulan en el log. Solo se lanza ImportadorException ante errores globales
//       no recuperables (fichero ilegible, TipoUsuario no encontrado, E/S).
// }
```

#### 5.2 `subsystem/importacion/importador/ImportadorFicheroFactory.java` (modificar firma)

```java
// public final class ImportadorFicheroFactory {
//
//     private ImportadorFicheroFactory() { }
//
//     public static ImportadorFichero create(TareaImportacion tareaImportacion);
//       Selecciona la implementación a partir de tareaImportacion.getTipoFichero():
//         PROFESOR, ALUMNO, FAMILIAR    → ImportadorUsuarioXML(tareaImportacion)
//         PROFESOR_EXTERNO              → ImportadorUsuarioCSV(tareaImportacion)
//       Si el tipoFichero no tiene importador asociado, lanza IllegalStateException
//       indicando el valor recibido y los valores soportados (defensivo: el enum lo limita).
// }
```

#### 5.3 `subsystem/importacion/importador/ResultadoImportacion.java` (modificado)

```java
// public record ResultadoImportacion(
//         int creados,
//         int ignorados,
//         int fallidos,
//         String log,
//         Centro centro,
//         Integer curso) { }
//
// Mantiene centro y curso para compatibilidad con ImportadorUsuarioXML, que los
// extrae del fichero XML. ImportadorUsuarioCSV los rellena por idempotencia copiando
// los valores que el servicio ya asignó a la tarea Antes (R-002, R-003).
//
// Se renombran/reformulan los contadores respecto al record actual:
//   - usuariosImportados → creados (R-014)
//   - numeroErrores      → fallidos (R-011, R-012)
//   - (nuevo)            → ignorados (R-015), antes implícito en "no creados ni fallidos"
//
// El campo estado de TareaImportacion lo fija el servicio en función de si el
// importador retornó normalmente (R-017) o lanzó excepción (R-018), NO se incluye
// en el record (sería redundante).
```

### Paso 6 — Importador: `ImportadorUsuarioCSV`

#### 6.1 `subsystem/importacion/importador/impl/ImportadorUsuarioCSV.java`

FQN: `com.educaflow.subsystem.importacion.importador.impl.ImportadorUsuarioCSV`

```java
// public final class ImportadorUsuarioCSV implements ImportadorFichero {
//
//     private final TareaImportacion tareaImportacion;
//     private final UsuarioAutorizadoRepository usuarioAutorizadoRepository
//             = Beans.get(UsuarioAutorizadoRepository.class);
//
//     public ImportadorUsuarioCSV(TareaImportacion tareaImportacion);
//       Único constructor. Guarda la TareaImportacion entera y resuelve el repositorio
//       de UsuarioAutorizado vía Beans.get (no se inyecta porque la factoría es estática).
//       El TipoUsuario se resuelve dentro de importar() (no en el constructor) para
//       que un mapping ausente se reporte como excepción global controlada (R-018).
// }
```

Método público:

```java
// @Override
// public ResultadoImportacion importar() throws ImportadorException;
//   Orquesta el flujo completo R-002, R-003, R-006, R-007, R-008..R-017.
//   Pseudo-lógica:
//     1. Centro centro = AuthUtils.getUser().getCentroActivo();              // R-002
//        if (centro == null) → throw ImportadorException con el mensaje
//        "El importador no tiene centro activo asignado."                    // R-006
//     2. Integer curso = centro.getCurso();                                  // R-003
//        if (curso == null) → throw ImportadorException con el mensaje
//        "El centro '{centro}' no tiene curso activo configurado."           // R-007
//     3. byte[] contenido = MetaFileUtil.downloadContent(tareaImportacion.getFichero())
//     4. String csv = readCsvAsUtf8WithoutBom(contenido)                     // R-008
//     5. List<String> lineas = readLines(csv)                                // R-009 (filtra vacías)
//     6. TipoUsuario tipoUsuario = resolverTipoUsuario()                     // R-013
//     7. Set<String> dnisYaVistos = new HashSet<>();
//        Contadores contadores = new Contadores();
//        StringBuilder detalleFallidos = new StringBuilder();
//        for (int i = 0; i < lineas.size(); i++) {
//            procesarLinea(i+1, lineas.get(i), centro, curso, tipoUsuario,
//                          dnisYaVistos, contadores, detalleFallidos);
//        }
//     8. String log = componerLog(contadores, detalleFallidos.toString())    // R-016
//     9. return new ResultadoImportacion(
//                contadores.creados, contadores.ignorados, contadores.fallidos,
//                log, centro, curso);
//   El servicio confirmará R-017 (estado=true) al recibir el resultado sin excepción.
//   Cualquier IOException de lectura o resolución global no recuperable se traduce
//   a ImportadorException con el motivo; el servicio la captura como R-018.
```

Métodos privados auxiliares (firma + comentario; sin cuerpos):

```java
// private String readCsvAsUtf8WithoutBom(byte[] contenido) throws ImportadorException;
//   Aplica R-008. Decodifica los bytes como UTF-8 y, si el primer carácter es la BOM
//   (﻿), la descarta. Cualquier fallo de decodificación se envuelve como
//   ImportadorException con mensaje informativo del motivo.
```

```java
// private List<String> readLines(String csvContent);
//   Aplica R-009. Divide por '\r\n' | '\n' | '\r' y elimina líneas vacías o solo
//   whitespace. La numeración de líneas en el log se preserva contando solo las
//   líneas devueltas (la 1ª línea no vacía es la nº 1) — el análisis no exige
//   conservar el número físico del fichero original.
```

```java
// private TipoUsuario resolverTipoUsuario() throws ImportadorException;
//   Aplica R-013. Resuelve TipoUsuario vía
//       JpaRepository.of(TipoUsuario.class).findByCodigo(tareaImportacion.getTipoFichero().name())
//   La convención del mapeo es 1:1 por igualdad de código (p. ej. PROFESOR_EXTERNO →
//   TipoUsuario con codigo='PROFESOR_EXTERNO'); en el futuro otros valores del enum
//   declararán su correspondencia siguiendo el mismo criterio.
//   Si no encuentra el TipoUsuario, lanza ImportadorException con mensaje que indica
//   el código buscado y que no existe en el catálogo (lo recoge R-018 en el servicio).
```

```java
// private void procesarLinea(
//         int numeroLinea,
//         String lineaCruda,
//         Centro centro,
//         Integer curso,
//         TipoUsuario tipoUsuario,
//         Set<String> dnisYaVistos,
//         Contadores contadores,
//         StringBuilder detalleFallidos);
//   Ciclo de vida de una línea no ignorable. Recibe centro y curso ya resueltos
//   por importar() para no recalcularlos en cada iteración.
//     - Aplica R-010: dni = DniUtil.clean(lineaCruda).
//     - Aplica R-011: si !DniUtil.isValid(dni) →
//          contadores.fallidos++; detalleFallidos.append(formatLineaFallidoDniInvalido(numeroLinea, lineaCruda));
//          return.
//     - Aplica R-012: si !dnisYaVistos.add(dni) →
//          contadores.fallidos++; detalleFallidos.append(formatLineaFallidoDniDuplicado(numeroLinea, dni));
//          return.
//     - Caso correcto: crearOIgnorarUsuarioAutorizado(dni, centro, curso, tipoUsuario, contadores).
//   No relanza excepciones controladas; las globales se propagan como ImportadorException
//   y las atrapa el servicio (R-018).
```

```java
// private void crearOIgnorarUsuarioAutorizado(
//         String dniNormalizado,
//         Centro centro,
//         Integer curso,
//         TipoUsuario tipoUsuario,
//         Contadores contadores);
//   Aplica:
//     - R-015 (Antes equivalente): si
//       usuarioAutorizadoRepository.existsByCentroDniTipoUsuarioCurso(centro, dniNormalizado, tipoUsuario, curso)
//       → contadores.ignorados++ y return.
//     - R-014: en otro caso, construye UsuarioAutorizado(centro, dniNormalizado,
//       tipoUsuario, curso, fechaExportacion=LocalDate.now()) y persiste vía
//       usuarioAutorizadoRepository.save(...). contadores.creados++.
//   Recibe centro y curso por argumento (ya resueltos por importar()).
```

```java
// private String formatLineaFallidoDniInvalido(int numeroLinea, String dniRecibido);
//   Aplica R-011. Devuelve la línea del log con el contenido informativo:
//   número de línea + DNI tal como apareció en el fichero (sin normalizar) + motivo
//   "no es un DNI/NIE/NIF válido". Una línea por fallo, con salto al final.
```

```java
// private String formatLineaFallidoDniDuplicado(int numeroLinea, String dniNormalizado);
//   Aplica R-012. Devuelve la línea del log con: número de línea + DNI normalizado +
//   motivo "duplicado en el fichero". El DNI se muestra normalizado porque ya pasó
//   por DniUtil.clean en este punto.
```

```java
// private String componerLog(Contadores contadores, String detalleFallidos);
//   Aplica R-016. Devuelve el log final:
//     - Bloque resumen con los tres totales (creados / ignorados / fallidos).
//     - A continuación, el detalle línea a línea de los fallidos (creados e ignorados
//       solo se cuentan, no se listan).
//   El servicio asignará este String literal a tareaImportacion.log.
```

```java
// private static final class Contadores {
//     int creados;
//     int ignorados;
//     int fallidos;
//   Estructura mutable interna para evitar pasar tres ints por referencia entre métodos.
// }
```

#### 6.2 `subsystem/importacion/importador/impl/ImportadorUsuarioXML.java` (modificar mínimamente)

```java
// public final class ImportadorUsuarioXML implements ImportadorFichero {
//     private final TareaImportacion tareaImportacion;
//
//     public ImportadorUsuarioXML(TareaImportacion tareaImportacion);
//       Único constructor. Adapta la firma a la nueva factoría. Sin lógica adicional.
//
//     @Override
//     public ResultadoImportacion importar() throws ImportadorException;
//       Sin cambios funcionales en esta iniciativa: fuera de alcance.
//       Sigue lanzando ImportadorException("@TODO: Importación no implementada todavía").
// }
```

### Paso 7 — Servicio: `TareaImportacionServiceImpl`

#### 7.1 `subsystem/importacion/service/impl/TareaImportacionServiceImpl.java`

FQN: `com.educaflow.subsystem.importacion.service.impl.TareaImportacionServiceImpl`

```java
// public class TareaImportacionServiceImpl
//         extends DefaultModelService<TareaImportacion>
//         implements TareaImportacionService { ... }
//
// Descubierto por ModelServiceFactory por convención de paquete (service.impl).
// NO se crea módulo Guice para registrar este ModelService.
```

Constructor:

```java
// public TareaImportacionServiceImpl(
//         Class<TareaImportacion> model,
//         Repository<TareaImportacion> repository);
//   Firma obligatoria que invoca ModelServiceFactory por reflexión.
//   Delega en super(model, repository). Sin lógica adicional.
```

Operación principal:

```java
// @Override
// public TareaImportacion insert(TareaImportacion tareaImportacion) {
//   Orquesta en este orden:
//     1. fireActionRule_asignarCamposSistema(tareaImportacion)   ← R-001..R-004 (Antes)
//     2. TareaImportacion creada = super.insert(tareaImportacion)
//        super.insert ejecuta validateInsert internamente (salvaguarda) y persiste.
//     3. fireActionRule_ejecutarImportacion(creada)              ← R-005..R-007, R-016, R-017, R-018 (Después)
//     4. return creada
//   La TareaImportacion SIEMPRE queda guardada con su estado y log; los fallos globales
//   (R-006, R-007, R-018) NO abortan la operación, solo dejan estado=false + log explicativo.
// }
```

Validaciones server:

```java
// @Override
// public Optional<BusinessMessages> validateInsert(TareaImportacion tareaImportacion);
//   Aplica:
//     - V-001 (tipoFichero obligatorio): si tareaImportacion.getTipoFichero() == null,
//       añade un BusinessMessage al campo "tipoFichero". El mensaje debe transmitir
//       que el tipo es obligatorio y listar los valores válidos
//       (Profesor, Alumno, Familiar, Profesor externo).
//     - V-002 (fichero obligatorio): si tareaImportacion.getFichero() == null,
//       añade un BusinessMessage al campo "fichero". El mensaje debe transmitir
//       que el fichero CSV es obligatorio.
//   Acumula errores en BusinessMessages y devuelve
//   messages.isValid() ? Optional.empty() : Optional.of(messages).
//   NO lanza excepciones.
```

```java
// @Override
// public Optional<BusinessMessages> validateUpdate(
//         TareaImportacion tareaImportacion,
//         TareaImportacion tareaImportacionOriginal);
//   Aplica:
//     - V-003 (inmutabilidad): siempre devuelve un BusinessMessages con un único
//       error global. El mensaje debe transmitir que las TareaImportacion son
//       inmutables una vez creadas. Nunca devuelve Optional.empty().
```

```java
// @Override
// public Optional<BusinessMessages> validateRemove(TareaImportacion tareaImportacion);
//   Aplica:
//     - V-004 (no borrable): siempre devuelve un BusinessMessages con un único error
//       global. El mensaje debe transmitir que las TareaImportacion no se pueden
//       eliminar. Nunca devuelve Optional.empty().
```

Reglas de negocio privadas:

```java
// private void fireActionRule_asignarCamposSistema(TareaImportacion tareaImportacion);
//   Momento: Antes de super.insert (asignaciones sobre el mismo registro).
//   Aplica:
//     - R-001: tareaImportacion.setUsuario(AuthUtils.getUser()).
//     - R-004: tareaImportacion.setFechaImportacion(LocalDateTime.now()).
//   NO asigna centro ni curso (R-002, R-003): los aporta el importador en
//   ResultadoImportacion y los aplica fireActionRule_ejecutarImportacion al recibir el
//   resultado. Esto mantiene el servicio agnóstico al tipo de importador: el CSV los
//   toma del contexto del usuario (centroActivo + Centro.curso), el XML los toma del
//   propio fichero.
//   Además, inicializa estado=false y log=null para que existan valores deterministas
//   hasta que se ejecute el importador.
//   El campo fechaExportacion de la tarea se deja como está (lo gestiona otro proceso
//   según el análisis).
```

```java
// private void fireActionRule_ejecutarImportacion(TareaImportacion tareaImportacion);
//   Momento: Después de super.insert (efectos colaterales sobre UsuarioAutorizado).
//   Orquesta R-005, R-002/R-003 (aplicación a la tarea), R-016, R-017 y R-018
//   (que incluye los casos de R-006 y R-007 vía ImportadorException con su mensaje):
//     try {
//         R-005: ImportadorFichero importador = ImportadorFicheroFactory.create(tareaImportacion);
//                ResultadoImportacion resultado = importador.importar();
//         R-002: tareaImportacion.setCentro(resultado.centro());   // valor calculado por el importador
//         R-003: tareaImportacion.setCurso(resultado.curso());     // valor calculado por el importador
//         R-016: tareaImportacion.setLog(resultado.log());
//         R-017: tareaImportacion.setEstado(true);                 // bucle llegó al final, aunque haya fallidos individuales
//     } catch (ImportadorException | RuntimeException ex) {
//         R-006/R-007/R-018: tareaImportacion.setEstado(false);
//                            tareaImportacion.setLog(ex.getMessage());
//         La distinción entre R-006, R-007 y R-018 es exclusivamente el contenido del
//         mensaje grabado en el log; el comportamiento del catch es uniforme. R-006 y
//         R-007 los origina el propio importador con un mensaje específico (centro
//         nulo / curso nulo); R-018 cubre cualquier otra excepción no esperada y, si
//         no llega mensaje, se compone uno indicando que ocurrió un error durante la
//         importación.
//         Cuando la excepción es R-006/R-007 los campos centro y curso de la tarea
//         pueden quedar null (no se llegó a asignar resultado.centro/curso) — es el
//         comportamiento esperado por el análisis: "centro puede quedar sin asignar si
//         la importación falla globalmente antes de resolverlo".
//     }
//     Persistir los cambios finales vía repository.save(tareaImportacion). No se llama
//     a this.update ni a service.update para evitar reentrar en validateUpdate (que
//     bloquearía por V-003); se usa el repositorio directamente, que es el patrón ya
//     empleado por el código actual.
//   La TareaImportacion SIEMPRE queda guardada con su log y estado.
```

### Paso 8 — Controlador

`subsystem/importacion/controller/TareaImportacionController.java`: **sin cambios**.

```java
// @CallMethod
// public void validateSave(ActionRequest actionRequest, ActionResponse actionResponse);
//   Mantiene la implementación actual:
//     - AllowProperties que permite solo tipoFichero y fichero.
//     - Delega en service.validateInsert si actionRequestHelper.getId() == null;
//       en service.validateUpdate(entidad, null) en otro caso.
//     - Envía los BusinessMessages al cliente vía
//       actionResponseHelper.doResponseBusinessMessagesAsError(...).
//   Parámetros con nombres completos actionRequest / actionResponse.
//   No requiere cambios para esta iniciativa.
```

### Paso 9 — Vistas

`subsystem/importacion/views/TareaImportacion.xml`: **sin cambios**.

Inventario actual (referenciado por nombre, sin XML literal) que cubre la trazabilidad:

- `subsysImportacion.TareaImportacion@Main-action` — única `<action-view>` del fichero (cumple "un `<action-view>` por fichero").
- `subsysImportacion.TareaImportacion@Main-grid` — rejilla solo lectura, orden `-fechaImportacion`.
- `subsysImportacion.TareaImportacion@Main-form` — formulario con `panelEntrada` (`showIf="id == null"`) y `panelResultado` (`showIf="id != null"` + `readonlyIf="true"`).
- Atributos en `panelEntrada`: `tipoFichero` y `fichero` con `required="true"` y `widget` específico → cubre **U-005** y la capa modelo de **V-001/V-002**.
- `subsysImportacion.TareaImportacion@Main-btnImportar-action` — `<action-group>` que encadena `Local → Remote → save`.
- `subsysImportacion.TareaImportacion@Main-Local-validateImportar-action` — `<action-condition>` que cubre **V-001** y **V-002** en cliente (mensaje al usuario sin esperar al roundtrip).
- `subsysImportacion.TareaImportacion@Main-Remote-validateSave-action` — `<action-method>` que llama a `TareaImportacionController.validateSave` (capa servidor de **V-001/V-002**, además de **V-003**/**V-004** cuando aplique en operaciones de update/remove).
- `subsysImportacion.TareaImportacion@Main-set-campos-sistema-action` — `<action-record>` invocado desde `onNew` que inicializa visualmente `usuario` y `fechaImportacion` (el servidor los reescribe definitivamente en R-001/R-004).
- `subsysImportacion.TareaImportacion@Main-onNew-action`, `@Main-btnCancelar-action`, `@Main-btnAceptar-action` — `<action-group>` de eventos y botones auxiliares.

Las reglas **U-001..U-004** quedan cubiertas por los `showIf` declarativos en los dos paneles del formulario; **U-005** queda cubierto por `required="true"` declarativo en los dos campos del `panelEntrada`.

### Paso 10 — Seguridad

`secretariavirtual/menus/menus.xml`: **sin cambios**. El menú existente apunta a `subsysImportacion.TareaImportacion@Main-action` con `groups="admins"` (control de acceso ejercido únicamente en la capa de menú/vista, según decisión explícita del análisis). No se reverifica en servidor.

Multicentro: garantizado por la R-002, que fija `centro = AuthUtils.getUser().getCentroActivo()` antes de persistir.

### Paso 11 — Datos iniciales

No se modifican `data-init`. El catálogo `TipoUsuario` ya contiene los códigos `PROFESOR`, `ALUMNO`, `FAMILIAR` y `PROFESOR_EXTERNO` (responsabilidad de otra iniciativa). No se introduce ningún seed nuevo.

### Paso 12 — Verificación final

Comando exacto:

```bash
./gradlew clean build --info
```

Criterios de aceptación:

- Compila sin errores.
- `AbstractUsuarioAutorizadoRepository` regenerado con el finder `findByCentroDniTipoUsuarioCurso`.
- `AbstractTipoUsuarioRepository` regenerado con el finder `findByCodigo`.
- `<unique-constraint>` aplicado en BD a 4 columnas.
- `TareaImportacionServiceImpl`, `ImportadorUsuarioCSV`, `ImportadorFicheroFactory` y `UsuarioAutorizadoRepository` compilan con las nuevas firmas.
- No hay nuevos módulos Guice para `ModelService` (los descubre la factoría).
- No hay listeners JPA con lógica de negocio (todo va en `fireActionRule_*` del servicio o métodos privados del importador).

Verificación funcional manual mínima (post-compilación, ya en runtime):

1. Login con un usuario `admins` con centro y curso activos. Lanzar una importación con un CSV válido → `estado=true`, log con resumen y altas en `UsuarioAutorizado`.
2. Lanzar con un CSV que mezcle DNIs válidos, inválidos y duplicados → `estado=true`, contadores correctos, detalle de fallidos presente.
3. Login con un usuario `admins` sin centro activo → `estado=false`, log con motivo de R-006, sin lecturas del fichero.
4. Centro sin curso activo → `estado=false`, log con motivo de R-007.
5. Intentar editar o borrar una tarea ya guardada → bloqueado por V-003/V-004 con los mensajes correspondientes.

---

## Matriz de trazabilidad

### Validaciones (`V-XXX`)

| ID | Capa modelo | Capa servidor | Capa cliente | Contenido informativo del mensaje |
|---|---|---|---|---|
| V-001 | `tipoFichero required="true"` en `domains/TareaImportacion.xml` (existente) | `TareaImportacionServiceImpl.validateInsert` | `subsysImportacion.TareaImportacion@Main-Local-validateImportar-action` (`<action-condition>` existente en `views/TareaImportacion.xml`) | El tipo de fichero es obligatorio; valores válidos: Profesor, Alumno, Familiar, Profesor externo. |
| V-002 | `fichero required="true"` en `domains/TareaImportacion.xml` (existente) | `TareaImportacionServiceImpl.validateInsert` | `subsysImportacion.TareaImportacion@Main-Local-validateImportar-action` (existente) | El fichero CSV es obligatorio. |
| V-003 | — | `TareaImportacionServiceImpl.validateUpdate` | — | Las TareaImportacion no pueden modificarse una vez creadas. |
| V-004 | — | `TareaImportacionServiceImpl.validateRemove` | — | Las TareaImportacion no se pueden eliminar. |
| V-005 | `<unique-constraint columns="centro,dni,tipoUsuario,curso"/>` en `domains/UsuarioAutorizado.xml` | (gestionado por BD; el importador detecta el caso vía `UsuarioAutorizadoRepository.existsByCentroDniTipoUsuarioCurso` antes de crear, evitando lanzar la excepción) | — | Existe ya un UsuarioAutorizado con esa combinación de centro, dni, tipoUsuario y curso. |

### Reglas de negocio (`R-XXX`)

| ID | Ubicación | Momento |
|---|---|---|
| R-001 | `TareaImportacionServiceImpl.fireActionRule_asignarCamposSistema` | Antes |
| R-002 | `ImportadorUsuarioCSV.importar` (cálculo `centro = AuthUtils.getUser().getCentroActivo()`) + `TareaImportacionServiceImpl.fireActionRule_ejecutarImportacion` (volcado a la tarea desde `resultado.centro()`) | Después (en el flujo del servicio); "Antes del procesamiento" desde el punto de vista funcional del importador |
| R-003 | `ImportadorUsuarioCSV.importar` (cálculo `curso = centro.getCurso()`) + `TareaImportacionServiceImpl.fireActionRule_ejecutarImportacion` (volcado a la tarea desde `resultado.curso()`) | Después (en el flujo del servicio); "Antes del procesamiento" desde el punto de vista funcional del importador |
| R-004 | `TareaImportacionServiceImpl.fireActionRule_asignarCamposSistema` | Antes |
| R-005 | `TareaImportacionServiceImpl.fireActionRule_ejecutarImportacion` (vía `ImportadorFicheroFactory.create`) | Después |
| R-006 | `ImportadorUsuarioCSV.importar` (lanza `ImportadorException` con mensaje "El importador no tiene centro activo asignado." cuando `centroActivo == null`) + `TareaImportacionServiceImpl.fireActionRule_ejecutarImportacion` (catch uniforme → estado=false + log con el mensaje) | Después |
| R-007 | `ImportadorUsuarioCSV.importar` (lanza `ImportadorException` con mensaje "El centro '{centro}' no tiene curso activo configurado." cuando `Centro.curso == null`) + `TareaImportacionServiceImpl.fireActionRule_ejecutarImportacion` (catch uniforme → estado=false + log con el mensaje) | Después |
| R-008 | `ImportadorUsuarioCSV.readCsvAsUtf8WithoutBom` | Después |
| R-009 | `ImportadorUsuarioCSV.readLines` | Después |
| R-010 | `ImportadorUsuarioCSV.procesarLinea` (vía `DniUtil.clean`) | Después |
| R-011 | `ImportadorUsuarioCSV.procesarLinea` (rama `!DniUtil.isValid`) + `formatLineaFallidoDniInvalido` | Después |
| R-012 | `ImportadorUsuarioCSV.procesarLinea` (set `dnisYaVistos`) + `formatLineaFallidoDniDuplicado` | Después |
| R-013 | `ImportadorUsuarioCSV.resolverTipoUsuario` (vía finder XML `TipoUsuario.findByCodigo`) | Después |
| R-014 | `ImportadorUsuarioCSV.crearOIgnorarUsuarioAutorizado` (rama "no existe") + `UsuarioAutorizadoRepository.save` | Después |
| R-015 | `ImportadorUsuarioCSV.crearOIgnorarUsuarioAutorizado` (rama "existe") + `UsuarioAutorizadoRepository.existsByCentroDniTipoUsuarioCurso` | Antes (lookup previo al alta) |
| R-016 | `ImportadorUsuarioCSV.componerLog` + asignación en `TareaImportacionServiceImpl.fireActionRule_ejecutarImportacion` | Después |
| R-017 | `TareaImportacionServiceImpl.fireActionRule_ejecutarImportacion` (rama `try`, asigna `estado=true`) | Después |
| R-018 | `TareaImportacionServiceImpl.fireActionRule_ejecutarImportacion` (rama `catch` global) | Después |

### Reglas de UI (`U-XXX`) — ya implementadas

| ID | Ubicación actual | Estado |
|---|---|---|
| U-001 | `views/TareaImportacion.xml` — `panelEntrada` con `showIf="id == null"` | Ya implementada — sin cambios |
| U-002 | `views/TareaImportacion.xml` — ocultación implícita del `panelEntrada` cuando `id != null` (por inversión del `showIf` de U-001) | Ya implementada — sin cambios |
| U-003 | `views/TareaImportacion.xml` — `panelResultado` con `showIf="id != null"` y `readonlyIf="true"` | Ya implementada — sin cambios |
| U-004 | `views/TareaImportacion.xml` — ocultación implícita del `panelResultado` cuando `id == null` (por inversión del `showIf` de U-003) | Ya implementada — sin cambios |
| U-005 | `views/TareaImportacion.xml` — `tipoFichero` y `fichero` con `required="true"` en `panelEntrada` | Ya implementada — sin cambios |

---

## Notas de unificación

- **Firma del importador**: 3 de los 5 diseños eligieron constructor `(TareaImportacion)` + `importar()` sin parámetros; los otros 2 propusieron `importar(TareaImportacion)`. Adoptamos la primera por las razones expuestas en "Decisiones de diseño previas". Mantiene además el contrato actual de `ImportadorFichero.importar()` y minimiza ondas de cambio.
- **`ResultadoImportacion`**: 4 de los 5 propusieron eliminar `centro` y `curso` del record. **Rechazada esa simplificación tras detección posterior**: la implementación hermana `ImportadorUsuarioXML` (fuera de alcance, pero usuario de la misma interfaz `ImportadorFichero`) obtiene el centro y el curso del propio fichero XML, no del contexto del importador; si se eliminan del record, esa implementación deja de poder devolverlos al servicio y se rompe su contrato. Por tanto el record **conserva** `centro` y `curso`. Lo único que se simplifica respecto al record actual son los contadores: `usuariosImportados`/`numeroErrores` → `creados`/`ignorados`/`fallidos`, que es la información que el análisis exige reflejar en el log final (R-016).
- **Reubicación de R-002, R-003, R-006 y R-007**: ninguno de los 5 subagentes detectó la asimetría CSV/XML; todos colocaron R-002/R-003 en el servicio *Antes* y R-006/R-007 como guards en el servicio *Después*. **Rechazado tras revisión**: si centro y curso vienen siempre desde `ResultadoImportacion` (necesario para XML), el servicio no debe asignarlos *Antes* — sería trabajo duplicado y conceptualmente inconsistente. Adoptamos:
  - R-002/R-003 se calculan dentro del importador (`ImportadorUsuarioCSV` para esta iniciativa) y se aplican a la tarea desde `fireActionRule_ejecutarImportacion` (*Después*) leyendo `resultado.centro()` y `resultado.curso()`. Para XML será idéntico, con los valores extraídos del fichero.
  - R-006/R-007 se modelan como `ImportadorException` lanzada por el importador con su mensaje específico; el servicio las captura en el mismo *catch* que cubre R-018 y graba el mensaje en el log con `estado=false`. La distinción entre las tres reglas es exclusivamente el mensaje.
  - El análisis declara R-002/R-003 como "Antes" pensando en el caso CSV; esa etiqueta sigue siendo coherente entendida como "antes del procesamiento de las líneas del fichero", pero desde el punto de vista del flujo `insert` del servicio son acciones *Después*. Documentado explícitamente en la matriz para evitar interpretaciones erróneas durante la implementación.
- **Repositorio para `TipoUsuario`**: los 5 coincidieron en usar `<finder-method>` XML y no crear repo concreto. Se mantiene.
- **`repository="abstract"` de `UsuarioAutorizado`**: se conserva (no se cambia a `"default"`), siguiendo la convención del proyecto y porque ya se va a crear un repo concreto que extiende el abstracto generado. Esto evita un cambio innecesario en el dominio.
- **Persistencia del log final**: tras el procesamiento, los cambios sobre `estado` y `log` se vuelcan a BD vía `repository.save(tareaImportacion)` directamente (no llamando a `this.update`) para no reentrar en `validateUpdate`, que la bloquearía por V-003. Es el mismo patrón del código actual.
- **Numeración de líneas en el log**: el análisis no exige conservar el número físico de línea del fichero original (incluyendo las vacías). Se opta por numerar las líneas no vacías a partir de 1; el motivo se documenta arriba. Si en el futuro el negocio requiere conservar el número físico, basta con cambiar la rutina de iteración para no filtrar las vacías y saltar dentro del bucle.
