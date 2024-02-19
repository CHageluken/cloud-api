package smartfloor.repository.jpa;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.entities.User;

public interface GroupRepository extends JpaRepository<Group, Long> {
    @Override
    Optional<Group> findById(Long id);

    Optional<Group> findByName(String name);

    boolean existsByIdNotAndTenantAndName(Long id, Tenant tenant, String name);

    boolean existsByTenantAndName(Tenant tenant, String name);

    @Query("SELECT g FROM Group g WHERE ?1 member of g.managers")
    List<Group> findGroupsByManager(User manager);

    @Query("SELECT g FROM Group g WHERE ?1 member of g.users")
    List<Group> findGroupsByUser(User authenticatedUser);

    @Query(
            value = "SELECT * FROM groups " +
                    "INNER JOIN group_users " +
                    "ON group_users.group_id = groups.id " +
                    "WHERE group_users.user_id = :userId",
            nativeQuery = true
    )
    List<Group> findGroupsForUserId(Long userId);

    /**
     * Acquire a pessimistic write lock on the group with the given identifier and return it.
     * This is used to prevent concurrent updates on the same group.
     * Added first as part of VIT-699.
     *
     * @param id of the group to lock and retrieve
     * @return the group with the given identifier
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "10000")})
    Optional<Group> findWithLockingById(Long id);

}
