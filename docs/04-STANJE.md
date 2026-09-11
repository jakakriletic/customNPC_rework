# 04 — Stanje projekta (živ dnevnik)

**To je edini dokument, ki se spreminja vsako sejo.** Vsaka seja ga na koncu posodobi.
Če seja tega ne naredi, je naslednja seja slepa.

---

## Trenutno stanje

| | |
|---|---|
| Zadnja posodobitev | **2026-09-11** |
| Trenutni milestone | **M1 — Integriteta podatkov** (M0 še ni zaključen) |
| Naslednji paketi | **M1.5b** (preostali save controllerji), **M1.6** (async lifecycle), **M0.5** (server smoke) |
| Prevedljivih razredov | 5 — `DataTimers`, `NBTJsonUtil`, `ServerCloneController`, `NbtJson`, `SafeFileWriter` |
| Testi | 21 primerjalnih v obeh načinih + 4 za novi varni writer; zeleni |
| Blokade | Q1 in Q5–Q10 odprta; specifična R9 forenzika je po navodilu uporabnika odložena, ne blokirana |

---

## Napredek po milestonih

| Milestone | Stanje | Opomba |
|---|---|---|
| M0 Temelj | **v teku** (≈70 %) | M0.1–M0.4 narejeno; M0.5–M0.8 odprto |
| M1 Integriteta podatkov | **v teku** (≈55 %) | M1.1–M1.3 narejeno; M1.5 delno; M1.4/M1.7/M1.8 zavestno odloženi |
| M2 Diagnostika | ni začeto | |
| M3 Jedro entitete | ni začeto | analiza narejena, glej R1 in R6 |
| M4 Gibanje | ni začeto | analiza narejena, glej R2 |
| M5 Performance | ni začeto | del že pokrit z M1.3, glej meritve |
| M6 Scripting | ni začeto | analiza narejena, glej R7 |
| M7 Animacije | ni začeto | analiza narejena, glej R4 |
| M8 CustomNPC+ | ni začeto | katalog še ne obstaja |
| M9 Chatbot | ni začeto | analiza narejena, glej R8 |
| M10 Release | ni začeto | |

### M1 po paketih

| ID | Paket | Stanje |
|---|---|---|
| M1.1 | Karakterizacijski testi, ki dokumentirajo napake | **narejeno** |
| M1.2 | `javap` verifikacija sumljive logike | **narejeno** — dve hipotezi ovrženi |
| M1.3 | Nov tipno varen NBT↔JSON serializer + fuzz testi | **narejeno** |
| M1.4 | Bralnik za že pokvarjene datoteke, popravek tipov kjer je mogoče | **odloženo** — ni vhodnih datotek, R9 je minoren |
| M1.5 | `SafeFileWriter` + preklop vseh controllerjev (B1) | **delno** — helper, NBT JSON in clone controller narejeni; ostali controllerji odprti |
| M1.6 | Lifecycle asinhronih zapisov (B2) | odprto |
| M1.7 | Verzioniranje `SaveFormat` + migracija | **ni več nujno za R9** — format nespremenjen |
| M1.8 | `.\dev.ps1 auditClones` | **odloženo** — brez konkretnih poškodovanih datotek |
| M1.9 | Fault injection testi | odprto |

---

## Dnevnik sej

### 2026-09-11 (4) — M1.5a: varen zapis clone JSON

**Paket:** M1.5 (prvi del)
**Stanje:** delno

**Narejeno:**

- Dodan `rework/data/SafeFileWriter`: začasna datoteka v isti mapi, `flush` + disk `sync`,
  validacija pred zamenjavo, atomski `Files.move` in preverjen fallback z obnovitvenim `.bak`.
- `NBTJsonUtil.SaveFile` zdaj kandidat ponovno prebere in zahteva bitno enakovreden NBT,
  preden ga namesti na ciljno pot.
- `ServerCloneController` ne uporablja več nevarnega zaporedja izbriši-staro → `renameTo`;
  clone JSON zapiše neposredno skozi varni writer.
- Štirje novi testi pokrijejo prvi zapis, zamenjavo, zavrnjeno validacijo z ohranitvijo stare
  datoteke in osirotel recovery backup. 21 primerjalnih testov ostaja zelenih v obeh načinih.
- Build zapakira 14 razredov; `verify-package` je preveril 1716 originalnih vnosov in našel
  samo 6 dovoljenih zamenjav originalnih razredov.
- Po uporabnikovem pojasnilu je R9 označen kot minoren; M1.4 in M1.8 sta odložena, R9 migracija
  ni potrebna. Splošna B1/B2 zaščita ostaja, ker varuje vse podatke sveta.
