package bobo.commands.owner;

import bobo.Bobo;
import bobo.utils.SQLConnection;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.sql.*;
import java.util.Objects;

public class SetActivityCommand extends AbstractOwner {
    private static final String createTableSQL = "CREATE TABLE IF NOT EXISTS activity (activity_type VARCHAR(255), activity_name VARCHAR(255), stream_url VARCHAR(255))";
    private static final String deleteSQL = "DELETE FROM activity";
    private static final String insertSQL = "INSERT INTO activity (activity_type, activity_name, stream_url) VALUES (?, ?, ?)";
    private static final String selectSQL = "SELECT * FROM activity";

    /**
     * Creates a new set-activity command.
     */
    public SetActivityCommand() {
        super(Commands.slash("set-activity", "Sets Bobo's activity.")
                .addSubcommands(new SubcommandData("custom", "Sets Bobo's activity to a custom status.")
                                .addOption(OptionType.STRING, "status", "Status to set.", true),
                        new SubcommandData("playing", "Sets Bobo's activity to playing.")
                                .addOption(OptionType.STRING, "activity", "Activity to set.", true),
                        new SubcommandData("streaming", "Sets Bobo's activity to streaming.")
                                .addOption(OptionType.STRING, "activity", "Activity to set.", true)
                                .addOption(OptionType.STRING, "url", "URL to stream.", true),
                        new SubcommandData("listening", "Sets Bobo's activity to listening.")
                                .addOption(OptionType.STRING, "activity", "Activity to set.", true),
                        new SubcommandData("watching", "Sets Bobo's activity to watching.")
                                .addOption(OptionType.STRING, "activity", "Activity to set.", true),
                        new SubcommandData("competing", "Sets Bobo's activity to competing.")
                                .addOption(OptionType.STRING, "activity", "Activity to set.", true))
        );
    }

    @Override
    protected void handleOwnerCommand() {
        event.deferReply().queue();

        String activityType = event.getSubcommandName();
        assert activityType != null;
        OptionMapping nameOption = activityType.equals("custom") ? event.getOption("status") : event.getOption("activity");
        assert nameOption != null;
        String activityName = nameOption.getAsString();

        String streamURL = null;
        if (activityType.equals("streaming")) {
            streamURL = Objects.requireNonNull(event.getOption("url")).getAsString();
            if (!Activity.isValidStreamingUrl(streamURL)) {
                hook.editOriginal("Invalid stream URL: " + streamURL).queue();
                return;
            }
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
            e.printStackTrace();
            hook.editOriginal("An error occurred while setting the activity.").queue();
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
        hook.editOriginal(message).queue();
    }

    /**
     * Sets Bobo's activity.
     */
    public static void setActivity() {
        Activity activity = null;
        try (Connection connection = SQLConnection.getConnection();
             Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(selectSQL);
            if (resultSet.next()) {
                String activityType = resultSet.getString("activity_type");
                String activityName = resultSet.getString("activity_name");
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
            e.printStackTrace();
        }

        Bobo.getJDA().getPresence().setActivity(activity);
    }

    @Override
    public String getName() {
        return "set-activity";
    }

    @Override
    public String getHelp() {
        return """
                Sets Bobo's activity.
                Usage: `/set-activity <subcommand>`
                Subcommands:
                * `custom <status>`: Sets Bobo's status to <status>.
                * `playing <activity>`: Sets Bobo's activity to "Playing <activity>".
                * `streaming <activity> <url>`: Sets Bobo's activity to "Streaming <activity>" with the stream URL <url>.
                * `listening <activity>`: Sets Bobo's activity to "Listening to <activity>".
                * `watching <activity>`: Sets Bobo's activity to "Watching <activity>".
                * `competing <activity>`: Sets Bobo's activity to "Competing in <activity>".""";
    }
}