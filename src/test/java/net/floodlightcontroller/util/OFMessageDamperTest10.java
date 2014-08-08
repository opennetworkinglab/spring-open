package net.floodlightcontroller.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.EnumSet;

import net.floodlightcontroller.core.FloodlightContext;

import org.junit.Before;
import org.junit.Test;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFEchoRequest;
import org.projectfloodlight.openflow.protocol.OFHello;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFVersion;

public class OFMessageDamperTest10 {

    /*
        OFFactory factory10 = OFFactories.getFactory(OFVersion.OF_10);
        OFMessageDamper damper;
        FloodlightContext cntx;

        OFMessageDamperMockSwitch sw1;
        OFMessageDamperMockSwitch sw2;

        OFEchoRequest echoRequst1;
        OFEchoRequest echoRequst1Clone;
        OFEchoRequest echoRequst2;
        OFHello hello1;
        OFHello hello2;

        @Before
        public void setUp() throws IOException {
            cntx = new FloodlightContext();

            sw1 = new OFMessageDamperMockSwitch();
            sw2 = new OFMessageDamperMockSwitch();

            echoRequst1 = factory10.buildEchoRequest()
                    .setData(new byte[] {1}).build();
            echoRequst1Clone = factory10.buildEchoRequest()
                    .setData(new byte[] {1}).build();
            echoRequst2 = factory10.buildEchoRequest()
                    .setData(new byte[] {2}).build();

            hello1 = factory10.buildHello()
                    .setXid(1).build();
            hello2 = factory10.buildHello()
                    .setXid(2).build();

        }

        protected void doWrite(boolean expectWrite,
                OFMessageDamperMockSwitch sw,
                OFMessage msg,
                FloodlightContext cntx) throws IOException {

            boolean result;
            sw.reset();
            result = damper.write(sw, msg, cntx);

            if (expectWrite) {
                assertEquals(true, result);
                sw.assertMessageWasWritten(msg, cntx);
            } else {
                assertEquals(false, result);
                sw.assertNoMessageWritten();
            }
        }

        @Test
        public void testOneMessageType() throws IOException, InterruptedException {
            int timeout = 50;
            int sleepTime = 60;
            damper = new OFMe ssageDamper(100,
                    EnumSet.of(OFType.ECHO_REQUEST),
                    timeout);

            // echo requests should be dampened
            doWrite(true, sw1, echoRequst1, cntx);
            doWrite(false, sw1, echoRequst1, cntx);
            doWrite(false, sw1, echoRequst1Clone, cntx);
            doWrite(true, sw1, echoRequst2, cntx);
            doWrite(false, sw1, echoRequst2, cntx);

            // we don't dampen hellos. All should succeed
            doWrite(true, sw1, hello1, cntx);
            doWrite(true, sw1, hello1, cntx);
            doWrite(true, sw1, hello1, cntx);

            // echo request should also be dampened on sw2
            doWrite(true, sw2, echoRequst1, cntx);
            doWrite(false, sw2, echoRequst1, cntx);
            doWrite(true, sw2, echoRequst2, cntx);

            Thread.sleep(sleepTime);
            doWrite(true, sw1, echoRequst1, cntx);
            doWrite(true, sw2, echoRequst1, cntx);

        }

        @Test
        public void testTwoMessageTypes() throws IOException, InterruptedException {
            int timeout = 50;
            int sleepTime = 60;
            damper = new OFMessageDamper(100,
                    EnumSet.of(OFType.ECHO_REQUEST,
                            OFType.HELLO),
                    timeout);

            // echo requests should be dampened
            doWrite(true, sw1, echoRequst1, cntx);
            doWrite(false, sw1, echoRequst1, cntx);
            doWrite(false, sw1, echoRequst1Clone, cntx);
            doWrite(true, sw1, echoRequst2, cntx);
            doWrite(false, sw1, echoRequst2, cntx);

            // hello should be dampened as well
            doWrite(true, sw1, hello1, cntx);
            doWrite(false, sw1, hello1, cntx);
            doWrite(false, sw1, hello1, cntx);

            doWrite(true, sw1, hello2, cntx);
            doWrite(false, sw1, hello2, cntx);
            doWrite(false, sw1, hello2, cntx);

            // echo request should also be dampened on sw2
            doWrite(true, sw2, echoRequst1, cntx);
            doWrite(false, sw2, echoRequst1, cntx);
            doWrite(true, sw2, echoRequst2, cntx);

            Thread.sleep(sleepTime);
            doWrite(true, sw1, echoRequst1, cntx);
            doWrite(true, sw2, echoRequst1, cntx);
            doWrite(true, sw1, hello1, cntx);
            doWrite(true, sw1, hello2, cntx);
        }
    */
}
