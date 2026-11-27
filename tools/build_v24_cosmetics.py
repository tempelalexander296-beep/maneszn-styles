"""Maneszn Styles v24 – procedural rebuild of auras, hip swords and wings.

Every rebuilt cosmetic gets new multi-part 3D geometry and a freshly painted HD texture
(painted on a logical grid, then lit/shaded by remaster_cosmetics.render at 16 px per texel).
Rebuilt entries are marked "gen": "v24" so remaster_cosmetics.py leaves them alone.

Run from the repo root:  python3 tools/build_v24_cosmetics.py mc-1.21.11/src/client/resources/assets/maneszn_styles
"""
import json, math, os, random, sys
from PIL import Image
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import remaster_cosmetics as R

def rgb(h):
    h = h.lstrip('#'); return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)

# ----------------------------------------------------------------------------- model + texture builder
class Model:
    def __init__(self):
        self.parts = []; self.colors = []  # colors[(part_idx, cube_idx)] parallel list
    def part(self, attach, pivot, rot, anim):
        p = {'attach': attach, 'pivot': [round(v, 4) for v in pivot], 'rot': [round(v, 4) for v in rot], 'anim': anim, 'cubes': []}
        self.parts.append(p); return p
    def cube(self, p, frm, size, color, inflate=0.0):
        p['cubes'].append({'from': [round(v, 4) for v in frm], 'size': [round(max(.05, v), 4) for v in size], 'inflate': inflate, 'uv': None})
        self.colors.append((p, len(p['cubes']) - 1, rgb(color) if isinstance(color, str) else color))
    def bake(self, assets, tex_rel, glow, seed):
        TW = 64
        while True:
            slots = {}; x = y = rowh = 0; ok = True
            for p, ci, col in self.colors:
                c = p['cubes'][ci]; w, h, d = (math.ceil(s) for s in c['size'])
                key = (w, h, d, col)
                if key not in slots:
                    rw, rh = 2 * (w + d), d + h
                    if rw > TW: ok = False; break
                    if x + rw > TW: x = 0; y += rowh; rowh = 0
                    slots[key] = (x, y, rw, rh); x += rw; rowh = max(rowh, rh)
                c['uv'] = [slots[key][0], slots[key][1]]
            if ok: break
            TW *= 2
        TH = 16
        while TH < y + rowh: TH *= 2
        img = Image.new('RGBA', (TW, TH), (0, 0, 0, 0)); px = img.load()
        for (w, h, d, col), (sx, sy, rw, rh) in slots.items():
            for yy in range(sy, sy + rh):
                for xx in range(sx, sx + rw):
                    if yy < sy + d and (xx < sx + d or xx >= sx + d + 2 * w): continue  # unused box-UV corners
                    px[xx, yy] = col
        fmap = R.face_map({'parts': self.parts}, TW, TH)
        hd = R.render(img, TW, TH, fmap, glow, random.Random(seed), False, True)
        path = os.path.join(assets, tex_rel + '.png'); os.makedirs(os.path.dirname(path), exist_ok=True)
        hd.save(path, optimize=True)
        return [TW, TH]

def place(model, part, obj, offset, scale=1.0, pal=None):
    ox, oy, oz = offset
    for frm, size, role in obj:
        model.cube(part, (ox + frm[0] * scale, oy + frm[1] * scale, oz + frm[2] * scale), tuple(s * scale for s in size), pal[role])

