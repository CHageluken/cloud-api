package smartfloor.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "version")
public class VersionActuator {

    String version;

    @ReadOperation
    public String getVersion() {
        return version;
    }

    VersionActuator() {
        APIDocConfig apiConfig = new APIDocConfig();
        OpenAPI api = apiConfig.customOpenAPI();
        version = api.getInfo().getVersion();
    }
}