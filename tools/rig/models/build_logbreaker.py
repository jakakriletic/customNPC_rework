"""Build the Logbreaker creature pack and render a preview with --preview PATH."""
import argparse
import json
import sys
from pathlib import Path

import numpy as np

from build_troll import BONES, MATERIALS
from geobuild import ROOT, box, build, mirror_box

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "anim"))
from compile import Library, compile_doc, load_yaml  # noqa: E402
from preview import Scene, render_sheet  # noqa: E402
from rig import Clip, RigConfig  # noqa: E402


PACK = ROOT / "rig_src/examples/creatures/logbreaker"
IMPACT = 1.25


def end_grain(img, face):
    if face != "north":
        return
    y, x = np.indices(img.shape[:2])
    radius = np.hypot(x - (img.shape[1] - 1) * 0.52, y - (img.shape[0] - 1) * 0.47)
    img[radius < 5] = (163, 115, 66)
    img[(radius < 5) & ((radius.astype(int) % 2) == 0)] = (124, 83, 44)


def warrior_face(img, face):
    if face == "north":
        img[4:6, 2:6] = img[4:6, -6:-2] = (70, 38, 29)
        img[6, 3:6] = img[6, -6:-3] = (221, 202, 171)
        img[6:8, 4] = img[6:8, -5] = (49, 54, 47)
        img[10:12, 1:4] = (123, 70, 44)


def geometry():
    log = [
        box((-26, 16, -21), (6, 6, 32), "wood_dark"),
        box((-29, 12, -56), (12, 14, 44), "bark", 1, tag="rings"),
        box((-31, 14, -56), (2, 10, 43), "bark"),
        box((-17, 14, -56), (2, 10, 40), "bark"),
        box((-28, 25, -47), (9, 2, 23), "moss"),
        box((-30, 23, -48), (4, 6, 6), "bark", 0.8),
        box((-30, 11, -24), (14, 16, 3), "strap"),
    ]
    shin = [
        box((-15, 5, -7), (13, 18, 13), "wool"),
        box((-15.5, 3, -8), (14, 12, 15), "boot"),
        box((-16, 0, -13), (15, 6, 19), "boot"),
        box((-16.2, 0, -13.3), (15.4, 2, 19.6), "strap"),
        box((-16, 11, -8.5), (15, 3, 16), "wrap", 0.8),
    ]
    replacement = {
        "club": log,
        "hips": [
            box((-14, 30, -9), (28, 14, 19), "wool"),
            box((-16, 35, -11), (32, 5, 22), "strap"),
            box((-3, 35, -12), (6, 5, 2), "bronze", 1),
            box((-14, 23, -11), (13, 13, 3), "cloth"),
            box((2, 22, -10), (11, 14, 3), "mail"),
        ],
        "torso": [
            box((-16, 38, -12), (32, 21, 24), "mail"),
            box((-19, 55, -11), (38, 18, 22), "skin"),
            box((-7, 70, -13), (14, 7, 11), "skin"),
            box((-18, 48, -13), (36, 7, 26), "wool"),
            box((-18, 55, -13), (35, 5, 3), "strap", 0.8,
                rotation=(0, 0, 24), pivot=(0, 58, -12)),
            box((-13, 62, -14), (6, 5, 2), "bronze", 1),
        ],
        "head": [
            box((-8, 72, -23), (16, 14, 15), "skin", 1, tag="warrior_face"),
            box((-2, 76, -26), (4, 5, 3), "skin_dark", 1),
            box((-9, 83, -23), (18, 5, 16), "hair", 1),
            box((-8, 74, -10), (16, 11, 4), "hair", .8),
            box((-9, 82, -24), (18, 2, 2), "bronze", 1),
            box((7, 71, -9), (4, 12, 5), "hair", .8),
        ],
        "jaw": [
            box((-7, 70, -23), (14, 5, 12), "skin", 1),
            box((-7, 68, -25), (14, 7, 6), "hair", 1),
            box((-4, 64, -23), (8, 5, 5), "hair", 1),
            box((-2, 64, -23.5), (4, 2, 5), "bronze", 1),
        ],
        "legR": [box((-16, 20, -8), (15, 18, 16), "wool")],
        "legL": [box((1, 20, -8), (15, 18, 16), "wool")],
        "shinR": shin,
        "shinL": [mirror_box(part) for part in shin],
    }
    extras = {
        "armL": [box((16, 65, -10), (16, 7, 17), "mail", .7),
                 box((22, 65, -11), (10, 2, 19), "bronze", .8)],
        "lowerArmL": [box((14, 30, -9.7), (18, 6, 16), "wrap", .8)],
        "lowerArmR": [box((-32, 30, -9.7), (18, 6, 16), "wrap", .8)],
    }
    bones = [
        (name, parent, pivot, replacement.get(name, boxes) + extras.get(name, []))
        for name, parent, pivot, boxes in BONES
    ]
    materials = dict(MATERIALS, skin=((165, 114, 76), 10, (126, 79, 52), .10),
                     skin_dark=((134, 83, 57), 10, (112, 68, 43), .10),
                     hair=((105, 53, 33), 14, (61, 34, 26), .13),
                     wool=((54, 75, 70), 12, (39, 56, 53), .15, "stitch"),
                     mail=((77, 82, 80), 16, (119, 122, 115), .23),
                     bronze=((163, 112, 55), 12, (100, 76, 42), .13),
                     boot=((61, 45, 36), 10, (85, 61, 45), .10),
                     wrap=((177, 164, 131), 12, (137, 118, 89), .10, "stitch"),
                     cloth=((151, 76, 39), 12, (92, 49, 30), .12, "stitch"),
                     bark=((85, 67, 45), 15, (57, 48, 31), .12, "grain"),
                     moss=((96, 113, 43), 18, (63, 88, 34), .20))
    return build("logbreaker", bones, materials, {"rings": end_grain, "warrior_face": warrior_face},
                 seed=412, visible=(11, 9, 3.6), output_dir=PACK)


