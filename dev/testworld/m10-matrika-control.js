// M10.1a disposable fixture only (controller, packaged Forge server; D-096, Q215-Q218). The same
// text runs in both arms: O = distribution CustomNPCs 01Oct19 jar, K = workspace jar with code
// defaults. Only the original CNPC script API, Forge/JDK class names and no SRG/MCP member names.
// @@PHASE@@ is "run" (build fixtures, exercise rows) or "verify" (after a real save/stop/start).
var M10_PHASE = @@PHASE@@;
var M10_OUT = @@OUTPUT@@;
var M10_TIMER = 7710;
var M10_END = 900;
var m10 = null;
var M10_GEAR = ['minecraft:iron_bootsx1', 'minecraft:iron_chestplatex1', 'minecraft:iron_helmetx1', 'minecraft:iron_leggingsx1', 'minecraft:iron_swordx1', 'minecraft:shieldx1'].join(',');
// Positional/identity keys; every other difference must be explained by the harness register.
var M10_POSITIONAL = ['Pos', 'Motion', 'Rotation', 'UUIDMost', 'UUIDLeast', 'StartPosNew', 'FallDistance', 'OnGround', 'Fire', 'Air', 'PortalCooldown', 'Dimension'];

function m10Log(text) { Java.type('java.lang.System').out.println(text); }
function m10Api() { return Java.type('noppes.npcs.api.NpcAPI').Instance(); }
function m10Check(key, ok, detail) { m10.checks.push({ key: key, ok: !!ok, detail: String(detail) }); }

// Top-level NBT key -> NbtJson text of that key alone (no member names of MC classes).
function m10Map(entity) {
    var api = m10Api();
    var Tag = Java.type('net.minecraft.nbt.NBTTagCompound');
    var nbt = entity.getEntityNbt();
    var keys = nbt.getKeys();
    var out = {};
    for (var i = 0; i < keys.length; i++) {
        var one = api.getINbt(new Tag());
        one.merge(nbt);
        for (var j = 0; j < keys.length; j++) { if (j !== i) { one.remove(keys[j]); } }
        out[String(keys[i])] = String(one.toJsonString()).replace(/\s+/g, ' ');
    }
    return out;
}

function m10Diff(a, b, skip) {
    var diff = [];
    var seen = {};
    for (var k in a) { seen[k] = 1; }
    for (var k2 in b) { seen[k2] = 1; }
    for (var key in seen) {
        if (skip.indexOf(key) >= 0) { continue; }
        if (a[key] !== b[key]) { diff.push(key); }
    }
    diff.sort();
    return diff;
}

function m10Item(stack) { return stack === null || stack.isEmpty() ? 'empty' : String(stack.getName()) + 'x' + stack.getStackSize(); }

function m10Gear(npc) {
    var inv = npc.getInventory();
    return [m10Item(inv.getRightHand()), m10Item(inv.getLeftHand()), m10Item(inv.getArmor(0)), m10Item(inv.getArmor(1)),
        m10Item(inv.getArmor(2)), m10Item(inv.getArmor(3))].join(',');
}

function m10Npc(world, name, x, z, hp) {
    // NpcAPI.spawnNPC takes the Minecraft World (01Oct19 API), not the IWorld wrapper.
    var npc = m10Api().spawnNPC(world.getMCWorld(), x, 4, z);
    npc.setName(name);
    npc.getStats().setMaxHealth(hp);
    npc.setHealth(hp);
    npc.setHome(x, 4, z);
    return npc;
}

function m10Golem(world, x, z) {
    var g = world.createEntityFromNBT(m10Api().stringToNbt('{id:"minecraft:villager_golem",NoAI:1b,PersistenceRequired:1b,CustomName:"M10_G' + x + '_' + z + '"}'));
    g.setPosition(x + 0.5, 4, z + 0.5);
    world.spawnEntity(g);
    g.setMaxHealth(400);
    g.setHealth(400);
    return g;
}

function m10Find(world, name) {
    var all = world.getAllEntities(2);
    for (var i = 0; i < all.length; i++) { if (String(all[i].getName()) === name) { return all[i]; } }
    return null;
}

function m10Dist(a, x, z) { return Math.sqrt(Math.pow(a.getX() - x, 2) + Math.pow(a.getZ() - z, 2)); }

