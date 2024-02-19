package smartfloor.repository.jpa;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import smartfloor.domain.entities.Application;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.entities.User;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    @Query("SELECT a FROM Application a WHERE ?1 member of a.users ORDER BY a.priority")
    List<Application> getApplicationsByUser(User user);

    @Query("SELECT a FROM Application a WHERE ?1 member of a.tenants ORDER BY a.priority")
    List<Application> getApplicationsByTenant(Tenant tenant);
}
