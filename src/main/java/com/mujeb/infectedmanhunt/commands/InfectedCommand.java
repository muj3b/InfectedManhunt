package com.mujeb.infectedmanhunt.commands;

import com.mujeb.infectedmanhunt.InfectedManhuntPlugin;
import com.mujeb.infectedmanhunt.game.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

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
            case "track": {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cPlayers only.");
                    return true;
                }
                if (!gm.isInfected(player) && !sender.hasPermission("infectedmanhunt.admin")) {
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
            for (String s : Arrays.asList("start", "stop", "status", "gui", "track", "reload")) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) list.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            list.add("random");
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    list.add(p.getName());
                }
            }
        }
        return list;
    }
}
