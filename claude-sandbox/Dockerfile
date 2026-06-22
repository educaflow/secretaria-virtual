# syntax=docker/dockerfile:1.4
#
# Imagen "claude-sandbox": un entorno aislado con Claude Code + todo lo necesario
# para COMPILAR y EJECUTAR la secretaría virtual + Playwright.
#
# El código (secretaria-virtual y sus 3 dependencias Axelor) NO se clona aquí:
# se monta desde el host como volúmenes en /workspace/* (ver docker-compose.yml).
# Por eso esta imagen solo trae el *tooling*; las dependencias Axelor se compilan
# e instalan en ~/.m2 en el primer arranque (ver entrypoint.sh).
FROM eclipse-temurin:21-jdk-jammy

# UID/GID del usuario del host: el contenedor crea un usuario "developer" con esos
# mismos ids para que los ficheros montados (código, ~/.claude) no den problemas
# de permisos.
ARG HOST_UID=1000
ARG HOST_GID=1000

# ── Zona horaria (antes que nada que use la hora) ─────────────────────────────
ENV TZ=Europe/Madrid
RUN apt-get update && apt-get install -y tzdata \
    && ln -snf /usr/share/zoneinfo/Europe/Madrid /etc/localtime \
    && echo "Europe/Madrid" > /etc/timezone \
    && dpkg-reconfigure -f noninteractive tzdata \
    && rm -rf /var/lib/apt/lists/*

# ── Herramientas del sistema ──────────────────────────────────────────────────
# Las mismas que usa el devops (git, maven, curl, gnupg, graphviz, apertium +
# apertium-es-ca) más utilidades cómodas dentro del sandbox (sudo, psql, less...).
RUN apt-get update && apt-get install -y \
    git \
    maven \
    curl \
    gnupg \
    graphviz \
    apertium \
    apertium-es-ca \
    ca-certificates \
    sudo \
    bash \
    less \
    procps \
    postgresql-client \
    socat \
    && rm -rf /var/lib/apt/lists/*

# ── Node.js 24 ────────────────────────────────────────────────────────────────
# Ambos package.json exigen node >= 24.13.0; axelor-front descarga node+pnpm
# internamente, pero el resto del tooling necesita node/npm/npx del sistema.
RUN curl -fsSL https://deb.nodesource.com/setup_24.x | bash - \
    && apt-get install -y nodejs \
    && rm -rf /var/lib/apt/lists/*

# corepack gestiona pnpm@10.x (definido en "packageManager" de los package.json).
# NO instalar pnpm con npm para no chocar con corepack.
RUN corepack enable

# ── Claude Code ───────────────────────────────────────────────────────────────
RUN npm install -g @anthropic-ai/claude-code

# Wrapper: dentro del sandbox Claude debe ir SIN restricciones de permisos (el
# aislamiento lo da el contenedor, que es justo para lo que se usa). /usr/local/bin
# va antes que /usr/bin en el PATH, así que este wrapper sombrea al binario real
# (/usr/bin/claude) y le añade --dangerously-skip-permissions. Corremos como usuario
# no-root "developer", por lo que ese flag está permitido.
# Para invocar Claude SIN el flag (excepción puntual): usar /usr/bin/claude directamente.
RUN printf '%s\n' '#!/usr/bin/env bash' 'exec /usr/bin/claude --dangerously-skip-permissions "$@"' \
      > /usr/local/bin/claude \
    && chmod +x /usr/local/bin/claude

# ── Playwright + navegador (chromium) con sus libs de sistema ─────────────────
# Los navegadores se instalan en una ruta compartida y legible por todos, de modo
# que el "npx playwright" del proyecto los encuentre vía PLAYWRIGHT_BROWSERS_PATH.
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
RUN npx --yes playwright@latest install --with-deps chromium \
    && chmod -R a+rX /ms-playwright

# ── Variables para los builds intensivos en memoria ──────────────────────────
ENV NODE_OPTIONS=--max-old-space-size=6096
ENV GRADLE_OPTS="-Xmx4g"

# ── Usuario no-root con el uid/gid del host ──────────────────────────────────
RUN groupadd -g ${HOST_GID} developer 2>/dev/null || true \
    && useradd -m -u ${HOST_UID} -g ${HOST_GID} -s /bin/bash developer \
    && echo "developer ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/developer \
    && chmod 0440 /etc/sudoers.d/developer \
    && mkdir -p /workspace /home/developer/.m2 \
    && chown -R developer:developer /workspace /home/developer

COPY --chown=developer:developer entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

USER developer
WORKDIR /workspace/secretaria-virtual

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
# El contenedor se mantiene vivo; trabajas dentro con:
#   docker compose exec -it claude-sandbox claude
CMD ["sleep", "infinity"]