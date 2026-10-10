# M2.5c - ponovitve scenarija perf (M2.4), 2026-10-10 15:24

Protokol: `docs/scenariji/M2.5c-ponovitve.md`. Vsaka ponovitev je svoj proces in
svez svet (reset z .\testworld.ps1).

Merila: T1-T6 zelena

## Zagoni

| # | izhodna koda | minut | zapis |
|---|---|---|---|
| 1 | 0 | 5,8 | `m24-perf-2026-10-10-1507-p1.json` |
| 2 | 0 | 5,7 | `m24-perf-2026-10-10-1507-p2.json` |
| 3 | 0 | 5,8 | `m24-perf-2026-10-10-1507-p3.json` |

## Odtis (mora biti enak v vseh ponovitvah)

```
celic                1
celica               1
mrezaSirina          25
mrezaX               48
mrezaZ               -12
npc                  500
obroc                1
ogrevanje            90
razprseno            0
rwpath               0
rwpathMemo           0
rwtarget             2
sekund               180
varianta             boj
```

## Sumni pas

Prag: relativni razpon <= 20 %. Velicina, oznacena kot SUMNA, se med ponovitvami
premakne bolj kot prag, zato A/B primerjava na njej potrebuje razliko, vecjo od stolpca
`razpon` - manjsa razlika je sum, ne izboljsava.

```
velicina                                  p1        p2        p3   mediana    razpon      rel  oznaka
-----------------------------------------------------------------------------------------------------
alok.KBnaTick                          3.792     4.645     4.549     4.549   852,800   18,7 %  stabilna
alok.MBnaS                                74    90,700    88,800    88,800    16,700   18,8 %  stabilna
boj.ranjenih                             500       500       500       500         0    0,0 %  enaka
boj.sCiljem                              500       500       500       500         0    0,0 %  enaka
gc.ms                                     69        80        78        78        11   14,1 %  stabilna
gc.msNaS                               0,380     0,440     0,430     0,430     0,060   14,0 %  stabilna
gc.old.ms                                  0         0         0         0         0      n/a  enaka
gc.old.zbirk                               0         0         0         0         0      n/a  enaka
gc.zbirk                                  24        29        28        28         5   17,9 %  stabilna
heap.maxMB                             1.879     1.820     1.820     1.820        59    3,2 %  stabilna
mspt.brezSave.max                     13,173    13,898    14,308    13,898     1,135    8,2 %  stabilna
mspt.brezSave.p99                      9,699    11,534    11,534    11,534     1,835   15,9 %  stabilna
mspt.max                              30,011    37,548    36,673    36,673     7,537   20,6 %  SUMNA
mspt.p50                               7,733     9,175     9,175     9,175     1,442   15,7 %  stabilna
mspt.p95                               9,175    10,486    10,486    10,486     1,311   12,5 %  stabilna
mspt.p99                               9,699    11,534    11,534    11,534     1,835   15,9 %  stabilna
mspt.povp                              7,819     9,179     9,064     9,064     1,360   15,0 %  stabilna
nasiceno                                   0         0         0         0         0      n/a  enaka
npc.per.tick.p50                         500       500       500       500         0    0,0 %  enaka
npc.us                                15,460    18,160    17,950    17,950     2,700   15,0 %  stabilna
rwpath.izracunov                           0         0         0         0         0      n/a  enaka
rwpath.klicev                              0         0         0         0         0      n/a  enaka
rwpath.memo.iskanj                         0         0         0         0         0      n/a  enaka
rwpath.memo.izracunov                      0         0         0         0         0      n/a  enaka
rwpath.memo.neujemanj                      0         0         0         0         0      n/a  enaka
rwpath.memo.ocen                           0         0         0         0         0      n/a  enaka
rwpath.neujemanj                           0         0         0         0         0      n/a  enaka
rwpath.ocen                                0         0         0         0         0      n/a  enaka
rwpath.primerjav                           0         0         0         0         0      n/a  enaka
rwtarget.predzavrnjenih                6.087     2.841     3.988     3.988     3.246   81,4 %  SUMNA
rwtarget.zozenih                           0         0         0         0         0      n/a  enaka
ticki                                  3.629     3.630     3.630     3.630         1    0,0 %  stabilna
ticki.nad100ms                             0         0         0         0         0      n/a  enaka
ticki.nad10ms                             32       373       305       305       341  111,8 %  SUMNA
ticki.nad25ms                              4         4         4         4         0    0,0 %  enaka
ticki.nad50ms                              0         0         0         0         0      n/a  enaka
tps                                   19,990    19,990    19,990    19,990         0    0,0 %  enaka
```

### Sumne velicine

- `mspt.max`: mediana 36,673, razpon 7,537 (20,6 %)
- `rwtarget.predzavrnjenih`: mediana 3.988, razpon 3.246 (81,4 %)
- `ticki.nad10ms`: mediana 305, razpon 341 (111,8 %)
