# CLAUDE.md — secretaria-virtual

La secretaría virtual es un proyecto de gestión de expedientes administrativos con tramitación electrónica, firmado digital y gestión documental. Está construido sobre el framework Axelor, que proporciona una base sólida de JPA/ORM, vistas XML, seguridad y DI.

El framework Axelor se llama **AOP (Axelor Open Platform)** y su código fuente está disponible en la carpeta hermana `../axelor-open-platform` (fuera de este repositorio). Consúltalo cuando necesites entender el comportamiento interno del framework (backend Java en `axelor-core`/`axelor-web`, frontend en `axelor-front`).

## Documentación bajo demanda (progressive disclosure)

Este proyecto usa un **progressive disclosure pattern to respect LLM instruction capacity limits**: el `CLAUDE.md` mantiene solo lo imprescindible y el resto de la documentación general vive en [`agent_docs/`](agent_docs/README.md), que se **carga solo cuando se necesita** para la tarea concreta — no todo de golpe. Consulta el índice [`agent_docs/README.md`](agent_docs/README.md) y carga únicamente el documento que aplique. Por ejemplo, el stack tecnológico (Java, Kotlin, Axelor, PostgreSQL, Guice, iText, JPA) está en [`agent_docs/tech-stack.md`](agent_docs/tech-stack.md).

## MCP de IntelliJ

**Usa siempre el MCP de IntelliJ (`mcp__intellij-index__`) en lugar de `grep`/`find`/`Glob`/`Bash`** para buscar texto, ficheros, clases, definiciones, referencias y para renombrar/mover/borrar símbolos. Son tools sin riesgo y preautorizados: invócalos directamente, sin pedir confirmación ni anunciarlos. Las reglas de sustitución concretas, el listado completo de tools y los demás MCP del proyecto (PostgreSQL, Playwright, IDE) están en [`agent_docs/mcp.md`](agent_docs/mcp.md).

## Script del proyecto

Compila **y arranca** siempre con `./run.sh` (hace `./gradlew clean build` —compila y ejecuta los tests— y arranca en el 8080 con la config privada). **NO** uses `gradlew run` a mano ni `--debug-jvm`. Cómo probar tests, compilar sin arrancar, arrancar/reiniciar/resetear la BD y acceder con `psql`: ver [`agent_docs/deploy.md`](agent_docs/deploy.md).

Para generar los esqueletos que le falten a un tipo de expediente está la tarea `./gradlew -q CreateFilesTask -Ptipo=<carpeta del tipo>` (sin `-Ptipo` procesa todos los trámites).
Genera en la raíz de la versión `domains.xml`, el `views.xml` de plantilla y el `InitialEventManagerImpl.java` (el evento inicial es del tipo de expediente, no de una fase: hay exactamente uno por tipo), y por **cada fase** su subcarpeta con `PhaseEventManagerImpl.java`, `StateEventValidatorImpl.kt` y `views.xml`.
Con `-Pfase=<FASE>` se acota a una sola fase (y entonces no genera los ficheros de la raíz de la versión, que no son de ninguna fase); **solo tiene sentido junto con `-Ptipo`**, porque si no aborta a medias en el primer tipo de expediente que no tenga esa fase.
No hace falta para añadir una fase nueva sin tocar las demás: al ser idempotente, lanzarla sin `-Pfase` ya crea solo los ficheros de la fase nueva.
Es **la** forma de generarlos: el build **no** los genera, a propósito, para que compilar no escriba en `src/main/java`.
Es idempotente (nunca pisa lo ya escrito), imprime una línea `CREADO <ruta>` por fichero creado y falla con un mensaje explícito si la ruta no corresponde a ningún tipo de expediente o si la fase no existe.
Detalle en el skill `k-tipo-expediente`.

Los diagramas PlantUML los renderiza la tarea `./gradlew -q GenerateDocs`, que recorre `src/` y genera un `<nombre>.png` junto a cada `<nombre>.puml` / `<nombre>.plantuml`.
No hace falta invocar el jar de PlantUML a mano: la dependencia `net.sourceforge.plantuml:plantuml` está en el `buildscript` y la tarea la usa embebida.
Va enganchada a `build` con `finalizedBy`, así que **`./run.sh` ya la ejecuta**; lánzala suelta solo cuando toques un `.puml` y no quieras compilar entero.
Es incremental por fecha: omite el PNG que sea más reciente que su fuente, así que **si editas un `.puml` MUST regenerar el PNG** (queda desincronizado si no).


## Configuración

La configuración de la aplicación está en [`src/main/resources/axelor-config.properties`](src/main/resources/axelor-config.properties). **Ese es el fichero donde deben estar las propiedades de configuración**, así que cuando busques o añadas una propiedad de configuración mírala/ponla ahí. Contiene, entre otras: información de la aplicación, modo (`application.mode`), locale, página de login, base de datos (`db.default.*`), Quartz scheduler (`quartz.*`, incluido `correos.envio.cron`), correo SMTP/IMAP (`mail.*`), adjuntos (`data.upload.*`), logging y entorno criptográfico (`entornoCriptografico.*`).

