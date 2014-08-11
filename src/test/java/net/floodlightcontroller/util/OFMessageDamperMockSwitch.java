package net.floodlightcontroller.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IFloodlightProviderService.Role;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.debugcounter.IDebugCounterService;
import net.floodlightcontroller.debugcounter.IDebugCounterService.CounterException;
import net.floodlightcontroller.threadpool.IThreadPoolService;

import org.jboss.netty.channel.Channel;
import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFCapabilities;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.types.U64;

/**
 * A mock implementation of IFOSwitch we use for {@link OFMessageDamper}
 * <p/>
 * We need to mock equals() and hashCode() but alas, EasyMock doesn't support
 * this. Sigh. And of course this happens to be the interface with the most
 * methods.
 *
 * @author gregor
 */
public class OFMessageDamperMockSwitch implements IOFSwitch {
    OFMessage writtenMessage;
    FloodlightContext writtenContext;

    public OFMessageDamperMockSwitch() {
        reset();
    }

    /* reset this mock. I.e., clear the stored message previously written */
    public void reset() {
        writtenMessage = null;
        writtenContext = null;
    }

    /* assert that a message was written to this switch and that the
     * written message and context matches the expected values
     * @param expected
     * @param expectedContext
     */
    public void assertMessageWasWritten(OFMessage expected,
            FloodlightContext expectedContext) {
        assertNotNull("No OFMessage was written", writtenMessage);
        assertEquals(expected, writtenMessage);
        assertEquals(expectedContext, writtenContext);
    }

    /*
     * assert that no message was written
     */
    public void assertNoMessageWritten() {
        assertNull("OFMessage was written but didn't expect one",
                writtenMessage);
        assertNull("There was a context but didn't expect one",
                writtenContext);
    }

    /*
     * use hashCode() and equals() from Object
     */

    @Override
    public void write(OFMessage m,
            FloodlightContext bc) throws IOException {
        assertNull("write() called but already have message", writtenMessage);
        assertNull("write() called but already have context", writtenContext);
        writtenContext = bc;
        writtenMessage = m;

    }

    @Override
    public void write(List<OFMessage> msglist,
            FloodlightContext bc) throws IOException {
        assertTrue("Unexpected method call", false);
    }

    // @Override
    // public void setFeaturesReply(OFFeaturesReply featuresReply) {
    // assertTrue("Unexpected method call", false);
    // }

    // @Override
    // public void setSwitchProperties(OFDescriptionStatistics description) {
    // assertTrue("Unexpected method call", false);
    // // TODO Auto-generated method stub
    // }

    @Override
    public Collection<OFPortDesc> getEnabledPorts() {
        assertTrue("Unexpected method call", false);
        return null;
    }

    @Override
    public Collection<Integer> getEnabledPortNumbers() {
        assertTrue("Unexpected method call", false);
        return null;
    }

    // @Override
    // public OFPhysicalPort getPort(short portNumber) {
    // assertTrue("Unexpected method call", false);
    // return null;
    // }

    @Override
    public OFPortDesc getPort(String portName) {
        assertTrue("Unexpected method call", false);
        return null;
    }

    // @Override
    // public void setPort(OFPhysicalPort port) {
    // assertTrue("Unexpected method call", false);
    // }

    // @Override
    // public void deletePort(short portNumber) {
    // assertTrue("Unexpected method call", false);
    // }

    // @Override
    // public void deletePort(String portName) {
    // assertTrue("Unexpected method call", false);
    // }

    @Override
    public Collection<OFPortDesc> getPorts() {
        assertTrue("Unexpected method call", false);
        return null;
    }

    // @Override
    // public boolean portEnabled(short portName) {
    // assertTrue("Unexpected method call", false);
    // return false;
    // }

    @Override
    public boolean portEnabled(String portName) {
        assertTrue("Unexpected method call", false);
        return false;
    }

    // @Override
    // public boolean portEnabled(OFPhysicalPort port) {
    // assertTrue("Unexpected method call", false);
    // return false;
    // }

    @Override
    public long getId() {
        assertTrue("Unexpected method call", false);
        return 0;
    }

    @Override
    public String getStringId() {
        assertTrue("Unexpected method call", false);
        return null;
    }

    @Override
    public Map<Object, Object> getAttributes() {
        assertTrue("Unexpected method call", false);
        return null;
    }

    @Override
    public Date getConnectedSince() {
        assertTrue("Unexpected method call", false);
        return null;
    }

    @Override
    public int getNextTransactionId() {
        assertTrue("Unexpected method call", false);
        return 0;
    }

