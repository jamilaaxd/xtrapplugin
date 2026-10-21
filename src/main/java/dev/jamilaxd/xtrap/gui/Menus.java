package dev.jamilaxd.xtrap.gui;

import dev.jamilaxd.xtrap.XTrapPlugin;
import dev.jamilaxd.xtrap.model.Rank;
import dev.jamilaxd.xtrap.model.Trap;
import dev.jamilaxd.xtrap.model.TrapMember;
import dev.jamilaxd.xtrap.util.Items;
import dev.jamilaxd.xtrap.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

public class Menus {

    public record BuyHolder(int trapId) implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    public record ListHolder(List<Trap> traps) implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    public record ManageHolder(int trapId) implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    public record RankHolder(int trapId, UUID target) implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    private final XTrapPlugin plugin;
    private final NamespacedKey action;

    public Menus(XTrapPlugin plugin) {
        this.plugin = plugin;
        this.action = new NamespacedKey(plugin, "gui");
    }

    public NamespacedKey action() { return action; }

    public void buy(Player player, Trap trap) {
        Inventory inv = Bukkit.createInventory(new BuyHolder(trap.id()), 27, Text.comp("&8Satin al: " + trap.name()));
        inv.setItem(11, button(Material.LIME_WOOL, "&aSatin al", List.of("&7Fiyat: &f" + plugin.trapService().priceText(trap.salePrice())), "buy"));
        inv.setItem(15, button(Material.RED_WOOL, "&cIptal", List.of("&7Pencereyi kapat"), "cancel"));
        player.openInventory(inv);
    }

    public void mine(Player player) {
        List<Trap> traps = plugin.traps().of(player.getUniqueId());
        int size = Math.max(9, Math.min(54, ((traps.size() + 8) / 9) * 9));
        Inventory inv = Bukkit.createInventory(new ListHolder(traps), size, Text.comp(plugin.messages().raw("list.gui-title")));
        for (int i = 0; i < traps.size() && i < size; i++) inv.setItem(i, icon(traps.get(i), player));
        player.openInventory(inv);
    }

    public void manage(Player player, Trap trap) {
        Inventory inv = Bukkit.createInventory(new ManageHolder(trap.id()), 54, Text.comp("&8" + trap.name()));
        int slot = 0;
        for (TrapMember member : trap.memberList()) {
            if (slot >= 45) break;
            ItemStack head = button(Material.PLAYER_HEAD, "&f" + member.name(), List.of(
                    "&7Rutbe: &f" + member.rank().display(),
                    "&7Oldurme: &f" + member.kills(),
                    "&7Olum: &f" + member.deaths(),
                    "&eTikla: rutbe / at"), "member:" + member.uuid());
            inv.setItem(slot++, head);
        }
        player.openInventory(inv);
    }

    public void ranks(Player player, Trap trap, TrapMember member) {
        Inventory inv = Bukkit.createInventory(new RankHolder(trap.id(), member.uuid()), 27, Text.comp("&8" + member.name()));
        inv.setItem(10, button(Material.IRON_INGOT, "&7Uye", List.of(), "rank:UYE"));
        inv.setItem(12, button(Material.BRICKS, "&eMimar", List.of("&7Blok kirar/koyar"), "rank:MIMAR"));
        inv.setItem(14, button(Material.GOLD_INGOT, "&6Moderator", List.of("&7Uye ve depo yonetir"), "rank:MODERATOR"));
        inv.setItem(16, button(Material.DIAMOND, "&bLider devret", List.of("&7Trapi bu oyuncuya ver"), "rank:LIDER"));
        inv.setItem(22, button(Material.BARRIER, "&cAtdan cikar", List.of(), "kick"));
        player.openInventory(inv);
    }

    private ItemStack icon(Trap trap, Player player) {
        Rank rank = trap.rankOf(player.getUniqueId());
        return button(Material.GRASS_BLOCK, "&6" + trap.name(), List.of(
                "&7ID: &f" + trap.id(),
                "&7Rutbe: &f" + (rank == null ? "-" : rank.display()),
                "&7Can: &c" + trap.health(),
                "&7Durum: &f" + plugin.messages().raw(trap.status().messageKey())), "open:" + trap.id());
    }

    private ItemStack button(Material material, String name, List<String> lore, String value) {
        ItemStack item = Items.named(material, name, lore);
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(action, PersistentDataType.STRING, value);
        item.setItemMeta(meta);
        return item;
    }
}
