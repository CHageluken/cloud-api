package smartfloor.domain.parameters;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Parameter<V> {
    private V value = null;

    public abstract String getName();

    public abstract String getUnit();

    protected void setValue(V value) {
        this.value = value;
    }

    public V getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return "Parameter{" +
                "value=" + value +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Parameter<?> parameter = (Parameter<?>) o;
        return Objects.equals(value, parameter.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
