package bobo.commands.admin;

import bobo.Bobo;
import bobo.Config;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Activity.ActivityType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.io.*;
import java.util.Objects;

public class SetActivityCommand extends AbstractAdmin {
    private static final String activityFileName = Config.get("ACTIVITY_FILE");

    /**
     * Creates a new set-activity command.
     */
    public SetActivityCommand() {
        super(Commands.slash("set-activity", "Sets bobo's activity with specified type (playing, streaming, listening, watching, competing).")
                .addSubcommands(
                        new SubcommandData("playing", "Sets bobo's activity to playing.")
                                .addOption(OptionType.STRING, "activity", "Activity to set.", true),
                        new SubcommandData("streaming", "Sets bobo's activity to streaming.")
                                .addOption(OptionType.STRING, "activity", "Activity to set.", true),
                        new SubcommandData("listening", "Sets bobo's activity to listening.")
                                .addOption(OptionType.STRING, "activity", "Activity to set.", true),
                        new SubcommandData("watching", "Sets bobo's activity to watching.")
                                .addOption(OptionType.STRING, "activity", "Activity to set.", true),
                        new SubcommandData("competing", "Sets bobo's activity to competing.")
                                .addOption(OptionType.STRING, "activity", "Activity to set.", true)
                )
        );
    }

    /**
     * Stores the bot's activity type and activity.
     */
    private static class BotActivity {
        ActivityType activityType;
        String activity;
    }

    @Override
    protected void handleAdminCommand() {
        event.deferReply().queue();

        String activity = Objects.requireNonNull(event.getOption("activity")).getAsString();

        ActivityType activityType = switch (Objects.requireNonNull(event.getSubcommandName())) {
            case "playing" -> ActivityType.PLAYING;
            case "listening" -> ActivityType.LISTENING;
            case "watching" -> ActivityType.WATCHING;
            case "competing" -> ActivityType.COMPETING;
            default -> ActivityType.STREAMING;
        };

        try {
            saveActivityToFile(activityType, activity);
            setActivity();
        } catch (IOException e) {
            e.printStackTrace();
            hook.editOriginal(e.getMessage()).queue();
            return;
        }

        hook.editOriginal("Activity type set to **" + activityType + "**" + "\n" + "Activity set to **" + activity + "**").queue();
    }

    /**
     * Sets bobo's activity
     *
     * @throws IOException exception
     */
    public static void setActivity() throws IOException {
        BotActivity botActivity = getActivityFromFile();
        String activityType = botActivity.activityType.toString();
        String activityName = botActivity.activity;

        Activity activity = switch (activityType) {
            case "PLAYING" -> Activity.playing(activityName);
            case "LISTENING" -> Activity.listening(activityName);
            case "WATCHING" -> Activity.watching(activityName);
            case "COMPETING" -> Activity.competing(activityName);
            default -> Activity.streaming(activityName, "https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        };
        Bobo.getJDA().getPresence().setActivity(activity);
    }

    /**
     * Saves the activity to a file
     *
     * @param activityType activity type
     * @param activity     activity
     * @throws IOException exception
     */
    private static void saveActivityToFile(ActivityType activityType, String activity) throws IOException {
        Gson gson = new Gson();
        try (Writer writer = new FileWriter(activityFileName)) {
            BotActivity botActivity = new BotActivity();
            botActivity.activityType = activityType;
            botActivity.activity = activity;
            gson.toJson(botActivity, writer);
        }
    }

    /**
     * Gets the activity from a file
     *
     * @return activity
     * @throws IOException exception
     */
    private static BotActivity getActivityFromFile() throws IOException {
        Gson gson = new Gson();
        try (JsonReader reader = new JsonReader(new FileReader(activityFileName))) {
            return gson.fromJson(reader, BotActivity.class);
        }
    }

    @Override
    public String getName() {
        return "set-activity";
    }
}