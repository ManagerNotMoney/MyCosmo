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
![Version](https://img.shields.io/badge/Version-1.0.2-blue?style=flat-square)
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

> Scanning takes time and is accompanied by a **red particle beam** rising from the tip of the telescope. Once finished, the signal stays active for **5 minutes** — record it onto a diskette before it expires!

> Telescope IDs are **persisted** to `telescopes.yml` — they survive server restarts and chunk unloads.

### 🔭 Observatory

Lets you manage several telescopes from one place. Stores telescope ID papers and shows their **live status**.

```
   [IRON_BLOCK] [IRON_BLOCK] [IRON_BLOCK]   ← top row (3 blocks)
  [REDSTONE_BLOCK]   [*]   [REDSTONE_BLOCK] ← middle row
            [LIGHT_BLUE_GLAZED_TERRACOTTA]   ← base (*)
```

**Orientation:** the structure can face either the **X** or **Z** axis.

**GUI:** 18 slots — 3 paper slots on top, 3 **status panels** directly below each paper. Status auto-refreshes every second.

**Controls:**
- **Right-click the base** (`LIGHT_BLUE_GLAZED_TERRACOTTA`) — open the observatory GUI
- Place telescope ID papers in slots 4, 5, 6
- **Right-click a paper** — open the matching telescope's GUI
- **Left-click a status panel** — start a sky scan on that telescope
- **Right-click a status panel** — start recording a signal (needs an empty diskette in your inventory)

The status panel shows the current state (scanning / recording / tuning / signal found / waiting), calibration level, current settings, and whether they match the active signal.

> Only one player can use an observatory at a time.

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
- Scrolling the mode selector **auto-starts** the upgrade as soon as the correct mode is selected

> Only one player can use a console at a time. Notifications about upgrade completion can be enabled via the bell icon.

### 🏧 Cashier

Buys diskettes with recorded signals and deposits money into your account.

```
[IRON_BARS] [IRON_BARS] [IRON_BARS]   ← top row (3 blocks)
             [IRON_BARS]               ← middle block
              [BARREL]                 ← base
```

**Pricing:** each diskette is priced **individually** with a random ± `price_drift` drift (configurable, see below). The final total is rounded to two decimals.

**Usage:**
1. Build the structure
2. Open the barrel (right-click)
3. Put hashed diskettes inside
4. Close the inventory — the cashier collects the diskettes and **after a delay** deposits the money

> Money is credited by UUID — you'll be paid **even if you go offline** while the drone is en route.  
> Any non-diskette items are dropped back next to the barrel — they are never lost.  
> If Vault/economy is not loaded, the cashier **will not** take your diskettes.

## 💿 Diskettes

Diskettes are the core information currency in MyCosmo.

**Diskette recipe** (shapeless):
```
1× Copper ingot + 1× Gold ingot + 2× Redstone dust
```

> ⚠️ Diskettes **cannot** be used as ingredients in other recipes — the plugin blocks such crafts automatically.

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

### 🔐 Encrypted diskettes

Some sources produce **encrypted** diskettes. Such diskettes:

- Always record at **Terrible** quality
- Carry a `§cШифрование: §4зашифровано` lore line
- Must be **decoded** in a Diskette Console (mode: **Decoder**) before regular upgrades can begin

Whether a source is encrypted is now a config flag (`encrypted: true` in `signals.yml`) — no code changes needed.

## ⚙️ Configuration

### Language

In `config.yml`, set:
```yaml
locale: en   # or: ru
```

Available locales: **ru** (Russian), **en** (English).

> ⚠️ Signal messages (`signals.yml`) ship only in **Russian**.  
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
  
  transmitter:
    name: "THE TRANSMITTER"
    value_bonus: 10
    encrypted: true      # quality locked to "Terrible" until decoded
    rare_chance: 250     # 1 in 250 chance, checked independently
    messages:
      - "We know you're listening."
```

- **`encrypted`** — locks recorded quality to the worst tier and requires decoding before upgrade.
- **`rare_chance: N`** — rare chance is now checked **independently per rare source**. If none of the rare sources hit, a normal source is chosen. Multiple rare sources are supported.
- **`value_bonus`** — added to the diskette's base price.
- **`name`** must be unique — the plugin warns about duplicates.

Add new sources, change messages and bonuses — the plugin picks up changes after a restart.

### Cashier pricing

```yaml
cashier:
  price_drift: 1.0   # ± random drift per diskette, 0 = no drift
```

Set to `0` for fixed prices, or increase for more variance.

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
- 🔐 Further expansion of the encryption mechanics *(started in 1.0.2)*
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

> Сканирование занимает время и сопровождается **красным лучевым эффектом** из частиц над верхушкой телескопа. После завершения сигнал активен **5 минут** — успейте записать его на дискету!

> ID телескопов **сохраняются** в `telescopes.yml` — они переживают перезапуск сервера и выгрузку чанков.

### 🔭 Обсерватория

Позволяет управлять несколькими телескопами из одного места. Хранит бумаги с ID телескопов и показывает их **живой статус**.

```
   [IRON_BLOCK] [IRON_BLOCK] [IRON_BLOCK]   ← верхний ряд (3 блока)
  [REDSTONE_BLOCK]   [*]   [REDSTONE_BLOCK] ← средний ряд
            [LIGHT_BLUE_GLAZED_TERRACOTTA]   ← основание (*)
```

**Ориентация:** структура может быть направлена по оси **X** или **Z**.

**GUI:** 18 слотов — 3 слота под бумаги сверху и 3 **панели статуса** прямо под ними. Статус обновляется автоматически раз в секунду.

**Управление:**
- **ПКМ по основанию** (`LIGHT_BLUE_GLAZED_TERRACOTTA`) — открыть GUI обсерватории
- Положите бумаги с ID телескопов в слоты 4, 5, 6
- **ПКМ по бумаге** — открыть GUI соответствующего телескопа
- **ЛКМ по панели статуса** — запустить сканирование неба на этом телескопе
- **ПКМ по панели статуса** — начать запись сигнала (нужна пустая дискета в инвентаре)

Панель статуса показывает текущее состояние (сканирование / запись / настройка / сигнал найден / ожидание), уровень калибровки, текущие настройки и совпадают ли они с активным сигналом.

> Обсерваторией может пользоваться только один игрок одновременно.

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
- При прокрутке режима улучшение **запускается автоматически**, как только выбран нужный режим

> Консолью может пользоваться только один игрок одновременно. Уведомления о завершении улучшения включаются через иконку колокольчика.

### 🏧 Кассир

Покупает дискеты с записанными сигналами и зачисляет деньги на ваш счёт.

```
[IRON_BARS] [IRON_BARS] [IRON_BARS]   ← верхний ряд (3 блока)
             [IRON_BARS]               ← средний блок
              [BARREL]                 ← основание
```

**Ценообразование:** каждая дискета оценивается **индивидуально** со случайным отклонением ± `price_drift` (настраивается, см. ниже). Итоговая сумма округляется до двух знаков.

**Использование:**
1. Постройте структуру
2. Откройте бочку (ПКМ)
3. Положите дискеты с хэшем внутрь
4. Закройте инвентарь — кассир заберёт дискеты и **после небольшой задержки** зачислит деньги

> Деньги зачисляются по UUID — вы получите оплату, **даже если выйдете из игры**, пока дрон летит.  
> Предметы, не являющиеся дискетами, **выпадают рядом с бочкой** — они никогда не теряются.  
> Если Vault/экономика не загружена, кассир **не заберёт** ваши дискеты.

## 💿 Дискеты

Дискеты — основная валюта информации в MyCosmo.

**Крафт дискеты** (бесформенный рецепт):
```
1× Медный слиток + 1× Золотой слиток + 2× Красная пыль
```

> ⚠️ Дискеты **нельзя** использовать как ингредиент в других рецептах — плагин блокирует такие крафты автоматически.

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

### 🔐 Зашифрованные дискеты

Некоторые источники выдают **зашифрованные** дискеты. Такие дискеты:

- Всегда записываются с качеством **Ужасное**
- Имеют строку `§cШифрование: §4зашифровано` в lore
- Должны быть **расшифрованы** в Консоли дискет (режим **Декодер**), прежде чем их можно будет улучшать обычным образом

Шифрование теперь задаётся в конфиге (`encrypted: true` в `signals.yml`) — правки Java-кода не нужны.

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
  
  transmitter:
    name: "ПЕРЕДАТЧИК"
    value_bonus: 10
    encrypted: true       # качество зафиксировано на "Ужасное" до расшифровки
    rare_chance: 250      # шанс 1 к 250, проверяется независимо
    messages:
      - "Мы знаем, что ты слушаешь."
```

- **`encrypted`** — фиксирует записанное качество на худшем уровне и требует расшифровки перед апгрейдом.
- **`rare_chance: N`** — шанс теперь проверяется **независимо для каждого редкого источника**. Если ни один не сработал — выбирается обычный. Поддерживается несколько редких источников.
- **`value_bonus`** — прибавка к базовой цене дискеты.
- **`name`** должно быть уникальным — плагин предупреждает о дубликатах.

Добавляйте любые источники, меняйте сообщения и бонусы — плагин подхватит изменения после перезапуска.

### Ценообразование кассира

```yaml
cashier:
  price_drift: 1.0   # ± случайное отклонение для каждой дискеты, 0 = без дрейфа
```

Установите `0` для фиксированных цен или увеличьте для большего разброса.

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
- 🔐 Дальнейшее развитие механики шифрования *(начато в 1.0.2)*
- 🏗️ Новые структуры и взаимодействия
- 📊 Таблицы лидеров и статистика перехватов
- 🎵 Аудиосигналы и атмосферные эффекты

Следите за обновлениями на [GitHub](https://github.com/ManagerNotMoney).

## 👤 Автор

Сделано с любовью к радиосигналам и тёмному небу.  
GitHub: [@ManagerNotMoney](https://github.com/ManagerNotMoney)

---

<div align="center">

*"Something is out there. Just keep listening."*

</div>
```
