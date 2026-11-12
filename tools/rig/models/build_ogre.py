"""Build the ogre model: the troll skeleton (same bones, so every troll clip plays on it),
with horns, tusks, a darker hide, a war-paint chest and an iron-banded club.

    py -3 tools/models/build_ogre.py

The model is written as a list of named bones with boxes in Bedrock / Blockbench
coordinates: 1 unit = 1 pixel = 1/16 block, Y up, the troll faces -Z (north),
x < 0 is HIS RIGHT side.  Every box names a material; the painter fills each
face of the box in the texture atlas from that material (noise, shading,
details) and `DECALS` add hand-drawn features such as the face.

Output:
    dev/src/patch/resources/assets/customnpcs/rig/ogre.geo.json
    dev/src/patch/resources/assets/customnpcs/textures/rig/ogre.png

Check the result with   py -3 tools/anim/preview.py model ogre
"""
import sys
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from geobuild import box, mirror_box, px, build  # noqa: E402


def nails_on_club():
    """Iron nails driven through the club head (head spans x -28.5..-17.5, y 12.5..23.5, z -52..-32)."""
    out = []
    for z in (-49, -43, -37):           # through the top
        out.append(box((-23.1, 22.5, z), (1.2, 4.5, 1.2), "iron", 1.0))
    for z in (-47, -40):                # out of the bottom
        out.append(box((-23.1, 8.5, z), (1.2, 4.5, 1.2), "iron", 1.0))
    for z, y in ((-50, 19), (-44, 15), (-37, 20)):   # out of his right side (outer)
        out.append(box((-32, y, z), (4.5, 1.2, 1.2), "iron", 1.0))
    for z, y in ((-48, 16), (-41, 20)):              # inner side
        out.append(box((-17.5, y, z), (4.5, 1.2, 1.2), "iron", 1.0))
    out.append(box((-23.1, 17.5, -55.5), (1.2, 1.2, 4.5), "iron", 1.0))  # tip spike
    out.append(box((-26.5, 21.5, -50), (1.2, 4, 1.2), "iron", 1.0, rotation=(-25, 0, 20), pivot=(-26, 22, -49.5)))
    return out


ARM_R = [
    ("armR", "torso", (-21, 70, -1), [
        box((-30, 47, -8), (14, 27, 13), "skin", tag="upper_arm_r"),
        box((-31, 64, -9), (10, 9, 15), "skin_dark", tag="shoulder_r"),      # big deltoid
        box((-31.5, 60, -9.5), (8, 2, 16), "strap", 0.5),                     # leather arm band
    ]),
    ("lowerArmR", "armR", (-23, 49, -1.5), [
        box((-31, 26, -9), (16, 24, 14), "skin", tag="forearm_r"),
        box((-31.5, 27, -9.5), (17, 6, 15), "strap", 0.5),                    # bracer
    ]),
    ("handR", "lowerArmR", (-23, 26, -1.5), [
        box((-31, 13, -9.5), (16, 13, 15), "skin_dark", 1.0, tag="fist_r"),
        box((-32, 17, -10.5), (3, 7, 8), "skin_dark", 1.0, tag="thumb_r"),
    ]),
    # Club in his right fist, pointing forward in the bind pose.
    ("club", "handR", (-23, 19, -1.5), [
        box((-25.5, 16.5, -24), (5, 5, 33), "wood", tag="handle"),
        box((-25.5, 15.8, 8.5), (5, 6.4, 3), "wood_dark", 1.0),               # knob at the end
        box((-27, 14.5, -35), (8, 9, 13), "wood", tag="club_mid"),
        box((-28.5, 12.5, -52), (11, 11, 20), "wood", tag="club_head"),
        box((-29, 13.5, -34), (12, 9, 2), "wood_dark", 1.0),                  # rope binding ring
    ] + nails_on_club()),
]

ARM_L = [
    ("armL", "torso", (21, 70, -1), [mirror_box(b) for b in ARM_R[0][3]]),
    ("lowerArmL", "armL", (23, 49, -1.5), [mirror_box(b) for b in ARM_R[1][3]]),
    ("handL", "lowerArmL", (23, 26, -1.5), [mirror_box(b) for b in ARM_R[2][3]]),
]

LEG_R = [
    ("legR", "hips", (-8, 36, 0), [
        box((-16, 20, -8), (15, 18, 16), "skin", tag="thigh_r"),
    ]),
    ("shinR", "legR", (-8.5, 22, -0.5), [
        box((-15, 5, -7), (13, 18, 13), "skin", tag="shin_r"),
        box((-16, 0, -13), (15, 6, 19), "skin_dark", 1.0, tag="foot_r"),
        box((-15.5, 0, -15), (4, 4, 3), "toenail", 1.0),
        box((-10.5, 0, -15.5), (4, 4.5, 3), "toenail", 1.0),
        box((-5.8, 0, -14.5), (3.6, 3.5, 2), "toenail", 1.0),
        box((-15.6, 14, -7.6), (13.2, 4, 14.2), "fur", 0.5),                  # rag wrap
    ]),
]
LEG_L = [
    ("legL", "hips", (8, 36, 0), [mirror_box(b) for b in LEG_R[0][3]]),
    ("shinL", "legL", (8.5, 22, -0.5), [mirror_box(b) for b in LEG_R[1][3]]),
]

