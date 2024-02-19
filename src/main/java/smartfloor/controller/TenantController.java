package smartfloor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import smartfloor.domain.dto.UserLimit;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.entities.User;

@Tag(name = "Tenant API", description = "Provides a method for checking a tenant's user limit.")
@RestController
@RequestMapping("/tenants")
public class TenantController {

    public TenantController() {
    }

    /**
     * Get user limit of current user's tenant.
     */
    @Operation(description = "Get user limit of tenant with given identifier.")
    @GetMapping("/user-limit")
    @ResponseStatus(HttpStatus.OK)
    public UserLimit getUserLimit(@AuthenticationPrincipal User user) {
        Tenant tenant = user.getTenant();
        return new UserLimit(tenant.getUserLimit());
    }
}
