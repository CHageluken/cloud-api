package smartfloor.domain.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import smartfloor.deserializer.CustomLocalDateTimeDeserializer;
import smartfloor.domain.Role;
import smartfloor.serializer.CustomLocalDateTimeSerializer;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true, value = {"hibernateLazyInitializer", "handler"})
@Table(name = "users")
@SecondaryTable(
        name = "user_info",
        pkJoinColumns = @PrimaryKeyJoinColumn(name = "user_id")
)
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "auth_id", unique = true)
    private String authId;

    /**
     * Every user belongs to a tenant.
     */
    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    @JsonBackReference
    private Tenant tenant;

    /**
     * References the users links to wearables over time.
     */
    @OneToMany(
            cascade = CascadeType.ALL,
            mappedBy = "user"
    )
    @JsonIgnore
    private List<UserWearableLink> userWearableLinks;

    /**
     * References the groups the user is part of.
     */
    @ManyToMany(mappedBy = "users")
    @JsonIgnore
    private List<Group> groups;

    /**
     * References the groups the user is managing.
     */
    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "managers")
    @JsonIgnore
    private List<Group> managedGroups;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "user")
    @JsonIgnore
    private List<UserMeasurement> measurements = new ArrayList<>();

    @Column(name = "archived_at")
    @Nullable
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime archivedAt;

    @ManyToOne
    @JoinColumn(name = "composite_user_id")
    private CompositeUser compositeUser;

    @Embedded
    @AttributeOverrides(
            {
                    @AttributeOverride(
                            name = "admissionDiagnosis",
                            column = @Column(name = "admission_diagnosis", table = "user_info")
                    ),
                    @AttributeOverride(
                            name = "secondaryDiagnosis",
                            column = @Column(name = "secondary_diagnosis", table = "user_info")
                    ),
                    @AttributeOverride(
                            name = "relevantMedication",
                            column = @Column(name = "relevant_medication", table = "user_info")
                    ),
                    @AttributeOverride(name = "gender", column = @Column(name = "gender", table = "user_info")),
                    @AttributeOverride(name = "height", column = @Column(name = "height", table = "user_info")),
                    @AttributeOverride(name = "weight", column = @Column(name = "weight", table = "user_info")),
                    @AttributeOverride(name = "age", column = @Column(name = "age", table = "user_info")),
                    @AttributeOverride(name = "orthosis", column = @Column(name = "orthosis", table = "user_info")),
                    @AttributeOverride(name = "shoes", column = @Column(name = "shoes", table = "user_info")),
                    @AttributeOverride(
                            name = "walkingAid",
                            column = @Column(name = "walking_aid", table = "user_info")
                    ),
                    @AttributeOverride(name = "notes", column = @Column(name = "notes", table = "user_info"))
            }
    )
    private UserInfo info;

    public User() {
    }

    @Builder
    private User(
            Long id,
            String authId,
            Tenant tenant,
            List<UserWearableLink> userWearableLinks,
            List<UserMeasurement> measurements,
            List<Group> groups,
            List<Group> managedGroups,
            LocalDateTime archivedAt,
            CompositeUser compositeUser
    ) {
        this.id = id;
        this.authId = authId;
        this.tenant = tenant;
        this.userWearableLinks = userWearableLinks;
        this.measurements = measurements;
        this.groups = groups;
        this.managedGroups = managedGroups;
        this.archivedAt = archivedAt;
        this.compositeUser = compositeUser;
    }

    public User(Tenant tenant, List<UserWearableLink> userWearableLinks) {
        this.tenant = tenant;
        this.userWearableLinks = userWearableLinks;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public List<UserWearableLink> getUserWearableLinks() {
        return userWearableLinks;
    }

    public void setUserWearableLinks(List<UserWearableLink> userWearableLinks) {
        this.userWearableLinks = userWearableLinks;
    }

    public String getAuthId() {
        return authId;
    }

    public void setAuthId(String authId) {
        this.authId = authId;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public List<UserMeasurement> getMeasurements() {
        return measurements;
    }

    public void setMeasurements(List<UserMeasurement> measurements) {
        this.measurements = measurements;
    }

    public UserInfo getInfo() {
        return info;
    }

    public void setInfo(UserInfo info) {
        this.info = info;
    }

    @Nullable
    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }

    @JsonProperty("isArchived")
    public boolean isArchived() {
        return archivedAt != null;
    }

    public void archive() {
        this.archivedAt = LocalDateTime.now();
    }

    public void unarchive() {
        this.archivedAt = null;
    }

    public List<Group> getManagedGroups() {
        return managedGroups;
    }

    public CompositeUser getCompositeUser() {
        return compositeUser;
    }

    public void setCompositeUser(CompositeUser compositeUser) {
        this.compositeUser = compositeUser;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", tenant=" + tenant +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) &&
                Objects.equals(authId, user.authId) &&
                Objects.equals(tenant, user.tenant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, authId, tenant);
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
