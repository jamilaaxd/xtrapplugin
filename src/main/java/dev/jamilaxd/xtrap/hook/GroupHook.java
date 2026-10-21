package dev.jamilaxd.xtrap.hook;

import dev.jamilaxd.xtrap.XTrapPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GroupHook {

    public enum Mode { NONE, TOWNY, CLANSPLUS }

    private final XTrapPlugin plugin;
    private Mode mode = Mode.NONE;
    private Object clansPlusApi;
    private Method clansPlusGet;
    private Method dataManager;
    private Method clanByPlayer;

    public GroupHook(XTrapPlugin plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        String wanted = plugin.getConfig().getString("siege.group.mode", "AUTO").toUpperCase();
        boolean towny = Bukkit.getPluginManager().getPlugin("Towny") != null;
        if (("AUTO".equals(wanted) || "TOWNY".equals(wanted)) && towny) {
            mode = Mode.TOWNY;
            plugin.getLogger().info("Siege grubu: Towny");
            return;
        }
        if ("AUTO".equals(wanted) || "CLANSPLUS".equals(wanted)) {
            String clazz = plugin.getConfig().getString("siege.group.clansplus-class", "com.cortezromeo.clansplus.api.ClanPlus");
            if (bindClansPlus(clazz)) {
                mode = Mode.CLANSPLUS;
                plugin.getLogger().info("Siege grubu: ClansPlus");
                return;
            }
        }
        mode = Mode.NONE;
        plugin.getLogger().info("Siege grubu: yok (tek trap)");
    }

    public Mode mode() {
        return mode;
    }

    public Set<UUID> groupOf(Player player) {
        if (player == null || mode == Mode.NONE) return Collections.emptySet();
        try {
            if (mode == Mode.TOWNY) return townyGroup(player);
            return clanGroup(player);
        } catch (Throwable ex) {
            plugin.getLogger().warning("Grup sorgusu basarisiz: " + ex.getMessage());
            return Collections.emptySet();
        }
    }

    private boolean bindClansPlus(String className) {
        try {
            Class<?> api = Class.forName(className);
            Object instance = Bukkit.getServicesManager().load(api);
            if (instance == null) {
                clansPlusGet = find(api, "getInstance", "getAPI", "get");
                if (clansPlusGet == null) return false;
                instance = clansPlusGet.invoke(null);
            }
            if (instance == null) return false;
            clansPlusApi = instance;
            dataManager = instance.getClass().getMethod("getPluginDataManager");
            clanByPlayer = findInstance(dataManager.getReturnType(), "getClanDatabaseByPlayerName");
            return clanByPlayer != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private Set<UUID> clanGroup(Player player) throws Exception {
        Object data = dataManager.invoke(clansPlusApi);
        Object clan = clanByPlayer.invoke(data, player.getName());
        if (clan == null) return Collections.emptySet();
        Object raw = clan.getClass().getMethod("getMembers").invoke(clan);
        if (!(raw instanceof Iterable<?> names)) return Collections.emptySet();
        Method playerData = data.getClass().getMethod("getPlayerDatabase", String.class);
        Set<UUID> out = new HashSet<>();
        for (Object name : names) {
            if (!(name instanceof String playerName)) continue;
            Object dataOf = playerData.invoke(data, playerName);
            if (dataOf == null) continue;
            Object uuid = dataOf.getClass().getMethod("getUUID").invoke(dataOf);
            if (uuid instanceof String text && !text.isBlank()) {
                try {
                    out.add(UUID.fromString(text));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return out;
    }

    private Set<UUID> townyGroup(Player player) throws Exception {
        Class<?> residentApi = Class.forName("com.palmergames.bukkit.towny.TownyUniverse");
        Method getInstance = residentApi.getMethod("getInstance");
        Object universe = getInstance.invoke(null);
        Object resident = universe.getClass().getMethod("getResident", UUID.class).invoke(universe, player.getUniqueId());
        if (resident == null) return Collections.emptySet();
        Object town = resident.getClass().getMethod("getTownOrNull").invoke(resident);
        if (town == null) return Collections.emptySet();
        Object nation = null;
        try {
            nation = town.getClass().getMethod("getNationOrNull").invoke(town);
        } catch (NoSuchMethodException ignored) {
        }
        Object source = nation != null ? nation : town;
        Method residents = source.getClass().getMethod("getResidents");
        return uuids(residents.invoke(source));
    }

    @SuppressWarnings("unchecked")
    private Set<UUID> uuids(Object raw) {
        Set<UUID> out = new HashSet<>();
        if (!(raw instanceof Iterable<?> iterable)) return out;
        for (Object item : iterable) {
            if (item instanceof UUID uuid) out.add(uuid);
            else if (item instanceof Player p) out.add(p.getUniqueId());
            else if (item instanceof org.bukkit.OfflinePlayer op) out.add(op.getUniqueId());
            else {
                try {
                    Object uuid = item.getClass().getMethod("getUUID").invoke(item);
                    if (uuid instanceof UUID id) out.add(id);
                } catch (Exception ignored) {
                    try {
                        Object uuid = item.getClass().getMethod("getUniqueId").invoke(item);
                        if (uuid instanceof UUID id) out.add(id);
                    } catch (Exception ignored2) {
                    }
                }
            }
        }
        return out;
    }

    private static Method find(Class<?> type, String... names) {
        for (String name : names) {
            try {
                return type.getMethod(name);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private static Method findInstance(Class<?> type, String... names) {
        for (Method method : type.getMethods()) {
            for (String name : names) {
                if (method.getName().equals(name) && method.getParameterCount() <= 1) return method;
            }
        }
        return null;
    }
}
