package net.onrc.onos.core.newintent;

import net.onrc.onos.api.intent.IntentId;
import net.onrc.onos.core.matchaction.match.PacketMatch;
import net.onrc.onos.core.util.SwitchPort;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PointToPointIntentTest {
    private final IntentId id = new IntentId(0);
    private final SwitchPort port1 = new SwitchPort(0L, 0L);
    private final SwitchPort port2 = new SwitchPort(1L, 1L);
    private final PacketMatch match = new PacketMatch();

    @Test
    public void testEqualityWhenIdleTimeoutIsZero() {
        PointToPointIntent intent1 = new PointToPointIntent(id, port1, port2, match,
                0, TimeUnit.DAYS);
        PointToPointIntent intent2 = new PointToPointIntent(id, port1, port2, match,
                0, TimeUnit.SECONDS);

        assertThat(intent1, is(intent2));
    }

    @Test
    public void testEqualityWhenIdleTimeoutIsOneSecond() {
        PointToPointIntent intent1 = new PointToPointIntent(id, port1, port2, match,
                1, TimeUnit.SECONDS);
        PointToPointIntent intent2 = new PointToPointIntent(id, port1, port2, match,
                1000, TimeUnit.MILLISECONDS);

        assertThat(intent1, is(intent2));
    }

    @Test
    public void testEqualityWhenIdleTimeoutIsTruncated() {
        PointToPointIntent intent1 = new PointToPointIntent(id, port1, port2, match,
                1, TimeUnit.MICROSECONDS);
        PointToPointIntent intent2 = new PointToPointIntent(id, port1, port2, match,
                1001, TimeUnit.NANOSECONDS);

        assertThat(intent1, is(intent2));
    }
}
