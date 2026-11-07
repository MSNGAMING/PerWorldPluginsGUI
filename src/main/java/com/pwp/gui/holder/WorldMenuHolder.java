package com.pwp.gui.holder;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Custom {@link InventoryHolder} for the "Select World" GUI.
 *
 * <p>Stores the target plugin name, the current world page, and the
 * return page of the main plugin menu so that hitting Back or ESC
 * restores the exact scroll position.
 */
public final class WorldMenuHolder implements InventoryHolder {

    private final String pluginName;
    private final int page;
    private final int returnPluginPage;
    private Inventory inventory;

    public WorldMenuHolder(String pluginName, int page, int returnPluginPage) {
        this.pluginName = pluginName;
        this.page = page;
        this.returnPluginPage = returnPluginPage;
    }

    public String getPluginName() {
        return pluginName;
    }

    public int getPage() {
        return page;
    }

    public int getReturnPluginPage() {
        return returnPluginPage;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
