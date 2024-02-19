package smartfloor.repository.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * Extends the JpaRepository, providing additional soft deletion related definitions.
 * Must be in turn extended only by repositories which require soft deletion support.
 */
@NoRepositoryBean
public interface SoftDeleteRepository<T, ID> extends JpaRepository<T, ID> {
    @Modifying
    @Transactional
    @Query(value = "UPDATE #{#entityName} SET deleted = TRUE WHERE id = ?1")
    void softDelete(ID id);

    Optional<T> findByIdAndDeleted(ID id, boolean deleted);
}
