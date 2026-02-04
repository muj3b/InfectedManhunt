package com.mujeb.infectedmanhunt.game;

import com.mujeb.infectedmanhunt.InfectedManhuntPlugin;
import com.mujeb.infectedmanhunt.utils.Msg;
import com.mujeb.infectedmanhunt.utils.TitleUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.Locale;

public class GameManager {
    private final InfectedManhuntPlugin plugin;

    private final Set<UUID> speedrunners = new LinkedHashSet<>();
    private final Set<UUID> infected = new LinkedHashSet<>();
    private final Map<UUID, Location> lastOverworldLocations = new HashMap<>();
    private final Map<UUID, Location> lastEndPortalLocations = new HashMap<>();

    private UUID initialInfected;
    private GameState state = GameState.LOBBY;

    private static final String TEAM_SPEEDRUNNERS = "infected_speedrunners";
    private static final String TEAM_INFECTED = "infected_hunters";

    private Scoreboard pluginBoard;
    private final Map<UUID, Scoreboard> previousBoards = new HashMap<>();

    public GameManager(InfectedManhuntPlugin plugin) {
        this.plugin = plugin;
        ensureTeams();
    }

    public GameState getState() {
        return state;
    }

    public boolean isRunning() {
        return state == GameState.RUNNING;
    }

    public boolean isSpeedrunner(Player player) {
        return player != null && speedrunners.contains(player.getUniqueId());
    }

    public boolean isInfected(Player player) {
        return player != null && infected.contains(player.getUniqueId());
    }

    public Set<UUID> getSpeedrunners() {
        return Collections.unmodifiableSet(speedrunners);
    }

    public Set<UUID> getInfected() {
        return Collections.unmodifiableSet(infected);
    }

    public UUID getInitialInfected() {
        return initialInfected;
    }

    public void startGame(Player chosenInfected) {
        if (chosenInfected == null || !chosenInfected.isOnline()) {
            Msg.broadcast("§c[Infected] No valid infected player selected.");
            return;
        }
        if (state == GameState.RUNNING) {
            Msg.broadcast("§c[Infected] Game is already running.");
            return;
        }

        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (online.size() < 2) {
            Msg.broadcast("§c[Infected] Need at least 2 online players to start.");
            return;
        }

        resetInternal();
        state = GameState.RUNNING;

        initialInfected = chosenInfected.getUniqueId();
        infected.add(initialInfected);

        for (Player p : online) {
            if (!p.getUniqueId().equals(initialInfected)) {
                speedrunners.add(p.getUniqueId());
            }
        }

        for (Player p : online) {
            assignScoreboard(p);
        }
        applyTeams();
        removeCompassesFromSpeedrunners();
        giveCompass(chosenInfected);

        boolean teleport = plugin.getConfig().getBoolean("start.teleport_to_spawn", true);
        if (teleport) {
            Location spawn = getWorldSpawn();
            for (Player p : online) {
                try { p.teleport(spawn); } catch (Throwable ignored) {}
            }
        }

        broadcastStart(chosenInfected);
        plugin.getTrackerManager().startTracking();
    }

    public void startGameRandom() {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (online.size() < 2) {
            Msg.broadcast("§c[Infected] Need at least 2 online players to start.");
            return;
        }
        Player chosen = online.get(new Random().nextInt(online.size()));
        startGame(chosen);
    }

    public void endGame(boolean speedrunnersWin) {
        if (state != GameState.RUNNING) return;
        state = GameState.FINISHED;
        plugin.getTrackerManager().stopTracking();

        if (speedrunnersWin) {
            Msg.broadcast("§a§l[Infected] Speedrunners win! The Ender Dragon has been slain.");
        } else {
            Msg.broadcast("§c§l[Infected] Infected win! All speedrunners have been infected.");
        }

        boolean teleport = plugin.getConfig().getBoolean("start.teleport_to_spawn", true);
        if (teleport) {
            Location spawn = getWorldSpawn();
            for (Player p : Bukkit.getOnlinePlayers()) {
                try { p.teleport(spawn); } catch (Throwable ignored) {}
            }
        }

        resetGame();
    }

