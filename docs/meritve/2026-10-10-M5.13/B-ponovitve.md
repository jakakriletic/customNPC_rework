# M2.5c - ponovitve scenarija perf (M2.4), 2026-10-10 10:36

Protokol: `docs/scenariji/M2.5c-ponovitve.md`. Vsaka ponovitev je svoj proces in
svez svet (reset z .\testworld.ps1).

Merila: T1-T6 zelena

## Zagoni

| # | izhodna koda | minut | zapis |
|---|---|---|---|
| 1 | 0 | 8,2 | `m24-perf-2026-10-10-1012-p1.json` |
| 2 | 0 | 8,1 | `m24-perf-2026-10-10-1012-p2.json` |
| 3 | 0 | 8,1 | `m24-perf-2026-10-10-1012-p3.json` |

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
rwblink              1
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
alok.KBnaTick                          1.059     1.059     1.059     1.059     0,200    0,0 %  stabilna
alok.MBnaS                            20,700    20,700    20,700    20,700         0    0,0 %  enaka
gc.ms                                     32        27        31        31         5   16,1 %  stabilna
gc.msNaS                               0,110     0,090     0,100     0,100     0,020   20,0 %  SUMNA
gc.old.ms                                  0         0         0         0         0      n/a  enaka
gc.old.zbirk                               0         0         0         0         0      n/a  enaka
gc.zbirk                                  12        12        12        12         0    0,0 %  enaka
heap.maxMB                             1.820     1.820     1.820     1.820         0    0,0 %  enaka
mspt.brezSave.max                      7,423     6,780     7,587     7,423     0,807   10,9 %  stabilna
mspt.brezSave.p99                      4,325     4,063     4,325     4,325     0,262    6,1 %  stabilna
mspt.max                              21,462    19,708    23,180    21,462     3,472   16,2 %  stabilna
mspt.p50                               2,359     2,228     2,425     2,359     0,197    8,4 %  stabilna
mspt.p95                               3,932     3,801     3,932     3,932     0,131    3,3 %  stabilna
mspt.p99                               4,456     4,129     4,325     4,325     0,327    7,6 %  stabilna
mspt.povp                              2,570     2,430     2,601     2,570     0,171    6,7 %  stabilna
nasiceno                                   0         0         0         0         0      n/a  enaka
npc.per.tick.p50                         500       500       500       500         0    0,0 %  enaka
npc.us                                 5,050     4,770     5,110     5,050     0,340    6,7 %  stabilna
rwblink.chunkov                    8.255.524 8.173.870 8.221.187 8.221.187    81.654    1,0 %  stabilna
rwblink.igralcev                           0         0         0         0         0      n/a  enaka
rwblink.iskanj                        18.334    18.151    18.257    18.257       183    1,0 %  stabilna
rwblink.neujemanj                          0         0         0         0         0      n/a  enaka
rwblink.poskus.neujemanj                   0         0         0         0         0      n/a  enaka
rwblink.poskus.primerjav                 320       320       320       320         0    0,0 %  enaka
rwblink.prejemnikov                        0         0         0         0         0      n/a  enaka
rwblink.primerjav                          0         0         0         0         0      n/a  enaka
rwtarget.predzavrnjenih                    0         0         0         0         0      n/a  enaka
rwtarget.zozenih                     253.160   253.367   252.835   253.160       532    0,2 %  stabilna
ticki                                  6.019     6.020     6.019     6.019         1    0,0 %  stabilna
ticki.nad100ms                             0         0         0         0         0      n/a  enaka
ticki.nad10ms                              7         7         7         7         0    0,0 %  enaka
ticki.nad25ms                              0         0         0         0         0      n/a  enaka
ticki.nad50ms                              0         0         0         0         0      n/a  enaka
tps                                       20        20        20        20         0    0,0 %  enaka
```

### Sumne velicine

- `gc.msNaS`: mediana 0,100, razpon 0,020 (20,0 %)
