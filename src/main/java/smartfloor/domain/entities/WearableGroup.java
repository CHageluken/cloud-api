package smartfloor.domain.entities;

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
import java.io.Serializable;
import java.util.List;
import lombok.Builder;

/**
 * <p>A wearable group contains one or more wearables, it directly relates to thing groups in AWS IoT.
 * The thing groups is what we use to group wearable (heelable) devices with. For various purposes, we need to mirror
 * this relationship conceptually in our database and on the application-level.
 * An example of such a purpose is figuring out which set of wearables can be selected for a certain group of users.
 * That is, we allow a certain group manager (user) to select from a certain set (group) of wearables to link (wearable)
 * users with</p>
 * <p>Another important point to make is the fact that the relationship between wearable groups and wearables is a
 * many-to-many relationship. On the Smart Floor domain level, this currently does not make much sense, as any wearable
 * can only really be assigned to a single group at the time. However, in AWS it is possible to have multiple thing
 * groups contain the same thing at the same time. Therefore, we chose to model it this way.</p>
 * Finally, it should be noted that the wearable groups grew out of a necessity to model the relationship between
 * thing groups and user groups within tenants. This because they are often used together: one almost always has a
 * limited pool of wearables available for a user group, specifically the wearables that are in a location where the
 * group's users are to be measured. In the future, we may want to remodel some relationships between users, wearables
 * and groups of them, especially in (transitive) relation to their tenant.
 * There is currently some possible awkwardness in the modeling, which can be ironed out as soon as the use cases
 * (at-home elderly measuring, etc.) crystallize.
 */
@Entity
@Table(name = "wearable_groups")
public final class WearableGroup implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinTable(
            name = "wearable_group_members",
            joinColumns = @JoinColumn(name = "wearable_group_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "wearable_id", referencedColumnName = "id")
    )
    private List<Wearable> wearables;

    public WearableGroup() {
    }

    /**
     * TODO.
     */
    @Builder
    public WearableGroup(Long id, String name, List<Wearable> wearables) {
        this.id = id;
        this.name = name;
        this.wearables = wearables;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Wearable> getWearables() {
        return wearables;
    }
}