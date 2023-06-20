package bobo.command.commands;

import bobo.Bobo;
import bobo.Config;
import bobo.command.ICommand;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Activity.ActivityType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.Objects;

public class SetActivityCommand implements ICommand {
    private static final String activityFileName = Config.get("ACTIVITY_FILE");

    private static class BotActivity {
        ActivityType activityType;
        String activity;
    }

    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        String activity = Objects.requireNonNull(event.getOption("activity")).getAsString();
        OptionMapping typeInput = event.getOption("type");
        ActivityType activityType = null;
        String message = "";

        if (typeInput != null) {
            String typeName = typeInput.getAsString().toUpperCase();
            activityType = getActivityType(typeName);
            if (activityType == null) {
                message = "Invalid activity type " + typeName + ", will remain unchanged.\n";
            }
        }

        try {
            if (activityType == null) {
                BotActivity botActivity = getActivityFromFile();
                activityType = botActivity.activityType;
                message += "Activity type not specified, will remain unchanged.\n";
            }
            saveActivityToFile(activityType, activity);
            setActivity();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (message.isEmpty()) {
            message = "Activity type set to **" + activityType + "**\n";
        }
        message += "Activity set to **" + activity + "**";
        event.getHook().editOriginal(message).queue();
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
     * Gets the activity type given a string
     *
     * @param activityType activity type string
     * @return activity type
     */
    private static ActivityType getActivityType(String activityType) {
        return switch (activityType) {
            case "PLAYING" -> ActivityType.PLAYING;
            case "LISTENING" -> ActivityType.LISTENING;
            case "WATCHING" -> ActivityType.WATCHING;
            case "COMPETING" -> ActivityType.COMPETING;
            default -> ActivityType.STREAMING;
        };
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