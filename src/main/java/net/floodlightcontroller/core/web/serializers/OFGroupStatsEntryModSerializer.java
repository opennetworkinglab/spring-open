package net.floodlightcontroller.core.web.serializers;
import java.io.IOException;
import java.util.List;

import net.floodlightcontroller.core.web.OFGroupStatsEntryMod;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;
import org.projectfloodlight.openflow.protocol.OFBucketCounter;
import org.projectfloodlight.openflow.protocol.OFGroupStatsEntry;

public class OFGroupStatsEntryModSerializer extends SerializerBase<OFGroupStatsEntryMod>{
    
    protected OFGroupStatsEntryModSerializer(){
        super(OFGroupStatsEntryMod.class);
    }

    @Override
    public void serialize(OFGroupStatsEntryMod groupStatsEntryMod, JsonGenerator jGen,
            SerializerProvider sp) throws IOException,
            JsonGenerationException {
        OFGroupStatsEntry groupStatsModEntry = groupStatsEntryMod.getGroupStatsEntry();
        List<OFBucketCounter> bucketCounters = groupStatsModEntry.getBucketStats();
        jGen.writeStartObject();
        jGen.writeNumberField("groupId", groupStatsModEntry.getGroup().getGroupNumber());
        jGen.writeNumberField("packetCount", groupStatsModEntry.getPacketCount().getValue());
        jGen.writeNumberField("byteCount", groupStatsModEntry.getByteCount().getValue());
        jGen.writeNumberField("durationNsec", groupStatsModEntry.getDurationNsec());
        jGen.writeNumberField("durationSec", groupStatsModEntry.getDurationSec());
        jGen.writeArrayFieldStart("bucketStats");
        for (OFBucketCounter bucketCouter : bucketCounters){
            jGen.writeStartObject();
            jGen.writeNumberField("pktCount", bucketCouter.getPacketCount().getValue());
            jGen.writeNumberField("byteCount", bucketCouter.getByteCount().getValue());
            jGen.writeEndObject();
        }
        jGen.writeEndArray();
        jGen.writeEndObject();
    }

}
