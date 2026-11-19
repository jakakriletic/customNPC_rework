"""Build the Logbreaker creature pack and render a preview with --preview PATH.

Look (owner's reference, 19. 11.): a hunched masked giant of the blue Gallic warband.
Dark iron helmet with a pale brow band and a slotted face grille, head sunk between
huge dark pauldrons, arms and chest bound in pale wraps with grey and blue stripes,
pale bare fists, dark navy legs and boots, and a huge dark log with blue iron bands
resting on his right shoulder.
"""
import argparse
import json
import sys
from pathlib import Path

import numpy as np

from build_troll import BONES, MATERIALS
from geobuild import ROOT, box, build, mirror_box

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "anim"))
from compile import Library, beat_values, channels_to_pose, compile_doc, load_yaml  # noqa: E402
from preview import Scene, render_sheet  # noqa: E402
from rig import Clip, RigConfig  # noqa: E402


PACK = ROOT / "rig_src/examples/creatures/logbreaker"
IMPACT = 1.25
LOG_TIP = [-23, 19, -106]
FIST_L = (23, 19.5, -2)       # centre of the left fist in its bind pose
GRIP_L = (-23, 19.5, -19)     # where the left fist holds the log, in the log's bind frame
LEFT_ARM = ("arm_l.raise", "arm_l.out", "arm_l.swing", "elbow_l.bend", "wrist_l.bend", "wrist_l.in")

# Two-handed overhead slam. The right arm drives the log; the left arm is solved onto
# the grip (two_hand) so both fists hold the log through the whole swing.
CENTER = {"tip": "hand_r", "using": "arm_r.out", "side": .1}   # right fist to the middle
SLAM_POSES = {
    "lb_grab": {"base": "stand", "torso.bend": 6, "head.nod": -18, "arm_r.raise": 120,
                "elbow_r.bend": 70, "wrist_r.bend": -30, "solve": [CENTER]},
    "lb_windup": {"base": "pound_windup", "torso.bend": -22, "arm_r.raise": 172,
                  "elbow_r.bend": 48, "wrist_r.bend": -45, "wrist_r.in": 0, "solve": [CENTER]},
    "lb_swing": {"base": "pound_windup", "torso.bend": 20, "head.nod": -6, "arm_r.raise": 125,
                 "elbow_r.bend": 18, "wrist_r.bend": -20, "wrist_r.in": 0,
                 "knee_r.bend": 26, "knee_l.bend": 26, "leg_r.raise": 14, "leg_l.raise": 14,
                 "solve": [CENTER]},
    "lb_hit": {"base": "pound_hit", "arm_r.raise": 70, "arm_r.out": 0, "elbow_r.bend": 4, "wrist_r.in": 0,
               "solve": [{"tip": "hand_r", "using": "arm_r.swing", "side": .1},
                         {"tip": "club", "using": "wrist_r.bend", "height": 0.15}]},
}

# Log on the right shoulder: fist up in front of the shoulder, log slanting back and up
# behind the head (as in the reference).
CARRY = {"arm_r.raise": 50, "arm_r.out": 4, "arm_r.swing": 0, "elbow_r.bend": 125,
         "wrist_r.bend": -20, "wrist_r.in": 0}

SLIT = (20, 21, 30)
GLINT = (228, 236, 250)
STRIPE_GREY = (112, 116, 132)
STRIPE_BLUE = (74, 122, 198)


def wrap_stripes(img, face):
    """Horizontal bandage bands: seam lines, every third band dyed blue."""
    if face in ("up", "down"):
        return
    h = img.shape[0]
    for v in range(h):
        band, row = divmod(v, 3)
        if row == 0:
            img[v] = img[v] * 0.55 + np.array(STRIPE_GREY) * 0.45
        elif band % 3 == 1:
            img[v] = img[v] * 0.3 + np.array(STRIPE_BLUE) * 0.7


def grille(img, face):
    """Mask face plate: dark horizontal slits between iron bars, one glinting eye."""
    if face != "north":
        return
    h, w = img.shape[:2]
    for v in range(1, h, 2):
        img[v, 1:w - 1] = SLIT
    img[1, 2:4] = (60, 62, 74)
    img[1, w - 4:w - 2] = GLINT          # eye catching the light (his left)
    img[1, 2] = GLINT * np.array(0.6)    # dimmer right eye


