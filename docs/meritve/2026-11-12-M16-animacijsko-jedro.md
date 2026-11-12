# M16 — vgrajeno animacijsko jedro (12. 11. 2026)

Zahteva lastnika (12. 11.): animacijsko jedro po vzoru `ladja_mod`, veliko že narejenih modelov s kostmi,
AI naj naredi animacije in napade (JS) in jih da v clone JSON NPC-ja; animiran boss/vojak v igri brez
ponovnega prevajanja jarja.

## Odločitve (M16.0)

- Format: Bedrock `.geo.json` (kosti, kocke, UV) in `.animation.json` (ključni okvirji po kosteh, easing,
  zanka, `stride`, dogodki), kot `customnpcentities` 0.7.0 (lastnikov mod) in z isto matematiko
  (`GeoMesh`, `AnimClip`, `Easing`, `Pose` prenesene v `noppes.npcs.rework.rig`). Semantični kontrolniki
  `ladja_mod` (`arm_r.raise` ...) ostanejo v orodjih (`rig.yaml` → prevajalnik), igra bere kosti.
- Podatki NPC-ja so v NBT `RwRig` (Model, Texture, Scale, Clips, Idle/Walk/Hurt/Death) in v spawn
  podatkih, zato jih nosijo clone tab, `/noppes clone` in spawnerji. Lastni klipi dodajo/zamenjajo
  klipe modela po imenu.
- Predvajanje akcije teče na strežniku (`RigPlayer`: čas, dogodki, vrsta, JS hooki `animationStart/Event/End`),
  klient dobi začetek/konec po kanalu `rwrig` s številko zaporedja (CustomNPCs pošilja posodobitve NPC-ja
  asinhrono; starejša novica ne razveljavi novejšega klipa). Idle/hoja/hurt/smrt klient izračuna sam.
- Napadi so podatki: `hit`, `sound`, `lunge`, `heal` delujejo na strežniku; vsak dogodek gre tudi v
  `animationEvent`. `Animators.of(npc)` vrne animator jedra za NPC z rigom (isti `IAnimator` kot prej).
- Stikalo `RwAnimCore` (privzeto 0): z 0 se `RwRig` ohrani, a se ne riše, ne predvaja in ne pošilja.
- Paket (`*.rig.json`: model, ime, merilo, vloge, klipi, JS skripta) in `/rwrig apply`: ena datoteka, ki jo
  napiše AI; po shranjevanju v clone tab je vse v NPC-ju.

## Knjižnica modelov

`humanoid`, `knight`, `skeleton_warrior` (novi, eno okostje: komolci, kolena, meč, ščit; 15 klipov:
idle, walk, guard, slash, overhead, thrust, block, kick, spin, leap_slam, warcry, salute, wave, hurt, death),
`ogre` (nov model na okostju trolla, vsi trollovi klipi), `troll`, `warg`, `giant_spider`, `cave_bear`
(iz `customnpcentities` 0.7.0 z zvoki pod `rwrig:`). Orodja `tools/rig/` (gradnja modelov, prevajalnik poz in
klipov, predogled PNG) in viri `rig_src/` so preneseni; prevod trolla je bitno enak izvirniku.

## Meritev

`rig-run.ps1` (integriran dev klient, izoliran run dir, `RwAnimCore=1`, brez `customnpcentities`)
**PASS** `audit/m16-rig-2026-11-12-123724.json`, 14/14: 8 NPC-jev le z NBT `RwRig` na strežniku in klientu,
vsi z mrežo (1912 sličic v 2 s), klip `smash` na klientu, `hit` iz klipa zombi 20 → 13,08, lastni klipi brez
napak, klon prek NBT obdrži model in lastne klipe, lastni klip se predvaja in pride na klient,
`/rwrig apply knight_boss.rig.json` (vitez ×1,25, JS vklopljen, lastni klip `taunt`), JS sam izbere
napad na tarčo: zombi 20 → 14. Slike `audit/screenshots/m16-rig-idle.png`, `m16-rig-attack.png`,
`m16-knight-boss.png`. Zavrnjena vmesna teka: knjižnica se ob `setClips` ni osvežila; klient je izgubil
klip zaradi pozne asinhrone posodobitve NPC-ja (oboje popravljeno). Unit 842 (+8 `RigCoreTest`), build in
paket PASS (novi vnosi samo pod `assets/customnpcs/rig/`, `textures/rig/`, `assets/rwrig/`).

## Legionar (Steve), privzetki NPC-ja (zahteva lastnika 12. 11.)

Lastnik: legionar naj bo navaden Steve s preneseno kožo legionarja; ob napadu na sovražnika dvigne ščit,
nato napade. `steve` in `legionary`: telo igralca z UV-ji kože 64x64 (notranji in zunanji sloj), razdeljeno na
komolce in kolena (iste kosti kot človeška družina, vsi klipi veljajo), nariše se s CustomNPCs kožo NPC-ja;
legionar ima še gladius in scutum kot ločen `legionary_gear` z lastno teksturo. Klipi `shield_advance` (zanka,
samo zgornji del telesa: noge hodijo naprej; akcija se zdaj meša samo na kosteh, ki jih klip premika),
`shield_stab`, `shield_bash`; paket `rig_src/examples/legionary.rig.json` (JS: `target` dvigne ščit, `tick`
napade v dosegu, `animationEnd` ga spet dvigne). Paket sprejme `skinTexture`/`skinUrl`/`skinPlayer`.
Nov NPC privzeto brez dodatnih oči (Eyes Type -1) in brez respawna (SpawnCycle 3); shranjeni obdržijo svoje.

Popravki po meritvi: `hit` je meril od točke 1 blok pred NPC-jem (privzeto `forward` 1 namesto 0 kot v
izvirniku), doseg se zdaj skalira z velikostjo NPC-ja. **Pomembno:** prejšnja meritev
`m16-rig-2026-11-12-123724` ni dokazala škode iz `hit`: zombi je izgubljal zdravje zaradi sonca. Harness zdaj
uporablja vindicatorja (ne gori) in šteje samo škodo z NPC-jem kot virom (`LivingHurtEvent`).

`rig-run.ps1` **PASS** `audit/m16-rig-2026-11-12-132013.json`, 21/21: 10 modelov, troll `smash` brez AI zada
5,0 (1,0 x melee 5), JS vitez (taunt, thrust, kick, overhead, slash) 26+ škode, legionar dvigne ščit (tudi na
klientu), napreduje 5,7 → 1,3 bloka, `shield_stab` 6,0 in `shield_bash` 2,0, sovražnik mrtev. Slike
`audit/screenshots/m16-legionary-advance.png`, `-attack.png`. Unit 844, paket PASS.

## Omejitve

Samo integriran klient (en igralec); dedicated z dvema klientoma, pozni vstop, 200 animiranih NPC-jev (cena)
in `/rwrig reload` na več klientih še niso izmerjeni (M16.5). Uporabniški modeli iz `customnpcs/rigs/` morajo
biti tudi na klientih. Rig nariše samo model (brez oklepa, predmetov v roki in CNPC delov); `steve`/`legionary` podpirata samo kože 64x64 (ne stare 64x32). Ni GUI-ja za
izbiro modela in klipa (M16.4); stari `customnpcentities` hooki ostanejo za tiste entitete.
