# Animacijsko jedro: poze → animacija → slika → igra

Narejeno po vzoru `fusion_MCA` (`tools/anim`), a brez GeckoLiba: mod sam bere
Bedrock `.geo.json` in `.animation.json`. Python predogled in Java predvajalnik
uporabljata **isto matematiko** (`rig.py` ↔ `anim/GeoMesh.java`, `AnimClip.java`,
`Easing.java`), zato slika predogleda kaže točno to, kar nariše igra.

```
tools/models/build_<entiteta>.py ─▶ geo/<e>.geo.json + textures/entity/<e>.png   (skupno: geobuild.py)
tools/sounds/make_<e>_sounds.py  ─▶ sounds/<e>/*.ogg + vnosi "<e>.*" v sounds.json (skupno: synth.py)
animations_src/<e>/rig.yaml        model, kontrolniki, konice, preverjanja, merilo predogleda
animations_src/<e>/poses.yaml      knjižnica imenovanih poz
animations_src/<e>/mob.yaml        statistike, tabela napadov, glasnosti zvokov, koraki
animations_src/<e>/*.anim.yaml ──compile.py──▶ animations/<e>.animation.json (igra: strežnik + odjemalec)
                                └─preview.py──▶ animations_src/<e>/_preview/*.png (+ .gif)
tools/anim/parity.py ─▶ src/test/resources/anim_parity/<e>.json ─▶ gradlew test (Java == Python)
```

Entitete: `troll` (dvonožec s kijem), `warg` (štirinožec), `giant_spider` (osem nog, skupinski kontrolniki, `floor`).

Potrebno: Python 3 z `numpy`, `pyyaml`, `Pillow` (za zvoke še `scipy` in `ffmpeg`).

## Ukazi

```bash
py -3 tools/models/build_troll.py                  # model + tekstura
py -3 tools/anim/preview.py model troll --face     # model z vseh strani
py -3 tools/anim/preview.py poses troll stand smash_hit   # poze (ali --all); izpiše položaje konic v blokih
py -3 tools/anim/preview.py clip troll smash --gif # prevede klip, nariše, preveri
py -3 tools/anim/compile.py troll                  # prevede vse klipe trolla
py -3 tools/sounds/make_troll_sounds.py            # zvoki + sounds.json
```

py -3 tools/anim/parity.py                         # referenčni vzorci za test Java == Python
py -3 tools/anim/check.py                          # VSE preveri: klipi, zvoki, napisi, Java registracija
```

Po `compile.py` poženi še `parity.py` in `gradlew test` (oz. `build.bat`, ki teste
požene sam): test preveri, da Java vzorči vse klipe enako kot predogled in da
klipi premikajo samo kosti, ki v modelu obstajajo.

Po `compile.py` je treba mod ponovno zgraditi (`build.bat` / `jar.bat`), ker je
`.animation.json` del JAR-a.

## Kako brati sliko

Vrstice so pogledi: **F** spredaj (njegova desna je LEVO na sliki), **S** z
njegove desne (gleda v desno), **T** tričetrt, **TOP** od zgoraj (gleda gor).
Stolpci so beati in dogodki. Mreža na tleh = 1 blok. Barvne črte so poti konic
iz `rig.yaml → trace` (kij, leva pest ...). **Rdeče območje na tleh** je `hit`
dogodek v trenutku, ko se zgodi: kij mora pristati v njem. Spodaj je izpisano,
kje je konica ob udarcu (naprej / desno / višina v blokih), rdeče vrstice so
težave (noga v tleh, kij v tleh, konica skozi trup, konica izven območja udarca).

## Oblika `.anim.yaml`

```yaml
name: smash
length: 2.0            # sekunde
loop: false
ground: auto           # spodnja noga ostane na tleh; off = brez (skoki, padci)
mirror: false          # zamenja levo/desno (pari v rig.yaml -> mirror)
stride: 3.0            # samo hoja: bloki na en cel cikel; čas teče z razdaljo
beats:
  - {t: 0.00, pose: stand}
  - {t: 0.55, pose: smash_windup, ease: out, hold: 0.22}
  - {t: 0.95, pose: smash_hit, ease: strike}
  - {t: 2.00, pose: stand}
events:
  - {t: 0.95, type: hit, id: smash, shape: circle, forward: 5.1, side: 0.8, radius: 2.6,
     damage: 1.0, knockback: 1.2, lift: 0.35}
