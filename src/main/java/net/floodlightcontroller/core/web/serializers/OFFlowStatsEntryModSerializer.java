package net.floodlightcontroller.core.web.serializers;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.floodlightcontroller.core.web.OFFlowStatsEntryMod;

import org.projectfloodlight.openflow.protocol.action.*;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;
import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFFlowModFlags;
import org.projectfloodlight.openflow.protocol.OFFlowStatsEntry;
import org.projectfloodlight.openflow.protocol.OFInstructionType;
import org.projectfloodlight.openflow.protocol.OFMatchV3;
import org.projectfloodlight.openflow.protocol.OFOxmList;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.instruction.*;
import org.projectfloodlight.openflow.protocol.match.MatchFields;
import org.projectfloodlight.openflow.protocol.oxm.OFOxm;
import org.projectfloodlight.openflow.protocol.ver13.OFInstructionTypeSerializerVer13;

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
        
        List<OFInstruction> instructions = flowStatsEntry.getInstructions();
        Set<OFFlowModFlags> flags = flowStatsEntry.getFlags();
        jGen.writeStartObject();
        
        jGen.writeNumberField("byteCount", flowStatsEntry.getByteCount().getValue());
        jGen.writeNumberField("pktCount", flowStatsEntry.getPacketCount().getValue());
        jGen.writeNumberField("priority", flowStatsEntry.getPriority());
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
        jGen.writeStartObject();
        Iterator<OFOxm<?>> match= matches.iterator();
        while(match.hasNext()){
            OFOxm<?> matchGeneric = match.next();
            //jGen.writeObjectField(matchGeneric.getMatchField().id.toString(), matchGeneric.getValue().toString());
            if (matchGeneric.getMatchField().id == MatchFields.IPV4_DST){
                jGen.writeStringField("networkDestination", matchGeneric.getValue().toString());
            }
            else if (matchGeneric.getMatchField().id == MatchFields.IPV4_SRC){
                jGen.writeStringField("networkSource", matchGeneric.getValue().toString());
            }
            else if (matchGeneric.getMatchField().id == MatchFields.ETH_DST){
                jGen.writeStringField("dataLayerDestination", matchGeneric.getValue().toString());
            }
            else if (matchGeneric.getMatchField().id == MatchFields.ETH_SRC){
                jGen.writeStringField("dataLayerSource", matchGeneric.getValue().toString());
            }
            else if (matchGeneric.getMatchField().id == MatchFields.ETH_TYPE){
                jGen.writeStringField("dataLayerType", "0x"+matchGeneric.getValue().toString());
            }
            else if (matchGeneric.getMatchField().id == MatchFields.IN_PORT){
                jGen.writeNumberField("inputPort", Integer.parseInt(matchGeneric.getValue().toString()));
            }
            else if (matchGeneric.getMatchField().id == MatchFields.MPLS_TC){
                jGen.writeNumberField("mplsTc", Integer.parseInt(matchGeneric.getValue().toString()));
            }
            else if (matchGeneric.getMatchField().id == MatchFields.MPLS_LABEL){
                jGen.writeNumberField("mplsLabel", Integer.parseInt(matchGeneric.getValue().toString()));
            }
            else if (matchGeneric.getMatchField().id == MatchFields.IP_PROTO){
                jGen.writeNumberField("networkProtocol", Integer.parseInt(matchGeneric.getValue().toString())); 
            }
            //TODO: Ask Saurav about the implementation of tcp and udp.
            else if (matchGeneric.getMatchField().id == MatchFields.TCP_DST || matchGeneric.getMatchField().id == MatchFields.UDP_DST){
                jGen.writeNumberField("transportDestination", Integer.parseInt(matchGeneric.getValue().toString()));
            }
            else if (matchGeneric.getMatchField().id == MatchFields.TCP_SRC || matchGeneric.getMatchField().id == MatchFields.UDP_SRC){
                jGen.writeNumberField("transportSource", Integer.parseInt(matchGeneric.getValue().toString()));
            }
        }
        jGen.writeEndObject();
        jGen.writeEndArray();

        /*\jGen.writeArrayFieldStart("instructions");
        jGen.writeStartObject();
        for (OFInstruction instruction: instructions){
            jGen.writeObjectField("instructionType", instruction.getType().name());
            jGen.writeArrayFieldStart("actions");
            jGen.writeStartObject();
            //OFInstructionApplyActions newInstruction = instruction.getClass().cast(instruction);
            //instruction = ;
            //instruction.g
            if (instruction.getType().equals(OFInstructionType.APPLY_ACTIONS)){
                //import org.projectfloodlight.openflow.protocol.instruction.OFInstructionApplyActions
                List<OFAction> actions = ((OFInstructionApplyActions) instruction).getActions();
                for (OFAction action : actions){
                    a
                }
            }
            else if (instruction.getType().equals(OFInstructionType.APPLY_ACTIONS)){
                List<OFAction> actions = ((OFInstructionApplyActions) instruction).getActions();
            }
            jGen.writeEndObject();
            jGen.writeEndArray();
        }
        jGen.writeEndObject();
        jGen.writeEndArray();*/
        jGen.writeEndObject();

        
        //for (OFAction action : actions){
            /*if(action.getType().compareTo(OFActionType.SET_FIELD) == 0){
                /*
                 * TODO: 1-Need better if condition.
                 * TODO: 2-Complete REST response. (Right now we are only sending what 
                 * SegmentRouter CLI needs).
                 */
                /*if (((OFActionSetField)action).getField().toString().contains("OFOxmEthSrcVer13")){
                    jGen.writeStringField("SET_DL_SRC", ((OFActionSetField)action).getField().getValue().toString());
                }
                else if (((OFActionSetField)action).getField().toString().contains("OFOxmEthDstVer13")){
                    jGen.writeStringField("SET_DL_DST", ((OFActionSetField)action).getField().getValue().toString());
                }
                System.out
            }
            else if(action.getType().compareTo(OFActionType.OUTPUT) == 0){
                jGen.writeNumberField("OUTPPUT", ((OFActionOutput)action).getPort().getPortNumber());
            }*/
        //}
        //jGen.writeEndObject();

    }
    

}
