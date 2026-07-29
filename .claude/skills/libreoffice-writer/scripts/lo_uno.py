#!/usr/bin/env python3
"""Drive a headless LibreOffice through UNO for things the file format alone
cannot do: fill in a table of contents, refresh fields, repaginate, export.

  python3 lo_uno.py refresh report.odt              # update TOC + fields in place
  python3 lo_uno.py refresh report.odt -o final.odt
  python3 lo_uno.py pdf report.odt -o report.pdf    # refresh, then export PDF

A soffice listener is started automatically and reused if already running.
"""
import argparse
import os
import subprocess
import sys
import time

PORT = 2002
SOFFICE = os.environ.get("SOFFICE_BIN", "soffice")


def connect(timeout=90):
    sys.path.append("/usr/lib/libreoffice/program")
    import uno  # noqa: E402
    from com.sun.star.connection import NoConnectException  # noqa: E402

    local = uno.getComponentContext()
    resolver = local.ServiceManager.createInstanceWithContext(
        "com.sun.star.bridge.UnoUrlResolver", local)
    url = ("uno:socket,host=127.0.0.1,port=%d;urp;"
           "StarOffice.ComponentContext" % PORT)
    try:
        return uno, resolver.resolve(url)
    except NoConnectException:
        pass

    subprocess.Popen(
        [SOFFICE, "--headless", "--invisible", "--nologo", "--nodefault",
         "--norestore", "--nolockcheck",
         "--accept=socket,host=127.0.0.1,port=%d;urp;" % PORT],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            return uno, resolver.resolve(url)
        except NoConnectException:
            time.sleep(0.5)
    raise SystemExit("could not start a LibreOffice listener on port %d" % PORT)


def prop(uno, name, value):
    p = uno.createUnoStruct("com.sun.star.beans.PropertyValue")
    p.Name, p.Value = name, value
    return p


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("action", choices=["refresh", "pdf"])
    ap.add_argument("path")
    ap.add_argument("-o", "--output")
    args = ap.parse_args()

    src = os.path.abspath(args.path)
    uno, ctx = connect()
    desktop = ctx.ServiceManager.createInstanceWithContext(
        "com.sun.star.frame.Desktop", ctx)

    doc = desktop.loadComponentFromURL(
        uno.systemPathToFileUrl(src), "_blank", 0, (prop(uno, "Hidden", True),))
    if doc is None:
        raise SystemExit("LibreOffice could not open %s" % src)

    try:
        # Order matters: fields first, then indexes (a TOC entry can contain a
        # field), then refresh again so page numbers settle after repagination.
        if hasattr(doc, "getTextFields"):
            doc.getTextFields().refresh()
        if hasattr(doc, "refresh"):
            doc.refresh()
        if hasattr(doc, "getDocumentIndexes"):
            idx = doc.getDocumentIndexes()
            for i in range(idx.getCount()):
                idx.getByIndex(i).update()   # the container has no refresh()
        if hasattr(doc, "getTextFields"):
            doc.getTextFields().refresh()

        if args.action == "pdf":
            out = args.output or os.path.splitext(src)[0] + ".pdf"
            doc.storeToURL(uno.systemPathToFileUrl(os.path.abspath(out)),
                           (prop(uno, "FilterName", "writer_pdf_Export"),))
        else:
            out = os.path.abspath(args.output) if args.output else src
            if args.output:
                doc.storeToURL(uno.systemPathToFileUrl(out),
                               (prop(uno, "FilterName", "writer8"),))
            else:
                doc.store()
        print("wrote", out)
    finally:
        doc.close(False)


if __name__ == "__main__":
    main()
