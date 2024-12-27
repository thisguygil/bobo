package bobo.commands.admin;

import bobo.Bobo;
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

public class ConfigCommand extends AbstractAdmin {
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
    protected void handleAdminCommand() {
        String guildId = event.getGuildChannel().getGuild().getId();
        String setting = event.getOption("setting").getAsString();
        OptionMapping channelOption = event.getOption("channel");
        GuildChannelUnion channel;
        if (channelOption != null) {
            channel = channelOption.getAsChannel();
            String channelId = channel.getId();
            switch (setting) {
                case "clips" -> configClips(guildId, channelId);
                case "quotes" -> configQuotes(guildId, channelId);
                case "fortnite-shop" -> configFortniteShop(guildId, channelId);
            }
        } else {
            switch (setting) {
                case "clips" -> resetClips(guildId);
                case "quotes" -> resetQuotes(guildId);
                case "fortnite-shop" -> resetFortniteShop(guildId);
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
            hook.editOriginal("An error occurred while configuring the clips channel.").queue();
            return;
        }
        hook.editOriginal("Clips channel set to <#" + channelId + ">.").queue();
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
            hook.editOriginal("An error occurred while configuring the quotes channel.").queue();
            return;
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
            hook.editOriginal("An error occurred while configuring the Fortnite Shop channel.").queue();
            return;
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
            hook.editOriginal("An error occurred while resetting the clips channel.").queue();
            return;
        }
        RandomCommand.guildClipListMap.remove(Bobo.getJDA().getGuildById(guildId));
        hook.editOriginal("Clips channel reset.").queue();
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
            hook.editOriginal("An error occurred while resetting the quotes channel.").queue();
            return;
        }
        RandomCommand.guildQuoteListMap.remove(Bobo.getJDA().getGuildById(guildId));
        hook.editOriginal("Quotes channel reset.").queue();
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
            hook.editOriginal("An error occurred while resetting the Fortnite Shop channel.").queue();
            return;
        }
        hook.editOriginal("Fortnite Shop channel reset.").queue();
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