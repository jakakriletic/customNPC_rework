# 08 — Pregled projekta in plan (2026-10-03)

Ocena stanja reworka in plan preostalega dela. **M8 (migracija funkcij iz CustomNPC+, R3) je
iz plana izločen** (D-023); vse ostale faze ostanejo. Podrobnosti posameznih paketov so v
[`03-FAZE.md`](03-FAZE.md), dnevnik v [`04-STANJE.md`](04-STANJE.md).

> **Popravek 5. 10. (seja 64).** Pregled 3. 10. ni poznal veje `origin/codex/m7-next`
> (28. 9.): A/B serije za Baritona so bile že opravljene in **vrata V4 sprejeta** (D-022,
> `crowdYield`, izbirno ozadje, privzeto izklopljeno) — naloga 0.4 je bila torej že narejena.
> Veja je zdaj prenesena v `codex/m7-cnpc-integration`. Diagnoza padca baselina (točka 4) je
> bila napačna: čiščenje med celicami je delovalo, server se je sesul z `NoClassDefFoundError`,
> v isti mapi pa je tekel drug baseline zagon. Popravek je paket **M2.6b** (zaščita zagona).
> Seja zdaj teče na uporabnikovem računalniku in **lahko sama poganja build in scenarije v
> svetu** — ozko grlo iz točke 2 je s tem večinoma odpravljeno.

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
   ena majhna (R9); animacije (R4) so v veliki meri narejene v ločenem modu
   `customNPC_entities_mod`. Letenje (R2), solid hitbox (R6), Java scripting (R7),
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
| **Baritone ozadje (D-022)** | ✅ V4 sprejet 28. 9. | most, stikalo `RwNavBackend`, reinstall po `updateTasks`, A/B scenarij; M7.8–M7.10c (veja `m7-next`, prenesena 5. 10.): z `crowdYield` 8/8 na obeh grlih in odprtem v 5 zagonih (vanilla 1/8, 6/8); izbirno, privzeto izklopljeno |
| M5 Performance | ⬜ ni začeto | blokirano z M2.6 |
| M6 Java scripting | ⬜ ni začeto | analiza narejena |
| M7 Animacije | 🟢 jedro narejeno **v ločenem modu** `customNPC_entities_mod` (v0.7.0) | ostane integracija s CNPC skriptami in verzijo; glej Fazo 5 |
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
| 0.1 | PR `codex/m7-cnpc-integration` → `main` | S | 🟡 5. 10.: `codex/m7-next` prenesena (cherry-pick), build/testi/`verify-package` zeleni, veja pushana; PR odpre uporabnik (na računalniku ni `gh`) |
| 0.2 | ~~Popraviti čiščenje med celicami~~ → **M2.6b zaščita zagona** (kopija runtime jarja na zagon, zaklep, P9, zaznava sesutja) in ⏵ pognati M2.6 (~3,5 h) | S | 🟡 5. 10.: M2.6b narejen in preverjen v svetu; zagon baselina čaka |
| 0.3 | ✅ ponovni zagon M3.6 (`m36-run.ps1`, načina 0 in 1) → M3.6 zaključen 5. 10. | S | P2 potrjen, odločitev o popravku pri uporabniku |
| 0.4 | ~~Baritone A/B, odločitev D-022~~ | S | ✅ 28. 9. (`m7-next`): V4 sprejet, Baritone ostane izbirno ozadje. Odprto za uporabnika: ali M4.11/M4.12 (izboljšava privzetega vanilla ozadja) še rabimo |
| 0.5 | ⏵ formacije M4.14b (F1–F12, FA1–FA6) | S | koda čaka od 18. 9. |
| 0.6 | Preimenovati Baritone pakete v CNPC (M7.x → NB.x), posodobiti README in "Trenutno stanje" | S | ✅ 5. 10.: preslikava NB.x = M7.x v `07-BARITONE-OZADJE.md` (datoteke in zgodovina ostanejo), README posodobljen |

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

### Faza 5 — M7 animacije (R4) — **M** (ne XL): jedro že obstaja v `customNPC_entities_mod`

Popravek 3. 10. (D-024): animacijsko jedro je narejeno v ločenem modu
`C:\Users\jakak\Desktop\customNPC_entities_mod` (v0.7.0, 5 mobov: ice golem, troll, warg,
giant spider, cave bear). Brez GeckoLib: Bedrock `.geo.json` modeli, klipi v YAML →
`animation.json`, Python preview z isto matematiko kot igra, JUnit test enakosti Java/Python,
dogodki v klipu (udarec, zvok, tresenje, projektil, summon), `mob.yaml` (statistika, tabela
napadov, enrage), `new_entity.py` (nov mob iz predloge), `check.py` (validator),
`/cne list|play`, `/cnedebug`, samotest v klientu. Entitete so podrazredi `EntityCustomNpc`.

