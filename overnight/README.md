# Nočni upravljalnik (OPS.1)

Lokalna vrsta razvojnih nalog za isti Windows PC. Uporablja obstoječe merilne skripte,
Codex CLI in Claude Code CLI. Node.js je že nameščen; dodatnih npm paketov ni.

Prva naloga je pripravljeni **A/B K3**: enotni testi, build, pregled paketa in štiri celice
idle-200/500 × oči 0/1, vsaka s tremi ponovitvami. Naloga ima lasten seznam obveznih testov;
AI ga z zahtevo po manjši seriji ne more izbrisati. Minecraft se zažene šele z ukazom `run`.

## Priprava in prvi zagon

V PowerShellu v korenu projekta:

```powershell
.\nocni-run.ps1 init
.\nocni-run.ps1 doctor
.\nocni-run.ps1 run --dry-run
.\nocni-run.ps1 self-test
```

`init` je že izveden na tej delovni postaji. Lokalna konfiguracija je
`overnight/config.local.json`; stanje in poročilo sta v `.overnight/`. Ti poti sta izključeni
iz Gita. Ponovni `init` ne prepiše obstoječe konfiguracije ali napredka.

`doctor` najde Codex na PATH-u ter Claude tudi v native binarnem paketu razširitve VS Code
ali Cursor. Preveri samo uradni status prijave, ne pošilja modelskega poziva. Pri tej
postavitvi je bila **Codexova ChatGPT prijava potrjena**, Claude Code 2.1.282 pa je bil najden
v VS Code, vendar **ni imel aktivne terminalske prijave**. Prijava v drugi aplikaciji ne
zagotovi prijave v ta native CLI.

## Prijava Claude računov

Pred zagonom upravljalnika:

```powershell
.\nocni-run.ps1 login claude-1
.\nocni-run.ps1 login claude-2
.\nocni-run.ps1 doctor
```

Vsak ukaz odpre uradni native Claude Code login. Pri prvem izberi prvi račun, pri drugem
drugi račun. Drugi profil ima svoj `CLAUDE_CONFIG_DIR` v `.overnight/claude-2` in je do
uspešne prijave izklopljen. Upravljalnik prijavnih žetonov ne bere, kopira ali posreduje;
hrani in osvežuje jih Claude Code sam. `login` omogoči profil in odstrani njegovo lokalno
oznako neuspele prijave. Za Codex je po potrebi enakovreden ukaz `login codex`.

Podpora native CLI ne pomeni uradne potrditve združevanja osebnih naročnin. Izrecna uradna
potrditev avtomatskega kroženja dveh osebnih Claude naročnin zaradi kvote ni bila najdena.
Vsak račun ohrani svoje omejitve. Lokalni profili kvot ne seštevajo v skupen račun.

Upravljalnik odstrani običajne API/cloud credential override iz okolja agentov ter pred
vsakim AI zagonom preveri native naročniško prijavo. Ne doda API ključa in ne vklopi
samodejnega API rezervnega plačevanja. **Že omogočena dodatna poraba v uporabnikovem Claude
računu je ločena nastavitev ponudnika**; te nastavitve upravljalnik ne spreminja.

## Med službo ali ponoči

```powershell
.\nocni-run.ps1 run --hours 8
```

Lahko tudi dvoklikneš `ZAZENI-NOCNI.bat`. To je ročen zagon za naslednjih osem ur;
ponavljajoč Windows urnik ni nameščen. Čas se meri od zagona, brez predpostavke o uri
odhoda v službo. Po izteku se ne začne nova operacija, že začeta pa se dokonča.

Samo že pripravljene teste, brez AI analize, zaženeš z:

```powershell
.\nocni-run.ps1 run --hours 8 --test-only
```

AI kvota ne vpliva na tek že sprožene testne serije. Ko testi končajo, se pri `--test-only`
shrani mesto za analizo in program konča. Običajen naslednji `run` nadaljuje z analizo.

Pred klientsko meritvijo upravljalnik počaka na:

- lokalno aktivno Windows namizje, brez zaklepa ali oddaljene namizne seje;
- vsaj 120 sekund brez vnosa;
- odsotnost drugih `java`/`javaw` procesov;
- največ 20 % trenutne skupne CPU obremenitve.

