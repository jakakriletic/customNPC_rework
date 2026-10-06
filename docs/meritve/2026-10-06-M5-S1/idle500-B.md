# M2.5c - ponovitve scenarija perf (M2.4), 2026-10-06 09:37

Protokol: `docs/scenariji/M2.5c-ponovitve.md`. Vsaka ponovitev je svoj proces in
svez svet (reset z .\testworld.ps1).

Merila: T1-T6 zelena

## Zagoni

| # | izhodna koda | minut | zapis |
|---|---|---|---|
| 1 | 0 | 8,1 | `m24-perf-2026-10-06-0913-p1.json` |
| 2 | 0 | 8,0 | `m24-perf-2026-10-06-0913-p2.json` |
| 3 | 0 | 8,0 | `m24-perf-2026-10-06-0913-p3.json` |

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
varianta             idle
```

## Sumni pas

Prag: relativni razpon <= 20 %. Velicina, oznacena kot SUMNA, se med ponovitvami
premakne bolj kot prag, zato A/B primerjava na njej potrebuje razliko, vecjo od stolpca
`razpon` - manjsa razlika je sum, ne izboljsava.

```
velicina                                  p1        p2        p3   mediana    razpon      rel  oznaka
-----------------------------------------------------------------------------------------------------
alok.KBnaTick                          1.076     1.074     1.074     1.074     1,900    0,2 %  stabilna
alok.MBnaS                                21        21        21        21         0    0,0 %  enaka
gc.ms                                     29        29        29        29         0    0,0 %  enaka
gc.msNaS                               0,100     0,100     0,100     0,100         0    0,0 %  enaka
gc.old.ms                                  0         0         0         0         0      n/a  enaka
gc.old.zbirk                               0         0         0         0         0      n/a  enaka
gc.zbirk                                  12        12        12        12         0    0,0 %  enaka
heap.maxMB                             1.820     1.820     1.820     1.820         0    0,0 %  enaka
mspt.brezSave.max                     18,778    13,295    12,823    13,295     5,955   44,8 %  SUMNA
mspt.brezSave.p99                     10,748    10,748    10,748    10,748         0    0,0 %  enaka
mspt.max                              34,193    33,656    34,014    34,014     0,537    1,6 %  stabilna
mspt.p50                               3,408     3,342     3,473     3,408     0,131    3,8 %  stabilna
mspt.p95                               9,961     9,961     9,961     9,961         0    0,0 %  enaka
mspt.p99                              10,748    10,748    11,010    10,748     0,262    2,4 %  stabilna
mspt.povp                              4,708     4,695     4,775     4,708     0,080    1,7 %  stabilna
nasiceno                                   0         0         0         0         0      n/a  enaka
npc.per.tick.p50                         500       500       500       500         0    0,0 %  enaka
npc.us                                 9,310     9,270     9,440     9,310     0,170    1,8 %  stabilna
rwtarget.predzavrnjenih           108.812.462108.306.511108.777.942108.777.942   505.951    0,5 %  stabilna
ticki                                  6.033     6.016     6.035     6.033        19    0,3 %  stabilna
ticki.nad100ms                             0         0         0         0         0      n/a  enaka
ticki.nad10ms                            237       220       285       237        65   27,4 %  SUMNA
ticki.nad25ms                              6         3         6         6         3   50,0 %  SUMNA
ticki.nad50ms                              0         0         0         0         0      n/a  enaka
tps                                       20        20        20        20         0    0,0 %  enaka
```

### Sumne velicine

- `mspt.brezSave.max`: mediana 13,295, razpon 5,955 (44,8 %)
- `ticki.nad10ms`: mediana 237, razpon 65 (27,4 %)
- `ticki.nad25ms`: mediana 6, razpon 3 (50,0 %)
