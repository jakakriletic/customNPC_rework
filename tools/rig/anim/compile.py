"""Compile short *.anim.yaml files into one .animation.json per entity.

    py -3 tools/anim/compile.py troll              # every clip of the troll
    py -3 tools/anim/compile.py troll smash stomp  # only these (others stay as they are)

Sources:  animations_src/<entity>/*.anim.yaml, poses.yaml, rig.yaml, mob.yaml
Output:   the file named by `output:` in rig.yaml (loaded by the game on start,
          server and client).  See tools/anim/README.md for the format.
          mob.yaml (stats, attack table, sounds) is checked and stored under "mob".
"""
import argparse
import copy
import json
import sys
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

import yaml

sys.path.insert(0, str(Path(__file__).resolve().parent))
from rig import ROOT, RigConfig, Model, Clip, is_prop, ease as ease_fn  # noqa: E402

AXES = {"x": 0, "y": 1, "z": 2}
CHANNEL_DEFAULT = {"rotation": 0.0, "position": 0.0, "scale": 1.0}

EASE_WORDS = {
    None: "easeInOutSine", "smooth": "easeInOutSine", "linear": "linear", "step": "step",
    "in": "easeInQuad",        # speeds up into the pose: strikes, throws
    "strike": "easeInCubic",   # sharper acceleration: heavy blows
    "out": "easeOutQuad",      # slows into the pose: wind-ups, settling
    "soft": "easeOutSine",
    "back": "easeOutBack",     # small built-in overshoot
    "elastic": "easeOutElastic",
    "bounce": "easeOutBounce",
}

# Keys every event may carry; the game ignores unknown keys.
# Rework M16 acts on hit, sound, lunge and heal on the server; every event also reaches scripts (animationEvent).
EVENT_TYPES = {"hit", "sound", "shake", "dust", "step", "lock", "lunge", "buff", "projectile", "summon", "heal", "script"}
PROJECTILE_KINDS = {"rock", "web", "arrow", "small_fireball"}
SOUNDS_JSON = ROOT / "dev/src/patch/resources/assets/rwrig/sounds.json"
VANILLA_POTIONS = {"speed", "slowness", "haste", "mining_fatigue", "strength", "instant_health",
                   "instant_damage", "jump_boost", "nausea", "regeneration", "resistance", "fire_resistance",
                   "water_breathing", "invisibility", "blindness", "night_vision", "hunger", "weakness",
                   "poison", "wither", "health_boost", "absorption", "saturation", "glowing", "levitation",
                   "luck", "unluck"}


class CompileError(Exception):
    pass


def load_yaml(path):
    return yaml.safe_load(Path(path).read_text(encoding="utf-8")) or {}


