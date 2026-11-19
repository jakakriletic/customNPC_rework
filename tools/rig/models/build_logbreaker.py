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
# The same pack ships inside the mod jar; the server installs it into customnpcs/creatures.
BUNDLED = ROOT / "dev/src/patch/resources/assets/customnpcs/rig/creatures/logbreaker"
PACK_FILES = ("creature.json", "animations.json", "model.geo.json", "texture.png", "script.js")
IMPACT = 1.25
LOG_LEN = 90

# Carried log: bone `log` (child of the torso) rests on the right shoulder, front end low in
# front of him, back end rising behind his head, and the right arm hugs it: elbow under the
# front of the log, forearm over it, fist on top (owner's reference photos).
SHOULDER = (-25, 88, 4)                # where the log's axis lies on the shoulder (pivot)
SHOULDER_TILT = (16, 6, 0)             # bind rotation: back end up, slightly away from the head
SHOULDER_SPAN = (-40, 50)              # log along z through SHOULDER (front, back)
HUG_FIST = (-25, 102, -24)             # in the log's frame: fist on top, in front of the shoulder
HUG_ELBOW = (-31, 74, -26)             # elbow under the front of the log

# Swung log: prop `prop_log` in the right fist, shown only while both hands hold it.
# Its axis runs beside the fist (fist on the bark, not inside it).
HAND_X, HAND_Y = -12, 19.5
HAND_SPAN = (-78, 12)                  # along the fist's z: far end, near end
LOG_TIP = [HAND_X, HAND_Y, HAND_SPAN[0]]
FIST_R = (-23, 19.5, -2)
FIST_L = (23, 19.5, -2)                # centres of the fists in their bind pose
ELBOW_R = (-23, 49, -1.5)
GRIP_L = (-23, 19.5, -20)              # left fist on the log beside the right one
RIGHT_ARM = ("arm_r.raise", "arm_r.out", "arm_r.swing", "elbow_r.bend", "wrist_r.bend", "wrist_r.in")
LEFT_ARM = ("arm_l.raise", "arm_l.out", "arm_l.swing", "elbow_l.bend", "wrist_l.bend", "wrist_l.in")
SHOULDER_LOG, HAND_LOG = {"log.shoulder": 1, "log.hand": 0}, {"log.shoulder": 0, "log.hand": 1}

