package com.mujeb.infectedmanhunt;

import com.mujeb.infectedmanhunt.commands.InfectedCommand;
import com.mujeb.infectedmanhunt.game.GameManager;
import com.mujeb.infectedmanhunt.gui.ChatInputHandler;
import com.mujeb.infectedmanhunt.gui.GuiManager;
import com.mujeb.infectedmanhunt.tracking.TrackerManager;
import com.mujeb.infectedmanhunt.listeners.GameListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class InfectedManhuntPlugin extends JavaPlugin {
    private static InfectedManhuntPlugin instance;

    private GameManager gameManager;
    private TrackerManager trackerManager;
    private GuiManager guiManager;
    private ChatInputHandler chatInputHandler;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.gameManager = new GameManager(this);
        this.trackerManager = new TrackerManager(this, gameManager);
        this.chatInputHandler = new ChatInputHandler(this);
        this.guiManager = new GuiManager(this);

        InfectedCommand cmd = new InfectedCommand(this);
        if (getCommand("infected") != null) {
            getCommand("infected").setExecutor(cmd);
            getCommand("infected").setTabCompleter(cmd);
        }

        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        getServer().getPluginManager().registerEvents(guiManager, this);
        getServer().getPluginManager().registerEvents(chatInputHandler, this);

        getLogger().info("InfectedManhunt enabled");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.resetGame();
        }
        getLogger().info("InfectedManhunt disabled");
    }

    public static InfectedManhuntPlugin getInstance() {
        return instance;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public TrackerManager getTrackerManager() {
        return trackerManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public ChatInputHandler getChatInputHandler() {
        return chatInputHandler;
    }

    public void reloadPluginConfig() {
        reloadConfig();
        if (gameManager != null) {
            gameManager.ensureTeams();
            gameManager.applyTeams();
            if (gameManager.isRunning()) {
                gameManager.removeCompassesFromSpeedrunners();
            }
        }
        if (trackerManager != null) {
            if (gameManager != null && gameManager.isRunning()) {
                trackerManager.startTracking();
            } else {
                trackerManager.stopTracking();
            }
        }
    }
}
