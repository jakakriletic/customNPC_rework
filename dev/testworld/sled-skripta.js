// M5.Z - sled skript in timerjev. Scenarij: docs/scenariji/M5.Z-sled.md
//
// Tece na fixture NPC-ju SLED_Skripta. Vir resnice je ta datoteka; v SLED_Skripta.json jo
// vlozi dev/testworld/vstavi-skripto.py (ob commitu, ne ob zagonu).
//
// Sled je zaporedje dogodkov skripte, vsak s starostjo NPC-ja (getAge = ticksExisted).
// Starost se zacne z 0 ob spawnu in raste z vsakim tickom entitete. Po restartu se NE
// zacne z 0 (prvi zagon M5.Z 22. 10. 2026: prvi tick faze 2 pri 252, ker se je svet shranil
// pri ~246), zato faza 2 zapise starost glede na starost ob init faze 2. Sled tako ni
// odvisna od tega, kdaj server NPC-ja postavi, kdaj shrani svet ali kako je obremenjen. sled-run.ps1 jo zajame v dveh krakih (stikala Rw* iz configa =
// original, in privzete vrednosti kode) in zahteva enakost znak za znakom.
//
// Faza 1 (svez NPC iz clone): ob init timerji 1 (20, ponavljajoc), 2 (50, enkraten) in
// 3 (7, ponavljajoc); iz callbackov reset, stop, forceStart in start na zasedenem ID
// (pricakovana izjema). tick pride vsakih 10 tickov, collide s SLED_Sosed vsake 4 ticke.
// Na koncu faze 1 ostane timer 7 (100, ponavljajoc), ki gre cez restart.
//
// Faza 2 (po restartu; storeddata sledFaza): has(7), tick in collide do starosti 200 od init in
// razmiki med prozenji timerja 7. Prvo prozenje je odvisno od trenutka shranjevanja, zato
// v sled gredo samo razmiki. Original zapise TimerTicks = ID (B3, PLAN_IMPLEMENTACIJE pogl. 4),
// zato je razmik po restartu 8 tickov, ne 101; bugfix B3 se ni narejen, zato mora sled
// napako pokazati v obeh krakih enako.
//
// Nashorn na Javi 8: brez let, brez puscicnih funkcij, brez sablonskih nizov.

var KONEC_FAZE2 = 200;
var DOLZINA_VRSTICE = 160;

var faza = 0;
var sled = [];
var n1 = 0;
var konec = false;
var zadnji7 = -1;
var zacetek = 0;
var razmiki = [];

function say(npc, m) { npc.executeCommand('/say ' + m); }

function starost(npc) { return npc.getAge() - zacetek; }

function zapis(npc, oznaka) { sled.push(oznaka + '@' + starost(npc)); }

function koncaj(npc) {
    konec = true;
    var vrstica = '';
    var del = 0;
    for (var i = 0; i < sled.length; i++) {
        vrstica += (vrstica === '' ? '' : ' ') + sled[i];
        if (vrstica.length >= DOLZINA_VRSTICE || i === sled.length - 1) {
            say(npc, 'SLED-D faza=' + faza + ' del=' + del + ' ' + vrstica);
            del++;
            vrstica = '';
        }
    }
    say(npc, 'SLED-KONEC faza=' + faza + ' dogodkov=' + sled.length + ' delov=' + del
        + ' razmiki7=' + (razmiki.length > 0 ? razmiki.join(',') : '-'));
}

function init(e) {
    var npc = e.npc;
    if (faza !== 0) {
        if (!konec) { zapis(npc, 'I'); }
        return;
    }
    var sd = npc.getStoreddata();
    faza = sd.has('sledFaza') ? 2 : 1;
    sd.put('sledFaza', 1);
    say(npc, 'SLED-INIT faza=' + faza);
    var t = npc.getTimers();
    if (faza === 1) {
        zapis(npc, 'I');
        t.start(1, 20, true);
        t.start(2, 50, false);
        t.start(3, 7, true);
    } else {
        zacetek = npc.getAge();
        // init v fazi 2 lahko sprozi prvi timer 7, katerega trenutek ni dolocen: brez starosti
        sled.push('I');
        sled.push('H7=' + t.has(7));
    }
}

function tick(e) {
    if (konec) { return; }
    zapis(e.npc, 'K');
    if (faza === 2 && starost(e.npc) >= KONEC_FAZE2) { koncaj(e.npc); }
}

function collide(e) {
    if (konec) { return; }
    zapis(e.npc, 'C' + e.entity.getName());
}

function timer(e) {
    var npc = e.npc;
    var id = e.id;
    var t = npc.getTimers();
    if (konec) { return; }
    if (faza === 2) {
        if (id !== 7) { sled.push('T' + id); return; }
        var s = starost(npc);
        if (zadnji7 >= 0) { razmiki.push(s - zadnji7); }
        zadnji7 = s;
        return;
    }
    zapis(npc, 'T' + id);
    if (id === 1) {
        n1++;
        if (n1 === 2) {
            t.reset(3);
            zapis(npc, 'R3');
        }
        if (n1 === 4) {
            zapis(npc, 'S3=' + t.stop(3));
            zapis(npc, 'S3b=' + t.stop(3));
        }
        if (n1 === 7) {
            t.stop(1);
            t.start(7, 100, true);
            zapis(npc, 'H1=' + t.has(1) + ',H7=' + t.has(7));
            koncaj(npc);
        }
    } else if (id === 2) {
        try {
            t.start(1, 5, true);
            zapis(npc, 'X=ne');
        } catch (ex) {
            zapis(npc, 'X=da');
        }
        t.forceStart(4, 15, false);
        zapis(npc, 'H2=' + t.has(2));
    } else if (id === 4) {
        t.forceStart(1, 30, true);
        zapis(npc, 'F1');
    }
}