- Dodan ignore za `*.log.gz`; pomotoma sledljiv Gradle log je odstranjen samo iz indeksa.

**Ni narejeno in zakaj:**

- Preostali JSON in stisnjeni-NBT controllerji še uporabljajo lastne `_new`/`_old` poti.
  Prenos vsakega originalnega razreda zahteva ločen preverjen baseline, zato ostanejo M1.5b.
- Polni fault-injection nabor (zaklenjena datoteka, prekinitev procesa, zavrnjen dostop) je
  še vedno M1.9.

**Ugotovitve:**

- Nova koda v originalnem testnem classpathu ne obstaja; `SafeFileWriterTest` zato teče samo
  pod `test`, medtem ko vseh 21 karakterizacijskih testov še vedno teče tudi pod `testOriginal`.

**Spremembe obnašanja:** clone JSON se namesti šele po trajnem zapisu in uspešni validaciji.

**Meritve:** nobene

**Naslednja seja:** M1.5b, preklop preostalih controllerjev na `SafeFileWriter`.

---

### 2026-09-11 (3) — M0.4: sledljivo git izhodišče

**Paket:** M0.4
**Stanje:** končano

**Narejeno:**

- Inicializiran git repozitorij na veji `main`; preverjena sta korenski in Gradlov
  `.gitignore`, zato lokalni JDK, odvisnosti, build izhodi, razvojni svetovi in logi niso
  vključeni v zgodovino.
- Na uporabnikovem računalniku pognan `.\dev.ps1 testOriginal test --offline`: vseh 21 testov
  je zelenih proti originalu in popravljeni kodi.
- Pognana `.\dev.ps1 buildPatchedMod --offline` in `.\verify-package.ps1`: build uspešen,
  zapakiranih je 8 obnovljenih razredov; preverjenih je 1716 originalnih vnosov, spremenjenih
  je samo 5 dovoljenih originalnih class datotek. Preostali 3 so novi `NbtJson` razredi.
- Ustvarjen prvi commit, ki predstavlja preverjeno izhodišče za nadaljnje pakete.

**Ni narejeno in zakaj:**

- M0.5–M0.8 ostajajo ločeni paketi; protokol dovoljuje en delovni paket na sejo.

**Ugotovitve:**

- Prejšnja omejitev, da lokalnih ukazov ni mogoče zaganjati, ne velja več.

**Spremembe obnašanja:** nobene

**Meritve:** nobene

**Naslednja seja:** M1.5 (`SafeFileWriter` + `ServerCloneController`).

---

### 2026-09-11 (2) — M1.1–M1.3: NBT↔JSON serializer

**Paket:** M1.1, M1.2, M1.3
**Stanje:** končano

**Narejeno:**

- Postavljeno oblačno prevajalno in testno okolje z **pravimi** projektnimi artefakti
  (mapiran CustomNPCs JAR, `forgeSrc-1.12.2-14.23.5.2847.jar`, guava 21.0, commons-io 2.5,
  log4j 2.8.1). S tem je bilo mogoče trditve preveriti z izvedbo, ne samo z branjem.
- **Empirično potrjenih 9 napak** v `NBTJsonUtil` in **ovrženi 2** prvotni hipotezi.
  Vse podrobnosti: [`meritve/2026-09-11-M1-nbtjson.md`](meritve/2026-09-11-M1-nbtjson.md).
- Napisan nadomestni serializer `noppes/npcs/rework/data/NbtJson.java` in shim
  `noppes/npcs/util/NBTJsonUtil.java`, ki ohrani vse javne podpise originala.
- Napisana testa `NbtJsonBaselineTest` (17 testov) in `NbtJsonFuzzTest`. Oba tečeta v dveh
  načinih: pod `testOriginal` trdita originalno napačno obnašanje, pod `test` popravljeno.
  Ločevanje gre prek refleksijske preverbe polja `NBTJsonUtil.REWORK_SERIALIZER`.
- V oblaku zeleno v obeh načinih: 18/18 novih testov.

**Ugotovitve:**

- Diferencialni fuzz čez 4000 naključnih NBT struktur: **original izgubi ali spremeni podatke
  v 1598 primerih (40 %)**, novi v 0.
- **Zapisano besedilo je znak za znak enako originalnemu v vseh 4000 primerih.** Vse napake so
  bile v logiki pretvorbe, ne v formatu. Posledica: za R9 **ni potrebna migracija**, obstoječe
  datoteke se berejo takoj, in nespremenjen CustomNPCs zna brati datoteke, ki jih zapišemo mi.
