# 08 — Pregled projekta in plan (2026-10-03)

Ocena stanja reworka in plan preostalega dela. **M8 (migracija funkcij iz CustomNPC+, R3) je
iz plana izločen** (D-023); vse ostale faze ostanejo. Podrobnosti posameznih paketov so v
[`03-FAZE.md`](03-FAZE.md), dnevnik v [`04-STANJE.md`](04-STANJE.md).

---

## 1. Ocena

### Kaj je dobro

- **Temelj je trden.** Ponovljiv build iz originalnega JAR-a, `verify-package.ps1` dokaže točen
  seznam spremenjenih razredov, testni svet iz semena, dedicated-server smoke, integracijska matrika.
- **Vsaka sprememba je pod stikalom, privzeto original** (D-007): `RwMountSteering`,
  `RwAttackPriority`, `RwNavBackend`, formacije samo prek ukaza. Obstoječi svetovi se ne spremenijo.
- **Meritve pred popravki.** R1 in R2 imata ponovljivi reprodukciji, navigacija ima šest veličin
  in izmerjen šumni pas (46 od 59 veličin determinističnih).
- **Podatki so varni** (M1): tipno varen NBT↔JSON, atomski zapisi, fault injection testi.
- **Dejanski popravki že obstajajo:** R1 (jahanje NPC na NPC) je popravljen in regresija zelena,
  hitbox po sestopu, prioriteta napada pred tavanjem.

### Kaj je narobe ali tvegano

1. **Hitrost glede na obseg.** 62 sej, smo v M3. Od devetih zahtev je popravljena ena (R1) in
   ena majhna (R9). Letenje (R2), solid hitbox (R6), Java scripting (R7), animacije (R4),
   performance (R5) in chatbot (R8) **še niso začeti v kodi**. Proces (scenariji, merila, zapisi)
   je kakovosten, a pri trenutnem tempu je pred nami še veliko več kot za nami.
2. **Ozko grlo so zagoni v svetu.** Seja ne more poganjati Minecrafta; vsak paket čaka na
   uporabnikov zagon. Trenutno čaka: ponovni zagon M3.6, baseline M2.6, formacije M4.14b,
   serija A/B za Baritona.
3. **Delo izven vrstnega reda.** Formacije (M4.14a) in Baritone ozadje (D-022) sta začeta,
   preden je M3 zaključen. Oboje je smiselno, a širi odprte fronte.
4. **Baseline M2.6 ne obstaja** — zagon 27. 9. je padel pri prehodu idle-500 → boj-50: v svetu
   je ostalo 500 NPC-jev iz prejšnje celice (`P2: npc=500 (pricakovano 50)`). To je napaka
   čiščenja med celicami v `perf-run.ps1`/`baseline-run.ps1`, ne v modu. **Brez baselina se M5
   ne sme začeti** (vhodni pogoj).
5. **Veja `codex/m7-cnpc-integration` ni združena v `main`** (4 commiti: most za Baritona,
   `RwNavBackend`, A/B scenarij, popravek `runServer`).
6. **Kolizija imen:** commiti in dnevnik uporabljajo "M7.x" za milestone **knjižnice**
   `npcbaritone`, v `03-FAZE.md` pa je M7 animacijski sistem. Predlog: Baritone delo v CNPC
   poimenovati **NB** (npr. NB.6 = A/B scenarij), da se ne pomeša z animacijami.
7. **Prekrivanje Baritona in M4.10–M4.12.** Če Baritone ozadje prestane vrata D-022, je velik
   del kopenske navigacije (nadaljevanje delne poti, lasten A\*) rešen drugače. Odločitev je
   treba sprejeti **pred** M4.10, sicer se dela dvakrat.
8. README in "Trenutno stanje" v `04-STANJE.md` sta zastarela (24. 9.).

---

## 2. Kaj je narejeno