class Library:
    def __init__(self, rig: RigConfig):
        self.rig = rig
        self.poses = rig.poses()
        self.model = Model(rig.geo)
        self.bones = set(self.model.order)
        self.bind_low = (float(rig.floor) if rig.floor is not None
                         else self.model.feet_low(rig.feet) if rig.feet else 0.0)
        mats = self.model.world_matrices()
        ys = [(mats[n] @ c)[1] for n in mats for cube in self.model.bones[n].cubes for c in cube.corners()]
        self.model_height = (max(ys) / 16.0) if ys else 1.0

    def pose(self, name, _seen=()):
        if name not in self.poses:
            raise CompileError(f"neznana poza '{name}' (glej {self.rig.dir.name}/poses.yaml)")
        if name in _seen:
            raise CompileError(f"poza '{name}' deduje sama od sebe")
        spec = dict(self.poses[name] or {})
        base = spec.pop("base", None)
        spec.pop("opis", None)
        values = dict(self.pose(base, _seen + (name,))) if base else {}
        values.pop("solve", None)       # a solve belongs to its own pose only
        values.update(spec)
        return values

    # -------------------------------------------------------------- solve
    def tip_position(self, values, tip):
        """(forward, side, height) in blocks of a rig tip for channel values (after grounding)."""
        spec = self.rig.tips[tip]
        mats = self.model.world_matrices(channels_to_pose(values))
        p = self.model.point(mats, spec["bone"], spec["at"])
        return {"forward": -p[2] / 16.0, "side": p[0] / 16.0, "height": p[1] / 16.0}

    def apply_solve(self, values, solves, warnings, where, grounded):
        """solve: [{tip: club, using: wrist_r.bend, height: 0.1}, ...]

        Each entry searches one control (in its limits, or `range: [a, b]`)
        so the tip reaches the wanted forward / side / height (blocks).
        """
        if isinstance(solves, dict):
            solves = [solves]
        for spec in solves or []:
            tip, control = spec.get("tip"), spec.get("using")
            if tip not in self.rig.tips:
                raise CompileError(f"{where}: solve pozna tips iz rig.yaml, ne '{tip}'")
            if control not in self.rig.controls:
                raise CompileError(f"{where}: solve.using mora biti kontrolnik, ne '{control}'")
            goals = {k: float(spec[k]) for k in ("forward", "side", "height") if k in spec}
            if not goals:
                raise CompileError(f"{where}: solve potrebuje forward, side ali height")
            lo, hi = spec.get("range", self.rig.limits.get(control, (-180, 180)))
            targets = [((bone, ch, axis), sign) for bone, ch, axis, sign in self.rig.controls[control]]

            def error(x):
                trial = dict(values)
                for key, sign in targets:
                    trial[key] = sign * x
                if grounded:
                    self.ground(trial)
                pos = self.tip_position(trial, tip)
                return sum((pos[k] - v) ** 2 for k, v in goals.items())

            xs = [lo + (hi - lo) * i / 120 for i in range(121)]
            best = min(xs, key=error)
            step = (hi - lo) / 120
            a, b = max(lo, best - step), min(hi, best + step)
            for _ in range(40):
                m1, m2 = a + (b - a) / 3, b - (b - a) / 3
                if error(m1) < error(m2):
                    b = m2
                else:
                    a = m1
            x = (a + b) / 2
            for key, sign in targets:
                values[key] = sign * x
            miss = error(x) ** 0.5
            if miss > 0.15:
                warnings.append(f"{where}: solve {tip} z {control} zgreši za {miss:.2f} bloka "
                                f"(najboljše {control}={x:.0f})")
        return values

    def to_channels(self, values, warnings, where):
        """{control or 'bone.channel.axis': value} -> {(bone, channel, axis): value}."""
        out = {}
        for key, value in values.items():
            if key in self.rig.controls:
                lo, hi = self.rig.limits.get(key, (-1e9, 1e9))
                if not lo <= value <= hi:
                    warnings.append(f"{where}: {key}={value} je izven obsega [{lo}, {hi}]")
                for bone, ch, axis, sign in self.rig.controls[key]:
                    if axis == 3:       # xyz: same value on all axes (uniform scale)
                        for a in range(3):
                            out[(bone, ch, a)] = sign * float(value)
                        continue
                    out[(bone, ch, axis)] = out.get((bone, ch, axis), 0.0) + sign * float(value)
                continue
            parts = key.split(".")
            if len(parts) == 3 and parts[1] in CHANNEL_DEFAULT and parts[2] in AXES:
                if parts[0] not in self.bones:
                    raise CompileError(f"{where}: model nima kosti '{parts[0]}'")
                k = (parts[0], parts[1], AXES[parts[2]])
                out[k] = float(value) if parts[1] == "scale" else out.get(k, 0.0) + float(value)
                continue
            raise CompileError(f"{where}: neznan kontrolnik '{key}' (glej rig.yaml -> controls)")
        return out

    def ground(self, values):
        """Shift body.y so the lower foot rests on the floor (bind-pose height)."""
        if not self.rig.feet:
            return values
        key = ("body", "position", 1)
        values[key] = 0.0
        low = self.model.feet_low(self.rig.feet, channels_to_pose(values)) - self.bind_low
        values[key] = -low
        return values


def channel_default(bone, ch):
    """Rest value of a channel; a prop's scale rests at 0 (hidden)."""
    return 0.0 if ch == "scale" and is_prop(bone) else CHANNEL_DEFAULT[ch]


def channels_to_pose(values):
    pose = {}
    for (bone, ch, axis), v in values.items():
        vec = pose.setdefault(bone, {}).setdefault(ch, [1.0, 1.0, 1.0] if ch == "scale" else [0.0, 0.0, 0.0])
        vec[axis] = v
    return pose


def beat_values(beat, lib, warnings, where, grounded=True):
    values = {}
    pose = beat.get("pose")
    if isinstance(pose, str):
        values.update(lib.pose(pose))
    elif isinstance(pose, dict):
        values.update(pose)
    for key, v in (beat.get("add") or {}).items():
        values[key] = v if ".scale." in key else values.get(key, 0.0) + v
    solve = beat.get("solve", values.pop("solve", None))
    channels = lib.to_channels(values, warnings, where)
    if solve:
        lib.apply_solve(channels, solve, warnings, where, grounded)
    return channels


