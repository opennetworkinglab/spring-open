package net.floodlightcontroller.core.web.serializers;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import net.floodlightcontroller.core.web.OFFlowStatsEntryMod;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;
import org.projectfloodlight.openflow.protocol.OFFlowModFlags;
import org.projectfloodlight.openflow.protocol.OFFlowStatsEntry;
import org.projectfloodlight.openflow.protocol.OFMatchV3;
import org.projectfloodlight.openflow.protocol.OFOxmList;
import org.projectfloodlight.openflow.protocol.match.MatchFields;
import org.projectfloodlight.openflow.protocol.oxm.OFOxm;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmIpv4DstMasked;

public class OFFlowStatsEntryModSerializer extends SerializerBase<OFFlowStatsEntryMod> {
    
    protected OFFlowStatsEntryModSerializer(){
        super(OFFlowStatsEntryMod.class);
    }

    @Override
    public void serialize(OFFlowStatsEntryMod FlowStatsEntryMod, JsonGenerator jGen,
            SerializerProvider sp) throws IOException,
            JsonGenerationException {
        OFFlowStatsEntry flowStatsEntry = FlowStatsEntryMod.getFlowStatsEntry();
        OFOxmList matches = ((OFMatchV3)flowStatsEntry.getMatch()).getOxmList();
        Set<OFFlowModFlags> flags = flowStatsEntry.getFlags();
        jGen.writeStartObject();
        
        jGen.writeNumberField("byteCount", flowStatsEntry.getByteCount().getValue());
        jGen.writeNumberField("cookie", flowStatsEntry.getCookie().getValue());
        jGen.writeNumberField("durationNsec", flowStatsEntry.getDurationNsec());
        jGen.writeNumberField("durationSec", flowStatsEntry.getDurationSec());
        jGen.writeArrayFieldStart("flags");
        for (OFFlowModFlags flag: flags){
            jGen.writeNullField(flag.name());;
        }
        jGen.writeEndArray();
        jGen.writeNumberField("hardTimeout", flowStatsEntry.getHardTimeout());
        jGen.writeNumberField("idleTimeout", flowStatsEntry.getIdleTimeout());
        jGen.writeArrayFieldStart("match");
        //jGen.writeStartObject();
        Iterator<OFOxm<?>> match= matches.iterator();
        while(match.hasNext()){
            OFOxm<?> matchGeneric = match.next();
            if (matchGeneric.getMatchField().id == MatchFields.IPV4_DST){
                
                //jGen.writeObjectField("networkDestination", ((OFOxmIpv4DstVer13) matchGeneric).getValue());
            }
            
            
        }
        jGen.writeEndArray();

        jGen.writeEndObject();
    }
    

}
