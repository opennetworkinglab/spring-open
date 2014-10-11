package net.floodlightcontroller.core.web.serializers;
import java.io.IOException;
import java.util.List;

import net.floodlightcontroller.core.web.OFGroupDescStatsEntryMod;

import org.apache.commons.codec.binary.Hex;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;
import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFBucket;
import org.projectfloodlight.openflow.protocol.OFGroupDescStatsEntry;
import org.projectfloodlight.openflow.protocol.action.*;
public class OFGroupDescStatsEntryModSerializer extends SerializerBase<OFGroupDescStatsEntryMod>{



    protected OFGroupDescStatsEntryModSerializer(){
        super(OFGroupDescStatsEntryMod.class);
    }
//TODO:- Java-doc

    @Override
    public void serialize(OFGroupDescStatsEntryMod groupDescStatsModEntry, JsonGenerator jGen,
            SerializerProvider sp) throws IOException,
            JsonGenerationException {
        OFGroupDescStatsEntry groupDescStatsEntryMod = groupDescStatsModEntry.getGroupDescStatsEntry();
        List<OFBucket> Buckets = groupDescStatsEntryMod.getBuckets();
        jGen.writeStartObject();
        jGen.writeNumberField("groupId", groupDescStatsEntryMod.getGroup().getGroupNumber());
        jGen.writeStringField("groupType", groupDescStatsEntryMod.getGroupType().name());
        jGen.writeArrayFieldStart("bucketsActions");
        for (OFBucket bucket : Buckets){
            jGen.writeStartObject();
            List<OFAction> actions = bucket.getActions();
            for (OFAction action : actions ){
                if(action.getType().compareTo(OFActionType.SET_FIELD) == 0){
                    /*
                     * TODO: 1-Need better if condition.
                     * TODO: 2-Complete REST response. (Right now we are only sending what 
                     * SegmentRouter CLI needs).
                     */
                    if (((OFActionSetField)action).getField().toString().contains("OFOxmEthSrcVer13")){
                        jGen.writeStringField("SET_DL_SRC", ((OFActionSetField)action).getField().getValue().toString());
                    }
                    else if (((OFActionSetField)action).getField().toString().contains("OFOxmEthDstVer13")){
                        jGen.writeStringField("SET_DL_DST", ((OFActionSetField)action).getField().getValue().toString());
                    }
                    else if (((OFActionSetField)action).getField().toString().contains("OFOxmMplsLabelVer13")){
                        jGen.writeNumberField("PUSH_MPLS", 
                                Integer.decode(((OFActionSetField)action).getField().getValue().toString()));
                    }
                }
                else if(action.getType().compareTo(OFActionType.OUTPUT) == 0){
                    jGen.writeNumberField("OUTPPUT", ((OFActionOutput)action).getPort().getPortNumber());
                }
                else if(action.getType().compareTo(OFActionType.POP_MPLS) == 0){
                    jGen.writeStringField("POP_MPLS", "True");
                }
                
            }
            jGen.writeEndObject();
        }
        jGen.writeEndArray();
        jGen.writeEndObject();
        
    }

}
