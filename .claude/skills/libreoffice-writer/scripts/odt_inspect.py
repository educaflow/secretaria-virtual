#!/usr/bin/env python3
"""Dump the skeleton of an existing .odt/.ott: which styles exist, what they
define, and the outline of the body. Run this before editing someone else's
document or before reusing a corporate template.

  python3 odt_inspect.py template.ott
  python3 odt_inspect.py doc.odt --styles          # style definitions only
  python3 odt_inspect.py doc.odt --xml styles.xml  # raw pretty-printed XML
"""
import argparse
import sys
import zipfile

from lxml import etree

NS = {
    "office": "urn:oasis:names:tc:opendocument:xmlns:office:1.0",
    "style": "urn:oasis:names:tc:opendocument:xmlns:style:1.0",
    "text": "urn:oasis:names:tc:opendocument:xmlns:text:1.0",
    "table": "urn:oasis:names:tc:opendocument:xmlns:table:1.0",
    "fo": "urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0",
    "draw": "urn:oasis:names:tc:opendocument:xmlns:drawing:1.0",
}
Q = lambda p, t: "{%s}%s" % (NS[p], t)  # noqa: E731

ap = argparse.ArgumentParser()
ap.add_argument("path")
ap.add_argument("--styles", action="store_true")
ap.add_argument("--xml", metavar="MEMBER")
args = ap.parse_args()

z = zipfile.ZipFile(args.path)

if args.xml:
    root = etree.fromstring(z.read(args.xml))
    sys.stdout.write(etree.tostring(root, pretty_print=True).decode())
    raise SystemExit

print("== members ==")
for n in z.namelist():
    print("  ", n)


def describe(style):
    fam = style.get(Q("style", "family"))
    name = style.get(Q("style", "name"))
    parent = style.get(Q("style", "parent-style-name"))
    bits = []
    for child in style:
        for k, v in child.attrib.items():
            bits.append("%s=%s" % (etree.QName(k).localname, v))
    head = "  %-28s %-14s" % (name, fam)
    if parent:
        head += " <- %s" % parent
    print(head)
    if bits:
        print("      " + "  ".join(bits[:14]))


for member in ("styles.xml", "content.xml"):
    root = etree.fromstring(z.read(member))
    for section, label in (("styles", "named styles"),
                           ("automatic-styles", "automatic styles"),
                           ("master-styles", "master pages")):
        node = root.find(Q("office", section))
        if node is None or len(node) == 0:
            continue
        print("\n== %s :: %s ==" % (member, label))
        for el in node:
            tag = etree.QName(el).localname
            if tag in ("style", "page-layout", "list-style", "master-page"):
                if tag == "style":
                    describe(el)
                else:
                    print("  %-28s [%s]" % (el.get(Q("style", "name")), tag))

if args.styles:
    raise SystemExit

root = etree.fromstring(z.read("content.xml"))
body = root.find(Q("office", "body"))
print("\n== body outline ==")
if body is not None:
    for el in body.iter():
        tag = etree.QName(el).localname
        if tag == "h":
            lvl = int(el.get(Q("text", "outline-level"), "1"))
            print("  " + "  " * (lvl - 1) + "H%d: %s"
                  % (lvl, "".join(el.itertext())[:70]))
        elif tag == "table":
            cols = sum(int(c.get(Q("table", "number-columns-repeated"), "1"))
                       for c in el.findall(Q("table", "table-column")))
            rows = len(el.findall(".//" + Q("table", "table-row")))
            print("      [table %s: %d cols x %d rows, style=%s]"
                  % (el.get(Q("table", "name")), cols, rows,
                     el.get(Q("table", "style-name"))))
        elif tag == "image":
            print("      [image %s]" % el.get("{%s}href" %
                  "http://www.w3.org/1999/xlink"))
