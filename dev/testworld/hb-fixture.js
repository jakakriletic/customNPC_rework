// M3.8 (R6) - fixture za scenarij hitbox nacinov: hb-run.ps1.
//
// Iz PERF_Idle.json naredi stiri NPC-je, ki hodijo (MovingState 1), da sprejmejo potisk -
// EntityNPCInterface.addVelocity potisk zavrne, ce NPC ne hodi (isWalking). WalkingRange 1 in
// npcInteracting 0 drzita NPC-ja pri miru (EntityAIWander pri dosegu < 3 ne najde cilja).
//   HB_A, HB_B   velikost 5 (sirina 0,6)
//   HB_Velik     velikost 15 (sirina 1,8 - masa 27x)
//   HB_Scit      velikost 5, scit v levi roki (Weapons slot 2)
// M3.9 (m39-run.ps1):
//   M39_Nos, M39_Jah   nosilec in jahac (velikost 5)
//   M39_S1..S6         NPC-ji na poti, ki tavajo (kot HB_A)
//   M39_S7, M39_S8     NPC-ja na poti, ki stojita (MovingState 0: potisk zavrneta ze v originalu)
//   M39_H1..H8         hodeci (kot HB_A): spawn na cilju, tp na start, EntityAIReturn jih vrne skozi M39_S*
//
// Zagon: node dev/testworld/hb-fixture.js   (napise HB_*.json v customnpcs/clones/1)

var fs = require('fs');
var path = require('path');
var dir = path.join(__dirname, 'customnpcs', 'clones', '1');
var src = fs.readFileSync(path.join(dir, 'PERF_Idle.json'), 'utf8');

function zamenjaj(s, a, b) {
    if (s.indexOf(a) < 0) { throw new Error('ni najdeno: ' + a); }
    return s.replace(a, b);
}

function fixture(ime, velikost, scit, stoji) {
    var s = src;
    s = zamenjaj(s, '"Name": "PERF_Idle",', '"Name": "' + ime + '",');
    s = zamenjaj(s, '"Size": 5,', '"Size": ' + velikost + ',');
    if (!stoji) {
        s = zamenjaj(s, '"MovingState": 0,', '"MovingState": 1,');
    }
    s = zamenjaj(s, '"WalkingRange": 10,', '"WalkingRange": 1,');
    s = zamenjaj(s, '"npcInteracting": 1b,', '"npcInteracting": 0b,');
    if (scit) {
        s = zamenjaj(s, '"Weapons": [\n    ],',
            '"Weapons": [\n        {\n            "Slot": 2b,\n            "id": "minecraft:shield",\n            "Count": 1b,\n            "Damage": 0s\n        }\n    ],');
    }
    fs.writeFileSync(path.join(dir, ime + '.json'), s);
    console.log('napisan ' + ime);
}

fixture('HB_A', 5, false);
fixture('HB_B', 5, false);
fixture('HB_Velik', 15, false);
fixture('HB_Scit', 5, true);

fixture('M39_Nos', 5, false);
fixture('M39_Jah', 5, false);
for (var i = 1; i <= 8; i++) {
    fixture('M39_S' + i, 5, false, i >= 7);
    fixture('M39_H' + i, 5, false);
}
