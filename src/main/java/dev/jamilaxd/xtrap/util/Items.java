package dev.jamilaxd.xtrap.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class Items {

    private Items() {
    }

    public static ItemStack named(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.comp(name));
        if (lore != null && !lore.isEmpty()) {
            meta.lore(Text.lore(lore));
        }
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack tagged(Material material, String name, List<String> lore, NamespacedKey key, String value) {
        ItemStack item = named(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        item.setItemMeta(meta);
        return item;
    }

    public static String tag(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }
}
