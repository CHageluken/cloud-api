package smartfloor.repository.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import smartfloor.domain.entities.CompositeUser;

@Repository
public interface CompositeUserRepository extends JpaRepository<CompositeUser, Long> {

    Optional<CompositeUser> findByAuthId(String authId);

    Optional<CompositeUser> findById(Long id);

    /**
     * Verifies whether a composite user currently has access to a wearable by checking if at least one of their
     * sub-users is participating in a group that is linked to a wearable group having that particular
     * wearable as its member.
     *
     * @param compositeUserId the id of the composite user for which access to the wearable is to be verified
     * @param wearableId      the id of the wearable, access to which is to be verified
     * @return whether access to the wearable is granted
     */
    @Query(
            "SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
                    "FROM User u JOIN u.groups g JOIN g.wearableGroup wg JOIN wg.wearables w " +
                    "WHERE u.compositeUser.id = :compositeUserId AND w.id = :wearableId"
    )
    Boolean isCUAllowedToAccessWearable(
            @Param("compositeUserId") Long compositeUserId,
            @Param("wearableId") String wearableId
    );
}
