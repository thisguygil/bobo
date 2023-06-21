package bobo.utils;

import bobo.Config;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class YouTubeUtil {
    private static final String API_KEY = Config.get("GOOGLE_API_KEY");

    /**
     * Search for a YouTube video based on the specified query and return its link.
     *
     * @param query The search query.
     * @return The link to the first video in the search results, or null if no videos were found.
     * @throws Exception If an error occurs while communicating with the YouTube Data API.
     */
    public static String searchForVideo(String query) throws Exception {
        YouTube youtube = new YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), null)
                .setApplicationName("Bobo")
                .build();

        List<SearchResult> searchResultList = youtube.search().list(Arrays.asList("id", "snippet"))
                .setKey(API_KEY)
                .setQ(query)
                .setType(List.of("video"))
                .setMaxResults((long) 1)
                .setFields("items(id/videoId)")
                .execute()
                .getItems();

        if (searchResultList == null || searchResultList.isEmpty()) {
            return null;
        }

        SearchResult firstResult = searchResultList.get(0);
        String videoId = firstResult.getId().getVideoId();

        return "https://www.youtube.com/watch?v=" + videoId;
    }

    /**
     * @param youTubeURL the YouTube link
     * @return the id of the given YouTube link
     */
    public static String getYouTubeID(String youTubeURL) {
        String pattern = "(?<=youtu.be/|watch\\?v=|/videos/|embed/)[^#&?]*";
        Matcher matcher = Pattern.compile(pattern).matcher(youTubeURL);
        if (matcher.find()) {
            return matcher.group();
        } else {
            return "error";
        }
    }

    /**
     * @param youTubeURL the YouTube link
     * @return a BufferedImage with proper aspect ratio of YouTube thumbnail from given link
     */
    public static BufferedImage getThumbnailImage(String youTubeURL) {
        // Define the desired aspect ratio
        double aspectRatio = 16.0 / 9.0;
        try {
            // Load the thumbnail image from the URL
            BufferedImage image = ImageIO.read(new URL("https://img.youtube.com/vi/" + getYouTubeID(youTubeURL) + "/hqdefault.jpg"));
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
     * @param url the URL
     * @return true if the given URL is a valid URL
     */
    public static boolean isValidURL(String url) {
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}