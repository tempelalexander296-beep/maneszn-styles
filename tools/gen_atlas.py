"""Generates the HD cosmetic texture atlas (2048x2048, 8x8 tiles of 256 px) and the Java tile index.
Run: python3 tools/gen_atlas.py"""
import math, random
from PIL import Image, ImageDraw, ImageFilter

T = 256; N = 8; S = T * N
TILES = [
    'GOLD', 'ROSE_GOLD', 'SILVER', 'DARK_METAL', 'ICE', 'GLASS', 'SNOW', 'FUR',
    'VELVET_RED', 'SATIN_RED', 'SATIN_GREEN', 'SATIN_BLACK', 'CANDY_RED', 'CANDY_GREEN', 'CANDY_GOLD', 'PEPPERMINT',
    'PAPER_RED_STARS', 'PAPER_GREEN_DOTS', 'PAPER_WHITE_FLAKES', 'PAPER_GOLD_STRIPES', 'PAPER_NIGHT_STARS', 'PAPER_ROYAL_PLAID', 'PAPER_RED_PLAID', 'PAPER_ICE_FLAKES',
    'LEAF', 'BERRY', 'WOOD', 'BARK', 'COAL', 'CARROT', 'SCALES', 'BELLY_GOLD',
    'PINE', 'GLOW_WHITE', 'GLOW_GOLD', 'GLOW_ICE', 'GLOW_RED', 'GLOW_GREEN', 'FLAME', 'CANDLE',
    'FUR_BROWN', 'CREAM', 'KNIT_RED', 'KNIT_GREEN', 'GEM_BLUE', 'GEM_RED', 'GEM_GREEN', 'ORNAMENT_RED',
    'ORNAMENT_GOLD', 'ORNAMENT_SILVER', 'ORNAMENT_GREEN', 'MANNEQUIN', 'SNOW_GROUND', 'RUNE_GOLD', 'RUNE_ICE', 'SPARKLE',
    'FEATHER_GOLD', 'MEMBRANE_RED', 'ANTLER', 'BEAM', 'STAR_GOLD', 'WHITE', 'BLACK', 'HOLLY_DARK',
]
assert len(TILES) <= N * N
rnd = random.Random(2412)

def lerp(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))
def hexc(h, a=255): h = h.lstrip('#'); return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)

def noise_layer(img, amount=18, scale=1):
    px = img.load()
    for y in range(T):
        for x in range(T):
            r, g, b, a = px[x, y]
            n = rnd.randint(-amount, amount)
            px[x, y] = (max(0, min(255, r + n)), max(0, min(255, g + n)), max(0, min(255, b + n)), a)
    return img

def metal(c1, c2, c3):
    im = Image.new('RGBA', (T, T))
    d = ImageDraw.Draw(im)
    for y in range(T):
        t = y / T
        w = 0.5 + 0.5 * math.sin(t * math.pi * 2.2 + 0.4)
        col = lerp(hexc(c1), hexc(c2), w) if t < 0.5 else lerp(hexc(c2), hexc(c3), (t - 0.5) * 2 * (1 - w * 0.5))
        d.line([(0, y), (T, y)], fill=col)
    for i in range(140):  # brushed micro-scratches + glints
        x = rnd.randint(0, T); y = rnd.randint(0, T); l = rnd.randint(8, 40)
        d.line([(x, y), (x + l, y + rnd.randint(-1, 1))], fill=(255, 255, 255, rnd.randint(18, 60)))
    return noise_layer(im, 6)

def flat(c, n=10): return noise_layer(Image.new('RGBA', (T, T), hexc(c)), n)

