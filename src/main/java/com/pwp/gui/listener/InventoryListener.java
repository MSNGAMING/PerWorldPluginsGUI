package com.pwp.gui.listener;

import com.pwp.gui.config.WorldPluginConfig;
import com.pwp.gui.holder.PluginMenuHolder;
import com.pwp.gui.holder.WorldMenuHolder;
import com.pwp.gui.menu.PluginSelectMenu;
import com.pwp.gui.menu.WorldSelectMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles click interactions and seamless ESC / Back-button navigation
 * within the PerWorldPluginsGUI menus.
 */
public final class InventoryListener implements Listener {

    private final WorldPluginConfig config;
    private final Plugin self;

    /**
     * Tracks players who are intentionally transitioning between inventories
     * (e.g. clicking buttons/toggles) to distinguish from pressing Escape.
     */
    private final Set<UUID> intentionalTransitions = ConcurrentHashMap.newKeySet();

    public InventoryListener(WorldPluginConfig config, Plugin self) {
        this.config = config;
        this.self   = self;
    }

    private void markIntentionalTransition(Player player) {
        UUID uuid = player.getUniqueId();
        intentionalTransitions.add(uuid);
        Bukkit.getScheduler().runTaskLater(self, () -> intentionalTransitions.remove(uuid), 2L);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Identify our GUI by its holder type
        InventoryHolder topHolder = event.getView().getTopInventory().getHolder();
        if (!(topHolder instanceof PluginMenuHolder)
                && !(topHolder instanceof WorldMenuHolder)) return;

        // Always cancel to prevent item movement while our GUI is open
        event.setCancelled(true);

        // Only process clicks that land inside our custom GUI
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

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        // If the close was caused by an intentional in-menu button click, ignore
        if (intentionalTransitions.remove(player.getUniqueId())) return;

        // If the player pressed Escape while in the World configuration menu:
        // Automatically return to the Main Plugin Menu at the exact page they came from!
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof WorldMenuHolder worldHolder) {
            int returnPage = worldHolder.getReturnPluginPage();
            Bukkit.getScheduler().runTask(self, () -> {
                if (player.isOnline()) {
                    PluginSelectMenu.open(player, self, returnPage);
                }
            });
        }
        // If they closed PluginMenuHolder, do nothing -> completely exits back to game
    }

    // ── Handler: plugin-select menu ──────────────────────────────────────

    private void handlePluginMenu(Player player, PluginMenuHolder holder,
                                  int slot, ItemMeta meta) {
        int page = holder.getPage();

        if (slot >= PluginSelectMenu.PAGE_SIZE) {
            // Navigation row — handle prev/next arrows
            if (slot == PluginSelectMenu.SLOT_PREV) {
                markIntentionalTransition(player);
                PluginSelectMenu.open(player, self, page - 1);
            } else if (slot == PluginSelectMenu.SLOT_NEXT) {
                markIntentionalTransition(player);
                PluginSelectMenu.open(player, self, page + 1);
            }
            return;
        }

        // Content row: display name == plugin name
        if (meta == null || !meta.hasDisplayName()) return;
        String pluginName = plainText(meta.displayName());

        markIntentionalTransition(player);
        WorldSelectMenu.open(player, pluginName, config, 0, page);
    }

    // ── Handler: world-select menu ───────────────────────────────────────

    private void handleWorldMenu(Player player, WorldMenuHolder holder,
                                 int slot, ItemMeta meta) {
        String pluginName = holder.getPluginName();
        int page = holder.getPage();
        int returnPluginPage = holder.getReturnPluginPage();

        if (slot >= WorldSelectMenu.PAGE_SIZE) {
            if (slot == WorldSelectMenu.SLOT_PREV) {
                markIntentionalTransition(player);
                WorldSelectMenu.open(player, pluginName, config, page - 1, returnPluginPage);
            } else if (slot == WorldSelectMenu.SLOT_NEXT) {
                markIntentionalTransition(player);
                WorldSelectMenu.open(player, pluginName, config, page + 1, returnPluginPage);
            } else if (slot == WorldSelectMenu.SLOT_BACK) {
                markIntentionalTransition(player);
                PluginSelectMenu.open(player, self, returnPluginPage);
            }
            return;
        }

        // Content row: display name == world name
        if (meta == null || !meta.hasDisplayName()) return;
        String worldName = plainText(meta.displayName());

        boolean current = config.isEnabled(pluginName, worldName);
        config.setEnabled(pluginName, worldName, !current);

        // Re-open to refresh the displayed wool colour while preserving scroll state
        markIntentionalTransition(player);
        WorldSelectMenu.open(player, pluginName, config, page, returnPluginPage);
    }

    // ── Utility ──────────────────────────────────────────────────────────

    /**
     * Strips all Adventure formatting from a {@link Component}, returning
     * only the raw text characters.
     */
    private static String plainText(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
