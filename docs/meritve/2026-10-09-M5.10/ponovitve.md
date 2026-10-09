# M2.5c - ponovitve scenarija perf (M2.4), 2026-10-09 09:13

Protokol: `docs/scenariji/M2.5c-ponovitve.md`. Vsaka ponovitev je svoj proces in
svez svet (reset z .\testworld.ps1).

Merila: T1-T6 zelena

## Zagoni

| # | izhodna koda | minut | zapis |
|---|---|---|---|
| 1 | 0 | 8,2 | `m24-perf-2026-10-09-0849-p1.json` |
| 2 | 0 | 8,2 | `m24-perf-2026-10-09-0849-p2.json` |
| 3 | 0 | 8,2 | `m24-perf-2026-10-09-0849-p3.json` |

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
alok.KBnaTick                          3.815     4.575     3.809     3.815       766   20,1 %  SUMNA
alok.MBnaS                            74,500    89,300    74,400    74,500    14,900   20,0 %  stabilna
boj.ranjenih                             500       500       500       500         0    0,0 %  enaka
boj.sCiljem                              500       500       500       500         0    0,0 %  enaka
gc.ms                                    133       163       136       136        30   22,1 %  SUMNA
gc.msNaS                               0,440     0,540     0,450     0,450     0,100   22,2 %  SUMNA
gc.old.ms                                  0         0         0         0         0      n/a  enaka
gc.old.zbirk                               0         0         0         0         0      n/a  enaka
gc.zbirk                                  45        58        46        46        13   28,3 %  SUMNA
heap.maxMB                             1.820     1.820     1.820     1.820         0    0,0 %  enaka
mspt.brezSave.max                     18,843    16,191    15,063    16,191     3,780   23,3 %  SUMNA
mspt.brezSave.p99                     12,845    12,845    11,534    12,845     1,311   10,2 %  stabilna
mspt.max                              30,302    38,729    41,015    38,729    10,713   27,7 %  SUMNA
mspt.p50                               8,389     9,175     7,864     8,389     1,311   15,6 %  stabilna
mspt.p95                              10,224    10,486     9,437    10,224     1,049   10,3 %  stabilna
mspt.p99                              13,107    13,369    11,534    13,107     1,835   14,0 %  stabilna
mspt.povp                              8,549     9,209     8,011     8,549     1,198   14,0 %  stabilna
nasiceno                                   0         0         0         0         0      n/a  enaka
npc.per.tick.p50                         500       500       500       500         0    0,0 %  enaka
npc.us                                16,860    18,230    15,840    16,860     2,390   14,2 %  stabilna
rwpath.branj                               0         0         0         0         0      n/a  enaka
rwpath.hist.0                         68.334    64.590    71.718    68.334     7.128   10,4 %  stabilna
rwpath.hist.1                      1.529.034 1.516.062 1.532.409 1.529.034    16.347    1,1 %  stabilna
rwpath.hist.2                              0         0         0         0         0      n/a  enaka
rwpath.hist.3                              0         0         0         0         0      n/a  enaka
rwpath.hist.4                              0         0         0         0         0      n/a  enaka
rwpath.hist.5                              0         0         0         0         0      n/a  enaka
rwpath.iskanjChunka                        0         0         0         0         0      n/a  enaka
rwpath.kandidat.delez                 47,100    53,100    49,400    49,400         6   12,1 %  stabilna
rwpath.kandidat.msNaTick               4,026     4,891     3,956     4,026     0,935   23,2 %  SUMNA
rwpath.kandidat.usNaKlic              16,040    19,680    15,750    16,040     3,930   24,5 %  SUMNA
rwpath.kandidatov                  1.529.034 1.516.062 1.532.409 1.529.034    16.347    1,1 %  stabilna
rwpath.kandidatovNaSledenje            0,957     0,959     0,955     0,957     0,004    0,4 %  stabilna
rwpath.kandidatovNaTick              250,900   248,500   251,200   250,900     2,700    1,1 %  stabilna
rwpath.klicev                              0         0         0         0         0      n/a  enaka
rwpath.neujemanj                           0         0         0         0         0      n/a  enaka
rwpath.primerjav                           0         0         0         0         0      n/a  enaka
rwpath.prostih                     1.529.034 1.516.062 1.532.409 1.529.034    16.347    1,1 %  stabilna
rwpath.sledenj                     1.597.368 1.580.652 1.604.127 1.597.368    23.475    1,5 %  stabilna
rwpath.sledenje.delez                 47,700    53,600        50        50     5,900   11,8 %  stabilna
rwpath.sledenje.msNaTick               4,082     4,937     4,002     4,082     0,935   22,9 %  SUMNA
rwpath.sledenje.usNaKlic              15,570    19,060    15,220    15,570     3,840   24,7 %  SUMNA
rwpath.sledenjNaTick                 262,200   259,100   262,900   262,200     3,800    1,4 %  stabilna
rwpath.tickov                          6.093     6.101     6.101     6.101         8    0,1 %  stabilna
rwtarget.predzavrnjenih                3.762     2.141     2.846     2.846     1.621   57,0 %  SUMNA
rwtarget.zozenih                           0         0         0         0         0      n/a  enaka
ticki                                  6.032     6.038     6.039     6.038         7    0,1 %  stabilna
ticki.nad100ms                             0         0         0         0         0      n/a  enaka
ticki.nad10ms                            305       634       133       305       501  164,3 %  SUMNA
ticki.nad25ms                              7         7         7         7         0    0,0 %  enaka
ticki.nad50ms                              0         0         0         0         0      n/a  enaka
tps                                       20        20        20        20         0    0,0 %  enaka
```

### Sumne velicine

- `alok.KBnaTick`: mediana 3.815, razpon 766 (20,1 %)
- `gc.ms`: mediana 136, razpon 30 (22,1 %)
- `gc.msNaS`: mediana 0,450, razpon 0,100 (22,2 %)
- `gc.zbirk`: mediana 46, razpon 13 (28,3 %)
- `mspt.brezSave.max`: mediana 16,191, razpon 3,780 (23,3 %)
- `mspt.max`: mediana 38,729, razpon 10,713 (27,7 %)
- `rwpath.kandidat.msNaTick`: mediana 4,026, razpon 0,935 (23,2 %)
- `rwpath.kandidat.usNaKlic`: mediana 16,040, razpon 3,930 (24,5 %)
- `rwpath.sledenje.msNaTick`: mediana 4,082, razpon 0,935 (22,9 %)
- `rwpath.sledenje.usNaKlic`: mediana 15,570, razpon 3,840 (24,7 %)
- `rwtarget.predzavrnjenih`: mediana 2.846, razpon 1.621 (57,0 %)
- `ticki.nad10ms`: mediana 305, razpon 501 (164,3 %)
