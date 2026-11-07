"""Adds or updates strings in all six languages at once.

Usage: python tools/translations/add_strings.py new_strings.json
The JSON maps a string name to {"en": ..., "tr": ..., "es": ..., "de": ..., "pt-BR": ..., "ja": ...}.
English and Turkish go straight into values/ and values-tr/; the other four go into
tools/translations/<lang>.txt and are rebuilt with build_strings.py.
"""
import json
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app" / "src" / "main" / "res"
sys.path.insert(0, str(Path(__file__).parent))
from build_strings import escape  # noqa: E402

TXT_LANGS = {"es": "values-es", "de": "values-de", "pt-BR": "values-pt-rBR", "ja": "values-ja"}


def put_xml(path: Path, name: str, text: str) -> None:
    xml = path.read_text(encoding="utf-8")
    line = f'    <string name="{name}">{escape(text)}</string>'
    pattern = re.compile(rf'^\s*<string name="{re.escape(name)}"[^>]*>.*?</string>\s*$', re.M | re.S)
    if pattern.search(xml):
        xml = pattern.sub(lambda _: line, xml, count=1)
    else:
        xml = xml.replace("</resources>", line + "\n</resources>")
    path.write_text(xml, encoding="utf-8")


def put_txt(path: Path, name: str, text: str) -> None:
    lines = path.read_text(encoding="utf-8").splitlines()
    entry = f"{name}|{text}"
    for i, raw in enumerate(lines):
        if raw.partition("|")[0].strip() == name:
            lines[i] = entry
            break
    else:
        lines.append(entry)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> None:
    data = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
    for name, texts in data.items():
        missing = {"en", "tr", *TXT_LANGS} - texts.keys()
        if missing:
            raise SystemExit(f"{name}: missing {sorted(missing)}")
        put_xml(RES / "values" / "strings.xml", name, texts["en"])
        put_xml(RES / "values-tr" / "strings.xml", name, texts["tr"])
        for lang in TXT_LANGS:
            put_txt(ROOT / "tools" / "translations" / f"{lang}.txt", name, texts[lang])
    for lang, folder in TXT_LANGS.items():
        subprocess.run([sys.executable, str(Path(__file__).parent / "build_strings.py"), lang, folder], check=True)
    print(f"{len(data)} strings updated in 6 languages")


if __name__ == "__main__":
    main()
