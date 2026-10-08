#!/bin/bash
#if [ -n "$HOME" ]; then
#  rm -rf ${HOME}/.axelor/attachments/
#fi
#./gradlew --stop

# Dos modos:
#  - Sin `ports.env` (modo por defecto): exactamente las dos llamadas a gradlew
#    de siempre, puertos 8080/5432, sin tocar Docker ni sobrescribir ninguna
#    propiedad. Es el modo que usa producción (../secretaria-virtual-devops).
#  - Con `ports.env` (modo worktree): cada worktree tiene su par de puertos fijo
#    (APP_PORT/DB_PORT) y su propio contenedor de BD `educaflow-db-<DB_PORT>`.
#    `./run.sh --new` crea el fichero buscando puertos libres; `--reset-db`
#    vacía la BD. Detalle en agent_docs/deploy.md.

set -e
clear
#./gradlew clean build --info --refresh-dependencies

PG_IMAGE="postgres:12.22"
PG_USER="educaflow"
PG_PASSWORD="educaflow"
PG_DB="educaflow"
LABEL_KEY="educaflow.worktree"

NEW=0
RESET_DB=0
for arg in "$@"; do
  case "$arg" in
    --new) NEW=1 ;;
    --reset-db) RESET_DB=1 ;;
    *) echo "Argumento desconocido: $arg (admitidos: --new, --reset-db)" >&2; exit 2 ;;
  esac
done

WT="$(git rev-parse --show-toplevel)"
PORTS_FILE="$WT/ports.env"

# Lista los tests que han fallado leyendo los XML de resultados (la fuente de
# verdad). Cada <testcase> que contiene <failure>/<error> se imprime como
# "  - paquete.Clase > metodo".
print_failed_tests() {
  local dir="build/test-results/test"
  if ! ls "$dir"/*.xml >/dev/null 2>&1; then
    echo "  (no hay resultados de tests: el build falló antes de ejecutarlos)"
    return
  fi
  awk '
    match($0, /<testcase /) {
      name=$0; sub(/.* name="/,"",name); sub(/".*/,"",name)
      cls=$0;  sub(/.*classname="/,"",cls); sub(/".*/,"",cls)
      open=1
      if ($0 ~ /\/>/) { open=0; next }                 # testcase OK (auto-cerrado)
      if ($0 ~ /<failure|<error/) { print "  - " cls " > " name; open=0 }
      next
    }
    open && /<failure|<error/ { print "  - " cls " > " name; open=0 }
    open && /<\/testcase>/    { open=0 }
  ' "$dir"/*.xml | sort -u
}

# Compila y ejecuta los tests. Sin --info para que los fallos de test no
# queden enterrados en el ruido. Si el build falla, mostramos qué tests han
# fallado y NO arrancamos la aplicación.
build() {
  set +e
  ./gradlew clean build
  BUILD_EXIT=$?
  set -e

  if [ "$BUILD_EXIT" -ne 0 ]; then
    echo
    echo "=================================================="
    echo "  BUILD FALLIDO — la aplicación NO se arranca."
    echo "  Tests fallidos:"
    echo "=================================================="
    print_failed_tests
    echo
    echo "  Informe HTML completo: build/reports/tests/test/index.html"
    exit "$BUILD_EXIT"
  fi
}

# ---------------------------------------------------------------------------
# Modo por defecto (sin ports.env): como siempre.
# ---------------------------------------------------------------------------
if [ "$NEW" -eq 0 ] && [ ! -f "$PORTS_FILE" ]; then
  if [ "$RESET_DB" -eq 1 ]; then
    echo "--reset-db solo aplica al modo worktree (con ports.env): este worktree no tiene ports.env." >&2
    exit 2
  fi
  WORKTREES=$(git worktree list --porcelain | grep -c '^worktree ' || true)
  if [ "$WORKTREES" -gt 1 ]; then
    echo "Este repositorio tiene varios worktrees y este no tiene ports.env: ejecuta ./run.sh --new" >&2
    echo "Worktrees:" >&2
    git worktree list >&2
    echo "Si alguno de los worktrees listados ya no existe, ejecuta git worktree prune" >&2
    exit 2
  fi

  build

  # ./gradlew clean test --info
  #./gradlew --no-daemon run --debug-jvm --port 8080 --context-path /
  ./gradlew --no-daemon run --port 8080 --context-path /  --config ../secretaria-virtual-private/axelor-config.dev.properties
  exit $?
