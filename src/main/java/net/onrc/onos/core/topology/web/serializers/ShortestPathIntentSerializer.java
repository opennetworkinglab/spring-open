package net.onrc.onos.core.topology.web.serializers;


import com.google.common.net.InetAddresses;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.intent.ShortestPathIntent;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;
import org.openflow.util.HexString;

import java.io.IOException;

/**
 * JSON serializer for ShortestPathIntents.
 */
public class ShortestPathIntentSerializer extends SerializerBase<ShortestPathIntent> {

    /**
     * Public constructor - just calls its super class constructor.
     */
    public ShortestPathIntentSerializer() {
        super(ShortestPathIntent.class);
    }

    /**
     * Converts an integer into a string representing an IP address.
     *
     * @param ipAddress integer representation of the address
     * @return string that represents the address
     */
    private String toIPAddressString(final int ipAddress) {
        return InetAddresses.fromInteger(ipAddress).getHostAddress();
    }

    /**
     * Serializes a ShortestPathIntent object.
     *
     * @param intent Intent to serialize
     * @param jsonGenerator generator to add the serialized object to
     * @param serializerProvider not used
     * @throws IOException if the JSON serialization fails
     */
    @Override
    public void serialize(final ShortestPathIntent intent,
                          final JsonGenerator jsonGenerator,
                          final SerializerProvider serializerProvider)
           throws IOException {
        jsonGenerator.writeStartObject();

        jsonGenerator.writeStringField("id", intent.getId());
        jsonGenerator.writeStringField("state", intent.getState().toString());
        jsonGenerator.writeStringField("pathFrozen",
                                       Boolean.toString(intent.isPathFrozen()));

        jsonGenerator.writeStringField("srcSwitchDpid",
                                       HexString.toHexString(intent.getSrcSwitchDpid()));
        jsonGenerator.writeStringField("srcPortNumber",
                                       Long.toString(intent.getSrcPortNumber()));
        jsonGenerator.writeStringField("srcMac",
                MACAddress.valueOf(intent.getSrcMac()).toString());
        jsonGenerator.writeStringField("srcIp",
                                       toIPAddressString(intent.getSrcIp()));

        jsonGenerator.writeStringField("dstSwitchDpid",
                HexString.toHexString(intent.getDstSwitchDpid()));
        jsonGenerator.writeStringField("dstPortNumber",
                                       Long.toString(intent.getDstPortNumber()));
        jsonGenerator.writeStringField("dstMac",
                MACAddress.valueOf(intent.getDstMac()).toString());
        jsonGenerator.writeStringField("dstIp",
                                       toIPAddressString(intent.getDstIp()));

        jsonGenerator.writeStringField("idleTimeout",
                                       Integer.toString(intent.getIdleTimeout()));
        jsonGenerator.writeStringField("hardTimeout",
                                       Integer.toString(intent.getHardTimeout()));
        jsonGenerator.writeStringField("firstSwitchIdleTimeout",
                                       Integer.toString(intent.getFirstSwitchIdleTimeout()));
        jsonGenerator.writeStringField("firstSwitchHardTimeout",
                                       Integer.toString(intent.getFirstSwitchHardTimeout()));

        jsonGenerator.writeArrayFieldStart("logs");
        for (final String log : intent.getLogs()) {
            jsonGenerator.writeObject(log);
        }
        jsonGenerator.writeEndArray();

        jsonGenerator.writeEndObject();
    }
}
