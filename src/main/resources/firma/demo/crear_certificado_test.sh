#!/bin/bash
# Emite con la CA FALSA de demo un certificado de test, que no es de ningún usuario de demo,
# en src/test/resources/firma/test/<nombre>.p12 (fuera de src/main: no va dentro del WAR).

DIR_SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=comun_demo.sh
source "${DIR_SCRIPT}/comun_demo.sh"

DIR_CERTIFICADOS_TEST="$(cd "${DIR_SCRIPT}/../../../.." && pwd)/test/resources/firma/test"
README_TEST="${DIR_CERTIFICADOS_TEST}/README.txt"
APELLIDOS_TEST="Certificado Test"

# Función para mostrar ayuda
mostrar_ayuda() {
    echo "Uso: $0 nombre"
    echo
    echo "Parámetros:"
    echo "  nombre   Nombre del certificado (test1, test2...). Es también el nombre del titular y del fichero"
    echo
    echo "Crea ${DIR_CERTIFICADOS_TEST}/<nombre>.p12 (contraseña ${PASSWORD_P12_DEMO}) con un DNI de demo nuevo"
    echo "que no es de ningún usuario de demo, y lo apunta en ${README_TEST}."
}

# Comprobar si el usuario pide ayuda
if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    mostrar_ayuda
    exit 0
fi

# Comprobar que haya exactamente 1 parámetro
if [[ $# -ne 1 ]]; then
    echo "Error: Se requiere exactamente 1 parámetro."
    mostrar_ayuda
    exit 1
fi

NOMBRE=$1

if ! [[ "$NOMBRE" =~ ^[A-Za-z0-9_-]+$ ]]; then
    echo "Error: El nombre solo puede tener letras, números, '_' y '-': ${NOMBRE}"
    exit 1
fi

FICHERO_P12="${DIR_CERTIFICADOS_TEST}/${NOMBRE}.p12"

if [[ -e "$FICHERO_P12" ]]; then
    echo "Error: Ya existe el fichero ${FICHERO_P12}. No se sobrescribe: bórralo a mano (y su línea de ${README_TEST}) si quieres regenerarlo"
    exit 1
fi

DNIS_OCUPADOS=$(dnis_usuarios_demo) || exit 1
if [[ -f "$README_TEST" ]]; then
    DNIS_OCUPADOS+=$'\n'$(grep -oE '[0-9]{8}[A-Z]' "$README_TEST")
fi

# Un DNI de demo que no sea de ningún usuario ni de otro certificado de test
DNI=""
for _ in {1..100}; do
    CANDIDATO=$("$GENERAR_DNI_DEMO") || exit 1
    if ! grep -qxF "$CANDIDATO" <<< "$DNIS_OCUPADOS"; then
        DNI=$CANDIDATO
        break
    fi
done
if [[ -z "$DNI" ]]; then
    echo "Error: No se ha podido generar un DNI que no esté ya en uso"
    exit 1
fi

emitir_certificado "$DNI" "$NOMBRE" "$APELLIDOS_TEST" "$FICHERO_P12" || exit 1

echo "${NOMBRE}.p12  DNI=${DNI}  titular=${NOMBRE} ${APELLIDOS_TEST}" >> "$README_TEST"

echo "Certificado de test creado"
echo "  Fichero: ${FICHERO_P12}"
echo "  DNI:     ${DNI}"
