package com.mujeb.infectedmanhunt.tracking;

import com.mujeb.infectedmanhunt.InfectedManhuntPlugin;
import com.mujeb.infectedmanhunt.game.GameManager;
import com.mujeb.infectedmanhunt.utils.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class TrackerManager {
    private final InfectedManhuntPlugin plugin;
    private final GameManager gameManager;
    private BukkitTask task;

    private final Map<UUID, UUID> hunterTargets = new HashMap<>();
    private final Map<UUID, Long> lastNotify = new HashMap<>();

    public TrackerManager(InfectedManhuntPlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    public void startTracking() {
        stopTracking();
        int updateTicks = Math.max(5, plugin.getConfig().getInt("tracking.update_ticks", 20));
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!gameManager.isRunning()) return;
            for (UUID hunterId : gameManager.getInfected()) {
                Player hunter = Bukkit.getPlayer(hunterId);
                if (hunter != null && hunter.isOnline()) {
                    updateHunterCompass(hunter);
                }
            }
        }, 0L, updateTicks);
    }

    public void stopTracking() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        hunterTargets.clear();
        lastNotify.clear();
    }

    public void cycleTarget(Player hunter) {
        if (hunter == null) return;
        if (!gameManager.isInfected(hunter)) return;
        List<UUID> runners = new ArrayList<>();
        for (UUID id : gameManager.getSpeedrunners()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) runners.add(id);
        }
        if (runners.isEmpty()) {
            Msg.send(hunter, "§e[Infected] No speedrunners left to track.");
            return;
        }
        UUID current = hunterTargets.get(hunter.getUniqueId());
        int idx = current == null ? -1 : runners.indexOf(current);
        int next = (idx + 1) % runners.size();
        hunterTargets.put(hunter.getUniqueId(), runners.get(next));
        Player target = Bukkit.getPlayer(runners.get(next));
        String name = target != null ? target.getName() : runners.get(next).toString().substring(0, 8);
        Msg.send(hunter, "§e[Infected] Tracking: §f" + name);
        updateHunterCompass(hunter);
    }

    public void setTarget(Player hunter, UUID targetId) {
        if (hunter == null || targetId == null) return;
        if (!gameManager.isInfected(hunter)) return;
        if (!gameManager.getSpeedrunners().contains(targetId)) return;
        hunterTargets.put(hunter.getUniqueId(), targetId);
        updateHunterCompass(hunter);
    }

    public void updateHunterCompass(Player hunter) {
        if (hunter == null || !hunter.isOnline()) return;
        if (!gameManager.isInfected(hunter)) return;

        UUID hunterId = hunter.getUniqueId();
        UUID targetId = hunterTargets.get(hunterId);

        if (targetId == null || !gameManager.getSpeedrunners().contains(targetId)) {
            // Default to nearest speedrunner
            targetId = findNearestSpeedrunner(hunter);
            if (targetId != null) {
                hunterTargets.put(hunterId, targetId);
            }
        }

        if (targetId == null) {
            return;
        }

        Player target = Bukkit.getPlayer(targetId);
        Location targetLoc = resolveTrackingLocation(hunter, targetId, target);
        if (targetLoc == null) return;

        updateCompassItem(hunter, targetLoc);
    }

    private UUID findNearestSpeedrunner(Player hunter) {
        Location hloc = hunter.getLocation();
        double best = Double.MAX_VALUE;
        UUID bestId = null;
        for (UUID id : gameManager.getSpeedrunners()) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.isOnline()) continue;
            double d = p.getLocation().distanceSquared(hloc);
            if (d < best) {
                best = d;
                bestId = id;
            }
        }
        return bestId;
    }

    private Location resolveTrackingLocation(Player hunter, UUID targetId, Player target) {
        Location hunterLoc = hunter.getLocation();
        World hw = hunterLoc.getWorld();
        if (hw == null) return null;

        if (target != null && target.isOnline()) {
            World tw = target.getWorld();
            if (tw != null) {
                if (tw.equals(hw)) {
                    return target.getLocation();
                }
                if (tw.getEnvironment() == World.Environment.THE_END) {
                    Location portal = gameManager.getLastEndPortalLocation(targetId);
                    if (portal != null) {
                        notifyLastKnown(hunter, "End portal location");
                        return portal;
                    }
                }
                if (tw.getEnvironment() == World.Environment.NETHER && hw.getEnvironment() == World.Environment.NORMAL) {
                    Location lastOver = gameManager.getLastOverworldLocation(targetId);
                    if (lastOver != null) {
                        notifyLastKnown(hunter, "last Overworld position");
                        return lastOver;
                    }
                }
                if (tw.getEnvironment() == World.Environment.NORMAL && hw.getEnvironment() == World.Environment.NETHER) {
                    Location tl = target.getLocation();
                    return new Location(hw, tl.getX() / 8.0, tl.getY(), tl.getZ() / 8.0);
                }
            }
        }

        // Fallback to last known overworld or spawn
        Location lastOver = gameManager.getLastOverworldLocation(targetId);
        if (lastOver != null) {
            notifyLastKnown(hunter, "last Overworld position");
            return lastOver;
        }
        return hw.getSpawnLocation();
    }

    private void updateCompassItem(Player hunter, Location target) {
        if (hunter == null) return;
        if (!hunter.getInventory().contains(Material.COMPASS)) return;

        ItemStack off = hunter.getInventory().getItemInOffHand();
        updateCompassMeta(off, target);
        for (ItemStack it : hunter.getInventory().getContents()) {
            updateCompassMeta(it, target);
        }
    }

    private void updateCompassMeta(ItemStack item, Location target) {
        if (item == null || item.getType() != Material.COMPASS) return;
        if (!(item.getItemMeta() instanceof CompassMeta meta)) return;
        meta.setLodestone(target);
        meta.setLodestoneTracked(false);
        item.setItemMeta(meta);
    }

    private void notifyLastKnown(Player hunter, String reason) {
        if (!plugin.getConfig().getBoolean("tracking.notify_last_known", true)) return;
        long cooldown = Math.max(1, plugin.getConfig().getInt("tracking.message_cooldown_seconds", 5)) * 1000L;
        long now = System.currentTimeMillis();
        long last = lastNotify.getOrDefault(hunter.getUniqueId(), 0L);
        if (now - last < cooldown) return;
        lastNotify.put(hunter.getUniqueId(), now);
        Msg.send(hunter, "§e[Infected] Tracking using " + reason + ".");
    }
}
