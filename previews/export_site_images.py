"""Crops preview renders into the website's per-language images (docs/img/<lang>/)."""
import os
from PIL import Image

SRC = os.path.join("previews", "images")
DST = os.path.join("docs", "img")
LANGS = ["en", "tr", "es", "de", "pt", "ja", "fr", "it", "ru", "ko"]
PALETTES = ["washi", "sumi", "matcha", "sakura", "umi"]

TILES = {
    "g_heatmap": ("detail_{l}_light", (40, 555, 1193, 1505)),
    "g_rate": ("progress_{l}_light", (40, 590, 1193, 1300)),
    "g_breakdown": ("progress_{l}_light", (40, 1310, 1193, 2190)),
    "g_editor": ("editor_{l}_light", (40, 260, 1193, 1600)),
}
PHONE = (0, 0, 1233, 1830)
WIDGET_PHONE = (60, 320, 1173, 1700)


def progress_card_top(im: Image.Image) -> int:
    """Top edge of Today's progress card. The greeting above it takes one or two lines
    depending on language and time of day, so the habit rows below move with it."""
    bg = im.getpixel((520, 640))
    for y in range(640, 1400):
        px = im.getpixel((520, y))
        if sum(abs(a - b) for a, b in zip(px, bg)) > 12:
            # Skip thin things (ring strokes): the card is a tall block of one colour.
            if all(sum(abs(a - b) for a, b in zip(im.getpixel((520, y + d)), px)) < 8 for d in range(12, 40, 4)):
                return y
    raise ValueError("progress card not found")


def save(im, box, width, out):
    im = im.crop(box)
    im = im.resize((width, round(im.height * width / im.width)), Image.LANCZOS)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    im.save(out, "WEBP", quality=90, method=6)


def load(name):
    return Image.open(os.path.join(SRC, name + ".png")).convert("RGB")


for l in LANGS:
    folder = os.path.join(DST, l)
    for mode in ("light", "dark"):
        save(load(f"today_{l}_{mode}"), PHONE, 900, os.path.join(folder, f"hero_{mode}.webp"))
    for name, (src, box) in TILES.items():
        save(load(src.format(l=l)), box, 760, os.path.join(folder, name + ".webp"))
    today = load(f"today_{l}_light")
    top = progress_card_top(today)
    save(today, (40, top + 345, 1193, top + 1495), 760, os.path.join(folder, "g_today.webp"))
    save(load(f"site_widgets_{l}"), WIDGET_PHONE, 760, os.path.join(folder, "widgets.webp"))
    suffix = "" if l == "tr" else "_" + l
    for p in PALETTES:
        for mode in ("light", "dark"):
            save(load(f"palette_{p}_{mode}{suffix}"), PHONE, 900, os.path.join(folder, f"pal_{p}_{mode}.webp"))
    print(l, "done, card top", top)
