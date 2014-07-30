package net.onrc.onos.api.rest;

/**
 * Object to represent an instance of a particular REST error.  The error
 * contains a formatted description which has information about the particular
 * occurence.  Objects of this type are passed back to REST callers if an error
 * is encountered.
 */
public final class RestError {

    private final RestErrorCode code;
    private final String summary;
    private final String formattedDescription;

    /**
     * Hidden default constructor to force usage of the factory method.
     */
    private RestError() {
        // This is never called, but Java requires these initializations
        // because the members are final.
        code = RestErrorCode.INTENT_ALREADY_EXISTS;
        summary = "";
        formattedDescription = "";
    }

    /**
     * Constructor to make a new Error.  Called by factory method.
     *
     * @param code code for the new error
     * @param summary summary string for the new error
     * @param formattedDescription formatted full description of the error
     */
    private RestError(final RestErrorCode code,
                      final String summary,
                      final String formattedDescription) {
        this.code = code;
        this.summary = summary;
        this.formattedDescription = formattedDescription;
    }

    /**
     * Fetch the code for this error.
     *
     * @return error code
     */
    public RestErrorCode getCode() {
        return code;
    }

    /**
     * Fetch the summary for this error.
     *
     * @return summary
     */
    public String getSummary() {
        return summary;
    }

    /**
     * Fetch the formatted descritpion for this error.
     *
     * @return formatted error
     */
    public String getFormattedDescription() {
        return formattedDescription;
    }

    /**
     * Creates an object to represent an instance of a REST error.  The
     * descrption is formatted for this particular instance.
     *
     * @param code code of the error in the catalog
     * @param parameters list of positional parameters to use in formatting
     *                   the description
     * @return new RestError representing this intance
     */
    public static RestError createRestError(final RestErrorCode code,
                                            final Object... parameters) {
        final RestErrorCatalogEntry error = RestErrorCatalog.getRestError(code);
        final String formattedDescription =
                RestErrorFormatter.formatErrorMessage(error, parameters);
        return new RestError(code, error.getSummary(), formattedDescription);
    }
}
