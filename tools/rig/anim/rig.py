"""Geometry + animation maths, identical to the in-game player (anim/GeoMesh.java,
anim/AnimClip.java).  Whatever the preview shows is what the game draws.

Model space ("Gecko space", used by the Java renderer as well): Bedrock X is
mirrored (x -> -x), bind and keyframe rotations become (-x, -y, z) and keyframe
positions (-x, y, z).  In that space the model faces -Z and its right hand is at +X.
"""
import json
import math
from pathlib import Path

import numpy as np
import yaml

ROOT = Path(__file__).resolve().parents[3]
SRC_ROOT = ROOT / "rig_src"


# ---------------------------------------------------------------- rig config
class RigConfig:
    """animations_src/<entity>/rig.yaml: which model, controls, checks."""

    def __init__(self, entity):
        self.entity = entity
        self.dir = SRC_ROOT / entity
        path = self.dir / "rig.yaml"
        if not path.exists():
            raise SystemExit(f"ni {path.relative_to(ROOT)} (entitete: "
                             + ", ".join(p.name for p in SRC_ROOT.iterdir() if (p / 'rig.yaml').exists()) + ")")
        raw = yaml.safe_load(path.read_text(encoding="utf-8"))
        self.raw = raw
        self.geo = ROOT / raw["geo"]
        self.texture = ROOT / raw["texture"]
        self.output = ROOT / raw["output"]
        self.feet = raw.get("feet", [])
        self.tips = raw.get("tips", {})
        self.weapon_tip = raw.get("weapon_tip")
        self.solid = raw.get("solid", [])
        axes = {"x": 0, "y": 1, "z": 2, "xyz": 3}    # xyz: all three axes (props: scale 0 = hidden)
        # control: [bone, channel, axis, sign]  or a list of those (moves several bones at once)
        self.controls = {}
        for k, v in raw["controls"].items():
            parts = v if isinstance(v[0], list) else [v]
            self.controls[k] = [(p[0], p[1], axes[p[2]], float(p[3])) for p in parts]
        self.limits = {k: tuple(v) for k, v in (raw.get("limits") or {}).items()}
        mirror = dict(raw.get("mirror") or {})
        mirror.update({v: k for k, v in list(mirror.items())})
        self.mirror = mirror
        self.trace = raw.get("trace", list(self.tips)[:3])
        # floor: model y (px) where the feet stand, when the bind pose is not a standing pose
        self.floor = raw.get("floor")
        # preview: {scale: 1.0, side: 0.3, three_quarter: 0.42}  (size and horizontal placement on sheets)
        self.preview = {"scale": 1.0, "side": 0.3, "three_quarter": 0.42}
        self.preview.update(raw.get("preview") or {})

    def poses(self):
        path = self.dir / "poses.yaml"
        if not path.exists():
            return {}
        return yaml.safe_load(path.read_text(encoding="utf-8")) or {}

    def clip_files(self):
        return sorted(self.dir.glob("*.anim.yaml"))


# ---------------------------------------------------------------- matrices
def rot_x(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1]])


def rot_y(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1]])


def rot_z(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]])


def translate(v):
    m = np.eye(4)
    m[:3, 3] = v
    return m


def scale(v):
    return np.diag([v[0], v[1], v[2], 1.0])


def euler_zyx(rx, ry, rz):
    return rot_z(rz) @ rot_y(ry) @ rot_x(rx)


def is_prop(bone):
    """Bones named prop_* are hidden unless the pose gives them a scale (Java: GeoMesh.isProp)."""
    return bone.startswith("prop_")


def bedrock_point(p):
    """Bedrock model coordinates -> Gecko model space."""
    return np.array([-p[0], p[1], p[2]], dtype=float)


# ---------------------------------------------------------------- easing
# Keep in sync with anim/Easing.java.
def _back(t, k=1.70158):
    return t * t * ((k + 1) * t - k)


def _elastic(t):
    if t in (0.0, 1.0):
        return t
    return -(2 ** (10 * (t - 1))) * math.sin((t - 1.075) * (2 * math.pi) / 0.3)


def _bounce_out(t):
    if t < 1 / 2.75:
        return 7.5625 * t * t
    if t < 2 / 2.75:
        t -= 1.5 / 2.75
        return 7.5625 * t * t + 0.75
    if t < 2.5 / 2.75:
        t -= 2.25 / 2.75
        return 7.5625 * t * t + 0.9375
    t -= 2.625 / 2.75
    return 7.5625 * t * t + 0.984375


_IN = {
    "Sine": lambda t: 1 - math.cos(t * math.pi / 2),
    "Quad": lambda t: t * t,
    "Cubic": lambda t: t ** 3,
    "Quart": lambda t: t ** 4,
    "Expo": lambda t: 0.0 if t == 0 else 2 ** (10 * (t - 1)),
    "Back": _back,
    "Elastic": _elastic,
    "Bounce": lambda t: 1 - _bounce_out(1 - t),
}


def ease(name, t):
    if not name or name == "linear":
        return t
    if name == "step":
        return 0.0 if t < 1 else 1.0
    for prefix, kind in (("easeInOut", "io"), ("easeIn", "i"), ("easeOut", "o")):
        if name.startswith(prefix):
            fn = _IN.get(name[len(prefix):])
            if fn is None:
                return t
            if kind == "i":
                return fn(t)
            if kind == "o":
                return 1 - fn(1 - t)
            return fn(2 * t) / 2 if t < 0.5 else 1 - fn(2 - 2 * t) / 2
    return t


# ---------------------------------------------------------------- keyframes
def channel_frames(channel):
    frames = []
    for key, value in channel.items():
        vec = value["vector"] if isinstance(value, dict) else value
        easing = value.get("easing") if isinstance(value, dict) else None
        frames.append((float(key), [float(x) for x in vec], easing))
    frames.sort(key=lambda f: f[0])
    return frames


