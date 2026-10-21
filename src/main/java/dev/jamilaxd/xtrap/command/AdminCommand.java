package dev.jamilaxd.xtrap.command;

import dev.jamilaxd.xtrap.XTrapPlugin;
import dev.jamilaxd.xtrap.model.ChunkKey;
import dev.jamilaxd.xtrap.model.Trap;
import dev.jamilaxd.xtrap.selection.SelectionManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final XTrapPlugin plugin;

    public AdminCommand(XTrapPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("xtrap.admin")) { plugin.messages().send(sender, "no-permission"); return true; }
        if (args.length == 0) { plugin.messages().sendList(sender, "admin.help"); return true; }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "wand" -> wand(sender);
            case "create" -> create(sender, args);
            case "delete" -> delete(sender, arg(args, 1));
            case "setprice" -> setPrice(sender, arg(args, 1), arg(args, 2));
            case "sethealth" -> setHealth(sender, arg(args, 1), arg(args, 2));
            case "setowner" -> setOwner(sender, arg(args, 1), arg(args, 2));
            case "reload" -> reload(sender);
            case "save" -> { plugin.storage().save(plugin.traps()); plugin.messages().send(sender, "admin.saved"); }
            case "info" -> info(sender, arg(args, 1));
            case "tp" -> tp(sender, arg(args, 1));
            case "tnt" -> tnt(sender, arg(args, 1));
            case "reset" -> reset(sender, arg(args, 1));
            case "recapture" -> recapture(sender, arg(args, 1));
            case "list" -> list(sender);
            default -> plugin.messages().sendList(sender, "admin.help");
        }
        return true;
    }

    private void wand(CommandSender sender) {
        if (!(sender instanceof Player player)) { plugin.messages().send(sender, "players-only"); return; }
        plugin.selections().begin(player, 0);
        plugin.messages().send(player, "admin.wand");
    }

    private void create(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { plugin.messages().send(sender, "players-only"); return; }
        SelectionManager.Pending pending = plugin.selections().get(player.getUniqueId());
        if (pending == null || pending.chunks.isEmpty()) { plugin.messages().send(player, "admin.need-chunks"); return; }
        for (ChunkKey chunk : pending.chunks) {
            if (plugin.traps().byChunk(chunk) != null) {
                plugin.messages().send(player, "unknown-trap", Map.of("trap", chunk + " dolu"));
                return;
            }
        }
        Trap trap = plugin.traps().create(plugin.traps().nextId());
        trap.world(pending.chunks.iterator().next().world());
        trap.chunks().addAll(pending.chunks);
        plugin.traps().index(trap);
        plugin.trapService().applyDefaults(trap, true);
        if (args.length >= 2) {
            try { trap.salePrice(Double.parseDouble(args[1])); } catch (NumberFormatException ignored) { }
        }
        plugin.selections().clearPreview(player, pending);
        plugin.selections().take(player.getUniqueId());
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.baseline().capture(trap));
        plugin.messages().send(player, "admin.created", Map.of("id", String.valueOf(trap.id()), "chunks", String.valueOf(trap.chunks().size())));
    }

    private void delete(CommandSender sender, String token) {
        Trap trap = plugin.traps().resolve(token);
        if (trap == null) { plugin.messages().send(sender, "unknown-trap", Map.of("trap", String.valueOf(token))); return; }
        int id = trap.id();
        plugin.trapService().release(trap);
        plugin.traps().remove(trap);
        new java.io.File(plugin.getDataFolder(), "baseline/" + id + ".bin").delete();
        plugin.messages().send(sender, "admin.deleted", Map.of("id", String.valueOf(id)));
    }

    private void setPrice(CommandSender sender, String token, String value) {
        Trap trap = plugin.traps().resolve(token);
        if (trap == null || value == null) { plugin.messages().send(sender, "unknown-trap", Map.of("trap", String.valueOf(token))); return; }
        try { trap.salePrice(Double.parseDouble(value)); } catch (NumberFormatException ex) { plugin.messages().send(sender, "sell.bad-price"); return; }
        trap.forSale(true);
        plugin.messages().send(sender, "admin.set-price", Map.of("trap", trap.name(), "price", plugin.trapService().priceText(trap.salePrice())));
    }

    private void setHealth(CommandSender sender, String token, String value) {
        Trap trap = plugin.traps().resolve(token);
        if (trap == null || value == null) { plugin.messages().send(sender, "unknown-trap", Map.of("trap", String.valueOf(token))); return; }
        try { trap.health(Integer.parseInt(value)); } catch (NumberFormatException ex) { plugin.messages().send(sender, "sell.bad-price"); return; }
        plugin.holograms().refresh(trap);
        plugin.messages().send(sender, "admin.set-health", Map.of("trap", trap.name(), "health", String.valueOf(trap.health())));
    }

    private void setOwner(CommandSender sender, String token, String name) {
        Trap trap = plugin.traps().resolve(token);
        Player target = name == null ? null : Bukkit.getPlayerExact(name);
        if (trap == null) { plugin.messages().send(sender, "unknown-trap", Map.of("trap", String.valueOf(token))); return; }
        if (target == null) { plugin.messages().send(sender, "unknown-player"); return; }
        plugin.trapService().giveOwnership(trap, target);
        plugin.messages().send(sender, "admin.set-owner", Map.of("trap", trap.name(), "player", target.getName()));
    }

    private void reload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.messages().load();
        plugin.messages().send(sender, "admin.reloaded");
    }

    private void info(CommandSender sender, String token) {
        Trap trap = plugin.traps().resolve(token);
        if (trap == null) { plugin.messages().send(sender, "unknown-trap", Map.of("trap", String.valueOf(token))); return; }
        sender.sendMessage("§e#" + trap.id() + " " + trap.name() + " §7sahip=" + trap.ownerName()
                + " can=" + trap.health() + "/" + trap.maxHealth() + " chunk=" + trap.chunks().size()
                + " uye=" + trap.members().size() + " durum=" + trap.status());
    }

    private void tp(CommandSender sender, String token) {
        if (!(sender instanceof Player player)) { plugin.messages().send(sender, "players-only"); return; }
        Trap trap = plugin.traps().resolve(token);
        if (trap == null) { plugin.messages().send(player, "unknown-trap", Map.of("trap", String.valueOf(token))); return; }
        org.bukkit.Location loc = trap.home();
        if (loc == null) {
            ChunkKey first = trap.chunks().isEmpty() ? null : trap.chunks().iterator().next();
            org.bukkit.World world = first == null ? null : plugin.getServer().getWorld(first.world());
            if (world != null) loc = world.getHighestBlockAt((first.x() << 4) + 8, (first.z() << 4) + 8).getLocation().add(0.5, 1, 0.5);
        }
        if (loc == null || loc.getWorld() == null) { plugin.messages().send(player, "home.missing"); return; }
        player.teleport(loc);
        plugin.messages().send(player, "admin.tp", Map.of("trap", trap.name()));
    }

    private void tnt(CommandSender sender, String amount) {
        if (!(sender instanceof Player player)) { plugin.messages().send(sender, "players-only"); return; }
        int count = 1;
        try { if (amount != null) count = Math.max(1, Integer.parseInt(amount)); } catch (NumberFormatException ignored) { }
        player.getInventory().addItem(plugin.tnt().create(count));
        plugin.messages().send(player, "admin.tnt", Map.of("amount", String.valueOf(count)));
    }

    private void reset(CommandSender sender, String token) {
        Trap trap = plugin.traps().resolve(token);
        if (trap == null) { plugin.messages().send(sender, "unknown-trap", Map.of("trap", String.valueOf(token))); return; }
        if (!plugin.baseline().restore(trap)) { plugin.messages().send(sender, "admin.no-baseline"); return; }
        plugin.messages().send(sender, "admin.reset", Map.of("trap", trap.name()));
    }

    private void recapture(CommandSender sender, String token) {
        Trap trap = plugin.traps().resolve(token);
        if (trap == null) { plugin.messages().send(sender, "unknown-trap", Map.of("trap", String.valueOf(token))); return; }
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.baseline().capture(trap));
        plugin.messages().send(sender, "admin.recapture", Map.of("trap", trap.name()));
    }

    private void list(CommandSender sender) {
        plugin.messages().send(sender, "list.header", Map.of("count", String.valueOf(plugin.traps().all().size())));
        for (Trap trap : plugin.traps().all()) {
            sender.sendMessage("§7#" + trap.id() + " §f" + trap.name() + " §7" + trap.ownerName() + " §8" + trap.chunks().size() + "c");
        }
    }

    private static String arg(String[] args, int index) {
        return args.length > index ? args[index] : null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(List.of("wand", "create", "delete", "setprice", "sethealth", "setowner", "reload", "save", "info", "tp", "tnt", "reset", "recapture", "list"), args[0]);
        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        List<String> out = new ArrayList<>();
        for (String option : options) if (option.startsWith(prefix.toLowerCase(Locale.ROOT))) out.add(option);
        return out;
    }
}

