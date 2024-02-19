package smartfloor.repository.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import smartfloor.domain.entities.fall.risk.profile.FallRiskProfile;

@Repository
public interface FallRiskProfileRepository extends JpaRepository<FallRiskProfile, Long> {

    /**
     * <p>We query the fall risk profiles of the wearable for which the id is given.
     * A fall risk profile has three temporal properties: its creation time, begin time and end time. The begin and end
     * time are respectively the timestamps of the first and last footstep that the FRP is based on.</p>
     * <p>Here, we look up the fall risk profiles which were created within the given period of time (beginTime,
     * endTime) where beginTime and endTime are the given parameters.</p> Note: we do not include hidden FRPs in the
     * result set.
     */

    @Query(
            value =
                    "SELECT frp.* FROM fall_risk_profiles frp " +
                            "LEFT JOIN fall_risk_profile_removals frp_rem ON frp.id = frp_rem.fall_risk_profile_id " +
                            "WHERE frp.wearable_id=?1 AND frp.hidden=false AND frp.created_at BETWEEN ?2 AND ?3 " +
                            "AND frp_rem.fall_risk_profile_id IS NULL " +
                            "ORDER BY frp.created_at",
            nativeQuery = true
    )
    List<FallRiskProfile> findByWearableIdAndCreationTimeBetweenAndHiddenFalseOrderByCreationTime(
            String wearableId,
            LocalDateTime beginTime,
            LocalDateTime endTime
    );

    /**
     * <p>We query the fall risk profiles of the wearable for which the id is given.
     * A fall risk profile has three temporal properties: its creation time, begin time and end time. The begin and end
     * time are respectively the timestamps of the first and last footstep that the FRP is based on.</p>
     * <p>Here, we look up the fall risk profiles which were created within the given period of time (beginTime,
     * endTime) where beginTime and endTime are the given parameters.</p> Note: we include hidden FRPs in the result
     * set.
     */
    @Query(
            value =
                    "SELECT frp.* FROM fall_risk_profiles frp " +
                            "LEFT JOIN fall_risk_profile_removals frp_rem ON frp.id = frp_rem.fall_risk_profile_id " +
                            "WHERE frp.wearable_id=?1 AND frp.created_at BETWEEN ?2 AND ?3 " +
                            "AND frp_rem.fall_risk_profile_id IS NULL " +
                            "ORDER BY frp.created_at",
            nativeQuery = true
    )
    List<FallRiskProfile> findByWearableIdAndCreationTimeBetweenOrderByCreationTime(
            String wearableId,
            LocalDateTime beginTime,
            LocalDateTime endTime
    );

    /**
     * <p>We query the fall risk profiles of the wearable for which the id is given.</p>
     * Note: we do not include hidden FRPs in the result set.
     */
    @Query(
            value =
                    "SELECT frp.* FROM fall_risk_profiles frp " +
                            "LEFT JOIN fall_risk_profile_removals frp_rem ON frp.id = frp_rem.fall_risk_profile_id " +
                            "WHERE frp.wearable_id=?1 AND frp.hidden=false " +
                            "AND frp_rem.fall_risk_profile_id IS NULL " +
                            "ORDER BY frp.created_at",
            nativeQuery = true
    )
    List<FallRiskProfile> findByWearableIdAndHiddenFalseOrderByCreationTime(String wearableId);

    /**
     * <p>We query the fall risk profiles of the wearable for which the id is given.</p>
     * Note: we include hidden FRPs in the result set.
     */
    @Query(
            value =
                    "SELECT frp.* FROM fall_risk_profiles frp " +
                            "LEFT JOIN fall_risk_profile_removals frp_rem ON frp.id = frp_rem.fall_risk_profile_id " +
                            "WHERE frp.wearable_id=?1 " +
                            "AND frp_rem.fall_risk_profile_id IS NULL " +
                            "ORDER BY frp.created_at",
            nativeQuery = true
    )
    List<FallRiskProfile> findByWearableIdOrderByCreationTime(String wearableId);

