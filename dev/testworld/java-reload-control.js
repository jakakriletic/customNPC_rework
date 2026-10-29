// M6.4 disposable fixture only. Runs the reload/cache probe once; the tempdata guard survives
// the script reload the probe triggers (a JS var would be reset by the re-evaluation).
function tick(e) {
    var d = e.npc.getTempdata();
    if (d.has('m64')) { return; }
    d.put('m64', 1);
    var System = Java.type('java.lang.System');
    try {
        var Probe = Java.type('noppes.npcs.rework.script.hooks.ReloadProbe');
        var ok = Probe.run(e.npc, @@OUTPUT@@, @@MODE@@);
        System.out.println('M64 DONE success=' + ok + ' mode=' + @@MODE@@);
    } catch (error) {
        System.out.println('M64 FAILED ' + error);
    }
}
