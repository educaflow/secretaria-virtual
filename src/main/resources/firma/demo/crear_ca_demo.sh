#!/bin/bash
# Crea la CA raíz FALSA de demo (ca/ca.key.pem y ca/ca.crt.pem) y, a continuación, el truststore de demo.
# Se lanza UNA sola vez: regenerar la CA invalidaría todos los .p12 emitidos (los de demo y los de test).

DIR_SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=comun_demo.sh
source "${DIR_SCRIPT}/comun_demo.sh"

DIAS_VALIDEZ_CA=10950
SUBJECT_CA="/C=ES/O=EducaFlow DEMO/CN=ACCV DEMO EducaFlow - NO VALIDA"

# Función para mostrar ayuda
mostrar_ayuda() {
    echo "Uso: $0"
    echo
    echo "Crea la CA raíz FALSA de demo en ${DIR_CA} y genera el truststore de demo (crear_truststore_demo.sh)."
    echo "Si la CA ya existe no hace nada: regenerarla invalidaría todos los certificados emitidos con ella."
}

# Comprobar si el usuario pide ayuda
if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    mostrar_ayuda
    exit 0
fi

# Comprobar que no haya parámetros
if [[ $# -ne 0 ]]; then
    echo "Error: No se admite ningún parámetro."
    mostrar_ayuda
    exit 1
fi

if [[ -e "$DIR_CA" ]]; then
    echo "Error: Ya existe la CA de demo en ${DIR_CA}."
    echo "Regenerarla invalidaría todos los .p12 emitidos con ella (los de los usuarios de demo y los de test) y el truststore de demo."
    exit 1
fi

DIR_TEMPORAL=$(mktemp -d)
trap 'rm -rf "${DIR_TEMPORAL}"' EXIT

cat > "${DIR_TEMPORAL}/openssl.cnf" <<EOF
[ req ]
distinguished_name = dn_sujeto
string_mask        = utf8only
utf8               = yes

[ dn_sujeto ]

[ extensiones_ca ]
basicConstraints     = critical,CA:TRUE
keyUsage             = critical,keyCertSign,cRLSign
subjectKeyIdentifier = hash
EOF

set -e
openssl genrsa -out "${DIR_TEMPORAL}/ca.key.pem" 3072 2>/dev/null
openssl req -new -x509 -utf8 -config "${DIR_TEMPORAL}/openssl.cnf" -extensions extensiones_ca \
    -key "${DIR_TEMPORAL}/ca.key.pem" -subj "$SUBJECT_CA" -days "$DIAS_VALIDEZ_CA" -sha256 \
    -out "${DIR_TEMPORAL}/ca.crt.pem"

mkdir -p "$DIR_CA"
mv "${DIR_TEMPORAL}/ca.key.pem" "$CA_KEY"
mv "${DIR_TEMPORAL}/ca.crt.pem" "$CA_CRT"
set +e

echo "CA de demo creada en ${DIR_CA}:"
openssl x509 -in "$CA_CRT" -noout -subject -dates

"${DIR_SCRIPT}/crear_truststore_demo.sh"