| Milestone | Stanje | Ključno |
|---|---|---|
| **M0 Temelj** | ✅ zaključen | okolje, build, testi, git, smoke, testni svet, integracijska matrika; M0.8 = blokada (ni modpacka) |
| **M1 Integriteta podatkov** | ✅ zaključen | serializer R9, `SafeFileWriter` (B1), async lifecycle (B2), fault injection; M1.4/1.7/1.8 zavestno odloženi |
| **M2 Diagnostika** | 🟡 ~95 % | `/rwdiag`, reprodukcije R1 in R2, obremenitve 50/200/500, protokol ponovitev, merila navigacije — vse zeleno v svetu. **Manjka:** baseline M2.6 (zagon padel), M2.4r (render), M2.1b (klicna mesta — zdaj izvedljivo, ker je M3.1 narejen) |
| **M3 Jedro entitete** | 🟡 5/10 | M3.1–M3.5 zaključeni (prenos `EntityNPCInterface` + `ai/**`, diagnoza in popravek R1, gating AI med jahanjem, hitbox po sestopu). M3.6 v kodi, čaka ponovni zagon |
| **M4 Gibanje** | 🟡 začet izven vrstnega reda | M4.14a formacije v kodi (21 testov), v svetu ne pognane. Letenje (R2) ni začeto; diagnoza R2 = zastarela delna pot |
| **Baritone ozadje (D-022)** | 🟡 integracija v kodi | most, stikalo `RwNavBackend`, reinstall po `updateTasks`, A/B scenarij; prvi A/B zagon: Baritone 8/8 v 40 tickih, vanilla 1/8 in 6/8. Manjkata dve seriji po tri ponovitve in odločitev |
| M5 Performance | ⬜ ni začeto | blokirano z M2.6 |
| M6 Java scripting | ⬜ ni začeto | analiza narejena |
| M7 Animacije | ⬜ ni začeto | analiza narejena |
| ~~M8 CustomNPC+~~ | ❌ **izločeno** (D-023) | |
| M9 Chatbot | ⬜ ni začeto | analiza narejena |
| M10 Release | ⬜ ni začeto | |

Koda: 102 razreda v `dev/src/patch/java`, 25 testnih datotek (192 JUnit testov zelenih ob
zadnjem prevodu).

---

## 3. Kaj se še rabi narediti

Velikosti: **S** = ena seja · **M** = 2–4 seje · **L** = 5–10 · **XL** = več kot 10.
⏵ = potreben uporabnikov zagon v svetu.

### Faza 0 — pospraviti odprte fronte (najprej, ~1 teden)

| # | Naloga | Vel. | Zakaj zdaj |
|---|---|---|---|
| 0.1 | PR `codex/m7-cnpc-integration` → `main` | S | 4 commiti visijo izven `main` |
| 0.2 | Popraviti čiščenje med celicami v `perf-run.ps1` (pred novo celico preveriti `npc=0` ali svet resetirati) in ⏵ pognati M2.6 (~3,5 h) | S | brez baselina ni M5 |
| 0.3 | ⏵ ponovni zagon M3.6 (`m36-run.ps1`, načina 0 in 1) → zaključiti M3.6 | S | koda čaka od 24. 9. |
| 0.4 | ⏵ Baritone A/B: rebuild knjižnice (popravek hitrosti D-042), dve seriji po 3 ponovitve → **odločitev D-022: ostane ali gre ven** | S | odloči obseg M4.10–M4.12 |
| 0.5 | ⏵ formacije M4.14b (F1–F12, FA1–FA6) | S | koda čaka od 18. 9. |
| 0.6 | Preimenovati Baritone pakete v CNPC (M7.x → NB.x), posodobiti README in "Trenutno stanje" | S | kolizija z M7 animacijami |

### Faza 1 — zaključiti M3 (entiteta)

| ID | Naloga | Vel. |
|---|---|---|
| M3.7 | Ločitev `minRange` od `npc.width`; spodnja meja dosega napada | S |
| M3.8 | **R6 solid hitbox**: `RwHitboxMode` NONE/NORMAL/SOLID + GUI + NBT + testi | M |
| M3.9 | Solid × mount; igralec ujet med dvema NPC-jema; scenarij `tpTo` jahača | S |
| M3.10 | *opcijsko:* jahač kot "commander" nosilca (konjenica) | M |
| — | odprto iz M3.6: hipoteza P2 (`aggroRange` utripanje) — popraviti ali ne, odloči uporabnik | S |

### Faza 2 — M4 gibanje

Kopenski del je odvisen od odločitve 0.4:

| ID | Naloga | Vel. | Če Baritone ostane |
|---|---|---|---|
| M4.10 | Nadaljevanje delne poti (neposreden popravek ugotovitve R2) | S | še vedno smiseln za vanilla ozadje in letenje |
| M4.11 | Lasten `NodeProcessor` (cena diagonale) | M | verjetno odpade |
| M4.12 | Lasten `PathNavigate` | L | odpade |
| M4.13 | Mehkejše sledenje poti | S | samo za vanilla |
| M4.14c | Cena formacij pri 50/200 članih | S | |
| NB | U1, U3–U6 iz [`07-BARITONE-OZADJE.md`](07-BARITONE-OZADJE.md) (vrata, voda, hitrost na instanco) | M | |