```

Beat: `pose` (ime ali `{kontrolnik: vrednost}`), `add` (prišteje), `solve`
(glej spodaj), `ease`, `overshoot` (0.05–0.15, ne pri udarcih v tla), `hold`,
`airborne: true`.

Easing: `strike`/`in` pospeši v udarec, `out`/`soft` upočasni (zamah nazaj,
umirjanje), `smooth` privzeto, `back`, `elastic`, `bounce`, `linear`, `step`.

### `solve` — naj prevajalnik sam najde kot

```yaml
solve: {tip: club, using: wrist_r.bend, height: 0.1}     # konica kija na tleh
solve: [{tip: club, using: arm_r.raise, forward: 4.5}, {tip: club, using: wrist_r.bend, height: 0.1}]
```
Za eno točko iz `tips` in en kontrolnik poišče vrednost, pri kateri je konica
na želeni višini / razdalji (bloki). Uporabi ga namesto ugibanja kotov.
`solve` se ne deduje prek `base:`.

## Dogodki

| type | kdo | ključi |
|---|---|---|
| `hit` | strežnik | `shape: circle` (`forward`, `side`, `radius`) ali `arc` (od entitete: `radius`, `arc` stopinj, `dir` 0 = naprej, + = njena desna); `damage` × melee moč iz CustomNPCs urejevalnika; `falloff` (škoda na robu); `knockback` bloki odriva; `lift` skok; `effect` (vanilla ime, npr. `slowness`) + `effect_seconds`, `effect_level`; `fire` sekunde gorenja; `below`/`above` navpični doseg; `tip` točka za preverjanje v predogledu |
| `sound` | strežnik (vsi slišijo) | `sound: troll.roar` ali seznam za plastenje; `volume`, `pitch` |
| `lock` | strežnik | od tu naprej se ne obrača več za tarčo |
| `lunge` | strežnik | `forward`: bloki na tik naprej, `ticks`: koliko tikov (privzeto 1), `up`: navpična hitrost skoka (0.42 ≈ 12 tikov v zraku) |
| `projectile` | strežnik | `kind: rock / web / arrow / small_fireball`, `from:` konica iz rig.yaml (compile.py zapiše njen položaj kot `origin`), `speed` bloki/tik (določi čas leta), `damage` (× melee), `radius`, `knockback`, `lift`, `size`, `impact_sound`, `shake` (potres ob pristanku), `effect` (+ `effect_seconds`, `effect_level`), `lead` (predvidevanje gibanja tarče), `inaccuracy`; let je balističen in pristane na tarči |
| `summon` | strežnik | prikliče `count` entitet `entity` (npr. `"minecraft:cave_spider"`) okoli `forward`/`side` v `radius`; največ `max_alive` te vrste v 32 blokih; dobijo tarčo, NPC-ji še frakcijo |
| `buff` | strežnik | učinek (`effect`, `effect_seconds`, `effect_level`) zase in za NPC-je iste frakcije v `radius` blokih |
| `shake` | odjemalec | `strength` (1 = udarec kija), `radius` bloki, `duration` s |
| `dust` | odjemalec | prah iz bloka pod točko: `forward`, `side`, `radius`, `count` |
| (`hurt` klip) | odjemalec | ni dogodek: klip z imenom `hurt` se ob prejeti škodi za trenutek primeša |
| `step` | odjemalec | korak: zvok, prah, rahel potres; `foot: r/l` ali `forward`/`side`, `heavy`; privzeto iz `mob.yaml → step` |

`forward`/`side` so v blokih glede na trolla (naprej, njegova desna).

## Rekviziti (`prop_*`)

Kost z imenom `prop_...` (npr. `prop_rock` v levi roki trolla) je skrita, razen
ko ji klip da velikost: kontrolnik `[prop_rock, scale, xyz, 1]` (`rock.show: 1`).
Privzeta velikost rekvizita je 0, zato ga je treba prikazati le v beatih, kjer
ga drži; trenuten pojav: dva beata 0.02 s narazen (glej `troll/throw.anim.yaml`).
V igri rekvizit sledi klipu napada brez mešanja (je ali ga ni).

## `mob.yaml` — vedenje

```yaml
name: Warg
title: Pack Hunter       # (neobvezno) napis pod imenom
boss_bar: 2              # (neobvezno) CustomNPCs boss bar: 0 brez, 1 vedno, 2 ko ima tarčo
boss_color: red          # pink, blue, red, green, yellow, purple, white
hitbox: [1.7, 2.3]       # širina, višina (bloki)
health: 120
melee: 14                # × `damage` vsakega hit dogodka
speed: 7                 # CustomNPCs hitrost hoje
knockback_resist: false
voice: {idle: warg.idle, angry: warg.growl, hurt: warg.hurt, death: warg.death}
attacks:                 # izbira po teži med napadi v dosegu in izven cooldowna
  - {clip: bite,   min: 0.0, max: 3.6, weight: 5, cooldown: 18,  recover: 6}
  - {clip: pounce, min: 3.5, max: 7.5, weight: 4, cooldown: 100, recover: 12, wounded: 2}
