#!/bin/bash
# Emite con la CA FALSA de demo el certificado de un usuario de demo, con los datos de usuarios-demo.xml.
# Solo crea el .p12: no toca usuarios-demo.xml ni certificados-demo.xml (el alta en CertificadoDigital).

DIR_SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=comun_demo.sh
source "${DIR_SCRIPT}/comun_demo.sh"

# Función para mostrar ayuda
mostrar_ayuda() {
    echo "Uso: $0 email"
    echo
    echo "Parámetros:"
    echo "  email   Email del usuario en data-demo/input/usuarios-demo.xml"
    echo
    echo "Crea certificados/<DNI>.p12 (contraseña ${PASSWORD_P12_DEMO}) con el DNI, el nombre y los apellidos del usuario."
    echo "No admite los usuarios que, a propósito, no tienen certificado: ${USUARIOS_SIN_CERTIFICADO[*]}"
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

EMAIL=$1

if es_usuario_sin_certificado "$EMAIL"; then
    echo "Error: El usuario ${EMAIL} no debe tener certificado: existe precisamente para probar lo que pasa sin él."
    exit 1
fi

DATOS_USUARIO=$(buscar_usuario_demo "$EMAIL") || exit 1
IFS=$'\t' read -r DNI NOMBRE APELLIDOS <<< "$DATOS_USUARIO"

FICHERO_P12="${DIR_CERTIFICADOS_DEMO}/${DNI}.p12"

emitir_certificado "$DNI" "$NOMBRE" "$APELLIDOS" "$FICHERO_P12" || exit 1

echo "Certificado de ${EMAIL} creado"
echo "  Ruta classpath: firma/demo/certificados/${DNI}.p12"
echo "  DNI:            ${DNI}"
