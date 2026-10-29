// Disposable M7.9 controller fixture. Production queue ticks run independently on the Forge bus.
var m79probe = null;
function tick(e) {
    if (e.npc.getTempdata().has('m79')) { return; }
    e.npc.getTempdata().put('m79', 1);
    try {
        var Probe = Java.type('noppes.npcs.rework.script.hooks.AnimationApiProbe');
        m79probe = new Probe(e.npc, @@ENTITIES@@, @@OUTPUT@@);
        e.npc.getTimers().start(7901, 0, true);
    } catch (err) { Java.type('java.lang.System').out.println('M79 FAILED ' + err); }
}
function timer(e) {
    if (e.id !== 7901 || m79probe === null) { return; }
    try { if (m79probe.step()) { e.npc.getTimers().stop(7901); } }
    catch (err) { Java.type('java.lang.System').out.println('M79 FAILED ' + err); }
}
