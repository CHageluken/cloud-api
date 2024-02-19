package smartfloor.domain;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public enum Role {
    ADMIN("ADMIN"),
    MANAGER("MANAGER"),
    USER("USER");

    private final String name;

    Role(final String name) {
        this.name = "ROLE_" + name;
    }

    @Override
    public String toString() {
        return name;
    }

    public GrantedAuthority toGrantedAuthority() {
        return new SimpleGrantedAuthority(name);
    }
}
