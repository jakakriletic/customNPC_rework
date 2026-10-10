# M2.5c - ponovitve scenarija perf (M2.4), 2026-10-10 12:37

Protokol: `docs/scenariji/M2.5c-ponovitve.md`. Vsaka ponovitev je svoj proces in
svez svet (reset z .\testworld.ps1).

Merila: T1-T6 zelena

## Zagoni

| # | izhodna koda | minut | zapis |
|---|---|---|---|
| 1 | 0 | 8,3 | `m24-perf-2026-10-10-1212-p1.json` |
| 2 | 0 | 8,3 | `m24-perf-2026-10-10-1212-p2.json` |
| 3 | 0 | 8,3 | `m24-perf-2026-10-10-1212-p3.json` |

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
rwpathNeg            0
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
alok.KBnaTick                         14.978    15.644    14.769    14.978   875,100    5,8 %  stabilna
alok.MBnaS                           292,500   305,500   288,400   292,500    17,100    5,8 %  stabilna
boj.ranjenih                               0         0         0         0         0      n/a  enaka
boj.sCiljem                              500       500       500       500         0    0,0 %  enaka
gc.ms                                    852       832       941       852       109   12,8 %  stabilna
gc.msNaS                               2,820     2,760     3,120     2,820     0,360   12,8 %  stabilna
gc.old.ms                                  0         0         0         0         0      n/a  enaka
gc.old.zbirk                               0         0         0         0         0      n/a  enaka
gc.zbirk                                 187       168       206       187        38   20,3 %  SUMNA
heap.maxMB                             1.820     1.820     1.820     1.820         0    0,0 %  enaka
mspt.brezSave.max                     27,828    29,810    28,941    28,941     1,982    6,8 %  stabilna
mspt.brezSave.p99                     24,117    24,642    24,642    24,642     0,525    2,1 %  stabilna
mspt.max                              37,927    43,748    55,813    43,748    17,886   40,9 %  SUMNA
mspt.p50                              17,302    18,350    17,826    17,826     1,048    5,9 %  stabilna
mspt.p95                              21,496    22,544    22,020    22,020     1,048    4,8 %  stabilna
mspt.p99                              24,117    25,166    24,642    24,642     1,049    4,3 %  stabilna
mspt.povp                             17,450    18,159    17,854    17,854     0,709    4,0 %  stabilna
nasiceno                                   0         0         0         0         0      n/a  enaka
npc.per.tick.p50                         500       500       500       500         0    0,0 %  enaka
npc.us                                34,750    36,180    35,570    35,570     1,430    4,0 %  stabilna
rwpath.izracunov                           0         0         0         0         0      n/a  enaka
rwpath.klicev                              0         0         0         0         0      n/a  enaka
rwpath.neg.neujemanj                       0         0         0         0         0      n/a  enaka
rwpath.neg.preskokov                       0         0         0         0         0      n/a  enaka
rwpath.neg.preskokovNaTick                 0         0         0         0         0      n/a  enaka
rwpath.neg.primerjav                       0         0         0         0         0      n/a  enaka
rwpath.neg.zapisov                         0         0         0         0         0      n/a  enaka
rwpath.neujemanj                           0         0         0         0         0      n/a  enaka
rwpath.ocen                                0         0         0         0         0      n/a  enaka
rwpath.primerjav                           0         0         0         0         0      n/a  enaka
rwtarget.predzavrnjenih                    0         0         0         0         0      n/a  enaka
rwtarget.zozenih                           0         0         0         0         0      n/a  enaka
ticki                                  6.034     6.037     6.034     6.034         3    0,0 %  stabilna
ticki.nad100ms                             0         0         0         0         0      n/a  enaka
ticki.nad10ms                          6.034     6.037     6.034     6.034         3    0,0 %  stabilna
ticki.nad25ms                             35        48        45        45        13   28,9 %  SUMNA
ticki.nad50ms                              0         0         1         0         1      n/a  SUMNA
tps                                       20        20        20        20         0    0,0 %  enaka
```

### Sumne velicine

- `gc.zbirk`: mediana 187, razpon 38 (20,3 %)
- `mspt.max`: mediana 43,748, razpon 17,886 (40,9 %)
- `ticki.nad25ms`: mediana 45, razpon 13 (28,9 %)
- `ticki.nad50ms`: mediana 0, razpon 1 (mediana je 0, razmerje ne obstaja)
