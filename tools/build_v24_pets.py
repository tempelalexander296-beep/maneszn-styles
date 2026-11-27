"""Maneszn Styles v24 – procedural rebuild of all pets (multi-part 3D, HD textures).
Coordinates: px, y up = negative, front = -z, pet stands on y = 0 (the shoulder).
Run from the repo root:  python3 tools/build_v24_pets.py mc-1.21.11/src/client/resources/assets/maneszn_styles
"""
import json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from build_v24_cosmetics import Model

BLACK, WHITE = '#141418', '#ffffff'

def eyes(m, p, y, z, dx=1.1, s=.9, col=BLACK, ring=None):
    for sx in (-1, 1):
        x = sx * dx - s / 2
        if ring: m.cube(p, (x - .25, y - .25, z + .05), (s + .5, s + .5, .2), ring)
        m.cube(p, (x, y, z), (s, s, .2), col)
        m.cube(p, (x + s * .15, y + s * .1, z - .05), (s * .4, s * .4, .1), WHITE)

def mirror(m, p, frm, size, col):
    m.cube(p, frm, size, col); m.cube(p, (-frm[0] - size[0], frm[1], frm[2]), size, col)

# ----------------------------------------------------------------------------- archetypes
def quadruped(m, P):
    A, B, N = P['a'], P['b'], P.get('nose', '#2a1a1a')
    body = m.part('buddy', (0, 0, 0), (0, 0, 0), 'breathe')
    m.cube(body, (-2, -4.5, -1.5), (4, 4.5, 3.6), A)                       # torso (sitting)
    m.cube(body, (-1.4, -4, -1.75), (2.8, 3.6, .4), B)                     # belly / chest
    mirror(m, body, (-2.4, -2.2, -.2), (1.1, 2.2, 2.4), A)                 # haunches
    mirror(m, body, (-1.7, -1.3, -2.4), (1.2, 1.3, 1.3), A)                # front paws
    mirror(m, body, (-1.7, -.5, -2.6), (1.2, .5, .4), B)                   # toe tips
    if P.get('wool'):
        for x, y, z in ((-2.3, -4.8, -1), (.9, -4.8, -1), (-2.3, -3, .6), (.9, -3, .6), (-.8, -5, .4), (-.8, -2.6, 1.6)):
            m.cube(body, (x, y, z), (1.6, 1.6, 1.6), A)
    if P.get('spikes'):
        for i, (x, y) in enumerate(((-1.6, -4.8), (-.4, -5), (.8, -4.8), (-1, -3.6), (.3, -3.6), (-1.6, -2.4), (.8, -2.4))):
            m.cube(body, (x, y, 1.5 + (i % 2) * .4), (.6, .6, 1.4), P.get('spike', '#3a2a1e'))
    head = m.part('buddy', (0, -4.5, -.3), (0, 0, 0), 'look')
    hw = P.get('head_w', 4.4)
    m.cube(head, (-hw / 2, -4, -2.2), (hw, 4, 3.6), P.get('head', A))
    if P.get('mask'): mirror(m, head, (-1.9, -3.1, -2.3), (1.6, 1.4, .15), P['mask'])
    sn = P.get('snout', 'short')
    if sn == 'long':   m.cube(head, (-1, -2, -3.6), (2, 1.6, 1.6), B); m.cube(head, (-.45, -2, -3.8), (.9, .6, .3), N)
    elif sn == 'flat': m.cube(head, (-1.5, -2.1, -2.9), (3, 1.8, .8), B); m.cube(head, (-.5, -2.1, -3.0), (1, .5, .2), N)
    else:              m.cube(head, (-1.2, -1.9, -2.8), (2.4, 1.5, .7), B); m.cube(head, (-.4, -1.9, -2.95), (.8, .55, .2), N)
    eyes(m, head, -3.1, -2.35, s=P.get('eye', .9))
    ears = P.get('ears', 'pointy'); EI = P.get('inner', '#ffb6c1')
    if ears == 'pointy':
        mirror(m, head, (-2.2, -5.5, -1), (1.3, 1.5, .8), P.get('ear', A)); mirror(m, head, (-1.95, -5.1, -1.1), (.75, 1, .2), EI)
    elif ears == 'round':
        mirror(m, head, (-2.6, -5.1, -.8), (1.6, 1.4, .8), P.get('ear', A)); mirror(m, head, (-2.3, -4.8, -.9), (1, .9, .2), EI)
    elif ears == 'big_round':
        mirror(m, head, (-3.4, -5, -.6), (2.2, 2, .8), P.get('ear', A)); mirror(m, head, (-3, -4.6, -.7), (1.4, 1.3, .2), EI)
    elif ears == 'long':
        mirror(m, head, (-1.8, -8.5, -.4), (1.1, 4.6, .8), P.get('ear', A)); mirror(m, head, (-1.6, -8.1, -.5), (.6, 3.8, .2), EI)
    elif ears == 'floppy':
        mirror(m, head, (-3, -3.8, -.6), (.9, 2.6, 1.6), P.get('ear', A))
    elif ears == 'small':
        mirror(m, head, (-2, -4.7, -.4), (.9, .8, .7), P.get('ear', A))
    tail = P.get('tail', 'thin')
    if tail:
        t = m.part('buddy', (0, -1.5, 1.8), (-.9 if tail != 'short' else 0, 0, 0), P.get('wag', 'wag'))
        if tail == 'thin':
            m.cube(t, (-.35, -.35, 0), (.7, .7, 3.6), A); m.cube(t, (-.4, -.4, 3.3), (.8, .8, .8), P.get('tip', B))
        elif tail == 'bushy':
            m.cube(t, (-.9, -.9, 0), (1.8, 1.8, 3), A); m.cube(t, (-.75, -.75, 3), (1.5, 1.5, 1.3), P.get('tip', B))
        elif tail == 'huge':
            m.cube(t, (-1.1, -1.1, 0), (2.2, 2.2, 2.4), A); m.cube(t, (-1.3, -4.6, 2), (2.6, 4.6, 2.2), A); m.cube(t, (-1, -5.6, 2.3), (2, 1.2, 1.6), P.get('tip', B))
        elif tail == 'ringed':
            for k in range(4): m.cube(t, (-.85, -.85, k * .95), (1.7, 1.7, .95), A if k % 2 == 0 else P.get('ring', '#3a2a1e'))
        elif tail == 'short':
            m.cube(t, (-.75, -.75, -.2), (1.5, 1.5, 1.1), P.get('tip', B))
        elif tail == 'flat':
            m.cube(t, (-.9, -.3, 0), (1.8, .6, 3.4), A)
    if P.get('horns'):
        mirror(m, head, (-1.7, -5.6, -.2), (.6, 1.8, .6), P['horns']); mirror(m, head, (-1.7, -6.2, .2), (.45, .8, .45), P['horns'])
    if P.get('wings'):
        for sx, an in ((1, 'flap_small_left'), (-1, 'flap_small_right')):
            w = m.part('buddy', (sx * 2, -4, .8), (0, 0, 0), an)
            x0 = 0 if sx > 0 else -3.6
            m.cube(w, (x0, -2.6, 0), (3.6, .4, .4), P['horns'] or B); m.cube(w, (x0, -2.3, .1), (3.4, 2.6, .25), P['wings'])
    if P.get('crown'):
        m.cube(head, (-1.6, -4.9, -1.6), (3.2, .9, 3.2), P['crown'])
        for x in (-1.5, -.3, .9): m.cube(head, (x, -5.6, -1.6), (.6, .7, .6), P['crown'])

