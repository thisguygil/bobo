package bobo.commands.general;

import bobo.Config;
import bobo.utils.api_clients.GoogleCustomSearchService;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.google.api.services.customsearch.v1.model.Result;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static bobo.utils.StringUtils.markdownLink;

public class GoogleCommand extends AbstractGeneral {
    private static final Logger logger = LoggerFactory.getLogger(GoogleCommand.class);

    private static final String GOOGLE_API_KEY = Config.get("GOOGLE_API_KEY");
    private static final String SEARCH_ENGINE_ID = Config.get("SEARCH_ENGINE_ID");

    /**
     * Creates a new Google command.
     */
    public GoogleCommand() {
        super(Commands.slash("google", "Searches given query on Google.")
                .addSubcommands(
                        new SubcommandData("search", "Searches given query on Google.")
                                .addOption(OptionType.STRING, "query", "What to search", true),
                        new SubcommandData("images", "Searches for images on Google.")
                                .addOption(OptionType.STRING, "query", "What to search", true)
                )
        );
    }

    @Override
    protected void handleGeneralCommand() {
        event.deferReply().queue();

        String subcommand = event.getSubcommandName();

        switch (subcommand) {
            case "search" -> search();
            case "images" -> searchImages();
            default -> throw new IllegalArgumentException("Unexpected value: " + subcommand);
        }
    }

    /**
     * Searches the given query on Google.
     */
    private void search() {
        String query = Objects.requireNonNull(event.getOption("query")).getAsString();
        List<Result> results;

        try {
            results = GoogleCustomSearchService.search(query, false);
            if (results == null || results.isEmpty()) {
                hook.editOriginal("No results found for query: " + query).queue();
                return;
            }
        } catch (Exception e) {
            hook.editOriginal(e.getMessage()).queue();
            logger.error("Error occurred while searching Google for query: {}", query);
            return;
        }

        displayResults(results, query, false);
    }

    /**
     * Searches for images on Google.
     */
    private void searchImages() {
        String query = Objects.requireNonNull(event.getOption("query")).getAsString();
        List<Result> images;

        try {
            images = GoogleCustomSearchService.search(query, true);
            if (images == null || images.isEmpty()) {
                hook.editOriginal("No images found for query: " + query).queue();
                return;
            }
        } catch (Exception e) {
            hook.editOriginal(e.getMessage()).queue();
            logger.error("Error occurred while searching Google Images for query: {}", query);
            return;
        }

        displayResults(images, query, true);
    }

    /**
     * Displays the search results in a paginated Discord embed.
     *
     * @param results       The search results.
     * @param query         The search query.
     * @param isImageSearch Whether the search is for images.
     */
    private void displayResults(List<Result> results, String query, boolean isImageSearch) {
        final List<Page> pages = new ArrayList<>();
        Member member = event.getMember();
        assert member != null;

        if (isImageSearch) {
            for (int i = 0; i < results.size(); i++) {
                Result imageResult = results.get(i);
                String title = imageResult.getTitle();
                String imageUrl = imageResult.getLink();
                String imageContextUrl = imageResult.getImage().getContextLink();

                MessageEmbed embed = new EmbedBuilder()
                        .setAuthor(member.getUser().getGlobalName(), "https://discord.com/users/" + member.getId(), member.getEffectiveAvatarUrl())
                        .setTitle("Image Search Result for: " + query)
                        .setFooter("Page " + (i + 1) + "/" + results.size())
                        .setColor(Color.red)
                        .addField(title, imageContextUrl, false)
                        .setImage(imageUrl)
                        .build();
                pages.add(i, InteractPage.of(embed));
            }
        } else {
            int resultsPerPage = 5;
            for (int i = 0; i < results.size(); i += resultsPerPage) {
                EmbedBuilder builder = new EmbedBuilder()
                        .setAuthor(member.getUser().getGlobalName(), "https://discord.com/users/" + member.getId(), member.getEffectiveAvatarUrl())
                        .setTitle("Search Results for: " + query)
                        .setColor(Color.blue);

                for (int j = 0; j < resultsPerPage && i + j < results.size(); j++) {
                    Result searchResult = results.get(i + j);
                    String title = searchResult.getTitle();
                    String link = searchResult.getLink();
                    String snippet = searchResult.getSnippet();

                    builder.addField(title, markdownLink(snippet, link), false);
                }

                builder.setFooter("Page " + ((i / resultsPerPage) + 1) + "/" + ((results.size() + resultsPerPage - 1) / resultsPerPage));
                pages.add(InteractPage.of(builder.build()));
            }
        }

        if (pages.size() == 1) {
            hook.editOriginalEmbeds((MessageEmbed) pages.getFirst().getContent()).queue();
        } else {
            hook.editOriginalEmbeds((MessageEmbed) pages.getFirst().getContent()).queue(success -> Pages.paginate(success, pages, true));
        }
    }

    @Override
    public String getName() {
        return "google";
    }

    @Override
    public String getHelp() {
        return """
                Searches given query on Google.
                Usage: `/google <query>""";
    }

    @Override
    protected List<Permission> getGeneralCommandPermissions() {
        return new ArrayList<>();
    }
}