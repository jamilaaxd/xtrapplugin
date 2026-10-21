package dev.jamilaxd.xtrap.selection;

import dev.jamilaxd.xtrap.XTrapPlugin;
import dev.jamilaxd.xtrap.model.ChunkKey;
import dev.jamilaxd.xtrap.util.Items;
import net.kyori.adventure.text.Component;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SelectionManager {

    private final XTrapPlugin plugin;
    private final NamespacedKey key;
    private final Map<UUID, Pending> pending = new HashMap<>();

    public SelectionManager(XTrapPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "wand");
    }

    public void begin(Player player, int trapId) {
        pending.put(player.getUniqueId(), new Pending(trapId, new HashSet<>()));
        Material material = Material.matchMaterial(plugin.getConfig().getString("settings.wand-material", "BLAZE_ROD"));
        if (material == null) material = Material.BLAZE_ROD;
        ItemStack wand = Items.tagged(material, "&6Trap Cubugu &7(#" + trapId + ")",
                java.util.List.of("&7Sol tik: chunk ekle/cikar", "&7Birak: kaydet", "&8id:" + trapId), key, String.valueOf(trapId));
        player.getInventory().addItem(wand);
    }

    public boolean isWand(ItemStack item) {
        return Items.tag(item, key) != null;
    }

    public Pending get(UUID uuid) {
        return pending.get(uuid);
    }

    public Pending take(UUID uuid) {
        return pending.remove(uuid);
    }

    public void toggle(Player player, Block clicked) {
        Pending sel = pending.get(player.getUniqueId());
        if (sel == null || player.getWorld() == null) return;
        Block target = clicked != null ? clicked : aimed(player);
        if (target == null || target.getWorld() == null) {
            player.sendActionBar(Component.text("Hedef blok yok"));
            return;
        }
        ChunkKey chunk = new ChunkKey(target.getWorld().getName(), target.getX() >> 4, target.getZ() >> 4);
        if (!sel.chunks.add(chunk)) {
            sel.chunks.remove(chunk);
            restore(player, chunk);
        }
        boolean on = sel.chunks.contains(chunk);
        player.sendActionBar(Component.text((on ? "+ " : "- ") + chunk.x() + ", " + chunk.z() + " (" + sel.chunks.size() + ")"));
        if (on) paint(player, chunk, target.getY());
    }

    private Block aimed(Player player) {
        double reach = plugin.getConfig().getDouble("settings.wand-reach", 120.0);
        RayTraceResult hit = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getEyeLocation().getDirection(), reach, FluidCollisionMode.NEVER, true);
        return hit == null ? null : hit.getHitBlock();
    }

    private void paint(Player player, ChunkKey chunk, int y) {
        var world = plugin.getServer().getWorld(chunk.world());
        if (world == null) return;
        Pending sel = pending.get(player.getUniqueId());
        if (sel != null) sel.markY(y);
        int baseX = chunk.x() << 4;
        int baseZ = chunk.z() << 4;
        for (int x = 0; x < 16; x += 2) {
            for (int z = 0; z < 16; z += 2) {
                if (x != 0 && x != 14 && z != 0 && z != 14) continue;
                player.sendBlockChange(new org.bukkit.Location(world, baseX + x, y, baseZ + z), Material.LIME_STAINED_GLASS.createBlockData());
            }
        }
    }

    private void restore(Player player, ChunkKey chunk) {
        Pending sel = pending.get(player.getUniqueId());
        if (sel == null) return;
        var world = plugin.getServer().getWorld(chunk.world());
        if (world == null) return;
        int baseX = chunk.x() << 4;
        int baseZ = chunk.z() << 4;
        for (int y : sel.paintedY) {
            for (int x = 0; x < 16; x += 2) {
                for (int z = 0; z < 16; z += 2) {
                    if (x != 0 && x != 14 && z != 0 && z != 14) continue;
                    org.bukkit.Location loc = new org.bukkit.Location(world, baseX + x, y, baseZ + z);
                    player.sendBlockChange(loc, world.getBlockAt(loc).getBlockData());
                }
            }
        }
    }

    public void clearPreview(Player player, Pending sel) {
        if (sel == null) return;
        for (ChunkKey chunk : sel.chunks) {
            var world = plugin.getServer().getWorld(chunk.world());
            if (world == null) continue;
            int baseX = chunk.x() << 4;
            int baseZ = chunk.z() << 4;
            for (int y : sel.paintedY) {
                for (int x = 0; x < 16; x += 2) {
                    for (int z = 0; z < 16; z += 2) {
                        if (x != 0 && x != 14 && z != 0 && z != 14) continue;
                        org.bukkit.Location loc = new org.bukkit.Location(world, baseX + x, y, baseZ + z);
                        player.sendBlockChange(loc, world.getBlockAt(loc).getBlockData());
                    }
                }
            }
        }
        sel.paintedY.clear();
    }

    public static final class Pending {
        public final int trapId;
        public final Set<ChunkKey> chunks;
        public final Set<Integer> paintedY = new HashSet<>();

        Pending(int trapId, Set<ChunkKey> chunks) {
            this.trapId = trapId;
            this.chunks = chunks;
        }

        void markY(int y) {
            paintedY.add(y);
        }
    }
}
