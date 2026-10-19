# M5-K K4 — napisi NPC-jev iz display lista (19. 10. 2026)

Paket K4 v [03-FAZE](../03-FAZE.md) (Q28: za K14b). Profil [M5-K.1](2026-10-18-M5-K.1-baseline-profil.md):
ime + naslov 20,5 % render niti pri 174 napisih v dometu (idle-200), ~8 µs na napis.

## 1. Koda

`FontRenderer.drawString` nariše vsak znak v takojšnjem načinu (`glBegin`, 4 × tekstura + oglišče,
`glEnd`) **[K]**. Za isti niz, barvo in nastavitve pisave (Unicode, bidi) so ukazi vedno isti (položaj
`x = −širina/2` je v ogliščih). `rework/render/LabelCache` ob **drugem** izrisu niza ukaze posname v display
list (`GL_COMPILE_AND_EXECUTE`) in jih nato predvaja z enim klicem. Display list hrani vrednosti ukazov,
zato gonilnik dobi iste ukaze z istimi vrednostmi **[K, GL 1.x]**.

- `GlStateManager` ukaz izpusti, kadar se predpomnjeno stanje ne spremeni. Pred snemanjem se zato
  predpomnilnik alfe, barve in teksture postavi tako, da se ukazi niza (`enableAlpha`, `color`, `bindTexture`)
  res posnamejo; po predvajanju se uskladi s končnim stanjem lista (alfa vklopljena, barva neznana, vezana
  zadnja tekstura niza, prebrana iz `GlStateManager` ob snemanju).
- Original za: prvi izris niza (naloži teksture znakov; `glTexImage2D` ne sme v list), podrazrede
  `FontRenderer` (modi pisav), OptiFine, nize s `§k` (naključni znaki), `§n`, `§m` (črta brez teksture).
  Ob ponovnem nalaganju virov se listi pobrišejo; LRU 1.024 nizov.
- 1:1 prenos `RenderNPCInterface` (commit 0abb22e; bajtkoda 26 metod: 18 enakih, 8 razlik v obliki, 0 za
  pregled), nato trije klici `drawString` v `renderLivingLabel` → `rwDrawLabel`. Stikalo **`RwLabelCache`**.

## 2. Preverba slike (`LabelCheck`, `k-run -Napisi 1 -PreveriNapise`, merilo K11)

V eni sličici se 200 NPC-jev idle-200 (174 v dometu imen) nariše v lasten framebuffer: original → list →
original; primerja se barva in globina vseh pikslov.

| serija | pikslov | izrisov z listom / po originalu | razlik barva / globina | ponovitev |
|---|---|---|---|---|
| ime in naslov testnega sveta | 37.478 | 348 / 0 | 0 / 0 | 0 |
| barvne kode, krepko, ležeče, šumniki (stran Unicode) | 37.971 | 348 / 0 | 0 / 0 | 0 |
| ime s `§k` (naslov brez) | 37.115 | 174 / 174 | (naključni znaki, se razlikuje že original proti originalu) | — |

Drug naslov spremeni 2.817 pikslov (preverba je občutljiva). Napis bližje kot 4 bloke (drugi prehod brez
globinskega testa, barva `+0x55000000`) gre po isti poti z drugo barvo; v prizorišču ga ni.

## 3. A/B (3 + 3 ponovitve, `audit/m5k4-ab.ps1`; mediana, razpon v oklepaju)

| celica | p50 original | p50 list | p95 original | p95 list | FPS |
|---|---|---|---|---|---|
| idle-200 | 6,556 (6,444–6,598) | **6,100 (6,029–6,128), −7,0 %** | 7,071 | **6,638, −6,1 %** | 150,7 → 161,5 |
| idle-50 | 2,164 (2,136–2,167) | **2,025 (2,013–2,044), −6,4 %** | 2,412 | **2,229, −7,6 %** | 457,0 → 488,5 |

Razponi se ne prekrivajo. Dobitek je ~⅓ stroška napisov (20 % → ~13 % sličice): predvajanje lista z
~34 znaki v takojšnjem načinu še vedno stane v gonilniku, prihranek je v ~340 klicih JNI na napis.

**Odločitev:** enakost po kodi + 0 razlik pikslov, dobitek nad šumom → **`RwLabelCache` privzeto 1** (D-032).
Dev `CustomNpcs.cfg` drži 0, zato baseline in scenariji merijo original.
