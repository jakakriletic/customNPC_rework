# 05 — Protokol seje

Projekt teče čez mnogo sej. Vsaka seja začne brez spomina na prejšnjo. Ta protokol poskrbi,
da se ne izgubi kontekst in da se delo ne podvaja.

**Za AI asistenta v novi seji: preberi to do konca, preden se česarkoli lotiš.**

**Avtonomna izmena (uporabnikovo navodilo 20. 10. 2026):** kadar neposredni poziv lokalnega
upravljalnika vsebuje `DIRECT OWNER INSTRUCTION`, ne čakaj na vprašanja ali rutinske
potrditve. Tudi odprte Q odloči po najbolje podprtih dokazih ter zapiši vprašanje, izbrani
odgovor in razlog v projektne evidence in strukturirani `questions`. To uporabnikovo
navodilo ima prednost pred spodnjimi pravili, ki odločitev prelagajo na uporabnika.
Obravnavaj en skladen paket na AI operacijo; naslednjo operacijo in teste zaganja upravljalnik.
Dokazov, resničnih prijav, manjkajočih podatkov ali uspešnih meritev ne izmišljaj. Če je
paket zaradi dejanske zunanje omejitve neizvedljiv, ga z razlogom odloži in izberi neodvisno
delo iz projektnega plana. Podrobnosti: [`overnight/README.md`](../overnight/README.md).

---

## 1. Začetek seje — vedno, brez izjeme

```
1. Preberi  README.md
2. Preberi  docs/04-STANJE.md          ← kje smo (kratek: ~17 KB)
3. Preberi  docs/05-SEJA-PROTOKOL.md   ← ta datoteka
4. Preberi razdelek v docs/03-FAZE.md za paket, ki ga boš delal
5. Preberi razdelek v docs/02-ZAHTEVE.md za pripadajočo zahtevo
6. Preveri git status — ali je kaj nedokončanega iz prejšnje seje
```

**Ne začni kodirati, dokler nisi prebral 1–5.** Vsak paket ima razlog, zakaj je tam kjer je,
in ta razlog ni v kodi.

**Arhiva in registrov ne beri na začetku.** Do 2026-10-19 je bil `04-STANJE.md` ~320 KB
(~80 k tokenov); zdaj je kratek, ostalo pa je dobesedno v:

| Datoteka | Vsebina | Kako do nje |
|---|---|---|
| `docs/arhiv/04-STANJE-dnevnik.md` | dnevnik sej, vsi vnosi razen zadnjih 2 | `grep -n "M5.16" docs/arhiv/04-STANJE-dnevnik.md`, nato `Read` z `offset`/`limit` samo za ta vnos |
| `docs/arhiv/04-STANJE-tabele.md` | polne stare tabele stanja in milestonov, M1/M2 po paketih, **vsa** vprašanja Q, **Znane omejitve** | `grep`; **Znane omejitve** (§5) preglej, preden pišeš nov scenarij ali meritev |
| `docs/04-REGISTRI.md` | živi registri: Sprejete spremembe obnašanja, Prevzeta koda, Meritve | `grep` po stikalu (`Rw…`); odpri, ko dodajaš vrstico |

Pravilo: kar potrebuješ za svoj paket, poišči z `grep` po imenu paketa, stikala ali ključni
besedi, ne bereš celih datotek. Če iščeš dolg vnos, bereš samo njegove vrstice.

### 1a. Nov milestone — najprej raziskava, šele nato paketi (D-029)

Plan v `03-FAZE.md` in `02-ZAHTEVE.md` je delno nastal s šibkejšimi AI modeli. **Ne sledi mu
slepo.** Preden se začne prvi paket novega milestona (ali nadaljuje milestone, ki ima v
`03-FAZE.md` odprt paket **M?.0**), seja izvede paket **M?.0 — raziskava izvedbe**:

1. **Koda in podatki:** preberi dejansko kodo (original v `reference-src`, vanilla/Forge vire),
   vse obstoječe meritve in profile za to področje. Kaj je **izmerjeno**, kaj samo **ocenjeno**?
2. **Zunanji viri (deep research):** kako so isti problem rešili drugi (CustomNPC+, drugi modi,
   Paper/Sponge/Spigot, Forge, literatura, issue trackerji). Navedi vire s povezavami.
3. **Kritika plana:** za vsak paket milestona zapiši *ohrani / spremeni / zavrzi / dodaj* in zakaj.
   Preveri tudi premise in vrstni red, ne samo izvedbo. Napačna premisa je najdražja napaka
   (primera: D-020, M5-S P1).
4. **Zapis:** `docs/raziskave/<milestone>-<tema>.md` z oznakami **[K]** (iz kode), **[M]** (izmerjeno),
   **[H]** (hipoteza), **[W]** (zunanji vir).
5. **Prilagoditev dokumentacije:** posodobi `03-FAZE.md` (paketi, vrstni red, izhodni kriterij),
   `02-ZAHTEVE.md`, povzetek v `README.md` in po potrebi nova odločitev D-0NN v `01-ARHITEKTURA.md`.
   Kar spremeni **obseg ali cilj** zahteve, ne odloči seja sama: zapiše se kot vprašanje
   uporabniku (Q) v `04-STANJE.md`.

