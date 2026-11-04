package com.pwp.gui.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe, in-memory cache for per-world plugin enable/disable states.
 *
 * <p>All reads are served from memory; disk I/O only happens on
 * {@link #setEnabled} (saves immediately) and {@link #reload} (re-reads disk).
 */
public final class WorldPluginConfig {

    private final Plugin plugin;

    /** pluginName → ( worldName → isEnabled ) */
    private final Map<String, Map<String, Boolean>> cache = new ConcurrentHashMap<>();

    public WorldPluginConfig(Plugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig(); // writes config.yml on first run if absent
        loadCache();
    }

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the given plugin is enabled in the given world.
     * Defaults to {@code true} when no explicit entry exists.
     */
    public boolean isEnabled(String pluginName, String worldName) {
        return cache.getOrDefault(pluginName, Map.of())
                    .getOrDefault(worldName, true);
    }

    /**
     * Toggles the enabled state of a plugin in a world and persists it to disk
     * immediately so the change survives a server restart without a manual save.
     */
    public void setEnabled(String pluginName, String worldName, boolean enabled) {
        cache.computeIfAbsent(pluginName, k -> new ConcurrentHashMap<>())
             .put(worldName, enabled);
        plugin.getConfig().set("plugins." + pluginName + "." + worldName, enabled);
        plugin.saveConfig();
    }

    /**
     * Reloads the configuration from disk and rebuilds the in-memory cache.
     * Called by {@code /pwp reload}.
     */
    public void reload() {
        plugin.reloadConfig();
        loadCache();
    }

    // ── Internals ───────────────────────────────────────────────────────────

    private void loadCache() {
        cache.clear();
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection pluginsSection = config.getConfigurationSection("plugins");
        if (pluginsSection == null) return;

        for (String pluginName : pluginsSection.getKeys(false)) {
            ConfigurationSection worldSection =
                    pluginsSection.getConfigurationSection(pluginName);
            if (worldSection == null) continue;

            Map<String, Boolean> worldMap = new HashMap<>();
            for (String worldName : worldSection.getKeys(false)) {
                worldMap.put(worldName, worldSection.getBoolean(worldName, true));
            }
            cache.put(pluginName, worldMap);
        }
    }
}
