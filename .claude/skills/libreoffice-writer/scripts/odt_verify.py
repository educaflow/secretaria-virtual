#!/usr/bin/env python3
"""Render a document to JPEGs so the agent can actually look at what it built.

  python3 odt_verify.py report.odt            -> verify_out/page-1.jpg ...
  python3 odt_verify.py report.odt -r 150 -p 1-3
"""
import argparse
import glob
import os
import shutil
import subprocess
import sys
import tempfile

ap = argparse.ArgumentParser()
ap.add_argument("path")
ap.add_argument("-o", "--outdir", default="verify_out")
ap.add_argument("-r", "--dpi", type=int, default=100)
ap.add_argument("-p", "--pages", help="e.g. 1-3 or 2")
args = ap.parse_args()

src = os.path.abspath(args.path)
if not os.path.exists(src):
    sys.exit("no such file: " + src)
os.makedirs(args.outdir, exist_ok=True)

tmp = tempfile.mkdtemp()
try:
    subprocess.run(
        ["soffice", "--headless", "--nologo", "--norestore",
         "--convert-to", "pdf", src, "--outdir", tmp],
        check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
        timeout=300)
    pdfs = glob.glob(os.path.join(tmp, "*.pdf"))
    if not pdfs:
        sys.exit("LibreOffice produced no PDF — the file is probably malformed")
    cmd = ["pdftoppm", "-jpeg", "-r", str(args.dpi)]
    if args.pages:
        first, _, last = args.pages.partition("-")
        cmd += ["-f", first, "-l", last or first]
    cmd += [pdfs[0], os.path.join(args.outdir, "page")]
    subprocess.run(cmd, check=True)
finally:
    shutil.rmtree(tmp, ignore_errors=True)

pages = sorted(glob.glob(os.path.join(args.outdir, "page*.jpg")))
print("\n".join(pages) or "no pages rendered")
print("\n%d page(s). Read these images before telling the user it is done."
      % len(pages))
