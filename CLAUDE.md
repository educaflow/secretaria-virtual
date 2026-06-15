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

- Para compilar **y arrancar** el proyecto lanza **siempre** el script: `./run.sh`. Hace `./gradlew clean build --info` y luego arranca la app en el puerto 8080 con la configuración correcta (`--config ../secretaria-virtual-private/axelor-config.dev.properties`). **NO** invoques `gradlew run` a mano ni añadas `--debug-jvm`: ese flag suspende la JVM esperando a que se conecte un depurador, así que la app nunca llega a responder y nunca debe usarse para arrancarla de forma desatendida.
- Si solo necesitas compilar sin arrancar: `./gradlew clean build --info`.


## Configuración

La configuración de la aplicación está en [`src/main/resources/axelor-config.properties`](src/main/resources/axelor-config.properties). **Ese es el fichero donde deben estar las propiedades de configuración**, así que cuando busques o añadas una propiedad de configuración mírala/ponla ahí. Contiene, entre otras: información de la aplicación, modo (`application.mode`), locale, página de login, base de datos (`db.default.*`), Quartz scheduler (`quartz.*`, incluido `correos.envio.cron`), correo SMTP/IMAP (`mail.*`), adjuntos (`data.upload.*`), logging y entorno criptográfico (`entornoCriptografico.*`).

El otro fichero de configuración que se pasa al arrancar (`--config ../secretaria-virtual-private/axelor-config.dev.properties`, fuera de este repositorio) es para las propiedades **privadas**: las que no deben versionarse ni hacerse públicas (credenciales reales, secretos, etc.). Sus valores **sobrescriben** a los de `axelor-config.properties`.


## Skills
Debido a que toda la aplicación está fuertemente acoplada al framework Axelor y que debes tener pocos conocimientos de Axelor se ha creado un sistema de skills para gestionar toda la parte de axelor.

Para cada parte de Axelor se han creado conjuntos de Skills:
- menus (`k-vistas`, fichero `menus.md`) → para todo lo relacionado con menús
- vistas (`k-vistas`) → para todo lo relacionado con vistas
- sistemas (`k-sistemas`) → para todo lo relacionado con sistemas y subsistemas (estructura de carpetas, servicios, controladores)
- modelos (`k-sistemas`, fichero `modelos.md`) → para todo lo relacionado con modelos (entidades JPA, dominios XML)
- validaciones (`k-validaciones`) → **cómo se implementan** en código/XML las restricciones (`RES-`), validaciones (`VAL-`), reglas de negocio (`RN-`), reglas de UI (`RUI-`) y campos calculados (`CC-`) que la spec ya definió (capas modelo XML / `validate*` / `fireActionRule_*` / vista). No clasifica las reglas: eso es del spec.
- guice (`k-guice`) → inyección de dependencias con Guice: módulos `module/<Subsistema>Module.java`, formas de binding (`bind`, `.to`, `.toProvider`, `@Provides`), cuándo hace falta un `Provider` (deps que vienen de configuración o runtime, no de otros beans) y diagnóstico del error `Guice/MissingConstructor`. Consúltalo **siempre que cablees el DI de un sistema y la construcción de un objeto no sea trivial**.
- seguridad (`k-seguridad`) → ⚠️ **OBSOLETO — NO USAR**: su modelo de dominio no coincide con las clases reales. Pendiente de rehacer. Para roles/permisos lee el código real en `src/main/java/com/educaflow/subsystem/security/`.
- secure-coding (`k-secure-coding`) → reglas de codificación segura: mass-assignment, `AllowProperties` por acción, asignación incondicional de campos `servidor` en `*ServiceImpl.insert/update`, multi-centro/IDOR, JPQL, log injection, adjuntos, secretos (**cómo** se escribe el código para que la seguridad del negocio no se pueda saltar). **CRITICAL**: aplicación obligatoria en cualquier modificación de código que toque entidades, servicios o controladores.
- acciones (`k-vistas`, fichero `actions.md`, y `k-sistemas`, fichero `controladores.md`) → para todo lo relacionado con acciones (action-views, controllers, etc.)

Es imperativo que siempre uses los skills correspondientes para cualquier acción relacionada con Axelor, ya que siguen una arquitectura propia de la secretaría virtual y del framework Axelor.

### Modificación de skills

Siempre que crees o modifiques un skill (cualquier `SKILL.md` en `.claude/skills/`), **MUST** consultar y aplicar el skill `/k-skill`, que define las reglas, frontmatter obligatorio, estructura y convenciones de redacción de los skills del proyecto.

## Flujo SDD (Spec-Driven Development)

El desarrollo de cualquier funcionalidad nueva en la secretaría virtual se hace siguiendo un pipeline de skills `/sdd-*` que transforma una idea informal en código implementado, pasando por etapas intermedias revisables. Cada etapa produce artefactos que sirven de input a la siguiente.

