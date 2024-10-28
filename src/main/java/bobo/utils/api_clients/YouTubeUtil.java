package bobo.utils.api_clients;

import bobo.Config;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class YouTubeUtil {
    private static final String API_KEY = Config.get("GOOGLE_API_KEY");
    private static final YouTube youtube;

    static {
        try {
            youtube = new YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), null)
                    .setApplicationName(Config.get("YOUTUBE_APPLICATION_NAME"))
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private YouTubeUtil() {} // Prevent instantiation

    /**
     * Search for a YouTube video based on the specified query and return its link.
     *
     * @param query The search query.
     * @return An array of links to the first (up to) 5 videos in the search results, or null if no videos were found.
     * @throws Exception If an error occurs while communicating with the YouTube Data API.
     */
    @Nullable
    public static List<SearchResult> searchForVideos(String query) throws Exception {
        List<SearchResult> searchResultList = youtube.search()
                .list(Arrays.asList("id", "snippet"))
                .setKey(API_KEY)
                .setQ(query)
                .setType(List.of("video"))
                .setMaxResults(5L)
                .setFields("items(id/videoId,snippet/channelId,snippet/title,snippet/channelTitle)")
                .execute()
                .getItems();

        if (searchResultList == null || searchResultList.isEmpty()) {
            return null;
        }

        return searchResultList;
    }

    /**
     * Search for a YouTube playlist based on the specified query and return its link.
     *
     * @param query The search query.
     * @return An array of links to the first (up to) 5 playlists in the search results, or null if no playlists were found.
     * @throws Exception If an error occurs while communicating with the YouTube Data API.
     */
    @Nullable
    public static List<SearchResult> searchForPlaylists(String query) throws Exception {
        List<SearchResult> searchResultList = youtube.search()
                .list(Arrays.asList("id", "snippet"))
                .setKey(API_KEY)
                .setQ(query)
                .setType(List.of("playlist"))
                .setMaxResults(5L)
                .setFields("items(id/playlistId,snippet/channelId,snippet/title,snippet/channelTitle)")
                .execute()
                .getItems();

        if (searchResultList == null || searchResultList.isEmpty()) {
            return null;
        }

        return searchResultList;
    }

    /**
     * Search for YouTube videos and playlists based on the specified query and return their links.
     *
     * @param query The search query.
     * @return An array of links to the videos and playlists in the search results, prioritized by relevance,
     *         or null if no results were found.
     * @throws Exception If an error occurs while communicating with the YouTube Data API.
     */
    @Nullable
    public static String[] searchForVideosAndPlaylists(String query) throws Exception {
        List<SearchResult> searchResultList = youtube.search()
                .list(Arrays.asList("id", "snippet"))
                .setKey(API_KEY)
                .setQ(query)
                .setType(Arrays.asList("video", "playlist"))
                .setMaxResults(5L)
                .setFields("items(id/videoId,id/playlistId)")
                .execute()
                .getItems();

        if (searchResultList == null || searchResultList.isEmpty()) {
            return null;
        }

        List<String> links = getStrings(searchResultList);

        return links.toArray(new String[0]);
    }

    private static @NotNull List<String> getStrings(List<SearchResult> searchResultList) {
        List<String> links = new ArrayList<>();
        for (SearchResult result : searchResultList) {
            if (result.getId().getVideoId() != null) {
                // It's a video
                String videoId = result.getId().getVideoId();
                links.add("https://www.youtube.com/watch?v=" + videoId);
            } else if (result.getId().getPlaylistId() != null) {
                // It's a playlist
                String playlistId = result.getId().getPlaylistId();
                links.add("https://www.youtube.com/playlist?list=" + playlistId);
            }
        }
        return links;
    }

    /**
     * Method to check if the given link is a YouTube link.
     *
     * @param youTubeURL the YouTube link
     * @return true if the given link is a YouTube link, false otherwise
     */
    public static boolean isYouTubeUrl(String youTubeURL) {
        String pattern = "(?:youtu\\.be/|youtube\\.com/watch\\?v=|youtube\\.com/videos/|youtube\\.com/embed/)[^#&?]*";
        return Pattern.compile(pattern).matcher(youTubeURL).find();
    }

    /**
     * @param youTubeURL the YouTube link
     * @return the id of the given YouTube link
     */
    @Nullable
    public static String getYouTubeID(String youTubeURL) {
        String pattern = "(?<=youtu.be/|watch\\?v=|/videos/|embed/)[^#&?]*";
        Matcher matcher = Pattern.compile(pattern).matcher(youTubeURL);
        if (matcher.find()) {
            return matcher.group();
        } else {
            return null;
        }
    }

    /**
     * @param youTubeURL the YouTube link
     * @return a BufferedImage with proper aspect ratio of YouTube thumbnail from given link
     */
    @Nullable
    public static BufferedImage getThumbnailImage(String youTubeURL) {
        // Return null if the link is not a YouTube link
        String id = getYouTubeID(youTubeURL);
        if (id == null) {
            return null;
        }

        // Define the desired aspect ratio
        double aspectRatio = 16.0 / 9.0;
        try {
            // Load the thumbnail image from the URL
            BufferedImage image = ImageIO.read(URI.create("https://img.youtube.com/vi/" + getYouTubeID(youTubeURL) + "/hqdefault.jpg").toURL());
            // Crop the image to the desired aspect ratio
            int width = image.getWidth();
            int height = image.getHeight();
            if ((double) width / (double) height > aspectRatio) {
                int newWidth = (int) (height * aspectRatio);
                int startX = (width - newWidth) / 2;
                image = image.getSubimage(startX, 0, newWidth, height);
            } else {
                int newHeight = (int) (width / aspectRatio);
                int startY = (height - newHeight) / 2;
                image = image.getSubimage(0, startY, width, newHeight);
            }
            return image;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param youTubeURL the YouTube link
     * @return the title of the video from the given link
     */
    public static String getTitle(String youTubeURL) {
        try {
            String id = getYouTubeID(youTubeURL);
            if (id == null) {
                return null;
            }

            List<Video> videoList = youtube.videos()
                    .list(List.of("snippet"))
                    .setKey(API_KEY)
                    .setId(List.of(id))
                    .setMaxResults(1L)
                    .execute()
                    .getItems();

            if (videoList == null || videoList.isEmpty()) {
                return null;
            }

            return videoList.getFirst().getSnippet().getTitle();
        } catch (Exception e) {
            return null;
        }
    }
}