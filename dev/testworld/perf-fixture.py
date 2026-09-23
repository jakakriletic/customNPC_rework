#!/usr/bin/env python3
"""Iz referencnih fixtur M0.6 naredi fixture NPC-je scenarija M2.4 (merilne obremenitve).

Zakaj generator in ne rocno urejanje: clone JSON ima ~350 vrstic in pet NPC-jev se od
predloge razlikuje v nekaj poljih. Rocna kopija bi tiho zanesla razliko, ki je nihce ne
opazi, meritev pa bi merila drug NPC, kot pise v scenariju. Tu je vsaka razlika od predloge
zapisana, vsaka zamenjava pa mora zadeti natanko enkrat - sicer generator pade.

Predloge (nespremenjene): T_Stand (mirujoc, brez skripte) in T_Scripted (skriptiran).

    PERF_Idle      T_Stand     mirujoc, frakcija 0                         obremenitev idle
    PERF_BojA      T_Stand     frakcija 1, napada druge frakcije, 100000 HP  obremenitev boj
    PERF_BojB      T_Stand     frakcija 2, napada druge frakcije, 100000 HP  obremenitev boj
    PERF_Skripte   T_Scripted  skripta perf-skripte.js                     obremenitev skripte
    PERF_Kontrola  T_Scripted  skripta perf-kontrola.js                    krmilnik, ni merjen

Zakaj 100000 HP, udarec 1 in Invulnerable 0b: boj mora trajati ves cas meritve in mora
deliti skodo. Ce bi NPC-ji umirali, bi se
stevilo NPC-jev med meritvijo spreminjalo in celica ne bi merila N NPC-jev.

Uporaba (iz korena mape):
    python3 dev/testworld/perf-fixture.py
"""
import importlib.util
import io
import os
import re
import sys
sys.dont_write_bytecode = True  # brez __pycache__ v dev/testworld

TU = os.path.dirname(os.path.abspath(__file__))
KLONI = os.path.join(TU, 'customnpcs', 'clones', '1')

spec = importlib.util.spec_from_file_location('vstavi', os.path.join(TU, 'vstavi-skripto.py'))
vstavi = importlib.util.module_from_spec(spec)
spec.loader.exec_module(vstavi)

HP = 100000

BOJ = [
    ('"AttackOtherFactions": 0b,', '"AttackOtherFactions": 1b,'),
    ('"MaxHealth": 20,', '"MaxHealth": %d,' % HP),
    ('"Health": 20.0f,', '"Health": %d.0f,' % HP),
    ('"Base": 20.0d,\n            "Name": "generic.maxHealth"',
     '"Base": %d.0d,\n            "Name": "generic.maxHealth"' % HP),
    ('"AttackStrenght": 5,', '"AttackStrenght": 1,'),
    # Predloge M0.6 so Invulnerable (README v dev/testworld): udarec bi odbilo
    # Entity.isEntityInvulnerable, boj bi bil samo zamah brez skode in merilo P5
    # (ranjenih > 0) bi padlo. Ranljivost + 100000 HP = boj brez smrti.
    ('"Invulnerable": 1b,', '"Invulnerable": 0b,'),
]

NPCJI = [
    ('PERF_Idle', 'T_Stand', 'M2.4 obremenitev: idle', [], None),
    ('PERF_BojA', 'T_Stand', 'M2.4 obremenitev: boj, frakcija 1',
     BOJ + [('"FactionID": 0,', '"FactionID": 1,')], None),
    ('PERF_BojB', 'T_Stand', 'M2.4 obremenitev: boj, frakcija 2',
     BOJ + [('"FactionID": 0,', '"FactionID": 2,')], None),
    ('PERF_Skripte', 'T_Scripted', 'M2.4 obremenitev: skripte', [], 'perf-skripte.js'),
    ('PERF_Kontrola', 'T_Scripted', 'M2.4 krmilnik (ni merjen)', [], 'perf-kontrola.js'),
]

SKRIPTA = re.compile(r'("Script": ")(.*?)(",\n)', re.S)


def zamenjaj(besedilo, staro, novo, ime):
    n = besedilo.count(staro)
    if n != 1:
        raise SystemExit('NAPAKA %s: "%s" se v predlogi pojavi %d-krat, pricakovano 1'
                         % (ime, staro.split('\n')[0], n))
    return besedilo.replace(staro, novo)


def main():
    for ime, predloga, naslov, zamenjave, js in NPCJI:
        raw = io.open(os.path.join(KLONI, predloga + '.json'), encoding='utf-8').read()
        oznaka = predloga.lower()
        raw = zamenjaj(raw, '"Name": "%s",' % predloga, '"Name": "%s",' % ime, ime)
        raw = zamenjaj(raw, '        "%s"\n' % oznaka,
                       '        "m24",\n        "%s"\n' % ime.lower(), ime)
        raw = re.sub(r'"Title": "[^"]*",', '"Title": "%s",' % naslov, raw, count=1)
        for staro, novo in zamenjave:
            raw = zamenjaj(raw, staro, novo, ime)
        if js is not None:
            src = io.open(os.path.join(TU, js), encoding='utf-8').read()
            mini = vstavi.minify(src)
            if not SKRIPTA.search(raw):
                raise SystemExit('NAPAKA %s: predloga nima polja "Script"' % ime)
            esc = mini.replace('\\', '\\\\').replace('"', '\\"')
            raw = SKRIPTA.sub(lambda m: m.group(1) + esc + m.group(3), raw, count=1)
            nazaj = SKRIPTA.search(raw).group(2).replace('\\"', '"').replace('\\\\', '\\')
            if vstavi.normalize(nazaj) != vstavi.normalize(src):
                raise SystemExit('NAPAKA %s: vstavljena skripta se ne ujema z virom' % ime)
        pot = os.path.join(KLONI, ime + '.json')
        io.open(pot, 'w', encoding='utf-8', newline='\n').write(raw)
        print('OK: %s <- %s (%d zamenjav%s)'
              % (ime, predloga, len(zamenjave) + 3, ', skripta ' + js if js else ''))
    return 0


if __name__ == '__main__':
    sys.exit(main())