    /**
     * Retrieve the fall risk profiles of the user for which the id is given. Only the fall risk profiles that were
     * created within the period of time (beginTime, endTime) are returned. Added as part of VIT-714 to optimize the
     * performance of the fall risk profile retrieval. All work for fetching fall risk profiles that can be handled by
     * the database is done there.
     *
     * @param userId The id of the user for which we want to retrieve the fall risk profiles.
     * @param beginTime The beginning time of the period of time for which we want to retrieve the fall risk profiles.
     * @param endTime The end time of the period of time for which we want to retrieve the fall risk profiles.
     * @return A list of non-hidden fall risk profiles that were created within the period of time (beginTime, endTime)
     * for the user with the given user id.
     */
    @Query(
            value =
                    "WITH filtered_frp AS ( " +
                            "SELECT f.* " +
                            "FROM fall_risk_profiles f " +
                            "LEFT JOIN fall_risk_profile_removals frp_rem ON f.id = frp_rem.fall_risk_profile_id " +
                            "WHERE f.begin_time >= ?2 " +
                            "AND f.end_time <= ?3 " +
                            "AND frp_rem.fall_risk_profile_id IS NULL " +
                            ") " +
                            "SELECT frp.* " +
                            "FROM filtered_frp frp " +
                            "INNER JOIN user_wearable_links uwl ON frp.wearable_id = uwl.wearable_id " +
                            "AND uwl.user_id = ?1 " +
                            "WHERE frp.begin_time >= uwl.begin_time " +
                            "AND frp.end_time <= uwl.end_time " +
                            "AND frp.hidden = false ",
            nativeQuery = true
    )
    List<FallRiskProfile> findByUserIdBetweenTimes(Long userId, LocalDateTime beginTime, LocalDateTime endTime);

    @Query(
            value =
                    "WITH user_frps AS ( " +
                            "SELECT frp.* " +
                            "FROM fall_risk_profiles frp " +
                            "LEFT JOIN fall_risk_profile_removals frp_rem ON frp.id = frp_rem.fall_risk_profile_id " +
                            "INNER JOIN user_wearable_links uwl ON frp.wearable_id = uwl.wearable_id " +
                            "WHERE uwl.user_id = ?1 AND frp.end_time BETWEEN uwl.begin_time AND uwl.end_time " +
                            "AND frp_rem.fall_risk_profile_id IS NULL " +
                            ") " +
                            "SELECT * " +
                            "FROM user_frps " +
                            "ORDER BY end_time DESC " +
                            "LIMIT 1",
            nativeQuery = true

    )
    Optional<FallRiskProfile> findLatestByUserId(Long userId);

    @Query(
            value =
                    "SELECT frp.* " +
                            "FROM fall_risk_profiles frp " +
                            "LEFT JOIN fall_risk_profile_removals frp_rem ON frp.id = frp_rem.fall_risk_profile_id " +
                            "WHERE frp_rem.fall_risk_profile_id IS NULL and frp.id = ?1",
            nativeQuery = true
    )
    Optional<FallRiskProfile> findById(Long id);

    @Query(
            value =
                    "SELECT frp.* " +
                            "FROM fall_risk_profiles frp " +
                            "LEFT JOIN fall_risk_profile_removals frp_rem ON frp.id = frp_rem.fall_risk_profile_id " +
                            "WHERE frp_rem.fall_risk_profile_id IS NULL",
            nativeQuery = true
    )
    List<FallRiskProfile> findAll();

    @Query(
            value =
                    "SELECT frp.* " +
                            "FROM fall_risk_profiles frp " +
                            "JOIN fall_risk_profile_removals frp_rem ON frp.id = frp_rem.fall_risk_profile_id " +
                            "WHERE frp_rem.fall_risk_profile_id IS NOT NULL",
            nativeQuery = true
    )
    List<FallRiskProfile> findAllRemoved();
}
