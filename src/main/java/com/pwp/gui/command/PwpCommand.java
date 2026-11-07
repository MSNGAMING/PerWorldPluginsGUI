package com.pwp.gui.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.pwp.gui.config.WorldPluginConfig;
import com.pwp.gui.listener.ListenerManager;
import com.pwp.gui.menu.PluginSelectMenu;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Native Paper Brigadier command builder for {@code /pwp}.
 *
 * <p>Commands provided:
 * <ul>
 *   <li>{@code /pwp} — opens the main plugin management GUI (player only)</li>
 *   <li>{@code /pwp reload} — reloads configuration, command cache, and event listener hooks</li>
 * </ul>
 */
public final class PwpCommand {

    private PwpCommand() {}

    public static LiteralCommandNode<CommandSourceStack> createNode(
            Plugin plugin,
            WorldPluginConfig config,
            CommandPluginResolver commandResolver,
            ListenerManager listenerManager) {

        return Commands.literal("pwp")
                .requires(source -> source.getSender().hasPermission("pwp.admin"))
                .executes(context -> {
                    CommandSender sender = context.getSource().getSender();
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(Component.text(
                                "Only players can open the GUI. Console usage: /pwp reload",
                                NamedTextColor.YELLOW));
                        return Command.SINGLE_SUCCESS;
                    }

                    PluginSelectMenu.open(player, plugin, 0);
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("reload")
                        .executes(context -> {
                            CommandSender sender = context.getSource().getSender();
                            config.reload();
                            commandResolver.buildIndex();
                            listenerManager.wrapAllListeners();

                            sender.sendMessage(Component.text(
                                    "✔ PerWorldPluginsGUI config and listener hooks reloaded successfully.",
                                    NamedTextColor.GREEN));
                            plugin.getLogger().info(sender.getName() + " reloaded the PerWorldPluginsGUI config and hooks.");
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .build();
    }
}
