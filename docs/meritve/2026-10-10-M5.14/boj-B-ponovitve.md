# M2.5c - ponovitve scenarija perf (M2.4), 2026-10-10 18:12

Protokol: `docs/scenariji/M2.5c-ponovitve.md`. Vsaka ponovitev je svoj proces in
svez svet (reset z .\testworld.ps1).

Merila: T1-T6 zelena

## Zagoni

| # | izhodna koda | minut | zapis |
|---|---|---|---|
| 1 | 0 | 5,8 | `m24-perf-2026-10-10-1755-p1.json` |
| 2 | 0 | 5,7 | `m24-perf-2026-10-10-1755-p2.json` |
| 3 | 0 | 5,8 | `m24-perf-2026-10-10-1755-p3.json` |

## Odtis (mora biti enak v vseh ponovitvah)

```
celic                1
celica               1
mrezaSirina          25
mrezaX               48
mrezaZ               -12
npc                  500
obroc                1
ogrevanje            90
razprseno            0
rwdata               1
rwtarget             2
sekund               180
varianta             boj
```

## Sumni pas

Prag: relativni razpon <= 20 %. Velicina, oznacena kot SUMNA, se med ponovitvami
premakne bolj kot prag, zato A/B primerjava na njej potrebuje razliko, vecjo od stolpca
`razpon` - manjsa razlika je sum, ne izboljsava.

```
velicina                                  p1        p2        p3   mediana    razpon      rel  oznaka
-----------------------------------------------------------------------------------------------------
alok.KBnaTick                          4.598     1.859     3.896     3.896     2.739   70,3 %  SUMNA
alok.MBnaS                            89,800    36,300    76,100    76,100    53,500   70,3 %  SUMNA
boj.ranjenih                             500       500       500       500         0    0,0 %  enaka
boj.sCiljem                              500       500       500       500         0    0,0 %  enaka
gc.ms                                     81        44        73        73        37   50,7 %  SUMNA
gc.msNaS                               0,450     0,240     0,400     0,400     0,210   52,5 %  SUMNA
gc.old.ms                                  0         0         0         0         0      n/a  enaka
gc.old.zbirk                               0         0         0         0         0      n/a  enaka
gc.zbirk                                  28        12        24        24        16   66,7 %  SUMNA
heap.maxMB                             1.820     1.842     1.820     1.820        22    1,2 %  stabilna
mspt.brezSave.max                     18,639    19,708    21,144    19,708     2,505   12,7 %  stabilna
mspt.brezSave.p99                     12,059     9,699    10,486    10,486     2,360   22,5 %  SUMNA
mspt.max                              29,900    28,935    30,844    29,900     1,909    6,4 %  stabilna
mspt.p50                               9,175     7,471     7,733     7,733     1,704   22,0 %  SUMNA
mspt.p95                              10,486     8,651     9,175     9,175     1,835   20,0 %  SUMNA
mspt.p99                              12,059     9,961    10,748    10,748     2,098   19,5 %  stabilna
mspt.povp                              9,155     7,564     7,880     7,880     1,591   20,2 %  SUMNA
nasiceno                                   0         0         0         0         0      n/a  enaka
npc.per.tick.p50                         500       500       500       500         0    0,0 %  enaka
npc.us                                18,100    14,940    15,540    15,540     3,160   20,3 %  SUMNA
rwdata.branj                      40.959.01340.445.97240.893.23140.893.231   513.041    1,3 %  stabilna
rwdata.branjNaTick                    11.290    11.142    11.265    11.265   147,600    1,3 %  stabilna
rwdata.neujemanj                           0         0         0         0         0      n/a  enaka
rwdata.primerjav                           0         0         0         0         0      n/a  enaka
rwdata.primerjavNaTick                     0         0         0         0         0      n/a  enaka
rwtarget.predzavrnjenih                8.532     5.907     4.292     5.907     4.240   71,8 %  SUMNA
rwtarget.zozenih                           0         0         0         0         0      n/a  enaka
ticki                                  3.628     3.630     3.630     3.630         2    0,1 %  stabilna
ticki.nad100ms                             0         0         0         0         0      n/a  enaka
ticki.nad10ms                            395        32        52        52       363  698,1 %  SUMNA
ticki.nad25ms                              4         4         4         4         0    0,0 %  enaka
ticki.nad50ms                              0         0         0         0         0      n/a  enaka
tps                                   19,990        20        20        20     0,010    0,1 %  stabilna
```

### Sumne velicine

- `alok.KBnaTick`: mediana 3.896, razpon 2.739 (70,3 %)
- `alok.MBnaS`: mediana 76,100, razpon 53,500 (70,3 %)
- `gc.ms`: mediana 73, razpon 37 (50,7 %)
- `gc.msNaS`: mediana 0,400, razpon 0,210 (52,5 %)
- `gc.zbirk`: mediana 24, razpon 16 (66,7 %)
- `mspt.brezSave.p99`: mediana 10,486, razpon 2,360 (22,5 %)
- `mspt.p50`: mediana 7,733, razpon 1,704 (22,0 %)
- `mspt.p95`: mediana 9,175, razpon 1,835 (20,0 %)
- `mspt.povp`: mediana 7,880, razpon 1,591 (20,2 %)
- `npc.us`: mediana 15,540, razpon 3,160 (20,3 %)
- `rwtarget.predzavrnjenih`: mediana 5.907, razpon 4.240 (71,8 %)
- `ticki.nad10ms`: mediana 52, razpon 363 (698,1 %)