- Najhujše potrjene napake: long array vrednosti prisekane na 8 bitov; `NBTTagList` bytov,
  intov ali longov se tiho spremeni v array tip (zato `getTagList` vrne prazno in nastavitev
  se "vrne nazaj"); prazen tipiziran array izgubi tip; nizi izgubijo vodilne presledke; niz,
  ki se konča z `\`, `NaN`, `Infinity`, prazen ključ in compound s >3000 ključi naredijo
  **celo datoteko neberljivo**.
- Hitrost na compoundu s 1500 ključi: zapis 235 ms → 2,9 ms, branje 2386 ms → 6,5 ms.
  Ker gredo skozi ta razred vse clone, dialog, quest, faction in player datoteke ob zagonu
  sveta, je to tudi prispevek k **R5**.
- Dve prvotni hipotezi sta bili napačni (byte/long array zapis; `SaveFile` ne zapre writerja).
  `javap` na originalnem bytecode ju je ovrgel. Pravilo iz `05-SEJA-PROTOKOL.md` se je
  izplačalo že prvič.

**Ni narejeno in zakaj:**

- Lokalni build in testi takrat še niso bili pognani; izvedeni so bili v naslednji seji
  M0.4 in so uspešni.
- M1.4–M1.9 ostajajo odprti.

**Spremembe obnašanja:** glej tabelo "Sprejete spremembe obnašanja".

**Meritve:** [`meritve/2026-09-11-M1-nbtjson.md`](meritve/2026-09-11-M1-nbtjson.md)

**Naslednja seja:** M1.5 (`SafeFileWriter` + `ServerCloneController`), nato M1.4.

---

### 2026-09-11 (1) — Postavitev načrta reworka

**Narejeno:**

- Pregledano obstoječe okolje (`OKOLJE.md`, `PLAN_IMPLEMENTACIJE.md`, `dev/build.gradle`,
  `environment-lock.json`, `audit/package-verification.txt`)
- Prebrani ključni razredi v `dev/reference-src/`: `EntityNPCInterface`, `EntityNPCFlying`,
  `FlyingMoveHelper`, `EntityAIFollow`, `EntityAIAttackTarget`, `ServerCloneController`,
  `ScriptController`, `ScriptContainer`, `NBTJsonUtil`, `ItemMounter`, `EntityChairMount`
- Raziskana referenčna projekta: BetaZavr 1.12.2-Unofficial in KAMKEEL CustomNPC+ (1.7.10)
- Sprejete odločitve D-001 do D-007 (`01-ARHITEKTURA.md`)
- Napisana dokumentacija: `README.md`, `docs/01-ARHITEKTURA.md`, `docs/02-ZAHTEVE.md`,
  `docs/03-FAZE.md`, `docs/04-STANJE.md`, `docs/05-SEJA-PROTOKOL.md`

**Ključne ugotovitve iz kode (podrobno v `02-ZAHTEVE.md`):**

- **R7 in R5 sta povezana**: `ScriptContainer.java:51,142` ima **en globalen `static` lock za
  vse skripte vseh NPC-jev**. Najverjetnejše največje ozko grlo pri skriptanih NPC-jih.
- **R1**: `EntityNPCInterface` ne override-a nobene passenger metode; `updateHitbox()` se ob
  spremembi jahanja ne pokliče; `EntityAIFollow.java:70` teleportira jahača z mounta;
  `EntityAIAttackTarget.java:98` veže napadalni doseg na `npc.width`, kar pri majhnem hitboxu
  sili NPC-je v kup.
- **R4 in R7 sta isti problem**: strojno berljiva shema, tipi, validacija, hitra povratna
  zanka. Zato M7 gradi na M6.
- **CustomNPC+ ima R7, R4 in del R2 že implementirane**. Glavni referenčni vir.

**Naslednja seja:** začetek razvoja.

---

## Odprta vprašanja za uporabnika

| # | Vprašanje | Vpliva na | Stanje |
|---|---|---|---|
| Q1 | Kateri modpack in Forge verzijo dejansko uporabljaš? | vse; združljivost je do takrat neznanka | odprto |
| Q2 | Kopija sveta s problematičnimi NPC-ji | M1.4 | zaprto — uporabnik je nima; paket odložen |
| Q3 | Konkretna pokvarjena clone JSON datoteka | M1.4 | zaprto — ne obstaja; paket odložen |
| Q4 | Katera nastavitev se vrne nazaj? | R9 | odgovorjeno — follower role, action `waiting` se po clone lahko vrne v `following`; minorno |
| Q5 | Pri R1 — jahač in nosilec sta oba CustomNPC, ali je eden vanilla mob (konj)? | M2.2 | odprto |
| Q6 | Pri R2 — "letala" pomenijo NPC kot vozilo, ki ga igralec krmili, ali NPC, ki leti sam? | M4 obseg | odprto |
| Q7 | Pri R3 — katerih 5–8 funkcij CustomNPC+ je najbolj pomembnih? | M8.2 | odprto — najprej katalog |
| Q8 | Pri R8 — kateri provider (Anthropic / OpenAI / lokalni model)? | M9.3 | odprto |
| Q9 | Koliko NPC-jev je "veliko" v tvojem primeru? 100? 500? 2000? | M2.4, cilj za M5 | odprto |
| Q10 | Ali strežnik, kjer to teče, sploh ima izhodni internetni dostop? | M9.1 | odprto |

---

## Prevzeta koda

| Datum | Kaj | Izvor | Licenca | Kje v našem drevesu |
|---|---|---|---|---|
| — | — | — | — | — |

Nič prevzetega. `NbtJson` je napisan na novo; format posnema original, koda ne.

---

## Sprejete spremembe obnašanja

| Datum | Sprememba | Zahteva / bug | Stikalo | Privzeto |
|---|---|---|---|---|
| 2026-09-11 | Long array vrednosti se berejo z `getLong()` namesto `getByte()` | R9-a | ne | popravljeno |
| 2026-09-11 | Prazen `[B;]`/`[I;]`/`[L;]` obdrži svoj tip namesto da postane prazen seznam | R9-b2 | ne | popravljeno |
| 2026-09-11 | `NBTTagList` bytov/intov/longov se ne pretvori več v array tip | R9-b | ne | popravljeno |
| 2026-09-11 | Nizi obdržijo vodilne presledke in prelome vrstic | R9-d | ne | popravljeno |
| 2026-09-11 | Niz, ki se konča z `\`, `NaN`, `Infinity` in prazen ključ se preberejo namesto da vržejo | R9-e, e2, e3 | ne | popravljeno |
| 2026-09-11 | Parser ne rekurzira več na ključ; namesto `StackOverflowError` je omejitev globine 512 z jasno napako | R9-e4 | ne | popravljeno |
| 2026-09-11 | Prazen ključ se zapiše kot `"": vrednost` namesto da se izpusti | R9-e3 | ne | popravljeno |
| 2026-09-11 | `SaveFile` po pisanju eksplicitno flusha | — | ne | popravljeno |
| 2026-09-11 | Clone JSON se zapiše, sinhronizira in validira pred atomsko zamenjavo | B1 / R9-f | ne | varni zapis |

Vse zgornje so popravki tihe izgube podatkov, zato so brez stikala in privzeto vklopljene.
Format datotek se ne spremeni, zato ni migracije. Izjema je zadnji stolpec pri praznem
ključu: tam original ni znal zapisati berljive datoteke, mi jo znamo, original pa jo zna
prebrati.

---

## Meritve

| Datum | Scenarij | Rezultat | Zapis |
|---|---|---|---|
| 2026-09-11 | NBT↔JSON round-trip, 4000 naključnih struktur | original 1598 napak (40 %), novi 0 | [zapis](meritve/2026-09-11-M1-nbtjson.md) |
| 2026-09-11 | NBT↔JSON zapis, 1500 ključev (68 KB) | 235,3 ms → 2,9 ms (81×) | [zapis](meritve/2026-09-11-M1-nbtjson.md) |
| 2026-09-11 | NBT↔JSON branje, 1500 ključev | 2386,3 ms → 6,5 ms (367×) | [zapis](meritve/2026-09-11-M1-nbtjson.md) |
| — | baseline MSPT še ni izmerjen (M2.6) | — | — |

Meritve so tekle na OpenJDK 21 v oblačnem okolju, ne na Javi 8. Ponovitev na Javi 8 je odprta.

---

## Znane omejitve

- Meritve so bile narejene na OpenJDK 21, produkcijski build je Java 8.
- Dekompilacija **ni** izvorna koda. To je bilo tokrat dvakrat potrjeno v praksi: dve trditvi
  iz prvotne analize sta bili napačni. Sumljivo logiko vedno preveri z `javap`.
- Vseh 746 dekompiliranih datotek **ni** ročno pregledanih.
- Združljivost z uporabnikovim dejanskim modpackom ni preverjena (Q1).
- Bugi iz `PLAN_IMPLEMENTACIJE.md`: B1 in B2 sta v M1.5/M1.6, B5 v M6.5. B3, B4, B6, B7, B8
  še niso razporejeni v milestone.
- Dedicated server in igranje v svetu še nista preverjena (M0.5).
- Že pokvarjenih datotek na disku nova koda ne popravlja; M1.4 je po navodilu uporabnika
  odložen, dokler ne obstaja konkreten primer.
