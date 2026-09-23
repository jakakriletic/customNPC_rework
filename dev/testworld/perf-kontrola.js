// M2.4 - krmilnik scenarija merilnih obremenitev. Scenarij: docs/scenariji/M2.4-obremenitve.md
//
// PERF_Kontrola .\perf-run.ps1 spawna dvakrat na celico: pred ogrevanjem in po posnetku.
// Ob prvem skriptnem ticku naredi vse naenkrat in se odstrani. Med merjenjem ga v svetu
// ni, zato njegovo delo (pregled vseh NPC-jev) ni del meritve.
//
// 1 Frakciji 1 (Neutral) in 2 (Aggressive) postavi za sovrazni druga drugi. Privzete
//   frakcije niso sovrazne nobeni (FactionController.java:46-48); NPC z AttackOtherFactions
//   napade samo frakcijo iz svojega attackFactions (NPCAttackSelector.java:75,
//   Faction.isAggressiveToNpc). Brez tega koraka bi celica 'boj' merila dve mirujoci skupini.
// 2 Presteje NPC-je po imenu in izpise PERF-KONTROLA. Stevec perfSkript v tempdata sveta
//   povecuje vsak PERF_Skripte ob svojem ticku; razlika med zacetnim in koncnim izpisom
//   dokaze, da so skripte tekle ves cas meritve.

var FRAKCIJA_A = 1;
var FRAKCIJA_B = 2;
var opravljeno = false;

function tick(e) {
    if (opravljeno) {
        return;
    }
    opravljeno = true;

    var fa = e.API.getFactions().get(FRAKCIJA_A);
    var fb = e.API.getFactions().get(FRAKCIJA_B);
    if (!fa.hasHostile(FRAKCIJA_B)) {
        fa.addHostile(FRAKCIJA_B);
        fa.save();
    }
    if (!fb.hasHostile(FRAKCIJA_A)) {
        fb.addHostile(FRAKCIJA_A);
        fb.save();
    }

    var idle = 0;
    var bojA = 0;
    var bojB = 0;
    var skripte = 0;
    var drugi = 0;
    var cilj = 0;
    var ranjenih = 0;
    var jaz = e.npc.getUUID();
    var vsi = e.npc.getWorld().getAllEntities(2);
    for (var i = 0; i < vsi.length; i++) {
        var n = vsi[i];
        if (n.getUUID() == jaz) {
            continue;
        }
        var ime = n.getName();
        if (ime == 'PERF_Idle') {
            idle++;
        } else if (ime == 'PERF_BojA') {
            bojA++;
        } else if (ime == 'PERF_BojB') {
            bojB++;
        } else if (ime == 'PERF_Skripte') {
            skripte++;
        } else {
            drugi++;
        }
        if (n.getAttackTarget() != null) {
            cilj++;
        }
        if (n.getHealth() < n.getMaxHealth()) {
            ranjenih++;
        }
    }
    var w = e.npc.getWorld().getTempdata();
    var skriptTickov = w.has('perfSkript') ? w.get('perfSkript') : 0;

    e.npc.executeCommand('/say PERF-FRAKCIJE a>b=' + fa.hasHostile(FRAKCIJA_B) + ' b>a=' + fb.hasHostile(FRAKCIJA_A));
    e.npc.executeCommand('/say PERF-KONTROLA idle=' + idle + ' bojA=' + bojA + ' bojB=' + bojB
        + ' skripte=' + skripte + ' drugi=' + drugi + ' cilj=' + cilj + ' ranjenih=' + ranjenih
        + ' skriptTickov=' + Math.round(skriptTickov));
    e.npc.despawn();
}