Raziskava je **samostojen paket** (ena seja) in ne piše produkcijske kode. Merilni poskus, ki
odloči med dvema možnostma, je dovoljen. Ista pravila veljajo, kadar se med milestonom izkaže,
da je premisa paketa napačna: ustavi se, zapiši, prilagodi plan, šele nato nadaljuj.

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
- **Brez nepotrebnih ponovitev (Q90, stalno pravilo lastnika, 29. 10. 2026):** če se
  produkcijska koda in testni scenariji od uspešne serije niso spremenili, se ta serija ne
  ponavlja. Za sprejem paketa se zahtevajo samo testi, ki dokazujejo **trenutno** kodo
  (enakost, cena in nujne regresije). Testi zavrnjenih prejšnjih poskusov, dodatne ponovitve
  »za vsak slučaj« in ponovna validacija zaradi sprememb upravljalnika (`overnight/`) ali
  novega testnega ID-ja niso potrebni. Upravljalnik to uveljavlja sam: uspešnih testov na
  istem odtisu ne požene znova, odtis pa ne vključuje več datotek `overnight/` in kataloga ID-jev.

---

## 3. Zaključek seje — obvezno

> **Q92 (29. 10. 2026):** obseg v1.0, cenovna meja samo za privzeto vklopljena stikala,
> največ 2 poskusa na paket in **kratka dokumentacija** so v
> [`10-OBSEG-V1.md`](10-OBSEG-V1.md) in imajo prednost pred podrobnostmi spodaj. Na paket:
> en zapis meritve + en kratek vnos v `04-STANJE.md`; ostale datoteke samo ob zaključku paketa.

Preden seja konča, **vedno**:

1. **Posodobi `docs/04-STANJE.md`** (ostane kratek, cilj ≤ ~25 KB):
   - tabela "Trenutno stanje": celica "Zadnja posodobitev" se **prepiše** (ne dopolnjuje): samo
     ta seja, 2–3 stavki. Povzetkov starejših sej tja ne kopiraj — so v dnevniku
   - "Napredek po milestonih": vrstica milestona, ki se je premaknil, z **eno kratko opombo**;
     podrobnosti po paketih sodijo v dnevnik in v zapis v `docs/meritve/`
   - **nov vnos v "Dnevnik sej" gre na vrh** razdelka. Nato je v tej datoteki več kot 2 vnosa:
     **najstarejši vnos prestavi dobesedno** na vrh `docs/arhiv/04-STANJE-dnevnik.md` (pod uvod,
     nad prejšnje vnose). V `04-STANJE.md` ostaneta vedno **zadnja 2 vnosa**. Pri prestavitvi
     popravi relativne povezave: `](meritve/…)` → `](../meritve/…)`, `](../OKOLJE.md)` →
     `](../../OKOLJE.md)` (arhiv je v podmapi)
   - nova odprta vprašanja za uporabnika → tabela "Odprta vprašanja" v `04-STANJE.md` (naslednja
     prosta številka Q) **in** ista vrstica v `arhiv/04-STANJE-tabele.md` §4. Odločeno vprašanje
     se v `04-STANJE.md` **izbriše**, v §4 arhiva pa posodobi stanje
   - nov odprt pojav (hipoteza) → "Odprti pojavi"; ko je razrešen, vrstico izbriši in rezultat
     zapiši v dnevnik
   - nova pasta ali pravilo merjenja → na konec **Znane omejitve**, `arhiv/04-STANJE-tabele.md` §5
   - **v `docs/04-REGISTRI.md`** (ne v `04-STANJE.md`):
     - vsaka sprejeta sprememba obnašanja → tabela "Sprejete spremembe obnašanja"
     - vsaka prevzeta koda → tabela "Prevzeta koda"
     - vsaka meritev → tabela "Meritve" (+ polni zapis v `docs/meritve/`)
   - števci v "Trenutno stanje" (razredi, testi), če so se spremenili

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

Vnos gre na vrh razdelka "Dnevnik sej" v `docs/04-STANJE.md` (glej §3). Naj bo kratek: dolge
tabele in surove številke sodijo v zapis v `docs/meritve/`, v vnosu pa je povzetek in povezava.

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
| Starejši dnevnik sej, vsa vprašanja Q, znane omejitve | `docs/arhiv/` (`grep`, glej §1) |
| Stikala (spremembe obnašanja), prevzeta koda, kazalo meritev | `docs/04-REGISTRI.md` |
| Kako zbuildam / poženem | `OKOLJE.md` |
| Kateri bugi so že najdeni v originalu | `PLAN_IMPLEMENTACIJE.md` |
| Originalno kodo razreda X | `dev/reference-src/noppes/npcs/…` (samo branje) |
| Kaj se dejansko prevaja | `dev/src/patch/java/` |
| Dokaz, kaj se je spremenilo | `audit/package-verification.txt` po `verify-package.ps1` |
