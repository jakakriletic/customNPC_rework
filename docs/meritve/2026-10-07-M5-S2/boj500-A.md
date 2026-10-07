# M2.5c - ponovitve scenarija perf (M2.4), 2026-10-07 19:03

Protokol: `docs/scenariji/M2.5c-ponovitve.md`. Vsaka ponovitev je svoj proces in
svez svet (reset z .\testworld.ps1).

Merila: T1-T6 zelena

## Zagoni

| # | izhodna koda | minut | zapis |
|---|---|---|---|
| 1 | 0 | 8,1 | `m24-perf-2026-10-07-1838-p1.json` |
| 2 | 0 | 8,1 | `m24-perf-2026-10-07-1838-p2.json` |
| 3 | 0 | 8,1 | `m24-perf-2026-10-07-1838-p3.json` |

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
varianta             boj
```

## Sumni pas

Prag: relativni razpon <= 20 %. Velicina, oznacena kot SUMNA, se med ponovitvami
premakne bolj kot prag, zato A/B primerjava na njej potrebuje razliko, vecjo od stolpca
`razpon` - manjsa razlika je sum, ne izboljsava.

```
velicina                                  p1        p2        p3   mediana    razpon      rel  oznaka
-----------------------------------------------------------------------------------------------------
alok.KBnaTick                          2.082     4.577     4.678     4.577     2.596   56,7 %  SUMNA
alok.MBnaS                            40,700    89,400    91,400    89,400    50,700   56,7 %  SUMNA
boj.ranjenih                             500       500       500       500         0    0,0 %  enaka
boj.sCiljem                              500       500       500       500         0    0,0 %  enaka
gc.ms                                     83       164       165       164        82   50,0 %  SUMNA
gc.msNaS                               0,280     0,540     0,550     0,540     0,270   50,0 %  SUMNA
gc.old.ms                                  0         0         0         0         0      n/a  enaka
gc.old.zbirk                               0         0         0         0         0      n/a  enaka
gc.zbirk                                  24        60        61        60        37   61,7 %  SUMNA
heap.maxMB                             1.820     1.820     1.820     1.820         0    0,0 %  enaka
mspt.brezSave.max                     13,768    12,903    14,818    13,768     1,915   13,9 %  stabilna
mspt.brezSave.p99                      9,175    11,272    11,272    11,272     2,097   18,6 %  stabilna
mspt.max                              34,524    37,908    35,846    35,846     3,384    9,4 %  stabilna
mspt.p50                               7,340     8,913     8,913     8,913     1,573   17,6 %  stabilna
mspt.p95                               8,389     9,961     9,961     9,961     1,572   15,8 %  stabilna
mspt.p99                               9,175    11,534    11,272    11,272     2,359   20,9 %  SUMNA
mspt.povp                              7,373     8,794     8,822     8,794     1,449   16,5 %  stabilna
nasiceno                                   0         0         0         0         0      n/a  enaka
npc.per.tick.p50                         500       500       500       500         0    0,0 %  enaka
npc.us                                14,610    17,440    17,500    17,440     2,890   16,6 %  stabilna
rwtarget.predzavrnjenih                1.588     2.158     2.562     2.158       974   45,1 %  SUMNA
rwtarget.zozenih                           0         0         0         0         0      n/a  enaka
ticki                                  6.033     6.030     6.033     6.033         3    0,0 %  stabilna
ticki.nad100ms                             0         0         0         0         0      n/a  enaka
ticki.nad10ms                             37       238       268       238       231   97,1 %  SUMNA
ticki.nad25ms                              6         7         6         6         1   16,7 %  stabilna
ticki.nad50ms                              0         0         0         0         0      n/a  enaka
tps                                       20        20        20        20         0    0,0 %  enaka
```

### Sumne velicine

- `alok.KBnaTick`: mediana 4.577, razpon 2.596 (56,7 %)
- `alok.MBnaS`: mediana 89,400, razpon 50,700 (56,7 %)
- `gc.ms`: mediana 164, razpon 82 (50,0 %)
- `gc.msNaS`: mediana 0,540, razpon 0,270 (50,0 %)
- `gc.zbirk`: mediana 60, razpon 37 (61,7 %)
- `mspt.p99`: mediana 11,272, razpon 2,359 (20,9 %)
- `rwtarget.predzavrnjenih`: mediana 2.158, razpon 974 (45,1 %)
- `ticki.nad10ms`: mediana 238, razpon 231 (97,1 %)
