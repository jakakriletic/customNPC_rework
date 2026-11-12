"""Build the giant spider model (geo.json) and paint its texture.

    py -3 tools/models/build_giant_spider.py

A cave spider the size of a cart: body about 1.2 blocks high, legs spanning
almost five blocks.  Bedrock / Blockbench coordinates: 1 unit = 1 pixel =
1/16 block, Y up, the spider faces -Z (north), x < 0 is ITS RIGHT side.

The BIND POSE IS NOT A STANDING POSE: every leg sticks straight out sideways
(easy to model); the `stand` pose in animations_src/giant_spider/poses.yaml
raises the thighs, bends the knees down and spreads the legs.  rig.yaml says
`floor: 0` so the compiler knows where the ground is.

Bones:  body > thorax > head > fangR, fangL
               thorax > abdomen
               thorax > legR1..legR4 > legR1b..legR4b   (and L)
Legs are numbered from the front (1) to the back (4).
"""
import sys
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from geobuild import box, mirror_bones, px, build  # noqa: E402

LEG_Z = {1: -9, 2: -5, 3: -1, 4: 3}      # where each leg leaves the thorax
HIP_X, HIP_Y = 7, 14
UPPER, LOWER = 13, 22                     # segment lengths (px)


def right_leg(n):
    z = LEG_Z[n]
    knee = HIP_X + UPPER
    return [
        (f"legR{n}", "thorax", (-HIP_X, HIP_Y, z), [
            box((-knee, HIP_Y - 1.5, z - 1.5), (UPPER, 3, 3), "leg", 1.0, tag="leg_upper"),
            box((-knee - 0.5, HIP_Y - 2, z - 2), (3, 4, 4), "leg_dark", 1.0),            # knee joint
        ]),
        (f"legR{n}b", f"legR{n}", (-knee, HIP_Y, z), [
            box((-knee - LOWER, HIP_Y - 1, z - 1), (LOWER, 2, 2), "leg_dark", 1.0, tag="leg_lower"),
            box((-knee - LOWER - 1.5, HIP_Y - 0.75, z - 0.75), (1.5, 1.5, 1.5), "claw", 1.0),
        ]),
    ]


RIGHT_LEGS = [b for n in (1, 2, 3, 4) for b in right_leg(n)]
LEFT = {f"legR{n}": f"legL{n}" for n in (1, 2, 3, 4)}
LEFT.update({f"legR{n}b": f"legL{n}b" for n in (1, 2, 3, 4)})

BONES = [
    ("body", None, (0, 0, 0), []),
    ("thorax", "body", (0, 14, -2), [
        box((-7, 9, -11), (14, 10, 15), "chitin", tag="thorax"),
        box((-5, 18.5, -9), (10, 1.5, 11), "chitin_dark", 1.0),                 # plate on the back
    ]),
    ("abdomen", "thorax", (0, 15, 3), [
        box((-11, 9, 2), (22, 18, 25), "chitin_dark", tag="abdomen"),
        box((-8, 26.5, 5), (16, 2, 19), "chitin_dark", 1.0, tag="abdomen_top"),   # rounded top
        box((-2.5, 12, 26.5), (5, 5, 3), "leg_dark", 1.0, tag="spinneret"),
    ]),
    ("head", "thorax", (0, 14, -11), [
        box((-5.5, 8.5, -19), (11, 9, 8), "chitin", 1.0, tag="head"),
        box((-4.5, 15, -19.8), (9, 3.5, 2), "eyes", 1.0, tag="eyes"),
        box((-6.5, 8, -15), (2.5, 3, 3), "leg", 1.0),                            # pedipalps
        box((4, 8, -15), (2.5, 3, 3), "leg", 1.0),
    ]),
    ("fangR", "head", (-2.5, 10, -18), [
        box((-4, 3.5, -20), (3, 7, 3), "chitin_dark", 1.0),
        box((-3.5, 1.5, -20.5), (2, 2.5, 2), "fang", 1.0),
    ]),
    ("fangL", "head", (2.5, 10, -18), [
        box((1, 3.5, -20), (3, 7, 3), "chitin_dark", 1.0),
        box((1.5, 1.5, -20.5), (2, 2.5, 2), "fang", 1.0),
    ]),
] + RIGHT_LEGS + mirror_bones(RIGHT_LEGS, LEFT)


# --------------------------------------------------------------------- paint
MATERIALS = {
    "chitin": ((84, 70, 60), 10, (58, 48, 42), 0.12, "fur"),
    "chitin_dark": ((60, 50, 46), 9, (42, 36, 34), 0.10, "fur"),
    "leg": ((92, 76, 64), 9, (64, 52, 44), 0.10, "fur"),
    "leg_dark": ((66, 54, 48), 8, (46, 38, 34), 0.08, "fur"),
    "claw": ((24, 20, 20), 4, None, 0.0, "plain"),
    "fang": ((214, 204, 176), 5, None, 0.0, "plain"),
    "eyes": ((30, 10, 12), 4, None, 0.0, "plain"),
}


def decal_eyes(img, face):
    if face != "north":
        return
    h, w = img.shape[:2]       # 9 x 4 (density 1, rounded)
    glow, dim = (255, 46, 40), (170, 20, 24)
    for u, v, c in ((1, 2, glow), (3, 1, glow), (5, 1, glow), (7, 2, glow),
                    (2, 3, dim), (6, 3, dim), (4, 2, dim), (0, 1, dim), (8, 1, dim)):
        px(img, u, v, c)


def decal_abdomen(img, face):
    h, w = img.shape[:2]
    if face == "up":           # red hourglass and side chevrons on the back
        cu = w // 2
        for v in range(h):
            half = abs(v - h // 2)
            width = max(1, half // 2)
            if 2 < v < h - 2:
                img[v, cu - width:cu + width] = img[v, cu - width:cu + width] * 0.3 + np.array([170, 26, 22]) * 0.7
        for v in range(3, h - 3, 4):
            for k in range(3):
                px(img, 2 + k, v + k, (120, 30, 26))
                px(img, w - 3 - k, v + k, (120, 30, 26))
    if face in ("east", "west"):
        img[h // 2, :] *= 0.75


def decal_abdomen_top(img, face):
    if face == "up":
        h, w = img.shape[:2]
        cu = w // 2
        img[2:h - 2, cu - 1:cu + 1] = img[2:h - 2, cu - 1:cu + 1] * 0.3 + np.array([180, 30, 26]) * 0.7


def decal_head(img, face):
    if face == "north":
        img[-2:, :] *= 0.7


def decal_leg(img, face):
    h, w = img.shape[:2]
    if face in ("up", "north", "south"):       # pale rings along the leg
        for u in range(2, w, 4):
            img[:, u] = img[:, u] * 0.5 + np.array([120, 104, 92]) * 0.5


DECALS = {"eyes": decal_eyes, "abdomen": decal_abdomen, "abdomen_top": decal_abdomen_top,
          "head": decal_head, "leg_upper": decal_leg}


if __name__ == "__main__":
    build("giant_spider", BONES, MATERIALS, DECALS, atlas_width=128, seed=808, visible=(6, 3, 1.0))
