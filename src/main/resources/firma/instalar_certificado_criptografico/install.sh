#!/bin/bash
# Carga en el dispositivo criptográfico los certificados de demo (CA FALSA de ../demo) del director y del
# secretario de CIPFP Mislata. Sus DNI (y por tanto sus ficheros) se sacan de usuarios-demo.xml por email.

DIR_SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../demo/comun_demo.sh
source "${DIR_SCRIPT}/../demo/comun_demo.sh"

# Imprime la ruta del .p12 de demo del usuario con ese email
fichero_certificado_demo() {
    local email="$1"
    local datos_usuario dni
    datos_usuario=$(buscar_usuario_demo "$email") || return 1
    IFS=$'\t' read -r dni _ <<< "$datos_usuario"
    echo "${DIR_CERTIFICADOS_DEMO}/${dni}.p12"
}

CERTIFICADO_DIRECTOR=$(fichero_certificado_demo "director@mislata.es") || exit 1
CERTIFICADO_SECRETARIO=$(fichero_certificado_demo "secretario@mislata.es") || exit 1

cd "$DIR_SCRIPT" || exit 1

./inicializar_dispositivo.sh 123456 12345678

./load_certificate.sh 01 Director  123456 "$CERTIFICADO_DIRECTOR"

./load_certificate.sh 02 Secretario  123456 "$CERTIFICADO_SECRETARIO"
