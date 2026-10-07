# M2.5c - ponovitve scenarija perf (M2.4), 2026-10-07 18:14

Protokol: `docs/scenariji/M2.5c-ponovitve.md`. Vsaka ponovitev je svoj proces in
svez svet (reset z .\testworld.ps1).

Merila: T1-T6 zelena

## Zagoni

| # | izhodna koda | minut | zapis |
|---|---|---|---|
| 1 | 0 | 8,1 | `m24-perf-2026-10-07-1750-p1.json` |
| 2 | 0 | 8,0 | `m24-perf-2026-10-07-1750-p2.json` |
| 3 | 0 | 8,1 | `m24-perf-2026-10-07-1750-p3.json` |

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
rwtarget             1
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
alok.KBnaTick                          1.074     1.074     1.097     1.074    23,500    2,2 %  stabilna
alok.MBnaS                                21        21    21,400        21     0,400    1,9 %  stabilna
gc.ms                                     32        35        34        34         3    8,8 %  stabilna
gc.msNaS                               0,110     0,120     0,110     0,110     0,010    9,1 %  stabilna
gc.old.ms                                  0         0         0         0         0      n/a  enaka
gc.old.zbirk                               0         0         0         0         0      n/a  enaka
gc.zbirk                                  12        13        13        13         1    7,7 %  stabilna
heap.maxMB                             1.820     1.820     1.820     1.820         0    0,0 %  enaka
mspt.brezSave.max                     14,170    17,762    12,593    14,170     5,169   36,5 %  SUMNA
mspt.brezSave.p99                     11,010    11,272    11,272    11,272     0,262    2,3 %  stabilna
mspt.max                              33,357    22,062    28,220    28,220    11,295   40,0 %  SUMNA
mspt.p50                               3,408     3,408     3,539     3,408     0,131    3,8 %  stabilna
mspt.p95                               9,961    10,224    10,224    10,224     0,263    2,6 %  stabilna
mspt.p99                              11,272    11,272    11,272    11,272         0    0,0 %  enaka
mspt.povp                              4,910     4,942     5,011     4,942     0,101    2,0 %  stabilna
nasiceno                                   0         0         0         0         0      n/a  enaka
npc.per.tick.p50                         500       500       500       500         0    0,0 %  enaka
npc.us                                 9,680     9,740     9,880     9,740     0,200    2,1 %  stabilna
rwtarget.predzavrnjenih           108.550.846108.411.604108.649.598108.550.846   237.994    0,2 %  stabilna
rwtarget.zozenih                           0         0         0         0         0      n/a  enaka
ticki                                  6.047     6.028     6.033     6.033        19    0,3 %  stabilna
ticki.nad100ms                             0         0         0         0         0      n/a  enaka
ticki.nad10ms                            280       370       386       370       106   28,6 %  SUMNA
ticki.nad25ms                              6         0         5         5         6  120,0 %  SUMNA
ticki.nad50ms                              0         0         0         0         0      n/a  enaka
tps                                       20        20        20        20         0    0,0 %  enaka
```

### Sumne velicine

- `mspt.brezSave.max`: mediana 14,170, razpon 5,169 (36,5 %)
- `mspt.max`: mediana 28,220, razpon 11,295 (40,0 %)
- `ticki.nad10ms`: mediana 370, razpon 106 (28,6 %)
- `ticki.nad25ms`: mediana 5, razpon 6 (120,0 %)
