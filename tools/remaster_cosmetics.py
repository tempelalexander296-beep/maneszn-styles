"""Maneszn Styles – Cosmetic HD Remaster.

Re-renders every model cosmetic texture (and every cape frame) at 8 px per texel with
per-face lighting, per-texel bevel, material detection (metal sheen, gem sparkle, cloth weave)
and brighter cores for glowing cosmetics. Geometry/UVs are untouched: Minecraft normalises UVs by
the declared tex_size, so a higher-resolution image drops straight in.

Run from the repo root:  python3 tools/remaster_cosmetics.py <assets/maneszn_styles dir> [--only id,id]
Idempotent: originals are cached in tools/remaster_src/ on first run and always re-read from there.
"""
import colorsys, json, math, os, random, shutil, sys
from PIL import Image

SCALE = 8  # output pixels per logical texel

def clamp(v): return 0 if v < 0 else 255 if v > 255 else int(v)

def logical(img, tw, th):
    """Sample the logical texel grid (block centres) of an upscaled pixel-art texture."""
    W, H = img.size
    sx, sy = W / tw, H / th
    px = img.load()
    out = Image.new('RGBA', (tw, th))
    op = out.load()
    for y in range(th):
        for x in range(tw):
            op[x, y] = px[min(W - 1, int((x + .5) * sx)), min(H - 1, int((y + .5) * sy))]
    return out

def material(r, g, b):
    h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
    if s > .45 and .09 < h < .17 and l > .35: return 'gold'
    if s < .12 and l > .62: return 'silver'
    if s > .55 and l > .25 and l < .75 and (h < .03 or h > .92 or .53 < h < .78 or .27 < h < .45): return 'gem'
    if l < .12: return 'dark'
    return 'cloth'

def face_map(cosmetic, tw, th):
    """texel -> (face kind, fx, fy, fw, fh) from Minecraft box-UV layout of every cube."""
    m = {}
    for part in cosmetic.get('parts', []):
        for c in part.get('cubes', []):
            u, v = c['uv']; w, h, d = (max(0, int(math.ceil(s))) for s in c['size'])
            faces = [('top', u + d, v, w, d), ('bottom', u + d + w, v, w, d),
                     ('side', u, v + d, d, h), ('side', u + d, v + d, w, h),
                     ('side', u + d + w, v + d, d, h), ('side', u + 2 * d + w, v + d, w, h)]
            for kind, fx, fy, fw, fh in faces:
                for yy in range(fy, fy + fh):
                    for xx in range(fx, fx + fw):
                        if 0 <= xx < tw and 0 <= yy < th: m[(xx, yy)] = (kind, fx, fy, fw, fh)
    return m

def render(img, tw, th, fmap, glow, rng, cape=False):
    """img: original texture (any multiple of tw x th). Output: SCALE px per texel, detail preserved."""
    W, H = img.size
    sx = W / tw
    up = img.resize((tw * SCALE, th * SCALE), Image.NEAREST)
    up_p = up.load(); src = img.load()
    out = Image.new('RGBA', up.size, (0, 0, 0, 0)); op = out.load()
    S = SCALE
    uniform = {}
    for y in range(th):
        for x in range(tw):
            x0, y0 = int(x * sx), int(y * sx)
            c0 = src[min(W - 1, x0), min(H - 1, y0)]
            uniform[(x, y)] = all(src[min(W - 1, x0 + i), min(H - 1, y0 + j)] == c0 for i in range(max(1, int(sx))) for j in range(max(1, int(sx))))
    for Y in range(th * S):
        y, py = divmod(Y, S)
        for X in range(tw * S):
            x, pxl = divmod(X, S)
            r, g, b, a = up_p[X, Y]
            if a == 0: continue
            kind, fx, fy, fw, fh = fmap.get((x, y), ('side', 0, 0, tw, th) if cape else ('side', x, y, 1, 1))
            mat = material(r, g, b)
            fxp = (x - fx + pxl / S) / max(1, fw); fyp = (y - fy + py / S) / max(1, fh)
            if cape: k = 1.06 - .14 * fyp
            elif kind == 'top': k = 1.10
            elif kind == 'bottom': k = .80
            else: k = 1.07 - .17 * fyp
            add = 0
            if not cape and uniform[(x, y)]:
                e = min(pxl, py, S - 1 - pxl, S - 1 - py)
                if e == 0: k *= 1.07 if (pxl == 0 or py == 0) else .90
                elif e == 1: k *= 1.02 if (pxl == 1 or py == 1) else .98
            if mat in ('gold', 'silver'):
                band = math.sin((fxp - fyp * .6) * math.pi * 1.4)
                k *= 1 + .13 * band
                if band > .96: add += 30
            elif mat == 'gem' and not cape:
                if uniform[(x, y)] and 1 <= pxl <= 2 and 1 <= py <= 2: add += 65
            elif mat == 'cloth':
                k *= 1 + (((X + Y) % 4 == 0) * .03 - ((X - Y) % 4 == 0) * .025)
            if glow:
                cx = abs(pxl - (S - 1) / 2) / S; cy = abs(py - (S - 1) / 2) / S
                add += int(70 * max(0, .5 - math.hypot(cx, cy)))
            n = (rng.random() - .5) * (8 if mat == 'cloth' else 4)
            op[X, Y] = (clamp(r * k + add + n), clamp(g * k + add + n), clamp(b * k + add + n), a)
    return out

def main():
    assets = sys.argv[1]
    only = set(sys.argv[sys.argv.index('--only') + 1].split(',')) if '--only' in sys.argv else None
    cache = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'remaster_src')
    data = json.load(open(os.path.join(assets, 'builtin/cosmetics.json')))['cosmetics']
    done = 0
    for c in data:
        if only and c['id'] not in only: continue
        tex = c.get('texture')
        if not tex: continue
        files = []
        if c.get('parts'): files = [tex + '.png']
        elif c['type'] == 'cape':
            n = int(c.get('frames') or 1)
            files = [tex + '.png'] if n <= 1 else [f'{tex}_{i}.png' for i in range(n)]
        for f in files:
            path = os.path.join(assets, f)
            if not os.path.exists(path): continue
            orig = os.path.join(cache, f)
            if not os.path.exists(orig):
                os.makedirs(os.path.dirname(orig), exist_ok=True); shutil.copy2(path, orig)
            img = Image.open(orig).convert('RGBA')
            if c.get('parts'):
                tw, th = c['tex_size']; fmap = face_map(c, tw, th); cape = False
            else:
                tw, th = 64, max(1, round(64 * img.size[1] / img.size[0])); fmap = {}; cape = True
            rng = random.Random(sum(map(ord, c['id'])))
            render(img, tw, th, fmap, bool(c.get('glow')), rng, cape).save(path, optimize=True)
            done += 1
    print('remastered textures:', done)

if __name__ == '__main__':
    main()
