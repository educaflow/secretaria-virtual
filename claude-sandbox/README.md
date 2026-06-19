# claude-sandbox

Entorno Docker aislado para trabajar con **Claude Code** sobre la secretaría virtual:
trae todo el tooling para **compilar y ejecutar** el proyecto y para correr **Playwright**,
más una base de datos **PostgreSQL** privada.

Levanta **2 contenedores**:

| Servicio         | Imagen                  | Qué es                                                              |
|------------------|-------------------------|--------------------------------------------------------------------|
| `db`             | `postgres:12.22`        | BD privada (misma versión que el devops). **No** publica puertos.  |
| `claude-sandbox` | construida del Dockerfile | Claude Code + JDK 21 + Node 24 + Maven + pnpm + Playwright + tooling |

La BD solo es accesible desde la red interna `backend`; la app llega a ella por el
hostname `educaflow-db`.

## Qué incluye la imagen `claude-sandbox`

- **Java**: Eclipse Temurin 21 (JDK)
- **Node.js 24** + `npm`/`npx` + **corepack** (pnpm)
- **Maven**
- **Claude Code** (`@anthropic-ai/claude-code`, global)
- **Playwright** + Chromium con sus libs de sistema (en `/ms-playwright`)
- Herramientas del sistema del devops: `git`, `curl`, `gnupg`, `graphviz`,
  `apertium`, `apertium-es-ca`, además de `psql`, `sudo`, `less`, `procps`
- Zona horaria `Europe/Madrid`

## Cómo entra el código (volúmenes)

El código **no se clona** dentro de la imagen: se **monta desde el host** cada repo por
separado (así nunca se anida la propia carpeta `claude-sandbox`):

| Host                                   | Contenedor                            |
|----------------------------------------|---------------------------------------|
| `../../secretaria-virtual`             | `/workspace/secretaria-virtual`       |
| `../../axelor-ui`                      | `/workspace/axelor-ui`                |
| `../../axelor-open-platform`           | `/workspace/axelor-open-platform`     |
| `../../EducaFlowBuildTools`            | `/workspace/EducaFlowBuildTools`      |
| `../../secretaria-virtual-private`     | `/workspace/secretaria-virtual-private` |
| `~/.claude` y `~/.claude.json`         | auth de Claude Code (sesión del host) |
| `~/.m2` (host)                         | `/home/developer/.m2` (Maven del host) |

> Los 5 repos deben estar como carpetas hermanas bajo `…/educaflow/` (ya lo están).

**No se compila Axelor dentro del contenedor**: se comparte el `~/.m2` del host, así que
se reutilizan los jars de Axelor (axelor-ui, axelor-open-platform, EducaFlowBuildTools)
que ya compilaste en local. El arranque es instantáneo. Si esos jars no estuvieran en
tu `~/.m2`, compílalos una vez en el host ejecutando los `install.sh` de cada repo.

## Uso

```bash
cd claude-sandbox

# 1) (opcional) ajusta uid/gid si tu usuario del host no es 1000
cp .env.example .env      # edita HOST_UID / HOST_GID si hace falta

# 2) construye y levanta los 2 contenedores
docker compose up -d --build
```

El arranque es **instantáneo**: no se compila Axelor (se usa el `~/.m2` del host). Logs:

```bash
docker compose logs -f claude-sandbox
```

### Entrar con los MCP del host puenteados (recomendado)

```bash
./claude-sandbox.sh
```

Levanta los contenedores, **puentea los MCP que el host expone en `127.0.0.1`** (p.ej. el
de IntelliJ, descubiertos de `~/.claude.json` y los `.mcp.json`) para que sean accesibles
desde dentro del contenedor en el mismo `127.0.0.1:PUERTO`, entra en Claude y **deshace los
puentes al salir**. Pide `sudo` una vez (regla de firewall acotada; ver `CLAUDE.md`).

### Entrar a trabajar con Claude Code (sin puentear MCP)

```bash
docker compose exec -it claude-sandbox claude
# o una shell:
docker compose exec -it claude-sandbox bash
```

Dentro del contenedor `claude` arranca **sin restricciones de permisos**
(`--dangerously-skip-permissions`): el aislamiento lo da el propio contenedor. Si en algún
caso quieres Claude **con** las restricciones normales, llama a `/usr/bin/claude`.

Ya dentro, el directorio de trabajo es `/workspace/secretaria-virtual`. Para compilar
y arrancar la app (igual que en local):

```bash
./run.sh
```

La app queda en **http://localhost:8081** (8081 del host → 8080 del contenedor; el 8080
del host estaba ocupado por otra app). La BD la resuelve por `educaflow-db` gracias a las
variables `AXELOR_CONFIG_DB_*` del `docker-compose.yml` (no hay que tocar
`axelor-config.properties`).

### Playwright

Chromium ya está instalado en la imagen (`PLAYWRIGHT_BROWSERS_PATH=/ms-playwright`).
Si el proyecto fija una versión de Playwright distinta y se queja del navegador,
dentro del contenedor:

```bash
npx playwright install chromium
```

## Comandos útiles

```bash
docker compose ps                       # estado
docker compose logs -f claude-sandbox   # logs del sandbox
docker compose logs -f db               # logs de la BD
docker compose down                     # parar (conserva el volumen de la BD)
docker compose down -v                  # parar y BORRAR los datos de la BD
```

## Notas

- **No se compila Axelor** dentro del contenedor: se comparte el `~/.m2` del host. Si
  cambias una dependencia Axelor, recompílala en el host (su `install.sh`) y el jar
  nuevo queda disponible al instante dentro del contenedor.
- `docker compose down -v` borra solo el volumen de la BD (`postgres_data`); tu `~/.m2`
  del host nunca se borra (es un bind-mount).
- El build de la propia secretaría virtual (`./run.sh` → `./gradlew clean build`) sí se
  ejecuta dentro del contenedor; sus dependencias se resuelven desde el `~/.m2` montado.