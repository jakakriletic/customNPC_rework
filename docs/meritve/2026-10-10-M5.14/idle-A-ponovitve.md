# M2.5c - ponovitve scenarija perf (M2.4), 2026-10-10 17:13

Protokol: `docs/scenariji/M2.5c-ponovitve.md`. Vsaka ponovitev je svoj proces in
svez svet (reset z .\testworld.ps1).

Merila: T1-T6 zelena

## Zagoni

| # | izhodna koda | minut | zapis |
|---|---|---|---|
| 1 | 0 | 8,3 | `m24-perf-2026-10-10-1648-p1.json` |
| 2 | 0 | 8,2 | `m24-perf-2026-10-10-1648-p2.json` |
| 3 | 0 | 8,2 | `m24-perf-2026-10-10-1648-p3.json` |

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
rwdata               0
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
gc.ms                                     37        33        33        33         4   12,1 %  stabilna
gc.msNaS                               0,120     0,110     0,110     0,110     0,010    9,1 %  stabilna
gc.old.ms                                  0         0         0         0         0      n/a  enaka
gc.old.zbirk                               0         0         0         0         0      n/a  enaka
gc.zbirk                                  12        12        12        12         0    0,0 %  enaka
heap.maxMB                             1.820     1.820     1.820     1.820         0    0,0 %  enaka
mspt.brezSave.max                     11,739     7,701    12,650    11,739     4,949   42,2 %  SUMNA
mspt.brezSave.p99                      5,112     4,981     5,243     5,112     0,262    5,1 %  stabilna
mspt.max                              22,262    21,085    21,121    21,121     1,177    5,6 %  stabilna
mspt.p50                               2,818     2,818     2,818     2,818         0    0,0 %  enaka
mspt.p95                               4,456     4,588     4,588     4,588     0,132    2,9 %  stabilna
mspt.p99                               5,112     4,981     5,374     5,112     0,393    7,7 %  stabilna
mspt.povp                              3,023     2,991     3,068     3,023     0,077    2,5 %  stabilna
nasiceno                                   0         0         0         0         0      n/a  enaka
npc.per.tick.p50                         500       500       500       500         0    0,0 %  enaka
npc.us                                 5,920     5,850         6     5,920     0,150    2,5 %  stabilna
rwdata.branj                               0         0         0         0         0      n/a  enaka
rwdata.branjNaTick                         0         0         0         0         0      n/a  enaka
rwdata.neujemanj                           0         0         0         0         0      n/a  enaka
rwdata.primerjav                           0         0         0         0         0      n/a  enaka
rwdata.primerjavNaTick                     0         0         0         0         0      n/a  enaka
rwtarget.predzavrnjenih                    0         0         0         0         0      n/a  enaka
rwtarget.zozenih                     254.330   253.859   253.040   253.859     1.290    0,5 %  stabilna
ticki                                  6.055     6.033     6.021     6.033        34    0,6 %  stabilna
ticki.nad100ms                             0         0         0         0         0      n/a  enaka
ticki.nad10ms                             13         7        15        13         8   61,5 %  SUMNA
ticki.nad25ms                              0         0         0         0         0      n/a  enaka
ticki.nad50ms                              0         0         0         0         0      n/a  enaka
tps                                       20        20        20        20         0    0,0 %  enaka
```

### Sumne velicine

- `mspt.brezSave.max`: mediana 11,739, razpon 4,949 (42,2 %)
- `ticki.nad10ms`: mediana 13, razpon 8 (61,5 %)
