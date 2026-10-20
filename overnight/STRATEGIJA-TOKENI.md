# Strategija porabe tokenov (OPS.3 predlog)

Stanje 20. 10. 2026, po pregledu OPS.1/OPS.2. Cilj: čim več opravljenega dela na
naročniško kvoto treh profilov (`claude-1`, `claude-2`, `codex`), brez zmanjšanja
dokazne strogosti (sprejemne teste še vedno poganja upravljalnik).

Oznake: **[K]** ugotovljeno iz kode, **[H]** hipoteza, ki jo potrdi šele meritev porabe.

## 0. Najprej meriti

**[K]** Claude `--output-format json` vrne `usage` (input/output/cache tokeni), `num_turns`
in `total_cost_usd`; Codex JSONL vrne `usage` v `turn.completed`. `parseAgent` to zavrže,
prav tako `session` id. Brez teh številk je vsaka optimizacija ugibanje.

- Za vsako AI operacijo shrani `{profil, model, faza, tokeni in/out/cache, turnov, trajanje, akcija}`
  v `state.json` in pokaži vsoto na paket v frontendu.
- Merilo uspeha strategije: **tokeni na zaključen paket** in **AI iteracije na paket**,
  ne tokeni na klic.

## 1. Manj AI iteracij (največji vzvod)

Vsaka iteracija je nova seja, ki znova prebere AGENTS/README/STANJE/protokol (~50 KB,
~13 k tokenov) in nato še kodo, ki jo je prejšnja seja že poznala.

1. **Hitre preverbe sme AI pognati sam.** **[K]** Poziv trenutno prepoveduje vse builde in
   teste, zato vsaka napaka prevajanja stane celo iteracijo (AI → controller build → AI).
   Med AI klicem meritev nikoli ne teče (controller ju serializira), `org.gradle.daemon=false`
   pa pomeni, da za AI ne ostane Java proces, ki bi blokiral desktop guard. Dovoli:
   `dev.ps1 compileJava --offline` in `dev.ps1 test --offline`. Minecraft, `k-run`,
   `perf-run` in paketiranje ostanejo izključno controllerjevi; za `done` še vedno
   štejejo samo controllerjevi rezultati.
2. **Nadaljuj isto sejo znotraj paketa.** **[K]** CLI to podpirata (`claude -p --resume <id>`,
   `codex exec resume <id>`). Ko isti profil nadaljuje isti paket, se prompt cache in že
   prebrane datoteke ohranijo. Pravila:
   - samo isti profil + isti paket; ob failoverju nova seja s predajo;
   - nova seja, ko seja preseže ~40 % konteksta (po `usage`) ali po 4 nadaljevanjih
     (enako pravilo kot ročne seje);
   - po timeoutu (1 h) nadaljuj z `--resume`, namesto da se izgubi vse razen predaje.
3. **Vsi potrebni testi v enem zahtevku.** V poziv: "request every test the package needs in
   one action=test, ordered cheapest first". Shema to že podpira (`testIds` je seznam).

## 2. Manjši kontekst na klic

1. **[K]** "Other deferred/completed packages" vstavi do 12 nalog s polno predajo
   (do 16 000 znakov vsaka), kar s časom naraste do ~50 k tokenov na klic. Zadošča
   `title`, `status` in `summary` ≤ 300 znakov; polna predaja samo za trenutni paket.
2. **Strukturirana predaja ≤ 3000 znakov** z obveznimi razdelki: *Stanje*, *Spremenjene
   datoteke*, *Naslednji korak*, *Odprta tveganja*, *Kje so dokazi*. Dobra predaja
   nadomesti raziskovanje repozitorija v naslednji seji.
3. **Branje ob začetku:** na prvi iteraciji paketa po protokolu; na nadaljevanjih samo
   predaja + `git diff --stat`. Za `03-FAZE.md` (62 KB) in `02-ZAHTEVE.md` (56 KB) vedno
   `grep` po paketu, nikoli celih datotek.
