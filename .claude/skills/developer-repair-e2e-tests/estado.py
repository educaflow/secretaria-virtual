#!/usr/bin/env python3
"""Mantiene el fichero de estado de /developer-repair-e2e-tests.

Cada línea es `- [x] <test>` o `- [ ] <test>`, con una nota opcional detrás de ` ⇒ `.
<test> es `<fichero .spec.ts relativo a src/test/e2e> › <describe> › <título>`.

Uso:
  estado.py [--out=<fichero>] sync <list.json>       añade como `[ ]` los tests que falten y elimina los que ya no existen
  estado.py [--out=<fichero>] pendientes             ficheros .spec.ts con algún test sin `[x]`
  estado.py [--out=<fichero>] aplicar <report.json>  vuelca el resultado de una ejecución
  estado.py [--out=<fichero>] marcar <ok|ko> <test> [nota]
  estado.py [--out=<fichero>] resumen

<list.json> y <report.json> son la salida del reporter JSON de Playwright.
"""
import json
import re
import sys
from pathlib import Path

SEP_TITULO = " › "
SEP_NOTA = " ⇒ "
LINEA = re.compile(r"^- \[( |x)\] (.*)$")
CABECERA = (
    "# Reparación de los tests E2E\n"
    "\n"
    "Estado que mantiene `/developer-repair-e2e-tests`: `[x]` el test pasa, `[ ]` no pasa o está sin procesar.\n"
    "Borra este fichero para volver a probar toda la suite.\n"
    "\n"
)


def leer(ruta):
    """Devuelve {test: (pasa, nota)} respetando el orden del fichero."""
    estado = {}
    if not ruta.exists():
        return estado
    for linea in ruta.read_text(encoding="utf-8").splitlines():
        m = LINEA.match(linea)
        if not m:
            continue
        test, _, nota = m.group(2).partition(SEP_NOTA)
        estado[test.strip()] = (m.group(1) == "x", nota.strip())
    return estado


def escribir(ruta, estado):
    lineas = []
    for test, (pasa, nota) in estado.items():
        lineas.append(f"- [{'x' if pasa else ' '}] {test}" + (SEP_NOTA + nota if nota else ""))
    ruta.write_text(CABECERA + "\n".join(lineas) + "\n", encoding="utf-8")


def tests_del_informe(ruta):
    """Devuelve [(test, status)] con status expected | unexpected | flaky | skipped."""
    informe = json.loads(Path(ruta).read_text(encoding="utf-8"))
    resultado = []

    def recorrer(suite, titulos):
        for spec in suite.get("specs", []):
            test = SEP_TITULO.join([spec["file"], *titulos, spec["title"]])
            estados = {t.get("status") for t in spec.get("tests", [])}
            if "unexpected" in estados:
                status = "unexpected"
            elif "flaky" in estados:
                status = "flaky"
            elif "expected" in estados:
                status = "expected"
            else:
                status = "skipped"
            resultado.append((test, status))
        for hija in suite.get("suites", []):
            recorrer(hija, [*titulos, hija["title"]])

    for suite_fichero in informe.get("suites", []):
        recorrer(suite_fichero, [])
    return resultado


def resumen(estado):
    pasan = sum(1 for pasa, _ in estado.values() if pasa)
    return f"TOTAL {len(estado)} · PASAN {pasan} · PENDIENTES {len(estado) - pasan}"


def main(argv):
    ruta = Path("repair-e2e-test.md")
    args = []
    for a in argv:
        if a.startswith("--out="):
            ruta = Path(a[len("--out="):])
        else:
            args.append(a)
    if not args:
        sys.exit(__doc__)
    orden, resto = args[0], args[1:]
    estado = leer(ruta)

    if orden == "sync":
        listado = [test for test, _ in tests_del_informe(resto[0])]
        nuevos = 0
        for test in listado:
            if test not in estado:
                estado[test] = (False, "")
                nuevos += 1
                print(f"AÑADIDO {test}")
        # El listado puede ser parcial: un test que no está en él solo se elimina
        # si su fichero sí está o si ese fichero ya no existe.
        raiz = json.loads(Path(resto[0]).read_text(encoding="utf-8")).get("config", {}).get("rootDir")
        ficheros = {test.split(SEP_TITULO)[0] for test in listado}
        eliminados = 0
        for test in [t for t in estado if t not in listado]:
            fichero = test.split(SEP_TITULO)[0]
            if fichero in ficheros or (raiz and not (Path(raiz) / fichero).exists()):
                del estado[test]
                eliminados += 1
                print(f"ELIMINADO {test}")
        escribir(ruta, estado)
        print(f"AÑADIDOS {nuevos}")
        print(f"ELIMINADOS {eliminados}")
        print(resumen(estado))
    elif orden == "pendientes":
        ficheros = []
        for test, (pasa, _) in estado.items():
            fichero = test.split(SEP_TITULO)[0]
            if not pasa and fichero not in ficheros:
                ficheros.append(fichero)
        print("\n".join(ficheros))
    elif orden == "aplicar":
        for test, status in tests_del_informe(resto[0]):
            pasa, nota = estado.get(test, (False, ""))
            if pasa:
                if status == "unexpected":
                    print(f"REGRESION {test}")
                continue  # un `[x]` no se vuelve a tocar
            if status == "expected":
                estado[test] = (True, "")  # la nota de un fallo anterior ya no aplica
                print(f"PASA {test}")
            elif status == "flaky":
                estado[test] = (True, nota or "pasa al reintentar")
                print(f"PASA {test}")
            elif status == "skipped":
                estado[test] = (False, nota or "omitido por el propio test (skip)")
                print(f"OMITIDO {test}")
            else:
                estado[test] = (False, nota)
                print(f"FALLA {test}")
        escribir(ruta, estado)
        print(resumen(estado))
    elif orden == "marcar":
        marca, test = resto[0], resto[1]
        if marca not in ("ok", "ko"):
            sys.exit("ERROR: la marca es `ok` o `ko`")
        if test not in estado:
            sys.exit(f"ERROR: el test no está en {ruta}: {test}")
        nota = resto[2] if len(resto) > 2 else estado[test][1]
        estado[test] = (marca == "ok", " ".join(nota.split()))
        escribir(ruta, estado)
        print(resumen(estado))
    elif orden == "resumen":
        print(resumen(estado))
    else:
        sys.exit(__doc__)


if __name__ == "__main__":
    main(sys.argv[1:])