def resolve_beats(doc, lib, warnings):
    beats = doc.get("beats") or []
    if not beats:
        raise CompileError("animacija nima 'beats'")
    resolved = []
    for i, beat in enumerate(beats):
        where = f"beat {i} (t={beat.get('t')})"
        if "t" not in beat:
            raise CompileError(f"{where}: manjka t")
        grounded = doc.get("ground", "auto") == "auto" and not beat.get("airborne")
        values = beat_values(beat, lib, warnings, where, grounded)
        if grounded:
            lib.ground(values)
        ease_word = beat.get("ease")
        resolved.append({
            "t": float(beat["t"]), "values": values, "ease": EASE_WORDS.get(ease_word, ease_word),
            "overshoot": float(beat.get("overshoot", 0.0)), "hold": float(beat.get("hold", 0.0)),
        })
    resolved.sort(key=lambda b: b["t"])
    if resolved[0]["t"] != 0.0:
        warnings.append("prvi beat ni na t=0; dodan je začetni ključ iz prvega beata")
        first = copy.deepcopy(resolved[0])
        first["t"] = 0.0
        resolved.insert(0, first)
    return resolved


def build_tracks(beats, length, loop):
    """Keyframes per (bone, channel, axis): list of (t, value, easing)."""
    keys = set()
    for b in beats:
        keys.update(b["values"])
    tracks = {}
    for key in keys:
        default = channel_default(key[0], key[1])
        frames = []
        for i, b in enumerate(beats):
            v = b["values"].get(key, default)
            prev = frames[-1][1] if frames else v
            if b["overshoot"] and frames and key[1] != "scale":
                nxt_t = beats[i + 1]["t"] if i + 1 < len(beats) else length
                settle = max(0.04, min(0.14, (nxt_t - b["t"] - b["hold"]) * 0.5))
                frames.append((b["t"], v + (v - prev) * b["overshoot"], b["ease"]))
                if b["t"] + settle < nxt_t - 1e-3:
                    frames.append((b["t"] + settle, v, "easeInOutSine"))
                    if b["hold"]:
                        frames.append((b["t"] + settle + b["hold"], v, "linear"))
                continue
            frames.append((b["t"], v, b["ease"]))
            if b["hold"]:
                frames.append((b["t"] + b["hold"], v, "linear"))
        if loop and frames[-1][0] < length - 1e-6:
            frames.append((length, frames[0][1], "easeInOutSine"))
        tracks[key] = frames
    return tracks


def sample_track(frames, t):
    if t <= frames[0][0]:
        return frames[0][1]
    for (t0, v0, _), (t1, v1, e1) in zip(frames, frames[1:]):
        if t <= t1:
            k = 1.0 if t1 <= t0 else ease_fn(e1, (t - t0) / (t1 - t0))
            return v0 + (v1 - v0) * k
    return frames[-1][1]


def mirror_tracks(tracks, mirror):
    out = {}
    for (bone, ch, axis), frames in tracks.items():
        nb = mirror.get(bone, bone)
        flip = (ch == "rotation" and axis in (1, 2)) or (ch == "position" and axis == 0)
        out[(nb, ch, axis)] = [(t, -v if flip else v, e) for t, v, e in frames]
    return out


def to_json_clip(tracks, length, loop):
    by_channel = {}
    for (bone, ch, axis), frames in tracks.items():
        by_channel.setdefault((bone, ch), {})[axis] = frames
    bones = {}
    for (bone, ch), axes in sorted(by_channel.items()):
        default = channel_default(bone, ch)
        if all(abs(f[1] - default) < 1e-4 for frames in axes.values() for f in frames) and not (
                is_prop(bone) and ch == "scale"):
            continue        # never leaves the bind pose
        times = sorted({round(f[0], 4) for frames in axes.values() for f in frames})
        keyed = {}
        for t in times:
            vec, easing = [], "linear"
            for axis in range(3):
                frames = axes.get(axis)
                if frames is None:
                    vec.append(default)
                    continue
                vec.append(round(sample_track(frames, t), 3))
                for ft, _, fe in frames:
                    if abs(ft - t) < 1e-4:
                        easing = fe or "linear"
            key = f"{t:.4f}".rstrip("0").rstrip(".")
            key = key if "." in key else key + ".0"
            keyed[key] = {"vector": vec, "easing": easing} if easing != "linear" else {"vector": vec}
        bones.setdefault(bone, {})[ch] = keyed
    clip = {"animation_length": round(length, 4), "bones": bones}
    if loop:
        clip["loop"] = True
    return clip


