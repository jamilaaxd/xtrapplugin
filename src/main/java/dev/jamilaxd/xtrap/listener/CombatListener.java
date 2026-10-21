package dev.jamilaxd.xtrap.listener;

import dev.jamilaxd.xtrap.XTrapPlugin;
import dev.jamilaxd.xtrap.model.Trap;
import dev.jamilaxd.xtrap.model.TrapMember;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.List;

public class CombatListener implements Listener {

    private final XTrapPlugin plugin;

    public CombatListener(XTrapPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = attacker(event.getDamager());
        if (attacker == null) return;
        if (!plugin.getConfig().getBoolean("protection.allies-cannot-pvp", true)) return;
        Trap a = memberOf(attacker);
        Trap b = memberOf(victim);
        if (plugin.trapService().areAllies(a, b)) {
            event.setCancelled(true);
            plugin.messages().send(attacker, "protection.deny-pvp");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) return;
        Trap victimHere = plugin.traps().at(victim.getLocation());
        Trap killerTrap = memberOf(killer);
        Trap victimTrap = memberOf(victim);
        creditStats(killerTrap, victimTrap, killer, victim);
        plugin.sieges().onKill(killer, victim);
        if (plugin.sieges().isHealthFrozen(victimHere) || plugin.sieges().isHealthFrozen(killerTrap)) return;
        Trap killerHere = plugin.traps().at(killer.getLocation());
        if (killerHere != null && killerHere.isOwned() && killerHere.isMember(killer.getUniqueId())
                && (victimTrap == null || !victimTrap.equals(killerHere))) {
            killerHere.addKill();
            TrapMember member = killerHere.member(killer.getUniqueId());
            if (member != null) member.addKill();
        }
        if (!plugin.trapService().isEnemy(killerTrap, victimTrap)) return;
        if (victimHere != null && victimHere.equals(victimTrap)) {
            plugin.trapService().damage(victimTrap, plugin.getConfig().getInt("health.enemy-health-taken", 25));
            plugin.trapService().heal(killerTrap, plugin.getConfig().getInt("health.enemy-health-gained", 10));
            return;
        }
        if (outside(victim.getWorld().getName())) {
            plugin.trapService().heal(killerTrap, plugin.getConfig().getInt("health.enemy-outside-heal", 5));
        }
    }

    private void creditStats(Trap killerTrap, Trap victimTrap, Player killer, Player victim) {
        if (killerTrap != null) {
            killerTrap.addKill();
            TrapMember member = killerTrap.member(killer.getUniqueId());
            if (member != null) member.addKill();
        }
        if (victimTrap != null) {
            victimTrap.addDeath();
            TrapMember member = victimTrap.member(victim.getUniqueId());
            if (member != null) member.addDeath();
        }
    }

    private boolean outside(String world) {
        List<String> worlds = plugin.getConfig().getStringList("health.enemy-outside-worlds");
        if (worlds.isEmpty()) return world.equalsIgnoreCase(plugin.getConfig().getString("settings.trap-world", "trap"));
        return worlds.contains("*") || worlds.stream().anyMatch(w -> w.equalsIgnoreCase(world));
    }

    private Trap memberOf(Player player) {
        for (Trap trap : plugin.traps().of(player.getUniqueId())) {
            if (player.getUniqueId().equals(trap.owner())) return trap;
        }
        var list = plugin.traps().of(player.getUniqueId());
        return list.isEmpty() ? null : list.get(0);
    }

    private Player attacker(org.bukkit.entity.Entity entity) {
        if (entity instanceof Player player) return player;
        if (entity instanceof org.bukkit.entity.Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        return null;
    }
}
