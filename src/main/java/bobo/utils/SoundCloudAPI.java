package bobo.utils;

import bobo.Config;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;

public final class SoundCloudAPI {
    private static final String baseURL = "https://api-v2.soundcloud.com/";

    private SoundCloudAPI() {} // Prevent instantiation

    @Nullable
    private static String sendGetRequest(String endpoint, Map<String, String> params) {
        try {
            HttpClient httpClient = HttpClients.createDefault();

            StringBuilder url = new StringBuilder(baseURL + endpoint + "?client_id=" + Config.get("SOUNDCLOUD_CLIENT_ID"));
            for (Map.Entry<String, String> entry : params.entrySet()) {
                url.append("&").append(entry.getKey()).append("=").append(entry.getValue());
            }
            String urlStr = url.toString()
                    .replaceAll(" ", "%20")
                    .replaceAll("\"", "%22");
            HttpGet httpGet = new HttpGet(urlStr);

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
     * Searches SoundCloud for a query.
     *
     * @param query The query to search for.
     * @param type The type of search to perform (tracks, playlists, or albums).
     * @param limit The maximum number of results to return.
     * @return The response from the API.
     */
    public static String search(String query, String type, int limit) {
        return sendGetRequest("search/" + type, Map.of("q", query, "limit", String.valueOf(limit)));
    }
}