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

## Script del proyecto

- Para compilar el proyecto ejecuta: `./gradlew clean build --info`
- Para ejecutar el proyecto ejecuta: `./gradlew --no-daemon run --debug-jvm --port 8080 --context-path /`


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
- menus-steps → Su tarea es indicar los pasos a seguir para crear o modificar un menú, usa el skill `menus-knowledge` para resolver cualquier duda sobre los pasos a seguir
- menus-reviewer → Su tarea es revisar los menús existente y detectar fallos o mejoras,usa el skill `menus-knowledge` para resolver cualquier duda sobre como deben ser las cosas.
- menus-builder-orchestrator → Su tarea es crear nuevos menús desde cero o modificar los existentes, siguiendo los pasos indicados en el skill `menus-steps` y los revisa con el skill `menus-reviewer` . Pero de forma iterativa, es decir, va creando o modificando los menús poco a poco con `menus-steps` y revisando cada paso con el skill `menus-reviewer` para asegurarse de que va por el buen camino.
- menus-fixer-orchestrator → Su tarea es revisar lo que ya está hecho para detectar errores, inconsistencias o mejoras usando el skill `menus-reviewer` y corriendolo con el skill `menus-steps`. Pero de forma iterativa, es decir, va detectando errores o mejoras con `menus-reviewer` y corrigiendo poco a poco con `menus-steps` para asegurarse de que va por el buen camino.

Los skills importantes son los `-orchestrator`, ya que son los encargados de realizar las acciones en forma de iteraciones susesivas para realizar las tareas.


### JPQL en `domain` — reglas del proyecto
- `:__user__` es el objeto `User` (no un Long) → usar `cu.usuario = :__user__` sin `.id`
- En dominios de **permisos** (`auth.xml`): los parámetros son **posicionales sin índice** (`?`), NO `?1`/`?2`. Cada `?` en el JPQL consume una posición del array de params en orden. Si el mismo valor aparece múltiples veces en la condición, debe repetirse en `conditionParams`. El `input-config.xml` usa `@condition`/`@conditionParams` como atributos XML, NO `<domain>`/`<domain-params>`:
  ```xml
  <permission condition="self.centro = ? AND self.usuario = ? AND self.centro = ?"
              conditionParams="__user__.centroActivo, __user__, __user__.centroActivo">
  ```
- En dominios de **action-view/panel**: los parámetros sí son nombrados (`:__user__`), pero `:__user__.campo` NO funciona — usar subquery scalar
- `self` en subconsultas EXISTS puede no correlacionarse → usar patrón `self.id IN (SELECT ...)`
- En permisos, preferir comparaciones de entidad directas (`self IN (SELECT aa.tramite ...)`, `aa.actor IN (SELECT cut.tipoUsuario ...)`) en lugar de comparar IDs
- Entidades con herencia JOINED (`TipoUsuario`, `CentroUsuario`) → navegar a campos de subtipo puede fallar; usar subselect explícito:
  `t.tipoUsuario IN (SELECT tu FROM TipoUsuario tu WHERE tu.code = 'X')`
- 


## Architectura

Existen 5 grandes paquetes en `com.educaflow.`:
- **`base/util/`** — shared utilities: `JsonUtil`, `MetaFileUtil`, `ActionRequestHelper`, `AllowProperties`, `AxelorViewUtil`, `TextUtil`, `Convert`, `DniUtil`, `ReflectionUtil`, `SecurityUtil`, `CryptoUtil`, `XmlUtil`
- **`base/infrastructure/`** — infrastructure modules: `pdf` (iText PDF operations), `validation` (BusinessMessages/BusinessException/ValidationEngine DSL), `criptografia` (X.509 certs, HSMs, FNMT/ACCV/DNI issuers), `autofirma` (desktop client integration), `mapper` (BeanMapperModel), `mail`, `evaluator` (Groovy expressions), `numeradores`, `metafile`
- **`subsystem/`** — business subsystems: `firmas`, `expedientes`, `registroentradasalida`, `pdfutilities`, `common`, `certificados`, `importer`, `sistemaeducativo`, `security`
- **`system/`** — business systems
- **`secretariavirtual/`** — top-level menus and navigation

## Sistemas y subsistemas



## Expedientes


## i18n
Nunca jamás, crear los ficheros `i18n_ca.csv` ni `i18n_es.csv` ya que hay un script que los genera automáticamente, así que es totalmente innecesario.
A veces hay palabras que acaban con "__!!" como en "AutoFirma__!!" esto es para indicar que esa palabra no se debe traducir, ya que el script de generación de i18n las deja tal cual pero sin el "__!!" al final, así que no hay que preocuparse por eso.
Por eso cuando se usa la palabra por ejemplo para ponerla en formato camelCase hay que quitar el "__!!" y ponerla en formato camelCase, por ejemplo "AutoFirma__!!" se convierte en "autoFirma" para usarla en el código.
