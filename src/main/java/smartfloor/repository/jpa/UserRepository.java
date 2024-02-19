package smartfloor.repository.jpa;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import smartfloor.domain.entities.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Look up user by authentication identifier (Cognito UUID), we cache this as looking up the authenticated user
     * is pretty frequently used throughout multiple API (route) calls.
     *
     * @param authId authentication identifier (Cognito UUID)
     * @return Optional user object matching the authentication identifier
     */
    Optional<User> findByAuthId(String authId);

    List<User> findByCompositeUserId(Long compositeUserId);

    /**
     * This method verifies whether a given user manages the (user) group with the given identifier.
     *
     * @param managerId the user id of the user for which their management of a group is to be verified
     * @param groupId   the group id of the group for which it is to be verified that the user manages it
     * @return whether the user manages this group
     */
    @Query(
            "SELECT CASE " +
                    "WHEN COUNT(g) > 0 THEN 'TRUE' " +
                    "ELSE 'FALSE' " +
                    "END " +
                    "FROM User u JOIN u.managedGroups g " +
                    "WHERE u.id = :managerId AND g.id = :groupId"
    )
    Boolean isManagingGroup(
            @Param("managerId") Long managerId,
            @Param("groupId") Long groupId
    );

    /**
     * This method verifies whether a given user manages the user with the given identifier.
     * This is done by verifying the transitive relationship to the other user, through the user managing at least one
     * group having that particular user as its member.
     *
     * @param managerId the user id of the group manager for which management of the user is to be verified
     * @param userId    the user id of the user for which access is requested
     * @return whether the group manager is managing the user
     */
    @Query(
            "SELECT CASE " +
                    "WHEN COUNT(u) > 0 THEN true " +
                    "ELSE false " +
                    "END " +
                    "FROM User u " +
                    "JOIN u.managedGroups g " +
                    "JOIN g.users gu " +
                    "WHERE u.id = :managerId AND gu.id = :userId"
    )
    Boolean isManagingUser(
            @Param("managerId") Long managerId,
            @Param("userId") Long userId
    );

    /**
     * This method verifies whether a given user is allowed access to a certain wearable.
     * This is done by verifying the transitive access relationship to the wearable, through the user managing at least
     * one group that is linked to a wearable group having that particular wearable as its member.
     *
     * @param managerId  the user id of the group manager for which access to the wearable is to be verified
     * @param wearableId the wearable id of the wearable for which access is requested
     * @return whether access to the wearable is allowed for this user
     */
    @Query(
            "SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
                    "FROM User u JOIN u.managedGroups g JOIN g.wearableGroup wg JOIN wg.wearables w " +
                    "WHERE u.id = :managerId AND w.id = :wearableId"
    )
    Boolean isManagerAllowedToAccessWearable(
            @Param("managerId") Long managerId,
            @Param("wearableId") String wearableId
    );

    /**
     * This method verifies whether a given user is allowed access to a certain wearable.
     * This is done by verifying the transitive access relationship to the wearable, through the user participating in
     * at least one group that is linked to a wearable group having that particular wearable as its member.
     *
     * @param userId     the user id of the group member for which access to the wearable is to be verified
     * @param wearableId the wearable id of the wearable for which access is requested
     * @return whether access to the wearable is allowed for this user
     */
    @Query(
            "SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
                    "FROM User u JOIN u.groups g JOIN g.wearableGroup wg JOIN wg.wearables w " +
                    "WHERE u.id = :userId AND w.id = :wearableId"
    )
    Boolean isUserAllowedToAccessWearable(@Param("userId") Long userId, @Param("wearableId") String wearableId);

}
