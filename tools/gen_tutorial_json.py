# -*- coding: utf-8 -*-
"""Генерация tutorial/ru_ru.json и en_us.json из подробных текстов уроков."""
import json
import sys
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
sys.path.insert(0, str(TOOLS))

from tutorial_bodies_en import BODIES as BODIES_EN, SEARCH as SEARCH_EN
from tutorial_bodies_ru import BODIES as BODIES_RU, SEARCH as SEARCH_RU

OUT = TOOLS.parent / "src/main/resources/assets/redstone-master/tutorial"
SRC = (
    "Источники: Minecraft Wiki — «Руководство:Редстоуновые схемы» "
    "(https://ru.minecraft.wiki/w/Руководство:Редстоуновые_схемы), страницы механик блоков; "
    "обобщение Redstone Master."
)
SRC_EN = (
    "Sources: Minecraft Wiki — Redstone circuits "
    "(https://minecraft.wiki/w/Redstone_circuits), block mechanic pages; summarized by Redstone Master."
)

LESSON_EXTRA_IMAGES = {
    "carry_signal": [
        "textures/tutorial/carry_signal_lever.png",
        "textures/tutorial/carry_signal_dust.png",
        "textures/tutorial/carry_signal_lamp.png",
    ],
    "power_sources": [
        "textures/tutorial/power_sources.png",
        "textures/tutorial/power_sources_button.png",
        "textures/tutorial/power_sources_plate.png",
        "textures/tutorial/power_sources_block.png",
    ],
}


def img_lesson(lid):
    if lid in LESSON_EXTRA_IMAGES:
        return LESSON_EXTRA_IMAGES[lid]
    return [f"textures/tutorial/{lid}.png"]


def img_section(sid):
    return [f"textures/tutorial/sections/{sid}.png"]


def les(lid, title, body, search):
    return {"id": lid, "title": title, "body": body, "search": search, "images": img_lesson(lid)}


def les_ru(lid, title):
    return les(lid, title, BODIES_RU[lid], SEARCH_RU.get(lid, ""))


def les_en(lid, title, search_fallback=""):
    return les(lid, title, BODIES_EN[lid], SEARCH_EN.get(lid, search_fallback))


def sec(sid, title, summary, search, lessons):
    return {
        "id": sid,
        "title": title,
        "summary": summary,
        "search": search,
        "sources": SRC,
        "images": img_section(sid),
        "lessons": lessons,
    }


def sec_en(sid, title, summary, search, lessons):
    return {
        "id": sid,
        "title": title,
        "summary": summary,
        "search": search,
        "sources": SRC_EN,
        "images": img_section(sid),
        "lessons": lessons,
    }


RU = [
    sec(
        "redstone_signal",
        "Редстоун сигнал",
        "Сигнал 0–15, источники, пыль, горизонталь и вертикаль. Пройдите уроки по порядку.",
        "редстоун сигнал пыль",
        [
            les_ru("carry_signal", "Довести редстоун сигнал"),
            les_ru("power_sources", "Изучить активирующие блоки"),
            les_ru("horizontal_wire", "Провести горизонтальный сигнал"),
            les_ru("vertical_wire", "Провести вертикальный сигнал"),
        ],
    ),
    sec(
        "repeater",
        "Редстоун повторитель",
        "Восстанавливает силу 15 и добавляет задержку 1–4 тика.",
        "повторитель",
        [les_ru("repeater_extend", "Продолжить редстоун сигнал повторителем")],
    ),
    sec(
        "redstone_torch",
        "Редстоун факел",
        "Инверсия сигнала и релейная передача с задержкой.",
        "факел",
        [
            les_ru("torch_invert", "Инвертировать редстоун сигнал с помощью редстоун факела"),
            les_ru("torch_chain", "Продолжить сигнал цепочкой факелов"),
        ],
    ),
    sec(
        "comparator",
        "Компаратор",
        "Сравнение сигналов, чтение сундуков, вычитание, кафедра.",
        "компаратор",
        [
            les_ru("comparator_chest", "Высчитать силу сигнала в сундуке"),
            les_ru("comparator_subtract", "Создать замкнутый сигнал с помощью компаратора"),
            les_ru("comparator_lectern", "Поменять силу сигнала кафедрой и книгой"),
        ],
    ),
    sec(
        "piston",
        "Поршень",
        "Толкание и втягивание блоков.",
        "поршень",
        [
            les_ru("piston_push", "Сдвинуть блок с помощью поршня"),
            les_ru("piston_sticky", "Притянуть блок липким поршнем"),
        ],
    ),
    sec(
        "observer",
        "Наблюдатель",
        "Детектор обновления блока.",
        "наблюдатель",
        [les_ru("observer_detect", "Отследить изменение состояния наблюдателем")],
    ),
    sec(
        "minecart",
        "Вагонетка",
        "Рельсы, виды вагонеток, активирующие рельсы.",
        "вагонетка рельсы",
        [
            les_ru("minecart_start", "Пустить вагонетку по рельсам"),
            les_ru("minecart_hopper", "Ферма с загрузочной вагонеткой"),
            les_ru("minecart_activator", "Активировать сигнал активирующей рельсой"),
            les_ru("minecart_furnace", "Проехаться на вагонетке с печкой"),
            les_ru("minecart_stack", "20 вагонеток на одной рельсе"),
        ],
    ),
    sec(
        "hopper",
        "Воронка",
        "Перемещение предметов и таймеры.",
        "воронка",
        [
            les_ru("hopper_insert", "Скинуть предмет в воронку"),
            les_ru("hopper_timer", "Создать таймер с воронками"),
        ],
    ),
    sec(
        "daylight_detector",
        "Датчик света",
        "Сигнал от уровня освещения неба.",
        "датчик света",
        [les_ru("daylight_wait", "Дождаться сигнала датчика")],
    ),
    sec(
        "target",
        "Мишень",
        "Сигнал от попадания.",
        "мишень",
        [les_ru("target_hit", "Стрельнуть в мишень")],
    ),
    sec(
        "sculk_sensor",
        "Скалк-сенсор",
        "Вибрации и калибровка.",
        "скалк",
        [
            les_ru("sculk_sound", "Создать звук рядом со скалк-сенсором"),
            les_ru("sculk_armor", "Снять броню у откалиброванного сенсора"),
        ],
    ),
    sec(
        "dropper_dispenser",
        "Выбрасыватель и раздатчик",
        "Выброс и активация предметов.",
        "выбрасыватель раздатчик",
        [
            les_ru("dropper_drop", "Выбросить предмет"),
            les_ru("dispenser_activate", "Активировать предмет раздатчиком"),
        ],
    ),
    sec(
        "crafter",
        "Автокрафтер",
        "Автоматический крафт по сетке.",
        "автокрафтер",
        [
            les_ru("crafter_gold", "Автокрафтер золотого слитка"),
            les_ru("crafter_cake", "Автокрафтер тортов"),
        ],
    ),
]