    public void resetGame() {
        plugin.getTrackerManager().stopTracking();
        resetInternal();
        state = GameState.LOBBY;
        clearTeams();
        restoreScoreboards();
    }

    private void resetInternal() {
        speedrunners.clear();
        infected.clear();
        lastOverworldLocations.clear();
        lastEndPortalLocations.clear();
        initialInfected = null;
    }

    public void infect(Player player) {
        if (player == null) return;
        UUID id = player.getUniqueId();
        if (!speedrunners.contains(id)) return;

        speedrunners.remove(id);
        infected.add(id);
        applyTeams();

        Msg.broadcast("§c[Infected] " + player.getName() + " has been infected and joined the hunters!");
        boolean titles = plugin.getConfig().getBoolean("start.announce_titles", true);
        if (titles) {
            TitleUtil.showTitle(player, "§c§lINFECTED!", "§7You are now a Hunter", 10, 60, 10);
        }

        // Give compass immediately if possible
        giveCompass(player);

        // Win check
        if (speedrunners.isEmpty()) {
            endGame(false);
        }
    }

    public void updateLastOverworldLocation(Player runner, Location location) {
        if (runner == null || location == null || location.getWorld() == null) return;
        if (location.getWorld().getEnvironment() != World.Environment.NORMAL) return;
        lastOverworldLocations.put(runner.getUniqueId(), location.clone());
    }

    public void updateLastEndPortalLocation(Player runner, Location location) {
        if (runner == null || location == null || location.getWorld() == null) return;
        lastEndPortalLocations.put(runner.getUniqueId(), location.clone());
    }

    public Location getLastOverworldLocation(UUID runnerId) {
        Location loc = lastOverworldLocations.get(runnerId);
        return loc == null ? null : loc.clone();
    }

    public Location getLastEndPortalLocation(UUID runnerId) {
        Location loc = lastEndPortalLocations.get(runnerId);
        return loc == null ? null : loc.clone();
    }

    public Location getWorldSpawn() {
        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        return world == null ? null : world.getSpawnLocation();
    }

    public Location getInfectedRespawnLocation() {
        String mode = plugin.getConfig().getString("respawn.mode", "vanilla");
        if (mode == null) mode = "vanilla";
        mode = mode.toLowerCase(Locale.ROOT);
        if (mode.equals("world_spawn")) {
            return getWorldSpawn();
        }
        if (mode.equals("fixed")) {
            String worldName = plugin.getConfig().getString("respawn.world", "world");
            World world = Bukkit.getWorld(worldName);
            if (world == null && !Bukkit.getWorlds().isEmpty()) world = Bukkit.getWorlds().get(0);
            if (world == null) return null;
            double x = plugin.getConfig().getDouble("respawn.x", world.getSpawnLocation().getX());
            double y = plugin.getConfig().getDouble("respawn.y", world.getSpawnLocation().getY());
            double z = plugin.getConfig().getDouble("respawn.z", world.getSpawnLocation().getZ());
            return new Location(world, x, y, z);
        }
        return null; // vanilla
    }

    public void ensureTeams() {
        if (pluginBoard == null) {
            pluginBoard = Bukkit.getScoreboardManager().getNewScoreboard();
        }
        Team runners = pluginBoard.getTeam(TEAM_SPEEDRUNNERS);
        if (runners == null) runners = pluginBoard.registerNewTeam(TEAM_SPEEDRUNNERS);
        Team hunters = pluginBoard.getTeam(TEAM_INFECTED);
        if (hunters == null) hunters = pluginBoard.registerNewTeam(TEAM_INFECTED);

        applyTeamFormatting(runners, true);
        applyTeamFormatting(hunters, false);
    }

