# P5 — Java v vseh script GUI-jih in samodejni `RwJavaClasspath` (2026-10-30)

**Paket:** P5 (Q248, Q263–Q266) · **Izhodišča:** [M6.12](2026-10-29-M6.12-izbira-jezika.md) (Q184–Q187),
[M6.11](2026-10-29-M6.11-player-world.md). **Stanje:** sprejet na poskusu 2 (Q268), privzeto prazno.

## Spremembe

- **Java v player/forge/block/door/item (Q263).** `PlayerScriptData`, `ForgeScriptData`, `TileScripted`,
  `TileScriptedDoor` in `ItemScriptedWrapper` (kopije originala) dobijo isto vejo kot `DataScript`:
  pri `ScriptLanguage=Java` tabi tečejo prek `JavaNpcScripts.handler()` (isti zaporedni red, konzola,
  generacije, B5 obnova, `ScriptContainer.Current`). Vsi GUI-ji dobijo isti seznam jezikov kot NPC
  (`withoutJava` odstranjen; vrstice `PacketHandlerServer` so spet originalne). Projektil ostane brez
  Jave (nima GUI-ja), `ScriptHooks.V1_EXCLUDED = [projectile]`.
- **Vezava po imenu (Q264).** Imen hookov teh handlerjev je veliko (EnumScriptType, forge imena po
  razredu dogodka), zato `JavaHookBinder.bindByName`: vsaka javna nestatična metoda z enim parametrom
  tipa `Event` (ali širšim) je hook pod svojim imenom; dve taki metodi z istim imenom = `hook.ambiguous`.
  Ob klicu `JavaNpcScripts` preveri, da parameter sprejme dogodek; sicer `hook.signature` v konzoli taba
  in tab se ustavi (napaka se ne zamolči). NPC vezava (registry) ostane enaka.
- **`RwJavaClasspath=auto` (Q265).** `AutoClasspath`: vse datoteke `LaunchClassLoader.getSources()`
  (zagonski classpath, Forge, modi, vgnezdene knjižnice) v vrstnem redu nalagalnika. V produkciji
  (`runtimeDeobf=1`) so Minecraft razredi na disku obfuscirani, zato se ob prvem zagonu enkrat
  zapiše `customnpcs-java/minecraft-<ključ>.jar`: razredi iz mappinga + `net/minecraft/` vnosi
  igralnega jarja skozi **samo Forgeove** transformerje v vrstnem redu nalagalnika (binarni popravki,
  deobfuskacija SRG, AT, SideOnly); coremodi modov in mixini se ne ponavljajo. Ključ = poti/velikosti/
  časi virov + imena transformerjev; ob restartu se jar ponovno uporabi. V dev (MCP) jar ni potreben.
  Gradi se v `FMLServerAboutToStartEvent` (pred prvim tickom), napaka gre v log in v konzolo taba.
  Prazno ostane privzeto (original, brez Jave); ročna pot/datoteka deluje kot prej.
- **Q266 (testi):** unit (`ScriptHooksTest` +3, `AutoClasspathTest` 7, `JavaLanguageTest`), build,
  package, nov **p5-handlers-v1** (`java-hooks-run.ps1 -Mode packaged -Package p5`, pravi pakirani
  Forge strežnik s `RwJavaClasspath=auto`) in regresija **m62-packaged-v3** (NPC Java pot v
  `JavaNpcScripts` spremenjena). Cene ni (privzeto prazno, Q92).

## Scenarij p5-handlers-v1

En pakiran strežnik, bootstrap (prvi zagon zapiše jar) + zagon (jar ponovno uporabljen). Kontrolni NPC
(JS) enkrat požene `HandlerJavaProbe`: za vsak handler JS tab in nato Java tab prek istega NBT kot
GUI save paket (`setPlayerScripts`, `setForgeScripts`, `TileScripted/TileScriptedDoor.setNBT`,
`ItemScriptedWrapper.setMCNbt`), en pravi `EventHooks` dogodek (player timer, forge `WorldEvent.Save`,
block/door timer, item update), sledi JS = Java. Java tabi uporabljajo `WorldServer` (game razred
mora priti iz samodejnega classpatha). Seznam jezikov GUI vsebuje Java. Java tab z napačnim tipom
dogodka → `hook.signature`, nič ne teče. 19 preverjanj + harness preveri jar in log vrstici.

**Omejitve:** brez pravih GUI klikov za nove handlerje (klientski `GuiScriptInterface` je skupen, M6.12
m612 ga je preveril za NPC); dev način `auto` ni zahtevan; spremembe razredov iz coremodov modov in mixinov
niso v jarju (AT modov so, prek Forgeovega `ModAccessTransformer`; Q265); pravi modpack ni preverjen (Q1).

## Rezultati

Prvi zagon p5-handlers-v1 (odtis cf079b80…) je padel pred zagonom strežnika na predpogoju
`config copy has no RwJavaClasspath yet`: `dev/run/config` zdaj vsebuje prazno vrstico ključa.
Popravek harnessa (Q267), ne nov poskus.

**Poskus 1 (odtis 5261f2e9…): FAIL, 17/19.** Strežnik in jar `auto` sta delovala (bootstrap zapisal,
restart `reused`); player, block, door, item JS = Java, seznam jezikov vsebuje Java, napačen tip
dogodka → `hook.signature` brez teka. Padla sta `forge-java-equals-js` in `forge-java-not-errored`:
`ForgeTab.java:7:81 incompatible types: amu cannot be converted to net.minecraft.world.WorldServer`.
Vzrok: Forgeovi razredi v universal jarju na disku kličejo obfuscirana imena (`WorldEvent.getWorld()`
vrne `amu`) in dobijo SRG imena šele v nalagalniku; jar `auto` je vseboval samo `net/minecraft`.
Popravek (Q268, poskus 2, zadnji): v jar gredo tudi `net/minecraftforge/` razredi Forge jarja skozi
isto verigo, razen paketov `classLoaderExceptions`/`transformerExceptions` (nalagalnik jih ne
spreminja); ključ dobi `format 2`, zato star jar ni ponovno uporabljen.

**Poskus 2 (odtis 6531b341…): PASS.** unit, build, package, **p5-handlers-v1** in regresija **m62-packaged-v3**
zeleni. p5: 19/19 (`audit/p5-packaged-2026-10-30-223208-proof.json`); sledi JS = Java za vseh pet handlerjev
(`player:5:[Minecraft]`, `forge:true`, `block:6:…npcscripted`, `door:6:…npcscripteddoor`, `item:…scripted_item`);
napačen tip → `hook.signature: … does not accept BlockEvent.TimerEvent`, tab ne teče. Jar `auto`: bootstrap 68 virov,
3.601 razredov, 1.976 ms (7 razredov Forgeova veriga zavrne, niso na classpathu); restart `reused`, 7 ms.
m62-packaged-v3: NPC Java pot nespremenjena (`audit/m62-packaged-2026-10-30-223241.json`).

**Omejitve:** enkraten strošek ~2 s ob prvem zagonu (podatek); razredi, ki jih spreminjajo coremodi/mixini modov,
so v jarju v Forgeovi obliki; brez pravih GUI klikov za nove handlerje; projektil brez Jave; pravi modpack ni
preverjen (Q1).
