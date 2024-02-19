package smartfloor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(
        exclude = {
                DataSourceAutoConfiguration.class,
        }
)
@EnableJpaRepositories
@EntityScan(basePackages = {"smartfloor.domain"})
@EnableTransactionManagement
@EnableCaching
public class SmartfloorRfidApplication extends SpringBootServletInitializer {
    public static void main(String[] args) {

        SpringApplication.run(SmartfloorRfidApplication.class, args);
    }

}
