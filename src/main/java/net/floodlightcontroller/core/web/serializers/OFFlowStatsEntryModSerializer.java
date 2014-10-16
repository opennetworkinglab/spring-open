package net.floodlightcontroller.core.web.serializers;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.floodlightcontroller.core.web.OFFlowStatsEntryMod;
import net.onrc.onos.core.packet.IPv4;

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
        jGen.writeStartObject();
        jGen.writeNumberField("byteCount", flowStatsEntry.getByteCount().getValue());
        jGen.writeNumberField("packetCount", flowStatsEntry.getPacketCount().getValue());
        jGen.writeNumberField("priority", flowStatsEntry.getPriority());
        jGen.writeNumberField("cookie", flowStatsEntry.getCookie().getValue());
        jGen.writeNumberField("durationNsec", flowStatsEntry.getDurationNsec());
        jGen.writeNumberField("durationSec", flowStatsEntry.getDurationSec());
        jGen.writeObjectField("flags", flowStatsEntry.getFlags());
        jGen.writeNumberField("hardTimeout", flowStatsEntry.getHardTimeout());
        jGen.writeNumberField("idleTimeout", flowStatsEntry.getIdleTimeout());
        jGen.writeFieldName("match");
        jGen.writeStartObject();
        Iterator<OFOxm<?>> match= matches.iterator();
        while(match.hasNext()){
            OFOxm<?> matchGeneric = match.next();
            if (matchGeneric.getMatchField().id == MatchFields.IPV4_DST){
                jGen.writeStringField("networkDestination", matchGeneric.getValue().toString()
                        +"/"
                        +(matchGeneric.isMasked() ?
                                OFFlowStatsEntryModSerializer.covertToMask(
                                        IPv4.toIPv4Address(
                                                matchGeneric.getMask().toString())):"32"));
            }
            else if (matchGeneric.getMatchField().id == MatchFields.IPV4_SRC){
                jGen.writeStringField("networkSource", matchGeneric.getValue().toString()
                        +"/"
                        +(matchGeneric.isMasked() ?
                                OFFlowStatsEntryModSerializer.covertToMask(
                                        IPv4.toIPv4Address(
                                                matchGeneric.getMask().toString())):"32"));
            }
            else if (matchGeneric.getMatchField().id == MatchFields.ETH_DST){
                jGen.writeStringField("dataLayerDestination", matchGeneric.getValue().toString());
            }
            else if (matchGeneric.getMatchField().id == MatchFields.ETH_SRC){
                jGen.writeStringField("dataLayerSource", matchGeneric.getValue().toString());
            }
            else if (matchGeneric.getMatchField().id == MatchFields.ETH_TYPE){
                jGen.writeNumberField("dataLayerType", Integer.decode(matchGeneric.getValue().toString()));
            }
            else if (matchGeneric.getMatchField().id == MatchFields.IN_PORT){
                jGen.writeNumberField("inputPort", Integer.parseInt(matchGeneric.getValue().toString()));
            }
            else if (matchGeneric.getMatchField().id == MatchFields.MPLS_TC){
                jGen.writeNumberField("mplsTc", Integer.decode(matchGeneric.getValue().toString()));
            }
            else if (matchGeneric.getMatchField().id == MatchFields.MPLS_BOS){
                jGen.writeStringField("mplsBos", matchGeneric.getValue().toString());
            }
            else if (matchGeneric.getMatchField().id == MatchFields.MPLS_LABEL){
                jGen.writeNumberField("mplsLabel", Integer.decode(matchGeneric.getValue().toString()));
            }
            else if (matchGeneric.getMatchField().id == MatchFields.IP_PROTO){
                jGen.writeNumberField("networkProtocol", Integer.parseInt(matchGeneric.getValue().toString())); 
            }
            //TODO: Ask Saurav about the implementation of tcp and udp.
            else if (matchGeneric.getMatchField().id == MatchFields.TCP_DST
                    || matchGeneric.getMatchField().id == MatchFields.UDP_DST){
                jGen.writeNumberField("transportDestination", Integer.parseInt(matchGeneric.getValue().toString()));
            }
            else if (matchGeneric.getMatchField().id == MatchFields.TCP_SRC
                    || matchGeneric.getMatchField().id == MatchFields.UDP_SRC){
                jGen.writeNumberField("transportSource", Integer.parseInt(matchGeneric.getValue().toString()));
            }
        }
        jGen.writeEndObject();
        
        jGen.writeFieldName("instructions");
        jGen.writeStartArray();
        jGen.writeStartObject();
        List<OFAction> actions = null;
        for (OFInstruction instruction: instructions){
            
            if(instruction.getType().equals(OFInstructionType.APPLY_ACTIONS)){
                actions = ((OFInstructionApplyActions)instruction).getActions();
            }
            else if(instruction.getType().equals(OFInstructionType.WRITE_ACTIONS)){
                actions = ((OFInstructionWriteActions)instruction).getActions();
            }
            else if(instruction.getType().equals(OFInstructionType.GOTO_TABLE)){
                
                jGen.writeFieldName(instruction.getType().name());
                jGen.writeStartObject();
                jGen.writeNumberField("tableId"
                        , ((OFInstructionGotoTable)instruction).getTableId().getValue());
                jGen.writeEndObject();
                continue;
            }//*/
            else{
                continue;
            }
            jGen.writeObjectFieldStart(instruction.getType().name());
            for (OFAction action : actions){
                if (action.getType().equals(OFActionType.GROUP)){
                    jGen.writeNumberField("group", ((OFActionGroup)action).getGroup().getGroupNumber());
                }
                else if (action.getType().equals(OFActionType.OUTPUT)){
                    if (((OFActionOutput)action).getPort().getPortNumber() == -3){
                        //Controller port
                        jGen.writeStringField("output", "CONTROLLER");
                    }
                    else{
                        jGen.writeNumberField("output", ((OFActionOutput)action).getPort().getPortNumber());
                    }
                }
                else if(action.getType().compareTo(OFActionType.POP_MPLS) == 0
                        || action.getType().compareTo(OFActionType.COPY_TTL_IN) == 0
                        || action.getType().compareTo(OFActionType.COPY_TTL_OUT) == 0
                        || action.getType().compareTo(OFActionType.DEC_MPLS_TTL) == 0
                        || action.getType().compareTo(OFActionType.DEC_NW_TTL) == 0
                        || action.getType().compareTo(OFActionType.POP_PBB) == 0
                        || action.getType().compareTo(OFActionType.POP_VLAN) == 0){
                    jGen.writeStringField(action.getType().name(), "True");
                }
                else if(action.getType().compareTo(OFActionType.COPY_TTL_IN) == 0){
                    jGen.writeStringField("POP_MPLS", "True");
                }
                else if (action.getType().equals(OFActionType.SET_FIELD)){
                    //TODO Support for more setFields
                    if (((OFActionSetField)action).getField().toString().contains("OFOxmEthSrcVer13")){
                        jGen.writeStringField("SET_DL_SRC", ((OFActionSetField)action).getField().getValue().toString());
                    }
                    else if (((OFActionSetField)action).getField().toString().contains("OFOxmEthDstVer13")){
                        jGen.writeStringField("SET_DL_DST", ((OFActionSetField)action).getField().getValue().toString());
                    }
                    else if (((OFActionSetField)action).getField().toString().contains("OFOxmNwDstVer13")){
                        jGen.writeStringField("SET_NW_SRC", ((OFActionSetField)action).getField().getValue().toString());
                    }
                    else if (((OFActionSetField)action).getField().toString().contains("OFOxmNwDstVer13")){
                        jGen.writeStringField("SET_NW_DST", ((OFActionSetField)action).getField().getValue().toString());
                    }
                    else if (((OFActionSetField)action).getField().toString().contains("OFOxmMplsLabelVer13")){
                        jGen.writeStringField("PUSH_MPLS", ((OFActionSetField)action).getField().getValue().toString());
                    }
                }
            }
            jGen.writeEndObject();
        }
        jGen.writeEndObject();
        jGen.writeEndArray();
        jGen.writeEndObject();
    }
    /**
     * Get the number of 1's in the 32bit integer
     * Use full to convert wildcard mask(x.x.x.x) to \x notation
     * for example
     * ("0.0.0.255") to int to "/8" or
     * ("0.0.255.255") to int to "/16"
     * @param x
     * @return
     */
    
    public static int covertToMask(int x) {
        x = x - ((x >>> 1) & 0x55555555);
        x = (x & 0x33333333) + ((x >>> 2) & 0x33333333);
        x = (x + (x >>> 4)) & 0x0F0F0F0F;
        x = x + (x >>> 8);
        x = x + (x >>> 16);
        return 32 - (x & 0x0000003F);
    } 

}
