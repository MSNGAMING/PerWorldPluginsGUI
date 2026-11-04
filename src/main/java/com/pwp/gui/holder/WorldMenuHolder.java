package com.pwp.gui.holder;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Custom {@link InventoryHolder} for the "Select World" GUI.
 *
 * <p>Stores the target plugin name and the current page number so the
 * click handler never needs to parse the inventory title string.
 */
public final class WorldMenuHolder implements InventoryHolder {

    private final String pluginName;
    private final int page;
    private Inventory inventory;

    public WorldMenuHolder(String pluginName, int page) {
        this.pluginName = pluginName;
        this.page = page;
    }

    public String getPluginName() {
        return pluginName;
    }

    public int getPage() {
        return page;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
