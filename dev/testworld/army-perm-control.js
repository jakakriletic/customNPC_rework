// M11.1b: rights of /rwarmy through the real command handler on a dedicated server, RwArmies=1. A sender is a
// CommandSenderWrapper over an RCon source (keeps the replies) with a FakePlayer as entity. Level -1 refuses every
// level as vanilla does for a player who is not an operator; level 2 is an operator. The NPC name picks the part.
// Installed only in a disposable test world; army-perm-run.ps1 checks the summary lines.
var A = Java.type('noppes.npcs.rework.army.ArmyApi');
var Log = Java.type('noppes.npcs.LogWriter');
var Server = Java.type('noppes.npcs.CustomNpcs');
var RCon = Java.type('net.minecraft.network.rcon.RConConsoleSource');
var Wrapper = Java.type('net.minecraft.command.CommandSenderWrapper');
var Fake = Java.type('net.minecraftforge.common.util.FakePlayerFactory');
var Profile = Java.type('com.mojang.authlib.GameProfile');
// Vec3d cannot be linked on a dedicated server: its public fromPitchYawVector names the client-only Vec2f. Only pass it on.
var Uuid = Java.type('java.util.UUID');

var U = {
    owner1: '00000000-0000-4000-8000-0000000000a1', owner2: '00000000-0000-4000-8000-0000000000a2',
    p1: '00000000-0000-4000-8000-0000000000b1', p2: '00000000-0000-4000-8000-0000000000b2',
    p3: '00000000-0000-4000-8000-0000000000b3', p4: '00000000-0000-4000-8000-0000000000b4',
    op: '00000000-0000-4000-8000-0000000000c1', absent: '00000000-0000-4000-8000-0000000000d1'
};
var age = 0, checks = 0, failed = 0, rcon, server, world;

function check(key, ok, detail) {
    checks++;
    if (!ok) { failed++; }
    Log.info('ARMYPERM CHECK i=' + checks + ' key=' + key + ' ok=' + (ok ? 1 : 0) + (ok ? '' : ' detail=' + detail));
}

function sender(name, level) {
    var fake = Fake.get(world, new Profile(Uuid.fromString(U[name]), 'M111B_' + name));
    return Wrapper.create(rcon).withEntity(fake, rcon.getPositionVector()).withPermissionLevel(level);
}

function run(actor, command) {
    rcon.resetLog();
    var ran = server.getCommandManager().executeCommand(actor, command);
    return ran + '|' + String(rcon.getLogContents());
}

// Runs the command and checks that the reply contains the text.
function expect(actor, command, key, text) {
    var reply = run(actor, command);
    check(key, reply.indexOf(text) >= 0, command + ' -> ' + reply);
    return reply;
}

function absent(actor, command, key, text) {
    var reply = run(actor, command);
    check(key, reply.indexOf(text) < 0, command + ' -> ' + reply);
}

