# P7a — točen dnevnik animacij in walk/idle/death hooki (strežnik)

2026-11-01 · seja 203 · poskus 1 · **sprejet** na `84bbd8da…`. Izhodišče `2caedcb`.
Paket po D-098 in Q277–Q280: P7 razdeljen na P7a (strežnik) in P7b (`blendTo`, `/npcanim reload`, klient).

## Kaj je narejeno

- `rework/core/AnimatedNpcPatch` (coremod, samo pri `RwAnimHooks>=1` v `config/CustomNpcs.cfg`):
  v naloženi `EntityAnimatedNpc` 0.7.0 vstavi pet statičnih klicev `AnimJournal` (glava in vsak
  return `startAction`, glava `stopAction`, glava `onServerEvent`, takoj za naravnim `stopAction`
  pred `onActionEnd`). Vsi vzorci preverjeni pred spremembo; neujemanje vrne iste bajte, hooki
  izklopljeni z napako. Pripeti jar (SHA-256 `0DCDEC55…`) in entities repo nedotaknjena.
- `ActionJournal` (čist model): vsi prehodi tika po vrsti, dogodki koraka pred stopom, naravni
  konec ločen od stopa pri length−1, meja 256 korakov, koraki iz hookov gredo v naslednji drain.
- `Locomotion` (čist model, `RwAnimHooks=2`): death > action > walk > idle, ure entities moda
  (walk `limbSwing/4/stride*length` in prag 0,25, idle `ticksExisted/20`, death `deathTime/20`).
- `AnimationEvent.source` (`action`/`idle`/`walk`/`death`); stari konstruktorji ostanejo.
- `AnimationHooks` bere dnevnik namesto END pollinga; pri 0 ni listenerja, dnevnika ali posega.

## Sprejemni testi (poskus 1)

| ID | Vsebina | Rezultat |
|---|---|---|
| unit | `ActionJournalTest` (izguba koraka / lažni finished na starem modelu), `LocomotionTest`, `AnimatedNpcPatchTest` (pravi jar, ASM analiza, neujemanje → isti bajti, stikalo 0) | PASS (583 `@Test`) |
| build, package | jar brez kopij entities razredov | PASS |
| p7-h0-v1 | razred nedotaknjen, brez hookov | PASS (`animpatch=off`, prazne sledi) |
| p7-h1-v1 | JS=Java: naravni bite, 4 prehodi v ticku, stop v ticku hita (10), stop pri 18; brez plasti | PASS 23/23, JS = Java |
| p7-h2-v1 | kot h1 + walk→idle in smrt (dogodka pri death starosti 15) | PASS 23/23, JS = Java |
| p7-api-v1 | regeneriran API (polje `source`, nov opis hookov) | PASS |

Popravki brez novega poskusa: catch tip v `AnimationHooks` (napaka prevajanja) in izključitev treh
novih testnih razredov iz `testOriginal` v `dev/build.gradle` (nalagali so P7a razrede na originalnem
binarju; `NoClassDefFoundError`, 12/57). p7-h0-v1 je padel še pred zagonom strežnika (`config copy has
no Java classpath`): dev/run konfiguracija od P5 vsebuje prazna privzeta `RwJavaClasspath=` in
`RwAnimHooks=0`; harness ju zdaj odstrani in nastavi eno vrednost (isti vzorec kot `java-hooks-run.ps1`
v P5); neprazna vrednost še vedno pade.
p7-h2-v1 (fingerprint 5806f133…) je padel samo na `walk-layer`/`death-layer`: oba medveda sta po 120
tickih še hodila (walk koraki do starosti 106, konec hoje šele ob smrti), smrtni del je bil pravilen.
Napaka fiksture, ne hookov (Q281): fikstura zdaj izklopi vračanje domov, hodi 6 blokov in čaka na
walk→idle največ 300 tickov; merilo (WALK vzorec) je nespremenjeno.

## Rezultati (`84bbd8da…`)

Pripete sledi v `audit/p7-h1-2026-11-01-144854-proof.json` in `audit/p7-h2-2026-11-01-144947-proof.json`,
JS = Java na obeh medvedih:

- bite: `S:0 · E sound 1 · lock 5 · lunge 5 · hit 10 · sound 10 · X:19:true`.
- 4 prehodi v enem ticku: `S bite · X:0:false · S hurt · X:0:false · S bite …` (vsi javljeni).
- stop v ticku hita: dogodki do 10, nato `X:bite:10:false` (stari END polling jih je izgubil);
  stop pri length−1: `X:bite:18:false` (ne lažni `finished`).
- h2: `S:walk · step 3/4/27/28/51/52 · X:walk:73 · S:idle`, nato `S:death · sound 15 · dust 15`;
  plasti brez akcijskih korakov, kontrolni NPC nespremenjen.
- h0: `animpatch=off`, razred nedotaknjen, noben hook.
- Cena opazovanja v mirovanju (ena serija, 2 NPC-ja, podatek): 0,35 µs (h1) in 2,92 µs (h2) na world tick.

## Omejitve

Brez klienta (P7b). Strežniški lokomocijski hooki so samo skriptni klici (brez zvoka/delcev). Konec smrti se javi šele
ob respawnu; odstranitev NPC-ja ne kliče hooka. `blendTo` in reload sta P7b.
