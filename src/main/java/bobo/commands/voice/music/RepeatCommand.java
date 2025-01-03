package bobo.commands.voice.music;

import bobo.commands.CommandResponse;
import bobo.lavaplayer.TrackRecord;
import bobo.lavaplayer.TrackType;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.ArrayList;
import java.util.List;

public class RepeatCommand extends AMusicCommand {
    /**
     * Creates a new repeat command.
     */
    public RepeatCommand() {
        super(Commands.slash("repeat", "Repeats the current/last-played track once."));
    }

    @Override
    protected CommandResponse handleMusicCommand() {
        Member member = getMember();
        if (!ensureConnected(member)) {
            return new CommandResponse("You must be connected to a voice channel to use this command.");
        }

        if (reAddTrackToQueue(member, currentTrack)) {
            return new CommandResponse("The current track has been re-added to the queue.");
        }

        if (reAddTrackToQueue(member, previousTrack)) {
            scheduler.nextTrack(); // If the previous track was re-added, it means the player is stopped and needs to be started again.
            return new CommandResponse("The previous track has been re-added to the queue.");
        }

        return new CommandResponse("There is nothing to repeat.");
    }

    /**
     * Re-adds the given track to the queue.
     *
     * @param member The member that requested the track to be repeated.
     * @param trackRecord The track to re-add.
     * @return {@code true} if the track was re-added, {@code false} otherwise.
     */
    private boolean reAddTrackToQueue(Member member, TrackRecord trackRecord) {
        if (trackRecord != null) {
            TrackRecord toRepeat = new TrackRecord(
                    trackRecord.track().makeClone(),
                    member, // Use the member that requested the track to be repeated rather than the member that originally requested the track.
                    trackRecord.channel(),
                    trackRecord.trackType()
            );
            queue.offerFirst(toRepeat);
            if (trackRecord.trackType() == TrackType.TTS) {
                Guild guild = getGuild();
                TTSCommand.addTTSMessage(
                        guild,
                        toRepeat.track(),
                        TTSCommand.getPreviousTTSMessage(
                                guild,
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

    @Override
    public Boolean shouldBeInvisible() {
        return false;
    }
}