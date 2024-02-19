package smartfloor.repository.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import smartfloor.domain.entities.WearableGroup;

@Repository
public interface WearableGroupRepository extends JpaRepository<WearableGroup, Long> {
    Optional<WearableGroup> findByName(String name);
}
