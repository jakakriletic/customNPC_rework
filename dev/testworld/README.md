# dev/testworld — seme ponovljivega testnega sveta (M0.6)

Ta mapa je **vir resnice** za testni svet. Sam svet v `dev/run/world` je izpeljan in
ga git ne sledi; to mapo pa sledi, zato lahko katerakoli seja postavi isti svet znova.

## Vsebina

| Pot | Kaj je |
|---|---|
| `server.properties` | determinističen svet: superflat, fiksni seed, brez pošasti, brez vremena |
| `customnpcs/clones/1/*.json` | fixture NPC-ji, ki se spawnajo z `noppes clone spawn` |
| `setup-commands.txt` | konzolni ukazi, ki postavijo prizorišče in NPC-je |

## Namestitev

```powershell
.\testworld.ps1            # arhivira star svet in namesti seme
.\dev.ps1 runServer --offline
# nato prilepi vsebino dev/testworld/setup-commands.txt v server konzolo
```

Postopek in merila: [`../../docs/scenariji/M0.6-testni-svet.md`](../../docs/scenariji/M0.6-testni-svet.md)

## Fixture NPC-ji

| Ime | Namen | `MovementType` | `MovingState` | `Role` |
|---|---|---|---|---|
| `T_Stand` | mirujoča referenca, brez AI šuma | 0 tla | 0 stoji | 0 |
| `T_Wander` | obremenitev pathfindinga | 0 tla | 1 tava | 0 |
| `T_Flyer` | R2 letenje, mora čez steno na x=20 | 1 leti | 1 tava | 0 |
| `T_Carrier` | R1 nosilec | 0 tla | 1 tava | 0 |
| `T_Rider` | R1 jahač | 0 tla | 0 stoji | 0 |
| `T_Follower` | R9 follower vloga | 0 tla | 0 stoji | 2 follower |
| `T_Trader` | trader vloga, zapis market podatkov | 0 tla | 0 stoji | 1 trader |
| `T_Scripted` | R7 in M5: NPC z aktivno skripto | 0 tla | 0 stoji | 0 |

`T_Scripted` ima `ScriptEnabled` in `ScriptLanguage = ECMAScript`, skripta pa ob `init`
požene `/say TW-SCRIPT-OK`, ob 200. `tick` klicu pa `/say TW-SCRIPT-TICK-200`. Oboje gre v
server konzolo tudi brez prijavljenega igralca, zato je izvajanje skript preverljivo iz loga
in ne le iz odsotnosti napake. Zahteva vklopljene command bloke; seme jih ima.

Vsi so `Invulnerable` in `PersistenceRequired`, da test meri to, kar naj bi meril,
in ne naključne smrti. Vsi imajo tag `testworld`, zato jih je mogoče izbrati z
`@e[tag=testworld]` in pobrisati z enim ukazom.

## Pravila

- Fixture datoteke so **generirane iz pravega NPC NBT zapisa**, ne napisane na pamet.
  Če se save format kdaj spremeni, se regenerirajo iz sveta, ne popravljajo ročno.
- Spremembo fixture NPC-ja vedno spremlja vnos v `docs/04-STANJE.md`; meritve pred
  spremembo in po njej niso primerljive.
- Quest in dialog fixture nastaneta **avtomatsko**: `.\fixture-run.ps1` pozene server, krmilnik
  `TW_Control` pa ju ustvari prek scripting API-ja moda (`IQuestCategory.create()` /
  `IDialogCategory.create()`), mod pa ju shrani po svoji poti. Rezultat pride v
  `customnpcs/quests/TW/` in `customnpcs/dialogs/TW/`, dialog pa je pripet na `T_Trader`.
  Postopek in omejitve: [`../../docs/scenariji/M0.7-integracijska-matrika.md`](../../docs/scenariji/M0.7-integracijska-matrika.md) §4.

## Krmilnik TW_Control

`tw-fixture.js` je **vir resnice**. V `customnpcs/clones/1/TW_Control.json` ga vloži isto
orodje kot pri R1:

```
python3 dev/testworld/vstavi-skripto.py dev/testworld/tw-fixture.js dev/testworld/customnpcs/clones/1/TW_Control.json
```

Vložena različica gre v commit; `fixture-run.ps1` samo preveri, da je tam, in datoteko
prekopira v svet. **Nobene druge minifikacije ne uvajamo** — dve vzporedni implementaciji sta
17. 9. povzročili `script errored` (komentar na koncu vrstice je po strnitvi pojedel pol skripte).

V skriptah so dovoljeni **samo enojni narekovaji**, Nashorn na Javi 8 pa ne pozna `let`,
puščičnih funkcij in šablonskih nizov.

## Fixture M2.4 (merilne obremenitve)

`PERF_Idle`, `PERF_BojA`, `PERF_BojB`, `PERF_Skripte` in `PERF_Kontrola` niso del testnega
sveta; `.\perf-run.ps1` jih skopira v svet sam. Naredi jih `perf-fixture.py` iz `T_Stand` in
`T_Scripted` (ročno urejanje ni dovoljeno). `PERF_Boj*` sta za razliko od predlog ranljiva
(`Invulnerable 0b`, 100000 HP), sicer boj ne bi delil škode. Scenarij:
[`../../docs/scenariji/M2.4-obremenitve.md`](../../docs/scenariji/M2.4-obremenitve.md).
