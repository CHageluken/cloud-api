package smartfloor.repository.jpa;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import smartfloor.domain.entities.interventions.Intervention;

@Repository
public interface InterventionRepository extends SoftDeleteRepository<Intervention, Long> {
    @Query(value = "SELECT i FROM Intervention i " +
            "WHERE i.user.id = ?1 " +
            "AND i.deleted = ?2 " +
            "AND i.beginTime >= ?3 " +
            "AND (i.endTime IS NULL OR i.endTime <= ?4) " +
            "ORDER BY i.beginTime")
    List<Intervention> findByUserBetweenTimesAndDeleted(
            Long userId,
            boolean deleted,
            LocalDateTime beginTime,
            LocalDateTime endTime
    );
}
