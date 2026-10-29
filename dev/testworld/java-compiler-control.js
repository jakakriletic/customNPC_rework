// M6.1 disposable fixture only. Existing original ECMAScript handler drives this trace.
var ticks = 0, initialized = 0, finished = false;
function init(e) { initialized++; }
function tick(e) {
    if (finished) { return; }
    ticks++;
    if (ticks < 3) { return; }
    finished = true;
    var System = Java.type('java.lang.System');
    try {
        if (initialized !== 1 || ticks !== 3) { throw new Error('JS golden trace'); }
        System.out.println('M61 JS init=' + initialized + ' ticks=' + ticks);
        var Probe = Java.type('noppes.npcs.rework.script.JavaCompilerProbe');
        var ok = Probe.run(e.npc, @@CLASSPATH@@, @@OUTPUT@@, @@MODE@@);
        System.out.println('M61 DONE success=' + ok + ' mode=' + @@MODE@@);
    } catch (error) {
        System.out.println('M61 FAILED ' + error);
        error.printStackTrace && error.printStackTrace();
    }
}
