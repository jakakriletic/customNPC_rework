# CustomNPC Rework — 1.12.2

Dolgoročni rework moda **CustomNPCs za Minecraft 1.12.2**. Cilj ni nov mod od nule, ampak
postopna predelava obstoječega moda v verzijo z uporabnim letenjem, delujočim jahanjem,
trdnimi hitboxi, Java scriptingom, animacijskim sistemom, AI chatboti in bistveno boljšim
performancem pri velikem številu NPC-jev.

Ta README je **vstopna točka**. Vsaka nova seja začne tukaj.

---

## Hitri status

| | |
|---|---|
| Datum zadnje posodobitve | 2026-09-24 |
| Trenutna faza | **M3 — jedro entitete**; M0, M1 zaključena, M2 razen zagona baselina M2.6 (odložen). M3.1 (prenos `EntityNPCInterface` + `ai/`) zaključen; **M3.2 zaključen: vzrok R1 potrjen v svetu** — vanilla `EntityLiving.updateEntityActionState` jahača nosilcu vsak tick izbriše pot |
| Naslednji korak | **M3.3** (`RiderState`: kdo krmili — nosilec ali jahač) in popravek R1 pod stikalom. Glej [`docs/04-STANJE.md`](docs/04-STANJE.md) |
| Build base | uradni `CustomNPCs_1.12.2-(01Oct19).jar`, SHA-256 `cafacade…00fa1` |
| Prevedljivih razredov | 74 v `src/patch` (37 iz M3.1 + prejšnji), v runtime JAR-u 99 prevedenih razredov; podrobnosti v [`docs/04-STANJE.md`](docs/04-STANJE.md) |
| Okolje deluje | da — `runClient` in `runServer` naložita Forge + CustomNPCs; NPC preživi save + restart (M0.5); testni svet z 8 NPC-ji in skripto preživi restart (M0.6) |

**Pomembno:** jedro popravka R9 (tipno varen NBT↔JSON serializer) je implementirano in
testirano. Uporabnikov konkretni simptom je zelo majhen robni primer: NPC z vlogo follower,
ki je ob kloniranju v stanju `waiting`, se lahko vrne v `following`. Ker ni pokvarjenih
datotek za obnovo in ima mod pomembnejše težave, se dodatna forenzika R9, migracija ter
`auditClones` **odložijo**. Že narejenega splošnega popravka ne odstranjujemo.

---

## Kazalo dokumentacije

| Dokument | Kaj vsebuje | Kdaj ga bereš |
|---|---|---|
| **README.md** (ta datoteka) | pregled, status, kazalo | vedno prvo |
| [`docs/01-ARHITEKTURA.md`](docs/01-ARHITEKTURA.md) | odločitve, struktura, kompatibilnostna politika, licence | preden se lotiš karkoli spreminjati |
| [`docs/02-ZAHTEVE.md`](docs/02-ZAHTEVE.md) | R1–R9 podrobno, z dokazi iz kode (`datoteka:vrstica`) | ko delaš na konkretni zahtevi |
| [`docs/03-FAZE.md`](docs/03-FAZE.md) | milestoni M0–M10, delovni paketi, odvisnosti, izhodni kriteriji | ko načrtuješ sejo |
| [`docs/04-STANJE.md`](docs/04-STANJE.md) | **živ dnevnik** — kaj je narejeno, kaj teče, kaj je blokirano | vedno drugo |
| [`docs/05-SEJA-PROTOKOL.md`](docs/05-SEJA-PROTOKOL.md) | kako seja začne, dela in zaključi | vedno, tudi na koncu seje |
| [`OKOLJE.md`](OKOLJE.md) | razvojno okolje, verzije, gradle ukazi | ko nekaj ne zbuilda |
| [`PLAN_IMPLEMENTACIJE.md`](PLAN_IMPLEMENTACIJE.md) | audit originala: bugi B1–B8 in performance kandidati | referenca; še vedno velja |
| [`docs/scenariji/M0.7-integracijska-matrika.md`](docs/scenariji/M0.7-integracijska-matrika.md) | 47 preverb obnašanja v svetu, postopek in pokritost | pred predajo vsakega paketa |
| [`docs/scenariji/`](docs/scenariji/) | ponovljivi scenariji: M0.5 smoke, M0.6 testni svet, M2.1 diagnostika, M2.2 R1, M2.3 R2, **M2.7 navigacija** | ko poganjaš ali spreminjaš scenarij |

---

## Devet zahtev na kratko

