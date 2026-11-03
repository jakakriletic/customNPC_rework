// M11.3 fixture only. No entity is moved by the harness after spawn; commands drive real Squad tasks.
var A = Java.type('noppes.npcs.rework.army.ArmyApi');
var S = Java.type('noppes.npcs.CustomNpcs');
var Log = Java.type('noppes.npcs.LogWriter');
var RCon = Java.type('net.minecraft.network.rcon.RConConsoleSource');
var Wrapper = Java.type('net.minecraft.command.CommandSenderWrapper');
var Fake = Java.type('net.minecraftforge.common.util.FakePlayerFactory');
var Profile = Java.type('com.mojang.authlib.GameProfile');
var Uuid = Java.type('java.util.UUID');
var owner = '00000000-0000-4000-8000-0000000000e1';
var guest = '00000000-0000-4000-8000-0000000000e2';
var age = 0, checks = 0, failed = 0, done = false, rcon = null, watch = false;
function check(key, ok, detail) {
    checks++; if (!ok) { failed++; }
    Log.info('ARMYCOMP CHECK i=' + checks + ' key=' + key + ' ok=' + (ok ? 1 : 0) + ' detail=' + detail);
}
function sender(id) {
    var server = S.Server;
    return Wrapper.create(rcon).withEntity(Fake.get(server.getWorld(0), new Profile(Uuid.fromString(id),
        id === owner ? 'M113_Owner' : 'M113_Guest')), rcon.getPositionVector()).withPermissionLevel(-1);
}
function run(actor, command) {
    rcon.resetLog();
    S.Server.getCommandManager().executeCommand(actor, command);
    return String(rcon.getLogContents());
}
function expect(actor, command, key, text) {
    var reply = run(actor, command); check(key, reply.indexOf(text) >= 0, reply);
}
function finish(npc, phase) {
    done = true; npc.getStoreddata().put('m113done', 1);
    Log.info('ARMYCOMP DONE phase=' + phase + ' checks=' + checks + ' failed=' + failed);
}
function setup(npc) {
    check('create', A.create('north', 'uuid:' + owner), 'army');
    var all = npc.getWorld().getAllEntities(2), n = 0;
    for (var i = 0; i < all.length; i++) {
        if (String(all[i].getName()) !== 'COMP_M') { continue; }
        check('assign-' + n, A.assign(all[i], 'north'), String(all[i].getUUID()));
        all[i].getStoreddata().put('m113x', all[i].getX());
        all[i].getStoreddata().put('m113z', all[i].getZ()); n++;
    }
    check('fixtures-32', n === 32 && A.members('north').length === 32, n);
    var o = sender(owner), g = sender(guest);
    expect(g, 'rwarmy move north 0 4 24', 'invisible-move', 'unknown army north');
    expect(g, 'rwarmy stop north', 'invisible-stop', 'unknown army north');
    expect(g, 'rwarmy status north', 'invisible-status', 'unknown army north');
    expect(o, 'rwarmy grant uuid:' + guest + ' view north', 'view-grant', 'granted view north');
    expect(g, 'rwarmy move north 0 4 24', 'view-no-move', 'no command permission');
    expect(g, 'rwarmy stop north', 'view-no-stop', 'no command permission');
    expect(g, 'rwarmy status north', 'view-status', 'state=NONE');
    expect(o, 'rwarmy grant uuid:' + guest + ' command north', 'command-grant', 'granted command north');
    expect(g, 'rwarmy move north 0 4 24', 'command-move', 'RWARMY move north');
    expect(g, 'rwarmy stop north', 'command-stop', 'cancelled north');
    expect(g, 'rwarmy status north', 'cancel-state', 'state=CANCELLED');
    expect(o, 'rwarmy revoke uuid:' + guest + ' command north', 'command-revoke', 'revoked command north');
    expect(g, 'rwarmy move north 0 4 24', 'revoked-no-move', 'no command permission');
    expect(g, 'rwarmy stop north', 'revoked-no-stop', 'no command permission');
    check('nan-refused', !A.move('north', 0, NaN, 4, 24, 'legion', 3), 'NaN');
    check('unsupported-mode', !A.move('north', 0, 0, 4, 24, 'swarm', 3), 'M11.4');
    check('bad-dimension', !A.move('north', 123456, 0, 4, 24, 'legion', 3), 'not loaded');
    expect(o, 'rwarmy move north 0 4 24 legion 3', 'owner-start', 'company=8');
    finish(npc, 'setup');
    watch = true;
}
function resume(npc) {
    var status = String(A.status('north'));
    if (status.indexOf('state=BLOCKED') >= 0 || age >= 2500) {
        check('arrival', false, status); finish(npc, 'resume'); return;
    }
    if (status.indexOf('state=ARRIVED') < 0) { return; }
    check('arrival', true, status);
    check('four-companies', status.indexOf('companies=4 members=32 paths=1') >= 0, status);
    var members = A.members('north'), positions = [], minMove = 1e9, maxGoal = 0;
    check('restart-members', members.length === 32, members.length);
    for (var i = 0; i < members.length; i++) {
        var m = members[i], d = m.getStoreddata();
        check('saved-origin-' + i, d.has('m113x') && d.has('m113z'), m.getUUID());
        var moved = Math.sqrt(Math.pow(m.getX() - Number(d.get('m113x')), 2)
            + Math.pow(m.getZ() - Number(d.get('m113z')), 2));
        var toGoal = Math.sqrt(m.getX() * m.getX() + Math.pow(m.getZ() - 24, 2));
        minMove = Math.min(minMove, moved); maxGoal = Math.max(maxGoal, toGoal);
        positions.push([m.getX(), m.getY(), m.getZ()]);
        Log.info('ARMYCOMP POSITION uuid=' + m.getUUID() + ' x=' + m.getX() + ' y=' + m.getY()
            + ' z=' + m.getZ() + ' moved=' + moved + ' goal=' + toGoal);
    }
    check('all-32-near-goal', maxGoal <= 12, maxGoal);
    check('all-walked', minMove >= 10, minMove);
    var minPair = 1e9;
    for (var a = 0; a < positions.length; a++) {
        for (var b = a + 1; b < positions.length; b++) {
            minPair = Math.min(minPair, Math.sqrt(Math.pow(positions[a][0] - positions[b][0], 2)
                + Math.pow(positions[a][2] - positions[b][2], 2)));
        }
    }
    check('distinct-slots', minPair >= 1, minPair);
    // One final move exercises an explicit stop, and cancellation is the durable intent after another restart.
    check('api-move', A.move('north', 0, 0, 4, 16, 'column', 2), 'trusted API');
    check('api-stop', A.stop('north'), A.status('north'));
    check('api-cancel-state', String(A.status('north')).indexOf('state=CANCELLED') >= 0, A.status('north'));
    finish(npc, 'resume');
}
function tick(e) {
    if (watch) {
        var status = String(A.status('north')), members = A.members('north');
        if (status.indexOf('state=MOVING') >= 0) {
            for (var i = 0; i < members.length; i++) {
                var m = members[i], d = m.getStoreddata();
                var moved = Math.sqrt(Math.pow(m.getX() - Number(d.get('m113x')), 2)
                    + Math.pow(m.getZ() - Number(d.get('m113z')), 2));
                if (Number(d.get('m113x')) >= -10 && moved >= 1.5) {
                    Log.info('ARMYCOMP MID main-member-walked=' + moved + ' ' + status); watch = false; break;
                }
            }
        }
        return;
    }
    if (done || e.npc.getStoreddata().has('m113done') || !A.enabled()) { return; }
    age += 10; if (age < 40) { return; }
    if (rcon === null) { rcon = new RCon(S.Server); }
    if (String(e.npc.getName()) === 'COMP_Resume') { resume(e.npc); }
    else { setup(e.npc); }
}
