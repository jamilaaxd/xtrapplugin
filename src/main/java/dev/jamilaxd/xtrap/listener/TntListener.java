package dev.jamilaxd.xtrap.listener;

import dev.jamilaxd.xtrap.XTrapPlugin;
import dev.jamilaxd.xtrap.model.Trap;
import dev.jamilaxd.xtrap.tnt.RestoreTask;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class TntListener implements Listener {

    private final XTrapPlugin plugin;

    public TntListener(XTrapPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!plugin.tnt().isTrapTnt(item)) return;
        Block block = event.getBlockPlaced();
        if (!event.getPlayer().hasPermission("xtrap.tnt")) {
            event.setCancelled(true);
            plugin.messages().send(event.getPlayer(), "tnt.no-permission");
            return;
        }
        if (plugin.getConfig().getBoolean("tnt.only-inside-traps", true) && plugin.traps().at(block.getLocation()) == null) {
            event.setCancelled(true);
            plugin.messages().send(event.getPlayer(), "not-in-trap");
            return;
        }
        block.setType(Material.AIR);
        Location loc = block.getLocation().add(0.5, 0, 0.5);
        TNTPrimed tnt = block.getWorld().spawn(loc, TNTPrimed.class, entity -> {
            entity.setSource(event.getPlayer());
            entity.setFuseTicks(fuse(event.getPlayer()));
            entity.addScoreboardTag("xtrap-tnt");
        });
        tnt.setYield((float) plugin.getConfig().getDouble("tnt.power", 4.0));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof TNTPrimed tnt) || !tnt.getScoreboardTags().contains("xtrap-tnt")) return;
        Trap origin = plugin.traps().at(tnt.getLocation());
        List<Block> broken = new ArrayList<>();
        event.blockList().removeIf(block -> {
            Trap trap = plugin.traps().at(block.getLocation());
            if (trap == null || trap != origin) return true;
            if (!canDamage(trap)) return true;
            broken.add(block);
            return false;
        });
        if (origin != null && canDamage(origin) && !broken.isEmpty()) {
            plugin.trapService().damage(origin, broken.size());
        }
        if (!broken.isEmpty()) {
            long ticks = plugin.getConfig().getInt("tnt.restore-seconds", 5) * 20L;
            RestoreTask.schedule(plugin, broken, ticks);
        }
    }

    private int fuse(Player player) {
        String path = player.hasPermission("xtrap.tnt.fast") ? "tnt.fuse-fast-seconds" : "tnt.fuse-seconds";
        int fallback = player.hasPermission("xtrap.tnt.fast") ? 3 : 5;
        return Math.max(1, plugin.getConfig().getInt(path, fallback)) * 20;
    }

    private boolean canDamage(Trap trap) {
        if (trap.owner() == null && !plugin.getConfig().getBoolean("tnt.damage-unowned", false)) return false;
        if (plugin.sieges().of(trap) != null && !plugin.getConfig().getBoolean("tnt.damage-during-siege", false)) return false;
        return true;
    }
}
