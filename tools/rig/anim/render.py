"""Software renderer: textured, z-buffered, orthographic, lit like Minecraft entities.

Used by preview.py to draw contact sheets that an AI (or a person) can look at.
"""
import math

import numpy as np
from PIL import Image

from rig import Model, euler_zyx

# Corner order for each face: top-left, top-right, bottom-right, bottom-left in
# texture space (Gecko model space).  Each corner picks x, y, z from (lo=0, hi=1).
# anim/GeoMesh.java uses the same table.
FACES = {
    "north": ((1, 1, 0), (0, 1, 0), (0, 0, 0), (1, 0, 0)),
    "south": ((0, 1, 1), (1, 1, 1), (1, 0, 1), (0, 0, 1)),
    "east":  ((1, 1, 1), (1, 1, 0), (1, 0, 0), (1, 0, 1)),
    "west":  ((0, 1, 0), (0, 1, 1), (0, 0, 1), (0, 0, 0)),
    "up":    ((0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1)),
    "down":  ((0, 0, 1), (1, 0, 1), (1, 0, 0), (0, 0, 0)),
}

# Minecraft's entity lighting: two lights fixed in the world, 0.6 diffuse each, 0.4 ambient.
_MC_LIGHTS = np.array([[0.2, 1.0, -0.7], [-0.2, 1.0, 0.7]])
_MC_LIGHTS /= np.linalg.norm(_MC_LIGHTS, axis=1, keepdims=True)


def mc_light(n):
    return float(min(1.0, 0.4 + 0.6 * np.clip(_MC_LIGHTS @ n, 0, None).sum()))


# Camera yaw (degrees around Y) for named views.  0 = looking at his face.
VIEWS = {"front": 0, "side": 90, "back": 180, "left": -90, "three_quarter": 35, "top": 0}


def view_matrix(yaw_deg, pitch_deg=8):
    m = euler_zyx(math.radians(pitch_deg), 0, 0) @ euler_zyx(0, math.radians(yaw_deg), 0)
    # Model faces -Z; a camera looking +Z sees his right hand (+X) on screen left.
    return np.diag([-1.0, 1, 1, 1]) @ m


