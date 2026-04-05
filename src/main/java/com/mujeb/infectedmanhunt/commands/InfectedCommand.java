package com.mujeb.infectedmanhunt.commands;

import com.mujeb.infectedmanhunt.InfectedManhuntPlugin;
import com.mujeb.infectedmanhunt.game.GameManager;
import com.mujeb.infectedmanhunt.game.ParticipantRole;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class InfectedCommand implements CommandExecutor, TabCompleter {
    private final InfectedManhuntPlugin plugin;

    public InfectedCommand(InfectedManhuntPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6InfectedManhunt commands:");
            sender.sendMessage("§e/infected start <player> §7Start with chosen infected");
            sender.sendMessage("§e/infected start random §7Start with random infected");
            sender.sendMessage("§e/infected stop §7Stop and reset game");
            sender.sendMessage("§e/infected status §7Show current game status");
            sender.sendMessage("§e/infected gui §7Open admin GUI");
            sender.sendMessage("§e/infected team <player> <runner|infected> §7Move a player between teams");
            sender.sendMessage("§e/infected track §7Open target picker");
            sender.sendMessage("§e/infected reload §7Reload config");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        GameManager gm = plugin.getGameManager();

        switch (sub) {
            case "start": {
                if (!sender.hasPermission("infectedmanhunt.admin")) {
                    sender.sendMessage("§cYou do not have permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /infected start <player|random>");
                    return true;
                }
                String target = args[1];
                if (target.equalsIgnoreCase("random")) {
                    gm.startGameRandom();
                    return true;
                }
                Player p = Bukkit.getPlayerExact(target);
                if (p == null) {
                    sender.sendMessage("§cPlayer not found: " + target);
                    return true;
                }
                gm.startGame(p);
                return true;
            }
            case "stop": {
                if (!sender.hasPermission("infectedmanhunt.admin")) {
                    sender.sendMessage("§cYou do not have permission.");
                    return true;
                }
                gm.resetGame();
                sender.sendMessage("§aInfectedManhunt reset.");
                return true;
            }
            case "status": {
                sender.sendMessage("§eState: §f" + gm.getState());
                sender.sendMessage("§eSpeedrunners: §f" + gm.getSpeedrunners().size());
                sender.sendMessage("§eInfected: §f" + gm.getInfected().size());
                return true;
            }
            case "gui": {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cPlayers only.");
                    return true;
                }
                if (!sender.hasPermission("infectedmanhunt.admin")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                plugin.getGuiManager().openMain(player);
                return true;
            }
            case "team":
            case "setteam": {
                if (!sender.hasPermission("infectedmanhunt.admin")) {
                    sender.sendMessage("§cYou do not have permission.");
                    return true;
                }
                if (!gm.isRunning()) {
                    sender.sendMessage("§cNo active game is running.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /infected team <player> <runner|infected>");
                    return true;
                }

                UUID targetId = resolveTargetId(args[1], gm);
                if (targetId == null) {
                    sender.sendMessage("§cPlayer not found in the active match: " + args[1]);
                    return true;
                }

                ParticipantRole targetRole = parseRole(args[2]);
                if (targetRole == null) {
                    sender.sendMessage("§cRole must be 'runner' or 'infected'.");
                    return true;
                }

                ParticipantRole currentRole = gm.getParticipantRole(targetId);
                String targetName = resolveTargetName(targetId);
                if (currentRole == targetRole) {
                    sender.sendMessage("§e" + targetName + " is already " + describeRole(targetRole) + ".");
                    return true;
                }
                if (targetRole == ParticipantRole.SPEEDRUNNER && currentRole == ParticipantRole.INFECTED && !gm.canConvertToSpeedrunner(targetId)) {
                    sender.sendMessage("§cYou must keep at least one infected hunter in an active game.");
                    return true;
                }

                boolean changed = gm.setParticipantRole(targetId, targetRole, true, true, true);
                if (!changed) {
                    sender.sendMessage("§cUnable to update " + targetName + ".");
                    return true;
                }

                sender.sendMessage("§aSet " + targetName + " to " + describeRole(targetRole) + ".");
                return true;
            }
            case "track": {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cPlayers only.");
                    return true;
                }
                if (!gm.isInfected(player)) {
                    sender.sendMessage("§cOnly infected hunters can use this.");
                    return true;
                }
                plugin.getGuiManager().openTargetPicker(player);
                return true;
            }
            case "reload": {
                if (!sender.hasPermission("infectedmanhunt.admin")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                plugin.reloadPluginConfig();
                sender.sendMessage("§aInfectedManhunt config reloaded.");
                return true;
            }
            default:
                sender.sendMessage("§cUnknown subcommand. Use /infected for help.");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            for (String s : Arrays.asList("start", "stop", "status", "gui", "team", "track", "reload")) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) list.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            list.add("random");
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    list.add(p.getName());
                }
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("team") || args[0].equalsIgnoreCase("setteam"))) {
            for (String name : getKnownPlayerNames(plugin.getGameManager())) {
                if (name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    list.add(name);
                }
            }
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("team") || args[0].equalsIgnoreCase("setteam"))) {
            for (String role : Arrays.asList("runner", "infected")) {
                if (role.startsWith(args[2].toLowerCase(Locale.ROOT))) {
                    list.add(role);
                }
            }
        }
        return list;
    }

    private ParticipantRole parseRole(String raw) {
        if (raw == null) return null;
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "runner", "speedrunner" -> ParticipantRole.SPEEDRUNNER;
            case "infected", "hunter" -> ParticipantRole.INFECTED;
            default -> null;
        };
    }

    private UUID resolveTargetId(String rawName, GameManager gm) {
        Player online = Bukkit.getPlayerExact(rawName);
        if (online != null) {
            return online.getUniqueId();
        }

        for (UUID id : gm.getParticipants()) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(id);
            if (offline.getName() != null && offline.getName().equalsIgnoreCase(rawName)) {
                return id;
            }
        }
        return null;
    }

    private Set<String> getKnownPlayerNames(GameManager gm) {
        Set<String> names = new LinkedHashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        for (UUID id : gm.getParticipants()) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(id);
            if (offline.getName() != null && !offline.getName().isBlank()) {
                names.add(offline.getName());
            }
        }
        return names;
    }

    private String resolveTargetName(UUID playerId) {
        Player online = Bukkit.getPlayer(playerId);
        if (online != null) {
            return online.getName();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerId);
        if (offline.getName() != null && !offline.getName().isBlank()) {
            return offline.getName();
        }
        return playerId.toString();
    }

    private String describeRole(ParticipantRole role) {
        return role == ParticipantRole.INFECTED ? "infected" : "runner";
    }
}
