package smartfloor.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import javax.sql.DataSource;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import smartfloor.domain.UserType;
import smartfloor.multitenancy.AccessScopeContext;

public class DataSourceConfig {



    static class TenantAwareDataSource extends HikariDataSource {

        private static final Logger log = LoggerFactory.getLogger(TenantAwareDataSource.class);

        @Override
        public Connection getConnection() throws SQLException {
            Connection connection = super.getConnection();
            UserType currentUserType = AccessScopeContext.INSTANCE.getUserType();
            if (currentUserType == UserType.COMPOSITE_USER) {
                try (Statement sql = connection.createStatement()) {
                    Long compositeUserId = AccessScopeContext.INSTANCE.getCompositeUserId();
                    sql.execute(String.format("SET app.composite_user_id = '%d'", compositeUserId));
                    sql.execute("SET app.tenant_id = 0");
                } catch (PSQLException e) {
                    log.error(
                            "Unable to set app.composite_user_id parameter. Exception message: {}",
                            e.getServerErrorMessage()
                    );
                }
            } else {
                try (Statement sql = connection.createStatement()) {
                    Long tenantId = AccessScopeContext.INSTANCE.getTenantId();
                    sql.execute(String.format("SET app.tenant_id = '%d'", tenantId));
                    sql.execute("SET app.composite_user_id = 0");
                } catch (PSQLException e) {
                    log.error(
                            "Unable to set app.tenant_id parameter. Exception message: {}",
                            e.getServerErrorMessage()
                    );
                }
            }
            return connection;
        }
    }

    /**
     * The production data source sets up a PostgreSQL database user sourced from an RDS secret that is injected
     * into the application in the production environment.
     */
    @Profile("prod")
    @Configuration
    public static class DataSourceProdConfig {

        private static final String RDS_MIGRATION_SECRET_ENV_VAR_NAME = "RDS_MIGRATION_SECRET";
        private static final String RDS_MAIN_SECRET_ENV_VAR_NAME = "RDS_MAIN_SECRET";

        private final RDSSecret migrationsSecret;
        private final RDSSecret mainSecret;

        private final Environment env;
        private final ObjectMapper objectMapper;

        private static final Logger log = LoggerFactory.getLogger(DataSourceProdConfig.class);

        /**
         * TODO.
         */
        @Autowired
        public DataSourceProdConfig(Environment env, ObjectMapper objectMapper) throws JsonProcessingException {
            this.env = env;
            this.objectMapper = objectMapper;
            migrationsSecret = extractEnvironmentRDSSecret(RDS_MIGRATION_SECRET_ENV_VAR_NAME);
            mainSecret = extractEnvironmentRDSSecret(RDS_MAIN_SECRET_ENV_VAR_NAME);
            log.info("Loaded data source production configuration.");
        }

        @FlywayDataSource
        @Bean(name = "migrationsDataSource")
        public DataSource migrationsDataSource() {
            return createDataSourceBuilderFromSecret(migrationsSecret).type(HikariDataSource.class).build();
        }

        @Primary
        @Bean(name = "mainDataSource")
        public DataSource mainDataSource() {
            return createDataSourceBuilderFromSecret(mainSecret).type(TenantAwareDataSource.class).build();
        }

        /**
         * TODO.
         */
        @Bean(name = "transactionManager")
        public JpaTransactionManager jpaTransactionManager() {
            JpaTransactionManager transactionManager = new JpaTransactionManager();
            transactionManager.setEntityManagerFactory(entityManagerFactoryBean().getObject());
            return transactionManager;
        }

        private HibernateJpaVendorAdapter vendorAdapter() {
            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setShowSql(false);
            vendorAdapter.setDatabasePlatform("org.hibernate.dialect.PostgreSQLDialect");
            return vendorAdapter;
        }

        /**
         * TODO.
         */
        @Bean(name = "entityManagerFactory")
        public LocalContainerEntityManagerFactoryBean entityManagerFactoryBean() {
            HibernateJpaVendorAdapter vendorAdapter = vendorAdapter();

            LocalContainerEntityManagerFactoryBean entityManagerFactoryBean =
                    new LocalContainerEntityManagerFactoryBean();
            entityManagerFactoryBean.setJpaVendorAdapter(vendorAdapter);
            entityManagerFactoryBean.setDataSource(mainDataSource());
            entityManagerFactoryBean.setPersistenceProviderClass(HibernatePersistenceProvider.class);
            entityManagerFactoryBean.setPackagesToScan("smartfloor.domain.entities");
            entityManagerFactoryBean.setPersistenceUnitName("name");
            entityManagerFactoryBean.setJpaDialect(vendorAdapter.getJpaDialect());

            return entityManagerFactoryBean;
        }

        /**
         * Attempts to extract the RDS secret from the given environmental variable.
         *
         * @param envVarName the name of the environmental variable pointing to the RDS secret to extract
         * @return the RDS secret based on the present env var
         */
        private RDSSecret extractEnvironmentRDSSecret(String envVarName) throws JsonProcessingException {
            String secret = Objects.requireNonNull(
                    env.getProperty(envVarName),
                    String.format(
                            "The environment variable %s that should contain the RDS secret is not present.",
                            envVarName
                    )
            );
            return objectMapper.readValue(secret, RDSSecret.class);
        }

