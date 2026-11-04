# P2 — klientska matrika (popravek omejitve M10.1d)

**Stanje:** matrika zelena v vseh štirih celicah po ponovnem odprtju (Q407, Q434–Q436, 4. 11.; zadnji razdelek). Prvotni
zaključek z omejitvijo (Q254–Q255) je spodaj ohranjen kot zgodovina. Popravek login synca (Q251) ostane v produkciji.

## Vzroki padcev M10.1d (iz obstoječih logov, `audit/m101d-*-2026-10-30-*`)

| Celica M10.1d | Simptom | Vzrok |
|---|---|---|
| dedicated K/K, original-server K/O | `Timeout JOIN screen=null`, klient brez kategorij dialogov | Originalni `PacketHandlerClient.onPacketData` bere `mc.player` na Netty niti in paket **zavrže**, če je še `null`. Login sync (`SyncController.syncPlayer` v `PlayerLoggedInEvent`) pride pred obdelavo `SPacketJoinGame` in se izgubi. Integrated je prešel samo zato, ker klient in strežnik delita statične kontrolerje. |
| original-client O/K | `Timeout ACK:SCENE` | Dedicated strežnik ne-opu zavrne vsak ukaz razen tell/help/me/trigger (`EntityPlayerMP.canUseCommand`); `/m10probe setup` se ni izvedel. |
| integrated K/K | `Missing Locked` | NPC-ji pri x = -2/-3 (chunk -1) so se spawnali v nenaložen chunk takoj po teleportu igralca z mesta spawna (≈747, -234); `spawnEntity` je tiho vrnil false. |

## Popravki (Q251, Q252)

- **Produkcija (Q251, vedno vklopljen popravek):** `noppes.npcs.client.PacketHandlerClient` — paket se vedno
  uvrsti v nit klienta, igralec se prebere šele tam. Netty uvrsti paket za prijavo prej, zato je igralec takrat
  že nastavljen; brez igralca (odjava) se paket zavrže kot prej. Izgubljen login sync je napaka originala.
- **Harness (Q252):** `ops.json` z offline UUID igralca `M101D_Player` v izoliranem imeniku; chunk se naloži pred
  spawnom in zavrnjen spawn pade glasno; fiksna barva oči (8339378) namesto naključne; SCENE čaka vseh 6 NPC-jev;
  diagnostika stanja ob timeoutu. Samo krak z **originalnim klientom** sme po 10 s brez sinhronizacije enkrat
  zahtevati enak `syncPlayer` (`/m10probe resync`) — to je izmerjena omejitev originala, ne kandidata; za K-klient
  strežnik `resync` zavrne in skript preveri, da ga ni bilo. Notranji časi skrajšani (Q250: celica ≤ 1800 s).
- Pogoji, trditve, ključi in štiri celice M10.1d so sicer enaki.

## Rezultati

**Poskus 1** (fingerprint 860fc952…): unit, build, package in `p2-compile-v1` zeleni. `p2-integrated-v1` je padel
na `exactly 13 assertions` (`audit/p2-klient-integrated-2026-10-30-205927-*`), čeprav je klient zapisal vseh 13
trditev `ok=true` (login sync, trader, bank, skin, reload) in strežnik edit 13/13 `ok=true`. Vzrok je harness:
v PowerShell 5.1 `ConvertFrom-Json` odda JSON tabelo kot en objekt, zato je `@(...)` štel 1 vrstico. Napaka je bila
v `Read-Checks` že v M10.1d, a tja ni prišel noben tek. Popravek `@((… | ConvertFrom-Json))` (Q253); merila, ključi
in pogoji nespremenjeni. Sprememba harnessa = poskus 2, novi ID-ji `p2-*-v2`.

**Poskus 2** (fingerprint b9d83659…): unit, build, package, `p2-compile-v2` zeleni.

- `p2-integrated-v2` **PASS** (K/K integrated): vse faze, vključno z login syncom, kliki dialog/quest, trader, bank,
  skin fallback, reload tekstur, offline mail in relogin playerdata.
