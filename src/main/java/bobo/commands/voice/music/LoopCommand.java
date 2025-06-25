package bobo.commands.voice.music;

import bobo.commands.CommandResponse;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class LoopCommand extends AMusicCommand {
    /**
     * The looping type.
     */
    public enum looping {
        NONE,
        TRACK,
        QUEUE
    }

    /**
     * Creates a new loop command.
     */
    public LoopCommand() {
        super(Commands.slash("loop", "Loop the currently playing track or queue.")
                .addOptions(
                        new OptionData(OptionType.STRING, "value", "What to set to loop", true)
                                .addChoices(
                                        new Command.Choice("track", "track"),
                                        new Command.Choice("queue", "queue"),
                                        new Command.Choice("off", "off")
                                )
                )
        );
    }

    @Override
    protected CommandResponse handleMusicCommand() {
        if (currentTrack == null) {
            return CommandResponse.text("There is nothing currently playing.");
        }

        try {
            return switch (getOptionValue("value", 0)) {
                case "track" -> track();
                case "queue" -> queue();
                case "off" -> off();
                default -> CommandResponse.text("Invalid usage. Please use `/help loop` for more information.");
            };
        } catch (RuntimeException e) {
            return CommandResponse.text("Invalid usage. Please use `/help loop` for more information.");
        }
    }

    /**
     * Loops the currently playing track.
     */
    private CommandResponse track() {
        if (currentTrack.track().getInfo().isStream) {
            return CommandResponse.text("Cannot loop a live stream.");
        }

        switch (scheduler.looping) {
            case NONE, QUEUE -> {
                scheduler.looping = looping.TRACK;
                return CommandResponse.text("The track has been set to loop.");
            }
            case TRACK -> {
                scheduler.looping = looping.NONE;
                return CommandResponse.text("Looping has been turned off.");
            }
        }

        return CommandResponse.EMPTY; // Should not reach here, but must return something
    }

    /**
     * Loops the entire queue.
     */
    private CommandResponse queue() {
        switch (scheduler.looping) {
            case NONE, TRACK -> {
                scheduler.looping = looping.QUEUE;
                return CommandResponse.text("The queue has been set to loop.");
            }
            case QUEUE -> {
                scheduler.looping = looping.NONE;
                return CommandResponse.text("Looping has been turned off.");
            }
        }

        return CommandResponse.EMPTY; // Should not reach here, but must return something
    }

    /**
     * Turns off looping.
     */
    private CommandResponse off() {
        return switch (scheduler.looping) {
            case NONE -> CommandResponse.text("Looping is already off.");
            case TRACK, QUEUE -> {
                scheduler.looping = looping.NONE;
                yield CommandResponse.text("Looping has been turned off.");
            }
        };
    }

    @Override
    public String getName() {
        return "loop";
    }

    @Override
    public String getHelp() {
        return """
                Loop the currently playing track or queue.
                Usage: `/loop <subcommand>`
                Subcommands:
                * `track` - Loop the currently playing track. If the track is already looping, it will turn off looping.
                * `queue` - Loop the entire queue. If the queue is already looping, it will turn off looping.
                * `off` - Turn off looping.""";
    }

    @Override
    public Boolean shouldBeInvisible() {
        return false;
    }
}