function m10SetupRun(npc) {
    var world = npc.getWorld();
    var f = m10.f;
    // IN8 / IN6 / IN3: one still NPC with equipment, stats, stored and temp data; IN3 clone of it.
    f.a = m10Npc(world, 'M10_A', 4, 8, 37);
    f.a.getAi().setMovingType(0);
    f.a.getAi().setReturnsHome(true);
    var inv = f.a.getInventory();
    inv.setRightHand(world.createItem('minecraft:iron_sword', 0, 1));
    inv.setLeftHand(world.createItem('minecraft:shield', 0, 1));
    var armor = ['minecraft:iron_helmet', 'minecraft:iron_chestplate', 'minecraft:iron_leggings', 'minecraft:iron_boots'];
    for (var s = 0; s < 4; s++) { inv.setArmor(s, world.createItem(armor[s], 0, 1)); }
    f.a.getStoreddata().put('m10k', 'stored-a');
    f.a.getTempdata().put('m10t', 'temp-a');
    // IN7: respawning NPC (SpawnCycle 0, 1 s) killed by damage at tick 40.
    f.d = m10Npc(world, 'M10_D', 12, 14, 20);
    f.d.getStats().setRespawnType(0);
    f.d.getStats().setRespawnTime(1);
    f.d.getAi().setMovingType(0);
    // IA1 melee and ranged on fixed targets (NoAI golems); IA2 melee on a moving target.
    f.g1 = m10Golem(world, 20, 20);
    f.m = m10Npc(world, 'M10_M', 23, 20, 500);
    f.m.getStats().getMelee().setStrength(6);
    f.g2 = m10Golem(world, 40, 26);
    f.r = m10Npc(world, 'M10_R', 28, 26, 500);
    f.r.getInventory().setProjectile(world.createItem('minecraft:arrow', 0, 1));
    f.r.getStats().getRanged().setStrength(6);
    f.r.getStats().getRanged().setRange(24);
    f.r.getStats().getRanged().setDelay(20, 30);
    f.g3 = m10Golem(world, 0, 40);
    f.p = m10Npc(world, 'M10_P', 0, 30, 500);
    f.p.getStats().getMelee().setStrength(6);
    // IA3: return home after a displacement; bounded wandering.
    f.h = m10Npc(world, 'M10_H', 40, 8, 20);
    f.h.getAi().setMovingType(0);
    f.h.getAi().setReturnsHome(true);
    f.w = m10Npc(world, 'M10_W', 40, 0, 20);
    f.w.getAi().setMovingType(1);
    f.w.getAi().setWanderingRange(4);
    // IA5 / IL5: teleport in the dimension, then the harness moves it to the nether roof.
    f.x = m10Npc(world, 'M10_X', 30, 8, 55);
    f.x.getAi().setWalkingSpeed(7);
    f.x.getInventory().setRightHand(world.createItem('minecraft:golden_sword', 0, 1));
    // IS3: scripted block whose tick hook counts in world tempdata and storeddata.
    world.setBlock(2, 4, -6, 'customnpcs:npcscripted', 0);
    var block = world.getBlock(2, 4, -6);
    var code = "function tick(e){var w=e.block.getWorld();var t=w.getTempdata();t.put('m10blk',(t.has('m10blk')?t.get('m10blk'):0)+1);"
        + "var s=w.getStoreddata();s.put('m10blkS',(s.has('m10blkS')?s.get('m10blkS'):0)+1);}";
    var tile = block.getTileEntityNBT();
    tile.merge(m10Api().stringToNbt('{ScriptEnabled:1b,ScriptLanguage:"ECMAScript",Scripts:[{Script:"' + code + '",ScriptList:[],Console:[]}]}'));
    block.setTileEntityNBT(tile);
    // IS8: world stored/temp data.
    world.getStoreddata().put('m10ws', 'stored-w');
    world.getTempdata().put('m10wt', 'temp-w');
}

