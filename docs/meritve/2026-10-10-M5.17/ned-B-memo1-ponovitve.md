# M2.5c - ponovitve scenarija perf (M2.4), 2026-10-10 16:32

Protokol: `docs/scenariji/M2.5c-ponovitve.md`. Vsaka ponovitev je svoj proces in
svez svet (reset z .\testworld.ps1).

Merila: T1-T6 zelena

## Zagoni

| # | izhodna koda | minut | zapis |
|---|---|---|---|
| 1 | 0 | 8,3 | `m24-perf-2026-10-10-1607-p1.json` |
| 2 | 0 | 8,3 | `m24-perf-2026-10-10-1607-p2.json` |
| 3 | 0 | 8,3 | `m24-perf-2026-10-10-1607-p3.json` |

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
rwpathMemo           1
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
alok.KBnaTick                          8.336    11.583     6.938     8.336     4.645   55,7 %  SUMNA
alok.MBnaS                           162,800   226,200   135,500   162,800    90,700   55,7 %  SUMNA
boj.ranjenih                               0         0         0         0         0      n/a  enaka
boj.sCiljem                              500       500       500       500         0    0,0 %  enaka
gc.ms                                    518       677       419       518       258   49,8 %  SUMNA
gc.msNaS                               1,720     2,240     1,390     1,720     0,850   49,4 %  SUMNA
gc.old.ms                                  0         0         0         0         0      n/a  enaka
gc.old.zbirk                               0         0         0         0         0      n/a  enaka
gc.zbirk                                 110       147        79       110        68   61,8 %  SUMNA
heap.maxMB                             1.820     1.820     1.820     1.820         0    0,0 %  enaka
mspt.brezSave.max                     32,466    29,381    31,795    31,795     3,085    9,7 %  stabilna
mspt.brezSave.p99                     21,496    23,069    21,496    21,496     1,573    7,3 %  stabilna
mspt.max                              36,414    33,442    36,061    36,061     2,972    8,2 %  stabilna
mspt.p50                              15,204    16,253    14,680    15,204     1,573   10,3 %  stabilna
mspt.p95                              18,874    20,447    18,350    18,874     2,097   11,1 %  stabilna
mspt.p99                              22,020    23,069    22,020    22,020     1,049    4,8 %  stabilna
mspt.povp                             15,253    16,486    14,802    15,253     1,684   11,0 %  stabilna
nasiceno                                   0         0         0         0         0      n/a  enaka
npc.per.tick.p50                         500       500       500       500         0    0,0 %  enaka
npc.us                                30,360    32,830    29,480    30,360     3,350   11,0 %  stabilna
rwpath.izracunov                           0         0         0         0         0      n/a  enaka
rwpath.klicev                              0         0         0         0         0      n/a  enaka
rwpath.memo.delezZadetkov             52,510    52,610    52,500    52,510     0,110    0,2 %  stabilna
rwpath.memo.iskanj                   446.087   449.638   443.380   446.087     6.258    1,4 %  stabilna
rwpath.memo.izracunov             289.388.122295.021.782279.272.146289.388.12215.749.636    5,4 %  stabilna
rwpath.memo.izracunovNaIskanje       648,700   656,100   629,900   648,700    26,200    4,0 %  stabilna
rwpath.memo.neujemanj                      0         0         0         0         0      n/a  enaka
rwpath.memo.ocen                  609.423.798622.528.440587.902.012609.423.79834.626.428    5,7 %  stabilna
rwpath.memo.ocenNaIskanje              1.366     1.385     1.326     1.366    58,500    4,3 %  stabilna
rwpath.neujemanj                           0         0         0         0         0      n/a  enaka
rwpath.ocen                                0         0         0         0         0      n/a  enaka
rwpath.primerjav                           0         0         0         0         0      n/a  enaka
rwtarget.predzavrnjenih                    0         0         0         0         0      n/a  enaka
rwtarget.zozenih                           0         0         0         0         0      n/a  enaka
ticki                                  6.016     6.035     6.020     6.020        19    0,3 %  stabilna
ticki.nad100ms                             0         0         0         0         0      n/a  enaka
ticki.nad10ms                          6.016     6.035     6.019     6.019        19    0,3 %  stabilna
ticki.nad25ms                             25        18        24        24         7   29,2 %  SUMNA
ticki.nad50ms                              0         0         0         0         0      n/a  enaka
tps                                       20        20        20        20         0    0,0 %  enaka
```

### Sumne velicine

- `alok.KBnaTick`: mediana 8.336, razpon 4.645 (55,7 %)
- `alok.MBnaS`: mediana 162,800, razpon 90,700 (55,7 %)
- `gc.ms`: mediana 518, razpon 258 (49,8 %)
- `gc.msNaS`: mediana 1,720, razpon 0,850 (49,4 %)
- `gc.zbirk`: mediana 110, razpon 68 (61,8 %)
- `ticki.nad25ms`: mediana 24, razpon 7 (29,2 %)
