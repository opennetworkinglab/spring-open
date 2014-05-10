package net.onrc.onos.api.rest;

/**
 * Describes a REST error.
 * code is a unique identifier for the error.
 * summary is indended to be a short description of what happened.
 * description is a long description of the problem, and can be formatted using
 * variable replacement.  Variable placeholders are indicated with the string
 * "{}" in the description.
 * Objects of this class are immutable.
 */

public final class RestError {

    private final RestErrorCodes.RestErrorCode code;
    private final String summary;
    private final String description;

    /**
     * Constructs a new RestError object from a code, summary and description.
     *
     * @param newCode code for the new error
     * @param newSummary short summary for the new error
     * @param newDescription formatable description for the new error
     */
    public RestError(final RestErrorCodes.RestErrorCode newCode,
                     final String newSummary,
                     final String newDescription) {
        code = newCode;
        summary = newSummary;
        description = newDescription;
    }

    /**
     * Gets the summary of the error.
     *
     * @return string for the summary
     */
    public String getSummary() {
        return summary;
    }

    /**
     * Gets the unique code for this error.
     *
     * @return unique code
     */
    public RestErrorCodes.RestErrorCode getCode() {
        return code;
    }

    /**
     * Gets the unformatted description string for the error.
     *
     * @return the unformatted description string.
     */
    public String getDescription() {
        return description;
    }
}
