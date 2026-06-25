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

## Base de datos

- PostgreSQL **12.22**. Conexión por defecto (en `src/main/resources/axelor-config.properties`,
  `db.default.*`): `jdbc:postgresql://localhost:5432/educaflow`, usuario `educaflow`, contraseña `educaflow`.
- El esquema lo gestiona Axelor automáticamente (`db.default.ddl = update`): no hay migraciones manuales.

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
