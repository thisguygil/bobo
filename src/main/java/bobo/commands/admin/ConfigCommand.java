package bobo.commands.admin;

import bobo.Bobo;
import bobo.commands.CommandResponse;
import bobo.commands.general.RandomCommand;
import bobo.utils.api_clients.SQLConnection;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class ConfigCommand extends AAdminCommand {
    private static final String createClipsTableSQL = "CREATE TABLE IF NOT EXISTS clips_channels (guild_id VARCHAR(255) PRIMARY KEY, channel_id VARCHAR(255) NOT NULL)";
    private static final String insertOrUpdateClipsSQL = "INSERT INTO clips_channels (guild_id, channel_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE channel_id = ?";
    private static final String createQuotesTableSQL = "CREATE TABLE IF NOT EXISTS quotes_channels (guild_id VARCHAR(255) PRIMARY KEY, channel_id VARCHAR(255) NOT NULL)";
    private static final String insertOrUpdateQuotesSQL = "INSERT INTO quotes_channels (guild_id, channel_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE channel_id = ?";
    private static final String createFortniteShopTableSQL = "CREATE TABLE IF NOT EXISTS fortnite_shop_channels (guild_id VARCHAR(255) PRIMARY KEY, channel_id VARCHAR(255) NOT NULL)";
    private static final String insertOrUpdateFortniteShopSQL = "INSERT INTO fortnite_shop_channels (guild_id, channel_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE channel_id = ?";
    public static final String resetClipsSQL = "DELETE FROM clips_channels WHERE guild_id = ?";
    public static final String resetQuotesSQL = "DELETE FROM quotes_channels WHERE guild_id = ?";
    public static final String resetFortniteShopSQL = "DELETE FROM fortnite_shop_channels WHERE guild_id = ?";

    /**
     * Creates a new config command.
     */
    public ConfigCommand() {
        super(Commands.slash("config", "Configures the server.")
                .addOptions(
                        new OptionData(OptionType.STRING, "setting", "The setting to configure.", true)
                                .addChoices(
                                        new Command.Choice("clips channel", "clips"),
                                        new Command.Choice("quotes channel", "quotes"),
                                        new Command.Choice("Fortnite Shop channel", "fortnite-shop")
                                ),
                        new OptionData(OptionType.CHANNEL, "channel", "The channel to set. No channel input clears the setting.", false)
                )
        );
    }

    @Override
    public String getName() {
        return "config";
    }

    @Override
    protected CommandResponse handleAdminCommand() {
        String guildId = event.getGuild().getId();
        String setting = getOptionValue("setting");

        OptionMapping channelOption = event.getOption("channel"); // Can't use getOptionValue because it doesn't work with channels
        GuildChannelUnion channel;
        if (channelOption != null) {
            channel = channelOption.getAsChannel();
            String channelId = channel.getId();
            return switch (setting) {
                case "clips" -> configClips(guildId, channelId);
                case "quotes" -> configQuotes(guildId, channelId);
                case "fortnite-shop" -> configFortniteShop(guildId, channelId);
                default -> CommandResponse.text("Invalid usage. Use `/help config` for more information.");
            };
        } else {
            return switch (setting) {
                case "clips" -> resetClips(guildId);
                case "quotes" -> resetQuotes(guildId);
                case "fortnite-shop" -> resetFortniteShop(guildId);
                default -> CommandResponse.text("Invalid usage. Use `/help config` for more information.");
            };
        }
    }

    /**
     * Configures the clips channel for the given guild.
     *
     * @param guildId the ID of the guild
     * @param channelId the ID of the channel
     */
    private CommandResponse configClips(String guildId, String channelId) {
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
            return CommandResponse.text("An error occurred while configuring the clips channel.");
        }
        return CommandResponse.text("Clips channel set to <#%s>.", channelId);
    }

    /**
     * Configures the quotes channel for the given guild.
     *
     * @param guildId the ID of the guild
     * @param channelId the ID of the channel
     */
    private CommandResponse configQuotes(String guildId, String channelId) {
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
            return CommandResponse.text("An error occurred while configuring the quotes channel.");
        }
        return CommandResponse.text("Quotes channel set to <#%s>.", channelId);
    }

    /**
     * Configures the Fortnite shop channel for the given guild.
     *
     * @param guildId the ID of the guild
     * @param channelId the ID of the channel
     */
    private CommandResponse configFortniteShop(String guildId, String channelId) {
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
            return CommandResponse.text("An error occurred while configuring the Fortnite Shop channel.");
        }
        return CommandResponse.text("The daily Fortnite Shop will be sent in <#%s> every day at 0:01 UTC.", channelId);
    }

    /**
     * Resets the clips channel for the given guild.
     *
     * @param guildId the ID of the guild
     */
    private CommandResponse resetClips(String guildId) {
        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(resetClipsSQL)) {
            statement.setString(1, guildId);
            statement.executeUpdate();
        } catch (SQLException e) {
            return CommandResponse.text("An error occurred while resetting the clips channel.");
        }
        RandomCommand.guildClipListMap.remove(Bobo.getJDA().getGuildById(guildId));
        return CommandResponse.text("Clips channel reset.");
    }

    /**
     * Resets the quotes channel for the given guild.
     *
     * @param guildId the ID of the guild
     */
    private CommandResponse resetQuotes(String guildId) {
        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(resetQuotesSQL)) {
            statement.setString(1, guildId);
            statement.executeUpdate();
        } catch (SQLException e) {
            return CommandResponse.text("An error occurred while resetting the quotes channel.");
        }
        RandomCommand.guildQuoteListMap.remove(Bobo.getJDA().getGuildById(guildId));
        return CommandResponse.text("Quotes channel reset.");
    }

    /**
     * Resets the Fortnite Shop channel for the given guild.
     *
     * @param guildId the ID of the guild
     */
    private CommandResponse resetFortniteShop(String guildId) {
        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(resetFortniteShopSQL)) {
            statement.setString(1, guildId);
            statement.executeUpdate();
        } catch (SQLException e) {
            return CommandResponse.text("An error occurred while resetting the Fortnite Shop channel.");
        }
        return CommandResponse.text("Fortnite Shop channel reset.");
    }

    @Override
    public String getHelp() {
        return super.getHelp() + " " + """
                Configures the server.
                Usage: `/config <setting> <channel>`
                Settings:
                * clips channel
                * quotes channel
                * Fortnite Shop channel
                No channel input clears the setting.
                """;

    }

    @Override
    public Boolean shouldBeEphemeral() {
        return false;
    }
}