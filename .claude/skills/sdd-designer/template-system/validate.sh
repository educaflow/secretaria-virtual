#!/usr/bin/env bash
# Validación mecánica de los artefactos XML de un diseño contra los XSD de Axelor (AOP).
# Es el hook de validación determinista de esta plantilla: lo ejecuta el **subagente
# verificador** con `Bash` en la fase de verificación (`sdd-designer` SKILL.md §10),
# tal como prescribe `validacion.md` §1. El motor `sdd-designer` **NUNCA** lo ejecuta:
# solo conoce el `README.md` de la plantilla, y son los subagentes quienes descubren
# este script leyéndolo. Es la fuente de verdad de la validación XML; `validacion.md` §1
# documenta los mismos comandos como referencia.
#
# Uso:   bash validate.sh <ruta-carpeta-design> [<ruta-resources-AOP>]
#   <ruta-carpeta-design>   carpeta del diseño a validar (contiene domains/, views/, menus.xml).
#   <ruta-resources-AOP>    opcional; carpeta con los XSD de AOP.
#                           Por defecto: ../axelor-open-platform/axelor-core/src/main/resources
#                           (relativa a la raíz del proyecto, que es el CWD esperado).
#
# Salida: una línea "FAIL: <fichero>" por cada XML inválido (con el error de xmllint),
#         y como última línea "VALIDACION-XML: OK" (código 0) o "VALIDACION-XML: FAIL" (código 1).

set -u

DESIGN_DIR="${1:?Uso: validate.sh <ruta-carpeta-design> [<ruta-resources-AOP>]}"
AOP_RES="${2:-../axelor-open-platform/axelor-core/src/main/resources}"

DOMAIN_XSD="$AOP_RES/domain-models.xsd"
VIEWS_XSD="$AOP_RES/object-views.xsd"

fail=0

validate_one() {
  local file="$1" xsd="$2"
  [ -e "$file" ] || return 0
  if ! xmllint --noout --schema "$xsd" "$file"; then
    echo "FAIL: $file"
    fail=1
  fi
}

shopt -s nullglob
for f in "$DESIGN_DIR"/domains/*.xml; do validate_one "$f" "$DOMAIN_XSD"; done
for f in "$DESIGN_DIR"/views/*.xml;   do validate_one "$f" "$VIEWS_XSD"; done
validate_one "$DESIGN_DIR/menus.xml" "$VIEWS_XSD"

if [ "$fail" -ne 0 ]; then
  echo "VALIDACION-XML: FAIL"
  exit 1
fi
echo "VALIDACION-XML: OK"
exit 0
