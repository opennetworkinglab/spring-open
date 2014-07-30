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
public class RestErrorCatalogEntryTest {

    /**
     * Test the formatting of a REST error that contains a single
     * positional parameter.
     */
    @Test
    public void testRestErrorFormatting1Parameter() {
        final RestErrorCatalogEntry restErrorCatalogEntry =
                RestErrorCatalog.getRestError(RestErrorCode.INTENT_ALREADY_EXISTS);
        assertThat(restErrorCatalogEntry, is(notNullValue()));

        final RestError formattedError =
                RestError.createRestError(RestErrorCode.INTENT_ALREADY_EXISTS,
                                          "INTENT-ID");
        final String formattedErrorString =
                formattedError.getFormattedDescription();
        assertThat(formattedErrorString, is(notNullValue()));

        final String expectedFormattedString =
                "An intent with the identifier INTENT-ID could not be created " +
                "because one already exists.";
        assertThat(formattedErrorString, is(equalTo(expectedFormattedString)));
    }

    /**
     * Test the formatting of a REST error that contains two
     * positional parameters.
     */
    @Test
    public void testRestErrorFormatting2Parameters() {
        final RestErrorCatalogEntry restErrorCatalogEntry =
                RestErrorCatalog.getRestError(RestErrorCode.INTENT_NO_PATH);
        assertThat(restErrorCatalogEntry, is(notNullValue()));

        final RestError formattedError =
                RestError.createRestError(RestErrorCode.INTENT_NO_PATH,
                                          "Switch1", "Switch2");
        final String formattedErrorString =
                formattedError.getFormattedDescription();

        assertThat(formattedErrorString, is(notNullValue()));

        final String expectedFormattedString =
                "No path found between Switch1 and Switch2";
        assertThat(formattedErrorString, is(equalTo(expectedFormattedString)));
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
     * Make sure that the RestErrorCatalogEntry class is immutable.
     */
    @Test
    public void assureThatRestErrorCatalogEntryIsImmutable() {
        assertThatClassIsImmutable(RestErrorCatalogEntry.class);
    }

    /**
     * Make sure that the RestErrorCatalogEntry class is immutable.
     */
    @Test
    public void assureThatRestErrorIsImmutable() {
        assertThatClassIsImmutable(RestError.class);
    }
}
