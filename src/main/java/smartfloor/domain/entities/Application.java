package smartfloor.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Builder;

@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    /**
     * <p>The name of an application, should be unique.</p>
     * Examples: "FRP" (Fall Risk Profile), "Rehabilitation", "Activity".
     */
    @Column(nullable = false, unique = true)
    public String name;

    /**
     * <p>The priority with which a certain application is shown, where a lower number indicates a higher priority.</p>
     * Can be used to determine (for example): 1. Which application to load/show initially in case a user or tenant has
     * access to two or more applications. 2. To order the navigation of accessible applications in the navigation bar.
     */
    @Column(nullable = false, unique = true)
    public int priority;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinTable(
            name = "application_access",
            joinColumns = @JoinColumn(name = "application_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id")
    )
    @JsonIgnore
    private List<User> users = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinTable(
            name = "tenant_applications",
            joinColumns = @JoinColumn(name = "application_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "tenant_id", referencedColumnName = "id")
    )
    @JsonIgnore
    private List<Tenant> tenants = new ArrayList<>();

    /**
     * TODO.
     */

    @Builder
    public Application(Long id, String name, int priority, List<User> users, List<Tenant> tenants) {
        this.id = id;
        this.name = name;
        this.priority = priority;
        this.users = users;
        this.tenants = tenants;
    }

    protected Application() {
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

    public void setName(String name) {
        this.name = name;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Application that = (Application) o;
        return priority == that.priority && id.equals(that.id) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, priority);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (users != null && !this.users.isEmpty()) {
            for (User u : this.users) sb.append(u.getId() + " ");
        }

        StringBuilder st = new StringBuilder();

        if (tenants != null && !this.tenants.isEmpty()) {
            for (Tenant u : this.tenants) st.append(u.getId() + " ");
        }

        return "Application{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", priority=" + priority +
                ", users=" + sb +
                ", tenants=" + st +
                '}';
    }
}
