#!/usr/bin/env bash
#
# Levanta el sandbox con docker compose, puentea los MCP que el host expone en
# 127.0.0.1 (p.ej. el de IntelliJ) para que sean alcanzables desde dentro del
# contenedor, entra en Claude Code y, al salir, deshace TODO (socat + reglas).
#
# El puente es un DOBLE socat por puerto, de modo que dentro del contenedor el MCP
# siga siendo accesible en el MISMO 127.0.0.1:PUERTO (sin tocar la config montada):
#
#   [contenedor] claude -> 127.0.0.1:PUERTO
#                          └─ socat (contenedor) -> GATEWAY:PUERTO
#                                                   └─ socat (host, bind=GATEWAY) -> 127.0.0.1:PUERTO -> MCP real
#
# GATEWAY = la IP de la gateway de la red del contenedor (el host visto desde la red
# del contenedor). El host de este equipo tiene una política INPUT restrictiva que
# descarta el tráfico contenedor->host, así que el script añade (con sudo) una regla
# iptables ACCEPT acotada a GATEWAY:PUERTO mientras dura la sesión y la elimina al salir.
#
# Uso:
#   ./claude-sandbox.sh             BD vacía (resetea el volumen postgres_data en cada arranque)
#   ./claude-sandbox.sh --keep-db   conserva la BD entre sesiones (no la resetea)
#   ./claude-sandbox.sh -h|--help   esta ayuda
#
set -uo pipefail
cd "$(dirname "$0")"

CONTAINER="claude-sandbox"
HOST_SOCAT_PIDS=()
IPTABLES_RULES=()   # cada entrada: "GW PORT" de una regla INPUT que añadimos
HAVE_SUDO=0
KEEP_DB=0           # 0 = resetear BD a vacía (por defecto); 1 = conservarla (--keep-db)

usage() {
  sed -n 's/^# \{0,1\}//p' "$0" | sed -n '/^Uso:/,/--help/p'
  exit 0
}

# ── Argumentos ────────────────────────────────────────────────────────────────
for arg in "$@"; do
  case "$arg" in
    --keep-db)   KEEP_DB=1 ;;
    -h|--help)   usage ;;
    *) echo "Opción desconocida: $arg (usa -h para la ayuda)" >&2; exit 2 ;;
  esac
done

log() { echo "==> [claude-sandbox.sh] $*"; }

discover_mcp_ports() {
  {
    [ -f "$HOME/.claude.json" ] && jq -r '.mcpServers // {} | .[] | .url // empty' "$HOME/.claude.json"
    [ -f .mcp.json ]            && jq -r '.mcpServers // {} | .[] | .url // empty' .mcp.json
    [ -f ../.mcp.json ]         && jq -r '.mcpServers // {} | .[] | .url // empty' ../.mcp.json
  } 2>/dev/null \
    | grep -oE '://(127\.0\.0\.1|localhost):[0-9]+' \
    | grep -oE '[0-9]+$' \
    | sort -u
}

cleanup() {
  log "Limpiando puentes..."
  for pid in "${HOST_SOCAT_PIDS[@]:-}"; do
    [ -n "${pid:-}" ] && kill "$pid" 2>/dev/null || true
  done
  docker exec "$CONTAINER" pkill -f 'socat TCP-LISTEN' 2>/dev/null || true
  if [ "$HAVE_SUDO" = "1" ]; then
    for rule in "${IPTABLES_RULES[@]:-}"; do
      [ -z "${rule:-}" ] && continue
      set -- $rule; gw="$1"; port="$2"
      sudo iptables -D INPUT -p tcp -d "$gw" --dport "$port" -j ACCEPT 2>/dev/null || true
    done
  fi
  log "Hecho. Los contenedores siguen arriba (pára con: docker compose down)."
}
trap cleanup EXIT INT TERM

# ── 1. Levantar los contenedores ──────────────────────────────────────────────
# Por defecto la BD arranca vacía: down -v borra el volumen postgres_data. Con --keep-db
# se conserva. En ambos casos el código, ~/.m2 y ~/.claude (bind-mounts) NO se tocan.
if [ "$KEEP_DB" = "1" ]; then
  log "Conservando la base de datos (--keep-db)."
