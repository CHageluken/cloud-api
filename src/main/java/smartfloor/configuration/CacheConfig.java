package smartfloor.configuration;

import java.util.List;
import java.util.Objects;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableCaching
public class CacheConfig {

    private static final String LATEST_FALL_RISK_ASSESSMENTS_CACHE_NAME = "latestFallRiskAssessments";

    /**
     * TODO.
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        ConcurrentMapCache latestFallRiskAssessmentsCache =
                new ConcurrentMapCache(LATEST_FALL_RISK_ASSESSMENTS_CACHE_NAME);
        cacheManager.setCaches(List.of(latestFallRiskAssessmentsCache));
        return cacheManager;
    }

    /**
     * Clears the latest fall risk assessments cache periodically to remove no longer relevant entries.
     */
    @Scheduled(cron = "0 0 */3 * * *")
    public void clearLatestFallRiskAssessmentsCache() {
        Objects.requireNonNull(cacheManager().getCache(LATEST_FALL_RISK_ASSESSMENTS_CACHE_NAME)).clear();
    }

}
