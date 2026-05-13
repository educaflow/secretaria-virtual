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

### Conjuntos de Skills
A fin de crear skills lo más especificos posibles se han creado conjuntos de skills para cada parte de la aplicación.

Por ejemplo para los menus se han creado los siguientes skills:
- menus-knowledge → No hace nada, solo es información sobre cómo funcionan los menús
- menus-steps → Su tarea es indicar los pasos a seguir para crear o modificar los menús, se usa el skill `menus-knowledge` para resolver cualquier duda sobre los pasos a seguir
- menus-reviewer → Su tarea es revisar los menús existente y detectar fallos o mejoras, se usa el skill `menus-knowledge` para resolver cualquier duda sobre como deben ser las cosas.
- menus-builder-orchestrator → Su tarea es crear nuevos menús desde cero o modificar los existentes, siguiendo los pasos indicados en el skill `menus-steps` y los revisa con el skill `menus-reviewer` . Pero de forma iterativa, es decir, va creando o modificando los menús poco a poco con `menus-steps` y revisando cada paso con el skill `menus-reviewer` para asegurarse de que va por el buen camino. Y vuelve a repetir este proceso iterativo hasta que todo funcione perfectamente.
- menus-fixer-orchestrator → Su tarea es revisar lo que ya está hecho para detectar errores, inconsistencias o mejoras usando el skill `menus-reviewer` y corriendolo con el skill `menus-steps`. Pero de forma iterativa, es decir, va detectando errores o mejoras con `menus-reviewer` y corrigiendo poco a poco con `menus-steps` para asegurarse de que va por el buen camino. Y vuelve a repetir este proceso iterativo hasta que todo funcione perfectamente.

Los skills importantes son los `-orchestrator`, ya que son los encargados de realizar las acciones en forma de iteraciones susesivas para realizar las tareas.

Igual que hemos puesto el ejemplo de el conjunto de skills de `menus` están para cualquiero otro tipo de skill




## Framework SDD (Spec-Driven Development)

Para construir o modificar sistemas/subsistemas se usa un pipeline de 5 fases con artefactos versionados en disco. Cada fase consume el artefacto de la anterior. Los artefactos se identifican por una cabecera frontmatter obligatoria con el campo `type:`.

### Pipeline

```
nombre iniciativa    →  /sdd-create-user-story  →  user-story.md (plantilla a rellenar)
user-story.md        →  /sdd-analyst-system     →  analysis.md      (type: analysis)
analysis.md          →  /sdd-designer-system    →  design_NN.md     (type: design)
design_NN.md         →  /sdd-implementer-system →  código real (drafts intactos)
código + draft       →  /sdd-close-spec         →  CLAUDE.md + archivo en .sdd/specs/
```

- **`/sdd-create-user-story`** — arranca una iniciativa: crea la carpeta `.sdd/drafts/YYYY-MM-DD_HH-MM_{nombre-kebab}/` con un `user-story.md` plantillado (`type: user-story`). El usuario rellena la plantilla; el skill no completa la historia por ti.
- **`/sdd-analyst-system`** — convierte una historia de usuario en análisis funcional. Hace preguntas iterativas con `AskUserQuestion` antes de generar nada (HARD-GATE: nunca genera sin aprobación). Produce entidades, operaciones, vistas, seguridad y **tres tablas paralelas de reglas**: validaciones `V-XXX` (bloquean), reglas de negocio `R-XXX` (actúan sobre el sistema) y reglas de UI `U-XXX` (solo cambian el formulario), cada una con sus columnas propias y mensajes al usuario donde corresponda. Internamente lanza **5 subagentes en paralelo** y unifica.
- **`/sdd-designer-system`** — convierte el análisis en un **diseño**, NO una implementación. Estructura de clases/métodos/vistas/acciones con firmas y comentarios descriptivos, pero **sin cuerpos de método ni XML literal de vistas/acciones** (la única excepción son los dominios XML, que sí van completos). Cobertura total obligatoria: cada `V-XXX`, `R-XXX` y `U-XXX` del análisis debe tener una entrada en la matriz de trazabilidad apuntando a una clase+método o fichero+acción concreta. También lanza 5 subagentes en paralelo y unifica.
- **`/sdd-implementer-system`** — delega en `code-implementer` pasándole el diseño y los skills (`k-sistemas`, `k-vistas`, opcionalmente `k-seguridad`). NO implementa nada por sí mismo y **NO archiva nada** en `.sdd/specs/`. Tras implementar, los drafts quedan intactos.
- **`/sdd-close-spec`** — cierra la iniciativa cuando el usuario está conforme con la implementación: usa `git diff` para identificar los ficheros que cambiaron, regenera los `CLAUDE.md` de las carpetas afectadas desde el código real, y archiva los tres artefactos en `.sdd/specs/NNNN_{desc}/` en versión **as-built**: el `analysis.md` y el `design.md` se corrigen para reflejar la implementación real, y el `user-story.md` se ajusta solo si la implementación reveló contradicciones evidentes con la intención original (cambios excepcionales). Cada artefacto archivado lleva una sección "Notas de cierre (as-built)" listando los cambios o "Sin cambios respecto al draft original."

