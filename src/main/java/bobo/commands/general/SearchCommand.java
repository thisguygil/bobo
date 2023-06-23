package bobo.commands.general;

import bobo.Config;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SearchCommand extends AbstractGeneral {
    private static final String GOOGLE_API_KEY = Config.get("GOOGLE_API_KEY");
    private static final String SEARCH_ENGINE_ID = Config.get("SEARCH_ENGINE_ID");

    @Override
    protected void handleGeneralCommand() {
        event.deferReply().queue();
        int numPages;
        JsonArray images;
        String query = Objects.requireNonNull(event.getOption("query")).getAsString();
        String url = "https://www.googleapis.com/customsearch/v1?key=" + GOOGLE_API_KEY + "&cx=" + SEARCH_ENGINE_ID +
                "&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&searchType=image";

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            images = json.getAsJsonArray("items");
            numPages = images.size();
            if (numPages == 0) {
                hook.editOriginal("No images found for query: " + query).queue();
                return;
            }
        } catch (Exception e) {
            hook.editOriginal(e.getMessage()).queue();
            e.printStackTrace();
            return;
        }

        final List<Page> pages = new ArrayList<>();
        MessageEmbed embed;
        Member member = event.getMember();
        assert member != null;
        for (int i = 0; i < numPages; i++) {
            JsonObject image = images.get(i).getAsJsonObject();
            String title = image.get("title").getAsString();
            String imageUrl = image.get("link").getAsString();
            String imageContextUrl = image.getAsJsonObject().get("image").getAsJsonObject().get("contextLink").getAsString();

            embed = new EmbedBuilder()
                    .setAuthor(member.getUser().getGlobalName(), "https://discord.com/users/" + member.getId(), member.getEffectiveAvatarUrl())
                    .setTitle("Search Results for: " + query)
                    .setFooter("Page " + (i + 1) + "/" + numPages)
                    .setColor(Color.red)
                    .addField(title, imageContextUrl, true)
                    .setImage(imageUrl)
                    .build();
            pages.add(i, InteractPage.of(embed));
        }

        if (pages.size() == 1) {
            hook.editOriginalEmbeds((MessageEmbed) pages.get(0).getContent()).queue();
        } else {
            hook.editOriginalEmbeds((MessageEmbed) pages.get(0).getContent()).queue(success -> Pages.paginate(success, pages, true));
        }
    }

    @Override
    public String getName() {
        return "search";
    }
}
