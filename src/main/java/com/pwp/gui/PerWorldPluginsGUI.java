package com.pwp.gui;

import com.pwp.gui.command.CommandPluginResolver;
import com.pwp.gui.command.PwpCommand;
import com.pwp.gui.config.WorldPluginConfig;
import com.pwp.gui.listener.CommandInterceptListener;
import com.pwp.gui.listener.InventoryListener;
import com.pwp.gui.listener.ListenerManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * PerWorldPluginsGUI — main plugin entry point.
 *
 * <p>Integrates dynamic event listener interception and command routing
 * to isolate plugins completely per world.
 */
public final class PerWorldPluginsGUI extends JavaPlugin {

    private WorldPluginConfig worldPluginConfig;
    private CommandPluginResolver commandResolver;
    private ListenerManager listenerManager;

    @Override
    public void onEnable() {
        // Load (and persist) default config, and construct in-memory cache
        worldPluginConfig = new WorldPluginConfig(this);

        // Command resolver capable of mapping Brigadier, CommandMap, and plugin.yml commands
        commandResolver = new CommandPluginResolver();

        // Event listener manager that wraps Bukkit HandlerLists to suppress events in disabled worlds
        listenerManager = new ListenerManager(worldPluginConfig, this);

        // Register command + tab-completer
        PwpCommand pwpCommand = new PwpCommand(this, worldPluginConfig, commandResolver, listenerManager);
        PluginCommand cmd = getCommand("pwp");
        if (cmd != null) {
            cmd.setExecutor(pwpCommand);
            cmd.setTabCompleter(pwpCommand);
        } else {
            getLogger().severe("'pwp' command is missing from plugin.yml! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register event listeners
        getServer().getPluginManager().registerEvents(
                new InventoryListener(worldPluginConfig, this), this);
        getServer().getPluginManager().registerEvents(
                new CommandInterceptListener(worldPluginConfig, this, commandResolver), this);
        getServer().getPluginManager().registerEvents(listenerManager, this);

        // Wrap existing listeners now
        listenerManager.wrapAllListeners();

        // Also schedule a sweep on tick 1 after all other plugins have finished onEnable()
        getServer().getScheduler().runTask(this, () -> {
            commandResolver.buildIndex();
            listenerManager.wrapAllListeners();
            getLogger().info("Event and command intercepts initialized for all active plugins.");
        });

        getLogger().info("PerWorldPluginsGUI v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        // Safely unwrap all listeners back to vanilla state
        if (listenerManager != null) {
            listenerManager.unwrapAllListeners();
        }
        getLogger().info("PerWorldPluginsGUI disabled.");
    }
}
