package smartfloor.domain;

public enum UserType {
    COMPOSITE_USER("COMPOSITE_USER"),
    DIRECT_USER("DIRECT_USER");

    private final String name;

    UserType(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
