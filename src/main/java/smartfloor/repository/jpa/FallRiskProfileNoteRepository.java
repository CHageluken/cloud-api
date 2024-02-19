package smartfloor.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import smartfloor.domain.entities.fall.risk.profile.FallRiskProfileNote;

@Repository
public interface FallRiskProfileNoteRepository extends JpaRepository<FallRiskProfileNote, Long> {
}