package smartfloor.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import smartfloor.domain.entities.Wearable;

@Repository
public interface WearableRepository extends JpaRepository<Wearable, Long> {
    Wearable findById(String id);
}
