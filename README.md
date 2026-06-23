# MyCosmo — Minecraft Radio Astronomy Plugin

**MyCosmo** is a **Paper 1.21.11** plugin that brings interstellar signal hunting, diskette recording, quality upgrading, and an in-game economy to your Minecraft server. Build radio telescopes, scan the sky for mysterious transmissions, store them on diskettes, improve their quality, and even sell them for virtual currency.

---

## Features

- 📡 **Radio Telescopes** — multi‑block structures that scan the sky for signals.
- 💾 **Diskettes** — record signals onto diskettes, each with a random message, source, and quality level.
- 🔍 **Signal Sources** — dozens of unique sources (planets, relics, transmitters) with their own lore messages.
- ⚙️ **Quality System** — diskettes have quality tiers (Terrible → Excellent), affecting readability and value.
- 🛠️ **Upgrade Console** — improve diskette quality using different upgrade modes.
- 💰 **Cashier / ATM** — sell diskettes for in‑game money (requires Vault).
- 🏛️ **Observatory** — store and manage telescope IDs.
- 🌍 **Localization** — fully translatable (Russian and English included).

---

## Requirements

- **Paper 1.21.11** (or compatible fork)
- **Java 21+**
- **Vault** (optional, for economy support)

---

## Installation

1. Download the latest `MyCosmo.jar` from the releases page.
2. Place it into your server’s `plugins/` folder.
3. Start the server. The plugin will generate configuration files.
4. (Optional) Edit `config.yml`, `lang/ru.yml` / `lang/en.yml`, or `signals.yml` to customise messages and sources.
5. Reload or restart the server.

---

## Getting Started

### 1. Build a Radio Telescope

A telescope is a 5‑block tall vertical structure:

| Layer (Y) | Block                        |
|-----------|------------------------------|
| +4        | Redstone Torch              |
| +3        | Waxed Lightning Rod         |
| +2        | Waxed Copper Bars           |
| +1        | Redstone Block              |
| 0 (base)  | **Blue Glazed Terracotta**  |

When placed correctly, the base block will become interactive.

### 2. Scan for Signals

Right‑click the **Redstone Block** (layer +1) to start a scan.  
The telescope will listen for a random duration (5–15 seconds).  
If the sky above is clear and calibration is good, a signal is found.

> 💡 The signal’s **frequency**, **direction**, and **polarisation** must match the settings on the GUI to record it.

### 3. Open the Telescope GUI

Right‑click the **Blue Glazed Terracotta** (base) to open the telescope interface.

From there you can:
- Change frequency, direction, and polarisation.
- Start/stop scanning.
- Record a signal onto a blank diskette.
- View the telescope’s unique ID (and copy it to paper).

### 4. Obtain a Blank Diskette

Craft a blank diskette:

```
  Copper Ingot  +  Gold Ingot  +  2 Redstone Dust
```

(Shapeless recipe – any order.)

### 5. Record a Signal

With a blank diskette in your inventory, open the telescope GUI, ensure the signal parameters match, and click the **Record** button.  
The recording takes about 11 seconds. After completion, a filled diskette appears in the GUI slot.

### 6. Use a Diskette

Right‑click the air or a block (except certain “blocked” blocks like redstone blocks, barrels, etc.) with a diskette in hand to **read** its message.  
The message will be distorted depending on the diskette’s quality. The source and signal text are displayed in chat.

### 7. Upgrade Diskettes

Build an **Upgrade Console** to improve diskette quality.

#### Console Structure

A 3×3×3 structure (viewed from above):

**Layer Y+2 (top)**  
```
[Iron Block] [Iron Block] [Iron Block]
```
**Layer Y+1**  
```
[Iron Block] [Iron Block] [Iron Block]
```
**Layer Y (base)**  
```
[Redstone Block] [Diamond Block] [Redstone Block]
```
(Or rotated 90° – the diamond block is the center.)

Right‑click the **Diamond Block** to open the console GUI.

#### Upgrade Modes

| Mode           | Required Quality   | Result Quality |
|----------------|--------------------|----------------|
| Decoder        | (Encrypted)        | Terrible       |
| Denoise        | Terrible           | Bad            |
| Equalize       | Bad                | Average        |
| Dynamic Range  | Average            | Good           |
| Mastering      | Good               | Excellent      |

Place a diskette into the console slot, select the correct mode, and wait for the upgrade to finish (15 seconds).  
You can subscribe to notifications (bell icon) to be alerted when the upgrade completes.

### 8. Sell Diskettes (Cashier)

Build a **Cashier** to sell diskettes for money.

#### Cashier Structure

Vertical:

- Bottom: **Barrel**
- Middle: **Iron Bars**
- Top (3 blocks): **Iron Bars** in a horizontal line (either X or Z direction).

Right‑click the barrel to open its inventory. Place any diskettes inside; close the inventory to start the transaction.  
After a few seconds (4–10 seconds), the structure vanishes with particles, and you receive payment based on the diskette’s value.

**Value formula:** base value (depends on quality) + source bonus (if any).  
- Terrible → 1, Bad → 2, Average → 5, Good → 7, Excellent → 12 (plus source bonus).

Requires **Vault** to work. If Vault is not installed, the cashier will warn you.

### 9. Observatory (Telescope ID Storage)

Build an **Observatory** to store telescope ID papers.

#### Observatory Structure

- Base (center): **Light Blue Glazed Terracotta**
- Left/Right (or front/back) at same Y: **Redstone Block**
- Above those (Y+1): **Iron Block** covering a 3×3 area (including center).

Right‑click the base block to open an interface with 3 slots.  
Place a telescope ID paper (obtained by right‑clicking a telescope’s redstone block with a paper) into these slots to save them.

---

## Configuration

### `config.yml`

```yaml
locale: ru   # or en
```

Set the default language.

### `lang/`

- `ru.yml` – Russian translations.
- `en.yml` – English translations.

You can add or modify any message key.

### `signals.yml`

Defines signal sources. Each source has:
- `id` – unique identifier.
- `name` – display name.
- `value_bonus` – extra money when sold.
- `messages` – list of possible transmissions.

You can add new sources or edit existing ones.

---

## Commands

This plugin does **not** add any commands. All interaction is done through block GUI.

---

## Permissions

No permissions are required.

---

## Localization

The plugin uses a built‑in language system. To switch language, change `locale` in `config.yml`.  
All user‑facing messages are stored in the `lang/` folder. You can freely edit them.

---

## Building from Source

1. Clone the repository.
2. Build with Maven:

```bash
mvn clean package
```

The JAR will be in `target/`.

---

## Dependencies

- **Paper API 1.21.11** (provided)
- **Vault** (optional, for economy)

---

## License

This project is licensed under the **MIT License** – see the [LICENSE](LICENSE) file for details.

---

## Author

**ManagerMoney** – [GitHub](https://github.com/your-profile)

---

## Contributing

Issues and pull requests are welcome. Please follow the existing code style and include tests where appropriate.

---

**Enjoy exploring the cosmos!** 🌌