| Paket iz `03-FAZE.md` | Stanje v `customNPC_entities_mod` |
|---|---|
| M7.1 port ali novo | ✅ novo, brez GeckoLib |
| M7.2 podatkovni model | ✅ `AnimClip`, `Pose`, `Easing`, `AnimLibrary`, `MobDef` |
| M7.3 runtime + sync | ✅ server izbere akcijo (`DataParameter` ACTION), klient vzorči; dogodki na tick |
| M7.4 render | ✅ za lastne modele (`GeoMesh`, display liste) · ❌ **ne za navadne CNPC NPC-je** (`ModelData`/`ModelPartData`) |
| M7.5 hooki za skripte | ❌ dogodki so v klipu, CNPC skripte jih ne vidijo |
| M7.6 format + vodnik | ✅ YAML + `tools/anim/README.md` (slovensko); JSON Schema ni |
| M7.7 validator | ✅ `tools/anim/check.py` |
| M7.8 reload v igri | 🟡 `/cne play` obstaja, ponovnega nalaganja brez restarta ni |
| M7.9 script API (`play/queue/stop`) | ❌ |
| M7.10 blending | ✅ idle↔walk, crossfade akcije, hurt flinch |
| M7.11 urejevalnik | ❌ (opcijsko; nadomešča ga Python preview) |

**Kaj ostane (skupaj M):**

1. **Združljivost verzij:** entities mod je preveden proti `CustomNPCs (05Jul20)`, rework pa
   gradi na `(01Oct19)`. Odločiti, na kateri osnovi tečeta skupaj, in preveriti, da podrazredi
   `EntityCustomNpc` delujejo s popravljenim `EntityNPCInterface` (M3.x stikala). **S–M, najprej.**
2. Odločitev: entities mod ostane **ločen mod** (priporočeno — že deluje, rework ostane
   bitno enak originalu brez njega) ali se vključi v `rework/anim`.
3. Script API in hooki (M7.5, M7.9) — po M6.3, ali takoj prek obstoječega Nashorn API-ja
   (npr. `/cne play` kot metoda na wrapperju).
4. *Opcijsko:* animacije za navadne humanoidne CNPC NPC-je (M7.4 za `ModelData`) — samo, če
   jih rabiš; sicer so animacije omejene na lastne mobe.
5. Reload klipov brez restarta (M7.8), JSON Schema (M7.6).

Q12 (uvoz zunanjih modelov) je s tem delno odgovorjen: Bedrock `.geo.json` in OBJ (ice golem)
že delujeta.

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
  ├─ Faza 5  M7 script API za animacije (jedro že obstaja; uskladitev verzij CNPC lahko takoj)
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
| Q12 | Uvoz zunanjih 3D modelov — delno rešeno v `customNPC_entities_mod` (`.geo.json`, OBJ); ali rabiš še animacije za navadne CNPC NPC-je? | M7.4 |
| — | Entities mod: ločen mod ali del reworka? Na kateri CNPC verziji (01Oct19 / 05Jul20)? | M7 |
| — | Popravimo P2 (utripajoč napad izven `aggroRange`)? | M3.6/M3.7 |
| Q1 | Modpack / pravi svet | M10.3 (trajna blokada) |

---

## Dopolnitev 5. 10.: raziskava performance

[`09-PERFORMANCE-RAZISKAVA.md`](09-PERFORMANCE-RAZISKAVA.md) (zapiski v `raziskave/performance/`):
plan M5 je skoraj ves strežniški; predlog razdelitve v **M5-S** (strežnik) in **M5-K** (klient,
vhodni pogoj M2.4r = klientski harness). Glavni kandidat S1: `NPCAttackSelector` naredi raytrace
vidnosti pred preverbo frakcije (preverjeno v kodi 5. 10.) — model napove izmerjene alokacije pri
idle-50/200/500. Popravki M5: M5.1 brez MSPT dobitka (lock ni tekmovan), M5.3 pri privzetih
nastavitvah ne zadene, „startY“ iz PLAN §5 črtati. Paketi še niso vpisani v `03-FAZE.md` — čaka
na potrditev uporabnika.
