package dev.jamilaxd.xtrap.storage;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

public final class Schematic {

    private Schematic() {
    }

    public static void write(File file, List<BlockState> states) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            out.writeInt(states.size());
            for (BlockState state : states) {
                out.writeInt(state.getX());
                out.writeInt(state.getY());
                out.writeInt(state.getZ());
                out.writeUTF(state.getBlockData().getAsString());
                if (state instanceof Container container) {
                    ItemStack[] items = container.getInventory().getContents();
                    out.writeInt(items.length);
                    for (ItemStack item : items) {
                        out.writeUTF(item == null ? "" : Base64.getEncoder().encodeToString(item.serializeAsBytes()));
                    }
                } else {
                    out.writeInt(-1);
                }
            }
        }
    }

    public static void read(File file, World world) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                int x = in.readInt();
                int y = in.readInt();
                int z = in.readInt();
                var data = Bukkit.createBlockData(in.readUTF());
                int inv = in.readInt();
                ItemStack[] items = null;
                if (inv >= 0) {
                    items = new ItemStack[inv];
                    for (int s = 0; s < inv; s++) {
                        String raw = in.readUTF();
                        if (!raw.isEmpty()) items[s] = ItemStack.deserializeBytes(Base64.getDecoder().decode(raw));
                    }
                }
                world.getBlockAt(x, y, z).setBlockData(data, false);
                if (items != null && world.getBlockAt(x, y, z).getState() instanceof Container container) {
                    container.getInventory().setContents(items);
                    container.update(true, false);
                }
            }
        }
    }
}
