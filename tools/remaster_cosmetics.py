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

SCALE = 16       # output pixels per logical texel for 3D models (HD)
CAPE_SCALE = 4   # capes keep their original resolution (fine art, many animation frames)

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

METAL_WORDS = ('silber', 'silver', 'eisen', 'iron', 'diamant', 'diamond', 'schwert', 'sword', 'ruestung', 'rüstung', 'armor',
               'krone', 'crown', 'ritter', 'knight', 'helm', 'stahl', 'steel', 'netherit', 'chrom', 'robot', 'axt', 'hammer', 'klinge', 'blade')

def material(r, g, b, metal_ok=False, isolated=False):
    h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
    if s > .45 and .09 < h < .17 and l > .35: return 'gold'
    if metal_ok and s < .12 and l > .55: return 'silver'
    if isolated and s > .5 and .2 < l < .8: return 'gem'
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

def render(img, tw, th, fmap, glow, rng, cape=False, metal_ok=False):
    """Vectorised remaster. img: original texture (any multiple of tw x th). Output: SCALE px per texel."""
    import numpy as np
    S = CAPE_SCALE if cape else SCALE
    W, H = img.size
    src = np.asarray(img, dtype=np.float32)
    up = np.asarray(img.resize((tw * S, th * S), Image.NEAREST), dtype=np.float32)
    OH, OW = up.shape[:2]
    # logical texel colours (block centres)
    cy = np.minimum(H - 1, ((np.arange(th) + .5) * H / th).astype(int))
    cx = np.minimum(W - 1, ((np.arange(tw) + .5) * W / tw).astype(int))
    lg = src[cy][:, cx]                                   # (th, tw, 4)
    def far(dx, dy):
        sh = np.zeros((th, tw), bool)
        nb = np.full((th + 2, tw + 2, 4), -1000.0, np.float32); nb[1:-1, 1:-1] = lg
        q = nb[1 + dy:1 + dy + th, 1 + dx:1 + dx + tw]
        return (q[..., 3] <= 0) | (np.abs(q[..., :3] - lg[..., :3]).sum(-1) > 90)
    bl, br, bt, bb = far(-1, 0), far(1, 0), far(0, -1), far(0, 1)
    isolated_t = (bl.astype(int) + br + bt + bb) >= 3
    # per-texel face data
    kind_t = np.zeros((th, tw), np.int8); fx_t = np.zeros((th, tw)); fy_t = np.zeros((th, tw)); fw_t = np.ones((th, tw)); fh_t = np.ones((th, tw))
    if cape:
        fw_t[:] = tw; fh_t[:] = th
    else:
        fx_t[:] = np.arange(tw)[None, :]; fy_t[:] = np.arange(th)[:, None]
    for (x, y), (kind, fx, fy, fw, fh) in fmap.items():
        kind_t[y, x] = {'side': 0, 'top': 1, 'bottom': 2}[kind]; fx_t[y, x] = fx; fy_t[y, x] = fy; fw_t[y, x] = max(1, fw); fh_t[y, x] = max(1, fh)
    Y, X = np.mgrid[0:OH, 0:OW]
    ty, tx = Y // S, X // S; py, pxl = Y % S, X % S
    kind = kind_t[ty, tx]; fw = fw_t[ty, tx]; fh = fh_t[ty, tx]
    fxp = (tx - fx_t[ty, tx] + pxl / S) / fw; fyp = (ty - fy_t[ty, tx] + py / S) / fh
    r, g, b, a = up[..., 0], up[..., 1], up[..., 2], up[..., 3]
    mx = np.maximum(np.maximum(r, g), b) / 255; mn = np.minimum(np.minimum(r, g), b) / 255
    l = (mx + mn) / 2; d = mx - mn
    sat = np.where(d == 0, 0, d / np.where(l > .5, 2 - mx - mn, mx + mn + 1e-9))
    rn, gn, bn = r / 255, g / 255, b / 255
    hue = np.where(d == 0, 0, np.where(mx == rn, ((gn - bn) / (d + 1e-9)) % 6, np.where(mx == gn, (bn - rn) / (d + 1e-9) + 2, (rn - gn) / (d + 1e-9) + 4))) / 6
    gold = (sat > .45) & (hue > .09) & (hue < .17) & (l > .35)
    silver = (~gold) & metal_ok & (sat < .12) & (l > .55)
    gem = (~gold) & (~silver) & isolated_t[ty, tx] & (sat > .5) & (l > .2) & (l < .8) & (not cape)
    cloth = ~(gold | silver | gem | (l < .12))
    if cape:
        k = 1.06 - .14 * fyp
    else:
        k = np.where(kind == 1, 1.10, np.where(kind == 2, .80, 1.07 - .17 * fyp))
    add = np.zeros_like(k)
    if not cape:
        edge = np.minimum(np.minimum(fxp, 1 - fxp) * fw, np.minimum(fyp, 1 - fyp) * fh)
        k = np.where(edge < .5, k * (.80 + .40 * edge), k)
        add += np.where((fyp * fh < .25) & (kind == 0), 22, 0)
        rim = max(1, S // 8)
        k *= np.where(bl[ty, tx] & (pxl < rim), 1.10, 1)
        k *= np.where(bt[ty, tx] & (py < rim), 1.12, 1)
        k *= np.where(br[ty, tx] & (pxl >= S - rim), .86, 1)
        k *= np.where(bb[ty, tx] & (py >= S - rim), .82, 1)
    band = np.sin((fxp - fyp * .6) * np.pi * 1.4)
    metal = gold | silver
    k = np.where(metal, k * (1 + .13 * band), k)
    add += np.where(metal & (band > .96), 30, 0)
    k = np.where(gem, k * np.where(pxl + py < S, 1.10, .82), k)
    add += np.where(gem & (pxl >= S // 8) & (pxl <= S // 4) & (py >= S // 8) & (py <= S // 4), 80, 0)
    k = np.where(cloth, k * (1 + ((X + Y) % 4 == 0) * .03 - ((X - Y) % 4 == 0) * .025), k)
    if glow:
        gcx = np.abs(pxl - (S - 1) / 2) / S; gcy = np.abs(py - (S - 1) / 2) / S
        add += 70 * np.maximum(0, .5 - np.hypot(gcx, gcy))
    gen = np.random.default_rng(rng.randrange(1 << 30))
    noise = (gen.random(k.shape) - .5) * np.where(cloth, 8, 4)
    rr, gg, bb2 = r * k + add + noise, g * k + add + noise, b * k + add + noise
    lum = .299 * rr + .587 * gg + .114 * bb2
    out = np.stack([lum + (rr - lum) * 1.15, lum + (gg - lum) * 1.15, lum + (bb2 - lum) * 1.15, a], -1)
    out[..., :3] = np.where(a[..., None] > 0, out[..., :3], 0)
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), 'RGBA')

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
            metal = any(w in c['id'].lower() or w in json.dumps(c.get('names', {})).lower() for w in METAL_WORDS)
            render(img, tw, th, fmap, bool(c.get('glow')), rng, cape, metal).save(path, optimize=True)
            done += 1
    print('remastered textures:', done)

if __name__ == '__main__':
    main()
