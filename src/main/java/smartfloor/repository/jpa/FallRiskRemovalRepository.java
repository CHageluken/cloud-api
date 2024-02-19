package smartfloor.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import smartfloor.domain.entities.fall.risk.profile.FallRiskProfileRemoval;

@Repository
public interface FallRiskRemovalRepository extends JpaRepository<FallRiskProfileRemoval, Long> {
}