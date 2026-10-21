package dev.jamilaxd.xtrap.gui;

import dev.jamilaxd.xtrap.XTrapPlugin;
import dev.jamilaxd.xtrap.model.Rank;
import dev.jamilaxd.xtrap.model.Trap;
import dev.jamilaxd.xtrap.model.TrapMember;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;

public class MenuListener implements Listener {

    private final XTrapPlugin plugin;

    public MenuListener(XTrapPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof Menus.BuyHolder || holder instanceof Menus.ListHolder
                || holder instanceof Menus.ManageHolder || holder instanceof Menus.RankHolder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;
        String action = event.getCurrentItem().getItemMeta().getPersistentDataContainer()
                .get(plugin.menus().action(), PersistentDataType.STRING);
        if (action == null) return;
        if (holder instanceof Menus.BuyHolder buy) handleBuy(player, buy, action);
        else if (holder instanceof Menus.ListHolder) handleList(player, action);
        else if (holder instanceof Menus.ManageHolder manage) handleManage(player, manage, action);
        else if (holder instanceof Menus.RankHolder rank) handleRank(player, rank, action);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) plugin.vaults().close(player, event.getInventory());
    }

    private void handleBuy(Player player, Menus.BuyHolder buy, String action) {
        player.closeInventory();
        if (!"buy".equals(action)) {
            plugin.messages().send(player, "buy.cancelled");
            return;
        }
        Trap trap = plugin.traps().byId(buy.trapId());
        if (trap == null || !trap.forSale() || trap.owner() != null) {
            plugin.messages().send(player, "buy.not-for-sale");
            return;
        }
        int cap = plugin.getConfig().getInt("settings.max-owned-traps", 3);
        if (cap > 0 && plugin.trapService().ownedCount(player.getUniqueId()) >= cap) {
            plugin.messages().send(player, "buy.limit");
            return;
        }
        if (!plugin.trapService().charge(player, trap.salePrice())) {
            plugin.messages().send(player, plugin.economy().available() ? "not-enough-money" : "economy-missing",
                    Map.of("price", plugin.trapService().priceText(trap.salePrice())));
            return;
        }
        if (trap.owner() != null) plugin.trapService().pay(plugin.getServer().getOfflinePlayer(trap.owner()), trap.salePrice());
        plugin.trapService().giveOwnership(trap, player);
        plugin.messages().send(player, "buy.bought", Map.of("trap", trap.name(), "price", plugin.trapService().priceText(trap.salePrice())));
    }

    private void handleList(Player player, String action) {
        if (!action.startsWith("open:")) return;
        Trap trap = plugin.traps().byId(Integer.parseInt(action.substring(5)));
        if (trap != null) plugin.menus().manage(player, trap);
    }

    private void handleManage(Player player, Menus.ManageHolder manage, String action) {
        if (!action.startsWith("member:")) return;
        Trap trap = plugin.traps().byId(manage.trapId());
        if (trap == null || !canManage(player, trap)) {
            plugin.messages().send(player, "not-your-trap");
            return;
        }
        TrapMember member = trap.member(UUID.fromString(action.substring(7)));
        if (member != null) plugin.menus().ranks(player, trap, member);
    }

    private void handleRank(Player player, Menus.RankHolder holder, String action) {
        Trap trap = plugin.traps().byId(holder.trapId());
        TrapMember member = trap == null ? null : trap.member(holder.target());
        if (trap == null || member == null || !canManage(player, trap)) return;
        if ("kick".equals(action)) {
            if (member.uuid().equals(trap.owner())) {
                plugin.messages().send(player, "manage.cannot");
                return;
            }
            trap.members().remove(member.uuid());
            Player online = plugin.getServer().getPlayer(member.uuid());
            if (online != null) plugin.messages().send(online, "manage.kicked", Map.of("trap", trap.name()));
            plugin.holograms().refresh(trap);
            plugin.menus().manage(player, trap);
            return;
        }
        if (!action.startsWith("rank:")) return;
        Rank rank = Rank.valueOf(action.substring(5));
        if (rank == Rank.LIDER) {
            if (!player.getUniqueId().equals(trap.owner())) {
                plugin.messages().send(player, "manage.cannot");
                return;
            }
            trap.members().get(player.getUniqueId()).rank(Rank.MODERATOR);
            trap.setOwner(member.uuid(), member.name());
        } else {
            if (!player.getUniqueId().equals(trap.owner()) && member.rank().atLeast(trap.rankOf(player.getUniqueId()))) {
                plugin.messages().send(player, "manage.cannot");
                return;
            }
            member.rank(rank);
        }
        Player online = plugin.getServer().getPlayer(member.uuid());
        if (online != null) plugin.messages().send(online, "manage.rank-changed", Map.of("rank", member.rank().display(), "trap", trap.name()));
        plugin.menus().manage(player, trap);
    }

    private boolean canManage(Player player, Trap trap) {
        Rank rank = trap.rankOf(player.getUniqueId());
        return player.hasPermission("xtrap.admin") || (rank != null && rank.canManage());
    }
}
