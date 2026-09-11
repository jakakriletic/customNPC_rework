# 05 — Protokol seje

Projekt teče čez mnogo sej. Vsaka seja začne brez spomina na prejšnjo. Ta protokol poskrbi,
da se ne izgubi kontekst in da se delo ne podvaja.

**Za AI asistenta v novi seji: preberi to do konca, preden se česarkoli lotiš.**

---

## 1. Začetek seje — vedno, brez izjeme

```
1. Preberi  README.md
2. Preberi  docs/04-STANJE.md          ← kje smo
3. Preberi  docs/05-SEJA-PROTOKOL.md   ← ta datoteka
4. Preberi razdelek v docs/03-FAZE.md za paket, ki ga boš delal
5. Preberi razdelek v docs/02-ZAHTEVE.md za pripadajočo zahtevo
6. Preveri git status — ali je kaj nedokončanega iz prejšnje seje
```

**Ne začni kodirati, dokler nisi prebral 1–5.** Vsak paket ima razlog, zakaj je tam kjer je,
in ta razlog ni v kodi.

---

## 2. Pravila dela

### Obseg

- **Ena seja = en delovni paket.** Če paket ni končan, se to zapiše v `04-STANJE.md` z
  natančnim opisom, kje si obstal.
- Ne začenjaj paketa iz kasnejšega milestona, ker je "hitro". Vrstni red faz ima razloge,
  zapisane v `README.md`.
- Če med delom najdeš nov bug, ga **zapiši** v `04-STANJE.md` in **ne popravljaj zdaj**,
  razen če blokira trenutni paket.

### Koda

- Vsak razred, ki se prvič prenese iz `reference-src` v `src/patch/java`, gre v **ločen commit
  brez funkcionalnih sprememb**, z baseline testom. Šele naslednji commit sme spreminjati
  obnašanje.
- Nova koda gre v `noppes/npcs/rework/…` (D-006). Original se patcha samo tam, kjer je nujno.
- Vsaka sprememba obnašanja ima **stikalo** in privzeto vrednost, ki posnema original (D-007).
- Brez velikih nepovezanih refaktorjev.

### Testi

- Bug brez reprodukcije je hipoteza, ne bug. V `04-STANJE.md` se označi kot
  *"nereproduciran — hipoteza"*.
- Vsak popravek ima test, ki **pade pred popravkom in je zelen po njem**.
- `verify-package.ps1` mora po buildu pokazati **točno pričakovan** seznam spremenjenih
  razredov. Nepričakovan vnos je napaka.

### Dekompilacija

- `reference-src` je za **branje**. Ne popravljaj ga.
- Preden popraviš sumljivo kontrolno logiko, jo preveri z `javap` na originalnem bytecode.
  Primer postopka: `audit/DataTimers-bytecode.txt`.

### Meritve

- "Hitreje" brez številke ne obstaja. Protokol je v `01-ARHITEKTURA.md` §7.
- Kandidat za optimizacijo, ki ne preseže merilnega šuma, se **zavrže**, ne obdrži "za vsak
  slučaj".

---

## 3. Zaključek seje — obvezno

Preden seja konča, **vedno**:

1. **Posodobi `docs/04-STANJE.md`:**
   - datum in trenutni milestone na vrhu
   - nov vnos v "Dnevnik sej": kaj je narejeno, kaj ni, zakaj
   - napredek po milestonih
   - nova odprta vprašanja za uporabnika
   - vsaka sprejeta sprememba obnašanja → tabela "Sprejete spremembe obnašanja"
   - vsaka prevzeta koda → tabela "Prevzeta koda"
   - vsaka meritev → tabela "Meritve" (+ polni zapis v `docs/meritve/`)

2. **Če je bila sprejeta arhitekturna odločitev**, jo dopiši v `01-ARHITEKTURA.md` §9 z
   novim ID-jem `D-0NN`, datumom in razlogom.

3. **Če se je spremenil obseg ali vrstni red faz**, popravi `docs/03-FAZE.md` in
   povzetek v `README.md`.

4. **Commit.** Sporočilo v obliki:
   ```
   <milestone>.<paket>: <kaj>

   <zakaj, in kaj je bilo namerno spremenjeno v obnašanju>
   ```
   Primer: `M1.3: nov tipno varen NBT<->JSON serializer`

5. **Povej uporabniku v enem odstavku:** kaj je narejeno, kaj je naslednji korak, kaj te blokira.

---

## 4. Kaj se NE sme

- Spremeniti ali prepisati `CustomNPCs_1.12.2-(01Oct19).jar`
- Kopirati originalni JAR v `dev/run/mods` ali `dev/libs` (FML mora najti točno eno instanco moda)
- Spremeniti `environment-lock.json` brez odločitve v `01-ARHITEKTURA.md`
- Spremeniti packet ID-je, vrstni red enumov, mod ID, registry imena ali obstoječe NBT tipe
- Označiti paket kot končan, če testi padajo ali je implementacija delna
- Trditi, da je nekaj popravljeno, brez reprodukcije in testa
- Trditi, da je nekaj hitrejše, brez meritve pred/po
- Pustiti pomembno ugotovitev samo v chatu

---

## 5. Predloga za vnos v dnevnik sej

```markdown
### YYYY-MM-DD — <kratek naslov>

**Paket:** M?.?
**Stanje:** končano / delno / blokirano

**Narejeno:**
- …

**Ni narejeno in zakaj:**
- …

**Ugotovitve:**
- …

**Spremembe obnašanja:** (ali "nobene")
- …

**Meritve:** (ali "nobene")
- …

**Naslednja seja:** …
```

---

## 6. Hitra referenca ukazov

```powershell
# iz korena mape CustomNPC_mod_rework

.\dev.ps1 setupDecompWorkspace --offline   # enkrat, priprava odvisnosti
.\prepare-assets.ps1                       # assets prek HTTPS (potrebuje internet)

.\dev.ps1 testOriginal test --offline      # testi originala IN obnovljene kode
.\dev.ps1 buildPatchedMod --offline        # sestavi lokalni testni mod
.\verify-package.ps1                       # DOKAŽE, kaj se je spremenilo proti originalu

.\dev.ps1 runClient --offline              # razvojni klient
.\dev.ps1 runServer --offline              # dedicated server (prvič potrebuje EULA)
.\dev.ps1 idea workspacePaths --offline    # IDE projekt + izpis classpatha
```

V IDE odpri `dev/build.gradle` in izberi **lokalni `.tools/jdk8`**, ne sistemske Jave.

Opozorila in podrobnosti: [`../OKOLJE.md`](../OKOLJE.md).

---

## 7. Kje kaj je

| Iščeš | Poglej |
|---|---|
| Zakaj je nekaj tako postavljeno | `docs/01-ARHITEKTURA.md` |
| Kaj točno je narobe z X | `docs/02-ZAHTEVE.md` |
| Kaj delam naslednje | `docs/04-STANJE.md`, potem `docs/03-FAZE.md` |
| Kako zbuildam / poženem | `OKOLJE.md` |
| Kateri bugi so že najdeni v originalu | `PLAN_IMPLEMENTACIJE.md` |
| Originalno kodo razreda X | `dev/reference-src/noppes/npcs/…` (samo branje) |
| Kaj se dejansko prevaja | `dev/src/patch/java/` |
| Dokaz, kaj se je spremenilo | `audit/package-verification.txt` po `verify-package.ps1` |
