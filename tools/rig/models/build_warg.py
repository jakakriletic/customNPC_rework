"""Build the warg model (geo.json) and paint its texture.

    py -3 tools/models/build_warg.py

A huge grey wolf: shoulder about 1.6 blocks, ears at 2.6 blocks, nearly five
blocks from nose to tail tip.  Bedrock / Blockbench coordinates: 1 unit = 1
pixel = 1/16 block, Y up, the warg faces -Z (north), x < 0 is ITS RIGHT side.

Bone chain:  body > chest > neck > head > jaw
                    chest > legFR > legFR2 > pawFR      (front legs, and L)
                    body > hips > legHR > legHR2 > pawHR   (hind legs, and L)
                                hips > tail > tail2

Check the result with   py -3 tools/anim/preview.py model warg --face
"""
import sys
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from geobuild import box, mirror_bones, px, build  # noqa: E402


# --------------------------------------------------------------------- model
FRONT_R = [
    ("legFR", "chest", (-5.5, 25, -15), [
        box((-9, 13, -19), (7, 15, 8), "fur", tag="shoulder_r"),
    ]),
    ("legFR2", "legFR", (-5.5, 14, -15.5), [
        box((-8.5, 3, -18), (6, 12, 5), "fur_dark"),
    ]),
    ("pawFR", "legFR2", (-5.5, 3, -15.5), [
        box((-9, 0, -20.5), (7, 3.5, 7), "fur_dark", 1.0, tag="paw"),
        box((-8.8, 0, -21.5), (1.4, 1.4, 1.5), "claw", 1.0),
        box((-6.3, 0, -21.8), (1.4, 1.4, 1.5), "claw", 1.0),
        box((-3.8, 0, -21.5), (1.4, 1.4, 1.5), "claw", 1.0),
    ]),
]

HIND_R = [
    ("legHR", "hips", (-5.5, 25, 13), [
        box((-9.5, 13, 7), (8, 15, 11), "fur", tag="thigh_r"),
    ]),
    ("legHR2", "legHR", (-5.5, 14, 13), [
        # the hock: angled shin from the knee down and back
        box((-8.5, 4, 12), (5, 11, 5), "fur_dark", rotation=(-18, 0, 0), pivot=(-6, 14, 14.5)),
    ]),
    ("pawHR", "legHR2", (-6, 4, 16), [
        box((-9, 0, 11), (6, 4.5, 7), "fur_dark", 1.0, tag="paw"),
        box((-8.8, 0, 10), (1.4, 1.4, 1.5), "claw", 1.0),
        box((-6.3, 0, 9.7), (1.4, 1.4, 1.5), "claw", 1.0),
        box((-3.8, 0, 10), (1.4, 1.4, 1.5), "claw", 1.0),
    ]),
]

LEFT = {"legFR": "legFL", "legFR2": "legFL2", "pawFR": "pawFL",
        "legHR": "legHL", "legHR2": "legHL2", "pawHR": "pawHL"}

BONES = [
    ("body", None, (0, 0, 0), []),
    ("chest", "body", (0, 25, -6), [
        box((-9.5, 16, -23), (19, 18, 21), "fur", tag="ribs"),
        box((-11, 21, -26), (22, 16, 11), "mane", tag="mane"),            # shaggy ruff over the shoulders
        box((-7, 14.5, -21), (14, 4, 17), "fur_light", tag="chest_under"),
        box((-3, 33, -16), (6, 3, 12), "mane", 1.0),                       # hackles along the spine
    ]),
    ("hips", "body", (0, 25, -2), [
        box((-8, 17, -3), (16, 14, 21), "fur", tag="flank"),
        box((-6, 15.5, -2), (12, 3, 15), "fur_light"),
        box((-2.5, 30.5, -3), (5, 2, 14), "fur_dark", 1.0),                # dark stripe on the back
    ]),
    ("tail", "hips", (0, 28, 17), [
        box((-3, 23.5, 16), (6, 7, 10), "fur", rotation=(-30, 0, 0), pivot=(0, 28, 17)),
    ]),
    ("tail2", "tail", (0, 23, 24.5), [
        box((-2.5, 19.5, 23.5), (5, 6, 11), "fur_dark", rotation=(-48, 0, 0), pivot=(0, 23, 24.5)),
        box((-2, 20, 33.5), (4, 4.5, 5), "fur_light", 1.0, rotation=(-48, 0, 0), pivot=(0, 23, 24.5)),
    ]),
    ("neck", "chest", (0, 31, -21), [
        box((-6, 24, -31), (12, 13, 11), "mane", tag="neck"),
    ]),
    ("head", "neck", (0, 32, -29), [
        box((-6.5, 27, -40), (13, 11, 11), "fur", 1.0, tag="skull"),
        box((-7.5, 29, -35), (2.5, 7, 6), "fur_light", 1.0),               # cheek ruff
        box((5, 29, -35), (2.5, 7, 6), "fur_light", 1.0),
        box((-6, 37, -34), (4, 6, 2.5), "fur_dark", 1.0, rotation=(-10, 0, -12), pivot=(-4, 37, -33), tag="ear"),
        box((2, 37, -34), (4, 6, 2.5), "fur_dark", 1.0, rotation=(-10, 0, 12), pivot=(4, 37, -33), tag="ear"),
        box((-4, 29, -49), (8, 6, 10), "fur_light", 1.0, tag="snout"),
        box((-1.5, 33.6, -49.6), (3, 1.8, 2), "nose", 1.0, tag="nose"),
        box((-3.8, 28.3, -48.6), (1.1, 1.8, 1.1), "tooth", 1.0),           # fangs over the lower lip
        box((2.7, 28.3, -48.6), (1.1, 1.8, 1.1), "tooth", 1.0),
    ]),
    ("jaw", "head", (0, 29.5, -38), [
        box((-3.5, 25.5, -48), (7, 3.5, 10), "fur_light", 1.0, tag="jaw"),
        box((-3.2, 28.6, -47.6), (1, 1.6, 1), "tooth", 1.0),
        box((2.2, 28.6, -47.6), (1, 1.6, 1), "tooth", 1.0),
    ]),
] + FRONT_R + mirror_bones(FRONT_R, LEFT) + HIND_R + mirror_bones(HIND_R, LEFT)


