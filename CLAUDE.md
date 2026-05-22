# CLAUDE.md — secretaria-virtual

La secretaría virtual es un proyecto de gestión de expedientes administrativos con tramitación electrónica, firmado digital y gestión documental. Está construido sobre el framework Axelor, que proporciona una base sólida de JPA/ORM, vistas XML, seguridad y DI.

## Tecnologías
- Java 21
- Kotlin 21
- iText 9 para PDF
- PostgreSQL 12 como base de datos
- Guice para injección de dependencias
- Axelor framework 8.1 para la capa de aplicación (ORM, vistas, seguridad, etc.)
- JPA para acceso a datos, con repositorios personalizados y genéricos

## MCP de IntelliJ
Tienes disponible el MCP de IntelliJ (`mcp__intellij-index__`). **Debes usarlo siempre que sea posible** en lugar de `grep`, `find`, `Glob` o búsquedas manuales con `Bash`.

Estos tools son de solo lectura o refactor seguro a través del índice de IntelliJ y **no tienen riesgo**, por lo que **debes invocarlos directamente sin pedir confirmación previa al usuario** y sin anunciar que vas a usarlos: simplemente úsalos. Están preautorizados en `.claude/settings.local.json` para que no aparezcan prompts de permiso.

Reglas concretas de sustitución:
- Buscar texto en el código → `ide_search_text` (NO `grep`/`rg`/`Bash`).
- Localizar un fichero por nombre → `ide_find_file` (NO `find`/`ls`/`Glob`).
- Localizar una clase → `ide_find_class` (NO buscar el `.java` con grep).
- Ir a la definición de un símbolo → `ide_find_definition`.
- Encontrar usos de un símbolo → `ide_find_references` (NO `grep` por el nombre).
- Renombrar, mover o borrar símbolos/ficheros → `ide_refactor_rename`, `ide_move_file`, `ide_refactor_safe_delete` (NO `mv`/`sed`/edición manual).
- Antes de asumir que el índice está disponible, si dudas, comprueba con `ide_index_status`.

Solo se admite recurrir a `grep`/`find`/`Bash` si el MCP de IntelliJ no está disponible o el caso queda fuera de lo que ofrece (por ejemplo, búsqueda en ficheros fuera del proyecto indexado).

Los tools disponibles son:

- `ide_find_class` — buscar una clase por nombre
- `ide_find_file` — buscar un fichero por nombre
- `ide_find_definition` — ir a la definición de un símbolo
- `ide_find_references` — encontrar todos los usos de un símbolo
- `ide_find_implementations` — encontrar implementaciones de una interfaz o clase abstracta
- `ide_find_super_methods` — encontrar métodos padre
- `ide_call_hierarchy` / `ide_type_hierarchy` — jerarquías de llamadas y tipos
- `ide_search_text` — búsqueda de texto en el proyecto
- `ide_diagnostics` — diagnósticos y errores del IDE
- `ide_index_status` — estado del índice de IntelliJ
- `ide_refactor_rename` — renombrar símbolo de forma segura
- `ide_refactor_safe_delete` — eliminar símbolo de forma segura
- `ide_move_file` — mover fichero de forma segura
- `ide_sync_files` — sincronizar ficheros con el IDE

Usar estos tools garantiza que las búsquedas y refactorizaciones son correctas y tienen en cuenta el índice real del proyecto.

## Script del proyecto

- Para compilar el proyecto lanza el comando: `./gradlew clean build --info`
- Para ejecutar el proyecto lanza el comando: `./gradlew --no-daemon run --debug-jvm --port 8080 --context-path /`


## Skills
Debido a que toda la aplicación está fuertemente acoplada al framework Axelor y que debes tener pocos conocimientos de Axelor se ha creado un sistema de skills para gestionar toda la parte de axelor.

