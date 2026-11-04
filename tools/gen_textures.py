#!/usr/bin/env python3
"""Draws the 32x32 item textures of the built-in shields (simple pixel art, no antialiasing). Run from the project root."""
from PIL import Image, ImageDraw
import os, sys

S = 32


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (255,)


def hexc(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def new():
    return Image.new("RGBA", (S, S), (0, 0, 0, 0))


def outline_mask(mask, draw_img, color):
    """Draws a 1px outline around a set of pixels onto draw_img."""
    px = draw_img.load()
    for (x, y) in list(mask):
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + dx, y + dy)
            if n not in mask and 0 <= n[0] < S and 0 <= n[1] < S:
                px[n] = color


def mask_of(fn):
    return {(x, y) for y in range(S) for x in range(S) if fn(x, y)}


import math

# Every shield gets its own surface pattern and its own way of being lit (see STYLES at the bottom).
def _hash(x, y):
    return ((x * 73856093) ^ (y * 19349663)) % 11


def _weave(x, y):
    return (1.0, 0.9, 1.04, 0.8, 1.0, 1.1, 0.88)[(x * 3 + y * 5) % 7]


def _grain(x, y):  # vertical wood grain with a few knots
    f = (1.0, 0.9, 1.07, 0.84, 1.0)[(x * 3 + (y // 6)) % 5]
    return f * (0.78 if _hash(x // 2, y // 3) == 0 else 1.0)


def _hgrain(x, y):  # horizontal streaks
    return (1.0, 0.92, 1.08, 0.86)[(y * 3 + x // 5) % 4]


def _plates(x, y):  # horizontal metal plates with dark seams
    if y % 7 == 0:
        return 0.62
    return 1.08 if (y // 7) % 2 == 0 else 0.94


def _hammered(x, y):
    return 0.84 + 0.04 * _hash(x, y)


def _scales(x, y):
    if y % 3 == 0:
        return 0.78
    return 1.14 if (x + 2 * ((y // 3) % 2)) % 4 == 0 else 1.0


def _facets(x, y):  # diagonal crystal bands
    return (1.16, 0.96, 0.8)[((x - y) // 3) % 3]


def _cloth(x, y):
    return (1.0, 0.9, 1.08)[(x + y) % 3]


def _stripes(x, y):
    return 1.1 if (x // 3) % 2 == 0 else 0.92


def _bricks(x, y):  # stone blocks with mortar
    row = y // 4
    if y % 4 == 0 or (x + (row % 2) * 4) % 8 == 0:
        return 0.6
    return 1.0 + 0.05 * (_hash(x // 3, row) - 5) / 5


def _chevron(x, y):  # leaf veins
    return 1.12 if int((abs(x - 15.5) + y) // 3) % 2 == 0 else 0.88


def _rings(x, y):
    return 1.12 if int(math.hypot(x - 15.5, y - 15.5) // 2.5) % 2 == 0 else 0.88


def _rays(x, y):
    return 1.12 if int((math.atan2(y - 15.5, x - 15.5) + math.pi) * 8 / math.pi) % 2 == 0 else 0.9


def _ripple(x, y):
    return 1.12 if (x + ((y // 3) % 2) * 3) % 6 < 3 else 0.9


def _hide(x, y):  # mottled hide in 2x2 blotches
    return 0.78 + 0.07 * ((x // 2) * 31 + (y // 2) * 17) % 5 if False else 0.8 + 0.07 * (((x // 2) * 31 + (y // 2) * 17) % 5)


def _stitch(x, y):  # leather with dashed stitching
    if (x % 8 == 1 or y % 8 == 1) and (x + y) % 2 == 0:
        return 0.65
    return 1.0 + 0.04 * (_hash(x, y) - 5) / 5


PATTERNS = dict(weave=_weave, grain=_grain, hgrain=_hgrain, plates=_plates, hammered=_hammered, scales=_scales, facets=_facets,
                cloth=_cloth, stripes=_stripes, bricks=_bricks, chevron=_chevron, rings=_rings, rays=_rays, ripple=_ripple,
                hide=_hide, stitch=_stitch, smooth=lambda x, y: 1.0)
LIGHTS = dict(
    tl=lambda x, y: 1.2 - 0.42 * (x + y) / (2 * S),
    top=lambda x, y: 1.22 - 0.5 * y / S,
    bl=lambda x, y: 1.0 + 0.3 * (x - y) / S,
    radial=lambda x, y: 1.25 - 0.55 * math.hypot(x - 15.5, y - 15.5) / 16,
    flat=lambda x, y: 1.0,
)
STYLE = ["weave", "tl"]


def tone(base, x, y, weave=True):
    pat = PATTERNS[STYLE[0]](x, y) if weave else 1.0
    return shade(base, LIGHTS[STYLE[1]](x, y) * pat)


def paint(img, mask, base, light=1.3, dark=0.62, bevel=True, weave=True):
    """Fills a mask with a woven, top-left-lit surface; bright top-left and dark bottom-right edges give a bevel."""
    px = img.load()
    for (x, y) in mask:
        c = tone(base, x, y, weave)
        if bevel:
            if (x + 1, y) not in mask or (x, y + 1) not in mask:
                c = shade(c, dark / 0.9)
            elif (x - 1, y) not in mask or (x, y - 1) not in mask:
                c = shade(c, light)
        px[(x, y)] = c


def circle(cx, cy, r):
    return lambda x, y: (x - cx) ** 2 + (y - cy) ** 2 <= r * r


def finish(img, name):
    img.save(os.path.join(OUT, name + ".png"))


def rim_and_face(img, mask_outer, mask_inner, rim, face, line=None, studs=True):
    outline_mask(mask_outer, img, line or shade(rim, 0.22))
    paint(img, mask_outer, rim)
    paint(img, mask_inner, face)
    if studs:  # rivets along the rim
        px = img.load()
        for (x, y) in mask_outer - mask_inner:
            if (x * 5 + y * 3) % 9 == 0 and (x - 1, y) in mask_outer and (x, y - 1) in mask_outer:
                px[(x, y)] = shade(rim, 1.6)


def boss(img, cx, cy, r, color):
    """A shaded dome: bright highlight toward the top-left, dark rim around it."""
    px = img.load()
    for y in range(S):
        for x in range(S):
            d2 = (x - cx) ** 2 + (y - cy) ** 2
            if d2 <= r * r:
                t = (((x - (cx - r * 0.35)) ** 2 + (y - (cy - r * 0.35)) ** 2) ** 0.5) / (r * 1.6)
                px[(x, y)] = shade(color, 1.55 - 1.0 * min(1.0, t))
            elif d2 <= (r + 1) * (r + 1):
                px[(x, y)] = shade(color, 0.3)
    if r >= 2:
        px[(int(cx - r * 0.4), int(cy - r * 0.4))] = (255, 255, 255, 255)


def stripe(img, mask, x0, x1, y0, y1, color):
    px = img.load()
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if (x, y) in mask:
                px[(x, y)] = tone(color, x, y)


def round_shield(name, rim, face, emblem=None, r=12, boss_color=None, planks=False, face2=None):
    img = new()
    outer = mask_of(circle(15.5, 15.5, r))
    inner = mask_of(circle(15.5, 15.5, r - 2))
    rim_and_face(img, outer, inner, rim, face)
    if planks:  # vertical planks of two alternating tones with dark seams
        px = img.load()
        alt = face2 or shade(face, 0.78)
        x0 = int(15.5 - r + 2)
        for (x, y) in inner:
            strip = (x - x0) // 4
            col = tone(face if strip % 2 == 0 else alt, x, y)
            if (x - x0) % 4 == 3:
                col = shade(col, 0.55)
            px[(x, y)] = col
    if emblem == "cross":
        stripe(img, inner, 14, 17, 6, 25, shade(rim, 1.1))
        stripe(img, inner, 6, 25, 14, 17, shade(rim, 1.1))
    if boss_color:
        boss(img, 15, 15, 3 if r >= 11 else 2, boss_color)
    finish(img, name)


def heater(name, rim, face, emblem, gem=None):
    img = new()

    def f(inset):
        def g(x, y):
            top, bottom = 3 + inset, 28 - inset
            if y < top or y > bottom:
                return False
            half = 11 - inset
            t = (y - 14) / max(1, (bottom - 14))
            w = half if y <= 14 else half * (1 - t ** 1.6)
            return abs(x - 15.5) <= w
        return g
    outer, inner = mask_of(f(0)), mask_of(f(2))
    rim_and_face(img, outer, inner, rim, face)
    if emblem:
        stripe(img, inner, 14, 17, 6, 22, emblem)
        stripe(img, inner, 8, 23, 9, 11, emblem)
    if gem:
        boss(img, 15, 13, 4, shade(rim, 1.1))
        boss(img, 15, 13, 2, gem)
    finish(img, name)


def pelta(name, rim, face):
    img = new()
    outer = mask_of(lambda x, y: circle(15.5, 17, 12)(x, y) and not circle(15.5, 6, 7)(x, y))
    inner = mask_of(lambda x, y: circle(15.5, 17, 10)(x, y) and not circle(15.5, 6, 8.5)(x, y))
    rim_and_face(img, outer, inner, rim, face)
    boss(img, 15, 20, 2, shade(rim, 1.3))
    finish(img, name)


def scutum(name, rim, face, spike):
    img = new()
    outer = mask_of(lambda x, y: 6 <= x <= 25 and 2 <= y <= 29)
    inner = mask_of(lambda x, y: 8 <= x <= 23 and 4 <= y <= 27)
    rim_and_face(img, outer, inner, rim, face)
    px = img.load()
    for (x, y) in ((11, 8), (20, 8), (11, 15), (20, 15), (11, 22), (20, 22), (15, 11), (15, 19)):
        px[(x, y)] = spike
        px[(x + 1, y)] = shade(spike, 0.7)
        px[(x, y - 1)] = shade(spike, 1.3)
    finish(img, name)


def pavise(name, rim, face, frost):
    img = new()
    outer = mask_of(lambda x, y: 7 <= x <= 24 and 1 <= y <= 30 and (y > 4 or 9 <= x <= 22))
    inner = mask_of(lambda x, y: 9 <= x <= 22 and 3 <= y <= 28 and (y > 5 or 10 <= x <= 21))
    rim_and_face(img, outer, inner, rim, face)
    px = img.load()
    for y in range(6, 27):
        for x in range(10, 22):
            if (x, y) in inner and (x * 7 + y * 3) % 11 == 0:
                px[(x, y)] = frost
    stripe(img, inner, 14, 17, 5, 26, shade(frost, 0.8))
    finish(img, name)


def aspis(name, rim, face, spark):
    img = new()
    outer = mask_of(circle(15.5, 15.5, 13))
    inner = mask_of(circle(15.5, 15.5, 11))
    rim_and_face(img, outer, inner, rim, face)
    px = img.load()
    bolt = [(17, 6), (16, 7), (15, 8), (14, 9), (15, 10), (16, 11), (15, 12), (14, 13), (13, 14), (14, 15), (15, 16), (16, 17), (15, 18),
            (14, 19), (13, 20), (14, 21), (15, 22)]
    for (x, y) in bolt:
        px[(x, y)] = spark
        px[(x + 1, y)] = shade(spark, 0.85)
    boss(img, 15, 15, 2, shade(rim, 1.3))
    finish(img, name)


def bulwark(name, rim, face, sun):
    img = new()
    outer = mask_of(lambda x, y: 5 <= x <= 26 and 2 <= y <= 29 and (x + y > 9 and (31 - x) + y > 9 and x + (31 - y) > 9 and (31 - x) + (31 - y) > 9))
    inner = mask_of(lambda x, y: 7 <= x <= 24 and 4 <= y <= 27 and (x + y > 13 and (31 - x) + y > 13 and x + (31 - y) > 13 and (31 - x) + (31 - y) > 13))
    rim_and_face(img, outer, inner, rim, face)
    boss(img, 15, 15, 5, sun)
    px = img.load()
    for ang in range(8):
        import math
        a = ang * math.pi / 4
        for r in (7, 8):
            px[(int(15.5 + math.cos(a) * r), int(15.5 + math.sin(a) * r))] = shade(sun, 1.2)
    finish(img, name)


def greatshield(name, rim, face, gem):
    img = new()

    def f(inset):
        def g(x, y):
            if y < 1 + inset or y > 30 - inset:
                return False
            half = 11 - inset
            w = half if y < 18 else half * (1 - ((y - 18) / (30 - inset - 18)) ** 1.8)
            return abs(x - 15.5) <= w
        return g
    outer, inner = mask_of(f(0)), mask_of(f(2))
    rim_and_face(img, outer, inner, rim, face)
    stripe(img, inner, 14, 17, 4, 26, shade(rim, 1.15))
    stripe(img, inner, 7, 24, 9, 12, shade(rim, 1.15))
    boss(img, 15, 10, 3, gem)
    finish(img, name)


def beacon(name, rim, face, light):
    img = new()
    outer = mask_of(circle(15.5, 15.5, 13))
    inner = mask_of(circle(15.5, 15.5, 11))
    rim_and_face(img, outer, inner, rim, face)
    boss(img, 15, 15, 7, shade(light, 0.7))
    boss(img, 15, 15, 5, light)
    boss(img, 14, 14, 2, (255, 255, 255, 255))
    finish(img, name)


def oval(name, rim, face, gem):
    img = new()
    outer = mask_of(lambda x, y: ((x - 15.5) / 10.5) ** 2 + ((y - 15.5) / 14.5) ** 2 <= 1)
    inner = mask_of(lambda x, y: ((x - 15.5) / 8.5) ** 2 + ((y - 15.5) / 12.5) ** 2 <= 1)
    rim_and_face(img, outer, inner, rim, face)
    stripe(img, inner, 14, 17, 5, 26, shade(rim, 1.1))
    boss(img, 15, 15, 3, gem)
    finish(img, name)


def hexa(name, rim, face, gem):
    img = new()

    def f(inset):
        def g(x, y):
            dx, dy = abs(x - 15.5), abs(y - 15.5)
            r = 14 - inset
            return dy <= r * 0.87 and dx + dy * 0.577 <= r
        return g
    outer, inner = mask_of(f(0)), mask_of(f(2))
    rim_and_face(img, outer, inner, rim, face)
    boss(img, 15, 15, 4, gem)
    finish(img, name)


def arch(name, rim, face, gem):
    img = new()

    def f(inset):
        def g(x, y):
            if y < 2 + inset or y > 29 - inset:
                return False
            half = 10 - inset
            if y < 11:  # rounded top
                return (x - 15.5) ** 2 / half ** 2 + (y - 11) ** 2 / (9 - inset) ** 2 <= 1
            return abs(x - 15.5) <= half
        return g
    outer, inner = mask_of(f(0)), mask_of(f(2))
    rim_and_face(img, outer, inner, rim, face)
    stripe(img, inner, 14, 17, 8, 27, shade(rim, 1.15))
    stripe(img, inner, 8, 23, 17, 19, shade(rim, 1.15))
    boss(img, 15, 18, 3, gem)
    finish(img, name)


def sq(x0, y0, x1, y1):
    return mask_of(lambda x, y: x0 <= x <= x1 and y0 <= y <= y1)


def rect(img, x0, y0, x1, y1, color):
    px = img.load()
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px[(x, y)] = color


def creeper(name):
    img = new()
    m = sq(4, 4, 27, 27)
    outline_mask(m, img, (10, 24, 10, 255))
    paint(img, m, hexc("#5fae4e"))
    dark = (14, 26, 14, 255)
    cells = [(1, 2), (2, 2), (1, 3), (2, 3), (5, 2), (6, 2), (5, 3), (6, 3), (3, 4), (4, 4),
             (2, 5), (3, 5), (4, 5), (5, 5), (2, 6), (5, 6), (2, 7), (5, 7)]
    for cx, cy in cells:
        rect(img, 4 + cx * 3, 4 + cy * 3, 6 + cx * 3, 6 + cy * 3, dark)
    finish(img, name)


def slime(name):
    img = new()
    outer = sq(4, 4, 27, 27)
    outline_mask(outer, img, (24, 70, 24, 255))
    paint(img, outer, hexc("#86d46c"))
    core = sq(10, 10, 21, 21)
    outline_mask(core, img, (40, 110, 40, 255))
    paint(img, core, hexc("#3f9b3a"))
    for (x, y) in ((6, 6), (7, 6), (6, 7), (12, 12), (13, 12), (12, 13)):
        img.putpixel((x, y), (235, 255, 225, 255))
    finish(img, name)


def cactus(name):
    img = new()
    m = mask_of(lambda x, y: 8 <= x <= 23 and 4 <= y <= 29 and not ((x in (8, 23)) and (y in (4, 29))))
    outline_mask(m, img, (14, 40, 16, 255))
    paint(img, m, hexc("#3f7d3a"))
    for y in range(7, 28, 5):
        for x in (11, 16, 21):
            xx = x + (2 if (y // 5) % 2 else 0)
            if (xx, y) in m:
                img.putpixel((xx, y), (238, 244, 208, 255))
    rect(img, 14, 1, 17, 3, hexc("#e0609a"))
    img.putpixel((15, 2), hexc("#ffd84a"))
    finish(img, name)


def skull(name):
    img = new()
    head = mask_of(lambda x, y: 6 <= x <= 25 and 3 <= y <= 21 and not (x in (6, 25) and y in (3, 21)))
    jaw = sq(10, 22, 21, 28)
    outline_mask(head | jaw, img, (6, 6, 8, 255))
    paint(img, head, hexc("#3b3b42"))
    paint(img, jaw, hexc("#2e2e34"))
    rect(img, 9, 10, 12, 14, (6, 6, 8, 255))
    rect(img, 19, 10, 22, 14, (6, 6, 8, 255))
    rect(img, 10, 11, 11, 12, hexc("#7a8aa0"))
    rect(img, 20, 11, 21, 12, hexc("#7a8aa0"))
    rect(img, 15, 15, 16, 18, (6, 6, 8, 255))
    for x in (13, 16, 19):
        rect(img, x, 23, x, 27, (6, 6, 8, 255))
    finish(img, name)


def jack(name):
    img = new()
    m = mask_of(lambda x, y: ((x - 15.5) / 13.5) ** 2 + ((y - 17.5) / 11.5) ** 2 <= 1)
    outline_mask(m, img, (70, 30, 6, 255))
    paint(img, m, hexc("#e0801e"))
    for dx in (4, 8, 11):
        for sgn in (-1, 1):
            x = int(15.5 + sgn * dx)
            for y in range(7, 28):
                if (x, y) in m:
                    img.putpixel((x, y), shade(hexc("#e0801e"), 0.72))
    rect(img, 14, 3, 17, 7, hexc("#5a7a2a"))
    outline_mask(sq(14, 3, 17, 7), img, (30, 50, 14, 255))
    glow = hexc("#ffd84a")
    for (rows, flip) in (({12: (11, 11), 13: (10, 12), 14: (9, 13)}, False),):
        for y, (a, b) in rows.items():
            rect(img, a, y, b, y, glow)
            rect(img, 31 - b, y, 31 - a, y, glow)
    rect(img, 9, 19, 22, 20, glow)
    rect(img, 13, 20, 13, 20, hexc("#e0801e"))
    rect(img, 18, 20, 18, 20, hexc("#e0801e"))
    rect(img, 11, 21, 12, 21, glow)
    rect(img, 19, 21, 20, 21, glow)
    finish(img, name)


def piston(name):
    img = new()
    m = sq(4, 4, 27, 27)
    inner = sq(7, 7, 24, 24)
    outline_mask(m, img, (40, 28, 14, 255))
    paint(img, m, hexc("#a8844f"))
    paint(img, inner, hexc("#8d8d92"))
    plate = sq(11, 11, 20, 20)
    outline_mask(plate, img, (40, 40, 44, 255))
    paint(img, plate, hexc("#5c5c62"))
    for (x, y) in ((12, 12), (19, 12), (12, 19), (19, 19)):
        img.putpixel((x, y), (200, 200, 205, 255))
    rect(img, 14, 14, 17, 17, hexc("#a8844f"))
    finish(img, name)


def beacon_block(name):
    img = new()
    m = sq(4, 4, 27, 27)
    glass = sq(7, 7, 24, 24)
    outline_mask(m, img, (8, 4, 18, 255))
    paint(img, m, hexc("#2a1a4a"))
    paint(img, glass, hexc("#a8e8f0"))
    px = img.load()
    for y in range(S):
        for x in range(S):
            d = abs(x - 15.5) + abs(y - 15.5)
            if d <= 6 and (x, y) in glass:
                px[(x, y)] = shade(hexc("#e6fcff"), 1.1 - d * 0.05)
    boss(img, 15, 15, 2, (255, 255, 255, 255))
    finish(img, name)


def door(name, wood, dark):
    img = new()
    outer = mask_of(lambda x, y: 8 <= x <= 23 and 1 <= y <= 30)
    inner = mask_of(lambda x, y: 9 <= x <= 22 and 2 <= y <= 29)
    outline_mask(outer, img, (20, 14, 10, 255))
    paint(img, outer, dark)
    paint(img, inner, wood)
    px = img.load()
    for y in range(2, 30):
        px[(15, y)] = shade(wood, 0.75)
    for x in range(9, 23):
        px[(x, 8)] = shade(wood, 0.8)
        px[(x, 23)] = shade(wood, 0.8)
    boss(img, 19, 16, 2, (214, 182, 90, 255))
    finish(img, name)


def pan(name, iron, handle):
    img = new()
    outer = mask_of(circle(14.5, 15.5, 12))
    inner = mask_of(circle(14.5, 15.5, 10))
    h = mask_of(lambda x, y: 25 <= x <= 31 and 14 <= y <= 17)
    outline_mask(outer | h, img, (12, 12, 14, 255))
    paint(img, h, handle, 1.2, 0.7)
    paint(img, outer, shade(iron, 1.1))
    paint(img, inner, shade(iron, 0.55))
    px = img.load()
    for (x, y) in ((8, 9), (9, 8), (10, 7), (7, 11), (7, 12)):
        px[(x, y)] = shade(iron, 1.5)
    finish(img, name)


def lid(name, wood, band):
    round_shield(name, band, wood, planks=True, r=11)


def kite(name, rim, face, emblem):
    img = new()

    def f(inset):
        def g(x, y):
            top, bottom = 1 + inset, 30 - inset
            if y < top or y > bottom:
                return False
            half = 9 - inset
            w = half * (1 - ((y - 10) / (bottom - 10)) ** 1.3) if y > 10 else half * (0.55 + 0.45 * (y - top) / max(1, 10 - top))
            return abs(x - 15.5) <= w
        return g
    outer, inner = mask_of(f(0)), mask_of(f(2))
    rim_and_face(img, outer, inner, rim, face)
    stripe(img, inner, 14, 17, 5, 25, emblem)
    boss(img, 15, 11, 2, shade(emblem, 1.2))
    finish(img, name)


# ---- extra silhouettes and per-shield emblems (core shields; each one has its own shape or motif) ----
def erode(mask, n=2):
    cur = set(mask)
    for _ in range(n):
        cur = {(x, y) for (x, y) in cur if all((x + dx, y + dy) in cur for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))}
    return cur


def poly_mask(pts):
    layer = Image.new("L", (S, S), 0)
    ImageDraw.Draw(layer).polygon(pts, fill=255)
    px = layer.load()
    return {(x, y) for y in range(S) for x in range(S) if px[(x, y)]}


def shaped(name, outer, rim, face):
    img = new()
    rim_and_face(img, outer, erode(outer, 2), rim, face)
    finish(img, name)


def diamond(name, rim, face):
    shaped(name, poly_mask([(16, 2), (28, 16), (16, 30), (4, 16)]), rim, face)


def octagon(name, rim, face):
    shaped(name, poly_mask([(10, 3), (22, 3), (28, 9), (28, 21), (22, 28), (10, 28), (4, 21), (4, 9)]), rim, face)


def teardrop(name, rim, face):
    circ = mask_of(circle(15.5, 19, 11))
    tri = poly_mask([(16, 2), (5, 17), (27, 17)])
    shaped(name, circ | tri, rim, face)


def bossed(name, outer, rim, face, boss_color, bx=15, by=15, br=3, cross=None, vstripe=None):
    """Plain silhouette with the classic look: rim, face, optional stripes and a shaded dome boss."""
    img = new()
    inner = erode(outer, 2)
    rim_and_face(img, outer, inner, rim, face)
    if cross:
        stripe(img, inner, 14, 17, 4, 27, cross)
        stripe(img, inner, 4, 27, by - 1, by + 2, cross)
    if vstripe:
        stripe(img, inner, 14, 17, 4, 27, vstripe)
    boss(img, bx, by, br, boss_color)
    finish(img, name)


def _circ_union(*cs):
    fns = [circle(*c) for c in cs]
    return mask_of(lambda x, y: any(f(x, y) for f in fns))


def quatrefoil():
    return _circ_union((10.5, 10.5, 7), (21, 10.5, 7), (10.5, 21, 7), (21, 21, 7), (15.5, 15.5, 8))


def trapezoid():
    return poly_mask([(4, 4), (27, 4), (23, 29), (9, 29)])


def inv_triangle():
    return poly_mask([(3, 4), (28, 4), (15.5, 30)])


def heart_shape():
    return _circ_union((10.5, 11, 8.5), (21, 11, 8.5)) | poly_mask([(3, 14), (28, 14), (15.5, 30)])



def _star(n, ro, ri, cx=15.5, cy=15.5):
    pts = []
    for k in range(n * 2):
        a = math.pi * k / n - math.pi / 2
        r = ro if k % 2 == 0 else ri
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return poly_mask(pts)


def _plus():
    return poly_mask([(11, 2), (20, 2), (20, 11), (29, 11), (29, 20), (20, 20), (20, 29), (11, 29), (11, 20), (2, 20), (2, 11), (11, 11)])


DARK = (22, 14, 12, 255)


def emblem_on(name, face, cover_r, draw_fn, spikes=None, cx=15.5, cy=15.5):
    """Re-opens an already drawn shield, wipes its centre boss with plain face colour and paints a unique motif on it."""
    path = os.path.join(OUT, name + ".png")
    img = Image.open(path).convert("RGBA")
    px = img.load()
    if cover_r:
        for y in range(S):
            for x in range(S):
                if (x - cx) ** 2 + (y - cy) ** 2 <= cover_r * cover_r and px[(x, y)][3]:
                    px[(x, y)] = tone(face, x, y)
    layer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    draw_fn(ImageDraw.Draw(layer))
    mask = {(x, y) for y in range(S) for x in range(S) if layer.getpixel((x, y))[3]}
    outline_mask(mask, layer, DARK)
    img.alpha_composite(layer)
    if spikes:
        spikes(img)
    img.save(path)


def _swords(d):
    for a, b in (((9, 8), (22, 21)), ((22, 8), (9, 21))):
        d.line([a, b], fill=hexc("#e6e9ee"), width=2)
    for x, y in ((9, 22), (22, 22), (9, 7), (22, 7)):
        d.rectangle([x - 1, y - 1, x + 1, y + 1], fill=hexc("#e8c04a"))


def _upright_sword(d):
    d.line([(16, 6), (16, 21)], fill=hexc("#e6e9ee"), width=3)
    d.polygon([(14, 6), (18, 6), (16, 3)], fill=hexc("#e6e9ee"))
    d.rectangle([11, 21, 21, 22], fill=hexc("#e8c04a"))
    d.rectangle([14, 23, 18, 26], fill=hexc("#7a4a22"))


def _compass(d):
    d.polygon([(16, 5), (19, 13), (27, 16), (19, 19), (16, 27), (13, 19), (5, 16), (13, 13)], fill=hexc("#efe6c0"))
    d.polygon([(16, 5), (18, 13), (14, 13)], fill=hexc("#c8402e"))
    d.ellipse([14, 14, 18, 18], fill=hexc("#6b5a2c"))


def _shell(d):
    d.polygon([(16, 24), (7, 15), (9, 9), (16, 6), (23, 9), (25, 15)], fill=hexc("#f2ead2"))
    for tip in ((9, 10), (12, 7), (16, 6), (20, 7), (23, 10)):
        d.line([(16, 24), tip], fill=hexc("#a8946a"), width=1)
    d.rectangle([14, 24, 18, 26], fill=hexc("#a8946a"))


def _gauge(d):
    d.rectangle([9, 6, 22, 26], fill=hexc("#1c3a4a"))
    for i, c in enumerate(("#9af0ff", "#6fd0ee", "#4fb0d8", "#2f5f78")):
        d.rectangle([11, 8 + i * 5, 20, 11 + i * 5], fill=hexc(c))


def _ripples(d):
    for r, c in ((9, "#bff7ff"), (6, "#e6fdff"), (3, "#ffffff")):
        d.ellipse([15.5 - r, 21 - r * 0.7, 15.5 + r, 21 + r * 0.7], outline=hexc(c))
    d.ellipse([14, 10, 17, 14], fill=hexc("#ffffff"))


def _heart(d):
    c = hexc("#ffd23a")
    d.ellipse([8, 9, 16, 17], fill=c)
    d.ellipse([15, 9, 23, 17], fill=c)
    d.polygon([(8, 14), (23, 14), (16, 25)], fill=c)
    d.polygon([(12, 12), (19, 12), (16, 19)], fill=hexc("#ff7a1e"))


def _crack(d):
    d.line([(20, 3), (15, 12), (19, 14), (12, 28)], fill=hexc("#8f1f2a"), width=2)
    d.line([(19, 14), (24, 17)], fill=hexc("#8f1f2a"), width=1)


def _watchful_eye(d):
    d.polygon([(7, 16), (16, 10), (25, 16), (16, 22)], fill=hexc("#e8dcc0"))
    d.ellipse([13, 13, 19, 19], fill=hexc("#d94a3a"))
    d.ellipse([15, 14, 17, 18], fill=DARK)
    for x in (4, 27):
        d.line([(x, 4), (x, 27)], fill=hexc("#2e3138"), width=2)


def _paw(d):
    c = hexc("#e8d8b8")
    d.ellipse([10, 16, 21, 25], fill=c)
    for x, y in ((7, 14), (12, 9), (18, 9), (23, 14)):
        d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=c)


def _feathers(d):
    for x0, x1 in ((9, 12), (15, 17), (21, 22)):
        d.polygon([(x0, 22), (x1 - 1, 9), (x1 + 2, 9), (x0 + 3, 22)], fill=hexc("#e8f2d0"))
        d.line([(x0 + 1, 22), (x1 + 1, 9)], fill=hexc("#5d8a5a"))


def _scutum_spikes(d):
    d.polygon([(16, 7), (21, 16), (16, 25), (11, 16)], fill=hexc("#dfe3e8"))
    d.polygon([(16, 7), (21, 16), (16, 16)], fill=hexc("#aeb4bd"))
    for x, y in ((9, 7), (22, 7), (9, 25), (22, 25)):
        d.polygon([(x, y - 2), (x + 2, y), (x, y + 2), (x - 2, y)], fill=hexc("#c9ced6"))


def _snowflake(d):
    c = hexc("#ffffff")
    for dx, dy in ((0, 9), (8, 4.5), (8, -4.5)):
        d.line([(15.5 - dx, 15.5 - dy), (15.5 + dx, 15.5 + dy)], fill=c, width=2)
    d.ellipse([13, 13, 18, 18], fill=hexc("#cfeaff"))


def _copper_rings(d):
    d.ellipse([8, 8, 23, 23], outline=hexc("#7a3f22"))
    d.ellipse([11, 11, 20, 20], outline=hexc("#f0b078"))
    d.polygon([(16, 12), (17, 15), (20, 16), (17, 17), (16, 20), (15, 17), (12, 16), (15, 15)], fill=hexc("#ffe0b8"))


def _rim_teeth(img):
    c = shade(hexc("#9a5a34"), 1.25)
    for pts in (((16, 1), (13, 4), (18, 4)), ((16, 30), (13, 27), (18, 27)), ((1, 16), (4, 13), (4, 18)), ((30, 16), (27, 13), (27, 18))):
        layer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
        ImageDraw.Draw(layer).polygon(list(pts), fill=c)
        m = {(x, y) for y in range(S) for x in range(S) if layer.getpixel((x, y))[3]}
        outline_mask(m, layer, DARK)
        img.alpha_composite(layer)


def _bolt(d):
    d.polygon([(19, 4), (10, 17), (15, 17), (12, 28), (22, 13), (16, 13)], fill=hexc("#ffe14d"))


def _sun(d):
    import math as m
    for k in range(8):
        a = k * m.pi / 4
        d.line([(15.5 + 5 * m.cos(a), 15.5 + 5 * m.sin(a)), (15.5 + 9 * m.cos(a), 15.5 + 9 * m.sin(a))], fill=hexc("#fff6c8"), width=2)
    d.ellipse([11, 11, 20, 20], fill=hexc("#fffbe0"))
    d.ellipse([13, 13, 18, 18], fill=hexc("#f4c542"))


USE_EMBLEMS = False  # the user kept the original surface look; only silhouettes changed (set True for the motif redraw)
# id: (face colour, cover radius, motif, optional extra on the finished image, shape drawn by P instead of the default)
EMBLEMS = dict(
    skirmisher_pelta=(hexc("#7cb06a"), 7, _feathers, None),
    thornback_scutum=(hexc("#7d6a56"), 9, _scutum_spikes, None),
    frostguard_pavise=(hexc("#a9c9e6"), 8, _snowflake, None),
    copper_roundel=(hexc("#d38a52"), 7, _copper_rings, _rim_teeth),
    stormcaller_aspis=(hexc("#4a74c4"), 7, _bolt, None),
    sunforged_bulwark=(hexc("#e7b24a"), 9, _sun, None),
    wardens_greatshield=(hexc("#7a808c"), 9, _watchful_eye, None),
    ironhide_targe=(hexc("#8a6a4c"), 7, _paw, None),
    duelists_parma=(hexc("#c45a5a"), 8, _swords, None),
    duelists_buckler=(hexc("#b8505e"), 0, _upright_sword, None),
    wayfarers_roundshield=(hexc("#8c9a5c"), 5, _compass, None),
    pilgrims_roundshield=(hexc("#c9b88a"), 0, _shell, None),
    reservoir_bulwark=(hexc("#4f8fb0"), 9, _gauge, None),
    wellspring_bulwark=(hexc("#4fb5a8"), 0, _ripples, None),
    phoenix_heart_shield=(hexc("#d4501e"), 7, _heart, None),
    shieldmaidens_last_stand=(hexc("#ece8dc"), 0, _crack, None),
)


def build(name, parts, dark, extra=None, behind=None):
    img = new()
    d = ImageDraw.Draw(img)
    if behind:
        behind(d)
    union = set()
    for mask, _ in parts:
        union |= mask
    layer = {(x, y) for y in range(S) for x in range(S) if img.getpixel((x, y))[3]}
    outline_mask(union | layer, img, dark)
    for mask, color in parts:
        paint(img, mask, color)
    if extra:
        extra(ImageDraw.Draw(img))
    finish(img, name)


def ell(cx, cy, rx, ry):
    return mask_of(lambda x, y: ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1.0)


def pufferfish(name):
    def behind(d):
        import math as m
        for k in range(10):
            a = k * m.pi / 5
            cx, cy = 15.5 + 11.5 * m.cos(a), 16 + 11.5 * m.sin(a)
            d.polygon([(15.5 + 8 * m.cos(a - 0.2), 16 + 8 * m.sin(a - 0.2)), (cx, cy), (15.5 + 8 * m.cos(a + 0.2), 16 + 8 * m.sin(a + 0.2))],
                      fill=hexc("#9c7424"))

    def extra(d):
        for ex in (9, 19):
            d.rectangle([ex, 10, ex + 3, 14], fill=(250, 250, 250, 255))
            d.rectangle([ex + 1, 11, ex + 2, 13], fill=(10, 10, 10, 255))
        d.rectangle([13, 19, 18, 20], fill=(60, 30, 20, 255))
    build(name, [(ell(15.5, 16, 10, 9.5), hexc("#d9b04a"))], (40, 28, 10, 255), extra, behind)


def turtle(name):
    def extra(d):
        g = hexc("#2d5a22")
        d.polygon([(11, 10), (20, 10), (23, 16), (20, 22), (11, 22), (8, 16)], outline=g)
        d.polygon([(12, 12), (19, 12), (21, 16), (19, 20), (12, 20), (10, 16)], fill=hexc("#7ab356"))
        for a, b in (((11, 10), (8, 5)), ((20, 10), (23, 5)), ((23, 16), (28, 16)), ((20, 22), (23, 27)), ((11, 22), (8, 27)), ((8, 16), (3, 16))):
            d.line([a, b], fill=g)
    build(name, [(ell(15.5, 16, 13, 11.5), hexc("#4f8f3a"))], (14, 30, 12, 255), extra)


def eye_of_ender(name):
    def extra(d):
        d.ellipse([9, 9, 22, 22], fill=hexc("#3aa88a"))
        d.ellipse([12, 12, 19, 19], fill=hexc("#d8f060"))
        d.rectangle([15, 12, 16, 19], fill=(8, 30, 24, 255))
        for x, y in ((5, 7), (26, 9), (6, 24), (25, 25), (15, 3), (16, 28)):
            d.point((x, y), fill=hexc("#b36bff"))
    build(name, [(mask_of(circle(15.5, 15.5, 12)), hexc("#1f5a4a"))], (6, 20, 18, 255), extra)


def blaze(name):
    def behind(d):
        d.line([(4, 4), (27, 27)], fill=hexc("#e8a020"), width=3)
        d.line([(27, 4), (4, 27)], fill=hexc("#e8a020"), width=3)

    def extra(d):
        for ex in (10, 19):
            d.rectangle([ex, 12, ex + 3, 14], fill=(30, 20, 5, 255))
            d.point((ex + 1, 13), fill=hexc("#ffe060"))
        d.rectangle([13, 18, 18, 21], fill=(30, 20, 5, 255))
        d.rectangle([9, 9, 22, 9], fill=hexc("#ffe27a"))
    build(name, [(sq(8, 8, 23, 23), hexc("#f0b020"))], (40, 20, 5, 255), extra, behind)


def prismarine(name):
    def extra(d):
        d.polygon([(16, 10), (22, 16), (16, 22), (10, 16)], fill=hexc("#cff5ec"))
        d.ellipse([14, 14, 18, 18], fill=hexc("#2b8f86"))
        for x, y in ((16, 5), (5, 16), (26, 16), (16, 27)):
            d.rectangle([x - 1, y - 1, x + 1, y + 1], fill=hexc("#e6fffa"))
    build(name, [(poly_mask([(16, 2), (29, 16), (16, 30), (3, 16)]), hexc("#5fd0c0"))], (8, 40, 40, 255), extra)


def phantom_wing(name):
    pts = [(4, 6), (27, 6), (29, 12), (26, 14), (28, 20), (24, 22), (25, 28), (16, 24), (7, 28), (8, 22), (4, 20), (6, 14), (3, 12)]

    def extra(d):
        bone = hexc("#cfd8ea")
        d.line([(4, 6), (27, 6)], fill=bone, width=2)
        for tip in ((5, 19), (8, 27), (16, 23), (24, 27), (27, 19)):
            d.line([(15, 7), tip], fill=bone)
        d.point((15, 4), fill=hexc("#7dffb0"))
    build(name, [(poly_mask(pts), hexc("#4a5fa8"))], (10, 12, 40, 255), extra)


def cobweb(name):
    def extra(d):
        import math as m
        web = hexc("#8d939c")
        for k in range(8):
            a = k * m.pi / 4
            d.line([(15.5, 15.5), (15.5 + 12 * m.cos(a), 15.5 + 12 * m.sin(a))], fill=web)
        for r in (4, 8, 11):
            pts = [(15.5 + r * m.cos(k * m.pi / 4), 15.5 + r * m.sin(k * m.pi / 4)) for k in range(8)]
            d.polygon(pts, outline=web)
        d.rectangle([21, 20, 23, 22], fill=(20, 20, 24, 255))
        for a, b in (((21, 20), (18, 18)), ((23, 20), (26, 18)), ((21, 22), (18, 25)), ((23, 22), (26, 25))):
            d.line([a, b], fill=(20, 20, 24, 255))
        d.point((21, 20), fill=(230, 50, 50, 255))
    build(name, [(mask_of(circle(15.5, 15.5, 13)), hexc("#dfe3e8"))], (70, 74, 82, 255), extra)


def golden_apple(name):
    body = ell(11.5, 17, 8, 9) | ell(20, 17, 8, 9) | ell(15.5, 19, 9, 9)

    def extra(d):
        d.rectangle([15, 4, 16, 9], fill=hexc("#6b4521"))
        d.polygon([(17, 7), (24, 4), (22, 10)], fill=hexc("#4f9a3a"))
        d.rectangle([8, 12, 10, 15], fill=(255, 250, 200, 255))
        d.point((12, 11), fill=(255, 250, 200, 255))
    build(name, [(body, hexc("#f2c230"))], (60, 40, 6, 255), extra)


def bell(name):
    pts = [(13, 4), (18, 4), (20, 7), (22, 13), (23, 19), (27, 24), (27, 26), (4, 26), (4, 24), (8, 19), (9, 13), (11, 7)]

    def extra(d):
        d.rectangle([6, 24, 25, 25], fill=hexc("#b58a1c"))
        d.rectangle([12, 8, 13, 20], fill=hexc("#fff0a0"))
        d.ellipse([14, 27, 18, 31], fill=hexc("#7a5a14"))
        d.ellipse([14, 0, 17, 3], outline=hexc("#7a5a14"))
    build(name, [(poly_mask(pts), hexc("#e8b830"))], (60, 40, 6, 255), extra)


def lantern(name):
    def extra(d):
        d.rectangle([10, 10, 21, 22], fill=hexc("#ffd060"))
        d.rectangle([12, 12, 19, 20], fill=hexc("#fff2b0"))
        d.rectangle([9, 7, 22, 9], fill=hexc("#2e2e34"))
        d.rectangle([9, 23, 22, 25], fill=hexc("#2e2e34"))
        d.arc([11, 0, 20, 9], 180, 360, fill=hexc("#2e2e34"), width=2)
        for x in (9, 22):
            d.line([(x, 9), (x, 23)], fill=hexc("#2e2e34"))
    build(name, [(sq(9, 7, 22, 25), hexc("#4a4a52"))], (10, 10, 12, 255), extra)



P = dict(
    bastion_shield=lambda: bossed("bastion_shield", poly_mask([(16, 3), (28, 12), (24, 28), (8, 28), (4, 12)]), hexc("#6b6f78"), hexc("#9aa0aa"), hexc("#d8dbe0"), by=16),
    bannermans_standard=lambda: bossed("bannermans_standard", poly_mask([(5, 3), (27, 3), (27, 29), (16, 22), (5, 29)]), hexc("#5a2a2a"), hexc("#b84a44"), hexc("#f0d878"), by=11, vstripe=hexc("#f0d878")),
    wicker_oval=lambda: bossed("wicker_oval", ell(15.5, 16, 14, 9.5), hexc("#6b4a2a"), hexc("#b58a52"), hexc("#d9c38a"), br=2),
    crusaders_cross=lambda: bossed("crusaders_cross", _plus(), hexc("#b8892a"), hexc("#e8dcc0"), hexc("#c0392b"), br=3),
    spearpoint_shield=lambda: bossed("spearpoint_shield", poly_mask([(16, 2), (28, 28), (4, 28)]), hexc("#5a5f68"), hexc("#a9afb8"), hexc("#e8eaee"), by=21, br=3),
    gatehouse_shield=lambda: bossed("gatehouse_shield", mask_of(circle(15.5, 13, 13)) & poly_mask([(0, 2), (31, 2), (31, 31), (0, 31)]) | poly_mask([(3, 3), (28, 3), (28, 13), (3, 13)]), hexc("#5c5a54"), hexc("#8f8b80"), hexc("#d5d0c0"), by=13, cross=hexc("#5c5a54")),
    forge_twins_roundel=lambda: bossed("forge_twins_roundel", _circ_union((10.5, 16, 9.5), (21, 16, 9.5)), hexc("#5a2418"), hexc("#c4522a"), hexc("#ffc23a"), bx=15, by=16, br=3),
    leaf_shield=lambda: bossed("leaf_shield", poly_mask([(16, 2), (25, 8), (28, 16), (25, 24), (16, 30), (7, 24), (4, 16), (7, 8)]), hexc("#3a5a2a"), hexc("#7fae5a"), hexc("#d9f0a8"), vstripe=hexc("#d9f0a8"), br=2),
    sunburst_targe=lambda: bossed("sunburst_targe", _star(8, 15, 9.5), hexc("#b87a1e"), hexc("#f0c24a"), hexc("#fff4b8"), br=4),
    cathedral_aegis=lambda: bossed("cathedral_aegis", poly_mask([(9, 4), (22, 4), (28, 12), (23, 21), (16, 30), (9, 21), (4, 12)]) | poly_mask([(16, 0), (24, 6), (8, 6)]), hexc("#8a8f9c"), hexc("#dfe4ee"), hexc("#5aa0d8"), by=14, cross=hexc("#8a8f9c")),
    standard_shield=lambda: heater("standard_shield", hexc("#8d939c"), hexc("#8a6a42"), None),
    wooden_buckler=lambda: round_shield("wooden_buckler", hexc("#7a5230"), hexc("#a97a47"), r=9, boss_color=hexc("#c9ccd2"), planks=True, face2=hexc("#8a5f35")),
    iron_heater_shield=lambda: heater("iron_heater_shield", hexc("#8d939c"), hexc("#b9bfc8"), hexc("#7b2d2d")),
    copper_roundel=lambda: bossed("copper_roundel", quatrefoil(), hexc("#9a5a34"), hexc("#d38a52"), hexc("#f0b078"), cross=shade(hexc("#9a5a34"), 1.1)),
    skirmisher_pelta=lambda: pelta("skirmisher_pelta", hexc("#3f6f4a"), hexc("#7cb06a")),
    thornback_scutum=lambda: scutum("thornback_scutum", hexc("#5a4a3a"), hexc("#7d6a56"), hexc("#d5d8dc")),
    frostguard_pavise=lambda: pavise("frostguard_pavise", hexc("#5c7fa3"), hexc("#a9c9e6"), hexc("#f4fbff")),
    stormcaller_aspis=lambda: aspis("stormcaller_aspis", hexc("#2f4f8f"), hexc("#4a74c4"), hexc("#ffe14d")),
    sunforged_bulwark=lambda: bulwark("sunforged_bulwark", hexc("#b8792a"), hexc("#e7b24a"), hexc("#fff0a8")),
    wardens_greatshield=lambda: greatshield("wardens_greatshield", hexc("#4b4f58"), hexc("#7a808c"), hexc("#d94a3a")),
    vanguard_kite=lambda: bossed("vanguard_kite", trapezoid(), hexc("#6b6f78"), hexc("#b43c3c"), hexc("#e8e2d0"), vstripe=hexc("#e8e2d0"), br=2),
    beacon_aegis=lambda: beacon_block("beacon_aegis"),
    tidebreaker_buckler=lambda: round_shield("tidebreaker_buckler", hexc("#2d6f86"), hexc("#58a9bd"), r=11, boss_color=hexc("#e6f6f8")),
    emberhide_targe=lambda: round_shield("emberhide_targe", hexc("#5a2a1a"), hexc("#b5532a"), emblem="cross", boss_color=hexc("#ffb347")),
    gravewatch_kite=lambda: kite("gravewatch_kite", hexc("#2f3a3a"), hexc("#4f6b5c"), hexc("#9de0c4")),
    verdant_ward=lambda: bulwark("verdant_ward", hexc("#2f6b3a"), hexc("#6fb55a"), hexc("#d9f5a0")),
    windrunner_parma=lambda: round_shield("windrunner_parma", hexc("#8fa9b8"), hexc("#d8e8ef"), r=12, boss_color=hexc("#ffffff")),
    dragonscale_heater=lambda: heater("dragonscale_heater", hexc("#3a1f1f"), hexc("#8f2a24"), None, gem=hexc("#ffb02e")),
    ironhide_targe=lambda: oval("ironhide_targe", hexc("#5a4636"), hexc("#8a6a4c"), hexc("#d5d8dc")),
    duelists_parma=lambda: hexa("duelists_parma", hexc("#7a2f3a"), hexc("#c45a5a"), hexc("#f2d27a")),
    wayfarers_roundshield=lambda: bossed("wayfarers_roundshield", inv_triangle(), hexc("#4a5a3a"), hexc("#8c9a5c"), hexc("#d9d2a8"), by=12),
    reservoir_bulwark=lambda: arch("reservoir_bulwark", hexc("#2f5a70"), hexc("#4f8fb0"), hexc("#9af0ff")),
    phoenix_heart_shield=lambda: kite("phoenix_heart_shield", hexc("#5a1a12"), hexc("#d4501e"), hexc("#ffc23a")),
    pufferfish_buckler=lambda: pufferfish("pufferfish_buckler"),
    turtle_shell_shield=lambda: turtle("turtle_shell_shield"),
    ender_eye_shield=lambda: eye_of_ender("ender_eye_shield"),
    blaze_shield=lambda: blaze("blaze_shield"),
    prismarine_shield=lambda: prismarine("prismarine_shield"),
    phantom_wing_shield=lambda: phantom_wing("phantom_wing_shield"),
    cobweb_shield=lambda: cobweb("cobweb_shield"),
    golden_apple_shield=lambda: golden_apple("golden_apple_shield"),
    bell_shield=lambda: bell("bell_shield"),
    lantern_shield=lambda: lantern("lantern_shield"),
    creeper_face=lambda: creeper("creeper_face"),
    slime_bouncer=lambda: slime("slime_bouncer"),
    cactus_buckler=lambda: cactus("cactus_buckler"),
    wither_skull_buckler=lambda: skull("wither_skull_buckler"),
    jack_o_shield=lambda: jack("jack_o_shield"),
    piston_ram=lambda: piston("piston_ram"),
    duelists_buckler=lambda: bossed("duelists_buckler", poly_mask([(16, 2), (28, 16), (16, 30), (4, 16)]), hexc("#6a2a3a"), hexc("#b8505e"), hexc("#f2d27a"), cross=hexc("#6a2a3a")),
    pilgrims_roundshield=lambda: bossed("pilgrims_roundshield", poly_mask([(10, 3), (22, 3), (28, 9), (28, 21), (22, 28), (10, 28), (4, 21), (4, 9)]), hexc("#7a6a4a"), hexc("#c9b88a"), hexc("#e8dcb0")),
    wellspring_bulwark=lambda: bossed("wellspring_bulwark", mask_of(circle(15.5, 19, 11)) | poly_mask([(16, 2), (5, 17), (27, 17)]), hexc("#2a6a6a"), hexc("#4fb5a8"), hexc("#d4fff4"), by=19),
    shieldmaidens_last_stand=lambda: bossed("shieldmaidens_last_stand", heart_shape(), hexc("#c99a2e"), hexc("#ece8dc"), hexc("#d94a5a"), cross=hexc("#c99a2e")),
    tavern_door=lambda: door("tavern_door", hexc("#a8733b"), hexc("#6b4521")),
    cast_iron_pan=lambda: pan("cast_iron_pan", hexc("#4a4d55"), hexc("#7b5a3a")),
    barrel_lid=lambda: lid("barrel_lid", hexc("#b08a54"), hexc("#5b5f66")),
)

# Usage: python3 tools/gen_textures.py [out_dir id ...]   (defaults: every shield drawn here, into the mod's textures)
if len(sys.argv) > 2:
    OUT, ids = sys.argv[1], sys.argv[2:]
else:
    OUT, ids = "src/main/resources/assets/shieldrpg/textures/item", list(P)
os.makedirs(OUT, exist_ok=True)
STYLES = dict(
    bastion_shield=("bricks", "top"), bannermans_standard=("cloth", "flat"), wicker_oval=("weave", "tl"), crusaders_cross=("plates", "tl"),
    spearpoint_shield=("plates", "top"), gatehouse_shield=("bricks", "top"), forge_twins_roundel=("hammered", "radial"),
    leaf_shield=("chevron", "tl"), sunburst_targe=("rays", "radial"), cathedral_aegis=("facets", "top"),
    standard_shield=("grain", "tl"),
    wooden_buckler=("grain", "tl"), iron_heater_shield=("plates", "top"), copper_roundel=("hammered", "radial"),
    skirmisher_pelta=("cloth", "flat"), thornback_scutum=("stitch", "tl"), frostguard_pavise=("facets", "top"),
    stormcaller_aspis=("hammered", "radial"), sunforged_bulwark=("rays", "radial"), wardens_greatshield=("plates", "tl"),
    vanguard_kite=("stripes", "flat"), beacon_aegis=("rings", "radial"), tidebreaker_buckler=("ripple", "bl"),
    emberhide_targe=("hide", "radial"), gravewatch_kite=("bricks", "top"), verdant_ward=("chevron", "tl"),
    windrunner_parma=("hgrain", "flat"), dragonscale_heater=("scales", "bl"), tavern_door=("grain", "top"),
    cast_iron_pan=("smooth", "radial"),
    duelists_buckler=("grain", "tl"), pilgrims_roundshield=("hgrain", "bl"), wellspring_bulwark=("ripple", "radial"),
    shieldmaidens_last_stand=("plates", "tl"),
    creeper_face=("hammered", "tl"), slime_bouncer=("smooth", "radial"), cactus_buckler=("stripes", "tl"),
    wither_skull_buckler=("hammered", "top"), jack_o_shield=("smooth", "radial"), piston_ram=("plates", "top"),
    pufferfish_buckler=("hammered", "radial"), turtle_shell_shield=("smooth", "top"), ender_eye_shield=("smooth", "radial"),
    blaze_shield=("plates", "tl"), prismarine_shield=("facets", "radial"), phantom_wing_shield=("cloth", "top"),
    cobweb_shield=("smooth", "flat"), golden_apple_shield=("smooth", "tl"), bell_shield=("hammered", "tl"), lantern_shield=("smooth", "flat"),
    ironhide_targe=("stitch", "radial"), duelists_parma=("chevron", "top"), wayfarers_roundshield=("cloth", "bl"),
    reservoir_bulwark=("rings", "top"), phoenix_heart_shield=("scales", "radial"), barrel_lid=("grain", "tl"),
)
for i in ids:
    STYLE[:] = STYLES.get(i, ("weave", "tl"))
    P[i]()
    if USE_EMBLEMS and i in EMBLEMS:
        face, cover_r, motif, extra = EMBLEMS[i]
        emblem_on(i, face, cover_r, motif, extra)
print(len(ids), "textures written to", OUT)
