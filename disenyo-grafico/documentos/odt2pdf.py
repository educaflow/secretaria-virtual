#!/usr/bin/env python3
"""Convierte un documento .odt a .pdf con LibreOffice en modo headless.

Uso:
    python3 odt2pdf.py entrada.odt [salida.pdf]

Si no se indica salida.pdf, se usa el nombre de entrada.odt cambiando la
extensión .odt por .pdf.

Los controles de formulario del .odt se conservan como campos AcroForm.
Si el .odt tiene varios controles con el MISMO nombre (p.ej. un campo inline
${...} repetido en valenciano y castellano), LibreOffice los renombra al
exportar añadiendo _2, _3...; este script deshace ese renombrado fusionándolos
en un único campo PDF con varios widgets, de forma que todos conservan el
nombre original y muestran el mismo valor.
"""

import argparse
import re
import shutil
import subprocess
import tempfile
import zipfile
from pathlib import Path

SUFIJO_RE = re.compile(r"(.+)_([0-9]+)$")


def odt_field_names(odt_path):
    """Nombres de los controles de formulario declarados en el .odt."""
    with zipfile.ZipFile(odt_path) as z:
        content = z.read("content.xml").decode("utf-8")
    names = re.findall(r'form:name="([^"]*)"', content)
    unescape = {"&quot;": '"', "&apos;": "'", "&lt;": "<", "&gt;": ">", "&amp;": "&"}
    result = []
    for n in names:
        for ent, ch in unescape.items():
            n = n.replace(ent, ch)
        result.append(n)
    return result


def pdf_string(s):
    return "(" + s.replace("\\", r"\\").replace("(", r"\(").replace(")", r"\)") + ")"


def merge_duplicated_fields(pdf_path, odt_names):
    """Fusiona los campos base_2, base_3... (renombrados por LibreOffice) con su
    campo base: un único campo con el nombre original y un widget por control.
    Devuelve el número de campos fusionados."""
    import fitz

    doc = fitz.open(str(pdf_path))
    by_name = {}
    for page in doc:
        for w in page.widgets():
            by_name.setdefault(w.field_name, []).append(w.xref)

    # base -> nombres renombrados por LibreOffice, en orden de sufijo.
    # Solo si el _N NO existe en el .odt y el base SÍ (evita fusionar campos
    # que legítimamente se llaman así).
    groups = {}
    for name in by_name:
        m = SUFIJO_RE.fullmatch(name)
        if m and m.group(1) in by_name and name not in odt_names and m.group(1) in odt_names:
            groups.setdefault(m.group(1), []).append((int(m.group(2)), name))
    if not groups:
        doc.close()
        return 0

    merged = 0
    for base, dups in sorted(groups.items()):
        dups.sort()
        kid_xrefs = by_name[base] + [x for _, n in dups for x in by_name[n]]
        b = kid_xrefs[0]

        # Nuevo nodo de campo: hereda nombre parcial, tipo y padre del base.
        p = doc.get_new_xref()
        doc.update_object(p, "<<>>")
        for key in ("T", "FT", "Ff", "V", "DA", "DR", "Q", "TU", "Parent"):
            ktype, kval = doc.xref_get_key(b, key)
            if ktype != "null":
                doc.xref_set_key(p, key, pdf_string(kval) if ktype == "string" else kval)
        doc.xref_set_key(p, "Kids", "[" + " ".join(f"{x} 0 R" for x in kid_xrefs) + "]")

        # Los controles quedan como widgets PUROS colgando del nuevo campo: se
        # reconstruye cada uno solo con sus claves de anotación. No basta con
        # poner a null las claves de campo (/T, /FT, /V...): quedarían presentes
        # con valor null e iText ya no los trataría como widgets.
        for x in kid_xrefs:
            doc.xref_set_key(x, "Parent", f"{p} 0 R")
            parts = []
            for key in ("Type", "Subtype", "F", "Rect", "P", "Parent", "AP",
                        "MK", "AS", "BS", "H", "OC"):
                ktype, kval = doc.xref_get_key(x, key)
                if ktype != "null":
                    parts.append(f"/{key} {pdf_string(kval) if ktype == 'string' else kval}")
            doc.update_object(x, "<<" + " ".join(parts) + ">>")

        # En la lista que contenía los campos: el base pasa a ser el nuevo nodo
        # y los duplicados desaparecen.
        parent_type, parent_val = doc.xref_get_key(p, "Parent")
        quitar = {str(x) for x in kid_xrefs[1:]}

        def nueva_lista(aval):
            refs = re.findall(r"(\d+) 0 R", aval)
            nuevos = [str(p) if r == str(b) else r for r in refs if r not in quitar]
            return "[" + " ".join(f"{r} 0 R" for r in nuevos) + "]"

        if parent_type != "null":
            gp = int(parent_val.split()[0])
            _, aval = doc.xref_get_key(gp, "Kids")
            doc.xref_set_key(gp, "Kids", nueva_lista(aval))
        else:
            cat = doc.pdf_catalog()
            aftype, afval = doc.xref_get_key(cat, "AcroForm")
            if aftype == "xref":
                af = int(afval.split()[0])
                _, aval = doc.xref_get_key(af, "Fields")
                doc.xref_set_key(af, "Fields", nueva_lista(aval))
            else:
                _, aval = doc.xref_get_key(cat, "AcroForm/Fields")
                doc.xref_set_key(cat, "AcroForm/Fields", nueva_lista(aval))

        merged += len(kid_xrefs) - 1

    tmp_out = str(pdf_path) + ".tmp"
    doc.save(tmp_out, garbage=1)
    doc.close()
    Path(tmp_out).replace(pdf_path)
    return merged