    // @Override
    // public Future<List<OFStatistics>>
    // getStatistics(OFStatisticsRequest request) throws IOException {
    // assertTrue("Unexpected method call", false);
    // return null;
    // }

    @Override
    public boolean isConnected() {
        assertTrue("Unexpected method call", false);
        return false;
    }

    @Override
    public void setConnected(boolean connected) {
        assertTrue("Unexpected method call", false);
    }

    @Override
    public Role getRole() {
        assertTrue("Unexpected method call", false);
        return null;
    }

    // @Override
    // public boolean isActive() {
    // assertTrue("Unexpected method call", false);
    // return false;
    // }

    // @Override
    // public void deliverStatisticsReply(OFMessage reply) {
    // assertTrue("Unexpected method call", false);
    // }

    @Override
    public void cancelStatisticsReply(int transactionId) {
        assertTrue("Unexpected method call", false);
    }

    @Override
    public void cancelAllStatisticsReplies() {
        assertTrue("Unexpected method call", false);
    }

    @Override
    public boolean hasAttribute(String name) {
        assertTrue("Unexpected method call", false);
        return false;
    }

    @Override
    public Object getAttribute(String name) {
        assertTrue("Unexpected method call", false);
        return null;
    }

    @Override
    public void setAttribute(String name, Object value) {
        assertTrue("Unexpected method call", false);
    }

    @Override
    public Object removeAttribute(String name) {
        assertTrue("Unexpected method call", false);
        return null;
    }

    @Override
    public void clearAllFlowMods() {
        assertTrue("Unexpected method call", false);
    }

    // @Override
    // public boolean updateBroadcastCache(Long entry, Short port) {
    // assertTrue("Unexpected method call", false);
    // return false;
    // }

    // @Override
    // public Map<Short, Long> getPortBroadcastHits() {
    // assertTrue("Unexpected method call", false);
    // return null;
    // }

    // @Override
    // public void sendStatsQuery(OFStatisticsRequest request, int xid,
    // IOFMessageListener caller)
    // throws IOException {
    // assertTrue("Unexpected method call", false);
    // }

    @Override
    public void flush() {
        assertTrue("Unexpected method call", false);
    }

    // @Override
    // public void deliverOFFeaturesReply(OFMessage reply) {
    // // TODO Auto-generated method stub
    //
    // }

    @Override
    public void cancelFeaturesReply(int transactionId) {
        // TODO Auto-generated method stub

    }

    // @Override
    // public int getBuffers() {
    // // TODO Auto-generated method stub
    // return 0;
    // }

    @Override
    public Set<OFActionType> getActions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<OFCapabilities> getCapabilities() {
        // TODO Auto-generated method stub
        return null;
    }

    // @Override
    // public byte getTables() {
    // // TODO Auto-generated method stub
    // return 0;
    // }

    @Override
    public void disconnectSwitch() {
        // TODO Auto-generated method stub

    }

    @Override
    public void setChannel(Channel channel) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getNumBuffers() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public byte getNumTables() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public OFDescStatsReply getSwitchDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setOFVersion(OFVersion ofv) {
        // TODO Auto-generated method stub

    }

    @Override
    public OFVersion getOFVersion() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OFPortDesc getPort(int portNumber) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OrderedCollection<PortChangeEvent> processOFPortStatus(OFPortStatus ps) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean portEnabled(int portName) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public OrderedCollection<PortChangeEvent> comparePorts(Collection<OFPortDesc> ports) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OrderedCollection<PortChangeEvent> setPorts(Collection<OFPortDesc> ports) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deliverStatisticsReply(OFMessage reply) {
        // TODO Auto-generated method stub

    }

    @Override
    public Future<List<OFStatsReply>> getStatistics(OFStatsRequest<?> request)
            throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setRole(Role role) {
        // TODO Auto-generated method stub

    }

    @Override
    public U64 getNextGenerationId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setFloodlightProvider(IFloodlightProviderService controller) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setThreadPoolService(IThreadPoolService threadPool) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setDebugCounterService(IDebugCounterService debugCounter)
            throws CounterException {
        // TODO Auto-generated method stub

    }

    @Override
    public void startDriverHandshake() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isDriverHandshakeComplete() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void processDriverHandshakeMessage(OFMessage m) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setTableFull(boolean isFull) {
        // TODO Auto-generated method stub

    }

    @Override
    public OFFactory getFactory() {
        // TODO Auto-generated method stub
        return null;
    }

}