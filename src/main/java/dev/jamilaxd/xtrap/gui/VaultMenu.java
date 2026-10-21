package dev.jamilaxd.xtrap.gui;

import dev.jamilaxd.xtrap.XTrapPlugin;
import dev.jamilaxd.xtrap.model.Trap;
import dev.jamilaxd.xtrap.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VaultMenu {

    private final XTrapPlugin plugin;
    private final Map<UUID, Session> open = new HashMap<>();

    public VaultMenu(XTrapPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Trap trap, int page) {
        int rows = Math.max(1, Math.min(6, trap.vaultRows()));
        int size = rows * 9;
        int used = trap.vault().keySet().stream().mapToInt(Integer::intValue).max().orElse(-1) + 1;
        int pages = Math.max(1, (used + size - 1) / size);
        page = Math.max(1, Math.min(page, pages));
        Inventory inventory = Bukkit.createInventory(null, size,
                Text.comp(plugin.messages().format("menu.vault-title", Map.of("trap", trap.name(), "page", String.valueOf(page)))));
        int start = (page - 1) * size;
        for (int slot = 0; slot < size; slot++) {
            ItemStack item = decode(trap.vault().get(start + slot));
            if (item != null) inventory.setItem(slot, item);
        }
        open.put(player.getUniqueId(), new Session(trap.id(), page, rows));
        player.openInventory(inventory);
    }

    public Session session(UUID player) {
        return open.get(player);
    }

    public void close(Player player, Inventory inventory) {
        Session session = open.remove(player.getUniqueId());
        if (session == null) return;
        Trap trap = plugin.traps().byId(session.trapId());
        if (trap == null) return;
        int start = (session.page() - 1) * session.rows() * 9;
        for (int slot = 0; slot < session.rows() * 9; slot++) {
            String data = encode(inventory.getItem(slot));
            if (data == null) trap.vault().remove(start + slot);
            else trap.vault().put(start + slot, data);
        }
    }

    @SuppressWarnings("deprecation")
    private static String encode(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        try (ByteArrayOutputStream raw = new ByteArrayOutputStream();
             BukkitObjectOutputStream out = new BukkitObjectOutputStream(raw)) {
            out.writeObject(item);
            return Base64.getEncoder().encodeToString(raw.toByteArray());
        } catch (Exception ex) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private static ItemStack decode(String data) {
        if (data == null || data.isBlank()) return null;
        try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(data)))) {
            return (ItemStack) in.readObject();
        } catch (Exception ex) {
            return null;
        }
    }

    public record Session(int trapId, int page, int rows) {}
}
