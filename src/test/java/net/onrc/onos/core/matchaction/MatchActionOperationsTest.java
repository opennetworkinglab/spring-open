package net.onrc.onos.core.matchaction;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import net.onrc.onos.core.util.TestUtils;
import org.junit.Test;

/**
 * Unit tests for Match Action Operations.
 */
public class MatchActionOperationsTest {

    /**
     * Test that creation of MatchActionOperations objects is correct and that
     * the objects have unique identifiers.
     */
    @Test
    public void testMatchActionoperationsCreate() {
        final MatchActionOperationsId id1 =
            new MatchActionOperationsId(1L);
        final MatchActionOperations operations1 =
            new MatchActionOperations(id1);
        assertThat(id1, is(notNullValue()));
        assertThat(id1, is(equalTo(operations1.getOperationsId())));

        final MatchActionOperationsId id2 =
            new MatchActionOperationsId(2L);
        final MatchActionOperations operations2 =
            new MatchActionOperations(id2);
        assertThat(id2, is(notNullValue()));
        assertThat(id2, is(equalTo(operations2.getOperationsId())));

        assertThat(id1.getId(), is(not(equalTo(id2.getId()))));
    }

    /**
     * Test the correctness of the equals() operation for
     * MatchActionOperationsId objects.
     * objects.
     */
    @Test
    public void testMatchActionOperationsIdEquals() {
        final MatchActionOperationsId id1 =
                new MatchActionOperationsId(1L);
        final MatchActionOperationsId id2 =
                new MatchActionOperationsId(2L);
        final MatchActionOperationsId id1Copy =
                new MatchActionOperationsId(1L);


        // Check that null does not match
        assertThat(id1, is(not(equalTo(null))));

        // Check that different objects do not match
        assertThat(id1, is(not(equalTo(id2))));

        // Check that copies match
        TestUtils.setField(id1Copy, "id", id1.getId());
        assertThat(id1, is(equalTo(id1Copy)));

        // Check that the same object matches
        assertThat(id1, is(equalTo(id1Copy)));
    }

    /**
     * Test the correctness of the hashCode() operation for
     * MatchActionOperationsId objects.
     */
    @Test
    public void testMatchActionOperationsIdHashCode() {
        final MatchActionOperationsId id1 =
                new MatchActionOperationsId(22L);
        assertThat(id1.hashCode(), is(equalTo(22)));
    }
}
