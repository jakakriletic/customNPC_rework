# M2.5c - ponovitve scenarija perf (M2.4), 2026-10-09 21:45

Protokol: `docs/scenariji/M2.5c-ponovitve.md`. Vsaka ponovitev je svoj proces in
svez svet (reset z .\testworld.ps1).

Merila: T1-T6 zelena

## Zagoni

| # | izhodna koda | minut | zapis |
|---|---|---|---|
| 1 | 0 | 8,4 | `m24-perf-2026-10-09-2120-p1.json` |
| 2 | 0 | 8,4 | `m24-perf-2026-10-09-2120-p2.json` |
| 3 | 0 | 8,4 | `m24-perf-2026-10-09-2120-p3.json` |

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
rwcollide            1
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
alok.KBnaTick                        987,500     1.011     1.011     1.011    23,500    2,3 %  stabilna
alok.MBnaS                            19,300    19,700    19,700    19,700     0,400    2,0 %  stabilna
gc.ms                                     32        31        35        32         4   12,5 %  stabilna
gc.msNaS                               0,110     0,100     0,120     0,110     0,020   18,2 %  stabilna
gc.old.ms                                  0         0         0         0         0      n/a  enaka
gc.old.zbirk                               0         0         0         0         0      n/a  enaka
gc.zbirk                                  12        12        12        12         0    0,0 %  enaka
heap.maxMB                             1.838     1.820     1.820     1.820        18    1,0 %  stabilna
mspt.brezSave.max                      8,027     6,828     7,321     7,321     1,199   16,4 %  stabilna
mspt.brezSave.p99                      4,129     4,129     4,456     4,129     0,327    7,9 %  stabilna
mspt.max                              29,729    28,776    28,970    28,970     0,953    3,3 %  stabilna
mspt.p50                               3,080     3,080     3,277     3,080     0,197    6,4 %  stabilna
mspt.p95                               3,932     3,932     4,194     3,932     0,262    6,7 %  stabilna
mspt.p99                               4,129     4,129     4,456     4,129     0,327    7,9 %  stabilna
mspt.povp                              3,009     3,023     3,236     3,023     0,227    7,5 %  stabilna
nasiceno                                   0         0         0         0         0      n/a  enaka
npc.per.tick.p50                         500       500       500       500         0    0,0 %  enaka
npc.us                                 5,800     5,840     6,270     5,840     0,470    8,0 %  stabilna
rwcollide.brezOpazovalca                   0         0         0         0         0      n/a  enaka
rwcollide.dogodkovBrezOpazovalca           0         0         0         0         0      n/a  enaka
rwcollide.klicev                     764.500   764.000   762.500   764.000     2.000    0,3 %  stabilna
rwcollide.preskocenih                764.500   764.000   762.500   764.000     2.000    0,3 %  stabilna
rwtarget.predzavrnjenih                    0         0         0         0         0      n/a  enaka
rwtarget.zozenih                     255.144   254.430   254.219   254.430       925    0,4 %  stabilna
ticki                                  6.035     6.032     6.038     6.035         6    0,1 %  stabilna
ticki.nad100ms                             0         0         0         0         0      n/a  enaka
ticki.nad10ms                              6         6         6         6         0    0,0 %  enaka
ticki.nad25ms                              6         6         4         6         2   33,3 %  SUMNA
ticki.nad50ms                              0         0         0         0         0      n/a  enaka
tps                                       20        20        20        20         0    0,0 %  enaka
```

### Sumne velicine

- `ticki.nad25ms`: mediana 6, razpon 2 (33,3 %)