def main():
    parser = argparse.ArgumentParser(
        description="Convierte un documento .odt a .pdf con LibreOffice en modo headless.")
    parser.add_argument("entrada", type=Path, help="fichero .odt a convertir")
    parser.add_argument("salida", type=Path, nargs="?", default=None,
                        help="fichero .pdf a generar (defecto: la entrada con extensión .pdf)")
    args = parser.parse_args()

    entrada = args.entrada
    if not entrada.is_file():
        raise SystemExit(f"ERROR: no existe {entrada}")
    salida = args.salida if args.salida is not None else entrada.with_suffix(".pdf")

    soffice = shutil.which("soffice") or shutil.which("libreoffice")
    if soffice is None:
        raise SystemExit("ERROR: no se encuentra LibreOffice (soffice) en el PATH")

    # ExportFormFields explícito: la exportación headless con perfil limpio no
    # conserva los controles de formulario como campos AcroForm si no se fuerza.
    pdf_filter = ('pdf:writer_pdf_Export:'
                  '{"ExportFormFields":{"type":"boolean","value":"true"}}')

    with tempfile.TemporaryDirectory(prefix="odt2pdf-") as tmp:
        # Perfil de usuario propio y temporal: así la conversión funciona
        # aunque haya otro LibreOffice abierto con el perfil normal.
        profile = Path(tmp) / "profile"
        outdir = Path(tmp) / "out"
        outdir.mkdir()
        result = subprocess.run(
            [soffice, f"-env:UserInstallation=file://{profile}", "--headless",
             "--convert-to", pdf_filter, "--outdir", str(outdir), str(entrada)],
            capture_output=True, text=True)
        generado = outdir / entrada.with_suffix(".pdf").name
        if result.returncode != 0 or not generado.is_file():
            raise SystemExit("ERROR: LibreOffice no ha generado el PDF\n"
                             + result.stdout + result.stderr)
        shutil.move(str(generado), str(salida))

    names = odt_field_names(entrada)
    duplicados = {n for n in names if names.count(n) > 1}
    if duplicados:
        try:
            merged = merge_duplicated_fields(salida, names)
        except ImportError:
            raise SystemExit("ERROR: el .odt tiene controles con nombre duplicado "
                             f"({', '.join(sorted(duplicados))}) y hace falta PyMuPDF "
                             "para fusionarlos en el PDF (pip install pymupdf)")
        if merged:
            print(f"Fusionados {merged} campos renombrados por LibreOffice "
                  f"({', '.join(sorted(duplicados))})")

    print(f"Generado {salida}")


if __name__ == "__main__":
    main()
