package bobo.commands.voice.music;

import bobo.utils.TrackRecord;
import bobo.utils.TrackType;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.ArrayList;
import java.util.List;

public class RepeatCommand extends AbstractMusic {
    /**
     * Creates a new repeat command.
     */
    public RepeatCommand() {
        super(Commands.slash("repeat", "Repeats the current/last-played track once."));
    }

    @Override
    protected void handleMusicCommand() {
        event.deferReply().queue();

        if (!ensureConnected(event)) {
            return;
        }

        Member member = event.getMember();
        if (reAddTrackToQueue(member, currentTrack, "The current track has been re-added to the queue.")) {
            return;
        }

        if (reAddTrackToQueue(member, previousTrack, "The previous track has been re-added to the queue.")) {
            scheduler.nextTrack(); // If the previous track was re-added, it means the player is stopped and needs to be started again.
            return;
        }

        hook.editOriginal("There is nothing to repeat.").queue();
    }

    /**
     * Re-adds the given track to the queue.
     *
     * @param trackRecord The track to re-add.
     * @param successMessage The message to send if the track was re-added.
     * @return {@code true} if the track was re-added, {@code false} otherwise.
     */
    private boolean reAddTrackToQueue(Member member, TrackRecord trackRecord, String successMessage) {
        if (trackRecord != null) {
            TrackRecord toRepeat = new TrackRecord(
                    trackRecord.track().makeClone(),
                    member, // Use the member that requested the track to be repeated rather than the member that originally requested the track.
                    trackRecord.channel(),
                    trackRecord.trackType()
            );
            hook.editOriginal(successMessage).queue();
            queue.offerFirst(toRepeat);
            if (trackRecord.trackType() == TrackType.TTS) {
                Guild guild = event.getGuild();
                TTSCommand.addTTSMessage(
                        guild,
                        toRepeat.track(),
                        TTSCommand.getPreviousTTSMessage(
                                event.getGuild(),
                                trackRecord.track()
                        )
                );
            }
            return true;
        }
        return false;
    }

    @Override
    public String getName() {
        return "repeat";
    }

    @Override
    public String getHelp() {
        return """
                Adds the current track to the next position in the queue to be played again.
                If the queue is empty, the most recently played track will be repeated.
                Usage: `/repeat`""";
    }

    @Override
    protected List<Permission> getMusicCommandPermissions() {
        return new ArrayList<>();
    }
}