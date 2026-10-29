// Harness contract only: mocked entities do not demonstrate Minecraft navigation.
'use strict';
var fs = require('fs'), vm = require('vm');
var clone = fs.readFileSync(process.argv[2], 'utf8');
var field = clone.match(/"Script"\s*:\s*"(?:\\[\s\S]|[^"\\])*"/g);
if (!field || field.length !== 1) { throw new Error('Expected one installed Script'); }
// Match NbtJson.readQuoted (NbtJson.java:324-342). JSON.parse hid the bad
// generator: Minecraft decoded \n to n, so the initial // swallowed every hook.
function readCloneString(quoted) {
    return quoted.substring(1, quoted.length - 1).replace(/\\([\s\S])/g, '$1');
}
var expected = fs.readFileSync(process.argv[3], 'utf8');
var source = readCloneString(field[0].replace(/^"Script"\s*:\s*/, ''));
if (source !== expected) { throw new Error('Installed Script differs after actual clone-format decoding'); }
if (readCloneString('"\\n\\u0027"') !== 'nu0027' || readCloneString('"a\nb"') !== 'a\nb') {
    throw new Error('Clone decoder must preserve raw newlines and consume legacy escapes literally');
}
var legacy = readCloneString(JSON.stringify(expected));
var legacyContext = {};
vm.createContext(legacyContext);
vm.runInContext(legacy, legacyContext);
if (typeof legacyContext.tick !== 'undefined') { throw new Error('Missing reproduction of JSON-escaped comment swallowing hooks'); }
var names = ['Normal', 'Large', 'Fly', 'Swim', 'Rider', 'Carrier'];
var actors = names.map(function (name, i) {
    var mc = {
        // EntityCustomNpc.updateHitbox: default model scales are 1, bodyY=0,
        // baseHeight=1.9; EntityNPCInterface scales width/height by Size/5.
        width: name === 'Large' ? 1.2 : 0.6, height: 1.9 * (name === 'Large' ? 2 : 1),
        getRwNavBackend: function () { return 1; },
        isRiding: function () { return !!this.mount; },
        isBeingRidden: function () { return !!this.rider; }
    };
    return {
        name: name, x: 0.5, z: i * 8 + 0.5, mc: mc,
        getName: function () { return 'NAVC_' + name; },
        getX: function () { return this.x; }, getZ: function () { return this.z; },
        getMCEntity: function () { return mc; },
        getAi: function () { return { getNavigationType: function () { return name === 'Fly' ? 1 : name === 'Swim' ? 2 : 0; } }; },
        setMount: function (carrier) {
            if (mc.mount) { mc.mount.rider = null; }
            mc.mount = carrier ? carrier.mc : null;
            if (carrier) {
                carrier.mc.rider = mc;
                // startRiding refreshes the hitbox; original mode 0 dismount
                // does not refresh it (D-019), nor does updateTasks.
                mc.height = 1.9 * 0.77;
            }
        },
        navigateTo: function (x, y, z) { this.x = x; this.z = z; }
    };
});
var lines = [], commands = 0, despawned = false;
var controller = {
    getWorld: function () { return { getAllEntities: function () { return actors; } }; },
    executeCommand: function (line) { lines.push(line); },
    despawn: function () { despawned = true; }
};
var context = {
    Java: { type: function (name) {
        if (name !== 'noppes.npcs.rework.nav.RwNavBackend') { throw new Error('Unexpected Java linkage'); }
        return { attached: function (mc) {
            var a = actors.filter(function (a) { return a.mc === mc; })[0];
            return a.getAi().getNavigationType() === 0 && mc.width <= 1 && mc.height <= 2 && !mc.isRiding() && !mc.isBeingRidden();
        } };
    } }
};
vm.createContext(context);
vm.runInContext(source, context);
for (var tick = 0; tick < 32; tick++) { context.tick({ npc: controller }); }
if (!despawned || lines.length !== 19) { throw new Error('Missing or duplicate controller samples'); }
if (actors.filter(function (a) { return a.mc.updateAI === true; }).length !== 6) { throw new Error('Task rebuild never requested'); }
process.stdout.write(lines.join('\n') + '\n');
