package dev.jamilaxd.xtrap.tnt;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public final class RestoreTask implements Runnable {

    private final List<BlockState> states = new ArrayList<>();

    public static void schedule(Plugin plugin, List<Block> blocks, long delayTicks) {
        RestoreTask task = new RestoreTask();
        for (Block block : blocks) {
            if (block == null) continue;
            BlockState state = block.getState();
            if (state instanceof Container container) {
                ItemStack[] copy = new ItemStack[container.getInventory().getSize()];
                ItemStack[] original = container.getInventory().getContents();
                for (int i = 0; i < original.length; i++) {
                    copy[i] = original[i] == null ? null : original[i].clone();
                }
                container.getInventory().setContents(copy);
            }
            task.states.add(state);
        }
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    @Override
    public void run() {
        for (BlockState state : states) {
            Block block = state.getBlock();
            if (!block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) {
                block.getWorld().loadChunk(block.getX() >> 4, block.getZ() >> 4);
            }
            state.update(true, false);
        }
    }
}
