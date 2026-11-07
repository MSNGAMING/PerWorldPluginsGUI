package com.pwp.gui.listener;

import com.pwp.gui.config.WorldPluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

import java.util.ArrayList;

/**
 * Manages the dynamic interception of Bukkit event listeners across all {@link HandlerList}s.
 *
 * <p>Wraps active listeners with {@link PerWorldRegisteredListener} so that event
 * triggers (like fishing, block interaction, damage, movement, etc.) are silenced
 * in worlds where the target plugin is disabled.
 */
public final class ListenerManager implements Listener {

    private final WorldPluginConfig config;
    private final Plugin self;

    public ListenerManager(WorldPluginConfig config, Plugin self) {
        this.config = config;
        this.self = self;
    }

    /**
     * Inspects all registered event handlers across the server and wraps any
     * listeners that have not yet been protected.
     */
    public synchronized void wrapAllListeners() {
        ArrayList<HandlerList> lists = HandlerList.getHandlerLists();
        for (HandlerList handlerList : lists) {
            wrapHandlerList(handlerList);
        }
    }

    /**
     * Wraps all un-wrapped listeners inside a specific {@link HandlerList}.
     */
    public synchronized void wrapHandlerList(HandlerList handlerList) {
        RegisteredListener[] listeners = handlerList.getRegisteredListeners();
        for (RegisteredListener listener : listeners) {
            if (listener instanceof PerWorldRegisteredListener) {
                continue;
            }

            Plugin plugin = listener.getPlugin();
            if (plugin == null || plugin.equals(self)) {
                continue;
            }

            handlerList.unregister(listener);
            handlerList.register(new PerWorldRegisteredListener(listener, config, self));
        }
    }

    /**
     * Restores all wrapped listeners back to their original state on server shutdown.
     */
    public synchronized void unwrapAllListeners() {
        ArrayList<HandlerList> lists = HandlerList.getHandlerLists();
        for (HandlerList handlerList : lists) {
            RegisteredListener[] listeners = handlerList.getRegisteredListeners();
            for (RegisteredListener listener : listeners) {
                if (listener instanceof PerWorldRegisteredListener pwrl) {
                    handlerList.unregister(pwrl);
                    handlerList.register(pwrl.getOriginal());
                }
            }
        }
    }

    /**
     * When another plugin is enabled after this one, wrap its listeners after
     * its {@code onEnable()} method finishes registering events.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginEnable(PluginEnableEvent event) {
        if (event.getPlugin().equals(self)) return;

        // Run 1 tick later on the main thread so all listeners from onEnable() are registered
        Bukkit.getScheduler().runTask(self, this::wrapAllListeners);
    }
}