El otro fichero de configuración que se pasa al arrancar (`--config ../secretaria-virtual-private/axelor-config.dev.properties`, fuera de este repositorio) es para las propiedades **privadas**: las que no deben versionarse ni hacerse públicas (credenciales reales, secretos, etc.). Sus valores **sobrescriben** a los de `axelor-config.properties`.


## Skills
Debido a que toda la aplicación está fuertemente acoplada al framework Axelor y que debes tener pocos conocimientos de Axelor se ha creado un sistema de skills para gestionar toda la parte de axelor.

Los skills se agrupan en **cuatro familias por prefijo** (las reglas de autoría de cada una están en `/k-skill`):
- `k-*` → **knowledge**: conocimiento de dominio reutilizable (convenciones, patrones, vocabulario). No hacen cosas, las describen; otros skills los cargan como referencia.
- `sdd-*` → **action**: los pasos del pipeline SDD (ver [`agent_docs/sdd-workflow.md`](agent_docs/sdd-workflow.md)).
- `developer-*` → **action**: procesos que producen o revisan código real en el árbol del proyecto (p.ej. `developer-code-implementer`, `developer-create-arch-tests`, `developer-create-view-tests`).
  La revisión está separada por tipo de artefacto, un skill por artefacto: `developer-code-reviewer` (código Java/Kotlin), `developer-view-reviewer` (vistas XML) y `developer-model-reviewer` (modelos de dominio XML). Los tres son wrappers finos: el bucle de revisión lo aporta el motor `skill-orquestador-reviewer` y **qué** revisa cada uno lo declara su `review-contract.md`. Para cambiar el criterio de una revisión se edita ese contrato, no el `SKILL.md`.
- `skill-*` → **action (meta)**: skills que ayudan a otros skills de forma **abstracta**, sin conocimiento del dominio ni del problema. Dos usos: motores de proceso reutilizables que otros skills invocan con su propio contrato (`skill-orquestador-reviewer`) y skills que evalúan o mejoran otros skills (`skill-eval`, `skill-reviewer`). El conocimiento de cómo se escribe un skill vive en `k-skill`.

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
- trámites (`k-tramite`) → alta y mantenimiento de un trámite: la carpeta `tramites/<tramite>/`, el fichero maestro `TramiteInstance.xml`, i18n del nombre y permisos.
- tipos de expediente (`k-tipo-expediente`) → todo lo que hay dentro de una carpeta de versión `tramites/<tramite>/<vN>/`: `TipoExpedienteInstance.xml` con sus **fases** y la máquina de estados, modelo (`modelo.md`), `PhaseEventManager` (`phaseeventmanager.md`), `StateEventValidator` (`validator.md`), vistas preprocesadas (`vistas.md`), documentos PDF de `documentospdf/` (`documentos.md`) y cómo duplicar un tipo para crear una versión nueva (`versionado.md`). Consúltalo siempre que toques cualquier fichero bajo una carpeta de versión.
  Los estados se agrupan en **fases** (solo una agrupación de ficheros, no una entidad del dominio), cada una con su subcarpeta `<vN>/<fase en minúsculas>/`.
  Un estado se identifica por la pareja `(codePhase, codeState)`, que son dos columnas del expediente; no existe ningún nombre compuesto. La máquina de estados de cada tipo es la clase `States` que la tarea `GenerateStatesTask` proyecta de su `TipoExpedienteInstance.xml` en `build/src-gen-states/main/java`: es **generada**, no se versiona ni se edita, y en código un estado se nombra por su constante en el enum de la fase, con la fase en UpperCamelCase: `States.Recepcion.ENTRADA_DATOS`.

Es imperativo que siempre uses los skills correspondientes para cualquier acción relacionada con Axelor, ya que siguen una arquitectura propia de la secretaría virtual y del framework Axelor.

### Modificación de skills

Siempre que crees o modifiques un skill (cualquier `SKILL.md` en `.claude/skills/`), **MUST** consultar y aplicar el skill `/k-skill`, que define las reglas, frontmatter obligatorio, estructura y convenciones de redacción de los skills del proyecto.

## Flujo SDD (Spec-Driven Development)

El desarrollo de cualquier funcionalidad nueva se hace siguiendo el pipeline de skills `/sdd-*`. La descripción de cada skill, su orden de ejecución y los skills auxiliares está en [`agent_docs/sdd-workflow.md`](agent_docs/sdd-workflow.md). Consúltalo siempre que vayas a trabajar con el pipeline SDD.

La carpeta `.sdd/` (specs, diseños, tareas, drafts y archive) es **material de trabajo del pipeline**, NO documentación del proyecto.
**MUST NOT** leerla ni citarla como fuente para entender qué hace el código, ni para responder preguntas, ni para diseñar o implementar cambios fuera del pipeline.
Solo la usan los skills `/sdd-*` mientras ejecutan su propio paso.
Motivo: después de programar con los `/sdd-*` se cambian cosas a mano, así que lo que hay en `.sdd/` puede no corresponderse con el código real y se queda desactualizado sin avisar.
La fuente de verdad es siempre el código, y para lo normativo `CLAUDE.md`, `agent_docs/` y los skills.

