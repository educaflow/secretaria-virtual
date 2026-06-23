#!/bin/bash
#if [ -n "$HOME" ]; then
#  rm -rf ${HOME}/.axelor/attachments/
#fi
#./gradlew --stop

set -e
clear
#docker stop educaflow-db
#docker run --name educaflow-db --hostname educaflow-db  -e POSTGRES_USER=educaflow -e POSTGRES_PASSWORD=educaflow -e POSTGRES_DB=educaflow -p 5432:5432 -d --rm postgres:12.22
#./gradlew clean build --info --refresh-dependencies

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

# ./gradlew clean test --info
#./gradlew --no-daemon run --debug-jvm --port 8080 --context-path /
./gradlew --no-daemon run --port 8080 --context-path /  --config ../secretaria-virtual-private/axelor-config.dev.properties