greet: {clip: howl, range: 24, cooldown: 900}   # ob novi tarči
enrage: {below: 0.3, clip: howl, damage: 1.2, speed: 1.3, cooldown: 0.6}
  # pod 30 % zdravja: enkrat klip, nato ×škoda, ×hitrost hoje, ×cooldowni; jezni delci
step: {sound: warg.step, volume: 0.6, shake: 0}
sound_volume: {warg.howl: 6.0}
```

`crowd` doda težo za vsakega dodatnega sovražnika v 5 blokih, `wounded` pod
polovico zdravja.  `compile.py` preveri, da klipi in zvoki obstajajo.  To so
privzete vrednosti; CustomNPCs urejevalnik jih lahko spremeni za posamezen NPC.

`rig.yaml → preview: {scale: 1.65, side: 0.42}` poveča manjše entitete na listih.

### Posebnosti `rig.yaml`

- Sestavljen kontrolnik: seznam `[kost, kanal, os, predznak]` premakne več kosti
  hkrati, npr. `gait_a.lift` dvigne štiri noge pajka (glej `giant_spider/rig.yaml`).
- `floor: 0` — višina tal v px, kadar osnovna poza modela ni stoja (pajek ima v
  osnovi vodoravne noge; prava stoja je poza `stand`, vse ostale jo dedujejo).
- Os `xyz` (samo za `scale`): ista vrednost na vseh treh oseh (rekviziti).
- Pri novem rigu najprej preveri smeri: testne poze z enim kontrolnikom
  (`t_lift: {leg_r1.lift: 40}`), pogled `--views front,side,top`.

## Postopek za AI

1. Poglej `poses.yaml`; uporabi obstoječe poze.
2. Nova poza: dodaj, poženi `preview.py poses troll <ime>`, **poglej sliko**,
   popravljaj. Za konice na tleh uporabi `solve`.
3. Klip v `animations_src/troll/<ime>.anim.yaml` (4–6 beatov), `preview.py clip
   troll <ime> --gif`, poglej sliko. Postavi `hit` tja, kjer kij pristane
   (izpis "naprej X desno Y").
4. Nov napad v igri: dodaj vrstico v `mob.yaml → attacks` (ime klipa, razdalja,
   teža, cooldown). Vse ostalo (čas, škoda, območje, zvok, potres) je v klipu.
5. `compile.py <e>`, `parity.py`, nato `build.bat`.

## Preizkus v igri

`/cne list`, `/cne play <klip>` (najbližji animirani NPC takoj odigra klip),
`/cnedebug` (odjemalec: obrise območij zadetkov za sekundo nariše na tla).
Dev client: `gradlew runClient` (z `-noverify`).

## Nova entiteta

Najhitreje z ogrodjem iz obstoječe pošasti (troll / warg / giant_spider):

```bash
py -3 tools/anim/new_entity.py cave_bear --from warg --name "Cave Bear" --egg 6B4A2E 3A2A1A
```

Prekopira model, rig, poze, klipe, `mob.yaml` in zvočno skripto, ustvari Java
razred, ga registrira (id, jajce, renderer) in doda napise; nato vse enkrat
zgradi.  Pošast takoj deluje (kot kopija predloge); potem jo preoblikuj:

1. `tools/models/build_<e>.py` (kopiraj `build_warg.py`; `geobuild.build(...)`),
   `preview.py model <e> --face`.
2. `animations_src/<e>/rig.yaml` (kontrolniki — smeri preveri s testnimi pozami
   z enim kontrolnikom), `poses.yaml`, klipi, `mob.yaml`.
3. `tools/sounds/make_<e>_sounds.py` (kopiraj `make_warg_sounds.py`; `synth.publish(...)`),
   napisi v `lang/en_us.lang`.
4. Java: razred `Entity<E> extends EntityAnimatedNpc` z `entityId()`, registracija v
   `CustomNpcEntities.registerEntities` in `ClientProxy` (`RenderAnimatedNpc.factory("<e>")`).