# ----------------------------------------------------------------------------- 3D object library (px units, y down, facing -z)
def C(x, y, z, w, h, d, role): return ((x, y, z), (w, h, d), role)
OBJ = {
    'orb':      [C(-1, -1, -1, 2, 2, 2, 'a'), C(-.6, -1.5, -.6, 1.2, .5, 1.2, 'a'), C(-.6, 1, -.6, 1.2, .5, 1.2, 'b'), C(-1.5, -.6, -.6, .5, 1.2, 1.2, 'a'),
                 C(1, -.6, -.6, .5, 1.2, 1.2, 'b'), C(-.6, -.6, -1.5, 1.2, 1.2, .5, 'a'), C(-.7, -1.1, -1.35, .5, .5, .3, 'w')],
    'crystal':  [C(-.35, -3, -.35, .7, .8, .7, 'w'), C(-.7, -2.2, -.7, 1.4, 1, 1.4, 'a'), C(-1, -1.2, -1, 2, 2.4, 2, 'a'), C(-.7, 1.2, -.7, 1.4, 1, 1.4, 'b'),
                 C(-.35, 2.2, -.35, .7, .8, .7, 'b'), C(-.3, -.8, -1.15, .6, 1.2, .2, 'w')],
    'star':     [C(-.4, -2.6, -.3, .8, 5.2, .6, 'a'), C(-2.6, -.4, -.3, 5.2, .8, .6, 'a'), C(-.9, -.9, -.5, 1.8, 1.8, 1, 'w'),
                 C(-1.6, -1.6, -.25, .7, .7, .5, 'a'), C(.9, -1.6, -.25, .7, .7, .5, 'a'), C(-1.6, .9, -.25, .7, .7, .5, 'a'), C(.9, .9, -.25, .7, .7, .5, 'a')],
    'spark':    [C(-.25, -1, -.15, .5, 2, .3, 'w'), C(-1, -.25, -.15, 2, .5, .3, 'a')],
    'heart':    [C(-2, -2, -.5, 1.6, 1, 1, 'a'), C(.4, -2, -.5, 1.6, 1, 1, 'a'), C(-2.2, -1, -.5, 4.4, 1, 1, 'a'), C(-1.7, 0, -.5, 3.4, 1, 1, 'a'),
                 C(-1.1, 1, -.5, 2.2, .9, 1, 'b'), C(-.5, 1.9, -.5, 1, .7, 1, 'b'), C(-1.6, -1.8, -.65, .6, .6, .2, 'w')],
    'flame':    [C(-1.2, 0, -1.2, 2.4, 1.2, 2.4, 'b'), C(-.9, -1.4, -.9, 1.8, 1.4, 1.8, 'a'), C(-.5, -2.6, -.5, 1, 1.2, 1, 'c'), C(-.25, -3.3, -.25, .5, .7, .5, 'w')],
    'snowflake':[C(-.25, -2.8, -.25, .5, 5.6, .5, 'a'), C(-2.8, -.25, -.25, 5.6, .5, .5, 'a'), C(-.5, -.5, -.4, 1, 1, .8, 'w')]
                 + [C(sx * k * .8 - .25, sy * k * .8 - .25, -.25, .5, .5, .5, 'a') for k in (1, 2, 3) for sx in (-1, 1) for sy in (-1, 1)],
    'rune':     [C(-1.6, -1.6, -.2, 3.2, 3.2, .4, 'b'), C(-.25, -1.2, -.45, .5, 2.4, .3, 'a'), C(.25, -1.2, -.45, .8, .5, .3, 'a'), C(-1.05, .45, -.45, .8, .5, .3, 'a'),
                 C(-1.7, -1.7, -.25, 3.4, .3, .5, 'c'), C(-1.7, 1.4, -.25, 3.4, .3, .5, 'c')],
    'leaf':     [C(-1, -1.8, -.2, 2, 3, .4, 'a'), C(-.5, -2.6, -.2, 1, .8, .4, 'a'), C(-.6, 1.2, -.2, 1.2, .5, .4, 'a'), C(-.15, -2.4, -.3, .3, 3.6, .2, 'b'), C(-.15, 1.7, -.15, .3, 1, .3, 'b')],
    'petal':    [C(-.8, -1.2, -.2, 1.6, 2.4, .4, 'a'), C(-.5, -1.8, -.2, 1, .6, .4, 'a'), C(-.4, 1.2, -.2, .8, .5, .4, 'b'), C(-.3, -.9, -.3, .5, .5, .2, 'w')],
    'sword':    [C(-.3, -3.5, -.15, .6, 4.5, .3, 'm'), C(-.15, -4, -.15, .3, .5, .3, 'w'), C(-1.2, 1, -.3, 2.4, .5, .6, 'c'), C(-.25, 1.5, -.25, .5, 1.5, .5, 'b'), C(-.4, 3, -.4, .8, .8, .8, 'c')],
    'coin':     [C(-1.5, -1, -.25, 3, 2, .5, 'a'), C(-1, -1.5, -.25, 2, 3, .5, 'a'), C(-.6, -.6, -.4, 1.2, 1.2, .2, 'b'), C(-1.1, -1.1, -.35, .4, .4, .1, 'w')],
    'gem':      [C(-.5, -1.5, -.5, 1, .6, 1, 'w'), C(-1, -.9, -1, 2, .9, 2, 'a'), C(-.6, 0, -.6, 1.2, .8, 1.2, 'a'), C(-.25, .8, -.25, .5, .6, .5, 'b')],
    'moon':     [C(-1.6, -1.4, -.4, 1, 2.8, .8, 'a'), C(-1, -2.2, -.4, 1.4, .8, .8, 'a'), C(.3, -2.4, -.4, .8, .6, .8, 'a'), C(-1, 1.4, -.4, 1.4, .8, .8, 'a'), C(.3, 1.8, -.4, .8, .6, .8, 'b')],
    'bolt':     [C(.2, -2.8, -.3, .8, 1.4, .6, 'w'), C(-.6, -1.6, -.3, 1.8, .6, .6, 'a'), C(-.6, -1, -.3, .8, 1.4, .6, 'a'), C(-1.2, .3, -.3, 1.6, .6, .6, 'a'), C(-1.2, .9, -.3, .7, 1.2, .6, 'b')],
    'note':     [C(-1.2, .6, -.4, 1.6, 1.2, .8, 'a'), C(.1, -2.2, -.2, .4, 3, .4, 'b'), C(.5, -2.2, -.2, 1, .5, .4, 'a'), C(.5, -1.6, -.2, .7, .4, .4, 'a')],
    'gift':     [C(-1, -1, -1, 2, 2, 2, 'a'), C(-.25, -1.05, -1.05, .5, 2.1, 2.1, 'c'), C(-1.05, -1.05, -.25, 2.1, 2.1, .5, 'c'),
                 C(-.8, -1.6, -.3, .6, .55, .6, 'c'), C(.2, -1.6, -.3, .6, .55, .6, 'c')],
    'shuriken': [C(-.5, -.5, -.2, 1, 1, .4, 'b'), C(-.25, -2.5, -.15, .5, 2, .3, 'm'), C(-.25, .5, -.15, .5, 2, .3, 'm'), C(-2.5, -.25, -.15, 2, .5, .3, 'm'), C(.5, -.25, -.15, 2, .5, .3, 'm')],
    'comet':    [C(-.8, -.8, -.8, 1.6, 1.6, 1.6, 'w'), C(.8, -.5, -.5, 1.2, 1, 1, 'a'), C(2, -.35, -.35, 1.2, .7, .7, 'b'), C(3.2, -.2, -.2, 1, .4, .4, 'c')],
    'flag':     [C(-.15, -2.5, -.15, .3, 5, .3, 'm'), C(.15, -2.5, -.1, 2.4, 1.6, .2, 'a'), C(.15, -1.7, -.12, 2.4, .5, .24, 'b')],
    'puff':     [C(-1, -1, -1, 2, 2, 2, 'a'), C(-1.6, -.4, -.6, 1, 1.2, 1.2, 'b'), C(.6, -.6, -.6, 1, 1.2, 1.2, 'b'), C(-.5, -1.6, -.5, 1, .8, 1, 'a')],
    'planet':   [C(-1.5, -1.5, -1.5, 3, 3, 3, 'a'), C(-2.6, -.15, -2.6, 5.2, .3, 5.2, 'c'), C(-.8, -1.2, -1.6, .8, .6, .2, 'w')],
    'skull':    [C(-1.2, -1.4, -1, 2.4, 2, 2, 'a'), C(-.8, .6, -.8, 1.6, .7, 1.6, 'a'), C(-.8, -.8, -1.1, .6, .6, .2, 'b'), C(.2, -.8, -1.1, .6, .6, .2, 'b')],
}

