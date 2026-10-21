package dev.jamilaxd.xtrap.placeholder;

import dev.jamilaxd.xtrap.XTrapPlugin;
import dev.jamilaxd.xtrap.model.Trap;
import dev.jamilaxd.xtrap.model.TrapStatus;
import dev.jamilaxd.xtrap.util.Money;

import java.util.Locale;

public class TrapPlaceholders {

    private final XTrapPlugin plugin;

    public TrapPlaceholders(XTrapPlugin plugin) {
        this.plugin = plugin;
    }

    public String apply(Trap trap, String line) {
        if (trap == null || line == null || !line.contains("%trapcore_")) return line == null ? "" : line;
        String out = line;
        for (String token : tokens(line)) {
            out = out.replace("%trapcore_" + token + "%", value(trap, token));
        }
        return out;
    }

    public String value(Trap trap, String token) {
        if (trap == null) return "";
        boolean siege = plugin.sieges().of(trap) != null;
        String status = siege ? plugin.messages().raw("status.siege") : plugin.messages().raw(trap.status().messageKey());
        return switch (token.toLowerCase(Locale.ROOT)) {
            case "name" -> trap.name();
            case "id" -> String.valueOf(trap.id());
            case "owner" -> trap.ownerName();
            case "health" -> String.valueOf(trap.health());
            case "max_health" -> String.valueOf(trap.maxHealth());
            case "members" -> String.valueOf(trap.members().size());
            case "member_list" -> trap.memberNames();
            case "online" -> String.valueOf(trap.onlineCount());
            case "kills" -> String.valueOf(trap.kills());
            case "deaths" -> String.valueOf(trap.deaths());
            case "price" -> Money.format(trap.forSale() ? trap.salePrice() : plugin.getConfig().getDouble("trap-defaults.price"));
            case "sale_price" -> Money.format(trap.salePrice());
            case "default_price" -> Money.format(plugin.getConfig().getDouble("trap-defaults.price"));
            case "status" -> status;
            case "status_raw" -> siege ? "SIEGE" : trap.status().name();
            case "forsale" -> trap.forSale() ? "evet" : "hayir";
            case "collapsed" -> trap.status() == TrapStatus.COLLAPSED ? "evet" : "hayir";
            case "reclaim_left", "reset_left" -> reclaimLeft(trap);
            default -> "";
        };
    }

    private String reclaimLeft(Trap trap) {
        if (trap.status() != TrapStatus.COLLAPSED || trap.collapsedAt() <= 0) return "-";
        long window = plugin.getConfig().getLong("collapse.reclaim-seconds", 3600) * 1000L;
        long left = Math.max(0L, window - (System.currentTimeMillis() - trap.collapsedAt()));
        long total = left / 1000L;
        long hours = total / 3600L;
        long minutes = (total % 3600L) / 60L;
        long seconds = total % 60L;
        if (hours > 0) return hours + "s " + minutes + "dk";
        if (minutes > 0) return minutes + "dk " + seconds + "sn";
        return seconds + "sn";
    }

    private static String[] tokens(String line) {
        java.util.List<String> found = new java.util.ArrayList<>();
        int i = 0;
        while ((i = line.indexOf("%trapcore_", i)) >= 0) {
            int end = line.indexOf('%', i + 10);
            if (end < 0) break;
            found.add(line.substring(i + 10, end));
            i = end + 1;
        }
        return found.toArray(String[]::new);
    }
}
