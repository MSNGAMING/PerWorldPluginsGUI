package com.pwp.gui;

import com.pwp.gui.command.PwpCommand;
import com.pwp.gui.config.WorldPluginConfig;
import com.pwp.gui.listener.CommandInterceptListener;
import com.pwp.gui.listener.InventoryListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * PerWorldPluginsGUI — main plugin entry point.
 *
 * <p>This class is intentionally thin: it wires together the config, command,
 * and event-listener components, then delegates all logic to them.
 */
public final class PerWorldPluginsGUI extends JavaPlugin {

    private WorldPluginConfig worldPluginConfig;

    @Override
    public void onEnable() {
        // Load (and persist) the default config, then build the in-memory cache.
        worldPluginConfig = new WorldPluginConfig(this);

        // Register command + tab-completer.
        PwpCommand pwpCommand = new PwpCommand(this, worldPluginConfig);
        PluginCommand cmd = getCommand("pwp");
        if (cmd != null) {
            cmd.setExecutor(pwpCommand);
            cmd.setTabCompleter(pwpCommand);
        } else {
            getLogger().severe("'pwp' command is missing from plugin.yml! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register event listeners.
        getServer().getPluginManager().registerEvents(
                new InventoryListener(worldPluginConfig, this), this);
        getServer().getPluginManager().registerEvents(
                new CommandInterceptListener(worldPluginConfig, this), this);

        getLogger().info("PerWorldPluginsGUI v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("PerWorldPluginsGUI disabled.");
    }
}
