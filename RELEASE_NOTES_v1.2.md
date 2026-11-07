# PerWorldPluginsGUI v1.2 Release Notes

## 📄 Overview
**PerWorldPluginsGUI v1.2** is a major architectural overhaul that converts the plugin into a **pure native Paper plugin** (`paper-plugin.yml`), introduces **dynamic event listener interception** (`HandlerList` / `RegisteredListener`), modern Paper **Brigadier lifecycle commands**, and intuitive **ESC / Back-button navigation with scroll memory**.

---

## 🚀 What's New in v1.2

### 1. 📜 Pure Native Paper Plugin (`paper-plugin.yml`)
- Fully migrated from legacy Bukkit `plugin.yml` to modern `paper-plugin.yml`.
- Leverages Paper's classloader isolation and lifecycle event architecture.
- Built specifically for modern **Paper and Purpur 1.21.x and 26.x** server environments.

### 2. ⚡ Dynamic Event Listener Interception
Previously, the plugin only checked chat commands (`PlayerCommandPreprocessEvent`), allowing gameplay event listeners (e.g. fishing rod cannons, custom weapon mechanics, explosion events, block breaks) to continue executing in restricted worlds.
- **Universal HandlerList Hooking (`PerWorldRegisteredListener` & `ListenerManager`):** Automatically wraps and monitors all registered Bukkit event listeners across all server plugins.
- **Smart Event World Resolver (`EventWorldResolver`):** Zero-allocation world extractor supporting `PlayerEvent`, `BlockEvent`, `EntityEvent`, `InventoryEvent`, `VehicleEvent`, `WeatherEvent`, and custom events.
- **True Isolation:** If a plugin is disabled in a world, any event fired in that world is dropped before reaching that plugin's listener.

### 3. 🎯 Native Paper Brigadier Command Lifecycle
- Migrated `/pwp` and `/pwp reload` to Paper's native **Brigadier Command API** (`LifecycleEvents.COMMANDS`).
- Provides instant client-side autocomplete suggestions (`/pwp r` ➔ `reload`), syntax error highlighting, and permission verification before dispatch.
- **Multi-Tier Command Resolver (`CommandPluginResolver`):** Accurately resolves commands to their plugin owners across Paper Brigadier nodes (such as `/orbital`), `plugin.yml` declarations, aliases, Bukkit `CommandMap` commands, and namespaced aliases (e.g. `/pluginname:command`).

### 4. 🔙 Smart GUI Navigation with Scroll Memory
- **ESC Key to Main Menu:** Pressing `ESC` inside the world configuration screen returns you directly back to the main plugin selection menu instead of exiting entirely. Pressing `ESC` on the main menu exits to the game.
- **Scroll Page Memory:** Remembers the exact page you were viewing in the plugin list when returning, eliminating repetitive scrolling.
- **Dedicated Back Button:** Added an Arrow icon at Slot 48 in the world configuration GUI for 1-click return to the plugin list.

### 5. 🎨 Adventure API & Performance
- Complete migration to Kyori Adventure API (`Component`, `NamedTextColor`).
- In-memory `ConcurrentHashMap` caching for zero-disk-read hot paths during gameplay.
- Full pagination support (up to 45 plugins/worlds per page) avoiding inventory size crashes.

---

## 📦 Installation
1. Download **`PerWorldPluginsGUI-1.2.jar`**.
2. Place the file into your server's `plugins/` folder.
3. Restart your Paper or Purpur server.
4. Run `/pwp` in-game to configure your per-world plugin rules!
