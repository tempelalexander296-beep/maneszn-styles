"""Generates the mod icon (512x512). Run: python3 tools/gen_icon.py"""
import math
from PIL import Image, ImageDraw
S = 512
im = Image.new('RGBA', (S, S), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
for r in range(S // 2, 0, -1):
    t = r / (S / 2); d.ellipse([S / 2 - r, S / 2 - r, S / 2 + r, S / 2 + r], fill=(int(122 * t + 30), int(12 * t + 4), int(34 * t + 10), 255))
d.rounded_rectangle([120, 200, 392, 420], 18, fill=(200, 16, 46, 255))
d.rounded_rectangle([104, 160, 408, 222], 14, fill=(226, 30, 60, 255))
d.rectangle([236, 160, 276, 420], fill=(255, 211, 107, 255))
d.ellipse([166, 96, 256, 166], outline=(255, 211, 107, 255), width=18)
d.ellipse([256, 96, 346, 166], outline=(255, 211, 107, 255), width=18)
d.ellipse([236, 140, 276, 180], fill=(255, 231, 168, 255))
cx, cy, ro, ri = 256, 300, 46, 20
d.polygon([(cx + math.cos(i * math.pi / 5 - math.pi / 2) * (ro if i % 2 == 0 else ri), cy + math.sin(i * math.pi / 5 - math.pi / 2) * (ro if i % 2 == 0 else ri)) for i in range(10)], fill=(255, 246, 208, 255))
im.save('common/src/main/resources/assets/adventcosmetics/icon.png')
