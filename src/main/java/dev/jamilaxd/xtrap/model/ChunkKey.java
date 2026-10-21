package dev.jamilaxd.xtrap.model;

public record ChunkKey(String world, int x, int z) {

    public static ChunkKey parse(String raw) {
        String[] p = raw.split(";");
        if (p.length != 3) {
            throw new IllegalArgumentException(raw);
        }
        return new ChunkKey(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]));
    }

    public String serialize() {
        return world + ";" + x + ";" + z;
    }

    @Override
    public String toString() {
        return serialize();
    }
}
