package net.onrc.onos.api.rest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Maintains a catalog of RestErrors and allows lookup by error code.
 */
public final class RestErrorCatalog {

    /**
     * Hide the default constructor of a utility class.
     */
    private RestErrorCatalog() { }

    /**
     * Static list of known errors.  Someday this will be read in from an
     * external file.
     */
    private static final RestErrorCatalogEntry[] ERROR_LIST = {
        new RestErrorCatalogEntry(RestErrorCodes.RestErrorCode.INTENT_NOT_FOUND,
                      "Intent not found",
                      "An intent with the identifier {} was not found."),
        new RestErrorCatalogEntry(RestErrorCodes.RestErrorCode.INTENT_ALREADY_EXISTS,
                      "Intent already exists",
                      "An intent with the identifier {} could not be created " +
                      "because one already exists."),
        new RestErrorCatalogEntry(RestErrorCodes.RestErrorCode.INTENT_NO_PATH,
                      "No path found",
                      "No path found between {} and {}"),
        new RestErrorCatalogEntry(RestErrorCodes.RestErrorCode.INTENT_INVALID,
                      "Intent invalid",
                      "The intent provided is empty or invalid"),
    };

    /**
     * Singleton implementation using the demand holder idiom.
     */
    private static class RestErrorMapHolder {
        private static Map<Integer, RestErrorCatalogEntry> restErrorMap = initializeRestErrorMap();

        /**
         * Load up the error map.
         *
         * @return REST error map
         */
        private static Map<Integer, RestErrorCatalogEntry> initializeRestErrorMap() {
            restErrorMap = new HashMap<>();
            for (final RestErrorCatalogEntry restErrorCatalogEntry : ERROR_LIST) {
                restErrorMap.put(restErrorCatalogEntry.getCode().ordinal(), restErrorCatalogEntry);
            }
            return Collections.unmodifiableMap(restErrorMap);
        }

        /**
         * Fetch the singleton map.
         *
         * @return map of the Rest Errors that was created from the known error
         * list.
         */
        public static Map<Integer, RestErrorCatalogEntry> getRestErrorMap() {
            return restErrorMap;
        }

    }

    /**
     * Fetch the map of REST errors.
     *
     * @return map of possible REST errors.
     */
    public static Map<Integer, RestErrorCatalogEntry> getRestErrorMap() {
        return RestErrorMapHolder.getRestErrorMap();
    }

    /**
     * Fetch the RestErrorCatalogEntry for the given code.
     *
     * @param code the code for the message to look up.
     * @return the REST error for the code if one exists, null if it does not
     *         exist.
     */
    public static RestErrorCatalogEntry getRestError(final RestErrorCodes.RestErrorCode code) {
        return getRestErrorMap().get(code.ordinal());
    }

    /**
     * Fetch the array of catalog entries.
     *
     * @return array of REST error catalog entries currently in use
     */
    public static RestErrorCatalogEntry[] getCatalogEntries() {
        return Arrays.copyOf(ERROR_LIST, ERROR_LIST.length);
    }
}
