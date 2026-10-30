# P6 — GUI za `RwFlightMode` (30. 10. 2026)

**Stanje:** sprejet, funkcionalni poskus 1 (Q269–Q271); privzeto Original (0).

## Kaj je narejeno (Q269)

- `rework.movement.FlightGui`: pogodba žice in stanje urejevalnika. Strežnik v odgovor
  `MainmenuAIGet` (`NavGuiData.reply`) doda `RwFlightGui` = shranjeni način; `NavGuiData.save`
  sprejme samo int 0 (Original) ali 1 (Hover). Klient pošlje ključ samo, če je uporabnik
  izbiro spremenil, zato star klient ali nedotaknjen urejevalnik načina ne spremeni.
- `SubGuiNpcMovement`: gumb (id 17) v prvi vrstici desno (68 px), samo pri `movementType=1`
  in po odgovoru strežnika. Napisi `Fly: Orig` / `Fly: Hover`; shranjen Momentum (v2, samo NBT)
  je prikazan kot onemogočen `Fly: Mom.`. Tekstura `menubg.png` ima ploščo visoko 216 px,
  zato nova vrstica ni mogoča. Klik tipa gibanja zdaj ponovno nariše urejevalnik.
- Brez novega paketa, NBT ključa entitete ali tick kode; način velja kot doslej (M4.3:
  `HoverMoveHelper` samo pri letečem NPC-ju, sprememba ob naslednjem `updateTasks`).

## Test `p6-gui-v1` (Q270)

`flight-gui-run.ps1` + `FlightGuiHarness`: svež izoliran svet, trije NPC iz kopije `LET_Walker`
(`FLGUI_0` leteč/Original, `FLGUI_1` kopenski, `FLGUI_2` leteč z `RwFlightMode: 2`).
Pravi wand, zavihek AI, Movement, kliki z miško, zapiranje prek menija (pravi `MainmenuAISave`).

| Krog | NPC | Dejanje | Pričakovano na strežniku |
|---|---|---|---|
| r0 | 0 | Orig → Hover | način 1, NBT ključ 1, `HoverMoveHelper` |
| r1 | 0 | brez spremembe | ostane 1 |
| r2 | 0 | Hover → Orig | 0, ključ odsoten, `FlyingMoveHelper` |
| r3 | 1 | ground → flying (gumb se pokaže), Orig → Hover | 1, leteč, Hover |
| r4 | 2 | Momentum onemogočen | ostane 2 |
| r5 | 0 | Orig → Hover, nato flying → ground (gumb izgine) | 1 shranjen, ne velja (kopenski) |

Nato `noppes clone add/spawn` treh NPC (nov UUID, isti način/gibanje) in drugi klientov JVM:
vseh 6 entitet odpre urejevalnik, shrani brez spremembe, način in UUID ostaneta ista.
Vsak prikazan gumb: vrednost, napis, omogočenost, meje plošče/zaslona, brez prekrivanja; PNG.

## Rezultati

Controller na `81e79f300c36e2f8917c4e226aba738fe21a42bcd1a2efade9fdbcc11f270e0b`:

| Test | Rezultat | Operacija |
|---|---|---|
| unit (`testOriginal`, `test`) | PASS; 567 metod `@Test` v 88 datotekah, FlightGuiTest 7 | `c8c3fd7d-ad25-4072-b4ca-7755f3df22e0` |
| build | PASS; 370 razredov v workspace jarju | `b0d020f2-163e-4462-a4d5-f793f633ec5d` |
| package | PASS; 1.716 originalnih vnosov, 85 pričakovanih razlik (84 razredov + manifest) | `9f9d3134-071b-48a9-b707-1d7cd194c969` |
| `p6-gui-v1` | PASS; edit 79/79 + verify 58/58, skupaj 137/137 | `289a08fe-d62c-4c26-a9f7-178d72d4bc78` |

Dokazi: `audit/p6-2026-10-30-224324.json`, `-edit.json`, `-verify.json` in pripadajoča
`-edit-client.log`/`-verify-client.log`. Vseh šest krogov iz tabele zgoraj uspe; strežniški
NBT in dejanski helper ustrezata izbiri. Original ne zapiše ključa, Hover velja le pri letenju,
Momentum ostane 2 z originalnim helperjem. Kloni imajo nove UUID-je; po drugem JVM vseh šest
UUID-jev in načinov ostane istih. Configa, server.properties, eula in options nespremenjeni
(CNPC config SHA256 `BDCB03455F21CDFB3DF95071ADA8E36812B53B11F44D23529265D5E10EA0DD7D`).

Pregledanih vseh 10 PNG `audit/screenshots/p6-2026-10-30-224324-*`: plošča in obstoječi
elementi ostanejo v zaslonu. **Omejitev vizualnega dokaza:** tutorial toast za premikanje
prekriva gumb v prvi vrstici; r3 zajame še prejšnji izris pred ground→flying. PNG zato ne
dokazujejo berljivosti napisa novega gumba. Njegov napis, vrednost, omogočenost, pojav/izginotje,
meje in razmik do soseda preverjajo GUI trditve; dejanski kliki in strežniški rezultat uspejo.
To ni poln vizualni sprejem niti dokaz packaged/dedicated klienta. Po Q90 ni ponovitve samo
za lepše PNG (Q271); produkcija in harness po PASS ostaneta ista. Fizika letenja in cena se
ne merita znova: ni spremembe tick kode ali privzetega načina (Q270).

Predhodni unit na `17ec7090…` je padel samo v `testOriginal`: novi FlightGuiTest je bil
naložen proti originalnemu jarju brez novega API-ja. Izključen iz `testOriginal` ob obstoječem
FlightModeTest; v `test` ostane vseh 7 preverjanj in uspe. Pogoji GUI scenarija niso spremenjeni.
