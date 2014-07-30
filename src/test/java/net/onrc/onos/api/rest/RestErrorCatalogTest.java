package net.onrc.onos.api.rest;

import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

/**
 * Tests to make sure that the error catalog is consistent.
 */
public class RestErrorCatalogTest {

    /**
     * Make sure that there are no duplicate entries in the catalog.
     */
    @Test
    public void testNoDuplicatesInCatalog() {
        final Set<RestErrorCode> entriesSeen = new HashSet<>();
        final RestErrorCatalogEntry[] rawEntries = RestErrorCatalog.getCatalogEntries();

        for (final RestErrorCatalogEntry entry : rawEntries) {
            //  There should only be one entry for this code, if we have seen it
            //  before that's an error.
            assertThat(entriesSeen, not(hasItem(entry.getCode())));

            entriesSeen.add(entry.getCode());
        }
    }

    /**
     * Make sure that each REST error code has an entry in the catalog.
     */
    @Test
    public void testAllCodesInCatalog() {

        final Map<Integer, RestErrorCatalogEntry> catalogEntryMap =
                RestErrorCatalog.getRestErrorMap();
        for (final RestErrorCode code : RestErrorCode.values()) {
            //  There should be a RestErrorCatalogEntry for every code.
            assertThat(catalogEntryMap.keySet(), hasItem(code.ordinal()));
        }
    }
}
