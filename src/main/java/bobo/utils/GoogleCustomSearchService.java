package bobo.utils;

import bobo.Config;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.customsearch.v1.CustomSearchAPI;
import com.google.api.services.customsearch.v1.model.Result;
import com.google.api.services.customsearch.v1.model.Search;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

public final class GoogleCustomSearchService {
    private static final String APPLICATION_NAME = "Bobo";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String GOOGLE_API_KEY = Config.get("GOOGLE_API_KEY");
    private static final String SEARCH_ENGINE_ID = Config.get("SEARCH_ENGINE_ID");
    private static final CustomSearchAPI customSearch;

    static {
        try {
            customSearch = new CustomSearchAPI.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JSON_FACTORY, null)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private GoogleCustomSearchService() {} // Prevent instantiation

    /**
     * Searches for the given query.
     *
     * @param query the query to search for
     * @param isImageSearch whether to search for images
     * @return the search results
     * @throws IOException if an error occurs while searching
     */
    public static List<Result> search(String query, boolean isImageSearch) throws IOException {
        CustomSearchAPI.Cse.List list = customSearch.cse()
                .list()
                .setQ(query)
                .setKey(GOOGLE_API_KEY)
                .setCx(SEARCH_ENGINE_ID);

        if (isImageSearch) {
            list.setSearchType("image");
        }

        Search results = list.execute();
        return results.getItems();
    }
}
