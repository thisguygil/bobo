package bobo.utils;

import bobo.Config;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchResult;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class YouTubeUtil {
    private static final String API_KEY = Config.get("GOOGLE_API_KEY");

    private YouTubeUtil() {} // Prevent instantiation

    /**
     * Search for a YouTube video based on the specified query and return its link.
     *
     * @param query The search query.
     * @return An array of links to the first (up to) 5 videos in the search results, or null if no videos were found.
     * @throws Exception If an error occurs while communicating with the YouTube Data API.
     */
    @Nullable
    public static String[] searchForVideos(String query) throws Exception {
        YouTube youtube = new YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), null)
                .setApplicationName("Bobo")
                .build();

        List<SearchResult> searchResultList = youtube.search().list(Arrays.asList("id", "snippet"))
                .setKey(API_KEY)
                .setQ(query)
                .setType(List.of("video"))
                .setMaxResults(5L)
                .setFields("items(id/videoId)")
                .execute()
                .getItems();

        if (searchResultList == null || searchResultList.isEmpty()) {
            return null;
        }

        String[] videoLinks = new String[searchResultList.size()];
        for (int i = 0; i < searchResultList.size(); i++) {
            SearchResult result = searchResultList.get(i);
            String videoId = result.getId().getVideoId();
            videoLinks[i] = "https://www.youtube.com/watch?v=" + videoId;
        }

        return videoLinks;
    }

    /**
     * Search for a YouTube playlist based on the specified query and return its link.
     *
     * @param query The search query.
     * @return An array of links to the first (up to) 5 playlists in the search results, or null if no playlists were found.
     * @throws Exception If an error occurs while communicating with the YouTube Data API.
     */
    @Nullable
    public static String[] searchForPlaylists(String query) throws Exception {
        YouTube youtube = new YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), null)
                .setApplicationName("Bobo")
                .build();

        List<SearchResult> searchResultList = youtube.search().list(Arrays.asList("id", "snippet"))
                .setKey(API_KEY)
                .setQ(query)
                .setType(List.of("playlist"))
                .setMaxResults(5L)
                .setFields("items(id/playlistId)")
                .execute()
                .getItems();

        if (searchResultList == null || searchResultList.isEmpty()) {
            return null;
        }

        String[] playlistLinks = new String[searchResultList.size()];
        for (int i = 0; i < searchResultList.size(); i++) {
            SearchResult result = searchResultList.get(i);
            String playlistId = result.getId().getPlaylistId();
            playlistLinks[i] = "https://www.youtube.com/playlist?list=" + playlistId;
        }

        return playlistLinks;
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
     * @return a String with the title of the YouTube video from given link
     */
    @Nullable
    public static String getVideoTitle(String youTubeURL) {
        try {
            YouTube youtube = new YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), null)
                    .setApplicationName("Bobo")
                    .build();

            List<SearchResult> searchResultList = youtube.search().list(Arrays.asList("id", "snippet"))
                    .setKey(API_KEY)
                    .setQ(getYouTubeID(youTubeURL))
                    .setType(List.of("video"))
                    .setMaxResults((long) 1)
                    .setFields("items(id/videoId,snippet/title)")
                    .execute()
                    .getItems();

            if (searchResultList == null || searchResultList.isEmpty()) {
                return null;
            }

            SearchResult result = searchResultList.get(0);
            String title = result.getSnippet().getTitle();

            return StringEscapeUtils.unescapeHtml4(title);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param youTubeURL the YouTube link
     * @return a String with the title of the YouTube playlist from given link
     */
    @Nullable
    public static String getPlaylistTitle(String youTubeURL) {
        try {
            YouTube youtube = new YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), null)
                    .setApplicationName("Bobo")
                    .build();

            List<SearchResult> searchResultList = youtube.search().list(Arrays.asList("id", "snippet"))
                    .setKey(API_KEY)
                    .setQ(getYouTubeID(youTubeURL))
                    .setType(List.of("playlist"))
                    .setMaxResults((long) 1)
                    .setFields("items(id/playlistId,snippet/title)")
                    .execute()
                    .getItems();

            if (searchResultList == null || searchResultList.isEmpty()) {
                return null;
            }

            SearchResult result = searchResultList.get(0);
            String title = result.getSnippet().getTitle();

            return StringEscapeUtils.unescapeHtml4(title);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}