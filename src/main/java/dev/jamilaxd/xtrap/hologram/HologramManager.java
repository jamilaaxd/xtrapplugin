package dev.jamilaxd.xtrap.hologram;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import dev.jamilaxd.xtrap.XTrapPlugin;
import dev.jamilaxd.xtrap.model.Trap;
import dev.jamilaxd.xtrap.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public final class HologramManager {

    private static final String NAME_PREFIX = "xtrap-";

    private final XTrapPlugin plugin;
    private final boolean fancy;

    public HologramManager(XTrapPlugin plugin) {
        this.plugin = plugin;
        this.fancy = detect();
        plugin.getLogger().info(fancy
                ? "Hologramlar FancyHolograms ile gosterilecek."
                : "FancyHolograms yok, hologramlar TextDisplay olarak gosterilecek.");
    }

    public boolean usingFancy() {
        return fancy;
    }

    public void place(Trap trap, Location base) {
        remove(trap);
        List<String> lines = plugin.getConfig().getStringList("hologram.lines");
        if (lines.isEmpty() || base.getWorld() == null) {
            return;
        }
        Location loc = base.clone().add(0.5, 1.6 + lines.size() * 0.28, 0.5);
        if (fancy) {
            spawnFancy(trap, loc, lines);
        } else {
            spawnDisplay(trap, loc, lines);
        }
        trap.hologram(loc);
    }

    public void refresh(Trap trap) {
        Location loc = trap.hologram();
        if (loc == null || loc.getWorld() == null) {
            return;
        }
        List<String> lines = plugin.getConfig().getStringList("hologram.lines");
        if (fancy) {
            refreshFancy(trap, loc, lines);
            return;
        }
        TextDisplay display = find(trap);
        if (display == null) {
            spawnDisplay(trap, loc, lines);
            return;
        }
        display.text(Text.comp(String.join("\n", fill(trap, lines))));
    }

    public void refreshAll() {
        for (Trap trap : plugin.traps().all()) {
            refresh(trap);
        }
    }

    public void remove(Trap trap) {
        if (fancy) {
            removeFancy(trap);
        }
        TextDisplay display = find(trap);
        if (display != null) {
            display.remove();
        }
        trap.hologram(null);
    }

    public void shutdown() {
        if (!fancy) {
            return;
        }
        for (Trap trap : plugin.traps().all()) {
            removeFancy(trap);
        }
    }

    private void spawnDisplay(Trap trap, Location loc, List<String> lines) {
        loc.getWorld().spawn(loc, TextDisplay.class, entity -> {
            entity.text(Text.comp(String.join("\n", fill(trap, lines))));
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setSeeThrough(false);
            entity.setShadowed(true);
            entity.setPersistent(true);
            entity.addScoreboardTag(NAME_PREFIX + trap.id());
        });
    }

    private void spawnFancy(Trap trap, Location loc, List<String> lines) {
        de.oliver.fancyholograms.api.HologramManager manager = manager();
        TextHologramData data = new TextHologramData(name(trap), loc);
        data.setPersistent(false);
        data.setBillboard(Display.Billboard.CENTER);
        data.setText(fill(trap, lines));
        data.setTextShadow(true);
        data.setSeeThrough(false);
        data.setBackground(Hologram.TRANSPARENT);
        data.setTextUpdateInterval(-1);
        Hologram hologram = manager.create(data);
        manager.addHologram(hologram);
    }

    private void refreshFancy(Trap trap, Location loc, List<String> lines) {
        de.oliver.fancyholograms.api.HologramManager manager = manager();
        Hologram hologram = manager.getHologram(name(trap)).orElse(null);
        if (hologram == null || !(hologram.getData() instanceof TextHologramData data)) {
            spawnFancy(trap, loc, lines);
            return;
        }
        data.setText(fill(trap, lines));
        hologram.forceUpdate();
        hologram.refreshForViewers();
    }

    private void removeFancy(Trap trap) {
        try {
            de.oliver.fancyholograms.api.HologramManager manager = manager();
            manager.getHologram(name(trap)).ifPresent(manager::removeHologram);
        } catch (Throwable ignored) {
        }
    }

    private de.oliver.fancyholograms.api.HologramManager manager() {
        return FancyHologramsPlugin.get().getHologramManager();
    }

    private boolean detect() {
        Plugin found = Bukkit.getPluginManager().getPlugin("FancyHolograms");
        if (found == null || !found.isEnabled()) {
            return false;
        }
        try {
            Class.forName("de.oliver.fancyholograms.api.FancyHologramsPlugin");
            Class.forName("de.oliver.fancyholograms.api.data.TextHologramData");
            return FancyHologramsPlugin.isEnabled();
        } catch (Throwable ex) {
            plugin.getLogger().warning("FancyHolograms bulundu ama API uyumsuz, TextDisplay kullanilacak: " + ex.getMessage());
            return false;
        }
    }

    private String name(Trap trap) {
        return NAME_PREFIX + trap.id();
    }

    private TextDisplay find(Trap trap) {
        Location loc = trap.hologram();
        if (loc == null || loc.getWorld() == null) {
            return null;
        }
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 1.5, 2.5, 1.5)) {
            if (entity instanceof TextDisplay display && entity.getScoreboardTags().contains(NAME_PREFIX + trap.id())) {
                return display;
            }
        }
        return null;
    }

    private List<String> fill(Trap trap, List<String> lines) {
        List<String> out = new ArrayList<>();
        for (String line : lines) {
            out.add(plugin.placeholders().apply(trap, line));
        }
        return out;
    }
}