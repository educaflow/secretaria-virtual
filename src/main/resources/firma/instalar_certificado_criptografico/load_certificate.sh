#!/bin/bash
# Carga en el dispositivo criptográfico (ePass2003 u otro token PKCS#15) la clave privada y el certificado
# de un fichero .p12. Se para en el primer paso que falle (no deja certificados sin clave en el token) y
# guarda la traza completa de pkcs15-init en un fichero de log para poder diagnosticar el fallo.

# Función para mostrar ayuda
mostrar_ayuda() {
    echo "Uso: $0 ID SUFIJO_ALIAS PIN CERTIFICADO [PASSWORD_P12]"
    echo
    echo "Parámetros:"
    echo "  ID             Identificador (hexadecimal) del certificado, debe ser único en el token. Empezar con 01"
    echo "  SUFIJO_ALIAS   Sufijo del Alias para el certificado. Es el nombre de la persona del certificado"
    echo "  PIN            PIN del dispositivo que se usa al crearse."
    echo "  CERTIFICADO    Archivo .p12 del certificado"
    echo "  PASSWORD_P12   Contraseña del archivo .p12 (opcional, por defecto demo1234)"
    echo
    echo "Si algo falla, la traza completa de pkcs15-init queda en <CERTIFICADO>.pkcs15-init.log"
    echo "Para ver lo que hay en el token: pkcs15-tool -D"
}

# Comprobar si el usuario pide ayuda
if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    mostrar_ayuda
    exit 0
fi

