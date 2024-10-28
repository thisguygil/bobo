package bobo.utils.api_clients;

import bobo.Config;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

public final class LastfmAPI {
    private LastfmAPI() {} // Prevent instantiation
    private static final String API_KEY = Config.get("LASTFM_API_KEY");
    private static final String SHARED_SECRET = Config.get("LASTFM_SHARED_SECRET");
    private static final String rootURL = "https://ws.audioscrobbler.com/2.0/"; // Root URL for all requests

    /**
     * Sends a GET request to the Last.fm API with the given parameters.
     *
     * @param params The parameters.
     * @return The response.
     */
    @Nullable
    public static String sendGetRequest(@Nonnull Map<String, String> params, boolean isAuthRequest) {
        // Add the required parameters
        params.put("api_key", API_KEY);
        if (isAuthRequest) {
            try {
                params.put("api_sig", generateApiSignature(params));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            params.put("format", "json");
        }

        // Build the URL with the given parameters encoded
        StringBuilder urlBuilder = new StringBuilder(rootURL).append("?");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            urlBuilder.append("&").append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append("=").append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        String url = urlBuilder.toString();

        // Send the GET request and return the response
        try {
            // Create the request
            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);

            // Send the request and get the response
            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                return EntityUtils.toString(entity);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Generates the API signature for the given parameters.
     *
     * @param params The parameters.
     * @return The API signature.
     * @throws NoSuchAlgorithmException If the MD5 algorithm is not available.
     */
    @Nonnull
    private static String generateApiSignature(Map<String, String> params) throws NoSuchAlgorithmException {
        // Use TreeMap to automatically sort the parameters alphabetically by key
        Map<String, String> sortedParams = new TreeMap<>(params);

        // Build the string to hash
        StringBuilder toHash = new StringBuilder();
        for (Map.Entry<String, String> param : sortedParams.entrySet()) {
            toHash.append(param.getKey()).append(param.getValue());
        }
        toHash.append(SHARED_SECRET);

        // Generate MD5 hash
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hashBytes = md.digest(toHash.toString().getBytes());

        // Convert byte array into hex string
        StringBuilder hexString = new StringBuilder();
        for (byte hashByte : hashBytes) {
            String hex = Integer.toHexString(0xff & hashByte);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }
}