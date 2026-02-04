package com.mujeb.infectedmanhunt.utils;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class GuiCompat {
    private GuiCompat() {}

    private static final InventoryHolder DEFAULT_HOLDER = new InventoryHolder() {
        @Override
        public Inventory getInventory() {
            return null;
        }
    };

    public static Inventory createInventory(InventoryHolder holder, int size, String title) {
        InventoryHolder effective = (holder != null) ? holder : DEFAULT_HOLDER;
        try {
            return Bukkit.createInventory(effective, size, title);
        } catch (Throwable t) {
            return Bukkit.createInventory(effective, size, title);
        }
    }

    public static void setDisplayName(ItemMeta meta, String name) {
        if (meta == null) return;
        try {
            meta.setDisplayName(name);
        } catch (Throwable ignored) {}
    }

    public static void setLore(ItemMeta meta, List<String> legacyLore) {
        if (meta == null) return;
        try {
            meta.setLore(new ArrayList<>(legacyLore));
        } catch (Throwable ignored) {}
    }
}
