package bobo.commands.general;

import bobo.Bobo;
import bobo.utils.SQLConnection;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NOTE: ONLY WORKS WITH QUOTES OF THE FORMAT:
 * <p>
 * "quote" -author
 * <p>
 * There can be multiple quotes in one message, and blank space does not matter.
 */
public class GetQuoteCommand extends AbstractGeneral {
    public static final Map<Guild, List<Message>> guildListMap = new HashMap<>();
    private static final String selectSQL = "SELECT channel_id FROM quotes_channels WHERE guild_id = ?";
    private static final String selectAllSQL = "SELECT * FROM quotes_channels";


    /**
     * Creates a new get-quote command.
     */
    public GetQuoteCommand() {
        super(Commands.slash("get-quote", "Gets a random quote from the configured quotes channel."));
    }

    @Override
    protected void handleGeneralCommand() {
        event.deferReply().queue();

        Guild guild = event.getGuild();
        assert guild != null;
        try {
            loadGuild(guild);
        } catch (SQLException e) {
            e.printStackTrace();
            hook.editOriginal("An error occurred while getting the quote.").queue();
            return;
        }

        List<Message> guildList = guildListMap.get(guild);
        if (guildList == null) {
            hook.editOriginal("No quotes channel has been configured for this server. Configure one with **/config quotes**").queue();
            return;
        }
        if (guildList.isEmpty()) {
            hook.editOriginal("No quotes have been added to the quotes channel.").queue();
            return;
        }

        int randomIndex = (int) (Math.random() * guildList.size());
        Message randomMessage = guildList.get(randomIndex);
        String messageContent = spoileredQuote(randomMessage.getContentDisplay());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        String time = randomMessage.getTimeCreated().format(formatter);

        hook.editOriginal(messageContent + "\n" + time).queue();
    }

    /**
     * Loads all quotes from the quotes channel of the given guild into the map
     * Since all quotes are loaded when the bot starts, this is for when new quotes are added or new channels are configured
     *
     * @param guild the guild to load quotes from
     */
    private static void loadGuild(@Nonnull Guild guild) throws SQLException {
        TextChannel channel;
        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectSQL)) {
            statement.setString(1, guild.getId());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                channel = Bobo.getJDA().getTextChannelById(resultSet.getString("channel_id"));
            } else {
                channel = null;
            }
        }

        if (channel == null) {
            return;
        }

        List<Message> messages = guildListMap.computeIfAbsent(guild, k -> new ArrayList<>());
        for (Message message : channel.getIterableHistory()) {
            if (!messages.contains(message)) {
                if (message.getContentDisplay().contains("\"")) {
                    messages.add(message);
                }
            } else {
                break;
            }
        }
    }

    /**
     * Loads all quotes from all quotes channels into the map
     */
    public static void loadMap() {
        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectAllSQL)) {
            ResultSet resultSet = statement.executeQuery();
            JDA jda = Bobo.getJDA();
            while (resultSet.next()) {
                Guild guild = jda.getGuildById(resultSet.getString("guild_id"));
                TextChannel channel = jda.getTextChannelById(resultSet.getString("channel_id"));
                assert guild != null;
                assert channel != null;

                List<Message> messages = new ArrayList<>();
                for (Message message : channel.getIterableHistory()) {
                    if (message.getContentDisplay().contains("\"")) {
                        messages.add(message);
                    }
                }
                guildListMap.put(guild, messages);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Encapsulates a quote's speaker by "||", which spoilers the text in Discord
     *
     * @param quote the quote to be spoilered
     * @return the spoilered quote
     */
    @Nonnull
    private static String spoileredQuote(String quote) {
        String regex = "(\".*?\")\\s*-\\s*(.*)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(quote);
        StringBuilder formattedQuote = new StringBuilder();
        String speaker;
        while (matcher.find()) {
            speaker = matcher.group(2);
            matcher.appendReplacement(formattedQuote, "$1\n-||" + speaker + "||");
        }
        matcher.appendTail(formattedQuote);
        return formattedQuote.toString();
    }

    @Override
    public String getName() {
        return "get-quote";
    }

    @Override
    public String getHelp() {
        return """
                Gets a random quote from the configured quotes channel.
                Usage: `/get-quote`
                Note: Quotes channel must be configured with `/config quotes`.""";
    }
}
