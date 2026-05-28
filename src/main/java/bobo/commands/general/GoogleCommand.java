package bobo.commands.general;

import bobo.commands.CommandResponse;
import bobo.utils.api_clients.GoogleCustomSearchService;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.google.api.services.customsearch.v1.model.Result;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static bobo.utils.StringUtils.markdownLink;

public class GoogleCommand extends AGeneralCommand {
    private static final Logger logger = LoggerFactory.getLogger(GoogleCommand.class);
    private static final List<OptionData> googleSearchOptions = List.of(
            new OptionData(OptionType.STRING, "query", "What to search", true),
            new OptionData(OptionType.INTEGER, "after", "Only results after this year"),
            new OptionData(OptionType.INTEGER, "before", "Only results before this year"),
            new OptionData(OptionType.INTEGER, "results", "Number of results (1-100, default 25)")
                    .setRequiredRange(1, 100)
    );

    /**
     * Creates a new Google command.
     */
    public GoogleCommand() {
        super(Commands.slash("google", "Searches given query on Google.")
                .addSubcommands(
                        new SubcommandData("search", "Searches given query on Google.")
                                .addOptions(googleSearchOptions),
                        new SubcommandData("images", "Searches for images on Google.")
                                .addOptions(googleSearchOptions)
                )
        );
    }

    @Override
    protected CommandResponse handleGeneralCommand() {
        if (isImageOnlyAlias()) {
            return search(true, 0);
        }

        String subcommand;
        try {
            subcommand = getSubcommandName(0);
        } catch (Exception e) {
            return CommandResponse.text("Invalid usage. Use `/help google` for more information.");
        }

        int argIndex = source == CommandSource.MESSAGE_COMMAND ? 1 : 0;
        return switch (subcommand.toLowerCase()) {
            case "search" -> search(false, argIndex);
            case "images" -> search(true, argIndex);
            default -> source == CommandSource.MESSAGE_COMMAND
                    ? search(false, 0)
                    : CommandResponse.text("Invalid usage. Use `/help google` for more information.");
        };
    }

    /**
     * Searches the given query on Google.
     *
     * @param isImageSearch Whether the search should return images or web pages.
     * @param argIndex What message array index to start looking at message command arguments
     * @return The command response
     */
    private CommandResponse search(boolean isImageSearch, int argIndex) {
        String query;
        try {
            query = getMultiwordOptionValue("query", argIndex);
        } catch (Exception e) {
            return CommandResponse.text(
                    "Invalid usage. Use `/help google` for more information."
            );
        }

        Integer after = getIntegerOption("after", null);
        Integer before = getIntegerOption("before", null);
        int maxResults = getIntegerOption("results", 25);

        if (after != null && before != null && after > before) {
            return CommandResponse.text("`after` cannot be greater than `before`.");
        }

        List<Result> results;
        try {
            results = GoogleCustomSearchService.search(
                    query,
                    isImageSearch,
                    after,
                    before,
                    maxResults
            );

            if (results.isEmpty()) {
                return CommandResponse.text("No %s found for query: %s",
                                isImageSearch ? "images" : "results",
                                query
                );
            }
        } catch (Exception e) {
            logger.error("Error occurred while searching Google{} for query: {}",
                    isImageSearch ? " Images" : "",
                    query,
                    e
            );

            return CommandResponse.text(e.getMessage());
        }

        return getResults(results, query, isImageSearch, before, after);
    }

    /**
     * Displays the search results in a paginated Discord embed.
     *
     * @param results       The search results.
     * @param query         The search query.
     * @param isImageSearch Whether the search is for images.
     * @param before        The before year filtered by the user
     * @param after         The after year filtered by the user
     * @return The command response
     */
    private CommandResponse getResults(List<Result> results, String query, boolean isImageSearch, Integer before, Integer after) {
        final List<Page> pages = new ArrayList<>();
        Member member = getMember();
        assert member != null;

        String range;
        if (before != null && after != null) {
            range = String.format(" [%d-%d]", after, before);
        } else if (before != null) {
            range = String.format(" [before %d]", before);
        } else if (after != null) {
            range = String.format(" [after %d]", after);
        } else {
            range = "";
        }

        if (isImageSearch) {
            for (int i = 0; i < results.size(); i++) {
                Result imageResult = results.get(i);
                String title = imageResult.getTitle();
                String imageUrl = imageResult.getLink();
                String imageContextUrl = imageResult.getImage().getContextLink();

                MessageEmbed embed = new EmbedBuilder()
                        .setTitle("\uD83D\uDDBC️ Google Images - " + query + range)
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
                        .setTitle("\uD83D\uDD0E Google Search - " + query + range)
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
            return CommandResponse.embed((MessageEmbed) pages.getFirst().getContent());
        } else {
            return CommandResponse.builder()
                    .addEmbeds((MessageEmbed) pages.getFirst().getContent())
                    .setPostExecutionFromMessage(success -> Pages.paginate(success, pages, true))
                    .build();
        }
    }

    /**
     * Helper method that tells if the alias used was the one for the images subcommand
     *
     * @return true if the alias used was the one for the images subcommand
     */
    private boolean isImageOnlyAlias() {
        return source == CommandSource.MESSAGE_COMMAND
                && (command.equalsIgnoreCase("img") || command.equalsIgnoreCase("image"));
    }

    @Override
    public String getName() {
        return "google";
    }

    @Override
    public String getHelp() {
        return """
                Searches given query on Google.
                Usage: `/google <subcommand> <query>`
                Subcommands:
                * `search` - Searches given query on Google.
                * `images` - Searches given query on Google Images.""";
    }

    @Override
    public Boolean isHidden() {
        return false;
    }

    @Override
    public List<String> getAliases() {
        return List.of("img", "image"); // Technically aliases for the whole Google command, but meant to only work for images
    }
}