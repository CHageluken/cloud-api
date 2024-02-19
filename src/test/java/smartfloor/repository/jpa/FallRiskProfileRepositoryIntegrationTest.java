package smartfloor.repository.jpa;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import smartfloor.IntegrationTestBase;
import smartfloor.domain.TimeWindow;
import smartfloor.domain.entities.Floor;
import smartfloor.domain.entities.Group;
import smartfloor.domain.entities.Tenant;
import smartfloor.domain.entities.Wearable;
import smartfloor.domain.entities.WearableGroup;
import smartfloor.domain.entities.fall.risk.profile.FallRiskProfile;

class FallRiskProfileRepositoryIntegrationTest extends IntegrationTestBase {

    @Autowired
    WearableRepository wearableRepository;

    @Autowired
    FloorRepository floorRepository;

    @Autowired
    FallRiskProfileRepository fallRiskProfileRepository;

    @Autowired
    WearableGroupRepository wearableGroupRepository;

    @Autowired
    GroupRepository groupRepository;


    @Test
    void testFindByWearableIdAndCreationTimeBetweenOrderByCreationTime() {
        // given
        LocalDateTime currentTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        Tenant tenant = getTestTenant();
        Wearable wearable = Wearable.builder().id("test_heelable").build();
        wearable = wearableRepository.save(wearable);
        WearableGroup wearableGroup = WearableGroup.builder()
                .wearables(List.of(wearable))
                .name("testFindByWearableIdAndCreationTimeBetweenOrderByCreationTime")
                .build();
        wearableGroup = wearableGroupRepository.save(wearableGroup);
        Group group = Group.builder()
                .name("testFindByWearableIdAndCreationTimeBetweenOrderByCreationTime")
                .wearableGroup(wearableGroup)
                .tenant(tenant)
                .build();
        groupRepository.save(group);
        Floor floor = Floor.builder().name("FRP test floor").build();
        floor = floorRepository.save(floor);
        TimeWindow timeWindow = new TimeWindow(currentTime.minusMinutes(5), currentTime);
        FallRiskProfile frp = FallRiskProfile.builder()
                .wearable(wearable)
                .floor(floor)
                .creationTime(timeWindow.getBeginTime())
                .beginTime(timeWindow.getBeginTime())
                .endTime(timeWindow.getEndTime())
                .walkingSpeed(0.5)
                .stepLength(0.5)
                .stepFrequency(0.5)
                .build();
        // when
        fallRiskProfileRepository.save(frp);
        // then
        List<FallRiskProfile> foundFallRiskProfiles =
                fallRiskProfileRepository.findByWearableIdAndCreationTimeBetweenAndHiddenFalseOrderByCreationTime(
                        wearable.getId(),
                        timeWindow.getBeginTime(),
                        timeWindow.getEndTime()
                );
        assertTrue(foundFallRiskProfiles.contains(frp));
    }

}
