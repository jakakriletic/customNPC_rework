# M2.5c - ponovitve scenarija perf (M2.4), 2026-10-09 22:32

Protokol: `docs/scenariji/M2.5c-ponovitve.md`. Vsaka ponovitev je svoj proces in
svez svet (reset z .\testworld.ps1).

Merila: T1-T6 zelena

## Zagoni

| # | izhodna koda | minut | zapis |
|---|---|---|---|
| 1 | 0 | 8,2 | `m24-perf-2026-10-09-2207-p1.json` |
| 2 | 0 | 8,2 | `m24-perf-2026-10-09-2207-p2.json` |
| 3 | 0 | 8,2 | `m24-perf-2026-10-09-2207-p3.json` |

## Odtis (mora biti enak v vseh ponovitvah)

```
celic                1
celica               1
mrezaSirina          25
mrezaX               48
mrezaZ               -12
npc                  500
obroc                1
ogrevanje            120
razprseno            0
rwcollide            0
rwtarget             2
sekund               300
varianta             idle
```

## Sumni pas

Prag: relativni razpon <= 20 %. Velicina, oznacena kot SUMNA, se med ponovitvami
premakne bolj kot prag, zato A/B primerjava na njej potrebuje razliko, vecjo od stolpca
`razpon` - manjsa razlika je sum, ne izboljsava.

```
velicina                                  p1        p2        p3   mediana    razpon      rel  oznaka
-----------------------------------------------------------------------------------------------------
alok.KBnaTick                          1.059     1.059     1.325     1.059   266,100   25,1 %  SUMNA
alok.MBnaS                            20,700    20,700    25,900    20,700     5,200   25,1 %  SUMNA
gc.ms                                     33        33        46        33        13   39,4 %  SUMNA
gc.msNaS                               0,110     0,110     0,150     0,110     0,040   36,4 %  SUMNA
gc.old.ms                                  0         0         0         0         0      n/a  enaka
gc.old.zbirk                               0         0         0         0         0      n/a  enaka
gc.zbirk                                  12        12        16        12         4   33,3 %  SUMNA
heap.maxMB                             1.820     1.820     1.820     1.820         0    0,0 %  enaka
mspt.brezSave.max                      9,289    11,392    12,768    11,392     3,479   30,5 %  SUMNA
mspt.brezSave.p99                      5,505     6,029     6,685     6,029     1,180   19,6 %  stabilna
mspt.max                              23,145    24,614    25,529    24,614     2,384    9,7 %  stabilna
mspt.p50                               2,949     3,342     3,604     3,342     0,655   19,6 %  stabilna
mspt.p95                               4,850     5,243     5,767     5,243     0,917   17,5 %  stabilna
mspt.p99                               5,505     6,160     6,816     6,160     1,311   21,3 %  SUMNA
mspt.povp                              3,139     3,526     3,861     3,526     0,722   20,5 %  SUMNA
nasiceno                                   0         0         0         0         0      n/a  enaka
npc.per.tick.p50                         500       500       500       500         0    0,0 %  enaka
npc.us                                 6,110     6,850     7,500     6,850     1,390   20,3 %  SUMNA
rwcollide.brezOpazovalca                   0         0         0         0         0      n/a  enaka
rwcollide.dogodkovBrezOpazovalca           0         0         0         0         0      n/a  enaka
rwcollide.klicev                           0         0         0         0         0      n/a  enaka
rwcollide.preskocenih                      0         0         0         0         0      n/a  enaka
rwtarget.predzavrnjenih                    0         0         0         0         0      n/a  enaka
rwtarget.zozenih                     255.014   254.279   255.011   255.011       735    0,3 %  stabilna
ticki                                  6.035     6.048     6.052     6.048        17    0,3 %  stabilna
ticki.nad100ms                             0         0         0         0         0      n/a  enaka
ticki.nad10ms                              7         9        10         9         3   33,3 %  SUMNA
ticki.nad25ms                              0         0         3         0         3      n/a  SUMNA
ticki.nad50ms                              0         0         0         0         0      n/a  enaka
tps                                       20        20        20        20         0    0,0 %  enaka
```

### Sumne velicine

- `alok.KBnaTick`: mediana 1.059, razpon 266,100 (25,1 %)
- `alok.MBnaS`: mediana 20,700, razpon 5,200 (25,1 %)
- `gc.ms`: mediana 33, razpon 13 (39,4 %)
- `gc.msNaS`: mediana 0,110, razpon 0,040 (36,4 %)
- `gc.zbirk`: mediana 12, razpon 4 (33,3 %)
- `mspt.brezSave.max`: mediana 11,392, razpon 3,479 (30,5 %)
- `mspt.p99`: mediana 6,160, razpon 1,311 (21,3 %)
- `mspt.povp`: mediana 3,526, razpon 0,722 (20,5 %)
- `npc.us`: mediana 6,850, razpon 1,390 (20,3 %)
- `ticki.nad10ms`: mediana 9, razpon 3 (33,3 %)
- `ticki.nad25ms`: mediana 0, razpon 3 (mediana je 0, razmerje ne obstaja)
