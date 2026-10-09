# M2.5c - ponovitve scenarija perf (M2.4), 2026-10-09 20:05

Protokol: `docs/scenariji/M2.5c-ponovitve.md`. Vsaka ponovitev je svoj proces in
svez svet (reset z .\testworld.ps1).

Merila: T1-T6 zelena

## Zagoni

| # | izhodna koda | minut | zapis |
|---|---|---|---|
| 1 | 0 | 8,2 | `m24-perf-2026-10-09-1940-p1.json` |
| 2 | 0 | 8,2 | `m24-perf-2026-10-09-1940-p2.json` |
| 3 | 0 | 8,2 | `m24-perf-2026-10-09-1940-p3.json` |

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
rwpath               1
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
alok.KBnaTick                          2.879     2.968     2.977     2.968    97,800    3,3 %  stabilna
alok.MBnaS                            56,200        58    58,100        58     1,900    3,3 %  stabilna
boj.ranjenih                             500       500       500       500         0    0,0 %  enaka
boj.sCiljem                              500       500       500       500         0    0,0 %  enaka
gc.ms                                    119       114       108       114        11    9,6 %  stabilna
gc.msNaS                               0,390     0,380     0,360     0,380     0,030    7,9 %  stabilna
gc.old.ms                                  0         0         0         0         0      n/a  enaka
gc.old.zbirk                               0         0         0         0         0      n/a  enaka
gc.zbirk                                  37        36        34        36         3    8,3 %  stabilna
heap.maxMB                             1.820     1.820     1.820     1.820         0    0,0 %  enaka
mspt.brezSave.max                     11,992    14,170    11,570    11,992     2,600   21,7 %  SUMNA
mspt.brezSave.p99                      8,389     8,389     8,389     8,389         0    0,0 %  enaka
mspt.max                              36,227    33,303    37,021    36,227     3,718   10,3 %  stabilna
mspt.p50                               5,767     5,636     5,767     5,767     0,131    2,3 %  stabilna
mspt.p95                               7,209     7,209     7,209     7,209         0    0,0 %  enaka
mspt.p99                               8,651     8,651     8,651     8,651         0    0,0 %  enaka
mspt.povp                              5,936     5,863     5,881     5,881     0,073    1,2 %  stabilna
nasiceno                                   0         0         0         0         0      n/a  enaka
npc.per.tick.p50                         500       500       500       500         0    0,0 %  enaka
npc.us                                11,730    11,570    11,620    11,620     0,160    1,4 %  stabilna
rwpath.hist.0                         69.615    69.851    69.482    69.615       369    0,5 %  stabilna
rwpath.hist.1                      1.529.512 1.518.734 1.532.131 1.529.512    13.397    0,9 %  stabilna
rwpath.hist.2                              0         0         0         0         0      n/a  enaka
rwpath.hist.3                              0         0         0         0         0      n/a  enaka
rwpath.hist.4                              0         0         0         0         0      n/a  enaka
rwpath.hist.5                              0         0         0         0         0      n/a  enaka
rwpath.izracunov                  85.300.49484.678.87085.477.18585.300.494   798.315    0,9 %  stabilna
rwpath.kandidat.delez                 33,600    33,800    34,200    33,800     0,600    1,8 %  stabilna
rwpath.kandidat.msNaTick               1,994     1,984     2,011     1,994     0,027    1,4 %  stabilna
rwpath.kandidat.usNaKlic               7,960     7,950     8,020     7,960     0,070    0,9 %  stabilna
rwpath.kandidatov                  1.529.512 1.518.734 1.532.131 1.529.512    13.397    0,9 %  stabilna
rwpath.kandidatovNaSledenje            0,956     0,956     0,957     0,956     0,001    0,1 %  stabilna
rwpath.kandidatovNaTick              250,300   249,500   250,700   250,300     1,200    0,5 %  stabilna
rwpath.klicev                      1.529.512 1.518.734 1.532.131 1.529.512    13.397    0,9 %  stabilna
rwpath.neujemanj                           0         0         0         0         0      n/a  enaka
rwpath.ocen                       258.482.148256.628.320259.090.764258.482.148 2.462.444    1,0 %  stabilna
rwpath.primerjav                           0         0         0         0         0      n/a  enaka
rwpath.prostih                     1.529.512 1.518.734 1.532.131 1.529.512    13.397    0,9 %  stabilna
rwpath.sledenj                     1.599.127 1.588.585 1.601.613 1.599.127    13.028    0,8 %  stabilna
rwpath.sledenje.delez                 34,200    34,500    34,800    34,500     0,600    1,7 %  stabilna
rwpath.sledenje.msNaTick               2,030     2,020     2,049     2,030     0,029    1,4 %  stabilna
rwpath.sledenje.usNaKlic               7,760     7,740     7,820     7,760     0,080    1,0 %  stabilna
rwpath.sledenjNaTick                 261,700       261   262,100   261,700     1,100    0,4 %  stabilna
rwpath.tickov                          6.110     6.086     6.111     6.110        25    0,4 %  stabilna
rwtarget.predzavrnjenih                1.359       848     2.748     1.359     1.900  139,8 %  SUMNA
rwtarget.zozenih                           0         0         0         0         0      n/a  enaka
ticki                                  6.029     6.025     6.028     6.028         4    0,1 %  stabilna
ticki.nad100ms                             0         0         0         0         0      n/a  enaka
ticki.nad10ms                             18        18        15        18         3   16,7 %  stabilna
ticki.nad25ms                              3         6         7         6         4   66,7 %  SUMNA
ticki.nad50ms                              0         0         0         0         0      n/a  enaka
tps                                       20        20        20        20         0    0,0 %  enaka
```

### Sumne velicine

- `mspt.brezSave.max`: mediana 11,992, razpon 2,600 (21,7 %)
- `rwtarget.predzavrnjenih`: mediana 1.359, razpon 1.900 (139,8 %)
- `ticki.nad25ms`: mediana 6, razpon 4 (66,7 %)
