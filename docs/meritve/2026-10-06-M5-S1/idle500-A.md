# M2.5c - ponovitve scenarija perf (M2.4), 2026-10-06 09:13

Protokol: `docs/scenariji/M2.5c-ponovitve.md`. Vsaka ponovitev je svoj proces in
svez svet (reset z .\testworld.ps1).

Merila: T1-T6 zelena

## Zagoni

| # | izhodna koda | minut | zapis |
|---|---|---|---|
| 1 | 0 | 8,3 | `m24-perf-2026-10-06-0849-p1.json` |
| 2 | 0 | 8,2 | `m24-perf-2026-10-06-0849-p2.json` |
| 3 | 0 | 8,1 | `m24-perf-2026-10-06-0849-p3.json` |

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
rwtarget             0
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
alok.KBnaTick                         17.539    17.531    17.524    17.531    15,600    0,1 %  stabilna
alok.MBnaS                           342,500   342,400   342,200   342,400     0,300    0,1 %  stabilna
gc.ms                                    762       689       814       762       125   16,4 %  stabilna
gc.msNaS                               2,520     2,280     2,690     2,520     0,410   16,3 %  stabilna
gc.old.ms                                  0         0         0         0         0      n/a  enaka
gc.old.zbirk                               0         0         0         0         0      n/a  enaka
gc.zbirk                                 452       419       469       452        50   11,1 %  stabilna
heap.maxMB                             1.820     1.820     1.820     1.820         0    0,0 %  enaka
mspt.brezSave.max                     79,433    48,636    47,502    48,636    31,931   65,7 %  SUMNA
mspt.brezSave.p99                     47,186    44,040    45,089    45,089     3,146    7,0 %  stabilna
mspt.max                              79,433    63,678    47,502    63,678    31,931   50,1 %  SUMNA
mspt.p50                               3,473     3,408     3,408     3,408     0,065    1,9 %  stabilna
mspt.p95                              44,040    41,943    41,943    41,943     2,097    5,0 %  stabilna
mspt.p99                              47,186    44,040    45,089    45,089     3,146    7,0 %  stabilna
mspt.povp                             15,258    14,505    14,565    14,565     0,753    5,2 %  stabilna
nasiceno                                   0         0         0         0         0      n/a  enaka
npc.per.tick.p50                         500       500       500       500         0    0,0 %  enaka
npc.us                                30,420    28,900    29,020    29,020     1,520    5,2 %  stabilna
rwtarget.predzavrnjenih                   -1        -1         0        -1         1  100,0 %  SUMNA
ticki                                  6.049     6.048     6.050     6.049         2    0,0 %  stabilna
ticki.nad100ms                             0         0         0         0         0      n/a  enaka
ticki.nad10ms                          2.024     2.016     2.022     2.022         8    0,4 %  stabilna
ticki.nad25ms                          2.016     2.016     2.016     2.016         0    0,0 %  enaka
ticki.nad50ms                             10         6         0         6        10  166,7 %  SUMNA
tps                                       20        20        20        20         0    0,0 %  enaka
```

### Sumne velicine

- `mspt.brezSave.max`: mediana 48,636, razpon 31,931 (65,7 %)
- `mspt.max`: mediana 63,678, razpon 31,931 (50,1 %)
- `rwtarget.predzavrnjenih`: mediana -1, razpon 1 (100,0 %)
- `ticki.nad50ms`: mediana 6, razpon 10 (166,7 %)
