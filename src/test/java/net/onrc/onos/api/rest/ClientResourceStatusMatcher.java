package net.onrc.onos.api.rest;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;

/**
 * Hamcrest Matcher to determine if a Restlet ClientResource object's status
 * matches a given status.
 */
public class ClientResourceStatusMatcher extends TypeSafeMatcher<ClientResource> {
    private Status status;

    /**
     * Hide constructor with no arguments.
     */
    @SuppressWarnings("unused")
    private ClientResourceStatusMatcher() { }

    /**
     * Create a matcher for the given status object.
     *
     * @param newStatus status to be matched against
     */
    private ClientResourceStatusMatcher(final Status newStatus) {
        status = newStatus;
    }

    /**
     * Match a Client resource.  Compares the status of the given client against
     * the expected Status that was given when the object was created.
     *
     * @param client the client to perform the match against
     * @return true if the Status of the given client matches the Status that
     *         was given when the Matcher was created, false otherwise
     */
    @Override
    public boolean matchesSafely(ClientResource client) {
        return status.equals(client.getStatus());
    }

    /**
     * Generate a Hamcrest description for the 'to' object of this Matcher.
     *
     * @param description Description object to add the textual description to
     */
    @Override
    public void describeTo(Description description) {
        description.appendText("status '")
                   .appendText(status.getDescription())
                   .appendText("'");
    }

    /**
     * Describe why a mismatch was generated for the given client.
     *
     * @param client the client that was detected as a mismatch
     * @param mismatchDescription Description object to add the textual
     *                            description to
     */
    @Override
    public void describeMismatchSafely(ClientResource client,
                                       Description mismatchDescription) {
        mismatchDescription.appendText("   was '")
                           .appendText(client.getStatus().getDescription())
                           .appendText("'");
    }

    /**
     * Factory method to create a status Matcher for an ClientResource.
     *
     * @param status HTTP status code that the client status is matched against.
     * @return Matcher object
     */
    @Factory
    public static Matcher<ClientResource> hasStatusOf(final Status status) {
        return new ClientResourceStatusMatcher(status);
    }
}
