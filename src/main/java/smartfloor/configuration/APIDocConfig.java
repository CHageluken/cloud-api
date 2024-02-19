package smartfloor.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition
public class APIDocConfig {

    /**
     * TODO.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smartfloor API")
                        .version("v1.14.0")
                        .description("The Smartfloor API service provides the user with various analyses derived " +
                                "from a Smartfloor and its devices."));
    }
}
