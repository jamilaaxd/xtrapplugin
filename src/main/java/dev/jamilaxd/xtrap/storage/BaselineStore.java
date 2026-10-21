package dev.jamilaxd.xtrap.storage;

import dev.jamilaxd.xtrap.XTrapPlugin;
import dev.jamilaxd.xtrap.model.ChunkKey;
import dev.jamilaxd.xtrap.model.Trap;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.BlockState;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class BaselineStore {

    private final XTrapPlugin plugin;
    private final File dir;

    public BaselineStore(XTrapPlugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "baseline");
        if (!dir.exists() && !dir.mkdirs()) plugin.getLogger().warning("baseline klasoru yok");
    }

    public void capture(Trap trap) {
        World world = Bukkit.getWorld(trap.world());
        if (world == null) return;
        List<BlockState> states = snapshot(world, trap);
        try {
            Schematic.write(file(trap), states);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Baseline yazilamadi #" + trap.id(), ex);
        }
    }

    public boolean restore(Trap trap) {
        File file = file(trap);
        if (!file.exists()) return false;
        World world = Bukkit.getWorld(trap.world());
        if (world == null) return false;
        try {
            Schematic.read(file, world);
            return true;
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Baseline okunamadi #" + trap.id(), ex);
            return false;
        }
    }

    public boolean has(Trap trap) {
        return file(trap).exists();
    }

    private List<BlockState> snapshot(World world, Trap trap) {
        List<BlockState> states = new ArrayList<>();
        int min = world.getMinHeight();
        int max = world.getMaxHeight();
        for (ChunkKey chunk : trap.chunks()) {
            if (!world.isChunkLoaded(chunk.x(), chunk.z())) world.loadChunk(chunk.x(), chunk.z());
            int x0 = chunk.x() << 4;
            int z0 = chunk.z() << 4;
            for (int x = x0; x < x0 + 16; x++) {
                for (int z = z0; z < z0 + 16; z++) {
                    int top = world.getHighestBlockYAt(x, z);
                    for (int y = min; y <= top && y < max; y++) {
                        states.add(world.getBlockAt(x, y, z).getState());
                    }
                }
            }
        }
        return states;
    }

    private File file(Trap trap) {
        return new File(dir, trap.id() + ".bin");
    }
}