def glow(c):
    im = Image.new('RGBA', (T, T), hexc(c))
    d = ImageDraw.Draw(im)
    for r in range(T // 2, 0, -2):
        d.ellipse([T / 2 - r, T / 2 - r, T / 2 + r, T / 2 + r], fill=lerp(hexc(c), (255, 255, 255, 255), 1 - r / (T / 2)))
    return im

def stripes(base, stripe, thin=None, count=4):
    im = Image.new('RGBA', (T * 2, T * 2), hexc(base)); d = ImageDraw.Draw(im); w = (T * 2) / count
    for i in range(-count, count * 2):
        x = i * w
        d.polygon([(x, 0), (x + w * 0.45, 0), (x + w * 0.45 + T * 2, T * 2), (x + T * 2, T * 2)], fill=hexc(stripe))
        if thin: d.polygon([(x + w * 0.6, 0), (x + w * 0.68, 0), (x + w * 0.68 + T * 2, T * 2), (x + w * 0.6 + T * 2, T * 2)], fill=hexc(thin))
    im = im.resize((T, T), Image.LANCZOS)
    d = ImageDraw.Draw(im)
    for _ in range(500): x, y = rnd.randint(0, T - 1), rnd.randint(0, T - 1); d.point((x, y), fill=(255, 255, 255, 90))
    return im

def star(d, cx, cy, ro, ri, fill, pts=5):
    d.polygon([(cx + math.cos(i * math.pi / pts - math.pi / 2) * (ro if i % 2 == 0 else ri), cy + math.sin(i * math.pi / pts - math.pi / 2) * (ro if i % 2 == 0 else ri)) for i in range(pts * 2)], fill=fill)

def flake(d, cx, cy, r, fill, w=2):
    for i in range(6):
        a = i * math.pi / 3; dx, dy = math.cos(a), math.sin(a)
        d.line([(cx, cy), (cx + dx * r, cy + dy * r)], fill=fill, width=w)
        for t in (0.45, 0.72):
            bx, by, bl = cx + dx * r * t, cy + dy * r * t, r * 0.3 * (1.1 - t)
            for s in (-1, 1):
                ba = a + s * 0.75
                d.line([(bx, by), (bx + math.cos(ba) * bl, by + math.sin(ba) * bl)], fill=fill, width=w)

def paper(c1, c2, ink, motif):
    im = Image.new('RGBA', (T, T)); d = ImageDraw.Draw(im)
    for y in range(T): d.line([(0, y), (T, y)], fill=lerp(hexc(c1), hexc(c2), y / T))
    n = 4; cell = T / n
    for j in range(n):
        for i in range(n):
            cx, cy = (i + 0.5 + (j % 2) * 0.5) * cell, (j + 0.5) * cell
            if motif == 'stars': star(d, cx, cy, cell * 0.24, cell * 0.1, hexc(ink))
            elif motif == 'dots': d.ellipse([cx - cell * .13, cy - cell * .13, cx + cell * .13, cy + cell * .13], fill=hexc(ink))
            elif motif == 'flakes': flake(d, cx, cy, cell * 0.3, hexc(ink), 3)
    if motif == 'stripes':
        for i in range(n * 2): d.rectangle([i * cell / 2, 0, i * cell / 2 + cell * 0.12, T], fill=hexc(ink))
    if motif == 'plaid':
        ov = Image.new('RGBA', (T, T), (0, 0, 0, 0)); od = ImageDraw.Draw(ov)
        for i in range(n):
            od.rectangle([i * cell, 0, i * cell + cell * 0.3, T], fill=hexc(ink, 80)); od.rectangle([0, i * cell, T, i * cell + cell * 0.3], fill=hexc(ink, 80))
            od.line([(i * cell + cell * .65, 0), (i * cell + cell * .65, T)], fill=hexc(ink, 200), width=2); od.line([(0, i * cell + cell * .65), (T, i * cell + cell * .65)], fill=hexc(ink, 200), width=2)
        im = Image.alpha_composite(im, ov)
    return noise_layer(im, 5)

def knit(c):
    im = Image.new('RGBA', (T, T), hexc(c)); d = ImageDraw.Draw(im)
    dark = lerp(hexc(c), (0, 0, 0, 255), 0.35); light = lerp(hexc(c), (255, 255, 255, 255), 0.2)
    for y in range(0, T, 16):
        for x in range(0, T, 16):
            d.polygon([(x, y), (x + 8, y + 12), (x + 8, y + 16), (x, y + 4)], fill=light)
            d.polygon([(x + 16, y), (x + 8, y + 12), (x + 8, y + 16), (x + 16, y + 4)], fill=dark)
    return noise_layer(im, 8)

def scales():
    im = Image.new('RGBA', (T, T), hexc('#06301a')); d = ImageDraw.Draw(im); w = T / 8
    for row in range(-1, 12):
        for col in range(-1, 9):
            x, y = (col + (row % 2) * 0.5) * w, row * w * 0.7
            for k in range(10, 0, -1):
                d.pieslice([x - w * .5 * k / 10, y - w * .2 * k / 10 + w * .3, x + w * .5 * k / 10, y + w * .8 * k / 10 + w * .3], 0, 180, fill=lerp(hexc('#1a7a40'), hexc('#5fd08a'), 1 - k / 10))
    return im

def feather():
    im = Image.new('RGBA', (T, T)); d = ImageDraw.Draw(im)
    for y in range(T): d.line([(0, y), (T, y)], fill=lerp(hexc('#b8862b'), hexc('#ffe39a'), y / T))
    for i in range(0, T, 6): d.line([(T / 2, i), (0, i + 30)], fill=hexc('#8a5c12', 120)); d.line([(T / 2, i), (T, i + 30)], fill=hexc('#fff1c2', 90))
    d.rectangle([T / 2 - 3, 0, T / 2 + 3, T], fill=hexc('#fff6d0'))
    return im

def rune(c):
    im = Image.new('RGBA', (T, T), (0, 0, 0, 0)); d = ImageDraw.Draw(im); col = hexc(c); cx = T / 2
    for r in (0.47, 0.44, 0.31, 0.29): d.ellipse([cx - T * r, cx - T * r, cx + T * r, cx + T * r], outline=col, width=3)
    for i in range(16):
        a = i / 16 * math.pi * 2; star(d, cx + math.cos(a) * T * .375, cx + math.sin(a) * T * .375, 7, 3, col)
    flake(d, cx, cx, T * 0.2, col, 3)
    return im.filter(ImageFilter.GaussianBlur(0.6))

def sparkle():
    im = Image.new('RGBA', (T, T), (0, 0, 0, 0)); d = ImageDraw.Draw(im); c = T / 2
    for r in range(int(T * .12), 0, -1): d.ellipse([c - r, c - r, c + r, c + r], fill=(255, 255, 255, int(255 * (1 - r / (T * .12)) ** 1.5)))
    for w, h in ((4, T * .48), (T * .48, 4)): d.ellipse([c - w, c - h, c + w, c + h], fill=(255, 255, 255, 200))
    return im.filter(ImageFilter.GaussianBlur(1.5))

def beam():
    im = Image.new('RGBA', (T, T)); px = im.load()
    for y in range(T):
        for x in range(T):
            a = (1 - abs(x / T - .5) * 2) ** 2 * min(1, y / (T * .25), (T - y) / (T * .25))
            px[x, y] = (255, 255, 255, int(255 * a))
    return im

def peppermint():
    im = Image.new('RGBA', (T, T), hexc('#fbf7f0')); d = ImageDraw.Draw(im); c = T / 2
    for i in range(8):
        pts = [(c, c)] + [(c + math.cos(i / 8 * 6.283 + k / 30 * 1.2) * k / 30 * c, c + math.sin(i / 8 * 6.283 + k / 30 * 1.2) * k / 30 * c) for k in range(31)]
        pts += [(c + math.cos(i / 8 * 6.283 + .32 + k / 30 * 1.2) * k / 30 * c, c + math.sin(i / 8 * 6.283 + .32 + k / 30 * 1.2) * k / 30 * c) for k in range(30, -1, -1)]
        d.polygon(pts, fill=hexc('#c8102e'))
    return im

def ornament(c):
    im = Image.new('RGBA', (T, T)); d = ImageDraw.Draw(im)
    base = hexc(c)
    for y in range(T): d.line([(0, y), (T, y)], fill=lerp(lerp(base, (255, 255, 255, 255), 0.45), lerp(base, (0, 0, 0, 255), 0.45), y / T))
    for y in range(int(T * .2), int(T * .28)): d.line([(0, y), (T, y)], fill=hexc('#ffe39a'))
    for i in range(10): star(d, 13 + i * 25.6, T * 0.6, 7, 3, hexc('#fff1c2'))
    return im

def antler():
    im = flat('#6b4428', 14); d = ImageDraw.Draw(im)
    for _ in range(80): y = rnd.randint(0, T); d.line([(0, y), (T, y + rnd.randint(-6, 6))], fill=hexc('#4a2c19', 90))
    return im

def ice():
    im = Image.new('RGBA', (T, T)); d = ImageDraw.Draw(im)
    for y in range(T): d.line([(0, y), (T, y)], fill=lerp(hexc('#e8f8ff', 170), hexc('#5fb0e8', 200), y / T))
    for _ in range(26):
        x, y = rnd.randint(0, T), rnd.randint(0, T); a = rnd.random() * 6.28; l = rnd.randint(20, 90)
        d.line([(x, y), (x + math.cos(a) * l, y + math.sin(a) * l)], fill=(255, 255, 255, 160), width=2)
    return im

def wood():
    im = flat('#3a1d10', 6); d = ImageDraw.Draw(im)
    for y in range(0, T, 3): d.line([(0, y), (T, y + int(math.sin(y * .1) * 4))], fill=hexc('#2a1208', 120))
    return im

gen = {
    'GOLD': lambda: metal('#fff1c2', '#e3b553', '#8a5c12'), 'ROSE_GOLD': lambda: metal('#ffe0cc', '#f2b48c', '#9a5a3a'),
    'SILVER': lambda: metal('#ffffff', '#cfd8e2', '#6a7686'), 'DARK_METAL': lambda: metal('#5a5f68', '#2a2d33', '#101114'),
    'ICE': ice, 'GLASS': lambda: Image.new('RGBA', (T, T), (230, 245, 255, 70)), 'SNOW': lambda: flat('#f4f8ff', 12), 'FUR': lambda: flat('#f7f5f1', 26),
    'VELVET_RED': lambda: flat('#b0101f', 16), 'SATIN_RED': lambda: metal('#ff5a6e', '#c8102e', '#5a0610'), 'SATIN_GREEN': lambda: metal('#5fd08a', '#1f8a45', '#0b3a1e'),
    'SATIN_BLACK': lambda: metal('#5a5a70', '#1a1a22', '#050507'), 'CANDY_RED': lambda: stripes('#fbf7f0', '#c8102e', '#2f8f46'), 'CANDY_GREEN': lambda: stripes('#fbf7f0', '#1f7a3f'),
    'CANDY_GOLD': lambda: stripes('#c8102e', '#ffd36b'), 'PEPPERMINT': peppermint,
    'PAPER_RED_STARS': lambda: paper('#8e0e1c', '#b3162a', '#f3d27a', 'stars'), 'PAPER_GREEN_DOTS': lambda: paper('#0d4a2c', '#14663c', '#f3d27a', 'dots'),
    'PAPER_WHITE_FLAKES': lambda: paper('#ece7de', '#ffffff', '#b3162a', 'flakes'), 'PAPER_GOLD_STRIPES': lambda: paper('#a8781f', '#e3b553', '#7a1020', 'stripes'),
    'PAPER_NIGHT_STARS': lambda: paper('#0c1a33', '#1b3160', '#cfe8ff', 'stars'), 'PAPER_ROYAL_PLAID': lambda: paper('#3d0412', '#7a0c22', '#ffd36b', 'plaid'),
    'PAPER_RED_PLAID': lambda: paper('#8e0e1c', '#b3162a', '#0f5a2a', 'plaid'), 'PAPER_ICE_FLAKES': lambda: paper('#5f9fd0', '#b8def5', '#ffffff', 'flakes'),
    'LEAF': lambda: metal('#3aa05f', '#0f5a2a', '#06281a'), 'BERRY': lambda: metal('#ff6b7a', '#c40f22', '#4a0008'), 'WOOD': wood, 'BARK': lambda: flat('#4a2c19', 22),
    'COAL': lambda: flat('#161616', 20), 'CARROT': lambda: flat('#ff7a1a', 18), 'SCALES': scales, 'BELLY_GOLD': lambda: metal('#fff1c2', '#ffd98a', '#b8862b'),
    'PINE': lambda: noise_layer(Image.new('RGBA', (T, T), hexc('#0d4d2a')), 30), 'GLOW_WHITE': lambda: glow('#dfefff'), 'GLOW_GOLD': lambda: glow('#ffb52e'),
    'GLOW_ICE': lambda: glow('#5fb8ff'), 'GLOW_RED': lambda: glow('#ff2238'), 'GLOW_GREEN': lambda: glow('#2be07a'), 'FLAME': lambda: glow('#ff7a1a'), 'CANDLE': lambda: flat('#f6ecd6', 8),
    'FUR_BROWN': lambda: flat('#8a5a36', 24), 'CREAM': lambda: flat('#e9d3b4', 14), 'KNIT_RED': lambda: knit('#b3162a'), 'KNIT_GREEN': lambda: knit('#1f8a45'),
    'GEM_BLUE': lambda: glow('#2a8fe0'), 'GEM_RED': lambda: glow('#c8102e'), 'GEM_GREEN': lambda: glow('#1f9a4a'), 'ORNAMENT_RED': lambda: ornament('#c8102e'),
    'ORNAMENT_GOLD': lambda: ornament('#e3b553'), 'ORNAMENT_SILVER': lambda: ornament('#cfe8ff'), 'ORNAMENT_GREEN': lambda: ornament('#0f7a3c'),
    'MANNEQUIN': lambda: metal('#5a6a8a', '#2b3344', '#141821'), 'SNOW_GROUND': lambda: flat('#dfe9f7', 10), 'RUNE_GOLD': lambda: rune('#ffd36b'), 'RUNE_ICE': lambda: rune('#bfe9ff'),
    'SPARKLE': sparkle, 'FEATHER_GOLD': feather, 'MEMBRANE_RED': lambda: metal('#ff6a6a', '#b3162a', '#5a0610'), 'ANTLER': antler, 'BEAM': beam,
    'STAR_GOLD': lambda: metal('#fffbe6', '#ffd36b', '#c98a1a'), 'WHITE': lambda: Image.new('RGBA', (T, T), (255, 255, 255, 255)), 'BLACK': lambda: flat('#0b0b0f', 4),
    'HOLLY_DARK': lambda: metal('#2a7a45', '#0a3a1c', '#031a0c'),
}

atlas = Image.new('RGBA', (S, S), (0, 0, 0, 0))
for i, name in enumerate(TILES):
    tile = gen[name]().convert('RGBA').resize((T, T))
    atlas.paste(tile, ((i % N) * T, (i // N) * T))
atlas.save('common/src/main/resources/assets/adventcosmetics/textures/cosmetic/atlas.png', optimize=True)

with open('common/src/main/java/dev/maneszn/advent/mesh/Tex.java', 'w') as f:
    f.write('package dev.maneszn.advent.mesh;\n\n/** Tiles of the 2048x2048 HD cosmetic atlas (generated by tools/gen_atlas.py). */\npublic enum Tex {\n')
    f.write(',\n'.join(f'\t{n}' for n in TILES) + ';\n\n')
    f.write(f'\tpublic static final int GRID = {N};\n\tprivate static final float INSET = 2f / {S}f;\n\n')
    f.write('\tpublic float u(float local) { int i = ordinal() % GRID; return (i + INSET * GRID + clamp(local) * (1 - 2 * INSET * GRID)) / GRID; }\n')
    f.write('\tpublic float v(float local) { int j = ordinal() / GRID; return (j + INSET * GRID + clamp(local) * (1 - 2 * INSET * GRID)) / GRID; }\n')
    f.write('\tprivate static float clamp(float x) { return x < 0f || x > 1f ? x - (float) Math.floor(x) : x; }\n}\n')
print('atlas ok', len(TILES))
