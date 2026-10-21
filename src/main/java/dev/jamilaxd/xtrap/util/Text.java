package dev.jamilaxd.xtrap.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public final class Text {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private Text() {
    }

    public static Component comp(String raw) {
        if (raw == null) {
            return Component.empty();
        }
        return LEGACY.deserialize(raw);
    }

    public static void send(CommandSender sender, String prefix, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        sender.sendMessage(comp((prefix == null ? "" : prefix) + message));
    }

    public static List<Component> lore(List<String> lines) {
        List<Component> out = new ArrayList<>();
        if (lines == null) {
            return out;
        }
        for (String line : lines) {
            out.add(comp(line));
        }
        return out;
    }

    public static String plain(String raw) {
        return LEGACY.serialize(comp(raw));
    }
}
