var Animators = Java.type('noppes.npcs.rework.anim.Animators');
var ATTACK = 'log_slam';

function init(e) {
    var npc = e.npc, stats = npc.getStats();
    if (!npc.getStoreddata().has('logbreaker_initialized')) {
        stats.setMaxHealth(180);
        npc.setHealth(180);
        npc.getDisplay().setTitle('Hill Warband');
        npc.getStoreddata().put('logbreaker_initialized', 1);
        npc.updateClient();
    }
    stats.getMelee().setStrength(22);
    stats.getMelee().setRange(5);
    stats.getMelee().setDelay(66);
}

function tick(e) {
    if (Animators.of(e.npc).getClip() == ATTACK) {
        e.npc.clearNavigation();
    }
}

function meleeAttack(e) {
    e.setCanceled(true);
    var npc = e.npc, animation = Animators.of(npc);
    if (animation.getClip() == '') {
        npc.clearNavigation();
        animation.play(ATTACK);
    }
}
