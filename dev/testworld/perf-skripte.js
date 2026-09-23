// M2.4 - obremenitev 'skripte'. To skripto ima vsak PERF_Skripte NPC.
// Scenarij: docs/scenariji/M2.4-obremenitve.md
//
// Namen ni zanimiva skripta, ampak tipicna: branje polozaja, dva zapisa v tempdata NPC-ja
// in skupni stevec v tempdata sveta. tick se klice vsakih 10 server tickov
// (EntityNPCInterface.java:358-363), torej 2 klica na sekundo na NPC.
// Stevec perfSkript prebere PERF_Kontrola; iz razlike med zacetkom in koncem celice
// .\perf-run.ps1 preveri, da skripte niso utihnile (merilo P5).

var n = 0;

function tick(e) {
    n++;
    var npc = e.npc;
    var x = npc.getX();
    var z = npc.getZ();
    npc.getTempdata().put('n', n);
    npc.getTempdata().put('d', Math.sqrt(x * x + z * z));
    var w = npc.getWorld().getTempdata();
    w.put('perfSkript', (w.has('perfSkript') ? w.get('perfSkript') : 0) + 1);
}
