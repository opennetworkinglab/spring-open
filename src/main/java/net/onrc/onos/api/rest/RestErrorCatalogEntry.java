package net.onrc.onos.api.rest;

/**
 * Describes a REST error.
 * code is a unique identifier for the error.
 * summary is indended to be a short description of what happened.
 * descriptionFormatString is a long description of the problem, and can be formatted using
 * variable replacement.  Variable placeholders are indicated with the string
 * "{}" in the descriptionFormatString.
 * Objects of this class are immutable.
 */

public final class RestErrorCatalogEntry {

    private final RestErrorCode code;
    private final String summary;
    private final String descriptionFormatString;

    /**
     * Constructs a new RestErrorCatalogEntry object from a code, summary and descriptionFormatString.
     *
     * @param newCode code for the new error
     * @param newSummary short summary for the new error
     * @param newDescriptionFormatString formatable description for the new error
     */
    public RestErrorCatalogEntry(final RestErrorCode newCode,
                                 final String newSummary,
                                 final String newDescriptionFormatString) {
        code = newCode;
        summary = newSummary;
        descriptionFormatString = newDescriptionFormatString;
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
    public RestErrorCode getCode() {
        return code;
    }

    /**
     * Gets the unformatted description string for the error.
     *
     * @return the unformatted description string.
     */
    public String getDescriptionFormatString() {
        return descriptionFormatString;
    }
}