EN_TITLES = {
    "redstone_signal": ("Redstone signal", "Signals, dust, horizontal and vertical lines."),
    "repeater": ("Redstone repeater", "Boost to 15 and delay."),
    "redstone_torch": ("Redstone torch", "Inversion and chains."),
    "comparator": ("Comparator", "Compare, containers, subtract."),
    "piston": ("Piston", "Push and sticky pull."),
    "observer": ("Observer", "Block update detector."),
    "minecart": ("Minecart", "Rails and cart types."),
    "hopper": ("Hopper", "Item transport and timers."),
    "daylight_detector": ("Daylight detector", "Sky light sensor."),
    "target": ("Target block", "Projectile hits."),
    "sculk_sensor": ("Sculk sensor", "Vibration detection."),
    "dropper_dispenser": ("Dropper and dispenser", "Drop vs use items."),
    "crafter": ("Crafter", "Automated crafting."),
}

EN_LESSON_TITLES = {
    "carry_signal": "Carry a redstone signal",
    "power_sources": "Learn power sources",
    "horizontal_wire": "Horizontal routing",
    "vertical_wire": "Vertical routing",
    "repeater_extend": "Extend with repeater",
    "torch_invert": "Invert with torch",
    "torch_chain": "Torch chain",
    "comparator_chest": "Chest signal strength",
    "comparator_subtract": "Subtract mode",
    "comparator_lectern": "Lectern pages",
    "piston_push": "Push with piston",
    "piston_sticky": "Sticky pull",
    "observer_detect": "Observer detect",
    "minecart_start": "Launch minecart",
    "minecart_hopper": "Hopper minecart farm",
    "minecart_activator": "Activator rail",
    "minecart_furnace": "Furnace minecart",
    "minecart_stack": "20-cart train",
    "hopper_insert": "Insert into hopper",
    "hopper_timer": "Hopper timer",
    "daylight_wait": "Daylight sensor",
    "target_hit": "Shoot target",
    "sculk_sound": "Sculk vibration",
    "sculk_armor": "Calibrated sculk",
    "dropper_drop": "Drop item",
    "dispenser_activate": "Dispense item",
    "crafter_gold": "Craft gold ingot",
    "crafter_cake": "Craft cake",
}

EN = []
for s in RU:
    tid, tsum = EN_TITLES[s["id"]]
    lessons = []
    for L in s["lessons"]:
        lid = L["id"]
        lessons.append(les_en(lid, EN_LESSON_TITLES[lid], L["search"]))
    EN.append(sec_en(s["id"], tid, tsum, s["search"], lessons))


def _check_coverage():
    ids = {L["id"] for s in RU for L in s["lessons"]}
    for lid in ids:
        assert lid in BODIES_RU, f"missing RU body: {lid}"
        assert lid in BODIES_EN, f"missing EN body: {lid}"


if __name__ == "__main__":
    _check_coverage()
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "ru_ru.json").write_text(
        json.dumps({"sections": RU}, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    (OUT / "en_us.json").write_text(
        json.dumps({"sections": EN}, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    lens = [len(L["body"]) for s in RU for L in s["lessons"]]
    print("written", len(RU), "sections,", len(lens), "lessons")
    print("RU body chars: min", min(lens), "max", max(lens), "avg", sum(lens) // len(lens))
