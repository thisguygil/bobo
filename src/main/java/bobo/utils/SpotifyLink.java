package bobo.utils;

import bobo.Config;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;

import javax.annotation.Nonnull;
import java.io.IOException;

public final class SpotifyLink {
    private static final String CLIENT_ID = Config.get("SPOTIFY_CLIENT_ID");
    private static final String CLIENT_SECRET = Config.get("SPOTIFY_CLIENT_SECRET");

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
}