function main() {
    var op = sender('op', 2), o1 = sender('owner1', -1), o2 = sender('owner2', -1);
    var p1 = sender('p1', -1), p2 = sender('p2', -1), p3 = sender('p3', -1), p4 = sender('p4', -1);

    // Setup by an operator who owns nothing; every NPC stands near the origin.
    expect(op, 'rwarmy create north uuid:' + U.owner1, 'op-create-north', 'RWARMY created north');
    expect(op, 'rwarmy create south uuid:' + U.owner1, 'op-create-south', 'RWARMY created south');
    expect(op, 'rwarmy create west uuid:' + U.owner2, 'op-create-west', 'RWARMY created west');
    expect(op, 'rwarmy create guard faction:0', 'op-create-faction', 'RWARMY created guard');
    expect(op, 'rwarmy create all faction:0', 'reserved-name', "invalid name 'all'");
    expect(op, 'rwarmy assign north PERM_N 50', 'op-assign-free', 'assigned north npcs=2 changed=2');
    expect(op, 'rwarmy assign south PERM_S 50', 'op-assign-south', 'assigned south npcs=1 changed=1');
    expect(op, 'rwarmy assign west PERM_W 50', 'op-assign-west', 'assigned west npcs=1 changed=1');

    // A raw FakePlayer fails canUseCommand at every level, as a real player who is not an operator does.
    var raw = Fake.get(world, new Profile(Uuid.fromString(U.p1), 'M111B_p1'));
    check('non-op-passes-handler', server.getCommandManager().executeCommand(raw, 'rwarmy list') === 1, 'raw');

    // Nothing without a right; an invisible army answers like a missing one.
    expect(p1, 'rwarmy list', 'none-list', 'RWARMY list armies=0');
    expect(p1, 'rwarmy info north', 'none-info', 'unknown army north');
    expect(p1, 'rwarmy info nosuch', 'none-info-missing', 'unknown army nosuch');
    expect(p1, 'rwarmy create mine faction:0', 'none-create', 'create needs an operator');
    expect(p1, 'rwarmy remove north', 'none-remove', 'unknown army north');
    expect(p1, 'rwarmy assign north PERM_F 50', 'none-assign', 'unknown army north');
    expect(p1, 'rwarmy unassign PERM_N 50', 'none-unassign', 'changed=0 refused=2');
    expect(p1, 'rwarmy grant M111B_Nobody view all', 'none-grant-all', 'unknown player');
    expect(p1, 'rwarmy grant uuid:' + U.p2 + ' view all', 'none-grant', 'only the owner or an operator');
    expect(p1, 'rwarmy players', 'none-players', 'RWARMY players');
    absent(p1, 'rwarmy players', 'none-players-empty', 'm111b_absent');
    expect(o1, 'rwarmy list', 'owner-list', 'RWARMY list armies=2');
    absent(o1, 'rwarmy list', 'owner-list-foreign', 'army west');
    expect(o1, 'rwarmy remove west', 'owner-remove-foreign', 'unknown army west');
    expect(o1, 'rwarmy list', 'owner-sees-command', 'access=command');
    expect(op, 'rwarmy list', 'op-list', 'RWARMY list armies=4');

    // Owner grants view of a selected army; no chain.
    expect(o1, 'rwarmy grant uuid:' + U.p1 + ' view north', 'grant-view', 'granted view north');
    expect(o1, 'rwarmy grant uuid:' + U.p1 + ' view north', 'grant-repeat', 'already granted view north');
    expect(p1, 'rwarmy list', 'view-list', 'RWARMY list armies=1');
    expect(p1, 'rwarmy list', 'view-access', 'access=view');
    expect(p1, 'rwarmy info north', 'view-info', 'RWARMY info north');
    expect(p1, 'rwarmy unassign PERM_N 50', 'view-no-command', 'changed=0 refused=2');
    expect(p1, 'rwarmy assign north PERM_F 50', 'view-no-assign', 'no command permission for north');
    expect(p1, 'rwarmy remove north', 'view-no-remove', 'only the owner or an operator can remove north');
    expect(p1, 'rwarmy grant uuid:' + U.p2 + ' view north', 'no-chain', 'only the owner or an operator can grant north');
    expect(o1, 'rwarmy grant uuid:' + U.p1 + ' view west', 'owner-grant-foreign', 'unknown army west');
    expect(o1, 'rwarmy grant M111B_Nobody view north', 'unknown-player', 'unknown player M111B_Nobody');
    expect(o1, 'rwarmy grant uuid:' + U.owner1 + ' view north', 'grant-self', 'cannot grant to yourself');
    expect(o2, 'rwarmy grant uuid:' + U.p1 + ' command north', 'owner2-grant-foreign', 'unknown army north');

    // A player who never joined, by a name from usercache; stored as UUID.
    expect(o1, 'rwarmy players m111b', 'players-candidates', 'm111b_absent');
    expect(o1, 'rwarmy grant m111b_ABSENT view south', 'grant-seen-player', 'granted view south to M111B_Absent scope=owner');
    check('seen-player-right', A.canView('uuid:' + U.absent, 'south') && !A.canCommand('uuid:' + U.absent, 'south'), 'absent');

    // Command includes view and moves NPCs only between commanded armies.
    expect(o1, 'rwarmy grant uuid:' + U.p1 + ' command south', 'grant-command', 'granted command south');
    expect(p1, 'rwarmy list', 'command-includes-view', 'RWARMY list armies=2');
    expect(p1, 'rwarmy unassign PERM_S 50', 'command-unassign', 'unassigned npcs=1 changed=1');
    expect(p1, 'rwarmy assign south PERM_S 50', 'command-free-npc', 'changed=0 refused=1');
    expect(p1, 'rwarmy assign south PERM_F 50', 'command-free-npc-2', 'changed=0 refused=1');
    expect(p1, 'rwarmy assign south PERM_W 50', 'command-foreign-npc', 'changed=0 refused=1');
    expect(p1, 'rwarmy assign south PERM_N 50', 'command-view-only-npc', 'changed=0 refused=2');
    expect(p1, 'rwarmy remove south', 'command-no-remove', 'only the owner or an operator can remove south');
    expect(p1, 'rwarmy grant uuid:' + U.p2 + ' command south', 'command-no-chain', 'only the owner or an operator can grant south');
    expect(op, 'rwarmy assign south PERM_S 50', 'op-assign-back', 'assigned south npcs=1 changed=1');

    // All of an owner, also later armies, never another owner's.
    expect(o1, 'rwarmy grant uuid:' + U.p2 + ' view all', 'grant-owner-all', 'granted view all to');
    expect(p2, 'rwarmy list', 'owner-all-list', 'RWARMY list armies=2');
    expect(op, 'rwarmy create east uuid:' + U.owner1, 'op-create-later', 'RWARMY created east');
    expect(p2, 'rwarmy list', 'owner-all-later', 'RWARMY list armies=3');
    absent(p2, 'rwarmy list', 'owner-all-foreign', 'army west');
    expect(p1, 'rwarmy list', 'selected-not-later', 'RWARMY list armies=2');

    // All of an operator: every army, faction armies and later ones too.
    expect(op, 'rwarmy grant uuid:' + U.p3 + ' command all', 'grant-server-all', 'scope=server');
    expect(p3, 'rwarmy list', 'server-all-list', 'RWARMY list armies=5');
    expect(op, 'rwarmy create later2 uuid:' + U.owner2, 'op-create-later2', 'RWARMY created later2');
    expect(p3, 'rwarmy list', 'server-all-later', 'RWARMY list armies=6');
    expect(p3, 'rwarmy assign west PERM_S 50', 'server-all-moves', 'assigned west npcs=1 changed=1');
    expect(p3, 'rwarmy assign south PERM_S 50', 'server-all-moves-back', 'assigned south npcs=1 changed=1');
    expect(p3, 'rwarmy assign west PERM_F 50', 'server-all-free-npc', 'changed=0 refused=1');
    expect(p3, 'rwarmy remove west', 'server-all-no-remove', 'only the owner or an operator can remove west');
    expect(p3, 'rwarmy grant uuid:' + U.p4 + ' view west', 'server-all-no-chain', 'only the owner or an operator can grant west');
    expect(op, 'rwarmy grant uuid:' + U.p4 + ' view guard', 'op-grant-faction', 'granted view guard');
    expect(p4, 'rwarmy info guard', 'faction-view', 'RWARMY info guard');
    expect(p4, 'rwarmy list', 'faction-only', 'RWARMY list armies=1');

    // Listing grants.
    expect(op, 'rwarmy grants', 'grants-op', 'grants count=5');
    expect(o1, 'rwarmy grants', 'grants-owner', 'grants count=3');
    expect(o2, 'rwarmy grants', 'grants-other-owner', 'grants count=0');
    expect(p1, 'rwarmy grants', 'grants-received', 'view=north command=south');
    expect(p4, 'rwarmy grants', 'grants-received-server', 'by server view=guard');
    expect(o1, 'rwarmy grants uuid:' + U.p2, 'grants-filter', 'grants count=1');

    // Revoke applies to the next command.
    expect(o1, 'rwarmy revoke uuid:' + U.p1 + ' command south', 'revoke-command', 'changed=1 left=none');
    expect(p1, 'rwarmy info south', 'revoked-invisible', 'unknown army south');
    expect(p1, 'rwarmy unassign PERM_S 50', 'revoked-no-command', 'changed=0 refused=1');
    expect(p1, 'rwarmy list', 'revoked-list', 'RWARMY list armies=1');
    expect(o1, 'rwarmy revoke uuid:' + U.p3 + ' command north', 'owner-revoke-keeps-server', 'changed=0 left=command');
    expect(p1, 'rwarmy revoke uuid:' + U.p2 + ' view all', 'player-cannot-revoke', 'only the owner or an operator can revoke');
    expect(p2, 'rwarmy list', 'still-granted', 'RWARMY list armies=3');
    expect(o2, 'rwarmy revoke uuid:' + U.p2 + ' all', 'other-owner-revokes-nothing', 'changed=0');
    expect(p2, 'rwarmy list', 'still-granted-2', 'RWARMY list armies=3');
    expect(op, 'rwarmy revoke uuid:' + U.p2 + ' all', 'op-revokes-owner-grant', 'changed=1');
    expect(p2, 'rwarmy list', 'op-revoke-applied', 'RWARMY list armies=0');

    // A removed army takes its selected grants along.
    expect(op, 'rwarmy create temp uuid:' + U.owner1, 'op-create-temp', 'RWARMY created temp');
    expect(o1, 'rwarmy grant uuid:' + U.p4 + ' command temp', 'grant-temp', 'granted command temp');
    expect(o1, 'rwarmy remove temp', 'owner-removes-own', 'RWARMY removed temp');
    expect(op, 'rwarmy create temp uuid:' + U.owner1, 'op-recreate-temp', 'RWARMY created temp');
    expect(p4, 'rwarmy info temp', 'old-grant-gone', 'unknown army temp');
    expect(op, 'rwarmy remove temp', 'op-removes', 'RWARMY removed temp');

    // A real entry of the operator list, read when asked: deop applies at once.
    check('before-op', !A.canCommand('M111B_RealOp', 'west'), 'before');
    run(rcon, 'op M111B_RealOp');
    check('real-op', A.canCommand('M111B_RealOp', 'west') && A.visibleArmies('M111B_RealOp').length === 6, 'op');
    run(rcon, 'deop M111B_RealOp');
    check('real-deop', !A.canView('M111B_RealOp', 'west') && A.visibleArmies('M111B_RealOp').length === 0, 'deop');

    // Script API acts with operator rights.
    check('api-grant', A.grant('uuid:' + U.p2, 'view', 'guard') === true && A.canView('uuid:' + U.p2, 'guard'), 'grant');
    check('api-grant-bad', A.grant('uuid:' + U.p2, 'own', 'guard') === false && A.grant('M111B_Nobody', 'view', 'guard') === false, 'bad');
    check('api-visible', Java.from(A.visibleArmies('uuid:' + U.p2)).map(String).join(',') === 'guard', 'visible');
    check('api-revoke', A.revoke('uuid:' + U.p2, 'view', 'guard') === true && !A.canView('uuid:' + U.p2, 'guard'), 'revoke');
    check('api-unknown', !A.canView('M111B_Nobody', 'guard') && !A.canView('uuid:' + U.p1, 'nosuch'), 'unknown');
    check('api-owner', A.canCommand('uuid:' + U.owner1, 'north') && A.canCommand('uuid:' + U.owner2, 'west'), 'owner');
}