BONES = [
    ("body", None, (0, 0, 0), []),
    ("hips", "body", (0, 38, 0), [
        box((-14, 31, -9), (28, 12, 18), "skin", tag="pelvis"),
        box((-15.5, 35, -10.5), (31, 5, 21), "strap", 0.5, tag="belt"),
        box((-11, 16, -11), (22, 20, 2), "cloth", 0.5, tag="loin_front"),
        box((-12, 18, 8.5), (24, 18, 2), "cloth", 0.5, tag="loin_back"),
        box((-3, 34, -12.5), (6, 7, 2.5), "bone", 1.0, tag="buckle"),          # skull buckle
    ]),
    ("torso", "hips", (0, 42, 1), [
        box((-17, 38, -15), (34, 23, 27), "belly", tag="belly"),
        box((-20, 56, -11), (40, 18, 23), "skin", tag="chest"),
        box((-15, 67, -5), (30, 10, 18), "skin_dark", tag="hump"),
        box((-7, 69, -16), (14, 7, 10), "skin", 1.0, tag="neck"),
        box((-20.5, 60, -12.6), (13, 3, 24.5), "strap", 0.5),                 # strap over the right shoulder
    ]),
    ("head", "torso", (0, 73, -10), [
        box((-9, 72, -24), (18, 14, 16), "skin", 1.0, tag="skull"),
        box((-10, 80.5, -25.5), (20, 3.5, 4), "skin_dark", 1.0, tag="brow"),
        box((-2.5, 74, -28), (5, 8, 4.5), "skin_dark", 1.0, tag="nose"),         # long droopy nose
        box((-3, 72.5, -29), (6, 3.5, 4), "skin_dark", 1.0, tag="nose_tip"),
        box((-3.5, 85.5, -21), (7, 4, 13), "hair", 1.0, tag="hair"),
        box((-2.5, 84, -9.5), (5, 3, 4), "hair", 1.0),                         # topknot tail
        box((-16, 77, -16.5), (7.5, 4, 1.5), "skin", 1.0, rotation=(0, -25, -18), pivot=(-9, 79, -16), tag="ear_r"),
        box((8.5, 77, -16.5), (7.5, 4, 1.5), "skin", 1.0, rotation=(0, 25, 18), pivot=(9, 79, -16), tag="ear_l"),
        box((-12, 84, -17), (4, 9, 4), "tooth", 1.0, rotation=(-20, 0, 25), pivot=(-10, 84, -15), tag="horn"),
        box((8, 84, -17), (4, 9, 4), "tooth", 1.0, rotation=(-20, 0, -25), pivot=(10, 84, -15), tag="horn"),
    ]),
    ("jaw", "head", (0, 74, -12), [
        box((-9.5, 66.5, -25.5), (19, 7, 14), "skin", 1.0, tag="jaw"),
        box((-7.6, 73, -26.4), (1.8, 3.2, 1.8), "tooth", 1.0, rotation=(-6, 0, 10), pivot=(-6.7, 73, -25.5)),
        box((5.8, 73, -26.4), (1.8, 3.2, 1.8), "tooth", 1.0, rotation=(-6, 0, -10), pivot=(6.7, 73, -25.5)),
        box((-9, 72, -24.5), (2.5, 7, 2.5), "tooth", 1.0, rotation=(-10, 0, 8), pivot=(-8, 72, -23.5)),
        box((6.5, 72, -24.5), (2.5, 7, 2.5), "tooth", 1.0, rotation=(-10, 0, -8), pivot=(8, 72, -23.5)),
    ]),
] + LEG_R + LEG_L + ARM_R + ARM_L

# Props: bones named prop_* are hidden unless a clip shows them (scale > 0).
# Kept last so adding props never moves the texture of the body.
PROPS = [
    ("prop_rock", "handL", (23, 10, -3), [            # a boulder in his free left hand (throw clip)
        box((15, 2, -11), (16, 14, 16), "stone", tag="rock"),
        box((17, 13, -9), (11, 5, 11), "stone_dark"),
        box((14, 5, -7), (3, 8, 9), "stone_dark"),
    ]),
]
BONES = BONES + PROPS



