package com.pwp.gui.listener;

import com.pwp.gui.command.CommandPluginResolver;
import com.pwp.gui.config.WorldPluginConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Intercepts player-issued commands and blocks them when the owning plugin
 * is marked as disabled in the player's current world.
 *
 * <p>Supports traditional Bukkit commands, Paper Brigadier lifecycle commands,
 * and namespaced aliases via {@link CommandPluginResolver}.
 */
public final class CommandInterceptListener implements Listener {

    private final WorldPluginConfig config;
    private final JavaPlugin self;
    private final CommandPluginResolver commandResolver;

    public CommandInterceptListener(WorldPluginConfig config, JavaPlugin self, CommandPluginResolver commandResolver) {
        this.config = config;
        this.self   = self;
        this.commandResolver = commandResolver;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message.length() <= 1) return;

        // Strip the leading '/' and isolate the root command token
        String commandRoot = message.substring(1).split(" ")[0].toLowerCase();
        if (commandRoot.isEmpty()) return;

        String pluginName = commandResolver.resolvePluginName(commandRoot);
        if (pluginName == null) return;

        // Never block commands that belong to this management plugin itself
        if (pluginName.equalsIgnoreCase(self.getName())) return;

        Player player    = event.getPlayer();
        String worldName = player.getWorld().getName();

        if (!config.isEnabled(pluginName, worldName)) {
            event.setCancelled(true);
            player.sendMessage(Component.text(
                    "⚠ The plugin '" + pluginName
                            + "' is disabled in world '" + worldName + "'.",
                    NamedTextColor.RED));
        }
    }
}
