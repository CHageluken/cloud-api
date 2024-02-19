package smartfloor.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Taken from: https://stackoverflow.com/questions/54479704/serialize-java8-localdatetime-to-utc-timestamp-using-jackson.
 */
public class CustomLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {

    protected CustomLocalDateTimeDeserializer() {
        this(null);
    }

    private CustomLocalDateTimeDeserializer(Class<LocalDateTime> t) {
        super(t);
    }

    @Override
    public LocalDateTime deserialize(JsonParser jsonparser, DeserializationContext context)
            throws IOException {
        long timestamp = jsonparser.getNumberValue().longValue();
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC);
    }
}
