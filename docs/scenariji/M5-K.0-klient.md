# Scenarij M5-K.0 — klientski merilni harness (M2.4r)

**To je merilni paket, ne popravek.** Dostavi meritev časa sličice razvojnega klienta z igralcem,
na kateri M5-K.1 izmeri baseline originala in pod katero se meri vsak K-paket (D-033).

Predhodniki: [`M2.4-obremenitve.md`](M2.4-obremenitve.md) (celice in prizorišče, ki jih harness
prevzame), [`M2.5c-ponovitve.md`](M2.5c-ponovitve.md) (ponovitve, šumni pas).

---

## Sestavni deli

| del | vloga |
|---|---|
| `k-run.ps1` | ena celica na zagon: zapiše navodilo, zažene `runClient -PrwHarness=<navodilo>`, preveri merila K0–K8 |
| `rework/diag/HarnessJob` | bralnik navodila (properties), brez Minecraft tipov, JUnit `HarnessJobTest` |
| `rework/diag/ClientHarness` | stroj stanj v klientu (`@SideOnly(CLIENT)`), namesti ga `CustomNpcs` prek refleksije **samo** z `-Drwharness` |
| `rework/diag/FrameStats` | p50/p95/p99/max, FPS, 1 % in 0,1 % low iz časov sličic, JUnit `FrameStatsTest` |
| `ponovitve-run.ps1 -Scenarij klient` | tri ponovitve in šumni pas (D-016) |

Brez lastnosti `rwharness` se v igri ne spremeni nič; `options.txt` se ne shrani.

## Potek v klientu

glavni meni → integrirani svet `rwk-<varianta>-<N>` (vsak zagon **svež** FLAT svet s semenom
`customnpc-rework`, fixture `PERF_*` iz `dev/testworld/customnpcs`) → spectator + teleport →
5 s nalaganja chunkov → ukazi prizorišča (isti generator kot `perf-run.ps1`, prebran z AST, ne
prepisan) → umiritev in štetje NPC-jev → ogrevanje (privzeto 30 s) → merjenje (privzeto 60 s,
razlika `System.nanoTime()` med zaporednima `RenderTickEvent` START) → JSON → izhod.

Nastavitve: VSync izklopljen, `limitFramerate` 260 (neomejeno), brez view bobbinga, brez GUI-ja,
RD 8, FOV 70. Kamera: južno od mreže, yaw 180, pitch proti sredini; najbolj oddaljen NPC mora biti
≤ 60 blokov (domet sledilnika `tracker(64, …)`), sicer ga strežnik ne pošlje.

## Merila

| | merilo |
|---|---|
| K0 | pred zagonom ne teče tuj gradle build ali Minecraft (zaklep `.scenarij.lock`) |
| K1 | klient je zapisal JSON v fazi `MEASURE`, brez napak |
| K2 | vsi ukazi prizorišča so uspeli (`fill … air` brez spremembe je dovoljen) |
| K3 | v klientu je natanko pričakovano število NPC-jev (`boj`/`nedosegljiva`: N + kontrola) |
| K4 | upodobljenih entitet v ≥ 80 % sekundnih vzorcev ≥ N (idle, skripte) oz. ≥ 0,9 N (boj) |
| K5 | kamera fiksna: odmik ≤ 0,01 bloka in ≤ 0,01° |
| K6 | VSync 0, limit ≥ 260, RD in FOV kot zahtevano |
| K7 | vsaj 10 sličic/s in trajanje merjenja ±2 s |
| K8 | med ogrevanjem in merjenjem ni tekel tuj java proces |

## Uporaba

```
.\k-run.ps1 -Variant idle -N 50 -Warmup 10 -Measure 20     # hiter preizkus
.\k-run.ps1 -Variant idle -N 200
.\ponovitve-run.ps1 -Scenarij klient -Dodatno @('-Variant','idle','-N','200')
```

## Omejitve (znane)

- Integrirani strežnik teče v istem procesu; njegov MSPT se zapiše (`integriraniStreznikMsPovp`),
  a deli CPU s klientom. Celice z dediciranim strežnikom so M5-V.1.
- Različice videza (oklep, ime, oblački, delci, URL skini) iz opisa paketa še niso v fixturah;
  dodajo se v M5-K.1, ko profil pokaže, katere so pomembne.
- Ločljivost okna je privzeta (854 × 480) in se zapiše v JSON (`sirina`, `visina`).
