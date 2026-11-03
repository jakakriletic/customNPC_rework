// M11.4 evidence only: never teleports workers, drives them through the public command/API.
var A = Java.type('noppes.npcs.rework.army.ArmyApi');
var Service = Java.type('noppes.npcs.rework.army.ArmyCommandService');
var Actor = Java.type('noppes.npcs.rework.army.ArmyActor');
var S = Java.type('noppes.npcs.CustomNpcs');
var Log = Java.type('noppes.npcs.LogWriter');
// spawn-animals=false removes cows alive. A golem is not an EntityAnimal and
// survives the unchanged scene settings; NoAI prevents it retaliating or moving.
var Target = Java.type('net.minecraft.entity.monster.EntityIronGolem');
var UUID = Java.type('java.util.UUID');
var RCon = Java.type('net.minecraft.network.rcon.RConConsoleSource');
var owner = '00000000-0000-4000-8000-0000000000f1';
var guest = UUID.fromString('00000000-0000-4000-8000-0000000000f2');
var viewer = Actor.player(guest, false);
var age = 0, checks = 0, failed = 0, done = false, phase = '', phaseAge = 0;
var dummy = null, fighter = null, hit = false, left = false, fightX = 0, fightZ = 0;
var passiveSamples = 0;
function check(key, ok, detail) {
    checks++; if (!ok) { failed++; }
    Log.info('ARMYSWARM CHECK i=' + checks + ' key=' + key + ' ok=' + (ok ? 1 : 0) + ' detail=' + detail);
}
function status() { return String(A.status('swarm')); }
function console(npc, text) {
    var rcon = new RCon(S.Server);
    S.Server.getCommandManager().executeCommand(rcon, text);
    return String(rcon.getLogContents());
}
function finish(npc, name) {
    done = true; npc.getStoreddata().put('m114done', 1);
    Log.info('ARMYSWARM DONE phase=' + name + ' checks=' + checks + ' failed=' + failed);
}
function makeDummy(npc, x, z) {
    var e = new Target(npc.getMCEntity().world);
    e.setPosition(x, 4, z); e.setNoAI(true); e.enablePersistence();
    check('dummy-spawn', npc.getMCEntity().world.spawnEntity(e), x + ',' + z);
    return e;
}
function setup(npc) {
    check('create', A.create('swarm', 'uuid:' + owner), 'army');
    var entities = npc.getWorld().getAllEntities(2), count = 0;
    for (var i = 0; i < entities.length; i++) {
        var e = entities[i]; if (String(e.getName()) !== 'SWARM_M') { continue; }
        check('assign-' + count, A.assign(e, 'swarm'), e.getUUID());
        e.getStoreddata().put('m114x', e.getX()); e.getStoreddata().put('m114z', e.getZ()); count++;
    }
    check('eight-workers', count === 8, count);
    check('invisible-stance', String(Service.engagement(S.Server, viewer, 'swarm', 'passive').message)
        .indexOf('unknown army') >= 0, 'server authority');
    check('grant-view', A.grant('uuid:' + guest, 'view', 'swarm'), 'view');
    check('view-no-stance', !Service.engagement(S.Server, viewer, 'swarm', 'passive').ok, 'view only');
    check('view-no-swarm', !Service.submitMove(S.Server, viewer, 'swarm', 0, 0, 4, 24, 'swarm', 3, 'passive').ok, 'view only');
    check('passive-start', A.move('swarm', 0, 0, 4, 24, 'swarm', 3, 'passive'), status());
    check('console-swarm-passive', console(npc, 'rwarmy move swarm 0 4 24 swarm 3 passive')
        .indexOf('mode=SWARM stance=PASSIVE') >= 0, status());
    check('console-stance', console(npc, 'rwarmy stance swarm engage').indexOf('stance swarm ENGAGE') >= 0, status());
    check('console-passive', console(npc, 'rwarmy stance swarm passive').indexOf('stance swarm PASSIVE') >= 0, status());
    var saved = status();
    check('bad-stance-refused', !A.move('swarm', 0, 0, 4, 24, 'swarm', 3, 'typo'), status());
    check('bad-stance-unchanged', status() === saved, status());
    check('grant-command', A.grant('uuid:' + guest, 'command', 'swarm'), 'command');
    check('command-stance', Service.engagement(S.Server, viewer, 'swarm', 'engage').ok, status());
    check('revoke-command', A.revoke('uuid:' + guest, 'command', 'swarm'), 'revoke');
    check('revoked-no-stance', !Service.engagement(S.Server, viewer, 'swarm', 'passive').ok, status());
    check('owner-passive', A.engagement('swarm', 'passive'), status());
    phase = 'mid'; finish(npc, 'setup'); done = false;
}
function verifyPositions(label, z, minMove, radius, separated) {
    var m = A.members('swarm'), maxGoal = 0, leastMove = 1e9, minPair = 1e9;
    check(label + '-eight', m.length === 8, m.length);
    for (var i = 0; i < m.length; i++) {
        var n = m[i], d = n.getStoreddata();
        var goal = Math.sqrt(n.getX() * n.getX() + Math.pow(n.getZ() - z, 2));
        var moved = Math.sqrt(Math.pow(n.getX() - Number(d.get('m114x')), 2) + Math.pow(n.getZ() - Number(d.get('m114z')), 2));
        check(label + '-height-' + i, Math.abs(n.getY() - 4) < 0.2, n.getY());
        maxGoal = Math.max(maxGoal, goal); leastMove = Math.min(leastMove, moved);
        for (var j = 0; j < i; j++) {
            minPair = Math.min(minPair, Math.sqrt(Math.pow(n.getX() - m[j].getX(), 2) + Math.pow(n.getZ() - m[j].getZ(), 2)));
        }
        Log.info('ARMYSWARM POSITION phase=' + label + ' uuid=' + n.getUUID() + ' x=' + n.getX()
            + ' y=' + n.getY() + ' z=' + n.getZ() + ' goal=' + goal + ' moved=' + moved);
    }
    check(label + '-near-goal', maxGoal <= radius, maxGoal);
    check(label + '-walked', leastMove >= minMove, leastMove);
    if (separated) { check(label + '-separated', minPair >= 1.3, minPair); }
}
function tick(e) {
    var resuming = e.npc.getStoreddata().has('m114resume');
    if (!A.enabled() || done || (e.npc.getStoreddata().has('m114done') && !resuming && phase !== 'mid')) { return; }
    age += 10; phaseAge += 10; if (!resuming && age < 40) { return; }
    if (phase === '') {
        if (!resuming) { setup(e.npc); return; }
        check('resume-mode-stance', status().indexOf('mode=SWARM stance=PASSIVE') >= 0, status());
        check('resume-not-terminal', status().indexOf('state=ARRIVED') < 0, status());
        phase = 'passive'; phaseAge = 0; dummy = makeDummy(e.npc, 6, 12);
    }
    if (phase === 'mid') {
        var m = A.members('swarm');
        for (var i = 0; i < m.length; i++) {
            if (Math.abs(m[i].getZ() - Number(m[i].getStoreddata().get('m114z'))) >= 1.5) {
                check('mid-moving', status().indexOf('state=MOVING') >= 0, status());
                // Reuse this persisted controller on the first script tick of the
                // next server. Console spawn + script warmup can miss a short march.
                e.npc.getStoreddata().put('m114resume', 1);
                Log.info('ARMYSWARM MID restart ' + status()); done = true; return;
            }
        }
        return;
    }
    if (status().indexOf('state=BLOCKED') >= 0 || phaseAge >= 2000) {
        check('no-block-or-timeout', false, phase + ' ' + status()); finish(e.npc, 'resume'); return;
    }
    if (phase === 'passive') {
        var members = A.members('swarm');
        if (status().indexOf('state=ARRIVED') < 0) {
            passiveSamples++;
            check('passive-target-alive-' + age, dummy.isEntityAlive(), dummy.getHealth());
            for (var j = 0; j < members.length; j++) {
                var mc = members[j].getMCEntity(); mc.setAttackTarget(dummy); mc.setRevengeTarget(dummy);
                check('passive-targets-' + age + '-' + j, mc.getAttackTarget() === null && mc.getRevengeTarget() === null, mc.getUniqueID());
            }
            check('passive-no-damage-' + age, dummy.getHealth() === dummy.getMaxHealth(), dummy.getHealth());
            return;
        }
        check('passive-observed-during-march', passiveSamples > 0, passiveSamples);
        check('passive-finished-undamaged', dummy.getHealth() === dummy.getMaxHealth(), dummy.getHealth());
        dummy.setDead(); verifyPositions('passive', 24, 12, 10, true);
        check('engage-formation-start', A.move('swarm', 0, 0, 4, 0, 'legion', 3, 'engage'), status());
        phase = 'engage-start'; phaseAge = 0; return;
    }
    if (phase === 'engage-start' && phaseAge >= 30 && status().indexOf('state=MOVING') >= 0) {
        var all = A.members('swarm'); fighter = all[0].getMCEntity();
        fightX = fighter.posX; fightZ = fighter.posZ;
        dummy = makeDummy(e.npc, fighter.posX + 6, fighter.posZ);
        fighter.setAttackTarget(dummy); fighter.setRevengeTarget(dummy);
        check('engage-target-accepted', fighter.getAttackTarget() === dummy, status());
        phase = 'fight'; phaseAge = 0; return;
    }
    if (phase === 'fight') {
        var moved = Math.sqrt(Math.pow(fighter.posX - fightX, 2) + Math.pow(fighter.posZ - fightZ, 2));
        if (moved >= 2) { left = true; }
        if (dummy.getHealth() < dummy.getMaxHealth()) { hit = true; }
        if (phaseAge % 100 === 0) {
            Log.info('ARMYSWARM FIGHT health=' + dummy.getHealth() + ' dead=' + dummy.isDead
                + ' walked=' + moved + ' attacking=' + fighter.isAttacking() + ' ' + status());
        }
        if (dummy.isEntityAlive()) {
            check('not-arrived-during-fight-' + age, status().indexOf('state=ARRIVED') < 0, status());
            return;
        }
        check('real-melee-damage', hit && dummy.getHealth() <= 0, dummy.getHealth());
        check('fighter-left-company', left, moved);
        Log.info('ARMYSWARM COMBAT killed=' + (hit && dummy.getHealth() <= 0) + ' walked=' + moved);
        phase = 'return'; phaseAge = 0; return;
    }
    if (phase === 'return' && phaseAge % 100 === 0) {
        Log.info('ARMYSWARM RETURN x=' + fighter.posX + ' z=' + fighter.posZ
            + ' attacking=' + fighter.isAttacking() + ' ' + status());
        Log.info('ARMYSWARM SQUAD ' + console(e.npc, 'rwsquad status'));
    }
    if (phase === 'return' && status().indexOf('state=ARRIVED') >= 0) {
        verifyPositions('return', 0, 0, 8, true);
        check('fighter-target-gone', fighter.getAttackTarget() === null || !fighter.getAttackTarget().isEntityAlive(), status());
        check('fighter-back', Math.sqrt(fighter.posX * fighter.posX + fighter.posZ * fighter.posZ) <= 8, fighter.posX + ',' + fighter.posZ);
        check('api-swarm-again', A.move('swarm', 0, 0, 4, 12, 'swarm', 3, 'passive'), status());
        phase = 'cancel'; phaseAge = 0; return;
    }
    if (phase === 'cancel' && phaseAge >= 20) {
        check('stop-passive', A.stop('swarm'), status());
        var worker = A.members('swarm')[0].getMCEntity();
        dummy = makeDummy(e.npc, worker.posX + 1, worker.posZ);
        worker.setAttackTarget(dummy); worker.setRevengeTarget(dummy);
        check('stop-releases-passive', worker.getAttackTarget() === dummy && worker.getRevengeTarget() === dummy, status());
        worker.setAttackTarget(null); worker.setRevengeTarget(null); dummy.setDead();
        e.npc.getStoreddata().remove('m114resume');
        check('durable-cancel', status().indexOf('state=CANCELLED') >= 0, status()); finish(e.npc, 'resume');
    }
}