class Renderer:
    def __init__(self, model: Model, texture_path, hidden_bones=()):
        self.model = model
        tex = Image.open(texture_path).convert("RGBA")
        self.tex = np.asarray(tex).astype(np.float32)
        self.th, self.tw = self.tex.shape[:2]
        self.su = self.tw / model.description["texture_width"]
        self.sv = self.th / model.description["texture_height"]
        self.hidden = set(hidden_bones)
        self.faces = []
        for name in model.order:
            for cube in model.bones[name].cubes:
                lo, hi = cube.lo, cube.hi
                for face, corners in FACES.items():
                    spec = cube.uv.get(face)
                    if not spec:
                        continue
                    u0, v0 = spec["uv"]
                    du, dv = spec["uv_size"]
                    pts = np.array([[hi[0] if c[0] else lo[0], hi[1] if c[1] else lo[1],
                                     hi[2] if c[2] else lo[2], 1.0] for c in corners])
                    pts = (cube.local @ pts.T).T
                    if np.linalg.norm(np.cross(pts[1, :3] - pts[0, :3], pts[3, :3] - pts[0, :3])) < 1e-6:
                        continue
                    uv = np.array([[u0, v0], [u0 + du, v0], [u0 + du, v0 + dv], [u0, v0 + dv]], dtype=float)
                    uv[:, 0] *= self.su
                    uv[:, 1] *= self.sv
                    self.faces.append((name, pts, uv))

    def render(self, pose, yaw=0, pitch=8, px_per_unit=3.0, size=(300, 380), background=(34, 44, 52),
               floor_marks=(), center=None):
        """floor_marks: [(polygon points on the floor in model space (N,3), rgb)] drawn on the ground."""
        W, H = size
        mats = self.model.world_matrices(pose, hidden=self.hidden)
        V = view_matrix(yaw, pitch)
        color = np.zeros((H, W, 3), np.float32)
        color[:] = background
        zbuf = np.full((H, W), np.inf, np.float32)
        cx, cy = center if center else (W / 2, H * 0.86)
        self._draw_floor(color, V, px_per_unit, cx, cy)
        for poly, rgb in floor_marks:
            self._draw_floor_poly(color, zbuf, V, px_per_unit, cx, cy, poly, rgb)
        for bone, pts, uv in self.faces:
            m = mats.get(bone)
            if m is None:
                continue
            w = (V @ m @ pts.T).T[:, :3]
            sx = cx + w[:, 0] * px_per_unit
            sy = cy - w[:, 1] * px_per_unit
            wm = (m @ pts.T).T[:, :3]
            n = -np.cross(wm[1] - wm[0], wm[3] - wm[0])   # outward (mirrored x flips the winding)
            nn = np.linalg.norm(n)
            if nn < 1e-9:
                continue
            shade = mc_light(n / nn)
            for tri in ((0, 1, 2), (0, 2, 3)):
                t = list(tri)
                self._raster(color, zbuf, sx[t], sy[t], w[t, 2], uv[t], shade)
        return Image.fromarray(np.clip(color, 0, 255).astype(np.uint8)), mats, V, (cx, cy)

    def project(self, V, point, ppu, center):
        w = V @ np.append(point, 1.0)
        return center[0] + w[0] * ppu, center[1] - w[1] * ppu

    def _draw_floor(self, color, V, ppu, cx, cy):
        """Grid of 1-block squares (16 px) on the ground."""
        H, W = color.shape[:2]
        for i in range(-128, 129, 16):
            for a, b in (((i, 0, -128), (i, 0, 128)), ((-128, 0, i), (128, 0, i))):
                pa, pb = V @ np.array([*a, 1.0]), V @ np.array([*b, 1.0])
                for k in np.linspace(0, 1, 400):
                    p = pa + (pb - pa) * k
                    x, y = int(cx + p[0] * ppu), int(cy - p[1] * ppu)
                    if 0 <= x < W and 0 <= y < H:
                        color[y, x] = color[y, x] * 0.6 + np.array([90, 110, 120]) * 0.4

    def _draw_floor_poly(self, color, zbuf, V, ppu, cx, cy, poly, rgb):
        pts = np.asarray(poly, dtype=float)
        w = (V @ np.column_stack((pts, np.ones(len(pts)))).T).T
        sx, sy = cx + w[:, 0] * ppu, cy - w[:, 1] * ppu
        c = np.array([sx.mean(), sy.mean()])
        for i in range(len(pts)):           # fan from the centre
            j = (i + 1) % len(pts)
            xs = np.array([c[0], sx[i], sx[j]])
            ys = np.array([c[1], sy[i], sy[j]])
            self._fill(color, xs, ys, rgb, 0.35)

    def _fill(self, color, xs, ys, rgb, alpha):
        H, W = color.shape[:2]
        x0, x1 = max(int(xs.min()), 0), min(int(math.ceil(xs.max())), W - 1)
        y0, y1 = max(int(ys.min()), 0), min(int(math.ceil(ys.max())), H - 1)
        if x1 < x0 or y1 < y0:
            return
        area = (xs[1] - xs[0]) * (ys[2] - ys[0]) - (xs[2] - xs[0]) * (ys[1] - ys[0])
        if abs(area) < 1e-9:
            return
        gx, gy = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
        w0 = ((xs[1] - gx) * (ys[2] - gy) - (xs[2] - gx) * (ys[1] - gy)) / area
        w1 = ((xs[2] - gx) * (ys[0] - gy) - (xs[0] - gx) * (ys[2] - gy)) / area
        inside = (w0 >= 0) & (w1 >= 0) & (1 - w0 - w1 >= 0)
        region = color[y0:y1 + 1, x0:x1 + 1]
        region[inside] = region[inside] * (1 - alpha) + np.array(rgb) * alpha

    def _raster(self, color, zbuf, xs, ys, zs, uv, shade):
        H, W = zbuf.shape
        x0, x1 = max(int(math.floor(xs.min())), 0), min(int(math.ceil(xs.max())), W - 1)
        y0, y1 = max(int(math.floor(ys.min())), 0), min(int(math.ceil(ys.max())), H - 1)
        if x1 < x0 or y1 < y0:
            return
        area = (xs[1] - xs[0]) * (ys[2] - ys[0]) - (xs[2] - xs[0]) * (ys[1] - ys[0])
        if abs(area) < 1e-9:
            return
        gx, gy = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
        w0 = ((xs[1] - gx) * (ys[2] - gy) - (xs[2] - gx) * (ys[1] - gy)) / area
        w1 = ((xs[2] - gx) * (ys[0] - gy) - (xs[0] - gx) * (ys[2] - gy)) / area
        w2 = 1 - w0 - w1
        inside = (w0 >= -1e-4) & (w1 >= -1e-4) & (w2 >= -1e-4)
        if not inside.any():
            return
        z = w0 * zs[0] + w1 * zs[1] + w2 * zs[2]
        u = w0 * uv[0, 0] + w1 * uv[1, 0] + w2 * uv[2, 0]
        v = w0 * uv[0, 1] + w1 * uv[1, 1] + w2 * uv[2, 1]
        ui = np.clip(np.floor(u).astype(int), 0, self.tw - 1)
        vi = np.clip(np.floor(v).astype(int), 0, self.th - 1)
        texel = self.tex[vi, ui]
        region_z = zbuf[y0:y1 + 1, x0:x1 + 1]
        mask = inside & (texel[..., 3] > 25) & (z < region_z)
        if not mask.any():
            return
        region_z[mask] = z[mask]
        region_c = color[y0:y1 + 1, x0:x1 + 1]
        region_c[mask] = texel[..., :3][mask] * shade