# --------------------------------------------------------------------- paint
MATERIALS = {
    # base rgb, noise amount, spot colour (or None), spot density
    "skin": ((150, 118, 96), 14, (124, 94, 76), 0.06),
    "skin_dark": ((122, 92, 74), 12, (98, 72, 58), 0.05),
    "belly": ((176, 146, 120), 12, (150, 120, 98), 0.04),
    "hair": ((24, 20, 18), 10, (14, 12, 10), 0.15),
    "cloth": ((128, 34, 30), 12, (100, 26, 24), 0.06),
    "strap": ((74, 50, 32), 8, (56, 38, 24), 0.04),
    "fur": ((96, 86, 72), 16, (70, 62, 50), 0.12),
    "wood": ((86, 60, 36), 10, (66, 46, 26), 0.0),
    "wood_dark": ((80, 54, 30), 8, None, 0.0),
    "iron": ((150, 152, 158), 10, (118, 84, 58), 0.10),
    "tooth": ((226, 214, 172), 6, None, 0.0),
    "toenail": ((186, 170, 120), 8, (150, 132, 90), 0.1),
    "bone": ((218, 206, 170), 8, None, 0.0),
    "stone": ((124, 120, 112), 14, (94, 90, 86), 0.14),
    "stone_dark": ((98, 94, 90), 12, (76, 74, 70), 0.12),
}

def decal_skull(img, face):
    if face != "north":
        return
    h, w = img.shape[:2]       # 18 x 14, left of the image = HIS RIGHT
    eye = (250, 196, 40)
    pupil = (150, 20, 10)
    for cx in (4, 13):         # two small sunken eyes right under the brow
        for du in range(0, 2):
            px(img, cx + du, 6, eye)
            px(img, cx + du, 7, (230, 170, 30))
        px(img, cx + 1, 6, pupil)
        px(img, cx + 1, 7, pupil)
        for du in range(-1, 3):  # dark sockets
            px(img, cx + du, 5, img[5, min(w - 1, max(0, cx + du))] * 0.55)
            px(img, cx + du, 8, img[8, min(w - 1, max(0, cx + du))] * 0.7)
    # wrinkles and a scar across his left cheek
    for i in range(4):
        px(img, 12 + i, 10 + (i // 2), (118, 92, 72))
    img[h - 1, 2:w - 2] *= 0.55  # upper lip shadow (the jaw sits just below)
    for u in range(5, 13):       # snarl line of the upper teeth
        px(img, u, h - 2, (52, 36, 28) if u % 3 else (200, 190, 150))


def decal_jaw(img, face):
    if face != "north":
        return
    h, w = img.shape[:2]       # 19 x 7
    for u in range(2, w - 2):  # bottom teeth row along the lip, crooked
        if u % 2 == 0 or u in (5, 12):
            px(img, u, 0, (214, 202, 160))
        else:
            px(img, u, 0, (60, 36, 30))
    img[1, 2:w - 2] *= 0.82
    img[h - 1, :] *= 0.8       # chin underside shade
    px(img, w // 2, 4, img[4, w // 2] * 0.75)  # chin dimple


def decal_nose(img, face):
    if face == "north":
        h, w = img.shape[:2]
        px(img, 1, h - 1, (40, 30, 24))
        px(img, w - 2, h - 1, (40, 30, 24))
        img[0:2, :] *= 0.9
    if face == "down":
        img[:] *= 0.7


def decal_brow(img, face):
    if face == "north":
        img[-1, :] *= 0.6        # heavy shadow over the eyes
        img[0, 3:-3] = img[0, 3:-3] * 0.4 + np.array([44, 38, 30]) * 0.6   # bushy brows


def decal_fist(img, face):
    h, w = img.shape[:2]
    if face in ("north", "down", "east", "west"):
        # knuckle / finger separation lines
        n = 4
        for i in range(1, n):
            u = int(w * i / n)
            img[:, u] *= 0.72
        if face == "north":
            img[1, :] *= 1.12
            for i in range(n):
                px(img, int(w * (i + 0.5) / n), h - 2, (186, 170, 120))   # dirty claws


def decal_belly(img, face):
    if face == "north":
        h, w = img.shape[:2]
        cv, cu = int(h * 0.62), w // 2
        px(img, cu, cv, (72, 84, 44))          # navel
        img[0:2, :] *= 0.85                    # chest overhang shadow
        for u in range(1, w - 1, 2):           # stretch marks
            if (u * 7) % 5 == 0:
                px(img, u, h // 3, img[h // 3, u] * 0.88)


def decal_buckle(img, face):
    if face == "north":
        h, w = img.shape[:2]
        for u, v in ((1, 2), (4, 2)):           # skull eye holes
            px(img, u, v, (30, 24, 20))
        for u in range(1, w - 1, 2):
            px(img, u, h - 2, (60, 50, 40))


def decal_hair(img, face):
    h, w = img.shape[:2]
    rng = np.random.default_rng(3)
    for _ in range(w * h // 3):
        u, v = rng.integers(0, w), rng.integers(0, h)
        img[v, u] *= 0.75


DECALS = {"skull": decal_skull, "jaw": decal_jaw, "nose": decal_nose, "nose_tip": decal_nose, "brow": decal_brow,
          "fist_r": decal_fist, "fist_l": decal_fist, "belly": decal_belly,
          "buckle": decal_buckle, "hair": decal_hair}


if __name__ == "__main__":
    build("ogre", BONES, MATERIALS, DECALS, visible=(8, 7, 2.8))