def animations(rig):
    lib = Library(rig)
    carry = {"arm_r.raise": 15, "arm_r.out": 10, "elbow_r.bend": 115,
             "wrist_r.bend": 53, "wrist_r.in": -5}
    for name in ("stand", "stand_breathe", "walk_contact_r", "walk_pass_l", "walk_contact_l", "walk_pass_r"):
        lib.poses[name].pop("solve", None)
        lib.poses[name].update(carry)
    for name in ("hurt", "death_kneel"):
        lib.poses[name].pop("solve", None)
    docs = [load_yaml(rig.dir / f"{name}.anim.yaml") for name in ("idle", "walk", "hurt", "death")]
    # Wind up over the shoulder, plant both feet and commit to the new impact pose.
    docs.append({
        "name": "log_slam", "length": 2.35,
        "beats": [
            {"t": 0, "pose": "stand"},
            {"t": .73, "pose": "pound_windup", "ease": "out", "hold": .20},
            {"t": IMPACT, "pose": "pound_hit", "ease": "strike", "hold": .25},
            {"t": 2.35, "pose": "stand", "ease": "smooth"},
        ],
        "events": [
            {"t": 0.30, "type": "sound", "sound": "minecraft:entity.player.attack.strong", "volume": 1.1},
            {"t": 1.04, "type": "sound", "sound": "troll.swing", "volume": 1.1},
            {"t": IMPACT, "type": "hit", "id": "log_slam", "shape": "circle",
             "forward": 3.8, "side": .5, "radius": 4.1, "above": 2.3,
             "damage": 1, "falloff": .45, "knockback": 1.1, "lift": .28},
            {"t": IMPACT, "type": "sound", "sound": "troll.ground_pound", "volume": 2, "pitch": .92},
            {"t": IMPACT, "type": "camera_shake", "strength": 3, "radius": 16, "duration": .65},
        ],
    })
    clips = {}
    for doc in docs:
        name = doc["name"]
        clips[name], warnings = compile_doc(doc, lib)
        for warning in warnings:
            print(f"{name}: {warning}")
    slam = Clip("log_slam", clips["log_slam"])
    tip = rig.tips["club"]
    mats = lib.model.world_matrices(slam.sample(IMPACT))
    x, y, z = lib.model.point(mats, tip["bone"], tip["at"])
    if not (-.15 <= y / 16 <= .4 and z < 0):
        raise ValueError(f"log must hit the ground in front of the warrior: {(x, y, z)}")
    print(f"Impact: forward={-z / 16:.2f} right={x / 16:.2f} height={y / 16:.2f} blocks")
    for event in clips["log_slam"]["events"]:
        if event["type"] == "camera_shake":
            event.update(forward=round(-z / 16, 3), side=round(x / 16, 3))
    return clips


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--preview", type=Path)
    args = parser.parse_args()
    rig = RigConfig("troll")
    rig.geo, rig.texture = geometry()
    rig.tips["club"]["at"] = [-23, 19, -56]
    clips = animations(rig)
    payload = {"format_version": "1.8.0", "rig": {"head": "head", "shadow": 1.9}, "animations": clips}
    (PACK / "animations.json").write_text(json.dumps(payload, separators=(",", ":")) + "\n", encoding="utf-8")
    if args.preview:
        clip = Clip("log_slam", clips["log_slam"])
        frames = [(label, clip.sample(t)) for label, t in
                  (("Carry", 0), ("Windup", .80), ("Impact", IMPACT))]
        rig.preview["side"] = .48
        rig.preview["three_quarter"] = .52
        render_sheet(Scene(rig), frames, "Logbreaker", ("three_quarter", "side"),
                     cell=(345, 330), ppu=1.85).save(args.preview)


if __name__ == "__main__":
    main()
