# M2.5c - ponovitve scenarija perf (M2.4), 2026-10-10 13:02

Protokol: `docs/scenariji/M2.5c-ponovitve.md`. Vsaka ponovitev je svoj proces in
svez svet (reset z .\testworld.ps1).

Merila: T1-T6 zelena

## Zagoni

| # | izhodna koda | minut | zapis |
|---|---|---|---|
| 1 | 0 | 8,3 | `m24-perf-2026-10-10-1237-p1.json` |
| 2 | 0 | 8,4 | `m24-perf-2026-10-10-1237-p2.json` |
| 3 | 0 | 8,3 | `m24-perf-2026-10-10-1237-p3.json` |

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
rwpath               0
rwpathNeg            1
rwpathNegDelne       0
rwpathNegTtl         20
rwtarget             2
sekund               300
varianta             nedosegljiva
```

## Sumni pas

Prag: relativni razpon <= 20 %. Velicina, oznacena kot SUMNA, se med ponovitvami
premakne bolj kot prag, zato A/B primerjava na njej potrebuje razliko, vecjo od stolpca
`razpon` - manjsa razlika je sum, ne izboljsava.

```
velicina                                  p1        p2        p3   mediana    razpon      rel  oznaka
-----------------------------------------------------------------------------------------------------
alok.KBnaTick                         12.738     6.729     6.924     6.924     6.009   86,8 %  SUMNA
alok.MBnaS                           248,700   131,400   135,200   135,200   117,300   86,8 %  SUMNA
boj.ranjenih                               0         0         0         0         0      n/a  enaka
boj.sCiljem                              500       500       500       500         0    0,0 %  enaka
gc.ms                                    857       339       374       374       518  138,5 %  SUMNA
gc.msNaS                               2,840     1,120     1,240     1,240     1,720  138,7 %  SUMNA
gc.old.ms                                  0         0         0         0         0      n/a  enaka
gc.old.zbirk                               0         0         0         0         0      n/a  enaka
gc.zbirk                                 190        69        81        81       121  149,4 %  SUMNA
heap.maxMB                             1.820     1.820     1.820     1.820         0    0,0 %  enaka
mspt.brezSave.max                     33,139    24,999    26,774    26,774     8,140   30,4 %  SUMNA
mspt.brezSave.p99                     22,020    19,923    19,923    19,923     2,097   10,5 %  stabilna
mspt.max                              41,756    33,714    38,499    38,499     8,042   20,9 %  SUMNA
mspt.p50                              15,204    14,418    14,156    14,418     1,048    7,3 %  stabilna
mspt.p95                              19,399    18,350    17,826    18,350     1,573    8,6 %  stabilna
mspt.p99                              22,020    20,447    19,923    20,447     2,097   10,3 %  stabilna
mspt.povp                             15,443    14,462    14,202    14,462     1,241    8,6 %  stabilna
nasiceno                                   0         0         0         0         0      n/a  enaka
npc.per.tick.p50                         500       500       500       500         0    0,0 %  enaka
npc.us                                30,740    28,790    28,270    28,790     2,470    8,6 %  stabilna
rwpath.izracunov                           0         0         0         0         0      n/a  enaka
rwpath.klicev                              0         0         0         0         0      n/a  enaka
rwpath.neg.neujemanj                       0         0         0         0         0      n/a  enaka
rwpath.neg.preskokov                 106.421   108.916   106.201   106.421     2.715    2,6 %  stabilna
rwpath.neg.preskokovNaTick            17,460    17,830    17,420    17,460     0,410    2,3 %  stabilna
rwpath.neg.primerjav                       0         0         0         0         0      n/a  enaka
rwpath.neg.zapisov                    47.259    48.119    47.128    47.259       991    2,1 %  stabilna
rwpath.neujemanj                           0         0         0         0         0      n/a  enaka
rwpath.ocen                                0         0         0         0         0      n/a  enaka
rwpath.primerjav                           0         0         0         0         0      n/a  enaka
rwtarget.predzavrnjenih                    0         0         0         0         0      n/a  enaka
rwtarget.zozenih                           0         0         0         0         0      n/a  enaka
ticki                                  6.032     6.029     6.036     6.032         7    0,1 %  stabilna
ticki.nad100ms                             0         0         0         0         0      n/a  enaka
ticki.nad10ms                          6.032     6.019     6.021     6.021        13    0,2 %  stabilna
ticki.nad25ms                             16         7        10        10         9   90,0 %  SUMNA
ticki.nad50ms                              0         0         0         0         0      n/a  enaka
tps                                       20        20        20        20         0    0,0 %  enaka
```

### Sumne velicine

- `alok.KBnaTick`: mediana 6.924, razpon 6.009 (86,8 %)
- `alok.MBnaS`: mediana 135,200, razpon 117,300 (86,8 %)
- `gc.ms`: mediana 374, razpon 518 (138,5 %)
- `gc.msNaS`: mediana 1,240, razpon 1,720 (138,7 %)
- `gc.zbirk`: mediana 81, razpon 121 (149,4 %)
- `mspt.brezSave.max`: mediana 26,774, razpon 8,140 (30,4 %)
- `mspt.max`: mediana 38,499, razpon 8,042 (20,9 %)
- `ticki.nad25ms`: mediana 10, razpon 9 (90,0 %)
