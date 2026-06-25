#!/usr/bin/env bash
#
# Entrypoint del contenedor claude-sandbox.
#
# NO compila las dependencias Axelor: se reutiliza el ~/.m2 del host (montado como
# volumen), donde ya están los jars compilados (axelor-ui, axelor-open-platform,
# EducaFlowBuildTools, etc.). Aquí solo se hace una comprobación informativa.
#
set -euo pipefail

log() { echo "==> [claude-sandbox] $*"; }

if [ -d "${HOME}/.m2/repository/com/axelor" ]; then
  log "Maven del host montado: jars de Axelor presentes en ~/.m2 (no se compila nada)."
else
  log "AVISO: no encuentro jars de Axelor en ~/.m2 (~/.m2/repository/com/axelor)."
  log "       Asegúrate de haber compilado las dependencias en el host (sus install.sh)"
  log "       o de que el volumen \${HOME}/.m2 está bien montado en el docker-compose.yml."
fi

# ── BD accesible también por localhost ────────────────────────────────────────
# El servicio postgres es "educaflow-db" en la red interna, pero axelor-config.properties
# y la mayoría de herramientas asumen localhost:5432. Reenviamos 127.0.0.1:5432 ->
# educaflow-db:5432 con socat para que la BD sea accesible por ambos nombres desde la
# shell del contenedor (psql -h localhost, JDBC localhost, etc.).
if command -v socat >/dev/null 2>&1; then
  socat TCP-LISTEN:5432,bind=127.0.0.1,fork,reuseaddr TCP:educaflow-db:5432 \
    >/dev/null 2>&1 &
  log "Reenvío activo: localhost:5432 -> educaflow-db:5432 (BD accesible por localhost)."
fi

log "Listo. Entra al sandbox con:  docker compose exec -it claude-sandbox claude"
exec "$@"
