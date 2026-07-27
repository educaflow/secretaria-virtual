# CLAUDE.md — secretaria-virtual

La secretaría virtual es un proyecto de gestión de expedientes administrativos con tramitación electrónica, firmado digital y gestión documental. Está construido sobre el framework Axelor, que proporciona una base sólida de JPA/ORM, vistas XML, seguridad y DI.

El framework Axelor se llama **AOP (Axelor Open Platform)** y su código fuente está disponible en la carpeta hermana `../axelor-open-platform` (fuera de este repositorio). Consúltalo cuando necesites entender el comportamiento interno del framework (backend Java en `axelor-core`/`axelor-web`, frontend en `axelor-front`).

## Documentación bajo demanda (progressive disclosure)

Este proyecto usa un **progressive disclosure pattern to respect LLM instruction capacity limits**: el `CLAUDE.md` mantiene solo lo imprescindible y el resto de la documentación general vive en [`agent_docs/`](agent_docs/README.md), que se **carga solo cuando se necesita** para la tarea concreta — no todo de golpe. Consulta el índice [`agent_docs/README.md`](agent_docs/README.md) y carga únicamente el documento que aplique. Por ejemplo, el stack tecnológico (Java, Kotlin, Axelor, PostgreSQL, Guice, iText, JPA) está en [`agent_docs/tech-stack.md`](agent_docs/tech-stack.md).

## MCP de IntelliJ

**Usa siempre el MCP de IntelliJ (`mcp__intellij-index__`) en lugar de `grep`/`find`/`Glob`/`Bash`** para buscar texto, ficheros, clases, definiciones, referencias y para renombrar/mover/borrar símbolos. Son tools sin riesgo y preautorizados: invócalos directamente, sin pedir confirmación ni anunciarlos. Las reglas de sustitución concretas, el listado completo de tools y los demás MCP del proyecto (PostgreSQL, Playwright, IDE) están en [`agent_docs/mcp.md`](agent_docs/mcp.md).

## Script del proyecto

Compila **y arranca** siempre con `./run.sh` (hace `./gradlew clean build` —compila y ejecuta los tests— y arranca en el 8080 con la config privada). **NO** uses `gradlew run` a mano ni `--debug-jvm`. Cómo probar tests, compilar sin arrancar, arrancar/reiniciar/resetear la BD y acceder con `psql`: ver [`agent_docs/deploy.md`](agent_docs/deploy.md).


## Configuración

La configuración de la aplicación está en [`src/main/resources/axelor-config.properties`](src/main/resources/axelor-config.properties). **Ese es el fichero donde deben estar las propiedades de configuración**, así que cuando busques o añadas una propiedad de configuración mírala/ponla ahí. Contiene, entre otras: información de la aplicación, modo (`application.mode`), locale, página de login, base de datos (`db.default.*`), Quartz scheduler (`quartz.*`, incluido `correos.envio.cron`), correo SMTP/IMAP (`mail.*`), adjuntos (`data.upload.*`), logging y entorno criptográfico (`entornoCriptografico.*`).

El otro fichero de configuración que se pasa al arrancar (`--config ../secretaria-virtual-private/axelor-config.dev.properties`, fuera de este repositorio) es para las propiedades **privadas**: las que no deben versionarse ni hacerse públicas (credenciales reales, secretos, etc.). Sus valores **sobrescriben** a los de `axelor-config.properties`.


## Skills
Debido a que toda la aplicación está fuertemente acoplada al framework Axelor y que debes tener pocos conocimientos de Axelor se ha creado un sistema de skills para gestionar toda la parte de axelor.

Los skills se agrupan en **cuatro familias por prefijo** (las reglas de autoría de cada una están en `/k-skill`):
- `k-*` → **knowledge**: conocimiento de dominio reutilizable (convenciones, patrones, vocabulario). No hacen cosas, las describen; otros skills los cargan como referencia.
- `sdd-*` → **action**: los pasos del pipeline SDD (ver [`agent_docs/sdd-workflow.md`](agent_docs/sdd-workflow.md)).
- `developer-*` → **action**: procesos que producen o revisan código real en el árbol del proyecto (p.ej. `developer-code-implementer`, `developer-code-reviewer`, `developer-create-arch-tests`, `developer-create-view-tests`).
- `skill-*` → **action (meta)**: skills que evalúan o mejoran **otros** skills (p.ej. `skill-eval`, `skill-reviewer`). El conocimiento de cómo se escribe un skill vive en `k-skill`.

