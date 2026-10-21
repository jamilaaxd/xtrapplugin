package dev.jamilaxd.xtrap.listener;

import dev.jamilaxd.xtrap.XTrapPlugin;
import dev.jamilaxd.xtrap.model.ChunkKey;
import dev.jamilaxd.xtrap.model.Trap;
import dev.jamilaxd.xtrap.selection.SelectionManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class WandListener implements Listener {

    private final XTrapPlugin plugin;

    public WandListener(XTrapPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onUse(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (!plugin.selections().isWand(item)) return;
        event.setCancelled(true);
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK
                && action != Action.LEFT_CLICK_AIR && action != Action.RIGHT_CLICK_AIR) return;
        plugin.selections().toggle(event.getPlayer(), event.getClickedBlock());
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (!plugin.selections().isWand(item)) return;
        event.getItemDrop().remove();
        SelectionManager.Pending pending = plugin.selections().take(event.getPlayer().getUniqueId());
        if (pending == null) return;
        plugin.selections().clearPreview(event.getPlayer(), pending);
        if (pending.chunks.isEmpty()) {
            plugin.messages().send(event.getPlayer(), "admin.need-chunks");
            return;
        }
        Trap trap = plugin.traps().byId(pending.trapId);
        if (trap == null) return;
        String world = pending.chunks.iterator().next().world();
        for (ChunkKey chunk : pending.chunks) {
            Trap other = plugin.traps().byChunk(chunk);
            if (other != null && other.id() != trap.id()) {
                plugin.messages().send(event.getPlayer(), "unknown-trap", java.util.Map.of("trap", chunk + " dolu (#" + other.id() + ")"));
                return;
            }
        }
        trap.chunks().clear();
        trap.chunks().addAll(pending.chunks);
        trap.world(world);
        plugin.traps().reindexChunks(trap);
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.baseline().capture(trap));
        plugin.messages().send(event.getPlayer(), "admin.selection-saved", java.util.Map.of(
                "id", String.valueOf(trap.id()), "count", String.valueOf(pending.chunks.size())));
    }
}
