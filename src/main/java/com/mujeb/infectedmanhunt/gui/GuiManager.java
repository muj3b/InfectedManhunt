package com.mujeb.infectedmanhunt.gui;

import com.mujeb.infectedmanhunt.InfectedManhuntPlugin;
import com.mujeb.infectedmanhunt.game.GameManager;
import com.mujeb.infectedmanhunt.tracking.TrackerManager;
import com.mujeb.infectedmanhunt.utils.GuiCompat;
import com.mujeb.infectedmanhunt.utils.Msg;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.function.Consumer;

public class GuiManager implements Listener {
    private final InfectedManhuntPlugin plugin;
    private final NamespacedKey buttonKey;
    private final Map<UUID, MenuSession> sessions = new HashMap<>();

    private static final List<ChatColor> COLOR_CYCLE = List.of(
            ChatColor.DARK_RED, ChatColor.RED, ChatColor.GOLD, ChatColor.YELLOW,
            ChatColor.GREEN, ChatColor.DARK_GREEN, ChatColor.AQUA, ChatColor.DARK_AQUA,
            ChatColor.BLUE, ChatColor.DARK_BLUE, ChatColor.LIGHT_PURPLE, ChatColor.DARK_PURPLE,
            ChatColor.WHITE, ChatColor.GRAY, ChatColor.DARK_GRAY
    );

    public GuiManager(InfectedManhuntPlugin plugin) {
        this.plugin = plugin;
        this.buttonKey = new NamespacedKey(plugin, "infected_button");
    }

    public void openMain(Player player) {
        Inventory inv = GuiCompat.createInventory(null, 45, "§6Infected Manhunt");
        MenuSession session = new MenuSession(MenuKey.MAIN, inv);

        fill(inv, Material.BLACK_STAINED_GLASS_PANE);

        GameManager gm = plugin.getGameManager();

        ItemStack status = icon(Material.PAPER, "§eStatus", List.of(
                "§7State: §f" + gm.getState(),
                "§7Speedrunners: §f" + gm.getSpeedrunners().size(),
                "§7Infected: §f" + gm.getInfected().size()
        ));
        addButton(session, 4, "status", status, ctx -> {});

        ItemStack startRandom = icon(Material.LIME_CONCRETE, "§aStart (Random)", List.of("§7Pick a random infected"));
        addButton(session, 10, "start_random", startRandom, ctx -> {
            if (!ctx.player.hasPermission("infectedmanhunt.admin")) {
                Msg.send(ctx.player, "§cNo permission.");
                return;
            }
            gm.startGameRandom();
            reopen(ctx.player);
        });

        ItemStack startSelect = icon(Material.PLAYER_HEAD, "§aStart (Select)", List.of("§7Choose infected player"));
        addButton(session, 11, "start_select", startSelect, ctx -> {
            if (!ctx.player.hasPermission("infectedmanhunt.admin")) {
                Msg.send(ctx.player, "§cNo permission.");
                return;
            }
            openStartSelect(ctx.player);
        });

        ItemStack stop = icon(Material.RED_CONCRETE, "§cStop/Reset", List.of("§7End and reset game"));
        addButton(session, 12, "stop", stop, ctx -> {
            if (!ctx.player.hasPermission("infectedmanhunt.admin")) {
                Msg.send(ctx.player, "§cNo permission.");
                return;
            }
            gm.resetGame();
            reopen(ctx.player);
        });

        ItemStack tracking = icon(Material.COMPASS, "§bTracking Settings", List.of("§7Compass behavior"));
        addButton(session, 14, "tracking", tracking, ctx -> openTracking(ctx.player));

        ItemStack respawn = icon(Material.RESPAWN_ANCHOR, "§dRespawn Settings", List.of("§7Infected respawn mode"));
        addButton(session, 15, "respawn", respawn, ctx -> openRespawn(ctx.player));

        ItemStack teams = icon(Material.NAME_TAG, "§6Team Settings", List.of("§7Colors and prefixes"));
        addButton(session, 16, "teams", teams, ctx -> openTeams(ctx.player));

        boolean teleportStart = plugin.getConfig().getBoolean("start.teleport_to_spawn", true);
        ItemStack tpToggle = icon(teleportStart ? Material.LIME_DYE : Material.GRAY_DYE,
                "§eTeleport On Start", List.of("§7Currently: " + (teleportStart ? "§aON" : "§cOFF")));
        addButton(session, 20, "tp_start", tpToggle, ctx -> {
            boolean next = !plugin.getConfig().getBoolean("start.teleport_to_spawn", true);
            plugin.getConfig().set("start.teleport_to_spawn", next);
            plugin.saveConfig();
            reopen(ctx.player);
        });

        boolean titles = plugin.getConfig().getBoolean("start.announce_titles", true);
        ItemStack titleToggle = icon(titles ? Material.LIME_DYE : Material.GRAY_DYE,
                "§eAnnouncement Titles", List.of("§7Currently: " + (titles ? "§aON" : "§cOFF")));
        addButton(session, 21, "titles", titleToggle, ctx -> {
            boolean next = !plugin.getConfig().getBoolean("start.announce_titles", true);
            plugin.getConfig().set("start.announce_titles", next);
            plugin.saveConfig();
            reopen(ctx.player);
        });

        ItemStack reload = icon(Material.WRITABLE_BOOK, "§bReload Config", List.of("§7Reload from disk"));
        addButton(session, 22, "reload", reload, ctx -> {
            if (!ctx.player.hasPermission("infectedmanhunt.admin")) {
                Msg.send(ctx.player, "§cNo permission.");
                return;
            }
            plugin.reloadPluginConfig();
            reopen(ctx.player);
        });

        if (gm.isInfected(player)) {
            ItemStack picker = icon(Material.TARGET, "§aTarget Picker", List.of("§7Choose who to track"));
            addButton(session, 23, "target_picker", picker, ctx -> openTargetPicker(ctx.player));
        }

        ItemStack close = icon(Material.BARRIER, "§7Close", List.of());
        addButton(session, 40, "close", close, ctx -> ctx.player.closeInventory());

        openSession(player, session);
    }