# Comprobar que haya 4 o 5 parámetros
if [[ $# -lt 4 || $# -gt 5 ]]; then
    echo "Error: Se requieren 4 parámetros y un quinto opcional."
    mostrar_ayuda
    exit 1
fi

ID=$1
ALIAS=$2
PIN=$3
CERTIFICADO=$4
PASSWORD_P12=${5:-demo1234}

AUTH_ID=01   # ID del PIN de usuario que crea inicializar_dispositivo.sh (perfil pkcs15+onepin)

# El driver epass2003 de OpenSC (hasta la 0.25 al menos) envía el exponente privado d sin rellenar, y el token rechaza las claves cuyo d es más
# corto que el módulo (una de cada ~128). El parche epass2003-pad-d.patch de esta carpeta lo arregla; en opensc-parcheado/ hay un pkcs15-init
# ya compilado con él, que se usa en vez del del sistema (la variable PKCS15_INIT permite indicar otro).
DIR_SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PKCS15_INIT=${PKCS15_INIT:-$DIR_SCRIPT/opensc-parcheado/bin/pkcs15-init}
if [[ -x "$PKCS15_INIT" ]]; then
    PKCS15_INIT_PARCHEADO=1
else
    PKCS15_INIT=pkcs15-init
    PKCS15_INIT_PARCHEADO=0
fi
KEY_PEM="${CERTIFICADO}.key.pem"
CERT_PEM="${CERTIFICADO}.cert.pem"
LOG="${CERTIFICADO}.pkcs15-init.log"

# Los ficheros PEM con la clave en claro se borran siempre, acabe como acabe el script
trap 'rm -f "$KEY_PEM" "$CERT_PEM"' EXIT

error() {
    echo "ERROR: $1" >&2
    exit 1
}

# Ejecuta pkcs15-init con traza completa guardada en el log. Si falla, enseña el final de la traza y aborta.
ejecutar_pkcs15_init() {
    local descripcion="$1"
    shift
    echo "==> $descripcion"
    echo "===== $descripcion: pkcs15-init $*" >> "$LOG"
    if ! "$PKCS15_INIT" -vvv "$@" 2>> "$LOG"; then
        echo "ERROR: ha fallado '$descripcion'. Últimas líneas de la traza ($LOG):" >&2
        grep -nE "APDU|SW|sc_|Failed|rror" "$LOG" | tail -25 >&2
        error "No se ha cargado el certificado de ${ALIAS}. Revisa $LOG y comprueba el estado del token con: pkcs15-tool -D"
    fi
}

[[ -f "$CERTIFICADO" ]] || error "No existe el fichero $CERTIFICADO"
[[ "$ID" =~ ^([0-9A-Fa-f]{2})+$ ]] || error "El ID debe ser hexadecimal con un número par de dígitos (p.ej. 01, 02, 0A)"
: > "$LOG"

# Comprobar que el ID no esté ya ocupado por una clave o un certificado del token (el PIN también tiene ID 01, pero no cuenta)
if { pkcs15-tool --list-keys; pkcs15-tool --list-certificates; } 2>/dev/null | grep -qE "^\s+ID\s+:\s+${ID}$"; then
    error "Ya hay un objeto con ID ${ID} en el token. Mira 'pkcs15-tool -D' y usa otro ID o bórralo con: pkcs15-init --delete-objects privkey,pubkey,cert --id ${ID} --pin <PIN>"
fi

openssl pkcs12 -in "${CERTIFICADO}" -nocerts -nodes  -out "$KEY_PEM"  -passin pass:"${PASSWORD_P12}" || error "No se ha podido extraer la clave privada del .p12 (¿contraseña incorrecta?)"
openssl pkcs12 -in "${CERTIFICADO}" -clcerts -nokeys -out "$CERT_PEM" -passin pass:"${PASSWORD_P12}" || error "No se ha podido extraer el certificado del .p12"

# Información de la clave: el ePass2003 solo admite RSA de hasta 2048 bits
openssl x509 -in "$CERT_PEM" -noout -subject -text | grep -E "Subject:|Public Key Algorithm|Public-Key"

# Longitud en bytes de un número del volcado de openssl rsa -text (se cuentan los pares hexadecimales, quitando el 00 inicial si lo hay)
bytes_de_factor() {
    openssl rsa -in "$KEY_PEM" -noout -text 2>/dev/null | awk -v campo="$1" '
        $0 ~ "^"campo":" { leyendo=1; next }
        leyendo && /^[a-zA-Z]/ { exit }
        leyendo { gsub(/[ \t:]/, ""); hex = hex $0 }
        END { sub(/^00/, "", hex); print length(hex) / 2 }'
}
BYTES_N=$(bytes_de_factor modulus)
BYTES_D=$(bytes_de_factor privateExponent)
echo "Módulo n: ${BYTES_N} bytes, exponente privado d: ${BYTES_D} bytes"
# El driver epass2003 de OpenSC sin parchear envía d tal cual, sin rellenar, y el token rechaza (SW 6A80, "write d failed") los d más cortos que n
if [[ "$BYTES_D" -lt "$BYTES_N" && "$PKCS15_INIT_PARCHEADO" -eq 0 ]]; then
    error "El exponente privado d (${BYTES_D} bytes) es más corto que el módulo (${BYTES_N} bytes) y el pkcs15-init del sistema no lo importa en el ePass2003 (responde 'write d failed: Incorrect parameters in APDU'). Usa el pkcs15-init parcheado de opensc-parcheado/ (ver README.txt) y vuelve a lanzar el script."
fi
[[ "$PKCS15_INIT_PARCHEADO" -eq 1 ]] && echo "Usando pkcs15-init parcheado: $PKCS15_INIT"

# La clave privada y el certificado llevan el MISMO ID: así PKCS#15 los asocia (y AutoFirma los ve como un único certificado con clave)
ejecutar_pkcs15_init "Guardar clave privada de ${ALIAS}" --store-private-key "$KEY_PEM"  -a "$AUTH_ID" -i "$ID" --key-usage sign --pin "${PIN}" --label KeyFirmaDigital"${ALIAS}"
ejecutar_pkcs15_init "Guardar certificado de ${ALIAS}"   --store-certificate "$CERT_PEM" -a "$AUTH_ID" -i "$ID"                   --pin "${PIN}" --label CertFirmaDigital"${ALIAS}"

echo "OK: certificado de ${ALIAS} cargado con ID ${ID}. Contenido del token:"
pkcs15-tool -D | grep -E "^(Private|Public|X\.509)|^\s+ID\s+:|^\s+Path"
