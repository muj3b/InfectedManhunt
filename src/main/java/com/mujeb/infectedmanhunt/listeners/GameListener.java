package com.mujeb.infectedmanhunt.listeners;

import com.mujeb.infectedmanhunt.InfectedManhuntPlugin;
import com.mujeb.infectedmanhunt.game.GameManager;
import com.mujeb.infectedmanhunt.utils.Msg;
import com.mujeb.infectedmanhunt.utils.TitleUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.GlowItemFrame;

public class GameListener implements Listener {
    private final InfectedManhuntPlugin plugin;

    public GameListener(InfectedManhuntPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        GameManager gm = plugin.getGameManager();
        if (!gm.isRunning()) return;
        Player player = event.getEntity();
        if (gm.isSpeedrunner(player)) {
            // Infect on death
            gm.convertSpeedrunner(player, true, false, false);
        }
        if (gm.isInfected(player)) {
            event.getDrops().removeIf(item -> item != null && item.getType() == Material.COMPASS);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        GameManager gm = plugin.getGameManager();
        Player player = event.getPlayer();
        if (!gm.isRunning()) return;
        if (gm.isInfected(player)) {
            Location loc = gm.getInfectedRespawnLocation();
            if (loc != null) {
                event.setRespawnLocation(loc);
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                gm.giveCompass(player);
                boolean titles = plugin.getConfig().getBoolean("start.announce_titles", true);
                if (titles) {
                    TitleUtil.showTitle(player, "§c§lINFECTED!", "§7You are now a Hunter", 10, 40, 10);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDragonDeath(EntityDeathEvent event) {
        GameManager gm = plugin.getGameManager();
        if (!gm.isRunning()) return;
        if (!(event.getEntity() instanceof EnderDragon)) return;
        gm.endGame(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPortal(PlayerPortalEvent event) {
        GameManager gm = plugin.getGameManager();
        if (!gm.isRunning()) return;
        Player player = event.getPlayer();
        if (!gm.isSpeedrunner(player)) return;
        Location from = event.getFrom();
        if (from == null || from.getWorld() == null) return;
        if (from.getWorld().getEnvironment() == World.Environment.NORMAL) {
            gm.updateLastOverworldLocation(player, from);
        }
        if (event.getTo() != null && event.getTo().getWorld() != null
                && event.getTo().getWorld().getEnvironment() == World.Environment.THE_END) {
            gm.updateLastEndPortalLocation(player, from);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        GameManager gm = plugin.getGameManager();
        if (!gm.isRunning()) return;
        Player player = event.getPlayer();
        if (gm.isSpeedrunner(player)) {
            if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                gm.updateLastOverworldLocation(player, player.getLocation());
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        GameManager gm = plugin.getGameManager();
        if (!gm.isRunning()) return;
        Player player = event.getPlayer();
        if (!gm.isSpeedrunner(player)) return;
        Location to = event.getTo();
        if (to == null) return;
        if (to.getWorld() != null && to.getWorld().getEnvironment() == World.Environment.NORMAL) {
            gm.updateLastOverworldLocation(player, to);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCompassUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null || event.getItem().getType() != Material.COMPASS) return;
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();
        if (!gm.isRunning()) return;
        if (!gm.isInfected(player)) return;
        if (player.isSneaking()) {
            plugin.getGuiManager().openTargetPicker(player);
        } else {
            plugin.getTrackerManager().cycleTarget(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Keep teams updated for late joiners
        GameManager gm = plugin.getGameManager();
        if (gm.isRunning()) {
            if (!gm.isInfected(event.getPlayer()) && !gm.isSpeedrunner(event.getPlayer())) {
                gm.addLateJoinerAsSpeedrunner(event.getPlayer());
                Msg.send(event.getPlayer(), "§e[Infected] Game in progress. You joined as a speedrunner.");
            }
            gm.assignScoreboard(event.getPlayer());
            gm.applyTeams();
            if (gm.isInfected(event.getPlayer())) {
                Bukkit.getScheduler().runTask(plugin, () -> gm.giveCompass(event.getPlayer()));
            } else if (gm.isSpeedrunner(event.getPlayer())) {
                Bukkit.getScheduler().runTask(plugin, () -> gm.stripCompass(event.getPlayer()));
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        GameManager gm = plugin.getGameManager();
        if (!gm.isRunning()) return;
        Player player = event.getPlayer();
        if (gm.isSpeedrunner(player)) {
            gm.convertSpeedrunner(player, true, false, false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        GameManager gm = plugin.getGameManager();
        if (!gm.isRunning()) return;
        if (!gm.isSpeedrunner(player)) return;
        if (event.getRecipe() == null || event.getRecipe().getResult() == null) return;
        if (event.getRecipe().getResult().getType() == Material.COMPASS) {
            event.setCancelled(true);
            player.sendMessage("§c[Infected] Speedrunners cannot craft compasses.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getGameManager().isRunning()) return;
        if (!plugin.getGameManager().isInfected(player)) return;
        ItemStack hand = event.getPlayer().getInventory().getItemInMainHand();
        ItemStack off = event.getPlayer().getInventory().getItemInOffHand();
        boolean holdingCompass = (hand != null && hand.getType() == Material.COMPASS) ||
                (off != null && off.getType() == Material.COMPASS);
        if (!holdingCompass) return;
        if (event.getRightClicked() instanceof ItemFrame || event.getRightClicked() instanceof GlowItemFrame
                || event.getRightClicked() instanceof ArmorStand) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getGameManager().isRunning()) return;
        if (!plugin.getGameManager().isInfected(player)) return;
        ItemStack hand = event.getPlayer().getInventory().getItemInMainHand();
        ItemStack off = event.getPlayer().getInventory().getItemInOffHand();
        boolean holdingCompass = (hand != null && hand.getType() == Material.COMPASS) ||
                (off != null && off.getType() == Material.COMPASS);
        if (!holdingCompass) return;
        if (event.getRightClicked() instanceof ItemFrame || event.getRightClicked() instanceof GlowItemFrame
                || event.getRightClicked() instanceof ArmorStand) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        GameManager gm = plugin.getGameManager();
        if (!gm.isRunning()) return;
        Player player = event.getPlayer();
        if (!gm.isInfected(player)) return;
        ItemStack stack = event.getItemDrop().getItemStack();
        if (stack != null && stack.getType() == Material.COMPASS) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        GameManager gm = plugin.getGameManager();
        if (!gm.isRunning()) return;
        ItemStack stack = event.getItem().getItemStack();
        if (stack != null && stack.getType() == Material.COMPASS) {
            if (!gm.isInfected(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (plugin.getGuiManager().isMenuInventory(event.getView().getTopInventory())) return;
        GameManager gm = plugin.getGameManager();
        if (!gm.isRunning()) return;

        ItemStack clicked = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        boolean clickedCompass = clicked != null && clicked.getType() == Material.COMPASS;
        boolean cursorCompass = cursor != null && cursor.getType() == Material.COMPASS;

        if (gm.isInfected(player)) {
            boolean inPlayerInv = event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory());
            if (clickedCompass || cursorCompass) {
                if (!inPlayerInv || event.isShiftClick() || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    event.setCancelled(true);
                    return;
                }
            }
        } else {
            if (clickedCompass || cursorCompass) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (plugin.getGuiManager().isMenuInventory(event.getView().getTopInventory())) return;
        GameManager gm = plugin.getGameManager();
        if (!gm.isRunning()) return;
        ItemStack cursor = event.getOldCursor();
        if (cursor == null || cursor.getType() != Material.COMPASS) return;
        if (gm.isInfected(player)) {
            int topSize = event.getView().getTopInventory() != null ? event.getView().getTopInventory().getSize() : 0;
            for (int raw : event.getRawSlots()) {
                if (raw < topSize) {
                    event.setCancelled(true);
                    return;
                }
            }
        } else {
            event.setCancelled(true);
        }
    }
}