// After a restart: what was granted stays, what was revoked stays revoked, a renamed player keeps the grant.
function restart() {
    var p1 = sender('p1', -1), p2 = sender('p2', -1), p3 = sender('p3', -1), p4 = sender('p4', -1);
    expect(p1, 'rwarmy list', 'restart-selected', 'RWARMY list armies=1');
    expect(p1, 'rwarmy info north', 'restart-view', 'RWARMY info north');
    expect(p1, 'rwarmy info south', 'restart-revoked', 'unknown army south');
    expect(p2, 'rwarmy list', 'restart-revoked-all', 'RWARMY list armies=0');
    expect(p3, 'rwarmy list', 'restart-server-all', 'RWARMY list armies=6');
    expect(p3, 'rwarmy assign west PERM_S 50', 'restart-command', 'assigned west npcs=1 changed=1');
    expect(p4, 'rwarmy info guard', 'restart-faction', 'RWARMY info guard');
    check('restart-seen-player', A.canView('uuid:' + U.absent, 'south'), 'absent');
    server.getPlayerProfileCache().addEntry(new Profile(Uuid.fromString(U.absent), 'M111B_Renamed'));
    check('restart-renamed', A.canView('M111B_Renamed', 'south'), 'renamed');
    expect(sender('owner1', -1), 'rwarmy grants', 'restart-renamed-shown', 'grant M111B_Renamed by');
}

function tick(e) {
    age += 10;
    if (age !== 40) { return; }
    server = Server.Server;
    world = e.npc.getWorld().getMCWorld();
    rcon = new RCon(server);
    var part = String(e.npc.getName());
    if (part === 'PERM_Main') {
        server.getPlayerProfileCache().addEntry(new Profile(Uuid.fromString(U.absent), 'M111B_Absent'));
        main();
    } else {
        restart();
    }
    Log.info('ARMYPERM DONE part=' + part + ' checks=' + checks + ' failed=' + failed);
    e.npc.despawn();
}
