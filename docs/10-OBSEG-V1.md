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
| 3 | M4.15a (obravnavan z omejitvami) → ~~M4.15b~~ → ~~M4.15c~~ | GUI/pogodba in klient/restart zaključeni (D-089, Q122); 70/70 ×2 + 68/68, 30 PNG pregledanih |
| 4 | ~~M3.8b~~ | GUI za `RwHitboxMode` zaključen 29. 10. (Q126), 216/216, clone/restart in 18 PNG potrjeni; naslednji M6, najprej raziskava M6.0 (D-029) |
| 5 | ~~M6.1~~, ~~M6.2~~, ~~M6.3~~, ~~M6.4~~, ~~M6.5~~, ~~M6.6~~, ~~M6.7~~, ~~M6.8~~, ~~M6.9~~, ~~M6.10~~ → ~~M6.12~~ | Java scripting; M6.0 raziskava (D-090) ter M6.1–M6.10 in M6.12 zaključeni; [M6.12 izbira jezika](meritve/2026-10-29-M6.12-izbira-jezika.md) (Q187): GUI JS↔Java, clone/restart, brez compilerja jasna napaka, 101 trditev PASS na `48c60f8e…`; modpack classpath ročni (Q184) |
| 6 | ~~M6.11~~ | samo najmanjši nabor (player, world); ostalo v v2. Zaključen 29. 10. (Q191): [player/world](meritve/2026-10-29-M6.11-player-world.md) m611 12/12 PASS na `e3e30ca1…`; naslednji ostanek M7 |
| 7 | ~~M7.0~~ → ~~M7.V~~ → ~~M7.9~~ → ~~M7.5~~ | [raziskava M7.0](raziskave/M7.0-animacije-integracija.md) zaključena 29. 10. (D-095, Q192–Q197): uskladitev verzije CNPC (M7.V, 01Oct19, [zaključen](meritve/2026-10-29-M7.V-entities-zagon.md) Q199), script API `Animators.of(npc)` ([zaključen](meritve/2026-10-29-M7.9-animator-api.md) Q202), hooki start/end/event ([zaključen](meritve/2026-10-29-M7.5-animacijski-hooki.md) Q206); M7 v1 zaključen, naslednji M10; M7.8 reload in `blendTo` v2 |
| 8 | ~~M10.0~~ → ~~M10.1a~~ → ~~M10.1b~~ → ~~M10.1c~~ → ~~M10.1d~~ → ~~M10.5~~ → ~~M10.4~~ → ~~M10.2~~ → ~~M10.6~~ → M10.7 | D-096/Q92: soak 4 h; M10.3 blokiran (Q1). M10.1a/b zaprta brez sprejema (Q220/Q223); [M10.1c](meritve/2026-10-29-M10.1c-matrika.md) primerjalno sprejet, O/K 13/13 + 9/9, IN3/IA3/IS3/IL5/IL6 delni (Q225). [M10.1d](meritve/2026-10-30-M10.1d-klient.md) zaprt brez sprejema po 2 poskusih, vse 4 celice v2 FAIL, nadaljnja validacija v2 (Q233); release ni sprejet. [M10.5](meritve/2026-10-30-M10.5-rollback.md) sprejet na poskusu 2, 58/58 in obnova 42/42 datotek (Q236). [M10.4](meritve/2026-10-30-M10.4-soak.md) zaprt brez sprejema po 2 poskusih, soak 2 h 25 min do izgube NPC zaradi diag meje chunkov, polni soak v2 (Q240). [M10.2](meritve/2026-10-30-M10.2-baseline.md) sprejet, 9/9 celic brez alarma p95 proti 5. 10. (Q242). [M10.6](meritve/2026-10-30-M10.6-dokumentacija.md) uporabniška dokumentacija [`UPORABNIKI.md`](UPORABNIKI.md) zaključena (Q244). Naslednji M10.7 |

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
