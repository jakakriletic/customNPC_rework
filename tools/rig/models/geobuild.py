"""Shared model builder: named bones of boxes -> Bedrock .geo.json + painted texture.

Used by tools/models/build_<entity>.py.  A model is a list of bones

    (name, parent or None, pivot, [box(...), ...])

in Bedrock / Blockbench coordinates: 1 unit = 1 pixel = 1/16 block, Y up, the
creature faces -Z (north), x < 0 is ITS RIGHT side.  Every box names a
material; the painter fills each face of the box in the texture atlas from
that material (noise, shading, a style) and decals (functions keyed by a box's
`tag`) add hand-drawn features such as eyes.

Material: (base rgb, noise, spot rgb or None, spot density[, style]).
Styles: "grain" (wood grain along z), "stitch" (cloth), "strap" (leather band
edges), "fur" (short dark strokes), "scales", "plain" (no outline darkening).
"""
import json
import math
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT / "dev/src/patch/resources/assets/customnpcs"
FACE_ORDER = ("north", "south", "east", "west", "up", "down")

# Older material names whose style is implied (keeps the troll's texture identical).
IMPLIED_STYLE = {"wood": "grain", "wood_dark": "grain", "cloth": "stitch", "strap": "strap",
                 "iron": "plain", "tooth": "plain"}


def box(origin, size, mat, density=0.5, rotation=None, pivot=None, tag=None):
    """density = texels per model pixel (0.5 for big bodies, 1.0 for faces and details)."""
    b = {"origin": list(origin), "size": list(size), "mat": mat, "density": density, "tag": tag}
    if rotation:
        b["rotation"] = list(rotation)
        b["pivot"] = list(pivot)
    return b


def mirror_box(b, right="_r", left="_l"):
    """The same box on the other side (x -> -x)."""
    m = dict(b)
    m["origin"] = [-(b["origin"][0] + b["size"][0]), b["origin"][1], b["origin"][2]]
    if "rotation" in b:
        r = b["rotation"]
        m["rotation"] = [r[0], -r[1], -r[2]]
        m["pivot"] = [-b["pivot"][0], b["pivot"][1], b["pivot"][2]]
    if b.get("tag"):
        m["tag"] = b["tag"].replace(right, left)
    return m


def mirror_bones(bones, rename):
    """Mirror a list of (name, parent, pivot, boxes); rename maps right-side names to left ones."""
    out = []
    for name, parent, pivot, boxes in bones:
        out.append((rename.get(name, name), rename.get(parent, parent), (-pivot[0], pivot[1], pivot[2]),
                    [mirror_box(b) for b in boxes]))
    return out


def face_dims(size, face):
    x, y, z = size
    return {"north": (x, y), "south": (x, y), "east": (z, y), "west": (z, y),
            "up": (x, z), "down": (x, z)}[face]


class Atlas:
    """Shelf packer: hands out rectangles left to right, row by row."""

    def __init__(self, width):
        self.width = width
        self.x = self.y = self.row_h = 0

    def place(self, w, h):
        if self.x + w > self.width:
            self.x, self.y, self.row_h = 0, self.y + self.row_h, 0
        pos = (self.x, self.y)
        self.x += w
        self.row_h = max(self.row_h, h)
        return pos

    @property
    def height(self):
        h = self.y + self.row_h
        return 1 << max(4, math.ceil(math.log2(max(1, h))))


