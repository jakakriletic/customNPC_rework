# Nočna izmena (OPS.2)

Lokalni frontend in trajni upravljalnik za ta Windows PC, Codex CLI in dva Claude Code
profila. Brez dodatnih npm odvisnosti. Dvoklikni **ODPRI-UPRAVLJALNIK.bat** ali
**ZAZENI-NOCNI.bat**. Odpre se stran na `127.0.0.1`, delo pa se začne z gumbom
**Zaženi neprekinjeno**. Zaprtje brskalnika dela ne prekine.

## Gumbi in potek

- **Zaženi neprekinjeno:** deluje brez roka in brez fiksne meje AI iteracij. Najprej nadaljuje
  shranjeno nalogo. Po končanem ali resnično odloženem paketu AI izbere naslednji izvedljivi
  paket iz projektnega načrta. Pri novem milestone najprej opravi zahtevano raziskavo.
- **Pavza po koraku:** trenutno operacijo dokonča, naslednje ne začne. **Nadaljuj** odstrani pavzo.
- **Ustavi zdaj:** zahteva preklic delovnega procesa. Ta prekine samo svoje neposredno
  zagnane procese in njihove podprocese (Windows `taskkill /T /F`), shrani rezultat preklica
  in izpusti delno testno celico. Upravljalnik potrdi prekinitev in konča. Običajno traja
  nekaj sekund; native preverjanje prijave pred zagonom lahko traja do 20 sekund.
  Nepovezanih Java procesov ali drugih sej ne zapira.
- **Po vsaki AI iteraciji:** delovni proces AI teče v Windows job objektu (kill-on-close).
  Ko je rezultat shranjen, se job zapre in ustavi tudi osirotele podprocese (npr. pozabljen
  `find /`), ki jih `taskkill /T` ne doseže več. Testi in meritve tega ne uporabljajo.
- **Kvote:** Claude omejitev (»session/weekly limit«, HTTP 429) je kvota, ne napaka paketa.
  Profil počaka do časa iz sporočila, npr. `resets 5:30pm (Europe/Ljubljana)`; paket se ne
  odloži. Brez navedenega časa velja `unknownQuotaRetryMinutes`.
- **Dodaj cilj:** doda trajno nalogo tudi med delovanjem. **Poskusi znova** vrne odloženo
  nalogo v vrsto. **Dnevnik izvajanja** pokaže zadnji izpis brez samodejnega nalaganja
  velikih logov med meritvijo.
- **Preveri prijave:** preveri native naročniško prijavo brez modelskega poziva; na voljo,
  ko je delo ustavljeno. **Prijava** odpre uradni interaktivni login v ločenem terminalu.

Gumb Start odstrani prejšnji STOP in pavzo. Že uspešne celice ostanejo shranjene; prekinjeno
celico je treba izvesti znova. Zaprtje ali padec upravljalnika brez Stop pusti ločen delovni
proces, da dokonča trenutno operacijo. Naslednji Start prevzame isti rezultat brez dvojnega
zagona. Windows mutex prepreči dva upravljalnika oziroma dva lokalna frontend strežnika.

**Samodejni ponovni zagon (OPS.8):** 27. 10. 2026 se je neprekinjeni upravljalnik med 20:00 in
23:28 ustavil brez zapisa v dnevniku, noč je bila izgubljena. Frontend zdaj vsakih 30 s preveri
upravljalnik. Če je bilo neprekinjeno delo zagnano, upravljalnik pa ne teče in datoteke STOP ni
(npr. prisilno ubit proces), v `controller.log` zapiše `Vmesnik: upravljalnik (pid N) se je ustavil
brez Stop` in ga znova zažene. PAUSE ostane, nedokončano operacijo prevzame nov upravljalnik.
Največ 3 ponovni zagoni v 15 minutah, nato se samodejni zagon ustavi z zapisom v dnevnik.
Stop iz vmesnika v STOP zapiše vir (`<čas> vmesnik Ustavi zdaj`). Nadzor deluje samo, dokler
teče frontend; po spremembi `dashboard.mjs` je treba frontend znova zagnati.

