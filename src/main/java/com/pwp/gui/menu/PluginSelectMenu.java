package com.pwp.gui.menu;

import com.pwp.gui.holder.PluginMenuHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

/**
 * Builds and opens the paginated "Select Plugin" inventory GUI.
 *
 * <p>Layout (6 rows, 54 slots):
 * <pre>
 *   Rows 1-5  : plugin items (up to 45 per page, sorted A-Z)
 *   Row  6    : navigation bar
 *     slot 45 : ← previous page  (or red glass if on page 0)
 *     slot 46-48, 50-52 : grey glass filler
 *     slot 49 : page-info book
 *     slot 53 : next page →      (or red glass if on last page)
 * </pre>
 */
public final class PluginSelectMenu {

    /** Number of content slots per page (rows 1-5). */
    public static final int PAGE_SIZE = 45;
    /** Total inventory size — always a full double-chest. */
    private static final int INV_SIZE = 54;

    // Navigation-row slot indices (row 6)
    public static final int SLOT_PREV = 45;
    public static final int SLOT_INFO = 49;
    public static final int SLOT_NEXT = 53;

    private PluginSelectMenu() {}

    /**
     * Opens the plugin-select GUI for {@code player} at the given page.
     *
     * @param player the player to open the GUI for
     * @param self   this plugin (excluded from the list)
     * @param page   0-indexed page; automatically clamped to valid range
     */
    public static void open(Player player, Plugin self, int page) {
        // Build a sorted, filtered plugin list once per open call.
        List<Plugin> plugins = Arrays.stream(Bukkit.getPluginManager().getPlugins())
                .filter(p -> !p.getName().equals(self.getName()))
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();

        int totalPages  = Math.max(1, (int) Math.ceil(plugins.size() / (double) PAGE_SIZE));
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));

        PluginMenuHolder holder = new PluginMenuHolder(currentPage);
        Inventory inv = Bukkit.createInventory(holder, INV_SIZE,
                Component.text("PWP » Select Plugin", NamedTextColor.DARK_AQUA)
                         .append(Component.text(
                                 " [" + (currentPage + 1) + "/" + totalPages + "]",
                                 NamedTextColor.GRAY)));
        holder.setInventory(inv);

        // Populate content slots for this page.
        int start = currentPage * PAGE_SIZE;
        int end   = Math.min(start + PAGE_SIZE, plugins.size());

        for (int i = start; i < end; i++) {
            Plugin plugin = plugins.get(i);
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(plugin.getName(), NamedTextColor.YELLOW));
                meta.lore(List.of(
                        Component.text("Version: " + plugin.getDescription().getVersion(),
                                NamedTextColor.GRAY),
                        Component.text("Click to manage worlds", NamedTextColor.AQUA)
                ));
                item.setItemMeta(meta);
            }
            inv.setItem(i - start, item);
        }

        addNavigationBar(inv, currentPage, totalPages);
        player.openInventory(inv);
    }

    // ── Navigation bar ───────────────────────────────────────────────────

    private static void addNavigationBar(Inventory inv, int currentPage, int totalPages) {
        ItemStack filler = makeFiller();
        for (int slot = PAGE_SIZE; slot < INV_SIZE; slot++) {
            inv.setItem(slot, filler);
        }

        inv.setItem(SLOT_PREV, currentPage > 0
                ? makeArrow("« Previous Page")
                : makeGlass(Material.RED_STAINED_GLASS_PANE, "No Previous Page"));

        inv.setItem(SLOT_INFO, makePageInfo(currentPage, totalPages));

        inv.setItem(SLOT_NEXT, currentPage < totalPages - 1
                ? makeArrow("Next Page »")
                : makeGlass(Material.RED_STAINED_GLASS_PANE, "No Next Page"));
    }

    // ── Item helpers ─────────────────────────────────────────────────────

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
