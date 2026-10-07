#!/bin/bash
# Genera en almacen/ el almacén de certificados confiables de demo, que solo usan los entornos de demo (desarrollo y
# preproducción) y que no toca nada de ../AlmacenCertificadosConfiables:
# - truststore-demo.jks: una copia de ../AlmacenCertificadosConfiables/truststore.jks con la raíz de la CA FALSA de demo.
# - educaflow-demo-ca.crl: la CRL (vacía) de la CA de demo, y crls-demo.xml, la lista de CRLs: las oficiales de
#   ../AlmacenCertificadosConfiables/crl/crls.xml (para poder probar con certificados reales) más la de demo.
#   Sin esa CRL la validación PKIX rechaza los certificados de demo: no tienen OCSP ni CRLDP y el PKIXRevocationChecker
#   solo los da por buenos si encuentra una CRL vigente de su emisor.

DIR_SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIR_ALMACEN="$(cd "${DIR_SCRIPT}/../AlmacenCertificadosConfiables" && pwd)"
TRUSTSTORE="${DIR_ALMACEN}/truststore.jks"
LISTA_CRLS="${DIR_ALMACEN}/crl/crls.xml"
# Ruta de las CRL oficiales vista desde almacen/ (la carpeta de crls-demo.xml)
RUTA_RELATIVA_CRLS="../../AlmacenCertificadosConfiables/crl"
TRUSTSTORE_PASSWORD=s3cr3T
CA_CRT="${DIR_SCRIPT}/ca/ca.crt.pem"
CA_KEY="${DIR_SCRIPT}/ca/ca.key.pem"
ALIAS_CA_DEMO=educaflow-demo-ca
DIR_ALMACEN_DEMO="${DIR_SCRIPT}/almacen"
TRUSTSTORE_DEMO="${DIR_ALMACEN_DEMO}/truststore-demo.jks"
FICHERO_CRL_DEMO=educaflow-demo-ca.crl
LISTA_CRLS_DEMO="${DIR_ALMACEN_DEMO}/crls-demo.xml"
DIAS_VALIDEZ_CRL=10950

# Función para mostrar ayuda
mostrar_ayuda() {
    echo "Uso: $0"
    echo
    echo "Genera en ${DIR_ALMACEN_DEMO}:"
    echo "  truststore-demo.jks    copia de ${TRUSTSTORE} (que no se modifica) con la CA de demo (alias ${ALIAS_CA_DEMO})"
    echo "  ${FICHERO_CRL_DEMO}  la CRL de la CA de demo"
    echo "  crls-demo.xml          la lista de CRLs: las oficiales de ${LISTA_CRLS} más la de la CA de demo"
    echo "Si ya existen los regenera. Hay que volver a lanzarlo cada vez que se regenere truststore.jks o crls.xml."
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

if [[ ! -f "$CA_CRT" || ! -f "$CA_KEY" ]]; then
    echo "Error: No existe la CA de demo (${CA_CRT} y ${CA_KEY}). Créala antes con crear_ca_demo.sh"
    exit 1
fi
if [[ ! -f "$TRUSTSTORE" ]]; then
    echo "Error: No existe el almacén de certificados confiables ${TRUSTSTORE}"
    exit 1
fi
if [[ ! -f "$LISTA_CRLS" ]]; then
    echo "Error: No existe la lista de CRLs ${LISTA_CRLS}"
    exit 1
fi

mkdir -p "$DIR_ALMACEN_DEMO"

# Se copia el fichero entero (no entrada a entrada) para conservar también el par de claves "educaflow"
rm -f "$TRUSTSTORE_DEMO"
cp "$TRUSTSTORE" "$TRUSTSTORE_DEMO"

if ! keytool -import -trustcacerts -noprompt -alias "$ALIAS_CA_DEMO" -file "$CA_CRT" -keystore "$TRUSTSTORE_DEMO" -storepass "$TRUSTSTORE_PASSWORD"; then
    rm -f "$TRUSTSTORE_DEMO"
    echo "Error: No se ha podido importar la CA de demo en ${TRUSTSTORE_DEMO}"
    exit 1
fi

echo "Generado ${TRUSTSTORE_DEMO} con la CA de demo (alias ${ALIAS_CA_DEMO})"

# CRL vacía de la CA de demo, con la misma vigencia que la CA
DIR_TEMPORAL=$(mktemp -d)
trap 'rm -rf "${DIR_TEMPORAL}"' EXIT
touch "${DIR_TEMPORAL}/index.txt"
echo 1000 > "${DIR_TEMPORAL}/crlnumber"
{
    echo "[ ca ]"
    echo "default_ca = ca_demo"
    echo
    echo "[ ca_demo ]"
    echo "database         = ${DIR_TEMPORAL}/index.txt"
    echo "crlnumber        = ${DIR_TEMPORAL}/crlnumber"
    echo "default_md       = sha256"
    echo "default_crl_days = ${DIAS_VALIDEZ_CRL}"
} > "${DIR_TEMPORAL}/openssl.cnf"

if ! openssl ca -batch -config "${DIR_TEMPORAL}/openssl.cnf" -gencrl -keyfile "$CA_KEY" -cert "$CA_CRT" -out "${DIR_TEMPORAL}/crl.pem" 2>/dev/null; then
    echo "Error: No se ha podido generar la CRL de la CA de demo"
    exit 1
fi
if ! openssl crl -in "${DIR_TEMPORAL}/crl.pem" -outform DER -out "${DIR_ALMACEN_DEMO}/${FICHERO_CRL_DEMO}"; then
    echo "Error: No se ha podido guardar la CRL de la CA de demo en ${DIR_ALMACEN_DEMO}/${FICHERO_CRL_DEMO}"
    exit 1
fi

# Las CRL oficiales (las de crls.xml, para poder probar con certificados reales) más la de demo. Los nombres de la
# lista se resuelven respecto a su carpeta: las oficiales se referencian con una ruta relativa, sin copiarlas.
{
    echo '<?xml version="1.0" encoding="UTF-8"?>'
    echo '<crls>'
    grep -oP '<crl>\K[^<]+(?=</crl>)' "$LISTA_CRLS" | while read -r crl; do
        echo "  <crl>${RUTA_RELATIVA_CRLS}/${crl}</crl>"
    done
    echo "  <crl>${FICHERO_CRL_DEMO}</crl>"
    echo '</crls>'
} > "$LISTA_CRLS_DEMO"

echo "Generadas ${DIR_ALMACEN_DEMO}/${FICHERO_CRL_DEMO} (CRL de la CA de demo) y ${LISTA_CRLS_DEMO}"
