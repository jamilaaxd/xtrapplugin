package dev.jamilaxd.xtrap.listener;

import dev.jamilaxd.xtrap.XTrapPlugin;
import dev.jamilaxd.xtrap.model.Rank;
import dev.jamilaxd.xtrap.model.Trap;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProtectionListener implements Listener {

    private final XTrapPlugin plugin;
    private final Map<UUID, Long> told = new HashMap<>();

    public ProtectionListener(XTrapPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!deny(event.getPlayer(), event.getBlock(), true)) return;
        event.setCancelled(true);
        tell(event.getPlayer(), "protection.deny-break");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!deny(event.getPlayer(), event.getBlock(), true)) return;
        event.setCancelled(true);
        tell(event.getPlayer(), "protection.deny-build");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || plugin.selections().isWand(event.getItem()) || !isInteract(block.getType())) return;
        if (!deny(event.getPlayer(), block, false)) return;
        event.setCancelled(true);
        tell(event.getPlayer(), "protection.deny-interact");
    }

    private boolean deny(Player player, Block block, boolean build) {
        if (player.hasPermission("xtrap.admin.bypass")) return false;
        Trap trap = plugin.traps().at(block.getLocation());
        if (trap == null || trap.owner() == null) return false;
        if (trap.isRaidable() && plugin.getConfig().getBoolean("collapse.allow-raid", true)) return false;
        Rank rank = trap.rankOf(player.getUniqueId());
        if (rank == null) return true;
        return build ? !rank.canBuild() : !rank.canInteract();
    }

    private boolean isInteract(Material type) {
        String name = type.name();
        if (plugin.getConfig().getBoolean("protection.doors", true)
                && (name.endsWith("_DOOR") || name.endsWith("_FENCE_GATE") || name.endsWith("_TRAPDOOR") || name.endsWith("_FENCE"))) {
            return true;
        }
        return plugin.getConfig().getStringList("protection.interact").contains(name);
    }

    private void tell(Player player, String key) {
        long now = System.currentTimeMillis();
        if (now - told.getOrDefault(player.getUniqueId(), 0L) < 1500) return;
        told.put(player.getUniqueId(), now);
        plugin.messages().send(player, key);
    }
}
