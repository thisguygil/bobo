package bobo.utils.api_clients;

import bobo.Config;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SpotifyLink {
    private static final String CLIENT_ID = Config.get("SPOTIFY_CLIENT_ID");
    private static final String CLIENT_SECRET = Config.get("SPOTIFY_CLIENT_SECRET");

    public static final Pattern URL_PATTERN = Pattern.compile("(https?://)(www\\.)?open\\.spotify\\.com/((?<region>[a-zA-Z-]+)/)?(user/(?<user>[a-zA-Z0-9-_]+)/)?(?<type>track|album|playlist|artist)/(?<identifier>[a-zA-Z0-9-_]+)");

    private SpotifyLink() {} // Prevent instantiation

    /**
     * Gets a Spotify API instance.
     *
     * @return Spotify API instance
     * @throws IOException If an error occurs while communicating with the Spotify API.
     * @throws ParseException If an error occurs while parsing the response from the Spotify API.
     * @throws SpotifyWebApiException If an error occurs while communicating with the Spotify API.
     */
    @Nonnull
    public static SpotifyApi getSpotifyApi() throws IOException, ParseException, SpotifyWebApiException {
        SpotifyApi spotifyApi = new SpotifyApi.Builder()
                .setClientId(CLIENT_ID)
                .setClientSecret(CLIENT_SECRET)
                .build();

        ClientCredentials clientCredentials = spotifyApi.clientCredentials()
                .build()
                .execute();

        String accessToken = clientCredentials.getAccessToken();
        spotifyApi.setAccessToken(accessToken);
        return spotifyApi;
    }

    /**
     * Gets the name of the album from a Spotify URL, or null if the URL is not a Spotify URL.
     *
     * @param url The Spotify URL.
     * @return The name of the album.
     */
    @Nullable
    public static String getAlbumName(String url) {
        try {
            Matcher matcher = URL_PATTERN.matcher(url);
            if (matcher.matches()) {
                SpotifyApi spotifyApi = SpotifyLink.getSpotifyApi();
                String id = matcher.group("identifier");
                return spotifyApi.getTrack(id)
                        .build()
                        .execute()
                        .getAlbum()
                        .getName();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}