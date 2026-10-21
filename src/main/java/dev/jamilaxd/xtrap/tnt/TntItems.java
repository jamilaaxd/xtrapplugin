package dev.jamilaxd.xtrap.tnt;

import dev.jamilaxd.xtrap.XTrapPlugin;
import dev.jamilaxd.xtrap.util.Items;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

public class TntItems {

    private final XTrapPlugin plugin;
    private final NamespacedKey key;

    public TntItems(XTrapPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "trap_tnt");
    }

    public NamespacedKey key() {
        return key;
    }

    public ItemStack create(int amount) {
        ItemStack item = Items.tagged(Material.TNT,
                plugin.getConfig().getString("tnt.display-name", "&cTrap TNT"),
                plugin.getConfig().getStringList("tnt.lore"),
                key, "1");
        item.setAmount(Math.max(1, amount));
        return item;
    }

    public boolean isTrapTnt(ItemStack item) {
        return "1".equals(Items.tag(item, key));
    }
}
