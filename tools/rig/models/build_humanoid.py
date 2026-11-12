"""Build the humanoid family: humanoid (soldier), knight, skeleton_warrior.

    py -3 tools/rig/models/build_humanoid.py            # all three
    py -3 tools/rig/models/build_humanoid.py knight     # one

All three share ONE skeleton (same bone names and pivots), so every clip of
rig_src/humanoid/ plays on all of them (compile.py humanoid copies the clips to
knight and skeleton_warrior).  Player-sized: 2 blocks tall, 1 unit = 1 pixel,
Bedrock coordinates, faces -Z, x < 0 is HIS RIGHT.

Bones:  body > hips > torso > head
                       torso > armR > lowerArmR > handR > weapon
                       torso > armL > lowerArmL > handL ; lowerArmL > shield
               hips  > legR > shinR ; legL > shinL
Every model has every bone (an empty one where it has nothing to show), so a
clip never names a bone a model lacks.
"""
import sys
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from geobuild import box, mirror_box, px, build  # noqa: E402

# Pivots shared by the whole family (joints).
PIV = {
    "body": (0, 0, 0), "hips": (0, 12, 0), "torso": (0, 13, 0), "head": (0, 24, 0),
    "armR": (-5.5, 22.5, 0), "lowerArmR": (-5.75, 17.5, 0), "handR": (-5.75, 12, 0), "weapon": (-5.75, 11, 0),
    "armL": (5.5, 22.5, 0), "lowerArmL": (5.75, 17.5, 0), "handL": (5.75, 12, 0), "shield": (8, 15, 0),
    "legR": (-2, 12, 0), "shinR": (-2, 6.5, 0), "legL": (2, 12, 0), "shinL": (2, 6.5, 0),
}
PARENT = {
    "body": None, "hips": "body", "torso": "hips", "head": "torso",
    "armR": "torso", "lowerArmR": "armR", "handR": "lowerArmR", "weapon": "handR",
    "armL": "torso", "lowerArmL": "armL", "handL": "lowerArmL", "shield": "lowerArmL",
    "legR": "hips", "shinR": "legR", "legL": "hips", "shinL": "legL",
}
ORDER = list(PIV)


def mirrored(boxes):
    return [mirror_box(b) for b in boxes]


def assemble(parts):
    """parts: bone -> boxes (missing bones are empty)."""
    return [(name, PARENT[name], PIV[name], parts.get(name, [])) for name in ORDER]


def sword(blade="iron", grip="leather", guard="iron_dark", length=14):
    """Held in the right fist, pointing forward (-Z) in the bind pose (arm hanging down)."""
    return [
        box((-6.25, 10.5, -2.5), (1, 1, 5), grip, 1.0),
        box((-7.75, 9.75, -3.5), (4, 2.5, 1), guard, 1.0, tag="guard"),
        box((-6.5, 10.25, -3.5 - length), (1.5, 1.5, length), blade, 1.0, tag="blade"),
        box((-6.5, 10.25, 2.5), (1.5, 1.5, 1), guard, 1.0),
    ]


