# Q459 — odstranitev skupine Linked NPC izbriše njeno datoteko (2026-11-16)

**Paket:** omejitev §6 po Q407/Q454 točka 2 (originalni bug Q393) · **Scenarij:** `linked-remove-run.ps1`
(`dev/testworld/linked-remove-control.js`) · **Stikalo:** `RwLinkedGroups`, privzeto 0.

## Vzrok [K]

`LinkedNpcController.removeData` skupino odstrani iz seznama in pokliče `save()`, ki zapiše samo obstoječe skupine.
Datoteka `<skupina>.json` ostane v `customnpcs/linkednpcs`, `load()` jo ob naslednjem zagonu prebere in skupina se vrne.

## Popravek

`removeData(name)` kliče `removeData(name, RwLinkedGroups == 1)`. Pri 1 se po odstranitvi izbrišeta `<skupina>.json`
in `<skupina>.json_prev` (neuspeh izbrisa gre v log); pri 0 ostane originalna pot. Brez novega stikala, ker `.json_prev`
že pripada `RwLinkedGroups`. Info besedilo stikala dopolnjeno.

## Zagoni (controller, odtis `d5ffbe14…`, vsi PASS)

| Test | Poročilo | Izid |
|---|---|---|
| `unit` | — | PASS (`LinkedRemoveTest` 4 testi; izključen iz `testOriginal`) |
| `build`, `package` | — | PASS |
| `q459-remove-v1` | `audit/q459-remove-2026-11-16-130054` | 47 provera harnessa + 17 provera skripte, vse OK |

Štirje zagoni enega izoliranega sveta (dev dedicated, prisilni chunki Q308):
1. **seed (0):** odstranjena skupina izgine iz pomnilnika, datoteka ostane — Q393 reproduciran.
2. **offafter (0):** po restartu se odstranjena skupina vrne iz datoteke (original).
3. **on (1):** izbrisana `.json` in `.json_prev`; druga skupina bitno nespremenjena.
4. **onafter (1):** po restartu skupine ni, datoteki ostaneta odsotni, ostali skupini naloženi.

Prvi `unit` (odtis `d51aa520…`) je padel, ker `testOriginal` novega testa ni izključil; popravljen samo `build.gradle`.

## Meje

- Samo strežnik; odstranitev je klicana iz skripte, ne iz GUI Global → Linked NPCs (ta kliče isto metodo).
- En zagon, dev dedicated brez igralca. Cena ni merjena (ne teče na tick).
