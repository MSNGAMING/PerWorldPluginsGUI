package com.pwp.gui.listener;

import com.pwp.gui.config.WorldPluginConfig;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

/**
 * Custom {@link RegisteredListener} that acts as a world-aware gatekeeper.
 *
 * <p>Before delegating to the wrapped listener, it checks whether the plugin that
 * registered the listener is allowed to execute events in the world where the
 * event occurred. If the plugin is disabled in that world, the event call is dropped.
 */
public final class PerWorldRegisteredListener extends RegisteredListener {

    private final RegisteredListener original;
    private final WorldPluginConfig config;
    private final Plugin self;

    public PerWorldRegisteredListener(RegisteredListener original, WorldPluginConfig config, Plugin self) {
        super(
                original.getListener(),
                original.getExecutor(),
                original.getPriority(),
                original.getPlugin(),
                original.isIgnoringCancelled()
        );
        this.original = original;
        this.config = config;
        this.self = self;
    }

    public RegisteredListener getOriginal() {
        return original;
    }

    @Override
    public void callEvent(Event event) throws EventException {
        Plugin targetPlugin = getPlugin();
        // Never restrict this plugin's own listeners
        if (targetPlugin != null && !targetPlugin.equals(self)) {
            World world = EventWorldResolver.resolveWorld(event);
            if (world != null && !config.isEnabled(targetPlugin.getName(), world.getName())) {
                // Event occurs in a world where the plugin is disabled — skip execution!
                return;
            }
        }

        super.callEvent(event);
    }
}