**Preverjanje pred zagonom (OPS.9):** 29. 10. 2026 je posodobitev Claude premaknila CLI iz
`~/.local/bin/claude.exe` v npm namestitev, zato je upravljalnik napačno javljal »potrebna
prijava«. Pred vsakim zagonom zato poženi `node overnight/predzagon.mjs`. Skripta za vsak
omogočen profil preveri, ali je izvršljiva datoteka najdena, ali CLI vrne verzijo, ali je
prijava naročniška in ali `--help` še pozna vse zastavice iz `agentArguments`. Izhod 1
pomeni, da je treba upravljalnik najprej popraviti, šele nato zagnati.

`launch` na Windows zažene frontend prek `Win32_Process.Create`, torej izven job objekta
procesa, ki ga je zagnal (Claude/Codex aplikacija, IDE, terminal). 22. 10. 2026 je samodejna
posodobitev aplikacije Claude ob 09:12 ubila frontend, upravljalnik in Minecraft sredi celice
`v8-10-5000-f0`, ker je bil `launch` zagnan iz Claude seje. `--attached` ohrani stari način.

Če AI ugotovi, da ni več izvedljivega dela v dogovorjenem obsegu, vrne `idle`. Upravljalnik
ostane prižgan, počaka do ene ure oziroma do spremembe merilnih vhodov ter znova preveri
plan. Dodan cilj se obravnava ob naslednjem preverjanju vrste. Ne ustvarja navideznega dela.

## Avtonomija in dovoljenja

Uporabnik je 20. 10. 2026 izrecno zahteval povsem samostojno delo do prekinitve, brez
čakanja na vprašanja ali dovoljenja. Lokalna konfiguracija ima:

```json
"autonomy": { "enabled": true, "fullAccess": true }
```

Vsak poziv to pove kot neposredno navodilo lastnika. AI sam izbere najbolje podprto možnost,
tudi pri prej odprtih Q vprašanjih, in zapiše **vprašanje + svojo odločitev + razlog** v
strukturiran rezultat, projektne evidence in `.overnight/ODLOCITVE.md`. Frontend jih pokaže
v razdelku Odločitve. Navodilo velja za ta način, opisano je tudi v korenskem `AGENTS.md`.

Codex se zažene z `--dangerously-bypass-approvals-and-sandbox`; Claude z
`--dangerously-skip-permissions --permission-prompts none`, brez starega omejenega seznama
orodij. To so dovoljenja posameznega CLI procesa. Globalne konfiguracije, Windows UAC,
varnostni zaklep in protivirusni program se ne spreminjajo. Manjkajoče prijave ali dejanskih
podatkov sistem ne more ustvariti: odvisni paket odloži in izbere neodvisno delo. Ne sme
izmišljati dokazov, odstraniti sprejemnih testov ali spremeniti izvirnega jarja/okoljskega locka.

Vsaka AI operacija obdeluje en skladen paket. Controller izvaja teste, builde in Minecraft;
AI jih ne zaganja sam. Delne spremembe drugega profila pred nadaljevanjem pregleda. Sledi
obstoječemu protokolu dokumentiranja, commit/push samo lastnih datotek in brez force-pusha.

## Trije naročniški profili

Vrstni red je `claude-1` → `claude-2` → `codex`. Codex desktop aplikacija ob vsaki posodobitvi premakne `codex.exe` v novo mapo `bin/<hash>/`; upravljalnik zato ob manjkajoči poti (ali `executable: "codex"`) vzame najnovejši `%LOCALAPPDATA%OpenAICodexin*codex.exe` (26. 10. 2026: posodobitev ob 8:56 je Codex tiho izločila iz kroženja). Vsak uporablja svojo native naročnino;
kvote niso združene. Ob dejanski napaki kvote se ohranijo diff, predaja in rezultati ter
izbere naslednji razpoložljiv profil. Ko so vsi v cooldownu, se čaka do najzgodnejšega reseta.
Strojno berljiv reset je točen, sicer je poskus čez 15 minut izrecno označen kot **ocena**.
Omejitve niso nujno samo peturne; obstajajo tudi druge ponudnikove omejitve.

