"""Copies widget renders into the app as previews for the widget page and the launcher picker.

drawable-nodpi = English light; drawable-<lang>-nodpi / -night- variants for the rest.
"""
import os
from PIL import Image, ImageDraw

SRC = os.path.join("previews", "images")
RES = os.path.join("app", "src", "main", "res")
WIDGETS = ["today", "habit", "week", "streak", "quick", "mood"]
LANGS = {"en": "", "tr": "-tr", "es": "-es", "de": "-de", "pt": "-pt-rBR", "ja": "-ja",
         "fr": "-fr", "it": "-it", "ru": "-ru", "ko": "-ko"}

total = 0
for lang, qualifier in LANGS.items():
    for mode in ("light", "dark"):
        night = "-night" if mode == "dark" else ""
        folder = os.path.join(RES, f"drawable{qualifier}{night}-nodpi")
        os.makedirs(folder, exist_ok=True)
        for w in WIDGETS:
            im = Image.open(os.path.join(SRC, f"widget_{w}_{lang}_{mode}.png")).convert("RGBA")
            im = im.resize((im.width * 2 // 3, im.height * 2 // 3), Image.LANCZOS)
            # Round the corners like a launcher does (24dp at 2px per dp), transparent outside.
            mask = Image.new("L", im.size, 0)
            ImageDraw.Draw(mask).rounded_rectangle((0, 0, im.width - 1, im.height - 1), radius=48, fill=255)
            im.putalpha(Image.composite(im.getchannel("A"), mask, mask))
            out = os.path.join(folder, f"widget_preview_{w}.webp")
            im.save(out, "WEBP", quality=82, method=6)
            total += os.path.getsize(out)
print(f"{total // 1024} KB in total")
