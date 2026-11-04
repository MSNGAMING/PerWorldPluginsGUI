package com.pwp.gui.listener;

import com.pwp.gui.config.WorldPluginConfig;
import com.pwp.gui.holder.PluginMenuHolder;
import com.pwp.gui.holder.WorldMenuHolder;
import com.pwp.gui.menu.PluginSelectMenu;
import com.pwp.gui.menu.WorldSelectMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

/**
 * Handles all click interactions within the PerWorldPluginsGUI inventories.
 *
 * <p>GUIs are identified by their custom {@link InventoryHolder} type, never
 * by title string, to avoid false positives with other plugins.
 *
 * <p>The event is cancelled for <em>any</em> click while one of our GUIs is
 * open (including the player's own inventory in the bottom half) to prevent
 * item-duplication exploits.
 */
public final class InventoryListener implements Listener {

    private final WorldPluginConfig config;
    private final Plugin self;

    public InventoryListener(WorldPluginConfig config, Plugin self) {
        this.config = config;
        this.self   = self;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Identify our GUI by its holder type.
        InventoryHolder topHolder = event.getView().getTopInventory().getHolder();
        if (!(topHolder instanceof PluginMenuHolder)
                && !(topHolder instanceof WorldMenuHolder)) return;

        // Always cancel to prevent item movement while our GUI is open.
        event.setCancelled(true);

        // Only process clicks that land inside our custom GUI (not the player's inventory).
        if (event.getClickedInventory() == null
                || event.getClickedInventory() != event.getView().getTopInventory()) return;

        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        ItemMeta meta = clicked.getItemMeta();

        if (topHolder instanceof PluginMenuHolder pluginHolder) {
            handlePluginMenu(player, pluginHolder, slot, meta);
        } else {
            handleWorldMenu(player, (WorldMenuHolder) topHolder, slot, meta);
        }
    }

    // ── Handler: plugin-select menu ──────────────────────────────────────

    private void handlePluginMenu(Player player, PluginMenuHolder holder,
                                  int slot, ItemMeta meta) {
        int page = holder.getPage();

        if (slot >= PluginSelectMenu.PAGE_SIZE) {
            // Navigation row — handle only prev/next arrows.
            if (slot == PluginSelectMenu.SLOT_PREV) {
                PluginSelectMenu.open(player, self, page - 1);
            } else if (slot == PluginSelectMenu.SLOT_NEXT) {
                PluginSelectMenu.open(player, self, page + 1);
            }
            // All other nav-row slots (filler, page-info) are ignored.
            return;
        }

        // Content row: display name == plugin name.
        if (meta == null || !meta.hasDisplayName()) return;
        String pluginName = plainText(meta.displayName());
        WorldSelectMenu.open(player, pluginName, config, 0);
    }

    // ── Handler: world-select menu ───────────────────────────────────────

    private void handleWorldMenu(Player player, WorldMenuHolder holder,
                                 int slot, ItemMeta meta) {
        String pluginName = holder.getPluginName();
        int page = holder.getPage();

        if (slot >= WorldSelectMenu.PAGE_SIZE) {
            if (slot == WorldSelectMenu.SLOT_PREV) {
                WorldSelectMenu.open(player, pluginName, config, page - 1);
            } else if (slot == WorldSelectMenu.SLOT_NEXT) {
                WorldSelectMenu.open(player, pluginName, config, page + 1);
            }
            return;
        }

        // Content row: display name == world name.
        if (meta == null || !meta.hasDisplayName()) return;
        String worldName = plainText(meta.displayName());

        boolean current = config.isEnabled(pluginName, worldName);
        config.setEnabled(pluginName, worldName, !current);
        // Re-open to refresh the displayed wool colour.
        WorldSelectMenu.open(player, pluginName, config, page);
    }

    // ── Utility ──────────────────────────────────────────────────────────

    /**
     * Strips all Adventure formatting from a {@link Component}, returning
     * only the raw text characters. Used to recover names from item display names.
     */
    private static String plainText(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
