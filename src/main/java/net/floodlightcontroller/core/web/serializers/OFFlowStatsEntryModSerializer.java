package net.floodlightcontroller.core.web.serializers;

import java.io.IOException;

import net.floodlightcontroller.core.web.OFFlowStatsEntryMod;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;

public class OFFlowStatsEntryModSerializer extends SerializerBase<OFFlowStatsEntryMod> {
    
    protected OFFlowStatsEntryModSerializer(){
        super(OFFlowStatsEntryMod.class);
    }

    @Override
    public void serialize(OFFlowStatsEntryMod FlowStatsEntryMod, JsonGenerator jGen,
            SerializerProvider sp) throws IOException,
            JsonGenerationException {
        
    }

}