def name_of(doc):
    return doc.get("name", "?")


def bake_projectiles(clip, lib, name):
    """`projectile` events name a rig tip in `from:`; store where that tip is at
    that moment as `origin: [forward, side, height]` (blocks), so the game
    spawns the rock / arrow exactly in the hand the preview shows."""
    sampled = None
    for e in clip["events"]:
        if e["type"] != "projectile":
            continue
        kind = e.get("kind", "rock")
        if kind not in PROJECTILE_KINDS:
            raise CompileError(f"projectile pri t={e['t']}: kind mora biti eden od {sorted(PROJECTILE_KINDS)}")
        tip = e.get("from")
        if tip is None:
            e.setdefault("origin", [1.0, 0.0, lib.model_height * 0.7])
            continue
        if tip not in lib.rig.tips:
            raise CompileError(f"projectile pri t={e['t']}: from mora biti konica iz rig.yaml, ne '{tip}'")
        sampled = sampled or Clip(name, clip)
        pose = sampled.sample(e["t"])
        spec = lib.rig.tips[tip]
        mats = lib.model.world_matrices(pose)
        if spec["bone"] not in mats:
            raise CompileError(f"projectile pri t={e['t']}: konica {tip} je skrita (prop brez scale)")
        pt = lib.model.point(mats, spec["bone"], spec["at"])
        e["origin"] = [round(-pt[2] / 16.0, 3), round(pt[0] / 16.0, 3), round(pt[1] / 16.0, 3)]


def compile_doc(doc, lib):
    warnings = []
    beats = resolve_beats(doc, lib, warnings)
    loop = bool(doc.get("loop", False))
    length = float(doc.get("length", beats[-1]["t"]))
    if length < beats[-1]["t"]:
        raise CompileError(f"length {length} je krajši od zadnjega beata {beats[-1]['t']}")
    tracks = build_tracks(beats, length, loop)
    if doc.get("mirror"):
        tracks = mirror_tracks(tracks, lib.rig.mirror)
    clip = to_json_clip(tracks, length, loop)
    events = []
    for e in doc.get("events") or []:
        if "t" not in e or "type" not in e:
            raise CompileError(f"dogodek {e} potrebuje t in type")
        if e["type"] not in EVENT_TYPES:
            raise CompileError(f"neznan tip dogodka '{e['type']}' (znani: {', '.join(sorted(EVENT_TYPES))})")
        if not 0 <= float(e["t"]) <= length:
            raise CompileError(f"dogodek pri t={e['t']} je izven klipa (0..{length})")
        events.append(dict(e, t=float(e["t"])))
    if events:
        clip["events"] = sorted(events, key=lambda e: e["t"])
        bake_projectiles(clip, lib, name_of(doc))
    for key in ("stride", "blend_in", "blend_out"):
        if key in doc:
            clip[key] = float(doc[key])
    return clip, warnings


def known_sounds():
    if not SOUNDS_JSON.exists():
        return None
    return set(json.loads(SOUNDS_JSON.read_text(encoding="utf-8")))


def check_sounds(data):
    """Every sound named by an event or mob.yaml must exist in sounds.json."""
    known = known_sounds()
    if known is None:
        return []
    used = []
    for name, clip in data["animations"].items():
        for e in clip.get("events", []):
            s = e.get("sound")
            if s:
                used += [(f"{name} t={e['t']}", x) for x in (s if isinstance(s, list) else [s])]
    mob = data.get("mob") or {}
    used += [("mob.voice", v) for v in (mob.get("voice") or {}).values()]
    used += [("mob.sound_volume", k) for k in (mob.get("sound_volume") or {})]
    if (mob.get("step") or {}).get("sound"):
        used.append(("mob.step", mob["step"]["sound"]))
    # A full id (minecraft:entity.ravager.roar) is a sound of the game or another mod: not checked here.
    return [f"{where}: '{s}' ni v sounds.json" for where, s in used if s not in known and ":" not in s]


MOB_ATTACK_KEYS = {"clip", "min", "max", "weight", "cooldown", "recover", "crowd", "wounded", "airborne_target"}


