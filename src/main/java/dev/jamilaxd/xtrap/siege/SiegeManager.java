package dev.jamilaxd.xtrap.siege;

import dev.jamilaxd.xtrap.XTrapPlugin;
import dev.jamilaxd.xtrap.model.Trap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SiegeManager {

    private final XTrapPlugin plugin;
    private final List<Siege> active = new ArrayList<>();

    public SiegeManager(XTrapPlugin plugin) {
        this.plugin = plugin;
    }

    public Siege of(Trap trap) {
        if (trap == null) return null;
        for (Siege siege : active) if (siege.involves(trap.id())) return siege;
        return null;
    }

    public boolean isHealthFrozen(Trap trap) {
        Siege siege = of(trap);
        return plugin.getConfig().getBoolean("siege.freeze-health", true)
                && siege != null && siege.phase() == Siege.Phase.FIGHT;
    }

    public String denyStart() {
        if (!plugin.getConfig().getBoolean("siege.enabled", true)) return "kapali";
        if (!plugin.getConfig().getBoolean("siege.hours.enabled", false)) return null;
        LocalTime now = LocalTime.now();
        LocalTime start = LocalTime.parse(plugin.getConfig().getString("siege.hours.start", "18:00"));
        LocalTime end = LocalTime.parse(plugin.getConfig().getString("siege.hours.end", "23:00"));
        boolean inside = start.isBefore(end)
                ? !now.isBefore(start) && now.isBefore(end)
                : !now.isBefore(start) || now.isBefore(end);
        return inside ? null : "hours";
    }

    public Siege start(Player attacker, Trap target) {
        Siege siege = new Siege(side(attacker), sideOfTrap(target),
                plugin.getConfig().getInt("siege.sessions", 3),
                plugin.getConfig().getInt("siege.session-seconds", 600) * 1000L);
        active.add(siege);
        return siege;
    }

    public void stop(Siege siege) {
        active.remove(siege);
    }

    public void tick() {
        long now = System.currentTimeMillis();
        for (Siege siege : new ArrayList<>(active)) {
            if (siege.phase() == Siege.Phase.FIGHT && now >= siege.phaseEndsAt()) finishSession(siege);
            else if (siege.phase() == Siege.Phase.BREAK && now >= siege.phaseEndsAt()) openNext(siege);
            else if (siege.phase() == Siege.Phase.FIGHT) presence(siege);
        }
    }

    private void openNext(Siege siege) {
        siege.nextSession(plugin.getConfig().getInt("siege.session-seconds", 600) * 1000L);
        announce(siege, "siege.session", Map.of("current", String.valueOf(siege.session()), "total", String.valueOf(siege.sessions())));
    }

    private void presence(Siege siege) {
        int points = plugin.getConfig().getInt("siege.points.presence-per-second", 1);
        if (sidePresent(siege.defenders(), siege.attackers())) siege.addPoints(true, points);
        if (sidePresent(siege.attackers(), siege.defenders())) siege.addPoints(false, points);
    }

    private boolean sidePresent(Set<Integer> lands, Set<Integer> visitors) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Trap standing = plugin.traps().at(player.getLocation());
            if (standing == null || !lands.contains(standing.id())) continue;
            for (int visitor : visitors) {
                Trap side = plugin.traps().byId(visitor);
                if (side != null && side.isMember(player.getUniqueId())) return true;
            }
        }
        return false;
    }

    public void onKill(Player killer, Player victim) {
        Trap killerTrap = memberTrap(killer);
        Trap victimTrap = memberTrap(victim);
        if (killerTrap == null || victimTrap == null) return;
        Siege siege = of(killerTrap);
        if (siege == null || siege != of(victimTrap) || siege.phase() != Siege.Phase.FIGHT) return;
        if (siege.attacking(killerTrap.id()) == siege.attacking(victimTrap.id())) return;
        Trap standing = plugin.traps().at(victim.getLocation());
        boolean inside = standing != null && siege.involves(standing.id());
        int amount = inside
                ? plugin.getConfig().getInt("siege.points.kill-in-enemy-trap", 5)
                : (allowed(victim.getWorld().getName()) ? plugin.getConfig().getInt("siege.points.kill-outside", 2) : 0);
        siege.addPoints(siege.attacking(killerTrap.id()), amount);
    }

    private void finishSession(Siege siege) {
        boolean tie = siege.attackerPoints() == siege.defenderPoints();
        boolean attackerWon = tie
                ? !plugin.getConfig().getBoolean("siege.defender-wins-tie", true)
                : siege.attackerPoints() > siege.defenderPoints();
        siege.awardSession(attackerWon);
        announce(siege, "siege.session-win", Map.of(
                "winner", attackerWon ? names(siege.attackers()) : names(siege.defenders()),
                "attacker", String.valueOf(siege.attackerPoints()),
                "defender", String.valueOf(siege.defenderPoints())));
        if (siege.session() >= siege.sessions()) finish(siege);
        else {
            int wait = plugin.getConfig().getInt("siege.break-seconds", 120);
            siege.beginBreak(wait * 1000L);
            announce(siege, "siege.break", Map.of("saniye", String.valueOf(wait)));
        }
    }

    private void finish(Siege siege) {
        active.remove(siege);
        boolean tie = siege.attackerSessions() == siege.defenderSessions();
        boolean attackerWon = tie
                ? !plugin.getConfig().getBoolean("siege.defender-wins-tie", true)
                : siege.attackerSessions() > siege.defenderSessions();
        if (!attackerWon) {
            announce(siege, "siege.won-defense", Map.of("defender", names(siege.defenders())));
            return;
        }
        Trap source = plugin.traps().byId(siege.attackers().iterator().next());
        if (source == null || source.owner() == null) return;
        for (int id : siege.defenders()) {
            Trap trap = plugin.traps().byId(id);
            if (trap == null) continue;
            trap.members().clear();
            trap.setOwner(source.owner(), source.ownerName());
            plugin.holograms().refresh(trap);
        }
        announce(siege, "siege.won-attack", Map.of("attacker", source.ownerName(), "defender", names(siege.defenders())));
    }

    private Set<Integer> side(Player player) {
        Set<UUID> group = plugin.groups().groupOf(player);
        Set<Integer> ids = new LinkedHashSet<>();
        if (!group.isEmpty()) {
            for (Trap trap : plugin.traps().all()) {
                if (trap.owner() != null && group.contains(trap.owner())) ids.add(trap.id());
            }
        }
        if (ids.isEmpty()) {
            Trap own = plugin.trapService().primaryOwned(player);
            if (own != null) ids.add(own.id());
        }
        return ids;
    }

    private Set<Integer> sideOfTrap(Trap target) {
        Player online = target.owner() == null ? null : Bukkit.getPlayer(target.owner());
        if (online == null) return new LinkedHashSet<>(Set.of(target.id()));
        Set<Integer> ids = side(online);
        ids.add(target.id());
        return ids;
    }

    private Trap memberTrap(Player player) {
        for (Trap trap : plugin.traps().of(player.getUniqueId())) return trap;
        return null;
    }

    private boolean allowed(String world) {
        List<String> worlds = plugin.getConfig().getStringList("siege.outside-worlds");
        if (worlds.isEmpty()) return world.equalsIgnoreCase(plugin.getConfig().getString("settings.trap-world", "trap"));
        return worlds.contains("*") || worlds.stream().anyMatch(w -> w.equalsIgnoreCase(world));
    }

    private void announce(Siege siege, String key, Map<String, String> vars) {
        for (int id : siege.all()) {
            Trap trap = plugin.traps().byId(id);
            if (trap != null) plugin.trapService().notifyMembers(trap, key, vars);
        }
    }

    private String names(Set<Integer> ids) {
        StringBuilder sb = new StringBuilder();
        for (int id : ids) {
            Trap trap = plugin.traps().byId(id);
            if (trap == null) continue;
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(trap.name());
        }
        return sb.toString();
    }
}

