#!/usr/bin/env python3
"""Vstavi .js skripto v polje "Script" clone JSON datoteke CustomNPCs.

Polje Script je en sam niz, zato gre skripta noter minificirana. Rocno urejanje te
vrstice v JSON-u je pot do tihe napake (docs/scenariji/M2.2-R1.md), zato to dela to
orodje. Po vstavljanju samo preveri, da sta vir in vstavljena razlicica po
normalizaciji identicna, in to izpise.

Uporaba:
    python3 dev/testworld/vstavi-skripto.py <skripta.js> <clone.json>
"""
import io
import re
import sys


def minify(src):
    """ES5 minifikacija, dovolj za Nashorn: stran komentarji, ena vrstica, enojni narekovaji.

    Zavedati se je treba, da to ni razclenjevalnik. Skripte v tem projektu ne smejo
    vsebovati // ali dvojnega narekovaja znotraj niza; preverba na koncu to ujame.
    """
    out = re.sub(r'//[^\n]*', '', src)
    out = out.replace('"', "'")
    out = re.sub(r'\s+', ' ', out).strip()
    return out


def normalize(src):
    out = re.sub(r'//[^\n]*', '', src)
    out = out.replace("'", '"')
    return re.sub(r'\s+', ' ', out).strip()


def main():
    if len(sys.argv) != 3:
        print(__doc__)
        return 2
    js_path, json_path = sys.argv[1], sys.argv[2]
    src = io.open(js_path, encoding='utf-8').read()
    mini = minify(src)

    raw = io.open(json_path, encoding='utf-8').read()
    pattern = re.compile(r'("Script": ")(.*?)(",\n)', re.S)
    if not pattern.search(raw):
        print('NAPAKA: v %s ni polja "Script"' % json_path)
        return 1
    escaped = mini.replace('\\', '\\\\').replace('"', '\\"')
    new_raw = pattern.sub(lambda m: m.group(1) + escaped + m.group(3), raw, count=1)
    io.open(json_path, 'w', encoding='utf-8', newline='\n').write(new_raw)

    # preverba: kar je zdaj v JSON-u, mora biti po normalizaciji enako viru
    check = pattern.search(io.open(json_path, encoding='utf-8').read()).group(2)
    check = check.replace('\\"', '"').replace('\\\\', '\\')
    if normalize(check) != normalize(src):
        print('NAPAKA: vstavljena skripta se po normalizaciji ne ujema z virom')
        return 1
    print('OK: %s -> %s (%d znakov, ujemanje z virom preverjeno)'
          % (js_path, json_path, len(mini)))
    return 0


if __name__ == '__main__':
    sys.exit(main())
