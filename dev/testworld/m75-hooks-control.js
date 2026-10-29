// Disposable M7.5 controller fixture. Production hooks run on the Forge world tick END observer.
var m75probe = null;
function tick(e) {
    if (e.npc.getTempdata().has('m75')) { return; }
    e.npc.getTempdata().put('m75', 1);
    try {
        var Probe = Java.type('noppes.npcs.rework.script.hooks.AnimationHooksProbe');
        m75probe = new Probe(e.npc, @@HOOKS@@, @@OUTPUT@@);
        e.npc.getTimers().start(7501, 0, true);
    } catch (err) { Java.type('java.lang.System').out.println('M75 FAILED ' + err); }
}
function timer(e) {
    if (e.id !== 7501 || m75probe === null) { return; }
    try { if (m75probe.step()) { e.npc.getTimers().stop(7501); } }
    catch (err) { Java.type('java.lang.System').out.println('M75 FAILED ' + err); }
}
