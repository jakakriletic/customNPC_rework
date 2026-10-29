// M6.11 disposable fixture only. Runs the player/world probe once; the tempdata guard keeps
// it to one run.
function tick(e) {
    var d = e.npc.getTempdata();
    if (d.has('m611')) { return; }
    d.put('m611', 1);
    var System = Java.type('java.lang.System');
    try {
        var Probe = Java.type('noppes.npcs.rework.script.hooks.PlayerWorldProbe');
        var ok = Probe.run(e.npc, @@OUTPUT@@, @@MODE@@);
        System.out.println('M611 DONE success=' + ok + ' mode=' + @@MODE@@);
    } catch (error) {
        System.out.println('M611 FAILED ' + error);
    }
}
