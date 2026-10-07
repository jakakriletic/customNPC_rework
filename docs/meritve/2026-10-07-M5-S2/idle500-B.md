# M2.5c - ponovitve scenarija perf (M2.4), 2026-10-07 18:38

Protokol: `docs/scenariji/M2.5c-ponovitve.md`. Vsaka ponovitev je svoj proces in
svez svet (reset z .\testworld.ps1).

Merila: T1-T6 zelena

## Zagoni

| # | izhodna koda | minut | zapis |
|---|---|---|---|
| 1 | 0 | 8,1 | `m24-perf-2026-10-07-1814-p1.json` |
| 2 | 0 | 8,1 | `m24-perf-2026-10-07-1814-p2.json` |
| 3 | 0 | 8,1 | `m24-perf-2026-10-07-1814-p3.json` |

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
varianta             idle
```

## Sumni pas

Prag: relativni razpon <= 20 %. Velicina, oznacena kot SUMNA, se med ponovitvami
premakne bolj kot prag, zato A/B primerjava na njej potrebuje razliko, vecjo od stolpca
`razpon` - manjsa razlika je sum, ne izboljsava.

```
velicina                                  p1        p2        p3   mediana    razpon      rel  oznaka
-----------------------------------------------------------------------------------------------------
alok.KBnaTick                          1.078     1.057     1.057     1.057    20,700    2,0 %  stabilna
alok.MBnaS                                21    20,600    20,600    20,600     0,400    1,9 %  stabilna
gc.ms                                     31        32        32        32         1    3,1 %  stabilna
gc.msNaS                               0,100     0,110     0,110     0,110     0,010    9,1 %  stabilna
gc.old.ms                                  0         0         0         0         0      n/a  enaka
gc.old.zbirk                               0         0         0         0         0      n/a  enaka
gc.zbirk                                  12        12        12        12         0    0,0 %  enaka
heap.maxMB                             1.820     1.820     1.820     1.820         0    0,0 %  enaka
mspt.brezSave.max                      7,734     6,806     7,211     7,211     0,928   12,9 %  stabilna
mspt.brezSave.p99                      5,505     5,374     5,374     5,374     0,131    2,4 %  stabilna
mspt.max                              29,452    29,368    27,916    29,368     1,536    5,2 %  stabilna
mspt.p50                               3,342     3,146     3,211     3,211     0,196    6,1 %  stabilna
mspt.p95                               5,243     5,243     5,112     5,243     0,131    2,5 %  stabilna
mspt.p99                               5,505     5,505     5,374     5,505     0,131    2,4 %  stabilna
mspt.povp                              3,491     3,346     3,410     3,410     0,145    4,3 %  stabilna
nasiceno                                   0         0         0         0         0      n/a  enaka
npc.per.tick.p50                         500       500       500       500         0    0,0 %  enaka
npc.us                                 6,770     6,470     6,600     6,600     0,300    4,5 %  stabilna
rwtarget.predzavrnjenih                    0         0         0         0         0      n/a  enaka
rwtarget.zozenih                     252.301   254.313   252.327   252.327     2.012    0,8 %  stabilna
ticki                                  6.020     6.035     6.036     6.035        16    0,3 %  stabilna
ticki.nad100ms                             0         0         0         0         0      n/a  enaka
ticki.nad10ms                              6         6         6         6         0    0,0 %  enaka
ticki.nad25ms                              4         5         3         4         2   50,0 %  SUMNA
ticki.nad50ms                              0         0         0         0         0      n/a  enaka
tps                                       20        20        20        20         0    0,0 %  enaka
```

### Sumne velicine

- `ticki.nad25ms`: mediana 4, razpon 2 (50,0 %)
