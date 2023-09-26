package bobo.commands.admin.owner;

import bobo.Bobo;
import bobo.utils.SQLConnection;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.sql.*;
import java.util.Objects;

public class SetActivityCommand extends AbstractOwner {
    private static final String createTableSQL = "CREATE TABLE IF NOT EXISTS activity (activity_type VARCHAR(255), activity_name VARCHAR(255) NOT NULL)";
    private static final String deleteSQL = "DELETE FROM activity";
    private static final String insertSQL = "INSERT INTO activity (activity_type, activity_name) VALUES (?, ?)";
    private static final String selectSQL = "SELECT * FROM activity";

    /**
     * Creates a new set-activity command.
     */
    public SetActivityCommand() {
        super(Commands.slash("set-activity", "Sets bobo's activity with specified type (playing, streaming, listening, watching, competing).")
                .addSubcommands(
                        new SubcommandData("playing", "Sets bobo's activity to playing.")
                                .addOption(OptionType.STRING, "activity-name", "Activity to set.", true),
                        new SubcommandData("streaming", "Sets bobo's activity to streaming.")
                                .addOption(OptionType.STRING, "activity-name", "Activity to set.", true),
                        new SubcommandData("listening", "Sets bobo's activity to listening.")
                                .addOption(OptionType.STRING, "activity-name", "Activity to set.", true),
                        new SubcommandData("watching", "Sets bobo's activity to watching.")
                                .addOption(OptionType.STRING, "activity-name", "Activity to set.", true),
                        new SubcommandData("competing", "Sets bobo's activity to competing.")
                                .addOption(OptionType.STRING, "activity-name", "Activity to set.", true)
                )
        );
    }

    @Override
    protected void handleOwnerCommand() {
        event.deferReply().queue();

        String activityType = event.getSubcommandName();
        String activityName = Objects.requireNonNull(event.getOption("activity-name")).getAsString();

        try (Connection connection = SQLConnection.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(createTableSQL);
                statement.executeUpdate(deleteSQL);
            }

            try (PreparedStatement insertStatement = connection.prepareStatement(insertSQL)) {
                insertStatement.setString(1, activityType);
                insertStatement.setString(2, activityName);
                insertStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            hook.editOriginal("An error occurred while setting the activity.").queue();
            return;
        }

        setActivity();
        hook.editOriginal("Activity type set to **" + activityType + "**" + "\n" + "Activity set to **" + activityName + "**").queue();
    }

    /**
     * Sets bobo's activity
     */
    public static void setActivity() {
        Activity activity = null;
        try (Connection connection = SQLConnection.getConnection();
             Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(selectSQL);
            if (resultSet.next()) {
                String activityType = resultSet.getString("activity_type");
                String activityName = resultSet.getString("activity_name");
                activity = switch (activityType) {
                    case "playing" -> Activity.playing(activityName);
                    case "listening" -> Activity.listening(activityName);
                    case "watching" -> Activity.watching(activityName);
                    case "competing" -> Activity.competing(activityName);
                    default -> Activity.streaming(activityName, "https://www.youtube.com/watch?v=dQw4w9WgXcQ");
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
}