package smartfloor;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.BeansException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import smartfloor.repository.jpa.PostgresqlContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("IntegrationTest")
public class IntegrationTestBase extends TenantTestBase implements ApplicationContextAware {
    private static Flyway f;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        f = applicationContext.getBean(Flyway.class);
    }

    @Container
    public static PostgreSQLContainer<PostgresqlContainer> postgreSQLContainer = PostgresqlContainer.getInstance();
    @LocalServerPort
    private int port;
    private TestRestTemplate restTemplate = new TestRestTemplate();

    /**
     * Re-run all migrations before each integration test suite we have.
     */
    @BeforeAll
    public static void cleanUpDatabase() {
        if (f != null) {
            f.clean();
            f.migrate();
        }
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public TestRestTemplate getRestTemplate() {
        return restTemplate;
    }

    public void setRestTemplate(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
}