# ------------------------------------------------------------------ soldier
def humanoid():
    arm_r = [box((-7.25, 17.5, -1.75), (3.5, 5.5, 3.5), "tunic", 1.0),
             box((-7.5, 21, -2), (4, 2.5, 4), "tunic_dark", 1.0)]
    fore_r = [box((-7.25, 12.5, -1.75), (3.5, 5, 3.5), "skin", 1.0),
              box((-7.5, 12.5, -2), (4, 2, 4), "leather", 1.0)]
    hand_r = [box((-7.25, 10.5, -1.75), (3, 2, 3.5), "skin", 1.0, tag="fist")]
    leg_r = [box((-4, 6.5, -2), (4, 6, 4), "pants", 1.0)]
    shin_r = [box((-3.9, 1.5, -1.9), (3.8, 5, 3.8), "pants", 1.0),
              box((-4.1, 0, -2.6), (4.2, 3, 4.6), "boot", 1.0, tag="boot")]
    parts = {
        "hips": [box((-4, 10, -2.25), (8, 3, 4.5), "pants", 1.0),
                 box((-4.25, 11.5, -2.5), (8.5, 1.5, 5), "belt", 1.0, tag="belt")],
        "torso": [box((-4, 13, -2), (8, 11, 4), "tunic", 1.0, tag="chest"),
                  box((-4.25, 13, -2.25), (8.5, 3, 4.5), "tunic_dark", 1.0)],
        "head": [box((-4, 24, -4), (8, 8, 8), "skin", 2.0, tag="face"),
                 box((-4.25, 29.5, -4.25), (8.5, 2.75, 8.5), "hair", 1.0, tag="hair"),
                 box((-4.25, 25, 1.5), (8.5, 5, 2.75), "hair", 1.0)],
        "armR": arm_r, "lowerArmR": fore_r, "handR": hand_r, "weapon": sword(),
        "armL": mirrored(arm_r), "lowerArmL": mirrored(fore_r), "handL": mirrored(hand_r),
        "legR": leg_r, "shinR": shin_r, "legL": mirrored(leg_r), "shinL": mirrored(shin_r),
    }
    mats = {
        "skin": ((214, 168, 130), 6, (196, 150, 112), 0.03),
        "hair": ((76, 52, 32), 8, (56, 38, 24), 0.2),
        "tunic": ((132, 40, 36), 8, (110, 32, 30), 0.05, "stitch"),
        "tunic_dark": ((98, 30, 28), 6, None, 0.0, "stitch"),
        "pants": ((84, 70, 54), 8, (70, 58, 44), 0.05, "stitch"),
        "boot": ((62, 42, 28), 6, None, 0.0, "strap"),
        "leather": ((104, 72, 44), 6, None, 0.0, "strap"),
        "belt": ((70, 48, 30), 4, None, 0.0, "strap"),
        "iron": ((196, 200, 206), 6, None, 0.0, "plain"),
        "iron_dark": ((120, 122, 128), 6, None, 0.0, "plain"),
    }
    return assemble(parts), mats, {"face": face_human, "fist": fist, "blade": blade, "belt": belt}


# ------------------------------------------------------------------ knight
def knight():
    arm_r = [box((-7.5, 17.5, -2), (4, 5.5, 4), "mail", 1.0),
             box((-8.25, 19.5, -2.75), (5, 4, 5.5), "plate", 1.0, tag="pauldron")]
    fore_r = [box((-7.5, 12.5, -2), (4, 5, 4), "plate", 1.0),
              box((-7.75, 12.5, -2.25), (4.5, 1.5, 4.5), "plate_dark", 1.0)]
    hand_r = [box((-7.25, 10.5, -1.75), (3, 2, 3.5), "plate_dark", 1.0, tag="fist")]
    leg_r = [box((-4.1, 6.5, -2.1), (4.2, 6, 4.2), "mail", 1.0),
             box((-4.3, 7, -2.6), (4.6, 4, 1), "plate", 1.0)]
    shin_r = [box((-4, 1.5, -2), (4, 5, 4), "plate", 1.0, tag="greave"),
              box((-4.2, 0, -2.8), (4.4, 2.5, 5), "plate_dark", 1.0)]
    parts = {
        "hips": [box((-4.25, 9, -2.4), (8.5, 4, 4.8), "mail", 1.0),
                 box((-4.5, 11.5, -2.75), (9, 1.5, 5.5), "belt", 1.0, tag="belt"),
                 box((-3, 4.5, -2.7), (6, 7, 0.5), "tabard", 1.0, tag="tabard_low")],
        "torso": [box((-4.25, 13, -2.25), (8.5, 11, 4.5), "plate", 1.0, tag="cuirass"),
                  box((-3.5, 13.5, -2.6), (7, 9.5, 0.5), "tabard", 1.0, tag="tabard"),
                  box((-3.5, 13.5, 2.1), (7, 9.5, 0.5), "tabard", 1.0)],
        "head": [box((-4.4, 23.6, -4.4), (8.8, 8.8, 8.8), "plate", 2.0, tag="helm"),
                 box((-1, 32.4, -3), (2, 2.5, 7), "plume", 1.0, tag="plume"),
                 box((-0.5, 31.5, 4.4), (1, 2, 1), "plume", 1.0)],
        "armR": arm_r, "lowerArmR": fore_r, "handR": hand_r,
        "weapon": sword(blade="steel", grip="leather", guard="gold", length=16),
        "armL": mirrored(arm_r), "lowerArmL": mirrored(fore_r), "handL": mirrored(hand_r),
        # Heater shield strapped to the outside of the left forearm, facing out (+X).
        "shield": [box((8, 9, -5), (1, 11, 10), "shield", 1.0, tag="shield_face"),
                   box((8.6, 13.5, -1.5), (0.8, 3, 3), "gold", 1.0, tag="shield_boss")],
        "legR": leg_r, "shinR": shin_r, "legL": mirrored(leg_r), "shinL": mirrored(shin_r),
    }
    mats = {
        "plate": ((176, 182, 190), 8, (150, 156, 164), 0.03, "plain"),
        "plate_dark": ((120, 126, 134), 6, None, 0.0, "plain"),
        "mail": ((128, 132, 138), 10, (96, 100, 106), 0.25, "scales"),
        "steel": ((214, 220, 228), 4, None, 0.0, "plain"),
        "gold": ((214, 172, 56), 6, None, 0.0, "plain"),
        "tabard": ((36, 64, 150), 6, (30, 54, 128), 0.03, "stitch"),
        "plume": ((180, 30, 34), 10, (150, 24, 28), 0.2, "fur"),
        "leather": ((104, 72, 44), 6, None, 0.0, "strap"),
        "belt": ((70, 48, 30), 4, None, 0.0, "strap"),
        "shield": ((36, 64, 150), 6, None, 0.0, "plain"),
    }
    return assemble(parts), mats, {"helm": helm, "fist": fist, "blade": blade, "tabard": tabard,
                                   "shield_face": shield_face, "belt": belt}


