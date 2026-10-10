# M2.5c - ponovitve scenarija perf (M2.4), 2026-10-10 17:55

Protokol: `docs/scenariji/M2.5c-ponovitve.md`. Vsaka ponovitev je svoj proces in
svez svet (reset z .\testworld.ps1).

Merila: T1-T6 zelena

## Zagoni

| # | izhodna koda | minut | zapis |
|---|---|---|---|
| 1 | 0 | 5,7 | `m24-perf-2026-10-10-1738-p1.json` |
| 2 | 0 | 5,8 | `m24-perf-2026-10-10-1738-p2.json` |
| 3 | 0 | 5,7 | `m24-perf-2026-10-10-1738-p3.json` |

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
rwdata               0
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
alok.KBnaTick                          2.086     4.665     4.666     4.665     2.580   55,3 %  SUMNA
alok.MBnaS                            40,700    91,100    91,100    91,100    50,400   55,3 %  SUMNA
boj.ranjenih                             500       500       500       500         0    0,0 %  enaka
boj.sCiljem                              500       500       500       500         0    0,0 %  enaka
gc.ms                                     52        88        82        82        36   43,9 %  SUMNA
gc.msNaS                               0,290     0,480     0,450     0,450     0,190   42,2 %  SUMNA
gc.old.ms                                  0         0         0         0         0      n/a  enaka
gc.old.zbirk                               0         0         0         0         0      n/a  enaka
gc.zbirk                                  14        30        29        29        16   55,2 %  SUMNA
heap.maxMB                             1.820     1.820     1.820     1.820         0    0,0 %  enaka
mspt.brezSave.max                     16,672    22,447    18,727    18,727     5,775   30,8 %  SUMNA
mspt.brezSave.p99                     10,224    12,845    12,321    12,321     2,621   21,3 %  SUMNA
mspt.max                              31,690    30,648    29,866    30,648     1,824    6,0 %  stabilna
mspt.p50                               7,864     9,437     9,175     9,175     1,573   17,1 %  stabilna
mspt.p95                               9,437    10,748    10,486    10,486     1,311   12,5 %  stabilna
mspt.p99                              10,486    13,107    12,583    12,583     2,621   20,8 %  SUMNA
mspt.povp                              7,960     9,416     9,244     9,244     1,456   15,8 %  stabilna
nasiceno                                   0         0         0         0         0      n/a  enaka
npc.per.tick.p50                         500       500       500       500         0    0,0 %  enaka
npc.us                                15,730    18,630    18,290    18,290     2,900   15,9 %  stabilna
rwdata.branj                               0         0         0         0         0      n/a  enaka
rwdata.branjNaTick                         0         0         0         0         0      n/a  enaka
rwdata.neujemanj                           0         0         0         0         0      n/a  enaka
rwdata.primerjav                           0         0         0         0         0      n/a  enaka
rwdata.primerjavNaTick                     0         0         0         0         0      n/a  enaka
rwtarget.predzavrnjenih                4.428     7.534     8.031     7.534     3.603   47,8 %  SUMNA
rwtarget.zozenih                           0         0         0         0         0      n/a  enaka
ticki                                  3.630     3.631     3.628     3.630         3    0,1 %  stabilna
ticki.nad100ms                             0         0         0         0         0      n/a  enaka
ticki.nad10ms                             50       562       446       446       512  114,8 %  SUMNA
ticki.nad25ms                              4         4         4         4         0    0,0 %  enaka
ticki.nad50ms                              0         0         0         0         0      n/a  enaka
tps                                   19,990    19,990    19,990    19,990         0    0,0 %  enaka
```

### Sumne velicine

- `alok.KBnaTick`: mediana 4.665, razpon 2.580 (55,3 %)
- `alok.MBnaS`: mediana 91,100, razpon 50,400 (55,3 %)
- `gc.ms`: mediana 82, razpon 36 (43,9 %)
- `gc.msNaS`: mediana 0,450, razpon 0,190 (42,2 %)
- `gc.zbirk`: mediana 29, razpon 16 (55,2 %)
- `mspt.brezSave.max`: mediana 18,727, razpon 5,775 (30,8 %)
- `mspt.brezSave.p99`: mediana 12,321, razpon 2,621 (21,3 %)
- `mspt.p99`: mediana 12,583, razpon 2,621 (20,8 %)
- `rwtarget.predzavrnjenih`: mediana 7.534, razpon 3.603 (47,8 %)
- `ticki.nad10ms`: mediana 446, razpon 512 (114,8 %)
