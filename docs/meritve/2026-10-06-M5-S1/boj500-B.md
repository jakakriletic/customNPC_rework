# M2.5c - ponovitve scenarija perf (M2.4), 2026-10-06 10:26

Protokol: `docs/scenariji/M2.5c-ponovitve.md`. Vsaka ponovitev je svoj proces in
svez svet (reset z .\testworld.ps1).

Merila: T1-T6 zelena

## Zagoni

| # | izhodna koda | minut | zapis |
|---|---|---|---|
| 1 | 0 | 8,1 | `m24-perf-2026-10-06-1002-p1.json` |
| 2 | 0 | 8,1 | `m24-perf-2026-10-06-1002-p2.json` |
| 3 | 0 | 8,1 | `m24-perf-2026-10-06-1002-p3.json` |

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
alok.KBnaTick                          4.443     4.701     4.666     4.666   257,500    5,5 %  stabilna
alok.MBnaS                            86,800    91,800    91,100    91,100         5    5,5 %  stabilna
boj.ranjenih                             500       500       500       500         0    0,0 %  enaka
boj.sCiljem                              500       500       500       500         0    0,0 %  enaka
gc.ms                                    143       146       159       146        16   11,0 %  stabilna
gc.msNaS                               0,470     0,480     0,530     0,480     0,060   12,5 %  stabilna
gc.old.ms                                  0         0         0         0         0      n/a  enaka
gc.old.zbirk                               0         0         0         0         0      n/a  enaka
gc.zbirk                                  54        57        60        57         6   10,5 %  stabilna
heap.maxMB                             1.820     1.820     1.820     1.820         0    0,0 %  enaka
mspt.brezSave.max                     24,105    17,041    19,782    19,782     7,064   35,7 %  SUMNA
mspt.brezSave.p99                     11,796    11,796    11,534    11,796     0,262    2,2 %  stabilna
mspt.max                              37,743    36,350    29,884    36,350     7,859   21,6 %  SUMNA
mspt.p50                               9,175     9,437     8,651     9,175     0,786    8,6 %  stabilna
mspt.p95                              10,486    10,486     9,961    10,486     0,525    5,0 %  stabilna
mspt.p99                              11,796    11,796    11,534    11,796     0,262    2,2 %  stabilna
mspt.povp                              9,255     9,343     8,735     9,255     0,608    6,6 %  stabilna
nasiceno                                   0         0         0         0         0      n/a  enaka
npc.per.tick.p50                         500       500       500       500         0    0,0 %  enaka
npc.us                                18,340    18,510    17,290    18,340     1,220    6,7 %  stabilna
rwtarget.predzavrnjenih                1.370     4.343         0     1.370     4.343  317,0 %  SUMNA
ticki                                  6.035     6.033     6.020     6.033        15    0,2 %  stabilna
ticki.nad100ms                             0         0         0         0         0      n/a  enaka
ticki.nad10ms                            667       790       264       667       526   78,9 %  SUMNA
ticki.nad25ms                              7         7         6         7         1   14,3 %  stabilna
ticki.nad50ms                              0         0         0         0         0      n/a  enaka
tps                                       20        20        20        20         0    0,0 %  enaka
```

### Sumne velicine

- `mspt.brezSave.max`: mediana 19,782, razpon 7,064 (35,7 %)
- `mspt.max`: mediana 36,350, razpon 7,859 (21,6 %)
- `rwtarget.predzavrnjenih`: mediana 1.370, razpon 4.343 (317,0 %)
- `ticki.nad10ms`: mediana 667, razpon 526 (78,9 %)
