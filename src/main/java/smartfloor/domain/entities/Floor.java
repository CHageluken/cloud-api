package smartfloor.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import smartfloor.domain.entities.fall.risk.profile.FallRiskProfile;

/**
 * Represents a smart floor, consisting of some metadata about the floor and the tags that together make up the floor
 * grid.
 */
@Table(name = "floors")
@Entity
@JsonIgnoreProperties(ignoreUnknown = true, value = {"hibernateLazyInitializer", "handler"})
public class Floor implements Serializable {

    private static final long serialVersionUID = 2L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    /**
     * Every floor has a certain maximum x coordinate, which equals the number of "columns" of tags that the floor has.
     */
    @Column(name = "max_x", nullable = false)
    private int maxX;

    /**
     * Every floor has a certain maximum y coordinate, which equals the number of "rows" of tags that the floor has.
     */
    @Column(name = "max_y", nullable = false)
    private int maxY;

    /**
     * The orientation of the floor with respect to north (in degrees). Used when determining the orientation of the
     * wearable based on the orientation values we get from it.
     */
    @Column(name = "orientation_north")
    private double orientationNorth = 0.0;

    /**
     * The rotation (in degrees) the floor should have when displayed (in a UI for example).
     */
    @Column(name = "rotation")
    private double rotation = 0.0;

    /**
     * Zero or more footsteps have been made on the floor.
     */
    @OneToMany(cascade = {CascadeType.ALL}, mappedBy = "floor", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Footstep> footsteps;

    /**
     * Zero or more fall risk profiles have been made on the floor.
     */
    @OneToMany(cascade = {CascadeType.ALL}, mappedBy = "floor", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<FallRiskProfile> fallRiskProfiles;

    /**
     * The floor has zero or more viewers that may access it.
     */
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinTable(
            name = "floor_viewers",
            joinColumns = @JoinColumn(name = "floor_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id")
    )
    private List<User> viewers;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinTable(
            name = "floor_groups",
            joinColumns = @JoinColumn(name = "floor_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "group_id", referencedColumnName = "id")
    )
    @JsonIgnoreProperties({"users", "archivedUsers"})
    private List<Group> groups;

    public Floor() {
    }

    /**
     * TODO.
     */
    public Floor(String name, int maxX, int maxY, double orientationNorth, double rotation) {
        this.name = name;
        this.maxX = maxX;
        this.maxY = maxY;
        this.orientationNorth = orientationNorth;
        this.rotation = rotation;
    }

    @Builder
    private Floor(
            String name,
            int maxX,
            int maxY,
            double orientationNorth,
            double rotation,
            List<Footstep> footsteps,
            List<FallRiskProfile> fallRiskProfiles,
            List<User> viewers,
            List<Group> groups
    ) {
        this.name = name;
        this.maxX = maxX;
        this.maxY = maxY;
        this.orientationNorth = orientationNorth;
        this.rotation = rotation;
        this.footsteps = footsteps;
        this.fallRiskProfiles = fallRiskProfiles;
        this.viewers = viewers;
        this.groups = groups;
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

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    public int getMaxX() {
        return maxX;
    }

    public void setMaxX(int maxX) {
        this.maxX = maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public void setMaxY(int maxY) {
        this.maxY = maxY;
    }

    public double getOrientationNorth() {
        return orientationNorth;
    }

    public void setOrientationNorth(double orientationNorth) {
        this.orientationNorth = orientationNorth;
    }

    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }

    public List<Footstep> getFootsteps() {
        return footsteps;
    }

    public List<FallRiskProfile> getFallRiskProfiles() {
        return fallRiskProfiles;
    }

    public List<User> getViewers() {
        return viewers;
    }

    public List<Group> getGroups() {
        return groups;
    }

    @Override
    public String toString() {
        return "Floor{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", maxX=" + maxX +
                ", maxY=" + maxY +
                ", orientationNorth=" + orientationNorth +
                ", rotation=" + rotation +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Floor floor = (Floor) o;
        return maxX == floor.maxX && maxY == floor.maxY &&
                Double.compare(floor.orientationNorth, orientationNorth) == 0 &&
                Double.compare(floor.rotation, rotation) == 0 && id.equals(floor.id) && name.equals(floor.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, maxX, maxY, orientationNorth, rotation);
    }
}