### Estructura de carpetas

```
.sdd/
├── drafts/
│   └── YYYY-MM-DD_HH-MM_{resumen-5-palabras}/   ← carpeta de iniciativa
│       ├── user-story.md            (type: user-story)
│       ├── design-guidelines.md     (type: design-guidelines, opcional)
│       └── analysis_NN/             ← una subcarpeta por análisis (01, 02…)
│           ├── analysis.md          (type: analysis)
│           └── design_NN.md         (type: design, varios por análisis)
└── specs/
    └── NNNN_{descr}/                ← spec final tras implementar (0001, 0002…)
        ├── user-story.md
        ├── analysis.md
        ├── design.md
        └── design-guidelines.md     (si existía)
```

- La iniciativa se identifica por `YYYY-MM-DD_HH-MM_{resumen-kebab-case}`.
- `analysis_NN/` se numera por iniciativa (01, 02…); `design_NN.md` se numera por análisis.
- En `.sdd/specs/` la numeración es global con 4 dígitos y solo cuenta carpetas que empiezan por `^[0-9]{4}_`.
- `design-guidelines.md` persiste a nivel de iniciativa: aplica a todos sus análisis y diseños.

### Tipos de frontmatter (obligatorios)

Cada artefacto empieza con un bloque `---` con uno de estos `type:`:

- `type: user-story` → input de `/sdd-analyst-system`
- `type: analysis` → output de `/sdd-analyst-system`, input de `/sdd-designer-system`
- `type: design-guidelines` → opcional, modifica el comportamiento de `/sdd-designer-system`
- `type: design` → output de `/sdd-designer-system`, input de `/sdd-implementer-system`

Si el frontmatter no coincide con lo que la fase espera, la fase se detiene con error.

### Auto-detección sin ruta

Si se invoca un skill sin ruta, busca el último artefacto disponible (orden alfabético del prefijo timestamp coincide con el cronológico) y pregunta al usuario con `AskUserQuestion` si quiere usarlo.

### Reglas críticas del framework

- **NUNCA** usar como referencia `expedientes`, `tiposexpedientes` ni `tramites` — siguen otra arquitectura.
- **NUNCA** leer otros `analysis*.md` ni `design*.md` previos como plantilla — cada artefacto se genera desde su input directo y el código real.
- En el diseño: dominios XML completos sí, **cuerpos Java implementados y XML literal de acciones/vistas NO**.
- Tres categorías de reglas sobre las entidades (skill `k-validaciones`):
  - **Validaciones (`V-XXX`)** — condiciones que **bloquean** una operación si no se cumplen; viven en el modelo XML (`required`/`unique`/...), en `validateInsert`/`validateUpdate`/`validateRemove` del servicio (fuente de verdad) y opcionalmente en `<action-condition>`/`<action-validate>` del cliente para UX.
  - **Reglas de negocio (`R-XXX`)** — acciones que el sistema **ejecuta** ante un evento (insert/update/remove/cambio de estado); se implementan como métodos `fireActionRule_*` en el `*ServiceImpl`, antes o después de `super.*` según escriban en el mismo registro o tengan efectos colaterales.
  - **Reglas de UI (`U-XXX`)** — cambios en el **formulario** (mostrar/ocultar, readonly, required, valor por defecto, filtrado de dominio) según el valor de otros campos, el usuario o el padre; se implementan con `showIf`/`hideIf`/`readonlyIf`/`requiredIf` o con `<action-attrs>`/`<action-record>` desde eventos `onNew`/`onLoad`/`onChange`.
- NO crear módulos Guice para `ModelService` (los descubre `ModelServiceFactory`).
- NO crear listeners JPA para lógica de negocio (va en el servicio como `fireActionRule_*`).


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