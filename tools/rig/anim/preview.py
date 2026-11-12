"""Render a model, poses or animation clips to PNG contact sheets, and check them.

    py -3 tools/anim/preview.py model troll                 # turnaround of the model
    py -3 tools/anim/preview.py poses troll club_overhead   # some poses   (or: --all)
    py -3 tools/anim/preview.py clip troll smash --gif      # compiles the clip first

Sheets land in animations_src/<entity>/_preview/.  Rows are camera views
(F front = looking at his face, his right is on the LEFT of the image;
S side = his right side, he faces screen-right; T three-quarter; TOP = from
above, he faces up).  Columns are beats, events and in-between moments.
Coloured lines trace the tips listed in rig.yaml (club tip, hands, feet).
Hit events are drawn on the floor in red at the moment they happen, so you can
see whether the club lands inside the damage area.  Problems are printed and
written on the sheet in red.
"""
import argparse
import math
import sys
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

import numpy as np
from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).resolve().parent))
from rig import ROOT, RigConfig, Model, Clip, load_clips  # noqa: E402
from render import Renderer, VIEWS  # noqa: E402
import compile as anim_compile  # noqa: E402

TRACE_COLORS = [(255, 110, 80), (90, 200, 255), (255, 210, 80), (150, 255, 140), (220, 140, 255)]
BLOCK = 16.0   # model pixels per block


def font(size):
    for name in ("DejaVuSans.ttf", "arial.ttf", "C:/Windows/Fonts/arial.ttf"):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    return ImageFont.load_default()


# ---------------------------------------------------------------- hit areas
def entity_to_model(forward, side, y=0.0):
    """Entity frame in blocks (forward, his right) -> model space pixels (faces -Z, right = +X)."""
    return np.array([side * BLOCK, y, -forward * BLOCK])


def hit_polygon(e):
    """Floor polygon (model space) of a hit event; see README for the shapes."""
    shape = e.get("shape", "circle")
    r = float(e.get("radius", 2.0))
    fwd, side = float(e.get("forward", 0.0)), float(e.get("side", 0.0))
    pts = []
    if shape == "circle":
        for k in range(28):
            a = 2 * math.pi * k / 28
            pts.append(entity_to_model(fwd + r * math.cos(a), side + r * math.sin(a), 0.2))
    elif shape == "arc":
        arc = math.radians(float(e.get("arc", 120)))
        center = math.radians(float(e.get("dir", 0)))     # 0 = straight ahead, + = to his right
        pts.append(entity_to_model(fwd, side, 0.2))
        for k in range(21):
            a = center - arc / 2 + arc * k / 20
            pts.append(entity_to_model(fwd + r * math.cos(a), side + r * math.sin(a), 0.2))
    return pts


def in_hit_area(e, p):
    """Is a model-space point (horizontally) inside the event's area?  Returns (inside, text)."""
    fwd = -p[2] / BLOCK
    side = p[0] / BLOCK
    dx, dz = fwd - float(e.get("forward", 0.0)), side - float(e.get("side", 0.0))
    dist = math.hypot(dx, dz)
    r = float(e.get("radius", 2.0))
    if e.get("shape", "circle") == "arc":
        ang = math.degrees(math.atan2(dz, dx)) - float(e.get("dir", 0))
        ang = (ang + 180) % 360 - 180
        ok = dist <= r + 0.3 and abs(ang) <= float(e.get("arc", 120)) / 2 + 10
        return ok, f"{dist:.1f} bl. od središča, kot {ang:+.0f}°"
    return dist <= r + 0.3, f"{dist:.1f} bl. od središča (r={r})"