# --------------------------------------------------------------------- paint
MATERIALS = {
    # base rgb, noise, spot rgb, spot density, style
    "fur": ((134, 128, 118), 11, (108, 102, 94), 0.06, "fur"),
    "fur_dark": ((98, 92, 86), 10, (76, 72, 68), 0.06, "fur"),
    "fur_light": ((192, 184, 166), 9, (166, 158, 142), 0.05, "fur"),
    "mane": ((88, 82, 78), 12, (64, 60, 56), 0.10, "fur"),
    "nose": ((28, 24, 24), 4, None, 0.0, "plain"),
    "tooth": ((232, 226, 204), 5, None, 0.0, "plain"),
    "claw": ((48, 42, 38), 5, None, 0.0, "plain"),
}


def decal_skull(img, face):
    h, w = img.shape[:2]       # north: 13 x 11, left of the image = ITS RIGHT
    if face == "north":
        eye, glow = (255, 214, 64), (255, 160, 30)
        for cx in (2, 9):      # slanted, glowing eyes under a dark brow
            px(img, cx, 4, eye)
            px(img, cx + 1, 4, eye)
            px(img, cx + (1 if cx < 6 else 0), 5, glow)
            for du in range(-1, 3):
                px(img, cx + du, 3, img[3, min(w - 1, max(0, cx + du))] * 0.45)
        px(img, 3, 5, (20, 16, 14))   # pupils
        px(img, 9, 5, (20, 16, 14))
        img[0:2, :] *= 0.8
        img[h - 2:, 4:w - 4] *= 0.75  # shadow where the snout starts
    if face == "up":
        img[:, w // 2 - 1:w // 2 + 1] *= 0.7   # dark stripe over the head


def decal_snout(img, face):
    h, w = img.shape[:2]
    if face == "north":
        img[h - 1, :] *= 0.5           # mouth line
        img[0, :] *= 0.85
    if face in ("east", "west"):
        img[h - 1, :] *= 0.55          # lip line along the side
        for u in range(1, w - 1, 3):
            px(img, u, h - 2, (60, 50, 46))   # whisker dots
    if face == "up":
        img[:, :] *= 0.9


def decal_jaw(img, face):
    h, w = img.shape[:2]
    if face == "up":                   # inside of the mouth
        img[:] = np.array([132, 52, 56]) + np.random.default_rng(5).normal(0, 6, (h, w, 1))
        img[:, 0] = (222, 216, 196)
        img[:, -1] = (222, 216, 196)
    if face == "north":
        img[0, :] *= 0.55


def decal_ear(img, face):
    if face == "north":
        h, w = img.shape[:2]
        img[1:, 1:w - 1] = img[1:, 1:w - 1] * 0.4 + np.array([150, 104, 100]) * 0.6   # pink inside


def decal_mane(img, face):
    h, w = img.shape[:2]
    if face in ("east", "west", "north"):
        rng = np.random.default_rng(11)
        for u in range(w):             # ragged lower edge
            cut = rng.integers(0, 3)
            img[h - cut:, u] *= 0.7


def decal_paw(img, face):
    if face == "down":
        h, w = img.shape[:2]
        img[:] = img * 0.5 + np.array([46, 36, 36]) * 0.5     # dark pads


def decal_flank(img, face):
    h, w = img.shape[:2]
    if face == "east":                 # three old claw scars on its left flank
        for i in range(3):
            for k in range(4):
                px(img, 4 + k + i * 2, 2 + k, img[2 + k, min(w - 1, 4 + k + i * 2)] * 0.7)


DECALS = {"skull": decal_skull, "snout": decal_snout, "jaw": decal_jaw, "ear": decal_ear,
          "mane": decal_mane, "neck": decal_mane, "paw": decal_paw, "flank": decal_flank}


if __name__ == "__main__":
    build("warg", BONES, MATERIALS, DECALS, atlas_width=256, seed=4242, visible=(6, 4, 1.4))