Para cada parte de Axelor se han creado conjuntos de Skills:
- menu → para todo lo relacionado con menús
- vistas → para todo lo relacionado con vistas
- sistemas → para todo lo relacionado con sistemas (tipos de expediente, tramites, etc.)
- modelos → para todo lo relacionado con modelos (entidades JPA)
- seguridad → para todo lo relacionado con seguridad (permisos, roles, etc.)
- acciones → para todo lo relacionado con acciones (action-views, controllers, etc.)

Es imperativo que siempre uses los skills correspondientes para cualquier acción relacionada con Axelor, ya que siguen una arquitectura propia de la secretaría virtual y del framework Axelor.

### Modificación de skills

Siempre que crees o modifiques un skill (cualquier `SKILL.md` en `.claude/skills/`), **MUST** consultar y aplicar el skill `/k-skill`, que define las reglas, frontmatter obligatorio, estructura y convenciones de redacción de los skills del proyecto.

## Flujo SDD (Spec-Driven Development)

El desarrollo de cualquier funcionalidad nueva en la secretaría virtual se hace siguiendo un pipeline de skills `/sdd-*` que transforma una idea informal en código implementado, pasando por etapas intermedias revisables. Cada etapa produce artefactos que sirven de input a la siguiente.

Skills del pipeline (en orden de uso):

1. `/sdd-create-user-story` — Crea la iniciativa: genera la carpeta `.sdd/drafts/YYYY-MM-DD_HH-MM_{nombre}/` con un `user-story.md` (intención del usuario) y un `design-guidelines.md` (directrices opcionales). Es el punto de entrada del pipeline.
2. `/sdd-specification-system` — Toma la historia de usuario y, mediante preguntas iterativas, produce una `specification.md` completa en lenguaje de negocio: entidades, operaciones, **flujos principales `F-NNN`** (narrativos, semilla de los tests E2E), pantallas, menús, seguridad y los requisitos del sistema en formato EARS (`E-UB`, `E-EV`, `E-ST`, `E-UN`, `E-OP`). La clasificación V/R/U es responsabilidad del analista.
3. `/sdd-analyst-system` — Interpreta la especificación y genera los artefactos de análisis: un `analysis.md` índice, un `entity-<Nombre>.md` por cada entidad detectada, un `screen-<nombre>.md` por cada pantalla, y un `tests.md` con escenarios E2E Given/When/Then (`T-NNN`) que materializan los `F-NNN` del spec. Cada V/R/U y cada test mantienen trazabilidad al spec (columnas "Origen EARS" y "Origen F").
4. `/sdd-designer-system` — A partir del análisis, produce un plan de DISEÑO que describe qué clases, métodos, vistas y acciones hay que construir y dónde va cada regla, sin escribir todavía el código. Propaga `tests.md` desde `analysis/` a `design/` tal cual (contrato fijo).
5. `/sdd-implementer-system` — Ejecuta el plan de diseño en tres fases: (a) copia los XML del diseño al árbol del proyecto, (b) invoca `code-implementer` para escribir el código Java, y (c) si existe `design/tests.md`, arranca la app y ejecuta los tests E2E con `playwright-cli` en un bucle de auto-corrección (máx 3 iteraciones; si fallan, reinvoca `code-implementer` con el reporte; si tras la 3ª siguen fallos, para y pregunta al usuario).
6. `/sdd-close-spec` — Cierra la iniciativa: archiva los artefactos en `.sdd/specs/NNNN_desc/` como versión "as-built" (corrigiendo análisis y diseño para reflejar lo realmente implementado) y actualiza los `CLAUDE.md` de las carpetas afectadas.

Skill auxiliar (no forma parte del flujo de desarrollo):

- `/sdd-eval` — Herramienta de meta-evaluación para medir y mejorar la calidad de los propios skills SDD comparando su output contra un artefacto "gold" de referencia.

Flujo resumido:

```
user-story  →  specification  →  analysis  →  design  →  implementation  →  close
```

## Architectura
Todo el proyecto cuelga del paquete: `com.educaflow`

