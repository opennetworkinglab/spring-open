package net.onrc.onos.core.datastore.utils;

import org.hamcrest.MatcherAssert;
import org.junit.Test;

import static net.onrc.onos.core.util.UtilityClassChecker.assertThatClassIsUtility;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Test cases for the ByteArrayUtil class.
 */

public class ByteArrayUtilTest {

    /**
     * Make sure that the ByteArrayUtil class is a utility class.
     */
    @Test
    public void testForWellDefinedUtilityClass() throws Exception {
        assertThatClassIsUtility(ByteArrayUtil.class);
    }

    /**
     * Test that when given a null array for the bytes to convert, the hex
     * conversion routines return an empty StringBuilder.
     */
    @Test
    public void testToHexStringBuilderNullParameter() {
        final StringBuilder allocatedBuilder =
                ByteArrayUtil.toHexStringBuilder(null, "");
        MatcherAssert.assertThat(allocatedBuilder, is(notNullValue()));
        MatcherAssert.assertThat(allocatedBuilder.toString(), is(equalTo("")));

        final StringBuilder providedBuilder = new StringBuilder();
        final StringBuilder returnedBuilder = ByteArrayUtil.toHexStringBuilder
                (null, "", providedBuilder);
        MatcherAssert.assertThat(providedBuilder, is(notNullValue()));
        MatcherAssert.assertThat(returnedBuilder, is(equalTo(providedBuilder)));
        MatcherAssert.assertThat(providedBuilder.toString(), is(equalTo("")));
    }

    /**
     * Test the array byte to String conversion routines to be sure the data
     * is converted properly.
     */
    @Test
    public void testToHexStringBuilder() {
        final byte[] testData = {0x1,  0x2,  0x3,  0x4,  0x5,
                                 0x3b, 0x3c, 0x3d, 0x3e, 0x3f};
        final String expectedResult = "1:2:3:4:5:3b:3c:3d:3e:3f";

        final StringBuilder result =
                ByteArrayUtil.toHexStringBuilder(testData, ":");

        MatcherAssert.assertThat(result, is(notNullValue()));
        MatcherAssert.assertThat(result.toString(),
                is(equalTo(expectedResult)));
        
        final StringBuilder providedBuilder = new StringBuilder();
        final StringBuilder returnedBuilder =
                ByteArrayUtil.toHexStringBuilder(testData, ":", providedBuilder);

        MatcherAssert.assertThat(returnedBuilder, is(notNullValue()));
        MatcherAssert.assertThat(returnedBuilder, is(equalTo(providedBuilder)));
        MatcherAssert.assertThat(returnedBuilder.toString(),
                is(equalTo(expectedResult)));
    }
}
