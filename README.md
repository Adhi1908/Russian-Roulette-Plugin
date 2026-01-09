# 🎰 Russian Roulette Plugin

<div align="center">

![MBTH STUDIOS](https://img.shields.io/badge/MBTH%20STUDIOS-Plugin-red?style=for-the-badge)
![Minecraft](https://img.shields.io/badge/Minecraft-1.20%2B-green?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

**A thrilling, fully-featured Russian Roulette minigame for Minecraft**

*Developed by MBTH STUDIOS*

</div>

---

## ✨ Features

### 🔫 Core Gameplay
- **6-Chamber Revolver** - Realistic revolver mechanics with spin animation
- **3 Game Modes** - Classic (1 bullet), Hardcore (2 bullets), Insane (3 bullets)
- **Right-Click to Shoot** - Immersive gameplay, no GUI required
- **Auto-Trigger Timeout** - Players who don't shoot in time auto-fire

### 🎬 Cinematic Experience
- **Camera Intro** - Cinematic pan around the table introducing all players
- **Player Introductions** - Each player announced with dramatic flair
- **Automatic Seating** - Players teleported to configured chair positions

### 💰 Betting System
- **Money Betting** - Wager in-game currency (requires Vault)
- **Item Betting** - Bet diamonds, netherite, gold, and other valuables
- **Configurable Items** - Define exactly which items can be bet and their values
- **House Cut** - Optional server cut from the pot (0-100%)
- **Auto-Refunds** - Bets refunded if game is cancelled

### 🎨 Visual Effects
- **Blood Particles** - Dramatic death particles
- **Smoke Effects** - Gun smoke on trigger pull
- **BANG! Title** - Screen flash on death
- **Slow-Motion Death** - Brief slow-mo effect when shot
- **Live Scoreboard** - Real-time game stats

### 🔒 Anti-Abuse
- **No Leaving During Turn** - Can't escape your fate
- **Disconnect = Death** - Quitters are eliminated
- **Movement Lock** - Players seated, can't run away

---

## 📋 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/rr start` | Start a new game | `russianroulette.start` |
| `/rr join` | Join a waiting game | `russianroulette.play` |
| `/rr leave` | Leave current game | `russianroulette.play` |
| `/rr forceend` | Force end active game | `russianroulette.admin` |
| `/rr reload` | Reload configuration | `russianroulette.admin` |
| `/rr setseat <1-6>` | Set seat position | `russianroulette.admin` |
| `/rr setcenter` | Set table center | `russianroulette.admin` |

---

## ⚙️ Configuration

### Arena Setup (`config.yml`)

```yaml
arena:
  world: "world"
  teleportToArena: true
  center:           # Table center for cinematic
    x: 100
    y: 64
    z: 200
  
  seats:            # Individual chair coordinates
    seat1:
      x: 102
      y: 64
      z: 200
      yaw: 270      # Face direction (0=south, 90=west, 180=north, 270=east)
    seat2:
      x: 101
      y: 64
      z: 198
      yaw: 180
    # ... up to seat6
```

### Betting Configuration

```yaml
betting:
  enabled: true
  type: MONEY       # MONEY or ITEMS
  moneyAmount: 1000 # For money betting
  
  # For item betting
  minItemValue: 100 # Minimum bet value
  allowedItems:
    - material: DIAMOND
      valuePerItem: 50
    - material: NETHERITE_INGOT
      valuePerItem: 500
    - material: GOLDEN_APPLE
      valuePerItem: 100
    # ... many more pre-configured
  
  houseCut: 0       # Server cut (0-100%)
  refundOnCancel: true
```

### Game Settings

```yaml
game:
  minPlayers: 2
  maxPlayers: 6
  turnTime: 10      # Seconds per turn
  gameMode: CLASSIC # CLASSIC, HARDCORE, or INSANE
  startCountdown: 5
```

---

## 🎮 Gameplay Flow

1. **Start Game** - Admin or player runs `/rr start`
2. **Join Game** - Players join with `/rr join` (bets are taken)
3. **Teleport** - Players teleported to their assigned seats
4. **Cinematic** - Camera pans around table introducing players
5. **Play** - Each player takes turns right-clicking the revolver
6. **Outcome** - *Click* (survive) or **BANG!** (eliminated)
7. **Winner** - Last player standing wins the pot!

---

## 📦 Dependencies

- **Paper/Spigot 1.20+** (required)
- **Vault** (optional, for money betting)
- **Economy Plugin** (optional, e.g., EssentialsX, CMI)

---

## 🔧 Installation

1. Download the latest JAR from [Releases](https://github.com/Adhi1908/Russian-Roulette-Plugin/releases)
2. Place in your server's `plugins/` folder
3. Restart server
4. Set up arena in-game:
   - `/rr setcenter` - Stand at table center
   - `/rr setseat 1` through `/rr setseat 6` - Stand on each chair
5. Start playing with `/rr start`!

---

## 🏗️ Building from Source

```bash
git clone https://github.com/Adhi1908/Russian-Roulette-Plugin.git
cd Russian-Roulette-Plugin
mvn clean package
```

JAR will be in `target/` folder.

---

## 📝 Default Allowed Bet Items

| Item | Value |
|------|-------|
| Diamond | 50 |
| Diamond Block | 450 |
| Gold Ingot | 10 |
| Gold Block | 90 |
| Golden Apple | 100 |
| Enchanted Golden Apple | 1000 |
| Netherite Ingot | 500 |
| Netherite Block | 4500 |
| Netherite Armor/Tools | 550-1500 |
| Emerald | 25 |
| Totem of Undying | 500 |
| Elytra | 2000 |
| Nether Star | 1500 |

---

## 📄 License

MIT License - Free to use and modify!

---

<div align="center">

### 🎮 MBTH STUDIOS

*Creating immersive Minecraft experiences*

[![GitHub](https://img.shields.io/badge/GitHub-Adhi1908-black?style=flat-square&logo=github)](https://github.com/Adhi1908)

</div>