function m10TickRun(npc) {
    var f = m10.f;
    var t = m10.tick;
    var world = npc.getWorld();
    var s = m10.s;
    if (t === 5) {
        // IN3: clone roundtrip through the clone store (tab 1).
        m10Api().getClones().set(1, 'M10_IN3', f.a);
        f.c = m10Api().getClones().spawn(10, 4, 8, 1, 'M10_IN3', world);
    }
    if (t === 6 && f.c !== null) {
        // Compared before the rename that keeps the clone apart from M10_A after the restart.
        s.aMap6 = m10Map(f.a); s.cMap6 = m10Map(f.c);
        f.c.setName('M10_C');
    }
    if (t === 10) { f.h.setPosition(48.5, 4, 8.5); s.hDisplaced = m10Dist(f.h, 40.5, 8.5); }
    if (t === 20) { s.xHome = [f.x.getHomeX(), f.x.getHomeY(), f.x.getHomeZ()].join(','); f.x.setPosition(30.5, 4, 24.5); }
    if (t === 21) {
        s.xTeleport = { pos: [Math.floor(f.x.getX()), Math.floor(f.x.getZ())].join(','), home: [f.x.getHomeX(), f.x.getHomeY(), f.x.getHomeZ()].join(','),
            hp: f.x.getMaxHealth(), speed: f.x.getAi().getWalkingSpeed(), item: m10Item(f.x.getInventory().getRightHand()) };
    }
    if (t === 40) { s.dBefore = f.d.isAlive(); f.d.damage(100000); }
    if (t > 40) {
        if (!f.d.isAlive() && s.dDeadAt === undefined) { s.dDeadAt = t; }
        if (s.dDeadAt !== undefined && f.d.isAlive() && s.dBackAt === undefined) {
            s.dBackAt = t; s.dBackDist = m10Dist(f.d, 12.5, 14.5); s.dBackHp = f.d.getHealth();
        }
    }
    if (t === 100) { s.aMap100 = m10Map(f.a); }
    if (t === 500) { s.aMap500 = m10Map(f.a); }
    // Targets are re-set every second (same schedule in both arms); hits are counted from golem HP.
    if (t % 20 === 1) { f.m.setAttackTarget(f.g1); f.r.setAttackTarget(f.g2); f.p.setAttackTarget(f.g3); }
    if (t % 10 === 0 && t <= 400) { f.g3.setPosition(Math.min(20, t / 20) + 0.5, 4, 40.5); }
    var pd = Math.sqrt(Math.pow(f.p.getX() - f.g3.getX(), 2) + Math.pow(f.p.getZ() - f.g3.getZ(), 2));
    if (s.pMin === undefined || pd < s.pMin) { s.pMin = pd; }
    var wd = m10Dist(f.w, 40.5, 0.5);
    if (s.wMax === undefined || wd > s.wMax) { s.wMax = wd; }
    if (t === 600) {
        s.uuidX = String(f.x.getUUID());
        m10Log('M10 DIM uuid=' + s.uuidX);
    }
    // allow-nether=false: the nether exists only after the harness moved the NPC (Forge setdimension).
    if (t > 600 && s.netherAt === undefined && Java.type('net.minecraftforge.common.DimensionManager').getWorld(-1) !== null) {
        var nether = m10Api().getIWorld(-1);
        var moved = nether.getEntity(s.uuidX);
        if (moved !== null) {
            s.netherAt = t;
            s.xNether = { hp: moved.getMaxHealth(), speed: moved.getAi().getWalkingSpeed(), item: m10Item(moved.getInventory().getRightHand()),
                y: Math.floor(moved.getY()), name: String(moved.getName()) };
            s.xOverworldGone = world.getEntity(s.uuidX) === null;
        }
    }
    return t >= M10_END && s.netherAt !== undefined || t >= M10_END + 600;
}