def P(a, b, c, w='#ffffff', m='#d8dde3'): return {'a': a, 'b': b, 'c': c, 'w': w, 'm': m}
# theme -> (main object, mid object, small object, palette)
THEMES = {
    'purple':  ('crystal', 'orb', 'spark', P('#b06cff', '#6a2fd0', '#e8d3ff')),
    'gold':    ('coin', 'star', 'spark', P('#ffd23f', '#c98a14', '#fff1b0', '#fffbe6')),
    'double':  ('orb', 'star', 'spark', P('#7fd4ff', '#b06cff', '#ffffff')),
    'crystal': ('crystal', 'gem', 'spark', P('#7fe0ff', '#2a8fe0', '#d8f6ff')),
    'emerald': ('crystal', 'gem', 'orb', P('#3ee07a', '#138a45', '#c8ffd8')),
    'heart':   ('heart', 'petal', 'spark', P('#ff4d7d', '#c41745', '#ffd0dc')),
    'star':    ('star', 'moon', 'spark', P('#ffe066', '#7a5cff', '#fff6c8')),
    'rune':    ('rune', 'crystal', 'spark', P('#7ee8ff', '#1c1c34', '#b06cff')),
    'fire':    ('flame', 'orb', 'spark', P('#ff9a1f', '#e0301c', '#ffe066', '#fff4c8')),
    'soul':    ('flame', 'skull', 'spark', P('#4fe6ff', '#1b6f8a', '#c8fbff', '#e8ffff')),
    'sword':   ('sword', 'star', 'gem', P('#ffd36b', '#4a2e18', '#e8c050', m='#e6ebf2')),
    'planet':  ('planet', 'moon', 'star', P('#5a7dff', '#2c3a9a', '#ffd36b')),
    'shadow':  ('puff', 'skull', 'spark', P('#3a3248', '#1a1522', '#b06cff', '#d8c8ff')),
    'wasp':    ('puff', 'bolt', 'spark', P('#ffd23f', '#1c1a14', '#fff1b0')),
    'snow':    ('snowflake', 'crystal', 'orb', P('#e8f6ff', '#7fc4ff', '#ffffff')),
    'note':    ('note', 'star', 'spark', P('#ff7ad9', '#6a2fd0', '#ffe066')),
    'magic':   ('rune', 'star', 'spark', P('#c77dff', '#3a1d6e', '#7fe0ff')),
    'dust':    ('star', 'orb', 'spark', P('#d9a8ff', '#8a4fe0', '#ffffff')),
    'rainbow': ('star', 'orb', 'spark', P('#ff5a6e', '#3ec7ff', '#ffe066')),
    'totem':   ('gem', 'star', 'orb', P('#ffd23f', '#3ec46b', '#fff1b0')),
    'sparks':  ('bolt', 'star', 'spark', P('#ffe066', '#ff9a1f', '#ffffff')),
    'glow':    ('orb', 'star', 'spark', P('#fff1a8', '#ffb52e', '#ffffff')),
    'poison':  ('orb', 'skull', 'spark', P('#7cff4a', '#2f8a1a', '#d8ffc8')),
    'blood':   ('moon', 'orb', 'crystal', P('#e0243c', '#6e0a18', '#ff8a9a')),
    'admin':   ('rune', 'bolt', 'crystal', P('#ff4d5e', '#2a0a10', '#ffd0d4')),
    'comet':   ('comet', 'star', 'spark', P('#ffe066', '#ff7a3d', '#7fd4ff')),
    'diamond': ('gem', 'crystal', 'star', P('#7ff7ff', '#2bb8d0', '#c77dff')),
    'leaf':    ('leaf', 'petal', 'spark', P('#5fd04a', '#2f7a1f', '#ffd6e8')),
    'pink':    ('heart', 'orb', 'spark', P('#ff8ad8', '#c43a9a', '#ffe0f4')),
    'ice':     ('crystal', 'snowflake', 'orb', P('#bfe9ff', '#5fb0e8', '#ffffff')),
    'gift':    ('gift', 'star', 'orb', P('#e0243c', '#1f8a45', '#ffd36b')),
    'shuriken':('shuriken', 'sword', 'spark', P('#5a5f6a', '#1a1c22', '#e0243c', m='#d8dde3')),
    'flag':    ('flag', 'star', 'coin', P('#e0243c', '#ffffff', '#ffd36b', m='#8a6a3a')),
    'owner':   ('crystal', 'star', 'gem', P('#c77dff', '#6a2fd0', '#ffd36b')),
    'vip':     ('coin', 'gem', 'star', P('#ffd23f', '#b8860b', '#fff1b0')),
}
AURA_THEME = {
    'orbit_lila': 'purple', 'orbit_gold': 'gold', 'orbit_doppelt': 'double', 'kristall_orbit': 'crystal', 'smaragd_orbit': 'emerald', 'herz_orbit': 'heart',
    'sternen_orbit': 'star', 'runenkreis': 'rune', 'feuerkreis': 'fire', 'schwert_orbit': 'sword', 'planetenring': 'planet', 'seelen_orbit': 'soul',
    'spirale_lila': 'purple', 'spirale_regenbogen': 'rainbow', 'spirale_gold': 'gold', 'spirale_frost': 'ice', 'spirale_seelen': 'soul', 'owner_spirale': 'owner',
    'schattenrauch': 'shadow', 'wespen_rauch': 'wasp', 'ht4_orbit': 'double', 'herzen': 'heart', 'flammen': 'fire', 'seelenfeuer': 'soul', 'endfunken': 'star',
    'schnee': 'snow', 'noten': 'note', 'verzauberung': 'magic', 'lila_staub': 'dust', 'regenbogen': 'rainbow', 'totem': 'totem', 'funken': 'sparks', 'gluehen': 'glow',
    'amethyst_glitzer': 'crystal', 'gift': 'poison', 'goldstaub': 'gold', 'blutwirbel': 'blood', 'vip_glanz': 'vip', 'admin_aura': 'admin', 'fuchsfeuer': 'fire',
    'vip_goldorbit': 'vip', 'legend_kometenring': 'comet', 'admin_bannkreis': 'admin', 'owner_diamantkranz': 'diamond', 'blaetterwirbel': 'leaf', 'pinker_orbit': 'pink',
    'eis_orbit': 'ice', 'stern_doppel': 'star', 'blutmond': 'blood', 'geschenke_orbit': 'gift', 'shuriken_orbit': 'shuriken', 'goldmuenzen': 'gold', 'crew_flaggen': 'flag',
}
AURA_FIX = {'amethyst_glitzer': P('#c77dff', '#6a2fd0', '#f0d8ff'), 'fuchsfeuer': P('#ff7a1f', '#c43a10', '#ffe0a0'), 'endfunken': P('#e8d8ff', '#6a2fd0', '#ffffff')}

