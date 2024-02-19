package smartfloor.configuration;

public enum Profiles {
    DEVELOPMENT("dev"),
    PRODUCTION("prod"),
    TEST("test");

    private final String profileValue;

    Profiles(String profileValue) {
        this.profileValue = profileValue;
    }

    @Override
    public String toString() {
        return profileValue;
    }
}