# ------------------------------------------------------------------ skeleton
def skeleton_warrior():
    arm_r = [box((-6.5, 17.5, -1), (2, 5.5, 2), "bone", 1.0),
             box((-7.25, 20.5, -1.75), (3.5, 2.5, 3.5), "rust", 1.0)]
    fore_r = [box((-6.5, 12.5, -1), (2, 5, 2), "bone", 1.0)]
    hand_r = [box((-7, 10.5, -1.5), (2.5, 2, 3), "bone", 1.0, tag="claw")]
    leg_r = [box((-3, 6.5, -1), (2, 6, 2), "bone", 1.0)]
    shin_r = [box((-3, 1, -1), (2, 5.5, 2), "bone", 1.0),
              box((-3.5, 0, -2.5), (3, 1.5, 4), "bone", 1.0)]
    parts = {
        "hips": [box((-3.5, 10, -1.5), (7, 2.5, 3), "bone", 1.0),
                 box((-4, 8, -2), (8, 4, 4), "rag", 1.0, tag="rag")],
        "torso": [box((-1, 13, -0.5), (2, 11, 1.5), "bone", 1.0),                 # spine
                  box((-4, 17, -2), (8, 6, 4), "bone", 1.0, tag="ribs"),
                  box((-4.5, 19, -2.5), (9, 5, 5), "rust", 1.0, tag="breastplate")],
        "head": [box((-4, 24, -4), (8, 8, 8), "bone", 2.0, tag="skull"),
                 box((-4.5, 28.5, -4.5), (9, 4, 9), "rust", 1.0, tag="cap")],
        "armR": arm_r, "lowerArmR": fore_r, "handR": hand_r,
        "weapon": sword(blade="rust_blade", grip="rag", guard="rust", length=13),
        "armL": mirrored(arm_r), "lowerArmL": mirrored(fore_r), "handL": mirrored(hand_r),
        "shield": [box((7.5, 10, -4.5), (1, 9, 9), "wood", 1.0, tag="round_shield")],
        "legR": leg_r, "shinR": shin_r, "legL": mirrored(leg_r), "shinL": mirrored(shin_r),
    }
    mats = {
        "bone": ((222, 216, 196), 8, (190, 182, 160), 0.06, "plain"),
        "rust": ((120, 78, 46), 14, (86, 54, 34), 0.3),
        "rust_blade": ((150, 120, 96), 14, (110, 74, 48), 0.3, "plain"),
        "rag": ((70, 64, 52), 10, (52, 46, 38), 0.2, "stitch"),
        "wood": ((106, 74, 42), 10, (84, 58, 32), 0.0, "grain"),
    }
    return assemble(parts), mats, {"skull": skull, "ribs": ribs, "claw": fist, "blade": blade}


