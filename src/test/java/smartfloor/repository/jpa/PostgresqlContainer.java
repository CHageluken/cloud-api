package smartfloor.repository.jpa;

import java.time.Duration;
import org.testcontainers.containers.PostgreSQLContainer;


public class PostgresqlContainer extends PostgreSQLContainer<PostgresqlContainer> {
    private static final String IMAGE_VERSION = "postgres:11.1";
    private static final String APP_DB_USERNAME = "rls";
    private static final String APP_DB_PASSWORD = "test_rls";

    private static PostgresqlContainer container;

    private PostgresqlContainer() {
        super(IMAGE_VERSION);
    }

    /**
     * TODO.
     */
    public static PostgresqlContainer getInstance() {
        if (container == null) {
            container = new PostgresqlContainer();
            container.withStartupTimeout(Duration.ofMinutes(5));
            container.withExposedPorts(5432);
        }
        return container;
    }

    @Override
    public void start() {
        super.start();
        System.setProperty("POSTGRES_URL", container.getJdbcUrl());
        System.setProperty("POSTGRES_USER", container.getUsername());
        System.setProperty("POSTGRES_PASSWORD", container.getPassword());
        System.setProperty("POSTGRES_APP_USER", APP_DB_USERNAME);
        System.setProperty("POSTGRES_APP_PASSWORD", APP_DB_PASSWORD);
    }

    @Override
    public void stop() {
        //do nothing, JVM handles shut down
    }
}
