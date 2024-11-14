package bobo.commands.voice.music;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.ArrayList;
import java.util.List;

public class LoopCommand extends AbstractMusic {
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
    protected void handleMusicCommand() {
        event.deferReply().queue();

        if (currentTrack == null) {
            hook.editOriginal("There is nothing currently playing.").queue();
            return;
        }

        switch (event.getOption("value").getAsString()) {
            case "track" -> track();
            case "queue" -> queue();
            case "off" -> off();
        }
    }

    /**
     * Loops the currently playing track.
     */
    private void track() {
        if (currentTrack.track().getInfo().isStream) {
            hook.editOriginal("Cannot loop a live stream.").queue();
            return;
        }

        switch (scheduler.looping) {
            case NONE, QUEUE -> {
                scheduler.looping = looping.TRACK;
                hook.editOriginal("The track has been set to loop.").queue();
            }
            case TRACK -> {
                scheduler.looping = looping.NONE;
                hook.editOriginal("Looping has been turned off.").queue();
            }
        }
    }

    /**
     * Loops the entire queue.
     */
    private void queue() {
        switch (scheduler.looping) {
            case NONE, TRACK -> {
                scheduler.looping = looping.QUEUE;
                hook.editOriginal("The queue has been set to loop.").queue();
            }
            case QUEUE -> {
                scheduler.looping = looping.NONE;
                hook.editOriginal("Looping has been turned off.").queue();
            }
        }
    }

    /**
     * Turns off looping.
     */
    private void off() {
        switch (scheduler.looping) {
            case NONE -> hook.editOriginal("Looping is already off.").queue();
            case TRACK, QUEUE -> {
                scheduler.looping = looping.NONE;
                hook.editOriginal("Looping has been turned off.").queue();
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
    protected List<Permission> getMusicCommandPermissions() {
        return new ArrayList<>();
    }
}