        /**
         * Generates a default data source builder object for connecting to a Postgres RDS instance based on the given
         * RDS secret.
         *
         * @param secret the RDS Secret containing credentials for the Postgres RDS instance
         * @return a (Postgres) data source builder object based on the secret that can be configured in more detail
         */
        private static DataSourceBuilder createDataSourceBuilderFromSecret(RDSSecret secret) {
            return DataSourceBuilder.create()
                    .url(
                            String.format(
                                    "jdbc:postgresql://%s:%d/%s",
                                    secret.getHost(),
                                    secret.getPort(),
                                    secret.getDbname()
                            ))
                    .username(secret.getUsername())
                    .password(secret.getPassword());
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        static class RDSSecret {
            private String host;
            private String dbname;
            private String engine;
            private int port;
            private String username;
            private String password;

            public RDSSecret() {
            }

            public RDSSecret(String host, String dbname, String engine, int port, String username, String password) {
                this.host = host;
                this.dbname = dbname;
                this.engine = engine;
                this.port = port;
                this.username = username;
                this.password = password;
            }

            public String getHost() {
                return host;
            }

            public void setHost(String host) {
                this.host = host;
            }

            public String getDbname() {
                return dbname;
            }

            public void setDbname(String dbname) {
                this.dbname = dbname;
            }

            public String getEngine() {
                return engine;
            }

            public void setEngine(String engine) {
                this.engine = engine;
            }

            public int getPort() {
                return port;
            }

            public void setPort(int port) {
                this.port = port;
            }

            public String getUsername() {
                return username;
            }

            public void setUsername(String username) {
                this.username = username;
            }

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }

            @Override
            public String toString() {
                return "RDSSecret{" +
                        "host='" + host + '\'' +
                        ", dbname='" + dbname + '\'' +
                        ", engine='" + engine + '\'' +
                        ", port=" + port +
                        ", username='" + username + '\'' +
                        ", password= <redacted>" +
                        '}';
            }
        }
    }

    /**
     * <p>The data source for the development and test environments is set up in such a way that it connects using a
     * PostgreSQL user for which "Row-Level Security" (RLS) is enabled. This means we are able to lock certain rows
     * based on the current tenant context, allowing CRUD operations for only those rows matching the current tenant
     * context (i.e. the current tenant id).</p>
     *
     * <p>The current tenant context is based upon a runtime (per logical connection) configuration parameter that
     * configures the tenant id. The tenant id is obtained from the tenant context, which can be set by the application
     * (for example after intercepting a tenant id request header from the web request).</p>
     *
     * <p>Note: Flyway migrations run under a separate user, since normally the table owner (creator) is exempt from any
     * RLS policies. We could enforce RLS on the table owner, but this is inconvenient. Hence, we use a separate
     * user.</p>
     */
    @Configuration
    @Profile({"dev", "test"})
    public static class DataSourceDevTestConfig {
        private static HibernateJpaVendorAdapter createHibernateVendorAdapter() {
            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setShowSql(false);
            vendorAdapter.setDatabasePlatform("org.hibernate.dialect.PostgreSQLDialect");
            return vendorAdapter;
        }

        private static LocalContainerEntityManagerFactoryBean createEntityManagerFactory(
                HibernateJpaVendorAdapter vendorAdapter,
                DataSource dataSource
        ) {
            LocalContainerEntityManagerFactoryBean entityManagerFactoryBean =
                    new LocalContainerEntityManagerFactoryBean();
            entityManagerFactoryBean.setJpaVendorAdapter(vendorAdapter);
            entityManagerFactoryBean.setDataSource(dataSource);
            entityManagerFactoryBean.setPersistenceProviderClass(HibernatePersistenceProvider.class);
            entityManagerFactoryBean.setPackagesToScan("smartfloor.domain.entities");
            entityManagerFactoryBean.setPersistenceUnitName("name");
            entityManagerFactoryBean.setJpaDialect(vendorAdapter.getJpaDialect());
            return entityManagerFactoryBean;
        }

        @Bean
        @ConfigurationProperties("spring.datasource")
        public DataSourceProperties dataSourceProperties() {
            return new DataSourceProperties();
        }

        /**
         * TODO.
         */
        @Primary
        @Bean
        public DataSource mainDataSource() {
            return dataSourceProperties()
                    .initializeDataSourceBuilder()
                    .type(TenantAwareDataSource.class)
                    .build();
        }

        @Bean(name = "entityManagerFactory")
        public LocalContainerEntityManagerFactoryBean entityManagerFactoryBean() {
            return createEntityManagerFactory(createHibernateVendorAdapter(), mainDataSource());
        }

        /**
         * TODO.
         */
        @Bean(name = "transactionManager")
        public JpaTransactionManager jpaTransactionManager() {
            JpaTransactionManager transactionManager = new JpaTransactionManager();
            transactionManager.setEntityManagerFactory(entityManagerFactoryBean().getObject());
            return transactionManager;
        }

    }

}