def bird(m, P):
    A, B, K = P['a'], P['b'], P.get('beak', '#ff9a1f')
    body = m.part('buddy', (0, 0, 0), (0, 0, 0), 'breathe')
    m.cube(body, (-2.1, -5, -1.8), (4.2, 5, 3.6), A); m.cube(body, (-1.5, -4.4, -2.05), (3, 4, .4), B)
    mirror(m, body, (-1.5, -.4, -2.3), (1.1, .4, 1.4), K)
    m.cube(body, (-1.2, -1.2, 1.7), (2.4, 1, 1.4), A)    # tail feathers
    head = m.part('buddy', (0, -5, 0), (0, 0, 0), 'look')
    m.cube(head, (-1.9, -3.4, -1.8), (3.8, 3.4, 3.4), P.get('head', A))
    if P.get('face'): m.cube(head, (-1.5, -3, -1.95), (3, 2.6, .2), P['face'])
    m.cube(head, (-.55, -1.9, -2.7), (1.1, .8, 1), K)
    eyes(m, head, -2.7, -2.05, dx=.95, s=P.get('eye', .8), ring=P.get('ring'))
    if P.get('tufts'): mirror(m, head, (-1.9, -4.3, -1), (.8, 1, .8), A)
    if P.get('crest'): m.cube(head, (-.4, -4.6, -1), (.8, 1.3, 2), P['crest'])
    for sx, an in ((1, 'flap_small_left'), (-1, 'flap_small_right')):
        w = m.part('buddy', (sx * 2.1, -4.3, 0), (0, 0, 0), an)
        m.cube(w, (0 if sx > 0 else -.6, 0, -1.3), (.6, 3.2, 2.8), P.get('wing', A))
    if P.get('crown'):
        for x in (-1.4, -.3, .8): m.cube(head, (x, -4.2, -.5), (.6, .9, .6), P['crown'])