Para cada parte de Axelor se han creado conjuntos de Skills:
- menus (`k-vistas`, fichero `menus.md`) → para todo lo relacionado con menús
- vistas (`k-vistas`) → para todo lo relacionado con vistas
- sistemas (`k-sistemas`) → para todo lo relacionado con sistemas y subsistemas (estructura de carpetas, servicios, controladores)
- modelos (`k-sistemas`, fichero `modelos.md`) → para todo lo relacionado con modelos (entidades JPA, dominios XML)
- datos iniciales (`k-datainit`) → cómo se cargan los datos maestros/semilla con carpetas `data-init` (`input-config.xml` + `input/`); cada sistema/subsistema dueño de la tabla crea su propia `data-init` (sus datos y sus permisos `auth-<sistema>.xml`), no la carpeta global. Consúltalo siempre que un sistema/subsistema necesite datos iniciales obligatorios.
- validaciones (`k-validaciones`) → **cómo se implementan** en código/XML las restricciones (`RES-`), validaciones (`VAL-`), reglas de negocio (`RN-`), reglas de UI (`RUI-`) y campos calculados (`CC-`) que la spec ya definió (capas modelo XML / `validate*` / `fireActionRule_*` / vista). No clasifica las reglas: eso es del spec.
- guice (`k-guice`) → inyección de dependencias con Guice: módulos `module/<Subsistema>Module.java`, formas de binding (`bind`, `.to`, `.toProvider`, `@Provides`), cuándo hace falta un `Provider` (deps que vienen de configuración o runtime, no de otros beans) y diagnóstico del error `Guice/MissingConstructor`. Consúltalo **siempre que cablees el DI de un sistema y la construcción de un objeto no sea trivial**.
- seguridad (`k-seguridad`) → ⚠️ **OBSOLETO — NO USAR**: su modelo de dominio no coincide con las clases reales. Pendiente de rehacer. Para roles/permisos lee el código real en `src/main/java/com/educaflow/subsystem/security/`.
- secure-coding (`k-secure-coding`) → reglas de codificación segura: mass-assignment, `AllowProperties` por acción, asignación incondicional de campos `servidor` en `*ServiceImpl.insert/update`, multi-centro/IDOR, JPQL, log injection, adjuntos, secretos (**cómo** se escribe el código para que la seguridad del negocio no se pueda saltar). **CRITICAL**: aplicación obligatoria en cualquier modificación de código que toque entidades, servicios o controladores.
- acciones (`k-vistas`, fichero `actions.md`, y `k-sistemas`, fichero `controladores.md`) → para todo lo relacionado con acciones (action-views, controllers, etc.)

Es imperativo que siempre uses los skills correspondientes para cualquier acción relacionada con Axelor, ya que siguen una arquitectura propia de la secretaría virtual y del framework Axelor.

### Modificación de skills

Siempre que crees o modifiques un skill (cualquier `SKILL.md` en `.claude/skills/`), **MUST** consultar y aplicar el skill `/k-skill`, que define las reglas, frontmatter obligatorio, estructura y convenciones de redacción de los skills del proyecto.

## Flujo SDD (Spec-Driven Development)

El desarrollo de cualquier funcionalidad nueva se hace siguiendo el pipeline de skills `/sdd-*`. La descripción de cada skill, su orden de ejecución y los skills auxiliares está en [`agent_docs/sdd-workflow.md`](agent_docs/sdd-workflow.md). Consúltalo siempre que vayas a trabajar con el pipeline SDD.

## Arquitectura

La descripción de la arquitectura (paquetes de `com.educaflow`, sistemas vs subsistemas y la arquitectura especial de expedientes) está en [`agent_docs/architecture.md`](agent_docs/architecture.md). Las invariantes **verificables** de esa arquitectura (dependencias entre capas, Controller→Service→Repository, nomenclatura/ubicación) están catalogadas como reglas verificables (formato ADR, sin código) en [`agent_docs/architecture-rules.md`](agent_docs/architecture-rules.md), de las que `/developer-create-arch-tests` genera los tests ArchUnit. **Ambos ficheros deben mantenerse coherentes entre sí.** Cárgalos solo cuando trabajes con la arquitectura.

## Vistas

Las **convenciones verificables de las vistas Axelor** (los XML bajo `**/views/*.xml` y `menus.xml`: nomenclatura, botones, action-groups, forms/grids, referencias, modales, menús) están catalogadas como reglas verificables (formato ADR `VAR-<categoría>.<n>`, sin código) en [`agent_docs/view-rules.md`](agent_docs/view-rules.md) — el equivalente para vistas de [`architecture-rules.md`](agent_docs/architecture-rules.md) —, de las que `/developer-create-view-tests` genera los tests (JUnit 5 planos en `src/test/java/com/educaflow/views`, una clase por categoría, que leen los XML con JAXP/XPath; **no** usan ArchUnit porque este analiza bytecode, no XML). `view-rules.md` es la **fuente de verdad** de esos tests: para cambiar un test se edita el markdown y se re-ejecuta `/developer-create-view-tests`, nunca se editan los `.java` a mano. **`view-rules.md` debe mantenerse coherente con los skills `k-vistas`** (que describen esas convenciones en prosa). Cárgalo solo cuando trabajes con las vistas o sus tests.



## i18n
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

Existe un usuario administrador con login `admin` y contraseña `admin`.

La sistemas o subsistemas que existen o van a existir son:
- Registro de entrada/salida: Los documentos que se presentan en la aplicación y los que genera la aplicación para los usuarios
- Notificaciones: Cada una de las notificaciones que se envian a los usuarios. Por ahora las únicas notificaciones son correos electrónicos
- Firmas: Permite a los usuarios firmas documentos que se ponen a él a firmar. Es una idea similar a la aplicación de Portafirmas del gobierno de españa pero más sencilla.
- Gestión de centro: Permite configurar todo lo que necesita un centro, como los usuarios, permisos, cargos, etc.
- Carpeta ciudadana: Contiene todo lo referido a un alumno o profesor (notificaciones, registros de entrada y salida, expedientes, firmas)