# CLAUDE.md — claude-sandbox

Entorno Docker para ejecutar **Claude Code** de forma aislada con todo el tooling para
**compilar y ejecutar** la secretaría virtual y correr **Playwright**, más una BD
PostgreSQL privada. Para uso humano detallado ver [`README.md`](README.md); este fichero
recoge lo que un agente necesita saber para trabajar aquí sin romper nada.

## Qué levanta

`docker-compose.yml` define **2 servicios** en la red interna `backend`:

- `db` — `postgres:12.22` (misma versión que el devops), hostname `educaflow-db`, **sin
  puertos publicados** (solo accesible desde la red interna). Datos en el volumen
  `postgres_data`.
- `claude-sandbox` — imagen construida del `Dockerfile`. Trae Java 21 (Temurin), Node 24
  + corepack/pnpm, Maven, Claude Code, Playwright + Chromium, y el tooling del devops
  (git, curl, gnupg, graphviz, apertium, apertium-es-ca). Usuario `developer` con
  uid/gid del host (args `HOST_UID`/`HOST_GID`, default 1000). Publica **`8081→8080`**.

## Decisiones de diseño (NO deshacer sin querer)

1. **No se compila Axelor dentro del contenedor.** Se monta el **`~/.m2` del host** como
   bind-mount en `/home/developer/.m2`, reutilizando los jars ya compilados
   (`com/axelor/*`). El `entrypoint.sh` solo verifica que están; no ejecuta ningún
   `install.sh`. Si faltan jars, se compilan **en el host** (los `install.sh` de
   axelor-ui / axelor-open-platform / EducaFlowBuildTools), no aquí.

2. **La BD se apunta al contenedor SIN tocar el código montado.** El
   `axelor-config.properties` del repo dice `localhost:5432`, pero el servicio
   `claude-sandbox` define `AXELOR_CONFIG_DB_DEFAULT_URL=jdbc:postgresql://educaflow-db:5432/educaflow`
   (+ `_USER`/`_PASSWORD`). Axelor aplica las variables con prefijo `AXELOR_CONFIG_`
   (`EnvSettingSource`) **por encima** del fichero de propiedades, así que en runtime
   gana `educaflow-db`. **No edites `axelor-config.properties` para esto.**

3. **Puerto host = 8081, no 8080.** El 8080 del host está ocupado por otro proceso
   `java`; por eso se publica `8081→8080`. Dentro del contenedor la app sigue en 8080,
   así que **Playwright interno usa `localhost:8080`** (sin cambios); solo el host ve
   8081 (`http://localhost:8081`).

4. **Repos montados por separado**, no un padre común: `../../secretaria-virtual`,
   `../../axelor-ui`, `../../axelor-open-platform`, `../../EducaFlowBuildTools`,
   `../../secretaria-virtual-private` → `/workspace/<repo>`. Que `secretaria-virtual`
   contenga su propia subcarpeta `claude-sandbox/` al montarse es inofensivo (los
   bind-mounts no recursan).

5. **Auth de Claude Code** = bind-mount de `~/.claude` y `~/.claude.json` del host →
   reutiliza la sesión del host.