def insect(m, P):
    body = m.part('buddy', (0, 0, 0), (0, 0, 0), 'hover')
    for k in range(4):
        m.cube(body, (-1.7, -3.4, -.6 + k * .95), (3.4, 3.2, .95), P['a'] if k % 2 == 0 else BLACK)
    m.cube(body, (-.25, -2, 3.2), (.5, .5, .8), BLACK)
    m.cube(body, (-1.4, -3.4, -2.6), (2.8, 2.8, 2), BLACK)
    eyes(m, body, -2.8, -2.7, dx=.8, s=.9, col='#2a2a40')
    mirror(m, body, (-.9, -4.6, -2.4), (.3, 1.3, .3), BLACK)
    for sx, an in ((1, 'buzz_left'), (-1, 'buzz_right')):
        w = m.part('buddy', (sx * .9, -3.5, .2), (0, 0, 0), an)
        m.cube(w, (0 if sx > 0 else -2.8, -.15, -1), (2.8, .2, 2.4), '#dff4ff')

def special(m, kind, P):
    if kind == 'frog':
        b = m.part('buddy', (0, 0, 0), (0, 0, 0), 'breathe')
        m.cube(b, (-2.4, -2.6, -2), (4.8, 2.6, 4), P['a']); m.cube(b, (-1.8, -2, -2.15), (3.6, 1.8, .3), P['b'])
        mirror(m, b, (-2.9, -1, -1), (1.2, 1, 2.6), P['a'])
        h = m.part('buddy', (0, -2.6, -.5), (0, 0, 0), 'look')
        m.cube(h, (-2.2, -2.2, -1.8), (4.4, 2.2, 3.4), P['a'])
        mirror(m, h, (-2.3, -3.2, -1.3), (1.6, 1.4, 1.4), P['a']); eyes(m, h, -2.9, -1.45, dx=1.5, s=.9)
        m.cube(h, (-1.4, -.7, -1.85), (2.8, .3, .2), '#2a1a1a')
    elif kind == 'axolotl':
        b = m.part('buddy', (0, 0, 0), (0, 0, 0), 'breathe')
        m.cube(b, (-1.5, -2.2, -2), (3, 2.2, 5), P['a']); mirror(m, b, (-2.1, -.8, -1.5), (.8, .8, .8), P['a']); mirror(m, b, (-2.1, -.8, 1.5), (.8, .8, .8), P['a'])
        t = m.part('buddy', (0, -1.2, 3), (0, 0, 0), 'wag_slow'); m.cube(t, (-.3, -1.4, 0), (.6, 2.4, 3), P['b'])
        h = m.part('buddy', (0, -1.6, -2), (0, 0, 0), 'look')
        m.cube(h, (-2.1, -1.4, -2.6), (4.2, 2.6, 2.8), P['a']); eyes(m, h, -.6, -2.7, dx=1.3, s=.7)
        for k in range(3): mirror(m, h, (-2.9 - k * .1, -1.8 + k * .9, -1), (1, .4, .4), P['b'])
    elif kind == 'slime':
        b = m.part('buddy', (0, 0, 0), (0, 0, 0), 'bounce')
        m.cube(b, (-1.6, -3.6, -1.6), (3.2, 3.2, 3.2), P['b']); m.cube(b, (-2.6, -5.2, -2.6), (5.2, 5.2, 5.2), P['a'])
        eyes(m, b, -3.6, -2.7, dx=1, s=.9); m.cube(b, (-.6, -2, -2.7), (1.2, .4, .2), BLACK)
    elif kind == 'jelly':
        b = m.part('buddy', (0, 0, 0), (0, 0, 0), 'hover')
        m.cube(b, (-2.4, -8, -2.4), (4.8, 3, 4.8), P['a']); m.cube(b, (-3, -5.4, -3), (6, .8, 6), P['b'])
        for k in range(6):
            x = -2.2 + (k % 3) * 1.8; z = -1.5 + (k // 3) * 2.6
            m.cube(b, (x, -4.6, z), (.4, 3.8 + (k % 2), .4), P['b'])
        eyes(m, b, -6.6, -2.45, dx=1, s=.7)
    elif kind == 'puffer':
        b = m.part('buddy', (0, 0, 0), (0, 0, 0), 'hover')
        m.cube(b, (-2.5, -6, -2.5), (5, 5, 5), P['a']); m.cube(b, (-1.8, -3.5, -2.6), (3.6, 2.3, .3), P['b'])
        for x, y, z in ((-3, -4, -.5), (2.5, -4, -.5), (-.5, -6.6, -.5), (-.5, -4, 2.5), (-2.2, -6.2, -2.2), (1.6, -6.2, 1.6), (-2.6, -2, 1.6), (2, -2, -2.2)):
            m.cube(b, (x, y, z), (.5, .5, .5), P['c'])
        eyes(m, b, -5, -2.65, dx=1.4, s=.9); mirror(m, b, (-3.1, -3.5, -.5), (.6, 1.4, 1.4), P['c'])
    elif kind == 'koi':
        b = m.part('buddy', (0, 0, 0), (0, 0, 0), 'hover')
        m.cube(b, (-1.2, -4, -3), (2.4, 2.6, 6), P['a'])
        for x, y, z in ((-1.25, -4.05, -1.5), (-1.25, -3, .8)): m.cube(b, (x, y, z), (2.5, 1.2, 1.6), P['b'])
        mirror(m, b, (-1.8, -2.4, -1), (.6, .3, 1.6), P['c']); m.cube(b, (-.2, -5, -.5), (.4, 1, 2.4), P['c'])
        t = m.part('buddy', (0, -2.7, 3), (0, 0, 0), 'wag'); m.cube(t, (-.2, -1.7, 0), (.4, 3.4, 2.2), P['c'])
        eyes(m, b, -3.6, -3.05, dx=.7, s=.6)
    elif kind == 'turtle':
        b = m.part('buddy', (0, 0, 0), (0, 0, 0), 'breathe')
        m.cube(b, (-2.6, -3.6, -2.6), (5.2, 2.6, 5.2), P['a']); m.cube(b, (-1.8, -4.3, -1.8), (3.6, .8, 3.6), P['a'])
        for x, z in ((-1.7, -1.7), (.4, -1.7), (-1.7, .4), (.4, .4)): m.cube(b, (x, -4.4, z), (1.3, .2, 1.3), P['c'])
        m.cube(b, (-2.7, -1.2, -2.7), (5.4, .5, 5.4), P['b'])
        for x, z in ((-2.4, -2.4), (1.4, -2.4), (-2.4, 1.4), (1.4, 1.4)): m.cube(b, (x, -.8, z), (1, .8, 1), P['b'])
        h = m.part('buddy', (0, -2, -2.6), (0, 0, 0), 'look'); m.cube(h, (-1, -1.6, -1.8), (2, 1.6, 2), P['b']); eyes(m, h, -1.2, -1.85, dx=.6, s=.5)
    elif kind == 'chameleon':
        b = m.part('buddy', (0, 0, 0), (0, 0, 0), 'breathe')
        m.cube(b, (-1.4, -3.6, -2.4), (2.8, 2.6, 4.8), P['a']); m.cube(b, (-.3, -4.2, -2), (.6, .6, 4), P['b'])
        for x, z in ((-1.7, -2), (1.1, -2), (-1.7, 1.4), (1.1, 1.4)): m.cube(b, (x, -1, z), (.6, 1, .6), P['a'])
        for k, (y, z) in enumerate(((-2.6, 2.4), (-1.8, 3.2), (-1, 3.4), (-.6, 2.8), (-1, 2.2))): m.cube(b, (-.4, y, z), (.8, .8, .8), P['b'] if k % 2 else P['a'])
        h = m.part('buddy', (0, -2.5, -2.4), (0, 0, 0), 'look'); m.cube(h, (-1.3, -1.6, -2), (2.6, 2.2, 2), P['a'])
        mirror(m, h, (-1.8, -1.6, -1.6), (.8, 1, 1), P['c']); m.cube(h, (-1.9, -1.3, -1.3), (.2, .4, .4), BLACK); m.cube(h, (1.7, -1.3, -1.3), (.2, .4, .4), BLACK)
    elif kind == 'caterpillar':
        b = m.part('buddy', (0, 0, 0), (0, 0, 0), 'breathe')
        for k in range(5):
            y = -2.2 - (.6 if k == 1 or k == 3 else 0)
            m.cube(b, (-1.2, y, -2.4 + k * 1.8), (2.4, 2.2, 1.8), P['a'] if k % 2 == 0 else P['b'])
            mirror(m, b, (-1.3, -.4, -2 + k * 1.8), (.4, .4, .4), BLACK)
        h = m.part('buddy', (0, -2, -2.4), (0, 0, 0), 'look'); m.cube(h, (-1.4, -2.2, -1.8), (2.8, 2.6, 1.8), P['a'])
        eyes(m, h, -1.6, -1.9, dx=.7, s=.6); mirror(m, h, (-.9, -3.4, -1.2), (.3, 1.2, .3), BLACK)
    elif kind == 'snowman':
        b = m.part('buddy', (0, 0, 0), (0, 0, 0), 'breathe')
        m.cube(b, (-2.2, -3.6, -2.2), (4.4, 3.6, 4.4), '#f4f8ff'); m.cube(b, (-1.7, -6.2, -1.7), (3.4, 2.8, 3.4), '#f4f8ff')
        for y in (-5.5, -4.6, -2.8): m.cube(b, (-.3, y, -1.85 if y < -4 else -2.35), (.6, .6, .2), BLACK)
        m.cube(b, (-1.8, -6.5, -1.8), (3.6, .7, 3.6), '#c8102e'); m.cube(b, (1, -6.2, -2), (.8, 2, .3), '#c8102e')
        mirror(m, b, (-3.8, -5.4, -.2), (2, .3, .3), '#6b4428')
        h = m.part('buddy', (0, -6.3, 0), (0, 0, 0), 'look'); m.cube(h, (-1.4, -2.8, -1.4), (2.8, 2.8, 2.8), '#f4f8ff')
        eyes(m, h, -2.1, -1.5, dx=.6, s=.5); m.cube(h, (-.25, -1.5, -2.6), (.5, .5, 1.2), '#ff7a1a')
        m.cube(h, (-1.8, -3.1, -1.8), (3.6, .4, 3.6), BLACK); m.cube(h, (-1.2, -5, -1.2), (2.4, 2, 2.4), BLACK); m.cube(h, (-1.25, -3.6, -1.25), (2.5, .5, 2.5), '#c8102e')
    elif kind == 'gingerbread':
        b = m.part('buddy', (0, 0, 0), (0, 0, 0), 'breathe')
        G, I = '#b5713a', '#ffffff'
        m.cube(b, (-1.8, -5, -.5), (3.6, 3.4, 1), G); mirror(m, b, (-3.4, -4.8, -.5), (1.6, 1.1, 1), G); mirror(m, b, (-1.7, -1.6, -.5), (1.4, 1.6, 1), G)
        for y, c in ((-4.4, '#e0243c'), (-3.3, '#1f8a45')): m.cube(b, (-.3, y, -.6), (.6, .6, .2), c)
        mirror(m, b, (-3.3, -4.4, -.55), (.2, .5, .1), I); mirror(m, b, (-1.6, -.6, -.55), (1.2, .2, .1), I)
        h = m.part('buddy', (0, -5, 0), (0, 0, 0), 'look'); m.cube(h, (-1.6, -3.2, -.5), (3.2, 3.2, 1), G)
        eyes(m, h, -2.3, -.65, dx=.6, s=.5); m.cube(h, (-.7, -1, -.6), (1.4, .25, .1), I)
    elif kind == 'crystal':
        b = m.part('buddy', (0, -2, 0), (0, 0, 0), 'spin')
        for x, y, h, w, c in ((-.8, -6.5, 6, 1.6, 'a'), (-2, -4.2, 3.6, 1.2, 'b'), (.9, -4.8, 4.2, 1.2, 'b'), (-.6, -3, 2.4, 1, 'c')):
            m.cube(b, (x, y, -w / 2), (w, h, w), P[c]); m.cube(b, (x + w * .2, y - .7, -w * .3), (w * .6, .7, w * .6), WHITE)
        r = m.part('buddy', (0, -4, 0), (0, 0, 0), 'spin_rev')
        for k in range(3): m.cube(r, (-.35 + (k - 1) * 3.2, -.35, -3.4 + abs(k - 1) * 3), (.7, .7, .7), P['c'])
    elif kind == 'golem':
        b = m.part('buddy', (0, 0, 0), (0, 0, 0), 'hover')
        m.cube(b, (-2.2, -5.4, -1.6), (4.4, 3.6, 3.2), P['a']); mirror(m, b, (-3.4, -5.2, -1), (1.2, 3.2, 2), P['b'])
        mirror(m, b, (-1.8, -1.8, -1.2), (1.4, 1.8, 2.4), P['b']); m.cube(b, (-.6, -4.4, -1.75), (1.2, 1.2, .2), P['c'])
        h = m.part('buddy', (0, -5.4, 0), (0, 0, 0), 'look'); m.cube(h, (-1.7, -3.2, -1.6), (3.4, 3.2, 3.2), P['a'])
        eyes(m, h, -2.2, -1.75, dx=.8, s=.7, col=P['c'])
        for x in (-1.6, -.4, .8): m.cube(h, (x, -4, -1.6), (.8, .9, .6), '#ffd36b')
        m.cube(h, (-1.7, -3.4, -1.7), (3.4, .4, 3.4), '#ffd36b')
    elif kind == 'drone':
        b = m.part('buddy', (0, 0, 0), (0, 0, 0), 'hover')
        m.cube(b, (-1.8, -4.4, -1.8), (3.6, 2.6, 3.6), P['a']); m.cube(b, (-1.2, -4.9, -1.2), (2.4, .5, 2.4), P['b'])
        m.cube(b, (-.7, -3.7, -1.95), (1.4, 1.2, .2), P['c']); m.cube(b, (-.35, -3.45, -2.05), (.7, .7, .1), WHITE)
        for sx, sz in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
            m.cube(b, (sx * 2.2 - .3, -4.2, sz * 2.2 - .3), (.6, .4, .6), P['b'])
            r = m.part('buddy', (sx * 3.2, -4.6, sz * 3.2), (0, 0, 0), 'spin_fast')
            m.cube(r, (-1.6, -.1, -.25), (3.2, .2, .5), '#c8ccd4'); m.cube(r, (-.25, -.1, -1.6), (.5, .2, 3.2), '#c8ccd4')
        m.cube(b, (-.2, -1.8, -.2), (.4, 1, .4), P['c'])

PETS = {
    'katze': ('quad', dict(a='#e0a050', b='#f6e6cc', ears='pointy', tail='thin', inner='#ffb0b8')),
    'schwarze_katze': ('quad', dict(a='#25222c', b='#3a3644', ears='pointy', tail='thin', inner='#6a4a6a', nose='#ff8aa8')),
    'fuchs': ('quad', dict(a='#e8702a', b='#fff4ea', ears='pointy', tail='bushy', snout='long', inner='#2a1a1a', ear='#e8702a', tip='#ffffff')),
    'hund': ('quad', dict(a='#c8935a', b='#f2e2c8', ears='floppy', tail='thin', snout='long', ear='#8a5a30')),
    'hase': ('quad', dict(a='#ece8e2', b='#ffffff', ears='long', tail='short', inner='#ffb6c1', nose='#ff8aa8')),
    'panda': ('quad', dict(a='#f6f6f6', b='#ffffff', ears='round', ear='#1c1c20', inner='#1c1c20', tail='short', tip='#1c1c20', mask='#1c1c20', snout='flat')),
    'pinguin': ('bird', dict(a='#1c1e2a', b='#ffffff', face='#ffffff', beak='#ff9a1f')),
    'frosch': ('frog', dict(a='#5fbf3a', b='#dff7a8')),
    'axolotl': ('axolotl', dict(a='#ffb6d6', b='#ff5a8a')),
    'biene': ('insect', dict(a='#ffd23f')),
    'drache': ('quad', dict(a='#3a9a4a', b='#c8e86a', ears='small', tail='thin', snout='long', horns='#f2e2b8', wings='#7ad07a', tip='#2a6a32')),
    'eule': ('bird', dict(a='#8a5a30', b='#e8d2a8', face='#f2e2c8', beak='#f2b23a', eye=1.0, ring='#ffd36b', tufts=True)),
    'schleim': ('slime', dict(a='#7ae05a', b='#3a9a2a')),
    'ente': ('bird', dict(a='#ffe066', b='#fff4b0', beak='#ff8a1f')),
    'owner_drache': ('quad', dict(a='#7a3ad0', b='#e8d3ff', ears='small', tail='thin', snout='long', horns='#ffd36b', wings='#c77dff', crown='#ffd36b', tip='#ffd36b')),
    'mini_baer': ('quad', dict(a='#8a5a30', b='#c8935a', ears='round', inner='#c8935a', tail='short', snout='flat')),
    'capybara': ('quad', dict(a='#a8784a', b='#c8955f', ears='small', tail=None, snout='flat', head_w=4.8)),
    'igel': ('quad', dict(a='#c8a078', b='#f0dcc0', ears='small', tail=None, spikes=True, spike='#5a3e2a', nose='#1a1a1a')),
    'hummel': ('insect', dict(a='#ffb52e')),
    'otter': ('quad', dict(a='#7a5030', b='#d8b890', ears='small', tail='flat', snout='flat')),
    'kugelfisch': ('puffer', dict(a='#ffd23f', b='#fff4c8', c='#e0a020')),
    'eichhoernchen': ('quad', dict(a='#c8602a', b='#f6e6cc', ears='pointy', tail='huge', inner='#e8a070')),
    'ht4_kristall': ('crystal', dict(a='#7fe0ff', b='#2a8fe0', c='#c77dff')),
    'koi': ('koi', dict(a='#ffffff', b='#ff6a2a', c='#ffb08a')),
    'schildkroete': ('turtle', dict(a='#3a8a4a', b='#a8c878', c='#2a6a32')),
    'qualle': ('jelly', dict(a='#c8a8ff', b='#ff8ad8')),
    'vip_sonnenkueken': ('bird', dict(a='#ffd23f', b='#fff4b0', beak='#ff8a1f', crown='#ff9a1f')),
    'owner_diamantwaechter': ('golem', dict(a='#7ff7ff', b='#2bb8d0', c='#c77dff')),
    'legend_glutfuchs': ('quad', dict(a='#ff5a1f', b='#ffd36b', ears='pointy', tail='bushy', snout='long', inner='#3a0e08', tip='#ffe066')),
    'admin_waechterdrohne': ('drone', dict(a='#2a2c34', b='#4a4c58', c='#ff3a4a')),
    'chamaeleon': ('chameleon', dict(a='#5fd04a', b='#3ac7a0', c='#a8e86a')),
    'raupe': ('caterpillar', dict(a='#7ad04a', b='#c8e86a')),
    'schaf': ('quad', dict(a='#f4f2ec', b='#d8d2c8', head='#3a302a', ears='floppy', ear='#3a302a', tail='short', wool=True, snout='flat', nose='#1a1a1a')),
    'waschbaer': ('quad', dict(a='#8a8a90', b='#e8e8ec', ears='round', inner='#2a2a30', tail='ringed', ring='#2a2a30', mask='#2a2a30', snout='long')),
    'koala': ('quad', dict(a='#9a9aa4', b='#e8e8ec', ears='big_round', inner='#e8e8ec', tail=None, snout='flat', nose='#2a2a30')),
    'roter_panda': ('quad', dict(a='#c8501e', b='#f6e6cc', ears='pointy', inner='#ffffff', tail='ringed', ring='#7a2a10', snout='short')),
    'schneemann': ('snowman', dict()),
    'lebkuchenmann': ('gingerbread', dict()),
    'papagei': ('bird', dict(a='#e0243c', b='#ffd23f', wing='#2a7ae0', beak='#2a2a30', crest='#ffd23f')),
}
GLOW = {'ht4_kristall', 'owner_diamantwaechter', 'legend_glutfuchs', 'qualle', 'owner_drache', 'vip_sonnenkueken'}

def build_pet(c, assets):
    kind, P = PETS[c['id']]
    m = Model()
    if kind == 'quad': quadruped(m, P)
    elif kind == 'bird': bird(m, P)
    elif kind == 'insect': insect(m, P)
    else: special(m, kind, P)
    glow = c['id'] in GLOW
    c['tex_size'] = m.bake(assets, c['texture'], glow, sum(map(ord, c['id'])))
    c['parts'] = m.parts; c['glow'] = glow; c['translucent'] = kind in ('slime', 'jelly'); c['gen'] = 'v24'

def main():
    assets = sys.argv[1]; path = os.path.join(assets, 'builtin/cosmetics.json')
    data = json.load(open(path)); n = 0
    for c in data['cosmetics']:
        if c['type'] == 'pet' and c['id'] in PETS: build_pet(c, assets); n += 1
    json.dump(data, open(path, 'w'), ensure_ascii=False, separators=(',', ':'))
    print('pets rebuilt', n, 'of', sum(1 for c in data['cosmetics'] if c['type'] == 'pet'))

if __name__ == '__main__':
    main()
