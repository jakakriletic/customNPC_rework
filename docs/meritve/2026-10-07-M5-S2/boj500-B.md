# M2.5c - ponovitve scenarija perf (M2.4), 2026-10-07 19:27

Protokol: `docs/scenariji/M2.5c-ponovitve.md`. Vsaka ponovitev je svoj proces in
svez svet (reset z .\testworld.ps1).

Merila: T1-T6 zelena

## Zagoni

| # | izhodna koda | minut | zapis |
|---|---|---|---|
| 1 | 0 | 8,1 | `m24-perf-2026-10-07-1903-p1.json` |
| 2 | 0 | 8,1 | `m24-perf-2026-10-07-1903-p2.json` |
| 3 | 0 | 8,1 | `m24-perf-2026-10-07-1903-p3.json` |

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
rwtarget             2
sekund               300
varianta             boj
```

## Sumni pas

Prag: relativni razpon <= 20 %. Velicina, oznacena kot SUMNA, se med ponovitvami
premakne bolj kot prag, zato A/B primerjava na njej potrebuje razliko, vecjo od stolpca
`razpon` - manjsa razlika je sum, ne izboljsava.

```
velicina                                  p1        p2        p3   mediana    razpon      rel  oznaka
-----------------------------------------------------------------------------------------------------
alok.KBnaTick                          3.897     4.576     4.663     4.576   766,400   16,7 %  stabilna
alok.MBnaS                            76,100    89,400    91,100    89,400        15   16,8 %  stabilna
boj.ranjenih                             500       500       500       500         0    0,0 %  enaka
boj.sCiljem                              500       500       500       500         0    0,0 %  enaka
gc.ms                                    147       160       169       160        22   13,8 %  stabilna
gc.msNaS                               0,490     0,530     0,560     0,530     0,070   13,2 %  stabilna
gc.old.ms                                  0         0         0         0         0      n/a  enaka
gc.old.zbirk                               0         0         0         0         0      n/a  enaka
gc.zbirk                                  50        59        60        59        10   16,9 %  stabilna
heap.maxMB                             1.820     1.820     1.820     1.820         0    0,0 %  enaka
mspt.brezSave.max                     12,327    14,201    14,028    14,028     1,874   13,4 %  stabilna
mspt.brezSave.p99                      9,961    11,272    11,010    11,010     1,311   11,9 %  stabilna
mspt.max                              35,759    36,477    37,099    36,477     1,340    3,7 %  stabilna
mspt.p50                               7,471     8,913     8,651     8,651     1,442   16,7 %  stabilna
mspt.p95                               8,651     9,961     9,961     9,961     1,310   13,2 %  stabilna
mspt.p99                              10,224    11,272    11,272    11,272     1,048    9,3 %  stabilna
mspt.povp                              7,569     8,796     8,667     8,667     1,227   14,2 %  stabilna
nasiceno                                   0         0         0         0         0      n/a  enaka
npc.per.tick.p50                         500       500       500       500         0    0,0 %  enaka
npc.us                                14,990    17,440    17,190    17,190     2,450   14,3 %  stabilna
rwtarget.predzavrnjenih                7.553     3.141     1.197     3.141     6.356  202,4 %  SUMNA
rwtarget.zozenih                           0         0         0         0         0      n/a  enaka
ticki                                  6.031     6.032     6.047     6.032        16    0,3 %  stabilna
ticki.nad100ms                             0         0         0         0         0      n/a  enaka
ticki.nad10ms                             61       218       188       188       157   83,5 %  SUMNA
ticki.nad25ms                              6         6         6         6         0    0,0 %  enaka
ticki.nad50ms                              0         0         0         0         0      n/a  enaka
tps                                       20        20        20        20         0    0,0 %  enaka
```

### Sumne velicine

- `rwtarget.predzavrnjenih`: mediana 3.141, razpon 6.356 (202,4 %)
- `ticki.nad10ms`: mediana 188, razpon 157 (83,5 %)
