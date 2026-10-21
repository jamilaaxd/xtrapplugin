package dev.jamilaxd.xtrap.trap;

import dev.jamilaxd.xtrap.XTrapPlugin;
import dev.jamilaxd.xtrap.model.Rank;
import dev.jamilaxd.xtrap.model.Relation;
import dev.jamilaxd.xtrap.model.Trap;
import dev.jamilaxd.xtrap.model.TrapMember;
import dev.jamilaxd.xtrap.model.TrapStatus;
import dev.jamilaxd.xtrap.util.Money;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TrapService {

    public record Invite(int trapId, long at) {}

    private final XTrapPlugin plugin;
    private final Map<UUID, List<Invite>> invites = new HashMap<>();

    public TrapService(XTrapPlugin plugin) {
        this.plugin = plugin;
    }

    public void applyDefaults(Trap trap, boolean forSale) {
        trap.health(plugin.getConfig().getInt("trap-defaults.health", 1000));
        trap.maxHealth(plugin.getConfig().getInt("health.max-health", 5000));
        if (trap.maxHealth() > 0 && trap.health() > trap.maxHealth()) trap.health(trap.maxHealth());
        trap.memberLimit(plugin.getConfig().getInt("trap-defaults.member-limit", 5));
        trap.vaultRows(plugin.getConfig().getInt("trap-defaults.vault-rows", 1));
        trap.salePrice(plugin.getConfig().getDouble("trap-defaults.price", 50000));
        trap.forSale(forSale);
        trap.status(forSale ? TrapStatus.FOR_SALE : TrapStatus.EMPTY);
    }

    public boolean charge(Player player, double price) {
        return plugin.economy().available() && plugin.economy().withdraw(player, price);
    }

    public int ownedCount(UUID uuid) {
        int n = 0;
        for (Trap trap : plugin.traps().all()) if (uuid.equals(trap.owner())) n++;
        return n;
    }

    public String priceText(double price) {
        return Money.format(price);
    }

    public void giveOwnership(Trap trap, Player player) {
        if (!plugin.getConfig().getBoolean("settings.keep-vault-on-sale", true)) trap.vault().clear();
        if (!plugin.getConfig().getBoolean("settings.keep-members-on-sale", false)) {
            trap.members().clear();
            trap.relations().clear();
        }
        trap.setOwner(player.getUniqueId(), player.getName());
        if (trap.health() <= 0) trap.health(plugin.getConfig().getInt("trap-defaults.health", 1000));
        trap.status(TrapStatus.OWNED);
        trap.forSale(false);
        trap.collapsedAt(0);
        plugin.holograms().refresh(trap);
    }

    public void release(Trap trap) {
        trap.clearOwnership(false);
        applyDefaults(trap, true);
        plugin.baseline().restore(trap);
        plugin.holograms().refresh(trap);
    }

    public void collapse(Trap trap) {
        if (trap.status() == TrapStatus.COLLAPSED || trap.owner() == null) return;
        trap.health(0);
        trap.status(TrapStatus.COLLAPSED);
        trap.collapsedAt(System.currentTimeMillis());
        plugin.messages().broadcast("collapse.collapsed", Map.of(
                "trap", trap.name(),
                "seconds", String.valueOf(plugin.getConfig().getInt("collapse.reclaim-seconds", 3600))));
        plugin.holograms().refresh(trap);
    }

    public void reclaim(Trap trap) {
        trap.status(TrapStatus.OWNED);
        trap.collapsedAt(0);
        trap.health(Math.max(1, plugin.getConfig().getInt("trap-defaults.health", 1000) / 4));
        plugin.holograms().refresh(trap);
    }

    public void tickCollapses() {
        long window = plugin.getConfig().getLong("collapse.reclaim-seconds", 3600) * 1000L;
        long now = System.currentTimeMillis();
        for (Trap trap : new ArrayList<>(plugin.traps().all())) {
            if (trap.status() == TrapStatus.COLLAPSED && trap.collapsedAt() > 0 && now - trap.collapsedAt() >= window) {
                plugin.messages().broadcast("collapse.reset", Map.of("trap", trap.name()));
                release(trap);
            }
        }
    }

    public void damage(Trap trap, int amount) {
        if (amount <= 0 || trap.owner() == null || trap.status() == TrapStatus.COLLAPSED) return;
        if (plugin.sieges().isHealthFrozen(trap)) return;
        trap.health(Math.max(plugin.getConfig().getInt("health.min-from-tnt", 0), trap.health() - amount));
        plugin.holograms().refresh(trap);
        if (trap.health() <= 0) collapse(trap);
    }

    public void heal(Trap trap, int amount) {
        if (amount <= 0 || trap.owner() == null || trap.status() != TrapStatus.OWNED) return;
        int next = trap.health() + amount;
        if (trap.maxHealth() > 0) next = Math.min(trap.maxHealth(), next);
        trap.health(next);
        plugin.holograms().refresh(trap);
    }

    public void invite(Trap trap, Player target) {
        List<Invite> list = invites.computeIfAbsent(target.getUniqueId(), k -> new ArrayList<>());
        list.removeIf(i -> i.trapId == trap.id());
        list.add(new Invite(trap.id(), System.currentTimeMillis()));
    }

    public List<Invite> invites(UUID uuid) {
        List<Invite> list = invites.computeIfAbsent(uuid, k -> new ArrayList<>());
        long now = System.currentTimeMillis();
        list.removeIf(i -> now - i.at > 600_000L || plugin.traps().byId(i.trapId) == null);
        return list;
    }

    public Invite takeInvite(UUID uuid, Trap trap) {
        Iterator<Invite> it = invites(uuid).iterator();
        while (it.hasNext()) {
            Invite invite = it.next();
            if (trap == null || invite.trapId == trap.id()) {
                it.remove();
                return plugin.traps().byId(invite.trapId) == null ? null : invite;
            }
        }
        return null;
    }

    public boolean join(Trap trap, Player player) {
        if (trap.members().size() >= trap.memberLimit()) return false;
        int cap = plugin.getConfig().getInt("settings.max-member-traps", 5);
        if (cap > 0 && plugin.traps().of(player.getUniqueId()).size() >= cap) return false;
        trap.members().put(player.getUniqueId(), new TrapMember(player.getUniqueId(), player.getName(), Rank.UYE));
        plugin.holograms().refresh(trap);
        return true;
    }

    public boolean areAllies(Trap a, Trap b) {
        return a != null && b != null && a.relationTo(b.id()) == Relation.ALLY && b.relationTo(a.id()) == Relation.ALLY;
    }

    public boolean isEnemy(Trap attacker, Trap victim) {
        return attacker != null && victim != null && attacker.relationTo(victim.id()) == Relation.ENEMY;
    }

    public Trap primaryOwned(Player player) {
        Trap best = null;
        for (Trap trap : plugin.traps().of(player.getUniqueId())) {
            if (player.getUniqueId().equals(trap.owner()) && (best == null || trap.id() < best.id())) best = trap;
        }
        return best;
    }

    public void pay(OfflinePlayer player, double amount) {
        if (plugin.economy().available()) plugin.economy().deposit(player, amount);
    }

    public void notifyMembers(Trap trap, String key, Map<String, String> vars) {
        for (UUID uuid : trap.members().keySet()) {
            Player online = Bukkit.getPlayer(uuid);
            if (online != null) plugin.messages().send(online, key, vars);
        }
    }
}