def build_aura(c, assets):
    th = AURA_THEME.get(c['id'], 'purple'); A, B, Cc, pal = THEMES[th]; pal = AURA_FIX.get(c['id'], pal)
    m = Model(); spiral = c['id'].startswith('spirale') or c['id'].endswith('spirale')
    rnd = random.Random(sum(map(ord, c['id'])))
    for i in range(5):  # main objects: big, bobbing orbit around the chest
        ang = i / 5 * math.tau
        p = m.part('buddy', (0, 9, 0), (0, ang, 0), 'orbit'); place(m, p, OBJ[A], (0, 0, -11.5), 1.0, pal)
    for i in range(7):  # secondary objects: counter-rotating ring (helix for spirals)
        ang = i / 7 * math.tau + .4
        y = 1 + i * 3.2 if spiral else 3
        p = m.part('buddy', (0, y, 0), (.1, ang, 0), 'spin_rev'); place(m, p, OBJ[B], (0, 0, -9), .75, pal)
    for i in range(10):  # small objects: fast ring around the legs
        ang = i / 10 * math.tau + rnd.random() * .2
        p = m.part('buddy', (0, 19 + (i % 2) * 2.5, 0), (0, ang, 0), 'spin_fast'); place(m, p, OBJ[Cc], (0, 0, -13), .6, pal)
    c['tex_size'] = m.bake(assets, c.get('texture') or f'textures/cosmetics/aura/{c["id"]}', True, sum(map(ord, c['id'])))
    c['texture'] = c.get('texture') or f'textures/cosmetics/aura/{c["id"]}'
    c['parts'] = m.parts; c['glow'] = True; c['translucent'] = True; c['gen'] = 'v24'