| ID | Zahteva | Faza | Velikost |
|---|---|---|---|
| **R1** | NPC mount — jahanje NPC na NPC uniči AI in premikanje nosilca | M3 | velika |
| **R2** | Letenje — creative-style in ender-dragon-style, osnova za letala | M4 | velika |
| **R3** | Migracija funkcij iz CustomNPC+ (1.7.10) na 1.12.2 | M8 (+ raztreseno) | zelo velika |
| **R4** | AI-prijazno okolje za izdelavo NPC animacij in integracijo v skripte | M7 | zelo velika |
| **R5** | Performance optimizacija NPC AI — čim več NPC-jev v svetu | M5 | velika |
| **R6** | Solid hitbox — NPC-ja se ne da odriniti (kot v 1.16.5) | M3 | majhna |
| **R7** | Scripting v Javi namesto Nashorn JavaScripta, optimiziran za AI | M6 | zelo velika |
| **R8** | Chatbot v NPC-jih, API ključ v GUI | M9 | srednja |
| **R9** | Clone follower: `waiting` se lahko po kloniranju vrne v `following` | M1 | majhna; jedro popravljeno, dodatno delo odloženo |

Podrobnosti, dokazi iz kode in kaj je še treba preveriti: [`docs/02-ZAHTEVE.md`](docs/02-ZAHTEVE.md).

---

## Vrstni red faz

```
M0  Temelj                      okolje, build, testi, git            ← zakljuceno (M0.8 = zabelezena blokada)
M1  Integriteta podatkov        R9 + B1/B2, atomski zapis, migracija ← zaključeno
M2  Diagnostika in meritve      reprodukcije, profiling, baseline    ← smo tu (M2.1, M2.2 zakljucena)
M3  Jedro entitete              R1 mount, R6 solid hitbox
M4  Gibanje in navigacija       R2 letenje, 3D pathfinding, kopenska navigacija
M5  Performance AI              R5, odstranitev globalnega script locka
M6  Scripting platforma         R7 Java scripting, hook registry
M7  Animacije + AI okolje       R4 animacijski sistem in avtorsko okolje
M8  Migracija CustomNPC+        R3 preostale funkcije
M9  Chatbot                     R8
M10 Release kandidat            soak, rollback vaja, dokumentacija
```

Zakaj ta vrstni red:

1. **Podatki prvi.** Serializer za R9 je popravljen. Dokončamo še splošno zaščito zapisovanja
   (B1/B2), ker varuje ves svet; specifične forenzike minornega clone simptoma ne širimo brez
   konkretnega primera.
2. **Meriti pred optimiziranjem.** Brez reprodukcije R1 in brez baseline meritev ni mogoče
   dokazati, da smo karkoli popravili ali pohitrili.
3. **Entiteta pred gibanjem.** R1 in R6 oba posegata v hitbox, kolizije in passenger logiko
   v `EntityNPCInterface`. Letenje (R2) gradi na očiščenem navigator/moveHelper sloju.
4. **Scripting pred animacijami in chatbotom.** R4 in R8 sta odvisna od hook sistema in
   tipiziranega API-ja, ki nastane v R7.

---

## Kako nadaljevati v novi seji

1. Preberi ta README.
2. Preberi [`docs/04-STANJE.md`](docs/04-STANJE.md) — tam je točno kje smo.
3. Preberi [`docs/05-SEJA-PROTOKOL.md`](docs/05-SEJA-PROTOKOL.md) in se ga drži.
4. Za paket, ki ga delaš, preberi ustrezni razdelek v
   [`docs/03-FAZE.md`](docs/03-FAZE.md) in [`docs/02-ZAHTEVE.md`](docs/02-ZAHTEVE.md).
5. Na koncu seje **obvezno** posodobi `docs/04-STANJE.md`.

Vse odločitve, ki jih sprejmeš med sejo, gredo v `docs/01-ARHITEKTURA.md` pod
"Dnevnik odločitev". Nič pomembnega ne sme obstajati samo v chatu.

---

## Osnovni ukazi (PowerShell, iz korena mape)

```powershell
.\obnovi-okolje.ps1                        # nova delovna postaja: obnovi dev/libs in preveri vse
.\dev.ps1 setupDecompWorkspace --offline   # priprava Minecraft/Forge odvisnosti
.\dev.ps1 testOriginal test --offline      # testi originala + obnovljene kode
.\dev.ps1 buildPatchedMod --offline        # sestavi lokalni testni mod
.\verify-package.ps1                       # dokaže, kaj točno se je spremenilo proti originalu
.\dev.ps1 runClient --offline              # razvojni klient
.\dev.ps1 runServer --offline              # razvojni dedicated server
.\smoke-server.ps1                         # M0.5 server smoke (skriptiran, 13 preverb)

.\testworld-run.ps1                        # M0.6 testni svet, skriptiran scenarij od zacetka do konca
.\testworld.ps1                            # samo postavi seme testnega sveta
.\verify-testworld.ps1                     # ovrednoti merila W1-W8 iz loga

.\matrika-run.ps1                          # M0.7 prehod 1 integracijske matrike (vse stopnje)
.\rwdiag-run.ps1                           # M2.1 instrumentacija, merila D1-D7 in C1-C6
.\r1-run.ps1                               # M2.2 reprodukcija R1 (NPC jaha NPC), merila E1-E6
.\r2-run.ps1                               # M2.3 reprodukcija R2 (leteci NPC in ovira), merila L1-L8
.\nav-run.ps1                              # M2.7 merila kakovosti navigacije, merila N1-N12
.\ponovitve-run.ps1                       # M2.5c tri ponovitve scenarija, sumni pas, merila T1-T6
.\ponovitve-samotest.ps1                  # M2.5c preverba samega protokola, brez Minecrafta
```

