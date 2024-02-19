package smartfloor.domain.entities.fall.risk.profile;

public enum FallRiskGroup {
    LOW(1),
    MODERATE(2),
    HIGH(3);


    public final int level;

    FallRiskGroup(final int level) {
        this.level = level;
    }

    /**
     * TODO.
     */
    public static FallRiskGroup getLevel(int level) {
        for (FallRiskGroup frp : values()) {
            if (frp.level == level) return frp;
        }
        return null;
    }
}
