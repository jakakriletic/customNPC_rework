// M11.1: script API of armies (noppes.npcs.rework.army.ArmyApi) on a real dedicated server, RwArmies=1.
// Installed only in a disposable test world; army-run.ps1 creates alpha/beta/delta by command first.
var A = Java.type('noppes.npcs.rework.army.ArmyApi');
var age = 0, checks = 0, failed = 0, control;

function check(key, ok) {
    checks++;
    if (!ok) { failed++; }
    control.executeCommand('/say ARMYAPI CHECK i=' + checks + ' key=' + key + ' ok=' + (ok ? 1 : 0));
}

function tick(e) {
    age += 10;
    control = e.npc;
    if (age !== 40) { return; }
    var all = e.npc.getWorld().getAllEntities(2), c = [], a = null;
    for (var i = 0; i < all.length; i++) {
        var name = String(all[i].getName());
        if (name === 'ARMY_C') { c.push(all[i]); }
        if (name === 'ARMY_A' && a === null) { a = all[i]; }
    }
    check('enabled', A.enabled() === true && A.getApiVersion() === 0);
    check('fixtures', c.length === 2 && a !== null);
    check('create', A.create('api_army', 'faction:1') === true);
    check('create-duplicate-case', A.create('API_ARMY', 'faction:0') === false);
    check('create-bad-name', A.create('bad name', 'faction:0') === false);
    check('create-unknown-faction', A.create('ghost', 'faction:999') === false && !A.exists('ghost'));
    check('create-bad-owner', A.create('ghost', 'nobody') === false);
    check('owner', String(A.owner('Api_Army')) === 'faction:1' && String(A.owner('none')) === '');
    var ok = true;
    for (var j = 0; j < c.length; j++) { ok = A.assign(c[j], 'API_ARMY') === true && ok; }
    check('assign', ok);
    check('stored-spelling', String(A.armyOf(c[0])) === 'api_army' && String(A.armyOf(c[1])) === 'api_army');
    check('members', A.members('api_army').length === 2);
    check('assign-unknown', A.assign(c[0], 'nosuch') === false && String(A.armyOf(c[0])) === 'api_army');
    check('assign-not-npc', A.assign(null, 'api_army') === false);
    check('command-assignment-visible', String(A.armyOf(a)) === 'alpha' && A.members('alpha').length === 3);
    var names = Java.from(A.list()).map(function (s) { return String(s); }).join(',');
    check('list', names === 'alpha,api_army,beta,delta');
    check('self-not-member', String(A.armyOf(e.npc)) === '');
    control.executeCommand('/say ARMYAPI DONE checks=' + checks + ' failed=' + failed);
    e.npc.despawn();
}
