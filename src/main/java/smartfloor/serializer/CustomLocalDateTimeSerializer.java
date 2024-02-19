package smartfloor.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
public class CustomLocalDateTimeSerializer extends StdSerializer<LocalDateTime> {

    private CustomLocalDateTimeSerializer(Class<LocalDateTime> t) {
        super(t);
    }

    protected CustomLocalDateTimeSerializer() {
        this(null);
    }

    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider sp)
            throws IOException {
        Long epoch = value.atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        gen.writeNumber(epoch);
    }
}