## Arquitectura

La descripción de la arquitectura (paquetes de `com.educaflow`, sistemas vs subsistemas y la arquitectura especial de expedientes) está en [`agent_docs/architecture.md`](agent_docs/architecture.md). Las invariantes **verificables** de esa arquitectura (dependencias entre capas, Controller→Service→Repository, nomenclatura/ubicación) están catalogadas como reglas verificables (formato ADR, sin código) en [`agent_docs/architecture-rules.md`](agent_docs/architecture-rules.md), de las que `/developer-create-arch-tests` genera los tests ArchUnit. **Ambos ficheros deben mantenerse coherentes entre sí.** Cárgalos solo cuando trabajes con la arquitectura.

## Vistas

Las **convenciones verificables de las vistas Axelor** (los XML bajo `**/views/*.xml` y `menus.xml`: nomenclatura, botones, action-groups, forms/grids, referencias, modales, menús) están catalogadas como reglas verificables (formato ADR `VAR-<categoría>.<n>`, sin código) en [`agent_docs/view-rules.md`](agent_docs/view-rules.md) — el equivalente para vistas de [`architecture-rules.md`](agent_docs/architecture-rules.md) —, de las que `/developer-create-view-tests` genera los tests (JUnit 5 planos en `src/test/java/com/educaflow/views`, una clase por categoría, que leen los XML con JAXP/XPath; **no** usan ArchUnit porque este analiza bytecode, no XML). `view-rules.md` es la **fuente de verdad** de esos tests: para cambiar un test se edita el markdown y se re-ejecuta `/developer-create-view-tests`, nunca se editan los `.java` a mano. **`view-rules.md` debe mantenerse coherente con los skills `k-vistas`** (que describen esas convenciones en prosa). Cárgalo solo cuando trabajes con las vistas o sus tests.

## Tipos de expediente

Los tests de `src/test/java/com/educaflow/tiposexpedientes` comprueban que lo que se escribe a mano en un tipo de expediente y en cada una de sus fases concuerda con su `TipoExpedienteInstance.xml` y con su `domains.xml`.
**A diferencia de las dos familias anteriores, estos tests se escriben A MANO**: los `.java` son la fuente de verdad y se editan directamente.
**MUST NOT** crear un `agent_docs/*-rules.md` ni un skill generador para ellos.
Qué comprueba cada regla, y cómo están construidos, está en el skill `k-tipo-expediente` (`SKILL.md` §3.3, `phaseeventmanager.md` §7, `validator.md` §5), que **debe mantenerse coherente con estos tests**.



## PENDIENTE (importante) — el endpoint REST automático se salta el tramitador

Axelor publica `POST /ws/rest/<FQN>` para **toda** entidad sin que haya que registrar nada (`RestService`, `@Path("/rest/{model}")`).
Esa ruta es `Resource.save` → `ModelService.validate` → `AllowProperties.filter` → `JPA.edit` → `ModelService.insert/update`, y **no pasa por `Tramitador` en ningún punto**.
Consecuencia: la whitelist por pareja (estado, evento) que `Tramitador` construye a partir de `@BeanValidationRulesForStateAndEvent` **no defiende esta puerta** — ahí lo único que filtra es el `allowPropertiesInsert/Update` del `ModelService` de la entidad, y sin `ModelService` propio eso es `createAllowAllProperties()`.
**Ahora mismo no hay NADA tapando esa puerta**, y es deliberado.
Hubo un parche (un `ModelService` de expedientes que devolvía `createDenyAllProperties()`) y **se retiró a propósito**: tapaba solo una parte del problema y se prefirió no dar la sensación de que el asunto estaba resuelto mientras no se decida la solución de verdad.
**MUST NOT** volver a introducirlo como arreglo puntual sin haber tomado esa decisión.

**MUST NOT** dar por protegida una entidad de expediente porque su tramitación valide por evento: son dos puertas distintas y la validación por evento solo ve una.

Puntos concretos que siguen abiertos:
- Cualquier usuario con permiso de escritura sobre una subclase de `Expediente` puede dictar por REST el valor de cualquier campo, incluidos `codePhase`/`codeState`/`abierto`, `centro` y `usuarioRegistrador`, saltándose la máquina de estados.
  `auth-expedientes.xml` concede `create`/`read`/`write`/`remove` sin `condition` sobre `PruebaV1`.
- `AllowProperties.filter` conserva siempre `id`, `version` y las claves `_`.
  Con deny-all un update queda en no-op, pero un alta sin `id` llega a `JPA.edit` con el mapa vacío: **sin comprobar** si crea una fila vacía o revienta contra los `NOT NULL`.
- `FormacionCentroTrabajo` no tiene `ModelService` ni hereda de `Expediente`, y `auth-expedientes.xml` le concede `create`/`write`/`remove` sin `condition`: sigue en allow-all.
- Los permisos de las subclases se conceden sin `condition` (p. ej. `PruebaV1.all`) y `AuthSecurity` **no recorre superclases**, así que no heredan las condiciones de los permisos de `Expediente`.

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