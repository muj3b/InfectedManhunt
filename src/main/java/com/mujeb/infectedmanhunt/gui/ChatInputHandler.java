package com.mujeb.infectedmanhunt.gui;

import com.mujeb.infectedmanhunt.InfectedManhuntPlugin;
import com.mujeb.infectedmanhunt.utils.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ChatInputHandler implements Listener {
    private final InfectedManhuntPlugin plugin;
    private final Map<UUID, Consumer<String>> callbacks = new HashMap<>();

    public ChatInputHandler(InfectedManhuntPlugin plugin) {
        this.plugin = plugin;
        registerPaperAsyncChatHook();
    }

    public void prompt(Player player, String prompt, Consumer<String> onSubmit) {
        if (player == null) return;
        callbacks.put(player.getUniqueId(), onSubmit);
        Msg.send(player, prompt);
    }

    public void clear(Player player) {
        if (player == null) return;
        callbacks.remove(player.getUniqueId());
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (!callbacks.containsKey(id)) return;
        event.setCancelled(true);
        String msg = event.getMessage();
        plugin.getServer().getScheduler().runTask(plugin, () -> handleInput(player, msg));
    }

    private void handleInput(Player player, String raw) {
        Consumer<String> cb = callbacks.remove(player.getUniqueId());
        if (cb == null) return;
        String msg = raw == null ? "" : raw.trim();
        if (msg.equalsIgnoreCase("cancel")) {
            Msg.send(player, "§cCancelled.");
            return;
        }
        cb.accept(msg);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        callbacks.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        callbacks.remove(event.getPlayer().getUniqueId());
    }

    @SuppressWarnings("unchecked")
    private void registerPaperAsyncChatHook() {
        try {
            final Class<?> asyncChatCls = Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
            org.bukkit.plugin.EventExecutor exec = (listener, event) -> {
                if (!asyncChatCls.isInstance(event)) return;
                Object ev = event;
                Player player;
                String msg;
                try {
                    player = (Player) asyncChatCls.getMethod("getPlayer").invoke(ev);
                } catch (Throwable t) {
                    return;
                }
                if (!callbacks.containsKey(player.getUniqueId())) return;
                try {
                    Object component = asyncChatCls.getMethod("message").invoke(ev);
                    Class<?> serCls = Class.forName("net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer");
                    Object serializer = serCls.getMethod("plainText").invoke(null);
                    msg = String.valueOf(serializer.getClass()
                            .getMethod("serialize", Class.forName("net.kyori.adventure.text.Component"))
                            .invoke(serializer, component));
                } catch (Throwable t) {
                    msg = "";
                }
                try {
                    asyncChatCls.getMethod("setCancelled", boolean.class).invoke(ev, true);
                } catch (Throwable ignored) {}
                final String finalMsg = msg;
                plugin.getServer().getScheduler().runTask(plugin, () -> handleInput(player, finalMsg));
            };
            plugin.getServer().getPluginManager().registerEvent(
                    (Class<? extends org.bukkit.event.Event>) asyncChatCls,
                    this,
                    org.bukkit.event.EventPriority.NORMAL,
                    exec,
                    plugin,
                    true);
        } catch (ClassNotFoundException ignored) {
            // Not Paper
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to hook Paper AsyncChatEvent: " + t.getMessage());
        }
    }
}
