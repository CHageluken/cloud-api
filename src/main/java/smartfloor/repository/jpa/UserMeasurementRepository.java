package smartfloor.repository.jpa;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;
import smartfloor.domain.UserMeasurementType;
import smartfloor.domain.entities.UserMeasurement;

@Repository
public interface UserMeasurementRepository extends SoftDeleteRepository<UserMeasurement, Long> {
    List<UserMeasurement> findByUserId(Long id);

    List<UserMeasurement> findByUserIdAndDeleted(Long userId, boolean deleted);

    List<UserMeasurement> findByUserIdAndTypeAndDeletedAndRecordedAtBetweenOrderByRecordedAtAsc(
            Long userId,
            UserMeasurementType type,
            boolean deleted,
            LocalDateTime beginTime,
            LocalDateTime endTime
    );
}
