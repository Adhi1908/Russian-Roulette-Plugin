# 🎰 Russian Roulette Plugin

A thrilling Russian Roulette minigame plugin for Minecraft Paper/Spigot 1.20+

## Features

- 🔫 **6-Chamber Revolver** - Realistic revolver mechanics with configurable bullets
- 🎮 **Game Modes** - Classic (1 bullet), Hardcore (2), Insane (3)
- 💰 **Betting System** - Players wager money or items, winner takes all
- 🎨 **Visual Effects** - Blood particles, smoke, "BANG!" titles, slow-motion death
- 📊 **Live Scoreboard** - Shows turn, players alive, bullets remaining
- 🔒 **Anti-Abuse** - No leaving during turn, disconnect = death, no teleporting

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/rr start` | Start new game | `russianroulette.start` |
| `/rr join` | Join waiting game | `russianroulette.play` |
| `/rr leave` | Leave current game | `russianroulette.play` |
| `/rr forceend` | Admin force end | `russianroulette.admin` |
| `/rr reload` | Reload config | `russianroulette.admin` |

## Dependencies

- **Paper/Spigot 1.20+** (required)
- **Vault** (optional, for money betting)

## Configuration

All features are fully configurable via `config.yml` and `messages.yml`.

### Betting Options
- **Money betting** - Requires Vault economy plugin
- **Item betting** - Configurable allowed items with values (diamonds, netherite, gold, etc.)

## Installation

1. Download the latest JAR from releases
2. Place in your server's `plugins/` folder
3. Restart server
4. Configure `plugins/RussianRoulette/config.yml`

## Building

```bash
mvn clean package
```

## License

MIT License
