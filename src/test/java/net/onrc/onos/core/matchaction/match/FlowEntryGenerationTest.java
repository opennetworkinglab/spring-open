package net.onrc.onos.core.matchaction.match;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.util.MACAddress;
import net.onrc.onos.core.datagrid.IDatagridService;
import net.onrc.onos.core.datagrid.IEventChannel;
import net.onrc.onos.core.datagrid.IEventChannelListener;
import net.onrc.onos.core.matchaction.MatchAction;
import net.onrc.onos.core.matchaction.MatchActionComponent;
import net.onrc.onos.core.matchaction.MatchActionId;
import net.onrc.onos.core.matchaction.MatchActionOperationEntry;
import net.onrc.onos.core.matchaction.MatchActionOperations;
import net.onrc.onos.core.matchaction.MatchActionOperationsId;
import net.onrc.onos.core.matchaction.SwitchResultList;
import net.onrc.onos.core.util.IPv4Net;
import net.onrc.onos.core.util.SwitchPort;
import org.junit.Before;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

public class FlowEntryGenerationTest {

    private FloodlightModuleContext modContext;
    private IDatagridService datagridService;

    @Before
    @SuppressWarnings("unchecked")
    public void setUpMocks() {
        final IEventChannel<String, MatchActionOperations> installSetChannel =
                createMock(IEventChannel.class);
        final IEventChannel<String, SwitchResultList> installSetReplyChannel =
                createMock(IEventChannel.class);

        datagridService = createNiceMock(IDatagridService.class);
        modContext = createMock(FloodlightModuleContext.class);

        expect(modContext.getServiceImpl(IDatagridService.class))
                .andReturn(datagridService).once();

        expect(datagridService.createChannel("onos.matchaction.installSetChannel",
                String.class,
                MatchActionOperations.class))
                .andReturn(installSetChannel).once();

        expect(datagridService.addListener(
                eq("onos.matchaction.installSetChannel"),
                anyObject(IEventChannelListener.class),
                eq(String.class),
                eq(MatchActionOperations.class)))
                .andReturn(installSetChannel).once();

        expect(datagridService.createChannel("onos.matchaction.installSetReplyChannel",
                String.class,
                SwitchResultList.class))
                .andReturn(installSetReplyChannel).once();

        expect(datagridService.addListener(
                eq("onos.matchaction.installSetReplyChannel"),
                anyObject(IEventChannelListener.class),
                eq(String.class),
                eq(SwitchResultList.class)))
                .andReturn(installSetReplyChannel).once();

        replay(datagridService);
    }

    //@Test
    public void testSingleFlow() throws Exception {
        final MatchActionOperationsId operationsId = new MatchActionOperationsId(1L);
        final MatchActionId matchActionId = new MatchActionId(1L);

        final MatchActionComponent component = new MatchActionComponent(datagridService, null, null);

        MACAddress srcMac = new MACAddress(new byte[]{0, 0, 0, 0, 0, 0});
        MACAddress dstMac = new MACAddress(new byte[]{0, 0, 0, 0, 0, 1});
        Short etherType = 1;
        IPv4Net srcIp = new IPv4Net("10.1.1.1/8");
        IPv4Net dstIp = new IPv4Net("10.2.2.2/8");
        Byte ipProto = 1;
        Short srcTcpPort = 80;
        Short dstTcpPort = 80;

        final MatchActionOperations operations = new MatchActionOperations(operationsId);
        final Match match = new PacketMatch(srcMac, dstMac, etherType, srcIp, dstIp, ipProto, srcTcpPort, dstTcpPort);

        final SwitchPort port = new SwitchPort(4L, 4L);
        final MatchAction target = new MatchAction(matchActionId, port, match, null);

        final MatchActionOperationEntry entry =
                new MatchActionOperationEntry(MatchActionOperations.Operator.ADD, target);
        operations.addOperation(entry);
        component.executeOperations(operations);
    }
}