Bajo el paquete `com.educaflow` existen estos 5 grandes paquetes:
- `base.util` — Son las utilidaes de bajo nivel para no repetir pequeños trozos de código. Ejemplos de ello son: `JsonUtil`, `MetaFileUtil`, `ActionRequestHelper`, `AllowProperties`, `AxelorViewUtil`, `TextUtil`, `Convert`, `DniUtil`, `ReflectionUtil`, `SecurityUtil`, `CryptoUtil`, `XmlUtil`
- `base.infrastructure` — Son clase completas y reutilizables en cualquier proyecto. Ejemplos de ello son `pdf` (iText PDF operations), `validation` (BusinessMessages/BusinessException/ValidationEngine DSL), `criptografia` (X.509 certs, HSMs, FNMT/ACCV/DNI issuers), `autofirma` (desktop client integration), `mapper` (BeanMapperModel), `mail`, `evaluator` (Groovy expressions), `numeradores`, `metafile`
- `subsystem` — Son subsistema que tiene una función completa dentro de la aplicación: `firmas`, `expedientes`, `registroentradasalida`, `pdfutilities`, `common`, `certificados`, `importer`, `sistemaeducativo`, `security`
- `system` — Son sistemas completos que tiene una función completa dentro de la aplicación
- `secretariavirtual` — Es donde están los menús y las tareas de inicialización.

### Sistemas y subsistemas
Un sistema y un subsistema siguen exactamente la misma estructura:

La diferencia entre uno y otro es que nadie depende de un sistema , mientras que alguien si que depende de los subsistemas.
De un subsistema puede depender un sistema u otro subsistema. Lo que no puede haber son relaciones cíclicas.



### Expedientes
Los expedientes es la parte más importate de la secretaría virtual y la más compleja debido a ello siguen una arquitecura diferente al resto de la aplicación.



### i18n
Nunca jamás, crear los ficheros `i18n_ca.csv` ni `i18n_es.csv` ya que hay un script que los genera automáticamente, así que es totalmente innecesario.
A veces hay palabras que acaban con `__!!` como en `AutoFirma__!!` esto es para indicar que esa palabra no se debe traducir, ya que el script de generación de i18n las deja tal cual pero sin el `__!!`  al final, así que no hay que preocuparse por eso. Pero cuando se use la palabra por ejemplo para ponerla en formato camelCase primero hay que quitar el `__!!` y ponerla en formato camelCase, por ejemplo `AutoFirma__!!` se convierte en "autoFirma".

## La aplicación
La aplicación de secretaría virtual va a ser usada en centros educativos para informatizarlos. La app permite que haya más de un centro educativo. Es decir que es una aplicación "multicentro" y cada centro solo puede ver su propia información.

### Tipos de usuarios y cargos
Existen varios tipos de usuarios en la aplicación:
- Administrador: Puede ver cosas de cualquier centro
- Supervidor: Gestiona un centro
- Profesor
- Exprofesor
- Alumno
- Exalumno
- Externo
- Familiar

Los cargos son:
- Director
- Jefes de estudio
- Secretario
- Vicesecretario
- Administrativas
- Conserjes

El hecho de que exista el tipo de  usuario administrador y que pueda ver cosas de cualquier centro hace que ha veces haya que añadir una nueva "pantalla" para que el administrador pueda ver de cualquier centro.

La sistemas o subsistemas que existen o van a existir son:
- Registro de entrada/salida: Los documentos que se presentan en la aplicación y los que genera la aplicación para los usuarios
- Notificaciones: Cada una de las notificaciones que se envian a los usuarios. Por ahora las únicas notificaciones son correos electrónicos
- Firmas: Permite a los usuarios firmas documentos que se ponen a él a firmar. Es una idea similar a la aplicación de Portafirmas del gobierno de españa pero más sencilla.
- Gestión de centro: Permite configurar todo lo que necesita un centro, como los usuarios, permisos, cargos, etc.
- Carpeta ciudadana: Contiene todo lo referido a un alumno o profesor (notificaciones, registros de entrada y salida, expedientes, firmas)