Letenje (**R2**) — potreben odgovor na **Q6** (letalo = vozilo za igralca ali NPC, ki leti sam):

| ID | Naloga | Vel. |
|---|---|---|
| M4.1 | Branje flying implementacije CustomNPC+ (kot referenca, ne migracija); port ali lasten | S |
| M4.2 | `FlightMode` + NBT + GUI | S |
| M4.3 | `HoverMoveHelper` (creative-style) | M |
| M4.4 | Popravek `isNotColliding` | S |
| M4.5 | 3D pathfinding | L |
| M4.6 | `MomentumMoveHelper` (dragon-style) | M |
| M4.7 | Pitch/roll sinhronizacija + render | M |
| M4.8 | Vrata/zavetje/voda za leteče | S |
| M4.9 | Meritve 50/200 letečih | S |

### Faza 3 — M5 performance (po M2.6)

M5.1 globalni script lock (skupaj z M6.6) · M5.2 AI budget · M5.3 deduplikacija poizvedb ·
M5.4 alokacije · M5.5 meritve · **M5.6 deljenje poti** (največji pričakovani dobitek) ·
M5.7 po potrebi. Skupaj **L**. Prej: M2.1b klicna mesta (S).

### Faza 4 — M6 Java scripting (R7) — **XL**

M6.1 Janino vs `JavaCompiler` · M6.2 hook registry · M6.3 tipiziran API · M6.4 ClassLoader +
hot reload · M6.5 diagnostika napak (B5) · M6.7 `compileScripts` · M6.8 varnostna omejitev ·
M6.9 generator API dokumentacije · M6.10 projektna predloga · M6.11 razširitev API ·
M6.12 združljivost JS. CustomNPC+ ostane **referenca** za Janino pristop.

Ker je M8 izločen, M8.4 (razširjeni hooki do ~154) ni več v planu; M6.11 pokrije, kar
potrebujejo M7 in M9.

### Faza 5 — M7 animacije (R4) — **XL**, po M6

M7.1–M7.10 iz `03-FAZE.md` (model, runtime + sync, render, hooki, tekstovni format + shema +
`ANIMATION-GUIDE.md`, validator, `/npcanim reload`, script API, blending); M7.11 urejevalnik
opcijsko. Q12 (uvoz zunanjih modelov) se odloči tu.

### Faza 6 — M9 chatbot (R8) — **M**, po M6

M9.1–M9.10. Potrebna odgovora **Q8** (provider) in **Q10** (internet na strežniku).

### Faza 7 — M10 release kandidat — **M**

Funkcionalna matrika, meritve proti M2.6, soak 24 h, rollback vaja, dokumentacija, changelog.
M10.3 (kopija pravega sveta) ostaja blokiran z Q1.

---

## 4. Priporočen vrstni red

```
Faza 0 (pospravljanje, odločitev Baritone)   ← zdaj
  └─ Faza 1  M3.7–M3.9 (R6 solid hitbox)
       └─ Faza 2  M4 kopenski (po odločitvi) + letenje R2
Faza 3  M5 performance        (lahko vzporedno s Fazo 2, ko obstaja M2.6)
Faza 4  M6 Java scripting     (veja A, neodvisna od M3/M4 — lahko začne takoj vzporedno)
  ├─ Faza 5  M7 animacije
  └─ Faza 6  M9 chatbot
Faza 7  M10 release
```

**Predlog za pospešitev:** M6 (scripting) je neodvisen od entitete in gibanja (stik samo pri
M5.1/M6.6). Ker veja B (M3/M4) čaka na zagone v svetu, lahko seje med čakanjem delajo vejo A
— M6.1 prototip prevajalnika, M6.7 in M6.9 tečejo brez Minecrafta in jih seja lahko preveri sama.

### Odprta vprašanja, ki blokirajo plan

| # | Vprašanje | Blokira |
|---|---|---|
| Q6 | Letala: vozilo za igralca ali NPC, ki leti sam? | obseg M4 |
| Q8 | Chatbot provider (Anthropic / OpenAI / lokalni)? | M9.3 |
| Q10 | Ima strežnik izhodni internet? | M9 |
| Q12 | Uvoz zunanjih 3D modelov — v obsegu? | M7 |
| — | Popravimo P2 (utripajoč napad izven `aggroRange`)? | M3.6/M3.7 |
| Q1 | Modpack / pravi svet | M10.3 (trajna blokada) |