def check_mob(mob, clips):
    """Validate mob.yaml and return the JSON stored under "mob"."""
    mob = dict(mob)
    for key in ("hitbox",):
        if key not in mob:
            raise CompileError(f"manjka '{key}'")
    if len(mob["hitbox"]) != 2:
        raise CompileError("hitbox: [širina, višina]")
    attacks = mob.get("attacks") or []
    if not attacks:
        raise CompileError("attacks: vsaj en napad")
    for i, a in enumerate(attacks):
        unknown = set(a) - MOB_ATTACK_KEYS
        if unknown:
            raise CompileError(f"napad {i}: neznani ključi {sorted(unknown)} (znani: {sorted(MOB_ATTACK_KEYS)})")
        if a.get("clip") not in clips:
            raise CompileError(f"napad {i}: ni klipa '{a.get('clip')}' (klipi: {', '.join(clips)})")
        if float(a.get("min", 0)) > float(a.get("max", 99)):
            raise CompileError(f"napad {a['clip']}: min > max")
    if mob.get("boss_bar", 0) not in (0, 1, 2):
        raise CompileError("boss_bar: 0 (brez), 1 (vedno), 2 (med bojem)")
    if mob.get("boss_color", "red") not in ("pink", "blue", "red", "green", "yellow", "purple", "white"):
        raise CompileError("boss_color: pink, blue, red, green, yellow, purple ali white")
    enrage = mob.get("enrage")
    if enrage:
        if not 0 < float(enrage.get("below", 0)) < 1:
            raise CompileError("enrage.below: delež zdravja med 0 in 1 (npr. 0.35)")
        if enrage.get("clip") and enrage["clip"] not in clips:
            raise CompileError(f"enrage: ni klipa '{enrage['clip']}'")
    greet = mob.get("greet")
    if greet and greet.get("clip") not in clips:
        raise CompileError(f"greet: ni klipa '{greet.get('clip')}'")
    if len(attacks) + (1 if greet else 0) > 250:
        raise CompileError("preveč napadov")
    for name, clip in clips.items():
        for e in clip.get("events", []):
            eff = e.get("effect")
            if eff and eff not in VANILLA_POTIONS and ":" not in eff:
                raise CompileError(f"{name} t={e['t']}: neznan effect '{eff}'")
    return mob


def compile_entity(entity, names=None, quiet=False):
    rig = RigConfig(entity)
    lib = Library(rig)
    files = rig.clip_files()
    if names:
        wanted = set(names)
        files = [f for f in files if f.name.split(".")[0] in wanted]
        missing = wanted - {f.name.split(".")[0] for f in files}
        if missing:
            raise SystemExit(f"ni klipov: {', '.join(sorted(missing))}")
    data = {"format_version": "1.8.0", "animations": {}}
    if names and rig.output.exists():
        data = json.loads(rig.output.read_text(encoding="utf-8"))
    failed, compiled = False, {}
    for f in files:
        doc = load_yaml(f)
        name = doc.get("name") or f.name.split(".")[0]
        try:
            clip, warnings = compile_doc(doc, lib)
        except (CompileError, yaml.YAMLError, KeyError, ValueError) as e:
            failed = True
            print(f"NAPAKA {f.name}: {e}")
            continue
        data["animations"][name] = clip
        compiled[name] = clip
        if not quiet:
            print(f"OK  {f.name}  ({clip['animation_length']} s, {len(clip['bones'])} kosti, "
                  f"{len(clip.get('events', []))} dogodkov)")
            for w in warnings:
                print(f"    opozorilo: {w}")
    data["animations"] = dict(sorted(data["animations"].items()))
    mob_path = rig.dir / "mob.yaml"
    if mob_path.exists():
        try:
            data["mob"] = check_mob(load_yaml(mob_path), data["animations"])
        except (CompileError, yaml.YAMLError) as e:
            failed = True
            print(f"NAPAKA mob.yaml: {e}")
    sound_problems = check_sounds(data)
    for problem in sound_problems:
        print(f"NAPAKA zvok: {problem}")
    failed = failed or bool(sound_problems)
    rig.output.parent.mkdir(parents=True, exist_ok=True)
    rig.output.write_text(json.dumps(data, indent=1) + "\n", encoding="utf-8")
    if not quiet:
        print(f"-> {rig.output.relative_to(ROOT)}")
    # Models with the same skeleton share these clips (rig.yaml: copies: [knight, ...]).
    for other in rig.raw.get("copies") or []:
        target = rig.output.with_name(f"{other}.animation.json")
        target.write_text(json.dumps(data, indent=1) + "\n", encoding="utf-8")
        if not quiet:
            print(f"-> {target.relative_to(ROOT)}")
    return compiled, failed


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("entity")
    ap.add_argument("clips", nargs="*")
    args = ap.parse_args(argv)
    _, failed = compile_entity(args.entity, args.clips)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
