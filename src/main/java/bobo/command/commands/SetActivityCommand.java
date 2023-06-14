package bobo.command.commands;

import bobo.Bobo;
import bobo.Config;
import bobo.command.ICommand;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Activity.ActivityType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileWriter;
import java.util.Objects;
import java.util.Scanner;

public class SetActivityCommand implements ICommand {
    private static final String activityFileName = Config.get("ACTIVITY_FILE");

    @Override
    public void handle(@Nonnull SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        String activity = Objects.requireNonNull(event.getOption("activity")).getAsString();
        OptionMapping typeInput = event.getOption("type");
        boolean validInput = true;
        String message = "";
        ActivityType activityType;
        if (typeInput == null) {
            try {
                activityType = getTypeFromFile();
                message = "Activity type not specified, will remain unchanged.\n";
            } catch (Exception e) {
                e.printStackTrace();
                activityType = ActivityType.STREAMING;
                message = "Activity type not specified, set to **Streaming**\n";
            }
        } else {
            String typeName = typeInput.getAsString().toUpperCase();
            activityType = getActivityType(typeName);
            if (activityType.equals(ActivityType.STREAMING)) {
                if (!typeName.equals("STREAMING")) {
                    message = "Invalid activity type" + typeName + ", will remain unchanged.\n";
                }
            }
        }

        try {
            if (!validInput) {
                activityType = getTypeFromFile();
            }
            FileWriter writer = new FileWriter(activityFileName);
            writer.write(activityType + "\n" + activity);
            writer.close();
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
     * @throws Exception exception
     */
    public static void setActivity() throws Exception {
        File activityFile = new File(activityFileName);
        Scanner scanner = new Scanner(activityFile);
        String activityType = scanner.nextLine();
        String activityName = scanner.nextLine();
        scanner.close();

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
     * Gets the activity type as a string from activity.txt
     *
     * @return activity type
     */
    private static ActivityType getTypeFromFile() throws Exception {
        File activityFile = new File(activityFileName);
        Scanner scanner = new Scanner(activityFile);
        String activityType = scanner.nextLine();
        scanner.close();
        return getActivityType(activityType);
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

    @Override
    public String getName() {
        return "set-activity";
    }
}