**Claude 1 in Claude 2 moraš enkrat prijaviti v različna računa.** Drugi profil ima ločen
`CLAUDE_CONFIG_DIR` v `.overnight/claude-2`. Codexova ChatGPT prijava je bila pri postavitvi
potrjena, oba Claude native profila pa sta zahtevala prijavo. Frontend status dejansko
preveri z gumbom. Terminalska prijava ni nujno ista kot prijava v drugi aplikaciji.

Ob končni preverbi sta **Claude 1 in Codex prijavljena in preverjena s pravim kratkim
modelskim pozivom**. **Claude 2 še potrebuje native prijavo v drugi račun.**

Prijava odda dogodek v vrsto, zato deluje tudi med prižganim upravljalnikom. AI ne vidi
ali kopira prijavnih žetonov; hrani jih native CLI. Pred vsakim AI klicem se ponovno preveri
native naročniška prijava. API/cloud credential override se odstrani iz okolja; sistem
ne dodaja API ključa ali rezervnega API plačevanja. Že vklopljene dodatne porabe na samem
računu ponudnika ne spreminja. Vgrajena podpora native profilu ni izjava ponudnika o
uradni podpori avtomatskemu kroženju osebnih naročnin.

Avtentikacijska napaka izloči samo prizadeti profil do nove prijave. Druge AI napake se
najprej samodejno ponovijo z ohladitvijo oziroma drugim profilom; po štirih zaporednih
napakah se paket odloži z razlogom, sistem pa nadaljuje neodvisno delo. Posamezen AI klic
ima časovno mejo ene ure. Napačna meritev se ne šteje kot uspeh.

## Minecraft in veljavnost meritev

Začetna vrsta je še vedno pripravljeni **A/B K3**: unit/build/package in štiri celice
idle-200/500 × oči 0/1, vsaka s tremi ponovitvami. K3 **še ni izmerjen ali zaključen**.

Pred klientskim testom (in koraki `quiet`) se čaka na lokalno aktivno Windows namizje,
120 sekund brez vnosa, nič drugih Java procesov in CPU ≤ 20 %. Med aktivno meritvijo
ni sočasnega AI klica/builda; frontend nima animacij, zunanjih fontov ali grafičnih zank,
prijav ne preverja v ozadju, stanje bere na tri sekunde (skrit zavihek na 15 sekund).
Upravljalnik ne blokira miške. Vnos ali neaktivno namizje razveljavi celico; v avtonomnem
načinu po ponavljajočih motnjah počaka minuto in poskusi isto celico znova. K0/K5/K8
merila obstoječega harnessa ostanejo veljavna.

Procesna zahteva `SetThreadExecutionState` zadrži spanje in ugašanje zaslona samo med
tekom. Ne izklopi Windows varnostnega zaklepa. Zaklenjen PC ne more izvajati teh klientskih
meritev; upravljalnik počaka. Prvo pravo nočno izvedbo je treba preveriti s tvojim namizjem
in gonilnikom. Meritev na drugem PC/VM ni isto izhodišče.

SHA-256 zajame produkcijsko kodo, fixture, lock, merilne skripte (tudi nove) in parametre.
Rezultati različnih vhodov se ne združujejo. `done` se sprejme samo po vseh obveznih,
svežih in uspešnih testih. Raziskovalni paket brez produkcijskih sprememb in brez obveznih
testov lahko zaključi z `verification=research` in obstoječimi dokaznimi dokumenti.

