package smartfloor.domain.tests;

import java.time.LocalDateTime;
import java.util.List;
import smartfloor.domain.entities.Footstep;
import smartfloor.domain.indicators.Indicator;

public interface Trial {
    LocalDateTime getBeginTime();

    LocalDateTime getEndTime();

    List<Footstep> getFootsteps();

    List<Indicator> getIndicators();

    Indicator getIndicatorByName(String name);
}
