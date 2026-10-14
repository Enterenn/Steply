"""Generate the adaptive launcher icon from the 1024 px design sources.

Sources (never overwritten):
  design/icon/steply-icon-background-1024.png
  design/icon/steply-icon-foreground-1024.png

The background may include transparent padding; only its opaque square is scaled.
The foreground is a white glyph on transparency and is also the monochrome layer
(Android 13+ themed icons use that alpha mask).

Requires Pillow. From the repo root:

  python scripts/generate_launcher_icons.py
"""

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ICON_DIR = ROOT / "design" / "icon"
RES = ROOT / "app" / "src" / "main" / "res"

BACKGROUND_SRC = ICON_DIR / "steply-icon-background-1024.png"
FOREGROUND_SRC = ICON_DIR / "steply-icon-foreground-1024.png"

# Adaptive-icon layers are 108 dp.
DENSITIES = {
    "mdpi": 108,
    "hdpi": 162,
    "xhdpi": 216,
    "xxhdpi": 324,
    "xxxhdpi": 432,
}

ADAPTIVE_XML = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@mipmap/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
    <monochrome android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>
"""


def opaque_bbox(image: Image.Image) -> tuple[int, int, int, int]:
    # Ignore the near-zero alpha fringe some exports leave under the artwork.
    alpha = image.convert("RGBA").getchannel("A")
    bbox = alpha.point(lambda value: 255 if value > 16 else 0).getbbox()
    if bbox is None:
        raise SystemExit("background has no opaque pixels")
    return bbox


def load_background(path: Path) -> Image.Image:
    image = Image.open(path).convert("RGBA")
    left, top, right, bottom = opaque_bbox(image)
    width = right - left
    height = bottom - top
    if width != height:
        raise SystemExit(f"{path.name}: opaque area is {width}x{height}, expected a square")
    return image.crop((left, top, right, bottom))


def load_foreground(path: Path) -> Image.Image:
    """Keep the transparent margin: it places the glyph inside the adaptive safe zone."""
    image = Image.open(path).convert("RGBA")
    width, height = image.size
    if width != height:
        raise SystemExit(f"{path.name}: canvas is {width}x{height}, expected a square")
    return image


def write_layers(name: str, image: Image.Image) -> None:
    for density, size in DENSITIES.items():
        folder = RES / f"mipmap-{density}"
        folder.mkdir(parents=True, exist_ok=True)
        image.resize((size, size), Image.Resampling.LANCZOS).save(
            folder / f"{name}.png",
            format="PNG",
            optimize=True,
        )


def main() -> None:
    background = load_background(BACKGROUND_SRC)
    foreground = load_foreground(FOREGROUND_SRC)
    write_layers("ic_launcher_background", background)
    write_layers("ic_launcher_foreground", foreground)

    anydpi = RES / "mipmap-anydpi-v26"
    anydpi.mkdir(parents=True, exist_ok=True)
    (anydpi / "ic_launcher.xml").write_text(ADAPTIVE_XML, encoding="utf-8", newline="\n")
    (anydpi / "ic_launcher_round.xml").write_text(ADAPTIVE_XML, encoding="utf-8", newline="\n")
    print(f"Wrote launcher layers for {', '.join(DENSITIES)} and mipmap-anydpi-v26 XML")


if __name__ == "__main__":
    main()