    private void applyTeamFormatting(Team team, boolean speedrunner) {
        if (team == null) return;
        String colorKey = speedrunner ? plugin.getConfig().getString("teams.speedrunner_color", "GREEN")
                                      : plugin.getConfig().getString("teams.infected_color", "RED");
        ChatColor color;
        try {
            color = ChatColor.valueOf(colorKey.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            color = speedrunner ? ChatColor.GREEN : ChatColor.RED;
        }
        try {
            team.setColor(color);
        } catch (Throwable ignored) {}

        String prefix = speedrunner ? plugin.getConfig().getString("teams.speedrunner_prefix", "§a[R] ")
                                    : plugin.getConfig().getString("teams.infected_prefix", "§c[INF] ");
        try {
            team.setPrefix(prefix);
        } catch (Throwable ignored) {
            // Older API versions ignore prefix component changes; safe to skip
        }
    }

    public void applyTeams() {
        ensureTeams();
        Team runners = pluginBoard.getTeam(TEAM_SPEEDRUNNERS);
        Team hunters = pluginBoard.getTeam(TEAM_INFECTED);
        if (runners == null || hunters == null) return;

        // Clear all entries then reassign
        for (String entry : new HashSet<>(runners.getEntries())) runners.removeEntry(entry);
        for (String entry : new HashSet<>(hunters.getEntries())) hunters.removeEntry(entry);

        for (UUID id : speedrunners) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                runners.addEntry(p.getName());
            }
        }

        for (UUID id : infected) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                hunters.addEntry(p.getName());
            }
        }
    }

    public void clearTeams() {
        if (pluginBoard == null) return;
        Team runners = pluginBoard.getTeam(TEAM_SPEEDRUNNERS);
        Team hunters = pluginBoard.getTeam(TEAM_INFECTED);
        if (runners != null) {
            for (String entry : new HashSet<>(runners.getEntries())) runners.removeEntry(entry);
        }
        if (hunters != null) {
            for (String entry : new HashSet<>(hunters.getEntries())) hunters.removeEntry(entry);
        }
    }

    public void broadcastStart(Player infectedPlayer) {
        Msg.broadcast("§c§l[Infected] " + infectedPlayer.getName() + " is the INFECTED HUNTER!");
        Msg.broadcast("§e[Infected] Speedrunners: beat the Ender Dragon before everyone is infected.");
        Msg.broadcast("§e[Infected] If you die, you join the infected hunters.");

        boolean titles = plugin.getConfig().getBoolean("start.announce_titles", true);
        if (titles) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getUniqueId().equals(infectedPlayer.getUniqueId())) {
                    TitleUtil.showTitle(p, "§c§lYOU ARE INFECTED", "§7Hunt the speedrunners", 10, 80, 10);
                } else {
                    TitleUtil.showTitle(p, "§a§lSPEEDRUNNERS", "§7Beat the dragon before you die", 10, 80, 10);
                }
            }
        }
    }

    public void giveCompass(Player hunter) {
        if (hunter == null) return;
        if (!infected.contains(hunter.getUniqueId())) return;
        boolean hasCompass = hunter.getInventory().contains(org.bukkit.Material.COMPASS);
        if (!hasCompass) {
            hunter.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.COMPASS, 1));
        }
    }

    public void removeCompassesFromSpeedrunners() {
        for (UUID id : speedrunners) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                stripCompass(p);
            }
        }
    }

    public void stripCompass(Player player) {
        if (player == null) return;
        if (player.getInventory().getItemInOffHand() != null &&
                player.getInventory().getItemInOffHand().getType() == org.bukkit.Material.COMPASS) {
            player.getInventory().setItemInOffHand(null);
        }
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == org.bukkit.Material.COMPASS) {
                stack.setAmount(0);
            }
        }
        player.updateInventory();
    }

    public void assignScoreboard(Player player) {
        if (player == null) return;
        ensureTeams();
        previousBoards.putIfAbsent(player.getUniqueId(), player.getScoreboard());
        player.setScoreboard(pluginBoard);
    }

    public void restoreScoreboards() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Scoreboard prev = previousBoards.get(p.getUniqueId());
            if (prev != null) {
                p.setScoreboard(prev);
            }
        }
        previousBoards.clear();
        pluginBoard = null;
    }
}
