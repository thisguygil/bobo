package bobo.command.commands;

import bobo.Config;
import bobo.command.ICommand;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import javax.annotation.Nonnull;
import java.awt.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class SearchCommand implements ICommand {
    private static final String API_KEY = Config.get("GOOGLE_API_KEY");
    private static final String SEARCH_ENGINE_ID = Config.get("SEARCH_ENGINE_ID");

    private JsonArray items;
    private int totalPages;
    private MessageEmbed currentEmbed;

    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        String query = Objects.requireNonNull(event.getOption("query")).getAsString();
        try {
            String url = "https://www.googleapis.com/customsearch/v1?key=" + API_KEY +
                    "&cx=" + SEARCH_ENGINE_ID + "&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8) +
                    "&searchType=image";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

            items = json.getAsJsonArray("items");
            totalPages = items.size();
            if (totalPages == 0) {
                event.reply("No images found for query: " + query).queue();
                return;
            }

            sendPage(event, 1);

        } catch (Exception e) {
            event.reply("Error: " + e.getMessage()).queue();
        }
    }

    private void sendPage(@Nonnull SlashCommandInteractionEvent event, int page) {
        Member member = event.getMember();
        JsonObject item = items.get(page - 1).getAsJsonObject();
        String title = item.get("title").getAsString();
        String imageUrl = item.get("link").getAsString();
        String imageContextUrl = item.getAsJsonObject().get("image").getAsJsonObject().get("contextLink").getAsString();

        assert member != null;
        MessageEmbed embed = new EmbedBuilder()
                .setAuthor(member.getUser().getAsTag(), "https://discord.com/users/" + member.getId(), member.getAvatarUrl())
                .setTitle("Search Results")
                .setFooter("Page " + page + "/" + totalPages)
                .setColor(Color.red)
                .addField(title, imageContextUrl, true)
                .setImage(imageUrl)
                .build();

        Button prevButton = Button.secondary("prev", "Prev").withDisabled(page == 1);
        Button nextButton = Button.secondary("next", "Next").withDisabled(page == totalPages);

        if (currentEmbed == null) {
            event.replyEmbeds(embed).addActionRow(prevButton, nextButton).queue();
        } else {
            event.getHook().editOriginalEmbeds(embed).queue();
        }
        currentEmbed = embed;
    }

    public void handleButtonClick(@Nonnull ButtonInteractionEvent event, int currentPage) {
        String buttonId = event.getComponentId();

        if (buttonId.equals("prev")) currentPage--;
        else if (buttonId.equals("next")) currentPage++;

        //sendPage(currentPage);
        event.deferEdit().queue();
    }

    @Override
    public String getName() {
        return "search";
    }

    @Override
    public String getHelp() {
        return """
                `/search`
                Search given query on Google
                Usage: `/search <query>`""";
    }
}
