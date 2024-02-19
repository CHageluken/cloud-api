package smartfloor.repository.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import smartfloor.domain.entities.user.info.history.UserInfoHistory;
import smartfloor.domain.entities.user.info.history.UserInfoHistoryId;

@Repository
public interface UserInfoHistoryRepository extends JpaRepository<UserInfoHistory, UserInfoHistoryId> {
    List<UserInfoHistory> findAllByUserIdOrderByEndTime(Long userId);

    @Query(
            value = "SELECT * FROM user_info_history" +
                    " WHERE user_id = ?1 AND end_time > ?2" +
                    " ORDER BY end_time ASC LIMIT 1",
            nativeQuery = true
    )
    Optional<UserInfoHistory> findEarliestByUserIdAfterEndTime(Long userId, LocalDateTime endTime);
}