6. **`claude` arranca sin restricciones de permisos y SIN ningún prompt.** El aislamiento
   lo da el contenedor (es el motivo de usarlo). En vez del flag
   `--dangerously-skip-permissions` (que fuerza la pantalla de aceptación "Bypass
   Permissions mode… Yes, I accept"), se fija en un **managed-settings**
   (`/etc/claude-code/managed-settings.json`, máxima precedencia, fuera del repo y del
   `~/.claude` montados):
   `{ "permissions": { "defaultMode": "bypassPermissions" }, "sandbox": { "enabled": false } }`.
   Así `claude` a secas ya permite todo sin pantallas. `sandbox.enabled=false` apaga el
   sandbox de comandos propio de Claude Code (el aislamiento ya lo da Docker; si se deja,
   avisa de que falta bubblewrap). **No** pasar `--dangerously-skip-permissions` (reintroduce
   el prompt). Verificado en headless: ejecuta herramientas sin prompt ni warning.

## `claude-sandbox.sh` — entrar con los MCP del host puenteados

`./claude-sandbox.sh` es la vía recomendada para entrar: **resetea la BD a vacía**
(`docker compose down -v` borra el volumen `postgres_data`), levanta los contenedores,
**puentea los MCP que el host expone en `127.0.0.1`** (los descubre de `~/.claude.json`
y de los `.mcp.json`), entra en Claude y **deshace los puentes al salir**.

> **La BD arranca vacía por defecto**: cada ejecución hace `down -v`, así que no persiste
> nada entre sesiones. La primera `./run.sh` dentro del contenedor recrea el esquema
> (`ddl=update`) y carga `data-init`. El `down -v` solo borra `postgres_data`; código,
> `~/.m2` y `~/.claude` son bind-mounts y no se tocan.
>
> Flag **`--keep-db`** para conservar la BD entre sesiones: `./claude-sandbox.sh --keep-db`.

El puente es un **doble socat** por puerto, para que dentro del contenedor el MCP siga
en el **mismo `127.0.0.1:PUERTO`** (sin tocar la config montada):

```
[contenedor] claude → 127.0.0.1:PUERTO
                      └ socat (contenedor) → GATEWAY:PUERTO
                                             └ socat (host, bind=GATEWAY) → 127.0.0.1:PUERTO → MCP real
```

`GATEWAY` = gateway de la red del contenedor (`docker inspect`). En este host la política
`INPUT` del firewall **descarta** el tráfico contenedor→host, así que el script añade con
`sudo` una regla `iptables -I INPUT -p tcp -d GATEWAY --dport PUERTO -j ACCEPT` (acotada a
esa IP+puerto) mientras dura la sesión y la borra al salir. Por eso pide `sudo` una vez.
Requisitos: `socat` (host e imagen), `jq` (host), `extra_hosts` en el compose, y la regla
de firewall. Si no hay `sudo`, el script avisa y el puente puede no funcionar.

> Nota: el plugin de IntelliJ escucha en `127.0.0.1` del host; un bridge sin este doble
> socat + regla no lo alcanza (loopback del contenedor ≠ loopback del host).

## Cómo se usa

```bash
./claude-sandbox.sh                               # RECOMENDADO: levanta + puentea MCP + entra
docker compose up -d --build                      # construir/levantar (arranque instantáneo)
docker compose exec -it claude-sandbox claude     # entrar sin puentear MCP
docker compose exec -it claude-sandbox bash       # shell; dentro: ./run.sh → http://localhost:8081
docker compose logs -f claude-sandbox             # logs
docker compose down                               # parar (conserva la BD)
docker compose down -v                            # parar y BORRAR la BD (NO borra ~/.m2: es bind-mount)
```

El working dir del contenedor es `/workspace/secretaria-virtual`. Compilar/arrancar la app
es lo de siempre: `./run.sh` (que hace `./gradlew clean build` + `run` en el 8080 interno).

## Avisos para el agente

- **Reconstruir la imagen** (`up -d --build`) tras tocar `Dockerfile` o `entrypoint.sh`:
  ambos quedan horneados en la imagen.
- Los comandos `docker` necesitan el **socket de Docker**; bajo el sandbox de comandos de
  Claude Code fallan con `permission denied ... docker.sock`. Hay que ejecutarlos con el
  sandbox desactivado.
- Compilar la **propia** secretaría virtual (no Axelor) sí ocurre dentro del contenedor y
  resuelve dependencias desde el `~/.m2` montado.
- `Dockerfile`/`docker-compose.yml` aquí son del **sandbox de desarrollo**; no confundir
  con los de `../../secretaria-virtual-devops/`, que son para despliegue (clonan de GitHub
  y compilan todo en la imagen).
