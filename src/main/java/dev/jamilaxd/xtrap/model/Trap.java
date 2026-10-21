package dev.jamilaxd.xtrap.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Trap {

    private final int id;
    private String name;
    private String world;
    private final Set<ChunkKey> chunks = new HashSet<>();
    private UUID owner;
    private String ownerName;
    private final Map<UUID, TrapMember> members = new HashMap<>();
    private int health;
    private int maxHealth;
    private int memberLimit;
    private int vaultRows;
    private double salePrice;
    private boolean forSale;
    private TrapStatus status = TrapStatus.EMPTY;
    private long collapsedAt;
    private Location home;
    private Location hologram;
    private int kills;
    private int deaths;
    private final Map<Integer, String> vault = new HashMap<>();
    private final Map<Integer, Relation> relations = new HashMap<>();

    public Trap(int id) {
        this.id = id;
        this.name = String.valueOf(id);
    }

    public int id() { return id; }
    public String name() { return name; }
    public void name(String name) { this.name = name; }
    public String world() { return world; }
    public void world(String world) { this.world = world; }
    public Set<ChunkKey> chunks() { return chunks; }
    public UUID owner() { return owner; }
    public String ownerName() { return ownerName == null ? "-" : ownerName; }
    public void ownerName(String ownerName) { this.ownerName = ownerName; }
    public Map<UUID, TrapMember> members() { return members; }
    public int health() { return health; }
    public void health(int health) { this.health = Math.max(0, health); }
    public int maxHealth() { return maxHealth; }
    public void maxHealth(int maxHealth) { this.maxHealth = maxHealth; }
    public int memberLimit() { return memberLimit; }
    public void memberLimit(int memberLimit) { this.memberLimit = memberLimit; }
    public int vaultRows() { return vaultRows; }
    public void vaultRows(int vaultRows) { this.vaultRows = vaultRows; }
    public double salePrice() { return salePrice; }
    public void salePrice(double salePrice) { this.salePrice = salePrice; }
    public boolean forSale() { return forSale; }
    public void forSale(boolean forSale) { this.forSale = forSale; }
    public TrapStatus status() { return status; }
    public void status(TrapStatus status) { this.status = status; }
    public long collapsedAt() { return collapsedAt; }
    public void collapsedAt(long collapsedAt) { this.collapsedAt = collapsedAt; }
    public int kills() { return kills; }
    public void kills(int kills) { this.kills = kills; }
    public void addKill() { kills++; }
    public int deaths() { return deaths; }
    public void deaths(int deaths) { this.deaths = deaths; }
    public void addDeath() { deaths++; }
    public Map<Integer, String> vault() { return vault; }
    public Map<Integer, Relation> relations() { return relations; }

    public Location home() { return home == null ? null : home.clone(); }
    public void home(Location home) { this.home = home == null ? null : home.clone(); }
    public Location hologram() { return hologram == null ? null : hologram.clone(); }
    public void hologram(Location hologram) { this.hologram = hologram == null ? null : hologram.clone(); }

    public boolean isOwned() {
        return owner != null && (status == TrapStatus.OWNED || status == TrapStatus.COLLAPSED);
    }

    public boolean isRaidable() {
        return status == TrapStatus.COLLAPSED;
    }

    public TrapMember member(UUID uuid) {
        return members.get(uuid);
    }

    public Rank rankOf(UUID uuid) {
        if (uuid == null) return null;
        if (uuid.equals(owner)) return Rank.LIDER;
        TrapMember member = members.get(uuid);
        return member == null ? null : member.rank();
    }

    public boolean isMember(UUID uuid) {
        return rankOf(uuid) != null;
    }

    public void setOwner(UUID uuid, String name) {
        this.owner = uuid;
        this.ownerName = name;
        if (uuid != null) {
            TrapMember existing = members.get(uuid);
            if (existing == null) {
                members.put(uuid, new TrapMember(uuid, name, Rank.LIDER));
            } else {
                existing.rank(Rank.LIDER);
                existing.name(name);
            }
            this.status = TrapStatus.OWNED;
            this.forSale = false;
        }
    }

    public void clearOwnership(boolean keepVault) {
        owner = null;
        ownerName = null;
        members.clear();
        home = null;
        forSale = true;
        status = TrapStatus.FOR_SALE;
        collapsedAt = 0;
        relations.clear();
        kills = 0;
        deaths = 0;
        if (!keepVault) vault.clear();
    }

    public Collection<TrapMember> memberList() {
        return Collections.unmodifiableCollection(members.values());
    }

    public String memberNames() {
        if (members.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (TrapMember member : members.values()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(member.name());
        }
        return sb.toString();
    }

    public int onlineCount() {
        int n = 0;
        for (UUID uuid : members.keySet()) {
            if (Bukkit.getPlayer(uuid) != null) n++;
        }
        return n;
    }

    public Relation relationTo(int otherId) {
        return relations.getOrDefault(otherId, Relation.NEUTRAL);
    }

    public void relationTo(int otherId, Relation relation) {
        if (relation == null || relation == Relation.NEUTRAL) relations.remove(otherId);
        else relations.put(otherId, relation);
    }
}
