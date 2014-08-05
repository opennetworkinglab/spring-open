package net.onrc.onos.api.newintent;

import net.onrc.onos.core.util.LinkTuple;
import net.onrc.onos.core.util.SwitchPort;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class PathIntentTest extends ConnectivityIntentTest {
    public static final SwitchPort P1_2 = new SwitchPort(111, (short) 0x11);
    public static final SwitchPort P2_2 = new SwitchPort(222, (short) 0x22);
    public static final SwitchPort P3_2 = new SwitchPort(333, (short) 0x33);

    private static final List<LinkTuple> PATH1 = Arrays.asList(
            new LinkTuple(P1_2, P2_2)
    );
    private static final List<LinkTuple> PATH2 = Arrays.asList(
            new LinkTuple(P1_2, P3_2)
    );

    @Test
    public void basics() {
        PathIntent intent = createOne();
        assertEquals("incorrect id", IID, intent.getId());
        assertEquals("incorrect match", MATCH, intent.getMatch());
        assertEquals("incorrect action", NOP, intent.getAction());
        assertEquals("incorrect ingress", P1, intent.getIngressPort());
        assertEquals("incorrect egress", P2, intent.getEgressPort());
        assertEquals("incorrect path", PATH1, intent.getPath());
    }

    @Override
    protected PathIntent createOne() {
        return new PathIntent(IID, MATCH, NOP, P1, P2, PATH1);
    }

    @Override
    protected PathIntent createAnother() {
        return new PathIntent(IID, MATCH, NOP, P1, P3, PATH2);
    }
}
