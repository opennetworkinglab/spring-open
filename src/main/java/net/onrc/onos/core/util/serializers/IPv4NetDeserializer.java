package net.onrc.onos.core.util.serializers;

import java.io.IOException;

import net.onrc.onos.core.util.IPv4Net;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deserialize an IPv4Net address from a string.
 */
public class IPv4NetDeserializer extends JsonDeserializer<IPv4Net> {

    private static final Logger log = LoggerFactory.getLogger(IPv4NetDeserializer.class);

    @Override
    public IPv4Net deserialize(JsonParser jp,
                               DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

        IPv4Net ipv4Net = null;

        jp.nextToken();        // Move to JsonToken.START_OBJECT
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            String fieldname = jp.getCurrentName();
            if ("value".equals(fieldname)) {
                String value = jp.getText();
                log.debug("Fieldname: {} Value: {}", fieldname, value);
                ipv4Net = new IPv4Net(value);
            }
        }
        return ipv4Net;
    }
}
