// Chat ukazi za vojsko NPC-jev, predelani na paket rework/formation (M4.14).
//
// Razlika proti prejsnji skripti: premik ne dela vec s timerji na igralcu in navigateTo
// za vsakega NPC-ja posebej. Enoto vodi Java (FormationApi): ena pot za vse, zaprta zanka
// (enota pocaka zaostale), stiskanje skozi vrata in sidranje na koncu.
// Sprememba skladnje: legija ima namesto [x] [z] samo [sirina] (globina sledi iz stevila
// NPC-jev), zato sta range in speed zdaj en argument prej. Nova sta kolona in stop.
// recruit/derecruit/attack/defense/wander/dewander so nespremenjeni.
//
// Cilj je blok NAD blokom, v katerega gleda igralec (lb.y + 1): tam stojijo stopala.
// Potrebuje build z rework/formation. Na originalnem modu Java.type vrze napako.

var F = Java.type("noppes.npcs.rework.formation.FormationApi");

var DEFAULT_RANGE = 100;
var MAX_RANGE = 220;
var ALLOWED_PLAYERS = ["FifiStein", "jakakriletic", "memzl543", "Hladilnik"];

function is_allowed(player) {
    var name = player.getName();
    for (var i = 0; i < ALLOWED_PLAYERS.length; i++) {
        if (ALLOWED_PLAYERS[i] == name) {
            return true;
        }
    }
    return false;
}

function num(value, fallback, min, max) {
    var n = parseFloat(value);
    if (isNaN(n)) {
        n = fallback;
    }
    return Math.max(min, Math.min(max, n));
}

function npcs_by_name(event, name, range) {
    if (!name) {
        event.player.message("Manjka ime NPCja.");
        return null;
    }
    var p = event.player;
    var nearby = p.world.getNearbyEntities(p.x, p.y, p.z, range, 2);
    var out = [];
    for (var i = 0; i < nearby.length; i++) {
        if (nearby[i].getName() == name) {
            out.push(nearby[i]);
        }
    }
    if (out.length == 0) {
        event.player.message("Ni najdenih NPCjev z imenom: " + name);
        return null;
    }
    return out;
}

function look_block(event) {
    var block = event.player.rayTraceBlock(5000, false, true);
    if (block == null) {
        event.player.message("Poglej v blok, kamor naj NPCji gredo.");
        return null;
    }
    return block.getPos();
}

// Zadnji argument je lahko zastavica: drzi (ne prekini za boj), brezsidra, takoj.
function flags_of(args) {
    var f = [];
    for (var i = 2; i < args.length; i++) {
        var a = String(args[i]).toLowerCase();
        if (a == "drzi" || a == "brezsidra" || a == "takoj") {
            f.push(a);
        }
    }
    return f.join(" ");
}

function facing(event) {
    return Math.round(event.player.getRotation() / 90) * 90;
}

// legija [ime] [sirina] [range] [speed] [zastavice]
function handle_legija(event, args) {
    var npcs = npcs_by_name(event, args[1], num(args[3], DEFAULT_RANGE, 1, MAX_RANGE));
    var lb = npcs && look_block(event);
    if (!lb) {
        return;
    }
    var width = num(args[2], 5, 1, 80);
    var speed = num(args[4], 3, 0.5, 6);
    event.player.message(F.legija(npcs, lb.x + 0.5, lb.y + 1, lb.z + 0.5, width, facing(event), speed, flags_of(args)));
}

// march [ime] [range] [speed] [zastavice]
function handle_march(event, args) {
    var npcs = npcs_by_name(event, args[1], num(args[2], DEFAULT_RANGE, 1, MAX_RANGE));
    var lb = npcs && look_block(event);
    if (!lb) {
        return;
    }
    var speed = num(args[3], 2, 0.5, 6);
    event.player.message(F.march(npcs, lb.x + 0.5, lb.y + 1, lb.z + 0.5, speed, flags_of(args)));
}

