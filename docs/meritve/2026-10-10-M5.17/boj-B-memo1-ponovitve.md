# M2.5c - ponovitve scenarija perf (M2.4), 2026-10-10 15:42

Protokol: `docs/scenariji/M2.5c-ponovitve.md`. Vsaka ponovitev je svoj proces in
svez svet (reset z .\testworld.ps1).

Merila: T1-T6 zelena

## Zagoni

| # | izhodna koda | minut | zapis |
|---|---|---|---|
| 1 | 0 | 5,7 | `m24-perf-2026-10-10-1524-p1.json` |
| 2 | 0 | 5,7 | `m24-perf-2026-10-10-1524-p2.json` |
| 3 | 0 | 5,8 | `m24-perf-2026-10-10-1524-p3.json` |

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
rwpath               0
rwpathMemo           1
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
alok.KBnaTick                          3.769     4.652     4.664     4.652   894,900   19,2 %  stabilna
alok.MBnaS                            73,600    90,800    91,100    90,800    17,500   19,3 %  stabilna
boj.ranjenih                             500       500       500       500         0    0,0 %  enaka
boj.sCiljem                              500       500       500       500         0    0,0 %  enaka
gc.ms                                     72        83        82        82        11   13,4 %  stabilna
gc.msNaS                               0,400     0,460     0,450     0,450     0,060   13,3 %  stabilna
gc.old.ms                                  0         0         0         0         0      n/a  enaka
gc.old.zbirk                               0         0         0         0         0      n/a  enaka
gc.zbirk                                  24        29        28        28         5   17,9 %  stabilna
heap.maxMB                             1.820     1.862     1.834     1.834        42    2,3 %  stabilna
mspt.brezSave.max                     13,931    42,690    28,522    28,522    28,759  100,8 %  SUMNA
mspt.brezSave.p99                     10,224    13,894    14,680    13,894     4,456   32,1 %  SUMNA
mspt.max                              35,890    42,690    33,642    35,890     9,048   25,2 %  SUMNA
mspt.p50                               7,864     9,437    10,224     9,437     2,360   25,0 %  SUMNA
mspt.p95                               9,437    11,010    12,059    11,010     2,622   23,8 %  SUMNA
mspt.p99                              10,224    14,418    14,942    14,418     4,718   32,7 %  SUMNA
mspt.povp                              7,963     9,424    10,205     9,424     2,242   23,8 %  SUMNA
nasiceno                                   0         0         0         0         0      n/a  enaka
npc.per.tick.p50                         500       500       500       500         0    0,0 %  enaka
npc.us                                15,760    18,630    20,130    18,630     4,370   23,5 %  SUMNA
rwpath.izracunov                           0         0         0         0         0      n/a  enaka
rwpath.klicev                              0         0         0         0         0      n/a  enaka
rwpath.memo.delezZadetkov             35,390    35,420    35,360    35,390     0,060    0,2 %  stabilna
rwpath.memo.iskanj                   267.214   268.333   265.666   267.214     2.667    1,0 %  stabilna
rwpath.memo.izracunov              3.558.793 3.589.279 3.577.928 3.577.928    30.486    0,9 %  stabilna
rwpath.memo.izracunovNaIskanje        13,300    13,400    13,500    13,400     0,200    1,5 %  stabilna
rwpath.memo.neujemanj                      0         0         0         0         0      n/a  enaka
rwpath.memo.ocen                   5.508.514 5.558.242 5.535.534 5.535.534    49.728    0,9 %  stabilna
rwpath.memo.ocenNaIskanje             20,600    20,700    20,800    20,700     0,200    1,0 %  stabilna
rwpath.neujemanj                           0         0         0         0         0      n/a  enaka
rwpath.ocen                                0         0         0         0         0      n/a  enaka
rwpath.primerjav                           0         0         0         0         0      n/a  enaka
rwtarget.predzavrnjenih                7.804     2.820     8.591     7.804     5.771   73,9 %  SUMNA
rwtarget.zozenih                           0         0         0         0         0      n/a  enaka
ticki                                  3.630     3.627     3.629     3.629         3    0,1 %  stabilna
ticki.nad100ms                             0         0         0         0         0      n/a  enaka
ticki.nad10ms                             46       617     1.868       617     1.822  295,3 %  SUMNA
ticki.nad25ms                              4         5         7         5         3   60,0 %  SUMNA
ticki.nad50ms                              0         0         0         0         0      n/a  enaka
tps                                   19,990    19,990    19,990    19,990         0    0,0 %  enaka
```

### Sumne velicine

- `mspt.brezSave.max`: mediana 28,522, razpon 28,759 (100,8 %)
- `mspt.brezSave.p99`: mediana 13,894, razpon 4,456 (32,1 %)
- `mspt.max`: mediana 35,890, razpon 9,048 (25,2 %)
- `mspt.p50`: mediana 9,437, razpon 2,360 (25,0 %)
- `mspt.p95`: mediana 11,010, razpon 2,622 (23,8 %)
- `mspt.p99`: mediana 14,418, razpon 4,718 (32,7 %)
- `mspt.povp`: mediana 9,424, razpon 2,242 (23,8 %)
- `npc.us`: mediana 18,630, razpon 4,370 (23,5 %)
- `rwtarget.predzavrnjenih`: mediana 7.804, razpon 5.771 (73,9 %)
- `ticki.nad10ms`: mediana 617, razpon 1.822 (295,3 %)
- `ticki.nad25ms`: mediana 5, razpon 3 (60,0 %)
