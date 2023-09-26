package bobo.commands.admin;

import bobo.utils.SQLConnection;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

public class ConfigCommand extends AbstractAdmin {
    private static final String createClipsTableSQL = "CREATE TABLE IF NOT EXISTS clips_channels (guild_id VARCHAR(255) PRIMARY KEY, channel_id VARCHAR(255) NOT NULL)";
    private static final String insertOrUpdateClipsSQL = "INSERT INTO clips_channels (guild_id, channel_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE channel_id = ?";

    /**
     * Creates a new config command.
     */
    public ConfigCommand() {
        super(Commands.slash("config", "Configures the server.")
                .addSubcommands(
                        new SubcommandData("clips", "Sets the channel to send clips to.")
                                .addOption(OptionType.INTEGER, "channel-id", "ID of the channel. Defaults to current channel.", false)
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

        if (Objects.requireNonNull(event.getSubcommandName()).equals("clips")) {
            try (Connection connection = SQLConnection.getConnection()) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(ConfigCommand.createClipsTableSQL);
                }

                try (PreparedStatement statement = connection.prepareStatement(ConfigCommand.insertOrUpdateClipsSQL)) {
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
    }
}
