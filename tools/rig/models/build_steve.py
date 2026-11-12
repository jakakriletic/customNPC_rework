"""Build the steve family: the plain player body with the standard 64x64 skin layout, split at the
elbows and knees, so it wears ANY player skin (the NPC's own CustomNPCs skin: file, URL or player name).

    py -3 tools/rig/models/build_steve.py

Writes, per model:
    rig/<id>.geo.json        the body, UVs of a 64x64 skin (inner + outer layer: hat, jacket, sleeves, pants)
    rig/<id>_gear.geo.json   + textures/rig/<id>_gear.png   what it holds (same bones, own painted texture)
The body has no texture of its own: it is drawn with the NPC's CustomNPCs skin (rig "skin": true).

Models: steve (empty hands), legionary (gladius + scutum; skin = the NPC's skin, default plain steve).
Same bone names as the humanoid family, so every rig_src/humanoid clip plays on them.
"""
import json
import sys
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

sys.path.insert(0, str(Path(__file__).resolve().parent))
from geobuild import box, build, ROOT, ASSETS  # noqa: E402
import build_humanoid as hum  # noqa: E402

# Player-shaped joints (Blockbench player template), Bedrock coordinates, x < 0 = HIS RIGHT.
PIV = {
    "body": (0, 0, 0), "hips": (0, 12, 0), "torso": (0, 12, 0), "head": (0, 24, 0),
    "armR": (-5, 22, 0), "lowerArmR": (-6, 18, 0), "handR": (-6, 12.5, 0), "weapon": (-6, 11.5, 0),
    "armL": (5, 22, 0), "lowerArmL": (6, 18, 0), "handL": (6, 12.5, 0), "shield": (8, 15, 0),
    "legR": (-1.9, 12, 0), "shinR": (-1.9, 6, 0), "legL": (1.9, 12, 0), "shinL": (1.9, 6, 0),
}
PARENT = hum.PARENT
ORDER = list(PIV)


def faces(U, V, w, h, d, rows=None):
    """Per-face UVs of a vanilla box at skin offset U,V (size w x h x d), optionally only rows [a, b) of
    its sides (a split limb).  GeoMesh draws Bedrock 'east' on his RIGHT (mirrored x), 'west' on his left."""
    a, b = rows if rows else (0, h)
    side_v = V + d + a
    return {
        "north": {"uv": [U + d, side_v], "uv_size": [w, b - a]},
        "south": {"uv": [U + 2 * d + w, side_v], "uv_size": [w, b - a]},
        "east": {"uv": [U, side_v], "uv_size": [d, b - a]},
        "west": {"uv": [U + d + w, side_v], "uv_size": [d, b - a]},
        "up": {"uv": [U + d, V], "uv_size": [w, d]},
        "down": {"uv": [U + d + w, V], "uv_size": [w, d]},
    }


def cube(origin, size, uv, inflate=0.0):
    c = {"origin": list(origin), "size": list(size), "uv": uv}
    if inflate:
        c["inflate"] = inflate
    return c


def limb(name, x0, U, V, U2, V2, top, upper=True):
    """Upper (rows 0-6) or lower (rows 6-12) half of a 4x12x4 limb whose top is at y=top."""
    rows = (0, 6) if upper else (6, 12)
    y = top - (6 if upper else 12)
    return [cube((x0, y, -2), (4, 6, 4), faces(U, V, 4, 12, 4, rows)),
            cube((x0, y, -2), (4, 6, 4), faces(U2, V2, 4, 12, 4, rows), 0.25)]


def body_bones():
    parts = {
        "torso": [cube((-4, 12, -2), (8, 12, 4), faces(16, 16, 8, 12, 4)),
                  cube((-4, 12, -2), (8, 12, 4), faces(16, 32, 8, 12, 4), 0.25)],
        "head": [cube((-4, 24, -4), (8, 8, 8), faces(0, 0, 8, 8, 8)),
                 cube((-4, 24, -4), (8, 8, 8), faces(32, 0, 8, 8, 8), 0.5)],
        "armR": limb("armR", -8, 40, 16, 40, 32, 24, True),
        "lowerArmR": limb("lowerArmR", -8, 40, 16, 40, 32, 24, False),
        "armL": limb("armL", 4, 32, 48, 48, 48, 24, True),
        "lowerArmL": limb("lowerArmL", 4, 32, 48, 48, 48, 24, False),
        "legR": limb("legR", -3.9, 0, 16, 0, 32, 12, True),
        "shinR": limb("shinR", -3.9, 0, 16, 0, 32, 12, False),
        "legL": limb("legL", -0.1, 16, 48, 0, 48, 12, True),
        "shinL": limb("shinL", -0.1, 16, 48, 0, 48, 12, False),
    }
    bones = []
    for name in ORDER:
        b = {"name": name, "pivot": list(PIV[name])}
        if PARENT[name]:
            b["parent"] = PARENT[name]
        if parts.get(name):
            b["cubes"] = parts[name]
        bones.append(b)
    return bones