def helm(img, face):
    if face != "north":
        return
    h, w = img.shape[:2]
    img[:, w // 2 - 1:w // 2 + 1] *= 0.8      # ridge down the middle of the helmet
    img[-3:, :] *= 0.7                         # shadow above the grille


def brow_band(img, face):
    if face in ("up", "down"):
        return
    w = img.shape[1]
    img[0] *= 1.12
    img[-1] *= 0.6
    for u in range(1, w, 4):                   # rivets
        img[1, u] = (70, 72, 84)


def plates(img, face):
    """Pauldron and hump: overlapping iron lames, light top edge."""
    if face == "up":
        img[:] *= 1.08
        return
    if face == "down":
        return
    h = img.shape[0]
    for v in range(0, h, 3):
        img[v] *= 1.18
        if v + 2 < h:
            img[v + 2] *= 0.7


def knuckles(img, face):
    if face not in ("north", "down", "east", "west"):
        return
    h, w = img.shape[:2]
    for i in range(1, 4):
        img[:, int(w * i / 4)] *= 0.78
    if face == "north":
        img[1, :] *= 1.1
        img[-1, :] *= 0.8


def log_rings(img, face):
    if face not in ("north", "south"):
        return
    y, x = np.indices(img.shape[:2])
    r = np.hypot(x - (img.shape[1] - 1) / 2, y - (img.shape[0] - 1) / 2)
    img[:] = (88, 86, 100)
    img[(r.astype(int) % 2) == 0] = (62, 60, 74)
    img[r < 1.2] = (44, 42, 54)


def log_bark(img, face):
    """Pale grey-blue streaks along the dark trunk."""
    rng = np.random.default_rng(img.shape[0] * 31 + img.shape[1])
    h, w = img.shape[:2]
    along_u = face in ("east", "west", "up", "down")
    count = max(2, (h if along_u else w) // 4)
    for _ in range(count):
        if along_u:
            v, u0 = rng.integers(0, h), rng.integers(0, max(1, w - 6))
            img[v, u0:u0 + rng.integers(4, 14)] = (112, 116, 140)
        else:
            u, v0 = rng.integers(0, w), rng.integers(0, max(1, h - 6))
            img[v0:v0 + rng.integers(4, 14), u] = (112, 116, 140)


def log():
    """Massive log along -z from the fists: a trimmed grip end both fists can hold
    (right fist at z ~ -2, left at GRIP_L), then a trunk thicker than his head."""
    return [
        # trimmed grip end, both fists close around it
        box((-30, 12, -36), (14, 14, 44), "log", .5, tag="bark"),
        box((-29, 11, -35.5), (12, 16, 43), "log", .5, tag="bark"),
        box((-28, 13, 8.1), (10, 12, 1), "log_end", 1, tag="rings"),
        box((-33, 9, -40), (20, 20, 6), "log", .5, tag="bark"),          # flare into the trunk
        # trunk
        box((-36, 8, -106), (26, 22, 68), "log", .5, tag="bark"),
        box((-34, 6, -105.5), (22, 26, 67), "log", .5, tag="bark"),
        box((-33, 9, -106.6), (20, 20, 1), "log_end", 1, tag="rings"),
        box((-36.6, 5.4, -46), (27.2, 27.2, 4), "band", 1),
        box((-36.6, 5.4, -74), (27.2, 27.2, 5), "band", 1),
        box((-36.6, 5.4, -100), (27.2, 27.2, 4), "band", 1),
        box((-40, 17, -62), (5, 6, 7), "log", 1),                         # knot stubs
        box((-20, 31.5, -86), (6, 4, 6), "log", 1),
        box((-11, 12, -58), (4, 6, 8), "log", 1),
        box((-28, 3.5, -90), (7, 3, 6), "log", 1),
    ]


def arm_r():
    return {
        "armR": [
            box((-30, 47, -8), (14, 20, 13), "wrap", .75, tag="wrap"),
            box((-35, 61, -12), (18, 13, 21), "iron", .75, tag="plates"),   # big pauldron
            box((-33, 73, -10), (14, 3, 17), "iron", .75, tag="plates"),
            box((-35.6, 60, -12.6), (19.2, 2, 22.2), "band", .75),        # blue rim
        ],
        "lowerArmR": [
            box((-31, 26, -9), (16, 24, 14), "wrap", .75, tag="wrap"),
            box((-31.6, 27, -9.6), (17.2, 6, 15.2), "band", .75),         # blue bracer
            box((-31.6, 44, -9.6), (17.2, 3, 15.2), "iron", .75),
        ],
        "handR": [
            box((-31, 13, -9.5), (16, 13, 15), "skin", 1, tag="fist"),
            box((-32, 17, -10.5), (3, 7, 8), "skin", 1),
        ],
    }


def geometry():
    shin = [
        box((-15, 7, -7), (13, 16, 13), "navy", .75, tag="leg_wrap"),
        box((-15.6, 0, -8.6), (14.2, 10, 15.2), "boot", .75),
        box((-16, 0, -14), (15, 5, 20), "boot", 1),
        box((-16.3, 9, -9), (14.8, 3, 16), "band", .75),
    ]
    arms = arm_r()
    replacement = {
        "club": log(),
        "hips": [
            box((-14, 30, -9), (28, 13, 18), "navy", .5),
            box((-16, 35, -11), (32, 6, 22), "belt", .75),
            box((-4, 34.5, -12.2), (8, 7, 2), "iron", 1),
            box((-12, 17, -11.2), (24, 19, 2), "navy", .5, tag="leg_wrap"),
            box((-12, 18, 9.2), (24, 18, 2), "navy", .5),
        ],
        "torso": [
            box((-17, 38, -14), (34, 22, 25), "wrap", .5, tag="wrap"),
            box((-20, 56, -11), (40, 18, 23), "wrap", .5, tag="wrap"),
            box((-17, 64, -6), (34, 13, 19), "iron", .5, tag="plates"),       # back hump plate
            box((-3.5, 42, -15.6), (7, 24, 2), "iron", .75, tag="plates"),    # chest harness plate
            box((-20.5, 61, -12.4), (13, 3, 24.6), "belt", .5),                 # strap under the log
        ],
        "head": [
            box((-10, 71, -23), (20, 15, 16), "iron", 1, tag="helm"),
            box((-9, 86, -21), (18, 1.5, 12), "iron", 1),
            box((-10.6, 80, -23.8), (21.2, 4, 17.4), "steel", 1, tag="brow"),
            box((-6.5, 72, -26), (13, 8, 3), "grille_iron", 1, tag="grille"),
            box((-11, 72, -20), (1.5, 9, 10), "iron", 1),                      # cheek guards
            box((9.5, 72, -20), (1.5, 9, 10), "iron", 1),
        ],
        "jaw": [
            box((-7.5, 66, -25), (15, 7, 13), "iron", 1),
            box((-6.5, 67, -26), (13, 5, 1.5), "grille_iron", 1, tag="grille_low"),
        ],
        "legR": [box((-16, 20, -8), (15, 18, 16), "navy", .5, tag="leg_wrap")],
        "legL": [box((1, 20, -8), (15, 18, 16), "navy", .5, tag="leg_wrap")],
        "shinR": shin,
        "shinL": [mirror_box(part) for part in shin],
        **arms,
        "armL": [mirror_box(b) for b in arms["armR"]],
        "lowerArmL": [mirror_box(b) for b in arms["lowerArmR"]],
        "handL": [mirror_box(b) for b in arms["handR"]],
    }
    bones = [(name, parent, pivot, replacement.get(name, boxes)) for name, parent, pivot, boxes in BONES]
    materials = dict(MATERIALS,
                     iron=((58, 61, 76), 7, (74, 78, 96), .12),
                     steel=((150, 154, 166), 7, (122, 126, 140), .10),
                     grille_iron=((96, 100, 118), 5, None, 0, "plain"),
                     wrap=((206, 204, 212), 8, (184, 182, 194), .10, "stitch"),
                     skin=((214, 196, 192), 7, (196, 176, 174), .08),
                     band=((56, 98, 172), 8, (40, 76, 140), .10, "strap"),
                     navy=((40, 45, 70), 8, (30, 34, 54), .12, "stitch"),
                     belt=((34, 36, 48), 6, None, 0, "strap"),
                     boot=((32, 33, 44), 6, (46, 48, 62), .10),
                     log=((56, 56, 74), 9, (76, 78, 100), .16, "grain"),
                     log_end=((88, 86, 100), 4, None, 0, "plain"))
    decals = {"wrap": wrap_stripes, "grille": grille, "grille_low": lambda img, f: grille_low(img, f),
              "helm": helm, "brow": brow_band, "plates": plates, "fist": knuckles,
              "rings": log_rings, "bark": log_bark, "leg_wrap": leg_stripes}
    return build("logbreaker", bones, materials, decals, seed=412, visible=(11, 9, 3.6), output_dir=PACK)


def grille_low(img, face):
    if face != "north":
        return
    h, w = img.shape[:2]
    for v in range(0, h, 2):
        img[v, 1:w - 1] = SLIT


def leg_stripes(img, face):
    """Dark navy cloth with a faint blue band every few rows."""
    if face in ("up", "down"):
        return
    for v in range(2, img.shape[0], 5):
        img[v] = img[v] * 0.5 + np.array((58, 92, 160)) * 0.5


def two_hand(lib, name):
    """Search the left arm so the left fist sits on GRIP_L; writes the values into the pose."""
    rig = lib.rig
    base = beat_values({"pose": name}, lib, [], name)
    keys = {c: [((b, ch, a), sign) for b, ch, a, sign in rig.controls[c]] for c in LEFT_ARM}
    values = {c: float(lib.pose(name).get(c, 0.0)) for c in LEFT_ARM}
    mirror = {"arm_l.raise": "arm_r.raise", "arm_l.out": "arm_r.out", "elbow_l.bend": "elbow_r.bend"}
    for c, r in mirror.items():
        values[c] = float(lib.pose(name).get(r, values[c]))

    def miss(vals):
        trial = dict(base)
        for c, v in vals.items():
            for key, sign in keys[c]:
                trial[key] = sign * v
        mats = lib.model.world_matrices(channels_to_pose(trial))
        return float(np.linalg.norm(np.array(lib.model.point(mats, "handL", FIST_L))
                                    - np.array(lib.model.point(mats, "club", GRIP_L))))

    def descend(values):
        best = miss(values)
        for step in (24, 12, 6, 3, 1.5, .75):
            improved = True
            while improved:
                improved = False
                for c in LEFT_ARM:
                    lo, hi = rig.limits.get(c, (-180, 180))
                    for d in (step, -step):
                        trial = dict(values, **{c: min(hi, max(lo, values[c] + d))})
                        m = miss(trial)
                        if m < best - 1e-6:
                            values, best, improved = trial, m, True
        return values, best

    starts = [values] + [dict(values, **{"arm_l.raise": r, "arm_l.out": o, "elbow_l.bend": e})
                         for r in (40, 90, 140, 180) for o in (-30, 0, 30) for e in (10, 60, 110)]
    values, best = min((descend(v) for v in starts), key=lambda vb: vb[1])
    if best > 3:
        print(f"{name}: left fist misses the log grip by {best / 16:.2f} blocks")
    if best > 99:
        raise ValueError(f"{name}: left fist misses the log grip by {best / 16:.2f} blocks")
    lib.poses[name].update({c: round(v, 1) for c, v in values.items()})
    return best


def animations(rig):
    lib = Library(rig)
    for name in ("stand", "stand_breathe", "walk_contact_r", "walk_pass_l", "walk_contact_l", "walk_pass_r"):
        lib.poses[name].pop("solve", None)
        lib.poses[name].update(CARRY)
    for name in ("hurt", "death_kneel"):
        lib.poses[name].pop("solve", None)
    for name, pose in SLAM_POSES.items():
        lib.poses[name] = dict(pose)
        print(f"{name}: left fist {two_hand(lib, name) / 16:.2f} blocks from the grip")
    docs = [load_yaml(rig.dir / f"{name}.anim.yaml") for name in ("idle", "walk", "hurt", "death")]
    # Grab the log with both hands, heave it overhead, and slam it down in front.
    docs.append({
        "name": "log_slam", "length": 2.35,
        "beats": [
            {"t": 0, "pose": "stand"},
            {"t": .38, "pose": "lb_grab", "ease": "smooth"},
            {"t": .78, "pose": "lb_windup", "ease": "out", "hold": .16},
            {"t": 1.10, "pose": "lb_swing", "ease": "in"},
            {"t": IMPACT, "pose": "lb_hit", "ease": "strike", "hold": .30},
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
    # The log lies on the ground from his feet to its tip: the damage circle covers that line.
    reach = -z / 16
    print(f"Impact: forward={-z / 16:.2f} right={x / 16:.2f} height={y / 16:.2f} blocks")
    for event in clips["log_slam"]["events"]:
        if event["type"] == "camera_shake":
            event.update(forward=round(-z / 16, 3), side=round(x / 16, 3))
        if event["type"] == "hit":
            event.update(forward=round(reach / 2 + .3, 2), side=round(x / 32, 2), radius=round(reach / 2 + .3, 2))
    return clips


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--preview", type=Path)
    args = parser.parse_args()
    rig = RigConfig("troll")
    rig.geo, rig.texture = geometry()
    rig.tips["club"]["at"] = LOG_TIP
    clips = animations(rig)
    payload = {"format_version": "1.8.0", "rig": {"head": "head", "shadow": 1.9}, "animations": clips}
    (PACK / "animations.json").write_text(json.dumps(payload, separators=(",", ":")) + "\n", encoding="utf-8")
    if args.preview:
        clip = Clip("log_slam", clips["log_slam"])
        frames = [(label, clip.sample(t)) for label, t in
                  (("Carry", 0), ("Grab", .38), ("Windup", .85), ("Swing", 1.10), ("Impact", IMPACT))]
        rig.preview["side"] = .48
        rig.preview["three_quarter"] = .52
        render_sheet(Scene(rig), frames, "Logbreaker", ("front", "three_quarter", "side"),
                     cell=(300, 330), ppu=1.35).save(args.preview)


if __name__ == "__main__":
    main()
