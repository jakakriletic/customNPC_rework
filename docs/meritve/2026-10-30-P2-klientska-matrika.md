# P2 — klientska matrika (popravek omejitve M10.1d)

**Stanje:** zaključen z omejitvijo po poskusu 2 (zadnji, Q92; Q254–Q255). Popravek login synca (Q251) ostane v
produkciji; celotna matrika ni zelena, preostanek (dedicated relogin/offline mail, mešana O/K para) je v2.

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
