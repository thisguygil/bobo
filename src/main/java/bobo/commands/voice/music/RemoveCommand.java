package bobo.commands.voice.music;

import bobo.lavaplayer.TrackScheduler;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.Iterator;
import java.util.Objects;

import static net.dv8tion.jda.api.interactions.commands.OptionType.INTEGER;

public class RemoveCommand extends AbstractMusic {
    /**
     * Creates a new remove command.
     */
    public RemoveCommand() {
        super(Commands.slash("remove", "Removes track at given position in the queue.")
                .addOption(INTEGER, "position", "What position in the queue to remove the track from", true));
    }

    @Override
    protected void handleMusicCommand() {
        event.deferReply().queue();

        if (currentTrack == null) {
            hook.editOriginal("The queue is currently empty.").queue();
            return;
        }

        int position = Objects.requireNonNull(event.getOption("position")).getAsInt();
        if (position < 1 || position > queue.size() + 1) {
            hook.editOriginal("Please enter an integer corresponding to a track's position in the queue.").queue();
            return;
        }
        if (position == 1) {
            scheduler.nextTrack();
        } else {
            int count = 1;
            Iterator<TrackScheduler.TrackChannelPair> iterator = queue.iterator();
            while (iterator.hasNext()) {
                if (count == position) {
                    iterator.remove();
                }
                count++;
                iterator.next();
            }
            if (count == position) {
                iterator.remove();
            }
        }
        hook.editOriginal("Removed track at position **" + position + "**.").queue();
    }

    @Override
    public String getName() {
        return "remove";
    }
}