    public void openStartSelect(Player player) {
        Inventory inv = GuiCompat.createInventory(null, 54, "§aSelect Infected");
        MenuSession session = new MenuSession(MenuKey.START_SELECT, inv);
        fill(inv, Material.BLACK_STAINED_GLASS_PANE);

        int slot = 10;
        for (Player online : Bukkit.getOnlinePlayers()) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(online);
            GuiCompat.setDisplayName(meta, "§f" + online.getName());
            head.setItemMeta(meta);

            addButton(session, slot, "start_" + online.getUniqueId(), head, ctx -> {
                if (!ctx.player.hasPermission("infectedmanhunt.admin")) {
                    Msg.send(ctx.player, "§cNo permission.");
                    return;
                }
                plugin.getGameManager().startGame(online);
                openMain(ctx.player);
            });

            slot++;
            if ((slot + 1) % 9 == 0) slot += 2;
            if (slot >= 53) break;
        }

        ItemStack back = icon(Material.ARROW, "§7Back", List.of());
        addButton(session, 49, "back", back, ctx -> openMain(ctx.player));

        openSession(player, session);
    }

    public void openTracking(Player player) {
        Inventory inv = GuiCompat.createInventory(null, 36, "§bTracking Settings");
        MenuSession session = new MenuSession(MenuKey.TRACKING, inv);
        fill(inv, Material.BLACK_STAINED_GLASS_PANE);

        int updateTicks = plugin.getConfig().getInt("tracking.update_ticks", 20);
        ItemStack update = icon(Material.REPEATER, "§eUpdate Ticks", List.of(
                "§7Current: §f" + updateTicks,
                "§7Left click: +5",
                "§7Right click: -5",
                "§7Shift: ±1"
        ));
        addButton(session, 11, "update_ticks", update, ctx -> {
            adjustInt("tracking.update_ticks", ctx.click, 1, 200, 5);
            if (plugin.getGameManager().isRunning()) {
                plugin.getTrackerManager().startTracking();
            }
            reopen(ctx.player);
        });

        int cooldown = plugin.getConfig().getInt("tracking.message_cooldown_seconds", 5);
        ItemStack cd = icon(Material.CLOCK, "§eLast-Known Message Cooldown", List.of(
                "§7Current: §f" + cooldown + "s",
                "§7Left click: +1",
                "§7Right click: -1",
                "§7Shift: ±5"
        ));
        addButton(session, 13, "cooldown", cd, ctx -> {
            adjustInt("tracking.message_cooldown_seconds", ctx.click, 1, 60, 1, 5);
            reopen(ctx.player);
        });

        boolean notify = plugin.getConfig().getBoolean("tracking.notify_last_known", true);
        ItemStack notifyItem = icon(notify ? Material.LIME_DYE : Material.GRAY_DYE,
                "§eNotify Last Known", List.of("§7Currently: " + (notify ? "§aON" : "§cOFF")));
        addButton(session, 15, "notify", notifyItem, ctx -> {
            boolean next = !plugin.getConfig().getBoolean("tracking.notify_last_known", true);
            plugin.getConfig().set("tracking.notify_last_known", next);
            plugin.saveConfig();
            reopen(ctx.player);
        });

        ItemStack back = icon(Material.ARROW, "§7Back", List.of());
        addButton(session, 31, "back", back, ctx -> openMain(ctx.player));

        openSession(player, session);
    }

    public void openRespawn(Player player) {
        Inventory inv = GuiCompat.createInventory(null, 36, "§dRespawn Settings");
        MenuSession session = new MenuSession(MenuKey.RESPAWN, inv);
        fill(inv, Material.BLACK_STAINED_GLASS_PANE);

        String mode = String.valueOf(plugin.getConfig().getString("respawn.mode", "vanilla")).toLowerCase(Locale.ROOT);
        ItemStack modeItem = icon(Material.RESPAWN_ANCHOR, "§eRespawn Mode", List.of(
                "§7Current: §f" + mode,
                "§7Click to cycle"
        ));
        addButton(session, 11, "mode", modeItem, ctx -> {
            String current = String.valueOf(plugin.getConfig().getString("respawn.mode", "vanilla")).toLowerCase(Locale.ROOT);
            String next = switch (current) {
                case "vanilla" -> "world_spawn";
                case "world_spawn" -> "fixed";
                default -> "vanilla";
            };
            plugin.getConfig().set("respawn.mode", next);
            plugin.saveConfig();
            reopen(ctx.player);
        });

        ItemStack setFixed = icon(Material.ENDER_PEARL, "§bSet Fixed Location", List.of(
                "§7Click to use your position"
        ));
        addButton(session, 13, "set_fixed", setFixed, ctx -> {
            if (ctx.player.getWorld() != null) {
                plugin.getConfig().set("respawn.world", ctx.player.getWorld().getName());
                plugin.getConfig().set("respawn.x", ctx.player.getLocation().getX());
                plugin.getConfig().set("respawn.y", ctx.player.getLocation().getY());
                plugin.getConfig().set("respawn.z", ctx.player.getLocation().getZ());
                plugin.saveConfig();
                Msg.send(ctx.player, "§aFixed respawn updated.");
            }
            reopen(ctx.player);
        });

        String world = plugin.getConfig().getString("respawn.world", "world");
        double x = plugin.getConfig().getDouble("respawn.x", 0.5);
        double y = plugin.getConfig().getDouble("respawn.y", 64.0);
        double z = plugin.getConfig().getDouble("respawn.z", 0.5);
        ItemStack fixedInfo = icon(Material.MAP, "§7Fixed Location", List.of(
                "§7World: §f" + world,
                String.format(Locale.ROOT, "§7XYZ: §f%.1f %.1f %.1f", x, y, z)
        ));
        addButton(session, 15, "fixed_info", fixedInfo, ctx -> {});

        ItemStack back = icon(Material.ARROW, "§7Back", List.of());
        addButton(session, 31, "back", back, ctx -> openMain(ctx.player));

        openSession(player, session);
    }

    public void openTeams(Player player) {
        Inventory inv = GuiCompat.createInventory(null, 45, "§6Team Settings");
        MenuSession session = new MenuSession(MenuKey.TEAMS, inv);
        fill(inv, Material.BLACK_STAINED_GLASS_PANE);

        String runnerColor = plugin.getConfig().getString("teams.speedrunner_color", "GREEN");
        ItemStack rc = icon(Material.LIME_DYE, "§aSpeedrunner Color", List.of(
                "§7Current: §f" + runnerColor,
                "§7Click to cycle"
        ));
        addButton(session, 10, "runner_color", rc, ctx -> {
            cycleColor("teams.speedrunner_color", true);
            reopen(ctx.player);
        });

        String infColor = plugin.getConfig().getString("teams.infected_color", "RED");
        ItemStack ic = icon(Material.RED_DYE, "§cInfected Color", List.of(
                "§7Current: §f" + infColor,
                "§7Click to cycle"
        ));
        addButton(session, 12, "infected_color", ic, ctx -> {
            cycleColor("teams.infected_color", false);
            reopen(ctx.player);
        });

        ItemStack runnerPrefix = icon(Material.NAME_TAG, "§aSpeedrunner Prefix", List.of(
                "§7Current: §f" + plugin.getConfig().getString("teams.speedrunner_prefix", "§a[R] "),
                "§7Click to edit in chat"
        ));
        addButton(session, 14, "runner_prefix", runnerPrefix, ctx -> {
            ctx.player.closeInventory();
            plugin.getChatInputHandler().prompt(ctx.player, "§eType new speedrunner prefix (or 'cancel'):", msg -> {
                plugin.getConfig().set("teams.speedrunner_prefix", msg);
                plugin.saveConfig();
                plugin.getGameManager().applyTeams();
                openTeams(ctx.player);
            });
        });

        ItemStack infectedPrefix = icon(Material.NAME_TAG, "§cInfected Prefix", List.of(
                "§7Current: §f" + plugin.getConfig().getString("teams.infected_prefix", "§c[INF] "),
                "§7Click to edit in chat"
        ));
        addButton(session, 16, "infected_prefix", infectedPrefix, ctx -> {
            ctx.player.closeInventory();
            plugin.getChatInputHandler().prompt(ctx.player, "§eType new infected prefix (or 'cancel'):", msg -> {
                plugin.getConfig().set("teams.infected_prefix", msg);
                plugin.saveConfig();
                plugin.getGameManager().applyTeams();
                openTeams(ctx.player);
            });
        });

        ItemStack apply = icon(Material.EMERALD, "§aApply Team Styles", List.of("§7Refresh team colors/prefixes"));
        addButton(session, 20, "apply", apply, ctx -> {
            plugin.getGameManager().ensureTeams();
            plugin.getGameManager().applyTeams();
            reopen(ctx.player);
        });

        ItemStack back = icon(Material.ARROW, "§7Back", List.of());
        addButton(session, 40, "back", back, ctx -> openMain(ctx.player));

        openSession(player, session);
    }

    public void openTargetPicker(Player player) {
        Inventory inv = GuiCompat.createInventory(null, 54, "§aPick Target");
        MenuSession session = new MenuSession(MenuKey.TARGET_PICKER, inv);
        fill(inv, Material.BLACK_STAINED_GLASS_PANE);

        int slot = 10;
        for (UUID id : plugin.getGameManager().getSpeedrunners()) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.isOnline()) continue;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(p);
            GuiCompat.setDisplayName(meta, "§f" + p.getName());
            head.setItemMeta(meta);

            addButton(session, slot, "target_" + id, head, ctx -> {
                plugin.getTrackerManager().setTarget(ctx.player, id);
                Msg.send(ctx.player, "§e[Infected] Tracking: §f" + p.getName());
                ctx.player.closeInventory();
            });

            slot++;
            if ((slot + 1) % 9 == 0) slot += 2;
            if (slot >= 53) break;
        }

        ItemStack back = icon(Material.ARROW, "§7Back", List.of());
        addButton(session, 49, "back", back, ctx -> openMain(ctx.player));

        openSession(player, session);
    }

    public boolean isMenuInventory(Inventory inv) {
        if (inv == null) return false;
        for (MenuSession session : sessions.values()) {
            if (session.inventory.equals(inv)) return true;
        }
        return false;
    }

    private void openSession(Player player, MenuSession session) {
        sessions.put(player.getUniqueId(), session);
        player.openInventory(session.inventory);
    }

    private void reopen(Player player) {
        MenuSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            openMain(player);
            return;
        }
        switch (session.key) {
            case MAIN -> openMain(player);
            case START_SELECT -> openStartSelect(player);
            case TRACKING -> openTracking(player);
            case RESPAWN -> openRespawn(player);
            case TEAMS -> openTeams(player);
            case TARGET_PICKER -> openTargetPicker(player);
        }
    }

    private void fill(Inventory inv, Material filler) {
        ItemStack item = new ItemStack(filler);
        ItemMeta meta = item.getItemMeta();
        GuiCompat.setDisplayName(meta, " ");
        item.setItemMeta(meta);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, item);
    }

    private ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        GuiCompat.setDisplayName(meta, name);
        GuiCompat.setLore(meta, lore);
        item.setItemMeta(meta);
        return item;
    }

    private void addButton(MenuSession session, int slot, String id, ItemStack icon, Consumer<ClickContext> action) {
        ItemMeta meta = icon.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(buttonKey, PersistentDataType.STRING, id);
        icon.setItemMeta(meta);
        session.inventory.setItem(slot, icon);
        session.actions.put(id, action);
    }

    private void adjustInt(String path, ClickType click, int min, int max, int step) {
        adjustInt(path, click, min, max, step, 1);
    }

    private void adjustInt(String path, ClickType click, int min, int max, int step, int shiftStep) {
        int current = plugin.getConfig().getInt(path, min);
        int delta = (click.isShiftClick() ? shiftStep : step);
        if (click.isRightClick()) delta = -delta;
        int next = Math.max(min, Math.min(max, current + delta));
        plugin.getConfig().set(path, next);
        plugin.saveConfig();
    }

    private void cycleColor(String path, boolean speedrunner) {
        String cur = plugin.getConfig().getString(path, speedrunner ? "GREEN" : "RED");
        ChatColor current;
        try {
            current = ChatColor.valueOf(cur.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            current = speedrunner ? ChatColor.GREEN : ChatColor.RED;
        }
        int idx = COLOR_CYCLE.indexOf(current);
        if (idx < 0) idx = 0;
        int next = (idx + 1) % COLOR_CYCLE.size();
        plugin.getConfig().set(path, COLOR_CYCLE.get(next).name());
        plugin.saveConfig();
        plugin.getGameManager().applyTeams();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        MenuSession session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (!event.getView().getTopInventory().equals(session.inventory)) return;
        event.setCancelled(true);

        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) return;
        ItemMeta meta = current.getItemMeta();
        if (meta == null) return;
        String id = meta.getPersistentDataContainer().get(buttonKey, PersistentDataType.STRING);
        if (id == null) return;
        Consumer<ClickContext> action = session.actions.get(id);
        if (action == null) return;
        action.accept(new ClickContext(player, event.getClick()));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        MenuSession session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (event.getView().getTopInventory().equals(session.inventory)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        MenuSession session = sessions.get(player.getUniqueId());
        if (session != null && session.inventory.equals(event.getInventory())) {
            sessions.remove(player.getUniqueId());
        }
    }

    private static class MenuSession {
        final MenuKey key;
        final Inventory inventory;
        final Map<String, Consumer<ClickContext>> actions = new HashMap<>();

        MenuSession(MenuKey key, Inventory inventory) {
            this.key = key;
            this.inventory = inventory;
        }
    }

    private static class ClickContext {
        final Player player;
        final ClickType click;

        ClickContext(Player player, ClickType click) {
            this.player = player;
            this.click = click;
        }
    }
}
