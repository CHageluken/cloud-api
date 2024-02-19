package smartfloor.domain.parameters;

import java.util.List;
import smartfloor.domain.entities.Footstep;

public abstract class FootstepParameter<V> extends Parameter<V> {
    protected FootstepParameter(List<Footstep> footsteps) {
        this.setValue(this.compute(footsteps));
    }

    protected abstract V compute(List<Footstep> footsteps);

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
