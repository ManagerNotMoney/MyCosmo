<div align="center">

```
███╗   ███╗██╗   ██╗ ██████╗ ██████╗ ███████╗███╗   ███╗ ██████╗ 
████╗ ████║╚██╗ ██╔╝██╔════╝██╔═══██╗██╔════╝████╗ ████║██╔═══██╗
██╔████╔██║ ╚████╔╝ ██║     ██║   ██║███████╗██╔████╔██║██║   ██║
██║╚██╔╝██║  ╚██╔╝  ██║     ██║   ██║╚════██║██║╚██╔╝██║██║   ██║
██║ ╚═╝ ██║   ██║   ╚██████╗╚██████╔╝███████║██║ ╚═╝ ██║╚██████╔╝
╚═╝     ╚═╝   ╚═╝    ╚═════╝ ╚═════╝ ╚══════╝╚═╝     ╚═╝ ╚═════╝ 
```

**Catch the signal. Record the data. Sell the truth.**

*Inspired by [Voices of the Void](https://mrdrnose.itch.io/votv)*

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.9%2B-brightgreen?style=flat-square&logo=minecraft)
![Java](https://img.shields.io/badge/Java-17%2B-orange?style=flat-square&logo=openjdk)
![Vault](https://img.shields.io/badge/Requires-Vault-blue?style=flat-square)
![Status](https://img.shields.io/badge/Status-Active%20development-yellow?style=flat-square)

**[🇷🇺 Читать на русском](#-русская-версия)** · **[🇬🇧 Read in English](#-english-version)**

</div>

---

# 🇬🇧 English version

## 📡 About

**MyCosmo** is a Minecraft plugin that turns your server into a signal intelligence station for the unknown.  
Build a radio telescope, point an antenna at the sky, capture an intercepted signal onto a diskette — then sell it for coins through the cashier.

> *Something out there is transmitting. Your job is to listen.*

The plugin is **under active development**: new signal sources, encryption mechanics, structures, and more are on the way.

## 🧱 Compatibility

Built and tested on **Minecraft 1.21.11** (Paper/Spigot, Java 17+).

Minimum supported version is **1.21.9**. The telescope structure uses `WAXED_LIGHTNING_ROD` and `WAXED_COPPER_BARS`, introduced in **"The Copper Age"** update (Java Edition 1.21.9, released September 30, 2025). On versions **1.21.0–1.21.8** these blocks don't exist yet, so the plugin will not work.

## 🔧 Dependencies

| Plugin | Required? |
|--------|-----------|
| **[Vault](https://www.spigotmc.org/resources/vault.34315/)** | ✅ Yes |
| Any economy plugin (EssentialsX etc.) | ✅ Yes |

## 🏗️ Structures

### 📻 Radio Telescope

The core structure for catching signals. Built bottom-up as a column:

```
        [REDSTONE_TORCH]       ← block 5 (top)
       [WAXED_LIGHTNING_ROD]   ← block 4
        [WAXED_COPPER_BARS]    ← block 3
         [REDSTONE_BLOCK]      ← block 2
   [BLUE_GLAZED_TERRACOTTA]    ← block 1 (base)
```

**Important:** the sky above the telescope must be clear — no blocks above the top up to the world ceiling.

**Controls:**
- **Right-click the base** (`BLUE_GLAZED_TERRACOTTA`) — open the telescope settings GUI
- **Right-click** `REDSTONE_BLOCK` **holding paper** — write the telescope's ID onto the paper (for the observatory)
- **Right-click** `REDSTONE_BLOCK` **empty-handed** — start scanning the sky

> Scanning takes time. Once finished, the signal stays active for **5 minutes** — record it onto a diskette before it expires!

### 🔭 Observatory

Lets you manage several telescopes from one place. Holds papers with telescope IDs.

```
   [IRON_BLOCK] [IRON_BLOCK] [IRON_BLOCK]   ← top row (3 blocks)
  [REDSTONE_BLOCK]   [*]   [REDSTONE_BLOCK] ← middle row
            [LIGHT_BLUE_GLAZED_TERRACOTTA]   ← base (*)
```

**Orientation:** the structure can face either the **X** or **Z** axis.

**Controls:**
- **Right-click the base** (`LIGHT_BLUE_GLAZED_TERRACOTTA`) — open the observatory GUI
- Place telescope ID papers in slots 4, 5, 6
- **Right-click a paper inside the GUI** — open the matching telescope's GUI

### 💾 Diskette Console

Lets you upgrade the quality of diskettes with recorded signals.

```
   [IRON_BLOCK] [IRON_BLOCK] [IRON_BLOCK]   ← top row
  [REDSTONE_BLOCK]   [*]   [REDSTONE_BLOCK] ← middle row
                [DIAMOND_BLOCK]              ← base (*)
```

**Controls:**
- **Right-click the base** (`DIAMOND_BLOCK`) — open the console GUI
- Insert a diskette and choose an upgrade mode

### 🏧 Cashier

Buys diskettes with recorded signals and deposits money into your account.

```
[IRON_BARS] [IRON_BARS] [IRON_BARS]   ← top row (3 blocks)
             [IRON_BARS]               ← middle block
              [BARREL]                 ← base
```

**Usage:**
1. Build the structure
2. Open the barrel (right-click)
3. Put hashed diskettes inside
4. Close the inventory — the cashier collects the diskettes and **after a delay** deposits the money

## 💿 Diskettes

Diskettes are the core information currency in MyCosmo.

**Diskette recipe** (shapeless):
```
1× Copper ingot + 1× Gold ingot + 2× Redstone dust
```

**Diskette quality** affects signal distortion when reading it and the price at the cashier:

| Quality | Base price |
|---------|-----------|
| Terrible  | 1  |
| Bad       | 2  |
| Average   | 5  |
| Good      | 7  |
| Excellent | 12 |

> Price also depends on the **signal source** — some sources grant a value bonus.

**Reading a diskette:** hold it in your hand and **right-click** air or a block.

## ⚙️ Configuration

### Language

In `config.yml`, set:
```yaml
locale: en   # or: ru
```

Available locales: **ru** (Russian), **en** (English).

> ⚠️ Right now, signal messages (`signals.yml`) are only available in **Russian**.  
> You can edit them yourself — just modify the `signals.yml` file in the plugin's folder.

### Signals

The `signals.yml` file lets you fully customize signal sources:

```yaml
sources:
  deep_space:
    name: "Deep Space"
    value_bonus: 3
    messages:
      - "...dash dash dot... come in..."
      - "Signal of unknown origin."
  
  transmitter:          # special rare source (1/250 chance)
    name: "THE TRANSMITTER"
    value_bonus: 10
    messages:
      - "We know you're listening."
```

Add new sources, change messages and bonuses — the plugin picks up changes after a restart.

## 🔬 Telescope calibration

Calibration affects scan speed:

| Level            | Delay multiplier |
|------------------|-------------------|
| Not calibrated   | ×1.55 (slower)    |
| Terrible         | ×1.35             |
| Bad              | ×1.20             |
| Inaccurate       | ×1.05             |
| Normal           | ×1.00             |
| Excellent        | ×0.85 (faster)    |

## 🚀 Roadmap

MyCosmo is a young plugin **under active development**. Coming up next:

- 🌐 Full multi-language support for signal messages
- 📻 New source types and rare events
- 🔐 Expanded diskette encryption mechanics
- 🏗️ New structures and interactions
- 📊 Leaderboards and interception statistics
- 🎵 Audio signals and ambient effects

Follow updates on [GitHub](https://github.com/ManagerNotMoney).

## 👤 Author

Made with a love for radio signals and dark skies.  
GitHub: [@ManagerNotMoney](https://github.com/ManagerNotMoney)

---

# 🇷🇺 Русская версия

## 📡 О плагине

**MyCosmo** — это плагин для Minecraft, который превращает сервер в радиостанцию наблюдения за неизвестными сигналами.  
Постройте телескоп, направьте антенну в небо, запишите перехваченный сигнал на дискету — и продайте её за монеты через кассира.

> *Что-то передаёт сигналы из темноты. Ваша задача — их поймать.*

Плагин **активно развивается**: впереди новые источники сигналов, механики шифрования, новые структуры и многое другое.

## 🧱 Совместимость

Плагин разработан и протестирован на **Minecraft 1.21.11** (Paper/Spigot, Java 17+).

Минимальная поддерживаемая версия — **1.21.9**. Структура телескопа использует материалы `WAXED_LIGHTNING_ROD` и `WAXED_COPPER_BARS`, которые появились только в обновлении **"The Copper Age"** (Java Edition 1.21.9, вышло 30 сентября 2025 года). На версиях **1.21.0–1.21.8** этих блоков не существует, поэтому плагин на них работать не будет.

## 🔧 Зависимости

| Плагин | Обязательно? |
|--------|-------------|
| **[Vault](https://www.spigotmc.org/resources/vault.34315/)** | ✅ Да |
| Любой экономический плагин (EssentialsX и др.) | ✅ Да |

## 🏗️ Постройки

### 📻 Радиотелескоп

Основная структура для перехвата сигналов. Строится снизу вверх, **столбом**:

```
        [REDSTONE_TORCH]       ← 5-й блок (верхушка)
       [WAXED_LIGHTNING_ROD]   ← 4-й блок
        [WAXED_COPPER_BARS]    ← 3-й блок
         [REDSTONE_BLOCK]      ← 2-й блок
   [BLUE_GLAZED_TERRACOTTA]    ← 1-й блок (основание)
```

**Важно:** над телескопом должно быть открытое небо — никаких блоков над вершиной до потолка мира.

**Управление:**
- **ПКМ по основанию** (`BLUE_GLAZED_TERRACOTTA`) — открыть GUI настроек телескопа
- **ПКМ по** `REDSTONE_BLOCK` **с бумагой в руке** — записать ID телескопа на бумагу (для обсерватории)
- **ПКМ по** `REDSTONE_BLOCK` **без предмета** — начать сканирование неба

> Сканирование занимает время. После завершения сигнал активен **5 минут** — успейте записать его на дискету!

### 🔭 Обсерватория

Позволяет управлять несколькими телескопами из одного места. Хранит бумаги с ID телескопов.

```
   [IRON_BLOCK] [IRON_BLOCK] [IRON_BLOCK]   ← верхний ряд (3 блока)
  [REDSTONE_BLOCK]   [*]   [REDSTONE_BLOCK] ← средний ряд
            [LIGHT_BLUE_GLAZED_TERRACOTTA]   ← основание (*)
```

**Ориентация:** структура может быть направлена по оси **X** или **Z**.

**Управление:**
- **ПКМ по основанию** (`LIGHT_BLUE_GLAZED_TERRACOTTA`) — открыть GUI обсерватории
- Положите бумаги с ID телескопов в слоты 4, 5, 6
- **ПКМ по бумаге внутри GUI** — открыть GUI соответствующего телескопа

### 💾 Консоль дискет

Позволяет улучшать качество дискет с записанными сигналами.

```
   [IRON_BLOCK] [IRON_BLOCK] [IRON_BLOCK]   ← верхний ряд
  [REDSTONE_BLOCK]   [*]   [REDSTONE_BLOCK] ← средний ряд
                [DIAMOND_BLOCK]              ← основание (*)
```

**Управление:**
- **ПКМ по основанию** (`DIAMOND_BLOCK`) — открыть GUI консоли
- Вставьте дискету и выберите режим улучшения

### 🏧 Кассир

Покупает дискеты с записанными сигналами и зачисляет деньги на ваш счёт.

```
[IRON_BARS] [IRON_BARS] [IRON_BARS]   ← верхний ряд (3 блока)
             [IRON_BARS]               ← средний блок
              [BARREL]                 ← основание
```

**Использование:**
1. Постройте структуру
2. Откройте бочку (ПКМ)
3. Положите дискеты с хэшем внутрь
4. Закройте инвентарь — кассир заберёт дискеты и **после небольшой задержки** зачислит деньги

## 💿 Дискеты

Дискеты — основная валюта информации в MyCosmo.

**Крафт дискеты** (бесформенный рецепт):
```
1× Медный слиток + 1× Золотой слиток + 2× Красная пыль
```

**Качество дискеты** влияет на искажение сигнала при чтении и на цену у кассира:

| Качество | Базовая цена |
|----------|-------------|
| Ужасное  | 1           |
| Плохое   | 2           |
| Среднее  | 5           |
| Хорошее  | 7           |
| Отличное | 12          |

> Цена также зависит от **источника сигнала** — некоторые источники дают бонус к стоимости.

**Прочитать дискету:** возьмите её в руку и нажмите **ПКМ** по воздуху или блоку.

## ⚙️ Настройка

### Язык

В файле `config.yml` измените параметр:
```yaml
locale: ru   # или: en
```

Доступные языки: **ru** (русский), **en** (английский).

> ⚠️ На данный момент сообщения в сигналах (`signals.yml`) доступны только на **русском языке**.  
> Вы можете изменить их вручную — просто отредактируйте файл `signals.yml` в папке плагина.

### Сигналы

Файл `signals.yml` позволяет полностью настроить источники сигналов:

```yaml
sources:
  deep_space:
    name: "Глубокий космос"
    value_bonus: 3
    messages:
      - "...тире тире точка... приём..."
      - "Сигнал неизвестного происхождения."
  
  transmitter:          # специальный редкий источник (шанс 1/250)
    name: "ПЕРЕДАТЧИК"
    value_bonus: 10
    messages:
      - "Мы знаем, что ты слушаешь."
```

Добавляйте любые источники, меняйте сообщения и бонусы — плагин подхватит изменения после перезапуска.

## 🔬 Калибровка телескопа

Калибровка влияет на скорость сканирования:

| Уровень          | Множитель задержки |
|------------------|--------------------|
| Не откалиброван  | ×1.55 (медленнее)  |
| Ужасная          | ×1.35              |
| Плохая           | ×1.20              |
| Неточная         | ×1.05              |
| Нормальная       | ×1.00              |
| Отличная         | ×0.85 (быстрее)    |

## 🚀 Планы развития

MyCosmo — молодой плагин в **активной разработке**. В ближайших обновлениях планируется:

- 🌐 Полная поддержка мультиязычных сообщений в сигналах
- 📻 Новые типы источников и редкие события
- 🔐 Расширенная механика шифрования дискет
- 🏗️ Новые структуры и взаимодействия
- 🎵 Аудиосигналы и атмосферные эффекты


## 👤 Автор

Сделано с любовью к радиосигналам и тёмному небу.  

---

<div align="center">

*"Something is out there. Just keep listening."*

</div>
