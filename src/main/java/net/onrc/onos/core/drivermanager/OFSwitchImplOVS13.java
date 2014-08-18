package net.onrc.onos.core.drivermanager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import net.floodlightcontroller.core.SwitchDriverSubHandshakeAlreadyStarted;
import net.floodlightcontroller.core.SwitchDriverSubHandshakeCompleted;
import net.floodlightcontroller.core.SwitchDriverSubHandshakeNotStarted;
import net.floodlightcontroller.core.internal.OFSwitchImplBase;

import org.projectfloodlight.openflow.protocol.OFBarrierRequest;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFErrorMsg;
import org.projectfloodlight.openflow.protocol.OFMatchV3;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFOxmList;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;

/**
 * OFDescriptionStatistics Vendor (Manufacturer Desc.): Nicira, Inc. Make
 * (Hardware Desc.) : Open vSwitch Model (Datapath Desc.) : None Software :
 * 2.1.0 (or whatever version + build) Serial : None
 */
public class OFSwitchImplOVS13 extends OFSwitchImplBase {
    private AtomicBoolean driverHandshakeComplete;
    private long barrierXidToWaitFor = -1;

    public OFSwitchImplOVS13(OFDescStatsReply desc) {
        super();
        driverHandshakeComplete = new AtomicBoolean(false);
        setSwitchDescription(desc);
    }

    @Override
    public String toString() {
        return "OFSwitchImplOVS13 [" + ((channel != null)
                ? channel.getRemoteAddress() : "?")
                + " DPID[" + ((stringId != null) ? stringId : "?") + "]]";
    }

    @Override
    public void startDriverHandshake() throws IOException {
        log.debug("Starting driver handshake for sw {}", getStringId());
        if (startDriverHandshakeCalled) {
            throw new SwitchDriverSubHandshakeAlreadyStarted();
        }
        startDriverHandshakeCalled = true;
        populateTableMissEntry(0, true, false, false, 0);
        configureSwitch();
    }

    @Override
    public boolean isDriverHandshakeComplete() {
        if (!startDriverHandshakeCalled) {
            throw new SwitchDriverSubHandshakeNotStarted();
        }
        return driverHandshakeComplete.get();
    }

    @Override
    public void processDriverHandshakeMessage(OFMessage m) {
        if (!startDriverHandshakeCalled) {
            throw new SwitchDriverSubHandshakeNotStarted();
        }
        if (driverHandshakeComplete.get()) {
            throw new SwitchDriverSubHandshakeCompleted(m);
        }

        switch (m.getType()) {
        case BARRIER_REPLY:
            if (m.getXid() == barrierXidToWaitFor) {
                driverHandshakeComplete.set(true);
            }
            break;

        case ERROR:
            log.error("Switch {} Error {}", getStringId(), (OFErrorMsg) m);
            break;

        case FEATURES_REPLY:
            break;
        case FLOW_REMOVED:
            break;
        case GET_ASYNC_REPLY:
            // OFAsyncGetReply asrep = (OFAsyncGetReply)m;
            // decodeAsyncGetReply(asrep);
            break;

        case PACKET_IN:
            break;
        case PORT_STATUS:
            break;
        case QUEUE_GET_CONFIG_REPLY:
            break;
        case ROLE_REPLY:
            break;

        case STATS_REPLY:
            // processStatsReply((OFStatsReply) m);
            break;

        default:
            log.debug("Received message {} during switch-driver subhandshake "
                    + "from switch {} ... Ignoring message", m, getStringId());

        }
    }


    private void configureSwitch() throws IOException {
        // setAsyncConfig();
        // getTableFeatures();
        /*sendGroupFeaturesRequest();
        setL2Groups();
        sendBarrier(false);
        setL3Groups();
        setL25Groups();
        sendGroupDescRequest();*/
        // populateTableVlan();
        // populateTableTMac();
        // populateIpTable();
        // populateMplsTable();
        // populateTableMissEntry(TABLE_ACL, false, false, false, -1);
        sendBarrier(true);
    }


    private void sendBarrier(boolean finalBarrier) throws IOException {
        int xid = getNextTransactionId();
        if (finalBarrier) {
            barrierXidToWaitFor = xid;
        }
        OFBarrierRequest br = getFactory()
                .buildBarrierRequest()
                .setXid(xid)
                .build();
        write(br, null);
    }

    /**
     * By default if none of the booleans in the call are set, then the
     * table-miss entry is added with no instructions, which means that pipeline
     * execution will stop, and the action set associated with the packet will
     * be executed.
     *
     * @param tableToAdd table number to add the table miss entry in
     * @param toControllerNow as an APPLY_ACTION instruction
     * @param toControllerWrite as a WRITE_ACITION instruction
     * @param toTable as a GOTO_TABLE instruction
     * @param tableToSend table number to a a GOTO_TABLE instruction to
     * @throws IOException if there's a problem writing to the channel
     */
    // TODO: This is copied straight from the CPqD switch. We need to find
    // an abstraction for this behaviour.
    @SuppressWarnings("unchecked")
    private void populateTableMissEntry(int tableToAdd, boolean toControllerNow,
            boolean toControllerWrite,
            boolean toTable, int tableToSend) throws IOException {
        OFOxmList oxmList = OFOxmList.EMPTY;
        OFMatchV3 match = getFactory().buildMatchV3()
                .setOxmList(oxmList)
                .build();
        OFAction outc = getFactory().actions()
                .buildOutput()
                .setPort(OFPort.CONTROLLER)
                .setMaxLen(0xffff)
                .build();
        List<OFInstruction> instructions = new ArrayList<OFInstruction>();
        if (toControllerNow) {
            // table-miss instruction to send to controller immediately
            OFInstruction instr = getFactory().instructions()
                    .buildApplyActions()
                    .setActions(Collections.singletonList(outc))
                    .build();
            instructions.add(instr);
        }

        if (toControllerWrite) {
            // table-miss instruction to write-action to send to controller
            // this will be executed whenever the action-set gets executed
            OFInstruction instr = getFactory().instructions()
                    .buildWriteActions()
                    .setActions(Collections.singletonList(outc))
                    .build();
            instructions.add(instr);
        }

        if (toTable) {
            // table-miss instruction to goto-table x
            OFInstruction instr = getFactory().instructions()
                    .gotoTable(TableId.of(tableToSend));
            instructions.add(instr);
        }

        if (!toControllerNow && !toControllerWrite && !toTable) {
            // table-miss has no instruction - at which point action-set will be
            // executed - if there is an action to output/group in the action
            // set
            // the packet will be sent there, otherwise it will be dropped.
            instructions = (List<OFInstruction>) Collections.EMPTY_LIST;
        }

        OFMessage tableMissEntry = getFactory().buildFlowAdd()
                .setTableId(TableId.of(tableToAdd))
                .setMatch(match) // match everything
                .setInstructions(instructions)
                .setPriority(0)
                .setBufferId(OFBufferId.NO_BUFFER)
                .setIdleTimeout(0)
                .setHardTimeout(0)
                .setXid(getNextTransactionId())
                .build();
        write(tableMissEntry, null);
    }
}
