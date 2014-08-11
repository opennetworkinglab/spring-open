package net.onrc.onos.core.drivermanager;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import net.floodlightcontroller.core.SwitchDriverSubHandshakeAlreadyStarted;
import net.floodlightcontroller.core.SwitchDriverSubHandshakeCompleted;
import net.floodlightcontroller.core.SwitchDriverSubHandshakeNotStarted;
import net.floodlightcontroller.core.internal.OFSwitchImplBase;

import org.projectfloodlight.openflow.protocol.OFBarrierRequest;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFErrorMsg;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMessage;

/**
 * OFDescriptionStatistics Vendor (Manufacturer Desc.): Nicira, Inc. Make
 * (Hardware Desc.) : Open vSwitch Model (Datapath Desc.) : None Software :
 * 2.1.0 (or whatever version + build) Serial : None
 */
public class OFSwitchImplOVS13 extends OFSwitchImplBase {
    private AtomicBoolean driverHandshakeComplete;
    private OFFactory factory;
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
        factory = getFactory();
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
        OFBarrierRequest br = factory
                .buildBarrierRequest()
                .setXid(xid)
                .build();
        write(br, null);
    }
}
