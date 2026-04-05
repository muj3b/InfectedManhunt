<div align="center">

# Infected Manhunt

### Beat the Ender Dragon before every runner gets infected

![Minecraft](https://img.shields.io/badge/Minecraft-1.21%2B-28A745?style=for-the-badge&logo=minecraft)
![API](https://img.shields.io/badge/API-Paper%2FSpigot%2FPurpur%2FBukkit-2875D7?style=for-the-badge)
![Mode](https://img.shields.io/badge/Mode-Infected_Manhunt-FF4D4D?style=for-the-badge)

</div>

Infected Manhunt is a Minecraft minigame plugin for **1.21+** servers.

One player starts as the **infected hunter**. Everyone else starts as a **runner**.  
If a runner dies, they switch to the infected team and start hunting too.  
Runners win by killing the Ender Dragon before the whole lobby gets infected.

This plugin is built to be easy to run for normal servers:
- simple setup
- clean in-game GUI
- configurable tracking and respawns
- live admin team management during a match

---

## What Players Experience

- One infected starts the game.
- Runners try to finish the dragon fight before they all get converted.
- Every runner death grows the infected team.
- Infected players get a tracking compass.
- The compass can cycle targets or open a target picker GUI.
- If runners move to another dimension, tracking falls back to useful last-known locations.

---

## Main Features

- **Infection on death**
  Runners become infected when they die.

- **Snowballing manhunt gameplay**
  The longer the game lasts, the more dangerous the infected team becomes.

- **Compass tracking**
  Infected players always get a compass that tracks active runners.

- **Target picker GUI**
  Infected players can choose exactly who they want to track.

- **Full admin GUI**
  Server operators can manage games, settings, teams, respawns, and tracking from one menu.

- **Live team management**
  Operators can move players between **runner** and **infected** directly from the GUI or with a command.

- **Disconnect-safe state**
  Leaving the server does not corrupt a player's team state.

- **Configurable infected respawns**
  Use vanilla respawns, world spawn, or a fixed custom location.

- **Custom team colors and prefixes**
  Adjust runner/infected formatting for your server.

---

## Installation

1. Download the plugin jar, or build it from source.
2. Put the jar in your server's `plugins/` folder.
3. Start or restart the server.
4. Edit `plugins/InfectedManhunt/config.yml` if needed.
5. Run `/infected gui` in game to manage the plugin.

### Requirements

- Minecraft **1.21+**
- Paper, Purpur, Spigot, or Bukkit

Paper or Purpur is recommended for the best overall compatibility.

---

## Quick Start

1. Join the server with at least 2 players online.
2. Run `/infected gui`.
3. Start a match with:
   - **Start (Random)**, or
   - **Start (Select)** to choose the first infected manually.
4. Let the runners go.
5. Stop or reset the game from the GUI or with `/infected stop`.

---

## Admin GUI

The GUI is the main way to control the plugin.

### Main Menu

- Start a match with a random infected
- Start a match with a selected infected
- Stop and reset the current match
- View match status
- Open tracking settings
- Open respawn settings
- Open team settings
- Reload config
- Open target picker if you are infected

### Tracking Settings

- Change compass update speed
- Change last-known message cooldown
- Toggle last-known tracking messages

### Respawn Settings

- Set infected respawn mode to:
  - `vanilla`
  - `world_spawn`
  - `fixed`
- Save a fixed respawn from your current location

### Team Settings

- Change runner team color
- Change infected team color
- Edit runner prefix in chat
- Edit infected prefix in chat
- Open the live team management screen

### Team Management

- Left click a player to make them a **runner**
- Right click a player to make them **infected**
- Shift click to toggle their team

This is meant for live match correction, moderation, and setup changes without needing to restart the game.

---

## Commands

### Admin Commands

- `/infected start <player>`  
  Start a game with the chosen infected player.

- `/infected start random`  
  Start a game with a random infected player.

- `/infected stop`  
  Stop and fully reset the current game.

- `/infected gui`  
  Open the admin GUI.

- `/infected team <player> <runner|infected>`  
  Move a player to a specific team during an active game.

- `/infected reload`  
  Reload the config.

### General Commands

- `/infected status`  
  Show current game state and team counts.

- `/infected track`  
  Open the target picker if you are infected.

---

## Permissions

- `infectedmanhunt.admin`
  Full control over the plugin, including GUI access, start/stop, reload, and team changes.

- `infectedmanhunt.command`
  Base command access.

---

## How the Gamemode Works

- One player starts as infected.
- All other players start as runners.
- When a runner dies, they become infected.
- Infected players stay infected unless an operator changes their team manually.
- Runners win by killing the Ender Dragon.
- Infected win when there are no runners left.
- Disconnecting and rejoining does not change your team.

---

## Compass Rules

- Only infected players use the hunter compass.
- Right click cycles between available runners.
- Sneak + right click opens the target picker GUI.
- If the target is in another dimension, the compass can use last-known data.
- Speedrunners cannot keep, pick up, or craft hunter compasses during the match.

---

## Configuration

The plugin stores its settings in `plugins/InfectedManhunt/config.yml`.

### Start Settings

- `start.teleport_to_spawn`
  Teleport all players to spawn when a game starts.

- `start.announce_titles`
  Show title messages when the game starts and when players become infected.

### Respawn Settings

- `respawn.mode`
  `vanilla`, `world_spawn`, or `fixed`

- `respawn.world`
  World name used for fixed respawn mode.

- `respawn.x`
- `respawn.y`
- `respawn.z`
  Coordinates used for fixed respawn mode.

### Tracking Settings

- `tracking.update_ticks`
  How often the compass updates.

- `tracking.message_cooldown_seconds`
  Cooldown for last-known tracking messages.

- `tracking.notify_last_known`
  Enable or disable those last-known messages.

### Team Settings

- `teams.speedrunner_color`
- `teams.infected_color`
  Team chat/scoreboard colors.

- `teams.speedrunner_prefix`
- `teams.infected_prefix`
  Prefixes shown for each team.

---

## Building From Source

If you want to build the jar yourself:

```bash
mvn package
```

Output jar:

`target/infectedmanhunt-1.0.2.jar`

---

## Notes for Server Owners

- The plugin is designed around vanilla-feeling gameplay with minimal visual clutter.
- The GUI is intended to be the primary control surface, so most server owners do not need to memorize commands.
- Live team management exists specifically so admins can correct player state during an active match without resetting everything.

---

## Credits

- Inspired by Sapnap's infected manhunt format
- Developed by **muj4b**
