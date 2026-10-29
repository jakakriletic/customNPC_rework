// M6.5 disposable fixture only. Runs the recovery/source map probe once; the tempdata guard survives
// the script reload the probe triggers (a JS var would be reset by the re-evaluation).
function tick(e) {
    var d = e.npc.getTempdata();
    if (d.has('m65')) { return; }
    d.put('m65', 1);
    var System = Java.type('java.lang.System');
    try {
        var Probe = Java.type('noppes.npcs.rework.script.hooks.RecoveryProbe');
        var ok = Probe.run(e.npc, @@OUTPUT@@, @@MODE@@, @@ORIGINAL@@);
        System.out.println('M65 DONE success=' + ok + ' mode=' + @@MODE@@);
    } catch (error) {
        System.out.println('M65 FAILED ' + error);
    }
}
