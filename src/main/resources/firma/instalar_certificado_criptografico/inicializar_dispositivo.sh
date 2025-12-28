#!/bin/bash

# Función para mostrar ayuda
mostrar_ayuda() {
    echo "Uso: $0 PIN PUK"
}

# Comprobar si el usuario pide ayuda
if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    mostrar_ayuda
    exit 0
fi

# Comprobar que haya exactamente 4 parámetros
if [[ $# -ne 2 ]]; then
    echo "Error: Se requieren exactamente 2 parámetros."
    mostrar_ayuda
    exit 1
fi

PIN=$1
PUK=$2

pkcs15-init -E
pkcs15-init --create-pkcs15  -p pkcs15+onepin --pin "$PIN" --puk "$PUK" --label "EducaFlow"

