package dev.jamilaxd.xtrap.command;

import dev.jamilaxd.xtrap.XTrapPlugin;
import dev.jamilaxd.xtrap.model.Rank;
import dev.jamilaxd.xtrap.model.Relation;
import dev.jamilaxd.xtrap.model.Trap;
import dev.jamilaxd.xtrap.siege.Siege;
import dev.jamilaxd.xtrap.trap.TrapService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TrapCommand implements CommandExecutor, TabCompleter {

    private final XTrapPlugin plugin;

    public TrapCommand(XTrapPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("yardim") || args[0].equalsIgnoreCase("help")) {
            plugin.messages().sendList(sender, "help");
            return true;
        }
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "players-only");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "al", "buy" -> buy(player);
            case "liste", "list" -> list(player);
            case "traplerim", "my" -> plugin.menus().mine(player);
            case "info", "bilgi" -> info(player, arg(args, 1));
            case "rename", "isimdegistir", "isim" -> rename(player, args);
            case "invite", "davet" -> invite(player, args);
            case "deny", "reddet" -> deny(player, arg(args, 1));
            case "leave", "ayril" -> leave(player, arg(args, 1));
            case "yonet", "yönet", "manage" -> manage(player, arg(args, 1));
            case "sethome" -> setHome(player);
            case "home", "ev" -> home(player, arg(args, 1));
            case "sell", "sat" -> sell(player, arg(args, 1));
            case "enemy", "dusman", "düşman" -> relation(player, arg(args, 1), Relation.ENEMY);
            case "ally", "dost" -> relation(player, arg(args, 1), Relation.ALLY);
            case "upgrade", "yukselt" -> upgrade(player, arg(args, 1));
            case "pv", "depo" -> vault(player, arg(args, 1));
            case "siege", "savas", "savaş" -> siege(player, arg(args, 1));
            case "reclaim", "kurtar" -> reclaim(player);
            case "disband", "kapat" -> disband(player, arg(args, 1));
            case "sifirla", "sıfırla", "reset" -> reset(player, arg(args, 1));
            default -> plugin.messages().sendList(sender, "help");
        }
        return true;
    }

    private void buy(Player player) {
        Trap trap = plugin.traps().at(player.getLocation());
        if (trap == null) { plugin.messages().send(player, "not-in-trap"); return; }
        if (trap.owner() != null || !trap.forSale()) { plugin.messages().send(player, "buy.not-for-sale"); return; }
        plugin.menus().buy(player, trap);
    }

    private void list(Player player) {
        List<Trap> sales = plugin.traps().all().stream().filter(Trap::forSale).toList();
        if (sales.isEmpty()) { plugin.messages().send(player, "list.empty"); return; }
        plugin.messages().send(player, "list.header", Map.of("count", String.valueOf(sales.size())));
        for (Trap trap : sales) {
            player.sendMessage(dev.jamilaxd.xtrap.util.Text.comp(plugin.messages().format("list.line", Map.of(
                    "id", String.valueOf(trap.id()), "name", trap.name(),
                    "price", plugin.trapService().priceText(trap.salePrice()),
                    "chunks", String.valueOf(trap.chunks().size())))));
        }
    }

    private void info(Player player, String token) {
        Trap trap = token == null ? plugin.traps().at(player.getLocation()) : plugin.traps().resolve(token);
        if (trap == null) { plugin.messages().send(player, token == null ? "not-in-trap" : "unknown-trap", Map.of("trap", String.valueOf(token))); return; }
        plugin.messages().send(player, "info.header", Map.of("name", trap.name(), "id", String.valueOf(trap.id())));
        plugin.messages().send(player, "info.owner", Map.of("owner", trap.ownerName()));
        plugin.messages().send(player, "info.health", Map.of("health", String.valueOf(trap.health()), "max", String.valueOf(trap.maxHealth())));
        plugin.messages().send(player, "info.members", Map.of("count", String.valueOf(trap.members().size()), "list", trap.memberNames()));
        String status = plugin.sieges().of(trap) != null ? plugin.messages().raw("status.siege") : plugin.messages().raw(trap.status().messageKey());
        plugin.messages().send(player, "info.status", Map.of("status", status));
        plugin.messages().send(player, "info.kills", Map.of("kills", String.valueOf(trap.kills()), "deaths", String.valueOf(trap.deaths())));
        plugin.messages().send(player, "info.price", Map.of("price", plugin.trapService().priceText(trap.salePrice())));
    }

    private void rename(Player player, String[] args) {
        Trap trap = requireHere(player, Rank.LIDER);
        if (trap == null) return;
        String mode = arg(args, 1);
        if (mode == null) { plugin.messages().sendList(player, "help"); return; }
        String next = mode.equalsIgnoreCase("reset") ? String.valueOf(trap.id()) : arg(args, 2);
        if (next == null) return;
        if (!validName(next)) { plugin.messages().send(player, "rename.invalid"); return; }
        if (plugin.traps().nameTaken(next, trap.id())) { plugin.messages().send(player, "rename.taken"); return; }
        String old = trap.name();
        trap.name(next);
        plugin.traps().reindexName(trap, old);
        plugin.holograms().refresh(trap);
        plugin.messages().send(player, mode.equalsIgnoreCase("reset") ? "rename.reset" : "rename.set", Map.of("name", trap.name()));
    }

    private void invite(Player player, String[] args) {
        if (args.length >= 2 && (args[1].equalsIgnoreCase("accept") || args[1].equalsIgnoreCase("kabul"))) {
            Trap wanted = args.length >= 3 ? plugin.traps().resolve(args[2]) : null;
            TrapService.Invite invite = plugin.trapService().takeInvite(player.getUniqueId(), wanted);
            if (invite == null) { plugin.messages().send(player, "invite.none"); return; }
            Trap trap = plugin.traps().byId(invite.trapId());
            if (trap == null || !plugin.trapService().join(trap, player)) { plugin.messages().send(player, "invite.full"); return; }
            plugin.messages().send(player, "invite.accepted", Map.of("trap", trap.name()));
            plugin.trapService().notifyMembers(trap, "invite.joined-notify", Map.of("player", player.getName()));
            return;
        }
        Trap trap = requireHere(player, Rank.MODERATOR);
        Player target = args.length >= 2 ? Bukkit.getPlayerExact(args[1]) : null;
        if (trap == null) return;
        if (target == null) { plugin.messages().send(player, "unknown-player"); return; }
        if (target.equals(player)) { plugin.messages().send(player, "invite.self"); return; }
        if (trap.isMember(target.getUniqueId())) { plugin.messages().send(player, "invite.already"); return; }
        if (trap.members().size() >= trap.memberLimit()) { plugin.messages().send(player, "invite.full"); return; }
        plugin.trapService().invite(trap, target);
        plugin.messages().send(player, "invite.sent", Map.of("player", target.getName()));
        plugin.messages().send(target, "invite.received", Map.of("trap", trap.name()));
    }

    private void deny(Player player, String token) {
        Trap wanted = token == null ? null : plugin.traps().resolve(token);
        TrapService.Invite invite = plugin.trapService().takeInvite(player.getUniqueId(), wanted);
        if (invite == null) { plugin.messages().send(player, "deny.none"); return; }
        Trap trap = plugin.traps().byId(invite.trapId());
        plugin.messages().send(player, "deny.done", Map.of("trap", trap == null ? "?" : trap.name()));
    }

    private void leave(Player player, String token) {
        Trap trap = token == null ? onlyOne(player) : plugin.traps().resolve(token);
        if (trap == null || !trap.isMember(player.getUniqueId())) { plugin.messages().send(player, "leave.not-member"); return; }
        if (player.getUniqueId().equals(trap.owner())) { plugin.messages().send(player, "leave.owner"); return; }
        trap.members().remove(player.getUniqueId());
        plugin.holograms().refresh(trap);
        plugin.messages().send(player, "leave.done", Map.of("trap", trap.name()));
    }

    private void manage(Player player, String token) {
        Trap trap = token == null ? plugin.traps().at(player.getLocation()) : plugin.traps().resolve(token);
        if (trap == null) { plugin.messages().send(player, "unknown-trap", Map.of("trap", String.valueOf(token))); return; }
        Rank rank = trap.rankOf(player.getUniqueId());
        if (rank == null || !rank.canManage()) { plugin.messages().send(player, "not-your-trap"); return; }
        plugin.menus().manage(player, trap);
    }

    private void setHome(Player player) {
        Trap trap = requireHere(player, Rank.MODERATOR);
        if (trap == null) return;
        trap.home(player.getLocation());
        plugin.messages().send(player, "home.set");
    }

    private void home(Player player, String token) {
        Trap trap = token == null ? onlyOne(player) : plugin.traps().resolve(token);
        if (trap == null || !trap.isMember(player.getUniqueId())) { plugin.messages().send(player, "not-your-trap"); return; }
        Location home = trap.home();
        if (home == null || home.getWorld() == null) { plugin.messages().send(player, "home.missing"); return; }
        player.teleport(home);
        plugin.messages().send(player, "home.tp", Map.of("trap", trap.name()));
    }

    private void sell(Player player, String token) {
        Trap trap = requireHere(player, Rank.LIDER);
        if (trap == null || token == null) return;
        double price;
        try { price = Double.parseDouble(token); } catch (NumberFormatException ex) { plugin.messages().send(player, "sell.bad-price"); return; }
        if (price <= 0) { plugin.messages().send(player, "sell.bad-price"); return; }
        trap.salePrice(price);
        trap.forSale(true);
        plugin.messages().send(player, "sell.listed", Map.of("trap", trap.name(), "price", plugin.trapService().priceText(price)));
    }

    private void relation(Player player, String token, Relation relation) {
        Trap from = requireHere(player, Rank.LIDER);
        Trap to = plugin.traps().resolve(token);
        if (from == null) return;
        if (to == null) { plugin.messages().send(player, "unknown-trap", Map.of("trap", String.valueOf(token))); return; }
        if (to.id() == from.id()) { plugin.messages().send(player, "enemy.self"); return; }
        from.relationTo(to.id(), relation);
        plugin.messages().send(player, relation == Relation.ENEMY ? "enemy.set" : "enemy.ally", Map.of("trap", to.name()));
    }

    private void upgrade(Player player, String token) {
        Trap trap = requireHere(player, Rank.MODERATOR);
        if (trap == null || token == null) return;
        String kind = token.toLowerCase(Locale.ROOT);
        if (kind.equals("uye") || kind.equals("member-limit") || kind.equals("member")) upgradeMembers(player, trap);
        else if (kind.equals("can") || kind.equals("healt") || kind.equals("health")) upgradeHealth(player, trap);
        else if (kind.equals("depo") || kind.equals("vault")) upgradeVault(player, trap);
        else plugin.messages().sendList(player, "help");
    }

    private void upgradeMembers(Player player, Trap trap) {
        int max = plugin.getConfig().getInt("upgrades.member-limit.max", 20);
        if (trap.memberLimit() >= max) { plugin.messages().send(player, "upgrade.max"); return; }
        int level = trap.memberLimit() - plugin.getConfig().getInt("trap-defaults.member-limit", 5) + 1;
        double price = plugin.getConfig().getDouble("upgrades.member-limit.price-base") + plugin.getConfig().getDouble("upgrades.member-limit.price-per-level") * Math.max(0, level);
        if (!plugin.trapService().charge(player, price)) { plugin.messages().send(player, "not-enough-money", Map.of("price", plugin.trapService().priceText(price))); return; }
        trap.memberLimit(trap.memberLimit() + 1);
        plugin.messages().send(player, "upgrade.member", Map.of("value", String.valueOf(trap.memberLimit()), "price", plugin.trapService().priceText(price)));
    }

    private void upgradeHealth(Player player, Trap trap) {
        int cap = plugin.getConfig().getInt("health.max-health", 0);
        int amount = plugin.getConfig().getInt("upgrades.health.amount", 250);
        if (cap > 0 && trap.health() >= cap) { plugin.messages().send(player, "upgrade.max"); return; }
        double price = plugin.getConfig().getDouble("upgrades.health.price", 15000);
        if (!plugin.trapService().charge(player, price)) { plugin.messages().send(player, "not-enough-money", Map.of("price", plugin.trapService().priceText(price))); return; }
        plugin.trapService().heal(trap, amount);
        plugin.messages().send(player, "upgrade.health", Map.of("amount", String.valueOf(amount), "value", String.valueOf(trap.health()), "price", plugin.trapService().priceText(price)));
    }

    private void upgradeVault(Player player, Trap trap) {
        int max = plugin.getConfig().getInt("upgrades.vault.max-rows", 6);
        if (trap.vaultRows() >= max) { plugin.messages().send(player, "upgrade.max"); return; }
        int level = trap.vaultRows();
        double price = plugin.getConfig().getDouble("upgrades.vault.price-base") + plugin.getConfig().getDouble("upgrades.vault.price-per-level") * Math.max(0, level - 1);
        if (!plugin.trapService().charge(player, price)) { plugin.messages().send(player, "not-enough-money", Map.of("price", plugin.trapService().priceText(price))); return; }
        trap.vaultRows(trap.vaultRows() + 1);
        plugin.messages().send(player, "upgrade.vault", Map.of("value", String.valueOf(trap.vaultRows()), "price", plugin.trapService().priceText(price)));
    }

    private void vault(Player player, String token) {
        Trap trap = requireHere(player, Rank.MODERATOR);
        if (trap == null) return;
        int page = 1;
        try { if (token != null) page = Integer.parseInt(token); } catch (NumberFormatException ignored) { page = 1; }
        plugin.vaults().open(player, trap, page);
    }

    private void siege(Player player, String token) {
        if (!player.hasPermission("xtrap.siege")) { plugin.messages().send(player, "no-permission"); return; }
        Trap target = plugin.traps().resolve(token);
        Trap own = plugin.trapService().primaryOwned(player);
        if (own == null) { plugin.messages().send(player, "siege.no-trap"); return; }
        if (target == null || target.owner() == null) { plugin.messages().send(player, "unknown-trap", Map.of("trap", String.valueOf(token))); return; }
        if (target.id() == own.id() || player.getUniqueId().equals(target.owner())) { plugin.messages().send(player, "siege.self"); return; }
        if (plugin.sieges().of(target) != null || plugin.sieges().of(own) != null) { plugin.messages().send(player, "siege.busy"); return; }
        if ("hours".equals(plugin.sieges().denyStart())) {
            plugin.messages().send(player, "siege.hours", Map.of(
                    "start", plugin.getConfig().getString("siege.hours.start", "18:00"),
                    "end", plugin.getConfig().getString("siege.hours.end", "23:00")));
            return;
        }
        Siege siege = plugin.sieges().start(player, target);
        Map<String, String> vars = Map.of("attacker", own.name(), "defender", target.name());
        plugin.trapService().notifyMembers(own, "siege.started", vars);
        plugin.trapService().notifyMembers(target, "siege.started", vars);
        plugin.trapService().notifyMembers(target, "siege.session", Map.of("current", "1", "total", String.valueOf(siege.sessions())));
    }

    private void reclaim(Player player) {
        Trap trap = plugin.traps().at(player.getLocation());
        if (trap == null || !player.getUniqueId().equals(trap.owner())) { plugin.messages().send(player, "not-your-trap"); return; }
        if (trap.status() != dev.jamilaxd.xtrap.model.TrapStatus.COLLAPSED) { plugin.messages().send(player, "collapse.not-collapsed"); return; }
        double price = plugin.getConfig().getDouble("collapse.reclaim-price", 25000);
        if (!plugin.trapService().charge(player, price)) { plugin.messages().send(player, "not-enough-money", Map.of("price", plugin.trapService().priceText(price))); return; }
        plugin.trapService().reclaim(trap);
        plugin.messages().send(player, "collapse.reclaimed", Map.of("trap", trap.name(), "price", plugin.trapService().priceText(price)));
    }

    private void disband(Player player, String token) {
        Trap trap = token == null ? requireHere(player, Rank.LIDER) : plugin.traps().resolve(token);
        if (trap == null || !player.getUniqueId().equals(trap.owner())) { plugin.messages().send(player, "not-your-trap"); return; }
        String name = trap.name();
        plugin.trapService().release(trap);
        plugin.messages().send(player, "disband.done", Map.of("trap", name));
    }

    private void reset(Player player, String token) {
        Trap trap = token == null ? requireHere(player, Rank.LIDER) : plugin.traps().resolve(token);
        if (trap == null || !player.getUniqueId().equals(trap.owner())) { plugin.messages().send(player, "not-your-trap"); return; }
        if (!plugin.baseline().restore(trap)) { plugin.messages().send(player, "admin.no-baseline"); return; }
        plugin.messages().send(player, "admin.reset", Map.of("trap", trap.name()));
    }

    private Trap requireHere(Player player, Rank need) {
        Trap trap = plugin.traps().at(player.getLocation());
        if (trap == null) { plugin.messages().send(player, "not-in-trap"); return null; }
        Rank rank = trap.rankOf(player.getUniqueId());
        if (rank == null || !rank.atLeast(need)) { plugin.messages().send(player, "not-your-trap"); return null; }
        return trap;
    }

    private Trap onlyOne(Player player) {
        List<Trap> list = plugin.traps().of(player.getUniqueId());
        if (list.size() == 1) return list.get(0);
        Trap here = plugin.traps().at(player.getLocation());
        if (here != null && here.isMember(player.getUniqueId())) return here;
        plugin.messages().send(player, "unknown-trap", Map.of("trap", "id/isim gerekli"));
        return null;
    }

    private boolean validName(String name) {
        int min = plugin.getConfig().getInt("settings.name.min-length", 3);
        int max = plugin.getConfig().getInt("settings.name.max-length", 16);
        return name.length() >= min && name.length() <= max
                && name.matches(plugin.getConfig().getString("settings.name.pattern", "^[A-Za-z0-9_]+$"));
    }

    private static String arg(String[] args, int index) {
        return args.length > index ? args[index] : null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(List.of("al", "liste", "traplerim", "info", "isimdegistir", "davet", "deny", "leave", "yonet", "sethome", "home", "sat", "dusman", "dost", "upgrade", "pv", "siege", "reclaim", "kapat", "sifirla", "yardim"), args[0]);
        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String option : options) if (option.startsWith(p)) out.add(option);
        return out;
    }
}