AI sme pripraviti nov `.ps1` scenarij v projektu in ga prijaviti s `testDefinitions`:
`id`, `script`, `parametersJson` (JSON literalnih parametrov), `desktop`, `timeoutSeconds`.
Katalog se shrani v stanje in obnovi po restartu. Obstoječih ID-jev ni mogoče prepisati;
novi scenariji imajo privzeto namizne zaščite. Poti zunaj projekta in simbolične povezave
navzven so zavrnjene. Tako K5/M5-V nista odvisna od ročnega urejanja kataloga.

## Datoteke in CLI

`overnight/config.local.json` in `.overnight/` sta git-ignored. Stanje je `state.json`,
poročilo `POROCILO.md`, odločitve `ODLOCITVE.md`, celotni pozivi/izpisi/rezultati pa
`operations/<id>/`. To so lokalni delovni podatki; desktop strežnik jih ne objavlja na
internet. Posluša samo `127.0.0.1`, mutacije zahtevajo lokalni Origin in naključen žeton
seje. Frontend se ne registrira ali objavlja pri zunanjem gostitelju.

```powershell
.\nocni-run.ps1 init                         # že opravljeno; ne prepiše napredka
node overnight/dashboard.mjs launch         # frontend in privzeti brskalnik
.\nocni-run.ps1 run --continuous             # CLI do prekinitve (tudi privzeti run)
.\nocni-run.ps1 run --hours 8                 # opcijsko omejeno okno, brez auto follow-up
.\nocni-run.ps1 run --hours 8 --test-only     # samo testi do AI koraka
.\nocni-run.ps1 pause
.\nocni-run.ps1 stop                         # prekine operacijo; enako Ctrl+C
.\nocni-run.ps1 resume                       # odstrani pavzo/STOP; nato run, če je izklopljen
.\nocni-run.ps1 status
.\nocni-run.ps1 doctor                       # native prijave, namizje; brez modelov
.\nocni-run.ps1 login claude-1
.\nocni-run.ps1 login claude-2
.\nocni-run.ps1 add "Izvedi naslednji paket po projektnem planu."
.\nocni-run.ps1 retry ID_NALOGE               # med ustavljenim upravljalnikom
.\nocni-run.ps1 run --dry-run
.\nocni-run.ps1 self-test
```

Po CLI `stop` uporabi `resume`, nato `run`; frontend Start to uredi sam. Za nov PC najprej
namesti Node.js in native CLI ter opravi `init` in prijave. Ponovljiv Windows urnik in
samodejni Start po ponovnem zagonu PC nista nameščena.

## Preverjanje

40/40 testov: parserji, failover/reset, zaklepi, literalni PowerShell argumenti, veljavnost
dokazov, dnevnik odločitev, nove testne definicije, raziskovalni zaključek, neomejene
iteracije, čakanje brez računov in dejanski lokalni HTTP start/pause/resume/stop/restart.
Resničen sintetični proces s podprocesom se prekine, nepovezan proces ostane živ. API
zavrne napačen Host, Origin in manjkajoč žeton. Brskalniško preverjeni še gumbi in dodajanje
naloge v vrsto. Ti testi ne kličejo AI modelov in ne zaganjajo Minecrafta.

Kratka **prava Codex in Claude 1 poziva prek naročniških prijav sta uspela** z novimi
dovoljenji in polno izhodno shemo (brez orodij ali sprememb projekta). Claude 2 adapter in
**prvi pravi Minecraft cikel** ostajata integracijska preverba po prijavi oziroma ob zagonu.
Nastavitev samega orodja ne pomeni izmerjenega K3 rezultata.

Uradne reference: [Codex CLI](https://learn.chatgpt.com/docs/cli/reference),
[Claude Code CLI](https://code.claude.com/docs/en/cli-reference),
[Claude konfiguracija](https://code.claude.com/docs/en/settings).
