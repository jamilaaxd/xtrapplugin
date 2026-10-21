package dev.jamilaxd.xtrap.model;

import java.util.UUID;

public class TrapMember {

    private final UUID uuid;
    private String name;
    private Rank rank;
    private int kills;
    private int deaths;

    public TrapMember(UUID uuid, String name, Rank rank) {
        this.uuid = uuid;
        this.name = name;
        this.rank = rank;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void name(String name) {
        this.name = name;
    }

    public Rank rank() {
        return rank;
    }

    public void rank(Rank rank) {
        this.rank = rank;
    }

    public int kills() {
        return kills;
    }

    public void addKill() {
        kills++;
    }

    public void kills(int kills) {
        this.kills = kills;
    }

    public int deaths() {
        return deaths;
    }

    public void addDeath() {
        deaths++;
    }

    public void deaths(int deaths) {
        this.deaths = deaths;
    }
}