# ---------------------------------------------------------------- scene
class Scene:
    def __init__(self, rig: RigConfig):
        self.rig = rig
        self.model = Model(rig.geo)
        self.renderer = Renderer(self.model, rig.texture)
        self.bind_low = (float(rig.floor) if rig.floor is not None
                         else self.model.feet_low(rig.feet) if rig.feet else 0.0)

    def tip(self, mats, name):
        spec = self.rig.tips[name]
        return self.model.point(mats, spec["bone"], spec["at"])

    def check(self, pose, t, airborne=False):
        mats = self.model.world_matrices(pose)
        problems = []
        lows = []
        for foot in self.rig.feet:
            y = self.model.lowest_y(mats, foot) - self.bind_low
            lows.append(y)
            if y < -1.0:
                problems.append(f"t={t:.2f}: {foot} {abs(y):.1f} px v tleh")
        if lows and not airborne and min(lows) > 1.5:
            problems.append(f"t={t:.2f}: nobena noga ne stoji na tleh ({min(lows):.1f} px zraka)")
        for name in self.rig.tips:
            if name.startswith("foot") or self.rig.tips[name]["bone"] not in mats:
                continue        # feet are checked above; hidden props have no tip
            p = self.tip(mats, name)
            if p[1] < -3.0:
                problems.append(f"t={t:.2f}: {name} gre {abs(p[1]):.0f} px v tla")
            own = set(self.model.descendants(self.rig.tips[name]["bone"]))
            for part in self.rig.solid:
                if part not in own and part in mats and self.model.inside(mats, part, p):
                    problems.append(f"t={t:.2f}: {name} gre skozi {part}")
        return problems


