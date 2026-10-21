package dev.jamilaxd.xtrap;

import dev.jamilaxd.xtrap.command.AdminCommand;
import dev.jamilaxd.xtrap.command.TrapCommand;
import dev.jamilaxd.xtrap.config.Messages;
import dev.jamilaxd.xtrap.gui.MenuListener;
import dev.jamilaxd.xtrap.gui.Menus;
import dev.jamilaxd.xtrap.gui.VaultMenu;
import dev.jamilaxd.xtrap.hologram.HologramManager;
import dev.jamilaxd.xtrap.hook.EconomyHook;
import dev.jamilaxd.xtrap.hook.GroupHook;
import dev.jamilaxd.xtrap.listener.CombatListener;
import dev.jamilaxd.xtrap.listener.ProtectionListener;
import dev.jamilaxd.xtrap.listener.TntListener;
import dev.jamilaxd.xtrap.listener.WandListener;
import dev.jamilaxd.xtrap.placeholder.TrapExpansion;
import dev.jamilaxd.xtrap.placeholder.TrapPlaceholders;
import dev.jamilaxd.xtrap.selection.SelectionManager;
import dev.jamilaxd.xtrap.siege.SiegeManager;
import dev.jamilaxd.xtrap.storage.BaselineStore;
import dev.jamilaxd.xtrap.storage.TrapRegistry;
import dev.jamilaxd.xtrap.storage.TrapStorage;
import dev.jamilaxd.xtrap.tnt.TntItems;
import dev.jamilaxd.xtrap.trap.TrapService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class XTrapPlugin extends JavaPlugin {

    private Messages messages;
    private TrapRegistry registry;
    private TrapStorage storage;
    private EconomyHook economy;
    private GroupHook groups;
    private SelectionManager selections;
    private TrapService trapService;
    private SiegeManager sieges;
    private TntItems tnt;
    private HologramManager holograms;
    private BaselineStore baseline;
    private Menus menus;
    private VaultMenu vaults;
    private TrapPlaceholders placeholders;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        messages = new Messages(this);
        messages.load();
        registry = new TrapRegistry();
        storage = new TrapStorage(this);
        storage.load(registry);
        economy = new EconomyHook();
        if (!economy.setup()) getLogger().warning("Vault ekonomisi yok. Satin alma kapali.");
        groups = new GroupHook(this);
        groups.setup();
        selections = new SelectionManager(this);
        trapService = new TrapService(this);
        sieges = new SiegeManager(this);
        tnt = new TntItems(this);
        holograms = new HologramManager(this);
        baseline = new BaselineStore(this);
        menus = new Menus(this);
        vaults = new VaultMenu(this);
        placeholders = new TrapPlaceholders(this);

        Bukkit.getPluginManager().registerEvents(new ProtectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TntListener(this), this);
        Bukkit.getPluginManager().registerEvents(new WandListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(this), this);

        TrapCommand trap = new TrapCommand(this);
        getCommand("trap").setExecutor(trap);
        getCommand("trap").setTabCompleter(trap);
        AdminCommand admin = new AdminCommand(this);
        getCommand("trapadmin").setExecutor(admin);
        getCommand("trapadmin").setTabCompleter(admin);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            sieges.tick();
            trapService.tickCollapses();
        }, 20L, 20L);
        long save = Math.max(60, getConfig().getInt("settings.autosave-seconds", 300)) * 20L;
        Bukkit.getScheduler().runTaskTimer(this, () -> storage.save(registry), save, save);
        Bukkit.getScheduler().runTaskLater(this, holograms::refreshAll, 40L);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new TrapExpansion(this).register();
        }
        getLogger().info("xTrap hazir. " + registry.all().size() + " trap yuklendi.");
    }

    @Override
    public void onDisable() {
        if (holograms != null) holograms.shutdown();
        if (storage != null && registry != null) storage.save(registry);
    }

    public Messages messages() { return messages; }
    public TrapRegistry traps() { return registry; }
    public TrapStorage storage() { return storage; }
    public EconomyHook economy() { return economy; }
    public GroupHook groups() { return groups; }
    public SelectionManager selections() { return selections; }
    public TrapService trapService() { return trapService; }
    public SiegeManager sieges() { return sieges; }
    public TntItems tnt() { return tnt; }
    public HologramManager holograms() { return holograms; }
    public BaselineStore baseline() { return baseline; }
    public Menus menus() { return menus; }
    public VaultMenu vaults() { return vaults; }
    public TrapPlaceholders placeholders() { return placeholders; }
}
