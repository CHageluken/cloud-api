package smartfloor.controller.advice;

import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.AbstractMappingJacksonResponseBodyAdvice;
import smartfloor.domain.Role;
import smartfloor.serializer.views.Views;

/**
 * This class is used to determine which JSON view to use when serializing a response.
 * The view is determined based on the role of the currently authenticated user.
 * Any entity that has its fields annotated with {@link com.fasterxml.jackson.annotation.JsonView} will be serialized
 * using the view determined by this class. For example, if the currently authenticated user is a manager,
 * then the view {@link Views.Manager} will be used to serialize the entity. This means that only the fields
 * annotated with {@code JsonView(Views.Manager)} will be serialized.
 */
@RestControllerAdvice
class GlobalRoleBasedJsonViewAdvice extends AbstractMappingJacksonResponseBodyAdvice {

    @Override
    protected void beforeBodyWriteInternal(
            MappingJacksonValue bodyContainer,
            MediaType contentType,
            MethodParameter returnType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
                SecurityContextHolder.getContext().getAuthentication().getAuthorities() != null) {
            Collection<? extends GrantedAuthority> authorities
                    = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
            boolean isAdmin = authorities.contains(Role.ADMIN.toGrantedAuthority());
            boolean isManager = authorities.contains(Role.MANAGER.toGrantedAuthority());
            boolean isUser = authorities.contains(Role.USER.toGrantedAuthority());

            if (isAdmin) {
                bodyContainer.setSerializationView(Views.Admin.class);
            } else if (isManager) {
                bodyContainer.setSerializationView(Views.Manager.class);
            } else if (isUser) {
                bodyContainer.setSerializationView(Views.User.class);
            } else {
                throw new IllegalArgumentException("Unknown role for authorities " + authorities.stream()
                        .map(GrantedAuthority::getAuthority).collect(Collectors.joining(",")));
            }
        }
    }
}