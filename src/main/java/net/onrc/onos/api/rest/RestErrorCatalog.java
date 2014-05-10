package net.onrc.onos.api.rest;

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
    private static final RestError[] ERROR_LIST = {
        new RestError(RestErrorCodes.RestErrorCode.INTENT_NOT_FOUND,
                      "Intent not found",
                      "An intent with the identifier {} was not found."),
        new RestError(RestErrorCodes.RestErrorCode.INTENT_ALREADY_EXISTS,
                      "Intent already exists",
                      "An intent with the identifier {} could not be created " +
                      "because one already exists."),
        new RestError(RestErrorCodes.RestErrorCode.INTENT_NO_PATH,
                      "No path found",
                      "No path found between {} and {}"),

    };

    /**
     * Singleton implementation using the demand holder idiom.
     */
    private static class RestErrorMapHolder {
        /**
         * Load up the error map.
         *
         * @return REST error map
         */
        private static Map<Integer, RestError> initializeRestErrorMap() {
            restErrorMap = new HashMap<>();
            for (final RestError restError : ERROR_LIST) {
                restErrorMap.put(restError.getCode().ordinal(), restError);
            }
            return restErrorMap;
        }

        /**
         * Fetch the singleton map.
         *
         * @return map of the Rest Errors that was created from the known error
         * list.
         */
        public static Map<Integer, RestError> getRestErrorMap() {
            return restErrorMap;
        }


        private static Map<Integer, RestError> restErrorMap = initializeRestErrorMap();
    }

    /**
     * Fetch the map of REST errors.
     *
     * @return map of possible REST errors.
     */
    public static Map<Integer, RestError> getRestErrorMap() {
        return RestErrorMapHolder.getRestErrorMap();
    }

    /**
     * Fetch the RestError for the given code.
     *
     * @param code the code for the message to look up.
     * @return the REST error for the code if one exists, null if it does not
     *         exist.
     */
    public static RestError getRestError(final RestErrorCodes.RestErrorCode code) {
        return getRestErrorMap().get(code.ordinal());
    }
}