function m10FinishRun(npc) {
    var f = m10.f;
    var s = m10.s;
    var world = npc.getWorld();
    var aMap = m10Map(f.a);
    var volatileKeys = m10Diff(s.aMap100, s.aMap500, []);
    m10.proof.volatile = volatileKeys;
    m10.proof.aMap = aMap;
    var skip = M10_POSITIONAL.concat(volatileKeys);
    // IN3 is "no field lost": every key of the source keeps its value in the clone (extra keys allowed).
    var cloneDiff = ['<no clone>'];
    if (s.cMap6 !== undefined) {
        cloneDiff = [];
        for (var key in s.aMap6) { if (skip.indexOf(key) < 0 && s.aMap6[key] !== s.cMap6[key]) { cloneDiff.push(key); } }
        cloneDiff.sort();
    }
    m10Check('IN3-clone-roundtrip', f.c !== null && cloneDiff.length === 0, cloneDiff.join(','));
    m10.proof.cloneDiff = cloneDiff;
    m10.proof.aGear = m10Gear(f.a);
    m10Check('IN8-gear-set', m10.proof.aGear.split(',').sort().join(',') === M10_GEAR, m10.proof.aGear);
    m10Check('IN7-dies', s.dBefore === true && s.dDeadAt !== undefined, s.dBefore + ' dead@' + s.dDeadAt);
    m10Check('IN7-respawns', s.dBackAt !== undefined && s.dBackAt - s.dDeadAt <= 200 && s.dBackDist < 1.5 && s.dBackHp === 20,
        'back@' + s.dBackAt + ' dist=' + s.dBackDist + ' hp=' + s.dBackHp);
    m10.proof.hits = { melee: 400 - f.g1.getHealth(), ranged: 400 - f.g2.getHealth(), moving: 400 - f.g3.getHealth() };
    m10Check('IA1-melee-hits', f.g1.getHealth() < 400, m10.proof.hits.melee);
    m10Check('IA1-ranged-hits', f.g2.getHealth() < 400, m10.proof.hits.ranged);
    m10Check('IA2-moving-target', f.g3.getHealth() < 400 && s.pMin < 3 && m10Dist(f.p, 0.5, 30.5) > 5, 'dmg=' + m10.proof.hits.moving + ' min=' + s.pMin.toFixed(2));
    var hHome = m10Dist(f.h, 40.5, 8.5);
    m10Check('IA3-return-home', s.hDisplaced > 7 && hHome < 2, s.hDisplaced.toFixed(2) + '->' + hHome.toFixed(2));
    m10Check('IA3-wander-bounded', s.wMax <= 7, s.wMax.toFixed(2));
    m10Check('IA5-teleport-keeps-state', s.xTeleport.pos === '30,24' && s.xTeleport.home === s.xHome && s.xTeleport.hp === 55 && s.xTeleport.speed === 7
        && s.xTeleport.item === 'minecraft:golden_swordx1', JSON.stringify(s.xTeleport) + ' home0=' + s.xHome);
    m10Check('IA5-dimension-keeps-state', s.netherAt !== undefined && s.xOverworldGone === true && s.xNether.hp === 55 && s.xNether.speed === 7
        && s.xNether.item === 'minecraft:golden_swordx1' && s.xNether.y >= 127 && s.xNether.name === 'M10_X', JSON.stringify(s.xNether));
    var blk = world.getTempdata().has('m10blk') ? world.getTempdata().get('m10blk') : 0;
    m10.proof.blockTicks = blk;
    m10.proof.blockStored = world.getStoreddata().has('m10blkS') ? world.getStoreddata().get('m10blkS') : 0;
    m10Check('IS3-block-tick', blk >= 20, blk);
    m10Check('IS8-set', world.getStoreddata().get('m10ws') === 'stored-w' && world.getTempdata().get('m10wt') === 'temp-w'
        && f.a.getStoreddata().get('m10k') === 'stored-a' && f.a.getTempdata().get('m10t') === 'temp-a', 'set');
    m10.proof.trace = {
        in3: cloneDiff.join(','), in7: s.dDeadAt !== undefined && s.dBackAt !== undefined, ia1m: f.g1.getHealth() < 400, ia1r: f.g2.getHealth() < 400,
        ia2: f.g3.getHealth() < 400, ia3: hHome < 2, ia5: s.xNether === undefined ? '' : JSON.stringify(s.xNether), is3: blk >= 20
    };
}

function m10SetupVerify(npc) {
    var world = npc.getWorld();
    var f = m10.f;
    f.a = m10Find(world, 'M10_A');
    f.d = m10Find(world, 'M10_D');
    f.c = m10Find(world, 'M10_C');
}

function m10TickVerify(npc) {
    var s = m10.s;
    var world = npc.getWorld();
    if (m10.tick === 1) {
        // The nether is not loaded at start (allow-nether=false, no player); Forge loads it on request,
        // reading a roof block loads the chunk and the saved NPC with it in the same tick.
        var Dims = Java.type('net.minecraftforge.common.DimensionManager');
        s.netherLoadedAtStart = Dims.getWorld(-1) !== null;
        if (!s.netherLoadedAtStart) { Dims.initDimension(-1); }
        var nether = m10Api().getIWorld(-1);
        nether.getBlock(8, 128, 8);
        var x = m10Find(nether, 'M10_X');
        s.xNether = x === null ? null : { hp: x.getMaxHealth(), speed: x.getAi().getWalkingSpeed(), item: m10Item(x.getInventory().getRightHand()), y: Math.floor(x.getY()) };
    }
    return m10.tick >= 300;
}

