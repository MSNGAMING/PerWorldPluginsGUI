package com.pwp.gui.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves the owning {@link Plugin} for any command, supporting:
 * <ul>
 *     <li>Traditional {@link PluginCommand} declarations</li>
 *     <li>Modern Paper Brigadier lifecycle commands (e.g. {@code /orbital})</li>
 *     <li>Namespaced commands (e.g. {@code /orbitalstrike:orbital})</li>
 *     <li>CommandMap dynamic registrations</li>
 * </ul>
 */
public final class CommandPluginResolver {

    private final Map<String, String> commandToPluginCache = new ConcurrentHashMap<>();

    public CommandPluginResolver() {
        buildIndex();
    }

    /**
     * Rebuilds the index of known commands to their registering plugin names.
     */
    public synchronized void buildIndex() {
        commandToPluginCache.clear();
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            String pluginName = plugin.getName();
            PluginDescriptionFile desc = plugin.getDescription();

            // 1. Direct plugin name matching (e.g. /orbitalstrike)
            commandToPluginCache.put(pluginName.toLowerCase(Locale.ROOT), pluginName);

            // 2. Commands declared in plugin.yml (and their aliases)
            Map<String, Map<String, Object>> commands = desc.getCommands();
            if (commands != null) {
                for (Map.Entry<String, Map<String, Object>> entry : commands.entrySet()) {
                    String cmdName = entry.getKey().toLowerCase(Locale.ROOT);
                    commandToPluginCache.put(cmdName, pluginName);

                    Object aliasesObj = entry.getValue().get("aliases");
                    if (aliasesObj instanceof List<?> list) {
                        for (Object alias : list) {
                            if (alias != null) {
                                commandToPluginCache.put(alias.toString().toLowerCase(Locale.ROOT), pluginName);
                            }
                        }
                    } else if (aliasesObj instanceof String aliasStr) {
                        commandToPluginCache.put(aliasStr.toLowerCase(Locale.ROOT), pluginName);
                    }
                }
            }
        }
    }

    /**
     * Determines which plugin owns a given command root.
     *
     * @param commandRoot the lowercase command token (without leading '/')
     * @return the owning plugin's name, or {@code null} if unknown/vanilla
     */
    public String resolvePluginName(String commandRoot) {
        if (commandRoot == null || commandRoot.isBlank()) return null;
        String cmd = commandRoot.toLowerCase(Locale.ROOT).trim();

        // 1. Explicit plugin namespace prefix: "orbitalstrike:orbital"
        if (cmd.contains(":")) {
            String[] parts = cmd.split(":", 2);
            String namespace = parts[0];
            Plugin plugin = Bukkit.getPluginManager().getPlugin(namespace);
            if (plugin != null) {
                return plugin.getName();
            }
            // Strip namespace and continue resolution
            cmd = parts[1];
        }

        // 2. Check cached plugin.yml / alias index
        String cached = commandToPluginCache.get(cmd);
        if (cached != null) {
            return cached;
        }

        // 3. Check Bukkit PluginCommand
        PluginCommand pluginCommand = Bukkit.getPluginCommand(cmd);
        if (pluginCommand != null && pluginCommand.getPlugin() != null) {
            String name = pluginCommand.getPlugin().getName();
            commandToPluginCache.put(cmd, name);
            return name;
        }

        // 4. Check Bukkit CommandMap for PluginIdentifiableCommand
        try {
            Command mapCommand = Bukkit.getCommandMap().getCommand(cmd);
            if (mapCommand instanceof PluginIdentifiableCommand pic && pic.getPlugin() != null) {
                String name = pic.getPlugin().getName();
                commandToPluginCache.put(cmd, name);
                return name;
            }

            // 5. Fallback: inspect command permission string (e.g. orbital.admin -> matches OrbitalStrike)
            if (mapCommand != null && mapCommand.getPermission() != null) {
                String perm = mapCommand.getPermission().toLowerCase(Locale.ROOT);
                for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                    String pName = plugin.getName().toLowerCase(Locale.ROOT);
                    if (perm.startsWith(pName + ".") || perm.contains("." + pName + ".")) {
                        commandToPluginCache.put(cmd, plugin.getName());
                        return plugin.getName();
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }
}
