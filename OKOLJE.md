# Razvojno okolje — CustomNPCs 01Oct19

Okolje je vezano na originalni `CustomNPCs_1.12.2-(01Oct19).jar` v tej mapi. Ne uporablja drugega CustomNPCs forka. Original mora imeti SHA-256 `cafacade45fb2aa6ac52956487889d0f4a9d4f6a1682ea28a1f7aca1ba100fa1`.

## Verzije

- Minecraft **1.12.2**.
- Forge **14.23.5.2847**, MDK iz uradnega Forge Maven repozitorija.
- Java **Temurin 8u492-b09**, lokalna kopija v `.tools/jdk8`.
- Gradle wrapper **4.9**.
- ForgeGradle **2.3-SNAPSHOT**, uporabljeni artefakt 2.3.4-gfc67182, SHA-256 `5e8d1af585b0936a40ccc6489fa9871da008a41b4f481e462d1dc61279f0274e`.
- MCP mappings **snapshot_20171003**, kot jih določa ta MDK.

To je izbrano kompatibilno okolje za Minecraft 1.12.2, ne dokaz, da je avtor original zgradil s točno istim Forge/JDK buildom. Natančen originalni build recept ni priložen JAR-u. Java je lokalna; Gradle in večje Minecraft odvisnosti uporabljajo že obstoječi uporabniški `.gradle` cache. Sistemske Java nastavitve niso spremenjene.

## Kje je koda

- `audit/decompiled`: prvotni SRG izpis iz JAR-a.
- `dev/reference-src`: celoten dekompiliran izpis s preslikanimi Minecraft imeni; namenjen branju, ni v celoti prevedljiv izvorni projekt.
- `dev/src/patch/java`: razredi, ki jih dejansko prevajamo in lahko popravljamo. Za začetek je obnovljen `noppes/npcs/entity/data/DataTimers.java`, brez funkcionalnega bugfixa.
- `dev/src/test/java`: testi obstoječega obnašanja. Enaki testi se izvajajo nad originalnim preslikanim binarnim razredom in nad obnovljeno Java kodo.
- `dev/baseline/customnpcs-original-01Oct19.jar`: nespremenjena razvojna kopija originala, izven classpatha.
- `dev/libs/customnpcs-mapped-01Oct19.jar`: isti original, preslikan s ForgeGradle za razvojni Minecraft API; to uporablja compile/test/runtime classpath.

Javno objavljenega, dokazljivo ujemajočega izvornega projekta za 01Oct19 pregled ni našel. Zato **ne prevajamo vseh 746 dekompiliranih datotek na slepo**. Razred, ki ga želimo spremeniti, najprej prenesemo iz `reference-src` v `src/patch/java`, odpravimo samo napake rekonstrukcije, preverimo baseline in nato naredimo bugfix. Ostali razredi in resources ostanejo iz točnega originala.

## Ukazi iz glavne mape v PowerShellu

```powershell
# Priprava Minecraft/Forge izvornih odvisnosti
.\dev.ps1 setupDecompWorkspace --offline
.\prepare-assets.ps1

# Test originalnega binarnega razreda in test obnovljene kode
.\dev.ps1 testOriginal test --offline

# Prevedi izbrane razrede in sestavi celoten lokalni testni mod
.\dev.ps1 buildPatchedMod --offline
.\verify-package.ps1

# Razvojni Minecraft klient
.\dev.ps1 runClient --offline

# Razvojni dedicated server (ob prvem zagonu zahteva Minecraft EULA)
.\dev.ps1 runServer --offline

# Projekt za IDE in zapis compile classpatha
.\dev.ps1 idea workspacePaths --offline
```

`--offline` pomeni, da Gradle uporabi že prenesene odvisnosti. Če pozneje dodaš novo knjižnico, bo potreben zagon brez te zastavice. `dev.ps1` nastavi Java 8 za ta proces in preveri exit code. V IDE odpri `dev/build.gradle` in izberi lokalni `.tools/jdk8`, ne sistemske novejše Jave.

Stari ForgeGradle uporablja neustrezen HTTP naslov za assets. Njegov `getAssets` je zato izključen; `prepare-assets.ps1` prenese manjkajoče datoteke prek uradnega HTTPS in preveri SHA-1 prenosa. `dev.ps1 runClient` ta korak izvede samodejno. To je ločeno od Gradlove zastavice `--offline`; ob manjkajočih assets potrebuje internet. JUnit 4.13.2 in Hamcrest 1.3 sta lokalno v `dev/libs`.

`buildPatchedMod` ustvari `dev/build/libs/CustomNPCs_1.12.2-01Oct19-workspace.jar`. Majhen `customnpcs-patch-classes-*.jar` je samo vmesni artefakt in ni samostojen mod. Originalni JAR se nikoli ne prepiše. V zunanjem testnem profilu uporabi samo en celoten CustomNPCs JAR.

Razvojni klient in server uporabljata posebej sestavljen `dev/build/libs/customnpcs-dev-runtime.jar`: preslikani original z vključenimi razredi iz `src/patch/java`. Run task iz classpatha odstrani ostale CustomNPCs JAR-e, da FML najde samo eno instanco moda in se popravki dejansko naložijo. Zato originala ne kopiraj še enkrat v `dev/run/mods` ali `dev/libs`. Testne svetove hrani v `dev/run`; obstoječi uporabniški svetovi niso del tega okolja. Zagon ima omejitev heap-a 2 GB.

## Pomen baseline testov

Test `preservesKnownOriginalIntervalBugUntilExplicitlyFixed` trenutno namerno pričakuje originalni napačni zapis ID-ja v `TimerTicks`. S tem preverjamo, da obnova kode še ni spremenila obnašanja. Pri dejanskem bugfixu spremenimo pričakovanje za popravljeno kodo; test originala ostane ločena referenca oziroma pričakovana reprodukcija buga.

Ti testi ne pokrivajo celotnega moda, AI-ja ali delovanja v uporabnikovem modpacku. Za vsak naslednji bug dodamo reprodukcijo in relevantne integracijske teste.

## Preverjeno med pripravo

- `testOriginal`: 3/3 uspešno.
- `test`: 3/3 uspešno nad obnovljeno kodo.
- `buildPatchedMod`: uspešno, sestavljen celoten lokalni testni JAR.
- `verify-package.ps1`: preverjenih 1.716 originalnih ZIP datotek; razlika je samo v `DataTimers.class` in `DataTimers$Timer.class`. Resources in vsi drugi razredi imajo identično vsebino originalu.
- Log builda: `audit/build-environment.log`. HTML poročili: `dev/build/reports/tests/test/index.html` in `dev/build/reports/tests/testOriginal/index.html`.
- Razvojni klient je uspešno naložil Forge in CustomNPCs; log potrjuje `Forge Mod Loader has successfully loaded 5 mods` (štiri Minecraft/Forge komponente in CustomNPCs). Log: `audit/client-smoke.log`. Ohranjeni so originalni access transformerji iz `FMLAT: cnpcs_at.cfg`.
- `environment-lock.json` vsebuje verzije in hashe ključnih lokalnih artefaktov.

Dedicated server in igranje v svetu še nista preverjena. Razvojni klient uporablja testno identiteto, zato Minecraft Realms prijava ni del tega smoke testa.

Originalni bugi še niso popravljeni. Obnova enega razreda ni zagotovilo enakovrednosti celotnega moda; nadaljnji popravki potrebujejo svoje teste.

Viri: [originalna izdaja](https://www.curseforge.com/minecraft/mc-mods/custom-npcs/files/2799797), [Forge 1.12.2](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.12.2.html), [CFR](https://www.benf.org/other/cfr/).
