"""Copy the design fonts into res/font using Android resource names.

Sources (never overwritten):
  design/fonts/Figtree-VariableFont_wght.ttf
  design/fonts/Figtree-Italic-VariableFont_wght.ttf
  design/fonts/DarkerGrotesque-VariableFont_wght.ttf

From the repo root:

  python scripts/copy_fonts.py
"""

import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "design" / "fonts"
DEST = ROOT / "app" / "src" / "main" / "res" / "font"

FILES = {
    "Figtree-VariableFont_wght.ttf": "figtree_variable.ttf",
    "Figtree-Italic-VariableFont_wght.ttf": "figtree_italic_variable.ttf",
    "DarkerGrotesque-VariableFont_wght.ttf": "darker_grotesque_variable.ttf",
}


def main() -> None:
    DEST.mkdir(parents=True, exist_ok=True)
    for source_name, dest_name in FILES.items():
        source = SOURCE / source_name
        if not source.is_file():
            raise SystemExit(f"missing {source}")
        shutil.copyfile(source, DEST / dest_name)
        print(f"{source_name} -> res/font/{dest_name}")


if __name__ == "__main__":
    main()