# ----------------------------------------------------------------------------- hip swords
SWORD = {  # m blade, m2 fuller, c guard/pommel, b grip, g gem, length, glow
    'eisenschwert': ('#dfe4ea', '#a8b0ba', '#9aa1ab', '#4a3224', '#c8102e', 8, False),
    'diamantschwert': ('#7ff7ee', '#2bb8b0', '#e6f2ff', '#24384a', '#3fe8ff', 8, False),
    'netheritschwert': ('#55494f', '#2e272c', '#7a6872', '#1b1517', '#b06cff', 8, False),
    'goldschwert': ('#ffd84a', '#d19b18', '#fff1b0', '#6b3d1e', '#31c4ff', 8, False),
    'holzschwert': ('#a8743f', '#6e4826', '#7d5634', '#4a2e18', '#5fbf4a', 7, False),
    'lila_klinge': ('#c08bff', '#7a2fe0', '#e8d3ff', '#2a1840', '#ff7ad9', 8.5, True),
    'katana_huefte': ('#eef1f5', '#b0b8c4', '#2b2b30', '#f2f2f2', '#c8102e', 10, False),
    'dolch': ('#d0d6de', '#9aa2ad', '#b0843c', '#3a2618', '#ffd36b', 5, False),
    'rubinschwert': ('#e8304a', '#9a0f24', '#ffd36b', '#3a1a14', '#ff8a9a', 8, False),
    'owner_klinge': ('#c77dff', '#7b2cbf', '#ffd36b', '#1a0f2a', '#ffffff', 9, True),
}
def build_sword(c, assets):
    blade, fuller, guard, grip, gem, L, glow = SWORD[c['id']]
    katana = 'katana' in c['id']
    m = Model()
    belt = m.part('body', (0, 0, 0), (0, 0, 0), 'none')
    m.cube(belt, (-4, 10, -2), (8, 1, 4), '#3b2a20', .3); m.cube(belt, (-1, 9.8, -2.5), (2, 1, 1), '#c9a45a', .1)
    m.cube(belt, (3.6, 10.6, -2.9), (2, 2.2, 1.2), '#4a3424', .05)  # frog / holder
    s = m.part('body', (4.7, 11.2, -2.4), (0.42, 0, -0.1), 'none')
    bw = 1.1 if katana else 1.7
    # pommel + gem
    m.cube(s, (-.6, -5.4, -.6), (1.2, 1.2, 1.2), guard); m.cube(s, (-.35, -5.15, -.72), (.7, .7, .25), gem)
    # grip with wraps
    m.cube(s, (-.45, -4.2, -.45), (.9, 3.2, .9), grip)
    for yy in (-3.9, -3.1, -2.3, -1.5): m.cube(s, (-.55, yy, -.55), (1.1, .35, 1.1), guard if not katana else '#1b1b20')
    # guard
    if katana:
        m.cube(s, (-1.1, -1, -1.1), (2.2, .5, 2.2), guard)
    else:
        m.cube(s, (-2, -1, -.7), (4, .8, 1.4), guard); m.cube(s, (-2.4, -1.3, -.8), (.6, 1.4, 1.6), guard); m.cube(s, (1.8, -1.3, -.8), (.6, 1.4, 1.6), guard)
        m.cube(s, (-.35, -.95, -.85), (.7, .7, .3), gem)
    # blade: body, fuller, tapered tip
    m.cube(s, (-bw / 2, -.2, -.25), (bw, L, .5), blade)
    m.cube(s, (-.2, .3, -.32), (.4, L - 2, .64), fuller)
    m.cube(s, (-bw / 2 + .25, L - .2, -.22), (bw - .5, 1, .44), blade)
    m.cube(s, (-.25, L + .8, -.18), (.5, .8, .36), blade)
    c['tex_size'] = m.bake(assets, c['texture'], glow, sum(map(ord, c['id'])))
    c['parts'] = m.parts; c['glow'] = glow; c['translucent'] = False; c['gen'] = 'v24'

