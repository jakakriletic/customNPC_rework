# M2.5c - ponovitve scenarija perf (M2.4), 2026-10-09 19:38

Protokol: `docs/scenariji/M2.5c-ponovitve.md`. Vsaka ponovitev je svoj proces in
svez svet (reset z .\testworld.ps1).

Merila: T1-T6 zelena

## Zagoni

| # | izhodna koda | minut | zapis |
|---|---|---|---|
| 1 | 0 | 8,2 | `m24-perf-2026-10-09-1913-p1.json` |
| 2 | 0 | 8,2 | `m24-perf-2026-10-09-1913-p2.json` |
| 3 | 0 | 8,2 | `m24-perf-2026-10-09-1913-p3.json` |

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
rwpathCas            1
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
alok.KBnaTick                          4.539     4.923     4.556     4.556   384,100    8,4 %  stabilna
alok.MBnaS                            88,600    96,100        89        89     7,500    8,4 %  stabilna
boj.ranjenih                             500       500       500       500         0    0,0 %  enaka
boj.sCiljem                              500       500       500       500         0    0,0 %  enaka
gc.ms                                    146       167       153       153        21   13,7 %  stabilna
gc.msNaS                               0,480     0,550     0,510     0,510     0,070   13,7 %  stabilna
gc.old.ms                                  0         0         0         0         0      n/a  enaka
gc.old.zbirk                               0         0         0         0         0      n/a  enaka
gc.zbirk                                  53        61        56        56         8   14,3 %  stabilna
heap.maxMB                             1.820     1.820     1.820     1.820         0    0,0 %  enaka
mspt.brezSave.max                     17,601    16,055    27,038    17,601    10,983   62,4 %  SUMNA
mspt.brezSave.p99                     11,010    11,796    11,272    11,272     0,786    7,0 %  stabilna
mspt.max                              36,520    31,072    32,791    32,791     5,448   16,6 %  stabilna
mspt.p50                               8,651     9,175     8,651     8,651     0,524    6,1 %  stabilna
mspt.p95                               9,961    10,486     9,699     9,961     0,787    7,9 %  stabilna
mspt.p99                              11,272    12,059    11,272    11,272     0,787    7,0 %  stabilna
mspt.povp                              8,734     9,112     8,592     8,734     0,520    6,0 %  stabilna
nasiceno                                   0         0         0         0         0      n/a  enaka
npc.per.tick.p50                         500       500       500       500         0    0,0 %  enaka
npc.us                                17,320    18,070    17,050    17,320     1,020    5,9 %  stabilna
rwpath.hist.0                         63.729    70.639    65.723    65.723     6.910   10,5 %  stabilna
rwpath.hist.1                      1.498.095 1.537.516 1.507.056 1.507.056    39.421    2,6 %  stabilna
rwpath.hist.2                              0         0         0         0         0      n/a  enaka
rwpath.hist.3                              0         0         0         0         0      n/a  enaka
rwpath.hist.4                              0         0         0         0         0      n/a  enaka
rwpath.hist.5                              0         0         0         0         0      n/a  enaka
rwpath.izracunov                           0         0         0         0         0      n/a  enaka
rwpath.kandidat.delez                 56,100        57    54,800    56,100     2,200    3,9 %  stabilna
rwpath.kandidat.msNaTick               4,898     5,193     4,705     4,898     0,488   10,0 %  stabilna
rwpath.kandidat.usNaKlic              19,900    20,630    19,020    19,900     1,610    8,1 %  stabilna
rwpath.kandidatov                  1.498.095 1.537.516 1.507.056 1.507.056    39.421    2,6 %  stabilna
rwpath.kandidatovNaSledenje            0,959     0,956     0,958     0,958     0,003    0,3 %  stabilna
rwpath.kandidatovNaTick              246,100   251,700   247,400   247,400     5,600    2,3 %  stabilna
rwpath.klicev                              0         0         0         0         0      n/a  enaka
rwpath.neujemanj                           0         0         0         0         0      n/a  enaka
rwpath.ocen                                0         0         0         0         0      n/a  enaka
rwpath.primerjav                           0         0         0         0         0      n/a  enaka
rwpath.prostih                     1.498.095 1.537.516 1.507.056 1.507.056    39.421    2,6 %  stabilna
rwpath.sledenj                     1.561.824 1.608.155 1.572.779 1.572.779    46.331    2,9 %  stabilna
rwpath.sledenje.delez                 56,600    57,500    55,200    56,600     2,300    4,1 %  stabilna
rwpath.sledenje.msNaTick               4,942     5,235     4,745     4,942     0,490    9,9 %  stabilna
rwpath.sledenje.usNaKlic              19,270    19,890    18,380    19,270     1,510    7,8 %  stabilna
rwpath.sledenjNaTick                 256,500   263,200   258,200   258,200     6,700    2,6 %  stabilna
rwpath.tickov                          6.088     6.109     6.092     6.092        21    0,3 %  stabilna
rwtarget.predzavrnjenih                    0       399     3.120       399     3.120  782,0 %  SUMNA
rwtarget.zozenih                           0         0         0         0         0      n/a  enaka
ticki                                  6.027     6.048     6.031     6.031        21    0,3 %  stabilna
ticki.nad100ms                             0         0         0         0         0      n/a  enaka
ticki.nad10ms                            183       466       158       183       308  168,3 %  SUMNA
ticki.nad25ms                              7         7         8         7         1   14,3 %  stabilna
ticki.nad50ms                              0         0         0         0         0      n/a  enaka
tps                                       20        20        20        20         0    0,0 %  enaka
```

### Sumne velicine

- `mspt.brezSave.max`: mediana 17,601, razpon 10,983 (62,4 %)
- `rwtarget.predzavrnjenih`: mediana 399, razpon 3.120 (782,0 %)
- `ticki.nad10ms`: mediana 183, razpon 308 (168,3 %)
