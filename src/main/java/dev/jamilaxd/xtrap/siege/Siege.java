package dev.jamilaxd.xtrap.siege;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Siege {

    public enum Phase { FIGHT, BREAK }

    private final Set<Integer> attackers = new LinkedHashSet<>();
    private final Set<Integer> defenders = new LinkedHashSet<>();
    private int session;
    private final int sessions;
    private Phase phase = Phase.FIGHT;
    private long phaseEndsAt;
    private int attackerPoints;
    private int defenderPoints;
    private int attackerSessions;
    private int defenderSessions;

    public Siege(Set<Integer> attackers, Set<Integer> defenders, int sessions, long fightMillis) {
        this.attackers.addAll(attackers);
        this.defenders.addAll(defenders);
        this.sessions = sessions;
        this.session = 1;
        this.phaseEndsAt = System.currentTimeMillis() + fightMillis;
    }

    public boolean involves(int trapId) {
        return attackers.contains(trapId) || defenders.contains(trapId);
    }

    public boolean attacking(int trapId) { return attackers.contains(trapId); }
    public boolean defending(int trapId) { return defenders.contains(trapId); }
    public Set<Integer> attackers() { return attackers; }
    public Set<Integer> defenders() { return defenders; }
    public int session() { return session; }
    public int sessions() { return sessions; }
    public Phase phase() { return phase; }
    public long phaseEndsAt() { return phaseEndsAt; }
    public int attackerPoints() { return attackerPoints; }
    public int defenderPoints() { return defenderPoints; }
    public int attackerSessions() { return attackerSessions; }
    public int defenderSessions() { return defenderSessions; }

    public void addPoints(boolean attacker, int amount) {
        if (phase != Phase.FIGHT || amount <= 0) return;
        if (attacker) attackerPoints += amount;
        else defenderPoints += amount;
    }

    public void beginBreak(long millis) {
        phase = Phase.BREAK;
        phaseEndsAt = System.currentTimeMillis() + millis;
    }

    public void nextSession(long fightMillis) {
        session++;
        attackerPoints = 0;
        defenderPoints = 0;
        phase = Phase.FIGHT;
        phaseEndsAt = System.currentTimeMillis() + fightMillis;
    }

    public void awardSession(boolean attackerWon) {
        if (attackerWon) attackerSessions++;
        else defenderSessions++;
    }

    public List<Integer> all() {
        List<Integer> ids = new ArrayList<>(attackers);
        ids.addAll(defenders);
        return ids;
    }
}
