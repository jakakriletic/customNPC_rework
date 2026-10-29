// M6.3 disposable fixture only. Runs the typed script probe once; the tempdata guard survives
// the script reload the probe triggers (a JS var would be reset by the re-evaluation).
function tick(e) {
    var d = e.npc.getTempdata();
    if (d.has('m63')) { return; }
    d.put('m63', 1);
    var System = Java.type('java.lang.System');
    try {
        var Probe = Java.type('noppes.npcs.rework.script.hooks.TypedScriptProbe');
        var ok = Probe.run(e.npc, @@OUTPUT@@, @@MODE@@);
        System.out.println('M63 DONE success=' + ok + ' mode=' + @@MODE@@);
    } catch (error) {
        System.out.println('M63 FAILED ' + error);
    }
}
