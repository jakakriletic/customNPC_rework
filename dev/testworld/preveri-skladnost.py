#!/usr/bin/env python3
"""Preveri, da si koordinate scenarija M2.3 ne nasprotujejo med tremi datotekami.

Scenarij zivi v treh datotekah hkrati: konstante v `r2-control.js`, postavitev sveta v
`r2-setup-commands.txt` in preverbe v `r2-run.ps1`. Ce kdo spremeni eno in ne drugih, se
to v svetu ne pokaze kot napaka, ampak kot cudno obnasanje NPC-jev - torej kot okvara
moda. To orodje tak razhod ujame v sekundi in brez Minecrafta.

Preveri:
  - da so proge polne (6/6/6) in cilji trije,
  - da se start, cilj in visina ujemajo s konstantami v skripti,
  - da sta progi F in W res za zidom, proga P pa ne,
  - da je obhod okoli konca zidu daljsi od NpcNavRange (32) - sicer zid ni ovira,
  - da je pot cez zid krajsa od 32 - sicer izid ni pripisljiv oviri, ampak dometu,
  - da `r2-run.ps1` pricakuje toliko NPC-jev, kot jih setup spawna.

Uporaba (iz korena mape):
    python3 dev/testworld/preveri-skladnost.py
"""
import io
import os
import re
import sys

NAV_RANGE = 32   # CustomNpcs.NpcNavRange; PathFinder.findPath maxDistance


def preberi(pot):
    return io.open(pot, encoding='utf-8').read()


def main():
    here = os.path.dirname(os.path.abspath(__file__))
    root = os.path.dirname(os.path.dirname(here))
    js = preberi(os.path.join(here, 'r2-control.js'))
    cmd = preberi(os.path.join(here, 'r2-setup-commands.txt'))
    ps = preberi(os.path.join(root, 'r2-run.ps1'))

    def const(name):
        m = re.search(r'var %s = (-?[\d.]+);' % name, js)
        if not m:
            raise SystemExit('konstante %s ni v r2-control.js' % name)
        return int(float(m.group(1)))

    C = dict((n, const(n)) for n in
             ['START_Z', 'WALL_Z', 'GOAL_Z', 'GOAL_Y', 'GOAL_X_F', 'GOAL_X_W', 'GOAL_X_P'])
    napake = []

    spawns = re.findall(r'noppes clone spawn (R2_\w+) 1 (-?\d+),(-?\d+),(-?\d+)', cmd)
    flyers = [(int(x), int(y), int(z)) for n, x, y, z in spawns if n == 'R2_Flyer']
    walkers = [(int(x), int(y), int(z)) for n, x, y, z in spawns if n == 'R2_Walker']
    targets = [(int(x), int(y), int(z)) for n, x, y, z in spawns if n == 'R2_Target']

    # ista delitev kot v skripti: leteci z x < 0 je proga F, ostali proga P
    laneF = [p for p in flyers if p[0] < 0]
    laneP = [p for p in flyers if p[0] >= 0]

    for ime, lane in (('F', laneF), ('W', walkers), ('P', laneP)):
        if len(lane) != 6:
            napake.append('proga %s ima %d NPC, pricakovano 6' % (ime, len(lane)))
        for x, y, z in lane:
            if z != C['START_Z']:
                napake.append('proga %s: NPC pri x=%d ima z=%d, START_Z je %d' % (ime, x, z, C['START_Z']))
            if y != C['GOAL_Y']:
                napake.append('proga %s: NPC pri x=%d ima y=%d, GOAL_Y je %d' % (ime, x, y, C['GOAL_Y']))

    if len(targets) != 3:
        napake.append('ciljev je %d, pricakovano 3' % len(targets))
    razvrstitev = (('F', [t for t in targets if t[0] < -20], C['GOAL_X_F']),
                   ('W', [t for t in targets if -20 <= t[0] < 10], C['GOAL_X_W']),
                   ('P', [t for t in targets if t[0] >= 10], C['GOAL_X_P']))
    for ime, lst, gx in razvrstitev:
        if len(lst) != 1:
            napake.append('proga %s: ciljev %d, pricakovano 1' % (ime, len(lst)))
            continue
        x, y, z = lst[0]
        if x != gx:
            napake.append('proga %s: cilj pri x=%d, GOAL_X_%s je %d' % (ime, x, ime, gx))
        if z != C['GOAL_Z']:
            napake.append('proga %s: cilj pri z=%d, GOAL_Z je %d' % (ime, z, C['GOAL_Z']))
        if y != C['GOAL_Y']:
            napake.append('proga %s: cilj pri y=%d, GOAL_Y je %d' % (ime, y, C['GOAL_Y']))

    visina = cez = 0
    m = re.search(r'fill (-?\d+) (\d+) (-?\d+) (-?\d+) (\d+) (-?\d+) minecraft:stone', cmd)
    if not m:
        napake.append('zid (fill ... minecraft:stone) ni najden v r2-setup-commands.txt')
    else:
        x1, y1, z1, x2, y2, z2 = [int(v) for v in m.groups()]
        if z1 != C['WALL_Z'] or z2 != C['WALL_Z']:
            napake.append('zid je pri z=%d..%d, WALL_Z je %d' % (z1, z2, C['WALL_Z']))
        visina = y2 - y1 + 1
        if visina < 3:
            napake.append('zid je visok %d; pesec prestopi 1 blok, ovira mora biti visja' % visina)
        for ime, lane, zaZidom in (('F', laneF, True), ('W', walkers, True), ('P', laneP, False)):
            pokrito = all(x1 <= x <= x2 for x, _, _ in lane)
            if pokrito != zaZidom:
                napake.append('proga %s: zid jo pokriva=%s, pricakovano %s' % (ime, pokrito, zaZidom))
        for ime, lane in (('F', laneF), ('W', walkers)):
            for x, _, _ in lane:
                obhod = min(abs(x - x1), abs(x - x2)) * 2 + abs(C['START_Z'] - C['GOAL_Z'])
                if obhod <= NAV_RANGE:
                    napake.append('proga %s: NPC pri x=%d obide zid v %d blokih, kar je znotraj NpcNavRange %d'
                                  % (ime, x, obhod, NAV_RANGE))
        cez = abs(C['START_Z'] - C['GOAL_Z']) + 2 * visina
        if cez >= NAV_RANGE:
            napake.append('pot cez zid je dolga %d blokov; pri NpcNavRange %d izid ne bo pripisljiv oviri'
                          % (cez, NAV_RANGE))

    m2 = re.search(r'v svetu je (\d+) R2 NPC-jev', ps)
    if not m2:
        napake.append('r2-run.ps1 ne preverja stevila NPC-jev')
    elif int(m2.group(1)) != len(spawns):
        napake.append('r2-run.ps1 pricakuje %s NPC-jev, setup jih spawna %d' % (m2.group(1), len(spawns)))

    print('konstante: %s' % C)
    print('proge: F=%d W=%d P=%d cilji=%d skupaj=%d'
          % (len(laneF), len(walkers), len(laneP), len(targets), len(spawns)))
    if visina:
        print('zid: visok %d, pot cez zid ~%d blokov (NpcNavRange %d)' % (visina, cez, NAV_RANGE))
    if napake:
        for n in napake:
            print('NAPAKA: %s' % n)
        return 1
    print('SKLADNOST SCENARIJA OK')
    return 0


if __name__ == '__main__':
    sys.exit(main())
