package net.onrc.onos.core.util;

import org.junit.Test;

import static net.onrc.onos.core.util.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Tests for class {@link OnosInstanceId}.
 */
public class OnosInstanceIdTest {
    /**
     * Tests the immutability of {@link OnosInstanceId}.
     */
    @Test
    public void testImmutable() {
        assertThatClassIsImmutable(OnosInstanceId.class);
    }

    /**
     * Tests valid class constructor for a string.
     */
    @Test
    public void testConstructorForString() {
        OnosInstanceId id = new OnosInstanceId("ONOS-ID");
        assertEquals(id.toString(), "ONOS-ID");
    }

    /**
     * Tests invalid class constructor for a null string.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidConstructorNullString() {
        OnosInstanceId id = new OnosInstanceId(null);
    }

    /**
     * Tests invalid class constructor for an empty string.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConstructors() {
        // Check constructor for invalid ID: empty string
        OnosInstanceId id = new OnosInstanceId("");
    }

    /**
     * Tests equality of {@link OnosInstanceId}.
     */
    @Test
    public void testEquality() {
        OnosInstanceId id1 = new OnosInstanceId("ONOS-ID");
        OnosInstanceId id2 = new OnosInstanceId("ONOS-ID");

        assertThat(id1, is(id2));
    }

    /**
     * Tests non-equality of {@link OnosInstanceId}.
     */
    @Test
    public void testNonEquality() {
        OnosInstanceId id1 = new OnosInstanceId("ONOS-ID1");
        OnosInstanceId id2 = new OnosInstanceId("ONOS-ID2");

        assertThat(id1, is(not(id2)));
    }

    /**
     * Tests object string representation.
     */
    @Test
    public void testToString() {
        OnosInstanceId id = new OnosInstanceId("ONOS-ID");
        assertEquals("ONOS-ID", id.toString());
    }
}
