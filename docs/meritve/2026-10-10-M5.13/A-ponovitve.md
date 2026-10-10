# M2.5c - ponovitve scenarija perf (M2.4), 2026-10-10 10:12

Protokol: `docs/scenariji/M2.5c-ponovitve.md`. Vsaka ponovitev je svoj proces in
svez svet (reset z .\testworld.ps1).

Merila: T1-T6 zelena

## Zagoni

| # | izhodna koda | minut | zapis |
|---|---|---|---|
| 1 | 0 | 8,2 | `m24-perf-2026-10-10-0947-p1.json` |
| 2 | 0 | 8,2 | `m24-perf-2026-10-10-0947-p2.json` |
| 3 | 0 | 8,1 | `m24-perf-2026-10-10-0947-p3.json` |

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
rwblink              0
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
alok.KBnaTick                          1.059     1.059     1.083     1.059    23,600    2,2 %  stabilna
alok.MBnaS                            20,700    20,700    21,100    20,700     0,400    1,9 %  stabilna
gc.ms                                     31        30        31        31         1    3,2 %  stabilna
gc.msNaS                               0,100     0,100     0,100     0,100         0    0,0 %  enaka
gc.old.ms                                  0         0         0         0         0      n/a  enaka
gc.old.zbirk                               0         0         0         0         0      n/a  enaka
gc.zbirk                                  12        12        12        12         0    0,0 %  enaka
heap.maxMB                             1.820     1.820     1.820     1.820         0    0,0 %  enaka
mspt.brezSave.max                      7,108     7,128     7,018     7,108     0,110    1,5 %  stabilna
mspt.brezSave.p99                      4,719     4,588     4,719     4,719     0,131    2,8 %  stabilna
mspt.max                              28,077    21,867    26,188    26,188     6,210   23,7 %  SUMNA
mspt.p50                               2,621     2,556     2,687     2,621     0,131    5,0 %  stabilna
mspt.p95                               4,325     4,194     4,325     4,325     0,131    3,0 %  stabilna
mspt.p99                               4,850     4,588     4,719     4,719     0,262    5,6 %  stabilna
mspt.povp                              2,824     2,761     2,915     2,824     0,154    5,5 %  stabilna
nasiceno                                   0         0         0         0         0      n/a  enaka
npc.per.tick.p50                         500       500       500       500         0    0,0 %  enaka
npc.us                                 5,540     5,430     5,730     5,540     0,300    5,4 %  stabilna
rwblink.chunkov                            0         0         0         0         0      n/a  enaka
rwblink.igralcev                           0         0         0         0         0      n/a  enaka
rwblink.iskanj                             0         0         0         0         0      n/a  enaka
rwblink.neujemanj                          0         0         0         0         0      n/a  enaka
rwblink.prejemnikov                        0         0         0         0         0      n/a  enaka
rwblink.primerjav                          0         0         0         0         0      n/a  enaka
rwtarget.predzavrnjenih                    0         0         0         0         0      n/a  enaka
rwtarget.zozenih                     253.981   254.742   252.436   253.981     2.306    0,9 %  stabilna
ticki                                  6.029     6.038     6.018     6.029        20    0,3 %  stabilna
ticki.nad100ms                             0         0         0         0         0      n/a  enaka
ticki.nad10ms                              7         7         7         7         0    0,0 %  enaka
ticki.nad25ms                              2         0         2         2         2  100,0 %  SUMNA
ticki.nad50ms                              0         0         0         0         0      n/a  enaka
tps                                       20        20        20        20         0    0,0 %  enaka
```

### Sumne velicine

- `mspt.max`: mediana 26,188, razpon 6,210 (23,7 %)
- `ticki.nad25ms`: mediana 2, razpon 2 (100,0 %)
