#!/bin/bash

# Función para mostrar ayuda
mostrar_ayuda() {
    echo "Uso: $0 CN fichero"
    echo "CN       CN del Subject del certificado "
    echo "fichero  Nombre del fichero p12 que se creará "
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

CN=$1
FICHERO=$2

openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout /tmp/temp.key.pem -out /tmp/temp.crt.pem -subj "/C=ES/ST=CV/L=Mislata/O=GVA/OU=IT/CN=$CN"
openssl pkcs12 -export -out "$FICHERO" -inkey /tmp/temp.key.pem -in /tmp/temp.crt.pem -name "$CN" -passout pass:nada 
rm /tmp/temp.key.pem /tmp/temp.crt.pem


