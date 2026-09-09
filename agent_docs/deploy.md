# Deploy y entorno de desarrollo — secretaría virtual

Cómo compilar, probar, arrancar la app y gestionar la base de datos en el entorno **host**
(`/home/logongas/Documentos/desarrollo/educaflow/secretaria-virtual`).

> **CRITICAL — contenedor Docker `/build`: NO tocar.** Existe **otra** instancia de la app
> corriendo en un contenedor Docker con el repo montado en `/build/secretaria-virtual` (es del
> usuario). **MUST NOT** pararla, matar sus procesos ni interferir con ella. Tu gestión (arrancar
> con `./run.sh`, parar por puerto) se limita **solo** a la instancia host. Antes de matar algo
> por puerto (`fuser -k 8080/tcp`, `lsof`), confirma que el PID dueño del socket pertenece a la
> ruta host (`/home/logongas/...`) y no a `/build/...` — matar por puerto es ciego.

## Compilar y arrancar la app

- Para compilar **y arrancar** la app lanza **siempre** `./run.sh`. Hace `./gradlew clean build`
  (compila Y ejecuta los tests; si fallan, imprime qué tests fallaron y **NO** arranca la app) y
  luego arranca en el puerto **8080** con la config privada
  (`--config ../secretaria-virtual-private/axelor-config.dev.properties`).
- **NO** invoques `gradlew run` a mano ni añadas `--debug-jvm`: ese flag suspende la JVM esperando
  un depurador, así que la app nunca llega a responder; no usarlo para arrancar de forma desatendida.
- Si solo necesitas **compilar sin arrancar**: `./gradlew clean build --info`.
- Compilar solo el código (sin tests): `./gradlew compileJava` (o `compileTestJava` para los tests).

## Probar los tests

- `./gradlew clean build` compila Y ejecuta los tests JUnit (es lo que hace `./run.sh` antes de arrancar).
- Solo los tests, sin arrancar: `./gradlew test`.
- Un test o clase concretos: `./gradlew test --tests 'com.educaflow.architecture.*'` (patrón por FQN).
- Resultados (fuente de verdad):
  - Informe HTML: `build/reports/tests/test/index.html`.
  - XML por test: `build/test-results/test/*.xml` (cada `<testcase>` con `<failure>`/`<error>` es un fallo).

### Mutation testing (PIT)

- Mide la **calidad** de los tests unitarios con [PIT](https://pitest.org/): introduce cambios pequeños
  en el bytecode (mutantes: invertir un `if`, devolver `null`, quitar una llamada…) y comprueba si algún
  test falla. Un mutante que sobrevive es código cuyo comportamiento ningún test está comprobando.
- **No** forma parte de `build` ni de `./run.sh` (es lento): se lanza a mano con `./gradlew pitest`.
- Informe HTML: `build/reports/pitest/index.html` (XML en `build/reports/pitest/mutations.xml`).
- Solo muta el código escrito a mano (excluye lo generado: `*.db.*`, `States`, numeradores) y solo ejecuta
  los tests unitarios de `base`, `system` y `subsystem`; los de `architecture`, `views` y
  `tiposexpedientes` comprueban estructura/XML y no matan mutantes. La configuración está en el bloque
  `pitest { }` de `build.gradle`.
- Las métricas que importan: **Test strength** (mutantes matados entre los que algún test cubre: lo bueno que
  es el test que hay) y **Mutations with no coverage** (código sin ningún test unitario que lo toque).

### Análisis estático (Error Prone)

- [Error Prone](https://errorprone.info/) detecta errores de programación habituales **dentro de javac**, en cada compilación.
  Va enganchado a `compileJava` y `compileTestJava`, así que `./gradlew build` y `./run.sh` ya lo pasan: no hay ninguna tarea aparte que lanzar.
- Cada aviso lleva el nombre del check entre corchetes (`warning: [JavaTimeDefaultTimeZone] ...`) y un enlace a su documentación.
  Si una compilación falla con un mensaje de esa forma, el fallo viene de Error Prone, no de javac.
- Los checks de severidad **ERROR rompen la compilación**; los **WARNING solo avisan** por consola.
  No genera informe: los avisos salen en la salida de la compilación.
  Como Gradle no recompila lo que está al día, para volver a verlos todos hay que forzar la recompilación (`./gradlew compileJava compileTestJava --rerun-tasks`).
- Solo analiza **Java**: el código Kotlin no pasa por él, y el código generado (`build/src-gen`, `build/src-gen-states`) está excluido a propósito.
- Un falso positivo se silencia **en el sitio concreto** con `@SuppressWarnings("NombreDelCheck")`, nunca desactivando el check para todo el proyecto.
  La configuración (plugin `net.ltgt.errorprone`, versión fija de `error_prone_core`, exclusiones) está en `build.gradle`.

## Base de datos

- PostgreSQL **12.22**. Conexión por defecto (en `src/main/resources/axelor-config.properties`,
  `db.default.*`): `jdbc:postgresql://localhost:5432/educaflow`, usuario `educaflow`, contraseña `educaflow`.
- El esquema lo gestiona Axelor automáticamente (`db.default.ddl = update`), pero **además** `DataBaseStartup.startup()`
  ejecuta **Flyway** en cada arranque, sobre `classpath:com/educaflow/secretariavirtual/startup/database`. Hoy esa
  carpeta está vacía, así que no hay ninguna migración que aplicar; si necesitas una, ése es su sitio.

### Arrancar / reiniciar la BD (Docker)

La BD se levanta como contenedor `educaflow-db` (mismo comando que está comentado en `run.sh`):

```bash
docker run --name educaflow-db --hostname educaflow-db \
  -e POSTGRES_USER=educaflow -e POSTGRES_PASSWORD=educaflow -e POSTGRES_DB=educaflow \
  -p 5432:5432 -d --rm postgres:12.22
```

- **Reiniciar** (sin perder datos, si el contenedor sigue vivo): `docker restart educaflow-db`.
- **Resetear desde cero**: como el comando usa `--rm` y no monta volumen, al parar el contenedor
  se borra su almacenamiento. `docker stop educaflow-db` y vuelve a lanzar el `docker run` de arriba
  → BD limpia (Axelor recrea el esquema al arrancar la app con `ddl = update`).

### Acceder con el CLI de PostgreSQL (`psql`)

- Desde el host (pide contraseña `educaflow`, o pásala con `PGPASSWORD`):

  ```bash
  PGPASSWORD=educaflow psql -h localhost -p 5432 -U educaflow -d educaflow
  ```

- Desde dentro del contenedor:

  ```bash
  docker exec -it educaflow-db psql -U educaflow -d educaflow
  ```

- Para consultas puntuales de **solo lectura** desde el asistente, está el MCP de PostgreSQL
  (`mcp__postgres__query`); ver [`mcp.md`](mcp.md).

## Configuración

Las propiedades van en `src/main/resources/axelor-config.properties` (`db.default.*`, `mail.*`,
`quartz.*`, etc.). Las propiedades **privadas** (credenciales reales, secretos) van en el fichero
externo `../secretaria-virtual-private/axelor-config.dev.properties`, que se pasa con `--config` al
arrancar y **sobrescribe** los valores del primero.
