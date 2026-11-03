    package com.pwp.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class PerWorldPluginsGUI extends JavaPlugin implements CommandExecutor, Listener {

    @Override
    public void onEnable() {
        //saveDefaultConfig();
        getCommand("pwp").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("PerWorldPluginsGUI enabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use the GUI.");
            return true;
        }
        
        Player player = (Player) sender;
        if (!player.hasPermission("pwp.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this.");
            return true;
        }

        openPluginMenu(player);
        return true;
    }

    private void openPluginMenu(Player player) {
        Plugin[] plugins = getServer().getPluginManager().getPlugins();
        int size = (int) Math.ceil(plugins.length / 9.0) * 9;
        if (size == 0) size = 9;
        if (size > 54) size = 54; // Cap at double chest size for simplicity

        Inventory inv = Bukkit.createInventory(null, size, "PWP: Select Plugin");

        for (Plugin plugin : plugins) {
            if (plugin.getName().equals(this.getName())) continue; // Skip managing this plugin itself

            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + plugin.getName());
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Click to manage worlds");
            meta.setLore(lore);
            
            item.setItemMeta(meta);
            inv.addItem(item);
        }

        player.openInventory(inv);
    }

    private void openWorldMenu(Player player, String pluginName) {
        List<World> worlds = Bukkit.getWorlds();
        int size = (int) Math.ceil(worlds.size() / 9.0) * 9;
        if (size == 0) size = 9;

        Inventory inv = Bukkit.createInventory(null, size, "PWP Worlds: " + pluginName);

        for (int i = 0; i < worlds.size(); i++) {
            World world = worlds.get(i);
            String worldName = world.getName();
            
            // Default to true (enabled) if not set in config
            boolean isEnabled = getConfig().getBoolean("plugins." + pluginName + "." + worldName, true);
            
            ItemStack item = new ItemStack(isEnabled ? Material.LIME_WOOL : Material.RED_WOOL);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + worldName);
            
            List<String> lore = new ArrayList<>();
            lore.add(isEnabled ? ChatColor.GREEN + "Status: ENABLED" : ChatColor.RED + "Status: DISABLED");
            lore.add(ChatColor.GRAY + "Click to toggle");
            meta.setLore(lore);
            
            item.setItemMeta(meta);
            inv.setItem(i, item);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith("PWP:")) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;

        Player player = (Player) event.getWhoClicked();
        String clickedName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

        if (title.equals("PWP: Select Plugin")) {
            openWorldMenu(player, clickedName);
        } 
        else if (title.startsWith("PWP Worlds: ")) {
            String pluginName = title.replace("PWP Worlds: ", "");
            String worldName = clickedName;
            
            String configPath = "plugins." + pluginName + "." + worldName;
            boolean currentState = getConfig().getBoolean(configPath, true);
            
            // Toggle state
            getConfig().set(configPath, !currentState);
            saveConfig();
            
            // Reopen to refresh the GUI
            openWorldMenu(player, pluginName);
        }
    }

    // Intercept commands from disabled plugins
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().substring(1); // Remove the slash
        String commandRoot = message.split(" ")[0].toLowerCase();

        PluginCommand cmd = getServer().getPluginCommand(commandRoot);
        if (cmd != null && cmd.getPlugin() != null) {
            String pluginName = cmd.getPlugin().getName();
            String worldName = event.getPlayer().getWorld().getName();
            
            // Check if disabled in this world
            boolean isEnabled = getConfig().getBoolean("plugins." + pluginName + "." + worldName, true);
            if (!isEnabled) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "You cannot use commands for " + pluginName + " in this world.");
            }
        }
    }
}