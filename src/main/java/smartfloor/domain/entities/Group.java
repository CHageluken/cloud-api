package smartfloor.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import lombok.Builder;
import org.springframework.lang.Nullable;
import smartfloor.serializer.views.Views;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true, value = {"hibernateLazyInitializer", "handler"})
@Table(name = "groups")
public class Group implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(Views.User.class)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    @JsonIgnore
    private Tenant tenant;

    /**
     * The name of the group, this is in fact meant to be the "display" name of the group.
     * That is, it will be the name that is used by any client applications to display to application users.
     */
    @Column(name = "name")
    @JsonView(Views.User.class)
    private String name;

    /**
     * <p>The name of the thing group (AWS IoT) that this user group corresponds to.
     * A thing group is a group of wearable devices that the user group can be associated with.</p>
     * In practice, the associated thing group is used by client applications to determine which subset of wearable
     * devices to show for linking to any of the users in the group.
     */
    @Column(name = "thing_group_name")
    @JsonView(Views.Manager.class)
    private String thingGroupName;

    /**
     * A group contains zero or more users.
     * In natural language, we may define this relation as "A user is part of a group".
     * While the relationship is many-to-many, the Hibernate mapping is one-to-many on the Group side due to the
     */

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinTable(
            name = "group_users",
            joinColumns = @JoinColumn(name = "group_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id")
    )
    @JsonIgnoreProperties("groups")
    @JsonView(Views.Manager.class)
    private List<User> users;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinTable(
            name = "group_managers",
            joinColumns = @JoinColumn(name = "group_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id")
    )
    @JsonView(Views.Admin.class)
    private List<User> managers;

    @Column(name = "user_limit")
    @Nullable
    @JsonView(Views.Manager.class)
    private Integer userLimit;

    @OneToOne
    @JoinColumn(name = "wearable_group_id")
    @JsonView(Views.Manager.class)
    private WearableGroup wearableGroup;

    public Group() {
    }

    /**
     * TODO.
     */
    @Builder
    public Group(
            Tenant tenant,
            String name,
            String thingGroupName,
            WearableGroup wearableGroup,
            List<User> users,
            List<User> managers,
            Integer userLimit
    ) {
        this.tenant = tenant;
        this.name = name;
        this.thingGroupName = thingGroupName;
        this.wearableGroup = wearableGroup;
        this.users = users;
        this.managers = managers;
        this.userLimit = userLimit;
    }

    /**
     * TODO.
     */
    public Group(Tenant tenant, String name, String thingGroupName, List<User> users) {
        this.tenant = tenant;
        this.name = name;
        this.thingGroupName = thingGroupName;
        this.users = users;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getThingGroupName() {
        return thingGroupName;
    }

    public void setThingGroupName(String thingGroupName) {
        this.thingGroupName = thingGroupName;
    }

    /**
     * Get only the active users of this group.
     */
    @JsonProperty("users")
    @JsonView(Views.Manager.class)
    public List<User> getUsers() {
        return users == null ? null : users
                .stream()
                .filter(Predicate.not(User::isArchived))
                .toList();
    }

    /**
     * Get all users for this group or only archived ones.
     */
    public List<User> getUsers(boolean includeActiveUsers) {
        if (includeActiveUsers) {
            return users;
        }
        return getArchivedUsers();
    }

    @JsonProperty("archivedUsers")
    @JsonView(Views.Manager.class)
    private List<User> getArchivedUsers() {
        return users.stream().filter(User::isArchived).toList();
    }

    public List<User> getManagers() {
        return managers;
    }

    @Nullable
    public Integer getUserLimit() {
        return userLimit;
    }

    public void setUserLimit(@Nullable Integer userLimit) {
        this.userLimit = userLimit;
    }

    public WearableGroup getWearableGroup() {
        return wearableGroup;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Group group = (Group) o;
        return Objects.equals(id, group.id) &&
                Objects.equals(tenant, group.tenant) &&
                Objects.equals(name, group.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tenant, name);
    }

    @Override
    public String toString() {
        return "Group{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