// obramba [ime] [radius] [range] [speed] [zastavice]
function handle_obramba(event, args) {
    var npcs = npcs_by_name(event, args[1], num(args[3], DEFAULT_RANGE, 1, MAX_RANGE));
    var lb = npcs && look_block(event);
    if (!lb) {
        return;
    }
    var radius = num(args[2], 5, 2, 80);
    var speed = num(args[4], 3, 0.5, 6);
    event.player.message(F.obramba(npcs, lb.x + 0.5, lb.y + 1, lb.z + 0.5, radius, speed, flags_of(args)));
}

// kolona [ime] [vrst] [range] [speed] [zastavice]
function handle_kolona(event, args) {
    var npcs = npcs_by_name(event, args[1], num(args[3], DEFAULT_RANGE, 1, MAX_RANGE));
    var lb = npcs && look_block(event);
    if (!lb) {
        return;
    }
    var files = num(args[2], 2, 1, 10);
    var speed = num(args[4], 3, 0.5, 6);
    event.player.message(F.kolona(npcs, lb.x + 0.5, lb.y + 1, lb.z + 0.5, files, facing(event), speed, flags_of(args)));
}

// stop [ime] [range]
function handle_stop(event, args) {
    var npcs = npcs_by_name(event, args[1], num(args[2], DEFAULT_RANGE, 1, MAX_RANGE));
    if (npcs) {
        event.player.message(F.stop(npcs));
    }
}

function apply_role_follow(event, args, follow) {
    var npcs = npcs_by_name(event, args[1], num(args[2], DEFAULT_RANGE, 1, MAX_RANGE));
    if (!npcs) {
        return;
    }
    var changed = 0;
    for (var i = 0; i < npcs.length; i++) {
        var role = npcs[i].getRole();
        if (role != null) {
            if (follow) {
                role.setFollowing(event.player);
                role.isFollowing = true;
            } else {
                role.isFollowing = false;
            }
            changed++;
        }
    }
    event.player.message((follow ? "Recruit" : "Derecruit") + ": " + changed + " NPC.");
}

function apply_ai(event, args, mode, value) {
    var npcs = npcs_by_name(event, args[1], num(args[2], DEFAULT_RANGE, 1, MAX_RANGE));
    if (!npcs) {
        return;
    }
    for (var i = 0; i < npcs.length; i++) {
        var ai = npcs[i].getAi();
        if (mode == "move") {
            ai.setMovingType(value);
        } else {
            ai.setTacticalType(value);
        }
    }
    event.player.message("AI update: " + npcs.length + " NPC.");
}

function help(player) {
    player.message("Commands for NPCs:");
    player.message("- legija [ime] [sirina] [range] [speed] [drzi|brezsidra|takoj]");
    player.message("- march [ime] [range] [speed] [zastavice]");
    player.message("- obramba [ime] [radius] [range] [speed] [zastavice]");
    player.message("- kolona [ime] [vrst] [range] [speed] [zastavice]");
    player.message("- stop [ime] [range]");
    player.message("- status");
    player.message("- recruit / derecruit / attack / defense / wander / dewander [ime] [range]");
}

function chat(event) {
    var raw = String(event.message).replace(/^\s+|\s+$/g, "");
    if (raw == "" || !is_allowed(event.player)) {
        return;
    }
    var args = raw.split(/\s+/);
    var cmd = String(args[0]).toLowerCase();

    if (cmd == "help") { help(event.player); return; }
    if (cmd == "legija") { handle_legija(event, args); return; }
    if (cmd == "march") { handle_march(event, args); return; }
    if (cmd == "obramba") { handle_obramba(event, args); return; }
    if (cmd == "kolona") { handle_kolona(event, args); return; }
    if (cmd == "stop") { handle_stop(event, args); return; }
    if (cmd == "status") { event.player.message(F.status()); return; }
    if (cmd == "recruit") { apply_role_follow(event, args, true); return; }
    if (cmd == "derecruit") { apply_role_follow(event, args, false); return; }
    if (cmd == "wander") { apply_ai(event, args, "move", 1); return; }
    if (cmd == "dewander") { apply_ai(event, args, "move", 0); return; }
    if (cmd == "attack") { apply_ai(event, args, "tactical", 0); return; }
    if (cmd == "defense") { apply_ai(event, args, "tactical", 6); return; }
}
