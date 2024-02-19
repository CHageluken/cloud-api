package smartfloor.repository.jpa;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.entities.Wearable;

@Repository
public interface FootstepRepository extends JpaRepository<Footstep, Long> {

    /**
     * Get the (wearableId, footstepCount) tuples that describe the total amount of footsteps per wearable
     * within the timeframe provided by the beginTime and endTime parameters.
     */
    @Query(
            value = "SELECT COUNT(id) as footstepCount, wearable_id AS wearableId FROM" +
                    " footsteps WHERE footsteps.timestamp BETWEEN ?1 AND ?2 GROUP BY wearableId",
            nativeQuery = true
    )
    Set<WearableStepCount> findWearableStepCounts(Date beginTime, Date endTime);

    List<Footstep> findByWearableIdAndTimeBetweenOrderByTimeAsc(
            String wearableId,
            LocalDateTime beginTime,
            LocalDateTime endTime
    );

    List<Footstep> findAllByWearableInAndTimeBetweenOrderByTimeAsc(
            List<Wearable> wearables,
            LocalDateTime beginTime,
            LocalDateTime endTime
    );

    int countByWearableIdAndTimeBetween(String wearableId, LocalDateTime beginTime, LocalDateTime endTime);

    Optional<Footstep> findFirstByFloorIdAndWearableIdOrderByTimeDesc(Long floorId, String wearableId);

    Optional<Footstep> findFirstByFloorIdOrderByTimeDesc(Long floorId);

    /**
     * Projection for the (wearableId, footstepCount) tuples that we get back from our custom footstep count aggregation
     * query.
     */
    interface WearableStepCount {
        int getFootstepCount();

        String getWearableId();
    }
}
