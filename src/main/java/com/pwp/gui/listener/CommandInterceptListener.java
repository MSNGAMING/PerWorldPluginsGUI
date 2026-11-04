package com.pwp.gui.listener;

import com.pwp.gui.config.WorldPluginConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.PluginCommand;
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
 * <p><b>Limitations (documented):</b>
 * <ul>
 *   <li>Console commands ({@code ServerCommandEvent}) and RCON commands are not
 *       blocked — blocking server-side automation is intentionally out of scope.</li>
 *   <li>Command blocks and {@code /execute} targeting a player bypass this check
 *       because those do not fire {@link PlayerCommandPreprocessEvent}.</li>
 *   <li>Direct API calls ({@code player.performCommand(...)}) from other plugins
 *       also bypass this check for the same reason.</li>
 * </ul>
 *
 * <p>Priority is set to {@code HIGHEST} with {@code ignoreCancelled = true}
 * so we act late (after other plugins) but only on events still alive.
 */
public final class CommandInterceptListener implements Listener {

    private final WorldPluginConfig config;
    private final JavaPlugin self;

    public CommandInterceptListener(WorldPluginConfig config, JavaPlugin self) {
        this.config = config;
        this.self   = self;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        // Strip the leading '/' and isolate the root command label.
        String message     = event.getMessage().substring(1);
        String commandRoot = message.split(" ")[0].toLowerCase();

        // Handle namespaced commands like "minecraft:tp" or "essentials:home".
        if (commandRoot.contains(":")) {
            commandRoot = commandRoot.substring(commandRoot.indexOf(':') + 1);
        }

        PluginCommand cmd = self.getServer().getPluginCommand(commandRoot);
        if (cmd == null || cmd.getPlugin() == null) return;

        String pluginName = cmd.getPlugin().getName();
        // Never block commands that belong to this plugin itself.
        if (pluginName.equals(self.getName())) return;

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
