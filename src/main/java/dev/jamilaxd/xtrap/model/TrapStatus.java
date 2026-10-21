package dev.jamilaxd.xtrap.model;

public enum TrapStatus {
    EMPTY,
    FOR_SALE,
    OWNED,
    COLLAPSED;

    public String messageKey() {
        return switch (this) {
            case EMPTY -> "status.empty";
            case FOR_SALE -> "status.sale";
            case OWNED -> "status.owned";
            case COLLAPSED -> "status.collapsed";
        };
    }
}
