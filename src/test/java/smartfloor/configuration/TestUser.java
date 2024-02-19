package smartfloor.configuration;

public enum TestUser {
    FIRST_REGULAR_USER(1L, "smartfloor-test"),
    SECOND_REGULAR_USER(2L, "smartfloor-test-second"),
    GROUP_MANAGER_USER(3L, "smartfloor-test-gm"),
    ADMIN_USER(4L, "smartfloor-test-adm");

    private final Long id;
    private final String username;

    TestUser(Long id, String username) {
        this.id = id;
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
}