Instrumentacija (M2.1), v konzoli serverja ali kot ukaz v igri:

```
rwdiag on            # vklopi merjenje in pocisti stevce
rwdiag chunks on 1   # pogoj meritve: prisilno nalozi chunke z NPC-ji (M2.1d)
rwdiag dump m21      # zapise posnetek v dev\run\logs\rwdiag\ in ga izpise
rwdiag off           # izklopi in se odjavi z event busa

rwdiag nav -10 4 78 3 32 NAV_WalkG   # M2.7: sonda poisce pot do cilja za vsak merjeni NPC
rwdiag nav status                    # skupna vrstica sonde
```

Od M2.7 ima posnetek tudi razdelek **kakovosti navigacije**: delež celih poti, razmerje
med dolžino poti in zračno razdaljo, doseg delnih poti in čas enega iskanja. Številke
nastanejo iz lastnih iskanj sonde (`rwdiag nav`), ne iz opazovanja AI-ja — razlog je v
[`docs/scenariji/M2.7-navigacija.md`](docs/scenariji/M2.7-navigacija.md).

Posnetek ima od M2.5a tabelo **najpočasnejših tickov s kontekstom** (koliko NPC-jev je
tiknilo, koliko chunkov se je naložilo, ali je tekel autosave). Percentil pove, koliko
tickov je počasnih; tabela pove, kateri in kaj je v njih teklo.

Podrobnosti in opozorila: [`OKOLJE.md`](OKOLJE.md).

---

## Struktura mape

```
CustomNPC_mod_rework/
├── README.md                    ← ta datoteka
├── OKOLJE.md                    razvojno okolje
├── PLAN_IMPLEMENTACIJE.md       audit originala (B1–B8)
├── obnovi-okolje.ps1            obnova okolja na novi delovni postaji
├── smoke-server.ps1             M0.5 dedicated-server smoke test
├── docs/                        načrt reworka
│   ├── 01-ARHITEKTURA.md
│   ├── 02-ZAHTEVE.md
│   ├── 03-FAZE.md
│   ├── 04-STANJE.md             ← živ dnevnik
│   ├── 05-SEJA-PROTOKOL.md
│   └── scenariji/               ponovljivi integracijski scenariji
├── testworld-run.ps1            M0.6 skriptiran scenarij testnega sveta
├── testworld.ps1                postavi testni svet iz semena
├── verify-testworld.ps1         ovrednoti merila testnega sveta
├── dev/                         gradle projekt
│   ├── testworld/               seme ponovljivega testnega sveta
│   ├── src/patch/java/          razredi, ki se dejansko prevajajo
│   ├── src/test/java/           testi
│   ├── reference-src/           dekompiliran izpis z mapiranimi imeni (branje)
│   ├── baseline/                nespremenjen original (izven classpatha)
│   └── libs/                    mapiran original + JUnit
├── audit/                       dekompilacija, logi, verifikacije
├── .tools/jdk8/                 lokalni Temurin 8
└── CustomNPCs_1.12.2-(01Oct19).jar   referenčni original, se nikoli ne spreminja
```

---

## Trdna pravila

Ta pravila veljajo za vsako sejo in vsak commit. Podrobna obrazložitev je v
[`docs/01-ARHITEKTURA.md`](docs/01-ARHITEKTURA.md).

- Original JAR se **nikoli** ne prepiše ali spremeni.
- Vsak popravek ima **reprodukcijo in test**, preden se šteje za narejenega.
- `verify-package.ps1` mora po vsakem buildu pokazati **točno pričakovan** seznam
  spremenjenih razredov. Nepričakovana sprememba je napaka, ne slučaj.
- Performance izboljšava brez meritve pred/po **ne obstaja**.
- Dekompilacija ni izvorna koda. Sumljivo kontrolno logiko pred popravkom preveriš z
  `javap` na originalnem bytecode.
- Brez velikih nepovezanih refaktorjev. En paket = en commit = ena stvar.

---

## Zunanji viri

- [Custom NPCs 01Oct19 na CurseForge](https://www.curseforge.com/minecraft/mc-mods/custom-npcs/files/2799797) — referenčni original
- [BetaZavr/CustomNPCs_1.12.2-Unofficial](https://github.com/BetaZavr/CustomNPCs_1.12.2-Unofficial) — referenčna implementacija, CC BY-NC 3.0
- [KAMKEEL/CustomNPC-Plus](https://github.com/KAMKEEL/CustomNPC-Plus) — 1.7.10 fork z Java scriptingom (Janino), animacijami, flying AI; glavni vir za R3, R4, R7
- [Noppes/CustomNPCsAPI](https://github.com/Noppes/CustomNPCsAPI) — uradni API vmesniki
- [Noppes/cnpcs-scripting-examples](https://github.com/Noppes/cnpcs-scripting-examples) — primeri skript
- [Forge 1.12.2](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.12.2.html)
