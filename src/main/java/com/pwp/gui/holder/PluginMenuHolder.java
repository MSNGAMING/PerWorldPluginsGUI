package com.pwp.gui.holder;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Custom {@link InventoryHolder} for the "Select Plugin" GUI.
 *
 * <p>Storing state here (rather than in the inventory title string) means:
 * <ul>
 *   <li>No string parsing / no risk of title spoofing.</li>
 *   <li>Works correctly even if another plugin opens an inventory whose
 *       title starts with "PWP".</li>
 *   <li>The page number is reliably available during click handling.</li>
 * </ul>
 */
public final class PluginMenuHolder implements InventoryHolder {

    /** 0-indexed page this inventory represents. */
    private final int page;

    /** Set immediately after {@link org.bukkit.Bukkit#createInventory} returns. */
    private Inventory inventory;

    public PluginMenuHolder(int page) {
        this.page = page;
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
