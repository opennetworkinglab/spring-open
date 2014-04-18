package net.onrc.onos.apps.sdnip;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple HTTP client. It is used to make REST calls to the BGPd process.
 */
public final class RestClient {
    private static final Logger log = LoggerFactory.getLogger(RestClient.class);

    /**
     * Default constructor.
     */
    private RestClient() {
        // Private constructor to prevent instantiation
    }

    /**
     * Issues a HTTP GET request to the specified URL.
     *
     * @param strUrl the URL to make the request to
     * @return a String containing any data returned by the server
     */
    public static String get(String strUrl) {
        StringBuilder response = new StringBuilder();

        try {
            URL url = new URL(strUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2 * 1000); // 2 seconds
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                // XXX bad. RestClient API needs to be redesigned
                throw new IOException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            if (!conn.getContentType().equals("application/json")) {
                log.warn("The content received from {} is not json", strUrl);
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
                            conn.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            br.close();
            conn.disconnect();

        } catch (MalformedURLException e) {
            log.error("Malformed URL for GET request", e);
        } catch (ConnectTimeoutException e) {
            log.warn("Couldn't connect to the remote REST server", e);
        } catch (IOException e) {
            log.warn("Couldn't connect to the remote REST server", e);
        }

        return response.toString();
    }

    /**
     * Issues a HTTP POST request to the specified URL.
     *
     * @param strUrl the URL to make the request to
     */
    public static void post(String strUrl) {

        try {
            URL url = new URL(strUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            if (conn.getResponseCode() != 200) {
                // XXX bad. RestClient API needs to be redesigned
                throw new IOException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            conn.disconnect();

        } catch (MalformedURLException e) {
            log.error("Malformed URL for GET request", e);
        } catch (IOException e) {
            log.warn("Couldn't connect to the remote REST server", e);
        }
    }

    /**
     * Issues a HTTP DELETE request to the specified URL.
     *
     * @param strUrl the URL to make the request to
     */
    public static void delete(String strUrl) {

        try {
            URL url = new URL(strUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                // XXX bad. RestClient API needs to be redesigned
                throw new IOException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            conn.disconnect();

        } catch (MalformedURLException e) {
            log.error("Malformed URL for GET request", e);
        } catch (IOException e) {
            log.warn("Couldn't connect to the remote REST server", e);
        }
    }
}
