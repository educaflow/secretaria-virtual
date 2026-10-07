#!/bin/bash
# Funciones comunes de los scripts de la CA FALSA de demo. No se ejecuta: lo cargan con "source"
# crear_certificado_usuario_demo.sh, crear_certificado_test.sh e instalar_certificado_criptografico/install.sh.

DIR_DEMO="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIR_CA="${DIR_DEMO}/ca"
CA_KEY="${DIR_CA}/ca.key.pem"
CA_CRT="${DIR_CA}/ca.crt.pem"
DIR_CERTIFICADOS_DEMO="${DIR_DEMO}/certificados"
USUARIOS_DEMO_XML="$(cd "${DIR_DEMO}/../.." && pwd)/data-demo/input/usuarios-demo.xml"
GENERAR_DNI_DEMO="$(cd "${DIR_DEMO}/../.." && pwd)/data-demo/generar_dni_demo.sh"
PASSWORD_P12_DEMO="demo1234"
DIAS_VALIDEZ_CERTIFICADO=7300

# Usuarios de demo que, a propósito, no tienen certificado
USUARIOS_SIN_CERTIFICADO=("sincertificado1@mislata.es" "sincertificado2@mislata.es" "autofirma@mislata.es")

# Imprime "DNI<TAB>nombre<TAB>apellidos" del <usuario> de usuarios-demo.xml con ese email.
# Falla si no existe el usuario o no tiene DNI.
buscar_usuario_demo() {
    local email="$1"
    python3 - "$USUARIOS_DEMO_XML" "$email" <<'EOF'
import sys
import xml.etree.ElementTree as ET

fichero, email = sys.argv[1], sys.argv[2]
usuarios = [u for u in ET.parse(fichero).getroot().iter("usuario") if u.get("email") == email]
if len(usuarios) != 1:
    sys.exit(f"Error: No existe ningún usuario con el email '{email}' en {fichero}")
usuario = usuarios[0]
dni = (usuario.get("documento") or "").strip()
if not dni:
    sys.exit(f"Error: El usuario '{email}' no tiene DNI (atributo documento) en {fichero}")
print(f"{dni}\t{usuario.get('nombre') or ''}\t{usuario.get('apellidos') or ''}")
EOF
}

# Imprime todos los DNI de los usuarios de usuarios-demo.xml, uno por línea
dnis_usuarios_demo() {
    python3 - "$USUARIOS_DEMO_XML" <<'EOF'
import sys
import xml.etree.ElementTree as ET

for usuario in ET.parse(sys.argv[1]).getroot().iter("usuario"):
    print((usuario.get("documento") or "").strip())
EOF
}

es_usuario_sin_certificado() {
    local email="$1"
    local sin_certificado
    for sin_certificado in "${USUARIOS_SIN_CERTIFICADO[@]}"; do
        if [[ "$email" == "$sin_certificado" ]]; then
            return 0
        fi
    done
    return 1
}

# Comprueba con asn1parse que el UID del subject, y el UID y el CN del SubjectAltName, van en UTF8String
# con los valores esperados: la aplicación ignora cualquier otra codificación (CertificateParser).
comprobar_codificacion_utf8() {
    local certificado="$1"
    local dni="$2"
    local nombre_apellidos_san="$3"

    local offset_san
    offset_san=$(openssl asn1parse -in "$certificado" | awk '/:X509v3 Subject Alternative Name/ {encontrado=1; next} encontrado && /OCTET STRING/ {split($1, partes, ":"); print partes[1]; exit}')
    if [[ -z "$offset_san" ]]; then
        echo "Error: El certificado no tiene la extensión SubjectAltName"
        return 1
    fi

    # Línea siguiente a cada OID buscado: "<tipo>:<valor>"
    local valores_subject valores_san
    valores_subject=$(openssl asn1parse -in "$certificado" | awk -F'prim: ' '/:userId$/ {getline; print $2}' | sed -E 's/ +:/:/')
    valores_san=$(openssl asn1parse -in "$certificado" -strparse "$offset_san" | awk -F'prim: ' '/:userId$|:commonName$/ {getline; print $2}' | sed -E 's/ +:/:/')

    if [[ "$valores_subject" != "UTF8STRING:${dni}" ]]; then
        echo "Error: El UID del subject no es un UTF8String con el DNI ${dni}: '${valores_subject}'"
        return 1
    fi
    if ! grep -qxF "UTF8STRING:${dni}" <<< "$valores_san"; then
        echo "Error: El UID del SubjectAltName no es un UTF8String con el DNI ${dni}: '${valores_san}'"
        return 1
    fi
    if ! grep -qxF "UTF8STRING:${nombre_apellidos_san}" <<< "$valores_san"; then
        echo "Error: El CN del SubjectAltName no es un UTF8String con '${nombre_apellidos_san}': '${valores_san}'"
        return 1
    fi
    return 0
}