# ----------------------------------------------------------------------------- wings
WINGS = {  # style, primary, secondary, accent, glow, translucent
    'engel': ('feather', '#ffffff', '#e6ecf5', '#ffe9a8', False, False),
    'drache': ('membrane', '#b3162a', '#3a1a14', '#ffd36b', False, False),
    'fee': ('insect', '#ffb8f0', '#b8f0ff', '#ffffff', True, True),
    'kristall': ('crystal', '#7fe0ff', '#2a8fe0', '#ffffff', True, True),
    'amethyst_schmetterling': ('insect', '#c77dff', '#6a2fd0', '#ffe066', True, True),
    'schmetterling_blau': ('insect', '#3ec7ff', '#1a3a8a', '#1a1a20', False, True),
    'fledermaus': ('membrane', '#3a2a40', '#1a141e', '#9a7aa8', False, False),
    'phoenix': ('feather', '#ff7a1f', '#e0301c', '#ffe066', True, False),
    'eis': ('crystal', '#d8f6ff', '#7fc4ff', '#ffffff', True, True),
    'giftsmaragd': ('crystal', '#3ee07a', '#138a45', '#c8ffd8', True, True),
    'goldrubin': ('crystal', '#ffd23f', '#e0243c', '#fff1b0', True, False),
    'seelen': ('feather', '#4fe6ff', '#1b6f8a', '#e8ffff', True, True),
    'galaxie': ('feather', '#2a1a6e', '#0c0a24', '#c8b8ff', True, False),
    'cyber': ('membrane', '#2be0ff', '#1a1c22', '#ff3ad0', True, False),
    'frostdrache': ('membrane', '#9fd8ff', '#3a6a9a', '#ffffff', False, False),
    'owner_fluegel': ('feather', '#c77dff', '#6a2fd0', '#ffd36b', True, False),
    'vip_goldfedern': ('feather', '#ffd23f', '#c98a14', '#fff6d0', True, False),
    'legend_glutschwingen': ('feather', '#ff5a1f', '#3a0e08', '#ffd36b', True, False),
    'admin_obsidianschwingen': ('membrane', '#2a1a3a', '#0c0810', '#ff4d5e', True, False),
    'owner_diamantschwingen': ('crystal', '#7ff7ff', '#c77dff', '#ffffff', True, True),
    'blattfluegel': ('feather', '#5fd04a', '#2f7a1f', '#c8ff9a', False, False),
    'libelle': ('dragonfly', '#bfefff', '#4a8aa8', '#ffffff', False, True),
}
def mix(a, b, t):
    A, B = rgb(a), rgb(b); return tuple(int(A[i] + (B[i] - A[i]) * t) for i in range(3)) + (255,)