Te nastavitve so v lokalni konfiguraciji. Med delovanjem je spanje/ugasnitev zaslona
zadržana s procesnim `SetThreadExecutionState`; trajnih Windows power nastavitev ne
spreminjamo. To **ne onemogoči varnostnega zaklepa zaslona**. Prvo pravo nočno meritev je
treba preveriti tudi glede nastavitev zaklepa in grafičnega gonilnika. Pred/po testu se
preveri sistemski zadnji vnos; zaznan vnos ali neaktivno namizje razveljavi serijo. Miške
in tipkovnice upravljalnik ne blokira in drugim procesom ne pošilja ukazov za zapiranje.

Začetni unit/build/package koraki imajo tudi `quiet: true`: pred zagonom počakajo na iste
pogoje miru, da ne obremenijo že aktivne meritve druge seje.

Obstoječa merila kamere K5 in tujih Java procesov K0/K8 ostanejo. Motena celica se ponovi
največ dvakrat; ponavljajoče motnje zahtevajo pregled. Drugi CPU procesi se ne ubijajo.
Meritev mora biti še vedno nad šumom po pravilih projekta. Virtualizacija ali drug PC
zahtevata novo primerljivo izhodišče.

## Stanje, pavza in nadaljevanje

```powershell
.\nocni-run.ps1 status
.\nocni-run.ps1 pause
.\nocni-run.ps1 resume
```

`pause` prepreči začetek novih operacij; obstoječa operacija se dokonča. `resume` odstrani
pavzo. Če upravljalnik ne teče več, po `resume` ponovno uporabi `run`.

Poročilo je `.overnight/POROCILO.md`, strojno stanje `.overnight/state.json`, vsi pozivi,
izpisi in rezultati operacij pa `.overnight/operations/<id>/`. Pozivi vsebujejo cilj in
predajo projekta; to so lokalni delovni podatki. Dnevnik ni skupna kopija naročniških
prijav. Med meritvijo upravljalnik ne izvaja buildov ali AI operacij.

`Ctrl+C` ustavi upravljalnik. Njegov ločen delovni proces dokonča že začeto operacijo in
zapiše rezultat. Ponovni `run` prevzame isto operacijo in je ne sproži še enkrat. Zaključene
celice ostanejo zaključene. Kvant nadaljevanja je celotna celica (tri ponovitve), ne ena
Minecraft sličica ali pol že prekinjene ponovitve.

Če odpove tudi delovni proces, se naloga označi za pregled; AI operacije z neznanimi
delnimi posledicami se ne ponavljajo samodejno. Po pregledu živih procesov in sprememb:

```powershell
.\nocni-run.ps1 retry ID_NALOGE
.\nocni-run.ps1 run --hours 8
```

`retry` ponastavi omejitev iteracij. Pri testni fazi ponovno začne serijo, stare rezultate
pa ohrani kot zgodovino. Windows named mutex prepreči dva upravljalnika naenkrat in se
ob izhodu procesa sprosti tudi brez ročnega brisanja zaklepa.

## AI iteracije in kvote

Vsaka AI operacija je nov native CLI poziv s ciljem, zadnjo predajo, rezultati in katalogom
testov. To je predaja shranjenega projektnega stanja, ne prenos skritega razmišljanja ali
zasebne zgodovine med ponudniki. Native zgodovino sej še naprej upravljata ponudnikovi orodji.

Agent lahko zahteva `test`, vrne `done` ali `blocked`. Ob `test` upravljalnik izvede zahtevane
vnaprej registrirane skripte. AI nima generiranega PowerShell ukaza, ki bi ga upravljalnik
izvedel kot kodo. Parametri se prenesejo kot podatki z dictionary splattingom. Codex uporablja
standardni `--approve-for-me`; Claude `acceptEdits`, brez čakanja na interaktivna dovoljenja.
Claude ima izrecno dovoljena bralna/urejevalna orodja in osnovne Git ukaze za repozitorijski
commit/push protokol. Ne uporablja se bypass sandboxa ali vseh dovoljenj. Zavrnjen potreben
ukaz pomeni blokado za pregled.

