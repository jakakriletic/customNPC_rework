# Arhiv: dnevnik sej (vnosi 186 → 1)

Dobesedno prenesen iz `docs/04-STANJE.md` (razdelek "Dnevnik sej") 2026-10-19. Novejši vnosi so
zgoraj. Povezave so popravljene na relativno pot iz mape `docs/arhiv/`; besedilo je nespremenjeno.
V `docs/04-STANJE.md` ostaneta samo zadnja 2 vnosa; ko se doda nov, se najstarejši od teh dveh
prestavi **na vrh** te datoteke (pod ta uvod).

---

### 2026-10-29 (186) — M10.0: raziskava release kandidata

**Paket:** M10.0 (D-096, Q207–Q214) · **Stanje:** zaključen, raziskava brez kode in testov.
Pokritost matrike M0.7 osvežena z dokazi M3.8b, M4.15c, M6 in M7 (npr. IS6/IS7, IL2, IK1 zeleni); ostane
31 nepokritih ali delnih vrstic. Release se primerja z **distribucijskim** originalom na pakiranem strežniku
z isto JS sledjo (D-096). M10.1 razdeljen na M10.1a (strežnik) in M10.1b (klient); vrstni red M10.1a →
M10.1b → M10.5 → M10.4 (4 h) → M10.2 → M10.6 → M10.7. [Raziskava](../raziskave/M10.0-release-kandidat.md).
Naslednji M10.1a.

### 2026-10-29 (185) — M7.5: animacijski hooki start/end/event

**Paket:** M7.5 (Q203–Q206) · **Stanje:** zaključen, sprejet v poskusu 1 na `e80e2658…`.
Hooki `animationStart`/`animationEnd`/`animationEvent` za JS in Java prek `DataScript.runScript`, za stikalom
`RwAnimHooks` (privzeto 0, pri 0 brez poslušalca). `AnimationHooks` + čisti `HookTracker` berejo ACTION enkrat na
world tick; entities mod nespremenjen. unit/build/package, m75-api-v1 (57 tipov/626 članov, 19 hookov),
m75-hooks-on-v1 (JS = Java pripeta sled), m75-hooks-off-v1 (noben hook pri 0) in m62-packaged-v3 16/16 PASS;
opazovalec 0,953 µs/world tick (podatek). [Dokazi/omejitve](../meritve/2026-10-29-M7.5-animacijski-hooki.md).
M7 v1 zaključen; naslednji M10.1.

### 2026-10-29 (184) — M7.9: refleksijski script animator

**Paket:** M7.9 (Q200–Q202) · **Stanje:** zaključen, sprejet v poskusu 1 na `1a2a2b75…`.
`Animators.of(npc)` / `IAnimator`, FIFO največ 32 na entiteto, šibke reference in poslušalec samo
ob čakajoči vrsti; originalni razredi in entities mod nespremenjeni. Šest unit primerov, API
generator in `animation-api-run.ps1` za JS/Java z dejanskim modom ter brez njega.
unit/build/package, m69-api-v1 (53 tipov/614 članov), m79-api-v1 28/28 (JS = Java FIFO
`hurt,idle,hurt`) in m79-absent-v1 10/10 PASS; getterji 1,41 µs/trojico (podatek).
[Dokazi/omejitve](../meritve/2026-10-29-M7.9-animator-api.md). Naslednji M7.5 → M10.

### 2026-10-29 (183) — M7.V: zagon rework + entities 0.7.0

**Paket:** M7.V (Q198–Q199) · **Stanje:** zaključen, poskus 2, brez produkcijske kode.
Nov harness `m7v-entities-run.ps1` (pakiran dedicated strežnik, rework + `customnpcentities-0.7.0`).
Controller **m7v-entities-v2 PASS na `2905a99d…`**: 5 mobov, vseh 37 klipov predvajanih in
končanih (1.134 vzorcev, hitbox ves čas = definicija moba), save/stop/start ohrani UUID, razred,
hitbox in animacijski NBT; brez povezovalnih in nalagalnih napak. Poskus 1 lažno negativen
(INFO »missing mods«, Q199). [Dokazi/omejitve](../meritve/2026-10-29-M7.V-entities-zagon.md).
Naslednji M7.9 → M7.5 → M10.

### 2026-10-29 (182) — M7.0: raziskava integracije animacij

**Paket:** M7.0 (D-095, Q192–Q197) · **Stanje:** zaključen, raziskava brez produkcijske kode.
javap: vsi CNPC člani, ki jih uporablja `customNPC_entities_mod` 0.7.0, obstajajo v 01Oct19 z
istimi deskriptorji; manjka zagon obeh modov (M7.V). Most `rework/anim` z refleksijo, entities
mod nespremenjen; API `Animators.of(npc)`, hooki `animationStart/animationEnd/animationEvent`
(`RwAnimHooks` privzeto 0). M7.8 reload in `blendTo` v2.
[Raziskava](../raziskave/M7.0-animacije-integracija.md). Naslednji M7.V → M7.9 → M7.5 → M10.

### 2026-10-29 (181) — M6.11: player/world iz NPC Java skripte

**Paket:** M6.11 (Q188–Q191) · **Stanje:** zaključen, poskus 1.
V objavljen v1 API pridejo samo `IPlayer` in `IWorld` (51 tipov/606 članov) ter preveden
`Example_playerWorld` v HOOKS.md; `NpcScript`/runtime ista. Controller **PASS na `e3e30ca1…`**:
unit/build/package, m69-api-v1 6.761 preverjanj in m611-packaged-v1 12/12 (sled JS = Java iz MC
stanja, zapis bloka, tempdata, `LossyTime.java:5`/`UnknownPlayer.java:5` brez izvajanja). Ogret
par kot podatek: JS 8,80 µs, Java 2,07 µs/interact; cold Java compile 287 ms.
[Dokazi/omejitve](../meritve/2026-10-29-M6.11-player-world.md). Q92 usklajen; M6 v1 zaključen, naslednji ostanek M7.

### 2026-10-29 (180) — M6.12: Java v obstoječi izbiri jezika

**Paket:** M6.12 (Q184–Q187) · **Stanje:** zaključen, poskus 1.
`JavaLanguage` doda `Java` v obstoječi seznam jezikov samo pri nepraznem `RwJavaClasspath`
(privzeto originalni seznam); samo NPC GUI, ostali skriptni GUI brez Jave. Ne-Java pot v
`DataScript` zavrže staro Java generacijo. Controller **PASS na `48c60f8e…`**: unit/build/package,
m612-gui-v1 101 trditev (JS→Java napaka→Java+vključitev→JS→brez compilerja→Java, clone, JVM
restart, 8 pregledanih PNG) in m62-packaged-v3 16/16. Samodejni modpack classpath v2 (Q184).
[Dokazi/omejitve](../meritve/2026-10-29-M6.12-izbira-jezika.md). Q92 usklajen; naslednji M6.11.

### 2026-10-29 (179) — M6.10: Java 8 IDE predloga

**Paket:** M6.10 (Q181–Q183) · **Stanje:** zaključen, poskus 1.
`scripts/java/template/`: Gradle Java/IDEA/Eclipse, dejanski API classpath, isti compile-only
CLI in typed StarterNpc z vključitvijo. Controller **m610-template-v1 PASS na `53ca9c0d…`**:
53 preverjanj, IDE/CLI exit 0, negativni CLI exit 1 z `WrongHook/WrongType/BrokenCounterText.java:5`
in compiler kodami; razredi brez inicializacije, isti binder. Ukaz 14.470 ms kot podatek.
Produkcija/JS/Java runtime isti; brez ponavljanja M6 serij (Q90).
[Dokazi/omejitve](meritve/2026-10-29-M6.10-predloga.md). IDE uporabniška seja/Forge izvajanje
ni nov dokaz. Q92 usklajen; naslednji M6.12.

### 2026-10-29 (178) — M6.9: generator API reference

**Paket:** M6.9 (Q177–Q180) · **Stanje:** zaključen, poskus 1.
Generator iz javac API simbolov in istega hook registry pripravi API.json/API.md/HOOKS.md.
Izrecni v1 nabor/izključitve, generiki, dedovanje, event polja in 16 Java primerov;
odsoten Javadoc/imena so označena. Razvojni sourceSet ostane izven moda; runtime isti.
Controller m69-api-v1 **PASS na `0ff903b4…`**: 5.661 preverjanj, 49 tipov/515 članov,
16 prevedenih primerov, napačen tip zavrnjen z lokacijo; neodvisni popis in bajtna enakost
med JVM/obrnjenim odkrivanjem potrjena. Celoten ukaz 7.372,120 ms kot podatek.
[Dokazi in omejitve](../meritve/2026-10-29-M6.9-api.md); produkcija/zapakirani mod isti,
brez Minecrafta ali izvajanja primerov. Q92 usklajen; naslednji M6.10, brez ponovitev.

### 2026-10-29 (177) — M6.8: trusted-only pravice in meje

**Paket:** M6.8 (D-094, Q173–Q176) · **Stanje:** zaključen, poskus 1.
Controller unit/build/package in m68-packaged-v1 **27/27 PASS** na `36620163…`.
Dejanski packet handler/ukaz reload potrdita JS/Java deny/grant, nespremenjen handler
ob zavrnitvi ter op reload; Java file/reflection marker potrdi odsotnost izolacije.
Provider/config obnovljena, SecurityManager isti; produkcijske pravice/runtime isti.
Originalni default ALL ni trust meja; profil za nezaupane igralce zahteva zaupane op
avtorje (`OpsOnly=true`) ali izrecno omejene vse urejevalne/uvozne poti.
[Politika, omejitve in dokazi](../meritve/2026-10-29-M6.8-pravice.md), sonda 351,797 ms kot
podatek, brez sandbox/GUI/login/plugin trditve. Q92 usklajen, naslednji M6.9; brez ponovitev.

### 2026-10-29 (176) — M6.7: compile-only CLI

**Paket:** M6.7 (D-090/D-093, Q169–Q172) · **Stanje:** zaključen, poskus 2 (harness).
`compileScripts` uporablja isti JDK compiler in skupni `JavaSources` kot runtime, manifest
urejenih tabov/vključitev ter JSON diagnostiko/exit status brez nalaganja skript.
Dodanih devet JUnit testov in `java-cli-run.ps1`. [Merila in odločitve](../meritve/2026-10-29-M6.7-cli.md).
Controller na `e00c80cf…`: CLI in m62 16/16 PASS; ukaza CLI 4,979/4,881 s,
tripwire za static/konstruktor/procesor ni nastal. M65 14/15 zaradi starega null checka;
popravek samo sonde/harnessa potrjen na `3e58697d…`: unit/build/package in m65 17/17 PASS,
caller ter vrhnji cleanup potrjena. CLI/m62 brez ponavljanja, produkcija ista.
Q92 usklajen; naslednji M6.8. Plain hook bind in samodejni modpack classpath nista CLI dokaz.

### 2026-10-29 (175) — M6.6: reprodukcija in obnova gnezdenega lastništva

**Paket:** M6.6 (D-090/D-092, Q164–Q168) · **Stanje:** zaključen, poskus 2 (harness).
Reprodukcija 48/48 na `01f08637…`; končni unit/build/package, m66-context-v1 88/88 in
m62-packaged-v3 16/16 PASS na `af46a352…`. JS/Java v finally obnovita prejšnji Current,
JS tudi CurrentType; binarno polje in lock ostaneta. Prvi m62 13/14 je bil zastarel null check
v živem controller tabu; popravek harnessa ločeno potrdi caller in vrhnji null ob return/throw,
zlate sledi iste. [Zapis](../meritve/2026-10-29-M6.6-context.md). GUI/impact/async in hitrost
niso dokazovani. Q92 usklajen; brez ponavljanja uspešnih testov, naslednji M6.7.


### 2026-10-29 (174) — M6.5: izvor napak, source map vključitev, B5

**Paket:** M6.5 (D-090, Q157–Q163) · **Stanje:** zaključen, poskus 2 (harness). Baseline `ScriptContainer` `5fbda3f`.
B5 popravljen v JS `ScriptContainer` (reload pred zgodnjimi izhodi) in Java (nova generacija počisti `errored`);
izvor napak `SourceMap` (JS `Script source:`, Java `[tab N|include ime]` in skrajšan stack). [Zapis](../meritve/2026-10-29-M6.5-napake.md).
Prvi controller m65 FAIL zaradi pričakovanega števila 14 namesto 15; popravek samo harnessa (Q163). Vseh šest zahtevanih testov PASS na `7ec18d6e…`: unit/build/package, m65 15/15, m62 14/14 in m64 15/15. Originalni bajti reproducirajo obe B5 napaki; popravek obnovi tudi prej manjkajoč hook. Omejitve Q162 ostanejo; naslednji pogojni M6.6 (najprej reprodukcija), nato M6.7 po Q92.


### 2026-10-29 (173) — M6.4: loader na NPC/tab/generacijo, bytecode cache, atomski reload

**Paket:** M6.4 (D-090, Q151–Q156) · **Stanje:** zaključen, poskus 1.
`BytecodeCache` (LRU 64/16 MiB samo bajtov), generacija na handler z lastnim loaderjem/instanco na tab;
pokvarjen reload pusti zadnjo veljavno generacijo. [Zapis](../meritve/2026-10-29-M6.4-reload.md).
Na 9490206d… unit (5 `BytecodeCacheTest`), build, package, m64-packaged-v1 15/15, m62-packaged-v2 in
m63-packaged-v1 14/14 PASS. Izključitev testa iz `testOriginal` je harness (Q156). Naslednji M6.5.

### 2026-10-29 (172) — M6.3: tipizirana script pogodba `NpcScript`

**Paket:** M6.3 (D-090, Q148–Q150) · **Stanje:** zaključen, poskus 1.
Izbiren osnovni razred `rework/script/NpcScript` (16 no-op hookov z event tipi iz registryja + `log`)
na obstoječih `noppes.npcs.api` vmesnikih; `JavaHookBinder` veže samo prepise. [Zapis](../meritve/2026-10-29-M6.3-pogodba.md).
Na 2e9136a9… unit (5 `NpcScriptTest`), build, package in m63-packaged-v1 PASS 14/14: tipizirana sled = JS
zlati niz tudi po reloadu, `@Override` napaka z vrstico, navaden M6.2 razred nespremenjen. Naslednji M6.4.

### 2026-10-29 (171) — M6.2: popis hookov in Java adapter na NPC handlerju

**Paket:** M6.2 (D-090, Q142–Q147) · **Stanje:** zaključen, poskus 2 (harness).
Baseline `DataScript` ločen commit `cf498ae`; `rework/script/hooks/` (16 NPC hookov, string hook
pravilo, vezava podpisov) in Java veja v `DataScript.runScript` pri `ScriptLanguage=Java`; JS pot ista.
[En zapis meril, odločitev in rezultatov](../meritve/2026-10-29-M6.2-hooki.md).
Na c7d8bd67… unit/build/package in m62-dev-v1 PASS 14/14; m62-packaged-v1 11/14 (harness: compiler
classpath brez Forge universal, Q147). Na d4972147… (ista produkcija) m62-packaged-v2 PASS 14/14.
Omejitev: modpack rabi ročni `RwJavaClasspath` z Forge universal (M6.7/M6.12). Naslednji M6.3; nepovezane datoteke ohranjene.

### 2026-10-29 (170) — M6.1: JDK prevajalnik izbran in potrjen v Forge

**Paket:** M6.1 (D-090, Q132–Q141) · **Stanje:** zaključen, poskus 1.
JavaCompiler prototip z diagnostiko, brez annotation procesorjev/inicializacije;
stock Janino primerjava, realni no-compiler JRE in ločena dev/packaged Forge scenarija.
Originalni handlerji/privzeti JS isti; dva nova rework razreda in šest unit testov.
[En zapis meril, hashov in odločitev](../meritve/2026-10-29-M6.1-prevajalnik.md).
Na 03095534… unit/build/package in m61-compiler-v1 PASS; m61-dev-v1 padel pred zagonom
strežnika (harness: Gradle je pod `dev/build` izbrisal kopijo configa/EULA, Q137). Delovna mapa
premaknjena v `dev/run-m61/`. Na 4771a7b9… krmilni NPC ni tiknil (spawn svežega sveta daleč
od 0,0; harness zdaj pribije spawn v bootstrap zagonu, Q138). Na 7372b8a6… dev: vseh 15 typed preverb,
golden JS in časi OK; padla je le hash-kontrola `deobfedDeps/providedDummy.jar`, ki ga ForgeGradle
ob vsakem zagonu znova zapiše (enak vnos `dummyThing`, nov zip čas). Harness ta dva dummy jarja zdaj
primerja po vsebini vnosov (Q139). Na b05e6f7a… je varovalo kot tujo Javo štelo osirotel enkratni
Gradle daemon; harness zdaj počaka lastne potomce (Q140). Na 85cc41cc… m61-dev-v1 in m61-packaged-v1 PASS (15/15).
Q141: JDK JavaCompiler (warm 17–31 ms); Janino hitrejši, a brez lambd/preverbe generikov. Naslednji M6.2; nepovezane datoteke ohranjene.

### 2026-10-29 (169) — M6.0: raziskava Java scriptinga zaključena

**Paket:** M6.0 (D-090, Q127–Q131) · **Stanje:** zaključen z raziskavo po D-029.
Preverjena originalna koda/bytecode, stari profil in primarni viri; vsak M6 paket ocenjen.
JDK 8 JavaCompiler prvi kandidat, obstoječi typed API/handler in JS privzet; trusted-only,
cache bajtov z ločenim stanjem, M6.6 samo zaradi pravilnosti. Plan/zahteve usklajeni s Q92.
[En zapis dokazov, odločitev in meril](../raziskave/M6.0-java-scripting.md).
Samo dokumentacija: koda/harnessi/configi/lock isti, testov ali Minecrafta nismo zaganjali
in novih meritev ni. M6.1 mora šele dokazati compiler/Forge povezavo in čase; ni trditve
o delujočem Java runtime ali prihranku. Naslednji M6.1; nepovezane datoteke ohranjene.

### 2026-10-29 (168) — M3.8b: GUI, clone in restart zaključeni

**Paket:** M3.8b (Q123–Q126) · **Stanje:** zaključen, poskus 2, fizika/podatkovna pogodba ista.
Vsi štirje controller testi PASS na `b2719356…`: unit/build/package in m38b-gui-v1,
147/147 + 69/69; wand/Display paketi, globalno 0/1, HasHitbox, clone in save/restart potrjeni.
Šest istih UUID po drugem JVM, vseh 18 PNG vizualno pregledanih, config/options isti.
Prvi GUI FAIL (`667b844b…`, spodnji rob 251 > 240) ostane reprodukcija; razmik 21 px
da spodnji rob 233 na istem zaslonu, brez spremembe meril, ločljivosti ali fizike.
[En zapis meril/dokazil](../meritve/2026-10-29-M3.8b-gui.md).
Po PASS samo dokumentacija, brez ponovitev (Q90/Q92); naslednji raziskava M6.0 (D-029).
M3.9 gibanje igralca med dvema SOLID s tem še ni izmerjeno; nepovezane datoteke ohranjene.

### 2026-10-29 (167) — M4.15c: GUI in save/restart zaključena

**Paket:** M4.15c (Q117–Q122) · **Stanje:** zaključen, brez spremembe produkcije M4.15b.
Vseh šest zahtevanih controller testov zelenih na `c91ad234…`: unit/build/package,
integrirano s knjižnico/brez nje 70/70 ×2, dedicated s knjižnico/klient brez nje 68/68.
Wand → AI → Movement → Backend, izbira 0/1, hoja/let/hoja in save/restart istega UUID
potrjeni; vseh 30 novih PNG vizualno pregledanih, vsebina panela berljiva.
Config/runtime/options isti; dedicated properties in ops obnovljeni, čisti izhodi.
Prejšnji FAIL ostanejo FAIL; Q119–Q121 popravijo dokazno kodo, ne sprejemnih meril.
[En zapis dokazil in omejitev](../meritve/2026-10-29-M4.15c-klient.md).
Po uspešnih testih samo dokumentacija, brez ponovitev. Naslednji M3.8b po Q92;
M4.15a odložena potrditev ostane. Baseline, ZAZENI-BASELINE in audit izven commita.

### 2026-10-29 (166) — M4.15b: GUI izbira in strežniška pogodba zaključena

**Paket:** M4.15b (D-089, Q108–Q116) · **Stanje:** zaključeno, poskus 1 produkcije.
Osnovni prenos preverjen/potisnjen kot `599751f`; nato AI → Movement → Backend 0/1,
strežniško dejansko ozadje/razlog in legacy save brez spremembe izbire.
Vseh sedem zahtevanih controller testov zelenih na `d71d75d4…`: 34 properties trditev,
unit/build/package in 97/97 ×3 za library/global 1/1, 1/0, 0/1; config/runtime SHA isti.
Predhodna FAIL ostaneta FAIL; zaščita preveri iste surove properties zapise in obnovi
izvirne bajte pred končnim SHA. Privzeto vanilla, brez sprememb tick/gibalne kode.
[En zapis dokazil in omejitev](../meritve/2026-10-29-M4.15b-gui.md). Naslednji M4.15c:
klikanje, svet save/reload in klient/dedicated povezava še niso potrjeni.
Po uspešnih testih samo dokumentacija; brez ponovitev, baseline/ZAZENI-BASELINE/audit ohranjeni.

### 2026-10-29 (165) — M4.15a: analiza izvedenih meritev, odložena potrditev

**Paket:** M4.15a (D-088, Q98–Q107) · **Stanje:** **raziskovalno obravnavan, lestvici neuspešni**.
Na nespremenjenem `f0606b11…` controller samotest in kompatibilnost zelena (18/18),
obe šestcelični lestvici exit 1 zaradi U1 pri 2000 nedosegljivih (881/800 <1000 tarč).
Ostalih 10 celic uspešnih; neuspešni par ni A/B referenca, sprejemni seznam ni zelen.
Q106: brez znižanja U1 ali ponavljanja; potrditev 2000 in skupne porabe pomnilnika odložena.
Q107: naslednji neodvisni GUI M4.15b/c, vanilla/Baritone 0/1, strežniško dejansko ozadje
in razlog fallbacka; opozorilo o odvisnosti cene od prizora, brez obljube kapacitete.
[En zapis pogojev, rezultatov in odločitev](../meritve/2026-10-29-M4.15a-baritone.md).
Produkcija, harnessi in merilni vhodi po uspešnih testih isti; seja ni zaganjala testov.
Commit vsebuje paketni harness in dokumentacijo; nepovezani baseline, ZAZENI-BASELINE in audit ohranjeni.

### 2026-10-29 (164) — M4.3: Hover zaključen

**Paket:** M4.3 (D-087, Q93–Q97) · **Stanje:** **zaključen, poskus 1, privzeto original**.
`RwFlightMode=1` izbere `HoverMoveHelper`: hitrost navigatorja × MOVEMENT_SPEED,
približevanje, ustavljanje in yaw ≤20°/tick. Controller unit/build/package zeleni;
štirje sprejemni ID-ji zeleni na `3eba45ab…`, 180/180 median privzetega načina = M4.16,
H0–H4 potrjeni tudi po restartu 30 letečih. P 6/6 A/B/C, C/A 0,522; drsenje A/C 0.
Cena enkrat: patrulja/boj p95 8,913/2,425 ms (podatek po Q92, brez cenovne meje).
Ovire/domet in aktivno ponovno ukazovanje B ostanejo omejitve; celoten M4 ni sprejet.
[En zapis dokazil in odločitev](../meritve/2026-10-29-M4.3-hover.md). Testov seja ni zagnala,
po njih se spreminja le dokumentacija. Naslednji M4.15a/b/c; nepovezane datoteke ohranjene.

### 2026-10-29 (163) — M4.2: shranjeni način letenja zaključen

**Paket:** M4.2 (D-086, Q6/Q31/Q91) · **Stanje:** **zaključen**.
`FlightMode` + root int `RwFlightMode` (0/1/2, original brez ključa); helperji ostanejo isti.
Controller `unit`, `build`, `package`, `m416-letenje-v1` zeleni na `d7a9f810…`; 71 pričakovanih
vnosov, vseh 180 median = M4.16, razpon 0. Osem novih testov pokrije NBT/clone/setter,
ne celotnega save/restart entitete ali fizike. [Dokazila in odločitve](../meritve/2026-10-29-M4.2-nacin-letenja.md).
Q6: samostojni NPC-ji; Q31: samo načini brez dodatkov; Q91: root int po obstoječem vzorcu.
Naslednji M4.3 → M4.15a/b/c po Q92; Momentum/GUI M4.2b v2. Koda po testih nespremenjena,
uspešne serije se ne ponavljajo. Nepovezani controller, baseline, ZAZENI-BASELINE in audit ohranjeni.

### 2026-10-29 (162) — M4.17 zaključen po odločitvi lastnika

**Paket:** M4.17 (D-085, Q90) · **Stanje:** **zaključen**, sprejet pod stikalom, privzeto 0.
Lastnik je zahteval sprejem brez ponovne validacije 23 ID-jev (Q88). Dokaz je celotna serija
poskusa 4 na `b8f98d28…`: 11 obveznih ID-jev zelenih, letenje 180/180 enako poskusom 1–3,
patrulja p95 9,175 ≤ 10,0925 ms in boj 2,687 ≤ 2,8116 ms. `dev/src` se po 28. 10. 12:38 UTC ni
spremenil; nov odtis izvira iz popravka controllerja (OPS.9) in ID-ja `m417-mcp-cache-v1`.
Q87/Q88 zaprti, Q81 (privzeto 0), Q82 (vogal → M4.5) in Q84 ostanejo. Naslednji: M4.2.
[Zapis](../meritve/2026-10-26-M4.17-vidna-crta.md).

### 2026-10-29 (161) — M4.17: obnova manjkajočega MCP artefakta pred validacijo

**Paket:** M4.17 (D-085, Q89) · **Stanje:** **v teku**, sprejem ni zaključen.
Controllerjev unit na `adf047b4…` je uspel, build pa pade pri `extractMcpMappings`:
za `de.oceanlabs.mcp:mcp_snapshot:20171003-1.12` ni offline predpomnilnika.
Pregled potrdi samo starejša imenika 20170908/20170927; zaklenjene različice ne menjamo.
[Zapis](../meritve/2026-10-26-M4.17-vidna-crta.md).

**Q89:** controller naj prek obstoječega `dev.ps1` izvede `extractMcpMappings` brez
`--offline` (nov ID `m417-mcp-cache-v1`), nato vseh 23 obveznih ID-jev z običajnim
offline buildom. Katalog testov spremeni odtis, zato tudi unit potrebuje nov rezultat.
Ob neuspehu prenosa naj se pred meritvami razišče konkreten log. Meje in scenariji isti;
letenje 180/180 proti referencam, cena proti originalu istega odtisa (zgornji rob p95 × 1,1).
AI ni zaganjal testov/builda/Minecrafta in ni spreminjal kode, configa ali zaklepa okolja.
Q82/Q84 in privzeto 0 ostajajo; commit/push M4.17 čaka veljavno celotno validacijo.
Nepovezani runner, baseline in audit so ohranjeni.

### 2026-10-29 (160) — M4.17: celotna validacija na novem trenutnem odtisu

**Paket:** M4.17 (D-085, Q88) · **Stanje:** **v teku**, brez novih rezultatov.
Pregledani so diff, R2, plan in prejšnji zapis; delne paketne spremembe ostanejo ohranjene.
AI ni zaganjal testov, builda ali Minecrafta in ni spreminjal kode ali merilnih vhodov.

Trenutni controller odtis je
`adf047b4f6608eaf59f756abb6cd78f914a7e06f0bf98abd73c6713cd80935f9`,
različen od `b8f98d28…` iz seje 159. Aktivna naloga nima rezultatov. Q88: zahteva se vseh
23 ID-jev iz Q87: unit/build/package, m416-samotest-v1, m416-letenje-v1, oba originala
m416b ter štiri variante m417 za vsako verzijo v1–v4. Parametri ostanejo isti; nove verzije
ID-ja ne pomenijo zagona stare implementacije. Starih meritev ne prenesemo na nov odtis.

Za vsak A/B par cene velja original istega odtisa, meja p95 = zgornji rob razpona
originala × 1,1; prejšnji absolutni meji 10,0925/2,8116 ms sta zgodovinski. Letenje:
stikalo 0 = M4.16, vidna črta in kombinacija = ustrezni prejšnji seriji, vsakič 180/180.
Vse ponovitve se obravnavajo, brez izbire ugodnejših. Vogal Q82 in zid v kombinaciji Q84
ostajata odprta; pragov ne spreminjamo. Po rezultatih analiza, evidence in šele nato
commit/push M4.17. Nepovezani runner, baseline in audit ostanejo zunaj commita.

### 2026-10-29 (159) — M4.17 poskus 4 izmerjen v mejah; celoten obvezni seznam še čaka

**Paket:** M4.17 (D-085, Q87) · **Stanje:** **v teku**. Koda in merilni vhodi nespremenjeni.
AI ni zaganjal testov/builda/Minecrafta. [Zapis](../meritve/2026-10-26-M4.17-vidna-crta.md).

Na `b8f98d2898eb510ce9452868865cf20f3d9e586f532f38aa0e19e133634ae268` je vseh 11 ID-jev
poskusa 4 zelenih. Primerjane mediane: `1340` = `1129`, `1349` = `1138`, `1358` = `1146`,
vsakič **180/180**, vse nove serije z razponom 0. Patrulja p95 9,175 → 9,175 ≤ 10,0925 ms;
boj 2,556 → **2,687 ≤ 2,8116 ms**, `npc.us` 9,97 → 10,57, alokacija 12,4 → 12,6 MB/s.
Iz zadnjega RWPATH boja pred prepisom loga: `vidKlicev` 703.173, `vidCelic` 59.499.651,
`vidProstih` = `vidUnija` 698.471, zaprtih 0. Dva zavržena boja 28. 10. zaradi vnosa na
namizju nista uporabljena; veljaven je par 29. 10. (`0931`/`0956`).

**Q87:** M4.17 še ne zaključimo: seznam upravljalnika zahteva tudi 12 ID-jev poskusov 1–3,
ki imajo samo stare odtise. Zahtevamo jih brez spremembe kode ali parametrov. Vsako dodatno
letenje mora ostati 180/180 enako, vsaka cena v isti meji proti originalu istega odtisa;
morebitne neuspehe raziščemo. Stari rezultati ne nadomestijo trenutnih. Šele po vseh veljavnih
rezultatih končni sprejem pod stikalom, posodobitev registrov/plana, commit in push. Q81
(privzeto 0), Q82 (vogal → M4.5) in Q84 (zid v kombinaciji 5/6) ostanejo. Nato M4.2 → M4.3 →
M4.5 po D-038. Nepovezani `overnight/runner.mjs`, baseline in `audit/*` ohranjeni zunaj paketa.

### 2026-10-28 (158) — M4.17 poskus 3 izmerjen (enak izid, boj 0,001 ms čez mejo), poskus 4 v kodi

**Paket:** M4.17 (D-085, Q79–Q86) · **Stanje:** **v teku** — poskus 3 izmerjen in ni sprejet,
poskus 4 v kodi. [Zapis](../meritve/2026-10-26-M4.17-vidna-crta.md). AI ni zaganjal testov/builda/Minecrafta.

Na `346e8b08…` vseh 11 zahtevanih testov poskusa 3 zelenih. Letenje: `m416-letenje-v1` = M4.16,
`m417-letenje-vid-v3` in `-vidnad-v3` **180/180 enaka** poskusoma 1 in 2. Cena: patrulja p95
9,699 ≤ 10,09 (v meji); **boj p95 2,556 (2,425–2,621) → 2,884 > 2,883**, `npc.us` 9,53 → 11,53,
alokacija 12,4 → 13,0 MB/s (dogodek je bil vzrok alokacije poskusa 2). `vidCelic` 56,2 M na 690 k
preverb = 81 celic na preverbo, ~9.260/tick, ~43 ns na celico [K+M]. Q86: meja ostane; poskus 4 —
`RwPathNavigateFlying.cells` vzame chunk enkrat na stolpec (`Chunk.getBlockState` = `World.getBlockState`
po virih 2847) in zrak preskoči pred verigo `addCollisionBoxToList` (`NULL_AABB`); `FlightFollowTest`
+1 (451 JUnit). Zahtevani: unit, build, package, `m416-samotest-v1`, `m416-letenje-v1`,
`m417-letenje-vid-v4`, `m417-letenje-vidnad-v4` (180/180 = poskusi 1–3), `m416b-leteci-v1`,
`m417-leteci-vid-v4`, `m416b-letboj-v1`, `m417-letboj-vid-v4`.

### 2026-10-28 (157) — M4.17 poskus 2 izmerjen (enak izid, boj en korak čez mejo), poskus 3 v kodi

**Paket:** M4.17 (D-085, Q79–Q85) · **Stanje:** **v teku** — poskus 2 izmerjen in ni sprejet,
poskus 3 v kodi. [Zapis](../meritve/2026-10-26-M4.17-vidna-crta.md). AI ni zaganjal testov/builda/Minecrafta.

Na `b79530e1…` vseh 11 zahtevanih testov poskusa 2 zelenih. Letenje: `m416-letenje-v1` = M4.16,
`m417-letenje-vid-v2` in `-vidnad-v2` **180/180 enaka** poskusu 1 (dokaz unije potrjen v svetu).
Cena: patrulja p95 9,175 = original (v meji); **boj p95 2,556 → 2,818 > 2,812** (+10,3 %, en korak
ločljivosti), `npc.us` 9,69 → 11,02, alokacija 12,4 → 20,2 MB/s; `vidUnija` = `vidProstih` (vse proste
preverbe odloči unija). Vzrok preostanka [K]: Forge `GetCollisionBoxesEvent` v vsaki celici
`World.getCollisionBoxes` (World:1470). Q85: meja ostane; poskus 3 — dogodek enkrat pred zanko,
`FlightFollow.scan` (prepis zanke originala, ponovno uporabljen seznam, števec `vidCelic`),
`FlightFollowTest` +3 (450 JUnit). Zahtevani: unit, build, package, `m416-samotest-v1`,
`m416-letenje-v1`, `m417-letenje-vid-v3`, `m417-letenje-vidnad-v3` (180/180 = poskus 1),
`m416b-leteci-v1`, `m417-leteci-vid-v3`, `m416b-letboj-v1`, `m417-letboj-vid-v3`.

### 2026-10-28 (156) — M4.17 poskus 1: vidna črta izmerjena, ni sprejeta (cena v boju)

**Paket:** M4.17 (D-085, Q79–Q84) · **Stanje:** **v teku** — poskus 1 izmerjen in zapisan,
poskus 2 (enak izid, nižja cena) v kodi. [Zapis](../meritve/2026-10-26-M4.17-vidna-crta.md). AI ni
zaganjal testov/builda/Minecrafta.

Koda poskusa 1: nov `rework/nav/FlightFollow` (pravilo brez sveta), prepis
`isDirectPathBetweenPoints` v `RwPathNavigateFlying`, stikalo `RwFlyLineOfSight` (privzeto 0,
`/rwpath vidno`), `FlightFollowTest` +10; harness `letenje-run.ps1 -VidnaCrta`, `perf-run.ps1
-RwVidno`; ključ `RwFlyLineOfSight=0` ročno v `dev/run/config`. Na `75269123…` vseh 11 zahtevanih
zelenih: stikalo 0 = M4.16 (180/180); labirint 0 → 0,125 b/t (B 6/6, z M4.10 A 6/6); vogal 0/6
(pot se že v originalu konča pred krakom → iskanje, M4.5, Q82); z M4.10 zid 6/6 → 5/6 (Q84,
odprt pojav); patrulja p95 8,913 = original; **boj p95 2,490 → 3,277 (+32 %)** čez mejo → ni
sprejet (Q83). Poskus 2: `FlightFollow.sweep` najprej preveri unijo vseh korakov (ena preverba
namesto ~12), izid dokazljivo enak; nov števec `vidUnija`, `FlightFollowTest` +3 (unija = koraki na 6.000 naključnih premikih po progah M4.16; 447 JUnit). Zahtevani: unit, build, package, `m416-samotest-v1`,
`m416-letenje-v1`, `m417-letenje-vid-v2`, `m417-letenje-vidnad-v2` (180/180 = poskus 1),
`m416b-leteci-v1`, `m417-leteci-vid-v2`, `m416b-letboj-v1`, `m417-letboj-vid-v2`.

---


### 2026-10-26 (155) — M4.10 izmerjen in zaključen

**Paket:** M4.10 (D-084, Q75–Q78) · **Stanje:** **zaključeno**, pod stikalom `RwPathResume`,
privzeto 0. [Zapis](../meritve/2026-10-26-M4.10-nadaljevanje.md). AI ni zaganjal testov/builda/Minecrafta.

Seja 154 je napisala kodo (`PathResume`, `RwPathNavigateGround`, nov `RwPathNavigateFlying`,
`PathResumeTest` +12; 434 JUnit v 70 datotekah, 184 razredov, 119 v rework) in harness
`-Nadaljevanje`; na 8d2dc7f5… DW 0/6 pri 600 tickih (Q78: 48 blokov pri 0,066 b/t ne gre v fazo),
zato `-TrajanjeFaze` in ID-ja `m410-letenje-dolgo-*`. Na `b53a0638…` zeleni unit, build, package,
`m416-samotest-v1`, `m416-letenje-v1` (180/180 = M4.16), `m410-letenje-nad-v1` (DF/Z 6/6),
`m410-letenje-dolgo-orig-v1` (DF/DW `navig0` 0/6), `m410-letenje-dolgo-nad-v1` (DF 6/6 220/230,
DW 6/6 710/720), `m410-nav-orig-v1`/`-nad-v1` (12/12 `pot.*` enakih, µs v šumu),
`m414b-navigator-harness-v3`; `m414b-navigator-trace-v3` pade samo na FA kot D-079. Privzeto 0
(Q77). Naslednji M4.17.

### 2026-10-26 (154) — M4.10: nadaljevanje delne poti in vmesni cilj (koda, čaka A/B)

**Paket:** M4.10 (D-084, Q75–Q77) · **Stanje:** **v teku** — koda in harness narejena, testi
zahtevani. AI ni zaganjal testov/builda/Minecrafta.

Raziskava ni bila potrebna (M4.0 §1.2 in §5 pokrijeta premiso). Nov `rework/nav/PathResume`
(pravilo brez sveta), `RwPathNavigateGround` dopolnjen, nov `RwPathNavigateFlying` (namesto
vanilla `PathNavigateFlying` v `EntityNPCInterface`). Pri `RwPathResume=1` (privzeto 0,
`/rwpath nadaljuj`): cilj manhattansko ≥ domet → vmesni cilj domet − 4; prehojena pot, ki ni
končala pri cilju → novo iskanje od tam; konec ob cilju, brez napredka ≥ 1 blok, 16 obnov ali
`clearPath`; obnova mimo S5. `PathResumeTest` +12 (434 JUnit v 70 datotekah; 184 razredov,
119 v rework, po grep). `letenje-run.ps1` in `nav-run.ps1` dobita `-Nadaljevanje`. Q75: ključ
`RwPathResume=0` ročno dopisan v `dev/run/config` (sicer ga nalagalnik dopiše sredi prve meritve).
Zahtevani: unit, build, package, `m416-letenje-v1`, `m410-letenje-nad-v1`, `m410-nav-orig-v1`,
`m410-nav-nad-v1` in `m414b-*`.

### 2026-10-26 (153) — M4.14d poskus 2 izmerjen in zavrnjen

**Paket:** M4.14d (D-083, Q72–Q74) · **Stanje:** **zavrnjeno, odloženo**.
[Zapis](../meritve/2026-10-26-M4.14d-poskus2.md). AI ni zaganjal testov/builda/Minecrafta.

Poskus 2: opravičilo ne vsili iskanja, preverba ravne črte (`straightWalkable`), TRACK samo
naprej ob zaprti črti, eno iskanje na obtičanje; unit +12 (Q73: M4.14b test prilagojen z zidom).
Na 6f39a760… unit, build, package in navigator-harness-v3 zeleni; navigator-trace-v3 izhod 1:
G `iskanj` 18 → 4 (+8 TRACK), obtičanja 3 (≤ 6), a **G DONE 288 > 246**, **FA5 G 5 > 4**,
FA4 G 9,326. Q74: ni sprejet; koda v `bf02674`, produkcija in testi povrnjeni na `cbad3c3`
(422 JUnit v 69 datotekah, 182 razredov, po grep). M4.14d odložen do per-članske analize zamude
ob vratih. Naslednji M4.10.

### 2026-10-26 (152) — M4.16b: celica `leteci-200` izmerjena

**Paket:** M4.16b (D-082, Q69–Q71) · **Stanje:** **zaključeno**.
[Zapis](../meritve/2026-10-26-M4.16b-leteci-200.md). AI ni zaganjal testov/builda/Minecrafta.

Brez produkcijske kode. Varianti `leteci` (N × `PERF_Leteci`, tava v `WalkingRange 16`) in
`leteci-boj` (`PERF_LetA/LetB`, frakciji 1↔2) v `perf-run.ps1`; krmilnik izpiše `letecih`,
`vZraku`, `odDoma`, merili L1/L2. Na 34b3574d… unit/build/package, `m416b-leteci-v1` in
`m416b-letboj-v1` zeleni (T1–T6; v vsakem zagonu P1–P9, L1, L2 oz. P5). Patrulja: p50 1,475,
p95 8,651 (8,651–9,175), p99 10,224 ms, `npc.us` 17,16, 65 MB/s, `odDoma` 197–200. Boj: p50 1,507,
p95 2,228 (2,228–2,294), p99 2,753 ms, `npc.us` 7,62, 12,4 MB/s, s ciljem/ranjenih 200/200.
Okvirno proti M2.6 (Q71, drug dan in koda): patrulja dražja od stoječega `idle-200` (p95 +40 %,
vsebuje tavanje), leteči boj cenejši od `boj-200` (p95 −43 %). Naslednji M4.14d (nov pristop).

### 2026-10-26 (151) — M4.16: harness scenarija letenja in dometa

**Paket:** M4.16 (D-081, Q66–Q68) · **Stanje:** **zaključeno**.
[Scenarij](../scenariji/M4.16-letenje.md), [zapis](../meritve/2026-10-26-M4.16-letenje.md). AI ni zaganjal testov/builda/Minecrafta.

Brez produkcijske kode. Ločen `letenje-run.ps1` (svež svet `-PrwWorld` na zagon, config
nespremenjen, SHA preverjen), `ponovitve-run.ps1 -Scenarij letenje`, `letenje-lib.ps1`,
krmilnik `letenje-control.js`, postavitev, fixture `LET_*` in samotest nad ponarejenim
svetom (node). Šest prog (zid, vogal, labirint, daleč leteči/kopenski, prosta kontrola) × tri
faze (en klic, osvežen, hitrost 0,5), merila veljavnosti LT1–LT9. Celica `leteci-200` je
M4.16b (Q66). Zahtevani ID-ji: `m416-samotest-v1`, nato `m416-letenje-v1` (3 ponovitve).
Na 163de09a… samotest zelen; `m416-letenje-v1` p1 padel na LT4 (postavitev): 1.12.2 `fill`
ne sprejme `hollow` brez podatkovne vrednosti (»'hollow' is not a state«), labirint ni bil
postavljen. Popravek ukaza v `minecraft:stone 0 hollow`; preverba LT4 nespremenjena.
Na 2882787d… `m416-samotest-v1`, `m416-letenje-v1` (LT1–LT9, T1–T6), unit, build in package
zeleni; vseh 180 veličin v treh ponovitvah razpon 0. Original z enim `navigateTo`: zid, vogal,
labirint in daleč 0/6 (daleč `navig0` 0/6 tudi kopenski; labirint brez premika), prosta
kontrola 6/6; osvežen zid 6/6 (215/270 tickov), vogal 0/6. Drsenje po prihodu 2,375/1,758
(max/mediana, spodnja meja; M4.0 ocena ~3,5 previsoka), hitrost 0,5 : 1,0 = 0,97 (ne velja).
Q68: pragovi izhodnega kriterija M4 isti. Naslednji M4.16b (`leteci-200`).

### 2026-10-26 (150) — M4.14d poskus 1 (TRACK) izmerjen in zavrnjen

**Paket:** M4.14d (D-080, Q63–Q65) · **Stanje:** **odprt, odložen; poskus 1 ni sprejet**.
[Dokazi](../meritve/2026-10-26-M4.14d-sledenje-poti.md). AI ni zaganjal testov/builda/Minecrafta.

Način TRACK (člana ob skupni poti vodi pot iz njenih točk, eno iskanje na obtičanje). Na
61bf7d8f… unit/build/package/navigator-harness-v3 zeleni; navigator-trace-v3 (8.960) in ab-v1
padeta samo na FA1/2 in FA4 (G 6,932 / 10,609, O 6,428), FA5 G formalno uspe (11 ≤ 11,
12 ≤ 15). A meja raste z obtičanji, ki jih TRACK povzroči: G 3–6 → 10–14, G DONE 233–246 →
318/348 tickov; obtičanja so ob zidu bočno od vrat (gneča na sredinski črti, hipoteza). Q65:
ni sprejet; koda v `884175d`, produkcija in testi povrnjeni na `cbad3c3` (= 984dc27a…),
števci nespremenjeni (422 @Test v 69 datotekah). Nov poskus mora ohraniti G DONE ≤ 246 in
obtičanja ≤ 6. Naslednji M4.16. Pragovi, scena in config isti.

### 2026-10-26 (149) — M4.14b zaključen kot preverba, ne izboljšava

**Paket:** M4.14b (D-079/Q62) · **Stanje:** **zaključeno; FA niso izpolnjeni, pragovi isti**.
[Dokazi](../meritve/2026-10-22-M4.14b-formacije.md). AI ni zaganjal testov/builda/Minecrafta.

Na zamrznjenem 984dc27a… ima vsak ID D-078 rezultat: harness-v1, recovery-harness-v2,
navigator-harness-v3, unit, build in package uspejo. ab-v1, trace-v1 (1.664),
recovery-trace-v2 (8.960, `iskanj_izgub` G 0) in navigator-trace-v3 (8.960) padejo samo na FA:
FA1/2 (merilo skupnega cilja proti 8 mestom; 8/8 mest OK), FA4 G 7,571–8,592 in O 6,42
(geometrija kolone skozi vrata, idealni premer LEGIJE 6,325) ter FA5 G 19 > 4 (člani v PATH
ne sledijo skupni poti → M4.14d). FA3/6 in FA5 O uspejo. F1–F12 ostanejo ročni test z
igralcem. Commit vključuje celoten paket M4.14b (koda, testi, harness, zapis).

### 2026-10-26 (147) — M4.14b: D-077 potrjen, koda zamrznjena, FA5 v M4.14d

**Paket:** M4.14b (D-078/Q61) · **Stanje:** **delno; preostali ID-ji na zamrznjenem odtisu**.
[Dokazi](../meritve/2026-10-22-M4.14b-formacije.md). AI ni zaganjal testov/builda/Minecrafta.

Na 984dc27a… uspejo unit (tudi `SquadAdapterTest`, `MountGuardTest`), build, package in
navigator-harness-v3. Navigator sled ima 8.960 zapisov brez `FORM-NAVERR`: izničenj je 17
(prej 28), vsa v tickih 7–26, nobene izgube obvoza G; `iskanj_izgub` G 7 → 0, `iskanj` 28 → 18.
FA5 G še pade (19 > 4), prav tako FA1/2 in FA4 G/O 8,041/6,417 >6.

Razčlenitev 18 zahtev: 3 obtičanja, 4 prva iskanja kolone, 2 opravičili, 4 premiki točke
kolone, 5 prehod v SETTLING. Pri 13 od 15 nedovoljenih je cilj na že poiskani skupni poti.
D-078: koda ostane na 984dc27a, na istem odtisu se zaženejo harness-v1, ab-v1, trace-v1,
recovery-harness-v2 in recovery-trace-v2. Sledenje skupni poti gre v nov paket M4.14d.
Pragovi isti, F1–F12 odprti.


### 2026-10-22 (140) — M4.14b: v1 popoln, zahtevana recovery prehoda

**Paket:** M4.14b (D-073/Q56) · **Stanje:** **delno; diagnostika še ni končana**.
[Dokazi](../meritve/2026-10-22-M4.14b-formacije.md). AI ni zaganjal testov/builda/Minecrafta.

Controller trace-v1 na c9edad1b… ima 1.664/1.664 zapisov, brez vrzeli; izhod 1.
FA1/2, FA4 G/O 9,564/6,428 >6 in FA5 G 29 >5 še padejo. G/O DONE 237/40,
8/8 končnih mest, FA3/6 in FA5 O uspejo. Pri 200 je G premer 9,563664 med
članoma 1/7 že v MARCHING; med 200 in 220 nastane še 14 zahtev. V1 nima
identitet in vmesnih tickov, zato ne dokazuje ohranitve istega obvoza ali avtorjev izgub.

Po D-073 zahtevana samo še recovery-harness-v2 in recovery-trace-v2 na isti kodi,
8.960 zapisov in vsi FA pogoji/pragovi isti. Produkcija in merilni vhodi nespremenjeni;
urejena samo dokumentacija in analiza že zbranih podatkov. F1–F12 odprti;
commit/push po zaključeni validaciji, nepovezane predhodne spremembe ohranjene.

### 2026-10-22 (139) — M4.14b: D-072 unit zelen, A/B še neuspešen

**Paket:** M4.14b (D-073/Q56) · **Stanje:** **delno; manjkajoči diagnostični prehodi**.
[Dokazi](../meritve/2026-10-22-M4.14b-formacije.md). AI ni zaganjal testov/builda/Minecrafta.

Na c9edad1b… harness/unit/build/package uspejo, vseh 16 adapterjevih testov zelenih.
A/B G/O DONE 237/40 in 8/8 končnih mest; G jih ohrani od vzorca 240 do 560
(napaka 0,399). FA1/2 skupni cilj 5/8 in 3/8, FA4 G/O 9,564/6,416 >6 in
FA5 G 29 >5 še padejo. G ima 28 članskih zahtev: 0 rok, 20 premik, 8 izguba;
8 v SETTLING in 6 nad STEER_MAX. FA3/6 in FA5 O uspejo, config hash isti.

Zahtevani samo manjkajoči trace-v1, recovery-harness-v2 in recovery-trace-v2 na isti
kodi pred novim posegom; zeleni testi in običajni A/B se ne ponavljajo. Zahtevani
pokritosti 1.664/8.960 in vsa FA merila ostanejo. Agregat ne dokazuje povzročitelja
izgub ali večjega razpona G. Urejena samo dokumentacija, produkcija in vsi merilni
vhodi/pogoji/pragovi isti. F1–F12 odprti; commit/push po zaključeni validaciji,
nepovezane predhodne spremembe ohranjene.

### 2026-10-22 (138) — M4.14b: potrjena reprodukcija, ohranitev obvoza v STEER

**Paket:** M4.14b (D-072/Q55) · **Stanje:** **delno; zahtevana sveža validacija**.
[Dokazi](../meritve/2026-10-22-M4.14b-formacije.md). AI ni zaganjal testov/builda/Minecrafta.

Controller unit na 59d50662… izvede 421 primerov: samo reprodukcija
changedSteerTargetDoesNotCutAcrossUnfinishedRecovery pade, ostalih 15 adapterjevih
testov uspe, tudi počiščena pot in izrecna zamenjava v PATH.

Ciljni popravek: activeRecovery preveri samo identiteto in nedokončanost poti.
STEER obvoz ohrani tudi ob novem cilju; PATH ga še lahko izrecno zamenja zaradi
premika cilja nad 3 bloke. Počiščenih in končanih poti ne obuja. Reprodukcije,
harness, geometrija, hitrosti in FA pogoji/pragovi isti. Za novo kodo še ni
rezultatov; zahtevanih vseh osem controller ID-jev. F1–F12 in M4.14b odprti;
commit/push po zaključeni validaciji, nepovezane predhodne spremembe ohranjene.

### 2026-10-22 (137) — M4.14b: recovery sled popolna, nova reprodukcija adapterja

**Paket:** M4.14b (D-071/Q54) · **Stanje:** **delno; zahtevan unit pred popravkom**.
[Dokazi](../meritve/2026-10-22-M4.14b-formacije.md). AI ni zaganjal testov/builda/Minecrafta.

Na 1f792335… recovery harness uspe, sled 8.960/8.960; FA1/2/4/5 G še padejo.
G/7 pri 166 zamenja nedokončan obvoz180/4/10 z ravno STEER potjo387/1 ob premiku
cilja prek zidu. Pri 201 spet stoji pred zidom, pri 202 zahteva nov obvoz.
G/4 pri 203 in G/7 pri 208 ločeno izgubita pot; vzrok teh izbrisov še nedokazan.

Dodani ciljni reprodukcija in kontrola za preklicano pot v SquadAdapterTest;
obstoječa kontrola še zahteva zamenjavo pri spremenjenem PATH cilju. Najprej unit
pred popravkom. Produkcija in FA pogoji/pragovi isti; F1–F12 odprti. Brez
commita/pusha nedokončanega paketa; nepovezane spremembe ohranjene.


### 2026-10-22 (136) — M4.14b: sled popolna, gosta diagnostika identitet poti

**Paket:** M4.14b (D-070/Q53) · **Stanje:** **delno; zahtevana nova recovery harness in trace ID-ja**.
[Dokazi](../meritve/2026-10-22-M4.14b-formacije.md). AI ni izvajal testov/builda/Minecrafta.

Controller sled na e225e880… ima 1.664/1.664 zapisov brez vrzeli, G/5 pri 180 HOLD
s pathUntil=0 → STEER pri 200. FA1/2/4/5 G še padejo: DONE 244/40, 8/8 mest;
G 30 >5, 19 premikov in 10 izgub, med 200–220 16 članskih zahtev.

Nov -TraceRecovery samo v diagnostičnem kraku FORMACIJA beleži vsak tick do 560:
8.960 zapisov, navId/recoveryId, napredovanje obvoza, zadnji iskalni tick/cilj in razloge.
Samotest preveri tudi identitete enake geometrije in okvarjene podatke. Nova ID-ja
m414b-recovery-harness-v2 in m414b-recovery-trace-v2 še brez rezultatov; prejšnji
odtis ne potrjuje spremenjenih testnih vhodov. Produkcija in FA pogoji/pragovi isti,
F1–F12 odprti; brez commita/pusha nedokončanega paketa, nepovezane spremembe ohranjene.

### 2026-10-22 (135) — M4.14b: unit D-068 zelen, manjkajoča sled iste kode

**Paket:** M4.14b (D-069/Q52) · **Stanje:** **delno; zahtevan samo m414b-trace-v1**.
[Dokazi](../meritve/2026-10-22-M4.14b-formacije.md). AI ni izvajal testov/builda/Minecrafta.

Na e225e880… harness/unit/build/package uspejo; reprodukcija D-067 in obe kontroli
po D-068 zeleni. A/B G/O DONE 244/40, 8/8 končnih mest, FA3/6 in FA5 O uspejo.
FA1/2 in FA4 6,540/6,416 >6 ter FA5 G 30 >5 še padejo. Od 29 članskih zahtev G
je 19 zaradi premika in 10 zaradi izgube; 8 v SETTLING, 6 nad STEER_MAX.

Zahtevan samo manjkajoči trace iste kode, 1.664 zapisov. Urejena samo dokumentacija;
produkcija, testi in vsi pogoji/pragovi isti. F1–F12 odprti, paket še brez commita/pusha
pred zaključeno validacijo; nepovezane predhodne datoteke ohranjene.


### 2026-10-22 (134) — M4.14b: reprodukcija potrjena, HOLD zaključi kazen PATH

**Paket:** M4.14b (D-068/Q51) · **Stanje:** **delno; zahtevani sveži controller testi**.
[Dokazi](../meritve/2026-10-22-M4.14b-formacije.md). AI ni izvajal testov/builda/Minecrafta.

Controller unit na cc58e6dc… izvede 419 primerov z natanko enim neuspehom:
pridobljeni HOLD ob premiku sidra vrne PATH namesto STEER. Vse trditve fixture in obe
kontroli uspeta. Produkcijska veja HOLD zdaj izniči samo `pathUntil`; `trailUntil`,
aktivni obvozi in PATH pri odmiku nad STEER_MAX ostanejo. Testi D-067 nespremenjeni.

Za novo kodo še ni rezultatov; zahtevani harness/unit/build/package, običajni A/B in
diagnostična sled. FA1/2/4/5 zadnjič neuspešni, F1–F12 odprti, pogoji/pragovi isti.
Paket ostane nedokončan in še brez commita/pusha. Nepovezane spremembe ohranjene.

### 2026-10-22 (133) — M4.14b: popolna sled in reprodukcija stare kazni po HOLD

**Paket:** M4.14b (D-067/Q50) · **Stanje:** **delno; zahtevan samo unit pred popravkom**.
[Dokazi](../meritve/2026-10-22-M4.14b-formacije.md). AI ni izvajal testov/builda/Minecrafta.

Controller sled na 44f3390b…: **1.664/1.664** zapisov brez vrzeli; zagon vrne 1 zaradi
FA1/2, FA4 6,477/6,403 in FA5 G 32 >6. G/O DONE 237/39, FA3/6, FA5 O in 8/8 mest uspejo.
Med 200–220 ostane 16 zahtev (9 premik, 7 izguba), 8 v SETTLING; redko vzorčenje ne
razloči vseh izvorov zamenjave poti. G/5 pri 180 že HOLD, a kazen PATH do 211; pri 200
PATH ob napaki približno 1,29. D-064 ohranja večtočkovni obvoz G/4 in G/7 tudi v STEER.

Dodane tri preverbe planerja: pridobljeni HOLD naj ob premiku sidra nadaljuje s STEER;
nedoseženi cilj in odriv nad STEER_MAX ohranita PATH. Fixture uporabi dejanski detektor
obtičanja brez zasebnih vpisov. Predviden en vsebinski neuspeh in dve kontroli, še
neizvedeno. Produkcija, harness in merilni pogoji nespremenjeni; novi testi zahtevajo
nov odtis. Najprej controller unit; nato ciljan popravek in sveža validacija. FA pragovi
isti, F1–F12 odprti. Brez commita/pusha nedokončanega paketa; nepovezano delo ohranjeno.


### 2026-10-22 (132) — M4.14b: izmerjeni razlogi zahtev, manjkajoča članska sled

**Paket:** M4.14b (nadaljevanje D-066/Q49) · **Stanje:** **delno; zahtevan samo m414b-trace-v1**.
[Dokazi](../meritve/2026-10-22-M4.14b-formacije.md). AI ni izvajal testov/builda/Minecrafta.

**Controller na 44f3390b…:** harness/unit/build/package zeleni. A/B
audit/m414b-2026-10-22-205327 neuspešen: G/O 8/8 mest, DONE 237/39, FA3/6 in FA5 O uspeta;
FA1/2, FA4 6,477/6,403 >6 in FA5 G **32 >6** padejo.

**Razčlenitev:** 31 članskih zahtev = rok 1 + premik 20 + opuščena pot 10; od tega 8 v
SETTLING in 6 z napako nad STEER_MAX. V oknu 200–220 16 zahtev (9 premik, 7 izguba).
Prvi trije razlogi so izključujoči; SETTLING in razdalja sta dodatna prereza.
Izguba pomeni drugo pot v navigatorju ob še nedokončani recovery, ne dokazanega izvora
zamenjave. Nadaljevanje Q49: pred posegom članski kontekst iz manjkajoče sledi iste kode.

**Naslednje:** samo m414b-trace-v1; posebej preveriti popolnost 1.664 zapisov, saj FA
preverbe ostanejo aktivne in lahko zagon vrne 1 tudi ob veljavni sledi. Že zelenih testov
in običajnega A/B brez novih vhodov ne ponavljamo. V tej operaciji samo dokumentacija;
produkcija, testi, harness in merilni pogoji isti. F1–F12 odprti. Brez commita/pusha pred
preostalimi preverjanji oziroma dokumentiranim zaključkom paketa; nepovezano delo ohranjeno.

### 2026-10-22 (131) — M4.14b: A/B po D-065, diagnostika razlogov iskanj

**Paket:** M4.14b (D-066, Q49) · **Stanje:** **delno; zahtevani sveži controller testi**.
[Dokazi](../meritve/2026-10-22-M4.14b-formacije.md). AI ni izvajal testov/builda/Minecrafta.

**Controller na `31c1fb2b…`:** harness/unit/build/package zeleni. Prvi A/B zavrnjen (vnos na
namizju), drugi `audit/m414b-2026-10-22-203728`: G/O 8/8 mest, FA3/FA6 uspeta; FA1/2, FA4
6,54/6,425 in FA5 G **32** (prej 36) >5 padejo. 17 zahtev G v oknu 200–220 (prehod v SETTLING).

**D-066:** brez spremembe obnašanja `RWSQUAD` doda razloge članskih iskanj (`iskanj_rok`,
`iskanj_premik`, `iskanj_izgub`, `iskanj_postav`, `iskanj_dalec`), da se hipoteza (premik
cilja člana v kazni + skok ciljev ob SETTLING) izmeri, preden se poseže v adapter.

**Nadaljevanje iteracija 11:** pregledan diff in controller log zadnjega veljavnega A/B;
produkcija, testi in merilni vhodi nespremenjeni. Na `44f3390b…` ni rezultatov. Zahtevan
tudi `m414b-trace-v1`, da poleg skupnih razlogov dobimo posamezne poti in cilje članov.

**Naslednje:** unit/build/package/harness/A/B/sled; po razčlenitvi ciljni popravek ali zapis FA5
G kot neuspeha z vzrokom. F1–F12 odprti; commit/push po sprejemu ali zapisanem zaključku.

### 2026-10-22 (130) — M4.14b: A/B po D-064, ponovljena iskanja po končani poti

**Paket:** M4.14b (D-065, Q48) · **Stanje:** **delno; zahtevani sveži controller testi**.
[Dokazi](../meritve/2026-10-22-M4.14b-formacije.md). AI ni izvajal testov/builda/Minecrafta.

**Controller na `637ad98f…`:** harness/unit/build/package zeleni (obe reprodukciji D-063 zeleni).
A/B `audit/m414b-2026-10-22-200920`: G/O stabilnih 8/8 mest, DONE 246/38, FA3/FA6 uspeta;
padejo FA1/2 (skupni cilj), FA4 6,458/6,403 >6 in FA5 G **36** (prej 57) proti meji 5.
21 zahtev G v oknu 200–220 (prehod v SETTLING, 4 upravičeni člani).

**Popravek D-065:** v PATH `noPath()` velja tudi za prehojeno pot; član v kazni je zato isto
točko zahteval vsakih 5 tickov. Po uspešno prehojeni poti (konec ≤1,5 od cilja, cilj ±3,
član ≤6) zdaj STEER brez iskanja; delna/neuspešna pot se ponovi po 20 tickih. +2 testa adapterja.

**Naslednje:** harness/unit/build/package/A/B/sled. FA1/2 in FA4 ostajata neuspeha zaradi
geometrije (pragovi isti); F1–F12 odprti; commit/push po sprejemu ali zapisanem zaključku.

---

### 2026-10-22 (129) — M4.14b: reprodukciji potrjeni, popravek zadržanja obvoza

**Paket:** M4.14b (D-064, Q47) · **Stanje:** **delno; zahtevani sveži controller testi**.
[Dokazi](../meritve/2026-10-22-M4.14b-formacije.md). Delni diff pregledan, nepovezane datoteke
ohranjene. AI ni izvajal testov/builda/Minecrafta.

**Controller unit na `63f20805…`:** 414 testov, natanko pričakovana 2 neuspeha
(`activeRecoveryDoesNotRepeatRequestJustBecauseIntervalElapsed`,
`steerNearTargetKeepsUnfinishedRecoveryDetour`); štiri kontrole zelene, fixture pravilna.

**Popravek:** `Squad` hrani pot zadnjega iskanja člana; dokler je v navigatorju, ni končana
in cilj ni premaknjen >3 bloke, PATH ne zahteva periodično, STEER pa je ne zamenja.
Ostali sprožilci (`noPath`, premik >3, cooldown) nespremenjeni.

**Naslednje:** harness/unit/build/package/A/B/sled na novi kodi; ob zelenem A/B dimni del
F1–F12. Commit/push čaka na sprejem; paket ni zaključen.

### 2026-10-22 (128) — M4.14b: sled ponovnih zahtev in prekinitve obvoza

**Paket:** M4.14b (D-063, Q46) · **Stanje:** **delno; zahtevan controller `unit` pred popravkom**.
[Članski dokazi in pogodbe](meritve/2026-10-22-M4.14b-formacije.md). Delni diff pregledan,
nepovezane datoteke ohranjene, ob pregledu ni živih Java procesov. AI ni izvajal
testov/builda/Minecrafta.

**Controller sled na `9c3650a7…`:** ob 14:42:50 UTC izhod 1, pokritost 1.664/1.664
brez vrzeli. G/O ohranita 8/8 mest in DONE pri 241/38, G FA5 55 proti meji 5. FA1/2,
FA4 še padeta; to ni običajni sprejemni A/B. G/7 pri ticku 51 znova zahteva pot ob istem
cilju in še aktivni poti, G/2 med 120 in 140 preklopi PATH → STEER ter izgubi večtočkovni
obvoz ob istem cilju. Iz sledi ne določamo točnega ticka drugega preklopa ali števila A*.

**Pripravljeno:** šest dodatnih testov dejanskega adapterja: dve reprodukciji za aktivno
pot brez periodične zahteve in ohranitev obvoza po preklopu STEER; štiri kontrole za
zaključeno/izbrisano pot, spremenjen cilj in neuspešno zahtevo. Še neizvedeno; pričakovani
dve vsebinski napaki. Produkcija, harness, nav vhodi, config in pragovi nespremenjeni.

**Naslednje:** controller `unit`; najprej izločiti napako fixture, nato po potrjenih
reprodukcijah ciljan popravek življenjske dobe poti in sveži vsi zahtevani testi.
F1–F12 ostanejo odprti. Commit/push čaka na sprejem; paket ni zaključen.

### 2026-10-22 (127) — M4.14b: stabilen konec G/O, preostali FA5

**Paket:** M4.14b (D-062, Q45) · **Stanje:** **delno; zahtevana manjkajoča sled iste kode**.
[Dokazi in ohranjene vrednosti](../meritve/2026-10-22-M4.14b-formacije.md). Delne spremembe
pregledane in ohranjene, ob pregledu ni živih Java procesov. AI ni zaganjal testov,
builda ali Minecrafta; v tej seji spreminja samo dokumentacijo.

**Controller na `9c3650a7…`:** harness/unit/build/package uspejo; šest testov adapterja
in deset planerja zelenih. Običajni A/B še pade. G DONE pri 234, vseh 8 mest pri vzorcu
240 in še pri 560 (napaka 0,384); O DONE pri 40, 8/8 mest do 560 (napaka 0,240).
Popravek D-061 je torej preverjen, celotni paket ni sprejet: FA1/2 skupni cilj 5/8 in 3/8,
FA4 premer 6,540/6,428 >6, FA5 G 57 zahtev proti meji 5. FA3 in FA6 uspeta.

**Odločitev:** pred naslednjim posegom manjkajoči `m414b-trace-v1` na isti kodi za
članski kontekst ponovnih zahtev PATH in preklopov STEER/PATH. Števec zahtev sam ne dokazuje
števila izvedb A*; ne spreminjamo ga za zelen izid. Prav tako ohranimo vsa neuspešna merila
in pogoje. Zelene teste ponovimo šele ob novih vhodih; stare sledi ne pripisujemo novi kodi.

**Naslednje:** controller sled, nato iz dokazov reprodukcije pred ciljnim popravkom.
F1–F12 še zahtevajo dimni del. Paket ni zaključen ali commit/pushan pred sprejemom;
nepovezane predhodne datoteke ohranjene.

### 2026-10-22 (126) — M4.14b: reprodukcije adapterja potrjene, ciljan popravek

**Paket:** M4.14b (D-061, Q44) · **Stanje:** **delno; zahtevani sveži controller testi**.
[Dokazi in obseg](../meritve/2026-10-22-M4.14b-formacije.md). Delne spremembe pregledane in
nadaljevane; ob pregledu ni živih Java procesov. AI ni izvajal testov/builda/Minecrafta.

**Controller na `fba333de…`:** `unit` ob 14:23:36 UTC izvede 408 primerov s štirimi
neuspehi. Vse štiri nove reprodukcije dosežejo vsebinsko trditev; obe kontroli uspeta.
Ni napake fixture. Dokazana cilja predteka sta 71/72 namesto 69/70, preostalo gibanje
motionX 0,012 pri HOLD in motionZ 0,146174 pri finish.

**Spremenjeno:** HOLD in upravičeni člani ob finish ustavijo pot/helper, vhod in vodoravno
vztrajnost brez teleportacije ali spremembe motionY/gravitacije. Predtek ostane pri
premikajočem sidru za običajno mesto; kolona in mirujoče sidro brez predteka. Šest testov
ostane istih, prav tako geometrija, merilni config, hitrosti in prvotni FA pragovi.

**Naslednje:** sveži harness/unit/build/package, običajni A/B in ločena diagnostična sled.
Popravek še ni preverjen; ne dokazuje odprave vseh zastojev G ali FA5. F1–F12 ostajajo
odprti, potreben dimni del. Paket ni zaključen ali commit/pushan pred sprejemom;
nepovezane predhodne datoteke ohranjene.

### 2026-10-22 (125) — M4.14b: popolna sled, vztrajnost O in cilji G

**Paket:** M4.14b (D-060, Q43) · **Stanje:** **delno; zahtevan controller `unit` pred popravkom**.
[Analiza in ključne vrednosti sledi](../meritve/2026-10-22-M4.14b-formacije.md). Delne spremembe
pregledane in ohranjene; ni živih Java procesov. AI ni izvajal testov/builda/Minecrafta.

**Controller na `c6d7fe1c…`:** razširjeni harness uspe; diagnostični `m414b-trace-v1` pade
na ohranjenih FA merilih, pokritost pa uspe (1.664/1.664, brez napak). O/0 pri ticku 38
že nima poti/vhoda in ima helper WAIT, vendar motionZ 0,146174. Po DONE se po vztrajnosti
premakne še 0,317311 bloka na napako 0,545629; brez ponovnega ukaza za hojo. G adapter
doda predtek tudi koloni in mirujočemu sidru, kar da cilj proč od dodeljenega mesta.
To samo še ne dokazuje vseh vzrokov vratnega zastoja ali številnih iskanj.

**Pripravljeno:** šest `SquadAdapterTest` primerov nad dejanskima `drive`/`finish` in cilji
navigatorja; pričakujemo štiri vsebinske neuspehe pred popravkom (še ne izmerjeno).
Fixture sledi `MountGuardTest`; ne zaganja sveta. Produkcija, geometrija, config,
merilni vhodi, hitrosti in prvotni FA pragovi ostanejo isti. Števci: 407 metod v 69 datotekah.

**Naslednje:** controller `unit`; najprej ločiti napako fixture od vsebinske reprodukcije.
Po potrditvi omejen popravek zaustavitve/predteka, nato sveži harness/unit/build/package,
običajni A/B in dimni F1–F12. Paket ni zaključen ali commit/pushan; nepovezano delo ohranjeno.

### 2026-10-22 (124) — M4.14b: popravek faz zelen, ločena sled adapterja

**Paket:** M4.14b (D-059, Q42) · **Stanje:** **delno; zahtevana samotest in diagnostična sled**.
[Dokazi in naslednji koraki](../meritve/2026-10-22-M4.14b-formacije.md). Pregledane in ohranjene
delne spremembe; ob pregledu ni živih Java procesov. AI ni zagnal testov/builda/Minecrafta.

**Controller na `a99e030c…`:** harness/unit/build/package uspejo; obe reprodukciji D-058
PASSED, `SquadPlannerTest` 10/10. A/B ob 13:51 UTC ostaja neuspešen: G MARCHING 7,2 bloka
pred koncem in 116 iskanj proti meji 35, O DONE pri ticku 38, vendar 0/8 mest pri 40–560,
napaka 0,507 → 0,555 bloka. FA1/2, FA3, FA4, mesta in G FA5/6 še padajo.

**Nov pristop (Q42):** samo ločeni `-Trace` overlay za FORMACIJA, ki ohrani reference po
DONE ter bere položaje/motion, vhod/helper, cilje/zastavice in pot navigatorja. 1.664 zapisov,
preverba pokritosti/starosti/identitet, prefix `m414b-trace-*`. Vsi prvotni FA testi ostanejo
aktivni; zaradi dodatnega zapisovanja to ni sprejemni/performance dokaz. Samotest razširjen
z dejanskim minificiranim overlayjem in negativnimi pogodbami; še ni izveden. Produkcijska
koda, običajni nav vhodi, pragovi in config v tej seji nespremenjeni.

**Naslednji korak:** controller `m414b-harness-v1`, nato novi `m414b-trace-v1`; razčleniti
prvi zastoj G in fiziko O pri DONE, dokazani vzrok reproducirati pred popravkom. Nato sveži
običajni harness/unit/build/package/A/B ter F1–F12. Paket še ni zaključen ali commit/pushan;
sprejem je neuspešen. Predhodne nepovezane datoteke ohranjene.

### 2026-10-22 (123) — M4.14b: potrjeni reprodukciji, ciljni popravek faz

**Paket:** M4.14b (D-058, Q41) · **Stanje:** **delno, zahtevani sveži controller testi**.
[Dokazi](../meritve/2026-10-22-M4.14b-formacije.md). Predhodne spremembe pregledane in ohranjene;
ob pregledu ni živih Java procesov. AI ni zaganjal testov, builda ali Minecrafta.

**Reprodukcija:** `unit` na `dcbd1219…` neuspešen ob 13:41 UTC: 402 izvedbi, padeta natanko
`settlingRequiresNewArrivalWhenTheFinalSlotChanges` (prezgodnji DONE) in
`completionPublishesOutwardFacingForEveryRingMember` (manjka končna smer). Ostalih 400 uspe.

**Sprememba:** samo `SquadPlanner`: ob novih končnih mestih reset `settled`, pri prehodu v
DONE tudi končna smer (krog navzven), HOLD in hitrost 0. Reprodukciji nespremenjeni. Brez
spreminjanja adapterja, pragov, geometrije, hitrosti, configa in merilnih vhodov. Q41 omeji
sklep na ti dve napaki; G in odmik O po koncu še nimata potrjenega vzroka.

**Naslednji korak:** `m414b-harness-v1`, `unit`, `build`, `package`, `m414b-ab-v1` na novi kodi;
nato razčleniti preostale neuspehe in pripraviti dimni del F1–F12. M4.14b ni zaključen ali
commit/pushan, saj veljavna preverba popravka še manjka. Nepovezane datoteke ohranjene.

### 2026-10-22 (122) — M4.14b: izmerjeni neuspeh in ciljni reprodukciji

**Paket:** M4.14b (D-056/D-057, Q40) · **Stanje:** **delno, zahtevan controller `unit` pred popravkom**.
[Meritev in načrt nadaljevanja](../meritve/2026-10-22-M4.14b-formacije.md). Delne spremembe so pregledane
in ohranjene. Nobenega testa, builda ali Minecrafta AI ni zagnal; config in merilni vhodi nespremenjeni.

**Dokazi na `360f3f60…`:** harness/unit/build/package uspejo, A/B neuspešen. Referenca G 6/8,
O 8/8 (zadnji 220 tickov); formacija G 0/8 in ostane v MARCHING z 7,2 bloka poti ter 117 iskanji
proti meji 36. O doseže 8/8 mest pri ticku 40, od ticka 60 ima napako 0,555 bloka in 0/8 mest
znotraj 0,4. FA1/2 in FA4 prav tako neuspešni (premer G 6,566 / O 6,338 proti meji 6).
Scenarijske predpostavke, kontrolna faza A in nespremenjen hash configa potrjeni.

**Narejeno:** dve novi reprodukciji v `SquadPlannerTest` za podedovane zastavice `settled` pri
prehodu na končna mesta in manjkajočo smer kroga pri neposrednem zaključku. Testa sta pripravljena,
ne izvedena; produkcijska koda ostane ista do controller reprodukcije. 401 metod v 68 datotekah.
Ti robni primeri še ne pojasnijo vseh zastojev pri vratih in preostalega gibanja po zaključku.

**Q40:** ohranimo vse pragove/geometrijo, najprej `unit` (predvideni rdeči reprodukciji), nato
ciljan popravek ter sveži harness/unit/build/package/A/B. F1–F12 še potrebujejo dimni harness.
Paket ni zaključen ali commit/pushan; nepovezani `audit/*`, baseline 27. 9. in bat ohranjeni.

### 2026-10-22 (121) — M4.14b: popravek branja živega loga

**Paket:** M4.14b (nadaljevanje, D-056, Q39) · **Stanje:** **delno, zahtevan svež controller prehod**.
[Dokazi](../meritve/2026-10-22-M4.14b-formacije.md). Predhodne spremembe ohranjene; fetch potrdi
poravnanost z origin pri `ff0ccd9`. Ob pregledu ni živih Java procesov.

**Rezultat starega odtisa:** na `7faf3e03…` so harness/unit/build/package zeleni, A/B pa se je ustavil
pred meritvijo: `ReadAllText` ne more odpreti stdout loga z odprtim zapisovalnikom. Ustavitev strežnika
in hash configa sta uspešna. FA1–FA6 zato nimajo rezultata; iz napake ni dovoljeno sklepati o formacijah.

**Popravljeno:** samo `Get-Log` v `formacije-run.ps1` uporablja `FileShare.ReadWrite` in zapre bralnik;
samotest dejansko bere ob odprtem zapisovalniku, po dodanem markerju in ob manjkajoči datoteki.
Q39 je razrešen brez spreminjanja scenarija, pragov, sveta ali produkcijske kode. Novi samotest še ni izveden.
AI ni zaganjal testov, builda ali Minecrafta. Števci Java/JUnit nespremenjeni.

**Naslednji korak:** controller zaporedno `m414b-harness-v1`, `unit`, `build`, `package`, `m414b-ab-v1`.
Rezultati prejšnjega odtisa niso dokaz spremenjenega harnessa. Nato razlaga FA1–FA6 in dimni del F1–F12;
šele po dokazih zaključek in commit/push tega paketa. Nepovezane datoteke ostanejo izven commita.

### 2026-10-22 (120) — M4.14b: priprava preverbe formacij

**Paket:** M4.14b (D-038, D-056, Q37/Q38) · **Stanje:** **delno, čaka controller teste**.
[Zapis priprave](../meritve/2026-10-22-M4.14b-formacije.md). Raziskava M4.0 že opravljena; produkcijska
koda formacij, originalni jar, config in environment lock nespremenjeni. Pred začetkom ni bilo sledenih
nedokončanih sprememb, veja je bila po fetchu poravnana z origin (`ff0ccd9`).

**Narejeno:** način `FORMACIJA` v izvorni `nav-control.js` (samo B, dva enkratna ukaza); novi
`formacije-run.ps1` za A/B nad kopijama istega svežega sveta z obojestranskimi privzetimi stikali kode.
Ohrani stare metrike, izmeri tudi zasedenost unikatnih mest in bere `RWSQUAD konec` iz ločenega CustomNPCs
loga. Novi samotest preveri dejanske bralnike logov, SNBT vgradnjo in minificirano skripto v obeh načinih.

**Ugotovitve [K]:** skupni cilj M2.7 (<2 bloka) ne zajame zunanjih stolpcev; idealni premer 4×2 je
6,325 > prag FA4 6. Prvotni pragovi ostanejo, rezultatov še ni. Status ne šteje začetnega iskanja,
zato ga FA5 izrecno prišteje. F1–F12 ostanejo odprti; A/B sam ne zaključi paketa.

**Verifikacija:** samo statični pregled in razčlenitev PowerShella; AI ni zaganjal testov, builda ali
Minecrafta. Zahtevani controller testi: `unit`, `build`, `package`, nova `m414b-harness-v1` in
`m414b-ab-v1`. Nobenega novega controller rezultata ali trditve o izboljšavi. Števci Java/JUnit nespremenjeni.

**Naslednji korak:** pregledati A/B rezultate in vsako neuspešno merilo; dokončati dimni del F1–F12.
Šele po ustreznih dokazih zaključek M4.14b in commit/push lastnih datotek. Nepovezani `audit/*`,
`ZAZENI-BASELINE.bat` ter baseline zapis 27. 9. ostanejo izven commita. Nato M4.16 po D-038.

### 2026-10-22 (119) — M5.Z: regresijski prehod, M5 zaključen

**Paket:** M5.Z (D-054, D-055, Q35/Q36) · **Stanje:** **zaključeno** — zahtevanih šest testov
(`unit`, `build`, `package`, `m5z-sled`, `m5z-matrika-orig`, `m5z-matrika-priv`) zelenih pri upravljalniku na
`c3e19fe4607c364e981c698b4c26efa07dcf7e19435ababa0b13c8dd2cb84c99`. Po testih spremenjeni samo dokumenti.
[Zapis](../meritve/2026-10-22-M5.Z-regresija.md), [scenarij sledi](../scenariji/M5.Z-sled.md).

**Narejeno:** `ConfigDefaults` zajame vrednosti `@ConfigProp Rw*` pred nalaganjem configa in jih pri
`rwprivzeto=1` obnovi (0 samo izpiše, brez lastnosti običajen config); Gradle prenaša `rwPrivzeto`.
`matrika-run.ps1 -Privzeto 0/1` preveri način v vseh svežih logih in jih arhivira po kraku; novi `sled-run.ps1`
primerja sled dogodkov ter timerjev pred/po restartu na dveh svežih kopijah istega sveta. Config in lock nespremenjena.
Dodani 1 razred in 4 testi; števec 182 razredov (117 rework), 399 metod `@Test` v 68 datotekah (XML 400 izvedb),
40 primerjalnih testov originala; package ostane 71 vnosov.

**Dokazi:** obe matriki po 5/5 izvedenih stopenj zelenih (build preverjen ločeno). Sledi faz 1/2 imata 97/75 dogodkov,
enaki znak za znakom; timer 7 ima v obeh krakih 19 razmikov po 8 tickov (znani B3 ohranjen). Krak 1 ima 60
preskokov collide poizvedbe soseda brez skripte, krak 0 nobenega. R1 ohrani reprodukcijo napake pri načinu 0;
R2 L1–L12 zeleni, fazi A/B enaki veljavni referenci 21. 9.; brez regresije v izvedenih preverjanjih.

**Odločitvi:** Q35 — ne prepisuj merilnega configa, uporabi začasno JVM preglasitev in dokaz v logih.
Q36 — M2.7 je navigacijska sonda, zgodovinske timer sledi ni: popravljena napačna povezava v planu,
referenca nove sledi je isti popravljeni jar s stikali iz configa (0), ne distribucijski original. B3 ni del popravka.
Prvi sled zagon je padel v obeh krakih: starost po restartu 252 je presegla mejo 200; popravljena faza meri od svojega
init, merila ostanejo ista, ponovitev zelena na novem odtisu. D-055 zaključi M5.Z in M5 v načrtovanem obsegu.

**Spremembe obnašanja:** samo izrecno zahtevana diagnostična lastnost za scenarije; brez nje običajne vrednosti
configa. **Ni potrjeno:** nepokrite vrstice M0.7/M10, player/block/item dogodki, Q1/Q22 ter nameščeni OptiFine/Entity
Culling; klientski sprejem je ločeno dokazan v M5-K.2/V.8. Brez novih performance meritev.

**Naslednji korak:** **M4.14b** (formacije F1–F12 in FA1–FA6), nato **M4.16** (scenarij letenja), po že opravljeni
raziskavi M4.0 in D-038. Upravljalnik zaganja vse teste in Minecraft. Q22 ostaja ločena preverba pred izdajo.

### 2026-10-22 (118) — M5-K.2: sprejem klienta — zaključeno

**Paket:** M5-K.2 (izhodni kriterij M5 za klient, D-054, Q34) · **Stanje:** **zaključeno** — vseh 8 zahtevanih serij
(`k2-{200,500}-{orig,priv}`, `k2-dalec-{200,500}-{orig,priv}`, 3 ponovitve) zelenih na kodi `332af8c4…`: T1–T6 v vseh
serijah, K1–K16 v vseh 24 zagonih ([zapis](../meritve/2026-10-22-M5-K.2-sprejem-klienta.md)).

**Narejeno:** `k-run.ps1` — K12 pri `-Napisi 1` in idle pričakuje napise iz geometrije (`renderName` do 22,6 bloka,
pas ±1 blok), K16 pri `-Far 2` brez `-Dalec 1` zahteva natanko 0 L2; razdelek M5-K.2 v scenariju M5-K.0. Obe strani
nastavita vsa štiri klientska stikala eksplicitno prek posla, `dev/run/config/CustomNpcs.cfg` ostane 0.

**Izid (mediane, original → privzeto, vsi razponi ločeni):** p95 idle-200 7,095 → 5,356 ms, idle-500 12,825 → 9,817,
dalec-200 5,405 → 1,851, dalec-500 13,017 → 2,942; 1 % low 102,5 → 139,8 / 54,4 → 67,0 / 134,9 → 364,6 /
50,2 → 259,6 FPS; p50 −24,7 do −82,5 %, p99 −19,6 do −80,6 %. Regresije na privzeti strani ni (strežnik enak, GC v
dalec-500 +6 % absolutno pri 5,7× več sličicah). Original se ujema z baselineom M5-K.1 in M5-V.6.

**Odločitve:** D-054 — klientski del izhodnega kriterija M5 izpolnjen; K10 ostane odložen (idle-500: 500 modelov
znotraj ~42 blokov, p95 9,817 ms < 16,7). Q34 (samostojno): M5 še ni zaprt — funkcionalna matrika §6 in timerji/script
trace od 17. 9. niso bili ponovljeni na kodi s privzetimi stikali → nov paket **M5.Z**.

**Spremembe obnašanja:** nobene (samo harness). **Minecraft meritve:** 24 zagonov klienta.

**Naslednji korak:** **M5.Z** — regresijski prehod `matrika-run.ps1` (build, package, testworld, rwdiag, r1, r2) in
primerjava M2.7 (timerji, script trace) na trenutni kodi; registrirati test ID-je. Nato M4 (M4.14b → M4.16 → …, D-038).
Neodvisno: Q22; ročna preverba z OptiFine in Entity Culling (odvisna od uporabnika).

### 2026-10-22 (117) — M5-K K3c: čistilni korak K3 — zaključeno

**Paket:** M5-K K3c (D-053) · **Stanje:** **zaključeno** — zahtevani `unit`, `build`, `package` zeleni na kodi
`241416ca…`. Brez novih Minecraft meritev (brez spremembe obnašanja).

**Narejeno:** odstranjena koda zavrženega poskusa K3 (`RwEyesList` je bil privzeto 0): `rework/render/EyesList`,
`rework/diag/EyesCheck`, `EyesListTest` (4 testi) in izločitev v `build.gradle`, `CustomNpcs.RwEyesList` s ključem
configa, veja v `LayerEyes` (spet 1:1 z 7881e50 + K5 `Lod.far`), `eyesList`/`eyesCheck` v `HarnessJob` in
`ClientHarness` (polja JSON `rwEyesList`, `oci*`), `k-run.ps1 -Oci`/`-PreveriOci` in merili K13/K14. Popravljeno
besedilo komentarja `RwRenderBox` in javadoc `RenderBox`: 64 namesto 38,4 bloka pri RD 8 (Size 5), 1,0 / 80 blokov pri RD 10.
`verify-package` ostane 71 vnosov (`LayerEyes` je še popravljen zaradi K5).

**Posledica:** zgodovinski test ID-ji `k3-200/500-off/on` in `audit/m5k3-ab.ps1` ne delujejo več (parameter `-Oci`);
ponovitev K3 je mogoča samo na commitu 49bd247. Ostanek ključa `RwEyesList` v obstoječih configih Forge ignorira.

**Spremembe obnašanja:** nobene. **Minecraft meritve:** nobene.

**Naslednji korak:** **M5-K.2** — sprejem klienta (izhodni kriterij M5): A/B 3 + 3 idle-200/500 (prizorišče M5-K.1) in
`-Dalec 1`, original proti vsem sprejetim privzetim stikalom, p50/p95/p99 in 1 % low; harness potrebuje način s
privzetimi vrednostmi kode brez spreminjanja `dev/run/config/CustomNpcs.cfg`. Neodvisno: M4.14b/M4.16; Q22; ročna
preverba z OptiFine in Entity Culling.

---

### 2026-10-22 (116) — M5-K: ponovna ocena K13 in K10 — zaključeno

**Paket:** ponovna ocena K13/K10 po D-035 (3) (D-053) · **Stanje:** **zaključeno** — raziskava brez produkcijske kode in
brez novih zagonov; vhodi so obstoječe meritve ([raziskava](../raziskave/M5-K-K13-K10-ponovna-ocena.md)).

**Ugotovitve:** K14 in K3 sta izmerila, da manj klicev pri isti geometriji ne pomaga (strošek so oglišča v gonilniku),
zato je premisa K10 »nič klicev na NPC« delno ovržena; po D-051 so modeli samo znotraj 56 blokov, L2 pa je že izmerjena
zgornja meja »instanciranega« izrisa (idle-500 −80 %). Vezava teksture (K13) v profilu M5-K.1 ni merljiva.

**Odločitve (D-053):** K13 zavržen kot samostojen paket; K10 odložen izven M5 (ponovno odprtje: ≥ 300 modelov znotraj
56 blokov s p95 > 16,7 ms ob privzetih stikalih, tedaj najprej poskus K10a z VBO); K2 in K11 zavržena; K1, K6–K9 odloženi
do prizorišča, ki jih vsebuje; cena izrisa L3 zavržena kot paket; pas `rwfar` po chunkih in PBO peka odložena s pogojem.

**Spremembe obnašanja:** nobene. **Minecraft meritve:** nobene.

**Naslednji korak:** **K3c** — odstranitev kode poskusa `RwEyesList` in popravek besedila `RwRenderBox`/`RenderBox`
(64/38,4 blokov pri RD 8, 1,0/80 pri RD 10), dokaz `unit`/`build`/`package`; nato **M5-K.2** (sprejem klienta,
izhodni kriterij M5: original proti vsem privzetim stikalom, idle-200/500 in `-Dalec 1`, p95 in 1 % low).
Neodvisno: M4.14b/M4.16; ročna preverba z OptiFine in Entity Culling; Q22.

### 2026-10-22 (115) — M5-K K12: okvir upodobitve in Entity Culling — zaključeno

**Paket:** M5-K K12 (D-052, Q33) · **Stanje:** **zaključeno** — vseh 7 zahtevanih testov zelenih na kodi `a435cbb9…`:
`unit`, `build`, `package`, `k12-okvir-on`, `k12-okvir-off` (K1–K8, K16, K17a–j, K18a–e) in A/B `k12-500-off`/`-on`
(3 + 3, T1–T6). Prvi `k12-okvir-on` na `0db45ac6…` je padel na K18d (ubit `RWV7_Mrtev` z dometom ležečega hitboxa
53,3 bloka, kamera ~58,5) → ležeč/ubit NPC dobi domet stoječega.

**Raziskava** ([zapis](meritve/2026-10-22-M5-K-K12-okvir.md) §1): CNPC ne prepiše `getRenderBoundingBox` ne
`isInRangeToRenderDist`; Entity Culling 6.5.0 / RenderLib 1.4.5 okluzijo računa na predpomnjenem
`getRenderBoundingBox` [K, jar]. Lastna okluzija zavržena.

**Narejeno:** `rework/render/RenderBox` (okvir = hitbox ∪ model; domet brez hitboxa in ležečih), prepisa v
`EntityNPCInterface` (klient), `CustomNpcs.RwRenderBox` (privzeto **1**); harness `HarnessJob.renderBox`,
`RenderBoxAudit` (okvir, domet, pečen obris L2 proti okvirju), `FarRender.bakedLook`, `k-run.ps1 -RenderBox` +
`RWV7_Mec` + K18; JUnit +12 (`RenderBoxTest` 8, `RenderBoxAuditTest` 3, `HarnessJobTest` +1).

**Izid:** z okvirjem izven dometa 0, obris čez okvir 0 (največ 0,091 bloka); brez okvirja izven dometa 2, obris čez
hitbox do 0,747 bloka (meč). idle-500 p50 11,492 → 11,483, p95 12,849 → 12,907 ms (prekrivanje) → cena v šumu.

**Spremembe obnašanja moda:** z novim configom (ali brez ključa) se NPC na robu zaslona/za oviro (Entity Culling)
nariše, kjer ga je original odrezal; NPC brez hitboxa in ležeč NPC imata domet stoječega (Size 5, RD 8: 64 namesto
38,4 / 42,7 bloka). `dev/run/config/CustomNpcs.cfg` ima `RwRenderBox=0` (merilni pogoji nespremenjeni).
**Minecraft meritve:** 2 + 1 (padel) zagona `k12-okvir`, 6 zagonov A/B. Entity Culling in OptiFine v svetu nista izmerjena.

**Naslednji korak:** ostali K-paketi — **K13 in K10 ponovno oceniti** (D-035; kandidati iz M5-V.8: cena izrisa L3,
pas `rwfar` po chunkih, PBO peka). Ob naslednji spremembi kode popraviti besedilo komentarja `RwRenderBox` in javadoc
`RenderBox` (64/38,4 blokov pri RD 8, ne 66/40). Neodvisno: čistilni korak K3, M4.14b/M4.16; ročna preverba z
OptiFine in Entity Culling.

### 2026-10-22 (114) — M5-V.8b: `RwFarView` privzeto 2 — zaključeno

**Paket:** M5-V.8b (D-051) · **Stanje:** **zaključeno** — zahtevani `unit`, `build`, `package` zeleni na kodi
`5c4754b8…` (22. 10. 2026; brez novih meritev v svetu; sprejem je M5-V.8 na kodi `9f9125b0…`).

**Narejeno:** `CustomNpcs.RwFarView` privzeto **2** (prej 0) in komentar konfiguracije z izidom sprejema (vrednost 1 na
klientu ni priporočena); komentar `FarServer`; vrstice obnašanja `RwFarView`, `RwFarRange`, `RwImpostorStart`,
`RwImpostorAtlas`, `RwFarShaders` in coremoda `RenderGlobal` v 04-REGISTRI kažejo privzeto 2; README, 03-FAZE.

**Spremembe obnašanja moda:** z novim configom (ali brez ključa) strežnik pošilja nadomestke po kanalu `rwfar`
klientom s `HELLO`, klient riše L2 (sledeni onkraj 56 blokov) in L3 (onkraj sledilnika do `RwFarRange`/vd). Obstoječi
configi s ključem obdržijo vrednost; `dev/run/config/CustomNpcs.cfg` ima `RwFarView=0` (merilni pogoji nespremenjeni),
`dev/run-m7` ključa nima in dobi 2. Stari klient ali klient z 0 ne pošlje `HELLO` in ne dobi nič.
**Minecraft meritve:** nobene.

**Naslednji korak:** **K12**, nato ostali K-paketi (K13, K10 ponovno oceniti; kandidati iz M5-V.8: cena izrisa L3,
pas `rwfar` po chunkih, PBO peka). Neodvisno: čistilni korak K3, M4.14b/M4.16; ročna preverba z OptiFine.

### 2026-10-22 (113) — M5-V.8: sprejem vidnosti na daljavo — zaključeno (vklop v kodi M5-V.8b)

**Paket:** M5-V.8 · **Stanje:** **zaključeno** — vseh 15 zahtevanih testov zelenih na kodi `9f9125b0…`: `unit`, `build`,
`package` in 12 serij `v8-{10,24}-{1000,5000}-f{0,1,2}` (3 ponovitve, V0–V26 in T1–T6 zelene v 36 zagonih).

**Narejeno:** harness sprejema — `HarnessJob` `walk` in `snap.N`, `ClientHarness` hoja po merjenju (žični in `rwfar`
bajti) in posnetki pri FOV merjenja in 10, `FarRender.DRAWN_IDS` vsebuje tudi L2 (V24 preveri mejo model ↔ L2);
`v-run.ps1 -Sprejem 1 -Far 0|1|2` (V25, V26), `ponovitve-run.ps1 -Scenarij sprejem`; `HarnessJobTest` +1;
[scenarij](../scenariji/M5-V.1-dalec.md).

**Izid** ([zapis](../meritve/2026-10-22-M5-V.8-sprejem.md)): vd 24 / 5.000 sličica p95 9,188 ms (0) → **8,893** (2, v
šumu) / **10,873** (1, +1,685, ločeno); vd 10 / 5.000 4,936 → 3,658 (2); `rwfar` 88–92 µs/tick/igralca; `rwfar`
0 B/s mirovanje, 513 B/s hoja, žica brez spremembe; V24 0 vrzeli / 0 dvojnih; sonda vidna na 368 blokih; VRAM
≈ 21,6 MiB [K]. Cilji D-035 izpolnjeni pri **2** → **D-051**: privzeto `RwFarView=2`. L3 sam (1) cilja sličice ne
izpolni — zapisano; na prizorišču brez sledenih onkraj 56 bi L2 ne kompenziral (~+1,7 ms).

**Odločitev o razdelitvi:** sprememba privzete vrednosti v kodi bi spremenila odtis in razveljavila 12 serij meritev,
zato je ločen paket **M5-V.8b** (koda + komentar + vrstice obnašanja, `unit`/`build`/`package`), kot pri Q30.

**Spremembe obnašanja moda:** pri privzetih nastavitvah še nobene (`RwFarView=0` do M5-V.8b).
**Minecraft meritve:** 36 zagonov sprejema (+ 3 starejše serije `0837/0847/0857` na prejšnji kodi, niso v presoji).

**Naslednji korak:** **M5-V.8b** (privzeto `RwFarView=2`), nato K12 in ostali K-paketi (K13, K10 ponovno oceniti;
kandidati iz M5-V.8: cena izrisa L3, pas `rwfar` po chunkih, PBO peka). Neodvisno: čistilni korak K3, M4.14b/M4.16;
ročna preverba z OptiFine.

### 2026-10-22 (112) — M5-V.7: združljivost izrisa L2/L3 — zaključeno

**Paket:** M5-V.7 · **Stanje:** **zaključeno** — vseh 5 zahtevanih testov zelenih na kodi `a369f197…`: `unit`, `build`,
`package`, `v7-posebni` (K1–K8, K16, K17a–j) in regresija L3 `v5-izris-10-64-1000` (V0–V24).

**Narejeno (D-050):**
- `FarCompat`: OptiFine prek refleksije; aktiven paket senčil + `RwFarShaders=0` (privzeto) → original (brez L2/L3,
  peke in skrivanja mrtvih), senčni prehod → brez impostorjev; stanje v `/rwimpostor` in JSON (`far*`).
- `SkinDownload` + `ImpostorBaker`: peka počaka na prenos URL skina / skina igralca (≤ 30 s, `downloadWaits`).
- `FarRender.l2()` brez `isInvisible()`: pol-viden NPC (`NpcVisible 2`) je v L2 pol-prozoren impostor — ista
  latentna napaka je bila v M5-V.6 (pol-vidni so v L2 ostali model).
- Harness: števci posebnih NPC-jev po imenu (`specialPrefix`), barva pečenega okvirja; `k-run.ps1 -Posebni 1`
  (9 klonov `RWV7_*`, `dev/tools/skin-streznik.js` z zamikom 4 s), merilo K17
  ([scenarij](../scenariji/M5-K.0-klient.md#m5-v7-posebni-npc-ji--posebni-1-merilo-k17)).
- JUnit: `FarCompatTest` (4, izključen iz `testOriginal`), `ImpostorFramesTest` +1, `HarnessJobTest` +1.

**Izid** ([zapis](../meritve/2026-10-21-M5-V.7-zdruzljivost.md)): velik (`Size 15`), morf (prašič), URL skin (okvir
`BB00BB` = preneseni magenta skin, 1.927 čakanj na prenos) in pol-viden (pol-prozoren) so impostor; neviden, ubit,
jahač in nosilec ostanejo model ali skriti; NPC brez hitboxa izven dometa vanille je impostor (`l2IzvenDometa` 1 —
veja, ki je M5-V.6 ni izmeril). Brez OptiFine je `FarCompat` neaktiven. Prvi zagon `v7-posebni` je padel na K17c
(pol-viden = model) → popravek zgoraj.
- **Samo statično [K]:** OptiFine 1.12.2 HD U G5 (sidro `"blockentities"` enkrat; s senčili `Shaders.endEntities`/
  `beginBlockEntities` pred njim in drugi klic v senčnem prehodu → samodejni izklop), Entity Culling 6.5.0 +
  RenderLib 1.4.5 (zakriti NPC ne sproži `RenderLivingEvent.Pre` → skladno), `customNPC_entities_mod` (peka ustvari
  pravi razred → njihov renderer). Tuji modi niso nameščeni v razvojnem klientu, zato meritve z njimi ni.

**Spremembe obnašanja moda:** pri privzetih nastavitvah nobene (`RwFarView=0`); novo stikalo `RwFarShaders`
(privzeto 0), registrirano v 04-REGISTRI.
**Minecraft meritve:** 3 zagoni `v7-posebni` (prvi ustavljen v upravljalniku pred meritvijo, drugi K17c padel, tretji
zelen) + 1 `-Izris 1`.

**Naslednji korak:** **M5-V.8** (sprejem: A/B celic M5-V.1 z `RwFarView` 0/1/2, posnetki pri 64/128/256/368
blokih). Neodvisno: čistilni korak K3, M4.14b/M4.16; ročna preverba z OptiFine/senčili v instanci uporabnika.

### 2026-10-21 (111) — Q30: K14b `RwHeadwearSkip` privzeto 1 — zaključeno

**Paket:** Q30 (odločitev lastnika) · **Stanje:** **zaključeno** — sprememba privzete vrednosti; zahtevani testi
`unit`, `build`, `package` na novi kodi (upravljalnik).

**Odločitev (D-049):** uporabnik 21. 10.: »Ja, Q30: K14b (RwHeadwearSkip=1) naj bo privzeto vklopljen. Zapisi
odlocitev.« `CustomNpcs.RwHeadwearSkip` je privzeto **1** (komentar konfiguracije dopolnjen), 0 vrne original;
varovala (OptiFine, ne-`SimpleTexture`/URL skini, `.mcmeta`, brez alfa testa → original) nespremenjena. Q30 zaprt
(arhiv §4), vrstica v 04-REGISTRI, 03-FAZE in [meritveni zapis](../meritve/2026-10-19-M5-K-K14b-pokrivalo-brez-prozornih.md)
posodobljeni.

**Dokaz:** brez novih meritev — A/B in preverba pikslov 19. 10. (idle-500 p50 −25 %, idle-200 −18 %, 0 razlik v 7
serijah). `dev/run/config/CustomNpcs.cfg` ima izrecno `RwHeadwearSkip=0` (obstoječi config obdrži vrednost), zato
merilni pogoji scenarijev ostanejo nespremenjeni.

**Spremembe obnašanja moda:** nova namestitev riše pokrivalo brez prozornih štirikotnikov (registrirano).
**Minecraft meritve:** nobene.

**Naslednji korak:** **M5-V.7** (združljivost). Neodvisno: čistilni korak K3, M4.14b/M4.16.


### 2026-10-21 (110) — M5-V.6: stopnja L2 (sledeni oddaljeni NPC-ji kot impostorji) — zaključeno

**Paket:** M5-V.6 · **Stanje:** **zaključeno** — vseh 8 zahtevanih testov zelenih na kodi `6db12833…`: `unit`, `build`,
`package`, `v6-200-off`, `v6-200-on`, `v6-500-off`, `v6-500-on` (3 + 3 ponovitve, T1–T6 in K16 zeleni v 12 zagonih) in
regresija L3 `v5-izris-10-64-1000` (V0–V24).

**Narejeno (D-048):**
- `ImpostorL2` (prag `RwImpostorStart` 8..512, histereza 4 bloke). `FarRender`: `RenderLivingEvent.Pre` (`LOWEST`,
  prehod 0) prekliče izris sledenega NPC-ja onkraj praga (stoj, viden, živ, ne jaha, pečen videz) in ga nariše v
  medpomnilniku L3; videz iz `FarLook.describe(writeSpawnData())` na klientu (≤ 16/sličico, znova po
  `EntityNPCInterface.rwLookVersion`); sledeni NPC izven dometa upodobitve vanille se nariše do konca megle.
  Števci L2 v `/rwimpostor` in JSON klienta. `CustomNpcs.RwImpostorStart` (56).
- Harness: ključ posla `impostorStart`, polja `l2*`; `k-run.ps1 -Dalec 1 -Far -ImpostorStart`, merilo K16
  ([scenarij](../scenariji/M5-K.0-klient.md#m5-v6-l2--dalec-1--far-02--impostorstart-56-merilo-k16)).
- JUnit: `ImpostorL2Test` (4), `HarnessJobTest` (+1); izključena iz `testOriginal`.

**Izid A/B** (`-Variant idle -Dalec 1 -RenderDistance 10`, `-Far 0` proti `-Far 2 -ImpostorStart 56`, mediane 3 ponovitev,
razponi povsod ločeni; [zapis](../meritve/2026-10-21-M5-V.6-L2.md)):

| Celica | L2 / NPC (pričakovano) | p50 ms | p95 ms | p99 ms | FPS |
|---|---|---|---|---|---|
| idle-200 | 137 / 200 (137) | 4,886 → **2,013 (−58,8 %)** | 5,414 → 2,348 | 6,005 → 2,632 | 200,6 → 485,4 |
| idle-500 | 437 / 500 (437) | 11,591 → **2,331 (−79,9 %)** | 13,865 → 3,357 | 19,121 → 3,949 | 83,1 → 411,4 |

- 0 brez peke, 0 napak videza, en pečen videz za vse klone; izris L2 v `FarRender` povp. 56 / 164 µs na sličico;
  integrirani strežnik in GC brez spremembe.
- Ni izmerjeno v svetu: veja izven dometa vanille (NPC brez hitboxa) — v prizorišču je števec 0 (za M5-V.7/V.8).
- Surovi izidi: git-ignored `audit/m25c-klient-2026-10-21-1437.md`, `-1443.md`, `-1450.md`, `-1456.md`.

**Spremembe obnašanja moda:** pri privzetih nastavitvah nobene (`RwFarView=0`); z `RwFarView=2` NPC onkraj praga
izgubi animacijo udov, oči in ime — zato privzeto 0 do M5-V.8 (registrirano v 04-REGISTRI).
**Minecraft meritve:** 12 zagonov klienta (A/B) + 1 `-Izris 1`.

**Naslednji korak:** **M5-V.7** (združljivost: OptiFine, Entity Culling, `customNPC_entities_mod`, posebni NPC-ji —
tu tudi preverba NPC-ja brez hitboxa). Neodvisno: čistilni korak K3, M4.14b/M4.16; Q30 je uporabnik odločil
21. 10. (K14b privzeto 1, v vrsti kot svoj paket).

### 2026-10-21 (109) — M5-V.5: nadomestki in izris L3 — zaključeno

**Paket:** M5-V.5 · **Stanje:** **zaključeno** — vseh 5 zahtevanih testov zelenih na kodi `92f1a22b…`: `unit`, `build`,
`package`, `v5-izris-24-64-5000`, `v5-izris-10-64-1000` (V0–V24 zeleni v obeh zagonih).

**Narejeno (D-047):**
- `FarRender` (en `BufferBuilder` za vse nadomestke, pol-vidni v drugem z alfo 0,15; cilindrični billboard, okvir po
  kotu `ImpostorFrames.frameFor`, alfa test 0,1, lightmap kraja `World.getCombinedLight`, megla; šiv po `entityId`,
  mrtva entiteta z nadomestkom se ne nariše), `FarInterp` (premik v 1 s od prikazanega položaja, skok nad 16 blokov),
  coremod `RenderEntitiesPatch` (`RenderGlobal.renderEntities` pred `"blockentities"`, prehod 0; rezerva
  `RenderWorldLastEvent`), ukaz `/rwimpostor`. `FarBand.PREFETCH`: strežnik videz izračuna 16 blokov pred notranjo
  mejo, da je ob prehodu že pečen.
- `FarDrawCheck` (harness): sonda na robu pasu, A/B/C pikslov v pravi sličici s FOV 10; faza `SEAM` s hitrostjo hoje.
  `v-run.ps1 -Izris 1` z merili V22–V24
  ([scenarij](scenariji/M5-V.1-dalec.md#m5-v5-izris-l3-v22v24--far-1--impostor-1--izris-1)).
- JUnit: `FarInterpTest` (9), `RenderEntitiesPatchTest` (4, prave bajte Forge 1.12.2), `FarDrawCheckTest` (4),
  `FarBandTest` (+1), `HarnessJobTest` (+1); izključeni iz `testOriginal`.

**Izid v svetu (ena ponovitev na celico, `-Far 1 -Impostor 1 -Izris 1`, nočni upravljalnik):**

| Celica | Narisanih min/max (pričakovano) | Živih v šivu | Najdlje | Izris povp. / max | V23 sonda | V24 vrzeli / dvojnih / prehodov | Sličica p50 / p95 ms | B/s | MSPT p50 / p95 ms |
|---|---|---|---|---|---|---|---|---|---|
| vd 24, domet 64, 5.000 | 4831 / 4831 (4820..4841) | 40 | **368,0** | 1.305,6 / 18.404 µs | 368 blokov: 114 px, razlika 165, kontrola 0 | **0 / 0** / 170 + 150 | 9,42 / 14,45 (FPS 99,9) | 887 | 58,72 / 71,303 |
| vd 10, domet 64, 1.000 | 176 / 176 (170..176) | 0 | 168,8 | 53,1 / 160,5 µs | 148 blokov: 810 px, razlika 98, kontrola 0 | **0 / 0** / 25 + 25 | 1,23 / 2,10 (FPS 761,8) | 170 | 1,245 / 1,704 |

- Poseg coremoda uspel v obeh zagonih, 0 sličic prek rezerve; 0 izven frustuma, 0 nadomestkov brez peke.
- Pot do zelenega: prvi zagon (`9c004fa8…`) je dal v V24 15 vrzeli (videz 0 v prvem krogu → nepečen ob prehodu) in
  400 dvojnih (mrtva entiteta v chunku do ticka) — popravljeno s predpasom videza in skrivanjem mrtve entitete.
  Drugi (`1538c62d…`) je pri vd 10 dal 0 pikslov sonde na 168,8 bloka (za koncem megle 160, neviden tudi v
  originalu) — sonda je odslej najbolj oddaljen nadomestek ≥ 12 blokov pred koncem megle; merilo pri vd 24 ostane 368.
- Sličica pri vd 24/5000: p50 9,42 ms proti 7,47 ms v M5-V.4 brez izrisa (ena ponovitev, ne A/B) — izris 4831
  impostorjev stane ~1,3 ms na sličico, kar se ujema s števcem `FarRender`. Primerjava z originalom je M5-V.8.
- Surovi izidi: git-ignored `audit/v-24-64-5000-izris-2026-10-21-141027*`, `audit/v-10-64-1000-izris-2026-10-21-141445*`.

**Spremembe obnašanja moda:** pri privzetih nastavitvah nobene vidne; coremod spremeni bajtkodo
`RenderGlobal.renderEntities` (klic, ki se pri `RwFarView=0` takoj vrne — registrirano v 04-REGISTRI).
**Minecraft meritve:** 6 zagonov `-Izris 1` (2 zelena na končni kodi).

**Za M5-V.8:** `FarRender` riše tudi nadomestke za koncem megle (`RD · 16`), kar je odvečno delo; hitrejše premikanje
od ~8 blokov/s lahko na meji pusti vrzel do ~1 s.

**Naslednji korak:** **M5-V.6** (L2: sledeni oddaljeni NPC-ji kot impostorji, `RwFarView=2`). Neodvisno: čistilni
korak K3, M4.14b/M4.16; Q30 odprt.

### 2026-10-21 (108) — M5-V.4: peka impostorjev na klientu — zaključeno

**Paket:** M5-V.4 · **Stanje:** **zaključeno** — vseh 5 zahtevanih testov zelenih na kodi `86da496c…`: `unit`, `build`,
`package`, `v4-imp-24-64-5000`, `v4-imp-10-64-1000` (V0–V21 zeleni v obeh zagonih).

**Narejeno (D-046):**
- `ImpostorFrames` (čiste funkcije brez GL: postavitev traku 8 celic 40 × 72, obrez, ploskovno pomanjšanje v
  ≤ 32 × 64, mipmapi z ohranjeno pokritostjo alfe, izbira okvirja po kotu), `ImpostorSlots` (LRU trakov),
  `ImpostorAtlas` (tekstura GL, 4 stopnje mipmap, stran `RwImpostorAtlas`, privzeto 2048 = 168 videzov),
  `ImpostorBaker` (vrsta, en videz na sličico, lažna entiteta, FBO 128 × 256, ortogonalno pod 8 koti, brez
  lightmapa; odlog pri skinu igralca/URL; izklop brez FBO). `FarClient` videz postavi v vrsto peke ob `LOOK`
  in ob vnosu z znanim, nepečenim videzom; ob izklopu `RwFarView` sprosti atlas.
- `ImpostorCheck` (harness): primerjava pikslov impostor ↔ model pri 56 blokih (perspektiva z gostoto pikslov
  zaslona 1080 p, FOV 70) za 8 videzov × 8 kotov in kontrola občutljivosti; merila določena pred meritvijo.
- Harness: `impostorCheck=1`, polja `imp*` v JSON klienta; `v-run.ps1 -Impostor 1` z meriloma V20/V21
  ([scenarij](../scenariji/M5-V.1-dalec.md#m5-v4-peka-impostorjev-v20v21--far-1--impostor-1)).
- JUnit: `ImpostorFramesTest` (9), `ImpostorSlotsTest` (3), `HarnessJobTest` (+1); izključeni iz `testOriginal`.

**Izid v svetu (ena ponovitev na celico, `-Far 1 -Impostor 1`, nočni upravljalnik):**

| Celica | Pečenih / v atlasu / brez peke / napak | Peka povp. / max ms | rwfar µs/tick/igralca | Sličica p50 / p95 ms | B/s | MSPT p50 / p95 ms |
|---|---|---|---|---|---|---|
| vd 24, domet 64, 5.000 | 9 / 9 / **0** / 0 | 12,30 / 49,89 | 92,123 | 7,47 / 11,96 (FPS 125,0) | 915 | 57,672 / 71,303 |
| vd 10, domet 64, 1.000 | 9 / 9 / **0** / 0 | 8,87 / 32,46 | 6,051 | 1,16 / 1,48 (FPS 827,1) | 172 | 1,147 / 1,573 |

Primerjava pikslov (enaka v obeh zagonih; IoU in razlika združeno čez 8 kotov, prag IoU ≥ 0,70, razlika ≤ 32/255):

| Videz | IoU (najslabši kot) | Razlika barve | Piksli model / impostor | Okvir |
|---|---|---|---|---|
| osnova (`PERF_Idle` iz kanala) | 0,884 (0,818) | 7,6 | 1656 / 1788 | 32 × 58 |
| `elffemale.png` | 0,902 (0,848) | 14,8 | 1676 / 1820 | 32 × 58 |
| `elfmage.png` | 0,912 (0,864) | 11,5 | 1702 / 1828 | 32 × 58 |
| `elfqueenformal.png` | 0,906 (0,856) | 21,7 | 1676 / 1808 | 32 × 58 |
| `elfsoldierhelmet.png` | 0,911 (0,856) | 22,0 | 1692 / 1820 | 32 × 58 |
| `blood_elf_mage.png` | 0,902 (0,856) | 15,1 | 1672 / 1804 | 32 × 58 |
| velikost 10 | 0,926 (0,789) | 4,2 | 6540 / 6828 | 32 × 57 |
| velikost 2 | 0,894 (0,789) | 12,5 | 239 / 244 | 32 × 57 |

- Občutljivost: enobarvni skin proti modelu osnove da razliko **95,5** (> 64), torej merilo razlike zazna napačno
  teksturo. Impostor ima 2–9 % več pokritih pikslov kot model (robovi pomanjšanja in mipmapov), IoU ostane ≥ 0,88.
- 9 pečenih = 1 videz iz kanala (vsi kloni) + 8 videzov preverbe; 0 odloženih, 0 obrezanih na robu, FBO na voljo.
- Peka enega videza stane 9–12 ms povprečno, največ 50 ms (`glReadPixels`); proračun en videz na sličico omeji
  zastoj na eno sličico na nov videz. Za M5-V.8: pri mnogo različnih videzih ob vstopu v svet je to zaporedje
  počasnih sličic — kandidat za asinhrono branje (PBO).
- Sličica/MSPT brez A/B (izrisa impostorjev še ni, M5-V.5); primerjava z originalom je M5-V.8.
- Surovi izidi: git-ignored `audit/v-24-64-5000-imp-2026-10-21-131120*`, `audit/v-10-64-1000-imp-2026-10-21-131528*`.

**Spremembe obnašanja moda:** nobene pri privzetih nastavitvah (`RwFarView=0`: peka se ne zgodi, atlas se ne
ustvari). **Minecraft meritve:** 2 zagona `-Far 1 -Impostor 1`.

**Naslednji korak:** **M5-V.5** (nadomestki in izris L3 iz atlasa). Neodvisno: čistilni korak K3, M4.14b/M4.16;
Q30 odprt.

### 2026-10-21 (107) — M5-V.3: opis videza (`FarLook`, id videza, `REQ`/`LOOK`) — zaključeno

**Paket:** M5-V.3 · **Stanje:** **zaključeno** — vseh 5 zahtevanih testov zelenih na kodi `57ac59cc…`: `unit`, `build`,
`package`, `v3-far-24-64-5000`, `v3-far-10-64-1000` (V0–V19 zeleni v obeh zagonih).

**Narejeno (D-045):**
- `FarLook` (opis = podmnožica spawn NBT po beli listi, brez imena/naslova/`ShowName`/vlog; kanonična oblika,
  FNV-1a 64 + mešanje, nikoli 0), `FarLooks` (strežniška tabela hash → kratek id, deljen med kloni, LRU 256
  nedejavnih), `FarRequests` (`REQ` samo za prejete id-je, vsak id enkrat do novega `HELLO`, vrsta ≤ 1.024,
  ≤ 8 odgovorov na tick na igralca). `FarCodec.VERSION` 2 (`REQ`, `LOOK` z GZIP opisom). `FarServer`: največ
  64 opisov na tick ob koncu ticka, `UPDATE_NPC` (`EntityNPCInterface.updateClient`) → `invalidateLook`, čas opisov
  v `usNaTickNaIgralca`. `FarClient`: id → hash na svet, hash → opis v LRU 512, preverba hasha po razpakiranju,
  ponovna zahteva po 10 s; `FarClient.description(id)` za M5-V.4.
- Harness: polja `farVidez*`/`farBrezVideza` v JSON klienta; `v-run.ps1 -Far 1` z merilom V19
  ([scenarij](../scenariji/M5-V.1-dalec.md#m5-v3-opis-videza-v19-isti--far-1)).
- JUnit: `FarLookTest` (6: kloni isti hash, sprememba videza nov hash, ime/naslov ne vplivata …), `FarLooksTest` (3),
  `FarRequestsTest` (4); izključeni iz `testOriginal`.

**Izid v svetu (ena ponovitev na celico, `-Far 1`, nočni upravljalnik):**

| Celica | Pas / nadomestkov klient | Videzov v rabi (strežnik) | Izračunov skupaj | Klient: opisov / zahtevanih id / odgovorov / napak / brez opisa | rwfar µs/tick/igralca (max krog) | Sličica p50 / p95 ms | B/s | MSPT p50 / p95 ms |
|---|---|---|---|---|---|---|---|---|
| vd 24, domet 64, 5.000 | 4870 / 4870 | **1** | 4870 | 1 / 1 / 1 / 0 / **0** | **93,373** (2,42 ms) | 7,37 / 11,84 (FPS 126,3) | 933 | 57,672 / 73,4 |
| vd 10, domet 64, 1.000 | 175 / 175 | **1** | 175 | 1 / 1 / 1 / 0 / **0** | **6,180** (0,18 ms) | 1,20 / 1,83 (FPS 785,7) | 171 | 1,245 / 1,769 |

- 4870 klonov `PERF_Idle` deli **en** videz; klient je za vse nadomestke prejel en opis z enim `REQ`, hash preverjen,
  0 zavrnjenih zahtev, 0 prevelikih opisov. Na žici videz stane en `LOOK` na igralca, ne na NPC.
- Vsi opisi so bili izračunani pred `rwfar reset` (v merilnem oknu `nsVidez=0`, `izracunov=0`, `cakaVidez=0`), zato
  čas izračuna opisov v oknu ni izmerjen — okno meri stalno stanje. Prvi krog pri 4870 NPC-jih se razdeli na
  ~77 tickov po 64 opisov.
- V18 drži tudi z opisi: 93,4 µs proti 89,2 µs v M5-V.2 (ena ponovitev, razlika ni dokazana). Sličica/MSPT brez A/B,
  primerjava z originalom je M5-V.8.
- Surovi izidi: git-ignored `audit/v-24-64-5000-far-2026-10-21-124436*`, `audit/v-10-64-1000-far-2026-10-21-124827*`.

**Znana omejitev (za M5-V.8):** NPC, ki kliče `updateClient` vsak tick, vsak tick pride v vrsto in porabi del proračuna
64 izračunov na tick; mirujoči/statični NPC-ji tega ne sprožijo.

**Spremembe obnašanja moda:** nobene pri privzetih nastavitvah (`RwFarView=0`; `invalidateLook` brez prijavljenih
igralcev `rwfar` ne naredi nič). **Minecraft meritve:** 2 zagona `-Far 1`.

**Naslednji korak:** **M5-V.4** (peka impostorjev na klientu, opis iz `FarClient.description(id)`). Neodvisno: čistilni
korak K3, M4.14b/M4.16; Q30 odprt.

### 2026-10-21 (106) — M5-V.2: strežnik `rwfar` (register, pas, delta, kanal) — zaključeno

**Paket:** M5-V.2 · **Stanje:** **zaključeno** — vseh 5 zahtevanih testov zelenih na kodi `f031f24b…`: `unit`, `build`,
`package`, `v2-far-24-64-5000`, `v2-far-10-64-1000` (V0–V18 zeleni v obeh zagonih).

**Narejeno:**
- Paket `rework/far` (D-044): `FarRegistry` (NPC-ji po svetovih prek `IWorldEventListener`, vzorec M5.16),
  `FarBand` (Chebyshev `(min(sledilnik, meja) − 8, min(RwFarRange, domet HELLO, meja)]`), `FarDelta` (samo novi,
  premik > 0,5 bloka, yaw > 11,25°, videz/zastavice; odstranjeni), `FarCodec` (`HELLO`/`BYE`/`SNAP`/`CLEAR`, vnos 14–16 B,
  pozicija 24 bitov z razvojem okoli igralca), `FarServer` (kadenca `(tick + id) % 20`, `isPlayerWatchingChunk`,
  nevidni in mrtvi s skritim truplom nikoli, števec ns na igralca), `FarClient` (`HELLO`/`BYE`, shramba brez izrisa),
  `/rwfar [0|1|reset|domet n]`. Stikali `RwFarView` (privzeto 0) in `RwFarRange` (384).
- Harness: `farView=` v navodilu, polja `far*` v JSON; `v-run.ps1 -Far 1` z merili V16–V18
  ([scenarij](../scenariji/M5-V.1-dalec.md#m5-v2--far-1-kanal-rwfar)).
- JUnit: `FarBandTest` (4), `FarCodecTest` (7), `FarDeltaTest` (5), `HarnessJobTest` +1.

**Odločitve (samostojno po pooblastilu):** D-044 — en ključ `RwFarView` za obe strani, 24-bitna pozicija z
razvojem, videz 0 do V.3, hoja iz premika, register vedno prijavljen, števec v `/rwfar`, klientska shramba v V.2 za
preverbo od konca do konca, `RwFarLos` odložen.

**Iteracija 2:** `unit` je prvič padel (`testOriginal` je prevajal nove `Far*Test`) → izključeni v `dev/build.gradle`;
na kodi `5a3454bd…` so `unit`, `build`, `package` zeleni. `v2-far-24-64-5000` je padel samo na V16/V17 (V0–V15, V16a,
V18 zeleni; klient 4870 nadomestkov ob začetku in koncu merjenja, pričakovano 4860..4880): skripta je `/rwfar` brala
šele po odjavi klienta (`igralcev=0`, `poIgralcih=` prazen, chunki izpraznjeni → 4573 NPC-jev, µs/tick 0). Napaka
harnessa, ne kode `rwfar`. Popravek: klient na dediciranem strežniku po DONE ostane povezan do `say RWHARNESS-IZHOD`
(največ 120 s), ki ga `v-run.ps1` pošlje po branju `rwdiag vd` in `rwfar`.

**Iteracija 3 (izid v svetu, ena ponovitev na celico, `-Far 1`, nočni upravljalnik):**

| Celica | NPC v klientu | Nadomestkov klient / strežnik (pričakovano) | rwfar µs/tick/igralca (max krog) | rwfar B/s v oknu | Sličica p50 / p95 ms | B/s | MSPT p50 / p95 ms |
|---|---|---|---|---|---|---|---|
| vd 24, domet 64, 5.000 | 170 | 4870 / 4870 (4860..4880) | **89,188** (2,31 ms) | 0 | 7,68 / 12,17 (FPS 121,4) | 896 | 59,769 / 73,4 |
| vd 10, domet 64, 1.000 | 25 | 175 / 175 (170..175) | **6,003** (0,15 ms) | 0 | 1,19 / 1,84 (FPS 793,8) | 162 | 1,278 / 1,737 |

- Kanal deluje od konca do konca: `HELLO` sprejet (domet 384 oz. `RD·16` = 160 pri RD 10), klient ima ob začetku in
  koncu okna isto število nadomestkov kot strežniški pas; mirujoči NPC-ji v oknu ne pošljejo nič (samo spremembe).
  Začetni posnetek je bil poslan pred `rwfar reset`, zato njegovi bajti v oknu niso šteti (merilo jih ne zahteva).
- V18 drži (≤ 100 µs), a pri 24/5000 tesno: ~1,8 ms na krog za 4870 NPC-jev v pasu (~0,37 µs na NPC na krog).
  Sličica/MSPT sta ena ponovitev brez A/B — primerjava z originalom (M5-V.1 §2: 7,19 ms, 942 B/s, MSPT p95 71,3)
  je stvar M5-V.8; ta zagon ni dokaz koristi ali cene.
- Surovi izidi: git-ignored `audit/v-24-64-5000-far-2026-10-21-122344*`, `audit/v-10-64-1000-far-2026-10-21-122736*`.

**Spremembe obnašanja moda:** nobene pri privzetih nastavitvah (`RwFarView=0`). **Minecraft meritve:** 2 zagona `-Far 1`
(+ 1 padli zagon iteracije 2, napaka harnessa).

**Naslednji korak:** M5-V.3 (opis videza: podmnožica spawn NBT, hash, LRU, `REQ`). Za V.8 zabeleženo: cena kroga pri
5.000 je blizu meje 0,1 ms/tick/igralca — kandidat za optimizacijo (npr. pas iz chunkov namesto preleta registra).

### 2026-10-21 (105) — M5-V.1: matrika "daleč" 8 × 3, varianta A zavrnjena — zaključeno

**Paket:** M5-V.1 · **Stanje:** **zaključeno** — vseh 13 zahtevanih testov zelenih na kodi `f1913db9…`
(commit `82c82e5`): `unit`, `build`, `package`, oba poskusna zagona in 8 celic × 3 ponovitve (V0–V15 in
T1–T6 zeleni v vseh 24 zagonih, naloženih/sledenih/upodobljenih razpon 0).

**Narejeno:**
- Seja 104: celica 10/64/1000 je na prvi kodi padla na V5 (300 namesto 230 naloženih) → D-042
  (`/rwdiag vd` takoj izprazni chunke zunaj `PlayerChunkMap`); na novi kodi 230 v vseh ponovitvah.
- Zapis [meritve/2026-10-21-M5-V.1-dalec.md](../meritve/2026-10-21-M5-V.1-dalec.md) (tabele prepisane iz
  git-ignored `audit/m25c-dalec-2026-10-21-*.md`), vrstica v `04-REGISTRI`, M5-V.1 v `03-FAZE` prečrtan, D-043.
- **vd 10 → 24 (original):** MSPT p50 1,245 → 7,340 ms (1000) in 8,913 → 56,623 ms (5000), ×5,9–6,4
  (naloženih 4–4,3×, chunkov 5,4×); sličica p50 +1,9 / +2,7 ms; B/s nespremenjeni (175 / ~1 KB/s).
- **Varianta A (368 proti 64):** pri 24/5000 sličica p50 7,189 → 163,977 ms (128 → 6 FPS), 942 →
  29.331 B/s, klientski tick 0,886 → 9,337 ms, MSPT v šumu; 23–32 µs sličice in ~6 B/s na sledenega NPC-ja,
  ~1,3 kB na spawn. Spawn NBT 559 B (vsi kloni).

**Odločitve (samostojno po pooblastilu):** D-043 — varianta A zavrnjena z meritvijo (~184× nad ciljem
sličice, ~28× nad ciljem omrežja), varianta B in cilj vd 24 ostaneta; izhodišče V.8 so celice originala;
strežniški cilj +≤ 0,1 ms na igralca V.2/V.8 meri z lastnim števcem kode `rwfar`, ker ga A/B MSPT ne razloči.
Opomba za operaterja: 5.000 naloženih NPC-jev pri vd 24 na merilnem stroju ne drži 20 TPS (vanilla cena).

**Omejitve:** strežnik in klient na istem CPU; samo mirujoči NPC-ji in statična kamera (hoja ni merjena);
en videz (kloni).

**Spremembe obnašanja moda:** nobene. **Minecraft meritve:** 8 serij × 3 + 2 poskusna zagona.

**Naslednji korak:** **M5-V.2** (strežnik: register, pas, kanal `rwfar`, stikali `RwFarView`/`RwFarRange`
privzeto 0, števec časa na igralca). Neodvisno: čistilni korak K3, M4.14b/M4.16; Q30 odprt.

### 2026-10-21 (103) — M5-V.1: harness "daleč" (dedicated server + klient) — delno

**Paket:** M5-V.1 · **Stanje:** **delno** — harness v kodi in preverjen (unit, build, package, oba poskusna
zagona zelena na kodi `5cf3683a…`, commit `2b1bbbf`); matrika 8 celic × 3 registrirana, meritve še niso narejene.

**Narejeno:**
- `v-run.ps1`: ena celica (`-Vd 10|24`, `-Domet 64|368`, `-N 1000|5000`) na dediciranem strežniku
  (`runServer --world rwv-dalec`, svež svet) s priključenim klientom; gruče 5 × 5 v izseku ±35° do 358 blokov,
  kamera pri z = 3000; merila V0–V15 (veljavnost V9: upodobljenih ≥ NPC-jev v klientu). Scenarij
  [M5-V.1](../scenariji/M5-V.1-dalec.md); `ponovitve-run.ps1 -Scenarij dalec`.
- Harness klienta: `connect=` (GuiConnecting, čaka `RWHARNESS-SCENA` v klepetu), `renderWeight=`, žični bajti
  (Netty), čas klientskega ticka. Strežnik: `/rwdiag vd|track|spawnnbt` (`rework/diag/FarProbe`).
  `build.gradle`: `-PrwWorld`. JUnit: `FarProbeTest` (3), `HarnessJobTest` +1.
- D-040 (metoda poskusa).

**Odločitve (samostojno po pooblastilu):** D-040 — svet prek `--world` namesto prepisa `server.properties`;
celica vd 10 prek `PlayerList.setViewDistance` po spawnu pri 24; varianta A kot diagnostični ukaz, ne stikalo.
Najprej dva poskusna zagona (`v1-poskus-24-368-5000`, `v1-poskus-10-64-1000`), šele nato matrika 8 celic × 3.
D-041 — V12 je veljavnost vzorca (≥ 300 sličic, ≥ 15 tickov/s), ne prag 10 FPS: poskus 24/368/5000 je po
popravku uteži (V9 5000/5000, V11 0 ponastavitev v merjenju) padel samo na V12 pri 356 sličicah (6 FPS), kar je
izmerjena cena variante A.

**Poskusna zagona (enkratna, nista meritev celic):** `v1-poskus-24-368-5000` V0–V15 zeleno: 5000/5000 NPC-jev v
klientu in upodobljenih, sličica p50 159,8 ms (6,1 FPS), **368 sličic** v 60 s (blizu praga 300; prag se ne
znižuje), MSPT p50 56,6 ms, 29 171 B/s. `v1-poskus-10-64-1000` V0–V15 zeleno: 230 naloženih, 25 v klientu,
sličica p50 1,17 ms (819 FPS), MSPT p50 1,25 ms, 172 B/s.

**Ni narejeno:** meritve celic — registrirani `v1-10-64-1000` … `v1-24-368-5000` (8 × `ponovitve-run.ps1
-Scenarij dalec`, 3 ponovitve).

**Spremembe obnašanja moda:** nobene (vse samo v harnessu in `/rwdiag`).

**Naslednji korak:** zagon matrike `v1-*` (8 celic × 3) → zapis meritve M5-V.1 in odločitev o varianti A.

### 2026-10-21 (102) — M5-K K5: stopnja podrobnosti L1 — v kodi, privzeto 0

**Paket:** M5-K K5 (L1) · **Stanje:** **zaključeno** — koda, A/B 3 + 3, `RwLodStart` privzeto 0 (D-039).

**Narejeno:**
- Nov razred `rework/render/Lod`, stikalo `RwLodStart` (bloki, 0 = original). Onkraj praga `LayerEyes`
  ne riše oči, `LayerHeadwear` pusti ploščato `bipedHeadwear` (videz `HeadWearType=0`) namesto 3D
  `ModelHeadwear`, `renderName` ne riše imena (original ga skrije že onkraj 22,6 bloka).
- Harness: `k-run.ps1 -Lod`, `HarnessJob.lodStart`, `ClientHarness` šteje odločitve L1; merilo **K15**
  primerja delež L1 z geometrijo prizorišča (±5 točk). `LodTest` (3), `HarnessJobTest` +1;
  `LodTest` izločen iz `testOriginal`.
- A/B nočni upravljalnik `k5-200-off/on`, `k5-500-off/on` na kodi `0a8d470e…`, kjer so zeleni tudi
  `unit`, `build`, `package`. T1–T6 v 4 serijah, K0–K8 in K15 v 12 zagonih zeleno; K15 idle-500
  60,6 % = pričakovano 60,6 %, idle-200 0 %.
- idle-500 p50 **11,464 → 8,691 ms (−24,2 %)**, p95 12,810 → 10,124 (−21,0 %), p99 16,858 → 13,285
  (−21,2 %), FPS 84,5 → 110,7; vsi razponi ločeni. idle-200 p50 +0,5 %, p95 +0,2 %, p99 +0,7 % —
  prekrivanje (preverba razdalje brez L1 je v šumu).
- Zapis [meritve/2026-10-21-M5-K-K5-lod-L1.md](../meritve/2026-10-21-M5-K-K5-lod-L1.md), vrstica v
  `04-REGISTRI` (meritve), K5 v `03-FAZE` prečrtan, D-039, Q32 v arhivu §4.

**Odločitve (samostojno po pooblastilu):**
- **Q32 → D-039:** `RwLodStart` privzeto **0**. L1 spremeni videz oddaljenih NPC-jev, zato D-032 ne
  velja (D-007); vklop (priporočeno 32) je izbira uporabnika. Za privzeto pot je boljši kandidat K14b
  (slikovno enak, primerljiv dobitek) — Q30 ostaja odprt.
- Največje število upodobljenih, prekrivne teksture/delci in histereza praga niso del tega paketa;
  ponovna ocena po M5-V (L2 impostor).

**Omejitve:** samo statična kamera; videz L1 ni ocenjen s posnetkom; kombinacija K14b + L1 ni izmerjena.

**Spremembe obnašanja moda:** nobene pri privzetih nastavitvah. **Minecraft meritve:** 4 serije × 3.

**Naslednji korak:** **M5-V** (najprej M5-V.1 po 03-FAZE). Neodvisno: čistilni korak K3, M4.14b/M4.16.

### 2026-10-21 (101) — M5-K K3: A/B oči iz display lista — zavrženo

**Paket:** M5-K K3 · **Stanje:** **zaključeno (zavrženo)** — A/B 3 + 3 brez koristi, `RwEyesList` ostane 0.

**Narejeno:**
- Analiza serij nočnega upravljalnika (prvi pravi Minecraft cikel OPS.2): `k3-200-off/on`, `k3-500-off/on`,
  vse na kodi `c5289db1…`, kjer so zeleni tudi `unit`, `build`, `package`. T1–T6 v 4 serijah, K0–K8 v 12
  zagonih zeleno; K14 potrdi pot z listom (0 izrisov po originalu); med serijami ni bilo vnosa.
- p50 idle-200 **6,451 → 6,644 ms (+3,0 %)**, razpona ločena v slabšo smer; idle-500 11,435 → 11,754
  (+2,8 %, prekrivanje); p95 +1,7 / +3,3 %; p99 idle-500 +6,3 % (ločena, slabše); FPS −2,1 / −2,8 %.
  Original se ujema z baseline M5-K.1. Po D-016 ni koristi → D-032 ne velja → **zavrženo** kot K14.
- Zapis [meritve/2026-10-21-M5-K-K3-oci-display-list.md](../meritve/2026-10-21-M5-K-K3-oci-display-list.md),
  vrstica v `04-REGISTRI` (meritve), K3 v `03-FAZE` prečrtan.

**Odločitve (samostojno po pooblastilu):**
- Koda poskusa (`EyesList`, `EyesCheck`) ostane v drevesu s privzeto 0: obvezni testi so tekli na tej kodi;
  odstranitev je ločen čistilni korak (unit/build/package), ne del tega paketa.
- Q30 (vklop K14b) v tem paketu **ni** odločen: zahteva spremembo privzete vrednosti in nove teste, je
  izven obsega K3 in ostane odprt (priporočilo v vrstici Q30 nespremenjeno).

**Ugotovitev:** [H] pri nekaj štirikotnikih na NPC je cena v gonilniku na izris; list doda
`glPushAttrib`/`glCallList`/uskladitev stanja in ne skrajša ničesar. Za morebitni nov paket oči najprej
zgornja meja (oči izpuščene).

**Spremembe obnašanja moda:** nobene. **Minecraft meritve:** 4 serije × 3 (zgoraj).

**Naslednji korak:** **K5** (LOD) po vrstnem redu D-035, nato M5-V. Neodvisno: čistilni korak K3, M4.14b/M4.16.

### 2026-10-20 (100) — OPS.2: frontend in neprekinjeno avtonomno delo

**Paket:** OPS.2 (razvojno orodje) · **Stanje:** implementirano, 40/40 krmilnih testov in
brskalniška preverba uspešni; kratka prava Codex in Claude 1 poziva uspešna. Minecraft K3 še neizmerjen.

**Narejeno:**
- Lokalni frontend **Nočna izmena**: `ODPRI-UPRAVLJALNIK.bat` / `ZAZENI-NOCNI.bat`,
  Start do prekinitve, pavza/nadaljevanje, takojšnji Stop z lastnim procesnim drevesom,
  računi/prijava/status, trajna vrsta, shranjene odločitve in izpisi. Zaprtje strani ne ustavi
  dela. Strežnik samo na 127.0.0.1; mutacije zahtevajo lokalni Origin in sejni žeton.
- Po koncu paketa naslednji izvedljivi korak iz projektnega plana. Brez osemurne/fiksne
  iteracijske meje v načinu continuous; če ni izvedljivega dela, čaka do 1 h ali novega cilja.
  Kvota menja native profil; ko ni prijave, čaka. Login dogodki delujejo tudi med tekom.
- Uporabnik izrecno pooblastil samostojno odločanje tudi pri odprtih Q. Navodilo v vsakem
  pozivu, AGENTS in protokolu; vprašanje/izbrani odgovor/razlog v UI in ODLOCITVE.md.
  Full native CLI dostopi brez potrditev samo za ta proces, brez globalnih/UAC sprememb.
  Dejanski manjkajoči podatki/prijave ostanejo omejitve, sprejemnih meril ni dovoljeno obiti.
- AI sme pripraviti/registrirati nove scenarije za controller, ne zamenjati obveznih ID-jev.
  Nove skripte vključene v SHA-256. Raziskovalni zaključek zahteva obstoječe dokumente in
  nespremenjene merilne vhode brez odprtih obveznih testov. Motnje meritev čakajo in ponovijo
  isto celico. Prekinjena celica se ne šteje za uspeh, prejšnje ostanejo shranjene.

**Preverjanje:** 40/40 testov (prejšnjih 29 + 11 avtonomija/API/preklic), tudi resničen
proces s podprocesom in nepovezan proces, ki ostane živ; HTTP start/pause/resume/stop/restart,
Host/Origin/žeton, dokazila, registracija scenarijev in profile dogodki. Brskalniško potrjeni
Start, pavza, Stop in dodajanje cilja na ločenem sintetičnem projektu. Prava Codex in Claude 1
poziva z novo shemo/dovoljenji prek naročniških prijav uspešna, brez orodij/sprememb repoja.
Claude 2 po loginu in pravi Minecraft cikel ostajata integracijska preverba.

**Naslednji korak:** odpreti frontend, native login Claude 2 v drugi račun (Claude 1 in Codex potrjena),
nato Start ob prostem odklenjenem PC. Prva naloga ostaja pripravljeni A/B K3, nato K5 in M5-V
po aktualnem planu. Navodila: [overnight/README.md](../../overnight/README.md).

**Spremembe obnašanja moda:** nobene. **Minecraft meritve:** nobene.

### 2026-10-20 (99) — OPS.1: nočni upravljalnik testov in AI iteracij

**Paket:** OPS.1 (lokalno razvojno orodje) · **Stanje:** implementirano in krmiljenje preverjeno;
**pravi Minecraft/AI cikel še nepreverjen**, Claude native prijavi čakata uporabnika.

**Narejeno:**
- `nocni-run.ps1`, `ZAZENI-NOCNI.bat` in `overnight/`: trajna vrsta, ločeni delovni procesi,
  nadaljevanje zaključene celice po prekinitvi upravljalnika, strukturirane AI odločitve,
  Codex/Claude native CLI, predaja rezultatov med profili in čakanje na kvoto. Ni API ključev
  ali branja/kopiranja prijavnih žetonov. Native prijava se preveri pred AI klicem.
- Začetna lokalna vrsta vsebuje samo že pripravljeni K3 (unit/build/package + idle-200/500,
  oči 0/1, po 3 ponovitve). AI ne more odstraniti obveznih testov ali zaključiti s starimi
  rezultati. SHA-256 vhodov prepreči mešanje različnih verzij. K3 ni označen kot opravljen.
- Pred klientom: aktivno lokalno namizje, 120 s brez vnosa, brez drugih Java procesov in
  CPU ≤ 20 %. Vnos med serijo pomeni neveljavno meritev. Globalni Windows mutex prepreči dva
  upravljalnika; procesna zahteva zadrži spanje brez trajne spremembe power nastavitev.
- Navodila in meje: [`overnight/README.md`](../../overnight/README.md). Lokalne poti/stanje/native
  profili so git-ignored. Ponovljiv urnik ni nameščen; uporabnik ročno zažene 8-urno okno.

**Preverjanje:** **29/29** Node testov: parserji ponudnikov, kvote/failover/reset, nespremenljivi
obvezni testi, meje iteracij/ponovitev, izhodni kode/timeout, Windows mutex, literalni PowerShell
argumenti (presledki, šumniki, `$()`) in resnični ločeni sintetični proces po izhodu upravljalnika.
`init`, `doctor`, suhi zagon ter Node/PowerShell sintaksa uspešni. Pravi Minecraft, Gradle build
in modeli pri tej postavitvi niso bili zagnani. Codex ChatGPT auth potrjen; Claude Code 2.1.282
najden v VS Code, `auth status` vrne brez prijave.

**Naslednji korak:** uporabnik izvede `login claude-1` in `login claude-2`, nato prvi pravi
`run --hours 8` (ali `--test-only`) med odsotnostjo. Preveriti nočni desktop/zaklep in prvo
pravo integracijo. Prihodnji K5/M5-V scenariji še niso v katalogu; manjkajoča merila vrnejo
blokado za pregled. Nadaljnji vrstni red M5 in odprta Q so nespremenjeni.

**Spremembe obnašanja moda:** nobene. **Minecraft meritve:** nobene.

### 2026-10-20 (98) — M4.0: raziskava izvedbe M4 (gibanje in letenje)

**Paket:** M4.0 · **Stanje:** **zaključeno** (raziskava po §1a, brez produkcijske kode in brez zagonov
Minecrafta — uporabnik je delal na računalniku).

**Narejeno:**
- Koda letenja in navigacije: CNPC `FlyingMoveHelper`/`EntityNPCFlying`, vanilla 1.12 `PathNavigate`,
  `PathNavigateFlying`, `PathFinder`, `FlyingNodeProcessor`, `EntityGhast`, `EntityFlyHelper`; za primerjavo
  1.21.1 (`FlyingPathNavigation`, `FlyingMoveControl`, `PathNavigation`, `PathFinder`) in CustomNPC+ (`master`,
  `ai/pathfinder/`). Pregled vseh meritev gibanja (M2.3, M2.7, M2.5c, M5.6, M5.9–M5.11, M7.8, M7.10) z ločitvijo
  izmerjenega od ocenjenega. Zunanji viri (ARLR, Pufferfish, Lithium, SVO/Warframe, Theta\*, bug tracker).
- Zapis [`raziskave/M4.0-gibanje-in-letenje.md`](../raziskave/M4.0-gibanje-in-letenje.md); `03-FAZE` M4 prepisan
  (D-038: M4.1/M4.4/M4.9/M4.12 zavrženi ali združeni, M4.5 in M4.11 pogojna, nova M4.16 scenarij in M4.17 vidna
  črta, merljiv izhodni kriterij); R2 v `02-ZAHTEVE` dopolnjen; README; Q31; priporočilo k Q6.

**Ugotovitve:**
- **Leteči NPC sledi poti brez preverbe ovir** [K]: vanilla `PathNavigateFlying.isDirectPathBetweenPoints` vedno
  vrne `true`, `pathFollow` skoči na zadnjo točko iste višine (tudi za vogalom), nato `isNotColliding` → `WAIT`.
  Obtičanje ob vodoravnem vogalu je [H] — M2.3 (zid čez vso širino, pot čez vrh) ga ne more videti.
- **`FlyingMoveHelper` = vanilla `GhastMoveHelper`** [K]: sunek vsake 4 ticke, po prihodu do ~3,5 bloka drsenja
  ([H] model). Časovna zaščita vozlišča je za leteče izklopljena (`getAIMoveSpeed()` = 0) [K], zato razlaga
  R-P1b s to zaščito odpade.
- **Cilj dlje od `NpcNavRange` vrne `null`, ne delne poti** [K] (`findPathOptions` zavrne sosede > 32 od cilja) —
  M4.10 razširjen na vmesni cilj.
- **CustomNPC+ ima isto iskanje kot vanilla 1.12**; uporabne so ideje okoli sledenja in krmiljenja (žarek ≤ 6
  blokov, hitrost, zavoj, višinska meja, spust brez poti). Port odpade.
- Kopensko: edini izmerjen strošek je sledenje poti (34,5 % boja-500 po S14b) → M4.13 (S15) ostane najbolj podprt.

**Spremembe obnašanja:** nobene. **Meritve:** nobene.

**Naslednja seja:** A/B K3 (`audit/m5k3-ab.ps1`, ko računalnik ni v rabi), nato po M5 vrstnem redu. Od M4 se lahko
vrineta M4.14b (formacije, samo zagon) in M4.16 (scenarij letenja, brez produkcijske kode). Odgovor na Q31 ni nujen
pred M4.16/M4.10/M4.17.

### 2026-10-19 (97) — M5-K K3: oči iz display lista (koda, A/B jutri)

**Paket:** M5-K K3 · **Stanje:** **delno** — koda, testi in preverba pikslov narejeni, **A/B ni pognan**
(uporabnik potrebuje računalnik; meritev 20. 10.). `RwEyesList` je zato privzeto 0.

**Narejeno:**
- Razčlenitev profila M5-K.1 (idle-500): 80 % časa `LayerEyes` je `Tessellator.draw` (`glDrawArrays` 54 %,
  `glVertexPointer` 23 %), menjave stanja in `setupFogColor` skupaj < 15 %.
- 1:1 prenos `LayerEyes` (7881e50; bajtkoda 8/8 enakih; `LayerEyesBaselineTest`).
- `rework/render/EyesList` (49bd247): ob prvem srečanju ključa (tip, vzorec, barva, sijaj pri živem NPC-ju,
  debelina in barva obrvi) se isti `Tessellator.draw` posname v display list (`glDrawArrays` z razrešenimi
  vrednostmi, kot vanilla chunki brez VBO) znotraj `glPushAttrib(GL_CURRENT_BIT)`; nato `callList`.
  `GlStateManager` uskladen (`color(1,1,1,1)` pred, `resetColor()` po, kot `WorldVertexBufferUploader`).
  Utrip in OptiFine po originalu; LRU 256. `EyesListTest` (4). Stikalo `RwEyesList`.
- Harness `EyesCheck` (`k-run -Oci 1 -PreveriOci`, merili K13/K14): idle-200, serije svet, vzorci
  (60 ključev), utrip — vse še v povečavi ×8 (projekcija); **0 razlik** barve in globine (povečava pokrije
  341.191 pikslov, druga barva oči spremeni 511), utrip gre po originalu. `audit/m5k3-preverba.out`.
- Testi 306/0 + testOriginal 40/0, `verify-package` PASS (71 vnosov).

**Kje je obstal / naslednji korak (natančno):** pognati `audit/m5k3-ab.ps1` (idle-200 in idle-500, `-Oci 0/1`,
3 + 3; ~40 min; **med meritvijo se miške nad oknom Minecrafta ne premika** — prvi poskus 19. 10. je padel
na K5, kamera 6,9°; skripta zdaj ponovi tudi padec K5). Če sta razpona ločena → `RwEyesList` privzeto 1
(D-032; dev `CustomNpcs.cfg` ostane 0), vrstici v registrih (spremembe obnašanja, meritve), zapis
`docs/meritve/2026-10-2?-M5-K-K3-oci-display-list.md`, K3 v `03-FAZE` prečrtan. Če ne → zavrženo kot K14.

**Ugotovitve:**
- Hitri zagon idle-200 (1 + 1, brez ponovitev) je bil neuporaben: dva zapuščena procesa `find /` sta
  pojedla CPU (p95 ~12 ms namesto ~7). Pravilo merjenja: pred A/B preveri tudi ne-java procese z velikim CPU.
- Oči so pri kameri M5-K le nekaj pikslov na NPC; preverba pikslov brez povečave je skoraj slepa
  (49 pikslov občutljivosti), zato povečava ×8.

**Spremembe obnašanja:** nobene (`RwEyesList=0`).

**Naslednja seja:** A/B K3 (zgoraj), nato K5 (LOD). Brez meritev je lahko vmes **M4.0** (raziskava M4).

### 2026-10-19 (96) — M5-K K4: napisi NPC-jev iz display lista

**Paket:** M5-K K4 · **Stanje:** **zaključeno** (`RwLabelCache` **privzeto 1**, D-032).

**Narejeno:**
- 1:1 prenos `RenderNPCInterface` (0abb22e; bajtkoda 26 metod: 18 enakih, 8 razlik v obliki, 0 za pregled;
  `RenderNPCInterfaceBaselineTest`), nato trije klici `drawString` v `renderLivingLabel` → `rwDrawLabel`.
- `rework/render/LabelCache`: ob drugem izrisu istega niza (barva, Unicode, bidi) posname ukaze v display list
  (`GL_COMPILE_AND_EXECUTE`) in jih nato predvaja; predpomnilnik `GlStateManager` (alfa, barva, tekstura) se
  pred snemanjem in po predvajanju uskladi. Original za prvi izris, podrazrede `FontRenderer`, OptiFine,
  `§k`/`§n`/`§m`; listi se pobrišejo ob ponovnem nalaganju virov; LRU 1.024. `LabelCacheTest` (2).
- Harness `LabelCheck` (`k-run -Napisi 1 -PreveriNapise`, merili K11/K12): idle-200, 174 napisov v dometu,
  serije ime testnega sveta in barvne kode/šumniki **0 razlik** barve in globine (drug naslov 2.817 pikslov).
- A/B 3 + 3 (`audit/m5k4-ab.ps1`): idle-200 p50 6,556 → **6,100 ms (−7,0 %)**, idle-50 2,164 → **2,025 (−6,4 %)**,
  p95 −6,1 / −7,6 %, razponi ločeni. [zapis](../meritve/2026-10-19-M5-K-K4-napisi-display-list.md)
- Testi 300/0 + testOriginal 38/0, `verify-package` PASS (70 vnosov).

**Ni narejeno in zakaj:** napis bližje kot 4 bloke (drugi prehod brez globinskega testa) gre po isti poti,
v prizorišču ga ni in ni posebej preverjen s pikslom.

**Ugotovitve:** list prihrani ~⅓ stroška napisov (klici JNI); ostanek je izvajanje ~34 znakov v takojšnjem
načinu v gonilniku. Večji dobitek bi zahteval VBO/atlas za vse napise (ni v obsegu K4).

**Spremembe obnašanja:** nobene vidne; `RwLabelCache=1` privzeto (isti ukazi GL, 0 razlik pikslov). Dev
`CustomNpcs.cfg` drži 0, zato scenariji merijo original.

**Naslednja seja:** **K3** (oči, 3,5 µs na NPC, 10–14 % render niti); Q30 še odprto.

### 2026-10-19 (95) — M5-K K14b: pokrivalo brez prozornih štirikotnikov

**Paket:** M5-K K14b · **Stanje:** **zaključeno** (koda pod stikalom, privzeto 0; vklop Q30).

**Narejeno:**
- **Q29 odločeno** (uporabnik: "nared kar misls da bi blo bolje" → priporočilo K14b pred K4).
- 1:1 prenos `LayerHeadwear` (1903772, bajtkoda 5/5), nato `rework/render/HeadwearQuads`, `HeadwearMask`,
  `HeadwearSkip`, `Model2DRenderer.rwCompileFiltered`, stikalo `RwHeadwearSkip`; harness `HeadwearCheck`
  (`k-run -Pokrivalo 1 -PreveriPokrivalo`, merili K9/K10).
- Preverba pikslov: 7 serij × 200 NPC-jev (skin, vzorec, 5 pravih skinov CNPC) **0 razlik** barve in globine.
- A/B 3 + 3 (`audit/m5k14b-ab.ps1`): idle-500 p50 11,493 → **8,595 ms (−25 %)**, p95 −24 %; idle-200 6,521 →
  **5,317 (−18 %)**; razponi ≤ 3,6 %, ločeni. [zapis](../meritve/2026-10-19-M5-K-K14b-pokrivalo-brez-prozornih.md)
- Testi 297/0 + testOriginal 37/0, `verify-package` PASS (69 vnosov).

**Ni narejeno in zakaj:** privzeti vklop — ni *dokazano* enako (fragment točno na robni črti lahko v
originalu vzame sosednji teksel), zato po D-007 → **Q30**.

**Ugotovitve:** strogo enaka maska (z robom enega teksla) pri standardni postavitvi skina ne izpusti
skoraj ničesar, ker so ob klobuku neprozorni teksli glave, telesa in rok (in čez `GL_REPEAT` noge) —
A/B v šumu, zavržena. Pravilo merjenja: pri dokazu "izpusti nevidno geometrijo" preveri sosednje teksle
v pravi postavitvi teksture, preden izmeriš.

**Spremembe obnašanja:** nobene privzeto (`RwHeadwearSkip=0`).

**Naslednja seja:** **K4** (imena: ~8 µs na napis, 20 % render niti pri 200 NPC-jih v dometu), nato K3.

### 2026-10-19 (94) — M5-K K14 zavržen: en list pokrivala ne pomaga, geometrija pa

**Paket:** M5-K K14 · **Stanje:** **zaključeno — zavrženo** (izid meritve, ne nedokončano delo).

**Narejeno:**
- Prenos 1:1 `Model2DRenderer` in `ModelHeadwear` v `src/patch` (`b896cfc`; `primerjaj-bytecode` 0 za pregled,
  `ModelHeadwearBaselineTest`). Ostane.
- Poskus `RwHeadwearList` (`03ab3bc`): 6 delov pokrivala v en display list (način 1 kliče liste otrok, 2 raven
  list), `HeadwearState` spremlja stanje otrok. Dokaz v klientu `k-run -PreveriPokrivalo` (`HeadwearCheck`, FBO,
  načini 0/1/2/0, skin + neprozorna testna tekstura): **0 razlik** barve in globine; med igro 0 prevezav.
- A/B 3 + 3 (`audit/m5k14-ab.ps1`): idle-500 p50 11,589 → 11,423 ms (razpona se prekrivata), idle-200
  6,538 → 6,590 → **zavrženo**, koda odstranjena v `4370184` (vrne se z `git revert 4370184`).
- Zgornja meja `HeadWearType=0` (3 ponovitve): idle-500 p50 **11,589 → 8,907 ms (−23 %)**, p95 −22 %.
- `dev/tools/skini-pokrivalo.js`: 41 % od 239 humanoidnih skinov CNPC ima povsem prozorno pokrivalo, prazni so
  60 % delov in 70 % trakov ekstruzije. [zapis](../meritve/2026-10-19-M5-K-K14-pokrivalo-en-list.md)

**Ni narejeno in zakaj:** privzeti vklop (D-032) — ni dobitka nad šumom.

**Ugotovitve:**
- **Napačna premisa K14:** profil z enim listom pokaže pokrivalo še vedno 26,1 % — čas je v gonilniku pri
  izvajanju **816 oglišč na NPC** (6 × 34 štirikotnikov `renderItemIn2D`), ne v ~30 klicih GL. Pravilo merjenja
  dodano v Znane omejitve (arhiv §5).
- Skin fixture `humanmale/steve.png` ima prozoren klobuk: pokrivalo stane 23 % sličice in ne nariše niti
  enega piksla → nov kandidat **K14b** (izpusti geometrijo prozornih tekselov, ista slika) → **Q29**.

**Spremembe obnašanja:** nobene (stikalo poskusa odstranjeno; `verify-package` 68 vnosov: +2 razreda prenosa 1:1).

**Meritve:** enakost pikslov, A/B idle-500/idle-200 (3 + 3), zgornja meja (3), profil idle-500 z enim listom.

**Naslednja seja:** po odgovoru na Q29 **K14b** (priporočeno) ali **K4** (imena) po Q28.

### 2026-10-18 (93) — M5-K.1 zaključen: baseline in profil klienta

**Paket:** M5-K.1 · **Stanje:** **zaključeno**.

**Narejeno:**
- **Napaka merilnega pogoja M5-K.0:** harness je nastavil `hideGUI`, kar v 1.12 izklopi vsa imena NPC-jev
  (`canRenderName` → `isGuiEnabled`); fixture imajo `ShowName 0` + naslov. Nov ključ `hideGui` (privzeto 0),
  `k-run -SkrijGui`, K6 preveri nastavitev. idle-200 p50 4,78 → **6,57 ms** z imeni.
- Baseline originala, 5 celic × 3 ponovitve (`audit/m5k1-baseline.ps1`), T1–T6 in K0–K8 zelena:
  p50 idle-50/200/500 2,15 / 6,57 / 11,57 ms, boj-200/500 5,78 / 12,55 ms; FPS 458 / 150 / 83 / 170 / 78.
- `k-run -Jfr` (JFR render niti) + `jfr-povzetek.js --native`: deluje, a **38 vzorcev** render niti v 14 s —
  vzorčevalnik JDK 8 nativnim nitim da en vzorec na 20 ms za vse. Zato nov **`rework/diag/StackSampler`**
  (vzorci samo render nit vsako 1 ms, ~665/s, ~100 µs na vzorec) + `dev/tools/skladi-povzetek.js`, `k-run -Vzorci 1`.
- Profili idle-50/200/500 in boj-500: NPC-ji ~89–90 % render niti pri 200/500; model 31–38 %, **pokrivalo
  (`LayerHeadwear`, 6 `Model2DRenderer` delov) 19–26 % = 6,7 µs/NPC**, ime + naslov 20,5 % pri 174 napisih
  (~8 µs na napis), oči 10–14 % (3,5 µs/NPC); lastni čas 45–56 % v `nglCallList`. Hipoteza 09 (klici na NPC,
  CPU + gonilnik) potrjena. [zapis](../meritve/2026-10-18-M5-K.1-baseline-profil.md)
- Testi 287/0 + testOriginal 35/0 (`StackSamplerTest` izključen iz testOriginal kot ostali testi harnessa),
  `verify-package` PASS (66 vnosov).

**Ni narejeno in zakaj:** različice videza (oklep, oblački, delci, URL skini) — profil ne kaže, da bi
spremenile vrstni red; dodajo se v paketu, ki jih spreminja. JFR zagoni idle-200/500 in boj-500 izpuščeni
(nadomestil jih je vzorčevalnik).

**Ugotovitve:** pokrivalo (`HeadWearType=1`) ni bilo v nobenem K-paketu raziskave 09 → nov paket **K14**
(6 delov v en display list, ista slika). Vrstni red D-035 se ne obrne (K5 in K10/K13 sta potrjena), a
K14/K4/K3 so ceneje in brez spremembe videza → **Q28**.

**Spremembe obnašanja:** nobene (harness in vzorčevalnik obstajata samo z `-Drwharness`).

**Meritve:** baseline + 4 profili (zgoraj).

**Naslednja seja:** po odgovoru na Q28 K14 (priporočeno) ali K5 (D-035 brez spremembe).

**Dopolnitev 19. 10.:** Q28 odločeno — **K14 → K4 → K3 pred K5**. K14 začet in prekinjen na zahtevo
uporabnika, **koda ni spremenjena**. Prebrano: `ModelHeadwear extends ModelScaleRenderer` ima 6 otrok
`Model2DRenderer`; vsak `render` = `push` + `postRender` (rotacijska točka + koti, v konstruktorju
konstantni) + `callList` (en `Tessellator.draw`, 34 štirikotnikov) + `pop`; starš `ModelScaleRenderer.render`
pokliče še svoj prazen list (nima kock). Noben od teh razredov še ni v `src/patch` → najprej 1:1 prenos
(ločen commit, `primerjaj-bytecode.js`), nato en list pod stikalom. Odprto za dokaz enakosti: ali kdo
med igro spreminja `showModel`/`isHidden`/kote otrok (`LayerHeadwear.preRender` nastavi samo
`config = null` in `bipedHeadwear.isHidden`).

### 2026-10-16 (92) — M5-K.0 zaključen: klientski harness

**Paket:** M5-K.0 · **Stanje:** **zaključeno**.

**Narejeno:**
- Nedokončano delo iz delovne kopije (harness brez commita) dokončano in pognano v svetu: `k-run.ps1`,
  `rework/diag/{ClientHarness,HarnessJob,FrameStats}`, `ponovitve-run.ps1 -Scenarij klient`,
  [scenarij](../scenariji/M5-K.0-klient.md). Brez `-Drwharness` se obnašanje ne spremeni.
- Celice idle-50/200/500 in boj-200, K0–K8 zelena; [zapis](../meritve/2026-10-16-M5-K.0-harness.md).
- Popravki: locale (decimalne vejice v navodilu kamere), boj K3 (`expectNpcMin`), boj K4 (delež upodobljenih/živih).
- Testi 285/0 + testOriginal 35/0, `verify-package` PASS (66 vnosov).

**Ni narejeno in zakaj:** različice videza (oklep, ime, oblački, delci, URL skini) in tri ponovitve —
prestavljeno v M5-K.1 (baseline), harness jih podpira prek fixtur in `ponovitve-run.ps1`.

**Spremembe obnašanja:** nobene. **Naslednja seja:** M5-K.1 (baseline in profil render niti).

### 2026-10-16 (91) — M5.8 zaključen: skrajšane potrditve K3

**Paket:** M5.8 · **Stanje:** **zaključeno**.

**Narejeno:**
- Uporabnik je izbral skrajšan obseg (**Q27 → (a)+(b)+(c)**). Zaganjalnik `audit/m58-skrajsano.ps1`
  (4 serije, 3 ponovitve, 60 + 180 s, 08:10–09:40), vse T1–T6 zelene. MSPT p95 mediana:
  S-boj 1500 **22,02**, O-boj 1000 **22,54**, S-ned 700 **21,50**, O-ned 500 **22,54** — vse ≤ 25 ms.
- Kapaciteta pri 25 ms: idle 350 → 3500 (10×), boj 1000 → 1500 (1,5×), nedosegljiva 500 → 700 (1,4×).
  [zapis](../meritve/2026-10-16-M5.8-kapaciteta.md)

**Ni narejeno in zakaj:** pari ob pragu za boj/nedosegljivo, potrditve pri 50 ms in `-Razprseno` —
izpuščeni po Q27 (≈10 h stroja); meja 50 ms ostane iz lestvice.

**Spremembe obnašanja:** nobene. **Naslednja seja:** M5-K.0 (harness klienta), izdaja čaka na Q22.
- Po meritvah: **Q21 → D-037** — `RwPathNegCache` privzeto 1, M5.2 zavržen. Testi 274/0 + testOriginal, `verify-package` PASS (66 vnosov). Dev `CustomNpcs.cfg` drži stikalo na 0, zato O-meritve ostanejo original.
- Opažanje: rezultati `testOriginal` imajo čas 08:42, med serijo O-boj-1000 je torej v tem repozitoriju tekel Gradle. p95 serije (22,54, razpon 2,3 %) ni slabši od lestvice (23,6), vpliv je verjetno zanemarljiv.

### 2026-10-15 (90) — M5.8: lestvica končana, meja prisilnih chunkov

**Paket:** M5.8 · **Stanje:** **delno** — lestvica (prva stopnja) končana, potrditve K3 za **idle
narejene**, ostale ustavljene ob 14:08 na zahtevo uporabnika (stroj ni bil več prost).

**Narejeno:**
- Lestvice **boj** (O 1000–3000, S 1000–3500), **nedosegljiva** (O 400–1200, S 500–1400) in
  **S-idle 5000–8000** (zaganjalnik `audit/m58-lestvica.ps1`, 14. 10. je obstal pred začetkom).
  Vsi pragovi obkroženi (K2), [zapis](../meritve/2026-10-15-M5.8-lestvica.md):

| varianta | p95 ≤ 25 ms O | S | ≤ 50 ms O | S |
|---|---|---|---|---|
| idle | 350–400 | 3500–4000 | 500–600 | 7000–8000 |
| boj | 1000–1500 | 1500–2000 | 1500–2000 | 2500–3000 |
| nedosegljiva | 500–600 | 700–800 | 800–1000 | 1200–1400 |

  Idle ~10–13× več NPC-jev, boj ~1,5× (pri istem N −38 % p95), nedosegljiva ~1,4×.
- **Napaka merilnega pogoja popravljena:** S-boj 2500+ je padel na P3/P4, ker je
  `DiagChunkLoader.MAX_CHUNKS_PER_WORLD` (400) med merjenjem stal na meji (razbežani borci, chunki se
  ne sproščajo, NPC-ji izven nabora se raztovorijo). Meja → **2000**, novo merilo **P4c** v `perf-run.ps1`.
  S-boj 2000–3500 in O-boj 2500 ponovljeni z novo mejo (max 363–562 chunkov). Testi 274 + 35 zeleni,
  `verify-package` PASS (66 vnosov, nespremenjeno — razred je nov, ne iz originala).
- Zaganjalnik potrditev `audit/m58-potrditve.ps1` (serije `ponovitve-run.ps1`, 3 ponovitve,
  120 + 300 s, ~30 min na serijo). **Idle potrjen (K3), vse T1–T6 zelene, razpona para se ne
  prekrivata:** O-idle 350 p95 20,97 (20,45–20,97), O-idle 400 27,26 (27,26–27,79), S-idle 3500
  24,64 (24,12–25,17), S-idle 4000 28,31 (26,21–29,36) → pri 25 ms **350 → 3500 stoječih NPC-jev
  (10×)**. S-boj 1500 prekinjen po 1/3 (22,02).
- Zaganjalnik je imel napako: `$S` (stikala) in zančna `$s` sta v PowerShellu ista spremenljivka;
  prva serija je zato padla na razčlenjevanju, preden je karkoli zagnala. Popravljeno (`$stikalaS`).

**Ni narejeno in zakaj:**
- Potrditve K3 za boj in nedosegljivo (8 serij, ~4 h), pari ob pragu 50 ms, `-Razprseno` za idle —
  stroj je bil potreben drugje; zaganjalnik ima narejene serije odstranjene. Obseg čaka na **Q27**.

**Ugotovitve:**
- Original je v boju **cenejši** kot v idlu (boj-1000 23,6 ms, idle-400 29,9 ms): idle original
  plača iskanje tarč, ki ga S1/S2 skoraj odstranita; v boju preostanek nosijo vanilla sledenje
  poti, gibanje in trki. Za večji skok v boju bodo potrebne spremembe obnašanja (S15, activation
  range, omejitev ponovnega iskanja) — odločitev uporabnika; pred tem nov profil JFR optimiziranega
  boja (zadnji je z 8. 10., pred S14b/S14c/M5.16).

- **Plan: nov paket M4.15** (D-036) na zahtevo uporabnika — izbira navigacijskega ozadja
  *vanilla* / *Baritone* v GUI NPC-ja s prikazom dejanskega ozadja. Izbira na NPC-ju v kodi že
  obstaja (NBT `RwNavBackend`, `/rwnav`, ponovni priklop po `updateTasks`), manjka samo GUI. Pred
  tem M4.15a: lestvica boj/nedosegljiva z Baritonom, velikosti, leteči, jahači. Kritična ocena:
  Baritone je boljši v kakovosti poti, cena pri velikem N ni izmerjena, model gibanja je igralčev
  → ni kandidat za privzeto ozadje vseh NPC-jev pred meritvijo.

**Spremembe obnašanja:** nobene (meja je merilna, `rework/diag`).
**Meritve:** lestvica M5.8 (kratke celice, ne končni rezultat paketa).

**Odprto vprašanje Q27 (obseg potrditev M5.8):** po polnem protokolu ostane ~14 h stroja (boj in
nedosegljiva pri 25 ms ~4 h, pari pri 50 ms ~8 h, `-Razprseno` ~2 h). Predlog: (a) potrdi samo N
tik pod pragom, (b) meja 50 ms samo iz lestvice, (c) okno 60 + 180 s — skupaj ~2–3 h. Uporabnik
je rekel, da dolge teste poženeta jutri zgodaj; izbire še ni.

**Naslednja seja:** po odgovoru na Q27 poženi `audit/m58-potrditve.ps1` (ostane S-boj 1500/2000,
O-boj 1000/1500, S-ned 700/800, O-ned 500/600), zapiši K3 v `meritve/…-M5.8-kapaciteta.md`.

---

### 2026-10-14 (89) — M5.8: kapaciteta, idle lestvici (delno, ustavljeno)

**Paket:** M5.8 · **Stanje:** **delno** — ustavljeno na zahtevo uporabnika (»ok ustavi to nimam zdj
tega cajta«); nič ni pokvarjeno, nadaljuje se po isti zasnovi.

**Narejeno:**
- **Zasnova** [scenariji/M5.8-kapaciteta.md](../scenariji/M5.8-kapaciteta.md): O = vsa stikala 0 (izrecno
  poslana, ne po predpostavki o configu), S = vsa po D-032 privzeto vklopljena
  (`-RwTarget 2 -RwPath 1 -RwPathMemo 1 -RwCollide 1 -RwBlink 1 -RwData 1 -RwMove 1`); lestvica s
  kratkimi celicami (60 + 120 s, ena ponovitev) samo obkroži prag, številko da potrditev s polnim
  oknom in tremi ponovitvami. Merila K1–K5.
- **`perf-run.ps1` do 8000 NPC-jev** (commit 428ce18). Stara meja 1000 je imela razlago »mreža bi
  segla čez prostor scenarija«, ki ne drži (ovire testnega sveta so pri x ≤ 20, mreža pri x 48..72
  raste v +z). Popravljeno, kar bi pri velikem N tiho merilo drugo: pas čiščenja čez cel obseg celice,
  umiritev po spawnu 3 + N/250 s, merilo S1 kot invarianta (v idle ni iskanja poti, zato je prej
  padlo v vsaki idle celici). Gostota mreže nespremenjena.
- **Lestvici idle** (kratke celice, P1–P9 zelena v vseh, vsi NPC-ji tikajo vsak tick):

| N | O p95 (ms) | O p99 | O alok. MB/s | | N | S p95 (ms) | S p99 | S alok. MB/s |
|---:|---:|---:|---:|---|---:|---:|---:|---:|
| 250 | 10,22 | 11,01 | 77 | | 1000 | 5,24 | 6,42 | 36 |
| 300 | 14,68 | 15,99 | 113 | | 1500 | 9,18 | 10,49 | 55 |
| 350 | **20,97** | 22,54 | 159 | | 2000 | 12,32 | 14,16 | 73 |
| 400 | **29,88** | 31,98 | 215 | | 2500 | 16,78 | 18,87 | 90 |
| 500 | **48,23** | 52,43 | 341 | | 3000 | 19,92 | 20,97 | 113 |
| 600 | **71,30** | 75,50 | 471 | | 3500 | **23,59** | 25,17 | 131 |
| | | | | | 4000 | **25,17** | 27,26 | 150 |

  **Obkroženo (K2):** O ≤ 25 ms med **350 in 400**, ≤ 50 ms med **500 in 600**; S ≤ 25 ms med
  **3500 in 4000** (4000 nad mejo le za 0,17 ms — potrditev lahko prag premakne). **~10× več stoječih
  NPC-jev pri istem p95.** O-500 iz lestvice (48,2) se ujema z baselineom M2.6 (44,0; 43,0–45,1) v
  okviru krajšega okna.
- S-idle 5000: spawn in prisilno nalaganje uspešna (P2: npc = 5000, 60 chunkov, 0 zavrnjenih),
  meritev prekinjena pred koncem → **številke ni**.

**Ni narejeno (za nadaljevanje):**
- S-idle **5000–8000** (prag 50 ms za S je nad 4000).
- **boj in nedosegljiva** za O in S — po dogovoru s sejo »Nadaljuj development« [202625] jih poganja
  ona (S-boj 1000–3500, O-boj 1000–3000, S-ned 500–1400, O-ned 400–1200; zaganjalnik
  `audit/m58-lestvica.ps1`, **ni komitiran**, požene se brez parametrov, poti so absolutne; izid v
  `audit/m24-perf-*` in `audit/m58-*`) — **ni pognano, zaganjalnik pripravljen**.
- `-Razprseno` za idle na pragu.
- **Potrditve pragov** (polno okno, tri ponovitve) in zapis `meritve/…-M5.8-kapaciteta.md`.

**Ugotovitve:**
- Razpršitev **polja** v PowerShellu stikala prenese pozicijsko (`@S` z `-RwTarget`… je pristalo na
  `ChunkRadius`); za stikala `perf-run` iz drugega ukaza je treba razpršiti **razpredelnico**.
- Dve seji na isti veji sta hkrati prevzeli M5.8; zaklep `.scenarij.lock` je preprečil prekrivanje
  zagonov, delo pa je bilo usklajeno s sporočili (delitev zgoraj).

**Spremembe obnašanja:** nobene. **Meritve:** zgornji lestvici (kratke celice, ne rezultat paketa).

**Naslednja seja:** nadaljuj M5.8 po [zasnovi](../scenariji/M5.8-kapaciteta.md): S-idle 5000–8000,
boj/nedosegljiva (O in S), nato potrditve s tremi ponovitvami.

---

### 2026-10-14 (88) — odgovor Q26 (M5-V)

**Paket:** dokumentacija odločitve (D-035) · **Stanje:** končano

**Narejeno:**
- **Q26: po priporočilu** → `RwFarLos` privzeto 0: strežnik pošilja nadomestke vseh NPC-jev v dosegu brez
  preverbe vidne črte (kot vanilla znotraj 64 blokov), imena in naslovi nikoli; operater lahko vklopi
  `RwFarLos=1`. Dopisano v D-035 kot točka (4), v `02-ZAHTEVE.md`, `03-FAZE.md` (M5-V.2) in raziskavo.

**Spremembe obnašanja:** nobene. **Meritve:** nobene.

**Naslednja seja:** nespremenjeno **M5.8**, nato M5-K.0.

### 2026-10-14 (87) — odgovori Q23–Q25 (M5-V)

**Paket:** dokumentacija odločitev (D-035) · **Stanje:** končano

**Narejeno:**
- **Q23: da** → po sprejemu M5-V.8 privzeto vklopljeno (strežnik `RwFarView=1`, klient `RwFarView=2`);
  izjema od D-007 po odločitvi uporabnika. Do sprejema stikala ostanejo 0, `dev/run` vedno 0.
- **Q24: strežnik** → cilj je dedicirani strežnik. Zapisana posledica: 24 chunkov zahteva
  `view-distance=24`; 1.12 nima ločene razdalje simulacije, zato je to 49² = 2.401 tickajočih chunkov na
  igralca proti 441 pri 10 (5,4×) — vanilla cena, ki je M5-V ne odpravi (uspavanje oddaljenih NPC-jev je v
  R5 prepovedano). M5-V.1 zato meri na dediciranem strežniku s klientom pri `view-distance` 10 in 24.
- **Q25: po priporočilu** → vrstni red M5-K.0 → M5-K.1 → K5 → M5-V.1 … M5-V.8 → K12 → ostali K-paketi;
  K13/K10 po M5-V.8 ponovno oceniti.
- Novo **Q26** (`RwFarLos`, preverba vidnosti): drugi del Q23, ki v chatu ni bil zastavljen.
- Posodobljeni `01-ARHITEKTURA.md` (D-035), `02-ZAHTEVE.md`, `03-FAZE.md`, raziskava M5-V.0, README.

**Spremembe obnašanja:** nobene (koda M5-V še ne obstaja).

**Meritve:** nobene.

**Naslednja seja:** nespremenjeno **M5.8**, nato M5-K.0.

### 2026-10-14 (86) — M5-V.0: raziskava vidnosti NPC-jev na daljavo (impostorji do 24 chunkov)

**Paket:** M5-V.0 (D-029, D-034) · **Stanje:** končano (raziskava, brez produkcijske kode)

**Narejeno:**
- Zahteva uporabnika: NPC-ji vidni do **24 chunkov** kot nekaj pikslov velika slika, brez vpliva na
  performance, "nujen modul na koncu klientske optimizacije". Zapisano kot **D-034** in nov modul
  **M5-V** v `03-FAZE.md` (paketi M5-V.1–M5-V.8, izhodni kriterij M5 razširjen), razširitev R5 v
  `02-ZAHTEVE.md`, [raziskava](../raziskave/M5-V.0-daljna-vidnost.md).
- **Vzrok "8 chunkov" najden v kodi [K]:** CNPC registrira NPC-je s `tracker(64, 3, true)`, strežnik
  NPC-ja pošlje samo znotraj kvadrata ±64 blokov (`EntityTrackerEntry.isVisibleTo`) = 4 chunki v vsako
  smer. Klient bi jih narisal dlje (`isInRangeToRenderDist`: 64 × rob AABB × clamp(RD/8, 1, 2.5) = do
  160 blokov). Samo klientski LOD zato dometa ne poveča.
- Ime "efekta": LOD; za teksture **mipmapping** (entitetne teksture ga v 1.12 nimajo, `GL_NEAREST`),
  za celo entiteto **impostor/billboard**. Pri 24 chunkih je NPC visok ~3,6 px, širok ~1,2 px (izračun).
- Variante: A (dvig dometa sledilnika) samo kot primerjalna celica — 33× površina, GZIP spawn NBT na NPC,
  polna klientska entiteta; **B (nadomestki po kanalu `rwfar` + impostorji v enem klicu) izbrana**;
  C (NPC-ji iz nenaloženih chunkov) in D (samo klient) zavrženi.
- Zunanji vzori: I See You Over There!, Entity Render Distance Extender, Spyglass Entity Distance
  (varianta A), **Render Distant Players** (lasten paket + pomanjšan nadomestek, najbližji vzor B), Bobby,
  FarPlaneTwo, Unreal Impostor Baker.
- Kritika M5-K: K5 razdeljen (L1 ostane v K5, impostor gre v M5-V), harness M5-K.0 razširjen s
  prizoriščem "daleč", K13 ni pogoj za M5-V, vrednost K10 po M5-V pade.

**Ni narejeno in zakaj:**
- Nobene meritve: klientski harness (M5-K.0) še ne obstaja; vse številke zmogljivosti v raziskavi so
  ocene **[H]**, zamenja jih M5-V.1 in M5-V.8.
- Brez produkcijske kode (raziskava je samostojen paket, §1a protokola).

**Ugotovitve:**
- NPC **brez hitboxa** (širina `1.0E-5`) ima povprečni rob AABB 0,6 in izgine že na **38 blokih** pri
  RD 8 (96 pri RD ≥ 20) — obstoječa posebnost originala; M5-V.6 jo odpravi mimogrede. Ni zapisana kot
  hrošč za takojšen popravek (sprememba videza).
- `RenderWorldLastEvent` je za izris impostorjev neprimeren (kliče se po `disableFog` in po prosojni
  plasti); pravo mesto je konec prehoda entitet v `RenderGlobal.renderEntities` (sidro `"blockentities"`,
  coremod D-031).

**Spremembe obnašanja:** nobene.

**Meritve:** nobene.

**Naslednja seja:** nespremenjeno **M5.8** (v teku), nato M5-K.0. Odprta Q23 (privzeto vklopljeno?),
Q24 (en igralec ali dedicirani strežnik), Q25 (M5-V takoj po M5-K.1?).

### 2026-10-14 (85) — M5.16: `World.getCollisionBoxes` brez odvečne poizvedbe entitet

**Paket:** M5.16 (D-031) · **Stanje:** končano

**Narejeno:**
- **Prvi pravi poseg v vanilla bajtkodo.** `rework/core/CollisionBoxesPatch` v
  `World.getCollisionBoxes(Entity, AABB)` zamenja `INVOKEVIRTUAL getEntitiesWithinAABBExcludingEntity`
  z `INVOKESTATIC CollisionGate.entitiesForCollision`. Sklad je pred klicem enak
  (`world`, `entity`, `box`), prejemnik postane prvi argument, vrnjeni tip isti — zato je dovolj
  `ClassWriter(0)` in nič drugega v metodi se ne spremeni.
- `CollisionGate` (odločitev `decide`, števci, načini 0/1/2), `CollisionClasses` (ali razred prepisuje
  `getCollisionBoundingBox()` / `getCollisionBox(Entity)`, refleksija z imeni MCP **in** SRG, izid
  predpomnjen), `CollisionWatcher` (štetje prispevajočih entitet po svetovih prek
  `IWorldEventListener`), ukaz `/rwmove`, stikalo `RwMoveBoxes`, `perf-run -RwMove N` z merili V1–V3.
- **Enakost:** v vanilli 1.12.2 prispevajo trkalno škatlo samo `EntityBoat`, `EntityMinecart` in
  `EntityShulker` (iskanje po viru Forge 14.23.5.2847); CNPC NPC-ji nobene od obeh metod ne prepišejo.
  V svetu: **945.162 primerjav v boju-500, 0 neujemanj**; idle-500 1,25 M klicev, poizvedba ne najde
  nikogar (NPC-ji stojijo blok narazen, kvader `grow(0.25)` se soseda ne dotakne).
- **A/B idle-500, pet ponovitev na serijo:** MSPT povp 2,787 → **2,540 ms (−8,9 %)**, p50 2,556 →
  2,294, p95 4,194 → **3,867**, p99 4,850 → 4,719, µs/NPC 5,47 → 4,98, alokacija 20,7 → 20,0 MB/s —
  **vse z ločenima razponoma**; 100 % klicev preskočenih (505 na tick). Serija B je tekla druga, na
  bolj ogretem stroju, torej vrstni red deluje proti njej.
- **boj-500 (3 + 3):** ločitve ni (šum 15–17 %), poslabšanja tudi ne — mediane v B rahlo nižje.
  Tudi tam se preskoči 100 % klicev, a je prihranek v senci sledenja poti (48–54 % ticka).
- 15 novih testov (`CollisionGateTest`, `CollisionClassesTest`, `CollisionBoxesPatchTest`), skupaj
  **274** `:test` in **35** `:testOriginal` zelenih. Testi posega tečejo nad **pravimi bajti**
  `net.minecraft.world.World`: klic poizvedbe izgine, na njegovem mestu je natanko en
  `INVOKESTATIC` na vrata, število vseh drugih klicev v metodi je nespremenjeno; drugi poseg, tuj
  razred in pokvarjeni bajti pustijo bajte nespremenjene.
- `verify-package` **PASS** (66 = 65 razredov + manifest), dimni test M0.5 **20/20** (dve novi
  preverbi: poseg uspel, svet spremljan), in ponovno zelen z `RwMoveBoxes=1` v configu `dev/run`
  (ta je nato vrnjen na 0, da baseline in scenariji merijo original — D-032).
- `javap -v` na zapakiranem jarju: `CollisionBoxesPatch`, `RwTransformer` in `RwCoreMod` se
  sklicujejo samo na JDK, log4j, ASM, `IClassTransformer` in `IFMLLoadingPlugin`. Ime vrat je v
  posegu **navaden niz**, zato se `CollisionGate` v fazi coremoda ne naloži (kar coremod naloži
  takrat, ni več mogoče transformirati).
- [zapis meritve](../meritve/2026-10-14-M5.16-trki-brez-poizvedbe.md), opis stikala v
  [scenariju M2.4](../scenariji/M2.4-obremenitve.md).

**Ni narejeno in zakaj:**
- **Zagon na pravem (ne-dev) Forge strežniku** — isto kot pri M5.15 (Q22). Od tega paketa je to bolj
  pereče, ker jar zdaj **privzeto** spreminja vanilla bajtkodo in so prav imena SRG tisto, česar dev
  okolje ne pokaže. Neuspel poseg je varen (iste bajte, vrata niso klicana, `/rwmove` pokaže
  `poseg=0`), izgubi se samo korist.
- Enakost v svetu s **čolnom, minecartom ali shulkerjem** ni pognana: v merilnih prizoriščih jih ni.
  Pot je pokrita s testi (`CollisionClassesTest`) in konzervativnim štetjem; preverba v svetu bi
  sodila k M5.8 ali k preverbi v klientu (Q20).

**Ugotovitve:**
- **Tri ponovitve niso bile dovolj, pet je bilo.** Razlika (−8,9 %) je manjša od razpona serije
  (8,6–9,0 %); prva serija po tri ponovitve je dala isto smer, a bi po protokolu končala kot
  "v šumu" (ločila se je samo alokacija). To je natanko primer, za katerega je M2.5c narejen — in
  opomin, da je pri 3 ponovitvah "v šumu" lahko tudi *premalo podatkov*, ne samo *ni koristi*.
- Profil JFR je poizvedbi pripisal 11,8 % CPU, izmerjeni dobitek MSPT je 8,9 % — v istem pasu,
  nekoliko pod pripisom (škatle blokov in `GetCollisionBoxesEvent` ostanejo).
- Poseg velja za **vse entitete**, ne le NPC-je, in se izklopi sam, kjer ni dokazano varno.

**Spremembe obnašanja:**
- `World.getCollisionBoxes` preskoči poizvedbo entitet, kadar je dokazano, da nihče ne bi prispeval
  škatle. Isti izid. Stikalo `RwMoveBoxes`, **privzeto 1** (D-032).

**Meritve:** idle-500 A/B 5 + 5 in boj-500 A/B 3 + 3, enakost (način 2) v idle in boju —
[zapis](../meritve/2026-10-14-M5.16-trki-brez-poizvedbe.md).

**Naslednja seja:** **M5.8** (kapaciteta proti baselineu, izhodni kriterij M5) ali **M5.2** po
odgovoru na Q21; pred izdajo Q22.

---

### 2026-10-11 (84) — M5.15: infrastruktura vanilla posegov (coremod v CNPC jarju)

**Paket:** M5.15 (D-031) · **Stanje:** končano v dev okolju; produkcijski zagon odprt (Q22)

**Narejeno:**
- `rework/core/RwCoreMod` (`IFMLLoadingPlugin`, anotacije `@MCVersion("1.12.2")`, `@Name`,
  `@SortingIndex(1001)`) in `rework/core/RwTransformer` (`IClassTransformer`, **vrne iste bajte**,
  šteje razrede in prvega zapiše v dnevnik). `SortingIndex` 1001 pomeni, da transformer teče za
  Forgeovim deobfuskacijskim transformerjem: v produkciji vidi imena **SRG**, v dev okolju **MCP**;
  katero velja, coremod prebere iz `runtimeDeobfuscationEnabled`.
- `buildPatchedMod` doda v original manifest `FMLCorePlugin` in `FMLCorePluginContainsFMLMod` — to je
  **edini vnos originala, ki ni bitna kopija**. `verify-package.ps1` zato manifest preveri vnos za
  vnosom in zahteva, da je razlika natanko ta dva vnosa (izguba ali sprememba obstoječega vnosa je
  napaka). `devRuntimeMod` ima ista vnosa, v dev okolju pa coremod prižge `-Dfml.coreMods.load`,
  ker je dev jar na classpathu in ne v `mods/`.
- Stanje gre v sistemske lastnosti (`rwcore.loaded`, `rwcore.deobf`, `rwcore.classes`), ne v statična
  polja: coremod naloži LaunchWrapper pred modi, zato je statično stanje iz kode moda lahko druga
  instanca. Ukaz `/rwcore` bere lastnosti.
- Dimni test M0.5 (`.\smoke-server.ps1`): **vseh 18 preverb zelenih**, dve novi (`RWCORE coremod
  nalozen`, `RWCORE transformer aktiven`). NPC preživi save in restart kot prej. Zagon strežnika
  0,92 / 0,78 / 0,72 s proti 0,95 / 0,93 s brez coremoda — dodatni transformer v verigi ni viden.
- `verify-package` PASS (66 = 65 razredov + manifest). Na zapakiranem jarju preverjeno z `javap -v`
  (constant pool), da se coremod in transformer sklicujeta **samo** na JDK, `IFMLLoadingPlugin`,
  `IClassTransformer` in log4j — na noben razred CNPC ali vanille. To je nujno: kar coremod naloži v
  svoji fazi, ni več mogoče transformirati.
- Build zelen (test 259), [zapis](../meritve/2026-10-11-M5.15-coremod-infrastruktura.md).

**Česa paket ne dokazuje:**
- **Zagon zapakiranega jarja na pravem (ne-dev) Forge strežniku.** Dev okolje teče deobfuskirano in
  reobfuskiranega jarja ne more naložiti, pravega Forge strežnika pa projekt nima (blokada M0.8 /
  Q1). Statično je preverjeno vse, kar se da. → **Q22**.

**Ob tem:** `smoke-server.ps1` je dobil isto ogrevanje standardnega vhoda kot `perf-run.ps1` (prvi
ukaz po zagonu se izgubi, glej Q18), ker bi sicer dimni test padel na `setworldspawn`. Q18 ostane
odprto za preostalih devet skript.

**Spremembe obnašanja:** nobene (transformer vrne iste bajte; v igri ni vidne razlike).

**Naslednja seja:** M5.16 (prvi pravi poseg: `Entity.move` brez odvečne poizvedbe entitet za trke)
ali M5.2 po odgovoru na Q21.

### 2026-10-10 (83) — M5.14 / S16: senčna polja `EntityDataManager`

**Paket:** M5.14 · **Stanje:** končano, privzeto vklopljeno (D-032)

**Narejeno:**
- NPC hrani svoje štiri ključe (`Attacking`, `IsDead`, `Walking`, `FactionData`) v navadnih poljih,
  osveženih v `notifyDataManagerChange`, in bere iz njih (`isAttacking`, `isKilled` → tudi
  `isEntityAlive`, `isWalking`, `getFaction`). `DataShadow` (`rework/entity`), stikalo `RwDataShadow`
  (0/1/2), ukaz `/rwdata`, `perf-run -RwData N` z merili D1–D3, test `DataShadowTest` (4).
  Build zelen (test 259), `verify-package` PASS (65).
- Enakost po viru vanille **[K]**: vrednost se spremeni samo prek `EntityDataManager.set` (strežnik in
  klient) ali `setEntryValues` (klient, iz paketa), **obe** metodi pa po zapisu pokličeta
  `entity.notifyDataManagerChange(key)` (`EntityDataManager:161` in `:314`). Začetne vrednosti postavi
  `register`, ki kavlja ne kliče, zato jih `entityInit` nastavi izrecno. Ključ se primerja z `equals`,
  ker klient iz paketa ustvari nove instance `DataParameter` z istim id-jem.
- **Enakost v svetu (način 2):** idle-500 **24.050.849** primerjav, boj-500 **27.946.381**, skupaj
  **52,0 M, 0 neujemanj**. Mimogrede izmerjeno: **~20 branj na NPC na tick** (9.814 / 500 v idle,
  11.265 / 500 v boju) — to pojasni 11,6 % profila za `EntityDataManager.get`.
- **A/B (3 + 3 na varianto):** idle-500 MSPT povp **3,023 → 2,863 ms (−5,3 %)**, p50 2,818 → 2,687,
  p95 4,588 → **4,325**, µs/NPC 5,92 → 5,60 — najslabša ponovitev B je boljša od najboljše A pri
  povprečju, p50, p95 in µs/NPC; p99 se **ne** loči (rep idle ticka je avtosave). boj-500 (90 s +
  180 s): mediane kažejo −15 %, a razpona se prekrivata (15,8 % in 20,2 %) — **smer ugodna, nad šumom
  ni**. [zapis](../meritve/2026-10-10-M5.14-sencna-polja.md)
- Po D-032 privzeto `RwDataShadow=1`; `dev/run` config ostane izrecno 0.

**Ugotovitve:**
- Vanilla ključi (`getHealth` prek `isEntityAlive`, `getFlag`) ostajajo nedotaknjeni; to je več kot
  polovica tistih 11,6 % in jih brez coremoda (M5.15/M5.16) ni mogoče doseči.
- Idle-500 je danes padel s 3,02 na 2,86 ms povprečnega ticka; v baselineu M2.6 je bil 44 ms p95,
  danes je p95 4,33 ms.

**Spremembe obnašanja:** nobene (isti izid; privzeto 1 po D-032). **Meritve:** M5.14 (tabela Meritve).

**Naslednja seja:** M5.15 (coremod infrastruktura, D-031) ali M5.2 po odgovoru na Q21; M4.0 vmes.

### 2026-10-10 (82) — M5.17 / S14c: pomnjenje tipa vozlišča med iskanjem poti

**Paket:** M5.17 (nov, dodan po M5.6) · **Stanje:** končano, privzeto vklopljeno (D-032)

**Narejeno:**
- `PathSearchMemo` (`rework/nav`) + `RwPathNavigateGround.getPathToPos` odpre okno pomnjenja
  (`RwWalkNodeProcessor.beginMemo`) čez **cel** `super.getPathToPos`. Pomnjena metoda je ista kot pri
  S14b (`getPathNodeType(IBlockAccess, x, y, z)`), le okno je daljše: med enim `findPath` je
  `blockaccess` ista instanca `ChunkCache`, entiteta ena, iskanje sinhrono in blokov nihče ne
  postavlja **[K]**. Stikalo `RwPathSearchMemo` (0/1/2), ukaz `/rwpath memo N`,
  `perf-run -RwPathMemo N` z merili S1/S2, test `PathSearchMemoTest` (4) + nov test
  `NodeTypeMemoTest.resetBrezBrisanjaVrednostiJeSeVednoPrazen`.
- **Enakost v svetu (način 2):** 180.188 iskanj, **239.529.126** primerjanih ocen, **0 neujemanj**
  (~190× večji vzorec od dokaza za S14b); poleg tega 0 neujemanj v vseh šestih zagonih načina 1.
- **Cena iskanja (en zagon na način, isto merjenje):** 184,0 → **144,9 µs** (−21 %), 9,690 → 7,541
  ms/tick; ocen na iskanje 1.408, od tega 671 izračunov, **zadetkov 52,3 %**. Iz tega sledi, da je
  ocena tipa vozlišča ~40 % stroška iskanja.
- **A/B celih zagonov (3 + 3 na varianto, štiri serije):**
  `nedosegljiva`-500 MSPT povp **17,959 → 15,253 ms (−15 %)**, p50 17,826 → 15,204, p95 22,02 →
  **18,874**, p99 24,64 → 22,02, µs/NPC 35,77 → 30,36, alokacija 291,5 → **162,8 MB/s**, ticki > 25 ms
  48 → 24; najslabša ponovitev B je boljša od najboljše A pri vseh štirih veličinah.
  `boj`-500 (90 s + 180 s) **nevtralen**: povp 9,064 → 9,424, razlika 0,36 ms globoko v razponu obeh
  vej (15 % in 24 %); zadetkov je tam 35 %, iskanje pa 2,45 % ticka.
  [zapis](../meritve/2026-10-10-M5.17-pomnjenje-med-iskanjem.md)
- Po D-032 privzeto `RwPathSearchMemo=1`; `dev/run` config ostane izrecno 0.

**Popravek med delom:** `NodeTypeMemo.reset()` je brisal tabelo vrednosti, kar je O(velikosti tabele)
ob vsakem klicu — pri 1024 režah in kratkem iskanju (3,5 µs v boju) bi to stalo več kot samo iskanje.
Veljavnost rež določa števec generacije, zato brisanje za pravilnost ni potrebno, pri enum vrednostih
pa ni kaj zadržati (nov parameter `clearValues`, S14b ga dobi tudi). Dodana je **zgornja meja 1024 rež**
(`setMaxCapacity`): brez nje bi pri ~1.400 ocenah na iskanje tabela zrasla na 4.096 rež, kar je pri
500 NPC-jih ~50 MB zadržanega pomnilnika. Vse meritve v zapisu so na popravljeni kodi.

**Ugotovitve:**
- Z M5.6/S5 sta to dva neodvisna vzvoda na isto celico: S14c −15 % (brez spremembe obnašanja,
  privzeto vklopljen), S5 −19 % (sprememba obnašanja, čaka Q21).
- Ostanek stroška iskanja (~60 %) je A\* knjigovodstvo, alokacija `PathPoint`-ov in `ChunkCache`;
  kdor bo hotel več, mora napasti te.
- `ponovitve-run.ps1` dovoli samo eno celico na serijo (`-JsonPath`), zato gre A/B z dvema variantama
  kot štiri ločene serije — zapisano v scenariju.

**Spremembe obnašanja:** nobene (isti izid; privzeto 1 po D-032). **Meritve:** M5.17 (tabela Meritve).

**Naslednja seja:** M5.14 (S16) ali M5.2 po odgovoru na Q21; M4.0 (raziskava M4) je lahko vmes.

### 2026-10-10 (81) — M5.6 / S5: negativni predpomnilnik iskanja poti

**Paket:** M5.6 (polovica: S5) · **Stanje:** končano, odločitev pri uporabniku (Q21)

**Narejeno:**
- `NegativePathCache` (`rework/nav`) + spomin na zadnje neuspelo iskanje v navigatorju
  (`RwPathNavigateGround`, ena reža, brez alokacije): po iskanju brez cele poti isti NPC za (skoraj)
  isto tarčo `ttl` tickov ne išče — `getPathToPos` vrne `null`. Stikalo `RwPathNegCache` (0/1/2),
  ukazi `/rwpath neg|negttl|negtol|negdelne`, `perf-run -RwPathNeg N [-RwPathNegTtl T] [-RwPathNegDelne]`
  z merili N1/N2, test `NegativePathCacheTest` (5). Build zelen (test 250), `verify-package` PASS (65).
- **Velikost spremembe obnašanja (način 2, `nedosegljiva`-500):** neuspeh = `null` ali delna pot,
  20 tickov → 195.402 primerjav, **66,8 %** neujemanj; neuspeh = samo `null`, 20 tickov → 106.996
  primerjav, **58,6 %**; samo `null`, 4 ticki → **0 preskokov** (NPC-ji isto tarčo iščejo na ~7 tickov,
  ne na 3). Trajanja, pri katerem bi bil S5 »skoraj enak«, torej ni.
- **A/B (3 + 3, `nedosegljiva`-500, pravilo samo `null`, 20 tickov):** MSPT povp **17,854 → 14,462 ms
  (−19 %)**, p50 17,826 → 14,418, p95 22,02 → **18,35**, p99 24,64 → 20,45, µs/NPC 35,57 → 28,79,
  alokacija 292,5 → **135,2 MB/s**, ticki > 25 ms 45 → 10; preskočenih 17,46 iskanj na tick (35 %).
  Najslabša ponovitev B je boljša od najboljše A pri vseh štirih veličinah ticka.
  [zapis](../meritve/2026-10-10-M5.6-negativni-predpomnilnik.md)

**Ugotovitve:**
- S5 odstrani petino ticka in polovico alokacij v pathološkem prizorišču, a **ni enakovreden
  originalu** → po D-007 privzeto 0 in **Q21** za uporabnika (priporočilo: da, ker je drenjanje ob
  oviri tudi vizualno slabše).
- Napoved M5.0, da ponovitve poganja `shouldExecute` (vsak 3. tick), je popravljena: prevladuje
  `updateTask` → `tryMoveToEntityLiving` vsakih 4–10 tickov, ker AI task **teče** z delno potjo.
- Nov kandidat **M5.17 (S14c)**, ki obnašanja **ne** spremeni: S14b pomni ocene tipa vozlišča samo med
  `isDirectPathBetweenPoints`, isto pomnjenje čez cel `findPath` pa napada 190 µs na iskanje
  neposredno. Dodan v `03-FAZE.md`.
- Druga polovica M5.6 (deljenje poti med NPC-ji) ostaja odprta; po S5 in S14c bo imela manjšo osnovo.

**Spremembe obnašanja:** S5 je sprememba obnašanja pod stikalom, privzeto 0 (D-007).
**Meritve:** M5.6 (tabela Meritve).

**Naslednja seja:** M5.17 (S14c) ali M5.2 (proračun iskanj) po odgovoru na Q21.

### 2026-10-10 (80) — M5.9: scenarij nedosegljive tarče

**Paket:** M5.9 · **Stanje:** končano

**Narejeno:**
- Nova varianta `perf-run.ps1 -Variants nedosegljiva`: kot `boj`, le da je okoli skupine B **pas ograje**
  (`minecraft:fence`), debel 3 bloke. Ograja je pathfindingu zaprta (`PathNodeType.FENCE`), visoka pa
  1,5 bloka, zato žarek med očmi NPC-jev (~1,62) gre nad njo — tarča je **vidna in nedosegljiva** hkrati.
  Merila U0 (ograja je res v svetu, `testforblock` štirih strani in proste notranjosti), U1 (NPC-ji imajo
  tarčo), U2 (nihče ni ranjen), U3 (merjenje je štelo).
- Instrumentacija `PathSearch` (`rework/nav`) + `RwPathNavigateGround.getPathToPos`: izmeri in razvrsti
  **vsak** klic iskanja poti (vsi klici gredo skozi `getPathToPos`) — *iz obstoječe poti* / *brez poti* /
  *cela* / *delna*. Ukaz `/rwpath iskanje 1`, `perf-run -RwPathIskanje`, test `PathSearchTest` (3).
  Build zelen (test 245), `verify-package` PASS (65).
- **Izid (ena celica na varianto v istem zagonu, 500 NPC-jev, 300 s):** iskanje poti je **2,45 % ticka pri
  `boj` in 51,18 % pri `nedosegljiva`**; eno iskanje **3,5 proti 180,5 µs**; MSPT povp 9,23 → **17,73 ms**,
  p95 10,75 → 22,02; alokacija 91 → **292,6 MB/s**; celih poti 219.335 proti **0**.
  Pri 50 NPC-jih 71,1 % ticka. [zapis](../meritve/2026-10-10-M5.9-nedosegljiva-tarca.md)
- Razlaga je v vanilla kodi **[K]**: `PathFinder.findPath` se ustavi po **200 iteracijah** A\*, zato
  neuspelo iskanje porabi ves proračun, uspelo pa konča takoj (cilj je sosednji blok). `null` vrne, ko ni
  našel nič bližjega od začetka, sicer delno pot.

**Ugotovitve:**
- **M5.2, M5.6 in S5 dobijo podlago** (pogoj iz M5.0 §5 je izpolnjen: 51 % ni šum). **S5 (negativni
  predpomnilnik) je najcenejši prvi korak**; je pa sprememba obnašanja → po D-007 stikalo in privzeto
  original (D-032 tu ne velja).
- Celico `nedosegljiva` je treba vključiti v M5.8 (kapaciteta brez nje je optimistična).
- Scenarij je imel tri padle zasnove, vse zapisane v `M2.4-obremenitve.md`: ograja tesno ob B (udarec
  skozi njo, doseg 2), odmik 3 bloke (obe skupini prideta do ograje in sta spet 2 bloka narazen) in
  **napačno ime bloka** (`minecraft:oak_fence` je ime iz 1.13; v 1.12.2 je `minecraft:fence`). Tretja je
  pol ure tiho merila navaden boj — zato je zdaj merilo U0, ki ograjo preveri v svetu.

**Spremembe obnašanja:** nobene (samo merjenje; privzeto izklopljeno). **Meritve:** M5.9 (tabela Meritve).

**Naslednja seja:** M5.6/S5 (negativni predpomnilnik poti) ali M4.0 (raziskava M4).

### 2026-10-10 (79) — M5.13: S7, prejemniki utripa oči po `playerEntities`

**Paket:** M5.13 · **Stanje:** končano

**Narejeno:**
- `ModelEyeData` prevzet v `src/patch` kot bitna kopija (ločen commit, `ModelEyeDataBaselineTest` (4) teče
  tudi v `:testOriginal` proti nespremenjenemu binarju).
- `AssociatedPlayers` (`rework/net`): prejemniki paketa `EYE_BLINK` se poiščejo z obhodom
  `world.playerEntities` z istim izborom kot vanilla poizvedba (`NOT_SPECTATING` = `!isSpectator()`,
  `addedToChunk` + `isChunkLoaded`, presek škatel), namesto pregleda **450 chunkov** na iskanje. Telo
  pošiljanja je nespremenjeno prepisano iz originala; `Server.java` ni prevzet (CFR opozorilo na
  `writeNBT`/`readNBT` — prevod celotne mrežne plasti zaradi ene vrstice je nesorazmerno tveganje).
  `RwWorldAccess` dobi `isChunkLoaded` (isti vzorec kot `pathListener`, M3.1).
- Stikalo `RwBlinkRecipients` (0/1/2), ukaz `/rwblink` (način, števci, `cas`, `poskus`),
  `perf-run -RwBlink N`, `-RwBlinkCas`, `-RwBlinkAB a,b` z merili B1–B5, AB3, AB4.
  Build zelen (test 242), `verify-package` PASS (65).
- **Enakost:** `AssociatedPlayersTest` (6) na **200.000 naključnih legah** preveri tisti del dokaza, ki ga ni
  mogoče prebrati iz ene vrstice — da je vanilla izbor chunkov in y-rezov konservativen (meje chunkov
  ±0,001, lege pod y = 0 in nad 255); model vanille je prepisan iz vira Forge 14.23.5.2847. V svetu:
  način 2 pri idle-500 **3.939 primerjav, 0 neujemanj**, `/rwblink poskus` 4 × **320 primerjav, 0 neujemanj**.
- **A/B iskanja v istem zagonu** (izmenjujoča se okna po 15 s, kot M5.11): iskanje prejemnikov
  **55,42 → 0,12 µs** (0,002×), iz ticka izgine **0,167 ms** = 5,7 % ticka; razpon iskanj na tick med
  načinoma 0,2 % (AB4). Ujema se z napovedjo profila JFR (6,0–6,5 %).
- **A/B celih zagonov** (3 + 3, idle-500, `RwTarget 2`): MSPT povp **2,824 → 2,570 ms**, p50 2,621 → 2,359,
  p95 4,325 → **3,932**, p99 4,719 → 4,325, µs/NPC 5,54 → 5,05, alokacija nespremenjena (20,7 MB/s).
  Najslabša ponovitev B je boljša od najboljše A pri **vseh štirih** veličinah ticka — prvi paket M5, kjer
  ločitev drži povsod. [zapis](../meritve/2026-10-10-M5.13-prejemniki-utripa-oci.md)
- Po D-032 privzeto `RwBlinkRecipients=1`; `dev/run` config ostane izrecno 0.

**Blokada paketa (najdena in popravljena):**
- Scenarij trikrat ni mogel niti postaviti sveta: **prvi ukaz po zagonu serverja se izgubi**. Vhod gre
  PowerShell → `cmd.exe` → `gradlew.bat` → gradle (ki zaradi `org.gradle.jvmargs` tudi z `--no-daemon`
  forka build JVM) → javaexec serverja; gradlov posredovalnik vhoda vrstico, zapisano preden se priključi,
  pogoltne — do serverja pride samo konec vrstice in ta odgovori `Unknown command`. Zato `perf-run` po
  `Done (` ogreje vhod z `list`, dokler server ne odgovori (prvi poskus pade po 5 s, drugi uspe).
  Prej je uspevalo zato, ker je bil zapis vedno dovolj pozen; danes se server zažene v 0,9 s.
- `-RwPathAB`/`-RwBlinkAB 0,1` brez narekovajev PowerShell poda kot niz `"0 1"`, zato se argument zdaj deli
  tudi po presledku (prej bi `-RwPathAB 0,1` padel na pretvorbi v `Int32`).
- Isti popravek še ni v ostalih desetih scenarijskih skriptah → **Q18**.

**Ugotovitve:**
- 3,01 iskanja na tick pri 500 NPC-jih se ujema s kodo: po utripu je še 20 tickov premora, zato je perioda
  140 + ~26 tickov (500/166 = 3,01), ne 500/140.
- Alokacija se ne spremeni: strošek je iskanje po chunkih, ne smeti (enako kot pri S4).
- Utrip oči ni več kandidat; preostanek idle-500 je vanilla `Entity.move` (M5.15/M5.16) in
  `EntityDataManager` (M5.14).
- Česa meritev ne pokrije: neprazen seznam prejemnikov v svetu (scenarij nima igralcev) → **Q20**.

**Spremembe obnašanja:** nobene (isti prejemniki; privzeto 1 po D-032). **Meritve:** M5.13 (tabela Meritve).

**Naslednja seja:** M5.9 (scenarij nedosegljive tarče) ali M4.0 (raziskava M4).

### 2026-10-09 (78) — M5.12: S4, `onCollide` brez opazovalca

**Paket:** M5.12 · **Stanje:** končano

**Narejeno:**
- `CollideSkip` (`rework/entity`): NPC brez vklopljenih skript preskoči poizvedbo v `onCollide`, ko na
  `WrapperNpcAPI.EVENT_BUS` ni poslušalca za `CollideEvent` (preverjeno ob vsakem klicu prek `ListenerList`).
  Stikalo `RwCollideSkip` (0/1/2), ukaz `/rwcollide` s poskusnim poslušalcem, `perf-run -RwCollide N` (K1–K3),
  `CollideSkipTest` (3). Build zelen (263), `verify-package` PASS (64).
- Skripte-500, način 2: 161.000 odločitev, nobena brez opazovalca (NPC-ji s skriptami ostanejo original).
- Idle-500 A/B (3 + 3): p95 5,24 → **3,93 ms**, p99 6,16 → **4,13**, povp 3,53 → 3,02, µs/NPC 6,85 → 5,84;
  K2/K3 zelena v vseh treh ponovitvah. [zapis](../meritve/2026-10-09-M5.12-oncollide-brez-opazovalca.md)
- Po D-032 privzeto `RwCollideSkip=1`; `dev/run` config izrecno 0.

**Ni narejeno in zakaj:**
- NPC-ji s skriptami (skripte ~15 % CPU v `onCollide`) niso pokriti: potreben bi bil vpogled v `ScriptContainer`
  (ali skripta ima `collide`) in obravnava `onNPCInit` po ponovnem nalaganju → kandidat S4b, ni v planu.

**Ugotovitve:**
- Povprečje −14 % se ujema s profilom (12 %), a je v razponu A (A je bil v tej seriji šumen, 20 %); repa sta jasno
  nad šumom.
- Dvakrat P9 zaradi Gradle builda `ladja_mod`; zaganjalnik je v istem procesu podedoval `$LASTEXITCODE` padle
  serije (lažen padec `testworld.ps1`) → serije zdaj vsaka v svojem procesu.

**Spremembe obnašanja:** nobene (isti izid; privzeto 1 po D-032). **Meritve:** M5.12 (tabela Meritve).

**Naslednja seja:** M5.13 (S7) ali M4.0 (raziskava M4).

### 2026-10-09 (77) — M5.11: S14b, pomnjenje tipa vozlišča v sledenju poti

**Paket:** M5.11 · **Stanje:** končano

**Narejeno:**
- `RwWalkNodeProcessor` (node procesor vsakega kopenskega NPC-ja, `RwPathNavigateGround.getPathFinder` kot vanilla) si
  med enim klicem `isDirectPathBetweenPoints` zapomni `getPathNodeType(x, y, z)` po poziciji (`NodeTypeMemo`); vanilla
  metoda se kliče nespremenjena. Stikalo `RwPathFollowCache` 1 = S14b, 2 = preverba, privzeto 0.
- Števci po načinu in ticki v vsakem načinu (`/rwpath ... poNacinu=`), `perf-run.ps1 -RwPathAB a,b -RwPathOkno N`
  (A/B v istem boju, merili AB1/AB2). Testi `NodeTypeMemoTest` (6), `PathFollowCacheTest` (+2).
- Enakost v svetu dvakrat: 618.004 in 627.601 primerjav, **0 neujemanj**; 3,03 ocene na izračun (model 3,07).
- A/B v istem boju (0/S14b/S14, okna 20 s, 3 ponovitve): µs na kandidata 0,37–0,41× originala, pathFollow
  −2,9 do −3,5 ms/tick (razpon originala 0,73); S14 −0,2 do −0,95 ms, v šumu.
- **S14 odstranjen** (`MemoBlockAccess`, `ChunkMemo`, `ChunkMemoTest`, prepis vanilla metode), načini preštevilčeni.
- A/B celih zagonov na končni kodi (3 + 3, `ab-boj.js` pogoj izpolnjen, odmik klicev ≤ 1,9 %): MSPT povp
  **8,73 → 5,88 ms**, p50 8,65 → 5,77, p95 9,96 → 7,21, µs/NPC 17,3 → 11,6, alokacija 89 → 58 MB/s.
  Build zelen (260), `verify-package` PASS (64). [zapis](../meritve/2026-10-09-M5.11-pomnjenje-tipa-vozlisca.md)

**Ugotovitve:**
- Boj-500, edina celica brez napredka po M5.0, je zdaj p50 5,8 ms proti 8,9 v baselineu M2.6.
- Šum boja se z S14b skoraj izgubi (razpon MSPT povp 0,52 → 0,07 ms): del na klic, ki ga je M5.10 označil za
  vir šuma, so bile ponovljene ocene [H].
- `pathFollow` ostane 34,5 % ticka; naslednji vzvod je S15 (M4.13, sprememba gibanja) ali S15b.
- Zaganjalnik je 4 ure čakal na obvisel `ladja_mod` proces (brez okna); uporabnik ga je dovolil ustaviti.

**Spremembe obnašanja:** nobene (S14b da isti izid; privzeto 0). **Meritve:** M5.11 (tabela Meritve).

**Odločitev uporabnika v isti seji:** Q17 → **D-032** — optimizacija z dokazano enakim izidom in izmerjeno koristjo
je privzeto vklopljena (D-007 velja še za spremembe obnašanja); `RwPathFollowCache` privzeto 1, `dev/run` config
ostane izrecno 0.

**Naslednja seja:** M5.12 (S4) ali M4.0 (raziskava M4).

### 2026-10-09 (76) — M5.10: delež sledenja poti v boju, neposredno

**Paket:** M5.10 · **Stanje:** končano

**Narejeno:**
- `RwPathNavigateGround`: `pathFollow` prepisan samo za merjenje (vedno `super`), ns okoli `pathFollow` in vsakega
  kandidata `isDirectPathBetweenPoints` v katerem koli načinu, histogram kandidatov na `pathFollow` (0…5+);
  `PathFollowCache` števci, `/rwpath cas 0|1` (privzeto izklopljeno), odgovor ima še ticke od `reset`.
  `PathFollowCacheTest` (3). Build zelen (`test` 225, `testOriginal` 31; test izključen iz `testOriginal` kot
  `ChunkMemoTest`), `verify-package` PASS (64).
- `perf-run.ps1 -RwPathCas`: delež povprečnega ticka v `pathFollow`/kandidatih, klici na tick, µs na klic,
  `rwpath.hist.0`–`5`, merilo M1, odtis `rwpathCas`. `dev/tools/ab-boj.js`: pogoj veljavnosti A/B boja.
- Serija boj-500 × 3 (`RwTarget 2`, `RwPath 0`), T1–T6 in P1–P9 zelena.
  [zapis](../meritve/2026-10-09-M5.10-delez-sledenja-poti.md)

**Ugotovitve:**
- `pathFollow` = **47,7 / 53,6 / 50,0 %** povprečnega ticka (4,0–4,9 ms od 8,0–9,2), skoraj ves v kandidatih.
  JFR (48 %) je imel prav za celoto, precenil je le iskanje chunka znotraj klica (S14).
- Na `pathFollow` vedno **0 ali 1 kandidat** in vsak uspe → napoved 5–8× ponovljenih ocen pri več kandidatih v
  boju ne velja; S14b zadošča za en klic.
- Klici na tick se med ponovitvami razlikujejo za 1,1–1,4 %, MSPT za 14 %, µs na kandidata za 24,5 % (p2 ima tudi
  največ alokacije) → šum boja je v **delu na klic** (geometrija boja, [H]), pogoj ±10 % klicev iz M5.0 je nujen,
  ne zadosten. A/B v M5.11 zato pri istem boju (preklapljanje načina med tekom, števci po načinu).
- Prvi poskus serije je P9 zavrnil (vzporedni testi `ladja_mod`); zaganjalnik je nato čakal 5 min mirnega stroja.

**Spremembe obnašanja:** nobene (merjenje privzeto izklopljeno, `pathFollow` vedno `super`).

**Meritve:** M5.10 (tabela Meritve).

**Naslednja seja:** M5.11 — S14b z A/B pri istem boju (ali M4.0, raziskava M4).

### 2026-10-08 (75) — M5.0: raziskava preostanka M5

**Paket:** M5.0 (D-029) · **Stanje:** končano

**Narejeno:**
- [Zapis raziskave](../raziskave/M5.0-preostanek-performance.md) z oznakami [K]/[M]/[H]/[W]: primerjava z
  baselineom, analiza vanilla sledenja poti, idle/skripte po profilu, kritika M5.1–M5.7 in S-seznama.
- `dev/tools/model-sledenje-poti.js`: model zank vanilla `isDirectPathBetweenPoints` (ravna tla,
  NPC 0,6 × 1,9) — ~1.100–1.140 branj blokov na klic, izmerjeno 1.110 (S14).
- Plan M5 prepisan (`03-FAZE.md`, D-030), R5 v `02-ZAHTEVE.md` dopolnjen s stanjem po meritvah, M4.13 dobi
  vhod S15, M6.6 prevzame M5.1. Q16.

**Ugotovitve:**
- Idle-500 p95 44,0 → 5,1 ms proti baselineu; **boj-500 je v razponu baselinea** (p50 7,3–8,9 proti 8,9 ms),
  torej edina celica brez napredka.
- ~90 % branj v `isDirectPathBetweenPoints` je prvo okno (3 × 3 stolpcev s kvadrom 3 × 2 × 3, ker vanilla
  podaljšano širino poda tudi v oceno kvadra). Isto mesto se oceni ~3× na klic in 5–8× na `pathFollow` z več
  kandidati → S14b. ~56 % alokacij boja (`BlockPos` v `getPathNodeTypeRaw`/`getPathNodeType`) je tu.
- Vanilla 1.21.1 (lokalni viri Forge 52.1.14) kopenskim mobom med sledenjem poti ne bere blokov
  (`followThePath` po razdalji in smeri, `canMoveDirectly` = false) in ima `PathTypeCache` (4.096 mest,
  invalidacija ob spremembi bloka). Pri nas bi bilo prvo sprememba gibanja → M4.13.
- Boj A/B je šumen zaradi poteka boja (baseline razpon p50 20 %, S14 B2 alokacija 36,7 proti 90 MB/s); nov
  pogoj veljavnosti: klici sledenja poti v obeh vejah ±10 %.
- `onCollide` v idle je skoraj samo poizvedba (dogodki ~2 % alokacij) → premisa S4 drži. Vanilla
  `Entity.move` (26–29 %) brez vanilla mixinov ni dosegljiv (Q16); Lithium ga za novejše verzije pospeši,
  za 1.12 portov ni.

**Spremembe obnašanja:** nobene. **Meritve:** nobene nove (model je izračun, ne meritev).

**Odločitev uporabnika v isti seji:** Q16 → **D-031** — vanilla/Forge posegi so dovoljeni, če so zapakirani v CNPC
jar (lasten coremod z ASM; MixinBooter kot zunanji mod in vgrajen Mixin zavrnjena zaradi odvisnosti oz. trkov v
modpackih). Dodana paketa M5.15 (infrastruktura coremoda) in M5.16 (`Entity.move` poizvedba entitet za trke).
Pričakovan dobitek M5.16 je zmeren (idle do ~12 % CPU strežniške niti, ki je že ~3 ms/tick), zato je za S14b, S4, S7.

**Naslednja seja:** M5.10 (meritev sledenja poti v boju), ali M4.0 (raziskava M4).

### 2026-10-08 (74) — M5-S S14 (delno) in D-029: raziskava pred vsakim milestonom

**Paket:** M5-S S14 · **Stanje:** delno — koda in dokaz enakosti narejena, učinek nedokazan

**Narejeno:**
- `rework/nav`: `RwPathNavigateGround` (navigator vsakega kopenskega NPC-ja, pri 0 kliče `super`),
  `MemoBlockAccess`, `ChunkMemo`, `PathFollowCache`, `/rwpath [0|1|2|reset]`; stikalo
  `RwPathFollowCache` (privzeto 0). `ChunkMemoTest` (4), build 253 testov zelen, `verify-package` PASS (64).
- `perf-run.ps1 -RwPath N` (merili R1/R2 za način 2).
- **Enakost v svetu:** boj-500, način 2: 616.276 primerjav, **0 neujemanj**.
- **A/B boj-500:** A 8,96 ms povp (razpon 0,30); B 8,84 in 7,85, tretja ponovitev B neveljavna (P9:
  med celico je tekel uporabnikov `ladja_mod`). Izid **nedokazano**, stikalo ostane 0.
  [zapis](../meritve/2026-10-08-M5-S14-sledenje-poti.md)
- **D-029** (odločitev uporabnika): pred vsakim milestonom paket M?.0 — raziskava izvedbe (koda,
  meritve, zunanji viri), ki lahko pakete spremeni, zavrže ali doda; plan je delno nastal s šibkejšimi
  AI modeli. `05-SEJA-PROTOKOL.md` §1a, `03-FAZE.md` (pravilo, M4.0, M5.0), README.

**Ugotovitve:**
- Profil JFR je strošek iskanja chunka precenil; pravi strošek je ~1.110 branj blokov na klic
  (prekrivajoča se okna `isSafeToStandAt`) → kandidat S14b.
- Uporabnik je dovolil, da seja med meritvijo zapre tuje java procese, nato je zagon zaustavil;
  serija B ni bila ponovljena in noben proces ni bil zaprt.

**Spremembe obnašanja:** nobene (privzeto 0).

**Meritve:** S14 enakost in delni A/B (tabela Meritve).

**Naslednja seja:** M5.0 — raziskava preostanka M5 (S14/S14b, M5.1–M5.7, S4, S7), po D-029.

### 2026-10-08 (73) — M5-S P1: profil JFR strežniške niti pri 500 NPC-jih

**Paket:** M5-S P1 (manjkajoči merilni korak iz [09](../09-PERFORMANCE-RAZISKAVA.md)) · **Stanje:** končano

**Narejeno:**
- `perf-run.ps1 -Jfr`: v merilnem oknu vsake celice `jcmd JFR.start settings=profile` / `JFR.stop`
  (posnetek v `audit/jfr/`, ni v gitu), merila J1–J3, odtis `jfr = 1`. Povzetki šele po ustavitvi
  serverja, da `jfr print` ne moti naslednje celice.
- `dev/tools/jfr-povzetek.js`: CPU vzorci strežniške niti (lastni, po vrstici, vključni, po prvem
  `noppes` okvirju), alokacije po razredu in klicnem mestu; `--klicatelji <metoda>` za verige.
- Zagon idle/boj/skripte × 500, `RwTarget 2`: P1–P9 in J1–J3 zelena.
  [zapis](../meritve/2026-10-08-M5-S-P1-profil-jfr.md)

**Ugotovitve:**
- Profil meritve ne pokvari (idle p95 5,11 proti 5,24 brez profila, v razponu); ocena alokacij iz JFR
  (20,6 / 93,4 MB/s) se ujema z `jvm.alloc.server` (20,7 / 93,6).
- **boj-500:** 48,3 % CPU v vanilla `pathFollow` → `isDirectPathBetweenPoints`, od tega 27,3 % v
  `ChunkProviderServer.getLoadedChunk` (hash za vsak blok). Iskanje poti 2 %, iskalnik tarč 0 %.
- **idle-500:** `Entity.move` 29 % (vanilla, tudi stoječi NPC), `onCollide` 12 %, `EntityDataManager.get`
  11,6 %, utrip oči (`sendAssociatedData`, AABB 160 blokov) 6,5 %, iskalnik tarč 1,8 %.
  Alokacija ~1 MB/tick: ~55 % vanilla `AxisAlignedBB` iz `Entity.move`, CNPC ~8 %.
- `HashMap$TreeNode.root` (9 % pri idle) pod `EntityDataManager.getEntry` je pripis JIT-a (drevesni koš
  pri majhnih `Integer` ključih ni mogoč).
- Nov kandidat **S14** v 09; S3 in S8 umaknjena; M5.6/S5 imata za boj nižjo prioriteto.
- Hipoteza (nepreverjeno): višji p95 skripte proti idle (9,4 proti 5,1 ms) ni v `ScriptContainer` (1,8 %).

**Spremembe obnašanja:** nobene (orodje in stikalo scenarija).

**Meritve:** M5-S P1 profil (tabela Meritve).

**Naslednja seja:** M5-S S14 (koda + test enakosti blokov, nato A/B boj-500) ali M3.8b GUI v klientu.

### 2026-10-07 (72) — Odločitvi Q13 in Q15

**Paket:** odločitvi uporabnika (Q13 → D-027, Q15 → D-028) · **Stanje:** končano

**Narejeno:**
- **D-027:** `RwTargetPrefilter` privzeto 2 (`CustomNpcs`). `dev/run/config` (ni v gitu) ima izrecno 0,
  zato scenariji in baseline še vedno merijo original; A/B jih nastavi z `-RwTarget`.
- **D-028:** `RwHitbox.acceptsPush` / `npcCrewOnly` / masa s posadko. Nosilec z igralcem in trk dveh
  originalnih NPC-jev ostaneta vanilla.
- `m39-run.ps1`: J4 in J5 z novimi pričakovanji (J4: nosilec v0, solid 0; J5: delež po masi nosilec +
  jahač iz izpisa probe — jahač med jahanjem ima manjši hitbox, masa 0,527 namesto 0,684). Tavajočih prog
  10 + 10 namesto 3 + 3: zagon s 3 + 3 je enkrat ostal brez stika (P2), ker NPC na poti tava.
- Regresija: `hb-run` H0–H8 zelen; `m39-run` **J1–J6 in P1–P4 zelena** (22 hodečih na cilju brez teleporta,
  stik tavajočih original 100 / solid 299 tickov, premik ob stiku na tick solid 0,0004 proti original 0,0158).

**Spremembe obnašanja:** D-027 (privzeto 2, iste tarče), D-028 (pod `RwHitbox=1`).

**Meritve:** nobene nove (regresija).

**Naslednja seja:** GUI M3.8b v klientu, nato S3/S4 ali profil JFR preostale alokacije.

### 2026-10-07 (71) — M5-S S2: poizvedba iskalnika tarč samo po igralcih

**Paket:** M5-S S2 ([09](../09-PERFORMANCE-RAZISKAVA.md)) · **Stanje:** končano

**Narejeno:**
- `TargetPrefilter.scanClass` + `onlyPlayers`, klic v `EntityAIClosestTarget.shouldExecute` po metu RNG;
  nov način `RwTargetPrefilter=2` (S1 + S2), `/rwtarget 2`, števec `zozenih`. Izčrpen test ekvivalence
  (3 testi), build zelen, `verify-package` PASS (64). Commit `3b712a8`.
- A/B v svetu (način 1 proti 2, idle-500 in boj-500, po 3 ponovitve, T1–T6 in P1–P9 zelena).
  [meritev](../meritve/2026-10-07-M5-S2-poizvedba-igralci.md)
- **idle-500:** p95 10,22 → 5,24 ms, p99 11,27 → 5,51, povp 4,94 → 3,41, µs/NPC 9,74 → 6,60, ticki >10 ms
  370 → 6; alokacija nespremenjena (21 MB/s). **boj-500:** `zozenih = 0` (bojni NPC-ji imajo
  `AttackOtherFactions`), vse v šumu.

**Ugotovitve:**
- Pogoj je strožji od predloga v 09: prazen seznam sovražnih frakcij ne zadošča, ker bi original pri NPC-ju
  brez frakcije vrgel izjemo; S2 velja samo brez `AttackOtherFactions` (privzeto).
- Preostala alokacija idle-500 (~1 MB/tick) ni iz iskalnika tarč.
- Serija je čakala ~4 ure na uporabnikov `ladja_mod`; na zahtevo uporabnika je bil njegov Minecraft ustavljen.
  Prvi poskus idle A je P9 zavrnil (uporabnikov gradle build med celico), ponovljen.

**Spremembe obnašanja:** nobene (privzeto 0; način 2 izbere iste tarče). **Meritve:** M5-S S2 (tabela Meritve).

**Odprto za uporabnika:** Q13 (zdaj: privzeto način 2?), Q15.

**Naslednja seja:** GUI M3.8b v klientu, odločitev Q13/Q15, nato S3/S4 ali profil JFR.

### 2026-10-07 (70) — M3.9: preverba solid × jahanje

**Paket:** M3.9 · **Stanje:** končano (igralec preverjen iz kode in s testom, ne v klientu)

**Narejeno:**
- Diagnostika v `/rwhitbox`: `mount`, `dismount`, `track` (nov razred `rework/entity/HitboxTrack`,
  sledilnik na vsak tick; ob ustavitvi strežnika se počisti). Test `igralecDobiNajvecDvakratVanilla`.
- Scenarij `m39-run.ps1` (fixture `M39_*` v `hb-fixture.js`): **J1–J6 in P1–P4 zelena**.
  [zapis](../meritve/2026-10-07-M3.9-solid-jahanje.md)
- Nosilec z jahačem: vanilla mu potiska ne doda (`isBeingRidden`), `RwHitbox` to ohrani v vseh načinih;
  po sestopu se nosilec obnaša kot navaden solid.
- NPC na poti: vseh 8 hodečih pride mimo (brez teleporta), tavajoči solid NPC se ob stiku ne premakne
  (0 proti 0,0219 bloka na tick stika pri originalu).

**Ugotovitve:**
- **Q15:** jahan nosilec in solid NPC se ob trku ne odrineta nikamor (J4: 0 / 0) — konjenica se lahko
  prekriva s solid pehoto. Pri SMART se izgubi delež nosilca.
- NPC, ki stoji (`MovingState 0`), je potisku zaprt že v originalu; hodeči je v stiku z njim od spredaj
  ~6,5 s, preden zdrsne mimo — v obeh načinih enako (obstoječe obnašanje).
- Prva dva zagona neveljavna: proge izven naloženih chunkov (scenarij zdaj sam pribije world spawn) in
  vzorčenje na sekundo pregrobo (zato `track`). Med sejo je trikrat tekel uporabnikov `ladja_mod`;
  P9 je zagon vsakič zavrnil, počakano.

**Spremembe obnašanja:** nobene (samo diagnostični podukazi). **Meritve:** M3.9 (tabela Meritve).

**Odprto za uporabnika:** Q15.

**Naslednja seja:** GUI M3.8b v klientu z igralcem (tudi potisk med dvema solid NPC-jema), odločitev
Q15, nato M3.10 ali M5-S S2.

### 2026-10-07 (69) — M3.8b: regresija `hb-run` na kodi z GUI-jem

**Paket:** M3.8b · **Stanje:** delno — strežniški del zelen, GUI v klientu ni preverjen

**Narejeno:**
- Ob začetku seje je tekel uporabnikov build `ladja_mod` (gradle); počakano, da je končal (~1 min), nato
  `.	estworld.ps1` in `.hb-run.ps1`: **H0–H8 zelena**, vse hitrosti enake zagonu 5. 10.; po restartu
  original=1 solid=1 smart=2, brez ERROR iz `noppes.*`. [zapis](../meritve/2026-10-05-M3.8-hitbox-nacini.md)

**Ni narejeno in zakaj:**
- GUI v klientu (Display → Hitbox → Solid, `/rwhitbox status`, ponovno odpri GUI) potrebuje igralca v
  klientu; ostane za uporabnika ali sejo z računalniškim upravljanjem, skupaj z M3.9.

**Spremembe obnašanja:** nobene. **Meritve:** nobene nove (regresija).

**Naslednja seja:** GUI M3.8b + M3.9 v klientu, ali naslednji strežniški paket M5-S (S2 kratki stik
»ni možne tarče« ali profil JFR za boj, [09](../09-PERFORMANCE-RAZISKAVA.md)).

### 2026-10-06 (68) — Nova zahteva R10 kot zadnja faza M11 (samo plan)

**Paket:** plan · **Stanje:** končano

**Narejeno:**
- Uporabnik je teoretično vprašal, ali bi se dalo vojsko (npr. 200 istoimenskih NPC-jev) prikazati
  na zemljevidu kot eno oznako in jo s klikom poslati na točko, kot pri ladjah. Ocena: izvedljivo,
  osnova 3–5 sej po M4.14b, polna verzija (dolg pohod, neaktivni chunki) še 5–10.
- Na željo uporabnika zapisano kot **zadnja faza M11** (D-026): R10 v `02-ZAHTEVE.md`, M11.1–M11.8
  v `03-FAZE.md`, README, Q14 (kateri zemljevid). Q12 kandidat preimenovan v R11.

**Ni narejeno in zakaj:** koda — uporabnik je izrecno rekel, da zdaj še ne dodajamo.

**Spremembe obnašanja:** nobene. **Meritve:** nobene.

**Naslednja seja:** nespremenjeno od seje 67 (M3.8b preverba, M3.9 ali naslednji M5-S paket).

### 2026-10-06 (67) — M5-S S1: A/B predzavrnitve tarč v svetu

**Paket:** M5-S S1 · **Stanje:** končano

**Narejeno:**
- A/B na idle-500 in boj-500, po 3 ponovitve v svežem svetu (`ponovitve-run` + `perf-run -RwTarget 0|1`),
  T1–T6 in P1–P9 zelena v vseh štirih serijah, med serijami ni tekel noben drug Java proces.
  [meritev](../meritve/2026-10-06-M5-S1-predzavrnitev-tarc.md)
- **idle-500:** alokacija 342,4 → 21 MB/s, GC 2,52 → 0,10 ms/s, MSPT p95 41,9 → 9,96 ms, p99 45,1 → 10,7,
  µs/NPC 29,0 → 9,31, ticki >25 ms 2.016 → 6; ~108,8 M predzavrnjenih kandidatov v 300 s. Vse daleč
  nad razponom. A se ujema z baselinom M2.6 (koda pri stikalu 0 stroška ne spremeni).
- **boj-500:** vse razlike v šumu (p95 10,5/10,5, alokacija 91,6/91,1) → predzavrnitev v boju ne škodi.
- Popravek `perf-run.ps1`: števec `rwtarget.predzavrnjenih` se je zaradi gnezdenih oklepajev v opisu
  načina bral kot −1.

**Ugotovitve:**
- **Model iz [09](../09-PERFORMANCE-RAZISKAVA.md) potrjen:** v originalu je bil nad 25 ms natanko vsak tretji
  tick (iskalnik tarč na tri tike), p50 pa ostane 3,4 ms. S tem je potrjen tudi vrstni red M5-S.
- Navodilo seje 66 je bilo napačno: `ponovitve-run` sprejme samo eno celico, `-Counts` je obvezen.
- Boj-500 p50 je danes 9,18 ms v obeh načinih, v baselinu 5. 10. 7,34 (7,34–8,91) — nepojasnjeno,
  hipoteza (stanje stroja ali spremembe po baselinu); na ta A/B ne vpliva.

**Spremembe obnašanja:** nobene nove (stikalo iz seje 66 ostane privzeto 0).

**Meritve:** M5-S S1 A/B (tabela Meritve).

**Odprto za uporabnika:** Q13 — naj bo `RwTargetPrefilter` privzeto vklopljen?

**Naslednja seja:** M3.8b preverba (`.\hb-run.ps1` po `.\testworld-run.ps1`, nato GUI v klientu), M3.9,
ali naslednji strežniški paket iz [09](../09-PERFORMANCE-RAZISKAVA.md) (profil JFR za boj).

### 2026-10-05 (66) — M5-S S1: predzavrnitev v iskalniku tarč pred raytraceom

**Paket:** M5-S S1 ([09](../09-PERFORMANCE-RAZISKAVA.md)) · **Stanje:** delno — koda in testi zeleni, A/B v svetu ni pognan

**Narejeno:**
- `rework/ai/TargetPrefilter` + en klic v `NPCAttackSelector.isEntityApplicable` (za preverjanjem
  dosega in zdravja, pred `canSee`). Kandidata zavrne pred raytraceom **samo, ko je gotovo**, da bi
  original vrnil `false`: ni stražarjeve tarče, ni tarče spremljevalca-stražarja, kandidat ni igralec
  (oz. je igralec v kreativnem načinu / z `disableDamage`), NPC kandidat je ubit ali ga frakcija ne
  napada. Sicer teče original nespremenjen. `NPCAttackSelector` je bil že prenesen (identičen
  reference-src), zato ločen commit prenosa ni bil potreben.
- Stikalo `RwTargetPrefilter` (privzeto 0 = original), ukaz `/rwtarget [0|1|reset]` s števcem
  predzavrnitev (marker `RWTARGET`), `perf-run.ps1 -RwTarget N` (odtis `rwtarget`, veličina
  `rwtarget.predzavrnjenih`).
- `TargetPrefilterTest` (6): izčrpno čez 1.296 kombinacij pogojev × vse razrešitve neznank ×
  vidnost/nevidnost/dom — kadar predzavrnitev zavrne, model originala vedno vrne `false` in nikoli
  ne vrže izjeme. Build 214/214 + 31/31, `verify-package` PASS (64).

**Ugotovitve (iz kode, pomembno za S1):**
- Premisa iz [09](../09-PERFORMANCE-RAZISKAVA.md) (»preverjanja so brez stranskih učinkov razen
  `PlayerData.get`«) je **premalo stroga**: `Faction.isAggressiveToPlayer` →
  `PlayerFactionData.getFactionPoints` ob prvem stiku igralca s frakcijo sproži skriptni dogodek
  `FactionUpdateEvent` in zapiše točke. Original to naredi samo za **vidnega** igralca. Zato igralec,
  ki ga NPC lahko napade, ostane v originalnem vrstnem redu (raytrace pred frakcijo). Za idle-500 to ni
  pomembno (igralca ni).
- Neskladno stanje (`job == 3` brez `JobGuard`, vloga 6 brez `RoleCompanion`, frakcija `null`) v
  originalu vrže izjemo samo za vidnega kandidata; predzavrnitev o njem ne odloča.
- Edina razlika je vsebina predpomnilnika `EntitySenses` (briše se vsak tick).

**Ni narejeno in zakaj:**
- **A/B v svetu:** ves čas seje sta tekla uporabnikov `ladja_mod` (gradle + Minecraft) in Minecraft
  iz CurseForge; P9 bi zagon zavrnil, meritev bi bila tudi neveljavna. Prav tako `hb-run.ps1` (M3.8b).

**Spremembe obnašanja:** nobene privzeto. Z `RwTargetPrefilter=1` se izbrane tarče ne spremenijo
(test); spremeni se samo, za katere kandidate se izvede raytrace.

**Meritve:** nobene.

**Naslednja seja (ko ne teče noben drug Minecraft/gradle):**
1. `.\testworld.ps1`, nato A in B z isto serijo celic (po 3 ponovitve, ~80 min vsaka):
   `.\ponovitve-run.ps1 -Scenarij perf -Dodatno @('-Variants','idle','-RwTarget','0')` in
   `... '-RwTarget','1'`. Pričakovano po modelu iz 09: idle-500 alokacija s 344 MB/s proti ravni
   idle-50/boj, ticki >25 ms skoraj nič; `rwtarget.predzavrnjenih` > 0. Če alokacija ne pade, je
   model napačen → profil (JFR). Kandidat pod šumom se zavrže (M5.5).
2. M3.8b preverba (`hb-run.ps1` + GUI v klientu).

### 2026-10-05 (65c) — klientske optimizacije: ideje iz UEBS2 dopisane v seznam K

Na vprašanje uporabnika (kako UEBS2 zmore milijone enot, učinkovita klientska optimizacija): v
[09](../09-PERFORMANCE-RAZISKAVA.md) dodani **K10** (instanced renderer), **K11** (tanjši klientski tick
oddaljenih), **K12** (okluzija, najprej Entity Culling), **K13** (skini v teksturnem nizu), impostor kot
zadnja stopnja **K5**; predlagan vrstni red M5-K: K0 → K5 → K12 → K13 → K10. Zniževanje ločljivosti
tekstur po gostoti ocenjeno kot neučinkovito za FPS (ozko grlo so klici na NPC, ne tekstura) — zapisano z
razlogom. Brez kode, brez meritev; vse čaka na harness K0 (M2.4r).

### 2026-10-05 (65b) — M3.8b: GUI za hitbox način

**Paket:** M3.8b · **Stanje:** delno — koda in testi zeleni, preverba v svetu in klientu ni narejena

**Narejeno:**
- Prenos `DataDisplay` in `GuiNpcDisplay` brez funkcionalnih sprememb (ločen commit): 63 metod, 58
  enakih, 3 razlika v obliki, 2 ročno pregledani (`audit/m38b-bytecode-primerjava.txt`). Orodje
  `primerjaj-bytecode` prepisano v node (`dev/tools/primerjaj-bytecode.js`), ker tu ni Pythona;
  primerjati je treba z reobfusciranim `dev/build/libs/CustomNPCs_1.12.2-01Oct19-workspace.jar`.
- Vrstica **Hitbox** (Original / Solid / Smart) na zavihku Display; pot podatkov brez novih paketov
  (`rework/entity/HitboxGui`, 7 testov). [zapis](../meritve/2026-10-05-M3.8-hitbox-nacini.md)
- Build 208/208 + 31/31 (`HitboxGuiTest` izključen iz `testOriginal`), `verify-package` PASS (64).

**Ni narejeno in zakaj:**
- `hb-run.ps1` je zavrnil zagon po P9: tekel je uporabnikov drug projekt (`ladja_mod`, gradle +
  Minecraft). Varovala nisem obšla. Spawn podatki so se spremenili samo za NPC-je z načinom.
- GUI v klientu z igralcem ni preverjen (tudi M3.9 potrebuje klienta).

**Spremembe obnašanja:** nobene privzeto. NPC z načinom ima v spawn podatkih ključ `RwHitboxStored`.

**Naslednja seja:** `.\hb-run.ps1` (svet je postavljen s `testworld-run`), nato v klientu: urejevalnik
NPC-ja → Display → Hitbox → Solid, zapri, `/rwhitbox status` mora pokazati solid; ponovno odpri GUI.

### 2026-10-05 (65) — M3.6 zaključen: ponovni zagon v obeh načinih

**Paket:** M3.6 (naloga 0.3 iz [08](../08-PREGLED-IN-PLAN.md)) · **Stanje:** končano

**Narejeno:**
- Ponovni zagon `m36-run.ps1` po dopolnitvi scenarija 24. 9. (ločen doseg napada). Način 1 **zelen**
  (A1–A6): 10 tavajočih v dosegu, vsi z zakasnitvijo 10 tickov = kontrola. Način 0 dvakrat: vsak
  zagon posebej pade na A2 (1 oz. 7 vzorcev v dosegu, potrebnih 8 — NPC-ji so odtavali dlje);
  združeno 8 vzorcev, mediana 25 > 20, 6/8 nad kontrolo → napaka originala ponovljena.
  [zapis §6](../meritve/2026-09-24-M3.6-prioriteta-napada.md)
- **P2 potrjen:** v načinu 1 so vse zakasnitve nad 20 izven `aggroRange` ob ukazu.
- Najdena past v postopku: sam `testworld.ps1` ustvari svet z vanilla spawnom (743, −231), chunki
  prog niso naloženi in `noppes clone spawn` tiho ne naredi nič. Prvi par zagonov je zato padel.
  `m36-run.ps1` zdaj takoj pade z navodilom (`testworld-run.ps1`) in ob izjemi ustavi server
  (preverjeno: 28 s, server čisto ustavljen). Navodila v scenariju popravljena.

**Ugotovitve:** čisti učinek napake prioritete pri NPC-ju v dosegu je 10–20 tickov; velike zakasnitve
prvega zagona (do 120) so bile večinoma vzorci izven dosega (P2).

**Spremembe obnašanja:** nobene nove (`RwAttackPriority` iz 24. 9.). Skripta `m36-run.ps1`: hitrejši padec.

**Meritve:** M3.6 ponovni zagon (tabela Meritve). Opomba: med zagonom b je uporabnik zagnal drug
Minecraft (`ladja_mod`); M3.6 meri v tickih, zato to na izid ne vpliva.

**Odprto za uporabnika:** popraviti P2 (utripajoč napad izven `aggroRange`)?

**Naslednja seja:** M3.8b (GUI hitbox), M3.9, M5-S S1. Svet v `dev/run` je trenutno prazen (po
preverbi padca) — pred naslednjim scenarijem `.\testworld-run.ps1`.

### 2026-10-05 (64b) — M3.8: hitbox načini SOLID in SMART (R6)

**Paket:** M3.8 · **Stanje:** končano (GUI v M3.8b)

Uporabnik je R6 dopolnil: poleg obstoječega odrivanja (ORIGINAL) **SOLID** (NPC-ja odrivanje ne premakne) in **SMART** (moč odrivanja po velikosti hitboxa, ščit v roki ga oteži; kompatibilnost s ščiti modov). Zasnova **D-025**: krmili se delež vanilla potiska v `applyEntityCollision`/`collideWithEntity`, ne trdi collision box. Stikalo `RwHitbox` (privzeto 0), per-NPC NBT `RwHitboxMode`, ukaz `/rwhitbox`.

**Preverjeno:** 201/201 + 31/31 testov, `verify-package` PASS, scenarij `hb-run.ps1` H0–H8 zelen (smart 5 proti 15: 1,93 v0 / 0,07 v0; ščit 4/3 / 2/3; solid 0). Scenarij je našel in popravljen je bil hrošč: trk, ki ga sproži originalni NPC, je solid NPC-ja odrinil. [zapis](../meritve/2026-10-05-M3.8-hitbox-nacini.md)

**Spremembe obnašanja:** samo pod `RwHitbox=1` in na NPC-jih z `RwHitboxMode`; privzeto nespremenjeno.

**Naslednja seja:** M3.8b (GUI), M3.9 (solid × jahanje, igralec v klientu), M5-S S1.

### 2026-10-05 (64) — integracija `m7-next`; M2.6b zaščita merilnega zagona

**Paket:** Faza 0, naloge 0.1 in 0.2 iz [`08-PREGLED-IN-PLAN.md`](../08-PREGLED-IN-PLAN.md)
**Stanje:** končano (zagon baselina M2.6 ostane)

**Narejeno:**
- **0.1:** veja `origin/codex/m7-next` (28. 9.: M7.6 hitrost, M7.7 serije, M7.8 zaključek, M7.9 sled,
  M7.10c `crowdYield`, V4 sprejet) je prenesena v `codex/m7-cnpc-integration` s cherry-pickom treh
  commitov; njen prvi commit (`6ee402b`) je bil rekonstrukcija lokalnih commitov in ima isto kodo
  kot `98c3d62`. Konflikti samo v dnevniku (oba vnosa ohranjena). Pregled 3. 10. te veje ni poznal —
  naloga 0.4 (A/B in odločitev D-022) je bila že narejena. Privzeta pot knjižnice popravljena za obe
  postavitvi repozitorija (`c102c50`).
- **0.2 → M2.6b:** diagnoza iz pregleda (čiščenje celic) je bila napačna. Server se je 27. 9. sesul z
  `NoClassDefFoundError`, ker je bil runtime jar, ki ga je imel odprtega, prepisan; v isti mapi je
  tekel drug baseline zagon. **Reproducirano 5. 10.** (enak podpis, tudi `SquadManager` ob ustavitvi).
  Popravek: kopija runtime jarja na zagon (`build.gradle`), zaklepa `.scenarij.lock`/`.baseline.lock`,
  merilo **P9** (tuj gradle/Minecraft pred in med celico), takojšnja zaznava sesutja, P2 brez stare
  vrstice, `taskkill /T` ob prisilni ustavitvi. [zapis](../meritve/2026-10-05-M2.6b-zascita-zagona.md)
- Najden in popravljen **P8**: manjkajoč `jvm.gc` (brez GC zbirke v kratki meritvi) je štel kot napaka
  in skril `alok.*`.

**Preverjeno v seji (prvič na uporabnikovem računalniku):** `buildPatchedMod` (test 193/193,
testOriginal 31/31), `verify-package` PASS (62 razredov), samotest 18/18, trije zagoni v svetu
(sabotaža skupnega jarja + tuj gradle; sabotaža odprte kopije = reprodukcija; čist zagon P1–P9 zelen).

**Spremembe obnašanja:** nobene v modu. Build: dev server in klient tečeta s kopijo runtime jarja.

**Naslednja seja:** zagon baselina M2.6, ponovni zagon M3.6, PR v `main`.

**Baseline M2.6 zelen (5. 10., 14:26–17:50):** prvi poskus (13:31) je v celici skripte-50 našel 4 NPC-je iz
boj-500 — `noppes slay npcs` brez števila pobije samo v radiju 120 blokov od konzole; popravek: radij
2000 in merilo P0 (svet pred spawnom prazen). Drugi zagon: 3/3 ponovitve, 27/27 celic zelenih, enak
odtis. Ključno: idle-500 MSPT p50 3,47 / p95 44,0 ms, alokacije 344 MB/s (enako skripte-500) — skoraj
ves strošek je iskanje tarče (kandidat S1); boj-500 p95 le 9,7 ms. [baseline](../meritve/baseline-2026-10-05.md)

**Dopolnitev med zagonom baselina (brez builda):** naloga **0.6** (README, oznake NB.x = M7.x v
`07-BARITONE-OZADJE.md`) in **M3.7 zaključen brez spremembe obnašanja** — premisa ovržena iz kode
(`minRange` doseg kvečjemu poveča, izklop hitboxa ga ne zmanjša); nov kandidat za zgoščevanje v
radiusu enega bloka: NPC brez hitboxa ima `canBePushed() == false` in ga vanilla
`collideWithNearbyEntities` (`getTeamCollisionPredicate`) ne odriva — *nereproducirano, hipoteza*;
scenarij in morebiten popravek v M3.8. [analiza](../meritve/2026-10-05-M3.7-analiza-minrange.md)

### 2026-10-03 (63) — pregled projekta; M8 izločen iz plana (D-023)

Ocena stanja in plan preostalega dela: [`08-PREGLED-IN-PLAN.md`](../08-PREGLED-IN-PLAN.md). Na
zahtevo uporabnika je **M8 (migracija CustomNPC+, R3) izločen** (D-023). Ugotovljeno: baseline
M2.6 (27. 9.) je padel pri prehodu idle-500 → boj-50, ker je v svetu ostalo 500 NPC-jev
prejšnje celice — napaka čiščenja v merilni skripti, ne v modu.

**Naslednji korak:** Faza 0 iz plana — PR veje v `main`, popravek čiščenja med celicami in
zagon M2.6, ponovni zagon M3.6, serija A/B za Baritona in odločitev D-022, formacije M4.14b.

### 2026-09-28 — M7.9–M7.10: vzrok grla, čakanje v gneči, V4 sprejet

M7.9 (10×, `-Sled`): grlo B je nestabilno zaradi gneče v vratih (~800 trkov, 12–15 preklicev
`movement_timeout` v vsakem zagonu), zamik rezultata iskanja (0–1 tick) jo samo sproži. M7.10:
knjižnica `crowdYield` — NPC počaka za entiteto pred sabo, ki se premika ali ima pot in manjši ID,
čakanje ne šteje v časovno omejitev premika. Tri različice po 5×; M7.10c: 8/8 na obeh grlih in
odprtem v vseh zagonih. `nav-run.ps1 -Umik` (→ `-PnpcbCrowdYield=true`, `dev/build.gradle`).
V4 sprejet z znano omejitvijo (zadnji prihod na skupni cilj 2–6 s kasneje).
[meritev](../meritve/2026-09-28-M7.10-cakanje-v-gneci.md). `audit/*.csv` (sledi, ~2 MB) v `.gitignore`.

**Naslednji korak:** commit + push, nato ponovni zagon M3.6 (`.\m36-run.ps1`, `.\m36-run.ps1 -Nacin 1`).

### 2026-09-28 — M7.9: sled nedeterminizma grla B; vrata V4 popravljena

**Paket:** M7.9 (knjižnica `npcbaritone`, izven vrstnega reda faz CNPC; potrdil uporabnik).

Po M7.8 je Baritone na grlu B edini nestabilen: 8, 8, 4 od 8, vanilla vedno 6. V slabem zagonu
(`audit/m25c-nav-2026-09-28-1053`, p1) je 7/8 v zastoju in knjižnica je iskala 77-krat namesto 68.
Sled loči dve hipotezi: rezultat asinhronega iskanja se uporabi v različnem ticku, ali pa se
skupina zagozdi v vratih in izvajalec pot prekliče. Koda in zapis v knjižnici
(`docs/04-STANJE.md` 28. 9., `PathTrace` stolpci 18–24, `tools/sled_grlo.py`).

**Tukaj:** `nav-run.ps1 -Sled` (samo z `-Ozadje baritone`) vklopi `/npcb trace on` pred fazo A, po
NAV-SUM sled izpiše (`NPCB-TRACE-DUMP`) in jo kopira ob zapis zagona kot `<zapis>-sled.csv`
(merila »M7.9: …«). Vanilla zagon in zagon brez `-Sled` sta nespremenjena. PowerShell parser
0 napak (pwsh 7.4), regex in ime ciljne datoteke preverjena na sintetičnem vnosu.

**Vrata V4** (D-022, knjižnica D-043): veličina 1 »ni slabše«, 2–3 boljša mediana in najslabši
Baritonov zagon ni slabši od vanille, Baritone ≥ 5 ponovitev. Izid M7.8 ostane »ni prestan«.

**Naslednji korak:** v `..\npcbaritone-m7` `.\dev.ps1 build --offline`, nato tukaj
`.\ponovitve-run.ps1 -Ponovitev 10 -Dodatno @('-Ozadje','baritone','-Sled','-BaritoneRoot','..\npcbaritone-m7')`.

### 2026-09-28 — M7.8: izmerjene poti, R1 in odločitev V4

[Zaključna meritev](../meritve/2026-09-28-M7.8-zakljucek.md): 3/3 vanilla in 3/3
Baritone N1–N15/T1–T6 zelena. Dejanske poti so pri obeh 8/8 cele; razmerje
dolžine p50 je Baritone 1,049 proti vanilla 1,131. Na grlu A prispe 6/8
proti 1/8; na grlu B ena Baritonova ponovitev zaostane (4/8 proti 6/8).
Strežniški tick brez shranjevanja je pri Baritonu nižji čez razpon, števca
iskanj pa nista primerljiva. **V4 ni prestan**, ker veličina 1 ni boljša
in B ni stabilno boljši; globalni `RwNavBackend` ostane privzeto 0.

R1 je razkril zastoj nosilca z jahačem pod Baritonom (1,37 bloka v fazi A).
`RwNavBackend` zdaj izključi tudi nosilca s potnikom. Po popravku R1 v svetu
zelen: 26/26 NPC-jev, 8/8 jahačev odklopljenih, 8 prostih nosilcev pripetih,
nosilci z jahači prevozijo 25,35 bloka. Build in `verify-package.ps1` zelena.

### 2026-09-28 — M7.6: hitrost in ponovljena kontrola

Ločeni worktree knjižnice in CNPC. `buildPatchedMod` in `verify-package` zelena;
knjižnica `build` zeleni testi. Popravljena pot do API jarja in razredov knjižnice pri
zagonu iz druge delovne kopije (`NPCB_MOD_DIR`, `-BaritoneRoot`). Vanilla in Baritone
scenarij M2.7 sta oba prestala N1–N15; Baritone tudi B1–B5, 16/16 pripetih.

Meritev [hitrosti in A/B](../meritve/2026-09-28-M7.6-hitrost-in-ab.md): po popravku
`moveForward` v knjižnici je hitrost odprte proge A 0,1566 proti vanilli 0,1482
bloka/tick. Prvi posamezni zagon na grlu B je dal 4/8 proti vanilli 6/8.
Nato sta bili opravljeni [obe seriji po tri ponovitve](../meritve/2026-09-28-M7.7-ab-tri-ponovitve.md):
grlo A Baritone 4–6/8 proti vanilla 1/8, grlo B 6–8/8 proti 6/8. Dolžina
Baritonove poti in primerljiv strošek iskanj ostajata odprta; V4 ni potrjen.

### 2026-09-27 (62) — prvi A/B zagon M7.6 ovrednoten

Baritone zagon zelen (N1–N15, B1–B5, 16/16 pripetih), vanilla prva ponovitev zelena.
Baritone 8/8 pri 40 tickih na obeh progah, vanilla G 1/8 (A) in 6/8 (B). Večji del razlike v
času je **hitrost**: knjižnica je v `own` ignorirala `speed × 0,7` iz `navigateTo` — popravljeno
v knjižnici (D-042). [meritev](../meritve/2026-09-27-M7.6-ab-prvi-zagon.md). Serija vanilla je
padla v ponovitvi 2 na gradle `:extractNatives` (zaklenjeni natives); `nav-run.ps1` zdaj enkrat
poskusi znova po 20 s.

**Naslednji korak:** knjižnica `.\dev.ps1 build --offline`, nato `.\testworld.ps1` +
`.\nav-run.ps1 -Ozadje baritone`; ko je `gib(max)` na progi O blizu vanilli, obe seriji po 3.

### 2026-09-27 (61) — M7.2–M7.4 commitani; M7.6 A/B scenarij (`nav-run -Ozadje baritone`)

**Paket:** M7 knjižnice `npcbaritone` (izven vrstnega reda faz CNPC, D-022).

**Commitano delo seje (4) knjižnice** (`3e458aa`, prej necommitano na tej veji): most
`NpcBaritoneBridge` (samo API 2, `compileOnly`), stikalo `RwNavBackend` (config 0 + NBT ključ,
zapisan samo pri 1), izbira samo za kopenskega NPC-ja brez jahanja in formacije (U7), ponovna
namestitev po `updateTasks()` (U2), `/rwnav`. Tam preverjeno: `buildPatchedMod` (192/192),
`verify-package`, dedicated smoke. Prevod v oblaku na tem drevesu: 102 + 25 datotek, 192/192.

**M7.6 (v tej seji, prevedeno, čaka na zagon):** `nav-run.ps1 -Ozadje baritone` —
[scenarij](../scenariji/M7.6-ab-baritone.md). Server z `-PnpcBaritoneDev -PrwNavBackend=1`,
`/rwnav on NAV_Walk`, `/npcb perf` okoli faz A/B; merila B1–B5; odtis `ozadje=baritone`, da se
serija ne pomeša z vanilla. `/rwnav status` zdaj izpiše tudi `pripetih=` (dejansko pripeti).
Vanilla zagon je nespremenjen (brez `-P`, isti odtis). Preverjeno: PowerShell parser 0 napak,
bralnika `RWNAV`/`NPCB-PERF` in `New-NavZapis` na sintetičnem logu (pwsh 7.4), JUnit 192/192.

**Znano:** sonda (veličine 1, 2, 5) je vanilla `PathFinder` tudi pri Baritonu — za A/B štejejo
`cele=` iz igre, `NAV-CAS` ter `bar.*` iz knjižnice; veličina 2 za Baritona še ni merjena.

**Naslednji korak:** knjižnica `.\dev.ps1 build --offline`, nato `.\testworld.ps1` in
`.\nav-run.ps1 -Ozadje baritone` (en zagon, B1–B5 + N1–N15); ko je zelen, obe seriji po tri
ponovitve (`.\ponovitve-run.ps1 -Scenarij nav` in `... -Dodatno @('-Ozadje','baritone')`).

### 2026-09-27 (60) — integracija lokalne veje (M4.14a); D-022 Baritone kot izbirno ozadje; revizija navigatorja

**Paket:** vzdrževanje repozitorija + M7.1 knjižnice `npcbaritone` (izven vrstnega reda faz CNPC —
izrecna potrditev uporabnika 2026-09-27)
**Stanje:** koda CNPC ni funkcionalno spremenjena; formacije integrirane, dokumentacija D-022

**Integracija:** mapa `Desktop\customNPC_rework` je bila na stanju 18. 9. z enim nepushanim
commitom (M4.14a formacije, lokalni D-017). `git pull --rebase` je 27. 9. ustavil konflikt
(D-017–D-020 so na `origin` že zasedeni) in je bil prekinjen brez izgube. Lokalna veja je
shranjena v `backup/lokalni-main-20260927`; `main` je postavljen na `origin/main` (M3.6), M4.14a
prenesen s cherry-pickom (`CustomNpcs`: obe registraciji ukazov; dokumenti: vse vrstice origin +
formacije) in njegova odločitev preštevilčena **D-017 → D-021**. Preverjeno v seji: 99 virov
`src/patch` prevedenih z `javac --release 8`, **189/189 testov**.

**D-022** (`01-ARHITEKTURA.md` §9): stopnja D iz D-012 se odpre samo kot izbirno ozadje
(`RwNavBackend`) prek zunanje knjižnice `npcbaritone`; vanilla privzeta; samo `movementType == 0`;
sprejem po M2.7 s tremi ponovitvami. Knjižnica: 200 NPC-jev, glavna nit p95 1,64 ms/tick.

**Revizija** [`07-BARITONE-OZADJE.md`](../07-BARITONE-OZADJE.md): klici navigatorja v `ai/**` in
`EntityNPCInterface` so po M3.1–M3.6 enaki izvirniku; ukrepi U1–U7. Najpomembnejši:
**U2** — `updateTasks()` ob vsaki posodobitvi AI ustvari nov vanilla navigator; **U7** —
formacije (`Squad`, `STEER`) vsak tick nastavijo pot z eno točko, zato člani enote ostanejo na
vanilla navigatorju; **U1** — `EntityAIAttackTarget.shouldExecute` preverja pot z vanilla A*.
V knjižnici popravljena napaka, ki bi jo sprožil CNPC skriptni `getNavigationPath` (NPE med
iskanjem; knjižnica `2ee0df2`).

**Naslednji korak:** knjižnica API 2 (U2 ponovna namestitev, U6 hitrost, U3/U4 vrata na
instanco), nato M7.2 (`NpcBaritoneBridge`), M7.3 (stikalo), M7.4 (`updateTasks`, U2, U7).
Formacije: dimni test F1–F12 v svetu še čaka (kot 18. 9.).

---

### 2026-09-24 (59) — M3.6 prvi zagon ovrednoten; scenarij dopolnjen z dosegom napada

**Paket:** M3.6 · **Stanje:** delno — popravek deluje, A5 strogo pade na enem vzorcu; **ponovni zagon čaka na uporabnika**.

**Izid prvega zagona** (`audit/m36-napad-n0.log`, `-n1.log`, [zapis](../meritve/2026-09-24-M3.6-prioriteta-napada.md)):
A1, A2, A3, A6 zelena v obeh načinih. **A4: napaka PONOVLJENA** — tavajoči mediana 30 / max 120 tickov,
2 brez napada; kontrola 10 / 10. **Način 1:** mediana 10, 17 od 20 tavajočih 10 tickov, nihče brez
napada, a max 50 > kontrola + 10 → **A5 PADLO** (en vzorec 50, en 30).

**Hipoteza za ostanek (nereproducirana):** `EntityAIAttackTarget.shouldContinueExecuting` ustavi napad
izven `aggroRange` (16, Čebiševa razdalja), `shouldExecute` ga brez te preverbe spet začne → NPC, ki je
ob ukazu odtaval predaleč, ima utripajoč napad. Obnašanje originala, neodvisno od prioritete.

**Narejeno:**

- `m36-control.js`: ob ukazu Čebiševa razdalja do tarče (`d=` v `M36-R`), oznaka `o` za vzorce nad
  `aggroRange`, ki ne štejejo v A2–A5; `M36-LAT` dobi polja `izven`, `izvenNikoli`, `latIzvenMed`,
  `latIzvenMax` (na koncu vrstice, stari regex ostane veljaven). Vstavljeno v `M36_Control.json` z
  `vstavi-skripto.py` (ujemanje preverjeno).
- `m36-run.ps1`: bere nova polja; če jih ni (stara skripta v JSON), preverba pade. **Popravljena
  napaka scenarija:** krmilnik iz prejšnjega zagona je shranjen v svetu in se ob nalaganju oglasi
  (`M36-INIT`/`M36-FIND`, vidno v n1 ob 11:07:57), A1 pa je bral prvi `M36-FIND`. Zdaj se štejejo samo
  markerji po spawnu, izid se bere od zadnjega `M36-INIT`.

**Preverjeno v seji:** skripta pognana v Node na mock svetu (NPC izven dosega → `ot50`, izločen iz
statistike); `m36-run.ps1` razčlenjen s PowerShell 7.4.6 (0 napak); korak 8 pognan na sintetičnem logu
(stari + novi krmilnik → vse zeleno, vzorec `o` izločen) in na **pravem** `m36-napad-n1.log` (pravilno
pade: A5 50 proti 10 in manjkajoča polja `izven`).

**Ni preverjeno:** ponovni zagon v svetu. Hipoteza je potrjena, če so v načinu 1 vse zakasnitve nad 20
označene z `o`.

**Ni del tega commita:** `dev/build.gradle` (izključitev testov `rework/**` iz `testOriginal`),
`vse-testi.ps1` in `ZAZENI-VSE-TESTE.bat` sta necommitana sprememba izven te seje in ostajata nedotaknjena.

**Opomba za seje:** `git status` v lupini na uporabnikovem računalniku pusti `.git/index.lock` (brisanje v
mapi ni dovoljeno); premaknjen je v `.git/_to_delete/`, ki ga uporabnik lahko pobriše. Za git v tej lupini
uporabljaj `git --no-optional-locks`.

**Spremembe obnašanja:** nobene (samo scenarij).

**Naslednja seja:** ovrednotiti ponovni zagon M3.6; če A5 zeleno → M3.6 zaključen, M3.7.

### 2026-09-24 (58) — M3.6 preusmerjen: mutex biti so pravilni, napaka je v prioriteti napada

**Paket:** M3.6 · **Stanje:** v kodi, prevedeno in testirano v seji; **zagon v svetu čaka na uporabnika**.

**Premisa ovržena (pred kodiranjem, z uporabnikom dogovorjena preusmeritev, D-020).** `AiMutex`
`PASSIVE/LOOK/PATHING` = 1/2/4 so po vrednosti vanilla MOVE/LOOK/JUMP. `EntityAIAttackTarget` ima 3,
enako kot vanilla `EntityAIAttackMelee`; vsi gibalni taski imajo bit 1, zato z napadom po
`EntityAITasks.canUse` hkrati ne teče noben. „Dodaj `PATHING`“ bi spremenil samo razmerje do skoka.
Popravljeno v `02-ZAHTEVE.md` (dve mesti), D-012, D-019, `03-FAZE.md`.

**Resnična napaka (javap na originalu):** `setResponse` doda napad s `taskCount` brez `++`, zato
`EntityAIWander`/`EntityAIMovingPath` dobi isto prioriteto; enake prioritete vanilla ne prekine.
`Wander` se ob napadu ne umakne sam → NPC, ki tava, tarčo napade šele na koncu poti tavanja.
`MovingPath` se umakne sam (`isAttacking()`), NPC s projektilom napake nima.

**Narejeno:**

- `rework/ai/AttackPriority` (čista funkcija `nextPriority`) in `CommandRwAttack` (`/rwattack 0|1`,
  ob spremembi vsem naloženim NPC-jem `updateAI = true`, marker `RWATTACK`); config
  `RwAttackPriority` (privzeto 0); klic v `EntityNPCInterface.setResponse` za bojnimi taski.
- `AttackPriorityTest` (8) na **pravem** vanilla `EntityAITasks`: `originalWanderingBlocksAttack`
  pokaže napako, `fixAttackPreemptsWandering` popravek; dva testa ovržeta staro premiso.
- Scenarij `m36-run.ps1` + `m36-setup-commands.txt` + `m36-control.js` + štiri fixture `M36_*`
  ([opis](../scenariji/M3.6-napad-med-tavanjem.md)): proga T (tavajo) proti kontroli S (stojijo), trije
  krogi, zakasnitev od `setAttackTarget` do poti k tarči; A4 diagnoza v načinu 0, A5 preverba v načinu 1.

**Preverjeno v seji:** vseh 84 izvornih datotek `src/patch` + testi prevedeni z `javac --release 8` (JDK 11 na
uporabnikovem računalniku, knjižnice iz gradle predpomnilnika, samo branje); **35/35 testov
`rework/**` zelenih**; `m36-run.ps1` razčlenjen s PowerShell 7.4.6 (0 napak), regexi preizkušeni na
sintetičnem logu; `m36-control.js` preverjen z Node in pognan na mock svetu; vstavitev v JSON preverjena.

**Ni preverjeno:** gradle build, zagon v svetu (A1–A6). `verify-package.ps1` bo pokazal nova razreda `rework/ai/AttackPriority` in
`CommandRwAttack` ter spremenjena `EntityNPCInterface` in `CustomNpcs`.

**Stranske ugotovitve (hipoteze, niso popravljene):** `EntityAIMoveIndoors`/`EntityAIFindShade` imata
prioriteto pred napadom in se ob napadu ne umakneta; napad in strelski napad imata isto prioriteto.
[zapis](../meritve/2026-09-24-M3.6-prioriteta-napada.md)

**Spremembe obnašanja:** nobene privzeto; z `RwAttackPriority=1` (tabela).

**Naslednja seja:** ovrednotiti `audit/m36-napad-n0.log` in `-n1.log`, nato M3.7.

### 2026-09-24 (57) — M3.5 zaključen: E9 zeleno

**Paket:** M3.5 · **Stanje:** zaključeno.

`r1-run.ps1` v načinu 0 in 1 (`audit/m22-r1.log`, `m22-r1-k1.log`, oba `R1-SUM mount=8`): višina
jahačev pred mountom 1,900, po mountu 1,463 v obeh; **po sestopu 1,463 v načinu 0 (napaka ponovljena)
in 1,900 v načinu 1 (popravek)**. Faza A enaka kot v M3.4 (M 1,19 / 22,57). Odjemalec ni preverjen.
[zapis](../meritve/2026-09-24-M3.5-hitbox.md)

**Spremembe obnašanja:** nobene privzeto. **Naslednja seja:** M3.6.

### 2026-09-24 (56) — M3.5: hitbox jahača po sestopu

**Paket:** M3.5 · **Stanje:** v kodi, prevedeno in testirano v seji; **zagon v svetu čaka na uporabnika**.

**Popravek predpostavke iz `02-ZAHTEVE.md` §R1.** Trditev „`updateHitbox()` se ob `startRiding` ne
pokliče“ velja za `EntityNPCInterface`, **ne pa za `EntityCustomNpc`** (običajni NPC): ta v originalu
override-a `startRiding` in po `super` brezpogojno pokliče `updateHitbox()` (`javap -c` na
`customnpcs-dev-runtime.jar`). Mount je torej že pravilen (višina × 0,77). Manjka samo **sestop**:
`dismountRidingEntity` ne override-a nihče, zato jahač po sestopu ostane skrčen, dokler ga ne osveži
animacija, smrt ali reset.

**Narejeno:**

- `EntityNPCInterface.dismountRidingEntity()`: po `super` pokliče `updateHitbox()` (virtualno, torej
  `EntityCustomNpc.updateHitbox` z modelom), če je entiteta prej jahala in `RiderState.hitboxFollowsRiding`
  (način ≠ 0, D-019). Pokriva vse poti sestopa: ukaz/skripta, `removePassengers` ob smrti nosilca,
  odjemalca prek `SPacketSetPassengers`. Forge `EntityMountEvent` ni uporabljen, ker se sproži **pred**
  spremembo stanja (v njem je `isRiding()` še star) — odstop od opisa paketa v `03-FAZE.md`.
- Scenarij: `r1-control.js` izpiše `R1-HB faza=mount` (višina jahačev pred in po mountu) in na koncu po
  sestopu vseh jahačev `R1-HB faza=sestop`; vstavljeno v `R1_Control.json` z `vstavi-skripto.py`
  (ujemanje preverjeno), sintaksa preverjena z Node. `r1-run.ps1`: **E9** — v načinu 0 diagnoza
  (pričakovano: napaka ponovljena), z `-Krmiljenje 1|2` preverba, da se višina vrne (±0,01).
  Sestop je po koncu faze C, zato meritev faz A–C ni spremenjena.
- Test `hitboxFollowsRidingOnlyInFixModes` (skupaj 18/18 v seji).

**Ni preverjeno:** v svetu; odjemalčeva stran (dedicated server nima odjemalca).

**Spremembe obnašanja:** nobene privzeto; z `RwMountSteering=1|2` (tabela).

**Naslednja seja:** ovrednotiti E9 v obeh zagonih, nato M3.6.

### 2026-09-24 (55) — M3.4 zaključen: regresija zelena v vseh treh načinih

**Paket:** M3.4 · **Stanje:** zaključeno.

159 testov zelenih; `r1-run` v načinih 0, 1 in 2 brez napak. Način 0 = original (A M 1,19). Način 1
in 2: E8 zeleno (A M 22,8 bloka proti kontroli 19–20). **Način 2 v fazi C nosilcev ne premakne** —
pričakovano, ker v načinu 2 jahačeva pot nikoli ne krmili; konjenica s poveljnikom ostaja v načinu 1.
Faze B/C niso deterministične (tudi kontrola se premika med zagoni) — za A/B primerjati fazo A.
`tpTo` in gating sledenja scenarij ne izvede — samo testi. [zapis](../meritve/2026-09-24-M3.4-regresija.md)

**Spremembe obnašanja:** nobene privzeto. **Naslednja seja:** M3.5.

### 2026-09-24 (54) — M3.4: gibalni AI jahača in `tpTo` pod istim stikalom

**Paket:** M3.4 · **Stanje:** v kodi, prevedeno in testirano v seji; **zagon v svetu čaka na uporabnika**.

**Odločitev o obsegu.** Blokiranje prek mutex bitov ni izvedljivo: vsi gibalni in tudi bojni taski
CustomNPCs uporabljajo `PASSIVE` (`EntityAIAttackTarget` `LOOK+PASSIVE`, M3.6), zato bi task z
`PASSIVE` jahaču ugasnil tudi napad in `EntityAIWorldLines`. Namesto tega eksplicitna preverba v
dveh gibalnih taskih, ki je še nista imela (`EntityAIWander` in `EntityAIReturn` že preverjata
`isRiding()`). Bojni taski ostanejo: pot jahača v boju je v načinu 1 ukaz nosilcu (osnova M3.10).

**Narejeno:**

- `RiderState.riderMovementBlocked(način, jahaNpcNosilca, nosilecImaPot)`: 0 → nikoli; 1 → ko ima
  nosilec-NPC svojo pot (takrat bi pot jahača itak zavrgli, `decide` = MOUNT — test to preveri za vse
  kombinacije); 2 → vedno na nosilcu-NPC. Uporabljeno v `EntityAIFollow.canExcute` in
  `EntityAIMovingPath.shouldExecute/shouldContinueExecuting` (ob prekinitvi se indeks poti vrne, kot
  pri napadu).
- `RiderState.teleport` + `EntityNPCInterface.tpTo`: v načinih 1/2 se jahač na nosilcu-NPC ne
  teleportira sam (vanilla `updatePassenger` bi ga vsak tick vrnil), ampak se teleportira **nosilec**
  in jahač gre z njim; na nosilcu, ki ni NPC, `tpTo` ne naredi nič. Način 0: original.
- Brez novega stikala: vse je vezano na `RwMountSteering` (D-019).
- Testi: `RiderStateTest` +4 (skupaj **17/17** z `MountGuardTest`).

**Preverjeno v seji:** prevedeno z `javac --release 8` (spremenjeni `EntityAIFollow`,
`EntityAIMovingPath`, `EntityNPCInterface`, `RiderState`, `MountGuard`); 17/17 testov zelenih.

**Ni preverjeno:** v svetu. Scenarij R1 nima sledilcev ne poti premikanja, zato lahko v svetu
preveri samo, da se nič ne podre (regresija). Neposreden scenarij za `tpTo` jahača (sledilec na
nosilcu, lastnik daleč) ne obstaja — predlog za M3.9 ali ob M3.10.

**Spremembe obnašanja:** nobene privzeto; z `RwMountSteering=1|2` (tabela).

**Naslednja seja:** ovrednotiti zagone, nato M3.5.

### 2026-09-24 (53) — M3.3 zaključen: R1 popravljen v svetu

**Paket:** M3.3 · **Stanje:** zaključeno.

Uporabnik je pognal `.\dev.ps1 test --offline` (**155 testov, 0 padlih**; `RiderStateTest` 9/9,
`MountGuardTest` 4/4), build, `.\r1-run.ps1` in `.\r1-run.ps1 -Krmiljenje 1`.

- **Način 0 = original:** faza A proga M 1,19 bloka, `navig=0/8` — enako kot 24. 9.
- **Način 1:** faza A proga M `navig>0` v 20/20 vzorcih, prevoženo 22,70 (kontrola 20,43), vseh
  8 jahačev ostane na nosilcih; **E8 zeleno**. Faza B: proga M do cilja 0,60 (prej 24,31),
  stopnica in vrata prečkana. Faza C nespremenjena (namerno vanilla, ko ima pot samo jahač).
- Brez napak v obeh logih.

**Ugotovitve:** jahač brez poti v fazi C je **ponovljiv** (razpon 32,96, kot 24. 9.) — ostaja odprto.
`audit/package-verification.txt` (07:46) je iz starega `…-workspace.jar`, ne iz tega builda.

**Spremembe obnašanja:** nobene privzeto (glej tabelo; `RwMountSteering=1` je preverjen popravek R1).

**Meritve:** [zapis](../meritve/2026-09-24-M3.3-R1-popravek.md)

**Naslednja seja:** M3.4.

### 2026-09-24 (52) — M3.3: `RiderState` in popravek R1 pod stikalom

**Paket:** M3.3 · **Stanje:** v kodi, prevedeno in testirano v seji; **zagon v svetu čaka na uporabnika**.

**Narejeno:**

- `rework/entity/RiderState` — enoten vir resnice o jahanju: vloga (`NONE`/`RIDER`/`MOUNT`/`RIDER_AND_MOUNT`)
  in odločitev **kdo krmili** nosilca (`decide(način, nosilecJeNpc, nosilecImaPot, jahačImaPot)`), brez
  Minecraft tipov. Načini: **0 original** (privzeto, D-007), **1** nosilec-NPC krmili sam, jahač prevzame
  samo, ko ima pot on in nosilec ne (vanilla spider jockey ostane osnova za M3.10), **2** nosilec-NPC krmili
  vedno. Nosilec, ki ni NPC, je v vseh načinih vanilla.
- `rework/entity/MountGuard` — v `EntityNPCInterface.onLivingUpdate` jahača pred `super.onLivingUpdate()`
  zajame pot, hitrost poti in move helper nosilca (natanko polja, ki jih prepiše `EntityMoveHelper.read`,
  preverjeno z `javap -c`), po njem jih vrne. Pot se vrne z zapisom polja, ne s `setPath`, ker `setPath`
  ponastavi zaznavo zataknitve. Dostop do zaščitenih polj: `net.minecraft.pathfinding.RwNavigatorAccess`
  in `net.minecraft.entity.ai.RwMoveHelperAccess` (vzorec `RwWorldAccess`).
- Stikalo: config `RwMountSteering` (0/1/2, privzeto 0) in ukaz `/rwmount [0|1|2]` (med tekom, ne zapiše
  v config; odgovor v log z markerjem `RWMOUNT`).
- `r1-run.ps1 -Krmiljenje 1|2`: pred scenarijem pošlje `rwmount`, izpis gre v `audit/m22-r1-k<N>.log`,
  doda merilo **E8** (faza A: nosilci M imajo pot in proga M prevozi ≥ 20 blokov). Brez parametra je zagon
  enak kot pred M3.3.
- Testi: `RiderStateTest` (9, izčrpna tabela odločitev) in `MountGuardTest` (4): test
  `withoutGuardRiderErasesMountPath` ponovi vanilla prepis in pokaže R1 (pot `null`, `WAIT`), test
  `guardRestoresPathAndMoveHelper` pokaže, da ga varovalo razveljavi.

**Preverjeno v seji:** nova razreda + spremenjena `EntityNPCInterface` in `CustomNpcs` prevedeni z
`javac --release 8` proti mapiranim MC razredom in `customnpcs-dev-runtime.jar` (guava/netty/authlib
nadomeščeni z minimalnimi nadomestki izven repozitorija, D-014); **13/13 testov zelenih**. `r1-run.ps1`
razčlenjen s PowerShell 7.4 (0 napak).

**Ni preverjeno:** build z gradlom, nalaganje v igri, E8 v svetu. `verify-package.ps1` bo pokazal nove
razrede: `RiderState` (+ `$Role`, `$Steering`), `MountGuard`, `CommandRwMount`, `RwNavigatorAccess`,
`RwMoveHelperAccess`, poleg spremenjenih `EntityNPCInterface` in `CustomNpcs`.

**Znana meja (zapisana v kodi):** ko imata pot oba (nosilec in jahač), vanilla `setPath` nosilcu ponastavi
zasebno stanje zaznave zataknitve; varovalo vrne pot, tega stanja ne. V fazah A/B se to ne zgodi.

**Ni v tem paketu:** jahač od osmih brez poti v fazi C (odprto vprašanje, ni R1); gating AI taskov in
`tpTo` (M3.4); `updateHitbox` ob mountu (M3.5).

**Spremembe obnašanja:** nobene privzeto; z `RwMountSteering=1|2` nosilec-NPC obdrži svojo pot (tabela
"Sprejete spremembe obnašanja").

**Za uporabnika:**
```
.\dev.ps1 test --offline
.\dev.ps1 build --offline
.\r1-run.ps1                  # mora ponoviti izid 24. 9. (E7 POTRJENO, proga M v fazi A obtiči)
.\r1-run.ps1 -Krmiljenje 1    # E8: proga M v fazi A pride do cilja
```

**Naslednja seja:** ovrednotiti oba zagona (`audit/m22-r1.log`, `audit/m22-r1-k1.log`), zapis v
`docs/meritve/`, nato M3.4.

### 2026-09-24 (51) — M3.2 zaključen: faza C potrdi mehanizem

**Paket:** M3.2 · **Stanje:** zaključeno.

Uporabnik je pognal `.\r1-run.ps1` (09:07–09:10). Faze A in B ponovijo izid; v fazi C
(`navigateTo` jahačem) imajo nosilci proge M pot **natanko takrat kot njihovi jahači**
(`navig == navigJ` v 20/20 vzorcih: 7/7, 4/4, 0/0), prečkajo stopnico z = 20 in vrata
z = 28 in pridejo na cilj (0,64) v ~180 tickih — 2,8× hitreje od proge S (napoved ≥ 2,1×
iz hitrosti 1,5 proti 0,7). S tem je potrjen tudi del „ne znajo čez bloke“.
[izid](../meritve/2026-09-24-M3.2-R1-diagnoza.md#izid-faze-c-24-9)

**Napaka v mojem merilu E7:** prva različica je gledala povprečni `navig` faze C ≥ 4/8; ker
nosilci pridejo na cilj v tretjini faze in pot tam legitimno konča, je bilo povprečje 3,0 in
skripta je izpisala `OVRZENO` na potrjenem izidu. Popravljeno na ujemanje `navig == navigJ`
+ premik ≥ 10; na tem izpisu preizkušeno s PowerShell 7.4 → `POTRJENO`.

**Odprto (ne R1):** en jahač od osmih v fazi C ni dobil poti že od prvega vzorca; njegov
nosilec je ostal na startu. Vzorec nima po-NPC izpisa. Pogledati v M3.3.

**Spremembe obnašanja:** nobene.

**Naslednja seja:** M3.3.

### 2026-09-24 (50) — M3.2: nosilcu pot izbriše jahač, v vanilla kodi

**Paket:** M3.2 · **Stanje:** mehanizem najden in dokazan na bytecode; potrditev v svetu
(faza C, E7) čaka na `.\r1-run.ps1`.

**Najprej zaključek M3.1.** `verify-package.ps1` 23. 9. ni zapisal novega poročila, ker
primerja `CustomNPCs_1.12.2-01Oct19-workspace.jar`, ki ga naredi samo `buildPatchedMod`
(ta je od 15. 9.), ne `build`. Namesto tega je seja preverila tisto, kar je v zagonu
dejansko teklo: `customnpcs-dev-runtime.jar` (23. 9. 14:51) proti
`customnpcs-mapped-01Oct19.jar` — 1716 vnosov originala, **nobeden ne manjka**, spremenjenih
62 in dodanih 37, **vsi v naboru 99 razredov** `customnpcs-patch-classes`; nič drugega ni
spremenjeno. M3.1 je s tem zaključen. `verify-package.ps1` ostane za predajo (M10) in
zahteva `.\dev.ps1 buildPatchedMod --offline`.

**Mehanizem.** `EntityLiving.updateEntityActionState()` (vanilla, `final`) na koncu AI ticka
vsake entitete, ki jaha živo entiteto, naredi `nosilec.getNavigator().setPath(jahač.getPath(), 1.5)`
in `nosilec.getMoveHelper().read(jahač.getMoveHelper())` — vanilla spider jockey. Jahač se
posodobi takoj za nosilcem (`World.updateEntityWithOptionalForce`, potniki po nosilcu). Jahač
brez cilja nosilcu torej vsak tick izbriše pot (`setPath(null)` → `currentPath = null`) in
ga postavi v `WAIT`. Preverjeno s CFR na mapiranih razredih iz
`dev/build/tmp/recompileMc/compiled` (`EntityLiving`, `PathNavigate`, `PathNavigateGround`,
`EntityMoveHelper`, `World`), ne po spominu.

**Razloži vse izmerjeno:** `navig=0/8` v vseh vzorcih; faza A 23. 9. raste **natanko za
0,06 bloka na osvežitev `navigateTo`** (en tick gibanja na klic — podpis mehanizma); faza B
8,9 bloka, ker `EntityAIAttackTarget` prepotuje vsakih 4–10 tickov; zastoj pred stopnico
(skok se ne dokonča, ker `read()` stanje `JUMPING` prepiše v `WAIT` — ta del je hipoteza).
**`canNavigate()` ni vzrok** (nosilec je na tleh tako kot kontrola). Ostali kandidati §R1
(mutex, `minRange`, `updateHitbox`, `tpTo`) R1 ne pojasnijo in ostanejo v svojih paketih.

**Faza C** v `r1-control.js` (ticki 940–1340): `navigateTo` dobijo **jahači** proge M.
Napoved: nosilci M dobijo pot in vozijo s hitrostjo 1,5 (proti 0,7 proge S). `r1-run.ps1`
čaka še `R1-B-END` in `R1-C-START`, zahteva ≥ 20 vzorcev tudi za CM/CS (E5) in izpiše
diagnozo **E7** (POTRJENO / OVRZENO / NEODLOCENO; ne podre zagona). Vzorec ima novo polje
`navigJ=` na koncu, regex ga sprejme kot neobvezno. Skripta vstavljena v `R1_Control.json`
z `vstavi-skripto.py` (ujemanje preverjeno); JS sintaksa preverjena z Node, `r1-run.ps1`
razčlenjen s PowerShell 7.4 (0 napak) in `Read-Samples` preizkušen na sintetičnem izpisu.

**Ni preverjeno:** zagon faze C v svetu.

**Spremembe obnašanja:** nobene (samo scenarij).

**Posledica za M3.3:** popravek ni v AI nosilca, ampak v odločitvi, kdo krmili. Predlog v
[diagnozi](../meritve/2026-09-24-M3.2-R1-diagnoza.md): jahač-NPC v `onLivingUpdate` pred
`super` zajame pot in move helper nosilca in ju po njem vrne (pod stikalom); vanilla
krmiljenje z jahačem ostane osnova za M3.10.

**Naslednja seja:** ovrednotiti E7 iz `audit/m22-r1.log`, nato M3.3.

### 2026-09-23 (49) — M3.1: prenos `EntityNPCInterface` in `noppes/npcs/ai/**` brez funkcionalnih sprememb

**Paket:** M3.1 · **Stanje:** preneseno in preverjeno na ravni bytecode; zagon v svetu čaka na uporabnika.
M2.6 baseline po navodilu uporabnika odložen (koda je pripravljena).

**Narejeno:** 37 razredov iz `reference-src` v `src/patch/java` (1 + 36 v `ai/`), plus
`net/minecraft/world/RwWorldAccess.java`. Rekonstrukcijski popravki (samo ti):

- `dataManager.register/set(X, (Object)v)` → brez `(Object)` (19× v `EntityNPCInterface`, 2× `CombatHandler`); `set(Walking, (!noPath() ? 1 : 0))` → `!noPath()` (CFR je boolean izpisal kot int)
- surovi `List`/`ArrayList` pri for-each → tipizirani (4×); `Object event` → `NpcEvent.TargetEvent`
- `EntityAIAvoidTarget`, `EntityAIStalkTarget`: CFR je spremenljivko dodelil samo v eni veji ternarnega izraza → rekonstruirano po `javap` (komentar v kodi)
- `EntityAIOpenAnyDoor`: `ImmutableSet` → `ImmutableSet<IProperty<?>>`
- `world.pathListener` je v dev `protected` (v igri ga naredi javnega `cnpcs_at.cfg`) → `RwWorldAccess.pathListener(world)` v istem paketu, brez refleksije

**Preverjeno v seji:** vseh 74 razredov `src/patch` prevedenih z `javac --release 8` proti
mapiranim MC razredom in pravim knjižnicam iz uporabnikovega gradle predpomnilnika (guava 21,
netty 4.1.9, authlib, log4j; dostop odobren 23. 9., samo branje). **Bytecode primerjava z
originalom** (`dev/tools/primerjaj-bytecode.py`, izid `audit/m31-bytecode-primerjava.txt`):
37/37 razredov, 558 metod — 508 enakih, 38 razlik samo v obliki prevajalnika, 12 ročno
pregledanih in enakovrednih (verizne dodelitve, združeni returni, tip iteratorja, namerni
`RwWorldAccess`).

**Ni preverjeno:** nalaganje v igri. Build, `verify-package.ps1` (pričakovan nov seznam
razredov: 37 prenesenih + `RwWorldAccess`), `testworld-run`, `r1-run` in `nav-run` morajo ostati zeleni.

**Spremembe obnašanja:** nobene.

**Zagon v svetu (23. 9., 14:51–14:55):** build z novimi razredi (`customnpcs-patch-classes`: 99 razredov, med njimi 36 `ai/`, `EntityNPCInterface`, `RwWorldAccess`); `testworld-run` zelen (8× `TW-OK`, `TW-SCRIPT-OK`); `r1-run` prišel do `R1-SUM mount=8`, brez napak, in **ponovi izid 17. 9.**: proga M obtiči (razpon 7,00, do cilja 24,3), proga S se razleti (razpon 33). `verify-package.ps1` ni zapisal novega poročila (`audit/package-verification.txt` je od 11. 9.) — še ni preverjeno.

**Naslednja seja:** `verify-package.ps1`, nato M3.2 (diagnoza R1).

### 2026-09-23 (48) — preverba spawna z `nogui`: hipoteza ovržena, D-017

Idle 500, 20 + 60 s, `nogui` deluje: GC 2,2 / 2,0 ms na sekundo, brez polnih zbirk.
Naenkrat: TPS 14,5, p50 14,2, p95 206 ms, 291 tickov > 50 ms. Razpršeno: TPS 13,7, p50 16,5,
p95 218 ms, 275 tickov > 50 ms. **Razlike ni** in počasni ticki so enakomerno po fazah
`tick % 10` → sinhroni `ticksExisted % 10` ni vzrok. **D-017: baseline samo s spawnom naenkrat.**
Odprto: ~23 % tickov nad 100 ms brez GC, poti in autosava (µs/NPC 136–144). Pri prvem zagonu
M2.4 (11:51) je bila ista celica µs/NPC 58 in TPS 20 — **razlika med zagoni je večja od
razlike med načini**, zato baseline zahteva miren računalnik (brez IDE, brskalnika).

### 2026-09-23 (47) — preverba spawna: meritev je merila okno serverja, ne moda

Idle 500, 20 + 60 s. Naenkrat: TPS 9,4, p50 14,7 ms, p95 377 ms; razpršeno: TPS 8,5, p50 17,3 ms,
p95 394 ms. **Obe neveljavni:** 118–121 polnih GC v 60 s (2/s, vsak s parom mlade zbirke),
354 ms GC na sekundo, stara generacija po GC pa samo 119 MB od 1820. To je `System.gc()`:
okno dedicated serverja (`StatsComponent`) ga kliče vsakih 500 ms (`StatsComponent.java:30,43`).
Popravek: `runServer` dobi `nogui` (`dev/build.gradle`). JvmProbe je to ujel v prvem zagonu.
**Posledica:** vse dosedanje meritve MSPT (M2.1, M2.7, M2.4) so tekle z oknom in vsebujejo
dve polni zbirki na sekundo; pred baselinom jih ne primerjati. Odločitev o spawnu (D-017) ni
mogoča iz teh dveh zagonov — preverba se ponovi z `nogui`.

### 2026-09-23 (46) — M2.6: GC/alokacije, baseline skripta, preverba sinhronega spawna

**Paket:** M2.6 · **Stanje:** koda narejena in preverjena v seji; zagon čaka na uporabnika

**Narejeno:** `rework/diag/JvmProbe.java` (+6 ključev v `DiagKeys`, klic v `Diag.reset`/
`snapshot`/`setEnabled(false)`), `JvmProbeTest` (5 testov); `perf-run.ps1`: P8, veličine GC
in alokacij, `-Razprseno`, `-SerijaDir`, `-Counts 50,200` iz ukazne vrstice; `baseline-run.ps1`
(B1–B3, mediana/razpon po celicah, `audit/m26-baseline-*.json`, `docs/meritve/baseline-*.md`);
scenarij `docs/scenariji/M2.6-baseline.md`.

**Preverjeno v seji:** 20 razredov `rework/**` prevedenih z `javac --release 8` proti
mapiranim razredom (D-014); **95 testov instrumentacije zelenih** (`Diag`, `DiagSnapshot`,
`DiagChunkPlan`, `Distribution`, `SlowTicks`, `NavProbe`, `JvmProbe`). `JvmProbe` na pravem
JVM-u: 64 MB alokacij izmerjenih kot 67,3 MB, `System.gc()` viden v `jvm.gc` in `jvm.gc.old`.
`perf-run.ps1` in `baseline-run.ps1` sintaktično čista (PowerShell 7.4.6); `baseline-run.ps1`
pognan nad ponarejenim `perf-run.ps1` (3 ponovitve × 4 celice): B1–B3 zelena, mediana in
razpon pravilna. Razpršen spawn da iste položaje kot spawn naenkrat (boj 500: 100 ukazov).

**Ugotovitve:**

- **`-Counts 50,200` prek `-File` pride kot en niz** in se pretvori v `50200`. Obe skripti
  zdaj vhod razbijeta sami. Ujel ga je šele test `baseline-run.ps1` nad ponarejeno skripto.
- **Popravek istega dne:** `[string[]]$Counts` ostane tipiziran tudi po prireditvi, zato je
  bil `500` niz in `'500' -gt 1000` je v primerjavi nizov resničen: `perf-run.ps1 -Counts 500`
  je padel z "Več kot 1000 NPC-jev". Parameter je zdaj `$CountsIn` (alias `-Counts`),
  `$Counts` je `[int[]]` (obe skripti). Test nad ponarejenim `perf-run` tega ni ujel, ker je
  ponaredek imel svoj param blok.
- **`ticksExisted` se ne shranjuje v NBT**, zato so po restartu vsi ob zagonu naloženi NPC-ji
  v isti fazi `% 10`. Sinhroni spawn scenarija je torej realen za zagon serverja, razpršen za
  NPC-je, ki pridejo s chunki kasneje.

**Spremembe obnašanja:** nobene (instrumentacija je privzeto izklopljena, D-007).

**Naslednja seja:** oceniti preverbo spawna, zapisati D-017, pognati baseline.

### 2026-09-23 (45) — M2.4 pognan: P1–P7 zelena, idle ni prost

**Paket:** M2.4 · **Stanje:** končano (brez M2.4r)

Preverbeni zagon (20 s + 60 s, en zagon) je v vseh 9 celicah zelen. Številke niso baseline.
Idle 500: MSPT p50 9,7 ms, p95 98,6 ms, 353 tickov nad 50 ms brez iskanja poti in brez
autosava; µs/NPC raste z N (27 → 58). **Hipoteza:** vsi NPC-ji celice so spawnani v istem
ticku, zato periodično delo (`ticksExisted % 10`) pade v isti tick — lahko artefakt
scenarija. Odločiti pred M2.6. [Zapis](../meritve/2026-09-23-M2.4-obremenitve-preverba.md).

**Naslednja seja:** M2.6 — najprej preverba spawna z zamikom (ugotovitev 2), nato baseline po protokolu z GC.

### 2026-09-23 (44) — M2.4: merilne obremenitve 50/200/500, brez zagona

**Paket:** M2.4
**Stanje:** koda, fixture in scenarij narejeni in preverjeni v seji; zagon v svetu čaka na uporabnika. Render (M2.4r) odprt.

**Narejeno:**

- **`perf-run.ps1`** — celice varianta × N v enem serverju (zagon A pribije spawn, zagon B
  meri). Na celico: slay, `noppes clone grid`, `rwdiag chunks on`, krmilnik, ogrevanje,
  merjenje, posnetek, krmilnik še enkrat, branje posnetka `.json`. Merila **P1–P7**, zapis
  celice prek `meritve-lib.ps1`, poročilo `audit/m24-perf-<čas>.md`. Privzeto po protokolu
  (120 s + 300 s), `-Seconds`/`-WarmupSeconds` za hitro preverbo.
- **Fixture** `PERF_Idle`, `PERF_BojA`, `PERF_BojB` (frakciji 1/2, 100000 HP, udarec 1, ranljiva — predloge M0.6 so `Invulnerable`),
  `PERF_Skripte`, `PERF_Kontrola` — naredi jih **`dev/testworld/perf-fixture.py`** iz
  `T_Stand`/`T_Scripted`; vsaka zamenjava mora zadeti natanko enkrat.
- **`perf-kontrola.js`** (frakciji sovražni, štetje po imenu, števec skript, `despawn`) in
  **`perf-skripte.js`** (tipična lahka skripta na `tick`).
- `perf-setup-commands.txt` (gamerule, `maxEntityCramming 0`, spawn).
- `ponovitve-run.ps1 -Scenarij perf` za ponovitve ene celice.
- Scenarij `docs/scenariji/M2.4-obremenitve.md`.
- Isto sejo: oznaka `dev\run\world\rework-scenarij.txt` v `nav-run.ps1` in
  `rwdiag-run.ps1` (tudi `perf-run.ps1` jo zapiše in preveri).

**Preverjeno v seji:**

- Vseh pet `.ps1` (perf, ponovitve, nav, rwdiag, meritve-lib) brez sintaktičnih napak
  (PowerShell 7.4.6 `Parser::ParseFile`).
- Ukazi za spawn vseh devetih celic izpisani in preverjeni proti prostoru (500 v boju do z = 11).
- Bralniki `Read-Chunks`, `Read-Kontrola`, `Read-Frakcije`, `Read-DumpJsonPath`, `Get-Dist`
  pognani nad ponarejenim logom z dvojno vrstico `[FINE/CustomNPCs]` (ta se ne šteje) in nad
  posnetkom v obliki prave `.json` datoteke.
- Obe skripti sta bili po vložitvi pognani v `node` nad lažnim API-jem: krmilnik nastavi
  frakciji, prešteje po imenu, izpiše obe vrstici in se odstrani enkrat, ne ob vsakem ticku.

**Ugotovitve:**

- **Slay pred budnim svetom ne odstrani ničesar.** `noppes slay npcs` samo označi `isDead`;
  brez igralca in brez prisilnih chunkov `updateEntities` po 300 tickih ne teče, mrtvi NPC-ji
  ostanejo v `loadedEntityList` in `DiagChunkLoader.scan` jih šteje. Zato se chunki vklopijo
  pred prvim slayem in ostanejo vklopljeni čez vse celice.
- **Privzete frakcije se ne bojujejo med sabo** (`attackFactions` je prazen). Boj brez
  nastavitve frakcij bi meril dve mirujoči skupini z drugim imenom.
- **Posnetek `.json` že ima** `world.npc.loaded`, `world.npc.killed` in `npc.update.window`
  (čas posodobitev NPC-jev); iz tega sta merilo P4 in veličina `npc.us` brez nove kode v `rework/diag`.

**Ni narejeno in zakaj:**

- **Render (M2.4r):** dedicated server nima izrisa; potrebuje klient z igralcem.
- **Alokacije/GC:** `rwdiag` jih ne meri; spada v M2.6, preden se baseline zapiše.

**Spremembe obnašanja:** nobene (samo scenarij in fixture).

**Meritve:** nobene (zagon čaka).

**Naslednja seja:** `.\testworld.ps1`, `.\perf-run.ps1 -Seconds 60 -WarmupSeconds 20`; ob
zelenem zagonu M2.6.

### 2026-09-21 (43) — M2.7b pognan: razdelitev dela, šum pa je drugje, kot sem mislil

**Paket:** M2.7b (ovrednotenje dveh zagonov in popravek)
**Stanje:** **delno.** Razdelitev na hladno in ogreto je pravilna in preverjena, hipoteza o
vzroku šuma pa je bila napačna. Popravek (ogrevanje iskalnika) je narejen in čaka na zagon.

**Narejeno:**

- Ovrednotena zagona `.\nav-run.ps1` ob 12:23 (N1–N14 zelena) in 12:28 (padlo N14) ter zagon
  `.\rwdiag-run.ps1` ob 12:55 (neveljaven).
- **Ogrevanje iskalnika** v `nav-run.ps1`: `-OgrevalnihPometanj` (privzeto 2 na progo), izid
  se zavrže, nato `rwdiag reset`. Novo merilo **N15**.
- **N14 prepisan** iz časovnega praga v strukturno invarianto.
- `rwdiag-run.ps1`: takojšen padec z navodilom, če v svetu ni `T_Scripted`; `odtis.npc` ne
  nosi več kumulativnih NPC-tickov.
- Meritev [`2026-09-21-M2.7b-ogrevanje.md`](../meritve/2026-09-21-M2.7b-ogrevanje.md) in razdelek
  M2.7b v scenariju.

**Ugotovitve:**

- **Razdelitev se sešteje povsod:** `prviN = iskanj = 8` in `ponN = ponovitev = 56` v vseh
  osmih pometanjih obeh zagonov. Vse, kar ni čas, je med zagonoma identično — `delezCelih`
  1,000, razmerje 1,151 / 1,236, prispelo `1/8 ob 180/180/180` in `6/8 ob 220/240/280`, razpon
  pri grlu 3,78 / 3,92 / 4,78 / 4,76.
- **Šumni pas se ni zaprl in se je pri eni veličini razširil:** `O.ponP50.poA` 254,0 proti
  42,0 µs, torej 143 % proti 114 % pred M2.7b.
- **Vzrok je stanje JVM-a med pometanji, ne število vzorcev.** `usSkupaj` zadnjih dveh
  pometanj: 9.069 in 18.076 µs (12:23) proti 4.579 in 2.939 µs (12:28). V enem zagonu se je
  JIT do konca ogrel, v drugem ne. Vseh 56 ogretih vzorcev enega pometanja si deli isto stanje
  JVM-a, zato večji vzorec zmanjša šum znotraj pometanja, ves pomemben šum pa je med njimi.
- **Razdelitev kljub temu ni bila odveč:** brez nje in brez `usSkupaj` bi bila ta razlaga
  skrita v mešanici. Meritev je ovrgla hipotezo in hkrati dala orodje, ki pove, zakaj.
- **Merilo N14 sem napisal napačno.** Časovni prag med dvema enako ogretima številkama ni
  merilo: predznak razlike je naključen (73,7 proti 69,6 pade, 42,0 proti 44,0 ne). Merilo
  veljavnosti mora biti invarianta, ki drži po konstrukciji.
- **Zagon rwdiag je meril napačen svet.** `world.npc.loaded = 17` — NAV prizorišče, ki ga je
  pustil `nav-run.ps1`, brez `T_Scripted`. Merila S1–S7 še vedno niso ovrednotena. To je tretji
  primer te vrste napake ta teden.

**Ni narejeno in zakaj:**

- Ali ogrevanje šumni pas res zapre, pove šele serija po enem zelenem zagonu.
- S1–S7 čakajo na pravilno zaporedje `.\testworld.ps1` → `.\testworld-run.ps1` → `.\rwdiag-run.ps1`.
- M2.4 in M2.6 nista začeta.

**Spremembe obnašanja:** nobene v modu. Spremenjeni sta merilni skripti; `rework/diag` je
tokrat nedotaknjen.

**Preverjeno brez sveta:** `Parser::ParseFile` na `nav-run.ps1` in `rwdiag-run.ps1` brez napak;
**novi N14 pognan nad resničnim logom zagona 12:28** — zelen v vseh štirih pometanjih, medtem
ko stara oblika pade v tretjem; ločitev ogrevalnih od merjenih pometanj preverjena nad
sintetičnim nizom osmih pometanj (ogrevalna G#0, G#1, O#0, O#1; merjena G#2, G#3, O#2, O#3).

**Naslednja seja:** `.\nav-run.ps1` (N1–N15, štiri merjena in štiri ogrevalna pometanja;
prebrati `5 cena celega pometanja` za obe merjeni — če sta si blizu, je ogrevanje zadostovalo),
nato `.\ponovitve-run.ps1 -Scenarij nav`. Ločeno: `.\testworld.ps1`, `.\testworld-run.ps1`,
`.\rwdiag-run.ps1` za S1–S7.

---

### 2026-09-21 (42) — M2.7b: peta veličina razdeljena na hladno in ogreto

**Paket:** M2.7b (koda in testi)
**Stanje:** **koda narejena, čaka na zagon.** Prevedeno in testirano v seji; v svetu še ne.

**Narejeno:**

- **`NavProbe` ima tri časovne porazdelitve namesto ene:** `firstNanos` (prvo iskanje na
  NPC, hladno), `repeatNanos` (ponovitve, ogreto) in `searchNanos` (vsota obojega, ostane
  zaradi že zapisanih meritev). Nova polja v vrstici sonde: `prviN`, `prviP50`, `prviP95`,
  `ponN`, `ponP50`, `ponP95`, `ponMax`, `usSkupaj` — vsa **na koncu** vrstice, da stari del
  regexa in že zapisane meritve berejo isto kot prej.
- **`NavSweep.DEFAULT_REPEATS` 3 → 8.** Pri osmih NPC-jih to da 8 hladnih in **56** ogretih
  vzorcev na pometanje namesto 8 in 16.
- **`nav-run.ps1`:** `-SweepRepeats` privzeto 8, branje novih polj, merili **N13** (vsaj 40
  ogretih vzorcev) in **N14** (ogreto ni počasnejše od hladnega), nove vrstice v izhodiščni
  tabeli in nove veličine v strojnem zapisu za `ponovitve-run.ps1`.
- Razdelek **M2.7b** v scenariju M2.7; 6 novih testov v `NavProbeTest` (21 → 27).

**Ugotovitve:**

- **Vzrok šuma ni bil samo premalo vzorcev, ampak dve populaciji v eni posodi.** Prvo
  iskanje na NPC plača nalaganje razredov in hladen JIT, ponovitev meri algoritem. Pri 8
  hladnih in 16 ogretih vzorcih je bil `usP95` praktično *drugo najpočasnejše hladno iskanje
  od osmih* — zato razpon do 114 % med sicer determinisičnimi ponovitvami serije (M2.5c).
- **To se vidi na enačbi, ne šele v svetu.** Na preverbeni vrstici, ki jo je izpisal
  prevedeni `NavProbe`, je mešani `usP95` 327,7 µs, ogreti `ponP95` pa 72,0 µs: mešanica
  poroča četrtkrat več, kot stane iskanje, ki ga A/B primerja.
- **Višje od 8 ponovitev se ne splača.** Pometanje teče v enem samem server ticku, zato vsako
  dodatno iskanje pokvari meritev MSPT v istem zagonu. `usSkupaj` ta vpliv naredi merljiv —
  in s tem odpre odprto vprašanje iz M2.7, zakaj je bil najpočasnejši tick (232 ms) tick s
  pathfindingom.
- **Stare in nove serije se ne mešajo tiho.** Odtis nosi `pometanjPon`, zato mešana serija
  pade na T4, nove veličine v starem zapisu pa na T5 (M2.5c). To je namen.

**Ni narejeno in zakaj:**

- Ni še pognano v svetu. Ali se šumni pas res zapre, pove šele serija
  `.\ponovitve-run.ps1 -Scenarij nav` po enem zelenem `.\nav-run.ps1`.
- Merila S1–S7 še vedno čakajo na `.\rwdiag-run.ps1`.

**Spremembe obnašanja:** nobene v modu za igralca. Spremenjena je diagnostika (`rework/diag`),
ki teče samo ob `rwdiag on`.

**Preverjeno brez sveta:** vseh **14** razredov `rework/diag` prevedenih z `javac --release 8`
proti mapiranim razredom in `customnpcs-mapped-01Oct19.jar` (manjkajoča `javax.annotation.Nullable`
in `com.google.common.collect.ImmutableSetMultimap` nadomeščena z minimalnima nadomestkoma
**izven** repozitorija); **27/27 `NavProbeTest` zelenih**; `Parser::ParseFile` na `nav-run.ps1`
brez napak; **pogodba Java ↔ PowerShell preverjena nad resnično vrstico**, ki jo je izpisal
prevedeni `NavProbe` — regex jo ujame, vseh osem novih polj pristane v pravi skupini,
`Format-Sonda` jo izpiše in N13/N14 sta nad njo zelena.

**Naslednja seja:** pognati `.\nav-run.ps1` (pričakovano N1–N14 zelena, `ponN = 56` na
pometanje), nato serijo `.\ponovitve-run.ps1 -Scenarij nav` in preveriti, ali je razpon
ogretih veličin padel pod 20 %. Vzporedno je še vedno odprt `.\rwdiag-run.ps1` (S1–S7).

---

### 2026-09-21 (41) — M2.3 zaključen: L1–L12 zelena, P1 ovržen

**Paket:** M2.3 (četrti zagon)
**Stanje:** **zaključeno.** Scenarij je veljaven v vseh štirih fazah, R2 je izmerjen, P1 je
razsojen z meritvijo in ne s sklepanjem.

**Narejeno:**

- Ovrednoten **četrti zagon** (21. 9. ob 07:45, `audit/m23-r2-2026-09-21-0745.md`).
- V meritev dopisan razdelek „Četrti zagon 21. 9. ob 07:45".
- V scenariju označeno, da je hipoteza faze D ovržena; M2.3 zaprt v tabelah stanja.

**Ugotovitve:**

- **L1–L12 zelena.** Vseh dvanajst prog ima `prevozeno + doCilja` prvega vzorca med 16,41 in
  18,06 (v zagonu 13:49 sta bili dve pri 10,94 in 11,88). Premik postavitve iz `endPhase` v
  `startPhase` je razhod odpravil, L12 ga nadzira.
- **Razsodba je prvič prišla v poročilo:** `P1 OVRZEN: proga P se premika na mrezi (17,63)
  in izven nje (17,67, gib=0,2734)`.
- **Hipoteza `pathFollow` 0,45 proti `FlyingMoveHelper` 0,5 je ovržena.** Faza D je začela
  natanko na z = −16,0; leteči NPC-ji so se premikali že v prvem vzorcu (`gib = 0,1183`) in
  fazo končali pri 15,93 prevoženih z `cele = 5/6`. Odmik pol bloka letečega NPC-ja ne ustavi.
- **Faza C proge P je prvič veljavna** (`cele = 6/6`, prevozi 16,34); v zagonu 13:49 je bila
  onesnažena in njenih številk ni bilo dovoljeno uporabljati.
- **R2 potrjen dvakrat zapored.** Faze A, B in C se med zagonoma 13:49 in 07:45 ujemajo tam,
  kjer sta oba veljavna: proga F čez zid 0/6 pri enem `navigateTo`, 6/6 ob osveženi poti,
  `cele = 6/6` pri vanilla AI. Ugotovitev „R2 je zastarela delna pot" stoji na dveh zagonih.
- **R-P1b — kaj je od P1 ostalo.** Stari `resetToStart` je postavljal na **iste koordinate**
  kot današnji `resetOffGrid`; edina preostala razlika je bila, da se je postavitev zgodila
  40 tickov pred fazo. Sama koordinata je oproščena (faza D 07:45) in sam razmik tudi
  (zagon 13:49, z = −15,5) — zmrznitev je zahtevala **oboje hkrati**. Recept je zapisan v
  meritvi; poskus (faza E) se požene šele, če ga M4 potrebuje.

**Ni narejeno in zakaj:**

- R-P1b ni razsojen. Ni blokade in M2.3 je dostavil, čemur je bil namenjen; dodajati peto
  fazo v zdaj zelen diagnostični scenarij za nevtralno vprašanje bi bilo širjenje obsega.
- Merila S1–S7 (M2.5a/b) še niso bila strojno ovrednotena — `.\rwdiag-run.ps1` ostaja odprt.
- M2.4, M2.6 in M2.7b niso začeti.

**Spremembe obnašanja:** nobene. Ta seja je samo brala in dokumentirala.

**Meritve:** [`meritve/2026-09-17-M2.3-R2-reprodukcija.md`](../meritve/2026-09-17-M2.3-R2-reprodukcija.md), razdelek „Četrti zagon ob 07:45"

**Naslednja seja:** pognati `.\rwdiag-run.ps1` (merila S1–S7, odločitveno drevo je v razdelku
M2.5a scenarija), nato **M2.7b** ali **M2.4**.

---

### 2026-09-21 (40) — M2.3 tretji zagon ovrednoten: R2 je zastarela pot, P1 še ne odločen

**Paket:** M2.3 (ovrednotenje zagona 13:49) + združitev vej
**Stanje:** **delno.** R2 je reproduciran in izmerjen, kar je bil namen paketa. P1 ostaja
odprt, ker je faza D padla na vhodnem pogoju; popravek je narejen, zagon je na uporabniku.

**Narejeno:**

- **Združitev vej.** Lokalni commit `c0ca7e1` (delo 17. 9. na namizni postaji) je bil
  rebasean na `origin/main`, ki je medtem odšel 11 commitov naprej (M2.5b, M2.5c, M2.7).
  Konflikt je bil samo v tem dnevniku; vnos 17. 9. je dobil številko 39, ker je bila 30 na
  `origin/main` že zasedena.
- Ovrednoten **tretji zagon** (13:49, `audit/m23-r2-2026-09-17-1349.md`, log
  `audit/m23-r2.log`), ki ga prejšnja seja ni videla — datoteka je bila nesledena.
- V meritev dopisan razdelek „Tretji zagon 17. 9. ob 13:49".
- **`r2-control.js`:** postavitev NPC-jev se je preselila iz `endPhase` v `startPhase`, tik
  pred `rememberStart`.
- **`r2-run.ps1`:** novo merilo **L12**, popravljena razsodba o P1 in popravljen razdelek
  `## Razsodba o P1`, ki prej sploh ni prišel v poročilo.
- Skripta vstavljena v oba klona `R2_Control.json` (7556 znakov).

**Ugotovitve:**

- **Zmrznitve iz zagonov 12:17 in 12:37 v tretjem zagonu ni nikjer.** `prevozeno` ni 0,00 v
  nobeni fazi in progi, `gib` je povsod nad nič. Edina sprememba je bil popravljeni
  `resetToStart`.
- **R2 je reproduciran in obhod izmerjen.** Leteča proga F pride čez zid **0/6** pri enem
  samem `navigateTo` (faza A, `cele = 0/6`, prevozi 7,95), **6/6** ob osveženi poti (faza B,
  prevozi 16,36) in `cele = 6/6` pri vanilla AI (faza C). Kopenska kontrola ne pride čez v
  nobeni fazi (L5 zelen). Razlika med A in B/C je **ena sama: ali se pot osvežuje.**
- **Zato R2 v tem prizorišču ni okvara letenja, ampak zastarela delna pot** — isti
  mehanizem kot Q11 pri M2.2, le da ga tam povzroči domet in tu ovira. Popravek v M4 mora
  osveževati pot, ne spreminjati `FlyingMoveHelper`.
- **Faza D P1 ne razsodi, ker se ni začela tam, kjer bi se morala.** Med `R2-C-END`
  (tick 1460) in `R2-D-START` (tick 1500) mine 40 server tickov; v njih se je prosta leteča
  proga premaknila 7,4 bloka nazaj proti cilju, čeprav je `endPhase` pred postavitvijo
  poklical `clearNavigation()` in `setAttackTarget(null)`. Onesnaženi sta natanko progi z
  neovirano potjo do cilja (`C P` in `D P`); progi za zidom v 40 tickih nista prišli nikamor.
- **Vsota `prevozeno + doCilja` prvega vzorca to pokaže brez ugibanja.** Deset čistih prog je
  med 16,39 in 17,34, obe onesnaženi pa pri 10,94 in 11,88. Iz tega je nastalo merilo L12.
- **Posledica za že zapisane številke:** proga P v fazi C zagona 13:49 je prav tako
  neveljavna in se ne sme uporabljati. Prej tega ni bilo videti.
- **Dve napaki orodja.** Razsodba o P1 sploh ni prišla v poročilo (blok je bil dopisan v
  `$head` pred izračunom `$p1` in pred `$head = @()` v koraku 9), merilo razsodbe pa je
  merilo „ali je proga prišla do cilja" (`prevozenoMax >= 12`), ne „ali je zmrznila".

**Ni narejeno in zakaj:**

- P1 ni razsojen. Za to je potreben četrti zagon na Windowsu.
- M2.4, M2.6 in M2.7b niso začeti; merila S1–S7 (M2.5a/b) še niso bila strojno ovrednotena.

**Spremembe obnašanja:** nobene. Spremenjena sta scenarij in merilna skripta, ne mod.

**Meritve:** [`meritve/2026-09-17-M2.3-R2-reprodukcija.md`](../meritve/2026-09-17-M2.3-R2-reprodukcija.md), razdelek „Tretji zagon ob 13:49"

**Preverjeno brez sveta:** `node --check` (vseh 6 skript testnega sveta),
`Parser::ParseFile` na `r2-run.ps1` (0 napak, PowerShell 7.4.6 v oblačnem okolju),
`preveri-skladnost.py` (SKLADNOST OK), `preveri-markerje.js` (264 vzorčnih vrstic, POGODBA OK),
`vstavi-skripto.py` na obeh klonih (7556 znakov, ujemanje preverjeno).

**Naslednja seja:** pognati `.\r2-run.ps1` četrtič in prebrati razdelek `## Razsodba o P1`
iz poročila; če L12 pade, je onesnažena še katera proga in izid te faze ne velja. Nato
`.\rwdiag-run.ps1` (merila S1–S7) in **M2.7b** ali **M2.4**.

---

### 2026-09-17 (39) — P1 zožen na `FlyingMoveHelper`; poskus s pol bloka pripravljen

*(delo s 17. 9. na namizni postaji; v `origin/main` združeno 21. 9., zato je številka vnosa 39 in ne 30)*

**Paket:** M2.3 (razčlenitev pojava P1)
**Stanje:** **delno.** Dve od treh razlag sta ovrženi z meritvijo, tretja ima kodno podlago
in poskus, ki jo potrdi ali ovrže. Zagon je na uporabniku.

**Narejeno:**

- Ovrednoten drugi zagon (12:37, `audit/m23-r2-2026-09-17-1237.md`), z `dStarost` in `gib`.
- V meritev dopisan razdelek „Ponovitev 17. 9. ob 12:37 — P1 dobi odgovor".
- **Popravljen `resetToStart`**: NPC se vrne na zapomnjeno izhodišče, ne na konstanto
  `START_Z`. To je bila tiha napaka scenarija, ne moda.
- **Nova faza D** v `r2-control.js`: enaka fazi B, samo izhodišče je pol bloka izven mreže
  (`resetOffGrid`) — torej natanko stanje obeh dosedanjih zagonov.
- `r2-run.ps1`: faza D v seznamu faz, merilo **L11** (kopenska kontrola v fazi D) in
  razsodba `P1 POTRJEN / OVRŽEN / NEODLOČEN`, ki jo skripta izpiše sama in zapiše v
  poročilo.
- `preveri-markerje.js` razširjen na fazo D (markerja `R2-D-START`/`R2-D-END`, 264 vzorčnih
  vrstic).

**Ugotovitve:**

- **`dStarost` je 20 v vseh fazah in progah.** Svet entitete posodablja ves čas; razlaga
  „svet jih ne posodablja" je **ovržena**.
- **`gib` je točno 0** v vseh 22 vzorcih obeh zmrznjenih faz, na istih NPC-jih pa je v fazi A
  dosegel 0,2306. Razlaga „gibanje je, a ga poje `move()`" je **ovržena**. Ostane
  `FlyingMoveHelper`, ki gibanja ne doda.
- **Drugi zagon je znak za znak enak prvemu** v vseh starih veličinah. Scenarij je
  determinističen, zato je primerjava med zagonoma veljavna.
- **Iz samih številk se da prebrati, kje je razlika.** `doCiljaMin` po resetu je 16,01, kar
  ustreza z = −16,0 (meja bloka); v fazi A se izide samo z = −15,5 (sredina bloka). Reset je
  NPC-je premikal za pol bloka, česar ni načrtoval nihče.
- **Kodna podlaga hipoteze:** `PathNavigate.pathFollow:286-292` prestevilči vozlišče samo pri
  odmiku < 0,45 (`0,75 − width/2`, `width = 0,6` iz `updateHitbox:1083-1086`), medtem ko
  `FlyingMoveHelper:39` doda gibanje samo pri `d3 > 0,5`. Odmik **točno 0,5** ne zadosti ne
  enemu ne drugemu. `EntityMoveHelper` takega praga nima — zato kopenska proga teče naprej.
- Če se to potrdi, P1 ni napaka scenarija, ampak **bug originala**, in ne velja samo za
  teleport: enako obtiči leteči NPC, ki se ustavi na taki koordinati po prihodu na cilj ali
  po spawnu. Takrat gre v M4 skupaj z obhodom ovire.

**Ni narejeno in zakaj:**

- Hipoteza ni potrjena. Za to je potreben zagon na Windowsu.
- Merili L4a in L4b bosta v naslednjem zagonu povedali tudi, ali je popravek reseta zalegel;
  če proga P v fazi B še vedno ne pride do cilja, vzrok ni bilo izhodišče.
- M2.4 in M2.7 nista začeta.

**Spremembe obnašanja:** nobene. Spremenjen je scenarij, ne mod.

**Meritve:** [`meritve/2026-09-17-M2.3-R2-reprodukcija.md`](../meritve/2026-09-17-M2.3-R2-reprodukcija.md), razdelek „Ponovitev ob 12:37"

**Preverjeno brez sveta:** `node --check`, `vstavi-skripto.py` (7445 znakov, oba klona),
`Parser::ParseFile` na `r2-run.ps1` in `matrika-run.ps1`, `preveri-markerje.js` s fazo D
skozi `Read-Samples` (264 vzorcev, 4 faze × 3 proge po 22), `preveri-skladnost.py`.

**Naslednja seja:** pognati `.\r2-run.ps1`, prepisati razsodbo o P1 v meritev, nato **M2.4**
ali **M2.7**.
### 2026-09-18 (39a) — M4.14a: formacije v Javi (skupina NPC-jev kot ena enota)

*Integrirano na `origin/main` (M3.6) 27. 9.: ta vnos je nastal na lokalni veji 18. 9. in ni bil pushan; odločitev preštevilčena D-017 → D-021.*

**Paket:** M4.14a (nov, izven vrstnega reda faz — izrecna zahteva uporabnika)
**Stanje:** **koda in testi narejeni; v svetu ni pognano** (M4.14b čaka na build in zagon)

**Ozadje:** uporabnik vodi vojsko NPC-jev s chat skripto (`legija`, `obramba`, `march`), ki
premik izvede s timerji na igralcu in `navigateTo` za vsakega NPC-ja posebej. Vprašal je,
ali bi v Javi lahko marširali skupaj, in naročil zasnovo, vpis v načrt in prototip. Paket je
pred M2 zaključkom, zato ga protokol (§2 "ne začenjaj paketa iz kasnejšega milestona")
sicer ne bi dovolil; izjema je zapisana v D-021.

**Narejeno:**

- Zasnova [`06-FORMACIJE.md`](../06-FORMACIJE.md): šest vzrokov, zakaj skripta razpade (z
  mesti v kodi), model sidra in mest, zaprta zanka, ozko grlo, faze, parametri, kaj je in
  kaj ni preverjeno.
- Paket `rework/formation` (15 datotek). Jedro brez Minecrafta: `SquadPlanner`,
  `FormationShape` (legija/obramba/kolona/march z razmiki iz skripte), `SlotAssigner`
  (dodelitev brez križanja), `PathTrack`, `FormationMath`. Adapter: `Squad`,
  `FormationMoveTask` (prioriteta −1, maska `PASSIVE|LOOK`), `SquadManager` (na event bus
  samo, dokler obstaja enota), `WorldTerrain`, ukaz `/rwsquad`, `FormationApi` za skripte.
- `CustomNpcs.java`: registracija `/rwsquad` in `SquadManager.clear()` ob ustavitvi.
- `build.gradle`: nova testa izključena iz `testOriginal`.
- Scenarij [`M4.14-formacije.md`](../scenariji/M4.14-formacije.md): dimni test F1–F12 in A/B
  proti skripti na prizorišču M2.7 (FA1–FA6).
- Predelana uporabnikova chat skripta [`formacije/vojska-chat.js`](../formacije/vojska-chat.js)
  (preverjena z `node --check` in z nadomestnim svetom v Node).
- `03-FAZE.md` (M4.14, M4.14a–c), `01-ARHITEKTURA.md` (D-021), `README.md`.

**Preverjeno v seji (D-014):** celotno drevo `rework/**` in popravljen `CustomNpcs.java` se
prevedeta z `javac --release 8` proti mapiranim razredom (za obstoječo kodo sta bila potrebna
nadomestka `GameProfile` in `ImmutableSetMultimap` izven repozitorija). 21 novih testov
zelenih. Simulacija pokaže: 20 članov prehodi 80 blokov, noben med pohodom ne zaostane več kot
5,5 bloka; sidro se prilagodi najpočasnejšemu; obtičan član enoto najprej ustavi, po 80
tickih je opravičen in enota gre naprej; pred vrati se formacija stisne in za njimi razpre;
delna pot se nadaljuje; končna mesta so na sredini blokov in unikatna.

**Ni narejeno in zakaj:**

- Build, zagon in dimni test v svetu — seja nima Gradla ne Minecrafta; to je M4.14b.
- `nav-control.js` še nima načina `FORMACIJA` za A/B (FA1–FA6); naslednja seja.
- Leteči in plavajoči NPC-ji niso bili niti razmišljeni do konca: drug navigator in move
  helper; zapisano kot omejitev.

**Ugotovitve:**

- **`setMovingType(0)` v skripti pusti `EntityAIReturn` aktiven.** Ko NPC nima poti in ni
  na domu, gre domov (`EntityAIReturn.shouldExecute`, movingType 0 →
  `!isVeryNearAssignedPlace()`, meja ±0,2 bloka). Uporabnikovi NPC-ji se zato po prihodu
  vrnejo na staro mesto, razen če imajo izklopljen "return to start". Formacije to rešijo s
  `setStartPos` ob koncu.
- **`Walking` flag CustomNPCs je vezan na navigatorjevo pot** (`EntityNPCInterface:464`), od
  njega pa sta odvisna `addVelocity` (knockback) in render pri standing type 3. Zato člani
  hodijo prek poti z eno točko in ne prek golega `moveHelper.setMoveTo`.
- **Hipoteza, nereproducirana:** `testOriginal` izključi `DiagTest` in sorodne, ne pa
  `SlowTicksTest` in `NavProbeTest`, ki prav tako uporabljata razreda iz `rework/diag`. Če
  gradle `testOriginal` ni bil pognan od M2.5a, lahko pade z `NoClassDefFoundError`. Ne
  popravljam (ni moj paket); preveri naslednji `.\dev.ps1 testOriginal`.

**Spremembe obnašanja:** nov ukaz `/rwsquad` in `FormationApi`; brez klica nobene (tabela).

**Meritve:** nobene v svetu.

**Naslednja seja:** uporabnik naj požene `.\dev.ps1 buildPatchedMod --offline`,
`.\verify-package.ps1` (pričakovano: `CustomNpcs.class` spremenjen kot prej, novih 26 razredov
v `rework/formation`) in dimni test F1–F12. Nato način `FORMACIJA` v `nav-control.js` in A/B.

---

### 2026-09-18 (38) — prva serija ponovitev: scenarij M2.7 je determinističen, šumen je samo čas

**Paket:** M2.5c (zagon)
**Stanje:** **zaključeno** — serija je veljavna in zapisana

**Izid:** `.\ponovitve-run.ps1` (tri ponovitve, vsaka svež svet, 2,6 / 2,1 / 2,0 minute):
**T1–T6 zelena**, vsaka ponovitev tudi N1–N12 zelena, odtis enak v vseh treh.
Polni zapis: [meritev](../meritve/2026-09-18-M2.5c-ponovitve-nav.md).

**Glavna ugotovitev:** od 59 veličin jih ima **46 razpon nič**. Vse, kar opisuje vedenje
skupine in kakovost poti (veličine 1, 2, 3, 4 in 6), je med zagoni **bitno enako** — kdo
prispe, kdaj prispe, kako dolga je pot, kako se skupina stisne pred vrati. Za A/B v
M4.10–M4.13 to pomeni, da statistika ni potrebna: en zagon pred in en po, in vsaka razlika
je resnična.

**Šumna je izključno veličina 5** (µs na eno iskanje), vseh osem njenih oblik, najslabša
`O.usP95.start` z **114,3 %** razpona. Vzrok ni navigacija, ampak premalo vzorcev: eno
pometanje naredi **8 iskanj**, p50 in p95 pa sta percentila nad temi osmimi.

| | prag za A/B |
|---|---|
| veličine 1, 2, 3, 4 | vsaka sprememba je pomenljiva (razpon 0) |
| veličina 6 (dodelitev poti na tick) | ≥ 3,4 % |
| veličina 5, ogreto (p50) | ≥ 5 % na progi G, ≥ 47 % na progi O |
| veličina 5, hladno (p50) | ≥ 25 % na progi G, ≥ 64 % na progi O |

**Ugotovitve:**

- **Determinizem drži tudi čez dan in čez sejo.** Zagon 17. 9. (druga seja, drug svet) ima
  za iste veličine iste vrednosti: 1/8 ob 180/180/180, 6/8 ob 220/240/280, razpon 3,78 in
  4,78, `novihPoti` 372. To je četrta neodvisna potrditev iste številke.
- **Hladno pometanje meri JIT, ne algoritma** (`G.usMax`: 3 192 µs hladno proti 181 µs
  ogreto). Za A/B se uporablja ogreto pometanje; hladno ostane ločen podatek o ceni prvega
  iskanja.
- **M4.11 in M5.6 sta blokirana na meritvi, ne na kodi.** Oba ciljata ceno iskanja, torej
  edino veličino, ki v tej obliki ni merljiva. Zato nov paket **M2.7b**: več vzorcev na
  pometanje (ali skupni čas namesto percentilov).
- Protokol je v praksi poceni: cela serija traja 7 minut, ker `nav-run.ps1` sam traja dve.

**Ni narejeno in zakaj:** serija za `rwdiag-run.ps1` (merila S1–S7) ni tekla; zapis zagona
ima, požene pa se s `.\ponovitve-run.ps1 -Scenarij rwdiag`.

**Spremembe obnašanja:** nobene.

**Meritve:** [`meritve/2026-09-18-M2.5c-ponovitve-nav.md`](../meritve/2026-09-18-M2.5c-ponovitve-nav.md).

**Naslednja seja:** **M2.7b** (več vzorcev za veličino 5, da postane merljiva), nato
**M2.4** ali **M2.6**.

---

### 2026-09-18 (37) — M2.5c: protokol ponovitev in zapis zagona

**Paket:** M2.5c
**Stanje:** **zaključeno** (koda in samotest); prva serija ponovitev v svetu je na uporabniku

**Izhodišče:** tabela M2.7 je nastala iz **enega** zagona, zato nima razpona. Dokler ga
nima, se o razliki med dvema zagonoma ne da reči, ali je izboljšava ali šum — in po
`05-SEJA-PROTOKOL.md` je M4.10–M4.13 ni dovoljeno razglasiti za izboljšavo.

**Narejeno:**

- `meritve-lib.ps1` — strojno berljiv **zapis zagona** (`Write-MeritevJson` /
  `Read-MeritevJson`, shema 1) z dvema ločenima deloma: **odtis** (pogoji, pod katerimi je
  meritev nastala; med ponovitvami se ne sme razlikovati) in **veličine** (kar se meri;
  razpon čez ponovitve *je* merilni šum). Plus `Get-Mediana` in `Get-Sum`.
- `ponovitve-run.ps1` — N ponovitev, vsaka svež svet (`.\testworld.ps1`) in svoj proces;
  združevanje po imenih veličin, izpis šumnega pasu (mediana, razpon, relativni razpon,
  oznaka `enaka` / `stabilna` / `SUMNA`), poročilo v `audit\m25c-<scenarij>-<datum>.md`,
  merila **T1–T6**.
- `ponovitve-samotest.ps1` — 7 primerov, **16 trditev**, brez Minecrafta: ponarejeni
  scenarij zapiše vnaprej znane številke. Pokriva zeleno pot, obe oznaki, spremenjen odtis,
  premalo ponovitev, padel zagon in manjkajočo veličino.
- `nav-run.ps1` in `rwdiag-run.ps1` dobita `-JsonPath` in po poročilu zapišeta zapis zagona.
  Mapiranje imen je v `nav-run.ps1` izločeno v čisto funkcijo `New-NavZapis`, da se da
  preveriti brez Minecrafta.
- `docs/scenariji/M2.5c-ponovitve.md`.

**Preverjeno v seji (brez Minecrafta):**

- `.\ponovitve-samotest.ps1`: **16/16 zelenih** v oblačnem PowerShellu 7.4.6. Test je bil
  najprej pokvarjen namerno (`Get-Sum` vrne razpon 0) — takrat pade na oznakah `stabilna`
  in `SUMNA`, kar potrjuje, da trditvi nista prazni.
- `New-NavZapis` pognan nad **pravim logom zagona z 17. 9.** (`audit\m27-nav.log`, funkcije
  izluščene iz `nav-run.ps1` prek AST, brez podvajanja kode): 59 veličin in odtis
  `npc=16 progaG=8 progaO=8 vrataX=-10 zidZ=72 ciljZ=78 zidBlokov=140 pometanj=4`. Številke
  se ujemajo z objavljeno tabelo (1,151 / 1,236; 237,6 / 67,6 µs; 1/8 in 6/8; razpon 3,78 in
  4,78; `naTick` 0,313, max 16). Zapis se tudi prebere nazaj.
- Vseh pet skript sintaktično brez napak.

**Ugotovitve:**

- **Ponovitev brez reseta sveta ni ponovitev.** Svet v `dev\run\world` se med zagoni
  ohrani z vsemi NPC-ji vred; drugi zagon `nav-run.ps1` bi meril 32 NPC-jev namesto 16.
  Zato je reset del protokola, merilo T4 (enak odtis) pa ga ujame tudi, če odpove. Dobra
  novica: N2 v `nav-run.ps1` bi tak zagon tako ali tako podrl — napaka je glasna, ne tiha.
- **Odtis mora biti ločen od veličin.** Če sta v istem loncu, je mediana čez tri zagone
  lahko povprečje treh različnih poskusov in izgleda enako verodostojno kot meritev.
- **Prag 20 % je oznaka, ne pravilo.** Pravi prag za posamezno veličino je njen izmerjeni
  razpon; zato T6 ničesar ne podre, samo izračuna.
- Kjer je mediana 0 (npr. `ai.naTickP95`), relativni razpon ne obstaja; stolpec pove `n/a`
  namesto izmišljenega nadomestka.

**Ni narejeno in zakaj:**

- **Serija ponovitev ni pognana** — potrebuje Windows, Javo 8 in Minecraft. Tabela M2.7
  ostane enozagonska, dokler uporabnik ne požene `.\ponovitve-run.ps1`.
- `r2-run.ps1` zapisa zagona še nima; dobi ga, ko bo pojav P1 razrešen in bo scenarij spet
  dal veljavne številke (sicer bi zapisovali zapise neveljavnih zagonov).
- Ocena trajanja serije: trije zagoni `nav-run.ps1` zaporedoma, vsak s svojim resetom sveta.

**Spremembe obnašanja:** nobene v modu. Spremembe so samo v merilnih skriptah; `-JsonPath`
je neobvezen in brez njega se vedenje obeh skript ne spremeni.

**Odločitev:** **D-016** v [`01-ARHITEKTURA.md`](../01-ARHITEKTURA.md) §9 — kaj je ponovitev,
zakaj je reset sveta njen del in zakaj sta odtis in veličine ločena.

**Meritve:** nobene nove (paket ne meri, ampak pove, kdaj je meritev veljavna).

**Naslednja seja:** prva serija `.\ponovitve-run.ps1` (če jo je uporabnik pognal → tabela
M2.7 dobi razpon in gre v `docs/meritve/`), sicer **M2.4** (50/200/500 NPC-jev) ali
**M2.6** (baseline originala).

---


### 2026-09-17 (36) — M2.7 pognan: N1–N12 zelena, izhodiščna tabela obstaja

**Paket:** M2.7 (zagon)
**Stanje:** **zaključeno** — meritev je veljavna in zapisana

**Izid:** `.\nav-run.ps1` po popravku dveh zagonov: zid 140 blokov, 16 NPC-jev,
`chunki=35 zavrnjeni=0`, scenarij od `NAV-INIT` do `NAV-SUM`, **merila N1–N12 zelena**.
Polni zapis: [meritev](../meritve/2026-09-17-M2.7-navigacija-baseline.md).

**Šest veličin (original):**

| # | Veličina | G (grlo) | O (odprto) |
|---|---|---|---|
| 1 | delež celih poti | 1,000 | 1,000 |
| 2 | razmerje dolžine p50 / p95 | 1,151 / 1,236 | 1,151 / 1,236 |
| 3 | prispelo, faza A / faza B | 1/8 · 6/8 (mediana 240) | 8/8 · 8/8 (mediana 200) |
| 4 | razpon pri grlu, A / B | 3,78 / 3,92 | 4,78 / 4,76 |
| 5 | µs na iskanje p50, hladno / ogreto | 237,6 / 67,6 | 167,9 / 77,8 |
| 6 | dodelitev poti na tick povp. / p95 / max | 0,313 / 0 / 16 (skupaj) | |

**Ugotovitve:**

- **Delna pot se v tem prizorišču ne pojavi** (32/32 celih). Pri 14 blokih in vratih na osi
  cilja proračun 200 vozlišč in domet 32 nista omejitev. **M4.10 dobi s tem vhodni pogoj:
  potrebuje prizorišče z razdaljo čez `NpcNavRange` ali s pravim obvozom**, sicer je delež
  celih poti trivialno 1,000 in popravek ni merljiv.
- **Grlo se pokaže v času, ne v geometriji.** Razmerje dolžine je na obeh progah enako;
  razlika je 1/8 proti 8/8 (faza A) in 6/8 proti 8/8 (faza B).
- **En klic `navigateTo` proti osveženemu: 1/8 proti 6/8.** Izhodiščna številka za M4.10 in
  M4.13 in razlaga, zakaj neosvežena skripta izgleda kot okvarjen AI (past Q11).
- **Iskanja so sunkovita:** p95 = 0 na tick, max 16. Najpočasnejši ticki (232 / 57 / 54 /
  48 ms, brez chunkov in brez autosave) sovpadajo z vzorčnimi ticki skripte, ko dobi vseh
  16 NPC-jev pot hkrati. To je primer, ki ga naslavlja M5.6; pripis je sočasnost, dokaz
  pride z M3.1.
- **Skupina je pri grlu ožja, ne širša** (3,8 proti 4,8) — stisne se pred vrati.
- Ob tem zagonu sta prvič tekli tudi vrstici M2.5a/M2.5b: `RWDIAG-SAVE izlocenih=1 … maxVsi=232,031 maxBrez=232,031` — **najpočasnejši tick tokrat ni bil autosave**, ampak tick s pathfindingom. Merila S1–S7 so v `rwdiag-run.ps1` in še niso bila strojno ovrednotena.

**Ni narejeno in zakaj:** meritev je **en zagon**; protokol zahteva tri (M2.5c še ni).
Dva NPC-ja od osmih na progi G ne prispeta niti v fazi B — kandidata sta zamašek pred
vrati in `canNavigate()`, loči ju šele M3.1.

**Spremembe obnašanja:** nobene.

**Meritve:** [`meritve/2026-09-17-M2.7-navigacija-baseline.md`](../meritve/2026-09-17-M2.7-navigacija-baseline.md).

**Naslednja seja:** **M2.5c** (protokol ponovitev, da bo tabela imela razpon) ali **M2.4**
(50/200/500 NPC-jev). Pred M4.10 je treba dodati prizorišče z razdaljo čez 32 blokov.

---

### 2026-09-17 (35) — Prvi zagon M2.7: prizorišče 750 blokov od spawna

**Paket:** M2.7 (zagon), brez sprememb v modu
**Stanje:** vzrok imenovan in odpravljen; ponovni zagon je na uporabniku

**Kaj se je zgodilo:** `.\nav-run.ps1` je padel na N2 in nato desetkrat iztekel v timeout.
V logu: `Cannot place blocks outside of the world` (zid), nobenega odziva na 16 ukazov
`noppes clone spawn`, `RWDIAG-CHUNKS … npc=0 razlog=v svetovih ni nalozenih NPC-jev`.

**Vzrok:** seme testnega sveta ima spawn na **(743, 4, −231)**, dedicated server brez
igralca pa drži naložene samo chunke v območju ±128 blokov okoli spawna
(`World.isSpawnChunk`). Prizorišče M2.7 pri `z = 64…78` je bilo 750 blokov proč, torej
sploh ni bilo naloženo: `fill` je padel, `noppes clone spawn` pa **ni javil ničesar** in
NPC-jev ni bilo. `setup-commands.txt` (M0.6) in `fixture-run.ps1` imata zato
`setworldspawn 0 4 0`; `nav-setup-commands.txt` ga ni imel.

**Narejeno:**

- `setworldspawn 0 4 0` v `nav-setup-commands.txt`, s pojasnilom, zakaj brez njega scenarij
  tiho odpove.
- `nav-run.ps1`: bralnik `Read-FillBlocks` in merilo **„zid je postavljen"** (`fill` mora
  javiti vsaj 100 blokov) — preverja **učinek** in ne dejanja.
- `Read-Chunks` zdaj bere **obe** obliki vrstice `RWDIAG-CHUNKS`; prej je ob zavrnitvi
  regex zgrešil in scenarij je povedal „chunki niso naloženi" namesto „v svetu ni NPC-jev".
- Ob `npc = 0` ali nepostavljenem zidu scenarij **pade takoj** namesto desetih minut
  čakanja na markerje, ki jih ne bo.

**Ugotovitve:**

- **`noppes clone spawn` v nenaložen chunk molči.** Ne javi napake in ne postavi ničesar.
  Edini znak je posredni (`npc=0`), zato ga mora scenarij izrecno preveriti.
- Isti razred napake kot 17. 9. pri `server.properties` (dnevnik 32): preverba dejanja
  („poslanih 30 ukazov") je bila zelena, učinka pa ni bilo. Vsak nov scenarij mora imeti
  merilo, ki prebere **posledico** v svetu.

**Preverjeno v seji:** `nav-run.ps1` sintaktično brez napak; `Read-Chunks` in
`Read-FillBlocks` sta pognana nad **pravim logom padlega zagona** in dasta
`npc=0, razlog=v svetovih ni nalozenih NPC-jev` ter `0 blokov` — torej bi novi merili
vzrok imenovali v prvih tridesetih sekundah.

**Spremembe obnašanja:** nobene.

**Meritve:** nobene — zagon ni dal veljavnih podatkov.

**Dopolnitev po drugem zagonu:** sam `setworldspawn 0 4 0` **ne zadošča** — `fill` je spet
javil „Cannot place blocks outside of the world". Chunke okoli novega spawna naloži šele
naslednji zagon serverja (`prepareSpawnArea`), zato ima M0.6 (`testworld-run.ps1`) **dva
zagona**: A pribije spawn in shrani, B postavi prizorišče. `nav-run.ps1` ima zdaj isto.
Opomba v M0.6 je bila zapisana že 16. 9. („Zakaj trije zagoni") — nov scenarij je moral
prevzeti postopek, ne samo ukaz.

**Naslednja seja:** ponovni `.\nav-run.ps1`.

---

### 2026-09-17 (34) — M2.7: merila kakovosti navigacije (sonda, opazovalec, scenarij)

**Paket:** M2.7 (del a)
**Stanje:** koda, testi, scenarij in merila končani in preverjeni v seji; zagon v svetu čaka na uporabnika

**Izhodišče:** D-012 pravi, da vanilla pathfindinga ne prepisujemo, ampak ga popravljamo po
stopnjah — vsako pod stikalom in z meritvijo. Meritve pa ni bilo: paketi M4.10–M4.13 in
M5.6 so ostali brez številke, proti kateri bi se lahko izmerili. M2.7 je ta manjkajoči
vhodni pogoj.

**Narejeno:**

- **`rework/diag/NavProbe.java`** — merilni razred brez Minecraft tipov (kot `SlowTicks`):
  delež celih poti, razmerje dolžina/zračna razdalja, doseg delne poti in porazdelitev
  časa iskanja. Ima vrstico z markerjem (`RWNAV-SONDA`, pod drugim markerjem tudi
  `RWNAV-POMET`), razdelek v posnetku in JSON.
- **`rework/diag/NavSweep.java`** — pometanje: vsak merjeni NPC poišče pot do istega cilja.
  Filter po predponi imena, ponovitve, poročilo `naTleh` in `chunkiForced` v isti vrstici.
- **Opazovalec v `DiagEventCollector`** — `nav.ai.path.new`, `nav.ai.paths.per.tick` in
  `nav.ai.navigating`; vrstica `RWNAV-AI` v posnetku.
- **Ukaz `rwdiag nav <x y z [ponovitev] [maxNpc] [imePredpona]>`** in `rwdiag nav status`.
- **Scenarij:** `dev/testworld/nav-control.js` (dve progi, dve fazi), `nav-setup-commands.txt`,
  fixture `NAV_WalkG`, `NAV_WalkO`, `NAV_Control`, `preveri-nav-markerje.js` in
  **`nav-run.ps1` z merili N1–N12**. Dokument: `docs/scenariji/M2.7-navigacija.md`.
- **23 novih testov** (21 `NavProbeTest`, 2 v `DiagSnapshotTest`); skupaj **84 testov
  instrumentacije, vsi zeleni**, prevedeno z `javac --release 8` v seji.
- Odločitev **D-015** (kako se meri in zakaj tako).

**Preverjeno v seji:**

- Prevedenih vseh 18 razredov `rework/diag` proti mapiranim Forge/Minecraft razredom;
  84/84 testov zelenih.
- `nav-run.ps1` sintaktično brez napak (PowerShell 7.4.6 `Parser::ParseFile`).
- **Razčlenjevalnik je pognan nad pravim izpisom, ne nad izmišljenim.** Vrstice
  `RWNAV-POMET`, `RWNAV-SONDA` in `RWNAV-AI` je ustvarila prevedena koda, vrstice `NAV-S`,
  `NAV-CAS` in `NAV-PRISPEL` pa minificirana `nav-control.js` nad ponarejenim svetom
  (`node preveri-nav-markerje.js`). Bralniki `Read-Samples`, `Read-Cas`, `Read-Pometi`,
  `Read-Sonda` in `Read-NavAi` iz `nav-run.ps1` so bili nato pognani nad tem logom, skupaj
  s podvojenimi vrsticami `[FINE/CustomNPCs]`: štiri pometanja so prebrana kot štiri in ne
  kot osem.
- `vstavi-skripto.py` je potrdil, da je skripta v `NAV_Control.json` po normalizaciji
  identična viru.

**Ugotovitve:**

- **Navigatorjeva pot ni merilno orodje.** `PathNavigate.getPathToPos` (`:111-124`) si cilj
  zapomni v `targetPos` in ob istem cilju vrne že izračunano pot. Sonda, ki bi ga
  uporabila, bi spremenila stanje tistega, kar meri, pri ponovitvah pa bi merila branje
  predpomnilnika namesto iskanja poti. Lasten `PathFinder` nad istim `NodeProcessor`-jem
  meri isto kodo brez stranskega učinka.
- **`Path.getTarget()` je `@SideOnly(Side.CLIENT)`** (`Path.java:152`). Celosti poti se na
  strežniku torej ne da prebrati iz poti same; primerjati je treba zadnjo točko s ciljnim
  vozliščem. To je drobna podrobnost, ki bi brez branja dekompiliranega izpisa prišla na
  dan šele ob `NoSuchMethodError` v svetu.
- **Razmerje dolžine sme meriti samo cela pot.** Razmerje delne poti primerja dolžino poti,
  ki nikamor ne pride, z razdaljo do cilja, ki ga ni dosegla — čim prej bi iskanje obupalo,
  tem „boljše" bi bilo razmerje. Test
  `partialPathIsExcludedFromTheRatioButNotFromTheShare` drži to mejo.
- **Ponovitve iskanja ne smejo skozi isto pot kot meritev kakovosti.** Prva različica je
  ponovitve zapisovala kot navadna iskanja in delež celih poti bi bil odvisen od števila
  ponovitev. Ločena `recordTimeOnly` to odpravi; test to zahteva.
- **Šesta veličina je spodnja meja in tako je tudi zapisana.** Opazovalec šteje spremembo
  identitete objekta `Path`, torej uspešne dodelitve; iskanje, ki vrne `null` ali ga
  `canNavigate()` zavrne, je zanj nevidno. Številka, ki bi se delala natančnejšo, kot je,
  bi bila slabša od odsotne.
- **Vzorec razčlenjevalnika iz prave kode je poceni in ujame razred napake, ki ga sicer
  ujame šele zagon.** Isti prijem kot pri M2.5b; tokrat je pokazal, da je bil pomožni
  vzorec regexa shranjen v spremenljivki, ki bi ob preimenovanju tiho dala prazen niz —
  zato je zdaj funkcija `Get-SondaVzorec`, katere manjkajoč klic pade takoj.

**Ni narejeno in zakaj:**

- **Meritve ni**, ker `nav-run.ps1` potrebuje Windows, Javo 8 in gradle z Minecraftom.
  Tabela šestih veličin nastane ob prvem zagonu; do takrat M4.10–M4.13 in M5.6 ostanejo
  zaprti.
- Letečih NPC-jev scenarij ne meri (ima jih M2.3) in cene pri stotinah NPC-jev ne (M2.4).
- `push` ni narejen — poverilnice so na Windows strani. Uporabnik požene
  `git push origin main`; lokalno sta pred originom dva commita (M2.5b in ta).

**Spremembe obnašanja:** nobene v modu. Sonda se sproži samo z ukazom, opazovalec pa teče
le, dokler je merjenje vklopljeno (zbiralnik je takrat že prijavljen na event bus).

**Meritve:** nobene nove.

**Naslednja seja:** prebrati izid `.\nav-run.ps1` (N1–N12) in šest veličin prepisati v
`docs/meritve/`; če je N10 zelen in N1–N9 padejo, je napaka v prizorišču, ne v sondi. Nato
**M2.4** ali **M2.5c**.

---

### 2026-09-17 (33) — M2.5b: autosave tick ima svojo porazdelitev

**Paket:** M2.5 (del b)
**Stanje:** koda, testi in scenarij končani in preverjeni v seji; zagon v svetu čaka na uporabnika

**Izhodišče:** meritev 17. 9. je vprašanje iz M2.5a zaprla — prepad `p95 → p99` je bilo
ogrevanje (81,8 ms → 4,3 ms), `max` pa je v vsakem zagonu en sam tick, ki sovpada z
autosave (58 / 74 / 87 / 124 ms). Ostalo je vprašanje, kaj s tem tickom: pustiti ga v
porazdelitvi pomeni, da primerjava pred/po meri vanilla shranjevanje; izbrisati ga pomeni
lagati, ker server ta čas res porabi.

**Narejeno:**

- `Diag.tick(...)` vsak tick uvrsti v natanko eno od treh veder: `server.tick.ns.nosave`
  (porazdelitev brez autosave), števec `server.tick.save` (izločeni) in števec
  `server.tick.nocontext` (tick brez konteksta, stara pot `Diag.tick(long)`). Tick brez
  konteksta **ne** gre med čiste: ne vemo, ali je v njem tekel autosave, in ugibanje bi
  porazdelitvi dalo videz natančnosti, ki je nima.
- `server.tick.ns` ostane nespremenjen, tabela najpočasnejših tickov tudi — autosave tick
  je v obeh še naprej viden. Nova je samo **druga** porazdelitev istega ticka.
- Posnetek izpiše vrstico z markerjem:
  `RWDIAG-SAVE izlocenih=1 brezKonteksta=0 ostalo=1199 p99vsi=0.705 p99brez=0.700 maxVsi=87.000 maxBrez=0.700`.
- `rwdiag-run.ps1`: bralnik `Read-SaveLine` in merila **S5–S7**. S5 trdi, da se vedra
  seštejejo v `n` porazdelitve (izločitev ne izgubi in ne šteje dvakrat), S6 da rep brez
  autosave ni daljši od celotnega, S7 pa je navzkrižna preverba dveh **neodvisnih**
  mehanizmov: kadar `max` pade pod proračun šele z izločitvijo autosave, mora biti prva
  vrstica tabele najpočasnejših tickov označena s `save > 0`. Če si porazdelitev in
  tabela nasprotujeta, je eden od njiju pokvarjen.
- 6 novih testov (4 v `DiagTest`, 2 v `DiagSnapshotTest`); skupaj **61 testov
  instrumentacije, vsi zeleni**, prevedeno z `javac --release 8` v seji.
- Scenarij: nov razdelek **M2.5b** v `docs/scenariji/M2.1-diag.md`.

**Preverjeno v seji:**

- Prevedeno in pognano: 61/61 zelenih.
- `rwdiag-run.ps1` sintaktično brez napak (PowerShell 7.4.6 `Parser::ParseFile`).
- Bralnik `Read-SaveLine` in merila S5–S7 so **pognana nad pravim posnetkom**, ki ga je
  ustvarila ista koda (1200 tickov, eden z autosave): S5 zelen (1 + 0 + 1199 = 1200),
  S6 zelen (`p99 0,705 → 0,700`, `max 87,000 → 0,700`), pogoj za S7 izpolnjen.

**Ugotovitve:**

- **Izločitev mora biti preverljiva, ne samo narejena.** Brez S5 bi bila napaka v uvrščanju
  (tick v dveh vedrih ali v nobenem) neopazna — porazdelitev bi izgledala lepše, ker bi ji
  manjkali ticki. Vsota veder je najcenejša možna zaščita.
- Ločitev je tudi priprava na **M2.6**: baseline originala mora navesti obe številki,
  sicer primerjava po posegu v AI ni poštena v nobeno smer.

**Ni narejeno in zakaj:**

- **M2.5c — protokol ponovitev** (koliko zagonov, kako se povprečijo, kolikšen razpon je
  še sprejemljiv) ostaja. Protokol iz `01-ARHITEKTURA.md` §7 zahteva 3 ponovitve; skripta
  jih danes ne zna pognati v enem zagonu in razpona ne izpiše.
- Zagona v svetu ni: `rwdiag-run.ps1` potrebuje Windows, Javo 8 in gradle z Minecraftom.

**Spremembe obnašanja:** nobene v modu. Nova porazdelitev in dva števca se polnijo samo,
dokler je merjenje vklopljeno.

**Meritve:** nobene nove.

**Naslednja seja:** prebrati izid `.\rwdiag-run.ps1` (S5–S7) in se odločiti, ali gre
naprej M2.5c (protokol ponovitev) ali M2.4 (scenariji 50/200/500). Če `brezKonteksta > 0`,
najprej najti klicno mesto, ki še kliče `Diag.tick(long)` brez konteksta.

---

### 2026-09-17 (32) — Zakaj je `r2-run.ps1` obstal: napačen svet, ne zanka

**Paket:** M2.3 (zagon), brez sprememb v modu
**Stanje:** vzrok imenovan, scenarija zaščitena; ponovni zagon je na uporabniku

**Kaj se je zgodilo:**

- Uporabnik je javil, da se je `.\r2-run.ps1` "zaciklal". Ni bila zanka: skripta je čakala
  na markerje `R2-INIT` in `R2-{A,B,C}-START`, ki jih ni moglo biti — v svetu ni bilo
  nobenega NPC-ja. V logu je bilo 20× `Could not find clone file` / `Unknown npc` in
  `RWDIAG-CHUNKS ... npc=0 razlog=v svetovih ni nalozenih NPC-jev`. Trije timeouti po
  180 s dajo ~10 minut navideznega obstanka, nato bi skripta padla z NEUSPESNO.
- **Vzrok:** `dev\run\server.properties` je bil od smoke testa (`level-name=m05-smoke`),
  zato je server naložil svet `dev\run\m05-smoke`. `r2-run.ps1` fixture kopira v
  `dev\run\world` — v drug svet. Na tej postaji testni svet sploh ni bil postavljen:
  v `dev\run\world` je bila samo mapa `customnpcs`, brez `level.dat`.
- Preverba "štiri R2 fixture datoteke so v svetu (kopiranih 4)" je bila **zelena in
  neresnična**: datoteke so res bile kopirane, samo ne v svet, ki se je zagnal.

**Narejeno:**

- `r1-run.ps1` in `r2-run.ps1` ob zagonu primerjata `level-name` in `level-seed` z
  `dev\testworld\server.properties`. Ob neujemanju scenarij **pade takoj**, še pred zagonom
  serverja, in pove, naj se požene `.\testworld.ps1`. Popravek ene vrstice (samo
  `level-name`) je bil zavrnjen: smoke test prepiše cel `server.properties`, zato bi
  meritev tekla v drugem okolju (seed, vidna razdalja, težavnost), le v pravem svetu.
- `fixture-run.ps1` in `matrika-run.ps1` popravka ne potrebujeta — `testworld.ps1` kličeta
  sama in s tem dobita pravi `server.properties`.
- Preverjeno v seji: oba scenarija sta sintaktično brez napak (PowerShell 7.4.6
  `Parser::ParseFile`), logika preverbe pa je **pognana** nad tremi pravimi datotekami
  (smoke properties, seme, popravljeno stanje) — smoke properties pade, seme in
  popravljeno stanje gresta skozi.

**Ugotovitve:**

- Merilo, ki preverja **dejanje** (datoteke so kopirane) namesto **učinka** (server jih
  vidi), ne varuje pred ničimer. Pravilna oblika je tista iz M2.1d: pogoj meritve se
  prebere iz sistema, ki ga meri — tu iz `server.properties`, ki ga bere server.
- Ta razred napake je drag na tak način, da ga je lahko spregledati: nič ni padlo, nič ni
  javilo napake, samo počakalo je deset minut in potem povedalo napačno zgodbo.

**Spremembe obnašanja:** nobene v modu; samo dve skripti.

**Meritve:** nobene — zagon ni dal veljavnih podatkov.

**Naslednja seja:** ponovni zagon `.\r2-run.ps1` po `.\testworld.ps1` (uporabnik je svet
medtem že postavil; `server.properties` je spet od testnega sveta).

---

### 2026-09-17 (31) — Združitev vej: podvojen M2.2, M2.5a prestavljen na origin

**Paket:** brez novega paketa — sanacija razhajanja med delovnima postajama
**Stanje:** končano; `push` čaka na uporabnika

**Kaj se je zgodilo:**

- Seja je na namizni postaji začela z `git status` in nadaljevala nedokončan M2.2 iz seje
  16. 9.: prevedla kodo, dopisala dva testa, napisala scenarij, popravila tri napake v
  `rwr1-run.ps1` in commitala (`ec9ede6`).
- Uporabnik je nato rekel, naj pogledam git. `origin/main` je bil star: stal je pri
  `b7827cd` (M2.1d), ker `git fetch` na tej postaji pade na posredniškem strežniku
  (`HTTP 403`). Prava oddaljena veja je bila `37e8d74` — **devet commitov naprej**.
- Na originu so bili **M2.2 (R1 reproduciran in izmerjen), M0.7 (M0 zaključen) in M2.3
  (R2, faza A)**. M2.2 je tam rešen povsem drugače: brez Java zbiralnika, s skriptami
  NPC-jev (`r1-control.js`) in `r1-run.ps1`. **Današnji M2.2 je bil torej podvojeno delo.**

**Narejeno:**

- Oddaljena veja prenesena v repozitorij skozi oblačno okolje z `git bundle` (recept je v
  Znanih omejitvah), `origin/main` je zdaj pravilen.
- Današnji M2.2 **umaknjen z `main` na vejo `m22-seja`** (`ec9ede6`). Ni izbrisan in ni
  združen: M2.2 je na originu zaključen in dvakratna reprodukcija iste zahteve bi bila
  breme, ne varnost.
- **M2.5a prestavljen (rebase) na `origin/main`.** Razrešeni trki: `README.md`,
  `docs/04-STANJE.md` (glava, tabela M2, dnevnik, Znane omejitve, rep porazdelitve) in
  `docs/01-ARHITEKTURA.md`.
- **Trk ID-jev odločitev:** obe veji sta uporabili `D-012` (tu pripis počasnih tickov, na
  originu navigacija). Naši odločitvi sta preštevilčeni v **D-013** (tabela top-N namesto
  profilerja) in **D-014** (seja prevaja in testira sama); vse sklice v dnevniku sem
  popravil. Odločitev originala `D-012` (navigacija) ostane nedotaknjena.
- Preverjeno po združitvi: 17 razredov `rework/**` prevedenih z `javac --release 8`,
  **55 testov instrumentacije zelenih**, `rwdiag-run.ps1` po samodejni združitvi
  **sintaktično brez napak** (PowerShell 7.4.6 `Parser::ParseFile` v oblačnem okolju —
  postopek z originala, prvič uporabljen tudi tu).

**Ugotovitve:**

- **Ista vrsta napake je bila najdena dvakrat neodvisno.** Origin jo je popravil v
  `rwdiag-run.ps1` (`49f7889`, "n-ti zadetek nad celim logom je bral dvojnik prvega"),
  ta seja pa v `rwr1-run.ps1` (merilo R1-1 je primerjalo fazo A samo s sabo). Vzrok je
  isti: `reply()` piše markerje v log **in** pošiljatelju, zato je vsak marker podvojen.
  To ni naključje dveh skript, ampak lastnost ukaza — vsak nov razčlenjevalnik markerjev
  mora brati sidrano na oznako faze, ne po vrstnem redu pojavitve.
- **Prepad `p95 → p99` je že pojasnjen**: origin ga je zaprl z ogrevanjem (81,8 ms → 4,3 ms),
  `max` pa je vsakič en tick z autosave. Odprto vprašanje iz M2.5a je s tem odgovorjeno,
  merila S1–S4 pa ostanejo uporabna kot strojna potrditev pripisa.
- **Iz današnjega M2.2 ostanejo uporabne tri ugotovitve**, tudi če koda ostane na veji:
  ime NPC-ja s presledkom ali podpičjem tiho razbije razčlenjevanje povzetka in stolpce
  CSV (velja za vsak marker, ki ime piše nezaščiteno); `/summon` v 1.12.2 izpiše
  `Object successfully summoned`, ne `Summoned new <ime>`; in zgornja ugotovitev o
  podvojenih markerjih.

**Ni narejeno in zakaj:**

- `push` ni narejen — poverilnice so na Windows strani, lupina seje pa nima ne njih ne
  dostopa do GitHuba. Uporabnik požene `git push origin main`; lokalno sta pred originom
  **dva** commita (M2.5a), veja `m22-seja` pa ostane samo lokalno, dokler je ne potisne
  posebej.
- Veja `m22-seja` ni prečiščena in ne bo vzdrževana. Če se M2.5/M2.7 kdaj lotita merjenja
  jahanja, je tam pripravljen Java zbiralnik z 78 zelenimi testi.

**Spremembe obnašanja:** nobene. Koda se ni spremenila; spremenila se je zgodovina veje.

**Meritve:** nobene.

**Naslednja seja:** **najprej preveri, ali je `origin/main` res svež** (če `fetch` pade,
uporabi recept z bundlom iz Znanih omejitev), šele nato izberi paket. Prvi na vrsti je
ponovni zagon `.\r2-run.ps1` z `dStarost` in `gib` (pojav P1), ob prvem `.\rwdiag-run.ps1`
pa še vrstica pripisa S1–S4.

---

### 2026-09-16 (30) — M2.5a: pripis počasnih tickov + seja prevaja in testira sama

> **Opomba o vrstnem redu.** Ta vnos je nastal 16. 9. na drugi delovni postaji in je v
> skupno zgodovino prišel šele 17. 9., ko sta se veji združili. Zato stoji pred vnosi
> z dne 17. 9., čeprav ima starejši datum. Številka (30) je naslednja prosta; vnos (18)
> je bil ob združitvi že zaseden.


**Paket:** M2.5 (del a)
**Stanje:** koda in testi končani in preverjeni v seji; zagon v svetu čaka na uporabnika

**Narejeno:**

- `rework/diag/SlowTicks.java` — tabela 16 najpočasnejših tickov meritve s kontekstom:
  zaporedna številka ticka, čas od začetka meritve, trajanje, koliko NPC-jev je tiknilo,
  koliko chunkov se je naložilo/odložilo, koliko svetov se je shranilo. Ločeno še števci,
  koliko tickov je preseglo 10, 25, 50 in 100 ms. Brez Minecraft tipov, zato enotsko
  testljiv; na vroči poti ena primerjava na tick brez alokacije.
- `DiagKeys`: `chunk.load`, `chunk.unload`, `world.save`.
- `DiagEventCollector`: naročnine na `ChunkEvent.Load`, `ChunkEvent.Unload` in
  `WorldEvent.Save` (vse s preverbo `world.isRemote`), števci na tick se ob `START`
  počistijo in se ob `END` predajo tabeli skupaj z indeksom ticka.
- `Diag.tick(...)` dobi različico s kontekstom; stara `tick(long)` ostane in zapiše
  kontekst kot neznan (`-`), da obstoječi testi in klici ostanejo veljavni.
- `DiagSnapshot`: nov razdelek v berljivem izpisu in ključ `slowTicks` v JSON.
- `rwdiag-run.ps1`: merila **S1–S4**, bralnika `Read-SlowTable` in `Read-TickMillis`
  (`server.tick.ns` je v tabeli izpisan v ms z decimalkami, zato ga stari bralnik celih
  števil ni prebral) ter vrstica pripisa
  `skupaj N, z autosave X, z nalaganjem chunkov Y, brez obojega Z`.
- 22 novih testov (`SlowTicksTest`); skupaj 55 testov instrumentacije, vsi zeleni.
- Scenarij: nov razdelek **M2.5a** v `docs/scenariji/M2.1-diag.md`. Odločitvi **D-013**
  (zakaj tabela top-N in ne profiler) in **D-014** (seja prevaja in testira sama).

**Ugotovitve:**

- **Seja lahko od zdaj sama prevaja in poganja teste.** Priključena mapa je dosegljiva
  skozi lupino na uporabnikovem računalniku, prevajanje pa teče v oblačnem okolju proti
  mapiranim razredom iz `dev/build/tmp/recompileMc/compiled` (28 MB, Forge + Minecraft z
  mapiranimi imeni) in `dev/libs/customnpcs-mapped-01Oct19.jar`. Manjkata samo `guava` in
  `log4j`, ki v repozitoriju ne obstajata; za prevajanje ju nadomestita dva minimalna
  nadomestka **izven** repozitorija. S tem je preverjeno: sintaksa, imena in podpisi Forge
  API-jev (`ChunkEvent.Load#getChunk`, `WorldEvent.Save`, `WorldEvent#getWorld`) in vsa
  logika brez Minecrafta. 27 testov iz M1, ki potrebujejo `log4j` v času izvajanja, se v
  seji ne da pognati; te še vedno pokrije `.\dev.ps1 test --offline`.
- Napaka, ki jo je ujel prvi test: števci ravni so bili najprej šteti od najvišje ravni
  navzdol z ustavitvijo pri prvi nedoseženi — tick z 30 ms zato ni bil štet niti pri
  10 ms. Test `tickOverALevelCountsInEveryLowerLevelToo` je to pokazal takoj. Ravni se
  odslej štejejo navzgor. To je natanko razlog za D-014: napaka bi sicer prišla v svet.
- Aritmetika, ki jo bo izid potrdil ali ovrgel: autosave teče vsakih 900 tickov
  (`MinecraftServer.tick():762`), torej sta v 60-sekundni meritvi to **največ dva** ticka.
  Počasnih tickov je bilo približno 12. Če jih pripis pripiše autosave, se nekaj ne ujema
  in je treba pogledati, kaj še piše v istem ticku.

**Ni narejeno in zakaj:**

- Meritev ni ponovljena. Zagon `rwdiag-run.ps1` potrebuje Windows, PowerShell, Javo 8 in
  gradle z Minecraftom; seja ima lupino na uporabnikovem računalniku, ne pa tega okolja.
- `rwdiag-run.ps1` spet ni sintaktično preverjen (ni PowerShella). Vsi štirje novi
  regularni izrazi pa **so** preverjeni proti pravemu izpisu posnetka, ki ga je seja
  ustvarila lokalno, vključno s tem, da stari bralnik celih števil še vedno bere
  `npc.per.tick`.
- M2.5 ni zaključen: izločitev autosave ticka iz percentilov in protokol ponovitev
  (koliko zagonov, kako se povprečijo) ostajata.
- **Commit `f3ea71f` je narejen, `push` ne.** Lupina seje je Linux in nima dostopa do
  Windows Credential Managerja, zato `git push` vpraša za geslo, ki ga ni. Uporabnik
  požene `git push origin main` iz korena projekta. Enkraten pogoj tega paketa je bil tudi
  `rm .git/index.lock` — `git fetch` iz prejšnje seje je pustil zaklep, brisanje pa je v
  priključeni mapi privzeto onemogočeno, dokler ga uporabnik ne dovoli.

**Spremembe obnašanja:** nobene v modu. Trije novi števci in tabela se polnijo samo,
dokler je merjenje vklopljeno; zbiralnik je na event bus prijavljen samo takrat. Nobena
nova koda ne posega v entitete, shranjevanje ali mrežo.

**Meritve:** nobene nove — ta paket je orodje, s katerim bo naslednja meritev povedala
vzrok namesto številke.

**Naslednja seja:** pognati `.\dev.ps1 test --offline` (pričakovano 55 testov
instrumentacije zelenih) in `.\rwdiag-run.ps1`, nato prebrati vrstico
`pripis tickov nad 50 ms: …`. Odločitveno drevo je v razdelku M2.5a scenarija:
chunki → podaljšaj ogrevanje in ponovi; autosave → poročaj posebej; brez obojega → rep je
delo v ticku in naslednji korak so klicna mesta (M2.1b), ne porazdelitve. Šele nato M2.2
(reprodukcija R1; uporabnik je potrdil, da sta oba NPC-ja CustomNPC).
### 2026-09-17 (29) — M2.3 pognan: R2 reproduciran v fazi A, fazi B in C razveljavi zmrznitev

**Paket:** M2.3 (reprodukcija R2)
**Stanje:** **delno.** Faza A je veljavna in R2 je reproduciran. Fazi B in C sta
razveljavljeni zaradi novega pojava; scenarij je razširjen tako, da ga naslednji zagon
razloži.

**Narejeno:**

- Ovrednoten zagon uporabnika z dne 17. 9. ob 12:17 (`audit/m23-r2.log`,
  `audit/m23-r2-2026-09-17-1217.md`).
- Napisana meritev [`meritve/2026-09-17-M2.3-R2-reprodukcija.md`](../meritve/2026-09-17-M2.3-R2-reprodukcija.md).
- V `dev/testworld/r2-control.js` dodani merjeni veličini `dStarost` (`getAge()`, torej
  `ticksExisted`) in `gib` (`motionX/Y/Z`); skripta na novo vložena v `R2_Control.json`.
- V `r2-run.ps1` dodana merili **L9** (entitete se tikajo) in **L10** (merilnik gibanja
  dela), funkciji `Min-DStarost` in `Max-Gib` ter razdelek poročila „Proge, ki se niso
  premaknile", ki razlago pove sam.
- `preveri-markerje.js` dopolnjen z `getAge`/`getMotion*` in preverbo, da ima vsaka vzorčna
  vrstica novi polji.
- Scenarij `docs/scenariji/M2.3-R2.md` dopolnjen z obema veličinama, meriloma L9/L10 in
  razlago, zakaj sta nastala.

**Ugotovitve — R2 (faza A, veljavna):**

- **Leteči NPC se pri oviri obnaša kot kopenski.** Proga F (leteči + zid) prevozi 7,95
  bloka in obstane **pri zidu**; proga W (kopenski + isti zid) prevozi 6,93. `cezOviro` je
  0/6 pri obeh. Zid je visok 4 bloke; leteči se dvigne za en blok (y max 5,00) in tam
  obvisi.
- **Letenje samo po sebi deluje.** Proga P (leteči, prosto) prevozi 16,27 bloka in pride na
  1,00 bloka do cilja, `cele=6/6`. Razlika F proti P je zato razlika **ovire**, ne letenja.
- **Najpomembnejša številka je `cele=0/6` na progi F, že v prvem vzorcu.** Leteči NPC ne
  obstane zato, ker bi vodenje odpovedalo, ampak zato, ker **iskanje poti ne vrne poti čez
  zid**. Pot čez zid je ≈ 24 blokov, torej pod `NpcNavRange` 32 — domet ni vzrok in past
  Q11 je izključena že s postavitvijo.
- Skupina se pred oviro **zgosti** (`razpon` 6,65 → 1,26), enako kot pri R1.

**Ugotovitve — pojav P1 (fazi B in C, neveljavni):**

- Po resetu z `setPosition` se **nobeden od dvanajstih letečih NPC-jev ne premakne niti za
  0,01 bloka** v 900 tickih, v dveh različnih načinih vodenja. Kopenska proga se premika
  normalno, torej reset sam po sebi ni pokvarjen.
- Navigator zanje ves čas javlja `navig=6/6` in na prosti progi `cele=6/6`: **pot je cela,
  premika ni.**
- Števec `npc.update` kaže 21,99 klicev na tick čez vseh 1531 tickov, torej svet entitete
  posodablja; razlaga „svet jih ne posodablja" je malo verjetna, a je števec agregaten.
- Ostanejo tri razlage, ki se izključujejo (ne posodablja se / vodenje ne doda gibanja /
  gibanje poje `move()`). Med njimi loči natanko par `dStarost` + `gib`, ki je zdaj dodan.
  **Razlaga pride iz zagona, ne iz ugibanja** — to je pravilo, ki se je pri Q11 že enkrat
  izplačalo.

**Ni narejeno in zakaj:**

- P1 ni pojasnjen. Za to je potreben zagon na Windowsu, ki ga seja ne more izvesti.
- M2.4 in M2.7 nista začeta; smiselna sta šele, ko je M2.3 veljaven v vseh treh fazah.
- Vzrok, zakaj iskanje poti ne vrne poti čez zid, ni dokazan. Omejitev 200 vozlišč
  (`PathFinder.findPath:65`) je hipoteza; dokaz zahteva klicna mesta v `PathNavigate`
  (M2.1b, čaka na M3.1).

**Spremembe obnašanja:** nobene. Ta seja ni spremenila moda.

**Meritve:** [`meritve/2026-09-17-M2.3-R2-reprodukcija.md`](../meritve/2026-09-17-M2.3-R2-reprodukcija.md)

**Preverjeno brez sveta:**

| Kaj | Kako | Izid |
|---|---|---|
| `r2-control.js` je veljaven ES5 | `node --check` | zeleno |
| skripta je vložena v klon | `vstavi-skripto.py` | zeleno, 6997 znakov |
| `r2-run.ps1` in `matrika-run.ps1` se razčlenita | PowerShell 7.4.6 `Parser::ParseFile` | zeleno |
| **novi polji prideta skozi razčlenjevalnik** | `preveri-markerje.js` + `Read-Samples`/`Min-DStarost`/`Max-Gib` iz AST-ja | zeleno: `dStarost(min)=20`, `gib(max)=0,04` na gibajoči progi in 0 na mirujoči |
| koordinate se ujemajo med tremi datotekami | `preveri-skladnost.py` | zeleno |

**Naslednja seja:** pognati `.\r2-run.ps1`, razdelek „Proge, ki se niso premaknile" prepisati
v meritev, P1 razložiti ali zavreči, nato **M2.4** ali **M2.7**.

---

### 2026-09-17 (29) — Q12 odprt: uvoz zunanjih 3D modelov ni v načrtu

**Paket:** brez (vprašanje uporabnika, brez spremembe kode)

Uporabnik je vprašal, ali rework kje predvideva podporo za **prenesene modele** (npr. model
zmaja s spleta, dodan prek kode, URL-ja ali lokalne namestitve), ki bi jih NPC lahko prevzel,
in ali to morda že pokriva R3 (migracija 1.7.10+).

**Ne pokriva.** Preverjeno v `docs/02-ZAHTEVE.md` (R3), `docs/03-FAZE.md` (M8.1–M8.11) in
`README.md`: katalog M8 ne vsebuje uvoza modelov, ker tega nima niti CustomNPC+.

**Kaj mod zna danes (iz dekompilata):**

- `INPCDisplay.setSkinUrl()` / `setCapeTexture()` / `setOverlayTexture()` — samo **teksture**,
  ne geometrija.
- `INPCDisplay.setModel(String id)` (`entity/data/DataDisplay.java:405`) — NPC prevzame model
  **katerekoli registrirane entitete** iz Forge registryja (`ModelData.entityClass`). Zmaj iz
  že nameščenega moda torej deluje že zdaj; samostojna datoteka ne.
- `ModelData` / `ModelPartData` in `client/model/` (`ModelNpcDragon`, `ModelPony`, part paketi)
  so **hardcoded Java `ModelBase` razredi**. Runtime loaderja za modelne datoteke ni.

**Kaj bi dodajanje pomenilo (nepotrjena ocena, XL):** loader formata (`.bbmodel` je
najverjetnejši kandidat), mapiranje kosti na obstoječi part sistem (sicer odpadejo oprema,
skaliranje po delih in hitbox), vezava na animacijski format iz M7 — ki mora govoriti o
kosteh zunanjega modela, ne o fiksnih Steve delih —, distribucija modela do vseh igralcev in
render cache invalidacija (tveganje je že zapisano v `PLAN_IMPLEMENTACIJE.md`).

**Odločitev:** zaenkrat **samo odprto vprašanje Q12**, brez nove zahteve in brez faze.
Odgovor ima smisel šele, ko je M7 dovolj jasen, da je znano, na kakšen skelet se model veže.

---

### 2026-09-17 (28) — M2.3 napisan: reprodukcija R2 s tremi progami, brez zagona

**Paket:** M2.3 (reprodukcija R2 — leteči NPC in ovira)
**Stanje:** **delno.** Vse, kar se da narediti brez Windowsa, je narejeno in preverjeno.
Zagon v svetu je na uporabniku: `.\r2-run.ps1`.

**Narejeno:**

- [`docs/scenariji/M2.3-R2.md`](../scenariji/M2.3-R2.md) — scenarij s tremi progami, osmimi
  vzorčenimi veličinami in merili L1–L8.
- `dev/testworld/r2-control.js` — skripta krmilnika, tri faze.
- Štirje fixture kloni: `R2_Flyer` (`MovementType 1`), `R2_Walker`, `R2_Target`, `R2_Control`;
  skripta je v `R2_Control.json` vložena z `vstavi-skripto.py`, ne ročno.
- `dev/testworld/r2-setup-commands.txt` — prizorišče in 21 NPC-jev, idempotentno.
- `r2-run.ps1` — gonilnik po vzoru `r1-run.ps1`, s poročilom v `audit/m23-r2-<datum>.md`.
- `matrika-run.ps1` ima novo stopnjo `r2`; vrstica IA4 matrike zabeležena kot „scenarij
  obstaja, čaka na zagon".
- `dev/testworld/preveri-markerje.js` — nova preverba pogodbe markerjev **brez Minecrafta**.
- `dev/testworld/preveri-skladnost.py` — nova preverba, da si koordinate scenarija ne
  nasprotujejo med `r2-control.js`, `r2-setup-commands.txt` in `r2-run.ps1`.

**Zakaj tri proge in ne ena.** Zahteva govori samo o „letečem NPC-ju z oviro". Če taka
skupina obstane, to samo po sebi ne pove, ali je kriva ovira, letenje ali navigacija
nasploh — zadnje je natanko past Q11. Scenarij zato vodi hkrati: **F** leteči + zid,
**W** kopenski + isti zid, **P** leteči brez ovire. F proti P izolira oviro, F proti W
izolira letenje.

**Ugotovitve iz kode (vse pred zagonom, iz dekompilacije in Forge-patchanega Minecrafta):**

- `FlyingMoveHelper.java:50`: ob oviri se akcija postavi na `WAIT` in **nič drugega** —
  obhoda ni. Smer se popravi le vsake 4 ticke (`:35`).
- `FlyingMoveHelper` **nikoli ne prebere `this.speed`**, ki mu ga navigator nastavi
  (`PathNavigateFlying.java:78`), ampak vedno `MOVEMENT_SPEED / 2.5`. Hitrost iz
  `navigateTo(x,y,z,speed)` torej na letenje **ne vpliva**. To je novo in gre v R2.
- `EntityNPCFlying.travel()` pri `canFly()` gravitacije sploh ne doda; ostane samo dušenje
  0,91. Leteči NPC, ki obstane, torej **obvisi** in ne pade — simptom je mirovanje v zraku.
- Vanilla `FlyingNodeProcessor` širi vozlišča v 3D, `PathFinder.findPath` pa se ustavi po
  **200 vozliščih** in vrne **delno** pot. V 3D je 200 vozlišč bistveno manj napredka kot
  v 2D, zato je delna pot pri letenju verjetnejša kot pri hoji.
- Zaradi tega scenarij meri `cele=b/N`: koliko poti se res konča pri cilju.
  `getNavigationPath()` (`EntityLivingWrapper.java:41-51`) vrne zadnjo točko poti in prav
  ta številka loči „ni poti" od „pot do zidu". M2.2 tega še ni meril.

**Zakaj so razdalje take, kot so.** Start → cilj je 16 blokov, zid je 4 bloke visok in
75 blokov širok. Pot čez zid je dolga ≈ 24 blokov, torej **pod** `NpcNavRange` 32: če
proga F ne pride, vzrok **ni** domet iskanja poti in izid je enoznačen. Obhod okoli konca
zidu je dolg ≈ 40–52 blokov, torej zunaj dometa — zid je prava ovira, ne dekoracija.

**Preverjeno brez sveta:**

| Kaj | Kako | Izid |
|---|---|---|
| `r2-control.js` je veljaven ES5 | `node --check`, tudi po minifikaciji | zeleno |
| skripta je pravilno vložena v klon | `vstavi-skripto.py` (primerja vir in vloženo) | zeleno, 6316 znakov |
| `r2-run.ps1` in `matrika-run.ps1` se razčlenita | PowerShell 7.4.6 `Parser::ParseFile` | zeleno |
| **markerji se ujemajo z razčlenjevalnikom** | `preveri-markerje.js` + funkcije iz `r2-run.ps1` nad izpisom | zeleno: 9 markerjev po enkrat, 22 vzorcev na fazo in progo (L6 zahteva 20) |
| koordinate se ujemajo med tremi datotekami | `preveri-skladnost.py` | zeleno: 6/6/6 + 3 cilji, zid visok 4, pot čez zid ≈ 24 < 32, obhod 40–52 > 32 |

Zadnja vrstica je pomembna: pri M0.7 je merilo padlo prav zato, ker se izpis in
razčlenjevalnik nista ujemala. Zdaj se to preveri brez zagona sveta.

**Ni narejeno in zakaj:**

- **Scenarij ni pognan.** Seja ima na uporabnikovem računalniku linuxovo lupino brez
  PowerShella, Gradla in Minecrafta. Zagon, meritev in vpis v `docs/meritve/` so naslednji
  korak in so na uporabniku.
- µs na iskanje poti in iskanj na tick (veličini 5 in 6 iz M2.7) nista merjeni — zahtevata
  klicna mesta v `PathNavigate`, torej M2.1b, ki čaka na prenos razredov v M3.1.
- Načina C (dragon/momentum) in nagiba ni kaj meriti; še ne obstajata.

**Spremembe obnašanja:** nobene. Ta seja ni spremenila moda; dodala je scenarij, fixture in
skripte.

**Meritve:** nobene — scenarij še ni pognan.

**Naslednja seja:** pognati `.\r2-run.ps1`, izid zapisati v `docs/meritve/2026-…-M2.3-R2-reprodukcija.md`
in v tabelo Meritve, nato **M2.4** ali **M2.7**.

---

### 2026-09-17 (27) — Prehod 1 integracijske matrike je zelen; M0.7 je s tem pognan

**Paket:** M0.7 (zaključek)
**Stanje:** **končano in pognano.** `audit/m07-matrika-2026-09-17-1133.md`, commit `49f7889`:
**4 stopnje zelene, 0 rdečih, 0 preskočenih.**

| Stopnja | Izid | Trajanje |
|---|---|---|
| verify-package | zeleno | 1 s |
| M0.6 testni svet W1–W8 | zeleno | 172 s |
| M2.1 diagnostika D1–D7, C1–C6 | zeleno | 118 s |
| M2.2 reprodukcija R1 E1–E6 | zeleno | 109 s |

Prehod 1 ni več samo dokument: postopek iz §3 matrike je izveden od začetka do konca, iz
enega ukaza, s poročilom, ki nosi hash jarja in commit. Vrstice IN1, IN2, IN4, IN6, IA7, IS1,
IL1 in IL7 so s tem **zelene v svetu**, ne samo na papirju.

**Q11 popravek je hkrati potrjen z meritvijo.** V fazi A ima kontrolna proga zdaj **8/8 poti**
in prevozi 24,68 bloka (prej 4/8 in 22,71), v fazi B pride do cilja (0,40 bloka). Proga M ima
**0/8 poti v obeh fazah** — ugotovitev R1 drži neodvisno od Q11. Ponovitev je dopisana v
[meritev](../meritve/2026-09-17-M2.2-R1-reprodukcija.md).

**Nova drobtina za M3:** proga M je tu prevozila 1,19 bloka (prvič 0,06). Ponavljajoči
`navigateTo` torej premakne nosilca za dober blok, čeprav `isNavigating()` ostane `false` —
premik brez poti. Ne spremeni izida, gre pa v M3 kot opažanje.

**Spremembe obnašanja:** nobene. Ta seja ni spremenila ne moda ne skript, samo zapisala izid.

**Naslednja seja:** **M2.3** (reprodukcija R2 — leteči NPC čez steno na x=20) ali **M2.7**
(navigacijski sklop, vhodni pogoj za M4/M5). Prehod 1 se odslej pogane pred vsako predajo
paketa; prehod 2 (klient, GUI, IC1–IC6) ostane odprt in zahteva igralca.

---

### 2026-09-17 (26) — Drugi zagon: fixture zelen, D7b lazno negativen

**Paket:** M0.7 (drugi zagon in popravek harnessa)
**Stanje:** `fixture-run.ps1` **zelen v celoti**; `matrika-run.ps1` zelen do M2.1, kjer je padel
zaradi napake v merilni skripti, ne v modu. Popravljeno; tretji zagon je na uporabniku.

**Fixture — deluje:**

- Zagon B: `TW-FIX-QUEST id=1 ime=TW_Quest tip=5 nov=1`, `TW-FIX-DIALOG id=1 ime=TW_Dialog
  quest=1 nov=1`, `TW-FIX-ATTACH npc=T_Trader slot=0 dialog=1`, `TW-FIX-DONE`.
- Zagon C po restartu: ista dva objekta z **`nov=0`** in istim id — round-trip z diska drži.
- V semenu sta zdaj `customnpcs/quests/TW/1.json` in `customnpcs/dialogs/TW/1.json`, oba
  zapisana z modovim `NBTJsonUtil` (`ModRev 18`, `DialogQuest 1`). **IC1 in IC2 nista več
  blokirana na GUI.**
- Iz na novo shranjenega `T_Trader.json` je v seme prenesen **samo blok `NPCDialogOptions`**
  (`Dialog 1`, `Title TW_Dialog`, `DialogSlot 0`). Cela datoteka bi prinesla še runtime šum
  (`Motion`, `OnGround`, `Air`, `Fire`, trader polja) in tiho spremenila fixture, s tem pa
  primerljivost meritev. `fixture-run.ps1` zdaj ta prenos naredi ciljno (merilo F17).

**Matrika — W1–W8 zeleno, M2.1 rdeče, in to je bila napaka meritve:**

`rwdiag-run.ps1` je javil `ticki mirujejo po izklopu (1227 -> 1267)`. Vzrok: **vsak odgovor
ukaza `/rwdiag` je v logu dvakrat** — enkrat kot konzolni odgovor
(`[minecraft/DedicatedServer]`) in enkrat prek `LogWriter` (`[FINE/CustomNPCs]`,
`CommandRwDiag:133`). Trije posnetki so torej dali šest `RWDIAG-OK` vrstic, `Read-OkMarker`
pa je n-ti zadetek štel nad celim logom: "drugi posnetek" je bil v resnici **dvojnik prvega**.
Prave številke iz istega loga: dump1 = 1227, dump2 = 1267, dump3 = 1267 — **D7b drži**, ticki
po `rwdiag off` res mirujejo. Isti zamik je delal `ticki so med merjenjem rasli` lažno zeleno.

**Popravek:** nova `Get-MarkerText` vrne log **brez** `[FINE/CustomNPCs]` vrstic; `Wait-ForCount`,
`Read-OkMarker` in `Read-ChunkMarker` štejejo samo konzolni kanal. `Read-Counter` in
`Read-Distribution` berejo tabelo posnetka in ostaneta na celem logu.

**Ugotovitev za naprej:** marker, ki gre v dva kanala, ni marker, ki se ga da šteti. Vsako
merilo oblike "n-ti zadetek" mora povedati, nad katerim kanalom šteje.

**Meritev iz tega zagona** (ni baseline, samo kontekst): 1227 server tickov v 61,3 s, 8 NPC-jev,
`npc.per.tick` p50 = 8, `npc.tick.gap` max = 1, MSPT p95 = 1,475 ms od 50 ms proračuna (2,9 %).

**Spremembe obnašanja:** v modu nobene. Popravljena sta `rwdiag-run.ps1` in `fixture-run.ps1`.

**Naslednja seja:** tretji zagon `matrika-run.ps1` (pričakovano zeleno do vključno M2.2), nato
**M2.3** ali **M2.7**.

---

### 2026-09-17 (25) — Prvi zagon M0.7: tri napake, vse v mojih skriptah

**Paket:** M0.7 (popravki po prvem realnem zagonu)
**Stanje:** uporabnik je pognal `.\fixture-run.ps1` in `.\matrika-run.ps1`; oba sta padla,
vzroki so najdeni in popravljeni. Ponoven zagon je na uporabniku.

**Kaj je padlo in zakaj:**

1. **`fixture-run.ps1` — `script errored`, noben `TW-FIX-*` marker**
   (`audit/m07-fixture-b.log:170`). Vzrok ni bil mod in ne API, ampak **dve vzporedni
   minifikaciji skripte**: `vstavi-skripto.py` iz M2.2 (briše `//` kjerkoli in izid preveri)
   in moja lastna PowerShell različica v `fixture-run.ps1` (brisala je samo cele
   komentarjske vrstice). Komentar na koncu vrstice `q.setType(5); // 5 = manual…` je po
   strnitvi v eno vrstico zakomentiral **preostanek skripte** → Nashorn sintaktična napaka.
   Stack trace gre v NPC-jev `console` v NBT, v log pride samo `script errored`, zato je
   izgledalo kot napaka moda.
   **Popravek:** PowerShell minifikacija je odstranjena. Skripto vloži izključno
   `vstavi-skripto.py`, in to **ob commitu**; runner samo preveri, da je vložena, in fixture
   prekopira v svet — enak vzorec kot `r1-run.ps1`. Vložena skripta je sintaktično preverjena
   (`new Function(code)` v node) in v repozitoriju.
2. **`verify-testworld.ps1` — W1 lažno negativen.** Merilo je zahtevalo **točno 8** datotek v
   `clones/1`, mapa pa je skupna in ima zdaj 13 (8 M0.6 + 4 R1 + TW_Control). W2–W8 so bile
   zelene, `TW-OK` 8× v B in C — svet je bil torej v redu, padlo je merilo.
   **Popravek:** W1 zdaj preverja, da je prisotnih vseh **8 pričakovanih** fixturov, tuje pa
   samo prešteje in poimenuje.
3. **`matrika-run.ps1` — poročilo brez števcev** (`Izid | zeleno, rdece, 2 preskoceno`).
   `.Count` na praznem rezultatu `Where-Object` v PS 5.1 ni `0`, ampak nič, in `-f` izpiše
   prazen niz. **Popravek:** `@(...)` okoli vseh treh. Poleg tega je `Tee-Object` pisal
   UTF-16 (log neberljiv v grepu in git diffu) in `Out-File -Encoding UTF8` je dodal BOM v
   naslov poročila — oboje zdaj piše UTF-8 brez BOM.

**Ugotovitev, ki velja širše:** dve implementaciji istega koraka sta v tem projektu že drugič
tiha napaka (prvič ročno lepljenje ukazov v konzolo, M0.6). Pravilo: en korak, eno orodje,
in orodje samo preveri svoj izid.

**Kaj je zagon vseeno dokazal:** `verify-package` zeleno; testni svet zeleno v W2–W8 po
restartu; `matrika-run.ps1` se je ustavil ob prvi rdeči stopnji, kot je zamišljeno, in
napisal `audit/m07-matrika-2026-09-17-1026.md` s hashem jarja in commitom.

**Spremembe obnašanja:** v modu nobene. Popravljeni so trije razvojni skripti.

**Meritve:** nobene.

**Naslednja seja:** rezultat ponovnega zagona; nato **M2.3** ali **M2.7**.

---

### 2026-09-17 (24) — Ovrednotenje navigacije in umestitev v plan (D-012)

**Paket:** načrtovanje
**Stanje:** končano — odločitev zapisana, paketi umeščeni

**Vprašanje uporabnika:** kako izvedljivo je napisati lasten `navigateTo`, ki bi bil bistveno
boljši od obstoječega.

**Ugotovitev, ki odloči:** od štirih simptomov navigacije, izmerjenih 17. 9., **nobeden ni
A\* algoritem**.

| Simptom | Dejanski vzrok | Sloj |
|---|---|---|
| nosilec z jahačem nima poti (0/8) | predpogoj `PathNavigateGround.canNavigate()` | vanilla predpogoj |
| obstanek pri 22,71 bloka | 200 vozlišč + `NpcNavRange` = 32, in en sam klic | konfiguracija in način uporabe |
| `navig` niha 0–4/8 med napadom | mutex biti `EntityAIAttackTarget:38` — *24. 9. ovrženo (D-020); verjetno osvežitev poti vsakih 4–10 tickov* | **CustomNPCs AI** |
| zgoščevanje na kup | `minRange` vezan na `npc.width` (`:98`) | **CustomNPCs AI** |

Prepis A\* bi tri od štirih pustil nedotaknjene. Dva sta že pokrita z M3.6 in M3.7.

**Kje je vanilla res slab** (vse preverjeno v dekompiliranem Minecraftu v projektu):
proračun 200 vozlišč vrne delno pot (`PathFinder:65`); manhattanska cena **in** hevristika pri
8-smernem gibanju (`PathPoint:86`) sistematično odrivata diagonale; domet vezan na
`FOLLOW_RANGE`; nič deljenja dela med NPC-ji, vsak z novim `ChunkCache` čez ~6×6 chunkov
(`PathNavigate:126-129`); `checkForStuck` po 100 tickih tiho počisti pot.

To so **namerne varovalke za MSPT**, ne napake. Pri 27 dejavnih NPC-jih je p99 že 115 ms od
50 ms proračuna, zato bi večji proračun vozlišč brez predpomnjenja šel naravnost v lag.

**Odločitev D-012:** vanilla pathfindinga ne prepisujemo. Popravki gredo po stopnjah, vsak
pod stikalom in z A/B meritvijo; brez meritve se kandidat zavrže.

**Dodano v plan:**

| ID | Paket | Zakaj |
|---|---|---|
| **M2.7** | merila kakovosti navigacije — šest veličin na originalu | brez njih je „boljše" nemerljivo; vhodni pogoj za ves sklop |
| **M4.10** | nadaljevanje delne poti | poceni, odpravi najbolj viden simptom (navidezne ovire) |
| **M4.11** | lasten `NodeProcessor` z realno ceno diagonale | odpravi cikcakanje; omejen obseg, podprta razširitvena točka |
| **M4.12** | lasten `PathNavigate` — **samo če** M4.10, M4.11 in M5.6 ne zadostujejo | drago; brez dokaza se ne začne |
| **M4.13** | mehkejše sledenje poti — samo če je trzanje merljivo | kozmetika, dokler ni številke |
| **M5.6** | deljenje in predpomnjenje poti, **povišano** iz „šele če" v redni paket | največji pričakovani dobitek v celem sklopu; 8 NPC-jev proti istemu cilju = 8 iskanj |

M4 se preimenuje v „Gibanje: navigacija po tleh in letenje"; prej je pokrival samo letenje.

**Zavrnjeno za zdaj:** asinhrono iskanje poti, flow fieldi za skupinsko gibanje, lasten
gibalni sklad. Async je edini del sklopa z resnično visokim tveganjem (svet se med iskanjem
spreminja), druga dva spremenita občutek gibanja vseh obstoječih NPC-jev. Odprejo se šele, če
po M4.10, M4.11 in M5.6 merila M2.7 še vedno padajo.

**Spremembe obnašanja:** nobene — to je načrtovalna seja.

**Meritve:** nobene nove.

**Naslednja seja:** M2.3 ali M2.4 po obstoječem vrstnem redu; M2.7 pred začetkom M4.

---

### 2026-09-17 (23) — Q11 zaprt: meja iskanja poti, ne ovira

**Paket:** M2.2 (popravek merilne naprave)
**Stanje:** končano — popravek narejen in preverjen; ponovitev meritve ni pognana

**Vprašanje:** zakaj se je kontrolna proga v fazi A ustavila pri 22,71 bloka, tik pred drugo
stopnico, čeprav je prvo stopnico in vrata prešla.

**Odgovor: ovira ni bila kriva.** Vanilla A* ima dve trdi meji, obe preverjeni v
dekompiliranem Minecraftu v projektu:

| Meja | Kje | Posledica |
|---|---|---|
| proračun **200 vozlišč** | `PathFinder.findPath:65` | vrne **delno pot** do najbližjega doseženega vozlišča, ne `null` |
| dolžina poti `< maxDistance` | `PathFinder.findPath:94` | `maxDistance` = `getPathSearchRange()` = `FOLLOW_RANGE` |
| `FOLLOW_RANGE = NpcNavRange` | `EntityNPCInterface.java:334`; `dev/run/config/CustomNpcs.cfg` → `NpcNavRange=32` | pot, daljša od 32, ni mogoča v enem klicu |

Proga je dolga **natanko 32 blokov**, z obvozom do vrat pa več. En sam `navigateTo` zato ne
more vrniti cele poti in NPC obstane na koncu delne. Skripta je `navigateTo` klicala enkrat
na začetku faze A; `EntityAIAttackTarget` v fazi B se prepathga sam — **zato je proga S v
fazi B cilj dosegla, v fazi A pa ne.** Vse tri opazke se ujamejo z isto razlago.

**Na ugotovitev o R1 to ne vpliva.** Delna pot bi bila še vedno pot: `isNavigating()` bi bil
`true` in `prevozeno` > 0. Proga M je imela 0/8 poti in 0,06 bloka. Meja pojasni samo
stransko opažanje o kontroli, ne razlike M/S.

**Narejeno:**

- `r1-control.js` v fazi A `navigateTo` ponovi ob vsakem vzorcu; vzorec se vzame **pred**
  osvežitvijo, da `navig=` pove stanje ob koncu intervala, ne takoj po svežem klicu.
- Nastal je `dev/testworld/vstavi-skripto.py`: vstavi vir v polje `Script` clone JSON in
  **sam preveri**, da sta vir in vstavljena različica po normalizaciji identična. Doslej je
  bila to ročna operacija, ki jo je scenarij sam označil za pot do tihe napake.
- Preverjeno: `R1_Control.json` se od prejšnjega commita razlikuje **samo** v vrstici
  `Script`; izluščena minificirana skripta gre skozi `node --check`.

**Ni narejeno in zakaj:** ponovitev `.\r1-run.ps1` s popravkom ni pognana — potrebuje Windows.
Ni nujna za izid M2.2; dala bi čistejšo kontrolo (proga S bi v fazi A prišla do cilja).

**Spremembe obnašanja:** nobene v modu.

**Meritve:** nobene nove.

**Naslednja seja:** M2.3 (reprodukcija R2) z upoštevanim pravilom o dometu poti.

---

### 2026-09-17 (22) — M0.7: prehod 1 in fixture kot dve skripti

**Paket:** M0.7 (avtomatizacija)
**Stanje:** koda napisana in pregledana proti izvorni kodi; **ni bila pognana** — seja nima
PowerShella, Gradla ne Minecrafta. Prvi zagon je na uporabniku.

**Narejeno:**

- **`matrika-run.ps1`** — prehod 1 matrike v enem zagonu. Orkestrator: pogane `dev.ps1 build`,
  `verify-package.ps1`, `testworld-run.ps1`, `rwdiag-run.ps1` in `r1-run.ps1`, ujame izhodne kode
  in napiše `audit\m07-matrika-<datum>.md` s SHA-256 jarja, `git` commitom, trajanji in seznamom
  vrstic matrike, ki jih prehod 1 ne pokriva. Privzeto se ustavi ob prvi padli stopnji
  (`-ContinueOnFail` to izklopi), zna `-SkipBuild` in `-Only <stopnja>`.
- **`fixture-run.ps1` + `dev/testworld/tw-fixture.js`** — quest in dialog fixture **brez GUI-ja**.
  Ključna ugotovitev: `noppes quest/dialog` ne zna `create`, zato je bila IC1/IC2 blokada videti
  trdna — a scripting API ima `IQuestCategory.create()` in `IDialogCategory.create()`
  (`api/handler/data/IQuestCategory.java:14`, `IDialogCategory.java:14`), `Quest.save()` pa gre
  skozi `QuestController.saveQuest` → `NBTJsonUtil.SaveFile`, torej **isto save pot kot GUI**.
  Formata ne interpretiramo; ustvari ga mod.
- Druga ugotovitev: quest kategorija nastane iz **imena mape** (`QuestController.java:94`), zato
  je edina stvar, ki jo pripravimo mi, prazna mapa `quests\TW` — kategorije brez tega ni, ker
  `QuestController.load()` na svežem svetu ne naredi privzete (dialogi jo dobijo,
  `loadDefaultDialogs`).
- Trije zagoni po vzorcu M0.5: A pribije world spawn, B ustvari `TW_Quest` (tip 5 manual, nagrada
  1× `minecraft:stone`) in `TW_Dialog`, poveže dialog s questom, ga pripne na `T_Trader`
  (`ICustomNpc.setDialog`) in NPC-ja shrani z `noppes clone add`, C po restartu isti objekt
  **najde** namesto ustvari (`nov=0`) — round-trip dokaz. Merila F1–F16.
- `tw-fixture.js` je vir resnice; `TW_Control.json` ima oznako `@@SCRIPT@@`, ki jo skripta ob
  zagonu zamenja s strnjeno vsebino `.js`. Enaka disciplina kot pri `r1-control.js`, le da je
  vlaganje zdaj skriptirano in ne ročno.
- Dokumentacija: §3 in §4 matrike prepisana, `dev/testworld/README.md` dopolnjen.

**Ni narejeno in zakaj:**

- Quest tipa `item` ostane GUI delo: `QuestItem.items` ni v API-ju, zato ga skripta ne nastavi.
  Fixture je tipa `manual`; za IC2 to zadošča, za item objective ne.
- Klikanje dialoga in dejanski prevzem questa zahtevata prijavljenega igralca → prehod 2.
- Nobena skripta ni bila pognana. `noppes clone add` je bil preverjen v izvorni kodi
  (`CmdClone.java:50-71` — primerja **ime**, ne selektorja, in išče 80 blokov okoli pošiljatelja,
  kar za konzolo pri `0,0,0` in `T_Trader` pri `8,4,8` drži), ne pa v svetu.

**Spremembe obnašanja:** v modu nobene. Nova sta dva razvojna skripta in en fixture krmilnik;
`verify-package.ps1` ostane pri istem seznamu razredov.

**Meritve:** nobene.

**Naslednja seja:** rezultat prvega zagona `matrika-run.ps1` in `fixture-run.ps1` zapisati v ta
dnevnik, popraviti, kar pade, nato **M2.3** ali **M2.4**.

---

### 2026-09-17 (21) — M0 zaključen: integracijska matrika (M0.7) in M0.8 kot blokada

**Paket:** M0.7 (+ formalni zaključek M0.8)
**Stanje:** končano — merila X1–X5 izpolnjena

**Narejeno:**

- Nov dokument [`scenariji/M0.7-integracijska-matrika.md`](../scenariji/M0.7-integracijska-matrika.md):
  funkcionalna matrika iz `PLAN_IMPLEMENTACIJE.md` §6 je prepisana v **47 oštevilčenih preverb**
  (IN NPC, IA AI, IC vsebina, IS skripte, IL lifecycle, IK klient, IO omrežje). Vsaka vrstica ima
  tip (**A** avtomatizirana / **R** ročna), imenovan dokaz (marker v logu, datoteka, NBT diff) in
  pošteno stanje pokritosti.
- Postopek izvedbe v treh prehodih: avtomatski server tek iz obstoječih skript
  (`verify-package.ps1`, `testworld-run.ps1`, `rwdiag-run.ps1`, `r1-run.ps1`), ročni klientski
  prehod in zapis izida v `audit\m07-matrika-<datum>.md`. Pravilo: **matrika ni nikoli zelena po
  opustitvi** — neizvedena vrstica ni uspeh.
- Zapisan enkratni postopek za quest/dialog fixture v GUI-ju (§4), ki odblokira IC1 in IC2.
- **M0.8 zaprt kot zabeležena blokada.** Uporabnik je potrdil, da nima dostopa do dejanskega
  modpacka, Forge builda, configa ne kopije pravega sveta in da nima nobenega pokvarjenega clone
  zapisa. Vpliv je razčlenjen po milestonih v §6 matrike; izhodni kriterij M0 to izrecno dovoljuje.
- `docs/03-FAZE.md`: M0 označen kot zaključen z datumom in sklicem na izpolnjene izhodne kriterije.

**Trenutna pokritost matrike:** 6 vrstic pokrito (IN1, IN4, IA7, IS1, IL1, IL7), 4 delno (IN2, IN6,
IA3, IL6), 3 blokirano (IC1, IC2 na GUI fixture; IO4 na M0.8), 34 ni pokrito. To je izhodišče,
ne pomanjkljivost — matriko polnijo milestoni M1–M10.

**Ni narejeno in zakaj:**

- Matrika ni bila **pognana** — seja nima PowerShella, Gradla ne Minecrafta (glej Znane omejitve).
  Prvi realni prehod 1 naj se izvede ob zaključku naslednjega paketa.
- Quest in dialog fixture ostajata odprta; brez GUI-ja ju ni mogoče narediti, ugibanje NBT
  strukture pa protokol prepoveduje.
- Nobena vrstica ni bila avtomatizirana na novo; M0.7 je dokument, ne koda. Premik vrstic iz **R**
  v **A** je delo pripadajočih milestonov.

**Spremembe obnašanja:** nobene. Koda ni bila spremenjena, `verify-package.ps1` ostane pri istem
seznamu razredov.

**Meritve:** nobene.

**Naslednja seja:** **M2.3** (reprodukcija R2 — leteči NPC čez steno na x=20) ali **M2.4**
(merilni scenariji 50/200/500 NPC-jev). Pred predajo naslednjega paketa se prvič požene prehod 1
integracijske matrike.

---

### 2026-09-17 (20) — M2.2 zaključen: R1 je reproduciran in izmerjen

**Paket:** M2.2
**Stanje:** končano — E1–E6 zelena

**Izid v enem stavku:** nosilec z jahačem **nikoli ne dobi navigacijske poti**
(`isNavigating()` je `false` v 40 od 40 vzorcev), medtem ko je identičen nosilec brez jahača
v istem svetu in istem ticku na poti in prevozi 22,7 bloka.

Polni zapis: [`meritve/2026-09-17-M2.2-R1-reprodukcija.md`](../meritve/2026-09-17-M2.2-R1-reprodukcija.md).

| Faza | Veličina | **M (jahači)** | **S (kontrola)** |
|---|---|---|---|
| A `navigateTo` | nosilcev s potjo | **0/8 ves čas** | 8/8 |
| A | prevoženo povp. / max | **0,06 / 0,06** | 19,25 / 22,71 |
| A | do cilja min | 31,44 (start 31,9) | 8,83 |
| B `setAttackTarget` | nosilcev s potjo | **0/8 ves čas** | 0–4/8 |
| B | prevoženo povp. / max | 8,26 / 9,17 | 19,62 / **32,70** |
| B | razpon skupine | 13,45 → **7,00** | 5,04 → **33,53** |
| B | do cilja min | 24,31 | **0,51** (cilj) |
| obe | jahačev na nosilcu | **8/8 ves čas** | — |
| obe | odstopanje jahač↔nosilec | **0,00 ves čas** | — |

**Kar spremeni zahtevo R1:**

- **Odprto vprašanje 2 v §R1 je odgovorjeno: nosilec poti sploh ne dobi.** Zahteva je
  predpostavljala, da jo dobi in jo nekaj kvari.
- **Hipoteza `EntityAIFollow.tpTo` se tu ne uresniči.** `odstopMax = 0,00` v vseh 40 vzorcih;
  jahač ostane točno na nosilcu. `EntityAIFollow` zahteva lastnika, fixture ga nima. R1 se
  reproducira **brez** te poti.
- **Vez jahač–nosilec je stabilna** (`jahacev=8` ves čas, tudi čez teleport med fazama).
  Pokvarjena ni vez, pokvarjena je navigacija nosilca.
- **„Ne znajo čez bloke“ je izmerjeno.** V fazi B se nosilci z jahači ustavijo pri z ≈ 19,7 in
  tam obstanejo 240 tickov. Stopnica je pri z = 20.
- **Zgoščevanje obstaja, a ni „radius enega bloka“**: razpon pade na 7,00, kontrola gre v
  nasprotno smer na 33,53. Za radius enega bloka §R1 imenuje poseben pogoj
  (`hasHitbox == false` → `minRange ≈ 0`); ti fixture hitbox imajo, zato ta pogoj **ni bil
  preizkušen**. Ločen scenarij.

**Vodilna hipoteza mehanizma — še ni dokazana.** V fazi B se nosilci premikajo, pa `isNavigating()`
ostane `false`; premikajo se torej mimo navigatorja. `PathNavigateGround.canNavigate()`
(`:32-35`) zahteva `onGround || isInLiquid || isRiding`, pri čemer `isRiding()` velja za
**jahača**, ne za nosilca. Preverjena in ovržena sta bila `addInteract()` (kliče ga samo
`interact` in `EntityAIWander`, ta pa se pri `MovingState 0` ne doda) in `EntityAIReturn`
(`ReturnToStart: 0b`). Test, ki hipotezo odloči: števec `onGround` / `canNavigate()` /
`noPath()` na nosilec na tick → **M2.1b oziroma M3.1**. Do takrat je to hipoteza.

**Stranski ugotovitvi:**

- Tudi kontrolna proga v fazi A ne pride do cilja: obstane pri 22,71 bloka, tik pred **drugo**
  stopnico (z = 36), `navig` pade z 8/8 na 4/8. Prvo stopnico in vrata je prešla (razpon se
  pri vratih stisne na 2,87 — lijak). Zakaj druga stopnica ustavi skupino, prva pa ne, je novo
  odprto vprašanje. Na razliko M/S ne vpliva, ker velja za obe progi.
- `navig` niha (0–4/8) tudi na kontrolni progi v fazi B, kar se ujema z dokazano napako mutex
  bitov v `EntityAIAttackTarget:38`. To je neodvisno od jahanja. *(24. 9.: mutex biti niso napaka, D-020.)*

**Meritve:** 27 NPC-jev, 945 tickov: `npc.update.window` 207,3 µs/NPC, `server.tick.ns`
p50 = 1,93 ms, p95 = 10,75 ms, p99 = 115,3 ms. **Ni baseline** in se ne sme primerjati z M2.1d
(66,6 µs pri 8 mirujočih NPC-jih) — tu jih 16 pathfinda čez oviro.

**Spremembe obnašanja:** nobene.

**Naslednja seja:** M2.3 (reprodukcija R2) ali M2.4 (50/200/500). Vzrok R1 se dokaže šele z
instrumentacijo iz M2.1b/M3.1 in ne sme se ga razglasiti prej.

---

### 2026-09-17 (19) — M2.2: preverba artefaktov in dve blokirni napaki pred zagonom

**Paket:** M2.2 (preverba)
**Stanje:** delno — vse, kar se da preveriti brez sveta, je preverjeno; zagon ostaja

**Kontekst:** `git pull` je pokazal, da je oddaljena veja enaka lokalni in da je vse delo
seje 18 (fixture, `r1-control.js`, `r1-setup-commands.txt`, `r1-run.ps1`, scenarij)
necommitano. Seja 18 je naštela tri stvari, ki jih ni mogla preveriti. Ta seja jih je.

**Zaprte vrzeli iz seje 18:**

| Vrzel iz seje 18 | Kako je zaprta | Izid |
|---|---|---|
| `r1-run.ps1` ni sintaktično preverjen | PowerShell 7.4.6 v oblačnem okolju, `Parser::ParseFile` | 2165 žetonov, **0 napak** |
| minificirana skripta v `R1_Control.json` morda ni enaka viru | normalizacija obeh (brez komentarjev, poenoteni narekovaji, stisnjeni presledki) in primerjava znak za znak | **identični** |
| fixture morda vsebujejo več kot naštete spremembe | `diff` vsakega `R1_*` proti osnovi `T_*` | **natanko naštete spremembe, nič drugega** |

Dodatno preverjeno: vsi klici API-ja v skripti obstajajo v `reference-src` s pravimi
podpisi, in `IWorld.getAllEntities(int)` vrne **`IEntity[]`**, ne `List` — pri `List` bi
`all.length` v Nashornu tiho vrnil `undefined` in `R1-MOUNT` bi javil `n=0`.

**Napaka 1 — nosilci ne morejo skozi edina vrata v zidu.** `T_Carrier` in s tem
`R1_Carrier` imata `DoorInteract: 2` (onemogočeno). Tedaj
`EntityNPCInterface.doorInteractType()` (`:867-881`) ne doda vratnega AI taska in nastavi
`setBreakDoors(false)`, kar je `canOpenDoors = false` (`PathNavigateGround.java:317-320`).
V `WalkNodeProcessor.getPathNodeType` (`:359-362`) zaprta lesena vrata postanejo `WALKABLE`
samo ob `canOpenDoors && canEnterDoors`; sicer ostanejo `DOOR_WOOD_CLOSED` s ceno `-1.0F`,
enako kot `BLOCKED` (`PathNodeType.java:20`).

Vrata so edini prehod skozi tri bloke visok zid pri z = 28, torej pot do cilja za nosilce
**sploh ne bi obstajala** — na obeh progah enako. Pohod bi se ustavil 16 blokov od starta,
merilo E2 (kontrolna proga prevozi vsaj 20 blokov) bi padlo in izid bi izgledal kot okvara
navigacije. **Popravek:** `R1_Carrier` dobi `DoorInteract: 1`. Jahači, cilja in krmilnik
ostanejo pri `2`, ker ne navigirajo.

**Napaka 2 — merilni kanal bi lahko molčal.** Ves izpis gre skozi
`npc.executeCommand('/say …')`. `NPCWrapper.executeCommand` (`:201-206`) vrže
`CustomNPCsException`, če command bloki niso vklopljeni, `NoppesUtilServer.runCommand`
(`:226-229`) pa v istem primeru samo zapiše opozorilo. Pri `enable-command-block=false` v
logu ne bi bilo **nobene** vrstice `R1-*` in vse preverbe bi padle hkrati — videti kot
pokvarjena skripta, v resnici nastavitev serverja. V `dev/run/server.properties` je
trenutno `true` (iz M0.6), a nikjer ni bilo zapisano kot pogoj. `r1-run.ps1` to odslej
preveri v koraku 1 in po potrebi popravi.

**Tretja ugotovitev, ki ne blokira, a spreminja branje izida.** `isWalking()`
(`EntityNPCInterface.java:1417`) je `movingType != 0 || isAttacking() || isFollower() ||
Walking` in nadzoruje edino `addVelocity` (`:1020`). Navigacije ne blokira, zato
`MovingState: 0` skriptnemu `navigateTo` ne škodi. Pomeni pa, da v fazi A nosilci **ne
dobivajo sunkov ob medsebojnih trkih**, v fazi B pa jih, ker je `isAttacking()` takrat
resničen. Veličina `razpon` zato **ni primerljiva med fazo A in fazo B**. Primerljiva je
samo med progama M in S znotraj iste faze — kar je edina primerjava, ki jo scenarij trdi,
tako da merila ostanejo veljavna.

**Preverjeno tudi, da ne bo presenečenja:** `EntityAIReturn` se v `addRegularEntries()`
(`:900`) doda **brezpogojno**, a `shouldExecute()` (`:40`) se konča takoj ob
`!ais.shouldReturnHome()`. `ReturnToStart: 0b` iz seje 18 torej res izklopi vlečenje domov.

**Ni narejeno in zakaj:**

- **Scenarij še ni pognan.** Lupina te seje na uporabnikovem računalniku je linuxova in
  nima PowerShella, Gradla ne Minecrafta. Zagon je na uporabniku; dogovorjeno 17. 9.
- Krmilna skripta še vedno ni tekla v Nashornu. Preverjeni so sintaksa, podpisi API-ja in
  vračilni tipi, ne pa izvedba.

**Spremembe obnašanja:** v modu nobene. Spremenjena sta testni fixture in zagonska skripta.

**Meritve:** nobene.

**Naslednja seja:** uporabnik požene `.\r1-run.ps1` iz korena projekta; seja prebere
`audit\m22-r1.log`, ovrednoti E1–E6 in zapiše razliko M/S v `docs/meritve/`. Če E1 pade
(mount ne uspe), je to samo po sebi ugotovitev o R1 in gre v zahtevo. Če pade E2, je
pokvarjena kontrola, ne mod.

---

### 2026-09-15 (18) — M2.2: raziskava in scenarij reprodukcije R1

**Paket:** M2.2 (priprava)
**Stanje:** delno — scenarij je zasnovan in zapisan, fixture in skripta še nista narejena

**Narejeno — raziskava, ki odpre M2.2 brez prenosa kode:**

| Vprašanje | Odgovor | Vir |
|---|---|---|
| Kako posaditi NPC na NPC brez GUI-ja? | `IEntity.setMount(IEntity)` iz skripte | `api/wrapper/EntityWrapper.java:414-420` |
| Gre to skozi vanilla `startRiding`? | **da**, `startRiding(entity, true)` | isto, `:418` |
| Ali gre `ItemMounter` po isti poti? | **ne**, paket `EnumPacketServer.SpawnRider` | `items/ItemMounter.java:31` |
| Ali nosilec sploh dobi pot? | `isNavigating()`, `getNavigationPath()` | `api/wrapper/EntityLivingWrapper.java:43-58` |
| Kako pognati skupino proti cilju? | `navigateTo(x,y,z,speed)`, `setAttackTarget(living)` | isto, `:32`, `:65` |
| Kako najti fixture NPC-je? | `world.getAllEntities(2)` + `hasTag(...)` | `WorldWrapper.java:279-297` |

- **Cela reprodukcija se da napisati kot skripta na enem krmilnem NPC-ju.** Nič ni treba
  prenesti v `src/patch/java`. To je pomembno: M2 ne sme začeti prenašati
  `EntityNPCInterface`, ker je to M3.1. S tem odpade zadnji razlog za prepletanje M2 in M3.
- S tem je **odgovorjeno tudi na odprto vprašanje 4 iz `02-ZAHTEVE.md` §R1** (ali mount
  sploh gre skozi `startRiding`) — za skriptno pot da. Za pot prek `ItemMounter` ne;
  popravek v M3 mora pokriti obe in reprodukcija prek skripte o drugi ne pove nič.
- Scenarij zapisan v [`docs/scenariji/M2.2-R1.md`](../scenariji/M2.2-R1.md): prizorišče,
  potek v dveh fazah, šest merjenih veličin in merila E1–E6.

**Ugotovitev, ki spreminja zahtevo:** zahteva predpisuje „8 jahačev na 8 nosilcih“, **brez
kontrolne skupine**. Tako postavljen poskus ne dokaže ničesar — če se nosilci slabo
premikajo, je lahko vzrok jahanje ali pa se skupina osmih CustomNPC-jev tako premika tudi
brez njih. Scenarij zato vodi **dve vzporedni progi hkrati**, v istem svetu in istem ticku:
proga M (8 nosilcev z jahači) in proga S (8 istih nosilcev brez jahačev). Vsaka številka se
poroča kot par M/S; razlika je ugotovitev, absolutna vrednost ni.

Druga sprememba: scenarij ima dve fazi, ne eno. Faza A je prosta pot (`navigateTo`) — čista
navigacija; faza B je `setAttackTarget`, torej uporabnikov dejanski primer („ko grejo
napadat“) in edina, ki sproži `EntityAIAttackTarget` s sumljivimi mutex biti in `minRange`,
odvisnim od širine.

**Narejeno — gradnja (ista seja, po zasnovi):**

- `dev/testworld/r1-control.js` — **vir** krmilne skripte, berljiv in komentiran. V
  `R1_Control.json` je vstavljena minificirana (polje `Script` je en sam niz). Skripta
  najde NPC-je po tagih, pari jahače z nosilci po koordinati x, vodi obe fazi in izpisuje
  vzorce z markerjem `R1-S`.
- Štirje fixture, **generirani iz obstoječih pravih NBT zapisov** (pravilo iz
  `dev/testworld/README.md`), s točno naštetimi spremembami in nič drugim:
  `R1_Carrier` (iz `T_Carrier`), `R1_Rider` (`T_Rider`), `R1_Target` (`T_Stand`),
  `R1_Control` (`T_Scripted`).
- `dev/testworld/r1-setup-commands.txt` — prizorišče in 26 NPC-jev.
- `r1-run.ps1` — zagon in samodejno ovrednotenje E1–E6, vključno z izpisom razlike M/S.

**Ugotovitve iz gradnje — vse preverjene v kodi, nobena po spominu:**

- **Hrastova vrata se v 1.12.2 registrirajo kot `minecraft:wooden_door`, ne `oak_door`**
  (`net/minecraft/init/Blocks.java:392`). Edina vrsta vrat s starim imenom; ugibanje bi
  tiho spodletelo in zid bi ostal brez prehoda.
- Spodnja polovica vrat meta `3` = `FACING NORTH`: `BlockDoor.getStateFromMeta` (`:423`)
  bere `EnumFacing.getHorizontal(meta & 3).rotateYCCW()`.
- **`ReturnToStart` je v osnovnih fixture `1b`.** To požene `EntityAIReturn` in vleče NPC
  nazaj domov; pri 32 blokov dolgi progi bi sámo po sebi ustavilo pohod in bi izgledalo kot
  okvara jahanja. V vseh štirih R1 fixture je `0b`.
- `setAttackTarget(null)` je varen (`EntityLivingWrapper:65-71`, `EntityLivingBaseWrapper:69-75`).
- **Vrstni red zagona ni poljuben.** Krmilna skripta začne šteti ob spawnu, zato se
  `R1_Control` spawna **šele po** `rwdiag chunks on` in ogrevanju. Sicer bi se scenarij po
  300 tickih ustavil sredi faze A in bi izgledal kot okvara AI — natanko past M2.1d.
- Ena sama datoteka `R1_Carrier` streže obema progama; skripta ju loči po `x < 10`. Tako
  sta nosilca na obeh progah zagotovo identična, kar je pri kontrolni skupini bistvo.

**Ni narejeno in zakaj:**

- Scenarij ni bil pognan. `r1-run.ps1` ni sintaktično preverjen (v seji ni PowerShella) in
  krmilna skripta ni tekla v Nashornu — preverjeno je le, da se izvorna in minificirana
  različica razčlenita (`node --check`) in da se vsi klici API-ja ujemajo s podpisi v
  `reference-src`.
- Merilo E6 (zapis razlike M/S v `docs/meritve/`) nastane šele po zagonu.

**Spremembe obnašanja:** v modu nobene. Vse novo je v testnem svetu in v skriptah.

**Meritve:** nobene — reprodukcija še ni tekla.

**Naslednja seja:** pognati `.\r1-run.ps1`, ovrednotiti E1–E6 in zapisati razliko M/S v
`docs/meritve/`. Če E1 pade (mount ne uspe), je to samo po sebi ugotovitev o R1 in gre v
zahtevo. Če E2 pade, je pokvarjena kontrola in scenarij, ne mod.

---

### 2026-09-15 (17) — M2.1d: pogoj meritve (prisilno naloženi chunki)

**Paket:** M2.1 (del d)
**Stanje:** končano — C1–C6 zelena v svetu isti dan ob 14:07

**Narejeno:**

- `rework/diag/DiagChunkPlan.java` — **čista** logika načrta: pretvorba blok→chunk (z
  `floor >> 4`, ne deljenjem, ki pri negativnih koordinatah pokrije napačen chunk),
  odstranjevanje podvojenih chunkov, razširjanje po obročih in proračun. Brez Minecraft
  tipov, zato je edini del paketa, ki se da enotsko testirati.
- `rework/diag/DiagChunkLoader.java` — lastni `ForgeChunkManager` ticketi tipa `NORMAL`,
  prisilno nalaganje chunkov vseh naloženih NPC-jev, osvežitev enkrat na sekundo prek
  `WorldTickEvent` (ta prinese svet s sabo, zato razred ne potrebuje poti do
  `MinecraftServer` izven ukaza), sprostitev ob `chunks off` in ob `FMLServerStoppedEvent`.
- `CommandRwDiag`: nov podukaz `/rwdiag chunks <on [obroč]|off|status>` z markerjem
  `RWDIAG-CHUNKS`. **Namenoma ni del `rwdiag on`** — meritev brez prisilno naloženih
  chunkov je veljavna le z igralcem v svetu, in ta razlika mora biti v scenariju vidna.
- `DiagKeys`: `world.chunks.forced` (vzorči se vsako sekundo skozi celo meritev) in
  `diag.chunks.added`. Prvi **zapiše pogoj meritve v sam posnetek**, da meritve brez pogoja
  ni več mogoče pomotoma brati kot veljavne.
- `rwdiag-run.ps1`: `chunks on` pred `rwdiag on`, nova merila **C1–C6**, stikali
  `-ChunkRadius` in `-NoChunks`. **C3 in C4 sta tisto, kar 15. 9. ni bilo preverjeno in
  zato ni bilo opaženo:** vsi števci so rasli, samo NPC-ji niso tikali. Skripta zdaj bere
  `npc.per.tick` in `npc.tick.gap` iz tabele posnetka in pade, če NPC-ji nehajo tikati.
- 10 novih JUnit testov (`DiagChunkPlanTest`); v seji je bilo zelenih vseh 33 testov
  instrumentacije, prevedenih z `javac --release 8`.
- Odločitev **D-011** v `01-ARHITEKTURA.md`; scenarij `docs/scenariji/M2.1-diag.md`
  dopolnjen z razdelkom M2.1d in merili C1–C7.

**Ugotovitve:**

- **Mod je že registriran chunkloader.** `CustomNpcs.load` kliče
  `ForgeChunkManager.setForcedChunkLoadingCallback(this, new ChunkController())`. To je
  bilo bistveno: `ForgeChunkManager.requestTicket` **vrže `RuntimeException`**, če mod
  nima registriranega `LoadingCallback`. Nova koda zato callbacka ne dodaja in ne spreminja.
- Proračun: 25 chunkov na ticket, 200 ticketov (`dev\run\config\forgeChunkLoading.cfg`),
  torej do 5000 chunkov. Omejitev v kodi je 400 chunkov na svet (16 ticketov).
- **Dve napaki v originalnem `ChunkController`** (najdeni med branjem, nista popravljeni —
  protokol prepoveduje popravljati mimogrede):
  1. `getTicket` (`:40-56`) ob uspešnem ustvarjanju ticketa vrne `null` namesto ticketa.
     Klicatelj torej misli, da je ustvarjanje spodletelo, čeprav je ticket shranjen.
  2. `ticketsLoaded` (`:70`) preverja `tickets.contains(npc)`, kjer je `tickets` seznam
     ticketov, `npc` pa entiteta — pogoj je vedno `false` in je mrtev.
  Nista razporejeni v milestone; spadata k opravilu „Chunk Loader“ (job 8), ki v M2 ni
  predmet dela. To je tudi razlog, da si merilni pogoj ne izposodi tega razreda.

**Preverjeno isti dan ob 14:07 (uporabnik je pognal, seja je preverila izpise in posnetke):**

- **Deluje.** `npc.update` = **9832** (prej 1968), kar je 8,007 na tick; `npc.update` / 8 =
  1229 = število tickov. NPC-ji so tikali **v vsakem ticku celotne meritve**.
  `npc.tick.gap` ima 1227 vzorcev in vsi so 1 — brez ene same luknje. `npc.per.tick`
  p50 = 8 (prej 0).
- **C1–C6 zelena**: `chunki=9 tiketi=1 obroc=1 zavrnjeni=0 npc=8`; po `chunks off`
  `chunki=0 tiketi=0`; `TW-SCRIPT-OK`, `TW-SCRIPT-TICK-10`, 0 `ERROR` iz `noppes.*`,
  `BUILD SUCCESSFUL`.
- Polni zapis: [`docs/meritve/2026-09-15-M2.1d-prva-veljavna.md`](../meritve/2026-09-15-M2.1d-prva-veljavna.md).

**Napaka v merilu, ki jo je ta zagon razkril:** C5 je bilo napisano kot `min = max` in bi
**zavrglo prav to, prvo veljavno meritev projekta**. Nabor je zrasel z 9 na 12 chunkov, ker
so trije NPC-ji zatavali iz pokritega območja in jih je osvežitev pokrila
(`diag.chunks.added = 3`) — to je delo zbiralnika, ne napaka. Pravo merilo je `min > 0`:
padec na 0 pomeni, da pogoj ni držal, rast ne pomeni ničesar slabega. Popravljeno v
`rwdiag-run.ps1` in v scenariju. Prestrogo merilo, ki zavrže veljavno meritev, je enako
škodljivo kot ohlapno, ki spusti neveljavno.

**Ni narejeno in zakaj:**

- `rwdiag-run.ps1` ni bil sintaktično preverjen v seji (ni PowerShella); preverila ga je
  šele uporabnikova izvedba, ki je tekla do konca.
- Prepad p95/p99 ni razčiščen (glej spodaj); to je M2.5, ne ta paket.

**Spremembe obnašanja:** nov podukaz `/rwdiag chunks` (raven dovoljenja 2). Dokler se ne
pokliče, ta koda ne pokliče ničesar iz `ForgeChunkManager` in ni prijavljena na event bus.
Na obnašanje NPC-jev, shranjevanje in mrežo ne vpliva nič.

**Meritve:** prva veljavna meritev projekta. p50 = 0,557 ms, p95 = 1,638 ms od 50 ms
proračuna pri 8 NPC-jih. **p99 = 81,789 ms in max = 269,019 ms se ne smeta navajati**,
dokler prepad ni pojasnjen.

**Prepad med p95 in p99 — postavljen in isti dan zaprt.** Prva meritev je imela p95 = 1,6 ms
in p99 = **81,8 ms**. Kandidat 1 (priklop ticketa sproži nalaganje chunka, to pa je delo na
server niti) je bil potrjen s ponovitvijo ob 14:19 z **10 s ogrevanja** med `chunks on` in
`rwdiag on` — edina sprememba:

| | brez ogrevanja (14:07) | z ogrevanjem (14:19) | |
|---|---|---|---|
| `server.tick.ns` p50 | 0,557 ms | **0,410 ms** | −26 % |
| `server.tick.ns` p95 | 1,638 ms | **1,016 ms** | −38 % |
| `server.tick.ns` **p99** | **81,789 ms** | **4,325 ms** | **faktor 19** |
| `server.tick.ns` max | 269,019 ms | 86,863 ms | autosave, en tick |
| `npc.update.window` | 182,5 µs / NPC | **66,6 µs / NPC** | −64 % |

Kandidata 2 (autosave) in 3 (delo NPC-jev) sta s tem izključena kot razlaga prepada.
Presenečenje ob strani: napihnjen je bil tudi `npc.update.window` — ker se meri kot razmik
do naslednje entitete, je tick z nalaganjem chunka ta razmik raztegnil. Vsaka meritev brez
ogrevanja torej precenjuje tudi porabo na NPC. **Ogrevanje je odslej privzeto** in je pogoj
vsake meritve, enako kot prisilno naloženi chunki.

**Merodajni rezultat pri 8 NPC-jih, brez igralca, 61,3 s:** p50 = 0,410 ms in p95 = 1,016 ms
od 50 ms proračuna; NPC-ji tikajo v vsakem ticku; zgornja meja porabe na NPC je 66,6 µs.
Še vedno **ni baseline** — 8 NPC-jev je preverba naprave, ne scenarij.

**Naslednja seja:** **M2.2** — reprodukcija R1 (8 jahačev na 8 nosilcih, oba CustomNPC).

---

### 2026-09-15 (16) — M2.1c zaključen: zakaj NPC-ji nehajo tikati

**Paket:** M2.1 (del c)
**Stanje:** končano — vzrok je imenovan z datoteko in vrstico, ne z domnevo

**Narejeno:**

- Uporabnik je pognal `.\rwdiag-run.ps1` s števcem `npc.update.blocked` (tretja meritev,
  12:47). Izid: **`npc.update.blocked` = 0 in `entity.update.blocked` = 0** v 61 sekundah.
- Preden je ničla sprejeta kot podatek, je bilo preverjeno, da števec ni tiho mrtev —
  in to na **jarju, ki ga je server res naložil** (`customnpcs-dev-runtime.jar`, žig
  12:46:54; gradle je izvedel `:compileJava`, `:devRuntimeMod`, `:jar`, nobenega kot
  `UP-TO-DATE`). `DiagKeys.class` vsebuje oba ključa, `DiagEventCollector.class` ima
  `onCanUpdate` z `@SubscribeEvent` in parametrom `EntityEvent$CanUpdate`.
- **Mehanizem 1 izključen.** `World.updateEntityWithOptionalForce` (`World.java:2139-2155`)
  doseže `ForgeEventFactory.canEntityUpdate` samo, kadar preverba območja ±32 blokov pade.
  Nobenega dogodka ⇒ preverba ni padla niti enkrat.
- **Mehanizem 2 imenovan.** `WorldServer.updateEntities()` (`WorldServer.java:628-644`):

  ```java
  if (this.playerEntities.isEmpty() && getPersistentChunks().isEmpty()) {
      if (this.updateEntityTick++ >= 300) { return; }
  } else { this.resetUpdateEntityTick(); }
  ```

  Vanilla neha posodabljati entitete **300 tickov (15 s)** po nalaganju sveta, če ni
  igralca in ni prisilno naloženega chunka. Zanka do entitet sploh ne pride.
- Aritmetika se ujema pri vseh treh zagonih (247, 246 in 237 tickov z NPC-ji, glede na
  zamik med `Done` in `rwdiag on`; ločljivost žigov v logu je 1 s = ±20 tickov).
- Razrešeno tudi navidezno protislovje "`world.tick` šteje, NPC-ji pa ne tikajo":
  `MinecraftServer.updateTimeLightAndEntities` kliče `worldserver.tick()` (`:831`) in
  `worldserver.updateEntities()` (`:842`) ločeno, `onPostWorldTick` pa je za obema (`:851`).
- Polni zapis, tabele in številke:
  [`docs/meritve/2026-09-15-M2.1-prvi-posnetek.md`](../meritve/2026-09-15-M2.1-prvi-posnetek.md).

**Ugotovitve:**

- **Vse tri dosedanje meritve so neveljavne kot izhodišče.** Za ~80 % svojega trajanja so
  merile prazen tek, zato se `server.tick.ns` percentili ne smejo navajati kot baseline.
  Prva uporabna številka pride šele po M2.1d.
- Ista vrstica, ki je vzrok, je tudi popravek. Dovolj je, da **eno** od dvojega ne drži.
  `ForgeChunkManager` ticket je strožje jamstvo kot igralec: odklene zanko
  `updateEntities()` (pogoj na ravni sveta) **in** postavi `range = 0` v preverbi ±32
  blokov (pogoj na ravni chunka), in ne potrebuje človeka pred zaslonom. Scenarij mora
  pokriti chunke vseh merjenih NPC-jev, ne le enega.
- Najdaljši tick je bil 123,9 ms (prej 74,5 in 58,1) in še vedno sovpada z autosave. Merilo
  za M2.5 ostaja nespremenjeno: autosave tick izločiti ali poročati posebej.

**Ni narejeno in zakaj:**

- M2.1d ni začet. Po protokolu je to svoj paket s svojim commitom; poleg tega bo takoj po
  njem treba ponoviti meritev, kar spet zahteva uporabnika.
- Commit ni narejen v seji — seja ne more poganjati `git`. Vsebina commita: ta dnevnik,
  dopolnjena meritev in obstoječa (že napisana) koda M2.1c, če še ni commitana.

**Spremembe obnašanja:** nobene. Za M2.1c ni bila spremenjena nobena vrstica kode; ta seja
je samo pognala, preverila in razložila.

**Meritve:** tretja meritev M2.1, `audit\m21-rwdiag.log`, posnetki
`dev\run\logs\rwdiag\rwdiag-20260915-1248*`. Zeleno: `TW-SCRIPT-OK`, 0 `ERROR` vrstic iz
`noppes.*`, 0 `script errored`, `BUILD SUCCESSFUL`, števci po `rwdiag off` mirujejo
(1268/1968 → 1268/1968).

**Naslednja seja:** **M2.1d** — `ForgeChunkManager` ticket za chunke z merjenimi NPC-ji,
pod stikalom in privzeto izklopljen (D-007), plus merilo: ista meritev mora pokazati
`npc.per.tick` p50 = 8 čez celotno trajanje in `npc.tick.gap` max = 1 pri ≥ 1200 vzorcih.
Šele nato M2.2 (reprodukcija R1; uporabnik je potrdil, da sta oba NPC-ja CustomNPC).

---

### 2026-09-15 (15) — M2.1a: instrumentacija `rework/diag`

**Paket:** M2.1 (del a)
**Stanje:** koda napisana in enotsko preverjena; gradle, `verify-package.ps1` in zagon v
svetu mora pognati uporabnik (seja še vedno ne more zaganjati ukazov na njegovem računalniku)

**Narejeno:**

- Nov paket `dev/src/patch/java/noppes/npcs/rework/diag/` z devetimi datotekami:
  `Diag` (vstopna točka, vklop/izklop, kljuci, posnetek), `DiagKey` (števec + čas na
  `LongAdder`), `Distribution` (logaritemski koši, p50/p95/p99), `DiagSnapshot`
  (nespremenljiv posnetek, tabela + JSON), `DiagKeys` (imena velicin na enem mestu),
  `DiagDump` (zapis v `logs/rwdiag/`), `DiagEventCollector` (zbiranje na Forge dogodkih),
  `CommandRwDiag` (ukaz `/rwdiag`) in `package-info`.
- **Izklopljena instrumentacija ne stane nič.** Zbiralnik se na Forge event bus prijavi
  šele ob `/rwdiag on` in se ob `off` odjavi, zato ni klica na entiteto na tick. Vsa
  klicna mesta v kodi moda pa se najprej vprašajo za eno `volatile` polje.
- 23 novih JUnit testov (`DiagTest`, `DistributionTest`, `DiagSnapshotTest`). Pokrivajo:
  izklopljeno stanje ne šteje nič, vklop počisti prejšnje števce, meritev, ki prečka
  vklop ali izklop, se zavrže, natančnost percentilov, ekstremne vrednosti, točnost
  štetja pod osmimi nitmi hkrati, in to, da se posnetek med izpisom ne premika.
- `CustomNpcs.java`: tri vrstice — registracija ukaza, `-Drwdiag=on` ob zagonu serverja in
  izklop ob `FMLServerStoppedEvent`.
- `dev/build.gradle`: novi testi so izključeni iz `testOriginal`, ker se nanašajo na
  razrede, ki jih v nedotaknjenem originalu ni.
- `docs/scenariji/M2.1-diag.md`: kaj se meri, kako se bere, merila D1–D7 in postopek.

**Ugotovitve:**

- Meritev časa posodobitve NPC-ja je zaenkrat **približek**: `EntityNPCInterface` še ni
  prenesen, Forge pa da le dogodek ob začetku posodobitve entitete. Čas se zato meri kot
  razmik do naslednje žive entitete v istem ticku — to je zgornja meja, ne točna poraba.
  Točna meritev pride z M3.1 in takrat se stolpca primerjata.
- Povprečje je pri performancu neuporabno, zato `Distribution` hrani porazdelitev v
  logaritemskih koših. 32 podkošev na potenco dvojke da ~3,2 % napake navzgor pri 16 KB na
  velicino; prvotnih 8 podkošev (~12 %) je bilo pri MSPT premalo natančnih — p95 in p99
  sta padla v isti koš.
- Ukaz je namenoma `/rwdiag` in ne podukaz `/noppes`: instrumentacija mora delovati tudi,
  če je z mod ukazi kaj narobe, in ne sme spreminjati obstoječega ukaznega drevesa.

**Ni narejeno in zakaj:**

- Števci za izračune poti, klice skript in čas po AI taskih (M2.1b) so **rezervirani z
  imenom, a prazni**. Njihova klicna mesta so v `EntityNPCInterface`, paketu `ai` in
  `ScriptContainer`, ki še niso preneseni v `src/patch/java`. Prenos teh razredov je
  vsebina M3.1 in M5.1; delati ga zdaj bi pomenilo velik prenos brez pripadajočega
  popravka, kar je v nasprotju s pravilom "en paket = en commit = ena stvar".
- Koda ni prevedena z gradle in ni tekla v Minecraftu. Jedro paketa (brez Minecrafta) je
  bilo prevedeno z `javac --release 8` in vseh 23 testov je v seji zelenih, razreda
  `DiagEventCollector` in `CommandRwDiag` pa se opirata na Forge in MC API, zato ju je
  preverila samo primerjava s klici v `reference-src` (`CommandNoppes`, `ScriptPlayerEventHandler`).

**Spremembe obnašanja:** nov ukaz `/rwdiag` (raven dovoljenja 2) in nov, privzeto
izklopljen zbiralnik meritev. Na obnašanje NPC-jev, shranjevanje in mrežo ne vpliva nič.

**Meritve:** nobene — to je paket, ki meritve šele omogoči.

**Preverjeno isti dan ob 10:52 (uporabnik je pognal, seja je preverila izpise in datoteke):**

- **D1 zeleno** — `.\dev.ps1 test`: `DiagTest` 10, `DistributionTest` 8, `DiagSnapshotTest` 5,
  skupaj 23 testov, 0 napak, 0 napak izvajanja (`dev/build/test-results/test/`). Testi zdaj
  tečejo na Javi 8 na uporabnikovem računalniku, ne le na OpenJDK 21 v seji.
- **D2 zeleno** — `testOriginal` je tekel brez treh novih razredov, kot mora.
- **D3 zeleno** — seja je sama primerjala `dev/build/libs/CustomNPCs_1.12.2-01Oct19-workspace.jar`
  z originalom: 25 spremenjenih razredov (vsi pričakovani, isti nabor kot prej + `CustomNpcs`),
  27 dodanih (11 `rework/data` + 11 `rework/diag` + notranji razredi), **0 odstranjenih**.
  `CommandRwDiag` in `DiagEventCollector` sta v jarju, kar pomeni, da se Forge in MC API, ki ju
  seja ni mogla prevesti, dejansko prevedeta — to je bilo edino resno tveganje tega paketa.
- **Commit in push narejena** — `c28b654` "M2.1a: instrumentacija rework/diag + ukaz /rwdiag";
  `origin/main` kaže na isti commit. V indeksu je vseh 9 datotek paketa, 3 testi, scenarij in
  posodobljeni dokumenti.
- **D4–D7 zelena ob 12:17** (`.\rwdiag-run.ps1`, izpis `audit\m21-rwdiag.log`):
  - **D4** — server z novim jarjem se naloži in čisto ustavi, `TW-SCRIPT-OK` je v logu,
    **nobene `ERROR` vrstice iz `noppes.*`**, nobenega `script errored`, `BUILD SUCCESSFUL`.
  - **D5** — `RWDIAG-OK ticki=1226 npc=1896` po 61 s merjenja.
  - **D6** — trije posnetki v `dev\run\logs\rwdiag\`, vsak v `.txt` in `.json`, nobeden prazen.
  - **D7** — pred vklopom `RWDIAG stanje=off ticki=0`; po `rwdiag off` sta dva posnetka v
    razmiku 10 s pokazala **enaka** števca (1246 / 1896), kar dokazuje, da je zbiralnik res
    odjavljen z event busa in ne le tiho.
- Da se ročno tipkanje v gradle konzolo ne ponovi (15. 9. je dvakrat tiho odpovedalo), je
  nastala skripta **`rwdiag-run.ps1`** — isti mehanizem kot `testworld-run.ps1`: požene
  server, pošlje `rwdiag status|on|dump|off`, primerja števce pred in po izklopu, preveri
  zapisane posnetke, ustavi server in izpiše prvo tabelo. Izhodna koda 0 = D4–D7 zelena.
- **Prva številka in prvo presenečenje.** MSPT p50 = 0,16 ms, p95 = 1,21 ms — pri osmih
  NPC-jih je server prazen. Toda `npc.per.tick` ima **p50 = 0** in je binaren (0 ali 8):
  NPC-ji tikajo le v približno vsakem petem ticku, ne v vsakem. To ni napaka števca, ampak
  lastnost sveta brez igralca; razčistiti jo je treba **pred** M2.4, sicer bodo merilni
  scenariji merili nekaj drugega, kot mislimo. Podrobnosti, kandidati in naslednji korak:
  [`docs/meritve/2026-09-15-M2.1-prvi-posnetek.md`](../meritve/2026-09-15-M2.1-prvi-posnetek.md).
- Najdaljši tick 74,5 ms sovpada z autosave. M2.5 mora autosave tick izločiti ali poročati
  posebej, sicer bo p99 vedno meril shranjevanje in ne AI.

**M2.1c — instrumentacija za razčiščenje `npc.per.tick` (isti dan, 12:2x):**

Da odgovor ne bo ugibanje, je zbiralnik dobil pet novih velicin, vse še vedno samo na Forge
dogodkih in samo dokler je merjenje vklopljeno:

- `npc.tick.gap` — razmik v tickih med dvema tickoma z NPC-ji. Enakomeren razmik pomeni
  sistematiko, razmetan pomeni chunk loading.
- `world.tick` — server ticki po svetovih, vsota čez dimenzije.
- `world.entities.loaded`, `world.npc.loaded`, `world.npc.killed`, `world.players` —
  vzorčenje `loadedEntityList` in `playerEntities` enkrat na sekundo, sešteto čez vse svetove.

`world.npc.killed` je tam zaradi pasti iz M0.6: ubit CustomNPC ostane v svetu in čaka na
respawn. Če je meritev tekla nad mrtvimi NPC-ji, so številke o nečem drugem, kot mislimo.

Uporabljeni so samo tisti deli MC API-ja, ki so potrjeni v `reference-src`
(`loadedEntityList` in `playerEntities` v `NPCSpawning`, `isKilled()` v
`EntityNPCInterface`); `addedToChunk` in `provider.getDimension()` sta bila namenoma
izpuščena, ker ju v dekompilatu ni bilo mogoče potrditi, prevesti pa jih seja ne more.
Jedro paketa se je v seji znova prevedlo z `javac --release 8` in 23 testov je zelenih.

**Druga meritev (12:32) je vprašanje razčistila do polovice.** Novi števci so izključili dva
kandidata od treh in pokazali nekaj, česar prva meritev ni mogla: `npc.tick.gap` ima 246
vzorcev in **vsi so 1**, skupaj 1976 = 8 × 247. NPC-ji so torej tikali v **247 zaporednih
tickih in nato nikoli več** — ne "vsak peti tick", ampak en strnjen blok na začetku.
`world.npc.loaded` je 62 vzorcev zapored točno 8, `world.npc.killed` 0, `world.tick` 1229
pri 1228 server tickih, `world.players` 0.

**Ugotovitev: približno 14 sekund po nalaganju sveta vanilla neha posodabljati entitete,
ker v svetu ni igralca.** Mod s tem nima nič. To pomeni, da sta obe dosedanji meritvi merili
prvih ~14 s dogajanja in nato prazen tek — in razloži, zakaj je MSPT tako nizek.

Ostaneta dva mehanizma: (1) `updateEntityWithOptionalForce` preskoči posodobitev, ker
območje 32 blokov okoli entitete ni naloženo — takrat Forge posije `EntityEvent.CanUpdate`;
(2) zanka `updateEntities` do entitet sploh ne pride. Zato je dodan števec
**`npc.update.blocked`**, ki šteje `EntityEvent.CanUpdate` za NPC-je in dogodka ne
spreminja. Naslednji zagon loči mehanizma brez ugibanja.

**Posledica za M2.4/M2.6, ki velja ne glede na izid:** vsak merilni scenarij mora
eksplicitno določiti, ali je v svetu igralec, ali pa morajo biti chunki z NPC-ji prisilno
naloženi (`ForgeChunkManager`). Dokler to ni urejeno, nobena številka iz M2 ni primerljiva
s produkcijskim strežnikom. Podrobnosti:
[`docs/meritve/2026-09-15-M2.1-prvi-posnetek.md`](../meritve/2026-09-15-M2.1-prvi-posnetek.md).

**Naslednja seja:** pognati `.\rwdiag-run.ps1` s števcem `npc.update.blocked` in dokončati
odgovor, nato urediti pogoj meritve (igralec ali prisilno naloženi chunki) in šele nato M2.2
(reprodukcija R1). Uporabnik je potrdil, da sta pri R1 **oba NPC-ja CustomNPC** (jahač in
nosilec), zato reprodukcija ne potrebuje vanilla konja.

---

### 2026-09-15 (14) — M0.6 zaključen: skriptiran scenarij testnega sveta

**Paket:** M0.6
**Stanje:** končano

**Narejeno:**

- `testworld-run.ps1` požene cel scenarij brez človeka: trije server zagoni, ukazi na
  standardni vhod, izpis v `audit/m06-testworld-{a,b,c}.log`, na koncu `verify-testworld.ps1`
  nad logoma B in C. Mehanizem je prevzet iz `smoke-server.ps1` (M0.5).
- **Izid: vse preverbe zelene.** Zagon B `32 entities deleted` in `TW-OK` 8×; zagon C po
  restartu `TW-OK` 8×, `TW-SCRIPT-OK` 1×, `TW-SCRIPT-TICK-10` 1×. Nobene `ERROR` vrstice iz
  `noppes.*`, nobenega `script errored`. Scenarij je tekel nad svetom, onesnaženim z 32
  NPC-ji iz neuspelih poskusov, in ga je sam počistil — s tem je dokazana idempotentnost.
- Merila W1–W8 veljajo; M0.6 je izhodni pogoj za M2 in je izpolnjen.

**Ni narejeno in zakaj:**

- Quest in dialog fixture ostajata v M0.7 — obojega ni mogoče sestaviti brez GUI-ja,
  ugibanje NBT strukture pa protokol prepoveduje.
- Igralec se še ni povezal v testni svet; `PlayerData.save` (B1/B2 v realnem obratovanju)
  zato še ni pokrit. Prav tako M0.7.

**Ugotovitve — štiri pasti, vsaka je stala en neuspel zagon:**

1. **Ročno lepljenje ukazov v gradle konzolo je nezanesljivo.** Prvi poskus je izgledal kot
   okvara testnega sveta: `noppes clone list 1` je izpisal vseh 8 imen, a
   `execute @e[tag=testworld]` je javil `found nothing`. V logu ni bilo **nobenega**
   `gamerule`, `fill` ali `clone spawn` — vsebina `setup-commands.txt` ni nikoli prišla do
   serverja. Odslej vsak scenarij pošilja ukaze skriptirano.
2. **`runServer` je lahko `UP-TO-DATE` in se sploh ne zažene.** ForgeGradle prijavi `dev/run`
   kot izhod naloge; če se med dvema zagonoma tam nič ne spremeni, Gradle nalogo preskoči,
   build se konča v 20 s in skripta čaka na marker do timeouta. Popravljeno v
   `dev/build.gradle` z `outputs.upToDateWhen { false }` za `runClient` in `runServer`.
   Velja tudi za `smoke-server.ps1`.
3. **`/kill` CustomNPC-ja ne odstrani, ampak ga respawna.** `DataStats` ima privzeto
   `RespawnTime = 20` in `SpawnCycle = 0` (`:30-31`); ubit NPC se shrani kot mrtev in ob
   nalaganju sveta oživi. Po dveh poskusih je bilo v svetu 24 in nato 32 NPC-jev, čeprav je
   `kill` vsakič javil, da jih je pobil. Pravi ukaz je `noppes slay npcs` →
   `entity.isDead = true` (`CmdSlay.java:140`). **Pomembno za M2**, kjer je število NPC-jev
   merjena količina.
4. **Zapis datoteke na uporabnikov disk je lahko tiho neuspešen.** Dvakrat je orodje javilo
   uspešen zapis, na disku pa je ostala prejšnja verzija (`setup-commands.txt` 1321 B namesto
   1654 B, `testworld-run.ps1` 9828 B namesto 10400 B), zato je zagon tekel s staro logiko in
   izgledal kot nova napaka v modu. Odslej seja po vsakem zapisu preveri velikost datoteke,
   preden zahteva zagon.

Poleg tega: `fill` javi `No blocks filled`, kadar so bloki že na mestu — izpis zgleda kot
odpoved, pa ni. In `EntityNPCInterface` kliče `EventHooks.onNPCTick` vsak 10. tick (`:358`),
torej je script tick 2 Hz na NPC; vsi ti klici gredo skozi en statičen `lock` v
`ScriptContainer.run`, kar je vhodni podatek za M5.

**Spremembe obnašanja:** v modu nobene. `dev/build.gradle` je dobil
`outputs.upToDateWhen { false }` za `runClient` in `runServer`; to je razvojno orodje, ne
del zapakiranega moda, zato `verify-package.ps1` ostane pri istem seznamu razredov.

**Meritve:** nobene.

**Naslednja seja:** M0.7 — integracijska matrika. Prvi korak zahteva uporabnika: v GUI-ju
narediti en quest in en dialog, pripeti dialog na fixture NPC-ja, nato se
`world/customnpcs/quests/` in `dialogs/` posname v `dev/testworld/`.

---

### 2026-09-15 (13) — M0.6: seme testnega sveta in samodejno ovrednotenje

**Paket:** M0.6
**Stanje:** delno — vse datoteke so pripravljene in preverjene proti izvorni kodi;
scenarij še ni bil pognan, ker seja ni mogla zagnati ukazov na uporabnikovem računalniku.

**Narejeno:**

- Pregled semena, ki je nastalo ob koncu prejšnje seje (`dev/testworld/`, `testworld.ps1`,
  `docs/scenariji/M0.6-testni-svet.md`). Prejšnja seja jih ni ne commitala ne zabeležila
  v ta dnevnik; to je bila odprta luknja in je zdaj zaprta.
- **Verifikacija semena proti izvorni kodi** (ne proti spominu):
  - `CmdClone.spawn` ima podpis `<name> <tab> [[world:]x,y,z] [newname]` —
    `setup-commands.txt` ga uporablja pravilno (`CmdClone.java:112`).
  - `CmdClone.grid` ima `<name> <tab> <lenght> <width> [[world:]x,y,z]` — kot je navedeno
    v scenariju za M2.4 (`CmdClone.java:173`).
  - `DataScript.readFromNBT` bere `Scripts`, `ScriptLanguage` in `ScriptEnabled` iz
    **korenskega** compounda entitete; `T_Scripted.json` ima vse tri na pravem mestu.
  - `ScriptContainer.readFromNBT` bere `Script`, `Console`, `ScriptList` — struktura v
    fixture datoteki se ujema.
  - Vseh 8 fixture datotek se ujema s tabelo v `dev/testworld/README.md`
    (`MovementType`, `MovingState`, `Role`, `Invulnerable`, `PersistenceRequired`, tagi).
- **Popravljeno merilo W5.** Prvotno merilo je bilo "`T_Scripted` ne vrže script napake",
  kar je lažno pozitivno, če scripting sploh ne teče. `DataScript.isEnabled()` zahteva
  `ScriptEnabled` **in** `ScriptController.HasStart` **in** `CustomNpcs.EnableScripting`
  (`CustomNpcs.java:271` postavi `HasStart=false` ob load, `:293` na `true` šele ob
  `FMLServerStartedEvent`). Merilo zdaj zahteva pozitiven izpis iz same skripte.
- Skripta v `T_Scripted` zato ob `init` požene `/say TW-SCRIPT-OK` in ob 200. `tick`
  `/say TW-SCRIPT-TICK-200`. Pot je `NPCWrapper.executeCommand` →
  `NoppesUtilServer.runCommand`, ki zahteva vklopljene command bloke; seme jih ima
  (`enable-command-block=true`). Izhod `/say` pride v konzolo tudi brez igralca.
- Nov `verify-testworld.ps1` samodejno ovrednoti W1–W8 iz `dev/run/logs/latest.log` in
  datotečnega sistema ter vrne izhodno kodo 0/1. S tem scenarij ni več odvisen od ročnega
  branja loga in ga lahko katerakoli seja ponovi enako.
- Scenarij in `dev/testworld/README.md` posodobljena; postopek ima zdaj eksplicitno fazo
  restarta za W6.

**Ni narejeno in zakaj:**

- Scenarij ni pognan iz seje. Windows posodobitev z 8. 9. je onemogočila, da bi seja
  dosegla uporabnikov datotečni sistem prek lupine; seja lahko datoteke bere in piše, ne
  pa izvajati gradle, server ali git. Zagon je naredil uporabnik.
- Commit in push je naredil uporabnik. Push je bil najprej zavrnjen: na `origin/main` sta
  bila commita `M0.2r` in `M0.5` z **druge delovne postaje**, ki ju ta postaja ni imela.
  Delo je bilo združeno brez force-pusha; oba M0.5 zapisa (ročni z 11. 9. in skriptirani
  s 14. 9.) sta ohranjena, dnevnik pa preštevilčen, ker sta obe veji uporabili številko (10).
- W6 in W7 sta preverljiva šele po dejanskem zagonu; do takrat M0.6 ostane *delno*.

**Ugotovitve:**

- `ScriptContainer.run` sinhronizira na **statičnem** `lock` objektu, torej si vse skripte
  vseh NPC-jev delijo eno ključavnico. To je konkreten dokaz za kandidata iz M5
  (odstranitev globalnega script locka) in za R7; zabeleženo, ne popravljeno.
- `ScriptContainer` ob napaki pokliče `NoppesUtilServer.NotifyOPs(... " script errored")`,
  kar je iskalni niz za negativno stran W5.
- Prejšnja seja je fixture NPC-je generirala iz pravega NPC zapisa (`ModRev 18`, polna
  lista atributov z `generic.flyingSpeed`), ne iz ugibanja. Pravilo iz
  `dev/testworld/README.md` je torej spoštovano; edina ročna sprememba v tej seji je
  vsebina niza `Script`, kar ni sprememba strukture NBT.

**Spremembe obnašanja:** nobene — spremenjeno je le testno seme in dokumentacija; v
`src/patch/java` ni sprememb, zato `verify-package.ps1` ostane pri istem seznamu razredov.

**Meritve:** nobene

**Odprto:** projekt zdaj teče na **dveh delovnih postajah**. Vsaka seja mora pred delom
pognati `git fetch origin` in preveriti, ali je `origin/main` pred njo; sicer se dnevnik
razide, kot se je tokrat.

**Naslednja seja:** zapisati rezultat W1–W8 iz uporabnikovega zagona; če je PASS, zapreti
M0.6 in odpreti M0.7 (integracijska matrika + quest/dialog fixture iz GUI-ja).

---

### 2026-09-14 (12) — M0.5: dedicated-server smoke test

**Paket:** M0.5
**Stanje:** končano

**Narejeno:**

- `dev/build.gradle` poveže `runServer.standardInput = System.in`. Gradlov `JavaExec`
  privzeto poda prazen vhod, zato je bila konzola dev serverja doslej mrtva in serverja
  ni bilo mogoče niti čisto ustaviti z `stop`. Deluje samo z `--no-daemon`.
- Dodan `smoke-server.ps1`: skriptiran, ponovljiv smoke test s 13 preverbami. Svet
  `m05-smoke` se vsakič zbriše in ustvari na novo (FLAT, seed 20260914, brez spawna mobov,
  `max-tick-time=-1`), ukazi gredo na standardni vhod, izpisi v `audit\m05-server-*.log`.
- Postopek zapisan kot ponovljiv scenarij v `docs/scenariji/M0.5-server-smoke.md`.
- Izid: vseh 13 preverb zeleno. Server naloži 5 modov, `Done (1.337s)`, NPC se ustvari,
  preživi `save-all flush` in restart, po restartu konzola izpiše `[SmokeNPC] M05-NPC-PRESENT`.
- Minecraft EULA je bila sprejeta na izrecno zahtevo uporabnika; skripta je zapiše samo
  z zastavico `-AcceptEula`.

**Ni narejeno in zakaj:**

- Igralec v svetu ni bil uporabljen; NPC je ustvarjen z `summon` in NBT, ne prek GUI-ja.
  Preverjen je en NPC in en NBT ključ, ne celotno stanje NPC-ja. To je obseg M0.7.
- M0.6 (testni svet z znanimi NPC-ji, questi, dialogi in skriptami) ostaja ločen paket;
  M0.5 mu je pripravil determinističen svet in spawn.

**Ugotovitve:**

- `CommandSummon` v 1.12.2 javi `Cannot summon the object out of the world` tudi takrat,
  ko **ciljni chunk ni naložen**, ne samo pri neveljavnih koordinatah. Spawn superflat
  sveta lahko pade do 256 blokov od izhodišča, zato chunk `0,0` ni nujno med spawn chunki.
  Zato ima test tri zagone: prvi pribije world spawn na `0 5 0`.
- `execute @e[…] ~ ~ ~ say` je boljša preverba kot `testfor`, ker konzola izpiše ime NPC-ja
  v oklepaju. En marker dokaže troje: entiteta obstaja, je pravega tipa in NBT ključ `Name`
  je preživel round-trip skozi popravljeno save pot.
- `save-all` v 1.12.2 izpiše `Saved the world`, ne `Saved the game`. Prvi osnutek skripte je
  čakal na napačen niz in 180 s lovil timeout.

**Spremembe obnašanja:** nobene v modu. Dev server zdaj bere konzolo; to je razvojno orodje,
ne del zapakiranega moda.

**Meritve:** nobene

**Naslednja seja:** M0.6 — testni svet z znanimi NPC-ji, questi, dialogi in skriptami, na
podlagi determinističnega sveta iz M0.5.

---

### 2026-09-14 (11) — M0.2r: obnova razvojnega okolja na novi delovni postaji

**Paket:** M0.2r (neplaniran, blokiral je vse ostalo)
**Stanje:** končano

**Narejeno:**

- Projekt je bil prestavljen na drug računalnik. `dev/libs`, `dev/baseline` in `.tools` so v
  `.gitignore`, zato na novi postavitvi **ni bilo mapiranega originala** in nič se ni prevajalo.
- `dev/build.gradle` je dobil task `exportMappedOriginal` (aktiven samo z `-PremapOriginal`),
  ki regenerira `dev/libs/customnpcs-mapped-01Oct19.jar` iz SHA-256-preverjenega originala po
  isti ForgeGradle deobf poti kot prvotna postavitev.
- `obnovi-okolje.ps1` preveri JDK 8 in hashe, nato požene `setupDecompWorkspace`, remap,
  `testOriginal` + `test`, `buildPatchedMod` in `verify-package.ps1`. Gradle koraki najprej
  poskusijo `--offline` in ob neuspehu ponovijo z omrežjem.
- Rezultat: `testOriginal` 31, `test` 47, build zapakira 42 razredov, `verify-package.ps1`
  pregleda 1716 vnosov in najde 25 pričakovanih zamenjav — **enako kot pred selitvijo**.

**Ni narejeno in zakaj:**

- `downloads\forge-1.12.2-14.23.5.2847-mdk.zip` na tej postaji ne obstaja in ni bil obnovljen;
  Gradle cache ima vse potrebno, zato paket ni blokiran. Vnos v locku ostane kot zapis izvora.

**Ugotovitve:**

- Tri napake v prvem osnutku remap taska, vse potrjene z izvedbo, ne z branjem:
  1. `deobfCompile files(...)` ForgeGradle 2.3 zavrne — zahteva `ExternalModuleDependency`.
  2. `flatDir` ignorira group in odgovori tudi na ForgeGradlovo lastno poizvedbo
     `deobf.<group>:<ime>:<verzija>`, zato vrne **neremapiran original** namesto deobf
     rezultata in remap se sploh ne zgodi. Rešitev je `ivy` repozitorij z `[organisation]`
     v vzorcu.
  3. Remap sproži šele `deobfCompileDummyTask`; brez te odvisnosti task ne naredi ničesar.
- `$ErrorActionPreference = 'Stop'` v PowerShellu spremeni **vsako vrstico stderr** zunanjega
  programa v terminating error. Že `java -version` je prekinil skripto. Native klici zato
  tečejo z `Continue`, uspeh pa se presoja po `$LASTEXITCODE`.
- Regeneriran jar **ni bitno enak** hashu v `environment-lock.json`, vsebinsko pa je enak:
  1716 vnosov se ujema po imenu, vseh **815 resourcev je bitno enakih**, remapiranih je 571
  razredov. Deobf izhod ForgeGradla torej ni bitno ponovljiv med postavitvami. Lock je
  posodobljen; razlog je zapisan kot odločitev **D-010**.

**Spremembe obnašanja:** nobene. Normalen build je nespremenjen; remap koda se naloži samo
z `-PremapOriginal`.

**Meritve:** nobene

**Naslednja seja:** M0.5.

---

### 2026-09-11 (10) — M0.5: dedicated server smoke test

**Paket:** M0.5
**Stanje:** končano

**Narejeno:**

- Scenarij je zapisan kot ponovljiv postopek v [`scenariji/M0.5-server-smoke.md`](../scenariji/M0.5-server-smoke.md)
  z devetimi merili sprejemljivosti, tako da ga lahko katerakoli kasnejša seja ponovi.
- Dedicated server se zažene iz popravljenega drevesa: Forge 14.23.5.2847, Java 1.8.0_492,
  5 modov naloženih, `Done (12.031s)` ob prvem in `Done (2.077s)` ob ponovnem zagonu.
- NPC `SmokeM05` (`customnpcs:CustomNpc`) spawnan na `(0,72,0)`, dodan v clone tab 1,
  nato `save-all flush` in `stop`.
- Po restartu: `M05-SURVIVED` 1× in `M05-TAG-OK` 1× — entiteta **in** vanilla `Tags`
  so preživeli serializacijo. `noppes clone list 1` še vedno vsebuje `SmokeM05`,
  `noppes clone spawn` uspe in `M05-AFTER-CLONE-SPAWN` se pojavi 2×.
- `world/customnpcs/clones/1/SmokeM05.json` (7399 B) je na disku brez ostankov `.tmp` ali
  `.bak`; vsebina ima pričakovane ključe `Name`, `Tags`, `Invulnerable`, `NoGravity`.
  To je prva potrditev popravka M1.5a na dejanskem strežniku, ne samo v testu.
- Oba `stop`-a sta čista: `Saving players` → `Saving worlds` → `Unloading dimension 0`
  brez izjeme; drain world seje iz M1.6 ni javil nečistega zaključka.
- V logu ni nobene napake iz `noppes.*`. Edina `ERROR` vnosa sta okoljska in prisotna tudi
  pri originalu (maven library folder, manjkajoči FML podpisi).

**Ni narejeno in zakaj:**

- Pot ob prijavi igralca (`PlayerData.save`, torej B1 in B2 v realnem obratovanju) ni pokrita,
  ker se noben klient ni povezal. Spada v M0.6/M0.7, kjer bo testni svet imel tudi igralca.
- Git commit te seje ni bil narejen iz seje; razlog je nova omejitev orodij spodaj.

**Ugotovitve:**

- Ukazi iz server konzole se izvajajo na `(0,0,0)`, ne na world spawnu `(132,64,-32)`,
  `/noppes clone add` pa išče NPC-je v radiju 80 blokov od pošiljatelja. Vsak kasnejši
  konzolni scenarij mora entitete postaviti blizu izhodišča ali uporabiti klienta.
- V shranjenem clone JSON je `"id": "customnpcs:customnpc"` z malimi črkami, registry ime
  entitete pa je `customnpcs:CustomNpc`. Za `/summon` in `@e[type=...]` je obvezna oblika
  z velikimi črkami. Zaenkrat neškodljivo, a kandidat za preverbo pri M8.
- Clone datoteke niso strog JSON (`1b`, `1.0f`). To je originalni format, ki ga `NbtJson`
  namerno ohranja; strog JSON parser jih ne prebere in to ni napaka.

**Spremembe obnašanja:** nobene

**Meritve:** nobene; zagon 12,0 s oziroma 2,1 s je informativen podatek, ne meritev po protokolu.

**Naslednja seja:** M0.6 — ponovljiv testni svet z znanimi NPC-ji, dialogi in skriptami.

---

### 2026-09-11 (9) — M1.9: fault injection varnih zapisov

**Paket:** M1.9
**Stanje:** končano

**Narejeno:**

- `SafeFileWriter` je dobil package-private datotečno mejo za deterministično fault injection;
  javni API in produkcijska pot še vedno uporabljata neposredno `java.nio.file.Files` implementacijo.
- Dodanih je 9 scenarijev: prekinitev med pisanjem pred zamenjavo, zaklenjen/zavrnjen cilj,
  zavrnjen dostop do imenika, nepodprt atomski premik, odpoved fallback namestitve, prekinitev
  po fallback zamenjavi, odpoved same obnove, pokvarjen JSON kandidat in restart z orphaned `.bak`.
- Pri vsaki odpovedi test zahteva, da ostane zadnja verzija na cilju ali kot eksplicitno
  obnovljiv `.bak`; začasne `.tmp` datoteke se ne puščajo.
- Normalna atomska zamenjava je ponovno uspešno tekla na dejanskem Windows datotečnem sistemu;
  redke OS veje uporabljajo deterministično vržene standardne Java NIO izjeme.
- Vseh 31 primerjalnih testov ter 16 testov writerjev, session lifecyclea in fault injection
  je zelenih v Javi 8.
- Build zapakira 42 razredov; `verify-package` je pregledal 1716 originalnih vnosov in našel
  samo 25 pričakovanih zamenjav originalnih class datotek.

**Ni narejeno in zakaj:**

- M1.4 in M1.8 ostajata zavestno odložena brez konkretne poškodovane uporabnikove datoteke.
- M1.7 ni potreben, ker se save format ni spremenil.

**Ugotovitve:**

- Če odpovesta tako namestitev kot obnova cilja, `SafeFileWriter` pusti staro vsebino v `.bak`
  in pripne napako obnove kot suppressed exception; podatki so še vedno obnovljivi.
- Orphaned backup se ne prepiše samodejno. Writer zavrne nov zapis, da ohrani zadnjo znano
  obnovljivo verzijo za pregled namesto ugibanja.

**Spremembe obnašanja:** nobene; dodana testna meja ni del javnega API-ja.

**Meritve:** nobene

**Naslednja seja:** M0.5 dedicated-server smoke, nato M0.6 testni svet kot vhod za M2.

---

### 2026-09-11 (8) — M1.6: lifecycle asinhronih world zapisov

**Paket:** M1.6
**Stanje:** končano

**Narejeno:**

- `CustomNpcs` je bil prenesen v prevedljivo drevo in shranjen v ločenem baseline commitu;
  popravljeni so bili samo trije generični artefakti dekompilacije.
- Dodan `rework/data/WorldSaveSession`: ločen enonitni executor za vsako server-world sejo,
  z absolutnim rootom sveta, zajetim ob `FMLServerAboutToStartEvent`.
- `PlayerData.save` ob zahtevi zajame NBT snapshot in ime cilja ter ga preda aktivni seji;
  ne izračunava več poti znotraj pozno izvedene naloge na globalnem schedulerju.
- Ob `FMLServerStoppedEvent` seja najprej preneha sprejemati zapise in izprazni vrsto, šele
  nato se `CustomNpcs.Server` nastavi na `null`. Nečist drain je eksplicitno zabeležen kot napaka.
- Če save izjemoma nastane brez aktivne server seje, se pot zajame in zapis izvede sinhrono;
  podatki se ne odložijo na neupravljano globalno vrsto.
- Testi pokrijejo svet A → drain → svet B z istim imenom igralca, zavrnitev poznega zapisa ter
  propagacijo I/O napake v rezultat draina. Primerjalni bytecode test potrdi staro in novo vezavo.
- Vseh 31 primerjalnih testov ter 7 testov varnih writerjev/session je zelenih v Javi 8.
- Build zapakira 40 razredov; `verify-package` je pregledal 1716 originalnih vnosov in našel
  samo 25 pričakovanih zamenjav originalnih class datotek.

**Ni narejeno in zakaj:**

- Zaklenjen cilj, zavrnjen dostop in procesne prekinitve spadajo v M1.9 fault injection.
- Splošni `CustomNPCsScheduler` ni ugasnjen, ker ga uporabljajo klient, paketni sendi, GUI zamiki
  in `BankData`; slednji je ločen bug B4 in se ne sme spreminjati v tem paketu.

**Ugotovitve:**

- Edini pregledani datotečni zapis na globalnem schedulerju je bil `PlayerData.save`; drugi
  uporabniki izvajajo omrežne ali GUI naloge, zato bi globalni shutdown povzročil regresije.

**Spremembe obnašanja:** pending player zapisi se ob ustavitvi izpraznijo pred resetom serverja;
zapis starega sveta ne more uporabiti poti novega sveta.

**Meritve:** nobene

**Naslednja seja:** M1.9, fault injection in zaključni kriteriji integritete podatkov.

---

### 2026-09-11 (7) — M1.5d: varen zapis stisnjenega NBT

**Paket:** M1.5 (četrti del)
**Stanje:** končano

**Narejeno:**

- `BankController`, `FactionController`, `GlobalDataController`, `TransportController`,
  `RecipeController`, `SpawnController`, klientov `PresetController` in `SchematicController`
  so bili vsak preneseni v prevedljivo drevo z ločenim baseline commitom.
- Dodan `rework/data/CompressedNbtFile`, ki stisnjeni NBT zapiše skozi `SafeFileWriter`, ga
  ponovno prebere in zahteva enak NBT pred atomsko zamenjavo cilja.
- Vseh osem controllerjev zdaj zapisuje neposredno na končno pot; odstranjena so zaporedja
  `.dat_new` → brisanje/preimenovanje in neposredno prepisovanje schematic datoteke.
- Regresijski test je pred popravkom dokazano padel, po popravku pa bytecode vseh controllerjev
  potrdi odsotnost nevarnih suffixov oziroma uporabo novega varnega writerja.
- Vseh 29 primerjalnih testov ter 5 testov varnih writerjev je zelenih v Javi 8.
- Build zapakira 32 razredov; `verify-package` je pregledal 1716 originalnih vnosov in našel
  samo 20 pričakovanih zamenjav originalnih class datotek.

**Ni narejeno in zakaj:**

- Lifecycle asinhronih player zapisov je ločen problem B2 in ostaja M1.6.
- Širši fault-injection scenariji (zaklenjen cilj, zavrnjen dostop, prekinitev procesa) so M1.9.

**Ugotovitve:**

- `CompressedStreamTools.writeCompressed` zapre podani stream. `CompressedNbtFile` zato uporabi
  non-closing ovoj, da lahko `SafeFileWriter` po koncu stiskanja še izvede `flush` in disk `sync`.
- Prvotni seznam šestih world controllerjev ni bil popoln; isti problem sta imela tudi klientov
  preset zapis in neposreden schematic zapis, zato sta vključena v isti paket.

**Spremembe obnašanja:** banke, factioni, globalni podatki, transporti, recepti, spawni,
klientovi preseti in schematiki ob neuspelem zapisu ohranijo zadnjo veljavno datoteko.

**Meritve:** nobene

**Naslednja seja:** M1.6, upravljan lifecycle asinhronih zapisov.

---

### 2026-09-11 (6) — M1.5c: varen zapis sinhronih JSON datotek

**Paket:** M1.5 (tretji del)
**Stanje:** končano

**Narejeno:**

- `DialogController`, `QuestController`, `LinkedNpcController` in `RoleTrader` so bili vsak
  preneseni v prevedljivo drevo in shranjeni v ločenem baseline commitu pred spremembo vedenja.
- Pri `RoleTrader` so bili odstranjeni samo dekompilatorski casti `Object`, ki se z generičnim
  `ItemStack` seznamom niso prevedli; runtime vedenje baselinea se s tem ne spremeni.
- Vsi štirje zapisi zdaj podajo končno `.json` pot neposredno v `NBTJsonUtil.SaveFile`.
  Odstranjeno je zunanje zaporedje `_new` → izbriši cilj → nepreverjen `renameTo`.
- Novi regresijski test pregleda bytecode konstantne nize: proti originalu potrdi stari nevarni
  suffix, proti popravljeni kodi pa zahteva njegovo odsotnost. Pred popravkom je test dokazano padel.
- Vseh 27 primerjalnih testov ter 4 testi varnega writerja so zeleni.
- Build zapakira 20 razredov; `verify-package` je pregledal 1716 originalnih vnosov in našel
  samo 12 pričakovanih zamenjav originalnih class datotek.

**Ni narejeno in zakaj:**

- Banke, factioni, globalni podatki, transporti, recepti in spawni uporabljajo stisnjeni NBT;
  spadajo v M1.5d in zahtevajo svoj validator za binarni format.
- Lifecycle asinhronih player zapisov ostaja M1.6.

**Ugotovitve:**

- `ScriptController` že zapisuje neposredno na končne poti, zato je postal varen z globalnim
  popravkom `NBTJsonUtil.SaveFile` iz M1.5a in ne potrebuje lastnega prenosa v tem paketu.

**Spremembe obnašanja:** dialogi, questi, linked NPC podatki in trader marketi ohranijo zadnjo
veljavno JSON datoteko, če nov zapis ali njegova validacija odpove.

**Meritve:** nobene

**Naslednja seja:** M1.5d, varen zapis controllerjev s stisnjenim NBT.

---

### 2026-09-11 (5) — M1.5b: varen zapis PlayerData

**Paket:** M1.5 (drugi del)
**Stanje:** končano

**Narejeno:**

- `PlayerData` prenesen v prevedljivo drevo brez funkcionalnih sprememb in shranjen v ločenem
  baseline commitu.
- Dodan karakterizacijski test privzetega `PlayerData` NBT snapshota; zelen je proti originalu
  in obnovljenemu razredu.
- Asinhroni zapis igralca ne uporablja več `_new` → izbriši cilj → nepreverjen `renameTo`,
  ampak ciljno JSON datoteko preda `NBTJsonUtil.SaveFile` oziroma `SafeFileWriter`.
- Vseh 22 primerjalnih testov ter 4 testi varnega writerja so zeleni.
- Build zapakira 15 razredov; `verify-package` je pregledal 1716 originalnih vnosov in našel
  samo 7 dovoljenih zamenjav.

**Ni narejeno in zakaj:**

- Zajem poti/sveta in zaključevanje async vrste ostajata nespremenjena; to je M1.6, ne B1.
- Drugi JSON in stisnjeni NBT controllerji ostajajo M1.5c/d.

**Ugotovitve:**

- `PlayerData` povezuje B1 in B2, zato sta bila atomski zapis in async lifecycle namenoma
  ločena v zaporedna paketa.

**Spremembe obnašanja:** veljaven player JSON se ne izbriše pred uspešnim zapisom in validacijo novega.

**Meritve:** nobene

**Naslednja seja:** M1.5c, sinhroni JSON controllerji.

---

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
  Vse podrobnosti: [`meritve/2026-09-11-M1-nbtjson.md`](../meritve/2026-09-11-M1-nbtjson.md).
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

**Meritve:** [`meritve/2026-09-11-M1-nbtjson.md`](../meritve/2026-09-11-M1-nbtjson.md)

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

