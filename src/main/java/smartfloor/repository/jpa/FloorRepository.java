package smartfloor.repository.jpa;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import smartfloor.domain.entities.Floor;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.User;

@Repository
public interface FloorRepository extends JpaRepository<Floor, Long> {

    @Query("SELECT f FROM Floor f WHERE ?1 member of f.viewers")
    List<Floor> findByViewer(User viewer);

    @Query("SELECT f FROM Floor f WHERE ?1 member of f.groups")
    List<Floor> findByGroup(Group group);

    Optional<Floor> findByName(String name);
}
