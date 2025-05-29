package bobo.commands.voice.music;

import bobo.commands.CommandResponse;
import bobo.commands.CommandResponseBuilder;
import bobo.lavaplayer.TrackScheduler;
import bobo.lavaplayer.TrackRecord;
import bobo.utils.AudioReceiveListener;
import bobo.utils.TimeFormat;
import bobo.lavaplayer.TrackType;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.awt.*;
import java.util.*;
import java.util.List;

import static bobo.utils.StringUtils.*;

public class QueueCommand extends AMusicCommand {
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
    protected CommandResponse handleMusicCommand() {
        if (currentTrack == null) {
            return new CommandResponse("The queue is currently empty.");
        }

        String subcommandName;
        try {
            subcommandName = getSubcommandName(0);
        } catch (RuntimeException e) { // No subcommand provided
            return switch (command) {
                case "q", "queue" -> show();
                case "clear" -> clear();
                case "shuffle" -> shuffle();
                case "remove" -> remove(0);
                default -> new CommandResponse("Please provide a subcommand.");
            };
        }

        return switch (subcommandName) {
            case "show" -> show();
            case "shuffle" -> shuffle();
            case "clear" -> clear();
            case "remove" -> remove(1);
            default -> new CommandResponse("Invalid usage. Use `/help queue` for more information.");
        };
    }

    /**
     * Shows the current queue.
     *
     * @return the command response
     */
    private CommandResponse show() {
        // Initialize all necessary variables
        final List<TrackRecord> trackList = new ArrayList<>(queue);
        trackList.addFirst(new TrackRecord(currentTrack.track(), null, null, currentTrack.trackType())); // Null values are not used
        StringBuilder tracksField = new StringBuilder();
        int trackCounter = 1;
        Member member = getMember();
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
            return new CommandResponse((MessageEmbed) pages.getFirst().getContent());
        } else {
            return new CommandResponseBuilder().addEmbeds((MessageEmbed) pages.getFirst().getContent())
                    .setPostExecutionAsMessage(
                            success -> Pages.paginate(success, pages, true)
                    )
                    .build();
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
     *
     * @return the command response
     */
    private CommandResponse shuffle() {
        List<TrackRecord> trackList = new ArrayList<>();
        queue.drainTo(trackList);
        Collections.shuffle(trackList);
        queue.clear();
        queue.addAll(trackList);
        return new CommandResponse("Shuffled.");
    }

    /**
     * Clears the queue.
     */
    private CommandResponse clear() {
        clearQueue(getGuild(), scheduler);
        return new CommandResponse("Queue cleared.");
    }

    /**
     * Clears the queue.
     * @param guild the guild
     * @param scheduler the track scheduler
     */
    public static void clearQueue(Guild guild, TrackScheduler scheduler) {
        for (TrackRecord record : scheduler.queue) {
            if (record.trackType() == TrackType.TTS) {
                TTSCommand.nextTTSMessage(guild, record.track());
            }
        }
        scheduler.queue.clear();
        scheduler.looping = LoopCommand.looping.NONE;
        scheduler.currentTrack = null;
        scheduler.player.stopTrack();
        scheduler.player.setPaused(false);
        AudioReceiveListener.stopListening(guild);
    }

    /**
     * Removes a track from the queue.
     *
     * @return the command response
     */
    private CommandResponse remove(int position) {
        try {
            position = Integer.parseInt(getOptionValue("position", position));
        } catch (Exception e) {
            return new CommandResponse("Please enter an integer corresponding to a track's position in the queue.");
        }
        if (position < 1 || position > queue.size() + 1) {
            return new CommandResponse("Please enter an integer corresponding to a track's position in the queue.");
        }

        TrackRecord current = null;
        boolean wasLooping = scheduler.looping == LoopCommand.looping.TRACK;
        if (position == 1) {
            if (queue.isEmpty()) {
                scheduler.looping = LoopCommand.looping.NONE;
            }

            if (wasLooping) {
                scheduler.looping = LoopCommand.looping.NONE;
            }
            scheduler.nextTrack();
            tryNextTTSMessage(currentTrack);
            current = currentTrack;
        } else {
            int count = 1;
            Iterator<TrackRecord> iterator = queue.iterator();
            while (!checkCountPosition(count, position, current, iterator)) {
                current = iterator.next();
                count++;
            }
        }

        AudioTrackInfo info = current.track().getInfo();
        String message = String.format("Removed track at position %s: %s (%s) by %s. %s",
                markdownBold(position),
                markdownLinkNoEmbed(info.title, info.uri),
                timeLeft(current.track(), 0), // Use 0 so it doesn't say "left - looping"
                markdownBold(info.author),
                position == 1 && wasLooping ? "Looping has been turned off." : ""
        );


        return new CommandResponse(message);
    }

    /**
     * Tries to go to the next TTS message.
     * @param track the track
     */
    private void tryNextTTSMessage(TrackRecord track) {
        if (track.trackType() == TrackType.TTS) {
            TTSCommand.nextTTSMessage(getGuild(), track.track());
        }
    }

    /**
     * Checks the current position, removing the track if it matches. Returns true if the track was removed.
     * @param count the count
     * @param position the position
     * @param track the track
     * @param iterator the iterator
     * @return true if the track was removed
     */
    private boolean checkCountPosition(int count, int position, TrackRecord track, Iterator<TrackRecord> iterator) {
        if (count == position) {
            tryNextTTSMessage(track);
            iterator.remove();
            return true;
        }
        return false;
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
        String trackDetails = String.format("%d. ", index);
        switch (record.trackType()) {
            case TRACK, FILE -> trackDetails += String.format("%s (%s) by %s",
                    markdownLinkNoEmbed(info.title, info.uri),
                    timeLeft(track, index),
                    markdownBold(info.author)
            );
            case TTS -> trackDetails += String.format("TTS Message: \"%s\" ",
                    info.title
            );
        }
        return trackDetails + "\n";
    }

    /**
     * Returns the formatted string for the time left on a track.
     * @param track the track
     * @param index the index
     * @return the time left
     */
    private String timeLeft(AudioTrack track, int index) {
        if (track.getInfo().isStream) {
            return markdownCode("LIVE");
        } else if (index == 1) {
            return markdownCode(TimeFormat.formatTime(track.getDuration() - track.getPosition())) + " left" + (scheduler.looping == LoopCommand.looping.TRACK ? " - looping" : "");
        } else {
            return markdownCode(TimeFormat.formatTime(track.getDuration()));
        }
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
    public Boolean shouldBeInvisible() {
        return false;
    }

    @Override
    public List<String> getAliases() {
        return List.of("q", "clear", "shuffle", "remove");
    }
}