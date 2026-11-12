"""Build the cave bear model (geo.json) and paint its texture.

    py -3 tools/models/build_cave_bear.py

A huge brown cave bear: shoulder hump at about 2.8 blocks, close to five
blocks long.  Scaffolded from the warg (tools/anim/new_entity.py) and reshaped;
it keeps the warg's bone names, so animations_src/cave_bear/rig.yaml works the
same way.  Bedrock / Blockbench coordinates: 1 unit = 1 pixel = 1/16 block,
Y up, the bear faces -Z (north), x < 0 is ITS RIGHT side.

Bone chain:  body > chest > neck > head > jaw
                    chest > legFR > legFR2 > pawFR      (front legs, and L)
                    body > hips > legHR > legHR2 > pawHR   (hind legs, and L)
                                hips > tail > tail2

Check the result with   py -3 tools/anim/preview.py model cave_bear --face
"""
import sys
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from geobuild import box, mirror_bones, px, build  # noqa: E402


def claws(x0, z_front):
    """Three long pale claws sticking out of the front of a paw."""
    return [box((x0 + k * 3.2, 0, z_front - 2.5), (1.6, 1.6, 3), "claw", 1.0) for k in range(3)]


FRONT_R = [
    ("legFR", "chest", (-8, 30, -16), [
        box((-13.5, 14, -21.5), (11, 19, 11), "fur", tag="shoulder_r"),
    ]),
    ("legFR2", "legFR", (-8, 15, -16), [
        box((-13, 4, -20.5), (10, 12, 9.5), "fur_dark"),
    ]),
    ("pawFR", "legFR2", (-8, 4, -16), [
        box((-13.5, 0, -23), (11, 5, 12), "fur_dark", 1.0, tag="paw"),
    ] + claws(-13, -23)),
]

HIND_R = [
    ("legHR", "hips", (-7.5, 31, 12), [
        box((-14, 14, 4), (13, 19, 15), "fur", tag="thigh_r"),
    ]),
    ("legHR2", "legHR", (-7.5, 15, 12), [
        box((-13, 4, 8.5), (10, 12, 10), "fur_dark"),
    ]),
    ("pawHR", "legHR2", (-7.5, 4, 13), [
        box((-13.5, 0, 5.5), (11, 5, 13), "fur_dark", 1.0, tag="paw"),
    ] + claws(-13, 5.5)),
]

LEFT = {"legFR": "legFL", "legFR2": "legFL2", "pawFR": "pawFL",
        "legHR": "legHL", "legHR2": "legHL2", "pawHR": "pawHL"}

BONES = [
    ("body", None, (0, 0, 0), []),
    ("chest", "body", (0, 30, -6), [
        box((-12.5, 17, -27), (25, 25, 27), "fur", tag="ribs"),
        box((-10.5, 40, -22), (21, 6, 17), "fur_dark", tag="hump"),          # shoulder hump
        box((-10, 15.5, -25), (20, 4, 22), "fur_light"),
    ]),
    ("hips", "body", (0, 29, -2), [
        box((-11.5, 17, -4), (23, 22, 25), "fur", tag="flank"),
        box((-9, 37.5, -2), (18, 3, 18), "fur_dark"),
    ]),
    ("tail", "hips", (0, 33, 21), [
        box((-3, 29.5, 20), (6, 6, 4), "fur_dark", 1.0),
    ]),
    ("tail2", "tail", (0, 31, 24), [
        box((-2, 30, 23.5), (4, 3.5, 2), "fur_dark", 1.0),
    ]),
    ("neck", "chest", (0, 36, -24), [
        box((-9, 25, -36), (18, 16, 13), "fur_dark", tag="neck"),
    ]),
    ("head", "neck", (0, 36, -33), [
        box((-9, 28, -47), (18, 14, 14), "fur", 1.0, tag="skull"),
        box((-9.5, 41, -40), (5, 5, 3), "fur_dark", 1.0, tag="ear"),
        box((4.5, 41, -40), (5, 5, 3), "fur_dark", 1.0, tag="ear"),
        box((-5.5, 28, -56), (11, 8, 10), "muzzle", 1.0, tag="snout"),
        box((-2.5, 33.5, -57), (5, 3, 2.5), "nose", 1.0),
        box((-4.5, 27.2, -55.5), (1.3, 2, 1.3), "tooth", 1.0),
        box((3.2, 27.2, -55.5), (1.3, 2, 1.3), "tooth", 1.0),
    ]),
    ("jaw", "head", (0, 30, -45), [
        box((-5, 24.5, -55), (10, 4, 10), "muzzle", 1.0, tag="jaw"),
    ]),
] + FRONT_R + mirror_bones(FRONT_R, LEFT) + HIND_R + mirror_bones(HIND_R, LEFT)


# --------------------------------------------------------------------- paint
MATERIALS = {
    "fur": ((108, 78, 54), 11, (86, 62, 42), 0.07, "fur"),
    "fur_dark": ((80, 58, 40), 10, (60, 44, 30), 0.07, "fur"),
    "fur_light": ((132, 102, 74), 9, (110, 84, 60), 0.05, "fur"),
    "muzzle": ((150, 120, 88), 8, (126, 100, 72), 0.05, "fur"),
    "nose": ((26, 20, 18), 4, None, 0.0, "plain"),
    "tooth": ((232, 224, 200), 5, None, 0.0, "plain"),
    "claw": ((214, 204, 182), 5, None, 0.0, "plain"),
}


def decal_skull(img, face):
    h, w = img.shape[:2]            # north: 18 x 14, left of the image = ITS RIGHT
    if face == "north":
        for cx in (3, 13):          # small dark eyes with a glint
            px(img, cx, 6, (20, 14, 12))
            px(img, cx + 1, 6, (20, 14, 12))
            px(img, cx, 7, (36, 24, 20))
            px(img, cx + 1, 6, (180, 150, 80))
            for du in range(-1, 3):
                px(img, cx + du, 5, img[5, min(w - 1, max(0, cx + du))] * 0.6)
        img[h - 3:, 4:w - 4] *= 0.8
    if face == "up":
        img[:, :] *= 0.95


def decal_snout(img, face):
    h, w = img.shape[:2]
    if face == "north":
        img[h - 1, :] *= 0.5
    if face in ("east", "west"):
        img[h - 1, :] *= 0.6


def decal_jaw(img, face):
    h, w = img.shape[:2]
    if face == "up":                # inside of the mouth
        img[:] = np.array([128, 50, 52]) + np.random.default_rng(6).normal(0, 6, (h, w, 1))
        img[:, 0] = (226, 218, 196)
        img[:, -1] = (226, 218, 196)


def decal_ear(img, face):
    if face == "north":
        h, w = img.shape[:2]
        img[1:h - 1, 1:w - 1] = img[1:h - 1, 1:w - 1] * 0.5 + np.array([70, 50, 40]) * 0.5


def decal_paw(img, face):
    if face == "down":
        img[:] = img * 0.4 + np.array([40, 30, 28]) * 0.6


def decal_hump(img, face):
    if face in ("up", "east", "west"):
        rng = np.random.default_rng(12)
        h, w = img.shape[:2]
        for _ in range(w * h // 4):
            u, v = rng.integers(0, w), rng.integers(0, h)
            img[v, u] *= 0.8        # shaggy, darker tips


DECALS = {"skull": decal_skull, "snout": decal_snout, "jaw": decal_jaw, "ear": decal_ear,
          "paw": decal_paw, "hump": decal_hump}


if __name__ == "__main__":
    build("cave_bear", BONES, MATERIALS, DECALS, atlas_width=256, seed=1717, visible=(6, 4, 1.6))
