package com.pwp.gui.menu;

import com.pwp.gui.config.WorldPluginConfig;
import com.pwp.gui.holder.WorldMenuHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Builds and opens the paginated "Select World" inventory GUI for a given plugin.
 *
 * <p>Layout:
 * <pre>
 *   Rows 1-5  : world items (up to 45 per page, showing green/red wool status)
 *   Row  6    : navigation bar
 *     slot 45 : « Previous Page
 *     slot 46-47, 50-52 : filler
 *     slot 48 : « Back to Plugins
 *     slot 49 : Page X / Y (Book)
 *     slot 53 : Next Page »
 * </pre>
 */
public final class WorldSelectMenu {

    public static final int PAGE_SIZE = 45;
    private static final int INV_SIZE = 54;

    public static final int SLOT_PREV = 45;
    public static final int SLOT_BACK = 48;
    public static final int SLOT_INFO = 49;
    public static final int SLOT_NEXT = 53;

    private WorldSelectMenu() {}

    public static void open(Player player, String pluginName,
                            WorldPluginConfig config, int page) {
        open(player, pluginName, config, page, 0);
    }

    /**
     * Opens the world-select GUI for the given plugin name at the given page.
     *
     * @param player           the player to open the GUI for
     * @param pluginName       the plugin whose per-world states are being edited
     * @param config           the config cache
     * @param page             0-indexed world page; automatically clamped
     * @param returnPluginPage 0-indexed page in the main plugin menu to return to
     */
    public static void open(Player player, String pluginName,
                            WorldPluginConfig config, int page, int returnPluginPage) {
        List<World> worlds = Bukkit.getWorlds();

        int totalPages  = Math.max(1, (int) Math.ceil(worlds.size() / (double) PAGE_SIZE));
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));

        WorldMenuHolder holder = new WorldMenuHolder(pluginName, currentPage, returnPluginPage);
        Inventory inv = Bukkit.createInventory(holder, INV_SIZE,
                Component.text("PWP » " + pluginName, NamedTextColor.DARK_AQUA)
                         .append(Component.text(
                                 " [" + (currentPage + 1) + "/" + totalPages + "]",
                                 NamedTextColor.GRAY)));
        holder.setInventory(inv);

        int start = currentPage * PAGE_SIZE;
        int end   = Math.min(start + PAGE_SIZE, worlds.size());

        for (int i = start; i < end; i++) {
            World world = worlds.get(i);
            String worldName = world.getName();
            boolean isEnabled = config.isEnabled(pluginName, worldName);

            ItemStack item = new ItemStack(isEnabled ? Material.LIME_WOOL : Material.RED_WOOL);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(worldName, NamedTextColor.AQUA));
                meta.lore(List.of(
                        isEnabled
                                ? Component.text("● Enabled — click to disable", NamedTextColor.GREEN)
                                : Component.text("● Disabled — click to enable", NamedTextColor.RED),
                        Component.text(
                                "Environment: " + world.getEnvironment().name(),
                                NamedTextColor.GRAY)
                ));
                item.setItemMeta(meta);
            }
            inv.setItem(i - start, item);
        }

        addNavigationBar(inv, currentPage, totalPages, returnPluginPage);
        player.openInventory(inv);
    }

    // ── Navigation bar ───────────────────────────────────────────────────

    private static void addNavigationBar(Inventory inv, int currentPage, int totalPages, int returnPluginPage) {
        ItemStack filler = makeFiller();
        for (int slot = PAGE_SIZE; slot < INV_SIZE; slot++) {
            inv.setItem(slot, filler);
        }

        inv.setItem(SLOT_PREV, currentPage > 0
                ? makeArrow("« Previous Page")
                : makeGlass(Material.RED_STAINED_GLASS_PANE, "No Previous Page"));

        inv.setItem(SLOT_BACK, makeBackArrow(returnPluginPage));

        inv.setItem(SLOT_INFO, makePageInfo(currentPage, totalPages));

        inv.setItem(SLOT_NEXT, currentPage < totalPages - 1
                ? makeArrow("Next Page »")
                : makeGlass(Material.RED_STAINED_GLASS_PANE, "No Next Page"));
    }

    // ── Item helpers ─────────────────────────────────────────────────────

    private static ItemStack makeBackArrow(int returnPluginPage) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("« Back to Plugins", NamedTextColor.YELLOW));
            meta.lore(List.of(
                    Component.text("Return to plugin menu (Page " + (returnPluginPage + 1) + ")", NamedTextColor.GRAY)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack makeArrow(String label) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(label, NamedTextColor.GREEN));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack makePageInfo(int currentPage, int totalPages) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(
                    "Page " + (currentPage + 1) + " / " + totalPages, NamedTextColor.WHITE));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack makeFiller() {
        return makeGlass(Material.GRAY_STAINED_GLASS_PANE, " ");
    }

    private static ItemStack makeGlass(Material material, String label) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(label, NamedTextColor.DARK_GRAY));
            item.setItemMeta(meta);
        }
        return item;
    }
}
