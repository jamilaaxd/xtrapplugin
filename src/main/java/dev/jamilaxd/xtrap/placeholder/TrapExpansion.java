package dev.jamilaxd.xtrap.placeholder;

import dev.jamilaxd.xtrap.XTrapPlugin;
import dev.jamilaxd.xtrap.model.Trap;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class TrapExpansion extends PlaceholderExpansion {

    private final XTrapPlugin plugin;

    public TrapExpansion(XTrapPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "trapcore";
    }

    @Override
    public @NotNull String getAuthor() {
        return "jamilaxd";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        Trap trap = null;
        String token = params;
        int split = params.indexOf('_');
        if (split > 0) {
            String head = params.substring(0, split);
            Trap byId = null;
            try {
                byId = plugin.traps().byId(Integer.parseInt(head));
            } catch (NumberFormatException ignored) {
                byId = plugin.traps().byName(head);
            }
            if (byId != null) {
                trap = byId;
                token = params.substring(split + 1);
            }
        }
        if (trap == null && player != null && player.isOnline()) {
            trap = plugin.traps().at(player.getPlayer().getLocation());
        }
        if (trap == null) return "";
        return plugin.placeholders().value(trap, token);
    }
}
