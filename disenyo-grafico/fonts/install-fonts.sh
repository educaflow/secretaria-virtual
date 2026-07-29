#!/usr/bin/env bash
set -euo pipefail

# Instala la fuente Roboto para el usuario actual a partir de Roboto.zip.
# Descomprime el zip, copia las variantes estáticas a $HOME/.local/share/fonts
# y borra el contenido descomprimido.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ZIP_FILE="$SCRIPT_DIR/Roboto.zip"
EXTRACT_DIR="$SCRIPT_DIR/Roboto-extracted"
FONTS_DIR="$HOME/.local/share/fonts/Roboto"

if [[ ! -f "$ZIP_FILE" ]]; then
    echo "ERROR: no se encuentra $ZIP_FILE" >&2
    exit 1
fi

echo "Descomprimiendo $ZIP_FILE ..."
rm -rf "$EXTRACT_DIR"
unzip -q "$ZIP_FILE" -d "$EXTRACT_DIR"

echo "Instalando fuentes en $FONTS_DIR ..."
mkdir -p "$FONTS_DIR"
# Se instalan las variantes estáticas (LibreOffice no maneja los ejes
# de las fuentes variables; con las estáticas están todos los estilos).
cp "$EXTRACT_DIR"/static/*.ttf "$FONTS_DIR/"

echo "Refrescando la caché de fuentes ..."
fc-cache -f "$FONTS_DIR"

echo "Borrando el contenido descomprimido ..."
rm -rf "$EXTRACT_DIR"

echo "Hecho: $(ls "$FONTS_DIR" | wc -l) fuentes instaladas en $FONTS_DIR"