# Two-handed overhead slam. The right arm drives the log; the left arm is solved onto
# the grip so both fists hold the log through the whole swing.
CENTER = {"tip": "hand_r", "using": "arm_r.out", "side": .1}   # right fist to the middle
SLAM_POSES = {
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
CARRIED = ("stand", "stand_breathe", "walk_contact_r", "walk_pass_l", "walk_contact_l", "walk_pass_r", "hurt")

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
    """Sawn end: pale wood with growth rings and a dark heart."""
    if face not in ("north", "south"):
        return
    y, x = np.indices(img.shape[:2])
    r = np.hypot(x - (img.shape[1] - 1) * .55, y - (img.shape[0] - 1) * .45)
    img[:] = (178, 140, 94)
    img[(r.astype(int) % 3) == 0] = (138, 102, 64)
    img[r < 1.5] = (96, 66, 40)
    img[r > img.shape[0] / 2 - .5] = (120, 86, 54)
    img[(x + y * 2) % 11 == 0] *= 0.82                       # radial crack


def log_bark(img, face):
    """Deep grooves along the trunk with pale grey-blue ridges."""
    rng = np.random.default_rng(img.shape[0] * 31 + img.shape[1] + len(face))
    h, w = img.shape[:2]
    if face in ("north", "south"):
        return
    along_u = face in ("east", "west", "up", "down")       # z runs along u on these faces
    for line in range(0, h if along_u else w, 2):
        a = rng.integers(0, max(1, (w if along_u else h) - 8))
        n = rng.integers(6, 22)
        if along_u:
            img[line, a:a + n] *= 0.62
            if line + 1 < h and rng.random() < .5:
                img[line + 1, a + 2:a + n // 2] = (106, 108, 124)
        else:
            img[a:a + n, line] *= 0.62
            if line + 1 < w and rng.random() < .5:
                img[a + 2:a + n // 2, line + 1] = (106, 108, 124)


def log(x, y, z0, z1, root_at_z1=True):
    """A real round log along z from z0 to z1 with its axis at (x, y): three overlapping boxes
    give the round section, sawn ends show rings, a thicker root end, knots, a broken branch."""
    n = z1 - z0
    rz = z1 - 26 if root_at_z1 else z0
    m = lambda t: z0 + n * t                                    # position along the log
    return [
        box((x - 11, y - 6.5, z0), (22, 13, n), "bark", .5, tag="bark"),
        box((x - 6.5, y - 11, z0 + .3), (13, 22, n - .6), "bark", .5, tag="bark"),
        box((x - 9.5, y - 9.5, z0 + .15), (19, 19, n - .3), "bark", .5, tag="bark"),
        box((x - 12, y - 7.5, rz + .2), (24, 15, 25.6), "bark", .5, tag="bark"),     # root flare
        box((x - 7.5, y - 12, rz + .35), (15, 24, 25.3), "bark", .5, tag="bark"),
        box((x - 8, y - 8, z0 - .5), (16, 16, .6), "wood", 1, tag="rings"),          # sawn ends
        box((x - 9, y - 9, z1 - .1), (18, 18, .6), "wood", 1, tag="rings"),
        box((x + 9.5, y - 2, m(.42)), (4, 5, 5), "bark", 1),                        # knots
        box((x - 13, y + 1, m(.7)), (3, 4, 4), "bark", 1),
        box((x - 3, y - 12.5, m(.22)), (5, 3, 6), "bark", 1),
        box((x - 2, y + 9, m(.55)), (4, 12, 4), "bark", 1,                          # broken branch
            rotation=(-28, 0, 14), pivot=(x, y + 9, m(.55) + 2)),
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
        "club": [],
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
    bones.append(("log", "torso", SHOULDER, log(SHOULDER[0], SHOULDER[1], *SHOULDER_SPAN), SHOULDER_TILT))
    bones.append(("prop_log", "handR", FIST_R, log(HAND_X, HAND_Y, *HAND_SPAN, root_at_z1=False)))
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
                     bark=((70, 60, 58), 10, (96, 98, 116), .10, "grain"),
                     wood=((178, 140, 94), 6, None, 0, "plain"))
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


def reach(lib, name, controls, goals, weights=None, start=None):
    """Search `controls` (degrees) so each (bone, point, target bone, target point) pair meets;
    writes the found values into the pose and returns the worst miss in px."""
    rig = lib.rig
    weights = weights or [1.0] * len(goals)
    base = beat_values({"pose": name}, lib, [], name)
    keys = {c: [((b, ch, a), sign) for b, ch, a, sign in rig.controls[c]] for c in controls}
    values = {c: float(lib.pose(name).get(c, 0.0)) for c in controls}
    values.update(start or {})

    def misses(vals):
        trial = dict(base)
        for c, v in vals.items():
            for key, sign in keys[c]:
                trial[key] = sign * v
        mats = lib.model.world_matrices(channels_to_pose(trial))
        return [float(np.linalg.norm(np.array(lib.model.point(mats, b, p))
                                     - np.array(lib.model.point(mats, tb, tp)))) for b, p, tb, tp in goals]

    def cost(vals):
        return sum(w * m * m for w, m in zip(weights, misses(vals)))

    def descend(values):
        best = cost(values)
        for step in (24, 12, 6, 3, 1.5, .75):
            improved = True
            while improved:
                improved = False
                for c in controls:
                    lo, hi = rig.limits.get(c, (-180, 180))
                    for d in (step, -step):
                        trial = dict(values, **{c: min(hi, max(lo, values[c] + d))})
                        m = cost(trial)
                        if m < best - 1e-6:
                            values, best, improved = trial, m, True
        return values, best

    raise_, out, bend = controls[0], controls[1], controls[3]
    starts = [values] if start else [values] + [dict(values, **{raise_: r, out: o, bend: e})
                                                for r in (40, 100, 160) for o in (-20, 20) for e in (20, 100)]
    values, _ = min((descend(v) for v in starts), key=lambda vb: vb[1])
    lib.poses[name].update({c: round(v, 1) for c, v in values.items()})
    return misses(values)


def two_hand(lib, name):
    """Left fist onto the swung log beside the right fist."""
    return max(reach(lib, name, LEFT_ARM, [("handL", FIST_L, "prop_log", GRIP_L)]))


def hug(lib, name, start=None):
    """Right arm around the shoulder log: fist on top of it, elbow under its front."""
    return reach(lib, name, RIGHT_ARM, [("handR", FIST_R, "log", HUG_FIST), ("lowerArmR", ELBOW_R, "log", HUG_ELBOW)],
                 weights=[1.0, .35], start=start)[0]


def shoulder_log_in_torso(lib, point):
    """A point of the shoulder log in the torso's bind frame (Bedrock coordinates)."""
    p = lib.model.point(lib.model.world_matrices(), "log", point)
    return [-p[0], p[1], p[2]]


def take_off_shoulder(lib, name):
    """Right arm so the swung log lies exactly where the shoulder log is: the swap between
    the two bones is invisible."""
    near, far = HAND_SPAN[1] - 8, HAND_SPAN[0] + 8
    goals = [("prop_log", [HAND_X, HAND_Y, near], "torso",
              shoulder_log_in_torso(lib, [SHOULDER[0], SHOULDER[1], SHOULDER_SPAN[0] + 8])),
             ("prop_log", [HAND_X, HAND_Y, far], "torso",
              shoulder_log_in_torso(lib, [SHOULDER[0], SHOULDER[1], SHOULDER_SPAN[1] - 8]))]
    return max(reach(lib, name, RIGHT_ARM, goals))


def animations(rig):
    lib = Library(rig)
    for name in CARRIED + ("death_kneel",):
        lib.poses[name].pop("solve", None)
    first = None
    for name in CARRIED:
        miss = hug(lib, name, first)
        first = first or {c: lib.poses[name][c] for c in RIGHT_ARM}
        print(f"{name}: right fist {miss / 16:.2f} blocks from its hold on the log")
    # Lift: both hands take the log on the shoulder, then the shoulder log is swapped for the
    # held one at the same place (lb_take), so the log never jumps.
    lib.poses["lb_take"] = dict({"base": "stand", "head.nod": -16}, **HAND_LOG)
    miss = take_off_shoulder(lib, "lb_take")
    print(f"lb_take: held log {miss / 16:.2f} blocks from the shoulder log")
    print(f"lb_take: left fist {two_hand(lib, 'lb_take') / 16:.2f} blocks from the grip")
    lib.poses["lb_lift"] = dict(lib.poses["lb_take"], **SHOULDER_LOG)
    for name, pose in SLAM_POSES.items():
        lib.poses[name] = dict(pose, **HAND_LOG)
        print(f"{name}: left fist {two_hand(lib, name) / 16:.2f} blocks from the grip")
    docs = [load_yaml(rig.dir / f"{name}.anim.yaml") for name in ("idle", "walk", "hurt", "death")]
    # Grab the log with both hands, heave it overhead, and slam it down in front.
    docs.append({
        "name": "log_slam", "length": 2.35,
        "beats": [
            {"t": 0, "pose": "stand"},
            {"t": .34, "pose": "lb_lift", "ease": "smooth"},
            {"t": .36, "pose": "lb_take"},
            {"t": .78, "pose": "lb_windup", "ease": "out", "hold": .16},
            {"t": 1.10, "pose": "lb_swing", "ease": "in"},
            {"t": IMPACT, "pose": "lb_hit", "ease": "strike", "hold": .25},
            {"t": 1.92, "pose": "lb_take", "ease": "smooth"},
            {"t": 1.94, "pose": "lb_lift"},
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
    rig.controls["log.shoulder"] = [("log", "scale", 3, 1.0)]       # 0 = hidden while swung
    rig.controls["log.hand"] = [("prop_log", "scale", 3, 1.0)]       # 1 = held in both hands
    rig.tips["club"] = {"bone": "prop_log", "at": LOG_TIP}
    rig.tips["log_hold"] = {"bone": "log", "at": list(HUG_FIST)}     # helper points
    rig.tips["log_grip_l"] = {"bone": "prop_log", "at": list(GRIP_L)}
    clips = animations(rig)
    payload = {"format_version": "1.8.0", "rig": {"head": "head", "shadow": 1.9}, "animations": clips}
    (PACK / "animations.json").write_text(json.dumps(payload, separators=(",", ":")) + "\n", encoding="utf-8")
    BUNDLED.mkdir(parents=True, exist_ok=True)
    for name in PACK_FILES:
        (BUNDLED / name).write_bytes((PACK / name).read_bytes())
    if args.preview:
        clip = Clip("log_slam", clips["log_slam"])
        frames = [(label, clip.sample(t)) for label, t in
                  (("Carry", 0), ("Lift", .34), ("Take", .36), ("Windup", .85), ("Swing", 1.10), ("Impact", IMPACT))]
        rig.preview["side"] = .48
        rig.preview["three_quarter"] = .52
        render_sheet(Scene(rig), frames, "Logbreaker", ("front", "three_quarter", "side"),
                     cell=(300, 330), ppu=1.35).save(args.preview)


if __name__ == "__main__":
    main()
