"""Gera ícones launcher PNG (quadrado e redondo) em todas as densidades padrão."""
import os
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app" / "src" / "main" / "res"

DENSITIES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

BG = (178, 58, 107, 255)   # Rose primary #B23A6B
FG = (255, 255, 255, 255)  # White


def draw_bag(draw: ImageDraw.ImageDraw, size: int) -> None:
    """Desenha uma sacola (shopping bag) centralizada."""
    cx, cy = size / 2, size / 2
    bag_w = size * 0.46
    bag_h = size * 0.40
    left = cx - bag_w / 2
    top = cy - bag_h / 2 + size * 0.02
    right = cx + bag_w / 2
    bottom = cy + bag_h / 2 + size * 0.02
    radius = max(2, int(size * 0.04))
    # corpo
    draw.rounded_rectangle([left, top, right, bottom], radius=radius, fill=FG)
    # alça
    handle_w = size * 0.04
    handle_x_left = cx - bag_w * 0.30
    handle_x_right = cx + bag_w * 0.30
    handle_top = top - size * 0.13
    draw.arc(
        [handle_x_left, handle_top, handle_x_right, top + size * 0.05],
        start=180, end=360, fill=FG, width=int(handle_w),
    )


def make_square(size: int) -> Image.Image:
    img = Image.new("RGBA", (size, size), BG)
    draw_bag(ImageDraw.Draw(img), size)
    return img


def make_round(size: int) -> Image.Image:
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    draw.ellipse([0, 0, size - 1, size - 1], fill=BG)
    draw_bag(draw, size)
    return img


def main() -> None:
    for d, s in DENSITIES.items():
        out_dir = RES / f"mipmap-{d}"
        out_dir.mkdir(parents=True, exist_ok=True)
        make_square(s).save(out_dir / "ic_launcher.png", "PNG", optimize=True)
        make_round(s).save(out_dir / "ic_launcher_round.png", "PNG", optimize=True)
        print(f"  generated mipmap-{d}/ic_launcher.png ({s}x{s})")
    print("Done.")


if __name__ == "__main__":
    main()
