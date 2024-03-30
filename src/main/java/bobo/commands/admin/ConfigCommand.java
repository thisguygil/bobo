package bobo.commands.admin;

import bobo.Bobo;
import bobo.commands.general.GetQuoteCommand;
import bobo.utils.SQLConnection;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

public class ConfigCommand extends AbstractAdmin {
    private static final String createClipsTableSQL = "CREATE TABLE IF NOT EXISTS clips_channels (guild_id VARCHAR(255) PRIMARY KEY, channel_id VARCHAR(255) NOT NULL)";
    private static final String insertOrUpdateClipsSQL = "INSERT INTO clips_channels (guild_id, channel_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE channel_id = ?";
    private static final String createQuotesTableSQL = "CREATE TABLE IF NOT EXISTS quotes_channels (guild_id VARCHAR(255) PRIMARY KEY, channel_id VARCHAR(255) NOT NULL)";
    private static final String insertOrUpdateQuotesSQL = "INSERT INTO quotes_channels (guild_id, channel_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE channel_id = ?";
    private static final String createFortniteShopTableSQL = "CREATE TABLE IF NOT EXISTS fortnite_shop_channels (guild_id VARCHAR(255) PRIMARY KEY, channel_id VARCHAR(255) NOT NULL)";
    private static final String insertOrUpdateFortniteShopSQL = "INSERT INTO fortnite_shop_channels (guild_id, channel_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE channel_id = ?";
    private static final String resetClipsSQL = "DELETE FROM clips_channels WHERE guild_id = ?";
    private static final String resetQuotesSQL = "DELETE FROM quotes_channels WHERE guild_id = ?";
    private static final String resetFortniteShopSQL = "DELETE FROM fortnite_shop_channels WHERE guild_id = ?";

    /**
     * Creates a new config command.
     */
    public ConfigCommand() {
        super(Commands.slash("config", "Configures the server.")
                .addSubcommands(
                        new SubcommandData("clips", "Sets the channel to send clips to.")
                                .addOption(OptionType.INTEGER, "channel-id", "ID of the channel. Defaults to current channel.", false),
                        new SubcommandData("quotes", "Sets the channel to send quotes to.")
                                .addOption(OptionType.INTEGER, "channel-id", "ID of the channel. Defaults to current channel.", false),
                        new SubcommandData("fortnite-shop", "Sets the channel to send the daily Fortnite Shop to.")
                                .addOption(OptionType.INTEGER, "channel-id", "ID of the channel. Defaults to current channel.", false)
                )
                .addSubcommandGroups(
                        new SubcommandGroupData("reset", "Resets the configured channel in the server.")
                                .addSubcommands(
                                        new SubcommandData("clips", "Resets the clips channel."),
                                        new SubcommandData("quotes", "Resets the quotes channel."),
                                        new SubcommandData("fortnite-shop", "Resets the Fortnite Shop channel.")
                                )
                )
        );
    }

    @Override
    public String getName() {
        return "config";
    }

    @Override
    protected void handleAdminCommand() {
        event.deferReply().queue();

        String guildId = event.getGuildChannel().getGuild().getId();
        OptionMapping channelIdOption = event.getOption("channel-id");
        String channelId = channelIdOption == null ? event.getChannel().getId() : channelIdOption.getAsString();
        String subcommandGroupName = event.getSubcommandGroup();
        String subcommandName = Objects.requireNonNull(event.getSubcommandName());

        if (subcommandGroupName != null) {
            if (subcommandGroupName.equals("reset")) {
                switch (subcommandName) {
                    case "clips" -> resetClips(guildId);
                    case "quotes" -> resetQuotes(guildId);
                    case "fortnite-shop" -> resetFortniteShop(guildId);
                }
            }
        } else {
            switch (subcommandName) {
                case "clips" -> configClips(guildId, channelId);
                case "quotes" -> configQuotes(guildId, channelId);
                case "fortnite-shop" -> configFortniteShop(guildId, channelId);
            }
        }


    }

    /**
     * Configures the clips channel for the given guild.
     *
     * @param guildId the ID of the guild
     * @param channelId the ID of the channel
     */
    private void configClips(String guildId, String channelId) {
        try (Connection connection = SQLConnection.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(createClipsTableSQL);
            }

            try (PreparedStatement statement = connection.prepareStatement(insertOrUpdateClipsSQL)) {
                statement.setString(1, guildId);
                statement.setString(2, channelId);
                statement.setString(3, channelId);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        event.getHook().editOriginal("Clips channel set to <#" + channelId + ">.").queue();
    }

    /**
     * Configures the quotes channel for the given guild.
     *
     * @param guildId the ID of the guild
     * @param channelId the ID of the channel
     */
    private void configQuotes(String guildId, String channelId) {
        try (Connection connection = SQLConnection.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(createQuotesTableSQL);
            }

            try (PreparedStatement statement = connection.prepareStatement(insertOrUpdateQuotesSQL)) {
                statement.setString(1, guildId);
                statement.setString(2, channelId);
                statement.setString(3, channelId);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        event.getHook().editOriginal("Quotes channel set to <#" + channelId + ">.").queue();
    }

    /**
     * Configures the Fortnite shop channel for the given guild.
     *
     * @param guildId the ID of the guild
     * @param channelId the ID of the channel
     */
    private void configFortniteShop(String guildId, String channelId) {
        try (Connection connection = SQLConnection.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(createFortniteShopTableSQL);
            }

            try (PreparedStatement statement = connection.prepareStatement(insertOrUpdateFortniteShopSQL)) {
                statement.setString(1, guildId);
                statement.setString(2, channelId);
                statement.setString(3, channelId);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        hook.editOriginal("The daily Fortnite Shop will be sent in <#" + channelId + "> every day at 0:01 UTC.").queue();
    }

    /**
     * Resets the clips channel for the given guild.
     *
     * @param guildId the ID of the guild
     */
    private void resetClips(String guildId) {
        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(resetClipsSQL)) {
            statement.setString(1, guildId);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        event.getHook().editOriginal("Clips channel reset.").queue();
    }

    /**
     * Resets the quotes channel for the given guild.
     *
     * @param guildId the ID of the guild
     */
    private void resetQuotes(String guildId) {
        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(resetQuotesSQL)) {
            statement.setString(1, guildId);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        GetQuoteCommand.guildListMap.remove(Bobo.getJDA().getGuildById(guildId));
        event.getHook().editOriginal("Quotes channel reset.").queue();
    }

    /**
     * Resets the Fortnite Shop channel for the given guild.
     *
     * @param guildId the ID of the guild
     */
    private void resetFortniteShop(String guildId) {
        try (Connection connection = SQLConnection.getConnection();
        PreparedStatement statement = connection.prepareStatement(resetFortniteShopSQL)) {
            statement.setString(1, guildId);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        event.getHook().editOriginal("Fortnite Shop channel reset.").queue();
    }

    @Override
    public String getHelp() {
        return super.getHelp() + " " + """
                Configures the server.
                Usage: `/config <subcommand>`
                Subcommands:
                * `clips <channel>` - Sets <channel> as the channel to send clips to. No channel defaults to current channel.
                * `quotes <channel>` - Sets <channel> as the channel to send quotes to. No channel defaults to current channel.
                * `fortnite-shop <channel> ` - Sets <channel> as the channel to send the daily Fortnite Shop to. No channel defaults to current channel.
                * `reset clips` - Resets the clips channel.
                * `reset quotes` - Resets the quotes channel.
                * `reset fortnite-shop` - Resets the Fortnite Shop channel.""";
    }
}