Pred in po testu ter med celicami se preveri SHA-256 vhodov: koda, fixture, okoljski lock,
merilne skripte in konfiguracija testov. Ob spremembi ni združevanja rezultatov različnih
verzij. `done` brez svežega uspešnega rezultata vseh obveznih testov je zavrnjen. Sprememba
produkcijske kode po meritvi zato praviloma zahteva novo serijo.

Preklop profila se zgodi ob dejanski napaki kvote/prijave v ponudnikovem error ovoju, ne
ob omembi »rate limit« v uspešnem odgovoru ali testnem logu. Vrstni red profilov je vrstni
red v konfiguraciji. Če CLI poda strojno berljiv reset, se uporabi ta; sicer se naslednji
poskus označi kot **ocena** in odloži za 15 minut. Ne predpostavljamo, da se vedno sprosti
po petih urah: obstajajo tudi tedenske omejitve. Ko so vsi uporabni profili v cooldownu,
upravljalnik čaka brez AI pozivov do najzgodnejšega naslednjega poskusa.

Največ šest uspešno vrnjenih AI odločitev na nalogo prepreči neomejen cikel brez zaključka.
Avtentikacijska napaka izklopi samo prizadeti profil do novega `login`. Druga napaka ali
neveljaven strukturiran odgovor se shrani za pregled.

## Dodajanje naslednjih paketov

```powershell
.\nocni-run.ps1 add "Izvedi naslednji ze odobreni paket M5 po stanju projekta; ob odprti odlocitvi vrni blocked."
```

Vsaka naloga je en delovni paket po repozitorijskem protokolu. Več nalog lahko dodaš tudi
med tekom. Neodvisni `inbox` prepreči prepis aktivnega stanja. Zaključena ali blokirana
naloga ostane vidna, upravljalnik pa lahko prevzame naslednjo nalogo; agent mora pred
delom preveriti njene odvisnosti in dogovorjeni vrstni red.

Za testno nalogo brez začetnega AI koraka:

```powershell
.\nocni-run.ps1 add "Ponovi dogovorjene teste" --tests unit,build,package
```

Nove scenarije se pred uporabo vpiše v `tests` lokalne konfiguracije. Vsak ima obstoječo
projektno `.ps1` pot, parameter dictionary, časovno mejo in po potrebi `desktop: true`.
Prva verzija vključuje K3 katalog; **vsi prihodnji scenariji K5/M5-V še niso registrirani**.
Če agent potrebuje manjkajoč scenarij, mora vrniti `blocked` s točno zahtevano razširitvijo,
ne zaobiti upravljalnika ali trditi, da je paket zaključen. Konfiguracijo spremeni med
ustavljenim upravljalnikom, nato ponovno uporabi `run --dry-run`.

## Preverjanje in meje prve postavitve

`self-test` izvede 29 testov krmiljenja, parserjev ponudniških izhodov, kvot, zaklepov,
sprememb vhodov, argumentov PowerShell, časovnih mej in dejanskega nadaljevanja ločenega
sintetičnega procesa po izhodu upravljalnika. Ne kliče AI modelov in ne zaganja Minecrafta.
Preverjen je tudi lokalni `init`, `doctor`, suhi zagon in sintaksa PowerShell/Node datotek.

**Pravi Minecraft A/B in živi AI poziv iz tega upravljalnika pri postavitvi nista bila
izvedena.** Prijavi Claude-1/2 zahtevata uporabnikov native login. Prvi pravi cikel ostane
praktična integracijska preverba. K3 zato v projektu ostane neizmerjen in nezaključen.

Uradna dokumentacija, preverjena ob postavitvi:

- [Codex: non-interactive mode](https://learn.chatgpt.com/docs/non-interactive-mode)
- [Claude Code: programmatic usage](https://code.claude.com/docs/en/headless)
- [Claude Code: CLI reference in auth status](https://code.claude.com/docs/en/cli-reference)
- [Claude Code: nastavitve in CLAUDE_CONFIG_DIR](https://code.claude.com/docs/en/settings)
- [Anthropic: credential use](https://code.claude.com/docs/en/legal-and-compliance)
