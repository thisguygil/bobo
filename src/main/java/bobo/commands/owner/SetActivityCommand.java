package bobo.commands.owner;

import bobo.Bobo;
import bobo.utils.api_clients.SQLConnection;
import net.dv8tion.jda.api.entities.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class SetActivityCommand extends AbstractOwner {
    private static final Logger logger = LoggerFactory.getLogger(SetActivityCommand.class);

    private static final String createTableSQL = "CREATE TABLE IF NOT EXISTS activity (activity_type VARCHAR(255), activity_name VARCHAR(255), stream_url VARCHAR(255))";
    private static final String deleteSQL = "DELETE FROM activity";
    private static final String insertSQL = "INSERT INTO activity (activity_type, activity_name, stream_url) VALUES (?, ?, ?)";
    private static final String selectSQL = "SELECT * FROM activity";

    /**
     * Creates a new set-activity command.
     */
    public SetActivityCommand() {}

    @Override
    protected void handleOwnerCommand() {
        if (args.length < 2) {
            event.getChannel().sendMessage("Invalid usage. Use `/help set-activity` for more information.").queue();
            return;
        }

        String activityType = args[0];
        String activityName;
        String streamURL = null;

        if (activityType.equals("streaming")) {
            if (args.length < 3) {
                event.getChannel().sendMessage("Invalid usage. Use `/help set-activity` for more information.").queue();
                return;
            }

            streamURL = args[args.length - 1];

            if (!Activity.isValidStreamingUrl(streamURL)) {
                event.getChannel().sendMessage("Invalid stream URL: " + streamURL).queue();
                return;
            }

            activityName = String.join(" ", args).substring(activityType.length() + 1, args.length - streamURL.length() - 1);
        } else {
            activityName = String.join(" ", args).substring(activityType.length() + 1);
        }

        try (Connection connection = SQLConnection.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(createTableSQL);
                statement.executeUpdate(deleteSQL);
            }

            try (PreparedStatement insertStatement = connection.prepareStatement(insertSQL)) {
                insertStatement.setString(1, activityType);
                insertStatement.setString(2, activityName);
                insertStatement.setString(3, streamURL);
                insertStatement.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error setting the activity: " + e.getMessage());
            event.getChannel().sendMessage("An error occurred while setting the activity.").queue();
            return;
        }

        setActivity();

        String message = switch (activityType) {
            case "playing" -> "Activity set to **Playing " + activityName + "**";
            case "streaming" -> "Activity set to **Streaming [" + activityName + "](<" + streamURL + ">)**";
            case "listening" -> "Activity set to **Listening to " + activityName + "**";
            case "watching" -> "Activity set to **Watching " + activityName + "**";
            case "competing" -> "Activity set to **Competing in " + activityName + "**";
            default -> "Status set to **" + activityName + "**";
        };
        event.getChannel().sendMessage(message).queue();
    }

    /**
     * Sets Bobo's activity.
     */
    public static void setActivity() {
        Activity activity = null;
        String activityName = "";
        try (Connection connection = SQLConnection.getConnection();
             Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(selectSQL);
            if (resultSet.next()) {
                String activityType = resultSet.getString("activity_type");
                activityName = resultSet.getString("activity_name");
                String streamURL = resultSet.getString("stream_url");
                activity = switch (activityType) {
                    case "playing" -> Activity.playing(activityName);
                    case "streaming" -> Activity.streaming(activityName, streamURL);
                    case "listening" -> Activity.listening(activityName);
                    case "watching" -> Activity.watching(activityName);
                    case "competing" -> Activity.competing(activityName);
                    default -> Activity.customStatus(activityName);
                };
            } else {
                return;
            }
        } catch (SQLException e) {
            logger.warn("Failed to set activity.");
        }

        if (activity != null) {
            Bobo.getJDA().getPresence().setActivity(activity);
            logger.info("Activity set to: {}", activityName);
        }
    }

    @Override
    public String getName() {
        return "set-activity";
    }

    @Override
    public String getHelp() {
        return """
                Sets Bobo's activity.
                Usage: `""" + PREFIX + "set-activity <subcommand>`\n" + """
                Subcommands:
                * `custom <status>`: Sets Bobo's status to <status>.
                * `playing <activity>`: Sets Bobo's activity to "Playing <activity>".
                * `streaming <activity> <url>`: Sets Bobo's activity to "Streaming <activity>" with the stream URL <url>.
                * `listening <activity>`: Sets Bobo's activity to "Listening to <activity>".
                * `watching <activity>`: Sets Bobo's activity to "Watching <activity>".
                * `competing <activity>`: Sets Bobo's activity to "Competing in <activity>".""";
    }
}