package com.mujeb.infectedmanhunt.utils;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class Msg {
    private Msg() {}

    public static void broadcast(String message) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            try { p.sendMessage(message); } catch (Throwable ignored) {}
        }
        try {
            CommandSender console = Bukkit.getConsoleSender();
            console.sendMessage(message);
        } catch (Throwable ignored) {}
    }

    public static void send(Player player, String message) {
        if (player == null) return;
        try { player.sendMessage(message); } catch (Throwable ignored) {}
    }
}
