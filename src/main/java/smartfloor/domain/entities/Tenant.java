package smartfloor.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import org.springframework.lang.Nullable;

@Entity
@Table(name = "tenants")
public class Tenant implements Serializable {

    /**
     * Default tenant, as of now we do not support multitenancy yet.
     */
    private static final Long DEFAULT_TENANT_ID = 1L;
    public static final String DEFAULT_TENANT_NAME = "smartfloor";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    @NotNull
    private String name;

    @Column(name = "user_limit")
    @Nullable
    private Integer userLimit;

    /**
     * The user that represents the tenant (ie. a manager or a system administrator for a tenant).
     */
    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "rep_user_id")
    @JsonIgnoreProperties("tenant")
    private User representative;

    /**
     * A tenant has one or more users.
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "tenant")
    @JsonManagedReference
    @JsonIgnoreProperties("groups")
    private List<User> users;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "tenant")
    private List<Group> groups;

    public Tenant() {
    }

    public Tenant(String name) {
        this.name = name;
    }

    /**
     * TODO.
     */
    @Builder
    public Tenant(String name, User representative, List<User> users, Integer userLimit) {
        this.name = name;
        this.representative = representative;
        this.users = users;
        this.userLimit = userLimit;
    }

    private Tenant(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static Tenant getDefaultTenant() {
        return new Tenant(DEFAULT_TENANT_ID, DEFAULT_TENANT_NAME);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public Integer getUserLimit() {
        return userLimit;
    }

    public void setUserLimit(@Nullable Integer userLimit) {
        this.userLimit = userLimit;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getRepresentative() {
        return representative;
    }

    public void setRepresentative(User representative) {
        this.representative = representative;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tenant tenant = (Tenant) o;
        return Objects.equals(id, tenant.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "Tenant{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", representative=" + representative +
                '}';
    }
}

