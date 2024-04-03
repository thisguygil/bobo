package bobo.utils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class MusicBrainzAPI {
    private static final String baseUrl = "https://musicbrainz.org/ws/2/";

    private MusicBrainzAPI() {} // Prevent instantiation

    /**
     * Sends a GET request to the MusicBrainz API.
     *
     * @param endpoint The endpoint to send the request to.
     * @return The response from the API.
     */
    @Nullable
    private static String sendGetRequest(String endpoint) {
        try {
            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(baseUrl + endpoint + "?fmt=json");

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
     * Gets information about an artist given their MusicBrainz MBID. Caller still needs to parse the JSON response.
     * @param artistMbid The MusicBrainz MBID of the artist.
     * @return The response from the API.
     */
    public static String getArtistInfo(String artistMbid) {
        return sendGetRequest("artist/" + artistMbid);
    }

    /**
     * Gets information about an album given its MusicBrainz MBID. Caller still needs to parse the JSON response.
     * @param albumMbid The MusicBrainz MBID of the album.
     * @return The response from the API.
     */
    public static String getAlbumInfo(String albumMbid) {
        return sendGetRequest("release/" + albumMbid);
    }
}