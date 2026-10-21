package dev.jamilaxd.xtrap.storage;

import dev.jamilaxd.xtrap.model.ChunkKey;
import dev.jamilaxd.xtrap.model.Trap;
import org.bukkit.Location;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class TrapRegistry {

    private final Map<Integer, Trap> byId = new HashMap<>();
    private final Map<String, Trap> byName = new HashMap<>();
    private final Map<ChunkKey, Trap> byChunk = new HashMap<>();

    public void addLoaded(Trap trap) {
        byId.put(trap.id(), trap);
        index(trap);
    }

    public Trap create(int id) {
        Trap trap = new Trap(id);
        byId.put(id, trap);
        byName.put(normalize(trap.name()), trap);
        return trap;
    }

    public void index(Trap trap) {
        byName.put(normalize(trap.name()), trap);
        for (ChunkKey chunk : trap.chunks()) {
            byChunk.put(chunk, trap);
        }
    }

    public void reindexName(Trap trap, String oldName) {
        byName.remove(normalize(oldName));
        byName.put(normalize(trap.name()), trap);
    }

    public void reindexChunks(Trap trap) {
        byChunk.entrySet().removeIf(e -> e.getValue().id() == trap.id());
        for (ChunkKey chunk : trap.chunks()) {
            byChunk.put(chunk, trap);
        }
    }

    public void remove(Trap trap) {
        byId.remove(trap.id());
        byName.remove(normalize(trap.name()));
        byChunk.entrySet().removeIf(e -> e.getValue().id() == trap.id());
    }

    public Trap byId(int id) {
        return byId.get(id);
    }

    public Trap byName(String name) {
        if (name == null) return null;
        return byName.get(normalize(name));
    }

    public Trap byChunk(ChunkKey key) {
        return byChunk.get(key);
    }

    public Trap at(Location location) {
        if (location == null || location.getWorld() == null) return null;
        return byChunk.get(new ChunkKey(location.getWorld().getName(), location.getBlockX() >> 4, location.getBlockZ() >> 4));
    }

    public Trap resolve(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            Trap byNumber = byId.get(Integer.parseInt(token));
            if (byNumber != null) return byNumber;
        } catch (NumberFormatException ignored) {
        }
        return byName(token);
    }

    public Collection<Trap> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public boolean nameTaken(String name, int exceptId) {
        Trap trap = byName(name);
        return trap != null && trap.id() != exceptId;
    }

    public int nextId() {
        int id = 1;
        while (byId.containsKey(id)) id++;
        return id;
    }

    public java.util.List<Trap> of(UUID uuid) {
        java.util.List<Trap> list = new java.util.ArrayList<>();
        for (Trap trap : byId.values()) {
            if (trap.isMember(uuid)) list.add(trap);
        }
        list.sort(java.util.Comparator.comparingInt(Trap::id));
        return list;
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
