package smartfloor.configuration;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This class is used to configure the Jackson JSON serializer.
 */
@Configuration
public class JacksonConfig {

    /**
     * This method configures the Jackson JSON serializer to include a field in the JSON response whenever a field is
     * not annotated with {@link com.fasterxml.jackson.annotation.JsonView}. This is useful when a field is not (yet)
     * annotated with {@link com.fasterxml.jackson.annotation.JsonView}. Using this configuration option, we can
     * gradually add {@link com.fasterxml.jackson.annotation.JsonView} annotations to the fields of existing entities
     * that had not yet been annotated (e.g. because they do not yet have any role-based "visible" fields).
     *
     * @return the configured Jackson JSON serializer
     */
    @Bean
    public JsonMapper objectMapper() {
        return JsonMapper.builder()
                .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, true)
                .build();
    }
}
