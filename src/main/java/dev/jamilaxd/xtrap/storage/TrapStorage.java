package dev.jamilaxd.xtrap.storage;

import dev.jamilaxd.xtrap.XTrapPlugin;
import dev.jamilaxd.xtrap.model.ChunkKey;
import dev.jamilaxd.xtrap.model.Rank;
import dev.jamilaxd.xtrap.model.Relation;
import dev.jamilaxd.xtrap.model.Trap;
import dev.jamilaxd.xtrap.model.TrapMember;
import dev.jamilaxd.xtrap.model.TrapStatus;
import dev.jamilaxd.xtrap.util.Locs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class TrapStorage {

    private final XTrapPlugin plugin;
    private final File file;

    public TrapStorage(XTrapPlugin plugin) {
        this.plugin = plugin;
        File dir = new File(plugin.getDataFolder(), "data");
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().warning("data klasoru olusturulamadi");
        }
        this.file = new File(dir, "traps.yml");
    }

    public void load(TrapRegistry registry) {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("traps");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec == null) continue;
            Trap trap = read(sec, key);
            if (trap != null) registry.addLoaded(trap);
        }
    }

    private Trap read(ConfigurationSection sec, String key) {
        int id = sec.getInt("id", Integer.parseInt(key));
        Trap trap = new Trap(id);
        trap.name(sec.getString("name", String.valueOf(id)));
        trap.world(sec.getString("world", ""));
        for (String chunk : sec.getStringList("chunks")) {
            try {
                trap.chunks().add(ChunkKey.parse(chunk));
            } catch (RuntimeException ignored) {
                plugin.getLogger().warning("Bozuk chunk: " + chunk);
            }
        }
        String owner = sec.getString("owner", "");
        if (!owner.isBlank()) {
            trap.setOwner(UUID.fromString(owner), sec.getString("owner-name", "bilinmiyor"));
        }
        trap.health(sec.getInt("health"));
        trap.maxHealth(sec.getInt("max-health"));
        trap.memberLimit(sec.getInt("member-limit"));
        trap.vaultRows(sec.getInt("vault-rows", 1));
        trap.salePrice(sec.getDouble("sale-price"));
        trap.forSale(sec.getBoolean("for-sale"));
        try {
            trap.status(TrapStatus.valueOf(sec.getString("status", "EMPTY")));
        } catch (IllegalArgumentException ex) {
            trap.status(TrapStatus.EMPTY);
        }
        trap.collapsedAt(sec.getLong("collapsed-at"));
        trap.kills(sec.getInt("kills"));
        trap.deaths(sec.getInt("deaths"));
        trap.home(Locs.read(sec.getString("home", "")));
        trap.hologram(Locs.read(sec.getString("hologram", "")));
        readMembers(sec.getConfigurationSection("members"), trap);
        readRelations(sec.getConfigurationSection("relations"), trap);
        ConfigurationSection vault = sec.getConfigurationSection("vault");
        if (vault != null) {
            for (String slot : vault.getKeys(false)) {
                trap.vault().put(Integer.parseInt(slot), vault.getString(slot, ""));
            }
        }
        return trap;
    }

    private void readMembers(ConfigurationSection members, Trap trap) {
        if (members == null) return;
        for (String idKey : members.getKeys(false)) {
            ConfigurationSection m = members.getConfigurationSection(idKey);
            if (m == null) continue;
            UUID uuid = UUID.fromString(idKey);
            Rank rank = Rank.parse(m.getString("rank", "uye"));
            if (rank == null) rank = Rank.UYE;
            TrapMember member = new TrapMember(uuid, m.getString("name", "?"), rank);
            member.kills(m.getInt("kills"));
            member.deaths(m.getInt("deaths"));
            trap.members().put(uuid, member);
        }
        if (trap.owner() != null && !trap.members().containsKey(trap.owner())) {
            trap.members().put(trap.owner(), new TrapMember(trap.owner(), trap.ownerName(), Rank.LIDER));
        }
    }

    private void readRelations(ConfigurationSection rel, Trap trap) {
        if (rel == null) return;
        for (String other : rel.getKeys(false)) {
            Relation relation = Relation.parse(rel.getString(other));
            if (relation != null) trap.relationTo(Integer.parseInt(other), relation);
        }
    }

    public void save(TrapRegistry registry) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Trap trap : registry.all()) write(yaml, trap);
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Trap kaydi yazilamadi", ex);
        }
    }

    private void write(YamlConfiguration yaml, Trap trap) {
        String path = "traps." + trap.id();
        yaml.set(path + ".id", trap.id());
        yaml.set(path + ".name", trap.name());
        yaml.set(path + ".world", trap.world());
        List<String> chunks = new ArrayList<>();
        trap.chunks().forEach(c -> chunks.add(c.serialize()));
        yaml.set(path + ".chunks", chunks);
        yaml.set(path + ".owner", trap.owner() == null ? "" : trap.owner().toString());
        yaml.set(path + ".owner-name", trap.owner() == null ? "" : trap.ownerName());
        yaml.set(path + ".health", trap.health());
        yaml.set(path + ".max-health", trap.maxHealth());
        yaml.set(path + ".member-limit", trap.memberLimit());
        yaml.set(path + ".vault-rows", trap.vaultRows());
        yaml.set(path + ".sale-price", trap.salePrice());
        yaml.set(path + ".for-sale", trap.forSale());
        yaml.set(path + ".status", trap.status().name());
        yaml.set(path + ".collapsed-at", trap.collapsedAt());
        yaml.set(path + ".kills", trap.kills());
        yaml.set(path + ".deaths", trap.deaths());
        yaml.set(path + ".home", Locs.write(trap.home()));
        yaml.set(path + ".hologram", Locs.write(trap.hologram()));
        for (TrapMember member : trap.memberList()) {
            String mp = path + ".members." + member.uuid();
            yaml.set(mp + ".name", member.name());
            yaml.set(mp + ".rank", member.rank().name());
            yaml.set(mp + ".kills", member.kills());
            yaml.set(mp + ".deaths", member.deaths());
        }
        trap.relations().forEach((id, relation) -> yaml.set(path + ".relations." + id, relation.name()));
        trap.vault().forEach((slot, data) -> yaml.set(path + ".vault." + slot, data));
    }
}

