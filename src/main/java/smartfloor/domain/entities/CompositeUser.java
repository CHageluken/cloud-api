package smartfloor.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import smartfloor.domain.Role;

@Entity
@Table(name = "composite_users")
public class CompositeUser implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "auth_id", unique = true)
    private String authId;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "compositeUser")
    @JsonIgnore
    private List<User> users = new ArrayList<>();

    public CompositeUser() {
    }

    @Builder
    public CompositeUser(Long id, String authId) {
        this.id = id;
        this.authId = authId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAuthId() {
        return authId;
    }

    public void setAuthId(String authId) {
        this.authId = authId;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CompositeUser)) return false;
        CompositeUser that = (CompositeUser) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(authId, that.authId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, authId);
    }

    @Override
    public String toString() {
        return "CompositeUser{" +
                "id=" + id +
                ", authId='" + authId + '\'' +
                '}';
    }

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated();
        if (isAuthenticated) {
            return authentication.getAuthorities();
        }

        return List.of(Role.USER.toGrantedAuthority());
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return null;
    }

    @Override
    @JsonIgnore
    public String getUsername() {
        return authId;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return true;
    }
}
