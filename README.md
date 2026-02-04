<div align="center">

# 🧟 Infected Manhunt 🧟

### Beat the Ender Dragon before everyone turns infected

</div>

<div align="center">

![Minecraft](https://img.shields.io/badge/Minecraft-1.21%2B-28A745?style=for-the-badge&logo=minecraft)
![API](https://img.shields.io/badge/API-Paper%2FSpigot%2FPurpur%2FBukkit-2875D7?style=for-the-badge)
![Mode](https://img.shields.io/badge/Mode-Infected_Manhunt-FF4D4D?style=for-the-badge)

</div>

> **One infected hunter starts the chase. Every speedrunner death creates a new hunter.**
> Your only objective: kill the Ender Dragon before everyone is infected.

---

## 🎬 Inspired By

- [Sapnap’s Infected Manhunt Video](https://www.youtube.com/watch?v=ReZMjIoMBwY)

---

## ✅ Core Features

- **Infection on Death**: Any speedrunner death permanently converts them to an infected hunter.
- **Snowballing Hunters**: The hunter team grows as deaths happen, making late game chaotic.
- **Win Conditions**:
  - Speedrunners win by killing the Ender Dragon.
  - Infected win when no speedrunners remain.
- **Compass Tracking**:
  - Infected hunters get a compass.
  - Right‑click cycles targets.
  - Sneak + right‑click opens a target picker GUI.
  - Uses last known locations when runners are in other dimensions.
  - Compass is anti‑drop and cannot be picked up by speedrunners.
- **Minimal Visuals**: Standard Minecraft look with clean chat + title messaging.
- **Full GUI Control**: Everything in the config and commands is controllable via GUI.

---

## ⚡ Quick Start

1. **Build or download the jar**
2. **Place it in** `plugins/`
3. **Restart server**
4. **Run** `/infected gui`
5. **Start a game** (random or choose infected)

---

## 🧭 Commands

- `/infected start <player>` Start with chosen infected
- `/infected start random` Start with random infected
- `/infected stop` Reset the game
- `/infected status` Show game status
- `/infected gui` Open admin GUI
- `/infected track` Open target picker (infected hunters only)
- `/infected reload` Reload config

---

## 🔐 Permissions

- `infectedmanhunt.admin` Full control (start/stop/config/GUI)
- `infectedmanhunt.command` Basic command access

---

## 🖥️ GUI Overview

Everything in config + commands is available inside the GUI.

**Main Menu**
- Start (random)
- Start (select infected)
- Stop/reset
- Status panel
- Tracking settings
- Respawn settings
- Team colors/prefix
- Target picker (if infected)
- Reload config

**Tracking Menu**
- Update ticks
- Message cooldown
- Toggle last‑known notifications

**Respawn Menu**
- Respawn mode: `vanilla`, `world_spawn`, or `fixed`
- Set fixed location from your current position

**Team Menu**
- Cycle team colors
- Edit prefixes by chat

**Target Picker**
- Select any active speedrunner to track

---

## ⚙️ Configuration (config.yml)

### Start
- `start.teleport_to_spawn` Teleport all players to spawn on start
- `start.announce_titles` Show titles at start and on infection

### Respawn
- `respawn.mode` One of `vanilla`, `world_spawn`, `fixed`
- `respawn.world` World name for fixed mode
- `respawn.x`, `respawn.y`, `respawn.z` Coordinates for fixed mode

### Tracking
- `tracking.update_ticks` Compass update frequency in ticks
- `tracking.message_cooldown_seconds` Delay between “last known” messages
- `tracking.notify_last_known` Toggle last‑known messaging

### Teams
- `teams.speedrunner_color` ChatColor value (e.g. GREEN)
- `teams.infected_color` ChatColor value (e.g. RED)
- `teams.speedrunner_prefix` Prefix for speedrunners
- `teams.infected_prefix` Prefix for infected hunters

---

## 🧟 How Infection Works

- At game start, one player is chosen as the infected hunter.
- All other players are speedrunners.
- Whenever a speedrunner dies, they permanently convert into infected hunters.
- Hunters always remain hunters even if they die again.
- The infected team wins when no speedrunners remain.

---

## 🧭 Compass Rules

- Hunters always receive a compass.
- The compass always tracks a speedrunner.
- Right‑click cycles targets.
- Sneak + right‑click opens the target picker.
- If a runner is in another dimension, the compass uses last‑known locations and tells you.
- Speedrunners cannot pick up hunter compasses or craft new ones during a match.

---

## 🛠️ Build

```bash
cd InfectedManhunt
mvn package
```

Jar output:
`InfectedManhunt/target/infectedmanhunt-1.0.0.jar`

---

## 📌 Notes

- Designed for 1.21+ on Paper, Spigot, Purpur, and Bukkit.
- No resource packs, no custom textures, just vanilla UI + titles.

---

## 🙌 Credits

- Inspired by Sapnap’s Infected Manhunt
- Developed by muj3b
