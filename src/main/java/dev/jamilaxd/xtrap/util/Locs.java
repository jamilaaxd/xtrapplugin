package dev.jamilaxd.xtrap.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class Locs {

    private Locs() {
    }

    public static String write(Location loc) {
        if (loc == null || loc.getWorld() == null) return "";
        return loc.getWorld().getName() + ";"
                + loc.getX() + ";" + loc.getY() + ";" + loc.getZ() + ";"
                + loc.getYaw() + ";" + loc.getPitch();
    }

    public static Location read(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String[] p = raw.split(";");
        if (p.length < 4) return null;
        World world = Bukkit.getWorld(p[0]);
        if (world == null) return null;
        double x = Double.parseDouble(p[1]);
        double y = Double.parseDouble(p[2]);
        double z = Double.parseDouble(p[3]);
        float yaw = p.length > 4 ? Float.parseFloat(p[4]) : 0f;
        float pitch = p.length > 5 ? Float.parseFloat(p[5]) : 0f;
        return new Location(world, x, y, z, yaw, pitch);
    }
}