4. **Logi:** v poziv dodaj, da se testni logi berejo z `Select-String`/zadnjimi vrsticami,
   ne celi (Minecraft logi so lahko več MB).

## 3. Model in effort glede na vrsto dela

Dodaj `model`/`effort` po fazi (v konfiguraciji, ne v kodi):

| Vrsta operacije | Prepoznava | Claude | Codex |
|---|---|---|---|
| Raziskava M?.0, nova arhitektura, razhroščevanje padle meritve | nov milestone, ali isti test pade 2× | najmočnejši model, `--effort high` | `-c model_reasoning_effort=high` |
| Običajna implementacija paketa | privzeto | privzeti model, `--effort medium` | `medium` |
| Zaključek: vsi sveži testi zeleni, ostane analiza + dokumentacija + commit | vsi `requiredTests` uspešni na trenutnem fingerprintu | manjši model (Sonnet), `--effort low` | `low` |
| Ponovni pregled plana po `idle` | naloga iz `waiting` | manjši model, `low` | `low` |

**[H]** Zaključni in idle klici so pogosti in rutinski; tam je prihranek največji brez
tveganja za kakovost. Potrditev s točko 0.

## 4. Kroženje računov

**[K]** Trenutni vrstni red je "izčrpaj prvega, nato naslednjega". Za 5-urna okna je to
dobro: medtem ko dela `claude-2`, se okno `claude-1` resetira.

1. **Lepljivost na paket:** profila ne menjaj sredi paketa, razen ob kvoti/avtentikaciji
   (ohrani cache, točka 1.2). To je že res, ostane naj tako.
2. **Codex kot drug ponudnik** ne deli cachea s Claude. Uporabi ga za nove pakete ali kot
   neodvisen pregled pred `done` pri velikih spremembah (ena kratka operacija), ne kot
   nadaljevanje Claude seje sredi razhroščevanja.
3. **Tedenske omejitve:** ko ima profil zabeleženih več zaporednih kvot v istem tednu, ga
   prestavi na konec vrstnega reda do tedenskega reseta, da se tedenske kvote porabijo
   enakomerno.
4. **Mrtvi čas:** ko so vsi profili v cooldownu, naj controller poganja čakajoče testne
   faze (merjenje ne porabi tokenov). **[K]** Zdaj `tick` obravnava samo prvo nalogo v vrsti.

## 5. Varovala proti zapravljanju

1. **Zanka padlega testa.** **[K]** V avtonomnem načinu ni meje iteracij, števec `failures`
   pa šteje le napake AI klica, ne padlih testov. Paket, ki vedno znova pade na istem
   testu, lahko kuri kvoto vso noč. Predlog: ko 3 zaporedne iteracije ne prinesejo
   novega uspešnega testa, ena iteracija z najmočnejšim modelom; če ni napredka, paket
   odloži z razlogom in nadaljuj neodvisno delo.
2. **Idle prebujanje.** **[K]** `idle` vsako uro sproži poln AI klic, ki praviloma ugotovi isto.
   Bolje: prebudi ob spremembi fingerprinta ali novem cilju, sicer eksponentno
   (1 h, 2 h, 4 h, največ 8 h).
3. **Proračun na paket:** ko vsota tokenov paketa preseže prag (nastavljiv, npr. 3× mediana
   zaključenih paketov), naslednja iteracija mora vrniti strukturiran plan in oceno,
   preden nadaljuje.

## Vrstni red uvedbe

1. Merjenje porabe (0). Brez tega ne vemo, kaj je delovalo.
2. Varovala 5.1 in 5.2 (preprečita najdražje napake).
3. Hitre preverbe (1.1) in krajši poziv (2.1, 2.2).
4. Nadaljevanje seje (1.2).
5. Usmerjanje modelov (3), umerjeno na podatkih iz točke 0.
6. Kroženje računov in mrtvi čas (4.3, 4.4).

Vsaka sprememba upravljalnika spremeni fingerprint (`overnight/*.mjs` je med merilnimi
vhodi), zato je ne uvajaj sredi A/B serije K3.
