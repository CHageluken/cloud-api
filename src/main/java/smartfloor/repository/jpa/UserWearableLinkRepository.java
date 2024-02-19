package smartfloor.repository.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import smartfloor.domain.entities.UserWearableLink;

@Repository
public interface UserWearableLinkRepository<T extends UserWearableLink> extends JpaRepository<UserWearableLink, Long> {

    UserWearableLink save(UserWearableLink userWearableLink);

    UserWearableLink saveAndFlush(UserWearableLink userWearableLink);

    @Modifying
    @Query(
            value = "UPDATE user_wearable_links SET end_time = NOW()" +
                    " WHERE user_id = ?1 AND end_time > NOW()", nativeQuery = true
    )
    void completeActiveByUserId(Long userId);

    List<T> findByUserIdOrderByBeginTimeAsc(Long id, Class<T> type);

    List<T> findByWearableId(String id, Class<T> type);

    /**
     * <p>Find all user wearable links that overlap the given time window W (beginTime, endTime).</p>
     * Useful for when we want to check if a to be created user wearable link L' would conflict with existing links.
     * Here, W defines the time window in which L' holds and for which an existing link L could already hold.
     * <p>Let ||-------------|| indicate our time window W with begin time b and end time e.
     * Let |-------------| indicate our user wearable link L with begin time b' and end time e'.</p>
     * <p>Moreover, let us define the following conditions
     * (1) b <= b' <= e.
     * (2) b <= e' <= e.
     * (3) b' <= b AND e' >= e.</p>
     * Then valid cases are:
     * ||---|----|---|| => L fully contained in W, so L is part of the result set. It matches the query because
     * (1) b <= b' <= e AND (2) b <= e' <= e.
     * ||------|--||--| => L partially overlaps W, so L is part of the result set. It matches the query because
     * (1) b <= b' <= e.
     * |--||--|------|| => L partially overlaps W, so L is part of the result set. It matches the query because
     * (2) b <= e' <= e.
     * ||---|----|---|| => W fully contained in L, so L is part of the result set. It matches the query because
     * (3) b' <= b AND e' >= e.
     * <p>Invalid cases are:
     * ||------|| |----| => L to the right of W, so L is not part of the result set. It does not match the query because
     * neither (1), (2) or (3) hold.
     * |----| ||------|| => L to the left of W, so L is not part of the result set. It does not match the query because
     * neither (1), (2) or (3) hold.</p>
     *
     * @return a list of user wearable links that overlap the given time window
     */
    @Query(
            value = "SElECT * FROM user_wearable_links WHERE" +
                    "((?1 <= end_time) AND (?2 >= begin_time)) ORDER BY begin_time DESC", nativeQuery = true
    )
    List<UserWearableLink> findAllByOverlappingTimeWindow(LocalDateTime beginTime, LocalDateTime endTime);

    @Query(
            value = "SELECT * FROM user_wearable_links WHERE wearable_id = ?1" +
                    " AND ((?2 <= end_time) AND (?3 >= begin_time)) ORDER BY begin_time DESC", nativeQuery = true
    )
    List<UserWearableLink> findAllByWearableIdWithOverlappingTimeWindow(
            String wearableId,
            LocalDateTime beginTime,
            LocalDateTime endTime
    );

    @Query(
            value = "SELECT * FROM user_wearable_links WHERE wearable_id = ?1" +
                    " AND ((?2 <= end_time) AND (?3 >= begin_time)) ORDER BY begin_time DESC LIMIT 1",
            nativeQuery = true
    )
    Optional<UserWearableLink> findLatestByWearableIdWithOverlappingTimeWindow(
            String wearableId,
            LocalDateTime beginTime,
            LocalDateTime endTime
    );

    @Query(
            value = "SELECT * FROM user_wearable_links WHERE user_id = ?1" +
                    " AND ((?2 <= end_time) AND (?3 >= begin_time)) ORDER BY begin_time DESC", nativeQuery = true
    )
    List<UserWearableLink> findAllByUserIdWithOverlappingTimeWindow(
            long userId,
            LocalDateTime beginTime,
            LocalDateTime endTime
    );

    void delete(UserWearableLink userWearableLink);

    @Query(
            value = "SELECT * FROM user_wearable_links" +
                    " WHERE user_id IN (?1) AND end_time >= NOW()" +
                    " ORDER BY user_id ASC", nativeQuery = true
    )
    List<UserWearableLink> findActiveByUserIds(List<Long> userIds);

    @Query(
            value = "SELECT * FROM user_wearable_links" +
                    " WHERE wearable_id = ?1 AND end_time > NOW()" +
                    " ORDER BY begin_time DESC LIMIT 1", nativeQuery = true
    )
    Optional<UserWearableLink> findActiveByWearableId(String id);

    @Query(
            value = "SELECT * FROM user_wearable_links" +
                    " WHERE user_id = ?1 AND end_time > NOW()" +
                    " ORDER BY begin_time DESC LIMIT 1", nativeQuery = true
    )
    Optional<UserWearableLink> findActiveByUserId(Long id);
}