Skills del pipeline (en orden de uso):

1. `/sdd-specification-system` — Punto de entrada del pipeline. Crea, mejora o revisa de forma interactiva (preguntando mucho al usuario) una especificación **multi-fichero** en lenguaje de negocio en `.sdd/drafts/YYYY-MM-DD_HH-MM_{nombre}/`: un índice `specification.md` (objetivo, actores, historias de usuario con sus **escenarios `ESC-NNN`** —semilla de los tests E2E—, tablas de enlaces a modelos y pantallas, seguridad, recursos y fuera de alcance), un `entity-<Nombre>.md` por cada modelo (campos, estados, restricciones `RES-NNN`, campos calculados `CC-NNN`, las **propiedades editables por acción `AllowProperties`**, y por evento validaciones `VAL-NNN` y reglas de negocio `RN-NNN`) y un `screen-<slug>.md` por cada pantalla (identidad, menú, paneles, botones, reglas de UI `RUI-NNN`). La historia de usuario va **embebida** en la propia spec (no hay fichero `user-story.md`). Se puede invocar varias veces sobre la misma spec; siempre pregunta si crear nueva / refinar la última / elegir otra, y sobre una spec existente si además hacer un review. La conversión a la capa técnica (taxonomía `V`/`R`/`U`, clasificación `cliente`/`servidor` por campo) y la materialización de los tests E2E las asumirá `/sdd-designer-system` (migración pendiente del usuario).
2. `/sdd-designer-system` — A partir de la spec, produce un plan de DISEÑO que describe qué clases, métodos, vistas y acciones hay que construir y dónde va cada regla, sin escribir todavía el código. Propaga `tests.md` a `design/` tal cual (contrato fijo). *(Nota: este skill aún no está migrado al formato multi-fichero de la spec; sigue esperando una carpeta `analysis/` — el usuario lo migrará después.)*
3. `/sdd-implementer-system` — Descompone el `design.md` en tareas atómicas (`implementation/task_NN.md` + índice `task.md`), propaga `design/tests.md` a `implementation/tests.md` tal cual y, tras la aprobación del usuario, implementa cada tarea: copia literalmente los XML ya materializados por el diseñador y delega el código Java en `code-implementer`. Termina compilando en un bucle de auto-corrección hasta que `./gradlew clean build` pase (máx 3 iteraciones; si tras la 3ª sigue sin compilar, para y pregunta al usuario). No ejecuta tests E2E — eso es de `/sdd-debug-app`.
4. `/sdd-close-spec` — Cierra la iniciativa: archiva los artefactos en `.sdd/specs/NNNN_desc/` como versión "as-built" (corrigiendo análisis y diseño para reflejar lo realmente implementado) y actualiza los `CLAUDE.md` de las carpetas afectadas.

Skills auxiliares (no forman parte del flujo lineal de desarrollo):

- `/sdd-designer-system-review` — Revisa la carpeta `design/` ya existente sin regenerarla (XSD con xmllint, cobertura V/R/U, reglas arquitectónicas, `tests.md` idéntico al de su origen).
- `/sdd-debug-app` — Ejecuta los tests E2E de `implementation/tests.md` contra la aplicación real (un subagente por test, con `playwright-cli` la primera vez y cacheo como `.spec.ts` para reejecuciones); cuando un test falla, corrige el código Java y reintenta (máx 3 por test). Progreso reanudable en `implementation/progress.jsonl`.
- `/sdd-eval` — Herramienta de meta-evaluación para medir y mejorar la calidad de los propios skills SDD comparando su output contra un artefacto "gold" de referencia.

Flujo resumido:

```
specification  →  design  →  implementation  →  close
                                  ↑
                  (debug E2E con /sdd-debug-app)
```

## Architectura
Todo el proyecto cuelga del paquete: `com.educaflow`

Bajo el paquete `com.educaflow` existen estos 5 grandes paquetes:
- `base.util` — Son las utilidaes de bajo nivel para no repetir pequeños trozos de código. Ejemplos de ello son: `JsonUtil`, `MetaFileUtil`, `ActionRequestHelper`, `AllowProperties`, `AxelorViewUtil`, `TextUtil`, `Convert`, `DniUtil`, `ReflectionUtil`, `SecurityUtil`, `CryptoUtil`, `XmlUtil`. El catálogo completo de clases y sus métodos está documentado en [`src/main/java/com/educaflow/base/util/CLAUDE.md`](src/main/java/com/educaflow/base/util/CLAUDE.md).
- `base.infrastructure` — Son clases completas y reutilizables en cualquier proyecto (PDF, validación, criptografía, autofirma, mail, mapper, etc.). El catálogo de paquetes está documentado en [`src/main/java/com/educaflow/base/infrastructure/CLAUDE.md`](src/main/java/com/educaflow/base/infrastructure/CLAUDE.md).
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