# M2.5c - ponovitve scenarija perf (M2.4), 2026-10-09 19:03

Protokol: `docs/scenariji/M2.5c-ponovitve.md`. Vsaka ponovitev je svoj proces in
svez svet (reset z .\testworld.ps1).

Merila: T1-T6 zelena

## Zagoni

| # | izhodna koda | minut | zapis |
|---|---|---|---|
| 1 | 0 | 8,4 | `m24-perf-2026-10-09-1837-p1.json` |
| 2 | 0 | 8,5 | `m24-perf-2026-10-09-1837-p2.json` |
| 3 | 0 | 8,5 | `m24-perf-2026-10-09-1837-p3.json` |

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
rwpathAB             0,3,1
rwpathCas            1
rwpathOkno           20
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
alok.KBnaTick                          4.113     4.093     4.062     4.093    50,900    1,2 %  stabilna
alok.MBnaS                            80,300    79,900    79,300    79,900         1    1,3 %  stabilna
boj.ranjenih                             500       500       500       500         0    0,0 %  enaka
boj.sCiljem                              500       500       500       500         0    0,0 %  enaka
gc.ms                                    145       147       168       147        23   15,6 %  stabilna
gc.msNaS                               0,460     0,460     0,530     0,460     0,070   15,2 %  stabilna
gc.old.ms                                  0         0         0         0         0      n/a  enaka
gc.old.zbirk                               0         0         0         0         0      n/a  enaka
gc.zbirk                                  51        53        56        53         5    9,4 %  stabilna
heap.maxMB                             1.820     1.820     1.820     1.820         0    0,0 %  enaka
mspt.brezSave.max                     17,535    17,624    16,878    17,535     0,746    4,3 %  stabilna
mspt.brezSave.p99                     10,486    12,845    10,748    10,748     2,359   21,9 %  SUMNA
mspt.max                              36,513    35,546    29,765    35,546     6,748   19,0 %  stabilna
mspt.p50                               8,258     8,258     8,258     8,258         0    0,0 %  enaka
mspt.p95                               9,699    11,796     9,961     9,961     2,097   21,1 %  SUMNA
mspt.p99                              10,748    12,845    11,010    11,010     2,097   19,0 %  stabilna
mspt.povp                              7,711     7,975     7,858     7,858     0,264    3,4 %  stabilna
nasiceno                                   0         0         0         0         0      n/a  enaka
npc.per.tick.p50                         500       500       500       500         0    0,0 %  enaka
npc.us                                15,280    15,810    15,560    15,560     0,530    3,4 %  stabilna
rwpath.ab.0.kandidat.msNaTick          4,881     5,607     5,234     5,234     0,726   13,9 %  stabilna
rwpath.ab.0.kandidat.usNaKlic         19,557    22,658    20,966    20,966     3,101   14,8 %  stabilna
rwpath.ab.0.kandidatov               534.141   524.403   533.796   533.796     9.738    1,8 %  stabilna
rwpath.ab.0.kandidatovNaTick         249,600   247,480   249,670   249,600     2,190    0,9 %  stabilna
rwpath.ab.0.sledenje.msNaTick          4,919     5,644     5,278     5,278     0,725   13,7 %  stabilna
rwpath.ab.0.sledenjNaTick            261,110   257,890   260,530   260,530     3,220    1,2 %  stabilna
rwpath.ab.0.tickov                     2.140     2.119     2.138     2.138        21    1,0 %  stabilna
rwpath.ab.1.kandidat.msNaTick          4,691     4,662     4,767     4,691     0,105    2,2 %  stabilna
rwpath.ab.1.kandidat.usNaKlic         18,720    18,860    19,085    18,860     0,365    1,9 %  stabilna
rwpath.ab.1.kandidatov               540.803   538.395   539.058   539.058     2.408    0,4 %  stabilna
rwpath.ab.1.kandidatovNaTick         250,600   247,200   249,800   249,800     3,400    1,4 %  stabilna
rwpath.ab.1.sledenje.msNaTick          4,730     4,699     4,812     4,730     0,113    2,4 %  stabilna
rwpath.ab.1.sledenjNaTick            261,990   257,630   260,680   260,680     4,360    1,7 %  stabilna
rwpath.ab.1.tickov                     2.158     2.178     2.158     2.158        20    0,9 %  stabilna
rwpath.ab.3.kandidat.msNaTick          2,018     2,059     2,128     2,059     0,110    5,3 %  stabilna
rwpath.ab.3.kandidat.usNaKlic          8,067     8,335     8,516     8,335     0,449    5,4 %  stabilna
rwpath.ab.3.kandidatov               530.265   522.958   529.371   529.371     7.307    1,4 %  stabilna
rwpath.ab.3.kandidatovNaTick         250,120   247,030   249,820   249,820     3,090    1,2 %  stabilna
rwpath.ab.3.sledenje.msNaTick          2,058     2,097     2,170     2,097     0,112    5,3 %  stabilna
rwpath.ab.3.sledenjNaTick            261,620   257,470   260,690   260,690     4,150    1,6 %  stabilna
rwpath.ab.3.tickov                     2.120     2.117     2.119     2.119         3    0,1 %  stabilna
rwpath.ab.kandidat.usRazmerje          0,412     0,368     0,406     0,406     0,044   10,8 %  stabilna
rwpath.ab.kandidatovNaTick.razpon      0,400     0,200     0,100     0,200     0,300  150,0 %  SUMNA
rwpath.ab.sledenje.msRazlika          -2,861    -3,547    -3,108    -3,108     0,686   22,1 %  SUMNA
rwpath.branj                      691.377.456688.763.160689.703.912689.703.912 2.614.296    0,4 %  stabilna
rwpath.hist.0                         73.592    66.891    69.728    69.728     6.701    9,6 %  stabilna
rwpath.hist.1                      1.605.209 1.585.756 1.602.225 1.602.225    19.453    1,2 %  stabilna
rwpath.hist.2                              0         0         0         0         0      n/a  enaka
rwpath.hist.3                              0         0         0         0         0      n/a  enaka
rwpath.hist.4                              0         0         0         0         0      n/a  enaka
rwpath.hist.5                              0         0         0         0         0      n/a  enaka
rwpath.iskanjChunka               30.540.56530.205.02430.545.41830.540.565   340.394    1,1 %  stabilna
rwpath.kandidat.delez                 50,200    51,600    51,600    51,600     1,400    2,7 %  stabilna
rwpath.kandidat.msNaTick               3,872     4,115     4,051     4,051     0,243    6,0 %  stabilna
rwpath.kandidat.usNaKlic              15,480    16,640    16,220    16,220     1,160    7,2 %  stabilna
rwpath.kandidatov                  1.605.209 1.585.756 1.602.225 1.602.225    19.453    1,2 %  stabilna
rwpath.kandidatovNaSledenje            0,956     0,960     0,958     0,958     0,004    0,4 %  stabilna
rwpath.kandidatovNaTick              250,100   247,200   249,800   249,800     2,900    1,2 %  stabilna
rwpath.klicev                      1.071.068 1.061.353 1.068.429 1.068.429     9.715    0,9 %  stabilna
rwpath.neujemanj                           0         0         0         0         0      n/a  enaka
rwpath.primerjav                           0         0         0         0         0      n/a  enaka
rwpath.prostih                     1.605.209 1.585.756 1.602.225 1.602.225    19.453    1,2 %  stabilna
rwpath.sledenj                     1.678.801 1.652.647 1.671.953 1.671.953    26.154    1,6 %  stabilna
rwpath.sledenje.delez                 50,700    52,100    52,100    52,100     1,400    2,7 %  stabilna
rwpath.sledenje.msNaTick               3,910     4,152     4,094     4,094     0,242    5,9 %  stabilna
rwpath.sledenje.usNaKlic              14,950    16,120    15,710    15,710     1,170    7,4 %  stabilna
rwpath.sledenjNaTick                 261,600   257,700   260,600   260,600     3,900    1,5 %  stabilna
rwpath.tickov                          6.418     6.414     6.415     6.415         4    0,1 %  stabilna
rwtarget.predzavrnjenih                  474     2.018     4.089     2.018     3.615  179,1 %  SUMNA
rwtarget.zozenih                           0         0         0         0         0      n/a  enaka
ticki                                  6.357     6.333     6.354     6.354        24    0,4 %  stabilna
ticki.nad100ms                             0         0         0         0         0      n/a  enaka
ticki.nad10ms                            102       536       230       230       434  188,7 %  SUMNA
ticki.nad25ms                              7         7         7         7         0    0,0 %  enaka
ticki.nad50ms                              0         0         0         0         0      n/a  enaka
tps                                       20        20        20        20         0    0,0 %  enaka
```

### Sumne velicine

- `mspt.brezSave.p99`: mediana 10,748, razpon 2,359 (21,9 %)
- `mspt.p95`: mediana 9,961, razpon 2,097 (21,1 %)
- `rwpath.ab.kandidatovNaTick.razpon`: mediana 0,200, razpon 0,300 (150,0 %)
- `rwpath.ab.sledenje.msRazlika`: mediana -3,108, razpon 0,686 (22,1 %)
- `rwtarget.predzavrnjenih`: mediana 2.018, razpon 3.615 (179,1 %)
- `ticki.nad10ms`: mediana 230, razpon 434 (188,7 %)