- `p2-dedicated-v2` **FAIL** (K/K dedicated, `audit/p2-klient-dedicated-2026-10-30-210544-*`): faza edit je zelena —
  klient 13/13 (`login-sync-dialogs/quests/factions` prek pravega dedicated strežnika, kar je bil vzrok JOIN timeouta
  v M10.1d), strežnik 13/13, 7 posnetkov, strežnik preživel nepravilen paket. Padec je v naslednjem koraku harnessa:
  `/m10probe offline` kliče `PlayerDataController.addPlayerMessage` za igralca, ki se je prvič prijavil v **istem**
  procesu strežnika. `nameUUIDs` se v originalu napolni samo ob zagonu strežnika, zato `getDataFromUsername` vrne
  `null` → NPE (`PlayerDataController.java:87`). Integrated pred tem korakom strežnik ponovno zažene, dedicated ne.
  To je omejitev originala (pravi mailbox prek `hasPlayer` takemu igralcu javi, da ne obstaja), ne popravka P2.
- `p2-original-client-v2`, `p2-original-server-v2` **nista zahtevana**: oba kraka uporabljata dedicated strežnik in
  isti korak offline maila, zato bi padla na isti napaki harnessa; poskusov ni več (Q92, Q255).

**Zaključek:** popravek Q251 je dokazan na integrated in na dedicated K/K (login sync 3/3 trditve na dedicated, prej
JOIN timeout). Ni dokazano: dedicated relogin/offline mail/playerdata po restartu, mešana para O-klient/K-strežnik in
K-klient/O-strežnik. Popravek harnessa (restart pred offline mailom ali prejemnik iz `nameUUIDs`) je v2.

## Dokončanje 4. 11. (Q407, Q434–Q436)

**Q434, harness:** pred offline mailom se tudi dedicated strežnik znova zažene, ker izvirni `nameUUIDs` nastane samo ob
zagonu. Merila in ključi so nespremenjeni. Na `e48eda6d…` je `p2b-dedicated-v1` s tem prešel.

**Q435, napaka produkcije:** `p2b-original-client-v1` je na `e48eda6d…` padel: `Fatally missing registry entries`,
`customnpcs:army_staff` (M11.8a, Q336). Originalni klient se sploh ni mogel povezati na K strežnik, tudi pri `RwArmies=0`.
Popravek: dedicated strežnik registrira palico samo pri `RwArmies>=1`, klient vedno; `MissingMappings` prezre shranjeni
vnos. Harness `off-item` (`m118a`, `m118b`) zdaj zahteva, da predmeta na strežniku pri 0 ni.

**Rezultati na `e22ef824…` (controller PASS):**

| Test | Klient/strežnik | Rezultat |
|---|---|---|
| `p2b-dedicated-v1` | K/K dedicated | klient 13 + 5, strežnik 13 + 5, offline 1 |
| `p2b-original-client-v1` | O/K | enako; originalni klient enkrat `resync` (izvirna napaka, Q251) |
| `p2b-original-server-v1` | K/O | enako; brez `resync`, register brez napake |
| `p2b-integrated-v1` | K/K integrated | enako |
| `m118a-gui-v1` | K/K, `RwArmies` 1→0 | 98 preverb; svet pri 0: `Found a missing id`, brez `Unidentified mapping`, `rwarmies.dat` bitno enak |
| `m118b-permissions-gui-v1` | K/K, `RwArmies` 1→0 | 102 preverbi; isti pogoj pri 0 |
| unit, build, package | | PASS |

Pet PNG pregledanih (Mailbox z `M101D_Offline` v O/K, K/O in K/K dedicated; trader O/K, bank K/O): pravilni GUI, brez napak.
Ostali PNG niso bili pregledani; njihovo prisotnost preveri harness.

**Cena Q435:** pri prehodu 1→0 na dedicated strežniku palice izginejo iz inventarjev in se pri vrnitvi na 1 ne vrnejo. To je
sklep iz vedenja Forge; samega izgubljanja predmeta harness ne preveri. Sprejeto, ker `RwArmies=1` ostaja izbira, privzeta
nastavitev pa originalnemu klientu ne sme zapreti povezave.

**Meje:** en dev par, en offline igralec, nekaj bližnjih NPC-jev, privzeta stikala (`RwArmies=0`), ni pakiranega klienta (Q22).
O klient na strežniku pri 1 ni bil pognan na končni kodi; zavrnitev sledi iz padca na `e48eda6d…` z istim mehanizmom.
K klient na O strežniku: nedejavnost palice izhaja iz vira Forge, predmet se v tej celici ni uporabljal.