# ------------------------------------------------------------------ decals
def face_human(img, face):
    if face != "north":
        return
    h, w = img.shape[:2]                 # 16 x 16 (density 2), left = HIS RIGHT
    white, iris, brow = (240, 236, 228), (60, 90, 140), (70, 48, 30)
    for cx in (4, 10):
        for u in range(cx, cx + 2):
            px(img, u, 8, white)
            px(img, u, 9, white)
        px(img, cx + (1 if cx < 8 else 0), 8, iris)
        px(img, cx + (1 if cx < 8 else 0), 9, iris)
        for u in range(cx - 1, cx + 3):
            px(img, u, 6, brow)
    for v in (10, 11):
        px(img, 7, v, img[v, 7] * 0.82)
        px(img, 8, v, img[v, 8] * 0.82)
    for u in range(6, 10):
        px(img, u, 13, (150, 90, 80))


def helm(img, face):
    h, w = img.shape[:2]
    if face == "north":
        img[7:9, 2:w - 2] = (24, 24, 28)            # eye slit
        for v in range(10, 15, 2):
            for u in range(5, w - 5, 2):
                px(img, u, v, (40, 40, 46))         # breathing holes
        img[:, w // 2 - 1:w // 2 + 1] *= 0.9        # centre ridge
    if face in ("east", "west"):
        img[6:8, :] *= 0.8


def skull(img, face):
    if face != "north":
        return
    h, w = img.shape[:2]
    dark = (30, 22, 18)
    for cx in (3, 10):
        img[6:10, cx:cx + 3] = dark                  # eye sockets
        px(img, cx + 1, 8, (220, 60, 30))           # ember eyes
    img[10:12, 7:9] = dark                           # nose hole
    for u in range(4, 12):
        px(img, u, 13, dark if u % 2 else (200, 194, 172))   # teeth


def ribs(img, face):
    if face in ("north", "south"):
        for v in range(1, img.shape[0], 2):
            img[v, :] *= 0.55


def fist(img, face):
    w = img.shape[1]
    if face in ("north", "down"):
        for i in (1, 2):
            img[:, int(w * i / 3)] *= 0.75


def blade(img, face):
    if face in ("up", "down", "east", "west"):
        img[:, :] *= 1.0
        if img.shape[1] > 2:
            img[:, img.shape[1] // 2] *= 1.12       # fuller shine


def belt(img, face):
    if face == "north":
        w = img.shape[1]
        img[:, w // 2 - 1:w // 2 + 1] = (200, 170, 70)   # buckle


def tabard(img, face):
    if face == "north":
        h, w = img.shape[:2]
        gold = (214, 172, 56)
        for v in range(2, h - 2):
            px(img, w // 2, v, gold)                 # cross
        for u in range(1, w - 1):
            px(img, u, h // 3, gold)


def shield_face(img, face):
    if face == "east":
        h, w = img.shape[:2]
        gold = (214, 172, 56)
        img[0, :] = gold
        img[-1, :] = gold
        img[:, 0] = gold
        img[:, -1] = gold
        for v in range(2, h - 2):
            px(img, w // 2, v, gold)
        for u in range(2, w - 2):
            px(img, u, h // 2 - 1, gold)


MODELS = {"humanoid": humanoid, "knight": knight, "skeleton_warrior": skeleton_warrior}

if __name__ == "__main__":
    wanted = sys.argv[1:] or list(MODELS)
    for name in wanted:
        bones, mats, decals = MODELS[name]()
        build(name, bones, mats, decals, atlas_width=128, visible=(3, 3, 1))
