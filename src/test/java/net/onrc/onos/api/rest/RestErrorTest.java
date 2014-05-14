package net.onrc.onos.api.rest;

import org.junit.Test;

import static net.onrc.onos.core.util.ImmutableClassChecker.assertThatClassIsImmutable;
import static net.onrc.onos.core.util.UtilityClassChecker.assertThatClassIsUtility;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;


/**
 * Unit tests for REST error handling classes.
 */
public class RestErrorTest {

    /**
     * Test the formatting of a REST error that contains a single
     * positional parameter.
     */
    @Test
    public void testRestErrorFormatting1Parameter() {
        final RestError restError =
                RestErrorCatalog.getRestError(RestErrorCodes.RestErrorCode.INTENT_ALREADY_EXISTS);
        assertThat(restError, is(notNullValue()));

        final String formattedError =
                RestErrorFormatter.formatErrorMessage(restError,
                                                      "INTENT-ID");
        assertThat(formattedError, is(notNullValue()));

        final String expectedFormattedString =
                "An intent with the identifier INTENT-ID could not be created " +
                "because one already exists.";
        assertThat(formattedError, is(equalTo(expectedFormattedString)));

    }

    /**
     * Test the formatting of a REST error that contains two
     * positional parameters.
     */
    @Test
    public void testRestErrorFormatting2Parameters() {
        final RestError restError =
                RestErrorCatalog.getRestError(RestErrorCodes.RestErrorCode.INTENT_NO_PATH);
        assertThat(restError, is(notNullValue()));

        final String formattedError =
                RestErrorFormatter.formatErrorMessage(restError,
                        "Switch1", "Switch2");
        assertThat(formattedError, is(notNullValue()));

        final String expectedFormattedString =
                "No path found between Switch1 and Switch2";
        assertThat(formattedError, is(equalTo(expectedFormattedString)));

    }

    /**
     * Make sure that the error formatter is a utility class.
     */
    @Test
    public void assureThatErrorFormatterIsUtility() {
        assertThatClassIsUtility(RestErrorFormatter.class);
    }

    /**
     * Make sure that the error catalog is a utility class.
     */
    @Test
    public void assureThatErrorCatalogIsUtility() {
        assertThatClassIsUtility(RestErrorCatalog.class);
    }

    /**
     * Make sure that the error codes is a utility class.
     */
    @Test
    public void assureThatErrorCodesIsUtility() {
        assertThatClassIsUtility(RestErrorCodes.class);
    }

    /**
     * Make sure that the RestError class is immutable.
     */
    @Test
    public void assureThatRestErrorIsImmutable() {
        assertThatClassIsImmutable(RestError.class);
    }
}
