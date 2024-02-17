package bobo.commands.lastfm;

import bobo.Config;
import bobo.commands.AbstractCommand;
import bobo.utils.SQLConnection;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.sql.*;
import java.util.Map;

public abstract class AbstractLastFM extends AbstractCommand {
    protected static final String API_KEY = Config.get("LASTFM_API_KEY");

    private static final String createSQL = "CREATE TABLE IF NOT EXISTS lastfmlogins (user_id VARCHAR(255) PRIMARY KEY, session_key VARCHAR(255) NOT NULL, lastfm_username VARCHAR(255) NOT NULL)";
    private static final String insertSQL = "INSERT INTO lastfmlogins (user_id, session_key, lastfm_username) VALUES (?, ?, ?)";
    private static final String selectSQL = "SELECT session_key, lastfm_username FROM lastfmlogins WHERE user_id = ?";
    private static final String removeSQL = "DELETE FROM lastfmtokens WHERE user_id = ?";


    /**
     * Creates a new LastFM command.
     *
     * @param commandData The command data.
     */
    public AbstractLastFM(CommandData commandData) {
        super(commandData);
    }

    @Override
    protected void handleCommand() {
        // If the command is not fmlogin, check if the user is logged in
        if (!getName().equals("fmlogin")) {
            String userId = event.getUser().getId();

            // Check if the session key and username are already stored in the database
            try (Connection connection = SQLConnection.getConnection()) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(selectSQL)) {
                    preparedStatement.setString(1, userId);
                    ResultSet resultSet = preparedStatement.executeQuery();
                    if (resultSet.next()) {
                        // Logged in already
                        handleLastFMCommand();
                        return;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // Check if the user's one-use token is stored in the database
            Map<String, String> sessionKeyAndUsername = FMLoginCommand.getSessionKeyAndUsernameFromToken(userId);
            if (sessionKeyAndUsername == null) {
                event.reply("You are not logged in to Last.fm. Log in by using the `/fmlogin` command").setEphemeral(true).queue();
                return;
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
                e.printStackTrace();
            }

            // Remove the token from the database
            try (Connection connection = SQLConnection.getConnection()) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(removeSQL)) {
                    preparedStatement.setString(1, userId);
                    preparedStatement.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        handleLastFMCommand();
    }

    protected abstract void handleLastFMCommand();

    protected static void storeSessionKey(String userId, String sessionKey, String lastfmUsername) {

    }
}