else
  log "Reseteando la base de datos a vacía (docker compose down -v)..."
  docker compose down -v 2>/dev/null || true
fi
# --build siempre: con la caché de capas es casi instantáneo si nada cambió, y recoge
# automáticamente cambios en Dockerfile/entrypoint.sh (que van horneados en la imagen).
log "Levantando contenedores (docker compose up -d --build)..."
docker compose up -d --build || { log "ERROR: docker compose up falló."; exit 1; }

# ── 2. Puentear cada puerto MCP ───────────────────────────────────────────────
mapfile -t PORTS < <(discover_mcp_ports)

if [ "${#PORTS[@]}" -eq 0 ]; then
  log "No hay MCP en 127.0.0.1 que puentear."
else
  GW="$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.Gateway}}{{end}}' "$CONTAINER" 2>/dev/null)"
  if [ -z "${GW:-}" ]; then
    log "AVISO: no obtengo la gateway de la red del contenedor; me salto los puentes."
  else
    log "Gateway de la red del contenedor = $GW. Puertos MCP: ${PORTS[*]}"
    # Necesitamos sudo para la regla iptables (firewall del host bloquea contenedor->host).
    if sudo -v 2>/dev/null; then HAVE_SUDO=1; else
      log "AVISO: sin sudo no puedo abrir el firewall; el puente puede no funcionar."
    fi
    for port in "${PORTS[@]}"; do
      if [ "$HAVE_SUDO" = "1" ]; then
        sudo iptables -I INPUT -p tcp -d "$GW" --dport "$port" -j ACCEPT 2>/dev/null \
          && IPTABLES_RULES+=("$GW $port")
      fi
      # socat del HOST: GATEWAY:PUERTO -> 127.0.0.1:PUERTO (el MCP real)
      socat "TCP-LISTEN:${port},bind=${GW},fork,reuseaddr" "TCP:127.0.0.1:${port}" 2>/dev/null &
      HOST_SOCAT_PIDS+=("$!")
      # socat del CONTENEDOR: 127.0.0.1:PUERTO -> GATEWAY:PUERTO
      docker exec -d "$CONTAINER" \
        socat "TCP-LISTEN:${port},bind=127.0.0.1,fork,reuseaddr" "TCP:${GW}:${port}"
      # autotest: ¿el contenedor llega al MCP por 127.0.0.1:PUERTO?
      sleep 0.5
      code="$(docker exec "$CONTAINER" bash -lc "curl -s -o /dev/null -w '%{http_code}' --max-time 4 http://127.0.0.1:${port}/ 2>/dev/null" 2>/dev/null)"
      if [ "${code:-000}" != "000" ]; then
        log "  puerto ${port}: OK (el contenedor alcanza el MCP, HTTP ${code})."
      else
        log "  puerto ${port}: FALLO (no se alcanza). ¿Firewall? Regla manual:"
        log "      sudo iptables -I INPUT -p tcp -d ${GW} --dport ${port} -j ACCEPT"
      fi
    done
  fi
fi

# ── 3. Entrar en Claude Code ──────────────────────────────────────────────────
# El managed-settings de la imagen ya arranca en modo bypass (sin prompts).
# --append-system-prompt: nota persistente para toda la sesión (no es un mensaje de
# usuario, así que no provoca ninguna acción) avisando de cómo acceder a la BD.
DB_HINT='Para acceder a la base de datos PostgreSQL desde la shell, usa el comando "psql" (ya está configurado con las variables PGHOST=educaflow-db, PGUSER/PGPASSWORD/PGDATABASE=educaflow, así que "psql" a secas conecta). También es accesible en localhost:5432 y en educaflow-db:5432.'
log "Entrando en Claude Code..."
docker compose exec -it "$CONTAINER" claude --append-system-prompt "$DB_HINT" || true

# Al salir de claude, el trap EXIT ejecuta cleanup().
