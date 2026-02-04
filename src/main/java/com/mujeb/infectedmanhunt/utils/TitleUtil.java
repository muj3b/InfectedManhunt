package com.mujeb.infectedmanhunt.utils;

import org.bukkit.entity.Player;

public final class TitleUtil {
    private TitleUtil() {}

    public static void showTitle(Player player, String title, String subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        if (player == null) return;
        try {
            player.sendTitle(title, subtitle, fadeInTicks, stayTicks, fadeOutTicks);
        } catch (Throwable ignored) {
            try {
                player.sendMessage(title);
                if (subtitle != null && !subtitle.isEmpty()) player.sendMessage(subtitle);
            } catch (Throwable ignored2) {}
        }
    }
}
