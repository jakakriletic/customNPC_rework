# Kaj moraš pognati — zaključek M0.7 (integracijska matrika)

Dva kosa, neodvisna. **A** je 100 % skriptiran in traja ~20–30 min večinoma brez tebe.
**B** te stane ~10 min v GUI-ju in odblokira vrstici IC1/IC2 matrike.

Vse iz **korena projekta** (`C:\Users\jaka.kriletic\Documents\CustomNPC_mod_rework`),
v **PowerShellu**, server ne sme teči vzporedno.

---

## A — prvi realni prehod 1 matrike

Poženi po vrsti. Vsaka skripta vrne izhodno kodo `0` (zeleno) ali `1` (rdeče).
**Če katera vrne 1, ustavi se in mi pošlji njen log** — matrika se ne nadaljuje z opombo.

```powershell
cd C:\Users\jaka.kriletic\Documents\CustomNPC_mod_rework

.\dev.ps1 build                  # 1. svez jar
.\verify-package.ps1             # 2. seznam spremenjenih razredov = pricakovan
.\testworld-run.ps1              # 3. W1-W8  (IN1, IN4, IS1, IL1)   ~8 min
.\rwdiag-run.ps1                 # 4. D1-D7 + C1-C6 (IL7)           ~5 min
.\r1-run.ps1                     # 5. E1-E6 (IA7)                   ~6 min
```

Po vsaki: `echo $LASTEXITCODE` (0 = OK).

Če se pritoži, da `dev\run\eula.txt` ni sprejet, dodaj `-AcceptEula` **enkrat**:
`.\testworld-run.ps1 -AcceptEula`.

**Kaj mi pošlješ nazaj:** vsebino mape `audit\` — konkretno
`m06-testworld-a/-b/-c.log`, `m21-rwdiag.log`, `m22-r1.log`, `package-verification.txt`
(ali samo povej "vse zeleno", pa preberem sam iz mape, ker jo vidim).

---

## B — quest in dialog fixture v GUI-ju (enkratno)

Brez tega ostaneta IC1 (dialog availability) in IC2 (quest accept/progress/complete/reward)
blokirani do M10. NBT strukture ne smem ugibati, zato mora to nastati v pravem GUI-ju.

### 1. Zaženi svet in klient

```powershell
.\testworld.ps1                        # namesti seme (arhivira star svet)
.\dev.ps1 runServer --offline          # okno 1 — pusti teci
```

V drugem PowerShell oknu:

```powershell
.\dev.ps1 runClient --offline
```

V klientu: Multiplayer → Direct Connect → `localhost` → Join.

### 2. Dialog

1. V inventarju vzemi **NPC Wand** (`/give @p customnpcs:npcWand`).
2. Desni klik na NPC-ja **`T_Trader`** (stoji pri spawnu, `0 4 0` do `8 4 8`).
3. Zavihek **Advanced → Dialogs** → **Add** → nov dialog:
   - **Name:** `TW_Dialog`
   - **Text:** `TW-DIALOG-LINE`
   - ena **Option**: `Konec`, tip *Close dialog*
4. Shrani in dialog **pripni na `T_Trader`** (Dialogs → Add na NPC-ju).

### 3. Quest

1. Meni **Quests** (v NPC GUI: Advanced → Quests, ali `/noppes quest`).
2. Nov quest:
   - **Name:** `TW_Quest`
   - **Type:** `Item`
   - **Objective:** 1× `minecraft:dirt`
   - **Reward:** 1× `minecraft:stone`
3. Quest pripni na dialog `TW_Dialog` (v dialogu: Options → nova opcija → *Quest* → `TW_Quest`).

> Imena morajo biti **točno** `TW_Dialog` in `TW_Quest` — matrika in skripte se sklicujejo nanju.

### 4. Zapri in shrani

V okno serverja vpiši `stop` in počakaj, da se proces konča sam (drugače zapis ni zagotovljen).

### 5. Kopiraj fixture v seme

```powershell
Copy-Item dev\run\world\customnpcs\dialogs -Destination dev\testworld\customnpcs\ -Recurse -Force
Copy-Item dev\run\world\customnpcs\quests  -Destination dev\testworld\customnpcs\ -Recurse -Force
dir -Recurse dev\testworld\customnpcs\dialogs, dev\testworld\customnpcs\quests
```

**Kaj mi pošlješ nazaj:** izpis zadnjega ukaza (da vidim, da datoteki obstajata).
Ostalo — vnos v `dev/testworld/README.md`, posodobitev matrike (IC1/IC2 iz *blokirano* v
*ni pokrito* → nato v *A*) in commit — naredim jaz.

---

## Vrstni red, če imaš malo časa

- **Samo A** → prehod 1 je opravljen, M0.7 postopek je dokazano izvedljiv, ne le napisan.
- **Samo B** → odblokirata se IC1/IC2, A lahko počaka na naslednji paket.
- Če gre oboje, poženi **B najprej** — potem A teče nad svetom, ki že ima quest in dialog.

---

## Eno vprašanje

`dev/testworld/r1-control.js` ima nezacommitano spremembo iz prejšnje seje (+15 vrstic
komentarja o dometu iskanja poti, Q11). Naj jo commitam posebej, ali jo pustim?
