package net.onrc.onos.core.util.serializers;

import java.io.IOException;

import net.onrc.onos.core.util.Dpid;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deserialize a DPID from a string.
 */
public class DpidDeserializer extends JsonDeserializer<Dpid> {

    private static final Logger log = LoggerFactory.getLogger(DpidDeserializer.class);

    @Override
    public Dpid deserialize(JsonParser jp,
                            DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

        Dpid dpid = null;

        jp.nextToken();        // Move to JsonToken.START_OBJECT
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            String fieldname = jp.getCurrentName();
            if ("value".equals(fieldname)) {
                String value = jp.getText();
                log.debug("Fieldname: {} Value: {}", fieldname, value);
                dpid = new Dpid(value);
            }
        }
        return dpid;
    }
}
