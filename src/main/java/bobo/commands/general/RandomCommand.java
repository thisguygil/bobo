package bobo.commands.general;

import bobo.Bobo;
import bobo.commands.CommandResponse;
import bobo.utils.api_clients.SQLConnection;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
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
public class RandomCommand extends AGeneralCommand {
    private static final Logger logger = LoggerFactory.getLogger(RandomCommand.class);

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
    protected CommandResponse handleGeneralCommand() {
        String subcommand;
        try {
            subcommand = getSubcommandName(0);
        } catch (Exception e) {
            return CommandResponse.text("Invalid usage. Use `/help random` for more information.");
        }

        return switch (subcommand) {
            case "quote" -> randomQuote();
            case "clip" -> randomClip();
            default -> CommandResponse.text("Invalid usage. Use `/help random` for more information.");
        };
    }

    /**
     * Gets a random quote from the configured quotes channel.
     */
    private CommandResponse randomQuote() {
        Guild guild = getGuild();

        try {
            loadGuildQuotes(guild);
        } catch (SQLException e) {
            logger.warn("Failed to load quotes for guild {}.", guild.getId());
            return CommandResponse.text("An error occurred while getting the quote.");
        }

        List<Message> guildList = guildQuoteListMap.get(guild);
        if (guildList == null) {
            return CommandResponse.text("No quotes channel has been configured for this server. Configure one with `/config quotes`");
        }
        if (guildList.isEmpty()) {
            return CommandResponse.text("No quotes have been added to the quotes channel.");
        }

        int randomIndex = (int) (Math.random() * guildList.size());
        Message randomMessage = guildList.get(randomIndex);
        String messageContent = spoileredQuote(randomMessage.getContentDisplay());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        String time = randomMessage.getTimeCreated().format(formatter);

        return CommandResponse.text(messageContent + "\n" + time);
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
        Guild guild;
        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectAllQuotesSQL)) {
            ResultSet resultSet = statement.executeQuery();
            JDA jda = Bobo.getJDA();
            while (resultSet.next()) {
                guild = jda.getGuildById(resultSet.getString("guild_id"));
                GuildChannel channel = jda.getGuildChannelById(resultSet.getString("channel_id"));
                if (guild == null || channel == null) {
                    continue;
                }

                List<Message> messages = new ArrayList<>();
                GuildMessageChannel messageChannel = (GuildMessageChannel) channel;
                for (Message message : messageChannel.getIterableHistory()) {
                    if (message.getContentDisplay().contains("\"")) {
                        messages.add(message);
                    }
                }
                guildQuoteListMap.put(guild, messages);
            }
            logger.info("Quotes map loaded.");
        } catch (SQLException e) {
            logger.warn("Failed to load quotes map.");
        }
    }

    /**
     * Gets a random clip from the configured clips channel
     */
    private CommandResponse randomClip() {
        Guild guild = getGuild();

        try {
            loadGuildClips(guild);
        } catch (SQLException e) {
            logger.error("Failed to get clip.");
            return CommandResponse.text("An error occurred while getting the clip.");
        }

        List<Message> guildList = guildClipListMap.get(guild);
        if (guildList == null) {
            return CommandResponse.text("No clips channel has been configured for this server. Configure one with `/config clips`");
        }
        if (guildList.isEmpty()) {
            return CommandResponse.text("No clips have been added to the clips channel.");
        }

        int randomIndex = (int) (Math.random() * guildList.size());
        Message randomMessage = guildList.get(randomIndex);
        Message.Attachment attachment = randomMessage.getAttachments().getFirst();

        // Download the attachment and re-upload it
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             InputStream inputStream = downloadFile(attachment.getUrl())) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }

            byte[] fileData = byteArrayOutputStream.toByteArray();
            FileUpload fileUpload = FileUpload.fromData(fileData, attachment.getFileName());

            return CommandResponse.builder()
                    .addAttachments(fileUpload).build();
        } catch (Exception e) {
            logger.error("Failed to send clip as attachment.", e);
            return CommandResponse.text("Failed to send the clip.");
        }
    }

    /**
     * Helper method to download a file from a URL
     *
     * @param fileUrl the URL of the file to download
     * @return the input stream of the downloaded file
     * @throws Exception if the file cannot be downloaded
     */
    private InputStream downloadFile(String fileUrl) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URI(fileUrl).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setDoInput(true);
        return connection.getInputStream();
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
        List<Message> messages = guildClipListMap.computeIfAbsent(guild, _ -> new ArrayList<>());
        for (Message message : messageChannel.getIterableHistory()) {
            List<Message.Attachment> attachments = message.getAttachments();
            if (!attachments.isEmpty()) {
                if (Objects.equals(attachments.getFirst().getFileExtension(), "wav")) {
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
                        if (Objects.equals(attachments.getFirst().getFileExtension(), "wav")) {
                            messages.add(message);
                        }
                    }
                }
                guildClipListMap.put(guild, messages);
            }
            logger.info("Clips map loaded.");
        } catch (SQLException e) {
            logger.warn("Failed to load clips map.");
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
        String regex = "(\".*?\"\\s*.*?)\\s*-\\s*(.*)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(quote);
        StringBuilder formattedQuote = new StringBuilder();
        String originalQuote, speaker;
        while (matcher.find()) {
            originalQuote = matcher.group(1).trim();
            speaker = matcher.group(2).trim();
            String replacement = String.format("%s\n-||%s||", originalQuote, speaker);
            matcher.appendReplacement(formattedQuote, replacement);
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

    @Override
    protected List<Permission> getGeneralCommandPermissions() {
        return new ArrayList<>(List.of(Permission.MESSAGE_HISTORY, Permission.MESSAGE_ATTACH_FILES));
    }

    @Override
    public Boolean shouldBeInvisible() {
        return false;
    }
}
