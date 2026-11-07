package com.pwp.gui;

import com.pwp.gui.command.CommandPluginResolver;
import com.pwp.gui.command.PwpCommand;
import com.pwp.gui.config.WorldPluginConfig;
import com.pwp.gui.listener.CommandInterceptListener;
import com.pwp.gui.listener.InventoryListener;
import com.pwp.gui.listener.ListenerManager;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * PerWorldPluginsGUI — Native Paper plugin entry point.
 *
 * <p>Integrates Paper Brigadier lifecycle commands, dynamic event listener
 * interception, and Adventure text components to provide per-world plugin isolation.
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

        // Native Paper Brigadier Command Registration
        LifecycleEventManager<Plugin> manager = this.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            commands.register(PwpCommand.createNode(this, worldPluginConfig, commandResolver, listenerManager));
        });

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

        getLogger().info("PerWorldPluginsGUI v" + getPluginMeta().getVersion() + " (Native Paper) enabled.");
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
