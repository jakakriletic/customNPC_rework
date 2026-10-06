# M2.5c - ponovitve scenarija perf (M2.4), 2026-10-06 10:02

Protokol: `docs/scenariji/M2.5c-ponovitve.md`. Vsaka ponovitev je svoj proces in
svez svet (reset z .\testworld.ps1).

Merila: T1-T6 zelena

## Zagoni

| # | izhodna koda | minut | zapis |
|---|---|---|---|
| 1 | 0 | 8,1 | `m24-perf-2026-10-06-0938-p1.json` |
| 2 | 0 | 8,1 | `m24-perf-2026-10-06-0938-p2.json` |
| 3 | 0 | 8,0 | `m24-perf-2026-10-06-0938-p3.json` |

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
varianta             boj
```

## Sumni pas

Prag: relativni razpon <= 20 %. Velicina, oznacena kot SUMNA, se med ponovitvami
premakne bolj kot prag, zato A/B primerjava na njej potrebuje razliko, vecjo od stolpca
`razpon` - manjsa razlika je sum, ne izboljsava.

```
velicina                                  p1        p2        p3   mediana    razpon      rel  oznaka
-----------------------------------------------------------------------------------------------------
alok.KBnaTick                          4.744     4.693     4.541     4.693   202,500    4,3 %  stabilna
alok.MBnaS                            92,600    91,600    88,700    91,600     3,900    4,3 %  stabilna
boj.ranjenih                             500       500       500       500         0    0,0 %  enaka
boj.sCiljem                              500       500       500       500         0    0,0 %  enaka
gc.ms                                    153       171       162       162        18   11,1 %  stabilna
gc.msNaS                               0,510     0,570     0,540     0,540     0,060   11,1 %  stabilna
gc.old.ms                                  0         0         0         0         0      n/a  enaka
gc.old.zbirk                               0         0         0         0         0      n/a  enaka
gc.zbirk                                  57        65        57        57         8   14,0 %  stabilna
heap.maxMB                             1.820     1.820     1.820     1.820         0    0,0 %  enaka
mspt.brezSave.max                     19,692    19,120    17,912    19,120     1,780    9,3 %  stabilna
mspt.brezSave.p99                     12,321    12,059    12,321    12,321     0,262    2,1 %  stabilna
mspt.max                              31,803    31,593    30,749    31,593     1,054    3,3 %  stabilna
mspt.p50                               9,175     9,175     9,175     9,175         0    0,0 %  enaka
mspt.p95                              10,748    10,486    10,486    10,486     0,262    2,5 %  stabilna
mspt.p99                              12,321    12,059    12,583    12,321     0,524    4,3 %  stabilna
mspt.povp                              9,172     9,147     9,098     9,147     0,074    0,8 %  stabilna
nasiceno                                   0         0         0         0         0      n/a  enaka
npc.per.tick.p50                         500       500       500       500         0    0,0 %  enaka
npc.us                                18,120    18,110        18    18,110     0,120    0,7 %  stabilna
rwtarget.predzavrnjenih                    0         0         0         0         0      n/a  enaka
ticki                                  6.017     6.035     6.032     6.032        18    0,3 %  stabilna
ticki.nad100ms                             0         0         0         0         0      n/a  enaka
ticki.nad10ms                            790       595       600       600       195   32,5 %  SUMNA
ticki.nad25ms                              6         6         6         6         0    0,0 %  enaka
ticki.nad50ms                              0         0         0         0         0      n/a  enaka
tps                                       20        20        20        20         0    0,0 %  enaka
```

### Sumne velicine

- `ticki.nad10ms`: mediana 600, razpon 195 (32,5 %)
