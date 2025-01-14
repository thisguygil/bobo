package bobo.commands.lastfm;

import bobo.Config;
import bobo.commands.ADualCommand;
import bobo.commands.CommandResponse;
import bobo.utils.api_clients.SQLConnection;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class ALastFMCommand extends ADualCommand {
    private static final Logger logger = LoggerFactory.getLogger(ALastFMCommand.class);

    protected static final String API_KEY = Config.get("LASTFM_API_KEY");

    private static final String createSQL = "CREATE TABLE IF NOT EXISTS lastfmlogins (user_id VARCHAR(255) PRIMARY KEY, session_key VARCHAR(255) NOT NULL, lastfm_username VARCHAR(255) NOT NULL)";
    private static final String insertSQL = "INSERT INTO lastfmlogins (user_id, session_key, lastfm_username) VALUES (?, ?, ?)";
    private static final String selectSQL = "SELECT session_key, lastfm_username FROM lastfmlogins WHERE user_id = ?";
    private static final String removeTokenSQL = "DELETE FROM lastfmtokens WHERE user_id = ?";
    private static final String selectUsernameSQL = "SELECT lastfm_username FROM lastfmlogins WHERE user_id = ?";

    /**
     * Creates a new LastFM command.
     *
     * @param commandData The command data.
     */
    public ALastFMCommand(CommandData commandData) {
        super(commandData);
    }

    /**
     * Ensure the user is logged in to Last.fm before handling the command, unless the command is fmlogin.
     */
    @Override
    protected CommandResponse handleCommand() {
        // If the command is not fmlogin, check if the user is logged in
        if (!getName().equals("fmlogin")) {
            String userId = getUser().getId();

            // Check if the session key and username are already stored in the database
            try (Connection connection = SQLConnection.getConnection()) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(selectSQL)) {
                    preparedStatement.setString(1, userId);
                    ResultSet resultSet = preparedStatement.executeQuery();
                    if (resultSet.next()) {
                        // Logged in already
                        return handleLastFMCommand();
                    }
                }
            } catch (SQLException e) {
                logger.error("Failed to check if user {} is logged in to Last.fm", userId);
            }

            // Check if the user's one-use token is stored in the database
            Map<String, String> sessionKeyAndUsername = FMLoginCommand.getSessionKeyAndUsernameFromToken(userId);
            if (sessionKeyAndUsername == null) {
                return new CommandResponse("You are not logged in to Last.fm. Log in by using the `/fmlogin` command", true);
            }

            // Store the session key and username in the database
            try (Connection connection = SQLConnection.getConnection()) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(createSQL);
                }

                try (PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
                    preparedStatement.setString(1, userId);
                    preparedStatement.setString(2, sessionKeyAndUsername.get("sessionKey"));
                    preparedStatement.setString(3, sessionKeyAndUsername.get("username"));
                    preparedStatement.executeUpdate();
                }
            } catch (SQLException e) {
                logger.error("Failed to store Last.fm session key and username for user {}", userId);
            }

            // Remove the token from the database
            try (Connection connection = SQLConnection.getConnection()) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(removeTokenSQL)) {
                    preparedStatement.setString(1, userId);
                    preparedStatement.executeUpdate();
                }
            } catch (SQLException e) {
                logger.error("Failed to remove Last.fm token for user {}", userId);
            }
        }

        return handleLastFMCommand();
    }

    protected abstract CommandResponse handleLastFMCommand();

    @Override
    public String getHelp() {
        return "Last.fm Command. You must be logged in to Last.fm to use this command. Use `/fmlogin` to log in.";
    }

    /**
     * Gets the Last.fm username of the user.
     *
     * @param userId The user id.
     * @return The Last.fm username, or null if the user is not logged in.
     */
    @Nullable
    protected String getUserName(String userId) {
        String username = null;
        try (Connection connection = SQLConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectUsernameSQL)) {
            statement.setString(1, userId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                username = resultSet.getString("lastfm_username");
            }
        } catch (SQLException e) {
            logger.error("Failed to get Last.fm username for user {}", userId);
        }

        return username;
    }

    /**
     * Creates a Discord timestamp from a given date of the form YYYY-MM-DD
     *
     * @param dateStr The date to create a timestamp from.
     * @return The Discord timestamp.
     */
    protected String createDiscordTimestamp(String dateStr) {
        // Add missing parts of the date string
        if (dateStr.length() == 4) { // Only year provided
            dateStr += "-01-01";
        } else if (dateStr.length() == 7) { // Year and month provided
            dateStr += "-01";
        }

        // Parse the date string
        LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // Convert LocalDate to Unix timestamp (seconds since 1970-01-01 00:00:00 UTC)
        long unixTimestamp = date.atStartOfDay(ZoneId.of("UTC")).toEpochSecond();

        // Format for Discord and return
        return String.format("<t:%d:D>", unixTimestamp);
    }

    /**
     * Wraps the given string in back quotes.
     * @param string The string to wrap.
     * @return The wrapped string.
     */
    protected String backQuotes(String string) {
        return "`" + string + "`";
    }

    @Override
    protected List<Permission> getCommandPermissions() {
        List<Permission> permissions = getLastFMCommandPermissions();
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        return permissions;
    }

    /**
     * Gets the permissions required for the Last.fm command.
     *
     * @return The permissions required for the Last.fm command.
     */
    protected List<Permission> getLastFMCommandPermissions() {
        return new ArrayList<>();
    }
}