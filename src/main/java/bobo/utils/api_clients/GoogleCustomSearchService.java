package bobo.utils.api_clients;

import bobo.Config;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.customsearch.v1.CustomSearchAPI;
import com.google.api.services.customsearch.v1.model.Result;
import com.google.api.services.customsearch.v1.model.Search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class GoogleCustomSearchService {
    private static final String APPLICATION_NAME = "Bobo";
    private static final String GOOGLE_API_KEY = Config.get("GOOGLE_API_KEY");
    private static final String SEARCH_ENGINE_ID = Config.get("SEARCH_ENGINE_ID");

    private static final CustomSearchAPI customSearch = new CustomSearchAPI.Builder(
            new NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            null
    ).setApplicationName(APPLICATION_NAME)
            .build();

    private GoogleCustomSearchService() {} // Prevent instantiation

    /**
     * Searches Google for the given query.
     *
     * @param query the query to search for
     * @param isImageSearch whether to search for images
     * @param postedAfter only return results after this year
     * @param postedBefore only return results before this year
     * @param maxResults the maximum number of results to return
     * @return the search results
     * @throws IOException if an error occurs while searching
     */
    public static List<Result> search(
            String query,
            boolean isImageSearch,
            Integer postedAfter,
            Integer postedBefore,
            int maxResults
    ) throws IOException {

        if (maxResults < 1 || maxResults > 100) {
            throw new IllegalArgumentException("maxResults must be between 1 and 100.");
        }

        StringBuilder finalQuery = new StringBuilder(query);

        if (postedAfter != null) {
            finalQuery.append(" after:").append(postedAfter);
        }

        if (postedBefore != null) {
            finalQuery.append(" before:").append(postedBefore);
        }

        List<Result> allResults = new ArrayList<>();

        for (int start = 1; start <= maxResults; start += 10) {
            int remaining = maxResults - allResults.size();
            int numResults = Math.min(remaining, 10);

            CustomSearchAPI.Cse.List list = customSearch.cse()
                    .list()
                    .setQ(finalQuery.toString())
                    .setKey(GOOGLE_API_KEY)
                    .setCx(SEARCH_ENGINE_ID)
                    .setStart((long) start)
                    .setNum(numResults);

            if (isImageSearch) {
                list.setSearchType("image");
            }

            Search response = list.execute();

            if (response.getItems() == null || response.getItems().isEmpty()) {
                break;
            }

            allResults.addAll(response.getItems());

            // Stop if fewer results than requested were returned
            if (response.getItems().size() < numResults) {
                break;
            }
        }

        return allResults;
    }
}