def sample_frames(frames, t):
    if t <= frames[0][0]:
        return list(frames[0][1])
    for (t0, v0, _), (t1, v1, e1) in zip(frames, frames[1:]):
        if t <= t1:
            span = t1 - t0
            k = 1.0 if span <= 0 else ease(e1, (t - t0) / span)
            return [a + (b - a) * k for a, b in zip(v0, v1)]
    return list(frames[-1][1])


class Clip:
    def __init__(self, name, data):
        self.name = name
        self.data = data
        self.loop = bool(data.get("loop"))
        self.tracks = {}
        for bone, channels in data.get("bones", {}).items():
            for ch, value in channels.items():
                self.tracks[(bone, ch)] = channel_frames(value)
        self.length = float(data.get("animation_length", 1.0))
        self.events = [dict(e) for e in data.get("events", [])]

    def sample(self, t):
        if self.loop and self.length > 0:
            t = t % self.length
        out = {}
        for (bone, ch), frames in self.tracks.items():
            out.setdefault(bone, {})[ch] = sample_frames(frames, t)
        return out


def load_clips(path):
    data = json.loads(Path(path).read_text(encoding="utf-8"))
    return {name: Clip(name, clip) for name, clip in data.get("animations", {}).items()}


# ---------------------------------------------------------------- geometry
class Cube:
    def __init__(self, raw):
        o, s = raw["origin"], raw["size"]
        inf = raw.get("inflate", 0.0)
        self.lo = np.array([-(o[0] + s[0]) - inf, o[1] - inf, o[2] - inf])
        self.hi = np.array([-o[0] + inf, o[1] + s[1] + inf, o[2] + s[2] + inf])
        self.uv = raw["uv"]
        p = raw.get("pivot", [0, 0, 0])
        r = raw.get("rotation", [0, 0, 0])
        if any(r):
            piv = bedrock_point(p)
            self.local = translate(piv) @ euler_zyx(*np.radians([-r[0], -r[1], r[2]])) @ translate(-piv)
        else:
            self.local = np.eye(4)

    def corners(self):
        return [self.local @ np.array([x, y, z, 1.0])
                for x in (self.lo[0], self.hi[0]) for y in (self.lo[1], self.hi[1])
                for z in (self.lo[2], self.hi[2])]


class Bone:
    def __init__(self, raw):
        self.name = raw["name"]
        self.parent = raw.get("parent")
        self.pivot = bedrock_point(raw.get("pivot", [0, 0, 0]))
        r = raw.get("rotation", [0, 0, 0])
        self.bind_rot = np.radians([-r[0], -r[1], r[2]])
        self.cubes = [Cube(c) for c in raw.get("cubes", [])]
        self.children = []


class Model:
    def __init__(self, path):
        raw = json.loads(Path(path).read_text(encoding="utf-8"))["minecraft:geometry"][0]
        self.description = raw["description"]
        self.bones = {}
        self.order = []
        for rb in raw["bones"]:
            b = Bone(rb)
            self.bones[b.name] = b
            self.order.append(b.name)
        self.roots = []
        for name in self.order:
            b = self.bones[name]
            if b.parent and b.parent in self.bones:
                self.bones[b.parent].children.append(b)
            else:
                self.roots.append(b)

    def descendants(self, name):
        out, stack = [], [self.bones[name]]
        while stack:
            b = stack.pop()
            out.append(b.name)
            stack.extend(b.children)
        return out

    def world_matrices(self, pose=None, hidden=()):
        """pose: {bone: {"rotation": deg[3], "position": px[3], "scale": [3]}} (file units)."""
        pose = pose or {}
        mats = {}

        def visit(bone, parent_m):
            if bone.name in hidden:
                return
            ch = pose.get(bone.name, {})
            if is_prop(bone.name) and ch.get("scale", [0, 0, 0])[0] < 0.01:
                return      # props (held rocks ...) exist only while a clip shows them
            r = ch.get("rotation", [0, 0, 0])
            p = ch.get("position", [0, 0, 0])
            s = ch.get("scale", [1, 1, 1])
            rot = bone.bind_rot + np.radians([-r[0], -r[1], r[2]])
            m = (parent_m @ translate([-p[0], p[1], p[2]]) @ translate(bone.pivot)
                 @ euler_zyx(*rot) @ scale(s) @ translate(-bone.pivot))
            mats[bone.name] = m
            for c in bone.children:
                visit(c, m)

        for root in self.roots:
            visit(root, np.eye(4))
        return mats

    def lowest_y(self, mats, bone, recursive=False):
        names = self.descendants(bone) if recursive else [bone]
        ys = [(mats[n] @ c)[1] for n in names if n in mats
              for cube in self.bones[n].cubes for c in cube.corners()]
        return min(ys) if ys else 0.0

    def feet_low(self, feet, pose=None):
        mats = self.world_matrices(pose)
        return min(self.lowest_y(mats, f) for f in feet)

    def inside(self, mats, bone, point, margin=0.5):
        """Is a world point inside any cube of the bone (shrunk by margin)?"""
        for cube in self.bones[bone].cubes:
            inv = np.linalg.inv(mats[bone] @ cube.local)
            p = (inv @ np.append(point, 1.0))[:3]
            if np.all(p > cube.lo + margin) and np.all(p < cube.hi - margin):
                return True
        return False

    def point(self, mats, bone, bedrock_at):
        """World position of a point given in Bedrock coordinates of the bind pose."""
        return (mats[bone] @ np.append(bedrock_point(bedrock_at), 1.0))[:3]
