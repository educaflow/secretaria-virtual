#!/bin/bash

# Función para mostrar ayuda
mostrar_ayuda() {
    echo "Uso: $0 ID SUFIJO_ALIAS PIN CERTIFICADO"
    echo
    echo "Parámetros:"
    echo "  ID             Identificador del certificado, debe ser único. Empezar con 01"
    echo "  SUFIJO_ALIAS   Sufijo del Alias para el certificado. Es el nombre de la persona del certificado"
    echo "  PIN            PIN del dispositivo que se usa al crearse."
    echo "  CERTIFICADO    Archivo .p12 del certificado"
}

# Comprobar si el usuario pide ayuda
if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    mostrar_ayuda
    exit 0
fi

# Comprobar que haya exactamente 4 parámetros
if [[ $# -ne 4 ]]; then
    echo "Error: Se requieren exactamente 4 parámetros."
    mostrar_ayuda
    exit 1
fi

ID=$1
ALIAS=$2
PIN=$3
CERTIFICADO=$4

openssl pkcs12 -in "${CERTIFICADO}" -nocerts -nodes  -out "${CERTIFICADO}.key.pem"   -legacy -passin pass:nada
openssl pkcs12 -in "${CERTIFICADO}" -clcerts -nokeys -out "${CERTIFICADO}.cert.pem"  -legacy -passin pass:nada

pkcs15-init --store-private-key "${CERTIFICADO}.key.pem"  -a "01" -i "$ID"A --key-usage sign --pin "${PIN}" --label KeyFirmaDigital"${ALIAS}" 
pkcs15-init --store-certificate "${CERTIFICADO}.cert.pem" -a "01" -i "$ID"B --auth-id "$ID"A --pin "${PIN}" --label CertFirmaDigital"${ALIAS}"

rm ${CERTIFICADO}.key.pem
rm ${CERTIFICADO}.cert.pem



