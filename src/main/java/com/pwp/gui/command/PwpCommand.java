package com.pwp.gui.command;

import com.pwp.gui.config.WorldPluginConfig;
import com.pwp.gui.menu.PluginSelectMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Handles the {@code /pwp} command.
 *
 * <p>Sub-commands:
 * <ul>
 *   <li>{@code /pwp}        — opens the plugin-select GUI (player only)</li>
 *   <li>{@code /pwp reload} — reloads config from disk (console + player)</li>
 * </ul>
 *
 * <p>Both sub-commands require the {@code pwp.admin} permission.
 */
public final class PwpCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final WorldPluginConfig config;

    public PwpCommand(JavaPlugin plugin, WorldPluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    // ── CommandExecutor ──────────────────────────────────────────────────

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!sender.hasPermission("pwp.admin")) {
            sender.sendMessage(Component.text(
                    "You do not have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        // /pwp reload
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            config.reload();
            sender.sendMessage(Component.text(
                    "✔ PerWorldPluginsGUI config reloaded successfully.", NamedTextColor.GREEN));
            plugin.getLogger().info(sender.getName() + " reloaded the PerWorldPluginsGUI config.");
            return true;
        }

        // /pwp — open GUI (players only)
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(
                    "Only players can open the GUI. Console usage: /pwp reload",
                    NamedTextColor.YELLOW));
            return true;
        }

        PluginSelectMenu.open(player, plugin, 0);
        return true;
    }

    // ── TabCompleter ─────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String label, String[] args) {
        if (!sender.hasPermission("pwp.admin")) return List.of();

        if (args.length == 1) {
            String typed = args[0].toLowerCase();
            return "reload".startsWith(typed) ? List.of("reload") : List.of();
        }

        return List.of();
    }
}
