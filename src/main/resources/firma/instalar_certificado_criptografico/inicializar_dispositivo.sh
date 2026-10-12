#!/bin/bash

# Función para mostrar ayuda
mostrar_ayuda() {
    echo "Uso: $0 PIN PUK [LABEL]"
    echo "  LABEL: etiqueta del dispositivo (por defecto: EducaFlow)"
}

# Comprobar si el usuario pide ayuda
if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    mostrar_ayuda
    exit 0
fi

# Comprobar que haya 2 o 3 parámetros
if [[ $# -lt 2 || $# -gt 3 ]]; then
    echo "Error: Se requieren 2 o 3 parámetros."
    mostrar_ayuda
    exit 1
fi

PIN=$1
PUK=$2
LABEL=${3:-EducaFlow}

pkcs15-init -E
pkcs15-init --create-pkcs15  -p pkcs15+onepin --pin "$PIN" --puk "$PUK" --label "$LABEL"

