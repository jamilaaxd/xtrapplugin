package dev.jamilaxd.xtrap.hook;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyHook {

    private Economy economy;

    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (provider == null) return false;
        economy = provider.getProvider();
        return economy != null;
    }

    public boolean available() {
        return economy != null;
    }

    public double balance(OfflinePlayer player) {
        return economy.getBalance(player);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (amount <= 0) return true;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public void deposit(OfflinePlayer player, double amount) {
        if (amount <= 0 || player == null) return;
        economy.depositPlayer(player, amount);
    }
}
