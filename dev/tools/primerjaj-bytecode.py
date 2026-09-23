#!/usr/bin/env python3
"""Primerja prevedene razrede z originalnim bytecode (M3.1, prenos brez funkcionalnih sprememb).

Za vsak razred iz originalnega JAR-a primerja metode po normaliziranem `javap -c`:
indeksi konstant, cilji skokov in stevilke lokalnih spremenljivk se ne stejejo. Metode,
ki se razlikujejo, razvrsti v dve skupini:

  oblika     isti multiset 'pomenskih' ukazov (klici, polja, konstante, aritmetika);
             razlika je v razporedu vej, st. lokalnih spremenljivk, verizni dodelitvi
             (a = b = x) - to je razlika prevajalnika, ne obnasanja
  PREGLED    razlicen multiset - rocni pregled je obvezen, izid gre v audit/

Uporaba:
    python3 primerjaj-bytecode.py <orig-dir|jar> <nov-dir> <razred> [<razred> ...]
    python3 primerjaj-bytecode.py orig out noppes.npcs.ai.CombatHandler ...
Potrebuje `javap` (JDK 8+) na PATH.
"""
import collections
import re
import subprocess
import sys

KEEP = re.compile(r'^(invoke\w+|getfield|putfield|getstatic|putstatic|new|anewarray|newarray|instanceof|ldc\w*|'
                  r'[bs]ipush|[ifld]const_\w+|aconst_null|[ifld](add|sub|mul|div|rem|neg|shl|shr|ushr|and|or|xor)|'
                  r'[ifld]2[ifldbcs]|athrow|arraylength|[ifldabcs]aload|[ifldabcs]astore|iinc|[fd]cmp[lg]|lcmp|monitor\w+)')


def metode(cp, cls):
    out = subprocess.run(['javap', '-c', '-p', '-cp', cp, cls], capture_output=True, text=True).stdout
    m, cur = {}, None
    for line in out.split('\n'):
        h = re.match(r'^  (\S.*?);?$', line)
        if h and not line.startswith('    '):
            cur = h.group(1)
            m[cur] = []
            continue
        if cur and re.match(r'^\s+\d+: ', line):
            i = re.sub(r'^\s+\d+: ', '', line)
            i = re.sub(r'#\d+(, *\d+)?\s*', '', i)
            i = re.sub(r'\b(goto|if\w*|jsr)\s+\d+', r'\1 L', i)
            i = re.sub(r'(tableswitch|lookupswitch).*', r'\1', i)
            m[cur].append(i.strip())
    return m


def pomen(ins):
    c = collections.Counter()
    for i in ins:
        if not KEEP.match(i):
            continue
        i = i.replace('invokeinterface', 'invokevirtual')
        i = re.sub(r'^ldc\w*\s+', 'ldc ', i)
        i = re.sub(r'^iinc\s+\d+,\s*', 'iinc ', i)
        c[i] += 1
    return c


def main(argv):
    if len(argv) < 4:
        print(__doc__)
        return 2
    orig, nov, razredi = argv[1], argv[2], argv[3:]
    vse = enake = oblika = 0
    pregled = []
    for cls in razredi:
        a, b = metode(orig, cls), metode(nov, cls)
        for m in sorted(set(a) | set(b)):
            vse += 1
            if a.get(m) == b.get(m):
                enake += 1
            elif pomen(a.get(m, [])) == pomen(b.get(m, [])):
                oblika += 1
            else:
                x, y = pomen(a.get(m, [])), pomen(b.get(m, []))
                pregled.append((cls, m, dict(x - y), dict(y - x)))
    print('razredov %d, metod %d: enakih %d, razlika v obliki %d, za PREGLED %d'
          % (len(razredi), vse, enake, oblika, len(pregled)))
    for cls, m, minus, plus in pregled:
        print('PREGLED %s :: %s' % (cls, m))
        print('    samo original: %s' % minus)
        print('    samo nov:      %s' % plus)
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv))
