package smartfloor.domain.entities.user.info.history;

import java.util.Collections;
import java.util.List;

/**
 * A list of genders currently supported by the system.
 */
public final class GenderConstants {
    public static final List<String> SUPPORTED_GENDERS = Collections.unmodifiableList(
            List.of("m", "f", "x", "")
    );

    private GenderConstants() {
    }
}