def render_sheet(scene, frames, title, views, traces=None, notes=(), problems=(), cell=(340, 420),
                 ppu=1.9, marks=None):
    """frames: [(label, pose)]; marks: {frame index: [(polygon, rgb)]}."""
    W, H = cell
    ppu *= scene.rig.preview["scale"]
    head_h = 46
    note_h = 18 * (len(notes) + len(problems)) + (10 if notes or problems else 0)
    label_w = 40
    sheet = Image.new("RGB", (label_w + W * len(frames), head_h + H * len(views) + note_h), (22, 28, 34))
    d = ImageDraw.Draw(sheet)
    f_title, f_small = font(20), font(14)
    d.text((8, 10), title, font=f_title, fill=(240, 240, 240))
    for r, view in enumerate(views):
        y0 = head_h + r * H
        d.text((4, y0 + H // 2), {"top": "TOP"}.get(view, view[0].upper()), font=f_small if view == "top" else f_title,
               fill=(150, 170, 180))
        yaw, pitch = VIEWS[view], (88 if view == "top" else 8)
        center = {"side": (W * scene.rig.preview["side"], H * 0.86),
                  "three_quarter": (W * scene.rig.preview["three_quarter"], H * 0.86),
                  "top": (W * 0.5, H * 0.62)}.get(view, (W * 0.5, H * 0.86))
        vppu = ppu * (0.75 if view == "top" else 1.0)
        for c, (label, pose) in enumerate(frames):
            x0 = label_w + c * W
            img, mats, V, ctr = scene.renderer.render(pose, yaw=yaw, pitch=pitch, size=(W, H), px_per_unit=vppu,
                                                      center=center, floor_marks=(marks or {}).get(c, ()))
            dd = ImageDraw.Draw(img)
            for i, (name, pts) in enumerate((traces or {}).items()):
                col = TRACE_COLORS[i % len(TRACE_COLORS)]
                xy = [scene.renderer.project(V, p, vppu, ctr) for p in pts]
                if len(xy) > 1:
                    dd.line(xy, fill=col, width=2)
                if name not in scene.rig.tips or scene.rig.tips[name]["bone"] not in mats:
                    continue        # projectile arcs have no moving marker
                cur = scene.renderer.project(V, scene.tip(mats, name), vppu, ctr)
                dd.ellipse((cur[0] - 4, cur[1] - 4, cur[0] + 4, cur[1] + 4), outline=col, width=2)
            sheet.paste(img, (x0, y0))
            d.rectangle((x0, y0, x0 + W - 1, y0 + H - 1), outline=(55, 68, 78))
            if r == 0:
                d.text((x0 + 6, y0 + 4), label, font=f_small, fill=(255, 235, 150))
    y = head_h + H * len(views) + 6
    if traces:
        x = 8
        for i, name in enumerate(traces):
            d.text((x, y), "— " + name, font=f_small, fill=TRACE_COLORS[i % len(TRACE_COLORS)])
            x += 110
        y += 18
    for n in notes:
        d.text((8, y), n, font=f_small, fill=(190, 205, 210))
        y += 18
    for p in problems:
        d.text((8, y), "! " + p, font=f_small, fill=(255, 110, 100))
        y += 18
    return sheet


def ballistic(dx, dy, dz, speed, gravity):
    """Same as EntityAnimatedNpc.ballistic(): initial velocity landing on (dx, dy, dz)."""
    d = math.hypot(dx, dz)
    ticks = int(max(6, round(d / speed)))
    s_t = (1 - 0.99 ** ticks) / 0.01
    sum_s = sum((1 - 0.99 ** i) / 0.01 for i in range(ticks))
    vy = (dy + gravity * sum_s) / s_t
    vh = d / s_t
    return (dx / d * vh if d else 0.0, vy, dz / d * vh if d else 0.0), ticks


def projectile_arc(e, target_forward):
    """Model-space points of a projectile's flight to a target on the floor `target_forward` blocks ahead."""
    f, side, h = e.get("origin", [1.0, 0.0, 1.5])
    gravity = 0.05      # EntityThrownRock.GRAVITY and arrows
    (vf, vy, vs), ticks = ballistic(target_forward - f, -h, -side, float(e.get("speed", 1.2)), gravity)
    pts, x, y, z = [], f, h, side
    for _ in range(ticks + 1):
        pts.append(entity_to_model(x, z, y * BLOCK))
        x, y, z = x + vf, y + vy, z + vs
        vf, vy, vs = vf * 0.99, vy * 0.99 - gravity, vs * 0.99
    return pts, ticks


def pick_times(length, keys, count):
    times = sorted(set(round(k, 3) for k in keys if 0 <= k <= length))
    if not times or len(times) > count:
        must = sorted(set(round(k, 3) for k in keys))
        times = list(np.linspace(0, length, count)) if not must else must[:count]
    while len(times) < count:
        gaps = [(b - a, a, b) for a, b in zip(times, times[1:])]
        if not gaps:
            break
        g, a, b = max(gaps)
        if g < 0.06:
            break
        times.append(round((a + b) / 2, 3))
        times.sort()
    return times


def out_dir(rig):
    d = rig.dir / "_preview"
    d.mkdir(parents=True, exist_ok=True)
    return d


# ---------------------------------------------------------------- commands
def preview_clip(args):
    rig = RigConfig(args.entity)
    src = rig.dir / f"{args.name}.anim.yaml"
    doc = {}
    if src.exists():
        doc = anim_compile.load_yaml(src)
        compiled, failed = anim_compile.compile_entity(args.entity, [args.name])
        if failed:
            sys.exit(1)
        clip = Clip(args.name, compiled[args.name])
    else:
        clips = load_clips(rig.output)
        if args.name not in clips:
            sys.exit(f"ni klipa '{args.name}'. Na voljo: {', '.join(clips)}")
        clip = clips[args.name]
    scene = Scene(rig)
    events = clip.events
    hits = [e for e in events if e["type"] == "hit"]
    airborne_doc = doc.get("ground") == "off"

    traces = {k: [] for k in (args.trace.split(",") if args.trace else rig.trace) if k}
    problems = []
    dense = np.linspace(0, clip.length, max(30, int(clip.length * 40)))
    for t in dense:
        pose = clip.sample(t)
        mats = scene.model.world_matrices(pose)
        for k in traces:
            if rig.tips[k]["bone"] in mats:
                traces[k].append(scene.tip(mats, k))
        for p in scene.check(pose, t, airborne_doc):
            key = p.split(": ", 1)[1]
            if not any(key.split(" ")[0] in q and q.endswith(key.split(" ")[-1]) for q in problems):
                problems.append(p)

    notes = [f"dolžina {clip.length:g} s{' (loop)' if clip.loop else ''}"
             + (f", stride {clip.data['stride']} blokov" if "stride" in clip.data else "")]
    for e in hits:
        pose = clip.sample(e["t"])
        mats = scene.model.world_matrices(pose)
        tip_name = e.get("tip", rig.weapon_tip)
        p = scene.tip(mats, tip_name)
        ok, text = in_hit_area(e, p)
        where = (f"{tip_name} pri t={e['t']}: naprej {-p[2] / BLOCK:.1f}, desno {p[0] / BLOCK:.1f}, "
                 f"višina {p[1] / BLOCK:.1f} blokov; {text}")
        notes.append(f"udarec '{e.get('id', '?')}' {e.get('shape', 'circle')} r={e.get('radius')} -> " + where)
        if not ok:
            problems.append(f"t={e['t']}: {tip_name} ni v območju udarca ({text})")
    shots = [e for e in events if e["type"] == "projectile"]
    for i, e in enumerate(shots):
        pts, ticks = projectile_arc(e, args.target)
        traces[f"let {e.get('kind', 'rock')}" + (f" {i + 1}" if len(shots) > 1 else "")] = pts
        o = e.get("origin", [0, 0, 0])
        notes.append(f"izstrelek '{e.get('kind', 'rock')}' pri t={e['t']}: iz naprej {o[0]:.1f}, desno {o[1]:.1f}, "
                     f"višina {o[2]:.1f}; do tarče {args.target:g} blokov naprej {ticks} tikov "
                     f"({ticks / 20:.2f} s), območje r={e.get('radius', 2)}")
    if events:
        notes.append("dogodki: " + ", ".join(f"{e['t']}s {e['type']}"
                                              + (f"({e.get('id') or e.get('sound') or e.get('foot') or ''})")
                                              for e in events))

    keys = [float(b["t"]) for b in doc.get("beats", [])] + [e["t"] for e in events]
    times = pick_times(clip.length, keys, args.frames)
    frames, marks = [], {}
    for c, t in enumerate(times):
        tags = [e.get("id") or e["type"] for e in events if abs(e["t"] - t) < 1e-3]
        frames.append((f"t={t:.2f}" + (f" [{', '.join(tags)}]" if tags else ""), clip.sample(t)))
        for e in hits:
            if abs(e["t"] - t) < 1e-3:
                marks.setdefault(c, []).append((hit_polygon(e), (255, 60, 50)))
        for e in shots:
            if abs(e["t"] - t) < 1e-3:
                landing = dict(shape="circle", forward=args.target, side=0, radius=e.get("radius", 2))
                marks.setdefault(c, []).append((hit_polygon(landing), (255, 150, 40)))
    views = args.views.split(",") if args.views else (
        ["side", "three_quarter", "top"] if hits else ["front", "side", "three_quarter"])
    sheet = render_sheet(scene, frames, f"{rig.entity}: {args.name}", views, traces, notes, problems, marks=marks)
    out = out_dir(rig) / f"{args.name}.png"
    sheet.save(out)
    print(f"slika -> {out.relative_to(ROOT)}")
    for n in notes:
        print("  " + n)
    if args.gif:
        gif_frames = []
        hit_times = [e["t"] for e in hits]
        for t in np.arange(0, clip.length + 1e-6, 1 / 20):
            fm = [(hit_polygon(e), (255, 60, 50)) for e in hits if 0 <= t - e["t"] < 0.15]
            img, *_ = scene.renderer.render(clip.sample(t), yaw=VIEWS["three_quarter"], size=(340, 420),
                                            px_per_unit=1.9 * rig.preview["scale"],
                                            center=(340 * rig.preview["three_quarter"], 420 * 0.86), floor_marks=fm)
            gif_frames.append(img)
        gif = out.with_suffix(".gif")
        gif_frames[0].save(gif, save_all=True, append_images=gif_frames[1:], duration=50, loop=0)
        print(f"gif -> {gif.relative_to(ROOT)}")
    for p in problems:
        print("TEŽAVA:", p)
    if not problems:
        print("preverjanje: brez težav")


def preview_poses(args):
    rig = RigConfig(args.entity)
    lib = anim_compile.Library(rig)
    names = list(lib.poses) if args.all else args.names
    scene = Scene(rig)
    frames, problems = [], []
    notes = []
    for n in names:
        warnings = []
        ch = anim_compile.beat_values({"pose": n}, lib, warnings, n)
        lib.ground(ch)
        pose = anim_compile.channels_to_pose(ch)
        problems += warnings
        problems += [p.replace("t=0.00", n) for p in scene.check(pose, 0.0)]
        frames.append((n, pose))
        tips = ", ".join(f"{t}: naprej {p['forward']:.1f} desno {p['side']:.1f} višina {p['height']:.1f}"
                         for t, p in ((t, lib.tip_position(ch, t)) for t in rig.trace))
        notes.append(f"{n}: {tips}")
        print(f"  {n}: {tips}")
    per = args.per_sheet
    for i in range(0, len(frames), per):
        chunk = frames[i:i + per]
        names_chunk = {f[0] for f in chunk}
        probs = [p for p in problems if p.split(":")[0] in names_chunk]
        nts = [n for n in notes if n.split(":")[0] in names_chunk]
        sheet = render_sheet(scene, chunk, f"{rig.entity}: poze (bloki)", args.views.split(","), notes=nts,
                             problems=probs, cell=(300, 380), ppu=1.7)
        stem = f"poze_{i // per + 1}" if len(frames) > per or args.all else "poza_" + "_".join(f[0] for f in chunk)
        out = out_dir(rig) / f"{stem}.png"
        sheet.save(out)
        print(f"slika -> {out.relative_to(ROOT)}")
    for p in problems:
        print("TEŽAVA:", p)


def preview_model(args):
    rig = RigConfig(args.entity)
    scene = Scene(rig)
    views = ["front", "three_quarter", "side", "back", "left"]
    W, H = 340, 420
    sheet = Image.new("RGB", (W * len(views), H + 30), (22, 28, 34))
    d = ImageDraw.Draw(sheet)
    for i, v in enumerate(views):
        cx = {"side": rig.preview["side"], "left": 1 - rig.preview["side"]}.get(v, 0.5)
        img, *_ = scene.renderer.render({}, yaw=VIEWS[v], size=(W, H), px_per_unit=3.4 * min(1.6, rig.preview["scale"]),
                                        center=(W * cx, H * 0.93))
        sheet.paste(img, (i * W, 30))
        d.text((i * W + 8, 6), v, font=font(16), fill=(255, 235, 150))
    out = out_dir(rig) / "model.png"
    sheet.save(out)
    print(f"slika -> {out.relative_to(ROOT)}")
    if args.face:
        # Centre the close-up on the head's boxes (long snouts sit far from the head pivot).
        from render import view_matrix
        head = "head" if "head" in scene.model.bones else scene.model.order[0]
        mats = scene.model.world_matrices({})
        pts = [(mats[n] @ c)[:3] for n in scene.model.descendants(head)
               for cube in scene.model.bones[n].cubes for c in cube.corners()]
        target = np.mean(pts, axis=0) if pts else np.zeros(3)
        size = float(np.ptp(np.array(pts), axis=0).max()) if pts else 16.0
        ppu = min(14.0, 300.0 / max(size, 1.0))
        for name, yaw in (("model_head", 25), ("model_face", 0)):
            w = view_matrix(yaw, 8) @ np.append(target, 1.0)
            img, *_ = scene.renderer.render({}, yaw=yaw, size=(400, 400), px_per_unit=ppu,
                                            center=(200 - w[0] * ppu, 200 + w[1] * ppu))
            out = out_dir(rig) / f"{name}.png"
            img.save(out)
            print(f"slika -> {out.relative_to(ROOT)}")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    c = sub.add_parser("clip", help="animacija iz animations_src/<entiteta>/<ime>.anim.yaml")
    c.add_argument("entity")
    c.add_argument("name")
    c.add_argument("--frames", type=int, default=7)
    c.add_argument("--views", help="npr. front,side,three_quarter,top,back")
    c.add_argument("--trace", help="imena iz rig.yaml -> tips, ločena z vejico")
    c.add_argument("--gif", action="store_true")
    c.add_argument("--target", type=float, default=12, help="izstrelki: tarča toliko blokov naprej (privzeto 12)")
    p = sub.add_parser("poses", help="galerija poz iz poses.yaml")
    p.add_argument("entity")
    p.add_argument("names", nargs="*")
    p.add_argument("--all", action="store_true")
    p.add_argument("--per-sheet", type=int, default=6)
    p.add_argument("--views", default="front,side")
    m = sub.add_parser("model", help="model z vseh strani")
    m.add_argument("entity")
    m.add_argument("--face", action="store_true", help="še bližnji posnetek glave")
    args = ap.parse_args()
    {"clip": preview_clip, "poses": preview_poses, "model": preview_model}[args.cmd](args)


if __name__ == "__main__":
    main()