fi

# ---------------------------------------------------------------------------
# Modo worktree.
# ---------------------------------------------------------------------------

# ¿Hay algo escuchando en el puerto (IPv4 o IPv6)?
port_listening() {
  ss -Hltn | awk '{print $4}' | grep -qE "[:.]$1\$"
}

# Describe qué ocupa un puerto: el contenedor Docker que lo publica, o el
# PID y comando si `ss` los puede ver (solo muestra procesos del propio usuario).
describe_port() {
  local port="$1" c
  c=$(docker ps --filter "publish=$port" --format '{{.Names}}' 2>/dev/null | head -1)
  if [ -n "$c" ]; then
    echo "el contenedor Docker '$c'"
    return
  fi
  local proc
  proc=$(ss -Hltnp | awk -v p="$port" '$4 ~ ("[:.]" p "$") && $NF ~ /^users:/ {print $NF}' | head -1)
  if [ -n "$proc" ]; then
    # users:(("java",pid=1234,fd=5)) -> java (PID 1234)
    echo "el proceso $(echo "$proc" | sed -E 's/^users:\(\("([^"]+)",pid=([0-9]+).*/\1 (PID \2)/')"
  else
    echo "un proceso de otro usuario (no visible sin root)"
  fi
}

port_busy_error() {
  echo "ERROR: el puerto $1 ($2) está ocupado por $(describe_port "$1")." >&2
  echo "Para usar otros puertos: borra ports.env y ejecuta ./run.sh --new" >&2
  exit 1
}

# Recogida de contenedores huérfanos: contenedores con la etiqueta cuyo
# worktree ya no existe como directorio. Solo se mira la etiqueta y solo se
# borra por ausencia del directorio.
collect_orphans() {
  docker ps -a --filter "label=$LABEL_KEY" --format "{{.Names}}\t{{.Label \"$LABEL_KEY\"}}" \
    | while IFS=$'\t' read -r name path; do
        if [ -n "$path" ] && [ ! -d "$path" ]; then
          docker rm -f "$name" >/dev/null
          echo "Borrado contenedor huérfano $name (worktree $path ya no existe)"
        fi
      done
}

# Puertos ya asignados en el ports.env de los demás worktrees (aunque estén parados).
ports_of_other_worktrees() {
  git worktree list --porcelain | awk '/^worktree /{sub(/^worktree /,""); print}' \
    | while read -r dir; do
        [ "$dir" = "$WT" ] && continue
        [ -f "$dir/ports.env" ] || continue
        ( set +e; . "$dir/ports.env"; echo "$APP_PORT"; echo "$DB_PORT" )
      done | grep -E '^[0-9]+$' || true
}

find_free_port() {
  local port="$1" reserved="$2"
  while port_listening "$port" || echo "$reserved" | grep -qx "$port"; do
    port=$((port + 1))
  done
  echo "$port"
}

collect_orphans

if [ "$NEW" -eq 1 ]; then
  RESERVED=$(ports_of_other_worktrees)
  APP_PORT=$(find_free_port 8080 "$RESERVED")
  DB_PORT=$(find_free_port 5432 "$RESERVED")
  printf 'APP_PORT=%s\nDB_PORT=%s\n' "$APP_PORT" "$DB_PORT" > "$PORTS_FILE"
  echo "Escrito $PORTS_FILE (APP_PORT=$APP_PORT, DB_PORT=$DB_PORT)"
fi

. "$PORTS_FILE"
if ! [[ "$APP_PORT" =~ ^[0-9]+$ && "$DB_PORT" =~ ^[0-9]+$ ]]; then
  echo "ERROR: $PORTS_FILE debe definir APP_PORT y DB_PORT numéricos. Bórralo y ejecuta ./run.sh --new" >&2
  exit 1
fi

CONTAINER="educaflow-db-$DB_PORT"

if [ "$RESET_DB" -eq 1 ] && [ -n "$(docker ps -aq --filter "name=^${CONTAINER}$")" ]; then
  docker rm -f "$CONTAINER" >/dev/null
  echo "Borrado contenedor $CONTAINER (--reset-db): la BD empieza vacía"
fi

if [ -n "$(docker ps -aq --filter "name=^${CONTAINER}$")" ]; then
  OWNER=$(docker inspect -f "{{index .Config.Labels \"$LABEL_KEY\"}}" "$CONTAINER")
  if [ "$OWNER" != "$WT" ]; then
    echo "ERROR: el contenedor $CONTAINER ya existe y pertenece a otro worktree ('${OWNER:-sin etiqueta}')." >&2
    echo "Para usar otros puertos: borra ports.env y ejecuta ./run.sh --new" >&2
    exit 1
  fi
  if [ -z "$(docker ps -q --filter "name=^${CONTAINER}$")" ]; then
    if port_listening "$DB_PORT"; then port_busy_error "$DB_PORT" "BD"; fi
    docker start "$CONTAINER" >/dev/null
    echo "Arrancado contenedor $CONTAINER (datos conservados)"
  fi
else
  if port_listening "$DB_PORT"; then port_busy_error "$DB_PORT" "BD"; fi
  docker run -d --name "$CONTAINER" --label "$LABEL_KEY=$WT" \
    -e POSTGRES_USER="$PG_USER" -e POSTGRES_PASSWORD="$PG_PASSWORD" -e POSTGRES_DB="$PG_DB" \
    -p "$DB_PORT:5432" "$PG_IMAGE" >/dev/null
  echo "Creado contenedor $CONTAINER"
fi

for i in $(seq 1 60); do
  if docker exec "$CONTAINER" pg_isready -U "$PG_USER" -d "$PG_DB" >/dev/null 2>&1; then break; fi
  if [ "$i" -eq 60 ]; then
    echo "ERROR: la BD del contenedor $CONTAINER no responde tras 60 s. Logs: docker logs $CONTAINER" >&2
    exit 1
  fi
  sleep 1
done

if port_listening "$APP_PORT"; then port_busy_error "$APP_PORT" "app"; fi

# Sobrescribe solo en este modo las propiedades que apuntan a recursos compartidos:
# la BD y las carpetas de adjuntos, temporales y logs (en axelor-config.properties
# están bajo /opt/secretariavirtual/data, comunes a todos los worktrees).
# AOP lee las variables AXELOR_CONFIG_* por encima de axelor-config.properties y
# del --config. `data.upload.temp-dir` lleva guion y no se puede expresar como
# variable (AOP convierte `_` en `.`), así que va como propiedad de sistema
# -Daxelor.config.*, que la JVM de Tomcat recoge de JAVA_TOOL_OPTIONS (solo en el run,
# no en el build).
DATA_DIR="$WT/.axelor-data"
mkdir -p "$DATA_DIR/attachments" "$DATA_DIR/temp" "$DATA_DIR/logs"
export AXELOR_CONFIG_DB_DEFAULT_URL="jdbc:postgresql://localhost:$DB_PORT/$PG_DB"
export AXELOR_CONFIG_DATA_UPLOAD_DIR="$DATA_DIR/attachments"
export AXELOR_CONFIG_LOGGING_PATH="$DATA_DIR/logs"

echo "=================================================="
echo "  Modo worktree: $WT"
echo "  App:   http://localhost:$APP_PORT"
echo "  BD:    localhost:$DB_PORT (contenedor $CONTAINER)"
echo "  Datos: $DATA_DIR (adjuntos, temporales y logs)"
echo "=================================================="

build

JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:+$JAVA_TOOL_OPTIONS }-Daxelor.config.data.upload.temp-dir=$DATA_DIR/temp" \
./gradlew --no-daemon run --port "$APP_PORT" --context-path /  --config ../secretaria-virtual-private/axelor-config.dev.properties
