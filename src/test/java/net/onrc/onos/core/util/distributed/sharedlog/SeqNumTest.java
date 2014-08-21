package net.onrc.onos.core.util.distributed.sharedlog;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import org.junit.Test;

import com.google.common.primitives.UnsignedLongs;

/**
 * Basic {@link SeqNum} class tests.
 */
public class SeqNumTest {

    /**
     * Tests {@link SeqNum#next()} points to next sequence number,
     * excluding reserved INITIAL value,
     * and correctly wraps around ULONG_MAX.
     */
    @Test
    public void testNext() {
        final SeqNum one = SeqNum.INITIAL.next();
        assertEquals(1L, one.longValue());

        // succ
        final SeqNum two = one.next();
        assertEquals(2L, two.longValue());

        // succ wraps around skipping INITIAL
        final SeqNum max = SeqNum.valueOf(UnsignedLongs.MAX_VALUE);
        assertEquals(one, max.next());
    }

    /**
     * Tests {@link SeqNum#prev()} points to previous sequence number,
     * excluding reserved INITIAL value,
     * and correctly wraps around ULONG_MAX.
     */
    @Test
    public void testPrev() {

        // prev
        final SeqNum two = SeqNum.valueOf(2);
        final SeqNum one = two.prev();
        assertEquals(1L, one.longValue());

        final SeqNum max = SeqNum.INITIAL.prev();
        assertEquals(UnsignedLongs.MAX_VALUE, max.longValue());

        // prev wraps around skipping INITIAL
        assertEquals(max, one.prev());
    }

    /**
     * Tests that SeqNum equals and hashCode.
     */
    @Test
    public void testEqualsObject() {
        final SeqNum s = SeqNum.valueOf(42L);

        assertTrue(s.equals(s));
        assertEquals(s.hashCode(), s.hashCode());

        assertTrue(s.equals(s.next().prev()));
        assertEquals(s.hashCode(), s.next().prev().hashCode());

        assertFalse(s.equals(s.next()));
        assertFalse(s.equals(s.prev()));
        assertFalse(s.equals(null));
        assertFalse(s.equals(Long.valueOf(42L)));
    }

    /**
     * Tests that SeqNum is converted to String as unsigned decimal.
     */
    @Test
    public void testToString() {
        assertEquals("0", SeqNum.INITIAL.toString());
        assertEquals("1", SeqNum.valueOf(1).toString());
        // toString format is unsigned decimal string
        assertEquals("9223372036854775808",
                     SeqNum.valueOf(Long.MAX_VALUE + 1).toString());
    }

    /**
     * Tests that comparison works treating long value as point in a ring.
     */
    @Test
    public void testCompareTo() {
        final SeqNum zero = SeqNum.INITIAL;
        final SeqNum one = SeqNum.valueOf(1);
        final SeqNum two = SeqNum.valueOf(2);
        final SeqNum oneAlt = zero.next();
        final SeqNum negOne = one.prev();

        // 0 < 1
        assertThat(zero.compareTo(one), lessThan(0));
        // 1 > 0
        assertThat(one.compareTo(zero), greaterThan(0));

        // 0 == 0
        assertThat(zero.compareTo(zero), equalTo(0));
        // 1 == 1
        assertThat(oneAlt.compareTo(one), equalTo(0));
        assertThat(one.compareTo(oneAlt), equalTo(0));

        // 2 > 1
        assertThat(two.compareTo(one), greaterThan(0));
        // 1 < 2
        assertThat(one.compareTo(two), lessThan(0));

        // (-1) < 1
        assertThat(negOne.compareTo(one), lessThan(0));
        // 1 > (-1)
        assertThat(one.compareTo(negOne), greaterThan(0));

        // (-1) > (-3)
        assertThat(negOne.compareTo(negOne.prev().prev()), greaterThan(0));
        // (-3) < (-1)
        assertThat(negOne.prev().prev().compareTo(negOne), lessThan(0));

        // (-1) > 0 [0 is always the smallest element]
        assertThat(negOne.compareTo(zero), greaterThan(0));
        // 0 < (-1) [0 is always the smallest element]
        assertThat(zero.compareTo(negOne), lessThan(0));

        /// comparison using shorter arc

        // 0 < SLONG_MAX+1(=HALF) [clockwise arc used]
        assertThat(zero.compareTo(zero.step(Long.MAX_VALUE).next()), lessThan(0));
        assertThat(zero.step(Long.MAX_VALUE).next().compareTo(zero), greaterThan(0));
        /// 0 is always compared clock wise (never wraps)
        // 0 < SLONG_MAX+1(=HALF)+1 [clockwise arc used]
        assertThat(zero.compareTo(zero.step(Long.MAX_VALUE).next().next()), lessThan(0));
        assertThat(zero.step(Long.MAX_VALUE).next().next().compareTo(zero), greaterThan(0));

        // 1 < 1+SLONG_MAX(=HALF-1) [clockwise arc used]
        assertThat(one.compareTo(one.step(Long.MAX_VALUE)), lessThan(0));
        assertThat(one.step(Long.MAX_VALUE).compareTo(one), greaterThan(0));
        // 1 < 1+SLONG_MAX+1(=HALF) [SAME, counter-clockwise arc used]
        assertThat(one.compareTo(one.step(Long.MAX_VALUE).next()), greaterThan(0));
        assertThat(one.step(Long.MAX_VALUE).next().compareTo(one), lessThan(0));
        // 1 < 1+SLONG_MAX+2(=HALF+1) [counter-clockwise arc used]
        assertThat(one.compareTo(one.step(Long.MAX_VALUE).next().next()), greaterThan(0));
        assertThat(one.step(Long.MAX_VALUE).next().next().compareTo(one), lessThan(0));

        // (-1) < (-1)+SLONG_MAX(=HALF-1) [clockwise arc used]
        assertThat(negOne.compareTo(negOne.step(Long.MAX_VALUE)), lessThan(0));
        assertThat(negOne.step(Long.MAX_VALUE).compareTo(negOne), greaterThan(0));
        // (-1) < (-1)+SLONG_MAX+1(=HALF) [SAME, clockwise arc used]
        assertThat(negOne.compareTo(negOne.step(Long.MAX_VALUE).next()), lessThan(0));
        assertThat(negOne.step(Long.MAX_VALUE).next().compareTo(negOne), greaterThan(0));
        // (-1) > (-1)+SLONG_MAX+2(=HALF+1) [counter-clockwise arc used]
        assertThat(negOne.compareTo(negOne.step(Long.MAX_VALUE).next().next()), greaterThan(0));
        assertThat(negOne.step(Long.MAX_VALUE).next().next().compareTo(negOne), lessThan(0));
    }

    /**
     * Tests parsing decimal unsigned long.
     */
    @Test
    public void testDecStr() {
        final SeqNum ref = SeqNum.valueOf(Long.MAX_VALUE + 1);
        assertEquals(ref, SeqNum.valueOf("9223372036854775808"));
        assertEquals(ref, SeqNum.anyValueOf("9223372036854775808"));
    }

    /**
     * Tests parsing hexadecimal unsigned long.
     */
    @Test
    public void testHexStr() {
        final SeqNum ref = SeqNum.valueOf(Long.MAX_VALUE + 1);
        assertEquals(ref, SeqNum.valueOf("0x8000000000000000"));
        assertEquals(ref, SeqNum.anyValueOf("0x8000000000000000"));
    }
}
