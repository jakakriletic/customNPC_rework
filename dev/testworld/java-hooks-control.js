// M6.2 disposable fixture only. Runs the hook trace probe once; the tempdata guard survives
// the script reload the probe triggers (a JS var would be reset by the re-evaluation).
function tick(e) {
    var d = e.npc.getTempdata();
    if (d.has('m62')) { return; }
    d.put('m62', 1);
    var System = Java.type('java.lang.System');
    try {
        var Probe = Java.type('noppes.npcs.rework.script.hooks.HookTraceProbe');
        var ok = Probe.run(e.npc, @@OUTPUT@@, @@MODE@@);
        System.out.println('M62 DONE success=' + ok + ' mode=' + @@MODE@@);
    } catch (error) {
        System.out.println('M62 FAILED ' + error);
    }
}