def write_body(entity):
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{entity}", "texture_width": 64, "texture_height": 64,
                        "visible_bounds_width": 3, "visible_bounds_height": 3, "visible_bounds_offset": [0, 1, 0]},
        "bones": body_bones()}]}
    out = ASSETS / f"rig/{entity}.geo.json"
    out.write_text(json.dumps(geo, indent=1) + "\n", encoding="utf-8")
    print(f"geo -> {out.relative_to(ROOT)}  (64x64 player skin)")


def gear_bones(parts):
    return [(n, PARENT[n], PIV[n], parts.get(n, [])) for n in ORDER]


def gladius():
    """Short sword in the right fist (fist centre x -6, y 11.5), pointing forward (-Z) with the arm down."""
    return [
        box((-6.5, 11, -2.5), (1, 1, 5), "wood", 1.0),
        box((-8, 10.25, -3.5), (4, 2.5, 1), "bronze", 1.0),
        box((-6.75, 10.75, -13), (1.5, 1.5, 9.5), "steel", 1.0, tag="blade"),
        box((-6.25, 11, -14), (0.5, 1, 1), "steel", 1.0),
        box((-6.75, 10.75, 2.5), (1.5, 1.5, 1.2), "bronze", 1.0),
    ]


def scutum():
    """Tall curved shield on the outside of the left forearm (x 8 = outside of a steve arm), face out."""
    return [box((8, 3, -6.5), (1, 19, 13), "scutum", 1.0, tag="scutum"),
            box((7.6, 3, -7), (1.6, 19, 0.5), "bronze_dark", 1.0),
            box((7.6, 3, 6.5), (1.6, 19, 0.5), "bronze_dark", 1.0),
            box((8.8, 10.5, -2), (1.2, 4, 4), "bronze", 1.0, tag="umbo")]


GEAR_MATS = {
    "steel": ((214, 220, 228), 4, None, 0.0, "plain"),
    "wood": ((110, 76, 44), 8, None, 0.0, "grain"),
    "bronze": ((190, 140, 60), 8, (160, 112, 44), 0.05, "plain"),
    "bronze_dark": ((140, 100, 40), 6, None, 0.0, "plain"),
    "scutum": ((156, 26, 26), 6, (130, 22, 22), 0.03, "plain"),
}

if __name__ == "__main__":
    for entity, gear in (("steve", {}), ("legionary", {"weapon": gladius(), "shield": scutum()})):
        write_body(entity)
        if gear:
            build(f"{entity}_gear", gear_bones(gear), GEAR_MATS, {"scutum": hum.scutum, "blade": hum.blade},
                  atlas_width=64, visible=(3, 3, 1))


def preview(entity, skin_png):
    """rig_src/<entity>/preview.geo.json + preview.png: body and gear in one mesh (skin left, gear right),
    only for tools/rig/anim/preview.py; the game draws them as two meshes."""
    from PIL import Image
    body = json.loads((ASSETS / f"rig/{entity}.geo.json").read_text(encoding="utf-8"))["minecraft:geometry"][0]
    gear_path = ASSETS / f"rig/{entity}_gear.geo.json"
    bones = {b["name"]: b for b in body["bones"]}
    gw = gh = 64
    if gear_path.exists():
        gear = json.loads(gear_path.read_text(encoding="utf-8"))["minecraft:geometry"][0]
        gw, gh = gear["description"]["texture_width"], gear["description"]["texture_height"]
        for b in gear["bones"]:
            for c in b.get("cubes", []):
                for f in c["uv"].values():
                    f["uv"] = [f["uv"][0] + 64, f["uv"][1]]
                bones[b["name"]].setdefault("cubes", []).append(c)
    W, H = 64 + gw, max(64, gh)
    body["description"]["texture_width"], body["description"]["texture_height"] = W, H
    out = ROOT / f"rig_src/{entity}"
    out.mkdir(parents=True, exist_ok=True)
    (out / "preview.geo.json").write_text(json.dumps({"format_version": "1.12.0", "minecraft:geometry": [body]}), encoding="utf-8")
    img = Image.new("RGBA", (W, H))
    img.paste(Image.open(skin_png).convert("RGBA"), (0, 0))
    if gear_path.exists():
        img.paste(Image.open(ASSETS / f"textures/rig/{entity}_gear.png").convert("RGBA"), (64, 0))
    img.save(out / "preview.png")
    print(f"predogled -> rig_src/{entity}/preview.geo.json")


if __name__ == "__main__":
    for entity in ("steve", "legionary"):
        preview(entity, ROOT / "rig_src/steve/preview_skin.png")
