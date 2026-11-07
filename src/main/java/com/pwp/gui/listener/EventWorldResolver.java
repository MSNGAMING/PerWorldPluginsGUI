package com.pwp.gui.listener;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.hanging.HangingEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.vehicle.VehicleEvent;
import org.bukkit.event.weather.WeatherEvent;
import org.bukkit.event.world.WorldEvent;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance, zero-allocation utility for extracting the target {@link World}
 * from any standard or custom Bukkit/Paper event.
 */
public final class EventWorldResolver {

    private static final Map<Class<? extends Event>, Method> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Method NO_METHOD;

    static {
        try {
            NO_METHOD = EventWorldResolver.class.getDeclaredMethod("markerNoMethod");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static void markerNoMethod() {}

    private EventWorldResolver() {}

    /**
     * Resolves the {@link World} associated with an event, or {@code null} if
     * the event is global or has no world context (e.g. server ping, console).
     */
    public static World resolveWorld(Event event) {
        if (event == null) return null;

        // 1. Direct standard Bukkit event hierarchies (hot path: no reflection)
        if (event instanceof PlayerEvent pe) {
            Player p = pe.getPlayer();
            return p != null ? p.getWorld() : null;
        }
        if (event instanceof BlockEvent be) {
            Block b = be.getBlock();
            return b != null ? b.getWorld() : null;
        }
        if (event instanceof EntityEvent ee) {
            Entity e = ee.getEntity();
            return e != null ? e.getWorld() : null;
        }
        if (event instanceof WorldEvent we) {
            return we.getWorld();
        }
        if (event instanceof WeatherEvent we) {
            return we.getWorld();
        }
        if (event instanceof VehicleEvent ve) {
            Vehicle v = ve.getVehicle();
            return v != null ? v.getWorld() : null;
        }
        if (event instanceof HangingEvent he) {
            Entity e = he.getEntity();
            return e != null ? e.getWorld() : null;
        }
        if (event instanceof InventoryEvent ie) {
            HumanEntity he = ie.getView().getPlayer();
            return he != null ? he.getWorld() : null;
        }

        // 2. Cached reflection fallback for custom or niche plugin events
        Class<? extends Event> clazz = event.getClass();
        Method method = METHOD_CACHE.get(clazz);
        if (method == null) {
            method = findWorldMethod(clazz);
            METHOD_CACHE.put(clazz, method != null ? method : NO_METHOD);
        }

        if (method != null && method != NO_METHOD) {
            try {
                Object result = method.invoke(event);
                if (result instanceof World w) return w;
                if (result instanceof Location loc) return loc.getWorld();
                if (result instanceof Block b) return b.getWorld();
                if (result instanceof Entity e) return e.getWorld();
            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    private static Method findWorldMethod(Class<?> clazz) {
        for (String name : new String[]{"getWorld", "getLocation", "getBlock", "getEntity", "getPlayer"}) {
            try {
                Method m = clazz.getMethod(name);
                Class<?> ret = m.getReturnType();
                if (World.class.isAssignableFrom(ret)
                        || Location.class.isAssignableFrom(ret)
                        || Block.class.isAssignableFrom(ret)
                        || Entity.class.isAssignableFrom(ret)) {
                    m.setAccessible(true);
                    return m;
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }
}
