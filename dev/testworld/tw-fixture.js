// TW fixture - ustvari quest in dialog prek uradnega scripting API-ja moda.
//
// Zakaj tako: konzolni ukazi 'noppes quest/dialog' znajo samo start, finish in reload,
// ne pa create. GUI je edina druga pot. NBT formata NE pisemo na roko - protokol to
// prepoveduje - ampak pustimo modu, da objekt ustvari (IQuestCategory.create()) in ga
// shrani po svoji poti (Quest.save() -> QuestController.saveQuest -> NBTJsonUtil.SaveFile).
// Datoteka, ki nastane, je zato enaka tisti iz GUI-ja.
//
// Nashorn na Javi 8: brez let, brez puscicnih funkcij, brez sablonskih nizov.
// V tej datoteki so dovoljeni SAMO enojni narekovaji - skripta se vlozi v clone JSON
// kot en sam niz.

var CAT = 'TW';

function say(npc, m) {
    npc.executeCommand('/say ' + m);
}

function api() {
    return Java.type('noppes.npcs.api.NpcAPI').Instance();
}

function findCat(list, name) {
    for (var i = 0; i < list.size(); i++) {
        if (list.get(i).getName() === name) { return list.get(i); }
    }
    return null;
}

function findNamed(list, name) {
    for (var i = 0; i < list.size(); i++) {
        if (list.get(i).getName() === name) { return list.get(i); }
    }
    return null;
}

function findNpc(world, name) {
    var all = world.getAllEntities(2);
    for (var i = 0; i < all.length; i++) {
        if (all[i].getName() === name) { return all[i]; }
    }
    return null;
}

function build(npc) {
    var A = api();
    var w = npc.getWorld();

    var qcat = findCat(A.getQuests().categories(), CAT);
    if (qcat === null) { say(npc, 'TW-FIX-ERROR ni quest kategorije ' + CAT); return; }
    var dcat = findCat(A.getDialogs().categories(), CAT);
    if (dcat === null) { say(npc, 'TW-FIX-ERROR ni dialog kategorije ' + CAT); return; }

    // Idempotentno: ce fixture ze obstaja, ga ne podvajamo.
    var q = findNamed(qcat.quests(), 'TW_Quest');
    var qNew = 0;
    if (q === null) {
        q = qcat.create();
        // tip 5 = manual: edini, ki ga API zna do konca nastaviti (QuestItem.items ni izpostavljen)
        q.setType(5);
        q.setName('TW_Quest');
        q.setLogText('Testni quest integracijske matrike.');
        q.setCompleteText('TW-QUEST-COMPLETE');
        q.setNpcName('T_Trader');
        q.getRewards().setSlot(0, w.createItem('minecraft:stone', 0, 1));
        q.save();
        qNew = 1;
    }
    say(npc, 'TW-FIX-QUEST id=' + q.getId() + ' ime=' + q.getName() + ' tip=' + q.getType() + ' nov=' + qNew);

    var d = findNamed(dcat.dialogs(), 'TW_Dialog');
    var dNew = 0;
    if (d === null) {
        d = dcat.create();
        d.setName('TW_Dialog');
        d.setText('TW-DIALOG-LINE');
        d.setQuest(q);
        d.save();
        dNew = 1;
    }
    var dq = d.getQuest();
    say(npc, 'TW-FIX-DIALOG id=' + d.getId() + ' ime=' + d.getName() + ' quest=' + (dq !== null ? dq.getId() : -1) + ' nov=' + dNew);

    var trader = findNpc(w, 'T_Trader');
    if (trader === null) { say(npc, 'TW-FIX-ERROR ni NPC-ja T_Trader v svetu'); return; }
    trader.setDialog(0, d);
    var back = trader.getDialog(0);
    say(npc, 'TW-FIX-ATTACH npc=T_Trader slot=0 dialog=' + (back !== null ? back.getId() : -1));

    say(npc, 'TW-FIX-DONE');
}

function init(e) {
    say(e.npc, 'TW-FIX-INIT');
    try {
        build(e.npc);
    } catch (err) {
        say(e.npc, 'TW-FIX-ERROR ' + err);
    }
}
