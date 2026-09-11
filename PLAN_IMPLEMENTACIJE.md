# CustomNPCs — plan bugfixov in performance optimizacij

Datum pregleda: 11. 9. 2026. Predmet: lokalni `CustomNPCs_1.12.2-(01Oct19).jar`, 10.440.159 bajtov.
SHA-256: `CAFACADE45FB2AA6AC52956487889D0F4A9D4F6A1682EA28A1F7ACA1BA100FA1`.

**Priporočilo: najprej obnovljiv build in zaščita podatkov, nato majhni dokazljivi bugfixi, šele potem izmerjene optimizacije.** Brez testnega sveta in profiliranja ni mogoče zagotoviti nič regresij ali pošteno napovedati odstotka pohitritve.

## 1. Kaj je bilo dejansko pregledano

V začetni mapi je bil samo JAR, brez izvorne kode, build konfiguracije, sveta, nastavitev, logov in testov. Z lokalnim Java 8 JDK in [CFR 0.152](https://www.benf.org/other/cfr/) je bilo izdelanih 746 datotek Java za analizo. Niso vse ročno pregledane. Ciljni pregled pokriva shranjevanje igralcev, scheduler, skripte, timerje, pakete, sinhronizacijo, glavni NPC tick, melee AI, spawning, chunk loader in prenos tekstur.

Analitične datoteke so v `audit/decompiled`; pot razreda in številke vrstic spodaj se nanašajo na te datoteke. `audit/DataTimers-bytecode.txt` vsebuje neodvisno preverjanje timer buga z `javap`. Originalni JAR ni spremenjen. Minecraft ni bil zagnan in performance meritve niso bile izvedene.

Dekompilacija ni izvorni projekt: manjkajo Minecraft/Forge odvisnosti, del imen je obfusciran, nekateri izrazi so nepravilno rekonstruirani. Primer je lokalna spremenljivka `pos` v `NPCSpawning`, ki v izpisu nastopa pred inicializacijo; tega ne štejem za dokazan bug. Pred patchanjem sumljive kontrolne logike preveriti bytecode ali ujemajočo izvorno kodo.

JAR metadata navaja `version: 1.12` in `mcversion: 1.12`, ime datoteke pa 1.12.2. Natančen Forge build in mappings je treba določiti, ne sklepati samo iz imena.

## 2. Meje kompatibilnosti

- Ohraniti mod ID, registry imena, entity ID-je, NBT/JSON ključe in javne script API podpise. Ne preimenovati niti javnih metod s tipkarskimi napakami, če jih lahko uporabljajo dodatki.
- Ohraniti packet ID-je oziroma vrstni red enumov. Sprememba protokola mora biti ločen projekt z izrecno določenimi kombinacijami klient/server.
- Ohraniti frekvenco in vrstni red script dogodkov, combat cooldownov, scen, AI odločanja in timerjev, razen pri posebej dokumentiranem bugfixu.
- Operacije nad svetom, entityji, inventoryjem in containerji izvajati na ustrezni glavni niti. V ozadje sodijo I/O in obdelava neodvisnih kopij podatkov.
- Ne združevati script engine-ov med NPC-ji: globalne spremenljivke in stanje skript morajo ostati izolirani.
- Ne uvajati privzetega uspavanja oddaljenih NPC-jev ali zmanjševanja njihove tick frekvence. To bi spremenilo quest, timer in scripted world logiko.
- Vsak popravek naj bo ločen commit z reprodukcijo, testom in opisom morebitne namerne spremembe obnašanja.

## 3. Faza 0 — ponovljiv build in referenčno obnašanje

1. Shraniti nespremenjeni JAR in hash kot referenco; pripraviti ločen razvojni projekt in kopijo testnega sveta.
2. Poiskati izvorni projekt, ki ustreza temu buildu, in preveriti razliko do JAR-a. Če ga ni, obnoviti build iz dekompilacije in ustreznih mappings; ne enačiti uspešne dekompilacije z delujočim buildom.
3. Zakleniti verzije JDK, Forge, Gradle, mappings in odvisnosti. Za začetno referenco preveriti Java 8, zlasti zaradi Nashorn skript; ne izvajati hkrati migracije Jave.
4. Brez funkcionalnih popravkov zgraditi referenčni kandidat. Preveriti manifest, resources, registracije, javni API in packet enume; zagnati klient in dedicated server.
5. Narediti primerjalni smoke test originala in obnovljenega builda: spawn/edit/save NPC, restart, dialog, quest, trader, banka, companion, script, custom texture in chunk loader.
6. Pridobiti dejansko uporabljeni modpack, Forge verzijo, konfiguracijo, skripte in kopijo sveta za integracijsko preverjanje. Do takrat uporabiti umetne testne podatke, združljivost s konkretnim modpackom pa ostane odprta.

**Pogoj za nadaljevanje:** obnovljeni build se za pokrite scenarije obnaša enako kot original. Če tega ni mogoče doseči, najprej rešiti obnovo; ne nalagati optimizacij na nepreverjeno osnovo.

## 4. Prednostni bugfixi

### B1 — varen zapis in migracija podatkov, P0

**Dokaz:** `controllers/data/PlayerData.java:191–203` ustvari `_new`, izbriše cilj in ne preveri rezultata `renameTo`. `loadPlayerDataOld`, vrstica 214 naprej, po branju izbriše `.dat`, še preden ta metoda zagotovi trajen JSON zapis. Podobni vzorci preimenovanja so v Bank/Faction/GlobalData/Transport/Recipe/Spawn controllerjih in drugih shrambah.

**Posledica:** možnost izgube zadnje veljavne datoteke ob odpovedi zapisa, preimenovanja ali prekinitvi migracije. To je vidno tveganje kode, ne dokaz, da so uporabnikovi podatki že poškodovani.

**Implementacija:** skupen pripomoček za zapis začasne datoteke v isti mapi, zapiranje in po potrebi prisilni flush, preverjanje berljivosti ter zamenjava cilja z atomskim premikom, kjer ga datotečni sistem podpira. Za nepodprto atomsko zamenjavo definirati preverjen postopek z ohranjeno varnostno kopijo in obnovo. Napako posredovati klicatelju in pustiti zadnjo veljavno verzijo nedotaknjeno. Migracija naj najprej zapiše in ponovno prebere novi format; šele nato arhivira stari vir. Pokvarjenega obstoječega JSON-a ne obravnavati kot novega praznega igralca in ga nato tiho prepisati.

**Test:** normalen zapis; zaklenjen cilj; zavrnjen dostop; simulirana I/O napaka; prekinitev pred/po zamenjavi; pokvarjen JSON; ponovni zagon med migracijo. Po vsakem neuspehu mora ostati obnovljiva zadnja veljavna verzija. Windows preimenovanje testirati na Windows.

### B2 — življenjski cikel asinhronih zapisov, P0

**Dokaz:** `PlayerData.save` mapo sveta izračuna znotraj odložene naloge. `CustomNPCsScheduler.java:11` ima statičen executor z eno nitjo brez metode za zaključek. `CustomNpcs.stopped`, vrstica 294, nastavi `Server = null`; `getWorldSaveDirectory`, vrstica 312, je odvisen od trenutnega globalnega `Server`.

**Posledica:** ob neizpraznjeni vrsti in ustavitvi/zamenjavi sveta obstaja pot do napačnega ciljnega imenika. Ena nit sicer zaporedno izvaja naloge; težava ni več hkratnih writerjev znotraj tega executorja, temveč njegov lifecycle in pozno izbrana pot.

**Implementacija:** pri zahtevi za zapis zajeti absolutno pot, identiteto seje sveta in neodvisen snapshot. Uvesti upravljan zapisovalnik za vsako server sejo. Ob ustavljanju prenehati sprejemati nove zahteve šele po zajemu končnega stanja, nato izprazniti vrsto pred resetom globalov. Napake in timeout ne pomenijo uspešno shranjenih podatkov. Ob naslednjem svetu ustvariti novo sejo. Preveriti vse uporabnike skupnega schedulerja, tudi klienta; ne izklopiti globalne storitve na slepo.

**Test:** umetno upočasnjen disk, zaporedni save-i istega UUID-ja, logout in takojšen stop, prehod svet A → B v istem procesu. Noben zapis A ne sme pristati v B ali v delovni mapi.

### B3 — napačno shranjen interval timerja, P1

**Potrjeno tudi z bytecode:** `entity/data/DataTimers.java:70` zapiše `TimerTicks = timer.id`, branje pa to polje uporabi za trajanje timerja.

**Implementacija:** zapisati `timer.timerTicks`. Ohraniti `Ticks`, repeat flag in obstoječo tick semantiko. Posebej ne popravljati hkrati izraza `ticks-- > 0` ali pomena `reset()`, ker to spremeni trenutek proženja skript.

**Test:** repeating timer ID 7, interval 200; po save/load ostaneta interval 200 in preostali čas nespremenjena. Dodati one-shot, več ID-jev in NPC/player/block variante.

**Stari podatki:** že napačno shranjenega prvotnega intervala ni mogoče zanesljivo rekonstruirati samo iz ID-ja. Ne ugibati in ne množično spreminjati vrednosti, kjer sta ID in interval enaka. Interval ponovno nastaviti iz znane skripte ali drugega zaupanja vrednega vira; dokumentirati omejitev.

### B4 — sprememba bančnega containerja na worker niti, P1

**Dokaz:** `controllers/data/BankData.java`, od vrstice 113, po 300 ms na skupnem schedulerju bere aktualni container in kliče `container.setCurrency(item)`.

**Implementacija:** po izteku zamika delo predati glavni server niti in preveriti povezavo igralca, izvorni window/container, banko ter slot. Zajeti potrebno kopijo itema. Če je igralec medtem odprl drug meni, starega callbacka ne uporabiti na njem. Ohraniti začetni zamik, dokler test ne potrdi, da ga je mogoče odstraniti.

**Test:** odpiranje bank A/B v hitrem zaporedju, zapiranje pred 300 ms, logout, visok ping, zapisovalna vrsta pod obremenitvijo. Nobene napačne valute, podvajanja ali spreminjanja drugega menija.

### B5 — reload skript preskoči prej manjkajoče dogodke, P1

**Dokaz:** `controllers/ScriptContainer.java:130` zavrne `unknownFunctions` in `errored` pred preverjanjem `lastLoaded` pri vrstici 137. Če se kliče le prej manjkajoča funkcija, preverjanje nove verzije kode ni doseženo.

**Implementacija:** preveriti generacijo kode pred zgodnjimi izhodi, ki so odvisni od stare kode. Ob spremembi invalidirati sestavljeno kodo in seznam manjkajočih funkcij. Ponastavitev stanja napake vezati na dejanski reload/spremembo, ne na vsak tick. Raje uporabiti monoton števec revizij kot odvisnost od milisekund ure. Ohranjanje engine globalov preveriti z referenčnimi skriptami.

**Test:** dodati prej manjkajočo funkcijo v vključeno skripto, reload in proženje samo tega dogodka; popraviti prej pokvarjeno skripto; spremeniti jezik; več reloadov v kratkem času. Ohraniti vsebino `event` in obstoječih globalnih spremenljivk po dogovorjeni reload semantiki.

**Dodatni kandidat:** `Current`/`CurrentType` sta globalna in notranji dogodek v `finally` nastavi `Current = null`. Za gnezdene klice preveriti potrebo po shranitvi in obnovi predhodnega konteksta; test zunanja skripta → notranji event → nadaljevanje zunanje skripte. Globalnega locka ne odstraniti brez dokaza o lastništvu niti.

### B6 — preverjanje dolžin paketov, P1

**Dokaz:** `Server.java:279,295` neposredno alocira `new byte[buffer.readInt()]`. Oba server handlerja indeksirata enum z omrežnim številom. Obstajajo permission/tool preverjanja; ne trdim, da jih ni.

**Implementacija:** pred branjem preveriti razpoložljivo glavo, nenegativno dolžino, mejo po tipu in `readableBytes`. Preveriti ID pred indeksiranjem. Za NBT določiti tudi omejitev razširjenih podatkov oziroma trackerja, skladno z dejanskimi velikimi legitimnimi NPC/script podatki. Zavrnitev naj bo nadzorovana in brez poplave stack trace-ov. Preverjanje dovoljenj ohraniti in pregledati še posamezne mutacijske poti.

**Test:** skrajšana glava, negativna/ogromna dolžina, neveljaven enum, poškodovan stisnjen NBT, normalni in največji legitimni paketi. Ne spreminjati wire formata. Lastništvo `ByteBuf` ob odložitvi na server nit preveriti glede na konkretno Forge implementacijo; odsotnosti `retain()` same po sebi ne razglasiti za leak ali use-after-free.

### B7 — offline player podatki s companionom, P1

**Dokaz:** `PlayerDataController.getDataFromUsername` za offline igralca ustvari `PlayerData` brez `player`; `PlayerData.setNBT`, vrstica 94 naprej, ob `PlayerCompanion` dostopa do `this.player.field_70170_p`.

**Implementacija:** ločiti nalaganje podatkov od spawnanja companion entityja. Offline obdržati neobdelan companion NBT in ga pri shranjevanju ohraniti; inicializacijo entityja izvesti ob veljavni prijavi. Sam `if (player != null)` ni dovolj, če nato save companion izbriše. Dodati jasen rezultat za neobstoječega prejemnika pošte in bank ID ter obravnavo nedostopnega imenika namesto slepega `listFiles()`.

**Test:** offline igralec s companionom prejme pošto ali podatkovno spremembo, nato login/restart. Companion in inventory ostaneta enaka, brez duplikatov. Preveriti tudi neobstoječe ime in odstranjeno banko.

### B8 — chunk ticket lifecycle, P2

**Vidno v kodi:** `ChunkController.getTicket`, vrstica 55, po uspešni kreaciji vrne `null`, zato `JobChunkLoader` prvič preskoči uporabo ticket-a. `ticketsLoaded`, vrstica 70, preverja prisotnost NPC-ja v seznamu ticketov. `clear()` samo zamenja mapo; dejanski učinek na Forge ticket-e je treba preveriti v lifecycle-u.

**Implementacija:** po kreaciji vrniti ticket; duplicate preverjanje izvajati po entityju v pravilni mapi. Uskladiti release pri odstranitvi NPC-ja, menjavi joba, unloadu in stopu s Forge lifecycle-om. Ne podvojiti release-a ali spremeniti števila/nabora prisiljenih chunkov.

**Test:** prva aktivacija, gibanje prek meje chunka, negativne koordinate, menjava joba, delete, reconnect/restart, dosežen limit. Število aktivnih ticketov se po ponavljanju ne povečuje brez aktivnih lastnikov.

## 5. Performance optimizacije po meritvah

Vsaka spodnja postavka je kandidat, ne izmerjeno ozko grlo. Najprej izmeriti delež časa in alokacij; nizkovplivne spremembe preskočiti.

| Kandidat | Kaj je vidno | Varen začetni poseg | Glavno tveganje / preverjanje |
|---|---|---|---|
| Timer alokacije | `DataTimers.update()` vsak klic kopira vrednosti v nov `ArrayList`, tudi za prazno mapo. | Najprej zgodnji izhod pri prazni mapi. | Za aktivne timerje ohraniti snapshot, ker callback lahko spreminja mapo. |
| Script sestavljanje in izpis | `getFullCode()` uporablja zaporedno konkatenacijo; vsak event ustvari writerje. | `StringBuilder` pri sestavljanju; profilirati writer alokacije in po potrebi lazy izpis. | Ohraniti newline, vrstni red include-ov, konzolo in gnezdene klice. Ne deliti mutable writerja. |
| Save vrsta | Vsak `PlayerData.save()` serializira snapshot in doda zapis v eno vrsto. | Po B1/B2 meriti globino/čas vrste; šele nato združevati pending zapise istega sveta+UUID-ja. | Najnovejše stanje mora preživeti stop. Ne združevati operacij z vmesnimi stranskimi učinki; ne izgubiti `updateClient`. |
| Imenik igralcev | Startup bere vse JSON-e; lookup imena linearno išče po `nameUUIDs`. | Začetni normaliziran indeks imena → UUID, osvežen pri rename/login/migraciji. | Ohranjanje case-insensitive semantike, starih imen in konfliktov; trajen indeks le z obnovo iz vira. |
| NPC pathfinding | Melee AI že uporablja naključen interval 4–10 tickov za ponovno navigacijo. | Profilirati navigacijske klice; morebitni kratkotrajni reuse samo ob enakem cilju in veljavni poti. | Ne povečati intervala kar globalno. Test premika cilja, blokiranja poti, vrat, stopnic, vode in knockbacka. |
| NPC okoljske poizvedbe | Pogojni AABB pregledi, collision in izračun startY; niso vsi vsak tick. | Meriti posamezne poti; cache le za dokazljivo enakovredne poizvedbe z natančno invalidacijo. | Sprememba blokov, vidljivost, faction, target in world lahko takoj spremenijo rezultat. |
| Spawning | Metoda je klicana iz world ticka, vendar dejansko delo že omeji na vsakih 400 world tickov. | Meriti periodične sunke; zmanjšati začasne objekte brez spremembe RNG in izbire chunkov. | Porazdelitev dela čez ticke ali drug števec NPC-jev lahko spremeni spawn rezultat; ločen opt-in poskus. |
| Login/sync | `syncPlayer` pošilja kategorije in NBT; dirty player sync že obstaja. | Meriti bajte in CPU, cache nespremenljivih serializiranih podatkov z revizijo. | Ne deliti ByteBuf-a z nepravilnim refcountom. Batch po velikosti le ob kompatibilni semantiki `SYNC_ADD/END`; kategorij ni dovoljeno poljubno razrezati. |
| Prenos skinov | `ImageDownloadAlt` ustvari nit za prenos; v pregledani poti ni nastavljenega connect/read timeouta. | Omejen executor, timeouti, deduplikacija identičnih zahtev, fallback ob napaki. | Slika iz cache je lahko neveljavna; preveriti null decode, resource reload, spremembo URL in GL upload na render niti. |
| Render in cache lifecycle | World wrapper ima že omejen LRU cache in clear; drugih leakov pregled ni dokazal. | Profil klienta pri več NPC-jih, teksturah in reloadih; heap primerjava po menjavi svetov. | Ne razglasiti vsake statične mape za leak. Model/cache invalidacija ob opremi, teksturi, animaciji in resource reloadu. |

**Izrecno izven prve izdaje:** večnitni AI, nov pathfinder, deljen script engine, globalno redkejši NPC tick, zamenjava Java verzije, nov save format ali protokol. Vsaka od teh sprememb zahteva samostojen kompatibilnostni projekt.

## 6. Testni in merilni načrt

### Funkcionalna matrika

- NPC: create/edit/clone/delete, linked NPC, NBT roundtrip, smrt/respawn, oprema, hitbox, animacija in bossbar.
- AI: melee/ranged, premikajoč cilj, follow/return/patrol, vrata/stopnice/voda, teleport in menjava dimenzije, različne faction nastavitve.
- Vsebina: dialog availability, quest accept/progress/complete/reward, trader in banka brez izgube ali podvajanja itemov, offline pošta, companion save/load.
- Skripte: NPC/player/block/item dogodki, timer start/stop/reset/forceStart iz callbacka, gnezdeni eventi, include reload, popravljena napaka, storeddata/tempdata. Drugi jeziki le, če so prisotni v dejanskem okolju.
- Lifecycle: dedicated server in singleplayer; logout/login; svet A → B → A; dimension unload; restart med save-i; chunk loader reset.
- Klient: GUI edit, sync, skin download failure, resource reload, veliko različnih skinov. Testirati dejansko uporabljene render dodatke/modpack.
- Omrežje: normalen promet originalnega klienta proti popravljenemu serverju in obratno, če je to obljubljena kompatibilnost; sicer jasno zahtevati usklajeni verziji.

### Performance scenariji

1. Prazen svet in svet brez NPC-jev: preveriti režijo samih sprememb.
2. 50/200/500 NPC-jev; če stroj ne zmore, zaključiti stopnjo in zapisati nasičenje. Ločeno idle, combat/pathfinding, scripts/timers, render in mešana obremenitev.
3. Veliko dialogov/questov ter več zaporednih loginov za sync; večkratni save-i in počasnejši disk za vrsto zapisov.
4. Spawning meriti dovolj dolgo, da zajamemo več 400-tick intervalov. Chunk loaderje testirati z aktivacijo in sproščanjem.
5. Original in kandidat: isti stroj, JVM parametri, seed/scenarij, config in profiler. Praktičen začetni protokol: 2 minuti ogrevanja, 5 minut merjenja, vsaj 3 ponovitve; podaljšati samo, če je šum prevelik.

Meriti MSPT p50/p95/p99 in max, TPS, CPU po call stackih, alokacije/sekundo in GC premore, heap po GC, število niti, dolžino in starost save vrste, trajanje login synca ter promet. Klient posebej: frame time p95/p99, FPS in texture memory. Sam FPS ne meri server optimizacije, sam TPS pri omejitvi 20 pa lahko skrije napredek.

**Sprejem:** vsi relevantni funkcionalni testi uspejo; napredka ali itemov ne izgubljamo; po ustavitvi ni pending zapisov; timer in script trace se ujema z referenco razen dokumentiranih bugfixov. Kandidat za performance ostane le, če izboljšava presega merilni šum in ne uvede ponovljive regresije v drugih scenarijih. Začetni alarm naj bo približno 5 % poslabšanje p95 MSPT/frame time; to ni avtomatska sodba pri šumnih meritvah.

## 7. Zaporedje implementacije in predaje

| Paket | Vsebina | Odvisnost in pogoj zaključka |
|---|---|---|
| 0 | Obnova builda in referenčni testi | Brez funkcionalnih odstopanj v pokritih scenarijih. |
| 1 | Osnovne meritve in reprodukcije B1–B8 | Vsak bug dobi test ali jasno označen nereproduciran status. |
| 2 | B1 + B2, zaščita in lifecycle zapisov | Fault-injection in A/B world prehod brez napačnih zapisov. |
| 3 | B3, B4, B7 v ločenih commitih | Timer roundtrip, varna banka, ohranjen offline companion. |
| 4 | B5, B6, B8 v ločenih commitih | Reload, malformed packet in ticket testi. |
| 5 | Majhne optimizacije iz profilov | Najprej prazni timerji, sestavljanje kode in dokazani hotspot-i; A/B rezultati. |
| 6 | Dražje optimizacije | Samo če po prejšnjih paketih ostane izmerjeno ozko grlo. |
| 7 | Release kandidat na kopiji pravega sveta | Celotna matrika, dolgoročni soak, rollback vaja. |

Za vsak paket predati: source diff, reproducibilen JAR s hashom, rezultate relevantnih testov, meritev pred/po, spremembe obnašanja in znane omejitve. Brez večjih nepovezanih refaktorjev, ki bi otežili pregled.

## 8. Varen prehod in rollback

1. Izdelati konsistentno kopijo sveta in vseh CustomNPCs podatkov pri ustavljenem serverju; shraniti originalni JAR ter konfiguracijo.
2. Kandidata najprej zagnati samo na tej kopiji. Preveriti loge, banker/trader iteme, napredek questov, companion podatke in skripte po ponovnem zagonu.
3. Nato izvesti reprezentativen daljši test, priporočljivo 24 ur z dejanskimi aktivnostmi, ne samo praznim serverjem.
4. Ob prehodu uporabiti dogovorjeno verzijo na klientih in serverju. Visokotvegane performance poskuse imeti privzeto izključene in z ločenimi stikali.
5. Rollback podatkov in JAR-a obravnavati skupaj. Ob vrnitvi na original se lahko ponovno pojavi napačno shranjevanje timerjev. Obnovitev stare kopije sveta izgubi poznejši napredek, zato je to treba upoštevati v času prehoda.

**Prvi konkretni razvojni korak je paket 0. Prva izdaja naj cilja na zanesljivost shranjevanja, timerje, banko in skripte; pospeševanje AI naj sledi meritvam.** Ta dokument je izvedbeni plan na podlagi omejenega statičnega pregleda, ne trditev, da je bilo vseh 746 datotek revidiranih ali da so vse napake moda odkrite.
