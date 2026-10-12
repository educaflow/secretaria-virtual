#!/bin/bash
# Instala y configura OpenSC en Linux (Debian/Ubuntu con apt) para poder usar un dispositivo criptográfico
# (ePass2003 u otro token PKCS#15) desde los scripts de esta carpeta y desde la aplicación (Java PKCS#11 / AutoFirma):
#   1. Instala los paquetes opensc, pcscd, pcsc-tools y libccid.
#   2. Activa y arranca el servicio pcscd (el demonio que habla con el lector).
#   3. Pone "disable_popups = true" en /etc/opensc/opensc.conf (si no, OpenSC abre ventanas pidiendo el PIN).
#   4. Comprueba la instalación y muestra la ruta de la librería PKCS#11, que es la que necesita la aplicación.
# Es idempotente: se puede lanzar las veces que haga falta. Usa sudo para lo que necesita root.
# El pkcs15-init del sistema NO se usa para cargar certificados: load_certificate.sh usa el parcheado de
# opensc-parcheado/ (ver README.txt), pero necesita igualmente las librerías del sistema que instala este script.

# Función para mostrar ayuda
mostrar_ayuda() {
    echo "Uso: $0"
    echo
    echo "Instala OpenSC y pcscd, activa el servicio pcscd, configura /etc/opensc/opensc.conf y comprueba la instalación."
    echo "No necesita parámetros. Pide la contraseña de sudo."
}

# Comprobar si el usuario pide ayuda
if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    mostrar_ayuda
    exit 0
fi

if [[ $# -ne 0 ]]; then
    echo "Error: este script no admite parámetros."
    mostrar_ayuda
    exit 1
fi

OPENSC_CONF=/etc/opensc/opensc.conf
PAQUETES=(opensc pcscd pcsc-tools libccid)

error() {
    echo "ERROR: $1" >&2
    exit 1
}

command -v apt-get >/dev/null 2>&1 || error "No se ha encontrado apt-get. Este script es para Debian/Ubuntu; en otra distribución instala a mano los paquetes equivalentes a: ${PAQUETES[*]}"
command -v sudo >/dev/null 2>&1 || error "No se ha encontrado sudo"

echo "==> Instalando paquetes: ${PAQUETES[*]}"
sudo apt-get update || error "Ha fallado apt-get update"
sudo apt-get install -y "${PAQUETES[@]}" || error "Ha fallado la instalación de los paquetes"

echo "==> Activando y arrancando el servicio pcscd"
sudo systemctl enable --now pcscd || error "No se ha podido activar pcscd"

echo "==> Configurando $OPENSC_CONF (disable_popups = true)"
if [[ ! -f "$OPENSC_CONF" ]]; then
    error "No existe $OPENSC_CONF. ¿Se ha instalado bien el paquete opensc?"
fi
if grep -qE '^\s*disable_popups\s*=\s*true' "$OPENSC_CONF"; then
    echo "Ya estaba configurado, no se toca"
else
    sudo cp "$OPENSC_CONF" "${OPENSC_CONF}.bak" || error "No se ha podido hacer copia de seguridad de $OPENSC_CONF"
    echo "Copia de seguridad en ${OPENSC_CONF}.bak"
    if grep -qE '^\s*app\s+default\s*\{' "$OPENSC_CONF"; then
        # Ya hay un bloque "app default {" sin comentar: se añade la línea justo después de abrirlo
        sudo sed -i -E '0,/^\s*app\s+default\s*\{/s//&\n\tdisable_popups = true;/' "$OPENSC_CONF" || error "No se ha podido editar $OPENSC_CONF"
    else
        # No hay bloque (en Ubuntu viene todo comentado): se añade uno al final
        printf '\napp default {\n\tdisable_popups = true;\n}\n' | sudo tee -a "$OPENSC_CONF" >/dev/null || error "No se ha podido editar $OPENSC_CONF"
    fi
    echo "Configurado"
fi

echo "==> Comprobando la instalación"
for herramienta in opensc-tool pkcs11-tool pkcs15-init pkcs15-tool pcsc_scan; do
    command -v "$herramienta" >/dev/null 2>&1 || error "No se ha encontrado $herramienta tras la instalación"
done
echo "Versión de OpenSC: $(opensc-tool -i | head -1)"
systemctl is-active --quiet pcscd && echo "Servicio pcscd: activo" || error "El servicio pcscd no está activo"

LIBRERIA_PKCS11=$(find /usr/lib /usr/lib64 -name opensc-pkcs11.so 2>/dev/null | head -1)
if [[ -n "$LIBRERIA_PKCS11" ]]; then
    echo "Librería PKCS#11 (la que necesita la aplicación en la configuración del entorno criptográfico): $LIBRERIA_PKCS11"
else
    echo "AVISO: no se ha encontrado opensc-pkcs11.so bajo /usr/lib" >&2
fi

echo
echo "OK: OpenSC instalado y configurado."
echo "Lectores y dispositivos conectados (opensc-tool -l):"
opensc-tool -l
echo
echo "Siguiente paso: conecta el dispositivo y lanza ./install.sh (o inicializar_dispositivo.sh y load_certificate.sh a mano)."
