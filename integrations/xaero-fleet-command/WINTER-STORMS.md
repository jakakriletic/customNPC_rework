# Xaero Fleet Command 1.12.2 — v0.1.8 Winter

Razsiritev uporabnikovega `xaerofleetcommand-1.12.2-v0.1.7.jar`.
Ohrani ladijsko mapo in doda tretji zavihek **Winter storm**.

## Uporaba

1. Odpri Xaerovo World Map in izberi **Winter storm** na vrhu (ali preklapljaj s Tab).
2. Klik izbere eno celico 64 x 64 blokov. **Ctrl + vleka** izbere pravokotnik.
   Navadna vleka se vedno premika po mapi. Escape pocisti oznaceno obmocje.
3. Izberi trajanje **2, 10 ali 30 minut** in klikni **Spawn blizzard**.
4. Za konec oznaci obmocje in klikni **Clear storms in area**.

Aktivne nevihte so obarvane na mapi in imajo prikazan preostali cas.
Najvecje oznaceno obmocje je 16 x 16 celic (1024 x 1024 blokov).
Na dimenzijo je lahko aktivnih do 32 neviht. Obmocja zunaj meje sveta so zavrnjena.

## Metez in mraz

- Gost, diagonalno odpihan sneg v vseh povrsinskih biomih, brez spreminjanja biomov.
- Modrikasto bela megla z vidljivostjo okoli 18 blokov v jedru nevihte.
- Sunki vetra potiskajo izpostavljene igralce; cepenje zmanjsa silo.
- Neprekinjen zvok vetra v kategoriji Weather; v zavetju je tise.
- Nevihta se postopno okrepi in umiri; ucinek se mehko preliva na robovih obmocja.
- Brez oklepa po priblizno 15 sekundah polne izpostavljenosti zacnes izgubljati
  **eno srce na dve sekundi**. To lahko igralca ubije.
- Vsak kos oklepa zmanjsa nabiranje mraza in poskodbe. Stiri kose poskodbe preprecijo.
- Streha ter ogenj, lava, magma ali prizgana pec v blizini nudijo zascito; mraz v zavetju popusca.
- Creative in spectator igralcev mraz in sunki ne prizadenejo.
- Kazalnik **Cold / Freezing** in stevilo kosov oklepa sta spodaj levo.

Snezne plasti se ne polagajo na bloke. Generiranje sveta in globalno vanilla vreme
ostaneta nedotaknjena; nevihte ne nalagajo oddaljenih chunkov.
Nevihte se shranijo v svet in trajajo po casu igranja, tudi po ponovnem zagonu.

## Namestitev

Uporabi en sam Xaero Fleet Command JAR v `mods`. Novi JAR nadomesti starega;
obeh ne namescaj hkrati. Xaero World Map ostane locen mod.
Preverjeno z Xaero World Map **1.34.1** iz uporabnikovega modpacka.

**Multiplayer:** isti novi JAR potrebujejo streznik in igralci, ki zelijo videti
meteze. Ustvarjanje in odstranjevanje preveri streznik. Privzeto je dovoljeno OP
igralcem (permission level 2); v singleplayerju lastniku sveta tudi brez vklopljenih goljufij.
Stari streznik se vedno podpira prejsnje ladijske ukaze, ne pa novega sloja neviht.

`config/xaerofleetcommand.cfg`, kategorija `winterStorms`:

- `enabled`: vklop lokalnih neviht.
- `allowAllPlayers`: dovoli ustvarjanje/odstranjevanje tudi ne-OP igralcem.
- `coldDelaySeconds`: privzeto 15.
- `coldDamage`: privzeto 2 HP (= eno srce) na dve sekundi.
- `windPush` in `windStrength`: fizicni sunki vetra.

Po spremembah strezniske konfiguracije ponovno zazeni igro oziroma streznik.

## Preverjanje in izvor

- 62 uspesnih enotskih/regresijskih testov: ladijska izbira in ukazi, koordinatni
  sistem mape, tretji zavihek, mreza, robovi sveta, paketi, shranjevanje neviht in mraz.
- Locen Forge testni svet z dejanskim Xaero 1.34.1: ustvarjanje nevihte prek omreznega
  paketa in sinhronizacija odjemalca; nezaščitenemu igralcu zdravje pade z 20 na 10;
  s polnim oklepom ostane 20; pod streho zdravje ostane 20, mraz pa 0.
- Posnetki zaslona so v `run/screenshots`; testni pomocni mod je v `src/qa`, locen
  od glavnega moda in ni del koncnega JAR-a.

Nespremenjeni original je v `baseline`. Izvorna koda obstojecega moda je bila
najdena v `Desktop/ladja_mod/milestones/xaero-fleet-command`; ta projekt je samostojna
kopija, zato prvotnega razvojnega projekta ne spreminja.

Gradnja: JDK 21 za Gradle 8.12, JDK 8 za Minecraft. `gradlew.bat test build`.
Lokalno pot JDK 8 v `gradle.properties` po potrebi prilagodi.
