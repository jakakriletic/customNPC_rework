# M2.5c - ponovitve scenarija perf (M2.4), 2026-10-10 17:38

Protokol: `docs/scenariji/M2.5c-ponovitve.md`. Vsaka ponovitev je svoj proces in
svez svet (reset z .\testworld.ps1).

Merila: T1-T6 zelena

## Zagoni

| # | izhodna koda | minut | zapis |
|---|---|---|---|
| 1 | 0 | 8,2 | `m24-perf-2026-10-10-1713-p1.json` |
| 2 | 0 | 8,3 | `m24-perf-2026-10-10-1713-p2.json` |
| 3 | 0 | 8,3 | `m24-perf-2026-10-10-1713-p3.json` |

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
rwdata               1
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
alok.KBnaTick                          1.059     1.060     1.083     1.060    23,300    2,2 %  stabilna
alok.MBnaS                            20,700    20,700    21,100    20,700     0,400    1,9 %  stabilna
gc.ms                                     32        31        35        32         4   12,5 %  stabilna
gc.msNaS                               0,110     0,100     0,120     0,110     0,020   18,2 %  stabilna
gc.old.ms                                  0         0         0         0         0      n/a  enaka
gc.old.zbirk                               0         0         0         0         0      n/a  enaka
gc.zbirk                                  12        12        13        12         1    8,3 %  stabilna
heap.maxMB                             1.820     1.820     1.820     1.820         0    0,0 %  enaka
mspt.brezSave.max                      9,114     7,990     8,564     8,564     1,124   13,1 %  stabilna
mspt.brezSave.p99                      5,112     4,850     4,850     4,850     0,262    5,4 %  stabilna
mspt.max                              21,239    22,458    20,614    21,239     1,844    8,7 %  stabilna
mspt.p50                               2,687     2,753     2,687     2,687     0,066    2,5 %  stabilna
mspt.p95                               4,325     4,325     4,325     4,325         0    0,0 %  enaka
mspt.p99                               5,243     4,850     4,981     4,981     0,393    7,9 %  stabilna
mspt.povp                              2,863     2,883     2,841     2,863     0,042    1,5 %  stabilna
nasiceno                                   0         0         0         0         0      n/a  enaka
npc.per.tick.p50                         500       500       500       500         0    0,0 %  enaka
npc.us                                 5,600     5,640     5,560     5,600     0,080    1,4 %  stabilna
rwdata.branj                      59.029.08259.227.04459.198.21559.198.215   197.962    0,3 %  stabilna
rwdata.branjNaTick                     9.781     9.814     9.816     9.814    34,600    0,4 %  stabilna
rwdata.neujemanj                           0         0         0         0         0      n/a  enaka
rwdata.primerjav                           0         0         0         0         0      n/a  enaka
rwdata.primerjavNaTick                     0         0         0         0         0      n/a  enaka
rwtarget.predzavrnjenih                    0         0         0         0         0      n/a  enaka
rwtarget.zozenih                     254.043   254.054   254.534   254.054       491    0,2 %  stabilna
ticki                                  6.035     6.035     6.031     6.035         4    0,1 %  stabilna
ticki.nad100ms                             0         0         0         0         0      n/a  enaka
ticki.nad10ms                              7         7         7         7         0    0,0 %  enaka
ticki.nad25ms                              0         0         0         0         0      n/a  enaka
ticki.nad50ms                              0         0         0         0         0      n/a  enaka
tps                                       20        20        20        20         0    0,0 %  enaka
```
