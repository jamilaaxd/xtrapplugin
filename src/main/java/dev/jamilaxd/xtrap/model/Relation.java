package dev.jamilaxd.xtrap.model;

public enum Relation {
    NEUTRAL,
    ALLY,
    ENEMY;

    public static Relation parse(String raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw.toLowerCase()) {
            case "ally", "dost" -> ALLY;
            case "enemy", "dusman", "düşman" -> ENEMY;
            case "neutral", "yok", "kaldir" -> NEUTRAL;
            default -> null;
        };
    }
}
