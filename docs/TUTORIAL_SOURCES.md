# Источники учебного контента (вкладка «Обучение»)

Тексты уроков в `assets/redstone-master/tutorial/ru_ru.json` и `en_us.json` подготовлены для мода **Redstone Master** на основе открытых материалов Minecraft Wiki и обобщены автором ВКР. Подробные версии (≈10× объём базовых описаний) хранятся в `tools/tutorial_bodies_ru.py` и `tools/tutorial_bodies_en.py`; пересборка JSON: `python tools/gen_tutorial_json.py`. Ниже — основные страницы, на которые опирается структура разделов.

## Главный обзор

- [Руководство:Редстоуновые схемы](https://ru.minecraft.wiki/w/Руководство:Редстоуновые_схемы) — общая терминология, типы схем, передача сигнала.
- [Redstone circuits](https://minecraft.wiki/w/Redstone_circuits) — англоязычный аналог (механики силы сигнала 0–15, проводка).

## По разделам мода

| Раздел мода | Страницы Minecraft Wiki (RU) |
|-------------|-------------------------------|
| Редстоун сигнал | [Красная пыль](https://ru.minecraft.wiki/w/Красная_пыль), [Сигнализация](https://ru.minecraft.wiki/w/Сигнализация), [Питание](https://ru.minecraft.wiki/w/Питание) |
| Редстоун повторитель | [Повторитель](https://ru.minecraft.wiki/w/Повторитель) |
| Редстоун факел | [Редстоуновый факел](https://ru.minecraft.wiki/w/Редстоуновый_факел) |
| Компаратор | [Редстоуновый компаратор](https://ru.minecraft.wiki/w/Редстоуновый_компаратор) |
| Поршень | [Поршень](https://ru.minecraft.wiki/w/Поршень), [Липкий поршень](https://ru.minecraft.wiki/w/Липкий_поршень) |
| Наблюдатель | [Наблюдатель](https://ru.minecraft.wiki/w/Наблюдатель) |
| Вагонетка | [Рельсы](https://ru.minecraft.wiki/w/Рельсы), [Вагонетка](https://ru.minecraft.wiki/w/Вагонетка), [Активирующие рельсы](https://ru.minecraft.wiki/w/Активирующие_рельсы) |
| Воронка | [Воронка](https://ru.minecraft.wiki/w/Воронка) |
| Датчик света | [Датчик дневного света](https://ru.minecraft.wiki/w/Датчик_дневного_света) |
| Мишень | [Мишень](https://ru.minecraft.wiki/w/Мишень) |
| Скалк-сенсор | [Скалк-сенсор](https://ru.minecraft.wiki/w/Скалк-сенсор) |
| Выбрасыватель и раздатчик | [Выбрасыватель](https://ru.minecraft.wiki/w/Выбрасыватель), [Раздатчик](https://ru.minecraft.wiki/w/Раздатчик) |
| Автокрафтер | [Автокрафтер](https://ru.minecraft.wiki/w/Автокрафтер) |

## Иллюстрации в уроках

Изображения в `assets/redstone-master/textures/tutorial/` загружаются с [Minecraft Wiki](https://minecraft.wiki/) (лицензия CC BY-SA 3.0): спрайты блоков и предметов, схемы. Скрипт обновления: `tools/download_tutorial_images.py`.

## Примечание

Формулировки в моде упрощены для обучения в игре и не заменяют полную wiki-статью. При расхождении с вашей версией Minecraft ориентируйтесь на актуальную wiki и патч-ноты.
