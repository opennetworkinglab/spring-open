package net.floodlightcontroller.core.web.serializers;
import java.io.IOException;
import java.util.List;

import net.floodlightcontroller.core.web.OFGroupDescStatsEntryMod;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;
import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFBucket;
import org.projectfloodlight.openflow.protocol.OFGroupDescStatsEntry;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.*;
public class OFGroupDescStatsEntryModSerializer extends SerializerBase<OFGroupDescStatsEntryMod>{



    protected OFGroupDescStatsEntryModSerializer(){
        super(OFGroupDescStatsEntryMod.class);
    }

    @Override
    public void serialize(OFGroupDescStatsEntryMod GroupDescStatsModEntry, JsonGenerator jGen,
            SerializerProvider sp) throws IOException,
            JsonGenerationException {
        OFGroupDescStatsEntry GroupStatsEntryMod = GroupDescStatsModEntry.getGroupDescStatsEntry();
        List<OFBucket> Buckets = GroupStatsEntryMod.getBuckets();
        jGen.writeStartObject();
        jGen.writeNumberField("groupId", GroupStatsEntryMod.getGroup().getGroupNumber());
        jGen.writeStringField("groupType", GroupStatsEntryMod.getGroupType().name());
        for (OFBucket bucket : Buckets){
            List<OFAction> actions = bucket.getActions();
            for (OFAction action : actions ){
                if(action.getType().compareTo(OFActionType.SET_FIELD) == 0){
                    //TODO: Need better if condition.
                    if (((OFActionSetField)action).getField().toString().contains("OFOxmEthSrcVer13")){
                        jGen.writeStringField("SET_DL_SRC", ((OFActionSetField)action).getField().getValue().toString());
                    }
                    else if (((OFActionSetField)action).getField().toString().contains("OFOxmEthDstVer13")){
                        jGen.writeStringField("SET_DL_DST", ((OFActionSetField)action).getField().getValue().toString());
                    }
                }
                else if(action.getType().compareTo(OFActionType.OUTPUT) == 0){
                    jGen.writeNumberField("OUTPPUT", ((OFActionOutput)action).getPort().getPortNumber());
                }
            }
        }
        jGen.writeEndObject();
        
    }

}