def paint_face(rng, materials, mat, w, h, face, vertical):
    entry = materials[mat]
    base, noise, spot, spot_density = entry[:4]
    style = entry[4] if len(entry) > 4 else IMPLIED_STYLE.get(mat)
    img = np.empty((h, w, 3), np.float32)
    img[:] = base
    img += rng.normal(0, noise * 0.55, (h, w, 1))          # brightness noise
    img += rng.normal(0, noise * 0.25, (h, w, 3))          # colour noise
    if spot is not None and spot_density > 0:
        mask = rng.random((h, w)) < spot_density
        img[mask] = img[mask] * 0.4 + np.array(spot) * 0.6
    if style == "grain":
        # grain runs along z: across east/west/up/down faces
        along_u = face in ("east", "west", "up", "down")
        lines = rng.random(h if along_u else w) < 0.3
        for i in np.nonzero(lines)[0]:
            if along_u:
                img[i, :, :] *= 0.86
            else:
                img[:, i, :] *= 0.86
    if style == "stitch":
        img[:, ::max(2, w // 4)] *= 0.9                      # stitch columns
        if h > 2:
            img[-1, ::2] *= 0.6                              # ragged hem
    if style == "strap":
        if h > 2:
            img[0] *= 0.75
            img[-1] *= 0.75
    if style == "fur":
        # short strokes "down" the fur: darker 2-texel streaks
        streaks = rng.random((h, w)) < 0.12
        for v, u in zip(*np.nonzero(streaks)):
            img[v:v + 2, u] *= 0.86
        if face == "up":
            img *= 0.95
    if style == "scales":
        for v in range(h):
            off = (v // 2) % 2
            img[v, off::3] *= 0.84 if v % 2 == 0 else 1.0
    # Light from above: darker toward the bottom of vertical faces, darker underside.
    if vertical and h > 1:
        grad = np.linspace(1.04, 0.84, h)[:, None, None]
        img *= grad
    if face == "down":
        img *= 0.78
    if face == "up":
        img *= 1.05
    # Darken the outline a little so boxes read as separate shapes.
    if w > 3 and h > 3 and style != "plain":
        img[0, :] *= 0.9
        img[-1, :] *= 0.86
        img[:, 0] *= 0.9
        img[:, -1] *= 0.9
    return img


def px(img, u, v, colour):
    h, w = img.shape[:2]
    if 0 <= v < h and 0 <= u < w:
        img[v, u] = colour


def build(entity, bones, materials, decals=None, atlas_width=256, seed=1337,
          visible=(8, 7, 2.8)):
    """Write geo/<entity>.geo.json and textures/entity/<entity>.png; returns (geo path, texture path)."""
    decals = decals or {}
    names = [b[0] for b in bones]
    for name, parent, _, _ in bones:
        assert parent is None or parent in names, f"kost {name}: ni starša {parent}"
    assert len(set(names)) == len(names), "podvojena imena kosti"
    geo_out = ASSETS / f"rig/{entity}.geo.json"
    tex_out = ASSETS / f"textures/rig/{entity}.png"
    rng = np.random.default_rng(seed)
    atlas = Atlas(atlas_width)
    jobs = []           # (x, y, w, h, material, face, tag)
    geo_bones = []
    for name, parent, pivot, boxes in bones:
        cubes = []
        for b in boxes:
            if b["mat"] not in materials:
                raise SystemExit(f"kost {name}: neznan material '{b['mat']}'")
            uv = {}
            for face in FACE_ORDER:
                fw, fh = face_dims(b["size"], face)
                tw = max(1, round(fw * b["density"]))
                th = max(1, round(fh * b["density"]))
                x, y = atlas.place(tw, th)
                uv[face] = {"uv": [x, y], "uv_size": [tw, th]}
                jobs.append((x, y, tw, th, b["mat"], face, b.get("tag")))
            cube = {"origin": [round(v, 3) for v in b["origin"]],
                    "size": [round(v, 3) for v in b["size"]], "uv": uv}
            if "rotation" in b:
                cube["rotation"] = b["rotation"]
                cube["pivot"] = b["pivot"]
            cubes.append(cube)
        bone = {"name": name, "pivot": list(pivot)}
        if parent:
            bone["parent"] = parent
        if cubes:
            bone["cubes"] = cubes
        geo_bones.append(bone)

    W, H = atlas_width, atlas.height
    tex = np.zeros((H, W, 4), np.uint8)
    for x, y, w, h, mat, face, tag in jobs:
        vertical = face not in ("up", "down")
        img = paint_face(rng, materials, mat, w, h, face, vertical)
        if tag in decals:
            decals[tag](img, face)
        tex[y:y + h, x:x + w, :3] = np.clip(img, 0, 255).astype(np.uint8)
        tex[y:y + h, x:x + w, 3] = 255

    geo = {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": f"geometry.{entity}",
                "texture_width": W, "texture_height": H,
                "visible_bounds_width": visible[0], "visible_bounds_height": visible[1],
                "visible_bounds_offset": [0, visible[2], 0],
            },
            "bones": geo_bones,
        }],
    }
    geo_out.parent.mkdir(parents=True, exist_ok=True)
    tex_out.parent.mkdir(parents=True, exist_ok=True)
    geo_out.write_text(json.dumps(geo, indent=1) + "\n", encoding="utf-8")
    Image.fromarray(tex, "RGBA").save(tex_out)
    cubes = [c for b in geo_bones for c in b.get("cubes", [])]
    top = max(c["origin"][1] + c["size"][1] for c in cubes)
    length = max(c["origin"][2] + c["size"][2] for c in cubes) - min(c["origin"][2] for c in cubes)
    print(f"geo -> {geo_out.relative_to(ROOT)}  ({len(geo_bones)} kosti, {len(cubes)} kock, "
          f"višina {top / 16:.2f}, dolžina {length / 16:.2f} bloka)")
    print(f"tekstura -> {tex_out.relative_to(ROOT)}  ({W}x{H})")
    return geo_out, tex_out