# Emite un certificado de usuario final firmado directamente por la CA de demo y lo guarda en un .p12.
# Parámetros: DNI nombre apellidos fichero_p12
emitir_certificado() {
    local dni="$1"
    local nombre="$2"
    local apellidos="$3"
    local fichero_p12="$4"

    if [[ ! -f "$CA_KEY" || ! -f "$CA_CRT" ]]; then
        echo "Error: No existe la CA de demo (${DIR_CA}). Créala antes con crear_ca_demo.sh"
        return 1
    fi
    if [[ -e "$fichero_p12" ]]; then
        echo "Error: Ya existe el fichero ${fichero_p12}. No se sobrescribe: bórralo a mano si quieres regenerarlo"
        return 1
    fi
    if [[ -z "$dni" || -z "$nombre" || -z "$apellidos" ]]; then
        echo "Error: El DNI, el nombre y los apellidos son obligatorios ('${dni}', '${nombre}', '${apellidos}')"
        return 1
    fi
    # Caracteres que romperían el fichero de configuración de openssl o el separador del CN
    if [[ "$dni$nombre$apellidos" =~ [\$\"\\|=,+\;\<\>#] ]]; then
        echo "Error: El DNI, el nombre o los apellidos contienen caracteres no admitidos: '${dni}', '${nombre}', '${apellidos}'"
        return 1
    fi

    local dir_temporal
    dir_temporal=$(mktemp -d)
    # shellcheck disable=SC2064
    trap "rm -rf '${dir_temporal}'; trap - RETURN EXIT" RETURN
    trap "rm -rf '${dir_temporal}'" EXIT

    local nombre_apellidos_san="${nombre}|${apellidos}"

    cat > "${dir_temporal}/openssl.cnf" <<EOF
[ req ]
distinguished_name = dn_sujeto
prompt             = no
string_mask        = utf8only
utf8               = yes

[ dn_sujeto ]
CN           = ${nombre} ${apellidos} - NIF:${dni}
serialNumber = ${dni}
UID          = ${dni}

[ extensiones_usuario ]
basicConstraints       = critical,CA:FALSE
keyUsage               = critical,digitalSignature,nonRepudiation
subjectKeyIdentifier   = hash
authorityKeyIdentifier = keyid
subjectAltName         = dirName:dn_subject_alt_name

[ dn_subject_alt_name ]
CN  = ${nombre_apellidos_san}
UID = ${dni}
EOF

    openssl genrsa -out "${dir_temporal}/clave.pem" 2048 2>/dev/null || return 1
    openssl req -new -utf8 -config "${dir_temporal}/openssl.cnf" -key "${dir_temporal}/clave.pem" -out "${dir_temporal}/peticion.csr" || return 1
    openssl x509 -req -in "${dir_temporal}/peticion.csr" -CA "$CA_CRT" -CAkey "$CA_KEY" \
        -set_serial "0x$(openssl rand -hex 16)" -days "$DIAS_VALIDEZ_CERTIFICADO" -sha256 \
        -extfile "${dir_temporal}/openssl.cnf" -extensions extensiones_usuario \
        -out "${dir_temporal}/certificado.pem" 2>/dev/null || return 1

    comprobar_codificacion_utf8 "${dir_temporal}/certificado.pem" "$dni" "$nombre_apellidos_san" || return 1

    mkdir -p "$(dirname "$fichero_p12")"
    # Algoritmos modernos (AES-256 + PBKDF2 + MAC SHA-256): los lee el JDK sin -legacy
    openssl pkcs12 -export -in "${dir_temporal}/certificado.pem" -inkey "${dir_temporal}/clave.pem" \
        -name "${nombre} ${apellidos}" -keypbe AES-256-CBC -certpbe AES-256-CBC -macalg sha256 \
        -passout "pass:${PASSWORD_P12_DEMO}" -out "${dir_temporal}/certificado.p12" || return 1
    mv "${dir_temporal}/certificado.p12" "$fichero_p12"
}
