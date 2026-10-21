package dev.jamilaxd.xtrap.model;

public enum Rank {
    UYE(1, "Uye"),
    MIMAR(2, "Mimar"),
    MODERATOR(3, "Moderator"),
    LIDER(4, "Lider");

    private final int power;
    private final String display;

    Rank(int power, String display) {
        this.power = power;
        this.display = display;
    }

    public int power() {
        return power;
    }

    public String display() {
        return display;
    }

    public boolean atLeast(Rank other) {
        return power >= other.power;
    }

    public boolean canBuild() {
        return atLeast(MIMAR);
    }

    public boolean canInteract() {
        return atLeast(UYE);
    }

    public boolean canManage() {
        return atLeast(MODERATOR);
    }

    public boolean canSetHome() {
        return atLeast(MODERATOR);
    }

    public boolean canUpgrade() {
        return atLeast(MODERATOR);
    }

    public boolean canVault() {
        return atLeast(MODERATOR);
    }

    public boolean canDisband() {
        return this == LIDER;
    }

    public boolean canTransfer() {
        return this == LIDER;
    }

    public static Rank parse(String raw) {
        if (raw == null) {
            return null;
        }
        String n = raw.trim().toLowerCase()
                .replace('ü', 'u').replace('ö', 'o').replace('ı', 'i')
                .replace('ş', 's').replace('ğ', 'g').replace('ç', 'c');
        return switch (n) {
            case "uye", "üye", "member" -> UYE;
            case "mimar", "architect" -> MIMAR;
            case "moderator", "mod", "moderatör" -> MODERATOR;
            case "lider", "leader", "owner" -> LIDER;
            default -> null;
        };
    }
}
