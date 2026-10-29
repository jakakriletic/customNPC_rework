# 10 — Obseg v1.0 in pravila hitrejšega razvoja (Q92)

**Odločitev lastnika, 29. 10. 2026.** Ta dokument ima prednost pred vrstnim redom in obsegom
v `03-FAZE.md`. Paketi, ki niso na seznamu v1.0, se **ne izvajajo** do izdaje v1.0. Ne
brišemo jih, ostanejo v `03-FAZE.md` kot plan za v2.

**Razlog:** pri dosedanjem tempu M4 (~25 ur na paket) je ostalih ~70 paketov pomenilo
500–1500 ur dela. Cilj je zaključen, izdan rework v ~100 urah dela upravljalnika.

## Obseg v1.0 (vrstni red)

| # | Paket | Opomba |
|---|---|---|
| 1 | ~~M4.2~~ | `FlightMode` + NBT zaključen 29. 10. (D-086); unit/build/package in M4.16 180/180 zeleni |
| 2 | ~~M4.3~~ | `HoverMoveHelper` zaključen 29. 10. (D-087); privzeto 180/180 = original, H0–H4 z restartom zeleni, cena enkrat kot podatek |
| 3 | M4.15a → M4.15b → M4.15c | izbira navigacijskega ozadja v GUI |
| 4 | M3.8b | GUI za `RwHitboxMode` |
| 5 | M6.1 → M6.10, M6.12 | Java scripting; M6.6 samo, če je potreben za pravilnost |
| 6 | M6.11 | samo najmanjši nabor (player, world); ostalo v v2 |
| 7 | ostanek M7 | M7.5 hooki, M7.8 reload, M7.9 script API, uskladitev verzije CNPC (D-024) |
| 8 | M10.1, M10.2, M10.4–M10.7 | soak M10.4 skrajšan na 4 h; M10.3 ostane blokiran (Q1) |

## Prestavljeno v v2 (po izdaji v1.0)

M3.10, M4.5, M4.6, M4.7, M4.8, M4.11, M4.13, M4.14c, M4.14d, M5.10, M7.11, M9 (chatbot),
M11 (poveljevanje prek zemljevida), M12 (skini prek URL), M13 (več NPC-jev hkrati),
M14 (instanciran izris). Odprti Q-ji teh paketov (npr. Q82 vogal → M4.5, Q84) ne blokirajo v1.0.

## Pravila hitrejšega razvoja

1. **Brez ponavljanja uspešnih serij (Q90).** Glej `05-SEJA-PROTOKOL.md` §2 Meritve.
2. **Funkcija pod stikalom, privzeto izklopljena (0), nima cenovne meje.** Za sprejem
   zadostujejo unit, build, package, dokaz enakosti originalu pri stikalu 0 in en funkcionalni
   test, ki pokaže, da funkcija pri 1 deluje. Cena pri 1 se izmeri **enkrat** (ena ponovitev)
   in zapiše kot podatek, ne kot pogoj. Cenovna meja (+10 % p95) velja šele za paket, ki
   stikalo privzeto vklopi (D-032), in takrat s tremi ponovitvami.
3. **Največ 2 poskusa na paket.** Če drugi poskus ne izpolni meril, se paket sprejme pod
   stikalom (privzeto 0) z zapisanimi omejitvami, ali pa se odloži v v2. Tretjega poskusa ni.
4. **Kratka dokumentacija.** Na paket: en zapis v `docs/meritve/` (rezultat, številke, odločitev)
   in en kratek vnos v `04-STANJE.md` (dnevnik + vrstici »Zadnja posodobitev« in »Naslednji
   paketi«). D-zapis v `01-ARHITEKTURA.md` samo za novo arhitekturno odločitev, največ 10 vrstic.
   `04-REGISTRI.md`, `03-FAZE.md` in README samo ob zaključku paketa, ena vrstica. Brez vmesnih
   »nadaljevanje seje« razdelkov, ki ponavljajo stanje.

## Ocena

~29 paketov (okoli 12 S, 15 M, 2 L). Ob pravilih zgoraj: S ~1,5 h, M ~3–4 h, L ~8 h dela
upravljalnika, z dodatkom ~30 % za popravke: **okoli 90–120 ur**, to je ~5–7 dni neprekinjenega
dela ali ~1,5–2 tedna samo nočnega dela. Ocena je projekcija iz tempa M5 (21.–22. 10.), ne meritev.
