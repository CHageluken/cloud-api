package smartfloor.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;

/**
 * Every footstep contains a position that has an (x,y) location on the floor that it was made on.
 * This entity describes such a position and is referenced by the footstep entity.
 */
@Embeddable
public class Position implements Serializable {

    @Column(name = "x", nullable = false)
    private double x;

    @Column(name = "y", nullable = false)
    private double y;

    public Position() {
    }

    public Position(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Computes the Euclidean distance to a given (footstep) position.
     *
     * @param p - a given footstep position
     * @return Euclidean distance from this position to p
     */
    public Double distanceTo(Position p) {
        return Math.sqrt(Math.pow(this.x - p.getX(), 2) + Math.pow(this.y - p.getY(), 2));
    }

    public double getX() {
        return x;
    }

    public void setX(double positionX) {
        this.x = positionX;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }
}
