package bobo.commands.general;

import bobo.Bobo;
import bobo.utils.SQLConnection;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NOTE: ONLY WORKS WITH QUOTES OF THE FORMAT:
 * <p>
 * "quote" -author
 * <p>
 * There can be multiple quotes in one message, and blank space does not matter.
 */
public class RandomCommand extends AbstractGeneral {
    public static final Map<Guild, List<Message>> guildQuoteListMap = new HashMap<>();
    public static final Map<Guild, List<Message>> guildClipListMap = new HashMap<>();
    private static final String selectQuotesSQL = "SELECT channel_id FROM quotes_channels WHERE guild_id = ?";
    private static final String selectAllQuotesSQL = "SELECT * FROM quotes_channels";
    private static final String selectClipsSQL = "SELECT channel_id FROM clips_channels WHERE guild_id = ?";
    private static final String selectAllClipsSQL = "SELECT * FROM clips_channels";

    /**
     * Creates a new random command.
     */
    public RandomCommand() {
        super(Commands.slash("random", "Gets a random quote or clip from the configured channel.")
                .addSubcommands(
                        new SubcommandData("quote", "Gets a random quote from the configured quotes channel."),
                        new SubcommandData("clip", "Gets a random clip from the configured clips channel.")
                ));
    }

    @Override
    protected void handleGeneralCommand() {
        event.deferReply().queue();

        switch (Objects.requireNonNull(event.getSubcommandName())) {
            case "quote" -> randomQuote();
            case "clip" -> randomClip();
        }
    }

    /**
     * Gets a random quote from the configured quotes channel.
     */
    private void randomQuote() {
        Guild guild = event.getGuild();
        assert guild != null;
        try {
            loadGuildQuotes(guild);
        } catch (SQLException e) {
            e.printStackTrace();
            hook.editOriginal("An error occurred while getting the quote.").queue();
            return;
        }

        List<Message> guildList = guildQuoteListMap.get(guild);
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
    private static void loadGuildQuotes(@Nonnull Guild guild) throws SQLException {
        GuildChannel channel;
        GuildMessageChannel messageChannel;
        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectQuotesSQL)) {
            statement.setString(1, guild.getId());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                channel = Bobo.getJDA().getGuildChannelById(resultSet.getString("channel_id"));
            } else {
                channel = null;
            }
        }

        if (channel == null) {
            return;
        }

        messageChannel = (GuildMessageChannel) channel;
        List<Message> messages = guildQuoteListMap.computeIfAbsent(guild, k -> new ArrayList<>());
        for (Message message : messageChannel.getIterableHistory()) {
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
    public static void loadQuotesMap() {
        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectAllQuotesSQL)) {
            ResultSet resultSet = statement.executeQuery();
            JDA jda = Bobo.getJDA();
            while (resultSet.next()) {
                Guild guild = jda.getGuildById(resultSet.getString("guild_id"));
                GuildChannel channel = jda.getGuildChannelById(resultSet.getString("channel_id"));
                assert guild != null;
                assert channel != null;

                List<Message> messages = new ArrayList<>();
                GuildMessageChannel messageChannel = (GuildMessageChannel) channel;
                for (Message message : messageChannel.getIterableHistory()) {
                    if (message.getContentDisplay().contains("\"")) {
                        messages.add(message);
                    }
                }
                guildQuoteListMap.put(guild, messages);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets a random clip from the configured clips channel
     */
    private void randomClip() {
        Guild guild = event.getGuild();
        assert guild != null;
        try {
            loadGuildClips(guild);
        } catch (SQLException e) {
            e.printStackTrace();
            hook.editOriginal("An error occurred while getting the clip.").queue();
            return;
        }

        List<Message> guildList = guildClipListMap.get(guild);
        if (guildList == null) {
            hook.editOriginal("No clips channel has been configured for this server. Configure one with **/config clips**").queue();
            return;
        }
        if (guildList.isEmpty()) {
            hook.editOriginal("No clips have been added to the clips channel.").queue();
            return;
        }

        int randomIndex = (int) (Math.random() * guildList.size());
        Message randomMessage = guildList.get(randomIndex);
        hook.editOriginal(randomMessage.getAttachments().get(0).getUrl()).queue();
    }

    /**
     * Loads all clips from the clips channel of the given guild into the map
     * Since all clips are loaded when the bot starts, this is for when new clips are added or new channels are configured
     *
     * @param guild the guild to load clips from
     */
    private static void loadGuildClips(Guild guild) throws SQLException {
        GuildChannel channel;
        GuildMessageChannel messageChannel;
        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectClipsSQL)) {
            statement.setString(1, guild.getId());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                channel = Bobo.getJDA().getGuildChannelById(resultSet.getString("channel_id"));
            } else {
                channel = null;
            }
        }

        if (channel == null) {
            return;
        }

        messageChannel = (GuildMessageChannel) channel;
        List<Message> messages = guildClipListMap.computeIfAbsent(guild, k -> new ArrayList<>());
        for (Message message : messageChannel.getIterableHistory()) {
            List<Message.Attachment> attachments = message.getAttachments();
            if (!attachments.isEmpty()) {
                if (Objects.equals(attachments.get(0).getFileExtension(), "wav")) {
                    messages.add(message);
                }
            }
        }
    }

    /**
     * Loads all clips from all clips channels into the map
     */
    public static void loadClipsMap() {
        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectAllClipsSQL)) {
            ResultSet resultSet = statement.executeQuery();
            JDA jda = Bobo.getJDA();
            while (resultSet.next()) {
                Guild guild = jda.getGuildById(resultSet.getString("guild_id"));
                GuildChannel channel = jda.getGuildChannelById(resultSet.getString("channel_id"));
                assert guild != null;
                assert channel != null;

                List<Message> messages = new ArrayList<>();
                GuildMessageChannel messageChannel = (GuildMessageChannel) channel;
                for (Message message : messageChannel.getIterableHistory()) {
                    List<Message.Attachment> attachments = message.getAttachments();
                    if (!attachments.isEmpty()) {
                        if (attachments.get(0).getFileExtension().equals("wav")) {
                            messages.add(message);
                        }
                    }
                }
                guildClipListMap.put(guild, messages);
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
        return "random";
    }

    @Override
    public String getHelp() {
        return """
                Gets a random quote from the configured quotes channel.
                Usage: `/random <subcommand>`
                Subcommands:
                * quote: Gets a random quote from the configured quotes channel.
                * clip: Gets a random clip from the configured clips channel.""";
    }
}