function m10FinishVerify(npc) {
    var f = m10.f;
    var s = m10.s;
    var world = npc.getWorld();
    m10Check('IN6-npc-loaded', f.a !== null && f.d !== null && f.c !== null, (f.a !== null) + ',' + (f.d !== null) + ',' + (f.c !== null));
    if (f.a !== null) {
        m10.proof.aMap = m10Map(f.a);
        m10.proof.aGear = m10Gear(f.a);
        m10.proof.aUuid = String(f.a.getUUID());
        m10Check('IN8-gear-after-restart', m10.proof.aGear.split(',').sort().join(',') === M10_GEAR, m10.proof.aGear);
        m10Check('IS8-npc-stored-kept', f.a.getStoreddata().get('m10k') === 'stored-a', f.a.getStoreddata().get('m10k'));
        m10Check('IS8-npc-temp-gone', !f.a.getTempdata().has('m10t'), f.a.getTempdata().has('m10t'));
    }
    m10Check('IS8-world-stored-kept', world.getStoreddata().get('m10ws') === 'stored-w', world.getStoreddata().get('m10ws'));
    m10Check('IS8-world-temp-gone', !world.getTempdata().has('m10wt'), world.getTempdata().has('m10wt'));
    var blk = world.getTempdata().has('m10blk') ? world.getTempdata().get('m10blk') : 0;
    m10.proof.blockTicks = blk;
    m10.proof.blockStored = world.getStoreddata().has('m10blkS') ? world.getStoreddata().get('m10blkS') : 0;
    m10Check('IS3-block-script-after-restart', blk >= 10, blk);
    m10Check('IN7-alive-after-restart', f.d !== null && f.d.isAlive(), f.d === null ? 'missing' : f.d.isAlive());
    m10Check('IA5-IL5-nether-npc-after-restart', s.xNether !== null && s.xNether !== undefined && s.xNether.hp === 55 && s.xNether.speed === 7
        && s.xNether.item === 'minecraft:golden_swordx1', JSON.stringify(s.xNether));
    m10.proof.trace = { in6: f.a !== null, is8: !world.getTempdata().has('m10wt') && world.getStoreddata().get('m10ws') === 'stored-w', is3: blk >= 10,
        ia5: JSON.stringify(s.xNether) };
}

function m10Finish(npc) {
    npc.getTimers().stop(M10_TIMER);
    var ok = true;
    for (var i = 0; i < m10.checks.length; i++) { if (!m10.checks[i].ok) { ok = false; } }
    m10.proof.success = ok;
    m10.proof.checks = m10.checks;
    m10.proof.ticks = m10.tick;
    var Files = Java.type('java.nio.file.Files');
    var Paths = Java.type('java.nio.file.Paths');
    var text = new java.lang.String(JSON.stringify(m10.proof, null, 1));
    Files.write(Paths.get(M10_OUT), text.getBytes('UTF-8'));
    m10Log('M10 ' + M10_PHASE.toUpperCase() + ' DONE success=' + ok);
    npc.despawn();
}

function tick(e) {
    var d = e.npc.getTempdata();
    if (d.has('m10')) { return; }
    d.put('m10', 1);
    try {
        m10 = { tick: 0, checks: [], f: { c: null }, s: {}, proof: { phase: M10_PHASE } };
        if (M10_PHASE === 'run') { m10SetupRun(e.npc); } else { m10SetupVerify(e.npc); }
        // B3: DataTimers repeats after interval + 1 ticks, so 0 samples every server tick.
        e.npc.getTimers().start(M10_TIMER, 0, true);
        m10Log('M10 ' + M10_PHASE.toUpperCase() + ' START');
    } catch (error) {
        m10Log('M10 FAILED ' + error);
    }
}

function timer(e) {
    if (e.id !== M10_TIMER || m10 === null) { return; }
    try {
        m10.tick++;
        var done = M10_PHASE === 'run' ? m10TickRun(e.npc) : m10TickVerify(e.npc);
        if (!done) { return; }
        if (M10_PHASE === 'run') { m10FinishRun(e.npc); } else { m10FinishVerify(e.npc); }
        m10Finish(e.npc);
    } catch (error) {
        m10Log('M10 FAILED ' + error);
        try { e.npc.getTimers().stop(M10_TIMER); } catch (ignored) { }
    }
}
