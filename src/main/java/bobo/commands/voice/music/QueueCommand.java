package bobo.commands.voice.music;

import bobo.utils.TrackRecord;
import bobo.utils.TimeFormat;
import bobo.utils.TrackType;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.awt.*;
import java.util.*;
import java.util.List;

public class QueueCommand extends AbstractMusic {
    /**
     * Creates a new queue command.
     */
    public QueueCommand() {
        super(Commands.slash("queue", "View or change the music queue.")
                .addSubcommands(
                        new SubcommandData("show", "Shows the currently queued tracks."),
                        new SubcommandData("shuffle", "Shuffles the queue."),
                        new SubcommandData("clear", "Clears the queue and stops current track."),
                        new SubcommandData("remove", "Removes track at given position in the queue.")
                                .addOption(OptionType.INTEGER, "position", "What position in the queue to remove the track from", true)
                )
        );
    }

    @Override
    protected void handleMusicCommand() {
        event.deferReply().queue();

        if (currentTrack == null) {
            hook.editOriginal("The queue is currently empty.").queue();
            return;
        }

        switch (Objects.requireNonNull(event.getSubcommandName())) {
            case "show" -> show();
            case "shuffle" -> shuffle();
            case "clear" -> clear();
            case "remove" -> remove();
        }
    }

    /**
     * Shows the current queue.
     */
    private void show() {
        // Initialize all necessary variables
        final List<TrackRecord> trackList = new ArrayList<>(queue);
        trackList.add(0, new TrackRecord(currentTrack.track(), null, null, currentTrack.trackType())); // Null values are not used
        StringBuilder tracksField = new StringBuilder();
        int trackCounter = 1;
        Member member = event.getMember();
        assert member != null;

        // Add all tracks to the pages
        List<EmbedBuilder> embedBuilders = new ArrayList<>();
        for (TrackRecord record : trackList) {
            String trackInfo = formatTrackInfo(trackCounter, record);
            if (trackCounter % 10 == 1 && trackCounter != 1) {
                embedBuilders.add(createQueueEmbed(member, tracksField.toString()));
                tracksField = new StringBuilder();
            }

            tracksField.append(trackInfo);
            trackCounter++;
        }

        // Add any remaining tracks to the last page
        String finalTracksField = tracksField.toString();
        if (!finalTracksField.isEmpty()) {
            embedBuilders.add(createQueueEmbed(member, finalTracksField));
        }

        // Add page counts to the footers and construct the pages
        int pageCount = 1;
        List<Page> pages = new ArrayList<>();
        for (EmbedBuilder embedBuilder : embedBuilders) {
            embedBuilder.setFooter("Page " + pageCount + " of " + embedBuilders.size());
            pages.add(InteractPage.of(embedBuilder.build()));
            pageCount++;
        }

        if (pages.size() == 1) { // Don't paginate if there's only one page
            hook.editOriginalEmbeds((MessageEmbed) pages.get(0).getContent()).queue();
        } else {
            hook.editOriginalEmbeds((MessageEmbed) pages.get(0).getContent()).queue(success -> Pages.paginate(success, pages, true));
        }
    }

    /**
     * Creates an embed with the current up to 10 tracks.
     * @param member the member who requested the queue
     * @param tracksField the field containing the tracks
     * @return the embed
     */
    private EmbedBuilder createQueueEmbed(Member member, String tracksField) {
        return new EmbedBuilder()
                .setAuthor(member.getEffectiveName(), "https://discord.com/users/" + member.getId(), member.getEffectiveAvatarUrl())
                .setTitle("Current Queue" + (scheduler.looping == LoopCommand.looping.QUEUE ? " - Looping" : ""))
                .setDescription(tracksField)
                .setColor(Color.red);
    }

    /**
     * Shuffles the queue.
     */
    private void shuffle() {
        List<TrackRecord> trackList = new ArrayList<>();
        queue.drainTo(trackList);
        Collections.shuffle(trackList);
        queue.clear();
        queue.addAll(trackList);
        hook.editOriginal("Shuffled.").queue();
    }

    /**
     * Clears the queue.
     */
    private void clear() {
        for (TrackRecord record : queue) {
            if (record.trackType() == TrackType.TTS) {
                TTSCommand.removeTTSMessage(record.track());
            }
        }
        queue.clear();
        scheduler.looping = LoopCommand.looping.NONE;
        scheduler.currentTrack = null;
        player.stopTrack();
        player.setPaused(false);
        hook.editOriginal("Queue cleared.").queue();
    }

    /**
     * Removes a track from the queue.
     */
    private void remove() {
        int position = Objects.requireNonNull(event.getOption("position")).getAsInt();
        if (position < 1 || position > queue.size() + 1) {
            hook.editOriginal("Please enter an integer corresponding to a track's position in the queue.").queue();
            return;
        }

        if (position == 1) {
            if (queue.isEmpty()) {
                scheduler.looping = LoopCommand.looping.NONE;
            }

            if (scheduler.looping == LoopCommand.looping.TRACK) {
                scheduler.looping = LoopCommand.looping.NONE;
            }
            scheduler.nextTrack();
            if (currentTrack.trackType() == TrackType.TTS) {
                TTSCommand.removeTTSMessage(currentTrack.track());
            }
        } else {
            int count = 1;
            Iterator<TrackRecord> iterator = queue.iterator();
            TrackRecord currentTrack = null;
            while (iterator.hasNext()) {
                if (count == position) {
                    if (currentTrack.trackType() == TrackType.TTS) {
                        TTSCommand.removeTTSMessage(currentTrack.track());
                    }
                    iterator.remove();
                }
                count++;
                currentTrack = iterator.next();
            }
            if (count == position) {
                if (currentTrack.trackType() == TrackType.TTS) {
                    TTSCommand.removeTTSMessage(currentTrack.track());
                }
                iterator.remove();
            }
        }

        hook.editOriginal("Removed track at position **" + position + "**.").queue();
    }

    /**
     * Formats the track info.
     * @param index the index of the track
     * @param record the track record
     * @return the formatted track info
     */
    private String formatTrackInfo(int index, TrackRecord record) {
        AudioTrack track = record.track();
        AudioTrackInfo info = track.getInfo();
        String timeLeft = String.format("[%s]\n", (index == 1 ? TimeFormat.formatTime(track.getDuration() - track.getPosition()) + " left" + (scheduler.looping == LoopCommand.looping.TRACK ? " - looping" : "") : TimeFormat.formatTime(track.getDuration())));
        String trackDetails = "";
        switch (record.trackType()) {
            case TRACK, FILE -> trackDetails = String.format("**%d**. [%s](%s) by **%s** %s", index, info.title, info.uri, info.author, timeLeft);
            case TTS -> trackDetails = String.format("**%d**. TTS Message: \"%s\" %s", index, info.title, timeLeft);
        }
        return trackDetails;
    }

    @Override
    public String getName() {
        return "queue";
    }

    @Override
    public String getHelp() {
        return """
                View or change the music queue.
                Usage: `/queue <subcommand>`
                Subcommands:
                * `show`: Shows the currently queued tracks.
                * `shuffle`: Shuffles the queue.
                * `loop`: Loop the queue.
                * `clear`: Clears the queue and stops current track.
                * `remove <position>`: Removes track at position <position> in the queue.""";
    }

    @Override
    protected List<Permission> getMusicCommandPermissions() {
        return new ArrayList<>();
    }
}