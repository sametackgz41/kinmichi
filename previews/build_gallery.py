"""Turn rendered previews into web-ready gallery assets.

1. Run the renderer:  ./gradlew :app:testDebugUnitTest -Ppreviews --tests "app.tsuzuku.previews.*"
2. Then:             python previews/build_gallery.py

Reads previews/images/*.png and writes previews/site/assets/: screens downscaled to phone
width, widgets kept sharp, and animation frames stitched into looping GIFs.
"""
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent
SRC = ROOT / "images"
OUT = ROOT / "site" / "assets"
SCREEN_WIDTH = 540
WIDGET_SCALE = 0.5  # renders are xxhdpi (3x); 1.5x keeps widgets crisp on retina screens


def save_webp(image: Image.Image, path: Path) -> None:
    image.save(path.with_suffix(".webp"), "WEBP", quality=88, method=6)


RES = ROOT.parent / "app" / "src" / "main" / "res"


def export_widget_previews() -> None:
    """Widget-picker previews (android:previewImage): light renders with rounded corners."""
    for lang, folder in (("en", "drawable-nodpi"), ("tr", "drawable-tr-nodpi")):
        (RES / folder).mkdir(exist_ok=True)
        for kind in ("today", "habit", "week"):
            image = Image.open(SRC / f"widget_{kind}_{lang}_light.png").convert("RGBA")
            image = image.resize((image.width // 2, image.height // 2), Image.LANCZOS)
            mask = Image.new("L", image.size, 0)
            ImageDraw.Draw(mask).rounded_rectangle((0, 0, *image.size), radius=36, fill=255)
            image.putalpha(mask)
            image.save(RES / folder / f"widget_preview_{kind}.webp", "WEBP", quality=90, method=6)


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for old in OUT.iterdir():
        old.unlink()

    for png in sorted(SRC.glob("*.png")):
        if png.stem.startswith("anim_"):
            continue
        image = Image.open(png).convert("RGB")
        if png.stem.startswith("widget_"):
            size = (int(image.width * WIDGET_SCALE), int(image.height * WIDGET_SCALE))
        else:
            size = (SCREEN_WIDTH, round(image.height * SCREEN_WIDTH / image.width))
        save_webp(image.resize(size, Image.LANCZOS), OUT / png.name)

    variants = sorted({"_".join(p.stem.split("_")[1:3]) for p in SRC.glob("anim_*.png")})
    for variant in variants:
        frames = [Image.open(p).convert("RGB") for p in sorted(SRC.glob(f"anim_{variant}_*.png"))]
        # The renderer draws the whole window; keep only the rows that hold the animated content.
        box = frames[-1].convert("L").point(lambda v: 255 if v > 12 else 0).getbbox()
        frames = [f.crop(box) for f in frames]
        width = 480
        frames = [f.resize((width, round(f.height * width / f.width)), Image.LANCZOS) for f in frames]
        # Short frames during the motion, a long hold at both ends so the loop reads clearly.
        durations = [600] + [100] * 3 + [40] * 28 + [150] * 5 + [1600]
        durations = (durations + [100] * len(frames))[: len(frames)]
        frames[0].save(
            OUT / f"anim_{variant}.gif",
            save_all=True,
            append_images=frames[1:],
            duration=durations,
            loop=0,
            optimize=True,
        )
    export_widget_previews()
    total = sum(p.stat().st_size for p in OUT.iterdir())
    print(f"{len(list(OUT.iterdir()))} assets, {total / 1024:.0f} KB")


if __name__ == "__main__":
    main()
