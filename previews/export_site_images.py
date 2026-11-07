"""Crops preview renders into the website's per-language images (docs/img/<lang>/)."""
import os
from PIL import Image

SRC = os.path.join("previews", "images")
DST = os.path.join("docs", "img")
LANGS = ["en", "tr", "es", "de", "pt", "ja"]
PALETTES = ["washi", "sumi", "matcha", "sakura", "umi"]

TILES = {
    "g_heatmap": ("detail_{l}_light", (40, 540, 1193, 1500)),
    "g_rate": ("progress_{l}_light", (40, 590, 1193, 1275)),
    "g_breakdown": ("progress_{l}_light", (40, 1310, 1193, 2190)),
    "g_today": ("today_{l}_light", (40, 1140, 1193, 2290)),
    "g_editor": ("editor_{l}_light", (40, 700, 1193, 1530)),
}
PHONE = (0, 0, 1233, 1830)
# The Today greeting fits on one line in these languages, so the rows sit 120px higher.
ONE_LINE_GREETING = {"tr", "es", "pt"}


def save(src, box, width, out):
    im = Image.open(os.path.join(SRC, src + ".png")).convert("RGB").crop(box)
    im = im.resize((width, round(im.height * width / im.width)), Image.LANCZOS)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    im.save(out, "WEBP", quality=90, method=6)


for l in LANGS:
    folder = os.path.join(DST, l)
    for mode in ("light", "dark"):
        save(f"today_{l}_{mode}", PHONE, 900, os.path.join(folder, f"hero_{mode}.webp"))
    for name, (src, box) in TILES.items():
        if name == "g_today" and l in ONE_LINE_GREETING:
            box = (box[0], box[1] - 120, box[2], box[3] - 120)
        save(src.format(l=l), box, 760, os.path.join(folder, name + ".webp"))
    suffix = "" if l == "tr" else "_" + l
    for p in PALETTES:
        for mode in ("light", "dark"):
            save(f"palette_{p}_{mode}{suffix}", PHONE, 900, os.path.join(folder, f"pal_{p}_{mode}.webp"))
    print(l, "done")
