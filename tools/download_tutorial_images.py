# -*- coding: utf-8 -*-
"""
Скачивает учебные изображения с Minecraft Wiki (CC BY-SA) для уроков мода.
Запуск: python tools/download_tutorial_images.py
"""
from __future__ import annotations

import json
import re
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/redstone-master/textures/tutorial"
API = "https://minecraft.wiki/api.php"

# Файлы на Minecraft Wiki (JE / актуальные спрайты и схемы)
LESSON_FILES = {
    "carry_signal": "Redstone_Dust_JE7_BE3.png",
    "power_sources": "Lever.png",
    "horizontal_wire": "Redstone_Dust_JE7_BE3.png",
    "vertical_wire": "Redstone_Torch_JE7_BE3.png",
    "repeater_extend": "Redstone_Repeater_JE7_BE3.png",
    "torch_invert": "Redstone_Torch_JE7_BE3.png",
    "torch_chain": "Redstone_Torch_JE7_BE3.png",
    "comparator_chest": "Redstone_Comparator.png",
    "comparator_subtract": "Redstone_Comparator.png",
    "comparator_lectern": "Lectern_JE7_BE3.png",
    "piston_push": "Piston_JE7_BE3.png",
    "piston_sticky": "Sticky_Piston_JE7_BE3.png",
    "observer_detect": "Observer_JE7_BE3.png",
    "minecart_start": "Minecart_JE7_BE3.png",
    "minecart_hopper": "Minecart_with_Hopper_JE7_BE3.png",
    "minecart_activator": "Activator_Rail_JE7_BE3.png",
    "minecart_furnace": "Minecart_with_Furnace_JE7_BE3.png",
    "minecart_stack": "Rail_JE7_BE3.png",
    "hopper_insert": "Hopper_JE7_BE3.png",
    "hopper_timer": "Hopper_JE7_BE3.png",
    "daylight_wait": "Daylight_Detector_JE7_BE3.png",
    "target_hit": "Target_JE7_BE3.png",
    "sculk_sound": "Sculk_Sensor_JE7_BE3.png",
    "sculk_armor": "Calibrated_Sculk_Sensor_JE7_BE3.png",
    "dropper_drop": "Dropper_JE7_BE3.png",
    "dispenser_activate": "Dispenser_JE7_BE3.png",
    "crafter_gold": "Crafter_JE7_BE3.png",
    "crafter_cake": "Crafter_JE7_BE3.png",
}

# Схемы / скриншоты механик (лучше для обучения, чем иконка предмета)
LESSON_CIRCUIT_FILES = {
    "carry_signal": "Redstone_circuit.png",
    "horizontal_wire": "Redstone_circuit.png",
    "vertical_wire": "Torch_vertical_transmission.png",
    "repeater_extend": "Redstone_circuit.png",
    "torch_invert": "NOT_gate.png",
    "comparator_chest": "Chest_item_output.png",
    "hopper_timer": "Hopper_clock.png",
    "observer_detect": "Observer_output.png",
}

SECTION_FILES = {
    "redstone_signal": "Redstone_Dust_JE7_BE3.png",
    "repeater": "Redstone_Repeater_JE7_BE3.png",
    "redstone_torch": "Redstone_Torch_JE7_BE3.png",
    "comparator": "Redstone_Comparator.png",
    "piston": "Piston_JE7_BE3.png",
    "observer": "Observer_JE7_BE3.png",
    "minecart": "Rail_JE7_BE3.png",
    "hopper": "Hopper_JE7_BE3.png",
    "daylight_detector": "Daylight_Detector_JE7_BE3.png",
    "target": "Target_JE7_BE3.png",
    "sculk_sensor": "Sculk_Sensor_JE7_BE3.png",
    "dropper_dispenser": "Dropper_JE7_BE3.png",
    "crafter": "Crafter_JE7_BE3.png",
}


def wiki_image_url(file_title: str, width: int = 0) -> str | None:
    if not file_title.startswith("File:"):
        file_title = "File:" + file_title
    params = {
        "action": "query",
        "titles": file_title,
        "prop": "imageinfo",
        "iiprop": "url",
        "format": "json",
    }
    if width > 0:
        params["iiurlwidth"] = str(width)
    url = API + "?" + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers={"User-Agent": "RedstoneMaster/1.0 (educational mod)"})
    with urllib.request.urlopen(req, timeout=30) as resp:
        data = json.loads(resp.read().decode())
    pages = data.get("query", {}).get("pages", {})
    for page in pages.values():
        if "missing" in page:
            continue
        infos = page.get("imageinfo", [])
        if infos:
            return infos[0].get("thumburl") if width > 0 and "thumburl" in infos[0] else infos[0].get("url")
    return None


def download_png(url: str, dest: Path) -> bool:
    req = urllib.request.Request(url, headers={"User-Agent": "RedstoneMaster/1.0 (educational mod)"})
    with urllib.request.urlopen(req, timeout=60) as resp:
        data = resp.read()
    if len(data) < 200 or not data.startswith(b"\x89PNG"):
        return False
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_bytes(data)
    return True


def resolve_file(lesson_id: str) -> str:
    if lesson_id in LESSON_CIRCUIT_FILES:
        return LESSON_CIRCUIT_FILES[lesson_id]
    return LESSON_FILES[lesson_id]


def try_download(file_name: str, dest: Path, width: int = 320) -> bool:
    for name in [file_name, file_name.replace("_JE7_BE3", "_JE6_BE3"), file_name.replace("_JE7_BE3", "_JE5_BE3")]:
        url = wiki_image_url("File:" + name, width)
        if url and download_png(url, dest):
            print("  OK", dest.name, "<-", name)
            return True
    # без суффикса версии
    base = re.sub(r"_JE\d+_BE\d+", "", file_name)
    url = wiki_image_url("File:" + base, width)
    if url and download_png(url, dest):
        print("  OK", dest.name, "<-", base)
        return True
    print("  FAIL", dest.name, file_name)
    return False


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "sections").mkdir(exist_ok=True)
    ok = 0
    fail = 0

    print("Lessons:")
    for lesson_id, file_name in LESSON_FILES.items():
        prefer = resolve_file(lesson_id)
        dest = OUT / f"{lesson_id}.png"
        if try_download(prefer, dest, 360) or try_download(file_name, dest, 360):
            ok += 1
        else:
            fail += 1

    print("Sections:")
    for section_id, file_name in SECTION_FILES.items():
        dest = OUT / "sections" / f"{section_id}.png"
        if try_download(file_name, dest, 480):
            ok += 1
        else:
            fail += 1

    print(f"Done: {ok} ok, {fail} failed")


if __name__ == "__main__":
    main()
