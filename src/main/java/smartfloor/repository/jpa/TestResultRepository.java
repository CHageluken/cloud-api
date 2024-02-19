package smartfloor.repository.jpa;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;
import smartfloor.domain.entities.rehabilitation.TestResult;
import smartfloor.domain.entities.rehabilitation.TestType;

@Repository
public interface TestResultRepository extends SoftDeleteRepository<TestResult, Long> {
    List<TestResult> findAllByUserIdAndBeginTimeGreaterThanEqualAndEndTimeLessThanEqual(
            Long id,
            LocalDateTime beginTime,
            LocalDateTime endTime
    );

    List<TestResult> findByUserIdIn(List<Long> userIds);

    List<TestResult> findAllByUserIdAndBeginTimeGreaterThanEqualAndEndTimeLessThanEqualAndDeleted(
            Long id,
            LocalDateTime beginTime,
            LocalDateTime endTime,
            boolean deleted
    );

    List<TestResult> findAllByUserIdAndTypeAndBeginTimeGreaterThanEqualAndEndTimeLessThanEqualAndDeleted(
            Long id,
            TestType type,
            LocalDateTime beginTime,
            LocalDateTime endTime,
            boolean deleted
    );

    List<TestResult> findByUserIdInAndDeleted(List<Long> userIds, boolean deleted);

    List<TestResult> findAllByDeleted(boolean deleted);
}