def build_wings(c, assets):
    style, a, b, acc, glow, trans = WINGS[c['id']]
    m = Model()
    for side in (1, -1):
        anim = 'flap_left' if side > 0 else 'flap_right'
        base = (0.05, -0.55 * side, -0.22 * side)
        piv = (1.5 * side, 1.5, 2.6)
        def blade(angle, length, width, thick, color, tip=None, z=0.0, start=1.5):
            p = m.part('body', piv, (base[0], base[1], base[2] + angle * side), anim)
            x0 = start if side > 0 else -start - length
            m.cube(p, (x0, -width / 2, z), (length, width, thick), color)
            if tip:
                tl = min(1.6, length * .2)
                tx = x0 + length if side > 0 else x0 - tl
                m.cube(p, (tx, -width * .3, z), (tl, width * .6, thick), tip)
            return p
        if style == 'feather':
            # arm bone
            blade(-.15, 12, 1.3, 1, b)
            for i in range(8):  # primaries (long, back layer)
                t = i / 7; blade(-.3 + t * 1.35, 13.5 - t * 4.5, 1.25, .35, mix(a, b, .15 + t * .25), acc, z=.55)
            for i in range(7):  # secondaries
                t = i / 6; blade(-.1 + t * 1.15, 9 - t * 2.5, 1.2, .35, mix(a, acc, .15 * t), None, z=.1)
            for i in range(6):  # coverts (front, short)
                t = i / 5; blade(.05 + t * .9, 4.6 - t * 1.2, 1.15, .35, mix(a, '#ffffff', .25), None, z=-.3)
        elif style in ('membrane',):
            bones = [(0.0, 13.5), (.38, 12.5), (.8, 11), (1.2, 8.5)]
            for ang, ln in bones:
                blade(ang, ln, .7, .7, b, acc, z=-.1)
            for k in range(len(bones) - 1):  # membrane panels between bones
                for j in range(3):
                    t = (j + 1) / 4; ang = bones[k][0] + (bones[k + 1][0] - bones[k][0]) * t
                    ln = bones[k][1] + (bones[k + 1][1] - bones[k][1]) * t - 1.2
                    blade(ang, ln, 2.2, .25, mix(a, b, .1 + j * .08), None, z=.25)
        elif style == 'crystal':
            for i in range(9):
                t = i / 8; ln = 12.5 - abs(t - .3) * 7; col = a if i % 2 == 0 else b
                p = blade(-.1 + t * 1.3, ln, 1.4, 1.0, col, acc, z=.1 * (i % 3))
            for i in range(5):
                t = i / 4; blade(.2 + t * 1.0, 4 + i * .4, .9, .8, acc, None, z=-.4)
        elif style == 'insect':
            for k, (ang, ln, w) in enumerate([(-.45, 10, 5), (-.25, 11, 5.5), (.55, 7.5, 4), (.8, 6.5, 3.4)]):
                p = blade(ang, ln, w, .25, a if k < 2 else mix(a, b, .35), None, z=.2)
                x0 = 1.5 + ln * .55 if side > 0 else -1.5 - ln * .55 - 1.4
                m.cube(p, (x0, -w * .18, .05), (1.4, w * .36, .3), acc)   # eye spot
                xe = 1.5 + ln - 1 if side > 0 else -1.5 - ln
                m.cube(p, (xe, -w / 2, .15), (1, w, .35), b)            # dark rim
        elif style == 'dragonfly':
            for ang in (-.2, .05, .45, .7):
                p = blade(ang, 12, 1.8, .2, a, None, z=.2)
                xv = 1.5 if side > 0 else -13.5
                m.cube(p, (xv, -.15, .1), (12, .3, .3), b)
        c['_tmp'] = True
    c.pop('_tmp', None)
    c['tex_size'] = m.bake(assets, c['texture'], glow, sum(map(ord, c['id'])))
    c['parts'] = m.parts; c['glow'] = glow; c['translucent'] = trans; c['gen'] = 'v24'

# ----------------------------------------------------------------------------- main
def main():
    assets = sys.argv[1]
    path = os.path.join(assets, 'builtin/cosmetics.json')
    data = json.load(open(path))
    n = {'aura': 0, 'hip': 0, 'wings': 0}
    for c in data['cosmetics']:
        if c['type'] == 'aura': build_aura(c, assets); n['aura'] += 1
        elif c['type'] == 'hip' and c['id'] in SWORD: build_sword(c, assets); n['hip'] += 1
        elif c['type'] == 'wings' and c['id'] in WINGS: build_wings(c, assets); n['wings'] += 1
    data['_info'] = 'v24: Auren, Hüft-Schwerter und Flügel werden von tools/build_v24_cosmetics.py erzeugt (gen=v24).'
    json.dump(data, open(path, 'w'), ensure_ascii=False, separators=(',', ':'))
    print('rebuilt', n)

if __name__ == '__main__':
    main()
