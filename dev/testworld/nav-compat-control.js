// M4.15a: installed only in a disposable world; six real NPCs, never in a cost window.
var age = 0;
var actors = {};
var starts = {};
var Nav = Java.type('noppes.npcs.rework.nav.RwNavBackend');

function sample(npc, phase) {
    var count = 0;
    for (var name in actors) {
        var a = actors[name], mc = a.getMCEntity();
        var dx = a.getX() - (starts[name].x + 8), dz = a.getZ() - starts[name].z;
        npc.executeCommand('/say NAVC phase=' + phase + ' name=' + name
            + ' selected=' + mc.getRwNavBackend() + ' attached=' + (Nav.attached(mc) ? 1 : 0)
            + ' movement=' + a.getAi().getNavigationType() + ' riding=' + (mc.isRiding() ? 1 : 0)
            + ' carrying=' + (mc.isBeingRidden() ? 1 : 0) + ' width=' + Number(mc.width).toFixed(3)
            + ' height=' + Number(mc.height).toFixed(3) + ' distance=' + Math.sqrt(dx * dx + dz * dz).toFixed(3));
        count++;
    }
    return count;
}

function tick(e) {
    age += 10; // CNPC script tick runs every 10 server ticks.
    if (age === 10) {
        var all = e.npc.getWorld().getAllEntities(2);
        for (var i = 0; i < all.length; i++) {
            var name = String(all[i].getName());
            if (name.indexOf('NAVC_') === 0 && name !== 'NAVC_Control') {
                var key = name.substring(5);
                actors[key] = all[i];
                starts[key] = { x: all[i].getX(), z: all[i].getZ() };
            }
        }
        actors.Rider.setMount(actors.Carrier);
    }
    if (age === 60) {
        sample(e.npc, 'mounted');
        actors.Normal.navigateTo(starts.Normal.x + 8, 4, starts.Normal.z, 1);
        actors.Large.navigateTo(starts.Large.x + 8, 4, starts.Large.z, 1);
    }
    if (age === 240) { actors.Rider.setMount(null); }
    if (age === 280) {
        sample(e.npc, 'dismounted');
        for (var key in actors) { actors[key].getMCEntity().updateAI = true; }
    }
    if (age === 320) {
        var count = sample(e.npc, 'rebuilt');
        e.npc.executeCommand('/say NAVC DONE count=' + count);
        e.npc.despawn();
    }
}
