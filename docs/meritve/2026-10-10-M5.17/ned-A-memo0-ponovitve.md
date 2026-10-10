# M2.5c - ponovitve scenarija perf (M2.4), 2026-10-10 16:07

Protokol: `docs/scenariji/M2.5c-ponovitve.md`. Vsaka ponovitev je svoj proces in
svez svet (reset z .\testworld.ps1).

Merila: T1-T6 zelena

## Zagoni

| # | izhodna koda | minut | zapis |
|---|---|---|---|
| 1 | 0 | 8,4 | `m24-perf-2026-10-10-1542-p1.json` |
| 2 | 0 | 8,3 | `m24-perf-2026-10-10-1542-p2.json` |
| 3 | 0 | 8,4 | `m24-perf-2026-10-10-1542-p3.json` |

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
rwpathMemo           0
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
alok.KBnaTick                         14.929    14.907    14.932    14.929    24,200    0,2 %  stabilna
alok.MBnaS                           291,500   291,100   291,600   291,500     0,500    0,2 %  stabilna
boj.ranjenih                               0         0         0         0         0      n/a  enaka
boj.sCiljem                              500       500       500       500         0    0,0 %  enaka
gc.ms                                    780       715       755       755        65    8,6 %  stabilna
gc.msNaS                               2,580     2,370     2,490     2,490     0,210    8,4 %  stabilna
gc.old.ms                                  0         0         0         0         0      n/a  enaka
gc.old.zbirk                               0         0         0         0         0      n/a  enaka
gc.zbirk                                 172       147       155       155        25   16,1 %  stabilna
heap.maxMB                             1.820     1.820     1.820     1.820         0    0,0 %  enaka
mspt.brezSave.max                     37,490    60,885    31,910    37,490    28,975   77,3 %  SUMNA
mspt.brezSave.p99                     23,593    26,214    24,642    24,642     2,621   10,6 %  stabilna
mspt.max                              37,490    60,885    35,326    37,490    25,559   68,2 %  SUMNA
mspt.p50                              17,302    17,826    17,826    17,826     0,524    2,9 %  stabilna
mspt.p95                              21,496    23,069    22,020    22,020     1,573    7,1 %  stabilna
mspt.p99                              23,593    26,739    24,642    24,642     3,146   12,8 %  stabilna
mspt.povp                             17,366    18,167    17,959    17,959     0,801    4,5 %  stabilna
nasiceno                                   0         0         0         0         0      n/a  enaka
npc.per.tick.p50                         500       500       500       500         0    0,0 %  enaka
npc.us                                34,590    36,140    35,770    35,770     1,550    4,3 %  stabilna
rwpath.izracunov                           0         0         0         0         0      n/a  enaka
rwpath.klicev                              0         0         0         0         0      n/a  enaka
rwpath.memo.iskanj                         0         0         0         0         0      n/a  enaka
rwpath.memo.izracunov                      0         0         0         0         0      n/a  enaka
rwpath.memo.neujemanj                      0         0         0         0         0      n/a  enaka
rwpath.memo.ocen                           0         0         0         0         0      n/a  enaka
rwpath.neujemanj                           0         0         0         0         0      n/a  enaka
rwpath.ocen                                0         0         0         0         0      n/a  enaka
rwpath.primerjav                           0         0         0         0         0      n/a  enaka
rwtarget.predzavrnjenih                    0         0         0         0         0      n/a  enaka
rwtarget.zozenih                           0         0         0         0         0      n/a  enaka
ticki                                  6.048     6.025     6.052     6.048        27    0,4 %  stabilna
ticki.nad100ms                             0         0         0         0         0      n/a  enaka
ticki.nad10ms                          6.048     6.025     6.052     6.048        27    0,4 %  stabilna
ticki.nad25ms                             33        91        48        48        58  120,8 %  SUMNA
ticki.nad50ms                              0         1         0         0         1      n/a  SUMNA
tps                                       20        20        20        20         0    0,0 %  enaka
```

### Sumne velicine

- `mspt.brezSave.max`: mediana 37,490, razpon 28,975 (77,3 %)
- `mspt.max`: mediana 37,490, razpon 25,559 (68,2 %)
- `ticki.nad25ms`: mediana 48, razpon 58 (120,8 %)
- `ticki.nad50ms`: mediana 0, razpon 1 (mediana je 0, razmerje ne obstaja)
