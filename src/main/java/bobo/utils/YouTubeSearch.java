package bobo.utils;

import bobo.Config;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchResult;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YouTubeSearch {
    /**
     * Search for a YouTube video based on the specified query and return its link.
     *
     * @param query The search query.
     * @return The link to the first video in the search results, or null if no videos were found.
     * @throws Exception If an error occurs while communicating with the YouTube Data API.
     */
    public static String searchForVideo(String query) throws Exception {
        YouTube youtube = new YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), null)
                .setApplicationName("Bobo")
                .build();

        List<SearchResult> searchResultList = youtube.search().list("id, snippet")
                .setKey(Config.get("YOUTUBE_KEY"))
                .setQ(query)
                .setType("video")
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
     * @param youTubeURL the string to check if it's a link
     * @return true if given string is a YouTube link, false otherwise
     */
    public static boolean isYouTubeURL(String youTubeURL) {
        String pattern = "^(http(s)?:\\/\\/)?((w){3}.)?youtu(be|.be)?(\\.com)?\\/.+";
        return !youTubeURL.isEmpty() && youTubeURL.matches(pattern);
    }

    /**
     * @param youTubeURL the YouTube link
     * @return the id of the given YouTube link
     */
    public static String getYouTubeID(String youTubeURL) {
        String pattern = "(?<=youtu.be/|watch\\?v=|/videos/|embed\\/)[^#\\&\\?]*";
        Matcher matcher = Pattern.compile(pattern).matcher(youTubeURL);
        if (matcher.find()) {
            return matcher.group();
        } else {
            return "error";
        }
    }

    /**
     * @param youTubeURL the YouTube link
     * @return a link to a thumbnail of the given video
     */
    public static String getThumbnailURL(String youTubeURL) {
        return "https://img.youtube.com/vi/" + getYouTubeID(youTubeURL) + "/0.jpg";
    }
}